// Copyright (c) 2025 Unicode, Inc.
// For terms of use, see http://www.unicode.org/copyright.html
// SPDX-License-Identifier: Unicode-3.0

import { XMLParser } from "fast-xml-parser";
import { readFileSync } from "node:fs";
import { join } from "node:path";
import * as abnf from "abnf";
import peggy from "peggy";

/** relative path to ABNF */
export const ABNF_DIR = "../../../keyboards/abnf";

/**
 * @param {string} abnfPath path to .abnf file
 * @returns the raw parser
 */
export async function getAbnfParser(abnfPath) {
  const parsed = await abnf.parseFile(abnfPath);
  const opts = {
    grammarSource: abnfPath,
    trace: false,
  };
  const text = parsed.toFormat({ format: "peggy" });
  const parser = peggy.generate(text, opts);

  return parser;
}

/**
 * @param {string} abnfPath path to .abnf file
 * @param {Object} parser parser from getAbnfParser
 * @returns function taking a string and returning results (or throwing)
 */
export async function getParseFunction(abnfPath) {
  const parser = await getAbnfParser(abnfPath);
  const opts = {
    grammarSource: abnfPath,
    trace: false,
  };
  const fn = (str) => parser.parse(str, opts);
  return fn;
}

/** @returns true if OK,otherwise throws */
export async function checkXml(path) {
  const parseFrom = await getParseFunction(
    join(ABNF_DIR, "transform-from-required.abnf")
  );
  const parseTo = await getParseFunction(
    join(ABNF_DIR, "transform-to-required.abnf")
  );

  const text = readFileSync(path);
  const parser = new XMLParser({
    ignoreAttributes: false,
    trimValues: false,
    htmlEntities: true,
  });
  const r = parser.parse(text, false);

  let transforms = r?.keyboard3?.transforms;

  if (!transforms) return true; // no transforms

  if (!Array.isArray(transforms)) {
    transforms = [transforms];
  }

  for (const transformSet of transforms) {
    let transformGroups = transformSet?.transformGroup;

    if (!transformGroups) continue; // no transforms

    if (!Array.isArray(transformGroups)) {
      // there was only one transformGroup
      transformGroups = [transformGroups];
    }

    for (const transformGroup of transformGroups) {
      let transforms = transformGroup?.transform;
      if (!transforms) continue;
      if (!Array.isArray(transforms)) {
        transforms = [transforms];
      }
      for (const transform of transforms) {
        const fromStr = transform["@_from"];
        try {
          parseFrom(fromStr);
        } catch (e) {
          throw Error(`Bad from="${fromStr}"`, { cause: e });
        }
        const toStr = transform["@_to"];
        if (toStr) {
          try {
            parseTo(toStr);
          } catch (e) {
            throw Error(`Bad to="${toStr}"`, { cause: e });
          }
        }
      }
    }
  }
  return true;
}

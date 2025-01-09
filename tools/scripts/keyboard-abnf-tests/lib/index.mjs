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

/**
 * Check XML file for valid transform from= and to=
 * @param {string} path path to keyboard XML
 * @returns true if OK,otherwise throws
 */
export async function checkXml(path) {
  // load the ABNF files. This creates two functions, parseFrom and parseTo
  // that can take any text and match against the grammar.

  const parseFrom = await getParseFunction(
    join(ABNF_DIR, "transform-from-required.abnf")
  );
  const parseTo = await getParseFunction(
    join(ABNF_DIR, "transform-to-required.abnf")
  );

  // read the XML and parse it
  const text = readFileSync(path);
  const parser = new XMLParser({
    ignoreAttributes: false,
    trimValues: false,
    htmlEntities: true,
  });
  const r = parser.parse(text, false);

  // pull out the transforms
  let transforms = r?.keyboard3?.transforms;

  if (!transforms) return true; // no transforms

  // If there's only one element, it will be element: {} instead of element: [{ … }]
  // this is how the XML parser works. We convert it to an array for processing.
  if (!Array.isArray(transforms)) {
    transforms = [transforms];
  }

  for (const transformSet of transforms) {
    let transformGroups = transformSet?.transformGroup;

    if (!transformGroups) continue; // no transforms

    // If there's only one element, it will be element: {} instead of element: [{ … }]
    // this is how the XML parser works. We convert it to an array for processing.
    if (!Array.isArray(transformGroups)) {
      // there was only one transformGroup
      transformGroups = [transformGroups];
    }

    for (const transformGroup of transformGroups) {
      let transforms = transformGroup?.transform;
      if (!transforms) continue;

      // If there's only one element, it will be element: {} instead of element: [{ … }]
      // this is how the XML parser works. We convert it to an array for processing.
      if (!Array.isArray(transforms)) {
        transforms = [transforms];
      }
      for (const transform of transforms) {
        // Check the from= string against the from ABNF
        const fromStr = transform["@_from"];
        try {
          parseFrom(fromStr);
        } catch (e) {
          throw Error(`Bad from="${fromStr}"`, { cause: e });
        }
        // Check the to= string against the to ABNF
        const toStr = transform["@_to"];
        if (toStr) { // it's legal to have a missing to=
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

// Copyright (c) 2025 Unicode, Inc.
// For terms of use, see http://www.unicode.org/copyright.html
// SPDX-License-Identifier: Unicode-3.0

import { readFileSync, readdirSync } from "node:fs";
import { join } from "node:path";
import { ABNF_DIR } from "./index.mjs";

/**
 *
 * @param {function} callback  given abnfFile, abnfPath, abnfText
 */
export async function forEachAbnf(callback) {
  return await Promise.all(
    readdirSync(ABNF_DIR).map((abnfFile) => {
      if (!/^.*\.abnf$/.test(abnfFile)) return;
      const abnfPath = join(ABNF_DIR, abnfFile);
      const abnfText = readFileSync(abnfPath);
      return callback({ abnfFile, abnfPath, abnfText });
    })
  );
}

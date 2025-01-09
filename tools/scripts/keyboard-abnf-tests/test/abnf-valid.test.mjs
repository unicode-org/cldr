// Copyright (c) 2025 Unicode, Inc.
// For terms of use, see http://www.unicode.org/copyright.html
// SPDX-License-Identifier: Unicode-3.0

import * as abnf from "abnf";
import { test } from "node:test";
import * as assert from "node:assert";
import { forEachAbnf } from "../lib/util.mjs";

function check_refs(parsed) {
  const errs = abnf.checkRefs(parsed);
  if (!errs) return 0;
  for (const err of errs) {
    console.error(err);
  }
  return 3;
}

await forEachAbnf(async ({ abnfFile, abnfText, abnfPath }) => {
  await test(`Test validity: ${abnfFile}`, async (t) => {
    const parsed = await abnf.parseFile(abnfPath);
    assert.equal(check_refs(parsed), 0);
  });
});

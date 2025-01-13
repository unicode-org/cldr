// Copyright (c) 2025 Unicode, Inc.
// For terms of use, see http://www.unicode.org/copyright.html
// SPDX-License-Identifier: Unicode-3.0

import { existsSync, readFileSync, readdirSync } from "node:fs";
import { test } from "node:test";
import { basename, join } from "node:path";
import * as assert from "node:assert";
import { forEachAbnf } from "../lib/util.mjs";
import { getParseFunction } from "../lib/index.mjs";

async function assertTest({ t, abnfPath, testText, expect }) {
  const parser = await getParseFunction(abnfPath);
  for (const str of testText
    .trim()
    .split("\n")
    .filter((l) => !/^#/.test(l))) {
    await t.test(`"${str}"`, async (t) => {
      if (!expect) {
        assert.throws(
          () => parser(str),
          `Expected this expression to fail parsing`
        );
      } else {
        const results = parser(str);
        assert.ok(results);
      }
    });
  }
}

await forEachAbnf(async ({ abnfFile, abnfText, abnfPath }) => {
  await test(`Test Data: ${abnfFile}`, async (t) => {
    const stub = basename(abnfFile, ".abnf");
    const testDir = `./${stub}.d`;
    assert.ok(existsSync(testDir), `No test dir: ${testDir}`);
    const tests = readdirSync(testDir)?.filter((f) =>
      /^.*\.(pass|fail)\.txt/.test(f)
    );
    assert.ok(tests && tests.length, `No tests in ${testDir}`);
    for (const testFile of tests) {
      if (testFile.endsWith(".pass.txt")) {
        await t.test(`${stub}/${testFile}`, async (t) => {
          await assertTest({
            t,
            abnfPath,
            testText: readFileSync(join(testDir, testFile), "utf-8"),
            expect: true,
          });
        });
      } else if (testFile.endsWith(".fail.txt")) {
        await t.test(`${stub}/${testFile}`, async (t) => {
          await assertTest({
            t,
            abnfPath,
            testText: readFileSync(join(testDir, testFile), "utf-8"),
            expect: false,
          });
        });
      } else throw Error(`Unknown testFile ${testFile}`);
    }
  });
});

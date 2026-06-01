// Copyright (c) 2025 Unicode, Inc.
// For terms of use, see http://www.unicode.org/copyright.html
// SPDX-License-Identifier: Unicode-3.0

import { join } from "node:path";
import { readdirSync } from "node:fs";
import { test } from "node:test";
import { checkXml } from "../lib/index.mjs";

const KBD_DIR = "../../../keyboards/3.0";

await test("Testing Keyboard XML files for valid transform from/to attributes", async (t) => {
  // keyboards, excluding -test.xml files
  const kbds = readdirSync(KBD_DIR).filter((f) =>
    /^.*(?<!-test)\.xml$/.test(f)
  );
  for (const kbd of kbds) {
    await t.test(`Testing ${kbd}`, async (t) => {
      await checkXml(join(KBD_DIR, kbd));
    });
  }
});

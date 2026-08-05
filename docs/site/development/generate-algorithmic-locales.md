---
title: Generate algorithmic locales
---

* Run the tool `org.unicode.cldr.tool.CLDRFileTransformer` to generate transformed locales.
* Run the tool with the `-h` option to get usage information
* Overwrites files in-place.
* Inspect, commit, open a PR
* If tests complain, might need to re-run generator tools such as `GenerateLocaleIDTestData` and `GeneratePersonNameTestData`

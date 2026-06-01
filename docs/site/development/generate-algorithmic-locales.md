---
title: Generate algorithmic locales
---

* Recommended: Remove existing `../Generated/cldr` directory.
* Run the tool `org.unicode.cldr.tool.CLDRFileTransformer` to generate transformed locales.
* Copy the generated xml files from `../Generated/cldr/common` into `common/`
* Inspect, commit, open a PR
* If tests complain, might need to re-run generator tools such as `GenerateLocaleIDTestData` and `GeneratePersonNameTestData`


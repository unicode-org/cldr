---
title: GenerateProductionData
---

This tool produces the final output .xml for release.

The CLDR core zip, `core.zip` is based on a run of `GenerateProductionData` with no arguments (default options).

The `-d` option controls the target directory, with the default location of `-d${CLDR_DIR}../cldr-staging/production/common`

## Exemplars

The `exemplars-delta` zip includes data from the `exemplars` tree, and also includes data for locales not yet at basic.

This is accomplished with the following options: (Include exemplars tree, Keep pre-basic.)

    GenerateProductionData -x -k -d../cldr-staging/exemplars-delta/common --delta=../cldr-staging/production-common

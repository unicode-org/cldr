---
title: Running Tools
---

# Running Tools

## Provides tools for manipulating CLDR files. Warning: these tools do not have much documentation

### Overview

**Variables: (you'll need to set these to whatever you have on your system)**

For the purposes of this document, / and \\ are equivalent. Note: Directories must have a trailing slash.

| Variables Here | Description | Example |
|---|---|---|
| {cldrdata} | Location of cldr data root; that is, common, docs, dropbox are found here. | /Users/markdavis/Documents/workspace/cldr/src/ |

**Tools:**

| Goal | Program Name | Description |  Arguments |
|---|---|---|---|
|   |  | *Common Arguments for all of following* | -Dfile.encoding=UTF-8<br> -Xmx700M<br> -DSHOW_FILES<br> -DSHOW<br> -D CLDR_DIR ={cldrdata} |
|  Testing |  CLDRConsole |  General testing of data |  -r<br> // (use -h to see options) |
| Canonicalizing format | CLDRModify | General verification of vetted data | -r<br> // (use -h to see options) |
| Generating Statistics | CountItems | Generate something like:<br> &emsp;Total Items 66,319<br> &emsp;Total Resolved Items 1,025,077<br> &emsp;Unique Paths 4,717<br> &emsp;Unique Values 45,226<br> &emsp;Unique Full Paths 9,301 | -Dmethod=countItems<br> -DSOURCE={cldrdata}\cldr_1_4\main<br><br> -Dmethod=countItems -DSOURCE={cldrdata}\incoming\vetted\main |
|  Build most charts |  ShowLanguages |   |  TBD |


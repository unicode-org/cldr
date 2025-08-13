---
title: Generate algorithmic locales
---

# Generate algorithmic locales

* Run (and edit if necessary when?) tool/CLDRFileTransformer to generate transformed locales. 
* Copy the generated xml files into common/{main, annotations, subdivisions}.

> NOTE: Need to re-run each modified file in (main, annotations, subdivisions) through CLDRModify -fp (but NOT minimize (-r)) after generation!

 * Can be done with filter, eg (but update list!!)

```
-s${workspace_loc}/cldr/common/annotations
-fp
m(de_CH|ha_NE|sr_Latn_BA|sr_Latn_ME|sr_Latn_XK|sr_Latn|yo_BJ|yue_Hans)\
```

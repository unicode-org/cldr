---
title: Updating UN Codes
---

# Updating UN Codes

1. UM M19
	1. Open https://unstats.un.org/unsd/methodology/m49/overview/
	2. Hit the Copy button, to copy all the data to the clipboard
	3. Open ...workspace/cldr/tools/java/org/unicode/cldr/util/data/external/UnCodes.txt
	4. Hit paste. you should see tab\-separated fields
	5. Save
2. Note: "git diff \-\-word\-diff" is helpful for finding that, for example, only a column was added.

### EU
1. Go to  [https://europa.eu/european\-union/about\-eu/countries\_en](https://european-union.europa.eu/principles-countries-history/eu-countries_en)
2. **Note: The instructions below don't work. Manually update tools/cldr\-code/src/main/resources/org/unicode/cldr/util/data/external/EuCode.txt**
3. ~~(Old instructions:  do the same with https://europa.eu/european\-union/about\-eu/countries/member\-countries\_en, into util/data/external/eu\_member\_states\_raw.txt  BROKEN LINK )~~
4. ~~Find the section "The XX member countries of the EU: (may be a link at the bottom or sidebar)~~
5. ~~Copy and past into ...workspace/cldr/tools/java/org/unicode/cldr/util/data/external/EuCodes.txt~~
6. ~~Compare with last revision; if there are differences, update containment.~~ 
	1. ~~If there are no real differences, do not bother updating EuCodes.txt~~
	2. ~~Note: "git diff \-\-word\-diff" is helpful for finding that, for example, only whitespace changed.~~
	3. ~~Record the latest version that's been synced as a meta\-data//This is new (Aug 2020\)!~~
	4. ~~Q: Not sure how or where to do this?~~

### Run TestUnContainment
1. ```mvn -Dorg.unicode.cldr.unittest.testArgs='-n -q -filter:TestUnContainment'  --file=tools/pom.xml -pl cldr-code test -Dtest=TestShim```


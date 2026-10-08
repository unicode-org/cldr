---
title: Run GenerateTestData.java
---

# Run GenerateTestData.java

There are currently 5 directories in common/testData.
Each also has a _readme.txt with copyright information for all the files in that directory. 
The format of the files in the directory is either in the individual data files, or in the _readme.txt

* localeIdentifiers — generated data (GenerateLocaleIDTestData, GenerateLikelySubtagTests)
  * localeCanonicalization.txt
  * localeDisplayName.txt
  * likelySubtags.txt
* personNameTest — generated data (GeneratePersonNameTestData)
  * af.txt
  * am.txt
  * …
* segmentation — curated data (not generated)
  * graphemeCluster
    * TestSegmenter-Bengali.txt
    * TestSegmenter-Devanagari.txt
    * …
* transforms — curated data (not generated)
  * am-fonipa-t-am.tx
  * am-Latn-t-am-m0-bgn.txt
  * am-t-am-fonipa.txt
  * …
* units — generated data (TestUnits)
  * unitPreferencesTest.txt
  * unitsTest.txt
* currency — generated data (GenerateCurrencyFormatTestData)
  * currencies.tsv
  * currencies_modern_locales.tsv
  * currencies_\<display\>_modern_currencies.tsv
  * currencies_\<display\>_extended_numbers.tsv

## Currency test data

GenerateTestData calls GenerateCurrencyFormatTestData. To regenerate only the currency test data, run from the repository root:

```bash
mvn compile exec:java -Dexec.mainClass="org.unicode.cldr.tool.GenerateCurrencyFormatTestData" -pl tools/cldr-code
```

To run the unit test that checks the generated files:

```bash
mvn test -pl tools/cldr-code -Dtest=TestShim -Dsurefire.failIfNoSpecifiedTests=false "-Dorg.unicode.cldr.unittest.testArgs=-f:TestCurrencyFormat -n"
```

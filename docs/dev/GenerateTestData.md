# TL;DR

Run GenerateTestData.java. There is still a manual step at the end

# Structure

There are currently 4 directories in common/testData.
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

# Generation

To produce the generated data (units/ and localeIdentifiers/):

* localeIdentifiers/
  * localeCanonicalization.txt
    * run GenerateLocaleIDTestData.java. It will generate the file in place.
  * localeDisplayName.txt
    * when you ran GenerateLocaleIDTestData.java it also generated this one
  * likelySubtags.txt
    * run GenerateLikelyTestData, to generate in place
* units/
 * run TestUnits with -DTestUnits:GENERATE_TESTS. It will create the two files in place.

# Issues

We want to supplant the manual step with one GenerateTestData.java.
See https://unicode-org.atlassian.net/browse/CLDR-14186
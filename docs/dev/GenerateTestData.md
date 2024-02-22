# TL;DR

Run GenerateTestData.java.

# Structure

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

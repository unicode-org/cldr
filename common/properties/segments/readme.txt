CLDR Segmentation data
#  Copyright © 1991-2020 Unicode, Inc.
#  For terms of use, see http://www.unicode.org/copyright.html
#  Unicode and the Unicode Logo are registered trademarks of Unicode, Inc. in the U.S. and other countries.
#  CLDR data files are interpreted according to the LDML specification (http://unicode.org/reports/tr35/)
The segments directory contains files used to customize the default segmentation data in the UCD. 

Currently this just applies to the Grapheme Cluster Break (GCB) (https://unicode.org/reports/tr29/) algorithm,
which was used in CLDR 35..43 to add support for not splitting Indic aksaras.
Unicode 15.1 has adoped these changes.
Starting with CLDR 44, the GraphemeBreakTest.* files are the same as in the UCD.

See the test files supplied by India to org.unicode.cldr.unittest.data.graphemeCluster/*

  TestSegmenter-Bengali.txt
  TestSegmenter-Devanagari.txt
  TestSegmenter-Gujarati.txt
  TestSegmenter-Malayalam.txt
  TestSegmenter-Odia.txt
  TestSegmenter-Telugu.txt


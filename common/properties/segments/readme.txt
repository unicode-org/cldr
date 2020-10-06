CLDR Segmentation data
#  Copyright © 1991-2020 Unicode, Inc.
#  For terms of use, see http://www.unicode.org/copyright.html
#  Unicode and the Unicode Logo are registered trademarks of Unicode, Inc. in the U.S. and other countries.
#  CLDR data files are interpreted according to the LDML specification (http://unicode.org/reports/tr35/)
The segments directory contains files used to customize the default segmentation data in the UCD. 

Currently this just applies to the Grapheme Cluster Break (GCB) (https://unicode.org/reports/tr29/) algorithm,
to add support for not splitting Indic aksaras.

The modifications are:

1. Adding 3 new character categories to https://unicode.org/reports/tr29/#Grapheme_Cluster_Break_Property_Values

  Virama=[\p{Gujr}\p{sc=Telu}\p{sc=Mlym}\p{sc=Orya}\p{sc=Beng}\p{sc=Deva}&\p{Indic_Syllabic_Category=Virama}]

  LinkingConsonant=[\p{Gujr}\p{sc=Telu}\p{sc=Mlym}\p{sc=Orya}\p{sc=Beng}\p{sc=Deva}&\p{Indic_Syllabic_Category=Consonant}]

  ExtCccZwj=[\p{gcb=Extend}-\p{ccc=0}] \p{gcb=ZWJ}]

Note that these categories are not GCB property values:
In fact, they overlap the GCB property values.
It is not necessary for the rules to have disjoint categories.
The list of scripts can be added to over time, as test files for them become available. 

2. Adding a rule to https://unicode.org/reports/tr29/#Grapheme_Cluster_Boundary_Rules

  9.3) LinkingConsonant ExtCccZwj* Virama ExtCccZwj* × LinkingConsonant
 
3. Adding test files supplied by India to org.unicode.cldr.unittest.data.graphemeCluster/*

  TestSegmenter-Bengali.txt
  TestSegmenter-Devanagari.txt
  TestSegmenter-Gujarati.txt
  TestSegmenter-Malayalam.txt
  TestSegmenter-Odia.txt
  TestSegmenter-Telugu.txt
  
4. Adding modified files in this directory, which can be used in place of the default files from 
   https://unicode.org/Public/12.0.0/ucd/auxiliary/

  GraphemeBreakTest.html
  GraphemeBreakTest.txt
  
Note: The GraphemeBreakProperty.txt file is unmodified, as those properties don't change.

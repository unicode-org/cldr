The graphemeCluster directory contains files used to modify the default Grapheme Cluster Break (gcb)
(https://unicode.org/reports/tr29/) algorithm to add support for not splitting Indic aksaras.

The modifications are:

1. Adding 3 new character categories to https://unicode.org/reports/tr29/#Grapheme_Cluster_Break_Property_Values

  Virama=[\\p{Gujr}\\p{sc=Telu}\\p{sc=Mlym}\\p{sc=Orya}\\p{sc=Beng}\\p{sc=Deva}&\\p{Indic_Syllabic_Category=Virama}]

  LinkingConsonant=[\\p{Gujr}\\p{sc=Telu}\\p{sc=Mlym}\\p{sc=Orya}\\p{sc=Beng}\\p{sc=Deva}&\\p{Indic_Syllabic_Category=Consonant}]

  ExtCccZwj=[[gcb:Extend-\\p{ccc=0}] ZWJ]

Note that these categories are not gcb property values: in fact, they overlap the gcb property values.
It is not however necessary for the rules to have disjoint categories.

2. Adding a rule to https://unicode.org/reports/tr29/#Grapheme_Cluster_Boundary_Rules

  9.3) Virama ExtCccZwj* ÷ LinkingConsonant
 
3. Adding test files supplied by India to org.unicode.cldr.unittest.data.graphemeCluster/*
  TestSegmenter-Bengali.txt
  TestSegmenter-Devanagari.txt
  TestSegmenter-Gujarati.txt
  TestSegmenter-Malayalam.txt
  TestSegmenter-Odia.txt
  TestSegmenter-Telugu.txt
  
4. Adding modified files in this directory, which can be used in place of the default files from 
   https://unicode.org/Public/12.0.0/ucd/auxiliary/

  GraphemeBreakTest-12.0.0mod.html
  GraphemeBreakTest-12.0.0mod.txt
  
Note: the GraphemeBreakProperty-12.0.0d.txt file is unmodified, as those properties don't change.

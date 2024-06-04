---
title: Copy en_GB to en_001
---


# BRS: Copy en\_GB to en\_001

The program **CompareEn.java** can be used to copy data from en\_GB up to en\_001.

Options:

-   \-u (uplevel) — move elements from en\_GB into en\_oo1. By default, the output directory is common/main and common/annotations in trunk
    -   If not present, just write a comparison to Generated/cldr/comparison/en.txt   
-   \-v (verbose) — provide verbose output
    
1.  Run with no options first.
    1.  That generates a file that indicates what changes would be made.   
    2.  Put that file in a spreadsheet     
    3.  Post to the CLDR TC for review.     
    4.  You'll then want to retract any items that shouldn't be copied.     
    5.  Change CompareEn.java if there are paths that should be skipped in the future.      
2.  Once you agree on the results, you'll run -u. 
    1.  That will modify your local copy of en\_oo1.xml      
    2.  Then do a diff with HEAD to make sure it matches expectations   
    3.  Then check in en\_oo1.xml and CompareEn.java

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
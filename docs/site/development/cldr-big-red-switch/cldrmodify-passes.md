---
title: CLDRModify Passes
---

# CLDRModify Passes

## Main Process

This section describes how to run the CLDRModify passes for the mechanical cleanup of the data before release.

### Successive Passes

You will run CLDRModify with different options, in multiple passes.

   1. Sanity check the results during each step, fix any problems, tag, and check in as described below.  
   1. ***This sanity check is important,*** since the regularizing often reveals problems in the original. For example, a date format like MMM'-yy regularizes to MMM-'yy' \-- but the original was clearly an error.
   1. If you need to do a single file over again (eg resolving conflicts), use the \-m option on CLDRModify, as described below.

### After passes

* After you are all done, run ConsoleCheckCLDR once more, to make sure you didn't introduce any new errors.

**Details**

For the purpose of this document, we'll assume you are generating into {cldrdata}/dropbox/gen/main/ as the target directory. Change any instance below to the directory that you actually use.

### Passes

* Each pass will go from a sourceDir (default=CLDRPaths.***MAIN\_DIRECTORY)*** to a targetDir (default \= CLDRPaths.***GEN\_DIRECTORY*** \+ "cldrModify/")  
  * Empty the contents of targetDir before running.  
  * Each time you will run CLDRModify with the standard options, plus one of the listed [Options](#options) bulleted below.  
  * After building, remember to refresh data files with F5 if you are using Eclipse, after building.  
  * Dif sourceDir and targetDir directories to make sure that there aren't any spurious changes.  
  * Then copy the targetDir files to the sourceDir files  
  * Run ConsoleCheckCLDR to verify that no errors have been introduced. Standard options, plus:  
    * *// set this to test in the original directory:* \-DCLDR\_MAIN={sourceDir}. You can set up some configuration.  
    * \-e  
    * \-z final\_testing  
  * If ok, check in (see [How to check in consistently after each pass](#how-to-check-in-consistently-after-each-pass)).  
  * Otherwise, either manually patch, or revert from SVN Head files to get back to a clean state, fix, and repeat.

### Options

Standard Options: add to your regular preferences \-DSHOW\_FILES *plus* your choice of source/target directories.

1. After doing the **/common/main/** files, do the other directories, with the extra options:  
   1. \-s{cldrdir}**/common/annotations**
   1. \-s{cldrdir}**/common/subdivisions** 
   1. \-s{cldrdir}**/exemplars/main**

Other options for each pass:  

You will have to repeat this cycle if any outside changes are made to the data\!

One-Time Fixes

There are a number of "one time fixes" that are in the CLDRModify code. The code remains in case we want to adapt for future cases, but don't use them unless you fix the code to do what you want, and carefully diff the results. Here are some of them:

1. \-fk: use a configuration file. Details on [CLDRModify Config](cldrmodify-using-config-file)  
   2. \-fe: fix Interindic. If you need to make changes in transliteration, you might want to modify this and run it.  
   3. \-fs: Fix the stand-alone narrow values.  
   4. \-fu: Fix the unit patterns.  
   5. \-fd: Fix dates  
   6. \-fz: Fix exemplars  
   7. \-fr: Fix references and standard  
   8. \-fc: Transition from an old currency to a new currency.  This fix is quite useful when a country introduces a new currency code ( usually due to a devaluation ), but the name remains the same.  In order to use this fix, modify the following values in the CLDRModify code under "**fixList.add('c', "Fix transiton from an old currency code to a new one**"  
      1. Change the String oldCurrencyCode and newCurrencyCode to reflect the currency codes you are transitioning.  
      2. Change the int fromDate and toDate to reflect the dates that the old currency was in circulation. These will be used to create the date range in the old currency string.  
      3. Run the CLDRModify tool as usual, diff the results and check in.

## How to check in consistently after each pass

### Sanity Check

1. The console will list changes made, such as:  
   * Creating File: {cldrdata}/dropbox\\gen\\main\\zh\_Hant.xml  
     * \*Renaming old {cldrdata}/dropbox\\gen\\main\\zh\_Hant.xml  
     * %zh\_Hant\_HK    \-    Replacing: \<yy'年'M'月'd'日'\>    by \<yy年M月d日\>     at: //ldml/dates/calendars/calendar\[@type="gregorian"\]/dateFormats/dateFormatLength\[@type="short"\]/dateFormat\[@type="standard"\]/pattern\[@type="standard"\]  
2. The diff folder in the output has CompareIt\! bat files for each change, or you can use SVN diff after moving to the SVN folder by doing the Copy and then checking.

### Copy Files

* Copy from the target directory (eg {cldrdata}/dropbox\\gen\\main) to the svn directory (eg {cldrdata}/common\\main)  
  * Run CLDRModify again, with nothing but standard options. This will verify that there are no xml errors.  
    * There should be no change in the output; thus you should not see anything but \_unchanged\_.....xml files in the target directory.

### Now ready to check in

* Run the CLDR Unit Tests to make sure you didn't break anything  
  * Run ConsoleCheckCLDR \-e \-z final\_testing  
  * Check in \- I usually use a comment such as "cldrbug 1234: BRS Task A21 : CLDRModify \-fp"

**If someone checks in a change in the middle of one of your passes, it is generally easier to check in the rest of the changes, check out a clean copy of that file, and return the pass with only that file. The \-m(uk) option can be used to restrict the pass to only uk.xml, for example.**

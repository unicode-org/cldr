---
title: CLDRModify Passes
---

# CLDRModify Passes

## Main Process

This section describes how to run the CLDRModify passes for the mechanical cleanup of the data at various points in the BRS.

### Successive Passes

You will run CLDRModify with different options, in multiple passes.

   1. Sanity check the results during each step, fix any problems, tag, and check in as described below.  
   1. ***This sanity check is important,*** since the regularizing often reveals problems in the original. For example, a date format like MMM'-yy regularizes to MMM-'yy' \-- but the original was clearly an error.
   1. If you need to do a single file over again (eg resolving conflicts), use the \-m option on CLDRModify, as described below.

**Details**

For the purpose of this document, we'll assume you are generating into {cldrdata}/dropbox/gen/main/ as the target directory. Change any instance below to the directory that you actually use.

### Passes

  * Each pass will go from a sourceDir (default=CLDRPaths.***MAIN\_DIRECTORY)*** to a targetDir (default \= CLDRPaths.***GEN\_DIRECTORY*** \+ "cldrModify/")  
  * Empty the contents of targetDir before running. (If you forget, just remember after building to delete the old files.)
  * Each time you will run CLDRModify with the standard options, plus one of the listed [Options](#options) bulleted below.
  * The console will list changes made, such as:  
     * Creating File: {cldrdata}/dropbox\\gen\\main\\zh\_Hant.xml  
     * \*Renaming old {cldrdata}/dropbox\\gen\\main\\zh\_Hant.xml  
     * %zh\_Hant\_HK    \-    Replacing: \<yy'年'M'月'd'日'\>    by \<yy年M月d日\>     at: //ldml/dates/calendars/calendar\[@type="gregorian"\]/dateFormats/dateFormatLength\[@type="short"\]/dateFormat\[@type="standard"\]/pattern\[@type="standard"\]  
  * After building, remember to refresh data files with F5 if you are using Eclipse, after building.  
  * Dif sourceDir and targetDir directories to make sure that there aren't any spurious changes.  
  * Then copy the targetDir files to the sourceDir files  
  * Run ConsoleCheckCLDR to verify that no errors have been introduced. Standard options, plus:  
    * *// set this to test in the original directory:* \-DCLDR\_MAIN={sourceDir}.  
    * \-e  
    * \-z final\_testing  
  * Otherwise, either manually patch, or revert from HEAD files to get back to a clean state, fix, and repeat.

### Options

Standard Options: add to your regular preferences \-DSHOW\_FILES *plus* your choice of source/target directories.

1. After doing the **/common/main/** files, do the other directories, with the extra options:  
   1. \-s{cldrdir}**/common/annotations**
   1. \-s{cldrdir}**/common/subdivisions** 
   1. \-s{cldrdir}**/exemplars/main**

#### Other options for each pass:  

You will have to repeat this cycle if any outside changes are made to the data\!

#### One-Time Fixes

There are a number of "one time fixes" that are in the CLDRModify code. The typical ones run at start and end of the release are:

0. _no settings_: simply reformat the file. Recommended to do this first, so that the later diffs are clean.
1. \-fP: Apply DAIP (DisplayAndInputProcessor) to clean up values from manual changes
2. \-fC: Generate derived /main path-values for all locales
    * Currently this needs to be run multiple times, until there are no more changes. This is because modifications trickle down due to inheritance.
3. \-fE: Fix null/inherited values in en.xml.

#### Config file
1. \-fk: use a configuration file; see the details on [CLDRModify Config](cldrmodify-using-config-file).

#### Inactive options
The following are some of the **inactive** options. The code remains in case we want to adapt for future cases, but don't use them unless you fix the code to do what you want, and carefully diff the results. You will have to enable them in checkSuboptions(): limits the ones that are active to prevent mistakes.

1. \-fc: Transition from an old currency to a new currency. This fix is quite useful when a country introduces a new currency code (usually due to a devaluation), but the name remains the same. In order to use this fix, modify the following values in the CLDRModify code under "**fixList.add('c', "Fix transiton from an old currency code to a new one**"
    1. Change the String oldCurrencyCode and newCurrencyCode to reflect the currency codes you are transitioning.  
    2. Change the int fromDate and toDate to reflect the dates that the old currency was in circulation.
These will be used to create the date range in the old currency string.  
      4. Run the CLDRModify tool as usual, diff the results and check in.
2. \-fe: fix Interindic. If you need to make changes in transliteration, you probably want to modify this and run it.  
4. \-fs: Fix the stand-alone narrow values.  
5. \-fu: Fix the unit patterns.  
6. \-fd: Fix dates  
7. \-fz: Fix exemplars  
8. \-fr: Fix references and standard
9. ...

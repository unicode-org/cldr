---
title: Updating English/Root
---

# Updating English/Root

Whenever you update English or Root, there is one additional step that needs to be done for the vetting viewer and tests to work properly.

Update CldrVersion.java to have the newest release in the list.

## Run GenerateBirth

The tool is in tools/java/org/unicode/cldr/tool/GenerateBirth.java. It requires a set of sources from all previous major CLDR release, trunk, and a generation directory. These three directories must be structured as follows. The tool takes environment parameters for the second two.

**cldr (set with \-t \<target\>, default\=CldrUtility.BASE\_DIRECTORY, set with environment variable \-DCLDR\_DIR)**

... common/ ... tools/ java/ (apps such as GenerateBirth are run from here) ...

**CldrUtility.ARCHIVE\_DIRECTORY**

1. Create the archive ([Creating the Archive](/development/creating-the-archive)) with all releases (if you don't have it already)
2. The archive directory should have the latest version of every major and minor version (where versions before 21\.0 have the major version split across the top two fields).
3. You will probably need to modify both CldrVersion.java and ToolConstants.java to bring them up to date.

**log (set with \-l \<log\>, default\=CldrUtility.UTIL\_DATA\_DIR, set with CLDR\_DIR**

Pass an argument for \-t to specify the output directory. Takes a few minutes to run (and make sure you have set Java with enough memory)!

The tool generates (among other things) the following two binary files (among others) in the output directory specified with \-t:

- **outdated.data**
- **outdatedEnglish.data**

Replacing the previous versions in /cldr/tools/java/org/unicode/cldr/util/data/births/. These files are used to support OutdatedPaths.java, which is used in CheckNew.

Readable data is found in https://github.com/unicode\-org/cldr\-staging/tree/master/births/\* That should also be checked in, for comparison over time. Easiest to read if you paste into a spreadsheet!

## Binary File Format

| outdatedEnglish.data | outdated.data |
|---|---|
| **int:size** | **str:locale** |
| long:pathId str:oldValue | **int:size** |
| long:pathId str:oldValue | long:pathId |
| ... | long:pathId |
|  | ... |
|  | **str:locale** |
|  | **int:size** |
|  | long:pathId |
|  | long:pathId |
|  | ... |
| **\$END\$** | **\$END\$** |
| ~50KB | ~100KB |

In a limited release, the file **SubmissionLocales.java** is set up to allow just certain locales and paths in those locales.

## Testing

Make sure TestOutdatedPaths.java passes. It may take some modifications, since it depends on the exact data.

Run TestCheckCLDR and TestBasic with the option **\-prop:logKnownIssue\=false** (that option is important!). This checks that the Limited Submission is set up properly and that SubmissionLocales are correct.



If you run into any problems, look below at debugging.

**Check in the files**

Eg https://github.com/unicode-org/cldr/pull/243

## Debugging

It also generates readable log files for double checking. These will be in {workspace}/cldr\-aux/births/\<version\>/, that is: CLDRPaths.AUX\_DIRECTORY \+ "births/" \+ trunkVersion. Examples: https://unicode.org/repos/cldr-aux/births/35.0/en.txt, https://unicode.org/repos/cldr-aux/births/35.0/fr.txt.

Their format is the following (TSV \= tab\-delimited\-values) — to view, it is probably easier to copy the files into a spreadsheet.

- English doesn't have the E... values, but is a complete record.
- Other languages only have lines where the English value is more recently changed (younger) than the native’s.
- So what the first line below says is that French has "bengali" dating back to version 1\.1\.1, while English has "Bangla" dating back to version 30\.

| Loc | Version | Value | PrevValue | EVersion | EValue | EPrevValue | Path |
|---|:---:|---|---|:---:|---|---|---|
| fr | 1.1.1 | bengali | � | 30 | Bangla | Bengali | //ldml/localeDisplayNames/languages/language[@type="bn"] |
| fr | 1.1.1 | galicien | � | 1.4.1 | Galician | Gallegan | //ldml/localeDisplayNames/languages/language[@type="gl"] |
| fr | 1.1.1 | kirghize | � | 24 | Kyrgyz | Kirghiz | //ldml/localeDisplayNames/languages/language[@type="ky"] |
| fr | 1.1.1 | ndébélé du Nord | � | 1.3 | North Ndebele | Ndebele, North | //ldml/localeDisplayNames/languages/language[@type="nd"] |
| fr | 1.1.1 | ndébélé du Sud | � | 1.3 | South Ndebele | Ndebele, South | //ldml/localeDisplayNames/languages/language[@type="nr"] |
| ... |  |  |  |  |  |  |  |
| fr | 34 | exclamation \| point d’exclamation blanc \| ponctuation | exclamation \| point d’exclamation blanc | trunk | ! \| exclamation \| mark \| outlined \| punctuation \| white exclamation mark | exclamation \| mark \| outlined \| punctuation \| white exclamation mark | //ldml/annotations/annotation[@cp="❕"] |
| fr | 34 | exclamation \| point d’exclamation \| ponctuation | exclamation \| point d’exclamation | trunk | ! \| exclamation \| mark \| punctuation | exclamation \| mark \| punctuation | //ldml/annotations/annotation[@cp="❗"] |
| fr | 34 | cœur \| cœur point d’exclamation \| exclamation \| ponctuation | cœur \| cœur point d’exclamation | trunk | exclamation \| heart exclamation \| mark \| punctuation | exclamation \| heavy heart exclamation \| mark \| punctuation | //ldml/annotations/annotation[@cp="❣"] |
| fr | 34 | couple \| deux hommes se tenant la main \| hommes \| jumeaux | couple \| deux hommes se tenant la main \| jumeaux | trunk | couple \| Gemini \| man \| twins \| men \| holding hands \| zodiac | couple \| Gemini \| man \| twins \| two men holding hands \| zodiac | //ldml/annotations/annotation[@cp="👬"] |
| fr | 34 | couple \| deux femmes se tenant la main \| femmes \| jumelles | couple \| deux femmes se tenant la main \| jumelles | trunk | couple \| hand \| holding hands \| women | couple \| hand \| two women holding hands \| woman | //ldml/annotations/annotation[@cp="👭"] |

A value of � indicates that there is no value for that version.


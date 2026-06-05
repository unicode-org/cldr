---
title: Updating English/Root
---

# Updating English/Root

Whenever the English or Root locales have been updated, in order for the vetting viewer and tests to work properly, additional steps are required as summarized here and described in more detail below.

* Update CldrVersion.java and ToolConstants.java
* Create cldr-archive
* Run GenerateBirth
* Run tests
* Check in the binary files for the cldr repo
* Check in the text (log) files for the cldr-staging repo

## Running GenerateBirth

The tool is in tools/java/org/unicode/cldr/tool/GenerateBirth.java. It requires four directories:

1. An archive directory (corresponding to the cldr-archive repository), containing a set of sources from all previous major CLDR releases, which defaults to ../cldr-archive or CLDRPaths.ARCHIVE_DIRECTORY, and may be set with the environment variable CLDR_ARCHIVE
2. Trunk (corresponding to the cldr repository), which defaults to the current directory or may be set with the environment variable CLDR\_DIR
3. An output directory for the binary data files it generates, which defaults to tools/cldr-code/src/main/resources/org/unicode/cldr/util/data/births (replacing the previous versions), and may be set by passing an argument for \-t to specify the output directory
4. An output directory for the readable .txt files it generates (corresponding to the cldr-staging repository; for example, ../cldr-staging/births/49.0)

The archive directory should have the latest version of every major and minor version (where versions before 21\.0 have the major version split across the top two fields).

**Update CldrVersion.java and ToolConstants.java to have the newest release in the list.**

**Create the archive ([Creating the Archive](/development/creating-the-archive)) with all releases (if you don't have it already).**

**Run GenerateBirth**

For help, call with \-h:
```
java -jar tools/cldr-code/target/cldr-code.jar GenerateBirth -h
```

To specify the oldest version to go back to, pass an argument for \-o (such as \-o36.1 for version 36.1).

Example command line:
```
java -jar tools/cldr-code/target/cldr-code.jar GenerateBirth -o46
```

GenerateBirth takes a few minutes to run, and may require setting Java with enough memory.

GenerateBirth generates the following two binary files in the output directory:

- **outdated.data**
- **outdatedEnglish.data**

These files are used to support OutdatedPaths.java, which is used in CheckNew.

GenerateBirth also generates readable .txt files (for cldr-staging) described further below. Their output directory can be overridden with \-l (log) option.

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

**Run the Tests**

Make sure tests pass including TestOutdatedPaths.java and TestCheckCLDR.TestCheckNew. Tests may require modifications, since they depend on the exact data.

Run TestCheckCLDR and TestBasic with the option **\-prop:logKnownIssue\=false** (that option is important!). This checks that the Limited Submission is set up properly and that SubmissionLocales are correct.

If you run into any problems, look below at fixing test failures.

**Check in the files**

E.g., https://github.com/unicode-org/cldr/pull/243

## Readable Files for cldr-staging

GenerateBirth generates readable (.txt) data files. The default location is a in version-specific folder under ../cldr\-staging/births/ or CLDRPaths.STAGING_DIRECTORY. To specify a different output directory, set the \-l (log) option.

Here, **cldr\-staging** corresponds to a git repo distinct from both **cldr** and **cldr\-archive**. Online this data is found in https://github.com/unicode-org/cldr-staging/tree/main/births .

The readable data is easiest to read if pasted into a spreadsheet.

**Check the readable data into the cldr\-staging repo, for comparison over time.**

TODO: The readable data has not been checked into the cldr-staging repo since version 44. Reference: https://unicode-org.atlassian.net/browse/CLDR-19536

The file format is the following (TSV \= tab\-delimited\-values) — to view, it is probably easier to copy the files into a spreadsheet.

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

## Fixing Test Failures

After GenerateBirth has been run, TestCheckCLDR.TestCheckNew is likely to fail. It checks that at least one path in a locale is outdated and one path is not. That may change with each CLDR release. If TestCheckNew fails, it may need to be revised in TestCheckCLDR.java with new paths. To find suitable paths, you can run TestOutdatedPaths with the -v (verbose) option:

```
mvn test --file tools/pom.xml -pl cldr-code -Dtest=org.unicode.cldr.unittest.TestShim -Dorg.unicode.cldr.unittest.testArgs='-e10 -v -n -filter:TestOutdatedPaths'
```

The output will include something like this:

```
    TestShow {
      (TestOutdatedPaths.java:69) de total outdated:	266
      (TestOutdatedPaths.java:78) English (48.0):\t«Saurashtra»	⇒\t«Sourashtra»	Native: «Saurashtra»	Path: Locale Display Names	Languages (O-S)	S	Sourashtra ► saz	XML-Path: //ldml/localeDisplayNames/languages/language[@type="saz"]
...
      (TestOutdatedPaths.java:78) English (47.0):\t«↑↑↑»	⇒\t« »	Native: « »	Path: Miscellaneous	Person Name Formats	AuxiliaryItems	foreignSpaceReplacement	XML-Path: //ldml/personNames/foreignSpaceReplacement
```

Copy those paths from XML-Path into TestCheckCLDR.TestCheckNew, replacing the "Outdated" paths for which it failed. Run TestCheckNew:

```
mvn test --file tools/pom.xml -pl cldr-code -Dtest=org.unicode.cldr.unittest.TestShim -Dorg.unicode.cldr.unittest.testArgs='-e10 -n -filter:TestCheckNew'
```

TestCheckNew will fail, since the messages need updating. Copy the messages it produced (like "In
CLDR 48.0 the English value for this field changed from “Saurashtra” to “Sourashtra”, but the corresponding value for your locale didn't change.") into TestCheckCLDR.TestCheckNew. Tests should now pass.

Another way to choose appropriate paths for TestCheckNew, if cldr-staging/births is up to date, is to look in the log file such as in https://github.com/unicode-org/cldr-staging/blob/main/births/41.0/fr.txt (for the current version, not necessarily 41.0) for a reasonable locale (not necessarily fr). Find a path that is outdated. To work on both limited and full submissions, choose one with English = trunk. Sometimes the English change is suppressed in a limited release if the change is small. Pick another in that case. Check the data files to ensure that it is in fact outdated. Change the path to that value. The 3rd parameter is the message displayed to the user, or "" if not 'English Changed'. So the first group of tests are for items that should not be outdated, and the second group is ones that should be outdated.

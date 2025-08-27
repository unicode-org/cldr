---
title: Coverage Levels
---

# Coverage Levels

There are four main coverage levels as defined in the [UTS \#35: Unicode Locale Data Markup Language (LDML) Part 6: Supplemental: 8 Coverage Levels](https://www.unicode.org/reports/tr35/tr35-info.html#Coverage_Levels). They are described more fully below.

## Usage

You can use the file **common/properties/coverageLevels.txt** (added in v41\) for a given release to filter the locales that they support. For example, see [coverageLevels.txt](https://github.com/unicode-org/cldr/blob/main/common/properties/coverageLevels.txt). (This and other links to data files are to the development versions; see the specific version for the release you are working with.) For a detailed chart of the coverage levels, see the [locale\_coverage.html](https://www.unicode.org/cldr/charts/latest/supplemental/locale_coverage.html) file for the respective release.

The file format is semicolon delimited, with 3 fields per line.


```Locale ID ; Coverage Level ; Name```

Each locale ID also covers all the locales that inherit from it. So to get locales at a desired coverage level or above, the following process is used.

1. Always include the root locale file, **root.xml**
2. Include all of the locale files listed in **coverageLevels.txt** at that level or above.
3. Recursively include all other files that inherit from the files in \#2\.
	- **Warning**: Inheritance is not simple truncation; the **parentLocale** information in [supplementalData.xml](https://github.com/unicode-org/cldr/blob/main/common/supplemental/supplementalData.xml) needs to be applied also. See [Parent\_Locales](https://www.unicode.org/reports/tr35/tr35.html#Parent_Locales).
	- For example, if you include fr.xml in \#2, you would also include fr\_CA.xml; if you include no.xml in \#2 you would also include nn.xml.

### Filtering

To filter "at that level or above", you use the fact that basic ⊂ moderate ⊂ modern, so 

1. to filter for basic and above, filter for basic\|moderate\|modern
2. to filter for moderate and above, filter for moderate\|modern

### Migration

As of v43, the files in **/seed/** have been moved to **/common/**. Older versions of CLDR separated some locale files into a 'seed' directory. Some implementations used for filtering, but the criteria for moving from seed to common were not rigorous. To maintain compatibility with the set of locales used from previous versions, an implementation may use the above process for Basic and above, but then also add locales that were previously included. For more information, see [CLDR 43 Release Note](/downloads/cldr-43). 

## Core Data

**The data needed for a new locale to be added. See [Core Data for New Locales](/index/cldr-spec/core-data-for-new-locales) for details on Core Data and how to submit for new locales.**

**It is expected that during the next Survey Tool cycle after a new locale is added, the data for the Basic Coverage Level will be supplied.**

## Basic Data

**Suitable for locale selection and minimal support, eg. choice of language on mobile phone**

This includes very minimal data for support of the language: basic dates, times, autonyms:

1. Delimiter Data —Quotation start/end, including alternates
2. Numbering system — default numbering system \+ native numbering system (if default \= Latin and native ≠ Latin)
3. Locale Pattern Info — Locale pattern and separator, and code pattern
4. Language Names — in the native language for the native language and for English
5. Script Name(s) — Scripts customarily used to write the language
6. Country Name(s) — For countries where commonly used (see "Core XML Data")
7. Measurement System — metric vs UK vs US
8. Full Month and Day of Week names
9. AM/PM period names
10. Date and Time formats
11. Date/Time interval patterns — fallback
12. Timezone baseline formats — region, gmt, gmt\-zero, hour, fallback
13. Number symbols — decimal and grouping separators; plus, minus, percent sign (for Latin number system, plus native if different)
14. Number patterns — decimal, currency, percent, scientific

## Moderate Data

**Suitable for “document content” internationalization, eg. content in a spreadsheet**

Before submitting data above the Basic Level, the following must be in place:

1. Plural and Ordinal rules
	- As in \[supplemental/plurals.xml] and \[supplemental/ordinals.xml]
	- Must also include minimal pairs
	- For more information, see [cldr\-spec/plural\-rules](/index/cldr-spec/plural-rules).
2. Casing information (only where the language uses a cased scripts according to [ScriptMetadata.txt](https://github.com/unicode-org/cldr/blob/main/common/properties/scriptMetadata.txt))
	- This will go into [common/casing](https://github.com/unicode-org/cldr/blob/main/common/casing/)
3. Collation rules \[non\-Survey Tool]
	- This can be supplied as a list of characters, or as rule file.
	- The list is a space\-delimited list of the characters used by the language (in the given script). The list may include multiple\-character strings, where those are treated specially. For example, if "ch" is sorted after "h" one might see "a b c d .. g h ch i j ..."
	- More sophisticated users can do a better job, supplying a file of rules as in [cldr\-spec/collation\-guidelines](/index/cldr-spec/collation-guidelines).
4. The result will be a file like: [common/collation/ar.xml](https://github.com/unicode-org/cldr/blob/main/common/collation/ar.xml) or [common/collation/da.xml](https://github.com/unicode-org/cldr/blob/main/common/collation/da.xml).

The data for the Moderate Level includes subsets of the Modern data, both in depth and breadth.

## Modern Data

**Suitable for full UI internationalization**

Before submitting data at the Moderate Level, the following must be in place:

1. Grammatical Features
	1. The grammatical cases and other information, as in [supplemental/grammaticalFeatures.xml](https://github.com/unicode-org/cldr/blob/main/common/supplemental/grammaticalFeatures.xml)
	2. Must include minimal pair values.
2. Romanization table (non\-Latin scripts only)
	1. This can be supplied as a spreadsheet or as a rule file.
	2. If a spreadsheet, for each letter (or sequence) in the exemplars, what is the corresponding Latin letter (or sequence).
	3. More sophisticated users can do a better job, supplying a file of rules like [transforms/Arabic\-Latin\-BGN.xml](https://github.com/unicode-org/cldr/blob/main/common/transforms/Arabic-Latin-BGN.xml).

The data for the Modern Level includes:

**\#\#\# TBD**

## References

For the coverage in the latest released version of CLDR, see [Locale Coverage Chart](https://www.unicode.org/cldr/charts/latest/supplemental/locale_coverage.html).

To see the development version of the rules used to determine coverage, see [coverageLevels.xml](https://github.com/unicode-org/cldr/blob/main/common/supplemental/coverageLevels.xml). For a list of the locales at a given level, see [coverageLevels.txt](https://github.com/unicode-org/cldr/blob/main/common/properties/coverageLevels.txt). 


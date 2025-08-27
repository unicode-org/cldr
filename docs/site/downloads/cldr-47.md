---
title: CLDR 47 Release Note
date: 2025-03-13
tag: release-47
json: 47.0.0
---

| No. |    Date    | Rel. Note |  Data  |  Charts  | Spec |   Delta  | GitHub Tag | Delta DTD | CLDR JSON |
|:---:|:----------:|:---------:|:------:|:--------:|:------------:|:---:|:----------:|:---------:|:---------:|
|  47 | 2025-03-13 | [v47][] | [CLDR47][] | [Charts47][] | [LDML47][] | [Δ47][] | [release-47][] | [ΔDtd47] | [47.0.0][] |

## Overview

Unicode CLDR provides key building blocks for software supporting the world's languages.
CLDR data is used by all [major software systems](/index#who-uses-cldr)
(including all mobile phones) for their software internationalization and localization,
adapting software to the conventions of different languages.

CLDR 47 focused on MessageFormat 2.0 and tooling for an expansion of DDL support.
It was a closed cycle: locale data changes were limited to bug fixes and the addition of new locales, mostly regional variants.

### Changes

This release note includes all changes from [CLDR v46.1](/downloads/cldr-46#461-changes), 
a special release, which many users of CLDR (including ICU) have not updated to.

The most significant changes in this release are the following:

   - MessageFormat has advanced from Final Candidate to Stable. For details, see below.
   - New locales:
      - Core data for Coptic (cop), Haitian Creole (ht)
      - Locale data for 11 English locales and Cantonese (Macau) (yue_Hant_MO)
   - Updated time zone data to tzdata 2025a
   - [RBNF](#number-spellout-data-changes) (Rule Based Number Formatting): Number spellout data improvements for multiple languages
   - Assorted transforms improvements
   - Updated and revised population data
   - Addition of derived emoji annotations that were missing: emoji with skin tones facing right
   - Fixes to make the ja, ko, yue, zh datetimeSkeletons useful for generating the standard patterns
   - Improved date/time test data

For more details, see below.

### Locale Coverage Status

Count | Level | Usage | Examples
-- | -- | -- | --
97 | Modern | Suitable for full UI internationalization | čeština, Ελληνικά‎, Беларуская‎, ‎ᏣᎳᎩ‎, Ქართული‎, ‎Հայերեն‎, ‎עברית‎, ‎اردو‎, አማርኛ‎, ‎नेपाली‎, অসমীয়া‎, ‎বাংলা‎, ‎ਪੰਜਾਬੀ‎, ‎ગુજરાતી‎, ‎ଓଡ଼ିଆ‎, தமிழ்‎, ‎తెలుగు‎, ‎ಕನ್ನಡ‎, ‎മലയാളം‎, ‎සිංහල‎, ‎ไทย‎, ‎ລາວ‎, မြန်မာ‎, ‎ខ្មែរ‎, ‎한국어‎, 中文, 日本語‎, … ‎|
16 | Moderate | Suitable for “document content” internationalization, eg. in spreadsheet | Akan, Balóchi [Látin], brezhoneg, Cebuano, føroyskt, IsiXhosa, Māori, sardu, veneto, Wolof, татар, тоҷикӣ, कांगड़ी‎, … |
55 | Basic | Suitable for locale selection, eg. choice of language on mobile phone | Basa Sunda, emakhuwa, Esperanto, eʋegbe, Frysk, Malti, босански (ћирилица), କୁୱି (ଅଡ଼ିଆ), కువి (తెలుగు), ᱥᱟᱱᱛᱟᱲᱤ, ᓀᐦᐃᓇᐍᐏᐣ‬, ꆈꌠꉙ‎, … |

**TBD: update the above**

\* Note: Each release, the number of items needed for Modern and Moderate increases. So locales without active contributors may drop down in coverage level.

For a full listing, see [Coverage Levels](https://unicode.org/cldr/charts/47/supplemental/locale_coverage.html)

## MessageFormat 2.0 now Stable

Software needs to construct messages that incorporate various pieces of information.
The complexities of the world's languages make this challenging. 
MessageFormat 2.0 enables developers and translators to create natural-sounding user interfaces
that can appear in any language and support the needs of various cultures.

The new MessageFormat defines the data model, syntax, processing, and conformance requirements
for the next generation of dynamic messages. 
It is intended for adoption by programming languages, software libraries, and software localization tooling. 
It enables the integration of internationalization APIs (such as date or number formats) and grammatical matching (such as plurals or genders). 
It is extensible, allowing software developers to create formatting or message selection logic that add on to the core capabilities. 
Its data model provides the means of representing existing syntaxes, thus enabling gradual adoption by users of older formatting systems.

Tech Preview implementations are available in C++, Java, and JavaScript:

* **ICU4J, Java:** [com.ibm.icu.message2](https://unicode-org.github.io/icu-docs/apidoc/dev/icu4j/index.html?com/ibm/icu/message2/package-summary.html), part of ICU 76, is a tech preview implementation of the MessageFormat 2.0, together with a formatting API. See the [ICU User Guide](https://unicode-org.github.io/icu/userguide/format_parse/messages/mf2.html) for examples and a quickstart guide, and [Trying MF 2.0 Final Candidate](https://unicode-org.github.io/icu/userguide/format_parse/messages/try-mf2.html) to try a “Hello World”.  
* **ICU4C, C++:** [icu::message2::MessageFormatter](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/classicu_1_1message2_1_1MessageFormatter.html), part of ICU 76, is a tech preview implementation of MessageFormat 2.0, together with a formatting API. See the [ICU User Guide](https://unicode-org.github.io/icu/userguide/format_parse/messages/mf2.html) for examples and a quickstart guide, and [Trying MF 2.0 Final Candidate](https://unicode-org.github.io/icu/userguide/format_parse/messages/try-mf2.html) to try a “Hello World”.  
* **Javascript:** [messageformat](https://github.com/messageformat/messageformat/tree/main/mf2/messageformat) 4.0 provides a formatter and conversion tools for the MessageFormat 2 syntax, together with a polyfill of the runtime API proposed for ECMA-402.

(Because of the timing, these implement a slightly earlier version of the spec, but can be used for initial evaluation, testing, and experimentation.)

## Specification Changes

The following is a summary of the most significant changes to the specification (LDML) since CLDR 46.
For more information, see [LDML 47](https://unicode.org/reports/tr35/47/tr35.html#modifications).

- Many changes to MessageFormat, including:
    - Now stable, with the stability policy now normative.
    - Added and clarified terminology
    - Modified portions of the syntax (ABNF)
    - Revised the Default Bidi Strategy, and added support for bidirectional isolates and marks
    - Enabled functions to know whether an option value was set using a literal or a variable, which is necessary for some function's selection mechanism (see below).
    - Updated the default functions
        - Only three default functions are stable: :string, :number, and :integer. Other functions are Draft.
- Added a Time Precision option to Semantic Datetime Skeletons, replacing discrete time fields
- Significant changes in Timezones, Unit Identifiers, Keyboard Transforms, Currency elements, and others.
- Moved @MATCH documentation to the site (it is for internal tooling)

There are many more changes that are important to implementations, such as changes to certain identifier syntax and various algorithms.
See the [Modifications section](https://www.unicode.org/reports/tr35/47/tr35.html#Modifications) of the specification for details.

## Data Changes

### DTD Changes
Changes include:
- Ordering currencies used in each region
- [CLDR-17531][] Adding script data (for example, relating the script code `Jpan` to its component script codes)
- Adding the attribution attribute to keyboards
- Most of the DTD changes were to tighten up the validity constraints (@MATCH) on various attributes.
These don't affect implementations.

For a full listing, see [Delta DTDs][ΔDtd47].

Also see other changes listed under [keyboards](#keyboard-changes).

### Supplemental Data Changes

- Ordered scripts in `<languageData>` in descending order of usage per locale [CLDR-18155][]
- Updated language matching for Afrikaans to English (en) from Dutch (nl) [CLDR-18198][]
- Fixed invalid codes [CLDR-18129][]
- Updated various language population numbers for [Bouvet Island](https://unicode-org.atlassian.net/browse/CLDR-9791), [Canada](https://unicode-org.atlassian.net/browse/CLDR-15391), [El Salvador](https://unicode-org.atlassian.net/browse/CLDR-11567), [Macau](https://unicode-org.atlassian.net/browse/CLDR-10478), [Mauritius](https://unicode-org.atlassian.net/browse/CLDR-18002), [Sierra Leone](https://unicode-org.atlassian.net/browse/CLDR-18002), [South Georgia and Bouvet Islands](https://unicode-org.atlassian.net/browse/CLDR-9791), [Tokelau](https://unicode-org.atlassian.net/browse/CLDR-18002), and [Zambia](https://unicode-org.atlassian.net/browse/CLDR-18002). Also for [French across the world](https://unicode-org.atlassian.net/browse/CLDR-11888).
- Fixed [matching languages to likely subtags](https://www.unicode.org/cldr/charts/47/supplemental/likely_subtags.html) for many languages in the above countries as well as: [Malay (individual language)](https://unicode-org.atlassian.net/browse/CLDR-10015), [Serbian in Russia](https://unicode-org.atlassian.net/browse/CLDR-14088), and [Sudanese Arabic](https://unicode-org.atlassian.net/browse/CLDR-10015).
- Added script metadata [CLDR-17531][]
- **TBD: add others**

For a full listing, see [¤¤BCP47 Delta](https://unicode.org/cldr/charts/47/delta/bcp47.html) and [¤¤Supplemental Delta](https://unicode.org/cldr/charts/47/delta/supplemental-data.html)

### Locale Data Changes

- Updated language matching for Afrikaans to English (en) from Dutch (nl) [CLDR-18198][]
- Ordered scripts in `<languageData>` in descending order of usage per locale [CLDR-18155][]
- Fixed certain invalid codes [CLDR-18129][]
- Cleanups for current pattern variants `alt="alphaNextToNumber"` and `alt="noCurrency"`:
    - These were introduced in CLDR 42
(per [CLDR-14336][]) to provide a cleaner way of adjusting currency
patterns when an alphabetic currency symbol is used, or when a currency-style pattern is desired without a currency symbol
(as for use in a table). Gaps in the data coverage showed up, because the translators weren't shown the right values. 
Fixes were made in  [CLDR-17879][].
- Fixed missing `numberSystem` attributes
    - As noted below in [Migration](#migration), number `<symbols>` elements and format elements (`<currencyFormats>`, `<decimalFormats>`, `<percentFormats>`, `<scientificFormats>`)
should all have a `numberSystem` attribute, and such elements without a `numberSystem` attribute will be deprecated in CLDR 48. To
prepare for this, in CLDR 47, all such elements were either removed (if redundant) or correct by adding a `numberSystem` attribute.
([CLDR-17760][])

For a full listing, see [Delta Data](https://unicode.org/cldr/charts/47/delta/index.html)

### Collation Data Changes

- Two old `zh` collation variants are removed: big5han and gb2312.
They are no longer typically used, and only cover a fraction of the CJK ideographs.
([CLDR-16062][])

### Number Spellout Data Changes

- Number spellout rules are added for Gujarati. ([CLDR-18111][])
- Number spellout rules are improved for several other languages:
    - Bulgarian: Improved usage of ‘и’ (“and”). ([CLDR-17818][])
    - Catalan: Added plural ordinal rules for both masculine and feminine, other fixes. ([CLDR-15972][])
    - Dutch: Added the alternate spellout-cardinal-stressed rule for specific Dutch scenarios. ([CLDR-17187][])
    - Hindi: Added the spellout-ordinal-masculine-oblique rule. ([CLDR-15278][])
    - Indonesian: Added missing semicolon that caused all ordinals to be prefixed with “pertama 2:”. ([CLDR-17730][])
    - Lithuanian: Added all of the grammatical cases, genders and grammatical numbers for cardinals and ordinals (no pronominal forms, and only the positive degree). ([CLDR-18110][])
    - Russian: Fixed grammatical case names in rules, and other issues. ([CLDR-17386][])
    - Ukrainian: Added digits-ordinal rules. ([CLDR-16096][])

### Segmentation Data Changes

- The word break tailorings for `fi` and `sv` are removed to align with recent changes to the root collation
and recent changes to ICU behavior. ([CLDR-18272][]

### Transform Data Changes

- A new `Hant-Latn` transform is added, and `Hans-Latn` is added as an alias for the existing `Hani-Latn` transform.
When the Unihan data `kMandarin` field has two values,
the first is preferred for a `CN`/`Hans` context, and is used by the `Hani-Latn`/`Hans-Latn` transform;
the second is preferred for a `TW`/`Hant` context, and is now used by the new `Hant-Latn` transform.
([CLDR-18080][])

### JSON Data Changes

- [CLDR-11874][] New package with subdivision data (Note: see [known issues](#known-issues)).
- [CLDR-17176][] Timezone data now has a `_type: "zone"` attribute indicating which objects are leaf timezones (`America/Argentina/Catamarca` is a timezone, `America/Argentina` is not.)
- [CLDR-18133][] Currency data preserves the priority order (highest to lowest) of preferred currencies. This was an error in the DTD.
- [CLDR-18277][] Package name for transforms was incorrectly generated.
- [CLDR-10397][] `languageMatching.json` was restructured and reformatted, as it was previously unusable due to critical missing data
- [CLDR-17531][] `scriptMetadata.json` is a new file in cldr-core, with metadata on varaints of ISO 15924 scripts.

### File Changes

In v47.0, but not 46.0:

* common/
    * main/
        * cop.xml, cop_EG.xml, en_CZ.xml, en_ES.xml, en_FR.xml, en_GS.xml, en_HU.xml, en_IT.xml, en_NO.xml, en_PL.xml, en_PT.xml, en_RO.xml, en_SK.xml, ht.xml, ht_HT.xml, yue_Hant_MO.xml
    * rbnf/
        * gu.xml
    * testData/
        * messageFormat/
            * tests/
                * bidi.json, fallback.json
                * functions/
                    * currency.json, math.json
              * u-options.json
        * transforms/
            * und-Latn-t-und-hans.txt, und-Latn-t-und-hant.txt, Hant-Latin.xml
keyboards/
    * abnf/
        * transform-from-required.abnf, transform-to-required.abnf

In 46.0, but not in 47.0:

* common/
    * segments/
        * fi.xml, sv.xml

### Tooling Changes

There were various SurveyTool improvements targeting expansion of DDL support and error detection, such as the following:

   - Added a CLA check [CLDR-17612][]
   - Improved validity checks for codes [CLDR-18129][]
   - Improved ability to detect invalid URLs in the site and spec [CLDR-16526][]

### Keyboard Changes

> **Note**: for the v48 timeframe, additional processes are being developed for broad intake of keyboards.

Spec and DTD changes:

- [CLDR-16836][] [CLDR-18318][] [CLDR-18319][] Added EBNF for keyboard transform format to the spec, and ABNF data files.
  This provides rigorous definition of the allowed keyboard transform format, as well as programmatic validation of the keyboard transform format.
- [CLDR-17971][] Added `<info attribution=…` attribute
- [CLDR-18324][] Added `<keyboard3 draft=…` attribute

## Migration

- Removal of number data without `numberSystem` attributes.
    - Number `<symbols>` elements and format elements (`<currencyFormats>`, `<decimalFormats>`, `<percentFormats>`, `<scientificFormats>`)
should all have a `numberSystem` attribute. In CLDR v48 such elements without a `numberSystem` attribute will be deprecated, and the
corresponding entries in root will be removed; these were only intended as a long-ago migration aid. See the relevant sections of the
LDML specification: [Number Symbols](https://www.unicode.org/reports/tr35/47/tr35-numbers.html#Number_Symbols) and
[Number Formats](https://www.unicode.org/reports/tr35/47/tr35-numbers.html#number-formats).

### V48 advance warnings
The following changes are planned for CLDR 48. Please plan accordingly to avoid disruption.

- Any locales that are missing Core data by the end of the CLDR 48 cycle will be removed [CLDR-16004][]
- The default week numbering will change to ISO instead being based on the calendar week starting in CLDR 48 [CLDR-18275][]. The calendar week data will be more clearly targeted at matching usage in displayed month calendars.
- The likely language for Belarus is slated to change to Russian [CLDR-14479][]
- The major components in [supplementalData.xml](https://github.com/unicode-org/cldr/blob/main/common/supplemental/supplementalData.xml) and [supplementalMetadata.xml](https://github.com/unicode-org/cldr/blob/main/common/supplemental/supplementalMetadata.xml) files are slated to be organized more logically and moved into separate files.
    - This will make it easier for implementations to filter out data that they don't need, and make internal maintenance easier. This will not affect the data: just which file it is located in. Please plan to update XML and JSON parsers accordingly.
- The `languageData` element and/or some of its attributes may be deprecated, and their data removed. [CLDR-18087][]
- The `<version number="$Revision$">` may be deprecated and data removed. The #FIXED values in the DTD may also be retired in favor of explicit values. [CLDR-18377][], [CLDR-14161][]

## Known Issues

- [CLDR-18219][] `common/subdivisions` data files contained additional values that should not be present. These will be removed in the future, but note that they may be present in the new [JSON data](#json-data-changes):
  - Non-subdivisions such as `AW`:  Use the region code `AW` instead for translation.
  - Overlong subdivisions such as `fi01`: Use the region code `AX` instead for translation.
- [CLDR-18407][] Unexpected time formatting patterns in Assamese, reverted in [CLDR-18415][] and will be fixed once day period data is validated.
- [CLDR-18367][] The wide formatted value for Sunday was reverted in Finnish. More investigation will need to be done on the datetime report in [CLDR-18441][]

## Acknowledgments

Many people have made significant contributions to CLDR and LDML;
see the [Acknowledgments](/index/acknowledgments) page for a full listing.

The Unicode [Terms of Use](https://unicode.org/copyright.html) apply to CLDR data;
in particular, see [Exhibit 1](https://unicode.org/copyright.html#Exhibit1).

For web pages with different views of CLDR data, see [charts](/index/charts).

[CLDR-10397]: https://unicode-org.atlassian.net/browse/CLDR-10397
[CLDR-11874]: https://unicode-org.atlassian.net/browse/CLDR-11874
[CLDR-14161]: https://unicode-org.atlassian.net/browse/CLDR-14161
[CLDR-14336]: https://unicode-org.atlassian.net/browse/CLDR-14336
[CLDR-14479]: https://unicode-org.atlassian.net/browse/CLDR-14479
[CLDR-15278]: https://unicode-org.atlassian.net/browse/CLDR-15278
[CLDR-15972]: https://unicode-org.atlassian.net/browse/CLDR-15972
[CLDR-16004]: https://unicode-org.atlassian.net/browse/CLDR-16004
[CLDR-16062]: https://unicode-org.atlassian.net/browse/CLDR-16062
[CLDR-16096]: https://unicode-org.atlassian.net/browse/CLDR-16096
[CLDR-16526]: https://unicode-org.atlassian.net/browse/CLDR-16526
[CLDR-16836]: https://unicode-org.atlassian.net/browse/CLDR-16836
[CLDR-17176]: https://unicode-org.atlassian.net/browse/CLDR-17176
[CLDR-17187]: https://unicode-org.atlassian.net/browse/CLDR-17187
[CLDR-17386]: https://unicode-org.atlassian.net/browse/CLDR-17386
[CLDR-17531]: https://unicode-org.atlassian.net/browse/CLDR-17531
[CLDR-17612]: https://unicode-org.atlassian.net/browse/CLDR-17612
[CLDR-17730]: https://unicode-org.atlassian.net/browse/CLDR-17730
[CLDR-17760]: https://unicode-org.atlassian.net/browse/CLDR-17760
[CLDR-17818]: https://unicode-org.atlassian.net/browse/CLDR-17818
[CLDR-17879]: https://unicode-org.atlassian.net/browse/CLDR-17879
[CLDR-17971]: https://unicode-org.atlassian.net/browse/CLDR-17971
[CLDR-18080]: https://unicode-org.atlassian.net/browse/CLDR-18080
[CLDR-18087]: https://unicode-org.atlassian.net/issues/CLDR-18087
[CLDR-18110]: https://unicode-org.atlassian.net/browse/CLDR-18110
[CLDR-18111]: https://unicode-org.atlassian.net/browse/CLDR-18111
[CLDR-18129]: https://unicode-org.atlassian.net/browse/CLDR-18129
[CLDR-18133]: https://unicode-org.atlassian.net/browse/CLDR-18133
[CLDR-18155]: https://unicode-org.atlassian.net/browse/CLDR-18155
[CLDR-18198]: https://unicode-org.atlassian.net/browse/CLDR-18198
[CLDR-18219]: https://unicode-org.atlassian.net/browse/CLDR-18219
[CLDR-18272]: https://unicode-org.atlassian.net/browse/CLDR-18272
[CLDR-18275]: https://unicode-org.atlassian.net/browse/CLDR-18275
[CLDR-18277]: https://unicode-org.atlassian.net/browse/CLDR-18277
[CLDR-18318]: https://unicode-org.atlassian.net/browse/CLDR-18318
[CLDR-18319]: https://unicode-org.atlassian.net/browse/CLDR-18319
[CLDR-18324]: https://unicode-org.atlassian.net/browse/CLDR-18324
[CLDR-18367]: https://unicode-org.atlassian.net/browse/CLDR-18367
[CLDR-18377]: https://unicode-org.atlassian.net/browse/CLDR-18377
[CLDR-18407]: https://unicode-org.atlassian.net/browse/CLDR-18407
[CLDR-18415]: https://unicode-org.atlassian.net/browse/CLDR-18415
[CLDR-18441]: https://unicode-org.atlassian.net/browse/CLDR-18441

<!-- CLDR 47 - 2025-03-13 - Release links-->
[CLDR47]: https://unicode.org/Public/cldr/47/
[Charts47]: https://unicode.org/cldr/charts/47
[LDML47]: https://www.unicode.org/reports/tr35/47
[Δ47]: https://unicode-org.atlassian.net/issues/?filter=10836
[release-47]: https://github.com/unicode-org/cldr/releases/tag/release-47
[ΔDtd47]: https://www.unicode.org/cldr/charts/47/supplemental/dtd_deltas.html
[47.0.0]: https://github.com/unicode-org/cldr-json/releases/tag/47.0.0
[v47]: /downloads/cldr-47

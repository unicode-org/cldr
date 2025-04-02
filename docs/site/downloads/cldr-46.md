---
title: CLDR 46 Release Note
---

# CLDR 46 Release Note

| No. |    Date    | Rel. Note |  Data  |  Charts  | Spec |   Delta  | GitHub Tag | Delta DTD | CLDR JSON |
|:---:|:----------:|:---------:|:------:|:--------:|:------------:|:---:|:----------:|:---------:|:---------:|
|  46 | 2024-10-24 | [v46][] | [CLDR46][] | [Charts46][] | [LDML46][] | [Δ46][]| [release-46][] | [ΔDtd46][] | [46.0.0][] |
| 46.1 | 2024-12-18 | [v46.1][] | n/a | [Charts46.1][]| [LDML46.1][]| [Δ46.1][] | [release-46-1][] | [ΔDtd46.1][] | [46.1.0][] |

## Overview

Unicode CLDR provides key building blocks for software supporting the world's languages. CLDR data is used by all [major software systems](/index#who-uses-cldr) (including all mobile phones) for their software internationalization and localization, adapting software to the conventions of different languages.

The most significant changes in this release were:

- Updated to Unicode 16.0 (including major changes to collation)
- Further revisions to the Message Format 2.0 tech preview
- Substantial additions and modifications of Emoji search keyword data
- ‘Upleveling’ the locale coverage
- Important changes to the [specification](#specification-changes)

For more details, see below.

### Locale Coverage Status
#### Current Levels

Count | Level | Usage | Examples
-- | -- | -- | --
97 | Modern | Suitable for full UI internationalization | čeština, Ελληνικά‎, Беларуская‎, ‎ᏣᎳᎩ‎, Ქართული‎, ‎Հայերեն‎, ‎עברית‎, ‎اردو‎, አማርኛ‎, ‎नेपाली‎, অসমীয়া‎, ‎বাংলা‎, ‎ਪੰਜਾਬੀ‎, ‎ગુજરાતી‎, ‎ଓଡ଼ିଆ‎, தமிழ்‎, ‎తెలుగు‎, ‎ಕನ್ನಡ‎, ‎മലയാളം‎, ‎සිංහල‎, ‎ไทย‎, ‎ລາວ‎, မြန်မာ‎, ‎ខ្មែរ‎, ‎한국어‎, 中文, 日本語‎, … ‎
16 | Moderate | Suitable for “document content” internationalization, e.g. in spreadsheets | Akan, Balóchi [Látin], brezhoneg, Cebuano, føroyskt, IsiXhosa, Māori, sardu, veneto, Wolof, татар, тоҷикӣ, कांगड़ी‎, …
55 | Basic | Suitable for locale selection, e.g. choice of language on mobile phone | Basa Sunda, emakhuwa, Esperanto, eʋegbe, Frysk, Malti, босански (ћирилица), କୁୱି (ଅଡ଼ିଆ), కువి (తెలుగు), ᱥᱟᱱᱛᱟᱲᱤ, ᓀᐦᐃᓇᐍᐏᐣ‬, ꆈꌠꉙ‎, …

#### Changes

| ± | New Level | Locales |
| -- | -- | -- |
| 📈 | Modern | Nigerian Pidgin, Tigrinya |
| 📈 | Moderate | Akan, Baluchi (Latin), Kangri, Tajik, Tatar, Wolof |
| 📈 | Basic | Ewe, Ga, Kinyarwanda, Konkani (Latin), Northern Sotho, Oromo, Sichuan Yi, Southern Sotho, Tswana |
| 📉 | Basic* | Chuvash, Anii |

\* Note: The number of items needed for Modern and Moderate increases every release. Therefore, locales without active contributors may drop in coverage level.

For a full listing, see [Coverage Levels](https://unicode.org/cldr/charts/46/supplemental/locale_coverage.html).


## Specification Changes

The following are the most significant changes to the [specification (LDML)](https://www.unicode.org/reports/tr35/tr35-73/tr35.html):

1. [Message Format](https://www.unicode.org/reports/tr35/tr35-73/tr35-messageFormat.html#Contents) — see a summary [below](#message-format-specification)
2. [LDML Conformance](https://www.unicode.org/reports/tr35/tr35-73/tr35.html#Conformance)
3. [Emoji search keywords](https://www.unicode.org/reports/tr35/tr35-73/tr35-general.html#Annotations) in tech preview — see a summary [below](#emoji-search-keywords)
4. [Semantic skeletons](https://www.unicode.org/reports/tr35/tr35-73/tr35-dates.html#Semantic_Skeletons)
5. [Grouping classes of characters](https://www.unicode.org/reports/tr35/tr35-73/tr35-collation.html#grouping_classes_of_characters) and other collation changes — see a summary [below](#collation-data-changes)

There are many more changes that are important to implementations, such as changes to certain identifier syntax and various algorithms.
See the [Modifications section](https://www.unicode.org/reports/tr35/tr35-73/tr35.html#Modifications) of the specification for details.

## Data Changes

### DTD Changes

1. Added `alt='official'` to represent cases where an official value differs from the customary value. Currently added for a small number of language names, decimal separators, and grouping separators.
2. Added new numbering systems from Unicode 16.0.

For a full listing, see [Delta DTDs](https://unicode.org/cldr/charts/46/supplemental/dtd_deltas.html).

### Supplemental Data Changes

1. Currency
    - New currency code `ZWG` added — because it was late in the cycle, many locales will just support the code (no symbol or name).
2. Dates & Times
    - Added a new calendar type, `iso8601`.
This is not the same as the [ISO 8601](https://en.wikipedia.org/wiki/ISO_8601) standard format, which is designed just for data interchange:
it is all ASCII, does not have all the options for fields (like "Sunday", "BC", or "AM"), and does not contain spaces.
The CLDR `iso8601` calendar uses patterns in the order: era, year, month, day, day-of-week, hour, minute, second, day-period, timezone.
	- Changed the metazone for Kazakhstan to reflect removal of Asia/Almaty, thus dropping the distinction among different regions in Kazakhstan.
    - Added support for deprecated timezone codes by remapping: `CST6CDT → America/Chicago`, `EST → America/Panama`, `EST5EDT → America/New_York`, `MST7MDT → America/Denver`, `PST8PDT → America/Los_Angeles`.
3. Units
    - Added units: `portion-per-1e9` (aka per-billion), `night` (for hotel stays), `light-speed` (as an internal prefix for **light-second**, **light-minute**, etc.)
    - Changed preferred wind speed preference for some locales to `meter-per-second`.
More preference changes are planned for the next release.
4. Minimization for likelySubtags removes many additional redundant mappings.
    - For example, the mapping `acy_Grek → acy_Grek_CY` is unnecessary, because the mapping `acy → acy_Latn_CY` is sufficient.
For the reason why, see the algorithm in [Likely Subtags](https://www.unicode.org/reports/tr35/tr35-73/tr35.html#likely-subtags).
    - The ordering in the file is more consistent: first the main mappings, then the mapping from region and/or script to likely language, then the data contributed by SIL.
    - The regions have been cleaned up: there are no entries with `ZZ`, and `001` is limited to artifical languages such as Interlingua. The only other macroregion code is in `und_419 → es_Latn_419` (Spanish‧Latin‧Latin America)
5. Macrolanguage mapping / locale canonicalization
   - Parent and defaultContent mappings have been added for Kara-Kalpak (`kaa`) and Konkani (`kok`); defaultContent mappings have been added for Kazakh (`kk`), Ladin (`lld`), Latgalian (`ltg`), Mócheno (`mhn`), and Chinese (Latin, China) (`zh_Latn_CN`).
   - The predominant language encompassed by "kok" (Konkani macrolanguage) has been changed from "knn" (Konkani / individual language) to "gom" (Goan Konkani) in [CLDR-17121][]
     - The TC found that the predominant encompassed language is "gom" according to local governments and also industry practice; and the CLDR data in the "kok" locale has really been "gom" not "knn".
     - As a result, "knn" no longer canonicalizes to "kok"; instead, "gom" now canonicalizes to "kok".
     - CLDR follows long-standing industry practice in using a macrolanguage subtag instead of the predominant encompassed language. Other examples include the use of "zh" for Mandarin ("cmn") and the use of "ar" for Standard Arabic ("arb").
6. Language matching
    - Dropped the fallback mapping `desired="uk" → supported="ru"` (so that Ukrainian (`uk`) doesn't fall back to Russian (`ru`)).
        - Note: A fallback language is used when the user's primary language is unavailable,
and either the user does not have a secondary language in their settings (as on Android or iOS) or the secondary languages are also not available.
As a result of this change, when the primary and secondary languages are not available, the fallback language for Ukrainian would be the system default instead of Russian.
    - Added the mapping `desired="scn" → supported="it"` (Sicilian → Italian).
    - Changed the mapping `gom` → `kok` to `knn` → `kok` (Konkani); see also the Macrolanguage mapping change above.
7. Transforms
    - Major update to `Han → Latn`, reflecting new data in Unicode 16.0
    - Fixes for Arabic numbers and a Farsi vowel
8. Other Unicode 16.0 changes
    - Additional numbering systems
    - Additional scripts and script identifiers
    - ScriptMeta has been expanded for Unicode 16.0
9. Other updates
    - The subdivision identifiers have been updated to the latest available from ISO.
        - The removed identifiers have been deprecated.
        - Missing names have been added (from Wikidata).
    - The language subtags, script subtags, and variant subtags have been updated to the latest from IANA.
       - Some codes have been deprecated.
    - Territory Info (GDP, population, languages) has been updated from World Bank and other sources.
    - LanguageGroup info has been updated from Wikidata.
    - Plural rules have been added for some new locales.
    - Week data
        - The first day of the week has been changed for `AE`.
        - Hour preferences (12 v 24) have been added for English as used in Hong Kong, Malaysia, and Israel (`en_HK`, `en_MY`, `en_IL`).

For a full listing, see [¤¤BCP47 Delta](https://unicode.org/cldr/charts/46/delta/bcp47.html) and [¤¤Supplemental Delta](https://unicode.org/cldr/charts/46/delta/supplemental-data.html)

### [Locale Changes](https://unicode.org/cldr/charts/46/delta/index.html)

1. Major changes to emoji search keywords and short names ([see below](#emoji-search-keywords))
2. Major changes to Chinese collation, reflecting new data in Unicode 16.0
3. Added iso8601 patterns to root.
These will use localized months, days of the week, day periods, and timezones.
In this first version, the separators are not localized, and will use "-" within numeric dates, ":" within times, and " " or ", " between major elements.
Full localization will await the next submission phase for CLDR.
4. Other changes
    1. Various locales also had smaller improvements agreed to by translators.
    2. Additional test files have been added.

For a full listing, see [Delta Data](https://unicode.org/cldr/charts/46/delta/index.html)

### Message Format Specification
The CLDR Technical Committee decided to continue the tech preview phase for [Message Format](https://www.unicode.org/reports/tr35/tr35-73/tr35-messageFormat.html#Contents) in version 46.
The plan is to have a final version of the specification in a 46.1 release before the end of 2024.

The most significant changes since v45 were:
- Removed all of the reserved and private use syntax constructs, simplifying the grammar.
- Changed the structure of the .match (selector) to require use of local or input declarations.
This is a breaking change for existing v45 messages.
- Added support for bidirectional isolates and marks and clarified whitespace handling,
to better enable messages that contains right-to-left identifiers and text.

Implementers should be aware of the following normative changes after the start of the tech review period.

* [\#885](https://github.com/unicode-org/message-format-wg/issues/885) Address equality of `name` and `literal` values, including requiring keys to use NFC
* [\#884](https://github.com/unicode-org/message-format-wg/issues/884) Add support for bidirectional isolates and strong marks in syntax and address UAX31/UTS55 requirements
* [\#883](https://github.com/unicode-org/message-format-wg/issues/883) Remove forward-compatibility promise and all reserved/private syntax.
* [\#882](https://github.com/unicode-org/message-format-wg/issues/882) Specify `bad-option` error for bad digit size options in `:number` and `:integer` functions
* [\#878](https://github.com/unicode-org/message-format-wg/issues/878) Clarify "rule" selection in `:number` and `:integer` functions
* [\#877](https://github.com/unicode-org/message-format-wg/issues/877) Match on variables instead of expressions.
* [\#854](https://github.com/unicode-org/message-format-wg/issues/854) Allow whitespace at complex message start
* [\#853](https://github.com/unicode-org/message-format-wg/issues/853) Add a "duplicate-variant" error
* [\#845](https://github.com/unicode-org/message-format-wg/issues/845) Define "attributes" feature
* [\#834](https://github.com/unicode-org/message-format-wg/issues/834) Modify the stability policy (not currently in effect due to Tech Preview)
* [\#816](https://github.com/unicode-org/message-format-wg/issues/816) Refine error handling
* [\#815](https://github.com/unicode-org/message-format-wg/issues/815) Removed machine-readable function registry as a deliverable
* [\#813](https://github.com/unicode-org/message-format-wg/issues/813) Change default of `:date` and `:datetime` date formatting from `short` to `medium`
* [\#812](https://github.com/unicode-org/message-format-wg/issues/812) Allow trailing whitespace for complex messages
* [\#793](https://github.com/unicode-org/message-format-wg/issues/793) Recommend the use of escapes only when necessary
* [\#775](https://github.com/unicode-org/message-format-wg/issues/775) Add formal definitions for variable, external variable, and local variable
* [\#774](https://github.com/unicode-org/message-format-wg/issues/774) Refactor errors, adding Message Function Errors
* [\#771](https://github.com/unicode-org/message-format-wg/issues/771) Remove inappropriate normative statement from errors.md
* [\#767](https://github.com/unicode-org/message-format-wg/issues/767) Add a test schema and [\#778](https://github.com/unicode-org/message-format-wg/issues/778) validate tests against it
* [\#775](https://github.com/unicode-org/message-format-wg/issues/775) Add a definition for `variable`
* [\#774](https://github.com/unicode-org/message-format-wg/issues/774) Refactor error types, adding a *Message Function Error* type (and subtypes)
* [\#769](https://github.com/unicode-org/message-format-wg/issues/769) Add `:test:function`, `:test:select` and `:test:format` functions for implementation testing
* [\#743](https://github.com/unicode-org/message-format-wg/issues/743) Collapse all escape sequence rules into one (affects the ABNF)

In addition to the above, the test suite is significantly modified and updated. There will be updated tech preview implementations available in ICU (Java and C++) and in Javascript.

### Emoji Search Keywords
The usage model for emoji search keywords is that:
- The user types one or more words in an emoji search field. The order of words doesn't matter; nor does upper- versus lowercase.
- Each word successively narrows a number of emoji in a results box
    - heart → 🥰 😘 😻 💌 💘 💝 💖 💗 💓 💞 💕 💟 ❣️ 💔 ❤️‍🔥 ❤️‍🩹 ❤️ 🩷 🧡 💛 💚 💙 🩵 💜 🤎 🖤 🩶 🤍 💋 🫰 🫶 🫀 💏 💑 🏠 🏡 ♥️ 🩺
    - blue → 🥶 😰 💙 🩵 🫐 👕 👖 📘 🧿 🔵 🟦 🔷 🔹 🏳️‍⚧️
    - therefore, [heart blue] → 💙 🩵
- A word matches all the words that begin with it; if there are no such matches, it is ignored.
    - [heart blue confabulation] is equivalent to [heart blue]
- Whenever the list is short enough to scan, the user will mouse-click on the right emoji — so it doesn't have to be narrowed too far.
Thus in the following, the user would just click on 🎉 if that works for them.
    - celebrate → 🥳 🥂 🎈 🎉 🎊 🪅

In this release WhatsApp emoji search keyword data has been incorporated. In the process of doing that, the maximum number of search keywords per emoji has been increased, and the keywords have been simplified in most locales by breaking up multi-word keywords. An example would be white flag (🏳️), formerly having 3 keyword phrases of [white waving flag | white flag | waving flag], now being replaced by the simpler 3 single keywords [white | waving | flag]. The simpler version typically works as well or better in practice.

### Collation Data Changes
There are two significant changes to the CLDR root collation (CLDR default sort order).

#### Realigned With DUCET
The [DUCET](https://www.unicode.org/reports/tr10/#Default_Unicode_Collation_Element_Table) is the Unicode Collation Algorithm default sort order. The [CLDR root collation](https://www.unicode.org/reports/tr35/tr35-collation.html#Root_Collation) is a tailoring of the DUCET. These sort orders have differed in the relative order of groups of characters including extenders, currency symbols, and non-decimal-digit numeric characters.

Starting with CLDR 46 and Unicode 16.0, the order of these groups is the same. In both sort orders, non-decimal-digit numeric characters now sort after decimal digits, and the CLDR root collation no longer tailors any currency symbols (making some of them sort like letter sequences, as in the DUCET).

These changes eliminate sort order differences among almost all regular characters between the CLDR root collation and the DUCET. See the [CLDR root collation](https://www.unicode.org/reports/tr35/tr35-collation.html#Root_Collation) documentation for details.

#### Improved Han Radical-Stroke Order
CLDR includes [data for sorting Han (CJK) characters in radical-stroke order](https://www.unicode.org/reports/tr35/46/tr35-collation.html#File_Format_FractionalUCA_txt).
It used to distinguish traditional and simplified forms of radicals on a higher level than sorting by the number of residual strokes.
Starting with CLDR 46, the CLDR radical-stroke order matches that of the [Unicode Radical-Stroke Index (large PDF)](https://www.unicode.org/Public/UCD/latest/charts/RSIndex.pdf).
[Its sorting algorithm is defined in UAX #38](https://www.unicode.org/reports/tr38/#SortingAlgorithm).
Traditional vs. simplified forms of radicals are distinguished on a lower level than the number of residual strokes.
This also has an effect on [alphabetic indexes](https://www.unicode.org/reports/tr35/46/tr35-collation.html#Collation_Indexes) for radical-stroke sort orders,
where only the traditional forms of radicals are now available as index characters.

### JSON Data Changes

1. Separate modern packages were dropped. [CLDR-16465][]
2. Transliteration (transform) data is now available in the `cldr-transforms` package. The JSON file contains transform metadata, and the `_rulesFile` key indicates an external (`.txt`) file containing the actual rules. [CLDR-16720][].

### Markdown

The CLDR site is in the process of being moved to markdown source (GFM),
which will regularize the formatting and make it easier to maintain and extend than with Google Sites.
The URLs will remain the same.
This process should be completed before release.

### File Changes

Most files added in this release were for new locales, with some files being added for existing locales that increased coverage, such as /testData/personNameTest/ak.txt.

The following new /common/testData/ files have been added:

- datetime/
    - README.md
    - datetime.json
- messageFormat/
    - schemas/v0/tests.schema.json
    - messageFormat/tests/
        - data-model-errors.json
        - functions/
            - date.json
            - datetime.json
            - integer.json
            - number.json
            - string.json
            - time.json
       - pattern-selection.json
       - syntax-errors.json
       - syntax.json

A few /common/testData/ files have been replaced:

- messageFormat/
    - data-model-errors.json
    - syntax-errors.json
    - test-core.json
    - test-core.json.d.ts
    - test-functions.json
    - test-functions.json.d.ts

### Tooling Changes

- Added and improved runtime examples
- Ability for certain languages to stay in submission mode longer
- General performance improvements to Survey Tool

### Keyboard Changes

- Clarified that `conformsTo=` does not need to be updated when a new CLDR version is released. [CLDR-17948][]

## Migration

1. Databases that use collation keys are sensitive to any changes in collation, and will need reindexing.
This can happen with any CLDR release (especially those for a new version of Unicode), but more characters are affected in this release: see a summary [above](#collation-data-changes).
2. Two collation variants are to be dropped in the CLDR 47 release: zh-u-co-gb2312 and zh-u-co-big5han.
These matched the ordering of two legacy character encodings. [CLDR-16062][]
3. The `light-speed` data was withdrawn from many locales, because the purpose (as an internal prefix for **light-second**, **light-minute**, etc.) was misunderstood. Implementations may hold off supporting it until the data is complete — expected for CLDR 47.

## Known Issues

1. [CLDR-17095][] The region-based firstDay value (see weekData) is currently used for several different purposes. In the future, some of these functions will be separated out:
    - The day that should be shown as the first day of the week in a calendar view.
    - The first day of the week (day 1) for weekday numbering.
    - The first day of the week for week-of-year calendar calculations.
1. [CLDR-18056][] “facing right” emojis are missing the skin tone in annotationsDerived
1. [CLDR-18080][] Han-Latin transform worse due to a change for 著 in Unicode 16
1. [CLDR-18407][] Unexpected time formatting patterns in Assamese, reverted in [CLDR-18415][] and will be fixed once day period data is validated.

### Fixed Issues

1. [CLDR-17610][] Updated zip to include LICENSE file following file renaming

## 46.1 Changes
Version 46.1 is a dot release. The following summarizes the changes. For a full listing, see [Δ46.1](https://unicode-org.atlassian.net/issues/?jql=project+%3D+CLDR+AND+status+%3D+Done+AND+resolution+%3D+Fixed+AND+fixVersion+%3D+%2246.1%22+ORDER+BY+priority+DESC)

### Message Format 2.0
The main focus is to bring the [Message Format 2.0 specification in Part 9]((https://www.unicode.org/reports/tr35/tr35-74/tr35.html#message-format-modifications)) to Final Candidate status. The following summarizes changes from v46:

* Core Message Format Specification  
  * Added ‘resolved values’ and ‘function handler’ sections to formatting
  * Defined “option resolution” and required order of options to be insignificant
  * Defined “implementation-defined” literal and type values more clearly
  * Required bidi isolation when u:dir is set and provided for ignoring isolation
* Default MessageFormat 2.0 function set and Unicode namespace  
  * Provided normative conditions on function, option, and option-value implementation; specified which functions and options were REQUIRED vs RECOMMENDED
  * Defined function composition for number, date/time, and string functions 
  * Added functions: :currency, :math, and :unit (proposed)
    * :math is to support the MessageFormat 1.0 ‘offset’
  * Added options to date/time functions: calendar, numberingSystem, hour12, and timezone (proposed)
  * Add various :u options to the :u namespace
* General
  * Provided general clarifications and reorganizations
  
There are 3 tech preview implementations available:
 - ICU4J: Java: [com.ibm.icu.message2](https://unicode-org.github.io/icu-docs/apidoc/dev/icu4j/index.html?com/ibm/icu/message2/package-summary.html), part of ICU 76, is a tech preview implementation of the MessageFormat 2.0, together with a formatting API. See the [ICU User Guide](https://unicode-org.github.io/icu/userguide/format_parse/messages/mf2.html) for examples and a quickstart guide, and [Trying MF 2.0 Final Candidate](https://unicode-org.github.io/icu/userguide/format_parse/messages/try-mf2.html) to try a “Hello World”.
 - ICU4C: [icu::message2::MessageFormatter](https://unicode-org.github.io/icu-docs/apidoc/released/icu4c/classicu_1_1message2_1_1MessageFormatter.html), part of ICU 76, is a tech preview implementation of MessageFormat 2.0, together with a formatting API. See the [ICU User Guide](https://unicode-org.github.io/icu/userguide/format_parse/messages/mf2.html) for examples and a quickstart guide, and [Trying MF 2.0 Final Candidate](https://unicode-org.github.io/icu/userguide/format_parse/messages/try-mf2.html) to try a “Hello World”.
 - Javascript: [messageformat](https://github.com/messageformat/messageformat/tree/main/mf2/messageformat) 4.0 provides a formatter and conversion tools for the MessageFormat 2 syntax, together with a polyfill of the runtime API proposed for ECMA-402.

### Other Changes
Changes aside from Message Format 2.0 include:

 - More explicit well-formedness and validity constraints for unit of measurement identifiers
 - Addition of derived emoji annotations that were missing: emoji with skin tones facing right
 - Fixes to make the ja, ko, yue, zh datetimeSkeletons useful for generating the standard patterns
 - Improved date/time test data

## Acknowledgments

Many people have made significant contributions to CLDR and LDML;
see the [Acknowledgments](/index/acknowledgments) page for a full listing.
We'd especially like to acknowledge the work done by interns this release: Chris Pyle, Helena Aytenfisu (ህሊና የሺጥላ አይተንፍሱ), and Emiyare Cyril Ikwut-Ukwa.

The Unicode [Terms of Use](https://unicode.org/copyright.html) apply to CLDR data;
in particular, see [Exhibit 1](https://unicode.org/copyright.html#Exhibit1).

For web pages with different views of CLDR data, see [http://cldr.unicode.org/index/charts](/index/charts).


[CLDR-16062]: https://unicode-org.atlassian.net/browse/CLDR-16062
[CLDR-16465]: https://unicode-org.atlassian.net/browse/CLDR-16465
[CLDR-16720]: https://unicode-org.atlassian.net/browse/CLDR-16720
[CLDR-17095]: https://unicode-org.atlassian.net/browse/CLDR-17095
[CLDR-17121]: https://unicode-org.atlassian.net/browse/CLDR-17121
[CLDR-17948]: https://unicode-org.atlassian.net/browse/CLDR-17948
[CLDR-18056]: https://unicode-org.atlassian.net/browse/CLDR-18056
[CLDR-17610]: https://unicode-org.atlassian.net/browse/CLDR-17610
[CLDR-18080]: https://unicode-org.atlassian.net/browse/CLDR-18080
[CLDR-18407]: https://unicode-org.atlassian.net/browse/CLDR-18407
[CLDR-18415]: https://unicode-org.atlassian.net/browse/CLDR-18415

<!-- CLDR 46 - 2024-10-24 - Release links-->
[v46]: /index/downloads/cldr-46
[CLDR46]: https://unicode.org/Public/cldr/46/
[Charts46]: https://unicode.org/cldr/charts/46/
[LDML46]: https://www.unicode.org/reports/tr35/tr35-73/tr35.html
[Δ46]: https://unicode-org.atlassian.net/issues/?filter=10834
[release-46]: https://github.com/unicode-org/cldr/releases/tag/release-46
[ΔDtd46]: https://www.unicode.org/cldr/charts/46/supplemental/dtd_deltas.html
[46.0.0]: https://github.com/unicode-org/cldr-json/releases/tag/46.0.0

<!-- CLDR 46.1 - 2024-12-18 - Release links-->
[v46.1]: /index/downloads/cldr-46#461-changes
[Charts46.1]: https://unicode.org/cldr/charts/46.1/
[LDML46.1]: https://www.unicode.org/reports/tr35/tr35-74/tr35.html
[Δ46.1]: https://unicode-org.atlassian.net/issues/?filter=10835
[release-46-1]: https://github.com/unicode-org/cldr/releases/tag/release-46-1
[ΔDtd46.1]: https://www.unicode.org/cldr/charts/46.1/supplemental/dtd_deltas.html
[46.1.0]: https://github.com/unicode-org/cldr-json/releases/tag/46.1.0

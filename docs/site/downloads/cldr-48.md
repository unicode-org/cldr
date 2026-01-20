---
title: CLDR 48 Release Note
---

# CLDR 48 Release Note

| No. |    Date    | Rel. Note |  Data  |  Charts  | Spec |   Delta  | GitHub Tag | Delta DTD | CLDR JSON |
|:---:|:----------:|:---------:|:------:|:--------:|:------------:|:---:|:----------:|:---------:|:---------:|
|  48 | 2025-10-29 | [v48] | [CLDR48] | [Charts48]| [LDML48] | [Δ48] | [release-48] | [ΔDtd48] | [48.0.0] |
|  48.1 | 2026-01-08 | [v48.1] | [CLDR48.1] | [Charts48.1] | [LDML48.1] | [Δ48.1] | [release-48-1] | n/a | [48.1.0] |

## Overview

Unicode CLDR provides key building blocks for software supporting the world's languages.
CLDR data is used by all [major software systems](/index#who-uses-cldr)
(including all mobile phones) for their software internationalization and localization,
adapting software to the conventions of different languages.

CLDR 48 was an open submission cycle allowing contributors to supply data for their languages via the CLDR Survey Tool —
data that is widely used to support much of the world’s software.
This data is also a factor in determining which languages are supported on mobile phones and computer operating systems.

Release notes for CLDR 48.1 can be found in the [48.1 Changes section](/downloads/cldr-48#481-changes)

### Changes

Some of the most significant changes in this release are:
- Updated for Unicode 17, including new names and search terms for new emoji, new sort-order, Han → Latin romanization additions for many characters.
- Many additions to language data including:
    - Likely Subtags, for deriving the likely script and region from the language (used in many processes).
    - Language populations in countries: significant updates to improve accuracy and maintainability.
- Updated to the latest external standards and data sources, such as the language subtag registry, UN M49 macro regions, ISO 4217 currencies, etc.
- New formatting options
    - Rational number formats added, allowing for formats like 5½.
    - For timezones, `usesMetazone` adds two new attributes `stdOffset` and `dstOffset` so that implementations can use either "main" or "rearguard" TZDB data sources.
    - There are now combination formats for _relative_ dates + times, such as “tomorrow _at_ 12:30”.
    - Additional units were added for scientific contexts (coulombs, farads, teslas, etc.) and for English systems (fortnights, imperial pints, etc.).
(New English system units were only translated into a few languages.)
- This is the first release where the new CLDR Organization process is in place for DDL languages.
As a result, several locales were able to reach higher levels.
- Many enhancements of the CLDR specification (LDML), covering the above items and many more.
For details see [Specification Changes](#specification-changes).

For more details, see below.

### Locale Coverage Status
The following shows the coverage levels per language in this version of CLDR.
- The **With Script** column indicates which of the **Count** locales are language-script variants.
    - For example, zh_Hant and zh(_Hans) add two to the **Count**, and one to **With Script**.
- The **Regional Variants** column indicates the number of other regional locales: none are in **Count**.
    - For example, there are 46 locales for French, such as fr, fr_CA, fr_BE, etc., so that adds 46 to the RV column for Modern.

#### Current Levels

Count | With Script | Regional Variants | Level | Usage | Examples
-- | -- | -- | -- | -- | --
104 | 5 | 305 | Modern | Suitable for full UI internationalization | Afrikaans, shqip, አማርኛ, ‫العربية‬, հայերեն, অসমীয়া, azərbaycan
13 | 0 | 1 | Moderate | Suitable for “document content” internationalization, eg. in spreadsheet | Akan, Cebuano, Māori, тоҷикӣ
57 | 10 | 22 | Basic | Suitable for locale selection, eg. choice of language on mobile phone | भोजपुरी, बर’, डोगरी, eʋegbe, Gã, हरियाणवी

#### Changes

| ± | New Level | Locales |
| -- | -- | -- |
| 📈 | Modern | Akan, Bashkir, Chuvash, Kazakh (Arabic), Romansh, Shan, Quechua |
| 📈 | Moderate | Anii, Esperanto |
| 📈 | Basic | Buriat, Piedmontese, Sicilian, Tuvinian |
| 📉 | Basic* | Baluchi (Latin), Kurdish |

\* Note: Two locales dropped in coverage (📉), from Moderate to Basic.
Each release, the number of items needed for Modern and Moderate increases.
So locales without active contributors may drop down in coverage level.

For a full listing, see [Coverage Levels](https://unicode.org/cldr/charts/48/supplemental/locale_coverage.html)

## Specification Changes

The following are some of the most significant changes to the specification (LDML).

### Locale Identifiers and Names
* [Display Name Elements](https://www.unicode.org/reports/tr35/48/tr35-general.html#display-name-elements) Described the usage of the `language` element `menu` values `core` and `extension`, and `alt="menu"`.
Also revamped the description of how to construct names for locale IDs, for clarity.

### Misc.
* [Character Elements](https://www.unicode.org/reports/tr35/48/tr35-general.html#character-elements) Added new exemplar types.
* [Person Name Validation](https://www.unicode.org/reports/tr35/48/tr35-personNames.html#person-name-validation) Added guidance for validating person names.

### DateTime formats

* [Element dateTimeFormat](https://www.unicode.org/reports/tr35/48/tr35-dates.html#dateTimeFormat) Added a new type `relative` for relative date/times, such as "tomorrow at 10:00",
and updated the guidelines for using the different `dateTimeFormat` types.
* [timeZoneNames Elements Used for Fallback](https://www.unicode.org/reports/tr35/48/tr35-dates.html#timeZoneNames-elements-used-for-fallback) Added the `gmtUnknownFormat` to indicate when the timezone is unknown.
* [Metazone Names](https://www.unicode.org/reports/tr35/48/tr35-dates.html#metazone-names) Added `usesMetazone` to specify which offset is considered standard time and which offset is considered daylight.
* [Time Zone Format Terminology](https://www.unicode.org/reports/tr35/48/tr35-dates.html#time-zone-format-terminology) Added the **Localized GMT format** (and removed the **Specific location format**).
This affects the behavior of the `z` timezone format symbol.
There is also now a mechanism for finding the region code from short timezone identifier, which is used for the _non-location formats (generic or specific)_
* [Calendar Data](https://www.unicode.org/reports/tr35/48/tr35-dates.html#calendar-data) Specified more precisely the meaning of the `era` attributes in supplemental data, and how to determine the transition point in time between eras.

### Numbers
* [Plural rules syntax](https://www.unicode.org/reports/tr35/48/tr35-numbers.html#plural-rules-syntax) Added substantial clarifications and new examples.
The order of execution is also clearly specified.
* [Compact Number Formats](https://www.unicode.org/reports/tr35/48/tr35-numbers.html#compact-number-formats) Specified the mechanism for formatting compact numbers more precisely.
* [Rational Numbers](https://www.unicode.org/reports/tr35/48/tr35-numbers.html#rational-numbers) Added support for formatting fractions like 5½.

### Units of Measurement
* [Unit Syntax](https://www.unicode.org/reports/tr35/48/tr35-general.html#unit-syntax) Simplified the EBNF `product_unit` and added an additional well-formedness constraint for mixed units.
* [Unit Identifier Normalization](https://www.unicode.org/reports/tr35/48/tr35-general.html) Modified the normalization process.
* [Mixed Units](https://www.unicode.org/reports/tr35/48/tr35-general.html#mixed-units) Modified the guidance for handling precision.

### MessageFormat
* Syntax and data model errors must now be prioritized over other errors. <!-- ([\#1011](https://github.com/unicode-org/message-format-wg/pull/1011)) -->
* The Default Bidi Strategy is now required and default. <!-- ([\#1066](https://github.com/unicode-org/message-format-wg/pull/1066)) -->
* The `:offset` function (previously named `:math`) is now available as Stable. <!-- ([\#1073](https://github.com/unicode-org/message-format-wg/pull/1073)) -->
* The `:datetime`, `:date`, and `:time` functions are updated to build on top of semantic skeletons. <!-- ([\#1078](https://github.com/unicode-org/message-format-wg/pull/1078), [\#1083](https://github.com/unicode-org/message-format-wg/pull/1083)) -->
* `:percent` is added as a new Draft function. <!-- ([\#1094](https://github.com/unicode-org/message-format-wg/pull/1094)) -->

There are many more changes that are important to implementations, such as changes to certain identifier syntax and various algorithms.
See the [Modifications section](https://www.unicode.org/reports/tr35/48/tr35-modifications.html) of the specification for details.

## Data Changes

### Locale Changes

#### General
- Languages that reached Basic in the last release have their names translated at Modern Coverage in this release.
- New emoji for Unicode 17 have added names and search keywords.
- Compound language names now have "core" and "extension" variants for more uniform formats in menus and lists.
The description of how to format names for locale IDs has been extended and clarified with new attributes added for better presentation in menus and other environments `menu="core"`, `menu="extension"`, `scope="core"`. [See Display Name Elements](https://www.unicode.org/reports/tr35/48/tr35-general.html#display-name-elements) for details.
- Recent or upcoming currency names were added (XCG, ZWG).
- To match ISO, added translations for the region Sark (CQ).
- There are now combination formats for _relative_ dates + times, such as “tomorrow _at_ 12:30”.
In some languages the use of a relative date such as “tomorrow” or “2 days ago” requires a _different_ combining pattern than for a fixed date like “March 20”.
A new “relative” variant is introduced to allow for those languages.
- Added some additional flexible date formats (aka `availableFormats`).
- Some locales had seldom-used short timezone abbreviations (such as EST) removed, or moved to sublocales that use them.
- The currency-number formats for `alphaNextToNumber`, `noCurrency`, and compact currency formats are now generated from other data for consistency.
The `alphaNextToNumber` patterns allow for adding a space between letter currency symbols and numbers, for example, "USD 123" vs "US$123".
- The tooling made it easier to see when a space was a non-breaking character or not, or a thin version of those. The usage is now more consistent in many locales.
- For the Etc/Unknown timezone, the `exemplarCity` name was changed from “Unknown City” to “Unknown Location” for clarity.
- Rational number formats were added, allowing for formats like 5½.
- Certain concentration units were reworked, for “parts per million”, “parts per billion”.
- Additional units were added for scientific contexts (coulombs, farads, teslas, etc.) and for English systems (fortnights, imperial pints, etc.).
However, translation of these English system names was not required.
- Additional guidance on translation was added, leading to refined translations or transcreations.
- SIL contributed exemplar data for 860 new or updated locales. The ones that don't have other locale data are in the `/exemplars/` directory (parallel to `/common`.

#### Specific Locales
- Kurdish (Kurmanji) `ku` split from 1 locale `ku_TR` into 5 locales across 2 scripts and 4 countries. [CLDR-18311][]
  - `ku_Latn_TR`: Kurdish (Kurmanji, Latin alphabet, Turkey) default for Kurdish (Kurmanji) `ku` and `ku_Latn`
  - `ku_Latn_SY`: Kurdish (Kurmanji, Latin alphabet, Syria)
  - `ku_Latn_IQ`: Kurdish (Kurmanji, Latin alphabet, Iraq)
  - `ku_Arab_IQ`: Kurdish (Kurmanji, Arabic writing, Iraq), default for Kurdish (Kurmanji, Arabic writing) `ku_Arab`
  - `ku_Arab_IR`: Kurdish (Kurmanji, Arabic writing, Iran)

For a full listing, see [Delta Data].

### DTD Changes
For a full listing, see [Delta DTDs].

#### ldml
The explanations of usage are in the [Locale Changes](#locale-changes) section.
- `exemplarCharacters` — added more `type` values:
   - `numbers-auxiliary` — for number characters that are not 'core' to the language, but sometimes used (like regular auxiliary).
   - `punctuation-auxiliary` — for punctual characters that are not 'core' to the language, but sometimes used (like regular auxiliary).
   - `punctuation-person` — for the limited set of punctuation characters used in person name fields: eg, "Jean-Luc", "MD, Ph.D.".
- `dateTimeFormat` — added a `relative` `type` value for combining time and date.
- `gmtUnknownFormat` — element was added — Indicating that the timezone is unknown (as opposed to absent from the format).
- `language` — added more `menu` values: `core` and `extension`.
- `type` — added a `core` `scope` value.
- `numbers` — added `rationalFormats` sub-elements: `rationalPattern`, `integerAndRationalPattern` (with an `alt="superSub" variant`), `rationalUsage`.
- `rbnf​/rulesetGrouping` — added `rbnfRules` sub-element:
    - This “flattens” the rules into a format that is easier for implementations to use directly.

#### supplementalData
- `era` — the range of `code` values now allows two letters before the first hyphen.
- `languageData` — the `territories` attribute [`supplementalData.xml`](https://github.com/unicode-org/cldr/blob/main/common/supplemental/supplementalData.xml) was deprecated and data using it removed.
The definition was unclear and prone to misunderstanding — the more detailed data is in `territoryInfo`. [CLDR-5708][]
- `usesMetazone` — adds two new attributes `stdOffset` and `dstOffset` so that implementations can use either "main" or "rearguard" TZDB data sources.
- `numberingSystem` — Unicode 17 data was added.

#### ldmlBCP47
- `type` — adds a new attribute `region`, for determining the region from short timezone IDs when not derivable from the first two characters.
- `keyboard3@conformsTo` — is updated to allow "48".
- `hc` — adds values `c12` and `c24` as Technical Preview. Also see the note about `h24` in "V49 advance warnings". [CLDR-18894][]

### BCP47 Data Changes

For a full listing, see [BCP47 Delta].

- `nu-tols` numbering system for Tolong Siki digits
- One additional zone: 	`America/Coyhaique` = `tz-clcxq`
- Region attributes in timezones for determining regions where it can't be derived from the short TZID
- Three additional aliases

### Supplemental Data Changes

For a full listing, see [Supplemental Delta].

#### Identifiers
- Added aliases/deprecations for languages (dek, mnk, nte).
- Updated to the latest language subtag registry, with various additions and deprecations.
- Updated to the ISO currency data, with various additions and deprecations.
- Added unit IDs part, part-per-1e6, part-per-1e9, cup-imperial, fluid-ounce-metric, … with conversions.
  - deprecated unit IDs permillion, portion, portion-per-1e9, 100-kilometer.
- Eras and era codes added and deleted to align with ECMA-402 ([CLDR-18369][])
  - New era: Before Hijri, code `bh`, in all Islamic calendars
  - New era codes and replacements:
    - `am` (Coptic)
    - `aa` (Ethiopian, replaces `mundi`)
    - `am` (Ethiopian, replaces `incar`)
    - `shaka` (Indian, replaces `saka`)
    - `broc` (ROC, replaces `before-roc`)
  - All codes that include the calendar name (like `gregory-inverse`) were removed, except for `roc`
  - Additional aliases removed for ECMA-402 alignment:
    - `minguo` (ROC)

#### Language Data
- [language_script.tsv](https://github.com/unicode-org/cldr/blob/main/tools/cldr-code/src/main/resources/org/unicode/cldr/util/data/language_script.tsv) updated to include only one "Primary" writing system for languages that used to have multiple options [CLDR-18114][].
Notable changes are:
  - Punjabi `pa` has changed the primary script to Gurmukhi `Guru` because widespread usage is in the Gurmukhi script. While most speakers are in Pakistan `PK`, written usage remains Gurmukhi.
  - Azerbaijani `az` and Northern Kurdish `ku` primarily are used in Latin `Latn`.
  - Chinese languages `zh`, `hak`, and `nan` are matched to Simplified Han writing `Hans` -- except Cantonese `yue`, which is known for a preference in Traditional Han writing `Hant`.
  - Hassiniyya `mey` was missing significant data, it should be associated with the Arabic `Arab` writing system by default, not Latin `Latn`.
- 5 new language distance values are added (for fallback to `zh`).
- Substantial updates to Language Info: additional languages in countries; revised population values, writing percentages, literacy percentages, and official status values.

#### Likely Subtags
- Many additions: see [Likely Subtags]
- Errors in likely subtags addressed
   - The default language for Belarus `BY` is now Russian `ru`, reflecting modern usage. [CLDR-14479][]
   - Literary Chinese `lzh` was written in Traditional Han writing `Hant`. [CLDR-16715][]
- Likely subtags updated because of prior mentioned primary script matches.
  - Northern Kurdish `ku` now matched to Cyrillic writing in the CIS countries. [CLDR-18114][]
  - Hassiniyya `mey` updated to default to `mey_Arab_DZ` instead of `mey_Latn_SN`. [CLDR-18114][]

#### Calendars, Timezones, Dayperiods

- Many updates and corrections for Metazone data
- Many updates to calendars, including the removal of eras and adjustment to era start dates
- Day periods for kok, scn, hi_Latn
- `de`: "Uhr" was removed where AM/PM was present. [CLDR-18934]

#### Plural Rules
- Additions for cv, ie, kok, sgs

#### Currencies
- Updates to the latest ISO currencies

#### Weekdata
- IS changed to firstDay=sun
- ku_SY adding H and hB

### Transforms
For a full listing, see [Transforms Delta].

- Fixed problem in Gujarati → Latin romanization, with ૰
- Updated to latest Unicode 17 data for Han → Latin, with very many changes.

### Numbers

- `de_CH`: the group symbol was changed to `'` (apostrophe). [CLDR-13542]

### Number Spellout Data Changes

- The biggest change is to the format, which has been “flattened” for easier use by clients.

### JSON Data Changes

- RBNF
  - Just as with the RBNF data format change in XML [CLDR-8909], the JSON data also has a change in structure. [CLDR-18956].
  - Below is an example of the changed data format.
  - The new data item is the `_rbnfRulesFile` key. Its value is the name of a data file in the same directory, containing the raw rules.  (Note: Do not interpret the .txt file’s name in any way.)
  - The previous data format is included for this release, but will be removed in a future release. In this case, the `%digits-ordinal` (and any other such keys) will be removed.


```js
{
    "rbnf": {
      "OrdinalRules": {
        "%digits-ordinal": [
          [
            "-x",
            "−→→;"
          ],
          [
            "0",
            "=#,##0=;"
          ]
        ],
        "_rbnfRulesFile": "ar-OrdinalRules.txt"
      },
    }
}
```

- The `rgScope` values are now approved, but they are not output in cldr-json at present.
See [CLDR-19029] for a discussion of the issue.

The `ar-OrdinalRules.txt` file contains all rules for this locale:

   %digits-ordinal:
   -x: −>>;
   0: =#,##0=;

### File Changes
The following files are new in the release:

| Level 1 | Level 2 | Level 3 | Files |
| :---- | :---- | :---- | :---- |
| common | annotations |  | ba.xml, shn.xml, sv\_FI.xml, syr.xml |
|  | casing |  | sgs.xml |
|  | collation |  | blo.xml, sgs.xml |
|  | main |  | bqi\_IR.xml, bqi.xml, bua\_RU.xml, bua.xml, en\_EE.xml, en\_GE.xml, en\_JP.xml, en\_LT.xml, en\_LV.xml, en\_UA.xml, kek\_GT.xml, kek.xml, ku\_Arab\_IQ.xml, ku\_Arab\_IR.xml, ku\_Arab.xml, ku\_Latn\_IQ.xml, ku\_Latn\_SY.xml, ku\_Latn\_TR.xml, ku\_Latn.xml, lzz\_TR.xml, lzz.xml, mww\_Hmnp\_US.xml, mww\_Hmnp.xml, mww.xml, oka\_CA.xml, oka\_US.xml, oka.xml, pi\_Latn\_GB.xml, pi\_Latn.xml, pi.xml, pms\_IT.xml, pms.xml, sgs\_LT.xml, sgs.xml, suz\_Deva\_NP.xml, suz\_Deva.xml, suz\_Sunu\_NP.xml, suz\_Sunu.xml, suz.xml |
|  | testData | personNameTest | ba.txt, blo.txt, cv.txt, kk\_Arab.txt, kok\_Latn.txt, rm.txt, shn.txt |
|  | uca |  | FractionalUCA\_blanked.txt |

## Migration

- Number patterns that did not have a specific numberSystem (such as `latn` or `arab`) had been deprecated for many releases, and were finally removed.
- Additionally, language and territory data in `languageData` and `territoryInfo` data received significant updates to improve accuracy and maintainability [CLDR-18087]
- The likely language for Belarus changed to Russian [CLDR-14479]
- [Using Time Zone Names](https://www.unicode.org/reports/tr35/48/tr35-dates.html#using-time-zone-names) Removed the "specific location format" and modified the fallback behavior of 'z'.
- [Unit Identifier Normalization](https://www.unicode.org/reports/tr35/48/#tr35-general.html) Modified the normalization process.
- The `era` element `type` attributes no longer need to start at 0. Implementations that use arrays may have to adjust their implementations.
- The default week numbering mechanism changes to be identical to ISO instead being based on the calendar week.
- Deprecated unit IDs `permillion`, `portion`, `portion-per-1e9`. These are replaced by IDs using `part`. Also deprecated the compound component `100-kilometer`, since (certain) integers are allowed in unit ID denominators.
- For compact short currency formatting (such as "$13B"), implementations should use the new `alphaNextToNumber` variants to get the correct spacing.
- The unit identifiers for the following changed for consistency.
As with all such changes, aliases are available to permit parsing and formatting to work across versions.
    - `permillion` changed to `part-per-1e6`; English values remain “parts per million”, “{0} part per million”, etc.
    - `portion-per-1e9` changed to `part-per-1e9`; English values remain “parts per billion”, “{0} part per billion”, etc.
    - `part` used for constructing arbitrary concentrations such as “parts per 100,000”; English values “parts”, “{0} part”, etc.
- English and/or root names of many exemplar cities and some metazones changed.
This was typically to move towards the official spelling in the country in question, such as retaining accents, or to add landscape terms such as “Island”.
For example: El Aaiun → El Aaiún; Casey → Casey Station; Hovd Time → Khovd Time.
- A few additional availableFormat and interval format patterns have been added, such as GyMEd and Hv, to fill some gaps.
- The metazone for Hawaii has changed.

## 48.1 Changes

| No. |    Date    | Rel. Note |  Data  |  Charts  | Spec |   Delta  | GitHub Tag | Delta DTD | CLDR JSON |
|:---:|:----------:|:---------:|:------:|:--------:|:------------:|:---:|:----------:|:---------:|:---------:|
|  48.1 | 2026-01-08 | [v48.1] | [CLDR48.1] | [Charts48.1] | [LDML48.1] | [Δ48.1] | [release-48-1] | n/a | [48.1.0] |

Version 48.1 is a dot release. The following summarizes the changes. For a full listing, see [Δ48.1](https://unicode-org.atlassian.net/issues/?jql=project+%3D+CLDR+AND+status+%3D+Done+AND+resolution+%3D+Fixed+AND+fixVersion+%3D+%2248.1%22+ORDER+BY+priority+DESC)

### Data changes

- Update RSD (Serbian Dinar) to use 2-digits for non-cash and 0-digits for cash.
- Fix issue with tippi and bindi in Punjabi exemplars.
- Fix parsing issue in numeric date+time patterns for zh_Hant and yue.
- Fix issues with hour cycle display names for several languages.

### Spec changes

- Segmentations - removed outdated note about `X Format*->` not being supported in CLDR since it no longer accurate.

## V49 advance warnings

The following changes are planned for CLDR 49. Please plan accordingly to avoid disruption.
- H24 will be deprecated. If it is encountered, it will have H23 behavior. There is no known intentional usage of H24. If you have a current need for H24 instead of H23, please comment on [CLDR-18303][].
- As of CLDR 49, the default week numbering system will change to follow ISO (where weeks are numbered based on Thursday), instead of being based on the start of the calendar week. The calendar week will be more clearly targeted at matching usage in displayed month calendars. [CLDR-18275][]
- The pre-Meiji Japanese eras will be removed: There was too much uncertainty in the exact values
and feedback that the general practice for exact dates is to use Gregorian for pre-Meiji dates.
- The major components in [supplementalData.xml](https://github.com/unicode-org/cldr/blob/main/common/supplemental/supplementalData.xml) and [supplementalMetadata.xml](https://github.com/unicode-org/cldr/blob/main/common/supplemental/supplementalMetadata.xml) files are slated to be organized more logically and moved into separate files.
    - This will make it easier for implementations to filter out data that they don't need, and make internal maintenance easier. This will not affect the data: just which file it is located in. Please plan to update XML and JSON parsers accordingly.

## Known Issues

- ISO 3166-2 subdivision codes for Iran changed in 2020, and there are not yet new equivalent stable codes. [CLDR-19046]
- Segmentation format changed, spec updated in CLDR 48.1 to remove obsolete note. [CLDR-19140]
- There are a few collisions in the emoji short names in ba, fr, ja, rm, vi. [CLDR-19189]

## Acknowledgments

Many people have made significant contributions to CLDR and LDML.
For a full listing, see the [Acknowledgments](https://www.unicode.org/reports/tr35/48/tr35-acknowledgments.html).

The Unicode [Terms of Use](https://unicode.org/copyright.html) apply to CLDR data;
in particular, see [Exhibit 1](https://unicode.org/copyright.html#Exhibit1).

For web pages with different views of CLDR data, see [http://cldr.unicode.org/index/charts](/index/charts).

[CLDR-5708]: https://unicode-org.atlassian.net/browse/CLDR-5708
[CLDR-8909]: https://unicode-org.atlassian.net/browse/CLDR-8909
[CLDR-11400]: https://unicode-org.atlassian.net/browse/CLDR-11400
[CLDR-13542]: https://unicode-org.atlassian.net/browse/CLDR-13542
[CLDR-14479]: https://unicode-org.atlassian.net/browse/CLDR-14479
[CLDR-16004]: https://unicode-org.atlassian.net/browse/CLDR-16004
[CLDR-16715]: https://unicode-org.atlassian.net/browse/CLDR-16715
[CLDR-18087]: https://unicode-org.atlassian.net/browse/CLDR-18087
[CLDR-18113]: https://unicode-org.atlassian.net/browse/CLDR-18113
[CLDR-18114]: https://unicode-org.atlassian.net/browse/CLDR-18114
[CLDR-18219]: https://unicode-org.atlassian.net/browse/CLDR-18219
[CLDR-18275]: https://unicode-org.atlassian.net/browse/CLDR-18275
[CLDR-18303]: https://unicode-org.atlassian.net/browse/CLDR-18303
[CLDR-18311]: https://unicode-org.atlassian.net/browse/CLDR-18311
[CLDR-18369]: https://unicode-org.atlassian.net/browse/CLDR-18369
[CLDR-18894]: https://unicode-org.atlassian.net/browse/CLDR-18894
[CLDR-18934]: https://unicode-org.atlassian.net/browse/CLDR-18934
[CLDR-18956]: https://unicode-org.atlassian.net/browse/CLDR-18956
[CLDR-19029]: https://unicode-org.atlassian.net/browse/CLDR-19029
[CLDR-19046]: https://unicode-org.atlassian.net/browse/CLDR-19046
[CLDR-19140]: https://unicode-org.atlassian.net/browse/CLDR-19140
[CLDR-19189]: https://unicode-org.atlassian.net/browse/CLDR-19189

[Delta DTDs]: https://unicode.org/cldr/charts/48/supplemental/dtd_deltas.html
[BCP47 Delta]: https://unicode.org/cldr/charts/48/delta/bcp47.html
[Supplemental Delta]: https://unicode.org/cldr/charts/48/delta/supplemental-data.html
[Likely Subtags]: https://www.unicode.org/cldr/charts/48/delta/supplemental-data.html#Likely
[Transforms Delta]: https://unicode.org/cldr/charts/48/delta/transforms.html
[Delta Data]: https://unicode.org/cldr/charts/48/delta/index.html

<!-- 48.1 release: 2026-01-08 -->
[v48.1]: /downloads/cldr-48#481-changes
[CLDR48.1]: https://unicode.org/Public/cldr/48.1/
[Charts48.1]: https://unicode.org/cldr/charts/48.1/
[LDML48.1]: https://www.unicode.org/reports/tr35/48/tr35.html
[Δ48.1]: https://unicode-org.atlassian.net/issues/?jql=project%20%3D%20CLDR%20AND%20status%20%3D%20Done%20AND%20resolution%20%3D%20Fixed%20AND%20fixversion%20%3D%2048%2E1%20ORDER%20BY%20priority%20DESC
[release-48-1]: https://github.com/unicode-org/cldr/releases/tag/release-48-1
<!-- no ΔDtd48.1 -->
[48.1.0]: https://github.com/unicode-org/cldr-json/releases/tag/48.1.0

<!-- 48 release: 2025-10-29 -->
[v48]: /downloads/cldr-48
[CLDR48]: https://unicode.org/Public/cldr/48/
[Charts48]: https://unicode.org/cldr/charts/48
[LDML48]: https://www.unicode.org/reports/tr35/tr35-76/tr35.html
[Δ48]: https://unicode-org.atlassian.net/issues/?jql=project%20%3D%20CLDR%20AND%20status%20%3D%20Done%20AND%20resolution%20%3D%20Fixed%20AND%20fixversion%20%3D%2048%20ORDER%20BY%20priority%20DESC
[release-48]: https://github.com/unicode-org/cldr/releases/tag/release-48
[ΔDtd48]: https://www.unicode.org/cldr/charts/48/supplemental/dtd_deltas.html
[48.0.0]: https://github.com/unicode-org/cldr-json/releases/tag/48.0.0


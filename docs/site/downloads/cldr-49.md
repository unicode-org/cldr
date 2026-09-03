---
title: CLDR 49 Release Note
---

# CLDR 49 Release Note

| No. |    Date    | Rel. Note |  Data  |  Charts  | Spec |   Delta  | GitHub Tag | Delta DTD | CLDR JSON |
|:---:|:----------:|:---------:|:------:|:--------:|:------------:|:---:|:----------:|:---------:|:---------:|
|  49 | 2026-10-~~XX~~ | [v49](/index/downloads/cldr-49) | ~~[CLDR49](https://unicode.org/Public/cldr/49/)~~ | [Charts49](https://unicode.org/cldr/charts/dev) | [LDML49](https://www.unicode.org/reports/tr35/49/tr35.html) | [Δ49](https://unicode-org.atlassian.net/issues/?jql=project%20%3D%20CLDR%20AND%20status%20%3D%20Done%20AND%20resolution%20%3D%20Fixed%20AND%20fixversion%20%3D%2049%20ORDER%20BY%20priority%20DESC) | [release-49-alpha2](https://github.com/unicode-org/cldr-staging/releases/tag/release-49-alpha2) | [ΔDtd49](https://www.unicode.org/cldr/charts/dev/supplemental/dtd_deltas.html) | [49.0.0-ALPHA1](https://github.com/unicode-org/cldr-json/releases/tag/49.0.0-ALPHA1) |

## Overview

Unicode CLDR provides key building blocks for software supporting the world's languages.
CLDR data is used by all [major software systems](/index#who-uses-cldr)
(including all mobile phones) for their software internationalization and localization,
adapting software to the conventions of different languages.


### Changes

The most significant changes in this release are:

- Updated for Unicode 18 including annotations for the new emoji, changes to sorting order, etc.
- Reflects the most recent updates to external standards and data sources, such as the language subtag registry, UN M49 macro regions, ISO 4217 currencies, etc.
- New date & time formatting features including:
  - Localized patterns for gluing date & timezoneAppend Items — e.g., Sept 3, EST
  - Ordinal days in dates — e.g., Sept 3rd
  - Customizing numeric datetime separators in patterns: 3-10-2031 → 3/10/2031
  - UTC Timezone Display Patterns
  - Dual format for zones with two offsets (summer and winter) — eg, GMT-8/-7
  - Deprecated Japanese era data before Meiji
  - Structure for preventing digit-digit merges — e.g., '2026/1/29 GMT-817时'
  - Additional skeleton-patterns added for flexible and interval date formats
  - New units (conversions and formatting)
    - 3 new units: Poundal, Dyne, and Milliinch (US mil)
    - 14 new display names in English
  - Nested Bracket Replacement — for constructing locale names with parts that have parentheses, eg, ”birmanês (Mianmar [Birmânia])”
  - Many additional localized locale option names for use in menus, such as calendar and number-system names
- [Basic coverage level locale data][] for 11 new locales: Coptic (moderate), Adyghe, Central Kurdish, Colognian, Kabardian, Kikuyu, Kʼicheʼ, Ladin, Prussian, Qʼeqchiʼ, Sunwar (Sunuwar) 
 
Note: Many enhancements of the CLDR specification (LDML) are due for addition by the CLDR 49 Beta (September 23rd).

For more details, see below.

### Locale Coverage Status

#### Current Levels

Count | Level | Usage | Examples
-- | -- | -- | --
100 | Modern | Suitable for full UI internationalization | Afrikaans, shqip, አማርኛ, ‫العربية‬, հայերեն, অসমীয়া, azərbaycan
12 | Moderate | Suitable for “document content” internationalization, eg. in spreadsheet | Akan, Cebuano, Māori, тоҷикӣ
73 | Basic | Suitable for locale selection, eg. choice of language on mobile phone | भोजपुरी, बर’, डोगरी, eʋegbe, Gã, हरियाणवी

\* Note: This includes just the base language and script. There are many more regional variants.

#### Changes

| ± | New Level | Locales |
| -- | -- | -- |
| 📈 | Modern | Akan |
| 📈 | Moderate | Breton, _Coptic_ |
| 📈 | Basic | **Adyghe, Central Kurdish, Colognian, Kabardian, Kikuyu, Kʼicheʼ, Ladin, Prussian, Qʼeqchiʼ, Sunwar (Sunuwar)** |
| 📉 | Moderate* | Romansh, Shan, Tigrinya |
| 📉 | Basic* | Bashkir, Faroese, Interlingua, Sardinian, Tajik, Venetian |

\* Note: Each release, the number of items needed for Modern and Moderate increases. So locales without active contributors may drop down in coverage level. Locales that were below Basic before are bolded.

For a full listing, see [Coverage Levels](https://unicode.org/cldr/charts/dev/supplemental/locale_coverage.html)

## Specification Changes

Note: Many enhancements of the CLDR specification (LDML) are due for addition by the CLDR 49 Beta (September 23rd).

The following are the most significant changes to the specification (LDML).

- Clarified the process of selecting the best `dateFormatItem` when there is no exact match, and how to use `appendItems` to add missing fields. This includes a clarification of what are date fields and what are time fields, and a note that `appendItems` for date and time fields should be appended before combining them.

<!-- There are many more changes that are important to implementations, such as changes to certain identifier syntax and various algorithms.-->
See the [Modifications section](https://www.unicode.org/reports/tr35/49/tr35-modifications.html#modifications) of the specification for details.

## Data Changes

### DTD Changes

- TBD

For a full listing, see [Delta DTDs](https://unicode.org/cldr/charts/dev/supplemental/dtd_deltas.html).

### Supplemental Data Changes

- TBD

For a full listing, see [¤¤BCP47 Delta](https://unicode.org/cldr/charts/dev/delta/bcp47.html) and [¤¤Supplemental Delta](https://unicode.org/cldr/charts/dev/delta/supplemental-data.html)

### Locale Changes

- Updated en-AU and en-NZ to include exemplar characters for Indigenous languages
- Updated AR, CL, PY, UY, and ZM to prefer 24 hour time
- Changes to plural and ordinal rules (see [Language Plural Rules chart][] for more information):
   - New plural case `many` for Galician (`gl`)
   - Added plural rules for Tajik (`tg`), and Vietnamese (`vi`)
   - Fixed bug so Norwegian Nynorsk (`nn`) inherits plural rules from Norwegian (`no`)
   - Added ordinal rules for Afrikaans (`af`) and Bulgarian (`bg`)
   - Updated ordinal rules for Spanish (`es`)
   - See [plurals tickets for full list][]
- Fixed issue with cross-language inheritance which was giving Haitian Creole an artificially high coverage level.

For a full listing, see [Delta Data](https://unicode.org/cldr/charts/dev/delta/index.html)

### Message Format Specification

- TBD

### Collation Data Changes

- TBD

### Number Spellout Data Changes

Addition or improvement of RBNF rules for many locales including Catalan, Italian, Croatian, Greek, Romanian, Ukrainian and more. See [RBNF tickets for full list][]. 



### Segmentation Data Changes

- TBD

### Transform Data Changes

- TBD

### JSON Data Changes

- TBD

### File Changes

* New locale files (28 files, eg. brh.xml)
* New test data
    * Date/time, decimal, messageFormat (10 files)
    * Person name formatting (1 file, br.txt)
    * Rule-based number formatting (99 files, eg, af.ssv)
* MessageFormat tests/functions (1 file, math.json)
* New keyboard files (4 files)
* Many new or modified readme files.

### Tooling Changes

- TBD

### Keyboard Changes

- TBD

## Migration

The following changes have been made in CLDR 49. Please plan accordingly to avoid disruption.

- The pre-Meiji Japanese eras will be removed: There was too much uncertainty in the exact values
and feedback that the general practice for exact dates is to use Gregorian for pre-Meiji dates.

 
### Advanced warnings CLDR 50 and beyond

The following changes are planned for CLDR 50. Please plan accordingly to avoid disruption.

- Locales which do not have Core data will be removed if still missing core data by alpha. [CLDR-16004]
- Montenegrin `cnr` will be considered a separate locale and will no longer alias to `sr_Latn_ME`. [CLDR-10769]
- The default week numbering system will change to follow ISO (where weeks are numbered based on Thursday), instead of being based on the start of the calendar week. The calendar week will be more clearly targeted at matching usage in displayed month calendars. [CLDR-18275][]
- The major components in [supplementalData.xml](https://github.com/unicode-org/cldr/blob/main/common/supplemental/supplementalData.xml) and [supplementalMetadata.xml](https://github.com/unicode-org/cldr/blob/main/common/supplemental/supplementalMetadata.xml) files are slated to be organized more logically and moved into separate files.
      - This will make it easier for implementations to filter out data that they don't need, and make internal maintenance easier. This will not affect the data, just which file it is located in. Please plan to update XML and JSON parsers accordingly.
- CLDR plans to update its segmentation once Finite automata for line breaking and segmentation is final. [UTC](https://www.unicode.org/consortium/utc.html) has a [PRI#555](https://www.unicode.org/review/pri555/) for the current proposal which is open until October 5, 2026.

## Known Issues

- ISO 3166-2 subdivision codes for Iran changed in 2020, and there are not yet new equivalent stable codes. See [CLDR-19060][] for more details.
- Keyboard: Normalization-safe segments definition does not cover all normalization cases. [CLDR-19218]


## Acknowledgments

Many people have made significant contributions to CLDR and LDML;
see the [Acknowledgments](/index/acknowledgments) page for a full listing.

The Unicode [Terms of Use](https://unicode.org/copyright.html) apply to CLDR data;
in particular, see [Exhibit 1](https://unicode.org/copyright.html#Exhibit1).

For web pages with different views of CLDR data, see [http://cldr.unicode.org/index/charts](/index/charts).

[CLDR-10769]: https://unicode-org.atlassian.net/browse/CLDR-10769
[CLDR-16004]: https://unicode-org.atlassian.net/browse/CLDR-16004
[CLDR-18275]: https://unicode-org.atlassian.net/browse/CLDR-18275
[CLDR-18303]: https://unicode-org.atlassian.net/browse/CLDR-18303
[CLDR-19046]: https://unicode-org.atlassian.net/browse/CLDR-19046
[CLDR-19060]: https://unicode-org.atlassian.net/browse/CLDR-19060
[CLDR-19218]: https://unicode-org.atlassian.net/browse/CLDR-19218
[Basic coverage level locale data]: /index/cldr-spec/coverage-levels#basic-data
[Language Plural Rules chart]: https://www.unicode.org/cldr/charts/49/supplemental/language_plural_rules.html
[plurals tickets for full list]: https://unicode-org.atlassian.net/issues?jql=project%20%3D%20CLDR%20AND%20status%20%3D%20Done%20AND%20resolution%20%3D%20Fixed%20AND%20fixversion%20%3D%2049%20AND%20component%20%3D%20plurals%0AORDER%20BY%20priority%20DESC
[RBNF tickets for full list]: https://unicode-org.atlassian.net/issues?jql=project%20%3D%20CLDR%0AAND%20status%20%3D%20Done%0AAND%20resolution%20%3D%20Fixed%0AAND%20fixversion%20%3D%2049%0AAND%20component%20%3D%20numbers-rbnf%0AORDER%20BY%20priority%20DESC

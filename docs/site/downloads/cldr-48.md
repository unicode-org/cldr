---
title: CLDR 48 Release Note
---

# CLDR 48 Release Note

| No. |    Date    | Rel. Note |  Data  |  Charts  | Spec |   Delta  | GitHub Tag | Delta DTD | CLDR JSON |
|:---:|:----------:|:---------:|:------:|:--------:|:------------:|:---:|:----------:|:---------:|:---------:|
|  48 | 2025-10-~~XX~~ | [v48](/index/downloads/cldr-48) | ~~[CLDR48](https://unicode.org/Public/cldr/48/)~~ | [Charts48](https://unicode.org/cldr/charts/dev) | [LDML48](https://www.unicode.org/reports/tr35/proposed.html) | [Δ48](https://unicode-org.atlassian.net/issues/?jql=project%20%3D%20CLDR%20AND%20status%20%3D%20Done%20AND%20resolution%20%3D%20Fixed%20AND%20fixversion%20%3D%2048%20ORDER%20BY%20priority%20DESC) | ~~[release-48]()~~ | [ΔDtd48](https://www.unicode.org/cldr/charts/dev/supplemental/dtd_deltas.html) | ~~[48.0.0]()~~ |

## Overview

Unicode CLDR provides key building blocks for software supporting the world's languages.
CLDR data is used by all [major software systems](/index#who-uses-cldr)
(including all mobile phones) for their software internationalization and localization,
adapting software to the conventions of different languages.

CLDR 48 was an open submission cycle allowing contributors to supply data for their languages via the CLDR Survey Tool —
data that is widely used to support much of the world’s software.
This data is also a factor in determining which languages are supported on mobile phones and computer operating systems. 

### Changes

The most significant changes in this release are:

- TBD

For more details, see below.

### Locale Coverage Status

#### Current Levels

Count | Level | Usage | Examples
-- | -- | -- | --
xx | Modern | Suitable for full UI internationalization | …
xx | Moderate | Suitable for “document content” internationalization, eg. in spreadsheet | …
xx | Basic | Suitable for locale selection, eg. choice of language on mobile phone | …

#### Changes

| ± | New Level | Locales |
| -- | -- | -- |
| 📈 | Modern | … |
| 📈 | Moderate | … |
| 📈 | Basic | … |
| 📉 | Basic* | … |

\* Note: Each release, the number of items needed for Modern and Moderate increases. So locales without active contributors may drop down in coverage level.

For a full listing, see [Coverage Levels](https://unicode.org/cldr/charts/dev/supplemental/locale_coverage.html)

## Specification Changes

The following are the most significant changes to the specification (LDML).

- TBD

There are many more changes that are important to implementations, such as changes to certain identifier syntax and various algorithms.
See the [Modifications section](https://www.unicode.org/reports/tr35/proposed.html#Modifications) of the specification for details.

## Data Changes

### DTD Changes

- `territories` attribute of `languageData` in [`supplementalData.xml`](https://github.com/unicode-org/cldr/blob/main/common/supplemental/supplementalData.xml) removed. While it was a nice proxy to count the most important territories for each language, it was not clear and it was ripe for mis-understanding. ([CLDR-5708][])

For a full listing, see [Delta DTDs](https://unicode.org/cldr/charts/dev/supplemental/dtd_deltas.html).

### Supplemental Data Changes

- [language_script.tsv](https://github.com/unicode-org/cldr/blob/main/tools/cldr-code/src/main/resources/org/unicode/cldr/util/data/language_script.tsv) updated to include only one "Primary" writing system for languages that used to have multiple options ([CLDR-18114][]). Notable changes are:
  - Panjabi `pa` has the primary  to Gurumukhi `Guru` because widespread usage is in the Gurumukhi script -- while most speakers are in Pakistan `PK`, written usage remains Gurumukhi.
  - Azerbaijani `az` and Northern Kurdish `ku` primarily are used in Latin `Latn`.
  - Chinese languages `zh`, `hak`, and `nan` are matched to Simplified Han writing `Hans` -- except Cantonese `yue`, which is known for a preference in Traditional Han writing `Hant`.
  - Hassiniyya `mey` was missing significant data, it should be associated with the Arabic `Arab` writing system by default, not Latin `Latn`.
- Errors in likely subtags addressed
   - The default language for Belarus `BY` is now Russian `ru`, reflecting modern usage. ([CLDR-14479][])
   - Literary Chinese `lzh` was written in Traditional Han writing `Hant`. ([CLDR-16715][])
- Likely subtags updated because of prior mentioned primary script matches.
  - Northern Kurdish `ku` now matched to Cyrillic writing in the CIS countries. ([CLDR-18114][])
  - Hassiniyya `mey` updated to default to `mey_Arab_DZ` instead of `mey_Latn_SN` ([CLDR-18114][])
  - See other likely subtags updated in [the Supplemental Data Delta page](https://www.unicode.org/cldr/charts/48/delta/supplemental-data.html#Likely)


For a full listing, see [¤¤BCP47 Delta](https://unicode.org/cldr/charts/dev/delta/bcp47.html) and [¤¤Supplemental Delta](https://unicode.org/cldr/charts/dev/delta/supplemental-data.html)

### Locale Changes

- Kurdish (Kurmanji) `ku` split from 1 locale `ku_TR` into 5 locales across 2 scripts and 4 countries. ([CLDR-18311][])
  - `ku_Latn_TR`: Kurdish (Kurmanji, Latin alphabet, Turkey) default for Kurdish (Kurmanji) `ku` and `ku_Latn`
  - `ku_Latn_SY`: Kurdish (Kurmanji, Latin alphabet, Syria)
  - `ku_Latn_IQ`: Kurdish (Kurmanji, Latin alphabet, Iraq)
  - `ku_Arab_IQ`: Kurdish (Kurmanji, Arabic writing, Iraq), default for Kurdish (Kurmanji, Arabic writing) `ku_Arab`
  - `ku_Arab_IR`: Kurdish (Kurmanji, Arabic writing, Iran)

For a full listing, see [Delta Data](https://unicode.org/cldr/charts/dev/delta/index.html)

### Message Format Specification

- TBD

### Collation Data Changes

- TBD

### Number Spellout Data Changes

- TBD

### Segmentation Data Changes

- TBD

### Transform Data Changes

- TBD

### JSON Data Changes

- TBD

### File Changes

- TBD

### Tooling Changes

- TBD

### Keyboard Changes

- TBD

## Migration

- TBD

### V48 advance warnings
The following changes are planned for CLDR 48. Please plan accordingly to avoid disruption.

- Any locales that are missing Core data by the end of the CLDR 48 cycle will be removed [CLDR-16004][]
- The default week numbering will change to ISO instead being based on the calendar week starting in CLDR 48 [CLDR-18275][]. The calendar week data will be more clearly targeted at matching usage in displayed month calendars.
- The likely language for Belarus is slated to change to Russian [CLDR-14479][]
- The major components in [supplementalData.xml](https://github.com/unicode-org/cldr/blob/main/common/supplemental/supplementalData.xml) and [supplementalMetadata.xml](https://github.com/unicode-org/cldr/blob/main/common/supplemental/supplementalMetadata.xml) files are slated to be organized more logically and moved into separate files.
    - This will make it easier for implementations to filter out data that they don't need, and make internal maintenance easier. This will not affect the data: just which file it is located in. Please plan to update XML and JSON parsers accordingly.
- Additionally, language and territory data in `languageData` and `territoryInfo` data will receive significant updates to improve accuracy and maintainability [CLDR-18087][]

### V49 advance warnings

- There is too much uncertainty in the exact values for pre-Meiji Japanese eras,
and there is feedback that the general practice for exact dates is to use Gregorian for pre-Meiji dates.
These are slated for removal in a future release.
Please add a comment to [CLDR-11400] if you use this data and explain your use case if possible.

## Known Issues

- [CLDR-18219][] `common/subdivisions` data files contained additional values that should not be present.
These will be removed in the future, but note that they may be present in the new [JSON data](#json-data-changes):
  - Non-subdivisions such as `AW`:  Use the region code `AW` instead for translation.
  - Overlong subdivisions such as `fi01`: Use the region code `AX` instead for translation.
  

## Acknowledgments

Many people have made significant contributions to CLDR and LDML;
see the [Acknowledgments](/index/acknowledgments) page for a full listing.

The Unicode [Terms of Use](https://unicode.org/copyright.html) apply to CLDR data;
in particular, see [Exhibit 1](https://unicode.org/copyright.html#Exhibit1).

For web pages with different views of CLDR data, see [http://cldr.unicode.org/index/charts](/index/charts).

[CLDR-5708]: https://unicode-org.atlassian.net/browse/CLDR-5708
[CLDR-14479]: https://unicode-org.atlassian.net/browse/CLDR-14479
[CLDR-16004]: https://unicode-org.atlassian.net/browse/CLDR-16004
[CLDR-16715]: https://unicode-org.atlassian.net/browse/CLDR-16715
[CLDR-18087]: https://unicode-org.atlassian.net/browse/CLDR-18087
[CLDR-18113]: https://unicode-org.atlassian.net/browse/CLDR-18113
[CLDR-18114]: https://unicode-org.atlassian.net/browse/CLDR-18114
[CLDR-18219]: https://unicode-org.atlassian.net/browse/CLDR-18219
[CLDR-18275]: https://unicode-org.atlassian.net/browse/CLDR-18275
[CLDR-18311]: https://unicode-org.atlassian.net/browse/CLDR-18311
[CLDR-11400]: https://unicode-org.atlassian.net/browse/CLDR-11400
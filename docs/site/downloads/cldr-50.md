---
title: CLDR 50 Release Note
---

# CLDR 50 Release Note

| No. |    Date    | Rel. Note |  Data  |  Charts  | Spec |   Delta  | GitHub Tag | Delta DTD | CLDR JSON |
|:---:|:----------:|:---------:|:------:|:--------:|:------------:|:---:|:----------:|:---------:|:---------:|
|  50 | 2026-10-22 | [v50] | [CLDR50] | [Charts50] | [Spec50] | [Δ50] | [release-50] | [ΔDtd50] | [JSON50] |

## Overview

Unicode CLDR provides key building blocks for software supporting the world's languages.
CLDR data is used by all [major software systems]
(including all mobile phones) for their software internationalization and localization,
adapting software to the conventions of different languages.

### Changes

The most significant changes in this release are:

- **TBD**

Note: Some features are in *Technical Preview*

For more details, see below.

### Locale Coverage Status

#### Coverage Levels

Count | Level | Usage | Examples
-- | -- | -- | --
**TBD** | Modern | Suitable for full UI internationalization | Afrikaans, shqip, አማርኛ, ‫العربية‬, հայերեն, অসমীয়া, azərbaycan
**TBD** | Moderate | Suitable for “document content” internationalization, eg. in spreadsheet | Akan, Cebuano, Māori, тоҷикӣ
**TBD** | Basic | Suitable for locale selection, eg. choice of language on mobile phone | भोजपुरी, बर’, डोगरी, eʋegbe, Gã, हरियाणवी

Note: This includes just the base language and script. There are many more regional variants.

#### Coverage Changes

| ± | New Level | Locales |
| -- | -- | -- |
| 📈 | Modern | **TBD** |
| 📈 | Moderate | **TBD** |
| 📈 | Basic | **TBD** |
| 📉 | Moderate* | **TBD** |
| 📉 | Basic* | **TBD** |

Notes:

\* For every release, the number of items needed for Modern and Moderate increases. Therefore, locales without active contributors may drop down in coverage level.

‡ Indicates locales that have gone up or down by more than one level.

For a full listing, see [Coverage Levels]

## Specification Changes

The following are the most significant changes to the CLDR specification, aside from those covered under DTD changes below.

- **TBD**

See the [Modifications section] of the specification for details.

### DTD Changes

Added:
- **TBD**

For a full listing, see [Delta DTDs].

## Data Changes

### Supplemental Data Changes

Updates for:
- **TBD**

For a full listing, see [¤¤BCP47 Delta] and [¤¤Supplemental Delta].

### Locale Data Changes

- **TBD**

For a full listing, see [Delta Data].

### Keyboard Changes

- **TBD**

### File Changes

- **TBD**

### JSON Data Changes
#### Package: cldr-localenames-full

- **TBD**

### Tooling Changes

#### Spec Tooling improvements
- **TBD**

####  Survey Tool improvements

- **TBD**


## Migration

The following changes have been made in CLDR 50. Please plan accordingly to avoid disruption.

- **TBD**

### Advanced warnings CLDR 51 and beyond

The following changes are planned for CLDR 51. Please plan accordingly to avoid disruption.

- **TBD**

## Known Issues

- **TBD**

## Acknowledgments

Many people have made significant contributions to CLDR (structure, data, specification, tooling, and process);
see the [Acknowledgments] page for a full listing.

The Unicode [Terms of Use] apply to CLDR data;
in particular, see [Exhibit 1].

For web pages with different views of CLDR data, see [http://cldr.unicode.org/index/charts](/index/charts).

<!-- Header links; need version updating each release, and changing the label text -->
[v50]: /index/downloads/cldr-50
[CLDR50]: https://unicode.org/Public/cldr/50/
[Charts50]: https://unicode.org/cldr/charts/50
[Spec50]: https://www.unicode.org/reports/tr35/tr35-79/tr35.html
[Δ50]: https://unicode-org.atlassian.net/issues?jql=project%3DCLDR%0AAND%20status%3DDone%0AAND%20resolution%3DFixed%0AAND%20fixversion%3D50%0AORDER%20BY%20priority%20DESC
[release-50]: https://github.com/unicode-org/cldr/releases/tag/release-50
[ΔDtd50]: https://www.unicode.org/cldr/charts/50/supplemental/dtd_deltas.html
[JSON50]: https://github.com/unicode-org/cldr-json/releases/tag/50.0.0

<!-- Common body links; need version updating each release, but no change to label text -->
[Delta DTDs]: https://unicode.org/cldr/charts/50/supplemental/dtd_deltas.html
[Delta Data]: https://unicode.org/cldr/charts/50/delta/index.html
[Modifications section]: https://www.unicode.org/reports/tr35/50/tr35-modifications.html#modifications
[¤¤BCP47 Delta]: https://unicode.org/cldr/charts/50/delta/bcp47.html
[¤¤Supplemental Delta]: https://unicode.org/cldr/charts/50/delta/supplemental-data.html
[RBNF tickets for full list]: https://unicode-org.atlassian.net/issues?jql=project%20%3D%20CLDR%0AAND%20status%20%3D%20Done%0AAND%20resolution%20%3D%20Fixed%0AAND%20fixversion%20%3D%2050%0AAND%20component%20%3D%20numbers-rbnf%0AORDER%20BY%20priority%20DESC
[Keyboard Spec Changes in v50]: https://www.unicode.org/reports/tr35/50/tr35-modifications.html#keyboards
[Language Plural Rules chart]: https://www.unicode.org/cldr/charts/50/supplemental/language_plural_rules.html

[Part 5: Collation]: https://www.unicode.org/reports/tr35/50/tr35-collation.html#Contents

<!-- Common constant links for each release page; no update needed for next release -->
[major software systems]: /index#who-uses-cldr
[tr35-78]: https://www.unicode.org/reports/tr35/tr35-78/tr35.html
[Acknowledgments]: /index/acknowledgments
[Terms of Use]: https://unicode.org/copyright.html
[Exhibit 1]: https://unicode.org/copyright.html#Exhibit1
[http://cldr.unicode.org/index/charts]: /index/charts
[Basic coverage level locale data]: /index/cldr-spec/coverage-levels#basic-data
[UTS #35]: https://www.unicode.org/reports/tr35/
[UTS #61]: https://www.unicode.org/reports/tr61/
[UTC]: https://www.unicode.org/consortium/utc.html
[Vetting]: /translation/getting-started/survey-tool-phases#survey-tool-phase-vetting
[CLDR Organization]: https://cldr.unicode.org/index/survey-tool/cldr-organization
[requesting_changes]: /requesting_changes#how-to-file-a-ticket
[UTS #10]: https://www.unicode.org/reports/tr10/tr10-55.html#Modifications

<!-- Specific links for this page: probably deleted for the next release — may need version updates if reused -->
<!-- TODO: delete unused ones -->

[CLDR-10769]: https://unicode-org.atlassian.net/browse/CLDR-10769
[CLDR-16004]: https://unicode-org.atlassian.net/browse/CLDR-16004
[CLDR-18275]: https://unicode-org.atlassian.net/browse/CLDR-18275
[CLDR-18303]: https://unicode-org.atlassian.net/browse/CLDR-18303
[CLDR-18624]: https://unicode-org.atlassian.net/browse/CLDR-18624
[CLDR-19046]: https://unicode-org.atlassian.net/browse/CLDR-19046
[CLDR-19060]: https://unicode-org.atlassian.net/browse/CLDR-19060
[CLDR-19143]: https://unicode-org.atlassian.net/browse/CLDR-19143
[CLDR-19218]: https://unicode-org.atlassian.net/browse/CLDR-19218
[CLDR-19483]: https://unicode-org.atlassian.net/browse/CLDR-19483
[CLDR-19608]: https://unicode-org.atlassian.net/browse/CLDR-19608
[CLDR-19752]: https://unicode-org.atlassian.net/browse/CLDR-19752
[CLDR-19754]: https://unicode-org.atlassian.net/browse/CLDR-19754
[CLDR-19757]: https://unicode-org.atlassian.net/browse/CLDR-19757
[CLDR-19806]: https://unicode-org.atlassian.net/browse/CLDR-19806
[CLDR-19193]: https://unicode-org.atlassian.net/browse/CLDR-19193
[CLDR-19774]: https://unicode-org.atlassian.net/browse/CLDR-19774

[plurals tickets for full list]: https://unicode-org.atlassian.net/issues?jql=project%20%3D%20CLDR%20AND%20status%20%3D%20Done%20AND%20resolution%20%3D%20Fixed%20AND%20fixversion%20%3D%2050%20AND%20component%20%3D%20plurals%0AORDER%20BY%20priority%20DESC
[/properties/labels.txt]: https://github.com/unicode-org/cldr/blob/main/common/properties/labels.txt
[UnicodeSet]: https://unicode.org/reports/tr35/#Unicode_Sets
[pathcoverage]: https://github.com/unicode-org/cldr/blob/main/common/pathCoverage/README.md

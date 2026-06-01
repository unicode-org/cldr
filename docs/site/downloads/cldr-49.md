---
title: CLDR 49 Release Note
---

# CLDR 49 Release Note

| No. |    Date    | Rel. Note |  Data  |  Charts  | Spec |   Delta  | GitHub Tag | Delta DTD | CLDR JSON |
|:---:|:----------:|:---------:|:------:|:--------:|:------------:|:---:|:----------:|:---------:|:---------:|
|  49 | 2026-03-~~XX~~ | [v49](/index/downloads/cldr-49) | ~~[CLDR49](https://unicode.org/Public/cldr/49/)~~ | [Charts49](https://unicode.org/cldr/charts/dev) | [LDML49](https://www.unicode.org/reports/tr35/49/tr35.html) | [Δ49](https://unicode-org.atlassian.net/issues/?jql=project%20%3D%20CLDR%20AND%20status%20%3D%20Done%20AND%20resolution%20%3D%20Fixed%20AND%20fixversion%20%3D%2049%20ORDER%20BY%20priority%20DESC) | ~~[release-49]()~~ | [ΔDtd48](https://www.unicode.org/cldr/charts/dev/supplemental/dtd_deltas.html) | ~~[49.0.0]()~~ |

## Overview

Unicode CLDR provides key building blocks for software supporting the world's languages.
CLDR data is used by all [major software systems](/index#who-uses-cldr)
(including all mobile phones) for their software internationalization and localization,
adapting software to the conventions of different languages.


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

- TBD

For a full listing, see [Delta DTDs](https://unicode.org/cldr/charts/dev/supplemental/dtd_deltas.html).

### Supplemental Data Changes

- TBD

For a full listing, see [¤¤BCP47 Delta](https://unicode.org/cldr/charts/dev/delta/bcp47.html) and [¤¤Supplemental Delta](https://unicode.org/cldr/charts/dev/delta/supplemental-data.html)

### Locale Changes

- TBD

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

### V49 advance warnings

The following changes are planned for CLDR 49. Please plan accordingly to avoid disruption.
- H24 will be deprecated. If it is encountered, it will have H23 behavior. There is no known intentional usage of H24. If you have a current need for H24 instead of H23, please comment on [CLDR-18303][].
- As of CLDR 49, the default week numbering system will change to follow ISO (where weeks are numbered based on Thursday), instead of being based on the start of the calendar week. The calendar week will be more clearly targeted at matching usage in displayed month calendars. [CLDR-18275][]
- The pre-Meiji Japanese eras will be removed: There was too much uncertainty in the exact values
and feedback that the general practice for exact dates is to use Gregorian for pre-Meiji dates.
- The major components in [supplementalData.xml](https://github.com/unicode-org/cldr/blob/main/common/supplemental/supplementalData.xml) and [supplementalMetadata.xml](https://github.com/unicode-org/cldr/blob/main/common/supplemental/supplementalMetadata.xml) files are slated to be organized more logically and moved into separate files.
    - This will make it easier for implementations to filter out data that they don't need, and make internal maintenance easier. This will not affect the data, just which file it is located in. Please plan to update XML and JSON parsers accordingly.
 
### V50 advance warnings

The following changes are planned for CLDR 50. Please plan accordingly to avoid disruption.

- Locales which do not have Core data will be removed if still missing core data by alpha. [CLDR-16004]

## Known Issues

- ISO 3166-2 subdivision codes for Iran changed in 2020, and there are not yet new equivalent stable codes. See [CLDR-19046][] for more details.
  

## Acknowledgments

Many people have made significant contributions to CLDR and LDML;
see the [Acknowledgments](/index/acknowledgments) page for a full listing.

The Unicode [Terms of Use](https://unicode.org/copyright.html) apply to CLDR data;
in particular, see [Exhibit 1](https://unicode.org/copyright.html#Exhibit1).

For web pages with different views of CLDR data, see [http://cldr.unicode.org/index/charts](/index/charts).

[CLDR-16004]: https://unicode-org.atlassian.net/browse/CLDR-16004
[CLDR-18275]: https://unicode-org.atlassian.net/browse/CLDR-18275
[CLDR-18303]: https://unicode-org.atlassian.net/browse/CLDR-18303
[CLDR-19046]: https://unicode-org.atlassian.net/browse/CLDR-19046

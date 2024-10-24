---
title: CLDR 47 Release Note
---

# CLDR 47 Release Note

| No. |    Date    | Rel. Note |  Data  |  Charts  | Spec |   Delta  | GitHub Tag | Delta DTD | CLDR JSON |
|:---:|:----------:|:---------:|:------:|:--------:|:------------:|:---:|:----------:|:---------:|:---------:|
|  47 | 2025-04-~~XX~~ | [v47](/index/downloads/cldr-47) | ~~[CLDR47](https://unicode.org/Public/cldr/47/)~~ | [Charts47](https://unicode.org/cldr/charts/dev) | [LDML47](https://www.unicode.org/reports/tr35/proposed.html) | [Δ47](https://unicode-org.atlassian.net/issues/?jql=project+%3D+CLDR+AND+status+%3D+Done+AND+resolution+%3D+Fixed+AND+fixVersion+%3D+%2247%22+ORDER+BY+priority+DESC) | ~~[release-47]()~~ | [ΔDtd47](https://www.unicode.org/cldr/charts/dev/supplemental/dtd_deltas.html) | ~~[47.0.0]()~~ |

## Overview

Unicode CLDR provides key building blocks for software supporting the world's languages.
CLDR data is used by all [major software systems](/index#who-uses-cldr)
(including all mobile phones) for their software internationalization and localization,
adapting software to the conventions of different languages.

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

## [Specification Changes](https://www.unicode.org/reports/tr35/proposed.html)

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

### [Locale Changes](https://unicode.org/cldr/charts/dev/delta/index.html)

- TBD

For a full listing, see [Delta Data](https://unicode.org/cldr/charts/dev/delta/index.html)

### Message Format Specification

- TBD

### Emoji Search Keywords

- TBD

### Collation Data Changes

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

## Known Issues

1. [CLDR-17095] The region-based firstDay value (see weekData) is currently used for several different purposes. In the future, some of these functions will be separated out:
    - The day that should be shown as the first day of the week in a calendar view.
    - The first day of the week (day 1) for weekday numbering.
    - The first day of the week for week-of-year calendar calculations.

## Acknowledgments

Many people have made significant contributions to CLDR and LDML;
see the [Acknowledgments](/index/acknowledgments) page for a full listing.

The Unicode [Terms of Use](https://unicode.org/copyright.html) apply to CLDR data;
in particular, see [Exhibit 1](https://unicode.org/copyright.html#Exhibit1).

For web pages with different views of CLDR data, see [http://cldr.unicode.org/index/charts](/index/charts).


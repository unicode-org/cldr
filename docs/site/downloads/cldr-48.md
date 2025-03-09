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

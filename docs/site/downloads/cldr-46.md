# CLDR 46 Release Note

| No. |    Date    | Rel. Note |  Data  |  Charts  | Spec |   Delta  | GitHub Tag | Delta DTD | CLDR JSON |
|:---:|:----------:|:---------:|:------:|:--------:|:------------:|:---:|:----------:|:---------:|:---------:|
|  46 | 2024-010-~~XX~~ |    ~~[v46]()~~   | ~~[CLDR46](http://unicode.org/Public/cldr/46/)~~ | [Charts46](http://unicode.org/cldr/charts/dev) |    [LDML46](http://www.unicode.org/reports/tr35/proposed.html)    | [Δ46](https://unicode-org.atlassian.net/issues/?jql=project+%3D+CLDR+AND+status+%3D+Done+AND+resolution+%3D+Fixed+AND+fixVersion+%3D+%2246%22+ORDER+BY+priority+DESC) | ~~[release-46]()~~ |   [ΔDtd46](https://www.unicode.org/cldr/charts/dev/supplemental/dtd_deltas.html)  |   ~~[46.0.0](https://github.com/unicode-org/cldr-json/releases/tag/46.0.0)~~  |

## Overview

Unicode CLDR provides key building blocks for software supporting the world's languages. 
CLDR data is used by all [major software systems](https://cldr.unicode.org/index#TOC-Who-uses-CLDR-) (including all mobile phones) for their software internationalization and localization, adapting software to the conventions of different languages.

The largest changes in this release were the updates to Unicode 16.0 and substantial additions of Emoji search keyword data.

± | New Coverage Level | Locales
-- | --|--
📈 | Modern | Nigerian Pidgin, Tigrinya
📈 | Moderate | Akan, Baluchi (Latin), Kangri, Tajik, Tatar, Wolof
📈 | Basic | Ewe, Ga, Kinyarwanda, Konkani (Latin), Northern Sotho, Oromo, Sichuan Yi, Southern Sotho, Tswana
📉 | Basic* | Chuvash, Anii

\* Note: Above Basic, the ‘bar’ for each release increases. So locales without active contributors may backslide

## [Specification Changes](http://www.unicode.org/reports/tr35/tr35.html)

**TBD**

## Data Changes

### DTD Changes 

1. Added alt='official' to represent cases where an official value differs from the customary value. Currently added for a small number of language names, decimal separators, and grouping separators
**TBD**
2. Added new numbering systems from Unicode 16.0

For a full listing, see [Delta DTDs](https://unicode.org/cldr/charts/46/supplemental/dtd_deltas.html).

### Supplemental Data Changes

1. Currency. New currency code ZWG added — because it was late in the cycle, many locales will just support the code.
**TBD**
2. Timezones.
    1. Changed Kazakhstan to reflect removal of Asia/Almaty
	2. Deprecated timezone ids. Altered the handling of: CST6CDT, EST, EST5EDT, MST7MDT, PST8PDT
3. Units
    1. Added units: portion-per-1e9 (aka per-billion), night (for hotel stays), light (as a prefix for light-second, light-minute, etc.)
	2. Changed preferred wind speed preference for some locales to 	meter-per-second
4. Updated: language IDs, likelySubtags, region gdp and language populations, etc.
   1. Minimization for likelySubtags removes some additional redundant mappings
5. Transforms.
   1. Major update to Han → Latn, reflecting new data in Unicode 16.0
   2. Fixes for Arabic numbers, a Farsi vowel

For a full listing, see [¤¤BCP47 Delta](https://unicode.org/cldr/charts/46/delta/bcp47.html) and [¤¤Supplemental Delta](https://unicode.org/cldr/charts/46/delta/supplemental-data.html)

### [Locale Changes](https://unicode.org/cldr/charts/46/delta/index.html)

1. Major changes to emoji search keywords and short names
    1. Data imported from WhatsApp
	2. Increased the maximum number of search keywords
	3. Revision of many search keywords to break up phrases
2. Major changes to Chinese collation, reflecting new data in Unicode 16.0
3. Other changes
    a. Locales also had smaller improvements agreed to by translators. 
**TBD**

For a full listing, see [Delta Data](https://unicode.org/cldr/charts/46/delta/index.html)

### JSON Data Changes

**TBD**

### File Changes
The following files were added:

**TBD**

### Tooling Changes

**TBD**

## Growth

**TBD**

## Migration

**TBD**

## [Known Issues](https://unicode-org.atlassian.net/issues/CLDR-17535?jql=project%20%3D%20cldr%20and%20labels%20%3D%20%22ReleaseKnownIssue%22%20and%20status%20!%3D%20done)

**TBD**

## Acknowledgments

Many people have made significant contributions to CLDR and LDML; see the [Acknowledgments](https://cldr.unicode.org/index/acknowledgments) page for a full listing.

The Unicode [Terms of Use](https://unicode.org/copyright.html) apply to CLDR data; in particular, see [Exhibit 1](https://unicode.org/copyright.html#Exhibit1).

For web pages with different views of CLDR data, see [http://cldr.unicode.org/index/charts](https://cldr.unicode.org/index/charts).

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)

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

   - New locales:
      - Core data for Coptic (cop) , Haitian Creole (ht)
      - Locale data for 11 English locales and Cantonese (Macau) (yue_Hant_MO)
   - Updated time zone data to tzdata 2025a
   - [RBNF](#number-spellout-data-changes) (Number Spellout Data Improvements) for multiple languages
   - Assorted transforms improvements
   - Updated language matching for Afrikaans to English (en) from Dutch (nl) [CLDR-18198](https://unicode-org.atlassian.net/browse/CLDR-18198)
   - Ordered scripts in decending order of usage per locale [CLDR-18155](https://unicode-org.atlassian.net/browse/CLDR-18155)
   - Fixed invalid codes [CLDR-18129](https://unicode-org.atlassian.net/browse/CLDR-18129)
   - Updated population data

For more details, see below.

### Locale Coverage Status

CLDR 47 was a closed cycle which means that locale data changes were limited to addition of new locales, and bug fixes.
This means that coverage levels for existing locales did not change in this release.

#### Current Levels

Count | Level | Usage | Examples
-- | -- | -- | --
97 | Modern | Suitable for full UI internationalization | čeština, Ελληνικά‎, Беларуская‎, ‎ᏣᎳᎩ‎, Ქართული‎, ‎Հայերեն‎, ‎עברית‎, ‎اردو‎, አማርኛ‎, ‎नेपाली‎, অসমীয়া‎, ‎বাংলা‎, ‎ਪੰਜਾਬੀ‎, ‎ગુજરાતી‎, ‎ଓଡ଼ିଆ‎, தமிழ்‎, ‎తెలుగు‎, ‎ಕನ್ನಡ‎, ‎മലയാളം‎, ‎සිංහල‎, ‎ไทย‎, ‎ລາວ‎, မြန်မာ‎, ‎ខ្មែរ‎, ‎한국어‎, 中文, 日本語‎, … ‎|
16 | Moderate | Suitable for “document content” internationalization, eg. in spreadsheet | Akan, Balóchi [Látin], brezhoneg, Cebuano, føroyskt, IsiXhosa, Māori, sardu, veneto, Wolof, татар, тоҷикӣ, कांगड़ी‎, … |
55 | Basic | Suitable for locale selection, eg. choice of language on mobile phone | Basa Sunda, emakhuwa, Esperanto, eʋegbe, Frysk, Malti, босански (ћирилица), କୁୱି (ଅଡ଼ିଆ), కువి (తెలుగు), ᱥᱟᱱᱛᱟᱲᱤ, ᓀᐦᐃᓇᐍᐏᐣ‬, ꆈꌠꉙ‎, … |

\* Note: Each release, the number of items needed for Modern and Moderate increases. So locales without active contributors may drop down in coverage level.

For a full listing, see [Coverage Levels](https://unicode.org/cldr/charts/dev/supplemental/locale_coverage.html)

## [Specification Changes](https://www.unicode.org/reports/tr35/proposed.html)

The following are the most significant changes to the specification (LDML).

- Don't produce "Unknown City Time" for VVV and VVVV, use localized offset format instead [CLDR-18237](https://unicode-org.atlassian.net/browse/CLDR-18237))

There are many more changes that are important to implementations, such as changes to certain identifier syntax and various algorithms.
See the [Modifications section](https://www.unicode.org/reports/tr35/proposed.html#Modifications) of the specification for details.

## Data Changes

### DTD Changes

- TBD

For a full listing, see [Delta DTDs](https://unicode.org/cldr/charts/dev/supplemental/dtd_deltas.html).

### Supplemental Data Changes

- Ordered scripts in decending order of usage per locale [CLDR-18155](https://unicode-org.atlassian.net/browse/CLDR-18155)
- Updated language matching for Afrikaans to English (en) from Dutch (nl) [CLDR-18198](https://unicode-org.atlassian.net/browse/CLDR-18198)
- Fixed invalid codes [CLDR-18129](https://unicode-org.atlassian.net/browse/CLDR-18129)
- TBD

For a full listing, see [¤¤BCP47 Delta](https://unicode.org/cldr/charts/dev/delta/bcp47.html) and [¤¤Supplemental Delta](https://unicode.org/cldr/charts/dev/delta/supplemental-data.html)

### [Locale Changes](https://unicode.org/cldr/charts/dev/delta/index.html)

- TBD

For a full listing, see [Delta Data](https://unicode.org/cldr/charts/dev/delta/index.html)

### Message Format Specification

- TBD

### Collation Data Changes

- TBD

### JSON Data Changes

- TBD

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

- Assorted SurveyTool improvements including:
   - Added a CLA check
   - 
- Improved validity checks for codes [CLDR-18129](https://unicode-org.atlassian.net/browse/CLDR-18129)
- Improved ability to detect invalid URLs in the site and spec

### Keyboard Changes

- TBD

## Migration

- Number `<symbols>` elements and format elements (`<currencyFormats>`, `<decimalFormats>`, `<percentFormats>`, `<scientificFormats>`)
  should all have a `numberSystem` attribute. In CLDR v48 such elements without a `numberSystem` attribute will be deprecated, and the
  corresponding entries in root will be removed; these were only intended as a long-ago migration aid. See the relevant sections of the
  LDML specification: [Number Symbols](https://www.unicode.org/reports/tr35/dev/tr35-numbers.html#Number_Symbols) and
  [Number Formats](https://www.unicode.org/reports/tr35/dev/tr35-numbers.html#number-formats).
- Any locales that are missing Core data by the end of the CLDR 48 cycle will be removed [CLDR-16004](https://unicode-org.atlassian.net/browse/CLDR-16004)
- The default week numbering will change to ISO instead being based on the calendar week starting in CLDR 48 [CLDR-18275](https://unicode-org.atlassian.net/browse/CLDR-18275).

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


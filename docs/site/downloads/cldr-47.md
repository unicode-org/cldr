---
title: CLDR 47 Release Note
---

# CLDR 47 Release Note

| No. |    Date    | Rel. Note |  Data  |  Charts  | Spec |   Delta  | GitHub Tag | Delta DTD | CLDR JSON |
|:---:|:----------:|:---------:|:------:|:--------:|:------------:|:---:|:----------:|:---------:|:---------:|
|  47 | 2025-04-~~XX~~ | [v47](/index/downloads/cldr-47) | ~~[CLDR47](https://unicode.org/Public/cldr/47/)~~ | [Charts47](https://unicode.org/cldr/charts/dev) | [LDML47](https://www.unicode.org/reports/tr35/proposed.html) | [Δ47](https://unicode-org.atlassian.net/issues/?jql=project+%3D+CLDR+AND+status+%3D+Done+AND+resolution+%3D+Fixed+AND+fixVersion+%3D+%2247%22+ORDER+BY+priority+DESC) | ~~release-47~~<br>[release-47-alpha2](https://github.com/unicode-org/cldr/releases/tag/release-47-alpha2) | [ΔDtd47](https://www.unicode.org/cldr/charts/dev/supplemental/dtd_deltas.html) | ~~47.0.0~~<br>[47.0.0-ALPHA2](https://github.com/unicode-org/cldr-json/releases/tag/47.0.0-ALPHA2) |

<span style="color:red; font-weight: bold;">This is an alpha version of CLDR v47.</span>

## Overview

Unicode CLDR provides key building blocks for software supporting the world's languages.
CLDR data is used by all [major software systems](/index#who-uses-cldr)
(including all mobile phones) for their software internationalization and localization,
adapting software to the conventions of different languages.

CLDR 47 focused on MessageFormat 2.0 and tooling for an expansion of DDL support.
It was a closed cycle: locale data changes were limited to bug fixes and the addition of new locales, mostly regional variants.

### Changes

The most significant changes in this release are:

   - New locales:
      - Core data for Coptic (cop) , Haitian Creole (ht)
      - Locale data for 11 English locales and Cantonese (Macau) (yue_Hant_MO)
   - Updated time zone data to tzdata 2025a
   - [RBNF](#number-spellout-data-changes) (Number Spellout Data Improvements) for multiple languages
   - Assorted transforms improvements
   - Updated and revised population data
   - Incorporates all changes from CLDR v46.1.
        - [CLDR v46.1](https://cldr.unicode.org/downloads/cldr-46#461-changes) was a special release, which many users of CLDR (including ICU) have not updated to.
So the listed changes are relative to [CLDR v46.0](https://cldr.unicode.org/downloads/cldr-46). v46.1 included the following:
       - Message Format 2.0 (Final Candidate)
       - More explicit well-formedness and validity constraints for unit of measurement identifiers
       - Addition of derived emoji annotations that were missing: emoji with skin tones facing right
       - Fixes to make the ja, ko, yue, zh datetimeSkeletons useful for generating the standard patterns
       - Improved date/time test data

For more details, see below.

### Locale Coverage Status

#### Current Levels

Count | Level | Usage | Examples
-- | -- | -- | --
97 | Modern | Suitable for full UI internationalization | čeština, Ελληνικά‎, Беларуская‎, ‎ᏣᎳᎩ‎, Ქართული‎, ‎Հայերեն‎, ‎עברית‎, ‎اردو‎, አማርኛ‎, ‎नेपाली‎, অসমীয়া‎, ‎বাংলা‎, ‎ਪੰਜਾਬੀ‎, ‎ગુજરાતી‎, ‎ଓଡ଼ିଆ‎, தமிழ்‎, ‎తెలుగు‎, ‎ಕನ್ನಡ‎, ‎മലയാളം‎, ‎සිංහල‎, ‎ไทย‎, ‎ລາວ‎, မြန်မာ‎, ‎ខ្មែរ‎, ‎한국어‎, 中文, 日本語‎, … ‎|
16 | Moderate | Suitable for “document content” internationalization, eg. in spreadsheet | Akan, Balóchi [Látin], brezhoneg, Cebuano, føroyskt, IsiXhosa, Māori, sardu, veneto, Wolof, татар, тоҷикӣ, कांगड़ी‎, … |
55 | Basic | Suitable for locale selection, eg. choice of language on mobile phone | Basa Sunda, emakhuwa, Esperanto, eʋegbe, Frysk, Malti, босански (ћирилица), କୁୱି (ଅଡ଼ିଆ), కువి (తెలుగు), ᱥᱟᱱᱛᱟᱲᱤ, ᓀᐦᐃᓇᐍᐏᐣ‬, ꆈꌠꉙ‎, … |

\* Note: Each release, the number of items needed for Modern and Moderate increases. So locales without active contributors may drop down in coverage level.

For a full listing, see [Coverage Levels](https://unicode.org/cldr/charts/dev/supplemental/locale_coverage.html)

## Specification Changes

**NOTE: the specification changes will be completed by the specification beta: only a few of them are listed here, and the Modifications section is not yet complete.**

The following are the most significant changes to the specification (LDML).

- Don't produce "Unknown City Time" for VVV and VVVV, use localized offset format instead [CLDR-18237](https://unicode-org.atlassian.net/browse/CLDR-18237))

There are many more changes that are important to implementations, such as changes to certain identifier syntax and various algorithms.
See the [Modifications section](https://www.unicode.org/reports/tr35/proposed.html#Modifications) of the specification for details.

## Data Changes
**TBD: Flesh out overview items**
   - Updated language matching for Afrikaans to English (en) from Dutch (nl) [CLDR-18198](https://unicode-org.atlassian.net/browse/CLDR-18198)
   - Ordered scripts in `<languageData>` in descending order of usage per locale [CLDR-18155](https://unicode-org.atlassian.net/browse/CLDR-18155)
   - Fixed certain invalid codes [CLDR-18129](https://unicode-org.atlassian.net/browse/CLDR-18129)

### DTD Changes

Most of the DTD changes were in 46.1. One additional change was to order currency values in **TBD get ticket number**

For a full listing, see [Delta DTDs](https://unicode.org/cldr/charts/dev/supplemental/dtd_deltas.html).

### Supplemental Data Changes

- Ordered scripts in decending order of usage per locale [CLDR-18155](https://unicode-org.atlassian.net/browse/CLDR-18155)
- Updated language matching for Afrikaans to English (en) from Dutch (nl) [CLDR-18198](https://unicode-org.atlassian.net/browse/CLDR-18198)
- Fixed invalid codes [CLDR-18129](https://unicode-org.atlassian.net/browse/CLDR-18129)
- TBD

For a full listing, see [¤¤BCP47 Delta](https://unicode.org/cldr/charts/dev/delta/bcp47.html) and [¤¤Supplemental Delta](https://unicode.org/cldr/charts/dev/delta/supplemental-data.html)

### Locale Changes

- Cleanups for current pattern variants `alt="alphaNextToNumber"` and `alt="noCurrency"`: These were introduced in CLDR 42
(per [CLDR-14336](https://unicode-org.atlassian.net/browse/CLDR-14336)) to provide a cleaner way of adjusting currency
patterns when an alphabetic currency symbol is used, or when a currency-style pattern is desired without a currency symbol
(as for use in a table). Gaps in the data coverage showed up, because the translators weren't shown the right values. 
Fixes were made in  [CLDR-17879](https://unicode-org.atlassian.net/browse/CLDR-17879).
- As noted below in [Migration](#migration), number `<symbols>` elements and format elements (`<currencyFormats>`, `<decimalFormats>`, `<percentFormats>`, `<scientificFormats>`)
should all have a `numberSystem` attribute, and such elements without a `numberSystem` attribute will be deprecated in CLDR 48. To
prepare for this, in CLDR 47, all such elements were either removed (if redundant) or correct by adding a `numberSystem` attribute.
([CLDR-17760](https://unicode-org.atlassian.net/browse/CLDR-17760))

For a full listing, see [Delta Data](https://unicode.org/cldr/charts/dev/delta/index.html)

### Message Format Specification

- TBD

### Collation Data Changes

- Two old `zh` collation variants are removed: big5han and gb2312.
They are no longer typically used, and only cover a fraction of the CJK ideographs.
([CLDR-16062](https://unicode-org.atlassian.net/browse/CLDR-16062))

### Number Spellout Data Changes

- Number spellout rules are added for Gujarati.
([CLDR-18111](https://unicode-org.atlassian.net/browse/CLDR-18111))
- Number spellout rules are improved for several other languages:
    - Bulgarian: Improve usage of ‘и’ (“and”). ([CLDR-17818](https://unicode-org.atlassian.net/browse/CLDR-17818))
    - Catalan: Add plural ordinal rules for both masculine and feminine, other fixes. ([CLDR-15972](https://unicode-org.atlassian.net/browse/CLDR-15972))
    - Dutch: Add the alternate spellout-cardinal-stressed rule for specific Dutch scenarios. ([CLDR-17187](https://unicode-org.atlassian.net/browse/CLDR-17187))
    - Hindi: Add the spellout-ordinal-masculine-oblique rule. ([CLDR-15278](https://unicode-org.atlassian.net/browse/CLDR-15278))
    - Indonesian: Add missing semicolon that caused all ordinals to be prefixed with “pertama 2:”. ([CLDR-17730](https://unicode-org.atlassian.net/browse/CLDR-17730))
    - Lithuanian: Add all of the grammatical cases, genders and grammatical numbers for cardinals and ordinals (no  pronomial forms, and only the positive degree). ([CLDR-18110](https://unicode-org.atlassian.net/browse/CLDR-18110))
    - Russian: Fix grammatcial case names in rules, and other issues. ([CLDR-17386](https://unicode-org.atlassian.net/browse/CLDR-17386))
    - Ukrainian: Add digits-ordinal rules. ([CLDR-16096](https://unicode-org.atlassian.net/browse/CLDR-16096))

### Segmentation Data Changes

- The word break tailorings for `fi` and `sv` are removed to align with recent changes to the root collation
and recent changes to ICU behavior. ([CLDR-18272](https://unicode-org.atlassian.net/browse/CLDR-18272))

### Transform Data Changes

- A new `Hant-Latn` transform is added, and `Hans-Latn` is added as an alias for the existing `Hani-Latn` transform.
When the Unihan data `kMandarin` field has two values,
the first is preferred for a `CN`/`Hans` context, and is used by the `Hani-Latn`/`Hans-Latn` transform;
the second is preferred for a `TW`/`Hant` context, and is now used by the new `Hant-Latn` transform.
([CLDR-18080](https://unicode-org.atlassian.net/browse/CLDR-18080))

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

There were various SurveyTool improvements targeting expansion of DDL support and error detection, such as the following:
   - Added a CLA check
   - Improved validity checks for codes [CLDR-18129](https://unicode-org.atlassian.net/browse/CLDR-18129)
   - Improved ability to detect invalid URLs in the site and spec

### Keyboard Changes

- TBD

## Migration

- Removal of number data without `numberSystem` attributes.
    - Number `<symbols>` elements and format elements (`<currencyFormats>`, `<decimalFormats>`, `<percentFormats>`, `<scientificFormats>`)
should all have a `numberSystem` attribute. In CLDR v48 such elements without a `numberSystem` attribute will be deprecated, and the
corresponding entries in root will be removed; these were only intended as a long-ago migration aid. See the relevant sections of the
LDML specification: [Number Symbols](https://www.unicode.org/reports/tr35/dev/tr35-numbers.html#Number_Symbols) and
[Number Formats](https://www.unicode.org/reports/tr35/dev/tr35-numbers.html#number-formats).
- V48 advance warnings
    - Any locales that are missing Core data by the end of the CLDR 48 cycle will be removed [CLDR-16004](https://unicode-org.atlassian.net/browse/CLDR-16004)
    - The default week numbering will change to ISO instead being based on the calendar week starting in CLDR 48 [CLDR-18275](https://unicode-org.atlassian.net/browse/CLDR-18275).

## Known Issues

## Acknowledgments

Many people have made significant contributions to CLDR and LDML;
see the [Acknowledgments](/index/acknowledgments) page for a full listing.

The Unicode [Terms of Use](https://unicode.org/copyright.html) apply to CLDR data;
in particular, see [Exhibit 1](https://unicode.org/copyright.html#Exhibit1).

For web pages with different views of CLDR data, see [http://cldr.unicode.org/index/charts](/index/charts).


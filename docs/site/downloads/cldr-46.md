---
title: CLDR 46 Release Note
---

# CLDR 46 Release Note

| No. |    Date    | Rel. Note |  Data  |  Charts  | Spec |   Delta  | GitHub Tag | Delta DTD | CLDR JSON |
|:---:|:----------:|:---------:|:------:|:--------:|:------------:|:---:|:----------:|:---------:|:---------:|
|  46 | 2024-10-~~XX~~ |    ~~[v46]()~~   | ~~[CLDR46](http://unicode.org/Public/cldr/46/)~~ | [Charts46](http://unicode.org/cldr/charts/dev) |    [LDML46](http://www.unicode.org/reports/tr35/proposed.html)    | [Δ46](https://unicode-org.atlassian.net/issues/?jql=project+%3D+CLDR+AND+status+%3D+Done+AND+resolution+%3D+Fixed+AND+fixVersion+%3D+%2246%22+ORDER+BY+priority+DESC) | ~~[release-46]()~~ |   [ΔDtd46](https://www.unicode.org/cldr/charts/dev/supplemental/dtd_deltas.html)  |   ~~[46.0.0](https://github.com/unicode-org/cldr-json/releases/tag/46.0.0)~~  |

## Overview

Unicode CLDR provides key building blocks for software supporting the world's languages.
CLDR data is used by all [major software systems](https://cldr.unicode.org/index#TOC-Who-uses-CLDR-) 
(including all mobile phones) for their software internationalization and localization, 
adapting software to the conventions of different languages.

The largest changes in this release were the updates to Unicode 16.0, substantial additions of Emoji search keyword data, and ‘upleveling’ the locale coverage.

### Locale Coverage Status
#### Current Levels

Count | Level | Usage | Examples
-- | -- | -- | --
97 | Modern | Suitable for full UI internationalization | čeština, Ελληνικά‎, Беларуская‎, ‎ᏣᎳᎩ‎, Ქართული‎, ‎Հայերեն‎, ‎עברית‎, ‎اردو‎, አማርኛ‎, ‎नेपाली‎, অসমীয়া‎, ‎বাংলা‎, ‎ਪੰਜਾਬੀ‎, ‎ગુજરાતી‎, ‎ଓଡ଼ିଆ‎, தமிழ்‎, ‎తెలుగు‎, ‎ಕನ್ನಡ‎, ‎മലയാളം‎, ‎සිංහල‎, ‎ไทย‎, ‎ລາວ‎, မြန်မာ‎, ‎ខ្មែរ‎, ‎한국어‎, 中文, 日本語‎, … ‎
16 | Moderate | Suitable for “document content” internationalization, eg. in spreadsheet | Akan, Balóchi [Látin], brezhoneg, Cebuano, føroyskt, IsiXhosa, Māori, sardu, veneto, Wolof, татар, тоҷикӣ, कांगड़ी‎, …
55 | Basic | Suitable for locale selection, eg. choice of language on mobile phone | Basa Sunda, emakhuwa, Esperanto, eʋegbe, Frysk, Malti, босански (ћирилица), କୁୱି (ଅଡ଼ିଆ), కువి (తెలుగు), ᱥᱟᱱᱛᱟᱲᱤ, ᓀᐦᐃᓇᐍᐏᐣ‬, ꆈꌠꉙ‎, …

#### Changes

| ± | New Level | Locales |
| -- | -- | -- |
| 📈 | Modern | Nigerian Pidgin, Tigrinya |
| 📈 | Moderate | Akan, Baluchi (Latin), Kangri, Tajik, Tatar, Wolof |
| 📈 | Basic | Ewe, Ga, Kinyarwanda, Konkani (Latin), Northern Sotho, Oromo, Sichuan Yi, Southern Sotho, Tswana |
| 📉 | Basic* | Chuvash, Anii |

\* Note: Each release, the number of items needed for Modern and Moderate increases. So locales without active contributors may drop down in coverage level.

For a full listing, see [Coverage Levels](https://unicode.org/cldr/charts/46/supplemental/locale_coverage.html)

## [Specification Changes](http://www.unicode.org/reports/tr35/tr35.html)

**TBD: Add the specification changes by Sept 25**

**Note: This will include Message Format 2.0 specification changes. A draft is at https://github.com/unicode-org/message-format-wg/tree/main/spec**

## Data Changes

### DTD Changes

1. Added `alt='official'` to represent cases where an official value differs from the customary value.
Currently added for a small number of language names, decimal separators, and grouping separators.
2. Added new numbering systems from Unicode 16.0.

For a full listing, see [Delta DTDs](https://unicode.org/cldr/charts/46/supplemental/dtd_deltas.html).

### Supplemental Data Changes

1. Currency
    1. New currency code `ZWG` added — because it was late in the cycle, many locales will just support the code (no symbol or name).
2. Timezones and Metazones
    1. Changed the metazone for Kazakhstan to reflect removal of Asia/Almaty, thus dropping the distinction among different regions in Kazakhstan.
    2. Added support for deprecated codes by remapping: `CST6CDT → America/Chicago`, `EST → America/Panama`, `EST5EDT → America/New_York`, `MST7MDT → America/Denver`, `PST8PDT → America/Los_Angeles`.
3. Units
    1. Added units: `portion-per-1e9` (aka per-billion), `night` (for hotel stays), `light-speed` (as an internal prefix for **light-second**, **light-minute**, etc.)
    2. Changed preferred wind speed preference for some locales to `meter-per-second`.
More preference changes are planned for the next release.
4. Minimization for likelySubtags removes many additional redundant mappings.
    - For example, the mapping `acy_Grek → acy_Grek_CY` is unnecessary, because the mapping `acy → acy_Latn_CY` is sufficient.
For the reason why, see the algorithm in [Likely Subtags](https://cldr-smoke.unicode.org/spec/main/ldml/tr35.html#likely-subtags).
    - The ordering in the file is more consistent; first the main mappings, then the mapping from region and/or script to likely language, then the data contributed by SIL.
    - The regions have been cleaned up: there are no entries with `ZZ`, and `001` is limited to artifical languages such as Interlingua. The only other macroregion code is in `und_419 → es_Latn_419` (Spanish‧Latin‧Latin America)
5. Language matching
    - Dropped the fallback mapping `desired="uk" → supported="ru"` (so that Ukrainian (`uk`) doesn't fall back to Russian (`ru`)).
        - Note: A fallback language is used when the user's primary language is unavailable,
and either the user doesn't have any secondaries language in their settings (as on Android or iOS) or those secondary languages are also not available.
As a result of this change, when the primary and secondary languages are not available, the fallback language would be the system default instead of Russian.
    - Added the mapping `desired="scn" → supported="it"`.
    - Changed the deprecated code `knn` to `gom`.
7. Transforms
    1. Major update to `Han → Latn`, reflecting new data in Unicode 16.0
    2. Fixes for Arabic numbers, and a Farsi vowel
8. Other Unicode 16.0 changes
    1. Additional numbering systems
    2. Additional scripts and script identifiers
    3. ScriptMeta has been expanded for Unicode 16.0
9. Locale identifiers
    1. The subdivision identifiers have been updated to the latest available from ISO
        - The removed identifiers have been deprecated
        - Missing names have been added (from Wikidata)
    2. The language subtags, script subtags, and variant subtags have been updated to the latest from IANA
        - Some codes have been deprecated
    3. Parent and DefaultContent mappings have been added for kaa and kok;  DefaultContent mappings added for `kk`, `lld`, `ltg`, `mhn`, and `zh_Latn_CN`
10. Territory Info has been updated from World Bank and other sources: gdp, population, languages.
11. LanguageGroup info has been updated from Wikidata
12. Plural rules have been added for some new locales
13. Week data
    - The first day of the week has been changed for `AE`
    - Hour preferences (12 v 24) have been added for `en_H`K, `en_MY`, `en_IL`

For a full listing, see [¤¤BCP47 Delta](https://unicode.org/cldr/charts/46/delta/bcp47.html) and [¤¤Supplemental Delta](https://unicode.org/cldr/charts/46/delta/supplemental-data.html)

### [Locale Changes](https://unicode.org/cldr/charts/46/delta/index.html)

1. Major changes to emoji search keywords and short names (see below)
2. Major changes to Chinese collation, reflecting new data in Unicode 16.0
3. Other changes
    1. Various locales also had smaller improvements agreed to by translators.
    2. Additional test files have been added.

For a full listing, see [Delta Data](https://unicode.org/cldr/charts/46/delta/index.html)

### Emoji Search Keywords
The usage model for emoji search keywords is that 
- The user types one or more words in an emoji search field. The order of words doesn't matter; nor does upper- versus lowercase.
- Each word successively narrows a number of emoji in a results box
    - heart → 🥰 😘 😻 💌 💘 💝 💖 💗 💓 💞 💕 💟 ❣️ 💔 ❤️‍🔥 ❤️‍🩹 ❤️ 🩷 🧡 💛 💚 💙 🩵 💜 🤎 🖤 🩶 🤍 💋 🫰 🫶 🫀 💏 💑 🏠 🏡 ♥️ 🩺
    - blue → 🥶 😰 💙 🩵 🫐 👕 👖 📘 🧿 🔵 🟦 🔷 🔹 🏳️‍⚧️
    - therefore, [heart blue] → 💙 🩵
- A word matches all the words that begin with it; if there are no such matches, it is ignored.
    - [heart blue confabulation] is equivalent to [heart blue]
- Whenever the list is short enough to scan, the user will mouse-click on the right emoji — so it doesn't have to be narrowed too far.
Thus in the following, the user would just click on 🎉 if that works for them.
    - celebrate → 🥳 🥂 🎈 🎉 🎊 🪅

In this release WhatsApp emoji search keyword data has been incorporated. 
In the process of doing that, the maximum number of search keywords per emoji has been increased,
and the keywords have been simplified in most locales by breaking up multi-word keywords. 
An example would be white flag (🏳️), formerly having 3 keyword phrases of [white waving flag | white flag | waving flag], 
now being replaced by the simpler 3 single keywords [white | waving | flag]. 
The simpler version typically works as well or better in practice.

### Collation Data Changes
There are two significant changes to the CLDR root collation (CLDR default sort order).

#### Realigned With DUCET
The [DUCET](https://www.unicode.org/reports/tr10/#Default_Unicode_Collation_Element_Table) is the Unicode Collation Algorithm default sort order.
The [CLDR root collation](https://www.unicode.org/reports/tr35/tr35-collation.html#Root_Collation) is a tailoring of the DUCET. 
These sort orders have differed in the relative order of groups of characters including extenders, currency symbols, and non-decimal-digit numeric characters.

Starting with CLDR 46 and Unicode 16.0, the order of these groups is the same. 
In both sort orders, non-decimal-digit numeric characters now sort after decimal digits, 
and the CLDR root collation no longer tailors any currency symbols (making some of them sort like letter sequences, as in the DUCET).

These changes eliminate sort order differences among almost all regular characters between the CLDR root collation and the DUCET. 
See the [CLDR root collation](https://www.unicode.org/reports/tr35/tr35-collation.html#Root_Collation) documentation for details.

#### Improved Han Radical-Stroke Order
CLDR includes [data for sorting Han (CJK) characters in radical-stroke order](tr35-collation.md#File_Format_FractionalUCA_txt). 
It used to distinguish traditional and simplified forms of radicals on a higher level than sorting by the number of residual strokes. 
Starting with CLDR 46, the CLDR radical-stroke order matches that of the [Unicode Radical-Stroke Index (large PDF)](https://www.unicode.org/Public/UCD/latest/charts/RSIndex.pdf). 
[Its sorting algorithm is defined in UAX #38](https://www.unicode.org/reports/tr38/#SortingAlgorithm). 
Traditional vs. simplified forms of radicals are distinguished on a lower level than the number of residual strokes. 
This also has an effect on [alphabetic indexes](tr35-collation.md#Collation_Indexes) for radical-stroke sort orders, 
where only the traditional forms of radicals are now available as index characters.

### JSON Data Changes

1. Separate modern packages were dropped [CLDR-16465] 
2. Adding transliteration rules [CLDR-16720] (In progress)

### Markdown ###

The CLDR site is in the process of being moved to markdown source (GFM),
which will regularize the formatting and make it easier to maintain and extend than with Google Sites.
The URLs will remain the same.
This process should be completed before release.

### File Changes

Most files added in this release were for new locales.
There were the following new test files: **TBD***

### Tooling Changes

**TBD**

## Migration

**TBD**

## [Known Issues](https://unicode-org.atlassian.net/issues/CLDR-17535?jql=project%20%3D%20cldr%20and%20labels%20%3D%20%22ReleaseKnownIssue%22%20and%20status%20!%3D%20done)

1. CLDR-17095. The region-based firstDay value (see weekData) is currently used for several different purposes. In the future, some of these functions will be separated out:
    - The day that should be shown as the first day of the week in a calendar view.
    - The first day of the week (day 1) for weekday numbering.
    - The first day of the week for week-of-year calendar calculations.
2. CLDR-17505. Blocking items are obsolete: the spec needs to be corrected to use @ORDERED

## Acknowledgments

Many people have made significant contributions to CLDR and LDML; see the [Acknowledgments](https://cldr.unicode.org/index/acknowledgments) page for a full listing. We'd also like to acknowledge the work done by interns this release: **TBD**

The Unicode [Terms of Use](https://unicode.org/copyright.html) apply to CLDR data; in particular, see [Exhibit 1](https://unicode.org/copyright.html#Exhibit1).

For web pages with different views of CLDR data, see [http://cldr.unicode.org/index/charts](https://cldr.unicode.org/index/charts).

---
title: CLDR Charts
---

# CLDR Charts

The Unicode CLDR Charts provide different ways to view the Common Locale Data & Rules data.

-   [Latest](https://www.unicode.org/cldr/charts/latest) - The charts for the latest release version
-   [Dev](https://www.unicode.org/cldr/charts/dev) - A snapshot of data under development
-   [Previous](/index/downloads) - Previous available charts are linked from the download page in the Charts column

The format of most of the fields in the charts will be clear from the Name and ID, such as the months of the year. The format for others, such as the date or time formats, is structured and requires more interpretation. For more information, see [UTS #35: Unicode Common Locale Data & Rules (CLDR)](http://www.unicode.org/reports/tr35/).

Most charts have "double links" somewhere in each row. These are links that put the address of that row into the address bar of the browser for copying.

*Note that not all CLDR data is included in the charts.*

### Version Deltas

-   [**Delta Data**](https://www.unicode.org/cldr/charts/latest/delta/index.html) - Data that changed in the current release.
-   [**Delta DTDs**](https://www.unicode.org/cldr/charts/latest/supplemental/dtd_deltas.html) - Differences between CLDR DTD's over time.


### Locale-Based Data

-   [**Verification**](https://www.unicode.org/cldr/charts/latest/verify/index.html) - Constructed data for verification: Dates, Timezones, Numbers
-   [**Summary**](https://www.unicode.org/cldr/charts/latest/summary/root.html) - Provides a summary view of the main locale data. Language locales (those with no territory or variant) are presented with fully resolved data; the inherited or aliased data can be hidden if desired. Other locales do not show inherited or aliased data, just the differences from the respective language locale. The English value is provided for comparison (shown as "=" if it is equal to the localized value, and n/a if not available). The Sublocales column shows variations across locales. Hovering over each Sublocale value shows a pop-up with the locales that have that value.
-   [**By-Type**](https://www.unicode.org/cldr/charts/latest/by_type/index.html) - provides a side-by-side comparison of data from different locales for each field. For example, one can see all the locales that are left-to-right, or all the different translaitons of the Arabic script across languages. Data that is unconfimred or provisional is marked by a red-italic locale ID, such as *·bn\_BD·*.
-   [**Character Annotations**](https://www.unicode.org/cldr/charts/latest/annotations/index.html) - The CLDR emoji character annotations.
-   [**Subdivision Names**](https://www.unicode.org/cldr/charts/latest/subdivisionNames/index.html) - The (draft) CLDR subdivision names (names for states, provinces, cantons, etc.).
-   [**Collation Tailorings**](https://www.unicode.org/cldr/charts/latest/collation/index.html) - Collation charts (draft) for CLDR locales.


Other Data

-   [**Supplemental Data**](https://www.unicode.org/cldr/charts/latest/supplemental/index.html) - General data that is not part of the locale hierarchy but is still part of CLDR. Includes: *plural rules, day-period rules, language matching, language-script information, territories (countries),* and their *subdivisions, timezones,* and so on.
-   **Transform** - (Disabled temporarily) Some of the transforms in CLDR: the transliterations between different scripts. For more on transliterations, see [Transliteration Guidelines](/index/cldr-spec/transliteration-guidelines).
-   [**Keyboards**](https://www.unicode.org/cldr/charts/latest/keyboards/index.html) - Provides a view of keyboard data: layouts for different locales, mappings from characters to keyboards, and from keyboards to characters.

For more details on the locale data collection process, please see the [CLDR process](/index/process). For filing or viewing bug reports, see [CLDR Bug Reports](/requesting_changes).

For how to generate or update the charts, see [Generating Charts](/development/cldr-big-red-switch/generating-charts)

### Which languages use a character?

CLDR's [exemplar character sets](/translation/core-data/exemplars#exemplar-characters) record commonly used characters for a locale. You can browse the main sets by language in the [Main Exemplars chart](https://www.unicode.org/cldr/charts/latest/by_type/core_data.alphabetic_information.main.html).

To look up a particular character:

1. Open the [UnicodeSet utility](https://util.unicode.org/UnicodeJsps/list-unicodeset.jsp?a=%C3%A9&i=exemplar), which uses `é` as an example.
2. Replace **Input** with your character, enter `exemplar` in **Info**, and click **Show Set**.
3. Read the locale IDs after `exem=` next to the character. For example, the result for `é` includes `fr` (French).

Use these **Info** values to select an exemplar category:

| Info value | Exemplar category | Result label |
|---|---|---|
| `exemplar` | Main letters | `exem=` |
| `exemplar_aux` | Auxiliary characters, such as letters used in foreign words | `exema=` |
| `exemplar_punct` | Punctuation | `exemp=` |

For example, the [auxiliary result for `é`](https://util.unicode.org/UnicodeJsps/list-unicodeset.jsp?a=%C3%A9&i=exemplar_aux) includes `en` (English). For punctuation, try [the result for a comma](https://util.unicode.org/UnicodeJsps/list-unicodeset.jsp?a=%2C&i=exemplar_punct).

Main and auxiliary exemplars generally use lowercase letters, so look up `é` when checking the uppercase `É`. The results reflect the exemplar data for locales covered by the utility.

A set such as [`[éñ]`](https://util.unicode.org/UnicodeJsps/list-unicodeset.jsp?a=%5B%C3%A9%C3%B1%5D&i=exemplar) gives a separate locale list for each character; it does not find the languages that use every character in the set.

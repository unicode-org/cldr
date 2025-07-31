---
title: Number Symbols
---

# Number Symbols

The following symbols are used to format numbers. The Approved symbols for the locale will be substituted for the placeholders described in [Number Patterns](/translation/number-currency-formats/number-and-currency-patterns). 

For regional locales where the language is not the primary language of the country/region (e.g. en\_DE, English as used in Germany; or es_SE, Spanish as used in Sweden), there are different possible options for the number formats and date formats.
As with other CLDR data, the most customary form of _that_ language as used in _that_ country should be followed.
This is generally one of two options (or something in between):

1. The conventions of one of the main regional variants (e.g. en\_GB), such as 1,234.5 or 21/02/2025 or 21 February 2025
2. The conventions of one of the main languages used in the country, such as 1’234,5 or 2025-21-01 or 21. February 2025

The choice of which option is most customary can be difficult to determine with languages that are operating as a lingua franca for people who don't have it as their first language.
In these situations, much of the available content is written for an international audience and most of the readers are from other countries/regions.

Developers can choose a specific language variant, such as en\_GB, with a specific region's formatting by using [the Region Override U extension](https://www.unicode.org/reports/tr35/#RegionOverride) if they, for example, prefer British English formatting in a region/country with different number or date formatting.

| Name | English Example |  | Meaning |
|---|---|---|---|
| decimal | 2,345 **.** 67 |  | decimal separator |
| group | 2 **,** 345 . 67 |  | grouping separator, typically for thousands |
| plusSign | **+** 23 | * | the plus sign used with numbers<br /><br />  **See description of minusSign for information on usage and directionality markers.** |
| minusSign | **-** 23 | * | the minus sign used with numbers<br /><br />  The + and - symbols are intended for unary usage, and not for binary usage; therefore, the + and - symbols are used to represent either a positive number or a negative number. For example, in an operation 3 -(-2), the defined symbol would be used for the second minus sign, but not for the subtraction operator. Any directionality markers to keep with the number (e.g. &lt;LRM&gt;) should be included. |
| approximatelySign | **~** 23 | * | the sign to indicate that a number is approximate or inexact. May be used with measurement units; for example, "~10 meters" |
| percentSign | 23.4 **%** | * | the percent sign (out of 100) |
| perMille | 234 **‰** | * | the [permille](https://en.wikipedia.org/wiki/Per_mille) sign (out of 1000) |
| exponential | 1.2 **E** 3 | * | used in computers for 1.2×10³. |
| superscriptingExponent | 1.2 **×** 10 3             | *   | human-readable format of exponential  |
| infinity | **∞** | * | used in +∞ and -∞.  |
| nan | **NaN** | * | "not a number".   |
| timeSeparator | 2 **:** 33 PM |   | This symbol is currently ***not used*** for anything (and is only visible in SurveyTool at comprehensive level)   but may be retrieved by CLDR data consumers. It should match the time separator symbol actually used in Date/Time Patterns . |

💡 **Helpful Tips**

- The symbols marked with \* (in the 3rd column) in the above table typically do not need to be changed for other languages.
- Review the examples in the Survey Tool on the right-side pane to ensure that all pattern and symbols are as expected. (Shown in the screenshot below)
- The winning symbols in your locale are used in the Number Format examples. Please complete the [Number Symbols](https://st.unicode.org/cldr-apps/v#/ja/Symbols/) before working on [Number Formatting Patterns](https://st.unicode.org/cldr-apps/v#/ja/Number_Formatting_Patterns/).

![image](../../images/number-currency-formats/number-symbol.JPG)


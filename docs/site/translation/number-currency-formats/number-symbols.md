---
title: Number Symbols
---

# Number Symbols

The following symbols are used to format numbers. The Approved symbols for the locale will be substituted for the placeholders described in [Number Patterns](https://cldr.unicode.org/translation/number-currency-formats/number-and-currency-patterns). 

For English regional locales (e.g. en\_DE) where English is not the primary language of the country/region (e.g. en\_DE; English as used in Germany), the number formats and date formats should follow the **English formatting usage** in that country/region. Often, the formatting usage in English tend to follow the British or American formatting rather than the formatting of the primary language. Consult with the English versions of prominent magazines or newspapers for guidance on date and number format usage.

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

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
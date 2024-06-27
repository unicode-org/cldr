---
title: Numbering Systems
---

# Numbering Systems

Certain general features of numbers need to be supplied for locale data.

## Numbering systems

Numbering systems are used to show different representations of numeric values. Each numbering system consists of characters that represent numeric digits. In addition, there are also number symbols used with each numbering system that may differ when the numbering system is used in different locales.

The default numbering system for a locale is the numbering system that is normally used to represent numbers in that locale.

The native numbering system for a locale is the numbering system used for native digits, and is normally in the script for the locale's language. Native numbering systems can only use numeric positional decimal digits, like for Latin numbers (0123456789). If the numbering system in your language uses an algorithm to spell out numbers in the language's script, label it as a traditional numbering system instead. The traditional numbering system does not need to be specified if it is the same as the native numbering system.

The default, native and traditional numbering systems for a locale may be different. For example, in Tamil the default numbering system is latn, the native numbering system is tamldec and the traditional numbering system is taml.

Codes are used to represent numbering systems in the Survey tool. Below are some examples of common codes:

| Code |  Description |  Digits |
|---|---|---|
| arab | Arabic-Indic digits | ٠١٢٣٤٥٦٧٨٩ |
| fullwide      | Full width digits | ０１２３４５６７８９ |
| hant | Traditional Chinese numerals — non-decimal | algorithmic |
| latn |  Latin digits |  0123456789 |

For further reference, see the [complete list](http://www.unicode.org/repos/cldr/trunk/common/bcp47/number.xml) of numbering system codes and their corresponding [rules](http://www.unicode.org/repos/cldr/trunk/common/supplemental/numberingSystems.xml).

## Minimum digits for grouping

In some languages, the grouping separator is suppressed in certain cases. For example, see [china-auf-wachstumskurs.gif](http://media0.faz.net/ppmedia/multimedia/interaktiv/2537959306/1.289750/width610x580/china-auf-wachstumskurs.gif), where there is a grouping separator in "12 080" but not in "4720". The minimumGroupingDigits determines what the default for a locale is. In this case the value should be "2" to illustrate that the separator only appears once the number of thousands goes into the double-digits (i.e. 10 thousand or above) and not for single-digit thousands (i.e. anything below 10 thousand).

Examples:

Indicate "3" for grouping separator starting at 6 digit-numbers (i.e. 100,000 and above)

Indicate "2" for grouping separator starting at 5 digit-numbers (i.e. 10,000 and above)

Indicate "1" for grouping separator starting at 4 digit-numbers (i.e. 1,000 and above)

Note that this is just the default, and the grouping separator may be retained in lists, or removed in other circumstances. For example, in English the "," is used by default, but not in addresses ("12345 Baker Street"), in 4-digit years (2014, but 12,000 BC), and certain other cases.

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
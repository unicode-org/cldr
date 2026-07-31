## <a name="Number_Range_Formatting" id="Number_Range_Formatting" href="#Number_Range_Formatting">Number Range Formatting</a>

Often ranges of numbers are presented to users, such as in “Length: 3.2–4.5 centimeters”. This means any length from 3.2 cm to 4.5 cm, inclusive.

To format a number range, the following steps are taken:

1. Format the lower bound and the upper bound independently following the steps in [Number Format Patterns](#Number_Format_Patterns), preserving semantic annotations\*.
1. If the resulting values are identical, stop evaluating these steps and, instead, perform the steps in [Approximate Number Formatting](#Approximate_Number_Formatting).
    1. Note: This behavior may be customized in order to, for example, print the range despite the endpoints being identical. However, a spec-compliant implementation must support approximate number formatting.
1. Perform the steps in [Collapsing Number Ranges](#Collapsing_Number_Ranges), obtaining modified *lower* and *upper* values.
1. Obtain a number range pattern by following the steps in [Range Pattern Processing](#Range_Pattern_Processing).
1. Substitute *lower* as `{0}` and *upper* as `{1}` into the range pattern from the previous step.

\* Semantic annotations are discussed in [Collapsing Number Ranges](#Collapsing_Number_Ranges).

For plural rule selection of number ranges, see [Plural Ranges](#Plural_Ranges).

### <a name="Approximate_Number_Formatting" id="Approximate_Number_Formatting" href="#Approximate_Number_Formatting">Approximate Number Formatting</a>

*Approximate number formatting* refers to a specific format of numbers in which the value is understood to not be exact; for example, "\~5 minutes".

* To format an approximate number, follow the normal number formatting procedure in [Number Format Patterns](#Number_Format_Patterns), but substitute the `approximatelySign` from [Number Symbols](#Number_Symbols) in for the minus sign placeholder.


* If the number is negative, or if the formatting options request the sign to be displayed, *prepend* the `approximatelySign` to the plus or minus sign before substituting it into the pattern. For example, "\~-5" means "approximately negative five". This procedure may change in the future.


### <a name="Collapsing_Number_Ranges" id="Collapsing_Number_Ranges" href="#Collapsing_Number_Ranges">Collapsing Number Ranges</a>

*Collapsing* a number range refers to the process of removing duplicated information in the *lower* and *upper* values. For example, if the lower string is "3.2 centimeters" and the upper string is "4.5 centimeters", it is desirable to remove the extra "centimeters" token.

* This operation requires *semantic annotations* on the formatted value. The exact form of the semantic annotations is implementation-dependent. However, implementations may consider the following broad categories of tokens:


1. Numerical value, including decimal and grouping separators
1. Sign symbol
1. Scientific or compact notation
1. Unit of measurement

For example, consider the string `-5.3M US dollars`. It may be annotated as follows:

- `-` → sign symbol
- `5.3` → numerical value
- `M` → compact notation
- `US dollars` → unit of measurement for the currency USD

Two tokens are *semantically equivalent* if they have the same *semantic annotations*, even if they are not the exact same string. For example:

1. "centimeter" is semantically equivalent to "centimeters".
1. "K" (the thousands symbol in compact decimals) is NOT semantically equivalent to "K" (the measurement unit Kelvin).

* The above description describes the expected output. Internally, the implementation may determine the equivalent units of measurement by passing the codes back from the number formatters, allowing for a precise determination of "semantically equivalent".


Two semantically equivalent tokens can be *collapsed* if they appear at the start of both values or the end of both values.
However, the implementation may choose different levels of aggressiveness with regard to collapsing tokens.
An API for displaying ranges should permit control over whether the tokens are collapsed or not, and the levels of aggressiveness.
The currently recommended heuristic is:

1. Never collapse scientific or compact notation. This is to avoid producing ambiguous strings such as "3–5M" (could represent 3–5,000,000 or 3,000,000–5,000,000).
2. Only collapse if the tokens are more than one code point in length. This is to increase clarity of strings such as "$3–$5".
3. To perform the collapse, remove the token that is closest to the range separator.
That is, for a prefix element, remove from the end value of the range, and for a suffix element remove it from the start value of the range:
    * USD 2 – USD 5 ⇒ USD 2 – 5
    * 2M EUR – 5M EUR ⇒  2M – 5M EUR
    * 2 km – 5 km ⇒ 2 – 5 km
    * 2M ft – 5M ft ⇒ 2M – 5M ft
4. When the tokens can have distinct plural forms, modify the remaining token so that it has the correct plural form. That is, use [Plural Ranges](#Plural_Ranges) to calculate the correct plural category for the range, and pick the variant of that the remaining token corresponding to that plural form.

In bidi contexts, the data is built so that rule #3 works **visually**.
For example, if a range from 2 km to 5 km would be presented visually as "_mk 5 – mk 2_", the collapsed form would be "_mk 5 – 2_".
(The _mk_ is a stand-in for the native representation.)
This requires consistent visually reordering among the elements: the range, the prefixes and the suffixes.
Thus a prefix value will be reordered to be visually a suffix value, and the order of the range will be visually reversed.

### <a name="Range_Pattern_Processing" id="Range_Pattern_Processing" href="#Range_Pattern_Processing">Range Pattern Processing</a>

To obtain a number range pattern, the following steps are taken:

1. Load the range pattern found in [Miscellaneous Patterns](#Miscellaneous_Patterns).
1. Optionally add spacing to the range pattern.

To determine whether to add spacing, the currently recommended heuristic is:

1. If the *lower* string ends with a character other than a digit, or if the *upper* string begins with a character other than a digit.
2. If the range pattern does not contain a character having the `White_Space` binary Unicode property after the `{0}` or before the `{1}` placeholders.

These heuristics may be refined in the future.

To add spacing, insert a non-breaking space (U+00A0) at the positions in item 2 above.

* * *

© 2001–2026 Unicode, Inc.
This publication is protected by copyright, and permission must be obtained from Unicode, Inc.
prior to any reproduction, modification, or other use not permitted by the [Terms of Use](https://www.unicode.org/copyright.html).
Specifically, you may make copies of this publication and may annotate and translate it solely for personal or internal business purposes and not for public distribution,
provided that any such permitted copies and modifications fully reproduce all copyright and other legal notices contained in the original.
* You may not make copies of or modifications to this publication for public distribution, or incorporate it in whole or in part into any product or publication without the express written permission of Unicode.


Use of all Unicode Products, including this publication, is governed by the Unicode [Terms of Use](https://www.unicode.org/copyright.html).
The authors, contributors, and publishers have taken care in the preparation of this publication,
* but make no express or implied representation or warranty of any kind and assume no responsibility or liability for errors or omissions or for consequential or incidental damages that may arise therefrom.

This publication is provided “AS-IS” without charge as a convenience to users.

Unicode and the Unicode Logo are registered trademarks of Unicode, Inc. in the United States and other countries.

## <a name="Number_Format_Patterns" id="Number_Format_Patterns" href="#Number_Format_Patterns">Number Format Patterns</a>

### <a name="Number_Patterns" id="Number_Patterns" href="#Number_Patterns">Number Patterns</a>

* Number patterns affect how numbers are interpreted in a localized context. Here are some examples, based on the French locale. The "." shows where the decimal point should go. The "," shows where the thousands separator should go. A "0" indicates zero-padding: if the number is too short, a zero (in the locale's numeric set) will go there. A "#" indicates no padding: if the number is too short, nothing goes there. A "¤" shows where the currency sign will go. The following illustrates the effects of different patterns for the French locale, with the number "1234.567". Notice how the pattern characters ',' and '.' are replaced by the characters appropriate for the locale.


###### <a name="Number_Pattern_Examples" id="Number_Pattern_Examples" href="#Number_Pattern_Examples">Table: Number Pattern Examples</a>

| Pattern    | Currency | Text       |
|------------|----------|------------|
| #,##0.##   | _n/a_    | 1 234,57   |
| #,##0.###  | _n/a_    | 1 234,567  |
| ###0.##### | _n/a_    | 1234,567   |
| ###0.0000# | _n/a_    | 1234,5670  |
| 00000.0000 | _n/a_    | 01234,5670 |
| #,##0.00 ¤ | EUR      | 1 234,57 € |
|            | JPY      | 1 235 ¥JP  |

* The number of # placeholder characters before the decimal does not matter, since no limit is placed on the maximum number of digits. There should, however, be at least one zero someplace in the pattern. In currency formats, the number of digits after the decimal also does not matter, since the information in the supplemental data (see _[Supplemental Currency Data](#Supplemental_Currency_Data))_ is used to override the number of decimal places — and the rounding — according to the currency that is being formatted. That can be seen in the above chart, with the difference between Yen and Euro formatting.


* To ensure correct layout, especially in currency patterns in which a variety of symbols may be used, number patterns may contain (invisible) bidirectional text format characters such as LRM, RLM, and ALM.


_When parsing using a pattern, a lenient parse should be used; see [Lenient Parsing](tr35.md#Lenient_Parsing)._ As noted there, lenient parsing should ignore bidi format characters.

### <a name="Special_Pattern_Characters" id="Special_Pattern_Characters" href="#Special_Pattern_Characters">Special Pattern Characters</a>

* Many characters in a pattern are taken literally; they are matched during parsing and output unchanged during formatting. Special characters, on the other hand, stand for other characters, strings, or classes of characters. For example, the '#' character is replaced by a localized digit for the chosen numberSystem. Often the replacement character is the same as the pattern character; in the U.S. locale, the ',' grouping character is replaced by ','. However, the replacement is still happening, and if the symbols are modified, the grouping character changes. Some special characters affect the behavior of the formatter by their presence; for example, if the percent character is seen, then the value is multiplied by 100 before being displayed.


* To insert a special character in a pattern as a literal, that is, without any special meaning, the character must be quoted. There are some exceptions to this which are noted below. The Localized Replacement column shows the replacement from _[Number Symbols](#Number_Symbols)_ or the numberSystem's digits: _italic_ indicates a special function.


Invalid sequences of special characters (such as “¤¤¤¤¤¤” in current CLDR) should be handled for formatting and parsing as described in [Handling Invalid Patterns](tr35.md#Invalid_Patterns).

###### <a name="Number_Pattern_Character_Definitions" id="Number_Pattern_Character_Definitions" href="#Number_Pattern_Character_Definitions">Table: Number Pattern Character Definitions</a>

| Symbol | Location | Localized Replacement | Meaning |
| :-- | :-- | :-- | :-- |
| 0 | Number | digit | Digit |
| 1-9 | Number | digit | '1' through '9' indicate rounding. |
| @ | Number | digit | Significant digit |
| # | Number | digit, _nothing_ | Digit, omitting leading/trailing zeros |
| . | Number | decimal, currencyDecimal | Decimal separator or monetary decimal separator |
| - | Number | minusSign, plusSign, approximatelySign | Minus sign. **Warning:** the pattern '-'0.0 is not the same as the pattern -0.0. In the former case, the minus sign is a literal. In the latter case, it is a special symbol, which is replaced by the minusSymbol, and can also be replaced by the plusSymbol for a format like +12% as in [Explicit Plus Signs](#Explicit_Plus). |
| , | Number | group, currencyGroup | Grouping separator. May occur in both the integer part and the fractional part. The position determines the grouping. |
| E | Number | exponential, superscriptingExponent | Separates mantissa and exponent in scientific notation. _Need not be quoted in prefix or suffix._ |
| + | Exponent or Number (for explicit plus) | plusSign | Prefix positive exponents with localized plus sign. Used for explicit plus for numbers as well, as described in [Explicit Plus Signs](#Explicit_Plus). _Need not be quoted in prefix or suffix._ |
| % | Prefix or suffix | percentSign | Multiply by 100 and show as percentage |
| ‰ (U+2030) | Prefix or suffix | perMille | Multiply by 1000 and show as per mille (aka “basis points”) |
| ; | Subpattern boundary | _syntax_ | Separates positive and negative subpatterns. When there is no explicit negative subpattern, an implicit negative subpattern is formed from the positive pattern with a prefixed - (ASCII U+002D HYPHEN-MINUS). |
| ¤ (U+00A4) | Prefix or suffix | _currency symbol/name from currency specified in API_ | Any sequence is replaced by the localized currency symbol for the currency being formatted, as in the table below. If present in a pattern, the monetary decimal separator and grouping separators (if available) are used instead of the numeric ones. If data is unavailable for a given sequence in a given locale, the display may fall back to ¤ or ¤¤. See also the formatting for currency display names, steps 2 and 4 in [Currencies](#Currencies). <table><tr><th>No.</th><th>Replacement / Example</th></tr><tr><td rowspan="2">¤</td><td>Standard currency symbol</td></tr><tr><td>_C$12.00_</td></tr><tr><td rowspan="2">¤¤</td><td>ISO currency symbol (constant)</td></tr><tr><td>_CAD 12.00_</td></tr><tr><td rowspan="2">¤¤¤</td><td>Appropriate currency display name for the currency, based on the plural rules in effect for the locale</td></tr><tr><td>_5.00 Canadian dollars_</td></tr><tr><td rowspan="2" >¤¤¤¤¤</td><td>Narrow currency symbol. The same symbols may be used for multiple currencies. Thus the symbol may be ambiguous, and should only be where the context is clear.</td></tr><tr><td>_$12.00_</td></tr><tr><td>_others_</td><td>_Invalid in current CLDR. Reserved for future specification_</td></tr></table> |
| * | Prefix or suffix boundary | _padding character specified in API_ | Pad escape, precedes pad character |
| ' | Prefix or suffix | _syntax-only_ | Used to quote special characters in a prefix or suffix, for example, `"'#'#"` formats 123 to `"#123"`. To create a single quote itself, use two in a row: `"# o''clock"`. |

* A pattern contains a positive subpattern and may contain a negative subpattern, for example, "#,##0.00;(#,##0.00)". Each subpattern has a prefix, a numeric part, and a suffix. If there is no explicit negative subpattern, the implicit negative subpattern is the ASCII minus sign (-) prefixed to the positive subpattern. That is, "0.00" alone is equivalent to "0.00;-0.00". (The data in CLDR is normalized to remove an explicit negative subpattern where it would be identical to the implicit form.)


* Note that if a negative subpattern is used as-is: a minus sign is _not_ added, eg "0.00;0.00" ≠ "0.00;-0.00". Trailing semicolons are ignored, eg "0.00;" = "0.00". Whitespace is not ignored, including those around semicolons, so "0.00 ; -0.00" ≠ "0.00;-0.00".


* If there is an explicit negative subpattern, it serves only to specify the negative prefix and suffix; the number of digits, minimal digits, and other characteristics are ignored in the negative subpattern. That means that "#,##0.0#;(#)" has precisely the same result as "#,##0.0#;(#,##0.0#)". However in the CLDR data, the format is normalized so that the other characteristics are preserved, just for readability.


> **Note:** The thousands separator and decimal separator in patterns are always ASCII ',' and '.'. They are substituted by the code with the correct local values according to other fields in CLDR. The same is true of the - (ASCII minus sign) and other special characters listed above.

* A currency decimal pattern normally contains a currency symbol placeholder (¤, ¤¤, ¤¤¤, or ¤¤¤¤¤). The currency symbol placeholder may occur before the first digit, after the last digit symbol, or where the decimal symbol would otherwise be placed (for formats such as "12€50", as in "12€50 pour une omelette").


| Placement | Examples                                                                         |
|-----------|----------------------------------------------------------------------------------|
| Before    | "¤#,##0.00" "¤ #,##0.00" "¤-#,##0.00" "¤ -#,##0.00" "-¤#,##0.00" "-¤ #,##0.00" … |
| After     | "#,##0.00¤" "#,##0.00 ¤" "#,##0.00-¤" "#,##0.00- ¤" "#,##0.00¤-" "#,##0.00 ¤-" … |
| Decimal   | "#,##0¤00"                                                                       |

Below is a sample of patterns, special characters, and results:

###### <a name="Sample_Patterns_and_Results" id="Sample_Patterns_and_Results" href="#Sample_Patterns_and_Results">Table: Sample Patterns and Results</a>

<table>
<tbody>
<tr><th>explicit pattern:</th><td colspan="2">0.00;-0.00</td><td colspan="2">0.00;0.00-</td><td colspan="2">0.00+;0.00-</td></tr>
<tr><th>decimalSign:</th><td colspan="2">,</td><td colspan="2">,</td><td colspan="2">,</td></tr>
<tr><th>minusSign:</th><td colspan="2">∸</td><td colspan="2">∸</td><td colspan="2">∸</td></tr>
<tr><th>plusSign:</th><td colspan="2">∔</td><td colspan="2">∔</td><td colspan="2">∔</td></tr>
<tr><th>number:</th><td>3.1415</td><td>-3.1415</td><td>3.1415</td><td>-3.1415</td><td>3.1415</td><td>-3.1415</td></tr>
<tr><th>formatted:</th><td>3,14</td><td>∸3,14</td><td>3,14</td><td>3,14∸</td><td>3,14∔</td><td>3,14∸</td></tr>
</tbody>
</table>

_In the above table, ∸ = U+2238 DOT MINUS and ∔ = U+2214 DOT PLUS are used for illustration._

* The prefixes, suffixes, and various symbols used for infinity, digits, thousands separators, decimal separators, and so on may be set to arbitrary values, and they will appear properly during formatting. _However, care must be taken that the symbols and strings do not conflict, or parsing will be unreliable._ For example, either the positive and negative prefixes or the suffixes must be distinct for any parser using this data to be able to distinguish positive from negative values. Another example is that the decimal separator and thousands separator should be distinct characters, or parsing will be impossible.


* The _grouping separator_ is a character that separates clusters of integer digits to make large numbers more legible. It is commonly used for thousands, but in some locales it separates ten-thousands. The _grouping size_ is the number of digits between the grouping separators, such as 3 for "100,000,000" or 4 for "1 0000 0000". There are actually two different grouping sizes: One used for the least significant integer digits, the _primary grouping size_, and one used for all others, the _secondary grouping size_. In most locales these are the same, but sometimes they are different. For example, if the primary grouping interval is 3, and the secondary is 2, then this corresponds to the pattern "#,##,##0", and the number 123456789 is formatted as "12,34,56,789". If a pattern contains multiple grouping separators, the interval between the last one and the end of the integer defines the primary grouping size, and the interval between the last two defines the secondary grouping size. All others are ignored, so "#,##,###,####" == "###,###,####" == "##,#,###,####".


* The grouping separator may also occur in the fractional part, such as in “#,##0.###,#”. This is most commonly done where the grouping separator character is a thin, non-breaking space (U+202F), such as “1.618 033 988 75”. See [physics.nist.gov/cuu/Units/checklist.html](https://physics.nist.gov/cuu/Units/checklist.html).


For consistency in the CLDR data, the following conventions are observed:

* All number patterns should be minimal: there should be no leading # marks except to specify the position of the grouping separators (for example, avoid  ##,##0.###).
* All formats should have one 0 before the decimal point (for example, avoid #,###.##)
* Decimal formats should have three hash marks in the fractional position (for example, #,##0.###).
* Currency formats should have two zeros in the fractional position (for example, ¤ #,##0.00).
    * The exact number of decimals is overridden with the decimal count in supplementary data or by API settings.
* The only time two thousands separators need to be used is when the number of digits varies, such as for Hindi: #,##,##0.
* The **minimumGroupingDigits** can be used to suppress groupings below a certain value. This is used for languages such as Polish, where one would only write the grouping separator for values above 9999. The minimumGroupingDigits contains the default for the locale.
    * The attribute value is used by adding it to the grouping separator value. If the input number has fewer integer digits, the grouping separator is suppressed.
    * ##### <a name="Examples_of_minimumGroupingDigits" href="#Examples_of_minimumGroupingDigits">Examples of minimumGroupingDigits</a>

        | minimum­GroupingDigits | Pattern Grouping | Input Number | Formatted |
        |-----------------------:|-----------------:|-------------:|----------:|
        |                      1 |                3 |         1000 |     1,000 |
        |                      1 |                3 |        10000 |    10,000 |
        |                      2 |                3 |         1000 |      1000 |
        |                      2 |                3 |        10000 |    10,000 |
        |                      1 |                4 |        10000 |    1,0000 |
        |                      2 |                4 |        10000 |     10000 |

#### <a name="Explicit_Plus" id="Explicit_Plus" href="#Explicit_Plus">Explicit Plus Signs</a>

* An explicit "plus" format can be formed, so as to show a visible + sign when formatting a non-negative number. The displayed plus sign can be an ASCII plus or another character, such as ＋ U+FF0B FULLWIDTH PLUS SIGN or ➕ U+2795 HEAVY PLUS SIGN; it is taken from whatever is set for plusSign in _[Number Symbols](#Number_Symbols)_.


1. Get the negative subpattern (explicit or implicit).
2. Replace any unquoted ASCII minus sign by an ASCII plus sign.
3. If there are any replacements, use that for the positive subpattern.

For an example, see [Sample Patterns and Results](#Sample_Patterns_and_Results).

### <a name="Formatting" id="Formatting" href="#Formatting">Formatting</a>

* Formatting is guided by several parameters, all of which can be specified either using a pattern or using an external API designed for number formatting. The following description applies to formats that do not use [scientific notation](#sci) or [significant digits](#sigdig).


* If the number of actual integer digits exceeds the _maximum integer digits_, then only the least significant digits are shown. For example, 1997 is formatted as "97" if the maximum integer digits is set to 2.
* If the number of actual integer digits is less than the _minimum integer digits_, then leading zeros are added. For example, 1997 is formatted as "01997" if the minimum integer digits is set to 5.
* If the number of actual fraction digits exceeds the _maximum fraction digits_, then half-even rounding it performed to the maximum fraction digits. For example, 0.125 is formatted as "0.12" if the maximum fraction digits is 2. This behavior can be changed by specifying a rounding increment and a rounding mode.
* If the number of actual fraction digits is less than the _minimum fraction digits_, then trailing zeros are added. For example, 0.125 is formatted as "0.1250" if the minimum fraction digits is set to 4.
* Trailing fractional zeros are not displayed if they occur _j_ positions after the decimal, where _j_ is less than the maximum fraction digits. For example, 0.10004 is formatted as "0.1" if the maximum fraction digits is four or less.

**Special Values**

* **NaN` is represented**: `NaN` is represented as a single character, typically `(U+FFFD)` . This character is determined by the localized number symbols. This is the only value for which the prefixes and suffixes are not used.


* Infinity is represented as a single character, typically ∞ `(U+221E)` , with the positive or negative prefixes and suffixes applied. The infinity character is determined by the localized number symbols.


### <a name="sci" id="sci" href="#sci">Scientific Notation</a>

* Numbers in scientific notation are expressed as the product of a mantissa and a power of ten, for example, 1234 can be expressed as 1.234 x 10<sup>3</sup>. The mantissa is typically in the half-open interval [1.0, 10.0) or sometimes [0.0, 1.0), but it need not be. In a pattern, the exponent character immediately followed by one or more digit characters indicates scientific notation. Example: "0.###E0" formats the number 1234 as "1.234E3".


* The number of digit characters after the exponent character gives the minimum exponent digit count. There is no maximum. Negative exponents are formatted using the localized minus sign, _not_ the prefix and suffix from the pattern. This allows patterns such as "0.###E0 m/s". To prefix positive exponents with a localized plus sign, specify '+' between the exponent and the digits: "0.###E+0" will produce formats "1E+1", "1E+0", "1E-1", and so on. (In localized patterns, use the localized plus sign rather than '+'.)
* The minimum number of integer digits is achieved by adjusting the exponent. Example: 0.00123 formatted with "00.###E0" yields "12.3E-4". This only happens if there is no maximum number of integer digits. If there is a maximum, then the minimum number of integer digits is fixed at one.
* The maximum number of integer digits, if present, specifies the exponent grouping. The most common use of this is to generate _engineering notation_, in which the exponent is a multiple of three, for example, "##0.###E0". The number 12345 is formatted using "##0.####E0" as "12.345E3".
* When using scientific notation, the formatter controls the digit counts using logic for significant digits. The maximum number of significant digits comes from the mantissa portion of the pattern: the string of #, 0, and period (".") characters immediately preceding the E. To get the maximum number of significant digits, use the following algorithm:

    1.  If the mantissa pattern contains a period:
        1.  If the mantissa pattern contains at least one 0:
            *   Return the number of 0s before the period added to the number of #s or 0s after the period
        2.  Else:
            *   Return 1 plus the number of #s after the period
    2.  Else:
        1.  If the mantissa pattern contains at least one 0:
            *   Return the number of 0s.
        2.  Else:
            *   Return positive infinity.

    Examples:

    *   0.##E0 means a max of 3 significant digits.
    *   #.##E0 also means a max of 3 significant digits.
    *   #.0#E0 means a max of 2 significant digits.
    *   0E0 means a max of 1 significant digit.
    *   #E0 means infinite precision.
    *   ###E0 means engineering notation with infinite precision.
*   Exponential patterns may not contain grouping separators.

### <a name="sigdig" id="sigdig" href="#sigdig">Significant Digits</a>

* There are two ways of controlling how many digits are shown: (a) significant digits counts, or (b) integer and fraction digit counts. Integer and fraction digit counts are described above. When a formatter is using significant digits counts, it uses however many integer and fraction digits are required to display the specified number of significant digits. It may ignore min/max integer/fraction digits, or it may use them to the extent possible.


###### <a name="Significant_Digits_Examples" id="Significant_Digits_Examples" href="#Significant_Digits_Examples">Table: Significant Digits Examples</a>

| Pattern | Minimum significant digits | Maximum significant digits | Number | Output |
| :-- | :-- | :-- | :-- | :-- |
| `@@@` | 3 | 3 | 12345 | `12300` |
| `@@@` | 3 | 3 | 0.12345 | `0.123` |
| `@@##` | 2 | 4 | 3.14159 | `3.142` |
| `@@##` | 2 | 4 | 1.23004 | `1.23` |

* In order to enable significant digits formatting, use a pattern containing the `'@'` pattern character. In order to disable significant digits formatting, use a pattern that does not contain the `'@'` pattern character.
* Significant digit counts may be expressed using patterns that specify a minimum and maximum number of significant digits. These are indicated by the `'@'` and `'#'` characters. The minimum number of significant digits is the number of `'@'` characters. The maximum number of significant digits is the number of `'@'` characters plus the number of `'#'` characters following on the right. For example, the pattern `"@@@"` indicates exactly 3 significant digits. The pattern `"@##"` indicates from 1 to 3 significant digits. Trailing zero digits to the right of the decimal separator are suppressed after the minimum number of significant digits have been shown. For example, the pattern `"@##"` formats the number 0.1203 as `"0.12"`.
* Implementations may forbid the use of significant digits in combination with min/max integer/fraction digits. In such a case, if a pattern uses significant digits, it may not contain a decimal separator, nor the `'0'` pattern character. Patterns such as `"@00"` or `"@.###"` would be disallowed.
* Any number of `'#'` characters may be prepended to the left of the leftmost `'@'` character. These have no effect on the minimum and maximum significant digits counts, but may be used to position grouping separators. For example, `"#,#@#"` indicates a minimum of one significant digit, a maximum of two significant digits, and a grouping size of three.
* The number of significant digits has no effect on parsing.
* Significant digits may be used together with exponential notation. Such patterns are equivalent to a normal exponential pattern with a minimum and maximum integer digit count of one, a minimum fraction digit count of `Minimum Significant Digits - 1`, and a maximum fraction digit count of `Maximum Significant Digits - 1`. For example, the pattern `"@@###E0"` is equivalent to `"0.0###E0"`.

### <a name="Padding" id="Padding" href="#Padding">Padding</a>

* Patterns support padding the result to a specific width. In a pattern the pad escape character, followed by a single pad character, causes padding to be parsed and formatted. The pad escape character is '*'. For example, `"$*x#,##0.00"` formats 123 to `"$xx123.00"` , and 1234 to `"$1,234.00"` .


* When padding is in effect, the width of the positive subpattern, including prefix and suffix, determines the format width. For example, in the pattern `"* #0 o''clock"`, the format width is 10.
* Some parameters which usually do not matter have meaning when padding is used, because the pattern width is significant with padding. In the pattern "* ##,##,#,##0.##", the format width is 14. The initial characters "##,##," do not affect the grouping size or maximum integer digits, but they do affect the format width.
* Padding may be inserted at one of four locations: before the prefix, after the prefix, before the suffix, or after the suffix. No padding can be specified in any other location. If there is no prefix, before the prefix and after the prefix are equivalent, likewise for the suffix.
* When specified in a pattern, the code point immediately following the pad escape is the pad character. This may be any character, including a special pattern character. That is, the pad escape _escapes_ the following character. If there is no character after the pad escape, then the pattern is illegal.

### <a name="Rounding" id="Rounding" href="#Rounding">Rounding</a>

* Patterns support rounding to a specific increment. For example, 1230 rounded to the nearest 50 is 1250. Mathematically, rounding to specific increments is performed by dividing by the increment, rounding to an integer, then multiplying by the increment. To take a more bizarre example, 1.234 rounded to the nearest 0.65 is 1.3, as follows:


<table>
<tbody>
<tr><th>Original:</th><td>1.234</td></tr>
<tr><th>Divide by increment (0.65):</th><td>1.89846…</td></tr>
<tr><th>Round:</th><td>2</td></tr>
<tr><th>Multiply by increment (0.65):</th><td>1.3</td></tr>
</tbody>
</table>

To specify a rounding increment in a pattern, include the increment in the pattern itself. "#,#50" specifies a rounding increment of 50. "#,##0.05" specifies a rounding increment of 0.05.

* Rounding only affects the string produced by formatting. It does not affect parsing or change any numerical values.
* An implementation may allow the specification of a _rounding mode_ to determine how values are rounded. In the absence of such choices, the default is to round "half-even", as described in IEEE arithmetic. That is, it rounds towards the "nearest neighbor" unless both neighbors are equidistant, in which case, it rounds towards the even neighbor. Behaves as for round "half-up" if the digit to the left of the discarded fraction is odd; behaves as for round "half-down" if it's even. Note that this is the rounding mode that minimizes cumulative error when applied repeatedly over a sequence of calculations.
* Some locales use rounding in their currency formats to reflect the smallest currency denomination.
* In a pattern, digits '1' through '9' specify rounding, but otherwise behave identically to digit '0'.

### <a name="Quoting_Rules" id="Quoting_Rules" href="#Quoting_Rules">Quoting Rules</a>

* **Single quotes (**'****: Single quotes (**'**) enclose bits of the pattern that should be treated literally. Inside a quoted string, two single quotes ('') are replaced with a single one ('). For example: `'X '`#`' Q '` -> **X 1939 Q** (Literal strings `shaded`.)



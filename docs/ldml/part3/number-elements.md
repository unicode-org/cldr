## <a name="Number_Elements" id="Number_Elements" href="#Number_Elements">Number Elements</a>

```dtd
<!ELEMENT numbers ( alias | ( defaultNumberingSystem*, otherNumberingSystems*, minimumGroupingDigits*, symbols*, decimalFormats*, scientificFormats*, percentFormats*, currencyFormats*, currencies?, miscPatterns*, minimalPairs*, special* ) ) >
```

* The numbers element supplies information for formatting and parsing numbers and currencies. It has the following sub-elements: `<defaultNumberingSystem>`, `<otherNumberingSystems>`, `<symbols>`, `<decimalFormats>`, `<scientificFormats>`, `<percentFormats>`, `<currencyFormats>`, and `<currencies>`. The currency IDs are from [[ISO4217](tr35.md#ISO4217)] (plus some additional common-use codes). For more information, including the pattern structure, see _[Section 3: Number Format Patterns](#Number_Format_Patterns)_.


### <a name="defaultNumberingSystem" id="defaultNumberingSystem" href="#defaultNumberingSystem">Default Numbering System</a>

```dtd
<!ELEMENT defaultNumberingSystem ( #PCDATA )>
```

This element indicates which numbering system should be used for presentation of numeric quantities in the given locale.

### <a name="otherNumberingSystems" id="otherNumberingSystems" href="#otherNumberingSystems">Other Numbering Systems</a>

```dtd
<!ELEMENT otherNumberingSystems ( alias | ( native*, traditional*, finance*)) >
```

* This element defines general categories of numbering systems that are sometimes used in the given locale for formatting numeric quantities. These additional numbering systems are often used in very specific contexts, such as in calendars or for financial purposes. There are currently three defined categories, as follows:

* `native`:
  * **Definition**: Defines the numbering system used for the native digits, usually defined as a part of the script used to write the language.
  * **Constraint**: The native numbering system can only be a numeric positional decimal-digit numbering system, using digits with `General_Category=Decimal_Number`.
  * **Fallback**: In locales where the native numbering system is the default, it is assumed that the numbering system `"latn"` (Western digits 0–9) is always acceptable, and can be selected using the `-nu` keyword as part of a Unicode locale identifier.

* `traditional`:
  * **Definition**: Defines the traditional numerals for a locale.
  * **Representation**: This numbering system may be numeric or algorithmic.
  * **Fallback**: If the traditional numbering system is not defined, applications should use the native numbering system as a fallback.

* `finance`:
  * **Definition**: Defines the numbering system used for financial quantities.
  * **Representation**: This numbering system may be numeric or algorithmic.
  * **Usage**: Often used for ideographic languages such as Chinese, where it would be easy to alter an amount represented in the default numbering system simply by adding additional strokes.
  * **Fallback**: If the financial numbering system is not specified, applications should use the default numbering system as a fallback.

The categories defined for other numbering systems can be used in a Unicode locale identifier to select the proper numbering system without having to know the specific numbering system by name. For example:

* To select Hindi language using the native digits for numeric formatting, use locale ID: `"hi-IN-u-nu-native"`.
* To select Chinese language using the appropriate financial numerals, use locale ID: `"zh-u-nu-finance"`.
* To select Tamil language using the traditional Tamil numerals, use locale ID: `"ta-u-nu-traditio"`.
* To select Arabic language using western digits 0–9, use locale ID: `"ar-u-nu-latn"`.

For more information on numbering systems and their definitions, see _[Section 1: Numbering Systems](#Numbering_Systems)_.

### <a name="Number_Symbols" id="Number_Symbols" href="#Number_Symbols">Number Symbols</a>

```dtd
<!ELEMENT symbols (alias | (decimal*, group*, list*, percentSign*, nativeZeroDigit*, patternDigit*, plusSign*, minusSign*, approximatelySign*, exponential*, superscriptingExponent*, perMille*, infinity*, nan*, currencyDecimal*, currencyGroup*, timeSeparator*, special*)) >
```

Number symbols define the localized symbols that are commonly used when formatting numbers in a given locale. These symbols can be referenced using a number formatting pattern as defined in _[Section 3: Number Format Patterns](#Number_Format_Patterns)_.

The available number symbols are as follows:

* `decimal`:
  * **Definition**: Separates the integer and fractional part of the number.

* `group`:
  * **Definition**: Separates clusters of integer digits to make large numbers more legible; commonly used for thousands (grouping size 3, e.g. `"100,000,000"`) or in some locales, ten-thousands (grouping size 4, e.g. `"1,0000,0000"`).
  * **Grouping Sizes**: There may be two different grouping sizes:
    * **Primary Grouping Size**: Used for the least significant integer group.
    * **Secondary Grouping Size**: Used for more significant groups; these are not the same in all locales (e.g. `"12,34,56,789"`).
  * **Pattern Parsing**: If a pattern contains multiple grouping separators, the interval between the last one and the end of the integer defines the primary grouping size, and the interval between the last two defines the secondary grouping size. All others are ignored, so `"#,##,###,####"` == `"###,###,####"` == `"##,#,###,####"`.

* `list`:
  * **Definition**: Symbol used to separate numbers in a list intended to represent structured data such as an array; must be different from the `decimal` value.
  * **Scope**: This list separator is for “non-linguistic” usage as opposed to the listPatterns for “linguistic” lists (e.g. “Bob, Carol, and Ted”) described in Part 2, _[List Patterns](tr35-general.md#ListPatterns)_.

* `percentSign`:
  * **Definition**: Symbol used to indicate a percentage (1/100th) amount.
  * **Formatting Rule**: If present, the numeric value is also multiplied by 100 before formatting (e.g. `1.23` → `123%`).

* ~~`nativeZeroDigit`~~:
  * **Status**: *Deprecated — do not use.*

* ~~`patternDigit`~~:
  * **Status**: *Deprecated.* Formerly used to provide the localized pattern character corresponding to `'#'`, but localization of the pattern characters themselves has been deprecated for some time (determining the locale-specific _replacements_ for pattern characters is part of normal number formatting).

* `minusSign`:
  * **Definition**: Symbol used to denote a negative value.

* `plusSign`:
  * **Definition**: Symbol used to denote a positive value.
  * **Substitution Rule**: It can be used to produce modified patterns, so that `3.12` is formatted as `"+3.12"`, for example. The standard number patterns (except for `type="accounting"`) will contain the `minusSign`, explicitly or implicitly. In the explicit pattern, the value of the `plusSign` can be substituted for the value of the `minusSign` to produce a pattern that has an explicit plus sign.

* `approximatelySign`:
  * **Definition**: Symbol used to denote a value that is approximate but not exact.
  * **Substitution Rule**: Substituted in place of `minusSign` using the same semantics as `plusSign` substitution.

* `exponential`:
  * **Definition**: Symbol separating the mantissa and exponent values in scientific notation.

* `superscriptingExponent`:
  * **Definition**: Exponent notation used to show a format like “1.23 × 10⁴”. (Programmers are used to the fallback exponent style “1.23E4”, but that should not be shown to end-users).
  * **Formatting**: The superscripting can use markup, such as `<sup>4</sup>` in HTML, or for the special case of Latin digits, use the superscript characters: U+207B ( ⁻ ), U+2070 ( ⁰ ), U+00B9 ( ¹ ), U+00B2 ( ² ), U+00B3 ( ³ ), U+2074 ( ⁴ ) .. U+2079 ( ⁹ ).

* `perMille`:
  * **Definition**: Symbol used to indicate a per-mille (1/1000th) amount.
  * **Formatting Rule**: If present, the numeric value is also multiplied by 1000 before formatting (e.g. `1.23` → `1230 ‰`).

* `infinity`:
  * **Definition**: The infinity sign. Corresponds to the IEEE infinity bit pattern.

* `nan`:
  * **Definition**: The NaN (Not a Number) sign. Corresponds to the IEEE NaN bit pattern.

* `currencyDecimal`:
  * **Definition**: *Optional.* If specified, then for currency formatting/parsing this is used as the decimal separator instead of using the regular decimal separator; otherwise, the regular decimal separator is used.

* `currencyGroup`:
  * **Definition**: *Optional.* If specified, then for currency formatting/parsing this is used as the group separator instead of using the regular group separator; otherwise, the regular group separator is used.

* `timeSeparator`:
  * **Definition**: Replaces any use of the `timeSeparator` pattern character in a date-time format pattern. This allows the same time format to be used for multiple number systems when the time separator depends on the number system (e.g. COLON for Latin digits, but ARABIC COMMA in traditional print styles).
  * **Note**: In CLDR 26 the `timeSeparator` pattern character was specified to be COLON. This was withdrawn in CLDR 28 due to backward compatibility issues, and no `timeSeparator` pattern character is currently defined. In the meantime, since CLDR data consumers can still request the `timeSeparator` symbol, it should match the symbol actually used in the [timeFormats](tr35-dates.md#timeFormats) and [availableFormats](tr35-dates.md#availableFormats_appendItems) items.

Example:

```xml
<symbols numberSystem="latn">
    <decimal>.</decimal>
    <group>,</group>
    <list>;</list>
    <percentSign>%</percentSign>
    <patternDigit>#</patternDigit>
    <plusSign>+</plusSign>
    <minusSign>-</minusSign>
    <approximatelySign>~</approximatelySign>
    <exponential>E</exponential>
    <superscriptingExponent>×</superscriptingExponent>
    <perMille>‰</perMille>
    <infinity>∞</infinity>
    <nan>☹</nan>
    <timeSeparator>:</timeSeparator>
</symbols>
```

```dtd
<!ATTLIST symbols numberSystem CDATA #IMPLIED >
```
* The `numberSystem` attribute is used to specify that the given number symbols are to be used when the given numbering system is active. Number symbols can only be defined for numbering systems of the "numeric" type, since any special symbols required for an algorithmic numbering system should be specified by the RBNF formatting rules used for that numbering system. The `numberSystem` attribute will always be present in CLDR 49 and beyond. The DTD does not require it, so that older versions of CLDR can be read with as before.  Locales that specify a numbering system other than "latn" as the default should also specify number formatting symbols that are appropriate for use within the context of the given numbering system. For example, a locale that uses the Arabic-Indic digits as its default would likely use an Arabic comma for the grouping separator rather than the ASCII comma.

For more information on numbering systems and their definitions, see _[Section 1: Numbering Systems](#Numbering_Systems)_.

### <a name="Number_Formats" id="Number_Formats" href="#Number_Formats">Number Formats</a>

```dtd
<!ELEMENT decimalFormats (alias | (default*, decimalFormatLength*, special*)) >
<!ELEMENT decimalFormatLength (alias | (default*, decimalFormat*, special*)) >
<!ATTLIST decimalFormatLength type ( full | long | medium | short ) #IMPLIED >
<!ELEMENT decimalFormat (alias | (pattern*, special*)) >
```

(scientificFormats, percentFormats have the same structure)

Number formats are used to define the rules for formatting numeric quantities using the pattern syntax described in _[Section 3: Number Format Patterns](#Number_Format_Patterns)_.

Different formats are provided for different contexts, as follows:

**decimalFormats**

> The normal locale specific way to write a base 10 number. Variations of the decimalFormat pattern are provided that allow compact number formatting.

**percentFormats**

> Pattern for use with percentage formatting

**scientificFormats**

> Pattern for use with scientific (exponent) formatting.

Example:

```xml
<decimalFormats numberSystem="latn">
  <decimalFormatLength type="long">
    <decimalFormat>
      <pattern>#,##0.###</pattern>
    </decimalFormat>
  </decimalFormatLength>
</decimalFormats>

<scientificFormats numberSystem="latn">
  <default type="long"/>
  <scientificFormatLength type="long">
    <scientificFormat>
      <pattern>0.000###E+00</pattern>
    </scientificFormat>
  </scientificFormatLength>
  <scientificFormatLength type="medium">
    <scientificFormat>
      <pattern>0.00##E+00</pattern>
    </scientificFormat>
  </scientificFormatLength>
</scientificFormats>

<percentFormats numberSystem="latn">
  <percentFormatLength type="long">
    <percentFormat>
      <pattern>#,##0%</pattern>
    </percentFormat>
  </percentFormatLength>
</percentFormats>
```

```dtd
<!ATTLIST symbols numberSystem CDATA #IMPLIED >
```

* The `numberSystem` attribute is used to specify that the given number formatting pattern(s) are to be used when the given numbering system is active. By default, number formatting patterns without a specific `numberSystem` attribute are assumed to be used for the "latn" numbering system, which is western (ASCII) digits; however, number formatting patterns without a specific `numberSystem` attribute should not be used and will be deprecated in CLDR v48. Locales that specify a numbering system other than "latn" as the default should also specify number formatting patterns that are appropriate for use within the context of the given numbering system.

For more information on numbering systems and their definitions, see _[Section 1: Numbering Systems](#Numbering_Systems)_.

#### <a name="Compact_Number_Formats" id="Compact_Number_Formats" href="#Compact_Number_Formats">Compact Number Formats</a>

A pattern `type` attribute is used for _compact number formats_, such as the following:

```xml
<decimalFormatLength type="long">
	<decimalFormat>
		<pattern type="1000" count="one">0 thousand</pattern>
		<pattern type="1000" count="other">0 thousand</pattern>
		<pattern type="10000" count="one">00 thousand</pattern>
		<pattern type="10000" count="other">00 thousand</pattern>
		<pattern type="100000" count="one">000 thousand</pattern>
		<pattern type="100000" count="other">000 thousand</pattern>
		<pattern type="1000000" count="one">0 million</pattern>
		<pattern type="1000000" count="other">0 million</pattern>
		<pattern type="10000000" count="one">00 million</pattern>
		<pattern type="10000000" count="other">00 million</pattern>
…
	</decimalFormat>
</decimalFormatLength>
<decimalFormatLength type="short">
	<decimalFormat>
		<pattern type="1000" count="one">0K</pattern>
		<pattern type="1000" count="other">0K</pattern>
		<pattern type="10000" count="one">00K</pattern>
		<pattern type="10000" count="other">00K</pattern>
		<pattern type="100000" count="one">000K</pattern>
		<pattern type="100000" count="other">000K</pattern>
		<pattern type="1000000" count="one">0M</pattern>
		<pattern type="1000000" count="other">0M</pattern>
		<pattern type="10000000" count="one">00M</pattern>
		<pattern type="10000000" count="other">00M</pattern>
…
	</decimalFormat>
</decimalFormatLength>
…
<currencyFormatLength type="short">
    <currencyFormat type="standard">
		<pattern type="1000" count="one">¤0K</pattern>
		<pattern type="1000" count="one" alt="alphaNextToNumber">¤ 0K</pattern>
		<pattern type="1000" count="other">¤0K</pattern>
		<pattern type="1000" count="other" alt="alphaNextToNumber">¤ 0K</pattern>
		<pattern type="10000" count="one">¤00K</pattern>
		<pattern type="10000" count="one" alt="alphaNextToNumber">¤ 00K</pattern>
		<pattern type="10000" count="other">¤00K</pattern>
		<pattern type="10000" count="other" alt="alphaNextToNumber">¤ 00K</pattern>
		<pattern type="100000" count="one">¤000K</pattern>
		<pattern type="100000" count="one" alt="alphaNextToNumber">¤ 000K</pattern>
		<pattern type="100000" count="other">¤000K</pattern>
		<pattern type="100000" count="other" alt="alphaNextToNumber">¤ 000K</pattern>
		<pattern type="1000000" count="one">¤0M</pattern>
		<pattern type="1000000" count="one" alt="alphaNextToNumber">¤ 0M</pattern>
		<pattern type="1000000" count="other">¤0M</pattern>
		<pattern type="1000000" count="other" alt="alphaNextToNumber">¤ 0M</pattern>
		<pattern type="10000000" count="one">¤00M</pattern>
		<pattern type="10000000" count="one" alt="alphaNextToNumber">¤ 00M</pattern>
		<pattern type="10000000" count="other">¤00M</pattern>
		<pattern type="10000000" count="other" alt="alphaNextToNumber">¤ 00M</pattern>        …
    </currencyFormat>
</currencyFormatLength>
```

Formats can be supplied for numbers (as above) or for currencies or other units. They can also be used with ranges of numbers, resulting in formatting strings like “$10K” or “$3–7M”.

To format a number N, use the following steps:

Notes:
- A _letter grapheme cluster_ is a grapheme cluster that starts with a letter and then 0 or more combining marks.
For example, each of the following are are _letter grapheme clusters_: \`<q>`, \<q, _combining ring above_>, \<q, _combining ring above_, _acute accent_>.
- All of the pattern elements with the same type must have the same number of zeros in the pattern element value.
- The examples use N = 123456, the currency = CAD, and the currency symbol string = "$CA"

1. Let P be the pattern element with greatest type less than or equal to N, and any count value.
    * P = ``<pattern type="100000" count="**one**">`¤000K`</pattern>``
2. Let V be the pattern element value.
    * V = "¤000K"
3. If the element value of P is "0", then use the corresponding non-compact number formatting instead, and skip the rest of these steps — but adjust the precision as described below.
    * For example, instead of `currencyFormat` ``<pattern type="10000" count="one">`¤00K`</pattern>``, use ``<pattern>`¤#,##0.00`</pattern>``.
4. If P is a currency format, look at the currency symbol string, and the position of the currency symbol ¤ in the pattern element value.
If ¤ is immediately to the left of a 0 and the currency string ends with a _letter grapheme cluster_ (eg, "$CA"),
or to the right and the currency starts with a letter (eg, "CA$"),
then switch to the `alt=alphaNextToNumber` pattern, if there is one.
    * P = ``<pattern type="100000" count="**one**" alt="alphaNextToNumber">`¤ 000K`</pattern>`` // with the currency symbol "CA$"
    * V = "¤ 000K"
5. Let Z be the number of 0 characters in V, minus 1.
    * Z = 2
6. Let T be the numeric value of the `type` attribute value, after removing the final Z zeros.
    * "100000" removing "00" = "1000"
    * T  = 1000
7. Let N' be N / T
    * N = 123.456
8. Determine the plural category of N, based on the numeric precision settings (the min/max number of significant or fraction digits), and switch  the value of V if necessary.
    * In this case, the plural category of 123.456 in English with any precision is "other", so the
    * P = ``<pattern type="100000" count="**other**" alt="alphaNextToNumber">`¤ 000K`</pattern>``
    * V = "¤ 000K"
    * For the short compact formats, it doesn't make a difference for English, but may for other locales!
9. Let V' be the same as V, but replacing that sequence of zeros by "{0}".
    * V' = "¤ {0}K"
10. Let F be N' formatted according to V' and the numeric precision settings.
    * F = "$CA 123K"   // where the precision is min = max = 3 significant digits
    * F = "$CA 123.4K" // where the precision is min = max = 1 fraction digit


* The default pattern for any type that is not supplied is the special value “0”, as in the following. The value “0” must be used when a child locale overrides a parent locale to drop the compact pattern for that type and use the default pattern.


 ``<pattern type="1" count="one">`0`</pattern>``

* If the value is precisely “0”, either explicit or defaulted, then the normal number format pattern for that sort of object is supplied — either `<decimalFormat>` or `<currencyFormat type="standard">` — with the normal formatting for the locale (such as the grouping separators). However, for the “0” case by default the significant digits are adjusted for consistency, typically to 2 or 3 digits, and the maximum fractional digits are set to 0 (for both currencies and plain decimal). Thus the output would be $12, not $12.01. APIs may, however, allow these default behaviors to be overridden.


* With the data above, N=12345 matches ``<pattern type="10000" count="other">`00 K`</pattern>``. N is divided by 1000 (obtained from 10000 after removing "00" and restoring one "0"). The result is formatted according to the normal decimal pattern. With no fractional digits, that yields "12 K".


* Formatting 1200 in USD would result in “1.2 K $”, while 990 implicitly maps to the special value “0”, which maps to `<currencyFormat type="standard">`<pattern>`#,##0.00 ¤`</pattern>``, and would result in simply “990 $”.


The short non-currency format is designed for UI environments where space is at a premium, and should ideally result in a formatted string no more than about 6 em wide (with no fractional digits).
The short currency format will include currency symbols, and should ideally be no more than 8 em in width.

#### <a name="Currency_Formats" id="Currency_Formats" href="#Currency_Formats">Currency Formats</a>

Patterns for use with currency formatting:

```dtd
<!ELEMENT currencyFormats (alias | (default*, currencySpacing*, currencyFormatLength*, currencyPatternAppendISO*, unitPattern*, special*)) >
<!ELEMENT currencyFormatLength (alias | (default*, currencyFormat*, special*)) >
<!ATTLIST currencyFormatLength type ( full | long | medium | short ) #IMPLIED >
<!ELEMENT currencyFormat (alias | (pattern*, special*)) >
<!ATTLIST currencyFormat type NMTOKEN "standard" >
    <!--@MATCH:literal/accounting, standard-->
<!ELEMENT currencyPatternAppendISO ( #PCDATA ) >
```

#### <a name="Element_Placement" id="Element_Placement" href="#Element_Placement">Element Placement</a>

* The following additional elements were intended to allow proper placement of the currency symbol relative to the numeric quantity. These are specified in the root locale and typically not overridden in any other locale. However, as of CLDR 42, the preferred approach to controlling placement of the currency symbol is use of the `alt="alphaNextToNumber"` variant for `currencyFormat` `pattern`s. See below and _[- Currencies](#Currencies)_ for additional information on the use of these options.


```dtd
<!ELEMENT currencySpacing (alias | (beforeCurrency*, afterCurrency*, special*)) >
<!ELEMENT beforeCurrency (alias | (currencyMatch*, surroundingMatch*, insertBetween*)) >
<!ELEMENT afterCurrency (alias | (currencyMatch*, surroundingMatch*, insertBetween*)) >
<!ELEMENT currencyMatch ( #PCDATA ) >
<!ELEMENT surroundingMatch ( #PCDATA ) >
<!ELEMENT insertBetween ( #PCDATA ) >
```

#### <a name="Format_Types_and_Display_Forms" id="Format_Types_and_Display_Forms" href="#Format_Types_and_Display_Forms">Format Types & Display Forms</a>

* In addition to a standard currency format, in which negative currency amounts might typically be displayed as something like “-$3.27”, locales may provide an "accounting" form, in which for "en_US" the same example would appear as “($3.27)”. The locale keyword "cf" can be used to select the standard or accounting form, see [Unicode Currency Format Identifier](tr35.md#UnicodeCurrencyFormatIdentifier).


```xml
<currencyFormats>
    <currencyFormatLength>
        <currencyFormat type="standard">
            <pattern>¤#,##0.00</pattern>
            <pattern alt="alphaNextToNumber">¤ #,##0.00</pattern>
            <pattern alt="noCurrency">#,##0.00</pattern>
        </currencyFormat>
        <currencyFormat type="accounting">
            <pattern>¤#,##0.00;(¤#,##0.00)</pattern>
            <pattern alt="alphaNextToNumber">¤ #,##0.00;(¤ #,##0.00)</pattern>
            <pattern alt="noCurrency">#,##0.00;(#,##0.00)</pattern>
        </currencyFormat>
    </currencyFormatLength>
    <currencyFormatLength type="short">
        <currencyFormat type="standard">
            <pattern type="1000" count="one">¤0K</pattern>
            <pattern type="1000" count="one" alt="alphaNextToNumber">¤ 0K</pattern>
            <pattern type="1000" count="other">¤0K</pattern>
            <pattern type="1000" count="other" alt="alphaNextToNumber">¤ 0K</pattern>
            ...
            <pattern type="100000000000000" count="other">¤000T</pattern>
            <pattern type="100000000000000" count="other" alt="alphaNextToNumber">¤ 000T</pattern>
        </currencyFormat>
    </currencyFormatLength>
</currencyFormats>
```

#### <a name="Pattern_Variant_Usage_Rules" id="Pattern_Variant_Usage_Rules" href="#Pattern_Variant_Usage_Rules">Pattern Variant Usage Rules</a>

* The `alt="alphaNextToNumber"` pattern, if available, should be used instead of the standard pattern when the currency symbol character closest to the numeric value has Unicode General Category L (letter). The `alt="alphaNextToNumber"` pattern is typically provided when the standard currency pattern does not have a space between currency symbol and numeric value; the alphaNextToNumber variant adds a non-breaking space if appropriate for the locale.


* The `alt="noCurrency"` pattern can be used when a currency-style format is desired but without the currency symbol. This sort of display may be used when formatting a large column of values all in the same currency, for example. For compact currency formats (`<currencyFormatLength type="short">`), the compact decimal format (`<decimalFormatLength type="short">`) should be used if no `alt="noCurrency"` pattern is present (so the `alt="noCurrency"` pattern is typically not needed for compact currency formats).


```xml
<currencyPatternAppendISO>{0} ¤¤</currencyPatternAppendISO>
```

* The `currencyPatternAppendISO` element provides a pattern that can be used to combine currency format that uses a currency symbol (¤ or ¤¤¤¤¤) with the ISO 4217 3-letter code for the same currency (¤¤), to produce a result such as “$1,432.00 USD”. Using such a format is only recommended to resolve ambiguity when:

* The currency symbol being used is the narrow symbol (¤¤¤¤¤) or has the same value as the narrow symbol, and
* The currency symbol does not have the same value as the ISO 4217 3-letter code.
Most locales will not need to override the pattern provided in root, shown in the xml sample above.

### <a name="Miscellaneous_Patterns" id="Miscellaneous_Patterns" href="#Miscellaneous_Patterns">Miscellaneous Patterns</a>

```dtd
<!ELEMENT miscPatterns (alias | (default*, pattern*, special*)) >
<!ATTLIST miscPatterns numberSystem CDATA #IMPLIED >
```

The miscPatterns supply additional patterns for special purposes. The currently defined values are:

* `approximately`: Indicates an approximate number, such as “~99” (not currently in use; see ICU-20163).
* `atMost`: Indicates a number or lower, such as “≤99” (99 items or fewer).
* `atLeast`: Indicates a number or higher, such as “99+” (99 items or more).
* `range`: Indicates a range of numbers, such as “99–103” (from 99 to 103 items).

_For example:_

```xml
<miscPatterns numberSystem="…">
  <pattern type="approximately">~{0}</pattern>
  <pattern type="atLeast">≥{0}</pattern>
  <pattern type="atMost">≤{0}</pattern>
  <pattern type="range">{0}–{1}</pattern>
</miscPatterns>
```

### <a name="Minimal_Pairs" id="Minimal_Pairs" href="#Minimal_Pairs">Minimal Pairs</a>

```dtd
<!ELEMENT minimalPairs ( alias | ( pluralMinimalPairs*, ordinalMinimalPairs*, caseMinimalPairs*, genderMinimalPairs*, special* ) ) >
```
```dtd
<!ELEMENT pluralMinimalPairs ( #PCDATA ) >
<!ATTLIST pluralMinimalPairs count NMTOKEN #IMPLIED >
```
```dtd
<!ELEMENT ordinalMinimalPairs ( #PCDATA ) >
<!ATTLIST ordinalMinimalPairs ordinal NMTOKEN #IMPLIED >
```

```dtd
<!ELEMENT caseMinimalPairs ( #PCDATA ) >
<!ATTLIST caseMinimalPairs case NMTOKEN #REQUIRED >
```

```dtd
<!ELEMENT genderMinimalPairs ( #PCDATA ) >
<!ATTLIST genderMinimalPairs gender NMTOKEN #REQUIRED >
```

* Minimal pairs provide examples that justify why multiple plural or ordinal categories exist, and for providing contextual examples for verifying consistency of translations. The allowable values for the `count`, `ordinal`, `case`, and `gender` attributes are found in the dtd file.


Examples

```xml
<minimalPairs>
    <pluralMinimalPairs count="one">{0} Tag</pluralMinimalPairs>
    <pluralMinimalPairs count="other">{0} Tage</pluralMinimalPairs>

    <ordinalMinimalPairs ordinal="other">{0}. Abzweigung nach rechts nehmen</ordinalMinimalPairs>

    <caseMinimalPairs case="accusative">… für {0} …</caseMinimalPairs>
    <caseMinimalPairs case="dative">… mit {0} …</caseMinimalPairs>
    <caseMinimalPairs case="genitive">Anstatt {0} …</caseMinimalPairs>
    <caseMinimalPairs case="nominative">{0} kostet (kosten) € 3,50.</caseMinimalPairs>

    <genderMinimalPairs gender="feminine">Die {0} ist …</genderMinimalPairs>
    <genderMinimalPairs gender="masculine">Der {0} ist …</genderMinimalPairs>
    <genderMinimalPairs gender="neuter">Das {0} ist …</genderMinimalPairs>
</minimalPairs>
```


For more information, see [Plural Rules](https://cldr.unicode.org/index/cldr-spec/plural-rules) and [Grammatical Inflection](https://cldr.unicode.org/translation/grammatical-inflection).


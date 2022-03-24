## Unicode Technical Standard #35

# Unicode Locale Data Markup Language (LDML)<br/>Part 3: Numbers

<!-- HTML: no th -->
<table><tbody>
<tr><td>Version</td><td>41</td></tr>
<tr><td>Editors</td><td>Shane F. Carr (<a href="mailto:shane@unicode.org">shane@unicode.org</a>) and <a href="tr35.html#Acknowledgments">other CLDR committee members</a></td></tr>
</tbody></table>

For the full header, summary, and status, see [Part 1: Core](tr35.md).

### _Summary_

This document describes parts of an XML format (_vocabulary_) for the exchange of structured locale data. This format is used in the [Unicode Common Locale Data Repository](https://unicode.org/cldr/).

This is a partial document, describing only those parts of the LDML that are relevant for number and currency formatting. For the other parts of the LDML see the [main LDML document](tr35.md) and the links above.

### _Status_

_This is a draft document which may be updated, replaced, or superseded by other documents at any time. Publication does not imply endorsement by the Unicode Consortium. This is not a stable document; it is inappropriate to cite this document as other than a work in progress._

> _**A Unicode Technical Standard (UTS)** is an independent specification. Conformance to the Unicode Standard does not imply conformance to any UTS._

_Please submit corrigenda and other comments with the CLDR bug reporting form [[Bugs](tr35.md#Bugs)]. Related information that is useful in understanding this document is found in the [References](tr35.md#References). For the latest version of the Unicode Standard see [[Unicode](tr35.md#Unicode)]. For a list of current Unicode Technical Reports see [[Reports](tr35.md#Reports)]. For more information about versions of the Unicode Standard, see [[Versions](tr35.md#Versions)]._

## <a name="Parts" href="#Parts">Parts</a>

The LDML specification is divided into the following parts:

*   Part 1: [Core](tr35.md#Contents) (languages, locales, basic structure)
*   Part 2: [General](tr35-general.md#Contents) (display names & transforms, etc.)
*   Part 3: [Numbers](tr35-numbers.md#Contents) (number & currency formatting)
*   Part 4: [Dates](tr35-dates.md#Contents) (date, time, time zone formatting)
*   Part 5: [Collation](tr35-collation.md#Contents) (sorting, searching, grouping)
*   Part 6: [Supplemental](tr35-info.md#Contents) (supplemental data)
*   Part 7: [Keyboards](tr35-keyboards.md#Contents) (keyboard mappings)

## <a name="Contents" href="#Contents">Contents of Part 3, Numbers</a>

*   1 [Numbering Systems](#Numbering_Systems)
*   2 [Number Elements](#Number_Elements)
    *   2.1 [Default Numbering System](#defaultNumberingSystem)
    *   2.2 [Other Numbering Systems](#otherNumberingSystems)
    *   2.3 [Number Symbols](#Number_Symbols)
    *   2.4 [Number Formats](#Number_Formats)
        *   2.4.1 [Compact Number Formats](#Compact_Number_Formats)
        *   2.4.2 [Currency Formats](#Currency_Formats)
    *   2.5 [Miscellaneous Patterns](#Miscellaneous_Patterns)
    *   2.6 [Minimal Pairs](#Minimal_Pairs)
*   3 [Number Format Patterns](#Number_Format_Patterns)
    *   3.1 [Number Patterns](#Number_Patterns)
        *   Table: [Number Pattern Examples](#Number_Pattern_Examples)
    *   3.2 [Special Pattern Characters](#Special_Pattern_Characters)
        *   Table: [Number Pattern Character Definitions](#Number_Pattern_Character_Definitions)
        *   Table: [Sample Patterns and Results](#Sample_Patterns_and_Results)
        *   Table: [Examples of minimumGroupingDigits](#Examples_of_minimumGroupingDigits)
        *   3.2.1 [Explicit Plus Signs](#Explicit_Plus)
    *   3.3 [Formatting](#Formatting)
    *   3.4 [Scientific Notation](#sci)
    *   3.5 [Significant Digits](#sigdig)
        *   Table: [Significant Digits Examples](#Significant_Digits_Examples)
    *   3.6 [Padding](#Padding)
    *   3.7 [Rounding](#Rounding)
    *   3.8 [Quoting Rules](#Quoting_Rules)
*   4 [Currencies](#Currencies)
    *   4.1 [Supplemental Currency Data](#Supplemental_Currency_Data)
*   5 [Language Plural Rules](#Language_Plural_Rules)
    *   5.1 [Plural rules syntax](#Plural_rules_syntax)
        *   5.1.1 [Operands](#Operands)
            *   Table: [Plural Operand Meanings](#Plural_Operand_Meanings)
            *   Table: [Plural Operand Examples](#Plural_Operand_Examples)
        *   5.1.2 [Relations](#Relations)
            *   Table: [Relations Examples](#Relations_Examples)
            *   Table: [Plural Rules Examples](#Plural_Rules_Examples)
        *   5.1.3 [Samples](#Samples)
            *   Table: [Plural Samples Examples](#Plural_Samples_Examples)
        *   5.1.4 [Using Cardinals](#Using_cardinals)
    *   5.2 [Plural Ranges](#Plural_Ranges)
*   6 [Rule-Based Number Formatting](#Rule-Based_Number_Formatting)
*   7 [Parsing Numbers](#Parsing_Numbers)
*   8 [Number Range Formatting](#Number_Range_Formatting)
    *   8.1 [Approximate Number Formatting](#Approximate_Number_Formatting)
    *   8.2 [Collapsing Number Ranges](#Collapsing_Number_Ranges)
    *   8.3 [Range Pattern Processing](#Range_Pattern_Processing)

## 1 <a name="Numbering_Systems" href="#Numbering_Systems">Numbering Systems</a>

```xml
<!ELEMENT numberingSystems ( numberingSystem* ) >  
<!ELEMENT numberingSystem EMPTY >  
<!ATTLIST numberingSystem id NMTOKEN #REQUIRED >  
<!ATTLIST numberingSystem type ( numeric | algorithmic ) #REQUIRED >  
<!ATTLIST numberingSystem radix NMTOKEN #IMPLIED >  
<!ATTLIST numberingSystem digits CDATA #IMPLIED >  
<!ATTLIST numberingSystem rules CDATA #IMPLIED >  
```

Numbering systems information is used to define different representations for numeric values to an end user. Numbering systems are defined in CLDR as one of two different types: algorithmic and numeric. Numeric systems are simply a decimal based system that uses a predefined set of digits to represent numbers. Examples are Western ( ASCII digits ), Thai digits, Devanagari digits. Algorithmic systems are more complex in nature, since the proper formatting and presentation of a numeric quantity is based on some algorithm or set of rules. Examples are Chinese numerals, Hebrew numerals, or Roman numerals. In CLDR, the rules for presentation of numbers in an algorithmic system are defined using the RBNF syntax described in _[Section 6: Rule-Based Number Formatting](#Rule-Based_Number_Formatting)_.

Attributes for the `<numberingSystem>` element are as follows:

- `id` - Specifies the name of the numbering system that can be used to designate its use in formatting.
- `type` - Specifies whether the numbering system is algorithmic or numeric.
- `digits` - For numeric systems, specifies the digits used to represent numbers, in order, starting from zero.
- `rules` - Specifies the RBNF ruleset to be used for formatting numbers from this numbering system. The rules specifier can contain simply a ruleset name, in which case the ruleset is assumed to be found in the rule set grouping "NumberingSystemRules". Alternatively, the specifier can denote a specific locale, ruleset grouping, and ruleset name, separated by slashes.

Examples:

```xml
<!-- ASCII digits - A numeric system -->
<numberingSystem id="latn" type="numeric" digits="0123456789"/>

<!-- A numeric system using Thai digits -->
<numberingSystem id="thai" type="numeric" digits="๐๑๒๓๔๕๖๗๘๙"/>

<!-- An algorithmic system - Georgian numerals , rules found in NumberingSystemRules -->
<numberingSystem id="geor" type="algorithmic" rules="georgian"/>

<!-- An algorithmic system. Traditional Chinese Numerals -->
<numberingSystem id="hant" type="algorithmic" rules="zh_Hant/SpelloutRules/spellout-cardinal"/>
```

For general information about the numbering system data, including the BCP47 identifiers, see the main document _Section Q.1.1 [Numbering System Data](tr35.md#Numbering%20System%20Data)._

## 2 <a name="Number_Elements" href="#Number_Elements">Number Elements</a>

```xml
<!ELEMENT numbers ( alias | ( defaultNumberingSystem*, otherNumberingSystems*, minimumGroupingDigits*, symbols*, decimalFormats*, scientificFormats*, percentFormats*, currencyFormats*, currencies?, miscPatterns*, minimalPairs*, special* ) ) >
```

The numbers element supplies information for formatting and parsing numbers and currencies. It has the following sub-elements: `<defaultNumberingSystem>`, `<otherNumberingSystems>`, `<symbols>`, `<decimalFormats>`, `<scientificFormats>`, `<percentFormats>`, `<currencyFormats>`, and `<currencies>`. The currency IDs are from [[ISO4217](tr35.md#ISO4217)] (plus some additional common-use codes). For more information, including the pattern structure, see _[Section 3: Number Format Patterns](#Number_Format_Patterns)_.

### 2.1 <a name="defaultNumberingSystem" href="#defaultNumberingSystem">Default Numbering System</a>

```xml
<!ELEMENT defaultNumberingSystem ( #PCDATA )>
```

This element indicates which numbering system should be used for presentation of numeric quantities in the given locale.

### 2.2 <a name="otherNumberingSystems" href="#otherNumberingSystems">Other Numbering Systems</a>

```xml
<!ELEMENT otherNumberingSystems ( alias | ( native*, traditional*, finance*)) >
```

This element defines general categories of numbering systems that are sometimes used in the given locale for formatting numeric quantities. These additional numbering systems are often used in very specific contexts, such as in calendars or for financial purposes. There are currently three defined categories, as follows:

**native**

> Defines the numbering system used for the native digits, usually defined as a part of the script used to write the language. The native numbering system can only be a numeric positional decimal-digit numbering system, using digits with General_Category=Decimal_Number. Note: In locales where the native numbering system is the default, it is assumed that the numbering system "latn" ( Western Digits 0-9 ) is always acceptable, and can be selected using the -nu keyword as part of a Unicode locale identifier.

**traditional**

> Defines the traditional numerals for a locale. This numbering system may be numeric or algorithmic. If the traditional numbering system is not defined, applications should use the native numbering system as a fallback.

**finance**

> Defines the numbering system used for financial quantities. This numbering system may be numeric or algorithmic. This is often used for ideographic languages such as Chinese, where it would be easy to alter an amount represented in the default numbering system simply by adding additional strokes. If the financial numbering system is not specified, applications should use the default numbering system as a fallback.

The categories defined for other numbering systems can be used in a Unicode locale identifier to select the proper numbering system without having to know the specific numbering system by name. For example:

*   To select Hindi language using the native digits for numeric formatting, use locale ID: "hi-IN-u-nu-native".
*   To select Chinese language using the appropriate financial numerals, use locale ID: "zh-u-nu-finance".
*   To select Tamil language using the traditional Tamil numerals, use locale ID: "ta-u-nu-traditio".
*   To select Arabic language using western digits 0-9, use locale ID: "ar-u-nu-latn".

For more information on numbering systems and their definitions, see _[Section 1: Numbering Systems](#Numbering_Systems)_.

### 2.3 <a name="Number_Symbols" href="#Number_Symbols">Number Symbols</a>

```xml
<!ELEMENT symbols (alias | (decimal*, group*, list*, percentSign*, nativeZeroDigit*, patternDigit*, plusSign*, minusSign*, approximatelySign*, exponential*, superscriptingExponent*, perMille*, infinity*, nan*, currencyDecimal*, currencyGroup*, timeSeparator*, special*)) >
```

Number symbols define the localized symbols that are commonly used when formatting numbers in a given locale. These symbols can be referenced using a number formatting pattern as defined in _[Section 3: Number Format Patterns](#Number_Format_Patterns)_.

The available number symbols are as follows:

**decimal**

> separates the integer and fractional part of the number.

**group**

> separates clusters of integer digits to make large numbers more legible; commonly used for thousands (grouping size 3, e.g. "100,000,000") or in some locales, ten-thousands (grouping size 4, e.g. "1,0000,0000"). There may be two different grouping sizes: The _primary grouping size_ used for the least significant integer group, and the _secondary grouping size_ used for more significant groups; these are not the same in all locales (e.g. "12,34,56,789"). If a pattern contains multiple grouping separators, the interval between the last one and the end of the integer defines the primary grouping size, and the interval between the last two defines the secondary grouping size. All others are ignored, so "#,##,###,####" == "###,###,####" == "##,#,###,####".

**list**

> symbol used to separate numbers in a list intended to represent structured data such as an array; must be different from the **decimal** value. This list separator is for “non-linguistic” usage as opposed to the listPatterns for “linguistic” lists (e.g. “Bob, Carol, and Ted”) described in Part 2, _Section 11 [List Patterns](tr35-general.md#ListPatterns)_.

**percentSign**

> symbol used to indicate a percentage (1/100th) amount. (If present, the value is also multiplied by 100 before formatting. That way 1.23 → 123%)

~~**nativeZeroDigit**~~

> Deprecated - do not use.

~~**patternDigit**~~

> Deprecated. This was formerly used to provide the localized pattern character corresponding to '#', but localization of the pattern characters themselves has been deprecated for some time (determining the locale-specific _replacements_ for pattern characters is of course not deprecated and is part of normal number formatting).

**minusSign**

> Symbol used to denote negative value.

**plusSign**

> Symbol used to denote positive value.  It can be used to produce modified patterns, so that 3.12 is formatted as "+3.12", for example. The standard number patterns (except for type="accounting") will contain the minusSign, explicitly or implicitly. In the explicit pattern, the value of the plusSign can be substituted for the value of the minusSign to produce a pattern that has an explicit plus sign.

**approximatelySign**

> Symbol used to denote a value that is approximate but not exact. The symbol is substituted in place of the minusSign using the same semantics as plusSign substitution.

**exponential**

> Symbol separating the mantissa and exponent values.

**superscriptingExponent**

> (Programmers are used to the fallback exponent style “1.23E4”, but that should not be shown to end-users. Instead, the exponential notation superscriptingExponent should be used to show a format like “1.23 × 104”. ) The superscripting can use markup, such as `<sup>4</sup>` in HTML, or for the special case of Latin digits, use the superscript characters: U+207B ( ⁻ ), U+2070 ( ⁰ ), U+00B9 ( ¹ ), U+00B2 ( ² ), U+00B3 ( ³ ), U+2074 ( ⁴ ) .. U+2079 ( ⁹ ).

**perMille**

> symbol used to indicate a per-mille (1/1000th) amount. (If present, the value is also multiplied by 1000 before formatting. That way 1.23 → 1230 [1/000])

**infinity**

> The infinity sign. Corresponds to the IEEE infinity bit pattern.

**nan - Not a number**

> The NaN sign. Corresponds to the IEEE NaN bit pattern.

**currencyDecimal**

> Optional. If specified, then for currency formatting/parsing this is used as the decimal separator instead of using the regular decimal separator; otherwise, the regular decimal separator is used.

**currencyGroup**

> Optional. If specified, then for currency formatting/parsing this is used as the group separator instead of using the regular group separator; otherwise, the regular group separator is used.

**timeSeparator**

> This replaces any use of the timeSeparator pattern character in a date-time format pattern (no timeSeparator pattern character is currently defined, see note below). This allows the same time format to be used for multiple number systems when the time separator depends on the number system. For example, the time format for Arabic should be COLON when using the Latin numbering system (0, 1, 2, …), but when the Arabic numbering system is used (٠‎ - ١‎ - ٢‎ …), the traditional time separator in older print styles was often ARABIC COMMA.
>
> **Note:** In CLDR 26 the timeSeparator pattern character was specified to be COLON. This was withdrawn in CLDR 28 due to backward compatibility issues, and no timeSeparator pattern character is currently defined. No CLDR locales are known to have a need to specify timeSeparator symbols that depend on number system; if this changes in the future a different timeSeparator pattern character will be defined. In the meantime, since CLDR data consumers can still request the timeSeparator symbol. it should match the symbol actually used in the [timeFormats](tr35-dates.md#timeFormats) and [availableFormats](tr35-dates.md#availableFormats_appendItems) items.

Example:

```xml
<symbols>
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

```xml
<!ATTLIST symbols numberSystem CDATA #IMPLIED >  
```
The `numberSystem` attribute is used to specify that the given number symbols are to be used when the given numbering system is active. Number symbols can only be defined for numbering systems of the "numeric" type, since any special symbols required for an algorithmic numbering system should be specified by the RBNF formatting rules used for that numbering system. By default, number symbols without a specific `numberSystem` attribute are assumed to be used for the "latn" numbering system, which is western (ASCII) digits. Locales that specify a numbering system other than "latn" as the default should also specify number formatting symbols that are appropriate for use within the context of the given numbering system. For example, a locale that uses the Arabic-Indic digits as its default would likely use an Arabic comma for the grouping separator rather than the ASCII comma.  
For more information on numbering systems and their definitions, see _[Section 1: Numbering Systems](#Numbering_Systems)_.

### 2.4 <a name="Number_Formats" href="#Number_Formats">Number Formats</a>

```xml
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
<decimalFormats>
  <decimalFormatLength type="long">
    <decimalFormat>
      <pattern>#,##0.###</pattern>
    </decimalFormat>
  </decimalFormatLength>
</decimalFormats>

<scientificFormats>
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

<percentFormats>
  <percentFormatLength type="long">
    <percentFormat>
      <pattern>#,##0%</pattern>
    </percentFormat>
  </percentFormatLength>
</percentFormats>
```

```xml
<!ATTLIST symbols numberSystem CDATA #IMPLIED >  
```

The `numberSystem` attribute is used to specify that the given number formatting pattern(s) are to be used when the given numbering system is active. By default, number formatting patterns without a specific `numberSystem` attribute are assumed to be used for the "latn" numbering system, which is western (ASCII) digits. Locales that specify a numbering system other than "latn" as the default should also specify number formatting patterns that are appropriate for use within the context of the given numbering system.  
For more information on numbering systems and their definitions, see _[Section 1: Numbering Systems](#Numbering_Systems)_.

#### 2.4.1 <a name="Compact_Number_Formats" href="#Compact_Number_Formats">Compact Number Formats</a>

A pattern `type` attribute is used for _compact number formats_, such as the following:

```xml
<decimalFormatLength type="long">  
    <decimalFormat>  
        <pattern type="1000" count="one">0 millier</pattern>  
        <pattern type="1000" count="other">0 milliers</pattern>  
        <pattern type="10000" count="one">00 mille</pattern>  
        <pattern type="10000" count="other">00 mille</pattern>  
        <pattern type="100000" count="one">000 mille</pattern>  
        <pattern type="100000" count="other">000 mille</pattern>  
        <pattern type="1000000" count="one">0 million</pattern>  
        <pattern type="1000000" count="other">0 millions</pattern>  
        …  
    </decimalFormat>  
</decimalFormatLength>  
<decimalFormatLength type="short">  
    <decimalFormat>  
        <pattern type="1000" count="one">0 K</pattern>  
        <pattern type="1000" count="other">0 K</pattern>  
        <pattern type="10000" count="one">00 K</pattern>  
        <pattern type="10000" count="other">00 K</pattern>  
        <pattern type="100000" count="one">000 K</pattern>  
        <pattern type="100000" count="other">000 K</pattern>  
        <pattern type="1000000" count="one">0 M</pattern>  
        <pattern type="1000000" count="other">0 M</pattern>  
        …  
    </decimalFormat>
…
<currencyFormatLength type="short">  
    <currencyFormat type="standard">  
        <pattern type="1000" count="one">0 K ¤</pattern>  
        <pattern type="1000" count="other">0 K ¤</pattern>  
        <pattern type="10000" count="one">00 K ¤</pattern>  
        <pattern type="10000" count="other">00 K ¤</pattern>  
        <pattern type="100000" count="one">000 K ¤</pattern>  
        <pattern type="100000" count="other">000 K ¤</pattern>  
        <pattern type="1000000" count="one">0 M ¤</pattern>  
        <pattern type="1000000" count="other">0 M ¤</pattern>
```

Formats can be supplied for numbers (as above) or for currencies or other units. They can also be used with ranges of numbers, resulting in formatting strings like “$10K” or “$3–7M”.

To format a number N, the greatest type less than or equal to N is used, with the appropriate plural category. N is divided by the type, after removing the number of zeros in the pattern, less 1. APIs supporting this format should provide control over the number of significant or fraction digits.

The default pattern for any type that is not supplied is the special value “0”, as in the following. The value “0” must be used when a child locale overrides a parent locale to drop the compact pattern for that type and use the default pattern.

 `<pattern type="1" count="one">0</pattern>`

If the value is precisely “0”, either explicit or defaulted, then the normal number format pattern for that sort of object is supplied — either `<decimalFormat>` or `<currencyFormat type="standard">` — with the normal formatting for the locale (such as the grouping separators). However, for the “0” case by default the signficant digits are adjusted for consistency, typically to 2 or 3 digits, and the maximum fractional digits are set to 0 (for both currencies and plain decimal). Thus the output would be $12, not $12.01. APIs may, however, allow these default behaviors to be overridden.

With the data above, N=12345 matches `<pattern type="10000" count="other">00 K</pattern>`. N is divided by 1000 (obtained from10000 after removing "00" and restoring one "0". The result is formatted according to the normal decimal pattern. With no fractional digits, that yields "12 K".

Formatting 1200 in USD would result in “1.2 K $”, while 990 implicitly maps to the special value “0”, which maps to `<currencyFormat type="standard"><pattern>#,##0.00 ¤</pattern>`, and would result in simply “990 $”.

The short format is designed for UI environments where space is at a premium, and should ideally result in a formatted string no more than about 6 em wide (with no fractional digits).

#### 2.4.2 <a name="Currency_Formats" href="#Currency_Formats">Currency Formats</a>

Pattern for use with currency formatting. This format contains a few additional structural options that allow proper placement of the currency symbol relative to the numeric quantity. Refer to _[Section 4 - Currencies](#Currencies)_ for additional information on the use of these options.

```xml
<!ELEMENT currencyFormats (alias | (default*, currencySpacing*, currencyFormatLength*, unitPattern*, special*)) >
<!ELEMENT currencySpacing (alias | (beforeCurrency*, afterCurrency*, special*)) >
<!ELEMENT beforeCurrency (alias | (currencyMatch*, surroundingMatch*, insertBetween*)) >
<!ELEMENT afterCurrency (alias | (currencyMatch*, surroundingMatch*, insertBetween*)) >
<!ELEMENT currencyMatch ( #PCDATA ) >
<!ELEMENT surroundingMatch ( #PCDATA )) >
<!ELEMENT insertBetween ( #PCDATA ) >
<!ELEMENT currencyFormatLength (alias | (default*, currencyFormat*, special*)) >
<!ATTLIST currencyFormatLength type ( full | long | medium | short ) #IMPLIED >
<!ELEMENT currencyFormat (alias | (pattern*, special*)) >
```

In addition to a standard currency format, in which negative currency amounts might typically be displayed as something like “-$3.27”, locales may provide an "accounting" form, in which for "en_US" the same example would appear as “($3.27)”.

```xml
<currencyFormats>
    <currencyFormatLength>
        <currencyFormat type="standard">
            <pattern>¤#,##0.00</pattern>
        </currencyFormat>
        <currencyFormat type="accounting">
            <pattern>¤#,##0.00;(¤#,##0.00)</pattern>
        </currencyFormat>
    </currencyFormatLength>
</currencyFormats>
```

### 2.5 <a name="Miscellaneous_Patterns" href="#Miscellaneous_Patterns">Miscellaneous Patterns</a>

```xml
<!ELEMENT miscPatterns (alias | (default*, pattern*, special*)) >  
<!ATTLIST miscPatterns numberSystem CDATA #IMPLIED >
```

The miscPatterns supply additional patterns for special purposes. The currently defined values are:

**approximately**

> indicates an approximate number, such as: “\~99”. This pattern is not currently in use; see ICU-20163.

**atMost**

> indicates a number or lower, such as: “`≤`99” to indicate that there are 99 items or fewer.

**atLeast**

> indicates a number or higher, such as: “99+” to indicate that there are 99 items or more.

**range**

> indicates a range of numbers, such as: “99–103” to indicate that there are from 99 to 103 items.

_For example:_

```xml
<miscPatterns numberSystem="…">  
  <pattern type="approximately">~{0}</pattern>  
  <pattern type="atLeast">≥{0}</pattern>  
  <pattern type="atMost">≤{0}</pattern>  
  <pattern type="range">{0}–{1}</pattern>  
</miscPatterns>
```

### 2.6 <a name="Minimal_Pairs" href="#Minimal_Pairs">Minimal Pairs</a>

```xml
<!ELEMENT minimalPairs ( alias | ( pluralMinimalPairs*, ordinalMinimalPairs*, caseMinimalPairs*, genderMinimalPairs*, special* ) ) >  
```
```xml
<!ELEMENT pluralMinimalPairs ( #PCDATA ) >  
<!ATTLIST pluralMinimalPairs count NMTOKEN #IMPLIED >  
```
```xml
<!ELEMENT ordinalMinimalPairs ( #PCDATA ) >  
<!ATTLIST ordinalMinimalPairs ordinal NMTOKEN #IMPLIED >  
```

```xml
<!ELEMENT caseMinimalPairs ( #PCDATA ) >  
<!ATTLIST caseMinimalPairs case NMTOKEN #REQUIRED >
```

```xml
<!ELEMENT genderMinimalPairs ( #PCDATA ) >  
<!ATTLIST genderMinimalPairs gender NMTOKEN #REQUIRED >
```

Minimal pairs provide examples that justify why multiple plural or ordinal categories exist, and for providing contextual examples for verifying consistency of translations. The allowable values for the `count`, `ordinal`, `case`, and `gender` attributes are found in the dtd file.

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


For more information, see [Plural Rules](http://cldr.unicode.org/index/cldr-spec/plural-rules) and [Grammatical Inflection](http://cldr.unicode.org/translation/grammatical-inflection).

## 3 <a name="Number_Format_Patterns" href="#Number_Format_Patterns">Number Format Patterns</a>

### 3.1 <a name="Number_Patterns" href="#Number_Patterns">Number Patterns</a>

Number patterns affect how numbers are interpreted in a localized context. Here are some examples, based on the French locale. The "." shows where the decimal point should go. The "," shows where the thousands separator should go. A "0" indicates zero-padding: if the number is too short, a zero (in the locale's numeric set) will go there. A "#" indicates no padding: if the number is too short, nothing goes there. A "¤" shows where the currency sign will go. The following illustrates the effects of different patterns for the French locale, with the number "1234.567". Notice how the pattern characters ',' and '.' are replaced by the characters appropriate for the locale.

##### <a name="Number_Pattern_Examples" href="#Number_Pattern_Examples">Number Pattern Examples</a>

| Pattern | Currency | Text |
| --- | --- | --- |
| #,##0.## | _n/a_ | 1 234,57 |
| #,##0.### | _n/a_ | 1 234,567 |
| ###0.##### | _n/a_ | 1234,567 |
| ###0.0000# | _n/a_ | 1234,5670 |
| 00000.0000 | _n/a_ | 01234,5670 |
| #,##0.00 ¤ | EUR | 1 234,57 € |
|| JPY | 1 235 ¥JP |

The number of # placeholder characters before the decimal do not matter, since no limit is placed on the maximum number of digits. There should, however, be at least one zero someplace in the pattern. In currency formats, the number of digits after the decimal also do not matter, since the information in the supplemental data (see _[Supplemental Currency Data](#Supplemental_Currency_Data))_ is used to override the number of decimal places — and the rounding — according to the currency that is being formatted. That can be seen in the above chart, with the difference between Yen and Euro formatting.

To ensure correct layout, especially in currency patterns in which a a variety of symbols may be used, number patterns may contain (invisible) bidirectional text format characters such as LRM, RLM, and ALM.

_When parsing using a pattern, a lenient parse should be used; see [Lenient Parsing](tr35.md#Lenient_Parsing)._ As noted there, lenient parsing should ignore bidi format characters.

### 3.2 <a name="Special_Pattern_Characters" href="#Special_Pattern_Characters">Special Pattern Characters</a>

Many characters in a pattern are taken literally; they are matched during parsing and output unchanged during formatting. Special characters, on the other hand, stand for other characters, strings, or classes of characters. For example, the '#' character is replaced by a localized digit for the chosen numberSystem. Often the replacement character is the same as the pattern character; in the U.S. locale, the ',' grouping character is replaced by ','. However, the replacement is still happening, and if the symbols are modified, the grouping character changes. Some special characters affect the behavior of the formatter by their presence; for example, if the percent character is seen, then the value is multiplied by 100 before being displayed.

To insert a special character in a pattern as a literal, that is, without any special meaning, the character must be quoted. There are some exceptions to this which are noted below. The Localized Replacement column shows the replacement from _Section 2.3 [Number Symbols](#Number_Symbols)_ or the numberSystem's digits: _italic_ indicates a special function.

Invalid sequences of special characters (such as “¤¤¤¤¤¤” in current CLDR) should be handled for formatting and parsing as described in [Handling Invalid Patterns](tr35.md#Invalid_Patterns).

##### <a name="Number_Pattern_Character_Definitions" href="#Number_Pattern_Character_Definitions">Number Pattern Character Definitions</a>

| Symbol | Location | Localized Replacement | Meaning |
| :-- | :-- | :-- | :-- |
| 0 | Number | digit | Digit |
| 1-9 | Number | digit | '1' through '9' indicate rounding. |
| @ | Number | digit | Significant digit |
| # | Number | digit, _nothing_ | Digit, omitting leading/trailing zeros |
| . | Number | decimal, currencyDecimal | Decimal separator or monetary decimal separator |
| - | Number | minusSign, plusSign, approximatelySign | Minus sign. **Warning:** the pattern '-'0.0 is not the same as the pattern -0.0. In the former case, the minus sign is a literal. In the latter case, it is a special symbol, which is replaced by the minusSymbol, and can also be replaced by the plusSymbol for a format like +12% as in Section 3.2.1 [Explicit Plus Signs](#Explicit_Plus). |
| , | Number | group, currencyGroup | Grouping separator. May occur in both the integer part and the fractional part. The position determines the grouping. |
| E | Number | exponential, superscriptingExponent | Separates mantissa and exponent in scientific notation. _Need not be quoted in prefix or suffix._ |
| + | Exponent or Number (for explicit plus) | plusSign | Prefix positive exponents with localized plus sign. Used for explicit plus for numbers as well, as described in Section 3.2.1 [Explicit Plus Signs](#Explicit_Plus). _Need not be quoted in prefix or suffix._ |
| % | Prefix or suffix | percentSign | Multiply by 100 and show as percentage |
| ‰ (U+2030) | Prefix or suffix | perMille | Multiply by 1000 and show as per mille (aka “basis points”) |
| ; | Subpattern boundary | _syntax_ | Separates positive and negative subpatterns. When there is no explicit negative subpattern, an implicit negative subpattern is formed from the positive pattern with a prefixed - (ASCII U+002D HYPHEN-MINUS). |
| ¤ (U+00A4) | Prefix or suffix | _currency symbol/name from currency specified in API_ | Any sequence is replaced by the localized currency symbol for the currency being formatted, as in the table below. If present in a pattern, the monetary decimal separator and grouping separators (if available) are used instead of the numeric ones. If data is unavailable for a given sequence in a given locale, the display may fall back to ¤ or ¤¤. See also the formatting forcurrency display names, steps 2 and 4 in [Currencies](#Currencies). <table><tr><th>No.</th><th>Replacement / Example</th></tr><tr><td rowspan="2">¤</td><td>Standard currency symbol</td></tr><tr><td>_C$12.00_</td></tr><tr><td rowspan="2">¤¤</td><td>ISO currency symbol (constant)</td></tr><tr><td>_CAD 12.00_</td></tr><tr><td rowspan="2">¤¤¤</td><td>Appropriate currency display name for the currency,based on the plural rules in effect for the locale</td></tr><tr><td>_5.00 Canadian dollars_</td></tr><tr><td rowspan="2" >¤¤¤¤¤</td><td>Narrow currency symbol. The same symbols may be used for multiple currencies. Thus the symbol may be ambiguous, and should only be where the context is clear.</td></tr><tr><td>_$12.00_</td></tr><tr><td>_others_</td><td>_Invalid in current CLDR. Reserved for future specification_</td></tr></table> |
| * | Prefix or suffix boundary | _padding character specified in API_ | Pad escape, precedes pad character |
| ' | Prefix or suffix | _syntax-only_ | Used to quote special characters in a prefix or suffix, for example, `"'#'#"` formats 123 to `"#123"`. To create a single quote itself, use two in a row: `"# o''clock"`. |

A pattern contains a positive subpattern and may contain a negative subpattern, for example, "#,##0.00;(#,##0.00)". Each subpattern has a prefix, a numeric part, and a suffix. If there is no explicit negative subpattern, the implicit negative subpattern is the ASCII minus sign (-) prefixed to the positive subpattern. That is, "0.00" alone is equivalent to "0.00;-0.00". (The data in CLDR is normalized to remove an explicit negative subpattern where it would be identical to the implicit form.)

Note that if an negative subpattern is used as-is: a minus sign is _not_ added, eg "0.00;0.00" ≠ "0.00;-0.00". Trailing semicolons are ignored, eg "0.00;" = "0.00". Whitespace is not ignored, including those around semicolons, so "0.00 ; -0.00" ≠ "0.00;-0.00".

If there is an explicit negative subpattern, it serves only to specify the negative prefix and suffix; the number of digits, minimal digits, and other characteristics are ignored in the negative subpattern. That means that "#,##0.0#;(#)" has precisely the same result as "#,##0.0#;(#,##0.0#)". However in the CLDR data, the format is normalized so that the other characteristics are preserved, just for readability.

> **Note:** The thousands separator and decimal separator in patterns are always ASCII ',' and '.'. They are substituted by the code with the correct local values according to other fields in CLDR. The same is true of the - (ASCII minus sign) and other special characters listed above.

A currency decimal pattern normally contains a currency symbol placeholder (¤, ¤¤, ¤¤¤, or ¤¤¤¤¤). The currency symbol placeholder may occur before the first digit, after the last digit symbol, or where the decimal symbol would otherwise be placed (for formats such as "12€50", as in "12€50 pour une omelette").

Placement | Examples
-------|-------
Before|"¤#,##0.00" "¤ #,##0.00" "¤-#,##0.00" "¤ -#,##0.00" "-¤#,##0.00" "-¤ #,##0.00" …
After|"#,##0.00¤" "#,##0.00 ¤" "#,##0.00-¤" "#,##0.00- ¤" "#,##0.00¤-" "#,##0.00 ¤-" …
Decimal|"#,##0¤00"

Below is a sample of patterns, special characters, and results:

##### <a name="Sample_Patterns_and_Results" href="#Sample_Patterns_and_Results">Sample Patterns and Results</a>

<table><tbody>
<tr><th>explicit pattern:</th><td colspan="2">0.00;-0.00</td><td colspan="2">0.00;0.00-</td><td colspan="2">0.00+;0.00-</td></tr>
<tr><th>decimalSign:</th><td colspan="2">,</td><td colspan="2">,</td><td colspan="2">,</td></tr>
<tr><th>minusSign:</th><td colspan="2">∸</td><td colspan="2">∸</td><td colspan="2">∸</td></tr>
<tr><th>plusSign:</th><td colspan="2">∔</td><td colspan="2">∔</td><td colspan="2">∔</td></tr>
<tr><th>number:</th><td>3.1415</td><td>-3.1415</td><td>3.1415</td><td>-3.1415</td><td>3.1415</td><td>-3.1415</td></tr>
<tr><th>formatted:</th><td>3,14</td><td>∸3,14</td><td>3,14</td><td>3,14∸</td><td>3,14∔</td><td>3,14∸</td></tr>
</tbody></table>

_In the above table, ∸ = U+2238 DOT MINUS and ∔ = U+2214 DOT PLUS are used for illustration._

The prefixes, suffixes, and various symbols used for infinity, digits, thousands separators, decimal separators, and so on may be set to arbitrary values, and they will appear properly during formatting. _However, care must be taken that the symbols and strings do not conflict, or parsing will be unreliable._ For example, either the positive and negative prefixes or the suffixes must be distinct for any parser using this data to be able to distinguish positive from negative values. Another example is that the decimal separator and thousands separator should be distinct characters, or parsing will be impossible.

The _grouping separator_ is a character that separates clusters of integer digits to make large numbers more legible. It is commonly used for thousands, but in some locales it separates ten-thousands. The _grouping size_ is the number of digits between the grouping separators, such as 3 for "100,000,000" or 4 for "1 0000 0000". There are actually two different grouping sizes: One used for the least significant integer digits, the _primary grouping size_, and one used for all others, the _secondary grouping size_. In most locales these are the same, but sometimes they are different. For example, if the primary grouping interval is 3, and the secondary is 2, then this corresponds to the pattern "#,##,##0", and the number 123456789 is formatted as "12,34,56,789". If a pattern contains multiple grouping separators, the interval between the last one and the end of the integer defines the primary grouping size, and the interval between the last two defines the secondary grouping size. All others are ignored, so "#,##,###,####" == "###,###,####" == "##,#,###,####".

The grouping separator may also occur in the fractional part, such as in “#,##0.###,#”. This is most commonly done where the grouping separator character is a thin, non-breaking space (U+202F), such as “1.618 033 988 75”. See [physics.nist.gov/cuu/Units/checklist.html](https://physics.nist.gov/cuu/Units/checklist.html).

For consistency in the CLDR data, the following conventions are observed:

* All number patterns should be minimal: there should be no leading # marks except to specify the position of the grouping separators (for example, avoid  ##,##0.###).
* All formats should have one 0 before the decimal point (for example, avoid #,###.##)
* Decimal formats should have three hash marks in the fractional position (for example, #,##0.###).
* Currency formats should have two zeros in the fractional position (for example, ¤ #,##0.00).
    * The exact number of decimals is overridden with the decimal count in supplementary data or by API settings.
* The only time two thousands separators needs to be used is when the number of digits varies, such as for Hindi: #,##,##0.
* The **minimumGroupingDigits** can be used to suppress groupings below a certain value. This is used for languages such as Polish, where one would only write the grouping separator for values above 9999. The minimumGroupingDigits contains the default for the locale.
    * The attribute value is used by adding it to the grouping separator value. If the input number has fewer integer digits, the grouping separator is suppressed.
    * ##### <a name="Examples_of_minimumGroupingDigits" href="#Examples_of_minimumGroupingDigits">Examples of minimumGroupingDigits</a>     

        | minimum­GroupingDigits | Pattern Grouping | Input Number | Formatted |
        | ---: | ---: | ---: | ---: |
        | 1 | 3 | 1000 | 1,000 |
        | 1 | 3 | 10000 | 10,000 |
        | 2 | 3 | 1000 | 1000 |
        | 2 | 3 | 10000 | 10,000 |
        | 1 | 4 | 10000 | 1,0000 |
        | 2 | 4 | 10000 | 10000 | 

#### 3.2.1 <a name="Explicit_Plus" href="#Explicit_Plus">Explicit Plus Signs</a>

An explicit "plus" format can be formed, so as to show a visible + sign when formatting a non-negative number. The displayed plus sign can be an ASCII plus or another character, such as ＋ U+FF0B FULLWIDTH PLUS SIGN or ➕ U+2795 HEAVY PLUS SIGN; it is taken from whatever is set for plusSign in _Section 2.3 [Number Symbols](#Number_Symbols)_.

1. Get the negative subpattern (explicit or implicit).
2. Replace any unquoted ASCII minus sign by an ASCII plus sign.
3. If there are any replacements, use that for the positive subpattern.

For an example, see [Sample Patterns and Results](#Sample_Patterns_and_Results).

### 3.3 <a name="Formatting" href="#Formatting">Formatting</a>

Formatting is guided by several parameters, all of which can be specified either using a pattern or using an external API designed for number formatting. The following description applies to formats that do not use [scientific notation](#sci) or [significant digits](#sigdig).

* If the number of actual integer digits exceeds the _maximum integer digits_, then only the least significant digits are shown. For example, 1997 is formatted as "97" if the maximum integer digits is set to 2.
* If the number of actual integer digits is less than the _minimum integer digits_, then leading zeros are added. For example, 1997 is formatted as "01997" if the minimum integer digits is set to 5.
* If the number of actual fraction digits exceeds the _maximum fraction digits_, then half-even rounding it performed to the maximum fraction digits. For example, 0.125 is formatted as "0.12" if the maximum fraction digits is 2. This behavior can be changed by specifying a rounding increment and a rounding mode.
* If the number of actual fraction digits is less than the _minimum fraction digits_, then trailing zeros are added. For example, 0.125 is formatted as "0.1250" if the minimum fraction digits is set to 4.
* Trailing fractional zeros are not displayed if they occur _j_ positions after the decimal, where _j_ is less than the maximum fraction digits. For example, 0.10004 is formatted as "0.1" if the maximum fraction digits is four or less.

**Special Values**

`NaN` is represented as a single character, typically `(U+FFFD)` . This character is determined by the localized number symbols. This is the only value for which the prefixes and suffixes are not used.

Infinity is represented as a single character, typically ∞ `(U+221E)` , with the positive or negative prefixes and suffixes applied. The infinity character is determined by the localized number symbols.

### 3.4 <a name="sci" href="#sci">Scientific Notation</a>

Numbers in scientific notation are expressed as the product of a mantissa and a power of ten, for example, 1234 can be expressed as 1.234 x 103. The mantissa is typically in the half-open interval [1.0, 10.0) or sometimes [0.0, 1.0), but it need not be. In a pattern, the exponent character immediately followed by one or more digit characters indicates scientific notation. Example: "0.###E0" formats the number 1234 as "1.234E3".

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

### 3.5 <a name="sigdig" href="#sigdig">Significant Digits</a>

There are two ways of controlling how many digits are shows: (a) significant digits counts, or (b) integer and fraction digit counts. Integer and fraction digit counts are described above. When a formatter is using significant digits counts, it uses however many integer and fraction digits are required to display the specified number of significant digits. It may ignore min/max integer/fraction digits, or it may use them to the extent possible.

##### <a name="Significant_Digits_Examples" href="#Significant_Digits_Examples">Significant Digits Examples</a>

| Pattern | Minimum significant digits | Maximum significant digits | Number | Output |
| :-- | :-- | :-- | :-- | :-- |
| `@@@` | 3 | 3 | 12345 | `12300` |
| `@@@` | 3 | 3 | 0.12345 | `0.123` |
| `@@##` | 2 | 4 | 3.14159 | `3.142` |
| `@@##` | 2 | 4 | 1.23004 | `1.23` |

* In order to enable significant digits formatting, use a pattern containing the `'@'` pattern character. In order to disable significant digits formatting, use a pattern that does not contain the `'@'` pattern character.
* Significant digit counts may be expressed using patterns that specify a minimum and maximum number of significant digits. These are indicated by the `'@'` and `'#'` characters. The minimum number of significant digits is the number of `'@'` characters. The maximum number of significant digits is the number of `'@'` characters plus the number of `'#'` characters following on the right. For example, the pattern `"@@@"` indicates exactly 3 significant digits. The pattern `"@##"` indicates from 1 to 3 significant digits. Trailing zero digits to the right of the decimal separator are suppressed after the minimum number of significant digits have been shown. For example, the pattern `"@##"` formats the number 0.1203 as `"0.12"`.
* Implementations may forbid the use of significant digits in combination with min/max integer/fraction digits. In such a case, if a pattern uses significant digits, it may not contain a decimal separator, nor the `'0'` pattern character. Patterns such as `"@00"` or `"@.###"` would be disallowed.
* Any number of `'#'` characters may be prepended to the left of the leftmost `'@'` character. These have no effect on the minimum and maximum significant digits counts, but may be used to position grouping separators. For example, `"#,#@#"` indicates a minimum of one significant digits, a maximum of two significant digits, and a grouping size of three.
* The number of significant digits has no effect on parsing.
* Significant digits may be used together with exponential notation. Such patterns are equivalent to a normal exponential pattern with a minimum and maximum integer digit count of one, a minimum fraction digit count of `Minimum Significant Digits - 1`, and a maximum fraction digit count of `Maximum Significant Digits - 1`. For example, the pattern `"@@###E0"` is equivalent to `"0.0###E0"`.

### 3.6 <a name="Padding" href="#Padding">Padding</a>

Patterns support padding the result to a specific width. In a pattern the pad escape character, followed by a single pad character, causes padding to be parsed and formatted. The pad escape character is '*'. For example, `"$*x#,##0.00"` formats 123 to `"$xx123.00"` , and 1234 to `"$1,234.00"` .

* When padding is in effect, the width of the positive subpattern, including prefix and suffix, determines the format width. For example, in the pattern `"* #0 o''clock"`, the format width is 10.
* Some parameters which usually do not matter have meaning when padding is used, because the pattern width is significant with padding. In the pattern "* ##,##,#,##0.##", the format width is 14. The initial characters "##,##," do not affect the grouping size or maximum integer digits, but they do affect the format width.
* Padding may be inserted at one of four locations: before the prefix, after the prefix, before the suffix, or after the suffix. No padding can be specified in any other location. If there is no prefix, before the prefix and after the prefix are equivalent, likewise for the suffix.
* When specified in a pattern, the code point immediately following the pad escape is the pad character. This may be any character, including a special pattern character. That is, the pad escape _escapes_ the following character. If there is no character after the pad escape, then the pattern is illegal.

### 3.7 <a name="Rounding" href="#Rounding">Rounding</a>

Patterns support rounding to a specific increment. For example, 1230 rounded to the nearest 50 is 1250. Mathematically, rounding to specific increments is performed by dividing by the increment, rounding to an integer, then multiplying by the increment. To take a more bizarre example, 1.234 rounded to the nearest 0.65 is 1.3, as follows:

<table><tbody>
<tr><th>Original:</th><td>1.234</td></tr>
<tr><th>Divide by increment (0.65):</th><td>1.89846…</td></tr>
<tr><th>Round:</th><td>2</td></tr>
<tr><th>Multiply by increment (0.65):</th><td>1.3</td></tr>
</tbody></table>

To specify a rounding increment in a pattern, include the increment in the pattern itself. "#,#50" specifies a rounding increment of 50. "#,##0.05" specifies a rounding increment of 0.05.

* Rounding only affects the string produced by formatting. It does not affect parsing or change any numerical values.
* An implementation may allow the specification of a _rounding mode_ to determine how values are rounded. In the absence of such choices, the default is to round "half-even", as described in IEEE arithmetic. That is, it rounds towards the "nearest neighbor" unless both neighbors are equidistant, in which case, it rounds towards the even neighbor. Behaves as for round "half-up" if the digit to the left of the discarded fraction is odd; behaves as for round "half-down" if it's even. Note that this is the rounding mode that minimizes cumulative error when applied repeatedly over a sequence of calculations.
* Some locales use rounding in their currency formats to reflect the smallest currency denomination.
* In a pattern, digits '1' through '9' specify rounding, but otherwise behave identically to digit '0'.

### 3.8 <a name="Quoting_Rules" href="#Quoting_Rules">Quoting Rules</a>

Single quotes, (**'**), enclose bits of the pattern that should be treated literally. Inside a quoted string, two single quotes ('') are replaced with a single one ('). For example: `'X '`#`' Q '` -> **X 1939 Q** (Literal strings `shaded`.)

## 4 <a name="Currencies" href="#Currencies">Currencies</a>

```xml
<!ELEMENT currencies (alias | (default?, currency*, special*)) >  
<!ELEMENT currency (alias | (((pattern+, displayName*, symbol*) | (displayName+, symbol*, pattern*) | (symbol+, pattern*))?, decimal*, group*, special*)) >  
<!ELEMENT symbol ( #PCDATA ) >  
<!ATTLIST symbol choice ( true | false ) #IMPLIED > <!-- deprecated -->
```

> **Note:** The term "pattern" appears twice in the above. The first is for consistency with all other cases of pattern + displayName; the second is for backwards compatibility.

```xml
<currencies>
    <currency type="USD">
        <displayName>Dollar</displayName>
        <symbol>$</symbol>
    </currency>
    <currency type ="JPY">
        <displayName>Yen</displayName>
        <symbol>¥</symbol>
    </currency>
    <currency type="PTE">
        <displayName>Escudo</displayName>
        <symbol>$</symbol>
    </currency>
</currencies>
```

In formatting currencies, the currency number format is used with the appropriate symbol from `<currencies>`, according to the currency code. The `<currencies>` list can contain codes that are no longer in current use, such as PTE. The `choice` attribute has been deprecated.

The `count` attribute distinguishes the different plural forms, such as in the following:

```xml
<currencyFormats>
    <unitPattern count="other">{0} {1}</unitPattern>
    …
<currencies>
```

```xml
<currency type="ZWD">
    <displayName>Zimbabwe Dollar</displayName>
    <displayName count="one">Zimbabwe dollar</displayName>
    <displayName count="other">Zimbabwe dollars</displayName>
    <symbol>Z$</symbol>
</currency>
```

Note on displayNames:
* In general the region portion of the displayName should match the territory name, see **Part 2** _Section 1.2 [Locale Display Name Fields](tr35-general.md#locale_display_name_fields)_.
* As a result, the English currency displayName in CLDR may not match the name in ISO 4217.

To format a particular currency value "ZWD" for a particular numeric value _n_ using the (long) display name:

1. If the numeric value is exactly 0 or 1, first see if there is a count with a matching explicit number (0 or 1). If so, use that string (see [Explicit 0 and 1 rules](#Explicit_0_1_rules)).
2. Otherwise, determine the `count` value that corresponds to _n_ using the rules in _[Section 5 - Language Plural Rules](#Language_Plural_Rules)_
3. Next, get the currency unitPattern.
   1. Look for a `unitPattern` element that matches the `count` value, starting in the current locale and then following the locale fallback chain up to, but not including root.
   2. If no matching `unitPattern` element was found in the previous step, then look for a `unitPattern` element that matches `count="other"`, starting in the current locale and then following the locale fallback chain up to root (which has a `unitPattern` element with `count="other"` for every unit type).
   3. The resulting unitPattern element indicates the appropriate positioning of the numeric value and the currency display name.
4. Next, get the `displayName` element for the currency.
   1. Look for a `displayName` element that matches the `count` value, starting in the current locale and then following the locale fallback chain up to, but not including root.
   2. If no matching `displayName` element was found in the previous step, then look for a `displayName` element that matches `count="other"`, starting in the current locale and then following the locale fallback chain up to, but not including root.
   3. If no matching `displayName` element was found in the previous step, then look for a `displayName` element that with no count, starting in the current locale and then following the locale fallback chain up to root.
   4. If there is no `displayName` element, use the currency code itself (for example, "ZWD").
5. Format the numeric value according to the locale. Use the locale’s `<decimalFormats …>` pattern, not the `<currencyFormats>` pattern that is used with the symbol (eg, Z$). As when formatting symbol currency values, reset the number of decimals according to the supplemental `<currencyData>` and use the currencyDecimal symbol if different from the decimal symbol.
   1. The number of decimals should be overridable in an API, so that clients can choose between “2 US dollars” and “2.00 US dollars”.
6. Substitute the formatted numeric value for the {0} in the `unitPattern`, and the currency display name for the {1}.

While for English this may seem overly complex, for some other languages different plural forms are used for different unit types; the plural forms for certain unit types may not use all of the plural-form tags defined for the language.

For example, if the the currency is ZWD and the number is 1234, then the latter maps to `count="other"` for English. The unit pattern for that is "{0} {1}", and the display name is "Zimbabwe dollars". The final formatted number is then "1,234 Zimbabwe dollars".

When the currency symbol is substituted into a pattern, there may be some further modifications, according to the following.

```xml
<currencySpacing>
  <beforeCurrency>
    <currencyMatch>[:^S:]</currencyMatch>
    <surroundingMatch>[:digit:]</surroundingMatch>
    <insertBetween> </insertBetween>
  </beforeCurrency>
  <afterCurrency>
    <currencyMatch>[:^S:]</currencyMatch>
    <surroundingMatch>[:digit:]</surroundingMatch>
    <insertBetween> </insertBetween>
  </afterCurrency>
</currencySpacing>
```

This element controls whether additional characters are inserted on the boundary between the symbol and the pattern. For example, with the above `currencySpacing`, inserting the symbol "US$" into the pattern "#,##0.00¤" would result in an extra _no-break space_ inserted before the symbol, for example, "#,##0.00 US$". The `beforeCurrency` element governs this case, since we are looking _before_ the "¤" symbol. The `currencyMatch` is positive, since the "U" in "US$" is at the start of the currency symbol being substituted. The `surroundingMatch` is positive, since the character just before the "¤" will be a digit. Because these two conditions are true, the insertion is made.

Conversely, look at the pattern "¤#,##0.00" with the symbol "US$". In this case, there is no insertion; the result is simply "US$#,##0.00". The `afterCurrency` element governs this case, since we are looking _after_ the "¤" symbol. The `surroundingMatch` is positive, since the character just after the "¤" will be a digit. However, the `currencyMatch` is **not** positive, since the "\$" in "US\$" is at the end of the currency symbol being substituted. So the insertion is not made.

For more information on the matching used in the `currencyMatch` and `surroundingMatch` elements, see the main document _[Appendix E: Unicode Sets](tr35.md#Unicode_Sets)_.

Currencies can also contain optional grouping, decimal data, and pattern elements. This data is inherited from the `<symbols>` in the same locale data (if not present in the chain up to root), so only the _differing_ data will be present. See the main document _Section 4.1 [Multiple Inheritance](tr35.md#Multiple_Inheritance)_.

> **Note:** _Currency values should **never** be interchanged without a known currency code. You never want the number 3.5 interpreted as $3.50 by one user and €3.50 by another._ Locale data contains localization information for currencies, not a currency value for a country. A currency amount logically consists of a numeric value, plus an accompanying currency code (or equivalent). The currency code may be implicit in a protocol, such as where USD is implicit. But if the raw numeric value is transmitted without any context, then it has no definitive interpretation.

Notice that the currency code is completely independent of the end-user's language or locale. For example, BGN is the code for Bulgarian Lev. A currency amount of <BGN, 1.23456×10³> would be localized for a Bulgarian user into "1 234,56 лв." (using Cyrillic letters). For an English user it would be localized into the string "BGN 1,234.56" The end-user's language is needed for doing this last localization step; but that language is completely orthogonal to the currency code needed in the data. After all, the same English user could be working with dozens of currencies. Notice also that the currency code is also independent of whether currency values are inter-converted, which requires more interesting financial processing: the rate of conversion may depend on a variety of factors.

Thus logically speaking, once a currency amount is entered into a system, it should be logically accompanied by a currency code in all processing. This currency code is independent of whatever the user's original locale was. Only in badly-designed software is the currency code (or equivalent) not present, so that the software has to "guess" at the currency code based on the user's locale.

> **Note:** The number of decimal places **and** the rounding for each currency is not locale-specific data, and is not contained in the Locale Data Markup Language format. Those values override whatever is given in the currency numberFormat. For more information, see _[Supplemental Currency Data](#Supplemental_Currency_Data)_.

For background information on currency names, see [[CurrencyInfo](tr35.md#CurrencyInfo)].

### 4.1 <a name="Supplemental_Currency_Data" href="#Supplemental_Currency_Data">Supplemental Currency Data</a>

```xml
<!ELEMENT currencyData ( fractions*, region+ ) >  
<!ELEMENT fractions ( info+ ) >  
  
<!ELEMENT info EMPTY >  
<!ATTLIST info iso4217 NMTOKEN #REQUIRED >  
<!ATTLIST info digits NMTOKEN #IMPLIED >  
<!ATTLIST info rounding NMTOKEN #IMPLIED >  
<!ATTLIST info cashDigits NMTOKEN #IMPLIED >  
<!ATTLIST info cashRounding NMTOKEN #IMPLIED >  
  
<!ELEMENT region ( currency* ) >  
<!ATTLIST region iso3166 NMTOKEN #REQUIRED >  
  
<!ELEMENT currency ( alternate* ) >  
<!ATTLIST currency from NMTOKEN #IMPLIED >  
<!ATTLIST currency to NMTOKEN #IMPLIED >  
<!ATTLIST currency iso4217 NMTOKEN #REQUIRED >  
<!ATTLIST currency tender ( true | false ) #IMPLIED >
```

Each `currencyData` element contains one `fractions` element followed by one or more `region` elements. Here is an example for illustration.

```xml
<supplementalData>
    <currencyData>
        <fractions>
        …
        <info iso4217="CHF" digits="2" rounding="5"/>
        …
        <info iso4217="ITL" digits="0"/>
        …
        </fractions>
        …
        <region iso3166="IT">
            <currency iso4217="EUR" from="1999-01-01"/>
            <currency iso4217="ITL" from="1862-8-24" to="2002-02-28"/>
        </region>
        …
        <region iso3166="CS">
            <currency iso4217="EUR" from="2003-02-04"/>
            <currency iso4217="CSD" from="2002-05-15"/>
            <currency iso4217="YUM" from="1994-01-24" to="2002-05-15"/>
        </region>
        …
    </currencyData>
    …
</supplementalData>
```

The `fractions` element contains any number of `info` elements, with the following attributes:

* **iso4217:** the ISO 4217 code for the currency in question. If a particular currency does not occur in the fractions list, then it is given the defaults listed for the next two attributes.
* **digits:** the minimum and maximum number of decimal digits normally formatted. The default is 2. For example, in the en_US locale with the default value of 2 digits, the value 1 USD would format as "$1.00", and the value 1.123 USD would format as → "$1.12".
* **rounding:** the rounding increment, in units of 10<sup>-digits</sup>. The default is 0, which means no rounding is to be done. Therefore, rounding=0 and rounding=1 have identical behavior. Thus with fraction digits of 2 and rounding increment of 5, numeric values are rounded to the nearest 0.05 units in formatting. With fraction digits of 0 and rounding increment of 50, numeric values are rounded to the nearest 50.
* **cashDigits:** the number of decimal digits to be used when formatting quantities used in cash transactions (as opposed to a quantity that would appear in a more formal setting, such as on a bank statement). If absent, the value of "digits" should be used as a default.
* **cashRounding:** the cash rounding increment, in units of 10-cashDigits. The default is 0, which means no rounding is to be done; and as with rounding, this has the same effect as cashRounding="1". This is the rounding increment to be used when formatting quantities used in cash transactions (as opposed to a quantity that would appear in a more formal setting, such as on a bank statement). If absent, the value of "rounding" should be used as a default.

For example, the following line

```xml
<info iso4217="CZK" digits="2" rounding="0"/>
```

should cause the value 2.006 to be displayed as “2.01”, not “2.00”.

Each `region` element contains one attribute:

* **iso3166:** the ISO 3166 code for the region in question. The special value _XXX_ can be used to indicate that the region has no valid currency or that the circumstances are unknown (usually used in conjunction with _before_, as described below).

And can have any number of `currency` elements, with the `ordered` subelements.

```xml
<region iso3166="IT"> <!-- Italy -->
    <currency iso4217="EUR" from="2002-01-01"/>
    <currency iso4217="ITL" to="2001-12-31"/>
</region>
```

* **iso4217:** the ISO 4217 code for the currency in question. Note that some additional codes that were in widespread usage are included, others such as GHP are not included because they were never used.
* **from:** the currency was valid from to the datetime indicated by the value. See the main document _Section 5.2.1 [Dates and Date Ranges](tr35.md#Date_Ranges)_.
* **to:** the currency was valid up to the datetime indicated by the value of _before_. See the main document _Section 5.2.1 [Dates and Date Ranges](tr35.md#Date_Ranges)_.
* **tender:** indicates whether or not the ISO currency code represents a currency that was or is legal tender in some country. The default is "true". Certain ISO codes represent things like financial instruments or precious metals, and do not represent normally interchanged currencies.
    

That is, each `currency` element will list an interval in which it was valid. The _ordering_ of the elements in the list tells us which was the primary currency during any period in time. Here is an example of such an overlap:

```xml
<currency iso4217="CSD" to="2002-05-15"/>
<currency iso4217="YUD" from="1994-01-24" to="2002-05-15"/>
<currency iso4217="YUN" from="1994-01-01" to="1994-07-22"/>
```

The `from` element is limited by the fact that ISO 4217 does not go very far back in time, so there may be no ISO code for the previous currency.

Currencies change relatively frequently. There are different types of changes:

1. YU=>CS (name change)
2. CS=>RS+ME (split, different names)
3. SD=>SD+SS (split, same name for one // South Sudan splits from Sudan)
4. DE+DD=>DE (Union, reuses one name // East Germany unifies with Germany)

The [UN Information](https://unstats.un.org/unsd/methodology/m49/) is used to determine dates due to country changes.

When a code is no longer in use, it is terminated (see #1, #2, #4, #5)

> Example:
> 
> * ```<currency iso4217="EUR" from="2003-02-04" to="2006-06-03"/>```

When codes split, each of the new codes inherits (see #2, #3) the previous data. However, some modifications can be made if it is clear that currencies were only in use in one of the parts.

When codes merge, the data is copied from the most populous part.

> Example. When CS split into RS and ME:
> 
> * RS & ME copy the former CS, except that the line for EUR is dropped from RS
> * CS now terminates on Jun 3, 2006 (following the UN info)

## 5 <a name="Language_Plural_Rules" href="#Language_Plural_Rules">Language Plural Rules</a>

```xml
<!ELEMENT plurals (pluralRules*, pluralRanges*) >  
<!ATTLIST plurals type ( ordinal | cardinal ) #IMPLIED > <!-- default is cardinal -->  
  
<!ELEMENT pluralRules (pluralRule*) >  
<!ATTLIST pluralRules locales NMTOKENS #REQUIRED >  
  
<!ELEMENT pluralRule ( #PCDATA ) >  
<!ATTLIST pluralRule count (zero | one | two | few | many | other) #REQUIRED >
```

The plural categories are used to format messages with numeric placeholders, expressed as decimal numbers. The fundamental rule for determining plural categories is the existence of minimal pairs: whenever two different numbers may require different versions of the same message, then the numbers have different plural categories.

This happens even if nouns are invariant; even if all English nouns were invariant (like “sheep”), English would still require 2 plural categories because of subject-verb agreement, and pronoun agreement. For example:

1. 1 sheep **is** here. Do you want to buy **it**?
2. 2 sheep **are** here. Do you want to buy **them**?

For more information, see [Determining-Plural-Categories](http://cldr.unicode.org/index/cldr-spec/plural-rules#h.44ozdx564iez).

English does not have a separate plural category for “zero”, because it does not require a different message for “0”. For example, the same message can be used below, with just the numeric placeholder changing.

1. You have 3 friends online.
2. You have 0 friends online.

However, across many languages it is commonly more natural to express "0" messages with a negative (“None of your friends are online.”) and "1" messages also with an alternate form “You have a friend online.”. Thus pluralized message APIs should also offer the ability to specify at least the 0 and 1 cases explicitly; developers can use that ability whenever these values might occur in a placeholder.

The CLDR plural rules are not expected to cover all cases. For example, strictly speaking, there could be more plural and ordinal forms for English. Formally, we have a different plural form where a change in digits forces a change in the rest of the sentence. There is an edge case in English because of the behavior of "a/an".

For example, in changing from 3 to 8:

* "a 3rd of a loaf" should result in "an 8th of a loaf", not "a 8th of a loaf"
* "a 3 foot stick" should result in "an 8 foot stick", not "a 8 foot stick"

So numbers of the following forms could have a special plural category and special ordinal category: 8(X), 11(X), 18(X), 8x(X), where x is 0..9 and the optional X is 00, 000, 00000, and so on.

On the other hand, the above constructions are relatively rare in messages constructed using numeric placeholders, so the disruption for implementations currently using CLDR plural categories wouldn't be worth the small gain.

This section defines the types of plural forms that exist in a language—namely, the cardinal and ordinal plural forms. Cardinal plural forms express units such as time, currency or distance, used in conjunction with a number expressed in decimal digits (i.e. "2", not "two", and not an indefinite number such as "some" or "many"). Ordinal plural forms denote the order of items in a set and are always integers. For example, English has two forms for cardinals:

* form "one": 1 day
* form "other": 0 days, 2 days, 10 days, 0.3 days

and four forms for ordinals:

* form "one": 1st floor, 21st floor, 101st floor
* form "two": 2nd floor, 22nd floor, 102nd floor
* form "few": 3rd floor, 23rd floor, 103rd floor
* form "other": 4th floor, 11th floor, 96th floor

Other languages may have additional forms or only one form for each type of plural. CLDR provides the following tags for designating the various plural forms of a language; for a given language, only the tags necessary for that language are defined, along with the specific numeric ranges covered by each tag (for example, the plural form "few" may be used for the numeric range 2–4 in one language and 3–9 in another):

* zero (see also plural case “0”, described in [Explicit 0 and 1 rules](#Explicit_0_1_rules))
* one (see also plural case “1”, described in [Explicit 0 and 1 rules](#Explicit_0_1_rules))
* two
* few
* many

In addition, an "other" tag is always implicitly defined to cover the forms not explicitly designated by the tags defined for a language. This "other" tag is also used for languages that only have a single form (in which case no plural-form tags are explicitly defined for the language). For a more complex example, consider the cardinal rules for Russian and certain other languages:

```xml
<pluralRules locales="hr ru sr uk">
    <pluralRules count="one">n mod 10 is 1 and n mod 100 is not 11</pluralRule>
    <pluralRules count="few">n mod 10 in 2..4 and n mod 100 not in 12..14</pluralRule>
</pluralRules>
```

These rules specify that Russian has a "one" form (for 1, 21, 31, 41, 51, …), a "few" form (for 2–4, 22–24, 32–34, …), and implicitly an "other" form (for everything else: 0, 5–20, 25–30, 35–40, …, decimals). Russian does not need additional separate forms for zero, two, or many, so these are not defined.

A source number represents the visual appearance of the digits of the result. In text, it can be represented by the EBNF for sampleValue. Note that the same double number can be represented by multiple source numbers. For example, "1.0" and "1.00" are different source numbers, but there is only one double number that they correspond to: 1.0d == 1.00d. As another example, 1e3d == 1000d, but the source numbers "1e3" and "1000" are different, and can have different plural categories. So the input to the plural rules carries more information than a computer double. The plural category for negative numbers is calculated according to the absolute value of the source number, and leading integer digits don't have any effect on the plural category calculation. (This may change in the future, if we find languages that have different behavior.)

Plural categories may also differ according to the visible decimals. For example, here are some of the behaviors exhibited by different languages:

| Behavior | Description | Example |
| --- | --- | --- |
| Base | The fractions are ignored; the category is the same as the category of the integer. | 1.13 has the same plural category as 1. |
| Separate | All fractions by value are in one category (typically ‘other’ = ‘plural’). | 1.01 gets the same class as 9; <br/> 1.00 gets the same category as 1. |
| Visible | All visible fractions are in one category (typically ‘other’ = ‘plural). | 1.00, 1.01, 3.5 all get the same category. |
| Digits | The visible fraction determines the category. | 1.13 gets the same class as 13. |

There are also variants of the above: for example, short fractions may have the Digits behavior, but longer fractions may just look at the final digit of the fraction.

#### <a name="Explicit_0_1_rules" href="#Explicit_0_1_rules">Explicit 0 and 1 rules</a>

Some types of CLDR data (such as [unitPatterns](tr35-general.md#Unit_Elements) and [currency displayNames](#Currencies)) allow specification of plural rules for explicit cases “0” and “1”, in addition to the language-specific plural cases specified above: “zero”, “one”, “two” ... “other”. For the language-specific plural rules:

* The rules depend on language; for a given language, only a subset of the cases may be defined. For example, English only defines “one” and “other”, cases like “two” and “few” cannot be used in plurals for English CLDR items.
* Each plural case may cover multiple numeric values, and may depend on the formatting of those values. For example, in French the “one” case covers 0.0 through 1.99.
* The “one” case, if defined, includes at least some formatted forms of the numeric value 1; the “zero” case, if defined, includes at least some formatted forms of the numeric value 0.

By contrast, for the explicit cases “0” and “1”:

* The explicit “0” and “1” cases are not defined by language-specific rules, and are available in any language for the CLDR data items that accept them.
* The explicit “0” and “1” cases apply to the exact numeric values 0 and 1 respectively. These cases are typically used for plurals of items that do not have fractional value, like books or files.
* The explicit “0” and “1” cases have precedence over the “zero” and “one” cases. For example, if for a particular element CLDR data includes values for both the “1” and “one” cases, then the “1” value is used for numeric values of exactly 1, while the “one” value is used for any other formatted numeric values matching the “one” plural rule for the language.

Usage example: In English (which only defines language-specific rules for “one” and “other”) this can be used to have special behavior for 0:

* count=“0”: no books
* count=“one”: {0} book, e.g. “1 book”
* count=“other”: {0} books, e.g. “3 books”

### 5.1 <a name="Plural_rules_syntax" href="#Plural_rules_syntax">Plural rules syntax</a>

The xml value for each pluralRule is a _condition_ with a boolean result.
That value specifies whether that rule (i.e. that plural form) applies to a given _source number N_ in sampleValue syntax, where _N_ can be expressed as a decimal fraction or with compact decimal formatting.
The compact decimal formatting is denoted by a special notation in the syntax, e.g., “1.2c6” for “1.2M”. 
Clients of CLDR may express all the rules for a locale using the following syntax:

```
rules         = rule (';' rule)*
rule          = keyword ':' condition samples
              | 'other' ':' samples
keyword       = [a-z]+
keyword       = [a-z]+
```

In CLDR, the keyword is the attribute value of 'count'. Those values in CLDR are currently limited to just what is in the DTD, but clients may support other values.

The conditions themselves have the following syntax.

```
condition       = and_condition ('or' and_condition)*
samples         = ('@integer' sampleList)?
                  ('@decimal' sampleList)?                
and_condition   = relation ('and' relation)*
relation        = is_relation | in_relation | within_relation 
is_relation     = expr 'is' ('not')? value
in_relation     = expr (('not')? 'in' | '=' | '!=') range_list
within_relation = expr ('not')? 'within' range_list
expr            = operand (('mod' | '%') value)?
operand         = 'n' | 'i' | 'f' | 't' | 'v' | 'w' | 'c' | 'e'
range_list      = (range | value) (',' range_list)*
range           = value'..'value
value           = digit+
sampleList      = sampleRange (',' sampleRange)* (',' ('…'|'...'))?
sampleRange     = sampleValue ('~' sampleValue)?
sampleValue     = value ('.' digit+)? ([ce] digitPos digit+)?
digit           = [0-9]
digitPos        = [1-9]
```                

* Whitespace (defined as Unicode [Pattern_White_Space](https://util.unicode.org/UnicodeJsps/list-unicodeset.jsp?a=%5Cp%7BPattern_White_Space%7D)) can occur between or around any of the above tokens, with the exception of the tokens in value, digit, and sampleValue.
* In the syntax, **and** binds more tightly than **or**. So **X or Y and Z** is interpreted as **(X or (Y and Z))**.
  * For example, e = 0 and i != 0 and i % 1000000 = 0 and *+v = 0+* or e != 0..5 is parsed as if it were (e = 0 and i != 0 and i % 1000000 = 0 and v = 0) or (e != 0..5)
* Each plural rule must be written to be self-contained, and not depend on the ordering. Thus rules must be mutually exclusive; for a given numeric value, only one rule can apply (i.e., the condition can only be true for one of the pluralRule elements. Each keyword can have at most one condition. The 'other' keyword must have an empty condition: it is only present for samples.
* The samples should be included, since they are used by client software for samples and determining whether the keyword has finite values or not.
* The 'other' keyword must have no condition, and all other keywords must have a condition.

#### 5.1.1 <a name="Operands" href="#Operands">Operands</a>

The operands are numeric values corresponding to features of the *source number N*, and have the following meanings given in the table below. 
Note that, contrary to source numbers, operands are treated numerically.
Although some of them are used to describe insignificant 0s in the source number, any insignificant 0s in the operands themselves are ignored, e.g., f=03 is equivalent to f=3.

##### <a name="Plural_Operand_Meanings" href="#Plural_Operand_Meanings">Plural Operand Meanings</a>

| Symbol | Value |
| --- | --- |
| n | the absolute value of N.* |
| i | the integer digits of N.* |
| v | the number of visible fraction digits in N, _with_ trailing zeros.* |
| w | the number of visible fraction digits in N, _without_ trailing zeros.* |
| f | the visible fraction digits in N, _with_ trailing zeros, expressed as an integer.* |
| t | the visible fraction digits in N, _without_ trailing zeros, expressed as an integer.* |
| c | compact decimal exponent value: exponent of the power of 10 used in compact decimal formatting. |
| e | a deprecated synonym for ‘c’. Note: it may be redefined in the future. |

\* If there is a compact decimal exponent value (‘c’), then the n, i, f, t, v, and w values are computed _after_ shifting the decimal point in the original by the ‘c’ value. 
So for 1.2c3, the n, i, f, t, v, and w values are the same as those of 1200:  i=1200 and f=0. 
Similarly, 1.2005c3 has i=1200 and f=5 (corresponding to 1200.5).

##### <a name="Plural_Operand_Examples" href="#Plural_Operand_Examples">Plural Operand Examples</a>

| source | n | i | v | w | f | t | e |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 1 | 1 | 0 | 0 | 0 | 0 | 0 |
| 1.0 | 1 | 1 | 1 | 0 | 0 | 0 | 0 |
| 1.00 | 1 | 1 | 2 | 0 | 0 | 0 | 0 |
| 1.3 | 1.3 | 1 | 1 | 1 | 3 | 3 | 0 |
| 1.30 | 1.3 | 1 | 2 | 1 | 30 | 3 | 0 |
| 1.03 | 1.03 | 1 | 2 | 2 | 3 | 3 | 0 |
| 1.230 | 1.23 | 1 | 3 | 2 | 230 | 23 | 0 |
| 1200000 | 1200000 | 1200000 | 0 | 0 | 0 | 0 | 0 |
| 1.2c6 | 1200000 | 1200000 | 0 | 0 | 0 | 0 | 6 |
| 123c6 | 123000000 | 123000000 | 0 | 0 | 0 | 0 | 6 |
| 123c5 | 12300000 | 12300000 | 0 | 0 | 0 | 0 | 5 |
| 1200.50 | 1200.5 | 1200 | 2 | 1 | 50 | 5 | 0 |
| 1.20050c3 | 1200.5 | 1200 | 2 | 1 | 50 | 5 | 3 |


#### 5.1.2 <a name="Relations" href="#Relations">Relations</a>

The positive relations are of the format **x = y** and **x = y mod z**. The **y** value can be a comma-separated list, such as **n = 3, 5, 7..15**, and is treated as if each relation were expanded into an OR statement. The range value **a..b** is equivalent to listing all the _**integers**_ between **a** and **b**, inclusive. When **!=** is used, it means the entire relation is negated.

##### <a name="Relations_Examples" href="#Relations_Examples">Relations Examples</a>

| Expression | Meaning |
| --- | --- |
| x = 2..4, 15 | x = 2 OR x = 3 OR x = 4 OR x = 15 |
| x != 2..4, 15 | NOT (x = 2 OR x = 3 OR x = 4 OR x = 15) |

| Expression | Value |
| --- | --- |
| 3.5 = 2..4, 15 | false |
| 3.5 != 2..4, 15 | true |
| 3 = 2..4, 15 | true |
| 3 != 2..4, 15 | false |

> The old keywords 'mod', 'in', 'is', and 'within' are present only for backwards compatibility. The preferred form is to use '%' for modulo, and '=' or '!=' for the relations, with the operand 'i' instead of within. (The difference between **in** and **within** is that **in** only includes integers in the specified range, while **within** includes all values.)

The modulus (% or **mod**) is a remainder operation as defined in Java; for example, where **n** = 4.3 the result of **n mod 3** is 1.3.

The values of relations are defined according to the operand as follows. Importantly, the results may depend on the visible decimals in the source, including trailing zeros, and the compact decimal exponent.

1. Let the base value BV be computed from absolute value of the original source number according to the operand.
2. Let R be false when the comparison contains ‘not’.
3. Let R be !R if the comparison contains ‘within’ and the source number is not an integer.
4. If there is a module value MV, let BV be BV - floor(BV/MV).
5. Let CR be the list of comparison ranges, normalized that overlapping ranges are merged. Single values in the rule are represented by a range with identical \<starti, endi> values.
6. Iterate through CR:
   * if starti ≤ BV ≤ endi then return R.
7. Otherwise return !R.

##### <a name="Plural_Rules_Examples" href="#Plural_Rules_Examples">Plural Rules Examples</a>

| Rules | Comments |
| --- | --- |
| one: n = 1 <br/> few: n = 2..4 | This defines two rules, for 'one' and 'few'. The condition for 'one' is "n = 1" which means that the number must be equal to 1 for this condition to pass. The condition for 'few' is "n = 2..4" which means that the number must be between 2 and 4 inclusive for this condition to pass. All other numbers are assigned the keyword 'other' by the default rule. |
| zero: n = 0 or n != 1 and n mod 100 = 1..19 <br/> one: n = 1 | Each rule must not overlap with other rules. Also note that a modulus is applied to n in the last rule, thus its condition holds for 119, 219, 319… |
| one: n = 1 <br/> few: n mod 10 = 2..4 and n mod 100 != 12..14 | This illustrates conjunction and negation. The condition for 'few' has two parts, both of which must be met: "n mod 10 = 2..4" and "n mod 100 != 12..14". The first part applies a modulus to n before the test as in the previous example. The second part applies a different modulus and also uses negation, thus it matches all numbers _not_ in 12, 13, 14, 112, 113, 114, 212, 213, 214… |

#### 5.1.3 <a name="Samples" href="#Samples">Samples</a>

Samples are provided if sample indicator (@integer or @decimal) is present on any rule. (CLDR always provides samples.)

Where samples are provided, the absence of one of the sample indicators indicates that no numeric values can satisify that rule. For example, the rule "i = 1 and v = 0" can only have integer samples, so @decimal must not occur. The @integer samples have no visible fraction digits, while @decimal samples have visible fraction digits; both can have compact decimal exponent values (if the 'e' operand occurs).

The sampleRanges have a special notation: **start**~**end**. The **start** and **end** values must have the same number of decimal digits, and the same compact decimal exponent values (or neither have compact decimal exponent values). The range encompasses all and only values those value **v** where **start ≤ v ≤ end**, and where **v** has the same number of decimal places as **start** and **end**, and the same compact decimal exponent values.

Samples must indicate whether they are infinite or not. The '…' marker must be present if and only infinitely many values (integer or decimal) can satisfy the rule. If a set is not infinite, it must list all the possible values.

##### <a name="Plural_Samples_Examples" href="#Plural_Samples_Examples">Plural Samples Examples</a>

| Rules | Comments |
| --- | --- |
| @integer 1, 3~5 | 1, 3, 4, 5. |
| @integer 3\~5, 103\~105, … | Infinite set: 3, 4, 5, 103, 104, 105, … |
| @decimal 1.3\~1.5, 1.03\~1.05, … | Infinite set: 1.3, 1.4, 1.5, 1.03, 1.04, 1.05, … |

In determining whether a set of samples is infinite, leading zero integer digits and trailing zero decimals are not significant. Thus "i = 1000 and f = 0" is satisfied by 01000, 1000, 1000.0, 1000.00, 1000.000, 01c3 etc. but is still considered finite.

#### 5.1.4 <a name="Using_cardinals" href="#Using_cardinals">Using Cardinals</a>

Elements such as `<currencyFormats>`, `<currency>` and `<unit>` provide selection among subelements designating various localized cardinal plural forms by tagging each of the relevant subelements with a different count value, or with no count value in some cases. Note that the plural forms for a specific currencyFormat, unit type, or currency type may not use all of the different plural-form tags defined for the language. To format a currency or unit type for a particular numeric value, determine the count value according to the plural rules for the language, then select the appropriate display form for the currency format, currency type or unit type using the rules in those sections:

* 2.3 [Number Symbols](#Number_Symbols) (for `currencyFormat`s elements)
* Section 4 [Currencies](#Currencies) (for `currency` elements)
* The main document section 5.11 [Unit Elements](tr35.md#Unit_Elements)

### 5.2 <a name="Plural_Ranges" href="#Plural_Ranges">Plural Ranges</a>

```xml
<!ELEMENT pluralRanges (pluralRange*) >  
<!ATTLIST pluralRanges locales NMTOKENS #REQUIRED >  
  
<!ELEMENT pluralRange ( #PCDATA ) >  
<!ATTLIST pluralRange start (zero|one|two|few|many|other) #IMPLIED >  
<!ATTLIST pluralRange end (zero|one|two|few|many|other) #IMPLIED >  
<!ATTLIST pluralRange result (zero|one|two|few|many|other) #REQUIRED >
```

Often ranges of numbers are presented to users, such as in “Length: 3.2–4.5 centimeters”. This means any length from 3.2 cm to 4.5 cm, inclusive. However, different languages have different conventions for the pluralization given to a range: should it be “0–1 centimeter” or “0–1 centimeters”? This becomes much more complicated for languages that have many different plural forms, such as Russian or Arabic.

The `pluralRanges` element provides information allowing an implementation to derive the plural category of a range from the plural categories of the `start` and `end` values. If there is no value for a _<`start`,`end`>_ pair, the default result is `end`. However, where that result has been verified for a given language, it is included in the CLDR data.

The data has been gathered presuming that in any usage, the start value is strictly less than the end value, and that no values are negative. Results for any cases that do not meet these criteria are undefined.

For the formatting of number ranges, see <a name="Number_Range_Formatting" href="#Number_Range_Formatting">Number Range Formatting</a>.

## 6 <a name="Rule-Based_Number_Formatting" href="#Rule-Based_Number_Formatting">Rule-Based Number Formatting</a>

```xml
<!ELEMENT rbnf ( alias | rulesetGrouping*) >  
  
<!ELEMENT rulesetGrouping ( alias | ruleset*) >  
<!ATTLIST rulesetGrouping type NMTOKEN #REQUIRED>  
  
<!ELEMENT ruleset ( alias | rbnfrule*) >  
<!ATTLIST ruleset type NMTOKEN #REQUIRED>  
<!ATTLIST ruleset access ( public | private ) #IMPLIED >  
  
<!ELEMENT rbnfrule ( #PCDATA ) >  
<!ATTLIST rbnfrule value CDATA #REQUIRED >  
<!ATTLIST rbnfrule radix CDATA #IMPLIED >  
<!ATTLIST rbnfrule decexp CDATA #IMPLIED >
```

The rule-based number format (RBNF) encapsulates a set of rules for mapping binary numbers to and from a readable representation. They are typically used for spelling out numbers, but can also be used for other number systems like roman numerals, Chinese numerals, or for ordinal numbers (1st, 2nd, 3rd,…).

Where, however, the CLDR plurals or ordinals can be used, their usage is recommended in preference to the RBNF data. First, the RBNF data is not completely fleshed out over all languages that otherwise have modern coverage. Secondly, the alternate forms are neither complete, nor useful without additional information. For example, for German there is spellout-cardinal-masculine, and spellout-cardinal-feminine. But a complete solution would have all genders (masculine/feminine/neuter), all cases (nominative, accusative, dative, genitive), plus context (with strong or weak determiner or none). Moreover, even for the alternate forms that do exist, CLDR does not supply any data for when to use one vs another (eg, when to use spellout-cardinal-masculine vs spellout-cardinal-feminine). So these data are inappropriate for general purpose software.

There are 4 common spellout rules. Some languages may provide more than these 4 types:  

* **numbering:** This is the default used when there is no context for the number. For many languages, this may also be used for enumeration of objects, like used when pronouncing "table number one" and "table number two". It can also be used for pronouncing a math equation, like "2 - 3 = -1".
* **numbering-year:** This is used for cases where years are pronounced or written a certain way. An example in English is the year 1999, which comes out as "nineteen ninety-nine" instead of the numbering value "one thousand nine hundred ninety-nine". The rules for this type have undefined behavior for non-integer numbers, and values less than 1.
* **cardinal:** This is used when providing the quantity of the number of objects. For many languages, there may not be a default cardinal type. Many languages require the notion of the gender and other grammatical properties so that the number and the objects being referenced are in grammatical agreement. An example of its usage is "one e-mail", "two people" or "three kilometers". Some languages may not have dedicated words for 0 or negative numbers for cardinals. In those cases, the words from the numbering type can be reused.
* **ordinal:** This is used when providing the order of the number of objects. For many languages, there may not be a default ordinal type. Many languages also require the notion of the gender for ordinal so that the ordinal number and the objects being referenced are in grammatical agreement. An example of its usage is "first place", "second e-mail" or "third house on the right". The rules for this type have undefined behavior for non-integer numbers, and values less than 1.

In addition to the spellout rules, there are also a numbering system rules. Even though they may be derived from a specific culture, they are typically not translated and the rules are in **root**. An example of these rules are the Roman numerals where the value 8 comes out as VIII.  

With regards to the number range supported for all these number types, the largest possible number range tries to be supported, but some languages may not have words for large numbers. For example, the old Roman numbering system can't support the value 5000 and beyond. For those unsupported cases, the default number format from CLDR is used.  

Any rules marked as **private** should never be referenced externally. Frequently they only support a subrange of numbers that are used in the public rules.  

The syntax used in the CLDR representation of rules is intended to be simply a transcription of ICU based RBNF rules into an XML compatible syntax. The rules are fairly sophisticated; for details see _Rule-Based Number Formatter_ [[RBNF](tr35.md#RBNF)].

```xml
<ruleSetGrouping>
```

Used to group rules into functional sets for use with ICU. Currently, the valid types of rule set groupings are "SpelloutRules", "OrdinalRules", and "NumberingSystemRules".

```xml
<ruleset>
```

This element denotes a specific rule set to the number formatter. The ruleset is assumed to be a public ruleset unless the attribute type="private" is specified.

```xml
<rule>
```

Contains the actual formatting rule for a particular number or sequence of numbers. The `value` attribute is used to indicate the starting number to which the rule applies. The actual text of the rule is identical to the ICU syntax, with the exception that Unicode left and right arrow characters are used to replace < and > in the rule text, since < and > are reserved characters in XML. The `radix` attribute is used to indicate an alternate radix to be used in calculating the prefix and postfix values for number formatting. Alternate radix values are typically used for formatting year numbers in formal documents, such as "nineteen hundred seventy-six" instead of "one thousand nine hundred seventy-six".

## 7 <a name="Parsing_Numbers" href="#Parsing_Numbers">Parsing Numbers</a>

The following elements are relevant to determining the value of a parsed number:

* A possible prefix or suffix, indicating sign
* A possible currency symbol or code
* Decimal digits
* A possible decimal separator
* A possible exponent
* A possible percent or per mille character

Other characters should either be ignored, or indicate the end of input, depending on the application. The key point is to disambiguate the sets of characters that might serve in more than one position, based on context. For example, a period might be either the decimal separator, or part of a currency symbol (for example, "NA f."). Similarly, an "E" could be an exponent indicator, or a currency symbol (the Swaziland Lilangeni uses "E" in the "en" locale). An apostrophe might be the decimal separator, or might be the grouping separator.

Here is a set of heuristic rules that may be helpful:

* Any character with the decimal digit property is unambiguous and should be accepted.
    
  **Note:** In some environments, applications may independently wish to restrict the decimal digit set to prevent security problems. See [[UTR36](https://www.unicode.org/reports/tr41/#UTR36)].
    
* The exponent character can only be interpreted as such if it occurs after at least one digit, and if it is followed by at least one digit, with only an optional sign in between. A regular expression may be helpful here.
* For the sign, decimal separator, percent, and per mille, use a set of all possible characters that can serve those functions. For example, the decimal separator set could include all of [.,']. (The actual set of characters can be derived from the number symbols in the By-Type charts [[ByType](tr35.md#ByType)], which list all of the values in CLDR.) To disambiguate, the decimal separator for the locale must be removed from the "ignore" set, and the grouping separator for the locale must be removed from the decimal separator set. The same principle applies to all sets and symbols: any symbol must appear in at most one set.
* Since there are a wide variety of currency symbols and codes, this should be tried before the less ambiguous elements. It may be helpful to develop a set of characters that can appear in a symbol or code, based on the currency symbols in the locale.
* Otherwise, a character should be ignored unless it is in the "stop" set. This includes even characters that are meaningful for formatting, for example, the grouping separator.
* If more than one sign, currency symbol, exponent, or percent/per mille occurs in the input, the first found should be used.
* A currency symbol in the input should be interpreted as the longest match found in the set of possible currency symbols.
* Especially in cases of ambiguity, the user's input should be echoed back, properly formatted according to the locale, before it is actually used for anything.

## 8 <a name="Number_Range_Formatting" href="#Number_Range_Formatting">Number Range Formatting</a>

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

### 8.1 <a name="Approximate_Number_Formatting" href="#Approximate_Number_Formatting">Approximate Number Formatting</a>

*Approximate number formatting* refers to a specific format of numbers in which the value is understood to not be exact; for example, "\~5 minutes".

To format an approximate number, follow the normal number formatting procedure in Number Format Patterns](#Number_Format_Patterns), but substitute the `approximatelySign` from [Number Symbols](#Number_Symbols) in for the minus sign placeholder.

If the number is negative, or if the formatting options request the sign to be displayed, *prepend* the `approximatelySign` to the plus or minus sign before substituting it into the pattern. For example, "\~-5" means "approximately negative five". This procedure may change in the future.

### 8.2 <a name="Collapsing_Number_Ranges" href="#Collapsing_Number_Ranges">Collapsing Number Ranges</a>

*Collapsing* a number range refers to the process of removing duplicated information in the *lower* and *upper* values. For example, if the lower string is "3.2 centimeters" and the upper string is "4.5 centimeters", it is desirable to remove the extra "centimeters" token.

This operation requires *semantic annotations* on the formatted value. The exact form of the semantic annotations is implementation-dependent. However, implementations may consider the following broad categories of tokens:

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

The above description describes the expected output. Internally, the implementation may determine the equivalent units of measurement by passing the codes back from the number formatters, allowing for a precise determination of "semantically equivalent".

Two semantically equivalent tokens can be *collapsed* if they appear at the start of both values or the end of both values. However, the implementation may choose different levels of aggressiveness with regard to collapsing tokens. The currently recommended heuristic is:

1. Only collapse semantically equivalent *unit of measurement* tokens. This is to avoid ambiguous strings such as "3–5K" (could represent 3–5000 or 3000–5000).
1. Only collapse if the tokens are more than one code point in length. This is to increase clarity of strings such as "$3–$5".

These heuristics may be refined in the future.

**To collapse tokens:** Remove the token from both values, and then re-compute the token based on the number range. If the token depends on the plural form, follow [Plural Ranges](#Plural_Ranges) to calculate the correct form. If the tokens originated at the beginning of the string, prepend the new token to the beginning of the *lower* string; otherwise, append the new token to the end of the *upper* string.

### 8.3 <a name="Range_Pattern_Processing" href="#Range_Pattern_Processing">Range Pattern Processing</a>

To obtain a number range pattern, the following steps are taken:

1. Load the range pattern found in [Miscellaneous Patterns](#Miscellaneous_Patterns).
1. Optionally add spacing to the range pattern.

To determine whether to add spacing, the currently recommended heuristic is:

1. If the *lower* string ends with a character other than a digit, or if the *upper* string begins with a character other than a digit.
2. If the range pattern does not contain a character having the `White_Space` binary Unicode property after the `{0}` or before the `{1}` placeholders.

These heuristics may be refined in the future.

To add spacing, insert a non-breaking space (U+00A0) at the positions in item 2 above.

* * *

Copyright © 2001–2022 Unicode, Inc. All Rights Reserved. The Unicode Consortium makes no expressed or implied warranty of any kind, and assumes no liability for errors or omissions. No liability is assumed for incidental and consequential damages in connection with or arising out of the use of the information or programs contained or accompanying this technical report. The Unicode [Terms of Use](https://unicode.org/copyright.html) apply.

Unicode and the Unicode logo are trademarks of Unicode, Inc., and are registered in some jurisdictions.

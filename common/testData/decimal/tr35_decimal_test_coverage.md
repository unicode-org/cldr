# UTS #35 Decimal Number Formatting Specification — Test Data Coverage Verification

This document traces each sentence of the number formatting specification for plain numbers (decimal, percent, scientific, and compact) in **UTS #35: Unicode LDML, Part 3: Numbers** ([`docs/ldml/tr35-numbers.md`](../../../docs/ldml/tr35-numbers.md)) to the test data dimension values that exercise it. The currency sections are covered in [`../currency/tr35_currency_test_coverage.md`](../currency/tr35_currency_test_coverage.md).

For each deep-linked section in UTS #35:
1. We link directly to the specification anchor in [`docs/ldml/tr35-numbers.md`](../../../docs/ldml/tr35-numbers.md), with its line range at the pinned commit.
2. We quote the exact normative snippet.
3. We break down every normative sentence/clause in the snippet into the exact **`(dimension = value)` combinations** required to exercise that behavior, with the expected result derived from the CLDR data.
4. We check those combinations against the dimensions of [`GenerateDecimalFormatTestData.java`](../../../tools/cldr-code/src/main/java/org/unicode/cldr/tool/GenerateDecimalFormatTestData.java): first whether the generator has each dimension, then whether each `(dimension = value)` is one of the CORE values of that dimension, and if not, whether it is one of its extended values.

### Pinned Inputs

| Input | Version |
| :--- | :--- |
| Specification text (`docs/ldml/tr35-numbers.md`) | `c33251cf82` (PR [#6191](https://github.com/unicode-org/cldr/pull/6191)) |
| CLDR data (`common/main`, `common/supplemental`) | `c33251cf82` |
| Generator and TSV files (`GenerateDecimalFormatTestData.java`, [`common/testData/decimal/`](.)) | `c33251cf82` (identical to `main` at `016a645a70`) |
| ICU4J that produced the TSV `expected` values | `79.0.1-20260723.162400-4` (pinned in `tools/pom.xml` at `c33251cf82`) |

Expected values in the breakdown tables are derived from the CLDR data and the specification, and cross-checked with the same ICU4J version.

### Coverage Status

| Status | Meaning |
| :--- | :--- |
| ✅ **Covered** | At least one combination of CORE dimension values represents the clause |
| 🟡 **Missing: *dimension* in CORE** | The needed value is only among the extended values of the dimension, so it is not combined with the other CORE values |
| 🟡 **Missing: *dimension*** | The needed value is in neither the CORE nor the extended values of the dimension |
| 🟡 **Missing: new dimension** | The generator does not have the dimension; the dimensions table below marks it **Needs to be added** |
| ❓ **Spec unclear** | The specification does not determine the expected value; see the linked ticket |
| ⚪ **Out of scope** | Not testable with formatting test data |

*dimension* is the name of a dimension, for example **Missing: `input` in CORE** or **Missing: `locale`**. This document checks coverage only: whether the `expected` values in the TSV files match the specification is out of scope.

---

## Current Test Data Dimensions in `GenerateDecimalFormatTestData.java`

Values at `c33251cf82`. `decimals.tsv` combines all CORE values with each other (225 rows). `decimals_modern_locales.tsv` combines each extended locale with all five styles and `CORE_NUMBERS` (2,400 rows), and `decimals_extended_numbers.tsv` combines the extended numbers with `CORE_LOCALES` and all five styles (6,300 rows).

| Dimension | Description | CORE values | Extended values |
| :--- | :--- | :--- | :--- |
| **`locale`** | CLDR locale identifier | `CORE_LOCALES`: `ar`, `ar_EG`, `bn`, `de`, `de_CH`, `en`, `ja`, `pt_PT`, `ru` | The other 96 locales that CLDR targets at `modern` coverage (`getExtendedModernLocales()`; `decimals_modern_locales.tsv`) |
| **`number_format`** | Kind of format | `"decimal"`, `"percent"`, `"scientific"` | — |
| **`format_length`** | Compact length | `""` (non-compact), `"short"`, `"long"` (compact) | — |
| **`input`** | Number to format | `CORE_NUMBERS`: `0.0`, `1.2`, `0.00831765`, `1234565.0`, `-1230.05` | 10ⁱ, 1.5 × 10ⁱ, and 5 × 10ⁱ for −6 ≤ i ≤ 12; `12`, `123`, `1234.56`, `1234567`, `0.000123`, `0.5`, `2.5`, `3.5`, `0.125`, `0.135`, `999.9`, `999999.9`; the negatives of all positive values, including the CORE ones; and `-0.0` (`getExtendedNumbers()` minus `CORE_NUMBERS`; `decimals_extended_numbers.tsv`) |
| **`numbering_system`** | The `nu` key of the Unicode locale identifier (`-u-nu-…`) | **Needs to be added** (Section 1): `"latn"`, `"native"`, `"traditio"`, `"finance"`. The current rows use no `nu` key, that is, the locale's default numbering system. | — |
| **`sign_display`** | When a sign is shown | **Needs to be added** (Section 2): `"always"` (the `plusSign` for positive numbers), `"approximately"` (the `approximatelySign`). The current rows show a sign only for negative numbers. | — |
| **`exponent_style`** | How the exponent of `"scientific"` is written | **Needs to be added** (Section 2): `"superscript"` (`superscriptingExponent`, as in `1.234565×10⁶`). The current rows use the `exponential` symbol (`1.234565E6`). | — |

The generator produces 5 of the 9 combinations of `number_format` and `format_length`: `"decimal"` with each length, and `"percent"` and `"scientific"` with `""` only.

A section that needs a dimension the generator does not have adds it to this table, marked **Needs to be added**.

---

## Section 1: Numbering Systems (`#defaultNumberingSystem`, `#otherNumberingSystems`)

* **TR35 Specification Link**: [`tr35-numbers.md#defaultNumberingSystem`](../../../docs/ldml/tr35-numbers.md#defaultNumberingSystem) and [`tr35-numbers.md#otherNumberingSystems`](../../../docs/ldml/tr35-numbers.md#otherNumberingSystems) (UTS #35 Part 3, Sections 2.1 and 2.2; L164–L199 at `c33251cf82`)
* **Related specification text**: L117–L154 ([Numbering Systems](../../../docs/ldml/tr35-numbers.md#Numbering_Systems): numeric and algorithmic systems), L303–L306 and L370–L374 (the `numberSystem` attribute of `<symbols>` and of the number formats), and [Numbering System Data](../../../docs/ldml/tr35.md#Numbering%20System%20Data) in Part 1 (the `nu` identifiers)

### 1.1 Verbatim Specification Snippet (`docs/ldml/tr35-numbers.md`)

> ### <a name="defaultNumberingSystem" href="../../../docs/ldml/tr35-numbers.md#defaultNumberingSystem">Default Numbering System</a>
>
> ```dtd
> <!ELEMENT defaultNumberingSystem ( #PCDATA )>
> ```
>
> This element indicates which numbering system should be used for presentation of numeric quantities in the given locale.
>
> ### <a name="otherNumberingSystems" href="../../../docs/ldml/tr35-numbers.md#otherNumberingSystems">Other Numbering Systems</a>
>
> ```dtd
> <!ELEMENT otherNumberingSystems ( alias | ( native*, traditional*, finance*)) >
> ```
>
> This element defines general categories of numbering systems that are sometimes used in the given locale for formatting numeric quantities. These additional numbering systems are often used in very specific contexts, such as in calendars or for financial purposes. There are currently three defined categories, as follows:
>
> **native**
>
> > Defines the numbering system used for the native digits, usually defined as a part of the script used to write the language. The native numbering system can only be a numeric positional decimal-digit numbering system, using digits with General_Category=Decimal_Number. Note: In locales where the native numbering system is the default, it is assumed that the numbering system "latn" (Western digits 0-9) is always acceptable, and can be selected using the -nu keyword as part of a Unicode locale identifier.
>
> **traditional**
>
> > Defines the traditional numerals for a locale. This numbering system may be numeric or algorithmic. If the traditional numbering system is not defined, applications should use the native numbering system as a fallback.
>
> **finance**
>
> > Defines the numbering system used for financial quantities. This numbering system may be numeric or algorithmic. This is often used for ideographic languages such as Chinese, where it would be easy to alter an amount represented in the default numbering system simply by adding additional strokes. If the financial numbering system is not specified, applications should use the default numbering system as a fallback.
>
> The categories defined for other numbering systems can be used in a Unicode locale identifier to select the proper numbering system without having to know the specific numbering system by name. For example:
>
> *   To select Hindi language using the native digits for numeric formatting, use locale ID: "hi-IN-u-nu-native".
> *   To select Chinese language using the appropriate financial numerals, use locale ID: "zh-u-nu-finance".
> *   To select Tamil language using the traditional Tamil numerals, use locale ID: "ta-u-nu-traditio".
> *   To select Arabic language using western digits 0-9, use locale ID: "ar-u-nu-latn".
>
> For more information on numbering systems and their definitions, see _[Section 1: Numbering Systems](../../../docs/ldml/tr35-numbers.md#Numbering_Systems)_.

---

### 1.2 Sentence-by-Sentence `(Dimension / Value)` Coverage Breakdown

**CLDR data.** Resolved values of the three elements. `↑↑↑` and missing values inherit from the parent locale, and finally from `root`.

| Locale | `defaultNumberingSystem` | `native` | `traditional` | `finance` |
| :--- | :--- | :--- | :--- | :--- |
| `root` | `latn` | `latn` | — | — |
| `en`, `de`, `de_CH`, `pt_PT`, `ru` | `latn` (inherited) | `latn` (inherited) | — | — |
| `ar` | `latn` (inherited; `ar` also has `<defaultNumberingSystem alt="latn">latn</defaultNumberingSystem>`) | `arab` | — | — |
| `ar_EG` | `arab` | `arab` (inherited from `ar`) | — | — |
| `bn` | `beng` | `beng` | — | — |
| `ja` | `latn` (inherited) | `latn` (inherited) | `jpan` (algorithmic) | `jpanfin` (algorithmic) |
| `hi` (extended) | `latn` (inherited) | `deva` | — | — |
| `ta` (extended) | `latn` (inherited) | `tamldec` | `taml` (algorithmic) | — |
| `zh` (extended) | `latn` (inherited) | `hanidec` | `hans` (algorithmic) | `hansfin` (algorithmic) |

Across `common/main`, 53 locales have a numeric default other than `latn` (21 `ar_*` locales with `arab`, `fa` with `arabext`, `mr` with `deva`, …), and 57 locales have a `native` value, all numeric. All 18 `traditional` values (`am`, `el`, `he`, `ja`, `ta`, `zh`, …) and all 5 `finance` values (`ja`, `yue`, `yue_Hans`, `zh`, `zh_Hant`) are algorithmic. The extended locales with a default other than `latn` are `as`, `fa`, `mr`, `my`, `ne`, `ps`, and `sd`.

Digits (`common/supplemental/numberingSystems.xml`): `arab` `٠١٢٣٤٥٦٧٨٩` (U+0660–U+0669), `beng` `০১২৩৪৫৬৭৮৯` (U+09E6–U+09EF), `deva` `०१२३४५६७८९` (U+0966–U+096F). The symbols come from `<symbols numberSystem="…">` of the selected numbering system (L305): for `arab`, `root` has `٫` (U+066B) as `decimal` and `٬` (U+066C) as `group`; `beng` uses the `latn` symbols.

| # | Verbatim Sentence / Normative Clause | Required `(Dimension = Value)` Combinations to Cover Clause | CLDR Data Evidence & Expected Behavior |
| :---: | :--- | :--- | :--- |
| **S1.1** | **`defaultNumberingSystem`** — *"This element indicates which numbering system should be used for presentation of numeric quantities in the given locale."* | • **S1.1a**: `locale` whose default is `latn` (e.g. `"en"`, `"de"`, `"ar"`), any `number_format`, any `input`<br>• **S1.1b**: `locale` whose default is a numeric system other than `latn` (`"ar_EG"`: `arab`; `"bn"`: `beng`), any `number_format` and `format_length`, any `input` | • S1.1a: `en` 1234565.0 → `1,234,565`; `ar` 1234565.0 → `1,234,565` (`ar` inherits `latn` from `root`).<br>• S1.1b: `ar_EG` 1234565.0 → `١٬٢٣٤٬٥٦٥`; `ar_EG` −1230.05 → `؜-١٬٢٣٠٫٠٥` / `"\u061C-\u0661\u066C\u0662\u0663\u0660\u066B\u0660\u0665"`; `bn` 1234565.0 → `১২,৩৪,৫৬৫` (`beng` digits, `latn` symbols, and the `bn` pattern `#,##,##0.###`). |
| **S1.2** | **`otherNumberingSystems`** — *"This element defines general categories of numbering systems that are sometimes used in the given locale for formatting numeric quantities. [...] There are currently three defined categories, as follows:"* | — (introduces S1.3–S1.7) | — |
| **S1.3** | **`native`** — *"Defines the numbering system used for the native digits, usually defined as a part of the script used to write the language."* | • **S1.3a**: `numbering_system = "native"` × `locale` whose `native` differs from its default (`"ar"`), any `input`<br>• **S1.3b**: `numbering_system = "native"` × `locale` whose `native` is its default (`"bn"`, `"en"`) | • S1.3a: `ar-u-nu-native` 1234565.0 → `١٬٢٣٤٬٥٦٥` (`arab`, not the default `latn`). Spec example: `hi-IN-u-nu-native` 1234565.0 → `१२,३४,५६५`.<br>• S1.3b: unchanged: `bn-u-nu-native` 1234565.0 → `১২,৩৪,৫৬৫`; `en-u-nu-native` → `1,234,565`. |
| **S1.4** | **`native`** — *"The native numbering system can only be a numeric positional decimal-digit numbering system, using digits with General_Category=Decimal_Number."* | — | A constraint on the data: all 57 `native` values are numeric systems. |
| **S1.5** | **`native`** — *"Note: In locales where the native numbering system is the default, it is assumed that the numbering system \"latn\" (Western digits 0-9) is always acceptable, and can be selected using the -nu keyword as part of a Unicode locale identifier."* | • `numbering_system = "latn"` × `locale` whose default is its `native` system (`"ar_EG"`, `"bn"`), any `input` | `ar-EG-u-nu-latn` 1234565.0 → `1,234,565`; −1230.05 → `‎-1,230.05` / `"\u200E-1,230.05"` (the `latn` symbols of `ar`); `bn-u-nu-latn` 1234565.0 → `12,34,565`. |
| **S1.6** | **`traditional`** — *"Defines the traditional numerals for a locale. This numbering system may be numeric or algorithmic. If the traditional numbering system is not defined, applications should use the native numbering system as a fallback."* | • **S1.6a**: `numbering_system = "traditio"` × `locale` with a `traditional` value (`"ja"`: `jpan`)<br>• **S1.6b**: `numbering_system = "traditio"` × `locale` without `traditional` whose `native` differs from its default (`"ar"`), any `input` | • S1.6a: `jpan` is algorithmic (`rules="ja/SpelloutRules/spellout-cardinal"`), so the number is formatted with rule-based number formatting, not with a pattern. No CLDR locale has a numeric `traditional` value.<br>• S1.6b: `ar-u-nu-traditio` 1234565.0 → `١٬٢٣٤٬٥٦٥` (the `native` `arab`, not the default `latn`). |
| **S1.7** | **`finance`** — *"Defines the numbering system used for financial quantities. This numbering system may be numeric or algorithmic. [...] If the financial numbering system is not specified, applications should use the default numbering system as a fallback."* | • **S1.7a**: `numbering_system = "finance"` × `locale` with a `finance` value (`"ja"`: `jpanfin`)<br>• **S1.7b**: `numbering_system = "finance"` × `locale` without `finance` whose default differs from its `native` (`"ar"`), any `input` | • S1.7a: `jpanfin` is algorithmic (`ja/SpelloutRules/spellout-cardinal-financial`), as are all 5 `finance` values.<br>• S1.7b: `ar-u-nu-finance` 1234565.0 → `1,234,565` (the default `latn`, not the `native` `arab`). |
| **S1.8** | *"The categories defined for other numbering systems can be used in a Unicode locale identifier to select the proper numbering system without having to know the specific numbering system by name."* | • `numbering_system` = `"native"`, `"traditio"`, or `"finance"` (S1.3, S1.6, S1.7) | The four examples: `hi-IN-u-nu-native` (S1.3a); `zh-u-nu-finance` (`hansfin`) and `ta-u-nu-traditio` (`taml`), both algorithmic (S1.6a, S1.7a); `ar-u-nu-latn`, a numbering system selected by name (S1.5). |

---

### 1.3 Comparison Against `GenerateDecimalFormatTestData.java`

| Clause | Required `(Dimension = Value)` Combination | Status | Generator Evidence / Action Required |
| :---: | :--- | :---: | :--- |
| **S1.1a** | `locale` with default `latn` × any `number_format` × any `input` | ✅ **Covered** | CORE values: `en`, `de`, `de_CH`, `ja`, `pt_PT`, `ru`, and `ar` in `CORE_LOCALES`, with all five styles and `CORE_NUMBERS`. |
| **S1.1b** | `locale` with a numeric default other than `latn` × any `number_format` and `format_length` × any `input` | ✅ **Covered** | CORE values: `ar_EG` (`arab`) and `bn` (`beng`) in `CORE_LOCALES`, with all five styles and `CORE_NUMBERS`. The extended locales `as`, `fa`, `mr`, `my`, `ne`, `ps`, and `sd` add `arabext`, `deva`, and `mymr`. |
| **S1.3a–b**, **S1.5**, **S1.6b**, **S1.7b**, **S1.8** | `numbering_system` = `"native"`, `"latn"`, `"traditio"`, or `"finance"` × the locales above | 🟡 **Missing: new dimension** | The generator formats each locale identifier as is (`new ULocale(localeStr)`), and no CORE or extended locale identifier has a `-u-nu-` key. The locales are CORE values (`ar`, `ar_EG`, `bn`, `en`). **Action**: add the `numbering_system` dimension (see the Summary). |
| **S1.4** | — | ⚪ **Out of scope** | A constraint on the CLDR data, not on formatting. |
| **S1.6a**, **S1.7a** | `numbering_system` = `"traditio"` or `"finance"` × a locale whose value is algorithmic (`ja`) | ⚪ **Out of scope** | Algorithmic numbering systems are formatted with rule-based number formatting ([Rule-Based Number Formatting](../../../docs/ldml/tr35-numbers.md#Rule-Based_Number_Formatting)), which this generator does not cover. |

### 1.4 Notes

* `ar`, `hnj`, and `mww` have a `<defaultNumberingSystem alt="latn">`. The specification text does not describe an `alt` value for `defaultNumberingSystem`.
* The spec example `ar-u-nu-latn` gives the same result as `ar`: in the CLDR data, `ar` already defaults to `latn`, and only `ar_EG` and 20 other `ar_*` locales default to `arab`. `ar-EG-u-nu-latn` (S1.5) shows the switch.
* S1.6b and S1.7b need a locale whose `native` differs from its default, such as `ar`: in `bn` or `en` the fallbacks to `native` and to the default give the same result.

---

## Section 2: Number Symbols (`#Number_Symbols`)

* **TR35 Specification Link**: [`tr35-numbers.md#Number_Symbols`](../../../docs/ldml/tr35-numbers.md#Number_Symbols) (UTS #35 Part 3, Section 2.3: *Number Symbols*; L201–L306 at `c33251cf82`). The quote below has L207–L265, the general text and the symbols used for plain numbers, and L302–L305, the `numberSystem` attribute. The other symbols are covered elsewhere: `currencyDecimal` and `currencyGroup` (L267–L273) in Section 1 of the [currency document](../currency/tr35_currency_test_coverage.md), and `timeSeparator` (L275–L279) belongs to date and time formats.
* **Related specification text**: L679–L691 ([Number Pattern Character Definitions](../../../docs/ldml/tr35-numbers.md#Number_Pattern_Character_Definitions): the pattern characters `.`, `-`, `,`, `E`, `+`, `%`, and `‰`, and the symbols that replace them), L755–L761 ([Explicit Plus Signs](../../../docs/ldml/tr35-numbers.md#Explicit_Plus)), L775–L779 ([Special Values](../../../docs/ldml/tr35-numbers.md#special-values): NaN and infinity), and L781 onward ([Scientific Notation](../../../docs/ldml/tr35-numbers.md#sci))

### 2.1 Verbatim Specification Snippet (`docs/ldml/tr35-numbers.md`)

> Number symbols define the localized symbols that are commonly used when formatting numbers in a given locale. These symbols can be referenced using a number formatting pattern as defined in _[Section 3: Number Format Patterns](../../../docs/ldml/tr35-numbers.md#Number_Format_Patterns)_.
>
> The available number symbols are as follows:
>
> **decimal**
>
> > separates the integer and fractional part of the number.
>
> **group**
>
> > separates clusters of integer digits to make large numbers more legible; commonly used for thousands (grouping size 3, e.g. "100,000,000") or in some locales, ten-thousands (grouping size 4, e.g. "1,0000,0000"). There may be two different grouping sizes: The _primary grouping size_ used for the least significant integer group, and the _secondary grouping size_ used for more significant groups; these are not the same in all locales (e.g. "12,34,56,789"). If a pattern contains multiple grouping separators, the interval between the last one and the end of the integer defines the primary grouping size, and the interval between the last two defines the secondary grouping size. All others are ignored, so "#,##,###,####" == "###,###,####" == "##,#,###,####".
>
> **list**
>
> > symbol used to separate numbers in a list intended to represent structured data such as an array; must be different from the **decimal** value. This list separator is for “non-linguistic” usage as opposed to the listPatterns for “linguistic” lists (e.g. “Bob, Carol, and Ted”) described in Part 2, _[List Patterns](../../../docs/ldml/tr35-general.md#ListPatterns)_.
>
> **percentSign**
>
> > symbol used to indicate a percentage (1/100th) amount. (If present, the value is also multiplied by 100 before formatting. That way 1.23 → 123%)
>
> ~~**nativeZeroDigit**~~
>
> > Deprecated - do not use.
>
> ~~**patternDigit**~~
>
> > Deprecated. This was formerly used to provide the localized pattern character corresponding to '#', but localization of the pattern characters themselves has been deprecated for some time (determining the locale-specific _replacements_ for pattern characters is of course not deprecated and is part of normal number formatting).
>
> **minusSign**
>
> > Symbol used to denote negative value.
>
> **plusSign**
>
> > Symbol used to denote positive value.  It can be used to produce modified patterns, so that 3.12 is formatted as "+3.12", for example. The standard number patterns (except for type="accounting") will contain the minusSign, explicitly or implicitly. In the explicit pattern, the value of the plusSign can be substituted for the value of the minusSign to produce a pattern that has an explicit plus sign.
>
> **approximatelySign**
>
> > Symbol used to denote a value that is approximate but not exact. The symbol is substituted in place of the minusSign using the same semantics as plusSign substitution.
>
> **exponential**
>
> > Symbol separating the mantissa and exponent values.
>
> **superscriptingExponent**
>
> > (Programmers are used to the fallback exponent style “1.23E4”, but that should not be shown to end-users. Instead, the exponential notation superscriptingExponent should be used to show a format like “1.23 × 10<sup>4</sup>”. ) The superscripting can use markup, such as `<sup>4</sup>` in HTML, or for the special case of Latin digits, use the superscript characters: U+207B ( ⁻ ), U+2070 ( ⁰ ), U+00B9 ( ¹ ), U+00B2 ( ² ), U+00B3 ( ³ ), U+2074 ( ⁴ ) .. U+2079 ( ⁹ ).
>
> **perMille**
>
> > symbol used to indicate a per-mille (1/1000th) amount. (If present, the value is also multiplied by 1000 before formatting. That way 1.23 → 1230 [1/000])
>
> **infinity**
>
> > The infinity sign. Corresponds to the IEEE infinity bit pattern.
>
> **nan - Not a number**
>
> > The NaN sign. Corresponds to the IEEE NaN bit pattern.
>
> […]
>
> ```dtd
> <!ATTLIST symbols numberSystem CDATA #IMPLIED >
> ```
> The `numberSystem` attribute is used to specify that the given number symbols are to be used when the given numbering system is active. Number symbols can only be defined for numbering systems of the "numeric" type, since any special symbols required for an algorithmic numbering system should be specified by the RBNF formatting rules used for that numbering system. The `numberSystem` attribute will always be present in CLDR 49 and beyond. The DTD does not require it, so that older versions of CLDR can be read with as before.  Locales that specify a numbering system other than "latn" as the default should also specify number formatting symbols that are appropriate for use within the context of the given numbering system. For example, a locale that uses the Arabic-Indic digits as its default would likely use an Arabic comma for the grouping separator rather than the ASCII comma.

---

### 2.2 Sentence-by-Sentence `(Dimension / Value)` Coverage Breakdown

**CLDR data.** Resolved symbols of the default numbering system of each CORE locale (`<symbols numberSystem="…">`, following `↑↑↑` and missing values to the parent locale and to `root`; `root` makes the `beng` symbols an alias of the `latn` ones).

| Locale (numbering system) | `decimal` | `group` | `percentSign` | `minusSign` | `plusSign` | `approximatelySign` | `exponential` | `superscriptingExponent` | `perMille` | `nan` |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `en`, `ja` (`latn`) | `.` | `,` | `%` | `-` | `+` | `~` (`ja`: `約`) | `E` | `×` | `‰` | `NaN` |
| `de` (`latn`) | `,` | `.` | `%` | `-` | `+` | `≈` | `E` | `·` | `‰` | `NaN` |
| `de_CH` (`latn`) | `.` | `'` (U+0027) | `%` | `-` | `+` | `≈` | `E` | `·` | `‰` | `NaN` |
| `pt_PT`, `ru` (`latn`) | `,` | `"\u00A0"` | `%` | `-` | `+` | `~` (`ru`: `≈`) | `E` | `×` | `‰` | `NaN` (`ru`: `не число` / `"\u043D\u0435\u00A0\u0447\u0438\u0441\u043B\u043E"`) |
| `bn` (`beng`) | `.` | `,` | `%` | `-` | `+` | `~` | `E` | `×` | `‰` | `NaN` |
| `ar` (`latn`) | `.` | `,` | `"\u200E%\u200E"` | `"\u200E-"` | `"\u200E+"` | `~` | `E` | `×` | `‰` | `ليس رقمًا` / `"\u0644\u064A\u0633\u00A0\u0631\u0642\u0645\u064B\u0627"` |
| `ar_EG` (`arab`) | `٫` (U+066B) | `٬` (U+066C) | `"\u066A\u061C"` | `"\u061C-"` | `"\u061C+"` | `~` | `أس` | `×` | `؉` (U+0609) | as `ar` |

`infinity` is `∞` (U+221E) in all of them. Patterns (default numbering system): `#,##0.###` for decimal (`bn`: `#,##,##0.###`), `#,##0%` for percent (`de` and `ru`: `"#,##0\u00A0%"`), and `#E0` for scientific.

Scans of `common/main` (all locales): no decimal, percent, scientific, or currency pattern has a primary grouping size of 4 or more than two grouping separators, and no pattern contains `‰`. 26 locales have a secondary grouping size different from the primary one (`bn`, `hi`, `en_IN`, …). 15 locales have U+2212 MINUS SIGN as `minusSign`; among the extended locales, `et`, `eu`, `fa`, `fi`, `hr`, `lt`, `no`, `sl`, and `sv`.

| # | Verbatim Sentence / Normative Clause | Required `(Dimension = Value)` Combinations to Cover Clause | CLDR Data Evidence & Expected Behavior |
| :---: | :--- | :--- | :--- |
| **S2.1** | *"Number symbols define the localized symbols that are commonly used when formatting numbers in a given locale. These symbols can be referenced using a number formatting pattern [...]"* | — (introduces S2.2–S2.14) | — |
| **S2.2** | **`decimal`** — *"separates the integer and fractional part of the number."* | • `locale` whose `decimal` is not `.` (e.g. `"de"`, `"ar_EG"`) and one whose `decimal` is `.` (`"en"`)<br>• `input` with a fractional part (e.g. `1.2`) | `de` 1.2 → `1,2`; `ar_EG` → `١٫٢`; `en` → `1.2`. |
| **S2.3** | **`group`** — *"separates clusters of integer digits to make large numbers more legible; commonly used for thousands (grouping size 3, e.g. "100,000,000") or in some locales, ten-thousands (grouping size 4, e.g. "1,0000,0000")."* | • **S2.3a**: grouping size 3: any `locale` with a primary grouping size 3, `input` of magnitude ≥ 1000 (e.g. `1234565.0`), with several `group` values (`"en"`, `"de"`, `"de_CH"`, `"ru"`, `"ar_EG"`)<br>• **S2.3b**: grouping size 4 | • S2.3a: `en` 1234565.0 → `1,234,565`; `de` → `1.234.565`; `de_CH` → `1'234'565`; `ru` → `1 234 565` / `"1\u00A0234\u00A0565"`; `ar_EG` → `١٬٢٣٤٬٥٦٥`.<br>• S2.3b: no CLDR pattern has a primary grouping size of 4. |
| **S2.4** | **`group`** — *"There may be two different grouping sizes: The primary grouping size used for the least significant integer group, and the secondary grouping size used for more significant groups; these are not the same in all locales (e.g. "12,34,56,789"). If a pattern contains multiple grouping separators, the interval between the last one and the end of the integer defines the primary grouping size, and the interval between the last two defines the secondary grouping size. All others are ignored, so "#,##,###,####" == "###,###,####" == "##,#,###,####"."* | • **S2.4a**: `locale` with different primary and secondary grouping sizes (`"bn"`: `#,##,##0.###`), `input` with at least 6 integer digits (e.g. `1234565.0`)<br>• **S2.4b**: a pattern with more than two grouping separators (*"All others are ignored"*) | • S2.4a: `bn` 1234565.0 → `১২,৩৪,৫৬৫` (primary 3, secondary 2).<br>• S2.4b: no CLDR pattern has more than two grouping separators. |
| **S2.5** | **`list`** — *"symbol used to separate numbers in a list intended to represent structured data such as an array; must be different from the **decimal** value. [...]"* | — | Number formatting does not use `list`. That it differs from `decimal` is a constraint on the data. |
| **S2.6** | **`percentSign`** — *"symbol used to indicate a percentage (1/100th) amount. (If present, the value is also multiplied by 100 before formatting. That way 1.23 → 123%)"* | • `number_format = "percent"` × several `locale` values (`"en"`, `"de"`, `"ar"`, `"ar_EG"`), any `input` | `en` 1.2 → `120%`; `de` → `120 %` / `"120\u00A0%"`; `ar` → `120‎%‎` / `"120\u200E%\u200E"`; `ar_EG` → `١٢٠٪؜` / `"\u0661\u0662\u0660\u066A\u061C"`. |
| **S2.7** | **`nativeZeroDigit`**, **`patternDigit`** — *"Deprecated - do not use."*, *"Deprecated. [...]"* | — | Deprecated; not used in formatting. |
| **S2.8** | **`minusSign`** — *"Symbol used to denote negative value."* | • negative `input` (e.g. `-1230.05`) × several `locale` values (`"en"`, `"ar"`, `"ar_EG"`) | `en` −1230.05 → `-1,230.05`; `ar` → `‎-1,230.05` / `"\u200E-1,230.05"`; `ar_EG` → `؜-١٬٢٣٠٫٠٥` / `"\u061C-\u0661\u066C\u0662\u0663\u0660\u066B\u0660\u0665"`. |
| **S2.9** | **`plusSign`** — *"Symbol used to denote positive value. It can be used to produce modified patterns, so that 3.12 is formatted as "+3.12", for example. The standard number patterns (except for type="accounting") will contain the minusSign, explicitly or implicitly. In the explicit pattern, the value of the plusSign can be substituted for the value of the minusSign to produce a pattern that has an explicit plus sign."* | • `sign_display = "always"` × positive `input` (e.g. `1.2`) × several `locale` values (`"en"`, `"ar"`, `"ar_EG"`) | `en` 1.2 → `+1.2`; `ar` → `‎+1.2` / `"\u200E+1.2"`; `ar_EG` → `؜+١٫٢` / `"\u061C+\u0661\u066B\u0662"`. |
| **S2.10** | **`approximatelySign`** — *"Symbol used to denote a value that is approximate but not exact. The symbol is substituted in place of the minusSign using the same semantics as plusSign substitution."* | • `sign_display = "approximately"` × positive `input` × several `locale` values (`"en"`, `"de"`, `"ja"`) | `en` 1.2 → `~1.2`; `de` → `≈1,2`; `ja` → `約1.2`. |
| **S2.11** | **`exponential`** — *"Symbol separating the mantissa and exponent values."* | • `number_format = "scientific"` × several `locale` values (`"en"`, `"ar_EG"`), any `input` | `en` 1234565.0 → `1.234565E6`; `ar_EG` → `١٫٢٣٤٥٦٥أس٦`. |
| **S2.12** | **`superscriptingExponent`** — *"(Programmers are used to the fallback exponent style “1.23E4”, but that should not be shown to end-users. Instead, the exponential notation superscriptingExponent should be used to show a format like “1.23 × 10<sup>4</sup>”. ) The superscripting can use markup, such as `<sup>4</sup>` in HTML, or for the special case of Latin digits, use the superscript characters: [...]"* | • `number_format = "scientific"` × `exponent_style = "superscript"` × `locale` with Latin digits and different `superscriptingExponent` values (`"en"`: `×`; `"de"`: `·`), any `input` | `en` 1234565.0 → `1.234565×10⁶`; `de` → `1,234565·10⁶` (see the notes on spacing). |
| **S2.13** | **`perMille`** — *"symbol used to indicate a per-mille (1/1000th) amount. (If present, the value is also multiplied by 1000 before formatting. That way 1.23 → 1230 [1/000])"* | — | No CLDR pattern contains `‰`. Per-mille amounts are formatted as the unit `concentr-permille`, outside this generator. |
| **S2.14** | **`infinity`**, **`nan`** — *"The infinity sign. Corresponds to the IEEE infinity bit pattern."*, *"The NaN sign. Corresponds to the IEEE NaN bit pattern."* | • `input` = `Infinity`, `-Infinity`, `NaN` × several `locale` values (`"en"`, `"ar_EG"`, `"ru"`) | `en` → `∞`, `-∞`, `NaN`; `ar_EG` −∞ → `؜-∞` / `"\u061C-\u221E"`; `ru` NaN → `не число` / `"\u043D\u0435\u00A0\u0447\u0438\u0441\u043B\u043E"`. |
| **S2.15** | **`numberSystem`** — *"The `numberSystem` attribute is used to specify that the given number symbols are to be used when the given numbering system is active. [...] Locales that specify a numbering system other than "latn" as the default should also specify number formatting symbols that are appropriate for use within the context of the given numbering system. For example, a locale that uses the Arabic-Indic digits as its default would likely use an Arabic comma for the grouping separator rather than the ASCII comma."* | • **S2.15a**: `locale` whose default is not `latn` (`"ar_EG"`) and the same language with `latn` (`"ar"`), `input` of magnitude ≥ 1000<br>• **S2.15b**: *"Number symbols can only be defined for numbering systems of the "numeric" type [...]"* and *"The `numberSystem` attribute will always be present in CLDR 49 and beyond."* | • S2.15a: `ar_EG` 1234565.0 → `١٬٢٣٤٬٥٦٥` (group `٬` U+066C, ARABIC THOUSANDS SEPARATOR) vs. `ar` → `1,234,565`. With Section 1's `numbering_system`, `ar-u-nu-native` also uses the `arab` symbols.<br>• S2.15b: constraints on the data. |

---

### 2.3 Comparison Against `GenerateDecimalFormatTestData.java`

| Clause | Required `(Dimension = Value)` Combination | Status | Generator Evidence / Action Required |
| :---: | :--- | :---: | :--- |
| **S2.2** | `locale` with `decimal` `,` / `٫` / `.` × fractional `input` | ✅ **Covered** | CORE values: `de`, `ar_EG`, and `en` in `CORE_LOCALES`; `1.2` and `-1230.05` in `CORE_NUMBERS`. |
| **S2.3a** | grouping size 3 × several `group` values × `input` ≥ 1000 | ✅ **Covered** | CORE values: `en`, `de`, `de_CH`, `ru`, `pt_PT`, and `ar_EG`; `1234565.0` and `-1230.05` in `CORE_NUMBERS`. |
| **S2.3b**, **S2.4b** | a pattern with grouping size 4, or with more than two grouping separators | ⚪ **Out of scope** | No CLDR data. |
| **S2.4a** | `locale = "bn"` × `input` with ≥ 6 integer digits | ✅ **Covered** | CORE values: `bn` in `CORE_LOCALES`; `1234565.0` in `CORE_NUMBERS`. |
| **S2.5**, **S2.7** | — | ⚪ **Out of scope** | Not used in number formatting (`list`), or deprecated. |
| **S2.6** | `number_format = "percent"` × several `locale` values | ✅ **Covered** | CORE values: `"percent"` with `format_length = ""`, all `CORE_LOCALES`, and `CORE_NUMBERS`. |
| **S2.8** | negative `input` × several `locale` values | ✅ **Covered** | CORE values: `-1230.05` in `CORE_NUMBERS`, all `CORE_LOCALES`. U+2212 MINUS SIGN is only in extended locales (`fi`, `sv`, …: `decimals_modern_locales.tsv`). |
| **S2.9**, **S2.10** | `sign_display = "always"` or `"approximately"` × positive `input` | 🟡 **Missing: new dimension** | The generator does not set a sign display, so it shows a sign only for negative numbers. **Action**: add the `sign_display` dimension (see the Summary). |
| **S2.11** | `number_format = "scientific"` × several `locale` values | ✅ **Covered** | CORE values: `"scientific"` with `format_length = ""`, all `CORE_LOCALES`, and `CORE_NUMBERS`. |
| **S2.12** | `exponent_style = "superscript"` × `number_format = "scientific"` | 🟡 **Missing: new dimension** | The generator's scientific rows use the `exponential` symbol (`E`) only. **Action**: add the `exponent_style` dimension (see the Summary). |
| **S2.13** | — | ⚪ **Out of scope** | No CLDR pattern uses `‰`; per-mille amounts are unit formatting. |
| **S2.14** | `input` = `Infinity`, `-Infinity`, `NaN` | 🟡 **Missing: `input`** | None of these is a CORE or extended number value. **Action**: see the Summary. |
| **S2.15a** | `locale = "ar_EG"` and `"ar"` × `input` ≥ 1000 | ✅ **Covered** | CORE values: `ar_EG` and `ar` in `CORE_LOCALES`; `1234565.0` in `CORE_NUMBERS`. |
| **S2.15b** | — | ⚪ **Out of scope** | Constraints on the data. |

### 2.4 Notes

* S2.10: the specification does not say how the approximately sign combines with a negative number, since both replace the same minus sign. The Summary therefore adds `"approximately"` rows only for non-negative inputs.
* S2.12: the example “1.23 × 10<sup>4</sup>” has spaces around `×`, but no CLDR `superscriptingExponent` value has spaces (`en`: `×`), and the text does not say where the `10` comes from or how it is localized (for example with `arab` digits, which have no superscript characters). The expected values above follow the data: `1.234565×10⁶`.
* S2.14: [Special Values](../../../docs/ldml/tr35-numbers.md#special-values) (L777) says that NaN is shown without the prefixes and suffixes of the pattern; a later section on special values checks the percent and compact results.

---

## Summary: Required Generator Changes

| Change | Needed by |
| :--- | :--- |
| Add the `numbering_system` dimension, with its rows in a separate file: `CORE_LOCALES` × `"latn"`, `"native"`, `"traditio"`, `"finance"` × `number_format = "decimal"`, `format_length = ""` × `CORE_NUMBERS` (+170 rows: 9 × 4 × 5, minus the 10 `ja` rows for `"traditio"` and `"finance"`, whose numbering systems are algorithmic) | S1.3a–b, S1.5, S1.6b, S1.7b, S1.8 |
| Add the `sign_display` dimension, with its rows in a separate file: `CORE_LOCALES` × `"always"` × `number_format = "decimal"`, `format_length = ""` × `CORE_NUMBERS` (45 rows), and `CORE_LOCALES` × `"approximately"` × the same × the non-negative `CORE_NUMBERS` (36 rows) (+81 rows) | S2.9, S2.10 |
| Add the `exponent_style` dimension, with its rows in a separate file: the 7 `CORE_LOCALES` with Latin digits (all but `ar_EG` and `bn`) × `"superscript"` × `number_format = "scientific"` × `CORE_NUMBERS` (+35 rows) | S2.12 |
| Add the special values `Infinity`, `-Infinity`, and `NaN`, with their rows in a separate file: `CORE_LOCALES` × `number_format = "decimal"`, `format_length = ""` (+27 rows) | S2.14 |

The rows that change the digits are `ar` × `"native"` and `"traditio"` (`arab`), and `ar_EG` and `bn` × `"latn"`. The other rows check that each key falls back to the expected numbering system.

Together, Sections 1 and 2 add 313 rows, all in separate files (170 + 81 + 35 + 27); `decimals.tsv` is unchanged.

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

## Summary: Required Generator Changes

| Change | Needed by |
| :--- | :--- |
| Add the `numbering_system` dimension, with its rows in a separate file: `CORE_LOCALES` × `"latn"`, `"native"`, `"traditio"`, `"finance"` × `number_format = "decimal"`, `format_length = ""` × `CORE_NUMBERS` (+170 rows: 9 × 4 × 5, minus the 10 `ja` rows for `"traditio"` and `"finance"`, whose numbering systems are algorithmic) | S1.3a–b, S1.5, S1.6b, S1.7b, S1.8 |

The rows that change the digits are `ar` × `"native"` and `"traditio"` (`arab`), and `ar_EG` and `bn` × `"latn"`. The other rows check that each key falls back to the expected numbering system.

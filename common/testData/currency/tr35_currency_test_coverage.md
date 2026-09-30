# UTS #35 Currency Formatting Specification — Test Data Coverage Verification

This document traces each sentence of the currency formatting specification in **UTS #35: Unicode LDML, Part 3: Numbers** ([`docs/ldml/tr35-numbers.md`](../../../docs/ldml/tr35-numbers.md)) to the test data dimension values that exercise it.

For each deep-linked currency section in UTS #35:
1. We link directly to the specification anchor in [`docs/ldml/tr35-numbers.md`](../../../docs/ldml/tr35-numbers.md), with its line range at the pinned commit.
2. We quote the exact normative snippet.
3. We break down every normative sentence/clause in the snippet into the exact **`(dimension = value)` combinations** required to exercise that behavior, with the expected result derived from the CLDR data.
4. We check those combinations against the dimensions of `GenerateCurrencyFormatTestData.java` (PR [#5808](https://github.com/unicode-org/cldr/pull/5808)): first whether the generator has each dimension, then whether each `(dimension = value)` is one of the CORE values of that dimension, and if not, whether it is one of its extended values.

### Pinned Inputs

| Input | Version |
| :--- | :--- |
| Specification text (`docs/ldml/tr35-numbers.md`) | `2997bffaf0` (PR [#6169](https://github.com/unicode-org/cldr/pull/6169)) |
| CLDR data (`common/main`, `common/supplemental`) | `2997bffaf0` |
| Generator and TSV files (PR [#5808](https://github.com/unicode-org/cldr/pull/5808)) | `5e1d4b0ee3` |
| ICU4J that produced the TSV `expected` values | `79.0.1-20260723.162400-4` (pinned in `tools/pom.xml` at `5e1d4b0ee3`) |

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

## Current Test Data Dimensions in `GenerateCurrencyFormatTestData.java`

Values at `5e1d4b0ee3`. `currencies.tsv` combines all CORE values with each other. Extended values are combined with fewer values: each extended locale with its own currencies (plus one more) and `TINY_NUMBERS` (`1.2`, `-1230.05`); each extended currency with `TINY_LOCALES` (`en`, `ar`, `de`), one or two related locales, and `TINY_NUMBERS`; and the extended numbers only with `TINY_LOCALES` and `TINY_CURRENCIES` (`USD`, `EUR`).

| Dimension | Description | CORE values | Extended values |
| :--- | :--- | :--- | :--- |
| **`locale`** | CLDR locale identifier | `CORE_LOCALES`: `ar`, `ar_EG`, `bn`, `de`, `de_CH`, `en`, `fy`, `ja`, `pt_PT`, `ru` | The other locales that CLDR targets at `modern` coverage (`getExtendedModernLocales()`; `currencies_modern_locales.tsv`) |
| **`currency`** | ISO 4217 currency code | `CORE_CURRENCIES`: `USD`, `EUR`, `JPY`, `RUB`, `EGP` | The other currencies that are legal tender today (`getExtendedModernCurrencies()`; `currencies_*_modern_currencies.tsv`) |
| **`currency_format_length`** | `<currencyFormatLength>` type | `""` (non-compact), `"short"` (compact) | — |
| **`currency_format_type`** | `<currencyFormat>` type | `"standard"`, `"accounting"` | — |
| **`currency_display`** | How the currency is shown | `"symbol"`, `"symbolNarrow"`, `"code"`, `"name"`, `"noCurrency"` (generated with ICU4J `UnitWidth.HIDDEN`) | — |
| **`input`** | Numeric currency amount | `CORE_NUMBERS`: `0.0`, `1.2`, `0.00831765`, `1234565.0`, `-1230.05` | 10ⁱ, 1.5 × 10ⁱ, and 5 × 10ⁱ for −6 ≤ i ≤ 12; `12`, `123`, `1234.56`, `1234567`, `0.000123`, `0.5`, `2.5`, `3.5`, `0.125`, `0.135`, `999.9`, `999999.9`; the negatives of all positive values, including the CORE ones; and `-0.0` (`getExtendedNumbers()` minus `CORE_NUMBERS`; `currencies_*_extended_numbers.tsv`) |

The generator produces 12 of the 20 combinations of `currency_format_length`, `currency_format_type`, and `currency_display`: it skips `"short"` with `"accounting"`, `"name"`, or `"noCurrency"`, and `"accounting"` with `"name"`. `"noCurrency"` is not combined with extended values. In the tables below, *any `¤` display* means `currency_display` = `"symbol"`, `"symbolNarrow"`, or `"code"`.

A section that needs a dimension the generator does not have adds it to this table, marked **Needs to be added**.

---

## Section 1: Monetary Decimal and Grouping Symbols (`#currencydecimal`, `#currencygroup`)

* **TR35 Specification Link**: [`tr35-numbers.md#currencydecimal`](../../../docs/ldml/tr35-numbers.md#currencydecimal) (UTS #35 Part 3, Section 2.3: *Number Symbols*; L286–L292 at `2997bffaf0`)
* **Related specification text**: L712–L714 (pattern characters `.` and `,`), L720 ([`¤` row](../../../docs/ldml/tr35-numbers.md#Number_Pattern_Character_Definitions)), and L1015 ([Formatting Currency Display Names](../../../docs/ldml/tr35-numbers.md#formatting-currency-display-names-unitpattern), step 5)

### 1.1 Verbatim Specification Snippet (`docs/ldml/tr35-numbers.md`)

> #### currencyDecimal
>
> > Optional. If specified, then for currency formatting/parsing this is used as the decimal separator instead of using the regular decimal separator; otherwise, the regular decimal separator is used.
>
> #### currencyGroup
>
> > Optional. If specified, then for currency formatting/parsing this is used as the group separator instead of using the regular group separator; otherwise, the regular group separator is used.

---

### 1.2 Sentence-by-Sentence `(Dimension / Value)` Coverage Breakdown

**CLDR data.** `git grep -n -E "<currency(Decimal|Group)[ >]" -- common/main` finds exactly two locales. Neither has child locales or `parentLocales` entries.

| Locale | Monetary separator | Regular separator | Other relevant data |
| :--- | :--- | :--- | :--- |
| `fr_CH` | `<currencyDecimal>.</currencyDecimal>` | `<decimal>,</decimal>` (inherited from `fr`) | Own `<group>'</group>` (U+0027, added by CLDR-13986) |
| `de_AT` | `<currencyGroup>.</currencyGroup>` | `<group draft="contributed"> </group>` / `"\u00A0"` | `<decimal>,</decimal>` (inherited from `de`). With approved data only, `group` is inherited as `.` from `de`, and the override cannot be observed. |

"Currency with digits > 0" means a currency whose `<currencyData>` fraction digits are greater than 0 (for example `EUR` or `USD`, but not `JPY`). The decimal separator appears for such currencies even when the input has no fractional part (`0.0` → `0.00 $US` in `fr_CH`).

| # | Verbatim Sentence / Normative Clause | Required `(Dimension = Value)` Combinations to Cover Clause | CLDR Data Evidence & Expected Behavior |
| :---: | :--- | :--- | :--- |
| **S1.1** | **`currencyDecimal`** — *"Optional. If specified, then for currency formatting/parsing this is used as the decimal separator instead of using the regular decimal separator;"* | • `locale = "fr_CH"`<br>• `currency` with digits > 0 (e.g. `"EUR"`)<br>• **S1.1a**: `currency_format_length = ""`, any `¤` display, any `input`<br>• **S1.1b**: `currency_format_length = "short"`, `input = 1234565.0`<br>• **S1.1c**: `currency_display = "name"`, any `input`<br>• **S1.1d**: `currency_display = "noCurrency"`, any `input` | Plain decimal in `fr_CH`: `-1'230,05`. Currency (EUR):<br>• S1.1a (−1230.05): `-1'230.05 €` / `"-1'230.05\u00A0€"`<br>• S1.1b (1234565.0): `1.2 M €` / `"1.2\u00A0M\u00A0€"`<br>• S1.1c (−1230.05): `-1'230.05 euros` (L1015 step 5 requires `currencyDecimal`)<br>• S1.1d (−1230.05): `-1'230.05` or `-1'230,05` (spec unclear) |
| **S1.2** | **`currencyDecimal`** — *"...otherwise, the regular decimal separator is used."* | • `locale` without `<currencyDecimal>` (e.g. `"en"`, `"de"`, `"ar_EG"`)<br>• `currency` with digits > 0<br>• `currency_format_length = ""`, any `¤` display, any `input` | None of `en`, `de`, or `ar_EG` specifies `<currencyDecimal>`, so currency formatting uses the regular `<decimal>` symbol (`.` in `en`, `,` in `de`, `٫` U+066B in `ar_EG`). |
| **S1.3** | **`currencyGroup`** — *"Optional. If specified, then for currency formatting/parsing this is used as the group separator instead of using the regular group separator;"* | • `locale = "de_AT"`<br>• **S1.3a**: `currency_format_length = ""`, any `¤` display, `input` of magnitude ≥ 1000 (e.g. `-1230.05`)<br>• **S1.3b**: `currency_format_length = "short"`, `input` with 1000 ≤ magnitude < 10⁶ (e.g. `-1230.05`)<br>• **S1.3c**: `currency_display = "name"`, `input` of magnitude ≥ 1000<br>• **S1.3d**: `currency_display = "noCurrency"`, `input` of magnitude ≥ 1000 | Plain decimal in `de_AT`: `-1 230,05` / `"-1\u00A0230,05"`. Currency (EUR):<br>• S1.3a (−1230.05): `-€ 1.230,05` / `"-€\u00A01.230,05"`<br>• S1.3b (−1230.05): `-€ 1.230` / `"-€\u00A01.230"` or `-€ 1.200` / `"-€\u00A01.200"` (see the S1.3b note below)<br>• S1.3c (−1230.05): `-1.230,05 Euro` or `-1 230,05 Euro` (spec unclear)<br>• S1.3d (−1230.05): `-1.230,05` or `-1 230,05` (spec unclear) |
| **S1.4** | **`currencyGroup`** — *"...otherwise, the regular group separator is used."* | • `locale` without `<currencyGroup>` (e.g. `"en"`, `"de"`, `"de_CH"`, `"bn"`)<br>• `currency_format_length = ""`, any `¤` display, `input` of magnitude ≥ 1000 | None of `en`, `de`, `de_CH`, or `bn` specifies `<currencyGroup>`, so currency formatting uses the regular `<group>` separator (`,` in `en`, `.` in `de`, `'` in `de_CH`, `,` with Indian `#,##,##0.00` grouping in `bn`). |
| **S1.5** | **`currencyDecimal`** and **`currencyGroup`** — *"...for currency formatting/parsing..."* (the parsing half) | — | Not testable: the TSV files contain formatting data only. |

---

### 1.3 Comparison Against `GenerateCurrencyFormatTestData.java` (PR [#5808](https://github.com/unicode-org/cldr/pull/5808))

| Clause | Required `(Dimension = Value)` Combination | Status | Generator Evidence / Action Required |
| :---: | :--- | :---: | :--- |
| **S1.1a–c** | `locale = "fr_CH"` × `currency` with digits > 0 × the S1.1a–c values of `currency_format_length`, `currency_display`, and `input` | 🟡 **Missing: `locale`** | `fr_CH` is in neither `CORE_LOCALES` nor the extended locales: none of the 10 TSV files has `fr_CH` rows, and the modern-locale suite has only `fr` and `fr_CA`. The other values are CORE values (`EUR` and `USD` in `CORE_CURRENCIES`, `1234565.0` in `CORE_NUMBERS`). **Action**: add `"fr_CH"` to `CORE_LOCALES` (see the Summary). |
| **S1.1d** | `locale = "fr_CH"` × `currency_display = "noCurrency"` | ❓ **Spec unclear** | [CLDR-19825](https://unicode-org.atlassian.net/browse/CLDR-19825) |
| **S1.2** | `locale` without `<currencyDecimal>` × `currency` with digits > 0 × `currency_format_length = ""` × any `¤` display | ✅ **Covered** | CORE values: `en`, `de`, and `ar_EG` in `CORE_LOCALES`; `USD`, `EUR`, and `EGP` in `CORE_CURRENCIES`. |
| **S1.3a–b** | `locale = "de_AT"` × the S1.3a–b values of `currency_format_length`, `currency_display`, and `input` | 🟡 **Missing: `locale`** | `de_AT` is in neither `CORE_LOCALES` nor the extended locales (no TSV file has `de_AT` rows). The inputs are CORE values (`-1230.05` and `1234565.0` in `CORE_NUMBERS`). **Action**: add `"de_AT"` to `CORE_LOCALES` only if it is chosen instead of `fr_CH` (see the Summary). |
| **S1.3c–d** | `locale = "de_AT"` × `currency_display = "name"` or `"noCurrency"` | ❓ **Spec unclear** | [CLDR-19825](https://unicode-org.atlassian.net/browse/CLDR-19825) |
| **S1.4** | `locale` without `<currencyGroup>` × `currency_format_length = ""` × any `¤` display × `input` of magnitude ≥ 1000 | ✅ **Covered** | CORE values: `en`, `de`, `de_CH`, and `bn` in `CORE_LOCALES`; `-1230.05` and `1234565.0` in `CORE_NUMBERS`. |
| **S1.5** | Parsing | ⚪ **Out of scope** | — |

### 1.4 Notes

* **Spec unclear** ([CLDR-19825](https://unicode-org.atlassian.net/browse/CLDR-19825)): L288/L292 apply the monetary separators to all currency formatting, L720 applies them only when `¤` is in the pattern, and L1015 step 5 mentions only `currencyDecimal`. ICU4J applies both overrides in all 12 styles.
* **S1.3b**: `de_AT` uses the `short` pattern `0` (inherited from `de`) for amounts below 10⁶. TR35 L471 and L502 format them with the non-compact pattern and "the normal formatting for the locale (such as the grouping separators)", so `currencyGroup` shows. L502 leaves the number of digits open ("typically to 2 or 3 digits"): 1.200 or 1.230 for −1230.05, grouped either way.

---

## Summary: Required Generator Changes

| Change | Needed by |
| :--- | :--- |
| Add one locale with a monetary separator to `CORE_LOCALES` (+300 rows): `"fr_CH"` (recommended: unlike the `de_AT` override, it also shows with approved data only; see 1.2) or `"de_AT"` | S1.1a–c (`fr_CH`) or S1.3a–b (`de_AT`) |

One such locale is enough to exercise the monetary separator override. `en` and the other CORE locales already cover the "otherwise" clauses (S1.2, S1.4). The clauses of the locale that is not added stay 🟡 **Missing: `locale`**.

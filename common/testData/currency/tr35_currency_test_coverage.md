# UTS #35 Currency Formatting Specification — Test Data Coverage Verification

This document provides a reproducible, sentence-by-sentence verification of test data coverage for the currency formatting specification in **UTS #35: Unicode LDML, Part 3: Numbers (`docs/ldml/tr35-numbers.md`)**.

For each deep-linked currency section in UTS #35:
1. We link directly to the normative specification anchor in `tr35-numbers.html`.
2. We quote the exact normative snippet from `docs/ldml/tr35-numbers.md`.
3. We break down every normative sentence/clause in the snippet into the exact **`(dimension = value)` combinations** required to exercise that behavior.
4. We compare the required `(dimension = value)` combinations against `GenerateCurrencyFormatTestData.java` (PR [#5808](https://github.com/unicode-org/cldr/pull/5808)) to verify whether each combination is already covered in `CORE` or needs to be added.

---

## Test Data Dimensions Reference

| Dimension | Description | Allowed / Representative Values |
| :--- | :--- | :--- |
| **`locale`** | CLDR locale identifier | `CORE_LOCALES` (`ar`, `ar_EG`, `bn`, `de`, `de_CH`, `en`, `fy`, `ja`, `pt_PT`, `ru`) + extended modern locales |
| **`currency`** | ISO 4217 3-letter currency code | `CORE_CURRENCIES` (`USD`, `EUR`, `JPY`, `RUB`, `EGP`) + extended modern currencies |
| **`currency_format_length`** | `<currencyFormatLength>` width | `""` (standard plain decimal) or `"short"` (compact short) |
| **`currency_format_type`** | `<currencyFormat type="...">` style | `"standard"` or `"accounting"` |
| **`currency_display`** | Currency symbol/unit representation | `"symbol"`, `"symbolNarrow"`, `"code"`, `"name"`, `"noCurrency"` |
| **`input`** | Numeric currency amount | `CORE_NUMBERS` (`0.0`, `1.2`, `0.00831765`, `1234565.0`, `-1230.05`) + extended numbers |

---

## Section 1: Monetary Decimal and Grouping Symbols (`#Currency_Symbols_Decimal_Group`)

* **TR35 Specification Link**: [`tr35-numbers.html#Currency_Symbols_Decimal_Group`](https://www.unicode.org/reports/tr35/tr35-numbers.html#Currency_Symbols_Decimal_Group) (UTS #35 Part 3, Section 2.3: *Number Symbols*)

### 1.1 Verbatim Specification Snippet (`docs/ldml/tr35-numbers.md`)

> <a name="Currency_Symbols_Decimal_Group"></a>**currencyDecimal**
>
> > Optional. If specified, then for currency formatting/parsing this is used as the decimal separator instead of using the regular decimal separator; otherwise, the regular decimal separator is used.
>
> **currencyGroup**
>
> > Optional. If specified, then for currency formatting/parsing this is used as the group separator instead of using the regular group separator; otherwise, the regular group separator is used.

---

### 1.2 Sentence-by-Sentence `(Dimension / Value)` Coverage Breakdown

| # | Verbatim Sentence / Normative Clause | Required `(Dimension = Value)` Combinations to Cover Clause | CLDR Data Evidence & Expected Behavior |
| :---: | :--- | :--- | :--- |
| **S1.1** | **`currencyDecimal`** — *"Optional. If specified, then for currency formatting/parsing this is used as the decimal separator instead of using the regular decimal separator;"* | • `locale = "fr_CH"` *(defines `<currencyDecimal>.</currencyDecimal>` vs. `<decimal>,</decimal>` inherited from `fr`)*<br>• `currency = "EUR"` or `"USD"` *(currency with fraction digits $> 0$)*<br>• `currency_format_length = ""`<br>• `currency_display = "symbol" \| "symbolNarrow" \| "code"`<br>• `input = 1.2` or `-1230.05` *(non-zero fractional part)* | In all of CLDR `common/main/*.xml`, **`fr_CH`** is the **only** locale that defines `<currencyDecimal>`. Standard decimal formatting in `fr_CH` uses `,` (`-1 230,05` / `"-1\u202F230,05"`), whereas currency formatting uses `.` (`-1 230.05 €` / `"-1\u202F230.05\u00A0€"`). |
| **S1.2** | **`currencyDecimal`** — *"...otherwise, the regular decimal separator is used."* | • `locale = "en"` *(uses regular `<decimal>.</decimal>`)*<br>• `locale = "de"` *(uses regular `<decimal>,</decimal>`)*<br>• `locale = "ar_EG"` *(uses regular `<decimal>٫</decimal>` `\u066B`)*<br>• `currency = "USD" \| "EUR" \| "EGP"`<br>• `currency_format_length = ""`<br>• `input = 1.2`, `-1230.05` | None of `en`, `de`, or `ar_EG` specifies `<currencyDecimal>`, so currency formatting falls back to the regular `<decimal>` symbol (`.` in `en`, `,` in `de`, `٫` in `ar_EG`). |
| **S1.3** | **`currencyGroup`** — *"Optional. If specified, then for currency formatting/parsing this is used as the group separator instead of using the regular group separator;"* | • `locale = "de_AT"` *(defines `<currencyGroup>.</currencyGroup>` vs. `<group> </group>` `\u00A0`)*<br>• `currency = "EUR"` or `"USD"`<br>• `currency_format_length = ""`<br>• `currency_display = "symbol" \| "symbolNarrow" \| "code"`<br>• `input = -1230.05` or `1234565.0` *(magnitude $\ge 10^3$ to trigger grouping)* | In all of CLDR `common/main/*.xml`, **`de_AT`** is the **only** locale that defines `<currencyGroup>`. Standard decimal formatting in `de_AT` uses NBSP ` ` (`1 234 565,00` / `"1\u00A0234\u00A0565,00"`), whereas currency formatting uses `.` (`€ 1.234.565,00` / `"€\u00A01.234.565,00"`). |
| **S1.4** | **`currencyGroup`** — *"...otherwise, the regular group separator is used."* | • `locale = "en"` *(regular `<group>,</group>`)*<br>• `locale = "de"` *(regular `<group>.</group>`)*<br>• `locale = "de_CH"` *(regular `<group>'</group>`)*<br>• `locale = "bn"` *(regular `<group>,</group>` with Indian `#,##,##0.00` secondary grouping)*<br>• `currency_format_length = ""`<br>• `input = -1230.05`, `1234565.0` | None of `en`, `de`, `de_CH`, or `bn` specifies `<currencyGroup>`, so currency formatting falls back to the regular `<group>` separator (`,`, `.`, `'`). |

---

### 1.3 Comparison Against `GenerateCurrencyFormatTestData.java` (PR [#5808](https://github.com/unicode-org/cldr/pull/5808))

| Clause | Required `(Dimension = Value)` Combination | Present in PR #5808 (`CORE`)? | Generator Audit & Action Required |
| :---: | :--- | :---: | :--- |
| **S1.1** | `locale = "fr_CH"` × `currency = "EUR" \| "USD"` × `input = 1.2, -1230.05` | **Missing** | `fr_CH` is not in `CORE_LOCALES`, and `StandardCodes` (`Level.MODERN`) only includes base locale `fr` in `currencies_modern_locales.tsv`. **Action**: Add `"fr_CH"` to `Dimensions.CORE_LOCALES`. |
| **S1.2** | `locale = "en" \| "de" \| "ar_EG"` × `currency = "USD" \| "EUR" \| "EGP"` × `input = 1.2, -1230.05` | **Covered** | Already present in `Dimensions.CORE_LOCALES` × `CORE_CURRENCIES` × `CORE_NUMBERS` (`currencies.tsv`). |
| **S1.3** | `locale = "de_AT"` × `currency = "EUR" \| "USD"` × `input = -1230.05, 1234565.0` | **Missing** | `de_AT` is not in `CORE_LOCALES`, and `StandardCodes` (`Level.MODERN`) only includes base locale `de` in `currencies_modern_locales.tsv`. **Action**: Add `"de_AT"` to `Dimensions.CORE_LOCALES`. |
| **S1.4** | `locale = "en" \| "de" \| "de_CH" \| "bn"` × `input = -1230.05, 1234565.0` | **Covered** | Already present in `Dimensions.CORE_LOCALES` × `CORE_CURRENCIES` × `CORE_NUMBERS` (`currencies.tsv`). |

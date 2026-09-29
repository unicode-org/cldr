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

## Section 2: Compact Currency `alt="alphaNextToNumber"` Selection (`#compact-currency-alphaNextToNumber`)

* **TR35 Specification Link**: [`tr35-numbers.md#compact-currency-alphaNextToNumber`](../../../docs/ldml/tr35-numbers.md#compact-currency-alphaNextToNumber) (UTS #35 Part 3, Section 2.4.1: *Compact Number Formats*, step 4; L456–L461 at `f18139dfa2`)
* **Related specification text**: L445–L446 (definition of *letter grapheme cluster*), L448 (the examples use the currency symbol "$CA"), L454–L455 (step 3: a pattern of "0" uses non-compact formatting), L481–L489 (the "0" pattern), L553 ([non-compact `alt="alphaNextToNumber"` rule](../../../docs/ldml/tr35-numbers.md#currency-alphaNextToNumber)), and L1010 ([`currencySpacing`](../../../docs/ldml/tr35-numbers.md#currency-spacing), kept for implementations that do not support `alt="alphaNextToNumber"`)

### 2.1 Verbatim Specification Snippet (`docs/ldml/tr35-numbers.md`)

> 4. <a name="compact-currency-alphaNextToNumber"></a>If P is a currency format, look at the currency symbol string, and the position of the currency symbol ¤ in the pattern element value.
> If ¤ is immediately to the left of a 0 and the currency string ends with a _letter grapheme cluster_ (eg, "$CA"),
> or to the right and the currency starts with a letter (eg, "CA$"),
> then switch to the `alt=alphaNextToNumber` pattern, if there is one.
>     * P = `<pattern type="100000" count="**one**" alt="alphaNextToNumber">¤ 000K</pattern>` // with the currency symbol "CA$"
>     * V = "¤ 000K"

---

### 2.2 Sentence-by-Sentence `(Dimension / Value)` Coverage Breakdown

**CLDR data.** Short `currencyFormat` patterns (`latn`, `type="standard"`, `count="other"`), resolved through inheritance:

| Locale | Pattern (`type`) | `alt="alphaNextToNumber"` | Position of `¤` |
| :--- | :--- | :--- | :--- |
| `en` | `¤0K` (1000), `¤0M` (1000000) | `¤ 0K` / `"¤\u00A00K"`, `¤ 0M` / `"¤\u00A00M"` | Left of `0` |
| `ja` | `0` (1000), `¤000万` (1000000) | 1000000: `¤ 000万` / `"¤\u00A0000万"` | Left of `0` (1000000) |
| `zh` | `0` (1000) | `¤ 0K` / `"¤\u00A00K"` | — (the pattern is `0`) |
| `kn` | `¤0ಸಾ` (1000) | `¤ 0ಸಾ` / `"¤\u00A00ಸಾ"` | Left of `0` |
| `id` | `¤0 rb` / `"¤0\u00A0rb"` (1000) | none | Left of `0` |
| `af` | `¤0 k` / `"¤0\u00A0k"` (1000) | none | Left of `0` |
| `bn` | `00 লা¤` / `"00\u00A0লা¤"` (1000000) | `00 লা ¤` / `"00\u00A0লা\u00A0¤"` | After the abbreviation `লা` |
| `km` | `0ពាន់¤` (1000) | `0ពាន់ ¤` / `"0ពាន់\u00A0¤"` | After the abbreviation `ពាន់` |
| `si` | `¤ද0` (1000) | `¤ ද0` / `"¤\u00A0ද0"` | Before the abbreviation `ද` |
| `de` | `0` (1000) | — | — (the pattern is `0`; non-compact `#,##0.00 ¤` / `"#,##0.00\u00A0¤"`) |

`root` has no compact `alt="alphaNextToNumber"` patterns, so `id` and `af`, whose `type="1000"` alt is the inheritance marker `↑↑↑`, have none. A scan of `common/main` (all numbering systems, skipping `draft="provisional"` and `draft="unconfirmed"` values) finds:
* no compact currency pattern with `¤` immediately to the right of a `0`;
* four locales whose compact `alt` patterns put `¤` next to an abbreviation instead of a `0`: `bn`, `kab`, and `km` (after it) and `si` (before it).

Currency strings used below: `en` USD `$`, RUB `RUB`, EGP `symbolNarrow` `E£`, CAD `CA$`, XCG `Cg.` (from `root`); `kn` RON `symbolNarrow` `ಲೀ` (U+0CB2 KANNADA LETTER LA, U+0CC0 KANNADA VOWEL SIGN II, General Category Mc); `id` IDR `Rp`; `af` ZAR `R`; `bn` USD `US$`. For `currency_display = "code"`, the ISO code takes the place of `¤`.

| # | Verbatim Sentence / Normative Clause | Required `(Dimension = Value)` Combinations to Cover Clause | CLDR Data Evidence & Expected Behavior |
| :---: | :--- | :--- | :--- |
| **S2.1** | *"If P is a currency format, look at the currency symbol string, and the position of the currency symbol ¤ in the pattern element value."* | • **S2.1a**: class B where P is a compact pattern (e.g. `locale = "en"`, `input = 1234565.0`)<br>• **S2.1b**: class B where P is `"0"` but has an `alt` (`locale = "zh"`, `input = -1230.05`)<br>• **S2.1c**: compact decimal format (P is not a currency format) | • S2.1a: `en` P = `¤0M`; step 4 applies (S2.2–S2.4).<br>• S2.1b: step 3 (L454) comes first and skips step 4, so the `zh` alt `¤ 0K` / `"¤\u00A00K"` is never used. CNY `code` uses the non-compact pattern `¤#,##0.00`, which the non-compact rule (L553) switches to its alt `¤ #,##0.00` / `"¤\u00A0#,##0.00"`. The result has no `K`, unlike `-CNY 1.2K` / `"-CNY\u00A01.2K"`.<br>• S2.1c: step 4 does not apply. |
| **S2.2** | *"If ¤ is immediately to the left of a 0 and the currency string ends with a letter grapheme cluster (eg, "$CA"), [...] then switch to the `alt=alphaNextToNumber` pattern"* | `locale` with `¤` left of `0` (`en`, `ja`, `kn`), class B, P not `"0"`, and a currency string that ends with:<br>• **S2.2a**: a letter (`code`; `en` RUB `symbol`)<br>• **S2.2b**: a currency sign (`$`, `E£`, `CA$`)<br>• **S2.2c**: another non-letter (`en` XCG `Cg.`)<br>• **S2.2d**: a letter and a combining mark (`kn` RON `symbolNarrow` `ಲೀ`) | • S2.2a: switch. USD `code` 1234565.0: `en` → `USD 1.2M` / `"USD\u00A01.2M"`, `ja` → `USD 123万` / `"USD\u00A0123万"`. `en` RUB `symbol` → `RUB 1.2M` / `"RUB\u00A01.2M"`.<br>• S2.2b: no switch. `en` → `$1.2M`, `E£1.2M`, `-CA$1.2K`.<br>• S2.2c: no switch. `en` XCG −1230.05 → `-Cg.1.2K`.<br>• S2.2d: switch, because `ಲೀ` is a letter grapheme cluster (L445). `kn` RON −1230.05 → `-ಲೀ 1.2ಸಾ` / `"-ಲೀ\u00A01.2ಸಾ"`. |
| **S2.3** | *"...or to the right and the currency starts with a letter (eg, "CA$"),"* | • **S2.3a**: `¤` right of `0` × a currency string that starts with a letter<br>• **S2.3b**: `¤` next to a compact abbreviation, in a locale with an `alt` (`bn`, `km`, `si`) | • S2.3a: no CLDR data.<br>• S2.3b: the text only checks for a `0`, so no switch: `bn` USD `symbol` 1234565.0 → `১২ লাUS$` / `"১২\u00A0লাUS$"`; `km` BDT `code` −1230.05 → `-1.2ពាន់BDT`; `si` KWD `code` −1230.05 → `-KWDද1.2`. The alt patterns would give `১২ লা US$` / `"১২\u00A0লা\u00A0US$"`, `-1.2ពាន់ BDT` / `"-1.2ពាន់\u00A0BDT"`, and `-KWD ද1.2` / `"-KWD\u00A0ද1.2"`. |
| **S2.4** | *"...if there is one."* | • **S2.4a**: the locale has an `alt` (S2.2a)<br>• **S2.4b**: the locale has no `alt` (`id`, `af`) × a currency string that ends with a letter | • S2.4b: no switch. `id` IDR `symbol` −1230.05 → `-Rp1,2 rb` / `"-Rp1,2\u00A0rb"`; `af` ZAR `symbol` −1230.05 → `-R1,2 k` / `"-R1,2\u00A0k"`. |
| **S2.5** | Related text (L485): *"...then the normal number format pattern for that sort of object is supplied [...] with the normal formatting for the locale (such as the grouping separators)."* | • class B where P is `"0"` × a 4-digit `input` × a locale whose `minimumGroupingDigits` is 1 (`de`) | `de` (`minimumGroupingDigits` 1, inherited from `root`) EUR `symbol`: 1500.0 → `1.500 €` / `"1.500\u00A0€"`; 5000.0 → `5.000 €` / `"5.000\u00A0€"`. |

---

### 2.3 Comparison Against `GenerateCurrencyFormatTestData.java` (PR [#5808](https://github.com/unicode-org/cldr/pull/5808))

| Clause | Required `(Dimension = Value)` Combination | Status | Witness Row / Action Required |
| :---: | :--- | :---: | :--- |
| **S2.1a** | Class B × compact P | ✅ **Covered** | W2.2a rows below. |
| **S2.1b** | `locale = "zh"` × class B × `input = -1230.05` | ✅ **Covered** | W2.1b uses the non-compact pattern. Its missing grouping separator is S2.5. |
| **S2.1c** | Compact decimal format | ⚪ **Out of scope** | The generator produces currency formats only. |
| **S2.2a** | `¤` left of `0` × currency string ending with a letter | ✅ **Covered** | W2.2a rows below. |
| **S2.2b** | `¤` left of `0` × currency string ending with a currency sign | ✅ **Covered** | W2.2b rows below. |
| **S2.2c** | `locale = "en"` × `currency = "XCG"` (`Cg.`) × class B | 🔴 **Non-conformant** | W2.2c has a no-break space; step 4 gives `-Cg.1.2K`. The TSV follows the legacy `currencySpacing` rule (see 2.4). **Action**: see the Summary. |
| **S2.2d** | `locale = "kn"` × `currency = "RON"` × `symbolNarrow` × class B | 🟡 **Missing: dimension** | The generator pairs `kn` only with `INR`, `KHR`, `SBD`, and `TWD`, and no other locale with `¤` left of `0` has such a symbol (apart from `lo` GEL `alt="variant"`, which the generator does not use). **Action**: add this combination. |
| **S2.3a** | `¤` right of `0` | ⚪ **Out of scope** | No CLDR data. |
| **S2.3b** | `locale` = `bn`, `km`, `si` × class B | ❓ **Spec unclear** | W2.3b rows follow the text, which never selects these locales' `alt` patterns (see 2.4). |
| **S2.4a** | Locale with an `alt` | ✅ **Covered** | W2.2a rows below. |
| **S2.4b** | `locale` = `id`, `af` × currency string ending with a letter × class B | 🔴 **Non-conformant** | W2.4b rows have a no-break space after the currency string; step 4 gives `-Rp1,2 rb` / `"-Rp1,2\u00A0rb"` and `-R1,2 k` / `"-R1,2\u00A0k"`. The TSV follows the legacy `currencySpacing` rule (see 2.4). **Action**: see the Summary. |
| **S2.5** | `locale = "de"` × class B × `input` = `1500.0`, `5000.0` | 🔴 **Non-conformant** | W2.5 rows have no grouping separator; L485 gives `1.500 €` / `"1.500\u00A0€"`. ICU4J groups only numbers with 5 or more integer digits in compact notation. **Action**: decide whether L485 or the test data changes. |

**Witness rows** at `5e1d4b0ee3` (all with `currency_format_length = "short"` and `currency_format_type = "standard"`):

| Row | File | `locale` | `currency` | `currency_display` | `input` | `expected` | Escaped |
| :---: | :--- | :--- | :--- | :--- | ---: | :--- | :--- |
| W2.1b | `currencies_modern_locales.tsv` | `zh` | `CNY` | `code` | `-1230.05` | `-CNY 1230` | `"-CNY\u00A01230"` |
| W2.2a | `currencies.tsv` | `en` | `USD` | `code` | `1234565.0` | `USD 1.2M` | `"USD\u00A01.2M"` |
| W2.2a | `currencies.tsv` | `en` | `RUB` | `symbol` | `1234565.0` | `RUB 1.2M` | `"RUB\u00A01.2M"` |
| W2.2a | `currencies.tsv` | `ja` | `USD` | `code` | `1234565.0` | `USD 123万` | `"USD\u00A0123万"` |
| W2.2b | `currencies.tsv` | `en` | `USD` | `symbol` | `1234565.0` | `$1.2M` | `"$1.2M"` |
| W2.2b | `currencies.tsv` | `en` | `EGP` | `symbolNarrow` | `1234565.0` | `E£1.2M` | `"E£1.2M"` |
| W2.2b | `currencies_symbol_modern_currencies.tsv` | `en` | `CAD` | `symbol` | `-1230.05` | `-CA$1.2K` | `"-CA$1.2K"` |
| W2.2c | `currencies_symbol_modern_currencies.tsv` | `en` | `XCG` | `symbol` | `-1230.05` | `-Cg. 1.2K` | `"-Cg.\u00A01.2K"` |
| W2.3b | `currencies.tsv` | `bn` | `USD` | `symbol` | `1234565.0` | `১২ লাUS$` | `"১২\u00A0লাUS$"` |
| W2.3b | `currencies_code_modern_currencies.tsv` | `km` | `BDT` | `code` | `-1230.05` | `-1.2ពាន់BDT` | `"-1.2ពាន់BDT"` |
| W2.3b | `currencies_code_modern_currencies.tsv` | `si` | `KWD` | `code` | `-1230.05` | `-KWDද1.2` | `"-KWDද1.2"` |
| W2.4b | `currencies_modern_locales.tsv` | `id` | `IDR` | `symbol` | `-1230.05` | `-Rp 1,2 rb` | `"-Rp\u00A01,2\u00A0rb"` |
| W2.4b | `currencies_modern_locales.tsv` | `af` | `ZAR` | `symbol` | `-1230.05` | `-R 1,2 k` | `"-R\u00A01,2\u00A0k"` |
| W2.5 | `currencies_symbol_extended_numbers.tsv` | `de` | `EUR` | `symbol` | `1500.0` | `1500 €` | `"1500\u00A0€"` |
| W2.5 | `currencies_symbol_extended_numbers.tsv` | `de` | `EUR` | `symbol` | `5000.0` | `5000 €` | `"5000\u00A0€"` |

### 2.4 Notes

* **Legacy `currencySpacing` in the TSV files** (S2.2c, S2.4b): L1010 keeps `currencySpacing` for "implementations that may not yet support the `alt="alphaNextToNumber"` variant". The pinned ICU4J is one of them: its data has the `alphaNextToNumber` patterns (resource key `currencyFormat%alphaNextToNumber`), but no class in the jar references them. The TSV values follow the `root` `currencySpacing` rule instead: insert U+00A0 between the currency string and a digit when the character of the string next to the digit is in `[[:^S:]&[:^Z:]]`. Of the 1,284 `short` rows whose CLDR pattern has `¤` immediately left of a `0` (inputs of magnitude 1000 or more), 1,253 agree with step 4. The other 31 are 4 `XCG` rows (S2.2c), 15 `af` and `id` rows (S2.4b), and 12 `ha` rows (data vintage, below).
* **Data vintage**: the data in the pinned ICU4J predates the Survey Tool import in CLDR-19616 (`03725842ae`, 2026-07-28). For 4 of the 106 locales with `short` rows (`af`, `ha`, `mn`, `ta`), ICU's compact patterns are the ones from before that import, so they differ from `f18139dfa2`. For `ha`, this changes the step 4 result: CLDR now has `¤0K` with no `alt`, so CNY `code` −1230.05 → `-CNY1.2K`, but the TSV has `-CNY 1.2K` / `"-CNY\u00A01.2K"` from ICU's older `¤ 0K` / `"¤\u00A00K"`. This section does not use these rows as witnesses.
* **Spec unclear: abbreviations next to `¤`** (S2.3b): step 4 only checks for a `0` next to `¤`, so the compact `alt` patterns of `bn`, `kab`, `km`, and `si` are never selected, although their only change is a no-break space between `¤` and the abbreviation. The compact and non-compact rules also test different things: step 4 checks for a letter grapheme cluster (L445), while L553 checks the General Category of the character closest to the number. For `kn` RON `ಲೀ`, that character is U+0CC0 (Mc), so step 4 switches and the non-compact rule does not. Section 4 covers the non-compact rule.
* **Editorial issues in the snippet**: L460 says "CA$", but with `¤000K` the symbol "CA$" does not switch (W2.2b: `-CA$1.2K`); L448 and L477 use "$CA". L468 says "N = 123.456" where it means N′. L446 repeats "are".
* **Compact grouping** (S2.5, deferred from Section 1): the same ungrouped 4-digit values appear in the CORE rows for −1230.05 (`ja` USD `-$1230`, `de` EUR `-1230 €` / `"-1230\u00A0€"`, `de_CH` EUR `EUR-1230`) and in W2.1b. For −1230.05, the digits also depend on the precision of the "0" case, which L485 leaves open ("typically to 2 or 3 digits", so `1,200` or `1,230`); W2.5 therefore uses 1500.0 and 5000.0. Section 1's `de_AT` example (`-€ 1230` / `"-€\u00A01230"`) is the same case.
* **`zh`**: its `type="1000"` pattern is `0` but still has an `alt` (`¤ 0K` / `"¤\u00A00K"`), which step 3 makes unreachable (S2.1b). [CLDR-19632](https://unicode-org.atlassian.net/browse/CLDR-19632) audits compact `alphaNextToNumber` data.

---

## Summary: Required Generator Changes

| Change | Needed by |
| :--- | :--- |
| Add one locale with a monetary separator to `CORE_LOCALES` (+300 rows): `"fr_CH"` (recommended: unlike the `de_AT` override, it also shows with approved data only; see 1.2) or `"de_AT"` | S1.1a–c (`fr_CH`) or S1.3a–b (`de_AT`) |
| Add `kn` × `RON` × `symbolNarrow` × `short` (a currency symbol that ends with a combining mark, with `¤` left of `0`) | S2.2d |
| Produce the 19 `short` rows where step 4 and the legacy `currencySpacing` rule differ (`en` and `en_GB` × `XCG`, `af`, `id`) with step 4 instead of ICU4J's output | S2.2c, S2.4b |

One such locale is enough to exercise the monetary separator override. `en` and the other CORE locales already cover the "otherwise" clauses (S1.2, S1.4). The clauses of the locale that is not added stay 🟡 **Missing: `locale`**.

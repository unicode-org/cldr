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
| 🟡 **Missing: combination** | Each needed value is a CORE value of its dimension, but the generator skips this combination of values |
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
| **`cf`** | `cf` key of the Unicode locale identifier, which selects the standard or accounting form ([Unicode Currency Format Identifier](../../../docs/ldml/tr35.md#UnicodeCurrencyFormatIdentifier)) | **Needs to be added** (Section 3): `"standard"`, `"account"` | — |
| **`currency_pattern_append_iso`** | Whether the result is combined with the ISO 4217 code through the locale's `currencyPatternAppendISO` pattern ([Combining Currency Symbols and ISO Codes](../../../docs/ldml/tr35-numbers.md#combining-currency-symbols-and-iso-codes-currencypatternappendiso)) | **Needs to be added** (Section 6): `true`, with `currency_display` = `"symbol"` or `"symbolNarrow"`; the current rows correspond to `false` | — |
| **`fraction_digits`** | Number of fraction digits requested through the API instead of the currency's number of decimals ([Formatting Currency Display Names](../../../docs/ldml/tr35-numbers.md#formatting-currency-display-names-unitpattern), step 5.1) | **Needs to be added** (Section 9): `0`, with `currency_display = "name"`; the current rows use the currency's number of decimals | — |

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

## Section 2: Compact Currency `alt="alphaNextToNumber"` Selection (`#Compact_Number_Formats`, step 4)

* **TR35 Specification Link**: [`tr35-numbers.md#Compact_Number_Formats`](../../../docs/ldml/tr35-numbers.md#Compact_Number_Formats) (UTS #35 Part 3, Section 2.4.1: *Compact Number Formats*, step 4; L473–L478 at `2997bffaf0`)
* **Related specification text**: L462–L463 (definition of *letter grapheme cluster*), L465 (the examples use the currency symbol "$CA"), L471–L472 (step 3: a pattern of "0" uses non-compact formatting), L498–L506 (the "0" pattern), L570 ([non-compact `alt="alphaNextToNumber"` rule](../../../docs/ldml/tr35-numbers.md#the-altalphanexttonumber-pattern-variant)), and L1027 ([`currencySpacing`](../../../docs/ldml/tr35-numbers.md#currency-boundary-spacing-currencyspacing), kept for implementations that do not support `alt="alphaNextToNumber"`)

### 2.1 Verbatim Specification Snippet (`docs/ldml/tr35-numbers.md`)

> 4. If P is a currency format, look at the currency symbol string, and the position of the currency symbol ¤ in the pattern element value.
> If ¤ is immediately to the left of a 0 and the currency string ends with a _letter grapheme cluster_ (eg, "$CA"),
> or to the right and the currency starts with a letter (eg, "CA$"),
> then switch to the `alt=alphaNextToNumber` pattern, if there is one.
>     * P = `<pattern type="100000" count="**one**" alt="alphaNextToNumber">¤ 000K</pattern>` // with the currency symbol "CA$"
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
| `id` | `¤0 rb` / `"¤0\u00A0rb"` (1000), `¤0 jt` / `"¤0\u00A0jt"` (1000000) | none | Left of `0` |
| `af` | `¤0 k` / `"¤0\u00A0k"` (1000) | none | Left of `0` |
| `ha` | `¤0K` (1000), `¤0M` (1000000) | none | Left of `0` |
| `bn` | `0 হা¤` / `"0\u00A0হা¤"` (1000), `00 লা¤` / `"00\u00A0লা¤"` (1000000) | `0 হা ¤` / `"0\u00A0হা\u00A0¤"`, `00 লা ¤` / `"00\u00A0লা\u00A0¤"` | After the abbreviation (`হা`, `লা`) |
| `km` | `0ពាន់¤` (1000) | `0ពាន់ ¤` / `"0ពាន់\u00A0¤"` | After the abbreviation `ពាន់` |
| `si` | `¤ද0` (1000) | `¤ ද0` / `"¤\u00A0ද0"` | Before the abbreviation `ද` |
| `de` | `0` (1000) | — | — (the pattern is `0`; non-compact `#,##0.00 ¤` / `"#,##0.00\u00A0¤"`) |

`root` has no compact `alt="alphaNextToNumber"` patterns, so `id`, `af`, and `ha`, whose `type="1000"` alt is the inheritance marker `↑↑↑`, have none. A scan of `common/main` (all numbering systems, skipping `draft="provisional"` and `draft="unconfirmed"` values) finds:
* no compact currency pattern with `¤` immediately to the right of a `0`;
* four locales whose compact `alt` patterns put `¤` next to an abbreviation instead of a `0`: `bn`, `kab`, and `km` (after it) and `si` (before it).

Currency strings used below: `en` USD `$`, RUB `RUB`, EGP `symbolNarrow` `E£`, CAD `CA$`, XCG `Cg.`, and CHF `symbolNarrow` `Fr.` (the last two from `root`); `kn` RON `symbolNarrow` `ಲೀ` (U+0CB2 KANNADA LETTER LA, U+0CC0 KANNADA VOWEL SIGN II, General Category Mc); `af` ZAR `R`; `bn` USD `US$`. For `currency_display = "code"`, the ISO code takes the place of `¤`.

| # | Verbatim Sentence / Normative Clause | Required `(Dimension = Value)` Combinations to Cover Clause | CLDR Data Evidence & Expected Behavior |
| :---: | :--- | :--- | :--- |
| **S2.1** | *"If P is a currency format, look at the currency symbol string, and the position of the currency symbol ¤ in the pattern element value."* | • **S2.1a**: `currency_format_length = "short"` where P is a compact pattern (e.g. `locale = "en"`, `input = 1234565.0`)<br>• **S2.1b**: `currency_format_length = "short"` where P is `"0"` but has an `alt` (`locale = "zh"`, `input = -1230.05`)<br>• **S2.1c**: compact decimal format (P is not a currency format) | • S2.1a: `en` P = `¤0M`; step 4 applies (S2.2–S2.4).<br>• S2.1b: step 3 (L471) comes first and skips step 4, so the `zh` alt `¤ 0K` / `"¤\u00A00K"` is never used. USD `code` uses the non-compact pattern `¤#,##0.00`, which the non-compact rule (L570) switches to its alt `¤ #,##0.00` / `"¤\u00A0#,##0.00"` (from `root`). The result has no `K`, unlike `-USD 1.2K` / `"-USD\u00A01.2K"`.<br>• S2.1c: step 4 does not apply. |
| **S2.2** | *"If ¤ is immediately to the left of a 0 and the currency string ends with a letter grapheme cluster (eg, "$CA"), [...] then switch to the `alt=alphaNextToNumber` pattern"* | `locale` with `¤` left of `0` (`en`, `ja`, `kn`), `currency_format_length = "short"`, P not `"0"`, and a currency string that ends with:<br>• **S2.2a**: a letter (`code`; `en` RUB `symbol`)<br>• **S2.2b**: a currency sign (`$`, `E£`, `CA$`)<br>• **S2.2c**: another non-letter (`en` XCG `Cg.`, CHF `symbolNarrow` `Fr.`)<br>• **S2.2d**: a letter and a combining mark (`kn` RON `symbolNarrow` `ಲೀ`) | • S2.2a: switch. USD `code` 1234565.0: `en` → `USD 1.2M` / `"USD\u00A01.2M"`, `ja` → `USD 123万` / `"USD\u00A0123万"`. `en` RUB `symbol` → `RUB 1.2M` / `"RUB\u00A01.2M"`.<br>• S2.2b: no switch. `en` → `$1.2M`, `E£1.2M`, `-CA$1.2K`.<br>• S2.2c: no switch. `en` −1230.05: XCG → `-Cg.1.2K`, CHF `symbolNarrow` → `-Fr.1.2K`.<br>• S2.2d: switch, because `ಲೀ` is a letter grapheme cluster (L462). `kn` RON −1230.05 → `-ಲೀ 1.2ಸಾ` / `"-ಲೀ\u00A01.2ಸಾ"`. |
| **S2.3** | *"...or to the right and the currency starts with a letter (eg, "CA$"),"* | • **S2.3a**: `¤` right of `0` × a currency string that starts with a letter<br>• **S2.3b**: `¤` next to a compact abbreviation, in a locale with an `alt` (`bn`, `km`, `si`) | • S2.3a: no CLDR data.<br>• S2.3b: the text only checks for a `0`, so no switch: `bn` USD `symbol` 1234565.0 → `১২ লাUS$` / `"১২\u00A0লাUS$"`; `km` BDT `code` −1230.05 → `-1.2ពាន់BDT`; `si` KWD `code` −1230.05 → `-KWDද1.2`. The alt patterns would give `১২ লা US$` / `"১২\u00A0লা\u00A0US$"`, `-1.2ពាន់ BDT` / `"-1.2ពាន់\u00A0BDT"`, and `-KWD ද1.2` / `"-KWD\u00A0ද1.2"`. |
| **S2.4** | *"...if there is one."* | • **S2.4a**: the locale has an `alt` (S2.2a)<br>• **S2.4b**: the locale has no `alt` (`id`, `af`, `ha`) × a currency string that ends with a letter | • S2.4b: no switch. `id` USD `code` −1230.05 → `-USD1,2 rb` / `"-USD1,2\u00A0rb"`; `af` ZAR `symbol` −1230.05 → `-R1,2 k` / `"-R1,2\u00A0k"`. |
| **S2.5** | Related text (L502): *"...then the normal number format pattern for that sort of object is supplied [...] with the normal formatting for the locale (such as the grouping separators)."* | • `currency_format_length = "short"` where P is `"0"` × `input` of magnitude ≥ 1000 × a locale whose `minimumGroupingDigits` is 1 (`locale = "de"`, `input = -1230.05`) | `de` (`minimumGroupingDigits` 1, inherited from `root`) EUR `symbol` −1230.05 → `-1.200 €` / `"-1.200\u00A0€"` or `-1.230 €` / `"-1.230\u00A0€"`: L502 sets the significant digits "typically to 2 or 3 digits", and both results are grouped. |

---

### 2.3 Comparison Against `GenerateCurrencyFormatTestData.java` (PR [#5808](https://github.com/unicode-org/cldr/pull/5808))

| Clause | Required `(Dimension = Value)` Combination | Status | Generator Evidence / Action Required |
| :---: | :--- | :---: | :--- |
| **S2.1a** | `locale` and `input` where P is a compact pattern × `currency_format_length = "short"` | ✅ **Covered** | CORE values: `en` in `CORE_LOCALES` with `-1230.05` and `1234565.0` in `CORE_NUMBERS` (P = `¤0K` and `¤0M`), and `ja` with `1234565.0` (P = `¤000万`). |
| **S2.1b** | `locale` and `input` where P is `"0"` but has an `alt` × `currency_format_length = "short"` | 🟡 **Missing: `locale` in CORE** | No CORE locale has such a P: `de`, `de_CH`, and `ja` have `"0"` for `type="1000"`, but no `alt`. `zh` is an extended locale value (`currencies_modern_locales.tsv`). **Action**: add `"zh"` to `CORE_LOCALES` (see the Summary). |
| **S2.1c** | Compact decimal format | ⚪ **Out of scope** | The generator produces currency formats only. |
| **S2.2a** | `locale` with `¤` left of `0` × `currency_format_length = "short"` × a currency string that ends with a letter | ✅ **Covered** | CORE values: `en` and `ja` in `CORE_LOCALES`; `currency_display = "code"`, or `"symbol"` with `RUB` or `EGP` from `CORE_CURRENCIES`; `1234565.0` in `CORE_NUMBERS`. |
| **S2.2b** | `locale` with `¤` left of `0` × `currency_format_length = "short"` × a currency string that ends with a currency sign | ✅ **Covered** | CORE values: `en` and `ja`; `"symbol"` with `USD`, `EUR`, or `JPY`, or `"symbolNarrow"` with `EGP` (`E£`); `1234565.0`. |
| **S2.2c** | `locale` with `¤` left of `0` × `currency_format_length = "short"` × a currency string that ends with another non-letter | 🟡 **Missing: `currency` in CORE** | No CORE currency has such a string in `en` or `ja`. `XCG` (`Cg.` in `en`) is an extended currency value (`currencies_symbol_modern_currencies.tsv`, `currencies_narrow_modern_currencies.tsv`). `CHF` (`symbolNarrow` `Fr.`, added by [CLDR-19297](https://unicode-org.atlassian.net/browse/CLDR-19297)) would also work, but the pinned ICU4J does not have that symbol yet. **Action**: add `"XCG"` to `CORE_CURRENCIES` (see the Summary). |
| **S2.2d** | `locale = "kn"` × `currency = "RON"` × `currency_display = "symbolNarrow"` × `currency_format_length = "short"` | 🟡 **Missing: `locale` in CORE**<br>🟡 **Missing: `currency` in CORE** | `kn` and `RON` are extended values, but the generator never combines them: it pairs `kn` only with `INR`, `KHR`, `SBD`, and `TWD`, and `RON` only with `am`, `ar`, `cs`, `de`, `en`, and `ro`. No other locale with `¤` left of `0` has a currency string that ends with a combining mark (apart from `lo` GEL `alt="variant"`, which the generator does not use). **Action**: add `"kn"` to `CORE_LOCALES` and `"RON"` to `CORE_CURRENCIES` (see the Summary). |
| **S2.3a** | `¤` right of `0` | ⚪ **Out of scope** | No CLDR data. |
| **S2.3b** | `locale` with `¤` next to a compact abbreviation and an `alt` × `currency_format_length = "short"` | ✅ **Covered** | CORE values: `bn` in `CORE_LOCALES`, for both `-1230.05` and `1234565.0` (see the table in 2.2). Step 4 never selects these `alt` patterns (see 2.4). |
| **S2.4a** | `locale` with an `alt` | ✅ **Covered** | CORE values: `en` and `ja` (S2.2a). |
| **S2.4b** | `locale` with `¤` left of `0` and no `alt` × `currency_format_length = "short"` × a currency string that ends with a letter | 🟡 **Missing: `locale` in CORE** | No CORE locale has such a P. `id`, `af`, and `ha` are extended locale values (`currencies_modern_locales.tsv`); `cop`, `ee`, `om`, and `quc` are in neither. **Action**: add `"id"` to `CORE_LOCALES` (see the Summary). |
| **S2.5** | `locale` and `input` of magnitude ≥ 1000 where P is `"0"` × `currency_format_length = "short"` | ✅ **Covered** | CORE values: `de` and `de_CH` in `CORE_LOCALES` (`"0"` for `type="1000"`, `minimumGroupingDigits` 1) with `-1230.05` in `CORE_NUMBERS`. |

### 2.4 Notes

* **Abbreviations next to `¤`** (S2.3b): step 4 only checks for a `0` next to `¤`, so the compact `alt` patterns of `bn`, `kab`, `km`, and `si` are never selected, although their only change is a no-break space between `¤` and the abbreviation. The specification does not say whether this is intended.
* **Letter test** (S2.2d): step 4 checks for a letter grapheme cluster (L462), while the non-compact rule (L570) checks the General Category of the character closest to the number. For `kn` RON `ಲೀ`, that character is U+0CC0 (Mc), so step 4 switches and the non-compact rule does not. Section 4 covers the non-compact rule.
* **Editorial issues in the snippet**: L477 says "CA$", but with `¤000K` the symbol "CA$" does not switch (`en` CAD: `-CA$1.2K`); L465 and L494 use "$CA". L485 says "N = 123.456" where it means N′. L463 repeats "are".
* **`zh`**: its `type="1000"` pattern is `0` but still has an `alt` (`¤ 0K` / `"¤\u00A00K"`), which step 3 makes unreachable (S2.1b). [CLDR-19632](https://unicode-org.atlassian.net/browse/CLDR-19632) audits compact `alphaNextToNumber` data.

---

## Section 3: Standard and Accounting Currency Format Types (`#standard-and-accounting-currency-format-types`)

* **TR35 Specification Link**: [`tr35-numbers.md#standard-and-accounting-currency-format-types`](../../../docs/ldml/tr35-numbers.md#standard-and-accounting-currency-format-types) (UTS #35 Part 3, Section 2.4.2: *Currency Formats*; L536–L566 at `2997bffaf0`)
* **Related specification text**: L260 (`plusSign`: the standard number patterns "(except for type="accounting")" contain the `minusSign`), L520–L521 (DTD: the `currencyFormat` `type` is `standard` by default, or `accounting`), L713, L719, L724, and L726 ([Special Pattern Characters](../../../docs/ldml/tr35-numbers.md#Special_Pattern_Characters): a pattern without an explicit negative subpattern gets a prefixed `-`; an explicit negative subpattern is used as is; a `-` in a pattern is replaced by the `minusSign`), and `tr35.md` L1048–L1058 ([Unicode Currency Format Identifier](../../../docs/ldml/tr35.md#UnicodeCurrencyFormatIdentifier): the `cf` values `standard` and `account`)

### 3.1 Verbatim Specification Snippet (`docs/ldml/tr35-numbers.md`)

> ##### Standard and Accounting Currency Format Types
>
> In addition to a standard currency format, in which negative currency amounts might typically be displayed as something like “-$3.27”, locales may provide an "accounting" form, in which for "en_US" the same example would appear as “($3.27)”. The locale keyword "cf" can be used to select the standard or accounting form, see [Unicode Currency Format Identifier](../../../docs/ldml/tr35.md#UnicodeCurrencyFormatIdentifier).
>
> ```xml
> <currencyFormats>
>     <currencyFormatLength>
>         <currencyFormat type="standard">
>             <pattern>¤#,##0.00</pattern>
>             <pattern alt="alphaNextToNumber">¤ #,##0.00</pattern>
>             <pattern alt="noCurrency">#,##0.00</pattern>
>         </currencyFormat>
>         <currencyFormat type="accounting">
>             <pattern>¤#,##0.00;(¤#,##0.00)</pattern>
>             <pattern alt="alphaNextToNumber">¤ #,##0.00;(¤ #,##0.00)</pattern>
>             <pattern alt="noCurrency">#,##0.00;(#,##0.00)</pattern>
>         </currencyFormat>
>     </currencyFormatLength>
>     <currencyFormatLength type="short">
>         <currencyFormat type="standard">
>             <pattern type="1000" count="one">¤0K</pattern>
>             <pattern type="1000" count="one" alt="alphaNextToNumber">¤ 0K</pattern>
>             <pattern type="1000" count="other">¤0K</pattern>
>             <pattern type="1000" count="other" alt="alphaNextToNumber">¤ 0K</pattern>
>             ...
>             <pattern type="100000000000000" count="other">¤000T</pattern>
>             <pattern type="100000000000000" count="other" alt="alphaNextToNumber">¤ 000T</pattern>
>         </currencyFormat>
>     </currencyFormatLength>
> </currencyFormats>
> ```

The link target in the quote is adjusted to the location of this file.

---

### 3.2 Sentence-by-Sentence `(Dimension / Value)` Coverage Breakdown

**CLDR data.** Non-compact `currencyFormat` patterns without `alt`, resolved through inheritance and the `root` aliases. The default numbering system is `latn`, except for `ar_EG` (`arab`) and `bn` (`beng` digits with the `latn` patterns, through a `root` alias). `en_US` has no `currencyFormats` and inherits those of `en`.

| Locale | `type="standard"` | `type="accounting"` |
| :--- | :--- | :--- |
| `root` | `¤ #,##0.00` / `"¤\u00A0#,##0.00"` | `<alias source="locale" path="../currencyFormat[@type='standard']"/>`: the standard pattern |
| `en`, `ja` | `¤#,##0.00` | `¤#,##0.00;(¤#,##0.00)` |
| `bn` | `#,##,##0.00¤` | `#,##,##0.00¤;(#,##,##0.00¤)` |
| `pt_PT` | `#,##0.00 ¤` / `"#,##0.00\u00A0¤"` | `#,##0.00 ¤;(#,##0.00 ¤)` / `"#,##0.00\u00A0¤;(#,##0.00\u00A0¤)"` |
| `fy` | `¤ #,##0.00;¤ #,##0.00-` / `"¤\u00A0#,##0.00;¤\u00A0#,##0.00-"` | `¤ #,##0.00;(¤ #,##0.00)` / `"¤\u00A0#,##0.00;(¤\u00A0#,##0.00)"` |
| `ar` | `‏#,##0.00 ¤;‏-#,##0.00 ¤` / `"\u200F#,##0.00\u00A0¤;\u200F-#,##0.00\u00A0¤"` | `؜#,##0.00¤;(؜#,##0.00¤)` / `"\u061C#,##0.00¤;(\u061C#,##0.00¤)"` |
| `de_CH` | `¤ #,##0.00;¤-#,##0.00` / `"¤\u00A0#,##0.00;¤-#,##0.00"` | none (`root` alias) |
| `de`, `ru` | `#,##0.00 ¤` / `"#,##0.00\u00A0¤"` | none (`root` alias) |
| `ar_EG` | `‏#,##0.00 ¤` / `"\u200F#,##0.00\u00A0¤"` (`arab`, from `ar`) | none (`root` alias) |

Symbols and currency strings used below: the `minusSign` is `-`, except `ar` `‎-` / `"\u200E-"` and `ar_EG` (`arab`) `؜-` / `"\u061C-"`; `ar_EG` decimal `٫` and group `٬`; `de_CH` group `'`; `pt_PT` `minimumGroupingDigits` 2 (no group separator in `1230,05`); `de_CH` EUR `EUR`, `ja` JPY `￥`, `ru` RUB `₽`, and `ar_EG` EGP `ج.م.‏` / `"ج.م.\u200F"`.

| # | Verbatim Sentence / Normative Clause | Required `(Dimension = Value)` Combinations to Cover Clause | CLDR Data Evidence & Expected Behavior |
| :---: | :--- | :--- | :--- |
| **S3.1** | *"In addition to a standard currency format, in which negative currency amounts might typically be displayed as something like “-$3.27”,"* | `currency_format_type = "standard"` × a negative `input` (e.g. `-1230.05`) × a `locale` whose standard pattern has:<br>• **S3.1a**: no explicit negative subpattern (`en`, `ar_EG`)<br>• **S3.1b**: an explicit negative subpattern (`de_CH`, `fy`, `ar`) | • S3.1a: a `-` is prefixed to the pattern and replaced by the `minusSign`. −1230.05: `en` USD → `-$1,230.05` (the example); `ar_EG` EGP → `؜-‏١٬٢٣٠٫٠٥ ج.م.‏` / `"\u061C-\u200F١٬٢٣٠٫٠٥\u00A0ج.م.\u200F"`.<br>• S3.1b: the negative subpattern is used as is, and its `-` is replaced by the `minusSign`. −1230.05: `de_CH` EUR → `EUR-1'230.05`; `fy` EUR → `€ 1.230,05-` / `"€\u00A01.230,05-"`; `ar` EUR → `‏‎-1,230.05 €` / `"\u200F\u200E-1,230.05\u00A0€"`. |
| **S3.2** | *"...locales may provide an "accounting" form, in which for "en_US" the same example would appear as “($3.27)”."* | `currency_format_type = "accounting"` and:<br>• **S3.2a**: `locale = "en"` × `currency = "USD"` × a negative `input`<br>• **S3.2b**: a `locale` with an accounting pattern × `input` ≥ 0 (`en`, `ar`)<br>• **S3.2c**: a `locale` without an accounting pattern × a negative `input` (`de`, `de_CH`, `ru`, `ar_EG`) | • S3.2a: `en` USD −1230.05 → `($1,230.05)`. The other locales with an accounting pattern, −1230.05: `ja` JPY → `(￥1,230)`; `bn` EUR → `(১,২৩০.০৫€)`; `ar` EUR → `(؜1,230.05€)` / `"(\u061C1,230.05€)"`; `fy` EUR → `(€ 1.230,05)` / `"(€\u00A01.230,05)"`; `pt_PT` EUR → `(1230,05 €)` / `"(1230,05\u00A0€)"`.<br>• S3.2b: the positive subpattern of the accounting pattern. `en` USD: 1234565.0 → `$1,234,565.00`, 0.0 → `$0.00`. `ar` EUR 1234565.0 → `؜1,234,565.00€` / `"\u061C1,234,565.00€"`, which differs from the standard `‏1,234,565.00 €` / `"\u200F1,234,565.00\u00A0€"`.<br>• S3.2c: the `root` alias gives the standard pattern. −1230.05: `de` EUR → `-1.230,05 €` / `"-1.230,05\u00A0€"`; `de_CH` EUR → `EUR-1'230.05`; `ru` RUB → `-1 230,05 ₽` / `"-1\u00A0230,05\u00A0₽"`; `ar_EG` EGP → as in S3.1a. |
| **S3.3** | *"The locale keyword "cf" can be used to select the standard or accounting form, see [Unicode Currency Format Identifier](../../../docs/ldml/tr35.md#UnicodeCurrencyFormatIdentifier)."* | `currency_format_type` not set, and:<br>• **S3.3a**: `cf = "account"` (e.g. `locale = "en"`, `currency = "USD"`, `input = -1230.05`)<br>• **S3.3b**: `cf = "standard"` (the same values) | • S3.3a: the accounting form. `en` USD −1230.05 → `($1,230.05)`; `de` EUR −1230.05 → `-1.230,05 €` / `"-1.230,05\u00A0€"` (`de` has no accounting pattern, S3.2c).<br>• S3.3b: the standard form. `en` USD −1230.05 → `-$1,230.05`. |

---

### 3.3 Comparison Against `GenerateCurrencyFormatTestData.java` (PR [#5808](https://github.com/unicode-org/cldr/pull/5808))

| Clause | Required `(Dimension = Value)` Combination | Status | Generator Evidence / Action Required |
| :---: | :--- | :---: | :--- |
| **S3.1a** | `currency_format_type = "standard"` × a negative `input` × a `locale` whose standard pattern has no explicit negative subpattern | ✅ **Covered** | CORE values: `en`, `ar_EG`, `bn`, `de`, `ja`, `pt_PT`, and `ru` in `CORE_LOCALES`; `-1230.05` in `CORE_NUMBERS`. |
| **S3.1b** | `currency_format_type = "standard"` × a negative `input` × a `locale` whose standard pattern has an explicit negative subpattern | ✅ **Covered** | CORE values: `de_CH`, `fy`, and `ar` in `CORE_LOCALES`; `-1230.05`. |
| **S3.2a** | `locale = "en"` × `currency = "USD"` × `currency_format_type = "accounting"` × a negative `input` | ✅ **Covered** | CORE values: `en` in `CORE_LOCALES`, `USD` in `CORE_CURRENCIES`, and `-1230.05` in `CORE_NUMBERS`. `ja`, `bn`, `ar`, `fy`, and `pt_PT` also have their own accounting patterns. |
| **S3.2b** | `locale` with an accounting pattern × `currency_format_type = "accounting"` × `input` ≥ 0 | ✅ **Covered** | CORE values: `en` and `ar`; `0.0`, `1.2`, `0.00831765`, and `1234565.0` in `CORE_NUMBERS`. |
| **S3.2c** | `locale` without an accounting pattern × `currency_format_type = "accounting"` × a negative `input` | ✅ **Covered** | CORE values: `de`, `de_CH`, `ru`, and `ar_EG`; `-1230.05`. |
| **S3.3a–b** | `cf = "account"` or `"standard"` × `currency_format_type` not set | 🟡 **Missing: new dimension** | The generator has no `cf` dimension: `currency_format_type` selects the form, and no `locale` value has a Unicode locale extension (`-u-`). **Action**: add the `cf` dimension (see the dimensions table and the Summary). |

### 3.4 Notes

* **`cf` and `currency_format_type`** (S3.3): the specification does not say which applies when the `cf` keyword and a format type requested through an API disagree. `tr35.md` L1974 only says that "an API or other context" can indicate `type="accounting"`. The proposed `cf` rows leave `currency_format_type` unset.
* **Non-negative amounts** (S3.2b): the `cf` descriptions (`tr35.md` L1057–L1058) mention only negative numbers, but the `ar` accounting pattern also differs for non-negative amounts: U+061C ARABIC LETTER MARK and no space, instead of U+200F RIGHT-TO-LEFT MARK and U+00A0 in the standard pattern. An implementation that uses the accounting pattern only for negative amounts fails S3.2b for `ar`.
* **Compact and long-name formats**: CLDR has no `short` accounting patterns (none in `common/main`, and the example at L554–L564 has only `type="standard"`) and no accounting `unitPattern`, so the generator's skips of `"short"` and `"name"` with `"accounting"` leave out no CLDR data. The specification does not say how to format compact or long-name amounts in the accounting form.
* **`alt` variants in the example**: Section 2 covers the compact `alt="alphaNextToNumber"` patterns, Section 4 the non-compact ones, and Section 5 `alt="noCurrency"` ([`#the-altnocurrency-pattern-variant`](../../../docs/ldml/tr35-numbers.md#the-altnocurrency-pattern-variant)).

---

## Section 4: The `alt="alphaNextToNumber"` Pattern Variant (`#the-altalphanexttonumber-pattern-variant`)

* **TR35 Specification Link**: [`tr35-numbers.md#the-altalphanexttonumber-pattern-variant`](../../../docs/ldml/tr35-numbers.md#the-altalphanexttonumber-pattern-variant) (UTS #35 Part 3, Section 2.4.2: *Currency Formats*; L568–L570 at `2997bffaf0`)
* **Related specification text**: L525 (since CLDR 42, the `alt="alphaNextToNumber"` variant is the preferred way to place the currency symbol), L540–L552 (the example gives both the standard and the accounting pattern an `alt="alphaNextToNumber"` variant; see Section 3), L462–L463 and L473–L478 ([compact step 4](../../../docs/ldml/tr35-numbers.md#Compact_Number_Formats), which tests for a *letter grapheme cluster*; see Section 2), L1027 ([`currencySpacing`](../../../docs/ldml/tr35-numbers.md#currency-boundary-spacing-currencyspacing), kept for implementations that do not support `alt="alphaNextToNumber"`), and `tr35.md` L1966, L1982, and L2078 ([Lateral Inheritance](../../../docs/ldml/tr35.md#Lateral_Inheritance): an `alt` falls back to the path without `alt` within the same locale, before the parent locale; the inheritance marker `↑↑↑` is equivalent to an absent value)

### 4.1 Verbatim Specification Snippet (`docs/ldml/tr35-numbers.md`)

> ##### The `alt="alphaNextToNumber"` Pattern Variant
>
> The `alt="alphaNextToNumber"` pattern, if available, should be used instead of the standard pattern when the currency symbol character closest to the numeric value has Unicode General Category L (letter). The `alt="alphaNextToNumber"` pattern is typically provided when the standard currency pattern does not have a space between currency symbol and numeric value; the alphaNextToNumber variant adds a non-breaking space if appropriate for the locale.

---

### 4.2 Sentence-by-Sentence `(Dimension / Value)` Coverage Breakdown

**CLDR data.** Non-compact patterns and their `alt="alphaNextToNumber"` variants, resolved with CLDR's resolver, which looks for the `alt` path in every locale up to `root` before it drops the `alt` (see 4.4 for `ja` and `kn`):

| Locale | Pattern | `alt="alphaNextToNumber"` | Space between `¤` and the number |
| :--- | :--- | :--- | :--- |
| `root` | `¤ #,##0.00` / `"¤\u00A0#,##0.00"` | The same | Yes |
| `en` | Standard `¤#,##0.00`; accounting `¤#,##0.00;(¤#,##0.00)` | Standard `¤ #,##0.00` / `"¤\u00A0#,##0.00"`; accounting `¤ #,##0.00;(¤ #,##0.00)` / `"¤\u00A0#,##0.00;(¤\u00A0#,##0.00)"` | Pattern: no; `alt`: yes |
| `ja`, `kn` | Standard `¤#,##0.00`; accounting `¤#,##0.00;(¤#,##0.00)` | Standard: `↑↑↑`, resolved to `root`'s `¤ #,##0.00` / `"¤\u00A0#,##0.00"`; accounting: their own `¤ #,##0.00;(¤ #,##0.00)` / `"¤\u00A0#,##0.00;(¤\u00A0#,##0.00)"` | Pattern: no; `alt`: yes |
| `bn` | Standard `#,##,##0.00¤`; accounting `#,##,##0.00¤;(#,##,##0.00¤)` | Standard `#,##,##0.00 ¤` / `"#,##,##0.00\u00A0¤"`; accounting `#,##,##0.00 ¤;(#,##,##0.00 ¤)` / `"#,##,##0.00\u00A0¤;(#,##,##0.00\u00A0¤)"` | Pattern: no; `alt`: yes |
| `ar` | Standard `‏#,##0.00 ¤;‏-#,##0.00 ¤` / `"\u200F#,##0.00\u00A0¤;\u200F-#,##0.00\u00A0¤"`; accounting `؜#,##0.00¤;(؜#,##0.00¤)` / `"\u061C#,##0.00¤;(\u061C#,##0.00¤)"` | Standard: the same; accounting `؜#,##0.00 ¤;(؜#,##0.00 ¤)` / `"\u061C#,##0.00\u00A0¤;(\u061C#,##0.00\u00A0¤)"` | Standard: yes; accounting: pattern no, `alt` yes |
| `ar_EG` (`arab`) | `‏#,##0.00 ¤` / `"\u200F#,##0.00\u00A0¤"` (both types) | None: neither `ar` nor `root` has one for `arab` | Yes |
| `de`, `ru`, `pt_PT` | `#,##0.00 ¤` / `"#,##0.00\u00A0¤"` (`pt_PT` accounting: the same in parentheses for negative amounts) | The same | Yes |
| `de_CH` | `¤ #,##0.00;¤-#,##0.00` / `"¤\u00A0#,##0.00;¤-#,##0.00"` | The same | Positive: yes; negative: `¤` is next to the `-` |
| `fy` | Standard `¤ #,##0.00;¤ #,##0.00-` / `"¤\u00A0#,##0.00;¤\u00A0#,##0.00-"` | The same | Yes |
| `en_ZA` | Standard `¤#,##0.00` and accounting `¤#,##0.00;(¤#,##0.00)`, both from `en` | Standard: its own `¤#,##0.00`; accounting: `en`'s `¤ #,##0.00;(¤ #,##0.00)` / `"¤\u00A0#,##0.00;(¤\u00A0#,##0.00)"` | Standard: no, and the `alt` adds none; accounting: `alt` yes |

A scan of `common/main` (skipping `draft="provisional"` and `draft="unconfirmed"` values) finds:
* one locale whose standard `alt` keeps `¤` next to the number: `en_ZA`;
* 30 locales with no `alt` for their default numbering system, all `arab` (`ar_EG` and 20 other `ar` locales, plus `ckb`, `sd`, `sdh`, and their 6 sublocales), and all with a space between `¤` and the number;
* no locale with `¤` next to the number and no `alt`.

Currency strings used below: `en` USD `$`, JPY `¥`, RUB `RUB` (`symbol`) and `₽` (`symbolNarrow`), EGP `EGP` (`symbol`) and `E£` (`symbolNarrow`), and XCG `Cg.` (from `root`); `bn` and `ar` USD `US$`; `ar` EGP `ج.م.‏` / `"ج.م.\u200F"`; `kn` RON `symbolNarrow` `ಲೀ` (U+0CB2 KANNADA LETTER LA, U+0CC0 KANNADA VOWEL SIGN II, General Category Mc); `en_ZA` ZAR `R`, with decimal `,` and group U+00A0. For `currency_display = "code"`, the ISO code takes the place of `¤`. The character closest to the number is the last character of the currency string when `¤` is before the number, and the first one when `¤` is after it.

| # | Verbatim Sentence / Normative Clause | Required `(Dimension = Value)` Combinations to Cover Clause | CLDR Data Evidence & Expected Behavior |
| :---: | :--- | :--- | :--- |
| **S4.1** | *"The `alt="alphaNextToNumber"` pattern [...] should be used instead of the standard pattern when the currency symbol character closest to the numeric value has Unicode General Category L (letter)."* | `currency_format_length = ""` × a `locale` whose pattern has no space between `¤` and the number × a currency string whose character closest to the number is:<br>• **S4.1a**: a letter, with `¤` before the number (`en` × `code`; `en` × `RUB` or `EGP` × `symbol`)<br>• **S4.1b**: a letter, with `¤` after the number (`bn` × `USD` × `symbol`; `ar` × `"accounting"` × `USD` or `EGP` × `symbol`)<br>• **S4.1c**: a currency sign, General Category Sc (`$`, `¥`, `€`, `₽`, `E£`)<br>• **S4.1d**: another non-letter (`en` × `XCG` × `symbol`: `Cg.` ends with U+002E, General Category Po)<br>• **S4.1e**: a combining mark (`kn` × `RON` × `symbolNarrow`: `ಲೀ` ends with U+0CC0, General Category Mc) | • S4.1a: switch. `en` −1230.05: USD `code` → `-USD 1,230.05` / `"-USD\u00A01,230.05"`; RUB → `-RUB 1,230.05` / `"-RUB\u00A01,230.05"`; EGP → `-EGP 1,230.05` / `"-EGP\u00A01,230.05"`. USD `code` 1234565.0 → `USD 1,234,565.00` / `"USD\u00A01,234,565.00"`.<br>• S4.1b: switch. −1230.05: `bn` USD → `-১,২৩০.০৫ US$` / `"-১,২৩০.০৫\u00A0US$"`; `ar` accounting USD → `(؜1,230.05 US$)` / `"(\u061C1,230.05\u00A0US$)"`, EGP → `(؜1,230.05 ج.م.‏)` / `"(\u061C1,230.05\u00A0ج.م.\u200F)"`.<br>• S4.1c: no switch. −1230.05: `en` → `-$1,230.05`, `-¥1,230`, `-₽1,230.05`, `-E£1,230.05`; `bn` EUR → `-১,২৩০.০৫€`; `ar` accounting EUR → `(؜1,230.05€)` / `"(\u061C1,230.05€)"`.<br>• S4.1d: no switch. `en` XCG −1230.05 → `-Cg.1,230.05`.<br>• S4.1e: no switch, although `ಲೀ` is a letter grapheme cluster (compact step 4 switches, S2.2d). `kn` RON −1230.05 → `-ಲೀ1,230.05`. |
| **S4.2** | *"...if available..."* | • **S4.2a**: the `locale` has its own `alt` (`en`, `bn`; `ar` accounting)<br>• **S4.2b**: the `locale` has its own pattern, but `↑↑↑` for the `alt` (`ja`, `kn` standard) × a currency string that ends with a letter<br>• **S4.2c**: neither the `locale` nor its parents have an `alt` (`ar_EG`) × a currency string that starts with a letter | • S4.2a: see S4.1a–b.<br>• S4.2b: `ja` USD `code` −1230.05 → `-USD 1,230.05` / `"-USD\u00A01,230.05"` with the `alt` that CLDR's resolver takes from `root`, or `-USD1,230.05` with the lateral inheritance of TR35 Part 1 (see 4.4).<br>• S4.2c: the pattern is used. `ar_EG` USD `code` −1230.05 → `؜-‏١٬٢٣٠٫٠٥ USD` / `"\u061C-\u200F١٬٢٣٠٫٠٥\u00A0USD"`. |
| **S4.3** | *"...instead of the standard pattern..."* | `currency_format_type = "accounting"` × a `locale` with an accounting `alt` × a currency string that ends with a letter (`en` × `code`) | The example (L548–L552) gives the accounting pattern its own `alt`, so "the standard pattern" means the pattern without `alt`, of either type. `en` accounting USD `code` −1230.05 → `(USD 1,230.05)` / `"(USD\u00A01,230.05)"`; the same in `ja`, from its own accounting `alt`. |
| **S4.4** | *"The `alt="alphaNextToNumber"` pattern is typically provided when the standard currency pattern does not have a space between currency symbol and numeric value; the alphaNextToNumber variant adds a non-breaking space if appropriate for the locale."* | • **S4.4a**: an `alt` that adds U+00A0 (S4.1a–b)<br>• **S4.4b**: an `alt` that adds no space (`locale = "en_ZA"`, standard) × a currency string that ends with a letter<br>• **S4.4c**: a pattern that already has a space, with the same `alt` (`de`, `de_CH`, `fy`, `pt_PT`, `ru`; `ar` standard) | • S4.4a: see S4.1a–b.<br>• S4.4b: the `alt` is used as is. `en_ZA` −1230.05: ZAR `symbol` → `-R1 230,05` / `"-R1\u00A0230,05"`; USD `code` → `-USD1 230,05` / `"-USD1\u00A0230,05"`.<br>• S4.4c: no change. USD `code` −1230.05: `de` → `-1.230,05 USD` / `"-1.230,05\u00A0USD"`; `de_CH` → `USD-1'230.05`; `fy` → `USD 1.230,05-` / `"USD\u00A01.230,05-"`. `ar` USD `symbol` −1230.05 → `‏‎-1,230.05 US$` / `"\u200F\u200E-1,230.05\u00A0US$"`. |

---

### 4.3 Comparison Against `GenerateCurrencyFormatTestData.java` (PR [#5808](https://github.com/unicode-org/cldr/pull/5808))

| Clause | Required `(Dimension = Value)` Combination | Status | Generator Evidence / Action Required |
| :---: | :--- | :---: | :--- |
| **S4.1a** | `locale` with `¤` right before the number × `currency_format_length = ""` × a currency string that ends with a letter | ✅ **Covered** | CORE values: `en` and `ja` in `CORE_LOCALES`; `currency_display = "code"`, or `"symbol"` with `RUB` or `EGP` from `CORE_CURRENCIES`; `-1230.05` and `1234565.0` in `CORE_NUMBERS`. |
| **S4.1b** | `locale` with `¤` right after the number × `currency_format_length = ""` × a currency string that starts with a letter | ✅ **Covered** | CORE values: `bn` (both types) and `ar` (`"accounting"`); `"code"`, or `"symbol"` with `USD` (`US$`); `-1230.05`. |
| **S4.1c** | `locale` with `¤` next to the number × `currency_format_length = ""` × a currency string whose closest character is a currency sign | ✅ **Covered** | CORE values: `en` and `ja` with `"symbol"` and `USD`, `EUR`, or `JPY`, or with `"symbolNarrow"` and `RUB` (`₽`) or `EGP` (`E£`); `bn` and `ar` (`"accounting"`) with `EUR`. |
| **S4.1d** | `locale` with `¤` next to the number × `currency_format_length = ""` × a currency string whose closest character is another non-letter | 🟡 **Missing: `currency` in CORE** | No CORE currency has such a string in a CORE locale with `¤` next to the number. `XCG` (`Cg.` in `en`) is an extended currency value, as in S2.2c; `CHF` (`symbolNarrow` `Fr.`) would also work. **Action**: add `"XCG"` to `CORE_CURRENCIES` (see the Summary). |
| **S4.1e** | `locale = "kn"` × `currency = "RON"` × `currency_display = "symbolNarrow"` × `currency_format_length = ""` | 🟡 **Missing: `locale` in CORE**<br>🟡 **Missing: `currency` in CORE** | As in S2.2d: `kn` and `RON` are extended values, but the generator never combines them. No other CLDR currency string ends with a combining mark in a locale with `¤` next to the number. **Action**: add `"kn"` to `CORE_LOCALES` and `"RON"` to `CORE_CURRENCIES` (see the Summary). |
| **S4.2a** | `locale` with its own `alt` | ✅ **Covered** | CORE values: `en`, `bn`, and `ar` (S4.1a–b). |
| **S4.2b** | `locale` with its own pattern and `↑↑↑` for the `alt` × a currency string that ends with a letter | ✅ **Covered** | CORE values: `ja` with `"code"`; `-1230.05` and `1234565.0`. See 4.4 for the expected value. |
| **S4.2c** | `locale` without an `alt` × a currency string that starts with a letter | ✅ **Covered** | CORE values: `ar_EG` with `"code"`; `-1230.05`. |
| **S4.3** | `locale` with an accounting `alt` × `currency_format_type = "accounting"` × a currency string that ends with a letter | ✅ **Covered** | CORE values: `en` and `ja` with `"accounting"` and `"code"`; `ar` (S4.1b). |
| **S4.4a** | `locale` whose `alt` adds U+00A0 | ✅ **Covered** | CORE values: as in S4.1a–b. |
| **S4.4b** | `locale = "en_ZA"` × `currency_format_length = ""` × `currency_format_type = "standard"` × a currency string that ends with a letter | 🟡 **Missing: `locale`** | `en_ZA` is in neither `CORE_LOCALES` nor the extended locales (no TSV file has `en_ZA` rows), and it is the only locale whose `alt` keeps `¤` next to the number. The other values are CORE values (`"code"`, or `"symbol"` with `RUB` or `EGP`; `-1230.05`). **Action**: add `"en_ZA"` to `CORE_LOCALES` (see the Summary). |
| **S4.4c** | `locale` whose pattern already has a space | ✅ **Covered** | CORE values: `de`, `de_CH`, `fy`, `pt_PT`, `ru`, and `ar` (`"standard"`). |

### 4.4 Notes

* **Inheritance of the `alt`** (S4.2b): `ja` and `kn` have their own standard pattern `¤#,##0.00`, but `↑↑↑` for its `alt`. The two readings give different results:
  * CLDR's resolver ([`XMLSource.java`](../../../tools/cldr-code/src/main/java/org/unicode/cldr/util/XMLSource.java): "alts are special; they act like there is a root alias to the path without the alt") tries the `alt` path in every parent locale first, so they get `root`'s `¤ #,##0.00` / `"¤\u00A0#,##0.00"`: `ja` USD `code` −1230.05 → `-USD 1,230.05` / `"-USD\u00A01,230.05"`.
  * TR35 Part 1 falls back to the path without `alt` "within the same locale, before inheriting from the parent" (`tr35.md` L1966, L1982), and `↑↑↑` is equivalent to an absent value (L2078). That gives their own `¤#,##0.00`: `-USD1,230.05`.

  The same applies to 193 locales with `¤` right before the number whose `alt` comes only from `root`, such as `ko`, `id`, and `fil` (skipping `draft="provisional"` and `draft="unconfirmed"` values). For 40 locales with `¤` right after the number, such as `agq` and `kab`, whose own `alt` is `draft="provisional"`, the resolver's result (`root`'s `¤ #,##0.00`) would move `¤` to the other side of the number.
* **`en_ZA` accounting** (S4.4b): `en_ZA` has no accounting `alt` of its own, so it inherits `en`'s, which adds a space: ZAR accounting −1230.05 → `(R 1 230,05)` / `"(R\u00A01\u00A0230,05)"`, while the standard form gives `-R1 230,05` / `"-R1\u00A0230,05"`.
* **Letter test** (S4.1e): compact step 4 tests for a letter grapheme cluster (L462), while this rule tests the General Category of the character closest to the number, so `kn` RON `ಲೀ` switches in compact formats (S2.2d) but not in non-compact ones.

---

## Section 5: The `alt="noCurrency"` Pattern Variant (`#the-altnocurrency-pattern-variant`)

* **TR35 Specification Link**: [`tr35-numbers.md#the-altnocurrency-pattern-variant`](../../../docs/ldml/tr35-numbers.md#the-altnocurrency-pattern-variant) (UTS #35 Part 3, Section 2.4.2: *Currency Formats*; L572–L574 at `2997bffaf0`)
* **Related specification text**: L546 and L551 (the example's `alt="noCurrency"` patterns `#,##0.00` and `#,##0.00;(#,##0.00)`; see Section 3), L415–L429 (a `<decimalFormatLength type="short">` example), L493–L495 and L498–L506 (the precision of compact formats, and the special value `"0"`), L720 ([`¤` row](../../../docs/ldml/tr35-numbers.md#Number_Pattern_Character_Definitions): the monetary separators are used "if present in a pattern"; see Section 1), L1064 ([Currency Codes and Currency Amounts](../../../docs/ldml/tr35-numbers.md#currency-codes-and-currency-amounts): the number of decimal places and the rounding of each currency override the pattern), and `tr35.md` L1966, L1982, and L2078 ([Lateral Inheritance](../../../docs/ldml/tr35.md#Lateral_Inheritance); see Section 4)

### 5.1 Verbatim Specification Snippet (`docs/ldml/tr35-numbers.md`)

> ##### The `alt="noCurrency"` Pattern Variant
>
> The `alt="noCurrency"` pattern can be used when a currency-style format is desired but without the currency symbol. This sort of display may be used when formatting a large column of values all in the same currency, for example. For compact currency formats (`<currencyFormatLength type="short">`), the compact decimal format (`<decimalFormatLength type="short">`) should be used if no `alt="noCurrency"` pattern is present (so the `alt="noCurrency"` pattern is typically not needed for compact currency formats).

---

### 5.2 Sentence-by-Sentence `(Dimension / Value)` Coverage Breakdown

**CLDR data.** Non-compact `alt="noCurrency"` patterns, resolved with CLDR's resolver, which looks for the `alt` path in every locale up to `root`, then follows the `root` aliases, and only then drops the `alt` (see 4.4 and 5.4). The patterns without `alt` are in 3.2 and 4.2.

| Locale | `type="standard"` | `type="accounting"` |
| :--- | :--- | :--- |
| `root` | `#,##0.00` (for `latn` and `arab`) | `<alias source="locale" path="../currencyFormat[@type='standard']"/>`: the standard one |
| `en` | Its own `#,##0.00` | Its own `#,##0.00;(#,##0.00)` |
| `bn` | Its own `#,##,##0.00` | Its own `#,##,##0.00;(#,##,##0.00)` |
| `ar` | Its own `‏#,##0.00;‏-#,##0.00` / `"\u200F#,##0.00;\u200F-#,##0.00"` | Its own `؜#,##0.00;(؜#,##0.00)` / `"\u061C#,##0.00;(\u061C#,##0.00)"` |
| `fy` | Its own `#,##0.00;#,##0.00-` | None, although `fy` has an accounting pattern: the standard one, through the `root` alias |
| `ja`, `pt_PT` | None: `root`'s `#,##0.00` | Its own `#,##0.00;(#,##0.00)` |
| `de`, `de_CH`, `ru` | None: `root`'s `#,##0.00` | None: `root`'s `#,##0.00` |
| `ar_EG` (`arab`) | None: `root`'s `#,##0.00` for `arab` | None: the same |

A scan of `common/main` (skipping `draft="provisional"` and `draft="unconfirmed"` values) finds:
* 127 locales with their own non-compact `alt="noCurrency"` pattern, and no compact (`<currencyFormatLength type="short">`) `alt="noCurrency"` pattern;
* no locale whose resolved `alt="noCurrency"` pattern has a `¤`.

Symbols used below (see 3.2): the `minusSign` is `-`, except `ar` `‎-` / `"\u200E-"` and `ar_EG` (`arab`) `؜-` / `"\u061C-"`; `ar_EG` decimal `٫` and group `٬`; `bn` `beng` digits; `de_CH` group `'`; `ru` group U+00A0; `pt_PT` `minimumGroupingDigits` 2. JPY has 0 decimal places.

| # | Verbatim Sentence / Normative Clause | Required `(Dimension = Value)` Combinations to Cover Clause | CLDR Data Evidence & Expected Behavior |
| :---: | :--- | :--- | :--- |
| **S5.1** | *"The `alt="noCurrency"` pattern can be used when a currency-style format is desired but without the currency symbol."* | `currency_display = "noCurrency"` × `currency_format_length = ""` and:<br>• **S5.1a**: a `locale` with its own `alt="noCurrency"` pattern × `currency_format_type = "standard"` (`en`, `bn`, `ar`, `fy`)<br>• **S5.1b**: a `locale` with its own accounting `alt="noCurrency"` pattern × `currency_format_type = "accounting"` × a negative `input` (`en`, `ja`, `bn`, `ar`, `pt_PT`)<br>• **S5.1c**: a `locale` whose `alt="noCurrency"` pattern comes from `root`, while its pattern without `alt` does not (`de`, `de_CH`, `ru`, `ar_EG`; `ja` and `pt_PT` standard)<br>• **S5.1d**: `locale = "fy"` × `currency_format_type = "accounting"` × a negative `input`: an accounting pattern, but no accounting `alt="noCurrency"` pattern<br>• **S5.1e**: a `currency` with 0 decimal places (`JPY`) | • S5.1a: the pattern is used, without a currency symbol. −1230.05: `en` → `-1,230.05`; `ar` → `‏‎-1,230.05` / `"\u200F\u200E-1,230.05"`; `fy` → `1.230,05-`. `bn` 1234565.0 → `১২,৩৪,৫৬৫.০০`, with the grouping of its pattern `#,##,##0.00¤`.<br>• S5.1b: −1230.05: `en` USD → `(1,230.05)`; `ja` JPY → `(1,230)`; `bn` → `(১,২৩০.০৫)`; `ar` → `(؜1,230.05)` / `"(\u061C1,230.05)"`; `pt_PT` → `(1230,05)`.<br>• S5.1c: `root`'s `#,##0.00`. −1230.05: `de` → `-1.230,05`; `de_CH` → `-1'230.05`; `ru` → `-1 230,05` / `"-1\u00A0230,05"`; `ar_EG` → `؜-١٬٢٣٠٫٠٥` / `"\u061C-١٬٢٣٠٫٠٥"`. The lateral inheritance of TR35 Part 1 would give results with the currency symbol (see 5.4).<br>• S5.1d: the resolver reaches `fy`'s standard `alt="noCurrency"` pattern through the `root` alias from the accounting to the standard format: −1230.05 → `1.230,05-`, without the parentheses of the accounting pattern (see 5.4).<br>• S5.1e: the currency's decimal places replace those of the pattern (L1064). `en` JPY: 1234565.0 → `1,234,565`; −1230.05 → `-1,230`. |
| **S5.2** | *"This sort of display may be used when formatting a large column of values all in the same currency, for example."* | — | An example of use. |
| **S5.3** | *"For compact currency formats (`<currencyFormatLength type="short">`), the compact decimal format (`<decimalFormatLength type="short">`) should be used if no `alt="noCurrency"` pattern is present (so the `alt="noCurrency"` pattern is typically not needed for compact currency formats)."* | `currency_display = "noCurrency"` × `currency_format_length = "short"` and:<br>• **S5.3a**: a `locale` without a compact `alt="noCurrency"` pattern (every locale) × an `input` whose compact pattern is not `"0"` (`en` × `1234565.0` or `-1230.05`; `fy`, `ja`, `ru`, or `bn` × `1234565.0`)<br>• **S5.3b**: a `locale` with a compact `alt="noCurrency"` pattern | • S5.3a: the compact decimal pattern is used. `en`: 1234565.0 → `1.2M`; −1230.05 → `-1.2K`. 1234565.0: `fy` → `1,2 mln.` / `"1,2\u00A0mln."`; `ja` → `123万`; `ru` → `1,2 млн` / `"1,2\u00A0млн"`; `bn` → `১২ লা` / `"১২\u00A0লা"`. For `fy`, removing `¤` from the compact currency pattern `¤ 0M` / `"¤\u00A00M"` would give `1,2M` instead. The number of digits depends on the precision settings (L493–L495); these examples round to an integer but keep at least 2 significant digits, as in Section 2.<br>• S5.3b: no CLDR data. |

---

### 5.3 Comparison Against `GenerateCurrencyFormatTestData.java` (PR [#5808](https://github.com/unicode-org/cldr/pull/5808))

| Clause | Required `(Dimension = Value)` Combination | Status | Generator Evidence / Action Required |
| :---: | :--- | :---: | :--- |
| **S5.1a** | `locale` with its own `alt="noCurrency"` pattern × `currency_display = "noCurrency"` × `currency_format_length = ""` × `currency_format_type = "standard"` | ✅ **Covered** | CORE values: `en`, `bn`, `ar`, and `fy` in `CORE_LOCALES`; `"noCurrency"`; `-1230.05` and `1234565.0` in `CORE_NUMBERS`. |
| **S5.1b** | `locale` with its own accounting `alt="noCurrency"` pattern × `"noCurrency"` × `currency_format_type = "accounting"` × a negative `input` | ✅ **Covered** | CORE values: `en`, `ja`, `bn`, `ar`, and `pt_PT`; `-1230.05`. |
| **S5.1c** | `locale` whose `alt="noCurrency"` pattern comes from `root` × `"noCurrency"` | ✅ **Covered** | CORE values: `de`, `de_CH`, `ru`, and `ar_EG` (both types), and `ja` and `pt_PT` (`"standard"`). See 5.4 for the expected value. |
| **S5.1d** | `locale = "fy"` × `"noCurrency"` × `"accounting"` × a negative `input` | ✅ **Covered** | CORE values: `fy`; `-1230.05`. See 5.4 for the expected value. |
| **S5.1e** | `currency = "JPY"` × `"noCurrency"` | ✅ **Covered** | CORE values: `JPY` in `CORE_CURRENCIES`; `1234565.0` and `-1230.05`. |
| **S5.2** | — | ⚪ **Out of scope** | An example of use; nothing to test. |
| **S5.3a** | `currency_format_length = "short"` × `currency_display = "noCurrency"` × an `input` whose compact pattern is not `"0"` | 🟡 **Missing: combination** | `"short"` and `"noCurrency"` are CORE values, but the generator skips this combination (see the dimensions table), although the sentence specifies the result: the compact decimal format. The other values are CORE values (`en`, `fy`, `ja`, `ru`, `bn`; `1234565.0` and `-1230.05`). **Action**: generate `"short"` × `"noCurrency"` (see the Summary). |
| **S5.3b** | `locale` with a compact `alt="noCurrency"` pattern | ⚪ **Out of scope** | No CLDR data. |

### 5.4 Notes

* **Inheritance of the `alt`** (S5.1c): the two readings of 4.4 differ here in whether the currency symbol appears.
  * CLDR's resolver tries the `alt="noCurrency"` path in every parent locale first, so `de` gets `root`'s `#,##0.00`: EUR −1230.05 → `-1.230,05`.
  * TR35 Part 1 falls back to the path without `alt` within the same locale, before the parent locale (`tr35.md` L1966, L1982), so `de` gets its own `#,##0.00 ¤` / `"#,##0.00\u00A0¤"`: `-1.230,05 €` / `"-1.230,05\u00A0€"`, with the currency symbol that S5.1 excludes.

  With the resolver, no `alt="noCurrency"` pattern has a `¤`. With lateral inheritance, 626 locales would get a pattern with `¤` for `type="standard"`, and 393 for `type="accounting"` (skipping `draft="provisional"` and `draft="unconfirmed"` values), such as `ja` (JPY −1230.05 → `-￥1,230`) and `ar_EG` (EGP −1230.05 → `؜-‏١٬٢٣٠٫٠٥ ج.م.‏` / `"\u061C-\u200F١٬٢٣٠٫٠٥\u00A0ج.م.\u200F"`).
* **Accounting without its own `alt`** (S5.1d): the resolver follows the `root` alias from the accounting to the standard `currencyFormat` before it drops the `alt` ([`XMLSource.java`](../../../tools/cldr-code/src/main/java/org/unicode/cldr/util/XMLSource.java)), so `fy` gets its standard `alt="noCurrency"` pattern `#,##0.00;#,##0.00-`, with a trailing minus instead of the parentheses of its accounting pattern `¤ #,##0.00;(¤ #,##0.00)` / `"¤\u00A0#,##0.00;(¤\u00A0#,##0.00)"`. 60 locales, such as `fy`, `es_UY`, and `haw`, have parentheses in their accounting pattern but not in their resolved accounting `alt="noCurrency"` pattern. Lateral inheritance gives the accounting pattern itself, with the currency symbol: `(€ 1.230,05)` / `"(€\u00A01.230,05)"`.
* **Other differences from the pattern without `alt`**: the resolved `alt="noCurrency"` pattern can also differ in:
  * grouping, in 14 locales: `en_IN` has its own `¤#,##,##0.00`, but gets `en`'s `alt="noCurrency"` pattern `#,##0.00`, so INR 1234565.0 → `₹12,34,565.00` with the symbol and `1,234,565.00` without it; `dv` has the opposite difference;
  * bidi marks, in the 21 `ar` locales whose default numbering system is `arab`, such as `ar_EG`: `root`'s `#,##0.00` has no U+200F RIGHT-TO-LEFT MARK, unlike their pattern `‏#,##0.00 ¤` / `"\u200F#,##0.00\u00A0¤"` from `ar`. The `ar` `alt="noCurrency"` pattern for `arab`, `‏#,##0.00` / `"\u200F#,##0.00"`, is `draft="provisional"`.

  The specification does not say how closely an `alt="noCurrency"` pattern must follow the pattern without `alt`.
* **Monetary separators** (S1.1d, S1.3c–d): "currency-style" could include `currencyDecimal` and `currencyGroup`, but the `¤` row (L720) uses them only "if present in a pattern", and `alt="noCurrency"` patterns have no `¤` ([CLDR-19825](https://unicode-org.atlassian.net/browse/CLDR-19825)).

---

## Section 6: Combining Currency Symbols and ISO Codes (`#combining-currency-symbols-and-iso-codes-currencypatternappendiso`)

* **TR35 Specification Link**: [`tr35-numbers.md#combining-currency-symbols-and-iso-codes-currencypatternappendiso`](../../../docs/ldml/tr35-numbers.md#combining-currency-symbols-and-iso-codes-currencypatternappendiso) (UTS #35 Part 3, Section 2.4.2: *Currency Formats*; L576–L585 at `2997bffaf0`)
* **Related specification text**: L516 and L522 (DTD: `currencyPatternAppendISO` is a child of `currencyFormats`, next to the `currencyFormatLength` elements), and L720 ([`¤` row](../../../docs/ldml/tr35-numbers.md#Number_Pattern_Character_Definitions): `¤` is the standard currency symbol, `¤¤` the ISO currency symbol, and `¤¤¤¤¤` the narrow currency symbol, which "may be ambiguous")

### 6.1 Verbatim Specification Snippet (`docs/ldml/tr35-numbers.md`)

> ##### Combining Currency Symbols and ISO Codes (`currencyPatternAppendISO`)
>
> ```xml
> <currencyPatternAppendISO>{0} ¤¤</currencyPatternAppendISO>
> ```
>
> The `currencyPatternAppendISO` element provides a pattern that can be used to combine currency format that uses a currency symbol (¤ or ¤¤¤¤¤) with the ISO 4217 3-letter code for the same currency (¤¤), to produce a result such as “$1,432.00 USD”. Using such a format is only recommended to resolve ambiguity when:
> * The currency symbol being used is the narrow symbol (¤¤¤¤¤) or has the same value as the narrow symbol, and
> * The currency symbol does not have the same value as the ISO 4217 3-letter code.
> Most locales will not need to override the pattern provided in root, shown in the xml sample above.

---

### 6.2 Sentence-by-Sentence `(Dimension / Value)` Coverage Breakdown

**CLDR data.** `currencyPatternAppendISO` values in `common/main` (skipping `↑↑↑`, `draft="provisional"`, and `draft="unconfirmed"` values):

| Locale | `currencyPatternAppendISO` | Difference from `root` |
| :--- | :--- | :--- |
| `root` | `{0} ¤¤` / `"{0}\u00A0¤¤"` | — (the XML sample at L579 has the same value) |
| `ar` and 16 other locales: `as`, `brx`, `cs`, `eu`, `gl`, `gu`, `ig`, `ml`, `ne`, `pa`, `ps`, `si`, `sq`, `tk`, `yo`, `yo_BJ` | `{0} ¤¤` | U+0020 SPACE instead of U+00A0 |
| `eo`, `vec` | `{0} ¤¤` / `"{0}\u202F¤¤"` | U+202F NARROW NO-BREAK SPACE instead of U+00A0 |
| `hi` | `¤¤ {0}` | The ISO code comes first, with U+0020 SPACE |

The other CORE locales use `root`'s pattern; `ar_EG` inherits the pattern of `ar`.

Currency strings used below: `en` USD `$`, EUR `€`, RUB `RUB` (`symbol`) and `₽` (`symbolNarrow`); `de` EUR `€`; `ar` USD `US$` (`symbol` and `symbolNarrow`), EGP `ج.م.‏` / `"ج.م.\u200F"` (`symbol`) and `E£` (`symbolNarrow`); `hi` USD `$`. The ISO code replaces `¤¤`.

| # | Verbatim Sentence / Normative Clause | Required `(Dimension = Value)` Combinations to Cover Clause | CLDR Data Evidence & Expected Behavior |
| :---: | :--- | :--- | :--- |
| **S6.1** | *"The `currencyPatternAppendISO` element provides a pattern that can be used to combine currency format that uses a currency symbol (¤ or ¤¤¤¤¤) with the ISO 4217 3-letter code for the same currency (¤¤), to produce a result such as “$1,432.00 USD”."* | `currency_pattern_append_iso = true` and:<br>• **S6.1a**: `currency_display = "symbol"` (`¤`) (e.g. `locale = "en"`, `currency = "USD"`, `input = 1.2`)<br>• **S6.1b**: `currency_display = "symbolNarrow"` (`¤¤¤¤¤`) × a currency whose narrow symbol differs from its symbol (`en` × `RUB`)<br>• **S6.1c**: a result with a minus sign, parentheses, or `¤` after the number (`input = -1230.05`; `currency_format_type = "accounting"`; `locale = "de"`)<br>• **S6.1d**: `currency_format_length = "short"` | • S6.1a: `{0}` is replaced by the result of the currency format, and `¤¤` by the ISO code. `en` USD 1.2 → `$1.20 USD` / `"$1.20\u00A0USD"`.<br>• S6.1b: `en` RUB −1230.05 → `-₽1,230.05 RUB` / `"-₽1,230.05\u00A0RUB"`.<br>• S6.1c: `{0}` is the whole result, including the minus sign or the parentheses. −1230.05: `en` accounting USD → `($1,230.05) USD` / `"($1,230.05)\u00A0USD"`; `de` EUR → `-1.230,05 € EUR` / `"-1.230,05\u00A0€\u00A0EUR"`.<br>• S6.1d: `en` USD −1230.05 → `-$1.2K USD` / `"-$1.2K\u00A0USD"`. |
| **S6.2** | *"Using such a format is only recommended to resolve ambiguity when:<br>• The currency symbol being used is the narrow symbol (¤¤¤¤¤) or has the same value as the narrow symbol, and<br>• The currency symbol does not have the same value as the ISO 4217 3-letter code."* | `currency_pattern_append_iso = true` × a currency string that:<br>• **S6.2a**: is the narrow symbol and differs from the ISO code (`en` × `RUB` × `symbolNarrow`: `₽`)<br>• **S6.2b**: is the symbol, equals the narrow symbol, and differs from the ISO code (`en` × `USD` × `symbol`: `$`)<br>• **S6.2c**: differs from the narrow symbol (`ar` × `EGP` × `symbol`: `ج.م.‏` / `"ج.م.\u200F"`, narrow `E£`) or equals the ISO code (`en` × `RUB` × `symbol`: `RUB`) | • S6.2a–b: the format is recommended; see S6.1a–b.<br>• S6.2c: the format is not recommended. The recommendation does not change the pattern, so if the format is used anyway, −1230.05: `ar` EGP → `‏‎-1,230.05 ج.م.‏ EGP` / `"\u200F\u200E-1,230.05\u00A0ج.م.\u200F EGP"`; `en` RUB → `-RUB 1,230.05 RUB` / `"-RUB\u00A01,230.05\u00A0RUB"`. |
| **S6.3** | *"Most locales will not need to override the pattern provided in root, shown in the xml sample above."* | `currency_pattern_append_iso = true` × a `locale` that:<br>• **S6.3a**: uses `root`'s pattern (`en`, `de`)<br>• **S6.3b**: has its own pattern with another space (`ar`)<br>• **S6.3c**: has its own pattern that puts the ISO code first (`hi`) | • S6.3a: see S6.1.<br>• S6.3b: `ar` USD −1230.05 → `‏‎-1,230.05 US$ USD` / `"\u200F\u200E-1,230.05\u00A0US$ USD"`, with U+0020 before `USD`.<br>• S6.3c: `hi` USD −1230.05 → `USD -$1,230.05`. |

---

### 6.3 Comparison Against `GenerateCurrencyFormatTestData.java` (PR [#5808](https://github.com/unicode-org/cldr/pull/5808))

| Clause | Required `(Dimension = Value)` Combination | Status | Generator Evidence / Action Required |
| :---: | :--- | :---: | :--- |
| **S6.1a–d** | `currency_pattern_append_iso = true` × `currency_display = "symbol"` or `"symbolNarrow"` | 🟡 **Missing: new dimension** | The generator has no dimension that combines the result with the ISO code. The other values are CORE values (`en` and `de` in `CORE_LOCALES`; `USD` and `RUB` in `CORE_CURRENCIES`; `"accounting"`; `"short"`; `1.2` and `-1230.05` in `CORE_NUMBERS`). **Action**: add the `currency_pattern_append_iso` dimension (see the dimensions table and the Summary). |
| **S6.2a–c** | `currency_pattern_append_iso = true` × a currency string for which the format is or is not recommended | 🟡 **Missing: new dimension** | As S6.1a–d. The other values are CORE values (`en` and `ar`; `USD`, `RUB`, and `EGP`; `-1230.05`). |
| **S6.3a–b** | `currency_pattern_append_iso = true` × `locale` = `en`, `de`, or `ar` | 🟡 **Missing: new dimension** | As S6.1a–d. `en`, `de`, and `ar` are CORE values. |
| **S6.3c** | `currency_pattern_append_iso = true` × `locale = "hi"` | 🟡 **Missing: new dimension** | As S6.1a–d. `hi` is an extended locale value (`currencies_modern_locales.tsv`), and the only locale whose pattern puts the ISO code first. **Action**: include `"hi"` in the rows of the new dimension (see the Summary). |

### 6.4 Notes

* **The space in the example** (S6.1): the example “$1,432.00 USD” (L582) has a U+0020 SPACE, but the XML sample (L579) and `root` have U+00A0 NO-BREAK SPACE, which gives `$1,432.00 USD` / `"$1,432.00\u00A0USD"`.
* **A recommendation** (S6.2): the two conditions tell the caller when to use the format. The specification does not define an option that applies them, so the expected result of every row is the pattern's result, whether or not the format is recommended for its currency string.
* **Displays and lengths** (S6.1): the snippet combines only `¤` and `¤¤¤¤¤` with the ISO code, so the proposed rows leave out `"code"` and `"name"`. It does not say whether the pattern also applies to compact formats; S6.1d assumes it does, because `currencyPatternAppendISO` is a child of `currencyFormats` and not of a `currencyFormatLength` (L516).
* **Editorial**: the last sentence (L585) follows the list without a blank line, so Markdown renders it as part of the second list item, as in the quote above.

---

## Section 7: Currency Symbol Placeholders (`#Number_Pattern_Character_Definitions`, `¤` row)

* **TR35 Specification Link**: [`tr35-numbers.md#Number_Pattern_Character_Definitions`](../../../docs/ldml/tr35-numbers.md#Number_Pattern_Character_Definitions) (UTS #35 Part 3, Section 3.2: *Special Pattern Characters*; the `¤` row of the table *Number Pattern Character Definitions*, L720 at `2997bffaf0`, quoted below with the header row of the table, L706–L707)
* **Related specification text**: L702 (invalid sequences such as “¤¤¤¤¤¤” are handled as described in [Handling Invalid Patterns](../../../docs/ldml/tr35.md#Invalid_Patterns)), L712 and L714 (the pattern characters `.` and `,` are replaced by `currencyDecimal` and `currencyGroup`; see Section 1), L576–L585 ([`currencyPatternAppendISO`](../../../docs/ldml/tr35-numbers.md#combining-currency-symbols-and-iso-codes-currencypatternappendiso), whose pattern has `¤¤`; see Section 6), L732–L738 ([placement of the placeholder](../../../docs/ldml/tr35-numbers.md#Special_Pattern_Characters)), and L978–L1021 ([Formatting Currency Display Names](../../../docs/ldml/tr35-numbers.md#formatting-currency-display-names-unitpattern))

### 7.1 Verbatim Specification Snippet (`docs/ldml/tr35-numbers.md`)

> | Symbol | Location | Localized Replacement | Meaning |
> | :-- | :-- | :-- | :-- |
> | ¤ (U+00A4) | Prefix or suffix | _currency symbol/name from currency specified in API_ | Any sequence is replaced by the localized currency symbol for the currency being formatted, as in the table below. If present in a pattern, the monetary decimal separator and grouping separators (if available) are used instead of the numeric ones. If data is unavailable for a given sequence in a given locale, the display may fall back to ¤ or ¤¤. See also the formatting for currency display names, steps 2 and 4 in [Currencies](../../../docs/ldml/tr35-numbers.md#Currencies). <table><tr><th>No.</th><th>Replacement / Example</th></tr><tr><td rowspan="2">¤</td><td>Standard currency symbol</td></tr><tr><td>_C$12.00_</td></tr><tr><td rowspan="2">¤¤</td><td>ISO currency symbol (constant)</td></tr><tr><td>_CAD 12.00_</td></tr><tr><td rowspan="2">¤¤¤</td><td>Appropriate currency display name for the currency, based on the plural rules in effect for the locale</td></tr><tr><td>_5.00 Canadian dollars_</td></tr><tr><td rowspan="2" >¤¤¤¤¤</td><td>Narrow currency symbol. The same symbols may be used for multiple currencies. Thus the symbol may be ambiguous, and should only be where the context is clear.</td></tr><tr><td>_$12.00_</td></tr><tr><td>_others_</td><td>_Invalid in current CLDR. Reserved for future specification_</td></tr></table> |

---

### 7.2 Sentence-by-Sentence `(Dimension / Value)` Coverage Breakdown

**CLDR data.** The currency patterns in `common/main` contain a single `¤`. The only other sequence in CLDR data is `¤¤`, in `currencyPatternAppendISO` (`root` and 20 locales; see Section 6); no CLDR data has `¤¤¤`, `¤¤¤¤`, `¤¤¤¤¤`, or a longer sequence. The generator chooses what replaces `¤` with `currency_display`:

| Placeholder | `currency_display` | Replacement in CLDR data |
| :--- | :--- | :--- |
| `¤` | `"symbol"` | The `<symbol>` of the `<currency>` element |
| `¤¤` | `"code"` | The ISO 4217 code: the `type` of the `<currency>` element |
| `¤¤¤` | `"name"` | A `<displayName count="…">`, placed with a `unitPattern` ([Formatting Currency Display Names](../../../docs/ldml/tr35-numbers.md#formatting-currency-display-names-unitpattern)) |
| `¤¤¤¤¤` | `"symbolNarrow"` | The `<symbol alt="narrow">` |

Currency strings of the CORE values, resolved through the locale chain (skipping `↑↑↑`, `draft="provisional"`, and `draft="unconfirmed"` values):
* **Symbol**: 35 of the 50 pairs of `CORE_LOCALES` and `CORE_CURRENCIES` have a `<symbol>`, such as `en` USD `$` and RUB `RUB` (both `en`'s own), `ru` RUB `₽`, and `ar` EGP `ج.م.‏` / `"ج.م.\u200F"`. The other 15 have none: RUB in `ar`, `ar_EG`, `bn`, `de`, `de_CH`, `fy`, `ja`, and `pt_PT`, and EGP in `bn`, `de`, `de_CH`, `fy`, `ja`, `pt_PT`, and `ru`.
* **Narrow symbol**: `root` has one for every CORE currency (USD `$`, EUR `€`, JPY `¥`, RUB `₽`, EGP `E£`), and `ar` (USD `US$`, JPY `JP¥`), `ja` (JPY `￥`), and `de_CH` (EUR `EUR`) have their own. The narrow symbol differs from the symbol for `en` RUB (`₽` and `RUB`), and for USD (`$` and `US$`) and JPY (`¥` and `JP¥`) in `bn`, `fy`, and `pt_PT`. 57 of the 148 extended currencies have no narrow symbol in `en` or `root`, such as `AED` and `XCG` (`root` has only the symbol `Cg.` for `XCG`).

| # | Verbatim Sentence / Normative Clause | Required `(Dimension = Value)` Combinations to Cover Clause | CLDR Data Evidence & Expected Behavior |
| :---: | :--- | :--- | :--- |
| **S7.1** | *"Prefix or suffix"* and *"currency symbol/name from currency specified in API"* (the Location and Localized Replacement cells) | `currency` (the currency specified in the API) × a `locale` with `¤` in the prefix (`en`) and one with `¤` in the suffix (`de`) | `en` USD 1.2 → `$1.20`; `de` EUR 1.2 → `1,20 €` / `"1,20\u00A0€"`. |
| **S7.2** | *"Any sequence is replaced by the localized currency symbol for the currency being formatted, as in the table below."*, and the table:<br>• ¤: *"Standard currency symbol"*, *"C$12.00"*<br>• ¤¤: *"ISO currency symbol (constant)"*, *"CAD 12.00"*<br>• ¤¤¤: *"Appropriate currency display name for the currency, based on the plural rules in effect for the locale"*, *"5.00 Canadian dollars"*<br>• ¤¤¤¤¤: *"Narrow currency symbol."*, *"$12.00"*<br>• *others*: *"Invalid in current CLDR. Reserved for future specification"* | • **S7.2a**: `currency_display = "symbol"` (e.g. `locale = "en"`, `currency = "USD"`, `input = 1.2`)<br>• **S7.2b**: `currency_display = "code"` (the same values)<br>• **S7.2c**: `currency_display = "name"` × inputs in different plural categories (`de` × `JPY` × `1.2` and `1234565.0`)<br>• **S7.2d**: `currency_display = "symbolNarrow"` × a currency whose narrow symbol differs from its symbol (`en` × `RUB`)<br>• **S7.2e**: a pattern with `¤¤¤¤` or more than five `¤` | • S7.2a: `en` USD 1.2 → `$1.20`.<br>• S7.2b: `en` USD 1.2 → `USD 1.20` / `"USD\u00A01.20"`: the code ends with a letter, so the `alt="alphaNextToNumber"` pattern `¤ #,##0.00` / `"¤\u00A0#,##0.00"` is used (Section 4).<br>• S7.2c: `de` JPY 1.2 → `1 Japanischer Yen` (`one`); 1234565.0 → `1.234.565 Japanische Yen` (`other`).<br>• S7.2d: `en` RUB 1.2 → `₽1.20`, where `¤` gives `RUB 1.20` / `"RUB\u00A01.20"`.<br>• S7.2e: no CLDR data.<br>See 7.4 for the examples of the table. |
| **S7.3** | *"If present in a pattern, the monetary decimal separator and grouping separators (if available) are used instead of the numeric ones."* | • **S7.3a**: a `locale` with `<currencyDecimal>` or `<currencyGroup>` (`fr_CH`, `de_AT`) × any `¤` display: the pattern has a `¤`<br>• **S7.3b**: a `locale` without them × any `¤` display<br>• **S7.3c**: `locale = "fr_CH"` × `currency_display = "name"`, whose pattern (the decimal pattern) has no `¤`<br>• **S7.3d**: `locale = "de_AT"` × `currency_display = "name"`; `fr_CH` or `de_AT` × `currency_display = "noCurrency"`: patterns without `¤` | • S7.3a: see S1.1a–b and S1.3a–b; `fr_CH` EUR −1230.05 → `-1'230.05 €` / `"-1'230.05\u00A0€"`.<br>• S7.3b: see S1.2 and S1.4.<br>• S7.3c: L1015 (step 5 of the display name steps) requires `currencyDecimal`, although the pattern has no `¤`: `-1'230.05 euros` (S1.1c).<br>• S7.3d: this sentence excludes the monetary separators, L288 and L292 apply them to all currency formatting, and L1015 mentions only `currencyDecimal` (S1.1d, S1.3c–d). |
| **S7.4** | *"If data is unavailable for a given sequence in a given locale, the display may fall back to ¤ or ¤¤."* | • **S7.4a**: `currency_display = "symbol"` × a `currency` without a symbol in the locale chain (`de` × `RUB`)<br>• **S7.4b**: `currency_display = "symbolNarrow"` × a `currency` without a narrow symbol (`de` × `XCG`)<br>• **S7.4c**: `currency_display = "name"` × a `currency` without a display name (`fy` × `XCG`) | • S7.4a: the ISO code (`¤¤`): `de` RUB 1.2 → `1,20 RUB` / `"1,20\u00A0RUB"`. `en` has its own symbol `RUB`, so `en` RUB is not a fallback.<br>• S7.4b: the symbol (`¤`) from `root`: `de` XCG 1.2 → `1,20 Cg.` / `"1,20\u00A0Cg."` (see 7.4).<br>• S7.4c: the ISO code (`¤¤`), as step 4.4 of [Formatting Currency Display Names](../../../docs/ldml/tr35-numbers.md#formatting-currency-display-names-unitpattern) says: `fy` XCG 1.2 → `1,20 XCG`. |
| **S7.5** | *"See also the formatting for currency display names, steps 2 and 4 in [Currencies](../../../docs/ldml/tr35-numbers.md#Currencies)."* | — | A cross-reference: step 2 chooses the plural category, and step 4 the display name, with its fallback to the ISO code (S7.4c). |
| **S7.6** | *"The same symbols may be used for multiple currencies. Thus the symbol may be ambiguous, and should only be where the context is clear."* (the `¤¤¤¤¤` cell) | — | Advice on when to use the narrow symbol. In `root`, `$` is the narrow symbol of USD, CAD, and other dollars. |

---

### 7.3 Comparison Against `GenerateCurrencyFormatTestData.java` (PR [#5808](https://github.com/unicode-org/cldr/pull/5808))

| Clause | Required `(Dimension = Value)` Combination | Status | Generator Evidence / Action Required |
| :---: | :--- | :---: | :--- |
| **S7.1** | `currency` × `¤` before and after the number | ✅ **Covered** | CORE values: `USD` and `EUR` in `CORE_CURRENCIES`; `en` and `de` in `CORE_LOCALES`. |
| **S7.2a** | `currency_display = "symbol"` | ✅ **Covered** | CORE values: `"symbol"`; `en`; `USD`; `1.2` in `CORE_NUMBERS`. |
| **S7.2b** | `currency_display = "code"` | ✅ **Covered** | CORE values: `"code"`; `en`; `USD`. |
| **S7.2c** | `currency_display = "name"` × inputs in different plural categories | ✅ **Covered** | CORE values: `"name"`; `de`; `JPY`; `1.2` and `1234565.0`. |
| **S7.2d** | `currency_display = "symbolNarrow"` × a currency whose narrow symbol differs from its symbol | ✅ **Covered** | CORE values: `en` × `RUB`; `bn`, `fy`, and `pt_PT` × `USD` and `JPY`. |
| **S7.2e** | A pattern with an invalid sequence of `¤` | ⚪ **Out of scope** | No CLDR data. |
| **S7.3a** | `locale = "fr_CH"` or `"de_AT"` × any `¤` display | 🟡 **Missing: `locale`** | As S1.1a–b and S1.3a–b. **Action**: the `fr_CH` row of the Summary. |
| **S7.3b** | A `locale` without monetary separators × any `¤` display | ✅ **Covered** | As S1.2 and S1.4. |
| **S7.3c** | `locale = "fr_CH"` × `currency_display = "name"` | 🟡 **Missing: `locale`** | As S1.1c. |
| **S7.3d** | `locale = "de_AT"` × `"name"`; `fr_CH` or `de_AT` × `"noCurrency"` | ❓ **Spec unclear** | [CLDR-19825](https://unicode-org.atlassian.net/browse/CLDR-19825) (as S1.1d and S1.3c–d). |
| **S7.4a** | `currency_display = "symbol"` × a currency without a symbol | ✅ **Covered** | CORE values: 15 pairs of `CORE_LOCALES` and `CORE_CURRENCIES`, such as `de` × `RUB`. |
| **S7.4b** | `currency_display = "symbolNarrow"` × a currency without a narrow symbol | 🟡 **Missing: `currency` in CORE** | Every CORE currency has a narrow symbol in `root`. `XCG` and `AED` are extended currency values, combined only with `TINY_LOCALES` and one or two related locales (`currencies_narrow_modern_currencies.tsv`). **Action**: the `XCG` row of the Summary covers it: no CORE locale has a narrow symbol for `XCG`. |
| **S7.4c** | `currency_display = "name"` × a currency without a display name | 🟡 **Missing: `currency` in CORE** | Every CORE locale has a display name for every CORE currency. `XCG` is an extended currency value, and the locales that it is combined with (`en`, `ar`, `de`, `en_GB`) have a display name for it. **Action**: the `XCG` row of the Summary covers it (`fy` × `XCG`). |
| **S7.5** | — | ⚪ **Out of scope** | A cross-reference; nothing to test. |
| **S7.6** | — | ⚪ **Out of scope** | Advice on usage; nothing to test. |

### 7.4 Notes

* **The examples of the table** (S7.2a–d): they use CAD, an extended currency value. `en` has the CAD symbol `CA$`, so `¤` gives `CA$12.00`; `C$` is the CAD symbol of `fy` and `nl`, which put it before the number with a U+00A0 NO-BREAK SPACE: `C$ 12,00` / `"C$\u00A012,00"`. For the other rows, `en` gives `CAD 12.00` / `"CAD\u00A012.00"` (with U+00A0 instead of the example's U+0020 SPACE; see S7.2b), `5.00 Canadian dollars`, and `$12.00`.
* **Fallback of the narrow symbol** (S7.4b): the sentence allows either `¤` or `¤¤`. CLDR's inheritance gives `¤`: a missing `alt="narrow"` symbol falls back to the symbol without `alt` ([Lateral Inheritance](../../../docs/ldml/tr35.md#Lateral_Inheritance)), and a currency without either gets the ISO code: `de` AED 1.2 → `1,20 AED` / `"1,20\u00A0AED"`.
* **Monetary separators** (S7.3): the condition "If present in a pattern" is not in L288 and L292, and L1015 requires `currencyDecimal` for display names, whose pattern has no `¤` ([CLDR-19825](https://unicode-org.atlassian.net/browse/CLDR-19825)).
* **Editorial**: the `¤¤¤¤¤` cell says "should only be where the context is clear", without "used".

---

## Section 8: Placement of the Currency Symbol Placeholder (`#Special_Pattern_Characters`)

* **TR35 Specification Link**: [`tr35-numbers.md#Special_Pattern_Characters`](../../../docs/ldml/tr35-numbers.md#Special_Pattern_Characters) (UTS #35 Part 3, Section 3.2: *Special Pattern Characters*; L732–L738 at `2997bffaf0`)
* **Related specification text**: L719 and L724–L728 (a pattern has a positive subpattern and may have a negative one; without one, the negative subpattern is the positive one with a `-` prefix), L720 ([`¤` row](../../../docs/ldml/tr35-numbers.md#Number_Pattern_Character_Definitions); see Section 7), L572–L574 ([`alt="noCurrency"`](../../../docs/ldml/tr35-numbers.md#the-altnocurrency-pattern-variant), patterns without a placeholder; see Section 5), and L1052–L1054 ([Currency-Specific Decimal and Grouping Overrides](../../../docs/ldml/tr35-numbers.md#currency-specific-decimal-and-grouping-overrides): a currency can have its own decimal separator)

### 8.1 Verbatim Specification Snippet (`docs/ldml/tr35-numbers.md`)

> A currency decimal pattern normally contains a currency symbol placeholder (¤, ¤¤, ¤¤¤, or ¤¤¤¤¤). The currency symbol placeholder may occur before the first digit, after the last digit symbol, or where the decimal symbol would otherwise be placed (for formats such as "12€50", as in "12€50 pour une omelette").
>
> | Placement | Examples                                                                         |
> |-----------|----------------------------------------------------------------------------------|
> | Before    | "¤#,##0.00" "¤ #,##0.00" "¤-#,##0.00" "¤ -#,##0.00" "-¤#,##0.00" "-¤ #,##0.00" … |
> | After     | "#,##0.00¤" "#,##0.00 ¤" "#,##0.00-¤" "#,##0.00- ¤" "#,##0.00¤-" "#,##0.00 ¤-" … |
> | Decimal   | "#,##0¤00"                                                                       |

---

### 8.2 Sentence-by-Sentence `(Dimension / Value)` Coverage Breakdown

**CLDR data.** The resolved non-compact patterns of the CORE locales, for their default numbering system (see Sections 3 and 4):

| Locale | `type="standard"` | `type="accounting"` | `alt="alphaNextToNumber"` |
| :--- | :--- | :--- | :--- |
| `en`, `ja` | `¤#,##0.00` | `¤#,##0.00;(¤#,##0.00)` | `¤ #,##0.00` / `"¤\u00A0#,##0.00"` and `¤ #,##0.00;(¤ #,##0.00)` / `"¤\u00A0#,##0.00;(¤\u00A0#,##0.00)"` |
| `de_CH` | `¤ #,##0.00;¤-#,##0.00` / `"¤\u00A0#,##0.00;¤-#,##0.00"` | The same as the standard pattern | The same |
| `fy` | `¤ #,##0.00;¤ #,##0.00-` / `"¤\u00A0#,##0.00;¤\u00A0#,##0.00-"` | `¤ #,##0.00;(¤ #,##0.00)` / `"¤\u00A0#,##0.00;(¤\u00A0#,##0.00)"` | The same |
| `de`, `ru` | `#,##0.00 ¤` / `"#,##0.00\u00A0¤"` | The same as the standard pattern | The same |
| `pt_PT` | `#,##0.00 ¤` / `"#,##0.00\u00A0¤"` | `#,##0.00 ¤;(#,##0.00 ¤)` / `"#,##0.00\u00A0¤;(#,##0.00\u00A0¤)"` | The same |
| `bn` | `#,##,##0.00¤` | `#,##,##0.00¤;(#,##,##0.00¤)` | `#,##,##0.00 ¤` / `"#,##,##0.00\u00A0¤"` and `#,##,##0.00 ¤;(#,##,##0.00 ¤)` / `"#,##,##0.00\u00A0¤;(#,##,##0.00\u00A0¤)"` |
| `ar` | `‏#,##0.00 ¤;‏-#,##0.00 ¤` / `"\u200F#,##0.00\u00A0¤;\u200F-#,##0.00\u00A0¤"` | `؜#,##0.00¤;(؜#,##0.00¤)` / `"\u061C#,##0.00¤;(\u061C#,##0.00¤)"` | Standard: the same; accounting: `؜#,##0.00 ¤;(؜#,##0.00 ¤)` / `"\u061C#,##0.00\u00A0¤;(\u061C#,##0.00\u00A0¤)"` |
| `ar_EG` (`arab`) | `‏#,##0.00 ¤` / `"\u200F#,##0.00\u00A0¤"` | The same as the standard pattern | The same |

A scan of the resolved patterns of every locale in `common/main` (the positive and negative subpatterns of the standard and accounting patterns and of their `alt="alphaNextToNumber"` variants, for the default numbering system; bidi marks are ignored, and any space character counts as a space) finds the examples of the snippet in these locales:

| Example (L736–L738) | CORE locales | Extended locales | Other locales |
| :--- | :--- | :--- | :--- |
| `"¤#,##0.00"` | `en`, `ja` | 44, such as `af` and `en_GB` | 324 |
| `"¤ #,##0.00"` | `de_CH`, `fy`; `en` and `ja` (`alt="alphaNextToNumber"`) | 55 | 731 |
| `"¤-#,##0.00"` | `de_CH` (negative subpattern) | `lo` | 10, such as `es_CL` and `it_CH` |
| `"¤ -#,##0.00"` | — | `nl` | 13, such as `es_PY` and `nl_BE` |
| `"-¤#,##0.00"` | `en`, `ja` (implicit negative subpattern) | 40 | 302 |
| `"-¤ #,##0.00"` | `en`, `ja` (`alt="alphaNextToNumber"`) | 51 | 700 |
| `"#,##0.00¤"` | `bn`; `ar` (accounting) | `km`, `uz` | 56 |
| `"#,##0.00 ¤"` | `ar`, `ar_EG`, `de`, `pt_PT`, `ru`; `bn` (`alt="alphaNextToNumber"`) | 45 | 332 |
| `"#,##0.00-¤"`, `"#,##0.00- ¤"`, `"#,##0.00¤-"`, `"#,##0.00 ¤-"` | — | — | — |
| `"#,##0¤00"` | — | — | — |

No pattern, compact or not, has `¤` between digits. Every non-compact currency pattern has a `¤`, except the `alt="noCurrency"` patterns (Section 5).

| # | Verbatim Sentence / Normative Clause | Required `(Dimension = Value)` Combinations to Cover Clause | CLDR Data Evidence & Expected Behavior |
| :---: | :--- | :--- | :--- |
| **S8.1** | *"A currency decimal pattern normally contains a currency symbol placeholder (¤, ¤¤, ¤¤¤, or ¤¤¤¤¤)."* | • **S8.1a**: any `¤` display: the pattern has a placeholder<br>• **S8.1b**: `currency_display = "noCurrency"`: the pattern has none | • S8.1a: CLDR patterns have `¤`, and `currency_display` chooses what replaces it (Section 7): `en` USD 1.2 → `$1.20`.<br>• S8.1b: the `alt="noCurrency"` pattern (Section 5): `en` USD 1.2 → `1.20`. |
| **S8.2** | *"The currency symbol placeholder may occur before the first digit, after the last digit symbol, or where the decimal symbol would otherwise be placed (for formats such as "12€50", as in "12€50 pour une omelette")."* | • **S8.2a**: `¤` before the first digit (`en`)<br>• **S8.2b**: `¤` after the last digit (`de`)<br>• **S8.2c**: `¤` where the decimal separator would be | • S8.2a: `en` USD 1.2 → `$1.20`.<br>• S8.2b: `de` EUR 1.2 → `1,20 €` / `"1,20\u00A0€"`.<br>• S8.2c: no CLDR data (see 8.4). |
| **S8.3** | *Before*: *"¤#,##0.00" "¤ #,##0.00" "¤-#,##0.00" "¤ -#,##0.00" "-¤#,##0.00" "-¤ #,##0.00" …* | `currency_display = "symbol"` × a `locale` with:<br>• **S8.3a**: `"¤#,##0.00"` (`en`)<br>• **S8.3b**: `"¤ #,##0.00"` (`de_CH`; `en` × a symbol that ends with a letter)<br>• **S8.3c**: `"¤-#,##0.00"` (`de_CH` × a negative `input`)<br>• **S8.3d**: `"¤ -#,##0.00"` (`nl` × a negative `input`)<br>• **S8.3e**: `"-¤#,##0.00"` (`en` × a negative `input`)<br>• **S8.3f**: `"-¤ #,##0.00"` (`en` × a symbol that ends with a letter × a negative `input`) | • S8.3a: `en` USD 1.2 → `$1.20`.<br>• S8.3b: `de_CH` USD 1.2 → `$ 1.20` / `"$\u00A01.20"`; `en` RUB 1.2 → `RUB 1.20` / `"RUB\u00A01.20"`.<br>• S8.3c: `de_CH` USD −1230.05 → `$-1'230.05`.<br>• S8.3d: `nl` EUR −1230.05 → `€ -1.230,05` / `"€\u00A0-1.230,05"`.<br>• S8.3e: `en` USD −1230.05 → `-$1,230.05`.<br>• S8.3f: `en` RUB −1230.05 → `-RUB 1,230.05` / `"-RUB\u00A01,230.05"`. |
| **S8.4** | *After*: *"#,##0.00¤" "#,##0.00 ¤" "#,##0.00-¤" "#,##0.00- ¤" "#,##0.00¤-" "#,##0.00 ¤-" …* | `currency_display = "symbol"` × a `locale` with:<br>• **S8.4a**: `"#,##0.00¤"` (`ar` × `currency_format_type = "accounting"` × a symbol that does not start with a letter; `bn`)<br>• **S8.4b**: `"#,##0.00 ¤"` (`de`)<br>• **S8.4c**: a minus sign after the number: `"#,##0.00-¤"`, `"#,##0.00- ¤"`, `"#,##0.00¤-"`, `"#,##0.00 ¤-"` | • S8.4a: `ar` accounting EUR 1.2 → `؜1.20€` / `"\u061C1.20€"`.<br>• S8.4b: `de` EUR 1.2 → `1,20 €` / `"1,20\u00A0€"`.<br>• S8.4c: no CLDR data. |
| **S8.5** | *Decimal*: *"#,##0¤00"* | `¤` where the decimal separator would be | No CLDR data (as S8.2c). |

---

### 8.3 Comparison Against `GenerateCurrencyFormatTestData.java` (PR [#5808](https://github.com/unicode-org/cldr/pull/5808))

| Clause | Required `(Dimension = Value)` Combination | Status | Generator Evidence / Action Required |
| :---: | :--- | :---: | :--- |
| **S8.1a** | Any `¤` display | ✅ **Covered** | CORE values: `"symbol"`, `"symbolNarrow"`, and `"code"`. |
| **S8.1b** | `currency_display = "noCurrency"` | ✅ **Covered** | CORE value (see S5.1a–e). |
| **S8.2a**, **S8.3a–c**, **S8.3e–f** | `¤` before the number, with the minus sign before `¤` or between `¤` and the number | ✅ **Covered** | CORE values: `en` and `de_CH` in `CORE_LOCALES`; `USD` and `RUB` in `CORE_CURRENCIES`; `1.2` and `-1230.05` in `CORE_NUMBERS`. |
| **S8.2b**, **S8.4a–b** | `¤` after the number | ✅ **Covered** | CORE values: `ar`, `bn`, and `de`; `EUR`; `"accounting"`; `1.2`. |
| **S8.3d** | `locale = "nl"` × a negative `input` | 🟡 **Missing: `locale` in CORE** | `nl` is an extended locale value: `currencies_modern_locales.tsv` combines it with its own currencies and `TINY_NUMBERS`. No CORE locale has a space between `¤` and the minus sign. **Action**: add `"nl"` to `CORE_LOCALES` (see the Summary). |
| **S8.2c**, **S8.5** | `¤` where the decimal separator would be | ⚪ **Out of scope** | No CLDR data. |
| **S8.4c** | `¤` and a minus sign after the number | ⚪ **Out of scope** | No CLDR data. |

### 8.4 Notes

* **"12€50"** (S8.2c, S8.5): CLDR has no pattern with `¤` between digits. A similar result comes from a currency's own decimal separator ([Currency-Specific Decimal and Grouping Overrides](../../../docs/ldml/tr35-numbers.md#currency-specific-decimal-and-grouping-overrides)): `pt_CV` and `kea` give CVE the decimal separator `$` and the symbol U+200B ZERO WIDTH SPACE, so `pt_CV` CVE 12.5 → `"12$50\u00A0\u200B"`. `pt_PT` does the same for the former currency PTE.
* **Spaces** (S8.3–S8.4): the examples have a U+0020 SPACE between `¤` and the number; CLDR patterns have U+00A0 NO-BREAK SPACE there.
* **The minus sign** (S8.3c–f, S8.4c): in CLDR data, a minus sign between `¤` and the number comes only from explicit negative subpatterns (`de_CH`, `lo`, `nl`), and a minus sign after the number only from `fy`'s `¤ #,##0.00-` / `"¤\u00A0#,##0.00-"`. With an implicit negative subpattern, the minus sign comes first (`-¤#,##0.00`, `-#,##0.00 ¤` / `"-#,##0.00\u00A0¤"`).
* **Other shapes** (the "…" of the table): CLDR data also has parentheses (`(¤#,##0.00)` in `en` and `ja`, `(¤ #,##0.00)` / `"(¤\u00A0#,##0.00)"` in `fy`, `(#,##0.00 ¤)` / `"(#,##0.00\u00A0¤)"` in `pt_PT`, and `(#,##0.00¤)` in `ar` and `bn`, all accounting patterns), and, in locales that are neither CORE nor extended values, `(#,##0.00) ¤` (`co`) and `¤- #,##0.00` (`luy`).
* **Bidi marks** (S8.4): the patterns of `ar` start with U+200F RIGHT-TO-LEFT MARK or U+061C ARABIC LETTER MARK; the scan ignores them.

---

## Section 9: Formatting Currency Display Names (`#formatting-currency-display-names-unitpattern`)

* **TR35 Specification Link**: [`tr35-numbers.md#formatting-currency-display-names-unitpattern`](../../../docs/ldml/tr35-numbers.md#formatting-currency-display-names-unitpattern) (UTS #35 Part 3, Section 5.1: *Formatting Currency Display Names (`unitPattern`)*; L978–L1021 at `2997bffaf0`)
* **Related specification text**: L720 ([`¤` row](../../../docs/ldml/tr35-numbers.md#Number_Pattern_Character_Definitions): `¤¤¤` is the "Appropriate currency display name for the currency, based on the plural rules in effect for the locale"; see Section 7), L286–L292 ([`currencyDecimal` and `currencyGroup`](../../../docs/ldml/tr35-numbers.md#currencydecimal); see Section 1), L1064 (the number of decimal places of a currency is not locale data; see [Supplemental Currency Data](../../../docs/ldml/tr35-numbers.md#Supplemental_Currency_Data)), L1278 ("A source number represents the visual appearance of the digits of the result", in [Language Plural Rules](../../../docs/ldml/tr35-numbers.md#Language_Plural_Rules)), L486 (step 8 of compact formatting determines the plural category "based on the numeric precision settings"), and L1293–L1311 ([Explicit 0 and 1 rules](../../../docs/ldml/tr35-numbers.md#Explicit_0_1_rules): `count="0"` and `count="1"` apply to the exact values 0 and 1, and take precedence over `zero` and `one`)

### 9.1 Verbatim Specification Snippet (`docs/ldml/tr35-numbers.md`)

> ### Formatting Currency Display Names (`unitPattern`)
>
> The `count` attribute distinguishes the different plural forms, such as in the following:
>
> ```xml
> <currencyFormats>
>     <unitPattern count="other">{0} {1}</unitPattern>
>     …
> </currencyFormats>
> ```
>
> ```xml
> <currency type="ZWD">
>     <displayName>Zimbabwe Dollar</displayName>
>     <displayName count="one">Zimbabwe dollar</displayName>
>     <displayName count="other">Zimbabwe dollars</displayName>
>     <symbol>Z$</symbol>
> </currency>
> ```
>
> Note on displayNames:
> * In general the region portion of the displayName should match the territory name, see **Part 2** _[Locale Display Name Fields](../../../docs/ldml/tr35-general.md#locale_display_name_fields)_.
> * As a result, the English currency displayName in CLDR may not match the name in ISO 4217.
>
> To format a particular currency value "ZWD" for a particular numeric value _n_ using the (long) display name:
>
> 1. If the numeric value is exactly 0 or 1, first see if there is a count with a matching explicit number (0 or 1). If so, use that string (see [Explicit 0 and 1 rules](../../../docs/ldml/tr35-numbers.md#Explicit_0_1_rules)).
> 2. Otherwise, determine the `count` value that corresponds to _n_ using the rules in _[- Language Plural Rules](../../../docs/ldml/tr35-numbers.md#Language_Plural_Rules)_
> 3. Next, get the currency unitPattern.
>    1. Look for a `unitPattern` element that matches the `count` value, starting in the current locale and then following the locale fallback chain up to, but not including root.
>    2. If no matching `unitPattern` element was found in the previous step, then look for a `unitPattern` element that matches `count="other"`, starting in the current locale and then following the locale fallback chain up to root (which has a `unitPattern` element with `count="other"` for every unit type).
>    3. The resulting unitPattern element indicates the appropriate positioning of the numeric value and the currency display name.
> 4. Next, get the `displayName` element for the currency.
>    1. Look for a `displayName` element that matches the `count` value, starting in the current locale and then following the locale fallback chain up to, but not including root.
>    2. If no matching `displayName` element was found in the previous step, then look for a `displayName` element that matches `count="other"`, starting in the current locale and then following the locale fallback chain up to, but not including root.
>    3. If no matching `displayName` element was found in the previous step, then look for a `displayName` element with no count, starting in the current locale and then following the locale fallback chain up to root.
>    4. If there is no `displayName` element, use the currency code itself (for example, "ZWD").
> 5. Format the numeric value according to the locale. Use the locale’s `<decimalFormats …>` pattern, not the `<currencyFormats>` pattern that is used with the symbol (eg, Z$). As when formatting symbol currency values, reset the number of decimals according to the supplemental `<currencyData>` and use the currencyDecimal symbol if different from the decimal symbol.
>    1. The number of decimals should be overridable in an API, so that clients can choose between “2 US dollars” and “2.00 US dollars”.
> 6. Substitute the formatted numeric value for the {0} in the `unitPattern`, and the currency display name for the {1}.
>
> While for English this may seem overly complex, for some other languages different plural forms are used for different unit types; the plural forms for certain unit types may not use all of the plural-form tags defined for the language.
>
> For example, if the currency is ZWD and the number is 1234, then the latter maps to `count="other"` for English. The unit pattern for that is "{0} {1}", and the display name is "Zimbabwe dollars". The final formatted number is then "1,234 Zimbabwe dollars".

---

### 9.2 Sentence-by-Sentence `(Dimension / Value)` Coverage Breakdown

**CLDR data.** The currency `unitPattern` elements of the CORE locales, for their default numbering system (skipping `↑↑↑`, `draft="provisional"`, and `draft="unconfirmed"` values), and the plural categories of each locale (`common/supplemental/plurals.xml`):

| Locale | `unitPattern` elements below `root` | Plural categories |
| :--- | :--- | :--- |
| `root` | `count="other"`: `{0} {1}` | — |
| `en`, `fy` | `count="one"` and `count="other"`: `{0} {1}` | `one`, `other` |
| `ja` | `count="other"`: `{0}{1}` | `other` |
| `ar_EG` (`arab`) | `count` = `zero`, `one`, `two`, `few`, `many`, and `other`: `{0} {1}` (the `arab` data of `ar`) | `zero`, `one`, `two`, `few`, `many`, `other` |
| `ar` (`latn`) | — | `zero`, `one`, `two`, `few`, `many`, `other` |
| `bn`, `de`, `de_CH` | — | `one`, `other` |
| `pt_PT` | — | `one`, `many`, `other` |
| `ru` | — | `one`, `few`, `many`, `other` |

Outside the CORE locales, `si` (`{1}{0}`) and `my` (`{1} {0}`), both extended locale values, and `blo` and `to` (`{1} {0}`) put the display name first. `ro`, an extended locale value, has `{0} {1}` for `count="one"` and `"few"` but `{0} de {1}` for `"other"`. No `unitPattern` or `displayName` in CLDR data has `count="0"` or `count="1"`.

Display names of the CORE currencies in the CORE locales:
* `en` and `pt_PT`: a name without `count` and names for `one` and `other` for all five currencies, such as `en` USD `US Dollar`, `US dollar`, and `US dollars`.
* `de` (inherited by `de_CH`): JPY, RUB, and EGP have names for `one` and `other` (JPY `Japanischer Yen` and `Japanische Yen`); USD (`US-Dollar`) and EUR (`Euro`) have only the name without `count`.
* `ru`: JPY, RUB, and EGP have names for `one`, `few`, `many`, and `other` (JPY `японская иена`, `японские иены`, `японских иен`, and `японской иены`); USD has `one`, `many`, and `other`, but no `few`; EUR (`евро`) has only the name without `count`.
* `ar` (inherited by `ar_EG`): EGP has names for `two` (`جنيهان مصريان`), `few` (`جنيهات مصرية`), and `many` (`جنيهًا مصريًا`) and the name without `count` (`جنيه مصري`), but none for `zero`, `one`, or `other`; USD, EUR, JPY, and RUB have only the name without `count`.
* `fy` EUR has a name for `other` (`euro`), and `ja` JPY one for `other` (`円`). The other currencies of `fy` and `ja`, and all currencies of `bn`, have only the name without `count`.

Plural categories that the CORE inputs reach, with the currency's number of decimals applied (see 9.4):

| Locale | Reached | Not reached |
| :--- | :--- | :--- |
| `ar`, `ar_EG` | `zero` (`0.0`), `one` (`JPY` × `1.2`, formatted as `1`), `many` (`1234565.0`), `other` (`1.2` with two decimals) | `two`, `few` |
| `ru` | `one` (`JPY` × `1.2`), `many` (`JPY` × `1234565.0`), `other` (`1.2` with two decimals) | `few` |
| `pt_PT` | `one` (`JPY` × `1.2`), `other` | `many` |
| `bn`, `de`, `de_CH`, `en`, `fy` | `one` (`JPY` × `1.2`; in `bn` also `0.0`), `other` | — |
| `ja` | `other` | — |

| # | Verbatim Sentence / Normative Clause | Required `(Dimension = Value)` Combinations to Cover Clause | CLDR Data Evidence & Expected Behavior |
| :---: | :--- | :--- | :--- |
| **S9.1** | *"The `count` attribute distinguishes the different plural forms, such as in the following:"* and the two XML samples (a `unitPattern` with `count="other"`, and the `displayName` elements of ZWD with and without `count`) | `currency_display = "name"` × inputs in different plural categories (`de` × `JPY` × `1.2` and `1234565.0`) | `de` JPY 1.2 → `1 Japanischer Yen` (`one`); 1234565.0 → `1.234.565 Japanische Yen` (`other`). The `unitPattern` sample is the value in `root`. |
| **S9.2** | *"Note on displayNames:"*<br>• *"In general the region portion of the displayName should match the territory name, see Part 2 [Locale Display Name Fields](../../../docs/ldml/tr35-general.md#locale_display_name_fields)."*<br>• *"As a result, the English currency displayName in CLDR may not match the name in ISO 4217."* | — | Guidance for the display name data. |
| **S9.3** | *"To format a particular currency value "ZWD" for a particular numeric value n using the (long) display name:"* | `currency_display = "name"` | The display name is the `¤¤¤` of Section 7: `en` USD 1.2 → `1.20 US dollars`. |
| **S9.4** | Step 1: *"If the numeric value is exactly 0 or 1, first see if there is a count with a matching explicit number (0 or 1). If so, use that string (see [Explicit 0 and 1 rules](../../../docs/ldml/tr35-numbers.md#Explicit_0_1_rules))."* | • **S9.4a**: `input = 0.0` (e.g. `locale` = `en` or `ar`, `currency = "USD"`)<br>• **S9.4b**: `input = 1.0`<br>• **S9.4c**: a `displayName` or `unitPattern` with `count="0"` or `count="1"` | • S9.4a: there is no `count="0"` data, so step 2 applies: `en` USD 0.0 → `0.00 US dollars` (`other`); `ar` USD 0.0 → `0.00 دولار أمريكي` (`zero`).<br>• S9.4b: there is no `count="1"` data, so step 2 applies: `en` USD 1.0 → `1.00 US dollars` (`1.00` is `other` in English); `de` JPY 1.0 → `1 Japanischer Yen` (`one`).<br>• S9.4c: no CLDR data. |
| **S9.5** | Step 2: *"Otherwise, determine the `count` value that corresponds to n using the rules in [- Language Plural Rules](../../../docs/ldml/tr35-numbers.md#Language_Plural_Rules)"* | • **S9.5a**: inputs that reach `zero`, `one`, `many`, and `other` (`ar` × `0.0`; `de` × `JPY` × `1.2`; `ru` × `JPY` × `1234565.0`; `en` × `USD` × `1.2`)<br>• **S9.5b**: `two` (`ar` × `EGP` × `2.0`)<br>• **S9.5c**: `few` (`ar` × `EGP` × `5.0`; `ru` × `JPY` × `2.0` or `123`)<br>• **S9.5d**: `many` in `pt_PT` (`pt_PT` × `JPY` × `1000000.0`) | • S9.5a: `ar` USD 0.0 → `0.00 دولار أمريكي` (`zero`); `de` JPY 1.2 → `1 Japanischer Yen` (`one`); `ru` JPY 1234565.0 → `1 234 565 японских иен` / `"1\u00A0234\u00A0565 японских иен"` (`many`); `en` USD 1.2 → `1.20 US dollars` (`other`).<br>• S9.5b: `ar` EGP 2.0 → `2.00 جنيهان مصريان`.<br>• S9.5c: `ar` EGP 5.0 → `5.00 جنيهات مصرية`; `ru` JPY 2.0 → `2 японские иены`, and 123 → `123 японские иены`.<br>• S9.5d: `pt_PT` JPY 1000000.0 → `1 000 000 ienes japoneses` / `"1\u00A0000\u00A0000 ienes japoneses"` (see S9.7b). |
| **S9.6** | Step 3: *"Next, get the currency unitPattern."*<br>• 3.1: *"Look for a `unitPattern` element that matches the `count` value, starting in the current locale and then following the locale fallback chain up to, but not including root."*<br>• 3.2: *"If no matching `unitPattern` element was found in the previous step, then look for a `unitPattern` element that matches `count="other"`, starting in the current locale and then following the locale fallback chain up to root (which has a `unitPattern` element with `count="other"` for every unit type)."*<br>• 3.3: *"The resulting unitPattern element indicates the appropriate positioning of the numeric value and the currency display name."* | • **S9.6a**: step 3.1: a `unitPattern` for the category below `root` (`en` × `JPY` × `1.2`: `count="one"`; `ja`: `count="other"`)<br>• **S9.6b**: step 3.2: `count="other"` from `root` (`de` × `JPY` × `1.2`: `one`; `ar` × `0.0`: `zero`)<br>• **S9.6c**: step 3.3: a `unitPattern` that puts the display name first (`si`) | • S9.6a: `en` JPY 1.2 → `1 Japanese yen` (the `count="one"` `{0} {1}` of `en`); `ja` USD 1.2 → `1.20米ドル` (`{0}{1}`, without a space).<br>• S9.6b: `de` JPY 1.2 → `1 Japanischer Yen`; `ar` USD 0.0 → `0.00 دولار أمريكي` (both with the `{0} {1}` of `root`).<br>• S9.6c: `si` USD 1.2 → `ඇමරිකානු ඩොලර්1.20` (`{1}{0}`). |
| **S9.7** | Step 4: *"Next, get the `displayName` element for the currency."*<br>• 4.1: *"Look for a `displayName` element that matches the `count` value, starting in the current locale and then following the locale fallback chain up to, but not including root."*<br>• 4.2: *"If no matching `displayName` element was found in the previous step, then look for a `displayName` element that matches `count="other"`, starting in the current locale and then following the locale fallback chain up to, but not including root."*<br>• 4.3: *"If no matching `displayName` element was found in the previous step, then look for a `displayName` element with no count, starting in the current locale and then following the locale fallback chain up to root."*<br>• 4.4: *"If there is no `displayName` element, use the currency code itself (for example, "ZWD")."* | • **S9.7a**: step 4.1 (`ru` × `JPY` × `1234565.0`; `ar` × `EGP` × `1234565.0`)<br>• **S9.7b**: step 4.2: a category without a name (`pt_PT` × `JPY` × `1000000.0`: `many`)<br>• **S9.7c**: step 4.3: no name for the category or for `other` (`de` × `USD`; `ar` × `EGP` × `0.0`)<br>• **S9.7d**: step 4.4: no name at all (`fy` × `XCG`) | • S9.7a: `ru` JPY 1234565.0 → `1 234 565 японских иен` / `"1\u00A0234\u00A0565 японских иен"`; `ar` EGP 1234565.0 → `1,234,565.00 جنيهًا مصريًا` (both `many`).<br>• S9.7b: `pt_PT` has no `many` names: `pt_PT` JPY 1000000.0 → `1 000 000 ienes japoneses` / `"1\u00A0000\u00A0000 ienes japoneses"`, with the `other` name.<br>• S9.7c: `de` USD 1.2 → `1,20 US-Dollar`; `ar` EGP 0.0 → `0.00 جنيه مصري` (`zero`, for which EGP has no name).<br>• S9.7d: the ISO code: `fy` XCG 1.2 → `1,20 XCG` (as S7.4c). |
| **S9.8** | Step 5: *"Format the numeric value according to the locale. Use the locale’s `<decimalFormats …>` pattern, not the `<currencyFormats>` pattern that is used with the symbol (eg, Z$). As when formatting symbol currency values, reset the number of decimals according to the supplemental `<currencyData>` and use the currencyDecimal symbol if different from the decimal symbol."* | • **S9.8a**: the decimal pattern, where it differs from the currency pattern in more than `¤` (`fy` × `-1230.05`; `ar` × `-1230.05`)<br>• **S9.8b**: the currency's number of decimals (`en` × `USD` × `0.0`; `en` × `JPY` × `1.2`)<br>• **S9.8c**: a `locale` with `currencyDecimal` (`fr_CH`) | • S9.8a: `fy` EUR −1230.05 → `-1.230,05 euro`, where the currency pattern gives `€ 1.230,05-` / `"€\u00A01.230,05-"`; `ar` USD −1230.05 → `‎-1,230.05 دولار أمريكي` / `"\u200E-1,230.05 دولار أمريكي"`, without the U+200F RIGHT-TO-LEFT MARK of the currency pattern.<br>• S9.8b: `en` USD 0.0 → `0.00 US dollars` (2 decimals, where the decimal pattern `#,##0.###` alone gives `0`); `en` JPY 1.2 → `1 Japanese yen` (0 decimals).<br>• S9.8c: `fr_CH` EUR −1230.05 → `-1'230.05 euros` (S1.1c). |
| **S9.9** | Step 5.1: *"The number of decimals should be overridable in an API, so that clients can choose between “2 US dollars” and “2.00 US dollars”."* | `fraction_digits = 0` × `currency_display = "name"` (e.g. `en` × `USD` × `1.2` and `-1230.05`) | `en` USD 1.2 → `1 US dollar` (the formatted number `1` is `one`); −1230.05 → `-1,230 US dollars`. For the example, `en` USD 2.0 → `2 US dollars` with 0 decimals, and `2.00 US dollars` with the currency's 2 decimals. |
| **S9.10** | Step 6: *"Substitute the formatted numeric value for the {0} in the `unitPattern`, and the currency display name for the {1}."* | `currency_display = "name"` | Every example above; see S9.6 for the order and the space. |
| **S9.11** | *"While for English this may seem overly complex, for some other languages different plural forms are used for different unit types; the plural forms for certain unit types may not use all of the plural-form tags defined for the language."* | A currency whose names do not use every plural category of the locale (`ar` × `EGP` × `0.0`; `ar` × `USD` × `1234565.0`) | `ar` has six categories. EGP has names for three of them, so `ar` EGP 0.0 (`zero`) uses the name without `count` (S9.7c); USD has none: `ar` USD 1234565.0 → `1,234,565.00 دولار أمريكي` (`many`). |
| **S9.12** | *"For example, if the currency is ZWD and the number is 1234, then the latter maps to `count="other"` for English. The unit pattern for that is "{0} {1}", and the display name is "Zimbabwe dollars". The final formatted number is then "1,234 Zimbabwe dollars"."* | `locale = "en"` × a `currency` without decimals × an `input` with a grouping separator (`en` × `JPY` × `1234565.0`) | `en` JPY 1234565.0 → `1,234,565 Japanese yen` (`other`, with the `{0} {1}` of `en`). With CLDR data, `en` ZWD 1234 → `1,234 Zimbabwean dollars (1980–2008)` (see 9.4). |

---

### 9.3 Comparison Against `GenerateCurrencyFormatTestData.java` (PR [#5808](https://github.com/unicode-org/cldr/pull/5808))

| Clause | Required `(Dimension = Value)` Combination | Status | Generator Evidence / Action Required |
| :---: | :--- | :---: | :--- |
| **S9.1** | `currency_display = "name"` × inputs in different plural categories | ✅ **Covered** | CORE values: `"name"`; `de`; `JPY`; `1.2` and `1234565.0` (as S7.2c). |
| **S9.2** | — | ⚪ **Out of scope** | Guidance for the display name data; nothing to test. |
| **S9.3**, **S9.10** | `currency_display = "name"` | ✅ **Covered** | CORE value `"name"` (with `currency_format_length = ""` and `currency_format_type = "standard"`), combined with every CORE locale, currency, and input. |
| **S9.4a** | `input = 0.0` | ✅ **Covered** | CORE value: `0.0` in `CORE_NUMBERS`. |
| **S9.4b** | `input = 1.0` | 🟡 **Missing: `input` in CORE** | `1.0` (10⁰) is an extended number value, combined only with `TINY_LOCALES` and `TINY_CURRENCIES` (`currencies_name_extended_numbers.tsv`). **Action**: the `"name"` row of the Summary. |
| **S9.4c** | `count="0"` or `count="1"` data | ⚪ **Out of scope** | No CLDR data. |
| **S9.5a** | Inputs that reach `zero`, `one`, `many`, and `other` | ✅ **Covered** | CORE values: `ar`, `de`, `ru`, and `en`; `USD` and `JPY`; `0.0`, `1.2`, and `1234565.0`. |
| **S9.5b** | `two` (`ar` × `EGP` × `2.0`) | 🟡 **Missing: `input`** | No CORE input is `two` in `ar`, and `2.0` is in neither `CORE_NUMBERS` nor the extended numbers. The extended `1.5` and `2.5` round to `2` only with a currency without decimals, and the extended numbers are combined only with `USD` and `EUR`. **Action**: the `"name"` row of the Summary. |
| **S9.5c** | `few` (`ar` × `EGP` × `5.0`; `ru` × `JPY` × `2.0` or `123`) | 🟡 **Missing: `input` in CORE** | No CORE input is `few` in `ar` or `ru`. `5.0` and `123` are extended number values, combined only with `TINY_LOCALES` and `TINY_CURRENCIES`. **Action**: the `"name"` row of the Summary. |
| **S9.5d** | `many` in `pt_PT` (`pt_PT` × `JPY` × `1000000.0`) | 🟡 **Missing: `input` in CORE** | `many` in `pt_PT` needs a multiple of 1,000,000 without visible decimals. `1000000.0` (10⁶) is an extended number value, and `pt_PT` is not in `TINY_LOCALES`. **Action**: the `"name"` row of the Summary. |
| **S9.6a** | Step 3.1: a `unitPattern` for the category below `root` | ✅ **Covered** | CORE values: `en` and `ja`; `JPY` and `USD`; `1.2`. |
| **S9.6b** | Step 3.2: the `count="other"` pattern of `root` | ✅ **Covered** | CORE values: `de` and `ar`; `JPY` and `USD`; `1.2` and `0.0`. |
| **S9.6c** | Step 3.3: the display name before the number | 🟡 **Missing: `locale` in CORE** | Every CORE locale puts `{0}` first. `si` (`{1}{0}`) and `my` (`{1} {0}`) are extended locale values: `currencies_modern_locales.tsv` combines them with their own currencies and `TINY_NUMBERS`. **Action**: include `"si"` in the `"name"` row of the Summary. |
| **S9.7a** | Step 4.1: a `displayName` for the category | ✅ **Covered** | CORE values: `ru` and `ar`; `JPY` and `EGP`; `1234565.0`. |
| **S9.7b** | Step 4.2: the `count="other"` name for a category without a name | 🟡 **Missing: `input` in CORE** | No CORE combination reaches this step: each category that the CORE inputs reach has a name, or the currency has no `count="other"` name either (every CORE currency in `ar`; `fy` × `JPY`). `1000000.0` is an extended number value (as S9.5d). **Action**: the `"name"` row of the Summary. |
| **S9.7c** | Step 4.3: the name without `count` | ✅ **Covered** | CORE values: `de` × `USD`; `ar` × `EGP` × `0.0`. |
| **S9.7d** | Step 4.4: the ISO code | 🟡 **Missing: `currency` in CORE** | As S7.4c: every CORE locale has a name for every CORE currency. **Action**: the `XCG` row of the Summary covers it (`fy` × `XCG`). |
| **S9.8a** | The decimal pattern, not the currency pattern | ✅ **Covered** | CORE values: `fy` and `ar`; `EUR` and `USD`; `-1230.05`. |
| **S9.8b** | The currency's number of decimals | ✅ **Covered** | CORE values: `en`; `USD` (2 decimals) and `JPY` (0); `0.0` and `1.2`. |
| **S9.8c** | `locale = "fr_CH"` × `currency_display = "name"` | 🟡 **Missing: `locale`** | As S1.1c. **Action**: the `fr_CH` row of the Summary. |
| **S9.9** | `fraction_digits = 0` × `currency_display = "name"` | 🟡 **Missing: new dimension** | The generator always uses the currency's number of decimals. **Action**: add the `fraction_digits` dimension (see the dimensions table and the Summary). |
| **S9.11** | A currency whose names do not use every plural category of the locale | ✅ **Covered** | CORE values: `ar`; `EGP` and `USD`; `0.0` and `1234565.0`. |
| **S9.12** | `en` × a currency without decimals × an input with a grouping separator | ✅ **Covered** | CORE values: `en`; `JPY`; `1234565.0`. |

### 9.4 Notes

* **The plural category** (S9.4–S9.5, S9.9): step 2 determines the `count` "that corresponds to n". The expected values above use the formatted number, as the plural rules do ("A source number represents the visual appearance of the digits of the result", L1278) and as step 8 of compact formatting does (L486). The difference shows with a currency without decimals, with `1.0`, and with `fraction_digits`: `de` JPY 1.2 → `1 Japanischer Yen` (`1` is `one`), where the category of 1.2 would give `1 Japanische Yen`; `en` USD 1.0 → `1.00 US dollars` (`1.00` is `other`), where the category of 1 would give `1.00 US dollar`; `en` USD 1.2 with 0 decimals → `1 US dollar`.
* **Unit patterns that depend on the category** (S9.6): `ro` JPY 1234565.0 → `1.234.565 de yeni japonezi` (`other`: `{0} de {1}`), but `ro` USD 1.2 → `1,20 dolari americani` (`few`: `{0} {1}`, with the `other` name of step 4.2). No CORE locale has unit patterns that differ by category.
* **The grouping separator** (S9.8): step 5 mentions only `currencyDecimal`. Whether `currencyGroup` also applies to display names is part of [CLDR-19825](https://unicode-org.atlassian.net/browse/CLDR-19825) (S1.3c).
* **The minus sign of `ar`** (S9.8a): the `minusSign` of `ar` (`latn`) is `‎-` / `"\u200E-"`, so the result keeps the U+200E LEFT-TO-RIGHT MARK; only the U+200F of the currency pattern is dropped.
* **ZWD** (S9.12): ZWD has 0 decimals in `<currencyData>`, which matches `1,234`. The names in the sample (`Zimbabwe Dollar`, `Zimbabwe dollar`, `Zimbabwe dollars`) are not the current `en` data: `Zimbabwean Dollar (1980–2008)`, `Zimbabwean dollar (1980–2008)`, and `Zimbabwean dollars (1980–2008)`. ZWD is not legal tender, so it is not an extended currency value.
* **Editorial**: the link text of step 2 (L1005) starts with "- ", and the sentence has no final period.

---

## Summary: Required Generator Changes

| Change | Needed by |
| :--- | :--- |
| Add one locale with a monetary separator to `CORE_LOCALES` (+300 rows): `"fr_CH"` (recommended: unlike the `de_AT` override, it also shows with approved data only; see 1.2) or `"de_AT"` | S1.1a–c, S7.3a, S7.3c, S9.8c (`fr_CH`) or S1.3a–b, S7.3a (`de_AT`) |
| Add `"zh"` to `CORE_LOCALES` (+300 rows) | S2.1b |
| Add `"XCG"` to `CORE_CURRENCIES` (+600 rows); `"CHF"` also works for S2.2c and S4.1d once ICU4J has its `symbolNarrow` `Fr.` | S2.2c, S4.1d, S7.4b–c, S9.7d |
| Add `"kn"` to `CORE_LOCALES` and `"RON"` to `CORE_CURRENCIES` (+960 rows) | S2.2d, S4.1e |
| Add `"id"` to `CORE_LOCALES` (+300 rows); `af` and `ha` also qualify, but the pinned ICU4J has older compact patterns for them | S2.4b |
| Add the `cf` dimension with `"standard"` and `"account"`, with `currency_format_type` unset, in a separate file: `TINY_LOCALES` × `TINY_CURRENCIES` × `TINY_NUMBERS` × the 2 `cf` values, with `currency_format_length = ""` and `currency_display = "symbol"` (+24 rows) | S3.3a–b |
| Add `"en_ZA"` to `CORE_LOCALES` (+300 rows): the only locale whose `alt="alphaNextToNumber"` pattern adds no space | S4.4b |
| Generate `currency_format_length = "short"` with `currency_display = "noCurrency"` (and `currency_format_type = "standard"`) for the CORE values (+250 rows): the specification gives the result, the compact decimal format | S5.3a |
| Add the `currency_pattern_append_iso` dimension with `true`, in a separate file: `TINY_LOCALES` and `"hi"` (whose pattern puts the ISO code first) × `CORE_CURRENCIES` × `TINY_NUMBERS` × `currency_display` = `"symbol"` and `"symbolNarrow"` × the 3 combinations of `currency_format_length` and `currency_format_type` that use them (+240 rows) | S6.1a–d, S6.2a–c, S6.3a–c |
| Add `"nl"` to `CORE_LOCALES` (+300 rows): the only extended locale whose negative pattern has a space between `¤` and the minus sign (`¤ -#,##0.00` / `"¤\u00A0-#,##0.00"`) | S8.3d |
| Add a separate file of `currency_display = "name"` rows: `CORE_LOCALES` and `"si"` (whose `unitPattern` puts the display name first) × `CORE_CURRENCIES` × the inputs `1.0` (exactly 1), `2.0` (`two` in `ar`; `few` in `ru` with `JPY`), `5.0` (`few` in `ar`), and `1000000.0` (`many` in `pt_PT` with `JPY`) (+220 rows) | S9.4b, S9.5b–d, S9.6c, S9.7b |
| Add the `fraction_digits` dimension with `0`, in a separate file: `TINY_LOCALES` × `TINY_CURRENCIES` × `TINY_NUMBERS` × `currency_display = "name"` (+12 rows) | S9.9 |

For Section 1, one locale with a monetary separator is enough to exercise the override. `en` and the other CORE locales already cover the "otherwise" clauses (S1.2, S1.4). The clauses of the locale that is not added stay 🟡 **Missing: `locale`**.

The row counts are for each change alone. All changes except the `cf`, `currency_pattern_append_iso`, and `fraction_digits` dimensions and the `"name"` rows of Section 9 add rows to `currencies.tsv`. The file now has 10 locales × 5 currencies × 12 combinations of the format and display dimensions × 5 inputs = 3,000 rows; with all the changes above, including `"short"` × `"noCurrency"` as a 13th combination, it would have 16 × 7 × 13 × 5 = 7,280. The 24 `cf` rows, the 240 `currency_pattern_append_iso` rows, the 220 `"name"` rows of Section 9, and the 12 `fraction_digits` rows go in their own files.

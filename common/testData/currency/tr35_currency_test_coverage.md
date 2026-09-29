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

## Summary: Required Generator Changes

| Change | Needed by |
| :--- | :--- |
| Add one locale with a monetary separator to `CORE_LOCALES` (+300 rows): `"fr_CH"` (recommended: unlike the `de_AT` override, it also shows with approved data only; see 1.2) or `"de_AT"` | S1.1a–c (`fr_CH`) or S1.3a–b (`de_AT`) |
| Add `"zh"` to `CORE_LOCALES` (+300 rows) | S2.1b |
| Add `"XCG"` to `CORE_CURRENCIES` (+600 rows); `"CHF"` also works once ICU4J has its `symbolNarrow` `Fr.` | S2.2c |
| Add `"kn"` to `CORE_LOCALES` and `"RON"` to `CORE_CURRENCIES` (+960 rows) | S2.2d |
| Add `"id"` to `CORE_LOCALES` (+300 rows); `af` and `ha` also qualify, but the pinned ICU4J has older compact patterns for them | S2.4b |

For Section 1, one locale with a monetary separator is enough to exercise the override. `en` and the other CORE locales already cover the "otherwise" clauses (S1.2, S1.4). The clauses of the locale that is not added stay 🟡 **Missing: `locale`**.

The row counts are for `currencies.tsv` and for each change alone. The file now has 10 locales × 5 currencies × 12 combinations of the format and display dimensions × 5 inputs = 3,000 rows; with all the changes above, it would have 14 × 7 × 12 × 5 = 5,880.

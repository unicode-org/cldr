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

## Summary: Required Generator Changes

| Change | Needed by |
| :--- | :--- |
| Add one locale with a monetary separator to `CORE_LOCALES` (+300 rows): `"fr_CH"` (recommended: unlike the `de_AT` override, it also shows with approved data only; see 1.2) or `"de_AT"` | S1.1a–c (`fr_CH`) or S1.3a–b (`de_AT`) |
| Add `"zh"` to `CORE_LOCALES` (+300 rows) | S2.1b |
| Add `"XCG"` to `CORE_CURRENCIES` (+600 rows); `"CHF"` also works once ICU4J has its `symbolNarrow` `Fr.` | S2.2c, S4.1d |
| Add `"kn"` to `CORE_LOCALES` and `"RON"` to `CORE_CURRENCIES` (+960 rows) | S2.2d, S4.1e |
| Add `"id"` to `CORE_LOCALES` (+300 rows); `af` and `ha` also qualify, but the pinned ICU4J has older compact patterns for them | S2.4b |
| Add the `cf` dimension with `"standard"` and `"account"`, with `currency_format_type` unset, in a separate file: `TINY_LOCALES` × `TINY_CURRENCIES` × `TINY_NUMBERS` × the 2 `cf` values, with `currency_format_length = ""` and `currency_display = "symbol"` (+24 rows) | S3.3a–b |
| Add `"en_ZA"` to `CORE_LOCALES` (+300 rows): the only locale whose `alt="alphaNextToNumber"` pattern adds no space | S4.4b |
| Generate `currency_format_length = "short"` with `currency_display = "noCurrency"` (and `currency_format_type = "standard"`) for the CORE values (+250 rows): the specification gives the result, the compact decimal format | S5.3a |

For Section 1, one locale with a monetary separator is enough to exercise the override. `en` and the other CORE locales already cover the "otherwise" clauses (S1.2, S1.4). The clauses of the locale that is not added stay 🟡 **Missing: `locale`**.

The row counts are for each change alone. All changes except the `cf` dimension add rows to `currencies.tsv`. The file now has 10 locales × 5 currencies × 12 combinations of the format and display dimensions × 5 inputs = 3,000 rows; with all the changes above, including `"short"` × `"noCurrency"` as a 13th combination, it would have 15 × 7 × 13 × 5 = 6,825. The 24 `cf` rows go in their own file.

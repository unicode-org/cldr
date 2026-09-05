# CLDR Currency Format Test Data (`common/testData/currency`)

This directory contains Tab-Separated Values (TSV) files used for testing standard, accounting, and compact currency formatting across CLDR locales.

---

## Dimensions

Each test case is defined by the following dimensions, mapped directly to CLDR LDML XML elements, Unicode Technical Standard (UTS) #35 specifications, and ECMA-402 Intl standards:

| Dimension Column | Specification & Source | Description | Allowed Values |
| :--- | :--- | :--- | :--- |
| **`locale`** | CLDR Locale Identifier | The locale under test (e.g. `en`, `ar`, `de_CH`, `bn`, `fy`). | Valid CLDR locales. |
| **`currency`** | ISO 4217 Currency Code | The 3-letter currency code (e.g. `USD`, `EUR`, `JPY`). | Valid ISO 4217 codes, or empty `""` for decimal fallback tests. |
| **`currency_format_length`** | `<currencyFormatLength type="...">` | Selects standard decimal vs. compact formatting. | `""` (standard length) or `short` (compact short). |
| **`currency_format_type`** | `<currencyFormat type="...">` | Selects sign display and negative formatting. | `standard` (minus sign) or `accounting` (parentheses). |
| **`currency_display`** | ECMA-402 / UTS #35 Section 3.2 & 3.3 | Controls how the currency symbol or unit is presented. (Dimension name and values follow ECMA-402 `Intl.NumberFormat`; formatting behavior and data follow UTS #35). | `symbol`, `symbolNarrow`, `code`, `name`, `noCurrency`. |
| **`input`** | Numeric amount | Floating-point test amount. | Representative doubles (e.g. `0.0`, `1.2`, `-1230.05`, `1234565.0`). |
| **`expected`** | Formatted output | Expected localized string including symbols, grouping, and BiDi marks. | Localized formatted string. |

### Detail on `currency_display` Values

The values in the `currency_display` column describe how the currency is represented in the formatted output:
* **`symbol`**: The standard currency symbol (replaces `¤` in the currency pattern, e.g., `$`, `€`).
* **`symbolNarrow`**: The narrow currency symbol variant (replaces `¤` in the currency pattern, e.g., `$`).
* **`code`**: The 3-letter ISO 4217 code (replaces `¤` in the currency pattern, e.g., `USD`, `EUR`).
* **`name`**: The localized currency unit name. Rather than substituting `¤` in a currency pattern, formatting uses the locale's unit pattern `<unitPattern count="...">` (such as `{0} {1}` or `{1} {0}`), where `{0}` is the formatted number and `{1}` is the localized currency display name (e.g., `1.20 US dollars`, `1,20 euro`).
* **`noCurrency`**: The currency symbol is omitted using the `<pattern alt="noCurrency">` pattern (or removing `¤`). The amount is formatted using the currency's fraction digits and monetary formatting rules, but without any currency symbol.

---

## Filtering Rules (Dimension Constraints)

In CLDR LDML TR35, `<currencyFormatLength>` (standard length) defines both `type="standard"` and `type="accounting"`, whereas `<currencyFormatLength type="short">` (compact short) **only** defines `type="standard"`. There is no `accounting` format type for compact short currency formatting in CLDR.

To align strictly with CLDR LDML data and eliminate unsupported or redundant combinations, the test generator enforces the following filtering rules:

### Rule 1: Exclude `currency_format_length="short"` with `currency_display="noCurrency"`
* **Rationale**: Compact short currency patterns in CLDR only define patterns with currency symbol placeholders (`¤0K`, `¤0M`). There are **zero** `<pattern alt="noCurrency">` elements under `<currencyFormatLength type="short">` across all 371 CLDR locales. If a compact number is formatted without a currency sign, that is **Compact Decimal Formatting** (`<decimalFormatLength type="short">`), which is already tested in [`common/testData/decimal/`](../decimal/).

### Rule 2: Exclude `currency_display="name"` with `currency_format_type="accounting"`
* **Rationale**: In CLDR LDML, `<currencyFormat type="accounting">` only applies to currency signs (`symbol`, `symbolNarrow`, `code`). Spelled-out currency unit names (`name`) are defined completely separately under `<currencyFormats>` using:
  ```xml
  <unitPattern count="one">{0} {1}</unitPattern>
  <unitPattern count="other">{0} {1}</unitPattern>
  ```
  CLDR does not define accounting variants for `<unitPattern>`. In financial practice worldwide, accounting parentheses are strictly used alongside currency symbols and codes (`($1,230.05)` or `(1,230.05 USD)`), never with spelled-out unit names (`(1,230.05 US dollars)` does not exist). When ICU formats `FULL_NAME` with `ACCOUNTING`, it ignores the accounting sign and emits a standard minus sign, producing identical output to `STANDARD`.

### Rule 3: Exclude `currency_display="name"` with `currency_format_length="short"`
* **Rationale**: In CLDR LDML, compact short currency patterns `<currencyFormatLength type="short">` only define patterns with currency symbol placeholders (`¤0K`, `¤0M`). There are no compact patterns for spelled-out currency unit names. Spelled-out currency unit names in CLDR only exist with standard format length (`currency_format_length=""`).

---

## Style Combination Matrix

Applying the filtering rules yields the following **12 valid formatting styles**:

| `currency_format_length` | `currency_format_type` | `currency_display` | Valid Styles | Notes / Exclusions |
| :--- | :--- | :--- | :---: | :--- |
| *(empty)* (Standard) | `standard` | `symbol`, `symbolNarrow`, `code`, `name`, `noCurrency` | **5** | Standard plain decimal currency formatting across all 5 displays. |
| *(empty)* (Standard) | `accounting` | `symbol`, `symbolNarrow`, `code`, `noCurrency` | **4** | Accounting format with negative parentheses. (`name` excluded by Rule 2). |
| `short` (Compact Short) | `standard` | `symbol`, `symbolNarrow`, `code` | **3** | Compact decimal with currency sign (`¤0K`). (`noCurrency` excluded by Rule 1; `name` excluded by Rule 3). |

> [!NOTE]
> **No `accounting` format for `short`**: In CLDR LDML TR35, `<currencyFormatLength type="short">` only defines `type="standard"`. There is no `accounting` format type for compact short currency formatting in CLDR.

* **`currencies.tsv`**: Tests all **12 valid styles** ($5 + 4 + 3$).
* **`currencies_modern_locales.tsv`**: Tests all **10 valid styles** ($4 + 3 + 3$, excluding `noCurrency`).

---

## Test Data Suites & Selection Strategy

The test data is organized into core verification and optimized extended coverage suites. To enforce the **10,000-line maximum file size limit** and remove redundancy, extended suites exclude `noCurrency` and employ a **hybrid consolidation/splitting strategy** across exactly **10 files**:

```
common/testData/currency/
├── currencies.tsv                              (Core: 3,601 lines)
├── currencies_modern_locales.tsv               (Extended Locales: 3,859 lines)
├── currencies_symbol_modern_currencies.tsv     (Extended Currencies - symbol: 3,565 lines)
├── currencies_narrow_modern_currencies.tsv     (Extended Currencies - narrow: 3,565 lines)
├── currencies_code_modern_currencies.tsv       (Extended Currencies - code: 3,565 lines)
├── currencies_name_modern_currencies.tsv       (Extended Currencies - name: 1,189 lines)
├── currencies_symbol_extended_numbers.tsv      (Extended Numbers - symbol: 2,521 lines)
├── currencies_narrow_extended_numbers.tsv      (Extended Numbers - narrow: 2,521 lines)
├── currencies_code_extended_numbers.tsv        (Extended Numbers - code: 2,521 lines)
└── currencies_name_extended_numbers.tsv        (Extended Numbers - name: 841 lines)
```

---

### 1. Core Verification (`currencies.tsv`)
Contains core verification tests for representative numbers, major world currencies, and core locales illustrating key formatting features (including Indian grouping `bn`, Swiss 2-digit grouping `de_CH`, and suffix-minus `fy`). Covers the full Cartesian product across all 12 valid formatting styles.
* **Size**: **3,601 lines**

---

### 2. Extended Modern Locales (`currencies_modern_locales.tsv`)
Verification tests for all **modern-coverage** CLDR locales (**minus** core locales in `currencies.tsv`) formatting currencies across all 10 valid extended styles. Consolidated into a single file.
* **Currencies Tested**: All active legal tender currencies associated with the locale + 1 deterministic pseudo-random extra currency. (Universal anchor currencies `USD` and `EUR` are not duplicated here as they are comprehensively tested in `currencies.tsv`).
* **Numbers Tested**: Uses **`TINY_NUMBERS`** (`1.2` and `-1230.05`) to keep test volume minimal while testing both positive and negative decimal amounts.
* **Size**: **3,859 lines**

#### Currency Selection Strategy for Extended Modern Locales

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│               Extended Modern Locales Strategy (currencies_modern_locales.tsv)         │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ For each locale in extendedModernLocales:                                              │
│                                                                                        │
│   Currencies to Test:                                                                  │
│   ├── 1. Locale Currencies (All Legal Tender Currencies for Locale):                   │
│   │      └── { all active legal tender currencies for the locale's territory }         │
│   │          Uses SupplementalDataInfo to test all official currencies for the         │
│   │          locale (e.g. Bhutan (dz): { BTN, INR }, Haiti (ht): { HTG, USD },         │
│   │          Panama (es_PA): { PAB, USD }, Japan (ja): { JPY }, UK (en_GB): { GBP }).  │
│   │                                                                                    │
│   └── 2. Extra Currency (Deterministic Pseudo-Random Selection):                       │
│          └── { 1 stable hash-selected currency from remaining modern currencies }       │
│              Uses SHA-256(locale + "_" + currency) to select a diverse non-native      │
│              currency without git diff churn when locale lists change.                 │
│                                                                                        │
│   ───> Combined with:                                                                  │
│        • 10 Valid Extended Styles (from Style Combination Matrix)                      │
│        • TINY_NUMBERS: { 1.2, -1230.05 }                                               │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

### 3. Extended Modern Currencies (`currencies_<display>_modern_currencies.tsv`)
Verification tests for all **modern-coverage** CLDR currencies (**minus** major currencies in `currencies.tsv`) formatted across `TINY_LOCALES` (`en`, `ar`, `de`), representative native locales, and extra hash-selected locales using `TINY_NUMBERS` (`1.2`, `-1230.05`).

To eliminate redundancy and keep file sizes compact, **all test cases already covered in Suite 1 (`currencies.tsv`) or Suite 2 (`currencies_modern_locales.tsv`) are automatically deduplicated and excluded**, ensuring zero duplicate test cases across suites.

Split by `currency_display` into 4 separate files:
* **`currencies_symbol_modern_currencies.tsv`**: **3,565 lines** (deduplicated against Suites 1 & 2)
* **`currencies_narrow_modern_currencies.tsv`**: **3,565 lines** (deduplicated against Suites 1 & 2)
* **`currencies_code_modern_currencies.tsv`**: **3,565 lines** (deduplicated against Suites 1 & 2)
* **`currencies_name_modern_currencies.tsv`**: **1,189 lines** (only tests standard length/type; deduplicated)

#### Locale Selection Strategy for Extended Modern Currencies

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│          Extended Modern Currencies Strategy (currencies_<display>_modern_currencies)  │
├────────────────────────────────────────────────────────────────────────────────────────┤
│ For each currency in extendedModernCurrencies:                                         │
│                                                                                        │
│   Locales to Test:                                                                     │
│   ├── 1. Tiny Locales (Universal Anchor Set):                                          │
│   │      └── { "en", "ar", "de" }                                                      │
│   │          Covers Latin LTR, Arabic RTL/BiDi, and German compound/spacing.           │
│   │                                                                                    │
│   ├── 2. Representative Locales (Territory Native Locales):                            │
│   │      └── { locales that use this currency as official legal tender }               │
│   │          Ensures the currency is verified in its authentic linguistic context.     │
│   │                                                                                    │
│   └── 3. Extra Locale (Deterministic Pseudo-Random Selection):                         │
│          └── { 1 stable hash-selected locale from remaining modern locales }           │
│              Uses SHA-256(currency + "_" + locale) to test unexpected pairings.        │
│                                                                                        │
│   ───> Deduplication Filter:                                                           │
│        • Excludes all cases already covered in Suite 1 (currencies.tsv) or              │
│          Suite 2 (currencies_modern_locales.tsv)                                       │
│                                                                                        │
│   ───> Combined with:                                                                  │
│        • Valid Styles for the specific currency_display (from Style Combination Matrix)│
│        • TINY_NUMBERS: { 1.2, -1230.05 }                                               │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

---

### 4. Extended Numbers (`currencies_<display>_extended_numbers.tsv`)
Extended numeric test inputs (covering edge cases, negative values, large numbers, and small fractions) formatted across `TINY_LOCALES` (`en`, `ar`, `de`) and `TINY_CURRENCIES` (`USD`, `EUR`). Split by `currency_display` into 4 separate files:
* **`currencies_symbol_extended_numbers.tsv`**: **2,521 lines**
* **`currencies_narrow_extended_numbers.tsv`**: **2,521 lines**
* **`currencies_code_extended_numbers.tsv`**: **2,521 lines**
* **`currencies_name_extended_numbers.tsv`**: **841 lines** (only tests standard length/type)

---

## Total Suite Summary

* **Total Files**: 10 files
* **Total Lines**: **27,748 lines** (all files strictly $\le$ 3,860 lines, far below the 10,000-line limit)
* **Redundant/Duplicate Lines Eliminated**: $>13,000$ lines removed compared to naive generation

---

## Regenerating Test Data

To regenerate all TSV files, run:
```bash
mvn compile exec:java -Dexec.mainClass="org.unicode.cldr.tool.GenerateCurrencyFormatTestData" -pl tools/cldr-code
```

To run unit tests validating the generated files:
```bash
mvn test -Dtest=TestCurrencyFormat -pl tools/cldr-code
```

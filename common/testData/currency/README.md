# CLDR Currency Format Test Data (`common/testData/currency`)

This directory contains Tab-Separated Values (TSV) files used for testing standard, accounting, and compact currency formatting across CLDR locales.

---

## Dimensions

Each test case is defined by the following dimensions, mapped directly to CLDR LDML XML elements and Unicode Technical Standard (UTS) #35 specifications:

| Dimension Column | XML Source / Specification | Description | Allowed Values |
| :--- | :--- | :--- | :--- |
| **`locale`** | CLDR Locale Identifier | The locale under test (e.g. `en`, `ar`, `de_CH`, `bn`, `fy`). | Valid CLDR locales. |
| **`currency`** | ISO 4217 Currency Code | The 3-letter currency code (e.g. `USD`, `EUR`, `JPY`). | Valid ISO 4217 codes, or empty `""` for decimal fallback tests. |
| **`currency_format_length`** | `<currencyFormatLength type="...">` | Selects standard decimal vs. compact formatting. | `""` (standard length) or `short` (compact short). |
| **`currency_format_type`** | `<currencyFormat type="...">` | Selects sign display and negative formatting. | `standard` (minus sign) or `accounting` (parentheses). |
| **`currency_display`** | UTS #35 Section 3.2 & 3.3 | Controls how the currency symbol or unit is presented. | `symbol`, `symbolNarrow`, `code`, `name`, `noCurrency`. |
| **`input`** | Numeric amount | Floating-point test amount. | Representative doubles (e.g. `0.0`, `1.2`, `-1230.05`, `1234565.0`). |
| **`expected`** | Formatted output | Expected localized string including symbols, grouping, and BiDi marks. | Localized formatted string. |

### Detail on `currency_display` Values

The values in the `currency_display` column correspond directly to UTS #35 pattern token substitutions and pattern variants:
* **`symbol`**: The standard currency symbol (replaces `¤`, e.g., `$`, `€`).
* **`symbolNarrow`**: The narrow currency symbol variant (replaces `¤¤¤¤¤`, e.g., `$`).
* **`code`**: The 3-letter ISO 4217 code (replaces `¤¤`, e.g., `USD`, `EUR`).
* **`name`**: The localized currency unit name (replaces `¤¤¤` via `<unitPattern>{0} {1}</unitPattern>`, e.g., `US dollars`, `euros`).
* **`noCurrency`**: The currency symbol is suppressed using `<pattern alt="noCurrency">`. The number is formatted using the currency's fraction digits and monetary formatting rules, but without any currency symbol.

---

## Filtering Rules (Dimension Constraints)

To align strictly with CLDR LDML TR35 data and eliminate unsupported or redundant combinations, the test generator enforces the following filtering rules:

### Rule 1: Exclude `CurrencyDisplay.EMPTY`
* **Rationale**: UTS #35 defines explicit tokens (`¤`, `¤¤`, `¤¤¤`, `¤¤¤¤¤`, and `alt="noCurrency"`). There is no "empty" or default display token in CLDR specifications. Emitting an empty column to test ICU's internal fallback defaulting was removed in favor of explicit display tokens.

### Rule 2: Exclude `(short, accounting)`
* **Rationale**: In CLDR LDML XML, `<currencyFormatLength type="short">` only contains `<currencyFormat type="standard">`. There are no compact accounting patterns in CLDR. Resolving an absent compact accounting format is implementation-defined default behavior rather than a CLDR standard.

### Rule 3: Exclude `(short, noCurrency)`
* **Rationale**: Compact short currency patterns in CLDR only define patterns with currency symbol placeholders (`¤0K`, `¤0M`). There are **zero** `<pattern alt="noCurrency">` elements under `<currencyFormatLength type="short">` across all 371 CLDR locales. If a compact number is formatted without a currency sign, that is **Compact Decimal Formatting** (`<decimalFormatLength type="short">`), which is already tested in [`common/testData/decimal/`](../decimal/).

### Rule 4: Exclude `(accounting, name)` and `(short, name)`
* **Rationale**: In CLDR LDML, `<currencyFormat type="accounting">` and `<currencyFormatLength type="short">` only apply to currency signs (`symbol`, `symbolNarrow`, `code`). Spelled-out currency unit names (`name`) are defined completely separately under `<currencyFormats>` using:
  ```xml
  <unitPattern count="one">{0} {1}</unitPattern>
  <unitPattern count="other">{0} {1}</unitPattern>
  ```
  CLDR does not define accounting or compact variants for `<unitPattern>`. In financial practice worldwide, accounting parentheses are strictly used alongside currency symbols and codes (`($1,230.05)` or `(1,230.05 USD)`), never with spelled-out unit names (`(1,230.05 US dollars)` does not exist). When ICU formats `FULL_NAME` with `ACCOUNTING`, it ignores the accounting sign and emits a standard minus sign, producing identical output to `STANDARD`.
  
  Therefore, **`name` is strictly tested only with standard format length and standard format type (`""` + `standard`)**.

---

## Style Combination Matrix

Applying the filtering rules yields the following valid formatting styles:

| `currency_format_length` | `currency_format_type` | `currency_display` | Valid? | Reason / Notes |
| :--- | :--- | :--- | :---: | :--- |
| *(empty)* | `standard` | `symbol`, `symbolNarrow`, `code`, `name`, `noCurrency` | **Yes (5)** | Full standard plain decimal currency formatting across all 5 displays. |
| *(empty)* | `accounting` | `symbol`, `symbolNarrow`, `code`, `noCurrency` | **Yes (4)** | Accounting format with parentheses for negatives. `name` excluded by Rule 4. |
| *(empty)* | `accounting` | `name` | **No** | Excluded by Rule 4 (`<unitPattern>` has no accounting variant). |
| `short` | `standard` | `symbol`, `symbolNarrow`, `code` | **Yes (3)** | Compact short currency formatting with currency signs/codes. |
| `short` | `standard` | `noCurrency` | **No** | Excluded by Rule 3 (compact without currency is compact decimal). |
| `short` | `standard` | `name` | **No** | Excluded by Rule 4 (`<unitPattern>` has no compact variant). |
| `short` | `accounting` | *(any)* | **No** | Excluded by Rule 2 (CLDR has no compact accounting patterns). |

* **`currencies.tsv`**: Tests all **12 valid styles** ($5 + 4 + 3$).
* **`currencies_modern_locales.tsv`**: Tests all **10 valid styles** ($4 + 3 + 3$, excluding `noCurrency`).

---

## Test Data Suites & File Organization

The test data is organized into core verification and optimized extended coverage suites. To enforce the **10,000-line maximum file size limit** and remove redundancy, extended suites exclude `noCurrency` and employ a **hybrid consolidation/splitting strategy** across exactly **10 files**:

### 1. Core Verification
* **`currencies.tsv`**
  Contains core verification tests for representative numbers, major world currencies, and core locales illustrating key formatting features (including Indian grouping `bn`, Swiss 2-digit grouping `de_CH`, and suffix-minus `fy`). Covers the full Cartesian product across all 12 valid formatting styles.
  * **Size**: **3,601 lines**

### 2. Extended Modern Locales
* **`currencies_modern_locales.tsv`**
  Verification tests for all **modern-coverage** CLDR locales (**minus** core locales in `currencies.tsv`) formatting major currencies across all 10 valid extended styles. Consolidated into a single file.
  * **Size**: **7,299 lines**

### 3. Extended Modern Currencies
Verification tests for all **modern-coverage** CLDR currencies (**minus** major currencies in `currencies.tsv`) formatted across `TINY_LOCALES` (`en`, `ar`, `de`) and `TINY_NUMBERS` (`1.2`, `-1230.05`). Split by `currency_display` into 4 separate files:
* **`currencies_symbol_modern_currencies.tsv`**: **3,883 lines**
* **`currencies_narrow_modern_currencies.tsv`**: **3,883 lines**
* **`currencies_code_modern_currencies.tsv`**: **3,883 lines**
* **`currencies_name_modern_currencies.tsv`**: **1,293 lines** (only tests standard length/type)

### 4. Extended Numbers
Extended numeric test inputs (covering edge cases, negative values, large numbers, and small fractions) formatted across `TINY_LOCALES` (`en`, `ar`, `de`) and `TINY_CURRENCIES` (`USD`, `EUR`). Split by `currency_display` into 4 separate files:
* **`currencies_symbol_extended_numbers.tsv`**: **2,521 lines**
* **`currencies_narrow_extended_numbers.tsv`**: **2,521 lines**
* **`currencies_code_extended_numbers.tsv`**: **2,521 lines**
* **`currencies_name_extended_numbers.tsv`**: **841 lines** (only tests standard length/type)

---

## Total Suite Summary

* **Total Files**: 10 files
* **Total Lines**: **32,246 lines** (all files strictly $\le$ 8,000 lines, well below the 10,000-line limit)
* **Redundant/Duplicate Lines Eliminated**: $>8,600$ lines removed compared to naive generation

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

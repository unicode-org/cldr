# CLDR Currency Format Test Data (`common/testData/currency`)

This directory contains Tab-Separated Values (TSV) files used for testing standard, accounting, and compact currency formatting across CLDR locales.

---

## Dimensions

Each test case is defined by the following dimensions, mapped directly to CLDR LDML XML elements, Unicode Technical Standard (UTS) #35 specifications, and ECMA-402 Intl standards:

| Dimension Column | Specification & Source | Description | Allowed Values |
| :--- | :--- | :--- | :--- |
| **`locale`** | CLDR Locale Identifier | The locale under test (e.g. `en`, `ar`, `de_CH`, `bn`, `fy`). | Valid CLDR locales. |
| **`currency`** | ISO 4217 Currency Code | The 3-letter currency code (e.g. `USD`, `EUR`, `JPY`). | Valid ISO 4217 codes. |
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

## File Format

All files are UTF-8 TSV, and every line has the same number of tabs. The first line is a comment that starts with `#` and names the columns:

```tsv
# locale	currency	currency_format_length	currency_format_type	currency_display	input	expected
```

## Using the Files in Tests

For each line that does not start with `#`:

1. Set up a currency formatter for `locale` and `currency`, with the options given by `currency_format_length`, `currency_format_type`, and `currency_display`. An empty `currency_format_length` means the standard (non-compact) length.
2. Format `input`.
3. Compare the result with `expected`. The comparison is exact, so it includes any BiDi marks and no-break spaces.

---

## Test Data Suites & Selection Strategy

Lines that would be present in multiple files according to the descriptions below may be omitted.

### 1. Core Verification (`currencies.tsv`)
Contains core verification tests for representative numbers, major world currencies, and core locales illustrating key formatting features (including Indian grouping `bn`, Swiss 2-digit grouping `de_CH`, and suffix-minus `fy`). Covers the full Cartesian product across all formatting styles.

### 2. Extended Modern Locales (`currencies_modern_locales.tsv`)
Verification tests for the other **modern-coverage** CLDR locales, across all formatting styles except `noCurrency`.
* **Currencies Tested**: All active legal tender currencies associated with the locale + 1 deterministic pseudo-random extra currency.
* **Numbers Tested**: `1.2` and `-1230.05`.

### 3. Extended Modern Currencies (`currencies_<display>_modern_currencies.tsv`)
Verification tests for the other **modern-coverage** CLDR currencies, formatted in `en`, `ar`, and `de`, in the locales that use the currency as legal tender, and in 1 deterministic pseudo-random extra locale, using `1.2` and `-1230.05`.

Split by `currency_display` into 4 files:
* **`currencies_symbol_modern_currencies.tsv`**
* **`currencies_narrow_modern_currencies.tsv`**
* **`currencies_code_modern_currencies.tsv`**
* **`currencies_name_modern_currencies.tsv`** (only tests standard length/type)

### 4. Extended Numbers (`currencies_<display>_extended_numbers.tsv`)
Extended numeric test inputs (covering edge cases, negative values, large numbers, and small fractions) formatted in `en`, `ar`, and `de` with `USD` and `EUR`. Split by `currency_display` into 4 files:
* **`currencies_symbol_extended_numbers.tsv`**
* **`currencies_narrow_extended_numbers.tsv`**
* **`currencies_code_extended_numbers.tsv`**
* **`currencies_name_extended_numbers.tsv`** (only tests standard length/type)

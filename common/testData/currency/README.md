# CLDR Currency Format Test Data (`common/testData/currency`)

This directory contains Tab-Separated Values (TSV) files used for testing standard, accounting, and compact currency formats in CLDR.

## Test Data Files

The test data is organized into core verification and optimized extended coverage suites. To strictly enforce the **10,000-line maximum file size limit** and remove massive redundancy, the extended suites exclude the `NO_CURRENCY` display style (which hides the symbol, making most currencies format identically) and employ a **hybrid consolidation/splitting strategy** (reducing the total file count from 45 to exactly **10 files**):

1. **`currencies.tsv`**
   Contains core verification tests for a selected set of representative numbers, major world currencies, and core locales that illustrate most features of currency formatting. It covers the full Cartesian product of the core dimensions, including all 18 valid formatting styles (3 valid format length/type pairs × 6 currency displays). It also includes special layouts like Indian grouping (`bn`), Swiss 2-digit grouping (`de_CH`), and suffix-minus formatting (`fy`). Total size: **5,401 lines**.

2. **`currencies_modern_locales.tsv` (Extended Modern Locales)**
   Contains verification tests for all **modern-coverage** CLDR locales (**minus** the core locales covered in `currencies.tsv`) formatting major currencies across all 12 valid combinations of format length, type, and display (Styles, excluding the redundant `NO_CURRENCY` style). Since it is already well under 10,000 lines, it remains consolidated as a single file. Total size: **8,755 lines**.

3. **`currencies_<currency_display>_modern_currencies.tsv` (Extended Modern Currencies)**
   Contains verification tests for all **modern-coverage** CLDR currencies (**minus** the major currencies covered in `currencies.tsv`) formatted across `TINY_LOCALES` (`en`, `ar`, `de`) and `TINY_NUMBERS` (`1.2`, `-1230.05`) across all 3 valid Style Pairs. 
   To strictly respect the 10,000-line limit and remove redundancy, it is **split by `currency_display` into 4 separate files** (excluding `noCurrency`): `symbol`, `narrow` [for symbolNarrow], `code` [for ISO code], and `name` (e.g., `currencies_symbol_modern_currencies.tsv`, `currencies_narrow_modern_currencies.tsv`, etc.).
   *   Each file is **3,877–3,883 lines**, well under the 10,000-line limit.

4. **`currencies_<currency_display>_extended_numbers.tsv` (Extended Numbers)**
   Contains extended numeric test inputs (covering edge cases, negative values, large numbers, and small fractions) formatted across `TINY_LOCALES` (`en`, `ar`, `de`) and `TINY_CURRENCIES` (`USD`, `EUR`) across all 3 valid Style Pairs.
   To strictly respect the 10,000-line limit, it is **split by `currency_display` into 4 separate files** (using the same naming convention as above, excluding `noCurrency`, e.g., `currencies_symbol_extended_numbers.tsv`, `currencies_narrow_extended_numbers.tsv`, etc.).
   *   Each file is **2,521 lines**, well under the 10,000-line limit.

## File Format

All files use a standard Tab-Separated Values (TSV) format with UTF-8 encoding. The first line is always a header:

```tsv
locale	currency	currency_format_length	currency_format_type	currency_display	input	expected
```

### Valid Format Length / Type Pairs

The generator emits only the combinations that exist in CLDR data, mirroring the structure of
`<currencyFormats>` in the locale XML:

```xml
<currencyFormatLength>              <!-- no type: the standard pattern -->
  <currencyFormat type="standard">
  <currencyFormat type="accounting">
<currencyFormatLength type="short"> <!-- compact -->
  <currencyFormat type="standard">
```

| `currency_format_length` | `currency_format_type` |
| --- | --- |
| *(empty)* | `standard` |
| *(empty)* | `accounting` |
| `short` | `standard` |

An empty `currency_format_length` is meaningful: it corresponds to `<currencyFormatLength>` with
no `type` attribute, i.e. the standard pattern with a plain decimal format. By contrast
`currency_format_type` is **never** empty — every `<currencyFormat>` in CLDR carries a type — and
there is no `short` + `accounting` pattern. Resolving an absent type, or a compact accounting
format, is implementation-defined default behavior rather than something LDML/TR35 specifies, so
those combinations are deliberately not tested here.

### Column Definitions:
* **`locale`**: The CLDR locale identifier (e.g., `ar`, `de_CH`, `en`).
* **`currency`**: The 3-letter ISO 4217 currency code (e.g., `USD`, `EUR`, `JPY`). Can be empty for "no currency" tests.
* **`currency_format_length`**: The currency format length (`short`, or empty). An empty value selects the standard pattern with a plain decimal format; `short` selects the compact short format.
* **`currency_format_type`**: The currency format type (`standard`, `accounting`). Always present; never empty.
* **`currency_display`**: The currency representation style (`symbol`, `symbolNarrow`, `code`, `name`, `noCurrency`).
* **`input`**: The floating-point numeric input value (e.g., `1.2`, `-1230.05`, `1234565.0`).
* **`expected`**: The expected output string, including all correct localized digits, currency symbols/names, accounting parentheses, grouping separators, and bi-directional control marks.

## How to Read and Use the Files

### Manual Verification
Because the files use plain TSV formatting, they can be loaded directly into spreadsheet tools or inspected via standard text editors to audit expected localized currency formatting behavior across different languages.

### Regenerating Test Data
If formatting rules or underlying ICU4J implementations change, the test data files can be regenerated by running the generator tool:
`org.unicode.cldr.tool.GenerateCurrencyFormatTestData`

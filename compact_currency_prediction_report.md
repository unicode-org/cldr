# CLDR Compact Currency Pattern Prediction Report

This report analyzes whether compact currency patterns can be predicted by combining compact decimal patterns with standard currency layouts.

## Executive Summary

*   **Total test cases (Locale/NS/Power/Count):** 41544
*   **Standard Compact Matches:** 36822 (88.63%)
*   **Alpha Next to Number Compact Matches:** 23401 (56.33%)
*   **Locales with at least one mismatch:** 53

### Key Findings

*   **High Spacing Inconsistency in Alpha Patterns (73.52% of Alpha mismatches):** Most Alpha mismatches are due to missing spacing in CLDR compact patterns, even when standard currency patterns specify spacing.
*   **Missing Compact Currency Patterns (36.59% of Std mismatches):** Many numbering systems have compact decimal formats but lack compact currency formats, causing fallback to standard formatting.
*   **Partial Overrides Cause Layout Inconsistencies:** Overriding standard currency patterns but not compact patterns in a numbering system leads to prefix/suffix inconsistencies (e.g., standard is prefix, compact is suffix).
*   **Terminology Discrepancies:** Some locales use different compact suffixes for currency vs decimal formats.

## Match Rates

| Pattern Type | Matches | Total Cases | Match % |
| --- | --- | --- | --- |
| Standard Compact | 36822 | 41544 | 88.63% |
| Alpha Next to Number | 23401 | 41544 | 56.33% |

## Mismatch Analysis

### Standard Compact Mismatches Breakdown

Total Standard Mismatches: 4722

| Reason | Count | % of Mismatches | Description |
| --- | --- | --- | --- |
| Actual is missing | 1728 | 36.59% | Compact decimal pattern exists, but actual compact currency pattern is missing (null) in CLDR. |
| Spacing mismatch | 72 | 1.52% | Patterns match if spaces are ignored, but differ in spacing. |
| Pattern mismatch | 2922 | 61.88% | Real differences in patterns (e.g. prefix/suffix swap, different suffixes). |
| Predicted is missing | 0 | 0.00% | Compact decimal is missing, but compact currency exists (unexpected). |

### Alpha Next to Number Mismatches Breakdown

Total Alpha Mismatches: 18143

| Reason | Count | % of Mismatches | Description |
| --- | --- | --- | --- |
| Spacing mismatch | 13339 | 73.52% | Patterns are identical if spaces are ignored. Usually due to missing space in actual compact alpha (inherited from standard compact). |
| Actual is missing | 1728 | 9.52% | Compact decimal pattern exists, but actual compact currency pattern is missing in CLDR. |
| Pattern mismatch | 3076 | 16.95% | Real differences in patterns. |
| Predicted is missing | 0 | 0.00% | Compact decimal is missing, but compact currency exists. |

## Key Findings & Examples

### 1. Spacing Mismatches in Alpha Patterns (73.52% of Alpha mismatches)
A large majority of Alpha mismatches are due to missing spacing next to the currency symbol in the actual CLDR compact patterns, even though the standard currency format specifies spacing.

**Example:** `af/latn` (Afrikaans)
*   Standard Currency Alt Alpha: `¤ #,##0.00` (has space after `¤`)
*   Decimal Compact Short (1000): `0 k`
*   Predicted Alpha: `¤ 0 k` (spacing applied)
*   Actual Alpha: `¤0 k` (no space after `¤` in CLDR)

### 2. Missing Actual Currency Compact Patterns (36.59% of Std mismatches)
For some numbering systems, CLDR defines compact decimal patterns but completely lacks compact currency patterns. In these cases, formatting falls back to standard non-compact currency.

**Example:** `ar/arab` (Arabic, Arabic digits)
*   Decimal Compact Short (1000): `0 ألف`
*   Actual Compact Currency: *Missing* (falls back to standard `‏#,##0.00 ¤`)
*   Predicted Compact Currency: `‏0 ألف ¤`

### 3. Inconsistent Layouts (Real Pattern Mismatches)
In some cases, the layout of compact currency does not match standard currency (e.g. prefix vs suffix), often due to partial overrides in numbering systems.

**Example (Prefix/Suffix Inconsistency):** `no/mlym` (Norwegian, Malayalam digits)
*   Standard Currency Pattern (`mlym`): `¤ #,##0.00` (Prefix symbol)
*   Actual Compact Currency (`mlym`): `0k ¤` (Suffix symbol - fell back to `latn` compact currency)
*   Predicted Compact Currency: `¤ 0k` (Prefix symbol - predicted from standard `mlym` currency)
This happens because `no/mlym` overrides standard currency to be prefix, but doesn't override compact currency, which then falls back to `latn` (suffix).

**Example (Suffix Difference):** `kok/talu` (Konkani, New Tai Lue digits)
*   Decimal Compact Short ($10^{14}$): `0हज'.'نिख'.''` (Devanagari/other script suffix)
*   Actual Compact Currency ($10^{14}$): `¤000LCr` (Latin script `LCr` suffix)
*   Predicted Compact Currency: `¤ 0हज'.'نिख'.''`
Here, the compact currency uses different terminology/script than compact decimal.

## Top Locales with Real Pattern Mismatches
Below are some examples of locales and numbering systems with real pattern mismatches (excluding spacing and missing actuals).

| Locale/NS | Mismatch Count | Example Powers | Dec Pattern | Actual | Predicted |
| --- | --- | --- | --- | --- | --- |
| no/mlym | 72 | 1000 | `0k` | `0k ¤` | `¤ 0k` |
| no/talu | 72 | 1000 | `0k` | `0k ¤` | `¤ 0k` |
| no/guru | 72 | 1000 | `0k` | `0k ¤` | `¤ 0k` |
| no/deva | 72 | 1000 | `0k` | `0k ¤` | `¤ 0k` |
| no/hanidec | 72 | 1000 | `0k` | `0k ¤` | `¤ 0k` |
| no/gujr | 72 | 1000 | `0k` | `0k ¤` | `¤ 0k` |
| no/arabext | 72 | 1000 | `0k` | `0k ¤` | `¤ 0k` |
| no/fullwide | 72 | 1000 | `0k` | `0k ¤` | `¤ 0k` |
| no/osma | 72 | 1000 | `0k` | `0k ¤` | `¤ 0k` |
| no/telu | 72 | 1000 | `0k` | `0k ¤` | `¤ 0k` |
| no/knda | 72 | 1000 | `0k` | `0k ¤` | `¤ 0k` |
| no/beng | 72 | 1000 | `0k` | `0k ¤` | `¤ 0k` |
| no/bali | 72 | 1000 | `0k` | `0k ¤` | `¤ 0k` |
| no/java | 72 | 1000 | `0k` | `0k ¤` | `¤ 0k` |
| no/brah | 72 | 1000 | `0k` | `0k ¤` | `¤ 0k` |

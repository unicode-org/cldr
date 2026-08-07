# CLDR Compact Currency Prediction Report - Alpha Next to Number Patterns

This report analyzes the prediction of **Alpha Next to Number** compact currency patterns (`alt="alphaNextToNumber"`).
Prediction formula: Alpha Next to Number Standard Currency Layout + Decimal Compact Pattern.

## Executive Summary

*   **Total Cases Analyzed:** 41544
*   **Explicit Alpha Data Exists:** 5159 (12.42%)
*   **Explicit Alpha Data Missing (Non-Redundant Fallback):** 14137 (34.03%)
*   **Redundant (No Alpha Difference):** 20520 (49.39%)
*   **Completely Missing (No Compact Currency):** 1728 (4.16%)

### Summary Table

| Data Category | Case Count | Percentage | Matches | Spacing Mismatches | Pattern Mismatches | Match Rate |
| --- | --- | --- | --- | --- | --- | --- |
| **Explicit Alpha Exists** | 5159 | 12.42% | 5005 | 0 | 154 | **97.01%** |
| **Explicit Alpha Missing (Fallback)** | 14137 | 34.03% | 798 | 13339 | 0 | **5.64%** (Spacing Bugs: 94.36%) |
| **Redundant (No Layout/Compact Diff)** | 20520 | 49.39% | N/A | N/A | N/A | N/A (Neglected/Identical to Std) |
| **Completely Missing** | 1728 | 4.16% | N/A | N/A | N/A | N/A (Standard Fallback) |
| **Total** | 41544 | 100.00% | | | | |

### Locale / Numbering System Match Rates (Excluding Redundant & Missing)

#### 1. Explicit Alpha Data (When defined in CLDR)
*   **Total Locale/NS Pairs with Explicit Alpha:** 264
*   **Fully Matching Pairs:** 187 (70.83%)
*   **Pairs with Mismatches:** 77 (29.17%)

#### 2. Fallback Alpha Data (Non-Redundant Fallback to Standard Compact)
*   **Total Locale/NS Pairs in Fallback:** 267
*   **Fully Matching Pairs:** 4 (1.50%)
*   **Pairs with Mismatches (Potential Bugs):** 263 (98.50%)
    *   *Note: Of these, 263 pairs have spacing mismatches (potential bugs).*
*   **Locales completely missing compact data:** 24 pairs

## Analysis of Explicit Alpha Patterns (When they exist in CLDR)

Total cases where explicit alpha compact exists: 5159

### Real Pattern Mismatches (Explicit Alpha)
| Locale/NS | Power | Count | Dec Pattern | Actual Alpha | Predicted Alpha |
| --- | --- | --- | --- | --- | --- |
| kok/talu | 100000000000000 | one | `0हज'.'निख'.'` | `¤000LCr` | `¤ 0हज'.'निख'.'` |
| kok/talu | 100000000000000 | other | `0हज'.'निख'.'` | `¤000LCr` | `¤ 0हज'.'निख'.'` |
| kok/bhks | 100000000000000 | one | `0हज'.'निख'.'` | `¤000LCr` | `¤ 0हज'.'निख'.'` |
| kok/bhks | 100000000000000 | other | `0हज'.'निख'.'` | `¤000LCr` | `¤ 0हज'.'निख'.'` |
| kok/guru | 100000000000000 | one | `0हज'.'निख'.'` | `¤000LCr` | `¤ 0हज'.'निख'.'` |
| kok/guru | 100000000000000 | other | `0हज'.'निख'.'` | `¤000LCr` | `¤ 0हज'.'निख'.'` |
| kok/arabext | 100000000000000 | one | `0हज'.'निख'.'` | `¤000LCr` | `¤ 0हज'.'निख'.'` |
| kok/arabext | 100000000000000 | other | `0हज'.'निख'.'` | `¤000LCr` | `¤ 0हज'.'निख'.'` |
| kok/gujr | 100000000000000 | one | `0हज'.'निख'.'` | `¤000LCr` | `¤ 0हज'.'निख'.'` |
| kok/gujr | 100000000000000 | other | `0हज'.'निख'.'` | `¤000LCr` | `¤ 0हज'.'निख'.'` |

## Analysis of Missing Explicit Alpha (Fallback to Standard Compact)

Total cases falling back to standard compact: 14137
In these cases, CLDR does not define a separate alpha compact pattern, so it inherits the standard compact pattern. We compare our prediction (which expects spacing if the standard currency layout has it) against this fallback standard compact pattern.

### Potential CLDR Spacing Bugs (Spacing Mismatches in Fallback)
These are cases where standard currency has spacing (e.g. `¤ #,##0.00`), but the resolved compact currency (fallback) lacks it (e.g. `¤0K` instead of `¤ 0K`).
Affects 263 unique locales/numbering systems.

| Locale/NS | Power | Count | Dec Pattern | Actual (Fallback Std) | Predicted Alpha (With Spacing) |
| --- | --- | --- | --- | --- | --- |
| af/latn | 1000 | zero | `0 k` | `¤0 k` | `¤ 0 k` |
| af/latn | 1000 | two | `0 k` | `¤0 k` | `¤ 0 k` |
| af/latn | 1000 | few | `0 k` | `¤0 k` | `¤ 0 k` |
| af/latn | 1000 | many | `0 k` | `¤0 k` | `¤ 0 k` |
| af/latn | 10000 | zero | `00 k` | `¤00 k` | `¤ 00 k` |
| af/latn | 10000 | two | `00 k` | `¤00 k` | `¤ 00 k` |
| af/latn | 10000 | few | `00 k` | `¤00 k` | `¤ 00 k` |
| af/latn | 10000 | many | `00 k` | `¤00 k` | `¤ 00 k` |
| af/latn | 100000 | zero | `000 k` | `¤000 k` | `¤ 000 k` |
| af/latn | 100000 | two | `000 k` | `¤000 k` | `¤ 000 k` |
| af/latn | 100000 | few | `000 k` | `¤000 k` | `¤ 000 k` |
| af/latn | 100000 | many | `000 k` | `¤000 k` | `¤ 000 k` |
| af/latn | 1000000 | zero | `0 m` | `¤0 m` | `¤ 0 m` |
| af/latn | 1000000 | two | `0 m` | `¤0 m` | `¤ 0 m` |
| af/latn | 1000000 | few | `0 m` | `¤0 m` | `¤ 0 m` |

*... and 13324 more potential spacing bugs.*

## Completely Missing Data

Locales/NS where no compact currency (neither standard nor alpha) is defined.

| Locale/NS | Missing Powers (Example) |
| --- | --- |
| ar/arab | 1000, 10000, 100000, 1000000, 10000000, ... |
| az/arab | 1000, 10000, 100000, 1000000, 10000000, ... |
| bs/arab | 1000, 10000, 100000, 1000000, 10000000, ... |
| eu/arab | 1000, 10000, 100000, 1000000, 10000000, ... |
| fa/arab | 1000, 10000, 100000, 1000000, 10000000, ... |
| ga/arab | 1000, 10000, 100000, 1000000, 10000000, ... |
| ha/arab | 1000, 10000, 100000, 1000000, 10000000, ... |
| ig/arab | 1000, 10000, 100000, 1000000, 10000000, ... |
| kk/arab | 1000, 10000, 100000, 1000000, 10000000, ... |
| ky/arab | 1000, 10000, 100000, 1000000, 10000000, ... |
| mn/arab | 1000, 10000, 100000, 1000000, 10000000, ... |
| ms/arab | 1000, 10000, 100000, 1000000, 10000000, ... |
| nl/arab | 1000, 10000, 100000, 1000000, 10000000, ... |
| no/arab | 1000, 10000, 100000, 1000000, 10000000, ... |
| pa/arab | 1000, 10000, 100000, 1000000, 10000000, ... |
| ps/arab | 1000, 10000, 100000, 1000000, 10000000, ... |
| sd/arab | 1000, 10000, 100000, 1000000, 10000000, ... |
| so/arab | 1000, 10000, 100000, 1000000, 10000000, ... |
| tk/arab | 1000, 10000, 100000, 1000000, 10000000, ... |
| ur/arab | 1000, 10000, 100000, 1000000, 10000000, ... |

*... and 4 more locales/NS with completely missing data.*

## Expert Assessment & Recommendations

### 1. Real Pattern Mismatches in Explicit Alpha (e.g. `kok/talu`)
**Status:** **CLDR Data Quality Bug**

Mismatches where explicit alpha patterns exist but differ significantly (e.g., using Latin script `LCr` for Konkani compact currency while compact decimal uses Devanagari/other script) indicate translation or script coverage issues. Compact currency terminology and script must be aligned with compact decimal to avoid mixed-script formatting in a single locale.

### 2. Spacing Mismatches in Fallback (Potential CLDR Spacing Bugs)
**Status:** **CLDR Consistency Bug (Severity: Low-Medium, Volume: High)**

There are **13,339 cases** where explicit `alt="alphaNextToNumber"` compact patterns are missing, causing fallback to standard compact (lacking space). However, the standard currency format for these locales specifies that spacing *should* be present next to the symbol. This results in a loss of the spacing behavior in compact formats. CLDR should systematically generate explicit `alt="alphaNextToNumber"` compact patterns for all locales that require spacing in standard currency formats, or the formatting engine should be updated to dynamically inject spacing based on standard layout rules when compact alt-alpha is missing.

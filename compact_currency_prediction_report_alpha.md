# CLDR Compact Currency Prediction Report - Alpha Next to Number Patterns

This report analyzes the prediction of **Alpha Next to Number** compact currency patterns (`alt="alphaNextToNumber"`).
Prediction formula: Alpha Next to Number Standard Currency Layout + Decimal Compact Pattern.

## Executive Summary

*   **Total Cases Analyzed:** 41544
*   **Explicit Alpha Data Exists:** 5159 (12.42%)
*   **Explicit Alpha Data Missing (Fallback to Std Compact):** 34657 (83.42%)
*   **Completely Missing (No Compact Currency):** 1728 (4.16%)

## Analysis of Explicit Alpha Patterns (When they exist in CLDR)

Total cases where explicit alpha compact exists: 5159

| Status | Count | Percentage |
| --- | --- | --- |
| Matches | 5005 | 97.01% |
| Mismatches (Spacing only) | 0 | 0.00% |
| Mismatches (Pattern/Layout) | 154 | 2.99% |

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

Total cases falling back to standard compact: 34657
In these cases, CLDR does not define a separate alpha compact pattern, so it inherits the standard compact pattern. We compare our prediction (which expects spacing if the standard currency layout has it) against this fallback standard compact pattern.

| Status | Count | Percentage | Description |
| --- | --- | --- | --- |
| Matches | 18396 | 53.08% | Standard currency layout had no spacing, and fallback standard compact also had no spacing (consistent). |
| Mismatches (Spacing only) | 13339 | 38.49% | **Potential CLDR Bugs**: Standard currency layout specifies spacing, but fallback standard compact lacks it. |
| Mismatches (Pattern/Layout) | 2922 | 8.43% | Real pattern differences. |

### Potential CLDR Spacing Bugs (Spacing Mismatches in Fallback)
These are cases where standard currency has spacing (e.g. `¤ #,##0.00`), but the resolved compact currency (fallback) lacks it (e.g. `¤0K` instead of `¤ 0K`).

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

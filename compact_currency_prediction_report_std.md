# CLDR Compact Currency Prediction Report - Standard Patterns

This report analyzes the prediction of **Standard** compact currency patterns.
Prediction formula: Standard Currency Layout (Prefix/Suffix/Spacing) + Decimal Compact Pattern.

## Executive Summary

*   **Total Cases Analyzed:** 41544
*   **Data Exists (Compact Currency defined):** 39816 (95.84%)
*   **Data Missing (Fallback to Std Currency):** 1728 (4.16%)

## Match Rates (For cases where data exists)

Total cases with data: 39816

| Status | Count | Percentage |
| --- | --- | --- |
| Matches | 36822 | 92.48% |
| Mismatches (Spacing only) | 72 | 0.18% |
| Mismatches (Pattern/Layout) | 2922 | 7.34% |

## Mismatch Details (Data Exists)

### Spacing Mismatches
Patterns match if spaces are ignored, but differ in spacing.

| Locale/NS | Power | Count | Dec Pattern | Actual | Predicted |
| --- | --- | --- | --- | --- | --- |
| fa/arabext | 1000 | zero | `0 هزار` | `‎¤ 0 هزار` | `‎¤0 هزار` |
| fa/arabext | 1000 | one | `0 هزار` | `‎¤ 0 هزار` | `‎¤0 هزار` |
| fa/arabext | 1000 | two | `0 هزار` | `‎¤ 0 هزار` | `‎¤0 هزار` |
| fa/arabext | 1000 | few | `0 هزار` | `‎¤ 0 هزار` | `‎¤0 هزار` |
| fa/arabext | 1000 | many | `0 هزار` | `‎¤ 0 هزار` | `‎¤0 هزار` |
| fa/arabext | 1000 | other | `0 هزار` | `‎¤ 0 هزار` | `‎¤0 هزار` |
| fa/arabext | 10000 | zero | `00 هزار` | `‎¤ 00 هزار` | `‎¤00 هزار` |
| fa/arabext | 10000 | one | `00 هزار` | `‎¤ 00 هزار` | `‎¤00 هزار` |
| fa/arabext | 10000 | two | `00 هزار` | `‎¤ 00 هزار` | `‎¤00 هزار` |
| fa/arabext | 10000 | few | `00 هزار` | `‎¤ 00 هزار` | `‎¤00 هزار` |
| fa/arabext | 10000 | many | `00 هزار` | `‎¤ 00 هزار` | `‎¤00 هزار` |
| fa/arabext | 10000 | other | `00 هزار` | `‎¤ 00 هزار` | `‎¤00 هزار` |
| fa/arabext | 100000 | zero | `000 هزار` | `‎¤ 000 هزار` | `‎¤000 هزار` |
| fa/arabext | 100000 | one | `000 هزار` | `‎¤ 000 هزار` | `‎¤000 هزار` |
| fa/arabext | 100000 | two | `000 هزار` | `‎¤ 000 هزار` | `‎¤000 هزار` |

*... and 57 more spacing mismatches.*

### Real Pattern Mismatches
Differences in layout (e.g. prefix vs suffix) or terminology.

| Locale/NS | Power | Count | Dec Pattern | Actual | Predicted |
| --- | --- | --- | --- | --- | --- |
| no/mlym | 1000 | zero | `0k` | `0k ¤` | `¤ 0k` |
| no/mlym | 1000 | one | `0k` | `0k ¤` | `¤ 0k` |
| no/mlym | 1000 | two | `0k` | `0k ¤` | `¤ 0k` |
| no/mlym | 1000 | few | `0k` | `0k ¤` | `¤ 0k` |
| no/mlym | 1000 | many | `0k` | `0k ¤` | `¤ 0k` |
| no/mlym | 1000 | other | `0k` | `0k ¤` | `¤ 0k` |
| no/mlym | 10000 | zero | `00k` | `00k ¤` | `¤ 00k` |
| no/mlym | 10000 | one | `00k` | `00k ¤` | `¤ 00k` |
| no/mlym | 10000 | two | `00k` | `00k ¤` | `¤ 00k` |
| no/mlym | 10000 | few | `00k` | `00k ¤` | `¤ 00k` |
| no/mlym | 10000 | many | `00k` | `00k ¤` | `¤ 00k` |
| no/mlym | 10000 | other | `00k` | `00k ¤` | `¤ 00k` |
| no/mlym | 100000 | zero | `000k` | `000k ¤` | `¤ 000k` |
| no/mlym | 100000 | one | `000k` | `000k ¤` | `¤ 000k` |
| no/mlym | 100000 | two | `000k` | `000k ¤` | `¤ 000k` |
| no/mlym | 100000 | few | `000k` | `000k ¤` | `¤ 000k` |
| no/mlym | 100000 | many | `000k` | `000k ¤` | `¤ 000k` |
| no/mlym | 100000 | other | `000k` | `000k ¤` | `¤ 000k` |
| no/mlym | 1000000 | zero | `0 mill'.'` | `0 mill'.' ¤` | `¤ 0 mill'.'` |
| no/mlym | 1000000 | one | `0 mill'.'` | `0 mill'.' ¤` | `¤ 0 mill'.'` |

*... and 2902 more pattern mismatches.*

## Missing Data Details

Locales/NS where compact decimal is defined, but compact currency is missing (causing fallback to standard currency formatting).

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

*... and 4 more locales/NS with missing data.*

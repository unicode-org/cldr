# CLDR Compact Currency Prediction Report - Standard Patterns

This report analyzes the prediction of **Standard** compact currency patterns.
Prediction formula: Standard Currency Layout (Prefix/Suffix/Spacing) + Decimal Compact Pattern.

## Executive Summary

*   **Total Cases Analyzed:** 41544
*   **Data Exists (Compact Currency defined):** 39816 (95.84%)
*   **Data Missing (Fallback to Std Currency):** 1728 (4.16%)

### Summary Table

| Data Category | Case Count | Percentage | Matches | Spacing Mismatches | Pattern Mismatches | Match Rate (Exists) |
| --- | --- | --- | --- | --- | --- | --- |
| **Data Exists** | 39816 | 95.84% | 36822 | 72 | 2922 | **92.48%** |
| **Data Missing** | 1728 | 4.16% | N/A | N/A | N/A | N/A (Standard Fallback) |
| **Total** | 41544 | 100.00% | | | | |

### Locale / Numbering System Match Rates (Only where data exists)

*   **Total Locale/NS Pairs with Data:** 553
*   **Fully Matching Pairs:** 511 (92.41%)
*   **Pairs with Mismatches:** 42 (7.59%)
*   **Locales Completely Missing Data (Standard Fallback):** 24 pairs

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

## Expert Assessment & Recommendations

### 1. Spacing Mismatches & Placement Issues in Persian (e.g. `fa/arabext`)
**Status:** **CLDR Bug (Inconsistency & Linguistic Error)**

In Persian (`fa`), we observe two issues:
1.  **Spacing Inconsistency:** The actual compact pattern has space (`¤ 0 هزار`) while standard currency layout does not (`¤#,##0.00`). This inconsistency arises because compact patterns are defined under the `latn` numbering system (which has spacing in standard format) but are inherited by the `arabext` numbering system (which lacks spacing in standard format).
2.  **Linguistic Error (Placement):** Research on Persian currency usage (e.g., Wikipedia, Iranian news sites like IRNA, and e-commerce sites like Digikala) shows that the currency unit (whether the word 'ریال' / 'تومان' or the symbol '﷼') is **always placed after the number** (suffix). CLDR's standard pattern `‎¤#,##0.00` and compact pattern `‎¤ 0 هزار` both incorrectly place the symbol as a prefix.

**Recommendation:** CLDR should change the currency pattern for Persian (`fa`) to be suffix (e.g., `#,##0.00 ¤` for standard and `0 هزار ¤` for compact) to align with actual linguistic usage. Additionally, spacing should be consistent across numbering systems.

### 2. Real Pattern Mismatches due to Partial Fallbacks (e.g. `no/mlym`)
**Status:** **CLDR Bug (Severity: Medium)**

Many mismatches in non-default numbering systems (like Norwegian with Malayalam digits `no/mlym`) are caused by partial overrides. The locale overrides standard currency to be prefix (e.g., `¤ #,##0.00`), but does not override compact currency. As a result, compact currency falls back to `latn` layout, which is suffix (`0k ¤`). This leads to highly inconsistent formatting where standard amounts are prefix but compact amounts are suffix. CLDR needs to ensure that when a numbering system overrides standard layout, it also overrides compact layout to match, or inheritance rules must be updated to prevent cross-NS fallback from changing layout orientation.

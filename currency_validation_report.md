# CLDR Currency vs Decimal Pattern Validation Report

## Overall Locale Match Rates

| Locale Match Status | Count | Percentage |
| :--- | :---: | :---: |
| Fully Matching | 85 | 90.43% |
| With Mismatches | 9 | 9.57% |
| **Total Analyzed** | **94** | **100.00%** |

## Summary Statistics

### Decimal Integer Part vs Currency Integer Part
| Currency Pattern Type | Matches | Total Present | Match % |
| --- | --- | --- | --- |
| Standard | 566 | 577 | 98.09% |
| Standard (Alt Alpha) - Total | 566 | 577 | 98.09% |
| &nbsp;&nbsp;&nbsp;&nbsp;*Explicit (Different from Std)* | 263 | 272 | 96.69% |
| &nbsp;&nbsp;&nbsp;&nbsp;*Fallback (Same as Std / Missing)* | 303 | 305 | 99.34% |
| Standard (Alt No Currency) | 566 | 577 | 98.09% |
| Accounting | 561 | 577 | 97.23% |
| Accounting (Alt Alpha) - Total | 561 | 577 | 97.23% |
| &nbsp;&nbsp;&nbsp;&nbsp;*Explicit (Different from Acc)* | 265 | 276 | 96.01% |
| &nbsp;&nbsp;&nbsp;&nbsp;*Fallback (Same as Acc / Missing)* | 296 | 301 | 98.34% |
| Accounting (Alt No Currency) | 561 | 577 | 97.23% |

### Standard Currency Number Part vs Other Currency Number Parts
| Other Currency Pattern Type | Matches | Total Present | Match % |
| --- | --- | --- | --- |
| Standard (Alt Alpha) - Total | 577 | 577 | 100.00% |
| &nbsp;&nbsp;&nbsp;&nbsp;*Explicit (Different from Std)* | 272 | 272 | 100.00% |
| &nbsp;&nbsp;&nbsp;&nbsp;*Fallback (Same as Std / Missing)* | 305 | 305 | 100.00% |
| Standard (Alt No Currency) | 577 | 577 | 100.00% |
| Accounting | 568 | 577 | 98.44% |
| Accounting (Alt Alpha) - Total | 568 | 577 | 98.44% |
| &nbsp;&nbsp;&nbsp;&nbsp;*Explicit (Different from Acc)* | 270 | 276 | 97.83% |
| &nbsp;&nbsp;&nbsp;&nbsp;*Fallback (Same as Acc / Missing)* | 298 | 301 | 99.00% |
| Accounting (Alt No Currency) | 568 | 577 | 98.44% |

## Legend
*   **Status**: Compares the **integer part** of the pattern against the **Decimal Standard** integer part (Reference).
*   **StatusCurrency**: Compares the **whole number part** (integer + fraction, positive and negative) of the pattern against the **Currency Standard** whole number part (Reference).
*   <span style='background-color: #ffcccc; padding: 2px;'>Red Background</span>: Indicates a mismatch.

## Locales with Mismatches

### Locale: [`as`](common/main/as.xml)

#### Numbering System: `beng`

<table>
  <tr>
    <th>Locale/NS</th><th>Pattern Type</th><th>CLDR Pattern</th><th>Integer Structure</th><th>Status</th><th>StatusCurrency</th>
  </tr>
  <tr>
    <td>as/beng</td>
    <td>Decimal</td>
    <td><code>#,##,##0.###</code></td>
    <td><code>#,##,##0</code></td>
    <td>Reference</td>
    <td>N/A</td>
  </tr>
  <tr>
    <td>as/beng</td>
    <td>Currency Std</td>
    <td><code>¤ #,##,##0.00</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td>Reference</td>
  </tr>
  <tr>
    <td>as/beng</td>
    <td>Currency Std Alpha</td>
    <td><code>¤ #,##,##0.00</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td>OK</td>
  </tr>
  <tr>
    <td>as/beng</td>
    <td>Currency Std NoCurr</td>
    <td><code>#,##,##0.00</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td>OK</td>
  </tr>
  <tr>
    <td>as/beng</td>
    <td>Currency Acc</td>
    <td><code>¤#,##0.00;(¤#,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
  </tr>
  <tr>
    <td>as/beng</td>
    <td>Currency Acc Alpha</td>
    <td><code>¤ #,##0.00;(¤ #,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
  </tr>
  <tr>
    <td>as/beng</td>
    <td>Currency Acc NoCurr</td>
    <td><code>#,##0.00;(#,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
  </tr>
</table>

---

#### Numbering System: `latn`

<table>
  <tr>
    <th>Locale/NS</th><th>Pattern Type</th><th>CLDR Pattern</th><th>Integer Structure</th><th>Status</th><th>StatusCurrency</th>
  </tr>
  <tr>
    <td>as/latn</td>
    <td>Decimal</td>
    <td><code>#,##,##0.###</code></td>
    <td><code>#,##,##0</code></td>
    <td>Reference</td>
    <td>N/A</td>
  </tr>
  <tr>
    <td>as/latn</td>
    <td>Currency Std</td>
    <td><code>¤ #,##,##0.00</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td>Reference</td>
  </tr>
  <tr>
    <td>as/latn</td>
    <td>Currency Std Alpha</td>
    <td><code>¤ #,##,##0.00</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td>OK</td>
  </tr>
  <tr>
    <td>as/latn</td>
    <td>Currency Std NoCurr</td>
    <td><code>#,##,##0.00</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td>OK</td>
  </tr>
  <tr>
    <td>as/latn</td>
    <td>Currency Acc</td>
    <td><code>¤#,##0.00;(¤#,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
  </tr>
  <tr>
    <td>as/latn</td>
    <td>Currency Acc Alpha</td>
    <td><code>¤ #,##0.00;(¤ #,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
  </tr>
  <tr>
    <td>as/latn</td>
    <td>Currency Acc NoCurr</td>
    <td><code>#,##0.00;(#,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
  </tr>
</table>

---

### Locale: [`az`](common/main/az.xml)

#### Numbering System: `arab`

<table>
  <tr>
    <th>Locale/NS</th><th>Pattern Type</th><th>CLDR Pattern</th><th>Integer Structure</th><th>Status</th><th>StatusCurrency</th>
  </tr>
  <tr>
    <td>az/arab</td>
    <td>Decimal</td>
    <td><code>standart onluq kəsr0.###</code></td>
    <td><code>0</code></td>
    <td>Reference</td>
    <td>N/A</td>
  </tr>
  <tr>
    <td>az/arab</td>
    <td>Currency Std</td>
    <td><code>#,##0.00 ¤</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>Reference</td>
  </tr>
  <tr>
    <td>az/arab</td>
    <td>Currency Std Alpha</td>
    <td><code>#,##0.00 ¤</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>az/arab</td>
    <td>Currency Std NoCurr</td>
    <td><code>#,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>az/arab</td>
    <td>Currency Acc</td>
    <td><code>#,##0.00 ¤</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>az/arab</td>
    <td>Currency Acc Alpha</td>
    <td><code>#,##0.00 ¤</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>az/arab</td>
    <td>Currency Acc NoCurr</td>
    <td><code>#,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
</table>

---

#### Numbering System: `arabext`

<table>
  <tr>
    <th>Locale/NS</th><th>Pattern Type</th><th>CLDR Pattern</th><th>Integer Structure</th><th>Status</th><th>StatusCurrency</th>
  </tr>
  <tr>
    <td>az/arabext</td>
    <td>Decimal</td>
    <td><code>standart onluq kəsr0.###</code></td>
    <td><code>0</code></td>
    <td>Reference</td>
    <td>N/A</td>
  </tr>
  <tr>
    <td>az/arabext</td>
    <td>Currency Std</td>
    <td><code>#,##0.00 ¤</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>Reference</td>
  </tr>
  <tr>
    <td>az/arabext</td>
    <td>Currency Std Alpha</td>
    <td><code>#,##0.00 ¤</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>az/arabext</td>
    <td>Currency Std NoCurr</td>
    <td><code>#,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>az/arabext</td>
    <td>Currency Acc</td>
    <td><code>#,##0.00 ¤</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>az/arabext</td>
    <td>Currency Acc Alpha</td>
    <td><code>#,##0.00 ¤</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>az/arabext</td>
    <td>Currency Acc NoCurr</td>
    <td><code>#,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
</table>

---

#### Numbering System: `latn`

<table>
  <tr>
    <th>Locale/NS</th><th>Pattern Type</th><th>CLDR Pattern</th><th>Integer Structure</th><th>Status</th><th>StatusCurrency</th>
  </tr>
  <tr>
    <td>az/latn</td>
    <td>Decimal</td>
    <td><code>#,##0.###</code></td>
    <td><code>#,##0</code></td>
    <td>Reference</td>
    <td>N/A</td>
  </tr>
  <tr>
    <td>az/latn</td>
    <td>Currency Std</td>
    <td><code>#,##0.00 ¤</code></td>
    <td><code>#,##0</code></td>
    <td>OK</td>
    <td>Reference</td>
  </tr>
  <tr>
    <td>az/latn</td>
    <td>Currency Std Alpha</td>
    <td><code>#,##0.00 ¤</code></td>
    <td><code>#,##0</code></td>
    <td>OK</td>
    <td>OK</td>
  </tr>
  <tr>
    <td>az/latn</td>
    <td>Currency Std NoCurr</td>
    <td><code>#,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td>OK</td>
    <td>OK</td>
  </tr>
  <tr>
    <td>az/latn</td>
    <td>Currency Acc</td>
    <td><code>#,##0.00 ¤</code></td>
    <td><code>#,##0</code></td>
    <td>OK</td>
    <td>OK</td>
  </tr>
  <tr>
    <td>az/latn</td>
    <td>Currency Acc Alpha</td>
    <td><code>#,##0.00 ¤</code></td>
    <td><code>#,##0</code></td>
    <td>OK</td>
    <td>OK</td>
  </tr>
  <tr>
    <td>az/latn</td>
    <td>Currency Acc NoCurr</td>
    <td><code>#,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td>OK</td>
    <td>OK</td>
  </tr>
</table>

---

### Locale: [`gu`](common/main/gu.xml)

#### Numbering System: `gujr`

<table>
  <tr>
    <th>Locale/NS</th><th>Pattern Type</th><th>CLDR Pattern</th><th>Integer Structure</th><th>Status</th><th>StatusCurrency</th>
  </tr>
  <tr>
    <td>gu/gujr</td>
    <td>Decimal</td>
    <td><code>#,##,##0.###</code></td>
    <td><code>#,##,##0</code></td>
    <td>Reference</td>
    <td>N/A</td>
  </tr>
  <tr>
    <td>gu/gujr</td>
    <td>Currency Std</td>
    <td><code>¤#,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>Reference</td>
  </tr>
  <tr>
    <td>gu/gujr</td>
    <td>Currency Std Alpha</td>
    <td><code>¤ #,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>gu/gujr</td>
    <td>Currency Std NoCurr</td>
    <td><code>#,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>gu/gujr</td>
    <td>Currency Acc</td>
    <td><code>¤#,##,##0.00;(¤#,##,##0.00)</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
  </tr>
  <tr>
    <td>gu/gujr</td>
    <td>Currency Acc Alpha</td>
    <td><code>¤ #,##,##0.00;(¤ #,##,##0.00)</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
  </tr>
  <tr>
    <td>gu/gujr</td>
    <td>Currency Acc NoCurr</td>
    <td><code>#,##,##0.00;(#,##,##0.00)</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
  </tr>
</table>

---

#### Numbering System: `latn`

<table>
  <tr>
    <th>Locale/NS</th><th>Pattern Type</th><th>CLDR Pattern</th><th>Integer Structure</th><th>Status</th><th>StatusCurrency</th>
  </tr>
  <tr>
    <td>gu/latn</td>
    <td>Decimal</td>
    <td><code>#,##,##0.###</code></td>
    <td><code>#,##,##0</code></td>
    <td>Reference</td>
    <td>N/A</td>
  </tr>
  <tr>
    <td>gu/latn</td>
    <td>Currency Std</td>
    <td><code>¤#,##,##0.00</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td>Reference</td>
  </tr>
  <tr>
    <td>gu/latn</td>
    <td>Currency Std Alpha</td>
    <td><code>¤ #,##,##0.00</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td>OK</td>
  </tr>
  <tr>
    <td>gu/latn</td>
    <td>Currency Std NoCurr</td>
    <td><code>#,##,##0.00</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td>OK</td>
  </tr>
  <tr>
    <td>gu/latn</td>
    <td>Currency Acc</td>
    <td><code>¤#,##,##0.00;(¤#,##,##0.00)</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td>OK</td>
  </tr>
  <tr>
    <td>gu/latn</td>
    <td>Currency Acc Alpha</td>
    <td><code>¤ #,##,##0.00;(¤ #,##,##0.00)</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td>OK</td>
  </tr>
  <tr>
    <td>gu/latn</td>
    <td>Currency Acc NoCurr</td>
    <td><code>#,##,##0.00;(#,##,##0.00)</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td>OK</td>
  </tr>
</table>

---

### Locale: [`ml`](common/main/ml.xml)

#### Numbering System: `latn`

<table>
  <tr>
    <th>Locale/NS</th><th>Pattern Type</th><th>CLDR Pattern</th><th>Integer Structure</th><th>Status</th><th>StatusCurrency</th>
  </tr>
  <tr>
    <td>ml/latn</td>
    <td>Decimal</td>
    <td><code>#,##,##0.###</code></td>
    <td><code>#,##,##0</code></td>
    <td>Reference</td>
    <td>N/A</td>
  </tr>
  <tr>
    <td>ml/latn</td>
    <td>Currency Std</td>
    <td><code>¤#,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>Reference</td>
  </tr>
  <tr>
    <td>ml/latn</td>
    <td>Currency Std Alpha</td>
    <td><code>¤ #,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>ml/latn</td>
    <td>Currency Std NoCurr</td>
    <td><code>#,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>ml/latn</td>
    <td>Currency Acc</td>
    <td><code>¤#,##0.00;(¤#,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>ml/latn</td>
    <td>Currency Acc Alpha</td>
    <td><code>¤ #,##0.00;(¤ #,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>ml/latn</td>
    <td>Currency Acc NoCurr</td>
    <td><code>#,##0.00;(#,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
</table>

---

#### Numbering System: `mlym`

<table>
  <tr>
    <th>Locale/NS</th><th>Pattern Type</th><th>CLDR Pattern</th><th>Integer Structure</th><th>Status</th><th>StatusCurrency</th>
  </tr>
  <tr>
    <td>ml/mlym</td>
    <td>Decimal</td>
    <td><code>#,##,##0.###</code></td>
    <td><code>#,##,##0</code></td>
    <td>Reference</td>
    <td>N/A</td>
  </tr>
  <tr>
    <td>ml/mlym</td>
    <td>Currency Std</td>
    <td><code>¤#,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>Reference</td>
  </tr>
  <tr>
    <td>ml/mlym</td>
    <td>Currency Std Alpha</td>
    <td><code>¤ #,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>ml/mlym</td>
    <td>Currency Std NoCurr</td>
    <td><code>#,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>ml/mlym</td>
    <td>Currency Acc</td>
    <td><code>¤#,##0.00;(¤#,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>ml/mlym</td>
    <td>Currency Acc Alpha</td>
    <td><code>¤ #,##0.00;(¤ #,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>ml/mlym</td>
    <td>Currency Acc NoCurr</td>
    <td><code>#,##0.00;(#,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
</table>

---

### Locale: [`mr`](common/main/mr.xml)

#### Numbering System: `deva`

<table>
  <tr>
    <th>Locale/NS</th><th>Pattern Type</th><th>CLDR Pattern</th><th>Integer Structure</th><th>Status</th><th>StatusCurrency</th>
  </tr>
  <tr>
    <td>mr/deva</td>
    <td>Decimal</td>
    <td><code>#,##,##0.###</code></td>
    <td><code>#,##,##0</code></td>
    <td>Reference</td>
    <td>N/A</td>
  </tr>
  <tr>
    <td>mr/deva</td>
    <td>Currency Std</td>
    <td><code>¤#,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>Reference</td>
  </tr>
  <tr>
    <td>mr/deva</td>
    <td>Currency Std Alpha</td>
    <td><code>¤ #,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>mr/deva</td>
    <td>Currency Std NoCurr</td>
    <td><code>#,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>mr/deva</td>
    <td>Currency Acc</td>
    <td><code>¤#,##0.00;(¤#,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>mr/deva</td>
    <td>Currency Acc Alpha</td>
    <td><code>¤ #,##0.00;(¤ #,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>mr/deva</td>
    <td>Currency Acc NoCurr</td>
    <td><code>#,##0.00;(#,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
</table>

---

#### Numbering System: `latn`

<table>
  <tr>
    <th>Locale/NS</th><th>Pattern Type</th><th>CLDR Pattern</th><th>Integer Structure</th><th>Status</th><th>StatusCurrency</th>
  </tr>
  <tr>
    <td>mr/latn</td>
    <td>Decimal</td>
    <td><code>#,##,##0.###</code></td>
    <td><code>#,##,##0</code></td>
    <td>Reference</td>
    <td>N/A</td>
  </tr>
  <tr>
    <td>mr/latn</td>
    <td>Currency Std</td>
    <td><code>¤#,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>Reference</td>
  </tr>
  <tr>
    <td>mr/latn</td>
    <td>Currency Std Alpha</td>
    <td><code>¤ #,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>mr/latn</td>
    <td>Currency Std NoCurr</td>
    <td><code>#,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>mr/latn</td>
    <td>Currency Acc</td>
    <td><code>¤#,##0.00;(¤#,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>mr/latn</td>
    <td>Currency Acc Alpha</td>
    <td><code>¤ #,##0.00;(¤ #,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>mr/latn</td>
    <td>Currency Acc NoCurr</td>
    <td><code>#,##0.00;(#,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
</table>

---

### Locale: [`or`](common/main/or.xml)

#### Numbering System: `latn`

<table>
  <tr>
    <th>Locale/NS</th><th>Pattern Type</th><th>CLDR Pattern</th><th>Integer Structure</th><th>Status</th><th>StatusCurrency</th>
  </tr>
  <tr>
    <td>or/latn</td>
    <td>Decimal</td>
    <td><code>#,##,##0.###</code></td>
    <td><code>#,##,##0</code></td>
    <td>Reference</td>
    <td>N/A</td>
  </tr>
  <tr>
    <td>or/latn</td>
    <td>Currency Std</td>
    <td><code>¤#,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>Reference</td>
  </tr>
  <tr>
    <td>or/latn</td>
    <td>Currency Std Alpha</td>
    <td><code>¤ #,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>or/latn</td>
    <td>Currency Std NoCurr</td>
    <td><code>#,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>or/latn</td>
    <td>Currency Acc</td>
    <td><code>¤#,##0.00;(¤#,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>or/latn</td>
    <td>Currency Acc Alpha</td>
    <td><code>¤ #,##0.00;(¤ #,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>or/latn</td>
    <td>Currency Acc NoCurr</td>
    <td><code>#,##0.00;(#,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
</table>

---

#### Numbering System: `orya`

<table>
  <tr>
    <th>Locale/NS</th><th>Pattern Type</th><th>CLDR Pattern</th><th>Integer Structure</th><th>Status</th><th>StatusCurrency</th>
  </tr>
  <tr>
    <td>or/orya</td>
    <td>Decimal</td>
    <td><code>#,##,##0.###</code></td>
    <td><code>#,##,##0</code></td>
    <td>Reference</td>
    <td>N/A</td>
  </tr>
  <tr>
    <td>or/orya</td>
    <td>Currency Std</td>
    <td><code>¤#,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>Reference</td>
  </tr>
  <tr>
    <td>or/orya</td>
    <td>Currency Std Alpha</td>
    <td><code>¤ #,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>or/orya</td>
    <td>Currency Std NoCurr</td>
    <td><code>#,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>or/orya</td>
    <td>Currency Acc</td>
    <td><code>¤#,##0.00;(¤#,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>or/orya</td>
    <td>Currency Acc Alpha</td>
    <td><code>¤ #,##0.00;(¤ #,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>or/orya</td>
    <td>Currency Acc NoCurr</td>
    <td><code>#,##0.00;(#,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
</table>

---

### Locale: [`pa`](common/main/pa.xml)

#### Numbering System: `arab`

<table>
  <tr>
    <th>Locale/NS</th><th>Pattern Type</th><th>CLDR Pattern</th><th>Integer Structure</th><th>Status</th><th>StatusCurrency</th>
  </tr>
  <tr>
    <td>pa/arab</td>
    <td>Decimal</td>
    <td><code>#,##,##0.###</code></td>
    <td><code>#,##,##0</code></td>
    <td>Reference</td>
    <td>N/A</td>
  </tr>
  <tr>
    <td>pa/arab</td>
    <td>Currency Std</td>
    <td><code>¤#,##,##0.00</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td>Reference</td>
  </tr>
  <tr>
    <td>pa/arab</td>
    <td>Currency Std Alpha</td>
    <td><code>¤ #,##,##0.00</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td>OK</td>
  </tr>
  <tr>
    <td>pa/arab</td>
    <td>Currency Std NoCurr</td>
    <td><code>#,##,##0.00</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td>OK</td>
  </tr>
  <tr>
    <td>pa/arab</td>
    <td>Currency Acc</td>
    <td><code>¤#,##,##0.00</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td>OK</td>
  </tr>
  <tr>
    <td>pa/arab</td>
    <td>Currency Acc Alpha</td>
    <td><code>¤ #,##,##0.00</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td>OK</td>
  </tr>
  <tr>
    <td>pa/arab</td>
    <td>Currency Acc NoCurr</td>
    <td><code>#,##,##0.00</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td>OK</td>
  </tr>
</table>

---

#### Numbering System: `arabext`

<table>
  <tr>
    <th>Locale/NS</th><th>Pattern Type</th><th>CLDR Pattern</th><th>Integer Structure</th><th>Status</th><th>StatusCurrency</th>
  </tr>
  <tr>
    <td>pa/arabext</td>
    <td>Decimal</td>
    <td><code>#,##,##0.###</code></td>
    <td><code>#,##,##0</code></td>
    <td>Reference</td>
    <td>N/A</td>
  </tr>
  <tr>
    <td>pa/arabext</td>
    <td>Currency Std</td>
    <td><code>¤#,##,##0.00</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td>Reference</td>
  </tr>
  <tr>
    <td>pa/arabext</td>
    <td>Currency Std Alpha</td>
    <td><code>¤ #,##,##0.00</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td>OK</td>
  </tr>
  <tr>
    <td>pa/arabext</td>
    <td>Currency Std NoCurr</td>
    <td><code>#,##,##0.00</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td>OK</td>
  </tr>
  <tr>
    <td>pa/arabext</td>
    <td>Currency Acc</td>
    <td><code>¤ #,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
  </tr>
  <tr>
    <td>pa/arabext</td>
    <td>Currency Acc Alpha</td>
    <td><code>¤ #,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
  </tr>
  <tr>
    <td>pa/arabext</td>
    <td>Currency Acc NoCurr</td>
    <td><code>#,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
  </tr>
</table>

---

#### Numbering System: `guru`

<table>
  <tr>
    <th>Locale/NS</th><th>Pattern Type</th><th>CLDR Pattern</th><th>Integer Structure</th><th>Status</th><th>StatusCurrency</th>
  </tr>
  <tr>
    <td>pa/guru</td>
    <td>Decimal</td>
    <td><code>#,##,##0.###</code></td>
    <td><code>#,##,##0</code></td>
    <td>Reference</td>
    <td>N/A</td>
  </tr>
  <tr>
    <td>pa/guru</td>
    <td>Currency Std</td>
    <td><code>¤#,##,##0.00</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td>Reference</td>
  </tr>
  <tr>
    <td>pa/guru</td>
    <td>Currency Std Alpha</td>
    <td><code>¤ #,##,##0.00</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td>OK</td>
  </tr>
  <tr>
    <td>pa/guru</td>
    <td>Currency Std NoCurr</td>
    <td><code>#,##,##0.00</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td>OK</td>
  </tr>
  <tr>
    <td>pa/guru</td>
    <td>Currency Acc</td>
    <td><code>¤ #,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
  </tr>
  <tr>
    <td>pa/guru</td>
    <td>Currency Acc Alpha</td>
    <td><code>¤ #,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
  </tr>
  <tr>
    <td>pa/guru</td>
    <td>Currency Acc NoCurr</td>
    <td><code>#,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
  </tr>
</table>

---

#### Numbering System: `latn`

<table>
  <tr>
    <th>Locale/NS</th><th>Pattern Type</th><th>CLDR Pattern</th><th>Integer Structure</th><th>Status</th><th>StatusCurrency</th>
  </tr>
  <tr>
    <td>pa/latn</td>
    <td>Decimal</td>
    <td><code>#,##,##0.###</code></td>
    <td><code>#,##,##0</code></td>
    <td>Reference</td>
    <td>N/A</td>
  </tr>
  <tr>
    <td>pa/latn</td>
    <td>Currency Std</td>
    <td><code>¤#,##,##0.00</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td>Reference</td>
  </tr>
  <tr>
    <td>pa/latn</td>
    <td>Currency Std Alpha</td>
    <td><code>¤ #,##,##0.00</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td>OK</td>
  </tr>
  <tr>
    <td>pa/latn</td>
    <td>Currency Std NoCurr</td>
    <td><code>#,##,##0.00</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td>OK</td>
  </tr>
  <tr>
    <td>pa/latn</td>
    <td>Currency Acc</td>
    <td><code>¤ #,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
  </tr>
  <tr>
    <td>pa/latn</td>
    <td>Currency Acc Alpha</td>
    <td><code>¤ #,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
  </tr>
  <tr>
    <td>pa/latn</td>
    <td>Currency Acc NoCurr</td>
    <td><code>#,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
  </tr>
</table>

---

### Locale: [`ta`](common/main/ta.xml)

#### Numbering System: `latn`

<table>
  <tr>
    <th>Locale/NS</th><th>Pattern Type</th><th>CLDR Pattern</th><th>Integer Structure</th><th>Status</th><th>StatusCurrency</th>
  </tr>
  <tr>
    <td>ta/latn</td>
    <td>Decimal</td>
    <td><code>#,##,##0.###</code></td>
    <td><code>#,##,##0</code></td>
    <td>Reference</td>
    <td>N/A</td>
  </tr>
  <tr>
    <td>ta/latn</td>
    <td>Currency Std</td>
    <td><code>¤#,##,##0.00</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td>Reference</td>
  </tr>
  <tr>
    <td>ta/latn</td>
    <td>Currency Std Alpha</td>
    <td><code>¤ #,##,##0.00</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td>OK</td>
  </tr>
  <tr>
    <td>ta/latn</td>
    <td>Currency Std NoCurr</td>
    <td><code>#,##,##0.00</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td>OK</td>
  </tr>
  <tr>
    <td>ta/latn</td>
    <td>Currency Acc</td>
    <td><code>¤#,##0.00;(¤#,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
  </tr>
  <tr>
    <td>ta/latn</td>
    <td>Currency Acc Alpha</td>
    <td><code>¤ #,##0.00;(¤ #,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
  </tr>
  <tr>
    <td>ta/latn</td>
    <td>Currency Acc NoCurr</td>
    <td><code>#,##0.00;(#,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
  </tr>
</table>

---

#### Numbering System: `tamldec`

<table>
  <tr>
    <th>Locale/NS</th><th>Pattern Type</th><th>CLDR Pattern</th><th>Integer Structure</th><th>Status</th><th>StatusCurrency</th>
  </tr>
  <tr>
    <td>ta/tamldec</td>
    <td>Decimal</td>
    <td><code>#,##,##0.###</code></td>
    <td><code>#,##,##0</code></td>
    <td>Reference</td>
    <td>N/A</td>
  </tr>
  <tr>
    <td>ta/tamldec</td>
    <td>Currency Std</td>
    <td><code>¤#,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>Reference</td>
  </tr>
  <tr>
    <td>ta/tamldec</td>
    <td>Currency Std Alpha</td>
    <td><code>¤ #,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>ta/tamldec</td>
    <td>Currency Std NoCurr</td>
    <td><code>#,##0.00</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>ta/tamldec</td>
    <td>Currency Acc</td>
    <td><code>¤#,##0.00;(¤#,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>ta/tamldec</td>
    <td>Currency Acc Alpha</td>
    <td><code>¤ #,##0.00;(¤ #,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>ta/tamldec</td>
    <td>Currency Acc NoCurr</td>
    <td><code>#,##0.00;(#,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
</table>

---

### Locale: [`te`](common/main/te.xml)

#### Numbering System: `latn`

<table>
  <tr>
    <th>Locale/NS</th><th>Pattern Type</th><th>CLDR Pattern</th><th>Integer Structure</th><th>Status</th><th>StatusCurrency</th>
  </tr>
  <tr>
    <td>te/latn</td>
    <td>Decimal</td>
    <td><code>#,##,##0.###</code></td>
    <td><code>#,##,##0</code></td>
    <td>Reference</td>
    <td>N/A</td>
  </tr>
  <tr>
    <td>te/latn</td>
    <td>Currency Std</td>
    <td><code>¤#,##,##0.00</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td>Reference</td>
  </tr>
  <tr>
    <td>te/latn</td>
    <td>Currency Std Alpha</td>
    <td><code>¤ #,##,##0.00</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td>OK</td>
  </tr>
  <tr>
    <td>te/latn</td>
    <td>Currency Std NoCurr</td>
    <td><code>#,##,##0.00</code></td>
    <td><code>#,##,##0</code></td>
    <td>OK</td>
    <td>OK</td>
  </tr>
  <tr>
    <td>te/latn</td>
    <td>Currency Acc</td>
    <td><code>¤#,##0.00;(¤#,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
  </tr>
  <tr>
    <td>te/latn</td>
    <td>Currency Acc Alpha</td>
    <td><code>¤ #,##0.00;(¤ #,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
  </tr>
  <tr>
    <td>te/latn</td>
    <td>Currency Acc NoCurr</td>
    <td><code>#,##0.00;(#,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
  </tr>
</table>

---

#### Numbering System: `telu`

<table>
  <tr>
    <th>Locale/NS</th><th>Pattern Type</th><th>CLDR Pattern</th><th>Integer Structure</th><th>Status</th><th>StatusCurrency</th>
  </tr>
  <tr>
    <td>te/telu</td>
    <td>Decimal</td>
    <td><code>#,##0.###</code></td>
    <td><code>#,##0</code></td>
    <td>Reference</td>
    <td>N/A</td>
  </tr>
  <tr>
    <td>te/telu</td>
    <td>Currency Std</td>
    <td><code>¤#,##,##0.00</code></td>
    <td><code>#,##,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>Reference</td>
  </tr>
  <tr>
    <td>te/telu</td>
    <td>Currency Std Alpha</td>
    <td><code>¤ #,##,##0.00</code></td>
    <td><code>#,##,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>te/telu</td>
    <td>Currency Std NoCurr</td>
    <td><code>#,##,##0.00</code></td>
    <td><code>#,##,##0</code></td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
    <td>OK</td>
  </tr>
  <tr>
    <td>te/telu</td>
    <td>Currency Acc</td>
    <td><code>¤#,##0.00;(¤#,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td>OK</td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
  </tr>
  <tr>
    <td>te/telu</td>
    <td>Currency Acc Alpha</td>
    <td><code>¤ #,##0.00;(¤ #,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td>OK</td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
  </tr>
  <tr>
    <td>te/telu</td>
    <td>Currency Acc NoCurr</td>
    <td><code>#,##0.00;(#,##0.00)</code></td>
    <td><code>#,##0</code></td>
    <td>OK</td>
    <td style='background-color: #ffcccc;'><span style='color: red; font-weight: bold;'>MISMATCH</span></td>
  </tr>
</table>

---

## Matching Locales
These locales have no mismatches in any of their numbering systems.

<details>
<summary>Click to expand matching locales</summary>

`af`, `am`, `ar`, `be`, `bg`, `bn`, `bs`, `ca`, `cs`, `cy`, `da`, `de`, `el`, `en`, `es`, `et`, `eu`, `fa`, `fi`, `fil`, `fr`, `ga`, `gd`, `gl`, `ha`, `he`, `hi`, `hi_Latn`, `hr`, `hu`, `hy`, `id`, `ig`, `is`, `it`, `ja`, `jv`, `ka`, `kk`, `km`, `kn`, `ko`, `kok`, `ky`, `lo`, `lt`, `lv`, `mk`, `mn`, `ms`, `my`, `nb`, `ne`, `nl`, `nn`, `no`, `pcm`, `pl`, `ps`, `pt`, `qu`, `ro`, `ru`, `sd`, `si`, `sk`, `sl`, `so`, `sq`, `sr`, `sr_Latn`, `sv`, `sw`, `th`, `tk`, `tr`, `uk`, `ur`, `uz`, `vi`, `yo`, `yue`, `zh`, `zh_Hant`, `zu`
</details>

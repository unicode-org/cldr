---
title: Special cases
---
# Special cases

### Pt\_CV: CVE conflict with PTE

Use Currency code when an old currency symbol conflicts with the current currency.

The currency for Cape Verde is CVE (Cape Verdean escudo), and is specified as the following for pt\_CV locale that results in “123 456 789$99 / -123 456 789$99 ​” ​

PTE (Portuguese escudo), is an old currency symbol for Portugal. This CANNOT have the same currency symbol as current pt\_CV currency, which is also U+200B ZWSP.

Thus, this old currency symbol for PTE is specified as "PTE" in pt\_CV to avoid the conflict with the symbol being used for the current currency symbol for CVE.

Thus, in pt\_CV, the following currency symbol information should be kept.

\<currency type="CVE"> 

&emsp; \<symbol>\</symbol> (Note: this is U+200B ZWSP, not nothing)

&emsp; \<decimal>$\</decimal>

&emsp; \<group> \</group>

\<currency type="PTE"> 

&emsp; \<symbol>PTE\</symbol>

&emsp; \<decimal>,\</decimal> 

&emsp; \<group> \</group> 


![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
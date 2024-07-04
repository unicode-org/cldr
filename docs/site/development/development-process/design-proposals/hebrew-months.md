---
title: Hebrew Months
---

# Hebrew Months

Here's what our Hebrew contacts are telling us about month names and numbers in the Hebrew calendar:

From an end-user's point of view, the numbering of Hebrew months is always consecutive. Even though the numbers are seldom ( if ever ) used in practice. That is to say, in a non-leap year:

Shevat = month 5, Adar = month 6, Nisan = Month 7

while in a leap year:

Shevat = month 5, Adar I = month 6, Adar II = month 7, and Nisan = month 8. 

According to Wikipedia, "Adar II" in a leap year is the "real" Adar, and "Adar I" is considered to be the "extra" month.

I think we can get the desired representation without having to make it overly complex.

To sum up. Currently we have:

\<monthWidth type="wide">

&emsp;\<month type="1">Tishri\</month>

&emsp;\<month type="2">Heshvan\</month>

&emsp;\<month type="3">Kislev\</month>

&emsp;\<month type="4">Tevet\</month>

&emsp;\<month type="5">Shevat\</month>

&emsp;**\<month type="6">Adar I\</month>**

&emsp;**\<month type="7">Adar\</month>**

&emsp;\<month type="8">Nisan\</month>

&emsp;\<month type="9">Iyar\</month>

&emsp;\<month type="10">Sivan\</month>

&emsp;\<month type="11">Tamuz\</month>

&emsp;\<month type="12">Av\</month>

&emsp;\<month type="13">Elul\</month>

\</monthWidth>

I propose that we add a distinguishing attribute called "yeartype" to the month element, and then simply add "Adar II" as follows:

\<monthWidth type="wide">

&emsp;\<month type="1">Tishri\</month>

&emsp;\<month type="2">Heshvan\</month>

&emsp;\<month type="3">Kislev\</month>

&emsp;\<month type="4">Tevet\</month>

&emsp;\<month type="5">Shevat\</month>

&emsp;**\<month type="6">Adar I\</month>**

&emsp;**\<month type="7">Adar\</month>**

&emsp;**\<month type="7" yeartype="leap">Adar II\</month>**

&emsp;\<month type="8">Nisan\</month>

&emsp;\<month type="9">Iyar\</month>

&emsp;\<month type="10">Sivan\</month>

&emsp;\<month type="11">Tamuz\</month>

&emsp;\<month type="12">Av\</month>

&emsp;\<month type="13">Elul\</month>

\</monthWidth>

This approach has a number of advantages:

a). It is only a one line change from the existing data, which means minimal disruption to anyone using the existing data.

b). It is technically more accurate according to the Wikipedia, since "Adar II" in a leap year is considered the equivalent month as "Adar" in a non-leap year. That is to say, "Adar II" is the "real" Adar, not "Adar I".

c). Calendaring applications have a relatively easy way to go through the data in numeric order. In a non-leap year, just use 1-5 and 7-12. In a leap year, use 1-6, + 7 alt + 8-12. 

The new attribute "yeartype" was chosed as opposed to using "alt", since ICU's build process excludes all "@alt" data by default.

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
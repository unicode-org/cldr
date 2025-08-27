---
title: Update Currency Codes
---

- Go to [SIX Financial Data Standards]
- Take the link for "Current Currency and Funds": [List one XML]
- Save the page as {cldr}/tools/cldr\-code/src/main/resources/org/unicode/cldr/util/data/dl\_iso\_table\_a1\.xml
- Take the link for "Historic denominations": [List three XML]
- Save the page as {cldr}/tools/cldr\-code/src/main/resources/org/unicode/cldr/util/data/dl\_iso\_table\_a3\.xml
- **Use git diff to sanity check the two XML files against the old, and check them in.**
  - **"git diff \-w" is helpful to ignore whitespace. If there are only whitespace changes, there's no need to check them in.**
- **Check the** [ISO amendments] **to get changes that will happen during the current cycle.**
- Example: <https://www.six-group.com/dam/download/financial-information/data-center/iso-currrency/amendments/dl-currency-iso-amendment-179.pdf>
- It appears right now like there is no good way to collect all the amendments that are applicable, except to change "179" in the above link by incrementing until error \#404 results. So:
- *Review all amendments that are dated after the previous update , and patch the XML files and the* ```supplementalData.xml``` *as below.*
- *Record the last number viewed in the URL above.*
- *(There is a "download all amendments" link now that has a spreadsheet summary.)*
- If there are no diffs in the two iso tables, and no relevant changes in the amendments, you are done.
- Run ```CountItems -Dmethod=generateCurrencyItems``` to generate the new currency list.
  - If any currency is missing from ISO4217\.txt, the program will throw an exception and will print a list of items at the end that need to be added to the ISO4217\.txt file. Add as described below.
  - Once the necessary codes are added to ISO4217\.txt, repeat the CountItems \-Dmethod\=generateCurrencyItems until it runs cleanly.
  - If any country changes the use of a currency, verify that there is a corresponding entry in SupplementalData
  - Since ISO doesn't publish the exact date change (usually just a month), you may need to do some additional research to see if you can determine the exact date when a new currency becomes active, or when an old currency becomes inactive. If you can't find the exact date, use the last day of the month ISO publishes for an old currency expiring.
  - For new stuff, see below.
- Adding a currency:
  - Make sure the new code exists in common/bcp47/currency.xml. The currency code should be in lower case, and make sure the "since" release corresponds to the next release of CLDR that will publish using this data.
  - In SupplementalData:
  - If it has unusual rounding or number of digits, add to:
    - \<fractions\>
    - \<info iso4217\="ADP" digits\="0" rounding\="0"/\>
    - ...
  - For each country in which it comes into use, add a line for when it becomes valid
    - \<region iso3166\="TR"\>
    - \<currency iso4217\="TRY" from\="2005\-01\-01"/\>
  - Add the code to the file java/org/unicode/cldr/util/data/ISO4217\.txt. This is important, since it is used to get the valid codes for the survey tool.
    - Example:
      - currency \| TRY \| new Turkish Lira \| TR \| TURKEY \| C
    - Mark the old code in java/org/unicode/cldr/util/data/ISO4217\.txt as deprecated.
      - currency \| TRL \| Old Turkish Lira \| TR \| TURKEY \| O
- Changing currency.
  - If the currency goes out of use in a country, then add the last day of use, such as:
    - \<region iso3166\="TR"\>
    - \<currency iso4217\="TRL" from\="1922\-11\-01"/\>
    - \=\>
    - \<region iso3166\="TR"\>
    - \<currency iso4217\="TRL" from\="1922\-11\-01" to\="2005\-12\-31"/\>
  - Edit common/main/en.xml to add the new names (or change old ones) based on the descriptions.
    - If there is a collision between a new and old name, the old one typically changes to the currency name with the date range
      - "currency\_name (1983\-2003\)".
- Check in your changes
  - common/bcp47/currency.xml
  - tools/java/org/unicode/cldr/util/data/ISO4217\.txt
  - common/main/en.xml
  - common/supplemental/supplementalData.xml

Note: the list of currencies is in `bcp47/currency.xml` There is also an ICU tool in <https://github.com/unicode-org/icu/tree/main/tools/currency> which (according to an earlier version of this document) should move to CLDR.

[SIX Financial Data Standards]: https://www.six-group.com/en/products-services/financial-information/data-standards.html#scrollTo=currency-codes
[List one XML]: https://www.six-group.com/dam/download/financial-information/data-center/iso-currrency/lists/list-one.xml
[List three XML]: https://www.six-group.com/dam/download/financial-information/data-center/iso-currrency/lists/list-three.xml
[ISO amendments]: https://www.six-group.com/en/products-services/financial-information/data-standards.html#scrollTo=amendments

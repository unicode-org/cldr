---
title: Currency Names and Symbols
---

# Currency Names and Symbols

## Currency Names must be Unique

Currency names must be unique; the same name can't be used for two different currency codes. 

When a country replaces a currency:

- It will get a new code.
- It will either reuse the name (and the old name needs to be modified), or use a new name. To find out details, go to [Detailed Territory-Currency Information](https://unicode.org/cldr/charts/latest/supplemental/detailed_territory_currency_information.html) to see what currencies are, and which countries they are used in. You can search for the code in square brackets, such as for [MZN] (for Mozambian Metical). A currency is current if it has "∞" in the **To** column for some country.

Guidelines:

- For a current currency, use the most common name, such as *Mozambian Metical*.
- When the currency name is ambiguous, and need to be qualified in some way:
	- Add the name of the country (e.g. British pound)
	- Or other ways to differentiate (e.g. pounds sterling)
- For an obsolete currencies:
	- It may be known by a different name, like *Mozambican Escudo*. Then there is no problem.
	- If it has the same name as some current currency, include a date range, like **Mozambian Metical (1980-2006)**

## Symbols

The following general guidelines are used for currency symbols. These guidelines are also subject to the CLDR [Currency Process](https://cldr.unicode.org/index/cldr-spec/currency-process).

1. If a symbol is not widely recognized around the world (eg shekel ₪)
	1. Where the currency is official in a country, use that symbol in locales with that country (eg IL)
	2. Where the currency would be widely recognized by users of a language, use it in the base language locale (eg he/iw).
	3. Otherwise, use the international currency symbol (eg ILS). This can be done just by omitting the translation.
2. Otherwise the symbol is widely recognized. If the symbol is used for only one currency (eg €) or widely recognized as being a given currency (eg £):
	1. Use that symbol in root.
	2. If it wouldn't be recognized in particular countries or among particular language users, in those locales/countries use the international currency code (eg EUR) or another replacement (see below). These other symbols have to be listed explicitly, so that they override root.
3. Otherwise the symbol is used for multiple currencies, so
	1. Use the symbol in the countries that have it as an official currency symbol
	2. Use the symbol in languages where it there is a well-established general understanding that it would mean a particular currency.
4. Otherwise, typically use international currency code or ("region-code" + symbol) or (symbol + "region-code") (the region is usually a country, but sometimes not) so that it is not ambiguous.
5. Note that the 3-letter international (ISO 4217) code for every currency is always available to CLDR data consumers, regardless of any other symbols that may be specified for the currency. So it is never necessary to specify the 3-letter code in place of a symbol in order to ensure that the 3-letter code can be used if desired; the choice of when to use a symbol should be governed by the guidelines above.

These are only general guidelines, and may need to be overridden in particular cases. Certain symbols like the dollar sign are particularly tricky, because they are used by a great many countries.

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
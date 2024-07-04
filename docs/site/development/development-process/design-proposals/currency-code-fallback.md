---
title: Currency Code Fallback
---

# Currency Code Fallback

The basic problem is that we can't use currency codes that our users won't have fonts for. It was fine to have, say, the shekel sign in Hebrew, as in CLDR 1.6, because we could presume that anyone using the Hebrew locale would have a Hebrew font, and that Hebrew fonts would have a shekel sign. But by putting it into root, we are presuming that \*everybody\* would have that character, which is not true. Our users/customers think there is something wrong when they scan a list of currencies, and some of them are black boxes.

Here are some examples of behavior I think we'd like to support.

- We show ￡ if the font is available, otherwise £, otherwise currency code.
- We show ₧ if the font is available, otherwise Pts, otherwise currency code.

For reference, here are the currency signs in Unicode: http://unicode.org/cldr/utility/list-unicodeset.jsp?a=[%3Asc%3A]. Also see http://en.wikipedia.org/wiki/Currency\_sign and http://www.unicode.org/cldr/data/charts/by\_type/names.currency.html.

For more details on the problem, see the email thread titled "Problems with currency codes".

Here are some recommendations.

1. We don't have the character fallback element for any currency symbol that is used for different currency codes. That is, it is ok to have EUR for €, but not ok to use KRW for ₩, since ₩ is also used for KPW, and not to have JAY for ¥, since ¥ is also used for CNY.
2. Even with this, we don't really want to use character fallback elements for currency substitution in general, since it is too coarse.
3. We should try to remove all the currency symbols that use Unicode symbol characters from the locales, except where they have special plurals, or where we have symbol reversals (eg in the US, \$ for USD and C\$ for CAD, while in CA, \$ for CAD and US\$ for USD).

Options

1. We then just make sure that all currency symbols in root are widely understood and in common fonts (eg in Windows Arial), or
2. We enhance the currency symbols so that we have a fallback list. We put the symbols that are in typical fonts in each locale in the currencySymbols exemplar list for that locale. When formatting, we walk through the fallback list until we hit one that works. If we don't get any, we use the currency code. If a smart client has font information, then he could also walk the fallback list using the font information instead of the currencySymbol exemplars.
3. We have something like "commonly used" that lets the application provider choose to force the symbol; otherwise only the commonly used symbols appear. So in root, commonly used would be on EUR, etc. Could be turned on in locales, or by application.

I'm leaning towards #1, just for simplicity.

However, see also: http://www.unicode.org/cldr/bugs/locale-bugs?findid=2244

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
---
title: Currency Process
---

# Currency Process

There are three stages for new currency symbols (such as the recent Russian, Indian, and Turkish symbols). The following shows the stage and the disposition in CLDR data:

|  |  |  |
|---|---|---|
| 1 | Not widely adopted. | It is added as an alt value in the relevant locales (based on language and country codes). That means that it won't be the "stock" symbol for those locales, but will be accessible to implementations that support alt values. |
| 2 | Adopted widely in fonts and keyboards used in the relevant locales. | It is added it to the relevant locales as the standard version. |
| 3 | Widely recognized outside of the locales, and in most operating systems (Android, iOS, Windows, Mac — not just the latest versions, but also older ones that have significant market share). | Added to root as the standard version. |

For more information, see [Currency Symbols \& Names](/translation/currency-names-and-symbols/currency-names).



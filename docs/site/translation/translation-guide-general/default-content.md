---
title: Default Content
---

# Default Content

Locales are primarily identified by their ***base*** language. For example, English \[en], Arabic \[ar] or German \[de].

We also label scripts explicitly, where a language is typically written in multiple scripts, such as Cyrillic or Latin. For example, Serbian (Cyrillic) \[sr\_Cyrl] and Serbian (Latin) \[sr\_Latn].

Each language \+ script combination is treated as a unit. (i.e. People do not mix different script in the same data set.)

If a language is ***not*** typically written in multiple scripts, then the script sub\-tag is omitted. For example, en\_US or ko\_KR.

Locales may also have regional variants. For example, English (US) \[en\_US] vs English (UK) \[en\_GB], or Serbian (Cyrillic, Montenegro) \[sr\_Cyrl\_ME] vs Serbian (Cyrillic, Serbia) \[sr\_Cyrl\_RS]. Regions may be countries such as China \[CN], parts of countries such as Hong Kong \[HK] or multi\-country regions such as Latin America \[419]. Also see [Regional Variants](/translation/getting-started/guide#TOC-Regional-Variants-also-known-as-Sub-locales-).

The contents for the base language should be as widely usable (neutral) as possible, but **must be** usable without modification for its *default content locale;* this is the locale for the language’s *default region,* which is typically the region with the most speakers of the language. A default content locale has no data other than identity information, it inherits all data from its parent.

For example:

- American English \[en\_US] is the default content locale for English \[en]
- German (Germany) \[de\_DE] is the default content locale for German \[de].
- Portuguese (Brazil) \[pt\_BR] is the default content locale for Portuguese \[pt]
- Serbian (Cyrillic) \[sr\_Cyrl] is the default content locale for Serbian \[sr], which is the default for Serbian (Cyrillic, Seriba) \[sr\_Cyrl\_RS] .
- Arabic (World) \[ar\_001] is the default content locale for Arabic \[ar], which is for Modern Standard Arabic.

**Tips for linguists:**

1. Make sure the base language content is correct; as widely usable (neutral) as possible, but must be usable **without** modification in the default content locale.
2. For example:
	- English \[en] locale content must be usable for English (US)
	- Arabic \[ar] content must be usable for Arabic (world/neutral).
3. Make sure that where there is a difference in a sub\-region, the differences are represented in the regional\-variant locale.
4. For example:
	- Spanish (Mexico) \[es\_MX] differences from Spanish (Latin America) \[es\_419]
	- Arabic (Egypt) \[ar\_EG] that are different from Arabic (World) \[ar\_001]


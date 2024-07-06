---
title: Path Filtering
---

# Path Filtering

Inside of CoverageLevel, and in the LDML2ICUConverter, and in various other places, we are filtering the XML files based on paths and values. However, these tend to be ad hoc mechanisms, and especially in the case of CoverageLevel, with a lot of hard-coded strings. This is a proposal for making a general, data-driven mechanism for handling this.

The data is a list of pairs, where the first of each pair is a result, and the second is a regex. Logically, the list is traversed until there is a match, and then the result for that pair is returned.

For example, here is what the start of the list for CoverageLevel might look like:

posix ; posix/messages/(yes|no)str

posix ; characters/exemplarCharacters

minimal ; timeZoneNames/(hourFormat|gmtFormat|regionFormat)

minimal ; unitPattern

basic ; measurementSystemName

The results do not need to be grouped together. Thus an inclusion/exclusion list can be formed like:

true ; posix

false ; examplarCharacter.\*auxiliary

true ; exemplarCharacters

...

You can also have special purpose pairs, such as the following to remove the alts at the front.

skip ; \\[@alt="[^"]\*proposed

**Specialized Wildcards**

There are a couple of extra features of the regex. For the coverage level (and perhaps others), we need some additional matches.

[TBD - add more]

| Variable | Description |
|---|---|
| $locale | the locale of the XML file in question |
| $eu | the EU languages |
| $localeScripts | the scripts used in this locale, eg (Latn\|Cyrl\|Arab) |
| $modernCurrencies | currencies that are currently valid tender in some country |
| $localeRegions | countries/regions that have the locale's language as an official language |
| $localeCurrencies | modern currencies for the $localeRegions |
| $modernMetazones | metazones ... |

*Issue:* 

- *I'm thinking that we may want to append the value to the path (eg .../\_VALUE="...") to allow for matching on that.*
- *Use XML instead of ; format?*

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
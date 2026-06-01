---
title: Core Data for New Locales
---

# Core Data for New Locales

This document describes the minimal data needed for a new locale. There are two kinds of data that are relevant for new locales:

1. **Core Data** \- This is data that the CLDR committee needs from the proposer ***before*** a new locale is added. The proposer is expected to also get a Survey Tool account, and contribute towards the Basic Data.
2. **Basic Data** \- The Core data is just the first step. It is only created under the expectation that people will engage in suppling data, at a [Basic Coverage Level](/index/cldr-spec/coverage-levels#h.yi1eiryx7yl4). **If the locale does not meet the [Basic Coverage Level](/index/cldr-spec/coverage-levels#h.yi1eiryx7yl4) in the next Survey Tool cycle, the committee may remove the locale.**

## Core Data

Collect and submit the following data, using the [Core Data Submission Form](https://docs.google.com/forms/d/e/1FAIpQLSfSyz0VUSXD93IJQQdjzUCnbQwC2nwz6eiLjTaFjASQZzpoSg/viewform). *Note to translators: If you are having difficulties or questions about the following data, please contact us: [file a new bug](/index/bug-reports#TOC-Filing-a-Ticket), or post a follow\-up to comment to your existing bug.*

1. The correct language code according to [Picking the Right Language Identifier](/index/cldr-spec/picking-the-right-language-code).
2. The four exemplar sets: main, auxiliary, numbers, punctuation. 
	- These must reflect the Unicode model. For more information, see [tr35\-general.html\#Character\_Elements](http://www.unicode.org/reports/tr35/tr35-general.html#Character_Elements).
3. Verified country data ( i.e. the population of speakers in the regions (countries) in which the language is commonly used) 
	- There must be at least one country, but should include enough others that they cover approximately 75% or more of the users of the language.
	- "Users of the language" includes as either a 1st or 2nd language. The main focus is on written language.
4. Default content script and region (normally the region is the country with largest population using that language, and the customary script used for that language in that country). 
	- **\[[supplemental/supplementalMetadata.xml](https://github.com/unicode-org/cldr/blob/main/common/supplemental/supplementalMetadata.xml#LC1654:~:text=%3CdefaultContent)]**
	- *See*: [http://cldr.unicode.org/translation/translation\-guide\-general/default\-content](/translation/translation-guide-general/default-content)
5. The correct time cycle used with the language in the default content region
	- In common/supplemental/supplementalData.xml, this is the "timeData" element
	- The value should be h (1\-12\), H (0\-23\), k (1\-24\), or K (0\-11\); as defined in [https://www.unicode.org/reports/tr35/tr35\-dates.html\#Date\_Field\_Symbol\_Table](https://www.unicode.org/reports/tr35/tr35-dates.html#Date_Field_Symbol_Table)

***You must commit to supplying [the data required for the new locale to reach Basic level](/index/cldr-spec/core-data-for-new-locales#h.yaraq3qjxnns) during the next open CLDR submission when requesting a new locale to be added.***

For more information on the other coverage levels refer to [Coverage Levels](/index/cldr-spec/coverage-levels) 


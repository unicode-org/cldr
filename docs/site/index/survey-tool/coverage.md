---
title: Setting Default Coverage Levels
---
# Setting Default Coverage Levels

## Introduction to Coverage Level

- For an introduction to coverage level, see the appropriate section in TR\#35, "[Coverage Levels](https://www.unicode.org/reports/tr35/tr35-info.html#Coverage_Levels)".
- To see how coverage level is used in the Survey Tool, see "[Advanced Features](/translation/getting-started/guide#TOC-Advanced-Features)" in the Survey Tool guide

## To set your organization's default coverage levels

The **Coverage Level** determines the number of items that your people will see for translation in each locale. The best target in general is *modern*: some companies that use CLDR data require coverage at a *modern* level before they will use the locale; others will use data that is less complete, such as *moderate* or *basic*. If it is not feasible to attain modern coverage in a given cycle, it is still best to set the coverage as high as feasible, such as *moderate*. 

The number of items at each level varies by locale, but is roughly the following:

| Approximate Count | Coverage Level |
|---|---|
| 200 | minimal/( **also known as Core?** ) |
| +300 | basic |
| +200 | moderate |
| + 1500 | modern |

To see your current coverage levels, look for your organization in [Locales.txt](https://github.com/unicode-org/cldr/blob/main/tools/cldr-code/src/main/resources/org/unicode/cldr/util/data/Locales.txt). It is a simple plaintext file, where each line is of the form:

\<organization\> ; \<language\_code\> ; \<coverage\_level\> ; \<language name \- optional\>

A \# starts a comment, and a \* for the language\_code means "all others".

An organization that focuses on a small number of locales will probably want to have just a line added for those locales, such as: 

Breton ; br ; modern Breton ; \*  ; moderate

To request a change in the default coverage for your locale, file a [new ticket](/index/bug-reports#TOC-Filing-a-Ticket).

As part of CLDR v35, the coverage level has been refined further. See ticket \#[11498](https://unicode-org.atlassian.net/browse/CLDR-11498)

1. *Basic Level*. The flexible date and time formats, such as day \+ month patterns, are moved down from *Moderate* to reflect the increased use of these patterns for the minimally viable locale data.
2. *Modern Level*. Some lesser\-used language names are pushed up to *Comprehensive*. (No data is removed: these are still accessible in the *Comprehensive* level.)
3. Languages. The following languages have higher target levels, and can have missing items added.
	1. *Basic Level* : Cebuano (ceb), Hausa (ha), Igbo (ig), Yoruba (yo)
	2. *Modern Level*: Somali (so), Javanese (jv)


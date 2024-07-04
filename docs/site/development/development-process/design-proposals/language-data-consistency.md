---
title: Language Data Consistency
---

# Language Data Consistency

We have a set of tests for consistency in the data for language, script, and country. The following is a draft description what those consistency checks should aim for.

## Default script, language

- 1. For each script encoded in Unicode, the default\* language is present in the script metadata.
2. For each language used in CLDR, there is a default\* script

\* default = most used in writing; currently if modern, otherwise historical.

## Implications for Language-Country population data (LCPD)

1. If a base-language has a CLDR locale, then it is in the LCPD for at least one country.
2. If there is a CLDR country locale for a language, then that language+country is in the LCPD.
	1. For each country, get the language most widely used as a written language in that country. That language+country combination is in the LCPD.
	2. When a significant proportion of the language use in a country is in a non-default script, that script is marked in the LCPD.
	3. When a script is not EXCLUDED in UAX#31, then we have at least one language-country pair in the LCPD.
3. If a language has a significant\* literate population in a country, the pair is in the LCPD. This target is fuzzier, but definitely
	1. anything \>1M, or
	2. \>100K and either official (real, not honorary) or 1/3 of the population.

## Implications for Likely Subtags

Likely Subtags are built from the language-country population data, plus the script metadata, plus an exception list.

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
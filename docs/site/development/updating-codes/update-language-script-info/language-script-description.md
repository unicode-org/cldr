---
title: Language Script Description
---

# Language Script Description

The [`language\_script.tsv`](https://github.com/unicode-org/cldr/blob/main/tools/cldr-code/src/main/resources/org/unicode/cldr/util/data/language_script.tsv) data file should list all of the language / script combinations that are in common use. Usage by country is indicated in the [`country\_language\_population.tsv`](https://github.com/unicode-org/cldr/blob/main/tools/cldr-code/src/main/resources/org/unicode/cldr/util/data/country_language_population.tsv) spreadsheet.

1. Every language needs at least 1 script considered the **primary** script.
    1. This data is used to determine [the most Likely language and region](likelysubtags-and-default-content) so there needs to be at least 1 primary value.
    2. [Changed in v47] Include a primary script for historical languages (eg. Ancient Greek, Coptic). The primary script should reflect where the majority of the written corpus originates from.
2. Languages written by significant populations with different scritps in different countries can have multiple **primary** scripts. The [likely subtags](https://www.unicode.org/cldr/charts/latest/supplemental/likely_subtags.html) patterns will use population counts to disambiguate the default script for each locale.
3. Other scripts used for a language should be marked **secondary**.

If a language has multiple primary scripts, then it should not appear without the script tag in the country\_language\_population.tsv. For example, we should not see "az", but rather "az\_Cyrl", "az\_Latn", and so on. For each country where the language is used, we should see figures on the script\-specific values. The values may overlap, that is, we may see az\_Cyrl at 60% and az\_Latn at 55%. However, the combination with the predominantly used script **must** have a larger figure than the others.

This is also reflected in CLDR main: languages with multiple scripts will have that reflected in their structure (eg sr\-Cyrl\-RS), with aliases for the language\-region combinations.

In order to re-generate the XML data use ConvertLanguageData as written about in [the article about updating the language scripts](.../update-language-script-info.md).

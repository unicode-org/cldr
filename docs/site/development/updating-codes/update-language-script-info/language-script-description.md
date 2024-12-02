---
title: Language Script Description
---

# Language Script Description

The [`language\_script.tsv`](https://github.com/unicode-org/cldr/blob/main/tools/cldr-code/src/main/resources/org/unicode/cldr/util/data/language_script.tsv) data file should list all of the language / script combinations that are in common use. Usage by country is indicated in the [`country\_language\_population.tsv`](https://github.com/unicode-org/cldr/blob/main/tools/cldr-code/src/main/resources/org/unicode/cldr/util/data/country_language_population.tsv) spreadsheet.

1. Every language should have 1 script considered the **primary** script.
    1. This data is used to determine [the most Likely language and region](likelysubtags-and-default-content) so there needs to be at least 1 primary value.
    2. __Changed in v47__ Include a primary script for historical languages (eg. Ancient Greek, Coptic). The primary script should reflect where the majority of the written corpus originates from.
2. Other scripts used for a language should be marked **secondary**.

Languages with multiple ambiguous scripts should have that reflected in their CLDR structure (eg. `sr_Cyrl_RS`), with aliases for the language\-region combinations.

In order to re-generate the XML data use ConvertLanguageData as written about in [the article about updating the language scripts](/development/update-language-script-info.md).

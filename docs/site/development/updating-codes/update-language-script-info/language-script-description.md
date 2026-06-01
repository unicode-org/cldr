---
title: Language Script Description
---

# Language Script Description

The [`language\_script.tsv`](https://github.com/unicode-org/cldr/blob/main/tools/cldr-code/src/main/resources/org/unicode/cldr/util/data/language_script.tsv) data file should list all of the language / script combinations that are in common use. Usage by country is indicated in the [`country\_language\_population.tsv`](https://github.com/unicode-org/cldr/blob/main/tools/cldr-code/src/main/resources/org/unicode/cldr/util/data/country_language_population.tsv) spreadsheet.

1. Every language should have 1 script considered the **primary** script.
    1. This data is currently used to determine [the likely script for a language](/development/updating-codes/likelysubtags-and-default-content) so there needs to be at least 1 primary value. Because it is the default, it determines the script of locales without language codes in the `<territoryInfo>`. 
    2. __Changed in v47__ Include a primary script for historical languages (eg. Ancient Greek, Coptic). The primary script should reflect where the majority of the written corpus originates from.
2. Other scripts used for a language should be marked **secondary**.

Languages with multiple ambiguous scripts should have that reflected in their CLDR structure (eg. `sr_Cyrl_RS`), with aliases for the language\-region combinations.

In order to re-generate the XML data use ConvertLanguageData as written about in [the article about updating the language scripts](/development/updating-codes/update-language-script-info).

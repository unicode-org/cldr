---
title: Language Script Description
---

# Language Script Description

The language\_script spreadsheet should list all of the language / script combinations that are in common modern use. The countries are not important, since their function has been overtaken by the country\_language\_population spreadsheet.

1. If the language and script are both modern, and the script is a major way to write the language in some country, then we should see that line marked as **primary**.
2. Otherwise it should be marked **secondary**.

Every language that is in official use in any country according to country\_language\_population  should have at least one primary script in the language\_script spreadsheet.

If a language has multiple primary scripts, then it should not appear without the script tag in the country\_language\_population.tsv. For example, we should not see "az", but rather "az\_Cyrl", "az\_Latn", and so on. For each country where the language is used, we should see figures on the script\-specific values. The values may overlap, that is, we may see az\_Cyrl at 60% and az\_Latn at 55%. However, the combination with the predominantly used script **must** have a larger figure than the others.

This is also reflected in CLDR main: languages with multiple scripts will have that reflected in their structure (eg sr\-Cyrl\-RS), with aliases for the language\-region combinations.

Files in https://github.com/unicode-org/cldr/tree/main/tools/cldr-code/src/main/resources/org/unicode/cldr/util/data

1. country\_language\_population.tsv
2. language\_script.tsv


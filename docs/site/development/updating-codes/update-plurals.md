---
title: Adding/Fixing Plurals and Ordinals
---

# Adding/Fixing Plural and Ordinals

## Adding rules for a locale

- Add the new plural rules to plurals.xml.
- Add the new ordinal rules to ordinals.xml.
- Add the new plural ranges to pluralRanges.xml
- Add the draft minimal pairs to the locale.
- Run the tools below.

## Modifying rules/ranges for a locale

- Normally, you just need to modify the rules in the appropriate file.
- However, if you modify the plural or ordinal categories (adding or removing a category), then make sure all the related files above are consistent.
- Run the tools below.

## Running tools

Whenever you change plural rules, ordinal rules, or plural ranges — _and at the end of the release_ — you must do the following.  The reformatting is required, since it regenerates the samples (after @), which are now necessary for correct functioning of ICU and other clients.

1. Run GeneratePluralRanges.java to reformat pluralRanges.xml
 - Overwrites cldr/common/supplemental/pluralRanges.xml in place.
 - Diff, commit if changed.
2. Run GeneratedPluralSamples to reformat plurals.xml, ordinals.xml.
 - Overwrites cldr/common/supplemental/plurals.xml and cldr/common/supplemental/ordinals.xml in place.
 - Diff, commit if changed.
3. Run GenerateAllCharts (actually, it is enough to run ShowLanguages) and the unit tests.
 - This will insure that the rules are correctly represented, and aligned with the minimal pairs.
 - Diff the plurals chart against the old one: eg against https://www.unicode.org/cldr/charts/48/supplemental/language_plural_rules.html
4. Compare the results carefully, then check in.

## Run WritePluralRulesSpreadsheets.java

For now don't do this step; it needs fixes as per [https://unicode-org.atlassian.net/browse/CLDR-19379](CLDR-19379)



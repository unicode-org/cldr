---
title: Adding/Fixing Plural and Ordinals
---

# Adding/Fixing Plural and Ordinals

## Run WritePluralRulesSpreadsheets.java
Q: Is the output supposed to be captured somewhere? Prints stuff to console.

## Adding rules for a locale
- Add the new plural and ordinal rules to plurals.xml and ordinals.xml.
- Run GeneratedPluralSamples as below.
- Add the minimal pairs to the locale.

## Changing rules/ranges

Whenever you change plural rules, ordinal rules, or plural ranges — and at the end of the release — you must do the following.  The reformatting is required, since it regenerates the samples (after @), which are now necessary for correct functioning of ICU and other clients.

1. Run GeneratePluralRanges.java to reformat pluralRanges.xml
 - Overwrites cldr/common/supplemental/pluralRanges.xml in place.
 - Diff, commit if changed.
1. Run GeneratedPluralSamples to reformat plurals.xml, ordinals.xml.
 - Overwrites cldr/common/supplemental/plurals.xml and cldr/common/supplemental/ordinals.xml in place.
 - Diff, commit if changed.
1. Run GenerateAllCharts (actually, it is enough to run ShowLanguages) and the unit tests.
 - This will insure that the rules are correctly represented, and aligned with the minimal pairs.
 - Diff the plurals chart against the old one: eg v.s. https://unicode-org.github.io/cldr-staging/charts/38/supplemental/language_plural_rules.html
~~1. Run FindPluralDifferences to see that the expected changes are there (only tests integers right now).~~
~~- Modify the versions variable to use the last release.~~
~~- This is also run at the end of the release to get the Migration info for the release.~~
1. Compare the results carefully, then check in.

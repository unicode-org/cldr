---
title: Adding a new locale to CLDR
---

# Adding a new locale to CLDR

NOTE: this is an internal page for TC developers. Others should see [Core Data for New Locales](https://cldr.unicode.org/index/cldr-spec/core-data-for-new-locales).

### Country Locales

If you are just adding a new country locale (eg we have a locale for the language), you just need to add the new empty locale file, and Update Survey Tool. Otherwise:

### Minimal Structure

Before adding a new locale, you must have the core data: see [Core Data for New Locales](https://cldr.unicode.org/index/cldr-spec/core-data-for-new-locales) for the process.

Here is an example: https://github.com/unicode-org/cldr/pull/59/files

### Add Data in git

- Before starting to add a new locale, make sure you have the minimal core data that cannot be added through the Survey Tool. See above.
- Create the new locale files. If you are adding a single new language locale, for example, language "xx" as spoken in country "YY", you will need two files:
	- **common/main/xx.xml** \- The main locale file containing the core data. You can use the template in seed/main/und.xml as a starting point.
	- **common/main/xx\_YY.xml** \- An empty country locale containing the identification of xx\_YY as a valid locale. You can use the template in seed/main/und\_XX.xml as a starting point.
	- See files are here: https://github.com/unicode-org/cldr/tree/master/seed/main
- Add the plural rules (if available) to **common/supplemental/plurals.xml**
- Add the day period rules (if you have them ) to **common/supplemental/dayPeriods.xml**
- If you are adding a new language
	- Add the language subtag to \<variable id\="$language" type\="choice"\> in
	- **/common/supplemental/attributeValueValidity.xml**
	- add the appropriate default content locale
	- to \<defaultContent locales\="..."\> in **common/supplemental/supplementalMetadata.xml**
	- The default content locale is usually the locale where the most people speak the language in question.
	- If the language is not already in common/supplemental/likelySubtags.xml
		- Send the literate pop information to Rick, or file a bug, if the language is not already in the supplemental data.
		- Once he has added, run the tool in [LikelySubtags and Default Content](https://cldr.unicode.org/development/updating-codes/likelysubtags-and-default-content) to add the new language and its associated subtags to common/supplemental/likelySubtags.xml
	- Also add the English translation for any new languages in **common/main/en.xml**
- If requested, add to vendor targets (Locale.txt), and to Cldr where resources are committed.
- Run the tests (will be done automatically when a PR is created)
- Commit your work to a branch and create a Pull Request.
- The new locale will be included in Smoketest when the PR is merged, and will be in production once a push to production occurs.


---
title: Adding a new locale to CLDR
---

# Adding a new locale to CLDR

NOTE: this is an internal page for TC developers. Others should see [Core Data for New Locales](/index/cldr-spec/core-data-for-new-locales).

## Regional locales

If you are just adding a new region locale (that is, CLDR already has a locale for the language),
you just need to add the new empty locale file, and Update Survey Tool. 
[This example](https://github.com/unicode-org/cldr/pull/4335/files) adds Kurdish in Syria ("ku-SY"). 

Otherwise:

## New locales

Before adding a new locale, you must have the minimal core data that cannot be added through the Survey Tool.
See [Core Data for New Locales](/index/cldr-spec/core-data-for-new-locales) for the process.
This will include:

 - language code
 - exemplar sets (main, auxiliary, numbers, punctuation)
 - country / population data
 - time cycle (12-hour vs 24-hour, 0-based vs 1-based)
 - orientation of characters on a line and lines on a page
 - English name for this language

The English name won't be used until the language reaches Basic level, but needs to be documented in the CLDR ticket which adds the locale.

### Add Data to the GitHub repository

#### Verify or update country/population/language/script Data

See [Update Language Script Info](/development/updating-codes/update-language-script-info)
and [LikelySubtags and Default Content](/development/updating-codes/likelysubtags-and-default-content)
for information on how to use the language, script, region and population data to verify that the information in
[country_language_population.tsv](https://github.com/unicode-org/cldr/blob/main/tools/cldr-code/src/main/resources/org/unicode/cldr/util/data/country_language_population.tsv)
and [language_script.tsv](https://github.com/unicode-org/cldr/blob/main/tools/cldr-code/src/main/resources/org/unicode/cldr/util/data/language_script.tsv) is correct.
If changes are needed, correct an existing entry or add a new one as appropriate. Include references. 

When the CLDR tools are run, they will use the entries in these .tsv files to generate updates to the supplemental data.
If necessary, in order to make the required tests pass when adding a locale,
you can make temporary changes in the generated supplemental data to reflect what will eventually be generated.
Here is a list of such changes that may be needed:

- In **common/supplemental/supplementalData.xml**, add a "language" element under the "languageData" element (if not already present)
to indicate script and territory information.
- In **common/supplemental/supplementalData.xml**, check for a "languagePopulation" element under the "territory" element
which corresponds to the region for this locale.
If there is no such element, add a "languagePopulation" element, specifying the language and population information.
If the element exists, verify (or update) the population values.
- In **common/supplemental/likelySubtags.xml**, check that a "likelySubtag" element with the correct script and territory information exists.
If there is no such element, add the appropriate "likelySubtag" element.

#### Create the new locale files
- If you are adding a single new language locale, for example, language "xx" as spoken in country "YY", you will need two files:
	- **common/main/xx.xml** \- The main locale file containing the core data. This will include:
		- layout information (which can be omitted if the character order is left-to-right and line order is top-to-bottom)
		- exemplar sets for main, auxiliary, numbers, punctuation
			- Check to see that data is properly "escaped". (See [UnicodeSet](https://www.unicode.org/reports/tr35/tr35.html#unicode-sets) for a list of characters that need to be escaped.)
				- Some commonly used characters are: `\- \: \[ \] \{ \} &quot; \&amp`
				- Less commonly used characters include: `\\ \^ &lt; &gt;`
			- For testing exemplar sets, https://util.unicode.org/UnicodeJsps/list-unicodeset.jsp may be useful.

	- **common/main/xx\_YY.xml** \- An empty country locale containing the identification of xx\_YY as a valid locale. 
- If you are adding locales for a language with multiple scripts, additional files are needed.
For example, when Sunuwar ("suz") was added, files for Devanagari ("Deva") and Sunuwar ("Sunu") scripts were needed.
See [PR#4651](https://github.com/unicode-org/cldr/pull/4651).
- Check for possible clashes with similarly named files in [exemplars/main](https://github.com/unicode-org/cldr/tree/main/exemplars/main).
(For example, having an exemplars/main/xyz.xml file when common/main/xyz.xml is added will cause an error.)
If such a file is found, remove it (after checking whether any information in the file is useful).

#### Supply metadata
- In **common/supplemental/attributeValueValidity.xml**, add the language subtag to
	- `<variable id\="$language" type\="choice">` for TC locales, or
	- `<variable id\="$languageNonTcLtBasic" type\="choice">` for non-TC locales
- In **common/supplemental/supplementalMetadata.xml**, add the appropriate default content locale to \<defaultContent locales\="..."\>
	- The default content locale will include language and region (and possibly script, for multi-script languages).
- Consider whether the locale needs parent locale information (which it probably won't)
	- If parent inheritance can be determined by truncating subtags (and then falling back to root), no parent locale information is needed.
In the majority of cases this will be true.
	- If the locale is written in the non-default script for the language, it needs to have the parent specified as root,
for example, zh_Hant and sr_Latn need to fall back to root (not to zh and sr which are in different scripts).
	- Similarly, en_GB inherits from en_001 not from en, and nb inherits from no, not root.
	- Make the appropriate entry to a parentLocale element in **common/supplemental/supplementalData.xml**
- Verify or add time cycle information in **common/supplemental/supplementalData.xml**
	- In the majority of cases no changes will be needed.
	- In general terms, check that, under the `<timeData>` element,
the `<hours>` element that has an attribute `"regions"` that contains the region for this locale,
also has a `"preferred"` attribute that corresponds to the time cycle selected for the locale.
	- NB: Even though the attribute is `"regions"`, it can contain a region or a locale.
Consider the example of `"fr_CA"` in the `"regions"` attribute of an `"hours"` element where the `"preferred"` attribute is `"H"`,
in contrast to `"CA"` which is in the `"regions"` attribute of an `"hours"` element where `"preferred"` attribute is `"h"`.
This corresponds to Canadian French (fr_CA) using 24-hour time even though 12-hour time is preferred for Canada (region CA).
	- For the locale submission for `"pap"` (ticket [CLDR-14872](https://unicode-org.atlassian.net/browse/CLDR-14872)
and pull request [PR#2542](https://github.com/unicode-org/cldr/pull/2542)),
the region `"CW"` did not appear in the `"timeData"` information and needed to be added to the appropriate location.

#### Optional additions
- Plural rules are not needed until the locale needs to move beyond Basic level,
but it is **strongly recommended** that plural rule information be collected as early as possible.
   - The addition of new plural forms to a language after data collection at higher levels has begun
will cause the coverage percentage to drop significantly until the data for plurals can be provided.
The addition of new plural forms can also be disruptive for any localized applications which are relying on CLDR plural data.
   - If plural rule information is available, it can be added at the same time the locale is established.
   - The simplest case is when the rules are identical to another locale's: it then just needs to be added to the list of locales with those rules.
   - Add the rules to **common/supplemental/plurals.xml** and **common/supplemental/ordinals.xml**.
- Similarly day period rules can be added to **common/supplemental/dayPeriods.xml**.

## Submit the pull request

- If requested, add to vendor targets in **tools/cldr-code/src/main/resources/org/unicode/cldr/util/data/Locales.txt**,
and to CLDR where resources are committed.
- Commit your work to a branch and create a Pull Request.
- Check the results from the tests (which are automatically run when a PR is created or updated) and make corrections as necessary.
- The new locale will be included in Smoketest when the PR is approved and merged,
and will be in production once a push to production occurs.

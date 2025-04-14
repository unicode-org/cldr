---
title: Updating Script Metadata
---

# Updating Script Metadata

### New Unicode scripts

We should work on script metadata early for a Unicode version, so that it is available for tools (such as Mark's "UCA" tools).

- Unicode 9/CLDR 29: New scripts in CLDR but not yet in ICU caused trouble.
- Unicode 10: Working on a pre\-CLDR\-31 branch, plan to merge into CLDR trunk after CLDR 31 is done.
- Should the script metadata code live in the Unicode Tools, so that we don't need a CLDR branch during early Unicode next\-version work?

If the new Unicode version's PropertyValueAliases.txt does not have lines for Block and Script properties yet, then create a preliminary version. Diff the Blocks.txt file and UnicodeData.txt to find new scripts. Get the script codes from <http://www.unicode.org/iso15924/codelists.html> . Follow existing patterns for block and script names, especially for abbreviations. Do not add abbreviations (which differ from the long forms) unless there is a well\-established pattern in the existing data.

Aside from instructions below for all script metadata changes, new script codes need English names (common/main/en.xml) and need to be added to common/supplemental/coverageLevels, under key %script100, so that the new script names will show up in the survey tool. For example, see the [changes for new Unicode 8 scripts](https://unicode-org.atlassian.net/browse/CLDR-8109).

Can we add new scripts in CLDR *trunk* before or only after adding them to CLDR's copy of ICU4J? We did add new Unicode 9 scripts in CLDR 29 before adding them to ICU4J. The CLDR unit tests do not fail any more for scripts that are newer than the Unicode version in CLDR's copy of ICU.

### Sample characters

We need sample characters for the "UCA" tools for generating FractionalUCA.txt.

Look for patterns of what kinds of characters we have picked for other scripts, for example the script's letter "KA". We basically want a character where people say "that looks Greek", and the same shape should not be used in multiple scripts. So for Latin we use "L", not "A". We usually prefer consonants, if applicable, but it is more important that a character look unique across scripts. It does want to be a *letter*, and if possible should not be a combining mark. It would be nice if the letters were commonly used in the majority language, if there are multiple. Compare with the [charts for existing scripts](http://www.unicode.org/charts/), especially related ones.

### Editing the spreadsheet

Google Spreadsheet: [Script Metadata](https://docs.google.com/spreadsheets/d/1Y90M0Ie3MUJ6UVCRDOypOtijlMDLNNyyLk36T6iMu0o/edit#gid=0)

Use and copy cell formulas rather than duplicating contents, if possible. Look for which cells have formulas in existing data, especially for Unicode 1\.1 and 7\.0 scripts.

For example,

- Script names should only be entered on the LikelyLanguage sheet. Other sheets should use a formula to map from the script code.
- On the Samples sheet, use a formula to map from the code point to the actual character. This is especially important for avoiding mistakes since almost no one will have font support for the new scripts, which means that most people will see "Tofu" glyphs for the sample characters.

### Script Metadata properties file
1. Go to the spreadsheet [Script Metadata](https://docs.google.com/spreadsheets/d/1Y90M0Ie3MUJ6UVCRDOypOtijlMDLNNyyLk36T6iMu0o/edit#gid=0)
	1. File\>Download as\>Comma Separated Values
	2. Location/Name \= {CLDR}/tools/cldr\-code/src/main/resources/org/unicode/cldr/util/data/Script\_Metadata.csv
	3. Refresh files (eclipse), then compare with previous version for sanity check. If there are no new scripts for target Unicode version of CLDR release you're working on, then skip the rest of steps below. For example, script "Toto" is ignore for CLDR 39 because target Unicode release of CLDR 39 is Unicode 13 and "Toto" will be added in Unicode 14\.
2. **Note: VM arguments**
	1. Each tool (and test) needs   \-DCLDR\_DIR\=/usr/local/google/home/mscherer/cldr/uni/src   (or wherever your repo root is)
	2. It is easiest to set this once in the global Preferences, rather than in the Run Configuration for each tool.
	3. Most of these tools also need   \-DSCRIPT\_UNICODE\_VERSION\=14   (set to the upcoming Unicode version), but it is easier to edit the ScriptMetadata.java line that sets the UNICODE\_VERSION variable.
	4. Run {cldr}/tools/cldr\-code/src/test/java/org/unicode/cldr/unittest/TestScriptMetadata.java
	5. A common error is if some of the data from the spreadsheet is missing, or has incorrect values.
3. Run GenerateScriptMetadata, which will produce a modified [common/properties/scriptMetadata.txt](https://github.com/unicode-org/cldr/blob/main/common/properties/scriptMetadata.txt) file.
	1. If this ignores the new scripts: Check the \-DSCRIPT\_UNICODE\_VERSION or the ScriptMetadata.java UNICODE\_VERSION.
	2. Add the English script names (from the script metadata spreadsheet) to common/main/en.xml.
	3. Add the French script names from [ISO 15924](https://www.unicode.org/iso15924/iso15924-codes.html) to common/main/fr.xml, but mark them as draft\="provisional".
	4. Add the script codes to common/supplemental/coverageLevels.xml (under key %script100\) so that the new script names will show up in the CLDR survey tool.
		1. See [\#8109\#comment:4](https://unicode-org.atlassian.net/browse/CLDR-8109#comment:4) [r11491](https://github.com/unicode-org/cldr/commit/1d6f2a4db84cc449983c7a01e5a2679dc1827598)
		2. See changes for Unicode 10: <http://unicode.org/cldr/trac/review/9882>
		3. See changes for Unicode 12: [CLDR\-11478](https://unicode-org.atlassian.net/browse/CLDR-11478) [commit/647ce01](https://github.com/unicode-org/cldr/commit/be3000629ca3af2ae77de6304480abefe647ce01)
	5. Maybe add the script codes to TestCoverageLevel.java variable script100\.
		1. Starting with [cldr/pull/1296](https://github.com/unicode-org/cldr/pull/1296) we should not need to list a script here explicitly unless it is Identifier\_Type\=Recommended.
	6. Remove new script codes from $scriptNonUnicode in common/supplemental/attributeValueValidity.xml if needed
	7. For the following step to work as expected, the CLDR copy of the IANA BCP 47 language subtag registry must be updated (at least with the new script codes).
		1. Copy the latest version of https://www.iana.org/assignments/language-subtag-registry/language-subtag-registry to {CLDR}/tools/cldr\-code/src/main/resources/org/unicode/cldr/util/data/language\-subtag\-registry
		2. Consider copying only the new script subtags (and making a note near the top of the CLDR file, or lines like "Comments: Unicode 14 script manually added 2021\-06\-01") to avoid having to update other parts of CLDR.
	8. Run GenerateValidityXML.java like this:
		1. See [Update Validity XML](/development/updating-codes/update-validity-xml)
		2. This needs the previous version of CLDR in a sibling folder.
			1. see [Creating the Archive](/development/creating-the-archive) for details on running the CheckoutArchive tool
		3. Now run GenerateValidityXML.java
		4. If this crashes with a NullPointerException trying to create a Validity object, check that ToolConstants.LAST\_RELEASE\_VERSION is set to the actual last release.
			1. Currently, the CHART\_VERSION must be a simple integer, no ".1" suffix.
	9. At least script.xml should show the new scripts. The generator overwrites the source data file; use ```git diff``` or ```git difftool``` to make sure the new scripts have been added.
	10. Run GenerateLikelySubtags, [as described on the likelysubtags page](/development/updating-codes/likelysubtags-and-default-content), which generates another two files.
	11. It modifies repo files directly. Compare generated files with previous versions for reasonable changes.
	    1. Watch for new mappings changing ones tagged with `origin="sil1"` (beyond losing this tag).
	       These might be over-eager inversions of script metadata script-to-language mappings
	       becoming language-to-script mappings.
	       Check with Lorna Evans and other sources for
	       whether a new script should really become the default script for the language.
	       If the SIL mapping should be preserved, or if we need a different mapping,
	       then try to work with `GenerateLikelySubtags.MAX_ADDITIONS`.
	12. Run the CLDR unit tests.
		1. Project cldr\-core: Debug As \> Maven test
	13. These tests have sometimes failed:
		1. LikelySubtagsTest
		2. TestInheritance
		3. They may need special adjustments, for example in GenerateLikelySubtags.java adding an extra entry to its `MAX_ADDITIONS` or `LANGUAGE_OVERRIDES`.
4. Check in the updated files.

Problems are typically because a non\-standard name is used for a territory name. That can be fixed and the process rerun.


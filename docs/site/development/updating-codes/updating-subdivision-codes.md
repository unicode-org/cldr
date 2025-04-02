---
title: Updating Subdivision Codes
---

# Updating Subdivision Codes

## Main Process

1. Get the latest version of the iso subdivision xml file from https://www.iso.org/obp/ui/ (you'll need a password) and add it to a cldr\-private directory:
	1. Click on the XML button to download a zip, and unzip into folder **iso\_country\_code\_ALL\_xml**
	2. Open **iso\_country\_codes.xml** in that folder. Find the generated line, eg \<country\-codes generated\="2016\-12\-09T08:22:27\.508296\+01:00"\>
	3. Add that date to the folder name, **2016\-12\-09\_iso\_country\_code\_ALL\_xml**
	4. Post that folder into [/cldr\-private/external/iso\_country\_codes](https://goto.google.com/isocountrycodes)/ if not already there.
	5. Copy the contents of the folder to {cldr\-private}/iso\_country\_codes/iso\_country\_codes.xml also (overriding current contents.
	6. Make sure that you have defined \-DCLDR\_PRIVATE\_DATA\="\<your directory\>/cldr\-private/"
	7. ~~Diff just to see what's new.~~
		1. Actually, this step is too painful, because ISO doesn't have a canonical XML format. So elements of a table come in random order... Sometimes
			1. \<subdivision\-code footnote\="\*"\>AZ\-ORD\</subdivision\-code\>
			2. \<subdivision\-code footnote\="\*"\>AZ\-SAD\</subdivision\-code\>
		2. And sometimes the reverse!
		3. May add diffs generation to GenerateSubdivisions...
	8. Run GenerateSubdivisions; it will create a number of files. The important ones are:
	9. {generated}/subdivision/subdivisions.xml
	10. {generated}/subdivision/subdivisionAliases.txt
	11. {generated}/subdivision/en.xml
	12. Diff {generated}**subdivisions.xml** and {workspace}/cldr/common/supplemental/**subdivisions.xml**
		1. If they not different (other than date/version/revision), skip to Step 4\.
		2. Copy the generated contents into the cldr file, and save.
		3. Make sure the added IDs make sense.
		4. Verify that we NEVER remove an ID. See [\#8735](http://unicode.org/cldr/trac/ticket/8735).
			1. An ID may be deprecated; in that case it should show up in **subdivisionAliases.txt** *if there is a good substitute.*
			2. We may need to add a 4\-letter code in case ISO messes up.
			3. In either of these cases, change GenerateSubdivisions.java to do the right thing.
		5. Save the Diffs, since they are useful for updating aliases. See example at end.
	13. Open up {workspace}/cldr/common/supplemental/**supplementalMetadata.xml**
		1. Search for \<!\-\- start of data generated with GenerateSubdivisions \-\-\>
		2. Replace the line after that up to the line before \<!\-\- end of data generated with GenerateSubdivisions \-\-\> with the contents of **subdivisionAliases.txt**
		3. Do a diff with the last release version. The new file should preserve the old aliases.
			1. *Note: there is a tool problem where some lines are duplicated. For now, check and fix them.*
			2. *If a line is duplicated, when you run the tests they will show as errors.*
			3. Make sure the changes make sense.
		4. ***IN PARTICULAR, make sure that NO former types (in*** ***uncommented*** ***lines) disappear!That is, restore any such lines before committing.) Put them below the line:***
			- \<!\-\- end of data generated with GenerateSubdivisions \-\-\>
		5. ***(Ideally the tool would do that, but we're not quite there.)***
	14. Use the names to add more aliases. (See Fixing). Check https://www.iso.org/obp/ui/#iso:code:3166:TW (replacing TW by the country code) to see notes there.
2. Put **en.xml** into {workspace}/cldr/common/subdivisions/
	1. You'll overwrite the one there. The new one reuses all the old names where they exist.
	2. Do a diff with the last release.
		1. Make sure the added names (from ISO) are consistent.
		2. Verify that we NEVER remove an ID. (The deprecated ones move down, but don't disappear).
3. Run the [Update Validity XML](/development/updating-codes/update-validity-xml) steps to produce a new {workspace}/cldr/common/validity/subdivision.xml
	1. Don't bother with the others, but diff and update that one.
	2. A code may move to deprecated, but it should never disappear. If you find that, then revisit \#4 (supplementalMetadata) above
4. Run the tests
	1. You may get some collisions in English. Those need to be fixed.
	2. Google various combinations like \[country code \<first\> \<second\>] to find articles like [ISO\_3166\-2:UG](https://en.wikipedia.org/wiki/ISO_3166-2:UG), then make a fix.
	3. Often a sub\-subdivision has the same name as a subdivision. When that is the case add a qualifier to the lesser know one, like "City" or "District".
	4. Sometimes a name will change in ISO to correct a mistake, which can cause a collision.
5. Fix the ?? in supplemental data (where possible; see below)

## Fixing ??

1. If there are not known new subdivisions that the old ones should map to, you'll see commented\-out lines in **supplementalMetadata** like:
	- \<!\-\- \<subdivisionAlias type\="BA\-01" replacement\="??" reason\="deprecated"/\> \<!\- \- Una\-Sana \=\> ?? \-\-\>
2. As many of these as possible, see if there is a mapping to one or more new subdivisions. That is, where possible, track down the best code(s) to map all of these to, and uncomment the line, and move BELOW \<!\-\- Curated subdivision data \-\-\>
	- Note that for the name comment, change \<!\- \- to \<!\-\-
3. First step is to go to https://www.iso.org/obp/ui/\#iso:code:3166:CZ (replacing CZ by the country code in question).
	1. Look at the change history at the bottom.
4. Other sources, in case \#3 doesn't pan out.
	1. Go to Wikipedia for the country, and looking for "administrative divisions". That can lead you to sections like: https://en.wikipedia.org/wiki/Local_government_in_Northern_Ireland#History, to track down what happened to the old subdivisions.
	2. In some cases it's very clear; a new major subdivision has the same contents as an old one, so it is just a rename. The following describes a process for handling that.

### Old data

\<subgroup type\="CZ" contains\="JC JM KA KR LI MO OL PA PL PR ST US VY ZL"/\>

**\<subgroup type\="CZ" subtype\="JC" contains\="311 312 313 314 315 316 317"/\>**

\<subgroup type\="CZ" subtype\="JM" contains\="621 622 623 624 625 626 627"/\>

**\<subgroup type\="CZ" subtype\="KA" contains\="411 412 413"/\>**

\<subgroup type\="CZ" subtype\="KR" contains\="521 522 523 524 525"/\>

\<subgroup type\="CZ" subtype\="LI" contains\="511 512 513 514"/\>

\<subgroup type\="CZ" subtype\="MO" contains\="801 802 803 804 805 806"/\>

...

### New data

\<subgroup type\="CZ" contains\="10 20 31 32 41 42 51 52 53 63 64 71 72 80"/\>

\<subgroup type\="CZ" subtype\="10" contains\="101 102 103 104 105 106 107 108 109 110 111 112 113 114 115 116 117 118 119 120 121 122"/\>

\<subgroup type\="CZ" subtype\="20" contains\="20A 20B 20C 201 202 203 204 205 206 207 208 209"/\>

**\<subgroup type\="CZ" subtype\="31" contains\="311 312 313 314 315 316 317"/\>**

\<subgroup type\="CZ" subtype\="32" contains\="321 322 323 324 325 326 327"/\>

**\<subgroup type\="CZ" subtype\="41" contains\="411 412 413"/\>**

\<subgroup type\="CZ" subtype\="42" contains\="421 422 423 424 425 426 427"/\>

...

### Exact matches

From this, we can see that items have been renamed. Easiest to add the type values and contains values to a [spreadsheet](https://docs.google.com/spreadsheets/d/1i3YAhD9ADP6d4j6p4s3lY0psNdlOuknBr4ZrX1mihCw/edit) (use regex to extract), marking with old/new. Then sort, and pick out the ones that match.

| Source |  | old | new | contents | Mechanical |
|---|---|---|---|---|---|
| \<subgroup type="FR" subtype="H" contains="2A 2B"/\> | FR | "H" |  | "2A 2B" | \<subdivisionAlias type="FR-H" replacement="FR-COR" reason="deprecated"/\> |
| \<subgroup type="FR" subtype="COR" contains="2A 2B"/\> | FR |  | "COR" | "2A 2B" |  |

### Partial Matches

Rearrange the leftovers to see if there is any OLD \=\> NEW1\+NEW2\... cases or OLD1 \= NEW, OLD2\=NEW cases. For example, for FR we get Q\=\>NOR and P\=\>NOR. Remember that these are "best fit", so there may be small discrepancies.

| Source |  | old | new | contents | Mechanical |  |
|---|---|---|---|---|---|---|
| Source |  | old | new | contents | Mechanical | Fixed ?? cases |
| \<subgroup type="FR" subtype="Q" contains="27 76"/\> | FR | "Q" |  | "27 76" | \<subdivisionAlias type="FR-Q" replacement="FR-?? reason="deprecated"/\> | \<subdivisionAlias type="FR-Q" replacement="FR-NOR" reason="deprecated"/\> |
| \<subgroup type="FR" subtype="P" contains="14 50 61"/\> | FR | "P" |  | "14 50 61" | \<subdivisionAlias type="FR-P" replacement="FR-?? reason="deprecated"/\> | \<subdivisionAlias type="FR-P" replacement="FR-NOR" reason="deprecated"/\> |
| \<subgroup type="FR" subtype="NOR" contains="14 27 50 61 76"/\> | FR |  | "NOR" | "14 27 50 61 76" |  |  |


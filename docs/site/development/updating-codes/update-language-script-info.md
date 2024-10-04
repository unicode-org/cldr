---
title: Update Language Script Info
---

# Update Language Script Info

### Main

1. https://github.com/unicode-org/cldr/tree/main/tools/cldr-code/src/main/resources/org/unicode/cldr/util/data has files with this form:
	1. **country\_language\_population.tsv**
	2. **language\_script.tsv**
	3. For a descriptions of the contents, see [Language Script Guidelines](/development/updating-codes/update-language-script-info/language-script-description)
		1. Do not edit the above files with a plain text editor; they are tab\-delimited UTF\-8 with many fields and should be imported/edited with a spreadsheet editor. Excel or Google sheets should also work fine.
2. The world bank, un, and factbook data should be updated as per [Updating Population, GDP, Literacy](/development/updating-codes/updating-population-gdp-literacy)
3. Note that there is an auxiliary file **util/data/external/other\_country\_data.txt**, which contains data that supplements the others. If there are errors below because the country population is less than the language population, then that file may need updating.
	1. Run the tool **ConvertLanguageData**.
		1. \-DADD\_POP\=**true**; for error messages.
			1. If there are any different country names, you'll get an error:  edit external/alternate\_country\_names.txt to add them.
			2. Look for failures in the language vs script data, following the line:
				- Problems in **language\_script.tsv**
			3. Look for Territory Language data, following the line:
				- **Possible Failures ...**
					- In Basic Data but not Population \> 20%
					- and the reverse.
			4. Look for general problems, following the line:
				- **Failures in Output.**
					- It will also warn if a country doesn't have an official or de facto official language.
			5. Work until resolved.
	2. *The tool updates in place*  **{cldrdata}/common/supplemental/supplementalData.xml**
	3. Carefully diff
	4. Then run QuickCheck to verify that the DTD is in order, and commit.

### Update the supplementalData.xml \<territoryContainment\>

1. For UN M.49 codes, see [Updating UN Codes](/development/updating-codes/updating-un-codes)
2. For the UN, go to https://www.un.org/en/member-states/index.html. Copy the table, and paste into util/data/external/un\_member\_states\_raw.txt. Diff with old. **BROKEN LINK**
3. For the EU, see instructions on [Updating UN Codes](/development/updating-codes/updating-un-codes)
4. For the EZ, do the same with <http://ec.europa.eu/economy_finance/euro/adoption/euro_area/index_en.htm>, into util/data/external/ez\_member\_states\_raw.txt  **BROKEN LINK**
	1. If there are changes, update \<territoryContainment\>


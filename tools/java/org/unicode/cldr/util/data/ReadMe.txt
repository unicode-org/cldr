UPDATING CODES
Periodically, the language, territory, script, currency, and country codes change.
This document describes how to update CLDR when this happens.
For the purposes of this document, C:\cvsdata\ is the CVS root for the CLDR data, and / and \ are equivalent.

1. Do the following to update to a new language tag registry 
(which updates language codes, script codes, and territory codes):

- Go to http://www.iana.org/assignments/language-subtag-registry
  (you can set up a watch for changes in this page with http://www.watchthatpage.com)
- Save as C:\cvsdata\unicode\cldr\tools\java\org\unicode\cldr\util\data\language-subtag-registry
    - If using Eclipse, refresh the files
- Diff with the old copy (via CVS) to check for consistency
- Any new codes need a corresponding update in supplementalMetadata.xml
  Run CountItems -DCLDR_DTD_CACHE=C:\cldrcache\ -Dfile.encoding=UTF-8 -DSHOW_FILES -Xmx512M -Dmethod=getSubtagVariables
	Replace sections of supplementalMetadata.xml
- Edit common/main/en.xml to add the new names, based on the Descriptions in the registry file.
    - The format of the registry is screwy: you can use http://demo.icu-project.org/icu-bin/translit
      to convert forms like N&#x2019;Ko into N’Ko
- If the associations to script or territory are known, edit supplementalData.xml
	- for example, add to <languageData>
		<language type="gsw" scripts="Latn" territories="CH"/>
- If the code becomes deprecated, then add to supplementalMetadata under alias
    - If there is a single replacement add it.
- For territories (regions), add to the territoryContainment in supplementalData.xml
    - The data for that is at the UN site: http://unstats.un.org/unsd/methods/m49/m49regin.htm
    - Grep through that whole file to fix currencies, etc.
- Update util/data/territory_codes.txt
    - This step will be different once the data is moved into SupplementalData.xml
    - Then run GenerateEnums.java, and make sure it completes with no exceptions.
- Run CheckCLDR -fen
	- If you missed any codes, you will get error message: "Unexpected Attribute Value"
- Run ShowLanguages -DSHOW_FILES to regenerate the supplemental.html files
    - Generate them in ...\unicode\cldr\diff\supplemental\...
	- Post to http://www.unicode.org/cldr/data/diff/supplemental/supplemental.html
	
2. Do the following to update to new currency codes.
  [Note: this is more complicated than it should be: we should simplify the process]
  
- Go to http://www.iso.org/iso/en/prods-services/popstds/currencycodeslist.html
  (you can set up a watch for changes in this page with http://www.watchthatpage.com)
  WARNING: this needs to be checked periodically, since ISO does not keep accurate histories!
  Best is to get on the notification list. If one is missed, try
  http://web.archive.org/web/*sa_/http://www.iso.org/iso/en/prods-services/popstds/currencycodeslist.html
  Also see http://publications.europa.eu/code/en/en-5000700.htm for background info.
- Open the page in IE (not other browsers), and 
  select from just before the first character in the line "ISO 4217 Currency names and code elements"
  to just after the last character in "Last modified yyyy-mm-dd", and copy.
- Open the page ...org\unicode\cldr\util\data\currencyCodesList.txt from inside Excel
  Select all, Delete
  Select A1, Paste, Save
  You should now have a tab-delimited plaintext file
  Use CVS Diff to verify the contents.
- If you are using Eclipse, refresh the file system (F5)
  Run CountItems -Dmethod=generateCurrencyItems to generate the new currency list.
  Verify that no currencies are removed. If one would be, add it to IsoCurrencyParser.oldValues,
  and rerun CountItems.
- It will print a list of items at the end that need to be added to the ISO4217.txt file. Add as described below.
- CountItems will also list the data from the file, for cross-checking SupplementalData
  If any country changes the use of a currency,
    verify that there is a corresponding entry in SupplementalData
    Since ISO doesn't publish the exact data change (!), just make sure it is ballpark.
    For new stuff, see below.
    
- Adding a currency:
  
  In SupplementalData:
  If it has unusual rounding or number of digits, add to:
          <fractions>
            <info iso4217="ADP" digits="0" rounding="0"/>
            ...
  For each country in which it comes into use, add a line for when it becomes valid
          <region iso3166="TR">
            <currency iso4217="TRY" from="2005-01-01"/>

  Add the code to the file java/org/unicode/cldr/util/data/ISO4217.txt. This is important, since it is used
  to get the valid codes for the survey tool. Example:
  		currency	|	TRY	|	new Turkish Lira	|	TR	|	TURKEY	|	C

  Mark the old code in java/org/unicode/cldr/util/data/ISO4217.txt as deprecated.

  		currency	|	TRL	|	Old Turkish Lira	|	TR	|	TURKEY	|	O
  
- Changing currency.
  If the currency goes out of use in a country, then add the last day of use, such as:
  
            <region iso3166="TR">
              <currency iso4217="TRL" from="1922-11-01"/>
              =>
            <region iso3166="TR">
              <currency iso4217="TRL" from="1922-11-01" to="2005-12-31"/>
  
- Edit common/main/en.xml to add the new names (or change old ones) based on the descriptions.
  If there is a collision between a new and old name, the old one typically changes to
  "old X" or "X (1983-2003)".

3. NEW TIMEZONE DATABASE
- Do the following to update to a new timezone database.
- Download the latest version of the data from ftp://elsie.nci.nih.gov/pub/
- Unpack, and copy contents into ...org\unicode\cldr\util\data
- Edit the file tzdb-version.txt to change the version, eg for the file tzdata2007a.tar.gz, the version is 2007a.
- If you are using Eclipse, remember to refresh the project.

A. Diff zone.tab.
- if any IDs in zone.tab changed name, 
- add the mapping to org.unicode.cldr.util.ZoneParser.FIX_UNSTABLE_TZID_DATA
   The format is <new name>, <old name>
   Eg {"America/Argentina/Buenos_Aires", "America/Buenos_Aires"},

B. Now Verify
- Run CountItems -Dmethod=genSupplementalZoneData to generate new data.
- Paste the zoneItems into supplementalData.xml
- Paste the $tzid into supplementalMetadata.xml
- Produce Diffs for both files.
- CAREFULLY COMPARE THE RESULTS TO THE LAST VERSION FOR BOTH
  - In supplementalData.xml, any changed name from zone.tab must show a diff like:
  old:	<zoneItem type="Atlantic/Faeroe" territory="FO"/>
  new:	<zoneItem type="Atlantic/Faeroe" territory="FO" aliases="Atlantic/Faroe"/>
  That is, the old name must not change, just add new aliases.
  - In both files, any new zone.tab ID must show up, eg.
  supplementalData.xml, new: 			<zoneItem type="Australia/Eucla" territory="AU"/>
  supplementalMetadata.xml, new: 				Australia/Eucla

C. REMOVED IDs. This doesn't happen very often, but requires some real thought when it does.

  If there are any $tzid's that are in the last version that are not in the current,
  find out what "real" alias it should point to. There are 2 types.
  
  1. ID was removed completely, like HST. In that case, there will be no item or alias in zoneItem's.
  Look at the old supplemental data to determine what it used to map to (eg Pacific/Honolulu)
  - Add to supplementalMetaData
  	 <zoneAlias type="HST" replacement="Pacific/Honolulu"/>
  - Add CountItems.ADD_ZONE_ALIASES_DATA
     eg {"HST", "Pacific/Honolulu"},
  - Rerun CountItems.genSupplementalZoneData()
  
  2. ID is no longer in zone.tab (after fixing "A"), but is still linked.
  In that case, there will still be a zoneItem as an alias, eg
  			<zoneItem type="Africa/Bamako" territory="ML" aliases="Africa/Timbuktu"/>
  - Add to supplementalMetadata. Use the zoneItem info
  	 <zoneAlias type="Africa/Timbuktu" replacement="Pacific/Honolulu"/>
  - Add to CountItems.FIX_DEPRECATED_ZONE_DATA. Example:
      	"Africa/Timbuktu",
  - Rerun CountItems.genSupplementalZoneData()
  
  Sometimes the aliases will be on the wrong element. If so, add to org.unicode.cldr.util.ZoneParser.PREFERRED_BASES.
  
D. Repeat B and C until done.
- When you are all done, there should ONLY be additions to $tzid and zoneItem's.

E. Check in the new supplemental files and the data files in ...org\unicode\cldr\util\data
- just the data files that are already checked in (eg check in 'africa', but not 'factory')

(Note: the code in CountItems and StandardCodes means that we have to duplicate a bit of process above, but
for now it's not worth fixing.)


UPDATING LANGUAGE/COUNTRY INFORMATION
  Below, the directory C:\cvsdata\unicode stands for wherever the cldr directory is for the locale CVS.

1. Get the spreadsheet from Rick, with a name of the form: country_language_population-0403.xls
2. Open, and pick the Save As... menu
Pick "Save as type:" Text (Tab delimited) (*.txt)
And "File name:" C:\cvsdata\unicode\cldr\tools\java\org\unicode\cldr\util\data\country_language_population_raw.txt
Respond OK, Yes; then close Excel (clicking No)
3. In CVS, diff C:\cvsdata\unicode\cldr\tools\java\org\unicode\cldr\util\data\country_language_population_raw.txt and sanity-check the changes.
4. Run the tool ConvertLanguageData.
5. At the bottom are a list of Failures. It will also warn if a country doesn't have an official
or de facto official language. Send those back to Rick. 
6. Also send the lists: In Basic Data but not Population > 20% and the reverse.

7. Diff the following two files as a sanity check. The fragment only has certain portions of supplemental data, so ignore the parts
it doesn't have:
C:\cvsdata\unicode\cldr\common\supplemental\supplementalData.xml
C:\cvsdata\unicode\cldr\dropbox\gen\supplemental\language_code_fragment.xml
7a. Also compare the following (for the defaultContent section only) 
C:\cvsdata\unicode\cldr\common\supplemental\supplementalMetadata.xml
C:\cvsdata\unicode\cldr\dropbox\gen\supplemental\language_code_fragment_metadata.xml

8. If all looks well, paste in the sections into supplementalData.xml
<territoryInfo>
<references>
8a. And into supplementalMetadata.xml
<defaultContent>

9. Then run QuickCheck to verify that the DTD is in order, and check in.
10. Then run ShowLanguages to generate the charts
11. Sanity check them, and check in.

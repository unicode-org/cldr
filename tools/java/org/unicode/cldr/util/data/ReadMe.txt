UPDATING CODES
Periodically, the language, territory, script, currency, and country codes change.
This document describes how to update CLDR when this happens.

1. Do the following to update to a new language tag registry 
(which updates language codes, script codes, and territory codes):

- Go to http://www.iana.org/assignments/language-subtag-registry
  (you can set up a watch for changes in this page with http://www.watchthatpage.com)
- Copy into org.unicode.cldr.util.data
    - If using Eclipse, refresh the files
- Diff with the old copy (via CVS)
- Any new codes need a corresponding update in supplementalMetadata.xml
	- languages: search for <variable id="$language", add to list
	- territories: search for <variable id="$territory", add to list
	- scripts: search for <variable id="$territory", add to list
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

- Go to http://www.iso.org/iso/en/prods-services/popstds/currencycodeslist.html
  (you can set up a watch for changes in this page with http://www.watchthatpage.com)
  WARNING: this needs to be checked periodically, since ISO does not keep accurate histories.
  Best is to get on the notification list.
  
- Adding a currency:
  
  Add the currency to supplementalMetadata.xml, in 
  <variable id="$currency" type="choice">ADP AED AFA AFN .... ZAL ZAR ZMK ZRN ZRZ ZWD</variable>
  
  In SupplementalData:
  If it has unusual rounding or number of digits, add to:
          <fractions>
            <info iso4217="ADP" digits="0" rounding="0"/>
            ...
  For each country in which it comes into use, add a line for when it becomes valid
          <region iso3166="TR">
            <currency iso4217="TRY" from="2005-01-01"/>

  Add the code to the test file /util/data/ISO4217.txt
  		currency	|	TRY	|	new Turkish Lira	|	TR	|	TURKEY	|	C
  
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
- Check in just the ones that are already checked in (eg check in 'africa', but not 'factory')
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

(Note: the code in CountItems and StandardCodes means that we have to duplicate a bit of process above, but
for now it's not worth fixing.)

UPDATING CODES
Periodically, the language, territory, script, currency, and country codes change.
This document describes how to update CLDR when this happens.

1. Do the following to update to a new language tag registry 
(which updates language codes, script codes, and territory codes):

- Go to http://www.iana.org/assignments/language-subtag-registry
  (you can set up a watch for changes in this page with WatchThatPage.org)
- Copy into org.unicode.cldr.util.data
- Diff with the old copy (via CVS)
- Any new codes need a corresponding update in supplementalMetadata.xml
	- languages: search for <variable id="$language", add to list
	- territories: search for <variable id="$territory", add to list
	- scripts: search for <variable id="$territory", add to list
- Edit common/main/en.xml to add the new names, based on the Descriptions in the registry file.
- If the associations to script or territory are known, edit supplementalData.xml
	- for example, add to <languageData>
		<language type="gsw" scripts="Latn" territories="CH"/>
- Run CheckCLDR -fen
	- If you missed any codes, you will get error message: "Unexpected Attribute Value"
- Run ShowLanguages to regenerate the supplemental.html file
	- Post to http://www.unicode.org/cldr/data/diff/supplemental/supplemental.html
	
2. Do the following to update to new currency codes.

- Go to http://www.iso.org/iso/en/prods-services/popstds/currencycodeslist.html
  (you can set up a watch for changes in this page with WatchThatPage.org)
  WARNING: this needs to be checked periodically, since ISO does not keep accurate histories.
  Best is to get on the notification list.
  
- Adding a currency:
  
  Add the currency to supplementalMetadata.txt, in 
  <variable id="$currency" type="choice">ADP AED AFA AFN .... ZAL ZAR ZMK ZRN ZRZ ZWD</variable>
  
  In SupplementalData:
  If it has unusual rounding or number of digits, add to:
          <fractions>
            <info iso4217="ADP" digits="0" rounding="0"/>
            ...
  Add the code to the test file /util/data/ISO4217.txt
  		currency	|	TRY	|	new Turkish Lira	|	TR	|	TURKEY	|	C
  
  For each country in which it comes into use, add a line for when it becomes valid
          <region iso3166="TR">
            <currency iso4217="TRY" from="2005-01-01"/>
  
- Changing currency.
  If the currency goes out of use in a country, then change a line, such as:
  
            <region iso3166="TR">
              <currency iso4217="TRL" from="1922-11-01" to="2005-12-31"/>
              =>
              <currency iso4217="TRL" from="1922-11-01"/>
  
[TBD]

3. NEW TIMEZONE DATABASE
- Do the following to update to a new timezone database.
- Download the latest version of the data from ftp://elsie.nci.nih.gov/pub/
- Unpack, and copy contents into ...org\unicode\cldr\util\data
- Check in just the ones that are already checked in (eg check in 'africa', but not 'factory')

Now Verify
- Run CountItems.genSupplementalZoneData() to generate new data.
  NOTE: should move into separate file to make this more turn-key
- Paste the zoneItems into supplementalData.xml
  Add the version number, <zoneFormatting multizone="001 ... UZ" tzidVersion="2006g">
- Paste the $tzid into supplementalData.xml
- CAREFULLY COMPARE THE RESULTS TO THE LAST VERSION

A. If any IDs in zone.tab changed name, add the mapping to StandardCodes.FIX_UNSTABLE_TZID_DATA
   AND rerun CountItems.genSupplementalZoneData()
   Eg {"America/Argentina/Buenos_Aires", "America/Buenos_Aires"},
   (the format is new name to old name)
   
B. If there are any $tzid's that are in the last version that are not in the current,
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
  - Add to supplementalMetaData. Use the zoneItem info
  	 <zoneAlias type="Africa/Timbuktu" replacement="Pacific/Honolulu"/>
  - Add to CountItems.FIX_DEPRECATED_ZONE_DATA. Example:
      	"Africa/Timbuktu",
  - Rerun CountItems.genSupplementalZoneData()

When you are all done, there should ONLY be additions to $tzid and zoneItem's.

(Note: the code in CountItems and StandardCodes means that we have to duplicate a bit of process above, but
for now it's not worth fixing.)

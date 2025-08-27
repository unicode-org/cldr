---
title: Update Language/Script/Region Subtags
---

# Update Language/Script/Region Subtags

### Updated 2021\-02\-17 by Yoshito Umaoka

### This updates language codes, script codes, and territory codes.

- First get the latest ISO 639\-3 from <https://iso639-3.sil.org/code_tables/download_tables>
    - Download the zip file containing the UTF\-8 tables, it will have a name like iso\-639\-3\_Code\_Tables\_20210202\.zip
    - Unpack the zip file and update files below with the latest version:
        - {CLDR}/tools/cldr\-code/src/main/resources/org/unicode/cldr/util/data/iso\-639\-3\.tab
        - {CLDR}/tools/cldr\-code/src/main/resources/org/unicode/cldr/util/data/iso\-639\-3\_Name\_Index.tab
        - {CLDR}/tools/cldr\-code/src/main/resources/org/unicode/cldr/util/data/iso\-639\-3\-macrolanguages.tab
        - {CLDR}/tools/cldr\-code/src/main/resources/org/unicode/cldr/util/data/iso\-639\-3\_Retirements.tab
    - Take the **latest** version number of the zip files (e.g. iso\-639\-3\_Code\_Tables\_**20210202**.zip), and paste into
        - {CLDR}/tools/cldr\-code/src/main/resources/org/unicode/cldr/util/data/iso\-639\-3\-version.tab
- Go to <https://www.iana.org/assignments/language-subtag-registry/language-subtag-registry>
    - (you can set up a watch for changes in this page with http://www.watchthatpage.com )
    - Save as {CLDR}/tools/cldr\-code/src/main/resources/org/unicode/cldr/util/data/language\-subtag\-registry
- Go to <https://data.iana.org/TLD/>
    - Right\-click on [tlds\-alpha\-by\-domain.txt](http://data.iana.org/TLD/tlds-alpha-by-domain.txt) save as
    - {CLDR}/tools/cldr\-code/src/main/resources/org/unicode/cldr/util//data/[tlds\-alpha\-by\-domain.txt](http://data.iana.org/TLD/tlds-alpha-by-domain.txt)
- If using Eclipse, refresh the files
- Diff each with the old copy to check for consistency
    - Certain of the steps below require that you note certain differences.
- Check if there is a new macrolanguage (marked with M in the second column of the iso\-639\-3\.tab file). (Should automate this, but there typically aren't that many new/changed entries).
- **Update tools/cldr\-code/src/main/resources/org/unicode/cldr/util/data/external/iso\_3166\_status.txt**
    - Go to <https://www.iso.org/obp/ui/#iso:pub:PUB500001:en>
    - Click **Full List of Country Codes**
    - Run the tool **CompareIso3166\_1Status**
    - Click on the "Officially Assigned" code type and also the "Other Codes" code type
    - Compare total counts with tool output:  example "*officially_assigned \|\|  249*"  coinciding with 249 Officially Assigned codes
        - Note 1: the counts are split, so you may need to add up all of the exceptionally_reserved codes etc
        - Note 2: Some formerly used codes are used again, For example `BY` is both formerly used AND officially_assigned (reused).
        - [CLDR-18912] was filed to cover the above two notes.
    - If something is wrong, you'll have to scroll through the code list and/or dig around for the updates
- Check if ISO has done something destabilizing with codes: you need to handle it specially.
- **Record the version: See [Updating External Metadata](/development/updating-codes/external-version-metadata)**
- Do validity checks and regenerate: for details see [Validity](/development/updating-codes/update-validity-xml)
    - You'll have to do this again in [Updating Subdivision Codes](/development/updating-codes/updating-subdivision-codes).
- Edit common/main/en.xml to add any new names, based on the Descriptions in the registry file.
    - *You only need to add new languages and scripts that we add to supplementalMetaData.*
        - But you need all territories.
        - Any new macrolanguages need a language alias.
        - Diff for sanity check
- If the code becomes deprecated, then add to supplementalMetadata under \<alias\>
    - If there is a single replacement add it.
    - Territories can have multiple replacements. Put them in population order.
- There are a few territories that don't yet have a top level domain (TLD) assigned, such as "BQ".
    - If there are new ones added in tlds\-alpha\-by\-domain.txt for a territory already in CLDR, update {cldrdata}\\tools\\java\\org\\unicode\\cldr\\util\\data\\territory\_codes.txt with the new TLD (usually the same as the country code.
- For new territories (regions) **// TODO: automate this more**
    - Add to the territoryContainment in supplementalData.xml
        - The data for that is at the UN site: <https://unstats.un.org/unsd/methodology/m49/>
        - With data from the EU at <https://european-union.europa.eu/principles-countries-history/eu-countries_en>
    - Add to territory\_codes.txt
        - Use the UN mapping above for the 3letter and 3number codes.
        - FIPS is a withdrawn standard as of 2008, so any new territories won't have a FIPS10 code.
        - Look at tlds\-alpha\-by\-domain.txt to see if the new territory has a TLD assigned yet.
        - rerun CountItems above.
    - Add metazone mappings as needed. (TODO: Add protocol)
    - Add the country/lang/population data (TODO: Add protocol)
    - Add the currency data (TODO: Add protocol)
    - ~~Update util/data/territory\_codes.txt~~
        - ~~This step will be different once the data is moved into SupplementalData.xml~~
        - ~~Todo: fix GenerateEnums around Utility.getUTF8Data("territory\_codes.txt");~~
- Then run GenerateEnums.java, and make sure it completes with no exceptions. Fix any necessary results.
    - Missing alpha3 for: xx, or "In RFC 4646 but not in CLDR: \[EA, EZ, IC, UN]"
    - Ignore if it is `{EA, EZ, IC, UN}` Otherwise means you needed to do "For new territories" above
- Collision with: xx
    - Ignore if it is `{MM, BU, 104}, {TP, TL, 626}, {YU, CS, 891}, {ZR, CD, 180}`
- Not in World but in CLDR: \[002, 003, 005, 009, 011, 013, 014, 015, 017\... Ignore 3\-digit coes
    - (should have exception lists in tool for the Ignore's above)
- Run **ConsoleCheckCLDR \-f en \-z FINAL\_TESTING \-e**
    - If you missed any codes, you will get error message: "Unexpected Attribute Value"
- Run all the unit tests.
    - If you get a failure in LikelySubtagsTest because of a new region, you can hack around it with something like:
        - \<likelySubtag from\="und\_202" to\="en\_Latn\_NG"/\>
        - \<!\-\- hack until rebuilt \-\-\>
    - You may also have to fix the coverageLevels.txt file for an error like:
    - Error: (TestCoverageLevel.java:604\) Comprehensive \& no exception for path \=\> //ldml/localeDisplayNames/territories/territory\[@type\="202"]

- [CLDR-18912]: https://unicode-org.atlassian.net/browse/CLDR-18912

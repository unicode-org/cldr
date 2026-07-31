## <a name="Supplemental_Time_Zone_Data" id="Supplemental_Time_Zone_Data" href="#Supplemental_Time_Zone_Data">Supplemental Time Zone Data</a>

### <a name="Metazones" id="Metazones" href="#Metazones">Metazones</a>

```dtd
<!ELEMENT metaZones (metazoneInfo?, mapTimezones?) >

<!ELEMENT metazoneInfo (timezone*) >

<!ELEMENT timezone (usesMetazone*) >
<!ATTLIST timezone type CDATA #REQUIRED >

<!ELEMENT usesMetazone EMPTY >
<!ATTLIST usesMetazone mzone NMTOKEN #REQUIRED >
<!ATTLIST usesMetazone from CDATA #IMPLIED >
<!ATTLIST usesMetazone to CDATA #IMPLIED >
<!ATTLIST usesMetazone stdOffset CDATA #IMPLIED >
<!ATTLIST usesMetazone dstOffset CDATA #IMPLIED >

<!ELEMENT mapTimezones ( mapZone* ) >
<!ATTLIST mapTimezones type NMTOKEN #IMPLIED >
<!ATTLIST mapTimezones typeVersion CDATA #IMPLIED >
<!ATTLIST mapTimezones otherVersion CDATA #IMPLIED >
<!ATTLIST mapTimezones references CDATA #IMPLIED >

<!ELEMENT mapZone EMPTY >
<!ATTLIST mapZone type CDATA #REQUIRED >
<!ATTLIST mapZone other CDATA #REQUIRED >
<!ATTLIST mapZone territory CDATA #IMPLIED >
<!ATTLIST mapZone references CDATA #IMPLIED >
```

* The following subelement of `<metaZones>` provides a mapping from a single Unicode time zone id to metazones. For more information about metazones, see _[Time Zone Names](tr35-dates.md#Time_Zone_Names)_.


```xml
<metazoneInfo>
    <timezone type="Europe/Andorra">
        <usesMetazone mzone="Europe_Central" />
    </timezone>
    ....
    <timezone type="America/Kentucky/Monticello">
        <usesMetazone to="2000-10-29 07:00" mzone="America_Central"/>
        <usesMetazone from="2000-10-29 07:00" mzone="America_Eastern"/>
    </timezone>
    ....
```

* The following subelement of `<metaZones>` specifies a mapping from a metazone to golden zones for each territory. For more information about golden zones, see _[Using Time Zone Names](tr35-dates.md#Using_Time_Zone_Names)_.


```xml
<mapTimezones type="metazones">
    <mapZone other="Acre" territory="001" type="America/Rio_Branco" />
    <mapZone other="Afghanistan" territory="001" type="Asia/Kabul" />
    <mapZone other="Africa_Central" territory="001" type="Africa/Maputo" />
    <mapZone other="Africa_Central" territory="BI" type="Africa/Bujumbura" />
    <mapZone other="Africa_Central" territory="BW" type="Africa/Gaborone" />
    ....
```

### <a name="Windows_Zones" id="Windows_Zones" href="#Windows_Zones">Windows Zones</a>

```dtd
<!ELEMENT windowsZones (mapTimezones?) >
```

The `<mapTimezones>` element can be also used to provide mappings between Unicode time zone IDs and other time zone IDs. This example specifies a mapping from Windows TZIDs to Unicode time zone IDs.

```xml
<mapTimezones otherVersion="07dc0000" typeVersion="2011n">
    ....
    <!-- (UTC-08:00) Baja California -->
    <mapZone other="Pacific Standard Time (Mexico)" territory="001" type="America/Santa_Isabel"/>
    <mapZone other="Pacific Standard Time (Mexico)" territory="MX" type="America/Santa_Isabel"/>

    <!-- (UTC-08:00) Pacific Time (US & Canada) -->
    <mapZone other="Pacific Standard Time" territory="001" type="America/Los_Angeles"/>
    <mapZone other="Pacific Standard Time" territory="CA" type="America/Vancouver America/Dawson America/Whitehorse"/>
    <mapZone other="Pacific Standard Time" territory="MX" type="America/Tijuana"/>
    <mapZone other="Pacific Standard Time" territory="US" type="America/Los_Angeles"/>
    <mapZone other="Pacific Standard Time" territory="ZZ" type="PST8PDT"/>
    ....
```

* The attributes otherVersion and typeVersion in `<mapTimezones>` specify the versions of two systems. In the example above, otherVersion="07dc0000" specifies the version of Windows time zone and typeVersion="2011n" specifies the version of Unicode time zone IDs. The attribute `territory="001"` in `<mapZone>` element indicates the long canonical Unicode time zone ID specified by the `type` attribute is used as the default mapping for the Windows TZID. For each unique Windows TZID, there must be exactly one `<mapZone>` element with `territory="001"`. `<mapZone>` elements other than `territory="001"` specify territory specific mappings. When multiple Unicode time zone IDs are available for a single territory, the value of the `type` attribute will be a list of Unicode time zone IDs delimited by space. In this case, the first entry represents the default mapping for the territory. The territory "ZZ" is used when a Unicode time zone ID is not associated with a specific territory.


**Note:** The long canonical Unicode time zone ID might be deprecated in the tz database [[Olson](tr35.md#Olson)]. For example, CLDR uses "Asia/Calcutta" as the long canonical time zone ID for Kolkata, India. The same ID was moved to 'backward' file and replaced with a new ID "Asia/Kolkata" in the tz database. Therefore, if you want to get an equivalent Windows TZID for a zone ID in the tz database, you have to resolve the long canonical Unicode time zone ID (e.g. "Asia/Calcutta") for the zone ID (e.g. "Asia/Kolkata"). For more details, see [Time Zone Identifiers](tr35.md#Time_Zone_Identifiers).

**Note:** Not all Unicode time zones have equivalent Windows TZID mappings. Also, not all Windows TZIDs have equivalent Unicode time zones. For example, there is no equivalent Windows zone for Unicode time zone "Australia/Lord_Howe", and there is no equivalent Unicode time zone for Windows zone "E. Europe Standard Time" (as of CLDR 25 release).

### <a name="Primary_Zones" id="Primary_Zones" href="#Primary_Zones">Primary Zones</a>

```dtd
<!ELEMENT primaryZones ( primaryZone* ) >
<!ELEMENT primaryZone ( #PCDATA ) >
<!ATTLIST primaryZone iso3166 NMTOKEN #REQUIRED >
```

* This element is for data that is used to format a time zone’s generic location name. Each `<primaryZone>` element specifies the dominant zone for a region; this zone should use the region name for its generic location name even though there are other canonical zones available in the same region. For example, Asia/Shanghai is displayed as "China Time", instead of "Shanghai Time". Sample data:


```xml
<primaryZones>
    <primaryZone iso3166="CL">America/Santiago</primaryZone>
    <primaryZone iso3166="CN">Asia/Shanghai</primaryZone>
    <primaryZone iso3166="DE">Europe/Berlin</primaryZone>
    …
```

* This information was previously specified by the LDML `<singleCountries>` element under each locale’s `<timeZoneNames>` element. However, that approach had inheritance issues, and the data is not really locale-specific anyway.



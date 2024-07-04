---
title: Extended Windows-Olson zid mapping
---

# Extended Windows-Olson zid mapping

**This proposal was approved by the CLDR TC on 2012-01-11 with some minor updates. See update comments.** 

## Background

There are two dominant time zone implementations in operating systems - Windows and Olson time zone database. Software sometimes require to one to another. For example, Java uses Olson time zone database and the default time zone is initialized by platform. When Java is running on Windows OS, it requires to map the Windows system time zone to a matching Olson time zone.

The current version of CLDR (as of 2.0) includes the mapping data from Windows tzid to Olson zone ID. The mapping data is currently 1-to-1 mapping. However, Windows uses a single time zone for larger territories comparing to Olson time zone database, the actual mapping should be 1-to-n.

For example, Windows zone "(UTC-06:00) Central America" (ID: Central America Standard Time) covers Central American regions with UTC offset of -6 hours, with no daylight saving time. In the Olson tz database, America/Belize, America/Costa\_Rica, America/El\_Salvador, America/Guatemala, America/Managua, America/Tegucigalpa, Pacific/Galapagos and Etc/GMT+6 would be equivalent to this zone. For now, CLDR only has America/Guatemala as the mapping. Therefore, a software utilizing the CLDR data returns America/Guatemala even the system locale is CR (Costa Rica). By adding mapping data associated with regions, CLDR users can get better mapping. In this example, if location is CR, Windows Central America Standard Time would be mapped to Olson America/Costa\_Rica.

Another requirement to CLDR is to allow user to map from a Olson time zone to a Windows time zone. In the example above, you can get Windows Central America Standard Time from America/Guatemala, but you cannot get the same Windows ID from America/Costa\_Rica.

The goal of this proposal is to extend the current mapping data (supplemental/windowsZones.xml) to include more Olson time zones associated with regions.

## Design Proposal

### Regional Mapping

supplemental/windowsZones.xml currently use \<mapTimezones> element. The \<mapTimezones> has multiple \<mapZone> child elements. The current data (2.0) look like below.

&emsp;\<windowsZones>

&emsp;&emsp;\<mapTimezones>

&emsp;&emsp;&emsp;\<mapZone other="Afghanistan Standard Time" type="Asia/Kabul"/> \<!-- S (UTC+04:30) Kabul -->

&emsp;&emsp;&emsp;\<mapZone other="Alaskan Standard Time" type="America/Anchorage"/> \<!-- D (UTC-09:00) Alaska -->

&emsp;&emsp;&emsp;\<mapZone other="Arab Standard Time" type="Asia/Riyadh"/> \<!-- S (UTC+03:00) Kuwait, Riyadh -->

&emsp;&emsp;&emsp;\<mapZone other="Arabian Standard Time" type="Asia/Dubai"/> \<!-- S (UTC+04:00) Abu Dhabi, Muscat -->

&emsp;&emsp;&emsp;\<mapZone other="Arabic Standard Time" type="Asia/Baghdad"/> \<!-- S (UTC+03:00) Baghdad -->

....

The \<mapZone> element is also used by supplemental/metaZones.xml for CLDR meta zone - Olson time zone mapping and its definition is below -

\<!ELEMENT mapZone EMPTY >

\<!ATTLIST mapZone type CDATA #REQUIRED >

\<!ATTLIST mapZone other CDATA #REQUIRED >

\<!ATTLIST mapZone territory CDATA #IMPLIED >

\<!ATTLIST mapZone references CDATA #IMPLIED >

metaZones.xml already contains multiple mapping per single meta zone by region like below -

&emsp;&emsp;\<mapZone other="Africa\_Central" territory="001" type="Africa/Maputo"/>

&emsp;&emsp;\<mapZone other="Africa\_Central" territory="BI" type="Africa/Bujumbura"/>

&emsp;&emsp;\<mapZone other="Africa\_Central" territory="BW" type="Africa/Gaborone"/>

&emsp;&emsp;\<mapZone other="Africa\_Central" territory="CD" type="Africa/Lubumbashi"/>

&emsp;&emsp;\<mapZone other="Africa\_Central" territory="MW" type="Africa/Blantyre"/>

&emsp;&emsp;\<mapZone other="Africa\_Central" territory="RW" type="Africa/Kigali"/>

&emsp;&emsp;\<mapZone other="Africa\_Central" territory="ZM" type="Africa/Lusaka"/>

&emsp;&emsp;\<mapZone other="Africa\_Central" territory="ZW" type="Africa/Harare"/>

So we could use the same scheme for representing Windows-Olson mapping. For example, mapping data for Windows "Central America Standard Time" could be represented as below - 

&emsp;&emsp;\<mapZone other="Central America Standard Time" territory="BZ" type="America/Belize"/>

&emsp;&emsp;\<mapZone other="Central America Standard Time" territory="CR" type="America/Costa\_Rica"/>

&emsp;&emsp;\<mapZone other="Central America Standard Time" territory="SV" type="America/El\_Salvador"/>

&emsp;&emsp;\<mapZone other="Central America Standard Time" territory="GT" type="America/Guatemala"/>

&emsp;&emsp;\<mapZone other="Central America Standard Time" territory="NI" type="America/Managua"/>

&emsp;&emsp;\<mapZone other="Central America Standard Time" territory="HN" type="America/Tegucigalpa"/>

&emsp;&emsp;\<mapZone other="Central America Standard Time" territory="EC" type="Pacific/Galapagos"/>

### Default Mapping

The mapping data in supplemental/metaZones.xml is designed for getting a single Olson time zone by a metazone and a region. Therefore, only a single Olson zone is allowed for a unique combination of metazone and region. In this mapping data, we selected a default Olson time zone per metazone/region combination and a global default Olson time zone for all regions (the one associated with "001"). The historical inverse mapping (Olson -> metazone) is represented by \<metazoneInfo> element in the same file.

One of our goal is to support better Olson -> Windows mapping. But having a separated inverse mapping table like the one in metaZones.xml does not make much sense, because a Windows time zone is uniquely determined by a Olson time zone. However, when we add multiple mapping data for a unique combination of a Windows time zone and a region, we have to represent which one is used as default Olson time zone. For example, Windows time zone (UTC-06:00) Saskatchewan (ID: Canada Central Standard Time) is mapped to two Olson time zones - America/Regina and America/Swift\_Current. Both Olson time zones are Canadian (CA). When input is Windows ID "Canada Central Standard Time" and region "CA", we have to pick one. Also, when a Windows time zone is mapped to multiple Olson time zones in multiple regions, we have to pick one for global default for the case region is unknown (similar to "001" zone in metaZones.xml).

For now, I could think of following options for representing defaults:

**[Update] In the CLDR TC call on 2012-01-11, the TC members agreed to take Design Option 3 below.**

Design Option 1 - New attribute to indicate global/regional default

Adding a new attribute "defaultfor" to \<mapZone> element. The value of "defaultfor" attribute is either "all" or "region"

For example, the mapping data for Windows time zone (UTC-05:00) Esstern Time (US & Canada) (ID: Eastern Standard Time) look like below - 

&emsp;&emsp;\<!-- (UTC-05:00) Eastern Time (US & Canada)-->

&emsp;&emsp;\<mapZone other="Eastern Standard Time" territory="BS" type="America/Nassau"/>

&emsp;&emsp;\<mapZone other="Eastern Standard Time" territory="CA" type="America/Iqaluit"/>

&emsp;&emsp;\<mapZone other="Eastern Standard Time" territory="CA" type="America/Montreal"/>

&emsp;&emsp;\<mapZone other="Eastern Standard Time" territory="CA" type="America/Nipigon"/>

&emsp;&emsp;\<mapZone other="Eastern Standard Time" territory="CA" type="America/Pangnirtung"/>

&emsp;&emsp;\<mapZone other="Eastern Standard Time" territory="CA" type="America/Thunder\_Bay"/>

&emsp;&emsp;\<mapZone other="Eastern Standard Time" territory="CA" type="America/Toronto" defaultfor="region"/>

&emsp;&emsp;\<mapZone other="Eastern Standard Time" territory="TC" type="America/Grand\_Turk"/>

&emsp;&emsp;\<mapZone other="Eastern Standard Time" territory="US" type="America/Detroit"/>

&emsp;&emsp;\<mapZone other="Eastern Standard Time" territory="US" type="America/Indiana/Petersburg"/>

&emsp;&emsp;\<mapZone other="Eastern Standard Time" territory="US" type="America/Indiana/Vincennes"/>

&emsp;&emsp;\<mapZone other="Eastern Standard Time" territory="US" type="America/Indiana/Winamac"/>

&emsp;&emsp;\<mapZone other="Eastern Standard Time" territory="US" type="America/Kentucky/Monticello"/>

&emsp;&emsp;\<mapZone other="Eastern Standard Time" territory="US" type="America/Louisville"/>

&emsp;&emsp;\<mapZone other="Eastern Standard Time" territory="US" type="America/New\_York" defaultfor="all"/>

When a \<mapZone> is unique to a region (territory), defaultfor="region" is not required.

Con: The attribute defaultfor does not make sense for the use of \<mapZone> in supplemental/metaZones.xml.

Design Option 2 - Use the ordering to represent the defaults

Use the first \<mapZone> element for a Windows time zone as its default and the first element for a unique combination of Windows time zone and a region as its default for the region. For example,

&emsp;&emsp;\<!-- (UTC-05:00) Eastern Time (US & Canada)-->

&emsp;&emsp;\<mapZone other="Eastern Standard Time" territory="US" type="America/New\_York"/> \<!-- global default for Eastern Standard Time -->

&emsp;&emsp;\<mapZone other="Eastern Standard Time" territory="US" type="America/Detroit"/>

&emsp;&emsp;\<mapZone other="Eastern Standard Time" territory="US" type="America/Indiana/Petersburg"/>

&emsp;&emsp;\<mapZone other="Eastern Standard Time" territory="US" type="America/Indiana/Vincennes"/>

&emsp;&emsp;\<mapZone other="Eastern Standard Time" territory="US" type="America/Indiana/Winamac"/>

&emsp;&emsp;\<mapZone other="Eastern Standard Time" territory="US" type="America/Kentucky/Monticello"/>

&emsp;&emsp;\<mapZone other="Eastern Standard Time" territory="US" type="America/Louisville"/>

&emsp;&emsp;\<mapZone other="Eastern Standard Time" territory="BS" type="America/Nassau"/>

&emsp;&emsp;\<mapZone other="Eastern Standard Time" territory="CA" type="America/Toronto"/> \<!-- regional default for CA -->

&emsp;&emsp;\<mapZone other="Eastern Standard Time" territory="CA" type="America/Iqaluit"/>

&emsp;&emsp;\<mapZone other="Eastern Standard Time" territory="CA" type="America/Montreal"/>

&emsp;&emsp;\<mapZone other="Eastern Standard Time" territory="CA" type="America/Nipigon"/>

&emsp;&emsp;\<mapZone other="Eastern Standard Time" territory="CA" type="America/Pangnirtung"/>

&emsp;&emsp;\<mapZone other="Eastern Standard Time" territory="CA" type="America/Thunder\_Bay"/>

&emsp;&emsp;\<mapZone other="Eastern Standard Time" territory="TC" type="America/Grand\_Turk"/>

Con: The data depends on the order of elements, which could be easily messed up with a certain type of tooling.

Design Option 3 - Use of territory="001" to indicate global default, use a list of Olson time zone in type attribute

Add an extra \<mapZone> element to represent global default using territory "001". Also, use space delimited list in type attribute and use the first one as the regional default. For example,

&emsp;&emsp;\<!-- (UTC-05:00) Eastern Time (US & Canada)-->

&emsp;&emsp;\<mapZone other="Eastern Standard Time" territory="001" type="America/New\_York"/>

&emsp;&emsp;\<mapZone other="Eastern Standard Time" territory="BS" type="America/Nassau"/>

&emsp;&emsp;\<mapZone other="Eastern Standard Time" territory="CA" type="America/Toronto America/Iqaluit America/Montreal America/Nipigon America/Pangnirtung America/Thunder\_Bay"/>

&emsp;&emsp;\<mapZone other="Eastern Standard Time" territory="TC" type="America/Grand\_Turk"/>

&emsp;&emsp;\<mapZone other="Eastern Standard Time" territory="US" type="America/New\_York America/Detroit America/Indiana/Petersburg America/Indiana/Vincennes America/Indiana/Winamac America/Kentucky/Monticello America/Louisville"/>

Con: A global default will be appeared twice. Also, the semantics of type attribute differs from the use in supplemental/metaZones.xml.

### Version Information

Olson tz database is updated frequently. Also, Windows time zone data is updated at least twice every year (two major scheduled update in August and December). When one of them is updated, the mapping data may need to be updated as well. Therefore, it is important to record what version of data was used for the mapping data. Olson tz database uses year and revision letter withing a year as version string, such as "2011n". Windows also has version information in the registry - HKEY\_LOCAL\_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Time Zones\TzVersion. The value is represented by DWORD and the current value is 0x07dc0000 (It looks high 16-bit represents calendar year: 0x07dc = 2012, and low 16-bit seems to represent revision info within a year).

~~This proposal adds two version attributes in the container element \<windowsZones>~~

~~\<!ELEMENT windowsZones (mapTimezones?) >~~

~~\<!ATTLIST windowsZones tzver NMTOKEN #REQUIRED >~~

~~\<!ATTLIST windowsZones wintzver NMTOKEN #REQUIRED >~~

~~The tzver attribute uses the Olson tz database version string. The wintzver attribute uses fixed 8-digit hexadecimal numeric string. For example,~~

&emsp;~~\<windowsZones tzver="2011n" wintzver="07dc0000">~~

&emsp;&emsp;~~\<mapTimezones>~~

&emsp;&emsp;&emsp;~~....~~

[Update] In the CLDR TC call on 2011-01-11, the CLDR TC members agreed to add time zone data version information in \<mapTimezones> element. It should be also applied to supplemental/metaZones.xml. Also, they suggested to use attribute names - "typeVersion" corresponding to type attribute, "otherVersion" corresponding to other attribute in \<mapZone> element. So the proposal is updated as below -

This proposal adds two version attributes in the container element \<windowsZones>

\<!ELEMENT mapTimezones ( mapZone\* ) >

\<!ATTLIST mapTimezones type NMTOKEN #IMPLIED >

\<!ATTLIST mapTimezones references CDATA #IMPLIED >

\<!ATTLIST mapTimezones typeVersion CDATA #IMPLIED >

\<!ATTLIST mapTimezones otherVersion CDATA #IMPLIED >

The tzver attribute uses the Olson tz database version string. The wintzver attribute uses fixed 8-digit hexadecimal numeric string. For example,

&emsp;\<windowsZones>

&emsp;&emsp;\<mapTimezones typeVersion="2011n" otherVersion="07dc0000">

&emsp;&emsp;&emsp;....

| Olson time zone |  Base UTC offset |
|---|---|
|  Australia/Eucla |  +08:45 |
|  Australia/Lord_Howe |  +10:30 |
|  Etc/GMT-14 |  +14:00 |
|  Pacific/Chatham |  +12:45 |
|  Pacific/Kiritimati |  +14:00 |
|  Pacific/Marquesas |  -09:30 |
|  Pacific/Norfolk |  +11:30 |

### Other Considerations

| Olson time zone |  Comment |
|---|---|
|  America/Adak |  -10:00/DST(US rule). (UTC-10:00) Hawaii is only the Windows zone using -10 hour offset, but no DST is observed. |
|  Etc/GMT+8 |  -08:00/No DST. Windows has two zones with -8 hour offset - (UTC-08:00) Pacific Time (US &amp; Canada) and (UTC-08:00) Baja California - but both of them observe DST. |
|  America/Metlakatla |  |
|  Pacific/Pitcairn |  |
|  Etc/GMT+9 |  -09:00/No DST. (UTC-09:00) Alaska is only the Windows zone using -9 hour offset, but it observes DST. |
|  Pacific/Gambier |  |
|  Pacific/Easter |  -06:00/DST(Southern Hemisphere style rule). Windows has two zones with -6 hour offset and DST enabled - (UTC-06:00) Central Time (US &amp; Canada) and (UTC-06:00) Guadalajara, Mexico City - but both of them uses Northern Hemisphere style DST rules. |
|  America/Miquelon |  -03:00/DST(Canada rule). (UTC-03:00) Greenland is the closest match (-3 hour offset, EU style DST rule). However, Canada rule used by Miquelon differs from EU rule used by Greenland by 2 or 3 weeks on DST start, 1 week on DST end. |

Unmappable Windows Time Zone (Mid-Atlantic Standard Time)

Windows time zone (UTC-02:00) Mid-Atlantic (ID: Mid-Atlantic Standard Time) uses UTC offset of -2 hour with daylight saving time (start: last Sunday in March / end: last Sunday in September). In Olson tz database, there are three zones using -2 hour offset (America/Noronha, Atlantic/South\_Georgia and Etc/GMT+2), but any of them observe daylight saving time. In the CLDR 2.0, we used Etc/GMT+2 as the mapping just because it was only the zone with -2 hour offset. Recently, Windows added (UTC-02:00) Coordinated Universal Time-02 (ID: UTC-02). Because we want to make the mapping from Olson to Windows unique, Olson Etc/GMT+2 should be only used for the new Windows zone UTC-02. So, this proposal remove the mapping data for Windows Mid-Atlantic Standard Time. I do not know why Microsoft keeps this zone. I assume no one really need this zone.

Unmappable Olson Time Zones

There are two kinds of unmappable Olson time zones. The first category is a set of Olson time zones that do not have the same base offset in Windows repertoires. The following zones belong to this category for now.

Because the mapping such zone to a Windows zone is harmful, these Olson time zones are excluded in the mapping data.

The second category is a set of Olson time zone that do not have any Windows time zones with equivalent (or similar) daylight saving time rule. The following zones belong to this category.

Olson time zone with no DST above (such as Etc/GMT+8) could be mapped to a Windows time zone using the same base offset, because Windows allow users to turn off daylight saving time in the control panel or via Windows API. However, it still requires extra operation, so we do not include these mappings. Significant difference in daylight saving time rules will result mapped zone to return incorrect time zone offset (for both direction), we also exclude these DST incompatible zones from the mapping data.

Region of Non-location Olson Time Zones

Olson tz database contains canonical time zones not associated with a specific region. Etc/\* zones and POSIX compatibility zones (such as EST, EST5EDT, PST8PDT...) fall into this category. In this proposal, "ZZ" is used as the region for these non-location time zones. For example,

&emsp;&emsp;\<!-- (UTC-12:00) International Date Line West-->

&emsp;&emsp;\<mapZone other="Dateline Standard Time" *territory="ZZ"* type="Etc/GMT+12"/>

[Alternative] "001" (World) might be another candidate, but I prefer "ZZ" over "001", because 1. such zone is applicable to only a piece of world, 2. "ZZ" represents the semantics of "unknown" explicitly.

Equivalent Windows Time Zones

Windows' time zone grouping is sometimes hard to understand. For example, (UTC+01:00) Belgrade, Bratislava, Budapest, Ljubljana, Prague, (UTC+01:00) Sarajevo, Skopje, Warsaw, Zagreb, (UTC+01:00) Brussels, Copenhagen, Madrid, Paris and (UTC+01:00) Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna shares the same rule (offset +1 hour / EU DST rule). To define mapping between these and Olson time zone require some heuristic decisions. This proposal uses the following policies to determine the mappings.

1. Windows exemplar location name. For example, Europe/Paris is mapped to (UTC+01:00) Brussels, Copenhagen, Madrid, Paris.
2. Olson time zone for a territory near by or related to another Olson time zone are grouped. For example, Europe/Madrid is mapped to (UTC+01:00) Brussels, Copenhagen, Madrid, Paris by the policy above. Europe/Ceuta is a zone associated with region ES, which is same with Europe/Madrid (and using the exact same rule in recent years), Europe/Ceuta is also mapped to (UTC+01:00) Brussels, Copenhagen, Madrid, Paris
3. When a mapping is not determined by above rules, use the most stable and populous Windows time zone as the mapping. For example, among the group of Central European time zones, we pick (UTC+01:00) Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna as the default and use this for other European zones such as Europe/Luxembourg.

The last policy is also used to decide mappings for Etc/\* zones when multiple Windows time zones with the same offset/with no daylight saving time are available.

Deprecated Windows Time Zones

Microsoft dose not delete existing time zone registry key even it's no longer used. Therefore, if you look time zone registry (HKEY\_LOCAL\_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Time Zones) on Windows XP, you'll probably find some zones not included in Windows 7. In the past, we tried to keep these zones in the mapping data. However, this proposal drops them because we want to provide unique Olson -> Windows mapping. In Windows, these deprecated time zones have a flag in the registry. For example, HKEY\_LOCAL\_MACHINE\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Time Zones\Kamchatka Standard Time has a DWORD value - IsObsolete = 0x00000001. Windows 7 does not show such zone in the time zone selection list in the control panel (I thought such mechanism was not available in Windows XP, but I'm not sure).

Maintenance

The mapping data involves some heuristic decisions. Also, in general, Windows time zone is not well maintained for minor regions comparing to the Olson tz database. There are some architectural limitations in Windows time zone implementation. For these reasons, maintaining the mapping data is not definitely a trivial effort. However, once we populate the mapping data, and developing a series of tools validating the mapping data from various aspects, I found that the incremental update would not be really a huge task. There is a program importing Windows time zone registry to create a TimeZone object in ICU. With this program, you can compare a Windows time zone with a Olson time zone side by side. Also, I developed a simple tool and collected exemplar locations from Windows time zone display name, as well as mapping data from a Windows exemplar location to a matching Olson time zone. With such tools, we could automate the mapping data validation. When some changes are necessary, we still need some heuristic decisions, but I believe such task is not really a big task. For now, these codes reside in ICU repository, but we could move them to CLDR later.

ICU Time Zone Data

We currently generate an ICU resource file from supplemental/windowsZones.xml. The mapping data in the current form (1-to-1 map) is used by ICU4C to detect the default system time zone on Windows platform. This implementation has been there for several releases. When a new Olson time zone data version is published, ICU team ships updated data to ICU users, including the mapping data generated from windowsZones.xml. We want to use the same resource for past ICU releases, we cannot change the current ICU resource format. Therefore, LDML2ICUConverter must filter non-default mappings once windowsZones.xml is updated. For future ICU use, LDML2ICUConverter may generate two tables, one in the current format, another for additional mappings.

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
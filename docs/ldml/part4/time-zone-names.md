## <a name="Time_Zone_Names" id="Time_Zone_Names" href="#Time_Zone_Names">Time Zone Names</a>

```dtd
<!ELEMENT timeZoneNames (alias | (hourFormat*, gmtFormat*, gmtUnknownFormat*, regionFormat*, fallbackFormat*, zone*, metazone*, special*)) >

<!ELEMENT hourFormat ( #PCDATA ) >
<!ELEMENT gmtFormat ( #PCDATA ) >
<!ELEMENT gmtUnknownFormat ( #PCDATA ) >

<!ELEMENT regionFormat ( #PCDATA ) >
<!ATTLIST regionFormat type ( standard | daylight ) #IMPLIED >

<!ELEMENT fallbackFormat ( #PCDATA ) >

<!ELEMENT zone (alias | ( long*, short*, exemplarCity*, special*)) >
<!ATTLIST zone type CDATA #REQUIRED >

<!ELEMENT metazone (alias | ( long*, short*, special*)) >
<!ATTLIST metazone type CDATA #REQUIRED >

<!ELEMENT long (alias | (generic*, standard*, daylight*, special*)) >
<!ELEMENT short (alias | (generic*, standard*, daylight*, special*)) >

<!ELEMENT generic ( #PCDATA ) >
<!ELEMENT standard ( #PCDATA ) >
<!ELEMENT daylight ( #PCDATA ) >

<!ELEMENT exemplarCity ( #PCDATA ) >
```

* The time zone IDs (TZID) are language-independent, and follow the _TZ time zone database_ [[Olson](tr35.md#Olson)] and naming conventions. However, the display names for those IDs can vary by locale. The generic time is so-called _wall-time_; what clocks use when they are correctly switched from standard to daylight time at the mandated time of the year.


* Unfortunately, the canonical TZIDs (those in zone.tab) are not stable: they may change in each release of the _TZ_ Time Zone database. In CLDR, however, stability of identifiers is very important. So the canonical IDs in CLDR are kept stable as described in [Canonical Form](tr35.md#Canonical_Form).


* The _TZ time zone database_ can have multiple IDs that refer to the same entity. It does contain information on equivalence relationships between these IDs, such as "Asia/Calcutta" and "Asia/Kolkata". It does not remove IDs (with a few known exceptions), but it may change the "canonical" ID which is in the file zone.tab.


* For lookup purposes specifications such as CLDR need a stable canonical ID, one that does not change from release to release. The stable ID is maintained as the first alias item _type_ element in the file bcp47/timezone.xml, such as:


    `<type name="inccu" alias="Asia/Calcutta Asia/Kolkata"/>`

That file also contains the short ID used in keywords. In versions of CLDR previous to 1.8, the alias information (but not the short ID) was in Supplemental Data under the zoneItem, such as:

    `<zoneItem type="Asia/Calcutta" territory="IN" aliases="Asia/Kolkata"/>`

This element was deprecated after the introduction of bcp47/timezone.xml, because the information became redundant (or was contained in the _TZ time zone database_).

* The following is an example of time zone data. Although this is an example of possible data, in most cases only the exemplarCity needs translation. And that does not even need to be present, if a country only has a single time one. As always, the _type_ field for each zone is the identification of that zone. It is not to be translated.


```xml
<zone type="America/Los_Angeles">
    <long>
        <generic>Pacific Time</generic>
        <standard>Pacific Standard Time</standard>
        <daylight>Pacific Daylight Time</daylight>
    </long>
    <short>
        <generic>PT</generic>
        <standard>PST</standard>
        <daylight>PDT</daylight>
    </short>
    <exemplarCity>San Francisco</exemplarCity>
</zone>

<zone type="Europe/London">
     <long>
        <generic>British Time</generic>
        <standard>British Standard Time</standard>
        <daylight>British Daylight Time</daylight>
    </long>
    <exemplarCity>York</exemplarCity>
</zone>
```

In a few cases, some time zone IDs do not designate a city, as in:

```xml
<zone type="America/Puerto_Rico">
    ...
</zone>

<zone type="America/Guyana">
    ...
</zone>

<zone type="America/Cayman">
    ...
</zone>

<zone type="America/St_Vincent">
    ...
</zone>
```

They may designate countries or territories; their actual capital city may be a name that is too common, or too uncommon. CLDR time zone IDs follow the [Olson](tr35.md#Olson) naming conventions.

> **Note:** CLDR does not allow "GMT", "UT", or "UTC" as translations (short or long) of time zones other than UTC/GMT itself.

> **Note:** Transmitting "14:30" with no other context is incomplete unless it contains information about the time zone. Ideally one would transmit neutral-format date/time information, commonly in UTC (GMT), and localize as close to the user as possible. (For more about UTC, see [[UTCInfo](tr35.md#UTCInfo)].)

* The conversion from local time into UTC depends on the particular time zone rules, which will vary by location. The standard data used for converting local time (sometimes called _wall time_) to UTC and back is the _TZ Data_ [[Olson](tr35.md#Olson)], used by Linux, UNIX, Java, ICU, and others. The data includes rules for matching the laws for time changes in different countries. For example, for the US it is:


> "During the period commencing at 2 o'clock antemeridian on the second Sunday of March of each year and ending at 2 o'clock antemeridian on the first Sunday of November of each year, the standard time of each zone established by sections 261 to 264 of this title, as modified by of this title, shall be advanced one hour..." (United States Law - 15 U.S.C. §6(IX)(260-7), as amended by Energy Policy Act of 2005).

* Each region that has a different time zone or daylight savings time rules, either now or at any time back to 1970, is given a unique internal ID, such as `Europe/Paris` . (Some IDs are also distinguished on the basis of differences before 1970.) As with currency codes, these are internal codes. A localized string associated with these is provided for users (such as in the Windows _Control Panels>Date/Time>Time Zone_).


* Unfortunately, laws change over time, and will continue to change in the future, both for the boundaries of time zone regions and the rules for daylight savings. Thus the _TZ_ data is continually being augmented. Any two implementations using the same version of the _TZ_ data will get the same results for the same IDs (assuming a correct implementation). However, if implementations use different versions of the data they may get different results. So if precise results are required then both the _TZ_ ID and the _TZ_ data version must be transmitted between the different implementations.


For more information, see [[Data Formats](tr35.md#DataFormats)].

The following subelements of `<timeZoneNames>` are used to control the fallback process described in [Using Time Zone Names](#Using_Time_Zone_Names).

###### <a name="timeZoneNames_Elements_Used_for_Fallback" id="timeZoneNames_Elements_Used_for_Fallback" href="#timeZoneNames_Elements_Used_for_Fallback">Table: timeZoneNames Elements Used for Fallback</a>

<table>
<tbody>
<tr><th>Element Name</th><th>Data Examples</th><th>Results/Comment</th></tr>
<tr><td rowspan="2">hourFormat</td><td rowspan="2">"+HHmm;-HHmm"</td><td>"+1200"</td></tr>
    <tr><td>"-1200"</td></tr>
<tr><td rowspan="2">gmtFormat</td><td>"GMT{0}"</td><td>"GMT-0800"</td></tr>
    <tr><td>"{0}ВпГ"</td><td>"-0800ВпГ"</td></tr>
<tr><td>gmtUnknownFormat</td><td>"GMT"</td><td>Specifies how GMT/UTC with an unknown offset should be represented.</td></tr>
<tr><td rowspan="2">regionFormat</td><td>"{0} Time"</td><td>"Japan Time"</td></tr>
    <tr><td>"Hora de {0}"</td><td>"Hora de Japón"</td></tr>
<tr><td rowspan="2">regionFormat type="daylight"<br>(or "standard")</td><td>"{0} Daylight Time"</td><td>"France Daylight Time"</td></tr><tr><td>"horario de verano de {0}"</td><td>"horario de verano de Francia"</td></tr>
    <tr><td>fallbackFormat</td><td>"{1} ({0})"</td><td>"Pacific Time (Canada)"</td></tr>
</tbody>
</table>

* When referring to the abbreviated (short) form of the time zone name, there are often situations where the location-based (city or country) time zone designation for a particular language may not be in common usage in a particular territory.


> **Note:** User interfaces for time zone selection can use the "generic location format" for time zone names to obtain the most useful ordering of names in a menu or list; see _[Using Time Zone Names](#Using_Time_Zone_Names)_ and the zone section of the _[Date Field Symbol Table](#Date_Field_Symbol_Table)._

### <a name="Metazone_Names" id="Metazone_Names" href="#Metazone_Names">Metazone Names</a>

* A metazone is a grouping of one or more internal TZIDs that share a common display name in current customary usage, or that have shared a common display name during some particular time period. For example, the zones _Europe/Paris, Europe/Andorra, Europe/Tirane, Europe/Vienna, Europe/Sarajevo, Europe/Brussels, Europe/Zurich, Europe/Prague, Europe/Berlin_, and so on are often simply designated _Central European Time_ (or translated equivalent).


* A metazone's display fields become a secondary fallback if an appropriate data field cannot be found in the explicit time zone data. The _usesMetazone_ field indicates that the target metazone is active for a particular time. This also provides a mechanism to effectively deal with situations where the time zone in use has changed for some reason. For example, consider the TZID "America/Indiana/Knox", which observed Central time (GMT-6:00) prior to October 27, 1991, and has currently observed Central time since April 2, 2006, but has observed Eastern time (GMT-5:00) between these two dates. This is denoted as follows


```xml
<timezone type="America/Indiana/Knox">
  <usesMetazone to="1991-10-27 07:00" mzone="America_Central" />
  <usesMetazone to="2006-04-02 07:00" from="1991-10-27 07:00" mzone="America_Eastern" />
  <usesMetazone from="2006-04-02 07:00" mzone="America_Central" />
</timezone>
```

Note that the dates and times are specified in UTC, not local time.

* **usesMetazone_ can also**: _usesMetazone_ can also optionally specify which offset is considered standard time, and which offset is considered daylight time. This is required for some zone such as `Europe/Dublin` where TZDB returns inconsistent results depending on platform/build mode etc.:


```xml
<timezone type="Europe/Dublin">
    <usesMetazone mzone="GMT" from="1971-10-31 02:00" stdOffset="+00" dstOffset="+01"/>
</timezone>
```

The metazones can then have translations in different locale files, such as the following.

```xml
<metazone type="America_Central">
    <long>
        <generic>Central Time</generic>
        <standard>Central Standard Time</standard>
        <daylight>Central Daylight Time</daylight>
    </long>
    <short>
        <generic>CT</generic>
        <standard>CST</standard>
        <daylight>CDT</daylight>
    </short>
    </metazone>
    <metazone type="America_Eastern">
        <long>
        <generic>Eastern Time</generic>
        <standard>Eastern Standard Time</standard>
        <daylight>Eastern Daylight Time</daylight>
    </long>
    <short>
        <generic>ET</generic>
        <standard>EST</standard>
        <daylight>EDT</daylight>
    </short>
</metazone>

<metazone type="America_Eastern">
    <long>
        <generic>Heure de l’Est</generic>
        <standard>Heure normale de l’Est</standard>
        <daylight>Heure avancée de l’Est</daylight>
    </long>
    <short>
        <generic>HE</generic>
        <standard>HNE</standard>
        <daylight>HAE</daylight>
    </short>
</metazone>
```

* When formatting a date and time value using this data, an application can properly be able to display "Eastern Time" for dates between 1991-10-27 and 2006-04-02, but display "Central Time" for current dates. (See also _[Dates and Date Ranges](tr35.md#Date_Ranges)_.)


* Metazones are used with the 'z', 'zzzz', 'v', and 'vvvv' date time pattern characters, and not with the 'Z', 'ZZZZ', 'VVVV' and other pattern characters for time zone formatting. For more information, see [Date Format Patterns](#Date_Format_Patterns).


Note that several of the CLDR metazone IDs are the same as TZID aliases provided by the _TZ time zone database_ and also included in ICU data. For example:
* “Japan” is a CLDR metazone ID (which has short ID “japa”), but also an alias to the TZID “Asia/Tokyo” (which has BCP 47 ID “jptyo”).
* “GMT” is a CLDR metazone ID (which has short ID “mgmt”), but also an alias to the TZID “Etc/GMT” (which has BCP 47 ID “gmt”).
In practice this is not an issue, since metazone IDs and TZIDs are never used in the same way in any data structure, or in the same APIs in a library such as ICU.

* The `commonlyUsed` element is now deprecated. The CLDR committee has found it nearly impossible to obtain accurate and reliable data regarding which time zone abbreviations may be understood in a given territory, and therefore has changed to a simpler approach. Thus, if the short metazone form is available in a given locale, it is to be used for formatting regardless of the value of commonlyUsed. If a given short metazone form is known NOT to be understood in a given locale and the parent locale has this value such that it would normally be inherited, the inheritance of this value can be explicitly disabled by use of the 'no inheritance marker' as the value, which is 3 simultaneous empty set characters (U+2205).



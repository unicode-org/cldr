## <a name="Using_Time_Zone_Names" id="Using_Time_Zone_Names" href="#Using_Time_Zone_Names">Using Time Zone Names</a>

* There are three main types of formats for zone identifiers: GMT, generic (wall time), and standard/daylight. Standard and daylight are equivalent to a particular offset from GMT, and can be represented by a GMT offset as a fallback. In general, this is not true for the generic format, which is used for picking timezones or for conveying a timezone for specifying a recurring time (such as a meeting in a calendar). For either purpose, a GMT offset would lose information.


### <a name="Time_Zone_Format_Terminology" id="Time_Zone_Format_Terminology" href="#Time_Zone_Format_Terminology">Time Zone Format Terminology</a>

The following terminology defines more precisely the formats that are used.

**Generic non-location format:** Reflects "wall time" (what is on a clock on the wall): used for recurring events, meetings, or anywhere people do not want to be overly specific. For example, "10 am Pacific Time" will be GMT-8 in the winter, and GMT-7 in the summer.

* "Pacific Time" (long)
* "PT" (short)

**Generic partial location format:** Reflects "wall time": used as a fallback format when the generic non-location format is not specific enough.

* "Pacific Time (Canada)" (long)
* "PT (Whitehorse)" (short)

**Generic location format:** Reflects "wall time": a primary function of this format type is to represent a time zone in a list or menu for user selection of time zone. It is also a fallback format when there is no translation for the generic non-location format. Times can also be organized hierarchically by country for easier lookup.

+ France Time
+ Italy Time
+ Japan Time
+ United States
  + Chicago Time
  + Denver Time
  + Los Angeles Time
  + New York Time
+ United Kingdom Time

* Note: A generic location format is constructed by a part of time zone ID representing an exemplar city name or its country as the final fallback. However, there are Unicode time zones which are not associated with any locations, such as "Etc/GMT+5" and "PST8PDT". Although the date format pattern "VVVV" specifies the generic location format, but it displays localized GMT format for these. Some of these time zones observe daylight saving time, so the result (localized GMT format) may change depending on input date. For generating a list for user selection of time zone with format "VVVV", these non-location zones should be excluded.


**Specific non-location format:** Reflects a specific standard or daylight time, which may or may not be the wall time. For example, "10 am Pacific Standard Time" will be GMT-8 in the winter and in the summer.

* "Pacific Standard Time" (long)
* "PST" (short)
* "Pacific Daylight Time" (long)
* "PDT" (short)

**Localized GMT format:** A constant, specific offset from GMT (or UTC), which may be in a translated form. There are two styles for this:

* The first is used when there is an explicit offset from GMT; this style is specified by the `<gmtFormat>` element and `<hourFormat>` element. The long format always uses 2-digit hours field and minutes field, with optional 2-digit seconds field. The short format is intended for the shortest representation and uses hour fields without leading zero, with optional 2-digit minutes and seconds fields. The digits used for hours, minutes and seconds fields in this format are the locale's default decimal digits:


* "GMT+03:30" (long)
* "GMT+3:30" (short)
* "UTC-03.00" (long)
* "UTC-3" (short)
* "Гринуич+03:30" (long)
* "GMT+00:00" (long)
* "UTC+0" (short)

The second is used when the offset from GMT is unknown. It is specified by the `<gmtUnknownFormat>` element:

* "GMT+?"
* "UTC+?"
* "Гринуич+?"

**ISO 8601 time zone formats:** The formats based on the [[ISO 8601](tr35.md#ISO8601)]  local time difference from UTC ("+" sign is used when local time offset is 0), or the UTC indicator ("Z" - only when the local time offset is 0 and the specifier X\* is used). The ISO 8601 basic format does not use a separator character between hours and minutes field, while the extended format uses colon (':') as the separator. The ISO 8601 basic format with hours and minutes fields is equivalent to RFC 822 zone format.

* "-0800" (basic)
* "-08" (basic - short)
* "-08:00" (extended)
* "Z" (UTC)

> Note: This specification extends the original ISO 8601 formats and some format specifiers append seconds field when necessary.

**Raw Offset** - an offset from GMT that does not include any daylight savings behavior. For example, the raw offset for Pacific Time is -8, even though the _observed offset_ may be -8 or -7.

**Metazone** - a collection of time zones that share the same behavior and same name during some period. They may differ in daylight behavior (whether they have it and when).

For example, the TZID America/Cambridge_Bay is in the following metazones during various periods:

```xml
<timezone type="America/Cambridge_Bay">
    <usesMetazone to="1999-10-31 08:00" mzone="America_Mountain" />
    <usesMetazone to="2000-10-29 07:00" from="1999-10-31 08:00" mzone="America_Central" />
    <usesMetazone to="2000-11-05 05:00" from="2000-10-29 07:00" mzone="America_Eastern" />
    <usesMetazone to="2001-04-01 09:00" from="2000-11-05 05:00" mzone="America_Central" />
    <usesMetazone from="2001-04-01 09:00" mzone="America_Mountain" />
</timezone>
```

Zones may join or leave a metazone over time. The data relating between zones and metazones is in the supplemental information; the locale data is restricted to translations of metazones and zones.

> **Invariants:**
>
> * At any given point in time, each zone belongs to no more than one metazone.
> * At a given point in time, a zone may not belong to any metazones.
> * _Except for daylight savings_, at any given time, all zones in a metazone have the same offset at that time.

**Golden Zone** - the TZDB zone that exemplifies a metazone. For example, America/New_York is the golden zone for the metazone America_Eastern:

```xml
<mapZone other="America_Eastern" territory="001" type="America/New_York"/>
```

> **Invariants:**
>
> * The golden zones are those in mapZone supplemental data under the territory "001".
> * Every metazone has exactly one golden zone.
> * Each zone has at most one metazone for which it is golden.
> * The golden zone is in that metazone during the entire life of the metazone. (The raw offset of the golden zone may change over time.)
> * Each other zone must have the same raw offset as the golden zone, for the entire period that it is in the metazone. (It might not have the same offset when daylight savings is in effect.)
> * A golden zone in mapTimezones must have reverse mapping in metazoneInfo.
> * A single time zone can be a golden zone of multiple different metazones if any two of them are never active at a same time.

**Preferred Zone** - for a given TZID, the "best" zone out of a metazone for a given country or language.

> **Invariants:**
>
> * The preferred zone for a given country XX are those in mapZone supplemental data under the territory XX.
> * Every metazone has at most one preferred zone for a given territory XX.
> * Each zone has at most one metazone for which it is preferred for a territory XX.
> * The preferred zone for a given metazone and territory XX is in a metazone M during any time when any other zone in XX is also in M
> * A preferred zone in mapTimezones must have reverse mapping in metazoneInfo

* For example, for America_Pacific the preferred zone for Canada is America/Vancouver, and the preferred zone for Mexico is America/Tijuana. The golden zone is America/Los_Angeles, which is also the preferred zone for any other country.


```xml
<mapZone other="America_Pacific" territory="001" type="America/Los_Angeles" />
<mapZone other="America_Pacific" territory="CA" type="America/Vancouver" />
<mapZone other="America_Pacific" territory="MX" type="America/Tijuana" />
```

**<a name="fallbackFormat" href="#fallbackFormat">fallbackFormat:</a>** a formatting string such as "{1} ({0})", where {1} is the metazone, and {0} is the country or city.

**regionFormat:** a formatting string such as "{0} Time", where {0} is the country or city.

### <a name="Time_Zone_Goals" id="Time_Zone_Goals" href="#Time_Zone_Goals">Goals</a>

The timezones are designed so that:

> For any given locale, every _time_ round trips with all patterns (but not necessarily every timezone). That is, given a time and a format pattern with a zone string, you can format, then parse, and get back the same time.
>
> Note that the round-tripping is not just important for parsing; it provides for formatting dates and times in an unambiguous way for users. It is also important for testing.
>
> There are exceptions to the above for transition times.
>
> * With generic format, time zone ID or exemplar city name, during the transition when the local time maps to two possible GMT times.
>   * For example, Java works as follows, favoring standard time:
>   * Source: Sun Nov 04 01:30:00 PDT 2007
>   * => Formatted: "Sunday, November 4, 2007 1:30:00 AM"
>   * => Parsed: Sun Nov 04 01:30:00 PST 2007
> * When the timezone changes offset, say from GMT+4 to GMT+5, there can also be a gap.
>
> The V/VV/VVV/VVVV format will roundtrip not only the time, but the canonical timezone.

When the data for a given format is not available, a fallback format is used. The fallback order is given in the following by a list.

1. **Specifics**
   * z - [short form] specific non-location
     * falling back to short localized GMT
   * zzzz - [long form] specific non-location
     * falling back to long localized GMT
   * Z/ZZZZZ/X+/x+ - ISO 8601 formats (no fallback necessary)
   * ZZZZ/O+ - Localized GMT formats (no fallback necessary)

2. **Generics**
   * v - [short form] generic non-location
     _(however, the rules are more complicated, see #5 below)_
     * falling back to generic location
     * falling back to short localized GMT
   * vvvv - [long form] generic non-location
     _(however, the rules are more complicated, see #5 below)_
     * falling back to generic location
     * falling back to long localized GMT
   * V - short time zone ID
     * falling back to the special ID "unk" (Unknown)
   * VV - long time zone ID (no fallback necessary, because this is the input)
   * VVV - exemplar city
     * falling back to the localized exemplar city for the unknown zone (Etc/Unknown), for example "Unknown Location" for English
   * VVVV - generic location
     * falling back to long localized GMT

The following process is used for the particular formats, with the fallback rules as above.

* Some of the examples are drawn from real data, while others are for illustration. For illustration the region format is "Hora de {0}". The fallback format in the examples is "{1} ({0})", which is what is in root.


1. In **all** cases, first canonicalize the _TZ_ ID according to the Unicode Locale Extension _type_ mapping data (see [Time Zone Identifiers](tr35.md#Time_Zone_Identifiers) for more details). Use that canonical TZID in each of the following steps.
    1. If the canonicalization fails (i.e. `Etc/Unknown` is returned), skip non-location and location formats and fall back to localized offset format
        * America/Atka → America/Adak
        * Australia/ACT → Australia/Sydney
        * Australia/Ulladulla -> Etc/Unknown // format as localized offset

2. For the localized GMT format, use the gmtFormat (such as "GMT{0}" or "HMG{0}") with the hourFormat (such as "+HH:mm;-HH:mm" or "+HH.mm;-HH.mm").
   * America/Los_Angeles → "GMT-08:00" // standard time
   * America/Los_Angeles → "HMG-07:00" // daylight time
   * Etc/GMT+3 → "GMT-03.00" // note that _TZ_ TZIDs have inverse polarity!
   * Etc/Unknown → "GMT+07:00" // if the offset is known
   * Etc/Unknown → "GMT+?" // if the offset is not known

    **Note:** The digits should be whatever are appropriate for the locale used to format the time zone, not necessarily from the western digits, 0..9. For example, they might be from ०..९.

3.  For ISO 8601 time zone format return the results according to the ISO 8601 specification.
    * America/Los_Angeles →
      * "-08" ("X","x")
      * "-0800" ("Z","XX","XXXX","xx","xxxx")
      * "-08:00" ("ZZZZZ","XXX","XXXXX","xxx","xxxxx")
    * Etc/GMT →
      * "Z" ("ZZZZZ", "X", "XX", "XXX", "XXXX", "XXXXX")
      * "+00" ("x")
      * "+0000" ("Z", "xx", "xxxx")
      * "+00:00" ("xxx", "xxxxx")

    **Note:** The digits in this case are always from the western digits, 0..9.

4. For the non-location formats (generic or specific):
   1. if there is an explicit translation for the TZID in `<timeZoneNames>` according to type (generic, standard, or daylight) in the resolved locale, return it.
      1. If the requested type is not available, but another type is, and there is a **Type Fallback** then return that other type.
         * Examples:
           * America/Los_Angeles → // generic
           * America/Los_Angeles → "アメリカ太平洋標準時" // standard
           * America/Los_Angeles → "Yhdysvaltain Tyynenmeren kesäaika" // daylight
           * Europe/Dublin  → "Am Samhraidh na hÉireann" // daylight
           * Note: This translation may not at all be literal: it would be what is most recognizable for people using the target language.
   2. Otherwise, get the requested metazone format according to type (generic, standard, daylight).
      1. If the requested type is not available, but another type is, get the format according to **Type Fallback**.
      2. If there is no format for the type, fall back.
   3. Otherwise do the following:
      1. Get the country for the current locale. If there is none, use the most likely country based on the likelySubtags data. If there is none, use “001”.
      2. Get the preferred zone for the metazone for the country; if there is none for the country, use the preferred zone for the metazone for “001”.
      3. If that preferred zone is the same as the requested zone, use the metazone format. For example, "Pacific Time" for Vancouver if the locale is en_CA, or for Los Angeles if locale is en_US.
      4. Otherwise, if the zone is the preferred zone for its country but not for the country of the locale, use the metazone format + country in the _fallbackFormat_.
      5. Otherwise, use the metazone format + city in the _fallbackFormat_.
         * Examples:
           * "Pacific Time (Canada)" // for the zone Vancouver in the locale en_MX.
           * "Mountain Time (Phoenix)"
           * "Pacific Time (Whitehorse)"
5. For the location formats (generic or specific):
   1. Get the _regionFormat_ format according to type (generic, standard, or daylight).
   2. Determine whether there is only one timezone in the region associated with the timezone (see [Time Zone Identifiers](tr35.md#Time_Zone_Identifiers)).
      1. If there is only one timezone or if the zone id is in the `<primaryZones>` list, continue with short country name, if it exists, otherwise the country name.
      2. Otherwise, continue with the localized name of the exemplar city for the zone.
   3. Format the region format obtained in step 1 with the location obtained in step 2.
      * Examples:
        * America/Buenos_Aires, generic → "Buenos Aires Time" // multiple zones in AR
        * Asia/Shanghai, standard → "China Standard Time" // Asia/Shanghai is the _primaryZone_ for CN
        * Europe/Rome, daylight → "Italy Summer Time" // Europe/Rome is the only zone in IT
        * Africa/Monrovia, generic → "Hora de Liberja"
        * America/Havana, generic → "Hora de CU" // if CU is not localized

> **Note:** If a language does require grammatical changes when composing strings, then the _regionFormat_ should either use a neutral format such as "Heure: {0}", or put all exceptional cases in explicitly translated strings.

**Type Fallback**

When a specified type (generic, standard, daylight) does not exist:

1. If the daylight type does not exist, then the metazone doesn’t require daylight support. For all three types:
   1. If the generic type exists, use it.
   2. Otherwise if the standard type exists, use it.
2. Otherwise if the generic type is needed, but not available, and the offset and daylight offset do not change within 184 day +/- interval around the exact formatted time, use the standard type.
   1. Example: "Mountain Standard Time" for Phoenix
   2. Note: 184 is the smallest number that is at least 6 months AND the smallest number that is more than 1/2 year (Gregorian).

**Composition**

In composing the metazone + city or country:

1. Use the _fallbackFormat_ "{1} ({0})", where:
   * {1} will be the metazone
   * {0} will be a qualifier (city or country)
   * Example:
     * metazone = Pacific Time
     * city = Phoenix
     * → "Pacific Time (Phoenix)"
2. If the localized country name is not available, use the code:
   * CU (country code) → "CU" _// no localized country name for Cuba_
3. If the localized exemplar city is not available, use as the exemplar city the last field of the raw TZID, stripping off the prefix and turning _ into space.
   * America/Los_Angeles → "Los Angeles" _// no localized exemplar city_

**Note:** As with the _regionFormat_, exceptional cases need to be explicitly translated.

### <a name="Time_Zone_Parsing" id="Time_Zone_Parsing" href="#Time_Zone_Parsing">Parsing</a>

In parsing, an implementation will be able to either determine the zone id, or a simple offset from GMT for anything formatting according to the above process.

The following is a sample process for how this might be done. It is only a sample; implementations may use different methods for parsing.

* The sample describes the parsing of a zone as if it were an isolated string. In implementations, the zone may be mixed in with other data (like the time), so the parsing actually has to look for the longest match, and then allow the remaining text to be parsed for other content. That requires certain adaptions to the following process.


1. Start with a string S.
2. If S matches ISO 8601 time zone format, return it.
   * For example, "-0800" (RFC 822), "-08:00" (ISO 8601) => Etc/GMT+8
3. If S matches the English or localized GMT format, return the corresponding TZID
* *** Matching should**: * Matching should be lenient. Thus allow for the number formats like: 03, 3, 330, 3:30, 33045 or 3:30:45. Allow +, -, or nothing. Allow spaces after GMT, +/-, and before number. Allow non-Latin numbers. Allow UTC or UT (per RFC 788) as synonyms for GMT ("GMT", "UT", "UTC" are global formats, always allowed in parsing regardless of locale).

   * For example, "GMT+3" or "UT+3" or "HPG+3" => Etc/GMT-3
   * When parsing, the absence of a numeric offset should be interpreted as offset 0, whether in localized or global formats. For example, "GMT" or "UT" or "UTC+0" or "HPG" => Etc/GMT
4. If S matches the fallback format, extract P = {0} [ie, the part in parens in the root format] and N = {1}.
   If S does not match, set P = "" and N = S.
   If N matches the region format, then M = {0} from that format, otherwise M = N.
   * For example, "United States (Los Angeles) Time" => N = "United States Time", M = "United States", P = "Los Angeles".
   * For example, "United States Time" => N = "United States Time", M = "United States", P = "".
   * For example, "United States" => N = M = "United States", P = "".
5. If P, N, or M is a localized country, set C to that value. If C has only one zone, return it.
   * For example, "Italy Time (xxx)" or "xxx (Italy)" => Europe/Rome
   * For example, "xxx (Canada)" or "Canada Time (xxx)" => Sets C = CA and continues
6. If P is a localized exemplar city name (and not metazone), return it.
   * For example, "xxxx (Phoenix)" or "Phoenix (xxx)" => America/Phoenix
7. If N, or M is a localized time zone name (and not metazone), return it.
   * For example, "Pacific Standard Time (xxx)" => "America/Los_Angeles" // this is only if "Pacific Standard Time" is not a metazone localization.
8. If N or M is a localized metazone
   * If it corresponds to only one TZID, return it.
   * If C is set, look up the Metazone + Country => TZID mapping, and return that value if it exists
   * Get the locale's language, and get the default country from that. Look up the Metazone + DefaultCountry => TZID mapping, and return that value if it exists.
   * Otherwise, look up Metazone + 001 => TZID and return it (that will always exist)
9. If you get this far, return an error.

> **Note:** This CLDR date parsing recommendation does not fully handle all RFC 788 date/time formats, nor is it intended to.

* Parsing can be more lenient than the above, allowing for different spacing, punctuation, or other variation. A stricter parse would check for consistency between the xxx portions above and the rest, so "Pacific Standard Time (India)" would give an error.


Using this process, a correct parse will roundtrip the location format (VVVV) back to the canonical zoneid.

  * Australia/ACT → Australia/Sydney → “Sydney (Australia)” → Australia/Sydney

The GMT formats (Z and ZZZZ) will return back an offset, and thus lose the original canonical zone id.

  * Australia/ACT → Australia/Sydney → "GMT+11:00" → GMT+11

* The daylight and standard time formats, and the non-location formats (z, zzzz, v, and vvvv) may either roundtrip back to the original canonical zone id, to a zone in the same metazone that time, or to just an offset, depending on the available translation data. Thus:


  * Australia/ACT → Australia/Sydney → "GMT+11:00" → GMT+11
  * PST8PDT → America/Los_Angeles → “PST” → America/Los_Angeles
  * America/Vancouver → “Pacific Time (Canada)” → America/Vancouver


---
title: Time Zones and City names
---

# Time Zones and City names

## Time Zone Names

Time zones (such as "Pacific Time" or "France Time") can be formatted in different ways:

- "metazone" names, such as "Pacific Time" or "Pacific Standard Time", that can apply to segments of a country or groups of countries.
- location (country or city) based names, such as "Paris Time" or "Japan Standard Time"
    - These are constructed using patterns (see below), plus [Country/Region Names](https://cldr.unicode.org/translation/displaynames/countryregion-territory-names) and City Names.
    - When a "metazone" name is absent, a location based name is used as a fallback.
- raw offsets, like "GMT+10:00"
    - These are constructed using patterns (see below).

**Tips:**

- When translating time zone names, include the Region information if the name can be ambiguous. For example, “Central time” would be ambiguous without the America\_Central region context.
- When the English name can be ambiguous in your language, use the name that's most commonly used. For example, the English America\_Mountain "Mountain time" can be ambiguous and including "Rocky" may be most commonly understood in your language; thus the translation would be for "Rocky Mountain time".

For each of the first two, there are three choices:

- Winter (standard) time, such as "Atlantic Standard Time"
- Summer (daylight) time, such as "Atlantic Daylight Time"
- Generic time, such as "Atlantic Time". This is used for recurring times (such as in a Calendar program) that change between summer and winter.

## Time Zone Patterns

The following special patterns are used in formatting timezones.

| Name | English Pattern | English_Example | Meaning |  |
|---|---|---|---|---|
| gmtFormat | GMT{0}<br /> *or*<br /> GMT{ HOURS_FROM_GMT } | GMT -2:00 | **GMT Pattern.** Modify this field if the format for GMT time uses different letters, such as  HUA+0200  for  GMT+02:00 , or if the letters GMT occur after the time. Make sure you include the  {0} ; that is where the actual time value will go! |  |
| gmtZeroFormat | GMT | GMT | **GMT Zero Pattern.**  *This field must be consistent with the GMT Pattern.* |  |
| hourFormat | +HH:mm;-HH:mm | GMT -02:00 | **GMT Hours Pattern.** This field controls the format for the time used with the GMT Pattern. It contains two patterns separated by a ";". The first controls positive time values (and zero), and the second controls the negative values. So to get  *GMT+02.00* for positive values, and  *GMT-02.00* for negative values, you'd use *+HH.mm;-HH.mm*. |  |
| regionFormat | {0} Time<br /> *or*<br /> { COUNTRY } Time / { CITY } Time | Bolivia Time | **Location-Based Time Zone Pattern.** For generic references to time zones, the country is used if possible, composed with a pattern that in English appears as "{0}  **Time** ". Thus a time zone may appear as "Malaysia  **Time** " or " **Hora de**  Malasia". If the country has multiple time zones, then a city is used to distinguish which one, thus "Argentina (La Rioja)  **Time** ".<br /><br /> Some languages would normally have grammatical adjustments depending on what the name of the city is. For example, one might need "12:43 pm  **Tempo d'** Australia" but "12:43 pm  **Tempo de**   Paris". In that case, there are two approaches:<br /><br /> 1. Use "{0}", which will give results like "12:43 pm Australia" and "12:43 pm Paris", or<br /> 2. Use a "form-style" phrasing such as " **Tempo de:**  {0}", which will give results like "12:43 pm  **Tempo de:**  Australia" and "12:43 pm **Tempo de:**  Paris". |  |
| regionFormat-standard | {0} Standard Time<br /> *or*<br /> { COUNTRY } Standard Time / { CITY } Standard Time | Bolivia Standard Time |  |   |
| regionFormat-daylight | {0} Daylight Time<br /> *or*<br /> { COUNTRY } Daylight Time / { CITY } Daylight Time | Bolivia Daylight Time |  |   |
| fallbackFormat | {1} ({0})<br /> *or*<br /> { METAZONE_NAME } ({ CITY }) | Central Time  ( Cancun ) | **Metazone Name with Location Pattern.** This field is usually not translated. This field to control the formatting of ambiguous metazone name name. When a set of metazone's generic names are shared by multiple different zones and GMT offset at the given time in a zone is different from other zones using the same metazone, this format pattern is used to distinguish the zone from others. In the pattern, {0} will be replaced by location name (either country or city) and {1} will be replaced with the metazone's name.      |  |

## City Names

Please choose the most neutral grammatical form of the city name. The city name will typically be used to indicate a timezone, either in a menu, or in formatting a time.

In a few cases, what is included in the list of cities for translation is actually a country name, such as the following. In those cases, use the name of the country instead.

- Costa Rica
- Cape Verde
- Faeroe (for the Faroe Islands)

**Usage**

A city may be used in a menu of timezone names, such as:

- ...
- United States Time (Los Angeles)
- United States Time (New York)
- United Kingdom Time (London)
- ...

Timezones may also have a simpler format, depending on the language, such as:

- ...
- United States (Los Angeles)
- United States (New York)
- United Kingdom (London)
- ...

The city name may also be used in formatted times, such as:

- 12:51 AM France Time (Paris)

**Unique Names**

City names must be unique. See [Country/Region Names](https://cldr.unicode.org/translation/displaynames/countryregion-territory-names) for techniques.

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
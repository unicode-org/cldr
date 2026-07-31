## <a name="Supplemental_Calendar_Data" id="Supplemental_Calendar_Data" href="#Supplemental_Calendar_Data">Supplemental Calendar Data</a>

### <a name="Calendar_Data" id="Calendar_Data" href="#Calendar_Data">Calendar Data</a>

```dtd
<!ELEMENT calendarData ( calendar* )>
<!ELEMENT calendar ( calendarSystem?, inheritEras?, eras? )>
<!ATTLIST calendar type NMTOKENS #REQUIRED>
<!ATTLIST calendar territories NMTOKENS #IMPLIED > <!-- deprecated, replaced by calendarPreferenceData -->

<!ELEMENT calendarSystem EMPTY>
<!ATTLIST calendarSystem type (solar | lunar | lunisolar | other) #REQUIRED>

<!ELEMENT inheritEras EMPTY >
<!ATTLIST inheritEras calendar NMTOKEN #REQUIRED >

<!ELEMENT eras ( era* )>

<!ELEMENT era EMPTY>
<!ATTLIST era type NMTOKENS #REQUIRED>
<!ATTLIST era start CDATA #IMPLIED>
<!ATTLIST era end CDATA #IMPLIED>
<!ATTLIST era code NMTOKEN #IMPLIED >
<!ATTLIST era aliases NMTOKENS #IMPLIED >
```

The `<calendarData>` element provides locale-independent data about calendar behaviors via its `<calendar>` subelements,
which for each calendar can specify the astronomical basis of the calendar (solar, lunar, etc.) and the date ranges for its eras.
For example:

```xml
<calendar type="gregorian">
  <calendarSystem type="solar" />
  <eras>
    <era type="0" end="0-12-31" code="bce" aliases="bc"/> <!-- Before Common Era, Before Christ -->
    <era type="1" start="1-01-01" code="ce" aliases="ad"/> <!-- Common Era, Anno Domini -->
  </eras>
</calendar>
```

If a `<calendar>` contains an `<inheritEras/>` element, all eras from the specified calendar should be inserted in order into the sequence of eras for the current calendar, as described below.
For example, the following means that the two eras from calendar "gregorian" should be inserted into the era list for "japanese" for calculations and formatting.

```xml
<calendar type="japanese">
  <inheritEras calendar="gregorian" />
  <eras>
    <era type="232" start="1868-10-23" code="meiji"/>
    <era type="233" start="1912-07-30" code="taisho"/>
    <era type="234" start="1926-12-25" code="showa"/>
    <era type="235" start="1989-01-08" code="heisei"/>
    <era type="236" start="2019-05-01" code="reiwa"/>
  </eras>
</calendar>
```

Each `era` element has a `code` attribute and optional `aliases` attributes that define stable strings for identifying the eras. These are more mnemonic than the `type` identifiers (see below).
The `code` defines the primary identifier for the era, and `aliases` are space-separated additional identifiers. 
Each identifier consists of a sequence of subtags consisting of ASCII letters and digits ([a-zA-Z0-9]) separated by ASCII hyphens. 
Each identifier is also limited to be at most 8 characters long, thus `abcdefg` and `abc-defg` would be well-formed, but `abcdefghi` and `abc-defgh` would not be.

The `start` date is specified in terms of the equivalent _proleptic_ Gregorian date in the format "yyyy-MM-dd", such as 1842-01-01.
An omitted start date behaves as if start=-∞.

The order for the eras is given by the following algorithm:
- Include all eras from the inheritEras calendar, if there is one.
- An omitted start date behaves as if start=-∞
- All elements are ordered by their start dates.
- No two elements can have the same start date (otherwise the data is invalid).

Note that the order of the eras is _not_ necessarily the order in the XML file, nor is it based on the numeric value of the `type`s.

For a given _proleptic_ Gregorian date D and calendar C, the era code for D is in the `era` element in C with the greatest start date ≤ the given date.
It is also the _first_ `era` element with start date ≤ the given date in C, given the above ordering for `era` elements.

The `type` has an integer value.
The type values do not have to start at 0, nor do they need to be in chronological order.
They are used to access the era names in locale files.
For example:

```xml
<era type="232">Meiji</era>
<era type="233">Taishō</era>
<era type="234">Shōwa</era>
<era type="235">Heisei</era>
<era type="236">Reiwa</era>
```

The `end` attribute is unused, and is slated for deprecation in the future.

**Note:** The `territories` attribute in the `calendar` element is deprecated. It was formerly used to indicate calendar preference by territory, but this is now given by the _[Calendar Preference Data](#Calendar_Preference_Data)_ below.

### <a name="Calendar_Preference_Data" id="Calendar_Preference_Data" href="#Calendar_Preference_Data">Calendar Preference Data</a>

```dtd
<!ELEMENT calendarPreferenceData ( calendarPreference* ) >
<!ELEMENT calendarPreference EMPTY >
<!ATTLIST calendarPreference territories NMTOKENS #REQUIRED >
<!ATTLIST calendarPreference ordering NMTOKENS #REQUIRED >
```

* The `calendarPreference` element provides a list of commonly used calendar types in a territory. The `ordering` attribute indicates the list of calendar types in preferred order. The first calendar type in the list is the default calendar type for the territory. For example:


```xml
<calendarPreference territories="001" ordering="gregorian"/>
<calendarPreference territories="JP" ordering="gregorian japanese"/>
<calendarPreference territories="TH" ordering="buddhist gregorian"/>
```

The `calendarPreference` elements above indicate:

* The default (for territory "001") is that only the Gregorian calendar is commonly used.
* For Japan, the Gregorian and Japanese calendars are both used, with Gregorian preferred (the default).
* For Thailand, the Buddhist and Gregorian calendars are both used, and Buddhist is preferred (the default).

The calendars in common use for a locale should typically be shown in UIs that provide a choice of calendars. (An 'Other...' button could give access to the other available calendars.)

### <a name="Week_Data" id="Week_Data" href="#Week_Data">Week Data</a>

```dtd
<!ELEMENT weekData ( minDays*, firstDay*, weekendStart*, weekendEnd*, weekOfPreference* )>

<!ELEMENT minDays EMPTY>
<!ATTLIST minDays count (1 | 2 | 3 | 4 | 5 | 6 | 7) #REQUIRED>
<!ATTLIST minDays territories NMTOKENS #REQUIRED>

<!ELEMENT firstDay EMPTY >
<!ATTLIST firstDay day (sun | mon | tue | wed | thu | fri | sat) #REQUIRED>
<!ATTLIST firstDay territories NMTOKENS #REQUIRED>

<!ELEMENT weekendStart EMPTY>
<!ATTLIST weekendStart day (sun | mon | tue | wed | thu | fri | sat) #REQUIRED>
<!ATTLIST weekendStart territories NMTOKENS #REQUIRED>

<!ELEMENT weekendEnd EMPTY>
<!ATTLIST weekendEnd day (sun | mon | tue | wed | thu | fri | sat) #REQUIRED>
<!ATTLIST weekendEnd territories NMTOKENS #REQUIRED>

<!ELEMENT weekOfPreference EMPTY>
<!ATTLIST weekOfPreference locales NMTOKENS #REQUIRED>
<!ATTLIST weekOfPreference ordering NMTOKENS #REQUIRED>
```

* These values provide territory-specific information needed for week-of-year and week-of-month calculations, as well as information on conventions for first day of the week, for weekends, and for week designations. For most elements, the default is provided by the element with `territories="001"`; for `weekOfPreference` elements the default is provided by the element with `locales="und"`.


```xml
<weekData>
    <minDays count="1" territories="001" />
    <minDays count="4" territories="AD AN AT AX BE BG CH CZ DE DK EE ES FI FJ FO FR GB …" />
    <firstDay day="mon" territories="001" />
    <firstDay day="fri" territories="BD MV" />
    <firstDay day="sat" territories="AE AF BH DJ DZ EG IQ IR JO …" />
    …
    <weekendStart day="sat" territories="001" />
    <weekendStart day="sun" territories="IN" />
    <weekendStart day="thu" territories="AF DZ IR OM SA YE" />
    <weekendStart day="fri" territories="AE BH EG IL IQ JO KW …" />
    …
    <weekOfPreference ordering="weekOfYear" locales="und" />
    <weekOfPreference ordering="weekOfYear weekOfMonth" locales="am az bs cs cy da el et hi ky lt mk sk ta th" />
    <weekOfPreference ordering="weekOfYear weekOfMonth weekOfInterval" locales="is mn no sv vi" />
    <weekOfPreference ordering="weekOfYear weekOfDate weekOfMonth" locales="fi zh-TW" />
    …
```

* In order for a week to count as the first week of a new year for week-of-year calculations, the week beginning with `firstDay` must include at least the number of days in the new year specified by the `minDays` value; otherwise the week will count as the last week of the previous year (and for week-of-month calculations, `minDays` also specifies the minimum number of days in the new month for a week to count as part of that month).


> **Note:** For week-of-year calculations, Gregorian years may have 52 or 53 weeks. Changes in the value of `minDays` or `firstDay` can affect the year to which a date is assigned as well as the number of weeks in a given year; implementations that parse dates using week-of-year formats should be prepared to handle such cases. For example when parsing a date in week 53 of a year for which current values of `minDays` and `firstDay` no longer result in a 53-week year, that date should be treated as in the first week of the following year.

* The day indicated by `firstDay` is the one that should be shown as the first day of the week in a calendar view. This is not necessarily the same as the first day after the weekend (or the first work day of the week), which should be determined from the weekend information. Currently, day-of-week numbering is based on `firstDay` (that is, day 1 is the day specified by `firstDay`), but in the future we may add a way to specify this separately. The `firstDay` value determined from the region can be overridden by the locale keyword "fw", see [Unicode First Day Identifier](tr35.md#UnicodeFirstDayIdentifier).


* What is meant by the weekend varies from country to country. It is typically when most non-retail businesses are closed. The time should not be specified unless it is a well-recognized part of the day. The `weekendStart` day defaults to "sat", and `weekendEnd` day defaults to "sun". For more information, see _[Dates and Date Ranges](tr35.md#Date_Ranges)_.


* Each `weekOfPreference` element provides, for its specified locales, an ordered list of the preferred types of week designations for that set of locales. There are four types of week designations, each of which makes use of date patterns available in the locale, as follows:


###### <a name="Week_Designation_Types" id="Week_Designation_Types" href="#Week_Designation_Types">Table: Week Designation Types</a>

| Type           | Examples                          | Date Pattern                                                | Comments    |
|----------------|-----------------------------------|-------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| weekOfYear     | week 15 of 2016                   | \`<dateFormatItem id='yw' count='one'\>`'week' w 'of' Y\<…       | The **week of** construction takes a count attribute, just in case the pattern changes depending on the numeric value of the **w** value. (In the future, we're likely to add an ordinal value, for constructions like “3rd week of March”.) In languages where the month name needs grammatical changes (aside from just the simple addition of a prefix or suffix), localizers will typically use a work-around construction. |
| weekOfMonth    | week 2 of April 2nd week of April | \`<dateFormatItem id='MMMMW'' count='one'\>`'week' W 'of' MMM\<… |   (same comment as above) |
| weekOfDate     | the week of April 11, 2016        | \`<field type="week"\>`\`<relativePeriod>`the week of {0}\<…        | The date pattern that replaces {0} is determined separately and may use the first day or workday of the week, the range of the full week or work week, etc.   |
| weekOfInterval | the week of April 11–15           | \`<field type="week"\>`\`<relativePeriod>`the week of {0}\<…    |  (same comment as above) |

#### <a name="First_Day_Overrides" id="First_Day_Overrides" href="#First_Day_Overrides">First Day Overrides</a>

* The calculation of the first day of the week depends on various fields in a locale_identifier, according to the following algorithm. The data in the `firstDay` elements is treated as a map from region to day, with any missing value using the value for 001.


1. If there is a valid `-u-fw-` day value, return that day.
2. Else if there is a valid `-u-rg-` region value, return that region's firstDay map value.
3. Else if there is a valid `-u-ca-` calendar value, where that calendar specifies the first day, then return that first day. (Most calendars do not specify the first day.)
4. Else if there is an explicit region subtag, then return that region's firstDay map value.
5. Else if there is a valid `-u-sd-` subdivision value, return that region's firstDay map value.
6. Else if the [Add Likely Subtags](tr35.md#Likely_Subtags) algorithm produces a region, return that region's firstDay map value.
7. Else return the firstDay map value for 001.

*Example:*

| Locale Identifier | "Winning" subtags | Region |
|----|----|----|
|en-AU-u-ca-iso8601-fw-tue-rg-afzzzz-sd-cabc | -fw-tue | n/a, uses Tuesday |
|en-AU-u-ca-iso8601-rg-afzzzz-sd-cabc | -rg-afzzzz | AF |
|en-AU-u-ca-iso8601-sd-cabc | -ca-iso8601 | n/a, uses Monday |
|en-AU-u-sd-cabc | -AU | AU |
|en-u-sd-cabc | -sd-cabc | CA |
|en | | US (from likely subtags) |
|zxx | 001 | (fallback) |

### <a name="Time_Data" id="Time_Data" href="#Time_Data">Time Data</a>

```dtd
<!ELEMENT timeData ( hours* ) >
<!ELEMENT hours EMPTY >
<!ATTLIST hours preferred NMTOKEN #REQUIRED >
<!ATTLIST hours allowed NMTOKENS #REQUIRED >
<!ATTLIST hours regions NMTOKENS #REQUIRED >
```

* This element is for data that indicates, for various regions, the preferred time cycle in the region, as well as all time cycles that are considered acceptable in the region. The defaults are those specified for region 001.


* There is a single `preferred` value, and multiple `allowed` values. The meanings of the values H, h, K, k, b and B are defined in [Date Field Symbol Table](#Date_Field_Symbol_Table). The `allowed` values are in preference order; they are used with the 'C' hour skeleton pattern symbol and the `c12` and `c24` values for the [Unicode Hour Cycle Identifier](tr35.md#UnicodeHourCycleIdentifier).


For example, in the following, RU (Russia) is marked as using only 24 hour time, and in particular the 24 hour time that goes from 0..23 (H), rather than from 1..24 (k).

Also note that the `regions` allows either region codes (`001`, `JP`) or
locale IDs (`gu_IN`).

```xml
<timeData>
    <hours preferred="H" allowed="H h" regions="001 …" />
    <hours preferred="H" allowed="H K h" regions="JP" />
    <hours preferred="H" allowed="H" regions="IL RU" />
    <hours preferred="h" allowed="H h" regions="AE AG AL … US … ZW" />
    <hours preferred="h" allowed="hB h hb H" regions="ta_IN"/>
    <hours preferred="h" allowed="hB hb h H" regions="TW ET gu_IN mr_IN pa_IN"/>
    …
```

* The B and b date symbols provide for formats like “3:00 at night”. When the ‘C’ option is used, the values in `allowed` are traversed from first to last, picking the first available format. For example, in the following a system that supports hB should choose that as the most preferred format for the C (not the `preferred` value H).


```xml
<hours preferred="H" allowed="hB H" regions="CD" />
<hours preferred="H" allowed="hB hb h H" regions="KE MM TZ UG" />
```

* Some systems may not want to use B and b, even if preferred for the locale, so for compatibility the `preferred` value is limited to {H, h, K, k}, and is the option selected by the ‘j’ date symbol. Thus the `preferred` value may not be the same as the first `allowed` value.


The preferred value for the locale can be overridden by the locale keyword "hc", see [Unicode Hour Cycle Identifier ](tr35.md#UnicodeHourCycleIdentifier).

### <a name="Day_Period_Rule_Sets" id="Day_Period_Rule_Sets" href="#Day_Period_Rule_Sets">Day Period Rule Sets</a>

```dtd
<!ELEMENT dayPeriodRuleSet ( dayPeriodRules* ) >
<!ATTLIST dayPeriodRuleSet type NMTOKEN #IMPLIED >

<!ELEMENT dayPeriodRules (dayPeriodRule*) >
<!ATTLIST dayPeriodRules locales NMTOKENS #REQUIRED >

<!ELEMENT dayPeriodRule EMPTY >
<!ATTLIST dayPeriodRule type NMTOKEN #REQUIRED >
<!ATTLIST dayPeriodRule at NMTOKEN #IMPLIED >
<!ATTLIST dayPeriodRule from NMTOKEN #IMPLIED >
<!ATTLIST dayPeriodRule before NMTOKEN #IMPLIED >
```

* Each locale can have a set of day period rules, which determine the periods during a day for use in time formats like "10:00 at night", or to select statements like "Your email arrived last night." If locales do not have dayPeriodRules, the computation of dayPeriods falls back to AM/PM.


There are two kinds of dayPeriodRuleSets, based on the type:

* **The **_format_** type**: The **_format_** type is used in conjunction with times, such as to express "3:00 in the afternoon", or "12:00 noon". Many languages do not normally use terms that match AM/PM for such times, instead breaking up the day into more periods.


The **stand-alone** type is used for selecting a period of the day for a general time associated with an event. For example, it can be used to select a message like:

```xml
<msg ... >
{day_period, select,
MORNING1 {Your email arrived yesterday morning.}
AFTERNOON1 {Your email arrived yesterday afternoon.}
EVENING1 {Your email arrived yesterday evening.}
NIGHT1 {Your email arrived last night.}
other {Your email arrived yesterday.}
...
}
</msg>
```

The translated values for the selection (**stand-alone**) day periods are intended for use in designating a time of day, without an hour value.

These are relative times within a single day. If the event can occur on multiple days, then that needs to be handled at a higher level.

* As with plurals, the exact set of periods used for any language may be different. It is the responsibility of any translation software to pick the relevant day periods for the locale for display to the translator (and end user).


#### <a name="Day_Period_Rules" id="Day_Period_Rules" href="#Day_Period_Rules">Day Period Rules</a>

Here are the requirements for a rule set.

#### <a name="Fixed_periods" id="Fixed_periods" href="#Fixed_periods">Fixed periods</a>

* There are 4 dayPeriods that are fixed; am/pm are always defined, and always have the same meaning and definition for every locale. Midnight and noon are optional, however if they are defined, they have the same meaning and definition as in all other locales where they are defined.


```xml
<dayPeriodRule type="midnight" at="00:00" />
<dayPeriodRule type="am" from="00:00" before="12:00" />
<dayPeriodRule type="noon" at="12:00" />
<dayPeriodRule type="pm" from="12:00" before="24:00" />
```

Note that midnight and am can overlap, as can noon and pm.

* All locales must support am/pm, but not all support **noon** or **midnight**; they are only supported if they meet the above definitions. For example, German has no unique term that means exactly 12:00 noon; the closest is Mittag, but that can extend before or after 12 noon.


**Midnight** is also special, since it can refer to either 00:00 or 24:00 — either at the start or end of the day. That means that Tuesday 24:00 = Wednesday 00:00. “Midnight Tuesday" is thus ambiguous: it means 24:00 in “the party is Tuesday from 10pm to 12 midnight”, while it means 00:00 in “I was awake from 12 midnight to 3 in the morning”.

* It is strongly recommended that implementations provide for the ability to specify whether **midnight** is supported or not (and for either 00:00 or 24:00 or both), since only the caller knows enough of the context to determine what to use. In the absence of such information, 24:00 may be the best choice.


#### <a name="Variable_periods" id="Variable_periods" href="#Variable_periods">Variable periods</a>

1. If a locale has a set of dayPeriodRules for variable periods, it needs to completely cover the 24 hours in a day (from 0:00 before 24:00), with **no** overlaps between any dayPeriodRules. They may overlap with the **Fixed Periods**.
   If it does not have a rule set for variable periods, behavior should fall back to using the fixed periods (am, pm).
2. "from" is a closed interval (inclusive). _(as is the deprecated "to")_
3. "before" is an open interval (exclusive). _(as is the deprecated "after")_
4. "at" means starting time and end time are the same. _("at" is deprecated except when used for the fixed periods)_
5. There must be exactly one of {at, from, after} and exactly one of {at, to, before} for each dayPeriodRule.
6. Use of non-zero minutes or seconds is deprecated.
7. The dayPeriodRules for format must allow that hh:mm [period name] and hh [period name] can be parsed uniquely to HH:mm [period name].
   * For example, you can't have `<dayPeriod type = "morning1" from="00:00" to="13:00"/>` because "12:30 {morning}" would be ambiguous.
8. There must not be two rules with the same type. A day period rule may, however, span 24:00 / 00:00. Example:
   * _Valid:_
     * `<dayPeriod type = "night1" from="21:00" to="05:00"/>`
   * _Invalid:_
     * `<dayPeriod type = "night1" from="00:00" to="05:00"/>`
     * `<dayPeriod type = "night1" from="21:00" to="24:00"/>`
9. 24:00 is _only_ allowed in _before_="24:00".

#### <a name="Parsing_Day_Periods" id="Parsing_Day_Periods" href="#Parsing_Day_Periods">Parsing Day Periods</a>

* When parsing, if the hour is present with a strict parse the dayperiod is checked for consistency with the hour. If there is no hour, the center of the first matching dayPeriodRule can be chosen (starting from 0:00). However, if there is other information available when parsing, a different point within the interval may be chosen.


* The dayPeriodRule may span two days, such as where **night1** is [21:00, 06:00). In that case, the midpoint is 01:30, so when parsing “Nov 12, at night”, the midpoint result would be Nov 12, 01:30. “Nov 12, am”, “Nov 12, pm”, “Nov 12, noon” can be parsed similarly, resulting in Nov 12, 06:00; Nov 12, 18:00; and Nov 12, 12:00; respectively.


* “Nov 12, midnight” is special, because midnight may mean either 00:00 or 24:00. Extra information may be needed to disambiguate which is meant, such as whether the time is at the start or end of an interval. In the absence of such information, 24:00 may be the best choice. See the discussion of **midnight** above.


If rounding is done—including the rounding done by the time format—then it needs to be done before the dayperiod is computed, so that the correct format is shown.

For examples, see [Day Periods Chart](https://www.unicode.org/cldr/charts/latest/supplemental/day_periods.html).


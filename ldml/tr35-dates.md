## Unicode Technical Standard #35

# Unicode Locale Data Markup Language (LDML)<br/>Part 4: Dates

|Version|43 (draft)        |
|-------|------------------|
|Editors|Peter Edberg and <a href="tr35.md#Acknowledgments">other CLDR committee members</a>|

For the full header, summary, and status, see [Part 1: Core](tr35.md).

### _Summary_

This document describes parts of an XML format (_vocabulary_) for the exchange of structured locale data. This format is used in the [Unicode Common Locale Data Repository](https://www.unicode.org/cldr/).

This is a partial document, describing only those parts of the LDML that are relevant for date, time, and time zone formatting. For the other parts of the LDML see the [main LDML document](tr35.md) and the links above.

### _Status_

_This is a draft document which may be updated, replaced, or superseded by other documents at any time.
Publication does not imply endorsement by the Unicode Consortium.
This is not a stable document; it is inappropriate to cite this document as other than a work in progress._

<!-- _This document has been reviewed by Unicode members and other interested parties, and has been approved for publication by the Unicode Consortium.
This is a stable document and may be used as reference material or cited as a normative reference by other specifications._ -->

> _**A Unicode Technical Standard (UTS)** is an independent specification. Conformance to the Unicode Standard does not imply conformance to any UTS._

_Please submit corrigenda and other comments with the CLDR bug reporting form [[Bugs](tr35.md#Bugs)]. Related information that is useful in understanding this document is found in the [References](tr35.md#References). For the latest version of the Unicode Standard see [[Unicode](tr35.md#Unicode)]. For a list of current Unicode Technical Reports see [[Reports](tr35.md#Reports)]. For more information about versions of the Unicode Standard, see [[Versions](tr35.md#Versions)]._

## <a name="Parts" href="#Parts">Parts</a>

The LDML specification is divided into the following parts:

*   Part 1: [Core](tr35.md#Contents) (languages, locales, basic structure)
*   Part 2: [General](tr35-general.md#Contents) (display names & transforms, etc.)
*   Part 3: [Numbers](tr35-numbers.md#Contents) (number & currency formatting)
*   Part 4: [Dates](tr35-dates.md#Contents) (date, time, time zone formatting)
*   Part 5: [Collation](tr35-collation.md#Contents) (sorting, searching, grouping)
*   Part 6: [Supplemental](tr35-info.md#Contents) (supplemental data)
*   Part 7: [Keyboards](tr35-keyboards.md#Contents) (keyboard mappings)
*   Part 8: [Person Names](tr35-personNames.md#Contents) (person names)

## <a name="Contents" href="#Contents">Contents of Part 4, Dates</a>

* 1 [Overview: Dates Element, Supplemental Date and Calendar Information](#Overview_Dates_Element_Supplemental)
* 2 [Calendar Elements](#Calendar_Elements)
  * 2.1 [Elements months, days, quarters, eras](#months_days_quarters_eras)
  * 2.2 [Elements monthPatterns, cyclicNameSets](#monthPatterns_cyclicNameSets)
  * 2.3 [Element dayPeriods](#dayPeriods)
  * 2.4 [Element dateFormats](#dateFormats)
  * 2.5 [Element timeFormats](#timeFormats)
  * 2.6 [Element dateTimeFormats](#dateTimeFormats)
    * 2.6.1 [Element dateTimeFormat](#dateTimeFormat)
      * Table: [Date-Time Combination Examples](#Date_Time_Combination_Examples)
    * 2.6.2 [Elements availableFormats, appendItems](#availableFormats_appendItems)
      * Table: [Mapping Requested Time Skeletons To Patterns](#Mapping_Requested_Time_Skeletons_To_Patterns)
      * 2.6.2.1 [Matching Skeletons](#Matching_Skeletons)
      * 2.6.2.2 [Missing Skeleton Fields](#Missing_Skeleton_Fields)
    * 2.6.3 [Element intervalFormats](#intervalFormats)
* 3 [Calendar Fields](#Calendar_Fields)
* 4 [Supplemental Calendar Data](#Supplemental_Calendar_Data)
  * 4.1 [Calendar Data](#Calendar_Data)
  * 4.2 [Calendar Preference Data](#Calendar_Preference_Data)
  * 4.3 [Week Data](#Week_Data)
    * Table: [Week Designation Types](#Week_Designation_Types)
  * 4.4 [Time Data](#Time_Data)
  * 4.5 [Day Period Rule Sets](#Day_Period_Rule_Sets)
    * 4.5.1 [Day Period Rules](#Day_Period_Rules)
      * 4.5.1.1 [Fixed periods](#Fixed_periods)
      * 4.5.1.2 [Variable periods](#Variable_periods)
      * 4.5.1.3 [Parsing Day Periods](#Parsing_Day_Periods)
* 5 [Time Zone Names](#Time_Zone_Names)
  * Table: [timeZoneNames Elements Used for Fallback](#timeZoneNames_Elements_Used_for_Fallback)
  * 5.1 [Metazone Names](#Metazone_Names)
* 6 [Supplemental Time Zone Data](#Supplemental_Time_Zone_Data)
  * 6.1 [Metazones](#Metazones)
  * 6.2 [Windows Zones](#Windows_Zones)
  * 6.3 [Primary Zones](#Primary_Zones)
* 7 [Using Time Zone Names](#Using_Time_Zone_Names)
  * 7.1 [Time Zone Format Terminology](#Time_Zone_Format_Terminology)
  * 7.2 [Goals](#Time_Zone_Goals)
  * 7.3 [Parsing](#Time_Zone_Parsing)
* 8 [Date Format Patterns](#Date_Format_Patterns)
  * Table: [Date Format Pattern Examples](#Date_Format_Pattern_Examples)
  * Table: [Date Field Symbol Table](#Date_Field_Symbol_Table)
  * 8.1 [Localized Pattern Characters (deprecated)](#Localized_Pattern_Characters)
  * 8.2 [AM / PM](#Date_Patterns_AM_PM)
  * 8.3 [Eras](#Date_Patterns_Eras)
  * 8.4 [Week of Year](#Date_Patterns_Week_Of_Year)
  * 8.5 [Week Elements](#Date_Patterns_Week_Elements)
* 9 [Parsing Dates and Times](#Parsing_Dates_Times)

## 1 <a name="Overview_Dates_Element_Supplemental" href="#Overview_Dates_Element_Supplemental">Overview: Dates Element, Supplemental Date and Calendar Information</a>

```xml
<!ELEMENT dates (alias | (calendars?, fields?, timeZoneNames?, special*)) >
```

The LDML top-level `<dates>` element contains information regarding the format and parsing of dates and times, the formatting of date/time intervals, and the naming of various calendar elements.

*   The `<calendars>` element is described in section 2 [Calendar Elements](#Calendar_Elements).
*   The `<fields>` element is described in section 3 [Calendar Fields](#Calendar_Fields).
*   The `<timeZoneNames>` element is described in section 5 [Time Zone Names](#Time_Zone_Names).
*   The formats use pattern characters described in section 8 [Date Format Patterns](#Date_Format_Patterns).

```xml
<!ELEMENT supplementalData ( …, calendarData?, calendarPreferenceData?, weekData?, timeData?, …, timezoneData?, …, metazoneInfo?, …, dayPeriodRuleSet*, metaZones?, primaryZones?, windowsZones?, …) >
```

The relevant top-level supplemental elements are listed above.

*   The `<calendarData>`, `<calendarPreferenceData>`, `<weekData>`, `<timeData>`, and `<dayPeriodRuleSet>` elements are described in section 4 [Supplemental Calendar Data](#Supplemental_Calendar_Data).
*   The `<timezoneData>` element is deprecated and no longer used; the `<metazoneInfo>` element is deprecated at this level, and is now only used as a sub-element of `<metaZones>`.
*   The `<metaZones>`, `<primaryZones>`, and `<windowsZones>` elements are described in section 6 [Supplemental Time Zone Data](#Supplemental_Time_Zone_Data).

## 2 <a name="Calendar_Elements" href="#Calendar_Elements">Calendar Elements</a>

```xml
<!ELEMENT calendars (alias | (calendar*, special*)) >
<!ELEMENT calendar (alias | (months?, monthPatterns?, days?, quarters?, dayPeriods?, eras?, cyclicNameSets?, dateFormats?, timeFormats?, dateTimeFormats?, special*))>
<!ATTLIST calendar type NMTOKEN #REQUIRED >
```

The `<calendars>` element contains multiple `<calendar>` elements, each of which specifies the fields used for formatting and parsing dates and times according to the calendar specified by the `type` attribute (e.g. "gregorian", "buddhist", "islamic"). The behaviors for different calendars in a locale may share certain aspects, such as the names for weekdays. They differ in other respects; for example, the Japanese calendar is similar to the Gregorian calendar but has many more eras (one for each Emperor), and years are numbered within each era. All calendar data inherits either from the Gregorian calendar or other calendars in the same locale (and if not present there then from the parent up to root), or else inherits directly from the parent locale for certain calendars, so only data that differs from what would be inherited needs to be supplied. See _[Multiple Inheritance](tr35.md#Multiple_Inheritance)_.

Each calendar provides—directly or indirectly—two general types of data:

*   _Calendar symbols, such as names for eras, months, weekdays, and dayPeriods._ Names for weekdays, quarters and dayPeriods are typically inherited from the Gregorian calendar data in the same locale. Symbols for eras and months should be provided for each calendar, except that the "Gregorian-like" Buddhist, Japanese, and Minguo (ROC) calendars also inherit their month names from the Gregorian data in the same locale.
*   _Format data for dates, times, and date-time intervals._ Non-Gregorian calendars inherit standard time formats (in the `<timeFormats>` element) from the Gregorian calendar in the same locale. Most non-Gregorian calendars (other than Chinese and Dangi) inherit general date format data (in the `<dateFormats>` and `<dateTimeFormats>` elements) from the "generic" calendar format data in the same locale, which in turn inherits from Gregorian.

Calendars that use cyclicNameSets and monthPatterns (such as Chinese and Dangi) have additional symbols and distinct formats, and typically inherit these items (along with month names) from their parent locales, instead of inheriting them from Gregorian or generic data in the same locale.

The primary difference between Gregorian and "generic" format data is that date formats in "generic" usually include era with year, in order to provide an indication of which calendar is being used (Gregorian calendar formats may also commonly include era with year when Gregorian is not the default calendar for the locale). Otherwise, the "generic" date formats should normally be consistent with those in the Gregorian calendar. The "generic" calendar formats are intended to provide a consistent set of default formats for non-Gregorian calendars in the locale, so that in most cases the only data items that need be provided for non-Gregorian calendars are the era names and month names (and the latter only for calendars other than Buddhist, Japanese, and Minguo, since those inherit month names from Gregorian).

### 2.1 <a name="months_days_quarters_eras" href="#months_days_quarters_eras">Elements months, days, quarters, eras</a>

```xml
<!ELEMENT months ( alias | (monthContext*, special*)) >
<!ELEMENT monthContext ( alias | (default*, monthWidth*, special*)) >
<!ATTLIST monthContext type ( format | stand-alone ) #REQUIRED >
<!ELEMENT monthWidth ( alias | (month*, special*)) >
<!ATTLIST monthWidth type ( abbreviated| narrow | wide) #REQUIRED >
<!ELEMENT month ( #PCDATA )* >
<!ATTLIST month type ( 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 13 ) #REQUIRED >
<!ATTLIST month yeartype ( standard | leap ) #IMPLIED >

<!ELEMENT days ( alias | (dayContext*, special*)) >
<!ELEMENT dayContext ( alias | (default*, dayWidth*, special*)) >
<!ATTLIST dayContext type ( format | stand-alone ) #REQUIRED >
<!ELEMENT dayWidth ( alias | (day*, special*)) >
<!ATTLIST dayWidth type NMTOKEN #REQUIRED >
<!ELEMENT day ( #PCDATA ) >
<!ATTLIST day type ( sun | mon | tue | wed | thu | fri | sat ) #REQUIRED >

<!ELEMENT quarters ( alias | (quarterContext*, special*)) >
<!ELEMENT quarterContext ( alias | (default*, quarterWidth*, special*)) >
<!ATTLIST quarterContext type ( format | stand-alone ) #REQUIRED >
<!ELEMENT quarterWidth ( alias | (quarter*, special*)) >
<!ATTLIST quarterWidth type NMTOKEN #REQUIRED >
<!ELEMENT quarter ( #PCDATA ) >
<!ATTLIST quarter type ( 1 | 2 | 3 | 4 ) #REQUIRED >

<!ELEMENT eras (alias | (eraNames?, eraAbbr?, eraNarrow?, special*)) >
<!ELEMENT eraNames ( alias | (era*, special*) ) >
<!ELEMENT eraAbbr ( alias | (era*, special*) ) >
<!ELEMENT eraNarrow ( alias | (era*, special*) ) >
```

The month and quarter names are identified numerically, starting at 1. The weekday names are identified with short strings, since there is no universally-accepted numeric designation.

Month, day, and quarter names may vary along two axes: the width and the context.

The context is either _format_ (the default), the form used within a complete date format string (such as "Saturday, November 12"), or _stand-alone_, the form for date elements used independently, such as in calendar headers. The _stand-alone_ form may be used in any other date format that shares the same form of the name. For month names, this is typically the nominative grammatical form, and might also be used in patterns such as "LLLL y" (month name + year). The _format_ form is an additional form that can be used in contexts where it is different than the stand-alone form. For example, in many languages, patterns that combine month name with day-of-month (and possibly other elements) may require the month name to be in a grammatical form such as genitive or partitive.
* In past versions of CLDR, the distinction between format and stand-alone forms was used to control capitalization (with stand-alone forms using titlecase); however, this can be controlled separately and more precisely using the `<contextTransforms>` element as described in _[ContextTransform Elements](tr35-general.md#Context_Transform_Elements)_, so both format and stand-alone forms should generally use middle-of-sentence capitalization.
* However, if in a given language, certain context/width combinations are always used in a titlecase form — for example, stand-alone narrow forms for months or weekdays — then these should be provided in that form.
* The distinctions between stand-alone (e.g. LLLL) and format (e.g. MMMM) forms are only relevant for how date elements are used within a date format. They are not intended to reflect how a date format is used within a sentence. For example, they are not intended to be used to generate the dative form of a date format when that format is used after a preposition that takes dative form.

The width can be _wide_ (the default), _abbreviated_, or _narrow_; for days only, the width can also be _short,_ which is ideally between the abbreviated and narrow widths, but must be no longer than abbreviated and no shorter than narrow (if short day names are not explicitly specified, abbreviated day names are used instead). Note that for `<monthPattern>`, described in the next section:

*   There is an additional context type _numeric_
*   When the context type is numeric, the width has a special type _all_

The format values must be distinct for the wide, abbreviated, and short widths. However, values for the narrow width in either format or stand-alone contexts, as well as values for other widths in stand-alone contexts, need not be distinct; they might only be distinguished by context. For example, "S" may be used both for Saturday and for Sunday. The narrow width is typically used in calendar headers; it must be the shortest possible width, no more than one character (or grapheme cluster, or exemplar set element) in stand-alone values (not including punctuation), and the shortest possible widths (in terms of grapheme clusters) in format values. The short width (if present) is often the shortest unambiguous form.

Era names should be distinct within each of the widths, including narrow; there is less disambiguating information for them, and they are more likely to be used in a format that requires parsing.

Due to aliases in root, the forms inherit "sideways". (See _[Multiple Inheritance](tr35.md#Multiple_Inheritance)_.) For example, if the abbreviated format data for Gregorian does not exist in a language X (in the chain up to root), then it inherits from the wide format data in that same language X.

```xml
<monthContext type="format">
    <monthWidth type="abbreviated">
        <alias source="locale" path="../monthWidth[@type='wide']"/>
    </monthWidth>
    <monthWidth type="narrow">
        <alias source="locale" path="../../monthContext[@type='stand-alone']/monthWidth[@type='narrow']"/>
    </monthWidth>
    <monthWidth type="wide">
        <month type="1">1</month>
        ...
        <month type="12">12</month>
    </monthWidth>
</monthContext>
<monthContext type="stand-alone">
    <monthWidth type="abbreviated">
        <alias source="locale" path="../../monthContext[@type='format']/monthWidth[@type='abbreviated']"/>
    </monthWidth>
    <monthWidth type="narrow">
        <month type="1">1</month>
        ...
        <month type="12">12</month>
    </monthWidth>
    <monthWidth type="wide">
        <alias source="locale" path="../../monthContext[@type='format']/monthWidth[@type='wide']"/>
    </monthWidth>
</monthContext>
```

The `yeartype` attribute for months is used to distinguish alternate month names that would be displayed for certain calendars during leap years. The practical example of this usage occurs in the Hebrew calendar, where the 7th month "Adar" occurs in non-leap years, with the 6th month being skipped, but in leap years there are two months named "Adar I" and "Adar II". There are currently only two defined year types, standard (the implied default) and leap.

For `era` elements, an additional `alt="variant"` form may be supplied. This is primarily intended for use in the "gregorian" calendar, with which two parallel sets of era designations are used in some locales: one set with a religious reference (e.g. English BC/AD), and one set without (e.g. English BCE/CE). The most commonly-used set for the locale should be provided as the default, and the other set may be provided as the `alt="variant"` forms. See the example below.

Example:

```xml
<calendar type="gregorian">
    <months>
        <monthContext type="format">
            <monthWidth type="wide">
                <month type="1">January</month>
                <month type="2">February</month>
                ...
                <month type="11">November</month>
                <month type="12">December</month>
            </monthWidth>
            <monthWidth type="abbreviated">
                <month type="1">Jan</month>
                <month type="2">Feb</month>
                ...
                <month type="11">Nov</month>
                <month type="12">Dec</month>
            </monthWidth>
        </monthContext>
        <monthContext type="stand-alone">
            <default type="wide"/>
            <monthWidth type="wide">
                <month type="1">Januaria</month>
                <month type="2">Februaria</month>
                ...
                <month type="11">Novembria</month>
                <month type="12">Decembria</month>
            </monthWidth>
            <monthWidth type="narrow">
                <month type="1">J</month>
                <month type="2">F</month>
                ...
                <month type="11">N</month>
                <month type="12">D</month>
            </monthWidth>
        </monthContext>
    </months>

    <days>
        <dayContext type="format">
            <dayWidth type="wide">
                <day type="sun">Sunday</day>
                <day type="mon">Monday</day>
                ...
                <day type="fri">Friday</day>
                <day type="sat">Saturday</day>
            </dayWidth>
            <dayWidth type="abbreviated">
                <day type="sun">Sun</day>
                <day type="mon">Mon</day>
                ...
                <day type="fri">Fri</day>
                <day type="sat">Sat</day>
            </dayWidth>
            <dayWidth type="narrow">
                <day type="sun">Su</day>
                <day type="mon">M</day>
                ...
                <day type="fri">F</day>
                <day type="sat">Sa</day>
            </dayWidth>
        </dayContext>
        <dayContext type="stand-alone">
            <dayWidth type="narrow">
                <day type="sun">S</day>
                <day type="mon">M</day>
                ...
                <day type="fri">F</day>
                <day type="sat">S</day>
            </dayWidth>
        </dayContext>
    </days>

    <quarters>
        <quarterContext type="format">
            <quarterWidth type="abbreviated">
                <quarter type="1">Q1</quarter>
                <quarter type="2">Q2</quarter>
                <quarter type="3">Q3</quarter>
                <quarter type="4">Q4</quarter>
            </quarterWidth>
            <quarterWidth type="wide">
                <quarter type="1">1st quarter</quarter>
                <quarter type="2">2nd quarter</quarter>
                <quarter type="3">3rd quarter</quarter>
                <quarter type="4">4th quarter</quarter>
            </quarterWidth>
        </quarterContext>
    </quarters>

    <eras>
        <eraAbbr>
            <era type="0">BC</era>
            <era type="0" alt="variant">BCE</era>
            <era type="1">AD</era>
            <era type="1" alt="variant">CE</era>
        </eraAbbr>
        <eraNames>
            <era type="0">Before Christ</era>
            <era type="0" alt="variant">Before Common Era</era>
            <era type="1">Anno Domini</era>
            <era type="1" alt="variant">Common Era</era>
        </eraNames>
        <eraNarrow>
            <era type="0">B</era>
            <era type="1">A</era>
        </eraNarrow>
    </eras>
```

### 2.2 <a name="monthPatterns_cyclicNameSets" href="#monthPatterns_cyclicNameSets">Elements monthPatterns, cyclicNameSets</a>

```xml
<!ELEMENT monthPatterns ( alias | (monthPatternContext*, special*)) >
<!ELEMENT monthPatternContext ( alias | (monthPatternWidth*, special*)) >
<!ATTLIST monthPatternContext type ( format | stand-alone | numeric ) #REQUIRED >
<!ELEMENT monthPatternWidth ( alias | (monthPattern*, special*)) >
<!ATTLIST monthPatternWidth type ( abbreviated| narrow | wide | all ) #REQUIRED >
<!ELEMENT monthPattern ( #PCDATA ) >
<!ATTLIST monthPattern type ( leap | standardAfterLeap | combined ) #REQUIRED >

<!ELEMENT cyclicNameSets ( alias | (cyclicNameSet*, special*)) >
<!ELEMENT cyclicNameSet ( alias | (cyclicNameContext*, special*)) >
<!ATTLIST cyclicNameSet type ( years | months | days | dayParts | zodiacs | solarTerms ) #REQUIRED >
<!ELEMENT cyclicNameContext ( alias | (cyclicNameWidth*, special*)) >
<!ATTLIST cyclicNameContext type ( format | stand-alone ) #REQUIRED >
<!ELEMENT cyclicNameWidth ( alias | (cyclicName*, special*)) >
<!ATTLIST cyclicNameWidth type ( abbreviated | narrow | wide ) #REQUIRED >
<!ELEMENT cyclicName ( #PCDATA ) >
<!ATTLIST cyclicName type NMTOKEN #REQUIRED >
```

The Chinese lunar calendar can insert a leap month after nearly any month of its year; when this happens, the month takes the name of the preceding month plus a special marker. The Hindu lunar calendars can insert a leap month before any one or two months of the year; when this happens, not only does the leap month take the name of the following month plus a special marker, the following month also takes a special marker. Moreover, in the Hindu calendar sometimes a month is skipped, in which case the preceding month takes a special marker plus the names of both months. The `<monthPatterns>` element structure supports these special kinds of month names. It parallels the `<months>` element structure, with various contexts and widths, but with some differences:

* Since the month markers may be applied to numeric months as well, there is an additional `monthPatternContext` type `numeric` for this case. When the numeric context is used, there is no need for different widths, so the `monthPatternWidth` type is `all` for this case.
* The `<monthPattern>` element itself is a pattern showing how to create the modified month name from the standard month name(s). The three types of possible pattern are for `leap`, `standardAfterLeap`, and `combined`.
* The `<monthPatterns>` element is not present for calendars that do not need it.

The Chinese and Hindu lunar calendars also use a 60-name cycle for designating years. The Chinese lunar calendars can also use that cycle for months and days, and can use 12-name cycles for designating day subdivisions or zodiac names associated with years; a 24-name cycle of solar terms (12 pairs of minor and major terms) is used to mark intervals in the solar cycle. The `<cyclicNameSets>` element structure supports these special kinds of name cycles; a `cyclicNameSet` can be provided for types `year`, `month`, `day`, `dayParts`, or `zodiacs`. For each `cyclicNameSet`, there is a context and width structure similar to that for day names. For a given context and width, a set of `cyclicName` elements provides the actual names.

Example:

```xml
    <monthPatterns>
        <monthPatternContext type="format">
            <monthPatternWidth type="wide">
                <monthPattern type="leap">闰{0}</monthPattern>
            </monthPatternWidth>
        </monthPatternContext>
        <monthPatternContext type="stand-alone">
            <monthPatternWidth type="narrow">
                <monthPattern type="leap">闰{0}</monthPattern>
            </monthPatternWidth>
        </monthPatternContext>
        <monthPatternContext type="numeric">
            <monthPatternWidth type="all">
                <monthPattern type="leap">闰{0}</monthPattern>
            </monthPatternWidth>
        </monthPatternContext>
    </monthPatterns>
    <cyclicNameSets>
        <cyclicNameSet type="years">
            <cyclicNameContext type="format">
                <cyclicNameWidth type="abbreviated">
                    <cyclicName type="1">甲子</cyclicName>
                    <cyclicName type="2">乙丑</cyclicName>
                    ...
                    <cyclicName type="59">壬戌</cyclicName>
                    <cyclicName type="60">癸亥</cyclicName>
                </cyclicNameWidth>
            </cyclicNameContext>
        </cyclicNameSet>
        <cyclicNameSet type="zodiacs">
            <cyclicNameContext type="format">
                <cyclicNameWidth type="abbreviated">
                    <cyclicName type="1">鼠</cyclicName>
                    <cyclicName type="2">牛</cyclicName>
                    ...
                    <cyclicName type="11">狗</cyclicName>
                    <cyclicName type="12">猪</cyclicName>
                </cyclicNameWidth>
            </cyclicNameContext>
        </cyclicNameSet>
        <cyclicNameSet type="solarTerms">
            <cyclicNameContext type="format">
                <cyclicNameWidth type="abbreviated">
                    <cyclicName type="1">立春</cyclicName>
                    <cyclicName type="2">雨水</cyclicName>
                    ...
                    <cyclicName type="23">小寒</cyclicName>
                    <cyclicName type="24">大寒</cyclicName>
                </cyclicNameWidth>
            </cyclicNameContext>
        </cyclicNameSet>
    </cyclicNameSets>
```

### 2.3 <a name="dayPeriods" href="#dayPeriods">Element dayPeriods</a>

The former `am`/`pm` elements have been deprecated, and replaced by the more flexible `dayPeriods`.

```xml
<!ELEMENT dayPeriods ( alias | (dayPeriodContext*) ) >

<!ELEMENT dayPeriodContext (alias | dayPeriodWidth*) >
<!ATTLIST dayPeriodContext type NMTOKEN #REQUIRED >

<!ELEMENT dayPeriodWidth (alias | dayPeriod*) >
<!ATTLIST dayPeriodWidth type NMTOKEN #REQUIRED >

<!ELEMENT dayPeriod ( #PCDATA ) >
<!ATTLIST dayPeriod type NMTOKEN #REQUIRED >
```

These behave like months, days, and so on in terms of having context and width. Each locale has an associated dayPeriodRuleSet in the supplemental data, rules that specify when the day periods start and end for that locale. Each type in the rules needs to have a translation in a dayPeriod (but if translation data is missing for a particular variable dayPeriod in the locale’s language and script, formatting should fall back to using the am/pm values). For more information, see _[Day Period Rules](#Day_Period_Rules)_.

The dayPeriod names should be distinct within each of the context/width combinations, including narrow; as with era names, there is less disambiguating information for them, and they are more likely to be used in a format that requires parsing. In some unambiguous cases, it is acceptable for certain overlapping dayPeriods to be the same, such as the names for "am" and "morning", or the names for "pm" and "afternoon".

Example:

```xml
    <dayPeriods>
        <dayPeriodContext type="format">
            <dayPeriodWidth type="wide">
                <dayPeriod type="am">AM</dayPeriod>
                <dayPeriod type="noon">noon</dayPeriod>
                <dayPeriod type="pm">PM</dayPeriod>
            </dayPeriodWidth>
        </dayPeriodContext>
    </dayPeriods>
```

### 2.4 <a name="dateFormats" href="#dateFormats">Element dateFormats</a>

```xml
<!ELEMENT dateFormats (alias | (default*, dateFormatLength*, special*)) >
<!ELEMENT dateFormatLength (alias | (default*, dateFormat*, special*)) >
<!ATTLIST dateFormatLength type ( full | long | medium | short ) #REQUIRED >
<!ELEMENT dateFormat (alias | (pattern*, datetimeSkeleton*, displayName*, special*)) >
<!ELEMENT pattern ( #PCDATA ) >
<!ATTLIST pattern numbers CDATA #IMPLIED >
<!ATTLIST pattern alt NMTOKENS #IMPLIED >
<!ATTLIST pattern draft (approved | contributed | provisional | unconfirmed) #IMPLIED >
<!ELEMENT datetimeSkeleton ( #PCDATA ) >
<!ATTLIST datetimeSkeleton numbers CDATA #IMPLIED >
<!ATTLIST datetimeSkeleton alt NMTOKENS #IMPLIED >
<!ATTLIST datetimeSkeleton draft (approved | contributed | provisional | unconfirmed) #IMPLIED >
```

Standard date formats have the following form:

```xml
    <dateFormats>
        <dateFormatLength type="full">
            <dateFormat>
                <pattern>EEEE, MMMM d, y</pattern>
                <datetimeSkeleton>yMMMMEEEEd</datetimeSkeleton>
            </dateFormat>
        </dateFormatLength>
        ...
        <dateFormatLength type="medium">
            <dateFormat>
                <pattern>MMM d, y</pattern>
                <datetimeSkeleton>yMMMd</datetimeSkeleton>
            </dateFormat>
        </dateFormatLength>
        ...
    <dateFormats>

    <dateFormats>
        ...
        <dateFormatLength type="medium">
            <dateFormat>
                <pattern numbers="hebr">d בMMMM y</pattern>
                <datetimeSkeleton numbers="hebr">yMMMMd</datetimeSkeleton>
            </dateFormat>
        </dateFormatLength>
        ...
    <dateFormats>

    <dateFormats>
        ...
        <dateFormatLength type="long">
            <dateFormat>
                <pattern numbers="d=hanidays">rU年MMMMd</pattern>
                <datetimeSkeleton numbers="d=hanidays">rMMMMd</datetimeSkeleton>
            </dateFormat>
        </dateFormatLength>
        ...
```

The patterns for date formats and time formats are defined in _[Date Format Patterns](#Date_Format_Patterns)._ These patterns are intended primarily for display of isolated date and time strings in user-interface elements, rather than for date and time strings in the middle of running text, so capitalization and grammatical form should be chosen appropriately.

Standard date and time patterns are each normally provided in four types: full (usually with weekday name), long (with wide month name), medium, and short (usually with numeric month).

The `numbers` attribute can be used to explicitly specify a number system to be used for all of the numeric fields in the date format (as in `numbers="hebr"`), or for a specific field in the date format (as in `numbers="d=hanidays"`). This attribute overrides any default numbering system specified for the locale.

The `datetimeSkeleton` element contains a _skeleton_ (see [availableFormats](#availableFormats_appendItems)) derived from the pattern. In the future the intent is to be able to generate the standard patterns from these `datetimeSkeleton` elements. However, in CLDR 40, the mechanisms associated with the `availableFormats` elements are not quite powerful enough to generate patterns that exactly match all of the ones provided in the `pattern` elements.

### 2.5 <a name="timeFormats" href="#timeFormats">Element timeFormats</a>

```xml
<!ELEMENT timeFormats (alias | (default*, timeFormatLength*, special*)) >
<!ELEMENT timeFormatLength (alias | (default*, timeFormat*, special*)) >
<!ATTLIST timeFormatLength type ( full | long | medium | short ) #REQUIRED >
<!ELEMENT timeFormat (alias | (pattern*, datetimeSkeleton*, displayName*, special*)) >
```
Standard time formats have the following form:

```xml
    <timeFormats>
        <timeFormatLength type="full">
            <timeFormat>
                <displayName>DIN 5008 (EN 28601)</displayName>
                <pattern>h:mm:ss a z</pattern>
                <datetimeSkeleton>ahmmssz</datetimeSkeleton>
            </timeFormat>
        </timeFormatLength>
        <timeFormatLength type="medium">
            <timeFormat>
                <pattern>h:mm:ss a</pattern>
                <datetimeSkeleton>ahmmss</datetimeSkeleton>
            </timeFormat>
        </timeFormatLength>
    </timeFormats>
```

The preference of 12 hour versus 24 hour for the locale can be derived from the [Time Data](#Time_Data). If the preferred hour symbol is 'h' or 'K' then the format is 12 hour; otherwise it is 24 hour. Formats with 'h' or 'K' must also include a field with one of the day period pattern characters: 'a', 'b', or 'B'.

To account for customary usage in some countries, APIs should allow for formatting times that go beyond 23:59:59. For example, in some countries it would be customary to indicate that opening hours extending from _Friday at 7pm_ to _Saturday at 2am_ in a format like the following:

Friday: 19:00 – 26:00

Time formats use the specific non-location format (z or zzzz) for the time zone name. This is the format that should be used when formatting a specific time for presentation. When formatting a time referring to a recurring time (such as a meeting in a calendar), applications should substitute the generic non-location format (v or vvvv) for the time zone in the time format pattern. See _[Using Time Zone Names](#Using_Time_Zone_Names)_ for a complete description of available time zone formats and their uses.

### 2.6 <a name="dateTimeFormats" href="#dateTimeFormats">Element dateTimeFormats</a>

```xml
<!ELEMENT dateTimeFormats (alias | (default*, dateTimeFormatLength*, availableFormats*, appendItems*, intervalFormats*, special*)) >
```

Date/Time formats have the following form:
```xml
    <dateTimeFormats>
        <dateTimeFormatLength type="full">
            <dateTimeFormat>
                <pattern>{1}, {0}</pattern>
            </dateTimeFormat>
            <dateTimeFormat type="atTime">
                <pattern>{1} 'at' {0}</pattern>
            </dateTimeFormat>
        </dateTimeFormatLength>
        <dateTimeFormatLength type="long">
            <dateTimeFormat>
                <pattern>{1}, {0}</pattern>
            </dateTimeFormat>
            <dateTimeFormat type="atTime">
                <pattern>{1} 'at' {0}</pattern>
            </dateTimeFormat>
        </dateTimeFormatLength>
        <dateTimeFormatLength type="medium">
            <dateTimeFormat>
                <pattern>{1}, {0}</pattern>
            </dateTimeFormat>
        </dateTimeFormatLength>
        <dateTimeFormatLength type="short">
            <dateTimeFormat>
                <pattern>{1}, {0}</pattern>
            </dateTimeFormat>
        </dateTimeFormatLength>
        <availableFormats>
            <dateFormatItem id="Hm">HH:mm</dateFormatItem>
            <dateFormatItem id="Hms">HH:mm:ss</dateFormatItem>
            <dateFormatItem id="M">L</dateFormatItem>
            <dateFormatItem id="MEd">E, M/d</dateFormatItem>
            <dateFormatItem id="MMM">LLL</dateFormatItem>
            <dateFormatItem id="MMMEd">E, MMM d</dateFormatItem>
            <dateFormatItem id="MMMMEd">E, MMMM d</dateFormatItem>
            <dateFormatItem id="MMMMd">MMMM d</dateFormatItem>
            <dateFormatItem id="MMMd">MMM d</dateFormatItem>
            <dateFormatItem id="Md">M/d</dateFormatItem>
            <dateFormatItem id="d">d</dateFormatItem>
            <dateFormatItem id="hm">h:mm a</dateFormatItem>
            <dateFormatItem id="ms">mm:ss</dateFormatItem>
            <dateFormatItem id="y">yyyy</dateFormatItem>
            <dateFormatItem id="yM">M/yyyy</dateFormatItem>
            <dateFormatItem id="yMEd">EEE, M/d/yyyy</dateFormatItem>
            <dateFormatItem id="yMMM">MMM yyyy</dateFormatItem>
            <dateFormatItem id="yMMMEd">EEE, MMM d, yyyy</dateFormatItem>
            <dateFormatItem id="yMMMM">MMMM yyyy</dateFormatItem>
            <dateFormatItem id="yQ">Q yyyy</dateFormatItem>
            <dateFormatItem id="yQQQ">QQQ yyyy</dateFormatItem>
            . . .
        </availableFormats>
        <appendItems>
            <appendItem request="Day">{0} ({2}: {1})</appendItem>
            <appendItem request="Day-Of-Week">{0} {1}</appendItem>
            <appendItem request="Era">{0} {1}</appendItem>
            <appendItem request="Hour">{0} ({2}: {1})</appendItem>
            . . .
        </appendItems>
    </dateTimeFormats>

</calendar>

<calendar type="buddhist">
    <eras>
        <eraAbbr>
            <era type="0">BE</era>
        </eraAbbr>
    </eras>
</calendar>
```

These formats allow for date and time formats to be composed in various ways.

#### 2.6.1 <a name="dateTimeFormat" href="#dateTimeFormat">Element dateTimeFormat</a>

```xml
<!ELEMENT dateTimeFormatLength (alias | (default*, dateTimeFormat*, special*))>
<!ATTLIST dateTimeFormatLength type ( full | long | medium | short ) #IMPLIED >
<!ELEMENT dateTimeFormat (alias | (pattern*, displayName*, special*))>
<!ATTLIST dateTimeFormat type NMTOKEN "standard" >
    <!--@MATCH:literal/standard, atTime-->
```

The `dateTimeFormat` element works like the dateFormats and timeFormats, except that the pattern is of the form "{1} {0}", where {0} is replaced by the time format, and {1} is replaced by the date format, with results such as "8/27/06 7:31 AM". Except for the substitution markers {0} and {1}, text in the dateTimeFormat is interpreted as part of a date/time pattern, and is subject to the same rules described in [Date Format Patterns](#Date_Format_Patterns). This includes the need to enclose ASCII letters in single quotes if they are intended to represent literal text.

When combining a standard date pattern with a standard time pattern, start with the `dateTimeFormatLength` whose `type` matches the type of the *date* pattern, and then use one of the `dateTimeFormat`s for that `dateTimeFormatLength` (as described after the following table). For example:

###### Table: <a name="Date_Time_Combination_Examples" href="#Date_Time_Combination_Examples">Date-Time Combination Examples</a>

| Date-Time Combination   | dateTimeFormat            | Results |
| ----------------------- | ------------------------- | ------- |
| full date + short time  | full, e.g. "{1} 'at' {0}" | Wednesday, September 18, 2013 at 4:30 PM |
| medium date + long time | medium, e.g. "{1}, {0}"   | Sep 18, 2013, 4:30:00 PM PDT |

For each `dateTimeFormatLength`, there is a standard `dateTimeFormat`. In addition to the placeholders {0} and {1}, this should not have characters other than space and punctuation; it should impose no grammatical context that might require specific grammatical forms for the date and/or time. For English, this might be “{1}, {0}”.

In addition, especially for the full and long `dateTimeFormatLength`s, there may be a `dateTimeFormat` with `type="atTime"`. This is used to indicate an event at a specific time, and may impose specific grammatical requirements on the formats for date and/or time. For English, this might be “{1} 'at' {0}”.

The default guidelines for choosing which `dateTimeFormat` to use for a given `dateTimeFormatLength` are as follows:
* If an interval is being formatted, use the standard combining pattern to produce e.g. “March 15, 3:00 – 5:00 PM” or “March 15, 9:00 AM – March 16, 5:00 PM”.
* If a single date or relative date is being combined with a single time, by default use the atTime pattern (if available) to produce an event time: “March 15 at 3:00 PM” or “tomorrow at 3:00 PM”.  However, at least in the case of combining a single date and time, APIs should also offer a “current time” option of using the standard combining pattern to produce a format more suitable for indicating  the current time: “March 15, 3:00 PM”.
* For all other uses of these patterns, use the standard pattern.

#### 2.6.2 <a name="availableFormats_appendItems" href="#availableFormats_appendItems">Elements availableFormats, appendItems</a>

```xml
<!ELEMENT availableFormats (alias | (dateFormatItem*, special*))>
<!ELEMENT dateFormatItem ( #PCDATA ) >
<!ATTLIST dateFormatItem id CDATA #REQUIRED >
```

The `availableFormats` element and its subelements provide a more flexible formatting mechanism than the predefined list of patterns represented by dateFormatLength, timeFormatLength, and dateTimeFormatLength. Instead, there is an open-ended list of patterns (represented by `dateFormatItem` elements as well as the predefined patterns mentioned above) that can be matched against a requested set of calendar fields and field lengths. Software can look through the list and find the pattern that best matches the original request, based on the desired calendar fields and lengths. For example, the full month and year may be needed for a calendar application; the request is MMMMyyyy, but the best match may be "y MMMM" or even "G yy MMMM", depending on the locale and calendar.

For some calendars, such as Japanese, a displayed year must have an associated era, so for these calendars dateFormatItem patterns with a year field should also include an era field. When matching availableFormats patterns: If a client requests a format string containing a year, and all the availableFormats patterns with a year also contain an era, then include the era as part of the result.

The `id` attribute is a so-called "skeleton", containing only field information, and in a canonical order. Examples are "yMMMM" for year + full month, or "MMMd" for abbreviated month + day. In particular:

* The fields are from the [Date Field Symbol Table](#Date_Field_Symbol_Table) in _[Date Format Patterns](#Date_Format_Patterns)_.
* The canonical order is from top to bottom in that table; that is, "yM" not "My".
* Only one field of each type is allowed; that is, "Hh" is not valid.

In order to support user overrides of default locale behavior, data should be supplied for both 12-hour-cycle time formats (using h or K) and 24-hour-cycle time formats (using H or k), even if one of those styles is not commonly used; the locale's actual preference for 12-hour or 24-hour time cycle is determined from the [Time Data](#Time_Data) as described above in [timeFormats](#timeFormats). Thus skeletons using h or K should have patterns that only use h or K for hours, while skeletons using H or k should have patterns that only use H or k for hours.

The rules governing use of day period pattern characters in patterns and skeletons are as follows:

* Patterns and skeletons for 24-hour-cycle time formats (using H or k) currently _should not_ include fields with day period characters (a, b, or B); these pattern characters should be ignored if they appear in skeletons. However, in the future, CLDR may allow use of B (but not a or b) in 24-hour-cycle time formats.
* Patterns for 12-hour-cycle time formats (using h or K) _must_ include a day period field using one of a, b, or B.
* Skeletons for 12-hour-cycle time formats (using h or K) _may_ include a day period field using one of a, b, or B. If they do not, the skeleton will be treated as implicitly containing a.

Locales should generally provide availableFormats data for a fairly complete set of time skeletons without B, typically the following:

`H, h, Hm, hm, Hms, hms, Hmv, hmv, Hmsv, hmsv`

Locales that use 12-hour-cycle time formats with B may provide availableFormats data for a smaller set of time skeletons with B, for example:

`Bh, Bhm, Bhms`

When matching a requested skeleton containing b or B to the skeletons actually available in the data, if there is no skeleton matching the specified day period field, then find a match in which the b or B matches an explicit or implicit 'a' in the skeleton, but replace the 'a' in the corresponding pattern with the requested day period b or B. The following table illustrates how requested skeletons map to patterns with different sets of `availableFormats` data:

###### Table: <a name="Mapping_Requested_Time_Skeletons_To_Patterns" href="#Mapping_Requested_Time_Skeletons_To_Patterns">Mapping Requested Time Skeletons To Patterns</a>

<!-- HTML: spanning columns, header cells on non-first row -->
<table><tbody>
<tr><th></th><th colspan="2">results for different availableFormats data sets</th></tr>
<tr><th>requested skeleton</th>
    <th>set 1:<br/>
        ...id="H"&gt;H&lt;/date...<br/>
        ...id="h"&gt;h a&lt;/date...</th>
    <th>set 2:<br/>
        ...id="H"&gt;H&lt;/date...<br/>
        ...id="h"&gt;h a&lt;/date...<br/>
        ...id="Bh"&gt;B h&lt;/date...</th></tr>
<tr><td>"h" (or "ah")</td><td>"h a"</td><td>"h a"</td></tr><tr><td>"bh"</td><td>"h b"</td><td>"h b"</td></tr>
<tr><td>"Bh"</td><td>"h B"</td><td>"B h"</td></tr><tr><td>"H" (or "aH", "bH", "BH")</td><td>"H"</td><td>"H"</td></tr>
</tbody></table>

The hour input skeleton symbols 'j', 'J', and 'C' can be used to select the best hour format (h, H, …) before processing, and the appropriate dayperiod format (a, b, B) after a successful match that contains an 'a' symbol.

The dateFormatItems inherit from their parent locale, so the inherited items need to be considered when processing.

##### 2.6.2.1 <a name="Matching_Skeletons" href="#Matching_Skeletons">Matching Skeletons</a>

It is not necessary to supply `dateFormatItem`s with skeletons for every field length; fields in the skeleton and pattern are expected to be adjusted in parallel to handle a request.

Typically a “best match” from requested skeleton to the `id` portion of a `dateFormatItem` is found using a closest distance match, such as:

1. Skeleton symbols requesting a best choice for the locale are replaced.
   * j → one of {H, k, h, K}; C → one of {a, b, B}

2. For skeleton and `id` fields with symbols representing the same type (year, month, day, etc):
   1. Most symbols have a small distance from each other.
      * M ≅ L; E ≅ c; a ≅ b ≅ B; H ≅ k ≅ h ≅ K; ...
   2. Width differences among fields, other than those marking text vs numeric, are given small distance from each other.
      * MMM ≅ MMMM
      * MM ≅ M
   3. Numeric and text fields are given a larger distance from each other.
      * MMM ≈ MM
   4. Symbols representing substantial differences (week of year vs week of month) are given a much larger distance from each other.
      * d ≋ D; ...

3. A requested skeleton that includes both seconds and fractional seconds (e.g. “mmssSSS”) is allowed to match a dateFormatItem skeleton that includes seconds but not fractional seconds (e.g. “ms”). In this case the requested sequence of ‘S’ characters (or its length) should be retained separately and used when adjusting the pattern, as described below.

4. Otherwise, missing or extra fields between requested skeleton and `id` cause a match to fail. (But see **[Missing Skeleton Fields](#Missing_Skeleton_Fields)** below.)

Once a best match is found between requested skeleton and `dateFormatItem` `id`, the corresponding `dateFormatItem` pattern is used, but with adjustments primarily to make the pattern field lengths match the skeleton field lengths. However, the pattern field lengths should not be matched in some cases:

1. When the best-match `dateFormatItem` has an alphabetic field (such as MMM or MMMM) that corresponds to a numeric field in the pattern (such as M or MM), that numeric field in the pattern should _not_ be adjusted to match the skeleton length, and vice versa; i.e. adjustments should _never_ convert a numeric element in the pattern to an alphabetic element, or the opposite. See the second set of examples below.

2. When the pattern field corresponds to an availableFormats skeleton with a field length that matches the field length in the requested skeleton, the pattern field length should _not_ be adjusted. This permits locale data to override a requested field length; see the third example below.

3. Pattern field lengths for hour, minute, and second should by default not be adjusted to match the requested field length (i.e. locale data takes priority). However APIs that map skeletons to patterns should provide the option to override this behavior for cases when a client really does want to force a specific pattern field length.

---

For an example of general behavior, consider the following `dateFormatItem`:

```xml
<dateFormatItem id="yMMMd">d MMM y</dateFormatItem>
```

If this is the best match for yMMMMd, the pattern is automatically expanded to produce a pattern "d MMMM y" in response to the request. Of course, if the desired behavior is that a request for yMMMMd should produce something _other_ than "d MMMM y", a separate `dateFormatItem` must be present, for example:

```xml
<dateFormatItem id="yMMMMd">d 'de' MMMM 'de' y</dateFormatItem>
```

---

For an example of not converting a pattern fields between numeric and alphabetic (point 1 above), consider the following `dateFormatItem`:

```xml
<dateFormatItem id="yMMM">y年M月</dateFormatItem>
```

If this is the best match for a requested skeleton yMMMM, automatic expansion should not produce a corresponding pattern “y年MMMM月”; rather, since “y年M月” specifies a numeric month M, automatic expansion should not modify the pattern, and should produce “y年M月” as the match for requested skeleton yMMMM. 

---

For an example of not converting a pattern field length if the corresponding skeleton field matches the requested field length (point 2 above), consider the following `dateFormatItem`:

```xml
<dateFormatItem id="MMMEd">E, d בMMMM</dateFormatItem>
```

For Hebrew calendar date formats in the Hebrew locale, only the full month names should be used, even if abbreviated months are requested. Hence the `dateFormatItem` maps a request for abbreviated months to a pattern with full months. The same `dateFormatItem` can be expanded to expanded to match a request for “MMMMEd” to the same pattern.

---

Finally: If the requested skeleton included both seconds and fractional seconds and the dateFormatItem skeleton included seconds but not fractional seconds, then the seconds field of the corresponding pattern should be adjusted by appending the locale’s decimal separator, followed by the sequence of ‘S’ characters from the requested skeleton.

##### 2.6.2.2 <a name="Missing_Skeleton_Fields" href="#Missing_Skeleton_Fields">Missing Skeleton Fields</a>

If a client-requested set of fields includes both date and time fields, and if the `availableFormats` data does not include a `dateFormatItem` whose skeleton matches the same set of fields, then the request should be handled as follows:

1. Divide the request into a date fields part and a time fields part.
2. For each part, find the matching `dateFormatItem`, and expand the pattern as above.
3. Combine the patterns for the two `dateFormatItem`s using the appropriate dateTimeFormat pattern, determined as follows from the requested date fields:
   * If the requested date fields include wide month (MMMM, LLLL) and weekday name of any length (e.g. E, EEEE, c, cccc), use `<dateTimeFormatLength type="full">`
   * Otherwise, if the requested date fields include wide month, use `<dateTimeFormatLength type="long">`
   * Otherwise, if the requested date fields include abbreviated month (MMM, LLL), use `<dateTimeFormatLength type="medium">`
   * Otherwise use `<dateTimeFormatLength type="short">`

```xml
<!ELEMENT appendItems (alias | (appendItem*, special*))>
<!ELEMENT appendItem ( #PCDATA ) >
<!ATTLIST appendItem request CDATA >
```

In case the best match does not include all the requested calendar fields, the `appendItems` element describes how to append needed fields to one of the existing formats. Each `appendItem` element covers a single calendar field. In the pattern, {0} represents the format string, {1} the data content of the field, and {2} the display name of the field (see [Calendar Fields](#Calendar_Fields)).

#### 2.6.3 <a name="intervalFormats" href="#intervalFormats">Element intervalFormats</a>

```xml
<!ELEMENT intervalFormats (alias | (intervalFormatFallback*, intervalFormatItem*, special*)) >

<!ELEMENT intervalFormatFallback ( #PCDATA ) >

<!ELEMENT intervalFormatItem (alias | (greatestDifference*, special*)) >
<!ATTLIST intervalFormatItem id NMTOKEN #REQUIRED >

<!ELEMENT greatestDifference ( #PCDATA ) >
<!ATTLIST greatestDifference id NMTOKEN #REQUIRED >
```

Interval formats allow for software to format intervals like "Jan 10-12, 2008" as a shorter and more natural format than "Jan 10, 2008 - Jan 12, 2008". They are designed to take a "skeleton" pattern (like the one used in availableFormats) plus start and end datetime, and use that information to produce a localized format.

The data supplied in CLDR requires the software to determine the calendar field with the greatest difference before using the format pattern. For example, the greatest difference in "Jan 10-12, 2008" is the day field, while the greatest difference in "Jan 10 - Feb 12, 2008" is the month field. This is used to pick the exact pattern.

The pattern is then designed to be broken up into two pieces by determining the first repeating field. For example, "MMM d-d, y" would be broken up into "MMM d-" and "d, y". The two parts are formatted with the first and second datetime, as described in more detail below.

For the purposes of determining a repeating field, standalone fields and format fields are considered equivalent. For example, given the pattern "LLL d - MMM d, Y", the repeating field would be "M" since standalone month field "L" is considered equivalent to format field "M" when determining the repeating field. Therefore the pattern would be broken up into "LLL d - " and "MMM d, Y".

In case there is no matching pattern, the intervalFormatFallback defines the fallback pattern. The fallback pattern is of the form "{0} - {1}" or "{1} - {0}", where {0} is replaced by the start datetime, and {1} is replaced by the end datetime. The fallback pattern determines the default order of the interval pattern. "{0} - {1}" means the first part of the interval patterns in current local are formatted with the start datetime, while "{1} - {0}" means the first part of the interval patterns in current locale are formatted with the end datetime.

The `id` attribute of intervalFormatItem is the "skeleton" pattern (like the one used in availableFormats) on which the format pattern is based. The `id` attribute of `greatestDifference` is the calendar field letter, for example 'M', which is the greatest difference between start and end datetime.

The greatest difference defines a specific interval pattern of start and end datetime on a "skeleton" and a greatestDifference. As stated above, the interval pattern is designed to be broken up into two pieces. Each piece is similar to the pattern defined in date format. Also, each interval pattern could override the default order defined in fallback pattern. If an interval pattern starts with "latestFirst:", the first part of this particular interval pattern is formatted with the end datetime. If an interval pattern starts with "earliestFirst:", the first part of this particular interval pattern is formatted with the start datetime. Otherwise, the order is the same as the order defined in `intervalFormatFallback`.

For example, the English rules that produce "Jan 10–12, 2008", "Jan 10 – Feb 12, 2008", and "Jan 10, 2008 – Feb. 12, 2009" are as follows:

```xml
<intervalFormatItem id="yMMMd">
    <greatestDifference id="M">MMM d – MMM d, yyyy</greatestDifference>
    <greatestDifference id="d">MMM d–d, yyyy</greatestDifference>
    <greatestDifference id="y">MMM d, yyyy – MMM d, yyyy</greatestDifference>
</intervalFormatItem>
```

To format a start and end datetime, given a particular "skeleton":

1. Look for the `intervalFormatItem` element that matches the "skeleton", starting in the current locale and then following the locale fallback chain up to, but not including root (better results are obtained by following steps 2-6 below with locale- or language-specific data than by using matching intervalFormats from root).
2. If no match was found from the previous step, check what the closest match is in the fallback locale chain, as in `availableFormats`. That is, this allows for adjusting the string value field's width, including adjusting between "MMM" and "MMMM", and using different variants of the same field, such as 'v' and 'z'.
3. If no match was found from the previous steps and the skeleton combines date fields such as y,M,d with time fields such as H,h,m,s, then an `intervalFormatItem` can be synthesized as follows:
   1. For `greatestDifference` values corresponding to the date fields in the skeleton, use the mechanisms described under [availableFormats](#availableFormats_appendItems) to generate the complete date-time pattern corresponding to the skeleton, and then combine two such patterns using the `intervalFormatFallback` pattern (the result will be the same for each `greatestDifference` of a day or longer). For example:
      MMMdHm/d → "MMM d 'at' H:mm – MMM d 'at' H:mm" → "Jan 3 at 9:00 – Jan 6 at 11:00"
   2. For `greatestDifference` values corresponding to the time fields in the skeleton, separate the skeleton into a date fields part and a time fields part. Use the mechanisms described under availableFormats to generate a date pattern corresponding to the date fields part. Use the time fields part to look up an `intervalFormatItem`. For each `greatestDifference` in the `intervalFormatItem`, generate a pattern by using the [dateTimeFormat](#dateTimeFormat) to combine the date pattern with the `intervalFormatItem`’s `greatestDifference` element value. For example:
      MMMdHm/H → "MMM d 'at' H:mm – H:mm" → "Jan 3 at 9:00 – 11:00"
4. If a match is found from previous steps, compute the calendar field with the greatest difference between start and end datetime. If there is no difference among any of the fields in the pattern, format as a single date using `availableFormats`, and return.
5. Otherwise, look for `greatestDifference` element that matches this particular greatest difference.
6. If there is a match, use the pieces of the corresponding pattern to format the start and end datetime, as above.
7. Otherwise, format the start and end datetime using the fallback pattern.

## 3 <a name="Calendar_Fields" href="#Calendar_Fields">Calendar Fields</a>

```xml
<!ELEMENT fields ( alias | (field*, special*)) >
<!ELEMENT field ( alias | (displayName*, relative*, relativeTime*, relativePeriod*, special*)) >
<!ATTLIST field type ( era | era-short | era-narrow | year | year-short | year-narrow | quarter | quarter-short | quarter-narrow | month | month-short | month-narrow | week | week-short | week-narrow | weekOfMonth | weekOfMonth-short | weekOfMonth-narrow | day | day-short | day-narrow | dayOfYear | dayOfYear-short | dayOfYear-narrow | weekday | weekday-short | weekday-narrow | weekdayOfMonth | weekdayOfMonth-short | weekdayOfMonth-narrow | sun | sun-short | sun-narrow | mon | mon-short | mon-narrow | tue | tue-short | tue-narrow | wed | wed-short | wed-narrow | thu | thu-short | thu-narrow | fri | fri-short | fri-narrow | sat | sat-short | sat-narrow | dayperiod | dayperiod-short | dayperiod-narrow | hour | hour-short | hour-narrow | minute | minute-short | minute-narrow | second | second-short | second-narrow | zone | zone-short | zone-narrow ) #IMPLIED >

<!ELEMENT relative (#PCDATA) >
<!ATTLIST relative type NMTOKEN #IMPLIED >

<!ELEMENT relativeTime ( alias | (relativeTimePattern*, special*)) >
<!ATTLIST relativeTime type NMTOKEN #REQUIRED >

<!ELEMENT relativeTimePattern ( #PCDATA ) >
<!ATTLIST relativeTimePattern count ( zero | one | two | few | many | other ) #REQUIRED >

<!ELEMENT relativePeriod (#PCDATA) >
```

Translations may be supplied for names of calendar fields (elements of a calendar, such as Day, Month, Year, Hour, and so on), and for relative values for those fields (for example, the day with relative value -1 is "Yesterday"). There are four types of translations; some are only relevant or useful for certain types of fields:

* `<displayName>` General display name for the field type. This should be relevant for all elements, including those like era and zone that might not have useful forms for the other name types. These are typically presented in titlecase (eg “Day”) since they are intended as labels in a UI.
* `<relative>` Display names for the current instance of the field, and one or two past and future instances. In English, data is provided for year, quarter, month, week, day, specific days of the week (sun, mon, tue, …), and—with offset 0 only—for hour, minute, and second.
* `<relativeTime>` Display names for an instance of the field that is a counted number of units in the past or the future relative to the current instance; this needs plural forms. In English, data is provided for year, quarter, month, week, day, specific days of the week, hour, minute, and second.
* `<relativePeriod>` Pattern for designating an instance of the specified field in relation to some other date reference. This is currently only used for weeks, and provides a pattern such as “the week of {0}” which can be used to generate designations such as “the week of April 11, 2016” or “the week of April 11–15”.

Where there is not a convenient, customary word or phrase in a particular language for a particular type of relative value, it should be omitted.

Examples, first for English:

```xml
<fields>
    …
    <field type="day">
        <displayName>Day</displayName>
        <relative type="-1">yesterday</relative>
        <relative type="0">today</relative>
        <relative type="1">tomorrow</relative>
        <relativeTime type="future">
            <relativeTimePattern count="one">in {0} day</relativeTimePattern>
            <relativeTimePattern count="other">in {0} days</relativeTimePattern>
        </relativeTime>
        <relativeTime type="past">
            <relativeTimePattern count="one">{0} day ago</relativeTimePattern>
            <relativeTimePattern count="other">{0} days ago</relativeTimePattern>
        </relativeTime>
    </field>
    <field type="weekday">
        <displayName>Day of the Week</displayName>
    </field>
    <field type="sun">
        <relative type="-1">last Sunday</relative>
        <relative type="0">this Sunday</relative>
        <relative type="1">next Sunday</relative>
        <relativeTime type="future">
            <relativeTimePattern count="one">in {0} Sunday</relativeTimePattern>
            <relativeTimePattern count="other">in {0} Sundays</relativeTimePattern>
        </relativeTime>
        <relativeTime type="past">
            <relativeTimePattern count="one">{0} Sunday ago</relativeTimePattern>
            <relativeTimePattern count="other">{0} Sundays ago</relativeTimePattern>
        </relativeTime>
    </field>
    …
    <field type="hour">
        <displayName>Hour</displayName>
        <relative type="0">now</relative>
        <relativeTime type="future">
            <relativeTimePattern count="one">in {0} hour</relativeTimePattern>
            <relativeTimePattern count="other">in {0} hours</relativeTimePattern>
        </relativeTime>
        <relativeTime type="past">
            <relativeTimePattern count="one">{0} hour ago</relativeTimePattern>
            <relativeTimePattern count="other">{0} hours ago</relativeTimePattern>
        </relativeTime>
    </field>
    …
</fields>

```

Second, for German; includes relative type="-2"/"2", present in the English example:

```xml
<fields>
    …
    <field type="day">
        <displayName>Tag</displayName>
        <relative type="-2">Vorgestern</relative>
        <relative type="-1">Gestern</relative>
        <relative type="0">Heute</relative>
        <relative type="1">Morgen</relative>
        <relative type="2">Übermorgen</relative>
        <relativeTime type="future">
            <relativeTimePattern count="one">In {0} Tag</relativeTimePattern>
            <relativeTimePattern count="other">In {0} Tagen</relativeTimePattern>
        </relativeTime>
        <relativeTime type="past">
            <relativeTimePattern count="one">Vor {0} Tag</relativeTimePattern>
            <relativeTimePattern count="other">Vor {0} Tagen</relativeTimePattern>
        </relativeTime>
    </field>
    …
</fields>
```

A special name for “now” is indicated using `<relative type="0">` for the "second" field. For example, in English:

```xml
<field type="second">
    <displayName>Second</displayName>
    <relative type="0">now</relative>
    …
</field>
```

Different widths can be supplied for certain fields, such as:

```xml
<field type="year-short">
    <displayName>yr.</displayName>
    <relative type="-1">last yr.</relative>
    <relative type="0">this yr.</relative>
    <relative type="1">next yr.</relative>
    <relativeTime type="future">
        <relativeTimePattern count="one">in {0} yr.</relativeTimePattern>
        <relativeTimePattern count="other">in {0} yr.</relativeTimePattern>
    </relativeTime>
    <relativeTime type="past">
        <relativeTimePattern count="one">{0} yr. ago</relativeTimePattern>
        <relativeTimePattern count="other">{0} yr. ago</relativeTimePattern>
    </relativeTime>
</field>
```

As in other cases, **narrow** may be ambiguous out of context.

## 4 <a name="Supplemental_Calendar_Data" href="#Supplemental_Calendar_Data">Supplemental Calendar Data</a>

### 4.1 <a name="Calendar_Data" href="#Calendar_Data">Calendar Data</a>

```xml
<!ELEMENT calendarData ( calendar* )>
<!ELEMENT calendar ( calendarSystem?, eras? )>
<!ATTLIST calendar type NMTOKENS #REQUIRED>
<!ATTLIST calendar territories NMTOKENS #IMPLIED > <!-- deprecated, replaced by calendarPreferenceData -->

<!ELEMENT calendarSystem EMPTY>
<!ATTLIST calendarSystem type (solar | lunar | lunisolar | other) #REQUIRED>

<!ELEMENT eras ( era* )>

<!ELEMENT era EMPTY>
<!ATTLIST era type NMTOKENS #REQUIRED>
<!ATTLIST era start CDATA #IMPLIED>
<!ATTLIST era end CDATA #IMPLIED>
```

The `<calendarData>` element now provides only locale-independent data about calendar behaviors via its `<calendar>` subelements, which for each calendar can specify the astronomical basis of the calendar (solar, lunar, etc.) and the date ranges for its eras.

Era start or end dates are specified in terms of the equivalent proleptic Gregorian date (in "y-M-d" format). Eras may be open-ended, with unspecified start or end dates. For example, here are the eras for the Gregorian calendar:

```xml
<era type="0" end="0" />
<era type="1" start="1" />
```

For a sequence of eras with specified start dates, the end of each era need not be explicitly specified (it is assumed to match the start of the subsequent era). For example, here are the first few eras for the Japanese calendar:

```xml
<era type="0" start="645-6-19" />
<era type="1" start="650-2-15" />
<era type="2" start="672-1-1" />
…
```

**Note:** The `territories` attribute in the `calendar` element is deprecated. It was formerly used to indicate calendar preference by territory, but this is now given by the _[Calendar Preference Data](#Calendar_Preference_Data)_ below.

### 4.2 <a name="Calendar_Preference_Data" href="#Calendar_Preference_Data">Calendar Preference Data</a>

```xml
<!ELEMENT calendarPreferenceData ( calendarPreference* ) >
<!ELEMENT calendarPreference EMPTY >
<!ATTLIST calendarPreference territories NMTOKENS #REQUIRED >
<!ATTLIST calendarPreference ordering NMTOKENS #REQUIRED >
```

The `calendarPreference` element provides a list of commonly used calendar types in a territory. The `ordering` attribute indicates the list of calendar types in preferred order. The first calendar type in the list is the default calendar type for the territory. For example:

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

### 4.3 <a name="Week_Data" href="#Week_Data">Week Data</a>

```xml
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

These values provide territory-specific information needed for week-of-year and week-of-month calculations, as well as information on conventions for first day of the week, for weekends, and for week designations. For most elements, the default is provided by the element with `territories="001"`; for `weekOfPreference` elements the default is provided by the element with `locales="und"`.

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

In order for a week to count as the first week of a new year for week-of-year calculations, it must include at least the number of days in the new year specified by the minDays value; otherwise the week will count as the last week of the previous year (and for week-of-month calculations, `minDays` also specifies the minimum number of days in the new month for a week to count as part of that month).

The day indicated by `firstDay` is the one that should be shown as the first day of the week in a calendar view. This is not necessarily the same as the first day after the weekend (or the first work day of the week), which should be determined from the weekend information. Currently, day-of-week numbering is based on `firstDay` (that is, day 1 is the day specified by `firstDay`), but in the future we may add a way to specify this separately.

What is meant by the weekend varies from country to country. It is typically when most non-retail businesses are closed. The time should not be specified unless it is a well-recognized part of the day. The `weekendStart` day defaults to "sat", and `weekendEnd` day defaults to "sun". For more information, see _[Dates and Date Ranges](tr35.md#Date_Ranges)_.

Each `weekOfPreference` element provides, for its specified locales, an ordered list of the preferred types of week designations for that set of locales. There are four types of week designations, each of which makes use of date patterns available in the locale, as follows:

###### Table: <a name="Week_Designation_Types" href="#Week_Designation_Types">Week Designation Types</a>

| Type           | Examples                          | Date Pattern                                                | Comments    |
|----------------|-----------------------------------|-------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| weekOfYear     | week 15 of 2016                   | \<dateFormatItem id='yw' count='one'\>'week' w 'of' Y\<…       | The **week of** construction takes a count attribute, just in case the pattern changes depending on the numeric value of the **w** value. (In the future, we're likely to add an ordinal value, for constructions like “3rd week of March”.) In languages where the month name needs grammatical changes (aside from just the simple addition of a prefix or suffix), localizers will typically use a work-around construction. |
| weekOfMonth    | week 2 of April 2nd week of April | \<dateFormatItem id='MMMMW'' count='one'\>'week' W 'of' MMM\<… |   (same comment as above) |
| weekOfDate     | the week of April 11, 2016        | \<field type="week"\>\<relativePeriod>the week of {0}\<…        | The date pattern that replaces {0} is determined separately and may use the first day or workday of the week, the range of the full week or work week, etc.   |
| weekOfInterval | the week of April 11–15           | \<field type="week"\>\<relativePeriod>the week of {0}\<…    |  (same comment as above) |

### 4.4 <a name="Time_Data" href="#Time_Data">Time Data</a>

```xml
<!ELEMENT timeData ( hours* ) >
<!ELEMENT hours EMPTY >
<!ATTLIST hours preferred NMTOKEN #REQUIRED >
<!ATTLIST hours allowed NMTOKENS #REQUIRED >
<!ATTLIST hours regions NMTOKENS #REQUIRED >
```

This element is for data that indicates, for various regions, the preferred time cycle in the region, as well as all time cycles that are considered acceptable in the region. The defaults are those specified for region 001.

There is a single `preferred` value, and multiple `allowed` values. The meanings of the values H, h, K, k, b and B are defined in [Date Field Symbol Table](#Date_Field_Symbol_Table). The `allowed` values are in preference order, and are used with the 'C' hour skeleton pattern symbol.

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

The B and b date symbols provide for formats like “3:00 at night”. When the ‘C’ option is used, the values in `allowed` are traversed from first to last, picking the first available format. For example, in the following a system that supports hB should choose that as the most preferred format for the C (not the `preferred` value H).

```xml
<hours preferred="H" allowed="hB H" regions="CD" />
<hours preferred="H" allowed="hB hb h H" regions="KE MM TZ UG" />
```

Some systems may not want to use B and b, even if preferred for the locale, so for compatibility the `preferred` value is limited to {H, h, K, k}, and is the option selected by the ‘j’ date symbol. Thus the `preferred` value may not be the same as the first `allowed` value.

### 4.5 <a name="Day_Period_Rule_Sets" href="#Day_Period_Rule_Sets">Day Period Rule Sets</a>

```xml
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

Each locale can have a set of day period rules, which determine the periods during a day for use in time formats like "10:00 at night", or to select statements like "Your email arrived last night." If locales do not have dayPeriodRules, the computation of dayPeriods falls back to AM/PM.

There are two kinds of dayPeriodRuleSets, based on the type:

The **_format_** type is used in conjunction with times, such as to express "3:00 in the afternoon", or "12:00 noon". Many languages do not normally use terms that match AM/PM for such times, instead breaking up the day into more periods.

The **stand-alone** type is used for selecting a period of the day for a general time associated with an event. For example, it can be used to select a message like:

```
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

As with plurals, the exact set of periods used for any language may be different. It is the responsibility of any translation software to pick the relevant day periods for the locale for display to the translator (and end user).

#### 4.5.1 <a name="Day_Period_Rules" href="#Day_Period_Rules">Day Period Rules</a>

Here are the requirements for a rule set.

##### 4.5.1.1 <a name="Fixed_periods" href="#Fixed_periods">Fixed periods</a>

There are 4 dayPeriods that are fixed; am/pm are always defined, and always have the same meaning and definition for every locale. Midnight and noon are optional, however if they are defined, they have the same meaning and definition as in all other locales where they are defined.

```xml
<dayPeriodRule type="midnight" at="00:00" />
<dayPeriodRule type="am" from="00:00" before="12:00" />
<dayPeriodRule type="noon" at="12:00" />
<dayPeriodRule type="pm" from="12:00" before="24:00" />
```

Note that midnight and am can overlap, as can noon and pm.

All locales must support am/pm, but not all support **noon** or **midnight**; they are only supported if they meet the above definitions. For example, German has no unique term that means exactly 12:00 noon; the closest is Mittag, but that can extend before or after 12 noon.

**Midnight** is also special, since it can refer to either 00:00 or 24:00 — either at the start or end of the day. That means that Tuesday 24:00 = Wednesday 00:00. “Midnight Tuesday" is thus ambiguous: it means 24:00 in “the party is Tuesday from 10pm to 12 midnight”, while it means 00:00 in “I was awake from 12 midnight to 3 in the morning”.

It is strongly recommended that implementations provide for the ability to specify whether **midnight** is supported or not (and for either 00:00 or 24:00 or both), since only the caller knows enough of the context to determine what to use. In the absence of such information, 24:00 may be the best choice.

##### 4.5.1.2 <a name="Variable_periods" href="#Variable_periods">Variable periods</a>

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

##### 4.5.1.3 <a name="Parsing_Day_Periods" href="#Parsing_Day_Periods">Parsing Day Periods</a>

When parsing, if the hour is present with a strict parse the dayperiod is checked for consistency with the hour. If there is no hour, the center of the first matching dayPeriodRule can be chosen (starting from 0:00). However, if there is other information available when parsing, a different point within the interval may be chosen.

The dayPeriodRule may span two days, such as where **night1** is [21:00, 06:00). In that case, the midpoint is 01:30, so when parsing “Nov 12, at night”, the midpoint result would be Nov 12, 01:30. “Nov 12, am”, “Nov 12, pm”, “Nov 12, noon” can be parsed similarly, resulting in Nov 12, 06:00; Nov 12, 18:00; and Nov 12, 12:00; respectively.

“Nov 12, midnight” is special, because midnight may mean either 00:00 or 24:00. Extra information may be needed to disambiguate which is meant, such as whether the time is at the start or end of an interval. In the absence of such information, 24:00 may be the best choice. See the discussion of **midnight** above.

If rounding is done—including the rounding done by the time format—then it needs to be done before the dayperiod is computed, so that the correct format is shown.

For examples, see [Day Periods Chart](https://unicode-org.github.io/cldr-staging/charts/38/supplemental/day_periods.html).

## 5 <a name="Time_Zone_Names" href="#Time_Zone_Names">Time Zone Names</a>

```xml
<!ELEMENT timeZoneNames (alias | (hourFormat*, gmtFormat*, gmtZeroFormat*, regionFormat*, fallbackFormat*, zone*, metazone*, special*)) >

<!ELEMENT hourFormat ( #PCDATA ) >
<!ELEMENT gmtFormat ( #PCDATA ) >
<!ELEMENT gmtZeroFormat ( #PCDATA ) >

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

The time zone IDs (TZID) are language-independent, and follow the _TZ time zone database_ [[Olson](tr35.md#Olson)] and naming conventions. However, the display names for those IDs can vary by locale. The generic time is so-called _wall-time_; what clocks use when they are correctly switched from standard to daylight time at the mandated time of the year.

Unfortunately, the canonical TZIDs (those in zone.tab) are not stable: they may change in each release of the _TZ_ Time Zone database. In CLDR, however, stability of identifiers is very important. So the canonical IDs in CLDR are kept stable as described in [Canonical Form](tr35.md#Canonical_Form).

The _TZ time zone database_ can have multiple IDs that refer to the same entity. It does contain information on equivalence relationships between these IDs, such as "Asia/Calcutta" and "Asia/Kolkata". It does not remove IDs (with a few known exceptions), but it may change the "canonical" ID which is in the file zone.tab.

For lookup purposes specifications such as CLDR need a stable canonical ID, one that does not change from release to release. The stable ID is maintained as the first alias item _type_ element in the file bcp47/timezone.xml, such as:

    <type name="inccu" alias="Asia/Calcutta Asia/Kolkata"/>

That file also contains the short ID used in keywords. In versions of CLDR previous to 1.8, the alias information (but not the short ID) was in Supplemental Data under the zoneItem, such as:

    <zoneItem type="Asia/Calcutta" territory="IN" aliases="Asia/Kolkata"/>

This element was deprecated after the introduction of bcp47/timezone.xml, because the information became redundant (or was contained in the _TZ time zone database_).

The following is an example of time zone data. Although this is an example of possible data, in most cases only the exemplarCity needs translation. And that does not even need to be present, if a country only has a single time one. As always, the _type_ field for each zone is the identification of that zone. It is not to be translated.

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

> **Note:** CLDR does not allow "GMT", "UT", or "UTC" as translations (short or long) of time zones other than GMT itself.

> **Note:** Transmitting "14:30" with no other context is incomplete unless it contains information about the time zone. Ideally one would transmit neutral-format date/time information, commonly in UTC (GMT), and localize as close to the user as possible. (For more about UTC, see [[UTCInfo](tr35.md#UTCInfo)].)

The conversion from local time into UTC depends on the particular time zone rules, which will vary by location. The standard data used for converting local time (sometimes called _wall time_) to UTC and back is the _TZ Data_ [[Olson](tr35.md#Olson)], used by Linux, UNIX, Java, ICU, and others. The data includes rules for matching the laws for time changes in different countries. For example, for the US it is:

> "During the period commencing at 2 o'clock antemeridian on the second Sunday of March of each year and ending at 2 o'clock antemeridian on the first Sunday of November of each year, the standard time of each zone established by sections 261 to 264 of this title, as modified by section 265 of this title, shall be advanced one hour..." (United States Law - 15 U.S.C. §6(IX)(260-7), as amended by Energy Policy Act of 2005).

Each region that has a different time zone or daylight savings time rules, either now or at any time back to 1970, is given a unique internal ID, such as `Europe/Paris` . (Some IDs are also distinguished on the basis of differences before 1970.) As with currency codes, these are internal codes. A localized string associated with these is provided for users (such as in the Windows _Control Panels>Date/Time>Time Zone_).

Unfortunately, laws change over time, and will continue to change in the future, both for the boundaries of time zone regions and the rules for daylight savings. Thus the _TZ_ data is continually being augmented. Any two implementations using the same version of the _TZ_ data will get the same results for the same IDs (assuming a correct implementation). However, if implementations use different versions of the data they may get different results. So if precise results are required then both the _TZ_ ID and the _TZ_ data version must be transmitted between the different implementations.

For more information, see [[Data Formats](tr35.md#DataFormats)].

The following subelements of `<timeZoneNames>` are used to control the fallback process described in [Using Time Zone Names](#Using_Time_Zone_Names).

###### Table: <a name="timeZoneNames_Elements_Used_for_Fallback" href="#timeZoneNames_Elements_Used_for_Fallback">timeZoneNames Elements Used for Fallback</a>

<table><tbody>
<tr><th>Element Name</th><th>Data Examples</th><th>Results/Comment</th></tr>
<tr><td rowspan="2">hourFormat</td><td rowspan="2">"+HHmm;-HHmm"</td><td>"+1200"</td></tr>
    <tr><td>"-1200"</td></tr>
<tr><td rowspan="2">gmtFormat</td><td>"GMT{0}"</td><td>"GMT-0800"</td></tr>
    <tr><td>"{0}ВпГ"</td><td>"-0800ВпГ"</td></tr>
<tr><td>gmtZeroFormat</td><td>"GMT"</td><td>Specifies how GMT/UTC with no explicit offset (implied 0 offset) should be represented.</td></tr>
<tr><td rowspan="2">regionFormat</td><td>"{0} Time"</td><td>"Japan Time"</td></tr>
    <tr><td>"Hora de {0}"</td><td>"Hora de Japón"</td></tr>
<tr><td rowspan="2">regionFormat type="daylight"<br>(or "standard")</td><td>"{0} Daylight Time"</td><td>"France Daylight Time"</td></tr><tr><td>"horario de verano de {0}"</td><td>"horario de verano de Francia"</td></tr>
    <tr><td>fallbackFormat</td><td>"{1} ({0})"</td><td>"Pacific Time (Canada)"</td></tr>
</tbody></table>

When referring to the abbreviated (short) form of the time zone name, there are often situations where the location-based (city or country) time zone designation for a particular language may not be in common usage in a particular territory.

> **Note:** User interfaces for time zone selection can use the "generic location format" for time zone names to obtain the most useful ordering of names in a menu or list; see _[Using Time Zone Names](#Using_Time_Zone_Names)_ and the zone section of the _[Date Field Symbol Table](#Date_Field_Symbol_Table)._

### 5.1 <a name="Metazone_Names" href="#Metazone_Names">Metazone Names</a>

A metazone is a grouping of one or more internal TZIDs that share a common display name in current customary usage, or that have shared a common display name during some particular time period. For example, the zones _Europe/Paris, Europe/Andorra, Europe/Tirane, Europe/Vienna, Europe/Sarajevo, Europe/Brussels, Europe/Zurich, Europe/Prague, Europe/Berlin_, and so on are often simply designated _Central European Time_ (or translated equivalent).

A metazone's display fields become a secondary fallback if an appropriate data field cannot be found in the explicit time zone data. The _usesMetazone_ field indicates that the target metazone is active for a particular time. This also provides a mechanism to effectively deal with situations where the time zone in use has changed for some reason. For example, consider the TZID "America/Indiana/Knox", which observed Central time (GMT-6:00) prior to October 27, 1991, and has currently observed Central time since April 2, 2006, but has observed Eastern time (GMT-5:00) between these two dates. This is denoted as follows

```xml
<timezone type="America/Indiana/Knox">
  <usesMetazone to="1991-10-27 07:00" mzone="America_Central" />
  <usesMetazone to="2006-04-02 07:00" from="1991-10-27 07:00" mzone="America_Eastern" />
  <usesMetazone from="2006-04-02 07:00" mzone="America_Central" />
</timezone>
```

Note that the dates and times are specified in UTC, not local time.

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

When formatting a date and time value using this data, an application can properly be able to display "Eastern Time" for dates between 1991-10-27 and 2006-04-02, but display "Central Time" for current dates. (See also _[Dates and Date Ranges](tr35.md#Date_Ranges)_.)

Metazones are used with the 'z', 'zzzz', 'v', and 'vvvv' date time pattern characters, and not with the 'Z', 'ZZZZ', 'VVVV' and other pattern characters for time zone formatting. For more information, see [Date Format Patterns](#Date_Format_Patterns).

Note that several of the CLDR metazone IDs are the same as TZID aliases provided by the _TZ time zone database_ and also included in ICU data. For example:
* “Japan” is a CLDR metazone ID (which has short ID “japa”), but also an alias to the TZID “Asia/Tokyo” (which has BCP 47 ID “jptyo”).
* “GMT” is a CLDR metazone ID (which has short ID “mgmt”), but also an alias to the TZID “Etc/GMT” (which has BCP 47 ID “gmt”).
In practice this is not an issue, since metazone IDs and TZIDs are never used in the same way in any data structure, or in the same APIs in a library such as ICU.

The `commonlyUsed` element is now deprecated. The CLDR committee has found it nearly impossible to obtain accurate and reliable data regarding which time zone abbreviations may be understood in a given territory, and therefore has changed to a simpler approach. Thus, if the short metazone form is available in a given locale, it is to be used for formatting regardless of the value of commonlyUsed. If a given short metazone form is known NOT to be understood in a given locale and the parent locale has this value such that it would normally be inherited, the inheritance of this value can be explicitly disabled by use of the 'no inheritance marker' as the value, which is 3 simultaneous empty set characters (U+2205).

## 6 <a name="Supplemental_Time_Zone_Data" href="#Supplemental_Time_Zone_Data">Supplemental Time Zone Data</a>

### 6.1 <a name="Metazones" href="#Metazones">Metazones</a>

```xml
<!ELEMENT metaZones (metazoneInfo?, mapTimezones?) >

<!ELEMENT metazoneInfo (timezone*) >

<!ELEMENT timezone (usesMetazone*) >
<!ATTLIST timezone type CDATA #REQUIRED >

<!ELEMENT usesMetazone EMPTY >
<!ATTLIST usesMetazone mzone NMTOKEN #REQUIRED >
<!ATTLIST usesMetazone from CDATA #IMPLIED >
<!ATTLIST usesMetazone to CDATA #IMPLIED >

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

The following subelement of `<metaZones>` provides a mapping from a single Unicode time zone id to metazones. For more information about metazones, see _[Time Zone Names](tr35-dates.md#Time_Zone_Names)_.

```xml
<metazoneInfo>
    <timezone type="Europe/Andorra">
        <usesMetazone mzone="Europe_Central" />
    </timezone>
    ....
    <timezone type="Asia/Yerevan">
        <usesMetazone to="1991-09-22 20:00" mzone="Yerevan" />
        <usesMetazone from="1991-09-22 20:00" mzone="Armenia" />
    </timezone>
    ....
```

The following subelement of `<metaZones>` specifies a mapping from a metazone to golden zones for each territory. For more information about golden zones, see _[Using Time Zone Names](tr35-dates.md#Using_Time_Zone_Names)_.

```xml
<mapTimezones type="metazones">
    <mapZone other="Acre" territory="001" type="America/Rio_Branco" />
    <mapZone other="Afghanistan" territory="001" type="Asia/Kabul" />
    <mapZone other="Africa_Central" territory="001" type="Africa/Maputo" />
    <mapZone other="Africa_Central" territory="BI" type="Africa/Bujumbura" />
    <mapZone other="Africa_Central" territory="BW" type="Africa/Gaborone" />
    ....
```

### 6.2 <a name="Windows_Zones" href="#Windows_Zones">Windows Zones</a>

```xml
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

The attributes otherVersion and typeVersion in `<mapTimezones>` specify the versions of two systems. In the example above, otherVersion="07dc0000" specifies the version of Windows time zone and typeVersion="2011n" specifies the version of Unicode time zone IDs. The attribute `territory="001"` in `<mapZone>` element indicates the long canonical Unicode time zone ID specified by the `type` attribute is used as the default mapping for the Windows TZID. For each unique Windows TZID, there must be exactly one `<mapZone>` element with `territory="001"`. `<mapZone>` elements other than `territory="001"` specify territory specific mappings. When multiple Unicode time zone IDs are available for a single territory, the value of the `type` attribute will be a list of Unicode time zone IDs delimited by space. In this case, the first entry represents the default mapping for the territory. The territory "ZZ" is used when a Unicode time zone ID is not associated with a specific territory.

**Note:** The long canonical Unicode time zone ID might be deprecated in the tz database [[Olson](tr35.md#Olson)]. For example, CLDR uses "Asia/Culcutta" as the long canonical time zone ID for Kolkata, India. The same ID was moved to 'backward' file and replaced with a new ID "Asia/Kolkata" in the tz database. Therefore, if you want to get an equivalent Windows TZID for a zone ID in the tz database, you have to resolve the long canonical Unicode time zone ID (e.g. "Asia/Culcutta") for the zone ID (e.g. "Asia/Kolkata"). For more details, see [Time Zone Identifiers](tr35.md#Time_Zone_Identifiers).

**Note:** Not all Unicode time zones have equivalent Windows TZID mappings. Also, not all Windows TZIDs have equivalent Unicode time zones. For example, there is no equivalent Windows zone for Unicode time zone "Australia/Lord_Howe", and there is no equivalent Unicode time zone for Windows zone "E. Europe Standard Time" (as of CLDR 25 release).

### 6.3 <a name="Primary_Zones" href="#Primary_Zones">Primary Zones</a>

```xml
<!ELEMENT primaryZones ( primaryZone* ) >
<!ELEMENT primaryZone ( #PCDATA ) >
<!ATTLIST primaryZone iso3166 NMTOKEN #REQUIRED >
```

This element is for data that is used to format a time zone’s generic location name. Each `<primaryZone>` element specifies the dominant zone for a region; this zone should use the region name for its generic location name even though there are other canonical zones available in the same region. For example, Asia/Shanghai is displayed as "China Time", instead of "Shanghai Time". Sample data:

```xml
<primaryZones>
    <primaryZone iso3166="CL">America/Santiago</primaryZone>
    <primaryZone iso3166="CN">Asia/Shanghai</primaryZone>
    <primaryZone iso3166="DE">Europe/Berlin</primaryZone>
    …
```

This information was previously specified by the LDML `<singleCountries>` element under each locale’s `<timeZoneNames>` element. However, that approach had inheritance issues, and the data is not really locale-specific anyway.

## 7 <a name="Using_Time_Zone_Names" href="#Using_Time_Zone_Names">Using Time Zone Names</a>

There are three main types of formats for zone identifiers: GMT, generic (wall time), and standard/daylight. Standard and daylight are equivalent to a particular offset from GMT, and can be represented by a GMT offset as a fallback. In general, this is not true for the generic format, which is used for picking timezones or for conveying a timezone for specifying a recurring time (such as a meeting in a calendar). For either purpose, a GMT offset would lose information.

### 7.1 <a name="Time_Zone_Format_Terminology" href="#Time_Zone_Format_Terminology">Time Zone Format Terminology</a>

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

Note: A generic location format is constructed by a part of time zone ID representing an exemplar city name or its country as the final fallback. However, there are Unicode time zones which are not associated with any locations, such as "Etc/GMT+5" and "PST8PDT". Although the date format pattern "VVVV" specifies the generic location format, but it displays localized GMT format for these. Some of these time zones observe daylight saving time, so the result (localized GMT format) may change depending on input date. For generating a list for user selection of time zone with format "VVVV", these non-location zones should be excluded.

**Specific non-location format:** Reflects a specific standard or daylight time, which may or may not be the wall time. For example, "10 am Pacific Standard Time" will be GMT-8 in the winter and in the summer.

* "Pacific Standard Time" (long)
* "PST" (short)
* "Pacific Daylight Time" (long)
* "PDT" (short)

**Localized GMT format:** A constant, specific offset from GMT (or UTC), which may be in a translated form. There are two styles for this. The first is used when there is an explicit non-zero offset from GMT; this style is specified by the `<gmtFormat>` element and `<hourFormat>` element. The long format always uses 2-digit hours field and minutes field, with optional 2-digit seconds field. The short format is intended for the shortest representation and uses hour fields without leading zero, with optional 2-digit minutes and seconds fields. The digits used for hours, minutes and seconds fields in this format are the locale's default decimal digits:

* "GMT+03:30" (long)
* "GMT+3:30" (short)
* "UTC-03.00" (long)
* "UTC-3" (short)
* "Гринуич+03:30" (long)

Otherwise (when the offset from GMT is zero, referring to GMT itself) the style specified by the `<gmtZeroFormat>` element is used:

* "GMT"
* "UTC"
* "Гринуич"

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

For example, for America_Pacific the preferred zone for Canada is America/Vancouver, and the preferred zone for Mexico is America/Tijuana. The golden zone is America/Los_Angeles, which is also the preferred zone for any other country.

```xml
<mapZone other="America_Pacific" territory="001" type="America/Los_Angeles" />
<mapZone other="America_Pacific" territory="CA" type="America/Vancouver" />
<mapZone other="America_Pacific" territory="MX" type="America/Tijuana" />
```

**<a name="fallbackFormat" href="#fallbackFormat">fallbackFormat:</a>** a formatting string such as "{1} ({0})", where {1} is the metazone, and {0} is the country or city.

**regionFormat:** a formatting string such as "{0} Time", where {0} is the country or city.

### 7.2 <a name="Time_Zone_Goals" href="#Time_Zone_Goals">Goals</a>

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
     * falling back to the localized exemplar city for the unknown zone (Etc/Unknown), for example "Unknown City" for English
   * VVVV - generic location
     * falling back to long localized GMT

The following process is used for the particular formats, with the fallback rules as above.

Some of the examples are drawn from real data, while others are for illustration. For illustration the region format is "Hora de {0}". The fallback format in the examples is "{1} ({0})", which is what is in root.

1. In **all** cases, first canonicalize the _TZ_ ID according to the Unicode Locale Extension _type_ mapping data (see [Time Zone Identifiers](tr35.md#Time_Zone_Identifiers) for more details). Use that canonical TZID in each of the following steps.
   * America/Atka → America/Adak
   * Australia/ACT → Australia/Sydney

2. For the localized GMT format, use the gmtFormat (such as "GMT{0}" or "HMG{0}") with the hourFormat (such as "+HH:mm;-HH:mm" or "+HH.mm;-HH.mm").
   * America/Los_Angeles → "GMT-08:00" // standard time
   * America/Los_Angeles → "HMG-07:00" // daylight time
   * Etc/GMT+3 → "GMT-03.00" // note that _TZ_ TZIDs have inverse polarity!

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
5. For the generic location format:
   1. From the TZDB get the country code for the zone, and determine whether there is only one timezone in the country. If there is only one timezone or if the zone id is in the `<primaryZones>` list, format the country name with the _regionFormat_, and return it.
      * Examples:
        * Europe/Rome → IT → "Italy Time" // for English
        * Asia/Shanghai → CN → "China Time" // Asia/Shanghai is the _primaryZone_ for China
        * Africa/Monrovia → LR → "Hora de Liberja"
        * America/Havana → CU → "Hora de CU" // if CU is not localized
   2. Otherwise format the exemplar city with the _regionFormat_, and return it.
      1. America/Buenos_Aires → "Buenos Aires Time"

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

### 7.3 <a name="Time_Zone_Parsing" href="#Time_Zone_Parsing">Parsing</a>

In parsing, an implementation will be able to either determine the zone id, or a simple offset from GMT for anything formatting according to the above process.

The following is a sample process for how this might be done. It is only a sample; implementations may use different methods for parsing.

The sample describes the parsing of a zone as if it were an isolated string. In implementations, the zone may be mixed in with other data (like the time), so the parsing actually has to look for the longest match, and then allow the remaining text to be parsed for other content. That requires certain adaptions to the following process.

1. Start with a string S.
2. If S matches ISO 8601 time zone format, return it.
   * For example, "-0800" (RFC 822), "-08:00" (ISO 8601) => Etc/GMT+8
3. If S matches the English or localized GMT format, return the corresponding TZID
   * Matching should be lenient. Thus allow for the number formats like: 03, 3, 330, 3:30, 33045 or 3:30:45. Allow +, -, or nothing. Allow spaces after GMT, +/-, and before number. Allow non-Latin numbers. Allow UTC or UT (per RFC 788) as synonyms for GMT ("GMT", "UT", "UTC" are global formats, always allowed in parsing regardless of locale).
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

Parsing can be more lenient than the above, allowing for different spacing, punctuation, or other variation. A stricter parse would check for consistency between the xxx portions above and the rest, so "Pacific Standard Time (India)" would give an error.

Using this process, a correct parse will roundtrip the location format (VVVV) back to the canonical zoneid.

  * Australia/ACT → Australia/Sydney → “Sydney (Australia)” → Australia/Sydney

The GMT formats (Z and ZZZZ) will return back an offset, and thus lose the original canonical zone id.

  * Australia/ACT → Australia/Sydney → "GMT+11:00" → GMT+11

The daylight and standard time formats, and the non-location formats (z, zzzz, v, and vvvv) may either roundtrip back to the original canonical zone id, to a zone in the same metazone that time, or to just an offset, depending on the available translation data. Thus:

  * Australia/ACT → Australia/Sydney → "GMT+11:00" → GMT+11
  * PST8PDT → America/Los_Angeles → “PST” → America/Los_Angeles
  * America/Vancouver → “Pacific Time (Canada)” → America/Vancouver

## 8 <a name="Date_Format_Patterns" href="#Date_Format_Patterns">Date Format Patterns</a>

A date pattern is a character string consisting of two types of elements:

* _Pattern fields_, which repeat a specific _pattern character_ one or more times. These fields are replaced with date and time data from a calendar when formatting, or used to generate data for a calendar when parsing. Currently, A..Z and a..z are reserved for use as pattern characters (unless they are quoted, see next item). The pattern characters currently defined, and the meaning of different fields lengths for then, are listed in the Date Field Symbol Table below.
* Literal text, which is output as-is when formatting, and must closely match when parsing. Literal text can include:
  * Any characters other than A..Z and a..z, including spaces and punctuation.
  * Any text between single vertical quotes ('xxxx'), which may include A..Z and a..z as literal text.
  * Two adjacent single vertical quotes (''), which represent a literal single quote, either inside or outside quoted text.

The following are examples:

###### Table: <a name="Date_Format_Pattern_Examples" href="#Date_Format_Pattern_Examples">Date Format Pattern Examples</a>

| Pattern | Result (in a particular locale) |
| ------- | ------------------------------- |
| yyyy.MM.dd G 'at' HH:mm:ss zzz | 1996.07.10 AD at 15:08:56 PDT |
| EEE, MMM d, ''yy | Wed, July 10, '96 |
| h:mm a | 12:08 PM |
| hh 'o''clock' a, zzzz | 12 o'clock PM, Pacific Daylight Time |
| K:mm a, z | 0:00 PM, PST |
| yyyyy.MMMM.dd GGG hh:mm aaa | 01996.July.10 AD 12:08 PM |

_When parsing using a pattern, a lenient parse should be used; see [Parsing Dates and Times](#Parsing_Dates_Times)._

```xml
<!ATTLIST pattern numbers CDATA #IMPLIED >
```

The `numbers` attribute is used to indicate that numeric quantities in the pattern are to be rendered using a numbering system other than the default numbering system defined for the given locale. The attribute can be in one of two forms. If the alternate numbering system is intended to apply to ALL numeric quantities in the pattern, then simply use the numbering system ID as found in [Numbering Systems](tr35-numbers.md#Numbering_Systems). To apply the alternate numbering system only to a single field, the syntax `<letter>=<numberingSystem>` can be used one or more times, separated by semicolons.

Examples:

```xml
<pattern numbers="hebr">dd/mm/yyyy</pattern>
<!-- Use Hebrew numerals to represent numbers in the Hebrew calendar, where "latn" numbering system is the default -->

<pattern numbers="y=hebr">dd/mm/yyyy</pattern>
<!-- Same as above, except that ONLY the year value would be rendered in Hebrew -->

<pattern numbers="d=thai;m=hans;y=deva">dd/mm/yyyy</pattern>
<!-- Illustrates use of multiple numbering systems for a single pattern. -->
```

**Pattern fields and the Date Field Symbol Table**

The Date Field Symbol Table below shows the pattern characters (Sym.) and associated fields used in date patterns. The length of the pattern field is related to the length and style used to format the data item. For numeric-only fields, the field length typically indicates the minimum number of digits that should be used to display the value (zero-padding as necessary). As an example using pattern character ‘H’ for hour (24-hour cycle) and values 5 and 11, a field “H” should produce formatted results “5” and “11” while a field “HH” should produce formatted results “05” and “11”. For alphanumeric fields (such as months) and alphabetic-only fields (such as era names), the relationship between field length and formatted result may be more complex. Typically this is as follows:

<!-- HTML: spanned rows, spanned columns -->
 <table>
    <tr><th>Pattern field length</th><th>Typical style, alphanumeric item</th><th>Typical style, alpha-only item</th></tr>
    <tr><td>1</td><td>Numeric, 1-2 digits (e.g. M)</td><td rowspan="3">Abbreviated (e.g. E, EE, EEE)</td></tr>
    <tr><td>2</td><td>Numeric, 2 digits (e.g. MM)</td></tr>
    <tr><td>3</td><td>Abbreviated (e.g. MMM)</td></tr>
    <tr><td>4</td><td colspan="2">Wide / Long / Full (e.g. MMMM, EEEE)</td></tr>
    <tr><td>5</td><td colspan="2">Narrow (e.g. MMMMM, EEEEE)<br/>(The counter-intuitive use of 5 letters for this is forced by backwards compatibility)</td></tr>
</table>

Notes for the table below:

* Any sequence of pattern characters other than those listed below is invalid. Invalid pattern fields should be handled for formatting and parsing as described in [Handling Invalid Patterns](tr35.md#Invalid_Patterns).
* The examples in the table below are merely illustrative and may not reflect current actual data.

###### Table: <a name="Date_Field_Symbol_Table" href="#Date_Field_Symbol_Table">Date Field Symbol Table</a>

<!-- HTML: spanned rows, spanned columns, vertical header cells -->
<table><tbody>
<tr><th>Field<br/>Type</th><th>Sym.</th><th>Field<br/>Patterns</th><th>Examples</th><th colspan="2">Description</th></tr>

<!-- == == == ERA == == == -->
<tr><th rowspan="3"><a name="dfst-era" href="#dfst-era">era</a></th><td rowspan="3">G</td><td>G..GGG</td><td>AD<br/>[variant: CE]</td><td>Abbreviated</td><td rowspan="3">Era name. Era string for the current date.</td></tr>
    <tr><td>GGGG</td><td>Anno Domini<br/>[variant: Common Era]</td><td>Wide</td></tr>
    <tr><td>GGGGG</td><td>A</td><td>Narrow</td></tr>

<!-- == == == YEAR == == == -->
<tr><th rowspan="15"><a name="dfst-year" href="#dfst-year">year</a><a name="Year_Length_Examples"></a></th><td rowspan="5">y</td><td>y</td><td>2, 20, 201, 2017, 20173</td>
        <td rowspan="5" colspan="2">Calendar year (numeric). In most cases the length of the y field specifies the minimum number of digits to display, zero-padded as necessary; more digits will be displayed if needed to show the full year.
                                    However, “yy” requests just the two low-order digits of the year, zero-padded as necessary. For most use cases, “y” or “yy” should be adequate.</td></tr>
    <tr><td>yy</td><td>02, 20, 01, 17, 73</td></tr>
    <tr><td>yyy</td><td>002, 020, 201, 2017, 20173</td></tr>
    <tr><td>yyyy</td><td>0002, 0020, 0201, 2017, 20173</td></tr>
    <tr><td>yyyyy+</td><td>...</td></tr>
    <!--  Y  -->
    <tr><td rowspan="5">Y</td><td>Y</td><td>2, 20, 201, 2017, 20173</td>
        <td rowspan="5" colspan="2">Year in “Week of Year” based calendars in which the year transition occurs on a week boundary; may differ from calendar year ‘y’ near a year transition.
                                    This numeric year designation is used in conjunction with pattern character ‘w’ in the ISO year-week calendar as defined by ISO 8601, but can be used in non-Gregorian based calendar systems where week date processing is desired.
                                    The field length is interpreted in the same was as for ‘y’; that is, “yy” specifies use of the two low-order year digits, while any other field length specifies a minimum number of digits to display.</td></tr>
    <tr><td>YY</td><td>02, 20, 01, 17, 73</td></tr>
    <tr><td>YYY</td><td>002, 020, 201, 2017, 20173</td></tr>
    <tr><td>YYYY</td><td>0002, 0020, 0201, 2017, 20173</td></tr>
    <tr><td>YYYYY+</td><td>...</td></tr>
    <!--  u  -->
    <tr><td>u</td><td>u+</td><td>4601</td>
        <td colspan="2">Extended year (numeric). This is a single number designating the year of this calendar system, encompassing all supra-year fields.
                        For example, for the Julian calendar system, year numbers are positive, with an era of BCE or CE. An extended year value for the Julian calendar system assigns positive values to CE years and negative values to BCE years, with 1 BCE being year 0.
                        For ‘u’, all field lengths specify a minimum number of digits; there is no special interpretation for “uu”.</td></tr>
    <!--  U  -->
    <tr><td rowspan="3">U</td><td>U..UUU</td><td>甲子</td><td>Abbreviated</td>
        <td rowspan="3">Cyclic year name. Calendars such as the Chinese lunar calendar (and related calendars) and the Hindu calendars use 60-year cycles of year names.
                        If the calendar does not provide cyclic year name data, or if the year value to be formatted is out of the range of years for which cyclic name data is provided, then numeric formatting is used (behaves like 'y').<br/>
                        Currently the data only provides abbreviated names, which will be used for all requested name widths.</td></tr>
    <tr><td>UUUU</td><td>甲子 [for now]</td><td>Wide</td></tr>
    <tr><td>UUUUU</td><td>甲子 [for now]</td><td>Narrow</td></tr>
    <!--  r  -->
    <tr><td>r</td><td>r+</td><td>2017</td>
        <td colspan="2">Related Gregorian year (numeric).
                        For non-Gregorian calendars, this corresponds to the extended Gregorian year in which the calendar’s year begins.
                        Related Gregorian years are often displayed, for example, when formatting dates in the Japanese calendar — e.g. “2012(平成24)年1月15日” — or in the Chinese calendar — e.g. “2012壬辰年腊月初四”.
                        The related Gregorian year is usually displayed using the "latn" numbering system, regardless of what numbering systems may be used for other parts of the formatted date.
                        If the calendar’s year is linked to the solar year (perhaps using leap months), then for that calendar the ‘r’ year will always be at a fixed offset from the ‘u’ year.
                        For the Gregorian calendar, the ‘r’ year is the same as the ‘u’ year. For ‘r’, all field lengths specify a minimum number of digits; there is no special interpretation for “rr”.</td></tr>

<!-- == == == QUARTER == == == -->
<tr><th rowspan="10"><a name="dfst-quarter" id="dfst-quarter" href="#dfst-quarter">quarter</a></th><td rowspan="5">Q</td><td>Q</td><td>2</td><td>Numeric: 1 digit</td><td rowspan="5">Quarter number/name.</td></tr>
    <tr><td>QQ</td><td>02</td><td>Numeric: 2 digits + zero pad</td></tr>
    <tr><td>QQQ</td><td>Q2</td><td>Abbreviated</td></tr>
    <tr><td>QQQQ</td><td>2nd quarter</td><td>Wide</td></tr>
    <tr><td>QQQQQ</td><td>2</td><td>Narrow</td></tr>
    <!--  q  -->
    <tr><td rowspan="5">q</td><td>q</td><td>2</td><td>Numeric: 1 digit</td><td rowspan="5"><b>Stand-Alone</b> Quarter number/name.</td></tr>
    <tr><td>qq</td><td>02</td><td>Numeric: 2 digits + zero pad</td></tr>
    <tr><td>qqq</td><td>Q2</td><td>Abbreviated</td></tr>
    <tr><td>qqqq</td><td>2nd quarter</td><td>Wide</td></tr>
    <tr><td>qqqqq</td><td>2</td><td>Narrow</td></tr>

<!-- == == == MONTH == == == -->
<tr><th rowspan="11"><a name="dfst-month" id="dfst-month" href="#dfst-month">month</a></th><td rowspan="5">M</td><td>M</td><td>9, 12</td><td>Numeric: minimum digits</td><td rowspan="5"><b>Format</b> style month number/name: The format style name is an additional form of the month name (besides the stand-alone style) that can be used in contexts where it is different than the stand-alone form. For example, depending on the language, patterns that combine month with day-of month (e.g. "d MMMM") may require the month to be in genitive form. See discussion of <a href="#months_days_quarters_eras">month element</a>. If a separate form is not needed, the format and stand-alone forms can be the same.</td></tr>
    <tr><td>MM</td><td>09, 12</td><td>Numeric: 2 digits, zero pad if needed</td></tr>
    <tr><td>MMM</td><td>Sep</td><td>Abbreviated</td></tr>
    <tr><td>MMMM</td><td>September</td><td>Wide</td></tr>
    <tr><td>MMMMM</td><td>S</td><td>Narrow</td></tr>
    <!--  L  -->
    <tr><td rowspan="5">L</td><td>L</td><td>9, 12</td><td>Numeric: minimum digits</td><td rowspan="5"><b>Stand-Alone</b> month number/name: For use when the month is displayed by itself, and in any other date pattern (e.g. just month and year, e.g. "LLLL y") that shares the same form of the month name. For month names, this is typically the nominative form. See discussion of <a href="#months_days_quarters_eras">month element</a>.</td></tr>
    <tr><td>LL</td><td>09, 12</td><td>Numeric: 2 digits, zero pad if needed</td></tr>
    <tr><td>LLL</td><td>Sep</td><td>Abbreviated</td></tr>
    <tr><td>LLLL</td><td>September</td><td>Wide</td></tr>
    <tr><td>LLLLL</td><td>S</td><td>Narrow</td></tr>
    <!--  l  -->
    <tr><td>l</td><td>l</td><td>[nothing]</td>
        <td colspan="2">This pattern character is deprecated, and should be ignored in patterns.
                        It was originally intended to be used in combination with M to indicate placement of the symbol for leap month in the Chinese calendar.
                        Placement of that marker is now specified using locale-specific &lt;monthPatterns&gt; data, and formatting and parsing of that marker should be handled as part of supporting the regular M and L pattern characters.</td></tr>

<!-- == == == WEEK == == == -->
<tr><th rowspan="3"><a name="dfst-week" id="dfst-week" href="#dfst-week">week</a></th><td rowspan="2">w</td><td>w</td><td>8, 27</td><td>Numeric: minimum digits</td><td rowspan="2">Week of Year (numeric). When used in a pattern with year, use ‘Y’ for the year field instead of ‘y’.</td></tr>
    <tr><td>ww</td><td>08, 27</td><td>Numeric: 2 digits, zero pad if needed</td></tr>
    <!--  W  -->
    <tr><td>W</td><td>W</td><td>3</td><td>Numeric: 1 digit</td><td>Week of Month (numeric)</td></tr>

<!-- == == == DAY == == == -->
<tr><th rowspan="5"><a name="dfst-day" id="dfst-day" href="#dfst-day">day</a></th><td rowspan="2">d</td><td>d</td><td>1</td><td>Numeric: minimum digits</td><td rowspan="2">Day of month (numeric).</td></tr>
    <tr><td>dd</td><td>01</td><td>Numeric: 2 digits, zero pad if needed</td></tr>
    <tr><td>D</td><td>D...DDD</td><td>345</td><td colspan="2">Day of year (numeric). The field length specifies the minimum number of digits, with zero-padding as necessary.</td></tr>
    <tr><td>F</td><td>F</td><td>2</td><td colspan="2">Day of Week in Month (numeric). The example is for the 2nd Wed in July</td></tr>
    <tr><td>g</td><td>g+</td><td>2451334</td>
        <td colspan="2">Modified Julian day (numeric).
                        This is different from the conventional Julian day number in two regards.
                        First, it demarcates days at local zone midnight, rather than noon GMT.
                        Second, it is a local number; that is, it depends on the local time zone.
                        It can be thought of as a single number that encompasses all the date-related fields.
                        The field length specifies the minimum number of digits, with zero-padding as necessary.</td></tr>

<!-- == == == WEEKDAY == == == -->
<tr><th rowspan="15"><a name="dfst-weekday" id="dfst-weekday" href="#dfst-weekday">week<br/>day</a></th><td rowspan="4">E</td><td>E..EEE</td><td>Tue</td><td>Abbreviated</td><td rowspan="4">Day of week name, format style.</td></tr>
    <tr><td>EEEE</td><td>Tuesday</td><td>Wide</td></tr>
    <tr><td>EEEEE</td><td>T</td><td>Narrow</td></tr>
    <tr><td>EEEEEE</td><td>Tu</td><td>Short</td></tr>
    <!--  e  -->
    <tr><td rowspan="6">e</td><td>e</td><td>2</td><td>Numeric: 1 digit</td>
        <td rowspan="6">Local day of week number/name, format style.
                        Same as E except adds a numeric value that will depend on the local starting day of the week.
                        For this example, Monday is the first day of the week.</td></tr>
    <tr><td>ee</td><td>02</td><td>Numeric: 2 digits + zero pad</td></tr>
    <tr><td>eee</td><td>Tue</td><td>Abbreviated</td></tr>
    <tr><td>eeee</td><td>Tuesday</td><td>Wide</td></tr>
    <tr><td>eeeee</td><td>T</td><td>Narrow</td></tr>
    <tr><td>eeeeee</td><td>Tu</td><td>Short</td></tr>
    <!--  c  -->
    <tr><td rowspan="5">c</td><td>c..cc</td><td>2</td><td>Numeric: 1 digit</td><td rowspan="5"><b>Stand-Alone</b> local day of week number/name.</td></tr>
    <tr><td>ccc</td><td>Tue</td><td>Abbreviated</td></tr>
    <tr><td>cccc</td><td>Tuesday</td><td>Wide</td></tr>
    <tr><td>ccccc</td><td>T</td><td>Narrow</td></tr>
    <tr><td>cccccc</td><td>Tu</td><td>Short</td></tr>

<!-- == == == PERIOD == == == -->
<tr><th rowspan="9"><a name="dfst-period" id="dfst-period" href="#dfst-period">period</a></th><td rowspan="3">a</td><td>a..aaa</td><td>am. [e.g. 12 am.]</td><td>Abbreviated</td>
        <td rowspan="3"><strong>AM, PM<br/></strong>May be upper or lowercase depending on the locale and other options.
                                                    The wide form may be the same as the short form if the “real” long form (eg <em>ante meridiem</em>) is not customarily used.
                                                    The narrow form must be unique, unlike some other fields.
                                                    See also Section 9 <a href="#Parsing_Dates_Times">Parsing Dates and Times</a>.</td></tr>
    <tr><td>aaaa</td><td>am. [e.g. 12 am.]</td><td>Wide</td></tr>
    <tr><td>aaaaa</td><td>a [e.g. 12a]</td><td>Narrow</td></tr>
    <!--  b  -->
    <tr><td rowspan="3">b</td><td>b..bbb</td><td>mid. [e.g. 12 mid.]</td><td>Abbreviated</td>
        <td rowspan="3"><strong>am, pm, noon, midnight</strong><br/>May be upper or lowercase depending on the locale and other options.
                        If the locale doesn't have the notion of a unique "noon" = 12:00, then the PM form may be substituted.
                        Similarly for "midnight" = 00:00 and the AM form.
                        The narrow form must be unique, unlike some other fields.</td></tr>
    <tr><td>bbbb</td><td>midnight<br/>[e.g. 12 midnight]</td><td>Wide</td></tr>
    <tr><td>bbbbb</td><td>md [e.g. 12 md]</td><td>Narrow</td></tr>
    <!--  B  -->
    <tr><td rowspan="3">B</td><td>B..BBB</td><td>at night<br/>[e.g. 3:00 at night]</td><td>Abbreviated</td>
        <td rowspan="3"><strong>flexible day periods</strong><br/>
                        May be upper or lowercase depending on the locale and other options.
                        Often there is only one width that is customarily used.</td></tr>
    <tr><td>BBBB</td><td>at night<br/>[e.g. 3:00 at night]</td><td>Wide</td></tr>
    <tr><td>BBBBB</td><td>at night<br/>[e.g. 3:00 at night]</td><td>Narrow</td></tr>

<!-- == == == HOUR == == == -->
<tr><th rowspan="22"><a name="dfst-hour" id="dfst-hour" href="#dfst-hour">hour</a></th><td rowspan="2">h</td><td>h</td><td>1, 12</td><td>Numeric: minimum digits</td>
        <td rowspan="2">Hour [1-12]. When used in skeleton data or in a skeleton passed in an API for flexible date pattern generation, it should match the 12-hour-cycle format preferred by the locale (h or K); it should not match a 24-hour-cycle format (H or k).</td></tr>
<tr><td>hh</td><td>01, 12</td><td>Numeric: 2 digits, zero pad if needed</td></tr>
    <!--  H  -->
    <tr><td rowspan="2">H</td><td>H</td><td>0, 23</td><td>Numeric: minimum digits</td>
        <td rowspan="2">Hour [0-23]. When used in skeleton data or in a skeleton passed in an API for flexible date pattern generation, it should match the 24-hour-cycle format preferred by the locale (H or k); it should not match a 12-hour-cycle format (h or K).</td></tr>
    <tr><td>HH</td><td>00, 23</td><td>Numeric: 2 digits, zero pad if needed</td></tr>
    <!--  K  -->
    <tr><td rowspan="2">K</td><td>K</td><td>0, 11</td><td>Numeric: minimum digits</td>
        <td rowspan="2">Hour [0-11]. When used in a skeleton, only matches K or h, see above.</td></tr>
    <tr><td>KK</td><td>00, 11</td><td>Numeric: 2 digits, zero pad if needed</td></tr>
    <!--  k  -->
    <tr><td rowspan="2">k</td><td>k</td><td>1, 24</td><td>Numeric: minimum digits</td>
        <td rowspan="2">Hour [1-24]. When used in a skeleton, only matches k or H, see above.</td></tr>
    <tr><td>kk</td><td>01, 24</td><td>Numeric: 2 digits, zero pad if needed</td></tr>
    <!--  j  -->
    <tr><td rowspan="6">j</td><td>j</td><td>8<br/>8 AM<br/>13<br/>1 PM</td><td>Numeric hour (minimum digits), abbreviated dayPeriod if used</td>
        <td rowspan="6"><em><strong>Input skeleton symbol</strong></em><br/>
                        It must not occur in pattern or skeleton data.
                        Instead, it is reserved for use in skeletons passed to APIs doing flexible date pattern generation.
                        In such a context, it requests the preferred hour format for the locale (h, H, K, or k), as determined by the <strong>preferred</strong> attribute of the <strong>hours</strong> element in supplemental data.
                        In the implementation of such an API, 'j' must be replaced by h, H, K, or k before beginning a match against availableFormats data.<br/>
                        Note that use of 'j' in a skeleton passed to an API is the only way to have a skeleton request a locale's preferred time cycle type (12-hour or 24-hour).</td></tr>
    <tr><td>jj</td><td>08<br/>08 AM<br/>13<br/>01 PM</td><td>Numeric hour (2 digits, zero pad if needed), abbreviated dayPeriod if used</td></tr>
    <tr><td>jjj</td><td>8<br/>8 A.M.<br/>13<br/>1 P.M.</td><td>Numeric hour (minimum digits), wide dayPeriod if used</td></tr>
    <tr><td>jjjj</td><td>08<br/>08 A.M.<br/>13<br/>01 P.M.</td><td>Numeric hour (2 digits, zero pad if needed), wide dayPeriod if used</td></tr>
    <tr><td>jjjjj</td><td>8<br/>8a<br/>13<br/>1p</td><td>Numeric hour (minimum digits), narrow dayPeriod if used</td></tr>
    <tr><td>jjjjjj</td><td>08<br/>08a<br/>13<br/>01p</td><td>Numeric hour (2 digits, zero pad if needed), narrow dayPeriod if used</td></tr>
    <!--  J  -->
    <tr><td rowspan="2">J</td><td>J</td><td>8<br/>8</td><td>Numeric hour (minimum digits)</td>
        <td rowspan="2"><em><strong>Input skeleton symbol</strong></em><br/>It must not occur in pattern or skeleton data.
                        Instead, it is reserved for use in skeletons passed to APIs doing flexible date pattern generation.
                        In such a context, like 'j', it requests the preferred hour format for the locale (h, H, K, or k), as determined by the <strong>preferred</strong> attribute of the <strong>hours</strong> element in supplemental data.
                        However, unlike 'j', it requests no dayPeriod marker such as “am/pm” (it is typically used where there is enough context that that is not necessary).
                        For example, with "jmm", 18:00 could appear as “6:00 PM”, while with "Jmm", it would appear as “6:00” (no PM).</td></tr>
    <tr><td>JJ</td><td>08<br/>08</td><td>Numeric hour (2 digits, zero pad if needed)</td></tr>
    <!--  C  -->
    <tr><td rowspan="6">C</td><td>C</td><td>8<br/>8 (morning)</td><td>Numeric hour (minimum digits), abbreviated dayPeriod if used</td>
        <td rowspan="6"><em><strong>Input skeleton symbol</strong></em><br/>It must not occur in pattern or skeleton data.
                        Instead, it is reserved for use in skeletons passed to APIs doing flexible date pattern generation.
                        In such a context, like 'j', it requests the preferred hour format for the locale.
                        However, unlike 'j', it can also select formats such as hb or hB, since it is based not on the <strong>preferred</strong> attribute of the <strong>hours</strong> element in supplemental data, but instead on the first element of the <strong>allowed</strong> attribute (which is an ordered preferrence list).
                        For example, with "Cmm", 18:00 could appear as “6:00 in the afternoon”.</td></tr>
    <tr><td>CC</td><td>08<br/>08 (morning)</td><td>Numeric hour (2 digits, zero pad if needed), abbreviated dayPeriod if used</td></tr>
    <tr><td>CCC</td><td>8<br/>8 in the morning</td><td>Numeric hour (minimum digits), wide dayPeriod if used</td></tr>
    <tr><td>CCCC</td><td>08<br/>08 in the morning</td><td>Numeric hour (2 digits, zero pad if needed), wide dayPeriod if used</td></tr>
    <tr><td>CCCCC</td><td>8<br/>8 (morn.)</td><td>Numeric hour (minimum digits), narrow dayPeriod if used</td></tr>
    <tr><td>CCCCCC</td><td>08<br/>08 (morn.)</td><td>Numeric hour (2 digits, zero pad if needed), narrow dayPeriod if used</td></tr>

<!-- == == == MINUTE == == == -->
<tr><th rowspan="2"><a name="dfst-minute" id="dfst-minute" href="#dfst-minute">minute</a></th><td rowspan="2">m</td><td>m</td><td>8, 59</td><td>Numeric: minimum digits</td>
        <td rowspan="2">Minute (numeric). Truncated, not rounded.<br/></td></tr>
    <tr><td>mm</td><td>08, 59</td><td>Numeric: 2 digits, zero pad if needed</td></tr>

<!-- == == == SECOND == == == -->
<tr><th rowspan="4"><a name="dfst-second" id="dfst-second" href="#dfst-second">second</a></th><td rowspan="2">s</td><td>s</td><td>8, 12</td><td>Numeric: minimum digits</td>
        <td rowspan="2">Second (numeric). Truncated, not rounded.<br/></td></tr>
    <tr><td>ss</td><td>08, 12</td><td>Numeric: 2 digits, zero pad if needed</td></tr>
    <tr><td>S</td><td>S+</td><td>3456</td>
        <td colspan="2">Fractional Second (numeric).
                        Truncates, like other numeric time fields, but in this case to the number of digits specified by the field length.
                        (Example shows display using pattern SSSS for seconds value 12.34567)</td></tr>
    <tr><td>A</td><td>A+</td><td>69540000</td>
        <td colspan="2">Milliseconds in day (numeric).
                        This field behaves <i>exactly</i> like a composite of all time-related fields, not including the zone fields.
                        As such, it also reflects discontinuities of those fields on DST transition days. On a day of DST onset, it will jump forward.
                        On a day of DST cessation, it will jump backward.
                        This reflects the fact that it must be combined with the offset field to obtain a unique local time value.
                        The field length specifies the minimum number of digits, with zero-padding as necessary.</td></tr>

<!-- == == == SEPARATOR == == == -->
<tr><th><a name="dfst-sep" id="dfst-sep" href="#dfst-sep">sep.</a></th><td>(none def., see note)</td><td></td><td></td>
        <td colspan="2">Time separator.<br/><br/><span class="note"><b>Note:</b>
                        In CLDR 26 the time separator pattern character was specified to be COLON.
                        This was withdrawn in CLDR 28 due to backward compatibility issues, and no time separator pattern character is currently defined.</span><br/><br/>
                        Like the use of "," in number formats, this character in a date pattern is replaced with the corresponding number symbol which may depend on the numbering system.
                        For more information, see <em><strong>Part 3: <a href="tr35-numbers.md#Contents">Numbers</a></strong>, Section 2.3 <a href="tr35-numbers.md#Number_Symbols">Number Symbols</a></em>.</td></tr>

<!-- == == == ZONE == == == -->
<tr><th rowspan="23"><a name="dfst-zone" id="dfst-zone" href="#dfst-zone">zone</a></th><td rowspan="2">z</td><td>z..zzz</td><td>PDT</td>
        <td colspan="2">The <i>short specific non-location format</i>. Where that is unavailable, falls back to the <i>short localized GMT format</i> ("O").</td></tr>
    <tr><td>zzzz</td><td>Pacific Daylight Time</td>
        <td colspan="2">The <i>long specific non-location format</i>.
                        Where that is unavailable, falls back to the <i>long localized GMT format</i> ("OOOO").</td></tr>
    <!--  Z  -->
    <tr><td rowspan="3">Z</td><td>Z..ZZZ</td><td>-0800</td>
        <td colspan="2">The <i>ISO8601 basic format</i> with hours, minutes and optional seconds fields.
                        The format is equivalent to RFC 822 zone format (when optional seconds field is absent).
                        This is equivalent to the "xxxx" specifier.</td></tr>
    <tr><td>ZZZZ</td><td>GMT-8:00</td>
        <td colspan="2">The <i>long localized GMT format</i>.
                        This is equivalent to the "OOOO" specifier.</td></tr>
    <tr><td>ZZZZZ</td><td>-08:00<br/>-07:52:58</td>
        <td colspan="2">The <i>ISO8601 extended format</i> with hours, minutes and optional seconds fields.
                        The ISO8601 UTC indicator "Z" is used when local time offset is 0.
                        This is equivalent to the "XXXXX" specifier.</td></tr>
    <!--  O  -->
    <tr><td rowspan="2">O</td><td>O</td><td>GMT-8</td><td colspan="2">The <i>short localized GMT format</i>.</td></tr>
    <tr><td>OOOO</td><td>GMT-08:00</td><td colspan="2">The <i>long localized GMT format</i>.</td></tr>
    <!--  v  -->
    <tr><td rowspan="2">v</td><td>v</td><td>PT</td>
        <td colspan="2">The <i>short generic non-location format</i>
                        Where that is unavailable, falls back to the <i>generic location format</i> ("VVVV"), then the <i>short localized GMT format</i> as the final fallback.</td></tr>
    <tr><td>vvvv</td><td>Pacific Time</td>
        <td colspan="2">The <i>long generic non-location format</i>.
                        Where that is unavailable, falls back to <i>generic location format</i> ("VVVV").</td></tr>
    <!--  V  -->
    <tr><td rowspan="4">V</td><td>V</td><td>uslax</td>
        <td colspan="2">The short time zone ID. Where that is unavailable, the special short time zone ID <i>unk</i> (Unknown Zone) is used.<br/>
                        <i><b>Note</b>: This specifier was originally used for a variant of the short specific non-location format, but it was deprecated in the later version of this specification.
                        In CLDR 23, the definition of the specifier was changed to designate a short time zone ID.</i></td></tr>
    <tr><td>VV</td><td>America/Los_Angeles</td><td colspan="2">The long time zone ID.</td></tr>
    <tr><td>VVV</td><td>Los Angeles</td>
        <td colspan="2">The exemplar city (location) for the time zone.
                        Where that is unavailable, the localized exemplar city name for the special zone <i>Etc/Unknown</i> is used as the fallback (for example, "Unknown City").</td></tr>
    <tr><td>VVVV</td><td>Los Angeles Time</td>
        <td colspan="2">The <i>generic location format</i>.
                        Where that is unavailable, falls back to the <i>long localized GMT format</i> ("OOOO"; Note: Fallback is only necessary with a GMT-style Time Zone ID, like Etc/GMT-830.)<br/>
                        This is especially useful when presenting possible timezone choices for user selection, since the naming is more uniform than the "v" format.</td></tr>
    <!--  X  -->
    <tr><td rowspan="5">X</td><td>X</td><td>-08<br/>+0530<br/>Z</td>
        <td colspan="2">The <i>ISO8601 basic format</i> with hours field and optional minutes field.
                        The ISO8601 UTC indicator "Z" is used when local time offset is 0. (The same as x, plus "Z".)</td></tr>
    <tr><td>XX</td><td>-0800<br/>Z</td>
        <td colspan="2">The <i>ISO8601 basic format</i> with hours and minutes fields.
                        The ISO8601 UTC indicator "Z" is used when local time offset is 0. (The same as xx, plus "Z".)</td></tr>
    <tr><td>XXX</td><td>-08:00<br/>Z</td>
        <td colspan="2">The <i>ISO8601 extended format</i> with hours and minutes fields.
                        The ISO8601 UTC indicator "Z" is used when local time offset is 0. (The same as xxx, plus "Z".)</td></tr>
    <tr><td>XXXX</td><td>-0800<br/>-075258<br/>Z</td>
        <td colspan="2">The <i>ISO8601 basic format</i> with hours, minutes and optional seconds fields.
                        The ISO8601 UTC indicator "Z" is used when local time offset is 0. (The same as xxxx, plus "Z".)<br/>
                        <i><b>Note</b>: The seconds field is not supported by the ISO8601 specification.</i></td></tr>
    <tr><td>XXXXX</td><td>-08:00<br/>-07:52:58<br/>Z</td>
        <td colspan="2">The <i>ISO8601 extended format</i> with hours, minutes and optional seconds fields.
                        The ISO8601 UTC indicator "Z" is used when local time offset is 0. (The same as xxxxx, plus "Z".)<br/>
                        <i><b>Note</b>: The seconds field is not supported by the ISO8601 specification.</i></td></tr>
    <!--  x  -->
    <tr><td rowspan="5">x</td><td>x</td><td>-08<br/>+0530<br/>+00</td>
        <td colspan="2">The <i>ISO8601 basic format</i> with hours field and optional minutes field. (The same as X, minus "Z".)</td></tr>
    <tr><td>xx</td><td>-0800<br/>+0000</td>
        <td colspan="2">The <i>ISO8601 basic format</i> with hours and minutes fields. (The same as XX, minus "Z".)</td></tr>
    <tr><td>xxx</td><td>-08:00<br/>+00:00</td>
        <td colspan="2">The <i>ISO8601 extended format</i> with hours and minutes fields. (The same as XXX, minus "Z".)</td></tr>
    <tr><td>xxxx</td><td>-0800<br/>-075258<br/>+0000</td>
        <td colspan="2">The <i>ISO8601 basic format</i> with hours, minutes and optional seconds fields. (The same as XXXX, minus "Z".)<br/>
                        <i><b>Note</b>: The seconds field is not supported by the ISO8601 specification.</i></td></tr>
    <tr><td>xxxxx</td><td>-08:00<br/>-07:52:58<br/>+00:00</td>
        <td colspan="2">The <i>ISO8601 extended format</i> with hours, minutes and optional seconds fields. (The same as XXXXX, minus "Z".)<br/>
                        <i><b>Note</b>: The seconds field is not supported by the ISO8601 specification.</i></td></tr>
</tbody></table>

### 8.1 <a name="Localized_Pattern_Characters" href="#Localized_Pattern_Characters">Localized Pattern Characters (deprecated)</a>

These are characters that can be used when displaying a date pattern to an end user. This can occur, for example, when a spreadsheet allows users to specify date patterns. Whatever is in the string is substituted one-for-one with the characters "GyMdkHmsSEDFwWahKzYeugAZvcLQqVUOXxr", with the above meanings. Thus, for example, if 'J' is to be used instead of 'Y' to mean Year (for Week of Year), then the string would be: "GyMdkHmsSEDFwWahKzJeugAZvcLQqVUOXxr".

This element is deprecated. It is recommended instead that a more sophisticated UI be used for localization, such as using icons to represent the different formats (and lengths) in the [Date Field Symbol Table](#Date_Field_Symbol_Table).

### 8.2 <a name="Date_Patterns_AM_PM" href="#Date_Patterns_AM_PM">AM / PM</a>

Even for countries where the customary date format only has a 24 hour format, both the am and pm localized strings must be present and must be distinct from one another. Note that as long as the 24 hour format is used, these strings will normally never be used, but for testing and unusual circumstances they must be present.

### 8.3 <a name="Date_Patterns_Eras" href="#Date_Patterns_Eras">Eras</a>

There are only two values for era in the Gregorian calendar, with two common naming conventions (here in abbreviated form for English): "BC" and "AD", or "BCE" and "CE". These values can be translated into other languages, like "a.C." and "d.C." for Spanish, but there are no other eras in the Gregorian calendar. Other calendars have different numbers of eras. Care should be taken when translating the era names for a specific calendar.

### 8.4 <a name="Date_Patterns_Week_Of_Year" href="#Date_Patterns_Week_Of_Year">Week of Year</a>

Values calculated for the Week of Year field range from 1 to 53 for the Gregorian calendar (they may have different ranges for other calendars). Week 1 for a year is the first week that contains at least the specified minimum number of days from that year. Weeks between week 1 of one year and week 1 of the following year are numbered sequentially from 2 to 52 or 53 (if needed). For example, January 1, 1998 was a Thursday. If the first day of the week is MONDAY and the minimum days in a week is 4 (these are the values reflecting ISO 8601 and many national standards), then week 1 of 1998 starts on December 29, 1997, and ends on January 4, 1998. However, if the first day of the week is SUNDAY, then week 1 of 1998 starts on January 4, 1998, and ends on January 10, 1998. The first three days of 1998 are then part of week 53 of 1997.

Values are similarly calculated for the Week of Month.

### 8.5 <a name="Date_Patterns_Week_Elements" href="#Date_Patterns_Week_Elements">Week Elements</a>

**firstDay**

A number indicating which day of the week is considered the 'first' day, for calendar purposes. Because the ordering of days may vary between calendar, keywords are used for this value, such as sun, mon, …. These values will be replaced by the localized name when they are actually used.

**minDays (Minimal Days in First Week)**

Minimal days required in the first week of a month or year. For example, if the first week is defined as one that contains at least one day, this value will be 1. If it must contain a full seven days before it counts as the first week, then the value would be 7.

**weekendStart, weekendEnd**

Indicates the day and time that the weekend starts or ends. As with firstDay, keywords are used instead of numbers.

## 9 <a name="Parsing_Dates_Times" href="#Parsing_Dates_Times">Parsing Dates and Times</a>

For general information on lenient parsing, see [Lenient Parsing](tr35.md#Lenient_Parsing) in the core specification. This section provides additional information specific to parsing of dates and times.

Lenient parsing of date and time strings is especially complicated, due to the large number of possible fields and formats. The fields fall into two categories: numeric fields (hour, day of month, year, numeric month, and so on) and symbolic fields (era, quarter, month, weekday, AM/PM, time zone). In addition, the user may type in a date or time in a form that is significantly different from the normal format for the locale, and the application must use the locale information to figure out what the user meant. Input may well consist of nothing but a string of numbers with separators, for example, "09/05/02 09:57:33".

The input can be separated into tokens: numbers, symbols, and literal strings. Some care must be taken due to ambiguity, for example, in the Japanese locale the symbol for March is "3 月", which looks like a number followed by a literal. To avoid these problems, symbols should be checked first, and spaces should be ignored (except to delimit the tokens of the input string).

The meaning of symbol fields should be easy to determine; the problem is determining the meaning of the numeric fields. Disambiguation will likely be most successful if it is based on heuristics. Here are some rules that can help:

* Always try the format string expected for the input text first. This is the only way to disambiguate 03/07 (March 2007, a credit card expiration date) from 03/07 (March 7, a birthday).
* Attempt to match fields and literals against those in the format string, using loose matching of the tokens. In particular, Unicode normalization and case variants should be accepted. Alternate symbols can also be accepted where unambiguous: for example, “11.30 am”.
* When matching symbols, try the narrow, abbreviated, and full-width forms, including standalone forms if they are unique. You may want to allow prefix matches too, or diacritic-insensitive, again, as long as they are unique. For example, for a month, accept 9, 09, S, Se, Sep, Sept, Sept., and so on. For abbreviated symbols (e.g. names of eras, months, weekdays), allow matches both with and without an abbreviation marker such as period (or whatever else may be customary in the locale); abbreviated forms in the CLDR data may or may not have such a marker.
  * Note: While parsing of narrow date values (e.g. month or day names) may be required in order to obtain minimum information from a formatted date (for instance, the only month information may be in a narrow form), the results are not guaranteed; parsing of an ambiguous formatted date string may produce a result that differs from the date originally used to create the formatted string.
  * For day periods, ASCII variants for AM/PM such as “am”, “a.m.”, “am.” (and their case variants) should be accepted, since they are widely used as alternates to native formats.
* When a field or literal is encountered that is not compatible with the pattern:
  * Synchronization is not necessary for symbolic fields, since they are self-identifying. Wait until a numeric field or literal is encountered before attempting to resynchronize.
  * Ignore whether the input token is symbolic or numeric, if it is compatible with the current field in the pattern.
  * Look forward or backward in the current format string for a literal that matches the one most recently encountered. See if you can resynchronize from that point. Use the value of the numeric field to resynchronize as well, if possible (for example, a number larger than the largest month cannot be a month).
  * If that fails, use other format strings from the locale (including those in `<availableFormats>`) to try to match the previous or next symbol or literal (again, using a loose match).

* * *

Copyright © 2001–2022 Unicode, Inc. All Rights Reserved. The Unicode Consortium makes no expressed or implied warranty of any kind, and assumes no liability for errors or omissions. No liability is assumed for incidental and consequential damages in connection with or arising out of the use of the information or programs contained or accompanying this technical report. The Unicode [Terms of Use](https://www.unicode.org/copyright.html) apply.

Unicode and the Unicode logo are trademarks of Unicode, Inc., and are registered in some jurisdictions.

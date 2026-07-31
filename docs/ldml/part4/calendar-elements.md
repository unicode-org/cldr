## <a name="Calendar_Elements" id="Calendar_Elements" href="#Calendar_Elements">Calendar Elements</a>

```dtd
<!ELEMENT calendars (alias | (calendar*, special*)) >
<!ELEMENT calendar (alias | (months?, monthPatterns?, days?, quarters?, dayPeriods?, eras?, cyclicNameSets?, dateFormats?, timeFormats?, dateTimeFormats?, special*))>
<!ATTLIST calendar type NMTOKEN #REQUIRED >
```

* **The `` element**: The `<calendars>` element contains multiple `<calendar>` elements, each of which specifies the fields used for formatting and parsing dates and times according to the calendar specified by the `type` attribute (e.g. "gregorian", "buddhist", "islamic"). The behaviors for different calendars in a locale may share certain aspects, such as the names for weekdays. They differ in other respects; for example, the Japanese calendar is similar to the Gregorian calendar but has many more eras (one for each Emperor), and years are numbered within each era. All calendar data inherits either from the Gregorian calendar or other calendars in the same locale (and if not present there then from the parent up to root), or else inherits directly from the parent locale for certain calendars, so only data that differs from what would be inherited needs to be supplied. See _[Multiple Inheritance](tr35.md#Multiple_Inheritance)_.


Each calendar provides—directly or indirectly—two general types of data:

*   _Calendar symbols, such as names for eras, months, weekdays, and dayPeriods._ Names for weekdays, quarters and dayPeriods are typically inherited from the Gregorian calendar data in the same locale. Symbols for eras and months should be provided for each calendar, except that the "Gregorian-like" Buddhist, Japanese, and Minguo (ROC) calendars also inherit their month names from the Gregorian data in the same locale.
*   _Format data for dates, times, and date-time intervals._ Non-Gregorian calendars inherit standard time formats (in the `<timeFormats>` element) from the Gregorian calendar in the same locale. Most non-Gregorian calendars (other than Chinese and Dangi) inherit general date format data (in the `<dateFormats>` and `<dateTimeFormats>` elements) from the "generic" calendar format data in the same locale, which in turn inherits from Gregorian.

* Calendars that use cyclicNameSets and monthPatterns (such as Chinese and Dangi) have additional symbols and distinct formats, and typically inherit these items (along with month names) from their parent locales, instead of inheriting them from Gregorian or generic data in the same locale.


* The primary difference between Gregorian and "generic" format data is that date formats in "generic" usually include era with year, in order to provide an indication of which calendar is being used (Gregorian calendar formats may also commonly include era with year when Gregorian is not the default calendar for the locale). Otherwise, the "generic" date formats should normally be consistent with those in the Gregorian calendar. The "generic" calendar formats are intended to provide a consistent set of default formats for non-Gregorian calendars in the locale, so that in most cases the only data items that need be provided for non-Gregorian calendars are the era names and month names (and the latter only for calendars other than Buddhist, Japanese, and Minguo, since those inherit month names from Gregorian).


### <a name="months_days_quarters_eras" id="months_days_quarters_eras" href="#months_days_quarters_eras">Elements months, days, quarters, eras</a>

```dtd
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

* The context is either _format_ (the default), the form used within a complete date format string (such as "Saturday, November 12"), or _stand-alone_, the form for date elements used independently, such as in calendar headers. The _stand-alone_ form may be used in any other date format that shares the same form of the name. For month names, this is typically the nominative grammatical form, and might also be used in patterns such as "LLLL y" (month name + year). The _format_ form is an additional form that can be used in contexts where it is different than the stand-alone form. For example, in many languages, patterns that combine month name with day-of-month (and possibly other elements) may require the month name to be in a grammatical form such as genitive or partitive.

* In past versions of CLDR, the distinction between format and stand-alone forms was used to control capitalization (with stand-alone forms using titlecase); however, this can be controlled separately and more precisely using the `<contextTransforms>` element as described in _[ContextTransform Elements](tr35-general.md#Context_Transform_Elements)_, so both format and stand-alone forms should generally use middle-of-sentence capitalization.
* However, if in a given language, certain context/width combinations are always used in a titlecase form — for example, stand-alone narrow forms for months or weekdays — then these should be provided in that form.
* The distinctions between stand-alone (e.g. LLLL) and format (e.g. MMMM) forms are only relevant for how date elements are used within a date format. They are not intended to reflect how a date format is used within a sentence. For example, they are not intended to be used to generate the dative form of a date format when that format is used after a preposition that takes dative form.

* The width can be _wide_ (the default), _abbreviated_, or _narrow_; for days only, the width can also be _short,_ which is ideally between the abbreviated and narrow widths, but must be no longer than abbreviated and no shorter than narrow (if short day names are not explicitly specified, abbreviated day names are used instead). Note that for `<monthPattern>`, described in the next section:


*   There is an additional context type _numeric_
*   When the context type is numeric, the width has a special type _all_

* The format values must be distinct for the wide, abbreviated, and short widths. However, values for the narrow width in either format or stand-alone contexts, as well as values for other widths in stand-alone contexts, need not be distinct; they might only be distinguished by context. For example, "S" may be used both for Saturday and for Sunday. The narrow width is typically used in calendar headers; it must be the shortest possible width, no more than one character (or grapheme cluster, or exemplar set element) in stand-alone values (not including punctuation), and the shortest possible widths (in terms of grapheme clusters) in format values. The short width (if present) is often the shortest unambiguous form.


Era names should be distinct within each of the widths, including narrow; there is less disambiguating information for them, and they are more likely to be used in a format that requires parsing.

* Due to aliases in root, the forms inherit "sideways". (See _[Multiple Inheritance](tr35.md#Multiple_Inheritance)_.) For example, if the abbreviated format data for Gregorian does not exist in a language X (in the chain up to root), then it inherits from the wide format data in that same language X.


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

* The `yeartype` attribute for months is used to distinguish alternate month names that would be displayed for certain calendars during leap years. The practical example of this usage occurs in the Hebrew calendar, where the 7th month "Adar" occurs in non-leap years, with the 6th month being skipped, but in leap years there are two months named "Adar I" and "Adar II". There are currently only two defined year types, standard (the implied default) and leap.


* For `era` elements, an additional `alt="variant"` form may be supplied. This is primarily intended for use in the "gregorian" calendar, with which two parallel sets of era designations are used in some locales: one set with a religious reference (e.g. English BC/AD), and one set without (e.g. English BCE/CE). The most commonly-used set for the locale should be provided as the default, and the other set may be provided as the `alt="variant"` forms. See the example below.


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

### <a name="monthPatterns_cyclicNameSets" id="monthPatterns_cyclicNameSets" href="#monthPatterns_cyclicNameSets">Elements monthPatterns, cyclicNameSets</a>

```dtd
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

* The Chinese lunar calendar can insert a leap month after nearly any month of its year; when this happens, the month takes the name of the preceding month plus a special marker. The Hindu lunar calendars can insert a leap month before any one or two months of the year; when this happens, not only does the leap month take the name of the following month plus a special marker, the following month also takes a special marker. Moreover, in the Hindu calendar sometimes a month is skipped, in which case the preceding month takes a special marker plus the names of both months. The `<monthPatterns>` element structure supports these special kinds of month names. It parallels the `<months>` element structure, with various contexts and widths, but with some differences:


* Since the month markers may be applied to numeric months as well, there is an additional `monthPatternContext` type `numeric` for this case. When the numeric context is used, there is no need for different widths, so the `monthPatternWidth` type is `all` for this case.
* The `<monthPattern>` element itself is a pattern showing how to create the modified month name from the standard month name(s). The three types of possible pattern are for `leap`, `standardAfterLeap`, and `combined`.
* The `<monthPatterns>` element is not present for calendars that do not need it.

* The Chinese and Hindu lunar calendars also use a 60-name cycle for designating years. The Chinese lunar calendars can also use that cycle for months and days, and can use 12-name cycles for designating day subdivisions or zodiac names associated with years; a 24-name cycle of solar terms (12 pairs of minor and major terms) is used to mark intervals in the solar cycle. The `<cyclicNameSets>` element structure supports these special kinds of name cycles; a `cyclicNameSet` can be provided for types `year`, `month`, `day`, `dayParts`, or `zodiacs`. For each `cyclicNameSet`, there is a context and width structure similar to that for day names. For a given context and width, a set of `cyclicName` elements provides the actual names.


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

### <a name="dayPeriods" id="dayPeriods" href="#dayPeriods">Element dayPeriods</a>

The former `am`/`pm` elements have been deprecated, and replaced by the more flexible `dayPeriods`.

```dtd
<!ELEMENT dayPeriods ( alias | (dayPeriodContext*) ) >

<!ELEMENT dayPeriodContext (alias | dayPeriodWidth*) >
<!ATTLIST dayPeriodContext type NMTOKEN #REQUIRED >

<!ELEMENT dayPeriodWidth (alias | dayPeriod*) >
<!ATTLIST dayPeriodWidth type NMTOKEN #REQUIRED >

<!ELEMENT dayPeriod ( #PCDATA ) >
<!ATTLIST dayPeriod type NMTOKEN #REQUIRED >
```

* These behave like months, days, and so on in terms of having context and width. Each locale has an associated dayPeriodRuleSet in the supplemental data, rules that specify when the day periods start and end for that locale. Each type in the rules needs to have a translation in a dayPeriod (but if translation data is missing for a particular variable dayPeriod in the locale’s language and script, formatting should fall back to using the am/pm values). For more information, see _[Day Period Rules](#Day_Period_Rules)_.


* The dayPeriod names should be distinct within each of the context/width combinations, including narrow; as with era names, there is less disambiguating information for them, and they are more likely to be used in a format that requires parsing. In some unambiguous cases, it is acceptable for certain overlapping dayPeriods to be the same, such as the names for `am` and `morning`, or the names for `pm` and `afternoon`.


* If dayPeriods are specified for `noon` and `midnight`, they can often be formatted without also specifying the numeric time, e.g. "May 6, noon" instead of "May 6, 12:00 noon" or "May 6, 12:00 PM". To prevent parse issues, this should only be done if the names for `noon` and `midnight` are not also used for any other day periods, such as for `morning2` or `night1`.


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

### <a name="dateFormats" id="dateFormats" href="#dateFormats">Element dateFormats</a>

```dtd
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

* The patterns for date formats and time formats are defined in _[Date Format Patterns](#Date_Format_Patterns)._ These patterns are intended primarily for display of isolated date and time strings in user-interface elements, rather than for date and time strings in the middle of running text, so capitalization and grammatical form should be chosen appropriately.


Standard date and time patterns are each normally provided in four types: full (usually with weekday name), long (with wide month name), medium, and short (usually with numeric month).

* The `numbers` attribute can be used to explicitly specify a number system to be used for all of the numeric fields in the date format (as in `numbers="hebr"`), or for a specific field in the date format (as in `numbers="d=hanidays"`). This attribute overrides any default numbering system specified for the locale.


* The `datetimeSkeleton` element contains a _skeleton_ (see [availableFormats](#availableFormats_appendItems)) derived from the pattern. In the future the intent is to be able to generate the standard patterns from these `datetimeSkeleton` elements. However, in CLDR 40, the mechanisms associated with the `availableFormats` elements are not quite powerful enough to generate patterns that exactly match all of the ones provided in the `pattern` elements.


### <a name="timeFormats" id="timeFormats" href="#timeFormats">Element timeFormats</a>

```dtd
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

* The preference of 12 hour versus 24 hour for the locale can be derived from the [Time Data](#Time_Data). If the preferred hour symbol is 'h' or 'K' then the format is 12 hour; otherwise it is 24 hour. Formats with 'h' or 'K' must also include a field with one of the day period pattern characters: 'a', 'b', or 'B'.


* To account for customary usage in some countries, APIs should allow for formatting times that go beyond 23:59:59. For example, in some countries it would be customary to indicate that opening hours extending from _Friday at 7pm_ to _Saturday at 2am_ in a format like the following:


Friday: 19:00 – 26:00

* Time formats use the specific non-location format (z or zzzz) for the time zone name. This is the format that should be used when formatting a specific time for presentation. When formatting a time referring to a recurring time (such as a meeting in a calendar), applications should substitute the generic non-location format (v or vvvv) for the time zone in the time format pattern. See _[Using Time Zone Names](#Using_Time_Zone_Names)_ for a complete description of available time zone formats and their uses.


### <a name="dateTimeFormats" id="dateTimeFormats" href="#dateTimeFormats">Element dateTimeFormats</a>

```dtd
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
            <dateTimeFormat type="relative">
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
            <dateTimeFormat type="relative">
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

#### <a name="dateTimeFormat" id="dateTimeFormat" href="#dateTimeFormat">Element dateTimeFormat</a>

```dtd
<!ELEMENT dateTimeFormatLength (alias | (default*, dateTimeFormat*, special*))>
<!ATTLIST dateTimeFormatLength type ( full | long | medium | short ) #IMPLIED >
<!ELEMENT dateTimeFormat (alias | (pattern*, displayName*, special*))>
<!ATTLIST dateTimeFormat type NMTOKEN "standard" >
    <!--@MATCH:literal/standard, atTime, relative-->
```

* The `dateTimeFormat` element works like the dateFormats and timeFormats, except that the pattern is of the form "{1} {0}", where {0} is replaced by the time format, and {1} is replaced by the date format, with results such as "8/27/06 7:31 AM". Except for the substitution markers {0} and {1}, text in the dateTimeFormat is interpreted as part of a date/time pattern, and is subject to the same rules described in [Date Format Patterns](#Date_Format_Patterns). This includes the need to enclose ASCII letters in single quotes if they are intended to represent literal text.


* When combining a standard date pattern with a standard time pattern, start with the `dateTimeFormatLength` whose `type` matches the type of the *date* pattern, and then use one of the `dateTimeFormat`s for that `dateTimeFormatLength` (as described after the following table). For example:


###### <a name="Date_Time_Combination_Examples" id="Date_Time_Combination_Examples" href="#Date_Time_Combination_Examples">Table: Date-Time Combination Examples</a>

| Date-Time Combination   | dateTimeFormat            | Results |
| ----------------------- | ------------------------- | ------- |
| full date + short time  | full, e.g. "{1} 'at' {0}" | Wednesday, September 18, 2013 at 4:30 PM |
| medium date + long time | medium, e.g. "{1}, {0}"   | Sep 18, 2013, 4:30:00 PM PDT |

* For each `dateTimeFormatLength`, there is a standard `dateTimeFormat`. In addition to the placeholders {0} and {1}, this should not have characters other than space and punctuation; it should impose no grammatical context that might require specific grammatical forms for the date and/or time. For English, this might be “{1}, {0}”.


* In addition, especially for the full and long `dateTimeFormatLength`s, there may be `dateTimeFormat`s with `type="atTime"` and/or `type="relative"`. These are used to indicate an event at a specific time, and may impose specific grammatical requirements on the formats for date and/or time. For English, this might be “{1} 'at' {0}”.


The default guidelines for choosing which `dateTimeFormat` to use for a given `dateTimeFormatLength` are as follows:
* If an interval is being formatted, use the standard combining pattern to produce e.g. “March 15, 3:00 – 5:00 PM” or “March 15, 9:00 AM – March 16, 5:00 PM”.
* If a single date or relative date is being combined with a single time:
    * For a single date with a single time, by default use the `atTime` pattern (if available) to produce an event time: “March 15 at 3:00 PM”. If there is no `atTime` pattern, use the `standard` pattern.
* *** For a**: * For a relative date with a single time, by default use the `relative` pattern (if available) to produce an event time: “tomorrow at 3:00 PM”. If there is no `relative` pattern, use the `standard` pattern.

* *** However, at**: * However, at least in the case of combining a single date and time, APIs should also offer a “current time” option of using the `standard` combining pattern to produce a format more suitable for indicating  the current time: “March 15, 3:00 PM”.

* For all other uses of these patterns, use the `standard` pattern.

#### <a name="availableFormats_appendItems" id="availableFormats_appendItems" href="#availableFormats_appendItems">Elements availableFormats, appendItems</a>

```dtd
<!ELEMENT availableFormats (alias | (dateFormatItem*, special*))>
<!ELEMENT dateFormatItem ( #PCDATA ) >
<!ATTLIST dateFormatItem id CDATA #REQUIRED >
```

* The `availableFormats` element and its subelements provide a more flexible formatting mechanism than the predefined list of patterns represented by dateFormatLength, timeFormatLength, and dateTimeFormatLength. Instead, there is an open-ended list of patterns (represented by `dateFormatItem` elements as well as the predefined patterns mentioned above) that can be matched against a requested set of calendar fields and field lengths. Software can look through the list and find the pattern that best matches the original request, based on the desired calendar fields and lengths. For example, the full month and year may be needed for a calendar application; the request is MMMMyyyy, but the best match may be "y MMMM" or even "G yy MMMM", depending on the locale and calendar.


* For some calendars, such as Japanese, a displayed year must have an associated era, so for these calendars dateFormatItem patterns with a year field should also include an era field. When matching availableFormats patterns: If a client requests a format string containing a year, and all the availableFormats patterns with a year also contain an era, then include the era as part of the result.


* The `id` attribute is a so-called "skeleton", containing only field information, and in a canonical order. Examples are "yMMMM" for year + full month, or "MMMd" for abbreviated month + day. In particular:


* The fields are from the [Date Field Symbol Table](#Date_Field_Symbol_Table) in _[Date Format Patterns](#Date_Format_Patterns)_.
* The canonical order is from top to bottom in that table; that is, "yM" not "My".
* Only one field of each type is allowed; that is, "Hh" is not valid.

* In order to support user overrides of default locale behavior, data should be supplied for both 12-hour-cycle time formats (using h or K) and 24-hour-cycle time formats (using H or k), even if one of those styles is not commonly used; the locale's actual preference for 12-hour or 24-hour time cycle is determined from the [Time Data](#Time_Data) as described above in [timeFormats](#timeFormats). Thus skeletons using h or K should have patterns that only use h or K for hours, while skeletons using H or k should have patterns that only use H or k for hours.


The rules governing use of day period pattern characters in patterns and skeletons are as follows:

* Patterns and skeletons for 24-hour-cycle time formats (using H or k) currently _should not_ include fields with day period characters (a, b, or B); these pattern characters should be ignored if they appear in skeletons. However, in the future, CLDR may allow use of B (but not a or b) in 24-hour-cycle time formats.
* Patterns for 12-hour-cycle time formats (using h or K) _must_ include a day period field using one of a, b, or B.
* Skeletons for 12-hour-cycle time formats (using h or K) _may_ include a day period field using one of a, b, or B. If they do not, the skeleton will be treated as implicitly containing a.

Locales should generally provide availableFormats data for a fairly complete set of time skeletons without B, typically the following:

`H, h, Hm, hm, Hms, hms, Hmv, hmv, Hmsv, hmsv`

Locales that use 12-hour-cycle time formats with B may provide availableFormats data for a smaller set of time skeletons with B, for example:

`Bh, Bhm, Bhms`

* When matching a requested skeleton containing b or B to the skeletons actually available in the data, if there is no skeleton matching the specified day period field, then find a match in which the b or B matches an explicit or implicit 'a' in the skeleton, but replace the 'a' in the corresponding pattern with the requested day period b or B. The following table illustrates how requested skeletons map to patterns with different sets of `availableFormats` data:


###### <a name="Mapping_Requested_Time_Skeletons_To_Patterns" id="Mapping_Requested_Time_Skeletons_To_Patterns" href="#Mapping_Requested_Time_Skeletons_To_Patterns">Table: Mapping Requested Time Skeletons To Patterns</a>

<!-- HTML: spanning columns, header cells on non-first row -->
<table>
<tbody>
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
</tbody>
</table>

* The hour input skeleton symbols 'j', 'J', and 'C' can be used to select the best hour format (h, H, …) before processing, and the appropriate dayperiod format (a, b, B) after a successful match that contains an 'a' symbol.


The dateFormatItems inherit from their parent locale, so the inherited items need to be considered when processing.

#### <a name="Matching_Skeletons" id="Matching_Skeletons" href="#Matching_Skeletons">Matching Skeletons</a>

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

* Once a best match is found between requested skeleton and `dateFormatItem` `id`, the corresponding `dateFormatItem` pattern is used, but with adjustments primarily to make the pattern field lengths match the skeleton field lengths. However, the pattern field lengths should not be matched in some cases:


1. When the best-match `dateFormatItem` has an alphabetic field (such as MMM or MMMM) that corresponds to a numeric field in the pattern (such as M or MM), that numeric field in the pattern should _not_ be adjusted to match the skeleton length, and vice versa; i.e. adjustments should _never_ convert a numeric element in the pattern to an alphabetic element, or the opposite. See the second set of examples below.

2. When the pattern field corresponds to an availableFormats skeleton with a field length that matches the field length in the requested skeleton, the pattern field length should _not_ be adjusted. This permits locale data to override a requested field length; see the third example below.

3. Pattern field lengths for hour, minute, and second should by default not be adjusted to match the requested field length (i.e. locale data takes priority). However APIs that map skeletons to patterns should provide the option to override this behavior for cases when a client really does want to force a specific pattern field length.

---

For an example of general behavior, consider the following `dateFormatItem`:

```xml
<dateFormatItem id="yMMMd">d MMM y</dateFormatItem>
```

* If this is the best match for yMMMMd, the pattern is automatically expanded to produce a pattern "d MMMM y" in response to the request. Of course, if the desired behavior is that a request for yMMMMd should produce something _other_ than "d MMMM y", a separate `dateFormatItem` must be present, for example:


```xml
<dateFormatItem id="yMMMMd">d 'de' MMMM 'de' y</dateFormatItem>
```

---

For an example of not converting a pattern fields between numeric and alphabetic (point 1 above), consider the following `dateFormatItem`:

```xml
<dateFormatItem id="yMMM">y年M月</dateFormatItem>
```

* If this is the best match for a requested skeleton yMMMM, automatic expansion should not produce a corresponding pattern “y年MMMM月”; rather, since “y年M月” specifies a numeric month M, automatic expansion should not modify the pattern, and should produce “y年M月” as the match for requested skeleton yMMMM.


---

For an example of not converting a pattern field length if the corresponding skeleton field matches the requested field length (point 2 above), consider the following `dateFormatItem`:

```xml
<dateFormatItem id="MMMEd">E, d בMMMM</dateFormatItem>
```

* For Hebrew calendar date formats in the Hebrew locale, only the full month names should be used, even if abbreviated months are requested. Hence the `dateFormatItem` maps a request for abbreviated months to a pattern with full months. The same `dateFormatItem` can be expanded to expanded to match a request for “MMMMEd” to the same pattern.


---

* Finally: If the requested skeleton included both seconds and fractional seconds and the dateFormatItem skeleton included seconds but not fractional seconds, then the seconds field of the corresponding pattern should be adjusted by appending the locale’s decimal separator, followed by the sequence of ‘S’ characters from the requested skeleton.


#### <a name="Missing_Skeleton_Fields" id="Missing_Skeleton_Fields" href="#Missing_Skeleton_Fields">Missing Skeleton Fields</a>

* If a client-requested set of fields includes both date and time fields, and if the `availableFormats` data does not include a `dateFormatItem` whose skeleton matches the same set of fields, then the request should be handled as follows:


1. Divide the request into a date fields part and a time fields part.
    * Date fields are: [year](#dfst-year), [month](#dfst-month), [day](#dfst-day), [era](#dfst-era), [week](#dfst-week), [quarter](#dfst-quarter), and [week day](#dfst-weekday).
    * Time fields are: [hour](#dfst-hour), [minute](#dfst-minute), [second](#dfst-second), [period](#dfst-period), and [zone](#dfst-zone).
2. For each part, find the matching `dateFormatItem`, and expand the pattern as above.
* *** If there**: * If there is still no `dateFormatItem` whose skeleton matches the same set of fields, select the one with the greatest number of matching fields (but no extra fields), then use `appendItems` to append any missing fields (see below).

* *** If multiple**: * If multiple `dateFormatItem`s with missing fields have the same distance, rank them by their matching fields in the order listed in step 1. For example, if the request is for "HBv", and the locale has `dateFormatItem`s for only "HB" and "Hv", select the "HB" pattern, because "B" has a higher weight than "v", and then use the `appendItem` for "v" (time zone).

3. Combine the patterns for the two `dateFormatItem`s using the appropriate glue pattern, determined as follows from the requested date fields:
   * If the date fields part contains *only* a weekday, use `<appendItem request="Time-Day-Of-Week">`.
   * Otherwise, if the time fields part contains *only* a time zone, use `<appendItem request="Date-Timezone">`.
   * Otherwise, if the requested date fields include wide month (MMMM, LLLL) and weekday name of any length (e.g. E, EEEE, c, cccc), use `<dateTimeFormatLength type="full">`
   * Otherwise, if the requested date fields include wide month, use `<dateTimeFormatLength type="long">`
   * Otherwise, if the requested date fields include abbreviated month (MMM, LLL), use `<dateTimeFormatLength type="medium">`
   * Otherwise use `<dateTimeFormatLength type="short">`

```dtd
<!ELEMENT appendItems (alias | (appendItem*, special*))>
<!ELEMENT appendItem ( #PCDATA ) >
<!ATTLIST appendItem request CDATA >
```

* In case the best match does not include all the requested calendar fields, the `appendItems` element describes how to append needed fields to one of the existing formats. Each `appendItem` element covers a single calendar field. In the pattern, {0} represents the format string, {1} the data content of the field, and {2} the display name of the field (see [Calendar Fields](#Calendar_Fields)).


* Note: as described above `appendItems` for date fields should be appended to the date, and `appendItems` for time fields should be appended to the time, _before_ combining them with the `dateTimeFormat`.


#### <a name="intervalFormats" id="intervalFormats" href="#intervalFormats">Element intervalFormats</a>

```dtd
<!ELEMENT intervalFormats (alias | (intervalFormatFallback*, intervalFormatItem*, special*)) >

<!ELEMENT intervalFormatFallback ( #PCDATA ) >

<!ELEMENT intervalFormatItem (alias | (greatestDifference*, special*)) >
<!ATTLIST intervalFormatItem id NMTOKEN #REQUIRED >

<!ELEMENT greatestDifference ( #PCDATA ) >
<!ATTLIST greatestDifference id NMTOKEN #REQUIRED >
```

* Interval formats allow for software to format intervals like "Jan 10-12, 2008" as a shorter and more natural format than "Jan 10, 2008 - Jan 12, 2008". They are designed to take a "skeleton" pattern (like the one used in availableFormats) plus start and end datetime, and use that information to produce a localized format.


* The data supplied in CLDR requires the software to determine the calendar field with the greatest difference before using the format pattern. For example, the greatest difference in "Jan 10-12, 2008" is the day field, while the greatest difference in "Jan 10 - Feb 12, 2008" is the month field. This is used to pick the exact pattern.


* The pattern is then designed to be broken up into two pieces by determining the first repeating field. For example, "MMM d-d, y" would be broken up into "MMM d-" and "d, y". The two parts are formatted with the first and second datetime, as described in more detail below.


* For the purposes of determining a repeating field, standalone fields and format fields are considered equivalent. For example, given the pattern "LLL d - MMM d, Y", the repeating field would be "M" since standalone month field "L" is considered equivalent to format field "M" when determining the repeating field. Therefore the pattern would be broken up into "LLL d - " and "MMM d, Y".


* In case there is no matching pattern, the intervalFormatFallback defines the fallback pattern. The fallback pattern is of the form "{0} - {1}" or "{1} - {0}", where {0} is replaced by the start datetime, and {1} is replaced by the end datetime. The fallback pattern determines the default order of the interval pattern. "{0} - {1}" means the first part of the interval patterns in current local are formatted with the start datetime, while "{1} - {0}" means the first part of the interval patterns in current locale are formatted with the end datetime.


* The `id` attribute of intervalFormatItem is the "skeleton" pattern (like the one used in availableFormats) on which the format pattern is based. The `id` attribute of `greatestDifference` is the calendar field letter, for example 'M', which is the greatest difference between start and end datetime.


* The greatest difference defines a specific interval pattern of start and end datetime on a "skeleton" and a greatestDifference. As stated above, the interval pattern is designed to be broken up into two pieces. Each piece is similar to the pattern defined in date format. Also, each interval pattern could override the default order defined in fallback pattern. If an interval pattern starts with "latestFirst:", the first part of this particular interval pattern is formatted with the end datetime. If an interval pattern starts with "earliestFirst:", the first part of this particular interval pattern is formatted with the start datetime. Otherwise, the order is the same as the order defined in `intervalFormatFallback`.


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


## <a name="Semantic_Skeletons" id="Semantic_Skeletons" href="#Semantic_Skeletons">Semantic Skeletons</a>

* When speaking about dates and times, not all combinations of fields are semantically valid. For example, it does not make sense to talk about a particular minute without knowing the hour, or a day-of-month and year without knowing the month. This section defines _semantic skeletons_, a mechanism for expressing the subset of date and time skeletons that are sufficient for almost all use cases.


* Libraries implementing UTS 35 may benefit from the use of semantic skeletons in their APIs. Software can optimize for the bounded set of datetime formats defined by semantic skeletons, delivering better performance to users.


* This section describes only the structures and enumerations for expressing a semantic skeleton. The section [Generating Patterns for Semantic Skeletons](#Generating_Patterns_for_Semantic_Skeletons) describes a mechanism to extract the actual pattern backing a semantic skeleton from CLDR data.


Note: This document does not currently define a string form, but we may need one for MessageFormat.

> [!IMPORTANT]
> Semantic skeletons (this section) are a technical preview and should not be considered stable.

### <a name="Parts_of_a_Semantic_Skeleton" id="Parts_of_a_Semantic_Skeleton" href="#Parts_of_a_Semantic_Skeleton">Parts of a Semantic Skeleton</a>

A semantic skeleton is composed of two parts:

1. The _field set_: the minimal set of fields to be displayed. For example, "month and day."
2. The _options_: configurations that impact the choice and style of fields. For example, "render the fields in a long format." Not all options modify the same fields.

As a general rule, the field set determines _what is being displayed_, and the options determine _how to display it_.

#### <a name="Semantic_Field_Sets" id="Semantic_Field_Sets" href="#Semantic_Field_Sets">Semantic Field Sets</a>

This section defines four disjoint categories of field sets:

1. [Date](#Semantic_Date_Field_Sets)
2. [Calendar Period](#Semantic_Calendar_Period_Field_Sets)
3. [Time](#Semantic_Time_Field_Sets)
4. [Time Zone](#Semantic_Time_Zone_Field_Sets)

Certain combinations of categories form [Composite Field Sets](#Semantic_Composite_Field_Sets).

#### <a name="Semantic_Date_Field_Sets" id="Semantic_Date_Field_Sets" href="#Semantic_Date_Field_Sets">Date Field Sets</a>

A _date field set_ refers to a particular day in time. Higher-order fields, such as the month or year, could be omitted, but there must always be a reference to a particular day.

The fields that may be included in a date field set are:

1. **Year:** The year, possibly with an era and possibly with partial precision, depending on factors such as the length, locale, calendar system, and [Year Style](#Semantic_Skeleton_Year_Style). If the era is displayed, it may or may not be directly adjacent to the numeric year in the output string.
2. **Month:** The month of a year. The year can be explicit or implied.
3. **Day:** The day of the month. The month can be explicit or implied.
4. **Weekday:** The day of the week. Often stands on its own or is used to clarify a Day.

The valid date field sets are in the following table:

| Field Set                         | Example                   |
|-----------------------------------|---------------------------|
| { Day }                           | The 1st                   |
| { Weekday }                       | Saturday                  |
| { Day, Weekday }                  | Saturday the 1st          |
| { Month, Day }                    | January 1                 |
| { Month, Day, Weekday }           | Saturday, January 1       |
| { Year, Month, Day }              | January 1, 2000           |
| { Year, Month, Day, Weekday }     | Saturday, January 1, 2000 |

Note: Month and Year are not valid date field sets on their own because they do not refer to a specific day. Instead, they are considered calendar period field sets.

Note: This table may be extended in the future to include additional fields, such as week and quarter.

#### <a name="Semantic_Calendar_Period_Field_Sets" id="Semantic_Calendar_Period_Field_Sets" href="#Semantic_Calendar_Period_Field_Sets">Calendar Period Field Sets</a>

A _calendar period field set_ refers to a span of time in a calendar system, _above_ the order of a day.

The fields that are permissible in date field sets are also the ones permissible in calendar period field sets.

The valid calendar period field sets are in the following table:

| Field Set           | Example      |
|---------------------|--------------|
| { Month }           | January      |
| { Year }            | 2000         |
| { Year, Month }     | January 2000 |

Note: This table may be extended in the future to include additional fields, such as week, quarter, or standalone era.

Note: A _calendar period_ is distinct from a _date_ because it cannot be paired with time to form a composite field set.

#### <a name="Semantic_Time_Field_Sets" id="Semantic_Time_Field_Sets" href="#Semantic_Time_Field_Sets">Time Field Sets</a>

The **Time** field signifies the time of day.

Whether to include the Hour, Minute, Second, or Fractional Second is configured with the [Time Precision](#Semantic_Skeleton_Time_Precision) option.

| Field Set | Example         |
|-----------|-----------------|
| { Time }  | 4:03 pm	/ 16:03 |

Note: A day period (AM/PM) may be implied by the time field, depending on factors such as the length, locale, and hour cycle locale keyword.

Note: Durations, such as "3 minutes and 12 seconds" (or 3:12), are not handled through the skeleton mechanisms.

#### <a name="Semantic_Time_Zone_Field_Sets" id="Semantic_Time_Zone_Field_Sets" href="#Semantic_Time_Zone_Field_Sets">Time Zone Field Sets</a>

The **Zone** field signifies the time zone.

The rendering can be configured with the [Zone Style](#Semantic_Skeleton_Time_Zone_Style) option.

| Field Set | Example                             |
|-----------|-------------------------------------|
| { Zone }  | PST / PT / Los Angeles Time / GMT-8 |

#### <a name="Semantic_Composite_Field_Sets" id="Semantic_Composite_Field_Sets" href="#Semantic_Composite_Field_Sets">Composite Field Sets</a>

Date, calendar period, and time field sets can be combined in certain ways shown in the following table:

| Categories              | Example Field Set          | Example Output       |
|-------------------------|----------------------------|----------------------|
| Date + Time             | { Month, Day, Time }       | January 1 at 4 pm    |
| Date + Time Zone        | { Month, Day, Zone }       | January 1, PT        |
| Date + Time + Time Zone | { Month, Day, Time, Zone } | January 1 at 4 pm PT |
| Time + Time Zone        | { Time, Zone }             | 4 pm PT              |

* Note: "Date + Time Zone" is a valid combination because it refers to a specific span of time. "January 1, PST" refers to the span of time starting at `01-01T00:00-0800` and ending before `01-02T00:00-0800` (with an implied year).


Note: This table may be extended in the future to include additional combinations.

#### <a name="Semantic_Skeleton_Options" id="Semantic_Skeleton_Options" href="#Semantic_Skeleton_Options">Semantic Skeleton Options</a>

* A semantic skeleton associates fields with zero or more options, listed in this section. Options apply to specific fields, and they should not be specified if their respective fields are not in the field set. Some options have a default value.


#### <a name="Semantic_Skeleton_Length" id="Semantic_Skeleton_Length" href="#Semantic_Skeleton_Length">Length</a>

**Required Fields: Year, Month, Day, Weekday, Hour, or Zone**

**Default Value: Medium**

The _length_ determines how wide the fields should be rendered. There are three choices:

1. **Long:** Much space is available. Fields are typically spelled-out. Examples:
    - January 1, 2000
    - Rabiʻ I 7, 1446 AH
2. **Medium:** Space is limited, and spelled-out fields are desired. Examples:
    - Jan. 1, 2000
    - Rab. I 7, 1446 AH
3. **Short:** Space is limited, and numeric fields are desired. Examples:
    - 1/1/00
    - 3/7/1446 AH

* Note: Unlike standard CLDR pattern and skeleton strings, there is only one length option for the whole semantic skeleton. This is based on the principle that developers ought to inform the library how much space is available and the context in which the date/time is being displayed, and translators ought to decide how to use that space. For example, it is possible for long month names and abbreviated weekday names to coexist, but that should be a translator decision, not a developer decision. However, this option may be extended in the future to allow hinting at lengths for individual fields.


* Note: The locale or calendar may coerce the month length to be different than the skeleton length. For example, there is no numeric representation of months in the Hebrew calendar in English, so spelled-out month names will be used in "en-u-ca-hebrew" even if the length is Short.


Note: Additional lengths could be added in the future, such as "narrow" or "conversational".

#### <a name="Semantic_Skeleton_Alignment" id="Semantic_Skeleton_Alignment" href="#Semantic_Skeleton_Alignment">Alignment</a>

**Required Fields: Year, Month, Day, or Hour**

**Default Value: Inline**

The _alignment_ provides additional context that can be used for determining how to display certain fields, particularly numeric ones. There are two choices:

1. **Inline:** The text will be displayed in a paragraph, label, heading, or similar context.
2. **Column:** The text will be displayed vertically in a column-like layout or similar context where similar rendered widths are preferred.

* Note: The most common behavior with "column" alignment is for implementations to render a minimum of two digits on impacted fields. For example, an implementation might render "01/01/2000" instead of "1/1/2000" in US English.


#### <a name="Semantic_Skeleton_Year_Style" id="Semantic_Skeleton_Year_Style" href="#Semantic_Skeleton_Year_Style">Year Style</a>

**Required Field: Year**

**Default Value: Auto**

The _year style_ defines the level of precision to use when displaying the year. There are three choices:

1. **Auto:** Display the year with full or partial precision, and display the era if needed to disambiguate the year, depending on locale, calendar, and length.
2. **Full:** Display the year with full precision, and display the era if needed to disambiguate the year, depending on locale and calendar.
3. **WithEra:** Display the year with full precision, and always display the era.

* Going down the list, the three options can be seen as requiring additional context. "Auto" gives translators the most flexibility; "Full" requires that the year be displayed with full precision; and "WithEra" additionally requires that the era field be displayed.


Implementations could choose to use heuristics such as the following:

- Gregorian years within 20 years of the current date: partial precision okay
- Gregorian years after January 1, 1000: require full precision, but okay to hide era
- Other Gregorian years: require full precision and the era
- Non-Gregorian years: show era if not the default calendar system in the locale

Examples in Gregorian:

| Year Style | 2020 CE | 1500 CE | 750 CE | 500 BCE |
|------------|---------|---------|--------|---------|
| Auto       | ‘20     | 1500    | 750 AD | 500 BC  |
| Full       | 2020    | 1500    | 750 AD | 500 BC  |
| WithEra    | 2020 AD | 1500 AD | 750 AD | 500 BC  |

Note: This algorithm and the list of choices is likely to evolve as CLDR learns more about era display customs in different regions and calendar systems, and it may become normative.

#### <a name="Semantic_Skeleton_Hour_Cycle" id="Semantic_Skeleton_Hour_Cycle" href="#Semantic_Skeleton_Hour_Cycle">Hour Cycle</a>

**Required Field: Hour**

**Default Value: Auto**

* The _hour cycle_, which corresponds directly to the `-u-hc` Unicode Locale extension keyword, determines how hours should be numbered. It is always left up to the locale to determine how and whether day periods should be displayed.


The choices are:

1. **Auto:** Locale default
2. **H11:** Display hours numbered from 0 through 11
3. **H12:** Display hours numbered from 1 through 12 (the most common 12-hour clock)
4. **H23:** Display hours numbered from 0 through 23 (the most common 24-hour clock)
5. **H24:** Display hours numbered from 1 through 24
6. **Clock12:** Display hours using a 12-hour clock preferred by the locale
7. **Clock24:** Display hours using a 24-hour clock preferred by the locale

* Typically, locales will display a day period on H11, H12, and Clock12, but the day period could be any of those allowed by CLDR, such as AM/PM (field "a"), noon/midnight (field "b"), or flexible day periods such as "in the afternoon" (field "B"). The choice could depend on locale, length, and calendar system.


Note: An option could be added in the future to give the developer more control over how day periods are displayed or to disable day periods when there is sufficient context.

#### <a name="Semantic_Skeleton_Time_Precision" id="Semantic_Skeleton_Time_Precision" href="#Semantic_Skeleton_Time_Precision">Time Precision</a>

**Required Field: Time**

**Default Value: Second**

The _time precision_ option defines how precisely the time of day should be displayed. The choices are:

1. **Hour:** Display the time to the hour. Drop minutes and seconds.
2. **Minute:** Display the time to the minute. Drop seconds.
3. **MinuteOptional:** Display the time to the minute, but drop minutes if they are zero. Drop seconds.
4. **Second:** Display the time to the second. Drop fractional seconds.
5. **FractionalSecond** paired with an integer from 1 to 9: Display the time to the second, and include the specified number of fractional digits.

* If the input contains more precision than the specified _time precision_ option, extra precision is truncated. For example, "11:59:59" can be displayed as one of "11h", "11:59", or "11:59:59", but never "12h" or "noon".


Note: The finest level of precision is currently specified as nanoseconds, consistent with the requirements of many popular datetime libraries.

Note: The default value of time precision may change as more options are added.

#### <a name="Semantic_Skeleton_Time_Zone_Style" id="Semantic_Skeleton_Time_Zone_Style" href="#Semantic_Skeleton_Time_Zone_Style">Time Zone Style</a>

**Required Field: Zone**

**Default Value: Auto**

The _time zone style_ defines how to display the time zone. There are choices are:

1. **Auto:** Choose the best style based on the locale.
2. **Specific:** A time zone that unambiguously maps the time of day to an instant, which can be understood independently of the location or time of year. This field could resolve to specific non-location (pattern symbol "x", "xxxx") or offset (pattern symbols "O", "OOOO"), depending on the locale, length, and time zone identity.
3. **Generic:** A time zone based on the location of an event. This field could resolve to generic non-location (pattern symbols "v", "vvvv"), generic partial-location, or location (pattern symbol "VVVV"), depending on the locale, length, and time zone identity. Do not use this field if the location of the event is unknown from context, because doing so could lead to ambiguity.
4. **Location:** A time zone based on the identity of the IANA time zone. This field always resolves to the location format (pattern symbol "VVVV").
5. **Offset:** A time zone based on the time offset from UTC.

Examples:

| Style     | Example               |
|-----------|-----------------------|
| Specific  | Pacific Standard Time |
| Generic   | Pacific Time          |
| Location  | Los Angeles Time      |
| Offset    | GMT-8                 |

### <a name="Generating_Patterns_for_Semantic_Skeletons" id="Generating_Patterns_for_Semantic_Skeletons" href="#Generating_Patterns_for_Semantic_Skeletons">Generating Patterns for Semantic Skeletons</a>

A semantic skeleton can be mapped to a standard skeleton, which in turn can be mapped to a pattern according to the procedure described in  [Matching Skeletons](#Matching_Skeletons).

#### <a name="Mapping_to_Standard_Skeletons" id="Mapping_to_Standard_Skeletons" href="#Mapping_to_Standard_Skeletons">Mapping to Standard Skeletons</a>

To convert from a semantic skeleton to a standard skeleton, use the following procedure:

1. Map the semantic fields to standard fields according to the following table.
2. Apply the [time precision adjustment](#Semantic_Time_Precision_Skeleton_Variations), which may depend on the value being formatted.
3. Apply the [year style adjustment](#Semantic_Year_Style_Skeleton_Variations), which may depend on the value being formatted.

The following table contains the basic mapping from a semantic field to a standard field. The special columns indicate:

- Standalone: whether the specified field is the only field in the semantic skeleton. "N/A" means to use the same standard field for both standalone and non-standalone.
- Option: for Time, this is the [hour cycle](#Semantic_Skeleton_Hour_Cycle), and for Zone, this is the [time zone style](#Semantic_Skeleton_Time_Zone_Style).

| Semantic Field | Standalone? | Option            | Long   | Medium | Short  |
|----------------|-------------|-------------------|--------|--------|--------|
| Year           | N/A         | N/A               | \*     | \*     | \*     |
| Month          | No          | N/A               | \*     | \*     | \*     |
| Month          | Yes         | N/A               | LLLL   | LLL    | L      |
| Day            | N/A         | N/A               | \*     | \*     | \*     |
| Weekday        | No          | N/A               | EEEE   | EEE    | EEE    |
| Weekday        | Yes         | N/A               | EEEE   | EEE    | EEEEE  |
| Time           | N/A         | unset             | C      | C      | C      |
| Time           | N/A         | H11, H12, Clock12 | h      | h      | h      |
| Time           | N/A         | H23, H24, Clock24 | H      | H      | H      |
| Zone           | No          | Generic           | v      | v      | v      |
| Zone           | Yes         | Generic           | vvvv   | vvvv   | v      |
| Zone           | No          | Specific          | z      | z      | z      |
| Zone           | Yes         | Specific          | zzzz   | zzzz   | z      |
| Zone           | N/A         | Location          | VVVV   | VVVV   | VVVV   |
| Zone           | N/A         | Offset            | O      | O      | O      |

* **\* Lengths for**: \* Lengths for Year, Month, and Day are taken from the [datetimeSkeleton](#dateFormats) in the Long, Medium, and Short variants. The era field, if present, should be included with the Year. For example, in en-US, CLDR 46, the datetimeSkeletons are:


| Length | Calendar  | datetimeSkeleton |
|--------|-----------|------------------|
| Long   | Gregorian | yMMMMd           |
| Medium | Gregorian | yMMMd            |
| Short  | Gregorian | yyMd             |
| Long   | Japanese  | GyMMMMd          |
| Medium | Japanese  | GyMMMd           |
| Short  | Japanese  | GGGGGyMd         |

This means that the Year, Month, and Day semantic field mapping in en-US should be:

| Semantic Field | Calendar  | Long   | Medium | Short  |
|----------------|-----------|--------|--------|--------|
| Year           | Gregorian | y      | y      | yy     |
| Month          | Gregorian | MMMM   | MMM    | M      |
| Day            | Gregorian | d      | d      | d      |
| Year           | Japanese  | Gy     | Gy     | GGGGGy |
| Month          | Japanese  | MMMM   | MMM    | M      |
| Day            | Japanese  | d      | d      | d      |

#### <a name="Semantic_Time_Precision_Skeleton_Variations" id="Semantic_Time_Precision_Skeleton_Variations" href="#Semantic_Time_Precision_Skeleton_Variations">Time Precision Skeleton Variations</a>

The [time precision](#Semantic_Skeleton_Time_Precision) should change the skeleton for all lengths as follows:

- Hour: No change.
- Minute: Add "m"
- MinuteOptional: Add "m" if the input has a nonzero minute
- Second: Add "m" and "s"
- FractionalSecond: Add "m", "s", and a number of "S" characters equal to the integer option

#### <a name="Semantic_Year_Style_Skeleton_Variations" id="Semantic_Year_Style_Skeleton_Variations" href="#Semantic_Year_Style_Skeleton_Variations">Year Style Skeleton Variations</a>

The [year style](#Semantic_Skeleton_Year_Style) should change the skeleton for all lengths as follows:

- Auto: No change from datetimeSkeleton (note: could be "y", "yy", "yG", or another combination of year and era fields)
- Full or Auto resolving to Full: Replace "yy" with "y"
- WithEra or Auto/Full resolving to WithEra: Replace "yy" with "y" and add "G" if there is not already an era field

### <a name="Semantic_Skeleton_Conformance" id="Semantic_Skeleton_Conformance" href="#Semantic_Skeleton_Conformance">Semantic Skeleton Conformance</a>

This specification describes at a high level the space of legal configurations for a semantic skeleton. The exact shape of the API or syntax is left to the implementation.

Requirements for an implementation of semantic skeletons to be conformant with this specification:

1. All field sets and options described by this specification must be fully implemented.
2. Field sets other than the ones described by this specification must cause an error.
3. If none of the required fields for an input option are in the field set, there must be an error.

For example, a conformant specification must reject the following inputs:

| Field Set      | Options                        | Rejection Reason               |
|----------------|--------------------------------|--------------------------------|
| { Year, Day }  | Length: Long                   | Invalid field set              |
| { Month, Day } | Length: Long\nYear Style: Full | Year Style requires Year field |

* * *

© 2001–2026 Unicode, Inc.
This publication is protected by copyright, and permission must be obtained from Unicode, Inc.
prior to any reproduction, modification, or other use not permitted by the [Terms of Use](https://www.unicode.org/copyright.html).
Specifically, you may make copies of this publication and may annotate and translate it solely for personal or internal business purposes and not for public distribution,
provided that any such permitted copies and modifications fully reproduce all copyright and other legal notices contained in the original.
* You may not make copies of or modifications to this publication for public distribution, or incorporate it in whole or in part into any product or publication without the express written permission of Unicode.


Use of all Unicode Products, including this publication, is governed by the Unicode [Terms of Use](https://www.unicode.org/copyright.html).
The authors, contributors, and publishers have taken care in the preparation of this publication,
* but make no express or implied representation or warranty of any kind and assume no responsibility or liability for errors or omissions or for consequential or incidental damages that may arise therefrom.

This publication is provided “AS-IS” without charge as a convenience to users.

Unicode and the Unicode Logo are registered trademarks of Unicode, Inc. in the United States and other countries.

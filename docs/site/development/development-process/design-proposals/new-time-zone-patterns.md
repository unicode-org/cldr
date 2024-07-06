---
title: New Time Zone Patterns
---

# New Time Zone Patterns

**This proposal was reviewed in the CLDR TC meetings on 2013-01-09 and 2013-01-16, and approved by the CLDR TC.**

| Z | 1..3 | -0800 | Time Zone - RFC 822 GMT format. For more information about timezone formats, see [Appendix J: Time Zone Display Names](http://www.unicode.org/reports/tr35/#Time_Zone_Fallback) . |
|---|---|---|---|
| | 4 | HPG-8:00 | Time Zone - The localized GMT format. For more information about timezone formats, see [Appendix J: Time Zone Display Names](http://www.unicode.org/reports/tr35/#Time_Zone_Fallback) . |
| | 5 | -08:00 | Time Zone - ISO8601 time zone format. For more information about timezone formats, see [Appendix J: Time Zone Display Names](http://www.unicode.org/reports/tr35/#Time_Zone_Fallback) . |

## Summary

This design proposal includes following new pattern letters in the LDML date format pattern definition for time zone formatting.

1. **X** and **x** for ISO 8601 style non localizable UTC offset format
	1. 'X' uses UTC designator "Z" when UTC offset is 0
	2. 'x' uses difference between local time and UTC always - i.e. format like "+0000" is used when UTC offset is 0. 
2. **O** for localized GMT format variations
3. **V** for time zone ID (V - short / VV - IANA) and exemplar city (VVV).

## Background

JDK and LDML started from the same base, but they have been extended independently. LDML is not necessarily 100% compatible with JDK or vise versa, but using completely different definitions for the same purpose introduce confusion among consumers. Now, Java folks are trying to integrate JSR-310 into Java 8 release and extend some date format pattern definitions to support things missing in the current SimpleDateFormat.

For example, LDML specification uses pattern letter 'Z' for UTC offset based time zone format. In CLDR 22.1, letter 'Z' is defined as below:

However, JDK supports ISO8601 style format using letter 'X'. JDK also introduced letter 'I' for direct use of zone ID.

This proposal is created for sorting out time zone patterns and hopefully filling a gap between LDML specification and future JDK and JSR-310 releases, as well as adding necessary pattern for CLDR requirements.

## Non-Localized Local Time Offset Format

The pattern letter 'X' was added in JDK 7 for supporting ISO8601 style time zone format. In JDK 7, the behavior of pattern letter 'X' is described as below:

*ISO8601TimeZone:* *OneLetterISO8601TimeZone* *TwoLetterISO8601TimeZone* *ThreeLetterISO8601TimeZone* *OneLetterISO8601TimeZone:* *Sign* *TwoDigitHours* Z *TwoLetterISO8601TimeZone:* *Sign* *TwoDigitHours* *Minutes* Z *ThreeLetterISO8601TimeZone:* *Sign* *TwoDigitHours* : *Minutes* Z *TwoDigitHours:* *Digit Digit*
*Sign:* one of + - *Hours:* *Digit* *Digit* *Digit* *Minutes:* *Digit* *Digit* *Digit:* one of 0 1 2 3 4 5 6 7 8 9

In the current LDML specification, ISO8601 style format is specified by pattern "ZZZZZ" (5 'Z's), but followings are not supported

- Offset without separator, such as -0800
- Hour only format, such as -08

The JSR-310 proposal is trying to extend the definition to support seconds field in offset, probably because it is necessary to handle LMT in the time zone database. So the proposal adds "XXXX" and "XXXXX" for supporting optional second field.

*Note: ISO8601 specification does not support seconds field in local time offset.*

In LDML, we could define "ZZZZZZ" (6 'Z's), "ZZZZZZZ" (7 'Z's)... to support these requirements, but it would become so messy. Luckily, pattern letter 'X' is not yet used by the LDML specification, I propose to use the letter for supporting these requirements and deprecate "ZZZZZ" (5 'Z's).

The JSR-310 definition (compatible with JDK 7 SimpleDateFormat, with some enhancements for seconds field) might be used also for LDML, but I think there are several issues.

- Single 'X' is used for limiting offset to be hour field only. Such usage is practically questionable. There are some active time zones using offsets with non-zero minutes field. So such format is highly discouraged when a zone has non-zero minutes field. ISO8601 specification also says "The minutes time element of the difference may only be omitted if the difference between the time scales is exactly an integral number of hours.".
- When non-zero minutes (or seconds) field is truncated and hour field is 0, the output becomes +00/-00/+0000/-0000/+00:00/-00:00. Use of negative sign for offset equivalent to UTC (-00/-0000/-00:00) is illegal in ISO8601. 

When to use "Z" or "+00"/"+0000"/"+00:00" is also a design question. JSR-310 seems to extend pattern letter Z to support format without ISO8601 UTC indicator "Z".

In ISO8601 specification, "Z" is specifically defined for UTC of day. "+00"/"+0000"/"+00:00" is a valid format for difference between local time and UTC (Section 4.2.5.1). Semantically, "Z" expresses UTC itself, while "+00"/"+0000"/"+00:00" expresses a local time with UTC offset of zero. LDML specification might handle zone "Etc/UTC" different from winter time of "Europe/London" (the former is formatted as "Z" and the latter is formatted as "+00"/"+0000"/"+00:00"). However, it is not clear how "Etc/GMT", "Etc/GMT+0"... should be handled. (Etc/GMT might be just an alias of Etc/UTC, but Etc/GMT+0 is probably not in this scope...). It would be much easier to understand to control the behavior through different pattern letter.

For this reason, this proposal defines two pattern letter - 'X' and 'x'. 'X' is mostly upward compatible with JDK 7 SimpleDateFormat's 'X' and the current JSR-310 proposal, but slightly modified for resolving some design issues. 'x' only differs that the format always use difference between local time and UTC ("+00"/"+0000"/"+00:00" when UTC offset is 0).

**Proposed Definition**

|   |   |   |   |
|---|---|---|---|
| **X** | 1 | Z<br /> -08<br /> -0830 | Variable length ISO8601 'difference between local time and UTC - basic format' including hours field and optional minutes field, or "Z" (ISO8601 UTC designator) when the offset is 0.<br /> *Note: The seconds field in the offset is truncated.* |
|  | 2 | Z<br /> -0800 | Fixed length ISO8601 'difference between local time and UTC - basic format' including hours field and optional minutes field, or "Z" (ISO8601 UTC designator) when the offset is 0.<br /> *Note: The seconds field in the offset is truncated.* |
|  | 3 | Z<br /> -08:00 | Fixed length ISO8601 'difference between local time and UTC - extended format' including hours field and optional minutes field, or "Z" (ISO8601 UTC designator) when the offset is 0.<br /> *Note: The seconds field in the offset is truncated. This pattern is equivalent to "ZZZZZ".* |
|  | 4 | Z<br /> -0800<br />-083015 | Variable length format based on ISO8601 'difference between local time and UTC - basic format' including hours/minutes field and optional seconds field, or "Z" (ISO8601 UTC designator) when the offset is 0.<br /> *Note: When seconds field value is no 0, the result format is not a legal ISO8601 local time offset format.* |
|  | 5 | Z<br /> -08:00<br />-08:30:15 | Variable length format based on ISO8601 'difference between local time and UTC - extended format' including hours/minutes field and optional seconds field, or "Z" (ISO8601 UTC designator) when the offset is 0.<br /> *Note: When seconds field value is no 0, the result format is not a legal ISO8601 local time offset format.* |


|   |   |   |   |
|---|---|---|---|
| **x** | 1 | -08<br /> -0830 | Variable length ISO8601 'difference between local time and UTC - basic format' including hours field and optional minutes field.<br /> *Note: The seconds field in the offset is truncated.* |
|  | 2 | -0800 | Fixed length ISO8601 'difference between local time and UTC - basic format' including hours field and optional minutes field.<br /> *Note: The seconds field in the offset is truncated. This pattern is equivalent to "Z" /"ZZ"/"ZZZ".* |
|  | 3 | -08:00 | Fixed length ISO8601 'difference between local time and UTC - extended format' including hours field and optional minutes field.<br /> *Note: The seconds field in the offset is truncated.* |
|  | 4 | -0800<br />-083015 | Variable length format based on ISO8601 'difference between local time and UTC - basic format' including hours/minutes field and optional seconds field.<br /> *Note: When seconds field value is no 0, the result format is not a legal ISO8601 local time offset format.* |
|  | 5 | -08:00<br />-08:30:15 | Variable length format based on ISO8601 'difference between local time and UTC - extended format' including hours/minutes field and optional seconds field.<br /> *Note: When seconds field value is no 0, the result format is not a legal ISO8601 local time offset format.* |

|   |   |   |   |
|---|---|---|---|
| O | 1 | GMT<br /> GMT-5<br /> HPG+8:30<br /> UTC-8.30.15 | Short localized GMT format. |
|  | 4 | GMT<br /> GMT-05:00<br /> HPG+08:30<br /> UTC-08.30.15 | Long localized GMT format including hours/minutes fields  and optional seconds field.<br /> Note: This format is equivalent to the current "ZZZZ", except optional seconds field might be appended. |


- X, XX, and XXX always produce valid ISO8601 formats, but may lose the information of the second fields.
- XXXX and XXXXX produce valid ISO8601 format for practical use cases, but the outputs may include the second fields (when offset is not exact minutes).
- When UTC offset is 1 to 59 seconds, X, XX, and XXX interpret as UTC offset of zero and emit "Z". That means, X, XX and XXX never emit "+00" or "+0000".

The table below illustrates the behavior of pattern letter 'X' in JDK 7, JSR-310 proposal and this proposal.

Proposed 'x' only differs when offset (or truncated offset) is 0 - using +00/+0000/+00:00 instead of "Z"

*Note: JDK 7 uses +00 and -00. -00 is illegal in ISO8601 specification.*

## Localized GMT Format Variants

Pattern "ZZZZ" is currently used for localized GMT format. This format is constructed with following elements:

\<hourFormat>+HH:mm;-HH:mm\</hourFormat>

\<gmtFormat>GMT{0}\</gmtFormat>

\<gmtZeroFormat>GMT\</gmtZeroFormat>

For example, UTC offset is -3:00, the output is "GMT-03:00" with above data. Unlike non-localized local time offset format, this format uses local decimal digits for hours/minutes field.

This format sometimes tend to be longer than what people expect. For example, Bulgarian locale in CLDR 22 has "Гриинуич{0}" for gmtFormat. CLDR[#5382](http://unicode.org/cldr/trac/ticket/5382) proposes to add shorter version and we need a pattern for this purpose.

Again, we could use "ZZZZZZ" (6 'Z's) for this purpose, but it's a little bit ugly. We may also want other variants, for example, using numeric/symbol only offset format, such as "(+3)" in future. Therefore, this proposal allocate a new pattern letter 'O' for the purpose.

*Note: "OOOO" (instead of "OO") is used for long format to be consistent with other patterns. Pattern "OO" and "OOO" are reserved for future enhancement.*

## Time Zone ID

JSR-310 proposal includes pattern letter 'I' (capital ai) for time zone ID itself. This is a little bit beyond "locale repository" purpose, but it is the most robust way to preserve date/time information in a text representation.

In CLDR, we're afraid of burning one letter just for this purpose. In the CLDR TC meeting on Jan 16, 2003, we agreed to use "VV" for this purpose. At the same time, we also agreed to redefine already deprecated pattern "V" for CLDR/BCP 47 short time zone ID, and newly define "VVV" for exemplar city (location - localizable). As the result, the series of all "V" patterns, including existing "VVVV" (generic location format), will be a set of formats supporting canonical time zone ID round trip.

**Proposed Definition**

|   | JDK 7 (SimpleDateFormat) |  |  |  |  | JSR-310(proposed) |  |  |  |  | LDML(proposed) |  |  |  |  |
|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| UTC Offset | 00:00:00 | -00:00:30 | -00:30:00 | -00:30:30 | -01:00:00 | 00:00:00 | -00:00:30 | -00:30:00 | -00:30:30 | -01:00:00 | 00:00:00 | -00:00:30 | -00:30:00 | -00:30:30 | -01:00:00 |
| X | Z | +00 | -00 | -00 | -01 | Z | **?** | **?** | **?** | -01 | Z | Z | -00:30 | -00:30 | -01 |
| XX | Z | +0000 | -0030 | -0030 | -0100 | Z | **?** | -0030 | -0030 | -0100 | Z | Z | -0030 | -0030 | -0100 |
| XXX | Z | +00:00 | -00:30 | -00:30 | -01:00 | Z | **?** | -00:30 | -00:30 | -01:00 | Z | Z | -00:30 | -00:30 | -01:00 |
| XXXX | - | - | - | - | - | Z | -000030 | -0030 | -003030 | -0100 | Z | -000030 | -0030 | -003030 | -0100 |
| XXXXX | - | - | - | - | - | Z | -00:00:30 | -00:30 | -00:30:30 | -01:00 | Z | -00:00:30 | -00:30 | -00:30:30 | -01:00 |


|   |   |   |   |
|---|---|---|---|
| **V** | 1 | uslax<br /> utc | Short time zone identifier (BCP 47 unicode locale extension, time zone value)<br /><br /> fallback: If there is no mapping to BCP 47 time zone value, format for pattern "xxxx" is used as a fallback, such as "-0500" |
|  | 2 | America/Los_Angeles<br /> Etc/GMT | Time zone identifier (IANA Time Zone Database, or user defined ID) |
|  | 3 | Los Angeles<br /> 東京 | Localized exemplar location (city) name for time zone<br /><br />  If a time zone is not associated with any specific locations (e.g. Etc/GMT+1), localized exemplar city name for time zone "Etc/Unknown" is used. |
   
![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
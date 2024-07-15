---
title: Islamic Calendar Types
---

# Islamic Calendar Types

|  Type |  Intercalary years with 355 days in each 30-year cycle |
|---|---|
|  I |  2, 5, 7, 10, 13, **15** , 18, 21, 24, 26, 29 |
|  II |  2, 5, 7, 10, 13, 16, 18, 21, 24, 26, 29 |
|  III |  2, 5, 8, 10, 13, 16, 19, 21, 24, **27** , 29 |
|  IV |  2, 5, 8, **11** , 13, 16, 19, 21, 24, 26, **30** |

| subtag |  Old Definition |  New Definition |  Comments |
|---|---|---|---|
|  islamic |  Astronomical Arabic calendar |  Islamic calendar |  Redefined as "generic" Islamic calendar. This type does not designate any specific Islamic calendar algorithm variants.<br /><br />  The old term "Arabic" is sometimes used specifically for Islamic calendar variants used in Arabic countries. Islamic (or Hijiri) would be more generic term which can be applicable to the calendar variants in Turkey, Malay and other locations. |
|  islamicc |  Civil (algorithmic) Arabic calendar |   *&lt;deprecated&gt;* |  Deprecated because it does not have the desired structure - replaced with "islamic-civil" |
|  islamic-civil (previously an alias of "islamicc") |  &lt; not available &gt; |  Islamic calendar, tabular (intercalary years [2,5,7,10,13,16,18,21,24,26,29] Friday epoch) |  "islamic-civil" was defined as an alias (legacy non-BCP type) of "islamicc" above. This proposal reuses the legacy type, instead of introducing a brand new subtag (such as "islamic-tblc" - tabular/civil) |
|  islamic-tbla (new) |  *&lt; not available &gt;* |  Islamic calendar, tabular (intercalary years [2,5,7,10,13,16,18,21,24,26,29] Thursday epoch) |  "tbla" means - Tabular / Thursday epoch (also known as astronomical epoch). Using the term "astronomical" would introduce the confusion with other variants (all sighting based variants are based on astronomical observation). |
|  islamic-umalqura |  *&lt; not available &gt;* |  Islamic calendar, Umm al-Qura |   |
|  islamic-rgsa (new) |  *&lt; not available &gt;* |  Islamic calendar, Saudi Arabia sighting |  Requested by Oracle. In future, we may need to add more regional sighting variants. In that case, we propose to use the syntax - "rg" (region) + country/region code. |

## Tickets

[#5525](http://unicode.org/cldr/trac/ticket/5525) Disambiguation of Islamic calendar variants

## Background

In CLDR, we have two Islamic calendar types available - "islamic" (astronomical?) and "islamic-civil". They originally came from the ICU implementations that seems to be largely influenced by the book - Calendrical Calculations, Nachum Dershowitz, Edward M. Reingold. In this book, "civil" calendar uses the simple arithmetical algorithm based on CE 622 July 16 Friday (Julian calendar). ICU's "islamic" (astronomical) is based on astronomical calculation of moon phase based on a certain location. The intent of latter is to provider better approximation of Hijri calendar actually used by countries, but it's not quite perfect.

Microsoft also provides two calendar types. In .NET, these are named HijriCalendar and UmAlQuraCalendar. The former was introduced a long time ago. According to this page [http://www.staff.science.uu.nl/~gent0113/islam/islam\_tabcal.htm], the algorithm used by MS's "HijriCalendar" is nothing more than simple arithmetic one, that is pretty similar to ICU's "islamic-civil" calendar. I compared MS's "HijriCalendar" with ICU's "islamic-civil" calendar side by side and I found ICU's islamic-civil is always one day after the MS's HijriCalendar. My understanding is that MS's implementation just use CE 622 July 15 (Julian) as the epoch date, that is one day before the ICU's implementation.

*Note: The link in the above reference page [*http://www.staff.science.uu.nl/~gent0113/islam/islam\_tabcal\_variants.htm*] indicates MS's "Kuwaiti Algorithm" is type I (using Intercalary years with 355 days in each 30-year cycle with 2,5,7,10,13,15,18,21,24,26 &29), but HijriCalendar class on .NET 4.5 seems to use type II (2,5,7,10,13,16,18,21,24,26 & 29).*

MS's UmAlQuraCalendar supports Umm al-Qura calendar introduced by Saudi Arabia [http://www.staff.science.uu.nl/~gent0113/islam/ummalqura.htm].

Recently, JSR-310 (formerly - Joda Time) was approved and the whole new date/time package will be integrated into Java 8. JSR-310 folks want to identify calendar type using BCP 47 locale extension. So they proposed to [add a few islamic calendar types](http://unicode.org/cldr/trac/ticket/5525) - one of Um al-Qura, one for Saudi Arabia sighting.

## Islamic Calendar Variants

Traditionally, the first day of month is the day of the first sighting of the hilal (crescent moon) shortly after sunset. Because of the sighting of the hilal is affected by various factors not predictable (clouds or brightness of the sky, geographic location and others), it is impossible to determine dates by calculation. This traditional practice is still followed in the majority of Muslim countries. In this proposal, we categorize all of sighting based Islamic calendar as a group of religious calendars.

Because the religious calendar date is not determined precisely, there were several algorithms developed for non-religious purposes. A group of Islamic calendar algorithm defined by simple rules without astronomical calculation is called tabular Islamic calendars. Some of them are used for calculating approximate dates and some are used for administrative purpose in some locations. ICU's civil calendar is one of these and Microsoft's "HijriCalendar" is another one in this group.

According to RH van Gent, there are several known variations of tabular calendar schemes [[http://www.staff.science.uu.nl/~gent0113/islam/islam\_tabcal.htm](http://www.staff.science.uu.nl/~gent0113/islam/islam_tabcal.htm)].

Also, there are two possible epoch dates for each scheme.

- Epoch type 'a' ("astronomical" or Thursday epoch): CE 622-07-15 (Julian)
- Epoch type 'c' ("civil" or "Friday" epoch): CE 622 07-16 (Julian)

With his categorization scheme, Microsoft's "HijiriCalendar" is Type IIa (Note: He originally categorized this one as type Ia - but he updated it to type IIa recently). ICU's civil calendar is Type IIc.

There is another algorithmic calendar called Umm Al-Qura. This algorithm is used by Saudi Arabia for administrative purpose. Unlike tabular calendars, the algorithm involves astronomical calculation, but it's still deterministic. Umm Al-Qura is also supported by Islamic communities in North America and Europe. This algorithm is implemented by Microsoft as "UmAlQuraCalendar". There is a request to implement the algorithm in ICU ([#8449](http://bugs.icu-project.org/trac/ticket/8449) Add Um Alqura Hijri Calendar Support).

So Islamic calendar variants are categorized as below:

- Religious: Based on the sighting of the hilal. Actual dates varies by location.
- Algorithmic
  - Tabular: ICU "civil", Microsoft "HijriCalendar"
  - Umm Al-Qura: Saudi Arabia and others, Microsoft "UmAlQuraCalendar"

## Hierarchical Calendar Type Subtags

CLDR contains a lot of locale data for formatting dates associated with various calendar types. But such calendar algorithm variants are irrelevant to resolving date format symbols and patterns. For example, the same instant may fall on to different dates depending on what Islamic algorithm variant is actually used. However, when formatting code creates a text representation of the dates, symbols (month names, day of week names ...) and patterns are most likely shared by all Islamic calendar variants.

This proposal will introduce a new policy in the calendar type name space.

- A calendar type might be represented by multiple subtags
- When a calendar type consists from multiple subtags, corresponding formatting data might be resolved by prefix match

For example, for a given calendar type "ca-xxx-yyy-zzz", if formatting data for type "xxx-yyy-zzz" is not available, them the formatting data for type "xxx-yyy" is used as the fallback, then finally "xxx".

## Proposed Calendar Type Value Changes

The table below specifies all of the proposed changes.

Below is the part of the actual XML contents (common/bcp47/calendar.xml)

\<ldmlBCP47>

&emsp;\<keyword>

&emsp;&emsp;\<key name="ca" alias="calendar" description="Calendar algorithm key">

&emsp;&emsp;&emsp;\<type name="islamic" description="Islamic calendar"/>

&emsp;&emsp;&emsp;\<type name="islamic-umalqura" description="Islamic calendar, Umm al-Qura" since="24"/>

&emsp;&emsp;&emsp;\<type name="islamic-tbla" description="Islamic calendar, tabular (intercalary years [2,5,7,10,13,16,18,21,24,26,29] Thursday epoch)" since="24"/>

&emsp;&emsp;&emsp;\<type name="islamic-civil" description="Islamic calendar, tabular (intercalary years [2,5,7,10,13,16,18,21,24,26,29] Friday epoch)" since="24"/>

&emsp;&emsp;&emsp;\<type name="islamic-rgsa" description="Islamic calendar, Saudi Arabia sighting" since="24"/>

&emsp;&emsp;&emsp;\<type name="islamicc" alias="islamic-civil" description="Civil (algorithmic) Arabic calendar" deprecated="true"/>

&emsp;&emsp;\</key>

&emsp;\</keyword>

\</ldmlBCP47>

**Note: Following sections including the old proposal and the discussions are preserved for reference purposes only**

### Calendar Type Keywords

In the CLDR technical committee meeting on 2013-01-02, we thought it's not a good idea to keep adding Islamic calendar variants. CLDR contains a bunch of formatting data associated with calendar type, but these Islamic calendar variants do nothing with formatting - they actually share the same formatting data. From this aspect, CLDR TC members prefer to support these variants using a separate extension.

For now,

&emsp;**ca-islamic**: Islamic religious calendar (ICU implementation is based on astronomical simulation of moon movement)

&emsp;**ca-islamicc**: Islamic civil calendar

After investigating various Islamic calendar algorithm and practical usage, we're probably going to **deprecate "islamicc"** as calendar type. In addition to "ca" (calendar) type, we would **introduce "cv" (calendar variant)** to distinguish one algorithm from another.So, all Islamic calendar variations share "ca-islamic", but "cv-xxx" to distinguish a variant from others. For example,

&emsp;**ca-islamic-cv-umalqura** : Islamic calendar / Umm al-Qura

### Proposed cv (Calendar Variant) Values

Below are the proposed calendar algorithmic variant values in this proposal

1. **umalqura** - Umm Al Qura calendar of Saudi Arabia
2. **tbla** - Tabular Islamic calendar with leap years in 2nd, 5th, 7th, 13th, 16th, 18th, 21st, 24th, 26th and 29th year in each 30-year cycle with the Thursday ('a' - astronomical) epoch(Microsoft Hijri Calendar)
3. **tblc** - Tabular Islamic calendar with leap years in 2nd, 5th, 7th, 13th, 16th, 18th, 21st, 24th, 26th and 29th year in each 30-year cycle with the Friday ('c' - civil) epoch ([Calendrica](http://emr.cs.iit.edu/home/reingold/calendar-book/Calendrica.html) Islamic - Arithmetic)

In addition to above, other tabular calendar like Fatimid ("tbl27a" or "tbl27c") might be added if necessary. (See wikipedia article: [Tabular Islamic calendar](http://en.wikipedia.org/wiki/Tabular_Islamic_calendar))

For the religious (sighting) calendar, we could explicitly represent the location. For now, JSR-310 community wants one for Saudi Arabia. We could define such variants by defining a syntax like \<2-letter county code> + \<padding character '0'>, for example "cv-sa0" (sa = Saudi Arabia), but the idea was rejected by the CLDR TC in the CLDR TC meeting on 2013-Jan-30 for following reasons: 1) the subtag registry should define the exact list whenever possible, 2) such syntax may allow subtags that are not really used - for example, jp0 (jp = Japan) is practically useless, and 3) the region might not be presented by country code. The CLDR TC's agreement was to register "rgsa" for Saudi Arabia (prefix "rg" (region) + 2-letter country code "sa") explicitly. We may add other regions based on requests in future using the same convention. So in addition to algorithmic variants above, this proposal includes:

1. **rgsa** - Islamic calendar variant for Saudi Arabia (based on sighting)

This proposal will deprecate the use of ca-islamicc (or islamic-civil in long format). With this proposal, keywords are mapped as below.

|  |  |  |
|---|---|---|
| New |  Old |  Semantics |
|  ca-islamic |  ca-islamic |  Islamic calendar (variation of calendar algorithm is irrelevant) |
|  ca-islamic-cv-tblc |  ca-islamicc |  Islamic tabular calendar based on the Calendrica arithmetic algorithm. (We should probably avoid the term - Islamic Civil Calendar) |

### Impacts

The impacts of the proposed changes are minimal. The proposed changes are mostly in bcp47/calendar.xml and the LDML specification. We could remove format data alias used for islamicc (islamic-civil) currently in root.xml.

**Counter Proposal: Calendar Type ("ca") with hierarchical subtags**

Mark (who proposed a new type "cv" originally) raised a concern about the calendar type specified by the combination of "ca" and "cv". His main argument is: A software dealing with calendar algorithm eventually need to read/store calendar type in some way. For this purpose, single combined string is more convenient than two strings stored in different keywords. With his counter proposal, a calendar variant will be specified as the second subtag after the main calendar type. For example,

ca-**islamic**-cv-**umalqura** -> ca-**islamic-umalqura**

Technically, both original proposal (ca-xxx-cv-yyy) and Mark's counter proposal (ca-xxx-yyy) agree that main calendar type and minor algorithm differences (not affecting formatting) are semantically separated. The question is whether if these two items should be physically separated or not. I'm writing down **pros**/**cons** of both approaches.

Note that we may get requests for some other calendar types/variations such as:

- Julian calendar
- Gregorian/Julian calendar with a certain switch over date
- Turkish variant of Islamic calendar
- Other regional variants of Islamic calendar

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
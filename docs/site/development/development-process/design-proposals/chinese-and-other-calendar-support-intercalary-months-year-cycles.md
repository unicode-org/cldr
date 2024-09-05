---
title: Chinese (and other) calendar support, intercalary months, year cycles
---

# Chinese (and other) calendar support, intercalary months, year cycles

|  |  |
|---|---|
| Author |  Peter Edberg, with info and ideas from many others |
|  Date |  2011-11-20 through 2011-11-30, **more 2012-01-10** |
|  Status |  Proposal |
|  Feedback to |  pedberg (at) apple (dot) com |
|  Bugs |  See list of tickets at the end of this document |

Currently the ICU Calendar object has basic support for the Chinese calendar (can determine era, year number, month, etc.). However, real date formatting using this calendar is blocked until CLDR adds necessary support for formatting Chinese calendar dates. In doing this, we need to take into account other calendars that may have similar issues, which we should support in a unified way. The intent here is to provide the minimum change necessary to support the Chinese calendar (and other luni-solar calendars) at the same level as other calendars are currently supported; support for additional special calendar features requiring significant enhancements to the ICU Calendar object (see below) is for future enhancements.

## A. Relevant calendar features

Salient features of the Chinese calendar, and related features of other calendars:

### 1. Chinese luni-solar calendar

- Months begin at a new moon and are 29 or 30 days long.
- A year consists of 12 or 13 months (determined by the number of new moons between winter solstices). Months are numbered 1-12. When an extra intercalary month is needed, it might be inserted after any of the standard months 2-11 (after 11 is unusual); it repeats the numbering of the preceding month, with an extra marker to indicate that it is a leap month (in Chinese this marker ‘闰’ precedes the month number). An astronomical rule determines whether and where it gets inserted in a given year. The winter solstice always occurs during month 11, so the new year (and month 1) usually begins on the second new moon after that (Unless month 11 has a leap month added).
- Astronomical calculations are based on a meridian of 120° (near Beijing).
- Years are named using a 60-year cycle. The year name is formed by combining a celestial stem from a 10-year cycle and an earthly branch from a 12-year cycle. The 12 earthly branches correspond to, but do not have the same names as, the 12 zodiac animals associated with them. For example:
	- Celestial stems: 甲 jiǎ, 乙 yǐ, 丙 bǐng, 丁 dīng, …
	- Earthly branches: 子 zǐ, 丑 chǒu, 寅 yín, 卯 mǎo, …
	- Zodiac animals: 鼠 Rat, 牛 Ox, 虎 Tiger, 兔 Rabbit, …
	- First years of 60-year cycle: 甲子 jiǎ-zǐ, 乙丑 yǐ-chǒu, 丙寅 bǐng-yín, 丁卯 dīng-mǎo, …
	- In principle each cycle can be treated as a separate era. However, such eras are not normally ever used in formatted dates, leading to potential ambiguity about which date is being represented. Traditionally this ambiguity could be resolved by also displaying a regnal period or regnal year along with the Chinese calendar date. In modern times this ambiguity is normally resolved by always displaying a Chinese calendar date in conjunction with a date (or at least a year) in at least one other calendar. In Taiwan this other calendar is typically the Minguo/ROC calendar; in Japan it is typically the Japanese calendar; in mainland China and elsewhere it is typically the Gregorian calendar (for a format like “y年U年MMMd日” where y is the Gregorian year and U is the stem-branch name). Note that the year transitions of the associated calendar do not occur at the same time as the year transitions of the Chinese calendar.
- There are at least two standard conventions for the epoch of the Chinese calendar — i.e. when was year 1 of era 1. Both are associated with the legendary emperor Huangdi **黃帝**, hence the "Huangdi era" **黃帝紀元**. The most common convention is to use the beginning of Huangdi's reign, commonly specified as 2697 BCE; a somewhat less common convention (and the one used by ICU) is to use the year when he supposedly invented the Chinese calendar, 2637 BCE. Since the latter is 60 years later, the stem-branch names associated with years do not change, but the cycle number is different. For some usages among calendar specialists Chinese calendar years may be numbered continuously from the beginning of the epoch, in which case Gregorian 2012 Jan. 23 is the beginning of Chinese calendar year 4650 or 4710 depending on which convention is used. However this kind of year numbering is not widely known.
- In Chinese the days of the month have special numbering. Days 1-10 use 初一, 初二, … 初十. For days 21-29 the number is formed using 廿 instead of 二十 to indicate 20. The first month is designated 正月 instead of 一月.

### 2. Other calendars related to the Chinese calendar (Japanese, Korean, Vietnamese)

- Similar luni-solar calendars are used in Japanese, Korean, and Vietnamese, with the computations based respectively on meridians near Tokyo, Seoul, and Hanoi. For the Japanese version, the date typically used for disambiguation would be a Japanese calendar date, not a Gregorian date. The Vietnamese calendar uses a different set of animals for the branch names in years, and the marker for intercalary month is inserted \*after\* the month name, not before.

### 3. Hebrew calendar

- The Hebrew calendar is another luni-solar calendar, with months of 29 or 30 days beginning at a new moon. Intercalary months are inserted during specific years of a 19-year cycle, always by doubling the month of Adar: A leap year has months Adar I and Adar II (Adar I is considered to be the extra inserted month).
- Month numbering is interesting. Traditionally, the month of Nisan was numbered 1, and Adar was 12 (thus Adar I and II were 12 and 13). However, this puts the month of Tishri, which begins with the new year (Rosh HaShanah), as month 7. A more modern numbering has Tishri as month 1 (to coincide with the new year) which leads to different schemes for numbering Adar and the subsequent months (see discussion below on what ICU does).

### 4. Coptic and Ethiopic solar calendars

- These *always* have 13 months; 12 months of 30 days each and a 13th month of 5 days (6 in a leap year). There is no leap month per se.

### 5. Hindu luni-solar calendar (old or new, with several variants):

- Months are 29 or 30 days, beginning at new moon (south India) or full moon (north India). Months are named based on which zodiac sign the sun transits into during the course of the lunar month. An intercalary month occurs when the sun does not transit into a zodiac sign during the lunar month, and it takes the name for the zodiac transit of the following month with a marker to indicate “extra”/“added”; the following month \*also\* takes a marker to indicate “original”/”regular”/”clean” (a bit like Adar I and Adar II, except that it can apply to any month). If the sun transits into two zodiac signs during a lunar month, then two months are collapsed into one; the resulting month takes the name associated with both zodiac signs, with a marker indicating “lost”. A year when this occurs must also have at least one added month, since the year must have 12 or 13 lunar months. Occasionally an added month with no transits is immediately followed by a collapsed month with two; in this case the first month takes the name of the first transit in the second month plus the marker “extra”/“added”, while the second month takes the names of both transits plus the marker “lost”.
- This calendar also uses a 60-year cycle of year names, but they are not derived as combinations of sub-cycle names (as with the Chinese calendar).

### 6. The Tibetan luni-solar calendar

- The Tibetan luni-solar calendar handles months like the Hindu calendar. Two different 60-year naming cycles are in use, one derived from the Chinese calendar and one derived from the Hindu calendar. In addition, three different cardinal year numbering schemes are used, with three different epochs (like the distinction between ethiopic and ethiopic-amete-alem calendars).

## B. Other features of the Chinese calendar, not for this proposal

The Chinese calendar divides the solar year into 24 solar terms— 12 major terms and 12 minor terms—each associated with divisions along the sun’s course through the zodiac. These are usually shown on printed calendars, and are used for agriculture and astrological purposes. The data could be derived from existing calendar fields, or a new field could be added.

Months and days are also named in cycles of 60 using the stem-branch names, and days are subdivided into 12 two-hour periods named according to the earthly branches. The combination of year name, month, day name and day period name (年月日時) is important for many purposes, including picking children’s names and arranging weddings, moves, travel, and funerals. This data could also be derived from existing calendar fields, or a new field added.

Festivals and holidays are shown on printed Chinese calendars, as well as on many other calendars. ICU4J has a preliminary framework for holiday support. ICU4C does not, and there is currently no commitment in ICU to move this along. Support for marking festivals and holidays is thus beyond the scope of this proposal.

Nothing in this proposal prevents or makes more difficult adding any of these other features later on; this proposal just focuses on features that can be implemented in the near term.

## C. ICU behavior

Here is how ICU currently handles the calendar behaviors above:

### 1. Chinese calendar

Months are numbered 0-11 (the zero-based value of UCAL\_MONTH). When an intercalary month is added, it has the same number as the preceding month, but the value of UCAL\_IS\_LEAP\_MONTH is 1 instead of 0 (this seems to be the only supported calendar that ever sets UCAL\_IS\_LEAP\_MONTH to anything other than 0).

For purposes of add and set operations, month is treated as a tuple represented by UCAL\_MONTH and UCAL\_IS\_LEAP\_MONTH. If UCAL\_IS\_LEAP\_MONTH is 0 for a month that has a leap month following, then adding 1 month, or setting UCAL\_IS\_LEAP\_MONTH to 1, sets the calendar to the leap month (which has the same value for UCAL\_MONTH). If a month does not have a leap month following, then a set of UCAL\_IS\_LEAP\_MONTH to 1 is ignored.

Years are numbered 1-60 (the value of UCAL\_YEAR) for each 60-year cycle. The era is incremented for each 60-year cycle, so we are currently in era 78. 

Current ICU4C formatting for the Chinese calendar is completely broken. For example, the short date format in root and zh is currently “y'x'G-Ml-d”; the result this produces for Chinese era 78, year 29, month 4 (non-leap or leap), day 2 is “29x-4-”: There is no era value or leap month indicator, and non-literal fields after the ‘l’ pattern character are skipped.

In ICU4J the existing situation is bit better. Via data in data/xml/main/root.xml, ICU inserts its own "isLeapMonth" resource into the calendar bundle for "chinese"; this provides a leapMonthMarker of "\*". There is a public ChineseDateFormatSymbols subclass of DateFormatSymbols which uses the "isLeapMonth" resource, and a public ChineseDateFormat of SimpleDateFormat; using ChineseDateFormat, Chinese calendar date formats using 'G' and 'l' can be formatted and parsed successfully.

### 2. Hebrew calendar

In a non-leap year, months run 0-4 (for months Tishri-Shevat), skip 5 (“Adar I”), then continue 6-12 (Adar-Elul). In a leap year, 5 is not skipped (“Adar I”), and CLDR data provides an alternate “leap” name for month 6 as “Adar II”.

### 3. Coptic and Ethiopic calendars

Months are numbered 0-12. 

### 4. Other calendars listed above

ICU does not currently support the Hindu, Vietnamese, or Tibetan calendars (it does support the quite different Indian Civil calendar).

## D. Problems with the current ICU behavior:

- For the Chinese and Hebrew calendars, there is no a priori way to know for a given year whether it is a leap year. You have to run through the dates in the year to check the behavior (and the way you have to do this depends on the calendar).
- The current model for UCAL\_IS\_LEAP\_MONTH (ICU4J) IS\_LEAP\_MONTH) as a boolean cannot directly indicate the "normal-month-after-leap-month" and "compressed-month" used in Hindu and Tibetan calendars. However, those special months can be inferred from looking at month data before and after the month of interest. Another alternative might be to re-interpret the IS\_LEAP\_MONTH field to take more than two values.
- It is a bit too bad that completely different models are used for leap months in the Hebrew and Chinese calendars. It would have been nice to have a more unified model that could also support the usage in Hindu and Tibetan calendars.
- Calendar::add (ucal\_add) for UCAL\_MONTH gives different strange results for the Hebrew and Chinese calendars. For the Hebrew calendar, in a non-leap year, adding 1 month to month 4 produces month 6. For the Chinese calendar, in a leap year, adding 1 month to month n (before a leap month) produces month n (but with IS\_LEAP\_MONTH set). This is similar to what happens to hours around daylight savings time transitions, except in that case there is no IS\_EXTRA\_HOUR field to provide disambuguation (we should add one, see below).

## E. Current CLDR support

CLDR currently provides the following:

### 1. yeartype attribute

The yeartype attribute for month name elements allows an alternate month name to be selected for leap years (current legal values are just “standard”—the default—and “leap”). It is only used for the Hebrew calendar, as follows:

\<month type="5">Shevat\</month> \<month type="6">Adar I\</month> \<month type="7">Adar\</month> \<month type="7" yeartype="leap">Adar II\</month>

This works with the normal MMM+/LLL+ pattern characters for months; the choice of which name to use is managed by ICU date formatting code.

Note that this yeartype month is currently mapped into ICU month name data as the 14th element in the array of Hebrew month names, which seems a bit hacky.

### 2. special pattern character ‘l’

The special pattern character ‘l’ (small L) is described as: “Special symbol for Chinese leap month, used in combination with M. Only used with the Chinese calendar.” It is intended to indicate where the leap month marker (when needed) should go in a date format. This is a bit odd:

- It is not clear how (or whether) this is supposed to work with availableFormats items and DateTImePatternGenerator.
- There is currently no structure in CLDR to provide the value for ‘l’. But assuming we added it…
- It is not clear how a client who wants month symbol names can get the name for a leap month - do they need to assemble it from two pieces? How would they know what order to use?
- It is not clear why this mechanism needs to be different than the mechanism used for the Hebrew calendar.

It seems unnecessary; the month naming could just be handled via the MMM+/LLL+ pattern, and CLDR data could provide complete month names both with and without the marker (distinguished using the something like the yeartype attribute). This would fit more smoothly into existing mechanisms.

## F. Proposal

Items 1-2 and 5-8 below are probably do-able for CLDR 21 and ICU 49. The others may come later.

### 1. ICU behavior for months

The Hebrew model of explicitly numbering all month names and skipping leap months in non-leap years does not work well for calendars like Chinese and Hindu that may insert leap months anywhere (and may combine months, etc.). The use of the UCAL\_IS\_LEAP\_MONTH field is better suited to this.

For choosing the correct month name variant, I had proposed the idea of enhancing the UCAL\_IS\_LEAP\_MONTH field to have 4 values, and adding an enum for these values:

- normal month, this is currently value 0 for UCAL\_IS\_LEAP\_MONTH
- leap month (for Chinese, this has the same month number as the month before; for Hindu & TIbetan, it has the same number as the month after), this is currently value 1 for UCAL\_IS\_LEAP\_MONTH
- normal month after leap month (needed for Hindu & Tibetan); this could be value -1 for UCAL\_IS\_LEAP\_MONTH (it is not a leap month, but does need a special name)
- compressed month (needed for Hindu & Tibetan); this could be value 2 for UCAL\_IS\_LEAP\_MONTH

While this was agreed in ICU PMC on 2011-11-09, I now think this idea should be withdrawn (agreed in PMC). For purposes of determining the variant month names, there are other approaches, e.g. for relevant calendars we can see whether subtracting a month gives the same month number (in which case we have a normal month after leap), or adding a month skips a month number (in which case we have a combined month). For calendrical calculations, however, the current UCAL\_IS\_LEAP\_MONTH values of 0 and 1 are adequate (since that is all that is needed to disambiguate month numbering); and in fact the extra values would complicate the calendrical calculations: if we set a month to be compressed, what does that mean?

For a unified model we could also change the Hebrew calendar to use this approach (since in a leap year it inserts Adar I before Adar, whose name then changes to Adar II - the form for normal after leap), but that might be a compatibility issue. We can at least set UCAL\_IS\_LEAP\_MONTH appropriately, even if we do not change the month numbering.

### 2. CLDR data for leap months

The yeartype attribute for month names cannot support different month name types for each month in a year, or for different months in a year.

Old ideas

*The first version of this proposal suggested defining for the month name element a new attribute “monthtype” which could have the values “standard”, “leap”, “standardAfterLeap”, or “combined”, and then supplying explicit names for each needed type for each month (rather than a mechanism to combing markers). The thought was that this would permit handling of special forms for e.g. the first month of the year. However, it is only the first month of the lunar year that may have a special form in the Chinese calendar, and that can never have a leap month anyway.*

*The second idea was to permit inside each \<monthWidth> element (i.e at the same level as the \<month> elements) zero or more \<monthPattern> elements, which could have a type attribute of "leap", "standardAfterLeap", or "combined", and whose value would be a a pattern showing how to combine a marker with a month name {0} (and possibly {1} for combined months) - e.g. "闰{0}" or "kshay {0}-{1}". This was approved in CLDR 2011-11-16. However, it does not address the problem of specifying a month type marker with* ***numeric*** *months as well. For this we need a separate structure that parallels monthContext…*

Current idea

(approved in CLDR meeting 2011-11-30)

Alongside the \<months> element, permit an optional parallel element \<monthPatterns> (only present for calendars that need it). The structure under this is similar to that for \<months>, except that:

- The \<monthPatternContext> element's type attribute that takes one of *three* values: "format", "stand-alone", or the added "numeric" (pattern to use with numeric months).
- The \<monthPatternWidth> element's type attribute can take an additional value "all" for use with the "numeric" context (since there is no width distinction for numeric months).
- The \<monthPattern> elements can have type "leap", "standardAfterLeap", or "combined"; the value is the pattern used for modifying the month name(s) to indicate that month type.
A Chinese calendar example (marker before the month name) in root:

\<monthPatterns> \<monthPatternContext type="format"> \<monthPatternWidth type="abbreviated"> (default alias to format/wide) \</monthPatternWidth> \<monthPatternWidth type="narrow"> (default alias to stand-alone/narrow) \</monthPatternWidth> \<monthPatternWidth type="wide"> \<monthPattern type=”leap”>{0}bis\</monthPattern> \</monthPatternWidth> \</monthPatternContext> \<monthPatternContext type="stand-alone"> \<monthPatternWidth type="abbreviated"> (default alias to format/abbreviated) \</monthPatternWidth> \<monthPatternWidth type="narrow"> \<monthPattern type=”leap”>{0}bis\</monthPattern> \</monthPatternWidth> \<monthPatternWidth type="wide"> (default alias to format/wide) \</monthPatternWidth> \</monthPatternContext> \<monthPatternContext type="numeric"> \<monthPatternWidth type="all"> \<monthPattern type=”leap”>{0}bis\</monthPattern> \</monthPatternWidth> \</monthPatternContext> \</monthPatterns>

And in the Chinese locale:

 \<monthPatterns> \<monthPatternContext type="format"> \<monthPatternWidth type="wide"> \<monthPattern type=”leap”>闰{0}\</monthPattern> \</monthPatternWidth> \</monthPatternContext> \<monthPatternContext type="stand-alone"> \<monthPatternWidth type="narrow"> \<monthPattern type=”leap”>闰{0}\</monthPattern> \</monthPatternWidth> \</monthPatternContext> \<monthPatternContext type="numeric"> \<monthPatternWidth type="all"> \<monthPattern type=”leap”>闰{0}\</monthPattern> \</monthPatternWidth> \</monthPatternContext> \</monthPatterns>

For other calendars, the \<monthPattern> elements above could be replaced by others such as the following:

- For the Hebrew calendar, in the Hebrew locale, one could have (for Adar I and II):

 \<monthPattern type=”leap”>{0} א׳\</monthPattern> \<monthPattern type=”standardAfterLeap”>{0} ב׳\</monthPattern>

- For the Hindu calendar, in root (for a combined month, the name will be an affix plus a combination of two month names):

 \<monthPattern type=”leap”>adhik {0}\</monthPattern> \<monthPattern type=”standardAfterLeap”>nija {0}\</monthPattern> \<monthPattern type=”combined”>kshay {0}-{1}\</monthPattern>

For the time being, at least, I don't think that we need to present this in the Survey Tool, and that may prove too complex and confusing anyway.

**3. Month name styles**

(mostly about data, some ideas for future structure requirements):

- Japanese locale month name styles, all for either Gregorian or lunar calendar (except as noted). The distinction among them is not just format vs standalone.
	- The style 1月, 2月, 3月... is almost always used for horizontal text and for yMd formats. This is by far the most common.
	- The style 一月, 二月, 三月... can be used for vertical text, as a special style e.g. on New Year cards, and rarely for government documents.
	- The traditional naming, which is still used sometimes for titles on calendar pages: 睦月, 如月, 弥生, 卯月, 皐月, 水無月, 文月, 葉月, 長月, ...
	- The name 正月 is formally an alternate name for Gregorian January, but in common usage means just the New Year holidays (first few days of Jan.).
- Chinese locale month names and alternates (applies to both traditional/simplified, mainland/Taiwan unless noted):
	- Gregorian calendar: The style 1月, 2月, 3月 is preferred for complete yMd dates (all of whose components should use 0-9 digits), especially when Gregorian dates are shown together with Chinese calendar dates. The style 一月, 二月, 三月 can also be used for month names by themselves, either in running text or as an isolated element on a calendar page.
	- Lunar calendar: The first month is always designated 正月. For the remaining months, the style 二月, 三月, 四月 is preferred (especially when Chinese calendar dates are shown together with Gregorian dates), except that 冬月 and 腊月 are sometimes used for months 11 and 12. Chinese numerals should be used for the other portions of complete dates as well.
- The “monthType” attribute in the first version of this proposal might have also provided a means to address variants such as some of the above, as well as the following:
	- For parsing, it would be useful to have multiple forms for month names—e.g. “Sep.” and “Sept.”

### 4. Day names

Will need some way to specify the special day numbering forms used in Chinese for the Chinese calendar - TBD, can be a future enhancement.

### 5. Deprecate the pattern character ‘l’ (small L).

If it occurs in a pattern it should be ignored.

### 6. CLDR data for year names

Option 1, \<years> element

(The following was originally agreed in CLDR 2011-11-16; however, it has been superseded by option 2, which was approved on 2011-11-30).

Add a \<years> element and sub-elements parallel to the current structure for \<months>, \<days>, and \<quarters>, as follows (with similar structure in ICU):

\<years> \<yearContext type=”format”> \<yearWidth type=”abbreviated”> \<year type="1">Jia-Zi\</month> \<year type="2">Yi-Chou\</month> … \<year type="60">Gui-Hai\</month> \</yearWidth> \<yearWidth type=”narrow”> (defaults to abbreviated) \</yearWidth> \<yearWidth type=”wide”> (defaults to abbreviated) \</yearWidth> \</yearContext> \</years>

Only the “format” context would be supported initially; other contexts could be added if needed.

Option 2, \<cyclicNames> element

(approved in CLDR meeting 2011-11-30)

As noted above, the cycle of 60 stem-branch names is used for months and days as well as years. Years as are also known according to the cycle of 12 zodiac animals associated with the branch portion of the stem-branch name. A cycle of 12 branch names is also used for subdivisions of a day. Thus, it would be beneficial to have a more general representation of such name cycles, even though cyclic names for months, days, and day subdivisions are not part of the current proposal.

In one of his comments on [#1507](http://unicode.org/cldr/trac/ticket/1507), Philippe Verdy mentions that the cycle of 60 names is also used for some non-calendrical enumerations in Chinese such as measurement of angles, and suggests that data for this should be independent of the calendar structure. These notions are specific to the Chinese locale, and are not notions that CLDR would support across multiple locales (unlike the Chinese calendar, which is supported across multiple locales), so it probably does not make sense to add CLDR structure for them.

The following proposes a ways to support cyclic names for years, zodiac mappings, months, days, and dayParts (not really the same as dayPeriods), with the currently-known cycles of length 60 or 12 (for the Chinese, Hindu, and related calendars); this structure would be just below the \<calendar> element:

 \<cyclicNameSets> \<cyclicNameSet type="years"> \<cyclicNameContext type=”format”> \<cyclicNameWidth type=”abbreviated”> \<cyclicName type="1">jia-zi\</month> \<cyclicName type="2">yi-chou\</month> … \<cyclicName type="60">gui-hai\</month> \</cyclicNameWidth> \< cyclicNameWidth type=”narrow”> (defaults to abbreviated) \</cyclicNameWidth> \< cyclicNameWidth type=”wide”> (defaults to abbreviated) \</cyclicNameWidth> \</cyclicNameContext> \</cyclicNameSet> \<cyclicNameSet type="months"> (root aliases to years) \</cyclicNameSet> \<cyclicNameSet type="days"> (root aliases to dayParts) \</cyclicNameSet> \<cyclicNameSet type="dayParts"> …data for branch names... \</cyclicNameSet> \<cyclicNameSet type="zodiacs"> (root aliases to dayParts, some locales will supply separate data) \</cyclicNameSet> \</cyclicNameSets>

As with the leap month data, this may not be appropriate for the Survey Tool.

**7. New pattern character(s)**

We would need to add a pattern character to indicate year name. A natural choice is ‘U’ since it is currently unused and ‘u’ is already used for a different year type.

### 8. ICU implementation changes

- Formatting... (to be supplied)
- Parsing (month names, year names)... (to be supplied)
- ICU4J ChineseDateFormat class, move relevant behaviors into SimpleDateFormat, leaving this as mostly a shell. Remove ChineseDateFormatSymbols use of "isLeapMonth" resource; instead derive the necessary data (needed only for backwards compatibility) from the monthPatterns data.

### 9. ICU API enhancements

- Add a calendar field IS\_EXTRA\_HOUR or IS\_REPEATED\_HOUR to disambiguate the hour added/repeated during DST transitions that set the clock back.
- Work out how and whether to map the modified month names (for leap month types) onto APIs that get date format symbols — use additional options to specify month symbol types? What about symbols for year names?
- Add Calendar API to answer the following questions for a given year and era:
	- Is it a leap year? And if so…
	- Of what type - does it adjust days or months?
	- When are the beginnings and ends (perhaps expressed as UDates) of the portions of the year that are affected by the adjustments? Note that calendars like Hindu could have in a single year up to two nonadjacent added months plus one combined month.

**10. Supporting the Vietnamese / Korean / Japanese variants of the Chinese lunar calendar**

These variants behave in a similar way, using different ways of designating leap months and different names for the stem-branch cycle, the branch cycle, and the zodiac cycle, and using a different meridian as the basis for astronomical calculations. We could support these in several ways:

- Treat them as separate calendars with different names, and potentially support all of them in each locale.
- Treat them each as the locale-specific variant of the Chinese lunar calendar. In that case the meridian for calculation needs to be supplied as part of the locale data. Need to clarify that the "Chinese" calendar means the locale-adapted version, not the historic imported one.
- An in-between approach in which there is just one locale-specific set of data for the Chinese-style lunar calendar, but the meridian for computation is specified independently—perhaps as another locale tag value.
- A combination of the above two approaches: Have locale data specify the default median, but also allow specification of meridian in the locale name to override this. This is the recommended approach.

**11. Chinese calendar ambiguous dates, and handling of 'y' pattern character**

For the Chinese calendar, the value within a Calendar object's YEAR file is the year number within a 60-year cycle. However, this year is never displayed numerically in a Chinese calendar date format; it is always displayed using the cyclic name, i.e. using pattern character 'U'. The Calendar object's ERA field is the cycle number, but this also is never used is a formatted date. Hence formatted dates that use only elements from the Chinese calendar itself are ambiguous as to which era/cycle they are associated with. For real-world usage, that is not a problem; the Chinese calendar is not intended to unambiguously represent a date, and is normally displayed in association with a date (at least a year) in one or more additional calendars that do provide that disambiguation.

As noted above, in Taiwan this other calendar is typically the Minguo/ROC calendar; in Japan it is typically the Japanese calendar; in mainland China and elsewhere it is typically the Gregorian calendar, often with additional calendars such as Islamic.

In the long run, CLDR calendar data for the Chinese calendar should specify which other calendar should be used as the associated calendar. Then it may be that for formatting and parsing Chinese calendar dates, the 'y' and 'G' pattern characters would be interpreted according to this associated calendar, rather than the Chinese calendar.

In the short term, ICU should specify that parse methods that do not take an associated Calendar object may not produce the expected results for the Chinese calendar. Such methods create a work Calendar object and then clear() it, which for the Chinese calendar will set it to era 1; since there is no era in the format, the parsed result will have era 1, producing a date in the range of Gregorian 2600 BCE (probably not what is expected).

Note that the convention of using a secondary calendar associated with a traditional calendar is not unique to the Chinese calendar. Real-world Japanese conventions for formatting dates often use both a Gregorian and Japanese Emperor year, e.g. "2012(平成24)年1月".

**G. Tickets**

The old CLDR and ICU tickets related to this are:

- CLDR [#1507](http://unicode.org/cldr/trac/ticket/1507): intercalary marker missing from chinese calendar (refile) [includes detailed comments from Philippe Verdy, some anticipating ideas above]
- CLDR [#2430](http://unicode.org/cldr/trac/ticket/2430): Illegal date-time field "l".
- CLDR [#2558](http://unicode.org/cldr/trac/ticket/2558): Chinese calendar formatting does not look right
- CLDR [#3672](http://unicode.org/cldr/trac/ticket/3672): Value inherited from "root" is in error
- ICU [#6049](http://bugs.icu-project.org/trac/ticket/6049): Intercalary markers

New tickets related to this, which supersede the above, are:

- CLDR [#4230](http://unicode.org/cldr/trac/ticket/4230): Add \<monthPattern> element for Chinese calendar support
- CLDR [#4231](http://unicode.org/cldr/trac/ticket/4231): Add cyclic year name support for Chinese calendar
- CLDR [#4232](http://unicode.org/cldr/trac/ticket/4232): Deprecate pattern character 'l', add pattern character 'U'
- CLDR [#](http://goog_1707162891)[4237](http://unicode.org/cldr/trac/ticket/4237): Change Chinese calendar formats to use 'U' pattern char [must wait for ICU format/parse support]
- CLDR [#4259](http://unicode.org/cldr/trac/ticket/4259): Chinese calendar data updates
- CLDR [#4277](http://unicode.org/cldr/trac/ticket/4277): Remove \<icu:isLeapMonth> processing from LDML2ICUConverter
- CLDR [#4282](http://unicode.org/cldr/trac/ticket/4282): Chinese calendar formats need era, Gregorian year, or ?
- CLDR [#4302](http://unicode.org/cldr/trac/ticket/4302): Fix spurious errors with chinese calendar (monthPatterns narrow, prettyPaths)
- CLDR [#4321](http://unicode.org/cldr/trac/ticket/4321): Skip some date pattern tests for Chinese calendar ('U' and other differences from std)
- CLDR [#4322](http://unicode.org/cldr/trac/ticket/4322): Fix test errors for monthPatterns
- CLDR [#4325](http://unicode.org/cldr/trac/ticket/4325): Fix test errors for date patterns using U (availableFormats...)
- ICU [#8958](http://bugs.icu-project.org/trac/ticket/8958): Use CLDR \<monthPatterns> data to format/parse Chinese cal dates
- ICU [#8977](http://bugs.icu-project.org/trac/ticket/8977): Use CLDR \<monthPatterns> data to format/parse Chinese cal dates (J)
- ICU [#8959](http://bugs.icu-project.org/trac/ticket/8959): Support new 'U' pattern char for formatting/parsing Chinese cal dates
- ICU [#9034](http://bugs.icu-project.org/trac/ticket/9034): Delete obsolete \<icu:isLeapMonth> in data/xml/main/root.xml
- ICU [#9035](http://bugs.icu-project.org/trac/ticket/9035): More ICU4C chinese calendar format/parse fixes
- ICU [#9043](http://bugs.icu-project.org/trac/ticket/9043): Chinese cal dates can't always be parsed - design 2-cal solution
- ICU [#9044](http://bugs.icu-project.org/trac/ticket/9044): Chinese cal dates can't always be parsed - document & fix tests
- ICU [#9055](http://bugs.icu-project.org/trac/ticket/9055): Integrate Chinese cal pattern updates (cldrbug 4237), update tests

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
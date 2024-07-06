---
title: Pattern character for “related year”
---

# Pattern character for “related year”

|   |   |  |
|---|---|---|
| Author |  Peter Edberg |  |
|  Date |  2014-Feb-11 |  |
|  Status |  Proposal |  |
|  Feedback to |  pedberg (at) apple (dot) com |  |
|  Bugs |  # [6938](http://unicode.org/cldr/trac/ticket/6938) |  |

In locales that use non-Gregorian calendars, it is often common to display some portion of a Gregorian or Gregorian-like date along with the formatted non-Gregorian date. This can take various forms:

1. For non-Gregorian calendars that are “Gregorian-like” (only the current-era year is different) such as the Japanese emperor-year calendar, the Gregorian year may be shown along with the calendar’s current year. For example, using the Japanese calendar in the Japanese locale, a common display format would be something like “2012(平成24)年1月15日” (year portion in yellow).
2. For non-Gregorian luni-solar calendars in which years are named in a 60-year cycle but eras are not used, the formatted date is ambiguous (it is not clear which era it belongs to, so it cannot be parsed reliably). Furthermore, users of the calendar may be unsure of the mapping between the named year and nearby years in the Gregorian or Gregorian-like calendar that they use for other purposes. To address this, there are two common conventions:
	1. Along with (or instead of) the year name, use the Gregorian year in which the lunar-calendar year began. For example, in the Chinese lunar calendar:
		- the lunar date corresponding to 2013-Jan-15, which is in the 12th month of the lunar year ren-chen that began 2012-Jan-23, could be represented as 2012壬辰年腊月初四 or just 2012年腊月初四 (year portion in yellow).
			- the lunar date corresponding to 2013-Feb-15, which is in the 1st month of the lunar year gui-si that began 2013-Feb-10, could be represented as 2013癸巳年正月初六 or just 2013年正月初六 (year portion in yellow).
	2. Along with the lunar year date (using the cyclic year name), show a complete year-month-day date in a Gregorian or Gregorian-like calendar. Which Gregorian-like calendar is used depends on the locale; for Japan it might be the Japanese imperial calendar, for Taiwan it might be the Minguo calendar, elsewhere it would typically be the Gregorian calendar (and possibly other calendars as well, in parts of China an Islamic calendar date may be shown too).

To address the format requirements for sections 1 and 2.1 above, I propose using pattern character 'r' to designate the related Gregorian year, which for any solar or luni-solar calendar would always be a fixed offset from the extended year 'u', and would correspond to the Gregorian year in which the calendar’s year begins (for the Gregorian calendar, 'r' would behave just 'u', i.e. the offset would be 0). For calendars completely unlinked from the solar year, such as the various Islamic calendars, 'r' could still correspond to the Gregorian year in which the Islamic year started, but the difference from extended year would not be a fixed offset (such formatting using Gregorian year is not common for the Islamic calendar anyway).

In ICU, each calendar could provide an internal method that would return that offset, then formatting or parsing using the 'r' character should just use that offset in conjunction with the EXTENDED\_YEAR calendar field. In CLDR, all that would be required is documenting the new pattern character.

The data and format requirements for section 2.2 above are more complex and not addressed here

See also the earlier discussion of these issues in section F.11 of the proposal “[Chinese (and other) calendar support, intercalary months, year cycles](https://cldr.unicode.org/development/development-process/design-proposals/chinese-and-other-calendar-support-intercalary-months-year-cycles).”

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
---
title: Date/Time Symbols
---

# Date/Time Symbols

Symbols is a required topic to work in [Date/Time Patterns](https://cldr.unicode.org/translation/date-time/date-time-patterns)

More details on date/time symbols and patterns may be found in the Spec [Date Field Symbol Table](http://www.unicode.org/reports/tr35/tr35-dates.html#Date_Field_Symbol_Table). 

## About Symbols

Dates and times are formatted using patterns, like "mm-dd". Within these patterns, each field, like the month or the hour, is represented by a sequence of letters (“pattern characters”) in the range A–Z or a–z. For example, sequences consisting of one or more ‘M‘ or ‘L‘ stand for various forms of a month name or number. 

When the software formats a date for your language, a value will be substituted for each field, according to the following table. Examples of the pattern usage you may see in an every day use may be on the lock screen on a mobile device showing the date or time, or as a date stamp on an email. 

Notice in the table below that there are different pattern characters for standalone and formatting. For example M to indicate the formatting and L to indicate the standalone month names. 

Make sure you understand the difference between standalone and formatting patterns and use the appropriate symbols in patterns. See [when to use standalone vs. formatting](https://cldr.unicode.org/translation/date-time/date-time-patterns) in Date and Time patterns.

| Symbol | Meaning  | English example  | Special note | Usage in pattern example |
|:---:|---|---|---|---|
| G | era | AD, BC | This symbol also covers era names and abbreviations for non-Gregorian calendars, such as Japanese. | y G = 1999 AD |
| y<br /><br /> yy | year   | 1987 | use y to show as many digits as necessary (987, 2017)<br /><br /> Use yy to always show year in two digits (87, 17, 09). | M/d/y = 9/5/2019  M/d/yy = 9/5/19 |
| M | month | September    | Used in patterns to reference use of Formatted month names. | MMMM d, y = September 5, 2019 |
| L | month | September | Used in patterns to reference use of Standalone month names.<br /> _See below under Stand-Alone vs Format Styles for the difference between M and L_. | LLLL d, y =September 5, 2019 |
| E | Day of week   | Tuesday  | Used in patterns to reference use of Formatted weekday names. | EEEE, MMMM d, y = Sunday, September 5, 2009 |
| c | Day of week | Tuesday | Used in patterns to reference use of Standalone weekday names.<br /> _See below under Stand-Alone vs Format Styles for the difference between E and c_. | ccc = Sun |
| d | day |  |  |  |
| h<br /> H<br /> K<br /> k | Hour | 12 | h- hour in a 12 hour clock<br /> H-hour in a 24 hour clock system using 0-23<br /> K -12 hour cycle using 0 through 11<br /> k - 24 hour cycle using 1 though 24 | h:mm a = 3:25 PM<br /> HH:mm = 15:25<br /> K:mm a = 0:25 AM<br /> kk:mm = 24:25 |
| m | minute | 49 |  | hh:mm a = 03:25 PM |
| s | second | 49 |  | h:mm:ss = 3:25:01 PM |
| a<br /> b<br /> B | day period | AM noon in the morning | a- AM or PM b-am, noon, pm, or midnight<br /> B-"in the morning", "in the evening" (see [Day period names](https://cldr.unicode.org/translation/date-time-1/date-time-names#TOC-Day-Periods-AM-PM-etc.-))<br /><br /> ONLY  used with "h" or K" for all a, b and B See [date/time patterns about flexible time formats](https://st.unicode.org/cldr-apps/v#/fr/Gregorian/7a365a21694f0127) even if your language does not use 12-hour time clock Also see date/time day period patterns. | h:mm a = 3:25 PM<br /><br />  h:mm b = 12:00 noon<br /><br />  h:mm B= 3:25 in the afternoon |
| z / v | timezone | Pacific Time, Paris Time | Don't change v to z or vice versa; just leave either z or v as in English. | h a – h a v = 5 AM – 5 PM GMT |
| ' |  |  | If you want a sequence of one or more real letters A–Z or a–z within a pattern, you need to put it in single quotes, such as 'ta'. This is because l etters included in a format have special meaning.<br /> For a real single quote, use '' (that is, two adjacent ' characters). | German example for skeleton h h 'Uhr' a = 1 Uhr PM  |
| Q | Quarter (a concept of 3 months period) |  | These are calendar quarters not fiscal quarters.<br /><br /> 4 Quarters in United States for example would be Jan-Mar, Apr-June, July-Sep, Sep-Dec. Also see 💡 **Translation Tips**  below | QQQ y = Q3 1999 |

| Symbol | Examples | Meaning |
|:---:|:---:|---|
| M | 3, 11 | Numeric form, at least 1 digit and without leading zero. |
| MM |  03, 11  | Number form, 2 digits with leading zero if necessary |
| MMM | Dec | Abbreviated form |
| MMMM | December | Full form |
| MMMMM | D | Narrow form - only used in where context makes it clear, such as headers in a calendar. _Should be one character wherever possible._ |

💡**Translation Tips**

- The symbols using characters a-z and A-Z are special placeholders; they stand for date or time fields. They are NOT real characters.
- For example, 'y' stands for a numeric year and will be replaced by a value like '1998'. DO NOT "translate" the placeholders; for example, don't change 'y' to 'j' even though in your language the word for "year" starts with a "j".
- Any "real" characters need to be quoted. For example, 'g' in a real character in this example pattern: EEE, yyyy. **'g'**. dd. MMM
- If your language doesn't have a concept of "**Quarters**", use a translation that describes the concept "three-month period" rather than “quarter-of-a-year”.

## Symbol Length

The number of letters in a field indicates the **format.** 

The number of letters used to indicate the format is the same for all date fields EXCEPT for the year. (See table above for y and yy).

The following are the available field lengths, and their meanings:

The longer forms are only relevant for the fields that are non-numeric, such as era names, month names, day of the week, and am/pm, etc...

## Standalone vs. Format Styles

 This section is relevant to [When to use standalone vs. Formatting](https://cldr.unicode.org/translation/date-time/date-time-patterns) in date/time patterns. 

Some languages use two different forms of strings (*standalone* and *format*) depending on the context. Typically the *standalone* version is the nominative form of the word, and the *format* version is in the genitive (or related form).

Two different characters are used:

| Field | Format | Standalone |
|:---:|---|---|
| Month | M | L |
| Day of the Week | E | c |

💡 **Translation Tips**

- Standalone and Format names must be coordinated with the format strings. See [When to use standalone vs. Formatting](https://cldr.unicode.org/translation/date-time/date-time-patterns) in date/time patterns.
- If your language uses two different form, be sure to provide the correct forms under Standalone and Formatting. For example in Brazilian Portuguese
	- "Dezembro" for December when standing alone; thus use LLLL
	- "Dezembru" when referencing December with a date (e.g. to mean "the nth day of that month"); thus, use MMMM
	- Then, make sure you use the intended forms by using the correct symbol (e.g. LLLL stand-alone form or MMMM format forms).
	- If your language formats months differently with vowels, eg "14 de gener de 2008" but "14 d'abril de 2008"; the stand-alone and format versions of the months should be as follows; in this case the format strings should not have the extra "de" before the month:

| Format Month | Stand-Alone Month |
|---|---|
| de gener | gener |
| d'abril | abril |

These days, standalone names should not be used merely to provide capitalized forms. There are other solutions for capitalizing date symbols which provide finer control over capitalization, see [capitalization guidelines](https://cldr.unicode.org/translation/translation-guide-general/capitalization).

### Examples:

Nominative/standalone (LLLL) vs genitive/format (MMMM):

| Format | Example1 | Example2 |
|:---:|:---:|:---:|
| LLLL | Dezembro | Dez. |
| d MMMM | 1 Dezembru | 1 Dez. |
| MMMM d yy | Dezembru 1 1953 | 1 Dez. 53 |

Precede months with de or d’ - coordinate with the formats strings, which can't have the extra "de" before the month:

| Format String | Date | Result |
|:---:|:---:|---|
| LLLL | 2008-1-14 | gener |
|  | 2008-4-14 | abril |
| d MMMM 'de' y | 2008-1-14 | 14 de gener de 2008 |
|  | 2008-4-14 | 14 d’abril de 2008 |
 
![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
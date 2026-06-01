---
title: Date & Time
---

# Date & Time

Date and time data provides information for formatting dates, times, timezones, datetime intervals. The pages below describe date and time pieces, how to write the patterns to format dates, the symbols used in the date time patterns, and time zones.

* [Date/Time Names](https://cldr.unicode.org/translation/date-time/date-time-names)
* [Date/Time Patterns](https://cldr.unicode.org/translation/date-time/date-time-patterns)
* [Date/Time Symbols](https://cldr.unicode.org/translation/date-time/date-time-symbols)
* [Time Zones and City names](https://cldr.unicode.org/translation/time-zones-and-city-names)

🚨
## Error/warning messages 

The following are some of the error/warning messages you might see, and how to handle them.

* The first part of the message lists the Code and Value. For example:
    * Code ┃ Value ┃ Message
    * GyMMMd ┃ dd MMM y G ┃ …
* The messages might not be exactly what you see on the screen,
because parts of the message are customized.
* The links to related paths (shown in italic on this page) will open up in a separate window so that you can compare side-by-side.
* Note that some errors or warnings may be due to inherited times, sometimes between calendars.

<!-- 
DONE
afternoon1 ┃ in die middag ┃ Narrow value "in die middag" shouldn't be longer than the corresponding abbreviated value "die middag" ┃

For later
morning1 ┃ du mat. ┃ Inconsistent use of periods in abbreviations for this section. ┃
afternoon1 ┃ in die middag ┃ Narrow value "in die middag" shouldn't be longer than the corresponding abbreviated value "die middag" ┃
medium ┃ d MMM. y ┃ Your pattern (d MMM. y) is probably incorrect; abbreviated month/weekday/quarter names that need a period should include it in the name, rather than adding it to the pattern. ┃
-->

### Numeric date separators

**Date ┃ \- ┃ Numeric date separator conflicts with «/» from the base «d/M/y» at _yMd_**
   * The numeric date separator should be consistent with what is in the pattern for Flexible dates with the Code yMd.
   * That is normally also the time separator for Codes that contain Md, and yM.
However, some locales change the separator if there are only two placeholders instead of three.
   * Check carefully to see whether you should change the separator or change what is in the base or other related Flexible time Codes.

**Date ┃ E d/M ┃ Numeric date separator «/» **in pattern** conflicts with «-», the default in _Date_ — fix as per _Date/time error/warning messages_.**
   * This warning is on the other side of the coin; it is on a pattern that contains Md, and yM (thus including MEd, GyM, etc.)
   * Look at the pattern carefully to see whether you need to change it for consistency across all the related Codes.

## Numeric time separators

**Time ┃ . ┃ Numeric time separator conflicts with «:» from the base «H:mm:ss» at _Hms_**
   * The numeric time separator conflicst what is in the pattern for Flexible times with the Code `Hms`.
   * Check carefully to see whether you should change the separator or change what is in the base.

**Time ┃ H.mm ┃ Numeric time separator «:» **in pattern** conflicts with «.», the default for _Date_**
   * This warning is on the other side of the coin; it is on a pattern that contains Hm, hm, ms, or ms (thus including Hms, hms, hmv, etc.)
   * Look at the pattern carefully to see whether you need to change it for consistency across all the related Codes.

### Interval Formats

**Bhm/h ┃ hh:mm B – hh:mm B ┃ Conflicts with «hh:mm – hh:mm B» from _Bhm_; diffs=\[fewer\]; samples=«09:35 die oggend – 10:40 die oggend», «09:35 – 10:40 die oggend»**

There is also a warning for Intervals, to help identify where there may be unintentional inconsistencies between the interval formats and the related flexible format.
The message has 4 different pieces of information.
Here is a summary, and what it means for you is below that

* «…» is a _constructed interval format_.
* _…_ is a link to the Flexible format used to construct it.
(Note: the new interval separators are also used in the construction.)
* diffs=\[…] provides details about the differences
* samples=«…» provides two examples of what this would look like:
    * the first is using the current interval format
    * the second is using the the constructed interval format.

Take an example like the following:

| Code | Available pattern | Interval Pattern | Constructed Interval Pattern |
| -- | -- | -- | -- |
| MMMMd/M | M月d日 | MMMM d – MMMM d	| M月d日～M月d日 |
| MMMMd/d | M月d日 | MMMM d–d | M月d日～d日 |

The **Interval Pattern** in each case is unexpectedly different than the **Available pattern** for "MMMMd".
To help with this, a warning is given when it is different than a _constructed interval pattern_.
That warning will provide the constructed pattern, plus status and samples.

Now, the constructed pattern might not be right for your locale (sometimes it needs a human touch!),
but can reveal when you can

1. fix inconsistencies with the available formats, or
2. restructure the pattern to get it shorter or clearer, or
3. fix the related flexible format used to create the constructed pattern.

So please look at the warnings for _each_ item in the Flexible Date or Time formats,
and decide which of the above is appropriate. 

NOTE: There are cases where the constructed pattern is wrong and you will need to [add or vote for the correct value][] for your locale.
For example

The **samples** are probably the most useful, but you may also find the **status** useful.
Here is a list of the meanings.

| Abbreviation | Description of the differences |
| :---- | :---- |
| fewer | for prefix or suffix, constructed has fewer fields |
| more | for prefix or suffix, constructed has more fields |
| sep | the two separators are different; eg d-d vs d – d vs d～d |
| lit | two fields are different literals strings (not placeholders like MM); eg ' de ' vs ' d’ ' |
| type | two fields have different types; eg y vs M or y vs ' de '; can be caused by fields in different order |
| num | two fields have the same type, but one is numeric and the other isn’t; eg MMM vs M |
| width | two fields have the same type & numeric status, but are of different lengths; eg MMMM vs MMM, or d vs dd |
| other | two fields have the same type, numeric status, and length; eg L vs M |

### Flexible Formats

**full ┃ EEE, d, MMMM, y ┃ date-FULL → «EEE, d, MMMM, y» doesn't match any of the corresponding flexible skeletons: \[yMMMMEd → «y MMMM d, E» or yMMMEd → «E, MMM d, y»\], eg _yMMMEd_**
   * The "stock" date or time format (for the 4 different widths: short, medium, long, full)
has a format that doesn't match what is in the availble formats.
Fix either this or the flexible pattern.

**GyMMMd ┃ dd MMM y G ┃ «dd MMM y G» ⊅ «d MMM y»: the pattern for GyMMMd should contain the pattern at _yMMMd_**
   * There are 5 fields (G, E, B, a, and v) that are normally prefixed or suffixed to a core pattern.
   * For example, in the majority of locales, if a Code like yMMM has a pattern «MMM y»,
then a Code like GyMMM will have a pattern like «MMM y G» or «G MMM y».
That is, the literals in the pattern (the spaces in this example) may vary by locale,
but the order of the placeholders in the core is retained.
   * Look carefully at the two patterns in the two different rows
to see if it would be better (or at least as good) to change the order of one or the other.

  
[add or vote for the correct value]: /translation/getting-started/guide#how-to-vote 

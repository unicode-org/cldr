---
title: Date & Time
---

# Date & Time

Date and time data provides information for formatting dates, times, timezones, datetime intervals (eg, Dec 15-18), and so on.
See the subpages in the sidebar.

## Error/warning messages 🚨 

The following are some of the error/warning messages you might see, and how to handle them.
The underlined portion of each message is the part that will change depending on which row you are on.
The messages might not be exactly what you see on the screen,
because parts of the message are customized.
It will open up in a separate window so that you can compare side-by-side.

### Numeric date separators

1. Numeric date separator conflicts with «/» from the base «d/M/y» at 🔗yMd🔗
   * The numeric date separator should be consistent with what is in the pattern for Flexible dates with the Code yMd.
   * That is normally also the time separator for Codes that contain Md, and yM.
However, some locales change the separator if there are only two placeholders instead of three.
   * Check carefully to see whether you should change the separator or change what is in the base or other related Flexible time Codes.
1. Numeric date separator «/» in pattern conflicts with «.», the default for 🔗Date🔗
   * This warning is on the other side of the coin; it is on a **pattern** that contains Md, and yM (thus including MEd, GyM, etc.)
   * Look at the pattern carefully to see whether you need to change it for consistency across all the related Codes.

## Numeric time separators

1. Numeric time separator conflicts with «:» from the base «H:mm:ss» at 🔗Hms🔗
   * The numeric time separator conflicst what is in the pattern for Flexible times with the Code `Hms`.
   * Check carefully to see whether you should change the separator or change what is in the base.
2. Numeric time separator «:» in pattern conflicts with «.», the default for 🔗Date🔗
   * This warning is on the other side of the coin; it is on a **pattern** that contains Hm, hm, ms, or ms (thus including Hms, hms, hmv, etc.)
   * Look at the pattern carefully to see whether you need to change it for consistency across all the related Codes.

### Interval Formats

1. Conflicts with «d. – d.» from 🔗d🔗; diffs=\[fewer]; samples=«25. – 26.», «25.–26.»

There is also new warning for Intervals, to help you get consistency _among_ the interval formats, and _with_ each associated flexible format.
The message has 4 different pieces of information.
Here is a summary, and what it means for you is below that

* «…» is a _constructed interval format_.
* 🔗…🔗 is a link to the Flexible format used to construct it.
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

NOTE: There are cases where the constructed pattern will just be wrong.
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
   
1. date-SHORT → “dd.MM.yy” doesn't match the corresponding flexible skeleton: yMd → “d. M. y”. 
   * The "stock" date or time format (for the 4 different widths: short, medium, long, full)
has a format that doesn't match what is in the availble formats.
Fix either this or the flexible pattern.
2. “E, d. M. y G” ⊅ “d. M. y GGGGG”: the pattern for GyMEd should contain the pattern at 🔗GyMd🔗
   * There are 5 fields (G, E, B, a, and v) that are normally prefixed or suffixed to a core pattern.
   * For example, in the majority of locales, if a Code like yMMM has a pattern «MMM y»,
then a Code like GyMMM will have a pattern like «MMM y G» or «G MMM y».
That is, the literals in the pattern (the spaces in this example) may vary by locale,
but the order of the placeholders in the core is retained.
   * Look carefully at the two patterns in the two different rows
to see if it would be better (or at least as good) to change the order of one or the other.
3. Unexpected order of era/year. Expected era then year, but got field between era and year in «G M/y» for gregorian/GyM — see 🔗Info Hub🔗
   * In most locales, the era placeholder (G) is adjacent to the year placeholder (y), either before or after.
   * Your locale might be an exceptional one, but check carefully to see whether you should move the G.

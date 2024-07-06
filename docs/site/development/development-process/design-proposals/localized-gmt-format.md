---
title: Localized GMT Format
---

# Localized GMT Format

### Tickets

[#3665](http://unicode.org/cldr/trac/ticket/3665) Additional time zone offset second field

[#5382](http://unicode.org/cldr/trac/ticket/5382) Add short localized GMT format

### Requirements

- Many zones in the IANA time zone database use LMT as the initial rule. LMT is calculated from longitude of each location and has non-zero seconds offset. For example, America/Los\_Angeles uses -7:52:58 as the initial UTC offset. At this moment, CLDR does not have a pattern including seconds field.
- Localized GMT format is used as the final fallback of other name types. Other name types have short/long variation, but localized GMT format does not have such variation. In many cases, UTC offsets can be represented by integer hours, and offset minutes field would be redundant when shorter format is desired.

### Current Implementation

In CLDR 22, elements used for localized GMT format are below:

- \<hourFormat> Format patterns used for representing UTC offset. This item is a single string containing two patterns, one for positive offset and another for negative offset, separated by semicolon (;). For example, "+HH:mm;-HH:mm". Each pattern must contain "H" (0-based 24 hours field) and "m" (minutes field).
- \<gmtFormat> Message format pattern such as "GMT{0}" used for localized GMT format. The variable part is replaced with UTC offset representation created by \<hourFormat> above.
- \<gmtZeroFormat> The string used for UTC (GMT) itself, such as "GMT". The string is used only when UTC offset is 0. 

### Proposed Changes

Below are the high level overview of the changes in this proposal

- Deprecate \<hourFormat> element
- Introduce new \<gmtOffsetPattern> element, with type attribute representing combinations - ( h | hm | hms ). For example, \<gmtOffsetPattern type="hm">+H:m;-H:m\</gmtOffsetPattern>
- Introduce new \<gmtOffsetSeparator> to store a locale specific separator used for offset patterns. Character colon (:) is reserved in \<gmtOffsetPattern> patterns for locale specific separator and actual pattern is produced by replacing colon (:) with the separator character specified by \<gmtOffsetSeparator> element.

With above change, root.xml would be changed from

**Old:**

&emsp;\<hourFormat>+HH:mm;-HH:mm\</hourFormat>

**New:**

&emsp;\<gmtOffsetPattern type="h">+H;-H\</gmtOffsetPattern>

&emsp;\<gmtOffsetPattern type="hm">+H:m;-H:m\</gmtOffsetPattern>

&emsp;\<gmtOffsetPattern type="hms">+H:m:s;-H:m:s\</gmtOffsetPattern>

&emsp;\<gmtOffsetSeparator>:\</gmtOffsetSeparator>

The table below illustrates the behavior of long / short format, with the root data above.

| UTC Offset |  Width |  Output |  Comment |
|---|---|---|---|
|  -8:00:00 |  long |  GMT-08:00 |  The negative pattern from &lt;gmtOffsetPattern type="hm"&gt;, interpret 'H' as fixed 2 digits hour, replace ':' with &lt;gmtOffsetSeparator&gt; |
|   |  short |  GMT-8 |  The negative pattern from &lt;gmtOffsetPattern type"h"&gt; |
|  -8:30:00 |  long |  GMT-08:30 |  The negative pattern from &lt;gmtOffsetPattern type="hm"&gt;, interpret 'H' as fixed 2 digits hour, replace ':' with &lt;gmtOffsetSeparator&gt; |
|   |  short |  GMT-8:30 |  The negative pattern from &lt;gmtOffsetPattern type="hm"&gt;, interpret 'H' as variable width hour, replace ':' with &lt;gmtOffsetSeparator&gt; |
|  -8:23:45 |  long |  GMT-08:23:45 |  The negative pattern from &lt;gmtOffsetPattern type="hms"&gt;, interpret 'H' as fixed 2 digits hour, replace ':' with &lt;gmtOffsetSeparator&gt; |
|   |  short |  GMT-8:23:45 |  The negative pattern from &lt;gmtOffsetPattern type="hms"&gt;, interpret 'H' as variable width hour, replace ':' with &lt;gmtOffsetSeparator&gt; |

- "long" format always uses zero-padded 2 digits for offset hours field, such as "00", "08", "11".
- "long" format does not use \<gmtOffsetPattern type="h">.
- With above two, "long" format is expected to generates fixed length outputs practically (non-zero seconds offset is not used for modern dates).
- "short" format always uses shortest offset hours field, such as "0", "8", "11".
- "short" format uses the shortest pattern (h \< hm \< hms) without offset data loss.

Design considerations

- \<gmtOffsetPattern> uses single "H", "m", "s", because they just indicate the disposition of these fields. "H" is interpreted as date format pattern "HH" or "H" depending on the width context.
- Many locales simply use the root data. Some locale may override only \<gmtOffsetSeparator>.
- Some existing locale data do not use any separators (e.g. zh.xml). This can be represented by \<gmtOffsetSeparator> to be empty string. However, empty string as data does not fit well to CLDR structure, so such locale data require to provide at least \<gmtOffsetPattern type="hm"> and \<gmtOffsetPattern type="hms">.
- Some existing locale data uses U+2212 MINUS SIGN instead of U+002D HYPHEN-MINUS. These locales need to provide all of \<gmtOffsetPattern> types.

### Open Issues

1. Distinction of H and HH in the current locale data.

Some locales currently use single "H" in \<hourFormat>.

&emsp;\<hourFormat>+H:mm;-H:mm\</hourFormat> (cs.xml)

&emsp;\<hourFormat>+H.mm;-H.mm\</hourFormat> (fi.xml)

This proposal is trying to set a new assumption that "long" format to be practically fixed length, and long format always place leading zero for single digit hour value. If this assumption is not acceptable by these locales, then the design must be changed to allow "HH" or "H" in \<gmtOffsetPattern> element, then "short" format to interpret "HH" as "H" (opposite way). However, I don't think this is the case.

2. Needs for long/short message pattern?

Although, [ticket #5382](http://unicode.org/cldr/trac/ticket/5382) mentioned about offset part of localized GMT format, some locales may want to have two \<gmtFormat> data in different length. For example,

&emsp;\<gmtFormat>Гриинуич{0}\</gmtFormat> (bg.xml)

&emsp;\<gmtFormat>গ্রীনিচ মান সময় {0}\</gmtFormat> (bn.xml)

There are some locales using relatively long patterns. If long/short distinction is given, these locales may want to provide shorter format such as "UTC{0}".

3. Impacts in CLDR ST

Because of another level of abstraction (separator, actual pattern width by context), this proposal may need a little bit more work on CLDR ST.

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
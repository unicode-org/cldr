## <a name="Date_Format_Patterns" id="Date_Format_Patterns" href="#Date_Format_Patterns">Date Format Patterns</a>

A date pattern is a character string consisting of two types of elements:

* _Pattern fields_, which repeat a specific _pattern character_ one or more times. These fields are replaced with date and time data from a calendar when formatting, or used to generate data for a calendar when parsing. Currently, A..Z and a..z are reserved for use as pattern characters (unless they are quoted, see next item). The pattern characters currently defined, and the meaning of different fields lengths for then, are listed in the Date Field Symbol Table below.
* Literal text, which is output as-is when formatting, and must closely match when parsing. Literal text can include:
  * Any characters other than A..Z and a..z, including spaces and punctuation.
  * Any text between single vertical quotes ('xxxx'), which may include A..Z and a..z as literal text.
  * Two adjacent single vertical quotes (''), which represent a literal single quote, either inside or outside quoted text.

The following are examples:

###### <a name="Date_Format_Pattern_Examples" id="Date_Format_Pattern_Examples" href="#Date_Format_Pattern_Examples">Table: Date Format Pattern Examples</a>

| Pattern | Result (in a particular locale) |
| ------- | ------------------------------- |
| yyyy.MM.dd G 'at' HH:mm:ss zzz | 1996.07.10 AD at 15:08:56 PDT |
| EEE, MMM d, ''yy | Wed, July 10, '96 |
| h:mm a | 12:08 PM |
| hh 'o''clock' a, zzzz | 12 o'clock PM, Pacific Daylight Time |
| K:mm a, z | 0:00 PM, PST |
| yyyyy.MMMM.dd GGG hh:mm aaa | 01996.July.10 AD 12:08 PM |

_When parsing using a pattern, a lenient parse should be used; see [Parsing Dates and Times](#Parsing_Dates_Times)._

```dtd
<!ATTLIST pattern numbers CDATA #IMPLIED >
```

* The `numbers` attribute is used to indicate that numeric quantities in the pattern are to be rendered using a numbering system other than the default numbering system defined for the given locale. The attribute can be in one of two forms. If the alternate numbering system is intended to apply to ALL numeric quantities in the pattern, then simply use the numbering system ID as found in [Numbering Systems](tr35-numbers.md#Numbering_Systems). To apply the alternate numbering system only to a single field, the syntax ``<letter>`=`<numberingSystem>`` can be used one or more times, separated by semicolons.


Examples:

```xml
<pattern numbers="hebr">dd/mm/yyyy</pattern>
<!-- Use Hebrew numerals to represent numbers in the Hebrew calendar, where "latn" numbering system is the default -->

<pattern numbers="y=hebr">dd/mm/yyyy</pattern>
<!-- Same as above, except that ONLY the year value would be rendered in Hebrew -->

<pattern numbers="d=thai;m=hans;y=deva">dd/mm/yyyy</pattern>
<!-- Illustrates use of multiple numbering systems for a single pattern. -->
```

**Pattern fields and the Date Field Symbol Table**

* The Date Field Symbol Table below shows the pattern characters (Sym.) and associated fields used in date patterns. The length of the pattern field is related to the length and style used to format the data item. For numeric-only fields, the field length typically indicates the minimum number of digits that should be used to display the value (zero-padding as necessary). As an example using pattern character ‘H’ for hour (24-hour cycle) and values 5 and 11, a field “H” should produce formatted results “5” and “11” while a field “HH” should produce formatted results “05” and “11”. For alphanumeric fields (such as months) and alphabetic-only fields (such as era names), the relationship between field length and formatted result may be more complex. Typically this is as follows:


<!-- HTML: spanned rows, spanned columns -->
 <table>
<tbody>
    <tr><th>Pattern field length</th><th>Typical style, alphanumeric item</th><th>Typical style, alpha-only item</th></tr>
    <tr><td>1</td><td>Numeric, 1-2 digits (e.g. M)</td><td rowspan="3">Abbreviated (e.g. E, EE, EEE)</td></tr>
    <tr><td>2</td><td>Numeric, 2 digits (e.g. MM)</td></tr>
    <tr><td>3</td><td>Abbreviated (e.g. MMM)</td></tr>
    <tr><td>4</td><td colspan="2">Wide / Long / Full (e.g. MMMM, EEEE)</td></tr>
    <tr><td>5</td><td colspan="2">Narrow (e.g. MMMMM, EEEEE)<br/>(The counter-intuitive use of 5 letters for this is forced by backwards compatibility)</td></tr>
    <tr><td>&gt;16</td><td colspan="2">Private Use<br/>(Reserved for use by implementations using CLDR; will never be otherwise used by CLDR.)</td></tr>
</tbody>
</table>

Notes for the table below:

* Any sequence of pattern characters other than those listed below is invalid. Invalid pattern fields should be handled for formatting and parsing as described in [Handling Invalid Patterns](tr35.md#Invalid_Patterns).
* The examples in the table below are merely illustrative and may not reflect current actual data.

###### <a name="Date_Field_Symbol_Table" id="Date_Field_Symbol_Table" href="#Date_Field_Symbol_Table">Table: Date Field Symbol Table</a>

<!-- HTML: spanned rows, spanned columns, vertical header cells -->
<table><tbody>
<tr><th>Field<br/>Type</th><th>Sym.</th><th>Field<br/>Patterns</th><th>Examples</th><th colspan="2">Description</th></tr>

<!-- == == == ERA == == == -->
<tr><th rowspan="3"><a name="dfst-era" href="#dfst-era">era</a></th><td rowspan="3">G</td><td>G..GGG</td><td>AD<br/>[variant: CE]</td><td>Abbreviated</td><td rowspan="3">Era name. Era string for the current date.</td></tr>
    <tr><td>GGGG</td><td>Anno Domini<br/>[variant: Common Era]</td><td>Wide</td></tr>
    <tr><td>GGGGG</td><td>A</td><td>Narrow</td></tr>

<!-- == == == YEAR == == == -->
<tr><th rowspan="15"><a name="dfst-year" href="#dfst-year">year</a><a name="Year_Length_Examples"></a></th><td rowspan="5">y</td><td>y</td><td>2, 20, 201, 2017, 20173</td>
        <td rowspan="5" colspan="2">Calendar year (numeric). In most cases the length of the y field specifies the minimum number of digits to display, zero-padded as necessary; more digits will be displayed if needed to show the full year.
                                    However, “yy” requests just the two low-order digits of the year, zero-padded as necessary. For most use cases, “y” or “yy” should be adequate.</td></tr>
    <tr><td>yy</td><td>02, 20, 01, 17, 73</td></tr>
    <tr><td>yyy</td><td>002, 020, 201, 2017, 20173</td></tr>
    <tr><td>yyyy</td><td>0002, 0020, 0201, 2017, 20173</td></tr>
    <tr><td>yyyyy+</td><td>...</td></tr>
    <!--  Y  -->
    <tr><td rowspan="5">Y</td><td>Y</td><td>2, 20, 201, 2017, 20173</td>
        <td rowspan="5" colspan="2">Year in “Week of Year” based calendars in which the year transition occurs on a week boundary; may differ from calendar year ‘y’ near a year transition.
                                    This numeric year designation is used in conjunction with pattern character ‘w’ in the ISO year-week calendar as defined by ISO 8601, but can be used in non-Gregorian based calendar systems where week date processing is desired.
                                    The field length is interpreted in the same was as for ‘y’; that is, “yy” specifies use of the two low-order year digits, while any other field length specifies a minimum number of digits to display.</td></tr>
    <tr><td>YY</td><td>02, 20, 01, 17, 73</td></tr>
    <tr><td>YYY</td><td>002, 020, 201, 2017, 20173</td></tr>
    <tr><td>YYYY</td><td>0002, 0020, 0201, 2017, 20173</td></tr>
    <tr><td>YYYYY+</td><td>...</td></tr>
    <!--  u  -->
    <tr><td>u</td><td>u+</td><td>4601</td>
        <td colspan="2">Extended year (numeric). This is a single number designating the year of this calendar system, encompassing all supra-year fields.
                        For example, for the Julian calendar system, year numbers are positive, with an era of BCE or CE. An extended year value for the Julian calendar system assigns positive values to CE years and negative values to BCE years, with 1 BCE being year 0.
                        For ‘u’, all field lengths specify a minimum number of digits; there is no special interpretation for “uu”.</td></tr>
    <!--  U  -->
    <tr><td rowspan="3">U</td><td>U..UUU</td><td>甲子</td><td>Abbreviated</td>
        <td rowspan="3">Cyclic year name. Calendars such as the Chinese lunar calendar (and related calendars) and the Hindu calendars use 60-year cycles of year names.
                        If the calendar does not provide cyclic year name data, or if the year value to be formatted is out of the range of years for which cyclic name data is provided, then numeric formatting is used (behaves like 'y').<br/>
                        Currently the data only provides abbreviated names, which will be used for all requested name widths.</td></tr>
    <tr><td>UUUU</td><td>甲子 [for now]</td><td>Wide</td></tr>
    <tr><td>UUUUU</td><td>甲子 [for now]</td><td>Narrow</td></tr>
    <!--  r  -->
    <tr><td>r</td><td>r+</td><td>2017</td>
        <td colspan="2">Related Gregorian year (numeric).
                        For non-Gregorian calendars, this corresponds to the extended Gregorian year in which the calendar’s year begins.
                        Related Gregorian years are often displayed, for example, when formatting dates in the Japanese calendar — e.g. “2012(平成24)年1月15日” — or in the Chinese calendar — e.g. “2012壬辰年腊月初四”.
                        The related Gregorian year is usually displayed using the "latn" numbering system, regardless of what numbering systems may be used for other parts of the formatted date.
                        If the calendar’s year is linked to the solar year (perhaps using leap months), then for that calendar the ‘r’ year will always be at a fixed offset from the ‘u’ year.
                        For the Gregorian calendar, the ‘r’ year is the same as the ‘u’ year. For ‘r’, all field lengths specify a minimum number of digits; there is no special interpretation for “rr”.</td></tr>

<!-- == == == QUARTER == == == -->
<tr><th rowspan="10"><a name="dfst-quarter" id="dfst-quarter" href="#dfst-quarter">quarter</a></th><td rowspan="5">Q</td><td>Q</td><td>2</td><td>Numeric: 1 digit</td><td rowspan="5">Quarter number/name.</td></tr>
    <tr><td>QQ</td><td>02</td><td>Numeric: 2 digits + zero pad</td></tr>
    <tr><td>QQQ</td><td>Q2</td><td>Abbreviated</td></tr>
    <tr><td>QQQQ</td><td>2nd quarter</td><td>Wide</td></tr>
    <tr><td>QQQQQ</td><td>2</td><td>Narrow</td></tr>
    <!--  q  -->
    <tr><td rowspan="5">q</td><td>q</td><td>2</td><td>Numeric: 1 digit</td><td rowspan="5"><b>Stand-Alone</b> Quarter number/name.</td></tr>
    <tr><td>qq</td><td>02</td><td>Numeric: 2 digits + zero pad</td></tr>
    <tr><td>qqq</td><td>Q2</td><td>Abbreviated</td></tr>
    <tr><td>qqqq</td><td>2nd quarter</td><td>Wide</td></tr>
    <tr><td>qqqqq</td><td>2</td><td>Narrow</td></tr>

<!-- == == == MONTH == == == -->
<tr><th rowspan="11"><a name="dfst-month" id="dfst-month" href="#dfst-month">month</a></th><td rowspan="5">M</td><td>M</td><td>9, 12</td><td>Numeric: minimum digits</td><td rowspan="5"><b>Format</b> style month number/name: The format style name is an additional form of the month name (besides the stand-alone style) that can be used in contexts where it is different than the stand-alone form. For example, depending on the language, patterns that combine month with day-of month (e.g. "d MMMM") may require the month to be in genitive form. See discussion of <a href="#months_days_quarters_eras">month element</a>. If a separate form is not needed, the format and stand-alone forms can be the same.</td></tr>
    <tr><td>MM</td><td>09, 12</td><td>Numeric: 2 digits, zero pad if needed</td></tr>
    <tr><td>MMM</td><td>Sep</td><td>Abbreviated</td></tr>
    <tr><td>MMMM</td><td>September</td><td>Wide</td></tr>
    <tr><td>MMMMM</td><td>S</td><td>Narrow</td></tr>
    <!--  L  -->
    <tr><td rowspan="5">L</td><td>L</td><td>9, 12</td><td>Numeric: minimum digits</td><td rowspan="5"><b>Stand-Alone</b> month number/name: For use when the month is displayed by itself, and in any other date pattern (e.g. just month and year, e.g. "LLLL y") that shares the same form of the month name. For month names, this is typically the nominative form. See discussion of <a href="#months_days_quarters_eras">month element</a>.</td></tr>
    <tr><td>LL</td><td>09, 12</td><td>Numeric: 2 digits, zero pad if needed</td></tr>
    <tr><td>LLL</td><td>Sep</td><td>Abbreviated</td></tr>
    <tr><td>LLLL</td><td>September</td><td>Wide</td></tr>
    <tr><td>LLLLL</td><td>S</td><td>Narrow</td></tr>
    <!--  l  -->
    <tr><td>l</td><td>l</td><td>[nothing]</td>
        <td colspan="2">This pattern character is deprecated, and should be ignored in patterns.
                        It was originally intended to be used in combination with M to indicate placement of the symbol for leap month in the Chinese calendar.
                        Placement of that marker is now specified using locale-specific &lt;monthPatterns&gt; data, and formatting and parsing of that marker should be handled as part of supporting the regular M and L pattern characters.</td></tr>

<!-- == == == WEEK == == == -->
<tr><th rowspan="3"><a name="dfst-week" id="dfst-week" href="#dfst-week">week</a></th><td rowspan="2">w</td><td>w</td><td>8, 27</td><td>Numeric: minimum digits</td><td rowspan="2">Week of Year (numeric). When used in a pattern with year, use ‘Y’ for the year field instead of ‘y’.</td></tr>
    <tr><td>ww</td><td>08, 27</td><td>Numeric: 2 digits, zero pad if needed</td></tr>
    <!--  W  -->
    <tr><td>W</td><td>W</td><td>3</td><td>Numeric: 1 digit</td><td>Week of Month (numeric)</td></tr>

<!-- == == == DAY == == == -->
<tr><th rowspan="5"><a name="dfst-day" id="dfst-day" href="#dfst-day">day</a></th><td rowspan="2">d</td><td>d</td><td>1</td><td>Numeric: minimum digits</td><td rowspan="2">Day of month (numeric).</td></tr>
    <tr><td>dd</td><td>01</td><td>Numeric: 2 digits, zero pad if needed</td></tr>
    <tr><td>D</td><td>D...DDD</td><td>345</td><td colspan="2">Day of year (numeric). The field length specifies the minimum number of digits, with zero-padding as necessary.</td></tr>
    <tr><td>F</td><td>F</td><td>2</td><td colspan="2">Day of Week in Month (numeric). The example is for the 2nd Wed in July</td></tr>
    <tr><td>g</td><td>g+</td><td>2451334</td>
        <td colspan="2">Modified Julian day (numeric).
                        This is different from the conventional Julian day number in two regards.
                        First, it demarcates days at local zone midnight, rather than noon GMT.
                        Second, it is a local number; that is, it depends on the local time zone.
                        It can be thought of as a single number that encompasses all the date-related fields.
                        The field length specifies the minimum number of digits, with zero-padding as necessary.</td></tr>

<!-- == == == WEEKDAY == == == -->
<tr><th rowspan="15"><a name="dfst-weekday" id="dfst-weekday" href="#dfst-weekday">week<br/>day</a></th><td rowspan="4">E</td><td>E..EEE</td><td>Tue</td><td>Abbreviated</td><td rowspan="4">Day of week name, format style.</td></tr>
    <tr><td>EEEE</td><td>Tuesday</td><td>Wide</td></tr>
    <tr><td>EEEEE</td><td>T</td><td>Narrow</td></tr>
    <tr><td>EEEEEE</td><td>Tu</td><td>Short</td></tr>
    <!--  e  -->
    <tr><td rowspan="6">e</td><td>e</td><td>2</td><td>Numeric: 1 digit</td>
        <td rowspan="6">Local day of week number/name, format style.
                        Same as E except adds a numeric value that will depend on the local starting day of the week.
                        For this example, Monday is the first day of the week.</td></tr>
    <tr><td>ee</td><td>02</td><td>Numeric: 2 digits + zero pad</td></tr>
    <tr><td>eee</td><td>Tue</td><td>Abbreviated</td></tr>
    <tr><td>eeee</td><td>Tuesday</td><td>Wide</td></tr>
    <tr><td>eeeee</td><td>T</td><td>Narrow</td></tr>
    <tr><td>eeeeee</td><td>Tu</td><td>Short</td></tr>
    <!--  c  -->
    <tr><td rowspan="5">c</td><td>c..cc</td><td>2</td><td>Numeric: 1 digit</td><td rowspan="5"><b>Stand-Alone</b> local day of week number/name.</td></tr>
    <tr><td>ccc</td><td>Tue</td><td>Abbreviated</td></tr>
    <tr><td>cccc</td><td>Tuesday</td><td>Wide</td></tr>
    <tr><td>ccccc</td><td>T</td><td>Narrow</td></tr>
    <tr><td>cccccc</td><td>Tu</td><td>Short</td></tr>

<!-- == == == PERIOD == == == -->
<tr><th rowspan="9"><a name="dfst-period" id="dfst-period" href="#dfst-period">period</a></th><td rowspan="3">a</td><td>a..aaa</td><td>am. [e.g. 12 am.]</td><td>Abbreviated</td>
        <td rowspan="3"><strong>AM, PM<br/></strong>May be upper or lowercase depending on the locale and other options.
                                                    The wide form may be the same as the short form if the “real” long form (eg <em>ante meridiem</em>) is not customarily used.
                                                    The narrow form must be unique, unlike some other fields.
                                                    See also <a href="#Parsing_Dates_Times">Parsing Dates and Times</a>.</td></tr>
    <tr><td>aaaa</td><td>am. [e.g. 12 am.]</td><td>Wide</td></tr>
    <tr><td>aaaaa</td><td>a [e.g. 12a]</td><td>Narrow</td></tr>
    <!--  b  -->
    <tr><td rowspan="3">b</td><td>b..bbb</td><td>mid. [e.g. 12 mid.]</td><td>Abbreviated</td>
        <td rowspan="3"><strong>am, pm, noon, midnight</strong><br/>May be upper or lowercase depending on the locale and other options.
                        If the locale doesn't have the notion of a unique "noon" = 12:00, then the PM form may be substituted.
                        Similarly for "midnight" = 00:00 and the AM form.
                        The narrow form must be unique, unlike some other fields.</td></tr>
    <tr><td>bbbb</td><td>midnight<br/>[e.g. 12 midnight]</td><td>Wide</td></tr>
    <tr><td>bbbbb</td><td>md [e.g. 12 md]</td><td>Narrow</td></tr>
    <!--  B  -->
    <tr><td rowspan="3">B</td><td>B..BBB</td><td>at night<br/>[e.g. 3:00 at night]</td><td>Abbreviated</td>
        <td rowspan="3"><strong>flexible day periods</strong><br/>
                        May be upper or lowercase depending on the locale and other options.
                        Often there is only one width that is customarily used.</td></tr>
    <tr><td>BBBB</td><td>at night<br/>[e.g. 3:00 at night]</td><td>Wide</td></tr>
    <tr><td>BBBBB</td><td>at night<br/>[e.g. 3:00 at night]</td><td>Narrow</td></tr>

<!-- == == == HOUR == == == -->
<tr><th rowspan="22"><a name="dfst-hour" id="dfst-hour" href="#dfst-hour">hour</a></th><td rowspan="2">h</td><td>h</td><td>1, 12</td><td>Numeric: minimum digits</td>
        <td rowspan="2">Hour [1-12]. When used in skeleton data or in a skeleton passed in an API for flexible date pattern generation, it should match the 12-hour-cycle format preferred by the locale (h or K); it should not match a 24-hour-cycle format (H or k).</td></tr>
<tr><td>hh</td><td>01, 12</td><td>Numeric: 2 digits, zero pad if needed</td></tr>
    <!--  H  -->
    <tr><td rowspan="2">H</td><td>H</td><td>0, 23</td><td>Numeric: minimum digits</td>
        <td rowspan="2">Hour [0-23]. When used in skeleton data or in a skeleton passed in an API for flexible date pattern generation, it should match the 24-hour-cycle format preferred by the locale (H or k); it should not match a 12-hour-cycle format (h or K).</td></tr>
    <tr><td>HH</td><td>00, 23</td><td>Numeric: 2 digits, zero pad if needed</td></tr>
    <!--  K  -->
    <tr><td rowspan="2">K</td><td>K</td><td>0, 11</td><td>Numeric: minimum digits</td>
        <td rowspan="2">Hour [0-11]. When used in a skeleton, only matches K or h, see above.</td></tr>
    <tr><td>KK</td><td>00, 11</td><td>Numeric: 2 digits, zero pad if needed</td></tr>
    <!--  k  -->
    <tr><td rowspan="2">k</td><td>k</td><td>1, 24</td><td>Numeric: minimum digits</td>
        <td rowspan="2">Hour [1-24]. When used in a skeleton, only matches k or H, see above.</td></tr>
    <tr><td>kk</td><td>01, 24</td><td>Numeric: 2 digits, zero pad if needed</td></tr>
    <!--  j  -->
    <tr><td rowspan="6">j</td><td>j</td><td>8<br/>8 AM<br/>13<br/>1 PM</td><td>Numeric hour (minimum digits), abbreviated dayPeriod if used</td>
        <td rowspan="6"><em><strong>Input skeleton symbol</strong></em><br/>
                        It must not occur in pattern or skeleton data.
                        Instead, it is reserved for use in skeletons passed to APIs doing flexible date pattern generation.
                        In such a context, it requests the preferred hour format for the locale (h, H, K, or k), as determined by the <strong>preferred</strong> attribute of the <strong>hours</strong> element in supplemental data.
                        In the implementation of such an API, 'j' must be replaced by h, H, K, or k before beginning a match against availableFormats data.<br/>
                        Note that use of 'j' in a skeleton passed to an API is the only way to have a skeleton request a locale's preferred time cycle type (12-hour or 24-hour).</td></tr>
    <tr><td>jj</td><td>08<br/>08 AM<br/>13<br/>01 PM</td><td>Numeric hour (2 digits, zero pad if needed), abbreviated dayPeriod if used</td></tr>
    <tr><td>jjj</td><td>8<br/>8 A.M.<br/>13<br/>1 P.M.</td><td>Numeric hour (minimum digits), wide dayPeriod if used</td></tr>
    <tr><td>jjjj</td><td>08<br/>08 A.M.<br/>13<br/>01 P.M.</td><td>Numeric hour (2 digits, zero pad if needed), wide dayPeriod if used</td></tr>
    <tr><td>jjjjj</td><td>8<br/>8a<br/>13<br/>1p</td><td>Numeric hour (minimum digits), narrow dayPeriod if used</td></tr>
    <tr><td>jjjjjj</td><td>08<br/>08a<br/>13<br/>01p</td><td>Numeric hour (2 digits, zero pad if needed), narrow dayPeriod if used</td></tr>
    <!--  J  -->
    <tr><td rowspan="2">J</td><td>J</td><td>8<br/>8</td><td>Numeric hour (minimum digits)</td>
        <td rowspan="2"><em><strong>Input skeleton symbol</strong></em><br/>It must not occur in pattern or skeleton data.
                        Instead, it is reserved for use in skeletons passed to APIs doing flexible date pattern generation.
                        In such a context, like 'j', it requests the preferred hour format for the locale (h, H, K, or k), as determined by the <strong>preferred</strong> attribute of the <strong>hours</strong> element in supplemental data.
                        However, unlike 'j', it requests no dayPeriod marker such as “am/pm” (it is typically used where there is enough context that that is not necessary).
                        For example, with "jmm", 18:00 could appear as “6:00 PM”, while with "Jmm", it would appear as “6:00” (no PM).</td></tr>
    <tr><td>JJ</td><td>08<br/>08</td><td>Numeric hour (2 digits, zero pad if needed)</td></tr>
    <!--  C  -->
    <tr><td rowspan="6">C</td><td>C</td><td>8<br/>8 (morning)</td><td>Numeric hour (minimum digits), abbreviated dayPeriod if used</td>
        <td rowspan="6"><em><strong>Input skeleton symbol</strong></em><br/>It must not occur in pattern or skeleton data.
                        Instead, it is reserved for use in skeletons passed to APIs doing flexible date pattern generation.
                        In such a context, like 'j', it requests the preferred hour format for the locale.
                        However, unlike 'j', it can also select formats such as hb or hB, since it is based not on the <strong>preferred</strong> attribute of the <strong>hours</strong> element in supplemental data, but instead on the first element of the <strong>allowed</strong> attribute (which is an ordered preferrence list).
                        For example, with "Cmm", 18:00 could appear as “6:00 in the afternoon”.</td></tr>
    <tr><td>CC</td><td>08<br/>08 (morning)</td><td>Numeric hour (2 digits, zero pad if needed), abbreviated dayPeriod if used</td></tr>
    <tr><td>CCC</td><td>8<br/>8 in the morning</td><td>Numeric hour (minimum digits), wide dayPeriod if used</td></tr>
    <tr><td>CCCC</td><td>08<br/>08 in the morning</td><td>Numeric hour (2 digits, zero pad if needed), wide dayPeriod if used</td></tr>
    <tr><td>CCCCC</td><td>8<br/>8 (morn.)</td><td>Numeric hour (minimum digits), narrow dayPeriod if used</td></tr>
    <tr><td>CCCCCC</td><td>08<br/>08 (morn.)</td><td>Numeric hour (2 digits, zero pad if needed), narrow dayPeriod if used</td></tr>

<!-- == == == MINUTE == == == -->
<tr><th rowspan="2"><a name="dfst-minute" id="dfst-minute" href="#dfst-minute">minute</a></th><td rowspan="2">m</td><td>m</td><td>8, 59</td><td>Numeric: minimum digits</td>
        <td rowspan="2">Minute (numeric). Truncated, not rounded.<br/></td></tr>
    <tr><td>mm</td><td>08, 59</td><td>Numeric: 2 digits, zero pad if needed</td></tr>

<!-- == == == SECOND == == == -->
<tr><th rowspan="4"><a name="dfst-second" id="dfst-second" href="#dfst-second">second</a></th><td rowspan="2">s</td><td>s</td><td>8, 12</td><td>Numeric: minimum digits</td>
        <td rowspan="2">Second (numeric). Truncated, not rounded.<br/></td></tr>
    <tr><td>ss</td><td>08, 12</td><td>Numeric: 2 digits, zero pad if needed</td></tr>
    <tr><td>S</td><td>S+</td><td>3456</td>
        <td colspan="2">Fractional Second (numeric).
                        Truncates, like other numeric time fields, but in this case to the number of digits specified by the field length.
                        (Example shows display using pattern SSSS for seconds value 12.34567)</td></tr>
    <tr><td>A</td><td>A+</td><td>69540000</td>
        <td colspan="2">Milliseconds in day (numeric).
                        This field behaves <i>exactly</i> like a composite of all time-related fields, not including the zone fields.
                        As such, it also reflects discontinuities of those fields on DST transition days. On a day of DST onset, it will jump forward.
                        On a day of DST cessation, it will jump backward.
                        This reflects the fact that it must be combined with the offset field to obtain a unique local time value.
                        The field length specifies the minimum number of digits, with zero-padding as necessary.</td></tr>

<!-- == == == SEPARATOR == == == -->
<tr><th><a name="dfst-sep" id="dfst-sep" href="#dfst-sep">sep.</a></th><td>(none def., see note)</td><td></td><td></td>
        <td colspan="2">Time separator.<br/><br/><span class="note"><b>Note:</b>
                        In CLDR 26 the time separator pattern character was specified to be COLON.
                        This was withdrawn in CLDR 28 due to backward compatibility issues, and no time separator pattern character is currently defined.</span><br/><br/>
                        Like the use of "," in number formats, this character in a date pattern is replaced with the corresponding number symbol which may depend on the numbering system.
                        For more information, see <em><strong>Part 3: <a href="tr35-numbers.md#Contents">Numbers</a></strong>, <a href="tr35-numbers.md#Number_Symbols">Number Symbols</a></em>.</td></tr>

<!-- == == == ZONE == == == -->
<tr><th rowspan="23"><a name="dfst-zone" id="dfst-zone" href="#dfst-zone">zone</a></th><td rowspan="2">z</td><td>z..zzz</td><td>PDT</td>
        <td colspan="2">The <i>short specific non-location format</i>. Where that is unavailable, falls back to the <i>short localized GMT format</i> ("O").</td></tr>
    <tr><td>zzzz</td><td>Pacific Daylight Time</td>
        <td colspan="2">The <i>long specific non-location format</i>.
                        Where that is unavailable, falls back to the <i>short localized GMT format</i>.</td></tr>
    <!--  Z  -->
    <tr><td rowspan="3">Z</td><td>Z..ZZZ</td><td>-0800</td>
        <td colspan="2">The <i>ISO8601 basic format</i> with hours, minutes and optional seconds fields.
                        The format is equivalent to RFC 822 zone format (when optional seconds field is absent).
                        This is equivalent to the "xxxx" specifier.</td></tr>
    <tr><td>ZZZZ</td><td>GMT-8:00</td>
        <td colspan="2">The <i>long localized GMT format</i>.
                        This is equivalent to the "OOOO" specifier.</td></tr>
    <tr><td>ZZZZZ</td><td>-08:00<br/>-07:52:58</td>
        <td colspan="2">The <i>ISO8601 extended format</i> with hours, minutes and optional seconds fields.
                        The ISO8601 UTC indicator "Z" is used when local time offset is 0.
                        This is equivalent to the "XXXXX" specifier.</td></tr>
    <!--  O  -->
    <tr><td rowspan="2">O</td><td>O</td><td>GMT-8</td><td colspan="2">The <i>short localized GMT format</i>.</td></tr>
    <tr><td>OOOO</td><td>GMT-08:00</td><td colspan="2">The <i>long localized GMT format</i>.</td></tr>
    <!--  v  -->
    <tr><td rowspan="2">v</td><td>v</td><td>PT</td>
        <td colspan="2">The <i>short generic non-location format</i>
                        Where that is unavailable, falls back to the <i>generic location format</i> ("VVVV"), then the <i>short localized GMT format</i> as the final fallback.</td></tr>
    <tr><td>vvvv</td><td>Pacific Time</td>
        <td colspan="2">The <i>long generic non-location format</i>.
                        Where that is unavailable, falls back to <i>generic location format</i> ("VVVV").</td></tr>
    <!--  V  -->
    <tr><td rowspan="4">V</td><td>V</td><td>uslax</td>
        <td colspan="2">The short time zone ID. Where that is unavailable, the special short time zone ID <i>unk</i> (Unknown Zone) is used.<br/>
                        <i><b>Note</b>: This specifier was originally used for a variant of the short specific non-location format, but it was deprecated in the later version of this specification.
                        In CLDR 23, the definition of the specifier was changed to designate a short time zone ID.</i></td></tr>
    <tr><td>VV</td><td>America/Los_Angeles</td><td colspan="2">The long time zone ID.</td></tr>
    <tr><td>VVV</td><td>Los Angeles</td>
        <td colspan="2">The exemplar city (location) for the time zone.
                        Where that is unavailable, the localized exemplar city name for the special zone <i>Etc/Unknown</i> is used as the fallback (for example, "Unknown Location").</td></tr>
    <tr><td>VVVV</td><td>Los Angeles Time</td>
        <td colspan="2">The <i>generic location format</i>.
                        Where that is unavailable, falls back to the <i>long localized GMT format</i> ("OOOO"; Note: Fallback is only necessary with a GMT-style Time Zone ID, like Etc/GMT-830.)<br/>
                        This is especially useful when presenting possible timezone choices for user selection, since the naming is more uniform than the "v" format.</td></tr>
    <!--  X  -->
    <tr><td rowspan="5">X</td><td>X</td><td>-08<br/>+0530<br/>Z</td>
        <td colspan="2">The <i>ISO8601 basic format</i> with hours field and optional minutes field.
                        The ISO8601 UTC indicator "Z" is used when local time offset is 0. (The same as x, plus "Z".)</td></tr>
    <tr><td>XX</td><td>-0800<br/>Z</td>
        <td colspan="2">The <i>ISO8601 basic format</i> with hours and minutes fields.
                        The ISO8601 UTC indicator "Z" is used when local time offset is 0. (The same as xx, plus "Z".)</td></tr>
    <tr><td>XXX</td><td>-08:00<br/>Z</td>
        <td colspan="2">The <i>ISO8601 extended format</i> with hours and minutes fields.
                        The ISO8601 UTC indicator "Z" is used when local time offset is 0. (The same as xxx, plus "Z".)</td></tr>
    <tr><td>XXXX</td><td>-0800<br/>-075258<br/>Z</td>
        <td colspan="2">The <i>ISO8601 basic format</i> with hours, minutes and optional seconds fields.
                        The ISO8601 UTC indicator "Z" is used when local time offset is 0. (The same as xxxx, plus "Z".)<br/>
                        <i><b>Note</b>: The seconds field is not supported by the ISO8601 specification.</i></td></tr>
    <tr><td>XXXXX</td><td>-08:00<br/>-07:52:58<br/>Z</td>
        <td colspan="2">The <i>ISO8601 extended format</i> with hours, minutes and optional seconds fields.
                        The ISO8601 UTC indicator "Z" is used when local time offset is 0. (The same as xxxxx, plus "Z".)<br/>
                        <i><b>Note</b>: The seconds field is not supported by the ISO8601 specification.</i></td></tr>
    <!--  x  -->
    <tr><td rowspan="5">x</td><td>x</td><td>-08<br/>+0530<br/>+00</td>
        <td colspan="2">The <i>ISO8601 basic format</i> with hours field and optional minutes field. (The same as X, minus "Z".)</td></tr>
    <tr><td>xx</td><td>-0800<br/>+0000</td>
        <td colspan="2">The <i>ISO8601 basic format</i> with hours and minutes fields. (The same as XX, minus "Z".)</td></tr>
    <tr><td>xxx</td><td>-08:00<br/>+00:00</td>
        <td colspan="2">The <i>ISO8601 extended format</i> with hours and minutes fields. (The same as XXX, minus "Z".)</td></tr>
    <tr><td>xxxx</td><td>-0800<br/>-075258<br/>+0000</td>
        <td colspan="2">The <i>ISO8601 basic format</i> with hours, minutes and optional seconds fields. (The same as XXXX, minus "Z".)<br/>
                        <i><b>Note</b>: The seconds field is not supported by the ISO8601 specification.</i></td></tr>
    <tr><td>xxxxx</td><td>-08:00<br/>-07:52:58<br/>+00:00</td>
        <td colspan="2">The <i>ISO8601 extended format</i> with hours, minutes and optional seconds fields. (The same as XXXXX, minus "Z".)<br/>
                        <i><b>Note</b>: The seconds field is not supported by the ISO8601 specification.</i></td></tr>
</tbody></table>

### <a name="Localized_Pattern_Characters" id="Localized_Pattern_Characters" href="#Localized_Pattern_Characters">Localized Pattern Characters (deprecated)</a>

* These are characters that can be used when displaying a date pattern to an end user. This can occur, for example, when a spreadsheet allows users to specify date patterns. Whatever is in the string is substituted one-for-one with the characters "GyMdkHmsSEDFwWahKzYeugAZvcLQqVUOXxr", with the above meanings. Thus, for example, if 'J' is to be used instead of 'Y' to mean Year (for Week of Year), then the string would be: "GyMdkHmsSEDFwWahKzJeugAZvcLQqVUOXxr".


* This element is deprecated. It is recommended instead that a more sophisticated UI be used for localization, such as using icons to represent the different formats (and lengths) in the [Date Field Symbol Table](#Date_Field_Symbol_Table).


### <a name="Date_Patterns_AM_PM" id="Date_Patterns_AM_PM" href="#Date_Patterns_AM_PM">AM / PM</a>

* Even for countries where the customary date format only has a 24 hour format, both the am and pm localized strings must be present and must be distinct from one another. Note that as long as the 24 hour format is used, these strings will normally never be used, but for testing and unusual circumstances they must be present.


### <a name="Date_Patterns_Eras" id="Date_Patterns_Eras" href="#Date_Patterns_Eras">Eras</a>

* There are only two values for era in the Gregorian calendar, with two common naming conventions (here in abbreviated form for English): "BC" and "AD", or "BCE" and "CE". These values can be translated into other languages, like "a.C." and "d.C." for Spanish, but there are no other eras in the Gregorian calendar. Other calendars have different numbers of eras. Care should be taken when translating the era names for a specific calendar.


### <a name="Date_Patterns_Week_Of_Year" id="Date_Patterns_Week_Of_Year" href="#Date_Patterns_Week_Of_Year">Week of Year</a>

* Values calculated for the Week of Year field range from 1 to 53 for the Gregorian calendar (they may have different ranges for other calendars). Week 1 for a year is the first week that contains at least the specified minimum number of days from that year. Weeks between week 1 of one year and week 1 of the following year are numbered sequentially from 2 to 52 or 53 (if needed). For example, January 1, 1998 was a Thursday. If the first day of the week is MONDAY and the minimum days in a week is 4 (these are the values reflecting ISO 8601 and many national standards), then week 1 of 1998 starts on December 29, 1997, and ends on January 4, 1998. However, if the first day of the week is SUNDAY, then week 1 of 1998 starts on January 4, 1998, and ends on January 10, 1998. The first three days of 1998 are then part of week 53 of 1997.


Values are similarly calculated for the Week of Month.

### <a name="Date_Patterns_Week_Elements" id="Date_Patterns_Week_Elements" href="#Date_Patterns_Week_Elements">Week Elements</a>

**firstDay**

* A number indicating which day of the week is considered the 'first' day, for calendar purposes. Because the ordering of days may vary between calendar, keywords are used for this value, such as sun, mon, …. These values will be replaced by the localized name when they are actually used.


**minDays (Minimal Days in First Week)**

* Minimal days required in the first week of a month or year. For example, if the first week is defined as one that contains at least one day, this value will be 1. If it must contain a full seven days before it counts as the first week, then the value would be 7.


**weekendStart, weekendEnd**

Indicates the day and time that the weekend starts or ends. As with firstDay, keywords are used instead of numbers.


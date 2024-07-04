---
title: Day-Period Design
---

# Day-Period Design

## Problem:

The two part AM/PM day division is not universal and some cultures require greater granularity in day divisions to properly represent a time. For more information, please see CLDR bugs [169](http://unicode.org/cldr/trac/ticket/168), [992](http://unicode.org/cldr/trac/ticket/992), [1935](http://unicode.org/cldr/trac/ticket/1935).

## Proposal

### New dayPeriods.xml under Supplemental directory:

\<dayPeriodRuleSet>

&emsp;\<dayPeriodRules locales = "locale1 locale2 ... ">

&emsp;&emsp;\<dayPeriodRule type = "typeName1" from | after = "HH:mm" to | before = "HH:mm"/>

&emsp;&emsp;\<dayPeriodRule type = "typeName2" at = "HH:mm"/>

.....

&emsp;\</dayPeriodRules>

&emsp;\<dayPeriodRules locales = "..." >

...

&emsp;\</dayPeriodRules>

\</dayPeriodRuleSet>

### New XML rules in locale xmls :

**\<dayPeriods>**

&emsp;**\<dayPeriodContext type="format">**

&emsp;&emsp;**\<dayPeriodWidth type="wide">**

&emsp;&emsp;&emsp;\<**dayPeriod** type = "typeName1"> translation for typeName1\</dayPeriod>

&emsp;&emsp;&emsp;\<**dayPeriod** type = "typeName2"> translation for typeName2\</dayPeriod>

...

&emsp;&emsp;\<**/dayPeriodWidth>**

&emsp;**\</dayPeriodContext>**

...

**\</dayPeriods>**

On "dayPeriods" in the future we can add an optional type if we need it, to support multiple rules for the same locale. For now we won't support that.

### Definition and validation

1. If locales are not defined in dayPeriods.xml, dayPeriods fallback to AM/PM.
2. "from" and "to" are closed intervals(inclusive).
3. "after" and "before" are open intervals(exclusive).
4. "at" means starting time and end time are the same.
5. There must be exactly one of {at, from, after} and exactly one of {at, to, before} for each dayPeriodRule.
	1. The set of dayPeriodRule's need to completely cover the 24 hours in a day (from 0:00 before 24:00), with **no** overlaps between each dayPeriodRule.
6. Both hh:mm [period name] and hh [period name] can be parsed uniquely to HH:mm [period name].
	1. For example, you can't have \<dayPeriod type = "morning" from="0:00" to="12:00"/> because "12 {morning}" would be ambiguous.
7. One dayPeriodRule can cross midnight. For example:
	1. \<dayPeriodRule type="night" from="20:00" before="5:00"/>
	2. However, this should be avoided unless the alternative is awkward, because of ambiguities. While the use of the dayPeriods without hours is not recommended, they can be used. And if the user sees "Tuesday night" they may not think that that includes 1:00 am Tuesday.
	3. dayPeriodRule's with the same type are only allowed if they are not adjacent. Example:
	- \<dayPeriod type = "twilight" from="5:00" to="7:00"/>
	- \<dayPeriod type = "twilight" from="17:00" to="19:00"/>
8. 24:00 is *only* allowed in *before*="24:00". A term for midnight should be avoided in the rules, because of ambiguity problems in most languages.
	1. *"Tuesday midnight" generally means at the end of the day on Tuesday (24:00)*
	2. *Most software does not format anything for 24:00, only for 00:00. And you don't want 00:00 Tuesday (the start of the day) to be formatted as midnight, meaning the end of the day.*
9. When parsing, if the hour is present the dayperiod is checked for consistency. If there is no hour, the center of the first matching dayPeriodRule is chosen (starting from 0:00).
10. Document that if people are rounding -- including the rounding done by the time format -- then the rounding needs to be done before the dayperiod is computed.

### Examples:

New xml file: Supplemental/dayPeriods.xml

\<dayPeriodRuleSet>

&emsp;\<dayPeriodRules locales = "en"> \<!-- default for any locales not listed under other dayPeriods -->

&emsp;&emsp;\<dayPeriodRule type = "am" from = "0:00" before="12:00"/>

&emsp;&emsp;\<dayPeriodRule type = "pm" from = "12:00" to="24:00"/>

&emsp;\</dayPeriodRules>

&emsp;\<dayPeriodRules locales = "de it fr">

&emsp;&emsp;\<dayPeriodRule type = "earlyMorning" from="0:00" before="9:00"/>

&emsp;&emsp;\<dayPeriodRule type = "morning" from="9:00" before="12:00"/>

&emsp;&emsp;\<dayPeriodRule type = "noon" at = "12:00"/>

&emsp;&emsp;\<dayPeriodRule type = "afternoon" after="12:00" before="17:00"/>

&emsp;&emsp;\<dayPeriodRule type = "evening" from="17:00" before="21:00"/>

&emsp;&emsp;\<dayPeriodRule type = "night" from="21:00" before="24:00"/>

&emsp;\</dayPeriodRules>

&emsp;\<dayPeriodRules locales = "zh">

&emsp;&emsp;\<dayPeriodRule type = "weeHours" from="00:00" before="4:00"/>

&emsp;&emsp;\<dayPeriodRule type = "earlyMorning" from="4:00" before="6:00"/>

&emsp;&emsp;\<dayPeriodRule type = "morning" from="6:00" before="12:00"/>

&emsp;&emsp;\<dayPeriodRule type = "midMay" from="12:00" before="13:00"/>

&emsp;&emsp;\<dayPeriodRule type = "afternoon" from="13:00" before="18:00"/>

&emsp;&emsp;\<dayPeriodRule type = "night" from="18:00" before="24:00"/>

&emsp;\</dayPeriodRules>

**. . .**

\</dayPeriodRuleSet>

Examples in locale XML files:

In en locale file(en.xml)

\<dayPeriods>

&emsp;**\<dayPeriodContext type="format">**

&emsp;&emsp;**\<dayPeriodWidth type="wide">**

&emsp;&emsp;&emsp;\<dayPeriod type = "am">AM\</dayPeriod>

&emsp;&emsp;&emsp;\<dayPeriod type = "am" alt="variant">a.m.\</dayPeriod>

&emsp;&emsp;&emsp;\<dayPeriod type = "pm">PM\</dayPeriod>

&emsp;&emsp;&emsp;\<dayPeriod type = "pm" alt="variant">p.m.\</dayPeriod>

&emsp;&emsp;\<**/dayPeriodWidth>**

&emsp;**\</dayPeriodContext>**

\</dayPeriods>

// was:

&emsp;&emsp;\<am>AM\</am>

&emsp;&emsp;\<am alt="variant">a.m.\</am>

&emsp;&emsp;\<pm>PM\</pm>

&emsp;&emsp;\<pm alt="variant">p.m.\</pm>

*Also, in root, replace all instances (except in Gregorian) of*

&emsp;&emsp;\<am>AM\</am>

&emsp;&emsp;\<pm>PM\</pm>

*by*

&emsp;&emsp;\<dayPeriods>

&emsp;&emsp;&emsp;\<alias source="locale" path="../../calendar[@type='gregorian']/dayPeriods"/>

&emsp;&emsp;\</dayPeriods>

In de locale file(de.xml):

\<dayPeriods>

&emsp;**\<dayPeriodContext type="format">**

&emsp;&emsp;**\<dayPeriodWidth type="wide">**

&emsp;&emsp;&emsp;\<dayPeriod type = "earlyMorning">morgens\</dayPeriod>

&emsp;&emsp;&emsp;\<dayPeriod type = "morning">vormittags\</dayPeriod>

&emsp;&emsp;&emsp;\<dayPeriod type = "noon">Mittag\</dayPeriod>

&emsp;&emsp;&emsp;\<dayPeriod type = "afternoon">nachmittags\</dayPeriod>

&emsp;&emsp;&emsp;\<dayPeriod type = "evening">abends\</dayPeriod>

&emsp;&emsp;&emsp;\<dayPeriod type = "night">nachts\</dayPeriod>

&emsp;&emsp;\<**/dayPeriodWidth>**

&emsp;**\</dayPeriodContext>**

\</dayPeriods>



In zh locale file(zh.xml):

\<dayPeriods>

&emsp;**\<dayPeriodContext type="format">**

&emsp;&emsp;**\<dayPeriodWidth type="wide">**

&emsp;&emsp;&emsp;\<dayPeriod type = "weeHours">凌晨\</dayPeriod>

&emsp;&emsp;&emsp;\<dayPeriod type = "earlyMorning">清晨\</dayPeriod>

&emsp;&emsp;&emsp;\<dayPeriod type = "morning">上午\</dayPeriod>

&emsp;&emsp;&emsp;\<dayPeriod type = "midDay">中午\</dayPeriod>

&emsp;&emsp;&emsp;\<dayPeriod type = "afternoon">下午\</dayPeriod>

&emsp;&emsp;&emsp;\<dayPeriod type = "night">晚上\</dayPeriod>

&emsp;&emsp;\<**/dayPeriodWidth>**

&emsp;**\</dayPeriodContext>**

\</dayPeriods>

Code changes

- The example code needs to be changed to get the AM/PM data from the new location (high priority)
- LDML2ICU needs to change to map the new data.
- ICU code needs to use the new structure
- We need to have a chart for the dayPeriodRules, like we do for plurals.
- Add invariant testing in CheckAttributes.

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
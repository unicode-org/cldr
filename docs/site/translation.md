---
title: Information Hub for Linguists
---

# Information Hub for Linguists

## News
- 2026-04-29 [CLDR Survey Tool][] opens for General Submission

### Status and Schedule

The [CLDR Survey Tool][] is now open for [General Submission][] for version 49.
[DDL locales][] remain open for submission throughout the General Submission and Vetting periods. 

The other key milestones are as follows:

- [Vetting][] will start on June 10.
- [Resolution][] will start on June 29.

For information about these phases, see [Survey Tool stages][].

We are reviewing new DDL locale requests for inclusion in CLDR 49.
See [how to add a new locales](https://cldr.unicode.org/development/adding-locales) for more information.

## Working in the Survey Tool

Before you start Submission, please review the [Critical reminders for all linguists][], and if you're new to CLDR, read the [CLDR training][].

For the Submission phase, please prioritize the sections Missing, Provisional, and Errors. A summary of new items can be found in the [New Areas][] section below.
Be sure to submit data for any item where you disagree with the currently winning value during the [General Submission][] phase.
You will not be able to submit new data once the CLDR Survey Tool is in [Vetting][] mode, but you will be able to change your vote to another, previously submitted value.
[Survey Tool Changes][] covers improvements to the CLDR Survey Tool since the previous cycle.

### Troubleshooting

- **Disconnect error**. If you see a persistent Loading error with a disconnect message or other odd behavior, please [empty your cache](translation/getting-started/empty-cache).
- **Missing notifications**. Survey Tool email notifications may be going to your spam folder. Check your spam folder regularly.

### Prerequisites

1. If you're **new to CLDR**, take the [CLDR training][] below.
2. If you're already **experienced with CLDR**, read the [Critical reminders][] section (mandatory).
3. Review the [Status and Schedule](#status-and-schedule), [New Areas](#new-areas), [Survey Tool](#survey-tool), and [Known Issues](#known-issues).
4. Once you are ready, go to the [Survey Tool](https://st.unicode.org/cldr-apps/) and log in.

## New Areas

**_Please review all of these areas before you start! Details and guidelines are supplied below_**

There are detailed sections for each of these below. In the title of those sections there is typically a link to a sample row in the Survey Tool.

| Area | New items | Number of items (approximate) |
| ---- | ----------------------- |:---------------:|
| Alphabetic Information | Preventing digit-digit concatenations | 1 |
| Locale display names | Nested Bracket Replacement | 4 |
| Locale display names | Territories | 3 |
| Locale display names | Additional Locale Display Names—Keys | ~90 | 
| Dates and times | Ordinal days in dates | ~30 per calendar + No. of ordinal categories |
| Dates and times | Numeric datetime separators | 2 |
| Dates and times | Additional flexible date formats | ~7 |
| Dates and times | Append items | 5 |
| Timezones | Dual Standard/Daylight UTC offset format | 1 |
| Timezones | UTC timezone display patterns | 2 |
| Timezones | Samoa timezone name update | 1 |
| Characters | Unicode 18 emoji annotations | 18 |

### Alphabetic Information

#### Preventing digit-digit concatenations

**In progress - This item might not be available at the start of General Submission**

Will be added soon; details in [CLDR-19227](https://unicode-org.atlassian.net/browse/CLDR-19227)

There are some circumstances in which placeholders are replaced by numbers that may concatenate.
This issue can occur in dates and times, especially in languages that don't use spaces between words.
For example, there are patterns like "vHH:mm" where a timezone placeholder (`v` symbol) is adjacent to an hour placeholder (h or H symbol).
When the timezone value is a word this may be intended: "育空时间13:59".
However, when the timezone is represented by an offset format, the result becomes garbled: "UTC+113:59".

There is a new Placeholder Boundary Spacing item to address that.
Whenever placeholder substitution would result in two adjacent digits, that value is inserted.
The default value is a single ASCII space.

##### Guidelines

If your language doesn't use spaces to separate words,
add the appropriate value that you would use to separate two numbers in your language,
such as a wide space.

### Locale display names

#### Nested Bracket Replacement

There are 4 new items that are used in constructing locale names (see [new items in Survey Tool](https://st.unicode.org/cldr-apps/v#/USER/Alphabetic_Information/5f07aaec0572deb9)).
When text containing parentheses is embedded in _other_ parentheses, 
these bracket characters are used to distinguish the embedded parentheses.

| Code | Description | Root Winning | Description |
| :--: | -- | :--: | -- |
| ( | ASCII open parenthesis | [ | ASCII (narrow) open square bracket |
| （ | Fullwidth open parenthesis |［ | Fullwidth open square bracket |
| ) | ASCII close parenthesis | ] | ASCII (narrow) close square bracket |
| ） | Fullwidth close parenthesis | ］| Fullwidth close square bracket |

For example, this is used in locale names such as the locale name for `en_MM`.
The name for the language (such as “anglais”) is composed with the name of the region 
(such as “Myanmar (Birmanie)”) with the `localePattern` “{0} ({1})”.
In so doing, any parentheses are replaced by square brackets.
The choice of brackets are determined by whether the source name contains ASCII vs fullwidth parentheses. Example:

| Example locale name | Description |
| -- | -- |
| anglais (Myanmar [Birmanie]) | The orginal name had (…) |
| ミャンマー語（ミャンマー [ビルマ]） | The orginal name had （…） |

##### Guidelines

Typically the default values are reasonable for all locales, because only the appropriate width characters are replaced. 
The only time you would need to replace them is if the best option for a locale would not be _square_ brackets, but instead some other form of bracket.
For example, if your locale used angle brackets when parentheses were embedded inside of parentheses, you might have:

| Code | Description | Your Locale | Description |
| :--: | -- | :--: | -- |
| ( | ASCII open parenthesis | ⟨ | narrow open square bracket |
| （ | Fullwidth open parenthesis | 〈 | CJK open angle bracket |
| ) | ASCII close parenthesis | ⟩ | narrow square bracket |
| ） | Fullwidth close parenthesis | 〉| Fullwidth close square bracket |

#### Territories

Added long display names for the territories 
"St. Helena, Ascension & Tristan da Cunha" (`SH`),
"French Southern and Antarctic Lands" (`TF`),
and "Heard Island & McDonald Islands" (`HM`).
The previous names were moved to `alt="short"`.

##### Guidelines

Revisit these items to make sure that the values are correct.

#### Additional Locale Display Names—Keys

Locale codes are not only used for languages and regional or script variants,
but can also include options / settings. See [new items in Survey Tool](https://st.unicode.org/cldr-apps/v#/USER/Keys/717c7818b29ac1ab).
Please review [Locale Option Names] to see how these work.

##### Guidelines

| Code | Native |
| :---- | :---- |
| calendar |	Kalender |
| calendar-buddhist |	Buddhistischer Kalender |
| calendar-buddhist-core |	Buddhistischer 

Where there are combined option-value names (like `calendar-buddhist` values),
you can use that to guide your name for the related `…-core` names
— basically removing the name of the key.

Otherwise, go by the English name for the key-option values.

### Dates and times

#### Ordinal days in dates

In some locales, ordinal numbers (such as 1st, 2nd, …) can be used in dates. 
For example, _ordinal_: "March 3rd, 2026"; compared to _cardinal_: "March 3, 2026". 
There are now two new types of data items to support this. See new items in Survey Tool:

* Date & Time | Gregorian | DayOfMonth-abbreviated-Formatting | few — English: `{0}rd`
   * [Ordinal days of month in the Survey Tool](https://st.unicode.org/cldr-apps/v#/USER/Gregorian/5aff1f2a84ad62f0)
   * The value substituted for `{0}` will always be an integer, such as English "**3**rd".
   * Many locales have a "constant" pattern, such as German.
In that case only the `other` code will appear.
   * A few locales didn't have ordinal categories, just plural categories. That is being corrected in this release.
* Date & Time | Gregorian | Formats-Flexible-Date_Formats | yMMMddd — English: `MMM ddd, y`
   * (~25 items)
   * The symbol `ddd` in this pattern will be replaced by an ordinal, resulting in something like "Sep **3rd**, 1999"
   * [Example of date patterns using the new ordinal format "ddd" in the Survey Tool](https://st.unicode.org/cldr-apps/v#/USER/Gregorian/1448633d3d7929b4)

##### Guidelines

_For DayOfMonth-abbreviated-Formatting:_

* These ordinal forms are _specific to dates_; they are _not_ general-purpose.
* They should have the appropriate grammatical form for a nominative date.
* They might not be the same as general-purpose ordinals.
    * For example, suppose that your locale uses a "er" suffix just on `one` in dates. 
    * In that case, the `one` form would be {0}er, but all other forms would have just {0}.
* If your locale _never_ uses ordinals in dates, then:
    * Set all the dayOfMonth patterns (`one`, `other`, …) to a constant “{0}” with no other text.

_For Formats-Flexible-Date_Formats:_

* The `ddd` is ignored in any pattern with a _numeric_ month (M, MM).
It will only appear and be used with _non-numeric_ months (MMM, MMMM). For example, Dec or December. [See Date/Time Symbols](/date-time/date-time-symbols) for more information about symbol length.
* If your locale _always_ uses ordinals with **non-numeric months**, then make sure the patterns where they are used _always_ have `ddd` in them (instead of `d` or `dd`).
    * For example, suppose that a form like "March 3, 2026" is not acceptable;
your locale always uses a form like "March 3rd, 2026".
In that case, for the code `yMMMMd` you would change its pattern to have `ddd` in it to force the use of ordinals, something like: "MMMM ddd, y"
* If your locale _sometimes_ uses ordinals with **non-numeric months**,
then generally when a skeleton has `ddd` in it, the pattern should also have it;
when a skeleton has `d` in it, the pattern should also have it;
    * However, review the results as there may be some patterns where ordinals are disallowed or required.

#### Numeric datetime separators

There are two new items used in pure-numeric dates and times, such as 03/04/2026 or 13:45:30.
For these, the values would be "/" and ":".  See [new items in Survey Tool](https://st.unicode.org/cldr-apps/v#/USER/Generic/373513a7ce47d340).

##### Guidelines

Make sure these match the typical characters used in pure-numeric formats of dates and times in your locale. 
If more than one is commonly used in your locale, 
please use the separator that matches the current date and time formats in the CLDR.

#### Formats - Intervals - Range

There are three new patterns used in interval ranges to separate fields. See [new items in Survey Tool](https://st.unicode.org/cldr-apps/v#/USER/Gregorian/header_Formats_Intervals_Range).
| Code | Example | Description |
| -- | -- | -- | -- |
| numeric	| {0}–{1} | Used to separate the same _numeric_ date fields, such as in “Dec 5–15” |
| non-numeric	| {0}–{1} | Used to separate the same _non-numeric_ date fields, such as in “June–July 2026” |
| mixed	| {0} – {1} | Used to separate the different date fields, such as in “Dec 10 – July 20 2026” |

##### Guidelines

Make sure these match the typical characters used in pure-numeric formats of dates and times in your locale. 
If more than one is commonly used in your locale, 
please use the separator that matches the current date and time formats in the CLDR.

#### Append Items

There are 5 "Append Items" that contain patterns for adding fields to date patterns.
The {0} placeholder has the base (a date or time pattern) to add the field to, while the {1} pattern is the field to be added.
See [new items in the Survey Tool](https://st.unicode.org/cldr-apps/v#/USER/Gregorian/7fa71371abb195ab)

| Code | Base | Example |
| :---- | :---- | :---- |
| Era | date | June 1 2026 *AD* |
| Day-Of-Week | date | *Tuesday*, June 1 2026 |
| Time-Day-Of-Week | time | *Tuesday*, 13:00 |
| Timezone | time | 13:00 *UTC+3* |
| Date-Timezone | date | June 1 2026 *UTC+3* |

The pattern is used to determine which side of the base to add to, and which characters are added between the field and the base.

##### Guidelines
Look at the existing date and time patterns in Flexible formats that have eras, day-of-weeks, or timezones.
Use that to determine what the best pattern would be for arbitrary bases of the given type.
For example, where would an era appear relative to a `yMMM` or `yMMMMEEEd` pattern?
Make sure that you put the {0} and {1} placeholders in the right order, and put the right separators between them.
(Locales that don't need spaces between words might have no separators, such as `{0}{1}`.)

#### Dual Standard/Daylight format

One of the formats for timezones is to list the offsets from UTC.
That works well for places that don't have a distinct daylight time (aka summer time).
- Nigeria Time → UTC+1
- South Africa Time → UTC+2

That doesn't work well for places that alternate between standard and daylight times, such as CET.
A majority of the year they are not on a standard time, but rather one hour ahead.
A new localizable pattern allows for a more informative representation, such as:
- Central European Time — UTC+1/+2

This is done with a pattern such as “{0}/{1}" that combines the two offsets, 
and is then substituted into the `gmtFormat`, which has localized versions of “UTC{0}" (or “GMT{0}").
See [new item in Survey Tool](https://st.unicode.org/cldr-apps/v#/USER/Timezone_Display_Patterns/617abc8969d51a65).

##### Guidelines

Use a punctuation character in the pattern for your locale that shows that a time zone has two alternate timezone offsets
(one in summer and one in winter).

#### Additional flexible date formats

Aside from the new skeletons with `ddd` used for Ordinal days in dates, 
there are some new patterns that flesh out support for different combinations of long months (MMMM) plus days, and eras or days of the week, such as and `MMMMEd`.
See [new items in Survey Tool](https://st.unicode.org/cldr-apps/v#/USER/Gregorian/75be2c5885156280).

##### Guidelines

Typically the format will be aligned with the format for abbreviated months (MMM).
So look for the corresponding skeleton to see what the pattern is, then modify it to have MMMM.
This is not done automatically, because in some locales the best format may be a bit different.

#### Additional interval skeletons

Like the *Additional available skeletons*, there are a few new interval skeletons.
Check to make sure they have patterns that are similar to related interval skeletons' patterns.
See [new items in Survey Tool](https://st.unicode.org/cldr-apps/v#/USER/Gregorian/d9bdb15b05e77dd)

#### UTC Timezone Display Patterns

The term GMT is ambiguous; it can either mean a timezone connected with London (Greenwich Mean Time, 
which has daylight time), or what is unambiguously referred to as UTC (Coordinated Universal Time). 
There are now two "alternative values" `GMT Format-utc` and `GMT Unknown Format-utc` that should contain the
localized abbreviation for "UTC", not "GMT". See [new items in the Survey Tool](https://st.unicode.org/cldr-apps/v#/USER/Timezone_Display_Patterns/3a87a42ed8f4d4b1)

##### Guidelines

1. For the new `-utc` versions, use the localized version of the abbreviation "UTC".
2. Review the `GMT Format` and `GMT Unknown Format` items.
They should contain the most customarily used patterns for numeric offset timezones.
Those will _often_ be identical to the `GMT Format-utc` and `GMT Unknown Format-utc` values respectively,
but there are locales where the most customary format uses a localized version of "GMT" such as many English locales.

Do not use the longer, spelled out versions of either one; these must be as short as possible.

#### Time zone updates

The following time zone display names have been added to modern coverage:

- [Greenland Time](https://st.unicode.org/cldr-apps/v#/USER/NAmerica/5c7eda744df5015)
- [Türkiye Time](https://st.unicode.org/cldr-apps/v#/USER/WAsia/68d13963fcc3d7d7)
- [Urumqi](https://st.unicode.org/cldr-apps/v#/USER/EAsia/6cc40a7f1a8cb19c)

##### Samoa time zone name updates

In order to disambiguate between the timezones of Samoa and American Samoa, 
which are on different sides of the [International Date Line](https://en.wikipedia.org/wiki/International_Date_Line), 
the Apia metazone has been renamed to be West Samoa Time.

See items in Survey Tool:
- [Pacific/Apia](https://st.unicode.org/cldr-apps/v#/USER/Oceania/header_Apia)
- [Pacific/Samoa](https://st.unicode.org/cldr-apps/v#/USER/Oceania/header_Samoa)

Please check to make sure that the two timezones are distinct in your locale.

### New emoji

There are 9 new emojis with short names and search keywords. You can find the new items in [the Characters section of the Survey Tool](https://st.unicode.org/cldr-apps/v#/USER/Smileys/42e8830260876de3)

![image](/images/Unicode%2018%20emojis.png)

## Survey Tool

Once trained and up to speed on [Critical reminders](#critical-reminders-for-all-linguists) (below),
log in to the [Survey Tool](https://st.unicode.org/cldr-apps/) to begin your work.

### Survey Tool Changes

#### Viewing / Adding Hidden Characters
When you are adding a value, you can see any "Hidden" characters, and insert additional ones.
These include characters that are completely invisible, as well as variants of spaces.
For example, in the image below, someone is in the middle of adding a new item. 
There is a new "eye" icon in the bottom left that the user can toggle to see hidden characters, 
and they have turned it on. 
That opens a new (uneditable) box below the text entry, where they can see the NBSP (non-breaking space) variant of the space character in a 'chit'.

<img width="520" height="169" alt="Text input with Show Hidden" src="https://github.com/user-attachments/assets/9a112871-1050-4073-81b8-a3581b3d5000" />

They realize that they need to insert a hidden character, 
so they pull down the insert-character menu from the new "insert-character" icon in the top left.
That lets them insert a character at the current insertion point in the text.

<img width="583" height="292" alt="Screenshot 2026-04-21 at 16 32 05" src="https://github.com/user-attachments/assets/8c77a348-6330-4f5e-a95a-913d65230776" />

Hovering over the items in that menu shows details about their usage, as you see in the image
— so it can also be used to decode the meaning of the abbreviations used in the chits.

NOTE: For alphabetic information (such as exemplar characters), an older mechanism is still in place.
We hope to update it during the submission phase.

----
Some of the changes below were in the previous version of the Survey Tool, 
but are retained here for those who didn't contribute in that version and may not have seen them.

#### Winning column display

The Survey Tool has been revised to display a candidate item in the Winning column if it is _currently_ winning,
even if it has the status "missing" due to not have enough recorded votes.
Previously, such items were shown in the Others column.
Items with the status "missing" may still be published in the final release.
Even if an item already appears in the Winning column, it is still important to vote for that item or one of the other items in that row,
or to submit a new item.
The Dashboard “Missing” category shows where votes are needed.

#### Searching in the Survey Tool

The ability to search in the Survey Tool has been added in [CLDR-18423][] and supports searching for: values, English value, and for the codes.
In the Dashboard header, each notification category (such as "Missing" or "Abstained") has a checkbox determining whether it is shown or hidden.
The symbols in the A column have been changed to be searchable in browsers (with *Find in Page*) and stand out more on the page. See below for a table. They override the symbols in [Survey Tool Guide: Icons](translation/getting-started/guide#icons).

#### Forum notifications
In each row of the vetting page, there is now a visible icon when there are forum messages at the right side of the English column:
    1. 👁️‍🗨️ if there are any open posts
    1. 💬 if there are posts, but all are closed

----
## Known Issues

Last updated: 2026-04-29

This list will be updated as fixes are made available in Survey Tool Production. If you find a problem, please [file a ticket](requesting_changes), but please review this list first to avoid creating duplicate tickets.


1. [CLDR-19420] - Browser back button does not work as expected for forum and reports
1. [CLDR-19427] - Number formatting examples not display as expected for some locales.
1. [CLDR-19428] - Unclear error message about forum access if you try to access the forum for a locale you don't have permissions for, also true for locked accounts.
1. [CLDR-19411] - Emoji page loads slowly. Refresh the page if it doesn't load.
1. [CLDR-19404] - Person names report should display error if not enough data has been submitted to generate the report.
1. [CLDR-18577] - If your language does not have a variant value, you can vote for inheritance from the standard version.
1. [CLDR-13477] - Images for the plain symbols. Non-emoji such as [€](https://st.unicode.org/cldr-apps/v#/fr/OtherSymbols/47925556fd2904b5), √, », ¹, §, ... do not have images in the Info Panel.  **Workaround**: Look at the Code column; unlike the new emoji, your browser should display them there.
1. [CLDR-17683] - Some items are not able to be flagged for TC review. This is being investigated. Meanwhile, Please enter forum posts meanwhile with any comments.

## Resolved Issues

Last updated: 2026-04-29

1. [CLDR-18689] - Languages are sorted by full English name instead of core element, and may appear on different pages in Locale Display Names
1. [CLDR-18615] - Unclear error message if a link sends you to a page that no longer exists in the Survey Tool
1. [CLDR-19412] - Some forum posts are not linking back to the item. If you need to access that item you will have to navigate to the item by searching or via the left navigation bar. Does not currently reproduce, please comment on the ticket with a link to the forum post if you see this issue.
1. [CLDR-19413] - Closed posts are showing up under Needing Action in the Forum view which is not expected. Does not currently reproduce, please comment on the ticket with a link to the forum post if you see this issue.

## CLDR training for new linguists

Before getting started to contribute data in CLDR, and jumping in to using the Survey Tool, it is important that you understand the CLDR process & take the CLDR training. It takes about 2-3 hours to complete the training.

1. **Understand the basics about the CLDR process** read the [Survey Tool Guide](translation/getting-started/guide) and an overview of the [Survey Tool Stages](translation/getting-started/survey-tool-phases).
    - New: A [video is available](https://www.youtube.com/watch?v=Wxs0TZl7Ljk) which shows how to login and begin contributing data for your locale.
2. **Read the Getting Started topics** on the Information Hub:
    - General translation guide
        - [Capitalization](translation/translation-guide-general/capitalization)
        - [Default Content](translation/translation-guide-general/default-content)
        - [References](translation/translation-guide-general/references)
    - [Handling Errors and Warnings](translation/getting-started/errors-and-warnings)
    - [Handling Logical Group Errors](translation/getting-started/resolving-errors)
    - [Plurals & Units](translation/getting-started/plurals)
    - [Review Date & Time](translation/date-time)
    - [Review Numbers](translation/number-currency-formats)
    - [Review Zones](translation/time-zones-and-city-names)
    - [Data stability](translation/getting-started/data-stability)
    - [Empty cache](translation/getting-started/empty-cache)

\*If you (individual or your organization) have not established a connection with the CLDR technical committee, start with [Survey Tool Accounts](index/survey-tool/survey-tool-accounts).

## Critical reminders for all linguists

You're already familiar with the CLDR process, but do keep the following in mind:

1. **Aim at commonly used language** - CLDR should reflect *common-usage* standards **not** *academic /official* standards (unless commonly followed). Keep that perspective in mind.
2. **Carefully consider changes to existing standards** - any change to a value from a previous CLDR release (blue star) should be carefully considered and discussed with your fellow linguists in the CLDR [Forum](translation/getting-started/guide#forum).
Remember your change will be reflected across thousands of online products — and potentially almost all online users of your language.
4. **Keep consistency across logical groups** - ensure that all related entries are consistent.
If you change the name of a weekday, make sure it's reflected across all related items.
Check that the order of month and day are consistent in all the date formats, etc.
    - *Tip: The [Reports](translation/getting-started/review-formats) are a great way to validate consistency across related logical groups,
e.g. translations of date formats.
Use them to proofread your work for consistency.*
6. **Avoid voting for English** - for items that do not work in your language, don't simply use English.
Find a solution that works for your language.
For example, if your language doesn't have a concept of calendar "quarters", use a translation that describes the concept "three-month period" rather than "quarter-of-a-year".
7. **Watch out for complex sections** and read the instructions carefully if in doubt:
    1. [Date & Time](translation/date-time/date-time-names)
        - [Names](translation/date-time/date-time-names)
        - [Patterns](translation/date-time)
        - [Symbols](translation/date-time/date-time-symbols)
    2. [Time zones](translation/time-zones-and-city-names)
    3. [Plural forms](translation/getting-started/plurals)
8. "**Same as code**" errors - when translating codes for items such as languages, regions, scripts, and keys, it is normally an error to select the code itself as the translated name.
If the error appears under Typography, you can ignore it. <!-- [[CLDR-13552](https://unicode-org.atlassian.net/browse/CLDR-13552)\] -->

*Tip: The links in the [Info Panel](translation/getting-started/guide#info-panel) will point you to relevant instructions for the
entry you're editing/vetting. Use it if in doubt.*

<!-- Tickets are in ascending order for easier maintenance -->
[CLDR-13477]: https://unicode-org.atlassian.net/browse/CLDR-13477
[CLDR-17683]: https://unicode-org.atlassian.net/browse/CLDR-17683
[CLDR-17829]: https://unicode-org.atlassian.net/browse/CLDR-17829
[CLDR-18423]: https://unicode-org.atlassian.net/browse/CLDR-18423
[CLDR-18577]: https://unicode-org.atlassian.net/browse/CLDR-18577
[CLDR-18607]: https://unicode-org.atlassian.net/browse/CLDR-18607
[CLDR-18615]: https://unicode-org.atlassian.net/browse/CLDR-18615
[CLDR-18689]: https://unicode-org.atlassian.net/browse/CLDR-18689
[CLDR-19404]: https://unicode-org.atlassian.net/browse/CLDR-19404
[CLDR-19412]: https://unicode-org.atlassian.net/browse/CLDR-19412
[CLDR-19411]: https://unicode-org.atlassian.net/browse/CLDR-19411
[CLDR-19413]: https://unicode-org.atlassian.net/browse/CLDR-19413
[CLDR-19420]: https://unicode-org.atlassian.net/browse/CLDR-19420
[CLDR-19428]: https://unicode-org.atlassian.net/browse/CLDR-19428
[CLDR-19427]: https://unicode-org.atlassian.net/browse/CLDR-19427
[stand-alone vs. formatting]: /translation/date-time/date-time-patterns#when-to-use-standalone-vs-formatting
[Year First Calendar]: /translation/date-time/date-time-patterns#year-first-calendar
[Example Hidden]: https://st.unicode.org/cldr-apps/v#/USER/Number_Formatting_Patterns/67afe297d3a17a3
[Key for Show Hidden]: https://cldr.unicode.org/translation/core-data/exemplars#key-to-escapes
[**input** these from the keyboard]: /translation/core-data/exemplars#input
[Change Protected Items]: /translation/getting-started/guide#changing-protected-items
[CLDR training]: /translation#cldr-training-for-new-linguists
[Critical reminders]: /translation#critical-reminders-for-all-linguists
[Critical reminders for all linguists]: /translation#critical-reminders-for-all-linguists
[Locale Coverage chart]: https://unicode.org/cldr/charts/dev/supplemental/locale_coverage.html
[DDL locales]: /ddl#list
[DDL: Help Center]: /translation/ddl
[New Areas]: /translation#new-areas
[Known Issues]: /translation#known-issues
[Locale Option Names]: /translation/displaynames/locale-option-names-key
[CLDR Survey Tool]: https://st.unicode.org/cldr-apps/v#locales///
[Survey Tool Changes]: /translation#known-issues
[Survey Tool stages]: /translation/getting-started/survey-tool-phases
[Shakedown]: /translation/getting-started/survey-tool-phases#survey-tool-phase-shakedown
[General Submission]: /translation/getting-started/survey-tool-phases#survey-tool-phase-general-submission
[Vetting]: /translation/getting-started/survey-tool-phases#survey-tool-phase-vetting
[Resolution]: /translation/getting-started/survey-tool-phases#resolution-closed-to-vetters




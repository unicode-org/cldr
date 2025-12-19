---
title: Information Hub for Linguists
---

# Information Hub for Linguists

The following list summarizes the recent changes, with more details in a section further down the page.
A sequence like 🆕 2025-12-18 marks items that have been recently added.
In your browser you can copy this sequence, then use ⌘-F (Mac) or Ctrl-F (Windows) to find all the places it occurs.

- **🆕 2025-12-18**
  - The Survey Tool has opened early for DDL locales to contribute data. See the [DDL: Help Center] to start.
    - This includes locales like Kurdish, Qʼeqchiʼ, and many others. See [DDL locales] for full list of locales.
    - In order to give more time for submission -- we will not have a resolution phase for v49, meaning that the survey tool will remain open for new submissions until resolution for CLDR 50 starts in July 2026
    - This does mean that submissions made between now and July 2026 will NOT be reflected in the v49 data release of CLDR. Instead, they will be included in the v50 release of CLDR, which is scheduled for October 2026.
  - Survey Tool submission for v50 for locales maintained by the Technical Committee (TC), such as English, German, French, will open as usual in April or May 2026.
  - Exceptional changes for v49 for TC-maintained locales should be [filed as JIRA tickets](requesting_changes) for TC review and they will be remediated by modifying the XML directly.

When a section below changes, the date will be in the header.

### Starting Submission

Before you start Submission, please read the [CLDR training](#cldr-training-for-new-linguists) (if new to the survey tool). 
Please prioritize the sections Missing, Provisional, and Errors.
Please read the [Updates](#updates). 
For more information about the priorities during Submission, see [Survey Tool stages](translation/getting-started/survey-tool-phases).

### Prerequisites

1. If you're **new to CLDR**, take the [CLDR training](#cldr-training-for-new-linguists) below.
2. If you're already **experienced with CLDR**, read the [Critical reminders](#critical-reminders-for-all-linguists) section (mandatory).
3. Review the [Status and Schedule](#status-and-schedule), [New Areas](#new-areas), [Survey Tool](#survey-tool), and [Known Issues](#known-issues).
4. Once you are ready, go to the [Survey Tool](https://st.unicode.org/cldr-apps/) and log in.

## Status and Schedule

The Survey Tool has opened early [DDL locales] data submission. See the [DDL: Help Center] for more information on submitting data for DDL locales.

Languages which have a value in the *Target Level* column of the [Locale Coverage chart] are considered *TC locales* and will not open for submission until the start of the next regular submission for CLDR 50 in April or May of 2026.

- **Disconnect error**. If you see a persistent Loading error with a disconnect message or other odd behavior, please [empty your cache](translation/getting-started/empty-cache).
- Survey Tool email notification may be going to your spam folder. Check your spam folder regularly.
- "**Same as code**" errors - when translating codes for items such as languages, regions, scripts, and keys, it is normally an error to select the code itself as the translated name.
If the error appears under Typography, you can ignore it. <!-- [[CLDR-13552](https://unicode-org.atlassian.net/browse/CLDR-13552)\] -->

## New languages

We are reviewing new locale requests for inclusion in CLDR 50. See [how to add a new locales](https://cldr.unicode.org/development/adding-locales).

## New Areas

TBA - New areas and data will be announced at the start of general submission for CLDR 50.

### New emoji

TBA - Section will be updated when Unicode 18 emoji keywords are added to the survey tool for localization

## Survey Tool

Once trained and up to speed on [Critical reminders](#critical-reminders-for-all-linguists) (below), log in to the [Survey Tool](https://st.unicode.org/cldr-apps/) to begin your work.

### Survey Tool Changes

1. The ability to search in the Survey Tool has been added in [CLDR-18423][] and supports searching for: values, English value, and for the codes
1. There has been substantial performance work that will show up for the first time. If there are performance issues, please file a ticket with a row URL and an explanation for what happened.
1. In the Dashboard, you can filter the messages instead of jumping to the first one. In the Dashboard header, each notification category (such as "Missing" or "Abstained") has a checkbox determining whether it is shown or hidden.
1. In each row of the vetting page, there is now a visible icon when there are forum messages at the right side of the English column:
    1. 👁️‍🗨️ if there are any open posts
    1. 💬 if there are posts, but all are closed
1. For Units and a few other sections, the Pages have changed to reduce the size on the page to improve performance.
    1. Pages may be split, and/or retitled
    1. Rows may move to a different page.
1. The symbols in the A column have been changed to be searchable in browsers (with *Find in Page*) and stand out more on the page. See below for a table. They override the symbols in [Survey Tool Guide: Icons](translation/getting-started/guide#icons).

See [Recent changes](https://cldr.unicode.org/translation#recent-changes) for additional recent changes in the Survey Tool.

### Important Notes

- Some of the Page reorganization may continue.

### New Approve Status Icons 

| Symbol | Status | Notes |
|:---:|---|---|
| ✅ | Approved | Enough votes for use in implementations … |
| ☑️ | Contributed | Enough votes for use in implementations … |
| ✖️ | Provisional | Not enough votes for implementations … |
| ❌ | Unconfirmed | Not enough votes for implementations … |
|  🕳️ | Missing | Completely missing |
| ⬆️ | Inherited | Used in combination with ✖️ and ❌ |

### Enhanced "Show Hidden"
🆕 2025-05-09 — If a field contains characters that are invisible or certain characters that look like others, 
a special Show Hidden bar will appear below the field that helps distinguish them.
For example, see [Example Hidden] — here is a screen-shot.

![Example of hidden characters](/translation/example-hidden.png)

Note that if you hover over the Show Hidden bar, you'll see the name of the special character and a short description.
Some of the commonly used special characters are listed below, with an example from CLDR.

| Symbol | Example | Show Hidden | Name | Description
| - | - | - | - | -
| ❰NDASH❱ | {0}–{1} | {0}❰NDASH❱{1} | En dash | Slightly wider than a hyphen; used for ranges of numbers and dates in many languages; for clarity may have ❰TSP❱s around it.
| ❰TSP❱ | d – d | d❰TSP❱❰NDASH❱❰TSP❱d | Thin space | A space character that is narrower (in most fonts) than the regular one.
| ❰NB❱ | {0}⁠{1} | {0}❰NB❱{1} | No Break | An invisible character that doesn't allow linebreaks on either side; also limits fraction super/subscripting
| ❰NBTSP❱ | h a | h❰NBTSP❱a | No-break thin space | A thin space that disallows linebreaks; equivalent to ❰TSP❱❰NB❱
| ❰NBSP❱ | re call | ❰NBTSP❱ | No-break space | A regular space that disallows linebreaks; equivalent to adding ❰NB❱ after a space
| ❰NBHY❱ | re‑call | re❰NBHY❱fine | No-break hyphen | A regular hyphen that disallows linebreaks; equivalent to -❰NB❱

The BIDI controls — ❰ALM❱ ❰LRM❱ ❰RLM❱ are used in bidirectional scripts (Arabic, Hebrew, etc.) to control the bidirectional order if needed; typically next to numbers or punctuation.

To see how to [**input** these from the keyboard], and for a key to **all** the escapes, see [Key for Show Hidden].

## Known Issues

Last updated: 2025-12-12

This list will be updated as fixes are made available in Survey Tool Production. If you find a problem, please [file a ticket](requesting_changes), but please review this list first to avoid creating duplicate tickets.

1. [CLDR-18689] - Languages are sorted by full English name instead of core element, and may appear on different pages in Locale Display Names
1. [CLDR-18577] - If your language does not have a variant value, you can vote for inheritance from the standard version.
1. [CLDR-13477] - Images for the plain symbols. Non-emoji such as [€](https://st.unicode.org/cldr-apps/v#/fr/OtherSymbols/47925556fd2904b5), √, », ¹, §, ... do not have images in the Info Panel.  **Workaround**: Look at the Code column; unlike the new emoji, your browser should display them there.
1. [CLDR-17683] - Some items are not able to be flagged for TC review. This is being investigated. Meanwhile, Please enter forum posts meanwhile with any comments.

## Resolved Issues

Last updated: 2025-12-12

1. [CLDR-18615] - Unclear error message if a link sends you to a page that no longer exists in the Survey Tool
1. [CLDR-18607] - Unable to download current votes in CSV
1. [CLDR-17829] - Some links in the Info panel not displaying properly

## CLDR training (for new linguists)

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

## Critical reminders (for all linguists)

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
[stand-alone vs. formatting]: /translation/date-time/date-time-patterns#when-to-use-standalone-vs-formatting
[Year First Calendar]: /translation/date-time/date-time-patterns#year-first-calendar
[Example Hidden]: https://st.unicode.org/cldr-apps/v#/USER/Number_Formatting_Patterns/67afe297d3a17a3
[Key for Show Hidden]: https://cldr.unicode.org/translation/core-data/exemplars#key-to-escapes
[**input** these from the keyboard]: /translation/core-data/exemplars#input
[Change Protected Items]: /translation/getting-started/guide#changing-protected-items]
[Locale Coverage chart]: https://unicode.org/cldr/charts/dev/supplemental/locale_coverage.html
[DDL locales]: /ddl#list
[DDL: Help Center]: /translation/ddl

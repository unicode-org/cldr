---
title: Information Hub for Linguists
---

# Information Hub for Linguists

### Starting Submission

During Submission, please read the CLDR Training (if new to the survey tool), please focus on the missing, provisional, and errors. Please read the [Updates](#updates). For more information about the priorities during Submission, see [Survey Tool stages](translation/getting-started/survey-tool-phases).

### Prerequisites

1. If you're **new to CLDR**, take the CLDR training below.
2. If you're already **experienced with CLDR**, read the [Critical reminders](#critical-reminders-for-all-linguists) section (mandatory).
3. Review the [Status and Schedule](#status-and-schedule), [New Areas](#new-areas), [Survey Tool](#survey-tool), and [Known Issues](#known-issues).
4. Once you are ready, go to the [Survey Tool](https://st.unicode.org/cldr-apps/) and log in.

### Updates

- TBD

When a section below changes, the date will be in the header.

## Status and Schedule

The Survey Tool is currently being prepared to open for CLDR 48 [General Submission](translation/getting-started/survey-tool-phases#survey-tool-phase-general-submission) in April 2025. The General Submission phase will be followed by the [Vetting phase](translation/getting-started/survey-tool-phases#survey-tool-phase-vetting).

- **Disconnect error**. If you see a persistent Loading error with a disconnect message or other odd behavior, please [empty your cache](translation/getting-started/empty-cache).
- Survey Tool email notification may be going to your spam folder. Check your spam folder regularly.
- "**Same as code**" errors - when translating codes for items such as languages, regions, scripts, and keys, it is normally an error to select the code itself as the translated name. If the error appears under Typography, you can ignore it. \[[CLDR-13552](https://unicode-org.atlassian.net/browse/CLDR-13552)\]

## New Areas

Most of the following are relevant to locales at the Modern Coverage Level.

### New emoji

TBD - New emoji will be added the week of April 21st.

### New/expanded units

1. TBD

### Language names

As new locales reach Basic Coverage, their language names have been added for locales targeting modern coverage: TBD

### DateTime formats

There is a new “-relative” variant for [Date-Time Combined Formats](/translation/date-time/date-time-patterns#date-time-combined-formats).

Before CLDR 48, there were two variants:
- A “standard” variant for combining date with time, typically without literal text. In English this was “{1}, {0}”
and resulted in combined date patterns like “March 20, 3:00 PM“, “March 20, 3:00-5:00 PM”, “tomorrow, 3:00 PM”, “tomorrow, 3:00-5:00 PM”, “in 2 days, 3:00 PM”
- An “atTime” variant for combining date with a single time (not a range). For longer styles in English this was “{1} 'at' {0}”
and resulted in combined date patterns like “March 20 at 3:00 PM“, “tomorrow at 3:00 PM”, “2 days ago at 3:00 PM”.

However, in some languages the use of a relative date such as “tomorrow” or “2 days ago” required a different combining pattern than for a fixed date like “March 20”.
So in CLDR 48 a new “relative” variant is introduced. This will be used (instead of the “atTime” variant) for the combination
of a relative date and a single time. If you do not supply this, that combination will fall back to using the “standard” variant;
in English that would produce “tomorrow, 3:00 PM”. If instead you want the same combining behavior for a relative date with a single time as for a
fvfixed date with single time (as was the case in CLDR 47 and earlier), then for each length style copy the existing “atTime” form to the new “relative” form.

### Metazones

TBD

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
1. In the Dashboard, the Abstains items will now only have one entry per page. You can use that entry to go to its page, and then fix Abstains on that page. Once you are done on that page, hit the Dashboard refresh button (↺). This fixes a performance problem for people with a large number of Abstains, and reduces clutter in the Dashboard.
1. The symbols in the A column have been changed to be searchable in browsers (with *Find in Page*) and stand out more on the page. See below for a table. They override the symbols in [Survey Tool Guide: Icons](translation/getting-started/guide#icons).

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

## Known Issues

Last updated: 2025-04-07

This list will be updated as fixes are made available in Survey Tool Production. If you find a problem, please [file a ticket](requesting_changes), but please review this list first to avoid creating duplicate tickets.

1. Images for the plain symbols. Non-emoji such as [€](https://st.unicode.org/cldr-apps/v#/fr/OtherSymbols/47925556fd2904b5), √, », ¹, §, ... do not have images in the Info Panel. [CLDR-13477](https://unicode-org.atlassian.net/browse/CLDR-13477) **Workaround**: Look at the Code column; unlike the new emoji, your browser should display them there.
1. [CLDR-17683](https://unicode-org.atlassian.net/browse/CLDR-17683) - Some items are not able to be flagged for TC review. This is being investigated.Meanwhile, Please enter forum posts meanwhile with any comments.

## Resolved Issues

Last updated: 2025-04-07

1. [CLDR-17694](https://unicode-org.atlassian.net/browse/CLDR-17694) - Back button in browser fails in forum under certain conditions
1. [CLDR-17658](https://unicode-org.atlassian.net/browse/CLDR-17658) - Dashboard slowness

## Recent Changes

1. [*CLDR-17658*](https://unicode-org.atlassian.net/browse/CLDR-17658) - In the Dashboard, the Abstains items will only have one entry per page. You can use that entry to go to its page, and then fix Abstains on that page. Once you are done on that page, hit the Dashboard refresh button (↺). This fixes a performance problem for people with a large number of Abstains, and reduces clutter in the Dashboard.

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
2. **Carefully consider changes to existing standards** - any change to an existing CLDR standard should be carefully considered and discussed with your fellow linguists in the CLDR [Forum](translation/getting-started/guide#forum). Remember your change will be reflected across thousands of online products!
3. **Keep consistency across logical groups** - ensure that all related entries are consistent. If you change the name of a weekday, make sure it's reflected across all related items. Check that the order of month and day are consistent in all the date formats, etc.
    - *Tip: The [Reports](translation/getting-started/review-formats) are a great way to validate consistency across related logical groups, e.g. translations of date formats. Use them to proofread your work for consistency.*
4. **Avoid voting for English** - for items that do not work in your language, don't simply use English. Find a solution that works for your language. For example, if your language doesn't have a concept of calendar "quarters", use a translation that describes the concept "three-month period" rather than "quarter-of-a-year".
5. **Watch out for complex sections** and read the instructions carefully if in doubt:
    1. [Date & Time](translation/date-time/date-time-names)
        - [Names](translation/date-time/date-time-names)
        - [Patterns](translation/date-time)
        - [Symbols](translation/date-time/date-time-symbols)
    2. [Time zones](translation/time-zones-and-city-names)
    3. [Plural forms](translation/getting-started/plurals)

*Tip: The links in the [Info Panel](translation/getting-started/guide#info-panel) will point you to relevant instructions for the
entry you're editing/vetting. Use it if in doubt.*

[CLDR-18423]: https://unicode-org.atlassian.net/browse/CLDR-18423
[CLDR-17694]: https://unicode-org.atlassian.net/browse/CLDR-17694

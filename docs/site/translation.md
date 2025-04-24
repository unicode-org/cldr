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

When a section below changes, the date will be in the header.

## Status and Schedule

The Survey Tool is currently being prepared to open for CLDR 48 [General Submission](translation/getting-started/survey-tool-phases#survey-tool-phase-general-submission) in April 2025. The General Submission phase will be followed by the [Vetting phase](translation/getting-started/survey-tool-phases#survey-tool-phase-vetting).

- **Disconnect error**. If you see a persistent Loading error with a disconnect message or other odd behavior, please [empty your cache](translation/getting-started/empty-cache).
- Survey Tool email notification may be going to your spam folder. Check your spam folder regularly.
- "**Same as code**" errors - when translating codes for items such as languages, regions, scripts, and keys, it is normally an error to select the code itself as the translated name. If the error appears under Typography, you can ignore it. \[[CLDR-13552](https://unicode-org.atlassian.net/browse/CLDR-13552)\]

## New Areas

Most of the following are relevant to locales at the Modern Coverage Level.

### New emoji

New emoji will be added the week of April 21st.

### Core Data
There are new Alphabetic Information items.
- `numbers-auxiliary` — If there are are characters used in numbers that are not customarily used, but may occur, add them here instead of in `auxiliary`.
- `punctuation-auxiliary` — If there are punctuation characters that are not customarily used, but may occur, add them here instead of in `auxiliary`.
- `punctuation-person` — If there are punctuation characters that are customarily used in people's names in standard documents, add them here.
This should be a small list such as “.” or “-”.
Do **not** include ‘fanciful’ characters such as emoji or [kaomoji](https://en.wikipedia.org/wiki/Kaomoji).

### Locale display names

#### More language names

As new locales reach Basic Coverage, their language names have been added for locales targeting modern coverage. These include: **TBD**

#### Languages whose English name changed

- tkl: English name changed to Tokelauan.
<!-- Let's not add ticket numbers, except for Known Issues — there is already enough for vetters to read, and the tickets will often be confusing to people or take more time to read and puzzle out than is worth it. Fine to leave them in the document, but commented out, for our usage. 
[CLDR-11231](https://unicode-org.atlassian.net/browse/CLDR-11231) -->

#### Core/Extensions

There is a new mechanism for better menu names. When you see a **Code** with `-core` or `-extension`, please read [Locale Option Value Names](/translation/displaynames/locale-option-names-key#locale-option-value-names).

#### Scripts


There are 5 new scripts for Unicode 17. Currently the names are in English: Beria Erfe, Chisoi, Sidetic, Tai Yo, Tolong Siki.
Coverage for other languages is at comprehensive. 
<!-- I don't think we want to encourage this: ... if there is a need to have coverage at lower level in some locale,
please file a ticket. [CLDR-18283](https://unicode-org.atlassian.net/browse/CLDR-18283) -->

#### ISO 8601 calendar

This is a variant of the Gregorian calendar whose formats always use year-month-day ordering and a 24-hour time cycle.
The English name has changed to reflect that (and also added a variant); locales should update accordingly:
- calendar-iso8601: Gregorian (Year First)
- calendar-iso8601-variant: ISO 8601 Order

<!-- Please go though the ISO8601 fields. You should change separators to match what is acceptable in your language. However, do not change the ordering of the elements, which should be strictly the following order (for any that occur in a particular pattern):
* year - month - day - day-of-week - hour - minute - second

Also avoid changing the width of numeric fields (like `dd`).

[CLDR-18447](https://unicode-org.atlassian.net/browse/CLDR-18447) -->

### DateTime formats

#### New “relative” variant for date-time combining pattern

There is a new “-relative” variant for [Date-Time Combined Formats](/translation/date-time/date-time-patterns#date-time-combined-formats). 
<!-- [CLDR-18350](https://unicode-org.atlassian.net/browse/CLDR-18350) -->

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

### Timezones, metazones and exemplar cities

#### New `gmtUnknownFormat`

Normally time zones formatted using UTC offset (like xxxx) use the `gmtFormat` pattern (“GMT{0}” in root). The new `gmtUnknownFormat` is used when formatting time zones using a UTC offset for cases when the offset or zone is unknown. The root value “GMT+?” need not be changed if it works for your locale; however it should be consistent with the `gmtFormat` and `gmtZeroFormat` in your locale. See [Time Zones and City names](translation/time-zones-and-city-names) 
<!-- [CLDR-18236](https://unicode-org.atlassian.net/browse/CLDR-18236) -->

#### “Unknown City” → “Unknown Location”

For zone `Etc/Unknown`, the exemplarCity name was changed in English from “Unknown City” to “Unknown Location”; other locales should update accordingly. 
<!-- [CLDR-18262](https://unicode-org.atlassian.net/browse/CLDR-18262) -->

#### Changes to the root and/or English names of many exemplar cities and some metazones

(TBD [CLDR-18249](https://unicode-org.atlassian.net/browse/CLDR-18249)

### Number formats

#### Currency patterns alphaNextToNumber, noCurrency

<!-- this is unnecessary for vetters to worry about. 
There actually added in CLDR 42 per (CLDR-14336)[https://unicode-org.atlassian.net/browse/CLDR-14336]. However, they were not properly set up for coverage and inheritance, and were not presented to many vetters. These issue were corrected in CLDR 47 per [CLDR-17879](https://unicode-org.atlassian.net/browse/CLDR-17879), which adjusted the data for some locales (and made it draft="provisional"). Many vetters will see these for the first time in CLDR 48.
-->
- The `alphaNextToNumber` patterns should be used when currency symbol is alphabetic, such as “USD”; in this case the m=pattern may add a space to offset the currency symbol from the numeric value, if the standard pattern does not already include a space.
    - **Note that some currency units may only be alphabetic at the start or end, such as CA$ or $CA.
This pattern will be used if an alphabetic character would end up being adjacent to a number in the regular pattern.
So suppose that the regular pattern is "¤#,##0" and this pattern is "¤ #,##0":
$CA would use this pattern ("$CA 123"), but CA$ would just use the regular pattern to get "CA$123".**
- The `alphaNextToNumber` patterns should be used when the currency amount should be formatted without a currency symbol, as in a table of values all using the same currency. This pattern must not include the currency symbol pattern character ‘¤’.

For more information see [Number and currency patterns](/translation/number-currency-formats/number-and-currency-patterns).

#### Rational formats

These describe the formatting of rational fractions such as ¾ or combinations of integers and fractions such as 5½. 
<!-- [CLDR-17570](https://unicode-org.atlassian.net/browse/CLDR-17570) -->

Here are the the English values and a short description of their purpose:
- `rationalFormats-rationalPattern`: “{0}⁄{1}” - The format for a rational fraction with arbitrary numerator and denominator; the English pattern uses the Unicode character ‘⁄’ U+2044 FRACTION SLASH which causes composition of fractions such as 22⁄7.
- `rationalFormats-integerAndRationalPattern`: “{0} {1}” - The format for combining an integer with a rational fraction composed using the pattern above; the English pattern uses U+202F NARROW NO-BREAK SPACE (NNBSP) to produce a small no-break space.
- `rationalFormats-integerAndRationalPattern-superSub`: “{0}⁠{1}” - The format for combining an integer with a rational fraction using composed using the pattern above; the English pattern uses U+2060 WORD JOINER, a zero-width no-break space.
- `rationalFormats-rationalUsage`: “sometimes” - An indication of the extent to which rational fractions are used in the locale; may be one of “never”, “sometimes”, ... (TBD)

If an integer and fraction (5½) is best expressed in your language with a space between them (5 ½),
then copy the pattern from integerAndRationalPattern to integerAndRationalPattern-superSub.
However, you **cannot** do the reverse.
Some fonts and rendering systems don't properly handle the fraction slash, and the user would see something like 51/2 (fifty-one halves).
So in that case, implementations must have the integerAndRationalPattern with a space in it to fall back on,
unless they have verified that the font / rendering system supports superscripting the numerator.

See [Rational Formatting](https://cldr.unicode.org/translation/number-currency-formats/number-and-currency-patterns#rational-formatting) for more information.

### Units

#### Rework certain concentration units 

The keys for two units changed (the translations can probably remain the same) and there is one new unit that is used for constructing certain other kinds of concentration units:
<!-- [CLDR-18274](https://unicode-org.atlassian.net/browse/CLDR-18274) -->
- key `permillion` changed to `concentr-part-per-1e6`; English values remain “parts per million”, “{0} part per million”, etc.
- key `portion-per-1e9` changed to `concentr-part-per-1e9`; English values remain “parts per billion”, “{0} part per billion”, etc.
- new key `part` used for constructing arbitrary concentrations such as “parts per 100,000”; English values “parts”, “{0} part”, etc.

**Please check over the values for `concentr-part-per-1e6` and `concentr-part-per-1e9` in your locale.
Some languages had used the equivalent of "millionths" instead of the equivalent of "parts per million".**

For more information see [Concentrations](/translation/units/unit-names-and-patterns#concentrations).

#### Many new units in English

Mnny new units were added in English. 
Vetters will not be asked to translate the non-metric units  for other languages.
However, the metric ones are used in scientific contexts, and will need to be translated.
However, the case inflections (accusative, dative, etc) will not be requested. 
<!-- In general these are very specific and vetters will not be
asked to translate them for other locales, so coverage will be comprehensive. If some of these units
would be useful in particualr locales, please file a ticekt and the coverage can be adjusted.
[CLDR-18215](https://unicode-org.atlassian.net/browse/CLDR-18215) -->

The units (English names) are: 
- angle: steradians 
- area: bu [JP], cho [JP], se [JP] (Japanese units)
- duration: fortnights 
- concentr: katals 
- electric: coulombs, farads, henrys, siemens 
- energy: becquerels, British thermal units [IT], calories [IT], grays, sieverts 
- force: kilograms-force 
- length: chains, rods; jo [JP], ken [JP], ri [JP], rin [JP], shaku [cloth, JP], >shaku [JP], sun [JP] (Japanese units)
- magnetic: teslas, webers 
- mass: slugs; fun [JP] (Japanese unit)
- temperature: rankines 
- volume: metric fluid ounces; cups Imperial, pints Imperial; cup [JP], koku [JP], kosaji [JP], osaji [JP], sai [JP], shaku [volume, JP], to [JP] (Japanese units)

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

1. Redirect from read-only locale to the default content locale does not work [CLDR-18513][]
2. Images for the plain symbols. Non-emoji such as [€](https://st.unicode.org/cldr-apps/v#/fr/OtherSymbols/47925556fd2904b5), √, », ¹, §, ... do not have images in the Info Panel. [CLDR-13477](https://unicode-org.atlassian.net/browse/CLDR-13477) **Workaround**: Look at the Code column; unlike the new emoji, your browser should display them there.
1. [CLDR-17683](https://unicode-org.atlassian.net/browse/CLDR-17683) - Some items are not able to be flagged for TC review. This is being investigated.Meanwhile, Please enter forum posts meanwhile with any comments.

## Resolved Issues

Last updated: 2025-04-07

1. [CLDR-17694](https://unicode-org.atlassian.net/browse/CLDR-17694) - Back button in browser fails in forum under certain conditions
2. [CLDR-17658](https://unicode-org.atlassian.net/browse/CLDR-17658) - Dashboard slowness

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

[CLDR-18513]: https://unicode-org.atlassian.net/browse/CLDR-18513
[CLDR-18423]: https://unicode-org.atlassian.net/browse/CLDR-18423
[CLDR-17694]: https://unicode-org.atlassian.net/browse/CLDR-17694

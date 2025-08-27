---
title: Information Hub for Linguists
---

# Information Hub for Linguists

The following list summarizes the recent changes, with more details in a section further down the page.
A sequence like 🆕 2025-05-09 marks items that have been recently added.
In your browser you can copy this sequence, then use ⌘-F (Mac) or Ctrl-F (Windows) to find all the places it occurs.
- **🆕 2025-06-04**
  - Some people have thought they couldn't vote on change-protected items, like the new [Characters in Use: punctuation-person](https://st.unicode.org/cldr-apps/v#/de/Alphabetic_Information/44e72c2489fcec13) (because the Survey Tool messaging was misleading!). Please read [Change Protected Items].
- **🆕 2025-06-04**
  -[CLDR-18712][] - Unit inflections issue is now fixed.
- **🆕 2025-06-04**
  - [CLDR-18712][] - Inflections are showing up for new units unexpectedly where grammar was not added. We are currently working on removing the unnecessary items.
- **🆕 2025-06-03**
  - Emoji search keywords are separated by a `|` character (U+007C `|` VERTICAL LINE). Some users had used the wrong character (such as U+FF5C	`｜` FULLWIDTH VERTICAL LINE). Those have been corrected, and will now be automatically converted whenever people enter the wrong character.
  - A reminder: whenever people enter keywords, they are ordered automatically (with a language-independent ordering). With some languages (eg, Icelandic) the _blue star_ items were in the wrong order, leading votes to appear in a different order. This also causes the wrong character for `|` items to be reordered.
  - The [ZWG currency code](https://en.wikipedia.org/wiki/Zimbabwean_ZiG), [XCG currency code](https://en.wikipedia.org/wiki/Caribbean_guilder)  and [Min Nan language code](https://en.wikipedia.org/wiki/Southern_Min) were not shown for translation in some locales. That has been corrected. Note that Min Nan will only show up with Coverage=Comprehensive.
  - You can now search for emoji characters, such as the 💔. Use the 🔍 control at the top right.
  - Plural rules were added for Konkani (kok), so the "one" category will show up.
  - Various infrastructure improvements were made. For example, some examples were not updating correctly when they used data from other fields.
- **🆕 2025-05-28**
  - Many people are not voting correctly for **Characters in Use**: [punctuation-person](https://st.unicode.org/cldr-apps/v#/USER/Alphabetic_Information/3be8e5f5960f3603)
- **🆕 2025-05-26**
  - Updated zone names
  - Please review the Gregorian [flexible date/time](https://st.unicode.org/cldr-apps/v#/USER/Gregorian/header_Formats_Flexible_Date_Formats) and [interval date/time formats](https://st.unicode.org/cldr-apps/v#/USER/Gregorian/header_Formats_Intervals_Date_Formats).
There are inconsistencies between formats in many locales. New examples and warnings have been added to help you see the issues.
- 🆕 2025-05-22
  - Added keycap emoji. These were synthesized formerly, but are now available for translation: see [Keycaps](https://st.unicode.org/cldr-apps/v#/USER/EmojiSymbols/header_keycap).
- 🆕 2025-05-18
  - Added missing date & time formats:
    - GyM and GyMEd
    - Time formats: EBh, Eh, EH when there is an existing EBhm, Ehm, and EHm in the calendar respectively.
    - Changed HH patterns in available and interval formats to include a reference to hour since seeing an hour number alone is ambigious.
  - The region Sark, CQ, is now in modern coverage
- 🆕 2025-05-09
  - New flexible format and interval patterns
  - Gregorian Calendar (Year First) calendar
  - 2025-05-09 Enhanced "Show Hidden"

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

The Survey Tool is now open for [General Submission](translation/getting-started/survey-tool-phases#survey-tool-phase-general-submission) for CLDR 48. 
The General Submission phase will be followed by the [Vetting phase](translation/getting-started/survey-tool-phases#survey-tool-phase-vetting) starting on June 11th.

- **Disconnect error**. If you see a persistent Loading error with a disconnect message or other odd behavior, please [empty your cache](translation/getting-started/empty-cache).
- Survey Tool email notification may be going to your spam folder. Check your spam folder regularly.
- "**Same as code**" errors - when translating codes for items such as languages, regions, scripts, and keys, it is normally an error to select the code itself as the translated name.
If the error appears under Typography, you can ignore it. <!-- [[CLDR-13552](https://unicode-org.atlassian.net/browse/CLDR-13552)\] -->

## New languages

The following new languages are available in the Survey Tool for submission during the CLDR 48 period:

- Buryat (bua)
- Coptic (cop)
- Haitian Creole (ht)
- 🆕 2025-05-18 — Hmong Daw (mww)
- Kazakh (Latin) (kk_Latn)
- Laz (lzz)
- Luri Bakhtiari (bqi)
- Nselxcin (Okanagan) (oka)
- Pāli (pi)
- Piedmontese (pms)
- Q’eqchi’ (kek)
- Samogitian (sgs)
- Sunuwar (suz)
- Chinese (Latin) (zh-Latn)

## New Areas

Most of the following are relevant to locales at the Modern Coverage Level.

### New emoji

Seven new emoji have been added (images below). These were released in Unicode 17 in September 2025.

![emoji image](/images/Unicode17emoji.png)

### Core Data
There are new Alphabetic Information items.
- `numbers-auxiliary` — If there are characters used in numbers that are not customarily used, but may occur, add them here instead of in `auxiliary`.
- `punctuation-auxiliary` — If there are punctuation characters that are not customarily used, but may occur, add them here instead of in `auxiliary`.
- `punctuation-person` — If there are punctuation characters that are customarily used in people's names in standard documents, add them here.
This should be a small list such as “.” or “-”.
Do **not** include ‘fanciful’ characters such as emoji or [kaomoji](https://en.wikipedia.org/wiki/Kaomoji).

**🆕 2025-05-18** — Many people are not voting correctly for **Characters in Use**: [punctuation-person](https://st.unicode.org/cldr-apps/v#/USER/Alphabetic_Information/3be8e5f5960f3603), especially for non-Latin scripts, such as Japanese and Chinese.  
1. It should *only* have punctuation that is used in *your* language with names.
But include **both** native-script names *and* foreign-script names (that are transliterated into your native script).  
3. Include punctuation in compound names (including transliterated, such as  “让-路易·加西” or “ジャン゠ルイ・ガセー” for “Jean-Louis Gassée”) —
For example, currently Chinese \[·] and Japanese \[゠・] are missing those characters, when compared to Wikipedia.
4. You should include ‘look alike’ characters that are in common use, such as ＝(full-width equals) for ゠(kana double hyphen);
5. Include punctuation that is used in special fields like titles (“Prof. Dr”), credentials (“MD, PhD)”, and generation (“Jr.”)

More information is available in the [Exemplars section of the Unicode Sets page](https://cldr.unicode.org/translation/core-data/exemplars#exemplar-characters)

### Locale display names

🆕 2025-05-18 — Sark, CQ, is now in modern coverage under Locale Display Names > Territories (Europe) > Northern Europe > [Sark](https://st.unicode.org/cldr-apps/v#/USER/T_Europe/7df36d3a79aacaf4)

#### Languages names which were added or changed in English

As new locales reach Basic Coverage, their language names are added for locales targeting modern coverage. This will be happening the week of April 28th.

- tkl: English name changed to Tokelauan.

#### Core/Extensions

There is a new mechanism for better menu names. When you see a **Code** with `-core` or `-extension`, 
please read [Locale Option Value Names](/translation/displaynames/locale-option-names-key).

🆕 2025-05-21 — The link in the Info Panel was not pointing to [Locale Option Value Names](/translation/displaynames/locale-option-names-key); 
that has been fixed. 
There is also now a Full List of the option names on that page.

#### Scripts

There are 5 new scripts for Unicode 17. Currently the names are in English: Beria Erfe, Chisoi, Sidetic, Tai Yo, Tolong Siki.
Coverage for other languages is at comprehensive. 
<!-- I don't think we want to encourage this: ... if there is a need to have coverage at lower level in some locale,
please file a ticket. [CLDR-18283](https://unicode-org.atlassian.net/browse/CLDR-18283) -->

### DateTime formats

#### Gregorian Calendar (Year First) calendar
🆕 2025-05-09 — This is a variant of the Gregorian calendar whose formats always use year-month-day ordering and a 24-hour time cycle. 
See [Year First Calendar] for more details.
*Note: the code is `iso8601`, but disregard that; it will be changed after submission.*

<!-- [CLDR-18447](https://unicode-org.atlassian.net/browse/CLDR-18447) -->

#### New flexible format and interval patterns 
🆕 2025-05-09 — There are some new patterns for you to supply. Make sure that the format is consistent with related patterns. 

Note: Some locales have inconsistent patterns using eras: in some patterns using G (AD vs BC in Gregorian) but in related patterns using GGGGG (which is a narrow form: A vs B in Gregorian).
The GGGGG is not typically needed except in special cases, such as the Japanese calendar.

**🆕 2025-05-28** — Please review the Gregorian [flexible date/time](https://st.unicode.org/cldr-apps/v#/USER/Gregorian/header_Formats_Flexible_Date_Formats) and [interval date/time formats](https://st.unicode.org/cldr-apps/v#/USER/Gregorian/header_Formats_Intervals_Date_Formats). 
There are inconsistencies between formats in many locales. 
New examples and warnings have been added to help you see the issues:

Intervals have "Related Flexible Dates" that show inconsistencies between the intervals and flexible. 
For example, in the following the era (n.C.) is on the opposite side.

![Screenshot 2025-05-25 at 17 35 42](https://github.com/user-attachments/assets/35ac8d16-2286-41c0-a978-9acb9b2aa6e5)

Available formats show related formats. 
For example, in the following the format with the day of the week (So) has zero-padded hours, while the format without the day of the week doesn't.

![Screenshot 2025-05-25 at 17 39 13](https://github.com/user-attachments/assets/96c4a2d9-8e6d-4127-9d73-7836c8d78f45)

New warnings are available for cases where the removal of an era (G), day-of-week (E), or timezone (v) changes the pattern unexpectedly. 
In the warning below, one pattern is in the order **year-month-day** and has **zero-padded** days (dd), 
while the other is in the order **day-month-year**, with **unpadded days** (d). 

![Screenshot 2025-05-25 at 17 50 09](https://github.com/user-attachments/assets/8a8dbda6-e7cb-4fd9-9c76-4f49b0c8110b)

**Note that `y` is favored over `yy` in date patterns.**
The `y` will show the year 2011 as "2011", while `yy` will truncate to "11", which could be a year, month, or day:
and even if it is known to be a year, could be either 2011 or 1911.

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
of a relative date and a single time.

If you do not supply this, that combination will fall back to using the “standard” variant;
in English that would produce “tomorrow, 3:00 PM”. If instead you want the same combining behavior for a relative date with a single time as for a
fixed date with single time (as was the case in CLDR 47 and earlier), then for each length style copy the existing “atTime” form to the new “relative” form.

#### Missing date & time patterns

🆕 2025-05-18 — Some dates and times are ambiguous due to missing patterns. These additional patterns have been added to resolve this issue:

  - GyM and GyMEd
  - Time formats: EBh, Eh, EH when there is an existing EBhm, Ehm, and EHm in the respective calendar.
  - Changed HH patterns in available and interval formats to include a reference to hour since seeing an hour number alone is ambiguous.

### Timezones, metazones and exemplar cities

#### New `gmtUnknownFormat`

Normally time zones formatted using UTC offset (like xxxx) use the `gmtFormat` pattern (“GMT{0}” in root). 
The new `gmtUnknownFormat` is used when formatting time zones using a UTC offset for cases when the offset or zone is unknown. 
The root value “GMT+?” need not be changed if it works for your locale; 
however it should be consistent with the `gmtFormat` and `gmtZeroFormat` in your locale. 
See [Time Zones and City names](translation/time-zones-and-city-names) 
<!-- [CLDR-18236](https://unicode-org.atlassian.net/browse/CLDR-18236) -->

#### “Unknown City” → “Unknown Location”

For zone `Etc/Unknown`, the exemplarCity name was changed in English from “Unknown City” to “Unknown Location”; other locales should update accordingly. 
<!-- [CLDR-18262](https://unicode-org.atlassian.net/browse/CLDR-18262) -->

#### Changes to the root and/or English names of many exemplar cities and some metazones

Exemplar cities added or changed in English.
This was typically to move towards the official spelling in the country in question, such as retaining accents, or to add landscape terms such as "Island".
You should check these, but don't hesitate to retain the older version(<image src='https://github.com/unicode-org/cldr/blob/main/tools/cldr-apps/src/main/webapp/star.png'>) if it is a different script or more customary in your language.
For example, English still uses "Mexico City" instead of "Ciudad de México".

| Code | New Value | Code | New Value |
| :---- | :---- | :---- | :---- |
| Africa/El\_Aaiun | El Aaiún | Antarctica/Casey | Casey Station |
| Africa/Lome | Lomé | Antarctica/DumontDUrville | Dumont d’Urville Station |
| Africa/Ndjamena | N’Djamena | Antarctica/McMurdo | McMurdo Station |
| America/Araguaina | Araguaína | Asia/Aqtau | Aktau |
| America/Argentina/Rio\_Gallegos | Río Gallegos | Asia/Hovd | Khovd |
| America/Argentina/Tucuman | Tucumán | Asia/Qyzylorda | Kyzylorda |
| America/Belem | Belém | Asia/Urumqi | Ürümqi |
| America/Bogota | Bogotá | Atlantic/Canary | Canarias |
| America/Cordoba | Córdoba | Europe/Busingen | Büsingen |
| America/Cuiaba | Cuiabá | Europe/Chisinau | Chișinău |
| America/Eirunepe | Eirunepé | Europe/Tirane | Tirana |
| America/Maceio | Maceió | Indian/Chagos | Chagos Archipelago |
| America/Mazatlan | Mazatlán | Indian/Comoro | Comoros |
| America/Mexico\_City | Ciudad de México | Indian/Kerguelen | Kerguelen Islands |
| America/Miquelon | Saint-Pierre | Indian/Mahe | Mahé |
| America/Santarem | Santarém | Pacific/Chatham | Chatham Islands |
| America/Sao\_Paulo | São Paulo | Pacific/Galapagos | Galápagos |
| Antarctica/Rothera | Rothera Station | Pacific/Kwajalein | Kwajalein Atoll |
| Antarctica/Palmer | Palmer Land | Pacific/Marquesas | Marquesas Islands |
| Antarctica/Troll | Troll Station | Pacific/Midway | Midway Atoll |
| Antarctica/Syowa | Showa Station | Pacific/Noumea | Nouméa |
| Antarctica/Mawson | Mawson Station | Pacific/Pitcairn | Pitcairn Islands |
| Antarctica/Vostok | Vostok Station | Pacific/Wallis | Wallis & Futuna |

🆕 2025-05-26

| Code | New Value | Code | New Value |
| :---- | :---- | :---- | :---- |
| America/Noronha | Fernando de Noronha | Pacific/Chatham | Chatham Islands |
| Antarctica/Macquarie | Macquarie Island | Pacific/Easter | Easter Island |
| Atlantic/Canary | Canaries | Pacific/Enderbury | Canton Island |
| Atlantic/Faeroe | Faroes  | Pacific/Galapagos | Galapagos Island |
| Australia/Lord_Howe | Lord Howe Island | Pacific/Kwajalein | Kwajalein Atoll |
| Indian/Christmas | Christmas Island | Pacific/Norfolk | Norfolk Island |
| Indian/Cocos | Cocos Islands | Pacific/Pitcairn | Pitcairn Islands |
| Indian/Kerguelen | Kerguelen Islands | Pacific/Wake | Wake Island |

Metazones:

- _Hovd Time_ changed to _Khovd Time_
- _Qyzylorda Time_ changed to _Kyzylorda Time_

🆕 2025-05-26
- _Apia Time_ changed to _Samoa Time_
- _Brunei Darussalam Time_ changed to _Brunei Time_
- _Cook Islands Half Summer Time_ changed to _Cook Islands Summer Time_
- _East Timor Time_ changed to _Timor-Leste Time_
- _Petropavlovsk-Kamchatski Time_ changed to _Kamchatka Time_
- _Ponape Time_ changed to _Pohnpei Time_
- _Pyongyang Time_ changed to _North Korea Time_
- _Samoa Time_ changed to _American Samoa Time_
- _Taipei Time_ changed to _Taiwan Time_

<!-- [CLDR-18249](https://unicode-org.atlassian.net/browse/CLDR-18249) -->

### Number formats

#### Currency patterns alphaNextToNumber, noCurrency

- The `alphaNextToNumber` patterns should be used when a currency symbol is alphabetic, such as “USD”;
in this case the m=pattern may add a space to offset the currency symbol from the numeric value, if the standard pattern does not already include a space.
    - **Note that some currency units may only be alphabetic at the start or end, such as CA$ or $CA.
This pattern will be used if an alphabetic character would end up being adjacent to a number in the regular pattern.
So suppose that the regular pattern is "¤#,##0" and this pattern is "¤ #,##0":
$CA would use this pattern ("$CA 123"), but CA$ would just use the regular pattern to get "CA$123".**
- The `noCurrency` patterns should be used when the currency amount is to be formatted without a currency symbol, as in a table of values all using the same currency.
This pattern must not include the currency symbol pattern character ‘¤’.

For more information see [Number and currency patterns](/translation/number-currency-formats/number-and-currency-patterns).

#### Rational formats

These patterns specify the formatting of rational fractions in your language. 
Rational fractions contain a numerator and denominator, such as ½, and may also have an integer, such a 5½.
There are two different "combination patterns", needed because sometimes fonts don't properly support fractions (such as displaying 5 1/2),
and need two patterns: one with a space and one without.
It can be tricky to understand the difference,
so be sure to carefully read [Rational Formatting](https://cldr.unicode.org/translation/number-currency-formats/number-and-currency-patterns#rational-formatting) before making any changes.

<!-- [CLDR-17570](https://unicode-org.atlassian.net/browse/CLDR-17570) -->

Here are the the English values and a short description of their purpose:

Code | Default Value | Description
-|:-:|-
`Rational` | {0}⁄{1} | The format for a rational fraction with arbitrary numerator and denominator; the English pattern uses the Unicode character ‘⁄’ U+2044 FRACTION SLASH which causes composition of fractions such as <sup>22</sup>⁄<sub>7</sub>.
`Integer + Rational` | {0} {1} | The format for combining an integer with a rational fraction that is composed using the `Rational` pattern; the English pattern uses U+202F NARROW NO-BREAK SPACE (NNBSP) to produce a `non-breaking thin space`.
`Integer + Rational-superSub` | {0}⁠{1} | The format for combining an integer with a rational fraction that is composed using the `Rational` pattern; the English pattern uses U+2060 WORD JOINER, a zero-width no-break space. **See below for the difference from `Integer + Rational`**
`Usage` | sometimes | An indication of the extent to which rational fractions are used in the locale; must be either `never` or `sometimes`.

**If** an integer and fraction (**5½**) is best expressed in your language with a space between them (**5 ½**),
then copy the pattern from `Integer + Rational` to `Integer + Rational-superSub`.
However, you **cannot** do the reverse.
Some fonts and rendering systems don't properly handle the fraction slash, and the user would see something like **51/2** (fifty-one halves)!
So in that case, implementations must have the `Integer + Rational` with a space in it to fall back on,
unless they have verified that the font / rendering system supports superscripting the numerator.

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

Many new units were added in English. 
The _metric_ ones are used in scientific contexts, and will need to be translated in all languages.
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

Last updated: 2025-06-05

This list will be updated as fixes are made available in Survey Tool Production. If you find a problem, please [file a ticket](requesting_changes), but please review this list first to avoid creating duplicate tickets.

1. 🆕 2025-06-04 [CLDR-18689][] - Languages are sorted by full English name instead of core element, and may appear on different pages in Locale Display Names
1. [[CLDR-18577][] - If your language does not have a variant value, you can vote for inheritance from the standard version.
1. [CLDR-17829][] - some links in the Info panel not displaying properly
1. [CLDR-13477][] - Images for the plain symbols. Non-emoji such as [€](https://st.unicode.org/cldr-apps/v#/fr/OtherSymbols/47925556fd2904b5), √, », ¹, §, ... do not have images in the Info Panel.  **Workaround**: Look at the Code column; unlike the new emoji, your browser should display them there.
1. [CLDR-17683][] - Some items are not able to be flagged for TC review. This is being investigated. Meanwhile, Please enter forum posts meanwhile with any comments.
1. [CLDR-18607][] - Unable to download current votes in CSV
1. [CLDR-18615][] - Unclear error message if a link sends you to a page that no longer exists in the Survey Tool

## Resolved Issues

Last updated: 2025-06-05

1. 🆕 2025-06-05 [CLDR-18712][] - Inflections are showing up for new units unexpectedly where grammar was not added
1. 🆕 2025-06-03 - [CLDR-18687][] - Fix ordering of keycaps keywords in emoji annotations
1. 🆕 2025-06-03 - [CLDR-18692][] - Automatically fix delimiters in emoji annotation keywords with DAIP
1. 🆕 2025-06-03 - [CLDR-18588][] - Fix XCG and ZWG which were not showing up properly in the Survey Tool
1. 🆕 2025-06-03 - [CLDR-18691][] - Emojis are now searchable in Survey Tool search
1. 🆕 2025-06-03 - [CLDR-18637][] - Don't show example pop-ups if no example is available
1. 🆕 2025-06-03 - [CLDR-18627][] - Allow nan to show up in comprehensive for all locales
1. 🆕 2025-05-18 — [CLDR-18605][] - Fix issue blocking import of winning votes from the previous cycle
1. 🆕 2025-05-18 — [CLDR-18649][] - Same as root is now a warning if English is the same as root as well
1. [CLDR-18513][] - Redirect from read-only locale to the default content locale does not work
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


<!-- Tickets are in ascending order for easier maintence -->
[CLDR-13477]: https://unicode-org.atlassian.net/browse/CLDR-13477
[CLDR-17683]: https://unicode-org.atlassian.net/browse/CLDR-17683
[CLDR-17694]: https://unicode-org.atlassian.net/browse/CLDR-17694
[CLDR-17829]: https://unicode-org.atlassian.net/browse/CLDR-17829
[CLDR-18423]: https://unicode-org.atlassian.net/browse/CLDR-18423
[CLDR-18513]: https://unicode-org.atlassian.net/browse/CLDR-18513
[CLDR-18577]: https://unicode-org.atlassian.net/browse/CLDR-18577
[CLDR-18588]: https://unicode-org.atlassian.net/browse/CLDR-18588
[CLDR-18605]: https://unicode-org.atlassian.net/browse/CLDR-18605
[CLDR-18607]: https://unicode-org.atlassian.net/browse/CLDR-18607
[CLDR-18615]: https://unicode-org.atlassian.net/browse/CLDR-18615
[CLDR-18627]: https://unicode-org.atlassian.net/browse/CLDR-18627
[CLDR-18637]: https://unicode-org.atlassian.net/browse/CLDR-18637
[CLDR-18649]: https://unicode-org.atlassian.net/browse/CLDR-18649
[CLDR-18687]: https://unicode-org.atlassian.net/browse/CLDR-18687
[CLDR-18689]: https://unicode-org.atlassian.net/browse/CLDR-18689
[CLDR-18691]: https://unicode-org.atlassian.net/browse/CLDR-18691
[CLDR-18692]: https://unicode-org.atlassian.net/browse/CLDR-18692
[CLDR-18712]: https://unicode-org.atlassian.net/browse/CLDR-18712
[stand-alone vs. formatting]: /translation/date-time/date-time-patterns#when-to-use-standalone-vs-formatting
[Year First Calendar]: /translation/date-time/date-time-patterns#year-first-calendar
[Example Hidden]: https://st.unicode.org/cldr-apps/v#/USER/Number_Formatting_Patterns/67afe297d3a17a3
[Key for Show Hidden]: https://cldr.unicode.org/translation/core-data/exemplars#key-to-escapes
[**input** these from the keyboard]: /translation/core-data/exemplars#input
[Change Protected Items]: https://cldr.unicode.org/translation/getting-started/guide#changing-protected-items

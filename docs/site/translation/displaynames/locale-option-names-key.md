---
title: 'Locale Option Names'
---

<!-- TODO
Update the tables to the latest English names.
Consider changing the English names for clarity.
-->

Locale codes can have special variants to indicate the use of particular calendars or other features. 
They can be used to select different options in menus, and to display which options are currently in effect for the user. 
The locale codes use a specific format, like `de_DE`. The format for these locales are assembled from the names of the language, script, region, and so on. 
Locale codes can also carry options, such as when users will expect digits in their native script or ASCII digits (aka Latin digits). 
The full name of a locale will include those options in a short format, such as _German (Germany, Buddhist Calendar)_.

Options have a 'title' and a 'value'.
For example, look at the following menu.
The _title_ is "Calendar" and the _values_ are Gregorian, Buddhist, Chinese, etc.

- **Calendar**
        - Gregorian
        - Buddhist
        - Chinese
        - …

You will be asked to supply these names in your language for the option title and for various option values.
The title is the first item under the header, eg:

<img width="605" height="165" alt="Screenshot 2026-04-27 at 17 02 26" src="https://github.com/user-attachments/assets/dfbe21a3-7eea-4ace-a500-478a99149f9a" />

The values will be listed underneath, all ending with "-core" in the Code cell.
For example:

<img width="606" height="82" alt="Screenshot 2026-04-27 at 17 06 56" src="https://github.com/user-attachments/assets/cb2a412f-8497-4d9a-abee-baf31465bbde" />

There are also _combined title-value_ items, which are a combination of the title and a value. 
These have hyphens in the Code cell, but do not end in "-core".
For example:

<img width="609" height="110" alt="Screenshot 2026-04-27 at 17 09 56" src="https://github.com/user-attachments/assets/1b434dbc-677f-416a-b338-c1d1e17a6722" />

When displayed in a locale name, an option+value name is used in parentheses.
There are two formats for that:

| Format | Display Example |
| --| -- |
| combined title-value | anglais (calendrier bouddhiste) |
| constructed title-value | anglais (calendrier: bouddhiste) |

The _constructed title-value_ is created by using the title, the value, and a [localeKeyTypePattern](https://st.unicode.org/cldr-apps/v#/USER/Locale_Name_Patterns/38d08336cae10c4e).

For the most common options, a combined title-value is used when available.
In other cases, the constructed title-pattern is used.

## Locale Option Names

Here are examples of names of Options to be translated.

| Key | Meaning   |
|---|---|
| Calendar | Calendar system (the European calendar is called "Gregorian"; others are the Chinese Lunar Calendar, and so on.) |
| Collation | How text is sorted (where a language has different possible ways to sort). |
| Currency | The default currency. (The value is any currency value, such as USD.) |
| Numbers | The numbering system in use, such as European (0,1,2), Arabic (٠, ١, ٢ ), Devanagari ( ०,  १,  २ ). <br /> - Usually these are just derived from the name of the script.<br /> - There are some special forms, such as "Simplified Chinese Financial Numerals" or "Full Width Digits". |

## On Off

These values are used to select certain BCP47 types that have On versus Off meaning.

For more details, see [On Off].

Rather than translate many different similar phrases,
that is **Do Ignore Symbols Sorting / Don’t Ignore Symbols Sorting**, 
there are simply values for **On** and **Off** which are used together with these.
Examples of how these are used:

* Ignore Symbols Sorting: **On**
* Reversed Accent Sorting: **Off**
* Uppercase/Lowercase Ordering: **On**

### Guidelines

* Choose the best terms that indicate that an option is active or not in a menu.
* The values are not inflected according to the option, so choose the form that is most generally applicable.

## Full List

The following provides a full list of keys and values.
You may see just a subset of the keys and/or their values, depending on your coverage level. 
(It excludes the Transform options, because those are used less often.)

Note that in many cases the values do not need to be translated, because the data is available elsewhere. 
For example, 
* numbering systems — most are identified by their Script code (because the Script names are already available)
* first day-of-week - the days of the week are available elsewhere

### Dates

#### Calendar

Codes: ca, calendar

This option allows the choice of calendar.

|Codes|English Name|Description|
|-|-|-|
|buddhist|Buddhist|Thai Buddhist calendar|
|chinese|Chinese|Traditional Chinese calendar|
|coptic|Coptic|Coptic calendar|
|dangi|Dangi|Traditional Korean calendar|
|ethioaa, ethiopic-amete-alem|Ethiopic Amete Alem|Ethiopic calendar, Amete Alem (epoch approx. 5493 B.C.E)|
|ethiopic|Ethiopic|Ethiopic calendar, Amete Mihret (epoch approx, 8 C.E.)|
|gregory, gregorian|Gregorian|Gregorian calendar|
|hebrew|Hebrew|Traditional Hebrew calendar|
|indian|Indian National|Indian calendar|
|islamic|Hijri|Hijri calendar|
|islamic-civil|Hijri (tabular, civil epoch)|Hijri calendar, tabular (intercalary years [2,5,7,10,13,16,18,21,24,26,29] - civil epoch)|
|islamic-rgsa|Hijri, Saudi Arabia sighting|Hijri calendar, Saudi Arabia sighting|
|islamic-tbla|Hijri (tabular, astronomical epoch)|Hijri calendar, tabular (intercalary years [2,5,7,10,13,16,18,21,24,26,29] - astronomical epoch)|
|islamic-umalqura|Hijri (Umm al-Qura)|Hijri calendar, Umm al-Qura|
|iso8601|Gregorian YMD|Gregorian calendar, but all fields in descending order: era, year, month, day, day-of-week, hour, minute, second|
|japanese|Japanese|Japanese Imperial calendar|
|persian|Persian|Persian calendar|
|roc|Minguo|Republic of China calendar|


#### Hour Cycle

Codes: hc, hours

This option allws the choice of hour cycle (12 vs 24, and starting with zero or one).

|Codes|English Name|Description|
|-|-|-|
|c12|12|The best choice of 12 hour cycle for the locale: starting with zero or with one|
|c24|24|The best choice of 24 hour cycle for the locale: starting with zero or with one|
|h11|12 (0–11)|Hour system using 0–11; 'K' in patterns|
|h12|12 (1–12)|Hour system using 1–12; 'h' in patterns|
|h23|24 (0–23)|Hour system using 0–23; 'H' in patterns|
|h24|24 (1–24)|Hour system using 1–24; 'k' in patterns|
|c24|24 (1–24)|Hour system using 1–24; 'k' in patterns|

### Sorting

Collation (alphabetic sorting) can be very different in different languages. For example, Swedish sorts å after z, while many other languages sort it after a.
(If you are interested, the [introduction section of the Unicode Collation Algorithm report](https://www.unicode.org/reports/tr10/#Introduction) is a good description of how collation works in software.)
The collation data is also used in searching, such as Search on Page in your browser.

#### Sort Order

Codes: co, collation

These allow for variant ways of sorting, such as:
* German sorting can be _standard_ or _phonebook_ (the latter treats umlauts as if they are spelled out)
— ä = ae, ö = oe, ü = ue, and ß = ss).
* emoji in a _standard_ order will appear jumbled because they are sorted by internal code points
— the _emoji_ option gives them a logical order. 

|Codes|English Name|Description|
|-|-|-|
|compat|Compatibility|A previous version of the ordering, for compatibility|
|dict, dictionary|Dictionary|Dictionary style ordering (such as in Sinhala)|
|ducet|Default Unicode|The default Unicode collation element table order|
|emoji|Emoji|Recommended ordering for emoji characters|
|eor|European rules|European ordering rules|
|phonebk, phonebook|Phonebook|Phonebook style ordering (such as in German)|
|phonetic|Phonetic|Phonetic ordering (sorting based on pronunciation)|
|pinyin|Pinyin|Pinyin ordering for Latin and for CJK characters (used in Chinese)|
|search|Search|Special collation type for string search|
|searchjl|Korean initial consonant|Special collation type for Korean initial consonant search|
|standard|Standard|Default ordering for each language|
|stroke|Stroke|Pinyin ordering for Latin, stroke order for CJK characters (used in Chinese)|
|trad, traditional|Traditional|Traditional style ordering (such as in Spanish)|
|unihan|Radical-Stroke|Pinyin ordering for Latin, Unihan radical-stroke ordering for CJK characters (used in Chinese)|
|zhuyin|Zhuyin|Pinyin ordering for Latin, zhuyin order for Bopomofo and CJK characters (used in Chinese)|

#### Uppercase/Lowercase Ordering

Codes: kf, colCaseFirst

Normally lower case characters are sorted before upper case, if there are no other differences.
For example: abbot < Abbot < abbots.
These options allow that collation preference to be controlled.

|Codes|English Name|Description|
|-|-|-|
|lower|Lowercase|Lower case to be sorted before upper case|
|upper|Uppercase|Upper case to be sorted before lower case|

#### Script/Block Reordering

Codes: kr, colReorder

Collation can also be optimized to sort groups of characters in a different order. 
These groups are: spaces, punctuation, numbers, currency symbols, other symbols, _and_ any scripts.
For example, people may want their native script to be before all others, or to have numbers sort after letters.
These options (plus scripts) are used in an ordered list to determine the order of the groups.

|Codes|English Name|Description|
|-|-|-|
|space|Whitespace | Spaces, tabs, thin space, … |
|punct|Punctuation| !, ", #, ¿ … |
|digit|Digit| 0123… ०१२३ …|
|currency|Currency | $, £, ¥, € …|
|symbol|Symbol | +, <, ≤, ∅, ∉ … (non-currency) |

#### Sorting Strength

Codes: ks, colStrength

This option specifies the "strength" of the ordering.
For example, at a primary level 'a', 'A', 'ä', 'Ⓐ' are all ordered identically.

|Codes|English Name|What isn't distinguished|
|-|-|-|
|primary|Base Letters Only|All but base letters|
|secondary|Base, Accents|Case, Variants, Kana|
|tertiary|Base, Accents, Case+Variants|Kana |
|quaternary|Base, Accents, Case, Variants, Kana|Canonical equivalents|
|identical|All|nothing — every code point is distinct|

#### Ignore Symbols Sorting

Codes: ka, colAlternate

There are two option values that affect how symbols and punctuation are sorted.
When the _shifted_ option is chosen, the "Highest Ignored" option determines exactly which groups of characters are affected.

|Codes|English Name|Description|
|-|-|-|
|noignore, non-ignorable|Punctuation… not ignored|Sort symbols, punctuation, and spaces logically|
|shifted|Punctuation… ignored|Ignore symbols, punctuation, and spaces|

NOTE: The strength level determines whether the characters are completely ignored, or whether they just act like a "weak" difference.

#### Highest Ignored

Codes: kv

This specifies which groups are affected by the "Ignore Symbols Sorting" option.

|Codes|English Name|Description|
|-|-|-|
|space|Shift Spaces|Only spaces are affected|
|punct|Shift spaces, punctuation|Spaces and punctuation are affected (default)|
|symbol|Shift spaces, punctuation, non-currency symbols|Spaces, punctuation and symbols except for currency symbols |
|currency|Shift Spaces, punctuation, all symbols|Spaces, punctuation and all symbols|

### Currencies

#### Currency Format

Codes: cf

Currency format

|Codes|English Name|Description|
|-|-|-|
|standard|Standard|Standard currency format|
|account|Accounting|Accounting currency format|

### Measurement

#### Measurement System

Codes: ms, measure

Measurement System

|Codes|English Name|Description|
|-|-|-|
|metric|Metric|Metric System|
|uksystem|UK|UK System of measurement: feet, pints, etc.; pints are 20oz|
|ussystem|US|US System of measurement: feet, pints, etc.; pints are 16oz|

### Numbers

#### Numbers

Codes: nu, numbers

There are many different numbering systems in use in the world.
This option allows the numbering system to be chosen.
The primary option values are based on script, such as 'latn' for ASCII 0,1,2,…
You do not need to translate those, because they use the name of the script.

The 'native' option is another common one. It chooses the numbering system associated with the locale's script, such as Devanagari digits for languages that use the Devanagari script (such as Hindi).
The 'traditional' option choses a traditional numbering system, if there is one.
Some scripts have alternate numbering systems, indicated with a suffix.

|Codes|English Name|Description|
|-|-|-|
|native|Native digits|Native digits|
|traditional|null|Traditional numerals — may be algorithmic|
|finance|Financial|Financial numerals — may be algorithmic|
|roman|Roman uppercase|Roman upper case numerals — algorithmic|
|romanlow|Roman lowercase|Roman lowercase numerals — algorithmic|
|arabext|Extended Arabic-Indic|Extended Arabic-Indic digits|
|armnlow|Armenian lowercase|Armenian lower case numerals — algorithmic|
|fullwide|Full-width|Full width digits|
|greklow|Greek lowercase|Greek lower case numerals — algorithmic|
|hanidays|Han-character day-of-month numbering for lunar/other traditional calendars|Han-character day-of-month numbering for lunar/other traditional calendars|
|hanidec|Positional decimal system using Chinese number ideographs as digits|Positional decimal system using Chinese number ideographs as digits|
|hansfin|Simplified Chinese financial|Simplified Chinese financial numerals — algorithmic|
|hantfin|Traditional Chinese financial|Traditional Chinese financial numerals — algorithmic|
|jpanfin|Japanese financial|Japanese financial numerals — algorithmic|
|jpanyear|Japanese first-year Gannen numbering|Japanese first-year Gannen numbering for Japanese calendar|
|lanatham|Tai Tham Tham (ecclesiastical)|Tai Tham Tham (ecclesiastical) digits|
|tamldec|Modern Tamil|Modern Tamil decimal digits|
|mymrepka|Myanmar Eastern Pwo Karen|Myanmar Eastern Pwo Karen digits|
|mymrpao|Myanmar Pao|Myanmar Pao digits|
|mymrshan|Myanmar Shan|Myanmar Shan digits|
|mymrtlng|Myanmar Tai Laing|Myanmar Tai Laing digits|
|mathbold|Mathematical bold|Mathematical bold digits|
|mathdbl|Mathematical double-struck|Mathematical double-struck digits|
|mathmono|Mathematical monospace|Mathematical monospace digits|
|mathsanb|Mathematical sans-serif bold|Mathematical sans-serif bold digits|
|mathsans|Mathematical sans-serif|Mathematical sans-serif digits|
|outlined|Outlined|Legacy computing outlined digits|
|segment|Segmented|Legacy computing segmented digits|

### Segmentation

#### Line Breaks within Words

Codes: lw

For languages that can linebreak in the middle of words (such as Japanese), there are a number of options to control how that happens. These are used by CSS (which controls formatting of HTML).

|Codes|English Name|Description|
|-|-|-|
|breakall|Break all|allow midword breaks unless forbidden by lb setting = CSS word-break=break-all|
|keepall|Keep all|prohibit midword breaks except for dictionary breaks = CSS word-break=keep-all|
|normal|Normal|normal script/language behavior for midword breaks = CSS word-break=normal|
|phrase|Keep in phrases|Prioritize keeping natural phrases (of multiple words) together when breaking, used in short text like title and headline|

#### CJK Line Break

Codes: lb

This is also used for CJK languages (Chinese, Japanese, Korean)

|Codes|English Name|Description|
|-|-|-|
|loose|Loose|CSS line-break=loose|
|normal|Normal|CSS line-break=normal, e.g. treat CJ as ID, break before hyphens for ja,zh|
|strict|Strict|CSS line-break=strict, e.g. treat CJ as NS|


#### Sentence Break After Abbr.

Codes: ss

Sentence break parameter key to control use of suppressions data

|Codes|English Name|Description|
|-|-|-|
|none|Off|Don't use segmentation suppressions data|
|standard|On|Use segmentation suppressions data of type standard|

### Misc

#### Emoji Presentation

Codes: em

Emoji presentation style request

|Codes|English Name|Description|
|-|-|-|
|default|Default|use the default presentation as specified in UTR #51|
|emoji|Emoji|use an emoji presentation for emoji characters if possible|
|text|Text|use a text presentation for emoji characters if possible|


#### Locale Variant

Codes: va

Common locale variant type

|Codes|English Name|Description|
|-|-|-|
|posix|POSIX variant|POSIX style locale variant|

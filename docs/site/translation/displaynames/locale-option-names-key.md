---
title: 'Locale Option Names'
---

# Locale Option Names

Locale codes can have special variants, to indicate the use of particular calendars or other features. They can be used to select among different options in menus, and also display which options are in effect for the user. The locale codes use a special format like `de_DE`; the names for these locales are assembled from names for language, script, region, and so on. Locale codes can also carry options, such as when users want to use native digits or ASCII digits (aka Latin digits). The full name of a locale will include those options in a short format, such as _German (Germany, Buddhist Calendar)_.

When displayed as a menu or in certain other contexts, the name of the _key_ for the option (such as _Calendar_) is split from the names of the different _values_ for that option (such as _Buddhist, Gregorian, Japanese_, etc.). That shorter version is called the core value.

You will be asked to translate the long name (such as _Buddhist Calendar_), plus the name of each key (_Calendar_) and the name of the "core" value (_Buddhist_). This allows systems to show users of your language what all the locale options are.

| Code | Name |
| -- | -- |
| `calendar` | Kalender |
| `calendar-buddhist` | Buddhistischer Kalender |
| `calendar-buddhist-core` | Buddhistischer |

The core name is typically used in menu listings or pull-down menus where the key name is used as the header, such as:
    - **Calendar**
        - Buddhist
        - Chinese
        - Coptic
        - …
     
This avoids the repetition that you would see if the long name were used:
    - **Calendar**
        - Buddhist Calendar
        - Chinese Calendar
        - Coptic Calendar
        - …

The core name can aso be used as an alternate form of a full locale name, such as "English (UK, Calendar: Buddhist)".


## Locale Option Names

Here are examples of names of Options to be translated.

| Key | Meaning   |
|---|---|
| Calendar | Calendar system (the European calendar is called "Gregorian"; others are the Chinese Lunar Calendar, and so on.) |
| Collation | How text is sorted (where a language has different possible ways to sort). |
| Currency | The default currency. (The value is any currency value, such as USD.) |
| Numbers | The numbering system in use, such as European (0,1,2), Arabic (٠, ١, ٢ ), Devanagari ( ०,  १,  २ ). <br /> - Usually these are just derived from the name of the script.<br /> - There are some special forms, such as "Simplified Chinese Financial Numerals" or "Full Width Digits". |

## Full List
The following provides a full list of keys and values. You may see just a subset of the keys and/or their values, depending on your coverage level. (It does exclude the Transform options, because those are used less often.)

Note that in many cases the values do not need to be translated, because the data is available elsewhere. For example, most numbering systems are identified by their Script code. Those are indicated in the list below by all-uppercase Codes.

### Dates

#### Calendar

Codes: ca, calendar

Calendar algorithm

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

Hour cycle (12 vs 24)

|Codes|English Name|Description|
|-|-|-|
|h11|12 (0–11)|Hour system using 0–11; 'K' in patterns|
|h12|12 (1–12)|Hour system using 1–12; 'h' in patterns|
|h23|24 (0–23)|Hour system using 0–23; 'H' in patterns|
|h24|24 (1–24)|Hour system using 1–24; 'k' in patterns|

### Sorting

The [introduction section of the Unicode Collation Algorithm report](https://www.unicode.org/reports/tr10/#Introduction) is a good description of how collation (sorting) works in software.

#### Sort Order

Codes: co, collation

Collation type

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


#### Ignore Symbols Sorting

Codes: ka, colAlternate

Collation parameter key for alternate handling

|Codes|English Name|Description|
|-|-|-|
|noignore, non-ignorable|Punctuation… not ignored|Variable collation elements are not reset to ignorable|
|shifted|Punctuation… ignored|Variable collation elements are reset to zero at levels one through three|


#### Uppercase/Lowercase Ordering

Codes: kf, colCaseFirst

Collation parameter key for ordering by case

|Codes|English Name|Description|
|-|-|-|
|lower|Lowercase < uppercase|Lower case to be sorted before upper case|
|upper|Uppercase < lowercase|Upper case to be sorted before lower case|


#### Script/Block Reordering

Codes: kr, colReorder

Collation reorder codes

|Codes|English Name|Description|
|-|-|-|
|currency|Currency reordering code, see LDML Part 5: Collation|Currency reordering code, see LDML Part 5: Collation|
|digit|Digit (number) reordering code, see LDML Part 5: Collation|Digit (number) reordering code, see LDML Part 5: Collation|
|punct|Punctuation reordering code, see LDML Part 5: Collation|Punctuation reordering code, see LDML Part 5: Collation|
|space|Whitespace reordering code, see LDML Part 5: Collation|Whitespace reordering code, see LDML Part 5: Collation|
|symbol|Symbol reordering code (other than currency), see LDML Part 5: Collation|Symbol reordering code (other than currency), see LDML Part 5: Collation|


#### Sorting Strength

Codes: ks, colStrength

Collation parameter key for collation strength

|Codes|English Name|Description|
|-|-|-|
|identic, identical|The identical level|The identical level|
|level1, primary|The primary level|The primary level|
|level2, secondary|The secondary level|The secondary level|
|level3, tertiary|The tertiary level|The tertiary level|
|level4, quaternary, quarternary|The quaternary level|The quaternary level|


#### Highest Ignored

Codes: kv

Collation parameter key for maxVariable, the last reordering group to be affected by ka-shifted

|Codes|English Name|Description|
|-|-|-|
|currency|Spaces, punctuation, all symbolsSpaces, punctuation ka-shifted|Spaces, punctuation and all symbols are affected by ka-shifted|
|punct|Spaces, punctuation ka-shifted|Spaces and punctuation are affected by ka-shifted (CLDR default)|
|space|Spaces, punctuation ka-shifted|Only spaces are affected by ka-shifted|
|symbol|Spaces, punctuation, non-currency symbols ka-shifted|Spaces, punctuation and symbols except for currency symbols are affected by ka-shifted (UCA default)|

### Currencies

#### Currency Format

Codes: cf

Currency format

|Codes|English Name|Description|
|-|-|-|
|account|Accounting|Accounting currency format|
|standard|Standard|Standard currency format|

### Measurement

#### Measurement System

Codes: ms, measure

Measurement System

|Codes|English Name|Description|
|-|-|-|
|metric|Metric|Metric System|
|uksystem, imperial|UK|UK System of measurement: feet, pints, etc.; pints are 20oz|
|ussystem|US|US System of measurement: feet, pints, etc.; pints are 16oz|

### Numbers

#### Numbers

Codes: nu, numbers

Numbering system type

|Codes|English Name|Description|
|-|-|-|
|arabext|Extended Arabic-Indic|Extended Arabic-Indic digits|
|armnlow|Armenian lowercase|Armenian lower case numerals — algorithmic|
|finance|Financial|Financial numerals — may be algorithmic|
|fullwide|Full-width|Full width digits|
|greklow|Greek lowercase|Greek lower case numerals — algorithmic|
|hanidays|Han-character day-of-month numbering for lunar/other traditional calendars|Han-character day-of-month numbering for lunar/other traditional calendars|
|hanidec|Positional decimal system using Chinese number ideographs as digits|Positional decimal system using Chinese number ideographs as digits|
|hansfin|Simplified Chinese financial|Simplified Chinese financial numerals — algorithmic|
|hantfin|Traditional Chinese financial|Traditional Chinese financial numerals — algorithmic|
|jpanfin|Japanese financial|Japanese financial numerals — algorithmic|
|jpanyear|Japanese first-year Gannen numbering|Japanese first-year Gannen numbering for Japanese calendar|
|lanatham|Tai Tham Tham (ecclesiastical)|Tai Tham Tham (ecclesiastical) digits|
|mathbold|Mathematical bold|Mathematical bold digits|
|mathdbl|Mathematical double-struck|Mathematical double-struck digits|
|mathmono|Mathematical monospace|Mathematical monospace digits|
|mathsanb|Mathematical sans-serif bold|Mathematical sans-serif bold digits|
|mathsans|Mathematical sans-serif|Mathematical sans-serif digits|
|mymrepka|Myanmar Eastern Pwo Karen|Myanmar Eastern Pwo Karen digits|
|mymrpao|Myanmar Pao|Myanmar Pao digits|
|mymrshan|Myanmar Shan|Myanmar Shan digits|
|mymrtlng|Myanmar Tai Laing|Myanmar Tai Laing digits|
|native|Native digits|Native digits|
|outlined|Outlined|Legacy computing outlined digits|
|roman|Roman uppercase|Roman upper case numerals — algorithmic|
|romanlow|Roman lowercase|Roman lowercase numerals — algorithmic|
|segment|Segmented|Legacy computing segmented digits|
|tamldec|Modern Tamil|Modern Tamil decimal digits|
|traditio, traditional|null|Traditional numerals — may be algorithmic|
|SCRIPT_CODE-N|_translation by other CLDR data_|Any script code with numbering system|

### Segmentation

#### Line Breaks within Words

Codes: lw

Line break key for CSS lev 3 word-break options

|Codes|English Name|Description|
|-|-|-|
|breakall|Break all|CSS lev 3 word-break=break-all, allow midword breaks unless forbidden by lb setting|
|keepall|Keep all|CSS lev 3 word-break=keep-all, prohibit midword breaks except for dictionary breaks|
|normal|Normal|CSS lev 3 word-break=normal, normal script/language behavior for midword breaks|
|phrase|Keep in phrases|Prioritize keeping natural phrases (of multiple words) together when breaking, used in short text like title and headline|


#### CJK Line Break

Codes: lb

Line break type

|Codes|English Name|Description|
|-|-|-|
|loose|Loose|CSS lev 3 line-break=loose|
|normal|Normal|CSS level 3 line-break=normal, e.g. treat CJ as ID, break before hyphens for ja,zh|
|strict|Strict|CSS level 3 line-break=strict, e.g. treat CJ as NS|


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

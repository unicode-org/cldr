---
title: Locale Option Names (Key)
---

# Locale Option Names (Key)

Locales can have special variants, to indicate the use of particular calendars, or other features. They be used to select among different options in menus, and also display which options are in effect for the user. 

## Locale Option Names

Here are the names of Options to be translated.

| Option | Meaning   |
|---|---|
| Calendar | Calendar system (the European calendar is called "Gregorian"; others are the Chinese Lunar Calendar, and so on.) |
| Collation | How text is sorted (where a language has different possible ways to sort). |
| Currency | The default currency. (The value is any currency value, such as USD). |
| Numbers | The numbering system in use, such as European (0,1,2), Arabic (٠, ١, ٢ ), Devanagari ( ०,  १,  २ ). <br /> - Usually these are just derived from the name of the script.<br /> - There are some special forms, such as " Simplified Chinese Financial Numerals " or " Full Width Digits ". |
| Private Use | Used for Private-Use options (x). |

## Locale Option Value Names

The following are some examples of Option+Value combinations that need translation. Where the Value for the Option is not from a small set (Currency and Private Use), then the Locale Option Pattern is used (see [Language/Locale Name Patterns](https://cldr.unicode.org/translation/displaynames/languagelocale-name-patterns).

| Option | Value | English Name | Meaning |
|---|---|---|---|
| Calendar | buddhist | Buddhist Calendar | Buddhist Calendar  |
| Collation | dict | Dictionary Sort Order | The ordering used in dictionaries, where that is distinct from other forms (such as in Sinhala) |
| Collation | phonebk | Phonebook Sort Order | The ordering used in phonebooks, where that is distinct from other forms (such as in German) |
| Collation  | trad | Traditional Sort Order | An ordering used traditionally (in contrast to later conventions) |
| Collation | reformed   | Reformed Sort Order  | Reformed collation (as opposed to earlier traditions — such as in Swedish) |
| Collation | phonetic   | Phonetic Sort Order  | A phonetic ordering based on pronunciation. It may interleave different scripts, if multiple scripts are in common use. |
| Collation  | direct | Direct Sort Order | The code-point order (in the Unicode charts) |
| Collation | unihan   | Radical-Stroke Sort Order | Unihan radical-stroke ordering for CJK characters (those used in Chinese, Japanese, and Korean. |
| Collation  | pinyin | Pinyin Sort Order | An ordering based on Pinyin (for Chinese) |
| Collation  | stroke | Stroke Sort Order | An ordering based on stroke-count (for Chinese) |
| Collation  | gb2312han | Simplified Sort Order - GB2312 | An ordering based on the character encoding GB2312 (for Chinese) |
| Collation  | big5han | Traditional Sort Order - Big5 | An ordering based on the character encoding Big5 (for Chinese)  |
| Numbers | armn | Armenian Numerals | The numbering system that uses Armenian digits. |

For transform names (BGN, Numeric, Tone, UNGEGN, x-Accents, x-Fullwidth, x-Halfwidth, x-Jamo, x-Pinyin, x-Publishing), see [Transforms](https://cldr.unicode.org/translation/transforms).


![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
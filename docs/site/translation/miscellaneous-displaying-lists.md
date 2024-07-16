---
title: 'Miscellaneous: Displaying Lists'
---

# Miscellaneous: Displaying Lists

List patterns can be used to format variable-length lists of things in a locale-sensitive manner, such as "Monday, Tuesday, Friday, and Saturday" (in English) versus "lundi, mardi, vendredi et samedi" (in French). The following patterns need to be translated:

| Symbol | CLDR Pattern | English Pattern | English Example | Meaning |
|---|---|---|---|---|
| 2 | {0} and {1} | [ ITEM1 ]  and  [ ITEM2 ] | Saturday **and** Sunday | List of 2 items |
| start | {0}, {1} | [ FIRST_ITEM ] , [ REMAINING_ITEMS ] | Saturday **,** Sunday, Tuesday, and Wednesday   | The start of a list of 3 or more items |
| middle | {0}, {1} | [ FIRST_ITEMS ] , [ LAST_ITEMS ] | Saturday, Sunday **,** Tuesday, and Wednesday | The middle of a list of 4 or more items. |
| end | {0}, and {1} | [ START_ITEMS ] , and [ LAST_ITEM ] | Saturday, Sunday, Tuesday **, and**  Wednesday | The end of a list of 3 or more items. |

There may be some variance within the language. For example, there are two different patterns for the *end* in English, with or without the comma. Use the convention that is most customary among educated users of your language; if two different conventions are both well established, use the form that is likely to cause fewer ambiguities. In this case CLDR follows the Chicago Manual of Style for English. See also [Serial Comma](http://en.wikipedia.org/wiki/Serial_comma).

**Warning:** *All* of the patterns can be used to format any kind of noun phrases: people, places, and things. Examples could be:

- John, Mary, and Fred
- Paris, Berlin, and Moscow
- italian cuisine, sports, and poker

Pick the most neutral formulation you can, so that it works with as many kinds of noun phrases as possible.

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)

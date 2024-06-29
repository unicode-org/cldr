---
title: Date & Time terminology
---

# Date & Time terminology

This topic is **in-progress** and and **not finalized** yet for use.

Following are terminology and definitions that are used for Date and Time structure and data in CLDR. The terminology used in CLDR have dependency on LDML Spec #35 and names of methods and objects in ICU. 

| Terminology |  Definition |  Examples |
|---|---|---|
|  Symbols or Pattern characters or placeholder symbol |   The ASCII-range letters used in patterns,    character in a placeholder field |  G, y, M, d |
| pattern fields or  placeholder fields |  A sequence of one or more of the above used in a pattern  sequence of characters that is replaced in a pattern at runtime, like MMM, {1}, "###" (in numbers) | MMM  E |
| Spec: Elements ICU: textual forms as symbols | The specific localized values that replace a pattern fields depending on the date |  Monday or January, or 7.  |
|   **calendar fields** |  The abstract calendar type that is represented by the letters |  era or year. |
|  calendar field “ **names**" |  The localized names for each type of calendar field |  “era” or “year”: |

 
![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
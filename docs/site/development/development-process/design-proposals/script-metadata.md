---
title: script-metadata
---

# script-metadata

[http://unicode.org/cldr/trac/ticket/3871](http://unicode.org/cldr/trac/ticket/3871)

Here is a proposed structure for supplemental data

\<script>

&emsp;\<scriptData type="Latn" lines="top-to-bottom" characters="left-to-right" spaces="true" shaping="minimal" usage="recommended" originRegion="150" sample="A">

lines/characters =

- as in the locale orientation element.

spaces = 

- true if the script normally uses spaces to separate words

shaping = (in normal usage)

- none if no shaping is normally needed
- minimal if only minor shaping is normally needed, such as accent placement
- major if glyphs normally need to rearrange, or change shape depending on context

usage = value in UAX31

originRegion = continent or subcontinent where script originated. Following the Unicode book / charts (see the spreadsheet).

sample = character with distinctive glyph that can be used to represent the script in icons (eg missing glyph)

The orientation field in a locale would only be used for overriding.


![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
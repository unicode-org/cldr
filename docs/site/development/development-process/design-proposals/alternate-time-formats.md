---
title: Alternate Time Formats
---

# Alternate Time Formats

This design proposal is intended to solve the problem that sometimes the desired time separator for a pattern may vary depending on the numbering system used. Rather than adding an additional type of number symbol just for the time separator, a more generalized solution would be to expand the syntax for numbering system overrides in patterns, in order to allow a simple replacement of a literal in the pattern based on the numbering system. The following description of the numbering system override is from the current TR35:

\<!ATTLIST pattern numbers CDATA #IMPLIED >

The numbers attribute is used to indicate that numeric quantities in the pattern are to be rendered using a numbering system other than then default numbering system defined for the given locale. The attribute can be in one of two forms. If the alternate numbering system is intended to apply to ALL numeric quantities in the pattern, then simply use the numbering system ID as found in Section C.13 [Numbering Systems](http://www.unicode.org/reports/tr35/#Numbering_Systems). To apply the alternate numbering system only to a single field, the syntax "\<letter>=\<numberingSystem>" can be used one or more times, separated by semicolons.
 
Examples:

\<pattern numbers="hebr">dd/mm/yyyy\</pattern>

\<!-- Use Hebrew numerals to represent numbers in the Hebrew calendar, where "latn" numbering system is the default -->

\<pattern numbers="y=hebr">dd/mm/yyyy\</pattern>

\<!-- Same as above, except that ONLY the year value would be rendered in Hebrew -->

\<pattern numbers="d=thai;m=hans;y=deva">dd/mm/yyyy\</pattern>

\<!-- Illustrates use of multiple numbering systems for a single pattern. -->

**Proposed Extension**

In addition to the syntax, allow symbol or string replacements of the form "\<string>=\<numberingSystem>=\<replacementString>"

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
---
title: XMB
---

# XMB

### Introduction

Adds tools to CLDR to convert to and from the [XMB message format](http://unicode.org/repos/cldr-tmp/trunk/dropbox/xmb/xmb.dtd). The XMB format is basically a key-value pair list, with no deeper structure. It does have a mechanism for named placeholders, with descriptions and examples. The messages for any given other language must correspond 1:1 with those of English.

The goal is to allow for bulk translation of CLDR files via existing translation tooling.

Examples:

**ENGLISH**

\<msg id='615EB568A2478EAF' desc='The name of the country or region with BCP47 code = UZ. Before translating, please read [cldr.org/translation](http://cldr.org/translation).'

\>**Uzbekistan**\</msg>

\<!-- English: MMMM d, y -->

\<msg id='5D6EA98708B9B43B' desc='Long date format. Before translating, please read [cldr.org/translation](http://cldr.org/translation).'

\>**\<ph name='MONTH\_LONG'>\<ex>September\</ex>MMMM\</ph> \<ph name='DAY\_1\_DIGIT'>\<ex>9\</ex>d\</ph>, \<ph name='YEAR'>\<ex>2010\</ex>y\</ph>**\</msg>

**FRENCH**

\<!-- English: Uzbekistan -->

\<msg id='615EB568A2478EAF'

\>**Ouzbékistan**\</msg>

\<!-- English: MMMM d, y -->

\<msg id='5D6EA98708B9B43B'

\>**\<ph name='DAY\_1\_DIGIT'>\<ex>9\</ex>d\</ph> \<ph name='MONTH\_LONG'>\<ex>September\</ex>MMMM\</ph> \<ph name='YEAR'>\<ex>2010\</ex>y\</ph>**\</msg>

The id is common across the different languages. The description, the placeholder names and the placeholder examples (\<ex>) are visible to the translator, as is the text between placeholders, of course. The translator can change the order of the placeholders, but they cannot be removed (or added).

The main tool for converting CLDR to this format is at [GenerateXMB.java](http://unicode.org/cldr/trac/browser/trunk/tools/java/org/unicode/cldr/tool/GenerateXMB.java). It reads the en.xml file, and puts together a EnglishInfo object that has a mapping from paths to descriptions and placeholders. It also generates the English XMB file for translation. Next, each of the other CLDR locale files are read and their data is used to populate a separate XTB file for translation memory.

Files:

|   |   |
|---|---|
| [xmb-en.xml](http://unicode.org/repos/cldr-tmp/trunk/dropbox/xmb/xmb-en.xml) | The base English file, for translation into other languages |
| [xtb-fr.xml](http://unicode.org/repos/cldr-tmp/trunk/dropbox/xmb/xtb-fr.xml) | Sample file (fr) for translation memory. Missing entries would be translated. |

Others are at [xmb/](http://www.unicode.org/repos/cldr-tmp/trunk/dropbox/xmb/).

The documentation files are at http://cldr.org/translation.

### Log Files

The tool generates log files during processing, targeted at development and debugging.

Examples:

|  |  |  |
|---|---|---|
| log/ | [en-missingDescriptions.txt](http://www.unicode.org/repos/cldr-tmp/trunk/dropbox/xmb/log/en-missingDescriptions.txt) | The paths that don't yet have descriptions in them, which need to be added to  xmbHandling.txt .  |
| log/ | [en-paths.txt](http://www.unicode.org/repos/cldr-tmp/trunk/dropbox/xmb/log/en-paths.txt) | The paths used for the base English file. |
| filtered/ | [xmb-en.xml](http://www.unicode.org/repos/cldr-tmp/trunk/dropbox/xmb/filtered/xmb-en.xml) | A filtered xmb-en.xml file that contains exactly one item per "starred" path (where a starred path is one with attribute values removed). Useful for reviewing descriptions. |
| filtered/ | [xtb-fr.xml](http://www.unicode.org/repos/cldr-tmp/trunk/dropbox/xmb/filtered/xtb-fr.xml) | A filtered sample (fr) xml file. |
| skipped/ | [xmb-en.txt](http://www.unicode.org/repos/cldr-tmp/trunk/dropbox/xmb/skipped/xmb-en.txt) | The paths that are skipped out of the base English file. |
| skipped/ | [xtb-fr.txt](http://www.unicode.org/repos/cldr-tmp/trunk/dropbox/xmb/skipped/xtb-fr.txt) | The paths that are skipped out of the sample (fr) file. |

### Placeholders

Replaces the placeholders ("{0}", "MMM", etc.) in patterns by variable names with examples. This is data-driven, using the file at [xmbPlaceholders.txt](http://unicode.org/cldr/trac/browser/trunk/tools/java/org/unicode/cldr/tool/xmbPlaceholders.txt).

Format:

&emsp;path\_regex ; variable=name example

The name cannot contain spaces.

Example:

&emsp;^//ldml/dates/.\*(pattern|available|intervalFormatItem) ; EEEE=**DAY\_OF\_WEEK\_LONG** Tuesday

### Filtering and descriptions

Data driven, using the file [xmbHandling.txt](http://unicode.org/cldr/trac/browser/trunk/tools/java/org/unicode/cldr/tool/xmbHandling.txt).

Format:

&emsp;path\_regex ; description

&emsp;path\_regex ; SKIP

&emsp;path\_regex ; ROOT type\_value; description

1. If the value is SKIP, then the path is skipped.
2. The description can have {0}-style variables in it. If so, then the (...) values in the path\_regex are substituted for them.
3. If the value starts with ROOT, then the path is skipped if the type\_value is not in ROOT, where the type\_value is from the first capture group. This is used to make sure that the type\_value is in the major coverage requirements for: language, script, territory, currency, timezone, and metazone. The description can have placeholders, as in case 21.

Example:

^//ldml/dates/timeZoneNames/metazone\\[@type=".\*"]/commonlyUsed ; SKIP

^//ldml/dates/timeZoneNames/zone\\[@type=".\*"]/exemplarCity ; The name of a city in: {0}. See cldr.org/xxxx.

### Plurals

Plurals are represented with ICU Syntax, such as:

<!--
  {% raw %}

  Disable liquid parsing on this codeblock to prevent errors reading '{{'
  See: https://talk.jekyllrb.com/t/code-block-is-improperly-handled-and-generates-liquid-syntax-error/7599/2
-->

```xml
<msg id='4AC13E2DA211C113' desc='[ICU Syntax] The pattern used to compose plural for week, including abbreviated forms. These forms are special! Before translating, see cldr.org/translation/plurals.'>

{LENGTH, select,

abbreviated {{NUMBER_OF_WEEKS, plural,

=0 {0 wks}

=1 {1 wk}

zero {# wks}

one {# wk}

two {# wks}

few {# wks}

many {# wks}

other {# wks}}}

other {{NUMBER_OF_WEEKS, plural,

=0 {0 weeks}

=1 {1 week}

zero {# weeks}

one {# week}

two {# weeks}

few {# weeks}

many {# weeks}

other {# weeks}}}}</msg>
```

<!-- {% endraw %} -->

### TODO

- Add missing descriptions
- Add missing site pages with detailed descriptions, and links from the descriptions
- Add a limited number of currency plurals (major currencies only).
- Add a limited number of extra language codes.
- Rewire items that are in undistinguished attributes
- Test each xml file for validity
- Do the conversion from xtb into cldr format to make sure we roundtrip.
- Figure out how to do the differences between HH and hh, etc.
    - Current thoughts: don't let the translator choose, but make it part of the xtb-cldr processing.

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)

---
title: BCP47 Syntax Mapping
---

# BCP47 Syntax Mapping

In the current LDML specification, a Unicode Locale Identifier consists from is composed of a Unicode Language Identifier plus optional locale extensions. Unicode Language Identifier is fully compatible with BCP47 language tag, but the syntax of locale extensions ("@" key "=" type (";" key "=" type)\* ) are not. The LDML is trying to define systematical mapping, but the current definition may truncate (and or remove "-" in some type values) key or type value to 8 characters because of the BCP47 language subtag's syntax restriction. The current definition utilizes BCP47 private use features, but we want to make locale extensions formal (writing a new RFC to reserve a singleton letter for the usage), so we can avoid any conflicts with other private use values and also allow software developers to write a parser for Unicode locale extensions confidently.

BCP 47 is undergoing a revision which should be done soon:

- [Current version (4646)](http://tools.ietf.org/html/rfc4646)
- [Latest draft of next version](http://inter-locale.com/ID/draft-ietf-ltru-4646bis-21.html)

Once we define formal representation of Unicode locale extensions in BCP47 syntax, we actually no longer have any good reasons to use @key1=type1;key2=type2... syntax for Unicode Locale Identifier other than backward compatibility reasons. This document proposes that we retire the proprietary syntax and fully migrate to the new syntax fully supported by BCP47 language tag.

There are several options for representing keyword key/type pairs in BCP47 syntax. Examples in following proposal assume a letter "u" is reserved for the Unicode locale extensions; however we could go for any of the [possible extensions](http://inter-locale.com/ID/draft-ietf-ltru-4646bis-21.html#syntax): [0-9 a-w y z].

The table below shows the locale extension keys/values currently defined by the LDML specification.

## Key/Type Definitions

| key | type | Description |
|---|---|---|
| collation | standard | The default ordering for each language. For root it is [ [UCA](http://www.unicode.org/reports/tr35/#UCA) ] order; for each other locale it is the same as UCA ordering except for appropriate modifications to certain characters for that language. The following are additional choices for certain locales; they only have effect in those locales. |
|  | phonebook | For a phonebook-style ordering (used in German). |
|  | pinyin | Pinyin ordering for Latin and for CJK characters; that is, an ordering for CJK characters based on a character-by-character transliteration into a pinyin. (used in Chinese) |
|  | traditional | For a traditional-style sort (as in Spanish) |
|  | stroke | Pinyin ordering for Latin, stroke order for CJK characters (used in Chinese) |
|  | direct | Hindi variant |
|  | posix | A "C"-based locale.  (no longer in CLDR data) |
|  | big5han | Pinyin ordering for Latin, big5 charset ordering for CJK characters. (used in Chinese) |
|  | gb2312han | Pinyin ordering for Latin, gb2312han charset ordering for CJK characters. (used in Chinese) |
|  | unihan | Pinyin ordering for Latin, Unihan radical-stroke ordering for CJK characters. (used in Chinese) |
| calendar<br /><br /> (*For information on the calendar algorithms associated with the data used with the above types, see [ [Calendars](https://www.unicode.org/reports/tr35/#Calendars) ].*) | gregorian | (default) |
|  | islamic<br /><br /> *alias: arabic* | Astronomical Arabic |
|  | chinese | Traditional Chinese calendar |
|  | islamic-civil<br /><br /> *alias: civil-arabic* | Civil (algorithmic) Arabic calendar |
|  | hebrew | Traditional Hebrew Calendar |
|  | japanese | Imperial Calendar (same as Gregorian except for the year, with one era for each Emperor) |
|  | buddhist<br /><br /> *alias: thai-buddhist* | Thai Buddhist Calendar (same as Gregorian except for the year) |
|  | persian | Persian Calendar |
|  | coptic | Coptic Calendar |
|  | ethiopic | Ethiopic Calendar |
| collation parameters:<br /><br /> &nbsp;&nbsp;colStrength<br /> &nbsp;&nbsp;colAlternate<br /> &nbsp;&nbsp;colBackwards<br /> &nbsp;&nbsp;colNormalization<br /> &nbsp;&nbsp;colCaseLevel<br /> &nbsp;&nbsp;colCaseFirst,<br /> &nbsp;&nbsp;colHiraganaQuaternary<br /> &nbsp;&nbsp;colNumeric<br /> &nbsp;&nbsp;variableTop | *Associated values as defined in: 5.14.1 &lt;[collation](http://www.unicode.org/reports/tr35/#Collation_Element)&gt;* | Semantics as defined in: 5.14.1 &lt;[collation](http://www.unicode.org/reports/tr35/#Collation_Element)&gt; |
| currency<br /><br /> (also known as a Unicode currency code ) | *ISO 4217 code,*<br /><br /> *plus others in common use* | Currency value identified by ISO 4217 code, plus others in common use. Also uses XXX as *Unknown or Invalid Currency* .<br /><br /> See [Appendix K: Valid Attribute Values](http://www.unicode.org/reports/tr35/#Valid_Attribute_Values) and also [ [Data Formats](http://www.unicode.org/reports/tr35/#DataFormats) ] |
| time zone<br /><br /> (also known as a Unicode time zone code ) | *TZID, plus the value:*<br /><br /> *Etc/Unknown* | Identification for time zone according to the TZ Database, plus the value Etc/Unknown .<br /><br /> Unicode LDML supports all of the time zone IDs by mapping all equivalent time zone IDs to a canonical ID for translation. This canonical time zone ID is not the same as the zone.tab time zone ID found in [ [Olson](http://www.unicode.org/reports/tr35/#Olson) ].<br /><br /> For more information, see [Section 5.9.2 Time Zone Names](http://www.unicode.org/reports/tr35/#Timezone_Names) , [Appendix F: Date Format Patterns](http://www.unicode.org/reports/tr35/#Date_Format_Patterns) , and [Appendix J: Time Zone Display Names](http://www.unicode.org/reports/tr35/#Time_Zone_Fallback) . |

### Collation Parameters

| Attribute | Options | Basic Example  | XML Example | Description |
|---|---|---|---|---|
| strength | primary (1)<br /> secondary (2)<br /> tertiary (3)<br /> quaternary (4)<br /> identical (5) | [strength 1] | strength = " primary " | Sets the default strength for comparison, as described in the UCA. |
| alternate | *non-ignorable shifted* | [alternate non-ignorable] | alternate = " non-ignorable " | Sets alternate handling for variable weights, as described in UCA |
| backwards | on<br /> *off* | [backwards 2]  | backwards = " on " | Sets the comparison for the second level to be backwards ("French"), as described in UCA |
| normalization | on<br /> off | [normalization on] | normalization = " off " | If *on* , then the normal UCA algorithm is used. If *off* , then all strings that are in [ [FCD](http://www.unicode.org/reports/tr35/#FCD) ] will sort correctly, but others will not necessarily sort correctly. So should only be set *off* if the the strings to be compared are in FCD. |
| caseLevel | on<br /> off | [caseLevel on] | caseLevel = " off " | If set to on, a level consisting only of case characteristics will be inserted in front of tertiary level. To ignore accents but take cases into account, set strength to primary and case level to on . |
| caseFirst | upper<br /> lower<br /> off | [caseFirst off] | caseFirst = " off " | If set to *upper* , causes upper case to sort before lower case. If set to *lower* , lower case will sort before upper case. Useful for locales that have already supported ordering but require different order of cases. Affects case and tertiary levels. |
| hiraganaQuaternary | on<br /> off | [hiraganaQ on] | hiragana­Quaternary = " on " | Controls special treatment of Hiragana code points on quaternary level. If turned *on* , Hiragana codepoints will get lower values than all the other non-variable code points. The strength must be greater or equal than quaternary if you want this attribute to take effect. |
| numeric | on<br /> off | [numeric on] | numeric = " on " | If set to *on* , any sequence of Decimal Digits (General_Category = Nd in the [ [UCD](http://www.unicode.org/reports/tr35/#UCD) ]) is sorted at a primary level with its numeric value. For example, "A-21" &lt; "A-123". |
| variableTop | uXXuYYYY | &amp; \u00XX\uYYYY &lt; [variable top] | variableTop = "uXXuYYYY" | The parameter value is an encoded Unicode string, with code points in hex, leading zeros removed, and 'u' inserted between successive elements.<br /><br /> Sets the default value for the variable top. All the code points with primary strengths less than variable top will be considered variable, and thus affected by the alternate handling. |
| match-boundaries: | none whole-character whole-word | n/a | match-boundaries = "whole-word" | The meaning is according to the descriptions in UTS #10 [Searching](https://unicode.org/reports/tr10/#Searching) . |
| match-style | minimal medial maximal | n/a | match-style = "medial" | The meaning is according to the descriptions in UTS #10 [Searching](https://unicode.org/reports/tr10/#Searching) . |

## 1. Proposed BCP47 subtag syntax

This document propose the syntax described by the BNF below.

locale-extensions = locale-singleton "-" extension \*("-" extension)

extension = key "-" type

locale-singleton = "u"

key = 2alphanum

type = 3\*8alphanum

alphanum = (ALPHA / DIGIT)

Example:

en-US-u-ca-islamicc-co-phonebk

this corresponds to the former syntax

en-US@calendar=islamic-civil;collation=phonebook

| Current |  Proposed |
|---|---|
|  collation   |  co |
|  calendar |  ca |
|  currency |  cu |
|  numbers |  nu |
|  time zone |  tz |
|  colStrength |  ks |
|  colAlternate |  ka |
|  colBackwards |  kb |
|  colNormalization |  kk |
|  colCaseLevel |  kc |
|  colCaseFirst |  kf |
|  colHiraganaQuaternary |  kh |
|  colNumeric |  kn |
|  variableTop |  kv |

### 2. Keys

Key names and only key names are always of length=2, and types (values) are always greater than 2. This proposal defines new canonical key names below.

The motivation is reduction of string size, and making sure that keys and values don't overlap syntactically.

### 3. Types

3.1 Collation

3.1.1 Collation (co) types

|   Current |   Proposed |
|---|---|
|  big5han |  big5han |
|  digits-after |   **digitaft** |
|  direct |  direct |
|  gb2312han |   **gb2312** |
|  phonebook |   **phonebk** |
|  pinyin |  pinyin |
|  reformed |  reformed |
|  standard |  standard |
|  stroke |  stroke |
|  traditional |   **trad** |

3.1.2 Collation Strength (ks) types

| Current |   Proposed |
|---|---|
|  primary |  level1 |
|  secondary |  level2 |
|  tertiary |  level3 |
|  quarternary |  level4 |
|  identical |  identic |

3.1.3 Collation Alternate (ka) types

| Current |   Proposed |
|---|---|
|  non-ignorable |  **noignore** |
|  secondary |  level2 |
|  shifted |  shifted |

3.1.4 Collation Backwards (kb) / Normalization (kk) / Case Level (kc) / Hiragana Quaternary (kh) / Numeric (kn) types

| Current |   Proposed |
|---|---|
|  yes |  **true** |
|  no |  **false** |

3.1.5 Collation Case First (kf) types

|  Current |   Proposed |
|---|---|
|  upper |   upper |
|  lower |  lower |
|  no |   **false** |


3.1.6 Collation Variable Top (kv) type

The variable top parameter is specified by a code point in the format *uXXuYYYY*. No changes are required.

3.2 Calendar (ca)

| Current |   Proposed |
|---|---|
|  buddhist |  buddhist |
|  coptic |   coptic |
|  ethiopic |  ethiopic |
|  ethiopic-amete-alem |   **ethiopaa** |
|  chinese |   chinese |
|  gregorian |   **gregory** |
|  hebrew |  hebrew |
|  indian |  indian |
|  islamic |  islamic |
|  islamic-civil |   **islamicc** |
|  japanese |  japanese |
|  persian |  presian |
|  roc |  roc |

3.3 Currency (cu) types

ISO4217 code (3-letter alpha) is used for currency. No changes required.

3.4 Number System (nu) types

The current CVS snapshot implementation uses CSS3 names. This proposal changes all of type names to script code with one exception (arabext).

| Current (CVS snapshot) |   Proposed |
|---|---|
|  arabic-indic |  arab |
|  bengali |  beng |
|  cambodian |  khmr |
|  decimal |  latn |
|  devanagari |  deva |
|  gujarati |  gujr |
|  gurmukhi |  guru |
|  hebrew |  hebr |
|  kannada |  knda |
|  lao |  laoo |
|  malayalam |  mlym |
|  mongolian |  mong |
|  myanmar |  mymr |
|  oriya |  orya |
|  persian |  arabext |
|  telugu |  telu |
|  thai |  thai |

3.5 Time Zone (tz) types

CLDR uses Olson tzids. These IDs are usually made from \<continent>+"/"+\<exemplar city> and relatively long. To satisfy the syntax requirement discussed in this document, we need to map these IDs to relatively short IDs uniquely. The UN LOCODE is designed to assign unique location code and it satisfies most of the requirement. A LOCODE consists from 2 letter ISO country code and 3 letter location code. This proproposal suggest that a 5 letter LOCODE is used as a short time zone ID if examplar city has a exact match in LOCODE repertoire. Some Olson tzids do not have direct mapping in LOCODE. In this case, we assign our own codes to them, but using 3-4/6-8 letter code to distinguish them from LOCODE. For Olson tzid Etc/GMT\*, this proposal suggest "UTC" + ["E" | "W"] + nn (hour offset), for example, UTCE01 means 1 hour east from UTC (Etc/GMT-1). The proposed short ID list is attached in this [document](https://drive.google.com/file/d/1O9B_hO6uD4m7dtb-hU9euBkgP8nQxJ9X/view?usp=sharing).

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
## <a name="Root_Collation" id="Root_Collation" href="#Root_Collation">Root Collation</a>

* The CLDR root collation order is based on the [Default Unicode Collation Element Table (DUCET)](https://www.unicode.org/reports/tr10/#Default_Unicode_Collation_Element_Table) defined in _UTS #10: Unicode Collation Algorithm_ [[UCA](https://www.unicode.org/reports/tr41/#UTS10)]. It is used by all other locales by default, or as the base for their tailorings. (For a chart view of the UCA, see Collation Chart [[UCAChart](tr35.md#UCAChart)].)


Starting with CLDR 1.9, CLDR uses modified tables for the root collation order. The root locale ordering is tailored in the following ways:

### <a name="grouping_classes_of_characters" id="grouping_classes_of_characters" href="#grouping_classes_of_characters">Grouping classes of characters</a>

CLDR groups the characters that sort below letters like this: Whitespace, punctuation, general symbols, currency symbols, and numbers. Letters are grouped by script.

* Users can parametrically reorder the groups. (The CLDR data adds special values to mark their boundaries.) For example, users can reorder numbers after all scripts, or reorder Greek before Latin. See [Collation Reordering](#Script_Reordering) for details.


* Starting with CLDR 46 and Unicode 16.0, the _order_ of characters in the CLDR root collation is the same as in the UCA DUCET (except for the CLDR addition of ten Tibetan contractions, see below). In earlier versions, the order of some below-letter characters differed, and CLDR had also tailored some currency symbols. Both sort orders have been changed to now sort the same.


### <a name="non_variable_symbols" id="non_variable_symbols" href="#non_variable_symbols">Non-variable symbols</a>

* There are multiple [Variable-Weighting](https://www.unicode.org/reports/tr10/#Variable_Weighting) options in the UCA for symbols and punctuation, including _non-ignorable_ and _shifted_. With the _shifted_ (`-u-ka-shifted`) option, almost all symbols and punctuation are ignored—except at a fourth level. The CLDR root locale ordering is modified so that symbols are not affected by the _shifted_ option. That is, by default, symbols are not “variable” in CLDR. So _shifted_ only causes whitespace and punctuation to be ignored, but not symbols (like ♥). The DUCET behavior can be approximated with a locale ID using the "kv" keyword, to set the Variable section to include all of the symbols below it (`-u-kv-symbol`), or be set parametrically where implementations allow access.


* Note that the CLDR “symbols” group includes at its end certain “extender” characters which are non-variable in the DUCET; one would also need to tailor the “extenders” into the “currency” group for achieving the exact same _shifted_ behavior.


See also:

* _[Setting Options](#Setting_Options)_
* [https://www.unicode.org/charts/collation/](https://www.unicode.org/charts/collation/)

### <a name="tibetan_contractions" id="tibetan_contractions" href="#tibetan_contractions">Additional contractions for Tibetan</a>

* Ten contractions are added for Tibetan: Two to fulfill [well-formedness condition 5](https://www.unicode.org/reports/tr10/#WF5), and eight more to preserve the default order for Tibetan. For details see _UTS #10, Section 3.8.2, [Well-Formedness of the DUCET](https://www.unicode.org/reports/tr10/#Well_Formed_DUCET)_.


### <a name="tailored_noncharacter_weights" id="tailored_noncharacter_weights" href="#tailored_noncharacter_weights">Tailored noncharacter weights</a>

U+FFFE and U+FFFF have special tailorings:

* **U+FFFF:** This code point is tailored to have a primary weight higher than all other characters. This allows the reliable specification of a range, such as “Sch” ≤ X ≤ “Sch\\uFFFF”, to include all strings starting with "sch" or equivalent.
* **U+FFFE:** This code point produces a CE with minimal, unique weights on primary and identical levels. For details see the _[CLDR Collation Algorithm](#Algorithm_FFFE)_ above.

* UCA (beginning with version 6.3) also maps **U+FFFD** to a special collation element with a very high primary weight, so that it is reliably non-[variable](https://www.unicode.org/reports/tr10/#Variable_Weighting), for use with [ill-formed code unit sequences](https://www.unicode.org/reports/tr10/#Handling_Illformed).


* In CLDR, so as to maintain the special collation elements, **U+FFFD..U+FFFF** are not further tailorable, and nothing can tailor to them. That is, neither can occur in a collation rule. For example, the following rules are illegal:


```text
&\uFFFF < x
```

```text
&x <\uFFFF
```

> 👉 **Note**: Java uses an early version of this collation syntax, but has not been updated recently. It does not support any of the syntax marked with [...], and its default table is not the DUCET nor the CLDR root collation.

### <a name="Root_Data_Files" id="Root_Data_Files" href="#Root_Data_Files">Root Collation Data Files</a>

The CLDR root collation data files are in the CLDR repository and release, under the path [common/uca/](https://github.com/unicode-org/cldr/blob/main/common/uca/).

For most data files there are **\_SHORT** versions available. They contain the same data but only minimal comments, to reduce the file sizes.

Comments with DUCET-style weights in files other than allkeys_CLDR.txt and allkeys_DUCET.txt use the weights defined in allkeys_CLDR.txt.

* **allkeys_CLDR** - A file that provides a remapping of UCA DUCET weights for use with CLDR.
* **allkeys_DUCET** - The same as DUCET allkeys.txt, but in alternate=non-ignorable sort order, for easier comparison with allkeys_CLDR.txt.
* **FractionalUCA** - A file that provides a remapping of UCA DUCET weights for use with CLDR. The weight values are modified:
  * The weights have variable length, with 1..4 bytes each. Each secondary or tertiary weight currently uses at most 2 bytes.
  * There are tailoring gaps between adjacent weights, so that a number of characters can be tailored to sort between any two root collation elements.
* *** There are**: * There are collation elements with primary weights at the boundaries between reordering groups and Unicode scripts, so that tailoring around the first or last primary of a group/script results in new collation elements that sort and reorder together with that group or script. These boundary weights also define the primary weight ranges for parametric group and script reordering.


  An implementation may modify the weights further to fit the needs of its data structures.

* **UCA_Rules** - A file that specifies the root collation order in the form of [tailoring rules](#Collation_Tailorings). This is only an approximation of the FractionalUCA data, since the rule syntax cannot express every detail of the collation elements. For example, in the DUCET and in FractionalUCA, tertiary differences are usually expressed with special tertiary weights on all collation elements of an expansion, while a typical from-rules builder will modify the tertiary weight of only one of the collation elements.
* **CollationTest_CLDR** - The CLDR versions of the CollationTest files, which use the tailorings for CLDR. For information on the format, see [UTS #10: 12.2 Conformance Tests](https://unicode.org/reports/tr10/#Conformance_Tests) and the [UCA data directory](https://www.unicode.org/reports/tr10/#Data10).
  * CollationTest_CLDR_NON_IGNORABLE.txt
  * CollationTest_CLDR_SHIFTED.txt

### <a name="Root_Data_File_Formats" id="Root_Data_File_Formats" href="#Root_Data_File_Formats">Root Collation Data File Formats</a>

The file formats may change between versions of CLDR. The formats for CLDR 23 and beyond are as follows. As usual, text after a # is a comment.

#### <a name="File_Format_allkeys_CLDR_txt" id="File_Format_allkeys_CLDR_txt" href="#File_Format_allkeys_CLDR_txt">allkeys_CLDR.txt</a>

This file defines CLDR’s tailoring of the DUCET, as described in _[Root Collation](#Root_Collation)_ .

The format is similar to that of [allkeys.txt](https://www.unicode.org/reports/tr10/#File_Format), although there may be some differences in whitespace.

#### <a name="File_Format_FractionalUCA_txt" id="File_Format_FractionalUCA_txt" href="#File_Format_FractionalUCA_txt">FractionalUCA.txt</a>

The format is illustrated by the following sample lines, with commentary afterwards.

```text
[UCA version = 6.0.0]
```

Provides the version number of the UCA table.

```text
[Unified_Ideograph 4E00..9FCC FA0E..FA0F FA11 FA13..FA14 FA1F FA21 FA23..FA24 FA27..FA29 3400..4DB5 20000..2A6D6 2A700..2B734 2B740..2B81D]
```

* Lists the ranges of Unified_Ideograph characters in collation order. (New in CLDR 24.) They map to collation elements with [implicit (constructed) primary weights](https://www.unicode.org/reports/tr10/#Implicit_Weights).


```text
[radical 6=⼅亅:亅𠄌了𠄍-𠄐亇𠄑𬼶-𬼸予㐧𠄒-𠄔𰁒争𠀩𠄕𬼹亊𠄖-𠄘𪜜事㐨𠄙𬼺𠄚𰁓𰁔𠄛𪜝𬼻𠄜𱎑𠄝𬼼]
[radical 210=⿑齊⻬齐⻫斉:齊𪗄𬹱𮮺-𮮼齐𪗅齋䶒䶓𪗆齌𠆜𪗇𪗈𬹳𱌗齍𪗉𪗊𬹲𱌘𪗋𪗌𱌙齎𪗎𪗍齏齑𪗏-𪗓]
[radical end]
```

* Data for Unihan radical-stroke order. (New in CLDR 26, modified in CLDR 46.) Following the `[Unified_Ideograph]` line, a section of `[radical ...]` lines defines a radical-stroke order of the Unified_Ideograph characters.


* For Han characters, an implementation may choose either to implement the order defined in the UCA and the `[Unified_Ideograph]` data, or to implement the order defined by the `[radical ...]` lines. Beginning with CLDR 26, the CJK `type="unihan"` tailorings assume that the root collation order sorts Han characters in Unihan radical-stroke order according to the `[radical ...]` data. The CollationTest_CLDR files only contain Han characters that are in the same relative order using implicit weights or the radical-stroke order.


* The root collation radical-stroke order is derived from the first (normative) values of the [Unihan kRSUnicode](https://www.unicode.org/reports/tr38/#kRSUnicode) field for each Han character. Han characters are ordered by radical. Characters with the same radical are ordered by residual stroke count.


* Starting with CLDR 46, this radical-stroke order matches that of the [UAX #38 section 2.1.2 Sorting Algorithm Used by the Radical-Stroke Indexes](https://www.unicode.org/reports/tr38/#SortingAlgorithm). The distinction between traditional and simplified radicals has been moved from a level above the number of residual strokes (always sorting traditional forms before simplified ones) to a level below the number of residual strokes. This also makes only the traditional forms of the radicals usable for grouping and indexing.


* Before CLDR 46, characters with the same radical-stroke values were ordered by block and code point, as for [UCA implicit weights](https://www.unicode.org/reports/tr10/#Implicit_Weights). Since CLDR 46, for the radical-stroke order, the order of CJK blocks now follows UAX #38 as well.


* There is one `[radical ...]` line per radical, in the order of radical numbers. Each line shows the radical number and the representative characters from the [UCD file CJKRadicals.txt](https://www.unicode.org/reports/tr44/#UCD_Files_Table), followed by a colon (“:”) and the Han characters with that radical in the order as described above. A range like `万-丌` indicates that the code points in that range sort in code point order.


Starting with CLDR 46, the representative characters for all of the traditional and simplified forms of the radical are included on the same line.

* The radical number and characters are informational. The sort order is established only by the order of the `[radical ...]` lines, and within each line by the characters and ranges between the colon (“:”) and the bracket (“]”).


Each Unified_Ideograph occurs exactly once. Only Unified_Ideograph characters are listed on `[radical ...]` lines.

This section is terminated with one `[radical end]` line.

```text
0000; [,,]     # Zyyy Cc       [0000.0000.0000]        * <NULL>
```

* Provides a weight line. The first element (before the ";") is a hex codepoint sequence. The second field is a sequence of collation elements. Each collation element has 3 parts separated by commas: the primary weight, secondary weight, and tertiary weight. The tertiary weight actually consists of two components: the top two bits (0xC0) are used for the _case level_, and should be masked off where a case level is not used.


* A weight is either empty (meaning a zero or ignorable weight) or is a sequence of one or more bytes. The bytes are interpreted as a "fraction", meaning that the ordering is 04 < 05 05 < 06. The weights are constructed so that no weight is an initial subsequence of another: that is, having both the weights 05 and 05 05 is illegal. The above line consists of all ignorable weights.


The vertical bar (“|”) character is used to indicate context, as in:

```text
006C | 00B7; [, DB A9, 05]
```

* This example indicates that if U+00B7 appears immediately after U+006C, it is given the corresponding collation element instead. This syntax is roughly equivalent to the following contraction, but is more efficient. For details see the specification of _[Context-Sensitive Mappings](#Context_Sensitive_Mappings)_ above.


```text
006C 00B7; CE(006C) [, DB A9, 05]
```

* Single-byte primary weights are given to particularly frequent characters, such as space, digits, and a-z. More frequent characters are given two-byte weights, while relatively infrequent characters are given three-byte weights. For example:


```text
...
0009; [03 05, 05, 05] # Zyyy Cc       [0100.0020.0002]        * <CHARACTER TABULATION>
...
1B60; [06 14 0C, 05, 05]    # Bali Po       [0111.0020.0002]        * BALINESE PAMENENG
...
0031; [14, 05, 05]    # Zyyy Nd       [149B.0020.0002]        * DIGIT ONE
```

The assignment of 2 vs 3 bytes does not reflect importance, or exact frequency.

```text
3041; [76 06, 05, 03]   # Hira Lo       [3888.0020.000D]        * HIRAGANA LETTER SMALL A
3042; [76 06, 05, 85]   # Hira Lo       [3888.0020.000E]        * HIRAGANA LETTER A
30A1; [76 06, 05, 10]   # Kana Lo       [3888.0020.000F]        * KATAKANA LETTER SMALL A
30A2; [76 06, 05, 9E]   # Kana Lo       [3888.0020.0011]        * KATAKANA LETTER A
```

* Beginning with CLDR 27, some primary or secondary collation elements may have below-common tertiary weights (e.g., `03` ), in particular to allow normal Hiragana letters to have common tertiary weights.


```ebnf
# <a name="SPECIAL_MAXMIN_COLLATION_ELEMENTS" id="SPECIAL_MAXMIN_COLLATION_ELEMENTS" href="#SPECIAL_MAXMIN_COLLATION_ELEMENTS">SPECIAL MAX/MIN COLLATION ELEMENTS</a>
FFFE; [02, 05, 05]     # Special LOWEST primary, for merge/interleaving
FFFF; [EF FE, 05, 05]  # Special HIGHEST primary, for ranges
```

The two tailored noncharacters have their own primary weights.

```text
F967; [U+4E0D]  # Hani Lo       [FB40.0020.0002][CE0D.0000.0000]        * CJK COMPATIBILITY IDEOGRAPH-F967
2F02; [U+4E36, 10]      # Hani So       [FB40.0020.0004][CE36.0000.0000]        * KANGXI RADICAL DOT
2E80; [U+4E36, 70, 20]  # Hani So       [FB40.0020.0004][CE36.0000.0000][0000.00FC.0004]        * CJK RADICAL REPEAT
```

* Some collation elements are specified by reference to other mappings. This is particularly useful for Han characters which are given implicit/constructed primary weights; the reference to a Unified_Ideograph makes these mappings independent of implementation details. This technique may also be used in other mappings to show the relationship of character variants.


* The referenced character must have a mapping listed earlier in the file, or the mapping must have been defined via the [Unified_Ideograph] data line. The referenced character must map to exactly one collation element.


* **U+4E0D]` copies U+4E0D’s**: `[U+4E0D]` copies U+4E0D’s entire collation element. `[U+4E36, 10]` copies U+4E36’s primary and secondary weights and specifies a different tertiary weight. `[U+4E36, 70, 20]` only copies U+4E36’s primary weight and specifies other secondary and tertiary weights.


* FractionalUCA.txt does not have any explicit mappings for implicit weights. Therefore, an implementation is free to choose an algorithm for computing implicit weights according to the principles specified in the UCA.


```text
FDD1 20AC;      [0D 20 02, 05, 05]      # CURRENCY first primary
FDD1 0034;      [0E 02 02, 05, 05]      # DIGIT first primary starts new lead byte
FDD0 FF21;      [26 02 02, 05, 05]      # REORDER_RESERVED_BEFORE_LATIN first primary starts new lead byte
FDD1 004C;      [28 02 02, 05, 05]      # LATIN first primary starts new lead byte
FDD0 FF3A;      [5D 02 02, 05, 05]      # REORDER_RESERVED_AFTER_LATIN first primary starts new lead byte
FDD1 03A9;      [5F 04 02, 05, 05]      # GREEK first primary starts new lead byte (compressible)
FDD1 03E2;      [5F 60 02, 05, 05]      # COPTIC first primary (compressible)
```

* These are special mappings with primaries at the boundaries of scripts and reordering groups. They serve as tailoring boundaries, so that tailoring near the first or last character of a script or group places the tailored item into the same group. Beginning with CLDR 24, each of these is a contraction of U+FDD1 with a character of the corresponding script (or of the General_Category [Z, P, S, Sc, Nd] corresponding to a special reordering group), mapping to the first possible primary weight per script or group. They can be enumerated for implementations of [Collation Indexes](#Collation_Indexes). (Earlier versions mapped contractions with U+FDD0 to the last primary weights of each group but not each script.)


* Beginning with CLDR 27, these mappings alone define the boundaries for reordering single scripts. (There are no mappings for Hrkt, Hans, or Hant because they are not fully distinct scripts; they share primary weights with other scripts: Hrkt=Hira=Kana & Hans=Hant=Hani.) There are some reserved ranges, beginning at boundaries marked with U+FDD0 plus following characters as shown above. The reserved ranges are not used for collation elements and are not available for tailoring.


* Some primary lead bytes must be reserved so that reordering of scripts along partial-lead-byte boundaries can “split” the primary lead byte and use up a reserved byte. This is for implementations that write sort keys, which must reorder primary weights by offsetting them by whole lead bytes. There are reorder-reserved ranges before and after Latin, so that reordering scripts with few primary lead bytes relative to Latin can move those scripts into the reserved ranges without changing the primary weights of any other script. Each of these boundaries begins with a new two-byte primary; that is, no two groups/scripts/ranges share the top 16 bits of their primary weights.


```text
FDD0 0034;      [11, 05, 05]    # lead byte for numeric sorting
```

* This mapping specifies the lead byte for numeric sorting. It must be different from the lead byte of any other primary weight, otherwise numeric sorting would generate ill-formed collation elements. Therefore, this mapping itself must be excluded from the set of regular mappings. This value can be ignored by implementations that do not support numeric sorting. (Other contractions with U+FDD0 can normally be ignored altogether.)


```ebnf
# <a name="HOMELESS_COLLATION_ELEMENTS" id="HOMELESS_COLLATION_ELEMENTS" href="#HOMELESS_COLLATION_ELEMENTS">HOMELESS COLLATION ELEMENTS</a>
FDD0 0063; [, 97, 3D]       # [15E4.0020.0004] [1844.0020.0004] [0000.0041.001F]    * U+01C6 LATIN SMALL LETTER DZ WITH CARON
FDD0 0064; [, A7, 09]       # [15D1.0020.0004] [0000.0056.0004]     * U+1DD7 COMBINING LATIN SMALL LETTER C CEDILLA
FDD0 0065; [, B1, 09]       # [1644.0020.0004] [0000.0061.0004]     * U+A7A1 LATIN SMALL LETTER G WITH OBLIQUE STROKE
```

* The DUCET has some weights that don't correspond directly to a character. To allow for implementations to have a mapping for each collation element (necessary for certain implementations of tailoring), this requires the construction of special sequences for those weights. These collation elements can normally be ignored.


Next, a number of tables are defined. The function of each of the tables is summarized afterwards.

```text
# <a name="VALUES_BASED_ON_UCA" id="VALUES_BASED_ON_UCA" href="#VALUES_BASED_ON_UCA">VALUES BASED ON UCA</a>
...
[first regular [0D 0A, 05, 05]] # U+0060 GRAVE ACCENT
[last regular [7A FE, 05, 05]] # U+1342E EGYPTIAN HIEROGLYPH AA032
[first implicit [E0 04 06, 05, 05]] # CONSTRUCTED
[last implicit [E4 DF 7E 20, 05, 05]] # CONSTRUCTED
[first trailing [E5, 05, 05]] # CONSTRUCTED
[last trailing [E5, 05, 05]] # CONSTRUCTED
...
```

This table summarizes ranges of important groups of characters for implementations.

```text
# <a name="Top_Byte_Reordering_Tokens" id="Top_Byte_Reordering_Tokens" href="#Top_Byte_Reordering_Tokens">Top Byte => Reordering Tokens</a>
[top_byte     00      TERMINATOR ]    #       [0]     TERMINATOR=1
[top_byte     01      LEVEL-SEPARATOR ]       #       [0]     LEVEL-SEPARATOR=1
[top_byte     02      FIELD-SEPARATOR ]       #       [0]     FIELD-SEPARATOR=1
[top_byte     03      SPACE ] #       [9]     SPACE=1 Cc=6 Zl=1 Zp=1 Zs=1
...
```

* This table is mostly irrelevant, except for the "COMPRESS" data. The table defines reordering group for simple script reordering by primary lead bytes. The table maps from the first bytes of the fractional weights to a reordering token. The format is `"[top_byte " byte-value reordering-token "COMPRESS"? "]"`. The "COMPRESS" value is present when there is only one byte in the reordering token, and primary-weight compression can be applied. Most reordering tokens are script values; others are special-purpose values, such as PUNCTUATION. Beginning with CLDR 24, this table precedes the regular mappings, so that parsers can use this information while processing and optimizing mappings. Beginning with CLDR 27, most of this data is irrelevant because single scripts can be reordered. Only the "COMPRESS" data is still useful.


```text
# <a name="Reordering_Tokens_Top_Bytes" id="Reordering_Tokens_Top_Bytes" href="#Reordering_Tokens_Top_Bytes">Reordering Tokens => Top Bytes</a>
[reorderingTokens     Arab    61=910 62=910 ]
[reorderingTokens     Armi    7A=22 ]
[reorderingTokens     Armn    5F=82 ]
[reorderingTokens     Avst    7A=54 ]
...
```

* This table is informational; it is an inverse mapping from reordering token to top byte(s). In terms like "61=910", the first value is the top byte, while the second indicates the number of primaries assigned with that top byte.


```text
# <a name="General_Categories_Top_Byte" id="General_Categories_Top_Byte" href="#General_Categories_Top_Byte">General Categories => Top Byte</a>
[categories   Cc      03{SPACE}=6 ]
[categories   Cf      77{Khmr Tale Talu Lana Cham Bali Java Mong Olck Cher Cans Ogam Runr Orkh Vaii Bamu}=2 ]
[categories   Lm      0D{SYMBOL}=25 0E{SYMBOL}=22 27{Latn}=12 28{Latn}=12 29{Latn}=12 2A{Latn}=12...
```

This table is informational, providing the top bytes, scripts, and primaries associated with each general category value.

```text
# <a name="FIXED_VALUES" id="FIXED_VALUES" href="#FIXED_VALUES">FIXED VALUES</a>
[fixed first implicit byte E0]
[fixed last implicit byte E4]
[fixed first trail byte E5]
[fixed last trail byte EF]
[fixed first special byte F0]
[fixed last special byte FF]

[fixed secondary common byte 05]
[fixed last secondary common byte 45]
[fixed first ignorable secondary byte 80]

[fixed tertiary common byte 05]
[fixed first ignorable tertiary byte 3C]
```

The final table gives certain hard-coded byte values. The "trail" area is provided for implementation of the "trailing weights" as described in the UCA.

> 👉 **Note**: The particular primary lead bytes for Hani vs. IMPLICIT vs. TRAILING are only an example. An implementation is free to move them if it also moves the explicit TRAILING weights. This affects only a small number of explicit mappings in FractionalUCA.txt, such as for U+FFFD, U+FFFF, and the “unassigned first primary”. It is possible to use no SPECIAL bytes at all, and to use only the one primary lead byte FF for TRAILING weights.

* Starting with CLDR 48/Unicode 17, the root collation data files include `FractionalUCA_blanked.txt` which has the same contents as `FractionalUCA.txt` but with “blanked weights” for most non-zero collation weights. It is not useful as a _data_ file, but it is valuable for simple diffing between versions of the data, showing changes in the sort order and in the number of bytes in fractional weights.


#### <a name="File_Format_UCA_Rules_txt" id="File_Format_UCA_Rules_txt" href="#File_Format_UCA_Rules_txt">UCA_Rules.txt</a>

The format for this file uses the CLDR collation syntax, see _[Collation Tailorings](#Collation_Tailorings)_.


## <a name="Character_Elements" id="Character_Elements" href="#Character_Elements">Character Elements</a>

```dtd
<!ELEMENT characters ( alias | ( exemplarCharacters*, ellipsis*, moreInformation*, stopwords*, indexLabels*, mapping*, parseLenients*, special* ) ) >
```

* **The `` element**: The `<characters>` element provides optional information about characters that are in common use in the locale, and information that can be helpful in picking resources or data appropriate for the locale, such as when choosing among character encodings that are typically used to transmit data in the language of the locale. It may also be used to help reduce confusability issues: see [[UTR39](https://www.unicode.org/reports/tr41/#UTR36)]. It typically only occurs in a language locale, not in a language/territory locale. The stopwords are an experimental feature, and should not be used.


### <a name="Exemplars" id="Exemplars" href="#Exemplars">Exemplars</a>

Exemplars are characters used by a language, separated into different categories. The following table provides a summary, with more details below.

| Type            | Description | Examples |
| --------------- | ----------- | -------- |
| main / standard | Main letters used in the language | a-z å æ ø |
| auxiliary       | Additional characters for common foreign words, technical usage | á à ă â å ä ã ā æ ç é è ĕ ê ë ē í ì ĭ î ï ī ñ ó ò ŏ ô ö ø ō œ ú ù ŭ û ü ū ÿ |
| numbers         | Main characters needed to display the common number formats: decimal, percent, and currency. | \[\\u061C\\u200E \\- , ٫ ٬ . % ٪ ‰ ؉ + 0٠ 1١ 2٢ 3٣ 4٤ 5٥ 6٦ 7٧ 8٨ 9٩\] |
| numbers-auxiliary         | Additional characters for use with numbers (technical or older usage) |  |
| punctuation     | Main punctuation characters | - ‐ – — , ; \\: ! ? . … “ ” ‘ ’ ( ) [ ] § @ * / & # † ‡ ′ ″ |
| punctuation-auxiliary     | Additional punctuation (technical or older usage) |  |
| punctuation-person     | Punctuation used in people names, such as "Jean-Luc Smith Ph.D., MD. | - / . , |
| index           | Characters for the header of an index | A B C D E F G H I J K L M N O P Q R S T U V W X Y Z |

* The basic exemplar character sets (main and auxiliary) contain the commonly used letters for a given modern form of a language, which can be for testing and for determining the appropriate repertoire of letters for charset conversion or collation. ("Letter" is interpreted broadly, as anything having the property Alphabetic in the [[UAX44](https://www.unicode.org/reports/tr41/#UAX44)], which also includes syllabaries and ideographs.) It is not a complete set of letters used for a language, nor should it be considered to apply to multiple languages in a particular country. Punctuation and other symbols should not be included in the main and auxiliary sets. In particular, format characters like CGJ are not included.


There are 4 types of sets altogether: main, numbers, punctuation, and index.
Within each type, there are are subtypes:
a _main_ set containing the minimal set required for users of the language,
and an _auxiliary_ set, which is designed to encompass additional characters —
those non-native or historical characters that would customarily occur in common publications, dictionaries, and so on.
There are two exceptions: an index set doesn't have an _auxiliary_ set,
and the punctuation set has an additional subtype for person-name punctuation (see [Person Name Validation](tr35-personNames.md#person-name-validation).

* Major style guidelines are good references for an auxiliary set. So, for example, if Irish newspapers and magazines would commonly have Danish names using å, for example, then it would be appropriate to include å in the auxiliary exemplar characters; just not in the main exemplar set. Thus English has the following:


```xml
<exemplarCharacters>[a b c d e f g h i j k l m n o p q r s t u v w x y z]</exemplarCharacters>
<exemplarCharacters type="auxiliary">[á à ă â å ä ã ā æ ç é è ĕ ê ë ē í ì ĭ î ï ī ñ ó ò ŏ ô ö ø ō œ ú ù ŭ û ü ū ÿ]</exemplarCharacters>
```

For a given language, there are a few factors that help for determining whether a character belongs in the auxiliary set, instead of the main set:

*   The character is not available on all normal keyboards.
*   It is acceptable to always use spellings that avoid that character.

* For example, the exemplar character set for en (English) is the set \[a-z\]. This set does not contain the accented letters that are sometimes seen in words like "résumé" or "naïve", because it is acceptable in common practice to spell those words without the accents. The exemplar character set for fr (French), on the other hand, must contain those characters: \[a-z é è ù ç à â ê î ô û æ œ ë ï ÿ\]. The main set typically includes those letters commonly "alphabet".


* The _punctuation_ set consists of common punctuation characters that are used with the language (corresponding to main and auxiliary). Symbols may also be included where they are common in plain text, such as ©. It does not include characters with narrow technical usage, such as dictionary punctuation/symbols or copy-edit symbols. For example, English would have something like the following:


> - ‐ – —
> , ; : ! ? . …
> ' ‘ ’ " “ ” ′ ″
> ( ) [ \] { } ⟨ ⟩
> © ® ™ @ & ° ‧ ·/ # % ¶ § * † ‡
> + − ± × ÷ < ≤ = ≅ ≥ > √

* The numbers exemplars do not currently include lesser-used characters: exponential notation (3.1 × 10²³, ∞, NaN). Nor does it contain the units or currency symbols such as $, ¥, ₹, … It does contain %, because that occurs in the percent format. It may contain some special formatting characters like the RLM. A full list of the currency symbols used with that locale are in the `<currencies>` element, while the units can be gotten from the `<units>` element (both using inheritance, of course).The digits used in each numbering system are accessed in numberingSystems.xml. For more information, see _**Part 3: [Numbers](tr35-numbers.md#Contents)**, [Number Elements](tr35-numbers.md#Number_Elements)_.


_Examples for zh.xml:_

| Type                              | Description |
| --------------------------------- | ----------- |
| defaultNumberingSystem            | latn        |
| otherNumberingSystems/native      | hanidec     |
| otherNumberingSystems/traditional | hans        |
| otherNumberingSystems/finance     | hansfin     |

* When determining the character repertoire needed to support a language, a reasonable initial set would include at least the characters in the main and punctuation exemplar sets, along with the digits and common symbols associated with the numberSystems supported for the locale (see _[Numbering Systems](tr35-numbers.md#Numbering_Systems)_).


* The _index_ characters are a set of characters for use as a UI "index", that is, a list of clickable characters (or character sequences) that allow the user to see a segment of a larger "target" list. For details see the [Unicode LDML: Collation](tr35-collation.md#Collation_Indexes) document. The index set may only contain characters whose lowercase versions are in the main and auxiliary exemplar sets, though for cased languages the index exemplars are typically in uppercase. Characters from the auxiliary exemplar set may be necessary in the index set if it needs to properly handle items such as names which may require characters not included in the main exemplar set.


Here is a sample of the XML structure:

```xml
<exemplarCharacters type="index">[A B C D E F G H I J K L M N O P Q R S T U V W X Y Z]</exemplarCharacters>
```

The display of the index characters can be modified with the `indexLabel`s elements, discussed in Section 3.3.

#### <a name="ExemplarSyntax" id="ExemplarSyntax" href="#ExemplarSyntax">Exemplar Syntax</a>

In all of the exemplar characters, the list of characters is in the [Unicode Set](tr35.md#Unicode_Sets) format, which normally allows boolean combinations of sets of letters and Unicode properties.

* Sequences of characters that act like a single letter in the language — especially in collation — are included within braces, such as `[a-z á é í ó ú ö ü ő ű {cs} {dz} {dzs} {gy} ...]`. The characters should be in normalized form (NFC). Where combining marks are used generatively, and apply to a large number of base characters (such as in Indic scripts), the individual combining marks should be included. Where they are used with only a few base characters, the specific combinations should be included. Wherever there is not a precomposed character (for example, single codepoint) for a given combination, that must be included within braces. For example, to include sequences from the [Where is my Character?](https://www.unicode.org/standard/where/) page on the Unicode site, one would write: `[{ch} {tʰ} {x̣} {ƛ̓} {ą́} {i̇́} {ト゚}]`, but for French one would just write `[a-z é è ù ...]`. When in doubt use braces, since it does no harm to include them around single code points: for example, `[a-z {é} {è} {ù} ...]`.


* If the letter 'z' were only ever used in the combination 'tz', then we might have `[a-y {tz}]` in the main set. (The language would probably have plain 'z' in the auxiliary set, for use in foreign words.) If combining characters can be used productively in combination with a large number of others (such as say Indic matras), then they are not listed in all the possible combinations, but separately, such as:


```text
[ॐ ऄ-ऋ ॠ ऌ ॡ ऍ-क क़ ख ख़ ग ग़ घ-ज ज़ झ-ड ड़ ढ ढ़ ण-फ फ़ ब-य य़ र-ह ़ ँ-ः ॑-॔ ऽ ् ॽ ा-ॄ ॢ ॣ ॅ-ौ]
```

* The exemplar character set for Han characters is composed somewhat differently. It is even harder to draw a clear line for Han characters, since usage is more like a frequency curve that slowly trails off to the right in terms of decreasing frequency. So for this case, the exemplar characters simply contain a set of reasonably frequent characters for the language.


* The ordering of the characters in the set is irrelevant, but for readability in the XML file the characters should be in sorted order according to the locale's conventions. The main and auxiliary sets should only contain lower case characters (except for the special case of Turkish and similar languages, where the dotted capital I should be included); the upper case letters are to be mechanically added when the set is used. For more information on casing, see the discussion of Special Casing in the Unicode Character Database.


#### <a name="Restrictions" id="Restrictions" href="#Restrictions">Restrictions</a>

1.  The main, auxiliary and index sets are normally restricted to those letters with a specific [Script](https://www.unicode.org/Public/UNIDATA/Scripts.txt) character property (that is, not the values Common or Inherited) or required [Default_Ignorable_Code_Point](https://www.unicode.org/Public/UNIDATA/DerivedCoreProperties.txt) characters (such as a non-joiner), or combining marks, or the [Word_Break](https://www.unicode.org/Public/UNIDATA/auxiliary/WordBreakProperty.txt) properties [Katakana](https://www.unicode.org/reports/tr29/#Katakana), [ALetter](https://www.unicode.org/reports/tr29/#ALetter), or [MidLetter](https://www.unicode.org/reports/tr29/#MidLetter).
2.  The auxiliary set should not overlap with the main set. There is one exception to this: Hangul Syllables and CJK Ideographs can overlap between the sets.
3.  Any [Default_Ignorable_Code_Point](https://www.unicode.org/Public/UNIDATA/DerivedCoreProperties.txt)s should be in the auxiliary set, or, if they are only needed for currency formatting, in the currency set. These can include characters such as U+200E LEFT-TO-RIGHT MARK and U+200F RIGHT-TO-LEFT MARK which may be needed in bidirectional text in order for date, currency or other formats to display correctly.
4.  For exemplar characters the [Unicode Set](tr35.md#Unicode_Sets) format is restricted so as to not use properties or boolean combinations.

### <a name="Character_Mapping" id="Character_Mapping" href="#Character_Mapping">~~Mapping~~</a>

**This element has been deprecated.** For information on its structure and how it was intended to specify locale-specific preferred encodings for various purposes (e-mail, web), see the [Mapping](tr35-general.md#Character_Mapping) section from the CLDR 27 version of the LDML Specification.

### <a name="IndexLabels" id="IndexLabels" href="#IndexLabels">~~Index Labels~~</a>

**This element and its subelements have been deprecated.** For information on its structure and how it was intended to provide data for a compressed display of index exemplar characters where space is limited, see the [Index Labels](tr35-general.md#IndexLabels) section from the CLDR 27 version of the LDML Specification.

```dtd
<!ELEMENT indexLabels (indexSeparator*, compressedIndexSeparator*, indexRangePattern*, indexLabelBefore*, indexLabelAfter*, indexLabel*) >
```

### <a name="Ellipsis" id="Ellipsis" href="#Ellipsis">Ellipsis</a>

```dtd
<!ELEMENT ellipsis ( #PCDATA ) >
<!ATTLIST ellipsis type ( initial | medial | final | word-initial | word-medial | word-final ) #IMPLIED >
```

* The `ellipsis` element provides patterns for use when truncating strings. There are three versions: initial for removing an initial part of the string (leaving final characters); medial for removing from the center of the string (leaving initial and final characters), and final for removing a final part of the string (leaving initial characters). For example, the following uses the ellipsis character in all three cases (although some languages may have different characters for different positions).


```xml
<ellipsis type="initial">…{0}</ellipsis>
<ellipsis type="medial">{0}…{1}</ellipsis>
<ellipsis type="final">{0}…</ellipsis>
```

There are alternatives for cases where the breaks are on a word boundary, where some languages include a space. For example, such a case would be:

```xml
<ellipsis type="word-initial">… {0}</ellipsis>
```

### <a name="Character_Nested_Bracket_Replacement" id="Character_Nested_Bracket_Replacement" href="#Character_Nested_Bracket_Replacement">Nested Bracket Replacement</a>

```dtd
<!ELEMENT nestedBracketReplacement ( #PCDATA ) >
<!ATTLIST nestedBracketReplacement bracket CDATA #REQUIRED >
```

Example:

```xml
<nestedBracketReplacement bracket="(">[</nestedBracketReplacement>
<nestedBracketReplacement bracket=")">]</nestedBracketReplacement>
<nestedBracketReplacement bracket="（">［</nestedBracketReplacement>
<nestedBracketReplacement bracket="）">］</nestedBracketReplacement>
```

The `nestedBracketReplacement` element indicates a character to be used when two sets of brackets (parentheses) are nested. This currently supports only one level of nesting.

* Clients should replace the inner bracket pair by substituting the bracket string with the value string. For example, in the string "a ( b ( c ) )", the two inner brackets should be replaced according to the replacements data, resulting in "a ( b [ c ] )".


In cases where it is necessary to determine whether the brackets are nested, clients can use the `Bidi_Paired_Bracket_Type` property.

### <a name="Character_More_Info" id="Character_More_Info" href="#Character_More_Info">More Information</a>

The moreInformation string is one that can be displayed in an interface to indicate that more information is available. For example:

```xml
<moreInformation>?</moreInformation>
```

### <a name="Character_Parse_Lenient" id="Character_Parse_Lenient" href="#Character_Parse_Lenient">Parse Lenient</a>

```dtd
<!ELEMENT parseLenients ( alias | ( parseLenient*, special* ) ) >
<!ATTLIST parseLenients scope (general | number | date) #REQUIRED >
<!ATTLIST parseLenients level (lenient | stricter) #REQUIRED >

<!ELEMENT parseLenient ( #PCDATA ) >
<!ATTLIST parseLenient sample CDATA #REQUIRED >
<!ATTLIST parseLenient alt NMTOKENS #IMPLIED >
<!ATTLIST parseLenient draft (approved | contributed | provisional | unconfirmed) #IMPLIED >
```

Example:

```xml
<parseLenients scope="date" level="lenient">
    <parseLenient sample="-">[\-./]</parseLenient>
    <parseLenient sample=":">[\:∶]</parseLenient>
</parseLenients>
```

* The `parseLenient` elements are used to indicate that characters within a particular UnicodeSet are normally to be treated as equivalent when doing a lenient parse. The `scope` attribute value defines where the lenient sets are intended for use. The `level` attribute value is included for future expansion; currently the only value is "lenient".


* The `sample` attribute value is a paradigm element of that UnicodeSet, but the only reason for pulling it out separately is so that different classes of characters are separated, and to enable inheritance overriding. The first version of this data is populated with the data used for lenient parsing from ICU.



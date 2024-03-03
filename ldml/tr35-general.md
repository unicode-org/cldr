## Unicode Technical Standard #35

# Unicode Locale Data Markup Language (LDML)<br/>Part 2: General

|Version|45 (draft)           |
|-------|---------------------|
|Editors|Yoshito Umaoka (<a href="mailto:yoshito_umaoka@us.ibm.com">yoshito_umaoka@us.ibm.com</a>) and <a href="tr35.md#Acknowledgments">other CLDR committee members|

For the full header, summary, and status, see [Part 1: Core](tr35.md).

### _Summary_

This document describes parts of an XML format (_vocabulary_) for the exchange of structured locale data. This format is used in the [Unicode Common Locale Data Repository](https://www.unicode.org/cldr/).

This is a partial document, describing general parts of the LDML: display names & transforms, etc. For the other parts of the LDML see the [main LDML document](tr35.md) and the links above.

_Note:_
Some links may lead to in-development or older
versions of the data files.
See <https://cldr.unicode.org> for up-to-date CLDR release data.

### _Status_

_This is a draft document which may be updated, replaced, or superseded by other documents at any time.
Publication does not imply endorsement by the Unicode Consortium.
This is not a stable document; it is inappropriate to cite this document as other than a work in progress._

<!-- _This document has been reviewed by Unicode members and other interested parties, and has been approved for publication by the Unicode Consortium.
This is a stable document and may be used as reference material or cited as a normative reference by other specifications._ -->

> _**A Unicode Technical Standard (UTS)** is an independent specification. Conformance to the Unicode Standard does not imply conformance to any UTS._

_Please submit corrigenda and other comments with the CLDR bug reporting form [[Bugs](tr35.md#Bugs)]. Related information that is useful in understanding this document is found in the [References](tr35.md#References). For the latest version of the Unicode Standard see [[Unicode](tr35.md#Unicode)]. For a list of current Unicode Technical Reports see [[Reports](tr35.md#Reports)]. For more information about versions of the Unicode Standard, see [[Versions](tr35.md#Versions)]._

## <a name="Parts" href="#Parts">Parts</a>

The LDML specification is divided into the following parts:

*   Part 1: [Core](tr35.md#Contents) (languages, locales, basic structure)
*   Part 2: [General](tr35-general.md#Contents) (display names & transforms, etc.)
*   Part 3: [Numbers](tr35-numbers.md#Contents) (number & currency formatting)
*   Part 4: [Dates](tr35-dates.md#Contents) (date, time, time zone formatting)
*   Part 5: [Collation](tr35-collation.md#Contents) (sorting, searching, grouping)
*   Part 6: [Supplemental](tr35-info.md#Contents) (supplemental data)
*   Part 7: [Keyboards](tr35-keyboards.md#Contents) (keyboard mappings)
*   Part 8: [Person Names](tr35-personNames.md#Contents) (person names)
*   Part 9: [MessageFormat](tr35-messageFormat.md#Contents) (message format)

## <a name="Contents" href="#Contents">Contents of Part 2, General</a>

* [Display Name Elements](#Display_Name_Elements)
  * [Locale Display Name Algorithm](#locale_display_name_algorithm)
  * [Locale Display Name Fields](#locale_display_name_fields)
* [Layout Elements](#Layout_Elements)
* [Character Elements](#Character_Elements)
  * [Exemplars](#Exemplars)
    * [Exemplar Syntax](#ExemplarSyntax)
    * [Restrictions](#Restrictions)
  * ~~[Mapping](#Character_Mapping)~~
  * ~~[Index Labels](#IndexLabels)~~
  * [Ellipsis](#Ellipsis)
  * [More Information](#Character_More_Info)
  * [Parse Lenient](#Character_Parse_Lenient)
* [Delimiter Elements](#Delimiter_Elements)
  * [Tailoring Linebreak Using Delimiters](#Tailor_Linebreak_With_Delimiters)
* [Measurement System Data](#Measurement_System_Data)
  * [Measurement Elements (deprecated)](#Measurement_Elements)
* [Unit Elements](#Unit_Elements)
  * [Unit Preference and Conversion Data](#Unit_Preference_and_Conversion)
  * [Unit Identifiers](#Unit_Identifiers)
    * [Nomenclature](#nomenclature)
    * [Syntax](#syntax)
  * [Unit Identifier Uniqueness](#Unit_Identifier_Uniqueness)
  * [Example Units](#Example_Units)
  * [Compound Units](#compound-units)
    * [Precomposed Compound Units](#precomposed-compound-units)
  * [Unit Sequences (Mixed Units)](#Unit_Sequences)
  * [durationUnit](#durationUnit)
  * [coordinateUnit](#coordinateUnit)
  * [Territory-Based Unit Preferences](#Territory_Based_Unit_Preferences)
  * [Private-Use Units](#Private_Use_Units)
* [POSIX Elements](#POSIX_Elements)
* [Reference Element](#Reference_Elements)
* [Segmentations](#Segmentations)
  * [Segmentation Inheritance](#Segmentation_Inheritance)
  * [Segmentation Suppressions](#Segmentation_Exceptions)
* [Transforms](#Transforms)
  * [Inheritance](#Inheritance)
    * [Pivots](#Pivots)
  * [Variants](#Variants)
  * [Transform Rules Syntax](#Transform_Rules_Syntax)
    * [Dual Rules](#Dual_Rules)
    * [Context](#Context)
    * [Revisiting](#Revisiting)
    * [Example](#Example)
    * [Rule Syntax](#Rule_Syntax)
    * [Transform Rules](#Transform_Rules)
    * [Variable Definition Rules](#Variable_Definition_Rules)
    * [Filter Rules](#Filter_Rules)
    * [Conversion Rules](#Conversion_Rules)
    * [Intermixing Transform Rules and Conversion Rules](#Intermixing_Transform_Rules_and_Conversion_Rules)
    * [Inverse Summary](#Inverse_Summary)
  * [Transform Syntax Characters](#transform-syntax-characters)
* [List Patterns](#ListPatterns)
  * [Gender of Lists](#List_Gender)
* [ContextTransform Elements](#Context_Transform_Elements)
  * Table: [Element contextTransformUsage type attribute values](#contextTransformUsage_type_attribute_values)
* [Choice Patterns](#Choice_Patterns)
* [Annotations and Labels](#Annotations)
  * [Synthesizing Sequence Names](#SynthesizingNames)
    * [Table: Synthesized Emoji Sequence Names](#table-synthesized-emoji-sequence-names)
  * [Annotations Character Labels](#Character_Labels)
    * [Table: characterLabelPattern](#table-characterlabelpattern)
    * [Table: characterLabel](#table-characterlabel)
  * [Typographic Names](#Typographic_Names)
* [Grammatical Features](#Grammatical_Features)
* [Features](#features)
  * [Gender](#Gender)
    * [Example](#example)
    * [Table: Values](#table-values)
  * [Case](#Case)
    * [Table: Case](#table-case)
      * [Example](#example)
        * [Table: Values](#table-values)
  * [Definiteness](#definiteness)
    * [Table: Values](#table-values)
* [Grammatical Derivations](#Grammatical_Derivations)
  * [Deriving the Gender of Compound Units](#gender_compound_units)
  * [Deriving the Plural Category of Unit Components](#plural_compound_units)
  * [Deriving the Case of Unit Components](#case_compound_units)

## <a name="Display_Name_Elements" href="#Display_Name_Elements">Display Name Elements</a>

```xml
<!ELEMENT localeDisplayNames ( alias | ( localeDisplayPattern?, languages?, scripts?, territories?, subdivisions?, variants?, keys?, types?, transformNames?, measurementSystemNames?, codePatterns?, special* ) )>
```

Display names for scripts, languages, countries, currencies, and variants in this locale are supplied by this element. They supply localized names for these items for use in user-interfaces for various purposes such as displaying menu lists, displaying a language name in a dialog, and so on. Capitalization should follow the conventions used in the middle of running text; the `<contextTransforms>` element may be used to specify the appropriate capitalization for other contexts (see _[ContextTransform Elements](#Context_Transform_Elements)_). Examples are given below.

> **Note:** The "en" locale may contain translated names for deprecated codes for debugging purposes. Translation of deprecated codes into other languages is discouraged.

Where present, the display names must be unique; that is, two distinct codes would not get the same display name. (There is one exception to this: in time zones, where parsing results would give the same GMT offset, the standard and daylight display names can be the same across different time zone IDs.)

Any translations should follow customary practice for the locale in question. For more information, see [[Data Formats](tr35.md#DataFormats)].

```xml
<localeDisplayPattern>
```
```xml
<!ELEMENT localeDisplayPattern ( alias | (localePattern*, localeSeparator*, localeKeyTypePattern*, special*) ) >
```

For compound language (locale) IDs such as "pt_BR" which contain additional subtags beyond the initial language code: When the `<languages>` data does not explicitly specify a display name such as "Brazilian Portuguese" for a given compound language ID, "Portuguese (Brazil)" from the display names of the subtags.

It includes three sub-elements:

*   The `<localePattern>` element specifies a pattern such as "{0} ({1})" in which {0} is replaced by the display name for the primary language subtag and {1} is replaced by a list of the display names for the remaining subtags.
*   The `<localeSeparator>` element specifies a pattern such as "{0}, {1}" used when appending a subtag display name to the list in the `<localePattern>` subpattern {1} above. If that list includes more than one display name, then `<localeSeparator>` subpattern {1} represents a new display name to be appended to the current list in {0}. _Note: Before CLDR 24, the `<localeSeparator>` element specified a separator string such as ", ", not a pattern._
*   The `<localeKeyTypePattern>` element specifies the pattern used to display key-type pairs, such as "{0}: {1}"

For example, for the locale identifier zh_Hant_CN_co_pinyin_cu_USD, the display would be "Chinese (Traditional, China, Pinyin Sort Order, Currency: USD)". The key-type for co_pinyin doesn't use the localeKeyTypePattern because there is a translation for the key-type in English:

```xml
<type type="pinyin" key="collation">Pinyin Sort Order</type>
```

### <a name="locale_display_name_algorithm" href="#locale_display_name_algorithm">Locale Display Name Algorithm</a>

A locale display name LDN is generated for a locale identifier L in the following way. First, convert the locale identifier to *canonical syntax* per **[Part 1, Canonical Unicode Locale Identifiers](tr35.md#Canonical_Unicode_Locale_Identifiers)**. That will put the subtags in a defined order, and replace aliases by their canonical counterparts. (That defined order is followed in the processing below.)

Then follow each of the following steps for the subtags in L, building a base name LDN and a list of qualifying strings LQS.

Where there is a match for a subtag, disregard that subtag from L and add the element value to LDN or LQS as described below. If there is no match for a subtag, use the fallback pattern with the subtag instead.

Once LDN and LQS are built, return the following based on the length of LQS.

<!-- HTML: no header -->
<table><tbody>
<tr><td>0</td><td>return LDN</td></tr>
<tr><td>1</td><td>use the &lt;localePattern&gt; to compose the result LDN from LDN and LQS[0], and return it.</td></tr>
<tr><td>&gt;1</td><td>use the &lt;localeSeparator&gt; element value to join the elements of the list into LDN2, then use the &lt;localePattern&gt; to compose the result LDN from LDN and LDN2, and return it.</td></tr>
</tbody></table>

The processing can be controlled via the following parameters.

*   `CombineLanguage`: boolean
    *   Example: the `CombineLanguage = true`, picking the bold value below.
    *   `<language type="nl">Dutch</language>`
    *   **`<language type="nl_BE">Flemish</language>`**
*   `PreferAlt`: map from element to preferred alt value, picking the bold value below.
    *   Example: the `PreferAlt` contains `{"language"="short"}`:
    *   `<language type="az">Azerbaijani</language>`
    *   **`<language type="az" alt="short">Azeri</language>`**

In addition, the input locale display name could be minimized (see [Part 1: Likely Subtags](tr35.md#Likely_Subtags)) before generating the LDN. Selective minimization is often the best choice. For example, in a menu list it is often clearer to show the region if there are any regional variants. Thus the user would just see \["Spanish"\] for es if the latter is the only supported Spanish, but where es-MX is also listed, then see \["Spanish (Spain)", "Spanish (Mexico)"\].

* * *

**Processing types of locale identifier subtags**

When the display name contains "(" or ")" characters (or full-width equivalents), replace them by "\[", "\]" (or full-width equivalents) before adding.

1.  **Language.** Match the L subtags against the type values in the `<language>` elements. Pick the element with the most subtags matching. If there is more than one such element, pick the one that has subtypes matching earlier. If there are two such elements, pick the one that is alphabetically less. If there is no match, then further convert L to *canonical form* per **[Part 1, Canonical Unicode Locale Identifiers](tr35.md#Canonical_Unicode_Locale_Identifiers)** and try the preceding steps again. Set LBN to the selected value. Disregard any of the matching subtags in the following processing.
    *   If CombineLanguage is false, only choose matches with the language subtag matching.
2.  **Script, Region, Variants.** Where any of these subtags are in L, append the matching element value to LQS.
3.  **T extensions.** Get the value of the `key="h0" type="hybrid"` element, if there is one; otherwise the value of the `<key type="t">` element. Next get the locale display name of the tlang. Join the pair using `<localePattern>` and append to the LQS. Then format and add display names to LQS for any of the remaining tkey-tvalue pairs as described below.
4.  **U extensions.** If there is an attribute value A, process the key-value pair <"u", A> as below and append to LQS. Then format and add display names for each of the remaining key-type pairs as described below.
5.  **Other extensions.** There are currently no such extensions defined. Until such time as there are formats defined for them, append each of the extensions’ subtags to LQS.
6.  **Private Use extensions.** Get the value

**Formatting T/U Key-Value pairs as display names**

1.  If there is a match for the key/value, then append the element value and return.
2.  Otherwise, get the display name for the key, using the subtag if not available.
3.  Format special values. As usual, if lacking data, use the subtag(s).
    1.  key="kr": (REORDER_CODE) assume the value is a script code, and get its display name.
    2.  key="dx": (SCRIPT_CODE) assume the value is a script code, and get its display name.
    3.  key="vt": (CODEPOINTS, deprecated) the value is a list of code points. Set the value display name to it, after replacing \[-\_\] by space.
    4.  key="x0": (PRIVATE_USE) the value is a list of subtags. No formatting available, so use the subtag(s).
    5.  key="sd": (SUBDIVISION_CODE) use the subdivision data to find the display name.
    6.  key="rg": (RG_KEY_VALUE): handle as with key="sd"
4.  Then use the value of the `<localeKeyTypePattern>` element to join the key display name and the value display name, and append the result to LQS.

**Examples of English locale display names**

| Locale identifier             | Locale display name |
| ----------------------------- | ------------------- |
| es                            | Spanish |
| es-419                        | Spanish (Latin America) |
| es-Cyrl-MX                    | Spanish (Cyrillic, Mexico) |
| en-Latn-GB-fonipa-scouse      | English (Latin, United Kingdom, IPA Phonetics, Scouse) |
| en-u-nu-thai-ca-islamic-civil | English (Calendar: islamic-civil, Thai Digits) |
| hi-u-nu-latn-t-en-h0-hybrid   | Hindi (Hybrid: English, Western Digits) |
| en-u-nu-deva-t-de             | English (Transform: German, Devanagari Digits) |
| fr-z-zz-zzz-v-vv-vvv-u-uu-uuu-t-ru-Cyrl-s-ss-sss-a-aa-aaa-x-u-x | French (Transform: Russian \[Cyrillic\], uu: uuu, a: aa-aaa, s: ss-sss, v: vv-vvv, x: u-x, z: zz-zzz) |



### <a name="locale_display_name_fields" href="#locale_display_name_fields">Locale Display Name Fields</a>

```xml
<languages>
```

This contains a list of elements that provide the user-translated names for language codes, as described in _[Unicode Language and Locale Identifiers](tr35.md#Unicode_Language_and_Locale_Identifiers)_.

```xml
<language type="ab">Abkhazian</language>
<language type="aa">Afar</language>
<language type="af">Afrikaans</language>
<language type="sq">Albanian</language>
```

There should be no expectation that the list of languages with translated names be complete: there are thousands of languages that could have translated names. For debugging purposes or comparison, when a language display name is missing, the Description field of the language subtag registry can be used to supply a fallback English user-readable name.

The type can actually be any locale ID as specified above. The set of which locale IDs is not fixed, and depends on the locale. For example, in one language one could translate the following locale IDs, and in another, fall back on the normal composition.

| type | translation | composition |
| --- | --- | --- |
| nl_BE | Flemish | Dutch (Belgium) |
| zh_Hans | Simplified Chinese | Chinese (Simplified) |
| en_GB | British English | English (United Kingdom) |

Thus when a complete locale ID is formed by composition, the longest match in the language type is used, and the remaining fields (if any) added using composition.

Alternate short forms may be provided for some languages (and for territories and other display names), for example.

```xml
<language type="az">Azerbaijani</language>
<language type="az" alt="short">Azeri</language>
<language type="en_GB">British English</language>
<language type="en_GB" alt="short">U.K. English</language>
<language type="en_US">American English</language>
<language type="en_US" alt="short">U.S. English</language>
```

* * *

```xml
<scripts>
```

This element can contain a number of `script` elements. Each `script` element provides the localized name for a script code, as described in _[Unicode Language and Locale Identifiers](tr35.md#Unicode_Language_and_Locale_Identifiers)_ (see also _UAX #24: Script Names_ [[UAX24](https://www.unicode.org/reports/tr41/#UAX24)]). For example, in the language of this locale, the name for the Latin script might be "Romana", and for the Cyrillic script is "Kyrillica". That would be expressed with the following.

```xml
<script type="Latn">Romana</script>
<script type="Cyrl">Kyrillica</script>
```

The script names are most commonly used in conjunction with a language name, using the `<localePattern>` combining pattern, and the default form of the script name should be suitable for such use. When a script name requires a different form for stand-alone use, this can be specified using the "stand-alone" alternate:

```xml
<script type="Hans">Simplified</script>
<script type="Hans" alt="stand-alone">Simplified Han</script>
<script type="Hant">Traditional</script>
<script type="Hant" alt="stand-alone">Traditional Han</script>
```

This will produce results such as the following:

* Display name of language + script, using `<localePattern>`: “Chinese (Simplified)”
* Display name of script alone, using `<localePattern>`: “Simplified Han”

* * *

```xml
<territories>
```

This contains a list of elements that provide the user-translated names for territory codes, as described in _[Unicode Language and Locale Identifiers](tr35.md#Unicode_Language_and_Locale_Identifiers)_.

```xml
<territory type="AD">Andorra</territory>
<territory type="AF">Afghanistan</territory>
<territory type="AL">Albania</territory>
<territory type="AO">Angola</territory>
<territory type="DZ">Algeria</territory>
<territory type="GB">United Kingdom</territory>
<territory type="GB" alt="short">U.K.</territory>
<territory type="US">United States</territory>
<territory type="US" alt="short">U.S.</territory>
```

Notes:
* Territory names may not match the official name of the territory, and the English or French names may not match those in ISO 3166. Reasons for this include:
    * CLDR favors customary names in common parlance, not necessarily the official names.
    * CLDR endeavors to provide names that are not too long, in order to avoid problems with truncation or overflow in user interfaces.
* In general the territory names should also match those used in currency names, see **Part 3** _[Currencies](tr35-numbers.md#Currencies)_.

* * *

```xml
<variants>
```

This contains a list of elements that provide the user-translated names for the _variant_code_ values described in _[Unicode Language and Locale Identifiers](tr35.md#Unicode_Language_and_Locale_Identifiers)_.

```xml
<variant type="nynorsk">Nynorsk</variant>
```

* * *

```xml
<keys>
```

This contains a list of elements that provide the user-translated names for the _key_ values described in _[Unicode Language and Locale Identifiers](tr35.md#Unicode_Language_and_Locale_Identifiers)_.

```xml
<key type="collation">Sortierung</key>
```

Note that the `type` values may use aliases. Thus if the locale u-extension key "co" does not match, then the aliases have to be tried, using the bcp47 XML data:

```xml
<key name="co" description="…" alias="collation">
```

* * *

```xml
<types>
```

This contains a list of elements that provide the user-translated names for the _type_ values described in _[Unicode Language and Locale Identifiers](tr35.md#Unicode_Language_and_Locale_Identifiers)_. Since the translation of an option name may depend on the _key_ it is used with, the latter is optionally supplied.

```xml
<type type="phonebook" key="collation">Telefonbuch</type>
```

Note that the `key` and `type` values may use aliases. Thus if the locale u-extension key "co" does not match, then the aliases have to be tried, using the bcp47 XML data.

```xml
<key name="co" description="…" alias="collation">

<type name="phonebk" description="…" alias="phonebook"/>
```

* * *

```xml
<measurementSystemNames>
```

This contains a list of elements that provide the user-translated names for systems of measurement. The types currently supported are "US", "metric", and "UK".

```xml
<measurementSystemName type="US">U.S.</type>
```

**Note:** In the future, we may need to add display names for the particular measurement units (millimeter versus millimetre versus whatever the Greek, Russian, etc are), and a message format for positioning those with respect to numbers. For example, "\{number} \{unitName}" in some languages, but "\{unitName} \{number}" in others.

* * *

```xml
<transformNames>
```

```xml
<transformName type="Numeric">Numeric</type>
```

* * *

```xml
<codePatterns>
```

```xml
<codePattern type="language">Language: {0}</type>
```

* * *

```xml
<!ELEMENT subdivisions ( alias | ( subdivision | special )* ) >
<!ELEMENT subdivision ( #PCDATA )>
```

Note that the subdivision names are in separate files, in the subdivisions/ directory. The type values are the fully qualified subdivision names. For example:

```xml
<subdivision type="AL-04">Fier County</subdivision>
<subdivision type="AL-FR">Fier</subdivision> <!-- in AL-04 : Fier County -->
<subdivision type="AL-LU">Lushnjë</subdivision> <!-- in AL-04 : Fier County -->
<subdivision type="AL-MK">Mallakastër</subdivision> <!-- in AL-04 : Fier County -->
```

See also **Part 6** _[Subdivision Containment](tr35-info.md#Subdivision_Containment)_.

## <a name="Layout_Elements" href="#Layout_Elements">Layout Elements</a>

```xml
<!ELEMENT layout ( alias | (orientation*, inList*, inText*, special*) ) >
```

This top-level element specifies general layout features. It currently only has one possible element (other than `<special>`, which is always permitted).

```xml
<!ELEMENT orientation ( characterOrder*, lineOrder*, special* ) >
<!ELEMENT characterOrder ( #PCDATA ) >
<!ELEMENT lineOrder ( #PCDATA ) >
```

The `lineOrder` and `characterOrder` elements specify the default general ordering of lines within a page, and characters within a line. The possible values are:

<!-- HTML: rowspan -->
<table><tbody>
<tr><th>Direction</th><th>Value</th></tr>
<tr><td rowspan="2">Vertical</td><td>top-to-bottom</td></tr>
<tr>                             <td>bottom-to-top</td></tr>
<tr><td rowspan="2">Horizontal</td><td>left-to-right</td></tr>
<tr>                               <td>right-to-left</td></tr>
</tbody></table>

If the value of lineOrder is one of the vertical values, then the value of characterOrder must be one of the horizontal values, and vice versa. For example, for English the lines are top-to-bottom, and the characters are left-to-right. For Mongolian (in the Mongolian Script) the lines are right-to-left, and the characters are top to bottom. This does not override the ordering behavior of bidirectional text; it does, however, supply the paragraph direction for that text (for more information, see _UAX #9: The Bidirectional Algorithm_ [[UAX9](https://www.unicode.org/reports/tr41/#UAX9)]).

For dates, times, and other data to appear in the right order, the display for them should be set to the orientation of the locale.

* * *

```xml
<inList> (deprecated)
```

The `<inList>` element is deprecated and has been superseded by the `<contextTransforms>` element; see _[ContextTransform Elements](#Context_Transform_Elements)_.

This element controls whether display names (language, territory, etc) are title cased in GUI menu lists and the like. It is only used in languages where the normal display is lower case, but title case is used in lists. There are two options:

```xml
<inList casing="titlecase-words">

<inList casing="titlecase-firstword">
```

In both cases, the title case operation is the default title case function defined by Chapter 3 of _[[Unicode](tr35.md#Unicode)]_. In the second case, only the first word (using the word boundaries for that locale) will be title cased. The results can be fine-tuned by using alt="list" on any element where titlecasing as defined by the Unicode Standard will produce the wrong value. For example, suppose that "turc de Crimée" is a value, and the title case should be "Turc de Crimée". Then that can be expressed using the alt="list" value.

* * *

```xml
<inText> (deprecated)
```

The `<inList>` element is deprecated and has been superseded by the `<contextTransforms>` element; see _[ContextTransform Elements](#Context_Transform_Elements)_.

This element indicates the casing of the data in the category identified by the `inText` `type` attribute, when that data is written in text or how it would appear in a dictionary. For example:

```xml
<inText type="languages">lowercase-words</inText>
```

indicates that language names embedded in text are normally written in lower case. The possible values and their meanings are :

*   titlecase-words : all words in the phrase should be title case
*   titlecase-firstword : the first word should be title case
*   lowercase-words : all words in the phrase should be lower case
*   mixed : a mixture of upper and lower case is permitted, generally used when the correct value is unknown

## <a name="Character_Elements" href="#Character_Elements">Character Elements</a>

```xml
<!ELEMENT characters ( alias | ( exemplarCharacters*, ellipsis*, moreInformation*, stopwords*, indexLabels*, mapping*, parseLenients*, special* ) ) >
```

The `<characters>` element provides optional information about characters that are in common use in the locale, and information that can be helpful in picking resources or data appropriate for the locale, such as when choosing among character encodings that are typically used to transmit data in the language of the locale. It may also be used to help reduce confusability issues: see [[UTR39](https://www.unicode.org/reports/tr41/#UTR36)]. It typically only occurs in a language locale, not in a language/territory locale. The stopwords are an experimental feature, and should not be used.

### <a name="Exemplars" href="#Exemplars">Exemplars</a>

Exemplars are characters used by a language, separated into different categories. The following table provides a summary, with more details below.

| Type            | Description | Examples |
| --------------- | ----------- | -------- |
| main / standard | Main letters used in the language | a-z å æ ø |
| auxiliary       | Additional characters for common foreign words, technical usage | á à ă â å ä ã ā æ ç é è ĕ ê ë ē í ì ĭ î ï ī ñ ó ò ŏ ô ö ø ō œ ú ù ŭ û ü ū ÿ |
| index           | Characters for the header of an index | A B C D E F G H I J K L M N O P Q R S T U V W X Y Z |
| punctuation     | Common punctuation | - ‐ – — , ; \\: ! ? . … “ ” ‘ ’ ( ) [ ] § @ * / & # † ‡ ′ ″ |
| numbers         | The characters needed to display the common number formats: decimal, percent, and currency. | \[\\u061C\\u200E \\- , ٫ ٬ . % ٪ ‰ ؉ + 0٠ 1١ 2٢ 3٣ 4٤ 5٥ 6٦ 7٧ 8٨ 9٩\] |

The basic exemplar character sets (main and auxiliary) contain the commonly used letters for a given modern form of a language, which can be for testing and for determining the appropriate repertoire of letters for charset conversion or collation. ("Letter" is interpreted broadly, as anything having the property Alphabetic in the [[UAX44](https://www.unicode.org/reports/tr41/#UAX44)], which also includes syllabaries and ideographs.) It is not a complete set of letters used for a language, nor should it be considered to apply to multiple languages in a particular country. Punctuation and other symbols should not be included in the main and auxiliary sets. In particular, format characters like CGJ are not included.

There are five sets altogether: main, auxiliary, punctuation, numbers, and index. The _main_ set should contain the minimal set required for users of the language, while the _auxiliary_ exemplar set is designed to encompass additional characters: those non-native or historical characters that would customarily occur in common publications, dictionaries, and so on. Major style guidelines are good references for the auxiliary set. So, for example, if Irish newspapers and magazines would commonly have Danish names using å, for example, then it would be appropriate to include å in the auxiliary exemplar characters; just not in the main exemplar set. Thus English has the following:

```xml
<exemplarCharacters>[a b c d e f g h i j k l m n o p q r s t u v w x y z]</exemplarCharacters>
<exemplarCharacters type="auxiliary">[á à ă â å ä ã ā æ ç é è ĕ ê ë ē í ì ĭ î ï ī ñ ó ò ŏ ô ö ø ō œ ú ù ŭ û ü ū ÿ]</exemplarCharacters>
```

For a given language, there are a few factors that help for determining whether a character belongs in the auxiliary set, instead of the main set:

*   The character is not available on all normal keyboards.
*   It is acceptable to always use spellings that avoid that character.

For example, the exemplar character set for en (English) is the set \[a-z\]. This set does not contain the accented letters that are sometimes seen in words like "résumé" or "naïve", because it is acceptable in common practice to spell those words without the accents. The exemplar character set for fr (French), on the other hand, must contain those characters: \[a-z é è ù ç à â ê î ô û æ œ ë ï ÿ\]. The main set typically includes those letters commonly "alphabet".

The _punctuation_ set consists of common punctuation characters that are used with the language (corresponding to main and auxiliary). Symbols may also be included where they are common in plain text, such as ©. It does not include characters with narrow technical usage, such as dictionary punctuation/symbols or copy-edit symbols. For example, English would have something like the following:

> - ‐ – —
> , ; : ! ? . …
> ' ‘ ’ " “ ” ′ ″
> ( ) [ \] { } ⟨ ⟩
> © ® ™ @ & ° ‧ ·/ # % ¶ § * † ‡
> + − ± × ÷ < ≤ = ≅ ≥ > √

The numbers exemplars do not currently include lesser-used characters: exponential notation (3.1 × 10²³, ∞, NaN). Nor does it contain the units or currency symbols such as $, ¥, ₹, … It does contain %, because that occurs in the percent format. It may contain some special formatting characters like the RLM. A full list of the currency symbols used with that locale are in the `<currencies>` element, while the units can be gotten from the `<units>` element (both using inheritance, of course).The digits used in each numbering system are accessed in numberingSystems.xml. For more information, see _**Part 3: [Numbers](tr35-numbers.md#Contents)**, [Number Elements](tr35-numbers.md#Number_Elements)_.

_Examples for zh.xml:_

| Type                              | Description |
| --------------------------------- | ----------- |
| defaultNumberingSystem            | latn        |
| otherNumberingSystems/native      | hanidec     |
| otherNumberingSystems/traditional | hans        |
| otherNumberingSystems/finance     | hansfin     |

When determining the character repertoire needed to support a language, a reasonable initial set would include at least the characters in the main and punctuation exemplar sets, along with the digits and common symbols associated with the numberSystems supported for the locale (see _[Numbering Systems](tr35-numbers.md#Numbering_Systems)_).

The _index_ characters are a set of characters for use as a UI "index", that is, a list of clickable characters (or character sequences) that allow the user to see a segment of a larger "target" list. For details see the [Unicode LDML: Collation](tr35-collation.md#Collation_Indexes) document. The index set may only contain characters whose lowercase versions are in the main and auxiliary exemplar sets, though for cased languages the index exemplars are typically in uppercase. Characters from the auxiliary exemplar set may be necessary in the index set if it needs to properly handle items such as names which may require characters not included in the main exemplar set.

Here is a sample of the XML structure:

```xml
<exemplarCharacters type="index">[A B C D E F G H I J K L M N O P Q R S T U V W X Y Z]</exemplarCharacters>
```

The display of the index characters can be modified with the `indexLabel`s elements, discussed in Section 3.3.

#### <a name="ExemplarSyntax" href="#ExemplarSyntax">Exemplar Syntax</a>

In all of the exemplar characters, the list of characters is in the [Unicode Set](tr35.md#Unicode_Sets) format, which normally allows boolean combinations of sets of letters and Unicode properties.

Sequences of characters that act like a single letter in the language — especially in collation — are included within braces, such as `[a-z á é í ó ú ö ü ő ű {cs} {dz} {dzs} {gy} ...]`. The characters should be in normalized form (NFC). Where combining marks are used generatively, and apply to a large number of base characters (such as in Indic scripts), the individual combining marks should be included. Where they are used with only a few base characters, the specific combinations should be included. Wherever there is not a precomposed character (for example, single codepoint) for a given combination, that must be included within braces. For example, to include sequences from the [Where is my Character?](https://www.unicode.org/standard/where/) page on the Unicode site, one would write: `[{ch} {tʰ} {x̣} {ƛ̓} {ą́} {i̇́} {ト゚}]`, but for French one would just write `[a-z é è ù ...]`. When in doubt use braces, since it does no harm to include them around single code points: for example, `[a-z {é} {è} {ù} ...]`.

If the letter 'z' were only ever used in the combination 'tz', then we might have `[a-y {tz}]` in the main set. (The language would probably have plain 'z' in the auxiliary set, for use in foreign words.) If combining characters can be used productively in combination with a large number of others (such as say Indic matras), then they are not listed in all the possible combinations, but separately, such as:

```
[ॐ ऄ-ऋ ॠ ऌ ॡ ऍ-क क़ ख ख़ ग ग़ घ-ज ज़ झ-ड ड़ ढ ढ़ ण-फ फ़ ब-य य़ र-ह ़ ँ-ः ॑-॔ ऽ ् ॽ ा-ॄ ॢ ॣ ॅ-ौ]
```

The exemplar character set for Han characters is composed somewhat differently. It is even harder to draw a clear line for Han characters, since usage is more like a frequency curve that slowly trails off to the right in terms of decreasing frequency. So for this case, the exemplar characters simply contain a set of reasonably frequent characters for the language.

The ordering of the characters in the set is irrelevant, but for readability in the XML file the characters should be in sorted order according to the locale's conventions. The main and auxiliary sets should only contain lower case characters (except for the special case of Turkish and similar languages, where the dotted capital I should be included); the upper case letters are to be mechanically added when the set is used. For more information on casing, see the discussion of Special Casing in the Unicode Character Database.

#### <a name="Restrictions" href="#Restrictions">Restrictions</a>

1.  The main, auxiliary and index sets are normally restricted to those letters with a specific [Script](https://www.unicode.org/Public/UNIDATA/Scripts.txt) character property (that is, not the values Common or Inherited) or required [Default_Ignorable_Code_Point](https://www.unicode.org/Public/UNIDATA/DerivedCoreProperties.txt) characters (such as a non-joiner), or combining marks, or the [Word_Break](https://www.unicode.org/Public/UNIDATA/auxiliary/WordBreakProperty.txt) properties [Katakana](https://www.unicode.org/reports/tr29/#Katakana), [ALetter](https://www.unicode.org/reports/tr29/#ALetter), or [MidLetter](https://www.unicode.org/reports/tr29/#MidLetter).
2.  The auxiliary set should not overlap with the main set. There is one exception to this: Hangul Syllables and CJK Ideographs can overlap between the sets.
3.  Any [Default_Ignorable_Code_Point](https://www.unicode.org/Public/UNIDATA/DerivedCoreProperties.txt)s should be in the auxiliary set, or, if they are only needed for currency formatting, in the currency set. These can include characters such as U+200E LEFT-TO-RIGHT MARK and U+200F RIGHT-TO-LEFT MARK which may be needed in bidirectional text in order for date, currency or other formats to display correctly.
4.  For exemplar characters the [Unicode Set](tr35.md#Unicode_Sets) format is restricted so as to not use properties or boolean combinations.

### ~~<a name="Character_Mapping" href="#Character_Mapping">Mapping</a>~~

**This element has been deprecated.** For information on its structure and how it was intended to specify locale-specific preferred encodings for various purposes (e-mail, web), see the [Mapping](https://www.unicode.org/reports/tr35/tr35-39/tr35-general.html#Character_Mapping) section from the CLDR 27 version of the LDML Specification.

### ~~<a name="IndexLabels" href="#IndexLabels">Index Labels</a>~~

**This element and its subelements have been deprecated.** For information on its structure and how it was intended to provide data for a compressed display of index exemplar characters where space is limited, see the [Index Labels](https://www.unicode.org/reports/tr35/tr35-39/tr35-general.html#IndexLabels) section from the CLDR 27 version of the LDML Specification.

```xml
<!ELEMENT indexLabels (indexSeparator*, compressedIndexSeparator*, indexRangePattern*, indexLabelBefore*, indexLabelAfter*, indexLabel*) >
```

### <a name="Ellipsis" href="#Ellipsis">Ellipsis</a>

```xml
<!ELEMENT ellipsis ( #PCDATA ) >
<!ATTLIST ellipsis type ( initial | medial | final | word-initial | word-medial | word-final ) #IMPLIED >
```

The `ellipsis` element provides patterns for use when truncating strings. There are three versions: initial for removing an initial part of the string (leaving final characters); medial for removing from the center of the string (leaving initial and final characters), and final for removing a final part of the string (leaving initial characters). For example, the following uses the ellipsis character in all three cases (although some languages may have different characters for different positions).

```xml
<ellipsis type="initial">…{0}</ellipsis>
<ellipsis type="medial">{0}…{1}</ellipsis>
<ellipsis type="final">{0}…</ellipsis>
```

There are alternatives for cases where the breaks are on a word boundary, where some languages include a space. For example, such a case would be:

```xml
<ellipsis type="word-initial">… {0}</ellipsis>
```

### <a name="Character_More_Info" href="#Character_More_Info">More Information</a>

The moreInformation string is one that can be displayed in an interface to indicate that more information is available. For example:

```xml
<moreInformation>?</moreInformation>
```

### <a name="Character_Parse_Lenient" href="#Character_Parse_Lenient">Parse Lenient</a>

```xml
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

The `parseLenient` elements are used to indicate that characters within a particular UnicodeSet are normally to be treated as equivalent when doing a lenient parse. The `scope` attribute value defines where the lenient sets are intended for use. The `level` attribute value is included for future expansion; currently the only value is "lenient".

The `sample` attribute value is a paradigm element of that UnicodeSet, but the only reason for pulling it out separately is so that different classes of characters are separated, and to enable inheritance overriding. The first version of this data is populated with the data used for lenient parsing from ICU.

## <a name="Delimiter_Elements" href="#Delimiter_Elements">Delimiter Elements</a>

```xml
<!ELEMENT delimiters (alias | (quotationStart*, quotationEnd*, alternateQuotationStart*, alternateQuotationEnd*, special*)) >
```

The delimiters supply common delimiters for bracketing quotations. The quotation marks are used with simple quoted text, such as:

> He said, “Don’t be absurd!”

When quotations are nested, the quotation marks and alternate marks are used in an alternating fashion:

> He said, “Remember what the Mad Hatter said: ‘Not the same thing a bit! Why you might just as well say that “I see what I eat” is the same thing as “I eat what I see”!’”

```xml
<quotationStart>“</quotationStart>
<quotationEnd>”</quotationEnd>
<alternateQuotationStart>‘</alternateQuotationStart>
<alternateQuotationEnd>’</alternateQuotationEnd>
```

### <a name="Tailor_Linebreak_With_Delimiters" href="#Tailor_Linebreak_With_Delimiters">Tailoring Linebreak Using Delimiters</a>

The delimiter data can be used for language-specific tailoring of linebreak behavior, as suggested
in the [description of linebreak class QU: Quotation](https://www.unicode.org/reports/tr14/#QU)
in [[UAX14](https://www.unicode.org/reports/tr41/#UAX14)]. This is an example of
[tailoring type](https://www.unicode.org/reports/tr14/#Tailoring) 1 (from that same document),
changing the line breaking class assignment for some characters.

If the values of `<quotationStart>` and `<quotationEnd>` are different, then:
* if the value of `<quotationStart>` is a single character with linebreak class QU: Quotation, change its class to OP: Open Punctuation.
* if the value of `<quotationEnd>` is a single character with linebreak class QU: Quotation, change its class to CL: Close Punctuation.
Similarly for `<alternateQuotationStart>` and `<alternateQuotationEnd>`.

Some characters with multiple uses should generally be excluded from this linebreak class remapping, such as:
* U+2019 RIGHT SINGLE QUOTATION MARK, often used as apostrophe, should not be changed from QU; otherwise it will introduce breaks after apostrophe.
* Several locales (mostly for central and eastern Europe) have U+201C LEFT DOUBLE QUOTATION MARK as `<quotationEnd>` or `<alternateQuotationEnd>`. However users in these locales may also encounter English text in which U+201C is used as `<quotationStart>`. In order to prevent improper breaks for English text, in these locales U+201C should not be changed from QU.

## <a name="Measurement_System_Data" href="#Measurement_System_Data">Measurement System Data</a>

```xml
<!ELEMENT measurementData ( measurementSystem*, paperSize* ) >

<!ELEMENT measurementSystem EMPTY >
<!ATTLIST measurementSystem type ( metric | US | UK ) #REQUIRED >
<!ATTLIST measurementSystem category ( temperature ) #IMPLIED >
<!ATTLIST measurementSystem territories NMTOKENS #REQUIRED >

<!ELEMENT paperSize EMPTY >
<!ATTLIST paperSize type ( A4 | US-Letter ) #REQUIRED >
<!ATTLIST paperSize territories NMTOKENS #REQUIRED >
```

The measurement system is the normal measurement system in common everyday use (except for date/time). For example:

```xml
<measurementData>
 <measurementSystem type="metric" territories="001" />
 <measurementSystem type="US" territories="LR MM US" />
 <measurementSystem type="metric" category="temperature" territories="LR MM" />
 <measurementSystem type="US" category="temperature" territories="BS BZ KY PR PW" />
 <measurementSystem type="UK" territories="GB" />
 <paperSize type="A4" territories="001" />
 <paperSize type="US-Letter" territories="BZ CA CL CO CR GT MX NI PA PH PR SV US VE" />
</measurementData>
```

The values are "metric", "US", or "UK"; others may be added over time.

* The "metric" value indicates the use of SI [[ISO1000](tr35.md#ISO1000)] base or derived units, or non-SI units accepted for use with SI: for example, meters, kilograms, liters, and degrees Celsius.
* The "US" value indicates the customary system of measurement as used in the United States: feet, inches, pints, quarts, degrees Fahrenheit, and so on.
* The "UK" value indicates the mix of metric units and Imperial units (feet, inches, pints, quarts, and so on) used in the United Kingdom, in which Imperial volume units such as pint, quart, and gallon are different sizes than in the "US" customary system. For more detail about specific units for various usages, see **Part 6: Supplemental:** _[Preferred Units for Specific Usages](tr35-info.md#Preferred_Units_For_Usage)_.

In some cases, it may be common to use different measurement systems for different categories of measurements. For example, the following indicates that for the category of temperature, in the regions LR and MM, it is more common to use metric units than US units.

```xml
<measurementSystem type="metric" category="temperature" territories="LR MM"/>
```

The `paperSize` attribute gives the height and width of paper used for normal business letters. The values are "A4" and "US-Letter".

For both `measurementSystem` entries and `paperSize` entries, later entries for specific territories such as "US" will override the value assigned to that territory by earlier entries for more inclusive territories such as "001".

The measurement information was formerly in the main LDML file, and had a somewhat different format.

Again, for finer-grained detail about specific units for various usages, see **Part 6: Supplemental:** _[Preferred Units for Specific Usages](tr35-info.md#Preferred_Units_For_Usage)_.

### <a name="Measurement_Elements" href="#Measurement_Elements">Measurement Elements (deprecated)</a>

```xml
<!ELEMENT measurement (alias | (measurementSystem?, paperSize?, special*)) >
```

The `measurement` element is deprecated in the main LDML files, because the data is more appropriately organized as connected to territories, not to linguistic data. Instead, the `measurementData` element in the supplemental data file should be used.

## <a name="Unit_Elements" href="#Unit_Elements">Unit Elements</a>

```xml
<!ELEMENT units (alias | (unit*, unitLength*, durationUnit*, special*) ) >

<!ELEMENT unitIdComponents ( unitIdComponent* ) >

<!ELEMENT unitLength (alias | (compoundUnit*, unit*, coordinateUnit*, special*) ) >
<!ATTLIST unitLength type (long | short | narrow) #REQUIRED >

<!ELEMENT compoundUnit (alias | (compoundUnitPattern*, special*) ) >
<!ATTLIST compoundUnit type NMTOKEN #REQUIRED >

<!ELEMENT unit ( alias | ( gender*, displayName*, unitPattern*, perUnitPattern*, special* ) ) >
<!ATTLIST unit type NMTOKEN #REQUIRED >

<!ELEMENT gender ( #PCDATA )>

<!ELEMENT durationUnit (alias | (durationUnitPattern*, special*) ) >
<!ATTLIST durationUnit type NMTOKEN #REQUIRED >

<!ELEMENT unitPattern ( #PCDATA ) >
<!ATTLIST unitPattern count (0 | 1 | zero | one | two | few | many | other) #REQUIRED >

<!ELEMENT compoundUnitPattern ( #PCDATA ) >
<!ATTLIST compoundUnitPattern case NMTOKENS #IMPLIED >

<!ELEMENT compoundUnitPattern1 ( #PCDATA ) >
<!ATTLIST compoundUnitPattern1 count (0 | 1 | zero | one | two | few | many | other) #IMPLIED >
<!ATTLIST compoundUnitPattern1 gender NMTOKENS #IMPLIED >
<!ATTL IST compoundUnitPattern1 case NMTOKENS #IMPLIED >

<!ELEMENT coordinateUnit ( alias | ( displayName*, coordinateUnitPattern*, special* ) ) >
<!ELEMENT coordinateUnitPattern ( #PCDATA ) >
<!ATTLIST coordinateUnitPattern type (north | east | south | west) #REQUIRED >

<!ELEMENT durationUnitPattern ( #PCDATA ) >
```

These elements specify the localized way of formatting quantities of units such as years, months, days, hours, minutes and seconds— for example, in English, "1 day" or "3 days". The English rules that produce this example are as follows ({0} indicates the position of the formatted numeric value):

```xml
<unit type="duration-day">
  <displayName>days</displayName>
  <unitPattern count="one">{0} day</unitName>
  <unitPattern count="other">{0} days</unitName>
</unit>
```

The German rules are more complicated, because German has both gender and case. They thus have additional information, as illustrated below. Note that if there is no `@case` attribute, for backwards compatibility the implied case is nominative. The possible values for @case are listed in the `grammaticalFeatures` element. These follow the inheritance specified in Part 1, Section Lateral Inheritance](tr35.md#Lateral_Inheritance). Note that the additional grammar elements are only present in the `<unitLength type='long'>` form.

```xml
<unit type="duration-day">
    <gender>masculine</gender>
    <displayName>Tage</displayName>
    <unitPattern count="one">{0} Tag</unitPattern>
    <unitPattern count="one" case="accusative">{0} Tag</unitPattern>
    <unitPattern count="one" case="dative">{0} Tag</unitPattern>
    <unitPattern count="one" case="genitive">{0} Tages</unitPattern>
    <unitPattern count="other">{0} Tage</unitPattern>
    <unitPattern count="other" case="accusative">{0} Tage</unitPattern>
    <unitPattern count="other" case="dative">{0} Tagen</unitPattern>
    <unitPattern count="other" case="genitive">{0} Tage</unitPattern>
    <perUnitPattern>{0} pro Tag</perUnitPattern>
</unit>
```

These follow the inheritance specified in Part 1, Section Lateral Inheritance](tr35.md#Lateral_Inheritance). In addition to supporting language-specific plural cases such as “one” and “other”, unitPatterns support the language-independent explicit cases “0” and “1” for special handling of numeric values that are exactly 0 or 1; see [Explicit 0 and 1 rules](tr35-numbers.md#Explicit_0_1_rules).

The `<unitPattern>` elements may be used to format quantities with decimal values; in such cases the choice of plural form will depend not only on the numeric value, but also on its formatting (see [Language Plural Rules](tr35-numbers.md#Language_Plural_Rules)). In addition to formatting units for stand-alone use, `<unitPattern>` elements are increasingly being used to format units for use in running text; for such usages, the developing [Grammatical Features](#Grammatical_Features) information will be very useful.

Note that for certain plural cases, the unit pattern may not provide for inclusion of a numeric value—that is, it may not include “{0}”. This is especially true for the explicit cases “0” and “1” (which may have patterns like “zero seconds”). In certain languages such as Arabic and Hebrew, this may also be true with certain units for the plural cases “zero”, “one”, or “two” (in these languages, such plural cases are only used for the corresponding exact numeric values, so there is no concern about loss of precision without the numeric value).

Units, like other values with a `count` attribute, use a special inheritance. See **Part 1: Core:** _[Multiple Inheritance](tr35.md#Multiple_Inheritance)_.

The displayName is used for labels, such as in a UI. It is typically lowercased and as neutral a plural form as possible, and then uses the casing context for the proper display. For example, for English in a UI it would appear as titlecase:

**Duration:**

<!-- HTML: UI drawing -->
<table><tbody>
<tr><td>Days</td><td style="color: silver;">enter the vacation length</td></tr>
</tbody></table>


### <a name="Unit_Preference_and_Conversion" href="#Unit_Preference_and_Conversion">Unit Preference and Conversion Data</a>

Different locales have different preferences for which unit or combination of units is used for a particular usage, such as measuring a person’s height. This is more fine-grained than merely a preference for metric versus US or UK measurement systems. For example, one locale may use meters alone, while another may use centimeters alone or a combination of meters and centimeters; a third may use inches alone, or (informally) a combination of feet and inches.

The unit preference and conversion data allows formatting functions to pick the right measurement units for the locale and usage, and convert input measurement into those units. For example, a program (or database) could use 1.88 meters internally, but then for person-height have that measurement convert to _6 foot 2 inches_ for en-US and to _188 centimeters_ for de-CH. Using the unit display names and list formats, those results can then be displayed according to the desired width (eg _2″_ vs _2 in_ vs 2 _inches_) and using the locale display names and number formats.

The size of the measurement can also be taken into account, so that an infant can have a height as _18 inches_, and an adult the height as _6 foot 2 inches._

This data is supplied in **Part 6: [Supplemental](tr35-info.md#Contents)**: [Unit Conversion](tr35-info.md#Unit_Conversion) and [Unit Preferences](tr35-info.md#Unit_Preferences).

### <a name="Unit_Identifiers" href="#Unit_Identifiers">Unit Identifiers</a>

Units of measurement, such as _meter_, have defined programmatic identifiers as described in this section.
The main identifier is a _core unit identifier_, which encompasses a number of simpler types of identifiers as follows.
A secondary type of identifier is a _mixed unit identifier_, which combines a series of units such as _5° 30′_ or _3 feet 7 inches_.

| Name             | Examples |
| ---------------- | -------- |
| core unit ID     | kilometer-per-hour, kilogram-meter, kilogram-meter-per-square-second, … <br/> _plus single unit IDs_ |
| single unit ID   | square-foot, cubic-centimeter, … <br/> _plus prefixed unit IDs_ |
| prefixed unit ID | kilometer, centigram, … <br/> _plus simple unit IDs_ |
| simple unit ID   | meter, foot, inch, pound, pound-force, … |
| mixed unit ID    | foot-and-inch, degree-and-arc-minute-and-arc-second |


There is currently a ‘long’ style of unit identifier corresponding to each _core unit identifier_, as illustrated below.
The only difference is that the long unit identifier adds a prefix which was used in the CLDR Survey Tool for grouping related identifiers together.
The long unit identifers are used as a key in the translated unit names for locales, but dealing with these two styles is unnecessarily complicated, so the long unit identifiers are slated for deprecation (after replacing their use as a key for translations).

| core unit ID | long unit ID |
| ------------ | ------------ |
| meter        | length-meter |
| pound        | mass-pound   |
| day          | duration-day |


The list of valid CLDR simple unit identifiers is found in _[Section Validity Data](tr35.md#Validity_Data)_.
These names should not be presented to end users, however: the translated names for different languages (or variants of English) are available in the CLDR localized data.
All syntactically valid CLDR unit identifiers values that are not listed in the validity data are reserved by CLDR for additional future units.
There is one exception: implementations that need to define their own unit identifiers can do so via _[Private-Use Units](#Private_Use_Units)_.

A core unit identifier that is not a simple unit is called a _complex unit_ (aka _compound unit_).
A complex unit identifier can be constructed from simple unit identifiers using multiplication (kilogram-meter) and division (kilogram-per-meter), powers (square-second), and prefixes (kilo-, 100-, kiBi).
As usual, with division the part before the (first) -per- is called the _numerator_, and the part after it is called the _denominator_.

The identifiers and unit conversion data are built to handle core unit IDs and mixed unit IDs based on their simple unit identifiers.
Thus they support converting generated units such as inch-pound-per-square-week into comparable units, such as newtons.

Where a core unit ID or mixed unit ID does not have an explicit translation in CLDR, a mechanism is supplied for producing a generated translation from the translations for the simple unit identifiers.
See _[Compound Units](#compound-units)_.
That can be used for less common units, such as _petasecond_.
However, the generated translations may have the wrong spelling in languages where orthographic changes are needed when combining words.
For example, “kilometer” can be formed in English from “kilo” and “meter”; the same process in Greek would combine “χιλιο” and “μέτρα” to get “χιλιομέτρα” — when the correct result is “χιλιόμετρα” (note the different location of the accent).
Thus the most commonly-used complex units have explicit translations in CLDR.

* A power (square, cubic, pow4, etc) modifies one prefixed unit ID, and must occur immediately before it in the identifier: square-foot, not foot-square.
* Multiplication binds more tightly than division, so kilogram-meter-per-second-ampere is interpreted as (kg ⋅ m) / (s ⋅ a).
* Thus if -per- occurs multiple times, each occurrence after the first is equivalent to a multiplication:
  * kilogram-meter-per-second-ampere ⩧ kilogram-meter-per-second-per-ampere.

#### Nomenclature

As with other identifiers in CLDR, the American English spelling is used for unit identifiers. For the US spelling, see the [Preface of the Guide for the Use of the International System of Units (SI), NIST special publication 811](https://www.nist.gov/pml/special-publication-811), which is explicit about the discrepancy with the English-language BIPM spellings:

> In keeping with U.S. and International practice (see Sec. C.2), this Guide uses the dot on the line as the decimal marker. In addition this Guide utilizes the American spellings “meter,” “liter,” and “deka” rather than “metre,” “litre,” and “deca,” and the name “metric ton” rather than “tonne.”

#### Syntax

The formal syntax for identifiers is provided below.
Some of the constraints reference data from the unitIdComponents in [Unit_Conversion](tr35-info.md#Unit_Conversion).

<!-- HTML: no header -->

<table><tbody>
<tr><td>unit_identifier</td><td>:=</td>
    <td>core_unit_identifier<br/>
        | mixed_unit_identifier<br/>
        | long_unit_identifier</td></tr>

<tr><td>core_unit_identifier</td><td>:=</td>
    <td>product_unit ("-" per "-" product_unit)*<br/>
        | per "-" product_unit ("-" per "-" product_unit)*
        <ul><li><em>Examples:</em>
            <ul><li>foot-per-second-per-second</li>
                <li>per-second</li>
            </ul></li>
            <li><em>Note:</em> The normalized form will have only one "per"</li>
        </ul></td></tr>

<tr><td>per</td><td>:=</td>
    <td>"per"
        <ul>
			<li><em>Constraint:</em> The token 'per' is the single value in &lt;unitIdComponent type="per"&gt;</li>
		</ul></td></tr>

<tr><td>product_unit</td><td>:=</td>
        <td>single_unit ("-" single_unit)* ("-" pu_single_unit)*<br/>
            | pu_single_unit ("-" pu_single_unit)*
            <ul><li><em>Example:</em> foot-pound-force</li>
                <li><em>Constraint:</em> No pu_single_unit may precede a single unit</li>
            </ul></td></tr>

<tr><td>single_unit</td><td>:=</td>
    <td>number_prefix? dimensionality_prefix? simple_unit
        <ul><li><em>Examples: </em>square-meter, or 100-square-meter</li></ul></td></tr>

<tr><td>pu_single_unit</td><td>:=</td>
    <td>"xxx-" single_unit | "x-" single_unit
    <ul><li><em>Example:</em> xxx-square-knuts (a Harry Potter unit)</li>
        <li><em>Note:</em> "x-" is only for backwards compatibility</li>
        <li>See <a href="#Private_Use_Units">Private-Use Units</a></li>
    </ul></td></tr>

<tr><td>number_prefix</td><td>:=</td>
    <td>("1"[0-9]+ | [2-9][0-9]*) "-"
        <ul><li><em>Examples:</em>
            <ul><li>kilowatt-hour-per-100-kilometer</li>
                <li>gallon-per-100-mile</li>
                <li>per-200-pound</li>
            </ul></li>
            <li><em>Note:</em> The number is an integer greater than one.</li>
        </ul></td></tr>

<tr><td>dimensionality_prefix</td><td>:=</td>
    <td>"square-"<p>| "cubic-"<p>| "pow" ([2-9]|1[0-5]) "-"
        <ul>
			<li><em>Constraint:</em> must be value in: &lt;unitIdComponent type="power"&gt;.</li>
			<li><em>Note:</em> "pow2-" and "pow3-" canonicalize to "square-" and "cubic-"</li>
			<li><em>Note:</em> These are values in &lt;unitIdComponent type="power"&gt;</li>
		</ul></td></tr>

<tr><td>simple_unit</td><td>:=</td>
    <td>(prefix_component "-")* (prefixed_unit | base_component) ("-" suffix_component)*<br/>
		|  currency_unit<br/>
		| "em" | "g" | "us" | "hg" | "of"
        <ul>
		<li><em>Examples:</em> kilometer, meter, cup-metric, fluid-ounce, curr-chf, em</li>
		<li><em>Note:</em> Three simple units are currently allowed as legacy usage, for tokens that wouldn’t otherwise be a base_component due to length (eg, "<strong>g</strong>-force").
			We will likely deprecate those and add conformant aliases in the future: the "hg" and "of" are already only in deprecated simple_units.</li>
        </ul></td></tr>

<tr><td>prefixed_unit</td><td></td>
    <td>prefix base_component<ul><li><em>Example: </em>kilometer</li></ul></td></tr>

<tr><td>prefix</td><td></td>
    <td>si_prefix | binary_prefix</td></tr>

<tr><td>si_prefix</td><td>:=</td>
    <td>"deka" | "hecto" | "kilo", …
        <ul><li><em>Note:</em> See full list at <a href="https://www.nist.gov/pml/special-publication-811">NIST special publication 811</a></li></ul></td></tr>

<tr><td>binary_prefix</td><td>:=</td>
    <td>"kibi", "mebi", …
        <ul><li><em>Note:</em> See full list at <a href="https://physics.nist.gov/cuu/Units/binary.html">Prefixes for binary multiples</a></li></ul></td></tr>

<tr><td>prefix_component</td><td>:=</td>
    <td>[a-z]{3,∞}
        <ul><li><em>Constraint:</em> must be value in: &lt;unitIdComponent type="prefix"&gt;.</li></ul></td></tr>

<tr><td>base_component</td><td>:=</td>
    <td>[a-z]{3,∞}
        <ul><li><em>Constraint:</em> must not be a value in any of the following:<br>
			&lt;unitIdComponent type="prefix"&gt;<br>
			or &lt;unitIdComponent type="suffix"&gt; <br>
			or &lt;unitIdComponent type="power"&gt;<br>
			or &lt;unitIdComponent type="and"&gt;<br>
			or &lt;unitIdComponent type="per"&gt;.
		</li>
		<li><em>Constraint:</em> must not have a prefix as an initial segment.</li>
		<li><em>Constraint:</em> no two different base_components will share the first 8 letters.
				(<b>For more information, see <a href="#Unit_Identifier_Uniqueness">Unit Identifier Uniqueness</a>.)</b>
			</li>
		</ul>
	</td></tr>

<tr><td>suffix_component</td><td>:=</td>
    <td>[a-z]{3,∞}
        <ul>
			<li><em>Constraint:</em> must be value in: &lt;unitIdComponent type="suffix"&gt;</li>
		</ul></td></tr>

<tr><td>mixed_unit_identifier</td><td>:=</td>
    <td>(single_unit | pu_single_unit) ("-" and "-" (single_unit | pu_single_unit ))*
        <ul><li><em>Example: foot-and-inch</em></li>
		</ul></td></tr>

<tr><td>and</td><td>:=</td>
    <td>"and"
		<ul>
			<li><em>Constraint:</em> The token 'and' is the single value in &lt;unitIdComponent type="and"&gt;</li>
		</ul></td></tr>

<tr><td>long_unit_identifier</td><td>:=</td>
    <td>grouping "-" core_unit_identifier</td></tr>

<tr><td>grouping</td><td>:=</td>
    <td>[a-z]{3,∞}</td></tr>

<tr><td>currency_unit</td><td>:=</td>
    <td>"curr-" [a-z]{3}
        <ul>
			<li><em>Constraint:</em> The first part of the currency_unit is a standard prefix; the second part of the currency unit must be a valid <a href="tr35.md#UnicodeCurrencyIdentifier">Unicode currency identifier</a>.</li>
		</ul>
		<ul>
            <li><em>Examples:</em> <b>curr-eur</b>-per-square-meter, or pound-per-<b>curr-usd</b></li>
			<li><em>Note:</em> CLDR does not provide conversions for currencies; this is only intended for formatting.
				The locale data for currencies is supplied in the <code>currencies</code> element, not in the <code>units</code> element.</li>
        </ul>
	</td></tr>

</tbody></table>

Note that while the syntax allows for number_prefixes in multiple places, the typical use case is only one instance, after a "-per-".

The simple_unit structure does not allow for any two simple_units to overlap.
That is, there are no cases where simple_unit1 consists of X-Y and simple_unit2 consists of Y-Z.
This was not true in previous versions of LDML: cup-metric overlapped with metric-ton.
That meant that the unit identifiers for the product_unit of cup and metric-ton and the product_unit of cup-metric and ton were ambiguous.

The constraint that the identifiers can't overlap also means that parsing of multiple-subtag simple units is simpler.
For example:
* When a prefix_component is encountered, one can collect any other prefix-components, then one base_component, then any suffix components, and stop.
* Similarly, when a base_component is encountered, one can collect any suffix components, and stop.
* Encountering a suffix_component in any other circumstance is an error.

### <a name="Unit_Identifier_Uniqueness" href="#Unit_Identifier_Uniqueness">Unit Identifier Uniqueness</a>
CLDR Unit Identifiers can be used as values in locale identifiers. When that is done, the syntax is modified whenever a `prefixed_unit` would be longer than 8 characters. In such a case:

* If there is no `prefix` the `prefixed_unit` is truncated to 8 characters.
* If there is a `prefix`, a hyphen is added between the `prefix` and the `base_component`. If that `base_component` is longer than 8 characters, it is truncated to 8 characters.

_Example_
| Unit identifer | BCP47 syntax example | Comment |
| ----      | ----               | ----                           |
| kilogram  | en-u-ux-kilogram   | kilogram fits in 8 characters  |
| centilux  | en-u-ux-centilux   | centilux fixs in 8 characters  |
| steradian | en-u-ux-steradia   | steradian exceeds 8 characters |
| centigram | en-u-ux-centi-gram | centigram exceeds 8 characters |
| kilometer | en-u-ux-kilo-meter | kilometer exceeds 8 characters |
| quectolux | en-u-ux-kilo-meter | kilometer exceeds 8 characters |

This requires that each of the elements in base_components are unique to eight letters, that is: **no two different base_components will share the first 8 letters**.

The reason that the `prefixed_unit` as a whole is not simply truncated to 8 characters is that would impose too strict a constraint. There  are 5 letter prefixes such as 'centi' and more recently 6 letter prefixes such as 'quecto'. That would cause prefixed `base_component` as short as 'gram' and 'gray' to be ambiguous when truncated to 8 letters: 'centigra'; and 'lumen' and 'lux' would fail with the 6 letter prefixes.

### <a name="Example_Units" href="#Example_Units">Example Units</a>

The following table contains examples of groupings and units currently defined by CLDR.
The units in CLDR are not comprehensive; it is anticipated that more will be added over time.
The complete list of supported units is in the validity data: see _[Section Validity Data](tr35.md#Validity_Data)_.

| Type           | Core Unit Identifier     | Compound? | Sample Format  |
| -------------- | ------------------------ | --------- | -------------- |
| _acceleration_ | g-force                  | simple    | {0} G          |
| _acceleration_ | meter-per-square-second  | compound  | {0} m/s²       |
| _angle_        | revolution               | simple    | {0} rev        |
| _angle_        | radian                   | simple    | {0} rad        |
| _angle_        | degree                   | simple    | {0}°           |
| _angle_        | arc-minute               | simple    | {0}′           |
| _angle_        | arc-second               | simple    | {0}″           |
| _area_         | square-kilometer         | simple    | {0} km²        |
| _area_         | hectare                  | simple    | {0} ha         |
| ...            | ...                      | ...       | ...            |
| _area_         | square-inch              | simple    | {0} in²        |
| _area_         | dunam                    | simple    | {0} dunam      |
| _concentr_     | karat                    | simple    | {0} kt         | dimensionless |
| _concentr_     | milligram-per-deciliter  | compound  | {0} mg/dL      |
| _concentr_     | millimole-per-liter      | compound  | {0} mmol/L     |
| _concentr_     | permillion               | compound  | {0} ppm        | dimensionless |
| _concentr_     | percent                  | simple    | {0}%           | dimensionless |
| _concentr_     | permille                 | simple    | {0}‰           | dimensionless |
| _concentr_     | permyriad                | simple    | {0}‱          | dimensionless |
| _concentr_     | mole                     | simple    | {0} mol        | dimensionless |
| _consumption_  | liter-per-kilometer      | compound  | {0} L/km       |
| _consumption_  | liter-per-100-kilometer  | compound  | {0} L/100km    |
| _consumption_  | mile-per-gallon (US)     | compound  | {0} mpg        |
| _consumption_  | mile-per-gallon-imperial | compound  | {0} mpg Imp.   |
| _digital_      | petabyte                 | simple    | {0} PB         |
| ...            | ...                      | ...       | ...            |
| _digital_      | byte                     | simple    | {0} byte       |
| _digital_      | bit                      | simple    | {0} bit        |
| _duration_     | century                  | simple    | {0} c          |
| _duration_     | year                     | simple    | {0} y          |
| _duration_     | year-person              | simple    | {0} y          | for duration or age related to a person |
| _duration_     | month                    | simple    | {0} m          |
| _duration_     | month-person             | simple    | {0} m          | for duration or age related to a person |
| _duration_     | week                     | simple    | {0} w          |
| _duration_     | week-person              | simple    | {0} w          | for duration or age related to a person |
| _duration_     | day                      | simple    | {0} d          |
| _duration_     | day-person               | simple    | {0} d          | for duration or age related to a person |
| _duration_     | hour                     | simple    | {0} h          |
| ...            | ...                      | ...       | ...            |
| _duration_     | nanosecond               | simple    | {0} ns         |
| _electric_     | ampere                   | simple    | {0} A          |
| _electric_     | milliampere              | simple    | {0} mA         |
| _electric_     | ohm                      | simple    | {0} Ω          |
| _electric_     | volt                     | simple    | {0} V          |
| _energy_       | kilocalorie              | simple    | {0} kcal       |
| _energy_       | calorie                  | simple    | {0} cal        |
| _energy_       | foodcalorie              | simple    | {0} Cal        |
| _energy_       | kilojoule                | simple    | {0} kJ         |
| _energy_       | joule                    | simple    | {0} J          |
| _energy_       | kilowatt-hour            | simple    | {0} kWh        |
| _energy_       | electronvolt             | simple    | {0} eV         |
| _energy_       | british-thermal-unit     | simple    | {0} Btu        |
| _force_        | pound-force              | simple    | {0} lbf        |
| _force_        | newton                   | simple    | {0} N          |
| _frequency_    | gigahertz                | simple    | {0} GHz        |
| _frequency_    | megahertz                | simple    | {0} MHz        |
| _frequency_    | kilohertz                | simple    | {0} kHz        |
| _frequency_    | hertz                    | simple    | {0} Hz         |
| _length_       | kilometer                | simple    | {0} km         |
| ...            | ...                      | ...       | ...            |
| _length_       | inch                     | simple    | {0} in         |
| _length_       | parsec                   | simple    | {0} pc         |
| _length_       | light-year               | simple    | {0} ly         |
| _length_       | astronomical-unit        | simple    | {0} au         |
| _length_       | furlong                  | simple    | {0} fur        |
| _length_       | fathom                   | simple    | {0} fm         |
| _length_       | nautical-mile            | simple    | {0} nmi        |
| _length_       | mile-scandinavian        | simple    | {0} smi        |
| _length_       | point                    | simple    | {0} pt         | typographic point, 1/72 inch |
| _length_       | solar-radius             | simple    | {0} R☉        |
| _light_        | lux                      | simple    | {0} lx         |
| _light_        | solar-luminosity         | simple    | {0} L☉        |
| _mass_         | metric-ton               | simple    | {0} t          |
| _mass_         | kilogram                 | simple    | {0} kg         |
| ...            | ...                      | ...       | ...            |
| _mass_         | ounce                    | simple    | {0} oz         |
| _mass_         | ounce-troy               | simple    | {0} oz t       |
| _mass_         | carat                    | simple    | {0} CD         |
| _mass_         | dalton                   | simple    | {0} Da         |
| _mass_         | earth-mass               | simple    | {0} M⊕         |
| _mass_         | solar-mass               | simple    | {0} M☉        |
| _power_        | gigawatt                 | simple    | {0} GW         |
| ...            | ...                      | ...       | ...            |
| _power_        | milliwatt                | simple    | {0} mW         |
| _power_        | horsepower               | simple    | {0} hp         |
| _pressure_     | hectopascal              | simple    | {0} hPa        |
| _pressure_     | millimeter-ofhg          | simple    | {0} mm Hg      |
| _pressure_     | pound-force-per-square-inch | compound | {0} psi      |
| _pressure_     | inch-ofhg                | simple    | {0} inHg       |
| _pressure_     | millibar                 | simple    | {0} mbar       |
| _pressure_     | atmosphere               | simple    | {0} atm        |
| _pressure_     | kilopascal               | simple    | {0} kPa        |
| _pressure_     | megapascal               | simple    | {0} MPa        |
| _speed_        | kilometer-per-hour       | compound  | {0} km/h       |
| _speed_        | meter-per-second         | compound  | {0} m/s        |
| _speed_        | mile-per-hour            | compound  | {0} mi/h       |
| _speed_        | knot                     | simple    | {0} kn         |
| _temperature_  | generic                  | simple    | {0}°           |
| _temperature_  | celsius                  | simple    | {0}°C          |
| _temperature_  | fahrenheit               | simple    | {0}°F          |
| _temperature_  | kelvin                   | simple    | {0} K          |
| _torque_       | pound-force-foot         | simple    | {0} lbf⋅ft     |
| _torque_       | newton-meter             | simple    | {0} N⋅m        |
| _volume_       | cubic-kilometer          | simple    | {0} km³        |
| ...            | ...                      | ...       | ...            |
| _volume_       | cubic-inch               | simple    | {0} in³        |
| _volume_       | megaliter                | simple    | {0} ML         |
| ...            | ...                      | ...       | ...            |
| _volume_       | pint                     | simple    | {0} pt         |
| _volume_       | cup                      | simple    | {0} c          |
| _volume_       | fluid-ounce (US)         | simple    | {0} fl oz      |
| _volume_       | fluid-ounce-imperial     | simple    | {0} fl oz Imp. |
| _volume_       | tablespoon               | simple    | {0} tbsp       |
| _volume_       | teaspoon                 | simple    | {0} tsp        |
| _volume_       | barrel                   | simple    | {0} bbl        |

There are three widths: **long**, **short**, and **narrow**. As usual, the narrow forms may not be unique: in English, 1′ could mean 1 minute of arc, or 1 foot. Thus narrow forms should only be used where the context makes the meaning clear.

Where the unit of measurement is one of the [International System of Units (SI)](https://physics.nist.gov/cuu/Units/units.html), the short and narrow forms will typically use the international symbols, such as “mm” for millimeter. They may, however, be different if that is customary for the language or locale. For example, in Russian it may be more typical to see the Cyrillic characters “мм”.

Units are sometimes included for translation even where they are not typically used in a particular locale, such as kilometers in the US, or inches in Germany. This is to account for use by travelers and specialized domains, such as the German “Fernseher von 32 bis 55 Zoll (80 bis 140 cm)” for TV screen size in inches and centimeters.

For temperature, there is a special unit `<unit type="temperature-generic">`, which is used when it is clear from context whether Celcius or Fahrenheit is implied.

For duration, there are special units such as `<unit type="duration-year-person">` and `<unit type="duration-year-week">` for indicating the age of a person, which requires special forms in some languages. For example, in "zh", references to a person being 3 days old or 30 years old would use the forms “他3天大” and “他30岁” respectively.

<a name="compoundUnitPattern"></a><a name="perUnitPatterns"></a>

### Compound Units

A common combination of units is X per Y, such as _miles per hour_ or _liters per second_ or _kilowatt-hours_.

There are different types of structure used to build the localized name of compound units. All of these follow the inheritance specified in [Part 1, Lateral Inheritance](tr35.md#Lateral_Inheritance).

**Prefixes** are for powers of 10 and powers of 1024 (the latter only used with digital units of measure). These are invariant for case, gender, or plural (though those could be added in the future if needed by a language).

```xml
<compoundUnit type="10p9">
  <unitPrefixPattern>Giga{0}</unitPrefixPattern>
</compoundUnit>

<compoundUnit type="1024p3">
  <unitPrefixPattern>Gibi{0}</unitPrefixPattern>
</compoundUnit>
```

**number prefixes** are integers within a single_unit, such as in liter-per-**100-kilometer**. The formatting for these uses the normal number formats for the locale. Their presence does have an effect on the plural formatting of the simple unit in a "per" form. For example, in English you would write 3 liters per kilometer (singular "kilometer") but 3 liters per 100 kilometers (plural kilometers).

**compoundUnitPatterns** are used for compounding units by multiplication or division: kilowatt-hours, or meters per second. These are invariant for case, gender, or plural (though those could be added in the future if needed by a language).

```xml
<compoundUnit type="per">
  <compoundUnitPattern>{0} pro {1}</compoundUnitPattern>
</compoundUnit>

<compoundUnit type="times">
  <compoundUnitPattern>{0}⋅{1}</compoundUnitPattern>
</compoundUnit>
```

There can be at most one "per" pattern used in producing a compound unit, while the "times" pattern can be used multiple times.

`compoundUnitPattern1`s are used for expressing powers, such as square meter or cubic foot. These are the most complicated, since they can vary by plural category (count), by case, and by gender. However, these extra attributes are only used if they are present in the `grammaticalFeatures` element for the language in question. See [Grammatical Features](#Grammatical_Features). Note that the additional grammar elements are only present in the `<unitLength type='long'>` form.

```xml
<compoundUnit type="power2">
  <compoundUnitPattern1>{0} kw.</compoundUnitPattern1>
  <compoundUnitPattern1 count="one">{0} kwadratowe</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" case="accusative">{0} kwadratowe</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" case="dative">{0} kwadratowemu</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" case="genitive">{0} kwadratowego</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" case="instrumental">{0} kwadratowym</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" case="locative">{0} kwadratowym</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" case="vocative">{0} kwadratowe</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" gender="feminine">{0} kwadratowa</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" gender="feminine" case="accusative">{0} kwadratową</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" gender="feminine" case="dative">{0} kwadratowej</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" gender="feminine" case="genitive">{0} kwadratowej</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" gender="feminine" case="instrumental">{0} kwadratową</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" gender="feminine" case="locative">{0} kwadratowej</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" gender="feminine" case="vocative">{0} kwadratowa</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" gender="inanimate">{0} kwadratowy</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" gender="inanimate" case="accusative">{0} kwadratowy</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" gender="inanimate" case="dative">{0} kwadratowemu</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" gender="inanimate" case="genitive">{0} kwadratowego</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" gender="inanimate" case="instrumental">{0} kwadratowym</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" gender="inanimate" case="locative">{0} kwadratowym</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" gender="inanimate" case="vocative">{0} kwadratowy</compoundUnitPattern1>
  <compoundUnitPattern1 count="few">{0} kwadratowe</compoundUnitPattern1>
  <compoundUnitPattern1 count="few" case="accusative">{0} kwadratowe</compoundUnitPattern1>
  <compoundUnitPattern1 count="few" case="dative">{0} kwadratowym</compoundUnitPattern1>
  …
```

Some units already have 'precomputed' forms, such as **kilometer-per-hour**; where such units exist, they should be used in preference.

If there is no precomputed form, the following process in pseudocode is used to generate a pattern for the compound unit.

**pattern(unitId, locale, length, pluralCategory, caseVariant)**

1.  If the unitId is empty or invalid, fail
2.  Put the unitId into normalized order: hour-kilowatt => kilowatt-hour, meter-square-meter-per-second-second => cubic-meter-per-square-second
3.  Set result to be getValue(unitId with length, pluralCategory, caseVariant)
    1. If result is not empty, return it
4.  Divide the unitId into numerator (the part before the "-per-") and denominator (the part after the "-per-). If both are empty, fail
5.  Set both globalPlaceholder and globalPlaceholderPosition to be empty
6.  Set numeratorUnitString to patternTimes(numerator, length, per0(pluralCategory), per0(caseVariant))
7.  Set denominatorUnitString to patternTimes(denominator, length, per1(pluralCategory), per1(caseVariant))
8.  Set perPattern to be getValue(per, locale, length)
9.  If the denominatorString is empty, set result to numeratorString, otherwise set result to format(perPattern, numeratorUnitString, denominatorUnitString)
10. return format(result, globalPlaceholder, globalPlaceholderPosition)

**patternTimes(product_unit, locale, length, pluralCategory, caseVariant)**

1. Set hasMultiple to true iff product_unit has more than one single_unit
2. Set timesPattern to be getValue(times, locale, length)
3. Set result to be empty
4. Set multiplier to be empty
4. For each single_unit in product_unit
   1.  If hasMultiple
       1. Set singlePluralCategory to be times0(pluralCategory)
       2. Set singleCaseVariant to be times0(caseVariant)
       3. Set pluralCategory to be times1(pluralCategory)
       4. Set caseVariant to be times1(caseVariant)
   2.  If the singleUnit is a currency_unit
       1. Set coreUnit to be the formatted currency according to the pluralCategory
	   2. Set the gender to the default unit gender for the locale
	   3. Goto step 11
   2.  Get the gender of that single_unit
   3.  If singleUnit starts with a dimensionality_prefix, such as 'square-'
       1. set dimensionalityPrefixPattern to be getValue(that dimensionality_prefix, locale, length, singlePluralCategory, singleCaseVariant, gender), such as "{0} kwadratowym"
       2. set singlePluralCategory to be power0(singlePluralCategory)
       3. set singleCaseVariant to be power0(singleCaseVariant)
       4. remove the dimensionality_prefix from singleUnit
   4.  if singleUnit starts with an si_prefix, such as 'centi' and/or a number_prefix such as '100'
       1. set siPrefixPattern to be getValue(that si_prefix, locale, length), such as "centy{0}"
       2. set singlePluralCategory to be prefix0(singlePluralCategory)
       3. set singleCaseVariant to be prefix0(singleCaseVariant)
       4. remove the si_prefix from singleUnit
	   5. set multiplier to be the locales integer numberFormat of number_prefix.
   5.  Set corePattern to be the getValue(singleUnit, locale, length, singlePluralCategory, singleCaseVariant), such as "{0} metrem"
   6.  Extract(corePattern, coreUnit, placeholder, placeholderPosition) from that pattern.
   7.  If the position is _middle_, then fail
   8.  If globalPlaceholder is empty
       1. Set globalPlaceholder to placeholder
       2. Set globalPlaceholderPosition to placeholderPosition
   9.  If siPrefixPattern is not empty
       1. Set coreUnit to be the combineLowercasing(locale, length, siPrefixPattern, coreUnit)
   10. If dimensionalityPrefixPattern is not empty
       1. Set coreUnit to be the combineLowercasing(locale, length, dimensionalityPrefixPattern, coreUnit)
   10. If multiplier is not empty
       1. Combine the multiplier with coreUnit, using placeholder and placeholderPosition
   11. If the result is empty, set result to be coreUnit
   12. Otherwise set result to be format(timesPattern, result, coreUnit)
5. Return result

__Note: CLDR does not currently have gender or case data for currency units, so the formatting will not be optimal for inflected languages.__

**combineLowercasing(locale, length, prefixPattern, coreUnit)**

1. If the length is "long" and the prefixPattern contains no spaces, lowercase the coreUnit according to the locale, thus "Quadrat{0}" causes "Zentimeter" to become "zentimeter"
2. return format(prefixPattern, unitPattern), eg "Quadratzentimeter"

**format(pattern, arguments…)**

1. return the result of substituting the arguments for the placeholders {0}, {1}, etc.

**getValue(key, locale, length, variants…)**

1. return the element value in the locale for the path corresponding to the key, locale, length, and variants — using normal inheritance including [Lateral Inheritance](tr35.md#Multiple_Inheritance) and [Parent Locales](tr35.md#Parent_Locales).

**Extract(corePattern, coreUnit, placeholder, placeholderPosition)**

1. Find the position of the **placeholder** in the core pattern
2. Set **placeholderPosition** to that position (start, middle, or end)
3. Remove the **placeholder** from the **corePattern** and set **coreUnit** to that result

**per0(...), times0(...), etc.**

1. These represent the **deriveComponent** data values from **[Grammatical Derivations](#Grammatical_Derivations)**, where value0 of the per-structure is given as per0(...), and so on.
2. "power" corresponds to dimensionality_prefix, while "prefix" corresponds to si_prefix.

If the locale does not provide full modern coverage, the process could fall back to root locale for some localized patterns. That may give a "ransom-note" effect for the user. To avoid that, it may be preferable to abort the process at that point, and then localize the unitId for the root locale.

If a unit is not supported by root, then the localization is not supported by CLDR and will fail.

#### Precomposed Compound Units

At each point in the process, if there is a precomposed form for a segment of the unitId, then that precomposed form should be used instead. For example, if there is a pattern in the locale for (square-kilometer, length, singlePluralCategory, singleCaseVariant, gender), then it should be used instead of composing the name from "square" and "kilometer".

There is also a precomposed **perUnitPattern** which is used as the denominator with another unit name. For example, a form such as "{0} per second" can be used to form "2 feet **per second**". The difference between these is that in some inflected languages, the compoundUnit cannot be used to form grammatical phrases. This is typically because the "per" + "second" combine in a non-trivial way. The `perUnitPattern` should be applied if the denominator has only one element, and matches the `perUnitPattern` type.

### <a name="Unit_Sequences" href="#Unit_Sequences">Unit Sequences (Mixed Units)</a>

Units may be used in composed sequences (aka _mixed units_), such as **5° 30′** for 5 degrees 30 minutes, or **3 ft 2 in.** For that purpose, the appropriate width of the unit `listPattern` can be used to compose the units in a sequence.

```xml
<listPattern type="unit"> (for the long form)
<listPattern type="unit-narrow">
<listPattern type="unit-short">
```

In such a sequence, decimal fractions are typically only displayed for the last element of the sequence, if at all.

### <a name="durationUnit" href="#durationUnit">durationUnit</a>

The durationUnit is a special type of unit used for composed time unit durations.

```xml
<durationUnit type="hms">
  <durationUnitPattern>h:mm:ss</durationUnitPattern> <!-- 33:04:59 -->
</durationUnit>
```

The type contains a skeleton, where 'h' stands for hours, 'm' for minutes, and 's' for seconds. These are the same symbols used in availableFormats, except that there is no need to distinguish different forms of the hour.

### <a name="coordinateUnit" href="#coordinateUnit">coordinateUnit</a>

The **coordinateUnitPattern** is a special type of pattern used for composing degrees of latitude and longitude, with an indicator of the quadrant. There are exactly 4 type values, plus a displayName for the items in this category. An angle is composed using the appropriate combination of the **angle-degrees**, **angle-arc-minute** and **angle-arc-second** values. It is then substituted for the placeholder field {0} in the appropriate **coordinateUnit** pattern.

```xml
<displayName>direction</displayName>
<coordinateUnitPattern type="east">{0}E</coordinateUnitPattern>
<coordinateUnitPattern type="north">{0}N</coordinateUnitPattern>
<coordinateUnitPattern type="south">{0}S</coordinateUnitPattern>
<coordinateUnitPattern type="west">{0}W</coordinateUnitPattern>
```

### <a name="Territory_Based_Unit_Preferences" href="#Territory_Based_Unit_Preferences">Territory-Based Unit Preferences</a>

Different locales have different preferences for which unit or combination of units is used for a particular usage, such as measuring a person’s height. This is more fine-grained than merely a preference for metric versus US or UK measurement systems. For example, one locale may use meters alone, while another may use centimeters alone or a combination of meters and centimeters; a third may use inches alone, or (informally) a combination of feet and inches.

The `<unitPreferenceData>` element, described in [Preferred Units for Specific Usages](tr35-info.md#Preferred_Units_For_Usage), provides information on which unit or combination of units is used for various purposes in different locales, with options for the level of formality and the scale of the measurement (e.g. measuring the height of an adult versus that of an infant).

### <a name="Private_Use_Units" href="#Private_Use_Units">Private-Use Units</a>

CLDR has reserved the "xxx-" prefix in the simple_unit part of the unit identifier BNF for private-use units. CLDR will never define a type, simple unit, or compound unit such that the unit identifier starts with "xxx-", ends with "-xxx", or contains "-xxx-".

For example, if you wanted to define your own unit "foo", you could use the simple unit "xxx-foo".

It is valid to construct compound units containing one or more private-use simple units. For example, "xxx-foo-per-second" and "xxx-foo-per-xxx-bar" are both valid core unit identifiers for compound units.

As explained earlier, CLDR defines all associations between types and units. It is therefore not possible to construct a valid long unit identifier containing a private-use unit; only core unit identifiers are possible.

The older syntax used “x-”, which was expanded to “xxx-” to simplify use with BCP47 syntax. That should be converted to “xxx-”.

## <a name="POSIX_Elements" href="#POSIX_Elements">POSIX Elements</a>

```xml
<!ELEMENT posix (alias | (messages*, special*)) >
<!ELEMENT messages (alias | ( yesstr*, nostr*)) >
```

The following are included for compatibility with POSIX.

```xml
<posix>
    <posix:messages>
        <posix:yesstr>ja</posix:yesstr>
        <posix:nostr>nein</posix:nostr>
    </posix:messages>
</posix>
```

1. The values for yesstr and nostr contain a colon-separated list of strings that would normally be recognized as "yes" and "no" responses. For cased languages, this shall include only the lower case version. POSIX locale generation tools must generate the upper case equivalents, and the abbreviated versions, and add the English words wherever they do not conflict. Examples:
    * ja → ja:Ja:j:J:yes:Yes:y:Y
    * ja → ja:Ja:j:J:yes:Yes // exclude y:Y if it conflicts with the native "no".
2. The older elements `yesexpr` and `noexpr` are deprecated. They should instead be generated from `yesstr` and `nostr` so that they match all the responses.

So for English, the appropriate strings and expressions would be as follows:

```
yesstr "yes:y"
nostr "no:n"
```

The generated yesexpr and noexpr would be:

```
yesexpr "^([yY]([eE][sS])?)"
```

This would match y,Y,yes,yeS,yEs,yES,Yes,YeS,YEs,YES.

```
noexpr "^([nN][oO]?)"
```

This would match n,N,no,nO,No,NO.

## <a name="Reference_Elements" href="#Reference_Elements">Reference Element</a>

(Use only in supplemental data; deprecated for ldml.dtd and locale data)

```xml
<!ELEMENT references ( reference* ) >
<!ELEMENT reference ( #PCDATA ) >
<!ATTLIST reference type NMTOKEN #REQUIRED>
<!ATTLIST reference standard ( true | false ) #IMPLIED >
<!ATTLIST reference uri CDATA #IMPLIED >
```

The references section supplies a central location for specifying references and standards. The uri should be supplied if at all possible. If not online, then an ISBN number should be supplied, such as in the following example:

```xml
<reference type="R2" uri="https://www.ur.se/nyhetsjournalistik/3lan.html">Landskoder på Internet</reference>
<reference type="R3" uri="URN:ISBN:91-47-04974-X">Svenska skrivregler</reference>
```

## <a name="Segmentations" href="#Segmentations">Segmentations</a>

```xml
<!ELEMENT segmentations ( alias | segmentation*) >

<!ELEMENT segmentation ( alias | (variables?, segmentRules? , exceptions?, suppressions?) | special*) >
<!ATTLIST segmentation type NMTOKEN #REQUIRED >

<!ELEMENT variables ( alias | variable*) >

<!ELEMENT variable ( #PCDATA ) >
<!ATTLIST variable id CDATA #REQUIRED >

<!ELEMENT segmentRules ( alias | rule*) >

<!ELEMENT rule ( #PCDATA ) >
<!ATTLIST rule id NMTOKEN #REQUIRED >

<!ELEMENT suppressions ( suppression* ) >

<!ATTLIST suppressions type NMTOKEN "standard" >

<!ATTLIST suppressions draft ( approved | contributed | provisional | unconfirmed ) #IMPLIED >

<!ELEMENT suppression ( #PCDATA ) >
```

The `segmentations` element provides for segmentation of text into words, lines, or other segments. The structure is based on [[UAX29](https://www.unicode.org/reports/tr41/#UAX29)] notation, but adapted to be machine-readable. It uses a list of variables (representing character classes) and a list of rules. Each must have an `id` attribute.

The rules in _root_ implement the segmentations found in [[UAX29](https://www.unicode.org/reports/tr41/#UAX29)] and
[[UAX14](https://www.unicode.org/reports/tr41/#UAX14)], for grapheme clusters, words, sentences, and lines. They can be
overridden by rules in child locales. In addition, there are several locale keywords that affect segmentation:

* "dx", [Unicode Dictionary Break Exclusion Identifier](tr35.md#UnicodeDictionaryBreakExclusionIdentifier)
* "lb", [Unicode Line Break Style Identifier](tr35.md#UnicodeLineBreakStyleIdentifier)
* "lw", [Unicode Line Break Word Identifier ](tr35.md#UnicodeLineBreakWordIdentifier)
* "ss", [Unicode Sentence Break Suppressions Identifier ](tr35.md#UnicodeSentenceBreakSuppressionsIdentifier)

Here is an example:

```xml
<segmentations>
  <segmentation type="GraphemeClusterBreak">
    <variables>
      <variable id="$CR">\p{Grapheme_Cluster_Break=CR}</variable>
      <variable id="$LF">\p{Grapheme_Cluster_Break=LF}</variable>
      <variable id="$Control">\p{Grapheme_Cluster_Break=Control}</variable>
      <variable id="$Extend">\p{Grapheme_Cluster_Break=Extend}</variable>
      <variable id="$L">\p{Grapheme_Cluster_Break=L}</variable>
      <variable id="$V">\p{Grapheme_Cluster_Break=V}</variable>
      <variable id="$T">\p{Grapheme_Cluster_Break=T}</variable>
      <variable id="$LV">\p{Grapheme_Cluster_Break=LV}</variable>
      <variable id="$LVT">\p{Grapheme_Cluster_Break=LVT}</variable>
    </variables>
    <segmentRules>
      <rule id="3"> $CR × $LF </rule>
      <rule id="4"> ( $Control | $CR | $LF ) ÷ </rule>
      <rule id="5"> ÷ ( $Control | $CR | $LF ) </rule>
      <rule id="6"> $L × ( $L | $V | $LV | $LVT ) </rule>
      <rule id="7"> ( $LV | $V ) × ( $V | $T ) </rule>
      <rule id="8"> ( $LVT | $T) × $T </rule>
      <rule id="9"> × $Extend </rule>
    </segmentRules>
  </segmentation>
...
```

**Variables:** All variable ids must start with a $, and otherwise be valid identifiers according to the Unicode definitions in [[UAX31](https://www.unicode.org/reports/tr41/#UAX31)]. The contents of a variable is a regular expression using variables and [UnicodeSet](tr35.md#Unicode_Sets)s. The ordering of variables is important; they are evaluated in order from first to last (see _[Segmentation Inheritance](#Segmentation_Inheritance)_). It is an error to use a variable before it is defined.

**Rules:** The contents of a rule uses the syntax of [[UAX29](https://www.unicode.org/reports/tr41/#UAX29)]. The rules are evaluated in numeric id order (which may not be the order in which they appear in the file). The first rule that matches determines the status of a boundary position, that is, whether it breaks or not. Thus ÷ means a break is allowed; × means a break is forbidden. It is an error if the rule does not contain exactly one of these characters (except where a rule has no contents at all, or if the rule uses a variable that has not been defined.

There are some implicit rules:

*   The implicit initial rules are always "start-of-text ÷" and "÷ end-of-text"; these are not to be included explicitly.
*   The implicit final rule is always "Any ÷ Any". This is not to be included explicitly.

> **Note:** A rule like X Format\* -> X in [[UAX29](https://www.unicode.org/reports/tr41/#UAX29)] and [[UAX14](https://www.unicode.org/reports/tr41/#UAX14)] is not supported. Instead, this needs to be expressed as normal regular expressions. The normal way to support this is to modify the variables, such as in the following example:
>
> ```xml
> <variable id="$Format">\p{Word_Break=Format}</variable>
> <variable id="$Katakana">\p{Word_Break=Katakana}</variable>
> ...
> <!-- In place of rule 3, add format and extend to everything -->
> <variable id="$X">[$Format $Extend]*</variable>
> <variable id="$Katakana">($Katakana $X)</variable>
> <variable id="$ALetter">($ALetter $X)</variable>
> ...
> ```

### <a name="Segmentation_Inheritance" href="#Segmentation_Inheritance">Segmentation Inheritance</a>

Variables and rules both inherit from the parent.

**Variables:** The child's variable list is logically appended to the parent's, and evaluated in that order. For example:

```xml
// in parent
<variable id="$AL">[:linebreak=AL:]</variable>
<variable id="$YY">[[:linebreak=XX:]$AL]</variable> // adds $AL

// in child
<variable id="$AL">[$AL && [^a-z]]</variable> // changes $AL, does not affect $YY
<variable id="$ABC">[abc]</variable> // adds new rule
```

**Rules:** The rules are also logically appended to the parent's. Because rules are evaluated in numeric id order, to insert a rule in between others just requires using an intermediate number. For example, to insert a rule after id="10.1" and before id="10.2", just use id="10.15". To delete a rule, use empty contents, such as:

```xml
<rule id="3" /> // deletes rule 3
````

### <a name="Segmentation_Exceptions" href="#Segmentation_Exceptions">Segmentation Suppressions</a>

**Note:** As of CLDR 26, the `<suppressions>` data is to be considered a technology preview. Data currently in CLDR was extracted from the Unicode Localization Interoperability project, or ULI. The ULI committee has been disbanded, but historical information can be found at <https://www.unicode.org/uli/>.

The segmentation **suppressions** list provides a set of cases which, though otherwise identified as a segment by rules, should be skipped (suppressed) during segmentation.

For example, in the English phrase "Mr. Smith", CLDR segmentation rules would normally find a Sentence Break between "Mr" and "Smith". However, typically, "Mr." is just an abbreviation for "Mister", and not actually the end of a sentence.

Each suppression has a separate `<suppression>` element, whose contents are the break to be skipped.

Example:

```xml
<segmentation type="SentenceBreak">
    <suppressions type="standard" draft="provisional">
        <suppression>Maj.</suppression>
        <suppression>Mr.</suppression>
        <suppression>Lt.Cdr.</suppression>
        . . .
    </suppressions>
</segmentation>
```

**Note:** These elements were called `<exceptions>` and `<exception>` prior to CLDR 26, but those names are now deprecated.

## <a name="Transforms" href="#Transforms">Transforms</a>

Transforms provide a set of rules for transforming text via a specialized set of context-sensitive matching rules. They are commonly used for transliterations or transcriptions, but also other transformations such as full-width to half-width (for _katakana_ characters). The rules can be simple one-to-one relationships between characters, or involve more complicated mappings. Here is an example:

```xml
<transform source="Greek" target="Latin" variant="UNGEGN" direction="both">
    ...
    <comment>Useful variables</comment>
    <tRule>$gammaLike = [ΓΚΞΧγκξχϰ] ;</tRule>
    <tRule>$egammaLike = [GKXCgkxc] ;</tRule>
    ...
    <comment>Rules are predicated on running NFD first, and NFC afterwards</comment>
    <tRule>::NFD (NFC) ;</tRule>
    ...
    <tRule>λ ↔ l ;</tRule>
    <tRule>Λ ↔ L ;</tRule>
    ...
    <tRule>γ } $gammaLike ↔ n } $egammaLike ;</tRule>
    <tRule>γ ↔ g ;</tRule>
    ...
    <tRule>::NFC (NFD) ;</tRule>
    ...
</transform>
```

The source and target values are valid locale identifiers, where 'und' means an unspecified language, plus some additional extensions.

* The long names of a script according to [[UAX24](https://www.unicode.org/reports/tr41/#UAX24)] may be used instead of the short script codes. The script identifier may also omit und; that is, "und_Latn" may be written as just "Latn".
* The long names of the English languages may also be used instead of the languages.
* The term "Any" may be used instead of a solitary "und".
* Other identifiers may be used for special purposes. In CLDR, these include: Accents, Digit, Fullwidth, Halfwidth, Jamo, NumericPinyin, Pinyin, Publishing, Tone. (Other than these values, valid private use locale identifiers should be used, such as "x-Special".)
* When presenting localizing transform names, the "und\_" is normally omitted. Thus for a transliterator with the ID "und_Latn-und_Grek" (or the equivalent "Latin-Greek"), the translated name for Greek would be Λατινικό-Ελληνικό.

In version 29.0, BCP47 identifiers were added as aliases (while retaining the old identifiers). The following table shows the relationship between the old identifiers and the BCP47 format identifiers.

<!-- HTML: rowspan -->

<table><tbody>
<tr>
    <th>Old ID</th>
    <th>BCP47 ID</th>
    <th>Comments</th>
</tr>
<tr>
    <td><b>es_FONIPA</b>-es_419_FONIPA</td>
    <td>es-419-fonipa-t-<b>es-fonipa</b></td>
    <td rowspan="2">The order reverses with -t-. That is, the language subtag part is what results.</td>
</tr>
<tr>
    <td><b>hy_AREVMDA</b>-hy_AREVMDA_FONIPA</td>
    <td>hy-arevmda-fonipa-t-<b>hy-arevmda</b></td>
</tr>
<tr>
    <td><b>Devanagari</b>-Latin</td>
    <td>und-Latn-t-<b>und-deva</b></td>
    <td rowspan="2">Scripts add <b>und-</b></td>
</tr>
<tr>
    <td><b>Latin</b>-Devanagari</td>
    <td>und-Deva-t-<b>und-latn</b></td>
</tr>
<tr>
    <td>Greek-Latin/UNGEGN</td>
    <td>und-Latn-t-und-grek-<b>m0-ungegn</b></td>
    <td>Variants use the <b>-m0-</b> key.</td>
</tr>
<tr>
    <td>Russian-Latin/BGN</td>
    <td>ru<b>-Latn</b>-t-ru-m0-bgn</td>
    <td>Languages will have a script when it isn’t the default.</td>
</tr>
<tr>
    <td>Any-Hex/xml</td>
    <td>und-t-<b>d0-hex</b>-m0-xml</td>
    <td rowspan="2"><b>Any</b> becomes <b>und</b>, and keys <b>d0</b> (destination) and <b>s0</b> (source) are used for non-locales.</td>
</tr>
<tr>
    <td>Hex-Any/xml</td>
    <td>und-t-<b>s0-hex</b>-m0-xml</td>
</tr>
<tr>
    <td>Any-<b>Publishing</b></td>
    <td>und-t-d0-<b>publish</b></td>
    <td rowspan="2">Non-locales are normally the lowercases of the old ID, but may change because of BCP47 length restrictions.</td>
</tr>
<tr>
    <td><b>Publishing</b>-Any</td>
    <td>und-t-s0-<b>publish</b></td>
</tr>
</tbody></table>

Note that the script and region codes are cased iff they are in the main subtag, but are lowercase in extensions.

### <a name="Inheritance" href="#Inheritance">Inheritance</a>

The CLDR transforms are built using the following locale inheritance. While this inheritance is not required of LDML implementations, the transforms supplied with CLDR may not otherwise behave as expected without some changes.

For either the source or the target, the fallback starts from the maximized locale ID (using the likely-subtags data). It also uses the country for lookup before the base language is reached, and root is never accessed: instead the script(s) associated with the language are used. Where there are multiple scripts, the maximized script is tried first, and then the other scripts associated with the language (from supplemental data).

For example, see the bolded items below in the fallback chain for **az_IR**.

|     | Locale ID      | Comments                       |
| --- | -------------- | ------------------------------ |
|  1  | **az_Arab_IR** | The maximized locale for az_IR |
|  2  | az_Arab        | Normal fallback                |
|  3  | **az_IR**      | Inserted country locale        |
|  4  | az             | Normal fallback                |
|  5  | **Arab**       | Maximized script               |
|  6  | **Cyrl**       | Other associated script        |

The source, target, and variant use "laddered" fallback, where the source changes the most quickly (using the above rules), then the target (using the above rules), then the variant if any, is discarded. That is, in pseudo code:

* for variant in {variant, ""}
  * for target in target-chain
    * for source in source-chain
      * transform = lookup source-target/variant
      * if transform != null return transform

For example, here is the fallback chain for **ru_RU-el_GR/BGN**.

| source |     | target | variant |
| ------ | --- | ------ | ------- |
| ru_RU  | -   | el_GR  | /BGN    |
| ru     | -   | el_GR  | /BGN    |
| Cyrl   | -   | el_GR  | /BGN    |
| ru_RU  | -   | el     | /BGN    |
| ru     | -   | el     | /BGN    |
| Cyrl   | -   | el     | /BGN    |
| ru_RU  | -   | Grek   | /BGN    |
| ru     | -   | Grek   | /BGN    |
| Cyrl   | -   | Grek   | /BGN    |
| ru_RU  | -   | el_GR  |         |
| ru     | -   | el_GR  |         |
| Cyrl   | -   | el_GR  |         |
| ru_RU  | -   | el     |         |
| ru     | -   | el     |         |
| Cyrl   | -   | el     |         |
| ru_RU  | -   | Grek   |         |
| ru     | -   | Grek   |         |
| Cyrl   | -   | Grek   |         |

Japanese and Korean are special, since they can be represented by combined script codes, such as ja_Jpan, ja_Hrkt, ja_Hira, or ja_Kana. These need to be considered in the above fallback chain as well.

#### <a name="Pivots" href="#Pivots">Pivots</a>

Transforms can also use _pivots_. These are used when there is no direct transform between a source and target, but there are transforms X-Y and Y-Z. In such a case, the transforms can be internally chained to get X-Y = X-Y;Y-Z. This is done explicitly with the Indic script transforms: to get Devanagari-Latin, internally it is done by transforming first from Devanagari to Interindic (an internal superset encoding for Indic scripts), then from Interindic to Latin. This allows there to be only N sets of transform rules for the Indic scripts: each one to and from Interindic. These pivots are explicitly represented in the CLDR transforms.

Note that the characters currently used by Interindic are private use characters. To prevent those from “leaking” out into text, transforms converting from Interindic must ensure that they convert all the possible values used in Interindic.

The pivots can also be produced automatically (implicitly), as a fallback. A particularly useful pivot is IPA, since that tends to preserve pronunciation. For example, _Czech to IPA_ can be chained with _IPA to Katakana_ to get _Czech to Katakana_.

CLDR often has special forms of IPA: not just "und-FONIPA" but "cs-FONIPA": specifically IPA that has come from Czech. These variants typically preserve some features of the source language — such as double consonants — that are indistinguishable from single consonants in that language, but that are often preserved in traditional transliterations. Thus when matching prospective pivots, FONIPA is treated specially. If there is an exact match, that match is used (such as cs-cs_FONIPA + cs_FONIPA-ko). Otherwise, the language is ignored, as for example in cs-cs_FONIPA + ru_FONIPA-ko.

The interaction of implicit pivots and inheritance may result in a longer inheritance chain lookup than desired, so implementers may consider having some sort of caching mechanism to increase performance.

### <a name="Variants" href="#Variants">Variants</a>

Variants used in CLDR include UNGEGN and BGN, both indicating sources for transliterations. There is an additional attribute `private="true"` which is used to indicate that the transform is meant for internal use, and should not be displayed as a separate choice in a UI.

There are many different systems of transliteration. The goal for the "unqualified" script transliterations are

1. to be lossless when going to Latin and back
2. to be as lossless as possible when going to other scripts
3. to abide by a common standard as much as possible (possibly supplemented to meet goals 1 and 2).

Language-to-language transliterations, and variant script-to-script transliterations are generally transcriptions, and not expected to be lossless.

Additional transliterations may also be defined, such as customized language-specific transliterations (such as between Russian and French), or those that match a particular transliteration standard, such as the following:

* UNGEGN - United Nations Group of Experts on Geographical Names
* BGN - United States Board on Geographic Names
* ISO9 - ISO/IEC 9
* ISO15915 - ISO/IEC 15915
* ISCII91 - ISCII 91
* KMOCT - South Korean Ministry of Culture & Tourism
* USLC - US Library of Congress
* UKPCGN - Permanent Committee on Geographical Names for British Official Use
* RUGOST - Russian Main Administration of Geodesy and Cartography

The rules for transforms are described in [Transform Rules Syntax](#Transform_Rules_Syntax). For more information on Transliteration, see [Transliteration Guidelines](https://cldr.unicode.org/index/cldr-spec/transliteration-guidelines).

### <a name="Transform_Rules_Syntax" href="#Transform_Rules_Syntax">Transform Rules Syntax</a>

```xml
<!ELEMENT transforms ( transform*) >
<!ELEMENT transform ((comment | tRule)*) >
<!ATTLIST transform source CDATA #IMPLIED >
<!ATTLIST transform target CDATA #IMPLIED >
<!ATTLIST transform variant CDATA #IMPLIED >
<!ATTLIST transform direction ( forward | backward | both ) "both" >
<!ATTLIST transform alias CDATA #IMPLIED >
<!--@VALUE-->
<!ATTLIST transform backwardAlias CDATA #IMPLIED >
<!--@VALUE-->
<!ATTLIST transform visibility ( internal | external ) "external" >
<!ELEMENT comment (#PCDATA) >
<!ELEMENT tRule (#PCDATA) >
```

The `transform` attributes indicate the `source`, `target`, `direction`, and `alias`es. For example:

```xml
<transform
  source="ja_Hrkt"
  target="ja_Latn"
  variant="BGN"
  direction="forward"
  draft="provisional"
  alias="Katakana-Latin/BGN ja-Latn-t-ja-hrkt-m0-bgn">
```

The direction is either `forward` or `both` (`backward` is possible in theory, but not used). This indicates which directions the rules support.

If the direction is `forward`, then an ID is composed from `target + "-" + source + "/" + variant`. If the direction is `both`, then the inverse ID is also value: `source + "-" + target + "/" + variant`. The `alias` attribute contains a space-delimited list of alternant forward IDs, while the `backwardAlias` contains a space-delimited list of alternant backward IDs. The BCP47 versions of the IDs will be in the `alias` and/or `backwardAlias` attributes.

The `visibility` attribute indicates whether the IDs should be externally visible, or whether they are only used internally.

Note: In CLDR v28 and before, the rules were expressed as fine-grained XML.
That was discarded in CLDR version 29, in favor of a simpler format where the separate rules are simply terminated with ";".

The transform rules are similar to regular-expression substitutions, but adapted to the specific domain of text transformations. The rules and comments in this discussion will be intermixed, with # marking the comments. The simplest rule is a conversion rule, which replaces one string of characters with another. The conversion rule takes the following form:

```
xy → z ;
```

This converts any substring "xy" into "z". Rules are executed in order; consider the following rules:

```
sch → sh ;
ss → z ;
```

This conversion rule transforms "bass school" into "baz shool". The transform walks through the string from start to finish. Thus given the rules above "bassch" will convert to "bazch", because the "ss" rule is found before the "sch" rule in the string (later, we'll see a way to override this behavior). If two rules can both apply at a given point in the string, then the transform applies the first rule in the list.

All of the ASCII characters except numbers and letters are reserved for use in the rule syntax, as are the characters `→`, `←`, `↔`. Normally, these characters do not need to be converted. However, to convert them use either a pair of single quotes or a slash. The pair of single quotes can be used to surround a whole string of text. The slash affects only the character immediately after it. For example, to convert from a U+2190 ( ← ) LEFTWARDS ARROW to the string "arrow sign" (with a space), use one of the following rules:

```
\←    → arrow\ sign ;
'←'   → 'arrow sign' ;
'←'   → arrow' 'sign ;
```

Note: The characters `→`, `←`, `↔` are preferred, but can be represented by the ASCII character `>`, `<`, and `<>`, respectively.

Spaces may be inserted anywhere without any effect on the rules. Use extra space to separate items out for clarity without worrying about the effects. This feature is particularly useful with combining marks; it is handy to put some spaces around it to separate it from the surrounding text. The following is an example:

```
→ i ; # an iota-subscript diacritic turns into an i.
```

For a real space in the rules, place quotes around it. For a real backslash, either double it \\\\, or quote it '\\'. For a real single quote, double it '', or place a backslash before it \\'.

Any text that starts with a hash mark and concludes a line is a comment. Comments help document how the rules work. The following shows a comment in a rule:

```
x → ks ; # change every x into ks
```

The “\\u” and “\\x” hex notations can be used instead of any letter. For instance, instead of using the Greek π, one could write either of the following:

```
\u03C0 → p ;
\x{3C0} → p ;
```

One can also define and use variables, such as:

```
$pi = \u03C0 ;
$pi → p ;
```

#### <a name="Dual_Rules" href="#Dual_Rules">Dual Rules</a>

Rules can also specify what happens when an inverse transform is formed. To do this, we reverse the direction of the "←" sign. Thus the above example becomes:

```
$pi ← p ;
```

With the inverse transform, "p" will convert to the Greek p. These two directions can be combined together into a dual conversion rule by using the `↔` operator, yielding:

```
$pi ↔ p ;
```

#### <a name="Context" href="#Context">Context</a>

Context can be used to have the results of a transformation be different depending on the characters before or after. The following rule removes hyphens, but only when they follow lowercase characters:

```
[:Lowercase:] { '-' → ;
```

Contexts can be before or after or both, such as in a rule to remove hyphens between lowercase and uppercase letters:

```
[:Lowercase:] { '-' } [:Uppercase:] → ;
```

Each context is optional and may be empty; the following two rules are equivalent:

```
$pi ↔ p ;
{$pi} ↔ {p} ;
```

The context itself ([: `Lowercase` :]) is unaffected by the replacement; only the text within braces is changed.

Character classes (UnicodeSets) in the contexts can contain the special symbol $, which means “off either end of the string”. It is roughly similar to $ and ^ in regex. Unlike normal regex, however, it can occur in character classes. Thus the following rule removes hyphens that are after lowercase characters, _or_ are at the start of a string.

```
[[:Lowercase:]$] {'-' → ;
```

Thus the negation of a UnicodeSet will normally also match before or after the end of a string. The following will remove hyphens that are not after lowercase characters, _including hyphens at the start of a string_.

```
[^[:Lowercase:]] {'-' → ;
```

It will thus convert “-B A-B a-b” to “B AB a-b”.

#### <a name="Revisiting" href="#Revisiting">Revisiting</a>

If the resulting text contains a vertical bar "|", then that means that processing will proceed from that point and that the transform will revisit part of the resulting text.
Thus the | marks a "cursor" position.
For example, if we have the following, then the string "xa" will convert to "yw".

```
x → y | z ;
z a → w ;
```

First, "xa" is converted to "yza". Then the processing will continue from after the character "y", pick up the "za", and convert it. Had we not had the "|", the result would have been simply "yza".

The '@' character can be used as filler character to place the revisiting point off the start or end of the string — but only within the context. Consider the following rules, with the table afterwards showing how they work.

```
1. [a-z]{x > |@ab ;
2. ab > J;
3. ca > M;
```
The ⸠ indicates the virtual cursor:

| Current text | Matching rule |
| - | - |
| ⸠cx | no match, cursor advances one code point |
| c⸠x | matches rule 1, so the text is replaced and cursor backs up. |
| ⸠cab | matches rule 3, so the text is replaced, with cursor at the end. |
| Mb⸠ | cursor is at the end, so we are done. |

Notice that rule 2 did not have a chance to trigger.

There is a current restriction that @ cannot back up before the before_context or after the after_context.
Consider the rules if rule 1 is adjusted to have no before_context.

```
1'. x > |@ab ;
2. ab > J ;
3. ca > M;
```

In that case, the results are different.
| Current text | Matching rule |
| - | - |
| ⸠cx | no match, cursor advances one code point |
| c⸠x | matches rule 1, so the text is replaced and cursor backs up; but only to where  |
| c⸠ab | matches **rule 2**, so the text is replaced, with cursor at the end. |
| cJ⸠ | cursor is at the end, so we are done. |

#### <a name="Example" href="#Example">Example</a>

The following shows how these features are combined together in the Transliterator "Any-Publishing". This transform converts the ASCII typewriter conventions into text more suitable for desktop publishing (in English). It turns straight quotation marks or UNIX style quotation marks into curly quotation marks, fixes multiple spaces, and converts double-hyphens into a dash.

```perl
# Variables

$single = \' ;
$space = ' ' ;
$double = \" ;
$back = \` ;
$tab = '\u0008' ;

# the following is for spaces, line ends, (, [, {, ...
$makeRight = [[:separator:][:start punctuation:][:initial punctuation:]] ;

# fix UNIX quotes

$back $back → “ ; # generate right d.q.m. (double quotation mark)
$back → ‘ ;

# fix typewriter quotes, by context

$makeRight { $double ↔ “ ; # convert a double to right d.q.m. after certain chars
^ { $double → “ ; # convert a double at the start of the line.
$double ↔ ” ; # otherwise convert to a left q.m.

$makeRight {$single} ↔ ‘ ; # do the same for s.q.m.s
^ {$single} → ‘ ;
$single ↔ ’;

# fix multiple spaces and hyphens

$space {$space} → ; # collapse multiple spaces
'--' ↔ — ; # convert fake dash into real one
```

There is an online demo where the rules can be tested, at:

<https://util.unicode.org/UnicodeJsps/transform.jsp>

#### <a name="Rule_Syntax" href="#Rule_Syntax">Rule Syntax</a>

The following describes the full format of the list of rules used to create a transform. Each rule in the list is terminated by a semicolon. The list consists of the following:

* an optional filter rule
* zero or more transform rules
* zero or more variable-definition rules
* zero or more conversion rules
* an optional inverse filter rule

The filter rule, if present, must appear at the beginning of the list, before any of the other rules.  The inverse filter rule, if present, must appear at the end of the list, after all of the other rules.  The other rules may occur in any order and be freely intermixed.

The rule list can also generate the inverse of the transform. In that case, the inverse of each of the rules is used, as described below.

#### <a name="Transform_Rules" href="#Transform_Rules">Transform Rules</a>

Each transform rule consists of two colons followed by a transform name, which is of the form source-target. For example:

```
:: NFD ;
:: und_Latn-und_Greek ;
:: Latin-Greek; # alternate form
```

If either the source or target is 'und', it can be omitted, thus 'und_NFC' is equivalent to 'NFC'. For compatibility, the English names for scripts can be used instead of the und_Latn locale name, and "Any" can be used instead of "und". Case is not significant.

The following transforms are defined not by rules, but by the operations in the Unicode Standard, and may be used in building any other transform:

> **Any-NFC, Any-NFD, Any-NFKD, Any-NFKC** - the normalization forms defined by [[UAX15](https://www.unicode.org/reports/tr41/#UAX15)].
>
> **Any-Lower, Any-Upper, Any-Title** - full case transformations, defined by [[Unicode](tr35.md#Unicode)] Chapter 3.

In addition, the following special cases are defined:

> **Any-Null** - has no effect; that is, each character is left alone.
> **Any-Remove** - maps each character to the empty string; this, removes each character.

The inverse of a transform rule uses parentheses to indicate what should be done when the inverse transform is used. For example:

```
:: lower () ; # only executed for the normal
:: (lower) ; # only executed for the inverse
:: lower ; # executed for both the normal and the inverse
```

#### <a name="Variable_Definition_Rules" href="#Variable_Definition_Rules">Variable Definition Rules</a>

Each variable definition is of the following form:

```
$variableName = contents ;
```

The variable name can contain letters and digits, but must start with a letter. More precisely, the variable names use Unicode identifiers as defined by [[UAX31](https://www.unicode.org/reports/tr41/#UAX31)]. The identifier properties allow for the use of foreign letters and numbers.

The contents of a variable definition is any sequence of Unicode sets and characters or characters. For example:

```
$mac = M [aA] [cC] ;
```

Variables are only replaced within other variable definition rules and within conversion rules. They have no effect on transliteration rules.

#### <a name="Filter_Rules" href="#Filter_Rules">Filter Rules</a>

A filter rule consists of two colons followed by a UnicodeSet. This filter is global in that only the characters matching the filter will be affected by any transform rules or conversion rules. The inverse filter rule consists of two colons followed by a UnicodeSet in parentheses. This filter is also global for the inverse transform.

For example, the Hiragana-Latin transform can be implemented by "pivoting" through the Katakana converter, as follows:

```
:: [:^Katakana:] ; # do not touch any katakana that was in the text!
:: Hiragana-Katakana;
:: Katakana-Latin;
:: ([:^Katakana:]) ; # do not touch any katakana that was in the text
                     # for the inverse either!
```

The filters keep the transform from mistakenly converting any of the "pivot" characters. Note that this is a case where a rule list contains no conversion rules at all, just transform rules and filters.

#### <a name="Conversion_Rules" href="#Conversion_Rules">Conversion Rules</a>

Conversion rules can be forward, backward, or double. The complete conversion rule syntax is described below:

**Forward**

> A forward conversion rule is of the following form:
> ```
> before_context { text_to_replace } after_context → completed_result | result_to_revisit ;
> ```
> If there is no before_context, then the "{" can be omitted. If there is no after_context, then the "}" can be omitted. If there is no result_to_revisit, then the "|" can be omitted. A forward conversion rule is only executed for the normal transform and is ignored when generating the inverse transform.

**Backward**

> A backward conversion rule is of the following form:
> ```
> completed_result | result_to_revisit ← before_context { text_to_replace } after_context ;
> ```
> The same omission rules apply as in the case of forward conversion rules. A backward conversion rule is only executed for the inverse transform and is ignored when generating the normal transform.

**Dual**

> A dual conversion rule combines a forward conversion rule and a backward conversion rule into one, as discussed above. It is of the form:
>
> ```
> a { b | c } d ↔ e { f | g } h ;
> ```
>
> When generating the normal transform and the inverse, the revisit mark "|" and the before and after contexts are ignored on the sides where they do not belong. Thus, the above is exactly equivalent to the sequence of the following two rules:
>
> ```
> a { b c } d → f | g  ;
> b | c  ←  e { f g } h ;
> ```

The `completed_result` | `result_to_revisit` is also known as the `resulting_text`. Either or both of the values can be empty. For example, the following removes any a, b, or c.

```
[a-c] → ;
```

#### <a name="Intermixing_Transform_Rules_and_Conversion_Rules" href="#Intermixing_Transform_Rules_and_Conversion_Rules">Intermixing Transform Rules and Conversion Rules</a>

Transform rules and conversion rules may be freely intermixed. Inserting a transform rule into the middle of a set of conversion rules has an important side effect.

Normally, conversion rules are considered together as a group.  The only time their order in the rule set is important is when more than one rule matches at the same point in the string.  In that case, the one that occurs earlier in the rule set wins.  In all other situations, when multiple rules match overlapping parts of the string, the one that matches earlier wins.

Transform rules apply to the whole string.  If you have several transform rules in a row, the first one is applied to the whole string, then the second one is applied to the whole string, and so on.  To reconcile this behavior with the behavior of conversion rules, transform rules have the side effect of breaking a surrounding set of conversion rules into two groups: First all of the conversion rules before the transform rule are applied as a group to the whole string in the usual way, then the transform rule is applied to the whole string, and then the conversion rules after the transform rule are applied as a group to the whole string.  For example, consider the following rules:

```
abc → xyz;
xyz → def;
::Upper;
```

If you apply these rules to “abcxyz”, you get “XYZDEF”. If you move the “::Upper;” to the middle of the rule set and change the cases accordingly, then applying this to “abcxyz” produces “DEFDEF”.

```
abc → xyz;
::Upper;
XYZ → DEF;
```

This is because “::Upper;” causes the transliterator to reset to the beginning of the string. The first rule turns the string into “xyzxyz”, the second rule upper cases the whole thing to “XYZXYZ”, and the third rule turns this into “DEFDEF”.

This can be useful when a transform naturally occurs in multiple “passes.”  Consider this rule set:

```
[:Separator:]* → ' ';
'high school' → 'H.S.';
'middle school' → 'M.S.';
'elementary school' → 'E.S.';
```

If you apply this rule to “high school”, you get “H.S.”, but if you apply it to “high  school” (with two spaces), you just get “high school” (with one space). To have “high school” (with two spaces) turn into “H.S.”, you'd either have to have the first rule back up some arbitrary distance (far enough to see “elementary”, if you want all the rules to work), or you have to include the whole left-hand side of the first rule in the other rules, which can make them hard to read and maintain:

```
$space = [:Separator:]*;
high $space school → 'H.S.';
middle $space school → 'M.S.';
elementary $space school → 'E.S.';
```

Instead, you can simply insert “ `::Null;` ” in order to get things to work right:

```
[:Separator:]* → ' ';
::Null;
'high school' → 'H.S.';
'middle school' → 'M.S.';
'elementary school' → 'E.S.';
```

The “::Null;” has no effect of its own (the null transform, by definition, does not do anything), but it splits the other rules into two “passes”: The first rule is applied to the whole string, normalizing all runs of white space into single spaces, and then we start over at the beginning of the string to look for the phrases. “high    school” (with four spaces) gets correctly converted to “H.S.”.

This can also sometimes be useful with rules that have overlapping domains.  Consider this rule set from before:

```
sch → sh ;
ss → z ;
```

Applying this rule to “bassch” results in “bazch” because “ss” matches earlier in the string than “sch”. If you really wanted “bassh”—that is, if you wanted the first rule to win even when the second rule matches earlier in the string, you'd either have to add another rule for this special case...

```
sch → sh ;
ssch → ssh;
ss → z ;
```

...or you could use a transform rule to apply the conversions in two passes:

```
sch → sh ;
::Null;
ss → z ;
```

#### <a name="Inverse_Summary" href="#Inverse_Summary">Inverse Summary</a>

The following table shows how the same rule list generates two different transforms, where the inverse is restated in terms of forward rules (this is a contrived example, simply to show the reordering):

<!-- HTML: blocks in cells -->
<table>
<tr>
    <th>Original Rules</th>
    <th>Forward</th>
    <th>Inverse</th>
</tr>
<tr>
    <td><pre><code>:: [:Uppercase Letter:] ;
:: latin-greek ;
:: greek-japanese ;
x ↔ y ;
z → w ;
r ← m ;
:: upper;
a → b ;
c ↔ d ;
:: any-publishing ;
:: ([:Number:]) ;</code></pre></td>
    <td><pre><code>:: [:Uppercase Letter:] ;
:: latin-greek ;
:: greek-japanese ;
x → y ;
z → w ;
:: upper ;
a → b ;
c → d ;
:: any-publishing ;</code></pre></td>
    <td><pre><code>:: [:Number:] ;
:: publishing-any ;
d → c ;
:: lower ;
y → x ;
m → r ;
:: japanese-greek ;
:: greek-latin ;</code></pre></td>
</tr>
</table>

Note how the irrelevant rules (the inverse filter rule and the rules containing ←) are omitted (ignored, actually) in the forward direction, and notice how things are reversed: the transform rules are inverted and happen in the opposite order, and the groups of conversion rules are also executed in the opposite relative order (although the rules within each group are executed in the same order).

Because the order of rules matters, the following will not work as expected
```
c → s;
ch → kh;
```
The second rule can never execute, because it is "masked" by the first.
To help prevent errors, implementations should try to alert readers when this occurs, eg:
```
Rule {c > s;} masks {ch > kh;}
```

### Transform Syntax Characters

The following summarizes the syntax characters used in transforms.

| Character(s) | Description | Example |
| - | - | - |
| ;  | End of a conversion rule, variable definition, or transform rule invocation | a → b ; |
| \:\: | Invoke a transform | :: Null ; |
| (, ) | In a transform rule invocation, marks the backwards transform | :: Null (NFD); |
| $ | Mark the start of a variable, when followed by an ASCII letter | $abc |
| = | Used to define variables | $a = abc ; |
| →, \> | Transform from left to right (only for forward conversion rules) | a → b ; |
| ←, \< | Transform from right to left (only for backward conversion rules) | a ← b ; |
| ↔, \<\> | Transform from left to right (for forward) and right to left (for backward) | a ↔ b ; |
| { | Mark the boundary between before_context and the text_to_replace | a {b} c → B ; |
| } | Mark the boundary between the text_to_replace and after_context | a {b} c → B ; |
| ' | Escape one or more characters, until the next '  | '\<\>' → x ; |
| " | Escape one or more characters, until the next " | "\<\>" → x ; |
| \\ | Escape the next character | \\\<\\\> → x ; |
| # | Comment (until the end of a line) | a → ; # remove a |
| \| | In the resulting_text, moves the cursor | a → A \| b; |
| @ | In the resulting_text, filler character used to move the cursor before the start or after the end of the result | a → Ab@\|; |
| (, ) | In text_to_replace, a capturing group | ([a-b]) > &hex($1); |
| $ | In replacement_text, when followed by 1..9, is replaced by the contents of a capture group | ([a-b]) > &hex($1); |
| ^ | In a before_context, by itself, equivalent to [$] **(deprecated)** | ... |
| ? | In a before_context, after_context, or text_to_replace, a possessive quantifier for zero or one  | a?b → c ; |
| + | In a before_context, after_context, or text_to_replace, a possessive quantifier for one or more  | a+b → c ; |
| * | In a before_context, after_context, or text_to_replace, a possessive quantifier for zero or more  | a*b → c ; |
| & | Invoke a function in the replacement_text | ([a-b]) > &hex($1); |
| !, %, _, ~, -, ., / | Reserved for future syntax | ... |
| SPACE | Ignored except when quoted | a b # same as ab |
| \uXXXX | Hex notation: 4 Xs | \u0061 |
| \x{XX...} | Hex notation: 1-6 Xs | \x{61} |
| [, ] | Marks a UnicodeSet | [a-z] |
| \p{...} | Marks a UnicodeSet formed from a property | \p{di} |
| \P{...} | Marks a negative UnicodeSet formed from a property | \p{DI} |
| $ | Within a UnicodeSet (not before ASCII letter), matches the start or end of the source text (but is not replaced) | [$] b → c |
| Other | Many of these characters have special meanings inside a UnicodeSet | ... |

## <a name="ListPatterns" href="#ListPatterns">List Patterns</a>

```xml
<!ELEMENT listPatterns (alias | (listPattern*, special*)) >

<!ELEMENT listPattern (alias | (listPatternPart*, special*)) >
<!ATTLIST listPattern type (NMTOKEN) #IMPLIED >

<!ELEMENT listPatternPart ( #PCDATA ) >
<!ATTLIST listPatternPart type (start | middle | end | 2 | 3) #REQUIRED >
```

List patterns can be used to format variable-length lists of things in a locale-sensitive manner, such as "Monday, Tuesday, Friday, and Saturday" (in English) versus "lundi, mardi, vendredi et samedi" (in French). For example, consider the following example:

```xml
<listPatterns>
 <listPattern>
  <listPatternPart type="2">{0} and {1}</listPatternPart>
  <listPatternPart type="start">{0}, {1}</listPatternPart>
  <listPatternPart type="middle">{0}, {1}</listPatternPart>
  <listPatternPart type="end">{0}, and {1}</listPatternPart>
 </listPattern>
</listPatterns>
```

Each pattern satisifies the following conditions:
<ul>
    <li>it contains the placeholders <code>{0}</code>, <code>{1}</code>, and <code>{2}</code> ("3"-pattern only) in order</li>
    <li>"start" and "middle" patterns end with the <code>{1}</code> placeholder</li>
    <li>"middle" and "end" patterns begin with the <code>{0}</code> placeholder</li>
</ul>

That is,
<ul>
    <li>all patterns can have text between the placeholders</li>
    <li>only the "start", "2", and "3" patterns can have text before the first placeholder, and</li>
    <li>only the "end", "2", and "3" patterns can have text after the last placeholder.</li>
</ul>

The data is used as follows: If there is a type that matches exactly the number of elements in the desired list (such as "2" in the above list), then use that pattern. Otherwise,

1.  Format the last two elements with the "end" pattern.
2.  Then use the "middle" pattern to add on subsequent elements working towards the front, all but the very first element. That is, `{1}` is what you've already done, and `{0}` is the previous element.
3.  Then use "start" to add the front element, again with `{1}` as what you've done so far, and `{0}` is the first element.

Thus a list (a,b,c,...m, n) is formatted as: `start(a,middle(b,middle(c,middle(...end(m, n))...)))`. Alternatively, the list can also be processed front-to-back:

1. Format the first two elements with the "start" pattern.
2. Then use the "middle" pattern to add on subsequent elements working towards the back, all but the very last element. That is, `{0}` is what you've already done, and `{1}` is the next element.
3. Then use "end" to add the last element, again with `{0}` as what you've done so far, and `{1}` is the last element.

Here, the list (a,b,c,...m, n) is formatted as:  `end(middle(..., middle(start(a, b), c) ...) m) n) `. While this prefix-expression looks less suitable, it actually only requires appends,
so this algorithm can be used to write into append-only sinks. Both the back-to-front and the front-to back algorithm produce this expression:

```
start_before + a + start_between + b + middle_between + c + ... + middle_between + m + end_between + n + end_after
```

where the patters are "start": `start_before{0}start_between{1}`, "middle": `{0}middle_between{1}`, and "end": `{0}end_between{1}end_after`.

More sophisticated implementations can customize the process to improve the results for languages where context is important. For example:

<!-- HTML: rowspan, block elements in cells -->

<table><tbody>
<tr><td rowspan="3">Spanish</td><td>AND</td>
    <td>Use ‘e’ instead of ‘y’ in the listPatternPart for "end" and "2" in either of the following cases:
        <ol><li>The value substituted for {1} starts with ‘i’
                <ol><li><i>fuerte <b>e</b> indomable, </i>not <i>fuerte <b>y</b> indomable</i></li></ol>
            </li>
            <li>The value substituted for {1} starts with ‘hi’, but not with ‘hie’ or ‘hia’
                <ol><li><i>tos <b>e</b> hipo,</i> not <i>tos <b>y</b> hipo</i></li>
                    <li><i>agua <b>y</b> hielo,</i> not <i>agua <b>e</b> hielo</i></li></ol>
            </li></ol></td></tr>

<tr><td>OR</td>
    <td>Use ‘u’ instead of ‘o’ in the listPatternPart for "end" and "2" in any of the following cases:
        <ol><li>The value substituted for {1} starts with ‘o’ or ‘ho’
                <ol><li><i>delfines <b>u</b> orcas,</i> not <i>delfines <b>o</b> orcas</i></li>
                    <li><i>mañana <b>u</b> hoy,</i> not <i>mañana <b>o</b> hoy</i></li></ol>
            </li>
            <li>The value substituted for {1} starts with ‘8’
                <ol><li><i>6 <b>u</b> 8,</i> not <i>6 <b>o</b> 8</i></li></ol>
            </li>
            <li>The value substituted for {1} starts with ‘11’ where the numeric value is 11 x 10<sup>3×y</sup> (eg 11 thousand, 11.23 million, ...)
                <ol><li><i>10 <b>u</b> 11,</i> not <i>10 <b>o</b> 11</i></li>
                    <li><i>10 <b>u</b> 11.000,</i> not <i>10 <b>o</b> 11.000</i></li>
                    <li><i>10 <b>o</b> 111,</i> not <i>10 <b>u</b> 111</i></li></ol>
            </li></ol></td></tr>

<tr><td colspan="2">See <a href="https://www.rae.es/espanol-al-dia/cambio-de-la-y-copulativa-en-e-0">Cambio de la y copulativa en e</a><br><b>Note: </b>more advanced implementations may also consider the pronunciation, such as foreign words where the ‘h’ is not mute.</td></tr>

<tr><td rowspan="2">Hebrew</td><td>AND</td>
    <td>Use ‘-ו’ instead of ‘ו’ in the listPatternPart for "end" and "2" in the following case:
        <ol><li>if the value substituted for {1} starts with something other than a Hebrew letter, such as a digit (0-9) or a Latin-script letter
            <ol><li><i>one hour and two minutes =‎ ‏"שעה ושתי דקות"‏</i></li>
                <li><i>one hour and 9 minutes =‎ ‏"שעה ו-9 דקות"‏</i></li></ol>
            </li></ol></td></tr>

<tr><td colspan="2">See <a href="https://hebrew-academy.org.il/topic/hahlatot/punctuation/#target-3475">https://hebrew-academy.org.il/topic/hahlatot/punctuation/#target-3475</a></td></tr>

</tbody></table>

The following `type` attributes are in use:

| type attribute value      | Description                                                  | Examples                         |
| ------------------------- | ------------------------------------------------------------ | -------------------------------- |
| `standard` (or no `type`) | A typical 'and' list for arbitrary placeholders              | _January, February, and March_   |
| `standard-short`          | A short version of an 'and' list, suitable for use with short or abbreviated placeholder values | _Jan., Feb., and Mar._ |
| `standard-narrow`         | A yet shorter version of a short 'and' list (where possible) | _Jan., Feb., Mar._               |
| `or`                      | A typical 'or' list for arbitrary placeholders               | _January, February, or March_    |
| `or-short`                | A short version of an 'or' list                              | _Jan., Feb., or Mar._            |
| `or-narrow`               | A yet shorter version of a short 'or' list (where possible)  | _Jan., Feb., or Mar._            |
| `unit`                    | A list suitable for wide units                               | _3 feet, 7 inches_               |
| `unit-short`              | A list suitable for short units                              | _3 ft, 7 in_                     |
| `unit-narrow`             | A list suitable for narrow units, where space on the screen is very limited. | _3′ 7″_          |

In many languages there may not be a difference among many of these lists. In others, the spacing, the length or presence or a conjunction, and the separators may change.

Currently there are no locale keywords that affect list patterns; they are selected using the base locale ID, ignoring anu -u- extension keywords.

### <a name="List_Gender" href="#List_Gender">Gender of Lists</a>

```xml
<!-- Gender List support -->
<!ELEMENT gender ( personList+ ) >
<!ELEMENT personList EMPTY >
<!ATTLIST personList type ( neutral | mixedNeutral | maleTaints ) #REQUIRED >
<!ATTLIST personList locales NMTOKENS #REQUIRED >
```

This can be used to determine the gender of a list of 2 or more persons, such as "Tom and Mary", for use with gender-selection messages. For example,

```xml
<supplementalData>
    <gender>
        <!-- neutral: gender(list) = other -->
        <personList type="neutral" locales="af da en..."/>

        <!-- mixedNeutral: gender(all male) = male, gender(all female) = female, otherwise gender(list) = other -->
        <personList type="mixedNeutral" locales="el"/>

        <!-- maleTaints: gender(all female) = female, otherwise gender(list) = male -->
        <personList type="maleTaints" locales="ar ca..."/>
    </gender>
</supplementalData>
```

There are three ways the gender of a list can be formatted:

1. **neutral:** A gender-independent "other" form will be used for the list.
2. **mixedNeutral:** If the elements of the list are all male, "male" form is used for the list. If all the elements of the lists are female, "female" form is used. If the list has a mix of male, female and neutral names, the "other" form is used.
3. **maleTaints:** If all the elements of the lists are female, "female" form is used, otherwise the "male" form is used.

## <a name="Context_Transform_Elements" href="#Context_Transform_Elements">ContextTransform Elements</a>

```xml
<!ELEMENT contextTransforms ( alias | (contextTransformUsage*, special*)) >
<!ELEMENT contextTransformUsage ( alias | (contextTransform*, special*)) >
<!ATTLIST contextTransformUsage type CDATA #REQUIRED >
<!ELEMENT contextTransform ( #PCDATA ) >
<!ATTLIST contextTransform type ( uiListOrMenu | stand-alone ) #REQUIRED >
```

CLDR locale elements provide data for display names or symbols in many categories. The default capitalization for these elements is intended to be the form used in the middle of running text. In many languages, other capitalization may be required in other contexts, depending on the type of name or symbol.

Each `<contextTransformUsage>` element’s `type` attribute specifies a category of data from the table below; the element includes one or more `<contextTransform>` elements that specify how to perform capitalization of this category of data in different contexts. The `<contextTransform>` elements are needed primarily for cases in which the capitalization is other than the default form used in the middle of running text. However, it is also useful to mark cases in which it is _known_ that no transformation from this default form is needed; this may be necessary, for example, to override the transformation specified by a parent locale. The following values are currently defined for the `<contextTransform>` element:

* "titlecase-firstword" designates the case in which raw CLDR text that is in middle-of-sentence form, typically lowercase, needs to have its first word titlecased.
* "no-change" designates the case in which it is known that no change from the raw CLDR text (middle-of-sentence form) is needed.

Four contexts for capitalization behavior are currently identified. Two need no data, and hence have no corresponding `<contextTransform>` elements:

* In the middle of running text: This is the default form, so no additional data is required.
* At the beginning of a complete sentence: The initial word is titlecased, no additional data is required to indicate this.

Two other contexts require `<contextTransform>` elements if their capitalization behavior is other than the default for running text. The context is identified by the `type` attribute, as follows:

* uiListOrMenu: Capitalization appropriate to a user-interface list or menu.
* stand-alone: Capitalization appropriate to an isolated user-interface element (e.g. an isolated name on a calendar page)

Example:

```xml
<contextTransforms>
    <contextTransformUsage type="languages">
        <contextTransform type="uiListOrMenu">titlecase-firstword</contextTransform>
        <contextTransform type="stand-alone">titlecase-firstword</contextTransform>
    </contextTransformUsage>
    <contextTransformUsage type="month-format-except-narrow">
        <contextTransform type="uiListOrMenu">titlecase-firstword</contextTransform>
    </contextTransformUsage>
    <contextTransformUsage type="month-standalone-except-narrow">
        <contextTransform type="uiListOrMenu">titlecase-firstword</contextTransform>
    </contextTransformUsage>
</contextTransforms>
```

###### Table: <a name="contextTransformUsage_type_attribute_values" href="#contextTransformUsage_type_attribute_values">Element contextTransformUsage type attribute values</a>

| type attribute value             | Description |
| -------------------------------- | ----------- |
| `all`                            | Special value, indicates that the specified transformation applies to all of the categories below |
| `language`                       | `localeDisplayNames` language names |
| `script`                         | `localeDisplayNames` script names |
| `territory`                      | `localeDisplayNames` territory names |
| `variant`                        | `localeDisplayNames` variant names |
| `key`                            | `localeDisplayNames` key names |
| `keyValue`                       | `localeDisplayNames` key value type names |
| `month-format-except-narrow`     | `dates/calendars/calendar[type=*]/months` format wide and abbreviated month names |
| `month-standalone-except-narrow` | `dates/calendars/calendar[type=*]/months` stand-alone wide and abbreviated month names |
| `month-narrow`                   | `dates/calendars/calendar[type=*]/months` format and stand-alone narrow month names |
| `day-format-except-narrow`       | `dates/calendars/calendar[type=*]/days` format wide and abbreviated day names |
| `day-standalone-except-narrow`   | `dates/calendars/calendar[type=*]/days` stand-alone wide and abbreviated day names |
| `day-narrow`                     | `dates/calendars/calendar[type=*]/days` format and stand-alone narrow day names |
| `era-name`                       | `dates/calendars/calendar[type=*]/eras` (wide) era names |
| `era-abbr`                       | `dates/calendars/calendar[type=*]/eras` abbreviated era names |
| `era-narrow`                     | `dates/calendars/calendar[type=*]/eras` narrow era names |
| `quarter-format-wide`            | `dates/calendars/calendar[type=*]/quarters` format wide quarter names |
| `quarter-standalone-wide`        | `dates/calendars/calendar[type=*]/quarters` stand-alone wide quarter names |
| `quarter-abbreviated`            | `dates/calendars/calendar[type=*]/quarters` format and stand-alone abbreviated quarter names |
| `quarter-narrow`                 | `dates/calendars/calendar[type=*]/quarters` format and stand-alone narrow quarter names |
| `calendar-field`                 | `dates/fields/field[type=*]/displayName` field names<br/>(for relative forms see type "tense" below) |
| `zone-exemplarCity`              | `dates/timeZoneNames/zone[type=*]/exemplarCity` city names |
| `zone-long`                      | `dates/timeZoneNames/zone[type=*]/long` zone names |
| `zone-short`                     | `dates/timeZoneNames/zone[type=*]/short` zone names |
| `metazone-long`                  | `dates/timeZoneNames/metazone[type=*]/long` metazone names |
| `metazone-short`                 | `dates/timeZoneNames/metazone[type=*]/short` metazone names |
| `symbol`                         | `numbers/currencies/currency[type=*]/symbol` symbol names |
| `currencyName`                   | `numbers/currencies/currency[type=*]/displayName` currency names |
| `currencyName-count`             | `numbers/currencies/currency[type=*]/displayName[count=*]` currency names for use with count |
| `relative`                       | `dates/fields/field[type=*]/relative and dates/fields/field[type=*]/relativeTime` relative field names |
| `unit-pattern`                   | `units/unitLength[type=*]/unit[type=*]/unitPattern[count=*]` unit names |
| `number-spellout`                | `rbnf/rulesetGrouping[type=*]/ruleset[type=*]/rbnfrule` number spellout rules |

## <a name="Choice_Patterns" href="#Choice_Patterns">Choice Patterns</a>

A choice pattern is a string that chooses among a number of strings, based on numeric value. It has the following form:

```
<choice_pattern> = <choice> ( '|' <choice> )*
<choice> = <number><relation><string>
<number> = ('+' | '-')? ('∞' | [0-9]+ ('.' [0-9]+)?)
<relation> = '<' | ' ≤'
```

The interpretation of a choice pattern is that given a number N, the pattern is scanned from right to left, for each choice evaluating `<number> <relation> N`. The first choice that matches results in the corresponding string. If no match is found, then the first string is used. For example:

<!-- HTML: rowspan -->

<table><tbody>
<tr><th>Pattern</th><th>N</th><th>Result</th></tr>
<tr><td rowspan="4">0≤Rf|1≤Ru|1&lt;Re</td><td>-∞, -3, -1, -0.000001</td><td>Rf (defaulted to first string)</td></tr>
<tr><td>0, 0.01, 0.9999</td><td>Rf</td></tr>
<tr><td>1</td><td>Ru</td></tr>
<tr><td>1.00001, 5, 99, ∞</td><td>Re</td></tr>
</tbody></table>

Quoting is done using ' characters, as in date or number formats.

## <a name="Annotations" href="#Annotations">Annotations and Labels</a>

Annotations provide information about characters, typically used in input. For example, on a mobile keyboard they can be used to do completion. They are typically used for symbols, especially emoji characters.

For more information, see version 5.0 or [UTR #51, Unicode Emoji](https://www.unicode.org/reports/tr51/). (Note that during the period between the publication of CLDR v31 and that of Emoji 5.0, the “Latest Proposed Update” link should be used to get to the draft specification for Emoji 5.0.)

```xml
<!ELEMENT annotations ( annotation* ) >

<!ELEMENT annotation ( #PCDATA ) >

<!ATTLIST annotation cp CDATA #REQUIRED >

<!ATTLIST annotation type (tts) #IMPLIED >
```

There are two kinds of annotations: **short names**, and **keywords**.

With an attribute `type="tts"`, the value is a **short name**, such as one that can be used for text-to-speech. It should be treated as one of the element values for other purposes.

When there is no `type` attribute, the value is a set of **keywords**, delimited by |. Spaces around each element are to be trimmed. The **keywords** are words associated with the character(s) that might be used in searching for the character, or in predictive typing on keyboards. The short name itself can be used as a keyword.

Here is an example from German:

```xml
<annotation cp="👎">schlecht | Hand | Daumen | nach unten</annotation>
<annotation cp="👎" type="tts">Daumen runter</annotation>
```

The `cp` attribute value has two formats: either a single string, or if contained within \[…\] a UnicodeSet. The latter format can contain multiple code points or strings. A code point pr string can occur in multiple annotation element **cp** values, such as the following, which also contains the "thumbs down" character.

```xml
<annotation cp='[☝✊-✍👆-👐👫-👭💁🖐🖕🖖🙅🙆🙋🙌🙏🤘]'>hand</annotation>
```

Both for short names and keywords, values do not have to match between different languages. They should be the most common values that people using _that_ language would associate with those characters. For example, a "black heart" might have the association of "wicked" in English, but not in some other languages.

The cp value may contain sequences, but does not contain any Emoji or Text Variant (VS15 & VS16) characters. All such characters should be removed before looking up any short names and keywords.

### <a name="SynthesizingNames" href="#SynthesizingNames">Synthesizing Sequence Names</a>

Many emoji are represented by sequences of characters. When there are no `annotation` elements for that string, the short name can be synthesized as follows. **Note:** The process details may change after the release of this specification, and may further change in the future if other sequences are added. Please see the [Known Issues](https://cldr.unicode.org/index/downloads/cldr-41#h.qa3jolg7zi2s) section of the CLDR download page for any updates.

1.  If **sequence** is an **emoji flag sequence**, look up the territory name in CLDR for the corresponding ASCII characters and return as the short name. For example, the regional indicator symbols P+F would map to “Französisch-Polynesien” in German.
2.  If **sequence** is an **emoji tag sequence**, look up the subdivision name in CLDR for the corresponding ASCII characters and return as the short name. For example, the TAG characters gbsct would map to “Schottland” in German.
3.  If **sequence** is a keycap sequence or 🔟, use the characterLabel for "keycap" as the **prefixName** and set the **suffix** to be the sequence (or "10" in the case of 🔟), then go to step 8.
4.  If the **sequence** ends with the string ZWJ + ➡️, look up the name of that sequence with that string removed. Embed that name into the "facing-right" characterLabelPattern and return it.
5.  Let **suffix** and **prefixName** be "".
6.  If **sequence** contains any emoji modifiers, move them (in order) into **suffix**, removing them from **sequence**.
7.  If **sequence** is a "KISS", "HEART", "FAMILY", or "HOLDING HANDS" emoji ZWJ sequence, move the characters in **sequence** to the front of **suffix**, and set the **sequence** to be "💏", "💑", or "👪" respectively, and go to step 7.
    1. A KISS sequence contains ZWJ, "💋", and "❤", which are skipped in moving to **suffix**.
    2. A HEART sequence contains ZWJ and "❤", which are skipped in moving to **suffix**.
    3. A HOLDING HANDS sequence contains ZWJ+🤝+ZWJ, which are skipped in moving to **suffix**.
    4. A FAMILY sequence contains only characters from the set {👦, 👧, 👨, 👩, 👴, 👵, 👶}. Nothing is skipped in moving to **suffix**, except ZWJ.
8.  If **sequence** ends with ♂ or ♀, and does not have a name, remove the ♂ or ♀ and move the name for "👨" or "👩" respectively to the start of **prefixName**.
9.  Transform **sequence** and append to **prefixName**, by successively getting names for the longest subsequences, skipping any singleton ZWJ characters. If there is more than one name, use the listPattern for unit-short, type=2 to link them.
10.  Transform **suffix** into **suffixName** in the same manner.
11. If both the **prefixName** and **suffixName** are non-empty, form the name by joining them with the "category-list" characterLabelPattern and return it. Otherwise return whichever of them is non-empty.

The synthesized keywords can follow a similar process.

1.  For an **emoji flag sequence** or **emoji tag sequence** representing a subdivision, use "flag".
2.  For keycap sequences, use "keycap".
3.  For sequences with ZWJ + ➡️, use the keywords for the sequence without the ZWJ + ➡️.
3.  For other sequences, add the keywords for the subsequences used to get the short names for **prefixName**, and the short names used for **suffixName**.

Some examples for English data (v30) are given in the following table.

###### Table: Synthesized Emoji Sequence Names

| Sequence | Short Name | Keywords |
| --------- | ---------- | -------- |
| 🇪🇺        | European Union | flag |
| #️⃣        | keycap: # | keycap |
| 9️⃣        | keycap: 9 | keycap |
| 💏        | kiss | couple |
| 👩‍❤️‍💋‍👩 | kiss: woman, woman | couple, woman |
| 💑        | couple with heart | love, couple |
| 👩‍❤️‍👩    | couple with heart: woman, woman | love, couple, woman |
| 👪        | family | family |
| 👩‍👩‍👧        | family: woman, woman, girl | woman, family, girl |
| 👦🏻        | boy: light skin tone | young, light skin tone, boy |
| 👩🏿        | woman: dark skin tone | woman, dark skin tone |
| 👨‍⚖        | man judge | scales, justice, man |
| 👨🏿‍⚖        | man judge: dark skin tone | scales, justice, dark skin tone, man |
| 👩‍⚖        | woman judge | woman, scales, judge |
| 👩🏼‍⚖        | woman judge: medium-light skin tone | woman, scales, medium-light skin tone, judge |
| 👮        | police officer | police, cop, officer |
| 👮🏿        | police officer: dark skin tone | police, cop, officer, dark skin tone |
| 👮‍♂️       | man police officer | police, cop, officer, man |
| 👮🏼‍♂️       | man police officer: medium-light skin tone | police, cop, officer, medium-light skin tone, man |
| 👮‍♀️       | woman police officer | police, woman, cop, officer |
| 👮🏿‍♀️       | woman police officer: dark skin tone | police, woman, cop, officer, dark skin tone |
| 🚴        | person biking | cyclist, bicycle, biking |
| 🚴🏿        | person biking: dark skin tone | cyclist, bicycle, biking, dark skin tone |
| 🚴‍♂️       | man biking | cyclist, bicycle, biking, man |
| 🚴🏿‍♂️       | man biking: dark skin tone | cyclist, bicycle, biking, dark skin tone, man |
| 🚴‍♀️       | woman biking | cyclist, woman, bicycle, biking |
| 🚴🏿‍♀️       | woman biking: dark skin tone | cyclist, woman, bicycle, biking, dark skin tone |

For more information, see [Unicode Emoji](https://www.unicode.org/reports/tr51/).

### <a name="Character_Labels" href="#Character_Labels">Annotations Character Labels</a>

```xml
<!ELEMENT characterLabels ( alias | ( characterLabelPattern*, characterLabel*, special* ) ) >

<!ELEMENT characterLabelPattern ( #PCDATA ) >

<!ATTLIST characterLabelPattern type NMTOKEN #REQUIRED >

<!ATTLIST characterLabelPattern count (0 | 1 | zero | one | two | few | many | other) #IMPLIED > <!-- count only used for certain patterns" -->

<!ELEMENT characterLabel ( #PCDATA ) >

<!ATTLIST characterLabel type NMTOKEN #REQUIRED >
```

The character labels can be used for categories or groups of characters in a character picker or keyboard palette. They have the above structure. Items with special meanings are explained below. Many of the categories are based on terms used in Unicode. Consult the [Unicode Glossary](https://www.unicode.org/glossary/) where the meaning is not clear.

The following are special patterns used in composing labels.

###### Table: characterLabelPattern

| Type          | English             | Description of the group specified |
| ------------- | ------------------- | ----------------------------------- |
| all           | {0} — all           | Used where the title {0} is just a subset. For example, {0} might be "Latin", and contain the most common Latin characters. Then "Latin — all" would be all of them. |
| category-list | {0}: {1}            | Use for a name, where {0} is the main item like "Family", and {1} is a list of one or more components or subcategories. The list is formatted using a list pattern. |
| compatibility | {0} — compatibility | For grouping Unicode compatibility characters separately, such as "Arabic — compatibility". |
| enclosed      | {0} — enclosed      | For indicating enclosed forms, such as "digits — enclosed" |
| extended      | {0} — extended      | For indicating a group of "extended" characters (special use, technical, etc.) |
| historic      | {0} — historic      | For indicating a group of "historic" characters (no longer in common use). |
| miscellaneous | {0} — miscellaneous | For indicating a group of "miscellaneous" characters (typically that don't fall into a broader class). |
| other         | {0} — other         | Used where the title {0} is just a subset. For example, {0} might be "Latin", and contain the most common Latin characters. Then "Latin — other" would be the rest of them. |
| scripts       | scripts — {0}       | For indicating a group of "scripts" characters matching {0}. The value for {0} may be a geographic indicator, like "Africa" (although there are specific combinations listed below), or some other designation, like "other" (from below). |
| strokes       | {0} strokes         | Used as an index title for CJK characters. It takes a "count" value, which allows the right plural form to be specified for the language. |
| subscript     | subscript {0}       | For indicating subscript forms, such as "subscript digits". |
| superscript   | superscript {0}     | For indicating superscript forms, such as "superscript digits". |

The following are character labels. Where the meaning of the label is fairly clear (like "animal") or is in the Unicode glossary, it is omitted.

###### Table: characterLabel

| Type                        | English                 | Description of the group specified |
| --------------------------- | ----------------------- | ----------------------------------- |
| activities                  | activity                | Human activities, such as running. |
| african_scripts             | African script          | Scripts associated with the continent of Africa. |
| american_scripts            | American script         | Scripts associated with the continents of North and South America. |
| animals_nature              | animal or nature        | A broad category. |
| arrows                      | arrow                   | Arrow symbols |
| body                        | body                    | Symbols for body parts, such as an arm. |
| box_drawing                 | box drawing             | Unicode box-drawing characters (geometric shapes) |
| bullets_stars               | bullet or star          | Unicode bullets (such as • or ‣ or ⁍) or stars (★✩✪✵...) |
| consonantal_jamo            | consonantal jamo        | Korean Jamo consonants. |
| currency_symbols            | currency symbol         | Symbols such as $, ¥, £ |
| dash_connector              | dash or connector       | Characters like _ or ⁓ |
| dingbats                    | dingbat                 | Font dingbat characters, such as ❿ or ♜. |
| downwards_upwards_arrows    | downwards upwards arrow | ⇕,... |
| female                      | female                  | Indicates that a character is female or feminine in appearance. |
| format                      | format                  | A Unicode format character. |
| format_whitespace           | format & whitespace     | A Unicode format character or whitespace. |
| full_width_form_variant     | full-width variant      | Full width variant, such as a wide A. |
| half_width_form_variant     | half-width variant      | Narrow width variant, such as a half-width katakana character. |
| han_characters              | Han character           | Han (aka CJK: Chinese, Japanese, or Korean) ideograph |
| han_radicals                | Han radical             | Radical (component) used in Han characters. |
| hanja                       | hanja                   | Korean name for Han character. |
| hanzi_simplified            | Hanzi (simplified)      | Simplified Chinese ideograph |
| hanzi_traditional           | Hanzi (traditional)     | Traditional Chinese ideograph |
| historic_scripts            | historic script         | Script no longer in common modern usage, such as Runes or Hieroglyphs. |
| ideographic_desc_characters | ideographic desc. character | Special Unicode characters (see the glossary). |
| kanji                       | kanji                   | Japanese Han ideograph |
| keycap                      | keycap                  | A key on a computer keyboard or phone. For example, the "3" key on a phone or laptop would be "keycap: 3" |
| limited_use                 | limited-use             | Not in common modern use. |
| male                        | male                    | Indicates that a character is male or masculine in appearance. |
| modifier                    | modifier                | A Unicode modifier letter or symbol. |
| nonspacing                  | nonspacing              | Used for characters that occupy no width by themselves, such as the ¨ over the a in ä. |
| facing-left                 | facing-left             | Characters that face to the left. Also used to construct names for emoji variants. |
| facing-right                | facing-right            | Characters that face to the right. Also used to construct names for emoji variants. |

### <a name="Typographic_Names" href="#Typographic_Names">Typographic Names</a>

```xml
<!ELEMENT typographicNames ( alias | ( axisName*, styleName*, featureName*, special* ) ) >

<!ELEMENT axisName ( #PCDATA ) >
<!ATTLIST axisName type (ital | opsz | slnt | wdth | wght) #REQUIRED >
<!ATTLIST axisName alt NMTOKENS #IMPLIED >

<!ELEMENT styleName ( #PCDATA ) >
<!ATTLIST styleName type (ital | opsz | slnt | wdth | wght) #REQUIRED >
<!ATTLIST styleName subtype NMTOKEN #REQUIRED >
<!ATTLIST styleName alt NMTOKENS #IMPLIED >

<!ELEMENT featureName ( #PCDATA ) >
<!ATTLIST featureName type (afrc | cpsp | dlig | frac | lnum | onum | ordn | pnum | smcp | tnum | zero) #REQUIRED >
<!ATTLIST featureName alt NMTOKENS #IMPLIED >
```

The typographic names provide for names of font features for use in a UI. This is useful for apps that show the name of font styles and design axes according to the user’s languages. It would also be useful for system-level libraries.

The identifiers (types) use the tags from the [OpenType Feature Tag Registry](https://learn.microsoft.com/en-us/typography/opentype/spec/featuretags). Given their large number, only the names of frequently-used OpenType feature names are available in CLDR. (Many features are not user-visible settings, but instead serve as a data channel for software to pass information to the font.) The example below shows an approach for using the CLDR data. Of course, applications are free to implement their own algorithms depending on their specific needs.

To find a localized subfamily name such as “Extraleicht Schmal” for a font called “Extralight Condensed”, a system or application library might do the following:

1. Determine the set of languages in which the subfamily name can potentially be returned. This is the union of the languages for which the font contains ‘name’ table entries with ID 2 or 17, plus the languages for which CLDR supplies typographic names.

2. Use a language matching algorithm such as in ICU to find the best available language given the user preferences. The resulting subfamily name will be localized to this language.

3. If the font’s ‘name’ table contains a typographic subfamily name (ID17) in this language and all font variation axes are set to their defaults, return this name.

4. If the font’s ‘name’ table contains a font subfamilyname (‘name’ID2) in this language and all font variation axes are set to their defaults, return this name.

5. If the font has a style attributes (STAT) table, look up the design axis tags and their ordering. If the font has no STAT table, assume \[Width, Weight, Slant\] as axis ordering, and infer the font’s style attributes from other available data in the font (eg. the OS/2 table).

6. For each design axis, find a localized style name for its value.
   1. If the font’s style attributes point to a ‘name’ table entry that is available in the result language, use this name.
   2. Otherwise, generate a fallback name from CLDR style Name data.
      1. The type key is the OpenType axis tag (‘wght’). The subtype and alt keys are taken from the entry in English CLDR where the string is equal to the English name in the font. For example, when the font uses a weight whose English style name is “Extralight”, this will lead to subtype = “200” and alt = “variant”. If there is no match, take the axis value (“200”) for subtype and the empty string for alt.
      2. Look up (type, subtype) in a data table derived from CLDR’s style names. If CLDR supplies multiple alternate names for this (type, subtype), use the one whose “alt” key is matching; otherwise, use the default alternate (which has no “alt” attribute in CLDR).
7. Concatenate the strings, with a separator between them.

## <a name="Grammatical_Features" href="#Grammatical_Features">Grammatical Features</a>

LDML supplies grammatical information that can be used to distinguish localized forms on a per-locale basis. The current data is part of an initial phase; the longer term plan is to add structure to permit localized forms based on these features, starting with measurement units such as the dative form in Serbian of “kilometer”. That will allow unit values to be inserted as placeholders into messages and adopt the right forms for grammatical agreement.

The current data includes the following:

*   There are currently 3 grammatical features found in the [DTD](https://github.com/unicode-org/cldr/blob/main/common/dtd/ldmlSupplemental.dtd#1254): Gender, Case, Definiteness
*   There are mappings from supported locales to grammatical features they exhibit in the file [grammaticalFeatures.xml](https://github.com/unicode-org/cldr/blob/main/common/supplemental/grammaticalFeatures.xml). Note that this is supplemental data, so the inheritance to the available locales needs to be done by the client.

Note that the CLDR plural categories overlap some of these features, since some languages use case and other devices to change words based on the numeric values.

## Features

```xml
<!ELEMENT grammaticalData ( grammaticalFeatures*, grammaticalDerivations*) >
<!ELEMENT grammaticalFeatures ( grammaticalCase*, grammaticalGender*, grammaticalDefiniteness* ) >
<!ATTLIST grammaticalFeatures targets NMTOKENS #REQUIRED >
<!ATTLIST grammaticalFeatures locales NMTOKENS #REQUIRED >

<!ELEMENT grammaticalCase EMPTY>
<!ATTLIST grammaticalCase scope NMTOKENS #IMPLIED >
<!ATTLIST grammaticalCase values NMTOKENS #REQUIRED >

<!ELEMENT grammaticalGender EMPTY>
<!ATTLIST grammaticalGender scope NMTOKENS #IMPLIED >
<!ATTLIST grammaticalGender values NMTOKENS #REQUIRED >

<!ELEMENT grammaticalDefiniteness EMPTY>
<!ATTLIST grammaticalDefiniteness scope NMTOKENS #IMPLIED >
<!ATTLIST grammaticalDefiniteness values NMTOKENS #REQUIRED >
```

The @targets attribute contains the specific grammatical entities to which the features apply, such as ```nominal``` when they apply to nouns only. The @locales attribute contains the specific locales to which the features apply, such as ```de fr``` for German and French.

The @scope attribute, if present, indicates that the values are limited to a specific subset for certain kinds of entities. For example, a particular language might have an animate gender for nouns, but no units of measurement ever have that case; in another language, the language might have a rich set of grammatical cases, but units are invariant. If the @scope attribute is not present, then that has the meaning of "everything else".

The @scope attributes are targeted at messages created by computers, thus a feature may have a narrower scope if for all practical purposes the feature value is not used in messages created by computers. For example, it may be possible in theory for a kilogram to be in the vocative case (English poetry might have “O Captain! my Captain!/ our fearful trip is done”, but on computers you have little call to need the message “O kilogram! my kilogram! …”).

**Constraints:**

* a scope attribute is only used when there is a corresponding “general” element, one for the same language and target without a scope attribute.
* the scope attribute values must be narrower (a proper subset, possibly empty) of those in the corresponding general element.

### <a name="Gender" href="#Gender">Gender</a>

Feature that classifies nouns in classes.
This is grammatical gender, which may be assigned on the basis of sex in some languages, but may be completely separate in others.
Also used to tag elements in CLDR that should agree with a particular gender of an associated noun.
(adapted from: [linguistics-ontology.org/gold/2010/GenderProperty](http://linguistics-ontology.org/gold/2010/GenderProperty))

The term "gender" is somewhat of a misnomer, because CLDR treats "gender" as a broad term, equivalent to "noun class".
Thus it bundles noun class categories such as gender and animacy into a single identifier, such as "feminine-animate".

#### Example

```xml
<grammaticalFeatures targets="nominal" locales="es fr it pt">
   <grammaticalGender values="masculine feminine"/>
```

#### Table: Values

| Value     | Definition | References |
| --------- | ---------- | ---------- |
| animate   | In an animate/inanimate gender system, gender that denotes human or animate entities. | description adapted from: [wikipedia.org/wiki/Grammatical_gender](https://en.wikipedia.org/wiki/Grammatical_gender), [linguistics-ontology.org/gold/2010/AnimateGender](http://linguistics-ontology.org/gold/2010/AnimateGender) |
| inanimate | In an animate/inanimate gender system, gender that denotes object or inanimate entities .| adapted from: [wikipedia.org/wiki/Grammatical_gender](https://en.wikipedia.org/wiki/Grammatical_gender), [linguistics-ontology.org/gold/2010/InanimateGender](http://linguistics-ontology.org/gold/2010/InanimateGender) |
| personal  | In an animate/inanimate gender system in some languages, gender that specifies the masculine gender of animate entities. | adapted from: [wikipedia.org/wiki/Grammatical_gender](https://en.wikipedia.org/wiki/Grammatical_gender), [linguistics-ontology.org/gold/2010/HumanGender](http://linguistics-ontology.org/gold/2010/HumanGender) |
| common    | In a common/neuter gender system, gender that denotes human entities. | adapted from: [wikipedia.org/wiki/Grammatical_gender](https://en.wikipedia.org/wiki/Grammatical_gender) |
| feminine  | In a masculine/feminine or in a masculine/feminine/neuter gender system, gender that denotes specifically female persons (or animals) or that is assigned arbitrarily to object. | adapted from: https://en.wikipedia.org/wiki/Grammatical_gender, [linguistics-ontology.org/gold/2010/FeminineGender](http://linguistics-ontology.org/gold/2010/FeminineGender) |
| masculine | In a masculine/feminine or in a masculine/feminine/neuter gender system, gender that denotes specifically male persons (or animals) or that is assigned arbitrarily to object. | adapted from: [wikipedia.org/wiki/Grammatical_gender](https://en.wikipedia.org/wiki/Grammatical_gender), [linguistics-ontology.org/gold/2010/MasculineGender](http://linguistics-ontology.org/gold/2010/MasculineGender) |
| neuter    | In a masculine/feminine/neuter or common/neuter gender system, gender that generally denotes an object. | adapted from: [wikipedia.org/wiki/Grammatical_gender](https://en.wikipedia.org/wiki/Grammatical_gender), [linguistics-ontology.org/gold/2010/NeuterGender](http://linguistics-ontology.org/gold/2010/NeuterGender) |

There are further simplifications in the identifiers.
For example, consider a language that has 3 genders, and two levels of animacy, but only for masculine.
The set of combinations would be:

* masculine-animate
* masculine-inanimate
* feminine-unspecified
* neuter-unspecified

In such a case as this, CLDR abbreviates these as the following identifiers:

* masculine
* inanimate
* feminine
* neuter

That is:
* unspecified and animate are dropped.
* if there is only a single gender with inanimate, then the gender is dropped.

### <a name="Case" href="#Case">Case</a>

#### Table: Case

Feature that encodes the syntactic (and sometimes semantic) relationship of a noun with the other constituents of the sentence. (adapted from [linguistics-ontology.org/gold/2010/CaseProperty](http://linguistics-ontology.org/gold/2010/CaseProperty))

##### Example

```xml
<grammaticalFeatures targets="nominal" locales="de">
   <grammaticalCase values="nominative accusative genitive dative"/>
```

###### Table: Values

| Value              | Definition | References |
| ------------------ | ---------- | ---------- |
| abessive          | The abessive case expresses the absence of the referent it marks. It has the meaning of 'without'. | [purl.org/olia/olia.owl#AbessiveCase](https://purl.org/olia/olia.owl#AbessiveCase) [linguistics-ontology.org/gold/2010/AbessiveCase](http://linguistics-ontology.org/gold/2010/AbessiveCase)|
| ablative           | The ablative case expresses that the referent of the noun it marks is the location from which another referent is moving. It has the meaning 'from'. | [purl.org/olia/olia.owl#AblativeCase](https://purl.org/olia/olia.owl#AblativeCase), [linguistics-ontology.org/gold/2010/AblativeCase](http://linguistics-ontology.org/gold/2010/AblativeCase) |
| accusative         | Accusative case marks certain syntactic functions, usually direct objects. | [purl.org/olia/olia.owl#Accusative](https://purl.org/olia/olia.owl#Accusative), [linguistics-ontology.org/gold/2010/AccusativeCase](http://linguistics-ontology.org/gold/2010/AccusativeCase) |
| adessive  | The adessive case expresses that the referent of the noun it marks is the location near/at which another referent exists. It has the meaning of 'at' or 'near'. | [purl.org/olia/olia.owl#AdessiveCase](https://purl.org/olia/olia.owl#AdessiveCase), [linguistics-ontology.org/gold/2010/AdessiveCase](http://linguistics-ontology.org/gold/2010/AdessiveCase) |
| allative | The allative case expresses motion to or toward the referent of the noun it marks. | [purl.org/olia/olia.owl#AllativeCase](https://purl.org/olia/olia.owl#AllativeCase), [linguistics-ontology.org/gold/2010/AllativeCase](http://linguistics-ontology.org/gold/2010/AllativeCase) |
| causal | The causal (causal-final, not causative) case expresses that the marked noun is the objective or reason for something. It carries the meaning of 'for the purpose of'. | https://en.wikipedia.org/wiki/Causative#Causal-final_case, http://www.hungarianreference.com/Nouns/%C3%A9rt-causal-final.aspx |
| comitative         | Comitative Case expresses accompaniment. It carries the meaning 'with' or 'accompanied by' . | [purl.org/olia/olia.owl#ComitativeCase](https://purl.org/olia/olia.owl#ComitativeCase), [linguistics-ontology.org/gold/2010/ComitativeCase](http://linguistics-ontology.org/gold/2010/ComitativeCase) |
| dative             | Dative case marks indirect objects (for languages in which they are held to exist), or nouns having the role of a recipient (as of things given), a beneficiary of an action, or a possessor of an item. | [purl.org/olia/olia.owl#DativeCase](https://purl.org/olia/olia.owl#DativeCase), [linguistics-ontology.org/gold/2010/DativeCase](http://linguistics-ontology.org/gold/2010/DativeCase) |
| delative | The delative case expresses motion downward from the referent of the noun it marks. | [purl.org/olia/olia.owl#DelativeCase](https://purl.org/olia/olia.owl#DelativeCase), [linguistics-ontology.org/gold/2010/DelativeCase](http://linguistics-ontology.org/gold/2010/DelativeCase) |
| elative | The elative case expresses that the referent of the noun it marks is the location out of which another referent is moving. It has the meaning 'out of'. | [purl.org/olia/olia.owl#ElativeCase](https://purl.org/olia/olia.owl#ElativeCase), [linguistics-ontology.org/gold/2010/ElativeCase](http://linguistics-ontology.org/gold/2010/ElativeCase) |
| ergative           | In ergative-absolutive languages, the ergative case identifies the subject of a transitive verb. | [purl.org/olia/olia.owl#ErgativeCase](https://purl.org/olia/olia.owl#ErgativeCase), [linguistics-ontology.org/gold/2010/ErgativeCase](http://linguistics-ontology.org/gold/2010/ErgativeCase) |
| essive | The essive case expresses that the referent of the noun it marks is the location at which another referent exists. | [purl.org/olia/olia.owl#EssiveCase](https://purl.org/olia/olia.owl#EssiveCase), [linguistics-ontology.org/gold/2010/EssiveCase](http://linguistics-ontology.org/gold/2010/EssiveCase) |
| genitive           | Genitive case signals that the referent of the marked noun is the possessor of the referent of another noun, e.g. "the man's foot". In some languages, genitive case may express an associative relation between the marked noun and another noun. | [purl.org/olia/olia.owl#GenitiveCase](https://purl.org/olia/olia.owl#GenitiveCase), [linguistics-ontology.org/gold/2010/GenitiveCase](http://linguistics-ontology.org/gold/2010/GenitiveCase) |
| illative | The illative case expresses that the referent of the noun it marks is the location into which another referent is moving. It has the meaning 'into'. | [purl.org/olia/olia.owl#IllativeCase](https://purl.org/olia/olia.owl#IllativeCase), [linguistics-ontology.org/gold/2010/IllativeCase](http://linguistics-ontology.org/gold/2010/IllativeCase) |
| inessive  | The inessive case expresses that the referent of the noun it marks is the location within which another referent exists. It has the meaning of 'within' or 'inside'.  | [purl.org/olia/olia.owl#InessiveCase](https://purl.org/olia/olia.owl#InessiveCase), [linguistics-ontology.org/gold/2010/InessiveCase](http://linguistics-ontology.org/gold/2010/InessiveCase) |
| instrumental       | The instrumental case indicates that the referent of the noun it marks is the means of the accomplishment of the action expressed by the clause. | [purl.org/olia/olia.owl#InstrumentalCase](https://purl.org/olia/olia.owl#InstrumentalCase), [linguistics-ontology.org/gold/2010/InstrumentalCase](http://linguistics-ontology.org/gold/2010/InstrumentalCase) |
| locative           | Case that indicates a final location of action or a time of the action. | [purl.org/olia/olia.owl#LocativeCase](https://purl.org/olia/olia.owl#LocativeCase), [linguistics-ontology.org/gold/2010/LocativeCase](http://linguistics-ontology.org/gold/2010/LocativeCase) |
| locativecopulative | Copulative Case marker that indicates a location. | TBD Add reference, example |
| nominative         | In nominative-accusative languages, nominative case marks clausal subjects and is applied to nouns in isolation | [purl.org/olia/olia.owl#Nominative](https://purl.org/olia/olia.owl#Nominative), [linguistics-ontology.org/gold/2010/NominativeCase](http://linguistics-ontology.org/gold/2010/NominativeCase) |
| oblique            | Case that is used when a noun is the object of a verb or a proposition, except for nominative and vocative case. | [purl.org/olia/olia.owl#ObliqueCase](https://purl.org/olia/olia.owl#ObliqueCase) |
| partitive          | The partitive case is a grammatical case which denotes 'partialness', 'without result', or 'without specific identity'. | [purl.org/olia/olia.owl#PartitiveCase](https://purl.org/olia/olia.owl#PartitiveCase), [linguistics-ontology.org/gold/2010/PartitiveCase](http://linguistics-ontology.org/gold/2010/PartitiveCase) |
| prepositional      | Prepositional case refers to case marking that only occurs in combination with prepositions. | [purl.org/olia/olia.owl#PrepositionalCase](https://purl.org/olia/olia.owl#PrepositionalCase) |
| sociative          | Case related to the person in whose company the action is carried out, or to any belongings of people which take part in the action. | [purl.org/olia/olia.owl#SociativeCase](https://purl.org/olia/olia.owl#SociativeCase) |
| sublative  | The sublative case expresses that the referent of the noun it marks is the location under which another referent is moving toward. It has the meaning 'towards the underneath of'. | [purl.org/olia/olia.owl#SublativeCase](https://purl.org/olia/olia.owl#SublativeCase), [linguistics-ontology.org/gold/2010/SublativeCase](http://linguistics-ontology.org/gold/2010/SublativeCase) |
| superessive  | The superessive case expresses that the referent of the noun it marks is the location on which another referent exists. It has the meaning of 'on' or 'upon'. | [purl.org/olia/olia.owl#SuperessiveCase](https://purl.org/olia/olia.owl#SuperessiveCase), [linguistics-ontology.org/gold/2010/SuperessiveCase](http://linguistics-ontology.org/gold/2010/SuperessiveCase) |
| terminative  | The terminative case expresses the motion of something into but not further than (ie, not through) the referent of the noun it marks. It has the meaning 'into but not through'.  | [purl.org/olia/olia.owl#TerminativeCase](https://purl.org/olia/olia.owl#TerminativeCase), [linguistics-ontology.org/gold/2010/TerminativeCase](http://linguistics-ontology.org/gold/2010/TerminativeCase) |
| translative  | The translative case expresses that the referent of the noun that it marks is the result of a process of change. It has the meaning of 'becoming' or 'changing into'.  | [purl.org/olia/olia.owl#TranslativeCase](https://purl.org/olia/olia.owl#TranslativeCase), [linguistics-ontology.org/gold/2010/TranslativeCase](http://linguistics-ontology.org/gold/2010/TranslativeCase) |
| vocative           | Vocative case marks a noun whose referent is being addressed. | [purl.org/olia/olia.owl#VocativeCase](https://purl.org/olia/olia.owl#VocativeCase), [linguistics-ontology.org/gold/2010/VocativeCase](http://linguistics-ontology.org/gold/2010/VocativeCase) |

### Definiteness

Feature that encodes the fact that a noun has been already mentioned, or is familiar in the discourse. (adapted from [https://glossary.sil.org/term/definiteness](https://glossary.sil.org/term/definiteness))

#### Table: Values

| Value       | Definition | References |
| ----------- | ---------- | ---------- |
| definite    | Value referring to the capacity of identification of an entity. | [purl.org/olia/olia.owl#Definite](https://purl.org/olia/olia.owl#Definite) |
| indefinite  | An entity is specified as indefinite when it refers to a non-particularized individual of the species denoted by the noun. | [purl.org/olia/olia.owl#Indefinite](https://purl.org/olia/olia.owl#Indefinite) |
| construct   | The state of the first noun in a genitive phrase of a possessed noun followed by a possessor noun. | Not directly linked, but explained under: [purl.org/olia/olia-top.owl#DefinitenessFeature](https://purl.org/olia/olia-top.owl#DefinitenessFeature) |
| unspecified | Noun without any definiteness marking in some specific construction (specific to Danish). |   |


## <a name="Grammatical_Derivations" href="#Grammatical_Derivations">Grammatical Derivations</a>

```xml
<!ELEMENT grammaticalData ( grammaticalFeatures*, grammaticalDerivations*) >
<!ELEMENT grammaticalDerivations (deriveCompound*, deriveComponent*) >
<!ATTLIST grammaticalDerivations locales NMTOKENS #REQUIRED >

<!ELEMENT deriveCompound EMPTY >
<!ATTLIST deriveCompound feature NMTOKENS #REQUIRED >
<!ATTLIST deriveCompound structure NMTOKENS #REQUIRED >
<!ATTLIST deriveCompound value NMTOKEN #REQUIRED >

<!ATTLIST deriveComponent feature NMTOKENS #REQUIRED >
<!ATTLIST deriveComponent structure NMTOKENS #REQUIRED >
<!ATTLIST deriveComponent value0 NMTOKEN #REQUIRED >
<!ATTLIST deriveComponent value1 NMTOKEN #REQUIRED >
```

The grammatical derivation data contains information about the case, gender, and plural categories of compound units. This is supplemental data, so the inheritance by locale needs to be done by the client.

_Note: In CLDR v38, the data for two locales is provided so that implemenations can ready their code for when more locale data is available. In subsequent releases structure may be further extended as more locales are added, to deal with additional locale requirements._

A compound unit can use 4 mechanisms, illustrated here in formatted strings:

* **Prefix**: 1  **kilo**gram
* **Power**: 3 **square** kilometers
* **Per**: 3 kilograms **per** meter
  * An edge case is where there is no numerator, such as “1 per-second”
* **Times**: 3 kilowatt<strong>-</strong>hours

For the purposes of grammatical derivation (and name construction), a compound unit ID can be represented as a tree structure where the leaves are the atomic units, and the higher level node are one of the above. Here is an extreme example of that: _kilogram-square-kilometer-ampere-candela-per-square-second-mole_

<!-- HTML: colspan -->

<table><tbody>
<tr><th colspan="6">per</th></tr>
<tr><th colspan="4">times</th><th colspan="2">times</th></tr>
<tr><th>kilo</th><th>square</th><td>ampere</td><td>candela</td><th>square</th><td>mole</td></tr>
<tr><td>gram</td><th>kilo</th><td>-</td><td>-</td><td>second</td><td></td></tr>
<tr><td>-</td><td>meter</td><td>-</td><td>-</td><td colspan="2">-</td></tr>
</tbody></table>

Note that the prefix and power nodes are unary (exactly 1 child), the per pattern is unary or binary (1 or 2 children), and the times pattern is n-ary (where n > 1).

Each section below is only applicable if the language has more than one value _for units_: for example, for plural categories the language has to have more than just "other". When that information is available for a language, it is found in **[Grammatical Features](#Grammatical_Features)**.

The gender derivation would be appropriate for an API call like `String genderValue = getGrammaticalGender(locale, "kilogram-meter-per-square-second")`. This can be used where the choice of word forms in the rest of a phrase can depend on the gender of the unit.

On the other hand, the derivation of plural category and case are used in building up the name of a compound unit, where the desired plural category is available from the number to be formatted with the unit, and the case value is known from the position in a message. For example, the case could be accusative if the formatted unit is to be the direct object in a sentence or phrase. This could be expressed in an API call such as `String inflectedName = getUnitName(locale, "kilogram-meter-per-square-second", pluralCategory, caseValue)`.

When deriving an inflected compound unit pattern, as the tree-stucture is processed by getting the appropriate localized patterns for the structural components and names for the atomic components. The computation of the plural category and the case of the subtrees can be computed from the **deriveComponent** data. The **times** data is treated as binary, and applied from left to right: with the example from above, the plural categories for the components of _kilogram-square-kilometer-ampere-candela_ are computed by applying

**times**(_kilogram, **times**(square-kilometer, **times**(ampere, candela)))_

For a description of how to use these fields to construct a localized name, see **[Compound Units](#compound-units)**.

### <a name="gender_compound_units" href="#gender_compound_units">Deriving the Gender of Compound Units</a>

The **deriveCompound\[@feature="gender"\]** data provides information for how to derive the gender of the whole compound from the gender of its atomic units and structure. The `attributeValues` of value are: **`0` (=gender of the first element), `1` (=gender of second element), or one of the valid gender values for the language.** In the unusual case that the 'per' compound has no first element and 0 is supplied, then the value is 1.

Example:

```xml
<deriveCompound feature="gender" structure="per" value="0" /> <!-- gender(gram-per-meter) ← gender(gram) -->
<deriveCompound feature="gender" structure="times" value="1" /> <!-- gender(newton-meter) ← gender(meter) -->
<deriveCompound feature="gender" structure="power" value="0" /> <!-- gender(square-meter) ← gender(meter) -->
<deriveCompound feature="gender" structure="prefix" value="0" /> <!-- gender(kilometer) ← gender(meter) -->
```

For example, for gram-per-meter, the first line above means:

* The gender of the compound is the gender of the first component of the 'per', that is, of the "gram". So if gram is feminine in that language, the gender of the compound is feminine.


### <a name="plural_compound_units" href="#plural_compound_units">Deriving the Plural Category of Unit Components</a>

The `deriveComponent[@feature="plural"]` data provides information for how to derive the plural category for each of the atomic units, from the plural category of the whole compound and the structure of the compound. The `attributeValues` of `value0` and `value1` are: `compound` (=the `pluralCategory` of the compound), or one of the valid plural category values for the language.

Example:

```xml
<deriveComponent feature="plural" structure="per" value0="compound" value1="one" /> <!-- compound(gram-per-meter) ⇒  compound(gram) “per" singular(meter) -->
<deriveComponent feature="plural" structure="times" value0="one"  value1="compound" />  <!-- compound(newton-meter) ⇒  singular(newton) “-" compound(meter) -->
<deriveComponent feature="plural" structure="power" value0="one"  value1="compound" />  <!-- compound(square-meter) ⇒  singular(square) compound(meter) -->
<deriveComponent feature="plural" structure="prefix" value0="one"  value1="compound" /> <!-- compound(kilometer) ⇒  singular(kilo) compound(meter) -->
```

For example, for gram-per-meter, the first line above means:

*   When the plural form of gram-per-meter is needed (rather than singular), then the gram part of the translation has to have a plural form like “grams”, while the meter part of the translation has to have a singular form like “metre”. This would be composed with the pattern for "per" (say "{0} pro {1}") to get "grams pro metre".


### <a name="case_compound_units" href="#case_compound_units">Deriving the Case of Unit Components</a>

The `deriveComponent[@feature="case"]` data provides information for how to derive the grammatical case for each of the atomic units, from the grammatical case of the whole compound and the structure of the compound. The `attributeValues` of value0 and value1 are: `compound` (=the grammatical case of the compound), or one of the valid grammatical case values for the language.

Example:

```xml
<deriveComponent feature="case" structure="per" value0="compound" value1="nominative" /> <!-- compound(gram-per-meter) ⇒ compound(gram) “per" accusative(meter) -->
<deriveComponent feature="case" structure="times" value0="nominative"  value1="compound" /> <!-- compound(newton-meter) ⇒  nominative(newton) “-" compound(meter) -->
<deriveComponent feature="case" structure="power" value0="nominative"  value1="compound" /> <!-- compound(square-meter) ⇒  nominative(square) compound(meter) -->
<deriveComponent feature="case" structure="prefix" value0="nominative"  value1="compound" /><!--compound(kilometer) ⇒  nominative(kilo) compound(meter) -->
```

For example, for gram-per-meter, the first line above means:

* When the accusative form of gram-per-meter is needed, then the gram part of the translation has the accusative case (eg, “gramu”, in a language that marks the accusative case with 'u'), while the meter part of the translation has a nominative form like “metre”. This would be composed with the pattern for "per" (say "{0} pro {1}") to get "gramu pro metre".

* * *

Copyright © 2001–2024 Unicode, Inc. All Rights Reserved. The Unicode Consortium makes no expressed or implied warranty of any kind, and assumes no liability for errors or omissions. No liability is assumed for incidental and consequential damages in connection with or arising out of the use of the information or programs contained or accompanying this technical report. The Unicode [Terms of Use](https://www.unicode.org/copyright.html) apply.

Unicode and the Unicode logo are trademarks of Unicode, Inc., and are registered in some jurisdictions.

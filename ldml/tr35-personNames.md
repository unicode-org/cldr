## Unicode Technical Standard #35

# Unicode Locale Data Markup Language (LDML)<br/>Part 8: Person Names

|Version|43 (draft)              |
|-------|------------------------|
|Editors|Mark Davis, Peter Edberg,  Rich Gillam, Alex Kolisnychenko, Mike McKenna and <a href="tr35.md#Acknowledgments">other CLDR committee members</a>|

For the full header, summary, and status, see [Part 1: Core](tr35.md).

### _Summary_

This document describes parts of an XML format (_vocabulary_) for the exchange of structured locale data. This format is used in the [Unicode Common Locale Data Repository](https://www.unicode.org/cldr/).

This is a partial document, describing only those parts of the LDML that are relevant for person names (name structure, formats, sorting). For the other parts of the LDML see the [main LDML document](tr35.md) and the links above.

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

## <a name="Contents" href="#Contents">Contents of Part 8, Person Names</a>

* 1 [CLDR Person Names](#CLDRPersonNames)
  * 1.1 [Introduction](#Introduction)
    * 1.1.1 [Not in scope](#not-in-scope)
  * 1.2 [API Implementation](#APIImplementation)
  * 1.3 [Person Name Formatting Overview](#PersonNameFormattingOverview)
  * 1.4 [Example Usage](#ExampleUsage)
* 2 [XML Structure](#2-xml-structure)
  * 2.1 [personNames Element](#2-1-personnames-element)
  * 2.2 [personName Element](#2-2-personname-element)
  * 2.3 [nameOrderLocales Element](#2-3-nameorderlocales-element)
  * 2.4 [foreignSpaceReplacement Element](#2-4-foreignspacereplacement-element)
  * 2.5 [initialPattern Element](#2-5-initialpattern-element)
    * 2.5.1 [Syntax](#syntax)
* 3 [Person Name Object](#3-person-name-object)
* 4 [Person Name Attributes](#4-person-name-attributes)
  * 4.1 [order](#4-1-order)
  * 4.2 [length](#4-2-length)
  * 4.3 [usage](#4-3-usage)
  * 4.4 [formality](#4-4-formality)
* 5 [namePattern Syntax](#5-namepattern-syntax)
  * 5.1 [Fields](#5-1-fields)
  * 5.2 [Modifiers](#5-2-modifiers)
* 6 [Formatting Process](#6-formatting-process)
  * 6.1 [Derive the name locale](#6-1-derive-the-name-locale)
  * 6.2 [Derive the formatting locale](#6-2-derive-the-formatting-locale)
  * 6.3 [Derive the name order](#6-3-derive-the-name-order)
  * 6.4 [Choose a personName](#6-4-choose-a-personname)
  * 6.5 [Choose a namePattern](#6-5-choose-a-namepattern)
  * 6.6 [Examples of choosing a namePattern](#6-6-examples-of-choosing-a-namepattern)
    * 6.6.1 [Examples for rules 1 and 2](#examples-for-rules-1-and-2)
    * 6.6.2 [Examples for rule 3 and the interaction between the rules](#examples-for-rule-3-and-the-interaction-between-the-rules)
  * 6.7 [Deriving initials](#6-7-deriving-initials)
  * 6.8 [Handling foreign names](#6-8-handling-foreign-names)
* 7 [Sample Name](#7-sample-name)
  * 7.1 [Syntax](#7-1-syntax)
  * 7.2 [Expected values](#7-2-expected-values)
* 8 [PersonName Data Interface Examples](#8-personname-data-interface-examples)
  * 8.1 [Example 1](#8-1-example-1)
  * 8.2 [Example 2](#8-2-example-2)

## 1 <a name="CLDRPersonNames" href="#CLDRPersonNames">CLDR Person Names</a>

### 1.1 <a name="Introduction" href="#Introduction">Introduction</a>

CLDR provides formatting for person names, such as John Smith or 宮崎駿. These use patterns to show how a name object (for example, from a database) should be formatted for a particular locale. Name data has fields for the parts of people’s names, such as a **given** field with a value of “Maria”, and a **surname** field value of “Schmidt”. 

There is a wide variety in the way that people’s names appear in different languages. 

* People may have a different number of names, depending on their culture—they might have only one name (“Zendaya”), two (“Albert Einstein”), or three or more.
* People may have multiple words in a particular name field, eg “Mary Beth” as a given name, or “van Berg” as a surname.
* Some languages, such as Spanish, have two surnames (where each can be composed of multiple words).
* The ordering of name fields can be different across languages, as well as the spacing (or lack thereof) and punctuation.
* Name formatting needs to be adapted to different circumstances, such as a need to be presented shorter or longer; formal or informal context; or when talking about someone, or talking to someone, or as a monogram (JFK).

This document provides the [LDML](http://www.unicode.org/reports/tr35/) specification for formatting of personal names, using data, structure, and examples. 

> **This is a technology preview; thus not intended for production software (except where itself marked as a technology preview). We have gathered a first round of data, and intend to refine the way in which we gather data. We are looking for additional feedback on the tech preview so that we can make improvements. For example, there are a few areas where we intend enhancements: handling native vs foreign names (in the native script); handling prefix and suffix fields; and so on.**

The CLDR functionality is targeted at formatting names for typical usage on computers (e.g. contact names, automated greetings, etc.), rather than being designed for special circumstances or protocol, such addressing royalty. However, the structure may be enhanced in the future when it becomes clear that additional features are needed for some languages.

This addition to CLDR is based on review of current standards and practices that exist in LDAP, hcard, HTML and various other international standards and commercial implementations.

Additions to those structures were made to accomodate known issues in large population groups, such as mononyms in Indonesia, patronymic and matronymic naming structure in Iceland and India, the need for a second surname in Spanish-speaking regions and the common case of chains of patronymic names in Arabic-speaking locales. The formatting patterns allow for specifying different “input parameters” to account for different contexts.

#### 1.1.1 <a name="not-in-scope" href="#not-in-scope">Not in scope</a>

The following features are currently out of scope for Person Names formating:

* Grammatical inflection of formatted names.
* Context-specific cultural aspects, such as when to use “-san” vs “-sama” when addressing a Japanese person. 
* Providing lists of prefixes and suffixes (Mr, Ms., Mx., Dr., etc.).
* Distinctions among prefixes and suffixes, such as title (Dr., Prof., Sir), gender-honorifics, generation (Jr., IV), accreditations (MBA, Esq.).
* Validation of input, such as  which fields are required, and what characters are allowed.
* Combining alternative names, such as multicultural names in Hong Kong "[Jackie Chan Kong-Sang](https://en.wikipedia.org/wiki/Jackie_Chan)”, or ‘Dwayne “The Rock” Johnson’.
* More than two levels of formality for names.
* Parsing of names:
  * Parsing of name strings into specific name parts such as given and given2. A name like "Mary Beth Estrella" could conceivably be any of the following.

    | given     | given2    | surname       | surname2 |
    | --------- | --------- | ------------- | -------- |
    | Mary      | Beth      | Estrella      |          |
    | Mary Beth |           | Estrella      |          |
    | Mary      |           | Beth Estrella |          |
    | Mary      |           | Beth          | Estrella |

  * Parsing out the other components of a name in a string, such as surname prefixes ([Tussenvoegsel](https://en.wikipedia.org/wiki/Tussenvoegsel) in Dutch). 

### 1.2 <a name="APIImplementation" href="#APIImplementation">API Implementation</a>

A Tech Preview API for formatting personal names is included in ICU. The implementation can be found at  [PersonNameFormatter.java](https://github.com/unicode-org/icu/blob/main/icu4j/main/classes/core/src/com/ibm/icu/text/PersonNameFormatter.java).

### 1.3 <a name="PersonNameFormattingOverview" href="#PersonNameFormattingOverview">Person Name Formatting Overview</a>

Logically, the model used for applying the CLDR data is the following:

![diagram showing relationship of components involved in person name formatting](images/personNamesFormatModel.png)

Conceptually, CLDR person name formatting depends on data supplied by a PersonName Data Interface. That could be a very thin interface that simply accesses a database record, or it could be a more sophisticated interface that can modify the raw data before presenting it to be formatted. For example, based on the formatting locale a PersonName data interface could transliterate names that are in another script, or supply equivalent titles in different languages.

The specification below will talk about a “PersonName object” as an entity that is logically accessed via such an interface. If multiple formatted names are needed, such as in different scripts or with alternate names, or pronunciations (eg kana), the presumption is that those are logically separate PersonName objects. See [[Person Name Object](#3-person-name-object)]. 

The following summarizes the name data supplied via the PersonName Data Interface:

* Name data is composed of one or more name parts, which are categorized in this standard as
    * _prefix_ - a string that may precede a name and may indicate an honorific, title, etc.
    * _given_ - usually a name given to someone that is not passed to a person by way of parentage
    * _given2_ - name or names that may appear between the first given name string and the surname. In the West, this may be a middle name, in Slavic regions it may be a patronymic name, and in parts of the Middle East, it may be the _nasab (نسب)_ or series of patronymics.
    * _surname_ - usually the family name passed to a person that indicates their family, tribe, or community. In most Western languages, this is known as the last name.
    * _surname2_ - in some cultures, both the parent’s surnames are used and need to be handled separately for formatting in different contexts.
    * _suffix_ - a string that may succeed a person’s name to indicate status, generation, or title.
    * _See the section on [[Fields](#5-1-fields)] for more details._
* Name data may have additional attributes that this specification accommodates.
    * _-informal_ - A name may have a formal and an informal presentation form, for example “Bob” vs “Robert” or “Са́ша” vs “Алекса́ндра”. This is accomplished by using the simple construct _given-informal_.
    * _-prefix_ and _-core_ - In some languages the surname may have a prefix that needs to be treated differently, for example “van de Berg”. The data can refer to “van de” as _surname-prefix_ and “Berg” with _surname-core_ and the PersonNames formatters will format them correctly in Dutch and many other languages.
    * _See the section on [[Modifiers](#5-2-modifiers)] for more details._

To format a name correctly, the correct context needs to be known. The context is composed of:

* **The formatting locale.** This is used to choose the primary set of patterns to format name data.
* **The name locale.** If the name data comes from a locale different from the formatting locale, it may need to be handled differently. If the name locale is not known, an inferred name locale is derived from the information in the name and the formatting locale.
* **Input parameters.**
    * **_order_** - indicates whether the given name comes first or the surname. This is normally specified in the CLDR data for the locale. This feature is also used for the sorting format.
    * **_length_** - used to select patterns for common short, medium, and long formatted names.
    * **_usage_** - this is used to select the correct pattern to format a name when a program is _addressing_ or talking to a person or it is _referring_ to or talking about another person.
    * **_formality_** - This is used to select the formal or informal formatting of a name.
    * _See [[Person Name Attributes](#4-person-name-attributes)] for more details._

### 1.4 <a name="ExampleUsage" href="#ExampleUsage">Example Usage</a>

As an example, consider a person’s name that may contain:

| `prefix` | `given`  | `given2` | `surname` | `suffix` |
| -------- | -------- | -------- | --------- | -------- |
|          | Robin    | Finley   | Wang      | Ph.D.    |

If the selected personName data has the following formatting pattern:

> `{prefix} {given} {given2-initial} {surname}, {suffix}`

Then the output is:

> Robin F. Wang, Ph.D.

The _prefix_ field is empty, so both it and the space that follows it in the formatting pattern are omitted from the output, the _given2_ field is formatted as an initial, and a preceding comma is placed before the _suffix_.

Sections below specify the precise manner in which a pattern is selected, and how the pattern is modified for missing fields. 
    
## 2 <a name="2-xml-structure" href="#2-xml-structure">XML Structure</a>

Person name formatting data is stored as LDML with schema defined as follows.

### 2.1 <a name="2-1-personnames-element" href="#2-1-personnames-element">personNames Element</a>

```xml
<!ELEMENT personNames ( nameOrderLocales*, foreignSpaceReplacement?, initialPattern*, personName+, sampleName* ) >
```

The LDML top-level `<personNames>` element contains information regarding the formatting of person names, and the formatting of person names in specific contexts for a specific locale.

### 2.2 <a name="2-2-personname-element" href="#2-2-personname-element">personName Element</a>

The `<personName>` element contains the format patterns, or `<namePattern>` elements, for a specific context and is described in [[namePattern Syntax](#5-namepattern-syntax)]

The `<namePattern>` syntax is described in [[Person Name Format Patterns](#6-formatting-process)].

```xml
<!ELEMENT personName ( namePattern+ ) >
<!ATTLIST personName order NMTOKENS #IMPLIED >
```

* `NMTOKENS` is a space delimited list of `( surnameFirst | givenFirst | sorting )`

```xml
<!ATTLIST personName length NMTOKENS #IMPLIED >
```

* `NMTOKENS` is a space delimited list of `( long | medium | short )`

```xml
<!ATTLIST personName usage NMTOKENS #IMPLIED >
```

* `NMTOKENS` is a space delimited list of `( addressing | referring | monogram )`

```xml
<!ATTLIST personName formality NMTOKENS #IMPLIED >
```

* `NMTOKENS` is a space delimited list of `( formal | informal )`

The `<personName>` element has attributes of `order`, `length`, `usage`, and `formality`, and contains one or more `<namePattern>` elements.

* For each attribute, there must be at least one attribute value, no value can occur twice, and order is not important (but the canonical order of elements is `order, length, usage, formality`). Thus 
    * `formality="informal informal"` is invalid, 
    * as is `formality=""`. 
    * `formality="formal informal"` is valid and canonical
    * `formality="informal formal"` is valid, but not canonical
* A missing attribute is equivalent to a list of all valid values for that attribute. For example, if `formality=...` is missing, it is equivalent to `formality="formal informal"`.

```xml
<!ELEMENT namePattern ( #PCDATA ) >
```

A `namePattern` contains a list of PersonName fields enclosed in curly braces, separated by literals, such as:

> `<namePattern>{surname}, {given} {given2}</namePattern>`

which produces output like _“Smith, Robert James”_. See [[namePattern Syntax](#5-namepattern-syntax)] for more details.

### 2.3 <a name="2-3-nameorderlocales-element" href="#2-3-nameorderlocales-element">nameOrderLocales Element</a>

The `<nameOrderLocales>` element is optional, and contains information about selecting patterns based on the locale of a passed in PersonName object to determine the order of elements in a formatted name. For more information see [[NameOrder](#6-3-derive-the-name-order)]. It has a structure as follows:

```xml
<!ELEMENT nameOrderLocales `( #PCDATA )`>
<!ATTLIST nameOrderLocales order ( givenFirst | surnameFirst ) #REQUIRED >
```

* `#PCDATA `is a space delimited list of one or more [unicode_locale_id](https://unicode.org/reports/tr35/#unicode_locale_id)s. Normally they are limited to language, script, and region. The _und_ locale ID may only occur once, either in _surnameFirst_ or _givenFirst_, but not both, and matches all base locales not explicitly listed.

An example from English may look like the following

> `<nameOrderLocales order="givenFirst">und en</nameOrderLocales>`<br/>
> `<nameOrderLocales order="surnameFirst">ko vi yue zh</nameOrderLocales>`

This would tell the formatting code, when handling person name data from an English locale, to use patterns with the `givenFirst` order attribute for all data except name data from Korean, Vietnamese, Cantonese, and Chinese locales, where the `surnameFirst` patterns should be used.

### 2.4 <a name="2-4-foreignspacereplacement-element" href="#2-4-foreignspacereplacement-element">foreignSpaceReplacement Element</a>

The `<foreignSpaceReplacement>` element is used to specify how delimiters should appear between name parts when the name data (name locale) is different from the requested locale (formatting locale)., but they both use the same script.

```xml
<!ELEMENT foreignSpaceReplacement ( #PCDATA ) >
<!ATTLIST foreignSpaceReplacement xml:space preserve #REQUIRED >
```

* `xml:space` must be set to `'preserve'` so that actual spaces in the pattern are preserved. See [W3C XML White Space Handling](https://www.w3.org/TR/xml/#sec-white-space).
* The `#PCDATA `is the character sequence used to replace spaces between fields for name data from a name locale that is different from the formatting locale, but are in the same script.

### 2.5 <a name="2-5-initialpattern-element" href="#2-5-initialpattern-element">initialPattern Element</a>

The `<initialPattern>` element is used to specify how to format initials of name parts.

**_initial_** is a pattern used to display a single initial in the locale, while **_initialSequence_** is a pattern used to “glue” together multiple initials for multiword fields, for example with the given name “Mary Beth” in English.

#### 2.5.1 <a name="syntax" href="#syntax">Syntax</a>

```xml
<!ELEMENT initialPattern ( #PCDATA ) >
<!ATTLIST initialPattern type ( initial | initialSequence) #REQUIRED >
```

The `type="initial"` is used to specify the pattern for how single initials are created, for example “Wolcott” => “W.” would have an entry of

> `<initialPattern type="initial">{0}.</initialPattern>`

`type="initialSequence`” is used to specify how a series of initials should appear, for example “Wolcott Janus” => “W. J.”, with spaces between each initial, would have a specifier of

> `<initialPattern type="initialSequence">{0} {1}</initialPattern>`

## 3 <a name="3-person-name-object" href="#3-person-name-object">Person Name Object</a>

The information that is to be formatted logically consists of a data object containing a number of fields. This data object is a construct for the purpose of formatting, and doesn’t represent the source of the name data. That is, the original source may contain more information. The PersonName object is merely a logical ‘transport’ of information to formatting; it may in actuality consist of, for example, an API that fetches fields from a database.

Note that an application might have more than one set of name data for a given person, such as data for both a legal name and a nickname or preferred name. Or the source data may contain two whole sets of name data for a person from an Eastern Slavic region, one in Cyrillic characters and one in Latin characters. Or it might contain phonetic data for a name (commonly used in Japan). The additional application-specific information in person’s names is out of scope for the CLDR Person Name formatting data. Thus a calling application may produce more than one PersonName object to format depending on the purpose. 

For illustration, the following is a sample PersonName object.

| Field            | Value        | Comment                         |
| ---------------- | ------------ | ------------------------------- |
| `prefix`         | “Dr.”        |                                 |
| `given`          | “William”    |                                 |
| `given-informal` | “Bill”       | example inclusion of "nickname" |
| `given2`         | “Torval”     |                                 |
| `surname`        | “Brown”      |                                 |
| `nameLocale`     | “und-US”     | this is just for illustration   |
| `preferredOrder` | “givenFirst” | this too                        |

A PersonName object is logically composed of the fields above plus other possible variations. See [[Fields](#5-1-fields)]. There must be at least one field present: either a `given` or `surname` field. Other fields are optional, and some of them can be constructed from other fields if necessary.

A modifier is supplied, _-informal_, which can be used to indicate which data element to choose when formatting informal cases which might include nicknames or preferred names. For more details, see section on [_[Modifiers](#5-2-modifiers)_] in [namePattern Syntax](#5-namepattern-syntax) below.

## 4 <a name="4-person-name-attributes" href="#4-person-name-attributes">Person Name Attributes</a>

A person name pattern may have any of four attributes: order, length, usage, and formality. LDML specifies that all the values for these attributes are unique. For example, because length=long is valid, usage=long cannot also be valid. That allows the pattern labels to be simple, because the attribute names can be skipped. That is, 

> `{order=givenFirst, length=long, usage=referring, formality=formal}`

can be abbreviated without loss of information as: 

> _givenFirst-long-referring-formal._

Each of these attributes are described below using sample PersonName objects as examples.

### 4.1 <a name="4-1-order" href="#4-1-order">order</a>

The order attribute is used for patterns with different orders of fields. The order=sorting patterns are chosen based on input parameters, while the choice between givenFirst and surnameFirst is based on features of the PersonName object to be formatted and the nameOrder element values.

| Parameter      | Description                                  |
| -------------- | -------------------------------------------- |
| `givenFirst`   | The given name precedes the surname.         |
| `surnameFirst` | The surname precedes the given name.         |
| `sorting`      | Used to format names for a for a sorted list.<br/>example: “Brown, William”  [medium, informal] |

For example, when the display language is Japanese, it is customary to use _surnameFirst_ for names of people from Japan and Hungary, but use _givenFirst_ for names of people from the United States and France. Although the English pattern for sorting is distinct from the other patterns (except for unusual names), that is not necessarily the case in other languages.

### 4.2 <a name="4-2-length" href="#4-2-length">length</a>

The `length` attribute specifies the relative length of a formatted name depending on context. For example, a `long` formal name in English would include prefix, given, given2, surname plus suffix; whereas a `short` informal name may only be the given name.

Note that the formats may be the same for different lengths depending on the formality, usage, and cultural conventions for the locale. For example, medium and short may be the same for a particular context.

| Parameter | Description |
| --------- | ----------- |
| `long`    | A `long` length would usually include all parts needed for a legal name or identification.<br/>Example: `usage="referring", formality="formal"`<br/>_“Mr. Robert John Smith, PhD”_ |
| `medium`  | A `medium` length is between long and short.<br/>Example: `usage="referring", formality="formal"`<br/>_“Robert Smith”_ |
| `short`   | A `short` length uses a minimum set of names.<br/>Example: `usage="referring", formality="formal"`<br/>_“Mr. Smith”_ |

### 4.3 <a name="4-3-usage" href="#4-3-usage">usage</a>

The usage indicates if the formatted name is being used to address someone, refer to someone, or present their name in an abbreviated form.

The pattern for `usage="referring"` may be the same as the pattern for `usage="addressing"`.

| Parameter    | Description |
| ------------ | ----------- |
| `addressing` | Used when speaking “to” a person, or “vocative” case. This may also have an effect on the formality.<br/>example: “Robert” [medium, informal] |
| `referring`  | Used when speaking “about” a person, or “nominative” case.<br/>example: “Robert Smith”  [medium, informal] |
| `monogram`   | The `monogram` usage is for a specific abbreviated form.<br/>Example: monogram for Robert James Smith may be “RS” or “RJS”.<br/>`usage=”referring”, formality=”formal”`<br/>_“RJS”_ |

Slavic languages provide a good  example of `addressing` vs `referring`. An example _uk-Cyrl_ PersonName object:

| Field            | Value        | Comment                         |
| ---------------- | ------------ | ------------------------------- |
| `prefix`         | “г-н”        | “Mr.”                           |
| `given`          | “Иван”       | “Ivan”                          |
| `given2`         | “Петрович”   | “Petrovich”                     |
| `surname`        | “Васильев”   | “Vasiliev”                      |

In Slavic languages, when _`addressing`_ a person (with `length="long"`), it might be

* г-н Иван Петрович Васильев `// "Mr Ivan Petrovich Vasiliev"`

And when _`referring`_ to a person, it might place the surname first.:

* Васильев Иван Петрович `// "Vasiliev Ivan Petrovich"`

The `monogram` usage is for very short abbreviated names, such as might be found in online messaging text avatars or other annotations. Ideally, a `monogram` format should result in something that could fit in an em square. Some emoji provide examples of this: 🅰️ 🆎 🆘 

When used with `length`, for many alphabetic locales a `monogram` would resolve to one, two, or three characters for short, medium, and long respectively. But that may vary depending on the usage in a locale.

### 4.4 <a name="4-4-formality" href="#4-4-formality">formality</a>

The `formality` indicates the formality of usage. A name on a badge for an informal gathering may be much different from an award announcement at the Nobel Prize Ceremonies.

Note that the formats may be the same for different formality scenarios depending on the length, usage, and cultural conventions for the locale. For example short formal and short informal may both be just the given name.

| Parameter  | Description |
| ---------- | ----------- |
| `formal`   | A more formal name for the individual. The composition depends upon the language. For example, a particular locale might include the prefix and suffix and a full middle name (given2) in the long form.<br/><br/>`length="medium", formality="formal"`<br/>“Robert J. Smith” |
| `informal` | A less formal name for the individual. The composition depends upon the language. For example, a language might exclude the prefix, suffix and given2 (middle) name. Depending on the length, it may also exclude the surname. The formatting algorithm should choose any passed in name data that has an _informal_ attribute, if available.<br/><br/>`length="medium", formality="informal"`<br/>“Bob Smith” |

## 5 <a name="5-namepattern-syntax" href="#5-namepattern-syntax">namePattern Syntax</a>

A _namePattern_  is composed of a sequence of field IDs, each enclosed in curly braces, and separated by zero or more literal characters (eg, space or comma + space). An Extended Backus Normal Form (EBNF) is used to describe the namePattern format for a specific set of attributes. It has the following structure. This is the `( #PCDATA )` reference in the element specification above.

|              | EBNF                          | Comments |
| ------------ | ----------------------------- | -------- |
| namePattern  | = literal?<br/><span style="white-space:nowrap">( modField  literal? )+;</span> | Two literals cannot be adjacent |
| modField     | <span style="white-space:nowrap">= '{' field modifierList? '}';</span> | A name field, optionally modified |
| field        | = 'prefix'<br/>\| 'given'<br/>\| 'given2'<br/>\| 'surname'<br/>\| 'surname2'<br/>\| 'suffix' ; | See [Fields](#5-1-fields) |
| modifierList | = '-informal'?<br/><span style="white-space:nowrap">( '-allCaps' \| ‘-initialCap' )?;</span><br/><span style="white-space:nowrap">( '-initial'  \| '-monogram' )?</span><br/><span style="white-space:nowrap">( '-prefix' \| '-core' )?</span> | Optional modifiers that can be applied to name parts, see [Modifiers](#5-2-modifiers). Note that some modifiers are exclusive: only `prefix` or `core`, only `initial` or `monogram`, only `allCaps` or `initialCap`. |
| literal      | = codepoint+ ; | One or more Unicode codepoints. |

### 5.1 <a name="5-1-fields" href="#5-1-fields">Fields</a>

The Person Name formatting data assumes that the name data to be formatted consists of the fields in the table below. All of the fields may contain multiple words. Field IDs are lowercase ASCII alphanumeric, and start with an alphabetic character.

When determining how a full name is to be placed into name fields, the data to be formatted should be organized functionally. That is, if a name part is on the dividing line between `given2` and `given`, the key feature is whether it would always occur with the rest of the given name. For example, in _“Mary Jean Smith”_, if _“Mary”_ never occurs without the _“Jean”_, then the given name should be _“Mary Jean”_. If _“Smith”_ never occurs without the _“Jean”_, the `surname` should be _“Jean Smith”_. Otherwise, _“Jean”_ would be the `given2` field. 

For example, a patronymic would be treated as a `given2` name in most slavic languages. 

In some cultures, two surnames are used to indicate the paternal and maternal family names or generational names indicating father, grandfather. The `surname2` field is used to indicate this. The CLDR PersonName formatting data assumes that if a PersonName object to be formatted does not have two surnames, then the `surname2` field is not populated. (That is, no pattern should have a `surname2` field without a surname field.) Order of fields in a pattern can vary arbitrarily by locale.

In most cultures, there is a concept of nickname or preferred name, which is used in informal settings or sometimes to represent a “public” or “stage name”. The nickname or preferred name may be submitted as a separate PersonName object to be formatted, or included with a modifier such as `given-informal`. 

| Field      | Description<br/>Note: The values for each are as supplied by the PersonName object, via the PersonName data interface. |
| ---------- | ----------- |
| `prefix`   | Typically a title, honorific, or generational qualifier.<br/>Example: ‘Ms.’, ‘Mr.’, ’Dr’, ‘President’<br/><br/>Note that CLDR PersonName formats data does not define regional or locale-specific lists of prefixes, honorifics, or titles such as “Mr”, “Ms”, “Mx”, “Prof”, “Jr”, etc. |
| `given`    | The “given” name. Can be multiple words such as “Mary Ann”.<br/>Examples:  “Janus”, “Mary Jean”, or “Jean-Louis”|
| `given2`   | Additional given name or names or middle name, usually names(s) written between the given and surname. Can be multiple words. In some references, also known as a “second” or “additional” given name or patronymic. This field is separate from the “given” field because it is often optional in various presentation forms.<br/>Examples:  “Horatio Wallace” as in<br/>`{ given: "Janus", `**`given2: "Horatio Wallace"`**`, surname: "Young" }`<br/><br/>“S.” as in “Harry S. Truman”. Yes, his full middle name was legally just “S.”.|
| `surname`  | The “family name”. Can be more than one word.<br/><br/>Example: “van Gogh” as in<br/>`{ given: "Vincent", given2: "Willem", `**`surname: "van Gogh"`**` }`<br/><br/>Other examples: “Heathcote-Drummond-Willoughby” as in “William Emanuel Heathcote-Drummond-Willoughby III”|
| `surname2` | Secondary surname (used in some cultures), such as second or maternal surname in Mexico and Spain. This field is separate from the “surname” field because it is often optional in various presentation forms, and is considered a separate distinct name in some cultures.<br/><br/>Example: “Barrientos” in “Diego Rivera Barrientos”;<br/>`{ given: "Diego", surname: "Rivera", `**`surname2: "Barrientos"`**` }`<br/><br/>Example: if "Mary Jane Smith" moves to Spain the new name may be<br/>`{ given: "Mary", given2: "Jane", surname: "Smith", `**`surname2: "Jones"`**` }`|
| `suffix`   | Typically a title, honorific, or generational qualifier.<br/>Example: “PhD”, “Jr.”<br/><br/>Example: “Sonny Jarvis Jr.”<br/>`{ given: "Salvatore", given2: "Blinken", surname: "Jarvis", `**`suffix: "Jr."`**` }`<br/><br/>An alternate PersonName object may be presented for formatting using the “stage” name from the application’s data:<br/>`{ given: "Salvatore", given-informal: "Sonny", given2: "", surname: "Jarvis", `**`suffix: "Jr."`**` }` |

Some other examples:

* British name: _John Ronald Reuel Tolkien_: `given` name is "John", `given2` name would be  "Ronald Reuel", and the `surame` is "Tolkien".
* Dutch name: _Anneliese Louise van der Pol_: `given` name: "Anneliese", `given2` name: "Louise", `surname`: "van der Pol"
    * Also surname-prefix: “van der”, surname-core: “Pol” — see below.	
* French name: “Jean-Louis Trintignant” would _not_ be Jean (`given`) Louis (`given2`) Trintignant (`surname`), since “Louis” wouldn’t be discarded when formatting. Instead it would be Jean-Louis (`given`) Trintignant (`surname`)

Note: If the legal name, stage name, etc. are substantially different, then that information can be logically in a separate PersonName object. That is, it is up to the implementation to maintain any distinctions that are important to it: CLDR PersonName formats is focusing on formatting a PersonName object that is given to it. 

`surname2` would only be asked for in certain locales, and where it is considered a separate, divisible name, such as in Mexico or Spain. For instance, in Mexico, the first and second surname are used for the legal name and in formal settings, and sometimes only the first surname is used in familiar or informal contexts.

* Heathcote-Drummond is a single surname and would not be `{surname}-{surname2}` because we would never discard part of the name when formatting.
* Spanish name: "Jose Luis Garcia Barrientos":   The `given` name is “Jose”, the `given2` name is “Luis”, the `surname` is "Garcia”, and the `surname2` is “Barrientos"

How names get placed into fields to be formatted is beyond the scope of CLDR PersonName formats; this document just lays out the assumptions the formatting code makes when formatting the names.

### 5.2 <a name="5-2-modifiers" href="#5-2-modifiers">Modifiers</a>

Each field in a pattern can have one or more modifiers. The modifiers can be appended to any field name, such as `{given-initial}` for the first grapheme of the given name. If more than one modifier is applied, they must be structured as in the EBNF.

The modifiers transform the input data as described in the following table:

| Modifier   | Description |
| ---------- | ----------- |
| informal   | Requests an informal version of the name if available. For example, {given} might be “Thomas”, and {given-informal} might be “Tom”. If there is no informal version, then the normal one is returned. An informal version should not be generated, because they vary too much: Beth, Betty, Betsy, Bette, Liz, … |
| prefix     | Return the “prefix” name, or the “tussenvoegsel'' if present. For example, “van der Poel” becomes “van der”, “bint Fadi” becomes “bint”, “di Santis” becomes “di”. Note that what constitutes the prefix is language- and locale-sensitive. It may be passed in as part of the PersonName object, similar to the _“-informal”_ modifier, e.g. as _“surname-prefix”_.<br/><br/>The implementation of this modifier depends on the PersonName object. CLDR does not currently provide support for automatic identification of tussenvoegsels, but may in the future.<br/><br/>If the resulting _“-prefix”_ value is empty, it defaults to an empty string.<br/><br/>An example sorting pattern for “Johannes van den Berg” may be<br/>{surname-core}, {given} {given2} {surname-prefix}<br/><br/>Only the _“-prefix”_ or the _“-core”_ modifier may be used, but not both. They are mutually exclusive. |
| core       | Return the “core” name, removing any tussenvoegsel. For example, “van der Poel” becomes “Poel”, “bint Fadi” becomes “Fadi”, “di Santis” becomes “Santis”. Note that what constitutes the core is language- and locale-sensitive.<br/><br/>The implementation of this modifier depends on the PersonName object. CLDR does not currently provide support for identification of tussenvoegsel, but may in the future.<br/><br/>If the resulting _“-core”_ value is empty, it defaults to the field it modifies. E.g., if _“surname-core”_ is empty in the PersonName object to be formatted, it will default to the _“surname”_ field.<br/><br/>Vice-versa, if the _surname_ field is empty, the formatter will attempt to use _surname-prefix_ and _surname-core_, if present, to format the name.<br/><br/>Only the _“-prefix”_ or the _“-core”_ modifier may be used, but not both. They are mutually exclusive. |
| allCaps    | Requests the element in all caps, which is desired In some contexts. For example, a new guideline in Japan is that for the Latin representation of Japanese names, the family name comes first and is presented in all capitals. This would be represented as<br/>“{surname-allCaps} {given}”<br/><br/>Hayao Miyazaki (宮崎 駿) would be represented in Latin characters in Japan (ja-Latn-JP) as _“MIYAZAKI Hayao”_<br/><br/>_The default implementation uses the default Unicode uppercase algorithm; if the PersonName object being formatted has a locale, and CLDR supports a locale-specific algorithm for that locale, then that algorithm is used. The PersonName object can override this, as detailed below._<br/><br/>Only the _“-allCaps”_ or the _“-initalCap”_ modifier may be used, but not both. They are mutually exclusive. |
| initialCap | Request the element with the first grapheme capitalized, and remaining characters unchanged. This is used in cases where an element is usually in lower case but may need to be modified. For example in Dutch, the name<br/>{ prefix: “dhr.”, given: ”Johannes”, surname: “van den Berg” },<br/>when addressed formally, would need to be “dhr. Van den Berg”. This would be represented as<br/>“{prefix} {surname-initialCap}”<br/><br/>Only the _“-allCaps”_ or the _“-initalCap”_ modifier may be used, but not both. They are mutually exclusive. |
| initial    | Requests the initial grapheme cluster of each word in a field. The `initialPattern` patterns for the locale are used to create the format and layout for lists of initials. For example, if the initialPattern types are<br/>`<initialPattern type="initial">{0}.</initialPattern>`<br/>`<initialPattern type="initialSequence">{0} {1}</initialPattern>`<br/>then a name such as<br/>{ given: “John”, given2: “Ronald Reuel”, surname: “Tolkien” }<br/>could be represented as<br/>“{given-initial-allCaps} {given2-initial-allCaps} {surname}”<br/>and will format to “**J. R. R. Tolkien**”<br/><br/>_The default implementation uses the first grapheme cluster of each word for the value for the field; if the PersonName object has a locale, and CLDR supports a locale-specific grapheme cluster algorithm for that locale, then that algorithm is used. The PersonName object can override this, as detailed below._<br/><br/>Only the _“-initial”_ or the _“-monogram”_ modifier may be used, but not both. They are mutually exclusive. |
| monogram   | Requests initial grapheme. Example: A name such as<br/>{ given: “Landon”, given2: “Bainard Crawford”, surname: “Johnson” }<br/>could be represented as<br/>“{given-monogram-allCaps}{given2-monogram-allCaps}{surname-monogram-allCaps}”<br/>or “**LBJ**”<br/><br/>_The default implementation uses the first grapheme cluster of the value for the field; if the PersonName object has a locale, and CLDR supports a locale-specific grapheme cluster algorithm for that locale, then that algorithm is used. The PersonName object can override this, as detailed below. The difference between monogram an initial is that monogram only returns one element, not one element per word._<br/><br/>Only the _“-initial”_ or the _“-monogram”_ modifier may be used, but not both. They are mutually exclusive. |

There may be more modifiers in the future. 

Examples: 

1. For the initial of the surname **_“de Souza”_**, in a language that treats the “de” as a tussenvoegsel, the PersonName object can automatically recast `{surname-initial}` to:<br/>`{surname-prefix-initial}{surname-core-initial-allCaps} `to get “dS” instead of “d”.
2. If the locale expects a surname prefix to to be sorted after a surname, then both `{surname-core} `then `{surname-prefix}` would be used as in<br/>`{surname-core}, {given} {given2} {surname-prefix}`

## 6 <a name="6-formatting-process" href="#6-formatting-process">Formatting Process</a>

The patterns are in personName elements, which are themselves in a personNames container element. The following describes how these patterns are chosen. If the name locale is different than the formatting locale, then additional processing needs to take place: see [Handling foreign names](#6-8-handling-foreign-names).

The details of the XML structure behind the data referenced here are in [XML Structure](#2-xml-structure).

### 6.1 <a name="6-1-derive-the-name-locale" href="#6-1-derive-the-name-locale">Derive the name locale</a>

Create a **full name locale** as follows.

1. First, let the **full formatting locale** be the fully-fleshed-out formatting locale using likely subtags.
2. If there is a name locale available via the PersonName data interface, obtain the full name locale from the name locale using likely subtags. Thus de ⇒ de_Latn_de.
3. Otherwise the full name locale is created based on the characters in the name and the full formatting locale, as follows:
    1. Find the predominant script for the name in the following way.
        1. For each character in the given and surname, find the script(s) of the character using the Script_Extensions property.
        2. For each of those scripts, increment a counter for that script, and record the position of the first character encountered in that script.
    2. The predominant script is the script with the highest counter value.
        1. In the rare case that there are multiple counters with the highest counter value, take the one with the lowest first position.
        2. In the even rarer case that there is still more than one, use the script whose script code is alphabetically lowest. (These two steps are simply to guarantee a determinant result.)
    3. If the predominant script is the same as the script of the full formatting locale, then let the full name locale be the full formatting locale.
    4. Otherwise, find the likely locale for the predominant script, as specified by the likely subtags. (This will add a language and region.) Let the full name locale be that likely locale.

In all steps below, the "name locale" is the full name locale.

### 6.2 <a name="6-2-derive-the-formatting-locale" href="#6-2-derive-the-formatting-locale">Derive the formatting locale</a>

If the full name locale is different from the full formatting locale, and the predominant script of the name is different from the script of the formatting locale, then let the full formatting locale be the full name locale.

In all steps below, the "formatting locale" is the full formatting locale.

### 6.3 <a name="6-3-derive-the-name-order" href="#6-3-derive-the-name-order">Derive the name order</a>

A PersonName object’s fields are used to derive an order, as follows:

1. If the PersonName object to be formatted has a `preferredOrder` field, then return that field’s value
2. Otherwise use the nameOrderLocales elements to find the most best match for the full name locale, as follows.
    1. For each locale L1 in the parent locale lookup chain* for the full name locale, do the following
        1. Create a locale L2 by replacing the language subtag by 'und'. (Eg, 'de_DE' ⇒ 'und_DE')
        2. For each locale L in {L1, L2}, do the following
             1. If there is a precise match among the givenFirst nameOrderLocales for L, then let the nameOrder be givenFirst, and stop.
             2. Otherwise if there is a precise match among the surnameFirst nameOrderLocales for L, then let the nameOrder be surnameFirst, and stop.

For example, here is a parent locale lookup chain: 

    de_Latn_DE ⇒ de_Latn ⇒ de_DE ⇒ de ⇒ und

In other words, you'll check the givenFirst and surnameFirst resources for the following locales, in this order:

    de_Latn_DE, und_Latn_DE, de_Latn, und_Latn, de_DE, und_DE, de, und

This process will always terminate, because there is always a und value in one of the two nameOrderLocales elements.

For example, the data for a particular locale might look like the following:

```xml
<nameOrderLocales order="surnameFirst">zh ja und-CN und-TW und-SG und-HK und-MO und-HU und-JP</nameOrderLocales>
```
The nameOrderLocales will match any locale with a zh or ja [unicode_language_subtag](https://unicode.org/reports/tr35/#unicode_language_subtag) and any locale with a CN, TW, SG, HK MO, HU, or JP [unicode_region_subtag](https://unicode.org/reports/tr35/#unicode_region_subtag).

Here are some more examples. Note that if there is no order field or locale field in the PersonName object to be formatted, and the script of the PersonName data is different from that of the formatting locale, then the default result is givenFirst.

| PersonName Object preferredOrder | PersonName Object Locale | Resulting Order |
| -------------------------------- | ------------------------ | --------------- |
| surnameFirst                     | ?                        | surnameFirst    |
|                                  | zh                       | surnameFirst    |
|                                  | und-JP                   | surnameFirst    |
|                                  | fr                       | givenFirst      |
|                                  |                          | givenFirst      |

### 6.4 <a name="6-4-choose-a-personname" href="#6-4-choose-a-personname">Choose a personName</a>

The personName data in CLDR provides representations for how names are to be formatted across the different axes of _order_, _length_, _usage_, and _formality_. More than one `namePattern` can be associated with a single `personName` entry. An algorithm is then used to choose the best `namePattern` to use.

As an example for English, this may look like:

```xml
<personNames>
  <personName order="givenFirst" length="long" usage="referring" formality="formal">
    <namePattern>{prefix} {given} {given2} {surname}, {suffix}</namePattern>
  </personName>
  <personName order="givenFirst" length="long" usage="referring" formality="informal">
    <namePattern>{given} «{given2}» {surname}</namePattern>
    <namePattern alt="2">«{given2}» {surname}</namePattern>
  </personName>
  <personName order="givenFirst" length="long" usage="sorting" formality="informal">
    <namePattern>{surname}, {given} {given2}</namePattern>
  </personName>
  ...
</personNames>
```

The task is to find the best personName for a given set of input attributes. Well-formed data will always cover all possible combinations of the input parameters, so the algorithm is simple: traverse the list of person names until the first match is found, then return it.

In more detail:

A set of input parameters { order=O length=L usage=U formality=F } matches a personName element when:

* The order attribute values contain O or there is no order attribute, and
* The length attribute values contain L or there is no length attribute, and
* The usage attribute values contain U or there is no usage attribute, and
* The formality attribute values contain F or there is no formality attribute

Example for input parameters 

> `order = `**`givenFirst`**`, length = `**`long`**`, usage = `**`referring`**`, formality = `**`formal`**

To match a personName, all four attributes in the personName must match (a missing attribute matches any value for that attribute):

| Sample personName attributes                                 | Matches? | Comment |
| :----------------------------------------------------------- | :------: | :------ |
| `order=`_`"givenFirst"`_` length=`_`"long"`_` usage=`_`"referring"`_` formality=`_`"formal"`_ | Y | exact match |
| `length=`_`"long"`_` usage=`_`"referring"`_` formality=`_`"informal"`_ | N | mismatch for formality |
| `length=`_`"long"`_` formality=`_`"formal"`_                  | Y | missing usage = all! |

To find the matching personName element, traverse all the personNames in order until the first one is found. This will always terminate since the data is well-formed in CLDR.

### 6.5 <a name="6-5-choose-a-namepattern" href="#6-5-choose-a-namepattern">Choose a namePattern</a>

To format a name, the fields in a namePattern are replaced with fields fetched from the PersonName Data Interface. The personName element can contain multiple namePattern elements. Choose one based on the fields in the input PersonName object that are populated: 
1. Find the set of patterns with the most populated fields.
2. If there is just one element in that set, use it.
2. Otherwise, among that set, find the set of patterns with the fewest unpopulated fields.
3. If there is just one element in that set, use it.
4. Otherwise, take the pattern that is alphabetically least. (This step should rarely happen, and is only for producing a determinant result.)

For example:

1. Pattern A has 12 fields total, pattern B has 10 fields total, and pattern C has 8 fields total.
2. Both patterns A and B can be populated with 7 fields from the input PersonName object, pattern C can be populated with only 3 fields from the input PersonName object.
3. Pattern C is discarded, because it has the least number of populated name fields.
4. Out of the remaining patterns A and B, pattern B wins, because it has only 3 unpopulated fields compared to pattern A.

If the “winning” namePattern still has fields that are unpopulated in the PersonName object, we alter the pattern algorithmically as follows:

1. If one or more fields at the start of the pattern are empty, all fields, whitespace, and literal text before the **first** populated field are deleted.
2. If one or more fields at the end of the pattern are empty, all fields, whitespace, and literal text after the **last** populated field are deleted.
3. For each empty field in the middle of the pattern (going from left to right), that field and all literal text between it and the nearest whitespace or field on both sides is deleted. If this results in two whitespace characters next to each other, they are coalesced into one.

### 6.6 <a name="6-6-examples-of-choosing-a-namepattern" href="#6-6-examples-of-choosing-a-namepattern">Examples of choosing a namePattern</a>

#### 6.6.1 <a name="examples-for-rules-1-and-2" href="#examples-for-rules-1-and-2">Examples for rules 1 and 2</a>

The personName element contains:


> `<namePattern>{prefix} {given} {given2} {surname}, {suffix}</namePattern>`


The input PersonName object contains:

| `prefix` | `given` | `given2` | `surname` | `suffix` |
| -------- | ------- | -------- | --------- | -------- |
|          | Raymond | J.       | Johnson   | Jr.      |

The output is:

> Raymond J. Johnson, Jr.

The “prefix” field is empty, and so both it and the space that follows it are omitted from the output, according to rule 1 above.

If, instead, the input PersonName object contains:

| `prefix` | `given` | `given2` | `surname` | `suffix` |
| -------- | ------- | -------- | --------- | -------- |
|          | Raymond | J.       | Johnson   |          |

The output is:

> Raymond J. Johnson

The “prefix” field is empty, and so both it and the space that follows it are omitted from the output, according to rule 1 above.

The “suffix” field is also empty, so it and both the comma and the space that precede it are omitted from the output, according to rule 2 above.

#### 6.6.2 <a name="examples-for-rule-3-and-the-interaction-between-the-rules" href="#examples-for-rule-3-and-the-interaction-between-the-rules">Examples for rule 3 and the interaction between the rules</a>

To see how rule 3 interacts with the other rules, consider an imaginary language in which people generally have given and given2 (or middle)  names, and the given2 name is always written with parentheses around it, and the given name is usually written as an initial with a following period.

The personName element contains:

> `<namePattern>{given-initial}. ({given2}) {surname}</namePattern>`


The input PersonName object contains:

| `given` | `given2` | `surname` |
| ------- | -------- | --------- |
| Foo     | Bar      | Baz       |

The output is:

> F. (Bar) Baz

If, instead, the input PersonName object contains:

| `given` | `given2` | `surname` |
| ------- | -------- | --------- |
| Foo     |          | Baz       |

The output is:

> F. Baz

The “given2” field is empty, so it and the surrounding parentheses are omitted from the output, as is one of the surrounding spaces, according to rule 3. The period after “{given-initial}” remains, because it is separated from the “{given2}” element by  space-- punctuation around a missing field is only deleted up until the closest space in each direction.

If there were no space between the period and the parentheses, as might happen if our hypothetical language didn’t use spaces:

> `<namePattern>{given-initial}.({given2}) {surname}</namePattern>`

The input PersonName object still contains:

| `given` | `given2` | `surname` |
| ------- | -------- | --------- |
| Foo     |          | Baz       |

The output is:

> F Baz

Both the period after “{given-initial}” _and_ the parentheses around “{given2}” are omitted from the output, because there was no space between them — instead, we delete punctuation all the way up to the neighboring field. To solve this (making sure the “{given-initial}” field always has a period after it), you would add another namePattern:

> `<namePattern>{given-initial}.({given2}) {surname}</namePattern>`<br/>
> `<namePattern alt=”2”>{given-initial}. {surname}</namePattern>`

The first pattern would be used when the “given2” field is populated, and the second pattern would be used when the “given2” field is empty.

Rules 1 and 3 can conflict in similar ways. If the personName element contains (there’s a space between the period and the opening parenthesis again):

> `<namePattern>{given-initial}. ({given2}) {surname}</namePattern>`

And the input PersonName object contains:

| `given` | `given2` | `surname` |
| ------- | -------- | --------- |
|         | Bar      | Baz       |

The output is:

> Bar) Baz

Because the “given” field is empty, rule 1 not only has us delete it, but also all punctuation up to “{given2}”. This includes _both_ the period _and_ the opening parenthesis. Again, to solve this, you’d supply two namePatterns:

> `<namePattern>{given-initial}. ({given2}) {surname}</namePattern>`<br/>
> `<namePattern alt=”2”> ({given2}) {surname}</namePattern>`

The output would then be:

> (Bar) Baz

The first namePattern would be used if the “given” field was populated, and the second would be used if it was empty.

If, instead, the input PersonName object contains:

| `given` | `given2` | `surname` |
| ------- | -------- | --------- |
| Foo     |          | Baz       |

The output is:

> F. Baz

### 6.7 <a name="6-7-deriving-initials" href="#6-7-deriving-initials">Deriving initials</a>

The following process is used to produce initials when they are not supplied by the PersonName object. Assuming the input example is “Mary Beth”:

| Action              | Result |
| ------------------- | ------ |
| 1. Split into words | “Mary” and “Beth” |
| 2. Fetch the first grapheme cluster of each word | “M” and “B” |
| 3. The ***initial*** pattern is applied to each<br/>`  <initialPattern type="initial">{0}.</initialPattern>` | “M.” and “B.” |
| 4. Finally recombined with ***initialSequence***<br/>`  <initialPattern type="initialSequence">{0} {1}</initialPattern>` | “M. B.” |

See the “initial” modifier in the [Modifiers](#5-2-modifiers) section for more details.

### 6.8 <a name="6-8-handling-foreign-names" href="#6-8-handling-foreign-names">Handling foreign names</a>

There are two main challenges in dealing with foreign name formatting that needs to be considered. One is the ordering, which is dealt with under the section [[2.3 nameOrderLocales Element](#2-3-nameorderlocales-element)]. The other is spacing.

Some writing systems require spaces (or some other non-letters) to separate words. For example, [Hayao Miyazaki](https://en.wikipedia.org/wiki/Hayao_Miyazaki) is written in English with given name first and with a space between the two name fields, while in Japanese there is no space with surname first: 

> [宮崎駿](https://ja.wikipedia.org/wiki/%E5%AE%AE%E5%B4%8E%E9%A7%BF)

1. If a locale requires spaces between words, the normal patterns for the formatting locale are used. On Wikipedia, for example, note the space within the Japanese name on pages from English and Korean (an ideographic space is used here for emphasis).

* “​​[Hayao Miyazaki (宮崎<span style="background-color:aqua">　</span>駿, Miyazaki Hayao](https://en.wikipedia.org/wiki/Hayao_Miyazaki)…” or 
* “[미야자키<span style="background-color:aqua">　</span>하야오(일본어: 宮﨑<span style="background-color:aqua">　</span>駿 Miyazaki Hayao](https://ko.wikipedia.org/wiki/%EB%AF%B8%EC%95%BC%EC%9E%90%ED%82%A4_%ED%95%98%EC%95%BC%EC%98%A4)…”. 

2. If a locale **doesn’t** require spaces between words, there are two cases, based on whether the foreign name is written in the locale’s script, or the foreign name is left in its native script. In both cases, patterns from the **locale of the name** are used. For example, the formatting locale might be Japanese, and the locale of the PersonName object might be de_CH, German (Switzerland), such as Albert Einstein.

    1. **The foreign name is written in the formatting locale’s script.** In that case, the **foreignSpaceReplacement** is substituted for each space in the patterns from the _locale of the name_. Here are examples for Albert Einstein in Japanese and Chinese:
        * [アルベルト<span style="background-color:aqua">・</span>アインシュタイン](https://ja.wikipedia.org/wiki/%E3%82%A2%E3%83%AB%E3%83%99%E3%83%AB%E3%83%88%E3%83%BB%E3%82%A2%E3%82%A4%E3%83%B3%E3%82%B7%E3%83%A5%E3%82%BF%E3%82%A4%E3%83%B3) 
        * [阿尔伯特<span style="background-color:aqua">·</span>爱因斯坦](https://zh.wikipedia.org/wiki/%E9%98%BF%E5%B0%94%E4%BC%AF%E7%89%B9%C2%B7%E7%88%B1%E5%9B%A0%E6%96%AF%E5%9D%A6) 
    2. **The foreign name is written in a different script.** In that case, the patterns from the **locale of the name** are used as is.
        * [Albert Einstein](https://de.wikipedia.org/wiki/Albert_Einstein) 

In both cases, the ordering may be changed according to the **Name Order for Locales** settings that each locale provides. If the PersonName object does not supply a locale for a name, then a default locale will be derived based on other information (such as the script of the characters in the name fields).

> **Note** In the tech preview, the structure isn't yet powerful enough to handle cases with `foreignSpaceReplacement` where the formatting locale doesn’t need spaces between words, but the name locale has the same ordering as the formatting locale. 
> For example, consider where the formatting locale is Thai, and the name is in English, but transliterated into Thai.

To illustrate how foreign space replacement works, consider the following name data. For illustration, the name locale is given in the maximized form: in practice, `ja` would be used instead of `ja_Jpan_JP`, and so on.: For more information, see Likely Subtags [TBD add link].

| name locale   | given    | surname       |
| ------------- | -------- | ------------- |
| `de_Latn_CH`  | Albert   | Einstein      |
| `de_Jpan_CH`  | アルベルト | アインシュタイン |
| `ja_Jpan_JP`  | Hayao    | Miyazaki      |
| `ja_Jpan_JP`  | 駿       | 宮崎           |

Suppose the PersonNames formatting patterns for `ja_JP` and `de_CH` contained the following:

**`ja_JP` formatting patterns**

<pre>
&lt;personNames&gt;
   &lt;nameOrderLocales order="givenFirst"&gt;und&lt;/nameOrderLocales&gt;
   &lt;<strong>nameOrderLocales</strong> order="<strong>surnameFirst</strong>"&gt;hu <strong>ja</strong> ko vi yue zh <strong>und_JP</strong>&lt;/nameOrderLocales&gt;
   &lt;<strong>foreignSpaceReplacement</strong> xml:space="preserve"&gt;<span style="background-color:aqua">・</span>&lt;/foreignSpaceReplacement&gt;
   . . .
   &lt;personName order="<strong>givenFirst</strong>" length="medium" usage="referring" formality="formal"&gt;
      &lt;namePattern&gt;{given}<span style="background-color:aqua"> </span>{given2}<span style="background-color:aqua"> </span>{surname}{suffix}&lt;/namePattern&gt;
   &lt;/personName&gt;
   . . .
   &lt;personName order="<strong>surnameFirst</strong>" length="medium" usage="referring" formality="formal"&gt;
      &lt;namePattern&gt;{surname}{given2}{given}{suffix}&lt;/namePattern&gt;
   &lt;/personName&gt;
   . . .
&lt;/personNames&gt;
</pre>

Note in the `de_CH` locale, _ja_ is not listed in nameOrderLocales, and would therefore fall under _und_, and be formatted using the givenFirst order patterns if the name data is in the same script as the formatting locale.

**`de_CH` formatting patterns**

<pre>
&lt;personNames&gt;
   &lt;nameOrderLocales order="<strong>givenFirst</strong>"&gt;und <strong>de</strong>&lt;/nameOrderLocales&gt;
   &lt;nameOrderLocales order="surnameFirst"&gt;ko vi yue zh&lt;/nameOrderLocales&gt;
   &lt;foreignSpaceReplacemen xml:space="preserve"&gt;<span style="background-color:aqua"> </span>&lt;/foreignSpaceReplacement&gt;
   . . . 
   &lt;personName order="givenFirst" length="medium" usage="referring" formality="formal"&gt;
      &lt;namePattern&gt;{given}<span style="background-color:aqua"> </span>{given2-initial}<span style="background-color:aqua"> </span>{surname}, {suffix}&lt;/namePattern&gt;
   &lt;/personName&gt;
   . . . 
   &lt;personName order="surnameFirst" length="medium" usage="referring" formality="formal"&gt;
      &lt;namePattern&gt;{surname}<span style="background-color:aqua">, </span>{given}<span style="background-color:aqua"> </span>{given2-initial}<span style="background-color:aqua">,</span> {suffix}&lt;/namePattern&gt;
   &lt;/personName&gt;
   . . . 
&lt;/personNames&gt;`
</pre>

The name data would resolve as follows:

<table>
  <tr>
   <td colspan="7" ><strong>formatting locale: ja_JP, </strong>script is Jpan which includes Hani, Hira and Kana</td>
  </tr>
  <tr>
   <td><strong>name locale</strong></td>
   <td><strong>given</strong></td>
   <td><strong>surname</strong></td>
   <td><strong>same<br/>script</strong></td>
   <td><strong>formatting<br/>locale</strong</td>
   <td><strong>order</strong></td>
   <td><strong>foreign<br/>space</strong></td>
  </tr>
  <tr>
   <td>de_Latn_CH</td>
   <td>Albert</td>
   <td><span style="text-decoration:underline;">Einstein</span></td>
   <td>NO</td>
   <td>de</td>
   <td>given First</td>
   <td></td>
  </tr>
  <tr>
   <td colspan="7" style="text-align:center">“Albert <span style="text-decoration:underline;">Einstein</span>”</td>
  </tr>
  <tr>
   <td>de_Jpan_CH</td>
   <td>アルベルト</td>
   <td><span style="text-decoration:underline;">アインシュタイン</span></td>
   <td>YES</td>
   <td>und</td>
   <td>given First</td>
   <td>“<span style="background-color:aqua">・</span>”</td>
  </tr>
  <tr>
   <td colspan="7" style="text-align:center">“アルベルト<span style="background-color:aqua">・</span><span style="text-decoration:underline;">アインシュタイン</span>”</td>
  </tr>
  <tr>
   <td>ja_Jpan_JP</td>
   <td>駿</td>
   <td><span style="text-decoration:underline;">宮崎</span></td>
   <td>YES</td>
   <td>ja</td>
   <td>surname First</td>
   <td></td>
  </tr>
  <tr>
   <td colspan="7" style="text-align:center"><span style="text-decoration:underline;">宮崎</span>駿</td>
  </tr>
</table>
<br/>

<table>
  <tr>
   <td colspan="7" ><strong>formatting locale: de_CH</strong>, formatting locale script is Latn</td>
  </tr>
  <tr>
   <td><strong>name locale</strong></td>
   <td><strong>given</strong></td>
   <td><strong>surname</strong></td>
   <td><strong>same<br/>script</strong></td>
   <td><strong>formatting<br/>locale</strong></td>
   <td><strong>order</strong></td>
   <td><strong>foreign<br/>space</strong></td>
  </tr>
  <tr>
   <td>de_Latn_CH</td>
   <td>Albert</td>
   <td>Einstein</td>
   <td>YES</td>
   <td>de</td>
   <td>given First</td>
   <td></td>
  </tr>
  <tr>
   <td colspan="7" style="text-align:center">“Albert Einstein”</td>
  </tr>
  <tr>
   <td>de_Jpan_CH</td>
   <td>アルベルト</td>
   <td>アインシュタイン</td>
   <td>NO</td>
   <td>ja<br/>from script</td>
   <td>given First</td>
   <td>“<span style="background-color:aqua">・</span>”</td>
  </tr>
  <tr>
   <td colspan="7" style="text-align:center">“アルベルト<span style="background-color:aqua">・</span>アインシュタイン”</td>
  </tr>
  <tr>
   <td>und_Latn_JP</td>
   <td>Hayao</td>
   <td>Miyazaki</td>
   <td>YES</td>
   <td>und</td>
   <td>given First</td>
   <td>“<span style="background-color:aqua"> </span>”</td>
  </tr>
  <tr>
   <td colspan="7" style="text-align:center">“Hayao<span style="background-color:aqua"> </span>Miyazaki”</td>
  </tr>
</table>
<br/>

## 7 <a name="7-sample-name" href="#7-sample-name">Sample Name</a>

The sampleName element is used for test names in the personNames LDML data for each locale to aid in testing and display in the CLDR Survey Tool. They are not intended to be used in production software as prompts or placeholders in a user interface and should not be displayed in a user interface.

### 7.1 <a name="7-1-syntax" href="#7-1-syntax">Syntax</a>

```xml
<!ELEMENT sampleName ( nameField+ )  >
<!ATTLIST sampleName item NMTOKENS #REQUIRED >
```

* `NMTOKENS` must be one of `( givenOnly | givenSurnameOnly | given12Surname | full )`. However, these may change arbitrarily in the future.

### 7.2 <a name="7-2-expected-values" href="#7-2-expected-values">Expected values</a>

For the expected sample name items, assume a name such as Mr. Richard “Rich” Edward Smith Iglesias Ph.D.

* `givenOnly` is for an example name with only the given is presented: “Richard” or “Rich” (informal)
* `givenSurnameOnly` is for an example name with only the given name and surname: “Richard Smith” or “Rich Smith” (informal)
* `given12Surname` is for an example using all given names and a surname: “Richard Edward Smith” and “Rich E. Smith” (informal)
* `full` is used to present a name using all fields: “Mr. Richard Edward Smith Iglesias, Ph.D.”

The `nameField` values and their modifiers are described in the [Person Name Object](#3-person-name-object) and [namePattern Syntax](#5-namepattern-syntax) sections.

## 8 <a name="8-personname-data-interface-examples" href="#8-personname-data-interface-examples">PersonName Data Interface Examples</a>

### 8.1 <a name="8-1-example-1" href="#8-1-example-1">Example 1</a>

Greek initials can be produced via the following process in the PersonName object, and returned to the formatter.

* Include all letters up through the first consonant or digraph (including the consonant or digraph).<br/>
(This is a simplified version of the actual process.)

Examples:

* Χριστίνα Λόπεζ (Christina Lopez) ⟶ Χ. Λόπεζ (C. Lopez)
* Ντέιβιντ Λόπεζ (David Lopez) ⟶ Ντ. Λόπεζ (D. Lopez)<br/>Note that Ντ is a digraph representing the sound D.

### 8.2 <a name="8-2-example-2" href="#8-2-example-2">Example 2</a>

To make an initial when there are multiple words, an implementation might produce the following:

* Janus H. W. Young ⇒ {given2-initial} producing “H.W.”.
* Erik Martin van der Poel: {given2-initial} producing “V” by default, but might produce “vdP” or P in other languages.
* A field containing multiple words might not actually initialize all of them, such as in “Mohammed bin Ali bin Osman” (“MAO”).
* John Ronald Reuel Tolkien as “J.R.R. Tolkien” from { given: “John”, given2: “Ronald Reuel”, surname: “Tolkien” }
* The short version of "Son Heung-min" is "H. Son" and not "H. M. Son" or the like. Korean given-names have hyphens and the part after the hyphen is lower-case. 


* * *

Copyright © 2001–2022 Unicode, Inc. All Rights Reserved. The Unicode Consortium makes no expressed or implied warranty of any kind, and assumes no liability for errors or omissions. No liability is assumed for incidental and consequential damages in connection with or arising out of the use of the information or programs contained or accompanying this technical report. The Unicode [Terms of Use](https://www.unicode.org/copyright.html) apply.

Unicode and the Unicode logo are trademarks of Unicode, Inc., and are registered in some jurisdictions.

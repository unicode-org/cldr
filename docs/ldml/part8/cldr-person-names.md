## <a name="CLDR_Person_Names" id="CLDR_Person_Names" href="#CLDR_Person_Names">CLDR Person Names</a>

### <a name="Introduction" id="Introduction" href="#Introduction">Introduction</a>

* CLDR provides formatting for person names, such as John Smith or 宮崎駿. These use patterns to show how a name object (for example, from a database) should be formatted for a particular locale. Name data has fields for the parts of people’s names, such as a **given** field with a value of “Maria”, and a **surname** field value of “Schmidt”.


There is a wide variety in the way that people’s names appear in different languages.

* People may have a different number of names, depending on their culture—they might have only one name (“Zendaya”), two (“Albert Einstein”), or three or more.
* People may have multiple words in a particular name field, eg “Mary Beth” as a given name, or “van Berg” as a surname.
* Some languages, such as Spanish, have two surnames (where each can be composed of multiple words).
* The ordering of name fields can be different across languages, as well as the spacing (or lack thereof) and punctuation.
* Name formatting needs to be adapted to different circumstances, such as a need to be presented shorter or longer; formal or informal context; or when talking about someone, or talking to someone, or as a monogram (JFK).

This document provides the [LDML](tr35.md) specification for formatting of personal names, using data, structure, and examples.

* The CLDR functionality is targeted at formatting names for typical usage on computers (e.g. contact names, automated greetings, etc.), rather than being designed for special circumstances or protocol, such addressing royalty. However, the structure may be enhanced in the future when it becomes clear that additional features are needed for some languages.


This addition to CLDR is based on review of current standards and practices that exist in LDAP, OECD, S42, hCard, HTML and various other international standards and commercial implementations.

* Additions to those structures were made to accommodate known issues in large population groups, such as mononyms in Indonesia, patronymic and matronymic naming structure in Iceland and India, the need for a second surname in Spanish-speaking regions and the common case of chains of patronymic names in Arabic-speaking locales. The formatting patterns allow for specifying different “input parameters” to account for different contexts.


#### <a name="Not_in_scope" id="Not_in_scope" href="#Not_in_scope">Not in scope</a>

The following features are currently out of scope for Person Names formating:

* Grammatical inflection of formatted names.
* Context-specific cultural aspects, such as when to use “-san” vs “-sama” when addressing a Japanese person.
* Providing locale-specific lists of titles, generation terms, and credentials for use in pull-down menus or validation (Mr, Ms., Mx., Dr., Jr., M.D., etc.).
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

### <a name="API_Implementation" id="API_Implementation" href="#API_Implementation">API Implementation</a>

In addition to the settings in this document, it is recommended that implementations provide some additional features in their APIs to allow more control for clients, notably:

1. forceGivenFirst — no matter what the values are in nameOrderLocales or in the NameObject, display the name as givenFirst.
2. forceSurnameFirst — no matter what the values are in nameOrderLocales or in the NameObject, display the name as surnameFirst.
3. forceNativeOrdering — no matter what the values are in nameOrderLocales or in the NameObject, display the name with the same ordering as the native locale.
4. surnameFirstAllCaps — display the surname and surname2 fields in all caps **if** not using native order. Thus where the foreign name ordering is surnameFirst, the name {given=Shinzo, surname=Abe} would display as “ABE Shinzo”.

### <a name="Person_Name_Formatting_Overview" id="Person_Name_Formatting_Overview" href="#Person_Name_Formatting_Overview">Person Name Formatting Overview</a>

Logically, the model used for applying the CLDR data is the following:

![diagram showing relationship of components involved in person name formatting](images/personNamesFormatModel.png)

* Conceptually, CLDR person name formatting depends on data supplied by a PersonName Data Interface. That could be a very thin interface that simply accesses a database record, or it could be a more sophisticated interface that can modify the raw data before presenting it to be formatted. For example, based on the formatting locale a PersonName data interface could transliterate names that are in another script, or supply equivalent titles in different languages.


* The specification below will talk about a “PersonName object” as an entity that is logically accessed via such an interface. If multiple formatted names are needed, such as in different scripts or with alternate names, or pronunciations (eg kana), the presumption is that those are logically separate PersonName objects. See [[Person Name Object](#person-name-object)].


The following summarizes the name data supplied via the PersonName Data Interface:

* Name data is composed of one or more name parts, which are categorized in this standard as
    * _title_ - a string that represents one or more honorifics or titles, such as “Mr.”, or “Herr Doctor”.
    * _given_ - usually a name given to someone that is not passed to a person by way of parentage
* *** _given2_ -**: * _given2_ - name or names that may appear between the first given name string and the surname. In the West, this may be a middle name, in Slavic regions it may be a patronymic name, and in parts of the Middle East, it may be the _nasab (نسب)_ or series of patronymics.

    * _surname_ - usually the family name passed to a person that indicates their family, tribe, or community. In most Western languages, this is known as the last name.
    * _surname2_ - in some cultures, both the parent’s surnames are used and need to be handled separately for formatting in different contexts.
    * _generation_ - a string that represents a generation marker, such as “Jr.” or “III”.
    * _credentials_ - a string that represents one or more credentials or accreditations, such as “M.D.”, or “MBA”.
    * _See the section on [[Fields](#fields)] for more details._
* Name data may have additional attributes that this specification accommodates.
* *** _-informal_ -**: * _-informal_ - A name may have a formal and an informal presentation form, for example “Bob” vs “Robert” or “Са́ша” vs “Алекса́ндра”. This is accomplished by using the simple construct _given-informal_.

* *** _-prefix_ and**: * _-prefix_ and _-core_ - In some languages the surname may have a prefix that needs to be treated differently, for example “van den Berg”. The data can refer to “van den” as _surname-prefix_ and “Berg” with _surname-core_ and the PersonNames formatters will format them correctly in Dutch and many other languages.

    * _See the section on [[Modifiers](#modifiers)] for more details._

To format a name correctly, the correct context needs to be known. The context is composed of:

* **The formatting locale.** This is used to choose the primary set of patterns to format name data.
* **The name locale.** If the name data comes from a locale different from the formatting locale, it may need to be handled differently. If the name locale is not known, an inferred name locale is derived from the information in the name and the formatting locale.
* **Input parameters.**
    * **_order_** - indicates whether the given name comes first or the surname. This is normally specified in the CLDR data for the locale. This feature is also used for the sorting format.
    * **_length_** - used to select patterns for common short, medium, and long formatted names.
    * **_usage_** - this is used to select the correct pattern to format a name when a program is _addressing_ or talking to a person or it is _referring_ to or talking about another person.
    * **_formality_** - This is used to select the formal or informal formatting of a name.
    * _See [[Person Name Attributes](#person-name-attributes)] for more details._

### <a name="Example_Usage" id="Example_Usage" href="#Example_Usage">Example Usage</a>

As an example, consider a person’s name that may contain:

| `title`  | `given`  | `given2` | `surname` | `credentials` |
| -------- | -------- | -------- | --------- | --------      |
|          | Robin    | Finley   | Wang      | Ph.D.         |

If the selected personName data has the following formatting pattern:

> `{title} {given} {given2-initial} {surname}, {credentials}`

Then the output is:

> Robin F. Wang, Ph.D.

* The _title_ field is empty, so both it and the space that follows it in the formatting pattern are omitted from the output, the _given2_ field is formatted as an initial, and a preceding comma is placed before the _credentials_.


Sections below specify the precise manner in which a pattern is selected, and how the pattern is modified for missing fields.


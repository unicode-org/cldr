## <a name="Introduction" id="Introduction" href="#Introduction">Introduction</a>

* Not long ago, computer systems were like separate worlds, isolated from one another. The internet and related events have changed all that. A single system can be built of many different components, hardware and software, all needing to work together. Many different technologies have been important in bridging the gaps; in the internationalization arena, Unicode has provided a lingua franca for communicating textual data. However, there remain differences in the locale data used by different systems.


* The best practice for internationalization is to store and communicate language-neutral data, and format that data for the client. This formatting can take place on any of a number of the components in a system; a server might format data based on the user's locale, or it could be that a client machine does the formatting. The same goes for parsing data, and locale-sensitive analysis of data.


* But there remain significant differences across systems and applications in the locale-sensitive data used for such formatting, parsing, and analysis. Many of those differences are simply gratuitous; all within acceptable limits for human beings, but yielding different results. In many other cases there are outright errors. Whatever the cause, the differences can cause discrepancies to creep into a heterogeneous system. This is especially serious in the case of collation (sort-order), where different collation caused not only ordering differences, but also different results of queries! That is, with a query of customers with names between "Abbot, Cosmo" and "Arnold, James", if different systems have different sort orders, different lists will be returned. (For comparisons across systems formatted as HTML tables, see [[Comparisons](#Comparisons)].)


> **Note:** There are many different equally valid ways in which data can be judged to be "correct" for a particular locale. The goal for the common locale data is to make it as consistent as possible with existing locale data, and acceptable to users in that locale.

* This document specifies an XML format for the communication of locale data: the Unicode Locale Data Markup Language (LDML). This provides a common format for systems to interchange locale data so that they can get the same results in the services provided by internationalization libraries. It also provides a standard format that can allow users to customize the behavior of a system. With it, for example, collation (sorting) rules can be exchanged, allowing two implementations to exchange a specification of tailored collation rules. Using the same specification, the two implementations will achieve the same results in comparing strings. Unicode LDML can also be used to let a user encapsulate specialized sorting behavior for a specific domain, or create a customized locale for a minority language. Unicode LDML is also used in the Unicode Common Locale Data Repository (CLDR). CLDR uses an open process for reconciling differences between the locale data used on different systems and validating the data, to produce with a useful, common, consistent base of locale data.


For more information, see the Common Locale Data Repository project page [[LocaleProject](#localeProject)].

* As LDML is an interchange format, it was designed for ease of maintenance and simplicity of transformation into other formats, above efficiency of run-time lookup and use. Implementations should consider converting LDML data into a more compact format prior to use.


### <a name="Conformance" id="Conformance" href="#Conformance">Conformance</a>

There are many ways to use the Unicode LDML specification and the CLDR data.
The Unicode Consortium does not restrict the ways in which the format or data are used.
However, an implementation may also claim conformance to the LDML specification and/or to CLDR data, as follows:

<a name="UAX35-C1" href="#UAX35-C1"></a>
_**UAX35-C1.**_ An implementation that claims conformance to this specification shall:

1. Identify the sections of the specification that it conforms to.
   * For example, an implementation might claim conformance to all LDML features except for _transforms_ and _segments_.
   * The names of sections may change for clarity, so the associated links should be included in any reference — links into LDML will remain stable.
2. Interpret the relevant elements and attributes of LDML data in accordance with the descriptions in those sections.
* *** For example**: * For example, an implementation that claims conformance to the date format patterns must interpret the characters in such patterns according to [Date Field Symbol Table](tr35-dates.md#Date_Field_Symbol_Table).

3. Declare which types of CLDR data it uses.
   * For example, an implementation might declare that it only uses language names, and those with a _draft_ status of _contributed_ or _approved_.
4. Declare when it overrides CLDR data, or uses `alt` data
   * For example, for `//ldml/numbers/symbols/group` an implementation could use `alt="official"` data.

An implementation may also make a _general claim_ of conformance to the LDML specification and/or CLDR data.
Such a claim is understood to claim conformance to all portions of this specification that are relevant to the operations performed by the implementation,
except for those specifically declared as exceptions.
For example, if an implementation making a  _general claim_ of conformance performs date formatting, and does not declare date formatting as an exception,
it is understood to be claiming conformance to date formatting as described in the section listed below.

~~_**UAX35-C2.**_ An implementation that claims conformance to Unicode locale or language identifiers shall:~~

~~1. Specify whether Unicode locale extensions are allowed~~
~~2. Specify the canonical form used for identifiers in terms of casing and field separator characters.~~

~~External specifications may also reference particular components of Unicode locale or language identifiers, such as:~~

~~> _Field X can contain any Unicode region subtag values as given in Unicode Technical Standard #35: Unicode Locale Data Markup Language (LDML), excluding grouping codes._~~

<a name="UAX35-C2" href="#UAX35-C2"></a>
NOTE: _**UAX35-C2.**_ is replaced by the following generalization.

The following lists the high-level sections with structures and/or processing algorithms.
Conformance to a particular section may reference and require conformance to another section.

#### <a name="Unicode_Locale_Identifiers" id="Unicode_Locale_Identifiers" href="#Unicode_Locale_Identifiers">Unicode Locale Identifiers</a>
| Sections | Topics |
| --- | --- |
| [Unicode Locale Identifier](#Unicode_locale_identifier)| identifier syntax, interpretation, and validity |
| [Annex C. LocaleId Canonicalization](#LocaleId_Canonicalization) | canonicalize |
| [CLDR to BCP 47](#Unicode_Locale_Identifier_CLDR_to_BCP_47), [BCP 47 to CLDR](#Unicode_Locale_Identifier_BCP_47_to_CLDR) | convert |
| [Language Identifier Field Definitions](#Field_Definitions) | interpretation and validity of -u key-value pairs |
| [Locale Display Name Algorithm](tr35-general.md#locale_display_name_algorithm) | locale display names |

#### <a name="Unicode_Locale_Inheritance_and_Matching" id="Unicode_Locale_Inheritance_and_Matching" href="#Unicode_Locale_Inheritance_and_Matching">Unicode Locale Inheritance and Matching</a>
| Sections | Topics |
| --- | --- |
| [Locale Inheritance and Matching](#Locale_Inheritance) | locale inheritance  |
| [Likely Subtags](#Likely_Subtags) | likely subtags |
| [Language Matching](#LanguageMatching) | locale matching |

#### <a name="Units_of_Measurement" id="Units_of_Measurement" href="#Units_of_Measurement">Units of Measurement</a>
| Sections | Topics |
| --- | --- |
| [Unit Identifiers](tr35-general.md#unit-identifiers) | unit identifier syntax, interpretation, and validity |
| [Unit Identifier Normalization](tr35-info.md#Unit_Identifier_Normalization) | identifier normalization |
| [Unit Conversion](tr35-info.md#Unit_Conversion) | unit conversion |
| [Unit Preferences](tr35-info.md#Unit_Preferences) | evaluation of user preferences |
| [Unit Identifier Uniqueness](tr35-general.md#unit-identifier-uniqueness) | converting units into BCP47 format |
| [Compound Units](tr35-general.md#compound-units) | unit display names |

#### <a name="Number_Formatting" id="Number_Formatting" href="#Number_Formatting">Number Formatting</a>
| Sections | Topics |
| --- | --- |
| [Number Format Patterns](tr35-numbers.md#number-format-patterns) | number format patterns, syntax and interpretation |
| [Compact Number Formats](tr35-numbers.md#compact-number-formats) | compact number formats |
| [Rule-Based Number Formatting](tr35-numbers.md#Rule-Based_Number_Formatting) | spell-out number formatting |

#### <a name="Date_Formatting" id="Date_Formatting" href="#Date_Formatting">Date Formatting</a>
| Sections | Topics |
| --- | --- |
| [Elements availableFormats, appendItems](tr35-dates.md#availableFormats_appendItems)  | date formatting, patterns |
| [Date Format Patterns](tr35-dates.md#Date_Format_Patterns) | date format patterns and symbols|
| [Using Time Zone Names](tr35-dates.md#Using_Time_Zone_Names) | timezone forms, fallback and parsing |

#### <a name="Collation" id="Collation" href="#Collation">Collation</a>
| Sections | Topics |
| --- | --- |
| [Root Collation](tr35-collation.md#root-collation) | Root collation syntax and structure |
| [Collation Tailorings](tr35-collation.md#Collation_Tailorings) | Rule syntax and interpretation for language-specific ordering |

#### <a name="Grammar" id="Grammar" href="#Grammar">Grammar</a>
| Sections | Topics |
| --- | --- |
| [Grammatical Features](tr35-general.md#grammatical-features) | noun classes (except for plurals) |
| [Language Plural Rules](tr35-numbers.md#Language_Plural_Rules) | plural and ordinal category rules, ranges |

#### <a name="Miscellaneous" id="Miscellaneous" href="#Miscellaneous">Miscellaneous</a>
| Sections | Topics |
| --- | --- |
| [Unicode Sets](#Unicode_Sets) | Unicode set syntax and interpretation |
| [String Range](#string-range) | string-range syntax and interpretation |
| [Transforms](tr35-general.md#Transforms)| transform identifier and rule syntax and interpretation |
| [Segmentations](tr35-general.md#segmentations) | segmentation customizations |
| [Synthesizing Sequence Names](tr35-general.md#synthesizing-sequence-names) | constructing derived emoji names |
| [Formatting Process](tr35-personNames.md#formatting-process) | person name formatting |
| [Part 7: Keyboards](tr35-keyboards.md) | keyboard structure and interpretation |
| [Conformance](tr35-messageFormat.md#conformance) (Message Format) | message formatting |

### <a name="Customization" id="Customization" href="#Customization">Customization</a>

Conformant implementations cannot modify CLDR structures, such as the syntax or interpretation of locale identifiers.
There are usually mechanisms for implementations to customize these to a certain extent, using what are known a private use codes.
For example, an implementation could use the private-use language code `qfz` to mean a language that was not covered by BCP 47,
or use a [private use extension](#pu_extensions) in a Unicode locale identifer, or use a private-use unit such as `xxx-smoot-per-second`.

An implementation may also use a deprecated code instead of the corresponding preferred code.
For example, the most frequent case of this is with an implementation whose earlier versions predated BCP 47, and used `iw` for Hebrew,
rather than the BCP 47 (and CLDR) code `he`.
When this is done, the CLDR data needs to be modified in appropriate places, not just in some file names.
For example, the languageAlias data requires modification, from:
```xml
<languageAlias type="iw" replacement="he" reason="deprecated"/> <!-- Hebrew -->
```
to
```xml
<languageAlias type="he" replacement="iw" reason="deprecated"/> <!-- Hebrew -->
```

Minimized locale identifiers are also not required. For example, an implementation could consistently expand locale identifiers to include regions, such as `en` → `en_DE` or `de` → `de-AT`.

Implementations may customize CLDR data, as long as they declare that they are doing so. This may include:

#### <a name="Omitting_data" id="Omitting_data" href="#Omitting_data">Omitting data</a>

An implementation may dispense with locale data for locales that an implementation does not support, or for locales it does support,
dispense with data that is at CoverageLevel=Comprehensive, or dispense with particular sorts of data, such a annotations for emoji.

#### <a name="Adding_data" id="Adding_data" href="#Adding_data">Adding data</a>

An implementation could add data for a locale that CLDR does not yet support, or add higher-coverage data for a locale than what CLDR has.

#### <a name="Overriding_data" id="Overriding_data" href="#Overriding_data">Overriding data</a>

CLDR has a mechanism for overriding data using the `alt` mechanism.
At build time, an implementation could override the default value by using an alt value.
For example, take the following data:
```xml
<territory type="HK">Sonderverwaltungsregion Hongkong</territory>
<territory type="HK" alt="short">Hongkong</territory>
```
An implementation could, at build time, substitute the short value for the regular value, getting "Hongkong".
It could instead support both values at runtime, using display option settings to pick between the regular value and the short value.

Implementations can override the data in other ways as well, such as changing the spelling of a particular value.

#### <a name="Testing" id="Testing" href="#Testing">Testing</a>

The files in [testData](https://github.com/unicode-org/cldr/tree/main/common/testData) can be used to test conformance.
Brief instructions for use are supplied in `_readme.txt` files in the different directories and/or in the headers of the files in question.
For example, the following is from a sample header:
```text
# Format:
# <source locale identifier>	;	<expected canonicalized locale identifier>
#
# The data lines are divided into 4 sets:
#   explicit:    a short list of explicit test cases.
#   fromAliases: test cases generated from the alias data.
#   decanonicalized: test cases generated by reversing the normalization process.
#   withIrrelevants: test cases generated from the others by adding irrelevant fields where possible,
#                           to ensure that the canonicalization implementation is not sensitive to irrelevant fields. These include:
#     Language: aaa
#     Script:   Adlm
#     Region:   AC
#     Variant:  fonipa
```

If an implementation overrides CLDR data, then various lines in the relevant test files may need to be modified correspondingly, or skipped.

### <a name="EBNF" id="EBNF" href="#EBNF">EBNF</a>
The EBNF syntax used in LDML is a variant of the Extended Backus-Naur Form (EBNF) notation used in [W3C XML Notation](https://www.w3.org/TR/REC-xml/#sec-notation). The main differences are:

1. Bounded repetition following Perl regex syntax is allowed, such as `digit{3}` for 3 digits, `digit{3,5}` for 3 to 5 digits, and `digit{3,}` for 3 or more digits.
2. Whitespace inside bracketed enumerations and ranges is ignored.
   * eg., `[A-Z a-z]` is the same as `[A-Za-z]`
3. A backslash may be used to escape a following "x"-prefixed hexadecimal code point or the immediately following character.
   * eg., `\x20` is the same as `#x20` and `[\&\-]` is the same as `[#x26#x2D]`
4. Constraints (well-formedness or validity) may use separate notes, and/or the W3C notations:
   * [ wfc: ... ]
   * [ vc: ... ]

In the text, this is sometimes referred to as "EBNF (Perl-based)".


---
title: CLDR Specification (LDML) Style Guide
---

This style guide documents the conventions required for amending and drafting new sections
of the [Unicode Technical Standard \#35 (UTS \#35)](https://www.unicode.org/reports/tr35/).

[UTS \#35][], the CLDR specification, is also known as the Unicode Locale Data Markup Language (LDML) specification,
defines an XML-based format used to exchange structured locale data.
[UTS \#35][] provides a standardized way to define and use locale data related to language,
and regional preferences, such as patterns for formatting dates, times, numbers units, and currencies.

The [W3C Manual of Style][] is a useful resource if don't have experience in drafting technical reports.

> Note: If you're looking for technical details on how to update the spec or CLDR project site, see [Updating the Spec][] or [Updating the CLDR Site][] respectively.

## UTS \#35 structure

The LDML specification governs the structured interchange format used by the [Unicode Common Locale Data Repository (CLDR)][] project.
Because locale-sensitive data spans wide functional domains that range from language display names to dynamic message formatting,
[UTS \#35][] is partitioned into modular sub-documents known as "Parts".
Depending on the proposed change one or more part of [UTS \#35][] may require changes. See [Parts][] for a full list of the current sections in [UTS \#35][].

| Specification Part | Topic Focus | Key Structural Sub-Elements | Primary Specification Path |
| :---- | :---- | :---- | :---- |
| [Part 1: Core] | Overview, languages, locales, basic structure, conformance, customization, etc. | \<identity\>, \<alias\>, \<special\>, EBNF Grammars | tr35.html |
| [Part 2: General] | Display names, transforms, exemplars, etc. | \<localeDisplayNames\>, \<transforms\>, \<characters\> | tr35-general.html |
| [Part 3: Numbers] | Numbering systems, symbols, currencies, plurals, etc. | \<numbers\>, \<symbols\>, \<currencyFormats\>, \<pluralRules\> | tr35-numbers.html |
| [Part 4: Dates] | Calendars, date and time fields, time zones, skeletons, patterns, etc. | \<dates\>, \<calendars\>, \<timeZoneNames\>, \<dateFormatLength\> | tr35-dates.html |
| [Part 5: Collation] | String sorting, tailoring, weighting | \<collations\>, \<collation\>, \<cr\>, \<rules\> | tr35-collation.html |
| [Part 6: Supplemental] | Non-locale-specific metadata, territory specific data, etc. | \<supplementalData\>, \<territoryInfo\>, \<calendarData\> | tr35-info.html |
| [Part 7: Keyboards] | Keyboard layouts, key mappings, input transforms, etc. | \<keyboard\>, \<keyMap\>, \<transforms\>, \<reorders\> | tr35-keyboards.html |
| [Part 8: Person Names] | Person names including name order, patterns, etc. | \<personNames\>, \<personName\>, \<namePattern\> | tr35-personNames.html |
| [Part 9: MessageFormat] | Message format syntax | Expressions, Variants, Selectors, Functions (:number) | tr35-messageFormat.html |
| [Appendix A: Modifications] | Modifications | Log of modifications in the across the specification for the current version | tr35-modifications.html |
| [Appendix B: Acknowledgments] | Acknowledgments | Section-by-section change summaries | tr35-acknowledgments.html |

Contributors must maintain explicit cross-references between individual parts using relative URL anchors and ensure that shared data structures, such
as [Unicode Locale Identifiers][] remain defined in a single section to prevent definitions from drifting.

## Document header layout and metadata

Every part within [UTS \#35][] begins with a standardized metadata block.
Several sections of the block are updated as part of the regular [BRS][] tasks,
or done automatically as part of publishing the latest spec draft. See [Updating the Spec][].
[UTS \#35][] parts are only required to have the UTS version number, title including part name, editors for that part, summary, status,
and a link back to [Part 1: Core](https://www.unicode.org/reports/tr35/tr35.html#unicode-technical-standard-35) which contains the full header.

| Header Metadata Field | Required Format / Value Type | Example Entry |
| :---- | :---- | :---- |
| **Document Title** | Plain text string with UTS designation, and part if applicable | Unicode Technical Standard \#35: Unicode Locale Data Markup Language (LDML) Part 4: Dates |
| **Version** | Major\/Minor release designation and whether the version is currently in draft | 48.2 or 49 (draft) |
| **Editors** | Canonical list of editor names with email anchors | Peter Edberg, Mark Davis, and other CLDR committee members |
| **Date** | ISO Date format (YYYY-MM-DD) of the most recent modification | 2026-03-03 |
| **Version Links** | Links to current, previous, and latest versions | This Version, Previous Version, Latest Version |
| **Reference Links** | Relative links to associated assets | Corrigenda, Latest Proposed update, DTDs, Namespace, Change History |

A standardized summary block must immediately follow the metadata header, explicitly stating the scope of the document, its stability status, and its independent technical standard relationship to the core Unicode Standard.

## Conformance Clause Drafting Conventions

All normative rules and conformance requirements in UTS \#35 follow the structural paradigm established by requirement [UAX35-C1][].
Conformance statements must be unambiguous, testable, and explicitly enumerate implementation obligations.
See [UAX35-C1][] for more details.

> TODO: Add tips for authors about what is needed for conformance based on [UAX35-C1][].

## Terminology

Specification authors must maintain rigorous discipline regarding defined terminology. Ambiguous or conversational prose should be replaced with standardized terminology.
Terminology specific to [UTS \#35][] should be defined in the specification itself whenever possible.
The CLDR Technical Committee maintains a glossary of common [terms][] on the project site.

## **Formal Grammar and Syntactic Notation Standards**

UTS \#35 utilizes four standard formalisms to define valid syntax depending on the data domain. Specification proposals must employ the correct formalism corresponding to the section's technical requirements:

### Extended Backus-Naur Form (EBNF)

Used for identifier syntaxes, syntax definitions, and pattern structures (e.g., Unicode Locale Identifiers, BCP 47 extension subtags).
EBNF productions must use clear nonterminal naming conventions (e.g., unicode\_language\_subtag, uattribute, ufield).

### Document Type Definition (DTD)

Used to define the XML document structure and schema rules.
DTD fragments must be rendered in standard element and attribute declaration syntax representing precise content models.

### UnicodeSet Syntax

Used for defining character sets within key maps, exemplars, and transform filters.
Standard square-bracket notation must be used alongside formal set operations such as ranges, set intersections, and negations.

> Note: [UTC][] is planning on a new specification for [UnicodeSet][],
> because it is used across the Unicode encoding specifications (as well as in the higher levels: CLDR, ICU4*).
> See [UTS \#61][]: Unicode Set Notation (currently in draft). It is a much more complete and rigorous specification than what is in the CLDR specification [UTS \#35][].
> Following [UTC][] approval of [UTS \#61][], CLDR plans to deprecate the section in [UTS \#35][],
> and redirect people to the new [UTS \#61][].
> [UTS \#35][] will retain a short description, and make sure that all the links redirect reasonably. [CLDR-18624][]

### Rule Notation Syntaxes

See [Part 2: General][] and [Part 5: Collation][] for examples.
Segmentation rules must explicitly declare boundary conditions using ÷ (break allowed) and × (break forbidden) along with numeric rule identification order (e.g., id="10.15").
Transform rules must utilize standard mapping symbols (→, ←, ↔), variable assignments ($var \= ...;), and cursor positioning marks (| or ⸠).

## XML Data Modeling and Markup Conventions

To ensure reliable machine parsing, data inheritance, and automated workflows, all proposed XML structural additions must adhere strictly to five essential markup constraints:

> 1. **Separation of Translatable Content and Metadata**: All localizable or translatable textual data must reside exclusively in element textual content. XML attributes are strictly reserved for non-translatable structural metadata, structural keys, types, and numeric constraints.  
> 2. **Prohibition of Mixed Content in Structure Elements**: Structure elements containing textual data must never contain child XML elements, ensuring clean XPath calculations and translation unit extraction.  
> 3. **XPath Uniqueness Requirement**: Schema additions must guarantee that no two distinct leaf text nodes generate an identical XPath within a fully resolved data file.  
> 4. **Attribute-Value Mutually Exclusive Child Element Rule**: XML elements possessing structural value attributes must not contain child elements.  
> 5. **Extension and Redirection Mechanisms**: Vendor-specific or experimental data extensions must be encapsulated within \<special\> elements utilizing an explicit XML namespace (xmlns). Internal data redirection and fallback lookup targets must utilize \<alias\> elements with normalized source paths.


[BRS]: /development/cldr-big-red-switch
[Parts]: https://unicode.org/reports/tr35/#parts
[Part 1: Core]: https://unicode.org/reports/tr35/tr35.html
[Part 2: General]: https://unicode.org/reports/tr35/tr35-general.html
[Part 3: Numbers]: https://unicode.org/reports/tr35/tr35-numbers.html
[Part 4: Dates]: https://unicode.org/reports/tr35/tr35-dates.html
[Part 5: Collation]: https://unicode.org/reports/tr35/tr35-collation.html
[Part 6: Supplemental]: https://unicode.org/reports/tr35/tr35-info.html
[Part 7: Keyboards]: https://unicode.org/reports/tr35/tr35-keyboards.html
[Part 8: Person Names]: https://unicode.org/reports/tr35/tr35-personNames.html
[Part 9: MessageFormat]: https://unicode.org/reports/tr35/tr35-messageFormat.html
[Appendix A: Modifications]: https://unicode.org/reports/tr35/tr35-modifications.html
[Appendix B: Acknowledgments]: https://unicode.org/reports/tr35/tr35-acknowledgments.html
[terms]: /cldr-spec/definitions
[UAX35-C1]: https://unicode.org/reports/tr35/#conformance
[Unicode Common Locale Data Repository (CLDR)]: /index
[Unicode Locale Identifiers]: https://unicode.org/reports/tr35/#Unicode_locale_identifier
[UnicodeSet]: https://unicode.org/reports/tr35/#Unicode_Sets
[Updating the CLDR Site]: /development/updating-site
[Updating the Spec]: /cldr-spec/updating-spec
[UTC]: https://www.unicode.org/consortium/utc.html
[UTS \#35]: https://www.unicode.org/reports/tr35/
[UTS \#61]: https://www.unicode.org/reports/tr61/
[W3C Manual of Style]: https://www.w3.org/guide/manual-of-style/

[CLDR-18624]: https://unicode-org.atlassian.net/browse/CLDR-18624

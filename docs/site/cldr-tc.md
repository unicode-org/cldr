---
title: "CLDR Technical Committee (TC)"
---

# CLDR Technical Committee (TC)

The CLDR Technical Committee is responsible for the Unicode Common Locale Data Repository data repository,
which encompasses:

* the data repository itself, in XML,
* the specification for the data and structure [UTS 35 (LDML)][]
* the tooling used to gather locale data, test the data and structure, and provide test data for implementations
* additional other formats for the locale data (eg, JSON)

The [CLDR home page][] provides a longer overview. For information on the working groups (WGs) under the CLDR Technical Committee, see the descriptions below.
For the leadership of the TC and WGs, see the listings on [Unicode Technical Group Leadership][].

## CLDR Working Groups (WGs)

The CLDR WGs were established by the TC to perform tasks at the direction of the CLDR TC 
and/or develop proposals and recommendations for Technical Decisions for approval by the TC. 

The CLDR WGs are standing working groups set up to address specific technical and operational domains on an ongoing basis
(e.g., technical design, CLDR infrastructure or digitally disadvantaged languages advancement). 

All proposals and recommendations made by a CLDR WG are subject to approval or further action by the CLDR TC.
_**They can not make any Technical Decisions on their own.**_
More detailed information about the procedures related to working groups are available in the [Unicode® Technical Group Procedures][]

### CLDR Conformance Testing Working Group

The [CLDR Conformance Testing WG][] is tasked with ensuring consistency between implementations of [UTS 35 (LDML)][], the CLDR specification,
and other Unicode data-backed standards, including [ICU4C, ICU4J][], [ICU4X][], and [ECMA-402][].

**Goals:**

* New features have high quality CLDR test data.
* All libraries have a conformance score for those new features.
* The latest library versions are reflected in the scorecard.

**More details:** 

* Project page: <https://github.com/unicode-org/conformance>
* Dashboard: <https://unicode-org.github.io/conformance>
* In addition to the development and maintenance of the web-based conformance scorecard, the Working Group liaises with the other Technical Committees to ensure that:
  * Aware and involved in fixing bugs in their respective implementations of the LDML
  * Participate in Design Working Group discussions to resolve issues that stem from the need for clarity or changes in the LDML
  * Create test data files, auto-generated for each version of CLDR data, that can be used by any i18n library wishing to ensure to its users that its functionality is conformant

### CLDR Design Working Group

The [CLDR Design WG][] is responsible for assessing and investigating issues that require design work.
This includes issues that affect the ICU-TC and ICU4X TC.
The results are recommendations to the CLDR-TC (and possibly the ICU-TC and ICU4X TC) for resolution of the issue.
The recommendation may be to close the issue, or to produce a design that can meet the goals of the issue.

The CLDR TC relies on the [CLDR Design WG][] to recommend a design for any more complex changes
since the CLDR TC no longer has enough overlapping membership with library projects,
nor time to have long design discussions.

### CLDR Keyboard Working Group

The [CLDR Keyboard WG][] is responsible for the development and maintenance of a standard cross-platform XML format for use by keyboard authors.
These recommendations, once approved, are incorporated into the CLDR data and tooling, and the [UTS 35 Part 7: Keyboards][].

The WG also manages the process of validating new keyboards contained in the CLDR repository in the directory [cldr/keyboards][] as defined by the [Keyboard Intake Procedures][].

### CLDR MessageFormat Working Group

The [CLDR MessageFormat WG][] was tasked with developing the specification for Message Format 2 (MF2) and ensuring implementations of MF2 in the ICU and ICU4X libraries. These recommendations, once approved, are incorporated into the CLDR data and tooling, and the LDML specification [UTS 35 Part 9: MessageFormat][].

The main page for the Message Format WG is in the [MessageFormat Repository][].

### CLDR Person Name Working Group

The [Person Name WG][] is responsible for the development of recommendations for person name formatting structure and data. These recommendations, once approved, are incorporated into the CLDR data and tooling, and the LDML specification [UTS 35 Part 8: Person Names][]. There is a development version at Part 8: Person Names (development).

### CLDR Ops Working Group

The [CLDR Ops WG][] is responsible for maintaining and extending the code and tools that support the Survey Tool and processing/production of the CLDR data
(including test data).

It also is responsible for progressing through the [CLDR BRS][],
the running list of tasks that need to be completed for a successful release of a new version of CLDR.
However, any changes to the [CLDR BRS][] need to be approved by the CLDR TC if they affect the released CLDR product. 

It was formerly known as the CLDR Infrastructure Working Group,
but was renamed to disambiguate from the Unicode Infrastructure group since both groups were sometimes referred to as ‘Infrastructure’ as shorthand.

### DDL Working Group

Contributors for _Digitally Disadvantaged Languages (DDL)_ face unique challenges.
The [CLDR DDL WG][] has been formed to evaluate mechanisms to make it easier for contributors for DDLs to:

* become contributors to CLDR
* improve the coverage for their language in CLDR
* raise the status of their contributions, so that the CLDR data for their language is incorporated into more products.

Additionally, the CLDR TC may delegate any issue requiring research for an issue affecting a DDL to the DDL WG, including but not limited to: 

* Evaluating requests of new locale Core data requests

[CLDR BRS]: /development/cldr-big-red-switch
[CLDR Design WG]: /cldr-tc/design-wg
[CLDR DDL WG]: /ddl
[CLDR home page]: https://cldr.unicode.org/
[CLDR Keyboard WG]: https://cldr.unicode.org/index/keyboard-workgroup
[cldr/keyboards]: https://github.com/unicode-org/cldr/tree/main/keyboards
[CLDR MessageFormat WG]: /cldr-tc/message-format-wg
[CLDR Ops WG]: /cldr-tc/infrastructure-wg
[ECMA-402]: https://ecma-international.org/publications-and-standards/standards/ecma-402/
[ICU4C, ICU4J]: https://icu.unicode.org/
[ICU4X]: https://icu4x.unicode.org/
[Keyboard Intake Procedures]: https://cldr.unicode.org/index/process/keyboard-repository-process
[MessageFormat Repository]: https://github.com/unicode-org/message-format-wg
[UTS 35 Part 7: Keyboards]: https://www.unicode.org/reports/tr35/tr35-keyboards.html
[UTS 35 Part 8: Person Names]: https://www.unicode.org/reports/tr35/tr35-personNames.html
[UTS 35 Part 9: MessageFormat]: https://www.unicode.org/reports/tr35/tr35-messageFormat.html
[Person Name WG]: /cldr-tc/person-name-wg
[CLDR Conformance Testing WG]: /cldr-tc/conformance_wg
[Unicode Technical Group Leadership]: https://www.unicode.org/consortium/techcommittees.html
[Unicode® Technical Group Procedures]: https://www.unicode.org/consortium/tc-procedures.html
[UTS 35 (LDML)]: https://www.unicode.org/reports/tr35/

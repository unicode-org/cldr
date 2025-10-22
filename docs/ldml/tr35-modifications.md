## Unicode Technical Standard #35

# Unicode Locale Data Markup Language (LDML)<br/>Appendix A: Modifications

|Version|48 (draft)|
|-------|----------|
|Editors|<a href="tr35-acknowledgments.md#acknowledgments">CLDR committee members|

For the full header, summary, and status, see [Part 1: Core](tr35.md).

### _Summary_

This is a partial document, describing only the changes to the LDML since the previous release. For the other parts of the LDML see the [main LDML document](tr35.md) and the links above.

### _Status_

_This is a draft document which may be updated, replaced, or superseded by other documents at any time.
Publication does not imply endorsement by the Unicode Consortium.
This is not a stable document; it is inappropriate to cite this document as other than a work in progress._

<!-- _This document has been reviewed by Unicode members and other interested parties, and has been approved for publication by the Unicode Consortium.
This is a stable document and may be used as reference material or cited as a normative reference by other specifications._ -->

> _**A Unicode Technical Standard (UTS)** is an independent specification. Conformance to the Unicode Standard does not imply conformance to any UTS._

_Please submit corrigenda and other comments with the CLDR bug reporting form [[Bugs](https://cldr.unicode.org/index/bug-reports)].
Related information that is useful in understanding this document is found in the [References](tr35.md#References).
For the latest version of the Unicode Standard see [[Unicode](https://www.unicode.org/versions/latest/)].
For more information see [About Unicode Technical Reports](https://www.unicode.org/reports/about-reports.html) and the [Specifications FAQ](https://www.unicode.org/faq/specifications.html).
Unicode Technical Reports are governed by the Unicode [Terms of Use](https://www.unicode.org/copyright.html)._

## Parts

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
*   Appendix A: [Modifications](tr35-modifications.md#modifications)
*   Appendix B: [Acknowledgments](tr35-acknowledgments.md#acknowledgments)

## <a name="Contents" href="#Contents">Contents of Appendix A, Modifications</a>

* [Modifications](#modifications)
  * [Locale Identifiers and Names](#locale-identifiers-and-names)
  * [Misc.](#misc)
  * [DateTime formats](#datetime-formats)
  * [Numbers](#numbers)
  * [Units of Measurement](#units-of-measurement)
  * [Collation](#collation)
  * [MessageFormat](#messageformat)
  * [Keyboards](#keyboards)
  * [Modifications section](#modifications-section)
  * [Acknowledgments section](#acknowledgments-section)

## Modifications

<!--
      Note: As of CLDR-18209, ALL changes to the spec require this file, tr35-modifications.md to be modified as well. If you are making a change that doesn't need to be noted, or is already noted, please simply add an HTML comment (such as this one) to the appropriate section.  The comment will not be visible to viewers of the HTML page.
-->

**Changes in LDML Version 48 (Differences from Version 47)**
<!-- Update the date (need to add/update this comment to pass CI tests that require mods to be updated when spec is) -->
<!-- Some internal links were updated to fix broken links. -->
<!-- fixed broken links -->

### Locale Identifiers and Names
* [Display Name Elements](tr35-general.md#display-name-elements) Described the usage of the `language` element `menu` values `core` and `extension`, and `alt="menu"`.
Also revamped the description of how to construct names for locale IDs, for clarity.
* [Special Script Codes](tr35.md#special-script-codes) Added the `Hntl` compound script. (This is also reflected in the `<scriptData>` elements in supplementalData.xml.)
* [Likely Subtags](tr35.md#likely-subtags) Changed the Canonicalize step to point to the section on canonicalization.
* [Unicode Locale Identifier](tr35.md#unicode-locale-identifier) Changed the `attribute` component in the EBNF to be `uattribute` for consistency with `ufield`, etc.
and to reduce confusion with XML attributes.
* [Unicode Subdivision Codes](tr35.md#Unicode_Subdivision_Codes) Added more
explanation about the potential reuse of ISO 3166-2 codes, and clarified that
CLDR does not closely monitor all ISO 3166-2 changes.

### Misc.
* [Character Elements](tr35-general.md#character-elements) Added new exemplar types.
* [DTD Annotations](tr35.md#DTD_Annotations) Added the @CDATA annotation, to indicate which elements are generated with @CDATA format
* [Person Name Validation](tr35-personNames.md#person-name-validation) Added guidance for validating person names.
* [Supplemental Language Data](tr35-info.md#Supplemental_Language_Data) For the `language` subelement of `languageData`, the `territory`
attribute has been deprecated (and data using it has been removed). A better source for such information is the more detailed data
in [Supplemental Territory Information](tr35-info.md#Supplemental_Territory_Information).

### DateTime formats

* [Element dateTimeFormat](tr35-dates.md#dateTimeFormat) Added a new type `relative` for relative date/times, such as "tomorrow at 10:00",
and updated the guidelines for using the different `dateTimeFormat` types.
* [Using Time Zone Names](tr35-dates.md#using-time-zone-names) Removed the "specific location format".
* [timeZoneNames Elements Used for Fallback](tr35-dates.md#timeZoneNames_Elements_Used_for_Fallback) Added the `gmtUnknownFormat`, to indicate when the timezone is unknown.
* [Metazone Names](tr35-dates.md#metazone-names) Added `usesMetazone`, to specify which offset is considered standard time, and which offset is considered daylight.
* [Time Zone Format Terminology](tr35-dates.md#time-zone-format-terminology) Added the **Localized GMT format** (replacing the **Specific location format**).
This affects the behavior of the `z` timezone format symbol.
There is also now a mechanism for finding the region code from short timezone identifier, which is used for the _non-location formats (generic or specific)_
* [Calendar Data](tr35-dates.md#calendar-data) Specified more precisely the meaning of the `era` attributes in supplemental data, and how to determine the transition point in time between eras.

### Numbers
* [Plural rules syntax](tr35-numbers.md#plural-rules-syntax) Added substantial clarifications and new examples.
The order of execution is also clearly specified.
* [Compact Number Formats](tr35-numbers.md#compact-number-formats) Specified the mechanism for formatting compact numbers more precisely.
* [Rule-Based Number Formatting](tr35-numbers.md#) Added a full specification.
The rules have been converted to a “flat” format, which is easier for clients to handle (the old format will be retained for one more release).
* [Rational Numbers](tr35-numbers.md#rational-numbers) Added support for formatting fractions like 5½ in technical preview.

### Units of Measurement
* [Unit Syntax](tr35-general.md#unit-syntax) Simplified the EBNF `product_unit` and added an additional well-formedness constraint for mixed units.
* [Unit Identifier Normalization](tr35-info.md#Unit_Identifier_Normalization) Modified the normalization process
* [Mixed Units](tr35-general.md#Unit_Sequences) Modified the guidance for handling precision.

### Collation
* [Collation](tr35-collation.md) Added the new `FractionalUCA_blanked.txt` to the root collation data files.

### MessageFormat
* Syntax and data model errors must now be prioritized over other errors <!-- ([\#1011](https://github.com/unicode-org/message-format-wg/pull/1011)) -->
* The Default Bidi Strategy is now required and default <!-- ([\#1066](https://github.com/unicode-org/message-format-wg/pull/1066)) -->
* The `:offset` function (previously named `:math`) is now available as Stable <!-- ([\#1073](https://github.com/unicode-org/message-format-wg/pull/1073)) -->
* The `:datetime`, `:date`, and `:time` _draft_ functions are updated to build on top of semantic skeletons <!-- ([\#1078](https://github.com/unicode-org/message-format-wg/pull/1078), [\#1083](https://github.com/unicode-org/message-format-wg/pull/1083)) -->
* `:percent` is added as a new _draft_ function <!-- ([\#1094](https://github.com/unicode-org/message-format-wg/pull/1094)) -->
* The format is renamed to "Unicode MessageFormat" for clarity <!-- ([\#1064](https://github.com/unicode-org/message-format-wg/pull/1064)) -->
* The pattern selection definition is refactored to be easier to understand without changing its meaning <!-- ([\#1080](https://github.com/unicode-org/message-format-wg/pull/1080)) -->

### Keyboards

* [`display`](tr35-keyboards.md#element-display): Noted that a key without output may be indicated by means of the `keyId=` attribute on the display.
* [`layer`](tr35-keyboards.md#element-layer): Noted the use of the `modifiers=` attribute for hardware layouts being used as touch layouts.
* References and links into the section concerning keyboard test data (which was removed prior to spec finalization) were removed.

### Modifications section

* The Modifications section was split out to its own file, [Appendix A, Modifications](tr35-modifications.md)

### Acknowledgments section

* The Acknowledgments section was split out to its own file, [Appendix B, Acknowledgments](tr35-acknowledgments.md)

Note that small changes such as typos and link fixes are not listed above.
Modifications in previous versions are listed in those respective versions.
Click on **Previous Version** in the header until you get to the desired version.

* * *

© 2001–2025 Unicode, Inc.
This publication is protected by copyright, and permission must be obtained from Unicode, Inc.
prior to any reproduction, modification, or other use not permitted by the [Terms of Use](https://www.unicode.org/copyright.html).
Specifically, you may make copies of this publication and may annotate and translate it solely for personal or internal business purposes and not for public distribution,
provided that any such permitted copies and modifications fully reproduce all copyright and other legal notices contained in the original.
You may not make copies of or modifications to this publication for public distribution, or incorporate it in whole or in part into any product or publication without the express written permission of Unicode.

Use of all Unicode Products, including this publication, is governed by the Unicode [Terms of Use](https://www.unicode.org/copyright.html).
The authors, contributors, and publishers have taken care in the preparation of this publication,
but make no express or implied representation or warranty of any kind and assume no responsibility or liability for errors or omissions or for consequential or incidental damages that may arise therefrom.
This publication is provided “AS-IS” without charge as a convenience to users.

Unicode and the Unicode Logo are registered trademarks of Unicode, Inc. in the United States and other countries.

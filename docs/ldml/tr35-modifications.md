---
appendix: A
title: Modifications
---
## Unicode Technical Standard #35

# Unicode Locale Data Markup Language (LDML)<br/>Appendix A: Modifications

For the full header, table of contents, and status, see [Part 1: Core](tr35.md).

### _Summary_

This is a partial document, describing only the changes to the LDML since the previous release. For the other parts of the LDML see the [main LDML document](tr35.md).

## <a name="Contents" href="#Contents">Contents of Appendix A, Modifications</a>

* [Modifications](#modifications)
  * [Changes in LDML Version 49 (Differences from Version 48.0)](#changes-in-ldml-version-49-differences-from-version-480)
  * [Locales](#locales)
    * [Date and Time](#date-and-time)
    * [MessageFormat](#messageformat)
  * [Numbers](#numbers)
  * [Units](#units)
  * [Keyboard](#keyboard)
  * [Segmentation](#segmentation)

## Modifications

<!--
      Note: As of CLDR-18209, ALL changes to the spec require this file, tr35-modifications.md to be modified as well. If you are making a change that doesn't need to be noted, or is already noted, please simply add an HTML comment (such as this one) to the appropriate section.  The comment will not be visible to viewers of the HTML page.

      - regenerated ToC
      - bump spec date

-->

### Changes in LDML Version 49 (Differences from Version 48.0)
<!-- Updated spec date -->

### Locales

* [`typeValues`](tr35-general.md#type-values) Added typeValues for On/Off translations in menus and locale display names
<!-- CLDR-19394 -->
* [Key/Type Descriptions](tr35.md#Key_And_Type_Definitions) In the Key/Type Description table, added a description of which keys/types use constructed values, and a brief description of the `typeValue` element.
    * These elements are not present in root.xml. When not present in a top-level locale, they fall back to the key or value identifier.
    * Also changed the table from HTML into Markdown, with each Key description (such as `co` for collation) having its own level-five section heading.
* [Boundary Spacing](tr35-general.md#Character_Boundary_Spacing) Added new description of `placeholderBoundarySpacing`. <!-- CLDR-19227 -->
* (48.2) New section [Nested Bracket Replacement](tr35-general.md#Character_Nested_Bracket_Replacement).
* (48.2) [Locale Display Name Algorithm](tr35-general.md#locale_display_name_algorithm) updated to use the nested bracket replacement data and avoid nested parentheses by flattening `-t-` (transform) language names.
    * As part of this, the display name order is changed so that any names for `-u-` items appear _before_ (instead of after) any names for `-t-` items.
* (48.2) Specified that missing `<keys>` translations should fall back to the key identifier.
* (48.2) The section "Enhanced Language Matching" was retitled to [Language Matching Variables](tr35.md#enhanced-language-matching) and clarified.

#### Date and Time

* [Calendar era `code`s](tr35-dates.md#Calendar_Data) Added length limit and clarified format.
<!-- CLDR-5717 updated some example -->

* [Date Field Symbols & Skeletons](tr35-dates.md#availableFormats_appendItems) Document that pattern-only symbols (`L`, `q`, `c`), non-canonical hour symbols (`K`, `k`), and invalid day period combinations are discouraged in skeletons and planned for deprecation in CLDR 50.
<!-- CLDR-19757 -->
* [Missing Skeleton Fields](tr35-dates.md#Missing_Skeleton_Fields) Added `Date-Timezone` pattern for gluing date and time zone fields together, evaluated before `Time-Day-Of-Week`, and clarified placeholder assignments and usage in `appendItems`.
<!-- CLDR-19066 -->
* [`interval formats`](tr35-dates.md#format-range-separator-patterns) Described the new interval range separator patterns and how they are used to produce fallback patterns.
* [`Numeric date/time separators`](tr35-dates.md#elements-numericdateseparator-numerictimeseparator) Added separators for times and numeric dates in technical preview to allow easier customization.
<!-- CLDR-9980 -->
* [`Ordinal days`](tr35-dates.md#element-dayofmonth), [`ddd symbol`](tr35-dates.md#Date_Field_Symbol_Table) Added documentation of `dayOfMonth` elements and the related `ddd` symbol in technical preview.

* [Hour Cycle Pattern Variations](tr35-dates.md#Semantic_Hour_Cycle_Pattern_Variations) Specified how semantic skeleton hour cycle options (`Clock12`, `Clock24`, `H11`, `H12`, `H23`, `H24`) adjust the matched pattern after standard skeleton matching.
<!-- CLDR-18894 -->

* [`Time Zone Names`](tr35-dates.md#Time_Zone_Names) Removed `gmtZeroOffset` item.
* [`Time_Zone_Format_Terminology`](tr35-dates.md#Time_Zone_Format_Terminology) Clarified that not all timezones have location (or location format).
* [`Time_Zone_Goals`](tr35-dates.md#Time_Zone_Goals) Modified the location format construction.
* [`Time Zone Format Terminology`](tr35-dates.md#Time_Zone_Format_Terminology) Clarified the computation of the exemplar city for non-location zones.

#### MessageFormat

* [:currency](tr35-messageFormat.md#the-currency-function), [:percent](tr35-messageFormat.md#the-percent-function) The `:currency` and `:percent` functions are now Stable, with the same implementations as previously.
* The `u:locale` option (previously in Draft) has been dropped from the specification.

### Numbers

* [`Currencies`](tr35-numbers.md#Currencies) Revised the `numberFormat` description for currencies.
<!-- CLDR-18963 -->
* [`Plural rules syntax`](tr35-numbers.md#plural-rules-syntax) Made it clear that plural rules are evaluated in semantic order (`zero`, then `one`,…).
<!-- CLDR-19012 -->

### Units

* [Unit Preferences Data](tr35-info.md#Unit_Preferences_Data) Made formatting and phrasing more internally consistent
<!-- CLDR-19737 -->

### Keyboard
* References and links into the section concerning keyboard test data (which was removed prior to spec finalization) were removed.
* [Default Backspace Transform](tr35-keyboards.md#default-backspace-transform) Normalization for the default backspace transform was clarified,
and authors were encouraged to add backspace transforms to avoid the default.

### Segmentation
* (48.1) [Segmentations](tr35-general.md#Segmentations) removed outdated note about `X Format*->X`.

----

Note that small changes such as typos and link fixes are not listed above.
Modifications in previous versions are listed in those respective versions.
Click on **Previous Version** in the header until you get to the desired version.

* * *

© 2001–2026 Unicode, Inc.
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
<!-- Auto SpecFix Wed Apr 29 15:28:28 UTC 2026 -->

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
Related information that is useful in understanding this document is found in the [References](#References).
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
  * [DateTime formats](#datetime-formats)
  * [Modifications section](#modifications-section)

## Modifications

**Changes in LDML Version 48 (Differences from Version 47)**

### DateTime formats

* In [Element dateTimeFormat](tr35-dates.md#dateTimeFormat), added a new type `relative` and updated the
guidelines on how to use the different `dateTimeFormat` types.
* In [Using Time Zone Names](tr35-dates.md#Using_Time_Zone_Names), the "specific location format" was removed

Note that small changes such as typos and link fixes are not listed above.
Modifications in previous versions are listed in those respective versions.
Click on **Previous Version** in the header until you get to the desired version.

### Modifications section

* This Modifications section was split out to its own part, [Appendix A, Modifications](tr35-modifications.md)

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

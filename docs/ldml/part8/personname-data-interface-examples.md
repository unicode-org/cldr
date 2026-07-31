## <a name="PersonName_Data_Interface_Examples" id="PersonName_Data_Interface_Examples" href="#PersonName_Data_Interface_Examples">PersonName Data Interface Examples</a>

### <a name="Example_1" id="Example_1" href="#Example_1">Example 1</a>

Greek initials can be produced via the following process in the PersonName object, and returned to the formatter.

* Include all letters up through the first consonant or digraph (including the consonant or digraph).<br/>
(This is a simplified version of the actual process.)

Examples:

* Χριστίνα Λόπεζ (Christina Lopez) ⟶ Χ. Λόπεζ (C. Lopez)
* Ντέιβιντ Λόπεζ (David Lopez) ⟶ Ντ. Λόπεζ (D. Lopez)<br/>Note that Ντ is a digraph representing the sound D.

### <a name="Example_2" id="Example_2" href="#Example_2">Example 2</a>

To make an initial when there are multiple words, an implementation might produce the following:

* A field containing multiple words might skip some of them, such as in “Mohammed bin Ali bin Osman” (“MAO”).
* The short version of "Son Heung-min" is "H. Son" and not "H. M. Son" or the like. Korean given-names have hyphens and the part after the hyphen is lower-case.


* * *

© 2001–2026 Unicode, Inc.
This publication is protected by copyright, and permission must be obtained from Unicode, Inc.
prior to any reproduction, modification, or other use not permitted by the [Terms of Use](https://www.unicode.org/copyright.html).
Specifically, you may make copies of this publication and may annotate and translate it solely for personal or internal business purposes and not for public distribution,
provided that any such permitted copies and modifications fully reproduce all copyright and other legal notices contained in the original.
* You may not make copies of or modifications to this publication for public distribution, or incorporate it in whole or in part into any product or publication without the express written permission of Unicode.


Use of all Unicode Products, including this publication, is governed by the Unicode [Terms of Use](https://www.unicode.org/copyright.html).
The authors, contributors, and publishers have taken care in the preparation of this publication,
* but make no express or implied representation or warranty of any kind and assume no responsibility or liability for errors or omissions or for consequential or incidental damages that may arise therefrom.

This publication is provided “AS-IS” without charge as a convenience to users.

Unicode and the Unicode Logo are registered trademarks of Unicode, Inc. in the United States and other countries.```

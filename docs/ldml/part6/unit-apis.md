## <a name="Unit_APIs" id="Unit_APIs" href="#Unit_APIs">Unit APIs</a>
APIs should clearly allow for both the use of unit preferences with the above process, and for the _invariant use_ of a unit measure.
That is, while an application will usually want to obey the preferences for the locale or in the locale ID, there will definitely be instances where it will want to not use them.
For example, in showing the weather, an application may want to show:

High today: 68°F (20°C)

To do that, the application needs to show the first value with the locale information, and then (a) query what the alternative is, and show the temperature in that.
As an example, ICU only uses the unit preferences (with rg, ms, and/or mu and the likely region) in formatting units when a **usage** parameter is set.

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

Unicode and the Unicode Logo are registered trademarks of Unicode, Inc. in the United States and other countries.

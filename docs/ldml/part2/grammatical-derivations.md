## <a name="Grammatical_Derivations" id="Grammatical_Derivations" href="#Grammatical_Derivations">Grammatical Derivations</a>

```dtd
<!ELEMENT grammaticalData ( grammaticalFeatures*, grammaticalDerivations*) >
<!ELEMENT grammaticalDerivations (deriveCompound*, deriveComponent*) >
<!ATTLIST grammaticalDerivations locales NMTOKENS #REQUIRED >

<!ELEMENT deriveCompound EMPTY >
<!ATTLIST deriveCompound feature NMTOKENS #REQUIRED >
<!ATTLIST deriveCompound structure NMTOKENS #REQUIRED >
<!ATTLIST deriveCompound value NMTOKEN #REQUIRED >

<!ATTLIST deriveComponent feature NMTOKENS #REQUIRED >
<!ATTLIST deriveComponent structure NMTOKENS #REQUIRED >
<!ATTLIST deriveComponent value0 NMTOKEN #REQUIRED >
<!ATTLIST deriveComponent value1 NMTOKEN #REQUIRED >
```

* The grammatical derivation data contains information about the case, gender, and plural categories of compound units. This is supplemental data, so the inheritance by locale needs to be done by the client.


* **Note: In CLDR**: _Note: In CLDR v38, the data for two locales is provided so that implemenations can ready their code for when more locale data is available. In subsequent releases structure may be further extended as more locales are added, to deal with additional locale requirements._


A compound unit can use 4 mechanisms, illustrated here in formatted strings:

* **Prefix**: 1  **kilo**gram
* **Power**: 3 **square** kilometers
* **Per**: 3 kilograms **per** meter
  * An edge case is where there is no numerator, such as “1 per-second”
* **Times**: 3 kilowatt<strong>-</strong>hours

* For the purposes of grammatical derivation (and name construction), a compound unit ID can be represented as a tree structure where the leaves are the atomic units, and the higher level node are one of the above. Here is an extreme example of that: _kilogram-square-kilometer-ampere-candela-per-square-second-mole_


<!-- HTML: colspan -->

<table>
<tbody>
<tr><th colspan="6">per</th></tr>
<tr><th colspan="4">times</th><th colspan="2">times</th></tr>
<tr><th>kilo</th><th>square</th><td>ampere</td><td>candela</td><th>square</th><td>mole</td></tr>
<tr><td>gram</td><th>kilo</th><td>-</td><td>-</td><td>second</td><td></td></tr>
<tr><td>-</td><td>meter</td><td>-</td><td>-</td><td colspan="2">-</td></tr>
</tbody>
</table>

Note that the prefix and power nodes are unary (exactly 1 child), the per pattern is unary or binary (1 or 2 children), and the times pattern is n-ary (where n > 1).

* Each section below is only applicable if the language has more than one value _for units_: for example, for plural categories the language has to have more than just "other". When that information is available for a language, it is found in **[Grammatical Features](#Grammatical_Features)**.


* The gender derivation would be appropriate for an API call like `String genderValue = getGrammaticalGender(locale, "kilogram-meter-per-square-second")`. This can be used where the choice of word forms in the rest of a phrase can depend on the gender of the unit.


* On the other hand, the derivation of plural category and case are used in building up the name of a compound unit, where the desired plural category is available from the number to be formatted with the unit, and the case value is known from the position in a message. For example, the case could be accusative if the formatted unit is to be the direct object in a sentence or phrase. This could be expressed in an API call such as `String inflectedName = getUnitName(locale, "kilogram-meter-per-square-second", pluralCategory, caseValue)`.


* When deriving an inflected compound unit pattern, as the tree-stucture is processed by getting the appropriate localized patterns for the structural components and names for the atomic components. The computation of the plural category and the case of the subtrees can be computed from the **deriveComponent** data. The **times** data is treated as binary, and applied from left to right: with the example from above, the plural categories for the components of _kilogram-square-kilometer-ampere-candela_ are computed by applying


**times**(_kilogram, **times**(square-kilometer, **times**(ampere, candela)))_

For a description of how to use these fields to construct a localized name, see **[Compound Units](#compound-units)**.

### <a name="gender_compound_units" id="gender_compound_units" href="#gender_compound_units">Deriving the Gender of Compound Units</a>

* **The **deriveCompound\[@feature="gender"\]** data**: The **deriveCompound\[@feature="gender"\]** data provides information for how to derive the gender of the whole compound from the gender of its atomic units and structure. The `attributeValues` of value are: **`0` (=gender of the first element), `1` (=gender of second element), or one of the valid gender values for the language.** In the unusual case that the 'per' compound has no first element and 0 is supplied, then the value is 1.


Example:

```xml
<deriveCompound feature="gender" structure="per" value="0" /> <!-- gender(gram-per-meter) ← gender(gram) -->
<deriveCompound feature="gender" structure="times" value="1" /> <!-- gender(newton-meter) ← gender(meter) -->
<deriveCompound feature="gender" structure="power" value="0" /> <!-- gender(square-meter) ← gender(meter) -->
<deriveCompound feature="gender" structure="prefix" value="0" /> <!-- gender(kilometer) ← gender(meter) -->
```

For example, for gram-per-meter, the first line above means:

* The gender of the compound is the gender of the first component of the 'per', that is, of the "gram". So if gram is feminine in that language, the gender of the compound is feminine.


### <a name="plural_compound_units" id="plural_compound_units" href="#plural_compound_units">Deriving the Plural Category of Unit Components</a>

* The `deriveComponent[@feature="plural"]` data provides information for how to derive the plural category for each of the atomic units, from the plural category of the whole compound and the structure of the compound. The `attributeValues` of `value0` and `value1` are: `compound` (=the `pluralCategory` of the compound), or one of the valid plural category values for the language.


Example:

```xml
<deriveComponent feature="plural" structure="per" value0="compound" value1="one" /> <!-- compound(gram-per-meter) ⇒  compound(gram) “per" singular(meter) -->
<deriveComponent feature="plural" structure="times" value0="one"  value1="compound" />  <!-- compound(newton-meter) ⇒  singular(newton) “-" compound(meter) -->
<deriveComponent feature="plural" structure="power" value0="one"  value1="compound" />  <!-- compound(square-meter) ⇒  singular(square) compound(meter) -->
<deriveComponent feature="plural" structure="prefix" value0="one"  value1="compound" /> <!-- compound(kilometer) ⇒  singular(kilo) compound(meter) -->
```

For example, for gram-per-meter, the first line above means:

*   When the plural form of gram-per-meter is needed (rather than singular), then the gram part of the translation has to have a plural form like “grams”, while the meter part of the translation has to have a singular form like “metre”. This would be composed with the pattern for "per" (say "{0} pro {1}") to get "grams pro metre".


### <a name="case_compound_units" id="case_compound_units" href="#case_compound_units">Deriving the Case of Unit Components</a>

* The `deriveComponent[@feature="case"]` data provides information for how to derive the grammatical case for each of the atomic units, from the grammatical case of the whole compound and the structure of the compound. The `attributeValues` of value0 and value1 are: `compound` (=the grammatical case of the compound), or one of the valid grammatical case values for the language.


Example:

```xml
<deriveComponent feature="case" structure="per" value0="compound" value1="nominative" /> <!-- compound(gram-per-meter) ⇒ compound(gram) “per" accusative(meter) -->
<deriveComponent feature="case" structure="times" value0="nominative"  value1="compound" /> <!-- compound(newton-meter) ⇒  nominative(newton) “-" compound(meter) -->
<deriveComponent feature="case" structure="power" value0="nominative"  value1="compound" /> <!-- compound(square-meter) ⇒  nominative(square) compound(meter) -->
<deriveComponent feature="case" structure="prefix" value0="nominative"  value1="compound" /><!--compound(kilometer) ⇒  nominative(kilo) compound(meter) -->
```

For example, for gram-per-meter, the first line above means:

* When the accusative form of gram-per-meter is needed, then the gram part of the translation has the accusative case (eg, “gramu”, in a language that marks the accusative case with 'u'), while the meter part of the translation has a nominative form like “metre”. This would be composed with the pattern for "per" (say "{0} pro {1}") to get "gramu pro metre".

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

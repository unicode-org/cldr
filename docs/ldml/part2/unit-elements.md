## <a name="Unit_Elements" id="Unit_Elements" href="#Unit_Elements">Unit Elements</a>

```dtd
<!ELEMENT units (alias | (unit*, unitLength*, durationUnit*, special*) ) >

<!ELEMENT unitIdComponents ( unitIdComponent* ) >

<!ELEMENT unitLength (alias | (compoundUnit*, unit*, coordinateUnit*, special*) ) >
<!ATTLIST unitLength type (long | short | narrow) #REQUIRED >

<!ELEMENT compoundUnit (alias | (compoundUnitPattern*, special*) ) >
<!ATTLIST compoundUnit type NMTOKEN #REQUIRED >

<!ELEMENT unit ( alias | ( gender*, displayName*, unitPattern*, perUnitPattern*, special* ) ) >
<!ATTLIST unit type NMTOKEN #REQUIRED >

<!ELEMENT gender ( #PCDATA )>

<!ELEMENT durationUnit (alias | (durationUnitPattern*, special*) ) >
<!ATTLIST durationUnit type NMTOKEN #REQUIRED >

<!ELEMENT unitPattern ( #PCDATA ) >
<!ATTLIST unitPattern count (0 | 1 | zero | one | two | few | many | other) #REQUIRED >

<!ELEMENT compoundUnitPattern ( #PCDATA ) >
<!ATTLIST compoundUnitPattern case NMTOKENS #IMPLIED >

<!ELEMENT compoundUnitPattern1 ( #PCDATA ) >
<!ATTLIST compoundUnitPattern1 count (0 | 1 | zero | one | two | few | many | other) #IMPLIED >
<!ATTLIST compoundUnitPattern1 gender NMTOKENS #IMPLIED >
<!ATTL IST compoundUnitPattern1 case NMTOKENS #IMPLIED >

<!ELEMENT coordinateUnit ( alias | ( displayName*, coordinateUnitPattern*, special* ) ) >
<!ELEMENT coordinateUnitPattern ( #PCDATA ) >
<!ATTLIST coordinateUnitPattern type (north | east | south | west) #REQUIRED >

<!ELEMENT durationUnitPattern ( #PCDATA ) >
```

* These elements specify the localized way of formatting quantities of units such as years, months, days, hours, minutes and seconds— for example, in English, "1 day" or "3 days". The English rules that produce this example are as follows ({0} indicates the position of the formatted numeric value):


```xml
<unit type="duration-day">
  <displayName>days</displayName>
  <unitPattern count="one">{0} day</unitName>
  <unitPattern count="other">{0} days</unitName>
</unit>
```

* The German rules are more complicated, because German has both gender and case. They thus have additional information, as illustrated below. Note that if there is no `@case` attribute, for backwards compatibility the implied case is nominative. The possible values for @case are listed in the `grammaticalFeatures` element. These follow the inheritance specified in Part 1, Section Lateral Inheritance](tr35.md#Lateral_Inheritance). Note that the additional grammar elements are only present in the `<unitLength type='long'>` form.


```xml
<unit type="duration-day">
    <gender>masculine</gender>
    <displayName>Tage</displayName>
    <unitPattern count="one">{0} Tag</unitPattern>
    <unitPattern count="one" case="accusative">{0} Tag</unitPattern>
    <unitPattern count="one" case="dative">{0} Tag</unitPattern>
    <unitPattern count="one" case="genitive">{0} Tages</unitPattern>
    <unitPattern count="other">{0} Tage</unitPattern>
    <unitPattern count="other" case="accusative">{0} Tage</unitPattern>
    <unitPattern count="other" case="dative">{0} Tagen</unitPattern>
    <unitPattern count="other" case="genitive">{0} Tage</unitPattern>
    <perUnitPattern>{0} pro Tag</perUnitPattern>
</unit>
```

* These follow the inheritance specified in Part 1, Section Lateral Inheritance](tr35.md#Lateral_Inheritance). In addition to supporting language-specific plural cases such as “one” and “other”, unitPatterns support the language-independent explicit cases “0” and “1” for special handling of numeric values that are exactly 0 or 1; see [Explicit 0 and 1 rules](tr35-numbers.md#Explicit_0_1_rules).


* **The `` elements**: The `<unitPattern>` elements may be used to format quantities with decimal values; in such cases the choice of plural form will depend not only on the numeric value, but also on its formatting (see [Language Plural Rules](tr35-numbers.md#Language_Plural_Rules)). In addition to formatting units for stand-alone use, `<unitPattern>` elements are increasingly being used to format units for use in running text; for such usages, the developing [Grammatical Features](#Grammatical_Features) information will be very useful.


* Note that for certain plural cases, the unit pattern may not provide for inclusion of a numeric value—that is, it may not include “{0}”. This is especially true for the explicit cases “0” and “1” (which may have patterns like “zero seconds”). In certain languages such as Arabic and Hebrew, this may also be true with certain units for the plural cases “zero”, “one”, or “two” (in these languages, such plural cases are only used for the corresponding exact numeric values, so there is no concern about loss of precision without the numeric value).


Units, like other values with a `count` attribute, use a special inheritance. See **Part 1: Core:** _[Multiple Inheritance](tr35.md#Multiple_Inheritance)_.

* The displayName is used for labels, such as in a UI. It is typically lowercased and as neutral a plural form as possible, and then uses the casing context for the proper display. For example, for English in a UI it would appear as titlecase:


**Duration:**

<!-- HTML: UI drawing -->
<table>
<tbody>
<tr><td>Days</td><td style="color: silver;">enter the vacation length</td></tr>
</tbody>
</table>


### <a name="Unit_Preference_and_Conversion" id="Unit_Preference_and_Conversion" href="#Unit_Preference_and_Conversion">Unit Preference and Conversion Data</a>

* Different locales have different preferences for which unit or combination of units is used for a particular usage, such as measuring a person’s height. This is more fine-grained than merely a preference for metric versus US or UK measurement systems. For example, one locale may use meters alone, while another may use centimeters alone or a combination of meters and centimeters; a third may use inches alone, or (informally) a combination of feet and inches.


* The unit preference and conversion data allows formatting functions to pick the right measurement units for the locale and usage, and convert input measurement into those units. For example, a program (or database) could use 1.88 meters internally, but then for person-height have that measurement convert to _6 foot 2 inches_ for en-US and to _188 centimeters_ for de-CH. Using the unit display names and list formats, those results can then be displayed according to the desired width (eg _2″_ vs _2 in_ vs 2 _inches_) and using the locale display names and number formats.


The size of the measurement can also be taken into account, so that an infant can have a height as _18 inches_, and an adult the height as _6 foot 2 inches._

This data is supplied in **Part 6: [Supplemental](tr35-info.md#Contents)**: [Unit Conversion](tr35-info.md#Unit_Conversion) and [Unit Preferences](tr35-info.md#Unit_Preferences).

### <a name="Unit_Identifiers" id="Unit_Identifiers" href="#Unit_Identifiers">Unit Identifiers</a>

Units of measurement, such as _meter_, have defined programmatic identifiers as described in this section.
The main identifier is a _core unit identifier_, which encompasses a number of simpler types of identifiers as follows.
A secondary type of identifier is a _mixed unit identifier_, which combines a series of units such as _5° 30′_ or _3 feet 7 inches_.

| Name             | Examples |
| ---------------- | -------- |
| core unit ID     | kilometer-per-hour, kilogram-meter, kilogram-meter-per-square-second, … <br/> _plus single unit IDs_ |
| single unit ID   | square-foot, cubic-centimeter, … <br/> _plus prefixed unit IDs_ |
| prefixed unit ID | kilometer, centigram, … <br/> _plus simple unit IDs_ |
| simple unit ID   | meter, foot, inch, pound, pound-force, … |
| mixed unit ID    | foot-and-inch, degree-and-arc-minute-and-arc-second |


There is currently a ‘long’ style of unit identifier corresponding to each _core unit identifier_, as illustrated below.
The only difference is that the long unit identifier adds a prefix which was used in the CLDR Survey Tool for grouping related identifiers together.
* The long unit identifers are used as a key in the translated unit names for locales, but dealing with these two styles is unnecessarily complicated, so the long unit identifiers are slated for deprecation (after replacing their use as a key for translations).


| core unit ID | long unit ID |
| ------------ | ------------ |
| meter        | length-meter |
| pound        | mass-pound   |
| day          | duration-day |


The list of valid CLDR simple unit identifiers is found in _[Section Validity Data](tr35.md#Validity_Data)_.
These names should not be presented to end users, however: the translated names for different languages (or variants of English) are available in the CLDR localized data.
All syntactically valid CLDR unit identifiers values that are not listed in the validity data are reserved by CLDR for additional future units.
There is one exception: implementations that need to define their own unit identifiers can do so via _[Private-Use Units](#Private_Use_Units)_.

A core unit identifier that is not a simple unit is called a _complex unit_ (aka _compound unit_).
* A complex unit identifier can be constructed from simple unit identifiers using multiplication (kilogram-meter) and division (kilogram-per-meter), powers (square-second), and prefixes (kilo-, 100-, kiBi).

As usual, with division the part before the (first) -per- is called the _numerator_, and the part after it is called the _denominator_.

The identifiers and unit conversion data are built to handle core unit IDs and mixed unit IDs based on their simple unit identifiers.
Thus they support converting generated units such as inch-pound-per-square-week into comparable units, such as newtons.

* Where a core unit ID or mixed unit ID does not have an explicit translation in CLDR, a mechanism is supplied for producing a generated translation from the translations for the simple unit identifiers.

See _[Compound Units](#compound-units)_.
That can be used for less common units, such as _petasecond_.
However, the generated translations may have the wrong spelling in languages where orthographic changes are needed when combining words.
* For example, “kilometer” can be formed in English from “kilo” and “meter”; the same process in Greek would combine “χιλιο” and “μέτρα” to get “χιλιομέτρα” — when the correct result is “χιλιόμετρα” (note the different location of the accent).

Thus the most commonly-used complex units have explicit translations in CLDR.

* A power (square, cubic, pow4, etc) modifies one prefixed unit ID, and must occur immediately before it in the identifier: square-foot, not foot-square.
* Multiplication binds more tightly than division, so kilogram-meter-per-second-ampere is interpreted as (kg ⋅ m) / (s ⋅ a).
* Thus if -per- occurs multiple times, each occurrence after the first is equivalent to a multiplication:
  * kilogram-meter-per-second-ampere ⩧ kilogram-meter-per-second-per-ampere.

#### <a name="Nomenclature" id="Nomenclature" href="#Nomenclature">Nomenclature</a>

* As with other identifiers in CLDR, the American English spelling is used for unit identifiers. For the US spelling, see the [Preface of the Guide for the Use of the International System of Units (SI), NIST special publication 811](https://www.nist.gov/pml/special-publication-811), which is explicit about the discrepancy with the English-language BIPM spellings:


> In keeping with U.S. and International practice (see Sec. C.2), this Guide uses the dot on the line as the decimal marker. In addition this Guide utilizes the American spellings “meter,” “liter,” and “deka” rather than “metre,” “litre,” and “deca,” and the name “metric ton” rather than “tonne.”

<a name="syntax"></a>
#### <a name="Unit_Syntax" id="Unit_Syntax" href="#Unit_Syntax">Unit Syntax</a>

The formal [EBNF](tr35.md#ebnf) syntax for identifiers is provided below.
Some of the constraints reference data from various elements in the unit conversion data [units.xml](https://github.com/unicode-org/cldr/blob/main/common/supplemental/units.xml).
These may be either element values or element attribute values.
See [Unit_Conversion](tr35-info.md#Unit_Conversion).

<a name='unit_identifier' href='#unit_identifier'>unit_identifier</a>
<br/>:= core_unit_identifier
<br/>   | mixed_unit_identifier
<br/>   | long_unit_identifier

<a name='core_unit_identifier' href='#core_unit_identifier'>core_unit_identifier</a>
<br/>:= product_unit ("-" per "-" product_unit)\*
<br/>   | per "-" product_unit\*
<br/>   | per "-" product_unit ("-" per "-" product_unit)\*   // unnormalized
* *Examples:*

| normalized | unnormalized |
| :---- | :---- |
| foot-per-square-second | foot-per-second-per-second |
| per-meter-second | |
| per-1000 | per-100-10 |
| per-10000-meter-second | per-10-meter-10-second-10 |
* *Notes:*
    * The segment before the first `per` is called the `numerator`; it may be empty
    * The segment after the first `per` is called the `denominator`; it may be empty
    * unit_constants in the numerator are deprecated, and need not be supported in APIs or formatting
        * They may be supported internally, such as for conversion.
    * The normalized form has:
       * at most one `per`
       * at most one unit_constant; and that only immediately after a `per`

per
<br/>:= "per"
* [ wfc: The token 'per' is the single value in \`<unitIdComponent type="per"\>` ]

<a name='product_unit' href='#product_unit'>product_unit</a>
<br/>:= single_unit ("-" single_unit)*
* *Examples:*
    * foot-pound-force

<a name='single_unit' href='#single_unit'>single_unit</a>
<br/>:= dimensionality_prefix? simple_unit
<br/>   | unit_constant
<br/>   | pu_single_unit
* *Examples:*
    * square-kilometer
    * 100

<a name='pu_single_unit' href='#pu_single_unit'>pu_single_unit</a>
<br/>:= ("xxx-" | "x-") [a-z0-9]{3,8}
* *Examples:*
    * square-xxx-knuts (a Harry Potter unit)
* *Notes:*
    * "x-" is only for backwards compatibility; it is deprecated and should not be generated
    * See [Private-Use Units](https://github.com/unicode-org/cldr/edit/main/docs/ldml/tr35-general.md#Private_Use_Units)

<a name='unit_constant' href='#unit_constant'>unit_constant</a>
<br/>:= [1-9][0-9]* ("e" [1-9][0-9]*)?
* *Examples:*
  * kilowatt-hour-per-100-kilometer
  * gallon-per-100-mile
  * per-200-pound
  * per-12
* [ wfc:  The numeric value of the unit constant must be an integer greater than one. ]
* [ wfc:  The string length of the unit constant must be less than 9 characters. ]
* * *Notes:*
    * The normal interpretation of `e` is used, where 2e6 \= 2×10⁶
    * Implementations must support the numbers {1-14, 20, 144, 1eN for N <= 18}
        * They may support additional values, up to what is expressible with 8 characters.
    * The `e` notation is optional: `per-100-kilometer` and `per-1e2-kilometer` are equivalent unit\_identifiers
    * The normalized form has no exponents that are not multiples of 3, and the shortest form given that exponent restriction:
         * per-1e2 ⇒ per-100
         * per-1000 ⇒ per-1e3
         * per-10000 ⇒ per-10e3

<a name='dimensionality_prefix' href='#dimensionality_prefix'>dimensionality_prefix</a>
<br/>:= "square-"
<br/>   | "cubic-"
<br/>   | "pow" ([2-9]|1[0-5]) "-"
* [ wfc:  Must be value in: \`<unitIdComponent type="power"\>`. ]
* *Notes:*
    * "pow2-" and "pow3-" canonicalize to "square-" and "cubic-"

<a name='simple_unit' href='#simple_unit'>simple_unit</a>
<br/>:= (prefix_component "-")* (prefixed_unit
<br/>   | base_component) ("-" suffix_component)*
<br/>   | currency_unit
<br/>   | ("em" | "g" | "us" | "hg" | "of")
* *Examples:*
    * kilometer
    * meter
    * cup-metric
    * fluid-ounce
    * curr-chf
    * em
* *Notes:*
* *** Five simple**: * Five simple units are currently allowed as legacy usage, for tokens that wouldn’t otherwise be a base\_component due to length (eg, "g-force").Those are likely to be deprecated in teh future, with conformant aliases added: the "hg" and "of" are already only in deprecated simple\_units.


<a name='prefixed_unit' href='#prefixed_unit'>prefixed_unit</a>
    prefix base_component
* *Examples:*
    *  kilometer

<a name='prefix' href='#prefix'>prefix</a>
<br/>:= si_prefix
<br/>   | binary_prefix

<a name='si_prefix' href='#si_prefix'>si_prefix</a>
<br/>:= "deka"
<br/>   | "hecto"
<br/>   | "kilo", …
* [ wfc:  Must be an attribute value of the `type` in: \`<unitPrefix type='…' … power10='…'\>` ]
* *Notes:*
    * See also [NIST special publication 811](https://www.nist.gov/pml/special-publication-811)

<a name='binary_prefix' href='#binary_prefix'>binary_prefix</a>
<br/>:= "kibi", "mebi", …
* [ wfc:  Must be an attribute value of the `type` in: \`<unitPrefix type='…' … power2='…'\>`. ]
* *Notes:*
    * See also [Prefixes for binary multiples](https://physics.nist.gov/cuu/Units/binary.html)

<a name='prefix_component' href='#prefix_component'>prefix_component</a>
<br/>:= [a-z]{3,}
* [ vc:  must be value in: \`<unitIdComponent type="prefix"\>`. ]
* *Notes:*
    * The set of prefix components often expands in new releases, so the requirement to be one of these attribute values is a validity constraint, not a well-formedness constraint. *

<a name='base_component' href='#base_component'>base_component</a>
<br/>:= [a-z]{3,}
* [ wfc:  must not have a prefix as an initial segment. ]
* [ wfc:  must not be a value in \`<unitIdComponent type="X"\>` for X in \{prefix, suffix, power, and, per} ]
* [ vc:  Must be an attribute value of the `source` in: \`<convertUnit source='…' …\>` or the `type` in \`<unitAlias type="…" replacement="…" …\>` ]
* *Notes:*
    * The set of base components typically expands in new releases, so the requirement to be one of these attribute values is a validity constraint, not a well-formedness constraint.
    * The base-components in unitAlias `type` are deprecated, should be converted to their replacement values.
* *** No two**: * No two different base\_components will share the first 8 letters; see [Unit Identifier Uniqueness](https://github.com/unicode-org/cldr/edit/main/docs/ldml/tr35-general.md#Unit_Identifier_Uniqueness).) ]


<a name='suffix_component' href='#suffix_component'>suffix_component</a>
<br/>:= [a-z]{3,}
* [ vc:  must be value in: \`<unitIdComponent type="suffix"\>` ]
* *Notes:*
    * The set of suffix components often expands in new releases, so the requirement to be one of these attribute values is a validity constraint, not a well-formedness constraint.

<a name='mixed_unit_identifier' href='#mixed_unit_identifier'>mixed_unit_identifier</a>
<br/>:= single_unit ("-" and "-" single_unit)*
* [ wfc: Each part separated by -and- must be convertible to the others.]
* Note: in the normalized form, each part is smaller than the subsequent one: thus `inch-and-foot` normalizes to `foot-and-inch`.
* *Examples:*
    * foot-and-inch
    * degree-and-arc-minute-and-arc-second

and
<br/>:= "and"
* [ wfc:  The token 'and' is the single value in \`<unitIdComponent type="and"\>` ]

<a name='long_unit_identifier' href='#long_unit_identifier'>long_unit_identifier</a>
<br/>:= grouping "-" core_unit_identifier

grouping
<br/>:= [a-z]{3,}

<a name='currency_unit' href='#currency_unit'>currency_unit</a>
<br/>:= "curr-" [a-z]{3}
* [ wfc:  The first part of the currency\_unit is a standard prefix; the second part of the currency unit must be a valid [Unicode currency identifier](https://github.com/unicode-org/cldr/blob/main/docs/ldml/tr35.md#UnicodeCurrencyIdentifier). ]
* *Examples:*
    * curr-eur-per-square-meter
    * pound-per-curr-usd
* *Notes:*
    * CLDR does not provide conversions for currencies; this is only intended for formatting.
    * The locale data for currency display names is supplied in the `currencies` element, not in the `units` element.

Note that while the syntax allows for unit_constants in multiple places, the typical use case is only one instance, after a "-per-".
The normalized, non-deprecated form of a unit identifier has at most one unit_constant in the denominator immediately after the per.
For example, `kilowatt-hour-per-3-meter-5-second` has the equivalent normalized form `kilowatt-hour-per-15-meter-second`.

The simple_unit structure does not allow for any two simple_units to overlap.
That is, there are no cases where simple_unit1 consists of X-Y and simple_unit2 consists of Y-Z.
This was not true in previous versions of LDML: cup-metric overlapped with metric-ton.
That meant that the unit identifiers for the product_unit of cup and metric-ton and the product_unit of cup-metric and ton were ambiguous.

The constraint that the identifiers can't overlap also means that parsing of multiple-subtag simple units is simpler.
For example:
* When a prefix_component is encountered, one can collect any other prefix-components, then one base_component, then any suffix components, and stop.
* Similarly, when a base_component is encountered, one can collect any suffix components, and stop.
* Encountering a suffix_component in any other circumstance is an error.

### <a name="Unit_Identifier_Uniqueness" id="Unit_Identifier_Uniqueness" href="#Unit_Identifier_Uniqueness">Unit Identifier Uniqueness</a>
CLDR Unit Identifiers can be used as values in locale identifiers. When that is done, the syntax is modified whenever a `prefixed_unit` would be longer than 8 characters. In such a case:

* If there is no `prefix` the `prefixed_unit` is truncated to 8 characters.
* If there is a `prefix`, a hyphen is added between the `prefix` and the `base_component`. If that `base_component` is longer than 8 characters, it is truncated to 8 characters.

_Example_
| Unit identifer | BCP47 syntax example | Comment |
| ----      | ----               | ----                           |
| kilogram  | en-u-ux-kilogram   | kilogram fits in 8 characters  |
| centilux  | en-u-ux-centilux   | centilux fixs in 8 characters  |
| steradian | en-u-ux-steradia   | steradian exceeds 8 characters |
| centigram | en-u-ux-centi-gram | centigram exceeds 8 characters |
| kilometer | en-u-ux-kilo-meter | kilometer exceeds 8 characters |
| quectolux | en-u-ux-kilo-meter | kilometer exceeds 8 characters |

This requires that each of the elements in base_components are unique to eight letters, that is: **no two different base_components will share the first 8 letters**.

* The reason that the `prefixed_unit` as a whole is not simply truncated to 8 characters is that would impose too strict a constraint. There  are 5 letter prefixes such as 'centi' and more recently 6 letter prefixes such as 'quecto'. That would cause prefixed `base_component` as short as 'gram' and 'gray' to be ambiguous when truncated to 8 letters: 'centigra'; and 'lumen' and 'lux' would fail with the 6 letter prefixes.


### <a name="Example_Units" id="Example_Units" href="#Example_Units">Example Units</a>

The following table contains examples of groupings and units currently defined by CLDR.
The units in CLDR are not comprehensive; it is anticipated that more will be added over time.
The complete list of supported units is in the validity data: see _[Section Validity Data](tr35.md#Validity_Data)_.

| Type           | Core Unit Identifier     | Compound? | Sample Format  |
| -------------- | ------------------------ | --------- | -------------- |
| _acceleration_ | g-force                  | simple    | {0} G          |
| _acceleration_ | meter-per-square-second  | compound  | {0} m/s²       |
| _angle_        | revolution               | simple    | {0} rev        |
| _angle_        | radian                   | simple    | {0} rad        |
| _angle_        | degree                   | simple    | {0}°           |
| _angle_        | arc-minute               | simple    | {0}′           |
| _angle_        | arc-second               | simple    | {0}″           |
| _area_         | square-kilometer         | simple    | {0} km²        |
| _area_         | hectare                  | simple    | {0} ha         |
| ...            | ...                      | ...       | ...            |
| _area_         | square-inch              | simple    | {0} in²        |
| _area_         | dunam                    | simple    | {0} dunam      |
| _concentr_     | karat                    | simple    | {0} kt         | dimensionless |
| _concentr_     | milligram-per-deciliter  | compound  | {0} mg/dL      |
| _concentr_     | millimole-per-liter      | compound  | {0} mmol/L     |
| _concentr_     | permillion               | compound  | {0} ppm        | dimensionless |
| _concentr_     | percent                  | simple    | {0}%           | dimensionless |
| _concentr_     | permille                 | simple    | {0}‰           | dimensionless |
| _concentr_     | permyriad                | simple    | {0}‱          | dimensionless |
| _concentr_     | mole                     | simple    | {0} mol        | dimensionless |
| _consumption_  | liter-per-kilometer      | compound  | {0} L/km       |
| _consumption_  | liter-per-100-kilometer  | compound  | {0} L/100km    |
| _consumption_  | mile-per-gallon (US)     | compound  | {0} mpg        |
| _consumption_  | mile-per-gallon-imperial | compound  | {0} mpg Imp.   |
| _digital_      | petabyte                 | simple    | {0} PB         |
| ...            | ...                      | ...       | ...            |
| _digital_      | byte                     | simple    | {0} byte       |
| _digital_      | bit                      | simple    | {0} bit        |
| _duration_     | century                  | simple    | {0} c          |
| _duration_     | year                     | simple    | {0} y          |
| _duration_     | year-person              | simple    | {0} y          | for duration or age related to a person |
| _duration_     | month                    | simple    | {0} m          |
| _duration_     | month-person             | simple    | {0} m          | for duration or age related to a person |
| _duration_     | week                     | simple    | {0} w          |
| _duration_     | week-person              | simple    | {0} w          | for duration or age related to a person |
| _duration_     | day                      | simple    | {0} d          |
| _duration_     | day-person               | simple    | {0} d          | for duration or age related to a person |
| _duration_     | hour                     | simple    | {0} h          |
| ...            | ...                      | ...       | ...            |
| _duration_     | nanosecond               | simple    | {0} ns         |
| _electric_     | ampere                   | simple    | {0} A          |
| _electric_     | milliampere              | simple    | {0} mA         |
| _electric_     | ohm                      | simple    | {0} Ω          |
| _electric_     | volt                     | simple    | {0} V          |
| _energy_       | kilocalorie              | simple    | {0} kcal       |
| _energy_       | calorie                  | simple    | {0} cal        |
| _energy_       | foodcalorie              | simple    | {0} Cal        |
| _energy_       | kilojoule                | simple    | {0} kJ         |
| _energy_       | joule                    | simple    | {0} J          |
| _energy_       | kilowatt-hour            | simple    | {0} kWh        |
| _energy_       | electronvolt             | simple    | {0} eV         |
| _energy_       | british-thermal-unit     | simple    | {0} Btu        |
| _force_        | pound-force              | simple    | {0} lbf        |
| _force_        | newton                   | simple    | {0} N          |
| _frequency_    | gigahertz                | simple    | {0} GHz        |
| _frequency_    | megahertz                | simple    | {0} MHz        |
| _frequency_    | kilohertz                | simple    | {0} kHz        |
| _frequency_    | hertz                    | simple    | {0} Hz         |
| _length_       | kilometer                | simple    | {0} km         |
| ...            | ...                      | ...       | ...            |
| _length_       | inch                     | simple    | {0} in         |
| _length_       | parsec                   | simple    | {0} pc         |
| _length_       | light-year               | simple    | {0} ly         |
| _length_       | astronomical-unit        | simple    | {0} au         |
| _length_       | furlong                  | simple    | {0} fur        |
| _length_       | fathom                   | simple    | {0} fm         |
| _length_       | nautical-mile            | simple    | {0} nmi        |
| _length_       | mile-scandinavian        | simple    | {0} smi        |
| _length_       | point                    | simple    | {0} pt         | typographic point, 1/72 inch |
| _length_       | solar-radius             | simple    | {0} R☉        |
| _light_        | lux                      | simple    | {0} lx         |
| _light_        | solar-luminosity         | simple    | {0} L☉        |
| _mass_         | metric-ton               | simple    | {0} t          |
| _mass_         | kilogram                 | simple    | {0} kg         |
| ...            | ...                      | ...       | ...            |
| _mass_         | ounce                    | simple    | {0} oz         |
| _mass_         | ounce-troy               | simple    | {0} oz t       |
| _mass_         | carat                    | simple    | {0} CD         |
| _mass_         | dalton                   | simple    | {0} Da         |
| _mass_         | earth-mass               | simple    | {0} M⊕         |
| _mass_         | solar-mass               | simple    | {0} M☉        |
| _power_        | gigawatt                 | simple    | {0} GW         |
| ...            | ...                      | ...       | ...            |
| _power_        | milliwatt                | simple    | {0} mW         |
| _power_        | horsepower               | simple    | {0} hp         |
| _pressure_     | hectopascal              | simple    | {0} hPa        |
| _pressure_     | millimeter-ofhg          | simple    | {0} mm Hg      |
| _pressure_     | pound-force-per-square-inch | compound | {0} psi      |
| _pressure_     | inch-ofhg                | simple    | {0} inHg       |
| _pressure_     | millibar                 | simple    | {0} mbar       |
| _pressure_     | atmosphere               | simple    | {0} atm        |
| _pressure_     | kilopascal               | simple    | {0} kPa        |
| _pressure_     | megapascal               | simple    | {0} MPa        |
| _speed_        | kilometer-per-hour       | compound  | {0} km/h       |
| _speed_        | meter-per-second         | compound  | {0} m/s        |
| _speed_        | mile-per-hour            | compound  | {0} mi/h       |
| _speed_        | knot                     | simple    | {0} kn         |
| _temperature_  | generic                  | simple    | {0}°           |
| _temperature_  | celsius                  | simple    | {0}°C          |
| _temperature_  | fahrenheit               | simple    | {0}°F          |
| _temperature_  | kelvin                   | simple    | {0} K          |
| _torque_       | pound-force-foot         | simple    | {0} lbf⋅ft     |
| _torque_       | newton-meter             | simple    | {0} N⋅m        |
| _volume_       | cubic-kilometer          | simple    | {0} km³        |
| ...            | ...                      | ...       | ...            |
| _volume_       | cubic-inch               | simple    | {0} in³        |
| _volume_       | megaliter                | simple    | {0} ML         |
| ...            | ...                      | ...       | ...            |
| _volume_       | pint                     | simple    | {0} pt         |
| _volume_       | cup                      | simple    | {0} c          |
| _volume_       | fluid-ounce (US)         | simple    | {0} fl oz      |
| _volume_       | fluid-ounce-imperial     | simple    | {0} fl oz Imp. |
| _volume_       | tablespoon               | simple    | {0} tbsp       |
| _volume_       | teaspoon                 | simple    | {0} tsp        |
| _volume_       | barrel                   | simple    | {0} bbl        |

* There are three widths: **long**, **short**, and **narrow**. As usual, the narrow forms may not be unique: in English, 1′ could mean 1 minute of arc, or 1 foot. Thus narrow forms should only be used where the context makes the meaning clear.


* Where the unit of measurement is one of the [International System of Units (SI)](https://physics.nist.gov/cuu/Units/units.html), the short and narrow forms will typically use the international symbols, such as “mm” for millimeter. They may, however, be different if that is customary for the language or locale. For example, in Russian it may be more typical to see the Cyrillic characters “мм”.


* Units are sometimes included for translation even where they are not typically used in a particular locale, such as kilometers in the US, or inches in Germany. This is to account for use by travelers and specialized domains, such as the German “Fernseher von 32 bis 55 Zoll (80 bis 140 cm)” for TV screen size in inches and centimeters.


For temperature, there is a special unit `<unit type="temperature-generic">`, which is used when it is clear from context whether Celcius or Fahrenheit is implied.

* For duration, there are special units such as `<unit type="duration-year-person">` and `<unit type="duration-year-week">` for indicating the age of a person, which requires special forms in some languages. For example, in "zh", references to a person being 3 days old or 30 years old would use the forms “他3天大” and “他30岁” respectively.


<a name="compoundUnitPattern"></a><a name="perUnitPatterns"></a>

### <a name="Compound_Units" id="Compound_Units" href="#Compound_Units">Compound Units</a>

A common combination of units is X per Y, such as _miles per hour_ or _liters per second_ or _kilowatt-hours_.

There are different types of structure used to build the localized name of compound units. All of these follow the inheritance specified in [Part 1, Lateral Inheritance](tr35.md#Lateral_Inheritance).

**Prefixes** are for powers of 10 and powers of 1024 (the latter only used with digital units of measure). These are invariant for case, gender, or plural (though those could be added in the future if needed by a language).

```xml
<compoundUnit type="10p9">
  <unitPrefixPattern>Giga{0}</unitPrefixPattern>
</compoundUnit>

<compoundUnit type="1024p3">
  <unitPrefixPattern>Gibi{0}</unitPrefixPattern>
</compoundUnit>
```

**number prefixes** are integers within a single_unit, such as in liter-per-**100-kilometer**. The formatting for these uses the normal number formats for the locale. Their presence does have an effect on the plural formatting of the simple unit in a "per" form. For example, in English you would write 3 liters per kilometer (singular "kilometer") but 3 liters per 100 kilometers (plural kilometers).

**compoundUnitPatterns** are used for compounding units by multiplication or division: kilowatt-hours, or meters per second. These are invariant for case, gender, or plural (though those could be added in the future if needed by a language).

```xml
<compoundUnit type="per">
  <compoundUnitPattern>{0} pro {1}</compoundUnitPattern>
</compoundUnit>

<compoundUnit type="times">
  <compoundUnitPattern>{0}⋅{1}</compoundUnitPattern>
</compoundUnit>
```

There can be at most one "per" pattern used in producing a compound unit, while the "times" pattern can be used multiple times.

* **compoundUnitPattern1`s are used**: `compoundUnitPattern1`s are used for expressing powers, such as square meter or cubic foot. These are the most complicated, since they can vary by plural category (count), by case, and by gender. However, these extra attributes are only used if they are present in the `grammaticalFeatures` element for the language in question. See [Grammatical Features](#Grammatical_Features). Note that the additional grammar elements are only present in the `<unitLength type='long'>` form.


```xml
<compoundUnit type="power2">
  <compoundUnitPattern1>{0} kw.</compoundUnitPattern1>
  <compoundUnitPattern1 count="one">{0} kwadratowe</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" case="accusative">{0} kwadratowe</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" case="dative">{0} kwadratowemu</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" case="genitive">{0} kwadratowego</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" case="instrumental">{0} kwadratowym</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" case="locative">{0} kwadratowym</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" case="vocative">{0} kwadratowe</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" gender="feminine">{0} kwadratowa</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" gender="feminine" case="accusative">{0} kwadratową</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" gender="feminine" case="dative">{0} kwadratowej</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" gender="feminine" case="genitive">{0} kwadratowej</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" gender="feminine" case="instrumental">{0} kwadratową</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" gender="feminine" case="locative">{0} kwadratowej</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" gender="feminine" case="vocative">{0} kwadratowa</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" gender="inanimate">{0} kwadratowy</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" gender="inanimate" case="accusative">{0} kwadratowy</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" gender="inanimate" case="dative">{0} kwadratowemu</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" gender="inanimate" case="genitive">{0} kwadratowego</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" gender="inanimate" case="instrumental">{0} kwadratowym</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" gender="inanimate" case="locative">{0} kwadratowym</compoundUnitPattern1>
  <compoundUnitPattern1 count="one" gender="inanimate" case="vocative">{0} kwadratowy</compoundUnitPattern1>
  <compoundUnitPattern1 count="few">{0} kwadratowe</compoundUnitPattern1>
  <compoundUnitPattern1 count="few" case="accusative">{0} kwadratowe</compoundUnitPattern1>
  <compoundUnitPattern1 count="few" case="dative">{0} kwadratowym</compoundUnitPattern1>
  …
```

**format(numericValue, unitId, locale, length, caseVariant)**

format(numericValue, unitPattern) substitutes the numericValue (formatted for the locale) into the unitPattern.

Some unitIds already have patterns for the locale, including variants for length, pluralCategory, and caseVariant.
This includes simple units such as **meter** and more complex units like **kilometer-per-hour**.
Where such patterns exist, they should be used in preference (using fallbacks for caseVariant and length if needed).

If there is no precomputed form, the following process in pseudocode is used to generate a pattern for the compound unit.

**pattern(unitId, locale, length, pluralCategory, caseVariant)**

1.  If the unitId is empty or invalid, fail
2.  Put the unitId into normalized format, including order:
    * hour-kilowatt ⇒ kilowatt-hour
    * meter-square-meter-per-second-second ⇒ cubic-meter-per-square-second
    * per-10-meter-10-second-10 ⇒ per-10000-meter-second
4.  Set result to be getValue(unitId with length, pluralCategory, caseVariant)
    1. If result is not empty, return it
5.  Divide the unitId into numerator (the part before the "-per-") and denominator (the part after the "-per-). If both are empty, fail
6.  Set both globalPlaceholder and globalPlaceholderPosition to be empty
7.  Set numeratorUnitString to patternTimes(numerator, length, per0(pluralCategory), per0(caseVariant))
8.  If the denominator starts with a unit_constant
    *  Set denominatorUnitString to format(unitConstant, pattern(denominator, length, getPluralCategory(locale, unitConstant), per1(caseVariant))
    *  Otherwise set denominatorUnitString to patternTimes(denominator, length, per1(getPluralCategory(locale, 1)), per1(caseVariant))
10.  Set perPattern to be getValue(per, locale, length)
11.  If the denominatorString is empty, set result to numeratorString, otherwise set result to format(perPattern, numeratorUnitString, denominatorUnitString)
12. return format(result, globalPlaceholder, globalPlaceholderPosition)

**getPluralCategory(locale, constant)**

1. Return the pluralCategory for the constant, given the locale.

**patternTimes(product_unit, locale, length, pluralCategory, caseVariant)**

1. Set hasMultiple to true iff product_unit has more than one single_unit
2. Set timesPattern to be getValue(times, locale, length)
3. Set result to be empty
4. Set multiplier to be empty
4. For each single_unit in product_unit
   1.  If hasMultiple
       1. Set singlePluralCategory to be times0(pluralCategory)
       2. Set singleCaseVariant to be times0(caseVariant)
       3. Set pluralCategory to be times1(pluralCategory)
       4. Set caseVariant to be times1(caseVariant)
   2.  If the singleUnit is a currency_unit
       1. Set coreUnit to be the formatted currency according to the pluralCategory
	   2. Set the gender to the default unit gender for the locale
	   3. Goto step 11
   2.  Get the gender of that single_unit
   3.  If singleUnit starts with a dimensionality_prefix, such as 'square-'
       1. set dimensionalityPrefixPattern to be getValue(that dimensionality_prefix, locale, length, singlePluralCategory, singleCaseVariant, gender), such as "{0} kwadratowym"
       2. set singlePluralCategory to be power0(singlePluralCategory)
       3. set singleCaseVariant to be power0(singleCaseVariant)
       4. remove the dimensionality_prefix from singleUnit
   4.  if singleUnit starts with an si_prefix, such as 'centi' and/or a unit_constant such as '100'
       1. set siPrefixPattern to be getValue(that si_prefix, locale, length), such as "centy{0}"
       2. set singlePluralCategory to be prefix0(singlePluralCategory)
       3. set singleCaseVariant to be prefix0(singleCaseVariant)
       4. remove the si_prefix from singleUnit
	   5. set multiplier to be the locales integer numberFormat of unit_constant.
   5.  Set corePattern to be the getValue(singleUnit, locale, length, singlePluralCategory, singleCaseVariant), such as "{0} metrem"
   6.  Extract(corePattern, coreUnit, placeholder, placeholderPosition) from that pattern.
   7.  If the position is _middle_, then fail
   8.  If globalPlaceholder is empty
       1. Set globalPlaceholder to placeholder
       2. Set globalPlaceholderPosition to placeholderPosition
   9.  If siPrefixPattern is not empty
       1. Set coreUnit to be the combineLowercasing(locale, length, siPrefixPattern, coreUnit)
   10. If dimensionalityPrefixPattern is not empty
       1. Set coreUnit to be the combineLowercasing(locale, length, dimensionalityPrefixPattern, coreUnit)
   10. If multiplier is not empty
       1. Combine the multiplier with coreUnit, using placeholder and placeholderPosition
   11. If the result is empty, set result to be coreUnit
   12. Otherwise set result to be format(timesPattern, result, coreUnit)
5. Return result

__Note: CLDR does not currently have gender or case data for currency units, so the formatting will not be optimal for inflected languages.__

**combineLowercasing(locale, length, prefixPattern, coreUnit)**

1. If the length is "long" and the prefixPattern contains no spaces, lowercase the coreUnit according to the locale, thus "Quadrat{0}" causes "Zentimeter" to become "zentimeter"
2. return format(prefixPattern, unitPattern), eg "Quadratzentimeter"

**format(pattern, arguments…)**

1. return the result of substituting the arguments for the placeholders {0}, {1}, etc.

**getValue(key, locale, length, variants…)**

1. return the element value in the locale for the path corresponding to the key, locale, length, and variants — using normal inheritance including [Lateral Inheritance](tr35.md#Multiple_Inheritance) and [Parent Locales](tr35.md#Parent_Locales).

**Extract(corePattern, coreUnit, placeholder, placeholderPosition)**

1. Find the position of the **placeholder** in the core pattern
2. Set **placeholderPosition** to that position (start, middle, or end)
3. Remove the **placeholder** from the **corePattern** and set **coreUnit** to that result

**per0(...), times0(...), etc.**

1. These represent the **deriveComponent** data values from **[Grammatical Derivations](#Grammatical_Derivations)**, where value0 of the per-structure is given as per0(...), and so on.
2. "power" corresponds to dimensionality_prefix, while "prefix" corresponds to si_prefix.

* If the locale does not provide full modern coverage, the process could fall back to root locale for some localized patterns. That may give a "ransom-note" effect for the user. To avoid that, it may be preferable to abort the process at that point, and then localize the unitId for the root locale.


If a unit is not supported by root, then the localization is not supported by CLDR and will fail.

#### <a name="Precomposed_Compound_Units" id="Precomposed_Compound_Units" href="#Precomposed_Compound_Units">Precomposed Compound Units</a>

* At each point in the process, if there is a precomposed form for a segment of the unitId, then that precomposed form should be used instead. For example, if there is a pattern in the locale for (square-kilometer, length, singlePluralCategory, singleCaseVariant, gender), then it should be used instead of composing the name from "square" and "kilometer".


* There is also a precomposed **perUnitPattern** which is used as the denominator with another unit name. For example, a form such as "{0} per second" can be used to form "2 feet **per second**". The difference between these is that in some inflected languages, the compoundUnit cannot be used to form grammatical phrases. This is typically because the "per" + "second" combine in a non-trivial way. The `perUnitPattern` should be applied if the denominator has only one element, and matches the `perUnitPattern` type.


### <a name="Unit_Sequences" id="Unit_Sequences" href="#Unit_Sequences">Unit Sequences (Mixed Units)</a>

* Units may be used in composed sequences (aka _mixed units_), such as **5° 30′** for 5 degrees 30 minutes, or **3 ft 2 in.** For that purpose, the appropriate width of the unit `listPattern` can be used to compose the units in a sequence.


```xml
<listPattern type="unit"> (for the long form)
<listPattern type="unit-narrow">
<listPattern type="unit-short">
```

In such a sequence, decimal fractions are typically only displayed for the last element of the sequence, if at all.

### <a name="durationUnit" id="durationUnit" href="#durationUnit">durationUnit</a>

The durationUnit is a special type of unit used for composed time unit durations.

```xml
<durationUnit type="hms">
  <durationUnitPattern>h:mm:ss</durationUnitPattern> <!-- 33:04:59 -->
</durationUnit>
```

* The type contains a skeleton, where 'h' stands for hours, 'm' for minutes, and 's' for seconds. These are the same symbols used in availableFormats, except that there is no need to distinguish different forms of the hour.


### <a name="coordinateUnit" id="coordinateUnit" href="#coordinateUnit">coordinateUnit</a>

* **The **coordinateUnitPattern** is**: The **coordinateUnitPattern** is a special type of pattern used for composing degrees of latitude and longitude, with an indicator of the quadrant. There are exactly 4 type values, plus a displayName for the items in this category. An angle is composed using the appropriate combination of the **angle-degrees**, **angle-arc-minute** and **angle-arc-second** values. It is then substituted for the placeholder field {0} in the appropriate **coordinateUnit** pattern.


```xml
<displayName>direction</displayName>
<coordinateUnitPattern type="east">{0}E</coordinateUnitPattern>
<coordinateUnitPattern type="north">{0}N</coordinateUnitPattern>
<coordinateUnitPattern type="south">{0}S</coordinateUnitPattern>
<coordinateUnitPattern type="west">{0}W</coordinateUnitPattern>
```

### <a name="Territory_Based_Unit_Preferences" id="Territory_Based_Unit_Preferences" href="#Territory_Based_Unit_Preferences">Territory-Based Unit Preferences</a>

* Different locales have different preferences for which unit or combination of units is used for a particular usage, such as measuring a person’s height. This is more fine-grained than merely a preference for metric versus US or UK measurement systems. For example, one locale may use meters alone, while another may use centimeters alone or a combination of meters and centimeters; a third may use inches alone, or (informally) a combination of feet and inches.


* **The `` element**: The `<unitPreferenceData>` element, described in [Preferred Units for Specific Usages](tr35-info.md#Preferred_Units_For_Usage), provides information on which unit or combination of units is used for various purposes in different locales, with options for the level of formality and the scale of the measurement (e.g. measuring the height of an adult versus that of an infant).


### <a name="Private_Use_Units" id="Private_Use_Units" href="#Private_Use_Units">Private-Use Units</a>

* CLDR has reserved the "xxx-" prefix in the simple_unit part of the unit identifier BNF for private-use units. CLDR will never define a type, simple unit, or compound unit such that the unit identifier starts with "xxx-", ends with "-xxx", or contains "-xxx-".


For example, if you wanted to define your own unit "foo", you could use the simple unit "xxx-foo".

* It is valid to construct compound units containing one or more private-use simple units. For example, "xxx-foo-per-second" and "xxx-foo-per-xxx-bar" are both valid core unit identifiers for compound units.


* As explained earlier, CLDR defines all associations between types and units. It is therefore not possible to construct a valid long unit identifier containing a private-use unit; only core unit identifiers are possible.


The older syntax used “x-”, which was expanded to “xxx-” to simplify use with BCP47 syntax. That should be converted to “xxx-”.


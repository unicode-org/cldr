## <a name="Unit_Conversion" id="Unit_Conversion" href="#Unit_Conversion">Unit Conversion</a>

* The unit conversion data ([units.xml](https://github.com/unicode-org/cldr/blob/main/common/supplemental/units.xml)) provides the data for converting all of the cldr unit identifiers to base units, and back. That allows conversion between any two convertible units, such as two units of length. For any two convertible units (such as acre and dunum) the first can be converted to the base unit (square-meter), then that base unit can be converted to the second unit.


### <a name="Unit_Parsing_Data" id="Unit_Parsing_Data" href="#Unit_Parsing_Data">Unit Parsing Data</a>

```dtd
<!ELEMENT unitIdComponents ( unitIdComponent* ) >

<!ELEMENT unitIdComponent EMPTY >
<!ATTLIST unitIdComponent type NMTOKEN #REQUIRED >
<!ATTLIST unitIdComponent values NMTOKENS #REQUIRED >
```

These elements provide support for parsing unit identifiers, as described in [Unit Elements](tr35-general.md#Unit_Elements).
Each of the values has tokens with specific functions, identified by the type.
For example the following values can be suffixes in a simple_unit identifier such as `quart-imperial`.

```xml
<unitIdComponent type="suffix" values="force imperial luminosity mass metric person radius scandinavian troy unit us"/>
```

### <a name="Unit_Prefixes" id="Unit_Prefixes" href="#Unit_Prefixes">Unit Prefixes</a>
```dtd
<!ELEMENT unitPrefixes ( unitPrefix* ) >

<!ELEMENT unitPrefix EMPTY >
<!ATTLIST unitPrefix type NMTOKEN #REQUIRED >
<!ATTLIST unitPrefix symbol NMTOKEN #REQUIRED >
<!ATTLIST unitPrefix power10 NMTOKEN #IMPLIED >
<!ATTLIST unitPrefix power2 NMTOKEN #IMPLIED >
```

This data lists the SI prefixes that can be applied to units (typically limited to prefixable units),
such as the following:
```xml
<unitPrefixes>
	<unitPrefix type='quecto' symbol='q' power10='-30'/>
...
	<unitPrefix type='micro' symbol='μ' power10='-6'/>
...
	<unitPrefix type='giga' symbol='G' power10='9'/>
...
	<unitPrefix type='quetta' symbol='Q' power10='30'/>
	<unitPrefix type='kibi' symbol='Ki' power2='10'/>
...
	<unitPrefix type='yobi' symbol='Yi' power2='80'/>
</unitPrefixes>
```
The information includes the SI prefix and symbol, and the power of 10 or power of 2
(for binary prefixes, intended for use with digital units).

Note that the translated short form of a unit prefix is not the same as the localized symbol.
The localized symbol may be the same for most Latin-script languages,
but depending on the customary use in a language they can be in a different script
or use different letters even in Latin-script languages. They are, however, the same in the root locale.

The newer prefixes (quecto-, ronto-, -ronna, -quetta) are not yet being translated,
because the appropriate translated versions have not yet been well established across languages.

### <a name="Constants" id="Constants" href="#Constants">Constants</a>


```dtd
<!ELEMENT unitConstants ( unitConstant* ) >

<!ELEMENT unitConstant EMPTY >
<!ATTLIST unitConstant constant NMTOKEN #REQUIRED >
<!ATTLIST unitConstant value CDATA #REQUIRED >
<!ATTLIST unitConstant status NMTOKEN #IMPLIED >
<!ATTLIST unitConstant description CDATA #IMPLIED >
```

Many of the elements allow for a common @description attribute, to disambiguate the main attribute value or to explain the choice of other values. For example:
```xml
<unitConstant constant="glucose_molar_mass" value="180.1557"
  description="derivation from the mean atomic weights according to STANDARD ATOMIC WEIGHTS 2019 on https://ciaaw.org/atomic-weights.htm"/>
```

The data uses a small set of constants for readability, such as:

```xml
<unitConstant constant="ft_to_m" value="0.3048" />
<unitConstant constant="ft2_to_m2" value="ft_to_m*ft_to_m" />
```
The order of the elements in the file is significant.

* Each constant can have a value based on simple expressions using numbers, previous constants, plus the operators * and /. Parentheses are not allowed. The operator * binds more tightly than /, which may be unexpected. Thus a * b / c * d is interpreted as (a * b) / (c * d). A consequence of that is that a * b / c * d = a * b / c / d. In the value, the numbers represent rational values. So 0.3048 is interpreted as exactly 3048 / 10000.


* In the above case, ft2-to-m2 is a conversion constant for going from square feet to square meters. The expression evaluates to 0.09290304. Where the constants cannot be expressed as rationals, or where their interpretation is fluid, that is marked with a status value:


```xml
<unitConstant constant="PI" value="411557987 / 131002976" status='approximate' />
```

In such cases, software may decide to use different values for accuracy.

An implementation need not use rationals directly for conversion; it could use doubles, for example, if only double accuracy is needed.

### <a name="Conversion_Data" id="Conversion_Data" href="#Conversion_Data">Conversion Data</a>

```dtd
<!ELEMENT convertUnits ( convertUnit* ) >

<!ELEMENT convertUnit EMPTY >

<!ATTLIST convertUnit source NMTOKEN #REQUIRED >

<!ATTLIST convertUnit baseUnit NMTOKEN #REQUIRED >

<!ATTLIST convertUnit factor CDATA #IMPLIED >

<!ATTLIST convertUnit offset CDATA #IMPLIED >

<!ATTLIST convertUnit special NMTOKEN #IMPLIED >

<!ATTLIST convertUnit systems NMTOKENS #IMPLIED >

<!ATTLIST convertUnit description CDATA #IMPLIED >
```

* The conversion data provides the data for converting all of the cldr unit identifiers to base units, and back. That allows conversion between any two convertible units, such as two units of length. For any two convertible units (such as acre and dunum) the first can be converted to the base unit (square-meter), then that base unit can be converted to the second unit.


The data is expressed as conversions to the base unit from the source unit. The information can also be used for the conversion back.

Examples:

```xml
<convertUnit source='carat' baseUnit='kilogram' factor='0.0002'/>

<convertUnit source='gram' baseUnit='kilogram' factor='0.001'/>

<convertUnit source='ounce' baseUnit='kilogram' factor='lb_to_kg/16' systems="ussystem uksystem"/>

<convertUnit source='fahrenheit' baseUnit='kelvin' factor='5/9' offset='2298.35/9' systems="ussystem uksystem"/>
```

* For example, to convert from 3 carats to kilograms, the factor 0.0002 is used, resulting in 0.0006. To convert between carats and ounces, first the carets are converted to kilograms, then the kilograms to ounces (by reversing the mapping).


The factor and offset use the same structure as in the value in unitConstant; in particular, * binds more tightly than /.

The conversion may also require an offset, such as the following:

```xml
<convertUnit source='fahrenheit' baseUnit='kelvin' factor='5/9' offset='2298.35/9' systems="ussystem uksystem"/>
```

The factor and offset can be simple expressions, just like the values in the unitConstants.

Where a factor is not present, the value is 1; where an offset is not present, the value is 0.

* Instead of using `factor` and possibly `offset`, the `convertUnit` element can specify a `special` conversion that cannot be described by factor and offset (and this attribute cannot be used in conunction with factor and offset). For example:


```xml
<convertUnit source='beaufort' baseUnit='meter-per-second' special='beaufort' systems="metric_adjacent"/>
```

The only `special` conversion currently supported is for beaufort.

* The `systems` attribute indicates the measurement system(s) or other characteristics of a set of unts. Multiple values may be given; for example, a unit could be marked as systems="`si_acceptable` `metric_adjacent` `prefixable`".


The allowed attributes are the following:

Attribute Value   | Description
------------      | -------------
* **si` | The**: `si`              | The _International System of Units (SI)_ See [NIST Guide to the SI, Chapter 4: The Two Classes of SI Units and the SI Prefixes](https://www.nist.gov/pml/special-publication-811/nist-guide-si-chapter-4-two-classes-si-units-and-si-prefixes). Examples: meter, ampere.

* **si_acceptable` | Units**: `si_acceptable`   | Units acceptable for use with the SI. See [NIST Guide to the SI, Chapter 5: Units Outside the SI](https://www.nist.gov/pml/special-publication-811/nist-guide-si-chapter-5-units-outside-si). Examples: hour, liter, knot, hectare.

`metric`          | A superset of the _si_ units
`metric_adjacent` | Units commonly accepted in some countries that follow the metric system. Examples: month, arc-second, pound-metric (= ½ kilogram), mile-scandinavian.
`ussystem`        | The inch-pound system as used in the US, also called _US Customary Units_.
`uksystem`        | The inch-pound system as used in the UK, also called _British Imperial Units_, differing mostly in units of volume
`jpsystem`        | Traditional units used in Japan. For examples, see [Japanese units of measurement](https://en.wikipedia.org/wiki/Japanese_units_of_measurement).
`astronomical`    | Additional units used in astronomy. Examples: parsec, light-year, earth-mass
`person_age`      | Special units used for people’s ages in some languages. Except for translation, they have the same system as the associated regular units.
* **currency` | Currency**: `currency`        | Currency units. These are constructed algorithmically from the Unicode currency identifiers, and do not occur in the child elements of `convertUnits`. Examples: curr-usd (US dollar), curr-eur (Euro).

* **prefixable` | Those**: `prefixable`      | Those units that typically use SI prefixes or the [IEC binary prefixes](https://www.nist.gov/pml/special-publication-811/nist-guide-si-appendix-d-bibliography#05). This can include measures like `parsec` that are not SI units. It allows implementations to group those units together, and to do sanity checks on the prefix+unit combinations, if they choose. However, implementations may choose to allow prefixes on other units, especially since there is a significant variance in usage: even a term like `megafoot` might be acceptable in some contexts.


Over time, additional systems may be added, and the systems for a particular unit may be refined.

#### <a name="Derived_Unit_System" id="Derived_Unit_System" href="#Derived_Unit_System">Derived Unit System</a>

The systems attributes also apply to compound units, and are computed in the following way.

1. The `prefixable` system is only applicable to base_components, and is thus removed
2. The `number_prefixes`, `dimensionality_prefix`, `si_prefix`, and `binary_prefix` are ignored
   * Example: systems(square-kilometer) = systems(meter)
3. Currency units have the `currency` system
   * Example: systems(curr-usd) = {currency}
4. Units linked by `-and-`, `-per-`, and *adjacency* are resolved using a modified intersection, where:
   1. The intersection of {… si …} and {… si_acceptable … } is {… si_acceptable …}
   2. The intersection of {… metric …} and {… metric_adjacent … } is {… metric_adjacent …}

Examples:
```text
systems(liter-per-hectare)
	= {si_acceptable metric} ∪ {si_acceptable metric}
	= {si_acceptable metric}
systems(meter-per-hectare)
	= {si metric} ∩ {si_acceptable metric}
	= {si_acceptable metric}
systems(mile-scandinavian-per-hour)
	= {metric_adjacent} ∩ {si_acceptable metric_adjacent}
	= {metric_adjacent}
```

#### <a name="Conversion_Mechanisms" id="Conversion_Mechanisms" href="#Conversion_Mechanisms">Conversion Mechanisms</a>

CLDR follows conversion values where possible from:
* [NIST Special Publication 1038](https://www.govinfo.gov/content/pkg/GOVPUB-C13-f10c2ff9e7af2091314396a2d53213e4/pdf/GOVPUB-C13-f10c2ff9e7af2091314396a2d53213e4.pdf)
* [International Astronomical Union General Assembly](https://arxiv.org/pdf/1510.07674.pdf)

* See also [NIST Guide to the SI, Chapter 4: The Two Classes of SI Units and the SI Prefixes](https://www.nist.gov/pml/special-publication-811/nist-guide-si-chapter-4-two-classes-si-units-and-si-prefixes)


* For complex units, such as _pound-force-per-square-inch_, the conversions are computed by combining the conversions of each of the simple units: _pound-force_ and _inch_. Because the conversions in convertUnit are reversible, the computation can go from complex source unit to complex base unit to complex target units.


Here is an example:

> **50 foot-per-minute ⟹ X mile-per-hour**
> ⟹ source: 1 foot
> ⟹ factor: 381 / 1250 = 0.3048 meter
> ⟹ source: 1 minute
> ⟹ factor: 60 second
> ⟹ intermediate: 127 / 500 = 0.254 meter-per-second
> ⟹ mile-per-hour
> ⟹ source: 1 mile
> ⟹ factor: 201168 / 125 = 1609.344 meter
> ⟹ source: 1 hour
> ⟹ factor: 3600 second
> ⟹ target: 25 / 44 ≅ 0.5681818 mile-per-hour

**Reciprocals.** When you convert a complex unit to another complex unit, you typically convert the source to a complex base unit (like _meter-per-cubic-meter_), then convert the latter backwards to the desired target. However, there may not be a matching conversion from that complex base unit to the desired target unit. That is the case for converting from _mile-per-gallon_ (used in the US) to _liter-per-100-kilometer_ (used in Europe and elsewhere). When that happens, the reciprocal of the complex base unit is used, as in the following example:

> **50 mile-per-gallon ⟹ X liter-per-100-kilometer**
> ⟹ source: 1 mile
> ⟹ factor: 201168 / 125 = 1609.344 meter
> ⟹ source: 1 gallon
> ⟹ factor: 473176473 / 125000000000 ≅ 0.003785412 cubic-meter
> ⟹ intermediate: 2400000000000 / 112903 ≅ 2.125719E7 meter-per-cubic-meter
> ⟹ liter-per-100-kilometer
> ⟹ source: 1 liter
> ⟹ factor: 1 / 1000 = 0.001 cubic-meter
> ⟹ source: 1 100-kilometer
> ⟹ factor: 100000 meter
> **⟹ 1/intermediate: 112903 / 2400000000000 ≅ 4.704292E-8 cubic-meter-per-meter**
> ⟹ target: 112903 / 24000 ≅ 4.704292 liter-per-100-kilometer

This applies to more than just these cases: one can convert from any unit to related reciprocals as in the following example:

> **50 foot-per-minute ⟹ X hour-per-mile**
> ⟹ source: 1 foot
> ⟹ factor: 381 / 1250 = 0.3048 meter
> ⟹ source: 1 minute
> ⟹ factor: 60 second
> ⟹ intermediate: 127 / 500 = 0.254 meter-per-second
> ⟹ hour-per-mile
> ⟹ source: 1 hour
> ⟹ factor: 3600 second
> ⟹ source: 1 mile
> ⟹ factor: 201168 / 125 = 1609.344 meter
> **⟹ 1/intermediate: 500 / 127 ≅ 3.937008 second-per-meter**
> ⟹ target: 44 / 25 = 1.76 hour-per-mile

#### <a name="Exceptional_Cases" id="Exceptional_Cases" href="#Exceptional_Cases">Exceptional Cases</a>

#### <a name="Identities" id="Identities" href="#Identities">Identities</a>

For completeness, identity mappings are also provided for the base units themselves, such as:

```xml
<convertUnit source='meter' baseUnit='meter' />
```

#### <a name="Aliases" id="Aliases" href="#Aliases">Aliases</a>

In a few instances the old identifiers are deprecated in favor of regular syntax. Implementations should handle both on input:

```xml
<unitAlias type="meter-per-second-squared" replacement="meter-per-square-second" reason="deprecated"/>
<unitAlias type="liter-per-100kilometers" replacement="liter-per-100-kilometer" reason="deprecated"/>
<unitAlias type="pound-foot" replacement="pound-force-foot" reason="deprecated"/>
<unitAlias type="pound-per-square-inch" replacement="pound-force-per-square-inch" reason="deprecated"/>
```

These use the standard alias elements in XML, and are also included in the [units.xml](https://github.com/unicode-org/cldr/blob/main/common/supplemental/units.xml) file.

#### <a name="Duplicate_Units" id="Duplicate_Units" href="#Duplicate_Units">“Duplicate” Units</a>

* Some CLDR units are provided simply because they have different names in some languages. For example, year and year-person, or foodcalorie and kilocalorie. One CLDR unit is not convertible (temperature-generic), it is only used for the translation (where the exact unit would be understood by context).


#### <a name="Discarding_Offsets" id="Discarding_Offsets" href="#Discarding_Offsets">Discarding Offsets</a>

* The temperature units are special. When they represent a scale, they have an offset. But where they represent an amount, such as in complex units, they do not. So celsius-per-second is the same as kelvin-per-second.


#### <a name="Unresolved_Units" id="Unresolved_Units" href="#Unresolved_Units">Unresolved Units</a>

* Some SI units contain the same units in the numerator and denominator, so those cannot be resolved. For example, if cubic-meter-per-meter were always resolved, then _consumption_ (like “liter-per-kilometer”) could not be distinguished from _area_ (square-meter).


* However, in conversion, it may be necessary to resolve them in order to find a match. For example, kilowatt-hour maps to the base unit kilogram-square-meter-second-per-cubic-second, but that needs to be resolved to kilogram-square-meter-per-square-second in order matched against an _energy._



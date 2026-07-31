## <a name="Quantities_and_Base_Units" id="Quantities_and_Base_Units" href="#Quantities_and_Base_Units">Quantities and Base Units</a>

```dtd
<!ELEMENT unitQuantities ( unitQuantity* ) >

<!ELEMENT unitQuantity EMPTY >

<!ATTLIST unitQuantity baseUnit NMTOKEN #REQUIRED >

<!ATTLIST unitQuantity quantity NMTOKENS #REQUIRED >

<!ATTLIST unitQuantity status NMTOKEN #IMPLIED >

<!ATTLIST unitQuantity description CDATA #IMPLIED >
```

* Conversion is supported between comparable units. Those can be simple units, such as length, or more complex ‘derived’ units that are built up from _base units_. The `<unitQuantities>` element provides information on the base units used for conversion. It also supplies information about their _quantity_: mass, length, time, etc., and whether they are simple or not.


Examples:

```xml
<unitQuantity baseUnit='kilogram' quantity='mass' status='simple'/>
<unitQuantity baseUnit='meter-per-second' quantity='speed'/>
```

The order of the elements in the file is significant, since it is used in [Unit_Identifier_Normalization](#Unit_Identifier_Normalization).

* The quantity values themselves are informative. For example, _force per area_ can be referenced as either _pressure_ or _stress_. The quantity for a complex unit that has a reciprocal is formed by prepending “inverse-” to the quantity, such as _inverse-consumption._


* The base units for the quantities and the quantities themselves are based on [NIST Special Publication 811](https://www.nist.gov/pml/special-publication-811) and the earlier [NIST Special Publication 1038](https://www.govinfo.gov/content/pkg/GOVPUB-C13-f10c2ff9e7af2091314396a2d53213e4/pdf/GOVPUB-C13-f10c2ff9e7af2091314396a2d53213e4.pdf). In some cases, a different unit is chosen for the base. For example, a _revolution_ (360°) is chosen for the base unit for angles instead of the SI _radian_, and _item_ instead of the SI _mole_. Additional base units are added where necessary, such as _bit_ and _pixel_.


* This data is not necessary for conversion, but is needed for [Unit_Identifier_Normalization](#Unit_Identifier_Normalization). Some of the `unitQuantity` elements are not needed to convert CLDR units, but are included for completeness. Example:


```xml
<unitQuantity baseUnit='ampere-per-square-meter' quantity='current-density'/>
```

### <a name="UnitType_vs_Quantity" id="UnitType_vs_Quantity" href="#UnitType_vs_Quantity">UnitType vs Quantity</a>

* The unitType (as in “length-meter”) is not the same as the quantity. It is often broader: for example, the unitType _electric_ corresponds to the quantities _electric-current, electric-resistance,_ and _voltage_. The unitType itself is also informative, and can be dropped from a long unit identifier to get a still-unique short unit identifier.


### <a name="Unit_Identifier_Normalization" id="Unit_Identifier_Normalization" href="#Unit_Identifier_Normalization">Unit Identifier Normalization</a>

There are many possible ways to construct complex units. For comparison of unit identifiers, and for formatting, an implementation can normalize in the following way:

1. Convert all but the first -per- to simple multiplication. The result then has the format of /numerator ( -per- denominator)?/
   * foot-per-second-per-second ⇒ foot-per-second-second
2. Within each of the numerator and denominator:
3. Convert multiple instances of a unit into the appropriate power.
   * foot-per-second-second ⇒ foot-per-square-second
   * kilogram-meter-kilogram ⇒ meter-square-kilogram
4. For each single unit, disregarding prefixes and powers, get the order of the _simple_ unit among the `unitQuantity` elements in the [units.xml](https://github.com/unicode-org/cldr/blob/main/common/supplemental/units.xml).
Sort the single units by that order, using a stable sort.
If there are private-use single units, sort them after all the non-private use single units, in alphabetical order.
   * meter-square-kilogram ⇒ square-kilogram-meter
   * meter-square-gram ⇒ square-gram-meter
5. As an edge case, there could be two adjacent single units with the same _simple_ unit but different prefixes such as _meter-kilometer_.
In that case, sort a sequence of those units by the larger prefixes first, so … megameter < … meter < … picometer < …
     * meter-kilometer ⇒ kilometer-meter

The examples in #4 are due to the following ordering of the `unitQuantity` elements:

```xml
1.  <unitQuantity baseUnit='candela' quantity='luminous-intensity' status='simple'/>
2.  <unitQuantity baseUnit='kilogram' quantity='mass' status='simple'/>
3.  <unitQuantity baseUnit='meter' quantity='length' status='simple'/>
4.  …
```

Note that this uses an ordering of elements _within_ a unit identifier. It is different than an ordering _of_ separate units, such as within a table.


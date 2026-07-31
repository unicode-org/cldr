## <a name="Unit_Preferences" id="Unit_Preferences" href="#Unit_Preferences">Unit Preferences</a>

* Different locales have different preferences for which unit or combination of units is used for a particular usage, such as measuring a person’s height. This is more fine-grained than merely a preference for metric versus US or UK measurement systems. For example, one locale may use meters alone, while another may use centimeters alone or a combination of meters and centimeters; a third may use inches alone, or (informally) a combination of feet and inches.


* The determination of preferred units uses the user preference data in [units.xml](https://github.com/unicode-org/cldr/blob/main/common/supplemental/units.xml) together with **input unit**, the **input unit usage**, and the **input locale identifer**.

  * The _well-formed_ and _valid_ **units** are defined according to [Unit Syntax](tr35-general.md#unit-syntax).
  * The _well-formed_ **unit usages** are of the form [a-z0-9]{3-8}("-" [a-z0-9]{3-8})*.
* **The _valid_ **unit**: The _valid_ **unit usages** are the union of the set of `NMTOKENS` in the `usage` attribute value for the `unitPreferences` element in [units.xml](https://github.com/unicode-org/cldr/blob/main/common/supplemental/units.xml).

For example, the following `unitPreferences` elements produce the set {default, floor, geograph, land}.
    * \`<unitPreferences category="area" usage="default">`
    * \`<unitPreferences category="area" usage="geograph land">`
    * \`<unitPreferences category="area" usage="floor">`
  * There are currently no deprecated **unit usages**.
Should there be any in the future, for backwards compatibility the above definition would be expanded to include unitUsageAlias elements.

### <a name="Unit_Preferences_Overrides" id="Unit_Preferences_Overrides" href="#Unit_Preferences_Overrides">Unit Preferences Overrides</a>

Within the locale identifier, the subtags that can affect the result are:
  * the value of the keys mu, ms, and rg
  * the region in the locale identifier (if there is one)
  * and otherwise the likely region subtag for the locale identifier

The strongest priority is the mu key, then the ms key, then the rg key.
Beyond that the region of the locale identifer is used, and if not present, the likely-subtag region.
For example:

|   | Locale                                | Result     | Comment                                                            |
|---|---------------------------------------|------------|--------------------------------------------------------------------|
| 1 | en-u-rg-uszzzz-ms-ussystem-mu-celsius | Celsius    | despite the rg and ms settings for US, and the likely region of US |
| 2 | en-u-rg-uszzzz-ms-metric              | Celsius    | despite the rg setting for US, and the likely region of US         |
| 3 | en-u-rg-dezzzz.                       | Celsius    | despite the likely region of US                                    |
| 4 | en-DE                                 | Celsius    | because explicit region is DE                                      |
| 5 | en                                    | Fahrenheit | because the likely region for en with no region is US              |

If any key-values are invalid, then they are ignored. Thus the following constructs are ignored:

| subtags | reason |
| --- | --- |
| -mu-smoot | invalid unit |
| -ms-stanford | invalid unit system |
| -rg-abzzzz | invalid region 'AB' ‡|
| -AB | invalid region 'AB'|

‡ Only the region portion is currently used.
The -rg-abzzzz is ignored because AB is invalid;
if it were -rg-ustuvxy, it would not be ignored because US is valid.
The table below shows when the region portion is valid or not.

| Key-value | Region | Valid? | Comment |
| --- | --- | --- | --- |
| -rg-usut | US | Yes | Both the region portion (US) and the subdivision portion (ut = Utah) are valid. |
| -rg-uszzzz | US | Yes | Both the region portion (US) and the subdivision portion (zzzz = all) are valid. |
| -rg-usabc | US | Yes | The region portion (US) is valid, but the subdivision portion (abc) is not. |
| -rg-abzzzz | AB | No, ignored | The region portion (AB) is invalid, and thus the -rg is ignored, not matter that the subdivision portion (zzzz) is. |

The following algorithm is used to compute the override units, regions, and category.
The latter two items are used in the [Unit Preferences Data](#Unit_Preferences_Data).

#### <a name="Compute_override_units" id="Compute_override_units" href="#Compute_override_units">Compute override units</a>
If there is a valid -mu value then let the **output unit** be the that value, and return it.
This terminates the algorithm; there is no need to use the unit preferences information.

#### <a name="Compute_regions" id="Compute_regions" href="#Compute_regions">Compute  regions</a>
If there is no valid -mu value, the following steps are used to determine a region R from the **input locale identifer**.
(and optionally a Unit Systems Match (USM)):

1. If there is a valid -ms value then let USM  be the corresponding value in column 2 of the table below.
Otherwise FR is not used. In either case continue with step 2.
2. If there is a valid -rg region portion of the rg value, let R be that region, and go to Compute the category.
    * In the table above, this would handle the examples `usut`, `uszzzz`, and `usabc`, resulting in R = US.
    * Because the example `abzzzz` has an invalid region portion, no region is found and processing continues with step 3.
3. If there is a valid region in the locale, let R be that region, and go to Compute the category.
4. Otherwise, compute the likely subtags for the locale.
    1. If there is a likely region, then let R be that region, and go to Compute the category.
    2. Otherwise, let R be 001, and go to Compute the category

| Key-Value   | Unit Systems Match          | Fallback Region for Unit Preferences |
|-------------|-----------------------------|--------------------------------------|
| ms-metric   | metric OR metric_adjacent   | 001                                  |
| ms-ussystem | ussystem                    | US                                   |
| ms-uksystem | uksystem                    | UK                                   |

#### <a name="Compute_the_category" id="Compute_the_category" href="#Compute_the_category">Compute the category</a>

A **category** is determined as follows from the input unit:

1. From the input unit, use the conversion data in [baseUnit](tr35-info.md#Unit_Conversion) and let the **input base unit** be the baseUnit attribute value.
    * eg, for `pound-force` the baseUnit is `kilogram-meter-per-square-second`.
2. If there is no such base unit (such as for a an unusual unit like `ampere-pound-per-foot-square-minute`),
   convert the input unit to a combination of base units, reduce to lowest terms, and normalize.
   Let the **input base unit** be that value.
       * eg, `ampere-pound-per-foot-square-minute` ⇒ `kilogram-ampere-per-meter-square-second`
3. If the **input base unit** has a unitQuantity element, then let the **category** be the quantity attribute value.
       * eg, `force` from `<unitQuantity baseUnit='kilogram-meter-per-square-second' quantity='force'/>`
4. If the **input base unit** does not have a unitQuantity, let the output unit be the input base unit.
   An implementation may also set it to an equivalent metric/SI unit, as in the example below.
   This terminates the algorithm; there is no need to use the unit preferences information.
      * For example, for `ampere-pound-per-foot-square-minute` an implementation could return `kilogram-ampere-per-meter-square-second` or `pascal-ampere`.
      * That is, an implementation can use shorter metric/SI units as long as long as the combination is equivalent in value.

### <a name="Unit_Preferences_Data" id="Unit_Preferences_Data" href="#Unit_Preferences_Data">Unit Preferences Data</a>

* The CLDR data is intended to map from a particular usage — e.g. measuring the height of a person or the fuel consumption of an automobile — to the unit or combination of units typically used for that usage in a given region. Considerations for such a mapping include:


* The list of possible usages is large and open-ended, and will be extended in the future.
* Even for a given usage such a measuring a road distance, there are different choices of units based on the particular distance.
* For example, one set of units may be used for indicating the distance to the next city (kilometers or miles), while another may be used for indicating the distance to the next exit (meters, yards, or feet).

* There are also differences between more formal usage (official signage, medical records) and more informal usage (conversation, texting).
* For some usages, the measurement may be expressed using a sequence of units, such as “1 meter, 78 centimeters” or “12 stone, 2 pounds”.

The DTD structure is as follows:

```dtd
<!ELEMENT unitPreferenceData ( unitPreferences* ) >

<!ELEMENT unitPreferences ( unitPreference* ) >
<!ATTLIST unitPreferences category NMTOKEN #REQUIRED >
<!ATTLIST unitPreferences usage NMTOKENS #REQUIRED >

<!ELEMENT unitPreference ( #PCDATA ) >
<!ATTLIST unitPreference regions NMTOKENS #REQUIRED >
<!ATTLIST unitPreference geq NMTOKEN #IMPLIED >
<!ATTLIST unitPreference skeleton CDATA #IMPLIED >
```

| Term | Description |
|---|---|
| category | A unit quantity, such as “area” or “length”. See [Unit Conversion](#Unit_Conversion) |
| usage | A type of usage, such as person-height. |
| regions | One or more region identifiers (macroregions or regions), such as 001, US. (Note that this field may be extended in the future to also include subdivision identifiers and/or language identifiers, such as usca, and de-CH.) |
| geq | A threshold value, in a unit determined by the unitPreference element value. The unitPreference element is only used for values higher than this value (and lower than any higher value).<br/>The value must be non-negative. For picking negative units (-3 meters), use the absolute value to pick the unit. |
| skeleton | A skeleton in the ICU number format syntax, that is to be used to format the output unit amount. |


Logically, the unit preferences data is a map from categories to a map of usages to a map of regions to a list of ranked units and optional formats.

**Note:** As of CLDR 37, the `<unitPreference>` `geq` attribute replaces the now-deprecated `<unitPreferences>` `scope` attribute.

#### <a name="Examples" id="Examples" href="#Examples">Examples:</a>

```xml
<unitPreferences category="length" usage="default">
    <unitPreference regions="001">kilometer</unitPreference>
    <unitPreference regions="001">meter</unitPreference>
    <unitPreference regions="001">centimeter</unitPreference>
    <unitPreference regions="US GB">mile</unitPreference>
    <unitPreference regions="US GB">foot</unitPreference>
    <unitPreference regions="US GB">inch</unitPreference>
</unitPreferences>
```

The above information says that for default usage, in the US people use mile, foot, and inch, where people in the rest of the world (001) use kilometer, meter, and centimeter. Take another example:

```xml
<unitPreferences category="length" usage="road">
    <unitPreference regions="001" geq="0.9">kilometer</unitPreference>
    <unitPreference regions="001" geq="300.0" skeleton="precision-increment/50">meter</unitPreference>
    <unitPreference regions="001" skeleton="precision-increment/10">meter</unitPreference>
    <unitPreference regions="001">meter</unitPreference>
    <unitPreference regions="US" geq="0.5">mile</unitPreference>
    <unitPreference regions="US" geq="100.0" skeleton="precision-increment/50">foot</unitPreference>
    <unitPreference regions="US" skeleton="precision-increment/10">foot</unitPreference>
    <unitPreference regions="GB" geq="0.5">mile</unitPreference>
    <unitPreference regions="GB" geq="100.0" skeleton="precision-increment/50">yard</unitPreference>
    <unitPreference regions="GB">yard</unitPreference>
    <unitPreference regions="SE" geq="0.1">mile-scandinavian</unitPreference>
</unitPreferences>
```

The following is the algorithm for computing the preferred output unit from the category, usage, region, and USM.

#### <a name="Compute_the_preferred_output_unit" id="Compute_the_preferred_output_unit" href="#Compute_the_preferred_output_unit">Compute the preferred output unit</a>

1. Let category preferences be the result of a lookup of **category** in the unit preferences.
    1. If the lookup fails, let the **output unit** be the input base unit or an equivalent metric/SI unit, and return. This terminates the algorithm.
2. Let category-usage preferences be the result of a lookup of **input usage** in the category preferences.
    1. If the lookup fails, let the **input usage** be its containing usage, and repeat. (This will always terminate is always a 'default' usage for each category.)
    2. The containing usage is the result of truncating the last '-' and following text, if there is a '-', and other wise 'default'
        * For example, land-agriculture-grain ⊂ land-agriculture ⊂ land ⊂ default
3. Let ranked units be the result of a lookup of R in the category-usage preferences. There may be both region values and [containment regions](https://www.unicode.org/cldr/charts/latest/supplemental/territory_containment_un_m_49.html).
    1. If the lookup of R fails, set R to its containing region and repeat. (This will always terminate because 001 is always present.)
        * For example, CH (Switzerland) ⊂ 155 (Western Europe) ⊂ 150 (Europe) ⊂ 001 (World).
        * This loop can be optimized to only include containing regions that occur in the data (eg, only 001 in LDML 45).
4. If there is a USM, and the corresponding Fallback Region is different than R, and any of the units in the ranked list don't match the USM, then let the ranked units be the result of a lookup of the Fallback Region in the category-usage preferences.

#### <a name="Search_the_ranked_units" id="Search_the_ranked_units" href="#Search_the_ranked_units">Search the ranked units</a>

The ranked units will be of the following form:
  ```xml
  `<unitPreference regions="GB" geq="0.5">`mile`</unitPreference>`
  `<unitPreference regions="GB" geq="100.0" skeleton="precision-increment/50">`yard`</unitPreference>`
  `<unitPreference regions="GB">`yard`</unitPreference>`
```

* The geq item gives the value for the unit in the element value (or for the largest unit for mixed units). For example,
  * `...geq="0.5">mile<...` is ≥ 0.5 miles
  * `...geq="100.0">foot-and-inch<...` is  ≥ 100 feet
* If there is no `geq` attribute, then the implicit value is 1.0.
* Implementations will probably convert the values into the base units, so that the comparison is fast. Thus the above would be converted internally to something like:
  * ≥ 804.672 meters ⇒ mile
  * ≥ 30.48 meters ⇒ foot-and-inch

1. Search for the first matching unitPreference for the absolute value of the input measure. If there is no match (eg < 100 feet in the above example), take the last unitPreference. That is, the last unitPreference is effectively geq="0". In the above example, ``<unitPreference regions="GB">`yard`</unitPreference>`` is equivalent to ``<unitPreference geq="0" regions="GB">`yard`</unitPreference>``

For completeness, when comparing doubles to the geq values:
* Negative numbers are treated as if they were positive, so in the above example -804.672 meters will format as "-0.5 mile".
* _infinity_, NaN, and -_infinity_ match the largest possible value. Thus -∞ meters will format as "-∞ miles", not "-∞ yards".

2. Once a matching `unitPreference` element is found:

* The unit is the element value
* The skeleton (if there is one) supplies formatting information for the unit. API settings may allow that to be overridden.
  * The syntax and semantics for the skeleton value are defined by the [ICU Number Skeletons](https://unicode-org.github.io/icu/userguide/format_parse/numbers/skeletons.html) document.
* If the skeleton is missing, the default is skeleton="**precision-integer/@@\***". However, the client can also override or tune the number formatting.
* If the unit is mixed (eg foot-and-inch) the skeleton applies to the final subunit; the higher subunits are formatted as integers.

### <a name="Constraints" id="Constraints" href="#Constraints">Constraints</a>

* For a given category, there is always a “default” usage.
* For a given category and usage:
  * There is always a 001 region.
  * None of the sets of regions can overlap. That is, you can’t have “US” on one line and “US GB” on another. You _can_ have two lines with “US”, for different sizes of units.
* For a given category, usage, and region-set
  * The unitPreferences are in descending order.

#### <a name="Examples" id="Examples" href="#Examples">Examples</a>

**Example A: xx-SE-u-ms-metric, length, road**
1. Fetch the data from `<unitPreferences category="length" usage="road">` for xx-SE
```xml
<unitPreference regions="SE">mile-scandinavian</unitPreference>
<unitPreference regions="SE">kilometer</unitPreference>
<unitPreference regions="SE" geq="300.0" skeleton="precision-increment/50">meter</unitPreference>
<unitPreference regions="SE" geq="10" skeleton="precision-increment/10">meter</unitPreference>
<unitPreference regions="SE" skeleton="precision-increment/1">meter</unitPreference>
```
2. Meter is **metric**, mile-scandinavian is **metric_adjacent** so they both match the key-value ms-**metric**, so no change is made.

**Example B: xx-GB-u-ms-ussystem, volume, fluid**
1. Fetch the data from `<unitPreferences category="volume" usage="fluid">` for xx-GB
```xml
<unitPreference regions="GB">gallon-imperial</unitPreference>
<unitPreference regions="GB">fluid-ounce-imperial</unitPreference>
```
2. At least one of {gallon-imperial, fluid-ounce-imperial} does not match ms-**ussystem** so the locale is shifted to xx-**US**, and uses the following:
```xml
<unitPreference regions="US">gallon</unitPreference>
<unitPreference regions="US">quart</unitPreference>
<unitPreference regions="US">pint</unitPreference>
<unitPreference regions="US">cup</unitPreference>
<unitPreference regions="US">fluid-ounce</unitPreference>
<unitPreference regions="US">tablespoon</unitPreference>
<unitPreference regions="US">teaspoon</unitPreference>
```


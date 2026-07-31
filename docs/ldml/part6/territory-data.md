## <a name="Territory_Data" id="Territory_Data" href="#Territory_Data">Territory Data</a>

### <a name="Supplemental_Territory_Containment" id="Supplemental_Territory_Containment" href="#Supplemental_Territory_Containment">Supplemental Territory Containment</a>

```dtd
<!ELEMENT territoryContainment ( group* ) >
<!ELEMENT group EMPTY >
<!ATTLIST group type NMTOKEN #REQUIRED >
<!ATTLIST group contains NMTOKENS #IMPLIED >
<!ATTLIST group grouping ( true | false ) #IMPLIED >
<!ATTLIST group status ( deprecated, grouping ) #IMPLIED >
```

* The following data provides information that shows groupings of countries (regions). The data is based on the [[UNM49](tr35.md#UNM49)]. There is one special code, `QO` , which is used for outlying areas of Oceania that are typically uninhabited. The territory containment forms a tree with the following levels:


+ World
  + Continent
    + Subcontinent
      + Country

Excluding groupings, in this tree:

*   All non-overlapping regions form a strict tree rooted at World.
*   All leaf-nodes (country) are always at depth 4. Some of these “country” regions are actually parts of other countries, such as Hong Kong (part of China). Such relationships are not part of the containment data.

* For a chart showing the relationships (plus the included timezones), see the [Territory Containment Chart](https://www.unicode.org/cldr/charts/latest/supplemental/territory_containment_un_m_49.html). The XML structure has the following form.


```xml
<territoryContainment>

    <group type="001" contains="002 009 019 142 150"/> <!--World -->
    <group type="011" contains="BF BJ CI CV GH GM GN GW LR ML MR NE NG SH SL SN TG"/> <!--Western Africa -->
    <group type="013" contains="BZ CR GT HN MX NI PA SV"/> <!--Central America -->
    <group type="014" contains="BI DJ ER ET KE KM MG MU MW MZ RE RW SC SO TZ UG YT ZM ZW"/> <!--Eastern Africa -->
    <group type="142" contains="030 035 062 145"/> <!--Asia -->
    <group type="145" contains="AE AM AZ BH CY GE IL IQ JO KW LB OM PS QA SA SY TR YE"/> <!--Western Asia -->
    <group type="015" contains="DZ EG EH LY MA SD TN"/> <!--Northern Africa -->
...
```

There are groupings that don't follow this regular structure, such as:

```xml
<group type="003" contains="013 021 029" grouping="true"/> <!--North America -->
```

These are marked with the attribute `grouping="true"`.

When groupings have been deprecated but kept around for backwards compatibility, they are marked with the attribute `status="deprecated"`, like this:

```xml
<group type="029" contains="AN" status="deprecated"/> <!--Caribbean -->
```

When the containment relationship itself is a grouping, it is marked with the attribute `status="grouping"`, like this:

```xml
<group type="150" contains="EU" status="grouping"/> <!--Europe -->
```

That is, the type value isn’t a grouping, but if you filter out groupings you can drop this containment. In the example above, EU is a grouping, and contained in 150.

### <a name="Subdivision_Containment" id="Subdivision_Containment" href="#Subdivision_Containment">Subdivision Containment</a>

```dtd
<!ELEMENT subdivisionContainment ( subgroup* ) >

<!ELEMENT subgroup EMPTY >
<!ATTLIST subgroup type NMTOKEN #REQUIRED >
<!ATTLIST subgroup contains NMTOKENS #IMPLIED >
```

The subdivision containment data is similar to the territory containment. It is based on ISO 3166-2 data, but may diverge from it in the future.

```xml
<subgroup type="BD" contains="bda bdb bdc bdd bde bdf bdg bdh" />
<subgroup type="bda" contains="bd02 bd06 bd07 bd25 bd50 bd51" />
```

* The `type` is a [`unicode_region_subtag`](tr35.md#unicode_region_subtag) (territory) identifier for the top level of containment, or a [`unicode_subdivision_id`](tr35.md#unicode_subdivision_id) for lower levels of containment when there are multiple levels. The `contains` value is a space-delimited list of one or more [`unicode_subdivision_id`](tr35.md#unicode_subdivision_id) values. In the example above, subdivision bda contains other subdivisions bd02, bd06, bd07, bd25, bd50, bd51.


Note: Formerly (in CLDR 28 through 30):

* The `type` attribute could only contain a `unicode_region_subtag`;
* The `contains` attribute contained `unicode_subdivision_suffix` values; these are not unique across multiple territories, so...
* For lower containment levels, a now-deprecated subtype `attribute` was used to specify the parent `unicode_subdivision_suffix`.

* **\* The type**: \* The type attribute contained only a `unicode_region_subtag` `unicode_subdivision_suffix` values were used in the `contains` attribute; these are not unique across multiple territories, so for lower levels a now-deprecated


### <a name="Supplemental_Territory_Information" id="Supplemental_Territory_Information" href="#Supplemental_Territory_Information">Supplemental Territory Information</a>

```dtd
<!ELEMENT territory ( languagePopulation* ) >
<!ATTLIST territory type NMTOKEN #REQUIRED >
<!ATTLIST territory gdp NMTOKEN #REQUIRED >
<!ATTLIST territory literacyPercent NMTOKEN #REQUIRED >
<!ATTLIST territory population NMTOKEN #REQUIRED >

<!ELEMENT languagePopulation EMPTY >
<!ATTLIST languagePopulation type NMTOKEN #REQUIRED >
<!ATTLIST languagePopulation literacyPercent NMTOKEN #IMPLIED >
<!ATTLIST languagePopulation writingPercent NMTOKEN #IMPLIED >
<!ATTLIST languagePopulation populationPercent NMTOKEN #REQUIRED >
<!ATTLIST languagePopulation officialStatus (de_facto_official | official | official_regional | official_minority) #IMPLIED >
```

* This data provides testing information for language and territory populations. The main goal is to provide approximate figures for the literate, functional population for each language in each territory: that is, the population that is able to read and write each language, and is comfortable enough to use it with computers. For a chart of this data, see [Territory-Language Information](https://www.unicode.org/cldr/charts/latest/supplemental/territory_language_information.html).


_Example_

```xml
<territory type="AO" gdp="175500000000" literacyPercent="70.4" population="19088100"> <!--Angola-->
    <languagePopulation type="pt" populationPercent="67" officialStatus="official"/> <!--Portuguese-->
    <languagePopulation type="umb" populationPercent="29"/> <!--Umbundu-->
    <languagePopulation type="kmb" writingPercent="10" populationPercent="25" references="R1034"/> <!--Kimbundu-->
    <languagePopulation type="ln" populationPercent="0.67" references="R1010"/> <!--Lingala-->
</territory>
```

* Note that reliable information is difficult to obtain; the information in CLDR is an estimate culled from different sources, including the World Bank, CIA Factbook, and others. The GDP and country literacy figures are taken from the World Bank where available, otherwise supplemented by FactBook data and other sources. The GDP figures are “PPP (constant 2000 international $)”. Much of the per-language data is taken from the Ethnologue, but is supplemented and processed using many other sources, including per-country census data. (The focus of the Ethnologue is native speakers, which includes people who are not literate, and excludes people who are functional second-language users.) Some references are marked in the XML files, with attributes such as `references="R1010"` .


* The percentages may add up to more than 100% due to multilingual populations, or may be less than 100% due to illiteracy or because the data has not yet been gathered or processed. Languages with smaller populations might not be included.


The following describes the meaning of some of these terms—as used in CLDR—in more detail.

<a name="literacy_percent" href="#literacy_percent">literacy percent for the territory</a> — an estimate of the percentage of the country’s population that is functionally literate.

<a name="language_population_percent" href="#language_population_percent">language population percent</a> — an estimate of the number of people who are functional in that language in that country, including both first and second language speakers. The level of fluency is that necessary to use a UI on a computer, smartphone, or similar devices, rather than complete fluency.

<a name="literacy_percent_for_langPop" href="#literacy_percent_for_langPop">literacy percent for language population</a> — Within the set of people who are functional in the corresponding language (as specified by [language population percent](#language_population_percent)), this is an estimate of the percentage of those people who are functionally literate in that language, that is, who are _capable_ of reading or writing in that language, even if they do not regularly use it for reading or writing. If not specified, this defaults to the [literacy percent for the territory](#literacy_percent).

<a name="writing_percent" href="#writing_percent">writing percent</a> — Within the set of people who are functional in the corresponding language (as specified by [language population percent](#language_population_percent)), this is an estimate of the percentage of those people who regularly read or write a significant amount in that language. Ideally, the regularity would be measured as “7-day actives”. If it is known that the language is not widely or commonly written, but there are no solid figures, the value is typically given 1%-5%.

* For a language such as Swiss German, which is typically not written, even though nearly the whole native Germanophone population _could_ write in Swiss German, the [literacy percent for language population](#literacy_percent_for_langPop) is high, but the [writing percent](#writing_percent) is low.


<a name="official_language" href="#official_language">official language</a> — as used in CLDR, a language that can generally be used in all communications with a central government. That is, people can expect that essentially all communication from the government is available in that language (ballots, information pamphlets, legal documents, …) and that they can use that language in any communication to the central government (petitions, forms, filing lawsuits, …).

* Official languages for a country in this sense are not necessarily the same as those with official legal status in the country. For example, Irish is declared to be an official language in Ireland, but English has no such formal status in the United States. Languages such as the latter are called _de facto_ official languages. As another example, German has legal status in Italy, but cannot be used in all communications with the central government, and is thus not an official language _of Italy_ for CLDR purposes. It is, however, an _official regional language_. Other languages are declared to be official, but can’t actually be used for all communication with any major governmental entity in the country. There is no intention to mark such nominally official languages as “official” in the CLDR data.


<a name="official_regional_language" href="#official_regional_language">official regional language</a> — a language that is official (_de jure_ or _de facto_) in a major region within a country, but does not qualify as an official language of the country as a whole. For example, it can be used in an official petition to a provincial government, but not the central government. The term “major” is meant to distinguish from smaller-scale usage, such as for a town or village.

### <a name="Territory_Based_Preferences" id="Territory_Based_Preferences" href="#Territory_Based_Preferences">Territory-Based Preferences</a>

* The default preference for several locale items is based solely on a [unicode_region_subtag](tr35.md#unicode_region_subtag), which may either be specified as part of a [unicode_language_id](tr35.md#unicode_language_id), inferred from other locale ID elements using the [Likely Subtags](tr35.md#Likely_Subtags) mechanism, or provided explicitly using an “rg” [Region Override](tr35.md#RegionOverride) locale key. For more information on this process see [Locale Inheritance and Matching](tr35.md#Locale_Inheritance). The specific items that are handled in this way are:


* Default calendar (see [Calendar Preference Data](tr35-dates.md#Calendar_Preference_Data))
* Default week conventions (first day of week and weekend days; see [Week Data](tr35-dates.md#Week_Data))
* Default hour cycle (see [Time Data](tr35-dates.md#Time_Data))
* Default currency (see [Supplemental Currency Data](tr35-numbers.md#Supplemental_Currency_Data))
* Default measurement system and paper size (see [Measurement System Data](tr35-general.md#Measurement_System_Data))
* Default units for specific usage (see [Preferred Units for Specific Usages](#Preferred_Units_For_Usage), below)

The mu, ms, and rg keys also interact with the base locale and the unit preferences. For more information, see _[Unit Preferences](#Unit_Preferences)._

#### <a name="Preferred_Units_For_Usage" id="Preferred_Units_For_Usage" href="#Preferred_Units_For_Usage">Preferred Units for Specific Usages</a>

The determination of preferred units depends on the locale identifer: the keys mu, ms, rg, the base locale (language, script, region) and the user preferences.
_For information about preferred units and unit conversion, see [Unit Conversion](#Unit_Conversion) and [Unit Preferences](#Unit_Preferences)._

### <a name="rgScope" id="rgScope" href="#rgScope">`<rgScope>`: Scope of the “rg” Locale Key</a>

* The supplemental `<rgScope>` element specifies the data paths for which the region used for data lookup is determined by the value of any “rg” key present in the locale identifier (see [Region Override](tr35.md#RegionOverride) and [Region Priority Inheritance](tr35.md#Region_Priority_Inheritance)). If no “rg” key is present, the region used for lookup is determined as usual: from the unicode_region_subtag if present, else inferred from the unicode_language_subtag. The DTD structure is as follows:


```dtd
<!ELEMENT rgScope ( rgPath* ) >

<!ELEMENT rgPath EMPTY >
<!ATTLIST rgPath path CDATA #REQUIRED >
```

The `<rgScope>` element contains a list of `<rgPath>` elements, each of which specifies a datapath for which any “rg” key determines the region for lookup. For example:

```xml
<rgScope>
    <rgPath path="//supplementalData/currencyData/fractions/info[@iso4217='#'][@digits='*'][@rounding='*'][@cashDigits='*'][@cashRounding='*']" draft="provisional" />
    <rgPath path="//supplementalData/currencyData/fractions/info[@iso4217='#'][@digits='*'][@rounding='*'][@cashRounding='*']" draft="provisional" />
    <rgPath path="//supplementalData/currencyData/fractions/info[@iso4217='#'][@digits='*'][@rounding='*']" draft="provisional" />
    <rgPath path="//supplementalData/calendarPreferenceData/calendarPreference[@territories='#'][@ordering='*']" draft="provisional" />
    ...
    <rgPath path="//supplementalData/unitPreferenceData/unitPreferences[@category='*'][@usage='*'][@scope='*']/unitPreference[@regions='#'][@alt='*']" draft="provisional" />
    <rgPath path="//supplementalData/unitPreferenceData/unitPreferences[@category='*'][@usage='*'][@scope='*']/unitPreference[@regions='#']" draft="provisional" />
    <rgPath path="//supplementalData/unitPreferenceData/unitPreferences[@category='*'][@usage='*']/unitPreference[@regions='#'][@alt='*']" draft="provisional" />
    <rgPath path="//supplementalData/unitPreferenceData/unitPreferences[@category='*'][@usage='*']/unitPreference[@regions='#']" draft="provisional" />
</rgScope>
```

The exact format of the path is provisional in CLDR 29, but as currently shown:

*   An attribute value of `'*'` indicates that the path applies regardless of the value of the attribute.
*   Each path must have exactly one attribute whose value is marked here as `'#'`; in actual data items with this path, the corresponding value is a list of region codes. It is the region codes in this list that are compared with the region specified by the “rg” key to determine which data item to use for this path.


## Unicode Technical Standard #35

# Unicode Locale Data Markup Language (LDML)<br/>Part 6: Supplemental

<!-- HTML: no header -->
<table><tbody>
<tr><td>Version</td><td><b>42 (draft)</b></td></tr>
<tr><td>Editors</td><td>Steven Loomis (<a href="mailto:srl@icu-project.org">srl@icu-project.org</a>) and <a href="tr35.html#Acknowledgments">other CLDR committee members</a></td></tr>
</tbody></table>

For the full header, summary, and status, see [Part 1: Core](tr35.md).

### _Summary_

This document describes parts of an XML format (_vocabulary_) for the exchange of structured locale data. This format is used in the [Unicode Common Locale Data Repository](https://unicode.org/cldr/).

This is a partial document, describing only those parts of the LDML that are relevant for supplemental data. For the other parts of the LDML see the [main LDML document](tr35.md) and the links above.

_Note:_
Some links may lead to in-development or older
versions of the data files.
See <https://cldr.unicode.org> for up-to-date CLDR release data.

### _Status_

_This document has been reviewed by Unicode members and other interested parties, and has been approved for publication by the Unicode Consortium. This is a stable document and may be used as reference material or cited as a normative reference by other specifications._

> _**A Unicode Technical Standard (UTS)** is an independent specification. Conformance to the Unicode Standard does not imply conformance to any UTS._

_Please submit corrigenda and other comments with the CLDR bug reporting form [[Bugs](tr35.md#Bugs)]. Related information that is useful in understanding this document is found in the [References](tr35.md#References). For the latest version of the Unicode Standard see [[Unicode](tr35.md#Unicode)]. For a list of current Unicode Technical Reports see [[Reports](tr35.md#Reports)]. For more information about versions of the Unicode Standard, see [[Versions](tr35.md#Versions)]._

## <a name="Parts" href="#Parts">Parts</a>

The LDML specification is divided into the following parts:

*   Part 1: [Core](tr35.md#Contents) (languages, locales, basic structure)
*   Part 2: [General](tr35-general.md#Contents) (display names & transforms, etc.)
*   Part 3: [Numbers](tr35-numbers.md#Contents) (number & currency formatting)
*   Part 4: [Dates](tr35-dates.md#Contents) (date, time, time zone formatting)
*   Part 5: [Collation](tr35-collation.md#Contents) (sorting, searching, grouping)
*   Part 6: [Supplemental](tr35-info.md#Contents) (supplemental data)
*   Part 7: [Keyboards](tr35-keyboards.md#Contents) (keyboard mappings)

## <a name="Contents" href="#Contents">Contents of Part 6, Supplemental</a>

* 1 Introduction [Supplemental Data](#Supplemental_Data)
* 2 [Territory Data](#Territory_Data)
  * 2.1 [Supplemental Territory Containment](#Supplemental_Territory_Containment)
  * 2.2 [Subdivision Containment](#Subdivision_Containment)
  * 2.3 [Supplemental Territory Information](#Supplemental_Territory_Information)
  * 2.4 [Territory-Based Preferences](#Territory_Based_Preferences)
    * 2.4.1 [Preferred Units for Specific Usages](#Preferred_Units_For_Usage)
  * 2.5 [`<rgScope>`: Scope of the “rg” Locale Key](#rgScope)
* 3 [Supplemental Language Data](#Supplemental_Language_Data)
* 3.1 [Supplemental Language Grouping](#Supplemental_Language_Grouping)
* 4 [Supplemental Code Mapping](#Supplemental_Code_Mapping)
* 5 ~~[Telephone Code Data](#Telephone_Code_Data)~~ (Deprecated)
* 6 ~~[Postal Code Validation (Deprecated)](#Postal_Code_Validation)~~
* 7 [Supplemental Character Fallback Data](#Supplemental_Character_Fallback_Data)
* 8 [Coverage Levels](#Coverage_Levels)
  * 8.1 [Definitions](#Coverage_Level_Definitions)
  * 8.2 [Data Requirements](#Coverage_Level_Data_Requirements)
  * 8.3 [Default Values](#Coverage_Level_Default_Values)
* 9 [Supplemental Metadata](#Appendix_Supplemental_Metadata)
  * 9.1 [Supplemental Alias Information](#Supplemental_Alias_Information)
    * Table: [Alias Attribute Values](#Alias_Attribute_Values)
  * 9.2 ~~[Supplemental Deprecated Information (Deprecated)](#Supplemental_Deprecated_Information)~~
  * 9.3 [Default Content](#Default_Content)
* 10 [Locale Metadata Elements](#Metadata_Elements)
* 11 [Version Information](#Version_Information)
* 12 [Parent Locales](#Parent_Locales)
* 13 [Unit Conversion](#Unit_Conversion)
  * [Constants](#constants)
  * [Conversion Data](#conversion-data)
    * [Exceptional Cases](#exceptional-cases)
      * [Identities](#identities)
      * [Aliases](#aliases)
      * [“Duplicate” Units](#duplicate-units)
      * [Discarding Offsets](#discarding-offsets)
    * [Unresolved Units](#unresolved-units)
* [Quantities and Base Units](#quantities-and-base-units)
  * [UnitType vs Quantity](#unittype-vs-quantity)
  * [Unit Identifier Normalization](#Unit_Identifier_Normalization)
* [Mixed Units](#mixed-units)
* [Testing](#testing)
* 14 [Unit Preferences](#Unit_Preferences)
  * [Constraints](#constraints)
  * [Caveats](#caveats)

## 1 Introduction <a name="Supplemental_Data" href="#Supplemental_Data">Supplemental Data</a>

The following represents the format for additional supplemental information. This is information that is important for internationalization and proper use of CLDR, but is not contained in the locale hierarchy. It is not localizable, nor is it overridden by locale data. The current CLDR data can be viewed in the [Supplemental Charts](https://unicode-org.github.io/cldr-staging/charts/38/supplemental/index.html).

```xml
<!ELEMENT supplementalData (version, generation?, cldrVersion?, currencyData?, territoryContainment?, subdivisionContainment?, languageData?, territoryInfo?, postalCodeData?, calendarData?, calendarPreferenceData?, weekData?, timeData?, measurementData?, unitPreferenceData?, timezoneData?, characters?, transforms?, metadata?, codeMappings?, parentLocales?, likelySubtags?, metazoneInfo?, plurals?, telephoneCodeData?, numberingSystems?, bcp47KeywordMappings?, gender?, references?, languageMatching?, dayPeriodRuleSet*, metaZones?, primaryZones?, windowsZones?, coverageLevels?, idValidity?, rgScope?) >
```

The data in CLDR is presently split into multiple files: supplementalData.xml, supplementalMetadata.xml, characters.xml, likelySubtags.xml, ordinals.xml, plurals.xml, telephoneCodeData.xml, genderList.xml, plus transforms (see _Part 2 Section 10 [Transforms](tr35-general.md#Transforms)_ and _Part 2 Section 10.3 [Transform Rule Syntax](tr35-general.md#Transform_Rules_Syntax)_). The split is just for convenience: logically, they are treated as though they were a single file. Future versions of CLDR may split the data in a different fashion. Do not depend on any specific XML filename or path for supplemental data.

Note that [Chapter 10](#Metadata_Elements) presents information about metadata that is maintained on a per-locale basis. It is included in this section because it is not intended to be used as part of the locale itself.

## 2 <a name="Territory_Data" href="#Territory_Data">Territory Data</a>

### 2.1 <a name="Supplemental_Territory_Containment" href="#Supplemental_Territory_Containment">Supplemental Territory Containment</a>

```xml
<!ELEMENT territoryContainment ( group* ) >
<!ELEMENT group EMPTY >
<!ATTLIST group type NMTOKEN #REQUIRED >
<!ATTLIST group contains NMTOKENS #IMPLIED >
<!ATTLIST group grouping ( true | false ) #IMPLIED >
<!ATTLIST group status ( deprecated, grouping ) #IMPLIED >
```

The following data provides information that shows groupings of countries (regions). The data is based on the [[UNM49](tr35.md#UNM49)]. There is one special code, `QO` , which is used for outlying areas of Oceania that are typically uninhabited. The territory containment forms a tree with the following levels:

+ World
  + Continent
    + Subcontinent
      + Country

Excluding groupings, in this tree:

*   All non-overlapping regions form a strict tree rooted at World.
*   All leaf-nodes (country) are always at depth 4. Some of these “country” regions are actually parts of other countries, such as Hong Kong (part of China). Such relationships are not part of the containment data.

For a chart showing the relationships (plus the included timezones), see the [Territory Containment Chart](https://unicode-org.github.io/cldr-staging/charts/38/supplemental/territory_containment_un_m_49.html). The XML structure has the following form.

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

### 2.2 <a name="Subdivision_Containment" href="#Subdivision_Containment">Subdivision Containment</a>

```xml
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

The `type` is a [`unicode_region_subtag`](tr35.md#unicode_region_subtag) (territory) identifier for the top level of containment, or a [`unicode_subdivision_id`](tr35.md#unicode_subdivision_id) for lower levels of containment when there are multiple levels. The `contains` value is a space-delimited list of one or more [`unicode_subdivision_id`](tr35.md#unicode_subdivision_id) values. In the example above, subdivision bda contains other subdivisions bd02, bd06, bd07, bd25, bd50, bd51.

Note: Formerly (in CLDR 28 through 30):

* The `type` attribute could only contain a `unicode_region_subtag`;
* The `contains` attribute contained `unicode_subdivision_suffix` values; these are not unique across multiple territories, so...
* For lower containment levels, a now-deprecated subtype `attribute` was used to specify the parent `unicode_subdivision_suffix`.

\* The type attribute contained only a `unicode_region_subtag` `unicode_subdivision_suffix` values were used in the `contains` attribute; these are not unique across multiple territories, so for lower levels a now-deprecated

### 2.3 <a name="Supplemental_Territory_Information" href="#Supplemental_Territory_Information">Supplemental Territory Information</a>

```xml
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

This data provides testing information for language and territory populations. The main goal is to provide approximate figures for the literate, functional population for each language in each territory: that is, the population that is able to read and write each language, and is comfortable enough to use it with computers. For a chart of this data, see [Territory-Language Information](https://unicode-org.github.io/cldr-staging/charts/38/supplemental/territory_language_information.html).

_Example_

```xml
<territory type="AO" gdp="175500000000" literacyPercent="70.4" population="19088100"> <!--Angola-->
    <languagePopulation type="pt" populationPercent="67" officialStatus="official"/> <!--Portuguese-->
    <languagePopulation type="umb" populationPercent="29"/> <!--Umbundu-->
    <languagePopulation type="kmb" writingPercent="10" populationPercent="25" references="R1034"/> <!--Kimbundu-->
    <languagePopulation type="ln" populationPercent="0.67" references="R1010"/> <!--Lingala-->
</territory>
```

Note that reliable information is difficult to obtain; the information in CLDR is an estimate culled from different sources, including the World Bank, CIA Factbook, and others. The GDP and country literacy figures are taken from the World Bank where available, otherwise supplemented by FactBook data and other sources. The GDP figures are “PPP (constant 2000 international $)”. Much of the per-language data is taken from the Ethnologue, but is supplemented and processed using many other sources, including per-country census data. (The focus of the Ethnologue is native speakers, which includes people who are not literate, and excludes people who are functional second-language users.) Some references are marked in the XML files, with attributes such as `references="R1010"` .

The percentages may add up to more than 100% due to multilingual populations, or may be less than 100% due to illiteracy or because the data has not yet been gathered or processed. Languages with smaller populations might not be included.

The following describes the meaning of some of these terms—as used in CLDR—in more detail.

<a name="literacy_percent" href="#literacy_percent">literacy percent for the territory</a> — an estimate of the percentage of the country’s population that is functionally literate.

<a name="language_population_percent" href="#language_population_percent">language population percent</a> — an estimate of the number of people who are functional in that language in that country, including both first and second language speakers. The level of fluency is that necessary to use a UI on a computer, smartphone, or similar devices, rather than complete fluency.

<a name="literacy_percent_for_langPop" href="#literacy_percent_for_langPop">literacy percent for language population</a> — Within the set of people who are functional in the corresponding language (as specified by [language population percent](#language_population_percent)), this is an estimate of the percentage of those people who are functionally literate in that language, that is, who are _capable_ of reading or writing in that language, even if they do not regularly use it for reading or writing. If not specified, this defaults to the [literacy percent for the territory](#literacy_percent).

<a name="writing_percent" href="#writing_percent">writing percent</a> — Within the set of people who are functional in the corresponding language (as specified by [language population percent](#language_population_percent)), this is an estimate of the percentage of those people who regularly read or write a significant amount in that language. Ideally, the regularity would be measured as “7-day actives”. If it is known that the language is not widely or commonly written, but there are no solid figures, the value is typically given 1%-5%.

For a language such as Swiss German, which is typically not written, even though nearly the whole native Germanophone population _could_ write in Swiss German, the [literacy percent for language population](#literacy_percent_for_langPop) is high, but the [writing percent](#writing_percent) is low.

<a name="official_language" href="#official_language">official language</a> — as used in CLDR, a language that can generally be used in all communications with a central government. That is, people can expect that essentially all communication from the government is available in that language (ballots, information pamphlets, legal documents, …) and that they can use that language in any communication to the central government (petitions, forms, filing lawsuits, …).

Official languages for a country in this sense are not necessarily the same as those with official legal status in the country. For example, Irish is declared to be an official language in Ireland, but English has no such formal status in the United States. Languages such as the latter are called _de facto_ official languages. As another example, German has legal status in Italy, but cannot be used in all communications with the central government, and is thus not an official language _of Italy_ for CLDR purposes. It is, however, an _official regional language_. Other languages are declared to be official, but can’t actually be used for all communication with any major governmental entity in the country. There is no intention to mark such nominally official languages as “official” in the CLDR data.

<a name="official_regional_language" href="#official_regional_language">official regional language</a> — a language that is official (_de jure_ or _de facto_) in a major region within a country, but does not qualify as an official language of the country as a whole. For example, it can be used in an official petition to a provincial government, but not the central government. The term “major” is meant to distinguish from smaller-scale usage, such as for a town or village.

### 2.4 <a name="Territory_Based_Preferences" href="#Territory_Based_Preferences">Territory-Based Preferences</a>

The default preference for several locale items is based solely on a [unicode_region_subtag](tr35.md#unicode_region_subtag), which may either be specified as part of a [unicode_language_id](tr35.md#unicode_language_id), inferred from other locale ID elements using the [Likely Subtags](tr35.md#Likely_Subtags) mechanism, or provided explicitly using an “rg” [Region Override](tr35.md#RegionOverride) locale key. For more information on this process see [Locale Inheritance and Matching](tr35.md#Locale_Inheritance). The specific items that are handled in this way are:

* Default calendar (see [Calendar Preference Data](tr35-dates.md#Calendar_Preference_Data))
* Default week conventions (first day of week and weekend days; see [Week Data](tr35-dates.md#Week_Data))
* Default hour cycle (see [Time Data](tr35-dates.md#Time_Data))
* Default currency (see [Supplemental Currency Data](tr35-numbers.md#Supplemental_Currency_Data))
* Default measurement system and paper size (see [Measurement System Data](tr35-general.md#Measurement_System_Data))
* Default units for specific usage (see [Preferred Units for Specific Usages](#Preferred_Units_For_Usage), below)

#### 2.4.1 <a name="Preferred_Units_For_Usage" href="#Preferred_Units_For_Usage">Preferred Units for Specific Usages</a>

_For information about preferred units and unit conversion, see Section 13 [Unit Conversion](#Unit_Conversion) and Section 14 [Unit Preferences](#Unit_Preferences)._

### 2.5 <a name="rgScope" href="#rgScope">`<rgScope>`: Scope of the “rg” Locale Key</a>

The supplemental `<rgScope>` element specifies the data paths for which the region used for data lookup is determined by the value of any “rg” key present in the locale identifier (see [Region Override](tr35.md#RegionOverride)). If no “rg” key is present, the region used for lookup is determined as usual: from the unicode_region_subtag if present, else inferred from the unicode_language_subtag. The DTD structure is as follows:

```xml
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

## 3 <a name="Supplemental_Language_Data" href="#Supplemental_Language_Data">Supplemental Language Data</a>

```xml
<!ELEMENT languageData ( language* ) >
<!ELEMENT language EMPTY >
<!ATTLIST language type NMTOKEN #REQUIRED >
<!ATTLIST language scripts NMTOKENS #IMPLIED >
<!ATTLIST language territories NMTOKENS #IMPLIED >
<!ATTLIST language variants NMTOKENS #IMPLIED >
<!ATTLIST language alt NMTOKENS #IMPLIED >
```

The language data is used for consistency checking and testing. It provides a list of which languages are used with which scripts and in which countries. To a large extent, however, the territory list has been superseded by the data in _Section 2.2 [Supplemental Territory Information](#Supplemental_Territory_Information)_ .

```xml
<languageData>
    <language type="af" scripts="Latn" territories="ZA" />
    <language type="am" scripts="Ethi" territories="ET" />
    <language type="ar" scripts="Arab" territories="AE BH DZ EG IN IQ JO KW LB LY MA OM PS QA SA SD SY TN YE" />
    ...
```

If the language is not a modern language, or the script is not a modern script, or the language not a major language of the territory, then the `alt` attribute is set to secondary.

```xml
    <language type="fr" scripts="Latn" territories="IT US" alt="secondary" />
    ...
```

## 3.1 <a name="Supplemental_Language_Grouping" href="#Supplemental_Language_Grouping">Supplemental Language Grouping</a>

```xml
<!ELEMENT languageGroups ( languageGroup* ) >
<!ELEMENT languageGroup ( #PCDATA ) >
<!ATTLIST languageGroup parent NMTOKEN #REQUIRED >
```

The language groups supply language containment. For example, the following indicates that aav is the Unicode language code for a language group that contains caq, crv, etc.

```xml
<languageGroup parent="fiu">chm et fi fit fkv hu izh kca koi krl kv liv mdf mns mrj myv smi udm vep vot vro</languageGroup>
```

The vast majority of the languageGroup data is extracted from Wikidata, but may be overridden in some cases. The Wikidata information is more fine-grained, but makes use of language groups that don't have ISO or Unicode language codes. Those language groups are omitted from the data. For example, Wikidata has the following child-parent chain: only the first and last elements are present in the language groups.

| Name                      | Wikidata Code                                    | Language Code |
| ------------------------- | ------------------------------------------------ | ------------- |
| Finnish                   | [Q1412](https://www.wikidata.org/wiki/Q1412)     | fi |
| Finnic languages          | [Q33328](https://www.wikidata.org/wiki/Q33328)   |
| Finno-Samic languages     | [Q163652](https://www.wikidata.org/wiki/Q163652) |
| Finno-Volgaic languages   | [Q161236](https://www.wikidata.org/wiki/Q161236) |
| Finno-Permic languages    | [Q161240](https://www.wikidata.org/wiki/Q161240) |
| Finno-Ugric languages     | [Q79890](https://www.wikidata.org/wiki/Q79890)   | fiu |

## 4 <a name="Supplemental_Code_Mapping" href="#Supplemental_Code_Mapping">Supplemental Code Mapping</a>

```xml
<!ELEMENT codeMappings (languageCodes*, territoryCodes*, currencyCodes*) >

<!ELEMENT languageCodes EMPTY >
<!ATTLIST languageCodes type NMTOKEN #REQUIRED>
<!ATTLIST languageCodes alpha3 NMTOKEN #REQUIRED>

<!ELEMENT territoryCodes EMPTY >
<!ATTLIST territoryCodes type NMTOKEN #REQUIRED>
<!ATTLIST territoryCodes numeric NMTOKEN #REQUIRED>
<!ATTLIST territoryCodes alpha3 NMTOKEN #REQUIRED>
<!ATTLIST territoryCodes fips10 NMTOKEN #IMPLIED>
<!ATTLIST territoryCodes internet NMTOKENS #IMPLIED> [deprecated]

<!ELEMENT currencyCodes EMPTY >
<!ATTLIST currencyCodes type NMTOKEN #REQUIRED>
<!ATTLIST currencyCodes numeric NMTOKEN #REQUIRED>
```

The code mapping information provides mappings between the subtags used in the CLDR locale IDs (from BCP 47) and other coding systems or related information. The language codes are only provided for those codes that have two letters in BCP 47 to their ISO three-letter equivalents. The territory codes provide mappings to numeric (UN M.49 [[UNM49](tr35.md#UNM49)] codes, equivalent to ISO numeric codes), ISO three-letter codes, FIPS 10 codes, and the internet top-level domain codes.

The alphabetic codes are only provided where different from the type. For example:

```xml
<territoryCodes type="AA" numeric="958" alpha3="AAA" />
<territoryCodes type="AD" numeric="020" alpha3="AND" fips10="AN" />
<territoryCodes type="AE" numeric="784" alpha3="ARE" />
...
<territoryCodes type="GB" numeric="826" alpha3="GBR" fips10="UK" />
...
<territoryCodes type="QU" numeric="967" alpha3="QUU" internet="EU" />
...
<territoryCodes type="XK" numeric="983" alpha3="XKK" />
...
```

Where there is no corresponding code, sometimes private use codes are used, such as the numeric code for XK.

The currencyCodes are mappings from three letter currency codes to numeric values (ISO 4217 [Current currency & funds code list](https://www.currency-iso.org/en/home/tables/table-a1.html)). The mapping currently covers only current codes and does not include historic currencies. For example:

```xml
<currencyCodes type="AED" numeric="784" />
<currencyCodes type="AFN" numeric="971" />
...
<currencyCodes type="EUR" numeric="978" />
...
<currencyCodes type="ZAR" numeric="710" />
<currencyCodes type="ZMW" numeric="967" />
```

## 5 ~~<a name="Telephone_Code_Data" href="#Telephone_Code_Data">Telephone Code Data</a>~~ (Deprecated)

Deprecated in CLDR v34, and data removed.

```xml
<!ELEMENT telephoneCodeData ( codesByTerritory* ) >

<!ELEMENT codesByTerritory ( telephoneCountryCode+ ) >
<!ATTLIST codesByTerritory territory NMTOKEN #REQUIRED >

<!ELEMENT telephoneCountryCode EMPTY >
<!ATTLIST telephoneCountryCode code NMTOKEN #REQUIRED >
<!ATTLIST telephoneCountryCode from NMTOKEN #IMPLIED >
<!ATTLIST telephoneCountryCode to NMTOKEN #IMPLIED >
```

This data specifies the mapping between ITU telephone country codes [[ITUE164](tr35.md#ITUE164)] and CLDR-style territory codes (ISO 3166 2-letter codes or non-corresponding UN M.49 [[UNM49](tr35.md#UNM49)] 3-digit codes). There are several things to note:

* A given telephone country code may map to multiple CLDR territory codes; +1 (North America Numbering Plan) covers the US and Canada, as well as many islands in the Caribbean and some in the Pacific
* Some telephone country codes are for global services (for example, some satellite services), and thus correspond to territory code 001.
* The mappings change over time (territories move from one telephone code to another). These changes are usually planned several years in advance, and there may be a period during which either telephone code can be used to reach the territory. While the CLDR telephone code data is not intended to include past changes, it is intended to incorporate known information on planned future changes, using `from` and `to` date attributes to indicate when mappings are valid.

A subset of the telephone code data might look like the following (showing a past mapping change to illustrate the from and to attributes):

```xml
<codesByTerritory territory="001">
    <telephoneCountryCode code="800"/> <!-- International Freephone Service -->
    <telephoneCountryCode code="808"/> <!-- International Shared Cost Services (ISCS) -->
    <telephoneCountryCode code="870"/> <!-- Inmarsat Single Number Access Service (SNAC) -->
</codesByTerritory>
<codesByTerritory territory="AS"> <!-- American Samoa -->
    <telephoneCountryCode code="1" from="2004-10-02"/> <!-- +1 684 in North America Numbering Plan -->
    <telephoneCountryCode code="684" to="2005-04-02"/> <!-- +684 now a spare code -->
</codesByTerritory>
<codesByTerritory territory="CA">
    <telephoneCountryCode code="1"/> <!-- North America Numbering Plan -->
</codesByTerritory>
```

## 6 ~~<a name="Postal_Code_Validation" href="#Postal_Code_Validation">Postal Code Validation (Deprecated)</a>~~

Deprecated in v27. Please see other services that are kept up to date, such as:

* [https://i18napis.appspot.com/address/data/US](https://i18napis.appspot.com/address/data/US)
* [https://i18napis.appspot.com/address/data/CH](https://i18napis.appspot.com/address/data/CH)
* ...

```xml
<!ELEMENT postalCodeData (postCodeRegex*) >
<!ELEMENT postCodeRegex (#PCDATA) >
<!ATTLIST postCodeRegex territoryId NMTOKEN #REQUIRED >
```

The Postal Code regex information can be used to validate postal codes used in different countries. In some cases, the regex is quite simple, such as for Germany:

```xml
<postCodeRegex territoryId="DE" >\d{5}</postCodeRegex>
```

The US code is slightly more complicated, since there is an optional portion:

```xml
<postCodeRegex territoryId="US" >\d{5}([ \-]\d{4})?</postCodeRegex>
```

The most complicated currently is the UK.

## 7 <a name="Supplemental_Character_Fallback_Data" href="#Supplemental_Character_Fallback_Data">Supplemental Character Fallback Data</a>

```xml
<!ELEMENT characters ( character-fallback*) >

<!ELEMENT character-fallback ( character* ) >
<!ELEMENT character (substitute*) >
<!ATTLIST character value CDATA #REQUIRED >

<!ELEMENT substitute (#PCDATA) >
```

The `characters` element provides a way for non-Unicode systems, or systems that only support a subset of Unicode characters, to transform CLDR data. It gives a list of characters with alternative values that can be used if the main value is not available. For example:

```xml
<characters>
    <character-fallback>
        <character value="ß">
        <substitute>ss</substitute>
    </character>
    <character value="Ø">
        <substitute>Ö</substitute>
        <substitute>O</substitute>
    </character>
    <character value="₧">
        <substitute>Pts</substitute>
    </character>
    <character value="₣">
        <substitute>Fr.</substitute>
    </character>
    </character-fallback>
</characters>
```

The ordering of the `substitute` elements indicates the preference among them.

That is, this data provides recommended fallbacks for use when a charset or supported repertoire does not contain a desired character. There is more than one possible fallback: the recommended usage is that when a character _value_ is not in the desired repertoire the following process is used, whereby the first value that is wholly in the desired repertoire is used.

* `toNFC`(_value_)
* other canonically equivalent sequences, if there are any
* the explicit _substitutes_ value (in order)
* `toNFKC`(_value_)

## 8 <a name="Coverage_Levels" href="#Coverage_Levels">Coverage Levels</a>

The following describes the structure used to set coverage levels used for CLDR.
That structure is primarily intended for internal use in CLDR tooling — it is not anticipated that users of CLDR data would need it.

Each level adds to what is in the lower level. This list will change between releases of CLDR, and more detailed information for each level is on [Coverage Levels](https://cldr.unicode.org/index/cldr-spec/coverage-levels).


| Level | Description   |     |
| ----: | ------------- | --- |
| 0     | undetermined  | Does not meet any of the following levels. |
| 10    | core          | Core Locale — Has minimal data about the language and writing system that is required before other information can be added using the CLDR survey tool. |
| 40    | basic         | Selectable Locale — Minimal locale data necessary for a "selectable" locale in a platform UI. Very basic number and datetime formatting, etc. |
| 60    | moderate      | Document Content Locale — Minimal locale data for applications such as spreadsheets and word processors to support general document content internationalization: formatting number, datetime, currencies, sorting, plural handling, and so on. |
| 80    | modern        | UI Locale — Contains all fields in normal modern use, including all CLDR locale names, country names, timezone names, currencies in use, and so on. |
| 100   | comprehensive | Above modern level; typically far more data than is needed in practice. |

Levels 40 through 80 are based on the definitions and specifications listed below.

```xml
<!ELEMENT coverageLevels ( approvalRequirements, coverageVariable*, coverageLevel* ) >
<!ELEMENT coverageLevel EMPTY >
<!ATTLIST coverageLevel inLanguage CDATA #IMPLIED >
<!ATTLIST coverageLevel inScript CDATA #IMPLIED >
<!ATTLIST coverageLevel inTerritory CDATA #IMPLIED >
<!ATTLIST coverageLevel value CDATA #REQUIRED >
<!ATTLIST coverageLevel match CDATA #REQUIRED >
```

For example, here is an example coverageLevel line.

```xml
<coverageLevel
    value="30"
    inLanguage="(de|fi)"
    match="localeDisplayNames/types/type[@type='phonebook'][@key='collation']"/>
```

The `coverageLevel` elements are read in order, and the first match results in a coverage level value. The element matches based on the `inLanguage`, `inScript`, `inTerritory`, and `match` attribute values, which are regular expressions. For example, in the above example, a match occurs if the language is de or fi, and if the path is a locale display name for `collation=phonebook`.

The `match` attribute value logically has `//ldml/` prefixed before it is applied. In addition, the `[@` is automatically quoted. Otherwise standard Perl/Java style regular expression syntax is used.

```xml
<!ELEMENT coverageVariable EMPTY >
<!ATTLIST coverageVariable key CDATA #REQUIRED >
<!ATTLIST coverageVariable value CDATA #REQUIRED >
```

The `coverageVariable` element allows us to create variables for certain regular expressions that are used frequently in the coverageLevel definitions above. Each coverage variable must contain a `key` / `value` pair of attributes, which can then be used to be substituted into a coverageLevel definition above.

For example, here is an example coverageLevel line using coverageVariable substitution.

```xml
<coverageVariable key="%dayTypes" value="(sun|mon|tue|wed|thu|fri|sat)">
<coverageVariable key="%wideAbbr" value="(wide|abbreviated)">
<coverageLevel value="20" match="dates/calendars/calendar[@type='gregorian']/days/dayContext[@type='format']/dayWidth[@type='%wideAbbr']/day[@type='%dayTypes']"/>
```

In this example, the coverge variables %dayTypes and %wideAbbr are used to substitute their respective values into the match expression. This allows us to reuse the same variable for other coverageLevel matches that use the same regular expression fragment.

```xml
<!ELEMENT approvalRequirements ( approvalRequirement* ) >
<!ELEMENT approvalRequirement EMPTY >
<!ATTLIST approvalRequirement votes CDATA #REQUIRED >
<!ATTLIST approvalRequirement locales CDATA #REQUIRED >
<!ATTLIST approvalRequirement paths CDATA #REQUIRED >
```

The approvalRequirements allows to specify the number of survey tool votes required for approval, either based on locale, or path, or both. Certain locales require a higher voting threshold (usually 8 votes instead of 4), in order to promote greater stability in the data. Furthermore, certain fields that are very high visibility fields, such as number formats, require a CLDR TC committee member's vote for approval.

`votes=` can be a numeric value, or it can be of the form `=vetter` where `vetter` is one of the `VoteResolver.Level` enumerated values.
It can also be `=LOWER_BAR` (8) or `=HIGH_BAR` (same as `=tc`)  referring to the `VoteResolver` constants of the same names.

Here is an example of the approvalRequirements section.

```xml
<approvalRequirements>
    <!--  "high bar" items -->
    <approvalRequirement votes="=HIGH_BAR" locales="*" paths="//ldml/numbers/symbols[^/]++/(decimal|group)"/>
    <!--  established locales - https://cldr.unicode.org/index/process#h.rm00w9v03ia8 -->
    <approvalRequirement votes="=LOWER_BAR" locales="ar ca cs da de el es fi fr he hi hr hu it ja ko nb nl pl pt pt_PT ro ru sk sl sr sv th tr uk vi zh zh_Hant" paths=""/>
    <!--  all other items -->
    <approvalRequirement votes="=vetter" locales="*" paths=""/>
</approvalRequirements>
```

This section specifies that a TC vote (20 votes) is required for decimal and grouping separators. Furthermore it specifies that any field in the established locales list (i.e. ar, ca, cs, etc.) requires 8 votes, and that all other locales require 4 votes only.

For more information on the CLDR Voting process, see [http://cldr.unicode.org/index/process](http://cldr.unicode.org/index/process)

### 8.1 <a name="Coverage_Level_Definitions" href="#Coverage_Level_Definitions">Definitions</a>
This is a snapshot of the contents of certain variables. The actual definitions in the coverageLevels.xml file may vary from these descriptions.

* _Target-Language_ is the language under consideration.
* _Target-Territories_ is the list of territories found by looking up _Target-Language_ in the `<languageData>` elements in [Supplemental Language Data](tr35-info.md#Supplemental_Language_Data).
* _Language-List_ is _Target-Language_, plus
  * **moderate:** Chinese, English, French, German, Italian, Japanese, Portuguese, Russian, Spanish, Unknown; Arabic, Hindi, Korean, Indonesian, Dutch, Bengali, Turkish, Thai, Polish (de, en, es, fr, it, ja, pt, ru, zh, und, ar, hi, ko, in, nl, bn, tr, th, pl). If an EU language, add the remaining official EU languages.
  * **modern:** all languages that are official or major commercial languages of modern territories
* _Target-Scripts_ is the list of scripts in which _Target-Language_ can be customarily written (found by looking up _Target-Language_ in the `<languageData>` elements in [Supplemental Language Data](tr35-info.md#Supplemental_Language_Data))_,_ plus Unknown (Zzzz)_._
* _Script-List_ is the _Target-Scripts_ plus the major scripts used for multiple languages
  * Latin, Simplified Chinese, Traditional Chinese, Cyrillic, Arabic (Latn, Hans, Hant, Cyrl, Arab)
* _Territory-List_ is the list of territories formed by taking the _Target-Territories_ and adding:
  * **moderate:** Brazil, China, France, Germany, India, Italy, Japan, Russia, United Kingdom, United States, Unknown; Spain, Canada, Korea, Mexico, Australia, Netherlands, Switzerland, Belgium, Sweden, Turkey, Austria, Indonesia, Saudi Arabia, Norway, Denmark, Poland, South Africa, Greece, Finland, Ireland, Portugal, Thailand, Hong Kong SAR China, Taiwan (BR, CN, DE, GB, FR, IN, IT, JP, RU, US, ZZ, ES, BE, SE, TR, AT, ID, SA, NO, DK, PL, ZA, GR, FI, IE, PT, TH, HK, TW). If an EU language, add the remaining member EU countries.
  * **modern:** all current ISO 3166 territories, plus the UN M.49 [[UNM49](tr35.md#UNM49)] regions in [Supplemental Territory Containment](tr35-info.md#Supplemental_Territory_Containment).
* _Currency-List_ is the list of current official currencies used in any of the territories in _Territory-List_, found by looking at the `region` elements in [Supplemental Territory Containment](tr35-info.md#Supplemental_Territory_Containment), plus Unknown (XXX).
* _Calendar-List_ is the set of calendars in customary use in any of _Target-Territories_, plus Gregorian.
* _Number-System-List_ is the set of number systems in customary use in the language.

### 8.2 <a name="Coverage_Level_Data_Requirements" href="#Coverage_Level_Data_Requirements">Data Requirements</a>

The required data to qualify for each level based on these definitions is then the following.

1. localeDisplayNames
   1. _languages:_ localized names for all languages in _Language-List._
   2. _scripts:_ localized names for all scripts in _Script-List_.
   3. _territories:_ localized names for all territories in _Territory-List_.
   4. _variants, keys, types:_ localized names for any in use in _Target-Territories_; for example, a translation for PHONEBOOK in a German locale.

2. dates: all of the following for each calendar in _Calendar-List_.
   1. calendars: localized names
   2. month names, day names, era names, and quarter names
      * context=format and width=narrow, wide, & abbreviated
      * plus context=standAlone and width=narrow, wide, & abbreviated, _if the grammatical forms of these are different than for context=format._
   3. week: minDays, firstDay, weekendStart, weekendEnd
      * if some of these vary in territories in _Territory-List_, include territory locales for those that do.
   4. am, pm, eraNames, eraAbbr
   5. dateFormat, timeFormat: full, long, medium, short
   6. intervalFormatFallback

3. numbers: symbols, decimalFormats, scientificFormats, percentFormats, currencyFormats for each number system in _Number-System-List_.
4. currencies: displayNames and symbol for all currencies in _Currency-List_, for all plural forms
5. transforms: (moderate and above) transliteration between Latin and each other script in _Target-Scripts._

### 8.3 <a name="Coverage_Level_Default_Values" href="#Coverage_Level_Default_Values">Default Values</a>

Items should _only_ be included if they are not the same as the default, which is:

* what is in root, if there is something defined there.
* for timezone IDs: the name computed according to _[Appendix J: Time Zone Display Names](tr35.md#Time_Zone_Fallback)_
* for collation sequence, the UCA DUCET (Default Unicode Collation Element Table), as modified by CLDR.
  * however, in that case the locale must be added to the validSubLocale list in [collation/root.xml](https://github.com/unicode-org/cldr/blob/main/common/collation/root.xml).
* for currency symbol, language, territory, script names, variants, keys, types, the internal code identifiers, for example,
  * currencies: EUR, USD, JPY, ...
  * languages: en, ja, ru, ...
  * territories: GB, JP, FR, ...
  * scripts: Latn, Thai, ...
  * variants: PHONEBOOK, ...

## 9 <a name="Appendix_Supplemental_Metadata" href="#Appendix_Supplemental_Metadata">Supplemental Metadata</a>

Note that this section discusses the `<metadata>` element within the `<supplementalData>` element. For the per-locale metadata used in tests and the Survey Tool, see [10: Locale Metadata Element](#Metadata_Elements).

The supplemental metadata contains information about the CLDR file itself, used to test validity and provide information for locale inheritance. A number of these elements are described in

* Appendix I: [Inheritance and Validity](tr35.md#Inheritance_and_Validity)
* Appendix K: [Valid Attribute Values](tr35.md#Valid_Attribute_Values)
* Appendix L: [Canonical Form](tr35.md#Canonical_Form)
* Appendix M: [Coverage Levels](#Coverage_Levels)

### 9.1 <a name="Supplemental_Alias_Information" href="#Supplemental_Alias_Information">Supplemental Alias Information</a>

```xml
<!ELEMENT alias (languageAlias*,scriptAlias*,territoryAlias*,subdivisionAlias*,variantAlias*,zoneAlias*) >
```

_The following are common attributes for subelements of `<alias>`:_

```xml
<!ELEMENT *Alias EMPTY >
<!ATTLIST *Alias type NMTOKEN #IMPLIED >
<!ATTLIST *Alias replacement NMTOKEN #IMPLIED >
<!ATTLIST *Alias reason ( deprecated | overlong ) #IMPLIED >
```

_The `languageAlias` has additional reasons_

```xml
<!ATTLIST languageAlias reason ( deprecated | overlong | macrolanguage | legacy | bibliographic ) #IMPLIED >
```

This element provides information as to parts of locale IDs that should be substituted when accessing CLDR data. This logical substitution should be done to both the locale id, and to any lookup for display names of languages, territories, and so on. The replacement for the language and territory types is more complicated: see _Part 1: [Core](tr35.md#Contents), Section 3.3.1 [BCP 47 Language Tag Conversion](tr35.md#BCP_47_Language_Tag_Conversion)_ for details.

```xml
<alias>
    <languageAlias type="in" replacement="id">
    <languageAlias type="sh" replacement="sr">
    <languageAlias type="sh_YU" replacement="sr_Latn_YU">
    ...
    <territoryAlias type="BU" replacement="MM">
    ...
</alias>
```

Attribute values for the \*Alias values include the following:

###### Table: <a name="Alias_Attribute_Values" href="#Alias_Attribute_Values">Alias Attribute Values</a>

| Attribute   | Value         | Description |
| ----------- | ------------- | ----------- |
| type        | NMTOKEN       | The code to be replaced |
| replacement | NMTOKEN       | The code(s) to replace it, space-delimited. |
| reason      | deprecated    | The code in type is deprecated, such as 'iw' by 'he', or 'CS' by 'RS ME'. |
|             | overlong      | The code in type is too long, such as 'eng' by 'en' or 'USA' or '840' by 'US' |
|             | macrolanguage | The code in type is an encompassed language that is replaced by a macrolanguage, such as '[arb'](https://iso639-3.sil.org/code/arb) by 'ar'. |
|             | legacy        | The code in type is a legacy code that is replaced by another code for compatibility with established legacy usage, such as 'sh' by 'sr_Latn' |
|             | bibliographic | The code in type is a [bibliographic code](https://www.loc.gov/standards/iso639-2/langhome.html), which is replaced by a terminology code, such as 'alb' by 'sq'. |

### 9.2 ~~<a name="Supplemental_Deprecated_Information" href="#Supplemental_Deprecated_Information">Supplemental Deprecated Information (Deprecated)</a>~~

```xml
<!ELEMENT deprecated ( deprecatedItems* ) >
<!ATTLIST deprecated draft ( approved | contributed | provisional | unconfirmed | true | false ) #IMPLIED > <!-- true and false are deprecated. -->

<!ELEMENT deprecatedItems EMPTY >
<!ATTLIST deprecatedItems type ( standard | supplemental | ldml | supplementalData | ldmlBCP47 ) #IMPLIED > <!-- standard | supplemental are deprecated -->
<!ATTLIST deprecatedItems elements NMTOKENS #IMPLIED >
<!ATTLIST deprecatedItems attributes NMTOKENS #IMPLIED >
<!ATTLIST deprecatedItems values CDATA #IMPLIED >
```

The `deprecatedItems` element was used to indicate elements, attributes, and attribute values that are deprecated. This means that the items are valid, but that their usage is strongly discouraged. This element and its subelements have been deprecated in favor of [DTD Annotations](tr35.md#DTD_Annotations).

Where particular values are deprecated (such as territory codes like SU for Soviet Union), the names for such codes may be removed from the common/main translated data after some period of time. However, typically supplemental information for deprecated codes is retained, such as containment, likely subtags, older currency codes usage, etc. The English name may also be retained, for debugging purposes.

### 9.3 <a name="Default_Content" href="#Default_Content">Default Content</a>

```xml
<!ELEMENT defaultContent EMPTY >
<!ATTLIST defaultContent locales NMTOKENS #IMPLIED >
```

In CLDR, locales without territory information (or where needed, script information) provide data appropriate for what is called the _default content locale_. For example, the _en_ locale contains data appropriate for _en-US_, while the _zh_ locale contains content for _zh-Hans-CN_, and the _zh-Hant_ locale contains content for _zh-Hant-TW_. The default content locales themselves thus inherit all of their contents, and are empty.

The choice of content is typically based on the largest literate population of the possible choices. Thus if an implementation only provides the base language (such as _en_), it will still get a complete and consistent set of data appropriate for a locale which is reasonably likely to be the one meant. Where other information is available, such as independent country information, that information can always be used to pick a different locale (such as _en-CA_ for a website targeted at Canadian users).

If an implementation is to use a different default locale, then the data needs to be _pivoted_; all of the data from the CLDR for the current default locale pushed out to the locales that inherit from it, then the new default content locale's data moved into the base. There are tools in CLDR to perform this operation.

For the relationship between Inheritance, DefaultContent, LikelySubtags, and LocaleMatching, see **_Section 4.2.6 [Inheritance vs Related Information](tr35.md#Inheritance_vs_Related)_**.

## 10 <a name="Metadata_Elements" href="#Metadata_Elements">Locale Metadata Elements</a>

Note: This section refers to the per-locale `<metadata>` element, containing metadata about a particular locale. This is in contrast to the [_Supplemental_ Metadata](#Appendix_Supplemental_Metadata), which is in the supplemental tree and is not specific to a locale.

```xml
<!ELEMENT metadata ( alias | ( casingData?, special* ) ) >
<!ELEMENT casingData ( alias | ( casingItem*, special* ) ) >
<!ELEMENT casingItem ( #PCDATA ) >
<!ATTLIST casingItem type CDATA #REQUIRED >
<!ATTLIST casingItem override (true | false) #IMPLIED >
<!ATTLIST casingItem forceError (true | false) #IMPLIED >
```

The `<metadata>` element contains metadata about the locale for use by the Survey Tool or other tools in checking locale data; this data is not intended for export as part of the locale itself.

The `<casingItem>` element specifies the capitalization intended for the majority of the data in a given category with the locale. The purpose is so that warnings can be issued to translators that anything deviating from that capitalization should be carefully reviewed. Its `type` attribute has one of the values used for the `<contextTransformUsage>` element above, with the exception of the special value "all"; its value is one of the following:

* lowercase
* titlecase

The `<casingItem>` data is generated by a tool based on the data available in CLDR. In cases where the generated casing information is incorrect and needs to be manually edited, the `override` attribute is set to `true` so that the tool will not override the manual edits. When the casing information is known to be both correct and something that should apply to all elements of the specified type in a given locale, the `forceErr` attribute may be set to `true` to force an error instead of a warning for items that do not match the casing information.

## 11 <a name="Version_Information" href="#Version_Information">Version Information</a>

```xml
<!ELEMENT version EMPTY >
<!ATTLIST version cldrVersion CDATA #FIXED "27" >
<!ATTLIST version unicodeVersion CDATA #FIXED "7.0.0" >
```

The `cldrVersion` attribute defines the CLDR version for this data, as published on [CLDR Releases/Downloads](http://cldr.unicode.org/index/downloads).

The `unicodeVersion` attribute defines the version of the Unicode standard that is used to interpret data. Specifically, some data elements such as exemplar characters are expressed in terms of UnicodeSets. Since UnicodeSets can be expressed in terms of Unicode properties, their meaning depends on the Unicode version from which property values are derived.

## 12 <a name="Parent_Locales" href="#Parent_Locales">Parent Locales</a>

The parentLocales data is supplemental data, but is described in detail in the [core specification section 4.1.3.](tr35.md#Parent_Locales)

## 13 <a name="Unit_Conversion" href="#Unit_Conversion">Unit Conversion</a>

The unit conversion data ([units.xml](https://github.com/unicode-org/cldr/blob/main/common/supplemental/units.xml)) provides the data for converting all of the cldr unit identifiers to base units, and back. That allows conversion between any two convertible units, such as two units of length. For any two convertible units (such as acre and dunum) the first can be converted to the base unit (square-meter), then that base unit can be converted to the second unit.

Many of the elements allow for a common @description attribute, to disambiguate the main attribute value or to explain the choice of other values. For example:
```xml
<unitConstant constant="glucose_molar_mass" value="180.1557"
  description="derivation from the mean atomic weights according to STANDARD ATOMIC WEIGHTS 2019 on https://ciaaw.org/atomic-weights.htm"/>
```

```xml
<!ELEMENT unitConstants ( unitConstant* ) >

<!ELEMENT unitConstant EMPTY >

<!ATTLIST unitConstant constant NMTOKEN #REQUIRED >

<!ATTLIST unitConstant value CDATA #REQUIRED >

<!ATTLIST unitConstant status NMTOKEN #IMPLIED >

<!ATTLIST unitConstant description CDATA #IMPLIED >
```

### Constants

The data uses a small set of constants for readability, such as:

```xml
<unitConstant constant="ft_to_m" value="0.3048" />
<unitConstant constant="ft2_to_m2" value="ft_to_m*ft_to_m" />
```
The order of the elements in the file is significant.

Each constant can have a value based on simple expressions using numbers, previous constants, plus the operators * and /. Parentheses are not allowed. The operator * binds more tightly than /, which may be unexpected. Thus a * b / c * d is interpreted as (a * b) / (c * d). A consequence of that is that a * b / c * d = a * b / c / d. In the value, the numbers represent rational values. So 0.3048 is interpreted as exactly 3048 / 10000.

In the above case, ft2-to-m2 is a conversion constant for going from square feet to square meters. The expression evaluates to 0.09290304. Where the constants cannot be expressed as rationals, or where their interpretation is fluid, that is marked with a status value:

```xml
<unitConstant constant="PI" value="411557987 / 131002976" status='approximate' />
```

In such cases, software may decide to use different values for accuracy.

An implementation need not use rationals directly for conversion; it could use doubles, for example, if only double accuracy is needed.

### Conversion Data

```xml
<!ELEMENT convertUnits ( convertUnit* ) >

<!ELEMENT convertUnit EMPTY >

<!ATTLIST convertUnit source NMTOKEN #REQUIRED >

<!ATTLIST convertUnit baseUnit NMTOKEN #REQUIRED >

<!ATTLIST convertUnit factor CDATA #IMPLIED >

<!ATTLIST convertUnit offset CDATA #IMPLIED >

<!ATTLIST convertUnit description CDATA #IMPLIED >

<!ATTLIST convertUnit systems NMTOKENS #IMPLIED >
```

The conversion data provides the data for converting all of the cldr unit identifiers to base units, and back. That allows conversion between any two convertible units, such as two units of length. For any two convertible units (such as acre and dunum) the first can be converted to the base unit (square-meter), then that base unit can be converted to the second unit.

The data is expressed as conversions to the base unit. The information can also be used for the conversion back.

Examples:

```xml
<convertUnit source='carat' baseUnit='kilogram' factor='0.0002'/>

<convertUnit source='gram' baseUnit='kilogram' factor='0.001'/>

<convertUnit source='ounce' baseUnit='kilogram' factor='lb_to_kg/16' systems="ussystem uksystem"/>

<convertUnit source='fahrenheit' baseUnit='kelvin' factor='5/9' offset='2298.35/9' systems="ussystem uksystem"/>
```

For example, to convert from 3 carats to kilograms, the factor 0.0002 is used, resulting in 0.0006. To convert between carats and ounces, first the carets are converted to kilograms, then the kilograms to ounces (by reversing the mapping).

The factor and offset use the same structure as in the value in unitConstant; in particular, * binds more tightly than /.

The conversion may also require an offset, such as the following:

```xml
<convertUnit source='fahrenheit' baseUnit='kelvin' factor='5/9' offset='2298.35/9' systems="ussystem uksystem"/>
```

The factor and offset can be simple expressions, just like the values in the unitConstants.

Where a factor is not present, the value is 1; where an offset is not present, the value is 0.

The `systems` attribute indicates the measurement system(s). Multiple values may be given; for example, _minute_ is marked as systems="metric ussystem uksystem"

Attribute Value | Description
------------ | -------------
_si_ | the _International System of Units (SI)_
_metric_ | a superset of the _si_ units, with some non-SI units accepted for use with the SI or simple multiples of metric units, such as pound-metric (= ½ kilogram)
_ussystem_ | the inch-pound system as used in the US, also called _US Customary Units_
_uksystem_ | the inch-pound system as used in the UK, also called _British Imperial Units_, differing mostly in units of volume

CLDR follows conversion values where possible from:
* [NIST Special Publication 1038](https://www.govinfo.gov/content/pkg/GOVPUB-C13-f10c2ff9e7af2091314396a2d53213e4/pdf/GOVPUB-C13-f10c2ff9e7af2091314396a2d53213e4.pdf)
* [International Astronomical Union General Assembly](https://arxiv.org/pdf/1510.07674.pdf)

See also [NIST Guide to the SI, Chapter 4: The Two Classes of SI Units and the SI Prefixes](https://www.nist.gov/pml/special-publication-811/nist-guide-si-chapter-4-two-classes-si-units-and-si-prefixes)

For complex units, such as _pound-force-per-square-inch_, the conversions are computed by combining the conversions of each of the simple units: _pound-force_ and _inch_. Because the conversions in convertUnit are reversible, the computation can go from complex source unit to complex base unit to complex target units.

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

#### Exceptional Cases

##### Identities

For completeness, identity mappings are also provided for the base units themselves, such as:

```xml
<convertUnit source='meter' baseUnit='meter' />
```

##### Aliases

In a few instances the old identifiers are deprecated in favor of regular syntax. Implementations should handle both on input:

```xml
<unitAlias type="meter-per-second-squared" replacement="meter-per-square-second" reason="deprecated"/>
<unitAlias type="liter-per-100kilometers" replacement="liter-per-100-kilometer" reason="deprecated"/>
<unitAlias type="pound-foot" replacement="pound-force-foot" reason="deprecated"/>
<unitAlias type="pound-per-square-inch" replacement="pound-force-per-square-inch" reason="deprecated"/>
```

These use the standard alias elements in XML, and are also included in the [units.xml](https://github.com/unicode-org/cldr/blob/main/common/supplemental/units.xml) file.

##### “Duplicate” Units

Some CLDR units are provided simply because they have different names in some languages. For example, year and year-person, or foodcalorie and kilocalorie. One CLDR unit is not convertible (temperature-generic), it is only used for the translation (where the exact unit would be understood by context).

##### Discarding Offsets

The temperature units are special. When they represent a scale, they have an offset. But where they represent an amount, such as in complex units, they do not. So celsius-per-second is the same as kelvin-per-second.

#### Unresolved Units

Some SI units contain the same units in the numerator and denominator, so those cannot be resolved. For example, if cubic-meter-per-meter were always resolved, then _consumption_ (like “liter-per-kilometer”) could not be distinguished from _area_ (square-meter).

However, in conversion, it may be necessary to resolve them in order to find a match. For example, kilowatt-hour maps to the base unit kilogram-square-meter-second-per-cubic-second, but that needs to be resolved to kilogram-square-meter-per-square-second in order matched against an _energy._

## Quantities and Base Units

```xml
<!ELEMENT unitQuantities ( unitQuantity* ) >

<!ELEMENT unitQuantity EMPTY >

<!ATTLIST unitQuantity baseUnit NMTOKEN #REQUIRED >

<!ATTLIST unitQuantity quantity NMTOKENS #REQUIRED >

<!ATTLIST unitQuantity status NMTOKEN #IMPLIED >

<!ATTLIST unitQuantity description CDATA #IMPLIED >
```

Conversion is supported between comparable units. Those can be simple units, such as length, or more complex ‘derived’ units that are built up from _base units_. The `<unitQuantities>` element provides information on the base units used for conversion. It also supplies information about their _quantity_: mass, length, time, etc., and whether they are simple or not.

Examples:

```xml
<unitQuantity baseUnit='kilogram' quantity='mass' status='simple'/>
<unitQuantity baseUnit='meter-per-second' quantity='speed'/>
```

The order of the elements in the file is significant, since it is used in [Unit_Identifier_Normalization](#Unit_Identifier_Normalization).

The quantity values themselves are informative. Therer mayreflecting that _force per area_ can be referenced as either _pressure_ or _stress_, for example). The quantity for a complex unit that has a reciprocal is formed by prepending “inverse-” to the quantity, such as _inverse-consumption._

The base units for the quantities and the quantities themselves are based on [NIST Special Publication 811](https://www.nist.gov/pml/special-publication-811) and the earlier [NIST Special Publication 1038](https://www.govinfo.gov/content/pkg/GOVPUB-C13-f10c2ff9e7af2091314396a2d53213e4/pdf/GOVPUB-C13-f10c2ff9e7af2091314396a2d53213e4.pdf). In some cases, a different unit is chosen for the base. For example, a _revolution_ (360°) is chosen for the base unit for angles instead of the SI _radian_, and _item_ instead of the SI _mole_. Additional base units are added where necessary, such as _bit_ and _pixel_.

This data is not necessary for conversion, but is needed for [Unit_Identifier_Normalization](#Unit_Identifier_Normalization). Some of the `unitQuantity` elements are not needed to convert CLDR units, but are included for completeness. Example:

```xml
<unitQuantity baseUnit='ampere-per-square-meter' quantity='current-density'/>
```

### UnitType vs Quantity

The unitType (as in “length-meter”) is not the same as the quantity. It is often broader: for example, the unitType _electric_ corresponds to the quantities _electric-current, electric-resistance,_ and _voltage_. The unitType itself is also informative, and can be dropped from a long unit identifier to get a still-unique short unit identifier.

### <a name="Unit_Identifier_Normalization" href="#Unit_Identifier_Normalization">Unit Identifier Normalization</a>

There are many possible ways to construct complex units. For comparison of unit identifiers, an implementation can normalize in the following way:

1. Convert all but the first -per- to simple multiplication. The result then has the format of /numerator ( -per- denominator)?/
   * foot-per-second-per-second ⇒ foot-per-second-second
2. Within each of the numerator and denominator:
3. Convert multiple instances of a unit into the appropriate power.
   * foot-per-second-second ⇒ foot-per-square-second
   * kilogram-meter-kilogram ⇒ meter-square-kilogram
4. For each single unit, disregarding prefixes and powers, get the order of the _simple_ unit among the `unitQuantity` elements in the [units.xml](https://github.com/unicode-org/cldr/blob/main/common/supplemental/units.xml). Sort the single units by that order, using a stable sort. If there are private-use single units, sort them after all the non-private use single units.
   * meter-square-kilogram => square-kilogram-meter
   * meter-square-gram ⇒ square-gram-meter
5. As an edge case, there could be two adjacent single units with the same _simple_ unit but different prefixes, such as _meter-kilometer_. In that case, sort the larger prefixes first, such as _kilometer-meter_ or _kibibyte-kilobyte_.
6. Within private-use single units, sort by the simple unit alphabetically.

The examples in #4 are due to the following ordering of the `unitQuantity` elements:

```xml
1.  <unitQuantity baseUnit='candela' quantity='luminous-intensity' status='simple'/>
2.  <unitQuantity baseUnit='kilogram' quantity='mass' status='simple'/>
3.  <unitQuantity baseUnit='meter' quantity='length' status='simple'/>
4.  …
```

## Mixed Units

Mixed units, or unit sequences, are units with the same base unit which are listed in sequence. Common examples are feet and inches, meters and centimeters, and hours, minutes, and seconds. Mixed unit identifiers are expressed using the "-and-" infix, as in "foot-and-inch", "meter-and-centimeter", and "hour-and-minute-and-second".

Scalar values for mixed units are expressed in the largest unit, according to the sort order discussed above in "Normalization". For example, numbers for "foot-and-inch" are expressed in feet.

Mixed units are expected to be rendered in the order of the tokens in the identifier. For example, the value 1.25 with the identifier "foot-and-inch" should be rendered as "1 foot and 3 inches" and 1.25 inch-and-foot should be rendered as “3 inches and 1 foot". **NOTE:** the correct application of this may require adding locales to the regions attribute set.

## Testing

The [unitsTest.txt](https://github.com/unicode-org/cldr/blob/main/common/testData/units/unitsTest.txt) file supplies a list of all the CLDR units with conversions, for testing implementations. Instructions for use are supplied in the header of the file.

## 14 <a name="Unit_Preferences" href="#Unit_Preferences">Unit Preferences</a>

Different locales have different preferences for which unit or combination of units is used for a particular usage, such as measuring a person’s height. This is more fine-grained than merely a preference for metric versus US or UK measurement systems. For example, one locale may use meters alone, while another may use centimeters alone or a combination of meters and centimeters; a third may use inches alone, or (informally) a combination of feet and inches.

The CLDR data is intended to map from a particular usage — e.g. measuring the height of a person or the fuel consumption of an automobile — to the unit or combination of units typically used for that usage in a given region. Considerations for such a mapping include:

* The list of possible usages large and open-ended. The intent here is to start with a small set for which there is an urgent need, and expand as necessary.
* Even for a given usage such a measuring a road distance, there are multiple ranges in use. For example, one set of units may be used for indicating the distance to the next city (kilometers or miles), while another may be used for indicating the distance to the next exit (meters, yards, or feet).
* There are also differences between more formal usage (official signage, medical records) and more informal usage (conversation, texting).
* For some usages, the measurement may be expressed using a sequence of units, such as “1 meter, 78 centimeters” or “12 stone, 2 pounds”.

The DTD structure is as follows:

```xml
<!ELEMENT unitPreferenceData ( unitPreferences* ) >

<!ELEMENT unitPreferences ( unitPreference* ) >
<!ATTLIST unitPreferences category NMTOKEN #REQUIRED >
<!ATTLIST unitPreferences usage NMTOKENS #REQUIRED >

<!ELEMENT unitPreference ( #PCDATA ) >
<!ATTLIST unitPreference regions NMTOKENS #REQUIRED >
<!ATTLIST unitPreference geq NMTOKEN #IMPLIED >
<!ATTLIST unitPreference skeleton CDATA #IMPLIED >
```

<table><tbody>
<tr><td>category</td><td>A unit quantity, such as “area” or “length”. See Section 13 Unit Conversion</td></tr>
<tr><td>usage</td><td>A type of usage, such as person-height.</td></tr>
<tr><td>regions</td><td>One or more region identifiers (macroregions or regions), subdivision identifiers, or language identifiers, such as 001, US, usca, and de-CH.</td></tr>
<tr><td>geq</td><td>A threshold value, in a unit determined by the unitPreference element value. The unitPreference element is only used for values higher than this value (and lower than any higher value).<br/>The value must be non-negative. For picking negative units (-3 meters), use the absolute value to pick the unit.</td></tr>
<tr><td>skeleton</td><td>A skeleton in the ICU number format syntax, that can be used to format unit</td></tr>
</tbody></table>

**Note:** As of CLDR 37, the `<unitPreference>` `geq` attribute replaces the now-deprecated `<unitPreferences>` `scope` attribute.

Example:

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

The intended usage is to take the measure to be formatted, and the desired category, usage, and region and find the best match as follows.

* First, see if there is an exact match, producing a list of one or more `unitPreference` elements. For example, length/road/GB has a match above, giving

  ```xml
  <unitPreference regions="GB" geq="0.5">mile</unitPreference>
  <unitPreference regions="GB" geq="100.0" skeleton="precision-increment/50">yard</unitPreference>
  <unitPreference regions="GB">yard</unitPreference>
  ```

* If there is no match for the category, then the data is not available.
* Otherwise, given the category:
  * If there is an exact match for the usage, but not for the region, try region="001".
* The specification allows for [containment regions](https://unicode-org.github.io/cldr-staging/charts/38/supplemental/territory_containment_un_m_49.html), [region subdivisions](https://unicode-org.github.io/cldr-staging/charts/38/supplemental/territory_subdivisions.html).
* While in version 37 only 001 is used, in the future the data may contain others.
* The fallback is: subdivision2 ⇒ subdivision1 ⇒ region/country ⇒ subcontinent ⇒ continent ⇒ world
* Example:

  | Region/subdivision | Code  |
  | ------------------ | ----- |
  | Blackpool          | gbbpl |
  | England            | gbeng |
  | United Kingdom     | GB    |
  | Northern Europe    | 154   |
  | Europe             | 150   |
  | World              | 001   |

* If there is an exact match for the region, but not for the usage,
  * If the usage has multiple parts (eg land-agriculture-grain) drop the last part (eg land-agriculture)
  * Repeat dropping the last part and trying the result (eg land)
  * If you eliminate all of them, try usage="default".
  * If there is no exact match for either one, try usage="default", region="001". That will always match.

Once you have a list of `unitPreference` elements, find the applicable unitPreference. For a given category, usage, and set of regions (eg “US GB”), the units are ordered from largest to smallest.

* The geq item gives the value for the unit in the element value (or for the largest unit for mixed units). For example,
  * `...geq="0.5">mile<...` means 0.9 kilometers
  * `...geq="100.0">foot:inch<...` means 100 feet
* If there is no `geq` attribute, then the implicit value is 1.0.
* Implementations will probably convert the values into the base units, so that the comparison is fast. Thus the above would be converted internally to something like:
  * ≥ 804.672 meters ⇒ mile
  * ≥ 30.48 meters ⇒ foot:inch
* Search for the first matching unitPreference for the input measure. If there is no match (eg < 100 feet in the above example), take the last unitPreference. That is, the last unitPreference is effectively geq="0"

For completeness, when comparing doubles to the geq values:
* Negative numbers are treated as if they were positive.
* _infinity_ is treated as being the largest possible value.
* NaN is treated as the smallest possible value.

Once a matching `unitPreference` element is found:

* The unit is the element value
* The skeleton (if there is one) supplies formatting information for the unit. API settings may allow that to be overridden.
  * The syntax and semantics for the skeleton value are defined by the [ICU Number Skeletons](https://unicode-org.github.io/icu/userguide/format_parse/numbers/skeletons.html) document.
* If the unit is mixed (eg foot:inch) the skeleton applies to the final subunit; the higher subunits are formatted as integers.
* If the skeleton is missing, the default is skeleton="**precision-integer/@@\***". However, the client can also override or tune the number formatting.

### Constraints

* For a given category, there is always a “default” usage.
* For a given category, and usage:
  * There is always a 001 region.
  * None of the sets of regions can overlap. That is, you can’t have “US” on one line and “US GB” on another. You _can_ have two lines with “US”, for different sizes of units.
* For a given category, usage, and region-set
  * The unitPreferences are in descending order.

### Caveats

The extended unit support is still being developed further. See the Known Issues on the release page for futher information.

* * *

Copyright © 2001–2022 Unicode, Inc. All Rights Reserved. The Unicode Consortium makes no expressed or implied warranty of any kind, and assumes no liability for errors or omissions. No liability is assumed for incidental and consequential damages in connection with or arising out of the use of the information or programs contained or accompanying this technical report. The Unicode [Terms of Use](https://unicode.org/copyright.html) apply.

Unicode and the Unicode logo are trademarks of Unicode, Inc., and are registered in some jurisdictions.

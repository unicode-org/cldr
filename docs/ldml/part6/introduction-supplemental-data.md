## <a name="Supplemental_Data" id="Supplemental_Data" href="#Supplemental_Data">Introduction Supplemental Data</a>

* The following represents the format for additional supplemental information. This is information that is important for internationalization and proper use of CLDR, but is not contained in the locale hierarchy. It is not localizable, nor is it overridden by locale data. The current CLDR data can be viewed in the [Supplemental Charts](https://www.unicode.org/cldr/charts/latest/supplemental/index.html).


```dtd
<!ELEMENT supplementalData (version, generation?, cldrVersion?, currencyData?, territoryContainment?, subdivisionContainment?, languageData?, territoryInfo?, postalCodeData?, calendarData?, calendarPreferenceData?, weekData?, timeData?, measurementData?, unitPreferenceData?, timezoneData?, characters?, transforms?, metadata?, codeMappings?, parentLocales?, likelySubtags?, metazoneInfo?, plurals?, telephoneCodeData?, numberingSystems?, bcp47KeywordMappings?, gender?, references?, languageMatching?, dayPeriodRuleSet*, metaZones?, primaryZones?, windowsZones?, coverageLevels?, idValidity?, rgScope?) >
```

* The data in CLDR is presently split into multiple files: supplementalData.xml, supplementalMetadata.xml, characters.xml, likelySubtags.xml, ordinals.xml, plurals.xml, telephoneCodeData.xml, genderList.xml, plus transforms (see _Part 2 [Transforms](tr35-general.md#Transforms)_ and _Part 2 [Transform Rule Syntax](tr35-general.md#Transform_Rules_Syntax)_). The split is just for convenience: logically, they are treated as though they were a single file. Future versions of CLDR may split the data in a different fashion. Do not depend on any specific XML filename or path for supplemental data.


* Note that [Chapter 10](#Metadata_Elements) presents information about metadata that is maintained on a per-locale basis. It is included in this section because it is not intended to be used as part of the locale itself.



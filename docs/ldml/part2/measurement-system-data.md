## <a name="Measurement_System_Data" id="Measurement_System_Data" href="#Measurement_System_Data">Measurement System Data</a>

```dtd
<!ELEMENT measurementData ( measurementSystem*, paperSize* ) >

<!ELEMENT measurementSystem EMPTY >
<!ATTLIST measurementSystem type ( metric | US | UK ) #REQUIRED >
<!ATTLIST measurementSystem category ( temperature ) #IMPLIED >
<!ATTLIST measurementSystem territories NMTOKENS #REQUIRED >

<!ELEMENT paperSize EMPTY >
<!ATTLIST paperSize type ( A4 | US-Letter ) #REQUIRED >
<!ATTLIST paperSize territories NMTOKENS #REQUIRED >
```

The measurement system is the normal measurement system in common everyday use (except for date/time). For example:

```xml
<measurementData>
 <measurementSystem type="metric" territories="001" />
 <measurementSystem type="US" territories="LR MM US" />
 <measurementSystem type="metric" category="temperature" territories="LR MM" />
 <measurementSystem type="US" category="temperature" territories="BS BZ KY PR PW" />
 <measurementSystem type="UK" territories="GB" />
 <paperSize type="A4" territories="001" />
 <paperSize type="US-Letter" territories="BZ CA CL CO CR GT MX NI PA PH PR SV US VE" />
</measurementData>
```

The values are "metric", "US", or "UK"; others may be added over time.

* The "metric" value indicates the use of SI [[ISO1000](tr35.md#ISO1000)] base or derived units, or non-SI units accepted for use with SI: for example, meters, kilograms, liters, and degrees Celsius.
* The "US" value indicates the customary system of measurement as used in the United States: feet, inches, pints, quarts, degrees Fahrenheit, and so on.
* The "UK" value indicates the mix of metric units and Imperial units (feet, inches, pints, quarts, and so on) used in the United Kingdom, in which Imperial volume units such as pint, quart, and gallon are different sizes than in the "US" customary system. For more detail about specific units for various usages, see **Part 6: Supplemental:** _[Preferred Units for Specific Usages](tr35-info.md#Preferred_Units_For_Usage)_.

* In some cases, it may be common to use different measurement systems for different categories of measurements. For example, the following indicates that for the category of temperature, in the regions LR and MM, it is more common to use metric units than US units.


```xml
<measurementSystem type="metric" category="temperature" territories="LR MM"/>
```

The `paperSize` attribute gives the height and width of paper used for normal business letters. The values are "A4" and "US-Letter".

* For both `measurementSystem` entries and `paperSize` entries, later entries for specific territories such as "US" will override the value assigned to that territory by earlier entries for more inclusive territories such as "001".


The measurement information was formerly in the main LDML file, and had a somewhat different format.

Again, for finer-grained detail about specific units for various usages, see **Part 6: Supplemental:** _[Preferred Units for Specific Usages](tr35-info.md#Preferred_Units_For_Usage)_.

### <a name="Measurement_Elements" id="Measurement_Elements" href="#Measurement_Elements">Measurement Elements (deprecated)</a>

```dtd
<!ELEMENT measurement (alias | (measurementSystem?, paperSize?, special*)) >
```

* The `measurement` element is deprecated in the main LDML files, because the data is more appropriately organized as connected to territories, not to linguistic data. Instead, the `measurementData` element in the supplemental data file should be used.



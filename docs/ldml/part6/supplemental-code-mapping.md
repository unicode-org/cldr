## <a name="Supplemental_Code_Mapping" id="Supplemental_Code_Mapping" href="#Supplemental_Code_Mapping">Supplemental Code Mapping</a>

```dtd
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

* The code mapping information provides mappings between the subtags used in the CLDR locale IDs (from BCP 47) and other coding systems or related information. The language codes are only provided for those codes that have two letters in BCP 47 to their ISO three-letter equivalents. The territory codes provide mappings to numeric (UN M.49 [[UNM49](tr35.md#UNM49)] codes, equivalent to ISO numeric codes), ISO three-letter codes, FIPS 10 codes, and the internet top-level domain codes.


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

* The currencyCodes are mappings from three letter currency codes to numeric values (ISO 4217, see [Current currency & funds code list](https://www.six-group.com/en/products-services/financial-information/data-standards.html#scrollTo=maintenance-agency)). The mapping currently covers only current codes and does not include historic currencies. For example:


```xml
<currencyCodes type="AED" numeric="784" />
<currencyCodes type="AFN" numeric="971" />
...
<currencyCodes type="EUR" numeric="978" />
...
<currencyCodes type="ZAR" numeric="710" />
<currencyCodes type="ZMW" numeric="967" />
```


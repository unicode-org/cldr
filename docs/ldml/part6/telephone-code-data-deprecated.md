## <a name="Telephone_Code_Data" id="Telephone_Code_Data" href="#Telephone_Code_Data">~~Telephone Code Data~~ (Deprecated)</a>

Deprecated in CLDR v34, and data removed.
The data and structure for phone numbers changes quite often, so the recommended alternative is the open-source library [libphonenumber](https://github.com/google/libphonenumber#what-is-it).

```dtd
<!ELEMENT telephoneCodeData ( codesByTerritory* ) >

<!ELEMENT codesByTerritory ( telephoneCountryCode+ ) >
<!ATTLIST codesByTerritory territory NMTOKEN #REQUIRED >

<!ELEMENT telephoneCountryCode EMPTY >
<!ATTLIST telephoneCountryCode code NMTOKEN #REQUIRED >
<!ATTLIST telephoneCountryCode from NMTOKEN #IMPLIED >
<!ATTLIST telephoneCountryCode to NMTOKEN #IMPLIED >
```

* This data specifies the mapping between ITU telephone country codes [[ITUE164](tr35.md#ITUE164)] and CLDR-style territory codes (ISO 3166 2-letter codes or non-corresponding UN M.49 [[UNM49](tr35.md#UNM49)] 3-digit codes). There are several things to note:


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


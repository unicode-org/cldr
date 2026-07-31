## <a name="Appendix_Supplemental_Metadata" id="Appendix_Supplemental_Metadata" href="#Appendix_Supplemental_Metadata">Supplemental Metadata</a>

* Note that this section discusses the `<metadata>` element within the `<supplementalData>` element. For the per-locale metadata used in tests and the Survey Tool, see [10: Locale Metadata Element](#Metadata_Elements).


The supplemental metadata contains information about the CLDR file itself, used to test validity and provide information for locale inheritance. A number of these elements are described in

* Appendix I: [Inheritance and Validity](tr35.md#Inheritance_and_Validity)
* Appendix K: [Valid Attribute Values](tr35.md#Valid_Attribute_Values)
* Appendix L: [Canonical Form](tr35.md#Canonical_Form)
* Appendix M: [Coverage Levels](#Coverage_Levels)

### <a name="Supplemental_Alias_Information" id="Supplemental_Alias_Information" href="#Supplemental_Alias_Information">Supplemental Alias Information</a>

```dtd
<!ELEMENT alias (languageAlias*,scriptAlias*,territoryAlias*,subdivisionAlias*,variantAlias*,zoneAlias*) >
```

_The following are common attributes for subelements of `<alias>`:_

```dtd
<!ELEMENT *Alias EMPTY >
<!ATTLIST *Alias type NMTOKEN #IMPLIED >
<!ATTLIST *Alias replacement NMTOKEN #IMPLIED >
<!ATTLIST *Alias reason ( deprecated | overlong ) #IMPLIED >
```

_The `languageAlias` has additional reasons_

```dtd
<!ATTLIST languageAlias reason ( deprecated | overlong | macrolanguage | legacy | bibliographic ) #IMPLIED >
```

* This element provides information as to parts of locale IDs that should be substituted when accessing CLDR data. This logical substitution should be done to both the locale id, and to any lookup for display names of languages, territories, and so on. The replacement for the language and territory types is more complicated: see _Part 1: [Core](tr35.md#Contents), [BCP 47 Language Tag Conversion](tr35.md#BCP_47_Language_Tag_Conversion)_ for details.


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

###### <a name="Alias_Attribute_Values" id="Alias_Attribute_Values" href="#Alias_Attribute_Values">Table: Alias Attribute Values</a>

| Attribute   | Value         | Description |
| ----------- | ------------- | ----------- |
| type        | NMTOKEN       | The code to be replaced |
| replacement | NMTOKEN       | The code(s) to replace it, space-delimited. |
| reason      | deprecated    | The code in type is deprecated, such as 'iw' by 'he', or 'CS' by 'RS ME'. |
|             | overlong      | The code in type is too long, such as 'eng' by 'en' or 'USA' or '840' by 'US' |
|             | macrolanguage | The code in type is an encompassed language that is replaced by a macrolanguage, such as '[arb'](https://iso639-3.sil.org/code/arb) by 'ar'. |
|             | legacy        | The code in type is a legacy code that is replaced by another code for compatibility with established legacy usage, such as 'sh' by 'sr_Latn' |
|             | bibliographic | The code in type is a [bibliographic code](https://www.loc.gov/standards/iso639-2/langhome.html), which is replaced by a terminology code, such as 'alb' by 'sq'. |

### <a name="Supplemental_Deprecated_Information" id="Supplemental_Deprecated_Information" href="#Supplemental_Deprecated_Information">~~Supplemental Deprecated Information (Deprecated)~~</a>

```dtd
<!ELEMENT deprecated ( deprecatedItems* ) >
<!ATTLIST deprecated draft ( approved | contributed | provisional | unconfirmed | true | false ) #IMPLIED > <!-- true and false are deprecated. -->

<!ELEMENT deprecatedItems EMPTY >
<!ATTLIST deprecatedItems type ( standard | supplemental | ldml | supplementalData | ldmlBCP47 ) #IMPLIED > <!-- standard | supplemental are deprecated -->
<!ATTLIST deprecatedItems elements NMTOKENS #IMPLIED >
<!ATTLIST deprecatedItems attributes NMTOKENS #IMPLIED >
<!ATTLIST deprecatedItems values CDATA #IMPLIED >
```

* The `deprecatedItems` element was used to indicate elements, attributes, and attribute values that are deprecated. This means that the items are valid, but that their usage is strongly discouraged. This element and its subelements have been deprecated in favor of [DTD Annotations](tr35.md#DTD_Annotations).


* Where particular values are deprecated (such as territory codes like SU for Soviet Union), the names for such codes may be removed from the common/main translated data after some period of time. However, typically supplemental information for deprecated codes is retained, such as containment, likely subtags, older currency codes usage, etc. The English name may also be retained, for debugging purposes.


### <a name="Default_Content" id="Default_Content" href="#Default_Content">Default Content</a>

```dtd
<!ELEMENT defaultContent EMPTY >
<!ATTLIST defaultContent locales NMTOKENS #IMPLIED >
```

* In CLDR, locales without territory information (or where needed, script information) provide data appropriate for what is called the _default content locale_. For example, the _en_ locale contains data appropriate for _en-US_, while the _zh_ locale contains content for _zh-Hans-CN_, and the _zh-Hant_ locale contains content for _zh-Hant-TW_. The default content locales themselves thus inherit all of their contents, and are empty.


* The choice of content is typically based on the largest literate population of the possible choices. Thus if an implementation only provides the base language (such as _en_), it will still get a complete and consistent set of data appropriate for a locale which is reasonably likely to be the one meant. Where other information is available, such as independent country information, that information can always be used to pick a different locale (such as _en-CA_ for a website targeted at Canadian users).


* If an implementation is to use a different default locale, then the data needs to be _pivoted_; all of the data from the CLDR for the current default locale pushed out to the locales that inherit from it, then the new default content locale's data moved into the base. There are tools in CLDR to perform this operation.


For the relationship between Inheritance, DefaultContent, LikelySubtags, and LocaleMatching, see **_[Inheritance vs Related Information](tr35.md#Inheritance_vs_Related)_**.


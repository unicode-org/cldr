## <a name="Supplemental_Language_Data" id="Supplemental_Language_Data" href="#Supplemental_Language_Data">Supplemental Language Data</a>

```dtd
<!ELEMENT languageData ( language* ) >
<!ELEMENT language EMPTY >
<!ATTLIST language type NMTOKEN #REQUIRED >
<!ATTLIST language scripts NMTOKENS #IMPLIED >
<!ATTLIST language variants NMTOKENS #IMPLIED >
<!ATTLIST language alt NMTOKENS #IMPLIED >
```

The language data is used for consistency checking and testing. It provides a list of which languages are used with which scripts.
Formerly a `territory` attribute (deprecated in CLDR 48) also provided a list of territories in which the language was used; however
that has been superseded by the data in _[Supplemental Territory Information](#Supplemental_Territory_Information)_ .

```xml
<languageData>
    <language type="af" scripts="Latn"/>
    <language type="am" scripts="Ethi"/>
    <language type="ar" scripts="Arab"/>
    ...
```

If the language is not a modern language, or the script is not a modern script, then the `alt` attribute is set to secondary.

```xml
    <language type="ar" scripts="Syrc" alt="secondary"/>
    ...
```


## <a name="Version_Information" id="Version_Information" href="#Version_Information">Version Information</a>

```dtd
<!ELEMENT version EMPTY >
<!ATTLIST version cldrVersion CDATA #FIXED "27" >
<!ATTLIST version unicodeVersion CDATA #FIXED "7.0.0" >
```

The `cldrVersion` attribute defines the CLDR version for this data, as published on [CLDR Releases/Downloads](https://cldr.unicode.org/index/downloads).

* The `unicodeVersion` attribute defines the version of the Unicode standard that is used to interpret data. Specifically, some data elements such as exemplar characters are expressed in terms of UnicodeSets. Since UnicodeSets can be expressed in terms of Unicode properties, their meaning depends on the Unicode version from which property values are derived.



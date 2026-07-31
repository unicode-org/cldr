## <a name="Postal_Code_Validation" id="Postal_Code_Validation" href="#Postal_Code_Validation">~~Postal Code Validation (Deprecated)~~</a>

Deprecated in v27. Please see other services that are kept up to date, such as <https://github.com/google/libaddressinput>

```dtd
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


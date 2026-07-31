## <a name="Supplemental_Character_Fallback_Data" id="Supplemental_Character_Fallback_Data" href="#Supplemental_Character_Fallback_Data">Supplemental Character Fallback Data</a>

```dtd
<!ELEMENT characters ( character-fallback*) >

<!ELEMENT character-fallback ( character* ) >
<!ELEMENT character (substitute*) >
<!ATTLIST character value CDATA #REQUIRED >

<!ELEMENT substitute (#PCDATA) >
```

* The `characters` element provides a way for non-Unicode systems, or systems that only support a subset of Unicode characters, to transform CLDR data. It gives a list of characters with alternative values that can be used if the main value is not available. For example:


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

* That is, this data provides recommended fallbacks for use when a charset or supported repertoire does not contain a desired character. There is more than one possible fallback: the recommended usage is that when a character _value_ is not in the desired repertoire the following process is used, whereby the first value that is wholly in the desired repertoire is used.


* `toNFC`(_value_)
* other canonically equivalent sequences, if there are any
* the explicit _substitutes_ value (in order)
* `toNFKC`(_value_)


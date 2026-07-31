## <a name="Sample_Name" id="Sample_Name" href="#Sample_Name">Sample Name</a>

* The sampleName element is used for test names in the personNames LDML data for each locale to aid in testing and display in the CLDR Survey Tool. They are not intended to be used in production software as prompts or placeholders in a user interface and should not be displayed in a user interface.


### <a name="Syntax" id="Syntax" href="#Syntax">Syntax</a>

```dtd
<!ELEMENT sampleName ( nameField+ )  >
<!ATTLIST sampleName item NMTOKEN #REQUIRED >
```

* `NMTOKEN` must be one of `( nativeG, nativeGS, nativeGGS, nativeFull, foreignG, foreignGS, foreignGGS, foreignFull )`. However, these may change arbitrarily in the future.

### <a name="Expected_values" id="Expected_values" href="#Expected_values">Expected values</a>

The item values starting with "native" are expected to be native names, in native script.
The item values starting with "foreign" are expected to be foreign names, in native script.
There are no foreign names or native names in a foreign script, because those should be handled by a different locale's data.

The rest of the item value indicates how many fields are present.
For the expected sample name items, assume a name such as Mr. Richard “Rich” Edward Smith Iglesias Ph.D.

* `G` is for an example name with only the given is presented: “Richard” or “Rich” (informal)
* `GS` is for an example name with only the given name and surname: “Richard Smith” or “Rich Smith” (informal)
* `GSS` is for an example using both given and given2 names and a surname: “Richard Edward Smith” and “Rich E. Smith” (informal)
* `Full` is used to present a name using all possible fields: “Mr. Richard Edward Smith Iglesias, Ph.D.”

The `nameField` values and their modifiers are described in the [Person Name Object](#person-name-object) and [namePattern Syntax](#namepattern-syntax) sections.


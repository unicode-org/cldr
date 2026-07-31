## <a name="XML_Structure" id="XML_Structure" href="#XML_Structure">XML Structure</a>

* Person name formatting data is stored as LDML with schema defined as follows. Each element has a brief description of the usage, but the exact algorithms for using these elements are provided in [Formatting Process](#formatting-process).



### <a name="personNames_Element" id="personNames_Element" href="#personNames_Element">personNames Element</a>

```dtd
<!ELEMENT personNames ( nameOrderLocales*, parameterDefault*, nativeSpaceReplacement*, foreignSpaceReplacement*, initialPattern*, personName*, sampleName* ) >
```

The LDML top-level `<personNames>` element contains information regarding the formatting of person names, and the formatting of person names in specific contexts for a specific locale.

### <a name="personName_Element" id="personName_Element" href="#personName_Element">personName Element</a>

The `<personName>` element contains the format patterns, or `<namePattern>` elements, for a specific context and is described in [[namePattern Syntax](#namepattern-syntax)]

The `<namePattern>` syntax is described in [[Person Name Format Patterns](#formatting-process)].

```dtd
<!ELEMENT personName ( namePattern+ ) >
<!ATTLIST personName order NMTOKEN #IMPLIED >
```

* `NMTOKEN` is one of `( surnameFirst | givenFirst | sorting )`

```dtd
<!ATTLIST personName length NMTOKEN #IMPLIED >
```

* `NMTOKEN` is one of `( long | medium | short )`

```dtd
<!ATTLIST personName usage NMTOKEN #IMPLIED >
```

* `NMTOKEN` is one of `( addressing | referring | monogram )`

```dtd
<!ATTLIST personName formality NMTOKEN #IMPLIED >
```

* `NMTOKEN` is one of `( formal | informal )`

The `<personName>` element has attributes of `order`, `length`, `usage`, and `formality`, and contains one or more `<namePattern>` elements.

A missing attribute matches all valid values for that attribute. For example, if `formality=...` is missing, it is equivalent to multiple lines, one for each possible `formality` attribute.

```dtd
<!ELEMENT namePattern ( #PCDATA ) >
```

A `namePattern` contains a list of PersonName fields enclosed in curly braces, separated by literals, such as:

> ``<namePattern>`{surname}, {given} {given2}`</namePattern>``

which produces output like _“Smith, Robert James”_. See [[namePattern Syntax](#namepattern-syntax)] for more details.

### <a name="nameOrderLocales_Element" id="nameOrderLocales_Element" href="#nameOrderLocales_Element">nameOrderLocales Element</a>

* **The `` element**: The `<nameOrderLocales>` element is optional, and contains information about selecting patterns based on the locale of a passed in PersonName object to determine the order of elements in a formatted name. For more information see [[NameOrder](#derive-the-name-order)]. It has a structure as follows:


```dtd
<!ELEMENT nameOrderLocales `( #PCDATA )`>
<!ATTLIST nameOrderLocales order ( givenFirst | surnameFirst ) #REQUIRED >
```

* `#PCDATA `is a space delimited list of one or more [unicode_locale_id](tr35.md#unicode_locale_id)s. Normally each locale is limited to language, script, and region. The _und_ locale ID may only occur once, either in _surnameFirst_ or _givenFirst_, but not both, and matches all base locales not explicitly listed.

An example from English may look like the following

> ``<nameOrderLocales order="givenFirst">`und en`</nameOrderLocales>``<br/>
> ``<nameOrderLocales order="surnameFirst">`ko vi yue zh`</nameOrderLocales>``

* This would tell the formatting code, when handling person name data from an English locale, to use patterns with the `givenFirst` order attribute for all data except name data from Korean, Vietnamese, Cantonese, and Chinese locales, where the `surnameFirst` patterns should be used.


### <a name="parameterDefault_Element" id="parameterDefault_Element" href="#parameterDefault_Element">parameterDefault Element</a>
```dtd
<!ELEMENT parameterDefault ( #PCDATA ) >
<!ATTLIST parameterDefault parameter (length | formality) #REQUIRED >
```
* Many clients of the person-names functionality don’t really care about formal versus informal; they just want whatever the “normal” formality level is for the user’s language. The same goes for the default length.


This parameter provides that information, so that APIs can allow users to use default values for the formality and length. The exact form that this takes depends on the API conventions, of course.

### <a name="foreignSpaceReplacement_Element" id="foreignSpaceReplacement_Element" href="#foreignSpaceReplacement_Element">foreignSpaceReplacement Element</a>

* **The `` element**: The `<foreignSpaceReplacement>` element is used to specify how spaces should be handled when the name language is **different from** the formatting language. It is used in languages that don't normally require spaces between words. For example, Japanese and Chinese have the value of a middle dot (‘·’ U+00B7 MIDDLE DOT or ‘・’ U+30FB KATAKANA MIDDLE DOT), so that it is used between words in a foreign name; most other languages have the value of SPACE.


```dtd
<!ELEMENT foreignSpaceReplacement ( #PCDATA ) >
<!ATTLIST foreignSpaceReplacement xml:space preserve #REQUIRED >
```

* `xml:space` must be set to `'preserve'` so that actual spaces in the pattern are preserved. See [W3C XML White Space Handling](https://www.w3.org/TR/xml/#sec-white-space).
* The `#PCDATA `is the character sequence used to replace spaces when postprocessing a pattern.

### <a name="nativeSpaceReplacement_Element" id="nativeSpaceReplacement_Element" href="#nativeSpaceReplacement_Element">nativeSpaceReplacement Element</a>

* **The `` element**: The `<nativeSpaceReplacement>` element is used to specify how spaces should be handled when the name language is **the same as** the formatting language. It is used in languages that don't normally require spaces between words, but may use spaces within names. For example, Japanese and Chinese have the value of an empty string between words in a native name; most other languages have the value of SPACE.


```dtd
<!ELEMENT nativeSpaceReplacement ( #PCDATA ) >
<!ATTLIST nativeSpaceReplacement xml:space preserve #REQUIRED >
```

* `xml:space` must be set to `'preserve'` so that actual spaces in the pattern are preserved. See [W3C XML White Space Handling](https://www.w3.org/TR/xml/#sec-white-space).
* The `#PCDATA `is the character sequence used to replace spaces when postprocessing a pattern.

### <a name="initialPattern_Element" id="initialPattern_Element" href="#initialPattern_Element">initialPattern Element</a>

The `<initialPattern>` element is used to specify how to format initials of name parts.

**_initial_** is a pattern used to display a single initial in the locale, while **_initialSequence_** is a pattern used to “glue” together multiple initials for multiword fields, for example with the given name “Mary Beth” in English.

#### <a name="Syntax" id="Syntax" href="#Syntax">Syntax</a>

```dtd
<!ELEMENT initialPattern ( #PCDATA ) >
<!ATTLIST initialPattern type ( initial | initialSequence) #REQUIRED >
```

The `type="initial"` is used to specify the pattern for how single initials are created, for example “Wolcott” => “W.” would have an entry of

> ``<initialPattern type="initial">`{0}.`</initialPattern>``

`type="initialSequence`” is used to specify how a series of initials should appear, for example “Wolcott Janus” => “W. J.”, with spaces between each initial, would have a specifier of

> ``<initialPattern type="initialSequence">`{0} {1}`</initialPattern>``


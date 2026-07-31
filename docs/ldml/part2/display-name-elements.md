## <a name="Display_Name_Elements" id="Display_Name_Elements" href="#Display_Name_Elements">Display Name Elements</a>

```dtd
<!ELEMENT localeDisplayNames ( alias | ( localeDisplayPattern?, languages?, scripts?, territories?, subdivisions?, variants?, keys?, types?, transformNames?, measurementSystemNames?, codePatterns?, special* ) )>
```

* Display names for scripts, languages, countries, currencies, and variants in this locale are supplied by this element. They supply localized names for these items for use in user-interfaces for various purposes such as displaying menu lists, displaying a language name in a dialog, and so on. Capitalization should follow the conventions used in the middle of running text; the `<contextTransforms>` element may be used to specify the appropriate capitalization for other contexts (see _[ContextTransform Elements](#Context_Transform_Elements)_). Examples are given below.


> **Note:** The "en" locale may contain translated names for deprecated codes for debugging purposes. Translation of deprecated codes into other languages is discouraged.

* Where present, the display names must be unique; that is, two distinct codes would not get the same display name. (There is one exception to this: in time zones, where parsing results would give the same GMT offset, the standard and daylight display names can be the same across different time zone IDs.)


Any translations should follow customary practice for the locale in question. For more information, see [[Data Formats](tr35.md#DataFormats)].

```xml
<localeDisplayPattern>
```
```dtd
<!ELEMENT localeDisplayPattern ( alias | (localePattern*, localeSeparator*, localeKeyTypePattern*, special*) ) >
```

* For compound language (locale) IDs such as "pt_BR" which contain additional subtags beyond the initial language code: When the `<languages>` data does not explicitly specify a display name such as "Brazilian Portuguese" for a given compound language ID, "Portuguese (Brazil)" from the display names of the subtags.


It includes three sub-elements:

*   The `<localePattern>` element specifies a pattern such as "{0} ({1})" in which {0} is replaced by the display name for the primary language subtag and {1} is replaced by a list of the display names for the remaining subtags.
*   The `<localeSeparator>` element specifies a pattern such as "{0}, {1}" used when appending a subtag display name to the list in the `<localePattern>` subpattern {1} above. If that list includes more than one display name, then `<localeSeparator>` subpattern {1} represents a new display name to be appended to the current list in {0}. _Note: Before CLDR 24, the `<localeSeparator>` element specified a separator string such as ", ", not a pattern._
*   The `<localeKeyTypePattern>` element specifies the pattern used to display key-type pairs, such as "{0}: {1}"

* For example, for the locale identifier zh_Hant_CN_co_pinyin_cu_USD, the display would be "Chinese (Traditional, China, Pinyin Sort Order, Currency: USD)". The key-type for co_pinyin doesn't use the localeKeyTypePattern because there is a translation for the key-type in English:


```xml
<type type="pinyin" key="collation">Pinyin Sort Order</type>
```

The `language` element has the additional `alt="menu"` option, that allows for related languages to be sorted together.

```xml
<language type="yue" alt="menu">Chinese, Cantonese</language>
<language type="zh" alt="menu">Chinese, Mandarin</language>
```
However, when `localePattern`s are used, the names start to get complicated. There is an additional `menu` attribute, with two values: `core` and `extension`.For example:

```xml
<language type="ckb">Central Kurdish</language>
<language type="ckb" menu="core">Kurdish</language>
<language type="ckb" menu="extension">Central</language>
…
<language type="ku">Kurdish</language>
<language type="ku" menu="core">Kurdish</language>
<language type="ku" menu="extension">Kurmanji</language>
…
<language type="sdh">Southern Kurdish</language>
<language type="sdh" menu="core">Kurdish</language>
<language type="sdh" menu="extension">Southern</language>
```

The core part can be used as the language name, with the extension going into the `localePattern`, such as in the following illustration of part of a menu:

| Language |
| ---- |
| … |
| Kashmiri |
| Kurdish (Kurmanji, Latin) |
| Kurdish (Central, Arabic) |
| Kurdish (Southern, Arabic) |
| Kyrgyz |
| … |

### <a name="locale_display_name_algorithm" id="locale_display_name_algorithm" href="#locale_display_name_algorithm">Locale Display Name Algorithm</a>

A locale display name LDN is generated for a locale identifier L in the following way.
1. Convert the locale identifier to *canonical syntax* per **[Part 1, Canonical Unicode Locale Identifiers](tr35.md#Canonical_Unicode_Locale_Identifiers)**.
That will put the subtags in a defined order, and replace aliases by their canonical counterparts. (That defined order is followed in the processing below.)
2. Build a base name LDN from the language, possibly also some other subtags, taking into account the parameters listed below.
    * The language name uses the longest match, dropping all fields that match. For example:
        * With L = "nl_Cyrl_BE", if there is a `<language type="nl_BE">`Flemish`</language>`, the language name is set to "Flemish", and the "BE" is ignored in step 4.
        * With L = "ca_fonipa_valencia", if there is a `<language type="ca_valencia">`Valencian`</language>`, the language name is set to "Valencian", and the subtag "valencia" is ignored in step 4.
4. Build a list of qualifying strings LQS.
    1. For each remaining subtag language identifier (script, region, or variant):
        1. Where there is a match for a subtag, disregard that subtag from L and add the name of the subtag to LDN or LQS as described below.
        2. If there is no match for a subtag, use the fallback pattern with the subtag instead.
    2. For any remaining `-u` or `t` key-value pairs, there are two options (based on the parameters; the first is the default)
        1. `WholeKeyValue`: Add the formatted key-value, OR
        2. `SeparateKeyValue` Add a string created from the formatted key and the formatted value using `scope="core"`
5. Once LDN and LQS are built, return the following based on the length of LQS.

| Length | Processing |
| :---- | :---- |
| 0 | return LDN |
| 1 | use the \<localePattern\> to compose the result LDN from LDN and LQS\[0\], and return it. |
| \>1 | use the \<localeSeparator\> element value to join the elements of the list into LDN2, then use the \<localePattern\> to compose the result LDN from LDN and LDN2, and return it. |

The processing can be controlled via the following parameters (the names of the parameters are only illustrative):

*   `CombineLanguage`: boolean
    *   Example: the `CombineLanguage = true`, picking the bold value below.
    *   `<language type="nl">`Dutch`</language>`
    *   **``<language type="nl_BE">`Flemish`</language>``**
*   `PreferAlt`: map from element to preferred alt value, picking the bold value below.
    *   Example: the `PreferAlt` contains `{"language"="short"}`:
    *   `<language type="az">`Azerbaijani`</language>`
    *   **``<language type="az" alt="short">`Azeri`</language>``**
*  `CoreAndExtension`: if there is a `menu="core"` and a `menu="extension"` value:
    1.  Use the `menu=core` variant for the name in question.
    2.  Add the `menu=extension` variant to the head of the LQS before it is formatted.
*  `WholeKeyValue`: for `-u` or `t` key-value pairs
    1.  Format with combined key-value, if available; otherwise format with `SeparateKeyValue`
        *  For example, using `…_ca_buddhist`
        *  `<type key="calendar" type="buddhist">`Buddhist Calendar`</type>`
		* ⇒ "Buddhist Calendar"
*  `SeparateKeyValue`: for `-u` or `t` key-value pairs
    1.  Format with separate key and value using `scope="core"`, if available; otherwise format with `WholeKeyValue`
        *  For example, using `…_ca_buddhist`
         * `<key type="calendar">`Calendar`</key>` +
         * `<type key="calendar" type="buddhist" scope="core">`Buddhist`</type>` +
         * `<localeKeyTypePattern>`{0}: {1}`</localeKeyTypePattern>`
		 * ⇒ "Calendar: Buddhist"

* In addition, the input locale display name could be minimized (see [Part 1: Likely Subtags](tr35.md#Likely_Subtags)) before generating the LDN. Selective minimization is often the best choice. For example, in a menu list it is often clearer to show the region if there are any regional variants. Thus the user would just see \["Spanish"\] for es if the latter is the only supported Spanish, but where es-MX is also listed, then see \["Spanish (Spain)", "Spanish (Mexico)"\].


* The key-type `scope="core"` is also useful in menus. For example, if a menu or pull-down is offering different choices of calendars, it is cleaner to use the key value for the name of the menu (eg, "Calendar"), and use the `scope="core"` values for the choices. Thus:


| Calendar |
| ---- |
| Buddhist |
| Chinese |
| Gregorian |
| Hijri |

* * *

**Processing types of locale identifier subtags**

* When both the subtag display name and the \<localePattern\> contain bracket characters, replace the brackets in the subtag display name with their nested bracket equivalents according to the [Nested Bracket Replacement](#Character_Nested_Bracket_Replacement) data.


1.  **Language.** Match the L subtags against the type values in the `<language>` elements. Pick the element with the most subtags matching. If there is more than one such element, pick the one that has subtypes matching earlier. If there are two such elements, pick the one that is alphabetically less. If there is no match, then further convert L to *canonical form* per **[Part 1, Canonical Unicode Locale Identifiers](tr35.md#Canonical_Unicode_Locale_Identifiers)** and try the preceding steps again. Set LBN to the selected value. Disregard any of the matching subtags in the following processing.
    *   If CombineLanguage is false, only choose matches with the language subtag matching.
2.  **Script, Region, Variants.** Where any of these subtags are in L, append the matching element value to LQS.
3.  **U extensions.** If there is an attribute value A, process the key-value pair <"u", A> as below and append to LQS. Then format and add display names for each of the remaining key-type pairs as described below.
4.  **T extensions.** Get the value of the `key="h0" type="hybrid"` element, if there is one; otherwise the value of the `<key type="t">` element. Next get the locale display name of the tlang. Do not use `<localePattern>`; instead, append the subtag display names directly to the LQS. Then format and add display names to LQS for any of the remaining tkey-tvalue pairs as described below.
5.  **Other extensions.** There are currently no such extensions defined. Until such time as there are formats defined for them, append each of the extensions’ subtags to LQS.
6.  **Private Use extensions.** Get the value

**Formatting T/U Key-Value pairs as display names**

1.  If there is a match for the key/value, then append the element value and return.
2.  Otherwise, get the display name for the key, using the subtag if not available.
3.  Format special values. As usual, if lacking data, use the subtag(s).
    1.  key="kr": (REORDER_CODE) assume the value is a script code, and get its display name.
    2.  key="dx": (SCRIPT_CODE) assume the value is a script code, and get its display name.
    3.  key="vt": (CODEPOINTS, deprecated) the value is a list of code points. Set the value display name to it, after replacing \[-\_\] by space.
    4.  key="x0": (PRIVATE_USE) the value is a list of subtags. No formatting available, so use the subtag(s).
    5.  key="sd": (SUBDIVISION_CODE) use the subdivision data to find the display name.
    6.  key="rg": (RG_KEY_VALUE): handle as with key="sd"
4.  Then use the value of the `<localeKeyTypePattern>` element to join the key display name and the value display name, and append the result to LQS.

**Examples of English locale display names**

| Locale identifier             | Locale display name |
| ----------------------------- | ------------------- |
| es                            | Spanish |
| es-419                        | Spanish (Latin America) |
| es-Cyrl-MX                    | Spanish (Cyrillic, Mexico) |
| en-Latn-GB-fonipa-scouse      | English (Latin, United Kingdom, IPA Phonetics, Scouse) |
| en-u-nu-thai-ca-islamic-civil | English (Calendar: islamic-civil, Thai Digits) |
| hi-u-nu-latn-t-en-h0-hybrid   | Hindi (Western Digits, Hybrid: English) |
| en-u-nu-deva-t-de-mm-fonipa   | English (Devanagari Digits, Transform: German, Myanmar \[Burma\], IPA Phonetics) |
| fr-z-zz-zzz-v-vv-vvv-u-uu-uuu-t-ru-Cyrl-s-ss-sss-a-aa-aaa-x-u-x | French (uu: uuu, Transform: Russian, Cyrillic, a: aa-aaa, s: ss-sss, v: vv-vvv, x: u-x, z: zz-zzz) |



### <a name="locale_display_name_fields" id="locale_display_name_fields" href="#locale_display_name_fields">Locale Display Name Fields</a>

```xml
<languages>
```

* This contains a list of elements that provide the user-translated names for language codes, as described in _[Unicode Language and Locale Identifiers](tr35.md#Unicode_Language_and_Locale_Identifiers)_.


```xml
<language type="ab">Abkhazian</language>
<language type="aa">Afar</language>
<language type="af">Afrikaans</language>
<language type="sq">Albanian</language>
```

* There should be no expectation that the list of languages with translated names be complete: there are thousands of languages that could have translated names. For debugging purposes or comparison, when a language display name is missing, the Description field of the language subtag registry can be used to supply a fallback English user-readable name.


* The type can actually be any locale ID as specified above. The set of which locale IDs is not fixed, and depends on the locale. For example, in one language one could translate the following locale IDs, and in another, fall back on the normal composition.


| type | translation | composition |
| --- | --- | --- |
| nl_BE | Flemish | Dutch (Belgium) |
| zh_Hans | Simplified Chinese | Chinese (Simplified) |
| en_GB | British English | English (United Kingdom) |

Thus when a complete locale ID is formed by composition, the longest match in the language type is used, and the remaining fields (if any) added using composition.

Alternate short forms may be provided for some languages (and for territories and other display names), for example.

```xml
<language type="az">Azerbaijani</language>
<language type="az" alt="short">Azeri</language>
<language type="en_GB">British English</language>
<language type="en_GB" alt="short">U.K. English</language>
<language type="en_US">American English</language>
<language type="en_US" alt="short">U.S. English</language>
```

* * *

```xml
<scripts>
```

* This element can contain a number of `script` elements. Each `script` element provides the localized name for a script code, as described in _[Unicode Language and Locale Identifiers](tr35.md#Unicode_Language_and_Locale_Identifiers)_ (see also _UAX #24: Script Names_ [[UAX24](https://www.unicode.org/reports/tr41/#UAX24)]). For example, in the language of this locale, the name for the Latin script might be "Romana", and for the Cyrillic script is "Kyrillica". That would be expressed with the following.


```xml
<script type="Latn">Romana</script>
<script type="Cyrl">Kyrillica</script>
```

* The script names are most commonly used in conjunction with a language name, using the `<localePattern>` combining pattern, and the default form of the script name should be suitable for such use. When a script name requires a different form for stand-alone use, this can be specified using the "stand-alone" alternate:


```xml
<script type="Hans">Simplified</script>
<script type="Hans" alt="stand-alone">Simplified Han</script>
<script type="Hant">Traditional</script>
<script type="Hant" alt="stand-alone">Traditional Han</script>
```

This will produce results such as the following:

* Display name of language + script, using `<localePattern>`: “Chinese (Simplified)”
* Display name of script alone, using `<localePattern>`: “Simplified Han”

* * *

```xml
<territories>
```

* This contains a list of elements that provide the user-translated names for territory codes, as described in _[Unicode Language and Locale Identifiers](tr35.md#Unicode_Language_and_Locale_Identifiers)_.


```xml
<territory type="AD">Andorra</territory>
<territory type="AF">Afghanistan</territory>
<territory type="AL">Albania</territory>
<territory type="AO">Angola</territory>
<territory type="DZ">Algeria</territory>
<territory type="GB">United Kingdom</territory>
<territory type="GB" alt="short">U.K.</territory>
<territory type="US">United States</territory>
<territory type="US" alt="short">U.S.</territory>
```

Notes:
* Territory names may not match the official name of the territory, and the English or French names may not match those in ISO 3166. Reasons for this include:
    * CLDR favors customary names in common parlance, not necessarily the official names.
    * CLDR endeavors to provide names that are not too long, in order to avoid problems with truncation or overflow in user interfaces.
* In general the territory names should also match those used in currency names, see **Part 3** _[Currencies](tr35-numbers.md#Currencies)_.

* * *

```xml
<variants>
```

* This contains a list of elements that provide the user-translated names for the _variant_code_ values described in _[Unicode Language and Locale Identifiers](tr35.md#Unicode_Language_and_Locale_Identifiers)_.


```xml
<variant type="nynorsk">Nynorsk</variant>
```

* * *

```xml
<keys>
```

This contains a list of elements that provide the user-translated names for the _key_ values described in _[Unicode Language and Locale Identifiers](tr35.md#Unicode_Language_and_Locale_Identifiers)_.

```xml
<key type="collation">Sortierung</key>
```

Note that the `type` values may use aliases. Thus if the locale u-extension key "co" does not match, then the aliases have to be tried, using the bcp47 XML data:

```xml
<key name="co" description="…" alias="collation">
```

* * *

```xml
<types>
```

* This contains a list of elements that provide the user-translated names for the _type_ values described in _[Unicode Language and Locale Identifiers](tr35.md#Unicode_Language_and_Locale_Identifiers)_. Since the translation of an option name may depend on the _key_ it is used with, the latter is optionally supplied.


```xml
<type type="phonebook" key="collation">Telefonbuch</type>
```

Note that the `key` and `type` values may use aliases. Thus if the locale u-extension key "co" does not match, then the aliases have to be tried, using the bcp47 XML data.

```xml
<key name="co" description="…" alias="collation">

<type name="phonebk" description="…" alias="phonebook"/>
```

These elements are not present in root.xml. If they are missing in a locale, fall back to the key or value identifier.

* * *

```xml
<measurementSystemNames>
```

This contains a list of elements that provide the user-translated names for systems of measurement. The types currently supported are "US", "metric", and "UK".

```xml
<measurementSystemName type="US">U.S.</type>
```

**Note:** In the future, we may need to add display names for the particular measurement units (millimeter versus millimetre versus whatever the Greek, Russian, etc are), and a message format for positioning those with respect to numbers. For example, "\{number} \{unitName}" in some languages, but "\{unitName} \{number}" in others.

* * *

```xml
<transformNames>
```

```xml
<transformName type="Numeric">Numeric</type>
```

* * *

```xml
<codePatterns>
```

```xml
<codePattern type="language">Language: {0}</type>
```

* * *

```dtd
<!ELEMENT subdivisions ( alias | ( subdivision | special )* ) >
<!ELEMENT subdivision ( #PCDATA )>
```

Note that the subdivision names are in separate files, in the subdivisions/ directory. The type values are the fully qualified subdivision names. For example:

```xml
<subdivision type="AL-04">Fier County</subdivision>
<subdivision type="AL-FR">Fier</subdivision> <!-- in AL-04 : Fier County -->
<subdivision type="AL-LU">Lushnjë</subdivision> <!-- in AL-04 : Fier County -->
<subdivision type="AL-MK">Mallakastër</subdivision> <!-- in AL-04 : Fier County -->
```

See also **Part 6** _[Subdivision Containment](tr35-info.md#Subdivision_Containment)_.

### <a name="Type_Values" id="Type_Values" href="#Type_Values">Type Values</a>

Many BCP47 types have boolean values, such as `ka` (`colAlternate`).
Rather than track translations for each of these separately, the `<typeValues>` element provides a centralized translation for this particular purpose.

```xml
<localeDisplayNames>
    <typeValues>
        <typeValue type="no">Off</typeValue>
        <typeValue type="yes">On</typeValue>
    </typeValues>
</localeDisplayNames>
```

These are intended to be used with key type names such as:

* Ignore Symbols Sorting: **On**
* Reversed Accent Sorting: **Off**
* Uppercase/Lowercase Ordering: **On**

These two strings are not inflected.


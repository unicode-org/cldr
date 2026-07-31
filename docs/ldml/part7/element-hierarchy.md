## <a name="Element_Hierarchy" id="Element_Hierarchy" href="#Element_Hierarchy">Element Hierarchy</a>

This section describes the XML elements in a keyboard layout file, beginning with the top level element `<keyboard3>`.

### <a name="Element_keyboard3" id="Element_keyboard3" href="#Element_keyboard3">Element: keyboard3</a>

This is the top level element. All other elements defined below are under this element.

**Syntax**

```xml
<keyboard3 locale="…localeId">
    <!-- …definition of the layout as described by the elements defined below -->
</keyboard3>
```

> <small>
>
> Parents: _none_
>
> Children: [displays](#element-displays), [flicks](#element-flicks), [forms](#element-forms), [import](#element-import), [info](#element-info), [keys](#element-keys), [layers](#element-layers), [locales](#element-locales), [settings](#element-settings), [_special_](tr35.md#special), [transforms](#element-transforms), [variables](#element-variables), [version](#element-version)
>
> Occurrence: required, single
>
> </small>

_Attribute:_ `conformsTo` (required)

This attribute value specifies the minimum supported CLDR major version required to properly interpret this keyboard.

The value must be a whole number of `45` or greater. See [`cldrVersion`](tr35-info.md#version-information)

* CLDR's stability policy is such that keyboards which conform to a CLDR version automatically are conformant to all future versions. In other words, a layout with `conformsTo="45"` could be changed to `conformsTo="46"` with no other changes and the layout would remain conformant.


To promote wider interchange, authors and tooling should use the minimum `conformsTo` value necessary to support the keyboard.

```xml
<keyboard3 … conformsTo="45"/>
```

_Attribute:_ `locale` (required)

* This attribute value contains the primary locale of the keyboard using BCP 47 [Unicode locale identifiers](tr35.md#Canonical_Unicode_Locale_Identifiers) - for example `"el"` for Greek. Sometimes, the locale may not specify the base language. For example, a Devanagari keyboard for many languages could be specified by BCP-47 code: `"und-Deva"`. However, it is better to list out the languages explicitly using the [`locales`](#element-locales) element.


For further details about the choice of locale ID, see [Keyboard IDs](#keyboard-ids).

**Example** (for illustrative purposes only, not indicative of the real data)

```xml
<keyboard3 locale="ka">
  …
</keyboard3>
```

```xml
<keyboard3 locale="fr-CH-t-k0-azerty">
  …
</keyboard3>
```

_Attribute:_ `draft`

If this attribute is present, it indicates the status of all the data in this keyboard layout. See [draft attribute](tr35.md#attribute-draft) for further details.

* * *

### <a name="Element_import" id="Element_import" href="#Element_import">Element: import</a>

The `import` element is used to reference another xml file so that elements are imported from
another file. The use case is to be able to import a standard set of `transform`s and similar
from the CLDR repository, especially to be able to share common information relevant to a particular script.
The intent is for each single XML file to contain all that is needed for a keyboard layout, other than required standard import data from the CLDR repository.

* **` can be**: `<import>` can be used as a child of a number of elements (see the _Parents_ section immediately below). Multiple `<import>` elements may be used, however, `<import>` elements must come before any other sibling elements.

If two identical elements are defined, the later element will take precedence, that is, override.
Imported elements may contain other `<import>` statements. Implementations must prevent recursion, that is, each imported file may only be included once.

**Note:** imported files do not have any indication of their normalization mode. For this reason, the keyboard author must verify that the imported file is of a compatible normalization mode. See the [`settings` element](#element-settings) for further details.

**Syntax**
```xml
<import base="cldr" path="45/keys-Zyyy-punctuation.xml"/>
```
> <small>
>
> Parents: [displays](#element-displays), [flicks](#element-flicks), [forms](#element-forms), [keyboard3](#element-keyboard3), [keys](#element-keys), [layers](#element-layers), [transformGroup](#element-transformgroup), [transforms](#element-transforms), [variables](#element-variables)
> Children: _none_
>
> Occurrence: optional, multiple
>
> </small>

_Attribute:_ `base`

> The base may be omitted (indicating a local import) or have the value `"cldr"`.

**Note:** `base="cldr"` is required for all `<import>` statements within keyboard files in the CLDR repository.

_Attribute:_ `path` (required)

> If `base` is `cldr`, then the `path` must start with a CLDR major version (such as `45`) representing the CLDR version to pull imports from. The imports are located in the `keyboard/import` subdirectory of the CLDR source repository.
> Implementations are not required to have all CLDR versions available to them.
>
> If `base` is omitted, then `path` is an absolute or relative file path.


**Further Examples**

```xml
<!-- in a keyboard xml file-->
…
<transforms type="simple">
    <import base="cldr" path="45/transforms-example.xml"/>
    <transform from="` " to="`" />
    <transform from="^ " to="^" />
</transforms>
…


<!-- contents of transforms-example.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<transforms>
    <!-- begin imported part-->
    <transform from="`a" to="à" />
    <transform from="`e" to="è" />
    <transform from="`i" to="ì" />
    <transform from="`o" to="ò" />
    <transform from="`u" to="ù" />
    <!-- end imported part -->
</transforms>
```

**Note:** The root element, here `transforms`, is the same as
the _parent_ of the `<import/>` element. It is an error to import an XML file
whose root element is different than the parent element of the `<import/>` element.

After loading, the above example will be the equivalent of the following.

```xml
<transforms type="simple">
    <!-- begin imported part-->
    <transform from="`a" to="à" />
    <transform from="`e" to="è" />
    <transform from="`i" to="ì" />
    <transform from="`o" to="ò" />
    <transform from="`u" to="ù" />
    <!-- end imported part -->

    <!-- this line is after the import -->
    <transform from="^ " to="^" />
    <transform from="` " to="`" />
</transforms>
```

* * *

### <a name="Element_locales" id="Element_locales" href="#Element_locales">Element: locales</a>

The optional `<locales>` element allows specifying additional or alternate locales.

**Syntax**

```xml
<locales>
    <locale id="…"/>
    <locale id="…"/>
</locales>
```

> <small>
>
> Parents: [keyboard3](#element-keyboard3)
>
> Children: [locale](#element-locale)
>
> Occurrence: optional, single
>
> </small>

### <a name="Element_locale" id="Element_locale" href="#Element_locale">Element: locale</a>

The `<locale>` element specifies an additional or alternate locale. Denotes intentional support for an extra language, not just that a keyboard incidentally supports a language’s orthography.

**Syntax**

```xml
<locale id="…id"/>
```

> <small>
>
> Parents: [locales](#element-locales)
>
> Children: _none_
>
> Occurrence: optional, multiple
>
> </small>

_Attribute:_ `id` (required)

> The [BCP 47](tr35.md#Canonical_Unicode_Locale_Identifiers) locale ID of an additional language supported by this keyboard.
> Must _not_ include the `-k0-` subtag for this additional language.

**Example**

See [Principles for Keyboard IDs](#principles-for-keyboard-ids) for discussion and further examples.

```xml
<!-- Pan Nigerian Keyboard-->
<keyboard3 locale="mul-Latn-NG-t-k0-panng">
    <locales>
        <locale id="ha"/>
        <locale id="ig"/>
        <!-- others … -->
    </locales>
</keyboard3>
```

* * *

### <a name="Element_version" id="Element_version" href="#Element_version">Element: version</a>

Element used to keep track of the source data version.

**Syntax**

```xml
<version number="…number">
```

> <small>
>
> Parents: [keyboard3](#element-keyboard3)
>
> Children: _none_
>
> Occurrence: optional, single
>
> </small>

_Attribute:_ `number` (required)

> Must be a [[SEMVER](https://semver.org)] compatible version number, such as `1.0.0` or `38.0.0-beta.11`

_Attribute:_ `cldrVersion` (fixed by DTD)

> The CLDR specification version that is associated with this data file. This value is fixed and is inherited from the [DTD file](https://github.com/unicode-org/cldr/tree/main/keyboards/dtd) and therefore does not show up directly in the XML file.

**Example**

```xml
<keyboard3 locale="tok">
    …
    <version number="1"/>
    …
</keyboard3>
```

* * *

### <a name="Element_info" id="Element_info" href="#Element_info">Element: info</a>

Element containing informative properties about the layout, for displaying in user interfaces etc.

**Syntax**

```xml
<info
      name="…name"
      author="…author"
      layout="…hint of the layout"
      indicator="…short identifier"
      attribution="…attribution" />
```

> <small>
>
> Parents: [keyboard3](#element-keyboard3)
>
> Children: _none_
>
> Occurrence: required, single
>
> </small>

_Attribute:_ `name` (required)

> Note that this is the only required attribute for the `<info>` element.
>
> This attribute is an informative name for the keyboard.

```xml
<keyboard3 locale="bg-t-k0-phonetic-trad">
    …
    <info name="Bulgarian (Phonetic Traditional)" />
    …
</keyboard3>
```

* * *

_Attribute:_ `author`

> The `author` attribute value contains the name of the author of the layout file.
> There is no requirement that an implementation display, store, or otherwise process this informative attribute.

* * *

_Attribute:_ `layout`

> The `layout` attribute describes the layout pattern, such as QWERTY, DVORAK, INSCRIPT, etc. typically used to distinguish various layouts for the same language.
>
> This attribute is not localized, but is an informative identifier for implementation use.

* * *

_Attribute:_ `indicator`

> The `indicator` attribute describes a short string to be used in currently selected layout indicator, such as `US`, `SI9` etc.
> Typically, this is shown on a UI element that allows switching keyboard layouts and/or input languages.
>
> This attribute is not localized.

* * *

_Attribute:_ `attribution`

> The `attribution` attribute describes a short string which gives some indication of the originating entity of the keyboard design, if different from the author of the layout file.
> For example, an external standards body or other entity may have originated the layout used in the document.
> This attribute does not imply endorsement by the named entity.
>
> This attribute is not localized.
> There is no requirement that an implementation display, store, or otherwise process this attribute.

```xml
<info attribution="Malta Standards Authority"/>
```

* * *

### <a name="Element_settings" id="Element_settings" href="#Element_settings">Element: settings</a>

* An element used to keep track of layout-specific settings by implementations. This element may or may not show up on a layout. These settings reflect the normal practice by the implementation. However, an implementation using the data may customize the behavior.


**Syntax**

```xml
<settings normalization="disabled" />
```

> <small>
>
> Parents: [keyboard3](#element-keyboard3)
>
> Children: _none_
>
> Occurrence: optional, single
>
> </small>

_Attribute:_ `normalization="disabled"`

> The presence of this attribute indicates that normalization will not be applied to the input text, matching, or the output.
> See [Normalization](#normalization) for additional details.
>
> **Note**: while this attribute is allowed by the specification, it should be used with caution.


**Example**

```xml
<keyboard3 locale="bg">
    …
    <settings normalization="disabled" />
    …
</keyboard3>
```

* * *

### <a name="Element_displays" id="Element_displays" href="#Element_displays">Element: displays</a>

The `displays` element consists of a list of [`display`](#element-display) subelements.

**Syntax**

```xml
<displays>
    <display … />
    <display … />
    …
</displays>
```

> <small>
>
> Parents: [keyboard3](#element-keyboard3)
>
> Children: [display](#element-display), [displayOptions](#element-displayoptions), [_special_](tr35.md#special)
>
> Occurrence: optional, single
>
> </small>

* * *

### <a name="Element_display" id="Element_display" href="#Element_display">Element: display</a>

* The `display` elements can be used to describe what is to be displayed on the keytops for various keys. For the most part, such explicit information is unnecessary since the `@to` element from the `keys/key` element will be used for keytop display.


- Some characters, such as diacritics, do not display well on their own.
- Another useful scenario is where there are doubled diacritics, or multiple characters with spacing issues.
- Finally, the `display` element provides a way to specify the keytop for keys which do not otherwise produce output. Keys which switch layers using the `@layerId` attribute typically do not produce output.

> Note: `displays` elements are designed to be shared across many different keyboard layout descriptions, and imported with `<import>` where needed.

#### <a name="Nonspacing_marks_on_keytops" id="Nonspacing_marks_on_keytops" href="#Nonspacing_marks_on_keytops">Non-spacing marks on keytops</a>

For non-spacing marks, U+25CC `◌` is used as a base. It is an error to use a nonspacing character without a base in the `display` attribute. For example, `display="\u{0303}"` would produce an error.

A key which outputs a combining tilde (U+0303) could be represented as either of the following:

```xml
    <display output="\u{0303}" display="◌̃" />  <!-- \u{25CC} \u{0303}-->
    <display output="\u{0303}" display="\u{25cc}\u{0303}" />  <!-- also acceptable -->
```

This way, a key which outputs a combining tilde (U+0303) will be represented as `◌̃` (a tilde on a dotted circle).

Users of some scripts/languages may prefer a different base than U+25CC. See  [`<displayOptions baseCharacter=…/>`](#element-displayoptions).


**Syntax**

```xml
<display output="…string" display="…string" />
```

> <small>
>
> Parents: [displays](#element-displays)
>
> Children: _none_
>
> Occurrence: required, multiple
>
> </small>

One of the `output` or `id` attributes is required.

**Note**: There is currently no way to indicate that a key has a standardized identity (e.g. that a key should be identified as a “Shift”). This may be addressed in future versions of this standard.

_Attribute:_ `output` (optional)

> Specifies the character or character sequence from the `keys/key` element that is to have a special display.
> This attribute may be escaped with `\u` notation, see [Escaping](#escaping).
> The `output` attribute may also contain the `\m{…}` syntax to reference a marker. See [Markers](#markers). Implementations may highlight a displayed marker, such as with a lighter text color, or a yellow highlight.
> String variables may be substituted. See [String variables](#element-string)

_Attribute:_ `keyId` (optional)

> Specifies the `key` id. This is useful for keys which do not produce any output (no `output=` value), such as a shift key.
>
> Must match `[A-Za-z0-9][A-Za-z0-9_-]*`

_Attribute:_ `display` (required)

> Required and specifies the character sequence that should be displayed on the keytop for any key that generates the `@output` sequence or has the `@id`. (It is an error if the value of the `display` attribute is the same as the value of the `output` attribute, this would be an extraneous entry.)

> String variables may be substituted. See [String variables](#element-string)

This attribute may be escaped with `\u` notation, see [Escaping](#escaping).

**Example**

```xml
<keyboard3>
    <keys>
        <key id="grave" output="\u{0300}" /> <!-- combining grave -->
        <key id="marker" output="\m{acute}" /> <!-- generates a marker-->
        <key id="numeric" layerId="numeric" /> <!-- changes layers-->
    </keys>
    <displays>
        <display output="\u{0300}" display="ˋ" /> <!-- \u{02CB} -->
        <display keyId="numeric"  display="#" /> <!-- display the layer shift key as # -->
        <display output="\m{acute}" display="´" /> <!-- Display \m{acute} as ´ -->
    </displays>
</keyboard3>
```

* To allow `displays` elements to be shared across keyboards, there is no requirement that `@output` in a `display` element matches any `@output`/`@id` in any `keys/key` element in the keyboard description.


* * *

### <a name="Element_displayOptions" id="Element_displayOptions" href="#Element_displayOptions">Element: displayOptions</a>

The `displayOptions` is an optional singleton element providing additional settings on this `displays`.  It is structured so as to provide for future flexibility in such options.

**Syntax**

```xml
<displays>
    <display …/>
    <displayOptions baseCharacter="x"/>
</displays>
```

> <small>
>
> Parents: [displays](#element-displays)
>
> Children: _none_
>
> Occurrence: optional, single
>
> </small>

_Attribute:_ `baseCharacter` (optional)

**Note:** At present, this is the only option settable in the `displayOptions`.

> Some scripts/languages may prefer a different base than U+25CC.
> For Lao for example, `x` is often used as a base instead of `◌`.
> Setting `baseCharacter="x"` (for example) is a _hint_ to the implementation which
> requests U+25CC to be substituted with `x` on display.
> As a hint, the implementation may ignore this option.
>
> **Note** that not all base characters will be suitable as bases for combining marks.

This attribute may be escaped with `\u` notation, see [Escaping](#escaping).

* * *

### <a name="Element_keys" id="Element_keys" href="#Element_keys">Element: keys</a>

This element defines the properties of all possible keys via [`<key>` elements](#element-key) used in all layouts.
It is a “bag of keys” without specifying any ordering or relation between the keys.
There is only a single `<keys>` element in each layout.

**Syntax**

```xml
<keys>
    <key … />
    <key … />
    <key … />
</keys>
```

> <small>
>
> Parents: [keyboard3](#element-keyboard3)
> Children: [key](#element-key)
> Occurrence: optional, single
>
> </small>



* * *

### <a name="Element_key" id="Element_key" href="#Element_key">Element: key</a>

* This element defines a mapping between an abstract key and its output. This element must have the `keys` element as its parent. The `key` element is referenced by the `keys=` attribute of the [`row` element](#element-row).


**Syntax**

```xml
<key
 id="…keyId"
 flickId="…flickId"
 gap="true"
 output="…string"
 longPressKeyIds="…list of keyIds"
 longPressDefaultKeyId="…keyId"
 multiTapKeyIds="…listId"
 stretch="true"
 layerId="…layerId"
 width="…number"
 />
```

> <small>
>
> Parents: [keys](#element-keys)
>
> Children: _none_
>
> Occurrence: optional, multiple
> </small>

**Note**: The `id` attribute is required.

**Note**: _at least one of_ `layerId`, `gap`, or `output` are required.

_Attribute:_ `id`

> The `id` attribute uniquely identifies the key. NMTOKEN. It can (but needn't be) the key name (a, b, c, A, B, C, …), or any other valid token (e-acute, alef, alif, alpha, …).
>
> In the future, this attribute’s definition is expected to be updated to align with [UAX#31](https://www.unicode.org/reports/tr31/).

_Attribute:_ `flickId="…flickId"` (optional)

> The `flickId` attribute indicates that this key makes use of a [`flick`](#element-flick) set with the specified id.

_Attribute:_ `gap="true"` (optional)

> The `gap` attribute indicates that this key does not have any appearance, but causes a "gap" of the specified number of key widths. Can be used with `width` to set a width.
> Such elements may not be referred to by `display` elements, nor may they have any of the following attributes:  `flickId`, `longPressKeyId`, `longPressDefaultKeyId`, `multiTapKeyIds`, `layerId`, or `output`.

```xml
<key id="mediumgap" gap="true" width="1.5"/>
```

_Attribute:_ `output`

> The `output` attribute value contains the sequence of characters that is emitted when pressing this particular key. Control characters, whitespace (other than the regular space character) and combining marks in this attribute are escaped using the `\u{…}` notation. More than one key may output the same output.
>
> The `output` attribute may also contain the `\m{…markerId}` syntax to insert a marker. See the definition of [markers](#markers).

_Attribute:_ `longPressKeyIds="…list of keyIds"` (optional)

> A space-separated ordered list of `key` element ids, which keys which can be emitted by "long-pressing" this key. This feature is prominent in mobile devices.
>
> In a list of keys specified by `longPressKeyIds`, the key matching `longPressDefaultKeyId` attribute (if present) specifies the default long-press target, which could be different than the first element. It is an error if the `longPressDefaultKeyId` key is not in the `longPressKeyIds` list.
>
> Implementations shall ignore any gestures (such as flick, multiTap, longPress) defined on keys in the `longPressKeyIds` list.
>
> For example, if the default key is a key whose [display](#element-displays) value is `{`, an implementation might render the key as follows:
>
> ![keycap hint](images/keycapHint.png)
>
> _Example:_
> - pressing the `o` key will produce `o`
> - holding down the key will produce a list `ó`, `{` (where `{` is the default and produces a marker)
>
> ```xml
> `<displays>`
>    `<display output="\m{marker}" display="{" />`
> `</displays>`
>
> `<keys>`
>    `<key id="o" output="o" longPressKeyIds="o-acute marker" longPressDefaultKeyId="marker">`
>    `<key id="o-acute" output="ó"/>`
>    `<key id="marker" output="\m{marker}" />`
> `</key>`
>
> ```

_Attribute:_ `longPressDefaultKeyId="…keyId"` (optional)

> Specifies the default key, by id, in a list of long-press keys. See the discussion of `LongPressKeyIds`, above.

_Attribute:_ `multiTapKeyIds` (optional)

> A space-separated ordered list of `key` element ids, which keys, where each successive key in the list is produced by the corresponding number of quick taps.
> It is an error for a key to reference itself in the `multiTapKeyIds` list.
>
> Implementations shall ignore any gestures (such as flick, multiTap, longPress) defined on keys in the `multiTapKeyIds` list.
>
> _Example:_
> - first tap on the key will produce “a”
> - two taps will produce “bb”
> - three taps on the key will produce “c”
> - four taps on the key will produce “d”
>
> ```xml
> `<keys>`
>    `<key id="a" output="a" multiTapKeyIds="bb c d">`
>    `<key id="bb" output="bb" />`
>    `<key id="c" output="c" />`
>    `<key id="d" output="d" />`
> `</key>`
> ```

**Note**: Behavior past the end of the multiTap list is implementation specific.

_Attribute:_ `stretch="true"` (optional)

> The `stretch` attribute indicates that a touch layout may stretch this key to fill available horizontal space on the row.
> This is used, for example, on the spacebar. Note that `stretch=` is ignored for hardware layouts.

_Attribute:_ `layerId="shift"` (optional)

> The `layerId` attribute indicates that this key switches to another `layer` with the specified id (such as `<layer id="shift"/>` in this example).
> Note that a key may have both a `layerId=` and a `output=` attribute, indicating that the key outputs _prior_ to switching layers.
> Also note that `layerId=` is ignored for hardware layouts: their shifting is controlled via
> the modifier keys.
>
> This attribute is an NMTOKEN.
>
> In the future, this attribute’s definition is expected to be updated to align with [UAX#31](https://www.unicode.org/reports/tr31/).


_Attribute:_ `width="1.2"` (optional, default "1.0")

> The `width` attribute indicates that this key has a different width than other keys, by the specified number of key widths.

```xml
<key id="wide-a" output="a" width="1.2"/>
<key id="wide-gap" gap="true" width="2.5"/>
```

#### <a name="Implied_Keys" id="Implied_Keys" href="#Implied_Keys">Implied Keys</a>

Not all keys need to be listed explicitly.  The following two can be assumed to already exist:

```xml
<key id="gap" gap="true" width="1"/>
<key id="space" output=" " stretch="true" width="1"/>
```

In addition, these 62 keys, comprising 10 digit keys, 26 Latin lower-case keys, and 26 Latin upper-case keys, where the `id` is the same as the `to`, are assumed to exist:

```xml
<key id="0" output="0"/>
<key id="1" output="1"/>
<key id="2" output="2"/>
…
<key id="A" output="A"/>
<key id="B" output="B"/>
<key id="C" output="C"/>
…
<key id="a" output="a"/>
<key id="b" output="b"/>
<key id="c" output="c"/>
…
```

These implied keys are available in a data file named `keyboards/import/keys-Latn-implied.xml` in the CLDR distribution for the convenience of implementations.

Thus, the implied keys behave as if the following import were present.

```xml
<keyboard3>
    <keys>
        <import base="cldr" path="45/keys-Latn-implied.xml" />
    </keys>
</keyboard3>
```

**Note:** All implied keys may be overridden, as with all other imported data items. See the [`import`](#element-import) element for more details.

* * *

### <a name="Element_flicks" id="Element_flicks" href="#Element_flicks">Element: flicks</a>

The `flicks` element is a collection of `flick` elements.

> <small>
>
> Parents: [keyboard3](#element-keyboard3)
>
> Children: [flick](#element-flick), [import](#element-import), [_special_](tr35.md#special)
>
> Occurrence: optional, single
> </small>

* * *

#### <a name="Element_flick" id="Element_flick" href="#Element_flick">Element: flick</a>

The `flick` element is used to generate results from a "flick" of the finger on a mobile device.

**Syntax**

```xml
<keyboard3>
    <keys>
        <key id="a" flickId="a-flicks" output="a" />
    </keys>
    <flicks>
        <flick id="a-flicks">
            <flickSegment … />
            <flickSegment … />
            <flickSegment … />
        </flick>
    </flicks>
</keyboard3>
```

> <small>
>
> Parents: [flicks](#element-flicks)
>
> Children: [flickSegment](#element-flicksegment), [_special_](tr35.md#special)
>
> Occurrence: optional, multiple
>
> </small>

_Attribute:_ `id` (required)

> The `id` attribute identifies the flicks. It can be any NMTOKEN.
>
> The `id` attribute on `flick` elements are distinct from the `id` attribute on `key` elements.
> For example, it is permissible to have both `<key id="a" />` and
> `<flick id="a" />` which are two unrelated elements.
>
> In the future, this attribute’s definition is expected to be updated to align with [UAX#31](https://www.unicode.org/reports/tr31/).

* * *

#### <a name="Element_flickSegment" id="Element_flickSegment" href="#Element_flickSegment">Element: flickSegment</a>

> <small>
>
> Parents: [flick](#element-flick)
>
> Children: _none_
>
> Occurrence: required, multiple
>
> </small>

_Attribute:_ `directions` (required)

> The `directions` attribute value is a space-delimited list of keywords, that describe a path, currently restricted to the cardinal and intercardinal directions `{n e s w ne nw se sw}`.

_Attribute:_ `keyId` (required)

> The `keyId` attribute value is the result of (one or more) flicks.
>
> Implementations shall ignore any gestures (such as flick, multiTap, longPress) defined on the key specified by `keyId`.


**Example**
where a flick to the Northeast then South produces `Å`.

```xml
<keys>
    <key id="something" flickId="a" output="Something" />
    <key id="A-ring" output="A-ring" />
</keys>

<flicks>
    <flick id="a">
        <flickSegment directions="ne s" keyId="A-ring" />
    </flick>
</flicks>
```

* * *

### <a name="Element_forms" id="Element_forms" href="#Element_forms">Element: forms</a>

This element contains a set of `form` elements which define the layout of a particular hardware form.


> <small>
>
> Parents: [keyboard3](#element-keyboard3)
>
> Children: [import](#element-import), [form](#element-form), [_special_](tr35.md#special)
>
> Occurrence: optional, single
>
> </small>

***Syntax***

```xml
<forms>
    <form id="iso">
        <!-- … -->
    </form>
    <form id="us">
        <!-- … -->
    </form>
</forms>
```

* * *

### <a name="Element_form" id="Element_form" href="#Element_form">Element: form</a>

This element contains a specific `form` element which defines the layout of a particular hardware form.

> *Note:* Most keyboards will not need to use this element directly, and the CLDR repository will not accept keyboards which define a custom `form` element.  This element is provided for two reasons:

1. To formally specify the standard hardware arrangements used with CLDR for implementations. Implementations can verify the arrangement, and validate keyboards against the number of rows and the number of keys per row.

2. To allow a way to customize the scancode layout for keyboards not intended to be included in the common CLDR repository.

See [Implied Form Values](#implied-form-values), below.

> <small>
>
> Parents: [forms](#element-forms)
>
> Children: [scanCodes](#element-scancodes), [_special_](tr35.md#special)
>
> Occurrence: optional, multiple
>
> </small>

_Attribute:_ `id` (required)

> This attribute specifies the form id. The value may not be `touch`.

> Must match `[A-Za-z0-9][A-Za-z0-9_-]*`


***Syntax***

```xml
<form id="us">
    <scanCodes codes="00 01 02"/>
    <scanCodes codes="03 04 05"/>
</form>
```

#### <a name="Implied_Form_Values" id="Implied_Form_Values" href="#Implied_Form_Values">Implied Form Values</a>

There is an implied set of `<form>` elements corresponding to the default forms, thus implementations must behave as if there was the following import statement:

```xml
<keyboard3>
    <forms>
        <import base="cldr" path="45/scanCodes-implied.xml" /> <!-- the version will match the current conformsTo of the file -->
    </forms>
</keyboard3>
```

Here is a summary of the implied form elements. Keyboards included in the CLDR Repository must only use these `formId=` values and may not override the scanCodes.

> - `touch` - Touch (non-hardware) layout.
> - `abnt2` - Brazilian 103 key ABNT2 layout (iso + extra key near right shift)
> - `iso` - European 102 key layout (extra key near left shift)
> - `jis` - Japanese 109 key layout
> - `us` - ANSI 101 key layout
> - `ks` - Korean KS layout

* * *

### <a name="Element_scanCodes" id="Element_scanCodes" href="#Element_scanCodes">Element: scanCodes</a>

This element contains a keyboard row, and defines the scan codes for the non-frame keys in that row.

> <small>
>
> Parents: [form](#element-form)
>
> Children: none
>
> Occurrence: required, multiple
>
> </small>

> _Attribute:_ `codes` (required)

> The `codes` attribute is a space-separated list of 2-digit hex bytes, each representing a scan code.

**Syntax**

```xml
<scanCodes codes="29 02 03 04 05 06 07 08 09 0A 0B 0C 0D" />
```

* * *

### <a name="Element_layers" id="Element_layers" href="#Element_layers">Element: layers</a>

This element contains a set of `layer` elements with a specific physical form factor, whether
hardware or touch layout.

> <small>
>
> Parents: [keyboard3](#element-keyboard3)
>
> Children: [import](#element-import), [layer](#element-layer), [_special_](tr35.md#special)
>
> Occurrence: required, multiple
>
> </small>

- At least one `layers` element is required.

_Attribute:_ `formId` (required)

> This attribute specifies the physical layout of a hardware keyboard,
> or that the form is a `touch` layout.
>
> When using an on-screen touch keyboard, if the keyboard does not specify a `<layers formId="touch">`
> element, a `<layers formId="…formId">` element can be used as an fallback alternative.
> If there is no `hardware` form, the implementation may need
> to choose a different keyboard file, or use some other fallback behavior when using a
> hardware keyboard.
>
> Because a hardware keyboard facilitates non-trivial amounts of text input,
> and many touch devices can also be connected to a hardware keyboard, it
> is recommended to always have a hardware (non-touch) form.
>
> Multiple `<layers formId="touch">` elements are allowed with distinct `minDeviceWidth` values.
> At most one hardware (non-`formId="touch"`) `<layers>` element is allowed. If a different key arrangement is desired between, for example, `us` and `iso` formats, these should be separated into two different keyboards.
>
> The typical keyboard author will be designing a keyboard based on their circumstances and the hardware that they are using. So, for example, if they are in South East Asia, they will almost certainly be using an 101 key hardware keyboard with US key caps. So we want them to be able to reference that (`<layers formId="us">`) in their design, rather than having to work with an unfamiliar form.
>
> A mismatch between the hardware layout in the keyboard file, and the actual hardware used by the user could result in some keys being inaccessible to the user if their hardware cannot generate the scancodes corresponding to the layout specified by the `formId=` attribute. Such keys could be accessed only via an on-screen keyboard utility. Conversely, a user with hardware keys that are not present in the specified `formId=` will result in some hardware keys which have no function when pressed.
>
> The value of the `formId=` attribute may be `touch`, or correspond to a `form` element. See [`form`](#element-form).
>

_Attribute:_ `minDeviceWidth`

> This attribute specifies the minimum required width, in millimeters (mm), of the touch surface.  The `layers` entry with the greatest matching width will be selected. This attribute is intended for `formId="touch"`, but is supported for hardware forms.
>
> This must be a whole number between 1 and 999, inclusive.

### <a name="Element_layer" id="Element_layer" href="#Element_layer">Element: layer</a>

A `layer` element describes the configuration of keys on a particular layer of a keyboard. It contains one or more `row` elements to describe which keys exist in each row.

**Syntax**

```xml
<layer id="…layerId" modifiers="…modifier modifier, …modifier modifier, …">
    <row …/>
    <row …/>
    …
</layer>
```

> <small>
>
> Parents: [keyboard3](#element-keyboard3)
>
> Children: [row](#element-row), [_special_](tr35.md#special)
>
> Occurrence: optional, multiple
>
> </small>

_Attribute_ `id` (required for `touch`)

> The `id` attribute identifies the layer for touch layouts.  This identifier specifies the layout as the target for layer switching, as specified by the `layerId=` attribute on the [`<key>`](#element-key) element.
> Touch layouts must have one `layer` with `id="base"` to serve as the base layer.
>
> Must match `[A-Za-z0-9][A-Za-z0-9_-]*`

_Attribute:_ `modifiers` (required for `hardware`)

> This has two roles. It acts as an identifier for the `layer` element for hardware keyboards (in the absence of the id= element) and also provides the linkage from the hardware modifiers into the correct `layer`.
>
> For hardware layouts, the use of `@modifiers` as an identifier for a layer is sufficient since it is always unique among the set of `layer` elements in each  `form`.
>
> This attribute value is a list of lists. It is a comma-separated (`,`) list of modifier sets, and each modifier set is a space-separated list of modifier components.
>
> Each modifier component must match `[A-Za-z0-9]+`. Extra whitespace is ignored.
>
> To indicate that no modifiers apply, the reserved name of `none` is used.
>
> For hardware layouts, the `layer` with `modifiers="none"` becomes the base layer when the keyboard is used as a touch layout.

**Syntax**

```xml
<layer id="base"        modifiers="none">
    <row keys="a" />
</layer>

<layer id="upper"       modifiers="shift">
    <row keys="A" />
</layer>

<layer id="altgr"       modifiers="altR">
    <row keys="a-umlaut" />
</layer>

<layer id="upper-altgr" modifiers="altR shift">
    <row keys="A-umlaut" />
</layer>
```

#### <a name="Layer_Modifier_Sets" id="Layer_Modifier_Sets" href="#Layer_Modifier_Sets">Layer Modifier Sets</a>

The `@modifiers` attribute value contains one or more Layer Modifier Sets, separated by commas.
For example, in the element `<layer … modifiers="ctrlL altL, altR" …` the attribute value consists of two sets:

- `ctrlL altL` (two components)
- `altR` (one component)

* The order of the sets and the order of the components within each set is not significant. However, for clarity in reading, the canonical order within a set is in the order listed in Layout Modifier Components; the canonical order for the sets should be first by the cardinality of the sets (least first), then alphabetical.


#### <a name="Layer_Modifier_Components" id="Layer_Modifier_Components" href="#Layer_Modifier_Components">Layer Modifier Components</a>

Within a Layer Modifier Set, the following modifier components can be used, separated by spaces.

 - `none` (no modifier)
 - `alt`
 - `altL`
 - `altR`
 - `caps`
 - `ctrl`
 - `ctrlL`
 - `ctrlR`
 - `shift`
 - `other` (matches if no other layers match)

1. `alt` in this specification is referred to on some platforms as "opt" or "option".

2. `none` and `other` may not be combined with any other components.

#### <a name="Modifier_Left_and_Right_keys" id="Modifier_Left_and_Right_keys" href="#Modifier_Left_and_Right_keys">Modifier Left- and Right- keys</a>

1. `L` or `R` indicates a left- or right- side modifier only (such as `altL`)
 whereas `alt` indicates _either_ left or right alt key (that is, `altL` or `altR`). `ctrl` indicates either left or right ctrl key (that is, `ctrlL` or `ctrlR`).

2. Keyboard implementations must warn if a keyboard mixes `alt` with `altL`/`altR`, or `ctrl` with `ctrlL`/`ctrlR`.

3. Left- and right- side modifiers may not be mixed together in a single `modifier` attribute value, so neither `altL ctrlR"` nor `altL altR` are allowed.

4. `shift` indicates either shift key. The left and right shift keys are not distinguishable in this specification.

#### <a name="Layer_Modifier_Matching" id="Layer_Modifier_Matching" href="#Layer_Modifier_Matching">Layer Modifier Matching</a>

Layers are matched exactly based on the modifier keys which are down. For example:

- `none` as a modifier will only match if *all* of the keys `caps`, `alt`, `ctrl` and `shift` are up.

- `alt` as a modifier will only match if either `alt` is down, *and* `caps`, `ctrl`, and `shift` are up.

- `altL ctrl` as a modifier will only match if the left `alt` is down, either `ctrl` is down, *and* `shift` and `caps` are up.

- `other` as a modifier will match if no other layers match.

* Multiple modifier sets are separated by commas.  For example, `none, shift caps` will match either no modifiers *or* shift and caps.  `ctrlL altL, altR` will match either  left-control and left-alt, *or* right-alt.


* Keystrokes must be ignored where there isn’t a layer that explicitly matches nor a layer with `other`. Example: If there is a `ctrl` and `shift` layer, but no `ctrl shift` nor `other` layer, no output will result from `ctrl shift X`.


Layers are not allowed to overlap in their matching.  For example, the keyboard author will receive an error if one layer specifies `alt shift` and another layer specifies `altR shift`.

There is one special case:  the `other` layer matches if and only if no other layer matches. Thus logically the `other` layer is matched after all other layers have been checked.

Because there is no overlap allowed between layers, the order of `<layer>` elements is not significant.

> Note: The modifier syntax may be enhanced in the future, but will remain backwards compatible with the syntax described here.

* * *

### <a name="Element_row" id="Element_row" href="#Element_row">Element: row</a>

A `row` element describes the keys that are present in the row of a keyboard.

**Syntax**

```xml
<row keys="…keyId …keyId …" />
```

> <small>
>
> Parents: [layer](#element-layer)
>
> Children: _none_
>
> Occurrence: required, multiple
>
> </small>

_Attribute:_ `keys` (required)

> This is a string that lists the id of [`key` elements](#element-key) for each of the keys in a row, whether those are explicitly listed in the file or are implied.  See the `key` documentation for more detail.
>
> For non-`touch` forms, the number of keys in each row may not exceed the number of scan codes defined for that row, and the number of rows may not exceed the defined number of rows for that form. See [`scanCodes`](#element-scancodes);

**Example**

Here is an example of a `row` element:

```xml
<row keys="a z e r t y u i o p caret dollar" />
```

* * *

### <a name="Element_variables" id="Element_variables" href="#Element_variables">Element: variables</a>

> <small>
>
> Parents: [keyboard3](#element-keyboard3)
>
> Children: [import](#element-import), [_special_](tr35.md#special), [string](#element-string), [set](#element-set), [uset](#element-uset)
>
> Occurrence: optional, single
> </small>

This is a container for variables to be used with [transform](#element-transform), [display](#element-display) and [key](#element-key) elements.

Note that the `id=` attribute value must be unique across all children of the `variables` element.

**Example**

```xml
<variables>
    <string id="y" value="yes" /> <!-- a simple string-->
    <set id="upper" value="A B C D E FF" /> <!-- a set with 6 items -->
    <uset id="consonants" value="[कसतनमह]" /> <!-- a UnicodeSet -->
</variables>
```

* * *

### <a name="Element_string" id="Element_string" href="#Element_string">Element: string</a>

> <small>
>
> Parents: [variables](#element-variables)
>
> Children: _none_
>
> Occurrence: optional, multiple
> </small>

> This element contains a single string which is used by the [transform](#element-transform) elements for string matching and substitution, as well as by the [key](#element-key) and [display](#element-display) elements.

_Attribute:_ `id` (required)

> Specifies the identifier (name) of this string.
> All ids must be unique across all types of variables.
>
> `id` must match `[0-9A-Za-z_]{1,32}`

_Attribute:_ `value` (required)

> Strings may contain whitespaces. However, for clarity, it is recommended to escape spacing marks, even in strings.
> This attribute value may be escaped with `\u` notation, see [Escaping](#escaping).
> Variables may refer to other string variables if they have been previously defined, using `${string}` syntax.
> [Markers](#markers) may be included with the `\m{…}` notation.

**Example**

```xml
<variables>
    <string id="cluster_hi" value="हि" /> <!-- a string -->
    <string id="zwnj" value="\u{200C}"/> <!-- single codepoint -->
    <string id="grave" value="\m{grave}"/> <!-- refer to a marker -->
    <string id="backquote" value="`"/>
    <string id="zwnj_grave" value="${zwnj}${grave}"  /> <!-- Combine two variables -->
    <string id="zwnj_sp_grave" value="${zwnj}\u{0020}${grave}"  /> <!-- Combine two variables -->
</variables>
```

These may be then used in multiple contexts:

```xml
<!-- as part of a regex -->
<transform from="${cluster_hi}X" to="X" />
<transform from="Y" to="${cluster_hi}" />
…
<!-- as part of a key bag  -->
<key id="hi_key" output="${cluster_hi}" />
<key id="grave_key" output="${grave}" />
…
<!-- Display ` instead of the non-displayable marker -->
<display output="${grave}" display="${backquote}" />
```

* * *

### <a name="Element_set" id="Element_set" href="#Element_set">Element: set</a>

> <small>
>
> Parents: [variables](#element-variables)
>
> Children: _none_
>
> Occurrence: optional, multiple
> </small>

> This element contains a set of strings used by the [transform](#element-transform) elements for string matching and substitution.

_Attribute:_ `id` (required)

> Specifies the identifier (name) of this set.
> All ids must be unique across all types of variables.
>
> `id` must match `[0-9A-Za-z_]{1,32}`

_Attribute:_ `value` (required)

> The `value` attribute value is always a set of strings separated by whitespace, even if there is only a single item in the set, such as `"A"`.
> Leading and trailing whitespace is ignored.
> This attribute value may be escaped with `\u` notation, see [Escaping](#escaping).
> Sets may refer to other string variables if they have been previously defined, using `${string}` syntax, or to other previously-defined sets using `$[set]` syntax.
> Set references must be separated by whitespace: `$[set1]$[set2]` is an error; instead use `$[set1] $[set2]`.
> [Markers](#markers) may be included with the `\m{…}` notation.

**Examples**

```xml
<variables>
    <set id="upper" value="A B CC D E FF " /> <!-- 6 items -->
    <set id="lower" value="a b c  d e  f " /> <!-- 6 items -->
    <set id="upper_or_lower" value="$[upper] $[lower]"  /> <!-- Concatenate two sets -->
    <set id="lower_or_upper" value="$[lower] $[upper]"  /> <!-- Concatenate two sets -->
    <set id="a" value="A"/> <!-- Just one element, an 'A'-->
    <set id="cluster_or_zwnj" value="${hi_cluster} ${zwnj}"/> <!-- 2 items: "हि \u${200C}"-->
</variables>
```

Match "X" followed by any uppercase letter:

```xml
<transform from="X$[upper]" to="…" />
```

Map from upper to lower:

```xml
<transform from="($[upper])" to="$[1:lower]" />
```

See [transform](#element-transform) for further details and syntax.

* * *

### <a name="Element_uset" id="Element_uset" href="#Element_uset">Element: uset</a>

> <small>
>
> Parents: [variables](#element-variables)
>
> Children: _none_
>
> Occurrence: optional, multiple
> </small>

> This element contains a set, using a subset of the [UnicodeSet](tr35.md#Unicode_Sets) format, used by the [`transform`](#element-transform) elements for string matching and substitution.
> Note important restrictions on the syntax below.

_Attribute:_ `id` (required)

> Specifies the identifier (name) of this uset.
> All ids must be unique across all types of variables.
>
> `id` must match `[0-9A-Za-z_]{1,32}`

_Attribute:_ `value` (required)

> String value in a subset of [UnicodeSet](tr35.md#Unicode_Sets) format.
> Leading and trailing whitespace is ignored.
> Variables may refer to other string variables if they have been previously defined, using `${string}` syntax, or to other previously-defined `uset` elements (not `set` elements) using `$[...usetId]` syntax.


- Warning: `uset` elements look superficially similar to regex character classes as used in [`transform`](#element-transform) elements, but they are different. `uset`s must be defined with a `uset` element, and referenced with the `$[...usetId]` notation in transforms. `uset`s cannot be specified inline in a transform, and can only be used indirectly by reference to the corresponding `uset` element.
- Multi-character strings (`{}`) are not supported, such as `[żġħ{ie}{għ}]`.
- UnicodeSet property notation (`\p{…}` or `[:…:]`) may **NOT** be used.

> **Rationale**: allowing property notation would make keyboard implementations dependent on a particular version of Unicode. However, implementations and tools may wish to pre-calculate the value of a particular uset, and "freeze" it as explicit code points.  The example below of `$[KhmrMn]` matches nonspacing marks in the `Khmr` script.

- `uset` elements may represent a very large number of codepoints. Keyboard implementations may set a limit on how many unique range entries may be matched.
- The `uset` element may not be used as the source or target for mapping operations (`$[1:variable]` syntax).
- The `uset` element may not be referenced by [`key`](#element-key) or [`display`](#element-display) elements.

**Examples**

```xml
<variables>
  <uset id="consonants" value="[कसतनमह]" /> <!-- unicode set range -->
  <uset id="range" value="[a-z D E F G \u{200A}]" /> <!-- a through z, plus a few others -->
  <uset id="newrange" value="[$[range]-[G]]" /> <!-- The above range, but not including G -->
  <uset id="KhmrMn" value="[\u{17B4}\u{17B5}\u{17B7}-\u{17BD}\u{17C6}\u{17C9}-\u{17D3}\u{17DD}]"> <!--  [[:Khmr:][:Mn:]] as of Unicode 15.0-->
</variables>
```

* * *

### <a name="Element_transforms" id="Element_transforms" href="#Element_transforms">Element: transforms</a>

* This element defines a group of one or more `transform` elements associated with this keyboard layout. This is used to support features such as dead-keys, character reordering, backspace behavior, etc. using a straightforward structure that works for all the keyboards tested, and that results in readable source data.


There can be multiple `<transforms>` elements, but only one for each `type`.

**Syntax**

```xml
<transforms type="…type">
    <transformGroup …/>
    <transformGroup …/>
    …
</transforms>
```

> <small>
>
> Parents: [keyboard3](#element-keyboard3)
>
> Children: [import](#element-import), [_special_](tr35.md#special), [transformGroup](#element-transformgroup)
>
> Occurrence: optional, multiple
>
> </small>

_Attribute:_ `type` (required)

> Values: `simple`, `backspace`

There are other keying behaviors that are needed particularly in handing complex orthographies from various parts of the world. The behaviors intended to be covered by the transforms are:

* Reordering combining marks. The order required for underlying storage may differ considerably from the desired typing order. In addition, a keyboard may want to allow for different typing orders.
* Error indication. Sometimes a keyboard layout will want to specify to the application that a particular keying sequence in a context is in error and that the application should indicate that that particular keypress is erroneous.
* Backspace handling. There are various approaches to handling the backspace key. An application may treat it as an undo of the last key input, or it may simply delete the last character in the currently output text, or it may use transform rules to tell it how much to delete.

#### <a name="Markers" id="Markers" href="#Markers">Markers</a>

Markers are placeholders which record some state, but without producing normal visible text output.  They were designed particularly to support dead-keys.

The marker ID is any valid `NMTOKEN`.

Consider the following abbreviated example:

```xml
    <display output="\m{circ_marker}" display="^" />
…
    <key id="circ_key" output="\m{circ_marker}" />
    <key id="e" output="e" />
…
    <transform from="\m{circ_marker}e" to="ê" />
```

1. The user presses the `circ_key` key. The key can be shown with the keycap `^` due to the `<display>` element.

2. The special marker, `circ_marker`, is added to the end of the input context.

    The input context does not match any transforms.

    The input context has:

    - …
    - marker `circ_marker`

3. Also due to the `<display>` element, implementations can opt to display a visible `^` (perhaps visually distinct from a plain `^` carat). Implementations may opt to display nothing and only store the marker in the input context.

4. The user now presses the `e` key, which is also added to the input context. The input context now has:

    - …
    - character `e`
    - marker `circ_marker`

5. Now, the input context matches the transform.  The `e` and the marker are replaced with `ê`.

    The input context now has:

    - …
    - character `ê`

**Using markers to inhibit other transforms**

Sometimes it is desirable to prevent transforms from having an effect.
Perhaps two different keys output the same characters, with different key or modifier combinations, but only one of them is intended to participate in a transform.

Consider the following case, where pressing the keys `X`, `e` results in `^e`, which is transformed into `ê`.

```xml
<keys>
    <key id="X" output="^"/>
    <key id="e" output="e" />
</keys>
<transforms>
    <transform from="^e" output="ê"/>
</transforms>
```

However, what if the user wanted to produce `^e` without the transform taking effect?
One strategy would be to use a marker, which won’t be visible in the output, but will inhibit the transform.

```xml
<keys>
    <key id="caret" output="^\m{no_transform}"/>
    <key id="X" output="^" />
    <key id="e" output="e" />
</keys>
…
<transforms>
    <!-- this wouldn't match the key caret output because of the marker -->
    <transform from="^e" output="ê"/>
</transforms>
```

* Pressing `caret` `e` will result in `^e` (with an invisible _no_transform_ marker — note that any name could be used). The `^e` won’t have the transform applied, at least while the marker’s context remains valid.


Another strategy might be to use a marker to indicate where transforms are desired, instead of where they aren't desired.

```xml
<keys>
    <key id="caret" output="^"/>
    <key id="X" output="^\m{transform}"/>
    <key id="e" output="e" />
</keys>
…
<transforms …>
    <!-- Won't match ^e without marker. -->
    <transform from="^\m{transform}e" output="ê"/>
</transforms>
```

* In this way, only the `X`, `e` keys will produce `^e` with a _transform_ marker (again, any name could be used) which will cause the transform to be applied. One benefit is that navigating to an existing `^` in a document and adding an `e` will result in `^e`, and this output will not be affected by the transform, because there will be no marker present there (remember that markers are not stored with the document but only recorded in memory temporarily during text input).


Please note important considerations for [Normalization and Markers](#normalization-and-markers).

**Effect of markers on final text**

All markers must be removed before text is returned to the application from the input context.
If the input context changes, such as if the cursor or mouse moves the insertion point somewhere else, all markers in the input context are removed.

**Implementation Notes**

* Ideally, markers are implemented entirely out-of-band from the normal text stream. However, implementations _may_ choose to map each marker to a [Unicode private-use character](https://www.unicode.org/glossary/#private_use_character) for use only within the implementation’s processing and temporary storage in the input context.


* For example, the first marker encountered could be represented as U+E000, the second by U+E001 and so on.  If a regex processing engine were used, then those PUA characters could be processed through the existing regex processing engine.  `[^\u{E000}-\u{E009}]` could be used as an expression to match a character that is not a marker, and `[Ee]\u{E000}` could match `E` or `e` followed by the first marker.


* Such implementations must take care to remove all such markers (see prior section) from the resultant text. As well, implementations must take care to avoid conflicts if applications themselves are using PUA characters, such as is often done with not-yet-encoded scripts or characters.


* * *

### <a name="Element_transformGroup" id="Element_transformGroup" href="#Element_transformGroup">Element: transformGroup</a>

> <small>
>
> Parents: [transforms](#element-transforms)
>
> Children: [import](#element-import), [reorder](#element-reorder), [_special_](tr35.md#special), [transform](#element-transform)
>
> Occurrence: optional, multiple
> </small>

A `transformGroup` contains a set of transform elements or reorder elements.

Each `transformGroup` is processed entirely before proceeding to the next one.


* Each `transformGroup` element, after imports are processed, must have either [reorder](#element-reorder) elements or [transform](#element-transform) elements, but not both. The `<transformGroup>` element may not be empty.


**Examples**


#### <a name="Example_transformGroup_with_transform_elements" id="Example_transformGroup_with_transform_elements" href="#Example_transformGroup_with_transform_elements">Example: `transformGroup` with `transform` elements</a>

* This is a `transformGroup` that consists of one or more [`transform`](#element-transform) elements, prefaced by one or more `import` elements. See the discussion of those elements for details. `import` elements in this group may not import `reorder` elements.



```xml
<transformGroup>
    <import path="…"/> <!-- optional import elements-->
    <transform />
    <!-- other <transform/> elements -->
</transformGroup>
```


#### <a name="Example_transformGroup_with_reorder_elements" id="Example_transformGroup_with_reorder_elements" href="#Example_transformGroup_with_reorder_elements">Example: `transformGroup` with `reorder` elements</a>

* This is a `transformGroup` that consists of one or more [`transform`](#element-transform) elements, optionally prefaced by one or more `import` elements that import `transform` elements. See the discussion of those elements for details.


`import` elements in this group may not import `transform` elements.

```xml
<transformGroup>
    <import path="…"/> <!-- optional import elements-->
    <reorder … />
    <!-- other <reorder> elements -->
</transformGroup>
```

* * *

### <a name="Element_transform" id="Element_transform" href="#Element_transform">Element: transform</a>

* This element contains a single transform that may be performed using the keyboard layout. A transform is an element that specifies a set of conversions from sequences of code points into (one or more) other code points. For example, in most French keyboards hitting the `^` dead-key followed by the `e` key produces `ê`.


* Matches are processed against the "input context", a temporary buffer containing all relevant text up to the insertion point. If the user moves the insertion point, the input context is discarded and recreated from the application’s text buffer.  Implementations may discard the input context at any time.


The input context may contain, besides regular text, any [Markers](#markers) as a result of keys or transforms, since the insertion point was moved.

* Using regular expression terminology, matches are done as if there was an implicit `$` (match end of buffer) at the end of each pattern. In other words, `<transform from="ke" …>` will not match an input context ending with `…keyboard`, but it will match the last two codepoints of an input context ending with `…awake`.


* All of the `transform` elements in a `transformGroup` are tested for a match, in order, until a match is found. Then, the matching element is processed, and then processing proceeds to the **next** `transformGroup`. If none of the `transform` elements match, processing proceeds without modification to the buffer to the **next** `transformGroup`.


**Syntax**

```xml
<transform from="…matching pattern" to="…output pattern"/>
```

> <small>
>
> Parents: [transformGroup](#element-transformgroup)
> Children: _none_
> Occurrence: required, multiple
>
> </small>


_Attribute:_ `from` (required)

> The `from` attribute value consists of an input rule for matching the input context.
>
> The `transform` rule and output pattern uses a modified, mostly subsetted, regular expression syntax, with EcmaScript syntax (with the `u` Unicode flag) as its baseline reference (see [MDN-REGEX](https://developer.mozilla.org/docs/Web/JavaScript/Guide/Regular_Expressions)). Differences from regex implementations will be noted.

#### <a name="Regexlike_Syntax" id="Regexlike_Syntax" href="#Regexlike_Syntax">Regex-like Syntax</a>

- **Simple matches**

    `abc` `𐒵`

- **Unicode codepoint escapes**

    `\u{1234} \u{012A}`
    `\u{22} \u{012a} \u{1234A}`

    The hex escaping is case insensitive. The value may not match a surrogate or illegal character, nor a marker character.
    The form `\u{…}` is preferred as it is the same regardless of codepoint length.

- **Fixed character classes**

    `\s \S \t \r \n \f \v \d \w \D \W`

    The value of these classes do not change with Unicode versions.

    `\s` for example is exactly `[\f\n\r\t\v\u{00a0}\u{1680}\u{2000}-\u{200a}\u{2028}\u{2029}\u{202f}\u{205f}\u{3000}\u{feff}]`

- **Escapes**

    `\. \( \) \? \[ \\ \] \{ \} \* \/ \^ \+ \| \$`

    For example, `\\`, `\*`, and `\$` match `\`, `*`, and `$`, respectively.

    Some of these characters (such as `*`) aren't actually used as syntax in the keyboard transform syntax.
    However, they are required to be escaped in keyboard transforms, to avoid confusion or problems with characters which are syntax in regular expressions.

    Sequences not listed here as **Fixed Character Classes** nor as **Escapes** are disallowed.
    For example:
    * `\0` (octal escape) and `\1` (backreference) are not allowed.
    * `\a` is not defined as a character class and is also disallowed.

- **Character classes**

    `[abc]` `[^def]` `[a-z]` `[ॲऄ-आइ-ऋ]` `[\u{093F}-\u{0944}\u{0962}\u{0963}]`

    If the character class begins with a caret (`^`) then it is a negation, matching all characters except for those listed.

    Unicode properties such as `\p{…}` are not allowed.

    One additional escape is allowed within character classes besides those listed above: `\-`, for escaping the hyphen character.

* ****Note**: Character classes**: **Note**: Character classes look superficially similar to [`uset`](#element-uset) elements, but they are distinct and referenced with the `$[...usetId]` notation in transforms. The `uset` notation cannot be embedded directly in a transform.


- **Bounded quantifier**

    `{x,y}`

    `x` and `y` are required single digits (`0` to `9`) representing the minimum and maximum number of occurrences.

    `x` must be ≥ 0, `y` must be ≥ x and ≥ 1.

    Unbounded quantifiers such as `{3,}` are not allowed.

- **Optional Specifier**

    `?` - equivalent of `{0,1}`

- **Numbered Capture Groups**

    `([abc])([def])` (up to 9 groups)

    These refer to groups captured as a set, and can be referenced with the `$1` through `$9` operators in the `to=` pattern. May not be nested.

- **Non-capturing groups**

    `(?:thismatches)`

- **Nested capturing groups**

    `(?:[abc]([def]))|(?:[ghi])`

    Capture groups may be nested, however only the innermost group is allowed to be a capture group. The outer group must be a non-capturing group.

- **Disjunctions**

    `abc|def`

    Match either `abc` or `def`.

- **Match a single Unicode codepoint**

    `.`

    Matches a codepoint, not individual code units. (See the ’u’ option in EcmaScript262 regex.)
    For example, Osage `𐒵` is one match (`.`) not two.
    Does not match [markers](#markers). (See `\m{.}` and `\m{marker}`, below.)

- **Match the start of the text context**

    `^`

    The start of the context could be the start of a line, a grid cell, or some other formatting boundary.
    See description at the top of [`transforms`](#element-transform).

#### <a name="Additional_Features" id="Additional_Features" href="#Additional_Features">Additional Features</a>

The following are additions to standard Regex syntax.

- **Match a Marker**

    `\m{Some_Marker}`

    Matches the named marker.
    Also see [Markers](#markers).

- **Match a single marker**

    `\m{.}`

    Matches any single marker.
    Also see [Markers](#markers).

- **String Variables**

    `${zwnj}`

* In this usage, the variable with `id="zwnj"` will be substituted in at this point in the expression. The variable can contain a range, a character, or any other portion of a pattern. If `zwnj` is a simple string, the pattern will match that string at this point.


- **`set` or `uset` variables**

    `$[upper]`

* Given a space-separated `set` or `uset` variable, this syntax will match _any_ of the substrings. This expression may be thought of  (and implemented) as if it were a _non-capturing group_. It may, however, be enclosed within a capturing group. For example, the following definition of `$[upper]` will match as if it were written `(?:A|B|CC|D|E|FF)`.


    ```xml
    `<variables>`
        `<set id="upper" value=" A B CC  D E  FF " />`
    `</variables>`
```

    This expression in a `from=` may be used to **insert a mapped variable**, see below under [Replacement syntax](#replacement-syntax).

#### <a name="Disallowed_Regex_Features" id="Disallowed_Regex_Features" href="#Disallowed_Regex_Features">Disallowed Regex Features</a>

- **Matching an empty string**

    Transforms may not match an empty string. For example, `<transform from=""/>` or `<transform from="X{0,1}"/>` are not allowed and must be flagged as an error to keyboard authors.

- **Unicode properties**

    `\p{property}` `\P{property}`

    **Rationale:** The behavior of this feature varies by Unicode version, and so would not have predictable results.

* Tooling may choose to suggest an expansion of properties, such as `\p{Mn}` to all non spacing marks for a certain Unicode version.  As well, a set of variables could be constructed in an `import`-able file matching particularly useful Unicode properties.


    ```xml
    `<uset id="Mn" value="[\u{034F}\u{0591}-\u{05AF}\u{05BD}\u{05C4}\u{05C5}\…]" />` <!-- 1,985 code points -->
```

- **Backreferences**

    `([abc])-\1` `\k`<something>``

    **Rationale:** Implementation and cognitive complexity.

- **Unbounded Quantifiers**

    `* + *? +? {1,} {0,}`

    **Rationale:** Implementation and Computational complexity.

- **Nested capture groups**

    `((a|b|c)|(d|e|f))`

    **Rationale:** Computational and cognitive complexity.

- **Named capture groups**

    `(?`<something>`)`

    **Rationale:** Implementation complexity.

- **Assertions** other than `^`

    `\b` `\B` `(?<!…)` …

    **Rationale:** Implementation complexity.

- **End marker**

    `$`

    The end marker can be thought of as being implicitly at the end of every `from=` pattern, matching the insertion point. Transforms do not match past the insertion point.

_Attribute:_ `to`

> This attribute value represents the characters that are output from the transform.
>
> If this attribute is absent, it indicates that the no characters are output, such as with a backspace transform.
>
> A final rule such as `<transform from=".*"/>` will remove all context which doesn’t match one of the prior rules.

#### <a name="Replacement_syntax" id="Replacement_syntax" href="#Replacement_syntax">Replacement syntax</a>

Used in the `to=`

- **Literals**

    `$$ \$ \\` = `$ $ \`

- **Entire matched substring**

    `$0`

- **Insert the specified capture group**

    `$1 $2 $3 … $9`

- **Insert an entire variable**

    `${variable}`

    The entire contents of the named variable will be inserted at this point.

- **Insert a mapped set**

    `$[1:variable]` (Where "1" is any numbered capture group from 1 to 9)

    Maps capture group 1 to variable `variable`. The `from=` side must also contain a grouped variable. This expression may appear anywhere or multiple times in the `to=` pattern.

    **Example**

    ```xml
    `<set id="upper" value="A B CC D E  FF       G" />`
    `<set id="lower" value="a b c  d e  \u{0192} g" />`
    <!-- note that values may be spaced for ease of reading -->
    …
    `<transform from="($[upper])" to="$[1:lower]" />`
```

* - The capture group on the `from=` side **must** contain exactly one set variable.  `from="Q($[upper])X"` can be used (other context before or after the capture group), but `from="(Q$[upper])"` may not be used with a mapped variable and is flagged as an error.


    - The `from=` and `to=` sides of the pattern must both be using `set` variables. There is no way to insert a set literal on either side and avoid using a variable.

* - The two variables (here `upper` and `lower`) must have exactly the same number of whitespace-separated items. Leading and trailing space is ignored. A variable without any spaces is considered to be a set variable of exactly one item.


* - As described in [Additional Features](#additional-features), the `upper` set variable as used here matches as if it is `((?:A|B|CC|D|E|FF|G))`, showing the enclosing capturing group. When text from the input context matches this expression, and all above conditions are met, the mapping proceeds as follows:


    1. The portion of the input context, such as `CC`, is matched against the above calculated pattern.

    2. The position within the `from=` variable (`upper`) is calculated. The regex match may not have this information, but the matched substring `CC` can be compared against the tokenized input variable: `A`, `B`, `CC`, `D`, … to find that the 3rd item matches exactly.

    3. The same position within the `to=` variable (`lower`) is calculated. The 3rd item is `c`.

    4. `CC` in the input context is replaced with `c`, and processing proceeds to the next `transformGroup`.

- **Emit a marker**

    `\m{Some_marker}`

    Emits the named mark. Also see [Markers](#markers).

#### <a name="Transform_Grammar" id="Transform_Grammar" href="#Transform_Grammar">Transform Grammar</a>

#### <a name="Transform_From_Grammar" id="Transform_From_Grammar" href="#Transform_From_Grammar">Transform From Grammar</a>

The `from=` attribute MUST match the `from-match` rule in this grammar. Not all strings which match this grammar are valid, specifically

The following is the [LDML EBNF](tr35.md#ebnf) format for the grammar:

```ebnf
[ wfc: No more than 9 capture groups may be present. ]
[ vc: all variables referenced must be defined in the <variables> element ]

from-match
         ::= '^'? atoms
atoms    ::= atom ( '|'? atom )*
atom     ::= quark quantifier?
quark    ::= non-group
           | group
non-group
         ::= simple-matcher
           | escaped-codepoints
           | variable
variable ::= string-variable
           | set-variable
string-variable
         ::= '${' var-id '}'
set-variable
         ::= '$[' var-id ']'
var-id   ::= IDCHAR+
group    ::= capturing-group
           | non-capturing-group
quantifier
         ::= bounded-quantifier
           | '?'
escaped-codepoints
         ::= '\' 'u' '{' codepoints-hex '}'
escaped-codepoint
         ::= '\' 'u' '{' codepoint-hex '}'
bounded-quantifier
         ::= '{' DIGIT ',' DIGIT '}'
non-capturing-group
         ::= '(' '?' ':' atoms ')'
capturing-group
         ::= '(' catoms ')'
catoms   ::= catom+
catom    ::= cquark quantifier?
cquark   ::= non-group
codepoints-hex
         ::= codepoint-hex ( ' ' codepoint-hex )*
codepoint-hex
         ::= LHEXDIG ( LHEXDIG ( LHEXDIG ( LHEXDIG ( LHEXDIG LHEXDIG? )? )? )? )?
simple-matcher
         ::= text-char
           | class
           | '.'
           | match-marker
match-marker
         ::= '\m{.}'
           | match-named-marker
match-named-marker
         ::= '\m{' marker-id '}'
marker-id
         ::= NMTOKEN
class    ::= fixed-class
           | set-class
fixed-class
         ::= '\' fixed-class-char
fixed-class-char
         ::= 's'
           | 'S'
           | 't'
           | 'r'
           | 'n'
           | 'f'
           | 'v'
           | 'd'
           | 'w'
           | 'D'
           | 'W'
set-class
         ::= '[' set-negator set-members ']'
set-members
         ::= set-member+
set-member
         ::= char-range
           | range-char
           | match-marker
           | escaped-codepoint
char-range
         ::= range-edge '-' range-edge
range-edge
         ::= escaped-codepoint
           | range-char
set-negator
         ::= '^'?
text-char
         ::= content-char
           | ws
           | escaped-char
           | '-'
           | ':'
range-char
         ::= content-char
           | ws
           | escaped-range-char
           | '.'
           | '|'
           | '{'
           | '}'
content-char
         ::= ASCII-PUNCT
           | ALPHA
           | DIGIT
           | NON-ASCII
escaped-char
         ::= '\' escapable-char
escapable-char
         ::= '.'
           | '('
           | ')'
           | '?'
           | '['
           | '\'
           | ']'
           | '{'
           | '}'
           | '*'
           | '/'
           | '^'
           | '+'
           | '|'
           | '$'
escaped-range-char
         ::= '\' escapable-range-char
escapable-range-char
         ::= escapable-char
           | '-'
ws       ::= [ #x3000]
           | HTAB
           | CR
           | LF
IDCHAR   ::= ALPHA
           | DIGIT
           | '_'
ASCII-PUNCT
         ::= [!-#%-',/;->_`#x7E-#x7F]
NON-ASCII
         ::= [#x7E-#xD7FF#xE000-#x10FFFF]
DIGIT    ::= [0-9]
ALPHA    ::= [A-Za-z]
HTAB     ::= #xF900
LF       ::= #xA
CR       ::= #xD
HEXDIG   ::= DIGIT
           | 'A'
           | 'B'
           | 'C'
           | 'D'
           | 'E'
           | 'F'
LHEXDIG  ::= HEXDIG
           | 'a'
           | 'b'
           | 'c'
           | 'd'
           | 'e'
           | 'f'
NAMESTARTCHAR
         ::= [:_#xC0-#xD6#xD8-#xF6#xF8-#x2FF#x370-#x37D#x37F-#x1FFF#x200C-#x200D#x2070-#x218F#x2C00-#x2FEF#x3001-#xD7FF#xF900-#xFDCF#xFDF0-#xFFFD#x10000-#x10FFFF]
           | ALPHA
NAMECHAR ::= NAMESTARTCHAR
           | [-.#xB7#x300-#x36F#x203F-#x2040]
           | DIGIT
NMTOKEN  ::= NAMECHAR+
```

#### <a name="Transform_To_Grammar" id="Transform_To_Grammar" href="#Transform_To_Grammar">Transform To Grammar</a>

This is the grammar for the `<transform to="…"/>` attribute.  The `to=` attribute MUST match the `to-replacement` rule in this grammar. Not all strings which match this grammar are valid:

The following is the [LDML EBNF](tr35.md#ebnf) format for the grammar:

```ebnf
[ vc: A referenced capture group must be present in the from= match string. ]
[ vc: The `$[1:…]` set format may only be used where there is exactly one capture group with a set variable on the from= match string. ]
[ vc: all variables referenced must be defined in the <variables> element ]

to-replacement
         ::= atoms
atoms    ::= atom*
atom     ::= replacement-char
           | escaped-char
           | group-reference
           | escaped-codepoints
           | named-marker
           | string-variable
           | mapped-set
replacement-char
         ::= content-char
           | ws
           | '-'
           | ':'
           | '('
           | ')'
           | '.'
           | '*'
           | '+'
           | '?'
           | '['
           | ']'
           | '^'
           | '{'
           | '}'
           | '|'
escaped-char
         ::= '\' ( '\' | '$' )
           | '$$'
group-reference
         ::= '$' DIGIT
escaped-codepoints
         ::= '\' 'u' '{' codepoints-hex '}'
codepoints-hex
         ::= codepoint-hex ( ' ' codepoint-hex )*
codepoint-hex
         ::= LHEXDIG ( LHEXDIG ( LHEXDIG ( LHEXDIG ( LHEXDIG LHEXDIG? )? )? )? )?
named-marker
         ::= '\m{' marker-id '}'
marker-id
         ::= NMTOKEN
string-variable
         ::= '${' var-id '}'
var-id   ::= IDCHAR+
mapped-set
         ::= '$[1:' var-id ']'
content-char
         ::= ASCII-PUNCT
           | ALPHA
           | DIGIT
           | NON-ASCII
ws       ::= [ #x3000]
           | HTAB
           | CR
           | LF
IDCHAR   ::= ALPHA
           | DIGIT
           | '_'
ASCII-PUNCT
         ::= [!-#%-',/;->_`#x7E-#x7F]
NON-ASCII
         ::= [#x7E-#xD7FF#xE000-#x10FFFF]
DIGIT    ::= [0-9]
ALPHA    ::= [A-Za-z]
HTAB     ::= #xF900
LF       ::= #xA
CR       ::= #xD
HEXDIG   ::= DIGIT
           | 'A'
           | 'B'
           | 'C'
           | 'D'
           | 'E'
           | 'F'
LHEXDIG  ::= HEXDIG
           | 'a'
           | 'b'
           | 'c'
           | 'd'
           | 'e'
           | 'f'
NAMESTARTCHAR
         ::= [:_#xC0-#xD6#xD8-#xF6#xF8-#x2FF#x370-#x37D#x37F-#x1FFF#x200C-#x200D#x2070-#x218F#x2C00-#x2FEF#x3001-#xD7FF#xF900-#xFDCF#xFDF0-#xFFFD#x10000-#x10FFFF]
           | ALPHA
NAMECHAR ::= NAMESTARTCHAR
           | [-.#xB7#x300-#x36F#x203F-#x2040]
           | DIGIT
NMTOKEN  ::= NAMECHAR+
```

#### <a name="ABNF" id="ABNF" href="#ABNF">ABNF</a>

The grammar for the transform rules is also available in ABNF notation [[STD68](https://www.rfc-editor.org/info/std68)],
including the modifications found in [RFC 7405](https://www.rfc-editor.org/rfc/rfc7405).

RFC7405 defines a variation of ABNF that is case-sensitive.
Some ABNF tools are only compatible with the specification found in
[RFC 5234](https://www.rfc-editor.org/rfc/rfc5234).

The ABNF files are located in the `keyboards/abnf` directory in the CLDR source directory.  (The EBNF above was converted from the ABNF files.)

 * `transform-from-required.abnf`
 * `transform-to-required.abnf`

* * *

### <a name="Element_reorder" id="Element_reorder" href="#Element_reorder">Element: reorder</a>

* The reorder transform consists of a [`<transformGroup>`](#element-transformgroup) element containing `<reorder>` elements.  Multiple such `<transformGroup>` elements may be contained in an enclosing `<transforms>` element.


One or more [`<import>`](#element-import) elements are allowed to precede the `<reorder>` elements.

* This transform has the job of reordering sequences of characters that have been typed, from their typed order to the desired output order. The primary concern in this transform is to sort combining marks into their correct relative order after a base, as described in this section. The reorder transforms can be quite complex, keyboard layouts will almost always import them.


The reordering algorithm consists of four parts:

1. Create a sort key for each character in the input string. A sort key has 4 parts (primary, index, tertiary, quaternary):
   * The **primary weight** is the primary order value.
   * The **secondary weight** is the index, a position in the input string, usually of the character itself, but it may be of a character earlier in the string.
   * The **tertiary weight** is a tertiary order value (defaulting to 0).
   * The **quaternary weight** is the index of the character in the string. This is solely to ensure a stable sort for sequences of characters with the same tertiary weight.
2. Mark each character as to whether it is a prebase character, one that is typed before the base and logically stored after. Thus it will have a primary order > 0.
3. Use the sort key and the prebase mark to identify runs. A run starts with a prefix that contains any prebase characters and a single base character whose primary and tertiary key is 0. The run extends until, but not including, the start of the prefix of the next run or end of the string.
   * `run := preBase* (primary=0 && tertiary=0) ((primary≠0 || tertiary≠0) && !preBase)*`
4. Sort the character order of each character in the run based on its sort key.

* The primary order of a character with the Unicode property `Canonical_Combining_Class` (ccc) of 0 may well not be 0. In addition, a character may receive a different primary order dependent on context. For example, in the Devanagari sequence ka halant ka, the first ka would have a primary order 0 while the halant ka sequence would give both halant and the second ka a primary order > 0, for example 2. Note that “base” character in this discussion is not a Unicode base character. It is instead a character with primary=0.


* In order to get the characters into the correct relative order, it is necessary not only to order combining marks relative to the base character, but also to order some combining marks in a subsequence following another combining mark. For example in Devanagari, a nukta may follow a consonant character, but it may also follow a conjunct consisting of consonant, halant, consonant. Notice that the second consonant is not, in this model, the start of a new run because some characters may need to be reordered to before the first base, for example repha. The repha would get primary < 0, and be sorted before the character with order = 0, which is, in the case of Devanagari, the initial consonant of the orthographic syllable.


* The reorder transform consists of `<reorder>` elements encapsulated in a `<transformGroup>` element. Each element is a rule that matches against a string of characters with the action of setting the various ordering attributes (`primary`, `tertiary`, `tertiaryBase`, `preBase`) for the matched characters in the string.


The relative ordering of `<reorder>` elements is not significant.

**Syntax**

```xml
<transformGroup>
    <!-- one or more <import/> elements are allowed at this point -->
    <reorder from="…combination of characters"
    before="…look-behind required match"
    order="…list of weights"
    tertiary="…list of weights"
    tertiaryBase="…list of true/false"
    preBase="…list of true/false" />
    <!-- other <reorder/> elements… -->
</transformGroup>
```

> <small>
>
> Parents: [transformGroup](#element-transformgroup)
> Children: _none_
> Occurrence: optional, multiple
>
> </small>

_Attribute:_ `from` (required)

> This attribute value contains a string of elements. Each element matches one character and may consist of a codepoint or a UnicodeSet (both as defined in [UTS #35 Part One](tr35.md#Unicode_Sets)).

_Attribute:_ `before`

> This attribute value contains the element string that must match the string immediately preceding the start of the string that the @from matches.

_Attribute:_ `order`

> This attribute value gives the primary order for the elements in the matched string in the `@from` attribute. The value is a simple integer between -128 and +127 inclusive, or a space separated list of such integers. For a single integer, it is applied to all the elements in the matched string. Details of such list type attributes are given after all the attributes are described. If missing, the order value of all the matched characters is 0. We consider the order value for a matched character in the string.
>
> * If the value is 0 and its tertiary value is 0, then the character is the base of a new run.
> * If the value is 0 and its tertiary value is non-zero, then it is a normal character in a run, with ordering semantics as described in the `@tertiary` attribute.
> * If the value is negative, then the character is a primary character and will reorder to be before the base of the run.
> * If the value is positive, then the character is a primary character and is sorted based on the order value as the primary key following a previous base character.
>
> A character with a zero tertiary value is a primary character and receives a sort key consisting of:
>
> * Primary weight is the order value
> * Secondary weight is the index of the character. This may be any value (character index, codepoint index) such that its value is greater than the character before it and less than the character after it.
> * Tertiary weight is 0.
> * Quaternary weight is the same as the secondary weight.

_Attribute:_ `tertiary`

> This attribute value gives the tertiary order value to the characters matched. The value is a simple integer between -128 and +127 inclusive, or a space separated list of such integers. If missing, the value for all the characters matched is 0. We consider the tertiary value for a matched character in the string.
>
> * If the value is 0 then the character is considered to have a primary order as specified in its order value and is a primary character.
> * If the value is non zero, then the order value must be zero otherwise it is an error. The character is considered as a tertiary character for the purposes of ordering.
>
> A tertiary character receives its primary order and index from a previous character, which it is intended to sort closely after. The sort key for a tertiary character consists of:
>
> * Primary weight is the primary weight of the primary character..
> * Secondary weight is the index of the primary character, not the tertiary character
> * Tertiary weight is the tertiary value for the character.
> * Quaternary weight is the index of the tertiary character.

_Attribute:_ `tertiaryBase`

> This attribute value is a space separated list of `"true"` or `"false"` values corresponding to each character matched. It is illegal for a tertiary character to have a true `tertiaryBase` value. For a primary character it marks that this character may have tertiary characters moved after it. When calculating the secondary weight for a tertiary character, the most recently encountered primary character with a true `tertiaryBase` attribute value is used. Primary characters with an `@order` value of 0 automatically are treated as having `tertiaryBase` true regardless of what is specified for them.

_Attribute:_ `preBase`

> This attribute value gives the prebase attribute for each character matched. The value may be `"true"` or `"false"` or a space separated list of such values. If missing the value for all the characters matched is false. It is illegal for a tertiary character to have a true prebase value.
>
> If a primary character has a true prebase value then the character is marked as being typed before the base character of a run, even though it is intended to be stored after it. The primary order gives the intended position in the order after the base character, that the prebase character will end up. Thus `@order` shall not be 0. These characters are part of the run prefix. If such characters are typed then, in order to give the run a base character after which characters can be sorted, an appropriate base character, such as a dotted circle, is inserted into the output run, until a real base character has been typed. A value of `"false"` indicates that the character is not a prebase.

* For `@from` attribute values with a match string length greater than 1, the sort key information (`@order`, `@tertiary`, `@tertiaryBase`, `@preBase`) may consist of a space-separated list of values, one for each element matched. The last value is repeated to fill out any missing values. Such a list may not contain more values than there are elements in the `@from` attribute:


```java
if len(@from) < len(@list) then error
else
    while len(@from) > len(@list)
        append lastitem(@list) to @list
    endwhile
endif
```

**Example**

For example, consider the Northern Thai (`nod-Lana`, Tai Tham script) word: ᨡ᩠ᩅᩫ᩶ 'roasted'. This is ideally encoded as the following:

| name | _kha_ | _sakot_ | _wa_ | _o_  | _t2_ |
|------|-------|---------|------|------|------|
| code | 1A21  | 1A60    | 1A45 | 1A6B | 1A76 |
| ccc  | 0     | 9       | 0    | 0    | 230  |

(That sequence is already in NFC format.)

Some users may type the upper component of the vowel first, and the tone before or after the lower component. Thus someone might type it as:

| name | _kha_ | _o_  | _t2_ | _sakot_ | _wa_ |
|------|-------|------|------|---------|------|
| code | 1A21  | 1A6B | 1A76 | 1A60    | 1A45 |
| ccc  | 0     | 0    | 230  | 9       | 0    |

The Unicode NFC format of that typed value reorders to:

| name | _kha_ | _o_  | _sakot_ | _t2_ | _wa_ |
|------|-------|------|---------|------|------|
| code | 1A21  | 1A6B | 1A60    | 1A76 | 1A45 |
| ccc  | 0     | 0    | 9       | 230  | 0    |

Finally, the user might also type in the sequence with the tone _after_ the lower component.

| name | _kha_ | _o_  | _sakot_ | _wa_ | _t2_ |
|------|-------|------|---------|------|------|
| code | 1A21  | 1A6B | 1A60    | 1A45 | 1A76 |
| ccc  | 0     | 0    | 9       | 0    | 230  |

(That sequence is already in NFC format.)

We want all of these sequences to end up ordered as the first. To do this, we use the following rules:

```xml
<reorder from="\u{1A60}" order="127" />      <!-- max possible order -->
<reorder from="\u{1A6B}" order="42" />
<reorder from="[\u{1A75}-\u{1A79}]" order="55" />
<reorder before="\u{1A6B}" from="\u{1A60}\u{1A45}" order="10" />
<reorder before="\u{1A6B}[\u{1A75}-\u{1A79}]" from="\u{1A60}\u{1A45}" order="10" />
<reorder before="\u{1A6B}" from="\u{1A60}[\u{1A75}-\u{1A79}]\u{1A45}" order="10 55 10" />
```

* The first reorder is the default ordering for the _sakot_ which allows for it to be placed anywhere in a sequence, but moves any non-consonants that may immediately follow it, back before it in the sequence. The next two rules give the orders for the top vowel component and tone marks respectively. The next three rules give the _sakot_ and _wa_ characters a primary order that places them before the _o_. Notice particularly the final reorder rule where the _sakot_+_wa_ is split by the tone mark. This rule is necessary in case someone types into the middle of previously normalized text.


* **` elements are**: `<reorder>` elements are priority ordered based first on the length of string their `@from` attribute value matches and then the sum of the lengths of the strings their `@before` attribute value matches.


#### <a name="Using_with_elements" id="Using_with_elements" href="#Using_with_elements">Using `<import>` with `<reorder>` elements</a>

This section describes the impact of using [`import`](#element-import) elements with `<reorder>` elements.

* The @from string in a `<reorder>` element describes a set of strings that it matches. This also holds for the `@before` attribute. The **intersection** of any two `<reorder>` elements consists of the intersections of their `@from` and `@before` string sets. Tooling should warn users if the intersection between any two `<reorder>` elements in the same `<transformGroup>` element to be non empty prior to processing imports.


* If two `<reorder>` elements have a non empty intersection, then they are split and merged. They are split such that where there were two `<reorder>` elements, there are, in effect (but not actuality), three elements consisting of:


* `@from`, `@before` that match the intersection of the two rules. The other attribute values are merged, as described below.
* `@from`, `@before` that match the set of strings in the first rule not in the intersection with the other attribute values from the first rule.
* `@from`, `@before` that match the set of strings in the second rule not in the intersection, with the other attribute values from the second rule.

* When merging the other attributes, the second rule is taken to have priority (being an override of the earlier element). Where the second rule does not define the value for a character but the first does, the value is taken from the first rule, otherwise it is taken from the second rule.


* Notice that it is possible for two rules to match the same string, but for them not to merge because the distribution of the string across `@before` and `@from` is different. For example, the following would not merge:


```xml
<reorder before="ab" from="cd" />
<reorder before="a" from="bcd" />
```

After `<reorder>` elements merge, the resulting `reorder` elements are sorted into priority order for matching.

Consider this fragment from a shared reordering for the Myanmar script:

```xml
<!-- File: "myanmar-reordering.xml" -->
<transformGroup>
    <!-- medial-r -->
    <reorder from="\u{103C}" order="20" />

    <!-- [medial-wa or shan-medial-wa] -->
    <reorder from="[\u{103D}\u{1082}]" order="25" />

    <!-- [medial-ha or shan-medial-wa]+asat = Mon asat -->
    <reorder from="[\u{103E}\u{1082}]\u{103A}" order="27" />

    <!-- [medial-ha or mon-medial-wa] -->
    <reorder from="[\u{103E}\u{1060}]" order="27" />

    <!-- [e-vowel (U+1031) or shan-e-vowel (U+1084)] -->
    <reorder from="[\u{1031}\u{1084}]" order="30" />

    <reorder from="[\u{102D}\u{102E}\u{1033}-\u{1035}\u{1071}-\u{1074}\u{1085}\u{109D}\u{A9E5}]" order="35" />
</transformGroup>
```

A particular Myanmar keyboard layout can have these `reorder` elements:

```xml
<transformGroup>
    <import path="myanmar-reordering.xml"/> <!-- import the above transformGroup -->
    <!-- Kinzi -->
    <reorder from="\u{1004}\u{103A}\u{1039}" order="-1" />

    <!-- e-vowel -->
    <reorder from="\u{1031}" preBase="1" />

    <!-- medial-r -->
    <reorder from="\u{103C}" preBase="1" />
</transformGroup>
```

* The effect of this is that the _e-vowel_ will be identified as a prebase and will have an order of 30. Likewise a _medial-r_ will be identified as a prebase and will have an order of 20. Notice that a _shan-e-vowel_ (`\u{1084}`) will not be identified as a prebase (even if it should be!). The _kinzi_ is described in the layout since it moves something across a run boundary. By separating such movements (prebase or moving to in front of a base) from the shared ordering rules, the shared ordering rules become a self-contained combining order description that can be used in other keyboards or even in other contexts than keyboarding.


#### <a name="Example_Postreorder_transforms" id="Example_Postreorder_transforms" href="#Example_Postreorder_transforms">Example Post-reorder transforms</a>

* It may be desired to perform additional processing following reorder operations.  This may be aaccomplished by adding an additional `<transformGroup>` element after the group containing `<reorder>` elements.


First, a partial example from Khmer where split vowels are combined after reordering.

```xml
…
<transformGroup>
    <reorder … />
    <reorder … />
    <reorder … />
    …
</transformGroup>
<transformGroup>
    <transform from="\u{17C1}\u{17B8}" to="\u{17BE}" />
    <transform from="\u{17C1}\u{17B6}" to="\u{17C4}" />
</transformGroup>
```

Another partial example allows a keyboard implementation to prevent people typing two lower vowels in a Burmese cluster:

```xml
…
<transformGroup>
    <reorder … />
    <reorder … />
    <reorder … />
    …
</transformGroup>
<transformGroup>
    <transform from="[\u{102F}\u{1030}\u{1048}\u{1059}][\u{102F}\u{1030}\u{1048}\u{1059}]"  />
</transformGroup>
```

#### <a name="Reorder_and_Markers" id="Reorder_and_Markers" href="#Reorder_and_Markers">Reorder and Markers</a>

* Markers are not matched by `reorder` elements. However, if a character preceded by one or more markers is reordered due to a `reorder` element, those markers will be reordered with the characters, maintaining the same relative order.  This is a similar process to the algorithm used to normalize strings processed by `transform` elements.


Keyboard implementations must process `reorder` elements using the following algorithm.

Note that steps 1 and 3 are identical to the steps used for normalization using markers in the [Marker Algorithm Overview](#marker-algorithm-overview).

Given an input string from context or from a previous `transformGroup`:

1. Parsing/Removing Markers

2. Perform reordering (as in this section)

3. Re-Adding Markers

* * *

### <a name="Backspace_Transforms" id="Backspace_Transforms" href="#Backspace_Transforms">Backspace Transforms</a>

* The `<transforms type="backspace">` describe an optional transform that is not applied on input of normal characters, but is only used to perform extra backspace modifications to previously committed text.


When the backspace key is pressed, the `<transforms type="backspace">` element (if present) is processed, and then the `<transforms type="simple">` element (if processed) as with any other key.

Keyboarding applications typically work, but are not required to, in one of two modes:

**_text entry_**

> text entry happens while a user is typing new text. A user typically wants the backspace key to undo whatever they last typed, whether or not they typed things in the 'right' order.

**_text editing_**

> text editing happens when a user moves the cursor into some previously entered text which may have been entered by someone else. As such, there is no way to know in which order things were typed, but a user will still want appropriate behavior when they press backspace. This may involve deleting more than one character or replacing a sequence of characters with a different sequence.

* In text editing mode, different keyboard layouts may behave differently in the same textual context. The backspace transform allows the keyboard layout to specify the effect of pressing backspace in a particular textual context. This is done by specifying a set of backspace rules that match a string before the cursor and replace it with another string. The rules are expressed within a `transforms type="backspace"` element.



```xml
<transforms type="backspace">
    <transformGroup>
        <transform from="…match pattern" to="…output pattern" />
    </transformGroup>
</transforms>
```

**Example**

For example, consider deleting a Devanagari ksha क्श:

While this character is made up of three codepoints, the following rule causes all three to be deleted by a single press of the backspace.


```xml
<transforms type="backspace">
    <transformGroup>
        <transform from="\u{0915}\u{094D}\u{0936}"/>
    </transformGroup>
</transforms>
```

Note that the optional attribute `@to` is omitted, since the whole string is being deleted. This is not uncommon in backspace transforms.

A more complex example comes from a Burmese visually ordered keyboard:

```xml
<transforms type="backspace">
    <transformGroup>
        <!-- Kinzi -->
        <transform from="[\u{1004}\u{101B}\u{105A}]\u{103A}\u{1039}" />

        <!-- subjoined consonant -->
        <transform from="\u{1039}[\u{1000}-\u{101C}\u{101E}\u{1020}\u{1021}\u{1050}\u{1051}\u{105A}-\u{105D}]" />

        <!-- tone mark -->
        <transform from="\u{102B}\u{103A}" />

        <!-- Handle prebases -->
        <!-- diacritics stored before e-vowel -->
        <transform from="[\u{103A}-\u{103F}\u{105E}-\u{1060}\u{1082}]\u{1031}" to="\u{1031}" />

        <!-- diacritics stored before medial r -->
        <transform from="[\u{103A}-\u{103B}\u{105E}-\u{105F}]\u{103C}" to="\u{103C}" />

        <!-- subjoined consonant before e-vowel -->
        <transform from="\u{1039}[\u{1000}-\u{101C}\u{101E}\u{1020}\u{1021}]\u{1031}" to="\u{1031}" />

        <!-- base consonant before e-vowel -->
        <transform from="[\u{1000}-\u{102A}\u{103F}-\u{1049}\u{104E}]\u{1031}" to="\m{prebase}\u{1031}" />

        <!-- subjoined consonant before medial r -->
        <transform from="\u{1039}[\u{1000}-\u{101C}\u{101E}\u{1020}\u{1021}]\u{103C}" to="\u{103C}" />

        <!-- base consonant before medial r -->
        <transform from="[\u{1000}-\u{102A}\u{103F}-\u{1049}\u{104E}]\u{103C}" to="\m{prebase}\u{103C}" />

        <!-- delete lone medial r or e-vowel -->
        <transform from="\m{prebase}[\u{1031}\u{103C}]" />
    </transformGroup>
</transforms>
```

The above example is simplified, and doesn't fully handle the interaction between medial-r and e-vowel.


> The character `\m{prebase}` does not represent a literal character, but is instead a special marker, used as a "filler string". When a keyboard implementation handles a user pressing a key that inserts a prebase character, it also has to insert a special filler string before the prebase to ensure that the prebase character does not combine with the previous cluster. See the reorder transform for details. See [markers](#markers) for the `\m` syntax.

* The first three transforms above delete various ligatures with a single keypress. The other transforms handle prebase characters. There are two in this Burmese keyboard. The transforms delete the characters preceding the prebase character up to base which gets replaced with the prebase filler string, which represents a null base. Finally the prebase filler string + prebase is deleted as a unit.


#### <a name="Default_Backspace_Transform" id="Default_Backspace_Transform" href="#Default_Backspace_Transform">Default Backspace Transform</a>

* If no specified transform among all `transformGroup`s under the `<transforms type="backspace">` element matches, a default will be used instead — an implied final transform that simply deletes a single codepoint at the end of the input context.

Because the context is in NFD, this default behavior may break apart what the user considers to be one character.
* For example, if at the end of the context is the string `Dü`, in NFD form, this will be the codepoints `D` (U+0044), `u` (U+0075) followed by `¨` (U+0308). Pressing backspace once will delete the U+0308 codepoint, leaving `Du` in the context. Pressing backspace again will leave only `D`.


This implied transform is effectively similar to the following code sample, even though the `*` operator is not actually allowed in `from=`.
See the documentation for *Match a single Unicode codepoint* under [transform syntax](#regex-like-syntax) and [markers](#markers), above.

It is important that implementations do not by default delete more than one non-marker codepoint at a time, except in the case of emoji clusters.
* Note that implementations will vary in the emoji handling due to the iterative nature of successive Unicode releases. See [UTS#51 §2.4.2: Emoji Modifiers in Text](https://www.unicode.org/reports/tr51/#Emoji_Modifiers_in_Text)


Keyboard authors should almost always include backspace transforms in their keyboards, to ensure that backspacing has intuitive and expected behavior for users.
The default backspace transform described here may yield unexpected behavior for users.

```xml
<transforms type="backspace">
    <!-- Other explicit transforms -->

    <!-- Final implicit backspace transform: Delete the final codepoint. -->
    <transformGroup>
        <!-- (:?\m{.})*  - matches any number of contiguous markers -->
        <transform from="(:?\m{.})*.(:?\m{.})*" /> <!-- deletes any number of markers directly on either side of the final pre-caret codepoint -->
    </transformGroup>
</transforms>
```

* * *


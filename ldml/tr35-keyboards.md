## Unicode Technical Standard #35 Tech Preview

# Unicode Locale Data Markup Language (LDML)<br/>Part 7: Keyboards

|Version|44 (draft)   |
|-------|-------------|
|Editors|Steven Loomis (<a href="mailto:srloomis@unicode.org">srloomis@unicode.org</a>) and <a href="tr35.md#Acknowledgments">other CLDR committee members</a>|

For the full header, summary, and status, see [Part 1: Core](tr35.md).

#### _Important Note_

> This is a technical preview of a future version of the LDML Part 7. See [_Status_](#status), below.
>
> There are breaking changes, see [Compatibility Notice](#Compatibility_Notice)

### _Summary_

This document describes parts of an XML format (_vocabulary_) for the exchange of structured locale data. This format is used in the [Unicode Common Locale Data Repository](https://www.unicode.org/cldr/).

This is a partial document, describing keyboard mappings. For the other parts of the LDML see the [main LDML document](tr35.md) and the links above.

_Note:_
Some links may lead to in-development or older
versions of the data files.
See <https://cldr.unicode.org> for up-to-date CLDR release data.

### _Status_

This document is a draft of a _technical preview_ of the Keyboard standard.
This document has _not_ been approved for publication by the Unicode Consortium,
and may be substantially altered before any publication. For the latest published version of UTS#35, see <https://www.unicode.org/reports/tr35/>

In particular, Element and attribute names are expected to change pending further review.

The CLDR [Keyboard Workgroup](https://cldr.unicode.org/index/keyboard-workgroup) is currently
developing major changes to the CLDR keyboard specification.
For this draft, please see [CLDR-15034](https://unicode-org.atlassian.net/browse/CLDR-15034) for
status, the latest information, or to provide feedback.

## <a name="Parts" href="#Parts">Parts</a>

The LDML specification is divided into the following parts:

*   Part 1: [Core](tr35.md#Contents) (languages, locales, basic structure)
*   Part 2: [General](tr35-general.md#Contents) (display names & transforms, etc.)
*   Part 3: [Numbers](tr35-numbers.md#Contents) (number & currency formatting)
*   Part 4: [Dates](tr35-dates.md#Contents) (date, time, time zone formatting)
*   Part 5: [Collation](tr35-collation.md#Contents) (sorting, searching, grouping)
*   Part 6: [Supplemental](tr35-info.md#Contents) (supplemental data)
*   Part 7: [Keyboards](tr35-keyboards.md#Contents) (keyboard mappings)
*   Part 8: [Person Names](tr35-personNames.md#Contents) (person names)

## <a name="Contents" href="#Contents">Contents of Part 7, Keyboards</a>

  * [_Important Note_](#important-note)
  * [_Summary_](#summary)
  * [_Status_](#status)
* [Parts](#Parts)
* [Contents of Part 7, Keyboards](#Contents)
* [Keyboards](#Introduction)
* [Goals and Non-goals](#Goals_and_Nongoals)
  * [Compatibility Notice](#Compatibility_Notice)
  * [Accessibility](#Accessibility)
* [Definitions](#Definitions)
  * [Escaping](#Escaping)
  * [UnicodeSet Escaping](#unicodeset-escaping)
  * [UTS18 Escaping](#uts18-escaping)
* [File and Directory Structure](#File_and_Dir_Structure)
  * [Extensibility](#Extensibility)
* [Element Hierarchy](#element-hierarchy)
  * [Element: keyboard](#Element_Keyboard)
  * [Element: locales](#Element_locales)
  * [Element: locale](#Element_locale)
  * [Element: version](#Element_version)
  * [Element: info](#Element_info)
  * [Element: names](#Element_names)
  * [Element: name](#Element_name)
  * [Element: settings](#Element_settings)
  * [Element: keys](#Element_keys)
  * [Element: key](#Element_key)
    * [Implied Keys](#implied-keys)
    * [Elements: flicks, flick](#Element_flicks)
  * [Element: import](#Element_import)
  * [Element: displays](#Element_displays)
  * [Element: display](#Element_display)
  * [Element: displayOptions](#Element_displayOptions)
  * [Element: layers](#Element_layers)
  * [Element: layer](#Element_layer)
  * [Element: row](#Element_row)
  * [Element: vkeys](#Element_vkeys)
  * [Element: vkey](#Element_vkey)
  * [Element: variables](#Element_variables)
  * [Element: string](#element-string)
  * [Element: set](#element-set)
  * [Element: unicodeSet](#element-unicodeset)
  * [Element: transforms](#Element_transforms)
    * [Markers](#markers)
  * [Element: transformGroup](#Element_transformGroup)
    * [Example: `transformGroup` with `transform` elements](#example-transformgroup-with-transform-elements)
    * [Example: `transformGroup` with `reorder` elements](#example-transformgroup-with-reorder-elements)
  * [Element: transform](#Element_transform)
    * [Regex-like Syntax](#regex-like-syntax)
    * [Additional Features](#additional-features)
    * [Disallowed Regex Features](#disallowed-regex-features)
    * [Replacement syntax](#replacement-syntax)
  * [Element: reorder](#Element_reorder)
    * [Using `<import>` with `<reorder>` elements](#using-import-with-reorder-elements)
    * [Example Post-reorder transforms](#example-post-reorder-transforms)
  * [transform type="backspace"](#Element_backspaces)
* [Invariants](#Invariants)
* [Keyboard IDs](#Keyboard_IDs)
  * [Principles for Keyboard IDs](#Principles_for_Keyboard_IDs)
* [Platform Behaviors in Edge Cases](#Platform_Behaviors_in_Edge_Cases)
* [CLDR VKey Enum](#CLDR_VKey_Enum)
* [Keyboard Test Data](#keyboard-test-data)
  * [Test Doctype](#test-doctype)
  * [Test Element: keyboardTest](#test-element-keyboardtest)
  * [Test Element: info](#test-element-info)
  * [Test Element: repertoire](#test-element-repertoire)
  * [Test Element: tests](#test-element-tests)
  * [Test Element: test](#test-element-test)
  * [Test Element: startContext](#test-element-startcontext)
  * [Test Element: keystroke](#test-element-keystroke)
  * [Test Element: emit](#test-element-emit)
  * [Test Element: backspace](#test-element-backspace)
  * [Test Element: check](#test-element-check)
  * [Test Examples](#test-examples)

## <a name="Introduction" href="#Introduction">Keyboards</a>

The Unicode Standard and related technologies such as CLDR have dramatically improved the path to language support. However, keyboard support remains platform and vendor specific, causing inconsistencies in implementation as well as timeline.

> “More and more language communities are determining that digitization is vital to their approach to language preservation and that engagement with Unicode is essential to becoming fully digitized. For many of these communities, however, getting new characters or a new script added to The Unicode Standard is not the end of their journey. The next, often more challenging stage is to get device makers, operating systems, apps and services to implement the script requirements that Unicode has just added to support their language. …
>
> “However, commensurate improvements to streamline new language support on the input side have been lacking. CLDR’s new Keyboard Subcommittee has been established to address this very gap.”
> _(Cornelius et. al, “Standardizing Keyboards with CLDR,” presented at the 45th Internationalization and Unicode Conference, Santa Clara, California, USA, October 2021)_

The CLDR keyboard format seeks to address these challenges, by providing an interchange format for the communication of keyboard mapping data independent of vendors and platforms. Keyboard authors can then create a single mapping file for their language, which implementations can use to provide that language’s keyboard mapping on their own platform.

Additionally, the standardized identifier for keyboards can be used to communicate, internally or externally, a request for a particular keyboard mapping that is to be used to transform either text or keystrokes. The corresponding data can then be used to perform the requested actions.  For example, a remote screen-access application (such as used for customer service or server management) would be able to communicate and choose the same keyboard layout on the remote device as is used in front of the user, even if the two systems used different platforms.

The data can also be used in analysis of the capabilities of different keyboards. It also allows better interoperability by making it easier for keyboard designers to see which characters are generally supported on keyboards for given languages.

<!-- To illustrate this specification, here is an abridged layout representing the English US 101 keyboard on the macOS operating system (with an inserted long-press example). -->

For complete examples, see the XML files in the CLDR source repository.

* * *

## <a name="Goals_and_Nongoals" href="#Goals_and_Nongoals">Goals and Non-goals</a>

Some goals of this format are:

1. Physical and virtual keyboard layouts defined in a single file.
2. Provide definitive platform-independent definitions for new keyboard layouts.
    * For example, a new French standard keyboard layout would have a single definition which would be usable across all implementations.
3. Allow platforms to be able to use CLDR keyboard data for the character-emitting keys (non-frame) aspects of keyboard layouts.
    * For example, platform-specific keys such as Fn, Numpad, IME swap keys, and cursor keys are out of scope.
    * This also means that modifier (frame) keys cannot generate output, such as capslock -> backslash.
4. Deprecate & archive existing LDML platform-specific layouts so they are not part of future releases.

<!--
1. Make the XML as readable as possible.
2. Represent faithfully keyboard data from major platforms: it should be possible to create a functionally-equivalent data file (such that given any input, it can produce the same output).
3. Make as much commonality in the data across platforms as possible to make comparison easy. -->

Some non-goals (outside the scope of the format) currently are:

1. Adaptation for screen scaling resolution. Instead, keyboards should define layouts based on physical size. Platforms may interpret physical size definitions and adapt for different physical screen sizes with different resolutions.
2. Unification of platform-specific vkey and scan code mapping tables.
3. Unification of pre-existing platform layouts themselves (e.g. existing fr-azerty on platform a, b, c).
4. Support for prior (pre 3.0) CLDR keyboard files. See [Compatibility Notice](#Compatibility_Notice).
5. Run-time efficiency. [LDML is explicitly an interchange format](./tr35.md#Introduction), and so it is expected that data will be transformed to a more compact format for use by a keystroke processing engine.

<!-- 1. Display names or symbols for keycaps (eg, the German name for "Return"). If that were added to LDML, it would be in a different structure, outside the scope of this section.
2. Advanced IME features, handwriting recognition, etc.
3. Roundtrip mappings—the ability to recover precisely the same format as an original platform's representation. In particular, the internal structure may have no relation to the internal structure of external keyboard source data, the only goal is functional equivalence. -->

<!-- Note: During development of this section, it was considered whether the modifier RAlt (= AltGr) should be merged with Option. In the end, they were kept separate, but for comparison across platforms implementers may choose to unify them. -->

Note that in parts of this document, the format `@x` is used to indicate the _attribute_ **x**.

### <a name="Compatibility_Notice" href="#Compatibility_Notice">Compatibility Notice</a>

> 👉 Note: CLDR-TC has agreed that the changes required were too extensive to maintain compatibility. For this reason, the DTD used here is _not_ compatible with DTDs from prior versions of CLDR such as v41 and prior.
>
> To process earlier XML files, use the prior DTD and specification, such as v41 found at <https://www.unicode.org/reports/tr35/tr35-66/tr35.html>
>

### <a name="Accessibility" href="#Accessibility">Accessibility</a>

Keyboard use can be challenging for individuals with various types of disabilities. For this revision, the committee is not evaluating features or architectural designs for the purpose of improving accessibility. Such consideration could be fruitful for future revisions. However, some points on this topic should be made:

1. Having an industry-wide standard format for keyboards will enable accessibility software to make use of keyboard data with a reduced dependence on platform-specific knowledge.
2. Some features, such as multiTap and flicks, have the potential to reduce accessibility and thus should be discouraged. For example, multiTap requires pressing keys at a certain speed, and flicks require a more complex movement (press-and-flick) beyond a simple tap. Alternatively, inclusion of accessible methods of generating the same outputs (for example, simple keys on an additional layer), should be considered.
3. Public feedback is welcome on any aspects of this document which might hinder accessibility.

## <a name="Definitions" href="#Definitions">Definitions</a>

**Arrangement** is the term used to describe the relative position of the rectangles that represent keys, either physically or virtually. A physical keyboard has a static arrangement while a virtual keyboard may have a dynamic arrangement that changes per language and/or layer. While the arrangement of keys on a keyboard may be fixed, the mapping of those keys may vary.

**Base character:** The character emitted by a particular key when no modifiers are active. In ISO terms, this is group 1, level 1.

**Base map:** A mapping from the positions to the base characters. There is only one base map per layout. The characters on this map can be output without the use of any modifier keys.

**Core keys:** also known as “alpha” block. The primary set of key values on a keyboard that are used for typing the target language of the keyboard. For example, the three rows of letters on a standard US QWERTY keyboard (QWERTYUIOP, ASDFGHJKL, ZXCVBNM) together with the most significant punctuation keys. Usually this equates to the minimal keyset for a language as seen on mobile phone keyboards.
Distinguished from the **frame keys**.

**Dead keys:** These are keys which do not emit normal characters by themselves.  They are so named because to the user, they may appear to be “dead,” i.e., non-functional. However, they do produce a change to the input context. For example, in many Latin keyboards hitting the `^` dead-key followed by the `e` key produces `ê`. The `^` by itself may be invisible or presented in a special way by the platform.

**Frame keys:** These are keys which do not emit characters and are outside of the area of the **core keys**. These keys include both **modifier** keys, such as Shift or Ctrl, but also include platform specific keys: Fn, IME and layout-switching keys, cursor keys, emoji keys.

**Hardware keyboard:** an input device which has individual keys that are pressed. Each key has a unique identifier and the arrangement doesn't change, even if the mapping of those keys does. Also known as a physical keyboard.

<!-- **Hardware map:** A mapping between  and layout positions. -->

**Input Method Editor (IME):** a component or program that supports input of large character sets. Typically, IMEs employ contextual logic and candidate UI to identify the Unicode characters intended by the user.

<!-- **ISO position:** The corresponding position of a key using the ISO layout convention where rows are identified by letters and columns are identified by numbers. For example, "D01" corresponds to the "Q" key on a US keyboard. For the purposes of this document, an ISO layout position is depicted by a one-letter row identifier followed by a two digit column number (like "B03", "E12" or "C00"). The following diagram depicts a typical US keyboard layout superimposed with the ISO layout indicators (it is important to note that the number of keys and their physical placement relative to each-other in this diagram is irrelevant, rather what is important is their logical placement using the ISO convention):

![keyboard layout example showing ISO key numbering](images/keyPositions.png)

One may also extend the notion of the ISO layout to support keys that don't map directly to the diagram above (such as the Android device - see diagram). Per the ISO standard, the space bar is mapped to "A03", so the period and comma keys are mapped to "A02" and "A04" respectively based on their relative position to the space bar. Also note that the "E" row does not exist on the Android keyboard.

![keyboard layout example showing extension of ISO key numbering](images/androidKeyboard.png)

If it becomes necessary in the future, the format could extend the ISO layout to support keys that are located to the left of the "00" column by using negative column numbers "-01", "-02" and so on, or 100's complement "99", "98",... -->

**Key:** A key on a physical keyboard, or a virtual key on an on-screen layout.

**Key code:** The integer code sent to the application on pressing a key.

**Key map:** The basic mapping between hardware or on-screen positions and the output characters for each set of modifier combinations associated with a particular layout. There may be multiple key maps for each layout.

**Keyboard:** A particular arrangement of keys for the inputting of text, such as either a physical or virtual keyboard.

**Keyboard author:** The person or group of people designing and producing a particular keyboard layout designed to support one or more languages. In the context of this specification, that author may be editing the LDML XML file directly or by means of software tools.

**Keyboard layout:** A layout is the overall keyboard configuration for a particular locale. Within a keyboard layout, there is a single base map, one or more key maps and zero or more transforms.

**Layer** is an arrangement of keys on a virtual keyboard. A virtual keyboard is made up of a set of layers. Each layer may have a different key layout, unlike with a physical keyboard, and may not correspond directly to a physical keyboard's modifier keys. A layer is accessed via a switch key. See also virtual keyboard, modifier, switch.

**Long-press key:** also known as a “child key”. A secondary key that is invoked from a top level key on a software keyboard. Secondary keys typically provide access to variants of the top level key, such as accented variants (a => á, à, ä, ã)

**Modifier:** A key that is held to change the behavior of a hardware keyboard. For example, the "Shift" key allows access to upper-case characters on a US keyboard. Other modifier keys include but are not limited to: Ctrl, Alt, Option, Command and Caps Lock. On a touch layout, keys that appear to be modifier keys should be considered to be layer-switching keys.

**Physical keyboard:** see **Hardware keyboard**

**Touch keyboard** is a keyboard that is rendered on a, typically, touch surface. It has a dynamic arrangement and contrasts with a hardware keyboard. This term has many synonyms: software keyboard, SIP (Software Input Panel), virtual keyboard. This contrasts with other uses of the term virtual keyboard as an on-screen keyboard for reference or accessibility data entry.

**Transform:** A transform is an element that specifies a set of conversions from sequences of code points into one (or more) other code points. Transforms may reorder or replace text. They may be used to implement “dead key” behaviors, simple orthographic corrections, and visual (typewriter) type input.

**Virtual keyboard:** see **Touch keyboard**

### <a name="Escaping" href="#Escaping">Escaping</a>

When explicitly specified, attribute values can contain escaped characters. This specification uses two methods of escaping, the _UnicodeSet_ notation and the `\u{...}` notation.

### UnicodeSet Escaping

The _UnicodeSet_ notation is described in [UTS #35 section 5.3.3](tr35.md#Unicode_Sets) and allows for comprehensive character matching, including by character range, properties, names, or codepoints. Currently, the following attribute values allow _UnicodeSet_ notation:

* `from` or `before` on the `<transform>` element
* `from` or `before` on the `<reorder>` element
* `chars` on the [`<repertoire>`](#test-element-repertoire) test element.

### UTS18 Escaping

The `\u{...}` notation, a subset of hex notation, is described in [UTS #18 section 1.1](https://www.unicode.org/reports/tr18/#Hex_notation). It can refer to one or multiple individual codepoints. Currently, the following attribute values allow the `\u{...}` notation:

* `to`, `longPress`, `multiTap`, and `longPressDefault` on the `<key>` element
* `to` on the `<flick>` element
* `from` or `to` on the `<transform>` element
* `value` on the `<variable>` element
* `to` and `display` on the `<display>` element
* `baseCharacter` on the `<displayOptions>` element
* Some attributes on [Keyboard Test Data](#Keyboard Test Data) subelements

Characters of general category of Combining Mark (M), Control characters (Cc), Format characters (Cf), and whitespace other than space should be encoded using one of the notation above as appropriate.

Attribute values escaped in this manner are annotated with the `<!--@ALLOWS_UESC-->` DTD annotation, see [DTD Annotations](tr35.md#57-dtd-annotations)

* * *

## <a name="File_and_Dir_Structure" href="#File_and_Dir_Structure">File and Directory Structure</a>

* New collection of layouts that are prescriptive, and define the common core for a keyboard that can be consumed as data for implementation on different platforms. This collection will be in a different location than the existing CLDR keyboard files under main/keyboards. We should remove the existing data files, but keep the old DTD in the same place for compatibility, and also so that conversion tools can use it to read older files.
* New layouts will have version metadata to indicate their spec compliance versi​​on number.  For this tech preview, the value used must be `techpreview`.

```xml
<keyboard conformsTo="techpreview"/>
```

> _Note_: Unlike other LDML files, layouts are designed to be used outside of the CLDR source tree.  A new mechanism for referencing the DTD path should ideally be used, such as a URN or FPI. See <https://unicode-org.atlassian.net/browse/CLDR-15505> for discussion. For this tech preview, a relative path to the dtd will continue to be used as below.  Future versions may give other recommendations.

```xml
<!DOCTYPE keyboard SYSTEM "../dtd/ldmlKeyboard.dtd">
```

* The filename of a keyboard .xml file does not have to match the BCP47 primary locale ID, but it is recommended to do so. The CLDR repository may enforce filename consistency.

### <a name="Extensibility" href="#Extensibility">Extensibility</a>

For extensibility, the `<special>` element will be allowed at every nearly every level.

See [Element special](tr35.md#special) in Part 1.

* * *

## Element Hierarchy

This section describes the XML elements in a keyboard layout file, beginning with the top level element `<keyboard>`.

### <a name="Element_Keyboard" href="#Element_Keyboard">Element: keyboard</a>

This is the top level element. All other elements defined below are under this element.

**Syntax**

```xml
<keyboard locale="{locale ID}">
    {definition of the layout as described by the elements defined below}
</keyboard>
```

> <small>
>
> Parents: _none_
>
> Children: [displays](#Element_displays), [import](#Element_import), [info](#Element_info), [keys](#Element_keys), [layers](#Element_layers), [locales](#Element_locales), [names](#Element_names), [settings](#Element_settings), [_special_](tr35.md#special), [transforms](#Element_transforms), [variables](#Element_variables), [version](#Element_version), [vkeys](#Element_vkeys)
>
> Occurrence: required, single
>
> </small>

_Attribute:_ `conformsTo` (required)

This attribute distinguishes the keyboard from prior versions,
and it also specifies the minimum CLDR version required.

For purposes of this current draft spec, the value should always be `techpreview`

```xml
<keyboard … conformsTo="techpreview"/>
```

_Attribute:_ `locale` (required)

This attribute represents the primary locale of the keyboard using BCP 47 [Unicode locale identifiers](tr35.md#Canonical_Unicode_Locale_Identifiers) - for example `"el"` for Greek. Sometimes, the locale may not specify the base language. For example, a Devanagari keyboard for many languages could be specified by BCP-47 code: `"mul-Deva"`. For further details, see [Keyboard IDs](#Keyboard_IDs).

**Example** (for illustrative purposes only, not indicative of the real data)

```xml
<keyboard locale="ka">
  …
</keyboard>
```

```xml
<keyboard locale="fr-CH-t-k0-azerty">
  …
</keyboard>
```

* * *

### <a name="Element_locales" href="#Element_locales">Element: locales</a>

The optional `<locales>` element allows specifying additional or alternate locales. Denotes intentional support for an extra language, not just that a keyboard incidentally supports a language’s orthography.

**Syntax**

```xml
<locales>
    <locale id="…"/>
    <locale id="…"/>
</locales>
```

> <small>
>
> Parents: [keyboard](#Element_keyboard)
>
> Children: [locale](#Element_locale)
>
> Occurrence: optional, single
>
> </small>

### <a name="Element_locale" href="#Element_locale">Element: locale</a>

The optional `<locales>` element allows specifying additional or alternate locales. Denotes intentional support for an extra language, not just that a keyboard incidentally supports a language’s orthography.

**Syntax**

```xml
<locale id="{id}"/>
```

> <small>
>
> Parents: [locales](#Element_locales)
>
> Children: _none_
>
> Occurrence: optional, multiple
>
> </small>

_Attribute:_ `id` (required)

> The [BCP 47](tr35.md#Canonical_Unicode_Locale_Identifiers) locale ID of an additional language supported by this keyboard.
> Do _not_ include the `-k0-` subtag for this additional language.

**Example**

See [Principles for Keyboard IDs](#Principles_for_Keyboard_IDs) for discussion and further examples.

```xml
<!-- Pan Nigerian Keyboard-->
<keyboard locale="mul-Latn-NG-t-k0-panng">
    <locales>
    <locale id="ha"/>
    <locale id="ig"/>
    <!-- others … -->
    </locales>
</keyboard>
```

* * *

### <a name="Element_version" href="#Element_version">Element: version</a>

Element used to keep track of the source data version.

**Syntax**

```xml
<version number="..">
```

> <small>
>
> Parents: [keyboard](#Element_keyboard)
>
> Children: _none_
>
> Occurrence: optional, single
>
> </small>

_Attribute:_ `number` (required)

> Must be a [[SEMVER](https://semver.org)] compatible version number, such as `1.0.0`

_Attribute:_ `cldrVersion` (fixed by DTD)

> The CLDR specification version that is associated with this data file. This value is fixed and is inherited from the [DTD file](https://github.com/unicode-org/cldr/tree/main/keyboards/dtd) and therefore does not show up directly in the XML file.

**Example**

```xml
<keyboard locale="tok">
    …
    <version number="1"/>
    …
</keyboard>
```

* * *

### <a name="Element_info" href="#Element_info">Element: info</a>

Element containing informative properties about the layout, for displaying in user interfaces etc.

**Syntax**

```xml
<info [author="{author}"]
      [normalization="{form}"]
      [layout="{hint of the layout}"]
      [indicator="{short identifier}"] />
```

> <small>
>
> Parents: [keyboard](#Element_keyboard)
>
> Children: _none_
>
> Occurrence: optional, single
>
> </small>

_Attribute:_ `author`

> The `author` attribute contains the name of the author of the layout file.

_Attribute:_ `normalization`

> The `normalization` attribute describes the intended normalization form of the keyboard layout output. The valid values are `NFC`, `NFD` or `other`.
>
> An example use case is aiding a user to choose among the two same layouts with one outputting characters in the normalization form C and one in the normalization form D.
>
> All keyboards in the CLDR repository will be in `NFC` or `NFD` forms.  However, users and implementations may produce and consume other normalization forms or mixed output, use the `other` value to indicate this case.
>
> When using `NFC` or `NFD`, tooling should verify that all possible keystrokes, gestures, and transforms on the keyboard only produce the specified normalization form, producing warnings if not.

_Attribute:_ `layout`

> The `layout` attribute describes the layout pattern, such as QWERTY, DVORAK, INSCRIPT, etc. typically used to distinguish various layouts for the same language.

_Attribute:_ `indicator`

> The `indicator` attribute describes a short string to be used in currently selected layout indicator, such as US, SI9 etc.
> Typically, this is shown on a UI element that allows switching keyboard layouts and/or input languages.

* * *

### <a name="Element_names" href="#Element_names">Element: names</a>

Element used to store any names given to the layout.

These names are not currently localized.

**Syntax**

```xml
<names>
    {set of name elements}
</names>
```

> <small>
>
> Parents: [keyboard](#Element_keyboard)
>
> Children: [name](#Element_name), [_special_](tr35.md#special)
>
> Occurrence: required, single
>
> </small>

### <a name="Element_name" href="#Element_name">Element: name</a>

A single name given to the layout.

**Syntax**

```xml
<name value="..">
```

> <small>
>
> Parents: [names](#Element_names)
>
> Children: _none_
>
> Occurrence: required, multiple
>
> </small>

_Attribute:_ `value` (required)

> The name of the layout.


**Example**

```xml
<keyboard locale="bg-t-k0-phonetic-trad">
    …
    <names>
        <name value="Bulgarian (Phonetic Traditional)" />
    </names>
    …
</keyboard>
```

* * *

### <a name="Element_settings" href="#Element_settings">Element: settings</a>

An element used to keep track of layout specific settings. This element may or may not show up on a layout. These settings reflect the normal practice by the implementation. However, an implementation using the data may customize the behavior.

**Syntax**

```xml
<settings [fallback="omit"] [transformFailure="omit"] [transformPartial="hide"] />
```

> <small>
>
> Parents: [keyboard](#Element_keyboard)
>
> Children: _none_
>
> Occurrence: optional, single
>
> </small>

_Attribute:_ `fallback="omit"`

> The presence of this attribute means that when a modifier key combination goes unmatched, no output is produced. The default behavior (when this attribute is not present) is to fall back to the base map when the modifier key combination goes unmatched.

If this attribute is present, it must have a value of omit.


**Example**

```xml
<keyboard locale="bg">
    …
    <settings fallback="omit" />
    …
</keyboard>
```

Indicates that:

1.  When a modifier combination goes unmatched, do not output anything when a key is pressed.
2.  If a transform is escaped, output the contents of the buffer.
3.  During a transform, hide the contents of the buffer as the user is typing.

* * *

### <a name="Element_keys" href="#Element_keys">Element: keys</a>

This element defines the properties of all possible keys via [`<key>` elements](#Element_key) used in all layouts.
It is a “bag of keys” without specifying any ordering or relation between the keys.
There is only a single `<keys>` element in each layout.

**Syntax**

```xml
<keys>
    <key … />
    <key … />
    <key … />
    <flicks … />
    <key … />
    <flicks … />
</keys>
```

`key` and `flicks` elements may be interleaved in any order.

> <small>
>
> Parents: [keyboard](#Element_keyboard)
> Children: [key](#Element_key), [flicks](#Element_flicks)
> Occurrence: optional, single
>
> </small>



* * *

### <a name="Element_key" href="#Element_key">Element: key</a>

This element defines a mapping between an abstract key and its output. This element must have the `keys` element as its parent. The `key` element is referenced by the `keys=` attribute of the [`row` element](#Element_row).

**Syntax**

```xml
<key
 id="{key id}"
 [flicks="{flicks identifier}"]
 [gap="true"]
 [longPress="{long press keys}"]
 [longPressDefault="{default longpress target}"]
 [multiTap="{the output on subsequent taps}"]
 [stretch="true"]
 [switch="{layer id}"]
 [to="{the output}"]
 [transform="no"]
 [width="{key width}"]
 /><!-- {Comment to improve readability (if needed)} -->
```

> <small>
>
> Parents: [keys](#Element_keys)
>
> Children: _none_
>
> Occurrence: optional, multiple
> </small>

**Note**: The `id` attribute is required.

**Note**: _at least one of_ `switch`, `gap`, or `to` are required.

_Attribute:_ `id`

> The `id` attribute uniquely identifies the key. NMTOKEN, restricted to `[a-zA-Z0-9_.-]`. It can (but needn't be) the Latin key name for a Latn script keyboard (a, b, c, A, B, C, …), or any other valid token (e-acute, alef, alif, alpha, …)

_Attribute:_ `flicks="flick-id"` (optional)

> The `flicks` attribute indicates that this key makes use of a [`flicks`](#Element_flicks) set with the specified id.

_Attribute:_ `gap="true"` (optional)

> The `gap` attribute indicates that this key does not have any appearance, but represents a "gap" of the specified number of key widths. Can be used with `width` to set a width.

```xml
<key id="mediumgap" gap="true" width="1.5"/>
```

_Attribute:_ `longPress="a b c"` (optional)

> The `longPress` attribute contains any characters that can be emitted by "long-pressing" a key, this feature is prominent in mobile devices. The possible sequences of characters that can be emitted are whitespace delimited. Control characters, combining marks and whitespace (which is intended to be a long-press option) in this attribute are escaped using the `\u{...}` notation.
>
_Attribute:_ `longPressDefault` (optional)

> Indicates which of the `longPress` target characters is the default long-press target, which could be different than the first element. Ignored if not in the `longPress` list. Characters in this attribute can be escaped using the `\u{...}` notation.
> For example, if the `longPressDefault` is a key whose [display](#Element_displays) appears as `{` an implementation might render the key as follows:
>
> ![keycap hint](images/keycapHint.png)

_Attribute:_ `multiTap` (optional)

> A space-delimited list of strings, where each successive element of the list is produced by the corresponding number of quick taps. For example, three taps on the key C01 will produce a “c” in the following example (first tap produces “a”, two taps produce “bb” etc.).
>>
> _Example:_
>
> ```xml
> <key id="a" to="a" multiTap="bb c d">
> ```
>
> Control characters, combining marks and whitespace (which is intended to be a multiTap option) in this attribute are escaped using the `\u{...}` notation.

**Note**: Behavior past the end of the multiTap list is implementation specific.

_Attribute:_ `stretch="true"` (optional)

> The `stretch` attribute indicates that a touch layout may stretch this key to fill available horizontal space on the row.
> This is used, for example, on the spacebar. Note that `stretch=` is ignored for hardware layouts.

_Attribute:_ `switch="shift"` (optional)

> The `switch` attribute indicates that this key switches to another `layer` with the specified id (such as `<layer id="shift"/>` in this example).
> Note that a key may have both a `switch=` and a `to=` attribute, indicating that the key outputs prior to switching layers.
> Also note that `switch=` is ignored for hardware layouts: their shifting is controlled via
> the modifier keys.
>
> This attribute is an NMTOKEN, restricted to `[a-zA-Z0-9-]`


_Attribute:_ `to`

> The `to` attribute contains the output sequence of characters that is emitted when pressing this particular key. Control characters, whitespace (other than the regular space character) and combining marks in this attribute are escaped using the `\u{...}` notation. More than one key may output the same output.

> The `to` attribute may also contain the `\m{…}` syntax to insert a marker. See the definition of [markers](#markers).

_Attribute:_ `transform="no"` (optional)

> The `transform` attribute is used to define a key that does not participate in a transform (until the next keystroke). This attribute value must be `no` if the attribute is present.
> This attribute is useful where it is desired to output where two different keys could output the same characters (with different key or modifier combinations) but only one of them is intended to participate in a transform.
> When the next keystroke is pressed, the prior output may then combine using other transforms.
>
> Note that a more flexible way of solving this problem may be to use special markers which would inhibit matching.
>
> For example, suppose there are the following keys, their output and one transform:

```xml
<keys>
    <key id="X" to="^" transform="no"/>
    <key id="OptX" to="^"/>
</keys>
…
<transforms …>
    <transform from="^e" to="ê"/>
</transforms>
```

* **X** outputs `^` (caret)
* Option-**X** outputs `^` but is intended to be the first part of a transform.
* Option-**X** + `e` → `ê`

> Without the `transform="no"` on the base key **X**, it would not be possible to
> type the sequence `^e` (caret+e) as it would turn into `ê` per the transform.
> However, since there is `transform="no`" on **X**, if the user types **X** + `e` the sequence remains `^e`.

* **X** + `e` → `^e`

> Using markers, the same results can be obtained without need of `transform="no"` using:

```xml
<keys>
    <key id="X" to="^\m{no_transform}"/>
    <key id="OptX" to="^"/>
</keys>
…
<transforms …>
    <!-- this wouldn't match the key X output because of the marker -->
    <transform from="^e" to="ê"/>
</transforms>
```

Even better is to use a marker to indicate where transforms are desired:

```xml
<keys>
    <key id="X" to="^"/>
    <key id="OptX" to="^\m{transform}"/>
</keys>
…
<transforms …>
    <!-- again, this wouldn't match the key X output because of the missing marker -->
    <transform from="^\m{transform}e" to="ê"/>
</transforms>
```

_Attribute:_ `width="1.2"` (optional, default "1.0")

> The `width` attribute indicates that this key has a different width than other keys, by the specified number of key widths.

```xml
<key id="wide-a" to="a" width="1.2"/>
<key id="wide-gap" gap="true" width="2.5"/>
```

##### Implied Keys

Not all keys need to be listed explicitly.  The following two can be assumed to already exist:

```xml
<key id="gap" gap="true" width="1"/>
<key id="space" to=" " stretch="true" width="1"/>
```

In addition, these 62 keys, comprising 10 digit keys, 26 Latin lower-case keys, and 26 Latin upper-case keys, where the `id` is the same as the `to`, are assumed to exist:

```xml
<key id="0" to="0"/>
<key id="1" to="1"/>
<key id="2" to="2"/>
…
<key id="A" to="A"/>
<key id="B" to="B"/>
<key id="C" to="C"/>
…
<key id="a" to="a"/>
<key id="b" to="b"/>
<key id="c" to="c"/>
…
```

These implied keys are available in a data file named `keyboards/import/keys-Latn-implied.xml` in the CLDR distribution for the convenience of implementations.

Thus, the implied keys behave as if the following import were present.

```xml
<keyboard>
    <keys>
        <import base="cldr" path="techpreview/keys-Latn-implied.xml" />
    </keys>
</keyboard>
```

**Note:** All implied keys may be overridden, as with all other imported data items. See the [`import`](#Element_import) element for more details.

* * *

#### <a name="Element_flicks" href="#Element_flicks">Elements: flicks, flick</a>

The `flicks` element is used to generate results from a "flick" of the finger on a mobile device.

**Syntax**

```xml
<key id="a" flicks="a-flicks" to="a" />
<flicks id="a-flicks">
    {a set of flick elements}
</flicks>
```

> <small>
>
> Parents: [keys](#Element_keys)
>
> Children: [flick](#Element_flick), [_special_](tr35.md#special)
>
> Occurrence: optional, multiple
>
> </small>

_Attribute:_ `id` (required)

> The `id` attribute identifies the flicks. It can be any NMTOKEN matching `[A-Za-z0-9][A-Za-z0-9-]*`
> The `flicks` do not share a namespace with the `key`s, so it would also be allowed
> to have `<key id="a" flicks="a"/><flicks id="a"/>`

**Syntax**

```xml
<flick directions="{list of directions}" to="{the output}" />
```

> <small>
>
> Parents: [flicks](#Element_flicks)
>
> Children: _none_
>
> Occurrence: required, multiple
>
> </small>

_Attribute:_ `directions` (required)

> The `directions` attribute value is a space-delimited list of keywords, that describe a path, currently restricted to the cardinal and intercardinal directions `{n e s w ne nw se sw}`.

_Attribute:_ `to` (required)

> The to attribute value is the result of (one or more) flicks.

**Example**
where a flick to the Northeast then South produces two code points.

```xml
<flicks id="a">
    <flick directions="ne s" to="\uABCD\uDCBA" />
</flicks>
```

* * *

### <a name="Element_import" href="#Element_import">Element: import</a>

The `import` element is used to reference another xml file so that elements are imported from
another file. The use case is to be able to import a standard set of vkeys, transforms, and similar
from the CLDR repository.  `<import>` is not recommended as a way for keyboard authors to
split up their keyboard into multiple files, as the intent is for each single XML file to contain all that is needed for a keyboard layout.

`<import>` can be used as a child of a number of elements (see the _Parents_ section immediately below). Multiple `<import>` elements may be used, however, `<import>` elements must come before any other sibling elements.
If two identical elements are defined, the later element will take precedence, that is, override.

**Note:** imported files do not have any indication of their normalization mode. For this reason, the keyboard author must verify that the imported file is of a compatible normalization mode. See the [`info` element](#Element_info) for further details.

**Syntax**
```xml
<import base="cldr" path="techpreview/keys-Zyyy-punctuation.xml"/>
```
> <small>
>
> Parents: [displays](#Element_displays), [keyboard](#Element_keyboard), [keys](#Element_keys), [layers](#Element_layers), [names](#Element_names), [reorders](#Element_reorders), [transformGroup](#Element_transformGroup), [transforms](#Element_transforms), [variables](#Element_variables), [vkeys](#Element_vkeys)
>
> Children: _none_
>
> Occurrence: optional, multiple
>
> </small>

_Attribute:_ `base`

> The base may be omitted (indicating a local import) or have the value `"cldr"`.

**Note:** `base="cldr"` is required for all `<import>` statements within keyboard files in the CLDR repository.

_Attribute:_ `path` (required)

> If `base` is `cldr`, then the `path` must start with a CLDR version (such as `techpreview`) representing the CLDR version to pull imports from. The imports are located in the `keyboard/import` subdirectory of the CLDR source repository.
> If `base` is omitted, then `path` is an absolute or relative file path.


**Further Examples**

```xml
<!-- in a keyboard xml file-->
…
<transforms type="simple">
    <import base="cldr" path="techpreview/transforms-example.xml"/>
    <transform from="` " to="`" />
    <transform from="^ " to="^" />
</transforms>
…


<!-- contents of transforms-example.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE transforms SYSTEM "../dtd/ldmlKeyboard.dtd">
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

**Note:** The DOCTYPE and root element, here `transforms`, is the same as
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

### <a name="Element_displays" href="#Element_displays">Element: displays</a>

The displays can be used to describe what is to be displayed on the keytops for various keys. For the most part, such explicit information is unnecessary since the `@to` element from the `keys/key` element can be used. But there are some characters, such as diacritics, that do not display well on their own and so explicit overrides for such characters can help.
Another useful scenario is where there are doubled diacritics, or multiple characters with spacing issues.

The `displays` consists of a list of display subelements.

`displays` elements are designed to be shared across many different keyboard layout descriptions, and imported with `<import>` where needed.

For combining characters, U+25CC `◌` is used as a base. It is an error to use a combining character without a base in the `display` attribute.

For example, a key which outputs a combining tilde (U+0303) can be represented as follows:

```xml
    <display to="\u0303" display="◌̃" />  <!-- \u25CC \u0303-->
```

This way, a key which outputs a combining tilde (U+0303) will be represented as `◌̃` (a tilde on a dotted circle).

Some scripts/languages may prefer a different base than U+25CC.
See  [`<displayOptions baseCharacter=…/>`](#Element_displayOptions).

**Syntax**

```xml
<displays>
    {a set of display elements}
</displays>
```

**Note**: There is currently no way to indicate a custom display for a key without output (i.e. without a `to=` attribute), nor is there a way to indicate that such a key has a standardized identity (e.g. that a key should be identified as a “Shift”). These may be addressed in future versions of this standard.

> <small>
>
> Parents: [keyboard](#Element_keyboard)
>
> Children: [display](#Element_display), [displayOptions](#Element_displayOptions), [_special_](tr35.md#special)
>
> Occurrence: optional, single
>
> </small>

* * *

### <a name="Element_display" href="#Element_display">Element: display</a>

The `display` element describes how a character, that has come from a `keys/key` element, should be displayed on a keyboard layout where such display is possible.

**Syntax**

```xml
<display to="{the output}" display="{show as}" />
```

> <small>
>
> Parents: [displays](#Element_displays)
>
> Children: _none_
>
> Occurrence: required, multiple
>
> </small>

One of the `to` or `id` attributes is required.

_Attribute:_ `to` (optional)

> Specifies the character or character sequence from the `keys/key` element that is to have a special display.
> This attribute may be escaped with `\u` notation, see [Escaping](#Escaping).
> The `to` attribute may also contain the `\m{…}` syntax to reference a marker. See [Markers](#markers). Implementations may highlight a displayed marker, such as with a lighter text color, or a yellow highlight.

_Attribute:_ `id` (optional)

> Specifies the `key` id. This is useful for keys which do not produce any output (no `to=` value), such as a shift key.
>
> This attribute must match `[A-Za-z0-9][A-Za-z0-9-]*`

_Attribute:_ `display` (required)

> Required and specifies the character sequence that should be displayed on the keytop for any key that generates the `@to` sequence or has the `@id`. (It is an error if the value of the `display` attribute is the same as the value of the `to` attribute, this would be an extraneous entry.)

This attribute may be escaped with `\u` notation, see [Escaping](#Escaping).

**Example**

```xml
<keyboard>
    <keys>
        <key id="a" to="a" longpress="\u0301 \u0300" />
        <key id="shift" switch="shift" />
    </keys>
    <displays>
        <display to="\u0300" display="ˋ" /> <!-- \u02CB -->
        <display to="\u0301" display="ˊ" /> <!-- \u02CA -->
        <display id="shift"  display="⇪" /> <!-- U+21EA -->
        <display to="\m{grave}" display="`" /> <!-- Display \m{grave} as ` -->
    </displays>
</keyboard>
```

To allow `displays` elements to be shared across keyboards, there is no requirement that `@to` in a `display` element matches any `@to`/`@id` in any `keys/key` element in the keyboard description.

* * *

### <a name="Element_displayOptions" href="#Element_displayOptions">Element: displayOptions</a>

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
> Parents: [displays](#Element_displays)
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

This attribute may be escaped with `\u` notation, see [Escaping](#Escaping).

* * *

### <a name="Element_layers" href="#Element_layers">Element: layers</a>

This element represents a set of `layer` elements with a specific physical form factor, whether
hardware or touch layout.

> <small>
>
> Parents: [keyboard](#Element_keyboard)
>
> Children: [import](#Element_import), [layer](#Element_layer), [_special_](tr35.md#special)
>
> Occurrence: required, multiple
>
> </small>

- At least one `layers` element is required.

_Attribute:_ `form` (required)

> This attribute specifies the physical layout of a hardware keyboard,
> or that the form is a `touch` layout.
>
> It is recommended to always have at least one hardware (non-touch) form.
> If there is no `hardware` form, the implementation may need
> to choose a different keyboard file, or use some other fallback behavior when using a
> hardware keyboard.
>
> Multiple `<layers form="touch">` elements are allowed with distinct `minDeviceWidth` values.
> At most one hardware (non-`touch`) `<layers>` element is allowed. If a different key arrangement is desired between, for example, `us` and `iso` formats, these should be separated into two different keyboards.
>
> The typical keyboard author will be designing a keyboard based on their circumstances and the hardware that they are using. So, for example, if they are in South East Asia, they will almost certainly be using an 101 key hardware keyboard with US key caps. So we want them to be able to reference that (`<layers form="us">`) in their design, rather than having to work with an unfamiliar form.
>
> A mismatch between the hardware layout in the keyboard file, and the actual hardware used by the user could result in some keys being inaccessible to the user if their hardware cannot generate the scancodes corresponding to the layout specified by the `form=` attribute. Such keys could be accessed only via an on-screen keyboard utility. Conversely, a user with hardware keys that are not present in the specified `form=` will result in some hardware keys which have no function when pressed.
>
>
> When using an on-screen keyboard, if there is not a `<layers form="touch">`
> element, the hardware elements can be used for on-screen use.
>
> The following values are allowed for the `form` attribute:
>
> - `touch` - Touch (non-hardware) layout.
> - `abnt2` - Brazilian 103 key ABNT2 layout (iso + extra key left of right shift)
> - `iso` - European 102 key layout (extra key right of left shift)
> - `jis` - Japanese 109 key layout
> - `us` - ANSI 101 key layout

_Attribute:_ `minDeviceWidth`

> This attribute specifies the minimum required width, in millimeters (mm), of the touch surface.  The `layers` entry with the greatest matching width will be selected. This attribute is intended for `form="touch"`, but is supported for hardware forms.
>
> This must be a whole number between 1 and 999, inclusive.

### <a name="Element_layer" href="#Element_layer">Element: layer</a>

A `layer` element describes the configuration of keys on a particular layer of a keyboard. It contains one or more `row` elements to describe which keys exist in each row.

**Syntax**

```xml
<layer id="layerId" modifier="{Set of Modifier Combinations}">
    ...
</layer>
```

> <small>
>
> Parents: [keyboard](#Element_keyboard)
>
> Children: [row](#Element_row), [_special_](tr35.md#special)
>
> Occurrence: optional, multiple
>
> </small>

_Attribute_ `id` (required for `touch`)

> The `id` attribute identifies the layer for touch layouts.  This identifier specifies the layout as the target for layer switching, as specified by the `switch=` attribute on the [`<key>`](#Element_key) element.
> Touch layouts must have one `layer` with `id="base"` to serve as the base layer.
>
> Must match `[A-Za-z0-9][A-Za-z0-9-]*`

_Attribute:_ `modifier` (required for `hardware`)

> This has two roles. It acts as an identifier for the `layer` element for hardware keyboards (in the absence of the id= element) and also provides the linkage from the hardware modifiers into the correct `layer`.
>
> To indicate that no modifiers apply, the reserved name of `none` can be used.
> The following modifier components can be used, separated by spaces.
> Note that `L` or `R` indicates a left- or right- side modifier only (such as `altL`)
> whereas `alt` indicates _either_ left or right alt key.
> `shift` also indicates either shift key.
>
> - `none` (no modifier, may not be combined with others)
> - `alt`
> - `altL`
> - `altR`
> - `caps`
> - `ctrl`
> - `ctrlL`
> - `ctrlR`
> - `shift`
>
> Note that `alt` is sometimes referred to as _opt_ or _option_.
>
> Left- and right- side modifiers (such as `"altL ctrlR"` or `"altL altR"`) should not be used together in a single `modifier` attribute value.
>
> Left- and right- side modifiers (such as `"altL ctrlR"` or `"altL altR"`) should not be used together in a single `modifier` attribute value.

> For hardware layouts, the use of `@modifier` as an identifier for a layer is sufficient since it is always unique among the set of `layer` elements in a keyboard.
>
> The set of modifiers must match `(none|([A-Za-z0-9]+)( [A-Za-z0-9]+)*)`

* * *

### <a name="Element_row" href="#Element_row">Element: row</a>

A `row` element describes the keys that are present in the row of a keyboard.



**Syntax**

```xml
<row keys="{keyId} {keyId} …" />
```

> <small>
>
> Parents: [layer](#Element_layer)
>
> Children: _none_
>
> Occurrence: required, multiple
>
> </small>

_Attribute:_ `keys` (required)

> This is a string that lists the id of [`key` elements](#Element_key) for each of the keys in a row, whether those are explicitly listed in the file or are implied.  See the `key` documentation for more detail.

**Example**

Here is an example of a `row` element:

```xml
<row keys="a z e r t y u i o p caret dollar" />
```

* * *

### <a name="Element_vkeys" href="#Element_vkeys">Element: vkeys</a>

On some architectures, applications may directly interact with keys before they are converted to characters. The keys are identified using a virtual key identifier or vkey. The mapping between a physical keyboard key and a vkey is keyboard-layout dependent. For example, a French keyboard would identify the top-left key (ISO D01) as being an `A` with a vkey of `A` as opposed to `Q` on a US English keyboard. While vkeys are layout dependent, they are not modifier dependent. A shifted key always has the same vkey as its unshifted counterpart. In effect, a key may be identified by its vkey and the modifiers active at the time the key was pressed.

**Syntax**

```xml
<vkeys>
    {a set of vkey elements}
</vkeys>
```

> <small>
>
> Parents: [keyboard](#Element_keyboard)
>
> Children: [import](#Element_import), [_special_](tr35.md#special), [vkey](#Element_vkey)
>
> Occurrence: optional, single
>
> </small>

There is at most a single vkeys element per keyboard.

A `vkeys` element consists of a list of `vkey` elements.

* * *

### <a name="Element_vkey" href="#Element_vkey">Element: vkey</a>

A `vkey` element describes a mapping between a key and a vkey.

**Syntax**

```xml
<vkey from="{from}" to="{to}" />
```

> <small>
>
> Parents: [vkeys](#Element_vkeys)
>
> Children: _none_
>
> Occurrence: required, multiple
>
> </small>

_Attribute:_ `from` (required)

> The vkey being mapped from. See [CLDR VKey Enum](#CLDR_VKey_Enum) for a reference table.

_Attribute:_ `to` (required)

> The resultant vkey identifier. See [CLDR VKey Enum](#CLDR_VKey_Enum) for a reference table.

**Example**

This example shows some of the mappings for a French keyboard layout:

```xml
<keyboard>
    <vkeys>
		  <vkey from="Q" to="A" />
		  <vkey from="W" to="Z" />
		  <vkey from="A" to="Q" />
		  <vkey from="Z" to="W" />
    </vkeys>
</keyboard>
```

* * *

### <a name="Element_variables" href="#Element_variables">Element: variables</a>

> <small>
>
> Parents: [keyboard](#Element_keyboard)
>
> Children: [import](#Element_import), [_special_](tr35.md#special), [string](#element-string), [set](#element-set), [unicodeSet](#element-unicodeSet)
>
> Occurrence: optional, single
> </small>

This is a container for variables to be used with [transform](#element-transform), [display](#element-display) and [key](#element-key) elements.

Note that the `id=` attribute must be unique across all children of the `variables` element.

**Example**

```xml
<variables>
    <string id="y" value="yes" /> <!-- a simple string-->
    <set id="upper" value="A B C D E FF" /> <!-- a set with 6 items -->
    <unicodeSet id="consonants" value="[कसतनमह]" /> <!-- a UnicodeSet -->
</variables>
```

* * *

### Element: string

> <small>
>
> Parents: [variables](#Element_variables)
>
> Children: _none_
>
> Occurrence: optional, multiple
> </small>

> This element represents a single string which is used by the [transform](#element-transform) elements for string matching and substitution, as well as by the [key](#element-key) and [display](#element-display) elements.

_Attribute:_ `id` (required)

> Specifies the identifier (name) of this string.
> All ids must be unique across all types of variables.
>
> `id` must match `[0-9A-Za-z_]{1,32}`

_Attribute:_ `value` (required)

> Strings may contain whitespaces. However, for clarity, it is recommended to escape spacing marks, even in strings.
> This attribute may be escaped with `\u` notation, see [Escaping](#Escaping).
> Variables may refer to other string variables if they have been previously defined, using `${string}` syntax.
> [Markers](#markers) may be included with the `\m{…}` notation.

**Example**

```xml
<variables>
    <string id="cluster_hi" value="हि" /> <!-- a string -->
    <string id="zwnj" value="\u{200C}"/> <!-- single codepoint -->
    <string id="acute" value="\m{acute}"/> <!-- refer to a marker -->
    <string id="zwnj_acute" value="${zwnj}${acute}"  /> <!-- Combine two variables -->
    <string id="zwnj_sp_acute" value="${zwnj}\u{0020}${acute}"  /> <!-- Combine two variables -->
</variables>
```

These may be then used in multiple contexts:

```xml
<!-- as part of a regex -->
<transform from="${cluster_hi}X" to="X" />
<transform from="Y" to="${cluster_hi}" />
…
<!-- as part of a key bag  -->
<key id="hi_key" to="${cluster_hi}" />
<key id="acute_key" to="${acute}" />
…
<!-- Display ´ instead of the non-displayable marker -->
<display to="${acute}" display="´" />
```

* * *

### Element: set

> <small>
>
> Parents: [variables](#Element_variables)
>
> Children: _none_
>
> Occurrence: optional, multiple
> </small>

> This element represents a set of strings used by the [transform](#element-transform) elements for string matching and substitution.

_Attribute:_ `id` (required)

> Specifies the identifier (name) of this set.
> All ids must be unique across all types of variables.
>
> `id` must match `[0-9A-Za-z_]{1,32}`

_Attribute:_ `value` (required)

> The `value` attribute is always a set of strings separated by whitespace, even if there is only a single item in the set, such as `"A"`.
> Leading and trailing whitespace is ignored.
> This attribute may be escaped with `\u` notation, see [Escaping](#Escaping).
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

See [transform](#element-transform) for further details.

* * *

### Element: unicodeSet

> <small>
>
> Parents: [variables](#Element_variables)
>
> Children: _none_
>
> Occurrence: optional, multiple
> </small>

> This element represents a set, using a subset of the [UnicodeSet](tr35.md#Unicode_Sets) format, used by the [`transform`](#element-transform) elements for string matching and substitution.
> Note important restrictions on the syntax below.

_Attribute:_ `id` (required)

> Specifies the identifier (name) of this unicodeSet.
> All ids must be unique across all types of variables.
>
> `id` must match `[0-9A-Za-z_]{1,32}`

_Attribute:_ `value` (required)

> String value in [UnicodeSet](tr35.md#Unicode_Sets) format.
> Leading and trailing whitespace is ignored.
> Variables may refer to other string variables if they have been previously defined, using `${string}` syntax, or to other previously-defined UnicodeSets (not sets) using `$[unicodeSet]` syntax.

**Syntax Note**

- Warning: UnicodeSets look superficially similar to regex character classes as used in [`transform`](#element-transform) elements, but they are different. UnicodeSets must be defined with a `unicodeSet` element, and referenced with the `$[unicodeSet]` notation in transforms. UnicodeSets cannot be specified inline in a transform, and can only be used indirectly by reference to the corresponding `unicodeSet` element.
- Multi-character strings (`{}`) are not supported, such as `[żġħ{ie}{għ}]`.
- UnicodeSet property notation (`\p{…}` or `[:…:]`) may **NOT** be used, because that would make implementations dependent on a particular version of Unicode. However, implementations and tools may wish to pre-calculate the value of a particular UnicodeSet, and "freeze" it as explicit code points.  The example below of `$[KhmrMn]` matches all nonspacing marks in the `Khmr` script.
- UnicodeSets may represent a very large number of codepoints. A limit may be set on how many unique range entries may be matched.

**Examples**

```xml
<variables>
  <unicodeSet id="consonants" value="[कसतनमह]" /> <!-- unicode set range -->
  <unicodeSet id="range" value="[a-z D E F G \u200A]" /> <!-- a through z, plus a few others -->
  <unicodeSet id="newrange" value="[$[range]-[G]]" /> <!-- The above range, but not including G -->
  <unicodeSet id="KhmrMn" value="[\u17B4\u17B5\u17B7-\u17BD\u17C6\u17C9-\u17D3\u17DD]"> <!--  [[:Khmr:][:Mn:]] as of Unicode 15.0-->
</variables>
```

The `unicodeSet` element may not be used as the source or target for mapping operations (`$[1:variable]` syntax).
The `unicodeSet` element may not be referenced by [`key`](#element-key) and [`display`](#element-display) elements.

* * *

### <a name="Element_transforms" href="#Element_transforms">Element: transforms</a>

This element defines a group of one or more `transform` elements associated with this keyboard layout. This is used to support features such as dead-keys, character reordering, backspace behavior, etc. using a straightforward structure that works for all the keyboards tested, and that results in readable source data.

There can be multiple `<transforms>` elements, but only one for each `type`.

**Syntax**

```xml
<transforms type="...">
    {a set of transform groups}
</transforms>
```

> <small>
>
> Parents: [keyboard](#Element_keyboard)
>
> Children: [import](#Element_import), [_special_](tr35.md#special), [transformGroup](#Element_transformGroup)
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

#### Markers

Markers are placeholders which record some state, but without producing normal visible text output.  They were designed particularly to support dead-keys.

The marker ID is any valid `NMTOKEN` (But see [CLDR-17043](https://unicode-org.atlassian.net/browse/CLDR-17043) for future discussion.)

Consider the following abbreviated example:

```xml
    <display to="\m{circ_marker}" display="^" />
…
    <key id="circ_key" to="\m{circ_marker}" />
    <key id="e" to="e" />
…
    <transform from="\m{circ_marker}e" to="ê" />
```

1. The user presses the `circ_key` key. The key can be shown with the keycap `^` due to the `<display>` element.

2. The special marker, `circ_marker`, is added to the end of input context.

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

**Effect of markers on final text**

All markers must be removed before text is returned to the application from the input context.
If the input context changes, such as if the cursor or mouse moves the insertion point somewhere else, all markers in the input context are removed.

**Implementation Notes**

Ideally, markers are implemented entirely out-of-band from the normal text stream. However, implementations _may_ choose to map each marker to a [Unicode private-use character](https://www.unicode.org/glossary/#private_use_character) for use only within the implementation’s processing and temporary storage in the input context.

For example, the first marker encountered could be represented as U+E000, the second by U+E001 and so on.  If a regex processing engine were used, then those PUA characters could be processed through the existing regex processing engine.  `[^\uE000-\uE009]` could be used as an expression to match a character that is not a marker, and `[Ee]\u{E000}` could match `E` or `e` followed by the first marker.

Such implementations must take care to remove all such markers (see prior section) from the resultant text. As well, implementations must take care to avoid conflicts if applications themselves are using PUA characters, such as is often done with not-yet-encoded scripts or characters.

* * *

### <a name="Element_transformGroup" href="#Element_transformGroup">Element: transformGroup</a>

> <small>
>
> Parents: [transforms](#Element_transforms)
>
> Children: [import](#Element_import), [reorder](#Element_reorder), [_special_](tr35.md#special), [transform](#Element_transform)
>
> Occurrence: optional, multiple
> </small>

A `transformGroup` represents a set of transform elements or reorder elements.

Each `transformGroup` is processed entirely before proceeding to the next one.


Each `transformGroup` element, after imports are processed, must have either [reorder](#Element_reorder) elements or [transform](#Element_transform) elements, but not both. The `<transformGroup>` element may not be empty.

**Examples**


#### Example: `transformGroup` with `transform` elements

This is a `transformGroup` that consists of one or more [`transform`](#element-transform) elements, prefaced by one or more `import` elements. See the discussion of those elements for details. `import` elements in this group may not import `reorder` elements.


```xml
<transformGroup>
    <import path="..."/> <!-- optional import elements-->
    <transform />
    <!-- other <transform/> elements -->
</transformGroup>
```


#### Example: `transformGroup` with `reorder` elements

This is a `transformGroup` that consists of one or more [`transform`](#element-transform) elements, optionally prefaced by one or more `import` elements that import `transform` elements. See the discussion of those elements for details.

`import` elements in this group may not import `transform` elements.

```xml
<transformGroup>
    <import path="..."/> <!-- optional import elements-->
    <reorder ... />
    <!-- other <reorder> elements -->
</transformGroup>
```

* * *

### <a name="Element_transform" href="#Element_transform">Element: transform</a>

This element represents a single transform that may be performed using the keyboard layout. A transform is an element that specifies a set of conversions from sequences of code points into (one or more) other code points. For example, in most French keyboards hitting the `^` dead-key followed by the `e` key produces `ê`.

Matches are processed against the "input context", a temporary buffer containing all relevant text up to the insertion point. If the user moves the insertion point, the input context is discarded and recreated from the application’s text buffer.  Implementations may discard the input context at any time.

The input context may contain, besides regular text, any [Markers](#markers) as a result of keys or transforms, since the insertion point was moved.

Using regular expression terminology, matches are done as if there was an implicit `$` (match end of buffer) at the end of each pattern. In other words, `<transform from="ke" …>` will not match an input context ending with `…keyboard`, but it will match the last two codepoints of an input context ending with `…awake`.

All of the `transform` elements in a `transformGroup` are tested for a match, in order, until a match is found. Then, the matching element is processed, and then processing proceeds to the **next** `transformGroup`. If none of the `transform` elements match, processing proceeds without modification to the buffer to the **next** `transformGroup`.

**Syntax**

```xml
<transform from="{input rule}" to="{output pattern}"/>
```

> <small>
>
> Parents: [transformGroup](#Element_transformGroup)
> Children: _none_
> Occurrence: required, multiple
>
> </small>


_Attribute:_ `from` (required)

> The `from` attribute consists of an input rule for matching the input context.
>
> The `transform` rule and output pattern uses a modified, mostly subsetted, regular expression syntax, with EcmaScript syntax (with the `u` Unicode flag) as its baseline reference (see [MDN-REGEX](https://developer.mozilla.org/docs/Web/JavaScript/Guide/Regular_Expressions)). Differences from regex implementations will be noted.

#### Regex-like Syntax

- **Simple matches**

    `abc` `𐒵`

- **Unicode codepoint escapes**

    `\u1234 \u012A`
    `\u{22} \u{012a} \u{1234A}`

    The hex escaping is case insensitive. The value may not match a surrogate or illegal character, nor a marker character.
    The form `\u{…}` is preferred as it is the same regardless of codepoint length.

- **Fixed character classes and escapes**

    `\s \S \t \r \n \f \v \\ \$ \d \w \D \W \0`

    The value of these classes do not change with Unicode versions.

    `\s` for example is exactly `[\f\n\r\t\v\u00a0\u1680\u2000-\u200a\u2028\u2029\u202f\u205f\u3000\ufeff]`

    `\\` and `\$` evaluate to `\` and `$`, respectively.

- **Character classes**

    `[abc]` `[^def]` `[a-z]` `[ॲऄ-आइ-ऋ]` `[\u093F-\u0944\u0962\u0963]`

    - supported
    - no Unicode properties such as `\p{…}`
    - Warning: Character classes look superficially similar to UnicodeSets as defined in [`unicodeSet`](#element-unicodeSet) elements, but they are different. UnicodeSets must be defined with a `unicodeSet` element, and referenced with the `$[unicodeSet]` notation in transforms. UnicodeSets cannot be used directly in a transform.

- **Bounded quantifier**

    `{x,y}`

    `x` and `y` are required single digits representing the minimum and maximum number of occurrences.
    `x` must be ≥ 0, `y` must be ≥ x and ≥ 1

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

#### Additional Features

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

    In this usage, the variable with `id="zwnj"` will be substituted in at this point in the expression. The variable can contain a range, a character, or any other portion of a pattern. If `zwnj` is a simple string, the pattern will match that string at this point.

- **Set and UnicodeSet variables**

    `$[upper]`

    Given a space-separated variable, this syntax will match _any_ of the substrings. This expression may be thought of  (and implemented) as if it were a _non-capturing group_. It may, however, be enclosed within a capturing group. For example, the following definition of `$[upper]` will match as if it were written `(?:A|B|CC|D|E|FF)`.

    ```xml
    <variables>
        <set id="upper" value=" A B CC  D E  FF " />
    </variables>
    ```

    This expression in a `from=` may be used to **insert a mapped variable**, see below under [Replacement syntax](#replacement-syntax).

#### Disallowed Regex Features

- **Unicode properties**

    `\p{property}` `\P{property}`

    **Rationale:** The behavior of this feature varies by Unicode version, and so would not have predictable results.

    Tooling may choose to suggest an expansion of properties, such as `\p{Mn}` to all non spacing marks for a certain Unicode version.  As well, a set of variables could be constructed in an `import`-able file matching particularly useful Unicode properties.

    ```xml
    <unicodeSet id="Mn" value="[\u034F\u0591-\u05AF\u05BD\u05C4\u05C5\…]" /> <!-- 1,985 code points -->
    ```

- **Backreferences**

    `([abc])-\1` `\k<something>`

    **Rationale:** Implementation and cognitive complexity.

- **Unbounded Quantifiers**

    `* + *? +? {1,} {0,}`

    **Rationale:** Implementation and Computational complexity.

- **Nested capture groups**

    `((a|b|c)|(d|e|f))`

    **Rationale:** Computational and cognitive complexity.

- **Named capture groups**

    `(?<something>)`

    **Rationale:** Implementation complexity.

- **Assertions** other than `^`

    `\b` `\B` `(?<!…)` …

    **Rationale:** Implementation complexity.

- **End marker**

    `$`

    The end marker can be thought of as being implicitly at the end of every `from=` pattern, matching the insertion point. Transforms do not match past the insertion point.

_Attribute:_ `to`

> This attribute represents the characters that are output from the transform.
>
> If this attribute is absent, it indicates that the no characters are output, such as with a backspace transform.
>
> A final rule such as `<transform from=".*"/>` will remove all context which doesn’t match one of the prior rules.

#### Replacement syntax

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

    `$[1:variable]` (Where "1" is be any numbered capture group from 1 to 9)

    Maps capture group 1 to variable `variable`. The `from=` side must also contain a grouped variable. This expression may appear anywhere or multiple times in the `to=` pattern.

    **Example**

    ```xml
    <set id="upper" value="A B CC D E  FF       G" />
    <set id="lower" value="a b c  d e  \u{0192} g" />
    <!-- note that values may be spaced for ease of reading -->
    …
    <transform from="($[upper])" to="$[1:lower]" />
    ```

    - The capture group on the `from=` side **must** contain exactly one set variable.  `from="Q($[upper])X"` can be used (other context before or after the capture group), but `from="(Q$[upper])"` may not be used with a mapped variable and is flagged as an error.

    - The `from=` and `to=` sides of the pattern must both be using `set` variables. There is no way to insert a set literal on either side and avoid using a variable.
    A UnicodeSet may not be used directly, but must be defined as a `unicodeSet` variable.

    - The two variables (here `upper` and `lower`) must have exactly the same number of whitespace-separated items. Leading and trailing space (such as at the end of `lower`) is ignored. A variable without any spaces is considered to be a set variable of exactly one item.

    - As described in [Additional Features](#additional-features), the `upper` set variable as used here matches as if it is `((?:A|B|CC|D|E|FF|G))`, showing the enclosing capturing group. When text from the input context matches this expression, and all above conditions are met, the mapping proceeds as follows:

    1. The portion of the input context, such as `CC`, is matched against the above calculated pattern.

    2. The position within the `from=` variable (`upper`) is calculated. The regex match may not have this information, but the matched substring `CC` can be compared against the tokenized input variable: `A`, `B`, `CC`, `D`, … to find that the 3rd item matches exactly.

    3. The same position within the `to=` variable (`lower`) is calculated. The 3rd item is `c`.

    4. `CC` in the input context is replaced with `c`, and processing proceeds to the next `transformGroup`.

- **Emit a marker**

    `\m{Some_marker}`

    Emits the named mark. Also see [Markers](#markers).

* * *

### <a name="Element_reorder" href="#Element_reorder">Element: reorder</a>

The reorder transform consists of a [`<transformGroup>`](#element-transformgroup) element containing `<reorder>` elements.  Multiple such `<transformGroup>` elements may be contained in an enclosing `<transforms>` element.

One or more [`<import>`](#element-import) elements are allowed to precede the `<reorder>` elements.

This transform has the job of reordering sequences of characters that have been typed, from their typed order to the desired output order. The primary concern in this transform is to sort combining marks into their correct relative order after a base, as described in this section. The reorder transforms can be quite complex, keyboard layouts will almost always import them.

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

The primary order of a character with the Unicode property `Canonical_Combining_Class` (ccc) of 0 may well not be 0. In addition, a character may receive a different primary order dependent on context. For example, in the Devanagari sequence ka halant ka, the first ka would have a primary order 0 while the halant ka sequence would give both halant and the second ka a primary order > 0, for example 2. Note that “base” character in this discussion is not a Unicode base character. It is instead a character with primary=0.

In order to get the characters into the correct relative order, it is necessary not only to order combining marks relative to the base character, but also to order some combining marks in a subsequence following another combining mark. For example in Devanagari, a nukta may follow a consonant character, but it may also follow a conjunct consisting of consonant, halant, consonant. Notice that the second consonant is not, in this model, the start of a new run because some characters may need to be reordered to before the first base, for example repha. The repha would get primary < 0, and be sorted before the character with order = 0, which is, in the case of Devanagari, the initial consonant of the orthographic syllable.

The reorder transform consists of `<reorder>` elements encapsulated in a `<transformGroup>` element. Each element is a rule that matches against a string of characters with the action of setting the various ordering attributes (`primary`, `tertiary`, `tertiaryBase`, `preBase`) for the matched characters in the string.

The relative ordering of `<reorder>` elements is not significant.

**Syntax**

```xml
<transformGroup>
    <!-- one or more <import/> elements are allowed at this point -->
    <reorder from="{combination of characters}"
    [before="{look-behind required match}"]
    [order="{list of weights}"]
    [tertiary="{list of weights}"]
    [tertiaryBase="{list of true/false}"]
    [preBase="{list of true/false}"] />
    <!-- other <reorder/> elements... -->
</transformGroup>
```

> <small>
>
> Parents: [transformGroup](#Element_transformGroup)
> Children: _none_
> Occurrence: optional, multiple
>
> </small>

_Attribute:_ `from` (required)

> This attribute contains a string of elements. Each element matches one character and may consist of a codepoint or a UnicodeSet (both as defined in [UTS #35 Part One](tr35.md#Unicode_Sets)).

_Attribute:_ `before`

> This attribute contains the element string that must match the string immediately preceding the start of the string that the @from matches.

_Attribute:_ `order`

> This attribute gives the primary order for the elements in the matched string in the `@from` attribute. The value is a simple integer between -128 and +127 inclusive, or a space separated list of such integers. For a single integer, it is applied to all the elements in the matched string. Details of such list type attributes are given after all the attributes are described. If missing, the order value of all the matched characters is 0. We consider the order value for a matched character in the string.
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

> This attribute gives the tertiary order value to the characters matched. The value is a simple integer between -128 and +127 inclusive, or a space separated list of such integers. If missing, the value for all the characters matched is 0. We consider the tertiary value for a matched character in the string.
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

> This attribute is a space separated list of `"true"` or `"false"` values corresponding to each character matched. It is illegal for a tertiary character to have a true `tertiaryBase` value. For a primary character it marks that this character may have tertiary characters moved after it. When calculating the secondary weight for a tertiary character, the most recently encountered primary character with a true `tertiaryBase` attribute is used. Primary characters with an `@order` value of 0 automatically are treated as having `tertiaryBase` true regardless of what is specified for them.

_Attribute:_ `preBase`

> This attribute gives the prebase attribute for each character matched. The value may be `"true"` or `"false"` or a space separated list of such values. If missing the value for all the characters matched is false. It is illegal for a tertiary character to have a true prebase value.
>
> If a primary character has a true prebase value then the character is marked as being typed before the base character of a run, even though it is intended to be stored after it. The primary order gives the intended position in the order after the base character, that the prebase character will end up. Thus `@order` shall not be 0. These characters are part of the run prefix. If such characters are typed then, in order to give the run a base character after which characters can be sorted, an appropriate base character, such as a dotted circle, is inserted into the output run, until a real base character has been typed. A value of `"false"` indicates that the character is not a prebase.

For `@from` attributes with a match string length greater than 1, the sort key information (`@order`, `@tertiary`, `@tertiaryBase`, `@preBase`) may consist of a space-separated list of values, one for each element matched. The last value is repeated to fill out any missing values. Such a list may not contain more values than there are elements in the `@from` attribute:

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
<reorder from="\u1A60" order="127" />      <!-- max possible order -->
<reorder from="\u1A6B" order="42" />
<reorder from="[\u1A75-\u1A79]" order="55" />
<reorder before="\u1A6B" from="\u1A60\u1A45" order="10" />
<reorder before="\u1A6B[\u1A75-\u1A79]" from="\u1A60\u1A45" order="10" />
<reorder before="\u1A6B" from="\u1A60[\u1A75-\u1A79]\u1A45" order="10 55 10" />
```

The first reorder is the default ordering for the _sakot_ which allows for it to be placed anywhere in a sequence, but moves any non-consonants that may immediately follow it, back before it in the sequence. The next two rules give the orders for the top vowel component and tone marks respectively. The next three rules give the _sakot_ and _wa_ characters a primary order that places them before the _o_. Notice particularly the final reorder rule where the _sakot_+_wa_ is split by the tone mark. This rule is necessary in case someone types into the middle of previously normalized text.

`<reorder>` elements are priority ordered based first on the length of string their `@from` attribute matches and then the sum of the lengths of the strings their `@before` attribute matches.

#### Using `<import>` with `<reorder>` elements

This section describes the impact of using [`import`](#element-import) elements with `<reorder>` elements.

The @from string in a `<reorder>` element describes a set of strings that it matches. This also holds for the `@before` attribute. The **intersection** of any two `<reorder>` elements consists of the intersections of their `@from` and `@before` string sets. Tooling should warn users if the intersection between any two `<reorder>` elements in the same `<transformGroup>` element to be non empty prior to processing imports.

If two `<reorder>` elements have a non empty intersection, then they are split and merged. They are split such that where there were two `<reorder>` elements, there are, in effect (but not actuality), three elements consisting of:

* `@from`, `@before` that match the intersection of the two rules. The other attributes are merged, as described below.
* `@from`, `@before` that match the set of strings in the first rule not in the intersection with the other attributes from the first rule.
* `@from`, `@before` that match the set of strings in the second rule not in the intersection, with the other attributes from the second rule.

When merging the other attributes, the second rule is taken to have priority (being an override of the earlier element). Where the second rule does not define the value for a character but the first does, the value is taken from the first rule, otherwise it is taken from the second rule.

Notice that it is possible for two rules to match the same string, but for them not to merge because the distribution of the string across `@before` and `@from` is different. For example, the following would not merge:

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
    <reorder from="\u103C" order="20" />

    <!-- [medial-wa or shan-medial-wa] -->
    <reorder from="[\u103D\u1082]" order="25" />

    <!-- [medial-ha or shan-medial-wa]+asat = Mon asat -->
    <reorder from="[\u103E\u1082]\u103A" order="27" />

    <!-- [medial-ha or mon-medial-wa] -->
    <reorder from="[\u103E\u1060]" order="27" />

    <!-- [e-vowel (U+1031) or shan-e-vowel (U+1084)] -->
    <reorder from="[\u1031\u1084]" order="30" />

    <reorder from="[\u102D\u102E\u1033-\u1035\u1071-\u1074\u1085\u109D\uA9E5]" order="35" />
</transformGroup>
```

A particular Myanmar keyboard layout can have these `reorder` elements:

```xml
<transformGroup>
    <import path="myanmar-reordering.xml"/> <!-- import the above transformGroup -->
    <!-- Kinzi -->
    <reorder from="\u1004\u103A\u1039" order="-1" />

    <!-- e-vowel -->
    <reorder from="\u1031" preBase="1" />

    <!-- medial-r -->
    <reorder from="\u103C" preBase="1" />
</transformGroup>
```

The effect of this is that the _e-vowel_ will be identified as a prebase and will have an order of 30. Likewise a _medial-r_ will be identified as a prebase and will have an order of 20. Notice that a _shan-e-vowel_ (`\u1084`) will not be identified as a prebase (even if it should be!). The _kinzi_ is described in the layout since it moves something across a run boundary. By separating such movements (prebase or moving to in front of a base) from the shared ordering rules, the shared ordering rules become a self-contained combining order description that can be used in other keyboards or even in other contexts than keyboarding.

#### Example Post-reorder transforms

It may be desired to perform additional processing following reorder operations.  This may be aaccomplished by adding an additional `<transformGroup>` element after the reorders.

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
    <transform from="\u17C1\u17B8" to="\u17BE" />
    <transform from="\u17C1\u17B6" to="\u17C4" />
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
    <transform from="[\u102F\u1030\u1048\u1059][\u102F\u1030\u1048\u1059]"  />
</transformGroup>
```

* * *

### <a name="Element_backspaces" href="#Element_backspaces">transform type="backspace"</a>

The `<transforms type="backspace">` describe an optional transform that is not applied on input of normal characters, but is only used to perform extra backspace modifications to previously committed text.

When the backspace key is pressed, the `<transforms type="backspace">` element (if present) is processed, and then the `<transforms type="simple">` element (if processed) as with any other key.

Keyboarding applications typically work, but are not required to, in one of two modes:

**_text entry_**

> text entry happens while a user is typing new text. A user typically wants the backspace key to undo whatever they last typed, whether or not they typed things in the 'right' order.

**_text editing_**

> text editing happens when a user moves the cursor into some previously entered text which may have been entered by someone else. As such, there is no way to know in which order things were typed, but a user will still want appropriate behaviour when they press backspace. This may involve deleting more than one character or replacing a sequence of characters with a different sequence.

In text editing mode, different keyboard layouts may behave differently in the same textual context. The backspace transform allows the keyboard layout to specify the effect of pressing backspace in a particular textual context. This is done by specifying a set of backspace rules that match a string before the cursor and replace it with another string. The rules are expressed within a `transforms type="backspace"` element.


```xml
<transforms type="backspace">
    <transformGroup>
        <transform from="{combination of characters}" [to="{output}"] />
    </transformGroup>
</transforms>
```

**Example**

For example, consider deleting a Devanagari ksha क्श:

While this character is made up of three codepoints, the following rule causes all three to be deleted by a single press of the backspace.


```xml
<transforms type="backspace">
    <transformGroup>
        <transform from="\u0915\u094D\u0936"/>
    </transformGroup>
</transforms>
```

Note that the optional attribute `@to` is omitted, since the whole string is being deleted. This is not uncommon in backspace transforms.

A more complex example comes from a Burmese visually ordered keyboard:

```xml
<transforms type="backspace">
    <transformGroup>
        <!-- Kinzi -->
        <transform from="[\u1004\u101B\u105A]\u103A\u1039" />

        <!-- subjoined consonant -->
        <transform from="\u1039[\u1000-\u101C\u101E\u1020\u1021\u1050\u1051\u105A-\u105D]" />

        <!-- tone mark -->
        <transform from="\u102B\u103A" />

        <!-- Handle prebases -->
        <!-- diacritics stored before e-vowel -->
        <transform from="[\u103A-\u103F\u105E-\u1060\u1082]\u1031" to="\u1031" />

        <!-- diacritics stored before medial r -->
        <transform from="[\u103A-\u103B\u105E-\u105F]\u103C" to="\u103C" />

        <!-- subjoined consonant before e-vowel -->
        <transform from="\u1039[\u1000-\u101C\u101E\u1020\u1021]\u1031" to="\u1031" />

        <!-- base consonant before e-vowel -->
        <transform from="[\u1000-\u102A\u103F-\u1049\u104E]\u1031" to="\m{prebase}\u1031" />

        <!-- subjoined consonant before medial r -->
        <transform from="\u1039[\u1000-\u101C\u101E\u1020\u1021]\u103C" to="\u103C" />

        <!-- base consonant before medial r -->
        <transform from="[\u1000-\u102A\u103F-\u1049\u104E]\u103C" to="\m{prebase}\u103C" />

        <!-- delete lone medial r or e-vowel -->
        <transform from="\m{prebase}[\u1031\u103C]" />
    </transformGroup>
</transforms>
```

The above example is simplified, and doesn't fully handle the interaction between medial-r and e-vowel.


> The character `\m{prebase}` does not represent a literal character, but is instead a special marker, used as a "filler string". When a keyboard implementation handles a user pressing a key that inserts a prebase character, it also has to insert a special filler string before the prebase to ensure that the prebase character does not combine with the previous cluster. See the reorder transform for details. See [markers](#markers) for the `\m` syntax.

The first three transforms above delete various ligatures with a single keypress. The other transforms handle prebase characters. There are two in this Burmese keyboard. The transforms delete the characters preceding the prebase character up to base which gets replaced with the prebase filler string, which represents a null base. Finally the prebase filler string + prebase is deleted as a unit.

If no specified transform among all `transformGroup`s under the `<transforms type="backspace">` element matches, a default will be used instead — an implied final transform that simply deletes the codepoint at the end of the input context. This implied transform is effectively similar to the following code sample, even though the `*` operator is not actually allowed in `from=`.  See the documentation for *Match a single Unicode codepoint* under [transform syntax](#regex-like-syntax) and [markers](#markers), above.

It is important that implementations do not by default delete more than one non-marker codepoint at a time, except in the case of emoji clusters. Note that implementations will vary in the emoji handling due to the iterative nature of successive Unicode releases. See [UTS#51 §2.4.2: Emoji Modifiers in Text](https://www.unicode.org/reports/tr51/#Emoji_Modifiers_in_Text)

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

## <a name="Invariants" href="#Invariants">Invariants</a>

Beyond what the DTD imposes, certain other restrictions on the data are imposed on the data.
Please note the constraints given under each element section above.
DTD validation alone is not sufficient to verify a keyboard file.

<!--
TODO: Rewrite this? Probably push out to each element's section?

3.  No `keyMap[@modifiers]` value can overlap with another `keyMap[@modifiers]` value.
    * eg you can't have `"RAlt Ctrl"` in one `keyMap`, and `"Alt Shift"` in another (because Alt = RAltLAlt).
4.  Every sequence of characters in a `transform[@from]` value must be a concatenation of two or more `map[@to]` values.
    * eg with `<transform from="xyz" to="q">` there must be some map values to get there, such as `<map... to="xy">` & `<map... to="z">`
5.  If the base and chars values for `modifiers=""` are all identical, and there are no longpresses, that `keyMap` must not appear (??)
6.  There will never be overlaps among modifier values.
7.  A modifier set will never have ? (optional) on all values
    * eg, you'll never have `RCtrl?Caps?LShift?`
8.  Every `base[@base`] value must be unique.
9. A `modifier` attribute value will aways be minimal, observing the following simplification rules.

| Notation                                 | Notes |
|------------------------------------------|-------|
| Lower case character (e.g. _x_ )          | Interpreted as any combination of modifiers. <br/> (e.g. _x_ = CtrlShiftOption) |
| Upper-case character (e.g. _Y_ )          | Interpreted as a single modifier key (which may or may not have an L and R variant) <br/> (e.g. _Y_ = Ctrl, _RY_ = RCtrl, etc.) |
| Y? ⇔ Y ∨ ∅ <br/> Y ⇔ LY ∨ RY ∨ LYRY | E.g. Opt? ⇔ ROpt ∨ LOpt ∨ ROptLOpt ∨ ∅ <br/> E.g. Opt ⇔ ROpt ∨ LOpt ∨ ROptLOpt |

| Axiom                                       | Example                                      |
|---------------------------------------------|----------------------------------------------|
| xY ∨ x ⇒ xY?                              | OptCtrlShift OptCtrl → OptCtrlShift?         |
| xRY ∨ xY? ⇒ xY? <br/> xLY ∨ xY? ⇒ xY?   | OptCtrlRShift OptCtrlShift? → OptCtrlShift?  |
| xRY? ∨ xY ⇒ xY? <br/> xLY? ∨ xY ⇒ xY?   | OptCtrlRShift? OptCtrlShift → OptCtrlShift?  |
| xRY? ∨ xY? ⇒ xY? <br/> xLY? ∨ xY? ⇒ xY? | OptCtrlRShift? OptCtrlShift? → OptCtrlShift? |
| xRY ∨ xY ⇒ xY <br/> xLY ∨ xY ⇒ xY       | OptCtrlRShift OptCtrlShift → OptCtrlShift?   |
| LY?RY?                                      | OptRCtrl?LCtrl? → OptCtrl?                   |
| xLY? ⋁ xLY ⇒ xLY?                          |                                              |
| xY? ⋁ xY ⇒ xY?                             |                                              |
| xY? ⋁ x ⇒ xY?                              |                                              |
| xLY? ⋁ x ⇒ xLY?                            |                                              |
| xLY ⋁ x ⇒ xLY?                             |                                              |
-->

* * *

## <a name="Keyboard_IDs" href="#Keyboard_IDs">Keyboard IDs</a>

There is a set of subtags that help identify the keyboards. Each of these are used after the `"t-k0"` subtags to help identify the keyboards. The first tag appended is a mandatory platform tag followed by zero or more tags that help differentiate the keyboard from others with the same locale code.

### <a name="Principles_for_Keyboard_IDs" href="#Principles_for_Keyboard_IDs">Principles for Keyboard IDs</a>

The following are the design principles for the IDs.

1. BCP47 compliant.
   1. Eg, `en`, `sr-Cyrl`, or `en-t-k0-extended`.
2. Use the minimal language id based on `likelySubtags` (see [Part 1: Likely Subtags](tr35.md#Likely_Subtags))
   1. Eg, instead of `fa-Arab`, use `fa`.
   2. The data is in <https://github.com/unicode-org/cldr/blob/main/common/supplemental/likelySubtags.xml>
3. Keyboard files should be platform-independent, however, if included, a platform id is the first subtag after `-t-k0-`. If a keyboard on the platform changes over time, both are dated, eg `bg-t-k0-chromeos-2011`. When selecting, if there is no date, it means the latest one.
4. Keyboards are only tagged that differ from the "standard for each language". That is, for each language on a platform, there will be a keyboard with no subtags. Subtags with common semantics across languages and platforms are used, such as `-extended`, `-phonetic`, `-qwerty`, `-qwertz`, `-azerty`, …
5. In order to get to 8 letters, abbreviations are reused that are already in [bcp47](https://github.com/unicode-org/cldr/blob/main/common/bcp47/) -u/-t extensions and in [language-subtag-registry](https://www.iana.org/assignments/language-subtag-registry) variants, eg for Traditional use `-trad` or `-traditio` (both exist in [bcp47](https://github.com/unicode-org/cldr/blob/main/common/bcp47/)).
6. Multiple languages cannot be indicated in the locale id, so the predominant target is used.
   1. For Finnish + Sami, use `fi-t-k0-smi` or `extended-smi`
   2. The [`<locales>`](#Element_locales) element may be used to identify additional languages.
7. In some cases, there are multiple subtags, like `en-US-t-k0-chromeos-intl-altgr.xml`
8. Otherwise, platform names are used as a guide.

**Examples**

```xml
<!-- Serbian Latin -->
<keyboard locale="sr-Latn"/>
```

```xml
<!-- Serbian Cyrillic -->
<keyboard locale="sr-Cyrl"/>
```

```xml
<!-- Pan Nigerian Keyboard-->
<keyboard locale="mul-Latn-NG-t-k0-panng">
    <locales>
    <locale id="ha"/>
    <locale id="ig"/>
    <!-- others … -->
    </locales>
</keyboard>
```

```xml
<!-- Finnish Keyboard including Skolt Sami -->
<keyboard locale="fi-t-k0-smi">
    <locales>
    <locale id="sms"/>
    </locales>
</keyboard>
```

* * *

## <a name="Platform_Behaviors_in_Edge_Cases" href="#Platform_Behaviors_in_Edge_Cases">Platform Behaviors in Edge Cases</a>

| Platform | No modifier combination match is available | No map match is available for key position | Transform fails (i.e. if \^d is pressed when that transform does not exist) |
|----------|--------------------------------------------|--------------------------------------------|---------------------------------------------------------------------------|
| Chrome OS | Fall back to base | Fall back to character in a keyMap with same "level" of modifier combination. If this character does not exist, fall back to (n-1) level. (This is handled data-generation-side.) <br/> In the spec: No output | No output at all |
| Mac OS X  | Fall back to base (unless combination is some sort of keyboard shortcut, e.g. cmd-c) | No output | Both keys are output separately |
| Windows  | No output | No output | Both keys are output separately |

* * *

## <a name="CLDR_VKey_Enum" href="#CLDR_VKey_Enum">CLDR VKey Enum</a></a>

In the following chart, “CLDR Name” indicates the value used with the `from` and `to` attributes of the [vkey](#Element_vkey) element.

| CLDR Name | US English ISO | Hex<sup>1</sup> | Notes       |
|-----------|----------------|-----------------|-------------|
| SPACE     | A03            | 0x20            |
| 0         | E10            | 0x30            |
| 1         | E01            | 0x31            |
| 2         | E02            | 0x32            |
| 3         | E03            | 0x33            |
| 4         | E04            | 0x34            |
| 5         | E05            | 0x35            |
| 6         | E06            | 0x36            |
| 7         | E07            | 0x37            |
| 8         | E08            | 0x38            |
| 9         | E09            | 0x39            |
| A         | C01            | 0x41            |
| B         | B05            | 0x42            |
| C         | B03            | 0x43            |
| D         | C03            | 0x44            |
| E         | D03            | 0x45            |
| F         | C04            | 0x46            |
| G         | C05            | 0x47            |
| H         | C06            | 0x48            |
| I         | D08            | 0x49            |
| J         | C07            | 0x4A            |
| K         | C08            | 0x4B            |
| L         | C09            | 0x4C            |
| M         | B07            | 0x4D            |
| N         | B06            | 0x4E            |
| O         | D09            | 0x4F            |
| P         | D10            | 0x50            |
| Q         | D01            | 0x51            |
| R         | D04            | 0x52            |
| S         | C02            | 0x53            |
| T         | D05            | 0x54            |
| U         | D07            | 0x55            |
| V         | B05            | 0x56            |
| W         | D02            | 0x57            |
| X         | B02            | 0x58            |
| Y         | D06            | 0x59            |
| Z         | B01            | 0x5A            |
| SEMICOLON | C10            | 0xBA            |
| EQUAL     | E12            | 0xBB            |
| COMMA     | B08            | 0xBC            |
| HYPHEN    | E11            | 0xBD            |
| PERIOD    | B09            | 0xBE            |
| SLASH     | B10            | 0xBF            |
| GRAVE     | E00            | 0xC0            |
| LBRACKET  | D11            | 0xDB            |
| BACKSLASH | D13            | 0xDC            |
| RBRACKET  | D12            | 0xDD            |
| QUOTE     | C11            | 0xDE            |
| LESS-THAN | B00            | 0xE2            | 102nd key on European layouts, right of left shift.                  |
| ABNT2     | B11            | 0xC1            | Extra key, left of right-shift, Brazilian Portuguese ABNT2 keyboards |

Footnotes:

* <sup>1</sup> Hex value from Windows, web standards, Keyman, etc.

* * *

## Keyboard Test Data

Keyboard Test Data allows the keyboard author to provide regression test data to validate the repertoire and behavior of a keyboard. Tooling can run these regression tests against an implementation, and can also be used as part of the development cycle to validate that keyboard changes do not deviate from expected behavior.  In the interest of complete coverage, tooling could also indicate whether all keys and gestures in a layout are exercised by the test data.

Test data files have a separate DTD, named `ldmlKeyboardTest.dtd`.  Note that multiple test data files can refer to the same keyboard. Test files should be named similarly to the keyboards which they test, such as `fr_test.xml` to test `fr.xml`.

Sample test data files are located in the `keyboards/test` subdirectory.

The following describes the structure of a keyboard test file.

### Test Doctype

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE keyboardTest SYSTEM "../dtd/ldmlKeyboardTest.dtd">
```

The top level element is named `keyboardTest`.

### Test Element: keyboardTest

> <small>
>
> Children: [info](#test-element-info), [repertoire](#test-element-repertoire), [_special_](tr35.md#special), [tests](#test-element-tests)
> </small>

This is the top level element.

_Attribute:_ `conformsTo` (required)

The `conformsTo` attribute here is the same as on the [`<keyboard>`](#Element_Keyboard) element.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE keyboardTest SYSTEM "../dtd/ldmlKeyboardTest.dtd">
<keyboardTest conformsTo="techpreview">
    …
</keyboardTest>
```

### Test Element: info

> <small>
>
> Parents: [keyboardTest](#test-element-keyboardtest)
>>
> Occurrence: Required, Single
> </small>

_Attribute:_ `author`

This freeform attribute allows for description of the author or authors of this test file.

_Attribute:_ `keyboard` (required)

This attribute specifies the keyboard’s file name, such as `fr-t-k0-azerty.xml`.

_Attribute:_ `name` (required)

This attribute specifies a name for this overall test file. These names could be output to the user during test execution, used to summarize success and failure, or used to select or deselect test components.

**Example**

```xml
<info keyboard="fr-t-k0-azerty.xml" author="Team Keyboard" name="fr-test" />
```

### Test Element: repertoire

> <small>
>
> Parents: [keyboardTest](#test-element-keyboardtest)
>
> Children: _none_
>
> Occurrence: Optional, Multiple
> </small>

This element represents a repertoire test, to validate the available characters and their reachability. This test ensures that each of the specified characters is somehow typeable on the keyboard, after transforms have been applied. The characters in the repertoire will be matched against the complete set of possible generated outputs, post-transform, of all keys on the keyboard.

_Attribute:_ `name` (required)

This attribute specifies a unique name for this repertoire test. These names could be output to the user during test execution, used to summarize success and failure, or used to select or deselect test components.

_Attribute:_ `type`

This attribute is one of the following:

|  type     | Meaning                                                                                                  |
|-----------|----------------------------------------------------------------------------------------------------------|
| default   | This is the default, indicates that _any_ gesture or keystroke may be used to generate each character    |
| simple    | Each of the characters must be typeable by simple single keystrokes without needing any gestures.        |
| gesture   | The characters are typeable by use of any gestures such as flicks, long presses, or multiple taps.       |
| flick     | The characters are typeable by use of any `flick` element.                                               |
| longPress | The characters are typeable by use of any `longPress` value.                                             |
| multiTap  | The characters are typeable by use of any `multiTap` value.                                              |
| hardware  | The characters are typeable by use of any simple keystrokes on any hardware layout.                      |

_Attribute:_ `chars` (required)

This attribute specifies a list of characters in UnicodeSet format, which is specified in [UTS #35 Part One](tr35.md#Unicode_Sets).

**Example**

```xml
<repertoire chars="[a b c d e \u{22}]" type="default" />

<!-- taken from CLDR's common/main/fr.xml main exemplars - indicates that all of these characters should be reachable without requiring a gesture.
Note that the 'name' is arbitrary. -->
<repertoire name="cldr-fr-main" chars="[a à â æ b c ç d e é è ê ë f g h i î ï j k l m n o ô œ p q r s t u ù û ü v w x y ÿ z]" type="simple" />

<!-- taken from CLDR's common/main/fr.xml auxiliary exemplars - indicates that all of these characters should be reachable even if a gesture is required.-->
<repertoire name="cldr-fr-auxiliary" chars="[á å ä ã ā ć ē í ì ī ĳ ñ ó ò ö õ ø ř š ſ ß ú ǔ]" type="gesture" />

```

Note: CLDR’s extensive [exemplar set](tr35-general.md#Character_Elements) data may be useful in validating a language’s repertoire against a keyboard. Tooling may wish to make use of this data in order to suggest recommended repertoire values for a language.

### Test Element: tests

> <small>
>
> Parents: [keyboardTest](#test-element-keyboardtest)
>
> Children: [_special_](tr35.md#special), [test](#test-element-test)
>
> Occurrence: Optional, Multiple
> </small>

This element specifies a particular suite of `<test>` elements.

_Attribute:_ `name` (required)

This attribute specifies a unique name for this suite of tests. These names could be output to the user during test execution, used to summarize success and failure, or used to select or deselect test components.

**Example**

```xml
<tests name="key-tests">
    <test name="key-test">
        …
    </test>
    <test name="gestures-test">
        …
    </test>
</tests>
<tests name="transform tests">
    <test name="transform test">
        …
    </test>
</tests>
```

### Test Element: test

> <small>
>
> Parents: [tests](#test-element-tests)
>
> Children: [startContext](#test-element-startContext), [emit](#test-element-emit), [keystroke](#test-element-keystroke), [backspace](#test-element-backspace), [check](#test-element-check), [_special_](tr35.md#special)
>
> Occurrence: Required, Multiple
> </small>

This attribute specifies a specific isolated regression test. Multiple test elements do not interact with each other.

The order of child elements is significant, with cumulative effects: they must be processed from first to last.

_Attribute:_ `name` (required)

This attribute specifies a unique name for this particular test. These names could be output to the user during test execution, used to summarize success and failure, or used to select or deselect test components.

**Example**

```xml
<info keyboard="fr-t-k0-azerty.xml" author="Team Keyboard" name="fr-test" />
```

### Test Element: startContext

This element specifies pre-existing text in a document, as if prior to the user’s insertion point. This is useful for testing transforms and reordering. If not specified, the startContext can be considered to be the empty string ("").

> <small>
>
> Parents: [test](#Element_test)
>
> Children: _none_
>
> Occurrence: Optional, Single
> </small>

_Attribute:_ `to` (required)

Specifies the starting context. This text may be escaped with `\u` notation, see [Escaping](#Escaping).

**Example**

```xml
<startContext to="abc\u0022"/>
```


### Test Element: keystroke

> <small>
>
> Parents: [test](#test-element-test)
>
> Children: _none_
>
> Occurrence: Optional, Multiple
> </small>

This element represents a single keystroke or other gesture event, identified by a particular key element.

Optionally, one of the gesture attributes, either `flick`, `longPress`, or `tapCount` may be specified. If none of the gesture attributes are specified, then a regular keypress is effected on the key.  It is an error to specify more than one gesture attribute.

If a key is not found, or a particular gesture has no definition, the output should be behave as if the user attempted to perform such an action.  For example, an unspecified `flick` would result in no output.

When a key is found, processing continues with the transform and other elements before updating the test output buffer.

_Attribute:_ `key` (required)

This attribute specifies a key by means of the key’s `id` attribute.

_Attribute:_ `flick`

This attribute specifies a flick gesture to be performed on the specified key instead of a keypress, such as `e` or `nw se`. This value corresponds to the `directions` attribute of the [`<flick>`](#Element_flicks) element.

_Attribute:_ `longPress`

This attribute specifies that a long press gesture should be performed on the specified key instead of a keypress. For example, `longPress="2"` indicates that the second character in a longpress series should be chosen. `longPress="0"` indicates that the `longPressDefault` value, if any, should be chosen. This corresponds to `longPress` and `longPressDefault` on [`<key>`](#Element_key).

_Attribute:_ `tapCount`

This attribute specifies that a multi-tap gesture should be performed on the specified key instead of a keypress. For example, `tapCount="3"` indicates that the key should be tapped three times in rapid succession. This corresponds to `multiTap` on [`<key>`](#Element_key). The minimum tapCount is 2.

**Example**

```xml
<keystroke key="q"/>
<keystroke key="doublequote"/>
<keystroke key="s" flick="nw se"/>
<keystroke key="e" longPress="1"/>
<keystroke key="E" tapCount="2"/>
```

### Test Element: emit

> <small>
>
> Parents: [test](#test-element-test)
>
> Children: _none_
>
> Occurrence: Optional, Multiple
> </small>

This element also represents an input event, except that the input is specified in terms of textual value rather than key or gesture identity. This element is particularly useful for testing transforms.

Processing of the specified text continues with the transform and other elements before updating the test output buffer.

_Attribute:_ `to` (required)

This attribute specifies a string of output text representing a single keystroke or gesture. This string is intended to match the output of a `key`, `flick`, `longPress` or `multiTap` element or attribute.
Tooling should give a hint if this attribute does not match at least one keystroke or gesture. Note that the specified text is not injected directly into the output buffer.

This attribute may be escaped with `\u` notation, see [Escaping](#Escaping).

**Example**

```xml
<emit to="s"/>
```


### Test Element: backspace

> <small>
>
> Parents: [test](#test-element-test)
>
> Children: _none_
>
> Occurrence: Optional, Multiple
> </small>

This element represents a backspace action, as if the user typed the backspace key

**Example**

```xml
<backspace/>
```

### Test Element: check

> <small>
>
> Parents: [test](#test-element-test)
>
> Children: _none_
>
> Occurrence: Optional, Multiple
> </small>

This element represents a check on the current output buffer.

_Attribute:_ `result` (required)

This attribute specifies the expected resultant text in a document after processing this event and all prior events, and including any `startContext` text.  This text may be escaped with `\u` notation, see [Escaping](#Escaping).

**Example**

```xml
<check result="abc\u0022s\u0022•éÈ"/>
```


### Test Examples

```xml

<test name="spec-sample">
    <startContext to="abc\u0022"/>
    <!-- simple, key specified by to -->
    <emit to="s"/>
    <check result="abc\u0022s"/>
    <!-- simple, key specified by id -->
    <keystroke key="doublequote"/>
    <check result="abc\u0022s\u0022"/>
    <!-- flick -->
    <keystroke key="s" flick="nw se"/>
    <check result="abc\u0022s\u0022•"/>
    <!-- longPress -->
    <keystroke key="e" longPress="1"/>
    <check result="abc\u0022s\u0022•é"/>
    <!-- multiTap -->
    <keystroke key="E" tapCount="2"/>
    <check result="abc\u0022s\u0022•éÈ"/>
</test>
```

* * *

Copyright © 2001–2023 Unicode, Inc. All Rights Reserved. The Unicode Consortium makes no expressed or implied warranty of any kind, and assumes no liability for errors or omissions. No liability is assumed for incidental and consequential damages in connection with or arising out of the use of the information or programs contained or accompanying this technical report. The Unicode [Terms of Use](https://www.unicode.org/copyright.html) apply.

Unicode and the Unicode logo are trademarks of Unicode, Inc., and are registered in some jurisdictions.

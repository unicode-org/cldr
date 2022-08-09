## Unicode Technical Standard #35 Tech Preview

# Unicode Locale Data Markup Language (LDML)<br/>Part 7: Keyboards

<!-- HTML: no th -->
<table><tbody>
<tr><td>Version</td><td>Technical Preview (3.0)</td></tr>
<tr><td>Editors</td><td>Steven R. Loomis (<a href="mailto:srloomis@unicode.org">srloomis@unicode.org</a>) (for this draft), the <a href="https://cldr.unicode.org/index/keyboard-workgroup">CLDR Keyboard-SC</a>, and <a href="tr35.html#Acknowledgments">other CLDR committee members</a></td></tr>
</tbody></table>

For the full header, summary, and status, see [Part 1: Core](tr35.md).

#### _Important Note_

> This is a technical preview of a future version of the LDML Part 7. See [_Status_](#status), below.
>
> There are breaking changes, see [Compatibility Notice](#Compatibility_Notice)

### _Summary_

This document describes parts of an XML format (_vocabulary_) for the exchange of structured locale data. This format is used in the [Unicode Common Locale Data Repository](https://unicode.org/cldr/).

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
  * [Element: displayMap](#Element_displayMap)
  * [Element: display](#Element_display)
  * [Element: displayOptions](#Element_displayOptions)
  * [Element: layerMaps](#Element_layerMaps)
  * [Element: layerMap](#Element_layerMap)
  * [Element: row](#Element_row)
  * [Element: vkeyMaps](#Element_vkeyMaps)
  * [Element: vkeyMap](#Element_vkeyMap)
  * [Element: transforms](#Element_transforms)
  * [Element: transform](#Element_transform)
  * [Element: reorders](#Element_reorders)
  * [Element: reorder](#Element_reorder)
  * [Element: transform final](#Element_final)
  * [Element: backspaces](#Element_backspaces)
  * [Element: backspace](#Element_backspace)
* [Invariants](#Invariants)
* [Keyboard IDs](#Keyboard_IDs)
  * [Principles for Keyboard IDs](#Principles_for_Keyboard_IDs)
* [Platform Behaviors in Edge Cases](#Platform_Behaviors_in_Edge_Cases)
* [CLDR VKey Enum](#CLDR_VKey_Enum)

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
2. Some features, such as multitap and flicks, have the potential to reduce accessibility and thus should be discouraged. For example, multitap requires pressing keys at a certain speed, and flicks require a more complex movement (press-and-flick) beyond a simple tap.
3. Public feedback is welcome on any aspects of this document which might hinder accessibility.

## <a name="Definitions" href="#Definitions">Definitions</a>

**Arrangement** is the term used to describe the relative position of the rectangles that represent keys, either physically or virtually. A physical keyboard has a static arrangement while a virtual keyboard may have a dynamic arrangement that changes per language and/or layer. While the arrangement of keys on a keyboard may be fixed, the mapping of those keys may vary.

**Base character:** The character emitted by a particular key when no modifiers are active. In ISO terms, this is group 1, level 1.

**Base map:** A mapping from the positions to the base characters. There is only one base map per layout. The characters on this map can be output without the use of any modifier keys.

**Core keys:** also known as “alpha” block. The primary set of key values on a keyboard that are used for typing the target language of the keyboard. For example, the three rows of letters on a standard US QWERTY keyboard (QWERTYUIOP, ASDFGHJKL, ZXCVBNM) together with the most significant punctuation keys. Usually this equates to the minimal keyset for a language as seen on mobile phone keyboards.
Distinguished from the **frame keys**.

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

**Transform:** A transform is an element that specifies a set of conversions from sequences of code points into one (or more) other code points. For example, in most latin keyboards hitting the "^" dead-key followed by the "e" key produces "ê".

**Virtual keyboard:** see **Touch keyboard**

### <a name="Escaping" href="#Escaping">Escaping</a>

When explicitly specified, attributes can contain escaped characters. This specification uses two methods of escaping, the _UnicodeSet_ notation and the `\u{...}` notation.

The _UnicodeSet_ notation is described in [UTS #35 section 5.3.3](tr35.md#Unicode_Sets) and allows for comprehensive character matching, including by character range, properties, names, or codepoints. Currently, the following attributes allow _UnicodeSet_ notation:

* `from`, `before`, `after` on the `<transform>` element
* `from`, `before`, `after` on the `<reorder>` element
* `from`, `before`, `after` on the `<backspace>` element

The `\u{...}` notation, a subset of hex notation, is described in [UTS #18 section 1.1](https://www.unicode.org/reports/tr18/#Hex_notation). It can refer to one or multiple individual codepoints. Currently, the following attributes allow the `\u{...}` notation:

* `to`, `longPress`, `multitap`, `hint` on the `<map>` element
* `to` on the `<transform>` element
* `to` on the `<backspace>` element

Characters of general category of Combining Mark (M), Control characters (Cc), Format characters (Cf), and whitespace other than space should be encoded using one of the notation above as appropriate.

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

<!-- Each platform has its own directory, where a "platform" is a designation for a set of keyboards available from a particular source, such as Windows or Chrome OS. This directory name is the platform name (see Table 2 located further in the document). Within this directory there are two types of files:

1. A single platform file (see XML structure for Platform file), this file includes a mapping of hardware key codes to the ISO layout positions. This file is also open to expansion for any configuration elements that are valid across the whole platform and that are not layout specific. This file is simply called `_platform.xml`.
2. Multiple layout files named by their locale identifiers (e.g. `lt-t-k0-chromeos.xml` or `ne-t-k0-windows.xml`).

Keyboard data that is not supported on a given platform, but intended for use with that platform, may be added to the directory `/und/`. For example, there could be a file `/und/lt-t-k0-chromeos.xml`, where the data is intended for use with Chrome OS, but does not reflect data that is distributed as part of a standard Chrome OS release. -->

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
> Children: [backspaces](#Element_backspaces), [displayMap](#Element_displayMap), [info](#Element_info), [keys](#Element_keys), [layerMaps](#Element_layerMaps), [locales](#Element_locales), [names](#Element_names), [reorders](#Element_reorders), [settings](#Element_settings), [_special_](tr35.md#special), [transforms](#Element_transforms), [version](#Element_version), [vkeyMaps](#Element_vkeyMaps)
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

This mandatory attribute represents the primary locale of the keyboard using Unicode locale identifiers (see [LDML](tr35.md)) - for example `"el"` for Greek. Sometimes, the locale may not specify the base language. For example, a Devanagari keyboard for many languages could be specified by BCP-47 code: `"mul-Deva"`. For further details, see [Keyboard IDs](#Keyboard_IDs).

**Example** (for illustrative purposes only, not indicative of the real data)

```xml
<keyboard locale="ka-t-k0-qwerty-windows">
  …
</keyboard>
```

```xml
<keyboard locale="fr-CH-t-k0-android">
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

> The BCP47 locale ID of an additional language supported by this keyboard.
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

> Must be a [semver-compatible](https://semver.org) version number.
<!-- TODO make the above into a reverence to SEMVER -->

_Attribute:_ `cldrVersion` (fixed by DTD)

> The CLDR specification version that is associated with this data file. This value is fixed and is inherited from the [DTD file](https://github.com/unicode-org/cldr/tree/main/keyboards/dtd) and therefore does not show up directly in the XML file.

**Example**

```xml
<keyboard locale="..-osx">
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
> An example use case is aiding a user to choose among the two same layouts with one outputting characters in the normalization form C and one in the normalization form D.

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

An element used to keep track of layout specific settings. This element may or may not show up on a layout. These settings reflect the normal practice by the implementation. However, an implementation using the data may customize the behavior. For example, for `transformFailure` the implementation could ignore the setting, or modify the text buffer in some other way (such as by emitting backspaces).

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

_Attribute:_ `transformFailure="omit"`

> This attribute describes the behavior of a transform when it is escaped (see the `transform` element in the Layout file for more information). A transform is escaped when it can no longer continue due to the entry of an invalid key. For example, suppose the following set of transforms are valid:
>
> ^e → ê
>
> ^a → â

Suppose a user now enters the "\^" key then "\^" is now stored in a buffer and may or may not be shown to the user (see the `partial` attribute).

If a user now enters d, then the transform has failed and there are two options for output.

1. default behavior - "^d"

2. omit - "" (nothing and the buffer is cleared)

The default behavior (when this attribute is not present) is to emit the contents of the buffer upon failure of a transform.

If this attribute is present, it must have a value of omit.

_Attribute:_ `transformPartial="hide"`

> This attribute describes the behavior of the system while in a transform. When this attribute is present then don't show the values of the buffer as the user is typing a transform (this behavior can be seen on Windows or Linux platforms).

By default (when this attribute is not present), show the values of the buffer as the user is typing a transform (this behavior can be seen on the macOS platform).

If this attribute is present, it must have a value of hide.

**Example**

```xml
<keyboard locale="bg-t-k0-windows-phonetic-trad">
    …
    <settings fallback="omit" transformPartial="hide" />
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
> Occurrence: required, single
>
> </small>



* * *

### <a name="Element_key" href="#Element_key">Element: key</a>

This element defines a mapping between an abstract key and its output. This element must have the `keys` element as its parent. The `key` element is referened by the `keys=` attribute of the [`row` element](#Element_row).

**Syntax**

```xml
<key
 id="{key id}"
 [flicks="{flicks identifier}"]
 [gap="true"]
 [longPress="{long press keys}"]
 [longPressDefault="{default longpress target}"]
 [multitap="{the output on subsequent taps}"]
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

**Note**: The `id` attribute is required, as is _at least one of_ `switch`, `gap`, or `to`.

_Attribute:_ `id`

> The `id` attribute uniquely identifies the key. NMTOKEN, restricted to "[a-zA-Z0-9_.-]". It can (but needn't be) the Latin key name for a Latn script keyboard (a, b, c, A, B, C, …), or any other valid token (e-acute, alef, alif, alpha, …)

_Attribute:_ `flicks="flick-id"` (optional)

> The `flicks` attribute indicates that this key makes use of a [`flicks`](#Element_flicks) set with the specified id.

_Attribute:_ `gap="true"` (optional)

> The `gap` attribute indicates that this key does not have any appearance, but represents a "gap" of the specified number of key widths. Can be used with `width` to set a width.

```xml
<key id="mediumgap" gap="true" width="1.5"/>
```

_Attribute:_ `longPress="a b c"` (optional) (discouraged, see [Accessibility](#Accessibility))

> The `longPress` attribute contains any characters that can be emitted by "long-pressing" a key, this feature is prominent in mobile devices. The possible sequences of characters that can be emitted are whitespace delimited. Control characters, combining marks and whitespace (which is intended to be a long-press option) in this attribute are escaped using the `\u{...}` notation.

_Attribute:_ `longPressDefault` (optional)

> Indicates which of the `longPress` target characters is the default long presstarget, which could be different than the first element. Ignored if not in the `longPress` list. Characters in this attribute can be escaped using the `\u{...}` notation.
> For example, if the `longPressDefault` is a key whose [display](#Element_displayMap) appears as `{` an implementation might render the key as follows:
>
> ![keycap hint](images/keycapHint.png)

_Attribute:_ `multitap` (optional) (discouraged, see [Accessibility])

> A space-delimited list of strings, where each successive element of the list is produced by the corresponding number of quick taps. For example, three taps on the key C01 will produce a “c” in the following example (first tap produces “a”, two taps produce “bb” etc.).
>
> _Example:_
>
> ```xml
> <key id="a" to="a" multitap="bb c d">
> ```
>
> Control characters, combining marks and whitespace (which is intended to be a multitap option) in this attribute are escaped using the `\u{...}` notation.

**Note**: Behavior past the end of the multitap list is implementation specific.

_Attribute:_ `switch="shift"` (optional)

> The `switch` attribute indicates that this key switches to another `layerMap` with the specified id (such as `<layerMap id="shift"/>` in this example).
> Note that a key may have both a `switch=` and a `to=` attribute, indicating that the key outputs prior to switching layers.
> Also note that `switch=` is ignored for hardware layouts: their shifting is controlled via
> the modifier keys.

_Attribute:_ `to` (required)

> The `to` attribute contains the output sequence of characters that is emitted when pressing this particular key. Control characters, whitespace (other than the regular space character) and combining marks in this attribute are escaped using the `\u{...}` notation. More than one key may output the same output.

_Attribute:_ `transform="no"` (optional)

> The `transform` attribute is used to define a key that never participates in a transform but its output shows up as part of a transform. This attribute is necessary because two different keys could output the same characters (with different keys or modifier combinations) but only one of them is intended to be a dead-key and participate in a transform. This attribute value must be no if it is present.
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

_Attribute:_ `width="1.2"` (optional, default "1.0")

> The `width` attribute indicates that this key has a different width than other keys, by the specified number of key widths.

```xml
<key id="wide-a" to="a" width="1.2"/>
<key id="wide-gap" gap="true" width="2.5"/>
```

##### Implied Keys

Not all keys need to be listed explicitly.  The following keys can be assumed to already exist:

```xml
<!--  all 26 upper and lower case English letters -->
<key id="a" to="a"/>
<key id="b" to="b"/>
<key id="c" to="c"/>
…
<key id="A" to="A"/>
<key id="B" to="B"/>
<key id="C" to="C"/>
…

<key id="space" to=" "/>  <!-- Note: 'space' is always considered 'stretchable'-->

<!-- modifiers-->
<key id="shift" shift="shift"/>
… 
```

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

> The `id` attribute identifies the flicks. It can be any NMTOKEN.
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
another file.

_Attribute:_ `base`

> The base may be omitted (indicating a local import) or have the value `"cldr"` (specifying CLDR standard files).

_Attribute:_ `path` (required)

The use case is to be able to import a standard set of vkeyMaps, transforms, and similar
from the CLDR repository.  `<import>` is not recommended as a way for keyboard authors to
split up their keyboard into multiple files, as the intent is for each single XML file to contain all that is needed for a keyboard layout.

`<import>` can be used as a child of a number of elements.
<!-- TODO: which ones?-->

**Syntax**

```xml
<!-- in a keyboard xml file-->
…
<transforms type="simple">
    <!-- This line is before the import -->
    <transform from="` " to="`" />
    <import path="cldr/standard_transforms.xml"/>
    <!-- This line is after the import -->
    <transform from="^ " to="^" />
</transforms>
…


<!-- contents of cldr/standard_transforms.xml -->
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

Note that the DOCTYPE and root element, here `transforms`, is the same as
the _parent_ of the `<import/>` element. It is an error to import an XML file
whois root element is different than the parent element of the `<import/>` element.

After loading, the above example will be the equivalent of the following.

```xml
<transforms type="simple">
    <!-- This line is before the import -->
    <transform from="` " to="`" />

    <!-- begin imported part-->
    <transform from="`a" to="à" />
    <transform from="`e" to="è" />
    <transform from="`i" to="ì" />
    <transform from="`o" to="ò" />
    <transform from="`u" to="ù" />
    <!-- end imported part -->

    <!-- This line is after the import -->
    <transform from="^ " to="^" />
</transforms>
```

> <small>
>
> Parents: [backspaces](#Element_backspaces), [layerMaps](#Element_layerMaps), [reorders](#Element_reorders), [transforms](#Element_transforms), [vkeyMaps](#Element_vkeyMaps)
>
> Children: _none_
>
> Occurrence: optional, multiple
>
> </small>

_Attribute:_ `path` (required)

> The value is contains a relative path to the included XML file. There is a standard set of directories to be searched that an application may provide. This set is always prepended with the directory in which the current file being read is stored.

If two identical elements <!-- , as described below, --> are defined, the later element will take precedence.

<!-- TODO: Rework the below discussion. -->

<!-- Thus if a `hardwareMap/map` for the same keycode on the same page is defined twice (for example once in an included file), the later one will be the resulting mapping.

Elements are considered to have three attributes that make them unique: the tag of the element, the parent and the identifying attribute. The parent in its turn is a unique element and so on up the chain. If the distinguishing attribute is optional, its non-existence is represented with an empty value. Here is a list of elements and their defining attributes. If an element is not listed then if it is a leaf element, only one occurs and it is merely replaced. If it has children, then the subelements are considered, in effect merging the element in question.

| Element      | Parent       | Distinguishing attribute     |
|--------------|--------------|------------------------------|
| `import`     | `keyboard`   | `@path`                      |
| `keyMap`     | `keyboard`   | `@modifiers`                 |
| `map`        | `keyMap`     | `@iso`                       |
| `flicks`     | `keyMap`     | `@iso`                       |
| `flick`      | `flicks`     | `@directions`                |
| `display`    | `displayMap` | `@to`                        |
| `layer`      | `keyboard`   | `@modifier`                  |
| `row`        | `layer`      | `@keys`                      |
| `switch`     | `layer`      | `@iso`                       |
| `vkeys`      | `layer`      | `@iso`                       |
| `transforms` | `keyboard`   | `@type`                      |
| `transform`  | `keyboard`   | `@before`, `@from`, `@after` |
| `reorder`    | `reorders`   | `@before`, `@from`, `@after` |
| `backspace`  | `backspaces` | `@before`, `@from`, `@after` |

In order to help identify mistakes, it is an error if a file contains two elements that override each other. All element overrides must come as a result of an `<include>` element either for the element overridden or the element overriding.

The following elements are not imported from the source file:

* `version`
* `generation`
* `names`
* `settings`

-->

* * *

### <a name="Element_displayMap" href="#Element_displayMap">Element: displayMap</a>

The displayMap can be used to describe what is to be displayed on the keytops for various keys. For the most part, such explicit information is unnecessary since the `@to` element from the `keys/key` element can be used. But there are some characters, such as diacritics, that do not display well on their own and so explicit overrides for such characters can help.
Another useful scenario is where there are doubled diacritics, or multiple characters with spacing issues.

The `displayMap` consists of a list of display subelements.

displayMaps are designed to be shared across many different keyboard layout descriptions, and imported with `<import>` in where needed.

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
<displayMap>
    {a set of display elements}
</displayMap>
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
> Parents: [displayMap](#Element_displayMap)
>
> Children: _none_
>
> Occurrence: required, multiple
>
> </small>

_Attribute:_ `to` (optional- `to` or `id` is required, or both)

> Specifies the character or character sequence from the `keys/key` element that is to have a special display.

_Attribute:_ `id` (optional- `to` or `id` is required, or both)

> Specifies the `key` id. This is useful for keys which do not produce any output (no `to=` value), such as a shift key.

_Attribute:_ `display` (required)

> Required and specifies the character sequence that should be displayed on the keytop for any key that generates the `@to` sequence or has the `@id`. (It is an error if the value of the `display` attribute is the same as the value of the `to` attribute, this would be an extraneous entry.)

**Example**

```xml
<keyboard>
    <keys>
        <key id="a" to="a" longpress="\u0301 \u0300" />
        <key id="shift" switch="shift" />
    </keys>
    <displayMap>
        <display to="\u0300" display="ˋ" /> <!-- \u02CB -->
        <display to="\u0301" display="ˊ" /> <!-- \u02CA -->
        <display id="shift"  display="⇪" /> <!-- U+21EA -->
    </displayMap>
</keyboard>
```

To allow `displayMap`s to be shared across keyboards, there is no requirement that `@to` in a `display` element matches any `@to`/`@id` in any `keys/key` element in the keyboard description.

* * *

### <a name="Element_displayOptions" href="#Element_displayOptions">Element: displayOptions</a>

The `displayOptions` is an optional singleton element providing additional settings on this `displayMap`.  It is structured so as to provide for future flexibility in such options.

**Syntax**

```xml
<displayMap>
    <display …/>
    <displayOptions baseCharacter="x"/>
</displayMap>
```

> <small>
>
> Parents: [displayMap](#Element_displayMap)
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

* * *

### <a name="Element_layerMaps" href="#Element_layerMaps">Element: layerMaps</a>

This element represents a set of `layerMap` elements with a specific physical form factor, whether
hardware or touch layout.

> <small>
>
> Parents: [keyboard](#Element_keyboard)
>
> Children: [import](#Element_import), [layerMap](#Element_layerMap), [_special_](tr35.md#special)
>
> Occurrence: required, multiple
>
> </small>

_Attribute:_ `form` (required)

> `form` may be either `hardware` or `touch`.
>
> There may only be a single `layerMap` with `form="hardware"`.
>
> It is recommended to always have one
> `<layerMaps form="hardware">` element.
> If there is no `hardware` form, the implementation may need
> to choose a different keyboard file, or use some other fallback behavior when using a
> hardware keyboard.
>
> When using an on-screen keyboard, if there is not a `<layerMaps form="touch">`
> element, the `<layerMaps form="hardware">` element can be used for on-screen use.

### <a name="Element_layerMap" href="#Element_layerMap">Element: layerMap</a>

A `layerMap` element describes the configuration of keys on a particular layer of a keyboard. It contains one or more `row` elements to describe which keys exist in each row.

**Syntax**

```xml
<layerMap id="layerId" modifier="{Set of Modifier Combinations}">
    ...
</layerMap>
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
> Touch layouts must have one `layerMap` with `id="base"` to serve as the base layer.

_Attribute:_ `modifier` (required for `hardware`)

> This has two roles. It acts as an identifier for the `layer` element for hardware keyboards (in the absence of the id= element) and also provides the linkage from the hardware modifiers into the correct `layer`. To indicate that no modifiers apply, the reserved name of "none" can be used. For the purposes of fallback vkey mapping, the following modifier components are reserved: "shift", "ctrl", "alt", "caps", "cmd", "opt" along with the "L" and "R" optional single suffixes for the first 3 in that list.

For hardware layouts, the use of `@modifier` as an identifier for a layer is sufficient since it is always unique among the set of `layerMap` elements in a keyboard.

* * *

### <a name="Element_row" href="#Element_row">Element: row</a>

A `row` element describes the keys that are present in the row of a keyboard.



**Syntax**

```xml
<row keys="{keyId} {keyId} …" />
```

> <small>
>
> Parents: [layerMap](#Element_layerMap)
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

### <a name="Element_vkeyMaps" href="#Element_vkeyMaps">Element: vkeyMaps</a>

On some architectures, applications may directly interact with keys before they are converted to characters. The keys are identified using a virtual key identifier or vkey. The mapping between a physical keyboard key and a vkey is keyboard-layout dependent. For example, a French keyboard would identify the top-left key (ISO D01) as being an `A` with a vkey of `A` as opposed to `Q` on a US English keyboard. While vkeys are layout dependent, they are not modifier dependent. A shifted key always has the same vkey as its unshifted counterpart. In effect, a key may be identified by its vkey and the modifiers active at the time the key was pressed.

**Syntax**

```xml
<vkeyMaps>
    {a set of vkey elements}
</vkeyMaps>
```

> <small>
>
> Parents: [keyboard](#Element_keyboard)
>
> Children: [import](#Element_import), [_special_](tr35.md#special), [vkeyMap](#Element_vkeyMap)
>
> Occurrence: optional, single
>
> </small>

There is at most a single vkeyMaps element per keyboard.

A `vkeyMaps` element consists of a list of `vkeyMap` elements.

* * *

### <a name="Element_vkeyMap" href="#Element_vkeyMap">Element: vkeyMap</a>

A `vkeyMap` element describes a mapping between a key and a vkey.

**Syntax**

```xml
<vkeyMap from="{from}" to="{to}" />
```

> <small>
>
> Parents: [vkeyMaps](#Element_vkeyMaps)
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
    <vkeyMaps>
		<vkeyMap from="Q" to="A" />
		<vkeyMap from="W" to="Z" />
		<vkeyMap from="A" to="Q" />
		<vkeyMap from="Z" to="W" />
    </vkeyMaps>
</keyboard>
```

* * *

### <a name="Element_transforms" href="#Element_transforms">Element: transforms</a>

This element defines a group of one or more `transform` elements associated with this keyboard layout. This is used to support features such as dead-keys, character reordering, etc. using a straightforward structure that works for all the keyboards tested, and that results in readable source data.

There can be multiple `<transforms>` elements.

Syntax

```xml
<transforms type="...">
    {a set of transform elements}
</transforms>
```

> <small>
>
> Parents: [keyboard](#Element_keyboard)
>
> Children: [import](#Element_import), [_special_](tr35.md#special), [transform](#Element_transform)
>
> Occurrence: optional, multiple
>
> </small>

_Attribute:_ `type` (required)

> Current values: `simple`, `final`.

There are other keying behaviors that are needed particularly in handing complex orthographies from various parts of the world. The behaviors intended to be covered by the transforms are:

* Reordering combining marks. The order required for underlying storage may differ considerably from the desired typing order. In addition, a keyboard may want to allow for different typing orders.
* Error indication. Sometimes a keyboard layout will want to specify to the application that a particular keying sequence in a context is in error and that the application should indicate that that particular keypress is erroneous.
* Backspace handling. There are various approaches to handling the backspace key. An application may treat it as an undo of the last key input, or it may simply delete the last character in the currently output text, or it may use transform rules to tell it how much to delete.

We consider each transform type in turn and consider attributes to the `<transforms>` element pertinent to that type.

* * *

### <a name="Element_transform" href="#Element_transform">Element: transform</a>

This element must have the `transforms` element as its parent. This element represents a single transform that may be performed using the keyboard layout. A transform is an element that specifies a set of conversions from sequences of code points into (one or more) other code points. For example, in most French keyboards hitting the "^" dead-key followed by the "e" key produces "ê".

**Syntax**

```xml
<transform from="{combination of characters}" to="{output}"
   [before="{look-behind required match}"]
   [after="{look-ahead required match}"]
   [error="fail"] />
```

> <small>
>
> Parents: [transforms](#Element_transforms)
> Children: _none_
> Occurrence: required, multiple
>
> </small>

_Attribute:_ `from` (required)

> The `from` attribute consists of a sequence of elements. Each element matches one character and may consist of a codepoint or a UnicodeSet (both as defined in [UTS #35 section 5.3.3](https://www.unicode.org/reports/tr35/#Unicode_Sets)).

For example, suppose there are the following transforms:

```default
^e → ê
^a → â
^o → ô
```

If the user types a key that produces "\^", the keyboard enters a dead state. When the user then types a key that produces an "e", the transform is invoked, and "ê" is output. Suppose a user presses keys producing "\^" then "u". In this case, there is no match for the "\^u", and the "\^" is output if the `transformFailure` attribute in the `settings` element is set to emit. If there is no transform starting with "u", then it is also output (again only if `transformFailure` is set to emit) and the mechanism leaves the "dead" state.

The UI may show an initial sequence of matching characters with a special format, as is done with dead-keys on the Mac, and modify them as the transform completes. This behavior is specified in the `partial` attribute in the `transform` element.

Most transforms in practice have only a couple of characters. But for completeness, the behavior is defined on all strings. The following applies when no exact match exists:

1. If there could be a longer match if the user were to type additional keys, go into a 'dead' state.
2. If there could not be a longer match, find the longest actual match, emit the transformed text (if `transformFailure` is set to emit), and start processing again with the remainder.
3. If there is no possible match, output the first character, and start processing again with the remainder.

Suppose that there are the following transforms:

```default
ab → x
abc → y
abef → z
bc → m
beq → n
```

Here's what happens when the user types various sequence characters:

| Input characters | Result | Comments |
|------------------|--------|----------|
| ab               |        | No output, since there is a longer transform with this as prefix. |
| abc              | y      | Complete transform match. |
| abd              | xd     | The longest match is "ab", so that is converted and output. The 'd' follows, since it is not the start of any transform. |
| abeq             | xeq    | "ab" wins over "beq", since it comes first. That is, there is no longer possible match starting with 'a'. |
| bc               | m      |          |

Control characters, combining marks and whitespace in this attribute are escaped using the `\u{...}` notation.

_Attribute:_ `to` (required)

> This attribute represents the characters that are output from the transform. The output can contain more than one character, so you could have `<transform from="´A" to="Fred"/>`.

Control characters, whitespace (other than the regular space character) and combining marks in this attribute are escaped using the `\u{...}` notation.

Examples

```xml
<keyboard locale="fr-CA-t-k0-CSA-osx">
    <transforms type="simple">
        <transform from="´a" to="á" />
        <transform from="´A" to="Á" />
        <transform from="´e" to="é" />
        <transform from="´E" to="É" />
        <transform from="´i" to="í" />
        <transform from="´I" to="Í" />
        <transform from="´o" to="ó" />
        <transform from="´O" to="Ó" />
        <transform from="´u" to="ú" />
        <transform from="´U" to="Ú" />
    </transforms>
    ...
</keyboard>
```

```xml
<keyboard locale="nl-BE-t-k0-chromeos">
    <transforms type="simple">
        <transform from="\u{30c}a" to="ǎ" /> <!-- ̌a → ǎ -->
        <transform from="\u{30c}A" to="Ǎ" /> <!-- ̌A → Ǎ -->
        <transform from="\u{30a}a" to="å" /> <!-- ̊a → å -->
        <transform from="\u{30a}A" to="Å" /> <!-- ̊A → Å -->
    </transforms>
    ...
</keyboard>
```

_Attribute:_ `before` (optional)

> This attribute consists of a sequence of elements (codepoint or UnicodeSet) to match the text up to the current position in the text (this is similar to a regex "look behind" assertion: `(?<=a)b` matches a "b" that is preceded by an "a"). The attribute must match for the transform to apply. If missing, no before constraint is applied. The attribute value must not be empty.

_Attribute:_ `after` (optional)

> This attribute consists of a sequence of elements (codepoint or UnicodeSet) and matches as a zero-width assertion after the `@from` sequence. The attribute must match for the transform to apply. If missing, no after constraint is applied. The attribute value must not be empty. When the transform is applied, the string matched by the `@from` attribute is replaced by the string in the `@to` attribute, with the text matched by the `@after` attribute left unchanged. After the change, the current position is reset to just after the text output from the `@to` attribute and just before the text matched by the `@after` attribute. Warning: some legacy implementations may not be able to make such an adjustment and will place the current position after the `@after` matched string.

_Attribute:_ `error="fail"` (optional)

> If set this attribute indicates that the keyboarding application may indicate an error to the user in some way. Processing may stop and rewind to any state before the key was pressed. If processing does stop, no further transforms on the same input are applied. The `@error` attribute takes the value `"fail"`, or must be absent. If processing continues, the `@to` is used for output as normal. It thus should contain a reasonable value.

For example:

```xml
<transform from="\u037A\u037A" to="\u037A" error="fail" />
```

This indicates that it is an error to type two iota subscripts immediately after each other.

In terms of how these different attributes work in processing a sequence of transforms, consider the transform:

```xml
<transform before="X" from="Y" after="Z" to="B" />
```

This would transform the string:

```default
XYZ → XBZ
```

If we mark where the current match position is before and after the transform we see:

```default
X | Y Z → X B | Z
```

And a subsequent transform could transform the Z string, looking back (using @before) to match the B.

* * *

### <a name="Element_reorders" href="#Element_reorders">Element: reorders</a>

This element defines a group of one or more `reorder` elements associated with this keyboard layout.

> <small>
>
> Parents: [keyboard](#Element_keyboard)
>
> Children: [import](#Element_import), [reorder](#Element_reorder), [_special_](tr35.md#special)
>
> Occurrence: Optional, multiple
> </small>

* * *

### <a name="Element_reorder" href="#Element_reorder">Element: reorder</a>

The reorder transform is applied after all transforms except for those with `type="final"`.

This transform has the job of reordering sequences of characters that have been typed, from their typed order to the desired output order. The primary concern in this transform is to sort combining marks into their correct relative order after a base, as described in this section. The reorder transforms can be quite complex, keyboard layouts will almost always import them.

The reordering algorithm consists of four parts:

1. Create a sort key for each character in the input string. A sort key has 4 parts (primary, index, tertiary, quaternary):
   * The **primary weight** is the primary order value.
   * The **secondary weight** is the index, a position in the input string, usually of the character itself, but it may be of a character earlier in the string.
   * The **tertiary weight** is a tertiary order value (defaulting to 0).
   * The **quaternary weight** is the index of the character in the string. This is solely to ensure a stable sort for sequences of characters with the same tertiary weight.
2. Mark each character as to whether it is a prebase character, one that is typed before the base and logically stored after. Thus it will have a primary order > 0.
3. Use the sort key and the prebase mark to identify runs. A run starts with a prefix that contains any prebase characters and a single base character whose primary and tertiary key is 0. The run extends until, but not including, the start of the prefix of the next run or end of the string.
   * `run := prebase* (primary=0 && tertiary=0) ((primary≠0 || tertiary≠0) && !prebase)*`
4. Sort the character order of each character in the run based on its sort key.

The primary order of a character with the Unicode property Combining_Character_Class (ccc) of 0 may well not be 0. In addition, a character may receive a different primary order dependent on context. For example, in the Devanagari sequence ka halant ka, the first ka would have a primary order 0 while the halant ka sequence would give both halant and the second ka a primary order > 0, for example 2. Note that “base” character in this discussion is not a Unicode base character. It is instead a character with primary=0.

In order to get the characters into the correct relative order, it is necessary not only to order combining marks relative to the base character, but also to order some combining marks in a subsequence following another combining mark. For example in Devanagari, a nukta may follow a consonant character, but it may also follow a conjunct consisting of consonant, halant, consonant. Notice that the second consonant is not, in this model, the start of a new run because some characters may need to be reordered to before the first base, for example repha. The repha would get primary < 0, and be sorted before the character with order = 0, which is, in the case of Devanagari, the initial consonant of the orthographic syllable.

The reorder transform consists of `<reorder>` elements encapsulated in a `<reorders>` element. Each is a rule that matches against a string of characters with the action of setting the various ordering attributes (`primary`, `tertiary`, `tertiary_base`, `prebase`) for the matched characters in the string.

**Syntax**

```xml
<reorder from="{combination of characters}"
   [before="{look-behind required match}"]
   [after="{look-ahead required match}"]
   [order="{list of weights}"]
   [tertiary="{list of weights}"]
   [tertiary_base="{list of true/false}"]
   [prebase="{list of true/false}"] />
```

> <small>
>
> Parents: [reorders](#Element_reorder)
> Children: _none_
> Occurrence: required, multiple
>
> </small>

_Attribute:_ `from` (required)

> This attribute follows the `transform/@from` attribute and contains a string of elements. Each element matches one character and may consist of a codepoint or a UnicodeSet (both as defined in UTS #35 section 5.3.3).

_Attribute:_ `before`

> This attribute follows the `transform/@before` attribute and contains the element string that must match the string immediately preceding the start of the string that the @from matches.

_Attribute:_ `after`

> This attribute follows the `transform/@after` attribute and contains the element string that must match the string immediately following the end of the string that the `@from` matches.

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

_Attribute:_ `tertiary_base`

> This attribute is a space separated list of `"true"` or `"false"` values corresponding to each character matched. It is illegal for a tertiary character to have a true `tertiary_base` value. For a primary character it marks that this character may have tertiary characters moved after it. When calculating the secondary weight for a tertiary character, the most recently encountered primary character with a true `tertiary_base` attribute is used. Primary characters with an `@order` value of 0 automatically are treated as having `tertiary_base` true regardless of what is specified for them.

_Attribute:_ `prebase`

> This attribute gives the prebase attribute for each character matched. The value may be `"true"` or `"false"` or a space separated list of such values. If missing the value for all the characters matched is false. It is illegal for a tertiary character to have a true prebase value.
>
> If a primary character has a true prebase value then the character is marked as being typed before the base character of a run, even though it is intended to be stored after it. The primary order gives the intended position in the order after the base character, that the prebase character will end up. Thus `@order` shall not be 0. These characters are part of the run prefix. If such characters are typed then, in order to give the run a base character after which characters can be sorted, an appropriate base character, such as a dotted circle, is inserted into the output run, until a real base character has been typed. A value of `"false"` indicates that the character is not a prebase.

_Note:_ Unlike on the similar `transform` and `backspace` elements, there is no `@error` attribute.

For `@from` attributes with a match string length greater than 1, the sort key information (`@order`, `@tertiary`, `@tertiary_base`, `@prebase`) may consist of a space separated list of values, one for each element matched. The last value is repeated to fill out any missing values. Such a list may not contain more values than there are elements in the `@from` attribute:

```java
if len(@from) < len(@list) then error
else
    while len(@from) > len(@list)
        append lastitem(@list) to @list
    endwhile
endif
```

**Example**

For example, consider the Northern Thai (nod-Lana) word: ᨡ᩠ᩅᩫ᩶ 'roasted'. This is ideally encoded as the following:

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

`<reorder>` elements are priority ordered based first on the length of string their `@from` attribute matches and then the sum of the lengths of the strings their `@before` and `@after` attributes match.

If a layout has two `<reorders>` elements, e.g. from importing one and specifying the second, then `<reorder>` elements are merged. The @from string in a `<reorder>` element describes a set of strings that it matches. This also holds for the `@before` and `@after` attributes. The intersection of two `<reorder>` elements consists of the intersections of their `@from`, `@before` and `@after` string sets. It is illegal for the intersection between any two `<reorder>` elements in the same `<reorders>` element to be non empty, although implementors are encouraged to have pity on layout authors when reporting such errors, since they can be hard to track down.

If two `<reorder>` elements in two different `<reorders>` elements have a non empty intersection, then they are split and merged. They are split such that where there were two `<reorder>` elements, there are, in effect (but not actuality), three elements consisting of:

* `@from`, `@before`, `@after` that match the intersection of the two rules. The other attributes are merged, as described below.
* `@from`, `@before`, `@after` that match the set of strings in the first rule not in the intersection with the other attributes from the first rule.
* `@from`, `@before`, `@after` that match the set of strings in the second rule not in the intersection, with the other attributes from the second rule.

When merging the other attributes, the second rule is taken to have priority (occurring later in the layout description file). Where the second rule does not define the value for a character but the first does, it is taken from the first rule, otherwise it is taken from the second rule.

Notice that it is possible for two rules to match the same string, but for them not to merge because the distribution of the string across `@before`, `@from`, and `@after` is different. For example:

```xml
<reorder before="ab" from="cd" after="e" />
```

would not merge with:

```xml
<reorder before="a" from="bcd" after="e" />
```

When two `<reorders>` elements merge as the result of an import, the resulting `reorder` elements are sorted into priority order for matching.

Consider this fragment from a shared reordering for the Myanmar script:

```xml
<!-- medial-r -->
<reorder from="\u103C" order="20" />

<!-- [medial-wa or shan-medial-wa] -->
<reorder from="[\u103D\u1082]" order="25" />

<!-- [medial-ha or shan-medial-wa]+asat = Mon asat -->
<reorder from="[\u103E\u1082]\u103A" order="27" />

<!-- [medial-ha or mon-medial-wa] -->
<reorder from="[\u103E\u1060]" order="27" />

<!-- [e-vowel or shan-e-vowel] -->
<reorder from="[\u1031\u1084]" order="30" />

<reorder from="[\u102D\u102E\u1033-\u1035\u1071-\u1074\u1085\u109D\uA9E5]" order="35" />
```

A particular Myanmar keyboard layout can have this `reorders` element:

```xml
<reorders>
    <!-- Kinzi -->
    <reorder from="\u1004\u103A\u1039" order="-1" />

    <!-- e-vowel -->
    <reorder from="\u1031" prebase="1" />

    <!-- medial-r -->
    <reorder from="\u103C" prebase="1" />
</reorders>
```

The effect of this is that the _e-vowel_ will be identified as a prebase and will have an order of 30. Likewise a _medial-r_ will be identified as a prebase and will have an order of 20. Notice that a _shan-e-vowel_ will not be identified as a prebase (even if it should be!). The _kinzi_ is described in the layout since it moves something across a run boundary. By separating such movements (prebase or moving to in front of a base) from the shared ordering rules, the shared ordering rules become a self-contained combining order description that can be used in other keyboards or even in other contexts than keyboarding.

* * *

### <a name="Element_final" href="#Element_final">Element: transform final</a>

The final transform is applied after the reorder transform. It executes in a similar way to the simple transform with the settings ignored, as if there were no settings in the `<settings>` element.

**Example**

This is an example from Khmer where split vowels are combined after reordering.

```xml
<transforms type="final">
    <transform from="\u17C1\u17B8" to="\u17BE" />
    <transform from="\u17C1\u17B6" to="\u17C4" />
</transforms>
```

Another example allows a keyboard implementation to alert or stop people typing two lower vowels in a Burmese cluster:

```xml
<transform from="[\u102F\u1030\u1048\u1059][\u102F\u1030\u1048\u1059]" error="fail" />
```

* * *

### <a name="Element_backspaces" href="#Element_backspaces">Element: backspaces</a>

The backspace transform is an optional transform that is not applied on input of normal characters, but is only used to perform extra backspace modifications to previously committed text.

Keyboarding applications typically work, but are not required to, in one of two modes:

**_text entry_**

> text entry happens while a user is typing new text. A user typically wants the backspace key to undo whatever they last typed, whether or not they typed things in the 'right' order.

**_text editing_**

> text editing happens when a user moves the cursor into some previously entered text which may have been entered by someone else. As such, there is no way to know in which order things were typed, but a user will still want appropriate behaviour when they press backspace. This may involve deleting more than one character or replacing a sequence of characters with a different sequence.

In the text entry mode, there is no need for any special description of backspace behaviour. A keyboarding application will typically keep a history of previous output states and just revert to the previous state when backspace is hit.

In text editing mode, different keyboard layouts may behave differently in the same textual context. The backspace transform allows the keyboard layout to specify the effect of pressing backspace in a particular textual context. This is done by specifying a set of backspace rules that match a string before the cursor and replace it with another string. The rules are expressed as `backspace` elements encapsulated in a `backspaces` element.

**Syntax**

```xml
<backspaces>
    {a set of backspace elements}
</backspace>
```

> <small>
>
> Parents: [keyboard](#Element_keyboard)
>
> Children: [backspace](#Element_backspace), [import](#Element_import), [_special_](tr35.md#special)
>
> Occurrence: optional, single
>
> </small>

* * *

### <a name="Element_backspace" href="#Element_backspace">Element: backspace</a>

**Syntax**

```xml
<backspace from="{combination of characters}" [to="{output}"]
   [before="{look-behind required match}"]
   [after="{look-ahead required match}"]
   [error="fail"] />
```

> <small>
>
> Parents: [backspaces](#Element_backspaces)
>
> Children: _none_
>
> Occurrence: required, multiple
>
> </small>

The `backspace` element has the same `@before`, `@from`, `@after`, `@to`, `@error` of the `transform` element. The `@to` is optional with `backspace`.

**Example**

For example, consider deleting a Devanagari ksha:

```xml
<backspaces>
    <backspace from="\u0915\u094D\u0936"/>
</backspaces>
```

Here there is no `@to` attribute since the whole string is being deleted. This is not uncommon in the backspace transforms.

A more complex example comes from a Burmese visually ordered keyboard:

```xml
<backspaces>
    <!-- Kinzi -->
    <backspace from="[\u1004\u101B\u105A]\u103A\u1039" />

    <!-- subjoined consonant -->
    <backspace from="\u1039[\u1000-\u101C\u101E\u1020\u1021\u1050\u1051\u105A-\u105D]" />

    <!-- tone mark -->
    <backspace from="\u102B\u103A" />

    <!-- Handle prebases -->
    <!-- diacritics stored before e-vowel -->
    <backspace from="[\u103A-\u103F\u105E-\u1060\u1082]\u1031" to="\u1031" />

    <!-- diacritics stored before medial r -->
    <backspace from="[\u103A-\u103B\u105E-\u105F]\u103C" to="\u103C" />

    <!-- subjoined consonant before e-vowel -->
    <backspace from="\u1039[\u1000-\u101C\u101E\u1020\u1021]\u1031" to="\u1031" />

    <!-- base consonant before e-vowel -->
    <backspace from="[\u1000-\u102A\u103F-\u1049\u104E]\u1031" to="\uFDDF\u1031" />

    <!-- subjoined consonant before medial r -->
    <backspace from="\u1039[\u1000-\u101C\u101E\u1020\u1021]\u103C" to="\u103C" />

    <!-- base consonant before medial r -->
    <backspace from="[\u1000-\u102A\u103F-\u1049\u104E]\u103C" to="\uFDDF\u103C" />

    <!-- delete lone medial r or e-vowel -->
    <backspace from="\uFDDF[\u1031\u103C]" />
</backspaces>
```

The above example is simplified, and doesn't fully handle the interaction between medial-r and e-vowel.

The character \\uFDDF does not represent a literal character, but is instead a special placeholder, a "filler string". When a keyboard implementation handles a user pressing a key that inserts a prebase character, it also has to insert a special filler string before the prebase to ensure that the prebase character does not combine with the previous cluster. See the reorder transform for details. The precise filler string is implementation dependent. Rather than requiring keyboard layout designers to know what the filler string is, we reserve a special character that the keyboard layout designer may use to reference this filler string. It is up to the keyboard implementation to, in effect, replace that character with the filler string.

The first three transforms above delete various ligatures with a single keypress. The other transforms handle prebase characters. There are two in this Burmese keyboard. The transforms delete the characters preceding the prebase character up to base which gets replaced with the prebase filler string, which represents a null base. Finally the prebase filler string + prebase is deleted as a unit.

The backspace transform is much like other transforms except in its processing model. If we consider the same transform as in the simple transform example, but as a backspace:

```xml
<backspace before="X" from="Y" after="Z" to="B"/>
```

This would transform the string:

```default
XYZ → XBZ
```

If we mark where the current match position is before and after the transform we see:

```default
X Y | Z → X B | Z
```

Whereas a simple or final transform would then run other transforms in the transform list, advancing the processing position until it gets to the end of the string, the backspace transform only matches a single backspace rule and then finishes.

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
2. Use the minimal language id based on `likelySubtag`s.
   1. Eg, instead of `en-US`, use `en`, and instead of `fr-Latn-FR` use `fr`. Because there is `<likelySubtag from="en" to="en_Latn_US"/>`, en-US → en.
   2. The data is in <https://github.com/unicode-org/cldr/tree/main/common/supplemental/likelySubtags.xml>
3. Keyboard files should be platform-independent, however, a platform id is the first subtag after `-t-k0-` if present. If a keyboard on the platform changes over time, both are dated, eg `bg-t-k0-chromeos-2011`. When selecting, if there is no date, it means the latest one.
4. Keyboards are only tagged that differ from the "standard for each language". That is, for each language on a platform, there will be a keyboard with no subtags. Subtags with common semantics across languages and platforms are used, such as `-extended`, `-phonetic`, `-qwerty`, `-qwertz`, `-azerty`, …
5. In order to get to 8 letters, abbreviations are reused that are already in [bcp47](https://github.com/unicode-org/cldr/tree/main/common/bcp47/) -u/-t extensions and in [language-subtag-registry](https://www.iana.org/assignments/language-subtag-registry) variants, eg for Traditional use `-trad` or `-traditio` (both exist in [bcp47](https://github.com/unicode-org/cldr/tree/main/common/bcp47/)).
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

In the following chart, “CLDR Name” indicates the value used with the `from` and `to` attributes of the [vkeyMap](#Element_vkeyMap) element.

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
| LESS-THAN | B00            | 0xE2            | 102nd key on European layouts, right of left shift. |
| ABNT2     | B11            | -               | Extra key, left of right-shift |

Footnotes:

* <sup>1</sup> Hex value from Windows, web standards, Keyman, etc.

* * *

Copyright © 2001–2022 Unicode, Inc. All Rights Reserved. The Unicode Consortium makes no expressed or implied warranty of any kind, and assumes no liability for errors or omissions. No liability is assumed for incidental and consequential damages in connection with or arising out of the use of the information or programs contained or accompanying this technical report. The Unicode [Terms of Use](https://unicode.org/copyright.html) apply.

Unicode and the Unicode logo are trademarks of Unicode, Inc., and are registered in some jurisdictions.

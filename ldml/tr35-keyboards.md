## Unicode Technical Standard #35 Tech Preview

# Unicode Locale Data Markup Language (LDML)<br/>Part 7: Keyboards

|Version|45 (draft)   |
|-------|-------------|
|Editors|Steven Loomis (<a href="mailto:srloomis@unicode.org">srloomis@unicode.org</a>) and <a href="tr35.md#Acknowledgments">other CLDR committee members</a>|

For the full header, summary, and status, see [Part 1: Core](tr35.md).

### _Summary_

This document describes parts of an XML format (_vocabulary_) for the exchange of structured locale data. This format is used in the [Unicode Common Locale Data Repository](https://www.unicode.org/cldr/).

This is a partial document, describing keyboards. For the other parts of the LDML see the [main LDML document](tr35.md) and the links above.

_Note:_
Some links may lead to in-development or older
versions of the data files.
See <https://cldr.unicode.org> for up-to-date CLDR release data.

### _Status_

_This is a draft document which may be updated, replaced, or superseded by other documents at any time.
Publication does not imply endorsement by the Unicode Consortium.
This is not a stable document; it is inappropriate to cite this document as other than a work in progress._

<!-- _This document has been reviewed by Unicode members and other interested parties, and has been approved for publication by the Unicode Consortium.
This is a stable document and may be used as reference material or cited as a normative reference by other specifications._ -->

> _**A Unicode Technical Standard (UTS)** is an independent specification. Conformance to the Unicode Standard does not imply conformance to any UTS._

_Please submit corrigenda and other comments with the CLDR bug reporting form [[Bugs](tr35.md#Bugs)]. Related information that is useful in understanding this document is found in the [References](tr35.md#References). For the latest version of the Unicode Standard see [[Unicode](tr35.md#Unicode)]. For a list of current Unicode Technical Reports see [[Reports](tr35.md#Reports)]. For more information about versions of the Unicode Standard, see [[Versions](tr35.md#Versions)]._


See also [Compatibility Notice](#compatibility-notice).

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
*   Part 9: [MessageFormat](tr35-messageFormat.md#Contents) (message format)

## <a name="Contents" href="#Contents">Contents of Part 7, Keyboards</a>

* [Keyboards](#keyboards)
* [Goals and Non-goals](#goals-and-non-goals)
  * [Compatibility Notice](#compatibility-notice)
  * [Accessibility](#accessibility)
* [Definitions](#definitions)
* [Notation](#notation)
  * [Escaping](#escaping)
  * [UnicodeSet Escaping](#unicodeset-escaping)
  * [UTS18 Escaping](#uts18-escaping)
* [File and Directory Structure](#file-and-directory-structure)
  * [Extensibility](#extensibility)
* [Normalization](#normalization)
  * [Where Normalization Occurs](#where-normalization-occurs)
  * [Normalization and Transform Matching](#normalization-and-transform-matching)
  * [Normalization and Markers](#normalization-and-markers)
    * [Rationale for 'gluing' markers](#rationale-for-gluing-markers)
    * [Data Model: `Marker`](#data-model-marker)
    * [Data Model: string](#data-model-string)
    * [Data Model: `MarkerEntry`](#data-model-markerentry)
    * [Marker Algorithm Overview](#marker-algorithm-overview)
    * [Phase 1: Parsing/Removing Markers](#phase-1-parsingremoving-markers)
    * [Phase 2: Plain Text Processing](#phase-2-plain-text-processing)
    * [Phase 3: Adding Markers](#phase-3-adding-markers)
    * [Example Normalization with Markers](#example-normalization-with-markers)
  * [Normalization and Character Classes](#normalization-and-character-classes)
  * [Normalization and Reorder elements](#normalization-and-reorder-elements)
  * [Normalization-safe Segments](#normalization-safe-segments)
  * [Normalization and Output](#normalization-and-output)
  * [Disabling Normalization](#disabling-normalization)
* [Element Hierarchy](#element-hierarchy)
  * [Element: keyboard3](#element-keyboard3)
  * [Element: locales](#element-locales)
  * [Element: locale](#element-locale)
  * [Element: version](#element-version)
  * [Element: info](#element-info)
  * [Element: settings](#element-settings)
  * [Element: keys](#element-keys)
  * [Element: key](#element-key)
    * [Implied Keys](#implied-keys)
    * [Element: flicks](#element-flicks)
    * [Element: flick](#element-flick)
    * [Element: flickSegment](#element-flicksegment)
  * [Element: import](#element-import)
  * [Element: displays](#element-displays)
  * [Element: display](#element-display)
    * [Non-spacing marks on keytops](#non-spacing-marks-on-keytops)
  * [Element: displayOptions](#element-displayoptions)
  * [Element: forms](#element-forms)
  * [Element: form](#element-form)
    * [Implied Form Values](#implied-form-values)
  * [Element: scanCodes](#element-scancodes)
  * [Element: layers](#element-layers)
  * [Element: layer](#element-layer)
    * [Layer Modifier Sets](#layer-modifier-sets)
    * [Layer Modifier Components](#layer-modifier-components)
    * [Modifier Left- and Right- keys](#modifier-left--and-right--keys)
    * [Layer Modifier Matching](#layer-modifier-matching)
  * [Element: row](#element-row)
  * [Element: variables](#element-variables)
  * [Element: string](#element-string)
  * [Element: set](#element-set)
  * [Element: uset](#element-uset)
  * [Element: transforms](#element-transforms)
    * [Markers](#markers)
  * [Element: transformGroup](#element-transformgroup)
    * [Example: `transformGroup` with `transform` elements](#example-transformgroup-with-transform-elements)
    * [Example: `transformGroup` with `reorder` elements](#example-transformgroup-with-reorder-elements)
  * [Element: transform](#element-transform)
    * [Regex-like Syntax](#regex-like-syntax)
    * [Additional Features](#additional-features)
    * [Disallowed Regex Features](#disallowed-regex-features)
    * [Replacement syntax](#replacement-syntax)
  * [Element: reorder](#element-reorder)
    * [Using `<import>` with `<reorder>` elements](#using-import-with-reorder-elements)
    * [Example Post-reorder transforms](#example-post-reorder-transforms)
    * [Reorder and Markers](#reorder-and-markers)
  * [Backspace Transforms](#backspace-transforms)
* [Invariants](#invariants)
* [Keyboard IDs](#keyboard-ids)
  * [Principles for Keyboard IDs](#principles-for-keyboard-ids)
* [Platform Behaviors in Edge Cases](#platform-behaviors-in-edge-cases)
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

## Keyboards

The Unicode Standard and related technologies such as CLDR have dramatically improved the path to language support. However, keyboard support remains platform and vendor specific, causing inconsistencies in implementation as well as timeline.

More and more language communities are determining that digitization is vital to their approach to language preservation and that engagement with Unicode is essential to becoming fully digitized. For many of these communities, however, getting new characters or a new script added to The Unicode Standard is not the end of their journey. The next, often more challenging stage is to get device makers, operating systems, apps and services to implement the script requirements that Unicode has just added to support their language.

However, commensurate improvements to streamline new language support on the input side have been lacking. CLDR’s Keyboard specification has been updated in an attempt to address this gap.

This document specifies an interchange format for the communication of keyboard mapping data independent of vendors and platforms. Keyboard authors can then create a single mapping file for their language, which implementations can use to provide that language’s keyboard mapping on their own platform.

Additionally, the standardized identifier for keyboards can be used to communicate, internally or externally, a request for a particular keyboard mapping that is to be used to transform either text or keystrokes. The corresponding data can then be used to perform the requested actions.  For example, a remote screen-access application (such as used for customer service or server management) would be able to communicate and choose the same keyboard layout on the remote device as is used in front of the user, even if the two systems used different platforms.

The data can also be used in analysis of the capabilities of different keyboards. It also allows better interoperability by making it easier for keyboard designers to see which characters are generally supported on keyboards for given languages.

<!-- To illustrate this specification, here is an abridged layout representing the English US 101 keyboard on the macOS operating system (with an inserted long-press example). -->

For complete examples, see the XML files in the CLDR source repository.

Attribute values should be evaluated considering the DTD and [DTD Annotations](tr35.md#dtd-annotations).

* * *

## Goals and Non-goals

Some goals of this format are:

1. Physical and virtual keyboard layouts defined in a single file.
2. Provide definitive platform-independent definitions for new keyboard layouts.
    * For example, a new French standard keyboard layout would have a single definition which would be usable across all implementations.
3. Allow platforms to be able to use CLDR keyboard data for the character-emitting keys (non-frame) aspects of keyboard layouts.
4. Deprecate & archive existing LDML platform-specific layouts so they are not part of future releases.

<!--
1. Make the XML as readable as possible.
2. Represent faithfully keyboard data from major platforms: it should be possible to create a functionally-equivalent data file (such that given any input, it can produce the same output).
3. Make as much commonality in the data across platforms as possible to make comparison easy. -->

Some non-goals (outside the scope of the format) currently are:

1. Adaptation for screen scaling resolution. Instead, keyboards should define layouts based on physical size. Platforms may interpret physical size definitions and adapt for different physical screen sizes with different resolutions.
2. Unification of platform-specific virtual key and scan code mapping tables.
3. Unification of pre-existing platform layouts themselves (e.g. existing fr-azerty on platform a, b, c).
4. Support for prior (pre 3.0) CLDR keyboard files. See [Compatibility Notice](#compatibility-notice).
5. Run-time efficiency. [LDML is explicitly an interchange format](tr35.md#Introduction), and so it is expected that data will be transformed to a more compact format for use by a keystroke processing engine.
6. Platform-specific frame keys such as Fn, Numpad, IME swap keys, and cursor keys are out of scope.
   (This also means that in this specification, modifier (frame) keys cannot generate output, such as capslock producing backslash.)

<!-- 1. Display names or symbols for keycaps (eg, the German name for "Return"). If that were added to LDML, it would be in a different structure, outside the scope of this section.
2. Advanced IME features, handwriting recognition, etc.
3. Roundtrip mappings—the ability to recover precisely the same format as an original platform's representation. In particular, the internal structure may have no relation to the internal structure of external keyboard source data, the only goal is functional equivalence. -->

<!-- Note: During development of this section, it was considered whether the modifier RAlt (= AltGr) should be merged with Option. In the end, they were kept separate, but for comparison across platforms implementers may choose to unify them. -->

Note that in parts of this document, the format `@x` is used to indicate the _attribute_ **x**.

### Compatibility Notice

> A major rewrite of this specification, called "Keyboard 3.0", was introduced in CLDR v45.
> The changes required were too extensive to maintain compatibility. For this reason, the `ldmlKeyboard3.dtd` DTD is _not_ compatible with DTDs from prior versions of CLDR such as v43 and prior.
>
> To process earlier XML files, use the data and specification from v43.1, found at <https://www.unicode.org/reports/tr35/tr35-69/tr35.html>
>
> `ldmlKeyboard.dtd` continues to be made available in CLDR, however, it will not be updated.

### Accessibility

Keyboard use can be challenging for individuals with various types of disabilities. For this revision, features or architectural designs specifically for the purpose of improving accessibility are not yet included. However:

1. Having an industry-wide standard format for keyboards will enable accessibility software to make use of keyboard data with a reduced dependence on platform-specific knowledge.
2. Features which require certain levels of mobility or speed of entry should be considered for their impact on accessibility. This impact could be mitigated by means of additional, accessible methods of generating the same output.
3. Public feedback is welcome on any aspects of this document which might hinder accessibility.

## Definitions

**Arrangement:** The relative position of the rectangles that represent keys, either physically or virtually. A hardware keyboard has a static arrangement while a touch keyboard may have a dynamic arrangement that changes per language and/or layer. While the arrangement of keys on a keyboard may be fixed, the mapping of those keys may vary.

**Base character:** The character emitted by a particular key when no modifiers are active. In ISO 9995-1:2009 terms, this is Group 1, Level 1.

**Core keys:** also known as “alphanumeric” section. The primary set of key values on a keyboard that are used for typing the target language of the keyboard. For example, the three rows of letters on a standard US QWERTY keyboard (QWERTYUIOP, ASDFGHJKL, ZXCVBNM) together with the most significant punctuation keys. Usually this equates to the minimal set of keys for a language as seen on mobile phone keyboards.
Distinguished from the **frame keys**.

**Dead keys:** These are keys which do not emit normal characters by themselves. They are so named because to the user, they may appear to be “dead,” i.e., non-functional. However, they do produce a change to the input context. For example, in many Latin keyboards hitting the `^` dead-key followed by the `e` key produces `ê`. The `^` by itself may be invisible or presented in a special way by the platform.

**Frame keys:** These are keys which are outside of the area of the **core keys** and typically do not emit characters. These keys include **modifier** keys, such as Shift or Ctrl, but also include platform specific keys: Fn, IME and layout-switching keys, cursor keys, insert emoji keys etc.

**Hardware keyboard:** an input device which has individual keys that are pressed. Each key has a unique identifier and the arrangement doesn't change, even if the mapping of those keys does. Also known as a physical keyboard.

**Implementation:** see **Keyboard implementation**

**Input Method Editor (IME):** a component or program that supports input of large character sets. Typically, IMEs employ contextual logic and candidate UI to identify the Unicode characters intended by the user.

**Keyboard implementation:** Software which implements the present specification, such that keyboard XML files can be used to interpret keystrokes from a **Hardware keyboard** or an on-screen **Touch keyboard**.

Keyboard implementations will typically consist of two parts:

1. A _compile/build tool_ part used by **Keyboard authors** to parse the XML file and produce a compact runtime format, and
2. A _runtime_ part which interprets the runtime format when the keyboard is selected by the end user, and delivers the output plain text to the platform or application.

**Key:** A physical key on a hardware keyboard, or a virtual key on a touch keyboard.

**Key code:** The integer code sent to the application on pressing a key.

**Key map:** The basic mapping between hardware or on-screen positions and the output characters for each set of modifier combinations associated with a particular layout. There may be multiple key maps for each layout.

**Keyboard:** A particular arrangement of keys for the inputting of text, such as a hardware keyboard or a touch keyboard.

**Keyboard author:** The person or group of people designing and producing a particular keyboard layout designed to support one or more languages. In the context of this specification, that author may be editing the LDML XML file directly or by means of software tools.

**Keyboard layout:** A layout is the overall keyboard configuration for a particular locale. Within a keyboard layout, there is a single base map, one or more key maps and zero or more transforms.

**Layer** is an arrangement of keys on a touch keyboard. A touch keyboard is made up of a set of layers. Each layer may have a different key layout, unlike with a hardware keyboard, and may not correspond directly to a hardware keyboard's modifier keys. A layer is accessed via a switch key. See also touch keyboard, modifier, switch.

**Long-press key:** also known as a “child key”. A secondary key that is invoked from a top level key on a touch keyboard. Secondary keys typically provide access to variants of the top level key, such as accented variants (a => á, à, ä, ã)

**Modifier:** A key that is held to change the behavior of a hardware keyboard. For example, the "Shift" key allows access to upper-case characters on a US keyboard. Other modifier keys include but are not limited to: Ctrl, Alt, Option, Command and Caps Lock. On a touch keyboard, keys that appear to be modifier keys should be considered to be layer-switching keys.

**Physical keyboard:** see **Hardware keyboard**

**Touch keyboard:** A keyboard that is rendered on a, typically, touch surface. It has a dynamic arrangement and contrasts with a hardware keyboard. This term has many synonyms: software keyboard, SIP (Software Input Panel), virtual keyboard. This contrasts with other uses of the term virtual keyboard as an on-screen keyboard for reference or accessibility data entry.

**Transform:** A transform is an element that specifies a set of conversions from sequences of code points into one (or more) other code points. Transforms may reorder or replace text. They may be used to implement “dead key” behaviors, simple orthographic corrections, visual (typewriter) type input etc.

**Virtual keyboard:** see **Touch keyboard**

## Notation

- Ellipses (`…`) in syntax examples are used to denote substituted parts.

  For example, `id="…keyId"` denotes that `…keyId` (the part between double quotes) is to be replaced with something, in this case a key identifier. As another example, `\u{…usv}` denotes that the `…usv` is to be replaced with something, in this case a Unicode scalar value in hex.

### Escaping

When explicitly specified, attribute values can contain escaped characters. This specification uses two methods of escaping, the _UnicodeSet_ notation and the `\u{…usv}` notation.

### UnicodeSet Escaping

The _UnicodeSet_ notation is described in [UTS #35 section 5.3.3](tr35.md#Unicode_Sets) and allows for comprehensive character matching, including by character range, properties, names, or codepoints.

Note that the `\u1234` and `\x{C1}` format escaping is not supported, only the `\u{…}` format (using `bracketedHex`).

Currently, the following attribute values allow _UnicodeSet_ notation:

* `from` or `before` on the `<transform>` element
* `from` or `before` on the `<reorder>` element
* `chars` on the [`<repertoire>`](#test-element-repertoire) test element.

### UTS18 Escaping

The `\u{…usv}` notation, a subset of hex notation, is described in [UTS #18 section 1.1](https://www.unicode.org/reports/tr18/#Hex_notation). It can refer to one or multiple individual codepoints. Currently, the following attribute values allow the `\u{…}` notation:

* `output` on the `<key>` element
* `from` or `to` on the `<transform>` element
* `value` on the `<variable>` element
* `output` and `display` on the `<display>` element
* `baseCharacter` on the `<displayOptions>` element
* Some attributes on [Keyboard Test Data](#keyboard-test-data) subelements

Characters of general category of Mark (M), Control characters (Cc), Format characters (Cf), and whitespace other than space should be encoded using one of the notation above as appropriate.

Attribute values escaped in this manner are annotated with the `<!--@ALLOWS_UESC-->` DTD annotation, see [DTD Annotations](tr35.md#dtd-annotations)

* * *

## File and Directory Structure

* In the future, new layouts will be included in the CLDR repository, as a way for new layouts to be distributed in a cross-platorm manner. The process for this repository of layouts has not yet been defined, see the [CLDR Keyboard Workgroup Page][keyboard-workgroup] for up-to-date information.

* Layouts have version metadata to indicate their specification compliance versi​​on number, such as `45`. See [`cldrVersion`](tr35-info.md#version-information).

```xml
<keyboard3 xmlns="https://schemas.unicode.org/cldr/45/keyboard3" conformsTo="45"/>
```

> _Note_: Unlike other LDML files, layouts are designed to be used outside of the CLDR source tree.  As such, they do not contain DOCTYPE entries.
>
> DTD and Schema (.xsd) files are available for use in validating keyboard files.

* The filename of a keyboard .xml file does not have to match the BCP47 primary locale ID, but it is recommended to do so. The CLDR repository may enforce filename consistency.

### Extensibility

For extensibility, the `<special>` element will be allowed at nearly every level.

See [Element special](tr35.md#special) in Part 1.

## Normalization

Unicode Normalization, as described in [The Unicode Standard](https://www.unicode.org/reports/tr41/#Unicode/), is a process by which Unicode text is processed to eliminate unwanted distinctions.

This section discusses how conformant keyboards are affected by normalization, and the impact of normalization on keyboard authors and keyboard implmentations.

Keyboard implementations will usually apply normalization as appropriate when matching transform rules and `<display>` value matching.
Output from the keyboard, following application of all transform rules, will be normalized to the appropriate form by the keyboard implementation.

> Note: There are many existing software libraries which perform Unicode Normalization, including [ICU](https://icu.unicode.org), [ICU4X](https://icu4x.unicode.org), and JavaScript's [String.prototype.normalize()](https://developer.mozilla.org/docs/Web/JavaScript/Reference/Global_Objects/String/normalize).

Keyboard authors will not typically need to perform normalization as part of the keyboard layout.  However, authors should be aware of areas where normalization affects keyboard operation so that they may achieve their desired results.

### Where Normalization Occurs

There are four stages where normalization must be performed by keyboard implementations.

1. **From the keyboard source `.xml`**

    Keyboard source .xml files may be in any normalization form.
    However, in processing they are converted to NFD.

    - From any form to NFD: full normalization (decompose+reorder)
    - Markers must be processed as described [below](#marker-algorithm-overview).
    - Regex patterns must be processed so that matching is performed in NFD.

    Example: `<key output=`, and `<transform from= to=` attribute contents will be normalized to NFD.

2. **From the input context**

    The input context must be normalized for purposes of matching.

    - From any form to NFD: full normalization (decompose+reorder)
    - Markers in the cached context must be preserved.

    Example: The input context contains U+00E8 (`è`).  The user clicks the cursor after the character, then presses a key which produces U+0320 (`<key output="\u{0320}"/>`).
    The implementation must normalize the context buffer to `e\u{0320}\u{0300}` (`è̠`) before matching.

3. **Before each `transformGroup`**

    Text must be normalized before processing by the next `transformGroup`.

    - To NFD: no decomposition should be needed, because all of the input text (including transform rules) was already in NFD form.
    However, marker reordering may be needed if transforms insert segments out of order.
    - Markers must be preserved.

    Example: The input context contains U+00E8 (`è`).  The user clicks the cursor after this character, then presses a key producing `x`. A transform rule `<transform from='x' to='\u{0320}'/>` matches. The implementation must normalize the intermediate buffer to `e\u{0320}\u{0300}` (`è̠`) before proceeding to the next `transformGroup`.

4. **Before output to the platform/application**

    Text must be normalized into the output form requested by the platform or application. This will typically be NFC, but may not be.

    - If normalizing to NFC, full normalization (reorder+composition) will be required.
    - No markers are present in this text, they are removed prior to output but retained in the implementation's input context for subsequent keystrokes. See [markers](#markers).

    Example: The result of keystrokes and transform processing produces the string `e\u{0300}`. The keyboard implementation normalizes this to a single NFC codepoint U+00E8 (`è`), which is returned to the application.

### Normalization and Transform Matching

Regardless of the normalization form in the keyboard source file or in the edit buffer context, transform matching will be performed using **NFD**. For example, all of the following transforms will match the input strings è̠, whether the input is U+00E8 U+0320, U+0065 U+0320 U+0300, or U+0065 U+0300 U+0320.

```xml
<transform from="e\u{0320}\u{0300}" /> <!-- NFD -->
<transform from="\u{00E8}\u{0320}"  /> <!-- NFC: è + U+0320 -->
<transform from="e\u{0300}\u{0320}" /> <!-- Unnormalized -->
```

### Normalization and Markers

A special issue occurs when markers are involved.
[Markers](#markers) are not text, and so not themselves modified or reordered by the Unicode Normalization Algorithm.
Existing Normalization APIs typically operate on plain text, and so those APIs can not be used with content containing markers.

However, the markers must be retained and processed by keyboard implementations in a manner which will be both consistent across implementations and predictable to keyboard authors.
Inconsistencies would result in different user experiences — specifically, different or incorrect text output — on some implementations and not another.
Unpredictability would make it challenging for the keyboard author to create a keyboard with expected behavior.

This section gives an algorithm for implementing normalization on a text stream including markers.

_Note:_ When the algorithm is performed on a plain text stream that doesn't include markers, implementations may skip the removing/re-adding steps 1 and 3 because no markers are involved.

#### Rationale for 'gluing' markers

The processing described here describes an extension to Unicode normalization to account for the desired behavior of markers.

The algorithm described considers markers 'glued' (remaining with) the following character. If a context ends with a marker, that marker would be guaranteed to remain at the end after processing, consistently located with respect to the next keystroke to be input.

1. Keyboard authors can keep a marker together with a character of interest by emitting the marker just previous to that character.

For example, given a key `output="\m{marker}X"`, the marker will proceed `X` regardless of any normalization. (If `output="X\m{marker}"` were used, and `X` were to reorder with other characters, the marker would no longer be adjacent to the X.)

2. Markers which are at the end of the input remain at the end of input during normalization.

For example, given input context which ends with a marker, such as `...ABCDX\m{marker}`, the marker will remain at the end of the input context regardless of any normalization.

The 'gluing' is only applicable during one particular processing step. It does not persist or affect further processing steps or future keystrokes.

#### Data Model: `Marker`

For purposes of this algorithm, a `Marker` is an opaque data type which has one property, its ID. See [Markers](#markers) for a discussion of the marker ID.

#### Data Model: string

For purposes of this algorithm, a string is an array of elements, where each element is either a codepoint or a `Marker`. For example, a [`key`](#element-key) in the XML such as `<key id="sha" output="𐓯\m{mymarker}x" />` would produce a string with three elements:

1. The codepoint U+104EF
2. The `Marker` named `mymarker`
3. The codepoint U+0078

If this string were output to an application, it would be converted to _plain text_ by removing all markers, which would yield the plain text string with only two codepoints: `𐓯x`.

#### Data Model: `MarkerEntry`

This algorithm uses a temporary data structure which is an ordered array of `MarkerEntry` elements.

Each `MarkerEntry` element has the following properties:
- `glue` (a codepoint, or the special value `END_OF_SEGMENT`)
- `divider?` (true/false)
- `processed?` (true/false, defaults to false)
- `marker` (the `Marker` object)

#### Marker Algorithm Overview

This algorithm has three main phases to it.

1. **Parsing/Removing Markers**

    In this phase, the input string is analyzed to locate all markers. Metadata about each marker is stored in a temporary `MarkerArray` data structure.
    Markers are removed from the input string, leaving only plain text.

2. **Plain Text Processing**

    This phase is performed on the plain text string, such as NFD normalization.

3. **Re-Adding Markers**

    Finally, markers are re-added to the plain text string using the `MarkerEntry` metadata from step 1.
    This phase results in a string which contains both codepoints and markers.

#### Phase 1: Parsing/Removing Markers

Given an input string _s_

1. Initialize an empty `MarkerEntry` array _e_
2. Initialize an empty `Marker` array _pending_
2. Loop through each element _i_ of the input _s_
    1. If _i_ is a `Marker`:
        1. add the marker _i_ to the end of _pending_
        2. remove the marker from the input string _s_
    2. else if _i_ is a codepoint:
        1. Decompose _i_ into NFD form into a plain text string array of codepoints _d_
        2. Add an element with `glue=d[0]` (the first codepoint of _d_) and `divider? = true` to the end of _e_
        3. For every marker _m_ in _pending_:
            1. Add an element with `glue=d[0]` and `marker=m` and `divider? = false` to the end of _e_
        4. Clear the _pending_ array.
        5. Finally, for every codepoint _c_ in _d_ **following** the initial codepoint: (d[1]..):
            1. Add an element with `glue=c` and `divider? = true` to the end of _e_
3. At the end of text,
    1. Add an element with `glue=END` and `divider?=true` to the end of _e_
    2. For every marker _m_ in _pending_:
        1. Add an element with `glue=END` and `marker=m` and `divider? = false` to the end of _e_

The string _s_ is now plain text and can be processed by the next phase.

The array _e_ will be used in Phase 3.

#### Phase 2: Plain Text Processing

See [UAX #15](https://www.unicode.org/reports/tr15/#Description_Norm) for an overview of the process.  An existing Unicode-compliant API can be used here.

#### Phase 3: Adding Markers

1. Initialize an empty output string _o_
2. Loop through the elements _p_ of the array _e_ from end to beginning (backwards)
    1. If _p_.glue isn't `END`:
        1. break out of the loop
    2. If _p_.divider? == false:
        1. Prepend marker _p_.marker to the output string _o_
    3. Set _p_.processed?=true (so we don't process this again)
2. Loop through each codepoint _i_ ( in the plain text input string ) from end to beginning (backwards)
    1. Prepend _i_ to output _o_
    2. Loop through the elements _p_ of the array _e_ from end to beginning (backwards)
        1. If _p_.processed? == true:
            1. Continue the inner loop  (was already processed)
        2. If _p_.glue isn't _i_
            1. Continue the inner loop  (wrong glue, not applicable)
        3. If _p_.divider? == true:
            1. Break out of the inner loop  (reached end of this 'glue' char)
        4. Prepend marker _p_.marker to the output string _o_
        5. Set _p_.processed?=true (so we don't process this again)
3. _o_ is now the output string including markers.

#### Example Normalization with Markers

**Example 1**

Consider this example, without markers:

- `e\u{0300}\u{0320}` (input)
- `e\u{0320}\u{0300}` (NFD)

The combining marks are reordered.

**Example 2**

If we add markers:

- `e\u{0300}\m{marker}\u{0320}` (input)
- `e\m{marker}\u{0320}\u{0300}` (NFD)

Note that the marker is 'glued' to the _following_ character. In the above example, `\m{marker}` was 'glued' to the `\u{0320}`.

**Example 2**

A second example:

- `e\m{marker0}\u{0300}\m{marker1}\u{0320}\m{marker2}` (input)
- `e\m{marker1}\u{0320}\m{marker0}\u{0300}\m{marker2}` (NFD)

Here `\m{marker2}` is 'glued' to the end of the string. However, if additional text is added such as by a subsequent keystroke (which may add an additional combining character, for example), this marker may be 'glued' to that following text.

Markers remain in the same normalization-safe segment during normalization. Consider:

**Example 3**

- `e\u{0300}\m{marker1}\u{0320}a\u{0300}\m{marker2}\u{0320}` (original)
- `e\m{marker1}\u{0320}\u{0300}a\m{marker2}\u{0320}\u{0300}` (NFD)

There are two normalization-safe segments here:

1. `e\u{0300}\m{marker1}\u{0320}`
2. `a\u{0300}\m{marker2}\u{0320}`

Normalization (and marker rearranging) effectively occurs within each segment.  While `\m{marker1}` is 'glued' to the `\u{0320}`, it is glued within the first segment and has no effect on the second segment.

### Normalization and Character Classes

If pre-composed (non-NFD) characters are used in [character classes](#regex-like-syntax), such as `[á-é]`, these may not match as keyboard authors expect, as the U+00E1 character (á) will not occur in NFD form. Thus this may be masking serious errors in the data.

Tools that process keyboard data must reject the data when character classes include non-NFD characters.

The above should be written instead as a regex `(á|â|ã|ä|å|æ|ç|è|é)`. Alternatively, it could be written as a set variable `<set id="Example" value="á â ã ä å æ ç è é"/>` and matched as `$[Example]`.

There is another case where there is no explicit mention of a non-NFD character, but the character class could include non-NFD characters, such as the range `[\u{0020}-\u{01FF}]`. For these, the tools should raise a warning by default.

### Normalization and Reorder elements

[`reorder`](#element-reorder) elements operate on NFD codepoints.

### Normalization-safe Segments

For purposes of this algorithm, "normalization-safe segments" are defined as a string of codepoints which are

1. already in [NFD](https://www.unicode.org/reports/tr15/#Norm_Forms), and
2. begin with a character with [Canonical Combining Class](https://www.unicode.org/reports/tr44/#Canonical_Combining_Class_Values) of `0`.

See [UAX #15 Section 9.1: Stable Code Points](https://www.unicode.org/reports/tr15/#Stable_Code_Points) for related discussion.
Text under consideration can be segmented by locating such characters.

### Normalization and Output

On output, text will be normalized into a specified normalization form. That form will typically be NFC, but an implementation may allow a calling application to override the choice of normalization form.
For example, many platforms may request NFC as the output format. In such a case, all text emitted via the keyboard will be transformed into NFC.

Existing text in a document will only have normalization applied within a single normalization-safe segment from the caret.  The output will not contain any markers, thus any normalization is unaffected by any markers embedded within the segment.

For example, the sequence `e\m{marker}\u{300}` would be output in NFC as `è`. The marker is removed and has no effect on the output.

### Disabling Normalization

The attribute value `normalization="disabled"` can be used to indicate that no automatic normalization is to be applied in input, matching, or output. Using this setting should be done with caution:

- When this attribute value is used, all matching and output uses only the exact codepoints provided by the keyboard author.
- The input context from the application may not be normalized, which means that the keyboard author should consider all possible combinations, including NFC, NFD, and mixed normalization in `<transform from=` attributes.
- See [`<settings>`](#element-settings) for further details.

The majority of the above section only applies when `normalization="disabled"` is not used.

* * *

## Element Hierarchy

This section describes the XML elements in a keyboard layout file, beginning with the top level element `<keyboard3>`.

### Element: keyboard3

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
> Children: [displays](#element-displays), [import](#element-import), [info](#element-info), [keys](#element-keys), [flicks](#element-flicks), [layers](#element-layers), [locales](#element-locales), [settings](#element-settings), [_special_](tr35.md#special), [transforms](#element-transforms), [variables](#element-variables), [version](#element-version)
>
> Occurrence: required, single
>
> </small>

_Attribute:_ `conformsTo` (required)

This attribute value distinguishes the keyboard from prior versions,
and it also specifies the minimum CLDR major version required.

This attribute value must be a whole number of `45` or greater. See [`cldrVersion`](tr35-info.md#version-information)

```xml
<keyboard3 … conformsTo="45"/>
```

_Attribute:_ `locale` (required)

This attribute value contains the primary locale of the keyboard using BCP 47 [Unicode locale identifiers](tr35.md#Canonical_Unicode_Locale_Identifiers) - for example `"el"` for Greek. Sometimes, the locale may not specify the base language. For example, a Devanagari keyboard for many languages could be specified by BCP-47 code: `"und-Deva"`. However, it is better to list out the languages explicitly using the [`locales`](#element-locales) element.

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

* * *

### Element: locales

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
> Parents: [keyboard3](#element-keyboard3)
>
> Children: [locale](#element-locale)
>
> Occurrence: optional, single
>
> </small>

### Element: locale

The optional `<locales>` element allows specifying additional or alternate locales. Denotes intentional support for an extra language, not just that a keyboard incidentally supports a language’s orthography.

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

### Element: version

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

### Element: info

Element containing informative properties about the layout, for displaying in user interfaces etc.

**Syntax**

```xml
<info
      name="…name"
      author="…author"
      layout="…hint of the layout"
      indicator="…short identifier" />
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

_Attribute:_ `layout`

> The `layout` attribute describes the layout pattern, such as QWERTY, DVORAK, INSCRIPT, etc. typically used to distinguish various layouts for the same language.
>
> This attribute is not localized, but is an informative identifier for implementation use.

_Attribute:_ `indicator`

> The `indicator` attribute describes a short string to be used in currently selected layout indicator, such as `US`, `SI9` etc.
> Typically, this is shown on a UI element that allows switching keyboard layouts and/or input languages.
>
> This attribute is not localized.

* * *

### Element: settings

An element used to keep track of layout-specific settings by implementations. This element may or may not show up on a layout. These settings reflect the normal practice by the implementation. However, an implementation using the data may customize the behavior.

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

### Element: keys

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

### Element: key

This element defines a mapping between an abstract key and its output. This element must have the `keys` element as its parent. The `key` element is referenced by the `keys=` attribute of the [`row` element](#element-row).

**Syntax**

```xml
<key
 id="…keyId"
 flickId="…flickId"
 gap="true"
 longPressKeyIds="…list of keyIds"
 longPressDefaultKeyId="…keyId"
 multiTapKeyIds="…listId"
 stretch="true"
 layerId="…layerId"
 output="…string"
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

```xml
<key id="mediumgap" gap="true" width="1.5"/>
```

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
> <displays>
>    <displays output="\m{marker}" display="{" />
> </displays>
>
> <keys>
>    <key id="o" output="o" longPressKeyIds="o-acute marker" longPressDefaultKeyId="marker">
>    <key id="o-acute" output="ó"/>
>    <key id="marker" display="{"/>
> </key>
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
> <keys>
>    <key id="a" output="a" multiTapKeyIds="bb c d">
>    <key id="bb" output="bb" />
>    <key id="c" output="c" />
>    <key id="d" output="d" />
> </key>
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


_Attribute:_ `output`

> The `output` attribute value contains the sequence of characters that is emitted when pressing this particular key. Control characters, whitespace (other than the regular space character) and combining marks in this attribute are escaped using the `\u{…}` notation. More than one key may output the same output.
>
> The `output` attribute may also contain the `\m{…markerId}` syntax to insert a marker. See the definition of [markers](#markers).

_Attribute:_ `width="1.2"` (optional, default "1.0")

> The `width` attribute indicates that this key has a different width than other keys, by the specified number of key widths.

```xml
<key id="wide-a" output="a" width="1.2"/>
<key id="wide-gap" gap="true" width="2.5"/>
```

##### Implied Keys

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

#### Element: flicks

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

#### Element: flick

The `flick` element is used to generate results from a "flick" of the finger on a mobile device.

**Syntax**

```xml
<keyboard3>
    <keys>
        <key id="a" flicks="a-flicks" output="a" />
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
> The `flick` elements do not share a namespace with the `key`s, so it would also be allowed
> to have `<key id="a" flick="a"/>`
>
> In the future, this attribute’s definition is expected to be updated to align with [UAX#31](https://www.unicode.org/reports/tr31/).

* * *

#### Element: flickSegment

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

### Element: import

The `import` element is used to reference another xml file so that elements are imported from
another file. The use case is to be able to import a standard set of `transform`s and similar
from the CLDR repository, especially to be able to share common information relevant to a particular script.
The intent is for each single XML file to contain all that is needed for a keyboard layout, other than required standard import data from the CLDR repository.

`<import>` can be used as a child of a number of elements (see the _Parents_ section immediately below). Multiple `<import>` elements may be used, however, `<import>` elements must come before any other sibling elements.
If two identical elements are defined, the later element will take precedence, that is, override.

**Note:** imported files do not have any indication of their normalization mode. For this reason, the keyboard author must verify that the imported file is of a compatible normalization mode. See the [`settings` element](#element-settings) for further details.

**Syntax**
```xml
<import base="cldr" path="45/keys-Zyyy-punctuation.xml"/>
```
> <small>
>
> Parents: [displays](#element-displays), [keyboard3](#element-keyboard3), [keys](#element-keys), [flicks](#element-flicks), [layers](#element-layers), [transformGroup](#element-transformgroup), [transforms](#element-transforms), [variables](#element-variables)
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
<!DOCTYPE transforms SYSTEM "../dtd/ldmlKeyboard3.dtd">
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

### Element: displays

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

### Element: display

The `display` elements can be used to describe what is to be displayed on the keytops for various keys. For the most part, such explicit information is unnecessary since the `@to` element from the `keys/key` element will be used for keytop display.

- Some characters, such as diacritics, do not display well on their own.
- Another useful scenario is where there are doubled diacritics, or multiple characters with spacing issues.
- Finally, the `display` element provides a way to specify the keytop for keys which do not otherwise produce output. Keys which switch layers using the `@layerId` attribute typically do not produce output.

> Note: `displays` elements are designed to be shared across many different keyboard layout descriptions, and imported with `<import>` where needed.

#### Non-spacing marks on keytops

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

**Note**: There is currently no way to indicate a custom display for a key without output (i.e. without a `to=` attribute), nor is there a way to indicate that such a key has a standardized identity (e.g. that a key should be identified as a “Shift”). These may be addressed in future versions of this standard.


_Attribute:_ `output` (optional)

> Specifies the character or character sequence from the `keys/key` element that is to have a special display.
> This attribute may be escaped with `\u` notation, see [Escaping](#escaping).
> The `output` attribute may also contain the `\m{…}` syntax to reference a marker. See [Markers](#markers). Implementations may highlight a displayed marker, such as with a lighter text color, or a yellow highlight.
> String variables may be substituted. See [String variables](#element-string)

_Attribute:_ `id` (optional)

> Specifies the `key` id. This is useful for keys which do not produce any output (no `output=` value), such as a shift key.
>
> This attribute must match `[A-Za-z0-9][A-Za-z0-9-]*`

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

To allow `displays` elements to be shared across keyboards, there is no requirement that `@output` in a `display` element matches any `@output`/`@id` in any `keys/key` element in the keyboard description.

* * *

### Element: displayOptions

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

### Element: forms

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

### Element: form

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

***Syntax***

```xml
<form id="us">
    <scanCodes codes="00 01 02"/>
    <scanCodes codes="03 04 05"/>
</form>
```

##### Implied Form Values

There is an implied set of `<form>` elements corresponding to the default forms, thus implementations must behave as if there was the following import statement:

```xml
<keyboard3>
    <forms>
        <import base="cldr" path="45/scanCodes-implied.xml" /> <!-- the version will match the current conformsTo of the file -->
    </forms>
</keyboard3>
```

Here is a summary of the implied form elements. Keyboards included in the CLDR Repository must only use these `form=` values and may not override the scanCodes.

> - `touch` - Touch (non-hardware) layout.
> - `abnt2` - Brazilian 103 key ABNT2 layout (iso + extra key near right shift)
> - `iso` - European 102 key layout (extra key near left shift)
> - `jis` - Japanese 109 key layout
> - `us` - ANSI 101 key layout
> - `ks` - Korean KS layout

* * *

### Element: scanCodes

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

### Element: layers

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

_Attribute:_ `form` (required)

> This attribute specifies the physical layout of a hardware keyboard,
> or that the form is a `touch` layout.
>
> When using an on-screen touch keyboard, if the keyboard does not specify a `<layers form="touch">`
> element, a `<layers form="…formId">` element can be used as an fallback alternative.
> If there is no `hardware` form, the implementation may need
> to choose a different keyboard file, or use some other fallback behavior when using a
> hardware keyboard.
>
> Because a hardware keyboard facilitates non-trivial amounts of text input,
> and many touch devices can also be connected to a hardware keyboard, it
> is recommended to always have at least one hardware (non-touch) form.
>
> Multiple `<layers form="touch">` elements are allowed with distinct `minDeviceWidth` values.
> At most one hardware (non-`touch`) `<layers>` element is allowed. If a different key arrangement is desired between, for example, `us` and `iso` formats, these should be separated into two different keyboards.
>
> The typical keyboard author will be designing a keyboard based on their circumstances and the hardware that they are using. So, for example, if they are in South East Asia, they will almost certainly be using an 101 key hardware keyboard with US key caps. So we want them to be able to reference that (`<layers form="us">`) in their design, rather than having to work with an unfamiliar form.
>
> A mismatch between the hardware layout in the keyboard file, and the actual hardware used by the user could result in some keys being inaccessible to the user if their hardware cannot generate the scancodes corresponding to the layout specified by the `form=` attribute. Such keys could be accessed only via an on-screen keyboard utility. Conversely, a user with hardware keys that are not present in the specified `form=` will result in some hardware keys which have no function when pressed.
>
> The value of the `form=` attribute may be `touch`, or correspond to a `form` element. See [`form`](#element-form).
>

_Attribute:_ `minDeviceWidth`

> This attribute specifies the minimum required width, in millimeters (mm), of the touch surface.  The `layers` entry with the greatest matching width will be selected. This attribute is intended for `form="touch"`, but is supported for hardware forms.
>
> This must be a whole number between 1 and 999, inclusive.

### Element: layer

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

> The `id` attribute identifies the layer for touch layouts.  This identifier specifies the layout as the target for layer switching, as specified by the `switch=` attribute on the [`<key>`](#element-key) element.
> Touch layouts must have one `layer` with `id="base"` to serve as the base layer.
>
> Must match `[A-Za-z0-9][A-Za-z0-9-]*`

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

#### Layer Modifier Sets

The `@modifiers` attribute value contains one or more Layer Modifier Sets, separated by commas.
For example, in the element `<layer … modifiers="ctrlL altL, altR" …` the attribute value consists of two sets:

- `ctrlL altL` (two components)
- `altR` (one component)

The order of the sets and the order of the components within each set is not significant. However, for clarity in reading, the canonical order within a set is in the order listed in Layout Modifier Components; the canonical order for the sets should be first by the cardinality of the sets (least first), then alphabetical.

#### Layer Modifier Components

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

#### Modifier Left- and Right- keys

1. `L` or `R` indicates a left- or right- side modifier only (such as `altL`)
 whereas `alt` indicates _either_ left or right alt key (that is, `altL` or `altR`). `ctrl` indicates either left or right ctrl key (that is, `ctrlL` or `ctrlR`).

2. Keyboard implementations must warn if a keyboard mixes `alt` with `altL`/`altR`, or `ctrl` with `ctrlL`/`ctrlR`.

3. Left- and right- side modifiers may not be mixed together in a single `modifier` attribute value, so neither `altL ctrlR"` nor `altL altR` are allowed.

4. `shift` indicates either shift key. The left and right shift keys are not distinguishable in this specification.

#### Layer Modifier Matching

Layers are matched exactly based on the modifier keys which are down. For example:

- `none` as a modifier will only match if *all* of the keys `caps`, `alt`, `ctrl` and `shift` are up.

- `alt` as a modifier will only match if either `alt` is down, *and* `caps`, `ctrl`, and `shift` are up.

- `altL ctrl` as a modifier will only match if the left `alt` is down, either `ctrl` is down, *and* `shift` and `caps` are up.

- `other` as a modifier will match if no other layers match.

Multiple modifier sets are separated by commas.  For example, `none, shift caps` will match either no modifiers *or* shift and caps.  `ctrlL altL, altR` will match either  left-control and left-alt, *or* right-alt.

Keystrokes must be ignored where there isn’t a layer that explicitly matches nor a layer with `other`. Example: If there is a `ctrl` and `shift` layer, but no `ctrl shift` nor `other` layer, no output will result from `ctrl shift X`.

Layers are not allowed to overlap in their matching.  For example, the keyboard author will receive an error if one layer specifies `alt shift` and another layer specifies `altR shift`.

There is one special case:  the `other` layer matches if and only if no other layer matches. Thus logically the `other` layer is matched after all other layers have been checked.

Because there is no overlap allowed between layers, the order of `<layer>` elements is not significant.

> Note: The modifier syntax may be enhanced in the future, but will remain backwards compatible with the syntax described here.

* * *

### Element: row

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

### Element: variables

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

### Element: string

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
    <string id="acute" value="\m{acute}"/> <!-- refer to a marker -->
    <string id="backquote" value="`"/>
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
<key id="hi_key" output="${cluster_hi}" />
<key id="acute_key" output="${acute}" />
…
<!-- Display ´ instead of the non-displayable marker -->
<display output="${acute}" display="${backquote}" />
```

* * *

### Element: set

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

### Element: uset

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

### Element: transforms

This element defines a group of one or more `transform` elements associated with this keyboard layout. This is used to support features such as dead-keys, character reordering, backspace behavior, etc. using a straightforward structure that works for all the keyboards tested, and that results in readable source data.

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

#### Markers

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

Pressing `caret` `e` will result in `^e` (with an invisible _no_transform_ marker — note that any name could be used). The `^e` won’t have the transform applied, at least while the marker’s context remains valid.

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

In this way, only the `X`, `e` keys will produce `^e` with a _transform_ marker (again, any name could be used) which will cause the transform to be applied. One benefit is that navigating to an existing `^` in a document and adding an `e` will result in `^e`, and this output will not be affected by the transform, because there will be no marker present there (remember that markers are not stored with the document but only recorded in memory temporarily during text input).

Please note important considerations for [Normalization and Markers](#normalization-and-markers).

**Effect of markers on final text**

All markers must be removed before text is returned to the application from the input context.
If the input context changes, such as if the cursor or mouse moves the insertion point somewhere else, all markers in the input context are removed.

**Implementation Notes**

Ideally, markers are implemented entirely out-of-band from the normal text stream. However, implementations _may_ choose to map each marker to a [Unicode private-use character](https://www.unicode.org/glossary/#private_use_character) for use only within the implementation’s processing and temporary storage in the input context.

For example, the first marker encountered could be represented as U+E000, the second by U+E001 and so on.  If a regex processing engine were used, then those PUA characters could be processed through the existing regex processing engine.  `[^\u{E000}-\u{E009}]` could be used as an expression to match a character that is not a marker, and `[Ee]\u{E000}` could match `E` or `e` followed by the first marker.

Such implementations must take care to remove all such markers (see prior section) from the resultant text. As well, implementations must take care to avoid conflicts if applications themselves are using PUA characters, such as is often done with not-yet-encoded scripts or characters.

* * *

### Element: transformGroup

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


Each `transformGroup` element, after imports are processed, must have either [reorder](#element-reorder) elements or [transform](#element-transform) elements, but not both. The `<transformGroup>` element may not be empty.

**Examples**


#### Example: `transformGroup` with `transform` elements

This is a `transformGroup` that consists of one or more [`transform`](#element-transform) elements, prefaced by one or more `import` elements. See the discussion of those elements for details. `import` elements in this group may not import `reorder` elements.


```xml
<transformGroup>
    <import path="…"/> <!-- optional import elements-->
    <transform />
    <!-- other <transform/> elements -->
</transformGroup>
```


#### Example: `transformGroup` with `reorder` elements

This is a `transformGroup` that consists of one or more [`transform`](#element-transform) elements, optionally prefaced by one or more `import` elements that import `transform` elements. See the discussion of those elements for details.

`import` elements in this group may not import `transform` elements.

```xml
<transformGroup>
    <import path="…"/> <!-- optional import elements-->
    <reorder … />
    <!-- other <reorder> elements -->
</transformGroup>
```

* * *

### Element: transform

This element contains a single transform that may be performed using the keyboard layout. A transform is an element that specifies a set of conversions from sequences of code points into (one or more) other code points. For example, in most French keyboards hitting the `^` dead-key followed by the `e` key produces `ê`.

Matches are processed against the "input context", a temporary buffer containing all relevant text up to the insertion point. If the user moves the insertion point, the input context is discarded and recreated from the application’s text buffer.  Implementations may discard the input context at any time.

The input context may contain, besides regular text, any [Markers](#markers) as a result of keys or transforms, since the insertion point was moved.

Using regular expression terminology, matches are done as if there was an implicit `$` (match end of buffer) at the end of each pattern. In other words, `<transform from="ke" …>` will not match an input context ending with `…keyboard`, but it will match the last two codepoints of an input context ending with `…awake`.

All of the `transform` elements in a `transformGroup` are tested for a match, in order, until a match is found. Then, the matching element is processed, and then processing proceeds to the **next** `transformGroup`. If none of the `transform` elements match, processing proceeds without modification to the buffer to the **next** `transformGroup`.

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

#### Regex-like Syntax

- **Simple matches**

    `abc` `𐒵`

- **Unicode codepoint escapes**

    `\u{1234} \u{012A}`
    `\u{22} \u{012a} \u{1234A}`

    The hex escaping is case insensitive. The value may not match a surrogate or illegal character, nor a marker character.
    The form `\u{…}` is preferred as it is the same regardless of codepoint length.

- **Fixed character classes and escapes**

    `\s \S \t \r \n \f \v \\ \$ \d \w \D \W \0`

    The value of these classes do not change with Unicode versions.

    `\s` for example is exactly `[\f\n\r\t\v\u{00a0}\u{1680}\u{2000}-\u{200a}\u{2028}\u{2029}\u{202f}\u{205f}\u{3000}\u{feff}]`

    `\\` and `\$` evaluate to `\` and `$`, respectively.

- **Character classes**

    `[abc]` `[^def]` `[a-z]` `[ॲऄ-आइ-ऋ]` `[\u{093F}-\u{0944}\u{0962}\u{0963}]`

    - supported
    - no Unicode properties such as `\p{…}`
    - Warning: Character classes look superficially similar to [`uset`](#element-uset) elements, but they are distinct and referenced with the `$[...usetId]` notation in transforms. The `uset` notation cannot be embedded directly in a transform.

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

- **`set` or `uset` variables**

    `$[upper]`

    Given a space-separated `set` or `uset` variable, this syntax will match _any_ of the substrings. This expression may be thought of  (and implemented) as if it were a _non-capturing group_. It may, however, be enclosed within a capturing group. For example, the following definition of `$[upper]` will match as if it were written `(?:A|B|CC|D|E|FF)`.

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
    <uset id="Mn" value="[\u{034F}\u{0591}-\u{05AF}\u{05BD}\u{05C4}\u{05C5}\…]" /> <!-- 1,985 code points -->
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

> This attribute value represents the characters that are output from the transform.
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

    `$[1:variable]` (Where "1" is any numbered capture group from 1 to 9)

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

### Element: reorder

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

For `@from` attribute values with a match string length greater than 1, the sort key information (`@order`, `@tertiary`, `@tertiaryBase`, `@preBase`) may consist of a space-separated list of values, one for each element matched. The last value is repeated to fill out any missing values. Such a list may not contain more values than there are elements in the `@from` attribute:

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

The first reorder is the default ordering for the _sakot_ which allows for it to be placed anywhere in a sequence, but moves any non-consonants that may immediately follow it, back before it in the sequence. The next two rules give the orders for the top vowel component and tone marks respectively. The next three rules give the _sakot_ and _wa_ characters a primary order that places them before the _o_. Notice particularly the final reorder rule where the _sakot_+_wa_ is split by the tone mark. This rule is necessary in case someone types into the middle of previously normalized text.

`<reorder>` elements are priority ordered based first on the length of string their `@from` attribute value matches and then the sum of the lengths of the strings their `@before` attribute value matches.

#### Using `<import>` with `<reorder>` elements

This section describes the impact of using [`import`](#element-import) elements with `<reorder>` elements.

The @from string in a `<reorder>` element describes a set of strings that it matches. This also holds for the `@before` attribute. The **intersection** of any two `<reorder>` elements consists of the intersections of their `@from` and `@before` string sets. Tooling should warn users if the intersection between any two `<reorder>` elements in the same `<transformGroup>` element to be non empty prior to processing imports.

If two `<reorder>` elements have a non empty intersection, then they are split and merged. They are split such that where there were two `<reorder>` elements, there are, in effect (but not actuality), three elements consisting of:

* `@from`, `@before` that match the intersection of the two rules. The other attribute values are merged, as described below.
* `@from`, `@before` that match the set of strings in the first rule not in the intersection with the other attribute values from the first rule.
* `@from`, `@before` that match the set of strings in the second rule not in the intersection, with the other attribute values from the second rule.

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

The effect of this is that the _e-vowel_ will be identified as a prebase and will have an order of 30. Likewise a _medial-r_ will be identified as a prebase and will have an order of 20. Notice that a _shan-e-vowel_ (`\u{1084}`) will not be identified as a prebase (even if it should be!). The _kinzi_ is described in the layout since it moves something across a run boundary. By separating such movements (prebase or moving to in front of a base) from the shared ordering rules, the shared ordering rules become a self-contained combining order description that can be used in other keyboards or even in other contexts than keyboarding.

#### Example Post-reorder transforms

It may be desired to perform additional processing following reorder operations.  This may be aaccomplished by adding an additional `<transformGroup>` element after the group containing `<reorder>` elements.

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

#### Reorder and Markers

Markers are not matched by `reorder` elements. However, if a character preceded by one or more markers is reordered due to a `reorder` element, those markers will be reordered with the characters, maintaining the same relative order.  This is a similar process to the algorithm used to normalize strings processed by `transform` elements.

Keyboard implementations must process `reorder` elements using the following algorithm.

Note that steps 1 and 3 are identical to the steps used for normalization using markers in the [Marker Algorithm Overview](#marker-algorithm-overview).

Given an input string from context or from a previous `transformGroup`:

1. Parsing/Removing Markers

2. Perform reordering (as in this section)

3. Re-Adding Markers

* * *

### Backspace Transforms

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

## Invariants

Beyond what the DTD imposes, certain other restrictions on the data are imposed on the data.
Please note the constraints given under each element section above.
DTD validation alone is not sufficient to verify a keyboard file.

* * *

## Keyboard IDs

There is a set of subtags that help identify the keyboards. Each of these are used after the `"t-k0"` subtags to help identify the keyboards. The first tag appended is a mandatory platform tag followed by zero or more tags that help differentiate the keyboard from others with the same locale code.

### Principles for Keyboard IDs

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
   2. The [`<locales>`](#element-locales) element may be used to identify additional languages.
7. In some cases, there are multiple subtags, like `en-US-t-k0-chromeos-intl-altgr.xml`
8. Otherwise, platform names are used as a guide.

**Examples**

```xml
<!-- Serbian Latin -->
<keyboard3 locale="sr-Latn"/>
```

```xml
<!-- Serbian Cyrillic -->
<keyboard3 locale="sr-Cyrl"/>
```

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

```xml
<!-- Finnish Keyboard including Skolt Sami -->
<keyboard3 locale="fi-t-k0-smi">
    <locales>
    <locale id="sms"/>
    </locales>
</keyboard3>
```

* * *

## Platform Behaviors in Edge Cases

| Platform | No modifier combination match is available | No map match is available for key position | Transform fails (i.e. if \^d is pressed when that transform does not exist) |
|----------|--------------------------------------------|--------------------------------------------|---------------------------------------------------------------------------|
| Chrome OS | Fall back to base | Fall back to character in a keyMap with same "level" of modifier combination. If this character does not exist, fall back to (n-1) level. (This is handled data-generation-side.) <br/> In the specification: No output | No output at all |
| Mac OS X  | Fall back to base (unless combination is some sort of keyboard shortcut, e.g. cmd-c) | No output | Both keys are output separately |
| Windows  | No output | No output | Both keys are output separately |

* * *

## Keyboard Test Data

> **NOTE**: The Keyboard Test Data format is a technical preview, it is subject to revision in future versions of CLDR.

Keyboard Test Data allows the keyboard author to provide regression test data to validate the repertoire and behavior of a keyboard. Tooling can run these regression tests against an implementation, and can also be used as part of the development cycle to validate that keyboard changes do not deviate from expected behavior.  In the interest of complete coverage, tooling could also indicate whether all keys and gestures in a layout are exercised by the test data.

Test data files have a separate DTD, named `ldmlKeyboardTest3.dtd`.  Note that multiple test data files can refer to the same keyboard. Test files should be named similarly to the keyboards which they test, such as `fr_test.xml` to test `fr.xml`.

Sample test data files are located in the `keyboards/test` subdirectory.

The following describes the structure of a keyboard test file.

### Test Doctype

> **NOTE**: The Keyboard Test Data format is a technical preview, it is subject to revision in future versions of CLDR.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE keyboardTest3 SYSTEM "../dtd/ldmlKeyboardTest3.dtd">
```

The top level element is named `keyboardTest`.

### Test Element: keyboardTest

> **NOTE**: The Keyboard Test Data format is a technical preview, it is subject to revision in future versions of CLDR.

> <small>
>
> Children: [info](#test-element-info), [repertoire](#test-element-repertoire), [_special_](tr35.md#special), [tests](#test-element-tests)
> </small>

This is the top level element.

_Attribute:_ `conformsTo` (required)

The `conformsTo` attribute value here is a fixed value of `techpreview`, because the test data is a Technical Preview.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE keyboardTest3 SYSTEM "../dtd/ldmlKeyboardTest3.dtd">
<keyboardTest3 conformsTo="techpreview">
    …
</keyboardTest3>
```

### Test Element: info

> **NOTE**: The Keyboard Test Data format is a technical preview, it is subject to revision in future versions of CLDR.

> <small>
>
> Parents: [keyboardTest](#test-element-keyboardtest)
>>
> Occurrence: Required, Single
> </small>

_Attribute:_ `author`

This freeform attribute value allows for description of the author or authors of this test file.

_Attribute:_ `keyboard` (required)

This attribute value specifies the keyboard’s file name, such as `fr-t-k0-azerty.xml`.

_Attribute:_ `name` (required)

This attribute value specifies a name for this overall test file. These names could be output to the user during test execution, used to summarize success and failure, or used to select or deselect test components.

**Example**

```xml
<info keyboard="fr-t-k0-azerty.xml" author="Team Keyboard" name="fr-test" />
```

### Test Element: repertoire

> **NOTE**: The Keyboard Test Data format is a technical preview, it is subject to revision in future versions of CLDR.

> <small>
>
> Parents: [keyboardTest](#test-element-keyboardtest)
>
> Children: _none_
>
> Occurrence: Optional, Multiple
> </small>

This element contains a repertoire test, to validate the available characters and their reachability. This test ensures that each of the specified characters is somehow typeable on the keyboard, after transforms have been applied. The characters in the repertoire will be matched against the complete set of possible generated outputs, post-transform, of all keys on the keyboard.

_Attribute:_ `name` (required)

This attribute value specifies a unique name for this repertoire test. These names could be output to the user during test execution, used to summarize success and failure, or used to select or deselect test components.

_Attribute:_ `type`

This attribute value is one of the following:

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

This attribute value specifies a list of characters in UnicodeSet format, which is specified in [UTS #35 Part One](tr35.md#Unicode_Sets).

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

> **NOTE**: The Keyboard Test Data format is a technical preview, it is subject to revision in future versions of CLDR.


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

This attribute value specifies a unique name for this suite of tests. These names could be output to the user during test execution, used to summarize success and failure, or used to select or deselect test components.

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

> **NOTE**: The Keyboard Test Data format is a technical preview, it is subject to revision in future versions of CLDR.


> <small>
>
> Parents: [tests](#test-element-tests)
>
> Children: [startContext](#test-element-startcontext), [emit](#test-element-emit), [keystroke](#test-element-keystroke), [backspace](#test-element-backspace), [check](#test-element-check), [_special_](tr35.md#special)
>
> Occurrence: Required, Multiple
> </small>

This attribute value specifies a specific isolated regression test. Multiple test elements do not interact with each other.

The order of child elements is significant, with cumulative effects: they must be processed from first to last.

_Attribute:_ `name` (required)

This attribute value specifies a unique name for this particular test. These names could be output to the user during test execution, used to summarize success and failure, or used to select or deselect test components.

**Example**

```xml
<info keyboard="fr-t-k0-azerty.xml" author="Team Keyboard" name="fr-test" />
```

### Test Element: startContext

> **NOTE**: The Keyboard Test Data format is a technical preview, it is subject to revision in future versions of CLDR.

This element specifies pre-existing text in a document, as if prior to the user’s insertion point. This is useful for testing transforms and reordering. If not specified, the startContext can be considered to be the empty string ("").

> <small>
>
> Parents: [test](#test-element-test)
>
> Children: _none_
>
> Occurrence: Optional, Single
> </small>

_Attribute:_ `to` (required)

Specifies the starting context. This text may be escaped with `\u` notation, see [Escaping](#escaping).

**Example**

```xml
<startContext to="abc\u{0022}"/>
```


### Test Element: keystroke

> **NOTE**: The Keyboard Test Data format is a technical preview, it is subject to revision in future versions of CLDR.


> <small>
>
> Parents: [test](#test-element-test)
>
> Children: _none_
>
> Occurrence: Optional, Multiple
> </small>

This element contains a single keystroke or other gesture event, identified by a particular key element.

Optionally, one of the gesture attributes, either `flick`, `longPress`, or `tapCount` may be specified. If none of the gesture attribute values are specified, then a regular keypress is effected on the key.  It is an error to specify more than one gesture attribute.

If a key is not found, or a particular gesture has no definition, the output should be behave as if the user attempted to perform such an action.  For example, an unspecified `flick` would result in no output.

When a key is found, processing continues with the transform and other elements before updating the test output buffer.

_Attribute:_ `key` (required)

This attribute value specifies a key by means of the key’s `id` attribute.

_Attribute:_ `flick`

This attribute value specifies a flick gesture to be performed on the specified key instead of a keypress, such as `e` or `nw se`. This value corresponds to the `directions` attribute value of the [`<flickSegment>`](#element-flicksegment) element.

_Attribute:_ `longPress`

This attribute value specifies that a long press gesture should be performed on the specified key instead of a keypress. For example, `longPress="2"` indicates that the second character in a longpress series should be chosen. `longPress="0"` indicates that the `longPressDefault` value, if any, should be chosen. This corresponds to `longPress` and `longPressDefault` on [`<key>`](#element-key).

_Attribute:_ `tapCount`

This attribute value specifies that a multi-tap gesture should be performed on the specified key instead of a keypress. For example, `tapCount="3"` indicates that the key should be tapped three times in rapid succession. This corresponds to `multiTap` on [`<key>`](#element-key). The minimum tapCount is 2.

**Example**

```xml
<keystroke key="q"/>
<keystroke key="doublequote"/>
<keystroke key="s" flick="nw se"/>
<keystroke key="e" longPress="1"/>
<keystroke key="E" tapCount="2"/>
```

### Test Element: emit

> **NOTE**: The Keyboard Test Data format is a technical preview, it is subject to revision in future versions of CLDR.


> <small>
>
> Parents: [test](#test-element-test)
>
> Children: _none_
>
> Occurrence: Optional, Multiple
> </small>

This element also contains an input event, except that the input is specified in terms of textual value rather than key or gesture identity. This element is particularly useful for testing transforms.

Processing of the specified text continues with the transform and other elements before updating the test output buffer.

_Attribute:_ `to` (required)

This attribute value specifies a string of output text representing a single keystroke or gesture. This string is intended to match the output of a `key`, `flick`, `longPress` or `multiTap` element or attribute.
Tooling should give a hint if this attribute value does not match at least one keystroke or gesture. Note that the specified text is not injected directly into the output buffer.

This attribute value may be escaped with `\u` notation, see [Escaping](#escaping).

**Example**

```xml
<emit to="s"/>
```


### Test Element: backspace

> **NOTE**: The Keyboard Test Data format is a technical preview, it is subject to revision in future versions of CLDR.


> <small>
>
> Parents: [test](#test-element-test)
>
> Children: _none_
>
> Occurrence: Optional, Multiple
> </small>

This element contains a backspace action, as if the user typed the backspace key

**Example**

```xml
<backspace/>
```

### Test Element: check

> **NOTE**: The Keyboard Test Data format is a technical preview, it is subject to revision in future versions of CLDR.


> <small>
>
> Parents: [test](#test-element-test)
>
> Children: _none_
>
> Occurrence: Optional, Multiple
> </small>

This element contains a check on the current output buffer.

_Attribute:_ `result` (required)

This attribute value specifies the expected resultant text in a document after processing this event and all prior events, and including any `startContext` text.  This text may be escaped with `\u` notation, see [Escaping](#escaping).

**Example**

```xml
<check result="abc\u{0022}s\u{0022}•éÈ"/>
```


### Test Examples

```xml

<test name="spec-sample">
    <startContext to="abc\u{0022}"/>
    <!-- simple, key specified by to -->
    <emit to="s"/>
    <check result="abc\u{0022}s"/>
    <!-- simple, key specified by id -->
    <keystroke key="doublequote"/>
    <check result="abc\u{0022}s\u{0022}"/>
    <!-- flick -->
    <keystroke key="s" flick="nw se"/>
    <check result="abc\u{0022}s\u{0022}•"/>
    <!-- longPress -->
    <keystroke key="e" longPress="1"/>
    <check result="abc\u{0022}s\u{0022}•é"/>
    <!-- multiTap -->
    <keystroke key="E" tapCount="2"/>
    <check result="abc\u{0022}s\u{0022}•éÈ"/>
</test>
```

* * *

Copyright © 2001–2024 Unicode, Inc. All Rights Reserved. The Unicode Consortium makes no expressed or implied warranty of any kind, and assumes no liability for errors or omissions. No liability is assumed for incidental and consequential damages in connection with or arising out of the use of the information or programs contained or accompanying this technical report. The Unicode [Terms of Use](https://www.unicode.org/copyright.html) apply.

Unicode and the Unicode logo are trademarks of Unicode, Inc., and are registered in some jurisdictions.


[keyboard-workgroup]: https://cldr.unicode.org/index/keyboard-workgroup

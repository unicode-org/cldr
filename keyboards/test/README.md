# Technical Preview: CLDR Keyboard Test Data

## <a name="Contents" href="#Contents">Contents of CLDR Keyboard Test Data</a>

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
* [Copyright and License](#copyright-and-license)

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

## Copyright and License

See the top level [README.md](../../README.md#copyright--licenses)

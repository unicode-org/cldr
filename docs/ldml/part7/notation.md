## <a name="Notation" id="Notation" href="#Notation">Notation</a>

- Ellipses (`…`) in syntax examples are used to denote substituted parts.

* For example, `id="…keyId"` denotes that `…keyId` (the part between double quotes) is to be replaced with something, in this case a key identifier. As another example, `\u{…usv}` denotes that the `…usv` is to be replaced with something, in this case a Unicode scalar value in hex.


### <a name="Escaping" id="Escaping" href="#Escaping">Escaping</a>

When explicitly specified, attribute values can contain escaped characters. This specification uses two methods of escaping, the _UnicodeSet_ notation and the `\u{…usv}` notation.

### <a name="UnicodeSet_Escaping" id="UnicodeSet_Escaping" href="#UnicodeSet_Escaping">UnicodeSet Escaping</a>

The _UnicodeSet_ notation is described in [UTS #35 section 5.3.3](tr35.md#Unicode_Sets) and allows for comprehensive character matching, including by character range, properties, names, or codepoints.

Note that the `\u1234` and `\x{C1}` format escaping is not supported, only the `\u{…}` format (using `bracketedHex`).

Currently, the following attribute values allow _UnicodeSet_ notation:

* `from` or `before` on the `<transform>` element
* `from` or `before` on the `<reorder>` element

### <a name="UTS18_Escaping" id="UTS18_Escaping" href="#UTS18_Escaping">UTS18 Escaping</a>

* The `\u{…usv}` notation, a subset of hex notation, is described in [UTS #18 section 1.1](https://www.unicode.org/reports/tr18/#Hex_notation). It can refer to one or multiple individual codepoints. Currently, the following attribute values allow the `\u{…}` notation:


* `output` on the `<key>` element
* `from` or `to` on the `<transform>` element
* `value` on the `<variable>` element
* `output` and `display` on the `<display>` element
* `baseCharacter` on the `<displayOptions>` element

Characters of general category of Mark (M), Control characters (Cc), Format characters (Cf), and whitespace other than space should be encoded using one of the notation above as appropriate.

Attribute values escaped in this manner are annotated with the `<!--@ALLOWS_UESC-->` DTD annotation, see [DTD Annotations](tr35.md#dtd-annotations)

* * *


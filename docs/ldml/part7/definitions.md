## <a name="Definitions" id="Definitions" href="#Definitions">Definitions</a>

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

**Layer** is an arrangement of keys on a touch keyboard. A touch keyboard is made up of a set of layers. Each layer may have a different key layout, unlike with a hardware keyboard, and may not correspond directly to a hardware keyboard's modifier keys. A layer is accessed via a layer-switching key. See also touch keyboard and modifier.

**Long-press key:** also known as a “child key”. A secondary key that is invoked from a top level key on a touch keyboard. Secondary keys typically provide access to variants of the top level key, such as accented variants (a => á, à, ä, ã)

**Modifier:** A key that is held to change the behavior of a hardware keyboard. For example, the "Shift" key allows access to upper-case characters on a US keyboard. Other modifier keys include but are not limited to: Ctrl, Alt, Option, Command and Caps Lock. On a touch keyboard, keys that appear to be modifier keys should be considered to be layer-switching keys.

**Physical keyboard:** see **Hardware keyboard**

**Touch keyboard:** A keyboard that is rendered on a, typically, touch surface. It has a dynamic arrangement and contrasts with a hardware keyboard. This term has many synonyms: software keyboard, SIP (Software Input Panel), virtual keyboard. This contrasts with other uses of the term virtual keyboard as an on-screen keyboard for reference or accessibility data entry.

**Transform:** A transform is an element that specifies a set of conversions from sequences of code points into one (or more) other code points. Transforms may reorder or replace text. They may be used to implement “dead key” behaviors, simple orthographic corrections, visual (typewriter) type input etc.

**Virtual keyboard:** see **Touch keyboard**


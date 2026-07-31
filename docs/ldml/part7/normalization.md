## <a name="Normalization" id="Normalization" href="#Normalization">Normalization</a>

Unicode Normalization, as described in [The Unicode Standard](https://www.unicode.org/reports/tr41/#Unicode/), is a process by which Unicode text is processed to eliminate unwanted distinctions.

This section discusses how conformant keyboards are affected by normalization, and the impact of normalization on keyboard authors and keyboard implmentations.

Keyboard implementations will usually apply normalization as appropriate when matching transform rules and `<display>` value matching.
Output from the keyboard, following application of all transform rules, will be normalized to the appropriate form by the keyboard implementation.

> Note: There are many existing software libraries which perform Unicode Normalization, including [ICU](https://icu.unicode.org), [ICU4X](https://icu4x.unicode.org), and JavaScript's [String.prototype.normalize()](https://developer.mozilla.org/docs/Web/JavaScript/Reference/Global_Objects/String/normalize).

* Keyboard authors will not typically need to perform normalization as part of the keyboard layout.  However, authors should be aware of areas where normalization affects keyboard operation so that they may achieve their desired results.


### <a name="Where_Normalization_Occurs" id="Where_Normalization_Occurs" href="#Where_Normalization_Occurs">Where Normalization Occurs</a>

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

* Example: The input context contains U+00E8 (`è`).  The user clicks the cursor after this character, then presses a key producing `x`. A transform rule `<transform from='x' to='\u{0320}'/>` matches. The implementation must normalize the intermediate buffer to `e\u{0320}\u{0300}` (`è̠`) before proceeding to the next `transformGroup`.


4. **Before output to the platform/application**

    Text must be normalized into the output form requested by the platform or application. This will typically be NFC, but may not be.

    - If normalizing to NFC, full normalization (reorder+composition) will be required.
    - No markers are present in this text, they are removed prior to output but retained in the implementation's input context for subsequent keystrokes. See [markers](#markers).

* Example: The result of keystrokes and transform processing produces the string `e\u{0300}`. The keyboard implementation normalizes this to a single NFC codepoint U+00E8 (`è`), which is returned to the application.


### <a name="Normalization_and_Transform_Matching" id="Normalization_and_Transform_Matching" href="#Normalization_and_Transform_Matching">Normalization and Transform Matching</a>

* Regardless of the normalization form in the keyboard source file or in the edit buffer context, transform matching will be performed using **NFD**. For example, all of the following transforms will match the input strings è̠, whether the input is U+00E8 U+0320, U+0065 U+0320 U+0300, or U+0065 U+0300 U+0320.


```xml
<transform from="e\u{0320}\u{0300}" /> <!-- NFD -->
<transform from="\u{00E8}\u{0320}"  /> <!-- NFC: è + U+0320 -->
<transform from="e\u{0300}\u{0320}" /> <!-- Unnormalized -->
```

### <a name="Normalization_and_Markers" id="Normalization_and_Markers" href="#Normalization_and_Markers">Normalization and Markers</a>

A special issue occurs when markers are involved.
[Markers](#markers) are not text, and so not themselves modified or reordered by the Unicode Normalization Algorithm.
Existing Normalization APIs typically operate on plain text, and so those APIs can not be used with content containing markers.

However, the markers must be retained and processed by keyboard implementations in a manner which will be both consistent across implementations and predictable to keyboard authors.
Inconsistencies would result in different user experiences — specifically, different or incorrect text output — on some implementations and not another.
Unpredictability would make it challenging for the keyboard author to create a keyboard with expected behavior.

This section gives an algorithm for implementing normalization on a text stream including markers.

_Note:_ When the algorithm is performed on a plain text stream that doesn't include markers, implementations may skip the removing/re-adding steps 1 and 3 because no markers are involved.

#### <a name="Rationale_for_gluing_markers" id="Rationale_for_gluing_markers" href="#Rationale_for_gluing_markers">Rationale for 'gluing' markers</a>

The processing described here describes an extension to Unicode normalization to account for the desired behavior of markers.

* The algorithm described considers markers 'glued' (remaining with) the following character. If a context ends with a marker, that marker would be guaranteed to remain at the end after processing, consistently located with respect to the next keystroke to be input.


1. Keyboard authors can keep a marker together with a character of interest by emitting the marker just previous to that character.

* For example, given a key `output="\m{marker}X"`, the marker will proceed `X` regardless of any normalization. (If `output="X\m{marker}"` were used, and `X` were to reorder with other characters, the marker would no longer be adjacent to the X.)


2. Markers which are at the end of the input remain at the end of input during normalization.

For example, given input context which ends with a marker, such as `...ABCDX\m{marker}`, the marker will remain at the end of the input context regardless of any normalization.

The 'gluing' is only applicable during one particular processing step. It does not persist or affect further processing steps or future keystrokes.

#### <a name="Data_Model_Marker" id="Data_Model_Marker" href="#Data_Model_Marker">Data Model: `Marker`</a>

For purposes of this algorithm, a `Marker` is an opaque data type which has one property, its ID. See [Markers](#markers) for a discussion of the marker ID.

#### <a name="Data_Model_string" id="Data_Model_string" href="#Data_Model_string">Data Model: string</a>

* For purposes of this algorithm, a string is an array of elements, where each element is either a codepoint or a `Marker`. For example, a [`key`](#element-key) in the XML such as `<key id="sha" output="𐓯\m{mymarker}x" />` would produce a string with three elements:


1. The codepoint U+104EF
2. The `Marker` named `mymarker`
3. The codepoint U+0078

If this string were output to an application, it would be converted to _plain text_ by removing all markers, which would yield the plain text string with only two codepoints: `𐓯x`.

#### <a name="Data_Model_MarkerEntry" id="Data_Model_MarkerEntry" href="#Data_Model_MarkerEntry">Data Model: `MarkerEntry`</a>

This algorithm uses a temporary data structure which is an ordered array of `MarkerEntry` elements.

Each `MarkerEntry` element has the following properties:
- `glue` (a codepoint, or the special value `END_OF_SEGMENT`)
- `divider?` (true/false)
- `processed?` (true/false, defaults to false)
- `marker` (the `Marker` object)

#### <a name="Marker_Algorithm_Overview" id="Marker_Algorithm_Overview" href="#Marker_Algorithm_Overview">Marker Algorithm Overview</a>

This algorithm has three main phases to it.

1. **Parsing/Removing Markers**

    In this phase, the input string is analyzed to locate all markers. Metadata about each marker is stored in a temporary `MarkerArray` data structure.
    Markers are removed from the input string, leaving only plain text.

2. **Plain Text Processing**

    This phase is performed on the plain text string, such as NFD normalization.

3. **Re-Adding Markers**

    Finally, markers are re-added to the plain text string using the `MarkerEntry` metadata from step 1.
    This phase results in a string which contains both codepoints and markers.

#### <a name="Phase_1_ParsingRemoving_Markers" id="Phase_1_ParsingRemoving_Markers" href="#Phase_1_ParsingRemoving_Markers">Phase 1: Parsing/Removing Markers</a>

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

#### <a name="Phase_2_Plain_Text_Processing" id="Phase_2_Plain_Text_Processing" href="#Phase_2_Plain_Text_Processing">Phase 2: Plain Text Processing</a>

See [UAX #15](https://www.unicode.org/reports/tr15/#Description_Norm) for an overview of the process.  An existing Unicode-compliant API can be used here.

#### <a name="Phase_3_Adding_Markers" id="Phase_3_Adding_Markers" href="#Phase_3_Adding_Markers">Phase 3: Adding Markers</a>

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

#### <a name="Example_Normalization_with_Markers" id="Example_Normalization_with_Markers" href="#Example_Normalization_with_Markers">Example Normalization with Markers</a>

**Example 1a**

Consider this example, without markers:

- `e\u{0300}\u{0320}` (input)
- `e\u{0320}\u{0300}` (NFD)

The combining marks are reordered.

**Example 1b**

If we add markers:

- `e\u{0300}\m{marker}\u{0320}` (input)
- `e\m{marker}\u{0320}\u{0300}` (NFD)

Note that the marker is 'glued' to the _following_ character. In the above example, `\m{marker}` was 'glued' to the `\u{0320}`.

**Example 2**

A second example:

- `e\m{marker0}\u{0300}\m{marker1}\u{0320}\m{marker2}` (input)
- `e\m{marker1}\u{0320}\m{marker0}\u{0300}\m{marker2}` (NFD)

* Here `\m{marker2}` is 'glued' to the end of the string. However, if additional text is added such as by a subsequent keystroke (which may add an additional combining character, for example), this marker may be 'glued' to that following text.


Markers remain in the same normalization-safe segment during normalization. Consider:

**Example 3**

- `e\u{0300}\m{marker1}\u{0320}a\u{0300}\m{marker2}\u{0320}` (original)
- `e\m{marker1}\u{0320}\u{0300}a\m{marker2}\u{0320}\u{0300}` (NFD)

There are two normalization-safe segments here:

1. `e\u{0300}\m{marker1}\u{0320}`
2. `a\u{0300}\m{marker2}\u{0320}`

* Normalization (and marker rearranging) effectively occurs within each segment.  While `\m{marker1}` is 'glued' to the `\u{0320}`, it is glued within the first segment and has no effect on the second segment.


### <a name="Normalization_and_Character_Classes" id="Normalization_and_Character_Classes" href="#Normalization_and_Character_Classes">Normalization and Character Classes</a>

* If pre-composed (non-NFD) characters are used in [character classes](#regex-like-syntax), such as `[á-é]`, these may not match as keyboard authors expect, as the U+00E1 character (á) will not occur in NFD form. Thus this may be masking serious errors in the data.


Tools that process keyboard data must reject the data when character classes include non-NFD characters.

The above should be written instead as a regex `(á|â|ã|ä|å|æ|ç|è|é)`. Alternatively, it could be written as a set variable `<set id="Example" value="á â ã ä å æ ç è é"/>` and matched as `$[Example]`.

* There is another case where there is no explicit mention of a non-NFD character, but the character class could include non-NFD characters, such as the range `[\u{0020}-\u{01FF}]`. For these, the tools should raise a warning by default.


### <a name="Normalization_and_Reorder_elements" id="Normalization_and_Reorder_elements" href="#Normalization_and_Reorder_elements">Normalization and Reorder elements</a>

[`reorder`](#element-reorder) elements operate on NFD codepoints.

### <a name="Normalizationsafe_Segments" id="Normalizationsafe_Segments" href="#Normalizationsafe_Segments">Normalization-safe Segments</a>

For purposes of this algorithm, "normalization-safe segments" are defined as a string of codepoints which are

1. already in [NFD](https://www.unicode.org/reports/tr15/#Norm_Forms), and
2. begin with a character with [Canonical Combining Class](https://www.unicode.org/reports/tr44/#Canonical_Combining_Class_Values) of `0`.

See [UAX #15 Section 9.1: Stable Code Points](https://www.unicode.org/reports/tr15/#Stable_Code_Points) for related discussion.
Text under consideration can be segmented by locating such characters.

### <a name="Normalization_and_Output" id="Normalization_and_Output" href="#Normalization_and_Output">Normalization and Output</a>

* On output, text will be normalized into a specified normalization form. That form will typically be NFC, but an implementation may allow a calling application to override the choice of normalization form.

For example, many platforms may request NFC as the output format. In such a case, all text emitted via the keyboard will be transformed into NFC.

* Existing text in a document will only have normalization applied within a single normalization-safe segment from the caret.  The output will not contain any markers, thus any normalization is unaffected by any markers embedded within the segment.


For example, the sequence `e\m{marker}\u{300}` would be output in NFC as `è`. The marker is removed and has no effect on the output.

### <a name="Disabling_Normalization" id="Disabling_Normalization" href="#Disabling_Normalization">Disabling Normalization</a>

The attribute value `normalization="disabled"` can be used to indicate that no automatic normalization is to be applied in input, matching, or output. Using this setting should be done with caution:

- When this attribute value is used, all matching and output uses only the exact codepoints provided by the keyboard author.
- The input context from the application may not be normalized, which means that the keyboard author should consider all possible combinations, including NFC, NFD, and mixed normalization in `<transform from=` attributes.
- See [`<settings>`](#element-settings) for further details.

The majority of the above section only applies when `normalization="disabled"` is not used.

* * *


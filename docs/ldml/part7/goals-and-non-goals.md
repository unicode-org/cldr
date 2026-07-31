## <a name="Goals_and_Nongoals" id="Goals_and_Nongoals" href="#Goals_and_Nongoals">Goals and Non-goals</a>

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

### <a name="Compatibility_Notice" id="Compatibility_Notice" href="#Compatibility_Notice">Compatibility Notice</a>

> A major rewrite of this specification, called "Keyboard 3.0", was introduced in CLDR v45.
> The changes required were too extensive to maintain compatibility. For this reason, the `ldmlKeyboard3.dtd` DTD is _not_ compatible with DTDs from prior versions of CLDR such as v43 and prior.
>
> To process earlier XML files, use the data and specification from v43.1, found at <https://www.unicode.org/reports/tr35/tr35-69/tr35.html>
>
> `ldmlKeyboard.dtd` continues to be made available in CLDR, however, it will not be updated.

### <a name="Accessibility" id="Accessibility" href="#Accessibility">Accessibility</a>

* Keyboard use can be challenging for individuals with various types of disabilities. For this revision, features or architectural designs specifically for the purpose of improving accessibility are not yet included. However:


1. Having an industry-wide standard format for keyboards will enable accessibility software to make use of keyboard data with a reduced dependence on platform-specific knowledge.
2. Features which require certain levels of mobility or speed of entry should be considered for their impact on accessibility. This impact could be mitigated by means of additional, accessible methods of generating the same output.
3. Public feedback is welcome on any aspects of this document which might hinder accessibility.


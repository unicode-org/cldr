## <a name="Rational_Numbers" id="Rational_Numbers" href="#Rational_Numbers">Rational Numbers</a>

> [!IMPORTANT]
> Rational numbers (this section) are a technical preview and should not be considered stable.

```dtd
<!ELEMENT rationalFormats ( alias | ( rationalPattern*, integerAndRationalPattern*, rationalUsage*, special* ) ) >
<!ATTLIST rationalFormats numberSystem CDATA #REQUIRED >

<!ELEMENT rationalPattern ( #PCDATA ) >
<!ATTLIST rationalPattern alt NMTOKENS #IMPLIED >
<!ATTLIST rationalPattern draft (approved | contributed | provisional | unconfirmed) #IMPLIED >

<!ELEMENT integerAndRationalPattern ( #PCDATA ) >
<!ATTLIST integerAndRationalPattern alt NMTOKENS #IMPLIED >
<!ATTLIST integerAndRationalPattern draft (approved | contributed | provisional | unconfirmed) #IMPLIED >

<!ELEMENT rationalUsage ( #PCDATA ) >
<!ATTLIST rationalUsage alt NMTOKENS #IMPLIED >
<!ATTLIST rationalUsage draft (approved | contributed | provisional | unconfirmed) #IMPLIED >

```
For example:

```xml
<rationalFormats numberSystem="latn">
			<rationalPattern>{0}⁄{1}</rationalPattern>
			<integerAndRationalPattern>{0} {1}</integerAndRationalPattern>
			<integerAndRationalPattern alt="superSub">{0}⁠{1}</integerAndRationalPattern>
			<rationalUsage>sometimes</rationalUsage>
</rationalFormats>
```

The rational number patterns specify the formatting of rational fractions in different languages.
Rational fractions contain a numerator and denominator, such as ½, and may also have an integer, such a 5½.
There are two different “combination patterns”, needed because sometimes fonts and rendering systems don’t properly support fractions (such as displaying 5 1/2),
and need two patterns: one with a space and one without.
The choice of which to use depends on the rendering system and font support available, as described below.

Here are the the English values for example, and a short description of the purpose of each field:

| Code | Default Value | Description |
| :---- | :---: | :---- |
| `rationalPattern` | {0}⁄{1} | The format for a rational fraction with arbitrary numerator and denominator; the English pattern uses the Unicode character ‘⁄’ U+2044 FRACTION SLASH which causes composition of fractions such as 22⁄7, when supported properly by rendering systems and fonts. |
| `integerAndRationalPattern` | {0} {1} | The format for combining an integer with a rational fraction that is composed using the `Rational` pattern; the English pattern uses U+202F NARROW NO-BREAK SPACE (NNBSP) to produce a _non-breaking thin space_. |
| `integerAndRationalPattern-superSub` | {0}⁠{1} | The format for combining an integer with a rational fraction that is composed using the `Rational` pattern; the English pattern uses U+2060 WORD JOINER, a _zero-width no-break space_. |
| `rationalUsage` | sometimes | An indication of the extent to which rational fractions are used in the locale; either `never` or `sometimes`. |

* The `integerAndRationalPattern-superSub` is used for an integer with fraction. However, some fonts and rendering systems don’t properly handle the fraction slash, and the user would see something like **51/2** (fifty-one halves) when **5½** is desired\!

Therefore, the `integerAndRationalPattern` is available also, which forces a visible space between the integer and fraction (**5 ½**).
(In some languages, there there may always be a space: in that case the patterns for `integerAndRationalPattern` and `integerAndRationalPattern-superSub` will be identical. )

In environments where the rendering system and font can't be trusted to handle U+2044 FRACTION SLASH properly, there are a few techniques available to have a better rendering than 22/7:
- Use markup such as HTML `<super>` and `<sub>` for the numerator and denominator.
- Where markup is not available and the numbering system is `latn` (ASCII digits 0..9), there are two other choices:
    - If the fraction happens to match the precomposed fractions available in Unicode, those can be used (eg, ½ ⅔ ⅗ ⅐ ⅝ ¾ …)
    - The Latin superscript (¹ ² ³ …) and subscript digits (₁ ₂ ₃ …) digits can be used with the U+2044 FRACTION SLASH, such as ²²⁄₇.
    - In both cases, some fonts don't have consistent support for these characters, and so the sizes and positioning may vary.


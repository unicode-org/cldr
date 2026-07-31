## <a name="Person_Name_Validation" id="Person_Name_Validation" href="#Person_Name_Validation">Person Name Validation</a>

* When implementations allow entry of person names, they are often too strict; there are many instances where people can’t enter their real names, such as O’Brian, Stéphanie, Wałęsa, Þjóðólfr. Conversely, when an implementation is too lenient, it allows names like Ȟěl̀a, or B🅾️b. (See also [Zalgo](https://en.wikipedia.org/wiki/Zalgo_text).)


* Sometimes the constraints are imposed by limitations of outdated software or databases (such as not supporting Unicode character), or legal restrictions (such as only accepting names legal in Switzerland on native Swiss passports).


* However, when the limitations are due to unfamiliarity with the kinds of characters that can appear in languages, Unicode properties and CLDR data can help implementers to avoid being either too strict or too lenient.


### <a name="Letters" id="Letters" href="#Letters">Letters</a>

A common restriction is that the letters in a name only come from a single script. That may be too lenient: there are over 1,453 letters in the Latin script in Unicode 17\!

* To narrow it down, an implementation may form the union of exemplar characters from a set of languages in CLDR (together with their uppercase equivalents); these include letters and combining marks (accents). Here are some examples:


| Language | Exemplars (Main) |
| :---- | :---: |
| Icelandic | a á b d ð e é f g h i í j k l m n o ó p r s t u ú v x y ý þ æ ö |
| Polish | a ą b c ć d e ę f g h i j k l ł m n ń o ó p r s ś t u w y z ź ż |
| Arabic | ً ٌ ٍ َ ُ ِ ّ ْ ٰ ء أ ؤ إ ئ ا آ ب ة ت ث ج ح خ د ذ ر ز س ش ص ض ط ظ ع غ ف ق ك ل م ن ه و ى ي |
| Urdu | **ا ب پ ت ٹ ث ج چ ح خ د ڈ ذ ر ڑ ز ژ س ش ص ض ط ظ ع غ ف ق ک گ ل م ن و ہ ھ ء ی ے** |

There are also auxiliary exemplars (in the same script) that should be included, that are not part of the core alphabet, but are in use (typically loan words or names).
For example, in English someone would not be surprised to see a name such as René or Schröder.

| Language | Exemplars (Auxilliary) |
| :---- | :---: |
| Polish (aux) | à â å ä æ ç é è ê ë î ï ô ö œ q ß ù û ü v x ÿ |

It is often useful to explicitly include the exemplars from multiple languages.
For example, an implementation may choose to include the exemplars from official languages of the EU, or for major languages of Africa.
There is data in CLDR for the populations of languages in countries, and their official status, that may be useful for this.

### <a name="NonLetters" id="NonLetters" href="#NonLetters">Non-Letters</a>

* Names, even for a single name field like the family name, may have spaces, such as “de Silva”. Some additional punctuation characters commonly used in names are provided by the punctuation-person exemplars.


| Polish (punct-person) | , . \- / |
| :---- | :---: |

Those may include some variants of the ASCII hyphen; typically the best approach is to normalize them as below.

Examples include: Jean-Luc; Dr. Doom; James Smith Jr., MD

### <a name="Normalization" id="Normalization" href="#Normalization">Normalization</a>

When names are input from the keyboard, they should be normalized before validation. Typically the best foundation for that is Unicode NFC format. Additional useful normalizations are

* Replacement of arbitrary sequences of whitespace characters by a single space .
  * \\p{whitespace}{2,∞} → U+0020
* Replacement of  U+2010 HYPHEN and U+2011 NON-BREAKING HYPHEN
  * \[‐‑\] → \-

### <a name="Additional_possible_constraints" id="Additional_possible_constraints" href="#Additional_possible_constraints">Additional possible constraints</a>

* Other useful constraints include testing for extremely unusual cases, which may be mistakes or jokes ([Zalgo](https://en.wikipedia.org/wiki/Zalgo_text)). For these it is helpful to transform first into NFD, then apply the tests.


* Too many identical grapheme clusters in a sequence
  *  (Tóóóóóm)
* Too many non-letters in a row
  * (Jean—Luc Jr..,, MD)
* Too many combining marks in a row
  * Faruq̣̣̈̈

For further information, including confusables, mixed script detection, and so on, see [UTS \#39: Unicode Security Mechanisms](https://www.unicode.org/reports/tr39/).


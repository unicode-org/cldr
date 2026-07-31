## <a name="Transforms" id="Transforms" href="#Transforms">Transforms</a>

* Transforms provide a set of rules for transforming text via a specialized set of context-sensitive matching rules. They are commonly used for transliterations or transcriptions, but also other transformations such as full-width to half-width (for _katakana_ characters). The rules can be simple one-to-one relationships between characters, or involve more complicated mappings. Here is an example:


```xml
<transform source="Greek" target="Latin" variant="UNGEGN" direction="both">
    ...
    <comment>Useful variables</comment>
    <tRule>$gammaLike = [ΓΚΞΧγκξχϰ] ;</tRule>
    <tRule>$egammaLike = [GKXCgkxc] ;</tRule>
    ...
    <comment>Rules are predicated on running NFD first, and NFC afterwards</comment>
    <tRule>::NFD (NFC) ;</tRule>
    ...
    <tRule>λ ↔ l ;</tRule>
    <tRule>Λ ↔ L ;</tRule>
    ...
    <tRule>γ } $gammaLike ↔ n } $egammaLike ;</tRule>
    <tRule>γ ↔ g ;</tRule>
    ...
    <tRule>::NFC (NFD) ;</tRule>
    ...
</transform>
```

The source and target values are valid locale identifiers, where 'und' means an unspecified language, plus some additional extensions.

* The long names of a script according to [[UAX24](https://www.unicode.org/reports/tr41/#UAX24)] may be used instead of the short script codes. The script identifier may also omit und; that is, "und_Latn" may be written as just "Latn".
* The long names of the English languages may also be used instead of the languages.
* The term "Any" may be used instead of a solitary "und".
* Other identifiers may be used for special purposes. In CLDR, these include: Accents, Digit, Fullwidth, Halfwidth, Jamo, NumericPinyin, Pinyin, Publishing, Tone. (Other than these values, valid private use locale identifiers should be used, such as "x-Special".)
* When presenting localizing transform names, the "und\_" is normally omitted. Thus for a transliterator with the ID "und_Latn-und_Grek" (or the equivalent "Latin-Greek"), the translated name for Greek would be Λατινικό-Ελληνικό.

In version 29.0, BCP47 identifiers were added as aliases (while retaining the old identifiers). The following table shows the relationship between the old identifiers and the BCP47 format identifiers.

<!-- HTML: rowspan -->

<table>
<tbody>
<tr>
    <th>Old ID</th>
    <th>BCP47 ID</th>
    <th>Comments</th>
</tr>
<tr>
    <td><b>es_FONIPA</b>-es_419_FONIPA</td>
    <td>es-419-fonipa-t-<b>es-fonipa</b></td>
    <td rowspan="2">The order reverses with -t-. That is, the language subtag part is what results.</td>
</tr>
<tr>
    <td><b>hy_AREVMDA</b>-hy_AREVMDA_FONIPA</td>
    <td>hy-arevmda-fonipa-t-<b>hy-arevmda</b></td>
</tr>
<tr>
    <td><b>Devanagari</b>-Latin</td>
    <td>und-Latn-t-<b>und-deva</b></td>
    <td rowspan="2">Scripts add <b>und-</b></td>
</tr>
<tr>
    <td><b>Latin</b>-Devanagari</td>
    <td>und-Deva-t-<b>und-latn</b></td>
</tr>
<tr>
    <td>Greek-Latin/UNGEGN</td>
    <td>und-Latn-t-und-grek-<b>m0-ungegn</b></td>
    <td>Variants use the <b>-m0-</b> key.</td>
</tr>
<tr>
    <td>Russian-Latin/BGN</td>
    <td>ru<b>-Latn</b>-t-ru-m0-bgn</td>
    <td>Languages will have a script when it isn’t the default.</td>
</tr>
<tr>
    <td>Any-Hex/xml</td>
    <td>und-t-<b>d0-hex</b>-m0-xml</td>
    <td rowspan="2"><b>Any</b> becomes <b>und</b>, and keys <b>d0</b> (destination) and <b>s0</b> (source) are used for non-locales.</td>
</tr>
<tr>
    <td>Hex-Any/xml</td>
    <td>und-t-<b>s0-hex</b>-m0-xml</td>
</tr>
<tr>
    <td>Any-<b>Publishing</b></td>
    <td>und-t-d0-<b>publish</b></td>
    <td rowspan="2">Non-locales are normally the lowercases of the old ID, but may change because of BCP47 length restrictions.</td>
</tr>
<tr>
    <td><b>Publishing</b>-Any</td>
    <td>und-t-s0-<b>publish</b></td>
</tr>
</tbody>
</table>

Note that the script and region codes are cased iff they are in the main subtag, but are lowercase in extensions.

### <a name="Inheritance" id="Inheritance" href="#Inheritance">Inheritance</a>

* The CLDR transforms are built using the following locale inheritance. While this inheritance is not required of LDML implementations, the transforms supplied with CLDR may not otherwise behave as expected without some changes.


* For either the source or the target, the fallback starts from the maximized locale ID (using the likely-subtags data). It also uses the country for lookup before the base language is reached, and root is never accessed: instead the script(s) associated with the language are used. Where there are multiple scripts, the maximized script is tried first, and then the other scripts associated with the language (from supplemental data).


For example, see the bolded items below in the fallback chain for **az_IR**.

|     | Locale ID      | Comments                       |
| --- | -------------- | ------------------------------ |
|  1  | **az_Arab_IR** | The maximized locale for az_IR |
|  2  | az_Arab        | Normal fallback                |
|  3  | **az_IR**      | Inserted country locale        |
|  4  | az             | Normal fallback                |
|  5  | **Arab**       | Maximized script               |
|  6  | **Cyrl**       | Other associated script        |

* The source, target, and variant use "laddered" fallback, where the source changes the most quickly (using the above rules), then the target (using the above rules), then the variant if any, is discarded. That is, in pseudo code:


* for variant in {variant, ""}
  * for target in target-chain
    * for source in source-chain
      * transform = lookup source-target/variant
      * if transform != null return transform

For example, here is the fallback chain for **ru_RU-el_GR/BGN**.

| source |     | target | variant |
| ------ | --- | ------ | ------- |
| ru_RU  | -   | el_GR  | /BGN    |
| ru     | -   | el_GR  | /BGN    |
| Cyrl   | -   | el_GR  | /BGN    |
| ru_RU  | -   | el     | /BGN    |
| ru     | -   | el     | /BGN    |
| Cyrl   | -   | el     | /BGN    |
| ru_RU  | -   | Grek   | /BGN    |
| ru     | -   | Grek   | /BGN    |
| Cyrl   | -   | Grek   | /BGN    |
| ru_RU  | -   | el_GR  |         |
| ru     | -   | el_GR  |         |
| Cyrl   | -   | el_GR  |         |
| ru_RU  | -   | el     |         |
| ru     | -   | el     |         |
| Cyrl   | -   | el     |         |
| ru_RU  | -   | Grek   |         |
| ru     | -   | Grek   |         |
| Cyrl   | -   | Grek   |         |

Japanese and Korean are special, since they can be represented by combined script codes, such as ja_Jpan, ja_Hrkt, ja_Hira, or ja_Kana. These need to be considered in the above fallback chain as well.

#### <a name="Pivots" id="Pivots" href="#Pivots">Pivots</a>

* Transforms can also use _pivots_. These are used when there is no direct transform between a source and target, but there are transforms X-Y and Y-Z. In such a case, the transforms can be internally chained to get X-Y = X-Y;Y-Z. This is done explicitly with the Indic script transforms: to get Devanagari-Latin, internally it is done by transforming first from Devanagari to Interindic (an internal superset encoding for Indic scripts), then from Interindic to Latin. This allows there to be only N sets of transform rules for the Indic scripts: each one to and from Interindic. These pivots are explicitly represented in the CLDR transforms.


* Note that the characters currently used by Interindic are private use characters. To prevent those from “leaking” out into text, transforms converting from Interindic must ensure that they convert all the possible values used in Interindic.


* The pivots can also be produced automatically (implicitly), as a fallback. A particularly useful pivot is IPA, since that tends to preserve pronunciation. For example, _Czech to IPA_ can be chained with _IPA to Katakana_ to get _Czech to Katakana_.


* CLDR often has special forms of IPA: not just "und-FONIPA" but "cs-FONIPA": specifically IPA that has come from Czech. These variants typically preserve some features of the source language — such as double consonants — that are indistinguishable from single consonants in that language, but that are often preserved in traditional transliterations. Thus when matching prospective pivots, FONIPA is treated specially. If there is an exact match, that match is used (such as cs-cs_FONIPA + cs_FONIPA-ko). Otherwise, the language is ignored, as for example in cs-cs_FONIPA + ru_FONIPA-ko.


* The interaction of implicit pivots and inheritance may result in a longer inheritance chain lookup than desired, so implementers may consider having some sort of caching mechanism to increase performance.


### <a name="Variants" id="Variants" href="#Variants">Variants</a>

* Variants used in CLDR include UNGEGN and BGN, both indicating sources for transliterations. There is an additional attribute `private="true"` which is used to indicate that the transform is meant for internal use, and should not be displayed as a separate choice in a UI.


There are many different systems of transliteration. The goal for the "unqualified" script transliterations are

1. to be lossless when going to Latin and back
2. to be as lossless as possible when going to other scripts
3. to abide by a common standard as much as possible (possibly supplemented to meet goals 1 and 2).

Language-to-language transliterations, and variant script-to-script transliterations are generally transcriptions, and not expected to be lossless.

* Additional transliterations may also be defined, such as customized language-specific transliterations (such as between Russian and French), or those that match a particular transliteration standard, such as the following:


* UNGEGN - United Nations Group of Experts on Geographical Names
* BGN - United States Board on Geographic Names
* ISO9 - ISO/IEC 9
* ISO15915 - ISO/IEC 15915
* ISCII91 - ISCII 91
* KMOCT - South Korean Ministry of Culture & Tourism
* USLC - US Library of Congress
* UKPCGN - Permanent Committee on Geographical Names for British Official Use
* RUGOST - Russian Main Administration of Geodesy and Cartography

* The rules for transforms are described in [Transform Rules Syntax](#Transform_Rules_Syntax). For more information on Transliteration, see [Transliteration Guidelines](https://cldr.unicode.org/index/cldr-spec/transliteration-guidelines).


### <a name="Transform_Rules_Syntax" id="Transform_Rules_Syntax" href="#Transform_Rules_Syntax">Transform Rules Syntax</a>

```dtd
<!ELEMENT transforms ( transform*) >
<!ELEMENT transform ((comment | tRule)*) >
<!ATTLIST transform source CDATA #IMPLIED >
<!ATTLIST transform target CDATA #IMPLIED >
<!ATTLIST transform variant CDATA #IMPLIED >
<!ATTLIST transform direction ( forward | backward | both ) "both" >
<!ATTLIST transform alias CDATA #IMPLIED >
<!--@VALUE-->
<!ATTLIST transform backwardAlias CDATA #IMPLIED >
<!--@VALUE-->
<!ATTLIST transform visibility ( internal | external ) "external" >
<!ELEMENT comment (#PCDATA) >
<!ELEMENT tRule (#PCDATA) >
```

The `transform` attributes indicate the `source`, `target`, `direction`, and `alias`es. For example:

```xml
<transform
  source="ja_Hrkt"
  target="ja_Latn"
  variant="BGN"
  direction="forward"
  draft="provisional"
  alias="Katakana-Latin/BGN ja-Latn-t-ja-hrkt-m0-bgn">
```

The direction is either `forward` or `both` (`backward` is possible in theory, but not used). This indicates which directions the rules support.

* If the direction is `forward`, then an ID is composed from `target + "-" + source + "/" + variant`. If the direction is `both`, then the inverse ID is also value: `source + "-" + target + "/" + variant`. The `alias` attribute contains a space-delimited list of alternant forward IDs, while the `backwardAlias` contains a space-delimited list of alternant backward IDs. The BCP47 versions of the IDs will be in the `alias` and/or `backwardAlias` attributes.


The `visibility` attribute indicates whether the IDs should be externally visible, or whether they are only used internally.

Note: In CLDR v28 and before, the rules were expressed as fine-grained XML.
That was discarded in CLDR version 29, in favor of a simpler format where the separate rules are simply terminated with ";".

* The transform rules are similar to regular-expression substitutions, but adapted to the specific domain of text transformations. The rules and comments in this discussion will be intermixed, with # marking the comments. The simplest rule is a conversion rule, which replaces one string of characters with another. The conversion rule takes the following form:


```text
xy → z ;
```

This converts any substring "xy" into "z". Rules are executed in order; consider the following rules:

```text
sch → sh ;
ss → z ;
```

* This conversion rule transforms "bass school" into "baz shool". The transform walks through the string from start to finish. Thus given the rules above "bassch" will convert to "bazch", because the "ss" rule is found before the "sch" rule in the string (later, we'll see a way to override this behavior). If two rules can both apply at a given point in the string, then the transform applies the first rule in the list.


* All of the ASCII characters except numbers and letters are reserved for use in the rule syntax, as are the characters `→`, `←`, `↔`. Normally, these characters do not need to be converted. However, to convert them use either a pair of single quotes or a slash. The pair of single quotes can be used to surround a whole string of text. The slash affects only the character immediately after it. For example, to convert from a U+2190 ( ← ) LEFTWARDS ARROW to the string "arrow sign" (with a space), use one of the following rules:


```text
\←    → arrow\ sign ;
'←'   → 'arrow sign' ;
'←'   → arrow' 'sign ;
```

Note: The characters `→`, `←`, `↔` are preferred, but can be represented by the ASCII character `>`, `<`, and `<>`, respectively.

* Spaces may be inserted anywhere without any effect on the rules. Use extra space to separate items out for clarity without worrying about the effects. This feature is particularly useful with combining marks; it is handy to put some spaces around it to separate it from the surrounding text. The following is an example:


```text
→ i ; # an iota-subscript diacritic turns into an i.
```

For a real space in the rules, place quotes around it. For a real backslash, either double it \\\\, or quote it '\\'. For a real single quote, double it '', or place a backslash before it \\'.

Any text that starts with a hash mark and concludes a line is a comment. Comments help document how the rules work. The following shows a comment in a rule:

```text
x → ks ; # change every x into ks
```

The “\\u” and “\\x” hex notations can be used instead of any letter. For instance, instead of using the Greek π, one could write either of the following:

```text
\u03C0 → p ;
\x{3C0} → p ;
```

One can also define and use variables, such as:

```text
$pi = \u03C0 ;
$pi → p ;
```

#### <a name="Dual_Rules" id="Dual_Rules" href="#Dual_Rules">Dual Rules</a>

Rules can also specify what happens when an inverse transform is formed. To do this, we reverse the direction of the "←" sign. Thus the above example becomes:

```text
$pi ← p ;
```

With the inverse transform, "p" will convert to the Greek p. These two directions can be combined together into a dual conversion rule by using the `↔` operator, yielding:

```text
$pi ↔ p ;
```

#### <a name="Context" id="Context" href="#Context">Context</a>

* Context can be used to have the results of a transformation be different depending on the characters before or after. The following rule removes hyphens, but only when they follow lowercase characters:


```text
[:Lowercase:] { '-' → ;
```

Contexts can be before or after or both, such as in a rule to remove hyphens between lowercase and uppercase letters:

```text
[:Lowercase:] { '-' } [:Uppercase:] → ;
```

Each context is optional and may be empty; the following two rules are equivalent:

```text
$pi ↔ p ;
{$pi} ↔ {p} ;
```

The context itself ([: `Lowercase` :]) is unaffected by the replacement; only the text within braces is changed.

* Character classes (UnicodeSets) in the contexts can contain the special symbol $, which means “off either end of the string”. It is roughly similar to $ and ^ in regex. Unlike normal regex, however, it can occur in character classes. Thus the following rule removes hyphens that are after lowercase characters, _or_ are at the start of a string.


```text
[[:Lowercase:]$] {'-' → ;
```

* Thus the negation of a UnicodeSet will normally also match before or after the end of a string. The following will remove hyphens that are not after lowercase characters, _including hyphens at the start of a string_.


```text
[^[:Lowercase:]] {'-' → ;
```

It will thus convert “-B A-B a-b” to “B AB a-b”.

#### <a name="Revisiting" id="Revisiting" href="#Revisiting">Revisiting</a>

If the resulting text contains a vertical bar "|", then that means that processing will proceed from that point and that the transform will revisit part of the resulting text.
Thus the | marks a "cursor" position.
For example, if we have the following, then the string "xa" will convert to "yw".

```text
x → y | z ;
z a → w ;
```

First, "xa" is converted to "yza". Then the processing will continue from after the character "y", pick up the "za", and convert it. Had we not had the "|", the result would have been simply "yza".

* The '@' character can be used as filler character to place the revisiting point off the start or end of the string — but only within the context. Consider the following rules, with the table afterwards showing how they work.


```text
1. [a-z]{x > |@ab ;
2. ab > J;
3. ca > M;
```
The ⸠ indicates the virtual cursor:

| Current text | Matching rule |
| - | - |
| ⸠cx | no match, cursor advances one code point |
| c⸠x | matches rule 1, so the text is replaced and cursor backs up. |
| ⸠cab | matches rule 3, so the text is replaced, with cursor at the end. |
| Mb⸠ | cursor is at the end, so we are done. |

Notice that rule 2 did not have a chance to trigger.

There is a current restriction that @ cannot back up before the before_context or after the after_context.
Consider the rules if rule 1 is adjusted to have no before_context.

```text
1'. x > |@ab ;
2. ab > J ;
3. ca > M;
```

In that case, the results are different.
| Current text | Matching rule |
| - | - |
| ⸠cx | no match, cursor advances one code point |
| c⸠x | matches rule 1, so the text is replaced and cursor backs up; but only to where  |
| c⸠ab | matches **rule 2**, so the text is replaced, with cursor at the end. |
| cJ⸠ | cursor is at the end, so we are done. |

#### <a name="Example" id="Example" href="#Example">Example</a>

* The following shows how these features are combined together in the Transliterator "Any-Publishing". This transform converts the ASCII typewriter conventions into text more suitable for desktop publishing (in English). It turns straight quotation marks or UNIX style quotation marks into curly quotation marks, fixes multiple spaces, and converts double-hyphens into a dash.


```ebnf
# <a name="Variables" id="Variables" href="#Variables">Variables</a>

$single = \' ;
$space = ' ' ;
$double = \" ;
$back = \` ;
$tab = '\u0008' ;

# <a name="the_following_is_for_spaces_line_ends" id="the_following_is_for_spaces_line_ends" href="#the_following_is_for_spaces_line_ends">the following is for spaces, line ends, (, [, {, ...</a>
$makeRight = [[:separator:][:start punctuation:][:initial punctuation:]] ;

# <a name="fix_UNIX_quotes" id="fix_UNIX_quotes" href="#fix_UNIX_quotes">fix UNIX quotes</a>

$back $back → “ ; # generate right d.q.m. (double quotation mark)
$back → ‘ ;

# <a name="fix_typewriter_quotes_by_context" id="fix_typewriter_quotes_by_context" href="#fix_typewriter_quotes_by_context">fix typewriter quotes, by context</a>

$makeRight { $double ↔ “ ; # convert a double to right d.q.m. after certain chars
^ { $double → “ ; # convert a double at the start of the line.
$double ↔ ” ; # otherwise convert to a left q.m.

$makeRight {$single} ↔ ‘ ; # do the same for s.q.m.s
^ {$single} → ‘ ;
$single ↔ ’;

# <a name="fix_multiple_spaces_and_hyphens" id="fix_multiple_spaces_and_hyphens" href="#fix_multiple_spaces_and_hyphens">fix multiple spaces and hyphens</a>

$space {$space} → ; # collapse multiple spaces
'--' ↔ — ; # convert fake dash into real one
```

There is an online demo where the rules can be tested, at:

<https://util.unicode.org/UnicodeJsps/transform.jsp>

#### <a name="Rule_Syntax" id="Rule_Syntax" href="#Rule_Syntax">Rule Syntax</a>

The following describes the full format of the list of rules used to create a transform. Each rule in the list is terminated by a semicolon. The list consists of the following:

* an optional filter rule
* zero or more transform rules
* zero or more variable-definition rules
* zero or more conversion rules
* an optional inverse filter rule

* The filter rule, if present, must appear at the beginning of the list, before any of the other rules.  The inverse filter rule, if present, must appear at the end of the list, after all of the other rules.  The other rules may occur in any order and be freely intermixed.


The rule list can also generate the inverse of the transform. In that case, the inverse of each of the rules is used, as described below.

#### <a name="Transform_Rules" id="Transform_Rules" href="#Transform_Rules">Transform Rules</a>

Each transform rule consists of two colons followed by a transform name, which is of the form source-target. For example:

```text
:: NFD ;
:: und_Latn-und_Greek ;
:: Latin-Greek; # alternate form
```

* If either the source or target is 'und', it can be omitted, thus 'und_NFC' is equivalent to 'NFC'. For compatibility, the English names for scripts can be used instead of the und_Latn locale name, and "Any" can be used instead of "und". Case is not significant.


The following transforms are defined not by rules, but by the operations in the Unicode Standard, and may be used in building any other transform:

> **Any-NFC, Any-NFD, Any-NFKD, Any-NFKC** - the normalization forms defined by [[UAX15](https://www.unicode.org/reports/tr41/#UAX15)].
>
> **Any-Lower, Any-Upper, Any-Title** - full case transformations, defined by [[Unicode](tr35.md#Unicode)] Chapter 3.

In addition, the following special cases are defined:

> **Any-Null** - has no effect; that is, each character is left alone.
> **Any-Remove** - maps each character to the empty string; this, removes each character.

The inverse of a transform rule uses parentheses to indicate what should be done when the inverse transform is used. For example:

```text
:: lower () ; # only executed for the normal
:: (lower) ; # only executed for the inverse
:: lower ; # executed for both the normal and the inverse
```

#### <a name="Variable_Definition_Rules" id="Variable_Definition_Rules" href="#Variable_Definition_Rules">Variable Definition Rules</a>

Each variable definition is of the following form:

```text
$variableName = contents ;
```

* The variable name can contain letters and digits, but must start with a letter. More precisely, the variable names use Unicode identifiers as defined by [[UAX31](https://www.unicode.org/reports/tr41/#UAX31)]. The identifier properties allow for the use of foreign letters and numbers.


The contents of a variable definition is any sequence of Unicode sets and characters or characters. For example:

```ebnf
$mac = M [aA] [cC] ;
```

Variables are only replaced within other variable definition rules and within conversion rules. They have no effect on transliteration rules.

#### <a name="Filter_Rules" id="Filter_Rules" href="#Filter_Rules">Filter Rules</a>

* A filter rule consists of two colons followed by a UnicodeSet. This filter is global in that only the characters matching the filter will be affected by any transform rules or conversion rules. The inverse filter rule consists of two colons followed by a UnicodeSet in parentheses. This filter is also global for the inverse transform.


For example, the Hiragana-Latin transform can be implemented by "pivoting" through the Katakana converter, as follows:

```text
:: [:^Katakana:] ; # do not touch any katakana that was in the text!
:: Hiragana-Katakana;
:: Katakana-Latin;
:: ([:^Katakana:]) ; # do not touch any katakana that was in the text
                     # for the inverse either!
```

* The filters keep the transform from mistakenly converting any of the "pivot" characters. Note that this is a case where a rule list contains no conversion rules at all, just transform rules and filters.


#### <a name="Conversion_Rules" id="Conversion_Rules" href="#Conversion_Rules">Conversion Rules</a>

Conversion rules can be forward, backward, or double. The complete conversion rule syntax is described below:

* **Forward**:
  * A forward conversion rule is of the following form:
    ```text
    before_context { text_to_replace } after_context → completed_result | result_to_revisit ;
    ```
  * If there is no `before_context`, then the "{" can be omitted. If there is no `after_context`, then the "}" can be omitted. If there is no `result_to_revisit`, then the "|" can be omitted. A forward conversion rule is only executed for the normal transform and is ignored when generating the inverse transform.

* **Backward**:
  * A backward conversion rule is of the following form:
    ```text
    completed_result | result_to_revisit ← before_context { text_to_replace } after_context ;
    ```
  * The same omission rules apply as in the case of forward conversion rules. A backward conversion rule is only executed for the inverse transform and is ignored when generating the normal transform.

* **Dual**:
  * A dual conversion rule combines a forward conversion rule and a backward conversion rule into one, as discussed above. It is of the form:
    ```text
    a { b | c } d ↔ e { f | g } h ;
    ```
  * When generating the normal transform and the inverse, the revisit mark "|" and the before and after contexts are ignored on the sides where they do not belong. Thus, the above is exactly equivalent to the sequence of the following two rules:
    ```text
    a { b c } d → f | g  ;
    b | c  ←  e { f g } h ;
    ```

The `completed_result` | `result_to_revisit` is also known as the `resulting_text`. Either or both of the values can be empty. For example, the following removes any a, b, or c.

```text
[a-c] → ;
```

#### <a name="Intermixing_Transform_Rules_and_Conversion_Rules" id="Intermixing_Transform_Rules_and_Conversion_Rules" href="#Intermixing_Transform_Rules_and_Conversion_Rules">Intermixing Transform Rules and Conversion Rules</a>

Transform rules and conversion rules may be freely intermixed. Inserting a transform rule into the middle of a set of conversion rules has an important side effect.

* Normally, conversion rules are considered together as a group.  The only time their order in the rule set is important is when more than one rule matches at the same point in the string.  In that case, the one that occurs earlier in the rule set wins.  In all other situations, when multiple rules match overlapping parts of the string, the one that matches earlier wins.


* Transform rules apply to the whole string.  If you have several transform rules in a row, the first one is applied to the whole string, then the second one is applied to the whole string, and so on.  To reconcile this behavior with the behavior of conversion rules, transform rules have the side effect of breaking a surrounding set of conversion rules into two groups: First all of the conversion rules before the transform rule are applied as a group to the whole string in the usual way, then the transform rule is applied to the whole string, and then the conversion rules after the transform rule are applied as a group to the whole string.  For example, consider the following rules:


```text
abc → xyz;
xyz → def;
::Upper;
```

If you apply these rules to “abcxyz”, you get “XYZDEF”. If you move the “::Upper;” to the middle of the rule set and change the cases accordingly, then applying this to “abcxyz” produces “DEFDEF”.

```text
abc → xyz;
::Upper;
XYZ → DEF;
```

* This is because “::Upper;” causes the transliterator to reset to the beginning of the string. The first rule turns the string into “xyzxyz”, the second rule upper cases the whole thing to “XYZXYZ”, and the third rule turns this into “DEFDEF”.


This can be useful when a transform naturally occurs in multiple “passes.”  Consider this rule set:

```text
[:Separator:]* → ' ';
'high school' → 'H.S.';
'middle school' → 'M.S.';
'elementary school' → 'E.S.';
```

* If you apply this rule to “high school”, you get “H.S.”, but if you apply it to “high  school” (with two spaces), you just get “high school” (with one space). To have “high school” (with two spaces) turn into “H.S.”, you'd either have to have the first rule back up some arbitrary distance (far enough to see “elementary”, if you want all the rules to work), or you have to include the whole left-hand side of the first rule in the other rules, which can make them hard to read and maintain:


```ebnf
$space = [:Separator:]*;
high $space school → 'H.S.';
middle $space school → 'M.S.';
elementary $space school → 'E.S.';
```

Instead, you can simply insert “ `::Null;` ” in order to get things to work right:

```text
[:Separator:]* → ' ';
::Null;
'high school' → 'H.S.';
'middle school' → 'M.S.';
'elementary school' → 'E.S.';
```

* The “::Null;” has no effect of its own (the null transform, by definition, does not do anything), but it splits the other rules into two “passes”: The first rule is applied to the whole string, normalizing all runs of white space into single spaces, and then we start over at the beginning of the string to look for the phrases. “high    school” (with four spaces) gets correctly converted to “H.S.”.


This can also sometimes be useful with rules that have overlapping domains.  Consider this rule set from before:

```text
sch → sh ;
ss → z ;
```

* Applying this rule to “bassch” results in “bazch” because “ss” matches earlier in the string than “sch”. If you really wanted “bassh”—that is, if you wanted the first rule to win even when the second rule matches earlier in the string, you'd either have to add another rule for this special case...


```text
sch → sh ;
ssch → ssh;
ss → z ;
```

...or you could use a transform rule to apply the conversions in two passes:

```text
sch → sh ;
::Null;
ss → z ;
```

#### <a name="Inverse_Summary" id="Inverse_Summary" href="#Inverse_Summary">Inverse Summary</a>

* The following table shows how the same rule list generates two different transforms, where the inverse is restated in terms of forward rules (this is a contrived example, simply to show the reordering):


<!-- HTML: blocks in cells -->
<table>
<tbody>
<tr>
    <th>Original Rules</th>
    <th>Forward</th>
    <th>Inverse</th>
</tr>
<tr>
    <td><pre><code>:: [:Uppercase Letter:] ;
```text
:: latin-greek ;
:: greek-japanese ;
x ↔ y ;
z → w ;
r ← m ;
:: upper;
a → b ;
c ↔ d ;
:: any-publishing ;
:: ([:Number:]) ;</code></pre></td>
    <td><pre><code>:: [:Uppercase Letter:] ;
:: latin-greek ;
:: greek-japanese ;
x → y ;
z → w ;
:: upper ;
a → b ;
c → d ;
:: any-publishing ;</code></pre></td>
    <td><pre><code>:: [:Number:] ;
:: publishing-any ;
d → c ;
:: lower ;
y → x ;
m → r ;
:: japanese-greek ;
:: greek-latin ;</code></pre></td>
```
</tr>
</tbody>
</table>

* Note how the irrelevant rules (the inverse filter rule and the rules containing ←) are omitted (ignored, actually) in the forward direction, and notice how things are reversed: the transform rules are inverted and happen in the opposite order, and the groups of conversion rules are also executed in the opposite relative order (although the rules within each group are executed in the same order).


Because the order of rules matters, the following will not work as expected
```text
c → s;
ch → kh;
```
The second rule can never execute, because it is "masked" by the first.
To help prevent errors, implementations should try to alert readers when this occurs, eg:
```text
Rule {c > s;} masks {ch > kh;}
```

### <a name="Transform_Syntax_Characters" id="Transform_Syntax_Characters" href="#Transform_Syntax_Characters">Transform Syntax Characters</a>

The following summarizes the syntax characters used in transforms.

| Character(s) | Description | Example |
| - | - | - |
| ;  | End of a conversion rule, variable definition, or transform rule invocation | a → b ; |
| \:\: | Invoke a transform | :: Null ; |
| (, ) | In a transform rule invocation, marks the backwards transform | :: Null (NFD); |
| $ | Mark the start of a variable, when followed by an ASCII letter | $abc |
| = | Used to define variables | $a = abc ; |
| →, \> | Transform from left to right (only for forward conversion rules) | a → b ; |
| ←, \< | Transform from right to left (only for backward conversion rules) | a ← b ; |
| ↔, \<\> | Transform from left to right (for forward) and right to left (for backward) | a ↔ b ; |
| { | Mark the boundary between before_context and the text_to_replace | a {b} c → B ; |
| } | Mark the boundary between the text_to_replace and after_context | a {b} c → B ; |
| ' | Escape one or more characters, until the next '  | '\<\>' → x ; |
| " | Escape one or more characters, until the next " | "\<\>" → x ; |
| \\ | Escape the next character | \\\<\\\> → x ; |
| # | Comment (until the end of a line) | a → ; # remove a |
| \| | In the resulting_text, moves the cursor | a → A \| b; |
| @ | In the resulting_text, filler character used to move the cursor before the start or after the end of the result | a → Ab@\|; |
| (, ) | In text_to_replace, a capturing group | ([a-b]) > &hex($1); |
| $ | In replacement_text, when followed by 1..9, is replaced by the contents of a capture group | ([a-b]) > &hex($1); |
| ^ | In a before_context, by itself, equivalent to [$] **(deprecated)** | ... |
| ? | In a before_context, after_context, or text_to_replace, a possessive quantifier for zero or one  | a?b → c ; |
| + | In a before_context, after_context, or text_to_replace, a possessive quantifier for one or more  | a+b → c ; |
| * | In a before_context, after_context, or text_to_replace, a possessive quantifier for zero or more  | a*b → c ; |
| & | Invoke a function in the replacement_text | ([a-b]) > &hex($1); |
| !, %, _, ~, -, ., / | Reserved for future syntax | ... |
| SPACE | Ignored except when quoted | a b # same as ab |
| \uXXXX | Hex notation: 4 Xs | \u0061 |
| \x{XX...} | Hex notation: 1-6 Xs | \x{61} |
| [, ] | Marks a UnicodeSet | [a-z] |
| \p{...} | Marks a UnicodeSet formed from a property | \p{di} |
| \P{...} | Marks a negative UnicodeSet formed from a property | \p{DI} |
| $ | Within a UnicodeSet (not before ASCII letter), matches the start or end of the source text (but is not replaced) | [$] b → c |
| Other | Many of these characters have special meanings inside a UnicodeSet | ... |


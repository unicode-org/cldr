## <a name="CLDR_Collation" id="CLDR_Collation" href="#CLDR_Collation">CLDR Collation</a>

* Collation is the general term for the process and function of determining the sorting order of strings of characters, for example for lists of strings presented to users, or in databases for sorting and selecting records.


Collation varies by language, by application (some languages use special phonebook sorting), and other criteria (for example, phonetic vs. visual).

* CLDR provides collation data for many languages and styles. The data supports not only sorting but also language-sensitive searching and grouping under index headers. All CLDR collations are based on the [[UCA](https://www.unicode.org/reports/tr41/#UTS10)] default order, with common modifications applied in the CLDR root collation, and further tailored for language and style as needed.


### <a name="CLDR_Collation_Algorithm" id="CLDR_Collation_Algorithm" href="#CLDR_Collation_Algorithm">CLDR Collation Algorithm</a>

The CLDR collation algorithm is an extension of the [Unicode Collation Algorithm](https://www.unicode.org/reports/tr10/#Main_Algorithm).

#### <a name="Algorithm_FFFE" id="Algorithm_FFFE" href="#Algorithm_FFFE">U+FFFE</a>

* U+FFFE maps to a CE with a minimal, unique primary weight. Its primary weight is not "variable": U+FFFE must not become ignorable in alternate handling. On the identical level, a minimal, unique “weight” must be emitted for U+FFFE as well. This allows for [Merging Sort Keys](https://www.unicode.org/reports/tr10/#Merging_Sort_Keys) within code point space.


* For example, when sorting names in a database, a sortable string can be formed with _last_name_ + '\\uFFFE' + _first_name_. These strings would sort properly, without ever comparing the last part of a last name with the first part of another first name.


* For backwards secondary level sorting, text _segments_ separated by U+FFFE are processed in forward segment order, and _within_ each segment the secondary weights are compared backwards. This is so that such combined strings are processed consistently with merging their sort keys (for example, by concatenating them level by level with a low separator).


> 👉 **Note**: With unique, low weights on _all_ levels it is possible to achieve `sortkey(str1 + "\uFFFE" + str2) == mergeSortkeys(sortkey(str1), sortkey(str2))` . When that is not necessary, then code can be a little simpler (no special handling for U+FFFE except for backwards-secondary), sort keys can be a little shorter (when using compressible common non-primary weights for U+FFFE), and another low weight can be used in tailorings.

#### <a name="Context_Sensitive_Mappings" id="Context_Sensitive_Mappings" href="#Context_Sensitive_Mappings">Context-Sensitive Mappings</a>

* Contraction matching, as in the UCA, starts from the first character of the contraction string. It slows down processing of that first character even when none of its contractions matches. In some cases, it is preferrable to change such contractions to mappings with a prefix (context before a character), so that complex processing is done only when the less-frequently occurring trailing character is encountered.


* For example, the DUCET contains contractions for several variants of L· (L followed by middle dot). Collating ASCII text is slowed down by contraction matching starting with L/l. In the CLDR root collation, these contractions are replaced by prefix mappings (L|·) which are triggered only when the middle dot is encountered. CLDR also uses prefix rules in the Japanese tailoring, for processing of Hiragana/Katakana length and iteration marks.


* The mapping is conditional on the prefix match but does not change the mappings for the preceding text. As a result, a contraction mapping for "px" can be replaced by a prefix rule "p|x" only if px maps to the collation elements for p followed by the collation elements for "x if after p". In the DUCET, L· maps to CE(L) followed by a special secondary CE (which differs from CE(·) when · is not preceded by L). In the CLDR root collation, L has no context-sensitive mappings, but · maps to that special secondary CE if preceded by L.


* A prefix mapping for p|x behaves mostly like the contraction px, except when there is a contraction that overlaps with the prefix, for example one for "op". A contraction matches only new text (and consumes it), while a prefix matches only already-consumed text.


*   With mappings for "op" and "px", only the first contraction matches in text "opx". (It consumes the "op" characters, and there is no context-sensitive mapping for x.)
*   With mappings for "op" and "p|x", both the contraction and the prefix rule match in text "opx". (The prefix always matches already-consumed characters, regardless of whether they mapped as part of contractions.)

> 👉 **Note**: Matching of discontiguous contractions should be implemented without rewriting the text (unlike in the [[UCA](https://www.unicode.org/reports/tr41/#UTS10)] algorithm specification), so that prefix matching is predictable. (It should also help with contraction matching performance.) An implementation that does rewrite the text, as in the UCA, will get different results for some (unusual) combinations of contractions, prefix rules, and input text.

* Prefix matching uses a simple longest-match algorithm (op|c wins over p|c). It is recommended that prefix rules be limited to mappings where both the prefix string and the mapped string begin with an NFC boundary (that is, with a normalization starter that does not combine backwards). (In op|ch both o and c should be starters (ccc=0) and NFC_QC=Yes.) Otherwise, prefix matching would be affected by canonical reordering and discontiguous matching, like contractions. Prefix matching is thus always contiguous.


* A character can have mappings with both prefixes (context before) and contraction suffixes. Prefixes are matched first. This is to keep them reasonably implementable: When there is a mapping with both a prefix and a contraction suffix (like in Japanese: ぐ|ゞ), then the matching needs to go in both directions. The contraction might involve discontiguous matching, which needs complex text iteration and handling of skipped combining marks, and will consume the matching suffix. Prefix matching should be first because, regardless of whether there is a match, the implementation will always return to the original text index (right after the prefix) from where it will start to look at all of the contractions for that prefix.


* If there is a match for a prefix but no match for any of the suffixes for that prefix, then fall back to mappings with the next-longest matching prefix, and so on, ultimately to mappings with no prefix. (Otherwise mappings with longer prefixes would “hide” mappings with shorter prefixes.)


Consider the following mappings.

1. p → CE(p)
2. h → CE(h)
3. c → CE(c)
4. ch → CE(d)
5. p|c → CE(u)
6. p|ci → CE(v)
7. p|ĉ → CE(w)
8. op|ck → CE(x)

With these, text collates like this:

* pc → CE(p)CE(u)
* pci → CE(p)CE(v)
* pch → CE(p)CE(u)CE(h)
* pĉ → CE(p)CE(w)
* pĉ̣ → CE(p)CE(w)CE(U+0323) // discontiguous
* opck → CE(o)CE(p)CE(x)
* opch → CE(o)CE(p)CE(u)CE(h)

* However, if the mapping p|c → CE(u) is missing, then text "pch" maps to CE(p)CE(d), "opch" maps to CE(o)CE(p)CE(d), and "pĉ̣" maps to CE(p)CE(c)CE(U+0323)CE(U+0302) (because discontiguous contraction matching extends _an existing match_ by one non-starter at a time).


#### <a name="Algorithm_Case" id="Algorithm_Case" href="#Algorithm_Case">Case Handling</a>

* CLDR specifies how to sort lowercase or uppercase first, as a stronger distinction than other tertiary variants (**caseFirst**) or while completely ignoring all other tertiary distinctions (**caseLevel**). See _[Setting Options](#Setting_Options)_ and _[Case Parameters](#Case_Parameters)_.


#### <a name="Algorithm_Reordering_Groups" id="Algorithm_Reordering_Groups" href="#Algorithm_Reordering_Groups">Reordering Groups</a>

* CLDR specifies how to do parametric reordering of groups of scripts (e.g., “native script first”) as well as special groups (e.g., “digits after letters”), and provides data for the effective implementation of such reordering.


#### <a name="Combining_Rules" id="Combining_Rules" href="#Combining_Rules">Combining Rules</a>

Rules from different sources can be combined, with the later rules overriding the earlier ones. The following is an example of how this can be useful.

There is a root collation for "emoji" in CLDR. So use of "-u-co-emoji" in a Unicode locale identifier will access that ordering.

Example, using ICU:

```java
collator = Collator.getInstance(ULocale.forLanguageTag("en-u-co-emoji"));
```

However, use of the emoji will supplant the language's customizations. So the above is the equivalent of:

```java
collator = Collator.getInstance(ULocale.forLanguageTag("und-u-co-emoji"));
```

The same structure will not work for a language that does require customization, like Danish. That is, the following will fail.

```java
collator = Collator.getInstance(ULocale.forLanguageTag("da-u-co-emoji"));
```

For that, a slightly more cumbersome method needs to be employed, which is to take the rules for Danish, and explicitly add the rules for emoji.

```java
RuleBasedCollator collator = new RuleBasedCollator(
((RuleBasedCollator) Collator.getInstance(ULocale.forLanguageTag("da"))).getRules() +
((RuleBasedCollator) Collator.getInstance(ULocale.forLanguageTag("und-u-co-emoji")))
.getRules());
```

The following table shows the differences. When emoji ordering is supported, the two faces will be adjacent. When Danish ordering is supported, the ü is after the y.

<!-- HTML: no header row, jagged -->
<table>
<tbody>
<tr><td>code point order</td><td>,</td><td>Z</td><td>a</td><td>y</td><td>ü</td><td>☹️</td><td>✈️️</td><td>글</td><td>😀</td></tr>
<tr><td>en</td><td>,</td><td>☹️</td><td>✈️️</td><td>😀</td><td>a</td><td>ü</td><td>y</td><td>Z</td><td>글</td></tr>
<tr><td>en-u-co-emoji</td><td>,</td><td>😀</td><td>☹️</td><td>✈️️</td><td>a</td><td>ü</td><td>y</td><td>Z</td><td>글</td></tr>
<tr><td>da</td><td>,</td><td>☹️</td><td>✈️️</td><td>😀</td><td>a</td><td>y</td><td><strong><u>ü</u></strong></td><td>Z</td><td>글</td></tr>
<tr><td>da-u-co-emoji</td><td>,</td><td>😀</td><td>☹️</td><td>✈️️</td><td>a</td><td><strong><u>ü</u></strong></td><td>y</td><td>Z</td><td>글</td></tr>
<tr><td>combined rules</td><td>,</td><td>😀</td><td>☹️</td><td>✈️️</td><td>a</td><td>y</td><td><strong><u>ü</u></strong></td><td>Z</td><td>글</td></tr>
</tbody>
</table>


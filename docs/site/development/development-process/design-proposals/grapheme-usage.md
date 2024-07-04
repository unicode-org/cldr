---
title: Grapheme Usage
---

# Grapheme Usage

*Draft*

The goal is to allow the use of the appropriate grapheme clusters for given tasks, for a given language. See http://unicode.org/cldr/trac/ticket/2142. *Please leave any feedback as comments on that ticket.*

The idea is that we have explicit boundaries that represent certain common behaviors (codepoint breaks, or legacy grapheme cluster breaks), and we also have associations for a given language between a particular *function* and the explicit boundaries that should be used in that language for that function.

Here is a proposal for the structure in LDML:

\<characters>

...

 \<grapheme-usage type="count">**extended**\</grapheme-usage> \<!-- when counting 'user characters' -->

 \<grapheme-usage type="drop-cap">**legacy**\</grapheme-usage> \<!-- paragraph drop-caps -->

 \<grapheme-usage type="selection">**aksara**\</grapheme-usage> \<!-- selection boundaries: highlighting, keyboard arrows, cut&paste -->

 \<grapheme-usage type="backspace">**codepoint**\</grapheme-usage> \<!-- delete previous character -->

 \<grapheme-usage type="delete">**extended**\</grapheme-usage> \<!-- delete next character -->

...

\</characters>

*The above would be tailorable per locale.*

In segments/root.xml we have GraphemeClusterBreak. We interpret that as extended grapheme clusters for compatibility. We then add rules for:

- LegacyGraphemeClusterBreak // as per UAX#29
- AksaraGraphemeClusterBreak // the virama character connects extended clusters
- CodepointGraphemeClusterBreak // constant, trivial, probably usually implemented in code
- ExemplarGraphemeClusterBreak // uses the CLDR exemplar set in addition to extended clusters.

*These would also be tailorable per locale (except CodePoint), but should be more rarely done.*

Clients like ICU would add new constants for getting BreakIterators (or equivalents). These would be both corresponding to the new explicit rules:

- legacy
- extended = 'user-character'
- aksara
- codepoint
- exemplar

And to the new 'function-based' breaks:

- character\_count
- character\_drop\_cap
- character\_selection
- character\_backspace
- character\_delete

## Related bugs

- #[2142](http://unicode.org/cldr/trac/ticket/2142), Alternate Grapheme Clusters (pedberg, 2.0)
- #[2975](http://unicode.org/cldr/trac/ticket/2975), Support legacy grapheme break (pedberg, 2.0)
- #[2825](http://unicode.org/cldr/trac/ticket/2825), Add aksha grapheme break (pedberg, 2.0)
- #[2992](http://unicode.org/cldr/trac/ticket/2992), Grapheme Clusters or a new break type - TR29 vs TR18? [about language-specific treatment of digraphs as clusters - ]
- #[2406](http://unicode.org/cldr/trac/ticket/2406), Add locale keywords to specify the type (or variant) of word & grapheme break (pedberg, 2.0)
- There is also the suggestion to add another type which is beyond the scope of CLDR - a cluster type that treats ligatures as single clusters. This depends on font behavior.

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
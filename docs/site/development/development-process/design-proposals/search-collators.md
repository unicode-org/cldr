---
title: Search collators
---

# Search collators

|   |   |
|---|---|
| Author | Peter Edberg |
| Date | 2010-04-06 |
| Status | Proposal |
| Bugs | [2182](http://unicode.org/cldr/trac/ticket/2182), [2160](http://unicode.org/cldr/trac/ticket/2160), [2257](http://unicode.org/cldr/trac/ticket/2257) |

## Background

In April-May 2009, I filed the following tickets related to the addition of locale-specific "search" collators, intended for use with ICU's usearch functions, that would typically make a few more characters equivalent at the primary level:

- [2182](http://unicode.org/cldr/trac/ticket/2182) Generic "search" style for collator (filed by pedberg → assigned to pedberg)
- [2160](http://unicode.org/cldr/trac/ticket/2160) Arabic collation variant for search (filed by pedberg → assigned to pedberg)

In related CLDR meeting discussion, it was suggested that perhaps continuing to focus on collators for search was not the best approach, and that for search one could use something simpler than a collator – that is, we could develop a new type of object that does not provide order information (only equivalence), that may not need information at levels below secondary, etc. But then in June 2009 the following bug was filed, suggesting that perhaps we continue to see collators as the best available current solution for search:

- [2257](http://unicode.org/cldr/trac/ticket/2257) Add Collation "search" variant for Korean (filed by mark → assigned to pedberg)

Thinking about this some more (and discussing with others at Apple), what we really need is a default search collator at the root level, so any language-specific collators can just provide any necessary language-specific deltas, but will otherwise provide proper search behavior in other scripts.

## Proposal Notes

1. Add a new type "search" for the co/collation key.
	1. Normally, for a new collation type, we would add in the "en" locale (and eventually others), an element something like \<localeDisplayNames>/\<types>/\<type type="search" key="collation">Search Sort Order\</type>; however, this "search" collation is not necessarily something that should appear in a list of normal collation types. Solution from mtg 2010-04-07: For "en" use something like "General-Purpose Search" that does not include "Sort Order".
2. Provide a "search" collation variant at the root level. This will have the modified primary equivalences (and perhaps other modified equivalences) for Arabic, Japanese, etc.
	1. It also needs to turn off the swapping of prepended Thai and Lao vowels that is the normal behavior of collators. Note from mtg 2010-04-07: Recently this swapping behavior has been in the UCA data, not code, so the search collator can just change the data to achieve the desired result.
3. Some locales (e.g. Turkish) may need specific search collators. But hopefully, most of the required default search behavior for scripts can be expressed in the root search collator, eliminating the need for most language-specific search collators.
4. For inheritance, if a locale does not supply a "search" collation variant, we want to look for a "search" variant in the locale's parent, and on up to root, rather than using any other collation variant in the locale. I think this is how key/type inheritance already works. Note from mtg 2010-04-07: That is what is supposed to happen, but need to test to verify this.
5. Even if we do change in the future to use a different type of object for search, it can use this data; it looks like this proposal involves data changes only.

## Examples

- The normal Polish collator makes primary-level distinctions among certain accented characters and vs the baseform without accent. For search we do not want these distinctions; we want something like default UCA behavior for Latin letters. If Polish does not provide a search collator, then requesting a search variant for Polish collation will fall back to the root search collator, which would provide the desired search behavior for Polish.
- (more to be provided)

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
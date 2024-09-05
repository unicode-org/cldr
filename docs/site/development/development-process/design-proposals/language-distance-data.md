---
title: Language Distance Data
---

# Language Distance Data

The purpose is to provide a way to match language/locales according to "closeness" rather than just a truncation algorithm. and to allow for the user to specify multiple acceptable languages. The data is designed to allow for an algorithm that can account for the closeness of the relations between, say, tl and fil, or en-US and en-CA. This is based on code that we already use, but expanded to be more data-driven.

For example, if I understand written American English, German, French, Swiss German, and Italian, and the product has {ja-JP, de, zh-TW}, then de would be the best match; if I understand only {zh}, then zh-TW would be the best match. This represents a superset of the capabilities of locale fallback. Stated in those terms, it can have the effect of a more complex fallback, such as:

sr-Cyrl-RS

sr-Cyrl

sr-Latn-RS

sr-Latn

sr

hr-Latn

hr

Note that the goal, as with the rest of CLDR, is for matching written languages. Should we find in the future that it is also important to support spoken language matching in the same way, variant weights could be supplied.

This is related to the current aliasing mechanism, which is used to equate he and iw, for example. It is used to find the best locale ID for a given request, but does not interact with the fallback of resources *within the locale-parent chain.* It subsumes and replaces the current \<fallback> element (we'd take the current information in those elements and apply it).

## Expected Input

1. a weighted list of desired languages D (like AcceptLanguage)
2. a weighted list of available languages A (eg supported languages)

In the examples, the weights are given in AcceptLanguage syntax, eg ";" + number in (0.0 to 1.0). The weight 0.0 means don't match at all. Unlike AcceptLanguage, however, the relations among variants like "en" and "en-CA" are taken into account.

In very many cases, the weights will all be identical (1.0). Some exceptions might be:

- For desired languages, to indicate a preference. For example, I happen to prefer English to German to French to Swiss German to Italian. So the desired list for me might be {"en-US;q=1", "de;q=0.9", "fr;q=0.85", "gsw;q=0.8", "it;q=0.6"}
- For available languages, it can be used to indicate the "quality" of the user experience. Thus if it is known that the German version of a product or site is quite good, but the Danish is substandard, that could be reflected in the weightings. In most cases, however, the available language weights would be the same.

## Expected Output

1. A "best fit" language from A
2. A measure of how good the fit is

## Examples

Input:

desired: {"en-CA;q=1", "fr;q=1"}

available: {"en-GB;q=1", "en-US;q=1"}

threshold: script

Output:

en-US

good

Input:

desired: {"en-ZA;q=1", "fr;q=1"}

available: {"en-GB;q=1", "en-US;q=1", "fr-CA;q=0.9"}

threshold: script

Output:

en-GB

good

Input:

desired: {"de"}

available: {"en-GB;q=1", "en-US;q=1", "fr-CA;q=0.9"}

threshold: script

Output:

en-GB

bad

## Internals

The following is a logical expression of how this data can be used.

The lists are processed, with each Q value being inverted (x = 1/x) to derive a weight. There is a small progressive cost as well, so {x;q=1 y;q=1} turns into x;w=0 y;w=0.0001. Because AcceptLanguage is fatally underspecified, we also have to normalize the Q values.

For each pair (d,a) in D and A:

The base distance between d and a is computed by canonicalizing both languages and maximizing, using likely subtags, then computing the following.

baseDistance = diff(d.language, a.language) + diff(d.script, a.script) + diff(d.region, a.region) + diff(d.variants, a.variants)

There is also a small distance allotted for the maximization. That is, "en-Latn-CA" vs "en-Latn-CA" where the second "Latn" was added by maximization, will have a non-zero distance. Variants are handled as a sorted set, and the distance is variantDistance \* (count(variants1-variants2) + count(variants2-variants1)). As yet, there is no distance for extensions, but that may come in the future.

We then compute:

weight(d,a) = weight(d) \* weight(a) \* baseDistance(d,a)

The weight of each a is then computed as the min(weight(d,a)) for all d. The a with the smallest such weight is the winner. The "goodness" of the match is given as a scale from 0.0(perfect) to 1.0 (awful). Constants are provided for a Script-only difference and a Region-only difference, for comparison.

If, however, the winning language has too low a threshold, then the default locale (first in the available languages list) is returned.

Note that the distance metric is *not* symmetric: the distance from zh to yue may be different than the distance from yue to zh. That happens when it is more likely that a reader of yue would understand zh than the reverse.

Note that this doesn't have to be an N x M algorithm. Because there is a minimum threshold (otherwise returning the default locale), we can precompute the possible base language subtags that could be returned; anything else can be discarded.

## Data Sample

The data is designed to be relatively simple to understand. It would typically be processed into an internal format for fast processing. The data does not need to be exact; only the relative computed values are important. However, for keep the types of fields apart, they are given very different values. TODO: add values for [ISO 636 Deprecation Requests - DRAFT](https://cldr.unicode.org/development/development-process/design-proposals/iso-636-deprecation-requests-draft)

\<languageDistances> 

\<!-- Essentially synonyms. Note that true synonyms like he/iw are handled by default below. -->

\<distance desired="tl" available="fil">8\</distance>

\<distance desired="no" available="nb">1\</distance>

\<distance desired="ro-MO" available="mo">1\</distance>

\<!-- Scandanavian. Remember that we focus on written form -->

\<distance desired="nn" available="no">64\</distance>

\<distance desired="nn" available="nb">64\</distance>

\<distance desired="da" available="no">96\</distance>

\<distance desired="da" available="nb">96\</distance>

\<distance desired="da" available="nn">128\</distance>

\<!-- All the Serbo-Croatian variants are like regional variants -->

\<distance desired="hr" available="bs">64\</distance>

\<distance desired="sh" available="bs">64\</distance>

\<distance desired="sr" available="bs">64\</distance>

\<distance desired="sh" available="hr">64\</distance>

\<distance desired="sr" available="hr">64\</distance>

\<distance desired="sh" available="sr">64\</distance>

\<!-- Chinese scripts -->

\<distance desired="und-Hant" available="und-Hans">128\</distance>

\<!-- English: US and Canada are close; everything else closer to GB -->

\<distance desired="en-Zzzz-155" available="en-Zzzz-155">8\</distance> \<!-- Expand to cover the Americas --> 

\<distance desired="en-Zzzz-155" available="en-Zzzz-ZZ">64\</distance> \<!-- They aren't close to GB -->

\<distance desired="en-Zzzz-ZZ" available="en-Zzzz-ZZ">8\</distance> \<!-- All others are closer to GB, and each other -->

\<!-- default distances.

&emsp;Must be last!

&emsp;Note that deprecated differences in the alias file are given a weight of 1,

&emsp;and before this point. -->

\<distance desired="und" available="\*">1024\</distance> \<!-- default language distance -->

\<distance desired="und-Zzzz" available="\*">256\</distance> \<!-- default script distance -->

\<distance desired="und-Zzzz-ZZ" available="\*">64\</distance> \<!-- default region distance -->

\<distance desired="und-Zzzz-ZZ-UNKNOWN" available="\*">16\</distance> \<!-- default variant distance -->

\<languageDistances> 

## Interpreting the Format

1. The list is ordered, so the first match for a given type wins. That is, logically, you walk through the list looking for language matches. At the first one, you record the distance. Then you walk though for script differences, and so on.
2. The attributes desired and available both take language tags, and are assumed to be maximized for matching.
3. The Unknown subtags (und, Zzzz, ZZ, UNKNOWN) match any subtag of the same type. Trailing unknown values can be omitted. "\*" is a special value, used for the default distances. The macro regions (eg, 019 = Americas) match any region in them. So und-155 matches any language in Western Europe (155).
	1. As we expand, we may find out that we want more expressive power, like regex.
4. The attribute oneWay="true" indicates that the distance is only one direction.

Issues

- Should we have the values be symbolic rather than literal numbers? eg: L, S, R, ... instead of 1024, 256, 64,...
- The "\*" is a bit of a hack. Other thoughts for syntax?

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
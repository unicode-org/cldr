---
title: Picking the Right Language Identifier
---

# Picking the Right Language Identifier

Within programs and structured data, languages are indicated with stable identifiers of the form [en](http://unicode.org/cldr/utility/languageid.jsp?a=en), [fr\-CA](http://unicode.org/cldr/utility/languageid.jsp?a=fr-CA), or [zh\-Hant](http://unicode.org/cldr/utility/languageid.jsp?a=zh-Hant&l=en). The standard Unicode language identifiers follow IETF BCP 47, with some small differences defined in [UTS \#35: Locale Data Markup Language (LDML)](http://www.unicode.org/reports/tr35/). Locale identifiers use the same format, with certain possible extensions.

Often it is not clear which language identifier to use. For example, what most people call Punjabi in Pakistan actually has the code '[lah](http://unicode.org/cldr/utility/languageid.jsp?a=lah)', and formal name "Lahnda". There are many other cases where the same name is used for different languages, or where the name that people search for is not listed in the IANA registry. Moreover, a language identifier uses not only the 'base' language code, like '[en](http://unicode.org/cldr/utility/languageid.jsp?a=en)' for English or '[ku](http://unicode.org/cldr/utility/languageid.jsp?a=ku)' for Kurdish, but also certain modifiers such as [en\-CA](http://unicode.org/cldr/utility/languageid.jsp?a=en-CA) for *Canadian English*, or [ku\-Latn](http://ku-Latn) for *Kurdish written in Latin script*. Each of these modifiers are called *subtags* (or sometimes *codes*), and are separated by "\-" or "\_". The language identifier itself is also called a *language tag*, and sometimes a *language code*.

Here is an example of the steps to take to find the right language identifier to use. Let's say you to find the identifier for a language called "Ganda" which you know is spoken in Uganda. You'll first pick the base language subtag as described below, then add any necessary script/territory subtags, and then verify. If you can't find the name after following these steps or have other questions, ask on the [Unicode CLDR Mailing List](http://www.unicode.org/consortium/distlist.html#cldr_list).

If you are looking at a prospective language code, like "swh", the process is similar; follow the steps below, starting with the verification.

## Choosing the Base Language Code

1. Go to [iso639\-3](http://www-01.sil.org/iso639-3/codes.asp) to find the language. Typically you'll look under **Name** starting with **G** for Ganda.
2. There may be multiple entries for the item you want, so you'll need to look at all of them. For example, on the page for names starting with “P”, there are three records: “Panjabi”, “Mirpur Panjabi” and “Western Panjabi” (it is the last of these that corresponds to Lahnda). You can also try a search, but be [careful](https://cldr.unicode.org/index/cldr-spec/picking-the-right-language-code).
3. You'll find an entry like:

&emsp;lug&emsp; lug&emsp; **lg**&emsp; Ganda&emsp; Individual&emsp; Living&emsp; more ...

While you may think that you are done, you have to verify that the three\-letter code is correct.

1. Click on the "more..." in this case and you'll find [id\=lug](http://www.sil.org/iso639-3/documentation.asp?id=lug). You can also use the URL http://www.sil.org/iso639\-3/documentation.asp?id\=XXX, where you replace XXX by the three\-letter code.
2. Click on "See corresponding entry in [Ethnologue](http://www.ethnologue.com/show_language.asp?code=lug)." and you get to [code\=lug](http://www.ethnologue.com/show_language.asp?code=lug)
3. Verify that is indeed the language:
	1. Look at the information on the ethnologue page
	2. Check Wikipedia and other web sources
4. ***AND IMPORTANTLY: Review [Caution!](https://cldr.unicode.org/index/cldr-spec/picking-the-right-language-code) below***

Once you have the right three\-letter code, you are still not done. Unicode (and BCP 47\) uses the 2 letter ISO code if it exists. Unicode also uses the "macro language" where suitable. *So*

1. Use the two\-letter code if there is one. In the example above, the highlighted "lg" from the first table.
2. Verify that the code is in http://www.iana.org/assignments/language-subtag-registry
3. If the code occurs in http://unicode.org/repos/cldr/trunk/common/supplemental/supplementalMetadata.xml in the type attribute of a languageAlias element, then use the replacement instead.
	- For example, because "swh" occurs in \<languageAlias type\="swh" replacement\="sw"/\>, "sw" must be used instead of "swh".

## Choosing Script/Territory Subtags

If you need a particular variant of a language, then you'll add additional subtags, typically script or territory. Consult [Sample Subtags](http://unicode.org/cldr/utility/sample_subtags.html) for the most common choices. ***Again, review*** [***Caution!***](https://cldr.unicode.org/index/cldr-spec/picking-the-right-language-code) ***below.***

## Verifying Your Choice

1. Verify your choice by using the [online language identifier](http://unicode.org/cldr/utility/languageid.jsp) demo.
2. You need to fix the identifier and try again in *any* if the demo shows any of the following:
	1. the language identifer is illegal, or
	2. one of the subtags is invalid, or
	3. there are any replacement values. [\*\*](https://cldr.unicode.org/index/cldr-spec/picking-the-right-language-code)

## Documenting Your Choice

If you are requesting a new locale / language in CLDR, please include the links to the particular pages above so that we can process your request more quickly, as we have to double check before any addition. The links will be of the form:

- http://www.sil.org/iso639-3/documentation.asp?id=xxx
- http://www.ethnologue.com/show_language.asp?code=xxx
- http://en.wikipedia.org/wiki/Western_Punjabi
- and so on

## Caution!

### Canonical Form

Unicode language and locale IDs are based on BCP 47, but differ in a few ways. The canonical form is produced by using the canonicalization based on BCP47 (thus changing iw → he, and zh\-yue → yue), plus a few other steps:

1. Replacing the most prominent encompassed subtag by the macrolanguage (cmn → zh)
2. Canonicalizing overlong 3 letter codes (eng\-840 → en\-US)
3. Minimizing according to the likely subtag data (ru\-Cyrl → ru, en\-US → en).
4. BCP 47 also provides for "variant subtags", such as [zh\-Latn\-pinyin](http://unicode.org/cldr/utility/languageid.jsp?a=zh-Latn-pinyin). When there are multiple variant subtags, the canonical format for Unicode language identifiers puts them in alphabetical order.

Note that the CLDR likely subtag data is used to minimize scripts and regions, *not* the IANA Suppress\-Script. The latter had a much more constrained design goal, and is more limited.

In some cases, systems (or companies) may have different conventions than the Preferred\-Values in BCP 47 \-\- such as those in the Replacement column in the the [online language identifier](http://unicode.org/cldr/utility/languageid.jsp) demo. For example, for backwards compatibility, "iw" is used with Java instead of "he" (Hebrew). When picking the right subtags, be aware of these compatibility issues. *If a target system uses a different canonical form for locale IDs than CLDR, the CLDR data needs to be processed by remapping its IDs to the target system's.*

For compatibility, it is strongly recommended that all implementations accept both the preferred values and their alternates: for example, both "iw" and "he". Although BCP 47 itself only allows "\-" as a separator; for compatibility, Unicode language identifiers allows both "\-" and "\_". Implementations should also accept both.

### Macrolanguages

ISO (and hence BCP 47\) has the notion of an individual language (like en \= English) versus a Collection or Macrolanguage. For compatibility, Unicode language and locale identifiers always use the Macrolanguage to identify the predominant form. Thus the Macrolanguage subtag "zh" (Chinese) is used instead of "cmn" (Mandarin). Similarly, suppose that you are looking for Kurdish written in Latin letters, as in Turkey. It is a mistake to think that because that is in the north, that you should use the subtag 'kmr' for Northern Kurdish. You should instead use [ku\-Latn\-TR](http://ku-latn/). See also: [ISO 636 Deprecation Requests](https://cldr.unicode.org/development/development-process/design-proposals/iso-636-deprecation-requests-draft).

Unicode language identifiers do not allow the "extlang" form defined in BCP 47\. For example, use "yue" instead of "zh\-yue" for Cantonese.

### Ethnologue

*When searching, such as* [*site:ethnologue.com ganda*](http://www.google.com/search?q=site%3Aethnologue.com+ganda)*, be sure to completely disregard matches in* [*Ethnologue 14*](http://www.ethnologue.com/14/) *\-\- these are out of date, and do not have the right codes!*

The Ethnologue is a great source of information, but it must be approached with a certain degree of caution. Many of the population figures are far out of date, or not well substantiated. The Ethnologue also focus on native, spoken languages, whereas CLDR and many other systems are focused on written language, for computer UI and document translation, and on fluent speakers (not necessarily native speakers). So, for example, it would be a mistake to look at http://www.ethnologue.com/show_country.asp?name=EG and conclude that the right language subtag for the Arabic used in Egypt was "arz", which has the largest population. Instead, the right code is "ar", Standard Arabic, which would be the one used for document and UI translation.

### Wikipedia

Wikipedia is also a great source of information, but it must be approached with a certain degree of caution as well. Be sure to follow up on references, not just look at articles.

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
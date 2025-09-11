---
title: Picking the Right Language Identifier
---

# Picking the Right Language Identifier

Within programs and structured data, languages are indicated with stable identifiers of the form '[en](http://unicode.org/cldr/utility/languageid.jsp?a=en)', '[fr\-CA](http://unicode.org/cldr/utility/languageid.jsp?a=fr-CA)', or '[zh\-Hant](http://unicode.org/cldr/utility/languageid.jsp?a=zh-Hant&l=en)'. The standard Unicode language identifiers follow IETF BCP 47, with some small differences defined in [UTS \#35: Locale Data Markup Language (LDML)](http://www.unicode.org/reports/tr35/). Locale identifiers use the same format, with certain possible extensions.

Often it is not clear which language identifier to use. For example, what most people call Punjabi in Pakistan actually has the code '[lah](http://unicode.org/cldr/utility/languageid.jsp?a=lah)', and formal name "Lahnda". There are many other cases where the same name is used for different languages, or where the name that people search for is not listed in the IANA registry. Moreover, a language identifier uses not only the 'base' language code, like '[en](http://unicode.org/cldr/utility/languageid.jsp?a=en)' for English or '[ku](http://unicode.org/cldr/utility/languageid.jsp?a=ku)' for Kurdish, but also certain modifiers such as '[en\-CA](http://unicode.org/cldr/utility/languageid.jsp?a=en-CA)' for *Canadian English*, or '[ku\-Latn](http://ku-Latn)' for *Kurdish written in Latin script*. Each of these modifiers are called *subtags* (or sometimes *codes*), and are separated by "\-" or "\_". The language identifier itself is also called a *language tag*, and sometimes a *language code*.

Here is an example of the steps to take to find the right language identifier to use. Let's say you want to find the identifier for a language called "Ganda" which you know is spoken in Uganda. You'll first pick the base language subtag as described below, then add any necessary script/territory subtags, and then verify. If you can't find the name after following these steps or have other questions, ask on the [Unicode CLDR Mailing List](http://www.unicode.org/consortium/distlist.html#cldr_list).

If you are looking at a prospective language code, like 'swh', the process is similar; follow the steps below, starting with the verification.

## Choosing the Base Language Code

1. Go to [iso639\-3](https://iso639-3.sil.org/code_tables/639/data) to find the language. In the "Language Name(s) containing" field enter the language name (**Ganda** in this example) and select "Apply".
2. There may be multiple entries for the item you want, so you'll need to examine them carefully.
3. You'll find an entry like:

&emsp;lug&emsp; lug&emsp; **lg**&emsp; Ganda&emsp; Individual&emsp; Living

While you may think that you are done, you have to verify that the three\-letter code is correct.

1. Click on the item in the first column ('[lug](https://iso639-3.sil.org/code/lug)' in this case) and you'll find links to Ethnologue, Glottolog and Wikipedia.
2. Click on the [Ethnologue](http://www.ethnologue.com/language/lug) link and you get to the corresponding Ethnologue entry.
3. Verify that is indeed the language:
	1. Look at the information on the Ethnologue page
	2. Check Glottolog, Wikipedia and other web sources
4. ***AND IMPORTANTLY: Review "[Caution](#caution)" section below***

Once you have the right three\-letter code, you are still not done. Unicode and BCP 47 use the two\-letter ISO code if it exists. Unicode also uses the "macro language" where suitable. *So:*


1. Use the two\-letter code if there is one. In the example above, use the highlighted 'lg' from the first table.
2. Verify that the code is in the [IANA subtag registry](http://www.iana.org/assignments/language-subtag-registry)
3. If the code occurs in the [supplementalMetadata.xml](http://github.com/unicode-org/cldr/blob/main/common/supplemental/supplementalMetadata.xml) file in the type attribute of a languageAlias element, then use the replacement instead.
	- For example, because 'swh' occurs in `<languageAlias type="swh" replacement="sw"/>`, 'sw' must be used instead of 'swh'.


## Choosing Script/Territory Subtags

If you need a particular variant of a language, then you'll add additional subtags, typically script or territory. Consult [Sample Subtags](http://unicode.org/cldr/utility/sample_subtags.html) for the most common choices. ***Again, review "[Caution](#caution)" section below.***

## Verifying Your Choice

1. Verify your choice by using the [online language identifier](http://unicode.org/cldr/utility/languageid.jsp) demo.
2. You need to fix the identifier and try again in *any* if the demo shows any of the following:
	1. the language identifer is illegal, or
	2. one of the subtags is invalid, or
	3. there are any replacement values.

## Documenting Your Choice

If you are requesting a new locale / language in CLDR, please include the links to the particular pages above so that we can process your request more quickly, as we have to double-check the information before making the addition. The links will be of the form:


- https://iso639-3.sil.org/code/xxx
- https://www.ethnologue.com/language/xxx
- https://en.wikipedia.org/wiki/xxx
- and so on

## Caution

### Canonical Form

Unicode language and locale IDs are based on BCP 47, but differ in a few ways. The canonical form is produced by using the canonicalization based on BCP 47 (thus changing 'iw' → 'he', and 'zh\-yue' → 'yue'), plus a few other steps:

1. Replacing the most prominent encompassed subtag by the macrolanguage ('cmn' → 'zh')
2. Canonicalizing overlong 3 letter codes ('eng\-840' → 'en\-US')
3. Minimizing according to the likely subtag data ('ru\-Cyrl' → 'ru', 'en\-US' → 'en').
4. BCP 47 also provides for "variant subtags", such as '[zh\-Latn\-pinyin](http://unicode.org/cldr/utility/languageid.jsp?a=zh-Latn-pinyin)'. When there are multiple variant subtags, the canonical format for Unicode language identifiers puts them in alphabetical order.

Note that the CLDR likely subtag data is used to minimize scripts and regions, *not* the IANA Suppress\-Script. The latter had a much more constrained design goal and is more limited.


In some cases, systems (or companies) may have different conventions than the Preferred\-Values in BCP 47 \-\- such as those in the Replacement column in the the [online language identifier](http://unicode.org/cldr/utility/languageid.jsp) demo. For example, for backwards compatibility, 'iw' is used with Java instead of 'he' (Hebrew). Be aware of these compatibility issues when picking the subtags. *If a target system uses a different canonical form for locale IDs than CLDR, the CLDR data needs to be processed by remapping its IDs to those of the target system.*

For compatibility, it is strongly recommended that all implementations accept both the preferred values and their alternates: for example, both 'iw' and 'he'. Although BCP 47 itself only allows "\-" as a separator, Unicode language identifiers allow both "\-" and "\_" for compatibility reasons. Implementations should also accept both.

### Macrolanguages

ISO (and hence BCP 47\) has the notion of an individual language (like 'en' \= English) versus a Collection or Macrolanguage. For compatibility, Unicode language and locale identifiers always use the Macrolanguage to identify the predominant form. Thus the Macrolanguage subtag 'zh' (Chinese) is used instead of 'cmn' (Mandarin). Similarly, suppose that you are looking for Kurdish written in Latin letters, as in Turkey. It is a mistake to think that because that is in the north, that you should use the subtag 'kmr' for Northern Kurdish. You should instead use 'ku\-Latn\-TR'. See also: [ISO 639 Deprecation Requests](/development/development-process/design-proposals/iso-639-deprecation-requests-draft).

Unicode language identifiers do not allow the "extlang" form defined in BCP 47\. For example, use 'yue' instead of 'zh\-yue' for Cantonese.

### Ethnologue

The Ethnologue is a great source of information, but it must be approached with a certain degree of caution. Many of the population figures are out of date or not well substantiated. The Ethnologue also focus on native, spoken languages, whereas CLDR and many other systems are focused on written language, for computer UI and document translation, and on fluent speakers (not necessarily native speakers). So, for example, it would be a mistake to look at the [Ethnologue entry for Egypt](https://www.ethnologue.com/country/EG/) and conclude that the right language subtag for the Arabic used in Egypt is 'arz', which has the largest population. Instead, the right code is 'ar', Standard Arabic, which would be the one used for document and UI translation.


### Wikipedia

Wikipedia is also a great source of information, but it must be approached with a certain degree of caution as well. Be sure to follow up on the linked references, not just look at articles.









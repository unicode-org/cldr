---
title: Transform keywords
---

# Transform keywords

There is now an internet draft being developed. See http://tools.ietf.org/html/draft-davis-t-langtag-ext

**OLDER DRAFT**

We often need a language/locale identifier to indicate the source of transformed (transliterated or translated) content. For example, for Map data, the names of Italian or Russian cities need to be represented in Katakana for Japanese users.

It is important to be able to indicate not only the resulting content language, but also the source. Transforms such as transliteration may vary depending not only on the basis of the source and target script, but also language. Thus the Russian "Пу́тин" transliterates into "Putin" in English but "Poutine" in French. The identifier may be used to indicate a desired mechanical transliteration in an API, or it may be used to tag data that has been converted (mechanically or by hand) according to a transliteration method.

The specification uses the BCP47 extension 't' and the Unicode extension key "ts", such as in the following examples:

|   |   |
|---|---|
| ja-Kana-**t-it-u-ts-ungegn-2007** | the content is transliterated from Italian to Katakana (Japanese) according to the corresponding UNGEGN transliteration dated 2007 |
| und-Kana-**t-und-cyrl** | the content source was Cyrillic, translated or transliterated to Katakana, but the mechanism was not known (or not specified) |
| en-**t-fr-u-ts-mech** | the content was mechanically translated from French to English; the mechanism is unspecified |

The extension **t** indicates a source for the transformed content (transliterated or translated). It takes any Unicode language identifier, thus a subset of the registered BCP47 language tags:

- lang (-script)? (-region)? (-variant)\*

For script transliteration, or for specialized transform variants, the language tag 'und' is used. For example, a general romanization will have the language tag 'und-Latn'. The language tag 'und' is also used where the source or target are "Any" currently in CLDR. A new section of the LDML specification describes this tag.

The Unicode extension key **tm** is a keyword specifying the mechanism for the transform, where that is useful or necessary. It takes N subtypes, in order, that represent the transliteration variant. As usual, the subtypes will be listed in the bcp47 subdirectory of CLDR, adding a description field and optional aliases. Initially, registered are common transliteration standards like ISO15915, KMOCT, ... UNGEGN, ... , and specialized variants. See http://www.unicode.org/reports/tr35/#Transforms.

Any final subtype of 4, 6, or 8 digits represents a date in the format yyyy(MM(dd)?)?, such as 2010, or 201009, or 20100930. So, for example, und-Latn-t-und-hebr-tm-ungegn-2007 represents the transliteration as described in http://www.eki.ee/wgrs/rom1\_he.htm. The date should only be used where necessary, and if present only be as specific as necessary. So if the only dated variants for the given mechanism, source, and result are 1977 and 2007, the month and day in 2007 should not be present.

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
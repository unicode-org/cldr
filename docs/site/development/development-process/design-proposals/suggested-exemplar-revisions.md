---
title: Suggested Exemplar Revisions
---

# Suggested Exemplar Revisions

I've been doing some analysis of character frequency, and on that basis have the following recommendations for changes to the exemplar characters.

As a reminder, the **main** exemplar characters are those that are required for native words in modern customary use. For example, "a-z" suffice for English. The **aux** exemplar characters include other characters that would not be unexpected in common non-technical publications: those that are native but not required (eg, *ö* as in *coöperate*, or "pronunced *dāvĭs*"), foreign loan words (eg, *résumé*). [A useful source of the aux letter are newspaper style guidelines.]

There is a breakdown on http://www.unicode.org/repos/cldr-tmp/trunk/dropbox/mark/exemplars/summary.html. Note the following:

- Characters (more precisely, combining character sequences) are given in rough frequency order, in boxes colored according to the relationship to the CLDR exemplar sets.
- Some of the changes below have already been incorporated (marked in italic). Note that the characters inside the boxes are not ordered by frequency, but the boxes are (rightmost containing more frequent characters).
- There is some noise in the system, so don't give too much weight to characters towards the left (they are all trimmed to no more than 1000 characters), or for languages without much presence on the web.
- The characters are partially normalized (width, Arabic shapes, NFC).
- The characters are from a sample of the web, about 800M docs, and 5T characters.

Here are my suggestions. Please send feedback to [mark@macchiato.com](mailto:mark@macchiato.com) with any other suggestions, or add comments to http://unicode.org/cldr/trac/ticket/2789.

### Suggestions

1. For all Latin-script languages, add [a-z] into the aux set. *For others, check that any Latin script characters are deliberate, and either include all of a-z or none (I found in Tamil: \<exemplarCharacters type="auxiliary">[a g i m t]\</exemplarCharacters>)*
2. zh shows the following as high-frequency characters but not in exemplars 网 机 产 册 没 只 帖 万 ... . Consider adding to main. There are some other high frequency characters in aux, that probably should be in main: 线 录 户
3. I collected some draft info on languages\* we don't have, expressed in code at the end of this document. Consider adding locales to at least encompass this information.
4. ja doesn't include the following, should probably be in aux: 岡 阪 奈 藤 俺 伊 誰...
5. es should have ª in main, check that aux covers (French/German/Danish/Port.)
6. de *should have ß in main*, check aux covers French/Spanish/Danish/Turkish
7. pt should have ª º in main, French/Spanish/German/Danish in main
8. ko should have jamo in aux, and: 中 人 北 大 女 完 文 日 本 的 美 語...
9. it should check French/German/Danish in aux
10. In general, we should see which languages follow the convention of using trema to separate digraph vowels (eg naïve), and add the 6 vowels with trema to aux, at least.
11. in aux should cover French/Dutch
12. tr aux should cover French/German
13. zh-Hant: should look at 只 帖 搜 壇 ..
14. nl aux should cover French/Spanish/German/Danish
15. pl aux should cover French/German
16. fil aux should cover Spanish/French
17. qu\* aux should cover Spanish
18. hu aux should cover German
19. el aux should cover polytonic greek
20. fi aux should cover French, German
21. We should include non-Western decimal digits into the corresponding exemplars
22. fa aux should include Arabic; ar aux should include Persian
23. da aux should cover French/German/Spanish
24. ca aux should cover Spanish
25. All Cyrillic aux should cover Russian
26. eu (Basque) aux should cover Spanish/French
27. ku-Arab aux should cover Arabic/Persian
28. br (Breton) aux should cover French

### Additional exemplar sets

- qu - Quechua [pt{ch}kq{pʼ}{tʼ}{chʼ}{kʼ}{qʼ}{ph}{th}{chh}{kh}{qh}s{sh}hmnjl{ll}rwyñaiu]
- co - Corsican [abc{chj}defg{ghj}hijlmnopqrstuvz]
- fy - West Frisian [a b c d e f g h i y j k l m n o p q r s t u v w x zâ ê é ô û ú]
- bho - Bhojpuri [:sc=deva:]
- gd - Scottish Gaelic [abcdefghilmnoprstuàèìòù], aux: [á é ó]
- ht - Haitian Creole [a{an}b{ch}de{en}èfgijklmnoò{on}{ou}prst{tch}vwyz]
- jv - Javanese [a b c d e é è f g h i j k l m n o p q r s t u v w x y z]
- la - Latin [abcdefghiklmnopqrstuxyz]
- lb - Luxembourgish "[a-z é ä ë]
- sd - Sindhi [ا ب ٻ پ ڀ ت ث ٺ ٽ ٿ ج ڃ ڄ چ ڇ ح-ذ ڊ ڌ ڍ ڏ ر ز ڙ س-غ ف ڦ ق ک ڪ گ ڱ ڳ ل-ن ڻ ه ھ و ي]
- su - Sundanese [aeiouépbtdkgcjh{ng}{ny}mnswlry]
- gn - Guaraní = gug [a-vx-zá é í ó ú ý ñ ã ẽ ĩ õ ũ ỹ {g\u0303}]

From Bug 1947, for reference.

The exemplar character set for ja appears to be too small. 

1. It contains about 2,000 characters (Kanji, Hiragana and Katakana). 
2. If Exemplar Character set is limited to the most widely used one (Level 1
Kanji? in JIS X 208), I expected Auxiliary Exemplar Character set to contain the

rest of 

JIS X 0208 (plus JIS X 212 / 213). However, it contains only 5 characters. 

3. It does not contain \<U+30F7, U+30FA> ('composed Katakana letters'), U+30FB and U+30FC (conjunction and length marks).

For instance, characters like U+4EDD,U+66D9, U+7DBE are not included although they're used in Japanese IDN names (which is an indicator that they're pretty widely used. See <http://code.google.com/p/chromium/issues/detail?id=3158> ) 

While I was at it, I also looked at zh\* and ko. All of them have about 2000 characters (in case of 2350 which is the number of Hangul syllables in KS X 1001). The auxiliary sets for zh\* have only tens of characters (26 for zh\_Hans
and 33 for zh\_Hant). 

It's rather inconvenient to type hundreds (if not thousands) of characters in the CLDR survey tool. Perhaps, we have to fill in those values ('candidate sets' for vetting) using cvs before the next round of CLDR survey. 

...

Jungshik and I discussed this, and there are three possible sources (for each of Chinese (S+T), Japanese, and Korean) that we could tie the exemplars to:

1. charsets (in the case of Japanese, this would be probably: JIS 208 + 212 + 213. (This would be a large set, and 
contain many rarely-used characters).<br /><br />
1a. Only use JIS 208. (The current approach appears to be JIS 208, but only level 1.)

2. Use the educational standards in each country/territory for primary+secondary requirements. We'd have to 
look up sources for these.

3. Use the NIC restrictions for each country.

These would all overlap to a large degree, but wouldn't be the same. One possibility is to issue a PRI for public review.

There is a fourth possibility: Use the characters that are supported by the commonly-used fonts on various

platforms for these languages (e.g. the characters that are in the cmaps for [TrueType?](#BAD_URL) fonts).

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
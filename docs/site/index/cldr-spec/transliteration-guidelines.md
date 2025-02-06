---
title: Unicode Transliteration Guidelines
---

# Unicode Transliteration Guidelines

## Introduction

*This document describes guidelines for the creation and use of CLDR transliterations. Please file any feedback on this document or those charts at [Locale Bugs](/requesting_changes).*

Transliteration is the general process of converting characters from one script to another, where the result is roughly phonetic for languages in the target script. For example, "Phobos" and "Deimos" are transliterations of Greek mythological "Φόβος" and "Δεῖμος" into Latin letters, used to name the moons of Mars.

Transliteration is *not* translation. Rather, transliteration is the conversion of letters from one script to another without translating the underlying words. The following shows a sample of transliteration systems:

Sample Transliteration Systems

| Source | Translation | Transliteration | System |
|:---:|:---:|:---:|:---:|
| Αλφαβητικός | Alphabetic | Alphabētikós | Classic |
|  |   | Alfavi̱tikós | UNGEGN |
| しんばし | new bridge (district in Tokyo) | shimbashi | Hepburn |
|  |  | sinbasi | Kunrei |
| яйца Фаберже | Fabergé eggs | yaytsa Faberzhe | BGN/PCGN |
|  |  | jajca Faberže | Scholarly |
|  |  | âjca Faberže | ISO |

***Display**. Some of the characters in this document may not be visible in your browser, and with some fonts the diacritics will not be correctly placed on the base letters. See [Display Problems](http://www.unicode.org/help/display_problems.html).*

While an English speaker may not recognize that the Japanese word kyanpasu is equivalent to the English word campus, the word kyanpasu is still far easier to recognize and interpret than if the letters were left in the original script. There are several situations where this transliteration is especially useful, such as the following. See the sidebar for examples.

- When a user views names that are entered in a world\-wide database, it is extremely helpful to view and refer to the names in the user's native script.
- When the user performs searching and indexing tasks, transliteration can retrieve information in a different script.
- When a service engineer is sent a program dump that is filled with characters from foreign scripts, it is much easier to diagnose the problem when the text is transliterated and the service engineer can recognize the characters.

Sample Transliterations

| Source | Transliteration |
|---|---|
| 김, 국삼 | Gim, Gugsam |
| 김, 명희 | Gim, Myeonghyi |
| 정, 병호 | Jeong, Byeongho |
| ... | ... |
| たけだ, まさゆき | Takeda, Masayuki |
| ますだ, よしひこ | Masuda, Yoshihiko |
| やまもと, のぼる | Yamamoto, Noboru |
| ... | ... |
| Ρούτση, Άννα | Roútsē, Ánna |
| Καλούδης, Χρήστος | Kaloúdēs, Chrḗstos |
| Θεοδωράτου, Ελένη | Theodōrátou, Elénē |

The term *transliteration* is sometimes given a narrow meaning, implying that the transformation is *reversible* (sometimes called *lossless*). In CLDR this is not the case; the term *transliteration* is interpreted broadly to mean both reversible and non\-reversible transforms of text. (Note that even if theoretically a transliteration system is supposed to be reversible, in source standards it is often not specified in sufficient detail in the edge cases to actually be reversible.) A non\-reversible transliteration is often called a *transcription*, or called a *lossy* or *ambiguous* transcription.

Note that reversibility is generally only in one direction, so a transliteration from a native script to Latin may be reversible, but not the other way around. For example, Hangul is reversible, in that any Hangul to Latin to Hangul should provide the same Hangul as the input. Thus we have the following:

&emsp;갗 → gach → 갗

However, for completeness, many Latin characters have fallbacks. This means that more than one Latin character may map to the same Hangul. Thus from Latin we don't have reversibility, because two different Latin source strings round\-trip back to the same Latin string.

&emsp;gach → 갗 → gach

&emsp;gac → 갗 → gach

Transliteration can also be used to convert unfamiliar letters within the same script, such as converting Icelandic THORN (þ) to th. These are not typically reversible.

&emsp;*There is an online demo using released CLDR data at [ICU Transform Demo](https://icu4c-demos.unicode.org/icu-bin/translit).*

## Variants

There are many systems for transliteration between languages: the same text can be transliterated in many different ways. For example, for the Greek example above, the transliteration is classical, while the [UNGEGN](https://arhiiv.eki.ee/wgrs/) alternate has different correspondences, such as φ → f instead of φ → ph.

CLDR provides for generic mappings from script to script (such as Cyrillic\-Latin), and also language\-specific variants (Russian\-French, or Serbian\-German). There can also be semi\-generic mappings, such as Russian\-Latin or Cyrillic\-French. These can be referred to, respectively, as script transliterations, language\-specific transliterations, or script\-language transliterations. Transliterations from other scripts to Latin are also called *Romanizations*.

Even within particular languages, there can be variant systems according to different authorities, or even varying across time (if the authority for a system changes its recommendation). The canonical identifier that CLDR uses for these has the form:

&emsp;*source\-target/variant*

The source (and target) can be a language or script, either using the English name or a locale code. The variant should specify the authority for the system, and if necessary for disambiguation, the year. For example, the identifier for the Russian to Latin transliteration according to the UNGEGN system would be:

- ru\-und\_Latn/UNGEGN, or
- Russian\-Latin/UNGEGN

If there were multiple versions of these over time, the variant would be, say, UNGEGN2006\.

The assumption is that implementations will allow the use of fallbacks, if the exact transliteration specified is unavailable. For example, the following would be the fallback chain for the identifier Russian\-English/UNGEGN. This is similar to the *Lookup Fallback Pattern* used in [BCP 47 Tags for Identifying Languages](https://www.rfc-editor.org/info/bcp47), except that it uses a "stepladder approach" to progressively handle the fallback among source, target, and variant, with priorities being the target, source, and variant, in that order.

- Russian\-English/UNGEGN
- Russian\-English
- Cyrillic\-English/UNGEGN
- Cyrillic\-English
- Russian\-Latin/UNGEGN
- Russian\-Latin
- Cyrillic\-Latin/UNGEGN
- Cyrillic\-Latin

## Guidelines

There are a number of generally desirable guidelines for script transliterations. These guidelines are rarely satisfied simultaneously, so constructing a reasonable transliteration is always a process of balancing different requirements. These requirements are most important for people who are building transliterations, but are also useful as background information for users.

The following lists the general guidelines for Unicode CLDR transliterations:

- *standard*: follow established systems (standards, authorities, or de facto practice) where possible, deviating sometimes where necessary for reversibility. In CLDR, the systems are generally described in the comments in the XML data files found in the in the [transforms](https://github.com/unicode-org/cldr/tree/main/common/transforms) folder online. For example, the system for Arabic transliteration in CLDR are found in the comments in [Arabic\-Latin.xml](https://github.com/unicode-org/cldr/blob/main/common/transforms/Arabic-Latin.xml); there is a reference to the [UNGEGN Arabic Tables](https://arhiiv.eki.ee/wgrs/rom1_ar.pdf). Similarly for Hebrew, which also follows the [Hebrew UNGEGN Tables](https://arhiiv.eki.ee/wgrs/rom1_he.pdf).
- *complete*: every well\-formed sequence of characters in the source script should transliterate to a sequence of characters from the target script, and vice versa.
- *predictable*: the letters themselves (without any knowledge of the languages written in that script) should be sufficient for the transliteration, based on a relatively small number of rules. This allows the transliteration to be performed mechanically.
- *pronounceable*: the resulting characters have reasonable pronunciations in the target script. Transliteration is not as useful if the process simply maps the characters without any regard to their pronunciation. Simply mapping by alphabetic order ("αβγδεζηθ..." to "abcdefgh...") could yield strings that might be complete and unambiguous, but the pronunciation would be completely unexpected.
- *reversible*: it is possible to recover the text in the source script from the transliteration in the target script. That is, someone that knows the transliteration rules would be able to recover the precise spelling of the original source text. For example, it is possible to go from *Elláda* back to the original Ελλάδα, while if the transliteration were *Ellada* (with no accent), it would not be possible.

Some of these principles may not be achievable simultaneously; in particular, adherence to a standard system *and* reversibility. Often small changes in existing systems can be made to accommodate reversibility. However, where a particular system specifies a fundamentally non\-reversible transliterations, those transliterations as represented in CLDR may not be reversible.

### Ambiguity

In transliteration, multiple characters may produce ambiguities (non\-reversible mappings) unless the rules are carefully designed. For example, the Greek character PSI (ψ) maps to ps, but ps could also result from the sequence PI, SIGMA (πσ) since PI (π) maps to p and SIGMA (σ) maps to s.

The Japanese transliteration standards provide a good mechanism for handling these kinds of ambiguities. Using the Japanese transliteration standards, whenever an ambiguous sequence in the target script does not result from a single letter, the transform uses an apostrophe to disambiguate it. For example, it uses that procedure to distinguish between *man'ichi* and *manichi*. Using this procedure, the Greek character PI SIGMA (πσ) maps to p's. This method is recommended for all script transliteration methods, although sometimes the character may vary: for example, "\-" is used in Korean.

**Note**: We've had a recent proposal to consistently use the hyphenation dot for this code, thus we'd have πσ → p‧s.

A second problem is that some characters in a target script are not normally found outside of certain contexts. For example, the small Japanese "ya" character, as in "kya" (キャ), is not normally found in isolation. To handle such characters, the Unicode transliterations currently use different conventions.

- Tilde: "ャ" in isolation is represented as "\~ya"
- Diacritics: Greek "ς" in isolation is represented as s̱

**Note**: The CLDR committee is considering converging on a common representation for this. The advantage of a common representation is that it allows for easy filtering.

For the default script transforms, the goal is to have unambiguous mappings, with variants for any common use mappings that are ambiguous (non\-reversible). In some cases, however, case may not be preserved. For example,

| Latin | Greek | Latin |
|:---:|:---:|:---:|
| ps PS | ψ Ψ | ps PS |
| psa Psa **PsA** | ψα Ψα **ΨΑ** | psa Psa **PSA** |
| psA PSA **PSa** | ψΑ ΨΑ **Ψα** | psA PSA **Psa** |

The following shows Greek text that is mapped to fully reversible Latin:

| **Greek\-Latin** |  |
|---|---|
| τί φῄς; γραφὴν σέ τις, ὡς ἔοικε, γέγραπται: οὐ γὰρ ἐκεῖνό γε καταγνώσομαι, ὡς σὺ ἕτερον. | tí phḗis; graphḕn sé tis, hōs éoike, gégraptai: ou gàr ekeînó ge katagnṓsomai, hōs sỳ héteron. |

If the user wants a version without certain accents, then CLDR's chaining rules can be used to remove the accents. For example, the following transliterates to Latin but removes the macron accents on the long vowels.

| **Greek\-Latin; nfd; \[\\u0304] remove; nfc** |  |
|---|---|
| τί φῄς; γραφὴν σέ τις, ὡς ἔοικε, γέγραπται: οὐ γὰρ ἐκεῖνό γε καταγνώσομαι, ὡς σὺ ἕτερον. | tí phéis; graphèn sé tis, hos éoike, gégraptai: ou gàr ekeînó ge katagnósomai, hos sỳ héteron. |

The above chaining rules, separated by semi\-colons, perform the following commands in order:

| Rule | Description |
|---|---|
| Greek-Latin | transliterate Greek to Latin |
| nfd | convert to Unicode NFD format (separating accents from base characters) |
| [\u0304] remove | remove accents, but filter the command to only apply to a single character: [U+0304](http://unicode.org/cldr/utility/character.jsp?a=0304) ( ̄ ) COMBINING MACRON |
| nfc | convert to Unicode NFC format (rejoining accents to base characters) |

The following transliterates to Latin but removes *all* accents. Note that the only change is to expand the filter for the remove command.

| **Greek\-Latin; nfd; \[:nonspacing marks:] remove; nfc** |  |
|---|---|
| τί φῄς; γραφὴν σέ τις, ὡς ἔοικε, γέγραπται: οὐ γὰρ ἐκεῖνό γε καταγνώσομαι, ὡς σὺ ἕτερον. | ti pheis; graphen se tis, hos eoike, gegraptai: ou gar ekeino ge katagnosomai, hos sy heteron. |

### Pronunciation

Standard transliteration methods often do not follow the pronunciation rules of any particular language in the target script. For example, the Japanese Hepburn system uses a "j" that has the English phonetic value (as opposed to French, German, or Spanish), but uses vowels that do not have the standard English sounds. A transliteration method might also require some special knowledge to have the correct pronunciation. For example, in the Japanese kunrei\-siki system, "ti" is pronounced as English "chee".

This is similar to situations where there are different languages within the same script. For example, knowing that the word *Gewalt* comes from German allows a knowledgeable reader to pronounce the "w" as a "v".  When encountering a foreign word like *jawa*, there is little assurance how it is to be pronounced even when it is not a transliteration (it is just from /span\>another Latin\-script language). The *j* could be pronounced (for an English speaker) as in *jump*, or *Junker*, or *jour*; and so on. Transcriptions are only roughly phonetic, and only so when the specific pronunciation rules are understood.

The pronunciation of the characters in the original script may also be influenced by context, which may be particularly misleading in transliteration. For, in the Bengali নিঃশব, transliterated as niḥśaba, the *visarga ḥ* is not pronounced itself (whereas elsewhere it may be) but lengthens the ś sound, and the final inherent *a* is pronounced (whereas it commonly is not), and the two inherent a's are pronounced as ɔ and ô, respectively.

In some cases, transliteration may be heavily influenced by tradition. For example, the modern Greek letter beta (β) sounds like a "v", but a transliteration may use a b (as in biology). In that case, the user would need to know that a "b" in the transliterated word corresponded to beta (β) and is to be pronounced as a v in modern Greek.

Letters may also be transliterated differently according to their context to make the pronunciation more predictable. For example, since the Greek sequence GAMMA GAMMA (γγ) is pronounced as *ng*, the first GAMMA can be transcribed as an "n" in that context. Similarly, the transliteration can give other guidance to the pronunciation in the source language, for example, using "n" or "m" for the same Japanese character (ん) depending on context, even though there is no distinction in the source script.

In general, predictability means that when transliterating Latin script to other scripts using reversible transliterations, English text will not produce phonetic results. This is because the pronunciation of English cannot be predicted easily from the letters in a word: e.g. *grove*, *move*, and *love* all end with "ove", but are pronounced very differently.

### Cautions

Reversibility may require modifications of traditional transcription methods. For example, there are two standard methods for transliterating Japanese katakana and hiragana into Latin letters. The *kunrei\-siki* method is unambiguous. The Hepburn method can be more easily pronounced by foreigners but is ambiguous. In the Hepburn method, both ZI (ジ) and DI (ヂ) are represented by "ji" and both ZU (ズ) and DU (ヅ) are represented by "zu". A slightly amended version of Hepburn, that uses "dji" for DI and "dzu" for DU, is unambiguous.

When a sequence of two letters map to one, case mappings (uppercase and lowercase) must be handled carefully to ensure reversibility. For cased scripts, the two letters may need to have different cases, depending on the next letter. For example, the Greek letter PHI (Φ) maps to PH in Latin, but Φο maps to Pho, and not to PHo.

Some scripts have characters that take on different shapes depending on their context. Usually, this is done at the display level (such as with Arabic) and does not require special transliteration support. However, in a few cases this is represented with different character codes, such as in Greek and Hebrew. For example, a Greek SIGMA is written in a final form (ς) at the end of words, and a non\-final form (σ) in other locations. This also requires the transform to map different characters based on the context.

Another thing to look out for when dealing with cased scripts is that some of the characters in the target script may not be able to represent case distinctions, such as some of the IPA characters in the Latin script.

It is useful for the reverse mapping to be complete so that arbitrary strings in the target script can be reasonably mapped back to the source script. Complete reverse mapping makes it much easier to do mechanical quality checks and so on. For example, even though the letter "q" might not be necessary in a transliteration of Greek, it can be mapped to a KAPPA (κ). Such reverse mappings will not, in general, be unambiguous.

## Available Transliterations

Currently Unicode CLDR offers Romanizations for certain scripts, plus transliterations between the Indic scripts (excluding Urdu). Additional script transliterations will be added in the future.

Except where otherwise noted, all of these systems are designed to be reversible. For bicameral scripts (those with uppercase and lowercase), however, case may not be completely preserved.

The transliterations are also designed to be complete for any sequence of the Latin letters a\-z. A fallback is used for a letter that is not covered by the transliteration, and default letters may be inserted as required. For example, in the Hangul transliteration, rink → 린크 → linkeu. That is, "r" is mapped to the closest other letter, and a default vowel is inserted at the end (since "nk" cannot end a syllable).

*Preliminary [charts](http://www.unicode.org/cldr/data/charts/transforms/index.html) are available for the available transliterations. Be sure to read the known issues described there.*

### Korean

There are many Romanizations of Korean. The default transliteration in Unicode CLDR follows the [Korean Ministry of Culture \& Tourism Transliteration](http://www.korean.go.kr/06_new/rule/rule06.jsp) regulations (see also [English summary](https://web.archive.org/web/20070916025652/http://www.korea.net/korea/kor_loca.asp?code=A020303)). There is an optional clause 8 variant for reversibility:

"제 8 항 학술 연구 논문 등 특수 분야에서 한글 복원을 전제로 표기할 경우에는 한글 표기를 대상으로 적는다. 이때 글자 대응은 제2장을 따르되 'ㄱ, ㄷ, ㅂ, ㄹ'은 'g, d, b, l'로만 적는다. 음가 없는 'ㅇ'은 붙임표(\-)로 표기하되 어두에서는 생략하는 것을 원칙으로 한다. 기타 분절의 필요가 있을 때에도 붙임표(\-)를 쓴다."

*translation*: "Clause 8: When it is required to recover the original Hangul representation faithfully as in scholarly articles, ' ㄱ, ㄷ, ㅂ, ㄹ' must be always romanized as 'g, d, b, l' while the mapping for the rest of the letters remains the same as specified in clause 2\. The placeholder 'ㅇ' at the beginning of a syllable should be represented with '\-', but should be omitted at the beginning of a word. In addition, '\-' should be used in other cases where a syllable boundary needs to be explicitly marked (be disambiguated."

There are a number of cases where this Romanization may be ambiguous, because sometimes multiple Latin letters map to a single entity (jamo) in Hangul. This happens with vowels and consonants, the latter being slightly more complicated because there are both initial and final consonants:

| Type | Multi-Character Consonants |
|---|---|
| Initial-Only | tt pp jj |
| Initial-or-Final | kk ch ss |
| Final-Only | gs nj nh lg lm lb ls lt lp lh bs ng |

CLDR uses the following rules for disambiguation of the possible boundaries between letters, in order. The first rule comes from Clause 8\.

1. Don't break so as to require an implicit vowel or null consonant (if possible)
2. Don't break within Initial\-Only or Initial\-Or\-Final sequences (if possible)
3. Favor longest match first.

If there is a single consonant between vowels, then Rule \#1 will group it with the following vowel if there is one (this is the same as the first part of Clause 8\). If there is a sequence of four consonants between vowels, then there is only one possible break (with well\-formed text). So the only ambiguities lie with two or three consonants between vowels, where there are possible multi\-character consonants involved. Even there, in most cases the resolution is simple, because there isn't a possible multi\-character consonant in the case of two, or two possible multi\-character consonants in the case of 3\. For example, in the following cases, the left side is unambiguous:

&emsp;angda \= ang\-da → 앙다

&emsp;apda \= ap\-da → 앞다

There are a relatively small number of possible ambiguities, listed below using "a" as a sample vowel.

| No. of Cons. | Latin | CLDR Disambiguation | Hangul | Comments |  |
|---|---|---|---|---|---|
| 2 | atta | = a-tta | 아따 | Rule 1, then 2 |  |
|  | appa | = a-ppa | 아빠 |  |  |
|  | ajja | = a-jja | 아짜 |  |  |
|  | akka | = a-kka | 아까 | Rule 1, then 2 |  |
|  | assa | = a-ssa | 아싸 |  |  |
|  | acha | = a-cha | 아차 |  |  |
|  | agsa | = ag-sa | 악사 | Rule 1 |  |
|  | anja | = an-ja | 안자 |  |  |
|  | anha | = an-ha | 안하 |  |  |
|  | alga | = al-ga | 알가 |  |  |
|  | alma | = al-ma | 알마 |  |  |
|  | alba | = al-ba | 알바 |  |  |
|  | alsa | = al-sa | 알사 |  |  |
|  | alta | = al-ta | 알타 |  |  |
|  | alpa | = al-pa | 알파 |  |  |
|  | alha | = al-ha | 알하 |  |  |
|  | absa | = ab-sa | 압사 |  |  |
|  | anga | = an-ga | 안가 |  |  |
| 3 | agssa | = ag-ssa | 악싸 | Rule 1, then 2 |  |
|  | anjja | = an-jja | 안짜 |  |  |
|  | alssa | = al-ssa | 알싸 |  |  |
|  | altta | = al-tta | 알따 |  |  |
|  | alppa | = al-ppa | 알빠 |  |  |
|  | abssa | = ab-ssa | 압싸 |  |  |
|  | akkka | = akk-ka | 앆카 | Rule 1, then 2, then 3 |  |
|  | asssa | = ass-sa | 았사 |  |  |

For vowel sequences, the situation is simpler. Only Rule \#3 applies, so aeo \= ae\-o → 애오.

### Japanese

The default transliteration for Japanese uses the a slight variant of the Hepburn system. With Hepburn system, both ZI (ジ) and DI (ヂ) are represented by "ji" and both ZU (ズ) and DU (ヅ) are represented by "zu". This is amended slightly for reversibility by using "dji" for DI and "dzu" for DU.

### Greek

The default transliteration uses a standard transcription for Greek which is aimed at preserving etymology. The ISO 843 variant includes following differences:

| Greek | Default | ISO 843 |
|---|---|---|
| β | b | v |
| γ* | n | g |
| η | ē | ī |
| ̔ | h | (omitted) |
| ̀ | ̀ | (omitted) |
| ~ | ~ | (omitted) |

\* before γ, κ, ξ, χ

### Cyrillic

Cyrillic generally follows ISO 9 for the base Cyrillic set. There are tentative plans to add extended Cyrillic characters in the future, plus variants for GOST and other national standards.

### Indic

Transliteration of Indic scripts follows the ISO 15919 *Transliteration of Devanagari and related Indic scripts into Latin characters*. Internally, all Indic scripts are transliterated by converting first to an internal form, called Inter\-Indic, then from Inter\-Indic to the target script. Inter\-Indic thus provides a pivot between the different scripts, and contains a superset of correspondences for all of them.

ISO 15919 differs from ISCII 91 in application of diacritics for certain characters. These differences are shown in the following example (illustrated with Devanagari, although the same principles apply to the other Indic scripts):

| Devanagari | ISCII 91 | ISO 15919 |
|---|---|---|
| ऋ | ṛ | r̥ |
| ऌ | ḻ | l̥ |
| ॠ | ṝ | r̥̄  |
| ॡ | ḻ̄ | l̥̄  |
| ढ़ | d̂ha | ṛha |
| ड़ | d̂a | ṛa |

Transliteration rules from Indic to Latin are reversible with the exception of the ZWJ and ZWNJ used to request explicit rendering effects. For example:

| Devanagari | Romanization | Note |
|---|---|---|
| क्ष | kṣa | normal |
| क्‍ष  | kṣa | explicit halant requested |
| क्‌ष  | kṣa | half-consonant requested |

Transliteration between Indic scripts are roundtrip where there are corresponding letters. Otherwise, there may be fallbacks.

There are two particular instances where transliterations may produce unexpected results: (1\) where the final vowel is suppressed in speech, and (2\) with the transliteration of 'c'.

For example:

| Devanagari | Romanization | Notes |
|---|---|---|
| सेन्गुप्त    | Sēngupta |   |
| सेनगुप्त   | Sēnagupta | The final 'a' is not pronounced |
| मोनिक | Monika |   |
| मोनिच | Monica | The 'c' is pronounced "ch" |

### Others

Unicode CLDR provides other transliterations based on the [U.S. Board on Geographic Names](https://www.usgs.gov/us-board-on-geographic-names) (BGN) transliterations. These are currently unidirectional — to Latin only. The goal is to make them bidirectional in future versions of CLDR.

Other transliterations are generally based on the [UNGEGN: Working Group on Romanization Systems](https://arhiiv.eki.ee/wgrs/) transliterations. These systems are in wider actual implementation than most ISO standardized transliterations, and are published freely available on the web (<http://www.eki.ee/wgrs/>) and thus easily accessible to all. The UNGEGN also has good documentation. For example, the [UNGEGN Arabic Tables](https://arhiiv.eki.ee/wgrs/rom1_ar.pdf) not only presents the UN system, but compares it with the BGN/PCGN 1956 system, the I.G.N. System 1973, ISO 233:1984, the royal Jordanian Geographic Centre System, and the Survey of Egypt System.

## Submitting Transliterations

If you are interested in providing transliterations for one or more scripts, file an initial bug report at [*Locale Bugs*](http://www.unicode.org/cldr/bugs/locale-bugs). The initial bug should contain the scripts and or languages involved, and the system being followed (with a link to a full description of the proposed transliteration system), and a brief example. The proposed data can also be in that bug, or be added in a Reply to that bug. You can also file a bug in [*Locale Bugs*](http://www.unicode.org/cldr/bugs/locale-bugs) if you find a problem in an existing transliteration.

For submission to CLDR, the data needs to supplied in the correct XML format or in the ICU format, and should follow an accepted standard (like UNGEGN, BGN, or others).

- The format for rules is specified in [Transform\_Rules](https://www.unicode.org/reports/tr35/#Transform_Rules). It is best if the results are tested using the [ICU Transform Demo](https://icu4c-demos.unicode.org/icu-bin/translit) first, since if the data doesn't validate it would not be accepted into CLDR.
- As mentioned above, even if a transliteration is only used in certain countries or contexts CLDR can provide for them with different variant tags.
- For comparison, you can see what is currently in CLDR in the [transforms]() folder online. For example, see [Hebrew\-Latin.xml]().
- Script transliterators should cover every character in the exemplar sets for the CLDR locales using that script.
- Romanizations (Script\-Latin) should cover all the ASCII letters (some of these can be fallback mappings, such as the 'x' below).
- If the rules are very simple, they can be supplied in a spreadsheet, with two columns, such as

| Shavian |  Relation | Latin | Comments |
|:---:|:---:|:---:|---|
| &#x10450; | ↔ | p | Map all uppercase to lowercase first |
| &#x1045a; | ↔ | b |   |
| &#x10451; | ↔ | t |   |
| &#x10452;&#x10455; | ← | x | fallback |
|  ... |   |   |   |

## More Information

For more information, see:

- BGN: [U.S. Board on Geographic Names](https://www.usgs.gov/us-board-on-geographic-names)
- UNGEGN: [UNITED NATIONS GROUP OF EXPERTS ON GEOGRAPHICAL NAMES: Working Group on Romanization Systems](https://arhiiv.eki.ee/wgrs/)
- [Transliteration of Non\-Roman Alphabets and Scripts (Thomas T. Pedersen)](http://transliteration.eki.ee/)
- [Standards for Archival Description: Romanization](http://www.archivists.org/catalog/stds99/chapter8.html)
- [ISO\-15915 (Hindi)](http://transliteration.eki.ee/pdf/Hindi-Marathi-Nepali.pdf)
- [ISO\-15915 (Gujarati)](http://transliteration.eki.ee/pdf/Gujarati.pdf)
- [ISO\-15915 (Kannada)](http://transliteration.eki.ee/pdf/Kannada.pdf)
- [ISCII\-91](http://www.cdacindia.com/html/gist/down/iscii_d.asp)
- [UTS \#35: Locale Data Markup Language (LDML)](https://www.unicode.org/reports/tr35/)


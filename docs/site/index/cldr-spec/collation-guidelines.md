---
title: Collation Guidelines
---

# Collation Guidelines

Collation sequences can be quite tricky to specify.

The locale\-based collation rules in Unicode CLDR specify customizations of the standard data for [UTS \#10: Unicode Collation Algorithm](https://www.unicode.org/reports/tr10/#Introduction) (UCA). Requests to change the collation order for a given locale, or to supply additional variants, need to follow the guidelines in this document.

## Filing a Request

Requests to change the collation order for a given locale, or to supply additional variants should be reported by [requesting changes](/requesting_changes).

### Rules

The request should present the precise change expressed as rules. The rules must be supplied in the syntax as specified in [https://www.unicode.org/reports/tr35/tr35\-collation.html\#Rules](https://www.unicode.org/reports/tr35/tr35-collation.html#Rules). (This used to be called the "basic syntax".) The rules must also be [Minimal Rules](#minimal-rules) as described below: *only* differences from [https://www.unicode.org/charts/collation](https://www.unicode.org/charts/collation/) should be specified.

*\& c \< cs*

\& cs \<\<\< ccs / cs

Normally CLDR does not accept submissions that reorder *particular* digits, punctuation, or other symbols, following instead the UCA ordering for those characters. However, if punctuation, general symbols, currency symbols, or digits *as a class* all sort after letters, that change can be accommodated. Similarly, if the letters in a particular script sort ahead of others (such as Greek characters ahead of Latin), that can also be accommodated. Both of these are done with a reorder setting. Note: For a given language, CLDR normally sorts the language's native script before other scripts, via the reorder setting.

### Test Data

Please supply short test cases that illustrate the correct sorting behavior as a list of lines in sorted order. Try to include cases that show the boundary behavior by including suffixes, such as the following to illustrate that "cs" and "ccs" sort specially.

c

cy

cs

cscs

ccs

cscsy

ccsy

csy

d

### Justification

Provide justification for your change. Citations should be to authoritative pages on the web, in English.

### Testing Your Request

Please test out any suggested rules before filing a bug.

1. Go to the [ICU Collation Demo](https://demo.icu-project.org/icu-bin/collation.html).
2. Pick the language for which you want to change the rules, or keep it on "und" (root) if you want to start from the Unicode/CLDR default sort order.
3. Put your rules into the "Append rules" box.
4. Put an interesting list of strings into the Input box.
5. Click "sort" and verify the sort order and levels of differences.

Or

1. Go to the [ICU Locale Explorer](https://demo.icu-project.org/icu-bin/locexp).
2. Pick the appropriate locale.
3. Follow the instructions at the bottom to use your suggested rules on your suggested test data.
4. Verify that the proper order results.

## Determining the Order

The exact collation sequence for a given language may be difficult to determine. The base ordering of characters can be fairly straightforward, but there are quite a few other complications involved.

Most standards that specify collation, such as DIN or CS, are not targeted at algorithmic sorting, and are not complete algorithmic specifications. For example, CSN 97 6030 requires transliteration of foreign scripts, but there are many choices as to how to transliterate, and the exact mechanism is not specified. It also specifies that geometric shapes are sorted by the number of vertices and edges, which is, at a minimum, difficult to determine; and are subject to variation in glyphs.

The CLDR goals are to match the sorting of exemplar letters and common punctuation and leave everything else to the standard UCA ordering. For more information, see [UTS \#10: Unicode Collation Algorithm](https://www.unicode.org/reports/tr10/#Introduction) (UCA).

### Determining Level Differences

It is often tricky to determine the exact relationship between characters. In the UCA, case and similar variant differences are at a third (tertiary) level, while accent and similar differences are at a second (secondary) level, and base letter differences are at the first (primary) level. That results in an order like the following:

1. cina
2. Cina
3. çina
4. Çina
5. **d**ina

That is, the difference between c and C is weaker than the difference between c and ç, which in turn is weaker than the difference between c and d. For any two characters α and β, it may be very clear that α \< β, but not be clear what the right level difference is. To establish this, see if you can find examples of two words that of the following form.

Primary Test

1. ...α...Z
2. ...β...A

That is, the words are identical except for α, β, A, and Z, *and* you know that A and Z have a clear primary difference. If we get the above ordering in dictionaries and other sources, you know that the difference between α and β is a primary difference. If we get the opposite ordering than 1,2 above, then you only know that the difference between α and β is *not* a primary difference: it may be secondary or tertiary.

You now need to distinguish which of the non\-primary level differences you could have. So try again, this time seeing if you can find examples of two words that of the following form, where you know that A and Á have a clear secondary difference in the script.

Secondary Test1

1. ...α...Á
2. ...β...A

Now the ordering of these two strings tells you whether the difference between α and β is a secondary difference, or not. Alternatively, you can look for words of the form:

Secondary Test2

1. ...B...α
2. ...b...β

where b \< B at a tertiary level. If you get the above ordering for the secondary test2, you also know that the difference between α and β is at a secondary level. The Test2 form is often easier to find examples for.

If you have established that the characters have neither a primary nor secondary difference, the following can be used in a similar fashion to test whether the difference is at a tertiary level or not.

Tertiary Test

1. ...α...B
2. ...β...b

If there is no tertiary difference, then the difference is not significant enough for CLDR to take it into account, so they will be treated as equals (unless someone sorts with a final, codepoint level).

### Contractions

Characters may behave differently in different contexts. For example, "ch" in Slovak sorts after H. A sequence of characters that behaves that way is called a contraction. Another common case of contractions is in the case of syllabaries, where a sequence of characters forming a syllable collates as a unit.

Note that contractions are typically rather expensive in implementations: they take more storage, and are much slower to compare. So they should be avoided where possible. For example, suppose that we have the following sequence in a dictionary (where the uppercase characters represent characters in the target script):

KB

... // combinations of K with consonants

KZ

KA

KE

KI

KO

KU

LB

...

There are two ways to produce this ordering. One is to have KA, KE, KI, etc be contractions. The other is to order all the vowels after all the consonants. Where the latter is sufficient, it is strongly preferred.

## Minimal Rules

The goal is always specify the ***minimal*** differences from the DUCET. For example, take the case of Slovak, where everything sorts as in DUCET except for certain characters. The following rules place the characters ä, č, đ, and the sequence "ch" (and their case variants) at the appropriate positions in the sorting sequence, and with the appropriate strengths:

**Minimal Rules**

\& A

\< ä \<\<\< Ä

\& C

\< č \<\<\< Č

\& D

\< đ \<\<\< Đ

\& H

\< ch \<\<\< cH \<\<\< Ch \<\<\< CH

...

It would be possible instead to have rules that list every letter used by Slovak \[a á ä b c č d ď e é f\-h {ch} i í j\-l ĺ ľ m n ň o ó ô p\-r ŕ s š t ť u ú v\-y ý z ž], looking something like the following.

**Maximal Rules**

\& A \<\< á \<\<\< Á

\< ä \<\<\< Ä

\< b \<\<\< B

\< c \<\<\< C

\< č \<\<\< Č

\< d

...

***The Maximal Rules format is not accepted in CLDR.*** The reasons are:

1. Every time a character is tailored, the data for that character takes up more room in typical implementations. That means that the data for collation is larger, downloads of collation libraries with that data are slower, sort keys are longer, and performance is slower; sometimes very much so.
2. Related characters in the same script are in a peculiar order. For example, if the Slovak tailoring omits ƀ, then it would show up as after z.

You can see what the UCA currently does with a given script by looking at the charts at [Unicode Collation Charts](https://www.unicode.org/charts/collation/). For example, suppose that U\+0D89 SINHALA LETTER IYANNA and U\+0D8A SINHALA LETTER IIYANNA needed to come after U\+0D96 SINHALA LETTER AUYANNA, in primary order, and that otherwise DUCET was ok. Then you would give the following rules:

\& ඖ \# U\+0D96 SINHALA LETTER AUYANNA

\< ඉ \# U\+0D89 SINHALA LETTER IYANNA

\< ඊ \# U\+0D8A SINHALA LETTER IIYANNA

## Pitfalls

There are a number of pitfalls with collation, so be careful. In some cases, such as Hungarian or Japanese, the rules can be fairly complicated (of course, reflecting that the sorting sequence for those languages is complicated).

1. **Only tailor expected data.** We focus on the required collation sequence for a given language with normal data. So we don't include full\-width characters for a European collation sequence, such as
	- ... CSCS \<\<\< ＣＳＣＳ ...
	- ... CSCS \<\<\< \\uFF23\\uFF33\\uFF23\\uFF33 ... (equivalently)
2. **Tailor trailing contractions.** If a sequence of characters is treated as a unit for collation, it should be entered as a contraction.
	1. \& c \< ch
	2. One might think that sequence like "dz" doesn't require that, since it would always come after "d" followed by any other letter; it is a "trailing contraction". But in unusual cases, that wouldn't be true; if "dz" is a unit sorted as if it were a distinct letter after "d", one should get the ordering "dα" \< "dz". The correct behavior will only happen if "dz" is a contraction, such as
	3. \& d \< dz
3. **Watch out for Expansions.** If you have a rule like \&cs \< d, and "cs" has not occurred in a previous rule as a contraction, then this is automatically considered to be the same as \&c \< d / s; that is, the d *expands* as if it were a "cs" (actually, primary greater than a "cs", since we wrote "\<"). This expansion takes effect until the next primary difference.
	1. So suppose that "ccs" is to behave as if it were "cscs", and take case differences into account. You might try to do this with the rules on the left:

| Rules (Wrong) | Actual Effect |
|---|---|
| \& C \< cs \<\<\< Cs \<\<\< CS | \& C \< cs \<\<\< Cs \<\<\< CS |
| \& cscs \<\<\< ccs | **\& cs \<\<\< ccs / cs** |
| \<\<\< Cscs \<\<\< Ccs | **\<\<\< Cscs / cs \<\<\< Ccs / cs** |
| \<\<\< CSCS \<\<\< CCS | **\<\<\< CSCS / cs \<\<\< CCS / cs** |

1. But since the CSCS has not been made a contraction in previous rules, this produces an automatic expansion, one that continues through the entire sequence of non\-primary differences, as shown on the right. This is *not* what is wanted: each item acts like it expands compared to the previous item. So CCS, for example, will act like it expands to CSCScs!
2. What you actually want is the following:

| Rules (Right) | Actual Effect |
|---|---|
| \& C \< cs \<\<\< Cs \<\<\< CS | \& C \< cs \<\<\< Cs \<\<\< CS |
| \& cscs \<\<\< ccs | \& cs \<\<\< ccs / cs |
| \& Cscs \<\<\< Ccs | \& Cs \<\<\< Ccs / cs |
| \& CSCS \<\<\< CCS | \& CS \<\<\< CCS / CS |

1. In short, when you have expansions, it is always safer and clearer to express them with separate resets. There are only a few exceptions to this, notably when CJK characters are interleaved with Hangul Syllables.

1. **Minimal Rules.** Example: Maltese was sorting character sequences *before* a base character using the following style:
	1. \& B
	2. \< ċ
	3. \<\<\<Ċ
	4. \< c
	5. \<\<\<C
	6. The correct rules should be the minimal ones.
	7. \& \[before 1] c \< ċ \<\<\< Ċ
	8. This finds the highest primary (that's what the 1 is for) character less than c, and uses that as the reset point. For Maltese, the same technique needs to be used for ġ and ż.
2. **Blocking Contractions.** Contractions can be blocked with CGJ, as described in the Unicode Standard and in the [Characters and Combining Marks FAQ](https://www.unicode.org/faq/char_combmark.html).
3. **Case Combinations.** The lowercase, titlecase, and uppercase variants of contractions need to be supplied, with tertiary differences in that order (regardless of the caseFirst setting). That is, if *ch* is a contraction, then you would have the rules `... ch <<< Ch <<< CH`. Other case variants such as *cH* are excluded because they are unlikely to represent the contraction, for example in *McHugh*. (Therefore, *mchugh* and *McHugh* will be primary different if *ch* adds a primary difference.) \[[\#8248](https://unicode.org/cldr/trac/ticket/8248)]


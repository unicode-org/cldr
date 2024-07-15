---
title: Consistent Casing
---

# Consistent Casing

***Rough Draft***

We know that we need to improve the way we do casing in CLDR. We want the casing to be consistent, so that we don't see, for example, some language names with titlecase and some with lowercase.

## Current Status

We have the inText and inList items, but they are not consistently applied - and we haven't had tests for problems. Here is some text from [http://unicode.org/reports/tr35](http://unicode.org/reports/tr35) (I added notes in italic):

\<inList>

The following element controls whether display names (language, territory, etc) are title cased in GUI menu lists and the like. It is only used in languages where the normal display is lower case, but title case is used in lists. There are two options:

\<inList casing="titlecase-words">

\<inList casing="titlecase-firstword">

In both cases, the title case operation is the default title case function defined by Chapter 3 of *[*[*Unicode*](http://unicode.org/reports/tr35/#Unicode)*]*. In the second case, only the first word (using the word boundaries for that locale) will be title cased. The results can be fine-tuned by using alt="list" on any element where titlecasing as defined by the Unicode Standard will produce the wrong value. For example, suppose that "turc de Crimée" is a value, and the title case should be "Turc de Crimée". Then that can be expressed using the alt="list" value.

*Note: we have inList items currently for:*

*cs.xml*

*da.xml*

*es.xml*

*hr.xml*

*hu.xml*

*nl.xml*

*ro.xml*

*root.xml*

*ru.xml*

*sk.xml*

*uk.xml*

\<inText>

This element indicates the casing of the data in the category identified by the inText type attribute, when that data is written in text or how it would appear in a dictionary. For example :

\<inText type="languages">lowercase-words\</inText>

indicates that language names embedded in text are normally written in lower case. The possible values and their meanings are :

- titlecase-words : all words in the phrase should be title case
- titlecase-firstword : the first word should be title case
- lowercase-words : all words in the phrase should be lower case
- mixed : a mixture of upper and lower case is permitted. generally used when the correct value is unknown.

*Note: we have inText items currently in:*

*cs.xml*

*da.xml (20 matches)*

*es.xml (9 matches)*

*hr.xml (11 matches)*

*hu.xml (7 matches)*

*nl.xml (8 matches)*

*ro.xml (4 matches)*

*root.xml (13 matches)*

*uk.xml (6 matches)*

*For example, for Dutch we have (excluding draft items):*

*1,043: \<inText type="currency">lowercase-words\</inText>*

*1,045: \<inText type="languages">titlecase-firstword\</inText>*

*1,047: \<inText type="scripts">titlecase-firstword\</inText>*

*1,049: \<inText type="territories">titlecase-firstword\</inText>*

...

In certain circumstances, one or more elements do not follow the rule of the majority. as indicated by the inText element. In this case, the allow attribute is used:

The example below indicates that variant names are normally lower case with one exception.

\<inText type="languages">lowercase-words\</inText>

\<variants>

&emsp;\<variant type="1901">ortografia tradizionale tedesca\</variant>

&emsp;\<variant type="1996">ortografia tedesca del 1996\</variant>

&emsp;\<variant type="NEDIS" allow="verbatim">dialetto del Natisone\</variant>

\</variants>

## Improved Testing

As a part of bug http://www.unicode.org/cldr/bugs-private/locale-bugs-private/data?id=2227, I added a consistency test for casing. It just generates warnings for now, and the test is very simple: given a bucket of translations (eg language names), verify that everything have the same first-letter casing as the **first** item. Although simple (and not bulletproof!), it is revealing...

cs [Czech] warning names|language|lb 〈Luxembourgish〉 【】 〈Lucemburština〉 «=» 【】 Warning: First letter case of \<Lucemburština>=upper doesn't match that of \<afarština>=lower (names|language|aa).

cs [Czech] warning names|language|om 〈Oromo〉 【】 〈Oromo (Afan)〉 «=» 【】 Warning: First letter case of \<Oromo (Afan)>=upper doesn't match that of \<afarština>=lower (names|language|aa).

cs [Czech] warning names|language|ps 〈Pashto〉 【】 〈Pashto (Pushto)〉 «=» 【】 Warning: First letter case of \<Pashto (Pushto)>=upper doesn't match that of \<afarština>=lower (names|language|aa).

I didn't use the inText or inList data, because I don't think we have enough data, nor that it has been vetted enough, to be reliable. Moreover, I don't think the buckes it uses are fine-grained enough.. I put the test output in 3 different files in http://www.unicode.org/cldr/data/dropbox/casing/

The code is at http://www.unicode.org/cldr/data/tools/java/org/unicode/cldr/test/CheckConsistentCasing.java. Note that the buckets I used are defined in the code in typesICareAbout in the code.

## Feedback and Open Issues

It would be useful to get people's feedback on how the tests can be improved.

1. In particular, whether the "buckets" should be done differently. For example, it would reduce the warnings if we put the abbreviated format months in a different bucket than the wide format months. But I don't know whether it is right to suppress this warning, or whether it indicates a true problem.
	- az [Azerbaijani] warning calendar-gregorian|day|sunday:format-wide 〈Sunday〉 【】 〈bazar〉 «=» 【】 Warning: First letter case of \<bazar>=lower doesn't match that of \<B.>=upper (calendar-buddhist|day|sunday:format-abbreviated).
2. A second issue is how to pick the "paradigm" casing for each bucket. The algorithm I use now is to just use the first item in each bucket.
3. A third issue is how to "turn off" the warning; some way for the user to add data that says "it is ok for this item to have different case" (This is a more general issue regarding errors/warnings.)

A broader issue is what we should do with inText and inList in order to deal with casing, and how to deal with the fact that sometimes items in a bucket should undergo a case transformation in a particular environment (eg should be titlecased in menus but otherwise lowercased).

## Determining whether current structure is sufficient

(from Peter E, 2009 Nov 18)

The attached "CasingContexts.pdf" is a first draft of a doc providing examples of various contexts for usage of date formats and date elements, language names, region names, and names of various other CLDR keys. This document is somewhat oriented to Mac OS X (since those were the examples at hand), so I would like to solicit other type of examples that may cover situations not depicted here. Suggestions welcome!

What I hope to do with this (and very soon) is to send it out to localizers to solicit either sample translations for each item in context (so I can infer the various grammatical cases and capitalization cases that CLDR may need to support), or better yet, have the localizers let me know about all of the cases that are necessary.

With that information I hope to:

1. Determine what additional types (beyond form,at and standalone) may be necessary for date formatting, and
2. See whether inText/inList are adequate to cover the capitalization cases, and if not, try to come up with something better.

(from Peter E., 2009 Nov 22)

I have attached "CasingContextsV2.pdf" which fixes the calendar menu example (thanks Kent!) and adds examples of currencies in text and various examples of units in text. I still need to add an example of currency in a dialog, along with overall instructions.

(from Peter E., 2009 Nov 25)

Updated to "[CasingContextsV3.pdf](https://drive.google.com/file/d/1mvXlCSPhU87nl9owW_ZeCYHy_pJ-RqHL/view?usp=sharing)" which adds an overall explanation of the purpose of this document as well as instructions for localizers to provide feedback.


![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
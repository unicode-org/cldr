---
title: Plural Rules
---

# Plural Rules

Languages vary in how they handle plurals of nouns or unit expressions ("hour" vs "hours", and so on). Some languages have two forms, like English; some languages have only a single form; and some languages have multiple forms. CLDR uses short, mnemonic tags for these plural categories:

- zero
- one (singular)
- two (dual)
- few (paucal)
- many (also used for fractions if they have a separate class)
- other (required—general plural form—also used if the language only has a single form)

*See [Language Plural Rules](https://www.unicode.org/cldr/charts/latest/supplemental/language_plural_rules.html) for the categories for each language in CLDR.*

These categories are used to provide localized units, with a more natural ways of expressing phrases that vary in plural form, such as "1 hour" vs "2 hours". While they cannot express all the intricacies of natural languages, they allow for more natural phrasing than constructions like "1 hour(s)".

## Reporting Defects

When you find errors or omissions in this data, please report the information by [filing a ticket](/requesting_changes#how-to-file-a-ticket). Please give examples of how the forms may differ. You don't have to give the exact rules, but it is extremely helpful! Here's an example:  

**Sample Bug Report**

The draft Ukrainian (uk) plural rules are:

one: 1, 21, 31, 41, 51, 61\...

few: 2\-4, 22\-24, 32\-34\...

other: 0, 5\-20, 25\-30, 35\-40\...; 1\.31, 2\.31, 5\.31\...

Although rules for integer values are correct, there needs to be four categories,

with an extra one for fractions. For example:

1 день<br>
2 дні<br>
5 днів<br>
1\.31 дня<br>
2\.31 дня<br>
5\.31 дня

## Determining Plural Categories

The CLDR plural categories do not necessarily match the traditional grammatical categories. Instead, the categories are determined by changes required in a phrase or sentence if a numeric placeholder changes value. 

### Minimal pairs

The categories are verified by looking a minimal pairs: where a change in numeric value (expressed in digits) forces a change in the other words. For example, the following is a minimal pair for English, establishing a difference in category between "1" and "2".

| Category | Resolved String | Minimal Pair Template |
|---|---|---|
| one | 1 day | {NUMBER} day |
| other | 2 day s | {NUMBER}  day s |

Warning for Vetters

The Category (Code) values indicate a certain range of numbers that differ between languages. To see the meaning of each Code value for your language see [Language Plural Rules](https://www.unicode.org/cldr/charts/latest/supplemental/language_plural_rules.html) chart.

*The minimal pairs in the Survey Tool are not direct translations of English*. They *may* be translations of English, such as in [German](https://st.unicode.org/cldr-apps/v#/de/MinimalPairs/), but must be different if those words or terms do not show the right plural differences for your language. For example, if we look at [Belarusian](https://st.unicode.org/cldr-apps/v#/be/MinimalPairs/), they are quite different, corresponding to “{0} books in {0} days”, while [Welsh](https://st.unicode.org/cldr-apps/v#/cy/MinimalPairs/43b7793f1f673abe) has the equivalent of “{0} dog, {0} cat”. *Be sure to read the following examples carefully and pay attention to error messages.*

For example, English has no separate plural form for "sheep". It would be wrong for the two phrases to be: 

- one: {0} sheep
- other: {0} sheep

You have to pick a different phrase if that is the case in your language. Do not change the sentence in other ways, such as an "unforced change". For example, don't have the 'one' phrase be "{0} sheep" and the 'other' be "{0} deer".

The {0} will always have just a number composed of pure digits in it, such as 0, 1, 2, 3, … 11, 12, … 21, 22, .… 99, 100, …. For example, “1 dog, 1 cat” or “21 dog, 21 cat”. If there are multiple instances of {0}, they will always have the same number. The sentences must be parallel, with exactly the same construction except for what is forced by a change in digits. That is, for a language that has "one" and "other" categories: 

- take the phrase for "other"
- change the {0} to "1"
- make only the other changes to the phrase that are grammatically necessary because of that change
- change the "1" back to "{0}"
- you should then have the phrase for "one"

Gender is irrelevant. Do not contort your phrasing so that it could cover some (unspecified) item of a different gender. (Eg, don't have “Prenez la {0}re à droite; Prenez le {0}er à droite.”) The exception to that is where two nouns of different genders to cover all plural categories, such as Russian “из {0} книг за {0} дня”.

Non\-inflecting Nouns—Verbs

Some languages, like Bengali, do not change the form of the following noun when the numeric value changes. Even where nouns are invariant, other parts of a sentence might change. That is sufficient to establish a minimal pair. For example, even if all nouns in English were invariant (like 'fish' or 'sheep'), the verb changes are sufficient to establish a minimal pair:

| Category | Resolved String | Minimal Pair Template |
|---|---|---|
| one | 1 fish is swimming | {NUMBER}  fish is swimming |
| other | 2 fish **are** swimming | {NUMBER}  fish **are** swimming |

Non\-inflecting Nouns—Pronouns

In other cases, even the verb doesn't change, but *referents* (such as pronouns) change. So a minimal pair in such a language might look something like:

| Category | Resolved String | Minimal Pair Template |
|---|---|---|
| one | You have 1 fish in your cart; do you want to buy **it**? | You have {NUMBER} fish in your cart; do you want to buy **it**? |
| other | You have 2 fish in your cart; do you want to buy **them**? | You have {NUMBER} fish in your cart; do you want to buy **them**? |

Multiple Nouns

In many cases, a single noun doesn't exhibit all the numeric forms. For example, in Welsh the following is a minimal pair that separates 1 and 2:

| **Category** | **Resolved String** |
|---|---|
| one | 1 ci |
| two | 2 **g**i |

But the form of this word is the same for 1 and 4\. We need a separate word to get a minimal pair that separates 1 and 4:

| **Category** | **Resolved String** |
|---|---|
| one | 1 gath |
| two | 1 cath |

These combine into a single Minimal Pair Template that can be used to separate all 6 forms in Welsh.

| Category | Resolved String | Minimal Pair Template |
|---|---|---|
| zero | 0 cŵn, 0 cathod | {NUMBER}  cŵn, {NUMBER}  cathod |
| one | 1 ci, 1 gath | {NUMBER}  ci, {NUMBER}  gath |
| two | 2 gi, 2 gath | {NUMBER}  gi, {NUMBER}  gath |
| few | 3 chi, 3 cath | {NUMBER}  chi, {NUMBER}  cath |
| many | 6 chi, 6 chath | {NUMBER}  chi, {NUMBER}  chath |
| other | 4 ci, 4 cath | {NUMBER}  ci, {NUMBER}  cath |

Russian is similar, needing two different nouns:

| Category | Resolved String | Minimal Pair Template |
|---|---|---|
| one | из 1 книги за 1 день | из {NUMBER}  книги за {NUMBER}  день |
| few | из 2 книг за 2 дня | из {NUMBER}  книг за {NUMBER}  дня |
| many | из 5 книг за 5 дней | из {NUMBER}  книг за {NUMBER}  дней |
| other | из 1,5 книги за 1,5 дня | из {NUMBER}  книги за {NUMBER}  дня |

The minimal pairs are those that are required for correct grammar. So because 0 and 1 don't have to form a minimal pair (it is ok—even though often not optimal—to say "0 people") , 0 doesn't establish a separate category. However, implementations are encouraged to provide the ability to have special plural messages for 0 in particular, so that more natural language can be used:

- None of your friends are online.
- *rather than*
- You have 0 friends online.

Fractions

In some languages, fractions require a separate category. For example, Russian 'other' in the example above. In some languages, they all in a single category with some integers, and in some languages they are in multiple categories. In any case, they also need to be examined to make sure that there are sufficial minimal pairs.

### Rules

The next step is to determine the rules: which numbers go into which categories.

Integers

Test a variety of integers. Look for cases where the 'teens' (11\-19\) behave differently. Many languages only care about the last 2 digits only, or the last digit only.

Fractions

Fractions are often a bit tricky to determine: languages have very different behavior for them. In some languages the fraction is ignored (when selecting the category), in some languages the final digits of the fraction are important, in some languages a number changes category just if there are visible trailing zeros. Make sure to try out a range of fractions to make sure how the numbers behave: values like 1 vs 1\.0 may behave differently, as may numbers like 1\.1 vs 1\.2 vs 1\.21, and so on.

### Choosing Plural Category Names

In some sense, the names for the categories are somewhat arbitrary. Yet for consistency across languages, the following guidelines should be used when selecting the plural category names.

1. If no forms change, then stop (there are no plural rules — everything gets '**other**')
2. '**one**': Use the category '**one**' for the form used with 1\.
3. '**other**': Use the category '**other**' for the form used with the most integers.
4. '**two**': Use the category '**two**' for the form used with 2, *if it is limited to numbers whose integer values end with '2'.*
	- If everything else has the same form, stop (everything else gets '**other**')
5. '**zero**': Use the category '**zero**' for the form used with 0, *if it is limited to numbers whose integer values end with '0'.*
	- If everything else has the same form, stop (everything else gets '**other**')
6. '**few**': Use the category '**few**' for the form used with the least remaining number (such as '4')
	- If everything else has the same form, stop (everything else gets '**other**')
7. '**many**': Use the category '**many**' for the form used with the least remaining number (such as '10')
	- If everything else has the same form, stop (everything else gets '**other**')
	- If there needs to be a category for items only have fractional values, use '**many**'
8. If there are more categories needed for the language, describe what those categories need to cover in the bug report.

See [*Language Plural Rules*](https://www.unicode.org/cldr/charts/latest/supplemental/language_plural_rules.html) for examples of rules, such as for [Czech](https://www.unicode.org/cldr/charts/latest/supplemental/language_plural_rules.html#cs), and for [comparisons of values](https://www.unicode.org/cldr/charts/latest/supplemental/language_plural_rules.html#cs-comp). Note that in the integer comparison chart, most languages have 'x' (other—gray) for most integers. There are some exceptions (Russian and Arabic, for example), where the categories of 'many' and 'other' should have been swapped when they were defined, but are too late now to change.

## Important Notes

*These categories are only mnemonics \-\- the names don't necessarily imply the exact contents of the category.* For example, for both English and French the number 1 has the category one (singular). In English, every other number has a plural form, and is given the category other. French is similar, except that the number 0 also has the category one and not other or zero, because the form of units qualified by 0 is also singular.

*This is worth emphasizing:* A common mistake is to think that "one" is only for only the number 1\. Instead, "one" is a category for any number that behaves like 1\. So in some languages, for example, one → numbers that end in "1" (like 1, 21, 151\) but that don't end in 11 (like "11, 111, 10311\).

Note that these categories may be different from the forms used for pronouns or other parts of speech. *In particular, they are solely concerned with changes that would need to be made if different numbers, expressed with decimal digits,* are used with a sentence. If there is a dual form in the language, but it isn't used with decimal numbers, it should not be reflected in the categories. That is, the key feature to look for is: 

If you were to substitute a different number for "1" in a sentence or phrase, would the rest of the text be required to change? For example, in a caption for a video:

&emsp;"Duration: 1 hour" → "Duration: 3\.2 hours"

## Plural Rule Syntax

See [LDML Language Plural Rules](https://unicode.org/reports/tr35/tr35-numbers.html#Language_Plural_Rules).

## Plural Message Migration

The plural categories are used not only within CLDR, but also for localizing messages for different products. When the plural rules change (such as in [CLDR 24](/index/downloads/cldr-24-release-note)), the following issues should be considered. Fractional support in plurals is new in CLDR 24\. Because the fractions didn't work before, the changes in categories from 23 to 24 should not cause an issue for implementations. The other changes can be categorized as Splitting or Merging categories.

There are some more complicated cases, but the following outlines the main issues to watch for, using examples. For illustration, assume a language uses "" for singular, "u" for dual, and "s" for other.​ ​

- **OLD Rules \& OLD Messages** marks the situation before the change,
- **NEW Rules \& OLD Messages** marks the situation after the change (but before any fixes to messages), and
- **NEW Rules \& NEW Messages** shows the changes to the messages

### Merging

The language really doesn't need 3 cases, because the dual is always identical to one of the other forms. 

**OLD Rules \& OLD Messages**

one: book

two: books

other: books

1  ➞ book, 2 ➞ books, 3 ➞ ​ books​

**NEW Rules \& OLD or NEW Messages**

one: book

other: books

1  ➞ book, 2 ➞ books, 3  ➞​ books​

This is fairly harmless; merging two of the categories shouldn't affect anyone because the messages for the merged category should not have material differences. The old messages for 'two' are ignored in processing. They could be deleted if desired.

This was done in CLDR 24 for Russian, for example.

### Splitting Other

In this case, the 'other' needs to be fixed by moving some numbers to a 'two' category. The way plurals are defined in CLDR, when a message (eg for 'two') is missing, it always falls back to 'other'. So the translation is no worse than before. There are two subcases.

Specific Other Message

In this case, the *other* message is appropriate for the other case, and not for the new 'two' case.

**OLD Rules \& OLD Messages**

one: book

other: books

1  ➞ book, 2 ➞ books, 3  ➞​ books​

**NEW Rules \& OLD Messages**

one: book

two: **books**

other: books

1  ➞ book, 2 ➞ **books**, 3  ➞​ books​

The quality is no different than previously. The message can be improved by adding the correct message for 'two', so that the result is:

**NEW Rules \& NEW Messages**

one: book

two: booku

other: books

1  ➞ book, 2 ➞ **booku**, 3  ➞​ books​

***However, if the translated message is not missing, but has some special text like "UNUSED MESSAGE", then it will need to be fixed; otherwise the special text will show up to users!***

Generic Other Message

In this case, the *other* message was written to be generic by trying to handle (with parentheses or some other textual device) both the plural and dual categories.

**OLD Rules \& OLD Messages**

one: book

other: book(u/s)

1  ➞ book, 2 ➞ **book(u/s)**, 3  ➞​ **book(u/s)**

**NEW Rules \& OLD Messages**

one: book

two: book(u/s)

other: book(u/s)

1  ➞ book, 2 ➞ **book(u/s)**, 3  ➞​ **book(u/s)**

The message can be improved by adding a message for 'two', and fixing the message for 'other' to not have the (u/s) workaround:

**NEW Rules \& NEW Messages**

one: book

two: booku

other: books

1  ➞ book, 2 ➞ booku, 3  ➞​ books

### Splitting Non\-Other

In this case, the 'one' category needs to be fixed by moving some numbers to a 'two' category.

**OLD Rules \& OLD Messages**

one: book/u

other: books

1  ➞ book/u, 2 ➞ book/u, 3  ➞​ books​

**NEW Rules \& OLD Messages**

one: book/u

other: books

1  ➞ **book/u**, 2 ➞ **books**, 3  ➞​ books​

This is the one case where there is a regression in quality. In order to fix the problem, the message for 'two' needs to be fixed. If the messages for 'one' was written to be generic, then it needs to be fixed as well.

**NEW Rules \& NEW Messages**

one: book

two: booku

other: books

1  ➞ **book**, 2 ➞ **booku**, 3  ➞​ books​


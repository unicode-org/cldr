---
title: Capitalization
---

# Capitalization

Beginning with CLDR 22, the guidance is that names of items such as languages, regions, calendar and collation types, as well as names of months and weekdays in calendar data and the names of calendar fields, should be capitalized as appropriate for the middle of body text (except possibly for narrow forms, see note below).

Regarding the capitalization of months and weekdays, please apply middle\-of\-sentence capitalization rules even on stand\-alone items.

**In your language, if month and day names are generally lower case in the middle of the sentence, then please apply this same rule (lower case) to both formatting and standalone values.**

In your language, if month and day names are generally upper case in the middle of the sentence, then please apply the same rule (upper case) to the standalone values.

The primary reason for having both format and stand\-alone forms is to handle any necessary grammatical distinctions (rather than capitalization distinctions).

- Stand\-alone month names are intended to be used without a day\-of\-month number
- Format month names are intended to be used with a day\-of\-month number.

In many languages, that means that the stand\-alone month names should be in nominative form, while the format month names should be in genitive or a related form.

In this case, date formats will also reflect that, using the format form MMMM in a format such as “d MMMM y”, and the stand\-alone form LLLL in a format such as “LLLL y”.

**Note:** Narrow forms for items such as month and day names are typically too short to reflect differences between grammatical forms. For capitalization purposes, format narrow names should be capitalized according to the normal conventions for their use in running text, and stand\-alone narrow names should be capitalized according to conventions for stand\-alone use.

The new \<contextTransforms\> element now indicates how to change the capitalization for use in a menu, or for stand\-alone use such as in the title of a calendar page (the \<contextTransforms\> data cannot currently be edited in the Survey Tool; please file a bug for any necessary changes).

However, it is also important to ensure that there is consistent casing for all of the items in a section, so before making any changes, be sure to get agreement among all the translators for your language—otherwise the capitalization of items in a section may appear random.

To provide warnings when the capitalization of an item differs from what is intended for items in a given category, the Survey Tool now checks capitalization of items against the \<casingData\> within the \<metadata\> element; data for this comes from xml files in the CLDR common/casing/ directory. This data cannot be changed using the Survey Tool; if it is incorrect, please file a bug (initial data was created based on the predominant capitalization of items in each category within a locale, and may be wrong).


---
title: Delimiter (Quotation Mark) Proposal
---

# Delimiter (Quotation Mark) Proposal

## Delimiter Consistency

The following is a proposal for how to handle the delimiter issues in CLDR, raised in bug [http://unicode.org/cldr/trac/ticket/4201](http://unicode.org/cldr/trac/ticket/4201).

There are qute a number of problems in our delimiter data. Most of these are in the ‘lesser vetted’ languages, but some are in major languages. Problems include use of ASCII quotes, and obvious inconsistencies in data.

The goal for the v49 release is to at least have consistent data, then have the translators look at the changes in the data submission phase for v50. 

**Data Cleanup.** I went through the data, and cleaned up obvious problems (such as a generic ASCII quote on one side, and a curly quote on the other). I then compared it to the Wikipedia data where available; if there was a difference between CLDR and Wikipedia, I looked for original sources on the web. It is often a bit tricky, since there are be variant practices in many languages. The goal is to have it to be the most customary usage, but often it may not be clear which variant predominates.

Note that for a great many locales (especially African ones), there isn’t much to go on, so I left it at minor cleanup. I suspect that many of them are problematic; it might be worth asking whether they follow the conventions of another language, like French, rather than asking what the characters are. See below for the results.

**Bidi.** I tried to gauge from sources what the practice is. However, it is the visual practice, so we have two alternatives.

1. Use the ASCII ugly quotes
2. Reverse the two, which would be correct with all BIDI or with correct markup.

Personally, I lean towards #2. 

The first sheet has recommendations for changing; the others are just scratch sheets.

- The Proposed columns contain the recommendations for v49.
- The Style columns just show a label for each type, since it is sometimes hard to distinguish the directions of the marks.
- The Old columns show the current values.
- And the last column shows the locales in question.

[**Quotation (Delimiter) Proposal**](https://docs.google.com/spreadsheets/d/1_7vjBSmjlmevIQfpM4xX1yMGYQdibd6h8PTrQEPzzkQ/edit?gid=2#gid=2)

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
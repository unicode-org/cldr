---
title: European Ordering Rules Issues
---

# European Ordering Rules Issues

The European ordering rules feature is a new collation feature in CLDR which attempts to reflect the efforts within the European community to come up with a single non-language specific way to sort the characters used in various European languages.

A copy of a near-final draft (FprEN 13710:2010) is available to Unicode members in the [UTC document register](http://www.unicode.org/L2/L-curdoc.htm) (L2/14-143).

This document describes current issues in an attempt for us to have a clear picture of what level of EOR support will be contained within CLDR 21. 

Current Status

1. EOR rules as defined by EN 13710:2011 Annex G ( March 2011 draft ) are currently included in the latest CLDR root locale as \<collation type="eor">. There are a number of issues with these rules as currently written, see below...
2. EOR tailorings for specific languages should be relatively easy to create once we settle on the base EOR rules. These should be able to be created by doing an import of the base EOR rules and then importing the language specific tailoring on top of that.
3. The import feature in ICU is now considered to be stable and ready for use.


Issues with current EOR rules:

1. The ignoring rules for currency etc. should be filtered out in the CLDR context. ( Mark, John, Åke)
2. The rule for U+029F SMALL CAPITAL L is missing (typo in standard). ( Åke )
3. There are relevant comments by Kent Karlsson in ticket #[763](http://unicode.org/cldr/trac/ticket/763) (2010-10-27), with a modified proposal
	1. --- \&#x20E9;(\&#x20E9; = [U+20E9](http://unicode.org/cldr/utility/character.jsp?a=20E9) ( ⃩ ) COMBINING WIDE BRIDGE ABOVE) is the (currently) weightiest, at level 2, non-letter general purpose combining mark
	2. --- \&#x20E9; is used in the proposal to make all "variants" come after all single-accented versions of letters
	3. --- resetting to just A, B, etc. would make variant versions come before accented versions
4. ( Åke ) The current reset rules work fine with MimerSQL, but I think you must check the ICU behaviour. Kent might have a vital point here.
5. (Kent) (digraphs) ----tertiary difference in DUCET; keep it that way
6. ( Åke ) Kent is right, it should be tertiary difference. (EN13710 needs a revision).
7. (Kent) -hv/hwair is a case pair and should be treated as such, not be given two different level 2 weights ( Åke ) I agree. (EN13710 needs a revision).

Questions

1. Which EOR base to use? If EN13710 needs revisions, how do we make that happen?
2. Should we use Kent's modified rules as attached to http://unicode.org/cldr/trac/ticket/763 ?
3. What locales should provide EOR based tailorings?
4. Need to add EOR to BCP47. 

Choices

1. Wait for EN13710 to be fixed
2. Put in a fixed version ourselves
3. Put in the "stock" version, knowing about the problems.

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
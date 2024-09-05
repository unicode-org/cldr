---
title: Coverage Revision
---

# Coverage Revision

1. Propose changing CoverageLevel to be data driven. Rough thoughts.

Have a list of paths

\- ​/​/ldml​/identity​/version => NONE

\- ...

\+ /​/ldml​/localeDisplayNames​/languages​/language[@type="\*"] language:​type=​[en, und] => Minimal

\+ ...

\- ...

Have special variables for

- this language
- this language's countries
- this language's territories
- ...

2. Current coverage: needs review
	1. I put the tentative results in http://spreadsheets.google.com/pub?key=t5UzIpaSqcYBksSMtZp-f7Q&output=html
	2. The more detailed files are in http://unicode.org/repos/cldr-tmp/trunk/dropbox/mark/coverage/. In particular:
		1. http://unicode.org/repos/cldr-tmp/trunk/dropbox/mark/coverage/summary.txt
		2. http://unicode.org/repos/cldr-tmp/trunk/dropbox/mark/coverage/samples.txt
		3. http://unicode.org/repos/cldr-tmp/trunk/dropbox/mark/coverage/fullpaths.txt

There is more to do, but I wanted to give a snapshot

\- tune the weighting

\- weight by draft level

\- add collations, rbnf, plural rules, transforms (if non-Latin), etc.

From John

I've been doing some more thinking about how to deal with coverage in CLDR. It seems to me that we already have the notion that every XPath in CLDR should have some predefined number associated with it between 0 and 100 that denotes it's relative importance in terms ofcoverage, with 0 being absolutely critical, and 100 being not critical at all. See Appendix M of TR35 for a brief description of the levels. I think that if we could accurately quantify this using metadata, then it would be relatively easy for us to accomplish two things:

1). Filter out fields from the ST that we don't really need, since everything we do would be filtered based on a desired coverage level.

2). Allow individual users to set the filtering in the survey tool based on one of the predefinedcoverage levels as we already have in the spec, or actually any other numeric coverage level that they desire.

So with this in mind, I would like to propose the following structure to be added to the supplemental metadata:

\<coverageLevels>

&emsp;\<coverageLevel value="nn" match="regular expression"/>

&emsp;\<coverageLevel value="nn" match="another regular expression"/>

....

\</coverageLevels>

Finding the appropriate coverage level value would then be a matter of searching the coverageLevel entries in numeric order by value looking for a match of the path vs. "//ldml/" + "regular expression". In other words, we would not specifically include "//ldml" in the expressions, since they would all start with that. Once a given xpath's coverage level value was determined, it shouldn't be too hard for us to simply filter out fields whose coverage level was higher then the requested. I suppose that we will need some wildcards similar to what Mark has started working on in his path filtering proposal.

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
---
title: Post Mortem
---

# Post Mortem

Drivers marked in [...]. Drivers are to file bugs, put together plan for how to handle.

**Post Mortem (Phase I - not translators)**

1. Spec done late, little review, little time for public review. We should have a formal release and announcement ahead of time. Splitting up the spec into functional pieces, maintained in Sites, could let us put the introductory info into the pieces at the start. Move the design doc into the spec once approved. Point the survey tool to relevant parts of the spec. Sync up spec for each milestone. **[Peter, Mark]**
2. How to get metazone translated. Not enough coverage. We ask for translations that are not needed in the survey tool; those also contribute to the lack of coverage.
	1. Make coverage be data driven.
	2. Make metazone coverage exclude items that never need coverage; be dependant on the language (and the territories that use it). [http://cldr.unicode.org/development/design-proposals/coverage-revision](http://cldr.unicode.org/development/design-proposals/coverage-revision)
	3. Make coverage visible in ST.
	4. Make metazones easier to understand in the ST. Better examples of how they affect zones.
	5. CommonlyUsed can't be effectively entered in ST.
	6. **[John, (Mark for Coverage)]**
3. Avoid Xmas; don't do release in March. Release "clean up" tools release in Oct, include U6.0; have regular release in next June. The Oct release can have data changes, but it wouldn't use the ST to gather data. Agreement on that as tentative dates. **[DONE]**
4. Flexible data formatting also needs better presentation, and examples. **[Chris, Peter; #**[**2133**](http://unicode.org/cldr/trac/ticket/2133)**]**
5. Bulk import problems. Major problem was timing; need to come in at the latest a few weeks after data submission. Got bad data. Some language tags wrong (both inside the XML and as the name of the files); syntax wrong, choice of tags, and using wrong aliases. Character encoding problems; not UTF-8 or mixed. Need a tool that is more strict than regular tests, that prevents bulk import if there are problems. Need better gatekeeper for both checkin to SVN and import into ST. Need clearer policy on bulk import.
	1. [2424](http://unicode.org/cldr/trac/ticket/2424) - JSP to test bulk import
	2. [2579](http://unicode.org/cldr/trac/ticket/2579#comment:1) - comments on ConsoleCheckCLDR vs bulk import
	3. Example of bulk import; large changes of structure, example: casing changes.
	4. No bulk imports after certain date.
	5. Add comments on bulk changes; attached to each change, eg proposed-u666-r12 (r12 points to a string with background).
		1. srl: consider Wikipedia's [Bot Policy#Good Communication](http://en.wikipedia.org/wiki/Wikipedia:Bot_policy#Good_communication)
	6. **Delay until 2.0 (no bulk except brand-new locales). [John to file bug].**
6. Late implementation of new voting rules. Anything that changes what gets marked as "approved" must be done before data submission. ?Tune voting rules for new structure? **[Done]**
7. Quality suffered through using outside contractors. **[Done]**
8. Need tighter control of commitments vs. milestones. Reviews don't get done until it's too late to do anything about it. BRS for each milestone. Define what the milestones mean. Don't move ahead until all criteria for milestone are met, including reviews. Balance reviews each week also. **[Done]**
9. Tests and tools
	1. Clean up the unit tests; have regular suite that can be mandated (at least for quick check). **[Umesh]**
	2. Better tools in trac/svn, things that haven't been ported from ICU (code review). **[Steven]**
	3. Need automated build in CLDR code, one that run tests. **[Yoshito]**
	4. Would like to be able to also build/run ICU tests at the same time. **[Punt]** *(or file a bug for Steven to document somewhere how to do from the command-line? not a high priority)*
	5. When do we take drops of ICU4J? **[Yoshito to write page for process]**
	6. Need to control when we move to different versions of trunk, so we can do it at the same time. **[Under control]**
10. LDML2ICUConverter - need to recode for clarity... Get rid of the DOM-based code (LDMLUtilities). [Non-supplemental needs work.]
	1. General cleanup of supplemental data handling, coverage/filtering **[Mark]**
	2. Create staging plan **[John]**
	3. Don't have overall picture of utilities and how and why they are used. **[Punt]**

**Phase 2**

1. PM phase 2 (from translators)
	1. Voting issues
		1. new items had too high a threshold, remained provisional
		2. changes did not get approved
		3. allow new items without 8 votes should help, but other problems because of too few organizations.
	2. **[Chrish]**
	3. Arabic; difficult to work with certain data, because they don't know what they will look like in different contexts. Primarily tools. (Mark: maybe show examples in both RTL & LTR cells?) **[Peter, Chris; #**[**2133**](http://unicode.org/cldr/trac/ticket/2133)**]**
	4. Available formats, Interval formats. Vetters can't see what the effects are. Apple has internal tool; maybe integrate in ST? **[Peter, Chris]**
	5. Russian/Catalan. Difficulty in making bulk changes (eg make changes to large number of fields).  **[Chris]**
	6. All like the quick steps; **(thanks to Steven and John).**
	7. Leverage QuickSteps in rest of survey tool; allow Example column in QS, etc. **[Steven, John]**
	8. Vetters used to better tools, able to group. (sort/group/filter) **[Steven, John]**

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
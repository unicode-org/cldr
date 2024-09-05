---
title: UTS #35 Splitting
---

# UTS #35 Splitting

Strawman here for discussion.

1. Divide up the spec by functional lines:
	- Dates and Times
	- Numbers & Currencies
	- Collation
	- ...
	- Misc.
	- Other supplemental data
	- Supplemental metadata

Important features

- Collaboration
- Many authors
- Cheap tools, accessible to everyone
- Easy to edit.
- Must be able to snapshot.
- Stylesheets (or equivalent mechanisms) are critical.
- ...

2. Options.
	1. Use HTML, but break into Part1, Part2, .... Still have to muck with tagging; non-WYSIWYG editing.
	2. Eric strongly recommends docbook (see http://wiki.docbook.org/topic/DocBookAuthoringTools).
	3. Ask Richard Ishida about how W3C documents work. [Mark]
	4. Use Sites for the subdocuments. We've done this in ICU, and it makes it easier to edit, and thus easier to add new material.

	The release would consist of taking a snapshot of the site, copying to different number (eg ldmlspec2.1)

	4.1. There is a *rough* prototype:
	1. http://sites.google.com/site/ldmlspec/home?previewAsViewer=1
	2. http://unicode.org/repos/cldr-tmp/trunk/dropbox/mark/LDML.1.pdf
	3. http://sites.google.com/site/ldmlspec/home

	4.2. Discussion
	1. Mark to look at whether we can make a copy for a snapshot of a version. DONE (easy to do)
	2. Advantages:
		1. any of us can edit easily
	3. Disadvantages:
		1. Numbering couldn't be within chapter (eg Chapter 2 section 1 would be 1.)
			1. Could only approximate the TR format.
			2. CSS doesn't yet work.

![Unicode copyright](https://www.unicode.org/img/hb_notice.gif)
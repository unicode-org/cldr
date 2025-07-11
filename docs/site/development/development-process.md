---
title: 'Handling Tickets (bugs/enhancements)'
---

# Handling Tickets (bugs/enhancements)

### Filing tickets

1. You can add set other fields, but don't change the Owner. That way the committee sees the incoming tickets (those marked as Owner: somebody).
2. ***If you really have to fix a bug right away, file it and send a notice to cldr\-dev explaining what you are doing and why. Otherwise, file the bug, wait for the next Wednesday meeting for discussion and approval.***

### Pre\-Assessment for Weekly Triage

*There is a pre\-assessment of tickets to make the triage flow faster. This pre\-assessment should be done off\-line prior to every Wednesday in time for the TC triage. DO NOT fix a bug until it's been approved by the TC in triage.*

*The triage is a part of Monday and Wednesday meetings.*

1. Ticket comes in, and the default is **Priority**\=assess, **Milestone**\=to\-assess
	1. If user sets Component (we always should!) automatically assigns the Owner based on the component owner for the pre\-assessment. \[Add link to Components and Owner]
	2. *At this point the Owner function is really as an assessor.*
	3. If user does not select the Component, it remains for the group TC triage with Component\=to\-assess, Owner\=anybody. These tickets will be assessed after all the pre\-assessed tickets have been triaged.
2. All TC members, look at the triage spreadsheet linked from the agenda before each triage day.
	1. Assess the **to\-assess** tickets assigned to you.
	2. You can close bugs as duplicate or if user misunderstanding if it is very clear without committee discussion. Always include a comment if you close the ticket.
	3. If the component is wrong, change component and reassign to the right component owner.
		1. (TODO: copy components to that page, and add notes to clarify; if possible, link to list of components \+ descriptions at top of new ticket). http://cldr.unicode.org/index/bug\-reports
	4. Add your assessment information:
		1. Change **Milestone** to “assessed”
			1. Update Priority, Phase, Component as you'd recommend.
		2. Add in comments your recommendation for the Owner.
		3. Add additional comments as necessary for other recommendations (like “close as dup”, “maint”, in design, etc)
3. If a committee member files the ticket (or sees a ticket they want to assess), they can do the assessment and follow the same process as the Owner/Delegator to bring to group triage.
	1. Committee members can also request to take a ticket by email; that is assumed to be ok if at least one member confirms and nobody disagrees, or 2 working days pass
	2. The committee can also decide the fate of Milestone\=to\-assess tickets, typically where someone adds them to the agenda.
4. TC triage meeting, first review those Milestone\=**assessed**.
	1. Follow the triage practice by making changes to all fields, Owner, Component, Priority, … or closing the ticket.
	2. Update Milestone to one of the following (VV \= the current milestone, eg 35\)
		1. **VV** if the (new) Owner agrees that it is a priority — and has some level of commitment to fix during release
		2. **VV\-optional**, eg 35\-optional
			1. A **future** milestone
		3. Triage decides whether it's for Design or Accepted. (**design** indicates for more discussion needed. See design section below.).
5. Other ticket handling practices:
	1. There is no commitment to doing a Milestone\=**VV\-optional** ticket. If someone else wants there to be some level of commitment, they should ask to be the Owner, and make the ticket non\-optional.
	2. If a ticket has multiple owners, the Notes must describe who is doing what. Otherwise it needs to be split into multiple tickets.
	3. if the ticket should be handled in the survey tool there are two options:
		1. If to be supervised by TC, set Owner\=\<PM\>, Component\=fix\-in\-survey\-tool. The PM will coordinate with other PMs to bring the issue to the attention of vetters, and check the progress before Resolution.
		2. Otherwise return to sender, closed as fix\-in\-survey\-tool

### Design

When a ticket is in design, the owner is responsible for bringing back to the committee to approve the design before any lasting (aside from tests, instrumentation, etc.) work is checked in.

1. The Owner is responsible for documenting the results of the discussion in the TC in comments in the ticket.
2. The Reviewer is responsible for verifying that the design was accepted by the TC before accepting.

### Phases


|   |   |
|---|---|
| pre-sub | The ticket should be completed before submission starts. |
| pre-sub2 | The ticket should be completed within the first 2 weeks of the submission phase. |
| pre-vet | The ticket should be completed before vetting starts. |
| pre-xml | The ticket should be completed before data is moved to XML. |
| pre-res | The ticket should be completed before resolution starts. |
| pre-slush | The ticket should be completed before slush. |
| pre-icu | The ticket should be completed before data and structure used by ICU is frozen. |
| pre-data-beta  | The ticket must be completed before **Alpha — Final Data candidate** . No change to data affecting ICU thereafter. Other dtd, data, spec, docs, tool changes allowed. |
| pre-spec-beta | The ticket must be completed before  **Beta — Final Candidate** . No dtd or data changes allowed thereafter. Docs, charts, spec changes allowed (= ICU release candidate) |
| pre-release | The ticket must be completed before  **Release**  ( = ICU Release) |

### Assignees

1. Assignees periodically review and take care of their tickets.
	1. Go to http://unicode.org/cldr/trac/report/61 for **My Tickets**
2. ~~**design:**~~ ~~You must go back to the committee with the completed design before checking anything in. For example, if the ticket doesn't describe an exact DTD structure, then add the proposed structure to the ticket in a comment, and send an email to the committee asking whether anyone objects.~~
3. ~~**assess:**~~ ~~Tickets marked "assess" need to be evaluated for priority (or whether to do them at all). Send your suggested disposition to the cldr group by email.~~
	1. **duplicate:** If the ticket is a duplicate, any extra information is copied to the other ticket, an xref is set in both, and the ticket is returned.
	2. **splitting:**
		1. If the ticket requires separate phases, such as a change to the spec, then a separate ticket is filed, with xrefs set in both.
		2. The owner can carry over all the triage acceptance including Owner, Component, etc.
			1. If part of a ticket cannot be completed in the release:
				1. Set the milestone to the release number
				2. Add a comment indicating what part of the ticket was done, and what part wasn't.
				3. Set a reviewer.
				4. Create a new ticket for the remaining work, with an xref to the old bug.
	3. **problems:**
		1. If the ticket can't be done due to lack of time, or is being downgraded in priority from major or above to below major, send an email to the cldr group.
		2. If during the course of doing the ticket, the person runs up against a problem, then send a message to the cldr group. Examples:
		- The description doesn't work
		- Another mechanism looks better

### DTD Changes

- **If you are making any DTD changes, please follow the instructions on** [**Updating DTDs**](/development/updating-dtds)**.**

### Testing!

- Add unit tests for changes you make, then
- **Make sure to run the tests in** [**Running Tests**](/development/running-tests)**!**

Skipping test failures

If there is a test failure that is due to a bug that cannot be fixed right now (e.g. it requires a data change, but it is past the data freeze, and the CLDR TC considers it too risk or not a priority to fix in the current release), then use the logKnownIssue() method to disable the test.
See <https://icu.unicode.org/setup/eclipse/time> for details, and look in the CLDR test code for some examples of usage.

See also [BRS: Handling Known Issues](/development/cldr-big-red-switch/brs-log-known-issues) for the BRS task to review known issues.

### Survey Tool in Production Phase!

1. **Before** you make a commit, add a line to the Issues spreadsheet below ([Spreadsheet View](https://docs.google.com/spreadsheets/d/100_r1jobwFCTUdAafc2Ol38UHQn93OIZl5Qmw_Y_5_k/edit#gid=3)), with status OPEN, bug number, short desc.
	- *Don't do this if there is a status\=PUSHING*
2. When you're ready for checking, add the commit number to your line, send a message to a designee (cc cldr\-dev) and change the status to CHECK, add testing description (what to check, links if possible).
3. The Designee is normally someone who is familiar with the Survey Tool UI, and must be notified when the line is added. The goal is to check out smoke test to make sure that the change:
	1. doesn't make anything worse (with a quick sanity check: if so, mark as Bad, and the change needs to be reverted or fixed before pushing to production), AND
	2. does what it was supposed to do as designed per spec or comments in the ticket (if not, mark as Incomplete, and take the ticket out of State\=reviewing; but not a blocker for pushing to production)
	3. Capture any necessary information on the Information Hub: resolved items, new feature description, etc...
4. For code changes, the Owner should also:
	1. assign and notify a Reviewer
	2. add a note to the "How to test / comments" column with Reviewer\=XXX, and whether the changelists need review before a push to production (should be done for any trickier code).
	3. if the ticket is State\=reviewing (normal case), the Reviewer can then close the ticket.
5. Designee changes status to **READY**, or communicates problems back to you.

***Once the statuses are all READY, anyone can push to production:***

1. ***Important!*** *Make sure the statuses are all PUSHED or READY and that the "Last Built Rev" isn't later than any items on the list*
	1. *If there are any untested commits, double check the [timeline](http://unicode.org/cldr/trac/timeline) and make sure something isn't slipping in!*
2. ***Check*** [http://unicode.org/cldr/trac/timeline?changeset\=on\&build\=on\&daysback\=8](http://unicode.org/cldr/trac/timeline?changeset=on&build=on&daysback=8) to verify that no changes "slipped in"
3. ***Add a line PUSHING***
4. ***Push to production***
5. *If there were noticeable changes, put them in* [*http://cldr.unicode.org/index/survey\-tool\#TOC\-Latest\-Updates*](/index/survey-tool#TOC-Latest-Updates) ***(move the old items up to Tool Updates).***
6. *If it was a Known Bug, remove from* [*https://sites.google.com/site/cldr/index/survey\-tool/known\-bugs*](/index/survey-tool/faq-and-known-bugs)***.***
7. ***Change status of all READY and PUSHING items to PUSHED.***
8. ***You can delete some older items that are no longer relevant.***
9. ***If the spreadsheet data seems stale, switch to the BuildsCheckins tab, and increment the 'to update' cell. \[ If anyone knows a better way to structure the spreadsheet, please feel free.. \-srl]***

[Survey Tool Issues](https://docs.google.com/spreadsheets/d/0AqRLrRqNEKv-dF9DdVRPVmhHdzhheDliOEpLSUhObXc/edit)

### Checkin

1. All commits must have as their first line: "CLDR\-XXX \<description\>", where XXX is the ticket number.
2. **Don't mark the bug as fixed! Instead,**
	1. Once the ticket is done, assign a reviewer.
	- *Send a message to the reviewer if the review should be done soon.*

### Reviewers

1. On "Available Reports", click on [My Reviews](http://unicode.org/cldr/trac/report/21).
2. *For each of your tickets to review, open it and do the following:*
3. First read the description *and* comments/replies in the original ticket, since they may contain changes or additions, and also sometimes alert you to changes not captured by the diff tool.
	1. In the top right, under View Tickets, you'll see "Review \<x\> commits." Click on that to see the changed files. (Often best to to see side\-by\-side, with right\-click \> open in new window.)
	2. In the **Changes** column, click on each of the items (eg, [edit](http://unicode.org/cldr/trac/changeset?old=11625&old_path=trunk%2Ftools%2Fcldr-apps&new_path=trunk%2Ftools%2Fcldr-apps&new=11627)) in each of the cells, and make sure that the implementation matches the description.
	3. For data tickets, the goal is to verify that the data in the ticket matches what is entered in.
		1. For spec tickets, it is often easier to go to the [Modifications section](http://unicode.org/repos/cldr/trunk/specs/ldml/tr35.html) of the latest proposed spec update, search for the modifications entry with the ticket number for the change that you are reviewing, and then click in that entry’s link to the relevant portion of the spec. The modifications in that section will be shown in yellow, and that is what you need to review.
	4. Once you are done, go back to the original ticket (you can click on it at the top), and hit "Modify Ticket" near the bottom.
		1. If the implementation looks good:
		- click "REVIEWER: Close as fixed", then
		- click "Submit changes"
	5. If any problems are found
		- add a comment to original ticket describing the problems you found
		- click "back to owner"
		- click "Submit changes"
		- if time is short (near the end of the release), send an email to the owner also.
	6. Hit "Submit changes"
4. If any issues come up or the topic needs general review, send a message to the cldr@unicode.org list.

### Releasers

1. All reviewers should be done well before the end of the release, so that any problems can be taken care of.
2. At release time, all the tickets with status "fixed" are changed to "closed".

### Start of Release

1. The future folder tickets are moved to the discuss folder
2. Unscheduled tickets (with no release number) are re\-evaluated.



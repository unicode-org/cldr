---
title: CLDR Survey Tool Push to Production
---

# CLDR Survey Tool Push to Production

The CLDR Survey Tool may be pushed to production whenever there are important fixes in the Survey Tool for the vetters.

There is usually a weekly push to production when the Survey Tool is open for submission.

## Roles
   - Checker - checks the SMOKETEST version of the Survey Tool based on a general UI test list
   - Pusher - may be same person as the Checker

## Steps

### 1. Decide on the roles

Decide who will be the checker and the pusher for the current push to production.

### 2. Make sure all fixes are ready for the push to production

Compare the list of commits since the last push to production

- Open [Comparing changes][]
- Change the tag on the left to the most recent push to production tag, and keep `compare:` on the right as `compare: main`
   - Survey Tool releases are automatically tagged production/yyyy-MM-dd-xxxxz

![Commit Checker Workflow](/images/development/comparing-changes-for-push-to-production.png)

### 3. Notify the TC

Email the CLDR TC to ask people to hold off merging any changes until the push to production completes.

### 3. Verify Survey Tool is ready to push

   - Log into the SMOKETEST as a vetter. Be sure to specify locales so you can check the most recent bug fixes.
     - You may need to log in with more than one test account.
   - Verify you are reviewing the version of the Survey Tool for the latest commit.
     - Open Menu and click on **About**.
     - Check the hash which appears next to the gear icon. Make sure that it is the commit that is planned for the push.

### 4. Run Quick UI verification list

| Area                          | Details                                                                          |
| ----------------------------- |--------------------------------------------------------------------------------- |
| 1. Login                      |Log into SMOKETEST as a Vetter in an organization and locale(s).                  |
| 2. CLDR_CODE_HASH             |Check the CLDR_CODE_HASH info on the About page and document.                     |
| 3. Status                     |Verify status matches what was expected for the current stage. (SUBMIT/VETTING/VETTING_CLOSED/BETA/READONLY) |
| **Report issues for any of the following steps.** |                                                              |
| 4. Selecting a locale         |Open left navigatoion bar and verify only the locales you specified are visible.  |
| **5. Verify Dashboard**       |Dashboard works properly                                                          |
|    a.                         |Navigate Missing, Provisional, Warning, Reports via Dashboard.                    |  
|    b.                         |Open the Dashboard                                                                | 
|    c.                         |Check filtering works on the dashboard and clicking on the category works.        |
|    d.                         |Select an item via Dashboard, and verify you are taken to the right place         |
| **6. Verify voting view**     |Make sure examples, item status                                                   |
|    a.                         |Open the Date & Time / Fields page, and Navigate to Next/Previous page            |
| **7. Verify info panel**      |Verify the Info Panel on the right loaded correctly                               |
| **8. Verify voting**          |Voting works properly                                                             |
|    b.                         |Submitte a new value for an item                                                  |
|    c.                         |Vote for an existing item                                                         |
|    d.                         |Abstain the same existing item to undo the vote                                   | 
| **9. Verify errors**          |Vetters can't vote for invalid data items                                         |
|    a.                         |Vote for an abbreviated month or day that conflicts with another entry, verified warning displayed in the info panel |
|    b.                         |Voted for a Winning value with an error, and see error displayed in the Info Panel (requires a 4-vote locale) |
| **10. Verify forum**          |Navigation and communication works in the forum as expected                       |
|    a.                         |Clicked Forum navigation to show all posts that are Needing action                |
|    b.                         |Go to Item from Forum, and then back to the forum                                 |
|    c.                         |View forum conversation within Info Panel (make sure you can see the forum icon in the English column |
|    c.                         |Comment on conversation from within Info Panel                                    |
| **11. Verify Reports**        |Verify the reports work                                                           |         
|    a.                         |Check each report to make sure it displays properly                               |
|    b.                         |In the Date & Time report click `View` and verify it takes you to the right item  |
|    c.                         |Check voting on the report works                                                  |

### 5. Report results of Quick UI verification list

#### If check fails:

1. Add label:fails-smoketest to top item on PRs in Push to Production (update the date parameter for every release milestone).
2. Files a ticket and emails the CLDR Ops alias with the ticket and description of the issue.
3. The CLDR Ops team is responsible for figuring out what went wrong, and who needs to fix the issue.
4. Once fixed, someone from the CLDR Ops WG will email to Checker, remove label:fails-smoketest and start over the push to production process over.

#### If check succeeds:

1. Send email with the CLDR_CODE_HASH and results and Quick UI verification to Pusher with the CLDR TC on cc.
2. Pusher pushes to production (Github action - see screenshot below) for the verified CLDR_CODE_HASH version.
4. Pusher notifies the CLDR TC that people can begin merging changes again.
5. Pusher updates known issues on the Info Hub so the vetters understand what has changed.
   - Record new important Known Issues (with date changed)
   - Move any fixed Known issues to Resolved (with date changed)
      - Use [Comparing changes][] to compare the previous push to production against the latest for the list of changes.
   - Add News item at top, summarizing changes. (Drop lines more than 2 weeks old.)
6. CLDR TC sends an announcement to the vetter if there are any changes that the vetters need to be aware of more quickly.

Example announcement to vetters (with ST).

The Survey Tool has just been updated:
Resolved Issues: Survey Tool's info panel loads faster; auto-import of votes improved
Known Issues: Some old votes (notably for North Macedonia) were not imported correctly.
For more details, see http://cldr.unicode.org/translation.

[Comparing changes]: https://github.com/unicode-org/cldr/compare/

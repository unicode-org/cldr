---
title: Commit Checker
---

# Commit Checker

Commit checker is a tool to ensure that Jira tickets for the release are in sync with commits in Github.
It can validate any tickets which have commits in GitHub,
while tickets which are fixed elsewhere such as in the Survey Tool still must be validated manually.

## How to run the Commit Checker

1. Go to Actions · unicode-org/cldr (github.com)
2. Select Workflow: Commit-Checker

![Commit Checker Workflow](/images/development/commit-checker-workflow-fields.png)

3. Fill out the fields:
   - Branch: main
     - Always use main for branch 
   - CLDR Commit 
     - Run against main 
     - Use maint/maint-xx when the maint branch is created
   - Fix version
     - The current release version in development
   - Last version
     - Use the Dot release if the last released version of a Dot release. Use the Hyphen (e.g. 38-1)
    Commit Checker ICU Repo/ ICU branch/tag
4. Once the workflow completes, expand Run Check and Publish and find the report URL (e.g. https://cldr-smoke.unicode.org/***/CLDR-Report-YYYY-MM-DD-vXX-HASH.html)

## How to review the Commit Checker report

The Commit Checker report is separated into different categories. Commits can be included under more than one category.

### Categories

 - Closed Issues with No Commit
 - Closed Issues with Commit Policy Problems
 - Commits without Jira Issue Tag
 - Commits with Jira Issue Not Found
 - Commits with Open Jira Issue
 - Issue is under Review
 - Excluded Commits

### How to resolve tickets

1. Review tickets under **Closed Issues with No Commit** and update the resolution to match how the ticket was resolved. Only tickets which are resolved in GitHub should have a resolution of 'Fixed'.
2. Closed Issues with Commit Policy Problems, Commits without Jira Issue Tag, and Commits with Jira Issue Not Found (TBD)
3. Tickets in **Commits with Open Jira Issue** only need to be actioned on near the end of the release, although it is better to close tickets out as soon as they're complete to make sure that tickets aren't split across releases unnecessarily.
4. Tickets in **Issue is under Review** need to have review complete before the release is announced.

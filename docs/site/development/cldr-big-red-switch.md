---
title: 'CLDR: Big Red Switch'
---

# CLDR: Big Red Switch

The BRS (Big Red Switch) is the checklist of tasks that need to be completed in order to release the next version of CLDR.

For members of the CLDR TC: [Spreadsheet View](https://docs.google.com/spreadsheets/d/1D0wohmpmnW369UiTLeTm_UHT7y5mVISretDYM0m289I/edit?gid=169833626#gid=169833626&fvid=271364825)

The checklist can be filtered to see open tasks by going to Data > Change view and selecting the 'Skip/Done/Moot' filter.

TODO: List all major types of tasks with their definitions and link to pages with descriptions.

## Jira filters for release management and issue triage

### Ticket Triage

- [@New, by date](https://unicode-org.atlassian.net/issues/?filter=10033) - All untriaged issues sorted by date created.
- [@New, by fix version](https://unicode-org.atlassian.net/issues/?filter=10801) - All untriaged issues sorted by fix version when available.
- [Fix in Survey Tool](https://unicode-org.atlassian.net/issues/?filter=10999) - Open tickets that can be fixed by vetters in Survey Tool during submission.
- [@New + Agenda, by date](https://unicode-org.atlassian.net/issues/?filter=10802)
- [@Agenda](https://unicode-org.atlassian.net/issues/?filter=10158) - All issues tagged for TC discussion.
- [CLDR + ICU helpwanted](https://unicode-org.atlassian.net/issues/?filter=10202) - Tickets tagged as 'Help Wanted' by the CLDR or ICU Technical Committees.

### Ticket Reviewing

- [@Pending Reviews](https://unicode-org.atlassian.net/issues/?filter=10062) - All CLDR tickets currently in review.
- [@My Reviews](https://unicode-org.atlassian.net/issues/?filter=10179) - CLDR tickets in review assigned to oneself.

### Release tracking

#### Downloads page

- [latest-tickets](https://unicode-org.atlassian.net/issues/?filter=10838) - All issues with status 'Fixed' in the *latest version* (the most recently released version of CLDR).
- [dev-tickets](https://unicode-org.atlassian.net/issues/?filter=10837) - All issues with status 'Fixed' in the *dev* (the CLDR version currently under development).

#### General filters

- [@CURR CLDR (vXX)](https://unicode-org.atlassian.net/issues/?filter=10438) - All issues targeting the current release regardless of status.
- [@CLDR Known Issues](https://unicode-org.atlassian.net/issues/?filter=10237) - Current list of known issues.
- [@CURR mine — v48, current phase+](https://unicode-org.atlassian.net/issues/?filter=10074) - CLDR tickets in review assigned to oneself for the current and future phases of the release in development.

#### SBRS filters

- [@labels=vetter-data](https://unicode-org.atlassian.net/issues/?filter=10735) - All issues that are expected to impact vetters during data submission.
- [@SBRS My tickets Current Milestone (vXX)](https://unicode-org.atlassian.net/issues/?filter=10030) - CLDR SBRS tickets for the dev version assigned to oneself.

#### CLDR alpha filters

- [@CURR icu-data](https://unicode-org.atlassian.net/issues/?filter=10144) - Open tickets which impact data used by ICU, and therefore may be disruptive to ICU if changed too late.
- [@alpha-critical](https://unicode-org.atlassian.net/issues/?filter=10245) - Open tickets which must be done in preparation for public alpha.
- [@CURR - my open tickets not in brs, charts, docs-spec, site](https://unicode-org.atlassian.net/issues/?filter=10256) - Open tickets excluding docs, spec, charts and BRS tasks for current fix version.
- [@CURR - open tickets not in brs, charts, site, docs-spec, test-data](https://unicode-org.atlassian.net/issues/?filter=10230) - Open tickets excluding brs, charts, site, docs-spec, test-data for current fix version.

#### CLDR beta filters

- [@beta-critical](https://unicode-org.atlassian.net/issues/?filter=10247) - Open tickets which must be done in preparation for data beta or public beta.
- [@CURR docs-spec Accepted](https://unicode-org.atlassian.net/issues/?filter=10339) - All open CLDR spec tickets for the version currently in development.

## Contributor Message

For each release, we add names to the Unicode CLDR Acknowledgments page:

http://cldr.unicode.org/index/acknowledgments.

However, names are not automatically entered there, since some people may not wish to have their names listed. If your name is not there and you would like it to be, please send me your name as it should appear on that page. Your name should be in Latin characters, optionally with names in one or more other scripts in parentheses, such as "Vladimir Weinstein (Владимир Вајнштајн)"

**\-\-\-\- how to send this message: currently a crude process \-\-\-\-**

1. get list of those who contributed through Survey tool (Login as TC, under 'Manage Users', click 'Email Address of Users Who Participated' (shows all users, not just your org)
2. e\-mail that list **on BCC:** the above message with a subject line of "\[CLDR X.Y Contributor Message]", and a request to please keep the subject line intact.
3. Then, the subject line can be used to filter/locate the contributor requests.



# Contributing to CLDR

## Tips for contributing to CLDR

We appreciate contributions to the Unicode CLDR project. Here are a few tips to make navigating the CLDR issue tracker easier.

- **Data change requests** - CLDR rarely accepts PRs to change data that has already been vetted in the Survey Tool. The Unicode CLDR project relies on language specialists who review the data during an annual submission period. See [Requesting Changes][] for more information.
- **Reporting & fixing an issue** - If you are reporting an issue and have a proposed fix for it, first read Requesting Changes for information about what the Unicode CLDR Technical Committee requires for a ticket to be reviewed and [Requirements for merged PRs](#requirements-for-merged-prs)
- **Contributing via PRs in GitHub** - If you would like to contribute to the Unicode CLDR project and are not trying to fix specific issues, here is more information about types of tickets you may see in JIRA:
    - [Accepted tickets labelled with the helpwanted label][] - These tickets have been discussed and approved by a technical committee and are available for contributions for that project.
    - [Accepted tickets][] - These tickets have been approved by the CLDR Technical Committee. Please check with the current assignee by adding a comment on the ticket in JIRA if you are interested in working on it.
  	- [Fix in Survey Tool][] - These tickets require [review by language specialists during a Survey Tool general submission cycle][]. The CLDR Technical Committee will generally not approve PRs for changes of this type. You are welcome to do research and add supporting evidence on the ticket if it follows the guidance in [Requesting Changes][].
    - We don’t recommend working on tickets which have not yet been discussed or approved by the technical committee since it may disagree or request changes to the proposal before approving it, or it may decline it altogether.
    - Low quality PRs will be closed with minimal feedback. Repeated low quality PRs may result in the contributor being blocked.

## Overview of CLDR Project

The CLDR project consists of 3 aspects:

1. **Data:** We rely on contributions by native language speakers
that are most commonly used currently in the given country/region.
Most data is collected through the Survey Tool. See [How to contribute][].
    - If you are not a native language speaker,
	but have evidence of CLDR data being incorrect,
	file a [Jira][] ticket to report the issue.
    - For data that is not collected in the [Survey Tool],
	such as [supplemental data][], file a [Jira][] ticket
	and provide evidence for changing the data.
2. **Internationalization structure:** Requires a ticket to be changed.
    - For structural issues, note that many issues may be
	due to misunderstandings of the LDML spec.
	For example, `<pattern>¤#,##0.00</pattern>` has a specialized format
	where each character represents a special function.
    - File a [Jira][] ticket and provide evidence for the spec issues
	or to establish a new structure.
    - If your organization is not a [Unicode member][], consider becoming
	a member and working as a CLDR technical committee member.
3. **CLDR internal tooling** such as adding new tests or fixing tooling that
enable CLDR builds and releases. See [CLDR Developer][] introduction.
    - To contribute in internal tooling, [Contact][] us.
    - See [Tools source][] and [Repository Organization][]

### Areas where contributions are welcome

- `cldr-apps/`: (Survey Tool).
    Improvements here include:
  - UI:  performance improvements, visual and functional refinements
  - Additional checks (CheckCLDR) to validate CLDR data.
- `java/`: The core CLDR tooling.
    Improvements could include:
  - better documentation around code components
  - updates to use newer/different library dependencies
  - writing unit tests to improve code coverage
  - better documentation around command line tools

## Requirements for merged PRs

For all PRs these steps are required before the PR is merged:

- The PR and commits must reference a [Jira][] ticket
which has been accepted by the CLDR-TC.
Open a ticket if there is not already a relevant one open.
- A Contributor License Agreement (CLA) must be signed. For more information,
see [Contributor License Agreement](#contributor-license-agreement), below.
- All tests must pass (See below)
- The PR must be reviewed by a TC member.
- The PR should have a single commit, and the first line of the commit must
begin with the accepted Jira ticket number.

### Sample commit message

    CLDR-0000 Brief Description of Change

    Optionally, this is the first line of an extended description,
    after the blank line.
    - Here is an item
    - Here is another item

## Contributor License Agreement

In order to contribute to this project, the Unicode Consortium must have on file a Contributor License Agreement (CLA) covering your contributions, either an individual or a corporate CLA. Pull Requests, issues, and other contributions will not be merged/accepted until the correct CLA is signed. Which version needs to be signed depends on who owns the contribution being made: you as the individual making the contribution or your employer. **It is your responsibility to determine whether your contribution is owned by your employer.** Please review the [Unicode Intellectual Property, Licensing, & Technical Contribution Policy][policies] for further guidance on which CLA to sign, as well as other information and guidelines regarding the Consortium’s licensing and technical contribution policies and procedures.

To sign the CLA in Github, open a Pull Request (a comment will be automatically added with a link to the CLA Form), or go directly to [the CLA Form][sign-cla]. You may need to sign in to Github to see the entire CLA Form.

- **Individual CLA**: If you have determined that the Individual CLA is appropriate, then when you access the CLA Form, click the Individual CLA and complete the Form.

- **Corporate CLA**: If you have determined that a Corporate CLA is appropriate, please first check the [public list of Corporate CLAs][unicode-corporate-clas] that the Consortium has on file. If your employer is listed, then when you access the CLA Form, you can click the box indicating that you are covered by your employer’s corporate CLA. If your employer is not on the list, then it has not already signed a CLA and you will need to arrange for your employer to do so before you contribute, as described in [How to Sign a Unicode CLA][signing].

Unless otherwise noted in the [`LICENSE`](./LICENSE) file, this project is released under the [OSI-approved][osi-Unicode-License-3.0] free and open-source [Unicode License v3][unicode-license].

## Building

For setup details, see [Maven Setup][].
Builds are done with Maven:

    mvn test --file=tools/pom.xml

## Tests

For more detail, and how to run the tests locally,
see [Running Tests][] on the CLDR development site.

### Automatic Test Runs

Tests are automatically run once a commit is pushed to GitHub and when a PR
 is opened requesting changes to CLDR.
 You will see the status check next to each commit's hash,
 it will show as a circle, x, or checkmark. (●✖✔).
 For GitHub documentation, see [About Status Checks][].
 You can click on these indicators anywhere they appear for more details.

Briefly:

- ● An orange circle indicates that information is not available yet,
such as a test that is still running or has not started yet.
- ✖ A red X indicates a failure or a warning. Depending on the type of issue,
merging might be blocked.
- ✔ A green checkmark indicates success.

![Commit Checks](./docs/img/commit-checks.png)

Similarly, in a PR, certain checks are marked as “Required.”
All required checks must succeed before merging is allowed.

![PR Checks](./docs/img/pr-checks.png)

If a test or status check does not pass, see [Running Tests][]
on the CLDR development site.

## Copyright

Copyright &copy; 1991-2024 Unicode, Inc.
All rights reserved. [Terms of use][]

[Accepted tickets]: https://unicode-org.atlassian.net/issues?filter=11593
[Accepted tickets labelled with the helpwanted label]: https://unicode-org.atlassian.net/issues/?filter=11527
[Fix in Survey Tool]: https://unicode-org.atlassian.net/issues/?filter=10999
[review by language specialists during a Survey Tool general submission cycle]: https://cldr.unicode.org/#translations-and-other-language-data
[Requesting Changes]: https://cldr.unicode.org/requesting_changes#requesting-updates-to-locale-data-through-a-ticket
[Survey Tool]: https://cldr.unicode.org/index/survey-tool
[Terms of use]: https://www.unicode.org/copyright.html
[Jira]: https://github.com/unicode-org/cldr/blob/main/docs/requesting_changes.md
[Tools source]: https://github.com/unicode-org/cldr/tree/main/tools
[Maven setup]: https://cldr.unicode.org/development/maven
[Repository Organization]: https://cldr.unicode.org/index/downloads#h.lf1z45b9du36
[How to contribute]: https://cldr.unicode.org/#h.vw32p8sealpj
[Unicode member]: https://home.unicode.org/membership/why-join/
[supplemental data]: https://github.com/unicode-org/cldr/tree/main/common/supplemental
[About Status Checks]: https://docs.github.com/en/github/collaborating-with-issues-and-pull-requests/about-status-checks
[Running Tests]: https://cldr.unicode.org/development/cldr-development-site/running-tests
[policies]: https://www.unicode.org/policies/licensing_policy.html
[unicode-corporate-clas]: https://www.unicode.org/policies/corporate-cla-list/
[signing]: https://www.unicode.org/policies/licensing_policy.html#signing
[sign-cla]: https://cla-assistant.io/unicode-org/.github
[osi-Unicode-License-3.0]: https://opensource.org/license/unicode-license-v3/
[unicode-license]: https://www.unicode.org/license.txt
[CLDR Developer]: https://cldr.unicode.org/development/new-cldr-developers
[Contact]: https://www.unicode.org/reporting.html

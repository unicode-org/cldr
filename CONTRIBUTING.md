# Contributing to CLDR

## Overview

The CLDR project consists of 3 aspects:

1. **Data:** We rely on contributions by native language speakers that are most commonly used currently in the given country/region. Most data is collected through the Survey Tool. See [How to contribute][].
    - If you are not a native language speaker, but have evidence of CLDR data being incorrect, file a [Jira][] ticket to report the issue.
    - For data that is not collected in the [Survey Tool], such as [supplemental data][], file a [Jira][] ticket and provide evidence for changing the data.
2. **Internationalization structure:** Requires a ticket to be changed.
    - For structural issues, note that many issues may be due to misunderstandings of the LDML spec. For example, `<pattern>¤#,##0.00</pattern>` has a specialized format where each character represents a special function.
    - File a [Jira][] ticket and provide evidence for the spec issues or to establish a new structure.
    - If your organization is not a [Unicode member][], consider becoming a member and work as a CLDR technical committee member.
3. **CLDR internal tooling** such as adding new tests or fixing tooling that enable CLDR builds and releases. See [CLDR Developer][] introduction.
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

- The PR and commits must reference a [Jira][] ticket which has been accepted by the CLDR-TC. Open a ticket if there is not already a relevant one open.
- A Contributor License Agreement (CLA) must be signed. For more information, see [Contributor License Agreement](#contributor-license-agreement), below.
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

In order to contribute to this project, the Unicode Consortium must have on file a Contributor License Agreement (CLA) covering your contributions, either an individual or a corporate CLA. Pull Requests will not be merged until the correct CLA is signed. Which version needs to be signed depends on who owns the contribution being made: you as the individual making the contribution or your employer. _It is your responsibility to determine whether your contribution is owned by your employer._ Please review [The Unicode Consortium Intellectual Property, Licensing, and Technical Contribution Policies][policies] for further guidance on which CLA to sign, as well as other information and guidelines regarding the Consortium’s licensing and technical contribution policies and procedures.

- **Individual CLA**: If you have determined that the Individual CLA is appropriate, then when you open your first Pull Request, an automated comment will appear that contains a link you can follow to accept the Individual CLA.

- **Corporate CLA**: If you have determined that a Corporate CLA is appropriate, please check the [public list of Corporate CLAs][unicode-corporate-clas] that the Consortium has on file. If your employer has already signed a CLA, then when you open your first Pull Request, an automated comment will appear that contains a link you can follow to declare your affiliation with this employer. If your employer has not already signed a CLA, you will need to arrange for your employer to sign the Corporate CLA, as described in [How to Sign a Unicode CLA][signing].

Unless otherwise noted in the [LICENSE](./LICENSE) file, this project is released under the free and open-source [Unicode License][unicode-license], also known as Unicode, Inc. License Agreement - Data Files and Software.

## Building

For setup details, see [Maven Setup][].
Builds are done with Maven:

```
mvn test --file=tools/pom.xml
```

## Tests

For more detail, and how to run the tests locally, see [Running Tests][] on the CLDR development site.

### Automatic Test Runs

Tests are automatically run once a commit is pushed to GitHub and when a PR is opened requesting changes to CLDR. You will see the status check next to each commit's hash, it will show as a circle, x, or checkmark. (●✖✔). For GitHub documentation, see [About Status Checks][]. You can click on these indicators anywhere they appear for more details.

Briefly:

- ● An orange circle indicates that information is not available yet, such as a test that is still running or has not started yet.
- ✖ A red X indicates a failure or a warning. Depending on the type of issue, merging might be blocked.
- ✔ A green checkmark indicates success.

![Commit Checks](./docs/img/commit-checks.png)

Similarly, in a PR, certain checks are marked as “Required.” All required checks must succeed before merging is allowed.

![PR Checks](./docs/img/pr-checks.png)

If a test or status check does not pass, see [Running Tests][] on the CLDR development site.

## Copyright

Copyright &copy; 1991-2024 Unicode, Inc.
All rights reserved. [Terms of use][]

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
[unicode-license]: ./LICENSE

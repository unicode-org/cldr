# Unicode CLDR Project

For current CLDR release information, see [cldr.unicode.org](https://cldr.unicode.org/index/downloads/).

#### `main` branch:
[![cldr-mvn](https://github.com/unicode-org/cldr/workflows/cldr-mvn/badge.svg)](https://github.com/unicode-org/cldr/actions?query=branch%3Amain+workflow%3A%22cldr-mvn%22)
[![Ansible Lint](https://github.com/unicode-org/cldr/workflows/Ansible%20Lint/badge.svg)](https://github.com/unicode-org/cldr/actions?query=branch%3Amain+workflow%3A%22Ansible+Lint%22)
[![Publish to gh-pages](https://github.com/unicode-org/cldr/actions/workflows/gh-pages.yml/badge.svg)](https://github.com/unicode-org/cldr/actions/workflows/gh-pages.yml)
[![CodeQL](https://github.com/unicode-org/cldr/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/unicode-org/cldr/actions/workflows/codeql-analysis.yml)

## Status

Update: 2024-01-25

<!-- [inapplicable lines are commented out.]-->
**Note:**  CLDR 45 is in development and not recommended for use at this stage.
<!--**Note:**  This is the milestone 1 version of CLDR 45, intended for those wishing to do pre-release testing. It is not recommended for production use.-->
<!--**Note:** This is a preliminary version of CLDR 45, intended for those wishing to do pre-release testing. It is not recommended for production use.-->
<!--**Note:**  This is a pre-release candidate version of CLDR 45, intended for testing. It is not recommended for production use.-->
<!--This is the final release version of CLDR 45.-->

### What is CLDR?
The Unicode CLDR provides key building blocks for software to support the world's languages, with the largest and most extensive standard repository of locale data available. This data is used by a wide spectrum of companies for their software internationalization and localization, adapting software to the conventions of different languages for such common software tasks.

See for further information:

- [CLDR releases and downloads](https://cldr.unicode.org/index/downloads "CLDR Download Page"),
including the data files and LDML specification associated with each release

- [Repository organization](https://cldr.unicode.org/index/downloads#Repository_Organization "CLDR Download Page, Repository Organization"),
describing the organization of files within this repository

- [Building and running CLDR Tools](https://cldr.unicode.org/development/cldr-tools "CLDR Tools Page")

### Contributing

Most data submissions are done via the [CLDR Survey Tool](https://st.unicode.org/cldr-apps/), which is open and available on predetermined cycles.
Click [HERE](https://www.unicode.org/cldr/survey_tool.html) for information on how to obtain a survey tool account.

For details about code and other contributions, see [CONTRIBUTING.md](./CONTRIBUTING.md)

#### Spotless

A source formatter is now used, please see [spotless](./tools/README.md#spotless) for details.

### Copyright & Licenses

Copyright © 2004-2024 Unicode, Inc. Unicode and the Unicode Logo are registered trademarks of Unicode, Inc. in the United States and other countries.

The project is released under [LICENSE](./LICENSE).

Note that some CLDR tools depend on libraries managed via Maven; use of these libraries is governed by separate license agreements.

A CLA is required to contribute to this project - please refer to the [CONTRIBUTING.md](./CONTRIBUTING.md) file (or start a Pull Request) for more information.

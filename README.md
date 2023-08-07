# Unicode CLDR Project

Latest Release: [v43.1](https://cldr.unicode.org/index/downloads/cldr-43) published 2023-06-15

## Build Status

GitHub:
[![cldr-mvn](https://github.com/unicode-org/cldr/workflows/cldr-mvn/badge.svg)](https://github.com/unicode-org/cldr/actions?query=branch%3Amain+workflow%3A%22cldr-mvn%22)
[![Ansible Lint](https://github.com/unicode-org/cldr/workflows/Ansible%20Lint/badge.svg)](https://github.com/unicode-org/cldr/actions?query=branch%3Amain+workflow%3A%22Ansible+Lint%22)

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

### Licenses

This product is released under [LICENSE](./LICENSE)

Note that some CLDR tools depend on libraries managed via Maven; use of these libraries is governed by separate license agreements.

### Copyright

Copyright © 2004-2023 Unicode, Inc. Unicode and the Unicode Logo are registered trademarks of Unicode, Inc. in the United States and other countries.

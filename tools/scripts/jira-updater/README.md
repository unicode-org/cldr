# Jira Updater

on merged commits, updates the `Merged` field in Jira

called by the update-jira.yml workflow

## setup

Requires these secret env vars to be set:

- JIRA_HOST: top level host including https://
- JIRA_EMAIL: email for auth
- JIRA_APITOKEN: API token, get yours at <https://id.atlassian.com/manage-profile/security/api-tokens>

optional, JIRA_FIELD will override the field name from "Merged"

## LICENSE

Copyright © 2004-2025 Unicode, Inc. Unicode and the Unicode Logo are registered trademarks of Unicode, Inc. in the United States and other countries.

A CLA is required to contribute to this project - please refer to the [CONTRIBUTING.md](./CONTRIBUTING.md) file (or start a Pull Request) for more information.

The contents of this repository are governed by the Unicode [Terms of Use](https://www.unicode.org/copyright.html) and are released under [LICENSE](./LICENSE).

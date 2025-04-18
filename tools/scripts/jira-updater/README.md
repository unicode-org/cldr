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

Copyright © 2025 Unicode, Inc.
For terms of use, see <http://www.unicode.org/copyright.html>
SPDX-License-Identifier: Unicode-3.0

see [LICENSE](../../../LICENSE)

#!/usr/bin/env node

/**
 * Copyright © 2025 Unicode, Inc.
 * For terms of use, see http://www.unicode.org/copyright.html
 * SPDX-License-Identifier: Unicode-3.0
 */

import { Version3Client } from 'jira.js';
// import fs from 'node:fs';
import process from 'node:process';

const {
  // from repo
  JIRA_HOST, JIRA_EMAIL, JIRA_APITOKEN, JIRA_FIELD,
  // from workflow
  MERGED_TO,
  // Github stuff, we'll take what we can get
  GITHUB_REPOSITORY,
  GITHUB_ACTOR,
  GITHUB_SHA,
} = process.env;

// extract PR title from process (see workflow)
const [, , PR_TITLE] = process.argv;

const DONE_ICON = "✅";
const GEAR_ICON = "⚙️";
const TYPE_ICON = "📂";
const MISSING_ICON = "❌";
const LAND_ICON = "🛬";

if (!JIRA_HOST || !JIRA_EMAIL || !JIRA_APITOKEN) {
  throw Error(`${MISSING_ICON} Configuration error: set JIRA_HOST, JIRA_EMAIL, JIRA_APITOKEN`);
}

if (!PR_TITLE) throw Error(`${MISSING_ICON} PR_TITLE unset, something is wrong`);
if (!MERGED_TO) throw Error(`${MISSING_ICON} MERGED_TO unset, something is wrong`);

const DEBUG = false;

DEBUG && console.log(`Logging into ${config.host} as ${config.email}`);

const client = new Version3Client({
  host: JIRA_HOST,
  authentication: {
    basic: {
      email: JIRA_EMAIL,
      apiToken: JIRA_APITOKEN,
    },
  },
  newErrorHandling: true,
});

async function main() {
  let resp = /^([A-Z]+-[0-9]+)/.exec(PR_TITLE);
  if (!resp || !resp[1]) throw Error(`${MISSING_ICON} Could not find JIRA ticket in ${PR_TITLE}`);
  const issueIdOrKey = resp[1];
  // graceful behavior if the PR # is missing
  resp = /\(#([0-9]+)\)\s*$/.exec(PR_TITLE);
  let PR_NUMBER = undefined;
  if (resp && resp[1]) {
    PR_NUMBER = resp[1];
  }
  if (!PR_NUMBER) console.error(`${MISSING_ICON} Could not find PR# in ${PR_TITLE}`);
  const mergedTo = MERGED_TO.replace(/^refs\/heads\//,'');

  console.log(`${GEAR_ICON} Logging in`);

  try {
    await client.myself.getCurrentUser();
  } catch (e) {
    DEBUG && console.error(e);
    throw Error(`${MISSING_ICON} JIRA Authentication error: ${e}`);
  }

  console.log(`${TYPE_ICON} Updating ${issueIdOrKey} on Jira with a merge to ${mergedTo} from ${PR_NUMBER}`);

  const ourField = JIRA_FIELD || 'Merged'; // could be a config option later

  DEBUG && console.dir({ issueIdOrKey, mergedTo, ourField }); // our test setup

  const fields = (await client.issueFields.getFields())
    .filter(({ name }) => name === ourField);
  if (fields.length !== 1) {
    throw Error(`${MISSING_ICON} Looking for 1 custom field named ${ourField} but found ${fields.length}`);
  }
  const { id: fieldId } = fields[0];
  DEBUG && console.dir({ fieldId });
  try {
    const resp = await client.issues.editIssue({
      issueIdOrKey,
      update: {
        [fieldId]: [
          { add: mergedTo },
        ],
      },
    });
    DEBUG && console.dir(resp);
  } catch (e) {
    DEBUG && console.error(e);
    throw Error(`${MISSING_ICON} Error updating ${ourField}+${mergedTo} on ${issueIdOrKey}: ${e}`);
  }
  const digits = issueIdOrKey.split("-")[1];

  // the Jira hostname is redacted, so use an old redirect!
  console.log(`${DONE_ICON} Success: Updated ${ourField} += ${mergedTo} on https://unicode.org/cldr/trac/ticket/${digits}`);

  // now, let's add a comment
  await client.issueComments.addComment({
    issueIdOrKey,
    comment: {
      version: 1,
      type: "doc",
      content: [

        {
          "type": "heading",
          "attrs": {
            "level": 4
          },
          "content": [
            {
              "type": "text",
              "text": `${LAND_ICON} Merged PR`
            }
          ]
        },


        {
          type: "paragraph",
          content: [
            {
              type: "text",
              text: `@${GITHUB_ACTOR} merged a PR to ${GITHUB_REPOSITORY}:${mergedTo}`
            }
          ]
        },
        {
          type: "paragraph",
          content: [
            {
              type: "text",
              text: `${PR_TITLE}\n`
            },
            {
              "type": "inlineCard",
              "attrs": {
                "url": `https://github.com/${GITHUB_REPOSITORY}/pull/${PR_NUMBER || ''}`,
              }
            },
            {
              type: "text",
              text: `\n`
            },
            {
              "type": "inlineCard",
              "attrs": {
                "url": `https://github.com/${GITHUB_REPOSITORY}/commit/${GITHUB_SHA}`,
              }
            }
          ]
        }

      ]
    }
  });
}

main();

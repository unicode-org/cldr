<script setup>
import { reactive, ref } from "vue";

import * as cldrText from "../esm/cldrText.mjs";

// Currently cldrText has "Org." but not "User"
const orgColumnHeader = cldrText.get("voteInfo_orgColumn");

// For "(no votes)", prevent line break between "no" and "votes"
const noVotes = cldrText.get("voteInfo_noVotes").replace(" ", "\u00A0");

let totalVoteCount = ref(0);
let voteRows = null;
let gotVoteRows = ref(false);
let baileyClass = ref("");
let gotNoVotes = ref(false);

function setData(data) {
  totalVoteCount.value = data.totalVoteCount;
  voteRows = reactive(data.voteRows);
  gotVoteRows.value = Boolean(data.voteRows);
  baileyClass.value = data.baileyClass;
  gotNoVotes.value = Boolean(!data.voteRows[0].voteCount);
}

function abbreviateDetails(row) {
  // Example: "-23 B" where -23 means 23 days ago; B means BULK_UPLOAD
  const daysAgo = row.details?.daysAgo;
  let abbreviation = !daysAgo ? "0" : "-" + daysAgo;
  // Based on server enum VoteType: NONE, UNKNOWN, DIRECT, AUTO_IMPORT, MANUAL_IMPORT, BULK_UPLOAD.
  // NONE and UNKNOWN should not occur unless the vote was years ago.
  // MANUAL_IMPORT votes are imported anonymous zero votes.
  // DIRECT is default; don't show anything for DIRECT. Otherwise, show first letter, like "A" for AUTO_IMPORT.
  const type = row.details?.voteType || "?";
  if (type !== "DIRECT") {
    // U+202F NARROW NO-BREAK SPACE
    abbreviation += "\u202F" + type.charAt(0);
  }
  return abbreviation;
}

function describeDaysAgo(row) {
  const daysAgo = row.details?.daysAgo;
  if (!daysAgo) {
    return "Less than one day ago";
  } else if (Number(daysAgo) === 1) {
    return "About one day ago";
  } else {
    return daysAgo + " days ago";
  }
}

function describeVoteType(row) {
  // Display, for example, "AUTO_IMPORT" as "AUTO IMPORT"
  const type = row.details?.voteType.replace("_", " ") || "?";
  return "Vote type: " + type;
}

function shortName(name) {
  // Very long names (which sometimes are email addresses) tend to
  // make the table too wide, causing horizontal scrollbars or otherwise
  // making the table hard to read. The threshold length is roughly
  // based on observation with current data/fonts/layout.
  return name.length < 21 ? name : name.substring(0, 18) + "...";
}

function sendEmail(row) {
  location.href = "mailto:" + row.email;
}

function voteCountClass(row) {
  let c = "centerVoteCount";
  if (baileyClass.value) {
    c += " " + baileyClass.value;
  }
  // If an org has more than one row, only the first (top) such row counts
  // towards the total. Subsequent rows for the same org have blank in
  // the Org. column (meaning "same") and they have a different style
  // (like italic) for the number.
  c += row.org ? " orgTop" : " orgSame";
  return c;
}

defineExpose({
  setData,
});
</script>

<template>
  <div class="hideOverflow">
    <table v-if="gotVoteRows">
      <thead>
        <tr>
          <th>{{ orgColumnHeader }}</th>
          <th class="userColumn">User</th>
          <th></th>
          <th class="centerVoteCount">
            {{ totalVoteCount }}
          </th>
        </tr>
      </thead>
      <tbody v-if="gotNoVotes">
        <tr>
          <td>{{ noVotes }}</td>
          <td></td>
          <td></td>
          <td></td>
        </tr>
      </tbody>
      <tbody v-else>
        <tr v-for="row of voteRows" :key="row">
          <td>{{ row.voteCount ? row.org : noVotes }}</td>
          <td class="userColumn">
            <div v-if="row.email.includes('@')">
              <a-popover>
                <template #content
                  ><div class="centerEmail">
                    {{ row.userName }}
                    <br />
                    {{ row.email }}
                    <br />
                    <a-button @click="sendEmail(row)">Send email</a-button>
                  </div></template
                >
                {{ shortName(row.userName) }}
              </a-popover>
            </div>
            <div v-else>
              {{ shortName(row.userName) }}
            </div>
          </td>
          <td>
            <a-popover placement="topRight">
              <template #content
                ><div>
                  {{ describeDaysAgo(row) }}<br />{{ describeVoteType(row) }}
                </div>
              </template>
              {{ abbreviateDetails(row) }}
            </a-popover>
          </td>
          <td :class="voteCountClass(row)">{{ row.voteCount }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.hideOverflow {
  overflow-x: hidden;
}

table {
  width: 100%;
  margin-top: 10px;
  margin-bottom: 2px;
}

th,
td {
  padding-bottom: 2px !important;
  padding-top: 2px !important;
  padding-left: 4px !important;
  padding-right: 4px !important;
}

td {
  border-top: 1px solid black;
}

.userColumn {
  width: 100%;
}

.centerEmail {
  text-align: center;
}

.centerVoteCount {
  text-align: center;
}

.orgTop {
  font-weight: bold;
}

.orgSame {
  font-style: italic;
}
</style>

/*
 * cldrVoteTable: add table showing votes in Info Panel
 */
import * as cldrNotify from "./cldrNotify.mjs";
import * as cldrSurvey from "../esm/cldrSurvey.mjs";
import * as cldrText from "../esm/cldrText.mjs";
import * as cldrUserLevels from "./cldrUserLevels.mjs";
import * as cldrVue from "./cldrVue.mjs";

import InfoVoting from "../views/InfoVoting.vue";

function add(containerEl, votingResults, item, value, totalVoteCount) {
  try {
    const voteRows = addRows(votingResults, item, value);
    if (!voteRows?.length) {
      console.log("cldrVoteTable: no rows added; value = " + value);
      return;
    }
    /*
     * baileyClass is for inherited items, which may get a colored background on the vote count matching the colored
     * background of the candidate value: "alias", "fallback", "fallback_root", etc.
     */
    const baileyClass =
      item.rawValue === cldrSurvey.INHERITANCE_MARKER ? item.pClass : "";
    const wrapper = cldrVue.mount(InfoVoting, containerEl);
    wrapper.setData({
      totalVoteCount: totalVoteCount,
      voteRows: voteRows,
      baileyClass: baileyClass,
    });
  } catch (e) {
    console.error("Error loading InfoVoting vue " + e.message + " / " + e.name);
    cldrNotify.exception(e, "while loading InfoVoting");
  }
}

function addRows(votingResults, item, value) {
  const itemVotesLength = item.votes ? Object.keys(item.votes).length : 0;
  const anon =
    itemVotesLength == 1 &&
    cldrUserLevels.match(
      item.votes[Object.keys(item.votes)[0]].level,
      cldrUserLevels.ANONYMOUS
    );
  const voteRows = [];
  if (itemVotesLength == 0 || anon) {
    addAnonVote(voteRows, anon);
  } else {
    updateRowVoteInfoForAllOrgs(voteRows, votingResults, value, item);
  }
  return voteRows;
}

function addAnonVote(voteRows, anon) {
  const noVotes = cldrText.get("voteInfo_noVotes");
  const anonVoter = anon ? cldrText.get("voteInfo_anon") : "";
  const voteInfo = {
    voteCount: noVotes,
    userName: anonVoter,
    email: "",
    details: null,
  };
  voteRows.push(makeVoteObj("" /* org */, voteInfo));
}

/**
 * Update the vote info for one candidate item in this row, looping through all the orgs.
 *
 * @param {Array} voteRows the array of voteRow objects to append to
 * @param {Object} votingResults the voting results
 * @param {String} value the value of the candidate item
 * @param {Object} item the candidate item
 */
function updateRowVoteInfoForAllOrgs(voteRows, votingResults, value, item) {
  for (let org in votingResults.orgs) {
    // org is a string like "microsoft"
    const theOrg = votingResults.orgs[org]; // theOrg is an object
    const orgVoteValue = theOrg.votes[value]; // orgVoteValue is a number

    // This function is not called with "anonymous" imported losing votes (orgVoteValue === 0)
    // so we don't need to handle them here or distinguish 0/null/undefined for orgVoteValue.
    if (orgVoteValue) {
      // Someone in the org actually voted for it
      let topVoter = null; // top voter for this item
      let topVoterTime = 0; // Calculating the latest time for a user from same org
      // find a top-ranking voter to use for the top line
      for (let voter in item.votes) {
        const voteInfo = item.votes[voter];
        if (
          voteInfo.org == org &&
          getActualVoteCount(voteInfo) == theOrg.votes[value]
        ) {
          if (topVoterTime != 0) {
            // Get the latest time vote only
            if (
              votingResults.nameTime[`#${topVoter}`] <
              votingResults.nameTime[`#${voter}`]
            ) {
              topVoter = voter;
              topVoterTime = votingResults.nameTime[`#${topVoter}`];
            }
          } else {
            topVoter = voter;
            topVoterTime = votingResults.nameTime[`#${topVoter}`];
          }
        }
      }
      if (!topVoter) {
        // just find someone in the right org
        for (let voter in item.votes) {
          if (item.votes[voter].org == org) {
            topVoter = voter;
            break;
          }
        }
      }
      voteRows.push(makeVoteObj(org, item.votes[topVoter]));
      for (let voter in item.votes) {
        if (item.votes[voter].org != org || voter == topVoter) {
          continue; // skip; wrong org or already done
        }
        voteRows.push(makeVoteObj("" /* org */, item.votes[voter]));
      }
    }
  }
}

function makeVoteObj(org, voteInfo) {
  return {
    org: org,
    voteCount: getActualVoteCount(voteInfo),
    userName: getName(voteInfo),
    email: getEmail(voteInfo),
    details: voteInfo.voteDetails,
  };
}

function getActualVoteCount(voteInfo) {
  return voteInfo.voteDetails?.override || voteInfo.votes;
}

function getName(voteInfo) {
  return voteInfo?.name || cldrText.get("emailHidden");
}

function getEmail(voteInfo) {
  return voteInfo?.email
    ? voteInfo.email.replace(" (at) ", "@")
    : cldrText.get("emailHidden");
}

export { add };

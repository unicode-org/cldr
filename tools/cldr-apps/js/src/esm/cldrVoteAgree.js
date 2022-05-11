/**
 * cldrVoteAgree: enable voting with a Forum "Agree" button
 *
 * The back end automatically creates "Agree" forum posts when a user votes for a value that
 * was requested. Therefore, the front end only needs to submit the vote, not to make any explicit
 * request to submit a forum post.
 *
 * The existing event handling and client-server communication for both voting (cldrVote, cldrTable)
 * and forum posting (cldrForum) are complex and tightly coupled with the GUI, including dependence
 * on values stored in the DOM. The implementation of this module accepts those dependencies,
 * including the assumption that the page and row for the path in question need to be visible when
 * a vote is submitted. As the related modules get modernized it should become possible to avoid the
 * heavy-handed use of cldrLoad.reloadV, and passing the DOM element "tr" to cldrVote.handleWiredClick.
 *
 * "Agree" buttons occur in forum posts in two GUI contexts: (1) the Info Panel shown on the
 * right side of the Page table; (2) the Forum special page (with #forum in the URL). The row is
 * normally already visible in the first context, but not in the second. The current implementation
 * is intended to work in both contexts.
 */
import * as cldrLoad from "./cldrLoad.js";
import * as cldrStatus from "./cldrStatus.js";
import * as cldrSurvey from "./cldrSurvey.js";
import * as cldrText from "./cldrText.js";
import * as cldrVote from "./cldrVote.js";

import { notification } from "ant-design-vue";

/**
 * A map from locale and path to values for which the user has agreed to vote
 *
 * A mapping gets added when the user presses an Agree button, then removed when
 * the row in question is loaded and a vote is submitted
 */
const rememberToVote = {};

/**
 * The user has pressed a Forum "Agree" button in either the Info Panel or the
 * special #forum page
 *
 * Arrange to load the page view for this path and then vote for the given value
 *
 * @param {String} locale - the locale in which to vote
 * @param {String} xpstrid - the hex xpath id
 * @param {String} value - the value to vote for
 */
function vote(locale, xpstrid, value) {
  rememberToVote[agreeKey(locale, xpstrid)] = value;
  cldrStatus.setCurrentLocale(locale);
  cldrStatus.setCurrentId(xpstrid);
  cldrStatus.setCurrentSpecial("");
  cldrStatus.setCurrentPage("");
  // cldrLoad.reloadV will cause the page table to be loaded,
  // and then cldrTable will call voteIfAgreed for each row
  cldrLoad.reloadV();
}

/**
 * If we're waiting to make an "Agree" vote for this row, schedule it to happen
 *
 * This is called from cldrTable when the row for each path is loaded
 *
 * @param {Element} tr - a DOM element to pass to handleWiredClick
 * @param {Object} theRow - an object describing the row
 */
function voteIfAgreed(tr, theRow) {
  const key = agreeKey(cldrStatus.getCurrentLocale(), theRow.xpstrid);
  const value = rememberToVote[key];
  if (!value) {
    return;
  }
  delete rememberToVote[key];
  for (let item of Object.values(theRow.items)) {
    if (value === getValueOrInherited(item.value, theRow.inheritedValue)) {
      // set a timeout, to allow the table to finish loading before submitting the vote
      setTimeout(reallyVote(tr, theRow, value, item.valueHash), 1);
      return;
    }
  }
  notifyNotFound(value);
}

function reallyVote(tr, theRow, value, valueHash) {
  const pseudoButton = {
    value: value,
  };
  cldrVote.handleWiredClick(tr, theRow, valueHash, undefined, pseudoButton);
}

function agreeKey(locale, xpstrid) {
  return locale + "-" + xpstrid;
}

function getValueOrInherited(value, inheritedValue) {
  return value === cldrSurvey.INHERITANCE_MARKER ? inheritedValue : value;
}

function notifyNotFound(value) {
  const message = cldrText.get("vote_agree_not_found");
  const description = cldrText.sub("vote_agree_value_not_found", [value]);
  notification.warning({
    message: message,
    description: description,
    duration: 8,
  });
}

export { vote, voteIfAgreed };

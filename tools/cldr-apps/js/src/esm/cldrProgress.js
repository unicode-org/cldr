/*
 * cldrProgress: for Survey Tool progress indicators, encapsulate code
 * including getting/updating data through interaction with other modules
 * and the server. The display logic is in ProgressMeters.vue.
 */
import * as cldrAjax from "./cldrAjax.js";
import * as cldrCoverage from "./cldrCoverage.js";
import * as cldrGui from "./cldrGui.js";
import * as cldrLoad from "./cldrLoad.js";
import * as cldrStatus from "./cldrStatus.js";
import * as cldrSurvey from "./cldrSurvey.js";
import * as cldrText from "./cldrText.js";

import ProgressMeters from "../views/ProgressMeters.vue";

import { createCldrApp } from "../cldrVueRouter";

import { notification } from "ant-design-vue";

const USE_NEW_PROGRESS_WIDGET = true;
const CAN_GET_VOTER_PROGRESS = true;
const CAN_GET_LOCALE_PROGRESS = false; // For now

let progressWrapper = null;

let pageProgressStats = null;
let voterProgressStats = null;
let localeProgressStats = null;

let pageProgressRows = null;

class MeterData {
  /**
   * Construct a new MeterData object
   *
   * @param {String} description description of this meter
   * @param {Number} votes number of votes for this meter
   * @param {Number} total number of total votes for this meter
   * @param {String} level coverage level
   * @returns
   */
  constructor(description, votes, total, level) {
    this.description = description;
    this.votes = votes || 0;
    this.total = total || 0;
    this.level = level || cldrText.get("coverage_unknown");
    if (!description) {
      this.percent = 0;
      this.title = "No data";
      return;
    }
    this.percent = friendlyPercent(votes, total);
    const locale = cldrStatus.getCurrentLocale();
    const localeName = cldrLoad.getLocaleName(locale);
    this.title =
      `${this.description}: ${this.votes} / ${this.total} ≈ ${this.percent}%` +
      `\n•Locale: ${localeName} (${locale})` +
      `\n•Coverage: ${this.level}`;
  }

  /**
   * @returns {Number} percentage [0…100]
   */
  getPercent() {
    return this.percent;
  }

  /**
   * @returns {String} User visible title
   */
  getTitle() {
    return this.title;
  }

  /**
   * @returns {Boolean} True if data present, otherwise false
   */
  getPresent() {
    return !!this.description;
  }
}

/**
 * Create the ProgressMeters component
 *
 * @param spanId the id of the element that will contain the new component
 */
function insertWidget(spanId) {
  if (!USE_NEW_PROGRESS_WIDGET) {
    return;
  }
  hideLegacyCompletionWidget();
  try {
    const fragment = document.createDocumentFragment();
    progressWrapper = createCldrApp(ProgressMeters).mount(fragment);
    const el = document.getElementById(spanId);
    el.replaceWith(fragment);
  } catch (e) {
    console.error(
      "Error loading ProgressMeters vue " + e.message + " / " + e.name
    );
    notification.error({
      message: `${e.name} while loading ProgressMeters.vue`,
      description: `${e.message}`,
      duration: 0,
    });
  }
}

/**
 * The user's coverage level has changed. Inform all widgets we know about that need
 * updating to reflect the change.
 */
function updateWidgetsWithCoverage() {
  if (progressWrapper) {
    if (pageProgressRows) {
      // For page meter, we have saved a pointer to the rows; we need to recalculate
      // the votes and total based on the coverage level of each row
      pageProgressStats = getPageCompletionFromRows(pageProgressRows);
    }
    // For voter meter, the back end delivers data along with dashboard, and dashboard
    // itself gets updated when coverage changes, and updateVoterCompletion is called.

    // No reason to fetch the locale data here, it is unaffected by
    // the coverage setting.
  }
  refresh();
}

function refresh() {
  if (!USE_NEW_PROGRESS_WIDGET) {
    if (pageProgressStats) {
      updateLegacyCompletionWidget(pageProgressStats);
    }
    return;
  }
  refreshPageMeter();
  refreshVoterMeter();
  refreshLocaleMeter();
  if (pageProgressStats) {
    progressWrapper?.setHidden(false);
  }
}

function refreshPageMeter() {
  if (pageProgressStats) {
    const md = new MeterData(
      cldrText.get("progress_page"),
      pageProgressStats.votes,
      pageProgressStats.total,
      pageProgressStats.level
    );
    progressWrapper?.updatePageMeter(md);
  }
}

function refreshVoterMeter() {
  if (voterProgressStats) {
    if (!cldrGui.dashboardIsVisible()) {
      voterProgressStats = null;
      progressWrapper?.updateVoterMeter(new MeterData());
    } else {
      progressWrapper?.updateVoterMeter(
        new MeterData(
          cldrText.get("progress_voter"),
          voterProgressStats.votes,
          voterProgressStats.total,
          voterProgressStats.level
        )
      );
    }
  }
}

function refreshLocaleMeter() {
  if (localeProgressStats) {
    progressWrapper?.updateLocaleMeter(
      new MeterData(
        cldrText.get("progress_all_vetters"),
        localeProgressStats.votes,
        localeProgressStats.total,
        localeProgressStats.level
      )
    );
  }
}

/**
 * Update the page completion meter
 *
 * @param {Object} rows - the object whose values are rows from json; or null if all read-only
 */
function updatePageCompletion(rows) {
  pageProgressRows = rows;
  pageProgressStats = getPageCompletionFromRows(rows);
  refresh();
}

function getPageCompletionFromRows(rows) {
  if (!cldrStatus.getSurveyUser()) {
    return null;
  }
  const locale = cldrStatus.getCurrentLocale();
  if (!locale) {
    return null;
  }
  const levelNumber = cldrCoverage.effectiveCoverage(locale); // an integer
  const level = cldrCoverage.effectiveName(locale); // a string
  if (!levelNumber || !level) {
    return null;
  }
  return getPageStats(rows, levelNumber, level);
}

/**
 * Get the stats (votes and total) for the given set of rows and the given
 * coverage level
 *
 * Count only rows for which row.coverageValue <= cov
 * AND statusAction.vote (as calculated below) is truthy.
 * A row has been voted on by this user if row.hasVoted is truthy.
 * A row can be voted on by this user if statusAction.vote is truthy.
 *
 * Unfortunately there is duplication of logic here and in cldrTable.
 * Legacy code in cldrTable relies on storing essential data only in the DOM and
 * then reading the DOM to retrieve that data.
 *
 * @param {Object} rows - the object whose values are rows from json; or null if all read-only
 * @param {Number} levelNumber - the coverage level as an integer like 100
 * @param {String} level - the coverage level as a name like "comprehensive"
 *
 * @return an object with votes, total, and level
 */
function getPageStats(rows, levelNumber, level) {
  let votes = 0;
  let total = 0;
  if (rows) {
    for (let row of Object.values(rows)) {
      if (parseInt(row.coverageValue) > levelNumber) {
        continue;
      }
      const statusAction = cldrSurvey.parseStatusAction(row.statusAction);
      if (!statusAction?.vote) {
        continue;
      }
      ++total;
      if (row.hasVoted) {
        ++votes;
      }
    }
  }
  return {
    votes,
    total,
    level,
  };
}

function updateCompletionOneVote(hasVoted) {
  if (pageProgressStats || voterProgressStats) {
    updateStatsOneVote(pageProgressStats, hasVoted);
    updateStatsOneVote(voterProgressStats, hasVoted);
    refresh();
  }
  fetchLocaleData(false); // refresh 3rd meter if there is a vote
}

function updateStatsOneVote(stats, hasVoted) {
  if (stats) {
    if (hasVoted) {
      if (stats.votes < stats.total) {
        stats.votes++;
      }
    } else {
      if (stats.votes > 0) {
        stats.votes--;
      }
    }
  }
}

/**
 * Update the voter completion meter
 *
 * @param {Object} json - json.voterProgress has two fields for voter completion:
 *                      votedPathCount, votablePathCount
 *
 * The json is actually the dashboard data, with voter progress data as part of the same response
 */
function updateVoterCompletion(json) {
  if (
    !CAN_GET_VOTER_PROGRESS ||
    !progressWrapper ||
    !cldrStatus.getSurveyUser()
  ) {
    return;
  }
  if (
    typeof json.voterProgress.votedPathCount === "undefined" ||
    typeof json.voterProgress.votablePathCount === "undefined"
  ) {
    console.log("Warning: bad json in cldrProgress.updateVoterCompletion");
    return;
  }
  updateVoterStats(
    json.voterProgress.votedPathCount,
    json.voterProgress.votablePathCount
  );
}

function updateVoterStats(votes, total) {
  const locale = cldrStatus.getCurrentLocale();
  if (!locale) {
    return;
  }
  const level = cldrCoverage.effectiveName(locale);
  if (!level) {
    return;
  }
  progressWrapper.setHidden(false);
  voterProgressStats = {
    votes: votes,
    total: total,
    /*
     * Here level is NOT from json
     */
    level: level,
  };
  refreshVoterMeter();
}

/**
 *
 * @param {Boolean} unlessLoaded if true, skip if already present
 * @returns
 */
function fetchLocaleData(unlessLoaded) {
  if (
    !CAN_GET_LOCALE_PROGRESS ||
    !progressWrapper ||
    !cldrStatus.getSurveyUser()
  ) {
    return;
  }
  const locale = cldrStatus.getCurrentLocale();
  if (!locale) {
    return; // no locale
  }
  if (
    unlessLoaded &&
    localeProgressStats &&
    localeProgressStats.locale === locale
  ) {
    // LocaleMeter is already set
    // TODO: still refresh if it's been too long
    return;
  }
  progressWrapper.setHidden(false);
  reallyFetchLocaleData(locale);
}

function reallyFetchLocaleData(locale) {
  const url = `api/completion/locale/${locale}`;
  cldrAjax
    .doFetch(url)
    .then((response) => {
      if (!response.ok) {
        progressWrapper.setHidden(true);
        throw Error(response.statusText);
      }
      return response;
    })
    .then((data) => data.json())
    .then((json) => {
      progressWrapper.setHidden(false);
      localeProgressStats = {
        votes: json.votes,
        total: json.total,
        level: json.level,
        locale,
      };
      refreshLocaleMeter();
    })
    .catch((err) => {
      console.error("Error loading Locale Completion data: " + err);
      progressWrapper.setHidden(true);
    });
}

function friendlyPercent(votes, total) {
  if (!total) {
    // The task is finished since nothing needed to be done
    // Do not divide by zero (0 / 0 = NaN%)
    return 100;
  }
  if (!votes) {
    return 0;
  }
  // Do not round 99.9 up to 100
  const floor = Math.floor((100 * votes) / total);
  if (!floor) {
    // Do not round 0.001 down to zero
    // Instead, provide indication of slight progress
    return 1;
  }
  return floor;
}

function hideLegacyCompletionWidget() {
  const el = document.getElementById("legacyCompletionSpan");
  if (el) {
    el.style.display = "none";
  }
}

function updateLegacyCompletionWidget(pageVotesTotal) {
  const votes = pageVotesTotal.votes;
  const total = pageVotesTotal.total;
  const abstain = total - votes;
  document.getElementById("count-total").innerHTML = total;
  document.getElementById("count-abstain").innerHTML = abstain;
  document.getElementById("count-voted").innerHTML = votes;
  if (total === 0) {
    total = 1; // avoid division by zero
  }
  document.getElementById("progress-voted").style.width =
    (votes * 100) / total + "%";
  document.getElementById("progress-abstain").style.width =
    (abstain * 100) / total + "%";
}

export {
  MeterData,
  fetchLocaleData,
  insertWidget,
  refresh,
  updateCompletionOneVote,
  updatePageCompletion,
  updateVoterCompletion,
  updateWidgetsWithCoverage,
};

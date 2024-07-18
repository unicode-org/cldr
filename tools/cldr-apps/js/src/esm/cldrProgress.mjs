/*
 * cldrProgress: for Survey Tool progress indicators, encapsulate code
 * including getting/updating data through interaction with other modules
 * and the server. The display logic is in ProgressMeters.vue.
 */
import * as cldrAjax from "./cldrAjax.mjs";
import * as cldrCoverage from "./cldrCoverage.mjs";
import * as cldrDashContext from "./cldrDashContext.mjs";
import * as cldrNotify from "./cldrNotify.mjs";
import * as cldrSchedule from "./cldrSchedule.mjs";
import * as cldrStatus from "./cldrStatus.mjs";
import * as cldrSurvey from "./cldrSurvey.mjs";
import * as cldrText from "./cldrText.mjs";
import * as cldrVue from "./cldrVue.mjs";

import ProgressMeters from "../views/ProgressMeters.vue";

const CLDR_PROGRESS_DEBUG = false;

const LOCALE_METER_REFRESH_SECONDS = 60; // one minute

let progressWrapper = null;

let pageProgressStats = null;
let voterProgressStats = null;
let localeProgressStats = null;

let pageProgressRows = null;

const schedule = new cldrSchedule.FetchSchedule(
  "cldrProgress",
  LOCALE_METER_REFRESH_SECONDS,
  CLDR_PROGRESS_DEBUG
);

class MeterData {
  /**
   * Construct a new MeterData object
   *
   * @param {String} description description of this meter
   * @param {Number} done the number of tasks completed for this meter; maybe negative for locale meter
   *                      if new tasks (error/missing/provisional items) have been introduced
   * @param {Number} total the total number of tasks for this meter
   * @param {String} level coverage level
   * @returns the new MeterData object
   */
  constructor(description, done, total, level) {
    this.description = description;
    this.done = done || 0;
    this.total = total || 0;
    this.level = level || cldrText.get("coverage_unknown");
    this.exceptional = false;
    if (!description) {
      this.percent = 0;
      this.title = "No data";
      return;
    }
    this.percent = friendlyPercent(done, total);
    this.title = this.makeTitle();
  }

  makeTitle() {
    let newTitle =
      `${this.description}: ${this.done} / ${this.total} ≈ ${this.percent}%` +
      `\n•Coverage: ${this.level}`;
    if (this.done < 0) {
      newTitle += "\n•Note: " + cldrText.get("progress_negative");
    }
    return newTitle;
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
   * @returns {Boolean} True if the meter should be displayed, otherwise false
   */
  isVisible() {
    return !!this.description;
  }

  /**
   * @returns {Boolean} True if the meter state is exceptional, otherwise false
   */
  isExceptional() {
    return this.exceptional;
  }

  /**
   * Convert this into an exceptional meter
   *
   * Instead of percent completion, the meter will have a minimal/disabled appearance,
   * and a special message will be shown for its title (hover)
   *
   * @param {String} message
   */
  makeExceptional(message) {
    this.exceptional = true;
    this.percent = 0;
    this.title = `${this.description}: ${message}`;
  }
}

/**
 * Create the ProgressMeters component
 *
 * @param spanId the id of the element that will be replaced by the new component
 */
function insertWidget(spanId) {
  try {
    const el = document.getElementById(spanId);
    progressWrapper = cldrVue.mountReplace(ProgressMeters, el);
  } catch (e) {
    console.error(
      "Error loading ProgressMeters vue " + e.message + " / " + e.name
    );
    cldrNotify.exception(e, "while loading ProgressMeters");
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
      // votes and total based on the coverage level of each row
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
      pageProgressStats.votes /* done */,
      pageProgressStats.total,
      pageProgressStats.level
    );
    progressWrapper?.updatePageMeter(md);
  }
}

function refreshVoterMeter() {
  if (voterProgressStats) {
    if (!cldrDashContext.isVisible()) {
      voterProgressStats = null;
      progressWrapper?.updateVoterMeter(new MeterData());
    } else {
      progressWrapper?.updateVoterMeter(
        new MeterData(
          cldrText.get("progress_voter"),
          voterProgressStats.votes /* done */,
          voterProgressStats.total,
          voterProgressStats.level
        )
      );
    }
  } else if (pageProgressStats) {
    // If we don't have the Voter stats yet, but the Page meter is visible,
    // then display the Voter meter in an exceptional way
    const voterMeter = new MeterData(cldrText.get("progress_voter"));
    voterMeter.makeExceptional(cldrText.get("progress_voter_disabled"));
    progressWrapper?.updateVoterMeter(voterMeter);
  }
}

function refreshLocaleMeter() {
  if (localeProgressStats && progressWrapper) {
    const baselineProblems = localeProgressStats.baselineCount;
    const currentProblems =
      localeProgressStats.error +
      localeProgressStats.missing +
      localeProgressStats.provisional;
    const solvedProblems = baselineProblems - currentProblems; // can be negative!

    progressWrapper.updateLocaleMeter(
      new MeterData(
        cldrText.get("progress_all_vetters"),
        solvedProblems /* done */,
        baselineProblems /* total */,
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
  if (!progressWrapper || !cldrStatus.getSurveyUser()) {
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
 * Fetch locale data from the back end
 *
 * @param {Boolean} unlessLoaded if true, skip if data is already present
 */
function fetchLocaleData(unlessLoaded) {
  if (!progressWrapper || !cldrStatus.getSurveyUser()) {
    return;
  }
  const locale = cldrStatus.getCurrentLocale();
  if (!locale || locale === "USER") {
    return; // no locale
  }
  if (unlessLoaded && localeProgressStats?.locale === locale) {
    // LocaleMeter is already set
    // Only refresh if it's been long enough since last request and last response
    if (schedule.tooSoon()) {
      return;
    }
  }
  progressWrapper.setHidden(false);
  reallyFetchLocaleData(locale);
}

function reallyFetchLocaleData(locale) {
  schedule.setRequestTime();
  const url = `api/completion/locale/${locale}`;
  cldrAjax
    .doFetch(url)
    .then((response) => {
      if (!response.ok) {
        progressWrapper.setHidden(true);
        throw new Error(response.statusText);
      }
      return response;
    })
    .then((data) => data.json())
    .then((json) => {
      schedule.setResponseTime();
      progressWrapper.setHidden(false);
      setLocaleProgressStatsFromJson(json, locale);
      refreshLocaleMeter();
    })
    .catch((err) => {
      console.error("Error loading Locale Completion data: " + err);
      progressWrapper.setHidden(true);
    });
}

function setLocaleProgressStatsFromJson(json, locale) {
  localeProgressStats = {
    baselineCount: json.baselineCount,
    error: json.error,
    missing: json.missing,
    provisional: json.provisional,
    level: json.level,
    /*
     * Here locale is NOT from json
     */
    locale: locale,
    // Note: json.votes and json.total are currently unused
  };
}

/**
 * Calculate a user-friendly percentage
 *
 * @param {Number} done the number of tasks completed for this meter
 *        - For the page meter and voter meter, this is the number of items the user has voted on within the set
 *          of items that the user was expected to vote on
 *        - For the locale meter, this normally includes the number of problem (error/missing/provisional) items
 *          that were in baseline and have now been fixed
 *          CAUTION: it may be negative if there are now more problems than there were in baseline
 *          (for example, there were zero baseline errors and now there is one error)
 * @param {Number} total the total number of tasks for this meter
 *        - For the page meter and voter meter, this is the number of items the user was expected to vote on
 *        - For the locale meter, this normally includes the number of problem (error/missing/provisional) items
 *          that were in baseline
 * @returns a whole number between 0 and 100 inclusive
 */
function friendlyPercent(done, total) {
  if (done < 0) {
    return 0; // even if !total
  }
  if (!total) {
    // The task is finished since nothing needed to be done
    // Do not divide by zero (0 / 0 = NaN%)
    return 100;
  }
  if (!done) {
    return 0;
  }
  if (done >= total) {
    return 100;
  }
  // Do not round 99.9 up to 100
  const floor = Math.floor((100 * done) / total);
  if (!floor) {
    // Do not round 0.001 down to zero
    // Instead, provide indication of slight progress
    return 1;
  }
  return floor;
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
  /*
   * The following are meant to be accessible for unit testing only:
   */
  friendlyPercent,
};

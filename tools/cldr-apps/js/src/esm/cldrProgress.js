/*
 * cldrProgress: for Survey Tool progress indicators, encapsulate code
 * including getting/updating data through interaction with other modules
 * and the server. The display logic is in ProgressMeters.vue.
 */
import * as cldrAjax from "./cldrAjax.js";
import * as cldrCoverage from "./cldrCoverage.js";
import * as cldrStatus from "./cldrStatus.js";

import ProgressMeters from "../views/ProgressMeters.vue";

import { createCldrApp } from "../cldrVueRouter";

import { notification } from "ant-design-vue";

const USE_NEW_PROGRESS_WIDGET = false;

let progressWrapper = null;

let sectionProgressStats = null;
let voterProgressStats = null;

let sectionProgressRows = null;

/**
 * Create the ProgressMeters component
 *
 * @param spanId the id of the element that will contain the new component
 */
function insertWidget(spanId) {
  if (USE_NEW_PROGRESS_WIDGET) {
    hideLegacyCompletionWidget();
    reallyInsertWidget(spanId);
  }
}

function reallyInsertWidget(spanId) {
  try {
    const fragment = document.createDocumentFragment();
    progressWrapper = createCldrApp(ProgressMeters).mount(fragment);
    const el = document.getElementById(spanId);
    el.replaceWith(fragment);
  } catch (e) {
    console.error(
      "Error mounting voting completion vue " + e.message + " / " + e.name
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
 *
 * @param {String} newLevel the new coverage level, such as "modern" or "comprehensive"
 */
function updateWidgetsWithCoverage(newLevel) {
  if (progressWrapper) {
    if (sectionProgressRows) {
      // For sectionBar, we have saved a pointer to the rows; we need to recalculate
      // the votes and total based on the coverage level of each row
      sectionProgressStats = getSectionCompletionFromRows(sectionProgressRows);
      refresh();
    }
    // For voterBar
    fetchVoterData();

    // localeBar does NOT depend on the user's chosen coverage level
  }
}

function refresh() {
  if (sectionProgressStats) {
    if (USE_NEW_PROGRESS_WIDGET) {
      progressWrapper?.updateSectionVotesAndTotal(
        sectionProgressStats.votes,
        sectionProgressStats.total
      );
    } else {
      updateLegacyCompletionWidget(sectionProgressStats);
    }
  }
  if (voterProgressStats && USE_NEW_PROGRESS_WIDGET) {
    progressWrapper?.updateVoterVotesAndTotal(
      voterProgressStats.votes,
      voterProgressStats.total
    );
  }
}

function updateSectionCompletion(rows) {
  sectionProgressRows = rows;
  sectionProgressStats = getSectionCompletionFromRows(rows);
  refresh();
}

function getSectionCompletionFromRows(rows) {
  let votes = 0;
  let total = 0;
  const cov = cldrCoverage.effectiveCoverage(); // an integer
  for (let row of Object.values(rows)) {
    if (parseInt(row.coverageValue) > cov) {
      continue;
    }
    ++total;
    if (row.hasVoted) {
      ++votes;
    }
  }
  return {
    votes,
    total,
  };
}

function updateSectionCompletionOneVote(hasVoted) {
  if (sectionProgressStats || voterProgressStats) {
    updateStatsOneVote(sectionProgressStats, hasVoted);
    updateStatsOneVote(voterProgressStats, hasVoted);
    refresh();
  }
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

function fetchVoterData() {
  if (!progressWrapper) {
    return;
  }
  if (!cldrStatus.getSurveyUser()) {
    return;
  }
  const locale = cldrStatus.getCurrentLocale();
  if (!locale) {
    return;
  }
  const level = cldrCoverage.effectiveName(locale);
  if (!level) {
    return;
  }
  progressWrapper.setHidden(false);
  progressWrapper.setLevel(level);
  progressWrapper.setLocale(locale);
  reallyFetchVoterData(locale, level);
}

function reallyFetchVoterData(locale, level) {
  const url = `api/completion/voting/${locale}/${level}`;
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
      voterProgressStats = {
        votes: json.votes,
        total: json.total,
      };
      progressWrapper.updateVoterVotesAndTotal(json.votes, json.total);
    })
    .catch((err) => {
      console.error("Error loading Completion data: " + err);
      progressWrapper.setHidden(true);
    });
}

function hideLegacyCompletionWidget() {
  const el = document.getElementById("legacyCompletionSpan");
  if (el) {
    el.style.display = "none";
  }
}

function updateLegacyCompletionWidget(sectionVotesTotal) {
  const votes = sectionVotesTotal.votes;
  const total = sectionVotesTotal.total;
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
  insertWidget,
  refresh,
  updateSectionCompletion,
  updateSectionCompletionOneVote,
  updateWidgetsWithCoverage,
};

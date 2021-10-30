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

/**
 * Create the ProgressMeters component
 *
 * @param spanId the id of the element that will contain the new component
 */
function insertWidget(spanId) {
  if (USE_NEW_PROGRESS_WIDGET) {
    hideLegacyCompletionWidget();
    setTimeout(function () {
      reallyInsertWidget(spanId);
    }, 3000 /* three seconds -- temporary work-around for delays in initializing locale and coverage level */);
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
    console.log("cldrProgress changing level: " + newLevel);
    fetchVoterData(); // for voterBar
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
}

function updateSectionCompletion(rows) {
  sectionProgressStats = getSectionCompletionFromRows(rows);
  refresh();
}

function getSectionCompletionFromRows(rows) {
  let votes = 0;
  let total = 0;
  for (let row of Object.values(rows)) {
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
  if (sectionProgressStats) {
    if (hasVoted) {
      if (sectionProgressStats.votes < sectionProgressStats.total) {
        sectionProgressStats.votes++;
      }
    } else {
      if (sectionProgressStats.votes > 0) {
        sectionProgressStats.votes--;
      }
    }
    refresh();
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
  fetchVoterData,
  insertWidget,
  refresh,
  updateSectionCompletion,
  updateSectionCompletionOneVote,
  updateWidgetsWithCoverage,
};

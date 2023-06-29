/**
 * cldrCoverage: encapsulate Survey Tool "Coverage" functions
 */

import * as cldrStatus from "./cldrStatus.mjs";
import * as cldrClient from "./cldrClient.mjs";
import * as cldrXlsx from "./cldrXlsx.mjs";
import * as XLSX from "xlsx";

let surveyLevels = null;

const surveyOrgCov = {};

let surveyUserCov = null;

function getSurveyLevels() {
  return surveyLevels;
}

function setSurveyLevels(levs) {
  surveyLevels = levs;
}

/**
 * Get numeric, given string
 *
 * @param {String} lev
 * @return {Number} or 0
 */
function covValue(lev) {
  lev = lev.toUpperCase();
  const levs = getSurveyLevels();
  if (levs && levs[lev]) {
    return parseInt(levs[lev].level);
  } else {
    return 0;
  }
}

function covName(lev) {
  const levs = getSurveyLevels();
  if (!levs) {
    return null;
  }
  for (var k in levs) {
    if (parseInt(levs[k].level) == lev) {
      return k.toLowerCase();
    }
  }
  return null;
}

function effectiveCoverage(locale) {
  const userCov = getSurveyUserCov();
  if (userCov) {
    return covValue(userCov);
  } else {
    const orgCov = getSurveyOrgCov(locale);
    if (!orgCov) {
      console.error(`surveyOrgCov(${locale}) was not yet initialized`);
      return 0;
    }
    return covValue(orgCov);
  }
}

/**
 * Get the name for the effective coverage level, or null if none set
 *
 * The effective level is the coverage level that the user has chosen, or else the organization's
 * coverage level if the user has not chosen one
 *
 * @return the name, or null
 */
function effectiveName(locale) {
  if (surveyUserCov) {
    return surveyUserCov;
  }
  return getSurveyOrgCov(locale);
}

function getSurveyOrgCov(locale) {
  return surveyOrgCov[locale];
}

function setSurveyOrgCov(cov, locale) {
  surveyOrgCov[locale] = cov;
}

function getSurveyUserCov() {
  return surveyUserCov;
}

function setSurveyUserCov(cov) {
  surveyUserCov = cov;
}

function updateCovFromJson(json) {
  if (json.covlev_user && json.covlev_user != "default") {
    setSurveyUserCov(json.covlev_user);
  } else {
    setSurveyUserCov(null);
  }

  if (json.covlev_org) {
    // sets the organization coverage for this specific locale
    setSurveyOrgCov(json.covlev_org, json.loc);
  }
}

/**
 * Update the coverage classes, show and hide things in and out of coverage
 */
function updateCoverage(theDiv) {
  if (theDiv == null) return;
  var theTable = theDiv.theTable;
  if (theTable == null) return;
  if (!theTable.origClass) {
    theTable.origClass = theTable.className;
  }
  const levs = getSurveyLevels();
  if (levs != null) {
    const currentLocale = cldrStatus.getCurrentLocale();
    var effective = effectiveCoverage(currentLocale);
    var newStyle = theTable.origClass;
    for (var k in levs) {
      var level = levs[k];

      if (effective < parseInt(level.level)) {
        newStyle = newStyle + " hideCov" + level.level;
      }
    }
    if (newStyle != theTable.className) {
      theTable.className = newStyle;
    }
  }
}

async function getCoverageStatus() {
  const client = await cldrClient.getClient();
  const { body } = await client.apis.voting.getCoverageStatus();
  const { levelNames, results } = body;
  const cooked = results.reduce((p, v) => {
    const { proportions, locale } = v;
    delete v.proportions;
    v.proportions = {};
    for (let n = 0; n < levelNames.length; n++) {
      v.adjustedGoal = v.adjustedGoal?.toLowerCase();
      v.cldrLocaleLevelGoal = v.cldrLocaleLevelGoal?.toLowerCase();
      v.staticLevel = v.staticLevel?.toLowerCase();
      const name = levelNames[n];
      const val = proportions[n];
      v.proportions[name] = val; // "modern": 0.45 etc
    }
    p[locale] = v;
    return p;
  }, {});

  return cooked;
}

async function getCoverageStatusXlsx() {
  const data = await getCoverageStatus();

  const locales = Object.keys(data).sort();

  const COLUMNS = [
    {
      title: "locale",
    },
    {
      title: "level",
    },
  ];

  const ws_data = [
    [...COLUMNS.map(({ title }) => title)], // header
    ...locales.map((locale) => {
      const {
        adjustdGoal,
        cldrLocaleLevelGoal,
        found,
        icu,
        missing,
        proportions,
        shownMissingPaths,
        staticLevel,
        sumFound,
        sumUnconfirmed,
        unconfirmedc,
        visibleLevelComputed,
      } = data[locale];

      return [locale, visibleLevelComputed];
    }),
  ];

  // TODO: comments
  const wb = XLSX.utils.book_new();
  const ws = XLSX.utils.aoa_to_sheet(ws_data);
  const ws_name = "LiveLocaleCoverage";
  XLSX.utils.book_append_sheet(wb, ws, ws_name);
  XLSX.writeFile(wb, `${ws_name}.xlsx`);
}

export {
  covName,
  covValue,
  effectiveCoverage,
  effectiveName,
  getCoverageStatus,
  getCoverageStatusXlsx,
  getSurveyOrgCov,
  getSurveyUserCov,
  setSurveyLevels,
  setSurveyUserCov,
  updateCoverage,
  updateCovFromJson,
};

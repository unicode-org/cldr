/*
 * cldrGenericVue: encapsulate functions for the any other special pages of Survey Tool
 * which route through Vue
 */
import * as cldrEvent from "./cldrEvent.js";
import * as cldrLoad from "./cldrLoad.js";
import * as cldrRetry from "./cldrRetry.js";
import * as cldrStatus from "./cldrStatus.js";
import * as cldrSurvey from "./cldrSurvey.js";

const testCldrRetry = false; // danger, must not be true for production

// called as special.load
function load(specialPage) {
  loadHandler({}, specialPage); // null load
}

/**
 * Attempt to update the application with a new coverage level
 * @param {String} newLevel
 * @returns true if the change was handled
 */
function handleCoverageChanged(newLevel) {
  return cldrBundle.handleCoverageChanged(newLevel);
}

function loadHandler(json, specialPage) {
  if (testCldrRetry && Math.random() > 0.5) {
    cldrRetry.handleDisconnect(
      "while loading the Special page (testing)",
      json
    );
    return;
  }

  const app = document.createElement("div");

  // make right hand sidebar empty. TODO: better way to do this?
  cldrEvent.hideRightPanel();
  cldrSurvey.hideLoader();
  cldrLoad.flipToOtherDiv(app);
  const locale = cldrStatus.getCurrentLocale();
  const locmap = cldrLoad.getTheLocaleMap();
  let localeInfo = null;
  let localeDir = null;
  if (locale) {
    localeInfo = locmap.getLocaleInfo(locale);
    localeDir = localeInfo.dir;
  }

  // add Vue-based component

  cldrBundle.showPanel(specialPage, app, {
    // modules
    cldrLoad,
    cldrEvent, // Vue could call into these, if need be
    cldrStatus,
    cldrSurvey,

    // additional variables
    locale,
    locmap,
    localeInfo,
    localeDir,
    sessionId: cldrStatus.getSessionId()
  });
}

export { load, handleCoverageChanged };

/*
 * cldrGenericVue: encapsulate functions for the any other special pages of Survey Tool
 * which route through Vue
 */
import * as cldrLoad from "./cldrLoad.mjs";
import * as cldrRetry from "./cldrRetry.mjs";
import * as cldrSurvey from "./cldrSurvey.mjs";
import * as cldrVue from "./cldrVue.mjs";

const testCldrRetry = false; // danger, must not be true for production

// called as special.load
function load(specialPage) {
  loadHandler({}, specialPage); // null load
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
  cldrSurvey.hideLoader();
  cldrLoad.flipToOtherDiv(app);

  try {
    cldrVue.showPanel(specialPage, app);
  } catch (e) {
    // Note: because of error handle deficiencies (CLDR-14790) showPanel is going to do its own
    // notifications. So we do not expect this catch clause
    // to be called on a Vue error.
    console.error(
      "Error in vue load of [" +
        specialPage +
        "]  " +
        e.message +
        " / " +
        e.name
    );
    return cldrRetry.handleDisconnect(
      "Exception while loading: " +
        e.message +
        ", n=" +
        e.name +
        " \nStack:\n" +
        (e.stack || "[none]"),
      null
    ); // in case the 2nd line doesn't work
  }
}

export { load };

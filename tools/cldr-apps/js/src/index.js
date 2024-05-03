// This file gets bundled by webpack

import * as cldrGui from "./esm/cldrGui.mjs";
import * as cldrVue from "./esm/cldrVue.mjs";
import * as cldrMonitoring from "./esm/cldrMonitoring.mjs";

// kick off the top level monitoring
cldrMonitoring.init();

/**
 * This is called as cldrBundle.runGui by way of JavaScript embedded in HTML
 * embedded in Java code! See SurveyTool.java
 *
 * @returns {Promise}
 */
function runGui() {
  return cldrGui.run();
}

/**
 * This is called as cldrBundle.showPanel by way of JavaScript embedded in HTML
 * embedded in Java code! See SurveyTool.java
 */
function showPanel(...args) {
  return cldrVue.showPanel(...args);
}

/**
 * TODO Does not belong here. CLDR-14943
 * Workaround (aka hack) due to flattening in the current info panel.
 */
function toggleTranscript() {
  document
    .getElementsByClassName("transcript-container")[0]
    .classList.toggle("visible");
}

// The following will show up in the cldrBundle global
export default {
  runGui,
  showPanel,
  toggleTranscript,
};

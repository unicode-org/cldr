// This file gets bundled into bundle.js’s cldrBundle global
// From there, it is imported by SurveyTool.includeJavaScript()

// global stylesheets
import "./css/cldrForum.css";
import "../../../cldr-code/src/main/resources/org/unicode/cldr/tool/reports.css";

// module stylesheets need to go here. See cldrVue.mjs
// example: import 'someModule/dist/someModule.css'
import "ant-design-vue/dist/antd.min.css";

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

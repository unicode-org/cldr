// This file gets bundled into bundle.js’s cldrBundle global
// From there, it is imported by SurveyTool.includeJavaScript()

// global stylesheets
import "./css/cldrForum.css";

// module stylesheets need to go here. See cldrVueRouter.js
// example: import 'someModule/dist/someModule.css'
import "ant-design-vue/dist/antd.css";

// local modules
import { showPanel, createCldrApp } from "./cldrVueRouter.js";
import { runGui } from "./runGui.js";

/**
 * TODO Does not belong here. CLDR-14193
 * Workaround (aka hack) due to flattening in the current info panel.
 */
function toggleTranscript() {
  document
    .getElementsByClassName("transcript-container")[0]
    .classList.toggle("visible");
}

// The following will show up in the cldrBundle global
export default {
  createCldrApp,
  runGui,
  showPanel,
  toggleTranscript,
};

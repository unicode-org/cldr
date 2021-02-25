import "./style.css";
// This is the entrypoint for the new SurveyTool app

import { showPanel } from "./cldrVueRouter.js";
import { runGui } from "./runGui.js";

// The following will show up in the cldrBundle global
export default {
  runGui,
  showPanel,
};

import "./style.css";
// This is the entrypoint for the new SurveyTool app

import { createApp } from "vue";
import About from "./views/About.vue";

function showAbout(el) {
  return createApp(About).mount(el);
}

// The following will show up in the cldrBundle global
export default {
  showAbout
};

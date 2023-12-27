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

import { datadogRum } from "@datadog/browser-rum";
import { datadogLogs } from "@datadog/browser-logs";

if (window.dataDogClientToken) {
  datadogLogs.init({
    clientToken: window.dataDogClientToken,
    site: "us5.datadoghq.com",
    forwardErrorsToLogs: true,
    sessionSampleRate: 100,
  });

  datadogRum.init({
    applicationId: window.dataDogAppId,
    clientToken: window.dataDogClientToken,
    site: "us5.datadoghq.com",
    service: "surveytool",
    env: window.dataDogEnv,
    // Specify a version number to identify the deployed version of your application in Datadog
    // version: '1.0.0',
    sessionSampleRate: 100,
    sessionReplaySampleRate: 20,
    trackUserInteractions: true,
    trackResources: true,
    trackLongTasks: true,
    defaultPrivacyLevel: "allow",
    allowedTracingUrls: [
      {
        match: /https:\/\/.*\.unicode\.org/,
        propagatorTypes: ["tracecontext"],
      },
    ],
  });
}

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

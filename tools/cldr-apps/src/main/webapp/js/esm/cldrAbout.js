/*
 * cldrAbout: encapsulate functions for the "About" page of Survey Tool
 */
import * as cldrLoad from "./cldrLoad.js";
import * as cldrRetry from "./cldrRetry.js";
import * as cldrStatus from "./cldrStatus.js";
import * as cldrSurvey from "./cldrSurvey.js";

const testCldrRetry = false; // danger, must not be true for production

// called as special.load
function load() {
  loadHandler({}); // null load
}

function loadHandler(json) {
  if (testCldrRetry && Math.random() > 0.5) {
    cldrRetry.handleDisconnect("while loading the About page (testing)", json);
    return;
  }

  const ourDiv = document.createElement("div");
  ourDiv.innerHTML = getHtml(json);
  cldrSurvey.hideLoader();
  cldrLoad.flipToOtherDiv(ourDiv);

  // add Vue-based about box
  const app = document.createElement("div");
  ourDiv.appendChild(app);
  cldrBundle.showPanel('about', app);
}

function errorHandler(err) {
  const ourDiv = document.createElement("div");
  ourDiv.innerHTML = err;
  cldrSurvey.hideLoader();
  cldrLoad.flipToOtherDiv(ourDiv);
}

function getHtml(json) {
  let html = cldrStatus.logoIcon();
  return html;
}

export {
  load,
  /*
   * The following are meant to be accessible for unit testing only:
   */
  getHtml,
};

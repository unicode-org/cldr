/*
 * cldrReportZones: encapsulate functions for Zones report for Survey Tool
 */
import * as cldrAjax from "./cldrAjax.js";
import * as cldrDom from "./cldrDom.js";
import * as cldrEvent from "./cldrEvent.js";
import * as cldrInfo from "./cldrInfo.js";
import * as cldrLoad from "./cldrLoad.js";
import * as cldrStatus from "./cldrStatus.js";
import * as cldrSurvey from "./cldrSurvey.js";
import * as cldrText from "./cldrText.js";

// called as special.load
function load() {
  cldrSurvey.showLoader(null);
  const message = cldrText.get("reportGuidance");
  cldrInfo.showMessage(message);
  const url = getUrl();
  cldrSurvey.hideLoader();
  const xhrArgs = {
    url: url,
    // TODO: get json from server and do the presentation on the front end
    handleAs: "text", // not "html" or "json"!
    load: loadHandler,
    error: errorHandler,
  };
  cldrAjax.sendXhr(xhrArgs);
}

function getUrl() {
  return (
    cldrStatus.getContextPath() +
    "/SurveyAjax?what=report&x=r_zones" +
    "&_=" +
    cldrStatus.getCurrentLocale() +
    "&s=" +
    cldrStatus.getSessionId() +
    cldrSurvey.cacheKill()
  );
}

function loadHandler(html) {
  cldrSurvey.hideLoader();
  cldrLoad.setLoading(false);
  const frag = cldrDom.construct(html);
  cldrLoad.flipToOtherDiv(frag);
  cldrEvent.hideRightPanel();
}

function errorHandler(err) {
  cldrSurvey.hideLoader();
  cldrLoad.setLoading(false);
  const html =
    "<div style='padding-top: 4em; font-size: x-large !important;' class='ferrorbox warning'>" +
    "<span class='icon i-stop'>" +
    " &nbsp; &nbsp;</span>Error: could not load: " +
    err +
    "</div>";
  const frag = cldrDom.construct(html);
  cldrLoad.flipToOtherDiv(frag);
}

export { load };

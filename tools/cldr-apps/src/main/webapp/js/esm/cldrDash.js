/*
 * cldrDash: encapsulate functions for "Dashboard" for Survey Tool
 */
import * as cldrAjax from "./cldrAjax.js";
import * as cldrDom from "./cldrDom.js";
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
  const url = reportUrl();

  if (cldrStatus.isVisitor()) {
    alert("Please login to access Dashboard");
    cldrStatus.setCurrentSpecial("");
    cldrStatus.setCurrentLocale("");
    cldrLoad.reloadV();
    return;
  }
  const xhrArgs = {
    url: url,
    handleAs: "json",
    load: loadHandler,
    error: errorHandler,
  };
  cldrAjax.queueXhr(xhrArgs);
}

function reportUrl() {
  return (
    cldrStatus.getContextPath() +
    "/SurveyAjax?what=report&x=r_vetting_json" +
    "&_=" +
    cldrStatus.getCurrentLocale() +
    "&s=" +
    cldrStatus.getSessionId() +
    cldrSurvey.cacheKill()
  );
}

function loadHandler(json) {
  cldrSurvey.hideLoader();
  cldrLoad.setLoading(false);
  // further errors are handled in JSON

  // TODO: implement showReviewPage for non-Dojo; see review.js
  showReviewPage(json, function () {
    // show function - flip to the 'other' page.
    cldrLoad.flipToOtherDiv(null);
  });
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

// called as special.parseHash
function parseHash(pieces) {
  if (pieces.length > 2) {
    cldrStatus.setCurrentPage(pieces[2]);
    if (pieces.length > 3) {
      cldrStatus.setCurrentId(pieces[3]);
    } else {
      cldrStatus.setCurrentId("");
    }
    return true;
  } else {
    return false;
  }
}

export { load, parseHash };

"use strict";

/**
 * cldrDash: encapsulate functions for "Dashboard" for Survey Tool
 * This is the non-dojo version. For dojo, see CldrDojoLoad.js
 *
 * Use an IIFE pattern to create a namespace for the public functions,
 * and to hide everything else, minimizing global scope pollution.
 */
const cldrDash = (function () {
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

  /*
   * Make only these functions accessible from other files:
   */
  return {
    load,
    /*
     * The following are meant to be accessible for unit testing only:
     */
    // test: {
    // f,
    // },
  };
})();

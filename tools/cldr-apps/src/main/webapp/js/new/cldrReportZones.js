"use strict";

/**
 * cldrReportZones: encapsulate functions for Zones report for Survey Tool
 * This is the non-dojo version. For dojo, see CldrDojoLoad.js
 *
 * Use an IIFE pattern to create a namespace for the public functions,
 * and to hide everything else, minimizing global scope pollution.
 */
const cldrReportZones = (function () {
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
    cldrAjax.queueXhr(xhrArgs);
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

"use strict";

/**
 * cldrRecentActivity: encapsulate the "Recent Activity" feature, a.k.a. "myvotes".
 *
 * Use an IIFE pattern to create a namespace for the public functions,
 * and to hide everything else, minimizing global scope pollution.
 */
const cldrRecentActivity = (function () {
  function load() {
    const surveyUser = cldrStatus.getSurveyUser();
    if (!surveyUser || !surveyUser.id) {
      return;
    }
    const url =
      cldrStatus.getContextPath() +
      "/SurveyAjax?what=recent_activity" +
      "&s=" +
      cldrStatus.getSessionId() +
      "&user=" +
      surveyUser.id;
    "&" + cldrSurvey.cacheKill();

    cldrLoad.myLoad(url, "(loading recent activity)", loadWithJson);
  }

  function loadWithJson(json) {
    cldrSurvey.hideLoader();
    cldrLoad.setLoading(false);
    const frag = cldrDom.construct(getHtml(json));
    cldrLoad.flipToOtherDiv(frag);
    cldrEvent.hideRightPanel();
    cldrSurvey.showRecent("submitItems", null, json.user);
    cldrSurvey.showAllItems("allMyItems", json.user);
  }

  function getHtml(json) {
    let html =
      "<i>All items shown are for the current release, CLDR " +
      json.newVersion +
      ". Votes before " +
      json.votesAfterDate +
      " are not shown.</i>\n" +
      "<hr />\n" +
      "<h3>The most recently submitted items for user " +
      json.user +
      "</h3>\n" +
      "<div id='submitItems'></div>\n" +
      "<hr />\n" +
      "<h3>All active locales for user " +
      json.user +
      "</h3>\n" +
      "<div id='allMyItems'></div>\n" +
      "<hr />\n" +
      "<form method='POST' action='DataExport.jsp'>\n" +
      "  <input type='hidden' name='s' value='" +
      cldrStatus.getSessionId() +
      "'>\n" +
      "  <input type='hidden' name='user' value='" +
      json.user +
      "'>\n" +
      "  <input type='hidden' name='do' value='mydata'>\n" +
      "  <input type='submit' class='csvDownload' value='Download all of my votes as .csv'>\n" +
      "</form>\n";

    return html;
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
    //   f,
    // },
  };
})();

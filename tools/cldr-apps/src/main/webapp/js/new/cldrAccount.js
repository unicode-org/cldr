"use strict";

/**
 * cldrAccount: Survey Tool feature for My Account Settings
 * This is the new non-dojo version. For dojo, see special/users.js.
 *
 * Use an IIFE pattern to create a namespace for the public functions,
 * and to hide everything else, minimizing global scope pollution.
 */
const cldrAccount = (function () {
  let me = null;

  // called as special.load
  function load() {
    cldrInfo.showNothing();
    me = cldrStatus.getSurveyUser();
    if (!me) {
      pleaseLogIn();
      return;
    }
    const xhrArgs = {
      url: getUrl(),
      handleAs: "json",
      load: loadHandler,
      error: errorHandler,
    };
    cldrAjax.sendXhr(xhrArgs);
  }

  function pleaseLogIn() {
    const ourDiv = document.createElement("div");
    ourDiv.innerHTML = "Please log in to access your account settings";
    cldrSurvey.hideLoader();
    cldrLoad.flipToOtherDiv(ourDiv);
  }

  function getUrl() {
    const p = new URLSearchParams();
    p.append("what", cldrListUsers.WHAT_USER_LIST);
    p.append(cldrListUsers.LIST_JUST, me.email);
    p.append("s", cldrStatus.getSessionId());
    return cldrStatus.getContextPath() + "/SurveyAjax?" + p.toString();
  }

  function loadHandler(json) {
    const ourDiv = document.createElement("div");
    if (json.orgList) {
      orgList = json.orgList;
    }
    ourDiv.innerHTML = getHtml(json);
    cldrSurvey.hideLoader();
    cldrLoad.flipToOtherDiv(ourDiv);
    cldrListUsers.showUserActivity(json);
  }

  function errorHandler(err) {
    const ourDiv = document.createElement("div");
    ourDiv.innerHTML = err;
    cldrSurvey.hideLoader();
    cldrLoad.flipToOtherDiv(ourDiv);
  }

  function getHtml(json) {
    let html =
      "<h2>My Account</h2>\n" + cldrListUsers.getTable(json, cldrAccount);
    html += "<hr />Under construction. Json:";
    html += "<pre>" + JSON.stringify(json, null, 2) + "</pre>\n";
    return html;
  }

  /*
   * Make only these functions accessible from other files
   */
  return {
    load,
    /*
     * The following are meant to be accessible for unit testing only:
     */
    test: {
      getHtml,
    },
  };
})();

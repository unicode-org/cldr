"use strict";

/**
 * cldrRetry: encapsulate notification (errors, etc.) functions for Survey Tool
 * This is the non-dojo version.
 *
 * Use an IIFE pattern to create a namespace for the public functions,
 * and to hide everything else, minimizing global scope pollution.
 */
const cldrRetry = (function () {
  let errInfo = {};

  /**
   * Handle that ST has disconnected
   *
   * @param why - a message
   * @param json - data, if defined
   * @param word
   * @param what - what we were doing
   */
  function handleDisconnect(why, json, word, what) {
    if (json && json.err_code === "E_NOT_LOGGED_IN") {
      window.location.href = "login.jsp?operationFailed" + window.location.hash;
      return;
    }
    if (!what) {
      what = "unknown";
    }
    cldrSurvey.updateProgressWord(word);
    if (json && json.err) {
      why += "\nThe error message was:\n" + json.err;
      if (json.err.fileName) {
        why += "\nFile: " + json.err.fileName;
        if (json.err.lineNumber) {
          why += "\nLine: " + json.err.lineNumber;
        }
      }
    }
    errInfo.location = window.location.href;
    errInfo.why = why;
    errInfo.what = what;
    errInfo.json = json;
    console.log("Disconnect: " + why);
    window.location.href = "#retry"; // load() will be called
  }

  function load() {
    cldrInfo.showNothing();
    cldrEvent.hideOverlayAndSidebar();
    if (!errInfo.location) {
      window.location.href = "/cldr-apps/v";
      return;
    }
    const ourDiv = document.createElement("div");
    ourDiv.innerHTML = getHtml();
    cldrSurvey.hideLoader();
    cldrLoad.flipToOtherDiv(ourDiv);
  }

  function getHtml() {
    return (
      "<h3>" +
      cldrText.get("ari_message") +
      "</h3>\n" +
      "<p>" +
      cldrText.get("E_DISCONNECTED") +
      "</p>\n" +
      "<button onClick='cldrRetry.retry()'>\n" +
      "  <b>Reload</b>\n" +
      "</button>\n" +
      "<h3>Details</h3>\n" +
      "location: " +
      errInfo.location +
      "<br />\n" +
      "what: " +
      errInfo.what +
      "<br />\n" +
      "why: " +
      errInfo.why
    );
  }

  function retry() {
    window.location.href = errInfo.location;
    errInfo = {};
  }

  function format(json, subkey) {
    if (!subkey) {
      subkey = "unknown";
    }
    const theCode =
      json && json.session_err ? "E_SESSION_DISCONNECTED" : "E_UNKNOWN";
    let msg_str = theCode;
    if (json && json.err_code) {
      msg_str = theCode = json.err_code;
      if (cldrText.get(json.err_code) === json.err_code) {
        console.log("** Unknown error code: " + json.err_code);
        msg_str = "E_UNKNOWN";
      }
    }
    if (!json) {
      json = {}; // handle cases with no input data
    }
    return cldrText.sub(msg_str, {
      /* Possibilities include: err_what_section, err_what_locmap, err_what_menus,
			   err_what_status, err_what_unknown, err_what_oldvotes, err_what_vote */
      what: cldrText.get("err_what_" + subkey),
      code: theCode,
      message:
        json.err_data && json.err_data.message ? json.err_data.message : "",
      surveyCurrentLocale: cldrStatus.getCurrentLocale(),
    });
  }

  /*
   * Make only these functions accessible from other files:
   */
  return {
    format,
    handleDisconnect,
    load,
    retry,
    /*
     * The following are meant to be accessible for unit testing only:
     */
    // test: {
    //   f: f,
    // },
  };
})();

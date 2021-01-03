"use strict";

/**
 * cldrBulkClosePosts: Survey Tool feature for bulk-closing forum posts
 * This is the new non-dojo version. For dojo, see CldrDojoBulkClosePosts.js.
 *
 * Use an IIFE pattern to create a namespace for the public functions,
 * and to hide everything else, minimizing global scope pollution.
 */
const cldrBulkClosePosts = (function () {
  let saveParamsForExecute = null;

  let contentDiv = null;

  /**
   * Fetch the Bulk Close Posts data from the server, and "load" it
   *
   * @param params an object with various properties; see SpecialPage.js
   */
  function load(params) {
    saveParamsForExecute = params;
    /*
     * Set up the 'right sidebar'; cf. bulk_close_postsGuidance
     */
    const message = cldrText.get(params.name + "Guidance");
    cldrInfo.showMessage(message);

    const url = getBulkClosePostsUrl();
    const errorHandler = function (err) {
      params.special.showError(params, null, {
        err: err,
        what: "Loading Forum Bulk Close Posts data",
      });
    };
    const loadHandler = function (json) {
      if (json.err) {
        if (params.special) {
          params.special.showError(params, json, {
            what: "Loading Forum Bulk Close Posts data",
          });
        }
        return;
      }
      const html = makeHtmlFromJson(json);
      contentDiv = document.createElement("div");
      contentDiv.innerHTML = html;

      // No longer loading
      cldrSurvey.hideLoader();
      params.flipper.flipTo(params.pages.other, contentDiv);
    };
    const xhrArgs = {
      url: url,
      handleAs: "json",
      load: loadHandler,
      error: errorHandler,
    };
    cldrAjax.sendXhr(xhrArgs);
  }

  /**
   * Respond to button press
   */
  function execute() {
    const params = saveParamsForExecute;

    contentDiv.innerHTML = "<div><p>Bulk closing, in progress...</p></div>";

    const url = getBulkClosePostsUrl() + "&execute=true";
    const errorHandler = function (err) {
      params.special.showError(params, null, {
        err: err,
        what: "Executing Forum Bulk Close Posts",
      });
    };
    const loadHandler = function (json) {
      if (json.err) {
        params.special.showError(params, json, {
          what: "Executing Forum Bulk Close Posts",
        });
        return;
      }
      contentDiv.innerHTML = makeHtmlFromJson(json);
    };
    const xhrArgs = {
      url: url,
      handleAs: "json",
      load: loadHandler,
      error: errorHandler,
    };
    cldrAjax.sendXhr(xhrArgs);
  }

  /**
   * Get the URL to use for loading the Forum Bulk Close Posts page
   */
  function getBulkClosePostsUrl() {
    const sessionId = cldrStatus.getSessionId();
    if (!sessionId) {
      console.log("Error: sessionId falsy in getBulkClosePostsUrl");
      return "";
    }
    return "SurveyAjax?what=bulk_close_posts&s=" + sessionId;
  }

  /**
   * Make the html, given the json for Forum Bulk Close Posts
   *
   * @param json
   * @return the html
   */
  function makeHtmlFromJson(json) {
    let html = "<div>\n";
    if (
      typeof json.threadCount === "undefined" ||
      typeof json.status === "undefined" ||
      (json.status !== "ready" && json.status !== "done")
    ) {
      html +=
        "<p>An error occurred: status = " +
        json.status +
        "; err = " +
        json.err +
        "</p>\n";
    } else if (json.status === "ready") {
      html +=
        "<p>Total threads matching criteria for bulk closing: " +
        json.threadCount +
        "</p>\n";
      if (json.threadCount > 0) {
        html +=
          "<h4><a onclick='cldrBulkClosePosts.execute()'>Close Threads!</a></h4>\n";
        html += "<p>This action cannot be undone.</p>";
        html +=
          "<p>It should normally be done after a new version of CLDR is published, before opening Survey Tool.</p>\n";
      }
    } else if (json.status === "done") {
      html += "<p>Total threads closed: " + json.threadCount + "</p>\n";
      if (typeof json.postCount !== "undefined") {
        html +=
          "<p>Total posts closed (including replies): " +
          json.postCount +
          "</p>\n";
      }
    }
    html += "</div>";
    return html;
  }

  /*
   * Make only these functions accessible from other files
   */
  return {
    load,
    execute,
    /*
     * The following are meant to be accessible for unit testing only:
     */
    test: {
      makeHtmlFromJson,
    },
  };
})();

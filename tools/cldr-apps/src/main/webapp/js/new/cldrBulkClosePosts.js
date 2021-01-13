"use strict";

/**
 * cldrBulkClosePosts: Survey Tool feature for bulk-closing forum posts
 * This is the new non-dojo version. For dojo, see CldrDojoBulkClosePosts.js.
 *
 * Use an IIFE pattern to create a namespace for the public functions,
 * and to hide everything else, minimizing global scope pollution.
 */
const cldrBulkClosePosts = (function () {
  let contentDiv = null;

  /**
   * Fetch the Bulk Close Posts data from the server, and "load" it
   */
  function load() {
    cldrInfo.showMessage(cldrText.get("bulk_close_postsGuidance"));
    const url = getBulkClosePostsUrl();
    const xhrArgs = {
      url: url,
      handleAs: "json",
      load: loadHandler,
      error: errorHandler,
    };
    cldrAjax.sendXhr(xhrArgs);
  }

  function loadHandler(json) {
    if (json.err) {
      cldrRetry.handleDisconnect(
        json.err,
        json,
        "",
        "Loading Forum Bulk Close Posts data"
      );
      return;
    }
    const html = makeHtmlFromJson(json);
    contentDiv = document.createElement("div");
    contentDiv.innerHTML = html;
    cldrSurvey.hideLoader();
    cldrLoad.flipToOtherDiv(contentDiv);
  }

  function errorHandler(err) {
    const ourDiv = document.createElement("div");
    ourDiv.innerHTML = err;
    cldrSurvey.hideLoader();
    cldrLoad.flipToOtherDiv(ourDiv);
  }

  /**
   * Respond to button press
   */
  function execute() {
    contentDiv.innerHTML = "<div><p>Bulk closing, in progress...</p></div>";

    const url = getBulkClosePostsUrl() + "&execute=true";
    const xhrArgs = {
      url: url,
      handleAs: "json",
      load: executeLoadHandler,
      error: errorHandler,
    };
    cldrAjax.sendXhr(xhrArgs);
  }

  function executeLoadHandler(json) {
    if (json.err) {
      cldrRetry.handleDisconnect(
        json.err,
        json,
        "",
        "Executing Forum Bulk Close Posts data"
      );
      return;
    }
    contentDiv.innerHTML = makeHtmlFromJson(json);
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

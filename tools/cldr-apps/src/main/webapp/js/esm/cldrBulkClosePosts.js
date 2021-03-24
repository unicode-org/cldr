/*
 * cldrBulkClosePosts: Survey Tool feature for bulk-closing forum posts
 */
import * as cldrAjax from "./cldrAjax.js";
import * as cldrInfo from "./cldrInfo.js";
import * as cldrLoad from "./cldrLoad.js";
import * as cldrRetry from "./cldrRetry.js";
import * as cldrStatus from "./cldrStatus.js";
import * as cldrSurvey from "./cldrSurvey.js";
import * as cldrText from "./cldrText.js";

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
  setOnClicks();
}

function errorHandler(err) {
  const ourDiv = document.createElement("div");
  ourDiv.innerHTML = err;
  cldrSurvey.hideLoader();
  cldrLoad.flipToOtherDiv(ourDiv);
}

function setOnClicks() {
  const el = document.getElementById("bulkCloseThreads");
  if (el) {
    el.onclick = () => execute();
  }
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
      html += "<h4><a id='bulkCloseThreads'>Close Threads!</a></h4>\n";
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

export {
  load,
  /*
   * The following are meant to be accessible for unit testing only:
   */
  makeHtmlFromJson,
};

/*
 * cldrListEmails: encapsulate "List email addresses of participating users"
 */
import * as cldrAjax from "./cldrAjax.js";
import * as cldrDom from "./cldrDom.js";
import * as cldrInfo from "./cldrInfo.js";
import * as cldrLoad from "./cldrLoad.js";
import * as cldrStatus from "./cldrStatus.js";
import * as cldrSurvey from "./cldrSurvey.js";

const emailListId = "emailList";

const help =
  "If you have permission, this will show users who participated in the SurveyTool for CLDR ";
const header = "<h3>Email addresses of users who participated</h3>\n";
const textAreaStart = "<textarea id='" + emailListId + "' rows='7' cols='42'>";
const textAreaEnd = "</textarea>";
const gotNone = "<p>No data, or no users participated.</p>";

/**
 * Fetch the data from the server, and "load" it
 *
 * Called as special.load
 */
function load() {
  cldrInfo.showMessage(help + cldrStatus.getNewVersion());
  const xhrArgs = {
    url: getAjaxUrl(),
    handleAs: "json",
    load: loadHandler,
    error: errorHandler,
  };
  cldrAjax.sendXhr(xhrArgs);
}

function loadHandler(json) {
  const ourDiv = document.createElement("div");
  ourDiv.innerHTML = json.err || getHtml(json);
  cldrSurvey.hideLoader();
  cldrLoad.flipToOtherDiv(ourDiv);
  const el = document.getElementById(emailListId);
  if (el) {
    el.onclick = () => {
      el.focus();
      el.select();
    };
  }
}

function errorHandler(err) {
  cldrRetry.handleDisconnect(err, json, "", "Loading email data");
}

function getHtml(json) {
  let html = header;
  if (!json.participating_users) {
    html += gotNone;
  } else {
    html += textAreaStart;
    for (let i in json.participating_users.data) {
      const row = json.participating_users.data[i];
      const email = row[json.participating_users.header.EMAIL];
      if (email && email !== "admin@") {
        html += email + ", ";
      }
    }
    html += textAreaEnd;
  }
  return html;
}

function getAjaxUrl() {
  const p = new URLSearchParams();
  p.append("what", "participating_users");
  p.append("s", cldrStatus.getSessionId());
  // allow cache
  return cldrAjax.makeUrl(p);
}

export { load };

/*
 * cldrFlagged: Survey Tool feature for listing flagged items
 */
import * as cldrAjax from "./cldrAjax.js";
import * as cldrCsvFromTable from "./cldrCsvFromTable.js";
import * as cldrInfo from "./cldrInfo.js";
import * as cldrLoad from "./cldrLoad.js";
import * as cldrRetry from "./cldrRetry.js";
import * as cldrStatus from "./cldrStatus.js";
import * as cldrSurvey from "./cldrSurvey.js";
import * as cldrText from "./cldrText.js";

const tableId = "flaggedTable";
const fileName = "flagged.csv";
const flaggedCsvId = "flaggedCsv";

/**
 * Fetch the Flagged Items data from the server, and "load" it
 */
function load() {
  cldrInfo.showMessage(cldrText.get("flaggedGuidance"));
  const xhrArgs = {
    url: getFlaggedUrl(),
    handleAs: "json",
    load: loadHandler,
    error: errorHandler,
  };
  cldrAjax.sendXhr(xhrArgs);
}

function loadHandler(json) {
  if (json.err) {
    cldrRetry.handleDisconnect(json.err, json, "", "Loading Flagged data");
    return;
  }
  const ourDiv = document.createElement("div");
  const innerDiv = document.createElement("div");
  ourDiv.appendChild(innerDiv);
  innerDiv.className = "special_flagged"; // for css
  populateFromJson(innerDiv, json);
  if (json.details) {
    addDetails(ourDiv, json.details);
  }
  cldrSurvey.hideLoader();
  cldrLoad.flipToOtherDiv(ourDiv);
  setOnClicks();
}

function errorHandler(err) {
  const ourDiv = document.createElement("div");
  ourDiv.innerHTML = err;
  cldrSurvey.hideLoader();
  cldrLoad.flipToOtherDiv(ourDiv);
}

function populateFromJson(div, json) {
  const header = json.flagged.header;
  const rows = json.flagged.data;
  let lastLocale = undefined;
  let totalCount = 0;
  const totalCountChunk = $("<span></span>", {
    text: Number(0).toLocaleString(),
  });
  const totalCountHeader = $("<h3></h3>", {
    text: cldrText.get("flaggedTotalCount"),
  });
  totalCountChunk.appendTo(totalCountHeader);
  totalCountHeader.appendTo(div);
  let lastCount = 0;
  let lastCountChunk = null;
  for (let r = 0; r < rows.length; r++) {
    const row = rows[r];
    if (row[header.LOCALE] !== lastLocale) {
      lastCount = 0;
      // emit a header
      const h4 = $("<h4></h4>", { class: "flaggedLocaleHeader" });
      const asLink = $("<a></a>", {
        text: row[header.LOCALE_NAME],
        href: "#/" + row[header.LOCALE],
      });
      asLink.appendTo(h4);
      lastCountChunk = $("<span></span>", { text: "" });
      lastCountChunk.appendTo(h4);
      h4.appendTo(div);
      lastLocale = row[header.LOCALE]; // don't show the header next time
    }
    lastCountChunk.text(Number(++lastCount).toLocaleString());
    totalCountChunk.text(Number(++totalCount).toLocaleString());

    const theRow = $("<div></div>", { class: "flaggedItem" });
    const theDateChunk = $("<span></span>", {
      class: "dateChunk",
      text: new Date(row[header.LAST_MOD]).toLocaleDateString(),
    });
    theDateChunk.appendTo(theRow);

    const theXpathChunk = $("<span></span>", { class: "pathHeaderInfo" });

    const theXpathLink = $("<a></a>", {
      text: row[header.XPATH_CODE].replace(/\t/, " : "),
      href: "#/" + row[header.LOCALE] + "//" + row[header.XPATH_STRHASH],
    });

    theXpathLink.appendTo(theXpathChunk);
    theXpathChunk.appendTo(theRow);

    if (cldrStatus.getSurveyUser()) {
      // if logged in- try to get user info
      const theirUserId = row[header.SUBMITTER];
      getUserMap(theirUserId, { theRow: theRow });
    }
    theRow.appendTo(div);
  }
}

// For TC members, show detailed table and download button (includes emails, so TC only)
// If not authorized (TC), details won't be set
function addDetails(ourDiv, details) {
  let html = "<hr />\n<h3>Details</h3>\n";
  if (details.header && details.data) {
    const colHead = {};
    for (let name in details.header) {
      colHead[details.header[name]] = name;
    }
    html += "<p><button id='" + flaggedCsvId + "'>Download CSV</button></p>\n";
    html += "<table border='1' id='" + tableId + "'>\n";
    html += "<tr>\n";
    for (let i in colHead) {
      html += "<th>" + colHead[i] + "</th>\n";
    }
    html += "</tr>\n";
    for (let row of details.data) {
      html += "<tr>\n";
      for (let i in row) {
        html += "<td>" + row[i] + "</td>\n";
      }
      html += "</tr>\n";
    }
    html += "</table>\n";
  }
  const detailsDiv = document.createElement("div");
  detailsDiv.innerHTML = html;
  ourDiv.appendChild(detailsDiv);
}

const userMapHash = {};

function getUserMap(id, args) {
  if (userMapHash[id]) {
    // already have this id
    addUserInfo(userMapHash[id].err, args, { entry: userMapHash[id], id: id });
  } else {
    const xhrArgs = {
      url: getUserInfoUrl(id),
      handleAs: "json",
      load: userInfoLoadHandler,
      error: userInfoErrorHandler,
    };
    cldrAjax.sendXhr(xhrArgs);
  }

  function userInfoLoadHandler(json) {
    userMapHash[id] = json;
    addUserInfo(json.err, args, { entry: json, id: id }); // add err msg
  }

  function userInfoErrorHandler(err) {
    userMapHash[id] = { err: err };
    addUserInfo(userMapHash[id].err, args, { entry: userMapHash[id], id: id }); // add err msg
  }
}

/**
 * Add in the user's data. Keep it small, vertically.
 */
function addUserInfo(err, args, o) {
  if (err || o.entry.err || !o.entry.user) {
  } else {
    const thinUser = createUserThin(o.entry.user);
    thinUser.appendTo(args.theRow);
  }
}

function createUserThin(user) {
  const div = $("<div></div>", { class: "thinUser" });
  createGravatar16(user).appendTo(div);
  $("<span></span>", {
    text: user.name,
    class: "adminUserName",
  }).appendTo(div);
  if (!user.orgName) {
    user.orgName = user.org;
  }
  $("<span></span>", {
    text: user.orgName + " #" + user.id,
    class: "adminOrgName",
  }).appendTo(div);
  return div;
}

function createGravatar16(user) {
  if (user.emailHash) {
    return $("<img></img>", {
      src:
        "http://www.gravatar.com/avatar/" +
        user.emailHash +
        "?d=identicon&r=g&s=16",
      title: "gravatar - http://www.gravatar.com",
    });
  } else {
    return $("<span></span>");
  }
}

function getFlaggedUrl() {
  const p = new URLSearchParams();
  p.append("what", "flagged"); // cf. WHAT_FLAGGED in SurveyAjax.java
  p.append("s", cldrStatus.getSessionId());
  return cldrAjax.makeUrl(p);
}

function getUserInfoUrl(id) {
  const p = new URLSearchParams();
  p.append("what", "user_info"); // cf. WHAT_USER_INFO in SurveyAjax.java
  p.append("u", id);
  p.append("s", cldrStatus.getSessionId());
  return cldrAjax.makeUrl(p);
}

function setOnClicks() {
  const el = document.getElementById(flaggedCsvId);
  if (el) {
    el.onclick = () => cldrCsvFromTable.download(tableId, fileName);
  }
}

export { load };

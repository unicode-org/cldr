/*
 * cldrRecentActivity: encapsulate the "Recent Activity" feature
 *
 * This feature is reached in three ways:
 *  (1) "My Votes, Recent Activity" from the main menu
 *  (2) "My Account, Settings" from the main menu,
 *      then click the "User Activity" link in the table
 *  (3) "List ... Users" from the main menu,
 *      then click one of the "User Activity" links in the table
 *      -- the user in question might not be the current user
 */
import * as cldrAjax from "./cldrAjax.js";
import * as cldrDom from "./cldrDom.js";
import * as cldrGui from "./cldrGui.js";
import * as cldrLoad from "./cldrLoad.js";
import * as cldrRetry from "./cldrRetry.js";
import * as cldrStatus from "./cldrStatus.js";
import * as cldrSurvey from "./cldrSurvey.js";
import * as cldrText from "./cldrText.js";
import * as cldrUserListExport from "../esm/cldrUserListExport";

/**
 * The id of the user in question; not necessarily the current user
 */
let userId = Number.NaN;

// called as special.load
function load() {
  const me = cldrStatus.getSurveyUser();
  if (!me || !me.id) {
    pleaseLogIn();
    return;
  }
  // normally userId will have been set by handleIdChanged
  if (!userId) {
    userId = Number(me.id);
  }
  cldrLoad.myLoad(getUrl(), "(loading recent activity)", loadWithJson);
}

function pleaseLogIn() {
  const ourDiv = document.createElement("div");
  ourDiv.innerHTML = "Please log in to access account settings";
  cldrSurvey.hideLoader();
  cldrLoad.flipToOtherDiv(ourDiv);
}

function loadWithJson(json) {
  cldrSurvey.hideLoader();
  cldrLoad.setLoading(false);
  cldrGui.hideRightPanel();
  const frag = cldrDom.construct(getHtml(json));

  // Add a button for recent activity download
  const recentActivityButton = document.createElement("button");
  recentActivityButton.appendChild(
    document.createTextNode("Download User Activity… (.xlsx)")
  );
  recentActivityButton.onclick = () =>
    cldrUserListExport.downloadUserActivity(
      json.user,
      cldrStatus.getSessionId()
    );
  frag.appendChild(document.createElement("p"));
  frag.appendChild(document.createElement("hr"));
  frag.appendChild(recentActivityButton);

  cldrLoad.flipToOtherDiv(frag);
  if (Number(json.user) === Number(userId)) {
    showRecent();
    showAllItems();
  }
}

function getHtml(json) {
  if (Number(json.user) !== Number(userId)) {
    return "ERROR: user id mismatch " + json.user + " vs. " + userId;
  }
  let html = getItemsAndLocales(json);
  const me = cldrStatus.getSurveyUser();
  if (me && Number(me.id) === Number(userId)) {
    html += getDownloadMyVotesForm();
  }
  return html;
}

function getItemsAndLocales(json) {
  let who = userId;
  if (json.status.user.name && json.status.user.id === userId) {
    // show the name in addition to their numeric id
    // -- this currently only works when the user is viewing their own data;
    // otherwise the name corresponding to userId isn't included in
    // the json or the "#recent_activity" link url, though it could be
    // implemented in either way and might be a worthwhile improvement
    who += " " + json.status.user.name;
  }
  return (
    "<i>All items shown are for the current release, CLDR " +
    json.newVersion +
    ". Votes before " +
    json.votesAfterDate +
    " are not shown.</i>\n" +
    "<hr />\n" +
    "<h3>The most recently submitted items for user " +
    who +
    "</h3>\n" +
    "<div id='submitItems'></div>\n" +
    "<hr />\n" +
    "<h3>All active locales for user " +
    who +
    "</h3>\n" +
    "<div id='allMyItems'></div>\n"
  );
}

function getDownloadMyVotesForm() {
  return (
    "<hr />\n" +
    "<form method='POST' action='DataExport.jsp'>\n" +
    "  <input type='hidden' name='s' value='" +
    cldrStatus.getSessionId() +
    "'>\n" +
    "  <input type='hidden' name='user' value='" +
    userId +
    "'>\n" +
    "  <input type='hidden' name='do' value='mydata'>\n" +
    "  <input type='submit' class='csvDownload' value='Download all of my votes as .csv'>\n" +
    "</form>\n"
  );
}

function showRecent() {
  const div = document.getElementById("submitItems");
  div.className = "recentList";
  div.update = function () {
    cldrSurvey.showLoader("Loading recent items");
    const xhrArgs = {
      url: getRecentItemsUrl(),
      handleAs: "json",
      load: loadHandler,
      error: errorHandler,
    };
    cldrAjax.sendXhr(xhrArgs);
  };
  div.update();

  // TODO: no long nested functions!
  function loadHandler(json) {
    try {
      if (json && json.recent) {
        const frag = document.createDocumentFragment();
        const header = json.recent.header;
        const data = json.recent.data;

        if (data.length == 0) {
          frag.appendChild(
            cldrDom.createChunk(cldrText.get("recentNone"), "i")
          );
        } else {
          const rowDiv = document.createElement("div");
          frag.appendChild(rowDiv);

          rowDiv.appendChild(
            cldrDom.createChunk(cldrText.get("recentLoc"), "b")
          );
          rowDiv.appendChild(
            cldrDom.createChunk(cldrText.get("recentXpathCode"), "b")
          );
          rowDiv.appendChild(
            cldrDom.createChunk(cldrText.get("recentValue"), "b")
          );
          rowDiv.appendChild(
            cldrDom.createChunk(cldrText.get("recentWhen"), "b")
          );
          for (let q in data) {
            const row = data[q];
            const loc = row[header.LOCALE];
            const locname = row[header.LOCALE_NAME];
            const org = row[header.ORG];
            const last_mod = row[header.LAST_MOD];
            const xpath = row[header.XPATH];
            const xpath_code = row[header.XPATH_CODE].replace(/\t/g, " / ");
            const xpath_hash = row[header.XPATH_STRHASH];
            const value = row[header.VALUE];

            const rowDiv = document.createElement("div");
            frag.appendChild(rowDiv);
            rowDiv.appendChild(createLocLink(loc, locname, "recentLoc"));
            const xpathItem = cldrDom.createChunk(
              xpath_code,
              "a",
              "recentXpath"
            );
            rowDiv.appendChild(xpathItem);
            xpathItem.href = getXpathUrl(loc, xpath_hash);
            rowDiv.appendChild(
              cldrDom.createChunk(value, "span", "value recentValue")
            );
            rowDiv.appendChild(
              cldrDom.createChunk(
                new Date(last_mod).toLocaleString(),
                "span",
                "recentWhen"
              )
            );
          }
        }
        cldrDom.removeAllChildNodes(div);
        div.appendChild(frag);
        cldrSurvey.hideLoader();
      } else {
        cldrRetry.handleDisconnect("Failed to load JSON recent items", json);
      }
    } catch (e) {
      console.log("Error in ajax get ", e.message);
      cldrRetry.handleDisconnect(" exception in getrecent: " + e.message, null);
    }
  }

  function errorHandler(err) {
    cldrRetry.handleDisconnect("Error in showrecent: " + err);
  }
}

function showAllItems() {
  const div = document.getElementById("allMyItems");
  div.className = "recentList";
  div.update = function () {
    cldrSurvey.showLoader("Loading recent items");
    const xhrArgs = {
      url: getAllItemsUrl(),
      handleAs: "json",
      load: loadHandler,
      error: errorHandler,
    };
    cldrAjax.sendXhr(xhrArgs);
  };
  div.update();

  // TODO: no long nested functions!
  function loadHandler(json) {
    try {
      if (json && json.mine) {
        const frag = document.createDocumentFragment();
        const header = json.mine.header;
        const data = json.mine.data;
        if (data.length == 0) {
          frag.appendChild(
            cldrDom.createChunk(cldrText.get("recentNone"), "i")
          );
        } else {
          const rowDiv = document.createElement("div");
          frag.appendChild(rowDiv);

          rowDiv.appendChild(
            cldrDom.createChunk(cldrText.get("recentLoc"), "b")
          );
          rowDiv.appendChild(
            cldrDom.createChunk(cldrText.get("recentCount"), "b")
          );

          for (let q in data) {
            const row = data[q];
            const count = row[header.COUNT];
            const rowDiv = document.createElement("div");
            frag.appendChild(rowDiv);

            const loc = row[header.LOCALE];
            const locname = row[header.LOCALE_NAME];
            rowDiv.appendChild(createLocLink(loc, locname, "recentLoc"));
            rowDiv.appendChild(
              cldrDom.createChunk(count, "span", "value recentCount")
            );

            const sessionId = cldrStatus.getSessionId();
            if (sessionId) {
              const dlLink = cldrDom.createChunk(
                cldrText.get("downloadXmlLink"),
                "a",
                "notselected"
              );
              dlLink.href = getMyXmlUrl(loc);
              dlLink.target = "STDownload";
              rowDiv.appendChild(dlLink);
            }
          }
        }
        cldrDom.removeAllChildNodes(div);
        div.appendChild(frag);
        cldrSurvey.hideLoader();
      } else {
        cldrRetry.handleDisconnect("Failed to load JSON recent items", json);
      }
    } catch (e) {
      console.log("Error in ajax get ", e.message);
      cldrRetry.handleDisconnect(" exception in getrecent: " + e.message, null);
    }
  }

  function errorHandler(err) {
    cldrRetry.handleDisconnect("Error in showAllItems: " + err);
  }
}

function createLocLink(loc, locName, className) {
  const cl = cldrDom.createChunk(locName, "a", "localeChunk " + className);
  cl.title = loc;
  cl.href = "survey?_=" + loc;
  return cl;
}

// called as special.parseHash
function parseHash(pieces) {
  cldrStatus.setCurrentPage("");
  // example: "v#recent_activity///12345"
  // pieces[0] = recent_activity
  // pieces[1] = empty = locale, unused
  // pieces[2] = empty = ???, unused; somehow this seems required
  // pieces[3] = userId
  if (pieces && pieces.length > 3) {
    if (!pieces[3] || pieces[3] == "") {
      cldrStatus.setCurrentId("");
    } else {
      const id = new Number(pieces[3]);
      if (Number.isNaN(id)) {
        cldrStatus.setCurrentId("");
      } else {
        const idStr = id.toString();
        cldrStatus.setCurrentId(idStr);
        handleIdChanged(idStr);
      }
    }
    return true;
  } else {
    return false;
  }
}

// called as special.handleIdChanged
function handleIdChanged(idStr) {
  if (idStr) {
    const id = new Number(idStr);
    if (Number.isNaN(id)) {
      cldrStatus.setCurrentId("");
    } else {
      cldrStatus.setCurrentId(id.toString());
      userId = Number(id);
    }
  }
}

function getUrl() {
  const p = new URLSearchParams();
  p.append("what", "recent_activity");
  p.append("user", userId);
  p.append("s", cldrStatus.getSessionId());
  p.append("cacheKill", cldrSurvey.cacheBuster());
  return cldrAjax.makeUrl(p);
}

function getRecentItemsUrl() {
  const p = new URLSearchParams();
  p.append("what", "recent_items");
  p.append("user", userId);
  p.append("limit", "15");
  return cldrAjax.makeUrl(p);
}

function getAllItemsUrl() {
  const p = new URLSearchParams();
  p.append("what", "mylocales");
  p.append("user", userId);
  return cldrAjax.makeUrl(p);
}

function getMyXmlUrl(loc) {
  const p = new URLSearchParams();
  p.append("do", "myxml");
  p.append("_", loc);
  p.append("user", userId);
  p.append("s", cldrStatus.getSessionId());
  return "DataExport.jsp?" + p.toString();
}

function getXpathUrl(loc, xpath_hash) {
  const p = new URLSearchParams();
  p.append("_", loc);
  p.append("strid", xpath_hash);
  return "survey?" + p.toString();
}

export { handleIdChanged, load, parseHash };

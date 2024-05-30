/**
 * cldrSurvey: encapsulate miscellaneous Survey Tool functions
 */
import * as cldrAjax from "./cldrAjax.mjs";
import * as cldrCoverage from "./cldrCoverage.mjs";
import * as cldrCoverageReset from "./cldrCoverageReset.mjs";
import * as cldrDom from "./cldrDom.mjs";
import * as cldrEvent from "./cldrEvent.mjs";
import * as cldrGui from "./cldrGui.mjs";
import * as cldrLoad from "./cldrLoad.mjs";
import * as cldrMenu from "./cldrMenu.mjs";
import * as cldrRetry from "./cldrRetry.mjs";
import * as cldrStatus from "./cldrStatus.mjs";
import * as cldrText from "./cldrText.mjs";
import * as cldrVote from "./cldrVote.mjs";
import { XpathMap } from "./cldrXpathMap.mjs";

/*
 * INHERITANCE_MARKER indicates that the value of a candidate item is inherited.
 * Compare INHERITANCE_MARKER in CldrUtility.java.
 */
const INHERITANCE_MARKER = "↑↑↑";

const DEBUG_SHOWER = false;
const DEBUG_LOCALE_STAMP = false;

let xpathMap = null;

let wasBusted = false;
let didUnbust = false;

let loadOnOk = null; // TODO: SurveyMain.java writes scripts that try to reference loadOnOk

let clickContinue = null; // TODO: SurveyMain.java writes scripts that try to reference clickContinue

let surveyNextLocaleStamp = NaN;

let showers = {};

let progressWord = null;
let ajaxWord = null;
let specialHeader = null; // TODO: supposed to be same as specialHeader in cldrStatus.mjs, or not?

let updateParts = null;

let cacheKillStamp = null;

/**
 * Table mapping CheckCLDR.StatusAction into capabilities
 * @property statusActionTable
 */
const statusActionTable = {
  ALLOW: {
    vote: true,
    ticket: false,
    change: true,
  },
  ALLOW_VOTING_AND_TICKET: {
    vote: true,
    ticket: true,
    change: false,
  },
  ALLOW_VOTING_BUT_NO_ADD: {
    vote: true,
    ticket: false,
    change: false,
  },
  ALLOW_TICKET_ONLY: {
    vote: false,
    ticket: true,
    change: true,
  },
  DEFAULT: {
    vote: false,
    ticket: false,
    change: false,
  },
};

/**
 * How often to fetch updates. Default 15s.
 * Used only for delay in calling updateStatus.
 */
const timerSpeed = 15000; // 15 seconds
const fastTimerSpeed = 3000; // 3 seconds
let statusTimeout = null;

let overridedir = null;

/************************/

function getDidUnbust() {
  return didUnbust;
}

function getXpathMap() {
  if (!xpathMap) {
    xpathMap = new XpathMap(); // TODO: is it really a singleton?
  }
  return xpathMap;
}

/**
 * Is the keyboard or input widget 'busy'? i.e., it's a bad time to change the DOM
 *
 * @return true if window.getSelection().anchorNode.className contains "dijitInp" or "popover-content",
 *		 else false
 *
 * "popover-content" identifies the little input window, created using bootstrap, that appears when the
 * user clicks an add ("+") button. Added "popover-content" per https://unicode.org/cldr/trac/ticket/11265.
 *
 * Called only from CldrSurveyVettingLoader.js
 */
function isInputBusy() {
  if (!window.getSelection) {
    return false;
  }
  var sel = window.getSelection();
  if (sel && sel.anchorNode && sel.anchorNode.className) {
    if (sel.anchorNode.className.indexOf("popover-content") != -1) {
      return true;
    }
  }
  return false;
}

function createGravatar(user) {
  if (user.emailHash) {
    const gravatar = document.createElement("img");
    gravatar.src =
      "https://www.gravatar.com/avatar/" +
      user.emailHash +
      "?d=identicon&r=g&s=32";
    gravatar.title = "gravatar - http://www.gravatar.com";
    return gravatar;
  } else {
    return document.createTextNode("");
  }
}

/**
 * Mark the page as busted. Don't do any more requests.
 */
function busted() {
  cldrStatus.setIsDisconnected(true);
  cldrDom.addClass(document.getElementsByTagName("body")[0], "disconnected");
}

function unbust() {
  didUnbust = true;
  console.log("Un-busting");
  progressWord = "unbusted";
  cldrStatus.setIsDisconnected(false);
  cldrDom.removeClass(document.getElementsByTagName("body")[0], "disconnected");
  wasBusted = false;
  hideLoader();
  updateStatus(); // will restart regular status updates
}

/**
 * Process that the locale may have changed, which is indicated by receiving a timestamp
 * from the server different than was previously recorded as surveyNextLocaleStamp
 *
 * The special case where surveyNextLocaleStamp is NaN is handled the same as the case
 * where stamp != surveyNextLocaleStamp. The server response is assumed not to have NaN.
 *
 * The rare case where stamp < surveyNextLocaleStamp (presumably due to wrap-around or
 * server restart) is handled the same as the case where stamp > surveyNextLocaleStamp.
 *
 * @param {String} stamp the timestamp received from the server
 * @param {String} name locale name
 */
function handleChangedLocaleStamp(stamp, name) {
  if (stamp == surveyNextLocaleStamp) {
    if (DEBUG_LOCALE_STAMP) {
      console.log(
        "cldrSurvey.handleChangedLocaleStamp: surveyNextLocaleStamp is unchanged from " +
          surveyNextLocaleStamp
      );
    }
    return;
  }
  if (cldrStatus.isDisconnected()) {
    if (DEBUG_LOCALE_STAMP) {
      console.log(
        "cldrSurvey.handleChangedLocaleStamp: returning early since cldrStatus.isDisconnected"
      );
    }
    return;
  }
  /*
   * For performance, postpone the all-row WHAT_GETROW update if multiple
   * requests (e.g., vote or single-row WHAT_GETROW requests) are pending.
   */
  if (cldrVote.isBusy()) {
    if (DEBUG_LOCALE_STAMP) {
      console.log(
        "cldrSurvey.handleChangedLocaleStamp: returning early since cldrVote.isBusy"
      );
    }
    return;
  }
  if (Object.keys(showers).length == 0) {
    /*
     * TODO: explain this code. When, if ever, is it executed, and why?
     * Typically Object.keys(showers).length != 0.
     */
    if (DEBUG_SHOWER || DEBUG_LOCALE_STAMP) {
      console.log("cldrSurvey.handleChangedLocaleStamp: no showers!");
    }
    cldrDom.updateIf("stchanged_loc", name);
    var locDiv = document.getElementById("stchanged");
    if (locDiv) {
      locDiv.style.display = "block";
    }
  } else {
    for (let i in showers) {
      const fn = showers[i];
      if (fn) {
        if (DEBUG_SHOWER || DEBUG_LOCALE_STAMP) {
          console.log(
            "cldrSurvey.handleChangedLocaleStamp: running showers[" + i + "]"
          );
        }
        fn();
      }
    }
  }
  if (DEBUG_LOCALE_STAMP) {
    console.log(
      "cldrSurvey.handleChangedLocaleStamp: setting surveyNextLocaleStamp = " +
        stamp
    );
  }
  surveyNextLocaleStamp = stamp;
}

/**
 * Update the 'status' if need be.
 */
function showWord() {
  var p = document.getElementById("progress");
  var oneword = document.getElementById("progress_oneword");
  if (oneword == null) {
    // nowhere to show
    return;
  }
  if (
    cldrStatus.isDisconnected() ||
    (progressWord && progressWord == "disconnected") ||
    (progressWord && progressWord == "error")
  ) {
    // top priority
    cldrEvent.popupAlert(
      "danger",
      cldrStatus.stopIcon() + cldrText.get(progressWord)
    );
    busted(); // no further processing.
  } else if (ajaxWord) {
    p.className = "progress-ok";
  } else if (!progressWord || progressWord == "ok") {
    if (specialHeader) {
      p.className = "progress-special";
    } else {
      p.className = "progress-ok";
    }
  } else if (progressWord == "startup") {
    p.className = "progress-ok";
    cldrEvent.popupAlert("warning", cldrText.get("online"));
  }
}

/**
 * Update our progress
 *
 * @param {String} prog the status to update
 */
function updateProgressWord(prog) {
  progressWord = prog;
  showWord();
}

/**
 * Update ajax loading status
 *
 * @param {String} ajax
 */
function updateAjaxWord(ajax) {
  ajaxWord = ajax;
  showWord();
}

/**
 * Return a string to be used with a URL to avoid caching. Ignored by the server.
 *
 * @returns {String} the URL fragment like "&cacheKill=12345", for appending to a query
 */
function cacheKill() {
  return "&cacheKill=" + cacheBuster();
}

/**
 * Return a string to be used with a URL to avoid caching. Ignored by the server.
 *
 * @returns {String} the string like "12345"
 */
function cacheBuster() {
  if (!cacheKillStamp || cacheKillStamp < cldrStatus.getRunningStamp()) {
    cacheKillStamp = cldrStatus.getRunningStamp();
  }
  cacheKillStamp++;
  return "" + cacheKillStamp;
}

/**
 * Note that there is special site news.
 *
 * @param {String} newSpecialHeader site news
 */
function updateSpecialHeader(newSpecialHeader) {
  if (newSpecialHeader && newSpecialHeader.length > 0) {
    specialHeader = newSpecialHeader;
  } else {
    specialHeader = null;
  }
  showWord();
}

function trySurveyLoad() {
  try {
    var url = cldrStatus.getContextPath() + "/survey?" + cacheKill();
    console.log("Attempting to restart ST at " + url);
    cldrAjax.sendXhr({
      url: url,
    });
  } catch (e) {}
}

/**
 * Based on the last received packet of JSON, update our status and the DOM
 *
 * @param {Object} json received
 */
function updateStatusBox(json) {
  if (json.disconnected) {
    json.err_code = "E_DISCONNECTED";
    cldrRetry.handleDisconnect("Misc Disconnect", json, "disconnected"); // unknown
  } else if (json.err_code) {
    console.log("json.err_code == " + json.err_code);
    if (json.err_code == "E_NOT_STARTED") {
      trySurveyLoad();
    }
    cldrRetry.handleDisconnect(json.err_code, json, "disconnected", "status");
  } else if (json.SurveyOK == 0) {
    console.log("json.surveyOK==0");
    trySurveyLoad();
    cldrRetry.handleDisconnect(
      "The SurveyTool server is not ready to accept connections, please retry. ",
      json,
      "disconnected"
    ); // ST has restarted
  } else if (json.status && json.status.isBusted) {
    cldrRetry.handleDisconnect(
      "The SurveyTool server has halted due to an error: " +
        json.status.isBusted,
      json,
      "disconnected"
    ); // Server down- not our fault. Hopefully.
  } else if (!json.status) {
    cldrRetry.handleDisconnect(
      "The SurveyTool server returned a bad status",
      json
    );
  } else if (cldrStatus.runningStampChanged(json.status?.surveyRunningStamp)) {
    cldrRetry.handleDisconnect(
      "The SurveyTool server restarted since this page was loaded. Please retry.",
      json,
      "disconnected"
    ); // desync
  } else if (
    json.status &&
    json.status.isSetup == false &&
    json.SurveyOK == 1
  ) {
    updateProgressWord("startup");
  } else {
    updateProgressWord("ok");
  }

  if (json.status) {
    cldrStatus.updateAll(json.status);
    cldrGui.updateWithStatus();
    if (!updateParts) {
      var visitors = document.getElementById("visitors");
      updateParts = {
        visitors: visitors,
        ug: document.createElement("span"),
        load: document.createElement("span"),
        db: document.createElement("span"),
      };
    }
    //"~1 users, 8pg/uptime: 38:44/load:28% db:0/1"

    var ugtext = "~";
    ugtext = ugtext + json.status.users + " users, ";
    if (json.status.observers > 0) {
      ugtext = ugtext + json.status.observers + " observers, ";
    }
    ugtext = ugtext + json.status.pages + "pg/" + json.status.uptime;
    cldrDom.removeAllChildNodes(updateParts.ug);
    updateParts.ug.appendChild(document.createTextNode(ugtext));

    cldrDom.removeAllChildNodes(updateParts.load);
    updateParts.load.appendChild(
      document.createTextNode("Load:" + json.status.sysload)
    );

    cldrDom.removeAllChildNodes(updateParts.db);
    updateParts.db.appendChild(
      document.createTextNode(
        "db:" + json.status.dbopen + "/" + json.status.dbused
      )
    );

    var fragment = document.createDocumentFragment();
    fragment.appendChild(updateParts.ug);
    fragment.appendChild(document.createTextNode(" "));
    fragment.appendChild(updateParts.load);
    fragment.appendChild(document.createTextNode(" "));
    fragment.appendChild(updateParts.db);

    if (updateParts.visitors) {
      cldrDom.removeAllChildNodes(updateParts.visitors);
      updateParts.visitors.appendChild(fragment);
    }

    const surveyUser = cldrStatus.getSurveyUser();
    if (
      surveyUser !== null &&
      json.millisTillKick &&
      json.millisTillKick >= 0 &&
      json.millisTillKick < 60 * 1 * 1000
    ) {
      warnImpendingDisconnect(json); // show countdown when 1 minute to go
    } else if (
      surveyUser !== null &&
      (json.millisTillKick === 0 || json.session_err)
    ) {
      handleDisconnect(json);
    } else if (surveyUser && json.millisSinceAction) {
      cldrCoverageReset.resetIfLongSinceAction(json.millisSinceAction);
    } else if (
      json.status.specialHeader &&
      json.status.specialHeader.length > 0
    ) {
      updateSpecialHeader(json.status.specialHeader);
    } else {
      updateSpecialHeader(null);
    }
  }
}

function handleDisconnect(json) {
  const kmsg = cldrText.get("ari_sessiondisconnect_message");
  console.log(kmsg);
  updateSpecialHeader(standOutMessage(kmsg));
  cldrStatus.setIsDisconnected(true);
  cldrDom.addClass(document.getElementsByTagName("body")[0], "disconnected");
  if (!json.session_err) {
    json.session_err = "disconnected";
  }
  cldrRetry.handleDisconnect(kmsg, json, "Your session has been disconnected.");
}

function warnImpendingDisconnect(json) {
  const kmsg =
    "Your session will end if not active in about " +
    (parseInt(json.millisTillKick) / 1000).toFixed(0) +
    " seconds.";
  console.log(kmsg);
  updateSpecialHeader(standOutMessage(kmsg));
}

function standOutMessage(txt) {
  return "<b style='font-size: x-large; color: red;'>" + txt + "</b>";
}

/**
 * This is called periodically to fetch latest ST status
 */
function updateStatus() {
  statusTimeout = null;
  if (cldrStatus.isDisconnected()) {
    return;
  }

  const xhrArgs = {
    url: makeUpdateStatusUrl(),
    handleAs: "json",
    load: updateStatusLoadHandler,
    error: updateStatusErrHandler,
    timeout: cldrAjax.mediumTimeout(),
  };

  cldrAjax.sendXhr(xhrArgs);
}

function makeUpdateStatusUrl() {
  let surveyLocaleUrl = "";
  let surveySessionUrl = "";
  const curLocale = cldrStatus.getCurrentLocale();
  if (curLocale !== null && curLocale != "") {
    surveyLocaleUrl = "&_=" + curLocale;
  }
  const sessionId = cldrStatus.getSessionId();
  if (sessionId) {
    surveySessionUrl = "&s=" + sessionId;
  }
  return (
    cldrStatus.getContextPath() +
    "/SurveyAjax?what=status" +
    surveyLocaleUrl +
    surveySessionUrl +
    cacheKill()
  );
}

function updateStatusLoadHandler(json) {
  if (json == null || (json.status && json.status.isBusted)) {
    wasBusted = true;
    busted();
    return; // don't thrash
  }
  const st_err = document.getElementById("st_err");
  if (!st_err) {
    /*
     * This happens if updateStatus is called for a page like about.jsp, browse.jsp;
     * it shouldn't be called in such cases.
     */
    return;
  }
  if (json.err != null && json.err.length > 0) {
    st_err.innerHTML = json.err;
    if (
      json.status &&
      cldrStatus.runningStampChanged(json.status.surveyRunningStamp)
    ) {
      st_err.innerHTML =
        st_err.innerHTML +
        " <b>Note: Lost connection with Survey Tool or it restarted.</b>";
      updateStatusBox({
        disconnected: true,
      });
    }
    st_err.className = "ferrbox";
    wasBusted = true;
    busted();
  } else {
    if (cldrStatus.runningStampChanged(json.status.surveyRunningStamp)) {
      st_err.className = "ferrbox";
      st_err.innerHTML =
        "The SurveyTool has been restarted. Please reload this page to continue.";
      wasBusted = true;
      busted();
    } else if (
      !json.status || // SurveyTool may not be loaded yet
      (wasBusted == true && !json.status.isBusted) ||
      cldrStatus.runningStampChanged(json.status.surveyRunningStamp)
    ) {
      st_err.innerHTML =
        "Note: Lost connection with Survey Tool or it restarted.";
      if (clickContinue != null) {
        st_err.innerHTML =
          st_err.innerHTML +
          " Please <a href='" +
          clickContinue +
          "'>click here</a> to continue.";
      } else {
        st_err.innerHTML =
          st_err.innerHTML + " Please reload this page to continue.";
      }
      st_err.className = "ferrbox";
      busted();
    } else {
      st_err.className = "";
      cldrDom.removeAllChildNodes(st_err);
    }
  }
  updateStatusBox(json);
  if (json.localeStamp) {
    handleChangedLocaleStamp(json.localeStamp, json.localeStampName);
  }
  if (wasBusted == false && json.status.isSetup && loadOnOk != null) {
    window.location.replace(loadOnOk);
  } else {
    statusTimeout = setTimeout(updateStatus, timerSpeed);
  }
}

function updateStatusErrHandler(err) {
  wasBusted = true;
  updateStatusBox({
    err: err,
    disconnected: true,
  });
}

function expediteStatusUpdate() {
  if (statusTimeout) {
    clearTimeout(statusTimeout);
  }
  statusTimeout = setTimeout(updateStatus, fastTimerSpeed);
}

/**
 * Parse a CheckCLDR.StatusAction and return the capabilities table
 *
 * @param action
 * @returns {Object} capabilities
 */
function parseStatusAction(action) {
  if (!action) {
    return statusActionTable.DEFAULT;
  }
  var result = statusActionTable[action];
  if (!result) {
    result = statusActionTable.DEFAULT;
  }
  return result;
}

/**
 * Clone the node, removing the id
 *
 * @param {Node} i
 * @returns {Node} new return, deep clone but with no ids
 */
function cloneAnon(i) {
  if (i == null) {
    return null;
  }
  var o = i.cloneNode(true);
  if (o.id) {
    o.removeAttribute("id");
  }
  return o;
}

/**
 * like cloneAnon, but doing string substitution.
 *
 * @param o
 */
function localizeAnon(o) {
  if (o && o.childNodes) {
    for (var i = 0; i < o.childNodes.length; i++) {
      var k = o.childNodes[i];
      // This is related to elements like <th title='$flyovervorg' id='stui-htmlvorg'>
      if (k.id && k.id.indexOf("stui-html") == 0) {
        const key = k.id.slice(5); // e.g., 'htmlvorg'
        const str = cldrText.get(key); // e.g., "Org"
        if (str) {
          if (str.indexOf("$") == 0) {
            // unique case: "$TRANS_HINT_LANGUAGE_NAME"
            const key2 = str.slice(1); // key2 = "TRANS_HINT_LANGUAGE_NAME"
            const str2 = cldrText.get(key2); // str2 = "English"
            k.innerHTML = str2;
          } else {
            k.innerHTML = str;
          }
        }
        k.removeAttribute("id");
      } else {
        localizeAnon(k);
      }
    }
  }
}

/**
 * Localize the flyover text by replacing $X with ...
 *
 * @param {Node} o
 */
function localizeFlyover(o) {
  if (o && o.childNodes) {
    for (var i = 0; i < o.childNodes.length; i++) {
      var k = o.childNodes[i];
      if (k.title && k.title.indexOf("$") == 0) {
        const key = k.title.slice(1);
        const str = cldrText.get(key);
        if (str) {
          k.title = str;
        } else {
          k.title = null;
        }
      } else {
        localizeFlyover(k);
      }
    }
  }
}

/**
 * cloneAnon, then call localizeAnon
 *
 * @param {Node} i
 * @returns {Node}
 */
function cloneLocalizeAnon(i) {
  var o = cloneAnon(i);
  if (o) {
    localizeAnon(o);
  }
  return o;
}

/**
 * Return an array of all children of the item which are tags
 *
 * @param {Node} tr
 * @returns {Array}
 */
function getTagChildren(tr) {
  var rowChildren = [];
  for (var k in tr.childNodes) {
    var t = tr.childNodes[k];
    if (t.tagName) {
      rowChildren.push(t);
    }
  }
  return rowChildren;
}

/**
 * Append an icon to the div
 *
 * @param {Node} td
 * @Param {String} className name of icon's CSS class
 */
function addIcon(td, className) {
  var star = document.createElement("span");
  star.className = className;
  star.innerHTML = "&nbsp; &nbsp;";
  td.appendChild(star);
  return star;
}

function testsToHtml(tests) {
  var newHtml = "";
  if (!tests) {
    return newHtml;
  }

  for (var i = 0; i < tests.length; i++) {
    var testItem = tests[i];
    const { entireLocale } = testItem;
    if (entireLocale) {
      continue; // skip entireLocale errors
    }
    newHtml += "<p class='trInfo tr_" + testItem.type;
    if (testItem.type == "Warning") {
      newHtml += " alert alert-warning fix-popover-help";
    } else if (testItem.type == "Error") {
      newHtml += " alert alert-danger fix-popover-help";
    }
    newHtml +=
      "' title='" +
      testItem.type +
      ": " +
      (testItem.cause || "Unknown") +
      "." +
      testItem.subtype +
      "'>";
    if (testItem.type == "Warning") {
      newHtml += cldrStatus.warnIcon();
    } else if (testItem.type == "Error") {
      newHtml += cldrStatus.stopIcon();
    }
    newHtml += testItem.message;
    // Add a link

    if (testItem.subtypeUrl) {
      newHtml += ' <a href="' + testItem.subtypeUrl + '">(how to fix…)</a>';
    }

    newHtml += "</p>\n";
  }
  return newHtml;
}

function findItemByValue(items, value) {
  if (!items) {
    return null;
  }
  for (var i in items) {
    if (value == items[i].value) {
      return items[i];
    }
  }
  return null;
}

function appendExtraAttributes(container, theRow) {
  for (var attr in theRow.extraAttributes) {
    var attrval = theRow.extraAttributes[attr];
    var extraChunk = cldrDom.createChunk(
      attr + "=" + attrval,
      "span",
      "extraAttribute"
    );
    container.appendChild(extraChunk);
  }
}

/**
 * @param loc optional
 * @returns locale bundle
 */
function locInfo(loc) {
  if (!loc) {
    loc = cldrStatus.getCurrentLocale();
  }
  const locmap = cldrLoad.getTheLocaleMap();
  return locmap.getLocaleInfo(loc);
}

function setOverrideDir(dir) {
  overridedir = dir;
}

/**
 * Set the dir and lang attributes for a node that represents
 * a CLDR value.
 * Also appends the 'cldrValue' class to the node.
 * @param {Node} node DOM node
 * @param {String} loc locale
 */
function setLang(node, loc) {
  var info = locInfo(loc);

  if (overridedir) {
    node.dir = overridedir;
  } else if (info && info.dir) {
    node.dir = info.dir;
  }

  if (info && info.bcp47) {
    node.lang = info.bcp47;
  }

  // defined in surveytool.css and shared with CldrValue.vue
  node.classList.add("cldrValue");
}

/**
 * Show the 'loading' sign
 *
 * @param {String} text text to use
 */
function showLoader(text) {
  updateAjaxWord(text);
}

/**
 * Hide the 'loading' sign
 */
function hideLoader() {
  updateAjaxWord(null);
}

/**
 * Go to the next (1) or the previous page (1) during the vetting
 *
 * @param {Integer} shift next page (1) or previous (-1)
 */
function chgPage(shift) {
  // no page, or wrong shift
  const _thePages = cldrMenu.getThePages();
  if (!_thePages || (shift !== -1 && shift !== 1)) {
    return;
  }

  var menus = getMenusFilteredByCov();
  var parentIndex = 0;
  var index = 0;
  var parent = _thePages.pageToSection[cldrStatus.getCurrentPage()].id;

  // get the parent index
  for (var m in menus) {
    var menu = menus[m];
    if (menu.id === parent) {
      parentIndex = parseInt(m);
      break;
    }
  }

  for (var m in menus[parentIndex].pagesFiltered) {
    var menu = menus[parentIndex].pagesFiltered[m];
    if (menu.id === cldrStatus.getCurrentPage()) {
      index = parseInt(m);
      break;
    }
  }
  // go to the next one
  index += parseInt(shift);

  if (index >= menus[parentIndex].pagesFiltered.length) {
    parentIndex++;
    index = 0;
    if (parentIndex >= menus.length) {
      parentIndex = 0;
    }
  }

  if (index < 0) {
    parentIndex--;
    if (parentIndex < 0) {
      parentIndex = menus.length - 1;
    }
    index = menus[parentIndex].pagesFiltered.length - 1;
  }
  cldrStatus.setCurrentSection(menus[parentIndex].id);
  cldrStatus.setCurrentPage(menus[parentIndex].pagesFiltered[index].id);
  cldrStatus.setCurrentId(null);

  cldrLoad.reloadV();

  var sidebar = $("#locale-menu #" + cldrStatus.getCurrentPage());
  sidebar.closest(".open-menu").click();
}

/**
 * Get all the menus under this coverage
 *
 * @return {Array} list of all the menus under this coverage
 */
function getMenusFilteredByCov() {
  const _thePages = cldrMenu.getThePages();
  if (!_thePages) {
    return;
  }
  // get name of current coverage
  var cov = cldrCoverage.getSurveyUserCov();
  if (!cov) {
    cov = cldrCoverage.getSurveyOrgCov(cldrStatus.getCurrentLocale());
  }

  // get the value
  var val = cldrCoverage.covValue(cov);
  var sections = _thePages.sections;
  var menus = [];
  // add filtered pages
  for (var s in sections) {
    var section = sections[s];
    var pages = section.pages;
    var sectionContent = [];
    for (var p in pages) {
      var page = pages[p];
      var key = Object.keys(page.levs).pop();
      if (parseInt(page.levs[key]) <= val) sectionContent.push(page);
    }
    if (sectionContent.length) {
      section.pagesFiltered = sectionContent;
      menus.push(section);
    }
  }
  return menus;
}

function setShower(id, func) {
  showers[id] = func;
}

export {
  INHERITANCE_MARKER,
  addIcon,
  appendExtraAttributes,
  cacheBuster,
  cacheKill,
  chgPage,
  cloneAnon,
  cloneLocalizeAnon,
  createGravatar,
  expediteStatusUpdate,
  findItemByValue,
  getDidUnbust,
  getTagChildren,
  getXpathMap,
  hideLoader,
  isInputBusy,
  localizeFlyover,
  parseStatusAction,
  setLang,
  setOverrideDir,
  setShower,
  showLoader,
  testsToHtml,
  unbust,
  updateProgressWord,
  updateSpecialHeader,
  updateStatus,
};

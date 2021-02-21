/**
 * cldrSurvey: encapsulate miscellaneous Survey Tool functions
 */
import * as cldrAjax from "./cldrAjax.js";
import * as cldrDom from "./cldrDom.js";
import * as cldrEvent from "./cldrEvent.js";
import * as cldrForum from "./cldrForum.js";
import * as cldrGui from "./cldrGui.js";
import * as cldrInfo from "./cldrInfo.js";
import * as cldrLoad from "./cldrLoad.js";
import * as cldrMenu from "./cldrMenu.js";
import * as cldrRetry from "./cldrRetry.js";
import * as cldrStatus from "./cldrStatus.js";
import * as cldrTable from "./cldrTable.js";
import * as cldrText from "./cldrText.js";
import { XpathMap } from "./cldrXpathMap.js";

/*
 * INHERITANCE_MARKER indicates that the value of a candidate item is inherited.
 * Compare INHERITANCE_MARKER in CldrUtility.java.
 */
const INHERITANCE_MARKER = "↑↑↑";

let xpathMap = null;

let wasBusted = false;
let didUnbust = false;

let loadOnOk = null; // TODO: SurveyMain.java writes scripts that try to reference loadOnOk

let clickContinue = null; // TODO: SurveyMain.java writes scripts that try to reference clickContinue

let surveyNextLocaleStamp = 0;

let showers = {};

let progressWord = null;
let ajaxWord = null;
let specialHeader = null; // TODO: supposed to be same as specialHeader in cldrStatus.js, or not?

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
 * May (theoretically, if it were visible) be changed by js written by SurveyMain.showOfflinePage, etc.
 * @property timerSpeed
 */
let timerSpeed = 15000; // 15 seconds

let surveyLevels = null;

let surveyOrgCov = null;

let surveyUserCov = null;

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
 * TODO: clarify dependence on "dijitInp"; is that still used here, and if so, when?
 * Add automated regression testing to anticipate future changes to bootstrap/dojo/dijit/etc.
 *
 * Called only from CldrSurveyVettingLoader.js
 */
function isInputBusy() {
  if (!window.getSelection) {
    return false;
  }
  var sel = window.getSelection();
  if (sel && sel.anchorNode && sel.anchorNode.className) {
    if (sel.anchorNode.className.indexOf("dijitInp") != -1) {
      return true;
    }
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

// referenced in cldrGui.js but not actually called yet for non-dojo, due to data-dojo-props dependency
function unbust() {
  didUnbust = true;
  console.log("Un-busting");
  progressWord = "unbusted";
  cldrStatus.setIsDisconnected(false);
  cldrDom.removeClass(document.getElementsByTagName("body")[0], "disconnected");
  wasBusted = false;
  cldrAjax.clearXhr();
  hideLoader();
  updateStatus(); // will restart regular status updates
}

/**
 * Process that the locale has changed under us.
 *
 * @param {String} stamp timestamp
 * @param {String} name locale name
 */
function handleChangedLocaleStamp(stamp, name) {
  if (cldrStatus.isDisconnected()) {
    return;
  }
  if (stamp <= surveyNextLocaleStamp) {
    return;
  }
  /*
   * For performance, postpone the all-row WHAT_GETROW update if multiple
   * requests (e.g., vote or single-row WHAT_GETROW requests) are pending.
   */
  if (cldrAjax.queueCount() > 1) {
    return;
  }
  if (Object.keys(showers).length == 0) {
    /*
     * TODO: explain this code. When, if ever, is it executed, and why?
     * Typically Object.keys(showers).length != 0.
     */
    cldrDom.updateIf("stchanged_loc", name);
    var locDiv = document.getElementById("stchanged");
    if (locDiv) {
      locDiv.style.display = "block";
    }
  } else {
    for (let i in showers) {
      const fn = showers[i];
      if (fn) {
        fn();
      }
    }
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
  } else if (cldrStatus.runningStampChanged(json.status.surveyRunningStamp)) {
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
    if (json.status.guests > 0) {
      ugtext = ugtext + json.status.guests + " guests, ";
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

    function standOutMessage(txt) {
      return "<b style='font-size: x-large; color: red;'>" + txt + "</b>";
    }

    const surveyUser = cldrStatus.getSurveyUser();
    if (
      surveyUser !== null &&
      json.millisTillKick &&
      json.millisTillKick >= 0 &&
      json.millisTillKick < 60 * 1 * 1000
    ) {
      // show countdown when 1 minute to go
      var kmsg =
        "Your session will end if not active in about " +
        (parseInt(json.millisTillKick) / 1000).toFixed(0) +
        " seconds.";
      console.log(kmsg);
      updateSpecialHeader(standOutMessage(kmsg));
    } else if (
      surveyUser !== null &&
      (json.millisTillKick === 0 || json.session_err)
    ) {
      var kmsg = cldrText.get("ari_sessiondisconnect_message");
      console.log(kmsg);
      updateSpecialHeader(standOutMessage(kmsg));
      cldrStatus.setIsDisconnected(true);
      cldrDom.addClass(
        document.getElementsByTagName("body")[0],
        "disconnected"
      );
      if (!json.session_err) {
        json.session_err = "disconnected";
      }
      cldrRetry.handleDisconnect(
        kmsg,
        json,
        "Your session has been disconnected."
      );
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

/**
 * This is called periodically to fetch latest ST status
 */
function updateStatus() {
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
    if (surveyNextLocaleStamp == 0) {
      surveyNextLocaleStamp = json.localeStamp;
    } else {
      if (json.localeStamp > surveyNextLocaleStamp) {
        handleChangedLocaleStamp(json.localeStamp, json.localeStampName);
      }
    }
  }
  if (wasBusted == false && json.status.isSetup && loadOnOk != null) {
    window.location.replace(loadOnOk);
  } else {
    setTimeout(updateStatus, timerSpeed);
  }
}

function updateStatusErrHandler(err) {
  wasBusted = true;
  updateStatusBox({
    err: err,
    disconnected: true,
  });
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
 * Determine whether a JSONified array of CheckCLDR.CheckStatus is overall a warning or an error.
 *
 * @param {Object} testResults - array of CheckCLDR.CheckStatus
 * @returns {String} 'Warning' or 'Error' or null
 *
 * Note: when a user votes, the response to the POST request includes json.testResults,
 * which often (always?) includes a warning "Needed to meet ... coverage level", possibly
 * in addition to other warnings or errors. We then return Warning, resulting in
 * div.className = "d-item-warn" and a temporary yellow background for the cell, which, however,
 * goes back to normal color (with "d-item") after a multiple-row response is received.
 * The message "Needed to meet ..." may not actually be displayed, in which case the yellow
 * background is distracting; its purpose should be clarified.
 */
function getTestKind(testResults) {
  if (!testResults) {
    return null;
  }
  var theKind = null;
  for (var i = 0; i < testResults.length; i++) {
    var tr = testResults[i];
    if (tr.type == "Warning") {
      theKind = tr.type;
    } else if (tr.type == "Error") {
      return tr.type;
    }
  }
  return theKind;
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
 * Wire up the button to perform a submit
 *
 * @param button
 * @param tr
 * @param theRow
 * @param vHash
 * @param box
 */
function wireUpButton(button, tr, theRow, vHash, box) {
  if (box) {
    button.id = "CHANGE_" + tr.rowHash;
    vHash = "";
    box.onchange = function () {
      handleWiredClick(tr, theRow, vHash, box, button, "submit");
      return false;
    };
    box.onkeypress = function (e) {
      if (!e || !e.keyCode) {
        return true; // not getting the point here.
      } else if (e.keyCode == 13) {
        handleWiredClick(tr, theRow, vHash, box, button);
        return false;
      } else {
        return true;
      }
    };
  } else if (vHash == null) {
    button.id = "NO_" + tr.rowHash;
    vHash = "";
  } else {
    button.id = "v" + vHash + "_" + tr.rowHash;
  }
  cldrDom.listenFor(button, "click", function (e) {
    handleWiredClick(tr, theRow, vHash, box, button);
    cldrEvent.stopPropagation(e);
    return false;
  });

  // proposal issues
  if (tr.myProposal) {
    if (button == tr.myProposal.button) {
      button.className = "ichoice-x";
      button.checked = true;
      tr.lastOn = button;
    } else {
      button.className = "ichoice-o";
      button.checked = false;
    }
  } else if (theRow.voteVhash == vHash && !box) {
    button.className = "ichoice-x";
    button.checked = true;
    tr.lastOn = button;
  } else {
    button.className = "ichoice-o";
    button.checked = false;
  }
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

/**
 * Check if we need LRM/RLM marker to display
 * @param field choice field to append if needed
 * @param value the value of votes (check &lrm; &rlm)
 */
function checkLRmarker(field, value) {
  if (value) {
    if (value.indexOf("\u200E") > -1 || value.indexOf("\u200F") > -1) {
      value = value
        .replace(/\u200E/g, '<span class="visible-mark">&lt;LRM&gt;</span>')
        .replace(/\u200F/g, '<span class="visible-mark">&lt;RLM&gt;</span>');
      var lrm = document.createElement("div");
      lrm.className = "lrmarker-container";
      lrm.innerHTML = value;
      field.appendChild(lrm);
    }
  }
}

/**
 * Append just an editable span representing a candidate voting item
 *
 * @param div {DOM} div to append to
 * @param value {String} string value
 * @param pClass {String} html class for the voting item
 * @return {DOM} the new span
 */
function appendItem(div, value, pClass) {
  if (!value) {
    return;
  }
  var text = document.createTextNode(value);
  var span = document.createElement("span");
  span.appendChild(text);
  if (!value) {
    span.className = "selected";
  } else if (pClass) {
    span.className = pClass;
  } else {
    span.className = "value";
  }
  div.appendChild(span);
  return span;
}

function testsToHtml(tests) {
  var newHtml = "";
  if (!tests) {
    return newHtml;
  }
  for (var i = 0; i < tests.length; i++) {
    var testItem = tests[i];
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
      (testItem.cause || { class: "Unknown" }).class +
      "." +
      testItem.subType +
      "'>";
    if (testItem.type == "Warning") {
      newHtml += cldrStatus.warnIcon();
    } else if (testItem.type == "Error") {
      newHtml += cldrStatus.stopIcon();
    }
    newHtml += testItem.message;
    // Add a link

    if (testItem.subTypeUrl) {
      newHtml += ' <a href="' + testItem.subTypeUrl + '">(how to fix…)</a>';
    }

    newHtml += "</p>";
  }
  return newHtml;
}

function setDivClass(div, testKind) {
  if (!testKind) {
    div.className = "d-item";
  } else if (testKind == "Warning") {
    div.className = "d-item-warn";
  } else if (testKind == "Error") {
    div.className = "d-item-err";
  } else {
    div.className = "d-item";
  }
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

/**
 * Show an item that's not in the saved data, but has been proposed newly by the user.
 * Called only by loadHandler in handleWiredClick.
 * Used for "+" button, both in Dashboard Fix pop-up window and in regular (non-Dashboard) table.
 */
function showProposedItem(inTd, tr, theRow, value, tests, json) {
  // Find where our value went.
  var ourItem = findItemByValue(theRow.items, value);
  var testKind = getTestKind(tests);
  var ourDiv = null;
  var wrap;
  if (!ourItem) {
    /*
     * This may happen if, for example, the user types a space (" ") in
     * the input pop-up window and presses Enter. The value has been rejected
     * by the server. Then we show an additional pop-up window with the error message
     * from the server like "Input Processor Error: DAIP returned a 0 length string"
     */
    ourDiv = document.createElement("div");
    var newButton = cloneAnon(document.getElementById("proto-button"));
    const otherCell = tr.querySelector(".othercell");
    if (otherCell && tr.myProposal) {
      otherCell.removeChild(tr.myProposal);
    }
    tr.myProposal = ourDiv;
    tr.myProposal.value = value;
    tr.myProposal.button = newButton;
    if (newButton) {
      newButton.value = value;
      if (tr.lastOn) {
        tr.lastOn.checked = false;
        tr.lastOn.className = "ichoice-o";
      }
      wireUpButton(newButton, tr, theRow, "[retry]", {
        value: value,
      });
      wrap = wrapRadio(newButton);
      ourDiv.appendChild(wrap);
    }
    var h3 = document.createElement("span");
    var span = appendItem(h3, value, "value");
    setLang(span);
    ourDiv.appendChild(h3);
    if (otherCell) {
      otherCell.appendChild(tr.myProposal);
    }
  } else {
    ourDiv = ourItem.div;
  }
  if (json && !parseStatusAction(json.statusAction).vote) {
    ourDiv.className = "d-item-err";

    const replaceErrors =
      json.statusAction === "FORBID_PERMANENT_WITHOUT_FORUM";
    if (replaceErrors) {
      /*
       * Special case: for clarity, replace any warnings/errors that may be
       * in tests[] with a single error message for this situation.
       */
      tests = [
        {
          type: "Error",
          message: cldrText.get("StatusAction_" + json.statusAction),
        },
      ];
    }

    var input = $(inTd).closest("tr").find(".input-add");
    if (input) {
      input.closest(".form-group").addClass("has-error");
      input
        .popover("destroy")
        .popover({
          placement: "bottom",
          html: true,
          content: testsToHtml(tests),
          trigger: "hover",
        })
        .popover("show");
      if (tr.myProposal) tr.myProposal.style.display = "none";
    }
    if (ourItem || (replaceErrors && value === "") /* Abstain */) {
      let str = cldrText.sub(
        "StatusAction_msg",
        [cldrText.get("StatusAction_" + json.statusAction)],
        "p",
        ""
      );
      var str2 = cldrText.sub(
        "StatusAction_popupmsg",
        [cldrText.get("StatusAction_" + json.statusAction), theRow.code],
        "p",
        ""
      );
      // show in modal popup (ouch!)
      alert(str2);

      // show this message in a sidebar also
      const message = cldrStatus.stopIcon() + str;
      cldrInfo.showWithRow(message, tr);
    }
    return;
  } else if (json && json.didNotSubmit) {
    ourDiv.className = "d-item-err";
    const message = "(ERROR: Unknown error - did not submit this value.)";
    cldrInfo.showWithRow(message, tr);
    return;
  } else {
    setDivClass(ourDiv, testKind);
  }

  if (testKind || !ourItem) {
    var div3 = document.createElement("div");
    var newHtml = "";
    newHtml += testsToHtml(tests);

    if (!ourItem) {
      var h3 = document.createElement("h3");
      var span = appendItem(h3, value, "value");
      setLang(span);
      h3.className = "span";
      div3.appendChild(h3);
    }
    var newDiv = document.createElement("div");
    div3.appendChild(newDiv);
    newDiv.innerHTML = newHtml;
    if (json && !parseStatusAction(json.statusAction).vote) {
      div3.appendChild(
        cldrDom.createChunk(
          cldrText.sub(
            "StatusAction_msg",
            [cldrText.get("StatusAction_" + json.statusAction)],
            "p",
            ""
          )
        )
      );
    }

    div3.popParent = tr;

    // will replace any existing function
    var ourShowFn = function (showDiv) {
      var retFn;
      if (ourItem && ourItem.showFn) {
        retFn = ourItem.showFn(showDiv);
      } else {
        retFn = null;
      }
      if (tr.myProposal && value == tr.myProposal.value) {
        // make sure it wasn't submitted twice
        showDiv.appendChild(div3);
      }
      return retFn;
    };
    cldrInfo.listen(null, tr, ourDiv, ourShowFn);
    cldrInfo.showRowObjFunc(tr, ourDiv, ourShowFn);
  }

  return false;
}

/**
 * Return a function that will show info for the given item in the Info Panel.
 *
 * @param theRow the data row
 * @param item the candidate item
 * @returns the function
 *
 * Called only by addVitem.
 */
function showItemInfoFn(theRow, item) {
  return function (td) {
    var h3 = document.createElement("div");
    var displayValue = item.value;
    if (item.value === INHERITANCE_MARKER) {
      displayValue = theRow.inheritedValue;
    }

    var span = appendItem(h3, displayValue, item.pClass);
    setLang(span);
    h3.className = "span";
    td.appendChild(h3);

    if (item.value) {
      /*
       * Strings produced here, used as keys for cldrText.sub(), may include:
       *  "pClass_winner", "pClass_alias", "pClass_fallback", "pClass_fallback_code", "pClass_fallback_root", "pClass_loser".
       *  See getPClass in DataSection.java.
       *
       *  TODO: why not show stars, etc., here?
       */
      h3.appendChild(
        cldrDom.createChunk(
          cldrText.sub("pClass_" + item.pClass, item),
          "p",
          "pClassExplain"
        )
      );
    }

    if (item.value === INHERITANCE_MARKER) {
      addJumpToOriginal(theRow, h3);
    }

    var newDiv = document.createElement("div");
    td.appendChild(newDiv);

    if (item.tests) {
      newDiv.innerHTML = testsToHtml(item.tests);
    } else {
      newDiv.innerHTML = "<i>no tests</i>";
    }

    if (item.example) {
      appendExample(td, item.example);
    }
  }; // end function(td)
}

/**
 * Add a link in the Info Panel for "Jump to Original" (cldrText.get('followAlias')),
 * if theRow.inheritedLocale or theRow.inheritedXpid is defined.
 *
 * Normally at least one of theRow.inheritedLocale and theRow.inheritedXpid should be
 * defined whenever we have an INHERITANCE_MARKER item. Otherwise an error is reported
 * by checkRowConsistency.
 *
 * This is currently (2018-12-01) the only place inheritedLocale or inheritedXpid is used on the client.
 * An alternative would be for the server to send the link (clickyLink.href), instead of inheritedLocale
 * and inheritedXpid, to the client, avoiding the need for the client to know so much, including the need
 * to replace 'code-fallback' with 'root' or when to use cldrStatus.getCurrentLocale() in place of inheritedLocale
 * or use xpstrid in place of inheritedXpid.
 *
 * @param theRow the row
 * @param el the element to which to append the link
 */
function addJumpToOriginal(theRow, el) {
  if (theRow.inheritedLocale || theRow.inheritedXpid) {
    var loc = theRow.inheritedLocale;
    var xpstrid = theRow.inheritedXpid || theRow.xpstrid;
    if (!loc) {
      loc = cldrStatus.getCurrentLocale();
    } else if (loc === "code-fallback") {
      /*
       * Never use 'code-fallback' in the link, use 'root' instead.
       * On the server, 'code-fallback' sometimes goes by the name XMLSource.CODE_FALLBACK_ID.
       * Reference: https://unicode.org/cldr/trac/ticket/11622
       */
      loc = "root";
    }
    if (
      xpstrid === theRow.xpstrid && // current hash
      loc === cldrStatus.getCurrentLocale()
    ) {
      // current locale
      // i.e., following the alias would come back to the current item
      el.appendChild(
        cldrDom.createChunk(
          cldrText.get("noFollowAlias"),
          "span",
          "followAlias"
        )
      );
    } else {
      var clickyLink = cldrDom.createChunk(
        cldrText.get("followAlias"),
        "a",
        "followAlias"
      );
      clickyLink.href = "#/" + loc + "//" + xpstrid;
      el.appendChild(clickyLink);
    }
  }
}

function appendExample(parent, text, loc) {
  var div = document.createElement("div");
  div.className = "d-example well well-sm";
  div.innerHTML = text;
  setLang(div, loc);
  parent.appendChild(div);
  return div;
}

/**
 * Append a Vetting item ( vote button, etc ) to the row.
 *
 * @param {DOM} td cell to append into
 * @param {DOM} tr which row owns the items
 * @param {JSON} theRow JSON content of this row's data
 * @param {JSON} item JSON of the specific item we are adding
 * @param {DOM} newButton	 button prototype object
 */
function addVitem(td, tr, theRow, item, newButton) {
  var displayValue = item.value;
  if (displayValue === INHERITANCE_MARKER) {
    displayValue = theRow.inheritedValue;
  }
  if (!displayValue) {
    return;
  }
  var div = document.createElement("div");
  var isWinner = td == tr.proposedcell;
  var testKind = getTestKind(item.tests);
  setDivClass(div, testKind);
  item.div = div; // back link

  var choiceField = document.createElement("div");
  var wrap;
  choiceField.className = "choice-field";
  if (newButton) {
    newButton.value = item.value;
    wireUpButton(newButton, tr, theRow, item.valueHash);
    wrap = wrapRadio(newButton);
    choiceField.appendChild(wrap);
  }
  var subSpan = document.createElement("span");
  subSpan.className = "subSpan";
  var span = appendItem(subSpan, displayValue, item.pClass);
  choiceField.appendChild(subSpan);

  setLang(span);
  checkLRmarker(choiceField, item.value);

  if (item.isBaselineValue == true) {
    appendIcon(choiceField, "i-star", cldrText.get("voteInfo_baseline_desc"));
  }
  if (item.votes && !isWinner) {
    if (
      item.valueHash == theRow.voteVhash &&
      theRow.canFlagOnLosing &&
      !theRow.rowFlagged
    ) {
      addIcon(choiceField, "i-stop"); // DEBUG
    }
  }

  /*
   * Note: history is maybe only defined for debugging; won't normally display it in production.
   * See DataSection.USE_CANDIDATE_HISTORY which currently should be false for production, so
   * that item.history will be undefined.
   */
  if (item.history) {
    const historyText = " ☛" + item.history;
    const historyTag = cldrDom.createChunk(historyText, "span", "");
    choiceField.appendChild(historyTag);
    cldrInfo.listen(historyText, tr, historyTag, null);
  }

  const surveyUser = cldrStatus.getSurveyUser();
  if (
    newButton &&
    theRow.voteVhash == item.valueHash &&
    theRow.items[theRow.voteVhash].votes &&
    theRow.items[theRow.voteVhash].votes[surveyUser.id] &&
    theRow.items[theRow.voteVhash].votes[surveyUser.id].overridedVotes
  ) {
    var overrideTag = cldrDom.createChunk(
      theRow.items[theRow.voteVhash].votes[surveyUser.id].overridedVotes,
      "span",
      "i-override"
    );
    choiceField.appendChild(overrideTag);
  }

  div.appendChild(choiceField);

  // wire up the onclick function for the Info Panel
  td.showFn = item.showFn = showItemInfoFn(theRow, item);
  div.popParent = tr;
  cldrInfo.listen(null, tr, div, td.showFn);
  td.appendChild(div);

  if (item.example && item.value != item.examples) {
    appendExample(div, item.example);
  }
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

function getSurveyLevels() {
  return surveyLevels;
}

function setSurveyLevels(levs) {
  return (surveyLevels = levs);
}

/**
 * Get numeric, given string
 *
 * @param {String} lev
 * @return {Number} or 0
 */
function covValue(lev) {
  lev = lev.toUpperCase();
  const levs = getSurveyLevels();
  if (levs && levs[lev]) {
    return parseInt(levs[lev].level);
  } else {
    return 0;
  }
}

function covName(lev) {
  const levs = getSurveyLevels();
  if (!levs) {
    return null;
  }
  for (var k in levs) {
    if (parseInt(levs[k].level) == lev) {
      return k.toLowerCase();
    }
  }
  return null;
}

function effectiveCoverage() {
  const orgCov = getSurveyOrgCov();
  if (!orgCov) {
    throw new Error("surveyOrgCov not yet initialized");
  }
  const userCov = getSurveyUserCov();
  if (userCov) {
    return covValue(userCov);
  } else {
    return covValue(orgCov);
  }
}

function getSurveyOrgCov() {
  return surveyOrgCov;
}

function setSurveyOrgCov(cov) {
  surveyOrgCov = cov;
}

function getSurveyUserCov() {
  return surveyUserCov;
}

function setSurveyUserCov(cov) {
  surveyUserCov = cov;
}

function updateCovFromJson(json) {
  if (json.covlev_user && json.covlev_user != "default") {
    setSurveyUserCov(json.covlev_user);
  } else {
    setSurveyUserCov(null);
  }

  if (json.covlev_org) {
    setSurveyOrgCov(json.covlev_org);
  } else {
    setSurveyOrgCov(null);
  }
}

/**
 * Update the coverage classes, show and hide things in and out of coverage
 */
function updateCoverage(theDiv) {
  if (theDiv == null) return;
  var theTable = theDiv.theTable;
  if (theTable == null) return;
  if (!theTable.origClass) {
    theTable.origClass = theTable.className;
  }
  const levs = getSurveyLevels();
  if (levs != null) {
    var effective = effectiveCoverage();
    var newStyle = theTable.origClass;
    for (var k in levs) {
      var level = levs[k];

      if (effective < parseInt(level.level)) {
        newStyle = newStyle + " hideCov" + level.level;
      }
    }
    if (newStyle != theTable.className) {
      theTable.className = newStyle;
    }
  }
}

function appendIcon(toElement, className, title) {
  var e = cldrDom.createChunk(null, "div", className);
  e.title = title;
  toElement.appendChild(e);
  return e;
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
}

/**
 * Reload a specific row
 *
 * Called by loadHandler in handleWiredClick
 */
function refreshSingleRow(tr, theRow, onSuccess, onFailure) {
  showLoader(cldrText.get("loadingOneRow"));

  let ourUrl =
    cldrStatus.getContextPath() +
    "/SurveyAjax?what=getrow" +
    "&_=" +
    cldrStatus.getCurrentLocale() +
    "&xpath=" +
    theRow.xpathId +
    "&fhash=" +
    tr.rowHash +
    "&s=" +
    cldrStatus.getSessionId() +
    "&automatic=t";

  if (cldrStatus.isDashboard()) {
    ourUrl += "&dashboard=true";
  }

  var loadHandler = function (json) {
    try {
      if (json.section.rows[tr.rowHash]) {
        theRow = json.section.rows[tr.rowHash];
        tr.theTable.json.section.rows[tr.rowHash] = theRow;
        cldrTable.updateRow(tr, theRow);

        hideLoader();
        onSuccess(theRow);
        if (cldrStatus.isDashboard()) {
          refreshFixPanel(json);
        } else {
          cldrInfo.showRowObjFunc(tr, tr.proposedcell, tr.proposedcell.showFn);
          refreshCounterVetting();
        }
      } else {
        tr.className = "ferrbox";
        console.log("could not find " + tr.rowHash + " in " + json);
        onFailure(
          "refreshSingleRow: Could not refresh this single row: Server failed to return xpath #" +
            theRow.xpathId +
            " for locale " +
            cldrStatus.getCurrentLocale()
        );
      }
    } catch (e) {
      console.log("Error in ajax post [refreshSingleRow] ", e.message);
    }
  };
  var errorHandler = function (err) {
    console.log("Error: " + err);
    tr.className = "ferrbox";
    tr.innerHTML =
      "Error while  loading: <div style='border: 1px solid red;'>" +
      err +
      "</div>";
    onFailure("err", err);
  };
  var xhrArgs = {
    url: ourUrl + cacheKill(),
    handleAs: "json",
    load: loadHandler,
    error: errorHandler,
    timeout: cldrAjax.mediumTimeout(),
  };
  cldrAjax.queueXhr(xhrArgs);
}

/**
 * Bottleneck for voting buttons
 */
function handleWiredClick(tr, theRow, vHash, box, button, what) {
  var value = "";
  var valToShow;
  if (tr.wait) {
    return;
  }
  if (box) {
    valToShow = box.value;
    value = box.value;
    if (value.length == 0) {
      if (box.focus) {
        box.focus();
        myUnDefer();
      }
      return; // nothing entered.
    }
  } else {
    valToShow = button.value;
  }
  if (!what) {
    what = "submit";
  }
  if (what == "submit") {
    button.className = "ichoice-x-ok"; // TODO: ichoice-inprogress? spinner?
    showLoader(cldrText.get("voting"));
  } else {
    showLoader(cldrText.get("checking"));
  }

  // select
  cldrLoad.updateCurrentId(theRow.xpstrid);

  // and scroll
  cldrLoad.showCurrentId();

  if (tr.myProposal) {
    const otherCell = tr.querySelector(".othercell");
    if (otherCell) {
      otherCell.removeChild(tr.myProposal);
    }
    tr.myProposal = null; // mark any pending proposal as invalid.
  }

  var myUnDefer = function () {
    tr.wait = false;
  };
  tr.wait = true;
  cldrInfo.reset();
  theRow.proposedResults = null;

  console.log(
    "Vote for " + tr.rowHash + " v='" + vHash + "', value='" + value + "'"
  );
  var ourContent = {
    what: what,
    xpath: tr.xpathId,
    _: cldrStatus.getCurrentLocale(),
    fhash: tr.rowHash,
    vhash: vHash,
    s: tr.theTable.session,
  };

  let ourUrl = cldrStatus.getContextPath() + "/SurveyAjax";

  var voteLevelChanged = document.getElementById("voteLevelChanged");
  if (voteLevelChanged) {
    ourContent.voteLevelChanged = voteLevelChanged.value;
  }

  var originalTrClassName = tr.className;
  tr.className = "tr_checking1";

  var loadHandler = function (json) {
    /*
     * Restore tr.className, so it stops being 'tr_checking1' immediately on receiving
     * any response. It may change again below, such as to 'tr_err' or 'tr_checking2'.
     */
    tr.className = originalTrClassName;
    try {
      if (json.err && json.err.length > 0) {
        tr.className = "tr_err";
        cldrRetry.handleDisconnect("Error submitting a vote", json);
        tr.innerHTML =
          "<td colspan='4'>" +
          cldrStatus.stopIcon() +
          " Could not check value. Try reloading the page.<br>" +
          json.err +
          "</td>";
        myUnDefer();
        cldrRetry.handleDisconnect("Error submitting a vote", json);
      } else {
        if (json.submitResultRaw) {
          // if submitted..
          tr.className = "tr_checking2";
          refreshSingleRow(
            tr,
            theRow,
            function (theRow) {
              // submit went through. Now show the pop.
              button.className = "ichoice-o";
              button.checked = false;
              hideLoader();
              if (json.testResults && (json.testWarnings || json.testErrors)) {
                // tried to submit, have errs or warnings.
                showProposedItem(
                  tr.inputTd,
                  tr,
                  theRow,
                  valToShow,
                  json.testResults
                );
              }
              if (box) {
                box.value = ""; // submitted - dont show.
              }
              myUnDefer();
            },
            function (err) {
              myUnDefer();
              cldrRetry.handleDisconnect(err, json);
            }
          ); // end refresh-loaded-fcn
          // end: async
        } else {
          // Did not submit. Show errors, etc
          if (
            (json.statusAction && json.statusAction != "ALLOW") ||
            (json.testResults && (json.testWarnings || json.testErrors))
          ) {
            showProposedItem(
              tr.inputTd,
              tr,
              theRow,
              valToShow,
              json.testResults,
              json
            );
          } // else no errors, not submitted.  Nothing to do.
          if (box) {
            box.value = ""; // submitted - dont show.
          }
          button.className = "ichoice-o";
          button.checked = false;
          hideLoader();
          myUnDefer();
        }
      }
    } catch (e) {
      tr.className = "tr_err";
      tr.innerHTML =
        cldrStatus.stopIcon() +
        " Could not check value. Try reloading the page.<br>" +
        e.message;
      console.log("Error in ajax post [handleWiredClick] ", e.message);
      myUnDefer();
      cldrRetry.handleDisconnect("handleWiredClick:" + e.message, json);
    }
  };
  var errorHandler = function (err) {
    /*
     * Restore tr.className, so it stops being 'tr_checking1' immediately on receiving
     * any response. It may change again below, such as to 'tr_err'.
     */
    tr.className = originalTrClassName;
    console.log("Error: " + err);
    cldrRetry.handleDisconnect("Error: " + err, null);
    theRow.className = "ferrbox";
    theRow.innerHTML =
      "Error while  loading: <div style='border: 1px solid red;'>" +
      err +
      "</div>";
    myUnDefer();
  };
  if (box) {
    ourContent.value = value;
  }
  var xhrArgs = {
    url: ourUrl,
    handleAs: "json",
    content: ourContent,
    load: loadHandler,
    error: errorHandler,
    timeout: cldrAjax.mediumTimeout(),
  };
  cldrAjax.queueXhr(xhrArgs);
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
 * Update the counter on top of the vetting page
 */
function refreshCounterVetting() {
  if (cldrStatus.isVisitor() || cldrStatus.isDashboard()) {
    // if the user is a visitor, or this is the Dashboard, don't display the counter information
    $("#nav-page .counter-infos, #nav-page .nav-progress").hide();
    return;
  }

  var inputs = $(".vetting-page input:visible:checked");
  var total = inputs.length;
  var abstain = inputs.filter(function () {
    return this.id.substr(0, 2) === "NO";
  }).length;
  var voted = total - abstain;

  document.getElementById("count-total").innerHTML = total;
  document.getElementById("count-abstain").innerHTML = abstain;
  document.getElementById("count-voted").innerHTML = voted;
  if (total === 0) {
    total = 1;
  }
  document.getElementById("progress-voted").style.width =
    (voted * 100) / total + "%";
  document.getElementById("progress-abstain").style.width =
    (abstain * 100) / total + "%";

  if (cldrForum && cldrStatus.getCurrentLocale()) {
    const surveyUser = cldrStatus.getSurveyUser();
    if (surveyUser && surveyUser.id) {
      const forumSummary = cldrForum.getForumSummaryHtml(
        cldrStatus.getCurrentLocale(),
        surveyUser.id,
        false
      );
      document.getElementById("vForum").innerHTML = forumSummary;
    }
  }
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
  var cov = getSurveyUserCov();
  if (!cov) {
    cov = getSurveyOrgCov();
  }

  // get the value
  var val = covValue(cov);
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

/**
 * Add to the radio button, a more button style
 *
 * @param button
 * @returns a newly created label element
 */
function wrapRadio(button) {
  var label = document.createElement("label");
  label.title = "Vote";
  label.className = "btn btn-default";
  label.appendChild(button);
  $(label).tooltip();
  return label;
}

/**
 * Show the vote summary part of the Fix panel
 *
 * @param cont
 *
 * This was in review.js; for Dashboard
 */
function showHelpFixPanel(cont) {
  $(".fix-parent .data-vote").html("");
  $(".fix-parent .data-vote").append(cont);

  $(".data-vote > .span, .data-vote > .pClassExplain").remove();
  $(".data-vote > .span, .data-vote > .d-example").remove();

  var helpBox = $(".data-vote > *:not(.voteDiv)").add(".data-vote hr");
  $(".data-vote table:last").after(helpBox);

  if ($(".trInfo").length != 0) {
    $(".voteDiv").prepend("<hr/>");
    $(".voteDiv").prepend($(".trInfo").parent());
  }

  // move the element
  labelizeIcon();
}

export {
  INHERITANCE_MARKER,
  addIcon,
  addVitem,
  appendExample,
  appendExtraAttributes,
  appendIcon,
  appendItem,
  cacheBuster,
  cacheKill,
  chgPage,
  cloneAnon,
  cloneLocalizeAnon,
  covName,
  covValue,
  createGravatar,
  effectiveCoverage,
  findItemByValue,
  getDidUnbust,
  getSurveyLevels,
  getSurveyOrgCov,
  getSurveyUserCov,
  getTagChildren,
  getXpathMap,
  handleWiredClick,
  hideLoader,
  isInputBusy,
  localizeFlyover,
  parseStatusAction,
  refreshCounterVetting,
  setLang,
  setOverrideDir,
  setShower,
  setSurveyLevels,
  setSurveyUserCov,
  showHelpFixPanel,
  showLoader,
  testsToHtml,
  unbust,
  updateCovFromJson,
  updateCoverage,
  updateProgressWord,
  updateSpecialHeader,
  updateStatus,
  wireUpButton,
  wrapRadio,
};

/*
 * cldrLoad: encapsulate functions for loading GUI content for Survey Tool
 */
import * as cldrAccount from "./cldrAccount.mjs";
import * as cldrAdmin from "./cldrAdmin.mjs";
import * as cldrAjax from "./cldrAjax.mjs";
import * as cldrBulkClosePosts from "./cldrBulkClosePosts.mjs";
import * as cldrCoverage from "./cldrCoverage.mjs";
import * as cldrCreateLogin from "./cldrCreateLogin.mjs";
import * as cldrDashContext from "./cldrDashContext.mjs";
import * as cldrDom from "./cldrDom.mjs";
import * as cldrErrorSubtypes from "./cldrErrorSubtypes.mjs";
import * as cldrEvent from "./cldrEvent.mjs";
import * as cldrFlagged from "./cldrFlagged.mjs";
import { Flipper } from "./cldrFlip.mjs";
import * as cldrForum from "./cldrForum.mjs";
import * as cldrForumParticipation from "./cldrForumParticipation.mjs";
import * as cldrGenericVue from "./cldrGenericVue.mjs";
import * as cldrGui from "./cldrGui.mjs";
import * as cldrInfo from "./cldrInfo.mjs";
import * as cldrListEmails from "./cldrListEmails.mjs";
import * as cldrListUsers from "./cldrListUsers.mjs";
import { LocaleMap } from "./cldrLocaleMap.mjs";
import * as cldrLocales from "./cldrLocales.mjs";
import * as cldrMail from "./cldrMail.mjs";
import * as cldrMenu from "./cldrMenu.mjs";
import * as cldrNotify from "./cldrNotify.mjs";
import * as cldrOldVotes from "./cldrOldVotes.mjs";
import * as cldrRecentActivity from "./cldrRecentActivity.mjs";
import * as cldrReport from "./cldrReport.mjs";
import * as cldrRetry from "./cldrRetry.mjs";
import * as cldrStatus from "./cldrStatus.mjs";
import * as cldrSurvey from "./cldrSurvey.mjs";
import * as cldrTable from "./cldrTable.mjs";
import * as cldrText from "./cldrText.mjs";
import * as cldrVettingParticipation from "./cldrVettingParticipation.mjs";
import * as cldrVueMap from "./cldrVueMap.mjs";

import { h } from "vue";

const CLDR_LOAD_DEBUG = false;

let locmap = new LocaleMap(null); // a localemap that always returns the code
// locmap will be modified later with locmap = new LocaleMap(json.locmap)

let isLoading = false;

/**
 * list of pages to use with the flipper
 */
const pages = {
  loading: "LoadingMessageSection",
  data: "DynamicDataSection",
  other: "OtherSection",
};

let flipper = null;

/**************************/

/**
 * Call this once in the page. It expects to find a node #DynamicDataSection
 */
function showV() {
  flipper = new Flipper([pages.loading, pages.data, pages.other]);
  cldrDom.updateIf(
    "title-dcontent-link",
    cldrText.get("defaultContent_titleLink")
  );
}

function continueInitializing(canAutoImport) {
  window.addEventListener("hashchange", doHashChange);
  cldrEvent.hideOverlayAndSidebar();
  if (canAutoImport) {
    window.location.href = "#auto_import";
  } else {
    reloadV();
  }
}

function doHashChange(event) {
  const changedHash = getHash(); // window.location.hash minus "#"
  if (
    CLDR_LOAD_DEBUG &&
    sliceHash(new URL(event.newURL).hash) !== changedHash
  ) {
    // This can happen when rapidly clicking on one row after another.
    // In such cases, window.location.hash is more recent (per testing), so use it.
    console.log(
      "Mismatch in doHashChange: (event:) " +
        new URL(event.newURL).hash +
        " !== (window:) " +
        changedHash
    );
  }
  const oldLocale = trimNull(cldrStatus.getCurrentLocale());
  const oldSpecial = trimNull(cldrStatus.getCurrentSpecial());
  const oldPage = trimNull(cldrStatus.getCurrentPage());
  const oldId = trimNull(cldrStatus.getCurrentId());

  parseHashAndUpdate(changedHash);

  cldrStatus.setCurrentId(trimNull(cldrStatus.getCurrentId()));

  // did anything change?

  const changedLocale = oldLocale != trimNull(cldrStatus.getCurrentLocale());
  const curSpecial = trimNull(cldrStatus.getCurrentSpecial());
  const changedSpecial = oldSpecial != curSpecial;
  const changedPage = oldPage != trimNull(cldrStatus.getCurrentPage());
  if (changedLocale || (changedSpecial && curSpecial)) {
    cldrDashContext.hide();
  }
  if (changedLocale || changedSpecial || changedPage) {
    console.log("# hash changed, (loc, etc) reloadingV..");
    reloadV();
  } else if (
    oldId != cldrStatus.getCurrentId() &&
    cldrStatus.getCurrentId() != ""
  ) {
    console.log("# just ID changed, to " + cldrStatus.getCurrentId());
    // surveyCurrentID and the hash have already changed.
    // Make sure the item is visible.
    showCurrentId();
  }
}

/**
 * Parse the hash string into setCurrent___ variables.
 * Expected to update document.title also.
 *
 * @param {String} hash
 */
function parseHashAndUpdate(hash) {
  if (hash) {
    const pieces = hash.split("/");
    // pieces[1] is ALWAYS assumed to be locale or empty
    if (pieces.length > 1) {
      cldrStatus.setCurrentLocale(pieces[1]); // could be null
    } else {
      cldrStatus.setCurrentLocale("");
    }
    const curLocale = cldrStatus.getCurrentLocale();
    if (pieces[0].length == 0 && curLocale) {
      localeParseHash(pieces);
    } else {
      const curSpec = pieces[0] ? pieces[0] : "locales";
      cldrStatus.setCurrentSpecial(curSpec);
      const special = getSpecial(curSpec);
      if (special && special.parseHash && special.parseHash(pieces)) {
        // current page and id have been set by special.parseHash
      } else {
        unspecialParseHash();
      }
    }
  } else {
    cldrStatus.setCurrentLocale("");
    cldrStatus.setCurrentSpecial("locales");
    cldrStatus.setCurrentId("");
    cldrStatus.setCurrentPage("");
    cldrStatus.setCurrentSection("");
  }
  updateWindowTitle();

  // if there is no locale id, refresh the search.
  if (!cldrStatus.getCurrentLocale()) {
    cldrEvent.searchRefresh();
  }
}

function localeParseHash(pieces) {
  if (pieces.length > 2) {
    cldrStatus.setCurrentPage(pieces[2]);
    if (pieces.length > 3) {
      let id = pieces[3];
      if (id.substr(0, 2) == "x@") {
        id = id.substr(2);
      }
      cldrStatus.setCurrentId(id);
    } else {
      cldrStatus.setCurrentId("");
    }
  } else {
    cldrStatus.setCurrentPage("");
    cldrStatus.setCurrentId("");
  }
  cldrStatus.setCurrentSpecial(null);
}

function unspecialParseHash() {
  cldrStatus.setCurrentPage("");
  cldrStatus.setCurrentId("");
}

function updateWindowTitle() {
  let t = cldrText.get("survey_title");
  const curLocale = cldrStatus.getCurrentLocale();
  if (curLocale && curLocale != "") {
    t = t + ": " + locmap.getLocaleName(curLocale);
  }
  const curSpecial = cldrStatus.getCurrentSpecial();
  if (curSpecial) {
    t = t + ": " + cldrText.get("special_" + curSpecial);
  }
  const curPage = cldrStatus.getCurrentPage();
  if (curPage && curPage != "") {
    t = t + ": " + curPage;
  }
  document.title = t;
}

function updateCurrentId(id) {
  if (!id) {
    id = "";
  }
  if (cldrStatus.getCurrentId() != id) {
    // don't set if already set.
    cldrStatus.setCurrentId(id);
    replaceHash();
  }
}

/**
 * Update hash (and title)
 *
 * Called by cldrForum.parseContent, as well as locally.
 */
function replaceHash() {
  let theId = cldrStatus.getCurrentId();
  if (theId == null) {
    theId = "";
  }
  let theSpecial = cldrStatus.getCurrentSpecial();
  if (theSpecial == null) {
    theSpecial = "";
  }
  let thePage = cldrStatus.getCurrentPage();
  if (thePage == null) {
    thePage = "";
  }
  let theLocale = cldrStatus.getCurrentLocale();
  if (theLocale == null) {
    theLocale = "";
  }
  let newHash = theSpecial + "/" + theLocale + "/" + thePage + "/" + theId;
  if (newHash != getHash()) {
    setHash(newHash);
  }
}

/**
 * Verify that the JSON returned is as expected.
 *
 * @param json the returned json
 * @param subkey the key to look for,  json.subkey
 * @return true if OK, false if bad
 */
function verifyJson(json, subkey) {
  if (!json) {
    console.log("!json");
    cldrSurvey.showLoader(
      "Error while  loading " +
        subkey +
        ":  <br><div style='border: 1px solid red;'>" +
        "no data!" +
        "</div>"
    );
    return false;
  } else if (json.err_code) {
    var msg_fmt = cldrRetry.format(json, subkey);
    var loadingChunk = cldrDom.createChunk("", "p", "errCodeMsg");
    loadingChunk.appendChild(
      cldrDom.createChunk("", "i", "glyphicon glyphicon-remove-sign fgred")
    );
    loadingChunk.appendChild(cldrDom.createChunk(msg_fmt));
    flipper.flipTo(pages.loading, loadingChunk);
    loadingChunk.appendChild(cldrDom.createChunk("", "br"));
    var retryButton = cldrDom.createChunk(
      cldrText.get("loading_reload"),
      "button"
    );
    loadingChunk.appendChild(retryButton);
    retryButton.onclick = function () {
      window.location.reload(true);
    };
    var homeButton = cldrDom.createChunk(
      cldrText.get("loading_home"),
      "button"
    );
    loadingChunk.appendChild(homeButton);
    homeButton.onclick = function () {
      window.location.href = cldrStatus.getContextPath();
    };
    return false;
  } else if (json.err) {
    console.log("json.err!" + json.err);
    cldrSurvey.showLoader(
      "Error while  loading " +
        subkey +
        ": <br><div style='border: 1px solid red;'>" +
        json.err +
        "</div>"
    );
    cldrRetry.handleDisconnect("while loading " + subkey + "", json);
    return false;
  } else if (!json[subkey]) {
    console.log("!json." + subkey);
    cldrSurvey.showLoader(
      "Error while loading " +
        subkey +
        ": <br><div style='border: 1px solid red;'>" +
        "no data" +
        "</div>"
    );
    cldrRetry.handleDisconnect("while loading- no " + subkey + "", json);
    return false;
  } else {
    return true;
  }
}

function showCurrentId() {
  const curSpecial = cldrStatus.getCurrentSpecial();
  if (curSpecial) {
    const special = getSpecial(curSpecial);
    if (special && special.handleIdChanged) {
      special.handleIdChanged(curSpecial, showCurrentId);
    }
  } else {
    unspecialHandleIdChanged();
  }
}

function unspecialHandleIdChanged() {
  const curId = cldrStatus.getCurrentId();
  if (curId) {
    if (cldrTable.isHeaderId(curId)) {
      cldrTable.goToHeaderId(curId);
    } else {
      goToRowId(curId);
    }
  }
}

function goToRowId(curId) {
  const rowId = cldrTable.makeRowId(curId);
  const xtr = document.getElementById(rowId);
  if (!xtr) {
    if (CLDR_LOAD_DEBUG) {
      console.log(
        "Warning: could not load rowId = " + rowId + "; curId = " + curId
      );
    }
    updateCurrentId(null);
  } else {
    if (CLDR_LOAD_DEBUG && (!xtr.proposedcell || xtr.proposedcell.showFn)) {
      // warn, but show it anyway
      console.log(
        "Warning: now proposed cell && showFn " +
          curId +
          " - not setup - " +
          xtr.toString() +
          " pc=" +
          xtr.proposedcell +
          " sf = " +
          xtr.proposedcell.showFn
      );
    }
    cldrInfo.showRowObjFunc(xtr, xtr.proposedcell, xtr.proposedcell.showFn);
    if (CLDR_LOAD_DEBUG) {
      console.log("Changed to " + cldrStatus.getCurrentId());
    }
    xtr.scrollIntoView({ block: "nearest" });
  }
}

function insertLocaleSpecialNote(theDiv) {
  const bund = locmap.getLocaleInfo(cldrStatus.getCurrentLocale());
  let msg = localeSpecialNote(bund, false);
  if (msg) {
    msg = locmap.linkify(msg);
    const theChunk = cldrDom.construct(msg);
    const subDiv = document.createElement("div");
    subDiv.appendChild(theChunk);
    subDiv.className = "warnText";
    theDiv.insertBefore(subDiv, theDiv.childNodes[0]);
  }
  if (cldrStatus.getIsPhaseBeta()) {
    const html = cldrText.sub("beta_msg", {
      info: bund,
      locale: cldrStatus.getCurrentLocale(),
      msg: msg,
    });
    const theChunk = cldrDom.construct(html);
    const subDiv = document.createElement("div");
    subDiv.appendChild(theChunk);
    subDiv.className = "warnText";
    theDiv.insertBefore(subDiv, theDiv.childNodes[0]);
  }
}

/**
 *
 * @param {Object} bund the LocaleInfo bundle
 * @param {Boolean} brief if true, keep it short
 * @param {Boolean} plain if true, strip to plain text
 */
function localeSpecialNote(bund, brief) {
  if (!bund) return null;
  let msg = null;
  if (bund.dcParent) {
    if (brief) {
      msg = cldrText.sub("defaultContent_brief_msg", {
        name: bund.name,
        dcParent: bund.dcParent,
        locale: bund.bcp47,
        dcParentName: locmap.getLocaleName(bund.dcParent),
      });
    } else {
      msg = cldrText.sub("defaultContent_msg", {
        name: bund.name,
        dcParent: bund.dcParent,
        locale: bund.bcp47,
        dcParentName: locmap.getLocaleName(bund.dcParent),
      });
    }
  } else if (bund.readonly || bund.special_comment_raw) {
    if (bund.readonly) {
      if (bund.special_comment_raw) {
        msg = bund.special_comment_raw;
      } else if (bund.readonly_in_limited) {
        if (brief) {
          msg = cldrText.sub("readonly_in_limited_brief", {
            info: bund,
            locale: bund.bcp47,
          });
        } else {
          msg = cldrText.sub("readonly_in_limited", {
            info: bund,
            locale: bund.bcp47,
          });
        }
      } else {
        msg = cldrText.sub("readonly_unknown", {
          info: bund,
          locale: bund.bcp47,
        });
      }
      if (!brief) {
        msg = cldrText.sub("readonly_msg", {
          info: bund,
          locale: bund.bcp47,
          msg: msg,
        });
      }
    } else {
      // Not readonly, could be a scratch locale
      msg = bund.special_comment_raw;
    }
  } else if (!brief && bund.dcChild) {
    msg = cldrText.sub("defaultContentChild_msg", {
      name: bund.name,
      dcChild: bund.dcChild,
      locale: bund.bcp47,
      dcChildName: locmap.getLocaleName(bund.dcChild),
    });
  }
  return msg;
}

function reloadV() {
  if (cldrStatus.isDisconnected()) {
    cldrSurvey.unbust();
  }
  document.getElementById("DynamicDataSection").innerHTML = "";
  $("#nav-page").hide();
  $("#nav-page-footer").hide();
  isLoading = false;

  /*
   * Scroll back to top when loading a new page, to avoid a bug where, for
   * example, having scrolled towards bottom, we switch from a Section page
   * to the Forum page and the scrollbar stays where it was, making the new
   * content effectively invisible.
   */
  window.scrollTo(0, 0);

  const id = flipper.get(pages.data).id;
  cldrSurvey.setShower(id, ignoreReloadRequest);

  // assume parseHash was already called, if we are taking input from the hash
  updateHashAndMenus();

  const curLocale = cldrStatus.getCurrentLocale();
  if (curLocale != null && curLocale != "" && curLocale != "-") {
    var bund = locmap.getLocaleInfo(curLocale);
    if (bund !== null && bund.dcParent) {
      const html = cldrText.sub("defaultContent_msg", {
        name: bund.name,
        dcParent: bund.dcParent,
        locale: curLocale,
        dcParentName: locmap.getLocaleName(bund.dcParent),
      });
      var theChunk = cldrDom.construct(html);
      var theDiv = document.createElement("div");
      theDiv.appendChild(theChunk);
      theDiv.className = "ferrbox";
      flipper.flipTo(pages.other, theDiv);
      return;
    }
  }

  flipper.flipTo(
    pages.loading,
    cldrDom.createChunk(cldrText.get("loading"), "i", "loadingMsg")
  );

  // Create a little spinner to spin "..." so the user knows we are doing something..
  var spinChunk = cldrDom.createChunk("...", "i", "loadingMsgSpin");
  var spin = 0;
  var timerToKill = window.setInterval(function () {
    var spinTxt = "";
    spin++;
    switch (spin % 3) {
      case 0:
        spinTxt = ".  ";
        break;
      case 1:
        spinTxt = " . ";
        break;
      case 2:
        spinTxt = "  .";
        break;
    }
    cldrDom.removeAllChildNodes(spinChunk);
    spinChunk.appendChild(document.createTextNode(spinTxt));
  }, 1000);

  // Add the "..." until the Flipper flips
  flipper.addUntilFlipped(
    function () {
      var frag = document.createDocumentFragment();
      frag.appendChild(spinChunk);
      return frag;
    },
    function () {
      window.clearInterval(timerToKill);
    }
  );

  const itemLoadInfo = cldrDom.createChunk("", "div", "itemLoadInfo");
  itemLoadInfo.setAttribute("id", "itemLoadInfo");

  shower(itemLoadInfo); // first load

  // set up the "show-er" function so that if this locale gets reloaded,
  // the page will load again
  const id2 = flipper.get(pages.data).id;
  cldrSurvey.setShower(id2, function () {
    shower(itemLoadInfo);
  });
} // end reloadV

/**
 * The coverage level changed. Pass this off to the Vue component(s) or the Special page(s).
 * @param {String} newLevel
 */
function handleCoverageChanged(newLevel) {
  const currentSpecial = cldrStatus.getCurrentSpecial();
  if (currentSpecial && isReport(currentSpecial)) {
    // Cause the page to reload.
    reloadV();
  }
  cldrGui.updateWidgetsWithCoverage(newLevel);
}

function ignoreReloadRequest() {
  console.log(
    "reloadV()'s shower - ignoring reload request, we are in the middle of a load!"
  );
}

function shower(itemLoadInfo) {
  if (isLoading) {
    console.log("reloadV inner shower: already isLoading, exiting.");
    return;
  }
  isLoading = true;
  let theDiv = flipper.get(pages.data);
  let theTable = theDiv.theTable;
  if (!theTable) {
    const theTableList = theDiv.getElementsByTagName("table");
    if (theTableList) {
      theTable = theTableList[0];
      theDiv.theTable = theTable;
    }
  }
  cldrSurvey.showLoader(cldrText.get("loading"));
  const curSpecial = cldrStatus.getCurrentSpecial();
  cldrGui.setToptitleVisibility(curSpecial !== "menu");
  try {
    specialLoad(itemLoadInfo, curSpecial, theDiv);
  } catch (e) {
    cldrNotify.exception(e, `Showing SurveyTool page ${curSpecial || ""}`);
  }
}

function specialLoad(itemLoadInfo, curSpecial, theDiv) {
  const special = getSpecial(curSpecial); // special is an object; curSpecial is a string
  if (special && special.load) {
    cldrEvent.hideOverlayAndSidebar();
    if (curSpecial !== "general") {
      cldrDashContext.hide();
    }
    cldrInfo.closePanel(false /* userWantsHidden */);
    // Most special.load() functions do not use a parameter; an exception is
    // cldrGenericVue.load() which expects the special name as a parameter
    if (CLDR_LOAD_DEBUG) {
      console.log(
        "cldrLoad.specialLoad: running special.load(" + curSpecial + ")"
      );
    }
    special.load(curSpecial);
  } else if (curSpecial !== "general") {
    // Avoid recursion.
    unspecialLoad(itemLoadInfo, theDiv);
  } else {
    // This will only be called if 'general' is a missing special.
    handleMissingSpecial(curSpecial);
  }
}

function unspecialLoad(itemLoadInfo, theDiv) {
  const curSpecial = cldrStatus.getCurrentSpecial();
  const curLocale = cldrStatus.getCurrentLocale();
  if (curLocale && !curSpecial) {
    const curPage = cldrStatus.getCurrentPage();
    const curId = cldrStatus.getCurrentId();
    if (!curPage && !curId) {
      if (CLDR_LOAD_DEBUG) {
        console.log("cldrLoad.unspecialLoad: running specialLoad(general)");
      }
      cldrStatus.setCurrentSpecial("general");
      specialLoad(itemLoadInfo, "general", theDiv);
    } else if (curId === "!") {
      // TODO: clarify when and why this would happen
      if (CLDR_LOAD_DEBUG) {
        console.log("cldrLoad.unspecialLoad: running loadExclamationPoint");
      }
      loadExclamationPoint();
    } else {
      if (!cldrSurvey.isInputBusy()) {
        /*
         * Make “all rows” requests only when !isInputBusy, to avoid wasted requests
         * if the user leaves the input box open for an extended time.
         */
        if (CLDR_LOAD_DEBUG) {
          console.log("cldrLoad.unspecialLoad: running loadAllRows");
        }
        loadAllRows(itemLoadInfo, theDiv);
      } else if (CLDR_LOAD_DEBUG) {
        console.log(
          "cldrLoad.unspecialLoad: skipping loadAllRows because input is busy"
        );
      }
    }
  } else if (curSpecial) {
    if (CLDR_LOAD_DEBUG) {
      console.log("cldrLoad.unspecialLoad: calling handleMissingSpecial");
    }
    handleMissingSpecial(curSpecial);
  }
}

/**
 *
 * @param {String} curSpecial missing special
 */
function handleMissingSpecial(curSpecial) {
  console.log("No special js found for " + curSpecial);
  cldrSurvey.hideLoader();
  isLoading = false;
  flipper.flipTo(pages.other, document.createTextNode("Page Not Found")); // stop loader
  const description = h("div", [
    h(
      "p",
      {},
      `The page “${curSpecial}” does not exist.
    The link could be out-of-date, or you may have found a bug.`
    ),
    h(
      "button",
      {
        onclick: function () {
          window.location.replace("v#");
          window.location.reload();
        },
      },
      `Return to the SurveyTool`
    ),
  ]);
  cldrNotify.error("Page not found", description);
}

/**
 * Given a string like "about", return a "special" object like cldrAccount.
 * These objects share in common that they may define methods:
 *  - load
 *  - handleIdChanged
 *  - parseHash
 *
 * called as special.load, etc.
 *
 * TODO: replace this mechanism with something object-oriented.
 * Currently there is "inheritance" only in the crude form of fallback functions:
 *  - unspecialLoad
 *  - unspecialHandleIdChanged
 *  - unspecialParseHash
 *
 * Also these, which don't fit the fallback pattern:
 *  - loadExclamationPoint
 *  - loadAllRows
 *  - localeParseHash
 *
 * @param {string} str
 * @return the special object, or null if no such object
 */
function getSpecial(str) {
  if (!str) {
    return null;
  }
  if (cldrVueMap.isVueSpecial(str)) {
    return cldrGenericVue; // see cldrVueMap.specialToComponent
  }
  if (isReport(str)) {
    return cldrReport; // handle these as one.
  }
  const specials = {
    // Other special pages.
    account: cldrAccount,
    admin: cldrAdmin,
    bulk_close_posts: cldrBulkClosePosts,
    createAndLogin: cldrCreateLogin,
    error_subtypes: cldrErrorSubtypes,
    flagged: cldrFlagged,
    forum: cldrForum,
    forum_participation: cldrForumParticipation,
    list_emails: cldrListEmails,
    list_users: cldrListUsers,
    locales: cldrLocales,
    mail: cldrMail,
    oldvotes: cldrOldVotes,
    recent_activity: cldrRecentActivity,
    retry: cldrRetry,
    vetting_participation: cldrVettingParticipation,
  };
  if (str in specials) {
    return specials[str];
  } else {
    return null;
  }
}

/**
 * Is the given "special" name for a report, that is, does it start with "r_"?
 * (No longer applicable to Dashboard)
 * Cf. SurveyMain.ReportMenu.PRIORITY_ITEMS
 *
 * @param str the string
 * @return true if starts with "r_", else false
 */
function isReport(str) {
  return str[0] == "r" && str[1] == "_";
}

function loadExclamationPoint() {
  var frag = document.createDocumentFragment();
  frag.appendChild(
    cldrDom.createChunk(cldrText.get("section_help"), "p", "helpContent")
  );
  const curPage = cldrStatus.getCurrentPage();
  const infoHtml = cldrText.get("section_info_" + curPage);
  const infoChunk = document.createElement("div");
  infoChunk.innerHTML = infoHtml;
  frag.appendChild(infoChunk);
  flipper.flipTo(pages.other, frag);
  cldrSurvey.hideLoader();
  isLoading = false;
}

function loadAllRows(itemLoadInfo, theDiv) {
  const curId = cldrStatus.getCurrentId();
  const curPage = cldrStatus.getCurrentPage();
  const curLocale = cldrStatus.getCurrentLocale();
  itemLoadInfo.appendChild(
    document.createTextNode(
      locmap.getLocaleName(curLocale) + "/" + curPage + "/" + curId
    )
  );
  const url = cldrTable.getPageUrl(curLocale, curPage, curId);
  $("#nav-page").show(); // make top "Prev/Next" buttons visible while loading, cf. '#nav-page-footer' below

  if (CLDR_LOAD_DEBUG) {
    console.log("cldrLoad.loadAllRows sending request");
  }
  cldrAjax
    .doFetch(url)
    .then((response) => response.json())
    .then((json) => loadAllRowsFromJson(json, theDiv))
    .catch((err) => {
      console.error(err);
      isLoading = false;
      cldrNotify.exception(err, "loading rows");
    });
}

function loadAllRowsFromJson(json, theDiv) {
  if (CLDR_LOAD_DEBUG) {
    console.log("cldrLoad.loadAllRowsFromJson got response");
  }
  isLoading = false;
  cldrSurvey.showLoader(cldrText.get("loading2"));
  if (json.err) {
    // Err is set
    cldrSurvey.hideLoader();
    const surveyCurrentId = cldrStatus.getCurrentId();
    const surveyCurrentPage = cldrStatus.getCurrentPage();
    const surveyCurrentLocale = cldrStatus.getCurrentLocale();
    cldrNotify.error(
      "Error loading data",
      `There was a problem loading data to display for ${surveyCurrentLocale}/${surveyCurrentPage}/${surveyCurrentId}`
    );
    cldrStatus.setCurrentSection("");
    let msg = "";
    if (json.code) {
      // 'json' is a serialized org.unicode.cldr.web.api.STError
      // json.code has an error code which can be rendered (E_BAD_SECTION etc).
      // Use that code to show a more specific error to the user
      // instead of just "failed to load".
      msg = cldrText.sub(json.code, {
        what: "Load rows",
        code: json.code,
        surveyCurrentId,
        surveyCurrentLocale,
        surveyCurrentPage,
      });
    } else {
      // We don't have further information.
      msg = "Could not load rows. Try reloading or a different section/URL.";
    }
    flipper.flipTo(pages.other, cldrDom.createChunk(msg, "p", "ferrbox"));
  } else if (!verifyJson(json, "page")) {
    return;
  } else if (json.page.nocontent) {
    cldrStatus.setCurrentSection("");
    if (json.pageId) {
      cldrStatus.setCurrentPage(json.pageId);
    } else {
      cldrStatus.setCurrentPage("");
    }
    if (CLDR_LOAD_DEBUG) {
      console.log("cldrLoad.loadAllRowsFromJson got json.page.nocontent");
    }
    cldrSurvey.showLoader(null);
    updateHashAndMenus(); // find out why there's no content. (locmap)
  } else if (!json.page.rows) {
    console.log("!json.page.rows");
    cldrSurvey.showLoader(
      "Error while loading: <br><div style='border: 1px solid red;'>" +
        "no rows" +
        "</div>"
    );
    cldrRetry.handleDisconnect("while loading- no rows", json);
  } else {
    cldrSurvey.showLoader("loading..");
    if (json.dataLoadTime) {
      cldrDom.updateIf("dynload", json.dataLoadTime);
    }
    if (json.loc) {
      cldrStatus.setCurrentLocale(json.loc); // may replace "USER"
    }
    cldrStatus.setCurrentSection("");
    cldrStatus.setCurrentPage(json.pageId);
    updateHashAndMenus(); // now that we have a pageid
    if (!cldrSurvey.isInputBusy()) {
      cldrSurvey.showLoader(cldrText.get("loading3"));
      if (CLDR_LOAD_DEBUG) {
        console.log("cldrLoad.loadAllRowsFromJson calling insertRows");
      }
      cldrTable.insertRows(
        theDiv,
        json.pageId,
        cldrStatus.getSessionId(),
        json
      ); // pageid is the xpath..
      cldrCoverage.updateCoverage(flipper.get(pages.data)); // make sure cov is set right before we show.
      flipper.flipTo(pages.data); // TODO now? or later?
      showCurrentId(); // already calls scroll
      cldrGui.refreshCounterVetting();
      $("#nav-page-footer").show(); // make bottom "Prev/Next" buttons visible after building table
      if (!cldrStatus.getCurrentId()) {
        cldrInfo.showMessage(getGuidanceMessage(json.canModify));
      }
    } else if (CLDR_LOAD_DEBUG) {
      console.log(
        "cldrLoad.loadAllRowsFromJson skipping insertRows because isInputBusy"
      );
    }
  }
}

function getGuidanceMessage(canModify) {
  if (!cldrStatus.getSurveyUser()) {
    return cldrText.get("loginGuidance");
  } else if (!canModify) {
    return cldrText.get("readonlyGuidance");
  } else {
    return cldrText.get("dataPageInitialGuidance");
  }
}

/**
 * Update the #hash and menus to the current settings.
 */
function updateHashAndMenus() {
  replaceHash();
  cldrMenu.update();
}

function trimNull(x) {
  if (x == null) {
    return "";
  }
  try {
    x = x.toString().trim();
  } catch (e) {
    // do nothing
  }
  return x;
}

/**
 * Common function for loading.
 * @deprecated use cldrAjax.doFetch() instead
 * @param postData optional - makes this a POST
 */
function myLoad(url, message, handler, postData, headers) {
  const otime = new Date().getTime();
  console.log("MyLoad: " + url + " for " + message);
  const errorHandler = function (err, request) {
    console.log("Error: " + err);
    cldrNotify.error(`Could not fetch ${message}`, `Error: ${err.toString()}`);
    handler(null);
  };
  const loadHandler = function (json) {
    console.log(
      "        " + url + " loaded in " + (new Date().getTime() - otime) + "ms"
    );
    try {
      handler(json);
    } catch (e) {
      console.log(
        "Error in ajax post [" + message + "]  " + e.message + " / " + e.name
      );
      cldrRetry.handleDisconnect(
        "Exception while loading: " +
          message +
          " - " +
          e.message +
          ", n=" +
          e.name +
          " \nStack:\n" +
          (e.stack || "[none]"),
        null
      ); // in case the 2nd line doesn't work
    }
  };
  const xhrArgs = {
    url: url,
    handleAs: "json",
    load: loadHandler,
    error: errorHandler,
    postData: postData,
    headers: headers,
  };
  cldrAjax.sendXhr(xhrArgs);
}

function appendLocaleLink(subLocDiv, subLoc, subInfo, fullTitle) {
  let name = locmap.getRegionAndOrVariantName(subLoc);
  if (fullTitle) {
    name = locmap.getLocaleName(subLoc);
  }
  if (subInfo.special_type === "scratch") {
    cldrDom.addClass(subLocDiv, "scratch_locale");
    name = `${cldrText.get("scratch_locale")}: ${name}`;
  }
  const clickyLink = cldrDom.createChunk(name, "a", "locName");
  clickyLink.href = linkToLocale(subLoc);
  subLocDiv.appendChild(clickyLink);
  if (subInfo == null) {
    console.log("* internal: subInfo is null for " + name + " / " + subLoc);
  }
  if (subInfo.name_var) {
    cldrDom.addClass(clickyLink, "name_var");
  }
  clickyLink.title = localeSpecialNote(subInfo, true) || subLoc;

  // We parse subInfo to add CSS classes
  if (subInfo.readonly) {
    cldrDom.addClass(clickyLink, "locked");
    if (subInfo.readonly_in_limited) {
      cldrDom.addClass(clickyLink, "readonly_in_limited");
      if (cldrMenu.canModifyLoc(subLoc)) {
        cldrDom.addClass(clickyLink, "shown_but_locked"); // Never hide these
      }
    } else {
      // Don't hide locales due to limited submission
      cldrDom.addClass(subLocDiv, "hide"); // This locale is hidden by default
      cldrDom.addClass(clickyLink, "hidelocked"); // This locale can be hidden with 'hide locked'
    }
  } else if (cldrMenu.canModifyLoc(subLoc)) {
    cldrDom.addClass(clickyLink, "canmodify"); //  Other locales in the user's allowable modify list
  } else {
    // Some other reason
    cldrDom.addClass(subLocDiv, "hide"); // not modifiable
  }
  return clickyLink;
}

function getTheLocaleMap() {
  return locmap;
}

/**
 * Get the direction of a locale, if available
 * @param {String} locale
 * @returns null or 'ltr' or 'rtl'
 */
function getLocaleDir(locale) {
  const locmap = getTheLocaleMap();
  let localeDir = null;
  if (locale) {
    const localeInfo = locmap.getLocaleInfo(locale);
    if (localeInfo) {
      localeDir = localeInfo.dir;
    }
  }
  return localeDir;
}

/** @returns true if locmap has been loaded from data */
function localeMapReady() {
  return !!locmap.locmap;
}

/** event ID for localeMap changes */
const LOCALEMAP_EVENT = "localeMapReady";

/**
 * Calls the callback when the localeMap is ready (with real data).
 * Calls right away if the localeMap was already loaded.
 */
function onLocaleMapReady(callback) {
  if (localeMapReady()) {
    callback();
  } else {
    cldrStatus.on(LOCALEMAP_EVENT, callback);
  }
}

function setTheLocaleMap(lm) {
  locmap = lm;
  cldrStatus.dispatchEvent(new Event(LOCALEMAP_EVENT));
}
/**
 * Convenience for calling getTheLocaleMap().getLocaleName(loc)
 * @param {String} loc
 * @returns Locale name, or the locale code if data isn’t loaded yet.
 */
function getLocaleName(loc) {
  return locmap.getLocaleName(loc);
}

function getLocaleInfo(loc) {
  return locmap.getLocaleInfo(loc);
}

/**
 * Get the window location hash
 *
 * For example, if the current URL is "https:...#bar", return "bar".
 *
 * Typically the first value we return is "locales///"
 *
 * References: https://developer.mozilla.org/en-US/docs/Web/API/URL/hash
 * https://developer.mozilla.org/en-US/docs/Web/API/Window/location
 */
function getHash() {
  const hash = sliceHash(window.location.hash);
  if (CLDR_LOAD_DEBUG) {
    console.log("getHash returning " + hash);
  }
  return hash;
}

/**
 * Set the window location hash
 */
function setHash(newHash) {
  newHash = sliceHash(newHash);
  if (newHash !== sliceHash(window.location.hash)) {
    const oldUrl = window.location.href;
    const newUrl = oldUrl.split("#")[0] + "#" + newHash;
    if (CLDR_LOAD_DEBUG) {
      console.log("setHash going to " + newUrl + " - Called with: " + newHash);
    }
    window.location.href = newUrl;
  }
}

// given "#foo" or "foo", return "foo"
function sliceHash(hash) {
  return hash.charAt(0) === "#" ? hash.slice(1) : hash;
}

function flipToOtherDiv(div) {
  flipper.flipTo(pages.other, div);
}

function flipToGenericNoLocale() {
  cldrSurvey.hideLoader();
  flipper.flipTo(
    pages.other,
    cldrDom.createChunk(cldrText.get("generic_nolocale"), "p", "helpContent")
  );
}

function flipToEmptyOther() {
  return flipper.flipToEmpty(pages.other);
}

function coverageUpdate() {
  cldrCoverage.updateCoverage(flipper.get(pages.data));
  handleCoverageChanged(cldrCoverage.effectiveName());
}

function setLoading(loading) {
  isLoading = loading;
}

function linkToLocale(subLoc) {
  return (
    "#/" +
    subLoc +
    "/" +
    cldrStatus.getCurrentPage() +
    "/" +
    cldrStatus.getCurrentId()
  );
}

export {
  appendLocaleLink,
  continueInitializing,
  coverageUpdate,
  flipToEmptyOther,
  flipToGenericNoLocale,
  flipToOtherDiv,
  getHash,
  getLocaleDir,
  getLocaleInfo,
  getLocaleName,
  getTheLocaleMap,
  handleCoverageChanged,
  insertLocaleSpecialNote,
  linkToLocale,
  localeSpecialNote,
  myLoad,
  onLocaleMapReady,
  parseHashAndUpdate,
  reloadV,
  replaceHash,
  setLoading,
  setTheLocaleMap,
  showCurrentId,
  showV,
  updateCurrentId,
  updateHashAndMenus,
  verifyJson,
};

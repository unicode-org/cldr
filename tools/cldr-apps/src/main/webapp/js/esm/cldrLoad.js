/*
 * cldrLoad: encapsulate functions for loading GUI content for Survey Tool
 */
import * as cldrAccount from "./cldrAccount.js";
import * as cldrAdmin from "./cldrAdmin.js";
import * as cldrAjax from "./cldrAjax.js";
import * as cldrBulkClosePosts from "./cldrBulkClosePosts.js";
import * as cldrCreateLogin from "./cldrCreateLogin.js";
import * as cldrDom from "./cldrDom.js";
import * as cldrErrorSubtypes from "./cldrErrorSubtypes.js";
import * as cldrEvent from "./cldrEvent.js";
import * as cldrFlagged from "./cldrFlagged.js";
import { Flipper } from "./cldrFlip.js";
import * as cldrForum from "./cldrForum.js";
import * as cldrForumParticipation from "./cldrForumParticipation.js";
import * as cldrGenericVue from "./cldrGenericVue.js";
import * as cldrInfo from "./cldrInfo.js";
import * as cldrListEmails from "./cldrListEmails.js";
import * as cldrListUsers from "./cldrListUsers.js";
import { LocaleMap } from "./cldrLocaleMap.js";
import * as cldrLocales from "./cldrLocales.js";
import * as cldrMail from "./cldrMail.js";
import * as cldrMenu from "./cldrMenu.js";
import * as cldrOldVotes from "./cldrOldVotes.js";
import * as cldrRecentActivity from "./cldrRecentActivity.js";
import * as cldrReportDates from "./cldrReportDates.js";
import * as cldrReportNumbers from "./cldrReportNumbers.js";
import * as cldrReportZones from "./cldrReportZones.js";
import * as cldrRetry from "./cldrRetry.js";
import * as cldrStatus from "./cldrStatus.js";
import * as cldrSurvey from "./cldrSurvey.js";
import * as cldrTable from "./cldrTable.js";
import * as cldrText from "./cldrText.js";
import * as cldrVettingParticipation from "./cldrVettingParticipation.js";

const CLDR_LOAD_DEBUG = false;

/**
 * haveDialog: when true, it means a "dialog" of some kind is displayed.
 * Used for inhibiting $('#left-sidebar').hover in redesign.js.
 * Currently there are only two such dialogs, both for auto-import.
 */
let haveDialog = false;

let locmap = null;

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
  locmap = new LocaleMap(null); // TODO: is it really a singleton?
  // locmap will be modified later with locmap = new LocaleMap(json.locmap)

  flipper = new Flipper([pages.loading, pages.data, pages.other]);

  const pucontent = document.getElementById("itemInfo");
  const theDiv = flipper.get(pages.data);
  theDiv.pucontent = pucontent;
  pucontent.appendChild(
    cldrDom.createChunk(cldrText.get("itemInfoBlank"), "i")
  );
  cldrDom.updateIf(
    "title-dcontent-link",
    cldrText.get("defaultContent_titleLink")
  );
  /*
   * Arrange for getInitialMenusEtc to be called soon after we've gotten the session id.
   * Add a short timeout to avoid interrupting the code that sets the session id.
   */
  cldrStatus.setSessionIdChangeCallback(function (sessionId) {
    setTimeout(function () {
      parseHashAndUpdate(getHash());
      cldrMenu.getInitialMenusEtc(sessionId);
    }, 100 /* one tenth of a second */);
  });
}

function continueInitializing(canAutoImport) {
  if (canAutoImport) {
    doAutoImport();
  }
  reloadV();

  window.addEventListener("hashchange", doHashChange);
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
  if (
    oldLocale != trimNull(cldrStatus.getCurrentLocale()) ||
    oldSpecial != trimNull(cldrStatus.getCurrentSpecial()) ||
    oldPage != trimNull(cldrStatus.getCurrentPage())
  ) {
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
    const pieces = hash.substr(0).split("/");
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
    replaceHash(false); // usually don't want to save
  }
}

/**
 * Update hash (and title)
 *
 * @param doPush {Boolean} if true, do a push (instead of replace)
 *
 * Called by cldrForum.parseContent (doPush false), as well as locally.
 */
function replaceHash(doPush) {
  if (!doPush) {
    doPush = false; // by default -replace.
  }
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
    setHash(newHash, !doPush);
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
    var loadingChunk = cldrDom.createChunk(msg_fmt, "p", "errCodeMsg");
    flipper.flipTo(pages.loading, loadingChunk);
    var retryButton = cldrDom.createChunk(
      cldrText.get("loading_reload"),
      "button"
    );
    loadingChunk.appendChild(retryButton);
    retryButton.onclick = function () {
      window.location.reload(true);
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
  if (curSpecial && curSpecial != "" && !cldrStatus.isDashboard()) {
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
    var xtr = document.getElementById("r@" + curId);
    if (!xtr) {
      console.log("Warning could not load id " + curId + " does not exist");
      updateCurrentId(null);
    } else if (xtr.proposedcell && xtr.proposedcell.showFn) {
      // TODO: visible? coverage?
      cldrInfo.showRowObjFunc(xtr, xtr.proposedcell, xtr.proposedcell.showFn);
      console.log("Changed to " + cldrStatus.getCurrentId());
      if (!cldrStatus.isDashboard()) {
        scrollToItem();
      }
    } else {
      console.log(
        "Warning could not load id " +
          curId +
          " - not setup - " +
          xtr.toString() +
          " pc=" +
          xtr.proposedcell +
          " sf = " +
          xtr.proposedcell.showFn
      );
    }
  }
}

/**
 * Show the surveyCurrentId row
 */
function scrollToItem() {
  const curId = cldrStatus.getCurrentId();
  if (curId) {
    const xtr = document.getElementById("r@" + curId);
    if (xtr) {
      console.log("Scrolling to " + curId);
      xtr.scrollIntoView({ block: "nearest" });
    }
  }
}

function insertLocaleSpecialNote(theDiv) {
  const bund = locmap.getLocaleInfo(cldrStatus.getCurrentLocale());
  let msg = null;

  if (bund) {
    if (bund.readonly || bund.special_comment_raw) {
      if (bund.readonly) {
        if (bund.special_comment_raw) {
          msg = bund.special_comment_raw;
        } else {
          msg = cldrText.get("readonly_unknown");
        }
        msg = cldrText.sub("readonly_msg", {
          info: bund,
          locale: cldrStatus.getCurrentLocale(),
          msg: msg,
        });
      } else {
        // Not readonly, could be a scratch locale
        msg = bund.special_comment_raw;
      }
      if (msg) {
        msg = locmap.linkify(msg);
        var theChunk = cldrDom.construct(msg);
        var subDiv = document.createElement("div");
        subDiv.appendChild(theChunk);
        subDiv.className = "warnText";
        theDiv.insertBefore(subDiv, theDiv.childNodes[0]);
      }
    } else if (bund.dcChild) {
      const html = cldrText.sub("defaultContentChild_msg", {
        name: bund.name,
        dcChild: bund.dcChild,
        locale: cldrStatus.getCurrentLocale(),
        dcChildName: locmap.getLocaleName(bund.dcChild),
      });
      var theChunk = cldrDom.construct(html);
      var subDiv = document.createElement("div");
      subDiv.appendChild(theChunk);
      subDiv.className = "warnText";
      theDiv.insertBefore(subDiv, theDiv.childNodes[0]);
    }
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
  updateHashAndMenus(true);

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
  // the page will load again - except for the dashboard, where only the
  // row get updated
  if (!cldrStatus.isDashboard()) {
    const id2 = flipper.get(pages.data).id;
    cldrSurvey.setShower(id2, function () {
      shower(itemLoadInfo);
    });
  }
} // end reloadV

/**
 * The coverage level changed. Pass this off to the Vue component or the Special page.
 * @param {String} newLevel
 * @returns true if the change was handled.
 */
function handleCoverageChanged(newLevel) {
  const currentSpecial = cldrStatus.getCurrentSpecial();
  if (currentSpecial) {
    const special = getSpecial(currentSpecial);
    if (
      special.handleCoverageChanged &&
      special.handleCoverageChanged(newLevel)
    ) {
      return true;
    }
    if (isReport(currentSpecial)) {
      // Cause the page to reload.
      reloadV();
      return true;
    }
  }
  return false;
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
  if (curSpecial === "none") {
    // TODO: clarify when and why this would happen
    cldrSurvey.hideLoader();
    isLoading = false;
    window.location = cldrStatus.getSurvUrl(); // redirect home
  } else {
    const special = getSpecial(curSpecial);
    if (special && special.load) {
      special.load(curSpecial); // pass the special name to the loader
    } else {
      unspecialLoad(itemLoadInfo, theDiv);
    }
  }
}

function unspecialLoad(itemLoadInfo, theDiv) {
  const curSpecial = cldrStatus.getCurrentSpecial();
  const curLocale = cldrStatus.getCurrentLocale();
  if (curLocale && !curSpecial) {
    const curPage = cldrStatus.getCurrentPage();
    const curId = cldrStatus.getCurrentId();
    if (!curPage && !curId) {
      loadGeneral(itemLoadInfo);
    } else if (curId === "!") {
      // TODO: clarify when and why this would happen
      loadExclamationPoint();
    } else if (!cldrSurvey.isInputBusy()) {
      /*
       * Make “all rows” requests only when !isInputBusy, to avoid wasted requests
       * if the user leaves the input box open for an extended time.
       */
      loadAllRows(itemLoadInfo, theDiv);
    }
  } else if (curSpecial) {
    console.log("No special js found for " + curSpecial);
  }
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
 *  - loadGeneral
 *  - loadAllRows
 *  - localeParseHash
 *
 * @param {string} str
 * @return the special object, or null if no such object
 */
function getSpecial(str) {
  const specials = {
    // These are handled by Vue. When adding a new mapping to cldrGenericVue here, it may also
    // be necessary to map the special to a specific .vue file in specialToComponentMap.js
    about: cldrGenericVue,
    add_user: cldrGenericVue,
    lookup: cldrGenericVue,
    vsummary: cldrGenericVue,
    r_vetting_json: cldrGenericVue,
    default: cldrGenericVue, // Add this here for testing the '/v#default' page.
    test_panel: cldrGenericVue, // Test page

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
    r_compact: cldrReportNumbers,
    r_datetime: cldrReportDates,
    r_zones: cldrReportZones,
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
 * Is the given string for a report, that is, does it start with "r_"?
 * Really only (?) 4: "r_vetting_json" (Dashboard), "r_datetime", "r_zones", "r_compact"
 * Cf. SurveyMain.ReportMenu.PRIORITY_ITEMS
 *
 * @param str the string
 * @return true if starts with "r_", else false
 */
function isReport(str) {
  return str[0] == "r" && str[1] == "_";
}

// the 'General Info' page.
function loadGeneral(itemLoadInfo) {
  const curLocale = cldrStatus.getCurrentLocale();
  const curLocaleName = locmap.getLocaleName(curLocale);
  itemLoadInfo.appendChild(document.createTextNode(curLocaleName));
  showPossibleProblems();
  const message = cldrText.get("generalPageInitialGuidance");
  cldrInfo.showMessage(message);
  isLoading = false;
}

/**
 * Show the "possible problems" section which has errors for the locale
 */
function showPossibleProblems() {
  const effectiveCov = cldrSurvey.covName(cldrSurvey.effectiveCoverage());
  const requiredCov = effectiveCov;
  const url =
    cldrStatus.getContextPath() +
    "/SurveyAjax?what=possibleProblems&_=" +
    cldrStatus.getCurrentLocale() +
    "&s=" +
    cldrStatus.getSessionId() +
    "&eff=" +
    effectiveCov +
    "&req=" +
    requiredCov +
    cldrSurvey.cacheKill();
  myLoad(url, "possibleProblems", loadPossibleProblemsFromJson);
}

function loadPossibleProblemsFromJson(json) {
  if (verifyJson(json, "possibleProblems")) {
    if (json.dataLoadTime) {
      cldrDom.updateIf("dynload", json.dataLoadTime);
    }
    const theDiv = flipper.flipToEmpty(pages.other);
    insertLocaleSpecialNote(theDiv);
    if (json.possibleProblems.length > 0) {
      const subDiv = cldrDom.createChunk("", "div");
      subDiv.className = "possibleProblems";
      const h3 = cldrDom.createChunk(cldrText.get("possibleProblems"), "h3");
      subDiv.appendChild(h3);
      const div3 = document.createElement("div");
      div3.innerHTML = cldrSurvey.testsToHtml(json.possibleProblems);
      subDiv.appendChild(div3);
      theDiv.appendChild(subDiv);
    }
    const theInfo = cldrDom.createChunk("", "p", "special_general");
    theDiv.appendChild(theInfo);
    theInfo.innerHTML = cldrText.get("special_general");
    cldrSurvey.hideLoader();
  }
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
  const url =
    cldrStatus.getContextPath() +
    "/SurveyAjax?what=getrow&_=" +
    curLocale +
    "&x=" +
    curPage +
    "&strid=" +
    curId +
    "&s=" +
    cldrStatus.getSessionId() +
    cldrSurvey.cacheKill();
  $("#nav-page").show(); // make top "Prev/Next" buttons visible while loading, cf. '#nav-page-footer' below
  myLoad(url, "section", function (json) {
    loadAllRowsFromJson(json, theDiv);
  });
}

function loadAllRowsFromJson(json, theDiv) {
  isLoading = false;
  cldrSurvey.showLoader(cldrText.get("loading2"));
  if (!verifyJson(json, "section")) {
    return;
  } else if (json.section.nocontent) {
    cldrStatus.setCurrentSection("");
    if (json.pageId) {
      cldrStatus.setCurrentPage(json.pageId);
    } else {
      cldrStatus.setCurrentPage("");
    }
    cldrSurvey.showLoader(null);
    updateHashAndMenus(false); // find out why there's no content. (locmap)
  } else if (!json.section.rows) {
    console.log("!json.section.rows");
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
    cldrStatus.setCurrentSection("");
    cldrStatus.setCurrentPage(json.pageId);
    updateHashAndMenus(false); // now that we have a pageid
    if (!cldrStatus.getSurveyUser()) {
      const message = cldrText.get("loginGuidance");
      cldrInfo.showMessage(message);
    } else if (!json.canModify) {
      const message = cldrText.get("readonlyGuidance");
      cldrInfo.showMessage(message);
    } else {
      const message = cldrText.get("dataPageInitialGuidance");
      cldrInfo.showMessage(message);
    }
    if (!cldrSurvey.isInputBusy()) {
      cldrSurvey.showLoader(cldrText.get("loading3"));
      cldrTable.insertRows(
        theDiv,
        json.pageId,
        cldrStatus.getSessionId(),
        json
      ); // pageid is the xpath..
      cldrSurvey.updateCoverage(flipper.get(pages.data)); // make sure cov is set right before we show.
      flipper.flipTo(pages.data); // TODO now? or later?
      showCurrentId(); // already calls scroll
      cldrSurvey.refreshCounterVetting();
      $("#nav-page-footer").show(); // make bottom "Prev/Next" buttons visible after building table
    }
  }
}

/**
 * Update the #hash and menus to the current settings.
 *
 * @param doPush {Boolean} if false, do not add to history
 */
function updateHashAndMenus(doPush) {
  if (!doPush) {
    doPush = false;
  }
  replaceHash(doPush); // update the hash
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
 * Automatically import old winning votes
 */
function doAutoImport() {
  const autoImportProgressDialog = newProgressDialog({
    title: cldrText.get("v_oldvote_auto_msg"),
    content: cldrText.get("v_oldvote_auto_progress_msg"),
  });
  if (autoImportProgressDialog) {
    autoImportProgressDialog.show();
  }
  haveDialog = true;
  cldrEvent.hideOverlayAndSidebar();
  /*
   * See WHAT_AUTO_IMPORT = "auto_import" in SurveyAjax.java
   */
  const url =
    cldrStatus.getContextPath() +
    "/SurveyAjax?what=auto_import&s=" +
    cldrStatus.getSessionId() +
    cldrSurvey.cacheKill();
  myLoad(url, "auto-importing votes", function (json) {
    if (autoImportProgressDialog) {
      autoImportProgressDialog.hide();
    }
    haveDialog = false;
    if (json.autoImportedOldWinningVotes) {
      const vals = {
        count: json.autoImportedOldWinningVotes,
      };
      const autoImportedDialog = newProgressDialog({
        title: cldrText.get("v_oldvote_auto_msg"),
        content: cldrText.sub("v_oldvote_auto_desc_msg", vals),
      });
      if (autoImportedDialog) {
        autoImportedDialog.addChild(
          new dijitButton({
            label: "OK",
            onClick: function () {
              haveDialog = false;
              autoImportedDialog.hide();
              reloadV();
            },
          })
        );
        autoImportedDialog.show();
      }
      haveDialog = true;
      cldrEvent.hideOverlayAndSidebar();
    }
  });
}

/**
 * @param postData optional - makes this a POST
 */
function myLoad(url, message, handler, postData, headers) {
  const otime = new Date().getTime();
  console.log("MyLoad: " + url + " for " + message);
  const errorHandler = function (err) {
    console.log("Error: " + err);
    cldrRetry.handleDisconnect(
      "Could not fetch " +
        message +
        " - error " +
        err +
        "\n url: " +
        url +
        "\n",
      null,
      "disconnect"
    );
  };
  const loadHandler = function (json) {
    console.log(
      "        " + url + " loaded in " + (new Date().getTime() - otime) + "ms"
    );
    try {
      handler(json);
      // resize height
      $("#main-row").css({
        height: $("#main-row>div").height(),
      });
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
  const clickyLink = cldrDom.createChunk(name, "a", "locName");
  clickyLink.href = linkToLocale(subLoc);
  subLocDiv.appendChild(clickyLink);
  if (subInfo == null) {
    console.log("* internal: subInfo is null for " + name + " / " + subLoc);
  }
  if (subInfo.name_var) {
    cldrDom.addClass(clickyLink, "name_var");
  }
  clickyLink.title = subLoc; // remove auto generated "locName.title"

  if (subInfo.readonly) {
    cldrDom.addClass(clickyLink, "locked");
    cldrDom.addClass(subLocDiv, "hide");

    if (subInfo.special_comment) {
      clickyLink.title = subInfo.special_comment;
    } else if (subInfo.dcChild) {
      clickyLink.title = cldrText.sub("defaultContentChild_msg", {
        name: subInfo.name,
        dcChild: subInfo.dcChild,
        dcChildName: locmap.getLocaleName(subInfo.dcChild),
      });
    } else {
      clickyLink.title = cldrText.get("readonlyGuidance");
    }
  } else if (subInfo.special_comment) {
    // could be the sandbox locale, or some other comment.
    clickyLink.title = subInfo.special_comment;
  }

  if (cldrMenu.canModifyLoc(subLoc)) {
    cldrDom.addClass(clickyLink, "canmodify");
  } else {
    cldrDom.addClass(subLocDiv, "hide"); // not modifiable
  }
  return clickyLink;
}

function getTheLocaleMap() {
  return locmap;
}

function setTheLocaleMap(lm) {
  locmap = lm;
}

// getHash and setHash are replacements for dojo/hash
// https://dojotoolkit.org/reference-guide/1.10/dojo/hash.html
// return "locales///";

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
 *
 * TODO: implement setHash with "replace"
 *
 * This is a replacement for dojo/hash
 *
 * "To manipulate the value of the hash, simply call dojo/hash with the new value.
 * It will be added to the browser history stack and it will publish a /dojo/hashchange topic,
 * triggering anything subscribed ...
 * In order to not to add to the history stack, pass true as the second parameter (replace).
 * This will update the current browser URL and replace the current history state"
 */
function setHash(newHash, replace) {
  newHash = sliceHash(newHash);
  if (newHash !== sliceHash(window.location.hash)) {
    const oldUrl = window.location.href;
    const newUrl = oldUrl.split("#")[0] + "#" + newHash;
    if (CLDR_LOAD_DEBUG) {
      console.log(
        "setHash going to " +
          newUrl +
          " - Called with: " +
          newHash +
          " - " +
          replace
      );
    }
    window.location.href = newUrl;
  }
}

// given "#foo" or "foo", return "foo"
function sliceHash(hash) {
  return hash.charAt(0) === "#" ? hash.slice(1) : hash;
}

function dialogIsOpen() {
  return haveDialog;
}

function newProgressDialog(args) {
  // TODO: implement a replacement for dijit/Dialog (or something simpler)
  // This is used only for doAutoImport
  console.log("newProgressDialog not implemented yet! args = " + args);
  return null;
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
  cldrSurvey.updateCoverage(flipper.get(pages.data));
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
  dialogIsOpen,
  flipToEmptyOther,
  flipToGenericNoLocale,
  flipToOtherDiv,
  getTheLocaleMap,
  handleCoverageChanged,
  insertLocaleSpecialNote,
  linkToLocale,
  myLoad,
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

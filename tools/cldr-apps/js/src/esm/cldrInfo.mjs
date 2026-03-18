/*
 * cldrInfo: encapsulate Survey Tool "Info Panel" (right sidebar) functions
 */
import * as cldrDeferHelp from "./cldrDeferHelp.mjs";
import * as cldrDom from "./cldrDom.mjs";
import * as cldrForumPanel from "./cldrForumPanel.mjs";
import * as cldrLoad from "./cldrLoad.mjs";
import * as cldrNotify from "./cldrNotify.mjs";
import * as cldrSideways from "./cldrSideways.mjs";
import * as cldrStatus from "./cldrStatus.mjs";
import * as cldrSurvey from "./cldrSurvey.mjs";
import * as cldrTable from "./cldrTable.mjs";
import * as cldrText from "./cldrText.mjs";
import * as cldrVoteTable from "./cldrVoteTable.mjs";
import * as cldrVue from "./cldrVue.mjs";

import InfoPanel from "../views/InfoPanel.vue";
import InfoSelectedItem from "../views/InfoSelectedItem.vue";
import InfoCandidateItems from "../views/InfoCandidateItems.vue";
import InfoTicketItem from "../views/InfoTicketItem.vue";
import InfoRegionalVariants from "../views/InfoRegionalVariants.vue";

const DEBUG = false;

const DISABLE_SIDEWAYS_MENU = false;

let containerId = null;
let neighborId = null;
let buttonClass = null;

let panelInitialized = false;

/**
 * Is the Info Panel currently displayed?
 */
let panelVisible = false;

/**
 * Does the user want the Info Panel to be displayed when appropriate?
 * The panel is hidden automatically when there is a "special" view for which
 * the panel is inappropriate. When returning to the non-special vetting view,
 * the panel should be displayed again, unless the user has indicated, by clicking
 * its close box, that they don't want to see it.
 */
let panelWanted = true;

let selectedItemWrapper = null;
let candidateItemsWrapper = null;
let ticketItemWrapper = null;
let regionalVariantsWrapper = null;

const ITEM_INFO_ID = "itemInfo"; // must match redesign.css
const ITEM_INFO_CLASS = "sidebyside-scrollable"; // must match redesign.css, cldrGui.mjs, DashboardWidget.vue

const HELP_HTML_ID = "info-panel-help";
const PLACEHOLDER_HELP_ID = "info-panel-placeholder";
const INFO_MESSAGE_ID = "info-panel-message";
const SELECTED_ITEM_ID = "info-panel-selected";
const INFO_VOTE_ID = "info-panel-vote";
const INFO_TICKET_ID = "info-panel-ticket";
const INFO_REGIONAL_ID = "info-panel-regional";
const INFO_FORUM_ID = "info-panel-forum";
const INFO_XPATH_ID = "info-panel-xpath";
const INFO_BOTTOM_ID = "info-panel-bottom";

/**
 * Initialize the Info Panel
 *
 * @param {String} cid the id of the container element for the panel
 * @param {String} nid the id of the neighboring element to the left of the panel
 * @param {String} bclass the class for "Open Info Panel" buttons
 */
function initialize(cid, nid, bclass) {
  containerId = cid;
  neighborId = nid;
  buttonClass = bclass;
  insertWidget();
  setPanelAndNeighborStyles();
  updateOpenPanelButtons();
}

function insertWidget() {
  try {
    const containerEl = document.getElementById(containerId);
    cldrVue.mountAsFirstChild(InfoPanel, containerEl);
    insertLegacyElement(containerEl);
    const selectedItemEl = document.getElementById(SELECTED_ITEM_ID);
    selectedItemWrapper = cldrVue.mount(InfoSelectedItem, selectedItemEl);
    const candidateItemsEl = document.getElementById(INFO_VOTE_ID);
    candidateItemsWrapper = cldrVue.mount(InfoCandidateItems, candidateItemsEl);
    const ticketItemEl = document.getElementById(INFO_TICKET_ID);
    ticketItemWrapper = cldrVue.mount(InfoTicketItem, ticketItemEl);
    if (!DISABLE_SIDEWAYS_MENU) {
      const regionalVariantsEl = document.getElementById(INFO_REGIONAL_ID);
      regionalVariantsWrapper = cldrVue.mount(
        InfoRegionalVariants,
        regionalVariantsEl
      );
    }
  } catch (e) {
    console.error("Error loading InfoPanel vue " + e.message + " / " + e.name);
    cldrNotify.exception(e, "while loading InfoPanel");
  }
}

/**
 * Create an element to display the Info Panel contents.
 *
 * For compatibility with legacy Survey Tool code, this is not a Vue component, although it
 * may contain some Vue components.
 *
 * Create divs inside the element whose id is ITEM_INFO_ID, each containing a part of the Info Panel
 * which may be either a legacy div or a Vue component.
 *
 * Ideally, eventually Vue components will be used for the entire Info Panel.
 *
 * @param {Element} containerEl the element whose new child will be created
 */
function insertLegacyElement(containerEl) {
  const el = document.createElement("section");
  el.className = ITEM_INFO_CLASS;
  el.id = ITEM_INFO_ID;
  containerEl.appendChild(el);
  appendDiv(el, HELP_HTML_ID);
  appendDiv(el, PLACEHOLDER_HELP_ID);
  appendDiv(el, INFO_MESSAGE_ID);
  appendDiv(el, SELECTED_ITEM_ID);
  appendDiv(el, INFO_VOTE_ID);
  appendDiv(el, INFO_TICKET_ID);
  appendDiv(el, INFO_REGIONAL_ID);
  appendDiv(el, INFO_FORUM_ID);
  appendDiv(el, INFO_XPATH_ID);
  appendDiv(el, INFO_BOTTOM_ID);
}

function appendDiv(el, id) {
  const div = document.createElement("div");
  div.id = id;
  el.appendChild(div);
}

/**
 * Open the Info Panel
 */
function openPanel() {
  if (!panelVisible) {
    panelVisible = panelWanted = true;
    openOrClosePanel();
  }
}

/**
 * Close the Info Panel
 *
 * @param {Boolean} userWantsHidden true if closing because user clicked close box (or equivalent),
 *       false if closing because switching to a "special" view where the Info Panel doesn't belong
 */
function closePanel(userWantsHidden) {
  if (userWantsHidden === undefined) {
    console.error("cldrInfo.closePanel was called with undefined parameter");
  }
  if (panelVisible) {
    panelVisible = false;
    panelWanted = !userWantsHidden;
    openOrClosePanel();
  }
}

function openOrClosePanel() {
  setPanelAndNeighborStyles();
  updateOpenPanelButtons();
}

function setPanelAndNeighborStyles() {
  const main = document.getElementById(neighborId);
  const info = document.getElementById(containerId);
  if (main && info) {
    if (panelVisible) {
      main.style.width = "75%";
      info.style.width = "25%";
      info.style.display = "flex";
    } else {
      main.style.width = "100%";
      info.style.display = "none";
    }
  }
}

/**
 * Show or hide the "Open Info Panel" button(s), and set their onclick action.
 * Such buttons should only be displayed when the panel is not already visible.
 */
function updateOpenPanelButtons() {
  const els = document.getElementsByClassName(buttonClass);
  Array.from(els).forEach((element) => {
    element.style.display = panelVisible ? "none" : "inline";
    element.onclick = () => openPanel();
  });
}

function showGuidance(canModify) {
  if (panelInitialized && cldrStatus.getCurrentId()) {
    return;
  }
  if (panelShouldBeShown()) {
    let key;
    if (!cldrStatus.getSurveyUser()) {
      key = "loginGuidance";
    } else {
      key = canModify ? "dataPageInitialGuidance" : "readonlyGuidance";
    }
    const str = cldrText.get(key);
    show(str, null);
  }
}

function refresh(tr) {
  if (panelShouldBeShown()) {
    show(null, tr);
  }
}

/**
 * Should the Info Panel be shown?
 *
 * Default is true when called the first time.
 * Subsequently, remember whether the user has left it open or closed.
 *
 * @returns true if the Info Panel should be shown, else false
 */
function panelShouldBeShown() {
  if (!panelInitialized) {
    panelInitialized = true;
    // Leave panelVisible = false until openPanel makes it true.
    return true;
  }
  return panelWanted && !cldrStatus.getCurrentSpecial();
}

/**
 * Display the given information in the Info Panel
 *
 * Open the panel if it's not already open
 *
 * @param {String} str the string to show at the top, or null
 * @param {Node} tr the <TR> of the row
 */
function show(str, tr) {
  openPanel();
  const theRow = tr?.theRow || null;
  addDeferredHelp(theRow); // if !theRow, erase (as when click Next/Previous)
  addPlaceholderHelp(theRow); // ditto
  addInfoMessage(str);
  if (theRow) {
    updateRowVoteInfo(theRow);
  }
  addSelectedItem(theRow);
  addTicketLink(theRow);
  if (!DISABLE_SIDEWAYS_MENU) {
    addRegionalSidewaysMenu(tr?.xpstrid);
  }
  addForumPanel(tr);
  addXpath(theRow);
  addBottom();
}

function addDeferredHelp(theRow) {
  const el = document.getElementById(HELP_HTML_ID);
  if (!el) {
    return;
  }
  if (theRow) {
    const { helpHtml, rdf, translationHint } = theRow;
    if (helpHtml || rdf || translationHint) {
      const fragment = document.createDocumentFragment();
      cldrDeferHelp.addDeferredHelpTo(fragment, helpHtml, rdf, translationHint);
      if (el.firstChild) {
        el.firstChild.replaceWith(fragment);
      } else {
        el.appendChild(fragment);
      }
      return;
    }
  }
  cldrDom.removeAllChildNodes(el);
}

function addPlaceholderHelp(theRow) {
  const el = document.getElementById(PLACEHOLDER_HELP_ID);
  if (!el) {
    return;
  }
  if (theRow) {
    const { placeholderStatus, placeholderInfo } = theRow;
    if (placeholderStatus !== "DISALLOWED") {
      const fragment = document.createDocumentFragment();
      cldrDeferHelp.addPlaceholderHelp(
        fragment,
        placeholderStatus,
        placeholderInfo
      );
      if (el.firstChild) {
        el.firstChild.replaceWith(fragment);
      } else {
        el.appendChild(fragment);
      }
      return;
    }
  }
  cldrDom.removeAllChildNodes(el);
}

function addInfoMessage(html) {
  const el = document.getElementById(INFO_MESSAGE_ID);
  if (el) {
    if (html) {
      const div = document.createElement("div");
      div.innerHTML = html;
      if (el.firstChild) {
        el.firstChild.replaceWith(div);
      } else {
        el.appendChild(div);
      }
    } else {
      cldrDom.removeAllChildNodes(el);
    }
  }
}

function addSelectedItem(theRow) {
  if (!selectedItemWrapper) {
    return;
  }
  const item = findSelectedItem(theRow);

  const { displayValue, valueClass } = getValueAndClass(theRow, item);
  selectedItemWrapper.setValueAndClass(displayValue, valueClass);

  const { language, direction } = getLanguageAndDirection();
  selectedItemWrapper.setLanguageAndDirection(language, direction);

  const description = getItemDescription(item?.status, theRow);
  selectedItemWrapper.setDescription(description);

  const { linkUrl, linkText } = getLinkUrlAndText(theRow, item);
  selectedItemWrapper.setLink(linkUrl, linkText);

  const testHtml = item?.tests
    ? cldrSurvey.testsToHtml(item.tests)
    : "<i>no tests</i>";
  selectedItemWrapper.setTestHtml(testHtml);

  selectedItemWrapper.setExampleHtml(item?.example);
}

/**
 * Find the selected or default candidate item for the given row.
 *
 * If no item is selected, default to the winning item, or null if there is no winning item.
 *
 * The user can select a candidate item by clicking on it in the "Winning" or "Others" column.
 *
 * @param {Object} theRow
 * @returns {Object} the candidate item
 */
function findSelectedItem(theRow) {
  if (!theRow) {
    return null;
  }
  let valueHash = cldrStatus.getCurrentValueHash();
  if (!valueHash) {
    if (!theRow.winningVhash) {
      if (DEBUG) {
        console.log("No valueHash OR winningVhash in findSelectedItem");
      }
    } else {
      if (DEBUG) {
        console.log("No valueHash in findSelectedItem; using winningVhash");
      }
      valueHash = theRow.winningVhash;
      cldrStatus.setCurrentValueHash(valueHash);
    }
  }
  if (valueHash) {
    const item = cldrTable.findItemByValueHash(theRow, valueHash);
    if (item) {
      return item;
    }
  }
  if (DEBUG) {
    // This is not necessarily a bug. Some rows in some locales have no candidate items.
    console.log(
      "No item in findSelectedItem; no match for valueHash = " + valueHash
    );
  }
  return null;
}

function getValueAndClass(theRow, item) {
  let displayValue =
    item?.value === cldrSurvey.INHERITANCE_MARKER
      ? theRow?.inheritedDisplayValue
      : item?.value;
  if (!displayValue) {
    displayValue = ""; // not "undefined"
  }
  const valueClass = item?.status || "value";
  return { displayValue, valueClass };
}

function getLanguageAndDirection() {
  const loc = cldrStatus.getCurrentLocale();
  const locMap = loc ? cldrLoad.getTheLocaleMap() : null;
  const locInfo = locMap ? locMap.getLocaleInfo(loc) : null;
  const language = locInfo?.bcp47 || "";
  const direction = locInfo?.dir || "";
  return { language, direction };
}

/**
 * Add a link in the Info Panel for "Jump to Original" (cldrText.get('followAlias')),
 * if appropriate.
 *
 * @param {Object} theRow the row
 * @param {Object} item the candidate item
 */
function getLinkUrlAndText(theRow, item) {
  let linkUrl = null;
  let linkText = null;
  if (
    item?.value === cldrSurvey.INHERITANCE_MARKER &&
    item?.status !== "constructed" &&
    theRow?.inheritedUrl
  ) {
    linkText = cldrText.get("followAlias");
    linkUrl = theRow.inheritedUrl;
  }
  return { linkUrl, linkText };
}

/**
 * Update the voting info for this row
 *
 * @param theRow {Object} the data from the server for this row
 */
function updateRowVoteInfo(theRow) {
  if (!theRow) {
    console.error("theRow is null or undefined in updateRowVoteInfo");
    return;
  }
  if (!candidateItemsWrapper) {
    console.error(
      "candidateItemsWrapper is null or undefined in updateRowVoteInfo"
    );
    return;
  }
  const data = { candidateItems: [] };
  const surveyUser = cldrStatus.getSurveyUser();
  if (theRow.voteVhash && surveyUser) {
    const voteForItem = cldrTable.findItemByValueHash(theRow, theRow.voteVhash);
    if (voteForItem?.votes[surveyUser.id]?.voteDetails?.override) {
      data.overrideMessage = cldrText.sub("override_explain_msg", {
        overrideVotes: voteForItem.votes[surveyUser.id].voteDetails.override,
        votes: surveyUser.votecount,
      });
    }
    data.rowFlagged = Boolean(theRow.rowFlagged);
  }

  /*
   * The value_vote array has an even number of elements,
   * like [value0, vote0, value1, vote1, value2, vote2, ...].
   */
  const vr = theRow.votingResults;
  let n = 0;
  while (n < vr.value_vote.length) {
    const value = vr.value_vote[n++];
    const vote = parseInt(vr.value_vote[n++]);
    if (value === cldrTable.NO_WINNING_VALUE) {
      continue;
    }
    const item = cldrTable.findItemByRawValue(theRow, value);
    if (item == null) {
      continue;
    }
    if (value === cldrSurvey.INHERITANCE_MARKER && !theRow.inheritedValue) {
      continue; // theRow.inheritedValue can be undefined here; then do not append
    }
    const voteTableData = cldrVoteTable.construct(
      theRow.votingResults,
      item,
      value,
      vote
    );
    const candidateItem = {
      isBaselineValue: Boolean(item.isBaselineValue),
      vote: vote,
      voteTableData: voteTableData,
    };
    if (value === cldrSurvey.INHERITANCE_MARKER) {
      candidateItem.displayValue = theRow.inheritedDisplayValue;
      candidateItem.status = item.status;
      candidateItem.elaboration = cldrText.get("voteInfo_votesForInheritance");
    } else {
      candidateItem.displayValue = item.value;
      candidateItem.status = theRow.winningValue ? "winner" : "value";
      candidateItem.elaboration = "";
    }
    data.candidateItems.push(candidateItem);
  }
  data.transcript = theRow.voteTranscript;
  if (vr.valueIsLocked) {
    data.isLocked = true;
  } else if (vr.requiredVotes >= 50) {
    // Note: in this case, the numeric value of vr.requiredVotes (50+) is not included in the message
    data.explainFlag = true;
  } else if (vr.requiredVotes) {
    data.reqVoteMessage = cldrText.sub("explainRequiredVotes", {
      requiredVotes: vr.requiredVotes,
    });
  }
  const { language, direction } = getLanguageAndDirection();
  data.language = language;
  data.direction = direction;
  candidateItemsWrapper.setData(data);
}

/**
 * Add a section in the Info Panel for the given ticket link.
 *
 * This happens, for example, if the back end returns data for a PathHeader with status = READ_ONLY.
 *
 * @param {Object} theRow
 */
function addTicketLink(theRow) {
  if (ticketItemWrapper) {
    let linkText = null;
    let linkUrl = null;
    if (theRow?.hasTicketLink) {
      linkText = cldrText.get("file_a_ticket");
      const curLocale = cldrStatus.getCurrentLocale();
      // Note: the portion of this URL following "?" is currently (2026-03) ignored by the server.
      // It dates back to when CLDR used "trac".
      linkUrl =
        "https://cldr.unicode.org/requesting_changes#TOC-Filing-a-Ticket" +
        "?component=data&summary=" +
        curLocale +
        ":" +
        theRow.xpath +
        "&locale=" +
        curLocale +
        "&xpath=" +
        theRow.xpstrid +
        "&version=" +
        cldrStatus.getNewVersion();
      if (cldrStatus.getIsUnofficial()) {
        linkText += cldrText.get("file_ticket_unofficial");
        linkUrl += "&description=NOT+PRODUCTION+SURVEYTOOL!";
      }
    }
    ticketItemWrapper.setLink(linkUrl, linkText);
  }
}

// regional variants (sibling locales)
function addRegionalSidewaysMenu(xpstrid) {
  if (!regionalVariantsWrapper) {
    return;
  }
  cldrSideways.loadMenu(regionalVariantsWrapper, xpstrid);
}

function addForumPanel(tr) {
  const el = document.getElementById(INFO_FORUM_ID);
  if (!el) {
    return;
  }
  const fragment = document.createDocumentFragment();
  if (tr?.theRow && !cldrStatus.isVisitor()) {
    cldrForumPanel.loadInfo(fragment, tr, tr.theRow);
  }
  cldrDom.removeAllChildNodes(el);
  if (tr) {
    el.appendChild(fragment);
  }
}

/**
 * An empty paragraph at the bottom of the info panel enables scrolling
 * to bring the bottom content fully into view without being overlapped
 * by the xpath shown by addXpath
 */
function addBottom() {
  const el = document.getElementById(INFO_BOTTOM_ID);
  if (!el) {
    return;
  }
  el.innerHTML = "<p>&nbsp;</p>";
}

function addXpath(theRow) {
  const el = document.getElementById(INFO_XPATH_ID);
  if (!el) {
    return;
  }
  const fragment = document.createDocumentFragment();
  if (theRow?.xpath) {
    fragment.appendChild(
      cldrDom.clickToSelect(cldrDom.createChunk(theRow.xpath, "div", "xpath"))
    );
  }
  cldrDom.removeAllChildNodes(el);
  if (theRow) {
    el.appendChild(fragment);
  }
}

function getItemDescription(candidateStatus, theRow) {
  if (!candidateStatus) {
    return "";
  }
  /*
   * candidateStatus may be "winner, "alias", "constructed", "fallback", "fallback_code", "fallback_root", or "loser".
   * See DataPage.DataRow.CandidateItem.getCandidateStatus in DataPage.java
   */
  const inheritedLocale = theRow?.inheritedLocale;
  if (candidateStatus === "fallback") {
    const locName = cldrLoad.getLocaleName(inheritedLocale);
    return cldrText.sub("item_description_fallback", [locName]);
  } else if (candidateStatus === "alias") {
    if (inheritedLocale === cldrStatus.getCurrentLocale()) {
      return cldrText.get("item_description_alias_same_locale");
    } else {
      const locName = cldrLoad.getLocaleName(inheritedLocale);
      return cldrText.sub("item_description_alias_diff_locale", [locName]);
    }
  } else if (candidateStatus === "constructed") {
    return cldrText.get("noFollowAlias");
  } else {
    // Strings produced here, used as keys for cldrText.get(), may include:
    // "item_description_winner", "item_description_fallback_code", "item_description_fallback_root", "item_description_loser".
    return cldrText.get("item_description_" + candidateStatus);
  }
}

function clearCachesAndReload() {
  cldrForumPanel.clearCache();
  if (!DISABLE_SIDEWAYS_MENU) {
    cldrSideways.clearCache();
  }
  cldrLoad.reloadV();
}

export { clearCachesAndReload, closePanel, initialize, refresh, showGuidance };

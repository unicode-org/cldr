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
import * as cldrUserLevels from "./cldrUserLevels.mjs";
import * as cldrVote from "./cldrVote.mjs";
import * as cldrVue from "./cldrVue.mjs";

import InfoPanel from "../views/InfoPanel.vue";
import InfoSelectedItem from "../views/InfoSelectedItem.vue";
import InfoRegionalVariants from "../views/InfoRegionalVariants.vue";

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

let unShow = null;

let selectedItemWrapper = null;
let regionalVariantsWrapper = null;

const ITEM_INFO_ID = "itemInfo"; // must match redesign.css
const ITEM_INFO_CLASS = "sidebyside-scrollable"; // must match redesign.css, cldrGui.mjs, DashboardWidget.vue

const HELP_HTML_ID = "info-panel-help";
const PLACEHOLDER_HELP_ID = "info-panel-placeholder";
const INFO_MESSAGE_ID = "info-panel-message";
const SELECTED_ITEM_ID = "info-panel-selected";
const INFO_VOTE_TICKET_ID = "info-panel-vote-and-ticket";
const INFO_REGIONAL_ID = "info-panel-regional";
const INFO_FORUM_ID = "info-panel-forum";
const INFO_XPATH_ID = "info-panel-xpath";

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
    const regionalVariantsEl = document.getElementById(INFO_REGIONAL_ID);
    regionalVariantsWrapper = cldrVue.mount(
      InfoRegionalVariants,
      regionalVariantsEl
    );
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
 * The legacy code involving showRowObjFunc, etc., does extensive direct DOM manipulation.
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
  appendDiv(el, INFO_VOTE_TICKET_ID);
  appendDiv(el, INFO_REGIONAL_ID);
  appendDiv(el, INFO_FORUM_ID);
  appendDiv(el, INFO_XPATH_ID);
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

// This method is now only used for getGuidanceMessage, for the Page table
// before any row has been selected. Avoid using it for anything else.
// cldrLoad.mjs: cldrInfo.showMessage(getGuidanceMessage(json.canModify));
function showMessage(str) {
  if (panelShouldBeShown()) {
    show(str, null, null, null);
  }
}

// Major tech debt here. Currently called as follows:
// cldrLoad.mjs:   cldrInfo.showRowObjFunc(xtr, xtr.proposedcell, xtr.proposedcell.showFn);
// cldrTable.mjs:  cldrInfo.showRowObjFunc(tr, tr.proposedcell, tr.proposedcell.showFn);
// cldrVote.mjs:   cldrInfo.showRowObjFunc(tr, ourDiv, ourShowFn);
function showRowObjFunc(tr, hideIfLast, fn) {
  if (panelShouldBeShown()) {
    show(null, tr, hideIfLast, fn);
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
 * @param {String} str the string to show at the top
 * @param {Node} tr the <TR> of the row
 * @param {Object} hideIfLast mysterious parameter, a.k.a. theObj
 * @param {Function} fn the draw function, a.k.a. showFn, sometimes constructed by cldrTable.showItemInfoFn,
 *                      sometimes ourShowFn in cldrVote.showProposedItem
 */
function show(str, tr, hideIfLast, fn) {
  openPanel();
  if (unShow) {
    unShow();
    unShow = null;
  }
  cldrTable.updateSelectedRowAndCell(tr, hideIfLast);
  addDeferredHelp(tr?.theRow); // if !tr.theRow, erase (as when click Next/Previous)
  addPlaceholderHelp(tr?.theRow); // ditto
  addInfoMessage(str);
  addVoteDivAndTicketLink(tr, fn);
  addSelectedItem(tr?.theRow); // after addVoteDivAndTicketLink calls fn to set theRow.selectedItem
  addRegionalSidewaysMenu(tr);
  addForumPanel(tr);
  addXpath(tr);
  addVoterInfoHover();
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
  const item = theRow?.selectedItem;

  const { displayValue, valueClass } = getValueAndClass(theRow, item);
  selectedItemWrapper.setValueAndClass(displayValue, valueClass);

  const { language, direction } = getLanguageAndDirection();
  selectedItemWrapper.setLanguageAndDirection(language, direction);

  const description = getItemDescription(item?.pClass, theRow?.inheritedLocale);
  selectedItemWrapper.setDescription(description);

  const { linkUrl, linkText } = getLinkUrlAndText(theRow, item);
  selectedItemWrapper.setLink(linkUrl, linkText);

  const testHtml = item?.tests
    ? cldrSurvey.testsToHtml(item.tests)
    : "<i>no tests</i>";
  selectedItemWrapper.setTestHtml(testHtml);

  selectedItemWrapper.setExampleHtml(item?.example);
}

function getValueAndClass(theRow, item) {
  let displayValue =
    item?.value === cldrSurvey.INHERITANCE_MARKER
      ? theRow?.inheritedDisplayValue
      : item?.value;
  if (!displayValue) {
    displayValue = ""; // not "undefined"
  }
  const valueClass = item?.pClass || "value";
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
 * if theRow.inheritedLocale or theRow.inheritedXpid is defined.
 *
 * Normally at least one of theRow.inheritedLocale and theRow.inheritedXpid should be
 * defined whenever we have an INHERITANCE_MARKER item. Otherwise an error is reported
 * by checkRowConsistency.
 *
 * An alternative would be for the server to send the link, instead of inheritedLocale
 * and inheritedXpid, to the client, avoiding the need for the client to know so much,
 * including the need to replace 'code-fallback' with 'root' or when to use cldrStatus.getCurrentLocale()
 * in place of inheritedLocale or use xpstrid in place of inheritedXpid.
 *
 * @param {Object} theRow the row
 * @param {Object} item the candidate item
 */
function getLinkUrlAndText(theRow, item) {
  let linkUrl = null;
  let linkText = null;
  if (
    item?.value === cldrSurvey.INHERITANCE_MARKER &&
    (theRow?.inheritedLocale || theRow?.inheritedXpid)
  ) {
    let loc = theRow.inheritedLocale;
    let xpstrid = theRow.inheritedXpid || theRow.xpstrid;
    if (!loc) {
      loc = cldrStatus.getCurrentLocale();
    } else if (loc === "code-fallback") {
      /*
       * Never use 'code-fallback' in the link, use 'root' instead.
       * On the server, 'code-fallback' sometimes goes by the name XMLSource.CODE_FALLBACK_ID.
       */
      loc = "root";
    }
    if (xpstrid === theRow.xpstrid && loc === cldrStatus.getCurrentLocale()) {
      // following the alias would come back to the current item; no link
      linkText = cldrText.get("noFollowAlias");
    } else {
      linkText = cldrText.get("followAlias");
      linkUrl = "#/" + loc + "//" + xpstrid;
    }
  }
  return { linkUrl, linkText };
}

function addVoteDivAndTicketLink(tr, fn) {
  const fragment = document.createDocumentFragment();

  // If a generator fn (common case), call it.
  // Typically, fn is the function returned by showItemInfoFn.
  // However, there is also "ourShowFn" in cldrVote.mjs...
  // It's not clear why this is so indirect and complicated; tech debt; probably it could be more straightforward.
  if (fn) {
    unShow = fn(fragment);
  }
  if (tr?.voteDiv) {
    fragment.appendChild(tr.voteDiv.cloneNode(true));
  }
  if (tr?.ticketLink) {
    fragment.appendChild(tr.ticketLink.cloneNode(true));
  }
  const el = document.getElementById(INFO_VOTE_TICKET_ID);
  if (!el) {
    return;
  }
  cldrDom.removeAllChildNodes(el);
  el.appendChild(fragment);
}

// regional variants (sibling locales)
function addRegionalSidewaysMenu(tr) {
  if (!regionalVariantsWrapper) {
    return;
  }
  cldrSideways.loadMenu(regionalVariantsWrapper, tr?.xpstrid);
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

function addXpath(tr) {
  const el = document.getElementById(INFO_XPATH_ID);
  if (!el) {
    return;
  }
  const fragment = document.createDocumentFragment();
  if (tr?.theRow?.xpath) {
    fragment.appendChild(
      cldrDom.clickToSelect(
        cldrDom.createChunk(tr.theRow.xpath, "div", "xpath")
      )
    );
  }
  cldrDom.removeAllChildNodes(el);
  if (tr) {
    el.appendChild(fragment);
  }
}

function addVoterInfoHover() {
  $(".voteInfo_voterInfo").hover(
    function () {
      const email = $(this).data("email").replace(" (at) ", "@");
      if (email) {
        $(this).html(
          '<a href="mailto:' +
            email +
            '" title="' +
            email +
            '" style="color:black"><span class="glyphicon glyphicon-envelope"></span></a>'
        );
        $(this).closest("td").css("text-align", "center");
        $(this).children("a").tooltip().tooltip("show");
      } else {
        $(this).html($(this).data("name"));
        $(this).closest("td").css("text-align", "left");
      }
    },
    function () {
      $(this).html($(this).data("name"));
      $(this).closest("td").css("text-align", "left");
    }
  );
}

/**
 * Update the vote info for this row.
 *
 * Set up the "vote div".
 *
 * @param tr the table row
 * @param theRow the data from the server for this row
 *
 * Called by updateRow.
 *
 * TODO: shorten this function by using subroutines.
 */
function updateRowVoteInfo(tr, theRow) {
  if (!theRow) {
    console.error("theRow is null or undefined in updateRowVoteInfo");
    return;
  }
  const vr = theRow.votingResults;
  tr.voteDiv = document.createElement("div");
  tr.voteDiv.className = "voteDiv";
  const surveyUser = cldrStatus.getSurveyUser();
  if (theRow.voteVhash && theRow.voteVhash !== "" && surveyUser) {
    const voteForItem = theRow.items[theRow.voteVhash];
    if (
      voteForItem &&
      voteForItem.votes &&
      voteForItem.votes[surveyUser.id] &&
      voteForItem.votes[surveyUser.id].overridedVotes
    ) {
      tr.voteDiv.appendChild(
        cldrDom.createChunk(
          cldrText.sub("override_explain_msg", {
            overrideVotes: voteForItem.votes[surveyUser.id].overridedVotes,
            votes: surveyUser.votecount,
          }),
          "p",
          "helpContent"
        )
      );
    }
    if (theRow.rowFlagged) {
      cldrSurvey.addIcon(tr.voteDiv, "i-flag");
      tr.voteDiv.appendChild(
        cldrDom.createChunk(cldrText.get("flag_desc", "p", "helpContent"))
      );
    }
  }

  /*
   * The value_vote array has an even number of elements,
   * like [value0, vote0, value1, vote1, value2, vote2, ...].
   */
  let n = 0;
  while (n < vr.value_vote.length) {
    const value = vr.value_vote[n++];
    const vote = parseInt(vr.value_vote[n++]);
    if (value === cldrTable.NO_WINNING_VALUE) {
      continue;
    }
    var item = tr.rawValueToItem[value]; // backlink to specific item in hash
    if (item == null) {
      continue;
    }
    var vdiv = cldrDom.createChunk(
      null,
      "table",
      "voteInfo_perValue table table-vote"
    );
    var valdiv = cldrDom.createChunk(
      null,
      "div",
      n > 2 ? "value-div" : "value-div first"
    );
    // heading row
    var vrow = cldrDom.createChunk(
      null,
      "tr",
      "voteInfo_tr voteInfo_tr_heading"
    );
    if (
      item.rawValue === cldrSurvey.INHERITANCE_MARKER ||
      (item.votes && Object.keys(item.votes).length > 0)
    ) {
      vrow.appendChild(
        cldrDom.createChunk(
          cldrText.get("voteInfo_orgColumn"),
          "td",
          "voteInfo_orgColumn voteInfo_td"
        )
      );
    }
    var isection = cldrDom.createChunk(null, "div", "voteInfo_iconBar");
    var isectionIsUsed = false;
    var vvalue = cldrDom.createChunk(
      "User",
      "td",
      "voteInfo_valueTitle voteInfo_td"
    );
    var vbadge = cldrDom.createChunk(vote, "span", "badge");

    /*
     * Note: we can't just check for item.pClass === "winner" here, since, for example, the winning value may
     * have value = cldrSurvey.INHERITANCE_MARKER and item.pClass = "alias".
     */
    if (value === theRow.winningValue) {
      const statusClass = cldrTable.getRowApprovalStatusClass(theRow);
      const statusTitle = cldrText.get(statusClass);
      cldrDom.appendIcon(
        isection,
        "voteInfo_winningItem d-dr-" + statusClass,
        cldrText.sub("draftStatus", [statusTitle])
      );
      isectionIsUsed = true;
    }
    if (item.isBaselineValue) {
      cldrDom.appendIcon(
        isection,
        "i-star",
        cldrText.get("voteInfo_baseline_desc")
      );
      isectionIsUsed = true;
    }
    cldrSurvey.setLang(valdiv); // want the whole div to be marked as cldrValue
    if (value === cldrSurvey.INHERITANCE_MARKER) {
      /*
       * theRow.inheritedValue can be undefined here; then do not append
       */
      if (theRow.inheritedValue) {
        cldrVote.appendItem(valdiv, theRow.inheritedDisplayValue, item.pClass);
        valdiv.appendChild(
          cldrDom.createChunk(cldrText.get("voteInfo_votesForInheritance"), "p")
        );
      }
    } else {
      cldrVote.appendItem(
        valdiv,
        item.value, // get display value not raw
        value === theRow.winningValue ? "winner" : "value"
      );
    }
    if (value === theRow.inheritedValue) {
      valdiv.appendChild(
        cldrDom.createChunk(cldrText.get("voteInfo_votesForSpecificValue"), "p")
      );
    }
    if (isectionIsUsed) {
      valdiv.appendChild(isection);
    }
    vrow.appendChild(vvalue);
    var cell = cldrDom.createChunk(
      null,
      "td",
      "voteInfo_voteTitle voteInfo_voteCount voteInfo_td" + ""
    );
    cell.appendChild(vbadge);
    vrow.appendChild(cell);
    vdiv.appendChild(vrow);
    const itemVotesLength = item.votes ? Object.keys(item.votes).length : 0;
    const anon =
      itemVotesLength == 1 &&
      cldrUserLevels.match(
        item.votes[Object.keys(item.votes)[0]].level,
        cldrUserLevels.ANONYMOUS
      );
    if (itemVotesLength == 0 || anon) {
      var vrow = cldrDom.createChunk(
        null,
        "tr",
        "voteInfo_tr voteInfo_orgHeading"
      );
      vrow.appendChild(
        cldrDom.createChunk(
          cldrText.get("voteInfo_noVotes"),
          "td",
          "voteInfo_noVotes voteInfo_td"
        )
      );
      const anonVoter = anon ? cldrText.get("voteInfo_anon") : null;
      vrow.appendChild(
        cldrDom.createChunk(anonVoter, "td", "voteInfo_noVotes voteInfo_td")
      );
      vdiv.appendChild(vrow);
    } else {
      updateRowVoteInfoForAllOrgs(theRow, vr, value, item, vdiv);
    }
    tr.voteDiv.appendChild(valdiv);
    tr.voteDiv.appendChild(vdiv);
  }
  tr.voteDiv.appendChild(makeVoteExplainerDiv(theRow.voteTranscript));
  if (vr.valueIsLocked) {
    tr.voteDiv.appendChild(
      cldrDom.createChunk(
        cldrText.get("valueIsLocked"),
        "p",
        "alert alert-warning fix-popover-help"
      )
    );
  } else if (vr.requiredVotes) {
    var msg = cldrText.sub("explainRequiredVotes", {
      requiredVotes: vr.requiredVotes,
    });
    tr.voteDiv.appendChild(
      cldrDom.createChunk(msg, "p", "alert alert-warning fix-popover-help")
    );
  }
}

function makeVoteExplainerDiv(voteTranscript) {
  const voteExplainerDiv = document.createElement("div");
  const showTranscriptIcon = cldrDom.createChunk(
    "",
    "i",
    "glyphicon glyphicon-info-sign show-transcript"
  );
  showTranscriptIcon.setAttribute("title", cldrText.get("transcript_flyover"));
  const showTranscriptLink = cldrDom.createChunk("", "a", "show-transcript");
  showTranscriptLink.setAttribute(
    "href",
    "javascript:window.cldrBundle.toggleTranscript()"
  );
  showTranscriptLink.appendChild(showTranscriptIcon);
  voteExplainerDiv.appendChild(showTranscriptLink);
  if (voteTranscript) {
    const transcriptBox = cldrDom.createChunk(
      "",
      "div",
      "transcript-container"
    );
    const transcriptText = cldrDom.createChunk(
      voteTranscript,
      "pre",
      "transcript-text"
    );
    transcriptBox.appendChild(transcriptText);
    const transcriptNote = document.createElement("p");
    transcriptNote.className = "alert alert-warning";
    transcriptNote.innerHTML = cldrText.get("transcript_note");
    transcriptBox.appendChild(transcriptNote);
    voteExplainerDiv.appendChild(transcriptBox);
    voteExplainerDiv.appendChild(document.createElement("br"));
  }
  return voteExplainerDiv;
}

/**
 * Update the vote info for one candidate item in this row, looping through all the orgs.
 * Information will be displayed in the Information Panel (right edge of window).
 *
 * @param theRow the row
 * @param vr the vote resolver
 * @param value the value of the candidate item
 * @param item the candidate item
 * @param vdiv a table created by the caller as vdiv = cldrDom.createChunk(null, "table", "voteInfo_perValue table table-vote")
 */
function updateRowVoteInfoForAllOrgs(theRow, vr, value, item, vdiv) {
  for (let org in vr.orgs) {
    var theOrg = vr.orgs[org];
    var orgVoteValue = theOrg.votes[value];
    /*
     * We should display something under "Org." and "User" even when orgVoteValue is zero (not undefined),
     * for "anonymous" imported losing votes. Therefore do not require orgVoteValue > 0 here.
     * There does not appear to be any circumstance where we need to hide a zero vote count (on the client).
     * If we do discover such a circumstance, we could display 0 vote only if voter is "anonymous";
     * currently such voters have org = "cldr"; but if we don't need such a dependency here, don't add it.
     * Reference: https://unicode.org/cldr/trac/ticket/11517
     */
    if (orgVoteValue !== undefined) {
      // someone in the org actually voted for it
      var topVoter = null; // top voter for this item
      var orgsVote = theOrg.orgVote == value; // boolean
      var topVoterTime = 0; // Calculating the latest time for a user from same org
      if (orgsVote) {
        // find a top-ranking voter to use for the top line
        for (var voter in item.votes) {
          if (
            item.votes[voter].org == org &&
            item.votes[voter].votes == theOrg.votes[value]
          ) {
            if (topVoterTime != 0) {
              // Get the latest time vote only
              if (vr.nameTime[`#${topVoter}`] < vr.nameTime[`#${voter}`]) {
                topVoter = voter;
                topVoterTime = vr.nameTime[`#${topVoter}`];
              }
            } else {
              topVoter = voter;
              topVoterTime = vr.nameTime[`#${topVoter}`];
            }
          }
        }
      }
      if (!topVoter) {
        // just find someone in the right org..
        for (var voter in item.votes) {
          if (item.votes[voter].org == org) {
            topVoter = voter;
            break;
          }
        }
      }
      // ORG SUBHEADING row

      /*
       * This only affects cells ("td" elements) with style "voteInfo_voteCount", which appear in the info panel,
       * and which have contents like '<span class="badge">12</span>'. If the "fallback" style is added, then
       * these circled numbers are surrounded (outside the circle) by a colored background.
       *
       * TODO: see whether the colored background is actually wanted in this context, around the numbers.
       * For now, display it, and use item.pClass rather than literal "fallback" so the color matches when
       * item.pClass is "alias", "fallback_root", etc.
       */
      var baileyClass =
        item.rawValue === cldrSurvey.INHERITANCE_MARKER
          ? " " + item.pClass
          : "";
      var vrow = cldrDom.createChunk(
        null,
        "tr",
        "voteInfo_tr voteInfo_orgHeading"
      );
      vrow.appendChild(
        cldrDom.createChunk(org, "td", "voteInfo_orgColumn voteInfo_td")
      );
      if (item.votes[topVoter]) {
        vrow.appendChild(createVoter(item.votes[topVoter])); // voteInfo_td
      } else {
        vrow.appendChild(createVoter(null));
      }
      if (orgsVote) {
        var cell = cldrDom.createChunk(
          null,
          "td",
          "voteInfo_orgsVote voteInfo_voteCount voteInfo_td" + baileyClass
        );
        cell.appendChild(cldrDom.createChunk(orgVoteValue, "span", "badge"));
        vrow.appendChild(cell);
      } else {
        vrow.appendChild(
          cldrDom.createChunk(
            orgVoteValue,
            "td",
            "voteInfo_orgsNonVote voteInfo_voteCount voteInfo_td" + baileyClass
          )
        );
      }
      vdiv.appendChild(vrow);
      // now, other rows:
      for (var voter in item.votes) {
        if (
          item.votes[voter].org != org || // wrong org or
          voter == topVoter
        ) {
          // already done
          continue; // skip
        }
        // OTHER VOTER row
        var vrow = cldrDom.createChunk(null, "tr", "voteInfo_tr");
        vrow.appendChild(
          cldrDom.createChunk("", "td", "voteInfo_orgColumn voteInfo_td")
        ); // spacer
        vrow.appendChild(createVoter(item.votes[voter])); // voteInfo_td
        vrow.appendChild(
          cldrDom.createChunk(
            item.votes[voter].votes,
            "td",
            "voteInfo_orgsNonVote voteInfo_voteCount voteInfo_td" + baileyClass
          )
        );
        vdiv.appendChild(vrow);
      }
    }
  }
}

/**
 * Create an element representing a voter, including a link to the voter's email
 *
 * @param v the voter
 * @return the element
 */
function createVoter(v) {
  if (v == null) {
    return cldrDom.createChunk("(missing information)!", "i", "stopText");
  }
  var div = cldrDom.createChunk(
    v.name || cldrText.get("emailHidden"),
    "td",
    "voteInfo_voterInfo voteInfo_td"
  );
  div.setAttribute("data-name", v.name || cldrText.get("emailHidden"));
  div.setAttribute("data-email", v.email || "");
  return div;
}

function getItemDescription(itemClass, inheritedLocale) {
  /*
   * itemClass may be "winner, "alias", "fallback", "fallback_code", "fallback_root", or "loser".
   *  See getPClass in DataPage.java.*
   */
  if (itemClass === "fallback") {
    const locName = cldrLoad.getLocaleName(inheritedLocale);
    return cldrText.sub("item_description_fallback", [locName]);
  } else if (itemClass === "alias") {
    if (inheritedLocale === cldrStatus.getCurrentLocale()) {
      return cldrText.get("item_description_alias_same_locale");
    } else {
      const locName = cldrLoad.getLocaleName(inheritedLocale);
      return cldrText.sub("item_description_alias_diff_locale", [locName]);
    }
  } else {
    // Strings produced here, used as keys for cldrText.get(), may include:
    // "item_description_winner", "item_description_fallback_code", "item_description_fallback_root", "item_description_loser".
    return cldrText.get("item_description_" + itemClass);
  }
}

function clearCachesAndReload() {
  cldrForumPanel.clearCache();
  cldrSideways.clearCache();
  cldrLoad.reloadV();
}

export {
  clearCachesAndReload,
  closePanel,
  initialize,
  panelShouldBeShown,
  show,
  showMessage,
  showRowObjFunc,
  updateRowVoteInfo,
};

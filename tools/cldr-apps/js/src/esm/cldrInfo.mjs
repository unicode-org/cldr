/*
 * cldrInfo: encapsulate Survey Tool "Info Panel" (right sidebar) functions
 */
import * as cldrDeferHelp from "./cldrDeferHelp.mjs";
import * as cldrDom from "./cldrDom.mjs";
import * as cldrEvent from "./cldrEvent.mjs";
import * as cldrForumPanel from "./cldrForumPanel.mjs";
import * as cldrLoad from "./cldrLoad.mjs";
import * as cldrSideways from "./cldrSideways.mjs";
import * as cldrStatus from "./cldrStatus.mjs";
import * as cldrSurvey from "./cldrSurvey.mjs";
import * as cldrTable from "./cldrTable.mjs";
import * as cldrText from "./cldrText.mjs";
import * as cldrUserLevels from "./cldrUserLevels.mjs";
import * as cldrVote from "./cldrVote.mjs";

import InfoPanel from "../views/InfoPanel.vue";

import { createCldrApp } from "../cldrVueRouter.mjs";

let containerId = null;
let neighborId = null;
let buttonClass = null;

// Start with panel closed; it will get opened when a Page is chosen
let panelVisible = false;
let panelVisibleForPageView = true;

let unShow = null;
let lastShown = null;

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
    const fragment = document.createDocumentFragment();
    createCldrApp(InfoPanel).mount(fragment); // returns App object "wrapper", currently not saved
    const containerEl = document.getElementById(containerId);
    const vueEl = document.createElement("section");
    containerEl.appendChild(vueEl);
    vueEl.replaceWith(fragment);
    insertLegacyElement(containerEl);
  } catch (e) {
    console.error("Error loading InfoPanel vue " + e.message + " / " + e.name);
    notification.error({
      message: `${e.name} while loading InfoPanel.vue`,
      description: `${e.message}`,
      duration: 0,
    });
  }
}

/**
 * Create an element to display the Info Panel contents.
 *
 * For compatibility with legacy Survey Tool code, this is not a Vue component.
 * The legacy code involving showRowObjFunc, etc., does extensive direct DOM manipulation.
 * Ideally, eventually Vue components will be used for the entire Info Panel.
 *
 * @param {Element} containerEl the element whose new child will be created
 */
function insertLegacyElement(containerEl) {
  const nonVueEl = document.createElement("section");
  nonVueEl.className = "sidebyside-scrollable";
  nonVueEl.id = "itemInfo";
  containerEl.appendChild(nonVueEl);
}

function openPanel() {
  if (!panelVisible) {
    panelVisible = true;
    openOrClosePanel();
  }
}

function closePanel() {
  if (panelVisible) {
    panelVisible = false;
    openOrClosePanel();
  }
}

function openOrClosePanel() {
  setPanelAndNeighborStyles();
  rememberPanelVisibilityIfPageView();
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
 * Remember whether the Info Panel should be visible when the
 * Page table is displayed (rather than a "special").
 *
 * If there is no current "special", the main Page table view is visible,
 * and the Info Panel visibility state corresponds to the Page table view.
 *
 * Call this after setting panelVisible to true/false.
 */
function rememberPanelVisibilityIfPageView() {
  if (!cldrStatus.getCurrentSpecial()) {
    panelVisibleForPageView = panelVisible;
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

/**
 * Make the object "theObj" cause the info window to show when clicked.
 *
 * @param {String} str the string to display
 * @param {Node} tr the TR element that is clicked
 * @param {Node} theObj to listen to, a.k.a. "hideIfLast"
 * @param {Function} fn the draw function
 */
function listen(str, tr, theObj, fn) {
  cldrDom.listenFor(theObj, "click", function (e) {
    if (panelShouldBeShown()) {
      show(str, tr, theObj /* hideIfLast */, fn);
    }
    cldrEvent.stopPropagation(e);
    return false;
  });
}

function showMessage(str) {
  if (panelShouldBeShown()) {
    show(str, null, null, null);
  }
}

function showWithRow(str, tr) {
  if (panelShouldBeShown()) {
    show(str, tr, null, null);
  }
}

function showRowObjFunc(tr, hideIfLast, fn) {
  if (panelShouldBeShown()) {
    show(null, tr, hideIfLast, fn);
  }
}

/**
 * Should the Info Panel be shown?
 *
 * @returns true if the Info Panel should be shown, else false
 *
 * This is only called when one of the show... functions is called.
 * In all special views, return true (if the special never calls us, the panel
 * will remain hidden).
 * In the Page view (not special), rely on the setting of panelVisibleForPageView.
 */
function panelShouldBeShown() {
  return panelVisibleForPageView || Boolean(cldrStatus.getCurrentSpecial());
}

/**
 * Display the given information in the Info Panel
 *
 * Open the panel if it's not already open
 *
 * @param {String} str the string to show at the top
 * @param {Node} tr the <TR> of the row
 * @param {Object} hideIfLast mysterious parameter
 * @param {Function} fn the draw function
 */
function show(str, tr, hideIfLast, fn) {
  openPanel();
  if (unShow) {
    unShow();
    unShow = null;
  }

  if (tr && tr.sethash) {
    cldrLoad.updateCurrentId(tr.sethash);
  }
  setLastShown(hideIfLast);

  /*
   * This is the temporary fragment used for the
   * "info panel" contents.
   */
  var fragment = document.createDocumentFragment();

  if (tr && tr.theRow) {
    const { theRow } = tr;
    const {
      helpHtml,
      rdf,
      placeholderStatus,
      placeholderInfo,
      translationHint,
    } = theRow;
    if (helpHtml || rdf || translationHint) {
      cldrDeferHelp.addDeferredHelpTo(fragment, helpHtml, rdf, translationHint);
    }
    if (placeholderStatus !== "DISALLOWED") {
      // Hide the placeholder status if DISALLOWED
      cldrDeferHelp.addPlaceholderHelp(
        fragment,
        placeholderStatus,
        placeholderInfo
      );
    }
  }

  if (str) {
    // If a simple string, clone the string
    var div2 = document.createElement("div");
    div2.innerHTML = str;
    fragment.appendChild(div2);
  }
  // If a generator fn (common case), call it.
  if (fn) {
    unShow = fn(fragment);
  }

  var theVoteinfo = null;
  if (tr && tr.voteDiv) {
    theVoteinfo = tr.voteDiv;
  }
  if (theVoteinfo) {
    fragment.appendChild(theVoteinfo.cloneNode(true));
  }
  if (tr && tr.ticketLink) {
    fragment.appendChild(tr.ticketLink.cloneNode(true));
  }
  if (tr) {
    cldrSideways.loadMenu(fragment, tr.xpstrid); // regional variants (sibling locales)
  }
  if (tr?.theRow && !cldrStatus.isVisitor()) {
    cldrForumPanel.loadInfo(fragment, tr, tr.theRow);
  }
  if (tr && tr.theRow && tr.theRow.xpath) {
    fragment.appendChild(
      cldrDom.clickToSelect(
        cldrDom.createChunk(tr.theRow.xpath, "div", "xpath")
      )
    );
  }
  const pucontent = document.getElementById("itemInfo");
  if (!pucontent) {
    console.log("itemInfo not found in show!");
    return;
  }

  // Now, copy or append the 'fragment' to the
  // appropriate spot. This depends on how we were called.
  if (tr) {
    cldrDom.removeAllChildNodes(pucontent);
    pucontent.appendChild(fragment);
  } else {
    // show, for example, dataPageInitialGuidance in Info Panel
    var clone = fragment.cloneNode(true);
    cldrDom.removeAllChildNodes(pucontent);
    pucontent.appendChild(clone);
  }
  fragment = null;

  // for the voter
  $(".voteInfo_voterInfo").hover(
    function () {
      var email = $(this).data("email").replace(" (at) ", "@");
      if (email !== "") {
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

function setLastShown(obj) {
  if (lastShown && obj != lastShown) {
    cldrDom.removeClass(lastShown, "pu-select");
    const partr = parentOfType("TR", lastShown);
    if (partr) {
      cldrDom.removeClass(partr, "selectShow");
    }
  }
  if (obj) {
    cldrDom.addClass(obj, "pu-select");
    const partr = parentOfType("TR", obj);
    if (partr) {
      cldrDom.addClass(partr, "selectShow");
    }
  }
  lastShown = obj;
}

function reset() {
  lastShown = null;
}

function parentOfType(tag, obj) {
  if (!obj) {
    return null;
  }
  if (obj.nodeName === tag) {
    return obj;
  }
  return parentOfType(tag, obj.parentElement);
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

  //add the 'explain' icon
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
  tr.voteDiv.appendChild(showTranscriptLink);

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
    if (theRow.voteVhash !== theRow.winningVhash && theRow.canFlagOnLosing) {
      if (!theRow.rowFlagged) {
        cldrSurvey.addIcon(tr.voteDiv, "i-stop");
        tr.voteDiv.appendChild(
          cldrDom.createChunk(
            cldrText.sub("mustflag_explain_msg", {}),
            "p",
            "helpContent"
          )
        );
      } else {
        cldrSurvey.addIcon(tr.voteDiv, "i-flag");
        tr.voteDiv.appendChild(
          cldrDom.createChunk(cldrText.get("flag_desc", "p", "helpContent"))
        );
      }
    }
  }
  if (!theRow.rowFlagged && theRow.canFlagOnLosing) {
    cldrSurvey.addIcon(tr.voteDiv, "i-flag-d");
    tr.voteDiv.appendChild(
      cldrDom.createChunk(cldrText.get("flag_d_desc", "p", "helpContent"))
    );
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
        value,
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
  if (theRow.voteTranscript) {
    const transcriptBox = cldrDom.createChunk(
      "",
      "div",
      "transcript-container"
    );
    const hideTranscriptIcon = cldrDom.createChunk(
      "",
      "i",
      "glyphicon glyphicon-remove-sign show-transcript"
    );
    const hideTranscriptLink = cldrDom.createChunk("", "a", "show-transcript");
    hideTranscriptLink.setAttribute(
      "href",
      "javascript:window.cldrBundle.toggleTranscript()"
    );
    hideTranscriptLink.appendChild(hideTranscriptIcon);
    transcriptBox.appendChild(hideTranscriptLink);
    const transcriptText = cldrDom.createChunk(
      theRow.voteTranscript,
      "pre",
      "transcript-text"
    );
    transcriptBox.appendChild(transcriptText);
    const transcriptNote = cldrDom.createChunk(
      cldrText.get("transcript_note"),
      "p",
      "alert alert-warning"
    );
    transcriptBox.appendChild(transcriptNote);
    tr.voteDiv.appendChild(transcriptBox);
  }
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

/**
 * Return a function that will show info for the given item in the Info Panel.
 *
 * @param theRow the data row
 * @param item the candidate item
 * @returns the function
 *
 * Called only by cldrTable.addVitem.
 */
function showItemInfoFn(theRow, item) {
  return function (td) {
    var h3 = document.createElement("div");
    var displayValue = item.value;
    if (item.value === cldrSurvey.INHERITANCE_MARKER) {
      displayValue = theRow.inheritedDisplayValue;
    }

    cldrVote.appendItem(h3, displayValue, item.pClass);
    h3.className = "span";
    td.appendChild(h3);

    if (item.value) {
      /*
       * Strings produced here, used as keys for cldrText.sub(), may include:
       *  "pClass_winner", "pClass_alias", "pClass_fallback", "pClass_fallback_code", "pClass_fallback_root", "pClass_loser".
       *  See getPClass in DataPage.java.
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

    if (item.value === cldrSurvey.INHERITANCE_MARKER) {
      addJumpToOriginal(theRow, h3);
    }

    var newDiv = document.createElement("div");
    td.appendChild(newDiv);

    if (item.tests) {
      newDiv.innerHTML = cldrSurvey.testsToHtml(item.tests);
    } else {
      newDiv.innerHTML = "<i>no tests</i>";
    }

    if (item.example) {
      cldrTable.appendExample(td, item.example);
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

function clearCachesAndReload() {
  cldrForumPanel.clearCache();
  cldrSideways.clearCache();
  cldrLoad.reloadV();
}

export {
  clearCachesAndReload,
  closePanel,
  initialize,
  listen,
  openPanel,
  reset,
  showItemInfoFn,
  showMessage,
  showRowObjFunc,
  showWithRow,
  updateRowVoteInfo,
};

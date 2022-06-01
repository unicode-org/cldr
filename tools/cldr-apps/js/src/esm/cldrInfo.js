/*
 * cldrInfo: encapsulate Survey Tool "Info Panel" (right sidebar) functions
 */
import * as cldrDeferHelp from "./cldrDeferHelp.js";
import * as cldrDom from "./cldrDom.js";
import * as cldrEvent from "./cldrEvent.js";
import * as cldrForumPanel from "./cldrForumPanel.js";
import * as cldrLoad from "./cldrLoad.js";
import * as cldrStatus from "./cldrStatus.js";
import * as cldrSurvey from "./cldrSurvey.js";
import * as cldrTable from "./cldrTable.js";
import * as cldrText from "./cldrText.js";
import * as cldrVote from "./cldrVote.js";

let unShow = null;
let lastShown = null;

/**
 * Make the object "theObj" cause the info window to show when clicked.
 *
 * @param {String} str
 * @param {Node} tr the TR element that is clicked
 * @param {Node} theObj to listen to
 * @param {Function} fn the draw function
 */
function listen(str, tr, theObj, fn) {
  cldrDom.listenFor(theObj, "click", function (e) {
    show(str, tr, theObj /* hideIfLast */, fn);
    cldrEvent.stopPropagation(e);
    return false;
  });
}

function showNothing() {
  show(null, null, null, null);
}

function showMessage(str) {
  show(str, null, null, null);
}

function showWithRow(str, tr) {
  show(str, tr, null, null);
}

function showRowObjFunc(tr, hideIfLast, fn) {
  show(null, tr, hideIfLast, fn);
}

/**
 * Display the right-hand "info" panel.
 *
 * @param {String} str the string to show at the top
 * @param {Node} tr the <TR> of the row
 * @param {Object} hideIfLast
 * @param {Function} fn
 */
function show(str, tr, hideIfLast, fn) {
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
    const { helpHtml, rdf, placeholderStatus, placeholderInfo } = theRow;
    if (helpHtml || rdf) {
      cldrDeferHelp.addDeferredHelpTo(fragment, helpHtml, rdf);
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

  // forum stuff
  if (tr && tr.forumDiv) {
    /*
     * The name forumDivClone is a reminder that forumDivClone !== tr.forumDiv.
     * TODO: explain the reason for using cloneNode here, rather than using
     * tr.forumDiv directly. Would it work as well to set tr.forumDiv = forumDivClone,
     * after cloning?
     */
    var forumDivClone = tr.forumDiv.cloneNode(true);
    cldrForumPanel.loadInfo(fragment, forumDivClone, tr); // give a chance to update anything else
    fragment.appendChild(forumDivClone);
  }

  if (tr && tr.theRow && tr.theRow.xpath) {
    fragment.appendChild(
      cldrDom.clickToSelect(
        cldrDom.createChunk(tr.theRow.xpath, "div", "xpath")
      )
    );
  }
  var pucontent = document.getElementById("itemInfo");
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
  var vr = theRow.voteResolver;
  tr.voteDiv = document.createElement("div");
  tr.voteDiv.className = "voteDiv";
  const surveyUser = cldrStatus.getSurveyUser();
  if (theRow.voteVhash && theRow.voteVhash !== "" && surveyUser) {
    var voteForItem = theRow.items[theRow.voteVhash];
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
  var n = 0;
  while (n < vr.value_vote.length) {
    var value = vr.value_vote[n++];
    var vote = vr.value_vote[n++];
    if (
      value == null /* TODO: impossible? */ ||
      value === cldrTable.NO_WINNING_VALUE
    ) {
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
        cldrVote.appendItem(valdiv, theRow.inheritedValue, item.pClass);
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
      if (value === theRow.inheritedValue) {
        valdiv.appendChild(
          cldrDom.createChunk(
            cldrText.get("voteInfo_votesForSpecificValue"),
            "p"
          )
        );
      }
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
      item.votes[Object.keys(item.votes)[0]].level === "anonymous";
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
    var vrRaw = {};
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
      } else {
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
      displayValue = theRow.inheritedValue;
    }

    var span = cldrVote.appendItem(h3, displayValue, item.pClass);
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

export {
  listen,
  reset,
  showItemInfoFn,
  showMessage,
  showNothing,
  showRowObjFunc,
  showWithRow,
  updateRowVoteInfo,
};

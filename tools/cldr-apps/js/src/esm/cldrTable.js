/*
 * cldrTable: encapsulate code related to the main Survey Tool html table,
 * whose rows describe xpaths.
 *
 * Functions for populating the main table in the vetting page:
 * 		insertRows
 * 		updateRow
 * 		refreshSingleRow
 */
import * as cldrAjax from "./cldrAjax.js";
import * as cldrCoverage from "./cldrCoverage.js";
import * as cldrDom from "./cldrDom.js";
import * as cldrEvent from "./cldrEvent.js";
import * as cldrForumPanel from "./cldrForumPanel.js";
import * as cldrGui from "./cldrGui.js";
import * as cldrInfo from "./cldrInfo.js";
import * as cldrLoad from "./cldrLoad.js";
import * as cldrProgress from "./cldrProgress.js";
import * as cldrStatus from "./cldrStatus.js";
import * as cldrSurvey from "./cldrSurvey.js";
import * as cldrText from "./cldrText.js";
import * as cldrVote from "./cldrVote.js";
import * as cldrXPathUtils from "./cldrXpathUtils.js";

const CLDR_TABLE_DEBUG = false;

const TABLE_USES_NEW_API = false;

/*
 * ALWAYS_REMOVE_ALL_CHILD_NODES and NEVER_REUSE_TABLE should both be false for efficiency,
 * but if necessary they can be made true to revert to old less efficient behavior.
 * Reference: https://unicode.org/cldr/trac/ticket/11571
 */
const ALWAYS_REMOVE_ALL_CHILD_NODES = false;
const NEVER_REUSE_TABLE = false;

/*
 * NO_WINNING_VALUE indicates the server delivered path data without a valid winning value.
 * It must match NO_WINNING_VALUE in the server Java code.
 */
const NO_WINNING_VALUE = "no-winning-value";

/**
 * Prepare rows to be inserted into the table
 *
 * @param theDiv the division (typically or always? with id='DynamicDataSection') that contains, or will contain, the table
 * @param xpath = json.pageId; e.g., "Alphabetic_Information"
 * @param session the session id; e.g., "DEF67BCAAFED4332EBE742C05A8D1161"
 * @param json the json received from the server; including (among much else):
 * 			json.locale, e.g., "aa"
 *  		json.section.rows, with info for each row
 */
function insertRows(theDiv, xpath, session, json) {
  cldrProgress.updatePageCompletion(json.canModify ? json.section.rows : null);

  if (ALWAYS_REMOVE_ALL_CHILD_NODES) {
    cldrDom.removeAllChildNodes(theDiv); // maybe superfluous if always recreate the table, and wrong if we don't always recreate the table
  }

  $(".warnText").remove(); // remove any pre-existing "special notes", before insertLocaleSpecialNote
  cldrLoad.insertLocaleSpecialNote(theDiv);

  var theTable = null;
  const reuseTable =
    !NEVER_REUSE_TABLE &&
    theDiv.theTable &&
    theDiv.theTable.json &&
    tablesAreCompatible(json, theDiv.theTable.json);
  if (reuseTable) {
    /*
     * Re-use the old table, just update contents of individual cells
     */
    // console.log('🦋🦋🦋 re-use table, ' + Object.keys(json.section.rows).length + ' rows');
    theTable = theDiv.theTable;
  } else {
    /*
     * Re-create the table from scratch
     */
    // console.log('🦞🦞🦞 make new table, ' + Object.keys(json.section.rows).length + ' rows');
    theTable = cldrSurvey.cloneLocalizeAnon(
      document.getElementById("proto-datatable")
    );
    theTable.className += " vetting-page";

    /*
     * Give our table the unique id, 'vetting-table'. This is needed by the test SurveyDriverVettingTable.
     * Otherwise its id would be 'null' (the string 'null', not null!), and there is risk of confusion
     * with other table such as 'proto-datarow'.
     */
    theTable.id = "vetting-table";
    /*
     * This code seems to merge parts of two prototype tables,
     * in a complicated way. The two tables are both in hidden.html:
     * (1) a table with no id, which contains tr id='proto-datarow', which in turn contains multiple td;
     * (2) table id='proto-datatable', which contains multiple th.
     * The result of the merger is theTable.toAdd, which is eventually used as
     * a prototype for each row that gets added to the real (not hidden) table.
     * TODO: simplify.
     */
    cldrSurvey.localizeFlyover(theTable); // Replace titles starting with $ with strings from cldrText
    const headChildren = cldrSurvey.getTagChildren(
      theTable.getElementsByTagName("tr")[0]
    );
    var toAdd = document.getElementById("proto-datarow"); // loaded from "hidden.html", which see.
    var rowChildren = cldrSurvey.getTagChildren(toAdd);
    for (var c in rowChildren) {
      rowChildren[c].title = headChildren[c].title;
    }
    theTable.toAdd = toAdd;
  }
  cldrCoverage.updateCoverage(theDiv);
  if (!json.canModify) {
    /*
     * Remove the "Abstain" column from the header since user can't modify.
     */
    const headAbstain = theTable.querySelector("th.d-no");
    if (headAbstain) {
      cldrDom.setDisplayed(headAbstain, false);
    }
  }
  theDiv.theTable = theTable;
  theTable.theDiv = theDiv;

  theTable.json = json;
  theTable.xpath = xpath;
  theTable.session = session;

  if (!theTable.curSortMode) {
    theTable.curSortMode = theTable.json.displaySets["default"]; // typically (always?) "ph"
    // hack - choose one of these
    /*
     * TODO: is this no longer used? Cf. PREF_SORTMODE_CODE_CALENDAR and PREF_SORTMODE_METAZONE in SurveyMain.java
     * Cf. identical code in review.js
     */
    if (theTable.json.displaySets.codecal) {
      theTable.curSortMode = "codecal";
    } else if (theTable.json.displaySets.metazon) {
      theTable.curSortMode = "metazon";
    }
  }
  if (!reuseTable || !theDiv.contains(theTable)) {
    // reference: CLDR-13727 and CLDR-13885
    theDiv.appendChild(theTable);
  }
  insertRowsIntoTbody(theTable, reuseTable);
  cldrSurvey.hideLoader();
}

/**
 * Are the new (to-be-built) table and old (already-built) table compatible, in the
 * sense that we can re-use the old table structure, just replacing the contents of
 * individual cells, rather than rebuilding the table from scratch?
 *
 * @param json1 the json for one table
 * @param json2 the json for the other table
 * @returns true if compatible, else false
 *
 * Reference: https://unicode.org/cldr/trac/ticket/11571
 */
function tablesAreCompatible(json1, json2) {
  if (
    json1.section &&
    json2.section &&
    json1.pageId === json2.pageId &&
    json1.locale === json2.locale &&
    json1.canModify === json2.canModify &&
    Object.keys(json1.section.rows).length ===
      Object.keys(json2.section.rows).length
  ) {
    return true;
  }
  return false;
}

/**
 * Insert rows into the table
 *
 * @param theTable the table in which to insert the rows
 * @param reuseTable boolean, true if theTable already has rows and we're updating them,
 *                            false if we need to insert new rows
 *
 * Called by insertRows only.
 */
function insertRowsIntoTbody(theTable, reuseTable) {
  var tbody = theTable.getElementsByTagName("tbody")[0];
  var theRows = theTable.json.section.rows;
  var toAdd = theTable.toAdd;
  var parRow = document.getElementById("proto-parrow");

  if (ALWAYS_REMOVE_ALL_CHILD_NODES) {
    cldrDom.removeAllChildNodes(tbody);
  }

  var theSort = theTable.json.displaySets[theTable.curSortMode]; // typically (always?) curSortMode = "ph"
  var partitions = theSort.partitions;
  var rowList = theSort.rows;
  var partitionList = Object.keys(partitions);
  var curPartition = null;
  for (var i in rowList) {
    var k = rowList[i];
    var theRow = theRows[k];
    var dir = theRow.dir;
    cldrSurvey.setOverrideDir(dir != null ? dir : null);
    /*
     * Don't regenerate the headings if we're re-using an existing table.
     */
    if (!reuseTable) {
      var newPartition = findPartition(
        partitions,
        partitionList,
        curPartition,
        i
      );

      if (newPartition != curPartition) {
        if (newPartition.name != "") {
          var newPar = cldrSurvey.cloneAnon(parRow);
          var newTd = cldrSurvey.getTagChildren(newPar);
          var newHeading = cldrSurvey.getTagChildren(newTd[0]);
          newHeading[0].innerHTML = newPartition.name;
          newHeading[0].id = newPartition.name;
          tbody.appendChild(newPar);
          newPar.origClass = newPar.className;
          newPartition.tr = newPar; // heading
        }
        curPartition = newPartition;
      }

      var theRowCov = parseInt(theRow.coverageValue);
      if (!newPartition.minCoverage || newPartition.minCoverage > theRowCov) {
        newPartition.minCoverage = theRowCov;
        if (newPartition.tr) {
          // only set coverage of the header if there's a header
          newPartition.tr.className =
            newPartition.origClass + " cov" + newPartition.minCoverage;
        }
      }
    }

    /*
     * If tbody already contains tr with this id, re-use it
     * Cf. in updateRow: tr.id = "r@"+tr.xpstrid;
     */
    var tr = reuseTable ? document.getElementById("r@" + theRow.xpstrid) : null;
    if (!tr) {
      tr = cldrSurvey.cloneAnon(toAdd);
      tbody.appendChild(tr);
      // console.log("🦞 make new table row for " + theRow.xpstrid);
    } else {
      // console.log("🦋 re-use table row for " + theRow.xpstrid);
    }
    tr.rowHash = k;
    tr.theTable = theTable;

    /*
     * Update the xpath map, unless re-using the table. If we're re-using the table, then
     * curPartition.name isn't defined, and anyway xpathMap shouldn't need changing.
     */
    if (!reuseTable) {
      const xpathMap = cldrSurvey.getXpathMap();
      xpathMap.put({
        id: theRow.xpathId,
        hex: theRow.xpstrid,
        path: theRow.xpath,
        ph: {
          section: cldrStatus.getCurrentSection(), // Section: Timezones
          page: cldrStatus.getCurrentPage(), // Page: SEAsia ( id, not name )
          header: curPartition.name, // Header: Borneo
          code: theRow.code, // Code: standard-long
        },
      });
    }

    /*
     * Update the row's contents, unless it has an individual update pending.
     * We're working with a multiple-row response from the server, and should not use
     * this response to update any row(s) in which the user has just voted and for which
     * we're still waiting for single-row response(s).
     */
    if (tr.className === "tr_checking1" || tr.className === "tr_checking2") {
      // console.log("Skipping updateRow for tr.className === " + tr.className);
    } else {
      /*
       * TODO: for performance, if reuseTable and new data matches old data for this row, leave the DOM as-is.
       * Figure out an efficient way to test whether this row's data has changed.
       */
      updateRow(tr, theRow);
    }
  }
  // downloadObjectAsHtml(tbody);
  // downloadObjectAsJson(tbody);
  // downloadObjectAsJson(theTable);
}

/**
 * Find the specified partition.
 *
 * @param partitions
 * @param partitionList
 * @param curPartition
 * @param i
 * @returns the partition, or null
 */
function findPartition(partitions, partitionList, curPartition, i) {
  if (curPartition && i >= curPartition.start && i < curPartition.limit) {
    return curPartition;
  }
  for (var j in partitionList) {
    var p = partitions[j];
    if (i >= p.start && i < p.limit) {
      return p;
    }
  }
  return null;
}

/**
 * Reload a specific row
 *
 * Called only by load handler of cldrVote.handleWiredClick
 */
function refreshSingleRow(tr, theRow, onSuccess, onFailure) {
  cldrSurvey.showLoader(cldrText.get("loadingOneRow"));

  const xhrArgs = {
    url: getSingleRowUrl(tr, theRow),
    handleAs: "json",
    load: closureLoadHandler,
    error: closureErrHandler,
    timeout: cldrAjax.mediumTimeout(),
  };
  cldrAjax.sendXhr(xhrArgs);

  function closureLoadHandler(json) {
    singleRowLoadHandler(json, tr, theRow, onSuccess, onFailure);
  }

  function closureErrHandler(err) {
    singleRowErrHandler(err, tr, onFailure);
  }
}

function singleRowLoadHandler(json, tr, theRow, onSuccess, onFailure) {
  try {
    if (json.section.rows[tr.rowHash]) {
      theRow = json.section.rows[tr.rowHash];
      tr.theTable.json.section.rows[tr.rowHash] = theRow;
      updateRow(tr, theRow);
      cldrSurvey.hideLoader();
      onSuccess(theRow);
      cldrGui.updateDashboardRow(json);
      cldrInfo.showRowObjFunc(tr, tr.proposedcell, tr.proposedcell.showFn);
      cldrProgress.updateCompletionOneVote(theRow.hasVoted);
      cldrGui.refreshCounterVetting();
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
}

function singleRowErrHandler(err, tr, onFailure) {
  console.log("Error: " + err);
  tr.className = "ferrbox";
  tr.innerHTML =
    "Error while  loading: <div style='border: 1px solid red;'>" +
    err +
    "</div>";
  onFailure("err", err);
}

function getSingleRowUrl(tr, theRow) {
  if (TABLE_USES_NEW_API) {
    const loc = cldrStatus.getCurrentLocale();
    const xpath = tr.rowHash;
    const api = "voting/" + loc + "/row/" + xpath;
    let p = null;
    if (cldrGui.dashboardIsVisible()) {
      p = new URLSearchParams();
      p.append("dashboard", "true");
    }
    return cldrAjax.makeApiUrl(api, p);
  }
  const p = new URLSearchParams();
  p.append("what", "getrow"); // cf. WHAT_GETROW in SurveyAjax.java
  p.append("_", cldrStatus.getCurrentLocale());
  p.append("xpath", theRow.xpathId);
  p.append("fhash", tr.rowHash);
  p.append("automatic", "t");
  if (cldrGui.dashboardIsVisible()) {
    p.append("dashboard", "true");
  }
  p.append("s", cldrStatus.getSessionId());
  p.append("cacheKill", cldrSurvey.cacheBuster());
  return cldrAjax.makeUrl(p);
}

/**
 * Update one row using data received from server.
 *
 * @param tr the table row
 * @param theRow the data for the row
 *
 * Cells (columns) in each row:
 * Code    English    Abstain    A    Winning    Add    Others
 */
function updateRow(tr, theRow) {
  if (!tr || !theRow) {
    return;
  }
  const rowChecksum = cldrChecksum(JSON.stringify(theRow));
  if (tr.checksum !== undefined && rowChecksum === tr.checksum) {
    return; // already up to date
  }
  tr.checksum = rowChecksum;
  tr.theRow = theRow;
  checkRowConsistency(theRow);
  reallyUpdateRow(tr, theRow);
}

/**
 * Get a checksum for the given string
 *
 * @param s the string
 * @return the checksum
 */
function cldrChecksum(s) {
  let checksum = 0;
  for (let i = 0; i < s.length; i++) {
    checksum = (checksum << 5) - checksum + s.charCodeAt(i);
    checksum |= 0; // convert possible float to integer
  }
  return checksum;
}

/**
 * Update one row using data received from server.
 *
 * @param tr the table row
 * @param theRow the data for the row
 */
function reallyUpdateRow(tr, theRow) {
  /*
   * For convenience, set up a hash for reverse mapping from rawValue to item.
   */
  tr.rawValueToItem = {}; // hash:  string value to item (which has a div)
  for (var k in theRow.items) {
    var item = theRow.items[k];
    if (item.value) {
      tr.rawValueToItem[item.rawValue] = item; // back link by value
    }
  }

  /*
   * Update the vote info.
   */
  if (theRow.voteResolver) {
    cldrInfo.updateRowVoteInfo(tr, theRow);
  } else {
    tr.voteDiv = null;
  }

  tr.statusAction = cldrSurvey.parseStatusAction(theRow.statusAction);
  tr.canModify = tr.theTable.json.canModify && tr.statusAction.vote;
  tr.ticketOnly = tr.theTable.json.canModify && tr.statusAction.ticket;
  tr.canChange = tr.canModify && tr.statusAction.change;

  if (!theRow.xpathId) {
    tr.innerHTML = "<td><i>ERROR: missing row</i></td>";
    return;
  }
  if (!tr.xpstrid) {
    tr.xpathId = theRow.xpathId;
    tr.xpstrid = theRow.xpstrid;
    if (tr.xpstrid) {
      /*
       * TODO: usage of '@' in tr.id appears to be problematic for jQuery:
       * if we try to use selectors like $('#' + tr.id), we get
       * "Syntax error, unrecognized expression: #r@f3d4397b739b287"
       * Is there a good reason to keep '@'?
       */
      tr.id = "r@" + tr.xpstrid;
      tr.sethash = tr.xpstrid;
      // const test = $('#' + tr.id);
    }
  }

  var protoButton = null; // no voting at all, unless tr.canModify
  if (tr.canModify) {
    protoButton = document.getElementById("proto-button");
  }

  const statusCell = tr.querySelector(".statuscell");
  const abstainCell = tr.querySelector(".nocell");
  const codeCell = tr.querySelector(".codecell");
  const comparisonCell = tr.querySelector(".comparisoncell");
  const proposedCell = tr.querySelector(".proposedcell");
  const otherCell = tr.querySelector(".othercell");
  const addCell = tr.canModify ? tr.querySelector(".addcell") : null;

  /*
   * "Add" button, potentially used by updateRowOthersCell and/or by otherCell or addCell
   */
  const formAdd = document.createElement("form");

  /*
   * Update the "status cell", a.k.a. the "A" column.
   */
  if (statusCell) {
    updateRowStatusCell(tr, theRow, statusCell);
  }

  /*
   * Update part of the "no cell", cf. updateRowNoAbstainCell; should this code be moved to updateRowNoAbstainCell?
   */
  if (abstainCell) {
    if (theRow.hasVoted) {
      abstainCell.title = cldrText.get("voTrue");
      abstainCell.className = "d-no-vo-true nocell";
    } else {
      abstainCell.title = cldrText.get("voFalse");
      abstainCell.className = "d-no-vo-false nocell";
    }
  }

  /*
   * Assemble the "code cell", a.k.a. the "Code" column.
   */
  if (codeCell) {
    updateRowCodeCell(tr, theRow, codeCell);
  }

  /*
   * Set up the "comparison cell", a.k.a. the "English" column.
   */
  if (comparisonCell && !comparisonCell.isSetup) {
    updateRowEnglishComparisonCell(tr, theRow, comparisonCell);
  }

  /*
   * Set up the "proposed cell", a.k.a. the "Winning" column.
   *
   * Column headings are: Code    English    Abstain    A    Winning    Add    Others
   * TODO: are we going out of order here, from English to Winning, skipping Abstain and A?
   */
  if (proposedCell) {
    updateRowProposedWinningCell(tr, theRow, proposedCell, protoButton);
  }

  /*
   * Set up the "other cell", a.k.a. the "Others" column.
   */
  if (otherCell) {
    updateRowOthersCell(tr, theRow, otherCell, protoButton, formAdd);
  }

  /*
   * If the user can make changes, add "+" button for adding new candidate item.
   */
  if (tr.canChange) {
    if (addCell) {
      cldrDom.removeAllChildNodes(addCell);
      addCell.appendChild(formAdd);
    }
  }

  /*
   * Set up the "no cell", a.k.a. the "Abstain" column.
   * If the user can make changes, add an "abstain" button;
   * else, possibly add a ticket link, or else hide the column.
   */
  if (abstainCell) {
    updateRowNoAbstainCell(tr, theRow, abstainCell, proposedCell, protoButton);
  }

  /*
   * Set className for this row to "vother" and "cov..." based on the coverage value.
   * Elsewhere className can get values including "ferrbox", "tr_err", "tr_checking2".
   */
  tr.className = "vother cov" + theRow.coverageValue;

  /*
   * Show the current ID.
   * TODO: explain.
   */
  const curId = cldrStatus.getCurrentId();
  if (curId !== "" && curId === tr.id) {
    cldrLoad.showCurrentId(); // refresh again - to get the updated voting status.
  }
}

/**
 * Check whether the data for this row is consistent, and report to console error
 * if it isn't.
 *
 * @param theRow the data from the server for this row
 *
 * Called by updateRow.
 *
 * Inconsistencies should primarily be detected/reported/fixed on server (DataSection.java)
 * rather than here on the client, but better late than never, and these checks may be useful
 * for automated testing with WebDriver.
 */
function checkRowConsistency(theRow) {
  if (!theRow) {
    console.error("theRow is null or undefined in checkRowConsistency");
    return;
  }
  if (!theRow.winningVhash) {
    /*
     * The server is responsible for ensuring that a winning item is present, or using
     * the placeholder NO_WINNING_VALUE, which is not null.
     */
    console.error("For " + theRow.xpstrid + " - there is no winningVhash");
  } else if (!theRow.items) {
    console.error("For " + theRow.xpstrid + " - there are no items");
  } else if (!theRow.items[theRow.winningVhash]) {
    console.error(
      "For " + theRow.xpstrid + " - there is winningVhash but no item for it"
    );
  }

  for (var k in theRow.items) {
    var item = theRow.items[k];
    if (item.value === cldrSurvey.INHERITANCE_MARKER) {
      if (!theRow.inheritedValue) {
        /*
         * In earlier implementation, essentially the same error was reported as "... there is no Bailey Target item!").
         */
        if (!cldrXPathUtils.extraPathAllowsNullValue(theRow.xpath)) {
          console.error(
            "For " +
              theRow.xpstrid +
              " - there is INHERITANCE_MARKER without inheritedValue"
          );
        }
      } else if (!theRow.inheritedLocale && !theRow.inheritedXpid) {
        /*
         * It is probably a bug if item.value === cldrSurvey.INHERITANCE_MARKER but theRow.inheritedLocale and
         * theRow.inheritedXpid are both undefined (null on server).
         * This happens with "example C" in
         *     https://unicode.org/cldr/trac/ticket/11299#comment:15
         */
        console.log(
          "For " +
            theRow.xpstrid +
            " - there is INHERITANCE_MARKER without inheritedLocale or inheritedXpid"
        );
      }
    }
  }
}

/**
 * Update the "status cell", a.k.a. the "A" column.
 *
 * @param tr the table row
 * @param theRow the data from the server for this row
 * @param cell the table cell
 */
function updateRowStatusCell(tr, theRow, cell) {
  const statusClass = getRowApprovalStatusClass(theRow);
  cell.className = "d-dr-" + statusClass + " d-dr-status statuscell";

  if (!cell.isSetup) {
    cldrInfo.listen("", tr, cell, null);
    cell.isSetup = true;
  }

  const statusTitle = cldrText.get(statusClass);
  cell.title = cldrText.sub("draftStatus", [statusTitle]);
}

/**
 * On the client only, make further status distinctions when winning value is cldrSurvey.INHERITANCE_MARKER,
 * "inherited-unconfirmed" (red up-arrow icon) and "inherited-provisional" (orange up-arrow icon).
 * Reference: http://unicode.org/cldr/trac/ticket/11103
 *
 * @param theRow the data from the server for this row
 */
function getRowApprovalStatusClass(theRow) {
  var statusClass = theRow.confirmStatus;

  if (theRow.winningValue === cldrSurvey.INHERITANCE_MARKER) {
    if (statusClass === "unconfirmed") {
      statusClass = "inherited-unconfirmed";
    } else if (statusClass === "provisional") {
      statusClass = "inherited-provisional";
    }
  }
  return statusClass;
}

/*
 * Update the "Code" cell (column) of this row
 *
 * @param tr the table row
 * @param theRow the data from the server for this row
 * @param cell the table cell
 *
 * Called by updateRow.
 */
function updateRowCodeCell(tr, theRow, cell) {
  cldrDom.removeAllChildNodes(cell);
  var codeStr = theRow.code;
  if (theRow.coverageValue == 101) {
    codeStr = codeStr + " (optional)";
  }
  cell.appendChild(cldrDom.createChunk(codeStr));
  if (cldrStatus.getSurveyUser()) {
    cell.className = "d-code codecell";
    if (!tr.forumDiv) {
      tr.forumDiv = document.createElement("div");
      tr.forumDiv.className = "forumDiv";
    }
    cldrForumPanel.appendForumStuff(tr, theRow, tr.forumDiv);
  }
  // extra attributes
  if (
    theRow.extraAttributes &&
    Object.keys(theRow.extraAttributes).length > 0
  ) {
    cldrSurvey.appendExtraAttributes(cell, theRow);
  }
  if (CLDR_TABLE_DEBUG) {
    var anch = document.createElement("i");
    anch.className = "anch";
    anch.id = theRow.xpathId;
    cell.appendChild(anch);
    anch.appendChild(document.createTextNode("#"));
    var go = document.createElement("a");
    go.className = "anch-go";
    go.appendChild(document.createTextNode("zoom"));
    go.href =
      window.location.pathname +
      "?_=" +
      cldrStatus.getCurrentLocale() +
      "&x=r_rxt&xp=" +
      theRow.xpathId;
    cell.appendChild(go);
    var js = document.createElement("a");
    js.className = "anch-go";
    js.appendChild(document.createTextNode("{JSON}"));
    js.popParent = tr;
    cldrInfo.listen(JSON.stringify(theRow), tr, js, null);
    cell.appendChild(js);
    cell.appendChild(cldrDom.createChunk(" c=" + theRow.coverageValue));
  }
  if (!cell.isSetup) {
    var xpathStr = "";
    if (CLDR_TABLE_DEBUG) {
      xpathStr = "XPath: " + theRow.xpath;
    }
    cldrInfo.listen(xpathStr, tr, cell, null);
    cell.isSetup = true;
  }
}

/**
 * Update the "comparison cell", a.k.a. the "English" column, of this row
 *
 * @param tr the table row
 * @param theRow the data from the server for this row
 * @param cell the table cell
 *
 * Called by updateRow.
 */
function updateRowEnglishComparisonCell(tr, theRow, cell) {
  if (theRow.displayName) {
    var hintPos = theRow.displayName.indexOf("[translation hint");
    var hasExample = false;
    if (theRow.displayExample) {
      hasExample = true;
    }
    if (hintPos != -1) {
      theRow.displayExample =
        theRow.displayName.substr(hintPos, theRow.displayName.length) +
        (theRow.displayExample
          ? theRow.displayExample.replace(/\[translation hint.*?\]/g, "")
          : "");
      theRow.displayName = theRow.displayName.substr(0, hintPos);
    }
    cell.appendChild(
      cldrDom.createChunk(theRow.displayName, "span", "subSpan")
    );
    const TRANS_HINT_ID = "en_ZZ"; // must match SurveyMain.TRANS_HINT_ID
    cldrSurvey.setLang(cell, TRANS_HINT_ID);
    if (theRow.displayExample) {
      appendExample(cell, theRow.displayExample, TRANS_HINT_ID);
    }
    if (hintPos != -1 || hasExample) {
      var infos = document.createElement("div");
      infos.className = "infos-code";
      if (hintPos != -1) {
        var img = document.createElement("img");
        img.src = "hint.png";
        img.alt = "Translation hint";
        infos.appendChild(img);
      }
      if (hasExample) {
        var img = document.createElement("img");
        img.src = "example.png";
        img.alt = "Example";
        infos.appendChild(img);
      }
      cell.appendChild(infos);
    }
  } else {
    cell.appendChild(document.createTextNode(""));
  }
  cldrInfo.listen(null, tr, cell, null);
  cell.isSetup = true;
}

/**
 * Update the "proposed cell", a.k.a. the "Winning" column, of this row
 *
 * @param tr the table row
 * @param theRow the data from the server for this row
 * @param cell the table cell
 * @param protoButton
 *
 * Called by updateRow.
 */
function updateRowProposedWinningCell(tr, theRow, cell, protoButton) {
  cldrDom.removeAllChildNodes(cell); // win
  if (theRow.rowFlagged) {
    var flagIcon = cldrSurvey.addIcon(cell, "s-flag");
    flagIcon.title = cldrText.get("flag_desc");
  } else if (theRow.canFlagOnLosing) {
    var flagIcon = cldrSurvey.addIcon(cell, "s-flag-d");
    flagIcon.title = cldrText.get("flag_d_desc");
  }
  cldrSurvey.setLang(cell);
  tr.proposedcell = cell;

  /*
   * If server doesn't do its job properly, theRow.items[theRow.winningVhash] may be undefined.
   * Check for that here to prevent crash in addVitem. An error message might be appropriate here
   * in that case, though the consistency checking really should happen earlier, see checkRowConsistency.
   */
  if (getValidWinningValue(theRow) !== null) {
    addVitem(
      cell,
      tr,
      theRow,
      theRow.items[theRow.winningVhash],
      cldrSurvey.cloneAnon(protoButton)
    );
  } else {
    cell.showFn = function () {}; // nothing else to show
  }
  cldrInfo.listen(null, tr, cell, cell.showFn);
}

/**
 * Update the "Others" cell (column) of this row
 *
 * @param tr the table row
 * @param theRow the data from the server for this row
 * @param cell the table cell
 * @param protoButton
 * @param formAdd
 *
 * Called by updateRow.
 */
function updateRowOthersCell(tr, theRow, cell, protoButton, formAdd) {
  var hadOtherItems = false;
  cldrDom.removeAllChildNodes(cell); // other
  cldrSurvey.setLang(cell);

  if (tr.canModify) {
    formAdd.role = "form";
    formAdd.className = "form-inline";
    var buttonAdd = document.createElement("div");
    var btn = document.createElement("button");
    buttonAdd.className = "button-add form-group";

    toAddVoteButton(btn);

    buttonAdd.appendChild(btn);
    formAdd.appendChild(buttonAdd);

    var input = document.createElement("input");
    var popup;
    input.className = "form-control input-add";
    cldrSurvey.setLang(input);
    input.placeholder = "Add a translation";
    var copyWinning = document.createElement("button");
    copyWinning.className = "copyWinning btn btn-info btn-xs";
    copyWinning.title = "Copy Winning";
    copyWinning.type = "button";
    copyWinning.innerHTML =
      '<span class="glyphicon glyphicon-arrow-right"></span> Winning';
    copyWinning.onclick = function (e) {
      var theValue = getValidWinningValue(theRow);
      if (theValue === cldrSurvey.INHERITANCE_MARKER || theValue === null) {
        theValue = theRow.inheritedValue;
      }
      input.value = theValue || null;
      input.focus();
    };
    var copyEnglish = document.createElement("button");
    copyEnglish.className = "copyEnglish btn btn-info btn-xs";
    copyEnglish.title = "Copy English";
    copyEnglish.type = "button";
    copyEnglish.innerHTML =
      '<span class="glyphicon glyphicon-arrow-right"></span> English';
    copyEnglish.onclick = function (e) {
      input.value = theRow.displayName || null;
      input.focus();
    };
    btn.onclick = function (e) {
      //if no input, add one
      if ($(buttonAdd).parent().find("input").length == 0) {
        //hide other
        $.each($("button.vote-submit"), function () {
          toAddVoteButton(this);
        });

        //transform the button
        toSubmitVoteButton(btn);
        $(buttonAdd)
          .popover({
            content: " ",
          })
          .popover("show");
        popup = $(buttonAdd).parent().find(".popover-content");
        popup.append(input);
        if (theRow.displayName) {
          popup.append(copyEnglish);
        }
        const winVal = getValidWinningValue(theRow);
        if (winVal || theRow.inheritedValue) {
          popup.append(copyWinning);
        }
        popup
          .closest(".popover")
          .css("top", popup.closest(".popover").position().top - 19);
        input.focus();

        //enter pressed
        $(input).keydown(function (e) {
          var newValue = $(this).val();
          if (e.keyCode == 13) {
            //enter pressed
            if (newValue) {
              addValueVote(
                cell,
                tr,
                theRow,
                newValue,
                cldrSurvey.cloneAnon(protoButton)
              );
            } else {
              toAddVoteButton(btn);
            }
          } else if (e.keyCode === 27) {
            toAddVoteButton(btn);
          }
        });
      } else {
        var newValue = input.value;

        if (newValue) {
          addValueVote(
            cell,
            tr,
            theRow,
            newValue,
            cldrSurvey.cloneAnon(protoButton)
          );
        } else {
          toAddVoteButton(btn);
        }
        cldrEvent.stopPropagation(e);
        return false;
      }
      cldrEvent.stopPropagation(e);
      return false;
    };
  }
  /*
   * Add the other vote info -- that is, vote info for the "Others" column.
   */
  for (let k in theRow.items) {
    if (k === theRow.winningVhash) {
      // skip vote for winner
      continue;
    }
    hadOtherItems = true;
    addVitem(
      cell,
      tr,
      theRow,
      theRow.items[k],
      cldrSurvey.cloneAnon(protoButton)
    );
    cell.appendChild(document.createElement("hr"));
  }

  if (!hadOtherItems /*!onIE*/) {
    cldrInfo.listen(null, tr, cell);
  }
  if (
    tr.myProposal &&
    tr.myProposal.value &&
    !cldrSurvey.findItemByValue(theRow.items, tr.myProposal.value)
  ) {
    // add back my proposal
    cell.appendChild(tr.myProposal);
  } else {
    tr.myProposal = null; // not needed
  }
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
  if (displayValue === cldrSurvey.INHERITANCE_MARKER) {
    displayValue = theRow.inheritedValue;
  }
  if (!displayValue) {
    return;
  }
  var div = document.createElement("div");
  var isWinner = td == tr.proposedcell;
  var testKind = cldrVote.getTestKind(item.tests);
  cldrVote.setDivClass(div, testKind);
  item.div = div; // back link

  var choiceField = document.createElement("div");
  var wrap;
  choiceField.className = "choice-field";
  if (newButton) {
    newButton.value = item.value;
    cldrVote.wireUpButton(newButton, tr, theRow, item.valueHash);
    wrap = cldrVote.wrapRadio(newButton);
    choiceField.appendChild(wrap);
  }
  var subSpan = document.createElement("span");
  subSpan.className = "subSpan";
  var span = cldrVote.appendItem(subSpan, displayValue, item.pClass);
  choiceField.appendChild(subSpan);

  checkLRmarker(choiceField, item.value);

  if (item.isBaselineValue == true) {
    cldrDom.appendIcon(
      choiceField,
      "i-star",
      cldrText.get("voteInfo_baseline_desc")
    );
  }
  if (item.votes && !isWinner) {
    if (
      item.valueHash == theRow.voteVhash &&
      theRow.canFlagOnLosing &&
      !theRow.rowFlagged
    ) {
      cldrSurvey.addIcon(choiceField, "i-stop"); // DEBUG
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
  td.showFn = item.showFn = cldrInfo.showItemInfoFn(theRow, item);
  div.popParent = tr;
  cldrInfo.listen(null, tr, div, td.showFn);
  td.appendChild(div);

  if (item.example && item.value != item.examples) {
    appendExample(div, item.example);
  }
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

function appendExample(parent, text, loc) {
  var div = document.createElement("div");
  div.className = "d-example well well-sm";
  div.innerHTML = text;
  cldrSurvey.setLang(div, loc);
  parent.appendChild(div);
  return div;
}

/**
 * Handle new value submission
 *
 * @param td
 * @param tr
 * @param theRow
 * @param newValue
 * @param newButton
 */
function addValueVote(td, tr, theRow, newValue, newButton) {
  tr.inputTd = td; // cause the proposed item to show up in the right box
  cldrVote.handleWiredClick(tr, theRow, "", newValue, newButton);
}

/**
 * Transform input + submit button to the add button for the "add translation"
 *
 * @param btn
 */
function toAddVoteButton(btn) {
  btn.className = "btn btn-primary";
  btn.title = "Add";
  btn.type = "submit";
  btn.innerHTML = '<span class="glyphicon glyphicon-plus"></span>';
  $(btn).parent().popover("destroy");
  $(btn).tooltip("destroy").tooltip();
  $(btn).closest("form").next(".subSpan").show();
  $(btn).parent().children("input").remove();
}

/**
 * Transform the add button to a submit
 *
 * @param btn the button
 * @return the transformed button (return value is ignored by caller)
 */
function toSubmitVoteButton(btn) {
  btn.innerHTML = '<span class="glyphicon glyphicon-ok-circle"></span>';
  btn.className = "btn btn-success vote-submit";
  btn.title = "Submit";
  $(btn).tooltip("destroy").tooltip();
  $(btn).closest("form").next(".subSpan").hide();
  return btn;
}

/**
 * Update the "no cell", a.k.a, the "Abstain" column, of this row
 * Also possibly make changes to the "proposed" (winning) cell
 *
 * If the user can make changes, add an "abstain" button;
 * else, possibly add a ticket link, or else hide the column.
 *
 * @param tr the table row
 * @param theRow the data from the server for this row
 * @param noCell the table "no" (abstain) cell
 * @param proposedCell the table "proposed" (winning) cell
 * @param protoButton
 *
 * Called by updateRow.
 */
function updateRowNoAbstainCell(tr, theRow, noCell, proposedCell, protoButton) {
  if (tr.canModify) {
    cldrDom.removeAllChildNodes(noCell); // no opinion
    var noOpinion = cldrSurvey.cloneAnon(protoButton);
    cldrVote.wireUpButton(noOpinion, tr, theRow, null);
    noOpinion.value = null;
    var wrap = cldrVote.wrapRadio(noOpinion);
    noCell.appendChild(wrap);
    cldrInfo.listen(null, tr, noCell);
  } else if (tr.ticketOnly) {
    // ticket link
    if (!tr.theTable.json.canModify) {
      // only if hidden in the header
      cldrDom.setDisplayed(noCell, false);
    }
    proposedCell.className = "d-change-confirmonly";
    var surlink = document.createElement("div");
    surlink.innerHTML =
      '<span class="glyphicon glyphicon-list-alt"></span>&nbsp;&nbsp;';
    surlink.className = "alert alert-info fix-popover-help";
    var link = cldrDom.createChunk(cldrText.get("file_a_ticket"), "a");
    const curLocale = cldrStatus.getCurrentLocale();
    var newUrl =
      "http://unicode.org/cldr/trac" +
      "/newticket?component=data&summary=" +
      curLocale +
      ":" +
      theRow.xpath +
      "&locale=" +
      curLocale +
      "&xpath=" +
      theRow.xpstrid +
      "&version=" +
      cldrStatus.getNewVersion();
    link.href = newUrl;
    link.target = "cldr-target-trac";
    theRow.proposedResults = cldrDom.createChunk(
      cldrText.get("file_ticket_must"),
      "a",
      "fnotebox"
    );
    theRow.proposedResults.href = newUrl;
    if (cldrStatus.getIsUnofficial()) {
      link.appendChild(
        cldrDom.createChunk(
          " (Note: this is not the production SurveyTool! Do not submit a ticket!) ",
          "p"
        )
      );
      link.href = link.href + "&description=NOT+PRODUCTION+SURVEYTOOL!";
    }
    proposedCell.appendChild(
      cldrDom.createChunk(cldrText.get("file_ticket_notice"), "i", "fnotebox")
    );
    surlink.appendChild(link);
    tr.ticketLink = surlink;
  } else {
    // no change possible
    if (!tr.theTable.json.canModify) {
      // only if hidden in the header
      cldrDom.setDisplayed(noCell, false);
    }
  }
}

/**
 * Get the winning value for the given row, if it's a valid value.
 * Null and NO_WINNING_VALUE ('no-winning-value') are not valid.
 * See NO_WINNING_VALUE in VoteResolver.java.
 *
 * @param theRow
 * @returns the winning value, or null if there is not a valid winning value
 */
function getValidWinningValue(theRow) {
  if (!theRow) {
    console.error("theRow is null or undefined in getValidWinningValue");
    return null;
  }
  if (
    theRow.items &&
    theRow.winningVhash &&
    theRow.items[theRow.winningVhash]
  ) {
    const item = theRow.items[theRow.winningVhash];
    if (item.value) {
      const val = item.value;
      if (val !== NO_WINNING_VALUE) {
        return val;
      }
    }
  }
  return null;
}

export {
  NO_WINNING_VALUE,
  TABLE_USES_NEW_API,
  appendExample,
  getRowApprovalStatusClass,
  insertRows,
  refreshSingleRow,
  updateRow,
  /*
   * The following are meant to be accessible for unit testing only:
   */
  cldrChecksum,
};

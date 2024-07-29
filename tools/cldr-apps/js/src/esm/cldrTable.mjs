/*
 * cldrTable: encapsulate code related to the main Survey Tool html table,
 * whose rows describe xpaths.
 *
 * Functions for populating the main table in the vetting page:
 * 		insertRows
 * 		updateRow
 * 		refreshSingleRow
 */

import * as cldrAddAlt from "./cldrAddAlt.mjs";
import * as cldrAjax from "./cldrAjax.mjs";
import * as cldrCoverage from "./cldrCoverage.mjs";
import * as cldrDashContext from "./cldrDashContext.mjs";
import * as cldrDom from "./cldrDom.mjs";
import * as cldrEvent from "./cldrEvent.mjs";
import * as cldrGui from "./cldrGui.mjs";
import * as cldrInfo from "./cldrInfo.mjs";
import * as cldrLoad from "./cldrLoad.mjs";
import * as cldrProgress from "./cldrProgress.mjs";
import * as cldrStatus from "./cldrStatus.mjs";
import * as cldrSurvey from "./cldrSurvey.mjs";
import * as cldrText from "./cldrText.mjs";
import * as cldrVote from "./cldrVote.mjs";
import * as cldrXPathUtils from "./cldrXpathUtils.mjs";

const HEADER_ID_PREFIX = "header_";
const ROW_ID_PREFIX = "row_"; // formerly "r@"

const CLDR_TABLE_DEBUG = false;

/*
 * NO_WINNING_VALUE indicates the server delivered path data without a valid winning value.
 * It must match NO_WINNING_VALUE in the server Java code.
 */
const NO_WINNING_VALUE = "no-winning-value";

/**
 * Special input.value meaning an empty value as opposed to an abstention
 */
const EMPTY_ELEMENT_VALUE = "❮EMPTY❯";

/**
 * Remember the element (HTMLTableCellElement or HTMLDivElement) that was most recently
 * shown as "selected" (displayed with a thick border). When a new element gets selected,
 * this is used for removing the border from the old one
 */
let lastShown = null;

/**
 * Prepare rows to be inserted into the table
 *
 * @param theDiv the division (typically or always? with id='DynamicDataSection') that contains, or will contain, the table
 * @param xpath = json.pageId; e.g., "Alphabetic_Information"
 * @param session the session id; e.g., "DEF67BCAAFED4332EBE742C05A8D1161"
 * @param json the json received from the server; including (among much else):
 * 			json.locale, e.g., "aa"
 *  		json.page.rows, with info for each row
 */
function insertRows(theDiv, xpath, session, json) {
  cldrProgress.updatePageCompletion(json.canModify ? json.page.rows : null);
  $(".warnText").remove(); // remove any pre-existing "special notes", before insertLocaleSpecialNote
  cldrLoad.insertLocaleSpecialNote(theDiv);

  let theTable = null;
  const reuseTable =
    theDiv.theTable &&
    theDiv.theTable.json &&
    tablesAreCompatible(json, theDiv.theTable.json);
  if (reuseTable) {
    /*
     * Re-use the old table, just update contents of individual cells
     */
    theTable = theDiv.theTable;
  } else {
    /*
     * Re-create the table from scratch
     */
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
    const toAdd = document.getElementById("proto-datarow"); // loaded from "hidden.html", which see.
    const rowChildren = cldrSurvey.getTagChildren(toAdd);
    for (let c in rowChildren) {
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

  if (!reuseTable || !theDiv.contains(theTable)) {
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
 */
function tablesAreCompatible(json1, json2) {
  if (
    json1.page &&
    json2.page &&
    json1.pageId === json2.pageId &&
    json1.locale === json2.locale &&
    json1.canModify === json2.canModify &&
    Object.keys(json1.page.rows).length === Object.keys(json2.page.rows).length
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
  const tbody = theTable.getElementsByTagName("tbody")[0];
  const theRows = theTable.json.page.rows;
  const toAdd = theTable.toAdd;
  const parRow = document.getElementById("proto-parrow");
  const theSort = theTable.json.displaySets.ph; // path header
  const partitions = theSort.partitions;
  const rowList = theSort.rows;
  const partitionList = Object.keys(partitions);
  let curPartition = null;
  for (let i in rowList) {
    const k = rowList[i];
    const theRow = theRows[k];
    const dir = theRow.dir;
    cldrSurvey.setOverrideDir(dir != null ? dir : null);
    /*
     * Don't regenerate the headings if we're re-using an existing table.
     */
    if (!reuseTable) {
      const newPartition = findPartition(
        partitions,
        partitionList,
        curPartition,
        i
      );

      if (newPartition != curPartition) {
        if (newPartition.name != "") {
          addPartitionHeader(newPartition, tbody, parRow);
        }
        curPartition = newPartition;
      }

      const theRowCov = parseInt(theRow.coverageValue);
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
     */
    const rowId = makeRowId(theRow.xpstrid);
    let tr = reuseTable ? document.getElementById(rowId) : null;
    if (!tr) {
      tr = cldrSurvey.cloneAnon(toAdd);
      tbody.appendChild(tr);
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
    if (tr.className !== "tr_checking1" && tr.className !== "tr_checking2") {
      updateRow(tr, theRow);
    }
  }
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
  for (let j in partitionList) {
    const p = partitions[j];
    if (i >= p.start && i < p.limit) {
      return p;
    }
  }
  return null;
}

/**
 * Add a partition header, such as for a partition "Western Asia" in the
 * page for "Locale Display Names - Territories (Asia)"
 *
 * @param {Element} newPartition the element that was found by findPartition
 * @param {Element} tbody the element to which the header should be appended
 * @param {Element} parRow the element to be cloned to make the new header
 */
function addPartitionHeader(newPartition, tbody, parRow) {
  const newPar = cldrSurvey.cloneAnon(parRow);
  const newTd = cldrSurvey.getTagChildren(newPar);
  const newHeading = cldrSurvey.getTagChildren(newTd[0]);
  const headerId = makeHeaderId(newPartition.name);
  newHeading[0].id = headerId;
  newHeading[0].innerHTML = newPartition.name;
  newHeading[0].onclick = function () {
    cldrStatus.setCurrentId(headerId);
    cldrLoad.replaceHash();
  };
  tbody.appendChild(newPar);
  newPar.origClass = newPar.className;
  newPartition.tr = newPar; // heading
}

/**
 * Reload a specific row
 *
 * Called only by load handler of cldrVote.handleWiredClick
 */
function refreshSingleRow(tr, theRow, onSuccess, onFailure) {
  cldrSurvey.showLoader(cldrText.get("loadingOneRow"));

  const xhrArgs = {
    url: getSingleRowUrl(theRow),
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
  if (CLDR_TABLE_DEBUG) {
    console.log("singleRowLoadHandler start time = " + Date.now());
  }
  try {
    if (json.page.rows[tr.rowHash]) {
      theRow = json.page.rows[tr.rowHash];
      tr.theTable.json.page.rows[tr.rowHash] = theRow;
      updateRow(tr, theRow);
      if (CLDR_TABLE_DEBUG) {
        console.log(
          "singleRowLoadHandler after updateRow time = " + Date.now()
        );
      }
      cldrSurvey.hideLoader();
      onSuccess(theRow);
      if (CLDR_TABLE_DEBUG) {
        console.log(
          "singleRowLoadHandler after onSuccess time = " + Date.now()
        );
      }
      cldrDashContext.updateRow(json);
      cldrInfo.showRowObjFunc(tr, tr.proposedcell, tr.proposedcell.showFn);
      if (CLDR_TABLE_DEBUG) {
        console.log(
          "singleRowLoadHandler after showRowObjFunc time = " + Date.now()
        );
      }
      cldrProgress.updateCompletionOneVote(theRow.hasVoted);
      cldrGui.refreshCounterVetting();
      if (CLDR_TABLE_DEBUG) {
        console.log(
          "singleRowLoadHandler after refreshCounterVetting time = " +
            Date.now()
        );
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
  if (CLDR_TABLE_DEBUG) {
    console.log("singleRowLoadHandler end time = " + Date.now());
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

function getSingleRowUrl(theRow) {
  const loc = cldrStatus.getCurrentLocale();
  const api = "voting/" + loc + "/row/" + theRow.xpstrid;
  let p = null;
  if (cldrDashContext.isVisible()) {
    p = new URLSearchParams();
    p.append("dashboard", "true");
  }
  return cldrAjax.makeApiUrl(api, p);
}

function getPageUrl(curLocale, curPage, curId) {
  let p = null;
  if (curId && !curPage) {
    p = new URLSearchParams();
    p.append("xpstrid", curId);
    curPage = "auto";
  }
  const api = "voting/" + curLocale + "/page/" + curPage;
  return cldrAjax.makeApiUrl(api, p);
}

/**
 * Update one row using data received from server.
 *
 * @param {Node} tr the table row
 * @param {Object} theRow the data for the row
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
  for (let k in theRow.items) {
    const item = theRow.items[k];
    if (item.value || item.value === "") {
      tr.rawValueToItem[item.rawValue] = item; // back link by value
    }
  }

  /*
   * Update the vote info.
   */
  if (theRow.votingResults) {
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
      tr.id = makeRowId(tr.xpstrid);
      tr.sethash = tr.xpstrid;
    }
  }

  let protoButton = null; // no voting at all, unless tr.canModify
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
  if (comparisonCell) {
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
 * Inconsistencies should primarily be detected/reported/fixed on server (DataPage.java)
 * rather than here on the client, but better late than never, and these checks may be useful
 * for automated testing with WebDriver.
 */
function checkRowConsistency(theRow) {
  if (!theRow) {
    console.error("theRow is null or undefined in checkRowConsistency");
    return;
  }
  if (!theRow.winningVhash && theRow.winningVhash !== "") {
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

  for (let k in theRow.items) {
    const item = theRow.items[k];
    if (item.value === cldrSurvey.INHERITANCE_MARKER) {
      if (!theRow.inheritedValue) {
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
  cell.className = "d-dr-" + statusClass + " statuscell";
  cell.innerHTML = getStatusIcon(statusClass);
  if (!cell.isSetup) {
    listen("", tr, cell, null);
    cell.isSetup = true;
  }

  const statusTitle = cldrText.get(statusClass);
  cell.title = cldrText.sub("draftStatus", [statusTitle]);
}

/**
 * Get the Unicode character corresponding to the given status class
 *
 * @param statusClass "approved", "missing", etc.
 * @return the character such as "✓", "✘", "↑", etc.
 */
function getStatusIcon(statusClass) {
  switch (statusClass) {
    case "approved":
    case "contributed":
    case "missing":
    case "provisional":
    case "unconfirmed":
      return cldrText.get(`status_${statusClass}`);
    case "inherited-provisional":
    case "inherited-unconfirmed":
      return (
        cldrText.get(`status_inherited`) +
        "\u200B" +
        getStatusIcon(statusClass.split("-")[1])
      );
    default:
      return "\ufffd";
  }
}

/**
 * On the client only, make further status distinctions when winning value is cldrSurvey.INHERITANCE_MARKER,
 * "inherited-unconfirmed" (red up-arrow icon) and "inherited-provisional" (orange up-arrow icon).
 * Reference: http://unicode.org/cldr/trac/ticket/11103
 *
 * @param theRow the data from the server for this row
 */
function getRowApprovalStatusClass(theRow) {
  let statusClass = theRow.confirmStatus;

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
  let codeStr = theRow.code;
  if (theRow.coverageValue == 101) {
    codeStr = codeStr + " (optional)";
  }
  cell.appendChild(cldrDom.createChunk(codeStr));
  cell.className = "d-code codecell";

  // extra attributes
  if (
    theRow.extraAttributes &&
    Object.keys(theRow.extraAttributes).length > 0
  ) {
    cldrSurvey.appendExtraAttributes(cell, theRow);
  }
  if (!cell.isSetup) {
    listen("", tr, cell, null);
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
  cldrDom.removeAllChildNodes(cell);
  let trHint = theRow.translationHint; // sometimes null
  if (theRow.displayName) {
    cell.appendChild(
      cldrDom.createChunk(theRow.displayName, "span", "subSpan")
    );
  } else {
    cell.appendChild(document.createTextNode(""));
    if (!trHint) {
      trHint = cldrText.get("empty_comparison_cell_hint");
    }
  }
  const TRANS_HINT_ID = "en"; // expected to match SurveyMain.TRANS_HINT_ID
  cldrSurvey.setLang(cell, TRANS_HINT_ID);
  if (theRow.displayExample || trHint || theRow.forumStatus.hasPosts) {
    const infos = document.createElement("div");
    infos.className = "infos-code";
    if (trHint) {
      appendTranslationHintIcon(infos, trHint, TRANS_HINT_ID);
    }
    if (theRow.displayExample) {
      appendExampleIcon(infos, theRow.displayExample, TRANS_HINT_ID);
    }
    if (theRow.forumStatus.hasPosts) {
      appendForumStatus(infos, theRow.forumStatus, TRANS_HINT_ID);
    }
    cell.appendChild(infos);
  }
  listen(null, tr, cell, null);
  if (cldrStatus.getPermissions()?.userIsTC) {
    cldrAddAlt.addButton(cell, theRow.xpstrid);
  }
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
    const flagIcon = cldrSurvey.addIcon(cell, "s-flag");
    flagIcon.title = cldrText.get("flag_desc");
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
  listen(null, tr, cell, cell.showFn);
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
  let hadOtherItems = false;
  cldrDom.removeAllChildNodes(cell); // other
  cldrSurvey.setLang(cell);

  if (tr.canModify && !tr.theRow.fixedCandidates) {
    formAdd.role = "form";
    formAdd.className = "form-inline";
    const buttonAdd = document.createElement("div");
    const btn = document.createElement("button");
    buttonAdd.className = "button-add form-group";

    toAddVoteButton(btn);

    buttonAdd.appendChild(btn);
    formAdd.appendChild(buttonAdd);

    const input = document.createElement("input");
    let popup;
    input.className = "form-control input-add";
    input.id = "input-add-translation";
    cldrSurvey.setLang(input);
    input.placeholder = "Add a translation";
    const copyWinning = document.createElement("button");
    copyWinning.className = "copyWinning btn btn-info btn-xs";
    copyWinning.title = "Copy Winning";
    copyWinning.type = "button";
    copyWinning.innerHTML =
      '<span class="glyphicon glyphicon-arrow-right"></span> Winning';
    copyWinning.onclick = function (e) {
      let theValue = getValidWinningValue(theRow);
      if (theValue === cldrSurvey.INHERITANCE_MARKER || theValue === null) {
        theValue = theRow.inheritedDisplayValue;
      }
      input.value = theValue || null;
      input.focus();
    };
    const copyEnglish = document.createElement("button");
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
          const newValue = $(this).val();
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
        const newValue = input.value;

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
    listen(null, tr, cell);
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
  let displayValue = item.value;
  if (displayValue === cldrSurvey.INHERITANCE_MARKER) {
    displayValue = theRow.inheritedDisplayValue;
  }
  if (!displayValue && displayValue !== "") {
    return;
  }
  const div = document.createElement("div");
  const isWinner = td == tr.proposedcell;
  const testKind = cldrVote.getTestKind(item.tests);
  setDivClass(div, testKind);
  item.div = div; // back link

  const choiceField = document.createElement("div");
  choiceField.className = "choice-field";
  if (newButton) {
    newButton.value = item.value;
    cldrVote.wireUpButton(newButton, tr, theRow, item.valueHash);
    const wrap = cldrVote.wrapRadio(newButton);
    choiceField.appendChild(wrap);
  }
  const subSpan = document.createElement("span");
  subSpan.className = "subSpan";
  cldrVote.appendItem(subSpan, displayValue, item.pClass);
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
      const stopIcon = cldrSurvey.addIcon(choiceField, "i-stop");
      stopIcon.setAttribute("dir", "ltr");
      stopIcon.title = cldrText.get("mustflag_explain_msg");
    }
  }

  /*
   * Note: history is maybe only defined for debugging; won't normally display it in production.
   * See DataPage.USE_CANDIDATE_HISTORY which currently should be false for production, so
   * that item.history will be undefined.
   */
  if (item.history) {
    const historyText = " ☛" + item.history;
    const historyTag = cldrDom.createChunk(historyText, "span", "");
    choiceField.appendChild(historyTag);
    listen(historyText, tr, historyTag, null);
  }

  const surveyUser = cldrStatus.getSurveyUser();
  if (
    newButton &&
    theRow.voteVhash == item.valueHash &&
    theRow.items[theRow.voteVhash].votes &&
    theRow.items[theRow.voteVhash].votes[surveyUser.id] &&
    theRow.items[theRow.voteVhash].votes[surveyUser.id].overridedVotes
  ) {
    const overrideTag = cldrDom.createChunk(
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
  listen(null, tr, div, td.showFn);
  td.appendChild(div);

  if (item.example && item.value != item.examples) {
    appendExample(div, item.example);
  }
}

function setDivClassSelected(div, testKind) {
  setDivClass(div, testKind);
  setLastShown(div); // add thick border and remove it from previous selected element
}

function setDivClass(div, testKind) {
  if (testKind == "Warning") {
    div.className = "d-item-warn";
  } else if (testKind == "Error") {
    div.className = "d-item-err";
  } else {
    div.className = "d-item";
  }
}

/**
 * Return a function that will set theRow.selectedItem, which will result in
 * showing info for the given candidate item in the Info Panel.
 *
 * @param {Object} theRow the data row
 * @param {JSON} item JSON of the specific candidate item we are adding
 * @returns the function
 */
function showItemInfoFn(theRow, item) {
  return function (td) {
    theRow.selectedItem = item;
  };
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
      const lrm = document.createElement("div");
      lrm.className = "lrmarker-container";
      lrm.innerHTML = value;
      field.appendChild(lrm);
    }
  }
}

function appendTranslationHintIcon(parent, text, loc) {
  const el = document.createElement("span");
  el.className = "d-trans-hint well well-sm";
  el.textContent = "Translation hint: " + text;
  cldrSurvey.setLang(el, loc);
  parent.appendChild(el);
  // This is related to "mouseenter" configured in cldrEvent.startup
  const img = document.createElement("img");
  img.className = "d-trans-hint-img";
  img.src = "hint.png";
  img.alt = "Translation hint";
  parent.appendChild(img);
  return el;
}

function appendForumStatus(parent, forumStatus, loc) {
  const el = document.createElement("span");
  el.textContent = forumStatus.hasOpenPosts
    ? cldrText.get("forum_path_has_open_posts_icon")
    : cldrText.get("forum_path_has_only_closed_posts_icon");
  el.title =
    cldrText.get("forum_path_has_posts") +
    (forumStatus.hasOpenPosts
      ? cldrText.get("forum_path_has_open_posts")
      : cldrText.get("forum_path_has_only_closed_posts"));
  el.style.backgroundColor = forumStatus.hasOpenPosts ? "orange" : "green";
  el.style.padding = el.style.margin = ".5ex";
  cldrSurvey.setLang(el, loc);
  parent.appendChild(el);
  return el;
}

function appendExampleIcon(parent, text, loc) {
  const el = appendExample(parent, text, loc);
  const img = document.createElement("img");
  // This is related to "mouseenter" configured in cldrEvent.startup
  img.className = "d-example-img";
  img.src = "example.png";
  img.alt = "Example";
  parent.appendChild(img);
  return el;
}

// caution: this is called from other modules as well as by appendExampleIcon
function appendExample(parent, text, loc) {
  const el = document.createElement("div");
  el.className = "d-example well well-sm";
  el.innerHTML = text;
  cldrSurvey.setLang(el, loc);
  parent.appendChild(el);
  return el;
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
    const noOpinion = cldrSurvey.cloneAnon(protoButton);
    cldrVote.wireUpButton(noOpinion, tr, theRow, null);
    noOpinion.value = null;
    const wrap = cldrVote.wrapRadio(noOpinion);
    noCell.appendChild(wrap);
    listen(null, tr, noCell);
  } else if (tr.ticketOnly) {
    // ticket link
    if (!tr.theTable.json.canModify) {
      // only if hidden in the header
      cldrDom.setDisplayed(noCell, false);
    }
    proposedCell.className = "d-change-confirmonly";
    const surlink = document.createElement("div");
    surlink.innerHTML =
      '<span class="glyphicon glyphicon-list-alt"></span>&nbsp;&nbsp;';
    surlink.className = "alert alert-info fix-popover-help";
    const link = cldrDom.createChunk(cldrText.get("file_a_ticket"), "a");
    const curLocale = cldrStatus.getCurrentLocale();
    const newUrl =
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
    (theRow.winningVhash || theRow.winningVhash === "") &&
    theRow.items[theRow.winningVhash]
  ) {
    const item = theRow.items[theRow.winningVhash];
    if (item.value && item.value !== "") {
      const val = item.value;
      if (val !== NO_WINNING_VALUE) {
        return val;
      }
    }
  }
  return null;
}

function makeRowId(id) {
  return ROW_ID_PREFIX + id;
}

function isHeaderId(id) {
  return id.startsWith(HEADER_ID_PREFIX);
}

function makeHeaderId(name) {
  // Replace all sequences of non-alphanumeric characters with underscore, and
  // start with HEADER_ID_PREFIX to identify this kind of id uniquely
  return HEADER_ID_PREFIX + name.replaceAll(/[^a-zA-Z0-9]+/g, "_");
}

function goToHeaderId(headerId) {
  const el = document.getElementById(headerId);
  if (el) {
    el.scrollIntoView({ block: "start" });
  }
}

/**
 * Make the object "theObj" respond to being clicked. Clicking a cell in the main
 * vetting table should make the cell highlighted, update the URL bar to show
 * the hex id of the path for the row in question, and update the Info Panel if
 * the Info Panel is open.
 *
 * This function suffers from extreme tech debt. It was formerly in the cldrInfo module.
 * The fn parameter (a.k.a. showFn) shouldn't need to be preconstructed for each item in
 * each row, attached to DOM elements, and passed around as a parameter in such a complicated way.
 *
 * @param {String} str the string to display
 * @param {Node} tr the TR element that is clicked
 * @param {Node} theObj to listen to, a.k.a. "hideIfLast"
 * @param {Function} fn the draw function
 */
function listen(str, tr, theObj, fn) {
  cldrDom.listenFor(theObj, "click", function (e) {
    if (cldrInfo.panelShouldBeShown()) {
      cldrInfo.show(str, tr, theObj /* hideIfLast */, fn);
    } else {
      updateSelectedRowAndCell(tr, theObj);
    }
    cldrEvent.stopPropagation(e);
    return false;
  });
}

function updateSelectedRowAndCell(tr, obj) {
  if (tr?.sethash) {
    cldrLoad.updateCurrentId(tr.sethash);
  }
  setLastShown(obj);
}

function setLastShown(obj) {
  if (lastShown && obj != lastShown) {
    cldrDom.removeClass(lastShown, "pu-select");
    const partr = cldrDom.parentOfType("TR", lastShown);
    if (partr) {
      cldrDom.removeClass(partr, "selectShow");
    }
  }
  if (obj) {
    cldrDom.addClass(obj, "pu-select");
    const partr = cldrDom.parentOfType("TR", obj);
    if (partr) {
      cldrDom.addClass(partr, "selectShow");
    }
  }
  lastShown = obj;
}

function resetLastShown() {
  lastShown = null;
}

export {
  NO_WINNING_VALUE,
  EMPTY_ELEMENT_VALUE,
  appendExample,
  getPageUrl,
  getRowApprovalStatusClass,
  getStatusIcon,
  goToHeaderId,
  insertRows,
  isHeaderId,
  listen,
  makeRowId,
  refreshSingleRow,
  resetLastShown,
  setDivClassSelected,
  setLastShown,
  updateRow,
  updateSelectedRowAndCell,
  /*
   * The following are meant to be accessible for unit testing only:
   */
  cldrChecksum,
  makeHeaderId,
};

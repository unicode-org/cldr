/*
 * cldrTable: encapsulate code related to the main Survey Tool html table,
 * whose rows describe xpaths.
 *
 * Functions for populating the main table in the vetting page:
 * 		insertRows
 * 		updateRow
 *
 * updateRow is also used for the Dashboard.
 */
import * as cldrDom from "./cldrDom.js";
import * as cldrEvent from "./cldrEvent.js";
import * as cldrForumPanel from "./cldrForumPanel.js";
import * as cldrInfo from "./cldrInfo.js";
import * as cldrLoad from "./cldrLoad.js";
import * as cldrStatus from "./cldrStatus.js";
import * as cldrSurvey from "./cldrSurvey.js";
import * as cldrText from "./cldrText.js";

const CLDR_TABLE_DEBUG = false;
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
    /*
     * Note: cldrStatus.isDashboard() is currently never true here; see comments in insertRowsIntoTbody and updateRow
     */
    if (cldrStatus.isDashboard()) {
      theTable.className += " dashboard";
    } else {
      theTable.className += " vetting-page";
    }
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
  cldrSurvey.updateCoverage(theDiv);
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
 *
 * This function is not currently used for the Dashboard, only for the main vetting table.
 * Still we may want to keep the calls to cldrStatus.isDashboard for future use. Also note that updateRow,
 * which is called from here, IS also used for the Dashboard.
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
     * There is no partition (section headings) in the Dashboard.
     * Also we don't regenerate the headings if we're re-using an existing table.
     */
    if (!reuseTable && !cldrStatus.isDashboard()) {
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
 * Update one row using data received from server.
 *
 * @param tr the table row
 * @param theRow the data for the row
 *
 * Cells (columns) in each row:
 * Code    English    Abstain    A    Winning    Add    Others
 *
 * IMPORTANT: this function is used for the Dashboard as well as the main Vetting table.
 * Mostly the Dashboard tables are currently created by review.js showReviewPage
 * (invoked through writeVettingViewerOutput);
 * they're not created here. Nevertheless the calls here to cldrStatus.isDashboard() do serve a purpose,
 * cldrStatus.isDashboard() is true here when called by insertFixInfo in review.js. To see this, put
 * a breakpoint in this function, go to Dashboard, and click on a "Fix" button, whose pop-up
 * window then will include portions of the item's row as well as a version of the Info Panel.
 *
 * Dashboard columns are:
 * Code    English    CLDR 33    Winning 34    Action
 * -- but we don't add those here; instead, for Dashboard, this function is used
 * only for the "Fix" pop-up window, in which the "columns" aren't really in a table,
 * they're div elements.
 *
 * Called by insertRowsIntoTbody and loadHandler (in refreshSingleRow),
 * AND by insertFixInfo in review.js (Dashboard "Fix")!
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
    updateRowVoteInfo(tr, theRow);
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
   *
   * This code is for Dashboard as well as the basic vetting table.
   * This block concerns the "other" cell if cldrStatus.isDashboard(), otherwise it concerns the "add" cell.
   */
  if (tr.canChange) {
    if (cldrStatus.isDashboard()) {
      if (otherCell) {
        otherCell.appendChild(document.createElement("hr"));
        otherCell.appendChild(formAdd);
      }
    } else {
      if (addCell) {
        cldrDom.removeAllChildNodes(addCell);
        addCell.appendChild(formAdd);
      }
    }
  }

  /*
   * Set up the "no cell", a.k.a. the "Abstain" column.
   * If the user can make changes, add an "abstain" button;
   * else, possibly add a ticket link, or else hide the column.
   */
  updateRowNoAbstainCell(tr, theRow, abstainCell, proposedCell, protoButton);

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
        if (!extraPathAllowsNullValue(theRow.xpath)) {
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
 * Is the given path exceptional in the sense that null value is allowed?
 *
 * @param path the path
 * @return true if null value is allowed for path, else false
 *
 * This function is nearly identical to the Java function with the same name in TestPaths.java.
 * Keep it consistent with that function. It would be more ideal if this knowledge were encapsulated
 * on the server and the client didn't need to know about it. The server could send the client special
 * fallback values instead of null.
 *
 * Unlike the Java version on the server, here on the client we don't actually check that the path is an "extra" path.
 *
 * Example: http://localhost:8080/cldr-apps/v#/pa_Arab/Gregorian/35b886c9d25c9cb7
 * //ldml/dates/calendars/calendar[@type="gregorian"]/dayPeriods/dayPeriodContext[@type="stand-alone"]/dayPeriodWidth[@type="wide"]/dayPeriod[@type="midnight"]
 *
 * Reference: https://unicode-org.atlassian.net/browse/CLDR-11238
 */
function extraPathAllowsNullValue(path) {
  if (
    path.includes("timeZoneNames/metazone") ||
    path.includes("timeZoneNames/zone") ||
    path.includes("dayPeriods/dayPeriodContext")
  ) {
    return true;
  }
  return false;
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
    if (value == null /* TODO: impossible? */ || value === NO_WINNING_VALUE) {
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
      const statusClass = getRowApprovalStatusClass(theRow);
      const statusTitle = cldrText.get(statusClass);
      cldrSurvey.appendIcon(
        isection,
        "voteInfo_winningItem d-dr-" + statusClass,
        cldrText.sub("draftStatus", [statusTitle])
      );
      isectionIsUsed = true;
    }
    if (item.isBaselineValue) {
      cldrSurvey.appendIcon(
        isection,
        "i-star",
        cldrText.get("voteInfo_baseline_desc")
      );
      isectionIsUsed = true;
    }
    cldrSurvey.setLang(valdiv);
    if (value === cldrSurvey.INHERITANCE_MARKER) {
      /*
       * theRow.inheritedValue can be undefined here; then do not append
       */
      if (theRow.inheritedValue) {
        cldrSurvey.appendItem(valdiv, theRow.inheritedValue, item.pClass);
        valdiv.appendChild(
          cldrDom.createChunk(cldrText.get("voteInfo_votesForInheritance"), "p")
        );
      }
    } else {
      cldrSurvey.appendItem(
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
  // done with voteresolver table
  if (CLDR_TABLE_DEBUG) {
    tr.voteDiv.appendChild(cldrDom.createChunk(vr.raw, "p", "debugStuff"));
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
              if (
                vr.nameTime[item.votes[topVoter].name] <
                vr.nameTime[item.votes[voter].name]
              ) {
                topVoter = voter;
                topVoterTime = vr.nameTime[item.votes[topVoter].name];
              }
            } else {
              topVoter = voter;
              topVoterTime = vr.nameTime[item.votes[topVoter].name];
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
      cldrSurvey.appendExample(cell, theRow.displayExample, TRANS_HINT_ID);
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
    cldrSurvey.addVitem(
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
    cldrSurvey.addVitem(
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
  cldrSurvey.handleWiredClick(tr, theRow, "", { value: newValue }, newButton);
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
    cldrSurvey.wireUpButton(noOpinion, tr, theRow, null);
    noOpinion.value = null;
    var wrap = cldrSurvey.wrapRadio(noOpinion);
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
  insertRows,
  updateRow,
  /*
   * The following are meant to be accessible for unit testing only:
   */
  cldrChecksum,
};

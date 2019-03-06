/*
 * CldrSurveyVettingTable.js - split off from survey.js, for CLDR Survey Tool
 * 
 * Functions for populating the main table in the vetting page: insertRows, updateRow, etc.
 * Some functions here (such as updateRow) are also used for the Dashboard (see review.js).
 *
 * TODO: make this more of a modular object; encapsulate; identify and reduce dependencies;
 * add unit tests that don't depend on server or browser.
 */
'use strict';

// TODO: replace with AMD [?] loading
dojo.require("dojo.i18n");
dojo.require("dojo.string");

const ALWAYS_REMOVE_ALL_CHILD_NODES = false; // should be false, but can make true to revert to old behavior before https://unicode.org/cldr/trac/ticket/11571
const NEVER_REUSE_TABLE = false; // should be false, but can make true to revert to old behavior before https://unicode.org/cldr/trac/ticket/11571

/*
 * ERROR_NO_WINNING_VALUE indicates a bug on the server, which delivered path data without a valid winning value.
 * Compare ERROR_NO_WINNING_VALUE in DataSection.java.
 */
const ERROR_NO_WINNING_VALUE = "error-no-winning-value";

/**
 * Prepare rows to be inserted into the table
 * 
 * @param theDiv the division (typically or always? with id='DynamicDataSection') that contains, or will contain, the table
 * @param xpath = json.pageId; e.g., "Alphabetic_Information"
 * @param session the session id; e.g., "DEF67BCAAFED4332EBE742C05A8D1161"
 * @param json the json received from the server; including (among much else):
 * 			json.locale, e.g., "aa"
 * 			json.section.coverage, e.g., "comprehensive"
 *  		json.section.rows, with info for each row
 */
function insertRows(theDiv, xpath, session, json) {

	if (ALWAYS_REMOVE_ALL_CHILD_NODES) {
		removeAllChildNodes(theDiv); // maybe superfluous if always recreate the table, and wrong if we don't always recreate the table
	}

	$('.warnText').remove(); // remove any pre-existing "special notes", before insertLocaleSpecialNote
	window.insertLocaleSpecialNote(theDiv);

	var theTable = null;
	const reuseTable = (!NEVER_REUSE_TABLE) && theDiv.theTable && theDiv.theTable.json && tablesAreCompatible(json, theDiv.theTable.json);
	if (reuseTable) {
		/*
		 * Re-use the old table, just update contents of individual cells
		 */
		// console.log("🦋🦋🦋 re-use table");
		theTable = theDiv.theTable;
	} else {
		/*
		 * Re-create the table from scratch
		 */
		// console.log("🦞🦞🦞 make new table");
		var theTable = cloneLocalizeAnon(dojo.byId('proto-datatable'));
		/*
		 * Note: isDashboard() is currently never true here; see comments in insertRowsIntoTbody and updateRow
		 */
		if (isDashboard()) {
			theTable.className += ' dashboard';
		} else {
			theTable.className += ' vetting-page';
		}
		/*
		 * Give our table the unique id, 'vetting-table'. This is needed by the test SurveyDriverVettingTable.
		 * Otherwise its id would be 'null' (the string 'null', not null!), and there is risk of confusion
		 * with other table such as 'proto-datarow'.
		 */
		theTable.id = 'vetting-table';
	}
	updateCoverage(theDiv);
	localizeFlyover(theTable);
	theTable.theadChildren = getTagChildren(theTable.getElementsByTagName("tr")[0]);
	var toAdd = dojo.byId('proto-datarow'); // loaded from "hidden.html", which see.
	var rowChildren = getTagChildren(toAdd);
	theTable.config = surveyConfig = {};
	for (var c in rowChildren) {
		rowChildren[c].title = theTable.theadChildren[c].title;
		if (rowChildren[c].id) {
			surveyConfig[rowChildren[c].id] = c;
			stdebug("  config." + rowChildren[c].id + " = children[" + c + "]");
		} else {
			stdebug("(proto-datarow #" + c + " has no id");
		}
	}
	if (stdebug_enabled) {
		stdebug("Table Config: " + JSON.stringify(theTable.config));
	}

	theTable.toAdd = toAdd;
	if (!json.canModify) {
		setDisplayed(theTable.theadChildren[theTable.config.nocell], false);
	}

	theDiv.theTable = theTable;
	theTable.theDiv = theDiv;

	// append header row
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
	insertRowsIntoTbody(theTable, reuseTable);
	if (!reuseTable) {
		theDiv.appendChild(theTable);
	}
	hideLoader(theDiv.loader);
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
	if (json1.section && json2.section &&
		json1.pageId === json2.pageId &&
		json1.locale === json2.locale &&
		json1.canModify === json2.canModify &&
		json1.section.coverage === json2.section.coverage &&
		json1.section.rows.length === json2.section.rows.length) {
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
 * Still we may want to keep the calls to isDashboard for future use. Also note that updateRow,
 * which is called from here, IS also used for the Dashboard.
 */
function insertRowsIntoTbody(theTable, reuseTable) {
	var tbody = theTable.getElementsByTagName("tbody")[0];
	var theRows = theTable.json.section.rows;
	var toAdd = theTable.toAdd;
	var parRow = dojo.byId('proto-parrow');

	if (ALWAYS_REMOVE_ALL_CHILD_NODES) {
		removeAllChildNodes(tbody);
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
		overridedir = (dir != null ? dir : null);
		/*
		 * There is no partition (section headings) in the Dashboard.
		 * Also we don't regenerate the headings if we're re-using an existing table.
		 */
		if (!reuseTable && !isDashboard()) {
			var newPartition = findPartition(partitions, partitionList, curPartition, i);

			if (newPartition != curPartition) {
				if (newPartition.name != "") {
					var newPar = cloneAnon(parRow);
					var newTd = getTagChildren(newPar);
					var newHeading = getTagChildren(newTd[0]);
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
					newPartition.tr.className = newPartition.origClass + " cov" + newPartition.minCoverage;
				}
			}
		}

		/*
		 * If tbody already contains tr with this id, re-use it
		 * Cf. in updateRow: tr.id = "r@"+tr.xpstrid;
		 */
		var tr = reuseTable ? document.getElementById("r@" + theRow.xpstrid) : null;
		if (!tr) {
			tr = cloneAnon(toAdd);
			tbody.appendChild(tr);
			// console.log("🦞 make new table row");
		} else {
			// console.log("🦋 re-use table row");
		}
		tr.rowHash = k;
		tr.theTable = theTable;

		/*
		 * Update the xpath map, unless re-using the table. If we're re-using the table, then
		 * curPartition.name isn't defined, and anyway xpathMap shouldn't need changing.
		 */
		if (!reuseTable) {
			xpathMap.put({
				id: theRow.xpathId,
				hex: theRow.xpstrid,
				path: theRow.xpath,
				ph: {
					section: surveyCurrentSection, // Section: Timezones
					page: surveyCurrentPage, // Page: SEAsia ( id, not name )
					header: curPartition.name, // Header: Borneo
					code: theRow.code // Code: standard-long
				}
			});
		}

		/*
		 * Update the tr's contents
		 */
		updateRow(tr, theRow);
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
	if (curPartition &&
		i >= curPartition.start &&
		i < curPartition.limit) {
		return curPartition;
	}
	for (var j in partitionList) {
		var p = partitions[j];
		if (i >= p.start &&
			i < p.limit) {
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
 * From left to right, td elements have these id attributes (which BTW aren't unique when
 * other rows are taken into account, see <https://unicode.org/cldr/trac/ticket/11312>):
 * 
 * codecell  comparisoncell  nocell  statuscell  proposedcell  addcell  othercell
 *
 * IMPORTANT: this function is used for the Dashboard as well as the main Vetting table.
 * Mostly the Dashboard tables are currently created by review.js showReviewPage
 * (invoked through EmbeddedReport.jsp, r_vetting_json.jsp, writeVettingViewerOutput);
 * they're not created here. Nevertheless the calls here to isDashboard() do serve a purpose,
 * isDashboard() is true here when called by insertFixInfo in review.js. To see this, put
 * a breakpoint in this function, go to Dashboard, and click on a "Fix" button, whose pop-up
 * window then will include portions of the item's row as well as a version of the Info Panel.
 *
 * Dashboard columns are:
 * Code    English    CLDR 33    Winning 34    Action
 * 
 * Called by insertRowsIntoTbody and loadHandler (in refreshRow2),
 * AND by insertFixInfo in review.js!
 */
function updateRow(tr, theRow) {
	tr.theRow = theRow;

	checkRowConsistency(theRow);

	/*
	 * For convenience, set up two hashes, for reverse mapping from value or rawValue to item.
	 */
	tr.valueToItem = {}; // hash:  string value to item (which has a div)
	tr.rawValueToItem = {}; // hash:  string value to item (which has a div)
	for (var k in theRow.items) {
		var item = theRow.items[k];
		if (item.value) {
			tr.valueToItem[item.value] = item; // back link by value
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

	tr.statusAction = parseStatusAction(theRow.statusAction);
	tr.canModify = (tr.theTable.json.canModify && tr.statusAction.vote);
	tr.ticketOnly = (tr.theTable.json.canModify && tr.statusAction.ticket);
	tr.canChange = (tr.canModify && tr.statusAction.change);

	if (!theRow.xpathId) {
		tr.innerHTML = "<td><i>ERROR: missing row</i></td>";
		return;
	}
	if (!tr.xpstrid) {
		tr.xpathId = theRow.xpathId;
		tr.xpstrid = theRow.xpstrid;
		if (tr.xpstrid) {
			tr.id = "r@" + tr.xpstrid;
			tr.sethash = tr.xpstrid;
		}
	}

	var children = getTagChildren(tr);

	/*
	 * config = surveyConfig has fields indicating which cells (columns) to display. It might look like this: 
	 * 
	 * Object {codecell: "0", comparisoncell: "1", nocell: "2", votedcell: "3", statuscell: "4", errcell: "5", proposedcell: "6", addcell: "7", othercell: "8"}
	 *
	 * Or, like this for Dashboard:
	 * 
	 * Object {nocell: "7", othercell: "5", proposedcell: "2", statuscell: "3"}
	 */
	var config = surveyConfig;

	var protoButton = null; // no voting at all, unless tr.canModify
	if (tr.canModify) {
		protoButton = dojo.byId('proto-button');
	}

	/*
	 * Update the "status cell", a.k.a. the "A" column.
	 */
	updateRowStatusCell(tr, theRow, children[config.statuscell]);

	/*
	 * Update part of the "no cell", cf. updateRowNoAbstainCell; should this code be moved to updateRowNoAbstainCell?
	 */
	if (theRow.hasVoted) {
		children[config.nocell].title = stui.voTrue;
		children[config.nocell].className = "d-no-vo-true";
	} else {
		children[config.nocell].title = stui.voFalse;
		children[config.nocell].className = "d-no-vo-false";
	}

	/*
	 * Assemble the "code cell", a.k.a. the "Code" column.
	 */
	if (config.codecell) {
		updateRowCodeCell(tr, theRow, children[config.codecell]);
	}

	/*
	 * Set up the "comparison cell", a.k.a. the "English" column.
	 */
	if (!children[config.comparisoncell].isSetup) {
		updateRowEnglishComparisonCell(tr, theRow, children[config.comparisoncell]);
	}

	/*
	 * Set up the "proposed cell", a.k.a. the "Winning" column.
	 * 
	 * Column headings are: Code    English    Abstain    A    Winning    Add    Others
	 * TODO: are we going out of order here, from English to Winning, skipping Abstain and A?
	 */
	updateRowProposedWinningCell(tr, theRow, children[config.proposedcell], protoButton);

	/*
	 * Set up the "err cell"
	 * 
	 * TODO: is this something that's normally hidden? Clarify.
	 * 
	 * "config.errcell" doesn't occur anywhere else so this may be dead code.
	 */
	if (config.errcell) {
		listenToPop(null, tr, children[config.errcell], children[config.proposedcell].showFn);
	}

	/*
	 *  add button
	 *  
	 * TODO: clarify usage of formAdd
	 */
	var formAdd = document.createElement("form");

	/*
	 * Set up the "other cell", a.k.a. the "Others" column.
	 */
	updateRowOthersCell(tr, theRow, children[config.othercell], protoButton, formAdd);

	/*
	 * If the user can make changes, add "+" button for adding new candidate item.
	 * 
	 * This code for Dashboard as well as the basic vetting table.
	 * This block concerns othercell if isDashboard(), otherwise it concerns addcell.
	 */
	if (tr.canChange) {
		if (isDashboard()) {
			children[config.othercell].appendChild(document.createElement('hr'));
			children[config.othercell].appendChild(formAdd); //add button
		} else {
			removeAllChildNodes(children[config.addcell]);
			children[config.addcell].appendChild(formAdd); //add button
		}
	}

	/*
	 * Set up the "no cell", a.k.a. the "Abstain" column.
	 * If the user can make changes, add an "abstain" button;
	 * else, possibly add a ticket link, or else hide the column.
	 */
	updateRowNoAbstainCell(tr, theRow, children[config.nocell], children[config.proposedcell], protoButton);

	/*
	 * Set className for this row to "vother" and "cov..." based on the coverage value.
	 * Elsewhere className can get values including "ferrbox", "tr_err", "tr_checking2".
	 */
	tr.className = 'vother cov' + theRow.coverageValue;

	/*
	 * Show the current ID.
	 * TODO: explain.
	 */
	if (surveyCurrentId !== '' && surveyCurrentId === tr.id) {
		window.showCurrentId(); // refresh again - to get the updated voting status.
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
	if (!theRow.winningVhash) {
		/*
		 * The server, not the client, is responsible for ensuring that a winning item is present.
		 */
		console.error('For ' + theRow.xpstrid + ' - there is no winningVhash');
	} else if (!theRow.items) {
		console.error('For ' + theRow.xpstrid + ' - there are no items');
	} else if (!theRow.items[theRow.winningVhash]) {
		console.error('For ' + theRow.xpstrid + ' - there is winningVhash but no item for it');
	} else {
		const item = theRow.items[theRow.winningVhash];
		if (item.value && item.value === ERROR_NO_WINNING_VALUE) {
			console.log('For ' + theRow.xpstrid + ' - there is ' + ERROR_NO_WINNING_VALUE);
		}
	}

	for (var k in theRow.items) {
		var item = theRow.items[k];
		if (item.value === INHERITANCE_MARKER) {
			if (!theRow.inheritedValue) {
				/*
				 * In earlier implementation, essentially the same error was reported as "... there is no Bailey Target item!")
				 * Reference: https://unicode.org/cldr/trac/ticket/11238
				 */
				console.error('For ' + theRow.xpstrid + ' - there is INHERITANCE_MARKER without inheritedValue');
			} else if (!theRow.inheritedLocale && !theRow.inheritedXpid) {
				/*
				 * It is probably a bug if item.value === INHERITANCE_MARKER but theRow.inheritedLocale and
				 * theRow.inheritedXpid are both undefined (null on server).
				 * This happens with "example C" in
				 *     https://unicode.org/cldr/trac/ticket/11299#comment:15
				 */
				console.log('For ' + theRow.xpstrid + ' - there is INHERITANCE_MARKER without inheritedLocale or inheritedXpid');
			}
		}
	}
}

/**
 * Update the "status cell", a.k.a. the "A" column.
 *
 * @param tr the table row
 * @param theRow the data from the server for this row
 * @param cell the table cell = children[config.statuscell]
 */
function updateRowStatusCell(tr, theRow, cell) {
	const statusClass = getRowApprovalStatusClass(theRow);
	cell.className = "d-dr-" + statusClass + " d-dr-status";

	if (!cell.isSetup) {
		listenToPop("", tr, cell);
		cell.isSetup = true;
	}

	const statusTitle = stui.str(statusClass);
	cell.title = stui.sub('draftStatus', [statusTitle]);
}

/**
 * On the client only, make further status distinctions when winning value is INHERITANCE_MARKER,
 * "inherited-unconfirmed" (red up-arrow icon) and "inherited-provisional" (orange up-arrow icon).
 * Reference: http://unicode.org/cldr/trac/ticket/11103
 *
 * @param theRow the data from the server for this row
 */
function getRowApprovalStatusClass(theRow) {
	var statusClass = theRow.confirmStatus;

	if (theRow.winningValue === INHERITANCE_MARKER) {
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
	var vr = theRow.voteResolver;
	var div = tr.voteDiv = document.createElement("div");
	tr.voteDiv.className = "voteDiv";
	if (theRow.voteVhash &&
		theRow.voteVhash !== '' && surveyUser) {
		var voteForItem = theRow.items[theRow.voteVhash];
		if (voteForItem && voteForItem.votes && voteForItem.votes[surveyUser.id] &&
			voteForItem.votes[surveyUser.id].overridedVotes) {
			tr.voteDiv.appendChild(createChunk(stui.sub("override_explain_msg", {
				overrideVotes: voteForItem.votes[surveyUser.id].overridedVotes,
				votes: surveyUser.votecount
			}), "p", "helpContent"));
		}
		if (theRow.voteVhash !== theRow.winningVhash &&
			theRow.canFlagOnLosing) {
			if (!theRow.rowFlagged) {
				var newIcon = addIcon(tr.voteDiv, "i-stop");
				tr.voteDiv.appendChild(createChunk(stui.sub("mustflag_explain_msg", {}), "p", "helpContent"));
			} else {
				var newIcon = addIcon(tr.voteDiv, "i-flag");
				tr.voteDiv.appendChild(createChunk(stui.str("flag_desc", "p", "helpContent")));
			}
		}
	}
	if (!theRow.rowFlagged && theRow.canFlagOnLosing) {
		var newIcon = addIcon(tr.voteDiv, "i-flag-d");
		tr.voteDiv.appendChild(createChunk(stui.str("flag_d_desc", "p", "helpContent")));
	}
	var haveWinner = false;
	var haveLast = false;
	// next, the org votes
	var perValueContainer = div; // IF NEEDED: >>  = document.createElement("div");  perValueContainer.className = "perValueContainer";
	var n = 0;
	while (n < vr.value_vote.length) {
		var value = vr.value_vote[n++];
		if (value == null) continue;
		var vote = vr.value_vote[n++];
		var item = tr.rawValueToItem[value]; // backlink to specific item in hash
		if (item == null) continue;
		var vdiv = createChunk(null, "table", "voteInfo_perValue table table-vote");
		if (n > 2) {
			var valdiv = createChunk(null, "div", "value-div");
		} else {
			var valdiv = createChunk(null, "div", "value-div first")
		}
		// heading row
		var vrow = createChunk(null, "tr", "voteInfo_tr voteInfo_tr_heading");
		if (item.rawValue === INHERITANCE_MARKER || (item.votes && Object.keys(item.votes).length > 0)) {
			vrow.appendChild(createChunk(stui.str("voteInfo_orgColumn"), "td", "voteInfo_orgColumn voteInfo_td"));
		}
		var isection = createChunk(null, "div", "voteInfo_iconBar");
		var isectionIsUsed = false;
		var vvalue = createChunk("User", "td", "voteInfo_valueTitle voteInfo_td");
		var vbadge = createChunk(vote, "span", "badge");

		/*
		 * Note: due to the existence of the function fixWinningValue in DataSection.java, here on the the client
		 * we should use theRow.winningValue, not vr.winningValue. Eventually VoteResolver.getWinningValue may be
		 * fixed in such a way that fixWinningValue isn't necessary and there won't be a distinction between
		 * theRow.winningValue and vr.winningValue. Cf. theRow.winningVhash.
		 * Note: we can't just check for item.pClass === "winner" here, since, for example, the winning value may
		 * have value = INHERITANCE_MARKER and item.pClass = "alias".
		 */
		if (value === theRow.winningValue) {
			const statusClass = getRowApprovalStatusClass(theRow);
			const statusTitle = stui.str(statusClass);
			appendIcon(isection, "voteInfo_winningItem d-dr-" + statusClass, stui.sub('draftStatus', [statusTitle]));
			isectionIsUsed = true;
		}

		/*
		 * For adding star for last release value, we could check item.isOldValue or (value == vr.lastReleaseValue);
		 * ideally the two should be consistent. The star icon should be applied to old value = inherited value when
		 * appropriate. Work is in progress on ticket 11299, whether item with value INHERITANCE_MARKER has isOldValue,
		 * whether vr.lastReleaseValue is ever INHERITANCE_MARKER. For flexibility, for now, show star if either of the
		 * conditions is true.
		 */
		if (value == vr.lastReleaseValue || item.isOldValue) {
			appendIcon(isection, "voteInfo_lastRelease i-star", stui.str("voteInfo_lastRelease_desc"));
			isectionIsUsed = true;
		}
		setLang(valdiv);
		if (value === INHERITANCE_MARKER) {
			appendItem(valdiv, theRow.inheritedValue, item.pClass, tr);
			valdiv.appendChild(createChunk(stui.str("voteInfo_votesForInheritance"), 'p'));
		} else {
			appendItem(valdiv, value, (value === theRow.winningValue) ? "winner" : "value", tr);
			if (value === theRow.inheritedValue) {
				valdiv.appendChild(createChunk(stui.str('voteInfo_votesForSpecificValue'), 'p'));
			}
		}
		if (isectionIsUsed) {
			valdiv.appendChild(isection);
		}
		vrow.appendChild(vvalue);
		var cell = createChunk(null, "td", "voteInfo_voteTitle voteInfo_voteCount voteInfo_td" + "");
		cell.appendChild(vbadge);
		vrow.appendChild(cell);
		vdiv.appendChild(vrow);
		const itemVotesLength = item.votes ? Object.keys(item.votes).length : 0;
		const anon = (itemVotesLength == 1 && item.votes[Object.keys(item.votes)[0]].level === 'anonymous');
		if (itemVotesLength == 0 || anon) {
			var vrow = createChunk(null, "tr", "voteInfo_tr voteInfo_orgHeading");
			vrow.appendChild(createChunk(stui.str("voteInfo_noVotes"), "td", "voteInfo_noVotes voteInfo_td"));
			const anonVoter = anon ? stui.str("voteInfo_anon") : null
			vrow.appendChild(createChunk(anonVoter, "td", "voteInfo_noVotes voteInfo_td"));
			vdiv.appendChild(vrow);
		} else {
			updateRowVoteInfoForAllOrgs(theRow, vr, value, item, vdiv);
		}
		perValueContainer.appendChild(valdiv);
		perValueContainer.appendChild(vdiv);
	}
	if (vr.requiredVotes) {
		var msg = stui.sub("explainRequiredVotes", {
			requiredVotes: vr.requiredVotes
		});
		perValueContainer.appendChild(createChunk(msg, "p", "alert alert-warning fix-popover-help"));
	}
	// done with voteresolver table
	if (stdebug_enabled) {
		tr.voteDiv.appendChild(createChunk(vr.raw, "p", "debugStuff"));
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
 * @param vdiv a table created by the caller as vdiv = createChunk(null, "table", "voteInfo_perValue table table-vote")
 */
function updateRowVoteInfoForAllOrgs(theRow, vr, value, item, vdiv) {
	var createVoter = function(v) {
		if (v == null) {
			return createChunk("(missing information)!", "i", "stopText");
		}
		var div = createChunk(v.name || stui.str('emailHidden'), "td", "voteInfo_voterInfo voteInfo_td");
		div.setAttribute('data-name', v.name || stui.str('emailHidden'));
		div.setAttribute('data-email', v.email || '');
		return div;
	};
	for (org in theRow.voteResolver.orgs) {
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
		if (orgVoteValue !== undefined) { // someone in the org actually voted for it
			var topVoter = null; // top voter for this item
			var orgsVote = (theOrg.orgVote == value);
			var topVoterTime = 0; // Calculating the latest time for a user from same org
			if (orgsVote) {
				// find a top-ranking voter to use for the top line
				for (var voter in item.votes) {
					if (item.votes[voter].org == org && item.votes[voter].votes == theOrg.votes[value]) {
						if (topVoterTime != 0) {
							// Get the latest time vote only
							if (vr.nameTime[item.votes[topVoter].name] < vr.nameTime[item.votes[voter].name]) {
								topVoter = voter;
								// console.log(item);
								// console.log(vr.nameTime[item.votes[topVoter].name]);
								topVoterTime = vr.nameTime[item.votes[topVoter].name];
							}
						} else {
							topVoter = voter;
							// console.log(item);
							// console.log(vr.nameTime[item.votes[topVoter].name]);
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
			 * There was some buggy code here, testing item.votes[topVoter].isVoteForBailey, but no element
			 * of the votes array could have had isVoteForBailey, which was a property of an "item" (CandidateItem)
			 * not a "vote" (based on UserRegistry.User -- see CandidateItem.toJSONString in DataSection.java)
			 * 
			 * item.votes[topVoter].isVoteForBailey was always undefined (effectively false), so baileyClass
			 * was always "" (empty string) here.
			 * 
			 * This has been fixed, to test item.rawValue === INHERITANCE_MARKER instead.
			 * 
			 * This only affects cells ("td" elements) with style "voteInfo_voteCount", which appear in the info panel,
			 * and which have contents like '<span class="badge">12</span>'. If the "fallback" style is added, then
			 * these circled numbers are surrounded (outside the circle) by a colored background.
			 * 
			 * TODO: see whether the colored background is actually wanted in this context, around the numbers.
			 * For now, display it, and use item.pClass rather than literal "fallback" so the color matches when
			 * item.pClass is "alias", "fallback_root", etc.
			 */
			var baileyClass = (item.rawValue === INHERITANCE_MARKER) ? " " + item.pClass : "";
			var vrow = createChunk(null, "tr", "voteInfo_tr voteInfo_orgHeading");
			vrow.appendChild(createChunk(org, "td", "voteInfo_orgColumn voteInfo_td"));
			if (item.votes[topVoter]) {
				vrow.appendChild(createVoter(item.votes[topVoter])); // voteInfo_td
			} else {
				vrow.appendChild(createVoter(null));
			}
			if (orgsVote) {
				var cell = createChunk(null, "td", "voteInfo_orgsVote voteInfo_voteCount voteInfo_td" + baileyClass);
				cell.appendChild(createChunk(orgVoteValue, "span", "badge"));
				vrow.appendChild(cell);
			} else {
				vrow.appendChild(createChunk(orgVoteValue, "td", "voteInfo_orgsNonVote voteInfo_voteCount voteInfo_td" + baileyClass));
			}
			vdiv.appendChild(vrow);
			// now, other rows:
			for (var voter in item.votes) {
				if (item.votes[voter].org != org || // wrong org or
					voter == topVoter) { // already done
					continue; // skip
				}
				// OTHER VOTER row
				var vrow = createChunk(null, "tr", "voteInfo_tr");
				vrow.appendChild(createChunk("", "td", "voteInfo_orgColumn voteInfo_td")); // spacer
				vrow.appendChild(createVoter(item.votes[voter])); // voteInfo_td
				vrow.appendChild(createChunk(item.votes[voter].votes, "td", "voteInfo_orgsNonVote voteInfo_voteCount voteInfo_td" + baileyClass));
				vdiv.appendChild(vrow);
			}
		}
	}
}

/*
 * Update the "Code" cell (column) of this row
 * 
 * @param tr the table row
 * @param theRow the data from the server for this row
 * @param cell the table cell, children[config.codecell]
 * 
 * Called by updateRow.
 */
function updateRowCodeCell(tr, theRow, cell) {
	removeAllChildNodes(cell);
	var codeStr = theRow.code;
	if (theRow.coverageValue == 101 && !stdebug_enabled) {
		codeStr = codeStr + " (optional)";
	}
	cell.appendChild(createChunk(codeStr));
	if (tr.theTable.json.canModify) { // pointless if can't modify.
		cell.className = "d-code";
		if (!tr.forumDiv) {
			tr.forumDiv = document.createElement("div");
			tr.forumDiv.className = "forumDiv";
		}
		appendForumStuff(tr, theRow, tr.forumDiv);
	}
	// extra attributes
	if (theRow.extraAttributes && Object.keys(theRow.extraAttributes).length > 0) {
		appendExtraAttributes(cell, theRow);
	}
	if (stdebug_enabled) {
		var anch = document.createElement("i");
		anch.className = "anch";
		anch.id = theRow.xpathId;
		cell.appendChild(anch);
		anch.appendChild(document.createTextNode("#"));
		var go = document.createElement("a");
		go.className = "anch-go";
		go.appendChild(document.createTextNode("zoom"));
		go.href = window.location.pathname + "?_=" + surveyCurrentLocale + "&x=r_rxt&xp=" + theRow.xpathId;
		cell.appendChild(go);
		var js = document.createElement("a");
		js.className = "anch-go";
		js.appendChild(document.createTextNode("{JSON}"));
		js.popParent = tr;
		listenToPop(JSON.stringify(theRow), tr, js);
		cell.appendChild(js);
		cell.appendChild(createChunk(" c=" + theRow.coverageValue));
	}
	if (!cell.isSetup) {
		var xpathStr = "";
		if (stdebug_enabled) {
			xpathStr = "XPath: " + theRow.xpath;
		}
		listenToPop(xpathStr, tr, cell);
		cell.isSetup = true;
	}
}

/**
 * Update the "comparison cell", a.k.a. the "English" column, of this row
 * 
 * @param tr the table row
 * @param theRow the data from the server for this row
 * @param cell the table cell, children[config.comparisoncell]
 * 
 * Called by updateRow.
 */
function updateRowEnglishComparisonCell(tr, theRow, cell) {
	if (theRow.displayName) {
		var hintPos = theRow.displayName.indexOf('[translation hint');
		var hasExample = false;
		if (theRow.displayExample) {
			hasExample = true;
		}
		if (hintPos != -1) {
			theRow.displayExample = theRow.displayName.substr(hintPos, theRow.displayName.length) + (theRow.displayExample ? theRow.displayExample.replace(/\[translation hint.*?\]/g, "") : '');
			theRow.displayName = theRow.displayName.substr(0, hintPos);
		}
		cell.appendChild(createChunk(theRow.displayName, 'span', 'subSpan'));
		setLang(cell, surveyBaselineLocale);
		if (theRow.displayExample) {
			appendExample(cell, theRow.displayExample, surveyBaselineLocale);
		}
		if (hintPos != -1 || hasExample) {
			var infos = document.createElement("div");
			infos.className = 'infos-code';
			if (hintPos != -1) {
				var img = document.createElement("img");
				img.src = 'hint.png';
				img.alt = 'Translation hint';
				infos.appendChild(img);
			}
			if (hasExample) {
				var img = document.createElement("img");
				img.src = 'example.png';
				img.alt = 'Example';
				infos.appendChild(img);
			}
			cell.appendChild(infos);
		}
	} else {
		cell.appendChild(document.createTextNode(""));
	}
	/* The next line (listenToPop...) had been commented out, for unknown reasons.
	 * Restored (uncommented) for http://unicode.org/cldr/trac/ticket/10573 so that
	 * the right-side panel info changes when you click on the English column.
	 */
	listenToPop(null, tr, cell);
	cell.isSetup = true;
}

/**
 * Update the "proposed cell", a.k.a. the "Winning" column, of this row
 * 
 * @param tr the table row
 * @param theRow the data from the server for this row
 * @param cell the table cell, children[config.proposedcell])
 * @param protoButton
 *
 * Called by updateRow.
 */
function updateRowProposedWinningCell(tr, theRow, cell, protoButton) {
	removeAllChildNodes(cell); // win
	if (theRow.rowFlagged) {
		var flagIcon = addIcon(cell, "s-flag");
		flagIcon.title = stui.str("flag_desc");
	} else if (theRow.canFlagOnLosing) {
		var flagIcon = addIcon(cell, "s-flag-d");
		flagIcon.title = stui.str("flag_d_desc");
	}
	setLang(cell);
	tr.proposedcell = cell;

	/*
	 * If server doesn't do its job properly, theRow.items[theRow.winningVhash] may be undefined.
	 * Check for that here to prevent crash in addVitem. An error message might be appropriate here
	 * in that case, though the consistency checking really should happen earlier, see checkRowConsistency.
	 */
	if (getValidWinningValue(theRow) !== null) {
		addVitem(cell, tr, theRow, theRow.items[theRow.winningVhash], cloneAnon(protoButton));
	} else {
		cell.showFn = function() {}; // nothing else to show
	}
	listenToPop(null, tr, cell, cell.showFn);
}

/*
 * Update the "Others" cell (column) of this row
 * 
 * @param tr the table row
 * @param theRow the data from the server for this row
 * @param cell, the table cell, children[config.othercell]
 * @param protoButton
 * @param formAdd
 * 
 * Called by updateRow.
 */
function updateRowOthersCell(tr, theRow, cell, protoButton, formAdd) {
	var hadOtherItems = false;
	removeAllChildNodes(cell); // other
	setLang(cell);

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
		input.placeholder = 'Add a translation';
		var copyWinning = document.createElement("button");
		copyWinning.className = "copyWinning btn btn-info btn-xs";
		copyWinning.title = "Copy Winning";
		copyWinning.type = "button";
		copyWinning.innerHTML = '<span class="glyphicon glyphicon-arrow-right"></span> Winning';
		copyWinning.onclick = function(e) {
			var theValue = getValidWinningValue(theRow);
			if (theValue === INHERITANCE_MARKER || theValue === null) {
				theValue = theRow.inheritedValue;
			}
			input.value = theValue || null;
			input.focus();
		}
		var copyEnglish = document.createElement("button");
		copyEnglish.className = "copyEnglish btn btn-info btn-xs";
		copyEnglish.title = "Copy English";
		copyEnglish.type = "button";
		copyEnglish.innerHTML = '<span class="glyphicon glyphicon-arrow-right"></span> English';
		copyEnglish.onclick = function(e) {
			input.value = theRow.displayName || null;
			input.focus();
		}
		btn.onclick = function(e) {
			//if no input, add one
			if ($(buttonAdd).parent().find('input').length == 0) {

				//hide other
				$.each($('button.vote-submit'), function() {
					toAddVoteButton(this);
				});

				//transform the button
				toSubmitVoteButton(btn);
				$(buttonAdd).popover({
					content: ' '
				}).popover('show');
				popup = $(buttonAdd).parent().find('.popover-content');
				popup.append(input);
				if (theRow.displayName) {
					popup.append(copyEnglish);
				}
				const winVal = getValidWinningValue(theRow);
				if (winVal || theRow.inheritedValue) {
					popup.append(copyWinning);
				}
				popup.closest('.popover').css('top', popup.closest('.popover').position().top - 19);
				input.focus();

				//enter pressed
				$(input).keydown(function(e) {
					var newValue = $(this).val();
					if (e.keyCode == 13) { //enter pressed
						if (newValue) {
							addValueVote(cell, tr, theRow, newValue, cloneAnon(protoButton));
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
					addValueVote(cell, tr, theRow, newValue, cloneAnon(protoButton));
				} else {
					toAddVoteButton(btn);
				}
				stStopPropagation(e);
				return false;
			}
			stStopPropagation(e);
			return false;
		};
	}
	/*
	 * Add the other vote info -- that is, vote info for the "Others" column.
	 */
	for (k in theRow.items) {
		if (k === theRow.winningVhash) { // skip vote for winner
			continue;
		}
		hadOtherItems = true;
		addVitem(cell, tr, theRow, theRow.items[k], cloneAnon(protoButton));
		cell.appendChild(document.createElement("hr"));
	}

	if (!hadOtherItems /*!onIE*/ ) {
		listenToPop(null, tr, cell);
	}
	if (tr.myProposal && tr.myProposal.value && !findItemByValue(theRow.items, tr.myProposal.value)) {
		// add back my proposal
		cell.appendChild(tr.myProposal);
	} else {
		tr.myProposal = null; // not needed
	}
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
 * @param noCell the table "no" (abstain) cell, children[config.nocell]
 * @param proposedCell the table "proposed" (winning) cell, children[config.proposedcell]
 * @param protoButton
 * 
 * Called by updateRow.
 */
function updateRowNoAbstainCell(tr, theRow, noCell, proposedCell, protoButton) {
	if (tr.canModify) {
		removeAllChildNodes(noCell); // no opinion
		var noOpinion = cloneAnon(protoButton);
		wireUpButton(noOpinion, tr, theRow, null);
		noOpinion.value = null;
		var wrap = wrapRadio(noOpinion);
		noCell.appendChild(wrap);
		listenToPop(null, tr, noCell);
	} else if (tr.ticketOnly) { // ticket link
		if (!tr.theTable.json.canModify) { // only if hidden in the header
			setDisplayed(noCell, false);
		}
		proposedCell.className = "d-change-confirmonly";
		var surlink = document.createElement("div");
		surlink.innerHTML = '<span class="glyphicon glyphicon-list-alt"></span>&nbsp;&nbsp;';
		surlink.className = 'alert alert-info fix-popover-help';
		var link = createChunk(stui.str("file_a_ticket"), "a");
		var newUrl = "http://unicode.org/cldr/trac" +
			"/newticket?component=data&summary=" + surveyCurrentLocale + ":" + theRow.xpath +
			"&locale=" + surveyCurrentLocale + "&xpath=" + theRow.xpstrid + "&version=" + surveyVersion;
		link.href = newUrl;
		link.target = "cldr-target-trac";
		theRow.proposedResults = createChunk(stui.str("file_ticket_must"), "a",
			"fnotebox");
		theRow.proposedResults.href = newUrl;
		if (!window.surveyOfficial) {
			link.appendChild(createChunk(
				" (Note: this is not the production SurveyTool! Do not submit a ticket!) ", "p"));
			link.href = link.href + "&description=NOT+PRODUCTION+SURVEYTOOL!";
		}
		proposedCell.appendChild(createChunk(stui.str("file_ticket_notice"), "i", "fnotebox"));
		surlink.appendChild(link);
		tr.ticketLink = surlink;
	} else { // no change possible
		if (!tr.theTable.json.canModify) { // only if hidden in the header
			setDisplayed(noCell, false);
		}
	}

	/**
	 * Get the winning value for the given row, if it's a valid value.
	 * Null and ERROR_NO_WINNING_VALUE ('error-no-winning-value') are not valid.
	 * See ERROR_NO_WINNING_VALUE in DataSection.java.
	 *
	 * @param theRow
	 * @returns the winning value, or null if there is not a valid winning value
	 */
	function getValidWinningValue(theRow) {
		if (theRow.items && theRow.winningVhash && theRow.items[theRow.winningVhash]) {
			const item = theRow.items[theRow.winningVhash];
			if (item.value) {
				const val = item.value;
				if (val !== ERROR_NO_WINNING_VALUE) {
					return val;
				}
			}
		}
		return null;
	}
}

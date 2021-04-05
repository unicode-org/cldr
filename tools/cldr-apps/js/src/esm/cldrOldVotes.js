/**
 * cldrOldVotes: encapsulate the Import Old Votes feature.
 */
import * as cldrAjax from "./cldrAjax.js";
import * as cldrDom from "./cldrDom.js";
import * as cldrEvent from "./cldrEvent.js";
import * as cldrLoad from "./cldrLoad.js";
import * as cldrRetry from "./cldrRetry.js";
import * as cldrStatus from "./cldrStatus.js";
import * as cldrSurvey from "./cldrSurvey.js";
import * as cldrText from "./cldrText.js";
import * as cldrVote from "./cldrVote.js";

// called as special.load
function load() {
  const curLocale = cldrStatus.getCurrentLocale();
  const url = getUrl(curLocale, false /* doSubmit */);
  cldrLoad.myLoad(url, "(loading oldvotes " + curLocale + ")", loadWithJson);
}

function loadWithJson(json) {
  cldrLoad.setLoading(false);
  cldrSurvey.showLoader(cldrText.get("loading2"));
  if (!cldrLoad.verifyJson(json, "oldvotes")) {
    return;
  }
  cldrSurvey.showLoader("loading..");
  if (json.dataLoadTime) {
    cldrDom.updateIf("dynload", json.dataLoadTime);
  }
  const theDiv = cldrLoad.flipToEmptyOther();
  cldrDom.removeAllChildNodes(theDiv);

  const h2txt = cldrText.get("v_oldvotes_title");
  theDiv.appendChild(cldrDom.createChunk(h2txt, "h2", "v-title"));

  if (!json.oldvotes.locale) {
    loadNoLocale(json, theDiv);
  } else {
    loadWithLocale(json, theDiv);
  }
  cldrSurvey.hideLoader();
}

function loadNoLocale(json, theDiv) {
  cldrStatus.setCurrentLocale("");
  cldrLoad.updateHashAndMenus(false);

  const ul = document.createElement("div");
  ul.className = "oldvotes_list";
  const data = json.oldvotes.locales.data;
  const header = json.oldvotes.locales.header;

  if (data.length > 0) {
    data.sort((a, b) => a[header.LOCALE].localeCompare(b[header.LOCALE]));
    for (let k in data) {
      const li = document.createElement("li");
      const link = cldrDom.createChunk(data[k][header.LOCALE_NAME], "a");
      link.href = "#" + data[k][header.LOCALE];
      (function (loc, link) {
        return function () {
          const clicky = function (e) {
            cldrStatus.setCurrentLocale(loc);
            cldrLoad.reloadV();
            cldrEvent.stopPropagation(e);
            return false;
          };
          cldrDom.listenFor(link, "click", clicky);
          link.onclick = clicky;
        };
      })(data[k][header.LOCALE], link)();
      li.appendChild(link);
      li.appendChild(cldrDom.createChunk(" "));
      li.appendChild(cldrDom.createChunk("(" + data[k][header.COUNT] + ")"));
      ul.appendChild(li);
    }
    theDiv.appendChild(ul);
    theDiv.appendChild(
      cldrDom.createChunk(
        cldrText.get("v_oldvotes_locale_list_help_msg"),
        "p",
        "helpContent"
      )
    );
  } else {
    theDiv.appendChild(
      cldrDom.createChunk(cldrText.get("v_oldvotes_no_old"), "i")
    );
  }
}

function loadWithLocale(json, theDiv) {
  cldrStatus.setCurrentLocale(json.oldvotes.locale);
  cldrLoad.updateHashAndMenus(false);
  const loclink = cldrDom.createChunk(
    cldrText.get("v_oldvotes_return_to_locale_list"),
    "a",
    "notselected"
  );
  theDiv.appendChild(loclink);
  cldrDom.listenFor(loclink, "click", function (e) {
    cldrStatus.setCurrentLocale("");
    cldrLoad.reloadV();
    cldrEvent.stopPropagation(e);
    return false;
  });
  theDiv.appendChild(
    cldrDom.createChunk(json.oldvotes.localeDisplayName, "h3", "v-title2")
  );
  const oldVotesLocaleMsg = document.createElement("p");
  oldVotesLocaleMsg.className = "helpContent";
  oldVotesLocaleMsg.innerHTML = cldrText.sub("v_oldvotes_locale_msg", {
    version: json.oldvotes.lastVoteVersion,
    locale: json.oldvotes.localeDisplayName,
  });
  theDiv.appendChild(oldVotesLocaleMsg);
  if (
    (json.oldvotes.contested && json.oldvotes.contested.length > 0) ||
    (json.oldvotes.uncontested && json.oldvotes.uncontested.length > 0)
  ) {
    reallyLoadWithLocale(json, theDiv);
  } else {
    const el = cldrDom.createChunk(
      cldrText.get("v_oldvotes_no_old_here"),
      "i",
      ""
    );
    theDiv.appendChild(el);
  }
}

function reallyLoadWithLocale(json, theDiv) {
  const oldVoteCount =
    (json.oldvotes.contested ? json.oldvotes.contested.length : 0) +
    (json.oldvotes.uncontested ? json.oldvotes.uncontested.length : 0);
  const summaryMsg = cldrText.sub("v_oldvotes_count_msg", {
    count: oldVoteCount,
  });
  const frag = document.createDocumentFragment();
  frag.appendChild(cldrDom.createChunk(summaryMsg, "div", ""));

  const navChunk = document.createElement("div");
  navChunk.className = "v-oldVotes-nav";
  frag.appendChild(navChunk);

  let winningChunk = null;
  let losingChunk = null;
  if (json.oldvotes.uncontested && json.oldvotes.uncontested.length > 0) {
    winningChunk = addOldvotesType(
      json,
      "uncontested",
      json.oldvotes.uncontested,
      navChunk
    );
    frag.appendChild(winningChunk);
  }
  if (json.oldvotes.contested && json.oldvotes.contested.length > 0) {
    losingChunk = addOldvotesType(
      json,
      "contested",
      json.oldvotes.contested,
      navChunk
    );
    frag.appendChild(losingChunk);
  }
  if (losingChunk == null && winningChunk != null) {
    cldrDom.setDisplayed(winningChunk, true); // only item
  } else if (losingChunk != null && winningChunk == null) {
    cldrDom.setDisplayed(losingChunk, true); // only item
  } else {
    addLosingAndWinningNavigation(navChunk, losingChunk, winningChunk);
  }
  theDiv.appendChild(frag);
}

function addOldvotesType(json, type, voteList, navChunk) {
  const content = cldrDom.createChunk("", "div", "v-oldVotes-subDiv");
  content.strid = "v_oldvotes_title_" + type; // v_oldvotes_title_contested or v_oldvotes_title_uncontested
  /*
   * Normally this interface is for old "losing" (contested) votes only, since old "winning" (uncontested) votes
   * are imported automatically. An exception is for TC users, for whom auto-import is disabled. The server-side
   * code leaves json.oldvotes.uncontested undefined except for TC users.
   * Show headings for "Winning/Losing" only if json.oldvotes.uncontested is defined and non-empty.
   */
  if (json.oldvotes.uncontested && json.oldvotes.uncontested.length > 0) {
    const title = cldrText.get(content.strid);
    content.title = title;
    content.appendChild(cldrDom.createChunk(title, "h2", "v-oldvotes-sub"));
  }
  content.appendChild(showVoteTable(voteList, type, json));
  const button = document.createElement("button");
  button.innerHTML = cldrText.get("v_submit_msg");
  content.appendChild(button);
  cldrDom.listenFor(button, "click", function () {
    button.innerHTML = cldrText.get("v_submit_busy");
    button.disabled = true;
    cldrDom.setDisplayed(navChunk, false);
    let confirmList = []; // these will be revoted with current params
    for (let kk in voteList) {
      if (voteList[kk].box.checked) {
        confirmList.push(voteList[kk].strid);
      }
    }
    const curLocale = cldrStatus.getCurrentLocale();
    const url = getUrl(curLocale, true /* doSubmit */);
    cldrLoad.myLoad(
      url,
      "(submitting oldvotes " + curLocale + ")",
      function (json) {
        cldrSurvey.showLoader(cldrText.get("loading2"));
        if (!cldrLoad.verifyJson(json, "oldvotes")) {
          cldrRetry.handleDisconnect("Error submitting votes!", json, "Error");
        } else {
          cldrLoad.reloadV();
        }
      },
      { confirmList: confirmList }
      // JSON.stringify(confirmList)
    );
  });
  // hide by default
  cldrDom.setDisplayed(content, false);
  return content;
}

function addLosingAndWinningNavigation(navChunk, losingChunk, winningChunk) {
  navChunk.appendChild(cldrDom.createChunk(cldrText.get("v_oldvotes_show")));
  navChunk.appendChild(
    cldrDom.createLinkToFn(
      winningChunk.strid,
      function () {
        cldrDom.setDisplayed(losingChunk, false);
        cldrDom.setDisplayed(winningChunk, true);
      },
      "button"
    )
  );
  navChunk.appendChild(
    cldrDom.createLinkToFn(
      losingChunk.strid,
      function () {
        cldrDom.setDisplayed(losingChunk, true);
        cldrDom.setDisplayed(winningChunk, false);
      },
      "button"
    )
  );
  losingChunk.appendChild(
    cldrDom.createLinkToFn(
      "v_oldvotes_hide",
      function () {
        cldrDom.setDisplayed(losingChunk, false);
      },
      "button"
    )
  );
  winningChunk.appendChild(
    cldrDom.createLinkToFn(
      "v_oldvotes_hide",
      function () {
        cldrDom.setDisplayed(winningChunk, false);
      },
      "button"
    )
  );
}

/**
 * Get a table showing old votes available for importing, along with
 * controls for choosing which votes to import.
 *
 * @param voteList the array of old votes
 * @param type "contested" for losing votes or "uncontested" for winning votes
 * @param json
 * @returns a new div element containing the table and controls
 *
 * Called only by addOldvotesType
 */
function showVoteTable(voteList, type, json) {
  const translationHintsLanguage = json.TRANS_HINT_LANGUAGE_NAME;
  const lastVoteVersion = json.oldvotes.lastVoteVersion;
  const voteTableDiv = document.createElement("div");
  const t = document.createElement("table");
  t.id = "oldVotesAcceptList";
  voteTableDiv.appendChild(t);
  const th = document.createElement("thead");
  const tb = document.createElement("tbody");
  const tr = document.createElement("tr");
  tr.appendChild(
    cldrDom.createChunk(cldrText.get("v_oldvotes_path"), "th", "code")
  );
  tr.appendChild(cldrDom.createChunk(translationHintsLanguage, "th", "v-comp"));
  tr.appendChild(
    cldrDom.createChunk(
      cldrText.sub("v_oldvotes_winning_msg", {
        version: lastVoteVersion,
      }),
      "th",
      "v-win"
    )
  );
  tr.appendChild(
    cldrDom.createChunk(cldrText.get("v_oldvotes_mine"), "th", "v-mine")
  );
  tr.appendChild(
    cldrDom.createChunk(cldrText.get("v_oldvotes_accept"), "th", "v-accept")
  );
  th.appendChild(tr);
  t.appendChild(th);
  const mainCategories = loopThroughVoteList(voteList, type, json, tb);
  t.appendChild(tb);
  addImportVotesFooter(voteTableDiv, voteList, mainCategories);
  return voteTableDiv;
}

function loopThroughVoteList(voteList, type, json, tb) {
  let oldSplit = [];
  let mainCategories = [];
  for (let k in voteList) {
    const row = voteList[k];
    const tr = document.createElement("tr");

    // delete common substring
    const pathSplit = row.pathHeader.split("	");
    let nn = 0;
    for (nn in pathSplit) {
      if (pathSplit[nn] != oldSplit[nn]) {
        break;
      }
    }
    if (nn != pathSplit.length - 1) {
      // need a header row.
      const trh = document.createElement("tr");
      trh.className = "subheading";
      const tdh = document.createElement("th");
      tdh.colSpan = 5;
      for (nn in pathSplit) {
        if (nn < pathSplit.length - 1) {
          tdh.appendChild(
            cldrDom.createChunk(pathSplit[nn], "span", "pathChunk")
          );
        }
      }
      trh.appendChild(tdh);
      tb.appendChild(trh);
    }
    if (mainCategories.indexOf(pathSplit[0]) === -1) {
      mainCategories.push(pathSplit[0]);
    }
    oldSplit = pathSplit;
    const rowTitle = pathSplit[pathSplit.length - 1];
    const tdp = cldrDom.createChunk("", "td", "v-path");
    const dtpl = cldrDom.createChunk(rowTitle, "a");
    dtpl.href = "v#/" + cldrStatus.getCurrentLocale() + "//" + row.strid;
    dtpl.target = "_CLDR_ST_view";
    tdp.appendChild(dtpl);
    tr.appendChild(tdp);
    const td00 = cldrDom.createChunk(row.baseValue, "td", "v-comp"); // english
    tr.appendChild(td00);
    const td0 = cldrDom.createChunk("", "td", "v-win");
    if (row.winValue) {
      const span0 = cldrVote.appendItem(td0, row.winValue, "winner");
      span0.dir = json.oldvotes.dir;
    }
    tr.appendChild(td0);
    const td1 = cldrDom.createChunk("", "td", "v-mine");
    const label = cldrDom.createChunk("", "label", "");
    const span1 = cldrVote.appendItem(label, row.myValue, "value");
    td1.appendChild(label);
    span1.dir = json.oldvotes.dir;
    tr.appendChild(td1);
    const td2 = cldrDom.createChunk("", "td", "v-accept");
    const box = cldrDom.createChunk("", "input", "");
    box.type = "checkbox";
    if (type == "uncontested") {
      // uncontested true by default
      box.checked = true;
    }
    row.box = box; // backlink
    td2.appendChild(box);
    tr.appendChild(td2);

    (function (tr, box, tdp) {
      return function () {
        // allow click anywhere
        cldrDom.listenFor(tr, "click", function (e) {
          box.checked = !box.checked;
          cldrEvent.stopPropagation(e);
          return false;
        });
        // .. but not on the path.  Also listen to the box and do nothing
        cldrDom.listenFor([tdp, box], "click", function (e) {
          cldrEvent.stopPropagation(e);
          return false;
        });
      };
    })(tr, box, tdp)();

    tb.appendChild(tr);
  }
  return mainCategories;
}

/**
 * Add to the given div a footer with buttons for choosing all or none
 * of the old votes, and with checkboxes for choosing all or none within
 * each of two or more main categories such as "Locale Display Names".
 *
 * @param voteTableDiv the div to add to
 * @param voteList the list of old votes
 * @param mainCategories the list of main categories
 *
 * Called only by showVoteTable
 *
 * Reference: https://unicode.org/cldr/trac/ticket/11517
 */
function addImportVotesFooter(voteTableDiv, voteList, mainCategories) {
  voteTableDiv.appendChild(
    cldrDom.createLinkToFn(
      "v_oldvotes_all",
      function () {
        for (let k in voteList) {
          voteList[k].box.checked = true;
        }
        for (let cat in mainCategories) {
          $("#cat" + cat).prop("checked", true);
        }
      },
      "button"
    )
  );

  voteTableDiv.appendChild(
    cldrDom.createLinkToFn(
      "v_oldvotes_none",
      function () {
        for (let k in voteList) {
          voteList[k].box.checked = false;
        }
        for (let cat in mainCategories) {
          $("#cat" + cat).prop("checked", false);
        }
      },
      "button"
    )
  );
  if (mainCategories.length > 1) {
    addCategoryCheckboxes(mainCategories, voteList);
  }
}

function addCategoryCheckboxes(mainCategories, voteList) {
  voteTableDiv.appendChild(
    document.createTextNode(cldrText.get("v_oldvotes_all_section"))
  );
  for (let cat in mainCategories) {
    const mainCat = mainCategories[cat];
    const checkbox = document.createElement("input");
    checkbox.type = "checkbox";
    checkbox.id = "cat" + cat;
    voteTableDiv.appendChild(checkbox);
    voteTableDiv.appendChild(document.createTextNode(mainCat + " "));
    cldrDom.listenFor(checkbox, "click", function (e) {
      for (let k in voteList) {
        const row = voteList[k];
        if (row.pathHeader.startsWith(mainCat)) {
          row.box.checked = this.checked;
        }
      }
      cldrEvent.stopPropagation(e);
      return false;
    });
  }
}

function getUrl(curLocale, doSubmit) {
  const p = new URLSearchParams();
  p.append("what", "oldvotes"); // cf. WHAT_OLDVOTES in SurveyAjax.java
  p.append("_", curLocale);
  if (doSubmit) {
    p.append("doSubmit", "true");
  }
  p.append("s", cldrStatus.getSessionId());
  p.append("cacheKill", cldrSurvey.cacheBuster());
  return cldrAjax.makeUrl(p);
}

export { load };

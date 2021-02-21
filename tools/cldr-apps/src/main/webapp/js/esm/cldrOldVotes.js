/**
 * cldrOldVotes: encapsulate the Import Old Votes feature.
 */
import * as cldrDom from "./cldrDom.js";
import * as cldrEvent from "./cldrEvent.js";
import * as cldrLoad from "./cldrLoad.js";
import * as cldrRetry from "./cldrRetry.js";
import * as cldrStatus from "./cldrStatus.js";
import * as cldrSurvey from "./cldrSurvey.js";
import * as cldrText from "./cldrText.js";

// called as special.load
function load() {
  // TODO: shorten this function by means of subroutines
  const curLocale = cldrStatus.getCurrentLocale();
  const url =
    cldrStatus.getContextPath() +
    "/SurveyAjax?what=oldvotes&_=" +
    curLocale +
    "&s=" +
    cldrStatus.getSessionId() +
    "&" +
    cldrSurvey.cacheKill();
  cldrLoad.myLoad(url, "(loading oldvotes " + curLocale + ")", function (json) {
    cldrLoad.setLoading(false);
    cldrSurvey.showLoader(cldrText.get("loading2"));
    if (!cldrLoad.verifyJson(json, "oldvotes")) {
      return;
    } else {
      cldrSurvey.showLoader("loading..");
      if (json.dataLoadTime) {
        cldrDom.updateIf("dynload", json.dataLoadTime);
      }
      // clean slate, and proceed
      const theDiv = cldrLoad.flipToEmptyOther();
      cldrDom.removeAllChildNodes(theDiv);

      const h2txt = cldrText.get("v_oldvotes_title");
      theDiv.appendChild(cldrDom.createChunk(h2txt, "h2", "v-title"));

      if (!json.oldvotes.locale) {
        cldrStatus.setCurrentLocale("");
        cldrLoad.updateHashAndMenus(false);

        const ul = document.createElement("div");
        ul.className = "oldvotes_list";
        const data = json.oldvotes.locales.data;
        const header = json.oldvotes.locales.header;

        if (data.length > 0) {
          data.sort((a, b) => a[header.LOCALE].localeCompare(b[header.LOCALE]));
          for (let k in data) {
            var li = document.createElement("li");

            var link = cldrDom.createChunk(data[k][header.LOCALE_NAME], "a");
            link.href = "#" + data[k][header.LOCALE];
            (function (loc, link) {
              return function () {
                var clicky;
                cldrDom.listenFor(
                  link,
                  "click",
                  (clicky = function (e) {
                    cldrStatus.setCurrentLocale(loc);
                    cldrLoad.reloadV();
                    cldrEvent.stopPropagation(e);
                    return false;
                  })
                );
                link.onclick = clicky;
              };
            })(data[k][header.LOCALE], link)();
            li.appendChild(link);
            li.appendChild(cldrDom.createChunk(" "));
            li.appendChild(
              cldrDom.createChunk("(" + data[k][header.COUNT] + ")")
            );

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
          ); // TODO fix
        }
      } else {
        cldrStatus.setCurrentLocale(json.oldvotes.locale);
        cldrLoad.updateHashAndMenus(false);
        var loclink;
        theDiv.appendChild(
          (loclink = cldrDom.createChunk(
            cldrText.get("v_oldvotes_return_to_locale_list"),
            "a",
            "notselected"
          ))
        );
        cldrDom.listenFor(loclink, "click", function (e) {
          cldrStatus.setCurrentLocale("");
          cldrLoad.reloadV();
          cldrEvent.stopPropagation(e);
          return false;
        });
        theDiv.appendChild(
          cldrDom.createChunk(json.oldvotes.localeDisplayName, "h3", "v-title2")
        );
        var oldVotesLocaleMsg = document.createElement("p");
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
          var frag = document.createDocumentFragment();
          const oldVoteCount =
            (json.oldvotes.contested ? json.oldvotes.contested.length : 0) +
            (json.oldvotes.uncontested ? json.oldvotes.uncontested.length : 0);
          var summaryMsg = cldrText.sub("v_oldvotes_count_msg", {
            count: oldVoteCount,
          });
          frag.appendChild(cldrDom.createChunk(summaryMsg, "div", ""));

          var navChunk = document.createElement("div");
          navChunk.className = "v-oldVotes-nav";
          frag.appendChild(navChunk);

          var uncontestedChunk = null;
          var contestedChunk = null;

          function addOldvotesType(type, jsondata, frag, navChunk) {
            var content = cldrDom.createChunk("", "div", "v-oldVotes-subDiv");
            content.strid = "v_oldvotes_title_" + type; // v_oldvotes_title_contested or v_oldvotes_title_uncontested

            /* Normally this interface is for old "losing" (contested) votes only, since old "winning" (uncontested) votes
             * are imported automatically. An exception is for TC users, for whom auto-import is disabled. The server-side
             * code leaves json.oldvotes.uncontested undefined except for TC users.
             * Show headings for "Winning/Losing" only if json.oldvotes.uncontested is defined and non-empty.
             */
            if (
              json.oldvotes.uncontested &&
              json.oldvotes.uncontested.length > 0
            ) {
              var title = cldrText.get(content.strid);
              content.title = title;
              content.appendChild(
                cldrDom.createChunk(title, "h2", "v-oldvotes-sub")
              );
            }

            content.appendChild(
              showVoteTable(jsondata /* voteList */, type, json)
            );

            const button = document.createElement("button");
            button.innerHTML = cldrText.get("v_submit_msg");
            content.appendChild(button);
            cldrDom.listenFor(button, "click", function (e) {
              button.innerHTML = cldrText.get("v_submit_busy");
              button.disabled = true;
              cldrDom.setDisplayed(navChunk, false);
              var confirmList = []; // these will be revoted with current params

              // explicit confirm list -  save us desync hassle
              for (var kk in jsondata) {
                if (jsondata[kk].box.checked) {
                  confirmList.push(jsondata[kk].strid);
                }
              }

              var saveList = {
                locale: cldrStatus.getCurrentLocale(),
                confirmList: confirmList,
              };

              console.log(saveList.toString());
              console.log(
                "Submitting " + type + " " + confirmList.length + " for confirm"
              );
              const curLocale = cldrStatus.getCurrentLocale();
              var url =
                cldrStatus.getContextPath() +
                "/SurveyAjax?what=oldvotes&_=" +
                curLocale +
                "&s=" +
                cldrStatus.getSessionId() +
                "&doSubmit=true&" +
                cldrSurvey.cacheKill();
              cldrLoad.myLoad(
                url,
                "(submitting oldvotes " + curLocale + ")",
                function (json) {
                  cldrSurvey.showLoader(cldrText.get("loading2"));
                  if (!cldrLoad.verifyJson(json, "oldvotes")) {
                    cldrRetry.handleDisconnect(
                      "Error submitting votes!",
                      json,
                      "Error"
                    );
                    return;
                  } else {
                    cldrLoad.reloadV();
                  }
                },
                JSON.stringify(saveList),
                {
                  "Content-Type": "application/json",
                }
              );
            });

            // hide by default
            cldrDom.setDisplayed(content, false);

            frag.appendChild(content);
            return content;
          }

          if (
            json.oldvotes.uncontested &&
            json.oldvotes.uncontested.length > 0
          ) {
            uncontestedChunk = addOldvotesType(
              "uncontested",
              json.oldvotes.uncontested,
              frag,
              navChunk
            );
          }
          if (json.oldvotes.contested && json.oldvotes.contested.length > 0) {
            contestedChunk = addOldvotesType(
              "contested",
              json.oldvotes.contested,
              frag,
              navChunk
            );
          }

          if (contestedChunk == null && uncontestedChunk != null) {
            cldrDom.setDisplayed(uncontestedChunk, true); // only item
          } else if (contestedChunk != null && uncontestedChunk == null) {
            cldrDom.setDisplayed(contestedChunk, true); // only item
          } else {
            // navigation
            navChunk.appendChild(
              cldrDom.createChunk(cldrText.get("v_oldvotes_show"))
            );
            navChunk.appendChild(
              cldrDom.createLinkToFn(
                uncontestedChunk.strid,
                function () {
                  cldrDom.setDisplayed(contestedChunk, false);
                  cldrDom.setDisplayed(uncontestedChunk, true);
                },
                "button"
              )
            );
            navChunk.appendChild(
              cldrDom.createLinkToFn(
                contestedChunk.strid,
                function () {
                  cldrDom.setDisplayed(contestedChunk, true);
                  cldrDom.setDisplayed(uncontestedChunk, false);
                },
                "button"
              )
            );

            contestedChunk.appendChild(
              cldrDom.createLinkToFn(
                "v_oldvotes_hide",
                function () {
                  cldrDom.setDisplayed(contestedChunk, false);
                },
                "button"
              )
            );
            uncontestedChunk.appendChild(
              cldrDom.createLinkToFn(
                "v_oldvotes_hide",
                function () {
                  cldrDom.setDisplayed(uncontestedChunk, false);
                },
                "button"
              )
            );
          }
          theDiv.appendChild(frag);
        } else {
          theDiv.appendChild(
            cldrDom.createChunk(cldrText.get("v_oldvotes_no_old_here"), "i", "")
          );
        }
      }
    }
    cldrSurvey.hideLoader();
  });
}

/**
 * Get a table showing old votes available for importing, along with
 * controls for choosing which votes to import.
 *
 * @param voteList the array of old votes
 * @param type "contested" for losing votes or "uncontested" for winning votes
 * @param translationHintsLanguage a string indicating the translation hints language, generally "English"
 * @param dir the direction, such as "ltr" for left-to-right
 * @returns a new div element containing the table and controls
 *
 * Called only by addOldvotesType
 * TODO: move this to js
 */
function showVoteTable(voteList, type, json) {
  let translationHintsLanguage = json.TRANS_HINT_LANGUAGE_NAME;
  let dir = json.oldvotes.dir;
  let lastVoteVersion = json.oldvotes.lastVoteVersion;

  var voteTableDiv = document.createElement("div");
  var t = document.createElement("table");
  t.id = "oldVotesAcceptList";
  voteTableDiv.appendChild(t);
  var th = document.createElement("thead");
  var tb = document.createElement("tbody");
  var tr = document.createElement("tr");
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
  var oldSplit = [];
  var mainCategories = [];
  for (var k in voteList) {
    var row = voteList[k];
    var tr = document.createElement("tr");
    var tdp;
    var rowTitle = "";

    // delete common substring
    var pathSplit = row.pathHeader.split("	");
    for (var nn in pathSplit) {
      if (pathSplit[nn] != oldSplit[nn]) {
        break;
      }
    }
    if (nn != pathSplit.length - 1) {
      // need a header row.
      var trh = document.createElement("tr");
      trh.className = "subheading";
      var tdh = document.createElement("th");
      tdh.colSpan = 5;
      for (var nn in pathSplit) {
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
    rowTitle = pathSplit[pathSplit.length - 1];

    tdp = cldrDom.createChunk("", "td", "v-path");

    var dtpl = cldrDom.createChunk(rowTitle, "a");
    dtpl.href = "v#/" + cldrStatus.getCurrentLocale() + "//" + row.strid;
    dtpl.target = "_CLDR_ST_view";
    tdp.appendChild(dtpl);

    tr.appendChild(tdp);
    var td00 = cldrDom.createChunk(row.baseValue, "td", "v-comp"); // english
    tr.appendChild(td00);
    var td0 = cldrDom.createChunk("", "td", "v-win");
    if (row.winValue) {
      var span0 = cldrSurvey.appendItem(td0, row.winValue, "winner");
      span0.dir = dir;
    }
    tr.appendChild(td0);
    var td1 = cldrDom.createChunk("", "td", "v-mine");
    var label = cldrDom.createChunk("", "label", "");
    var span1 = cldrSurvey.appendItem(label, row.myValue, "value");
    td1.appendChild(label);
    span1.dir = dir;
    tr.appendChild(td1);
    var td2 = cldrDom.createChunk("", "td", "v-accept");
    var box = cldrDom.createChunk("", "input", "");
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
  t.appendChild(tb);
  addImportVotesFooter(voteTableDiv, voteList, mainCategories);
  return voteTableDiv;
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
        for (var k in voteList) {
          voteList[k].box.checked = true;
        }
        for (var cat in mainCategories) {
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
        for (var k in voteList) {
          voteList[k].box.checked = false;
        }
        for (var cat in mainCategories) {
          $("#cat" + cat).prop("checked", false);
        }
      },
      "button"
    )
  );

  if (mainCategories.length > 1) {
    voteTableDiv.appendChild(
      document.createTextNode(cldrText.get("v_oldvotes_all_section"))
    );
    for (var cat in mainCategories) {
      let mainCat = mainCategories[cat];
      var checkbox = document.createElement("input");
      checkbox.type = "checkbox";
      checkbox.id = "cat" + cat;
      voteTableDiv.appendChild(checkbox);
      voteTableDiv.appendChild(document.createTextNode(mainCat + " "));
      cldrDom.listenFor(checkbox, "click", function (e) {
        for (var k in voteList) {
          var row = voteList[k];
          if (row.pathHeader.startsWith(mainCat)) {
            row.box.checked = this.checked;
          }
        }
        cldrEvent.stopPropagation(e);
        return false;
      });
    }
  }
}

export { load };

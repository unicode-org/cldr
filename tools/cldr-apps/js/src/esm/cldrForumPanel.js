/*
 * cldrForumPanel: encapsulate Survey Tool Forum Info Panel code.
 */
import * as cldrAjax from "./cldrAjax.js";
import * as cldrDom from "./cldrDom.js";
import * as cldrEvent from "./cldrEvent.js";
import * as cldrForum from "./cldrForum.js";
import * as cldrInfo from "./cldrInfo.js";
import * as cldrLoad from "./cldrLoad.js";
import * as cldrRetry from "./cldrRetry.js";
import * as cldrStatus from "./cldrStatus.js";
import * as cldrSurvey from "./cldrSurvey.js";
import * as cldrText from "./cldrText.js";

/**
 * Array storing all only-1 sublocale
 */
let oneLocales = [];

/**
 * Timeout for showing sideways view
 */
let sidewaysShowTimeout = -1;

/**
 * Called when showing the popup (i.e., the Info Panel) each time
 *
 * @param {Node} frag
 * @param {Node} forumDivClone = tr.forumDiv.cloneNode(true)
 * @param {Node} tr
 *
 * TODO: shorten this function (use subroutines)
 *
 * Formerly known as cldrSurvey.showForumStuff
 */
function loadInfo(frag, forumDivClone, tr) {
  let isOneLocale = false;
  if (oneLocales[cldrStatus.getCurrentLocale()]) {
    isOneLocale = true;
  }
  if (!isOneLocale) {
    const sidewaysControl = cldrDom.createChunk(
      cldrText.get("sideways_loading0"),
      "div",
      "sidewaysArea"
    );
    frag.appendChild(sidewaysControl);

    function clearMyTimeout() {
      if (sidewaysShowTimeout != -1) {
        // https://www.w3schools.com/jsref/met_win_clearinterval.asp
        window.clearInterval(sidewaysShowTimeout);
        sidewaysShowTimeout = -1;
      }
    }
    clearMyTimeout();
    sidewaysShowTimeout = window.setTimeout(function () {
      clearMyTimeout();
      cldrDom.updateIf(sidewaysControl, cldrText.get("sideways_loading1"));
      const curLocale = cldrStatus.getCurrentLocale();
      if (!curLocale) {
        return;
      }
      var url =
        cldrStatus.getContextPath() +
        "/SurveyAjax?what=getsideways&_=" +
        curLocale +
        "&s=" +
        cldrStatus.getSessionId() +
        "&xpath=" +
        tr.theRow.xpstrid +
        cldrSurvey.cacheKill();
      cldrLoad.myLoad(url, "sidewaysView", function (json) {
        /*
         * Count the number of unique locales in json.others and json.novalue.
         */
        var relatedLocales = json.novalue.slice();
        for (var s in json.others) {
          for (var t in json.others[s]) {
            relatedLocales[json.others[s][t]] = true;
          }
        }
        // if there is 1 sublocale (+ 1 default), we do nothing
        if (Object.keys(relatedLocales).length <= 2) {
          oneLocales[cldrStatus.getCurrentLocale()] = true;
          cldrDom.updateIf(sidewaysControl, "");
        } else {
          if (!json.others) {
            cldrDom.updateIf(sidewaysControl, ""); // no sibling locales (or all null?)
          } else {
            cldrDom.updateIf(sidewaysControl, ""); // remove string

            var topLocale = json.topLocale;
            const locmap = cldrLoad.getTheLocaleMap();
            var curLocale = locmap.getRegionAndOrVariantName(topLocale);
            var readLocale = null;

            // merge the read-only sublocale to base locale
            var mergeReadBase = function mergeReadBase(list) {
              var baseValue = null;
              // find the base locale, remove it and store its value
              for (var l = 0; l < list.length; l++) {
                var loc = list[l][0];
                if (loc === topLocale) {
                  baseValue = list[l][1];
                  list.splice(l, 1);
                  break;
                }
              }

              // replace the default locale(read-only) with base locale, store its name for label
              for (var l = 0; l < list.length; l++) {
                var loc = list[l][0];
                var bund = locmap.getLocaleInfo(loc);
                if (bund && bund.readonly) {
                  readLocale = locmap.getRegionAndOrVariantName(loc);
                  list[l][0] = topLocale;
                  list[l][1] = baseValue;
                  break;
                }
              }
            };

            // compare all sublocale values
            var appendLocaleList = function appendLocaleList(list, curValue) {
              var group = document.createElement("optGroup");
              var br = document.createElement("optGroup");
              group.appendChild(br);

              group.setAttribute("label", "Regional Variants for " + curLocale);
              group.setAttribute("title", "Regional Variants for " + curLocale);

              var escape = "\u00A0\u00A0\u00A0";
              var unequalSign = "\u2260\u00A0";

              for (var l = 0; l < list.length; l++) {
                var loc = list[l][0];
                var title = list[l][1];
                var item = document.createElement("option");
                item.setAttribute("value", loc);
                if (title == null) {
                  item.setAttribute("title", "undefined");
                } else {
                  item.setAttribute("title", title);
                }

                var str = locmap.getRegionAndOrVariantName(loc);
                if (loc === topLocale) {
                  str = str + " (= " + readLocale + ")";
                }

                if (loc === cldrStatus.getCurrentLocale()) {
                  str = escape + str;
                  item.setAttribute("selected", "selected");
                  item.setAttribute("disabled", "disabled");
                } else if (title != curValue) {
                  str = unequalSign + str;
                } else {
                  str = escape + str;
                }
                item.appendChild(document.createTextNode(str));
                group.appendChild(item);
              }
              popupSelect.appendChild(group);
            };

            var dataList = [];

            var popupSelect = document.createElement("select");
            for (var s in json.others) {
              for (var t in json.others[s]) {
                dataList.push([json.others[s][t], s]);
              }
            }

            /*
             * Set curValue = the value for cldrStatus.getCurrentLocale()
             */
            var curValue = null;
            for (let l = 0; l < dataList.length; l++) {
              var loc = dataList[l][0];
              if (loc === cldrStatus.getCurrentLocale()) {
                curValue = dataList[l][1];
                break;
              }
            }
            /*
             * Force the use of unequalSign in the regional comparison pop-up for locales in
             * json.novalue, by assigning a value that's different from curValue.
             */
            if (json.novalue) {
              const differentValue = curValue === "A" ? "B" : "A"; // anything different from curValue
              for (s in json.novalue) {
                dataList.push([json.novalue[s], differentValue]);
              }
            }
            mergeReadBase(dataList);

            // then sort by sublocale name
            dataList = dataList.sort(function (a, b) {
              return (
                locmap.getRegionAndOrVariantName(a[0]) >
                locmap.getRegionAndOrVariantName(b[0])
              );
            });
            appendLocaleList(dataList, curValue);

            var group = document.createElement("optGroup");
            popupSelect.appendChild(group);

            cldrDom.listenFor(popupSelect, "change", function (e) {
              var newLoc = popupSelect.value;
              if (newLoc !== cldrStatus.getCurrentLocale()) {
                cldrStatus.setCurrentLocale(newLoc);
                cldrLoad.reloadV();
              }
              return cldrEvent.stopPropagation(e);
            });

            sidewaysControl.appendChild(popupSelect);
          }
        }
      });
    }, 2000); // wait 2 seconds before loading this.
  }

  // Still deep inside the very long function loadInfo...

  if (tr.theRow) {
    const theRow = tr.theRow;
    const couldFlag =
      theRow.canFlagOnLosing &&
      theRow.voteVhash !== theRow.winningVhash &&
      theRow.voteVhash !== "" &&
      !theRow.rowFlagged;
    const myValue = theRow.hasVoted ? getUsersValue(theRow) : null;
    cldrForum.addNewPostButtons(
      frag,
      cldrStatus.getCurrentLocale(),
      couldFlag,
      theRow.xpstrid,
      theRow.code,
      myValue
    );
  }

  let loader2 = cldrDom.createChunk(cldrText.get("loading"), "i");
  frag.appendChild(loader2);

  /**
   * @param {Integer} nrPosts
   */
  function havePosts(nrPosts) {
    cldrDom.setDisplayed(loader2, false); // not needed
    tr.forumDiv.forumPosts = nrPosts;

    if (nrPosts == 0) {
      return; // nothing to do,
    }

    var showButton = cldrDom.createChunk(
      "Show " + tr.forumDiv.forumPosts + " posts",
      "button",
      "forumShow"
    );

    forumDivClone.appendChild(showButton);

    var theListen = function (e) {
      cldrDom.setDisplayed(showButton, false);
      updatePosts(tr);
      cldrEvent.stopPropagation(e);
      return false;
    };
    cldrDom.listenFor(showButton, "click", theListen);
    cldrDom.listenFor(showButton, "mouseover", theListen);
  }

  // still in the very long function loadInfo...
  // lazy load post count!
  // load async
  let ourUrl = tr.forumDiv.url + "&what=forum_count" + cldrSurvey.cacheKill();
  window.setTimeout(function () {
    var xhrArgs = {
      url: ourUrl,
      handleAs: "json",
      load: function (json) {
        if (json && json.forum_count !== undefined) {
          havePosts(parseInt(json.forum_count));
        } else {
          console.log("Some error loading post count??");
        }
      },
    };
    cldrAjax.sendXhr(xhrArgs);
  }, 1900);
} // end of the very long function loadInfo

function getUsersValue(theRow) {
  const surveyUser = cldrStatus.getSurveyUser();
  if (surveyUser && surveyUser.id) {
    if (theRow.voteVhash && theRow.voteVhash !== "") {
      const item = theRow.items[theRow.voteVhash];
      if (item && item.votes && item.votes[surveyUser.id]) {
        if (item.value === cldrSurvey.INHERITANCE_MARKER) {
          return theRow.inheritedValue;
        }
        return item.value;
      }
    }
  }
  return null;
}

/**
 * Update the forum posts in the Info Panel
 *
 * This includes the version of the Info Panel displayed in the Dashboard "Fix" window
 *
 * @param tr the table-row element with which the forum posts are associated,
 *		and whose info is shown in the Info Panel; or null, to get the
 *		tr from surveyCurrentId
 */
function updatePosts(tr) {
  if (!tr) {
    if (cldrStatus.getCurrentId() !== "") {
      /*
       * TODO: encapsulate this usage of 'r@' somewhere
       */
      tr = document.getElementById("r@" + cldrStatus.getCurrentId());
    } else {
      /*
       * This is normal when adding a post in the main forum interface, which has no Info Panel).
       */
      return;
    }
  }
  if (!tr || !tr.forumDiv || !tr.forumDiv.url) {
    /*
     * This is normal for updatePosts(null) called by success handler
     * for submitPost, from Dashboard, since Fix window is no longer open
     */
    return;
  }
  let ourUrl = tr.forumDiv.url + "&what=forum_fetch";

  let errorHandler = function (err) {
    console.log("Error in updatePosts: " + err);
    const message =
      cldrStatus.stopIcon() +
      " Couldn't load forum post for this row- please refresh the page. <br>Error: " +
      err +
      "</td>";
    cldrInfo.showWithRow(message, tr);
    cldrRetry.handleDisconnect("Could not load for updatePosts:" + err, null);
  };

  let loadHandler = function (json) {
    try {
      if (json && json.ret && json.ret.length > 0) {
        const posts = json.ret;
        const content = cldrForum.parseContent(posts, "info");
        /*
         * Reality check: the json should refer to the same path as tr, which in practice
         * always matches cldrStatus.getCurrentId(). If not, log a warning and substitute "Please reload"
         * for the content.
         */
        let xpstrid = posts[0].xpath;
        if (xpstrid !== tr.xpstrid || xpstrid !== cldrStatus.getCurrentId()) {
          console.log(
            "Warning: xpath strid mismatch in updatePosts loadHandler:"
          );
          console.log("posts[0].xpath = " + posts[0].xpath);
          console.log("tr.xpstrid = " + tr.xpstrid);
          console.log("surveyCurrentId = " + cldrStatus.getCurrentId());

          content = "Please reload";
        }
        /*
         * Update the element whose class is 'forumDiv'.
         * Note: When updatePosts is called by the mouseover event handler for
         * the "Show n posts" button set up by havePosts, a clone of tr.forumDiv is created
         * (for mysterious reasons) by that event handler, and we could pass forumDivClone
         * as a parameter to updatePosts, then do forumDivClone.appendChild(content)
         * here, which is essentially how it formerly worked. However, that wouldn't work when
         * we're called by the success handler for submitPost. This works in all cases.
         */
        $(".forumDiv").first().html(content);
      }
    } catch (e) {
      console.log("Error in ajax forum read ", e.message);
      console.log(" response: " + json);
      const message =
        cldrStatus.stopIcon() + " exception in ajax forum read: " + e.message;
      cldrInfo.showWithRow(message, tr);
    }
  };

  const xhrArgs = {
    url: ourUrl,
    handleAs: "json",
    load: loadHandler,
    error: errorHandler,
  };
  cldrAjax.sendXhr(xhrArgs);
}

/**
 * Called when initially setting up the section.
 *
 * @param {Node} tr
 * @param {Node} theRow
 * @param {Node} forumDiv
 */
function appendForumStuff(tr, theRow, forumDiv) {
  cldrForum.setUserCanPost(tr.theTable.json.canModify);

  cldrDom.removeAllChildNodes(forumDiv); // we may be updating.
  const locmap = cldrLoad.getTheLocaleMap();
  var theForum = locmap.getLanguage(cldrStatus.getCurrentLocale());
  forumDiv.replyStub =
    cldrStatus.getContextPath() +
    "/survey?forum=" +
    theForum +
    "&_=" +
    cldrStatus.getCurrentLocale() +
    "&replyto=";
  forumDiv.postUrl = forumDiv.replyStub + "x" + theRow;
  /*
   * Note: SurveyAjax requires a "what" parameter for SurveyAjax.
   * It is not supplied here, but may be added later with code such as:
   *	let ourUrl = tr.forumDiv.url + "&what=forum_count" + cacheKill() ;
   *	let ourUrl = tr.forumDiv.url + "&what=forum_fetch";
   * Unfortunately that means "what" is not the first argument, as it would
   * be ideally for human readability of request urls.
   */
  forumDiv.url =
    cldrStatus.getContextPath() +
    "/SurveyAjax?xpath=" +
    theRow.xpathId +
    "&_=" +
    cldrStatus.getCurrentLocale() +
    "&fhash=" +
    theRow.rowHash +
    "&vhash=" +
    "&s=" +
    tr.theTable.session +
    "&voteinfo=t";
}

export { loadInfo, appendForumStuff, updatePosts };

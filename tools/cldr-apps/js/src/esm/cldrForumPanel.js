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
import * as cldrTable from "./cldrTable.js";
import * as cldrText from "./cldrText.js";

/**
 * Called when showing the Info Panel each time
 *
 * @param {Node} frag
 * @param {Node} forumDivClone = tr.forumDiv.cloneNode(true)
 * @param {Node} tr
 */
function loadInfo(frag, forumDivClone, tr) {
  if (tr.theRow) {
    addTopButtons(tr.theRow, frag);
  }
  const loader2 = cldrDom.createChunk(cldrText.get("loading"), "i");
  frag.appendChild(loader2);
  const ourUrl = tr.forumDiv.url + "&what=forum_count" + cldrSurvey.cacheKill();
  window.setTimeout(function () {
    const xhrArgs = {
      url: ourUrl,
      handleAs: "json",
      load: function (json) {
        if (json && json.forum_count !== undefined) {
          const nrPosts = parseInt(json.forum_count);
          havePosts(nrPosts, forumDivClone, tr, loader2);
        } else {
          console.log("Some error loading post count??");
        }
      },
    };
    cldrAjax.sendXhr(xhrArgs);
  }, 1900);
}

function addTopButtons(theRow, frag) {
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

function havePosts(nrPosts, forumDivClone, tr, loader2) {
  cldrDom.setDisplayed(loader2, false); // not needed
  tr.forumDiv.forumPosts = nrPosts;

  if (nrPosts == 0) {
    return; // nothing to do
  }

  const showButton = cldrDom.createChunk(
    "Show " + tr.forumDiv.forumPosts + " posts",
    "button",
    "forumShow"
  );

  forumDivClone.appendChild(showButton);

  const theListen = function (e) {
    cldrDom.setDisplayed(showButton, false);
    updatePosts(tr);
    cldrEvent.stopPropagation(e);
    return false;
  };
  cldrDom.listenFor(showButton, "click", theListen);
  cldrDom.listenFor(showButton, "mouseover", theListen);
}

/**
 * Update the forum posts in the Info Panel
 *
 * @param tr the table-row element with which the forum posts are associated,
 *		and whose info is shown in the Info Panel; or null, to get the
 *		tr from surveyCurrentId
 */
function updatePosts(tr) {
  if (!tr) {
    if (cldrStatus.getCurrentId() !== "") {
      const rowId = cldrTable.makeRowId(cldrStatus.getCurrentId());
      tr = document.getElementById(rowId);
    } else {
      /*
       * This is normal when adding a post in the main forum interface, which has no Info Panel).
       */
      return;
    }
  }
  if (!tr || !tr.forumDiv || !tr.forumDiv.url) {
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
        let content = cldrForum.parseContent(posts, "info");
        /*
         * Reality check: the json should refer to the same path as tr, which in practice
         * always matches cldrStatus.getCurrentId(). If not, log a warning and substitute "Please reload"
         * for the content.
         */
        const xpstrid = posts[0].xpath;
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

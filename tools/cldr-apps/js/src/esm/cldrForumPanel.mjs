/*
 * cldrForumPanel: encapsulate Survey Tool Forum Info Panel code.
 */
import * as cldrAjax from "./cldrAjax.mjs";
import * as cldrCache from "./cldrCache.mjs";
import * as cldrDom from "./cldrDom.mjs";
import * as cldrEvent from "./cldrEvent.mjs";
import * as cldrForum from "./cldrForum.mjs";
import * as cldrNotify from "./cldrNotify.mjs";
import * as cldrStatus from "./cldrStatus.mjs";
import * as cldrSurvey from "./cldrSurvey.mjs";
import * as cldrTable from "./cldrTable.mjs";
import * as cldrText from "./cldrText.mjs";

/**
 * Encapsulate this class name -- caution: it's used literally in surveytool.css
 */
const FORUM_DIV_CLASS = "forumDiv";

const forumCache = new cldrCache.LRU();

/**
 * Called when showing the Info Panel each time
 *
 * @param {Node} frag the fragment node to which we should append
 * @param {Node} tr the node for the row currently displayed in the DOM, plus associated data
 * @param {Object} theRow data for the row, based (partly) on latest json
 *
 * All dependencies on tr should be removed from this module.
 * Due to tech debt it's hard to tell for this tr object, what (if anything) comes from, or corresponds to
 * (1) the current DOM,
 * (2) the latest json,
 * (3) DOM fragments under construction,
 * (4) miscellaneous data items attached to tr although they don't need to be in the DOM...
 * Pretend we don't know that theRow === tr.theRow, since the DOM shouldn't be used as a database.
 *
 * Called by cldrInfo.show
 */
function loadInfo(frag, tr, theRow) {
  if (!frag || !tr || !theRow) {
    return;
  }
  addTopButtons(theRow, frag);
  const div = document.createElement("div");
  div.className = FORUM_DIV_CLASS;
  const cachedData = forumCache.get(makeCacheKey(theRow.xpstrid));
  if (cachedData) {
    setPostsFromData(frag, div, cachedData, theRow.xpstrid);
  } else {
    fetchAndLoadPosts(frag, div, tr, theRow);
  }
}

function setPostsFromData(frag, div, data, xpstrid) {
  const content = getForumContent(data, xpstrid);
  div.appendChild(content);
  frag.appendChild(div);
}

function fetchAndLoadPosts(frag, div, tr, theRow) {
  const loader2 = cldrDom.createChunk(cldrText.get("loading"), "i");
  frag.appendChild(loader2);
  frag.appendChild(div);
  const ourUrl = forumCountUrl(theRow);
  window.setTimeout(function () {
    const xhrArgs = {
      url: ourUrl,
      handleAs: "json",
      load: function (json) {
        cldrDom.setDisplayed(loader2, false);
        if (json?.err_code === "E_NO_PERMISSION") {
          cldrForum.setUserCanPost(false);
        } else if (json && json.forum_count !== undefined) {
          // It is assumed here that permission to view equals permission to post.
          cldrForum.setUserCanPost(true);
          const nrPosts = parseInt(json.forum_count);
          havePosts(nrPosts, div, tr, loader2);
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

function havePosts(nrPosts, div, tr, loader2) {
  if (nrPosts == 0) {
    return; // nothing to do
  }

  const showButton = cldrDom.createChunk(
    "Show " + nrPosts + " posts",
    "button",
    "forumShow"
  );

  div.appendChild(showButton);

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
 * @param {Node} tr the table-row element with which the forum posts are associated,
 *		and whose info is shown in the Info Panel; or null, to get the
 *		tr from cldrStatus.getCurrentId
 */
function updatePosts(tr) {
  if (!tr) {
    const id = cldrStatus.getCurrentId();
    if (id) {
      const rowId = cldrTable.makeRowId(id);
      tr = document.getElementById(rowId);
    } else {
      /*
       * This is normal when adding a post in the main forum interface, which has no Info Panel.
       */
      return;
    }
  }
  if (!tr?.theRow) {
    return;
  }
  const theRow = tr.theRow;
  const ourUrl = forumFetchUrl(theRow);

  function errorHandler(err) {
    console.log("Error in updatePosts: " + err);
    cldrNotify.error("Error updating posts, please refresh the page", err);
  }

  function loadHandler(json) {
    // Note: the reason for try/catch here isn't clear. There's no obvious reason why this
    // particular block of code would be more likely to throw an exception than any other block.
    try {
      if (json?.ret?.length > 0) {
        const posts = json.ret;
        forumCache.set(makeCacheKey(theRow.xpstrid), posts);
        const content = getForumContent(posts, theRow.xpstrid);

        /*
         * Update the first element whose class is FORUM_DIV_CLASS.
         */
        $("." + FORUM_DIV_CLASS)
          .first()
          .html(content);
      }
    } catch (e) {
      console.log("Error in ajax forum read ", e.message);
      console.log(" response: " + json);
      cldrNotify.exception(e, "while reading Forum");
    }
  }

  const xhrArgs = {
    url: ourUrl,
    handleAs: "json",
    load: loadHandler,
    error: errorHandler,
  };
  cldrAjax.sendXhr(xhrArgs);
}

function getForumContent(posts, xpstridExpected) {
  /*
   * Reality check: the json should refer to the same path as tr, which in practice
   * always matches cldrStatus.getCurrentId(). If not, log a warning and substitute "Please reload"
   * for the content.
   */
  const xpstrid = posts[0].xpath;
  if (xpstrid !== xpstridExpected || xpstrid !== cldrStatus.getCurrentId()) {
    console.log("Warning: xpath strid mismatch in updatePosts loadHandler:");
    console.log("posts[0].xpath = " + posts[0].xpath);
    console.log("xpstridExpected = " + xpstridExpected);
    console.log("surveyCurrentId = " + cldrStatus.getCurrentId());
    return "Please reload";
  }
  return cldrForum.parseContent(posts, "info");
}

function forumCountUrl(theRow) {
  return (
    cldrStatus.getContextPath() +
    "/SurveyAjax?what=forum_count" +
    "&xpath=" +
    theRow.xpathId +
    "&_=" +
    cldrStatus.getCurrentLocale() +
    "&fhash=" +
    theRow.rowHash +
    "&vhash=" +
    "&s=" +
    cldrStatus.getSessionId() +
    "&voteinfo=t" +
    cldrSurvey.cacheKill()
  );
}

function forumFetchUrl(theRow) {
  return (
    cldrStatus.getContextPath() +
    "/SurveyAjax?what=forum_fetch" +
    "&xpath=" +
    theRow.xpathId +
    "&_=" +
    cldrStatus.getCurrentLocale() +
    "&fhash=" +
    theRow.rowHash +
    "&vhash=" +
    "&s=" +
    cldrStatus.getSessionId() +
    "&voteinfo=t" +
    cldrSurvey.cacheKill()
  );
}

function makeCacheKey(xpstrid) {
  return cldrStatus.getCurrentLocale() + "-" + xpstrid;
}

function clearCache() {
  forumCache.clear();
}

export { clearCache, loadInfo, updatePosts };

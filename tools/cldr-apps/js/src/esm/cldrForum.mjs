/*
 * cldrForum: encapsulate main Survey Tool Forum code.
 *
 * Dependencies on external code: bootstrap.js
 */
import * as cldrAjax from "./cldrAjax.mjs";
import * as cldrDom from "./cldrDom.mjs";
import * as cldrEvent from "./cldrEvent.mjs";
import * as cldrForumFilter from "./cldrForumFilter.mjs";
import * as cldrForumPanel from "./cldrForumPanel.mjs";
import * as cldrForumType from "./cldrForumType.mjs";
import * as cldrLoad from "./cldrLoad.mjs";
import * as cldrNotify from "./cldrNotify.mjs";
import * as cldrStatus from "./cldrStatus.mjs";
import * as cldrSurvey from "./cldrSurvey.mjs";
import * as cldrText from "./cldrText.mjs";
import * as cldrVue from "./cldrVue.mjs";

import ForumButton from "../views/ForumButton.vue";

class PostInfo {
  constructor(postType) {
    this.postType = postType;

    this.locale = null;
    this.xpstrid = null;
    this.value = null;
    this.subject = "";
    this.willFlag = false;
    this.replyTo = -1;
    this.parentPost = null;
    this.isOpen = true; // may change in setReplyTo
  }

  /**
   * Set the parent data for a reply.
   *
   * @param {Object} rootPost - the data (as received from the back end) for the post to which
   * this is a reply. It is NOT a PostInfo object!
   */
  setReplyTo(rootPost) {
    this.replyTo = rootPost.id;
    this.parentPost = rootPost;
    this.isOpen = rootPost.open;
    // Copy these values from the parent.
    this.setLocalePathValueSubject(
      rootPost.locale,
      rootPost.xpath,
      rootPost.value,
      rootPost.subject
    );
  }

  setLocalePathValueSubject(locale, xpstrid, value, subject) {
    this.locale = locale;
    this.xpstrid = xpstrid;
    this.value = value;
    this.subject = subject;
  }

  setWillFlagTrue() {
    this.willFlag = true;
  }
}

const SUMMARY_CLASS = "getForumSummary";

const FORUM_DEBUG = false;

/**
 * If false, hide (effectively disable) Agree and Decline buttons, pending
 * (1) better ways to clarify to vetters what the Agree button actually does and doesn't
 * accomplish, and possibly
 * (2) automatic vote submission when a user clicks Agree
 * Reference: https://unicode-org.atlassian.net/browse/CLDR-14922
 */
const SHOW_AGREE_AND_DECLINE = false;

/**
 * The locale, like "fr_CA", for which to show Forum posts.
 * This module has persistent data for only one locale at a time, except that sublocales may be
 * combined, such as "fr_CA" combined with "fr".
 * Caution: the locale for a reply must exactly match the locale for the post to which it's a reply,
 * so the locale for a particular post might for example be "fr" even though forumLocale is "fr_CA",
 * or vice-versa.
 */
let forumLocale = null;

/**
 * The time when the posts were last updated from the server
 */
let forumUpdateTime = null;

/**
 * Mapping from post id to post object, describing the most recently parsed
 * full set of posts from the server
 */
let postHash = {};

/**
 * Mapping from thread id to array of post objects, describing the most recently parsed
 * full set of posts from the server
 */
let threadHash = {};

/**
 * Whether the current user can make posts
 */
let userCanPost = false;

/**
 * Whether to use UTC for displaying date/time
 * Helpful for unit tests
 */
let displayUtc = false;

// called as special.load; this is for the full-page Forum, not for posts shown in the Info Panel
function load() {
  const curLocale = cldrStatus.getCurrentLocale();
  if (!curLocale) {
    cldrLoad.flipToGenericNoLocale();
  } else {
    const locmap = cldrLoad.getTheLocaleMap();
    const forumName = locmap.getLocaleName(locmap.getLanguage(curLocale));
    const forumMessage = cldrText.sub("forum_msg", {
      forum: forumName,
      locale: cldrStatus.getCurrentLocaleName(),
    });
    const surveyUser = cldrStatus.getSurveyUser();
    const userId = surveyUser && surveyUser.id ? surveyUser.id : 0;
    loadForum(curLocale, userId, forumMessage);
  }
}

/**
 * Fetch the Forum data from the server, and "load" it
 *
 * @param locale the locale string, like "fr_CA" (cldrStatus.getCurrentLocale())
 * @param userId the id of the current user
 * @param forumMessage the forum message
 */
function loadForum(locale, userId, forumMessage) {
  setLocale(locale);
  const url = getLoadForumUrl();
  const errorHandler = function (err) {
    loadHandler({
      err: err,
      err_code: "Loading forum data",
    });
  };
  const loadHandler = function (json) {
    const ourDiv = document.createElement("div");
    if (json.err) {
      loadBad(ourDiv, json);
    } else {
      loadOk(ourDiv, json, userId, forumMessage);
    }
    // No longer loading
    cldrSurvey.hideLoader();
    cldrLoad.flipToOtherDiv(ourDiv);
    handleIdChanged(cldrStatus.getCurrentId()); // rescroll.
  };
  const xhrArgs = {
    url: url,
    handleAs: "json",
    load: loadHandler,
    error: errorHandler,
  };
  cldrAjax.sendXhr(xhrArgs);
}

function loadBad(ourDiv, json) {
  let html = "<p>" + json.err + "</p>";
  if (json.err_code) {
    html += "<p>" + cldrText.get(json.err_code) + "</p>";
  }
  ourDiv.innerHTML = html;
}

function loadOk(ourDiv, json, userId, forumMessage) {
  /*
   * The server has already confirmed that the user is logged in and has permission to view the forum.
   * Note: the criteria (here) for posting in the main forum window are less strict than in the info
   * panel; see the other call to setUserCanPost. Here, we have no "json.canModify" set by the server.
   */
  setUserCanPost(true);

  ourDiv.appendChild(forumCreateChunk(forumMessage, "h4", ""));
  ourDiv.appendChild(forumCreateChunk(cldrText.get("forumGuidance"), "p", ""));
  const filterMenu = cldrForumFilter.createMenu(cldrLoad.reloadV);
  const summaryDiv = document.createElement("div");
  summaryDiv.innerHTML = "";
  ourDiv.appendChild(summaryDiv);
  ourDiv.appendChild(filterMenu);
  ourDiv.appendChild(document.createElement("hr"));
  const posts = json.ret;
  if (posts.length == 0) {
    ourDiv.appendChild(
      forumCreateChunk(cldrText.get("forum_noposts"), "p", "helpContent")
    );
  } else {
    const content = parseContent(posts, "main");
    ourDiv.appendChild(content);
    summaryDiv.innerHTML = getForumSummaryHtml(forumLocale, userId, true); // after parseContent
  }
}

// called as special.handleIdChanged
function handleIdChanged(strid) {
  if (strid) {
    const id = new Number(strid);
    if (id == NaN) {
      cldrStatus.setCurrentId("");
    } else {
      cldrStatus.setCurrentId(id.toString());
    }
    const itemid = "fp" + id;
    const pdiv = document.getElementById(itemid);
    if (pdiv) {
      pdiv.scrollIntoView();
      (function (o, itemid, pdiv) {
        pdiv.style["background-color"] = "yellow";
        window.setTimeout(function () {
          pdiv.style["background-color"] = null;
        }, 2000);
      })(this, itemid, pdiv);
    } else {
      console.log("No item " + itemid);
    }
  }
}

/**
 * Set whether the user is allowed to make posts
 */
function setUserCanPost(canPost) {
  userCanPost = canPost ? true : false;
}

/**
 * Make the text (body) string for a forum post
 *
 * @param {PostInfo} pi
 * @return the string
 */
function prefillPostText(pi) {
  const postType = pi.postType; // the verb such as 'Request', 'Discuss', ...
  const value = pi.value; // the value that was requested in the root post, or null
  if (postType === cldrForumType.CLOSE) {
    return cldrText.get("forum_prefill_close");
  } else if (postType === cldrForumType.REQUEST) {
    if (value) {
      return cldrText.sub("forum_prefill_request", [value]);
    }
  } else if (postType === cldrForumType.AGREE) {
    return cldrText.sub("forum_prefill_agree", [value]);
  } else if (postType === cldrForumType.DECLINE) {
    return cldrText.sub("forum_prefill_decline", [value]);
  }
  return "";
}

function sendPostRequest(pi, text) {
  const url = cldrStatus.getContextPath() + "/SurveyAjax";
  const postData = {
    what: "forum_post",
    s: cldrStatus.getSessionId(),
    subj: pi.subject,
    _: pi.locale,
    open: pi.isOpen,
    postType: pi.postType,
    replyTo: pi.replyTo,
    root: pi.replyTo, // root and replyTo are always the same
    text: text,
    value: pi.value,
    xpath: pi.xpstrid,
  };
  const xhrArgs = {
    url: url,
    handleAs: "json",
    load: loadHandlerForSubmit,
    error: errorHandlerForSubmit,
    postData: postData,
  };
  cldrAjax.sendXhr(xhrArgs);
}

function loadHandlerForSubmit(data) {
  if (data.err) {
    cldrNotify.error("Error posting to Forum", data.err);
  } else if (data.ret && data.ret.length > 0) {
    if (cldrStatus.getCurrentSpecial() === "forum") {
      cldrLoad.reloadV(); // main Forum page
    } else {
      cldrForumPanel.updatePosts(null); // Info Panel
      cldrSurvey.expediteStatusUpdate(); // update forum icons (👁️‍🗨️, 💬) in the main table
    }
  } else {
    const message =
      "Your post was added, #" + data.postId + " but could not be shown.";
    cldrNotify.error("Error posting to Forum", message);
  }
}

function errorHandlerForSubmit(err) {
  cldrNotify.error("Error posting to Forum", err);
}

/**
 * Create a DOM object referring to this set of forum posts
 *
 * @param posts the array of forum post objects, newest first
 * @param context the string defining the context
 *
 * @return new DOM object
 *
 * NOTE: this method is too long and has tech debt. Probably it will be replaced completely
 * by modern code using Vue components rather than direct DOM manipulation.
 *
 * If context is 'summary', all DOM element creation here is a waste of time.
 *
 * The same locale+path can have multiple distinct threads.
 */
function parseContent(posts, context) {
  const opts = getOptionsForContext(context);

  updateForumData(posts, opts.fullSet);

  const postDivs = {}; //  postid -> div
  const topicDivs = {}; // xpath -> div or "#123" -> div

  /*
   * create the topic (thread) divs -- populate topicDivs with DOM elements
   *
   * If this code were to be refactored, ideally it would skip this loop if opts.createDomElements is false.
   * Currently we have to do this even if opts.createDomElements is false, since filterAndAssembleForumThreads
   * depends on topicDivs.
   */
  for (let num in posts) {
    const post = posts[num];
    if (!topicDivs[post.threadId]) {
      // add the topic div
      const topicDiv = document.createElement("div");
      topicDiv.className = "well well-sm postTopic";
      const rootPost = getThreadRootPost(post);
      if (opts.showItemLink) {
        const topicInfo = forumCreateChunk("", "h4", "postTopicInfo");
        topicDiv.appendChild(topicInfo);
        if (post.locale) {
          const locmap = cldrLoad.getTheLocaleMap();
          const localeLink = forumCreateChunk(
            locmap.getLocaleName(post.locale),
            "a",
            "localeName"
          );
          if (post.locale != cldrStatus.getCurrentLocale()) {
            localeLink.href = cldrLoad.linkToLocale(post.locale);
          }
          topicInfo.appendChild(localeLink);
        }
        if (post.xpath) {
          topicInfo.appendChild(makeItemLink(post));
        }
        addThreadSubjectSpan(topicInfo, rootPost);
      }
      if (opts.createDomElements) {
        if (rootPost.postType === cldrForumType.REQUEST && rootPost.value) {
          const requestInfo = forumCreateChunk(
            "Requesting “" + rootPost.value + "”",
            "h4",
            "postTopicInfo"
          );
          topicDiv.appendChild(requestInfo);
        }
      }
      topicDivs[post.threadId] = topicDiv;
      topicDiv.id = "fthr_" + post.threadId;
    }
  }
  // Now, top to bottom, just create the post divs
  for (let num in posts) {
    const post = posts[num];

    const subpost = forumCreateChunk("", "div", "post");
    postDivs[post.id] = subpost;
    subpost.id = "fp" + post.id;

    const headingLine = forumCreateChunk("", "h4", "selected");

    // If post.posterInfo is undefined, don't crash; insert "[Poster no longer active]".
    if (!post.posterInfo) {
      headingLine.appendChild(
        forumCreateChunk("[Poster no longer active]", "span", "")
      );
    } else {
      const gravatar = cldrSurvey.createGravatar(post.posterInfo);
      gravatar.className = "gravatar pull-left";
      subpost.appendChild(gravatar);
      const surveyUser = cldrStatus.getSurveyUser();
      if (surveyUser && post.posterInfo.id === surveyUser.id) {
        headingLine.appendChild(
          forumCreateChunk(cldrText.get("user_me"), "span", "forum-me")
        );
      } else {
        const usera = forumCreateChunk(post.posterInfo.name + " ", "a", "");
        if (post.posterInfo.email) {
          usera.appendChild(
            forumCreateChunk("", "span", "glyphicon glyphicon-envelope")
          );
          usera.href = "mailto:" + post.posterInfo.email;
        }
        headingLine.appendChild(usera);
        headingLine.appendChild(
          document.createTextNode(" (" + post.posterInfo.org + ") ")
        );
      }
      const userLevelKey = "userlevel_" + post.posterInfo.userlevelName;
      const userLevelStr = cldrText.get(userLevelKey);
      const userLevelChunk = forumCreateChunk(
        userLevelStr,
        "span",
        "userLevelName label-info label"
      );
      userLevelChunk.title = cldrText.get(userLevelKey + "_desc");
      headingLine.appendChild(userLevelChunk);
    }
    let date = fmtDateTime(post.date_long);
    if (post.version) {
      date = "[v" + post.version + "] " + date;
    }
    const dateChunk = forumCreateChunk(
      date,
      "span",
      "label label-primary pull-right forumLink"
    );
    (function (post) {
      cldrDom.listenFor(dateChunk, "click", function (e) {
        const locmap = cldrLoad.getTheLocaleMap();
        if (
          post.locale &&
          locmap.getLanguage(cldrStatus.getCurrentLocale()) !=
            locmap.getLanguage(post.locale)
        ) {
          cldrStatus.setCurrentLocale(locmap.getLanguage(post.locale));
        }
        cldrStatus.setCurrentPage("");
        cldrStatus.setCurrentId(post.id);
        cldrLoad.replaceHash();
        if (cldrStatus.getCurrentSpecial() != "forum") {
          cldrStatus.setCurrentSpecial("forum");
          cldrLoad.reloadV();
        }
        return cldrEvent.stopPropagation(e);
      });
    })(post);
    headingLine.appendChild(dateChunk);
    subpost.appendChild(headingLine);

    const subChunk = forumCreateChunk("", "div", "");
    subpost.appendChild(subChunk);
    const isReply = post.parent !== -1;
    const typeLabel = makePostTypeLabel(post.postType, isReply);
    if (!isReply) {
      const openOrClosed = post.open ? "Open" : "Closed";
      const comboChunk = forumCreateChunk(
        openOrClosed,
        "div",
        "forumThreadStatus"
      );
      comboChunk.appendChild(
        forumCreateChunk(typeLabel, "span", "postTypeComboLabel")
      );
      subChunk.appendChild(comboChunk);
    } else {
      subChunk.appendChild(forumCreateChunk(typeLabel, "div", "postTypeLabel"));
    }

    // actual text
    const postText = post2text(post.text);
    const postContent = forumCreateChunk(
      postText,
      "div",
      "postContent postTextBorder"
    );
    subpost.appendChild(postContent);
  }
  appendPostDivsToTopicDivs(posts, topicDivs, postDivs);
  if (opts.showReplyButton) {
    addReplyButtonsToEachTopic(topicDivs);
  }
  return filterAndAssembleForumThreads(
    posts,
    topicDivs,
    opts.applyFilter,
    opts.showThreadCount
  );
}

/**
 * Update several persistent data structures to describe the given set of posts
 *
 * @param posts the array of post objects, from newest to oldest
 * @param fullSet true if we should start fresh with these posts
 */
function updateForumData(posts, fullSet) {
  if (fullSet) {
    postHash = {};
    threadHash = {};
  }
  updatePostHash(posts);
  addThreadIds(posts);
  updateThreadHash(posts);
  forumUpdateTime = Date.now();
}

/**
 * Update the postHash mapping from post id to post object
 *
 * @param posts the array of post objects, from newest to oldest
 */
function updatePostHash(posts) {
  posts.forEach(function (post) {
    postHash[post.id] = post;
  });
}

/**
 * Add a "threadId" attribute to each post object in the given array
 *
 * For a post without a parent, the thread id is the post id.
 *
 * For a post with a parent, the thread id is the root post id.
 *
 * The json unfortunately has root = -1 if parent = -1; if instead it had
 * root = id if parent = -1 then there would be no need to distinguish
 * post.root and post.threadId, they would always be the same -- except
 * that threadId is expected to be a string, not an integer
 *
 * @param posts the array of post objects
 */
function addThreadIds(posts) {
  posts.forEach(function (post) {
    const id = post.parent > 0 ? post.root : post.id;
    post.threadId = id.toString();
  });
}

/**
 * Update the threadHash mapping from threadId to an array of all the posts in that thread
 *
 * @param posts the array of post objects, from newest to oldest
 *
 * The posts are assumed to have threadId set already by addThreadIds.
 */
function updateThreadHash(posts) {
  posts.forEach(function (post) {
    const threadId = post.threadId;
    if (!(threadId in threadHash)) {
      threadHash[threadId] = [];
    }
    threadHash[threadId].push(post);
  });
}

/**
 * Make a hyperlink from the given post to the the same post in the main Forum window
 *
 * @param post the post object
 * @return the DOM element
 */
function makeItemLink(post) {
  const itemLink = forumCreateChunk(
    cldrText.get("forum_item"),
    "a",
    "pull-right postItem glyphicon glyphicon-zoom-in"
  );
  itemLink.href = "#/" + post.locale + "//" + post.xpath;
  return itemLink;
}

/**
 * Make a span containing a subject line for the specified thread
 *
 * @param topicInfo the DOM element to which to attach the span
 * @param rootPost the oldest post in the thread
 */
function addThreadSubjectSpan(topicInfo, rootPost) {
  /*
   * Starting with CLDR v38, posts should all have post.xpath, and post.subject
   * should be like "Characters | Typography | Style | wght-900-heavy" (recognizable
   * by containing the character '|'), constructed from the xpath and path-header when
   * the post is created.
   *
   * In such normal cases (or if there is no xpath), the thread subject is the same as
   * the subject of the oldest post in the thread.
   */
  if (rootPost.subject.indexOf("|") >= 0 || !rootPost.xpath) {
    topicInfo.appendChild(
      forumCreateChunk(post2text(rootPost.subject), "span", "topicSubject")
    );
    return;
  }
  /*
   * Some old posts have subjects like "Review" or "Flag Removed".
   * In this case, construct a new subject based on the xpath and path-header.
   * This is awkward since xpathMap.get is asynchronous. Display the word
   * "Loading" as a place-holder while waiting for the result.
   */
  const loadingMsg = forumCreateChunk(
    cldrText.get("loading"),
    "i",
    "loadingMsg"
  );
  topicInfo.appendChild(loadingMsg);

  const xpathMap = cldrSurvey.getXpathMap();
  xpathMap.get(
    {
      hex: rootPost.xpath,
    },
    function (o) {
      if (o.result) {
        topicInfo.removeChild(loadingMsg);
        const itemPh = forumCreateChunk(
          xpathMap.formatPathHeader(o.result.ph),
          "span",
          "topicSubject"
        );
        itemPh.title = o.result.path;
        topicInfo.appendChild(itemPh);
      }
    }
  );
}

/**
 * Append post divs to their topic divs.
 * Within each thread, put old posts before new posts.
 * Go through the posts array in reverse order, old before new.
 *
 * @param posts the array of forum post objects, newest first
 * @param topicDivs the map from threadId to DOM elements
 * @param postDivs the map from post id to DOM elements
 */
function appendPostDivsToTopicDivs(posts, topicDivs, postDivs) {
  for (let i = posts.length - 1; i >= 0; i--) {
    const post = posts[i];
    topicDivs[post.threadId].appendChild(postDivs[post.id]);
  }
}

/**
 * Add a set of reply buttons for each div in topicDivs
 *
 * @param topicDivs the map from threadId to DOM elements
 */
function addReplyButtonsToEachTopic(topicDivs) {
  if (!userCanPost) {
    return;
  }
  Object.keys(topicDivs).forEach(function (threadId) {
    const rootPost = getRootPostFromThreadId(threadId);
    if (rootPost) {
      addReplyButtons(topicDivs[threadId], rootPost);
    }
  });
}

/**
 * Make one or more new-post buttons for the given post, and append them to the given element
 *
 * @param el the DOM element to append to
 * @param locale the locale
 * @param couldFlag true if the user could add a flag for this path, else false
 * @param xpstrid the xpath string id
 * @param code the "code" for the xpath
 * @param value the value the current user voted for, or null
 */
function addNewPostButtons(el, locale, couldFlag, xpstrid, code, value) {
  if (!userCanPost) {
    return;
  }
  const options = getPostTypeOptions(
    false /* isReply */,
    null /* rootPost */,
    value
  );
  // Only an enabled REQUEST button can really cause a path to be flagged.
  const reallyCanFlag =
    couldFlag && value && Object.keys(options).includes(cldrForumType.REQUEST);
  if (reallyCanFlag) {
    const message = cldrText.get("mustflag_explain_msg");
    cldrSurvey.addIcon(el, "i-stop");
    el.appendChild(cldrDom.createChunk(message, "span", ""));
    el.appendChild(document.createElement("p"));
  }
  Object.keys(options).forEach(function (postType) {
    const label = makePostTypeLabel(postType, false /* isReply */);
    // A "new post" button has type cldrForumType.REQUEST or cldrForumType.DISCUSS.
    // REQUEST is only enabled if there is a non-null value (which the user voted for).
    const disabled = postType === cldrForumType.REQUEST && value === null;
    // Only an enabled REQUEST button can really cause a path to be flagged.
    const willFlag =
      couldFlag && postType === cldrForumType.REQUEST && !disabled;
    const pi = new PostInfo(postType);
    const xpathMap = cldrSurvey.getXpathMap();
    xpathMap.get(
      {
        hex: xpstrid,
      },
      function (o) {
        const subject = makeSubject(code, xpstrid, o, xpathMap, willFlag);
        pi.setLocalePathValueSubject(locale, xpstrid, value, subject);
        if (willFlag) {
          pi.setWillFlagTrue();
        }
        addPostVueButton(el, pi, label, disabled);
      }
    );
  });
}

/**
 * Make one or more reply buttons for the given post, and append them to the given element
 *
 * @param el the DOM element to append to
 * @param rootPost the original post in the thread
 */
function addReplyButtons(el, rootPost) {
  if (!userCanPost) {
    return;
  }
  const options = getPostTypeOptions(
    true /* isReply */,
    rootPost,
    rootPost.value
  );

  Object.keys(options).forEach(function (postType) {
    const pi = new PostInfo(postType);
    pi.setReplyTo(rootPost);
    const typeLabel = makePostTypeLabel(postType, true /* isReply */);
    addPostVueButton(el, pi, typeLabel, false /* not disabled */);
  });
}

function makeSubject(code, xpstrid, o, xpathMap, willFlag) {
  let subj = code + " " + xpstrid;
  if (o.result && o.result.ph) {
    subj = xpathMap.formatPathHeader(o.result.ph);
  }
  if (willFlag) {
    subj += " (Flag for review)";
  }
  return subj;
}

function addPostVueButton(containerEl, pi, label, disabled) {
  try {
    const wrapper = cldrVue.mount(ForumButton, containerEl);
    wrapper.setPostInfo(pi);
    wrapper.setLabel(label);
    const reminder = cldrText.get(
      pi.willFlag ? "flag_must_have_reason" : "forum_remember_vote"
    );
    wrapper.setReminder(reminder);
    if (disabled) {
      wrapper.setDisabled();
    }
  } catch (e) {
    console.error(
      "Error loading Post Button vue " + e.message + " / " + e.name
    );
    cldrNotify.exception(e, "while loading Post Button");
  }
}

/**
 * Get an object defining the currently allowed post-type values
 * for making a new post, for the current user and given parameters
 *
 * @param isReply true if this post is a reply, else false
 * @param rootPost the original post in the thread, or null if not replying
 * @param value the value the root post requested, or null
 * @return the object mapping verbs like 'Request' to label strings like 'Request'
 *         (Currently the labels are the same as the verbs)
 *
 * Compare SurveyForum.ForumStatus on server
 */
function getPostTypeOptions(isReply, rootPost, value) {
  const options = {};
  if (rootPost === null || rootPost.open) {
    if (!isReply) {
      /*
       * Show Request button even if value is null. It will be visible but disabled if value is null.
       */
      options[cldrForumType.REQUEST] = cldrForumType.REQUEST;
    }
    if (canAgreeOrDecline(value, isReply, rootPost)) {
      options[cldrForumType.AGREE] = cldrForumType.AGREE;
      options[cldrForumType.DECLINE] = cldrForumType.DECLINE;
    }
    if (isReply || userIsTC()) {
      // only TC can initiate Discuss; others can reply
      options[cldrForumType.DISCUSS] = makePostTypeLabel(
        cldrForumType.DISCUSS,
        isReply
      );
    }
    if (userCanClose(isReply, rootPost)) {
      options[cldrForumType.CLOSE] = cldrForumType.CLOSE;
    }
  }
  return options;
}

function canAgreeOrDecline(value, isReply, rootPost) {
  if (
    SHOW_AGREE_AND_DECLINE &&
    value &&
    isReply &&
    rootPost &&
    rootPost.postType === cldrForumType.REQUEST &&
    !userIsPoster(rootPost)
  ) {
    return true;
  } else {
    return false;
  }
}

/**
 * For replies, label 'Comment' instead of 'Discuss'
 *
 * @param postType the post type
 * @param isReply true if this post is a reply, else false
 * @return the label
 */
function makePostTypeLabel(postType, isReply) {
  if (postType === cldrForumType.DISCUSS && isReply) {
    return cldrForumType.COMMENT;
  }
  return postType;
}

/**
 * Is this user allowed to close the thread now?
 *
 * The user is only allowed if they are the original poster of the thread,
 * or a TC (technical committee) member.
 *
 * @param isReply true if this post is a reply, else false
 * @param rootPost the original post in the thread, or null
 * @return true if this user is allowed to close, else false
 */
function userCanClose(isReply, rootPost) {
  return isReply && rootPost.open && (userIsPoster(rootPost) || userIsTC());
}

/**
 * Is the current user the poster of this post?
 *
 * @param post the post, or null
 * @returns true or false
 */
function userIsPoster(post) {
  if (post) {
    const surveyUser = cldrStatus.getSurveyUser();
    if (surveyUser && surveyUser.id === post.poster) {
      return true;
    }
  }
  return false;
}

/**
 * Is the current user a TC (Technical Committee) member?
 *
 * @return true or false
 */
function userIsTC() {
  const surveyUserPerms = cldrStatus.getPermissions();
  return surveyUserPerms && surveyUserPerms.userIsTC;
}

/**
 * Get an object whose properties define the parseContent options to be used for a particular
 * context in which parseContent is called
 *
 * @param context the string defining the context:
 *
 *   'main' for the context in which "Forum" is chosen from the left sidebar
 *
 *   'summary' for the context of getForumSummaryHtml
 *
 *   'info' for the "Info Panel" context (either main vetting view row, or Dashboard "Fix" button)
 *
 *   'parent' for the replied-to post at the bottom of the create-reply dialog
 *
 * @return an object with these properties:
 *
 *   showItemLink = true if there should be an "item" (xpath) link
 *
 *   showReplyButton = true if there should be a reply button
 *
 *   fullSet = true if this is a full set of posts
 *
 *   applyFilter = true if the currently menu-selected filter should be applied
 *
 *   showThreadCount = true to display the number of threads
 *
 *   createDomElements = true to create the DOM objects (false for summary)
 */
function getOptionsForContext(context) {
  const opts = getDefaultParseOptions();
  if (context === "main") {
    opts.showItemLink = true;
    opts.showReplyButton = true;
    opts.applyFilter = true;
    opts.showThreadCount = true;
  } else if (context === "summary") {
    opts.applyFilter = true;
    opts.createDomElements = false;
  } else if (context === "info") {
    opts.showReplyButton = true;
  } else if (context === "parent") {
    opts.fullSet = false;
  } else {
    console.log("Unrecognized context in getOptionsForContext: " + context);
  }
  return opts;
}

/**
 * Get the default parseContent options
 *
 * @return a new object with the default properties
 */
function getDefaultParseOptions() {
  const opts = {};
  opts.showItemLink = false;
  opts.showReplyButton = false;
  opts.fullSet = true;
  opts.applyFilter = false;
  opts.showThreadCount = false;
  opts.createDomElements = true;
  return opts;
}

/**
 * Convert the given text by replacing some html with plain text
 *
 * @param the plain text
 */
function post2text(text) {
  if (text === undefined || text === null) {
    text = "(empty)";
  }
  let out = text;
  out = out.replace(/<p>/g, "\n");
  out = out.replace(/&quot;/g, '"');
  out = out.replace(/&lt;/g, "<");
  out = out.replace(/&gt;/g, ">");
  out = out.replace(/&amp;/g, "&");
  return out;
}

/**
 * Create a DOM object with the specified text, tag, and HTML class.
 *
 * @param text textual content of the new object, or null for none
 * @param tag which element type to create, or null for "span"
 * @param className CSS className, or null for none.
 * @return new DOM object
 *
 * This duplicated a function in survey.js; copied here to avoid the dependency
 */
function forumCreateChunk(text, tag, className) {
  if (!tag) {
    tag = "span";
  }
  const chunk = document.createElement(tag);
  if (className) {
    chunk.className = className;
  }
  if (text) {
    chunk.appendChild(document.createTextNode(text));
  }
  return chunk;
}

/**
 * Get the root (original) post in the thread, i.e., the last one in the array
 *
 * @param threadId the thread id
 * @return the root post, or null if not found
 */
function getRootPostFromThreadId(threadId) {
  const threadPosts = threadHash[threadId];
  if (threadPosts.length < 1) {
    return null;
  }
  return threadPosts[threadPosts.length - 1];
}

/**
 * Get the first (original) post in the thread containing this post
 *
 * @param post the post object
 * @return the original post in the thread
 */
function getThreadRootPost(post) {
  if (postHash[post.threadId]) {
    return postHash[post.threadId];
  }
  /*
   * The following shouldn't be necessary unless data is missing/corrupted
   */
  while (post.parent >= 0 && postHash[post.parent]) {
    post = postHash[post.parent];
  }
  return post;
}

/**
 * Filter the forum threads and assemble them into a new document fragment,
 * ordering threads from newest to oldest, determining the time of each thread
 * by the newest post it contains
 *
 * @param posts the array of post objects, from newest to oldest
 * @param topicDivs the array of thread elements, indexed by threadId
 * @param applyFilter true if the currently menu-selected filter should be applied
 * @param showThreadCount true to display the number of threads
 * @return the new document fragment
 */
function filterAndAssembleForumThreads(
  posts,
  topicDivs,
  applyFilter,
  showThreadCount
) {
  let filteredArray = cldrForumFilter.getFilteredThreadIds(
    threadHash,
    applyFilter
  );
  const div = document.createDocumentFragment();
  let countEl = null;
  if (showThreadCount) {
    countEl = document.createElement("h4");
    div.append(countEl);
  }
  let threadCount = 0;
  posts.forEach(function (post) {
    if (filteredArray.includes(post.threadId)) {
      ++threadCount;
      /*
       * Append the div for this threadId, then remove this threadId
       * from filteredArray to prevent appending the same div again
       * (which would move the div to the bottom, not duplicate it).
       */
      div.append(topicDivs[post.threadId]);
      filteredArray = filteredArray.filter((id) => id !== post.threadId);
    }
  });
  if (showThreadCount) {
    countEl.innerHTML =
      threadCount + (threadCount === 1 ? " topic" : " topics");
  }
  return div;
}

/**
 * Format a date and time for display in a forum post
 *
 * @param x the number of seconds since 1970-01-01
 * @returns the formatted date and time as a string, like "2018-05-16 13:45"
 */
function fmtDateTime(x) {
  const d = new Date(x);

  function pad(n) {
    return n < 10 ? "0" + n : n;
  }
  if (displayUtc) {
    return (
      d.getUTCFullYear() +
      "-" +
      pad(d.getUTCMonth() + 1) +
      "-" +
      pad(d.getUTCDate()) +
      " " +
      pad(d.getUTCHours()) +
      ":" +
      pad(d.getUTCMinutes() + " UTC")
    );
  }
  return (
    d.getFullYear() +
    "-" +
    pad(d.getMonth() + 1) +
    "-" +
    pad(d.getDate()) +
    " " +
    pad(d.getHours()) +
    ":" +
    pad(d.getMinutes())
  );
}

function refreshSummary() {
  const locale = cldrStatus.getCurrentLocale();
  if (locale) {
    const surveyUser = cldrStatus.getSurveyUser();
    if (surveyUser?.id && getSummaryClassElements().length) {
      const html = getForumSummaryHtml(locale, surveyUser.id, false);
      setAllForumSummaryElements(html);
    }
  }
}

/**
 * Get a piece of html text summarizing the current Forum statistics
 *
 * @param locale the locale string
 * @param userId the current user's id, for cldrForumFilter
 * @param getTable true to get a table, false to get a one-liner
 * @return the html
 */
function getForumSummaryHtml(locale, userId, getTable) {
  setLocale(locale);
  cldrForumFilter.setUserId(userId);
  return reallyGetForumSummaryHtml(true /* canDoAjax */, getTable);
}

/**
 * Get a piece of html text summarizing the current Forum statistics
 *
 * @param canDoAjax true to call loadForumForSummaryOnly if needed, false otherwise; should
 *                  be false if the caller is the loadHandler for loadForumForSummaryOnly,
 *                  to prevent endless back-and-forth if things go wrong
 * @param getTable true to get a table, false to get a one-liner like "27/0/27"
 * @return the html
 */
function reallyGetForumSummaryHtml(canDoAjax, getTable) {
  const tag = getTable ? "div" : "span";
  let html = "<" + tag + ">\n";
  if (!forumUpdateTime) {
    if (canDoAjax) {
      if (getTable) {
        html += "<p>Loading Forum Summary...</p>\n";
      }
      loadForumForSummaryOnly(forumLocale, getTable);
    } else if (getTable) {
      html += "<p>Load failed</p>n";
    }
  } else {
    if (FORUM_DEBUG && getTable) {
      html += "<p>Retrieved " + fmtDateTime(forumUpdateTime) + "</p>\n";
    }
    const c = cldrForumFilter.getFilteredThreadCounts();
    if (getTable) {
      html += "<ul>\n";
      Object.keys(c).forEach(function (k) {
        html += "<li>" + k + ": " + c[k] + "</li>\n";
      });
      html += "</ul>\n";
    } else {
      let first = true;
      Object.keys(c).forEach(function (k) {
        if (!first) {
          html += "/";
        }
        html += c[k];
        first = false;
      });
    }
  }
  html += "</" + tag + ">\n";
  return html;
}

/**
 * Fetch the Forum data from the server, and show a summary
 *
 * @param locale the locale
 * @param getTable true to get a table, false to get a one-liner
 */
function loadForumForSummaryOnly(locale, getTable) {
  if (typeof cldrAjax === "undefined") {
    return;
  }
  setLocale(locale);
  const url = getLoadForumUrl();
  const errorHandler = function (err) {
    setAllForumSummaryElements(err);
  };
  const loadHandler = function (json) {
    if (!getSummaryClassElements().length) {
      return;
    }
    if (json.err) {
      setAllForumSummaryElements("Error");
      return;
    }
    const posts = json.ret;
    parseContent(posts, "summary");
    const html = reallyGetForumSummaryHtml(
      false /* do not reload recursively */,
      getTable
    ); // after parseContent
    setAllForumSummaryElements(html);
  };
  const xhrArgs = {
    url: url,
    handleAs: "json",
    load: loadHandler,
    error: errorHandler,
  };
  cldrAjax.sendXhr(xhrArgs);
}

function setAllForumSummaryElements(html) {
  const els = getSummaryClassElements();
  for (const el of Array.from(els)) {
    el.innerHTML = html;
  }
}

function getSummaryClassElements() {
  return document.getElementsByClassName(SUMMARY_CLASS);
}

/**
 * Load or reload the main Forum page
 */
function reload() {
  cldrStatus.setCurrentSpecial("forum");
  cldrStatus.setCurrentId("");
  cldrStatus.setCurrentPage("");
  cldrLoad.reloadV();
}

/**
 * Get the URL to use for loading the Forum
 */
function getLoadForumUrl() {
  const sessionId = cldrStatus.getSessionId();
  if (!sessionId) {
    console.log("Error: sessionId falsy in getLoadForumUrl");
    return "";
  }
  return (
    "SurveyAjax?what=forum_fetch&xpath=0&_=" + forumLocale + "&s=" + sessionId
  );
}

/**
 * If the given locale is not the one we've already loaded, switch to it,
 * initializing data to avoid using data for the wrong locale
 *
 * @param locale the locale string, like "fr_CA" (cldrStatus.getCurrentLocale())
 */
function setLocale(locale) {
  if (locale !== forumLocale) {
    forumLocale = locale;
    forumUpdateTime = null;
    postHash = {};
  }
}

function getThreadHash(posts) {
  updateForumData(posts, true /* fullSet */);
  return threadHash;
}

/**
 * Set whether to use UTC for displaying date/time
 */
function setDisplayUtc(utc) {
  displayUtc = utc ? true : false;
}

// called as special.parseHash
function parseHash(pieces) {
  cldrStatus.setCurrentPage("");
  if (pieces && pieces.length > 3) {
    if (!pieces[3] || pieces[3] == "") {
      cldrStatus.setCurrentId("");
    } else {
      const id = new Number(pieces[3]);
      if (id == NaN) {
        cldrStatus.setCurrentId("");
      } else {
        // e.g., http://localhost:8080/cldr-apps/v#forum/ar//69009
        const idStr = id.toString();
        cldrStatus.setCurrentId(idStr);
        handleIdChanged(idStr);
      }
    }
    return true;
  } else {
    return false;
  }
}

let formIsVisible = false;

function isFormVisible() {
  return formIsVisible;
}

function setFormIsVisible(visible) {
  formIsVisible = visible;
}

export {
  SUMMARY_CLASS,
  addNewPostButtons,
  handleIdChanged,
  isFormVisible,
  load,
  parseContent,
  parseHash,
  prefillPostText,
  refreshSummary,
  reload,
  sendPostRequest,
  setFormIsVisible,
  setUserCanPost,
  /*
   * The following are meant to be accessible for unit testing only:
   */
  getThreadHash,
  setDisplayUtc,
};

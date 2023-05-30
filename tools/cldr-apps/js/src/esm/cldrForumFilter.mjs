/*
 * cldrForumFilter: encapsulate filtering of forum threads.
 */
import * as cldrForumType from "./cldrForumType.mjs";
import * as cldrStatus from "./cldrStatus.mjs";

/**
 * An array of filter objects, each having a name and a boolean function
 */
const filters = [
  { name: "Needing action", func: passIfNeedingAction, keepCount: true },
  {
    name: "Open requests by you",
    func: passIfOpenRequestYouStarted,
    keepCount: true,
  },
  {
    name: "Open requests by others",
    func: passIfOpenRequestByOther,
    keepCount: true,
  },
  { name: "Open discussions", func: passIfOpenDiscuss, keepCount: true },
  {
    name: "Open topics without responses",
    func: passIfOpenWithoutResponse,
    keepCount: true,
  },
  { name: "All topics", func: passAll, keepCount: false },
];

/**
 * The index of the current filter in the "filters" array
 */
let filterIndex = 0;

/**
 * The id of the current user
 */
let filterUserId = 0;

/**
 * A function to call whenever a different filter is selected
 */
let filterReload = null;

/**
 * An object mapping filter names to the counts of threads passing those filters
 */
let filterCounts = {};

/**
 * Set the user id (the "you" in "you posted")
 *
 * @param userId the id of the current user
 */
function setUserId(userId) {
  filterUserId = userId;
}

/**
 * Get a popup menu from which the user can choose a filter, and set the reload function
 *
 * @param {Function} reloadFunction the reload function, for setting filterReload
 * @return {Node} the select element containing the menu
 */
function createMenu(reloadFunction) {
  filterReload = reloadFunction;
  const select = document.createElement("select");
  select.id = "forumFilterMenu";
  for (let i = 0; i < filters.length; i++) {
    const item = document.createElement("option");
    item.setAttribute("value", i);
    if (i === filterIndex) {
      item.setAttribute("selected", "selected");
    }
    item.appendChild(document.createTextNode(filters[i].name));
    select.appendChild(item);
  }
  select.addEventListener("change", function () {
    const index = parseInt(select.value, 10);
    if (index !== filterIndex) {
      filterIndex = index;
      if (filterReload) {
        filterReload();
      }
    }
  });
  const label = document.createElement("span");
  label.innerHTML = "Filter: ";
  const container = document.createElement("div");
  container.appendChild(label);
  container.appendChild(select);
  return container;
}

/**
 * Get an array of all the threadId strings for threads that pass the current filter
 *
 * Assume each post has post.threadId.
 *
 * @param {Object} threadHash an object mapping each threadId to an array of all the posts in that thread
 * @param {Boolean} applyFilter true if the currently menu-selected filter should be applied
 * @return {Array} the filtered array of threadId strings
 */
function getFilteredThreadIds(threadHash, applyFilter) {
  // Show this "exceptional" thread even if it doesn't pass the filter; if it matches the URL
  // like "100923" matches "...cldr-apps/v#forum/en_AU//100923" then don't filter it out
  const exceptionalId = cldrStatus.getCurrentId();
  const filteredArray = [];
  Object.keys(threadHash).forEach(function (threadId) {
    if (
      !applyFilter ||
      threadPasses(threadHash[threadId]) ||
      threadId == exceptionalId
    ) {
      filteredArray.push(threadId);
    }
  });
  if (applyFilter) {
    updateCounts(threadHash, filteredArray.length);
  }
  return filteredArray;
}

/**
 * Update the filterCounts map by calculating all the filter counts
 * for filters with keepCount true
 *
 * @param {Object} threadHash an object mapping each threadId to an array of all the posts in that thread
 * @param {Number} countCurrentFilter the count for the current filter, already calculated
 */
function updateCounts(threadHash, countCurrentFilter) {
  clearCounts();
  if (filters[filterIndex].keepCount) {
    filterCounts[filters[filterIndex].name] = countCurrentFilter;
  }
  Object.keys(threadHash).forEach(function (threadId) {
    for (let i = 0; i < filters.length; i++) {
      if (filters[i].keepCount && i !== filterIndex) {
        if (threadPassesI(threadHash[threadId], i)) {
          filterCounts[filters[i].name]++;
        }
      }
    }
  });
}

/**
 * Initialize the filterCounts map by setting to zero all the filter counts
 * for filters with keepCount true
 */
function clearCounts() {
  filters.forEach(function (filter) {
    if (filter.keepCount) {
      filterCounts[filter.name] = 0;
    }
  });
}

/**
 * Get an object mapping from certain filter names to the number
 * of threads currently passing those filters
 *
 * This only works right if getFilteredThreadIds was called
 */
function getFilteredThreadCounts() {
  return filterCounts;
}

/**
 * Does the thread with the given array of posts pass the current filter?
 *
 * @param {Array} threadPosts the array of posts in the thread
 * @return {Boolean} true if it passes, else false
 */
function threadPasses(threadPosts) {
  return filters[filterIndex].func(threadPosts);
}

/**
 * Does the thread with the given array of posts pass the filter with the given index?
 *
 * @param {Array} threadPosts the array of posts in the thread
 * @param {Number} i the index
 * @return {Boolean} true if it passes, else false
 */
function threadPassesI(threadPosts, i) {
  return filters[i].func(threadPosts);
}

/**************************/

/**
 * Pass all threads
 *
 * @param {Array} threadPosts the array of posts in the thread (unused)
 * @return {Boolean} true
 */
function passAll(threadPosts) {
  return true;
}

/**
 * Does the thread with the given array of posts need action?
 *
 * (Open request from others AND I haven’t agreed or declined)
 * OR (open discuss-only from others thread AND I haven’t responded and/or there was a follow up that I need to respond to again)
 * OR (request you declined, but the originator has added a follow up comment and not changed their vote)
 *
 * Note: "...and not changed their vote": if the originator changed their vote, the thread would be closed,
 * so we can assume here that the originator has not changed their vote.
 *
 * @param {Array} threadPosts the array of posts in the thread
 * @return {Boolean} true if it passes (needs action), else false
 */
function passIfNeedingAction(threadPosts) {
  const rootPost = getRootPost(threadPosts);
  if (!rootPost?.open) {
    return false;
  }
  if (
    rootPost.postType === cldrForumType.REQUEST ||
    rootPost.postType === cldrForumType.DISCUSS
  ) {
    if (!rootPost.poster || rootPost.poster === filterUserId) {
      return false; // not "from others"
    }
    return !hasYourUpToDateResponse(threadPosts);
  }
  return false;
}

/**
 * Is the thread with the given array of posts an open request started by the current user?
 *
 * @param {Array} threadPosts the array of posts in the thread
 * @return {Boolean} true or false
 */
function passIfOpenRequestYouStarted(threadPosts) {
  const rootPost = getRootPost(threadPosts);
  return (
    rootPost?.open &&
    rootPost.poster === filterUserId &&
    rootPost.postType === cldrForumType.REQUEST
  );
}

/**
 * Is the thread open and of type cldrForumType.REQUEST, started by other than the current user?
 *
 * Open Requests By Others should include all open Requests regardless of whether they need action
 * from you or not, and regardless of whether you have replied or not
 *
 * @param {Array} threadPosts the array of posts in the thread
 * @return {Boolean} true if it passes, else false
 */
function passIfOpenRequestByOther(threadPosts) {
  const rootPost = getRootPost(threadPosts);
  if (!rootPost.poster || rootPost.poster === filterUserId) {
    return false; // not "by other"
  }
  return rootPost?.open && rootPost.postType === cldrForumType.REQUEST;
}

/**
 * Is the thread open and of type cldrForumType.DISCUSS?
 *
 * Open Discussions should include all Open forum threads of type cldrForumType.DISCUSS".
 *
 * @param {Array} threadPosts the array of posts in the thread
 * @return {Boolean} true if it passes, else false
 */
function passIfOpenDiscuss(threadPosts) {
  const rootPost = getRootPost(threadPosts);
  return rootPost?.open && rootPost.postType === cldrForumType.DISCUSS;
}

/**
 * Does this thread include any responding (non-initial) posts by the current user,
 * which are up-to-date in the sense that the originator has not made a more recent post?
 *
 * @param {Array} threadPosts the array of posts in the thread
 * @return {Boolean} true if it has this user's up-to-date response, else false
 */
function hasYourUpToDateResponse(threadPosts) {
  const rootPost = getRootPost(threadPosts);
  const originator = rootPost.poster;
  /*
   * Go through the posts in reverse chronological order, i.e., most recent first.
   * The threadPosts array is already sorted with most recent first.
   *
   * If the order didn't matter, something like this with threadPosts.some (for future reference)
   * might be more efficient than a "for" loop:
   * function hasYourResponse(threadPosts) {
   *   return threadPosts.some((post) => post.poster && post.poster === filterUserId && post.parent !== -1);
   * }
   */
  for (let post of threadPosts) {
    if (post.poster === filterUserId) {
      return true;
    }
    if (post.poster === originator) {
      return false;
    }
  }
  return false;
}

/**
 * Get the root (original) post in the thread, i.e., the last one in the array
 *
 * @param {Array} threadPosts the array of posts in the thread
 * @return {Object} the root post, or null if not found
 */
function getRootPost(threadPosts) {
  /*
   * The root (original) post in the thread is the last one in the array
   */
  if (threadPosts.length < 1) {
    return null;
  }
  return threadPosts[threadPosts.length - 1];
}

/**
 * Is the thread with the given array of posts open, and without any responses?
 *
 * @param {Array} threadPosts the array of posts in the thread
 * @return {Boolean} true if it passes (open and without responses), else false
 */
function passIfOpenWithoutResponse(threadPosts) {
  const rootPost = getRootPost(threadPosts);
  return rootPost?.open && threadPosts.length === 1;
}

export { createMenu, getFilteredThreadCounts, getFilteredThreadIds, setUserId };

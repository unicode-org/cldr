/*
 * cldrForumFilter: encapsulate filtering of forum threads.
 */
import * as cldrForum from "./cldrForum.js";

/**
 * The zero-based index of 'Open topics' in the filters array
 */
const OPEN_TOPICS_INDEX = 2;

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
  { name: "Open topics", func: passIfOpen, keepCount: true },
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
 * @param reloadFunction the reload function, for setting filterReload
 * @return the select element containing the menu
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
 * @param threadHash an object mapping each threadId to an array of all the posts in that thread
 * @param applyFilter true if the currently menu-selected filter should be applied
 * @return the filtered array of threadId strings
 */
function getFilteredThreadIds(threadHash, applyFilter) {
  const filteredArray = [];
  Object.keys(threadHash).forEach(function (threadId) {
    if (!applyFilter || threadPasses(threadHash[threadId])) {
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
 * @param threadHash an object mapping each threadId to an array of all the posts in that thread
 * @param countCurrentFilter the count for the current filter, already calculated
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
  simplifyCounts();
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
 * If the count for 'Open threads' is zero, simplify filterCounts, since then there is
 * no need to display counts for 'Needing action' (implies open) or 'Open requests by you'
 */
function simplifyCounts() {
  if (filterCounts[filters[OPEN_TOPICS_INDEX].name] === 0) {
    for (let i = 0; i < filters.length; i++) {
      if (i !== OPEN_TOPICS_INDEX && filters[i].keepCount) {
        delete filterCounts[filters[i].name];
      }
    }
  }
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
 * @param threadPosts the array of posts in the thread
 * @return true or false
 */
function threadPasses(threadPosts) {
  return filters[filterIndex].func(threadPosts);
}

/**
 * Does the thread with the given array of posts pass the filter with the given index?
 *
 * @param threadPosts the array of posts in the thread
 * @param i the index
 * @return true or false
 */
function threadPassesI(threadPosts, i) {
  return filters[i].func(threadPosts);
}

/**************************/

/**
 * Pass all threads
 *
 * @param threadPosts the array of posts in the thread (unused)
 * @return true
 */
function passAll(threadPosts) {
  return true;
}

/**
 * Is the thread with the given array of posts open?
 *
 * @param threadPosts the array of posts in the thread
 * @return true or false
 */
function passIfOpen(threadPosts) {
  const rootPost = getRootPost(threadPosts);
  return rootPost && rootPost.open;
}

/**
 * Does the thread with the given array of posts need action?
 *
 * (Open request from others AND (I haven’t agreed or declined))
 * OR (open discuss-only thread AND I’m not the last poster)
 *
 * @param threadPosts the array of posts in the thread
 * @return true or false
 */
function passIfNeedingAction(threadPosts) {
  const rootPost = getRootPost(threadPosts);
  if (!rootPost || !rootPost.open) {
    return false;
  }
  if (rootPost.postType === "Request") {
    if (!rootPost.poster || rootPost.poster === filterUserId) {
      return false;
    }
    if (
      threadPosts.some(
        (post) =>
          post.poster &&
          post.poster === filterUserId &&
          (post.postType === "Agree" || post.postType === "Decline")
      )
    ) {
      return false;
    }
    return true;
  } else if (rootPost.postType === "Discuss") {
    const newestPost = threadPosts[0];
    return newestPost.poster && newestPost.poster !== filterUserId;
  }
  return false;
}

/**
 * Is the thread with the given array of posts open and was it started by the current user?
 *
 * @param threadPosts the array of posts in the thread
 * @return true or false
 */
function passIfOpenRequestYouStarted(threadPosts) {
  return passIfOpen(threadPosts) && passIfRequestYouStarted(threadPosts);
}

/**
 * Was the thread with the given array of posts a "Request" post started by the current user?
 *
 * @param threadPosts the array of posts in the thread
 * @return true or false
 */
function passIfRequestYouStarted(threadPosts) {
  const rootPost = getRootPost(threadPosts);
  return (
    rootPost &&
    rootPost.poster === filterUserId &&
    rootPost.postType === "Request"
  );
}

/**
 * Get the root (original) post in the thread, i.e., the last one in the array
 *
 * @param threadPosts the array of posts in the thread
 * @return the root post, or null if not found
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

export { createMenu, getFilteredThreadCounts, getFilteredThreadIds, setUserId };

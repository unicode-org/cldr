/*
 * cldrAnnounce: for Survey Tool announcements.
 * The display logic is in AnnouncePanel.vue.
 */
import * as cldrAjax from "./cldrAjax.mjs";
import * as cldrSchedule from "./cldrSchedule.mjs";
import * as cldrStatus from "./cldrStatus.mjs";

/**
 * This should be false for production. It can be made true during debugging, which
 * may be useful for performance testing.
 */
const DISABLE_ANNOUNCEMENTS = false;

const CLDR_ANNOUNCE_DEBUG = false;

const ANNOUNCE_REFRESH_SECONDS = 60; // one minute

const schedule = new cldrSchedule.FetchSchedule(
  "cldrAnnounce",
  ANNOUNCE_REFRESH_SECONDS,
  CLDR_ANNOUNCE_DEBUG
);

let thePosts = null;

let callbackSetData = null;
let callbackSetUnread = null;

/**
 * Get the number of unread announcements, to display in the main menu
 *
 * @param {Function} setUnreadCount the callback function for setting the unread count
 */
async function getUnreadCount(setUnreadCount) {
  if (setUnreadCount) {
    callbackSetUnread = setUnreadCount;
  }
  await refresh(callbackSetData);
}

/**
 * Refresh the Announcements page and/or unread count
 *
 * @param {Function} viewCallbackSetData the callback function for the Announcements page, or null
 *
 * The callback function for setting the data may be null if the Announcements page isn't open and
 * we're only getting the number of unread announcements to display in the main header
 */
async function refresh(viewCallbackSetData) {
  if (DISABLE_ANNOUNCEMENTS) {
    return;
  }
  if (viewCallbackSetData) {
    if (!callbackSetData) {
      // The Announcements page was just opened, so re-fetch the data immediately regardless
      // of how recently it was fetched previously (for unread count)
      schedule.reset();
    }
    callbackSetData = viewCallbackSetData;
  }
  if (!cldrStatus.getSurveyUser()) {
    if (viewCallbackSetData) {
      viewCallbackSetData(null);
    }
    return;
  }
  if (schedule.tooSoon()) {
    return;
  }
  const url = cldrAjax.makeApiUrl("announce", null);
  schedule.setRequestTime();
  return await cldrAjax
    .doFetch(url)
    .then(cldrAjax.handleFetchErrors)
    .then((r) => r.json())
    .then(setPosts)
    .catch((e) => console.error(e));
}

function setPosts(json) {
  thePosts = json;
  schedule.setResponseTime();
  if (callbackSetData) {
    callbackSetData(thePosts);
  }
  if (callbackSetUnread) {
    const totalCount = thePosts.announcements?.length || 0;
    let checkedCount = 0;
    for (let announcement of thePosts.announcements) {
      if (announcement.checked) {
        ++checkedCount;
      }
    }
    const unreadCount = totalCount - checkedCount;
    callbackSetUnread(unreadCount);
  }
  return thePosts;
}

function canAnnounce() {
  return cldrStatus.getPermissions()?.userIsManager || false;
}

function canChooseAllOrgs() {
  return cldrStatus.getPermissions()?.userIsTC || false;
}

async function compose(formState, viewCallbackComposeResult) {
  schedule.reset();
  const init = cldrAjax.makePostData(formState);
  const url = cldrAjax.makeApiUrl("announce", null);
  return await cldrAjax
    .doFetch(url, init)
    .then(cldrAjax.handleFetchErrors)
    .then((r) => r.json())
    .then(viewCallbackComposeResult)
    .catch((e) => console.error(e));
}

async function saveCheckmark(checked, announcement) {
  window.setTimeout(refreshCount, 1000);
  const init = cldrAjax.makePostData({
    id: announcement.id,
    checked: Boolean(checked),
  });
  const url = cldrAjax.makeApiUrl("announce/checkread", null);
  return await cldrAjax
    .doFetch(url, init)
    .then(cldrAjax.handleFetchErrors)
    .then((r) => r.json())
    .catch((e) => console.error(e));
}

function refreshCount() {
  if (callbackSetUnread) {
    getUnreadCount(callbackSetUnread); // update Main menu icon
  }
}

async function combineAndValidateLocales(locs, validateLocCallback) {
  await cldrAjax
    .doFetch(
      "./api/locales/combine-variants?" +
        new URLSearchParams({
          locs: locs,
        })
    )
    .then(cldrAjax.handleFetchErrors)
    .then((r) => r.json())
    .then(({ messages, normalized }) => {
      validateLocCallback(normalized, messages);
    })
    .catch((e) => console.error(`Error: ${e} validating locales`));
}

export {
  canAnnounce,
  canChooseAllOrgs,
  compose,
  getUnreadCount,
  refresh,
  saveCheckmark,
  combineAndValidateLocales,
};

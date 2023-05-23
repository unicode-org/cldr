/*
 * cldrAnnounce: for Survey Tool announcements.
 * The display logic is in AnnouncePanel.vue.
 */
import * as cldrAjax from "./cldrAjax.mjs";
import * as cldrStatus from "./cldrStatus.mjs";

let thePosts = null;

let callbackSetData = null;
let callbackSetUnread = null;

async function getUnreadCount(setUnreadCount) {
  callbackSetUnread = setUnreadCount;
  await refresh(callbackSetData);
}

async function refresh(viewCallbackSetData) {
  if (!cldrStatus.getSurveyUser()) {
    if (viewCallbackSetData) {
      viewCallbackSetData(null);
    }
    return;
  }
  callbackSetData = viewCallbackSetData;
  const url = cldrAjax.makeApiUrl("announce", null);
  return await cldrAjax
    .doFetch(url)
    .then(cldrAjax.handleFetchErrors)
    .then((r) => r.json())
    .then(setPosts)
    .catch((e) => console.error(e));
}

function setPosts(json) {
  thePosts = json;
  if (callbackSetData) {
    callbackSetData(thePosts);
  }
  if (callbackSetUnread) {
    let totalCount = thePosts.announcements?.length || 0;
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

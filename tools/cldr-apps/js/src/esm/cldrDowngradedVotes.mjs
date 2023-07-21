/*
 * cldrDowngradedVotes: Survey Tool feature for counting/deleting votes for downgraded paths
 */
import * as cldrAjax from "./cldrAjax.mjs";
import * as cldrNotify from "./cldrNotify.mjs";
import * as cldrStatus from "./cldrStatus.mjs";
import * as cldrText from "./cldrText.mjs";

/**
 * URL used with GET for getting statistics, or with DELETE for deleting votes
 */
const DOWNGRADE_URL = "voting/downgraded";

let callbackSetData = null;

async function refresh(viewCallbackSetData) {
  if (!hasPermission()) {
    viewCallbackSetData(null);
    return;
  }
  callbackSetData = viewCallbackSetData;
  const url = cldrAjax.makeApiUrl(DOWNGRADE_URL);
  return await cldrAjax
    .doFetch(url)
    .then(cldrAjax.handleFetchErrors)
    .then((r) => r.json())
    .then(setData)
    .catch(handleRefreshError);
}

function setData(json) {
  if (callbackSetData) {
    callbackSetData(json);
  }
  return json;
}

function handleRefreshError(e) {
  cldrNotify.exception(e, "loading stats about votes for downgraded paths");
}

function hasPermission() {
  return cldrStatus.getPermissions()?.userIsAdmin;
}

async function deleteAllImported() {
  const url = cldrAjax.makeApiUrl(DOWNGRADE_URL);
  const init = {
    method: "DELETE",
  };
  return await cldrAjax
    .doFetch(url, init)
    .then(cldrAjax.handleFetchErrors)
    .then((r) => r.json())
    .then(handleDeletionSuccess)
    .catch(handleDeletionError);
}

function handleDeletionSuccess(json) {
  cldrNotify.open(
    cldrText.get("downgradedDeletionSuccessHeader"),
    cldrText.get("downgradedDeletionSuccessDetail")
  );
  refresh(callbackSetData);
  return json;
}

function handleDeletionError(e) {
  cldrNotify.exception(e, "trying to delete votes for downgraded paths");
  refresh(callbackSetData);
}

export { deleteAllImported, hasPermission, refresh };

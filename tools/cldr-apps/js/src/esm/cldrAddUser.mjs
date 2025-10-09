/*
 * cldrAddUser: encapsulate code for adding a new Survey Tool user.
 */
import * as cldrAccount from "./cldrAccount.mjs";
import * as cldrAjax from "./cldrAjax.mjs";
import * as cldrLoad from "./cldrLoad.mjs";
import * as cldrOrganizations from "./cldrOrganizations.mjs";
import * as cldrStatus from "./cldrStatus.mjs";
import * as cldrUserLevels from "./cldrUserLevels.mjs";

const ALL_LOCALES = "*";

/**
 * Does the user have permission to add users?
 */
let canAdd = false;

/** @function */
let callbackToSetData = null;

const errors = [];

function hasPermission() {
  return canAdd;
}

async function viewMounted(setData) {
  callbackToSetData = setData;
  const perm = cldrStatus.getPermissions();
  canAdd = Boolean(perm?.userCanListUsers);
  if (!canAdd) {
    return;
  }
  await getLevelList();
  if (perm?.userIsAdmin) {
    setupOrgOptions();
  } else {
    getOrgLocales(cldrStatus.getOrganizationName());
  }
}

async function getLevelList() {
  cldrUserLevels.getLevelList().then(loadLevelList);
}

function loadLevelList(levelList) {
  if (!levelList) {
    addError("User-level list not received from server");
  } else {
    callbackToSetData({
      levelList,
    });
  }
}

/**
 * Set up the organization menu, for Admin only (canChooseOrg)
 */
async function setupOrgOptions() {
  const orgObject = await cldrOrganizations.get();
  if (!orgObject) {
    addError("Organization names not received from server");
  } else {
    callbackToSetData({
      orgObject,
    });
  }
}

async function getOrgLocales(orgName) {
  const resource = "./api/locales/org/" + orgName;
  await cldrAjax
    .doFetch(resource)
    .then(cldrAjax.handleFetchErrors)
    .then((r) => r.json())
    .then(setOrgLocales)
    .catch((e) => addError(`Error: ${e} getting org locales`));
}

function setOrgLocales(json) {
  if (json.err) {
    cldrRetry.handleDisconnect(json.err, json, "", "Loading org locales");
    return;
  }
  const orgLocales =
    json.locales == ALL_LOCALES
      ? Object.keys(cldrLoad.getTheLocaleMap().locmap.locales).join(" ")
      : json.locales;
  callbackToSetData({
    orgLocales,
  });
}

function getLocaleName(loc) {
  if (!loc) {
    return null;
  }
  return cldrLoad.getTheLocaleMap()?.getLocaleName(loc);
}

async function validateLocales(
  newUserOrg,
  newUserLocales,
  newUserLevel,
  levelList
) {
  const skipOrg = cldrUserLevels.canVoteInNonOrgLocales(
    newUserLevel,
    levelList
  );
  const orgForValidation = skipOrg ? "" : newUserOrg;
  const resource =
    "./api/locales/normalize?" +
    new URLSearchParams({
      locs: newUserLocales,
      org: orgForValidation,
    });
  await cldrAjax
    .doFetch(resource)
    .then(cldrAjax.handleFetchErrors)
    .then((r) => r.json())
    .then(({ messages, normalized }) => {
      if (newUserLocales != normalized) {
        // only update the warnings if the normalized value changes
        newUserLocales = normalized;
        if (messages) {
          callbackToSetData({
            validatedLocales: {
              locWarnings: messages,
              newUserLocales: normalized,
            },
          });
        }
      }
    })
    .catch((e) => addError(`Error: ${e} validating locale`));
}

function add(postData) {
  const xhrArgs = {
    url: cldrAjax.makeApiUrl("adduser", null),
    postData: postData,
    handleAs: "json",
    load: loadHandler,
    error: (err) => addError(err),
  };
  cldrAjax.sendXhr(xhrArgs);
}

function loadHandler(json) {
  if (json.err) {
    addError("Error from the server: " + translateErr(json.err));
  } else if (!json.userId) {
    addError("The server did not return a user id.");
  } else {
    const n = Math.floor(Number(json.userId));
    if (String(n) !== String(json.userId) || n <= 0 || !Number.isInteger(n)) {
      addError("The server returned an invalid id: " + json.userId);
    } else {
      // json.email is normalized, e.g., to lower case, by server
      callbackToSetData({
        newUser: { id: Number(json.userId), email: json.email },
      });
    }
  }
}

function addError(message) {
  const index = errors.indexOf(message);
  if (index < 0) {
    errors.push(message);
  }
  callbackToSetData({ error: message });
}

function removeError(message) {
  const index = errors.indexOf(message);
  if (index > -1) {
    errors.splice(index, 1);
  }
}

function clearErrors() {
  errors.length = 0;
  if (errorsExist()) {
    console.error("clearErrors failure");
  }
}

function errorsExist() {
  return Boolean(errors.length);
}

function getErrors() {
  return errors;
}

function translateErr(err) {
  const map = {
    BAD_NAME: "Missing or invalid name",
    BAD_EMAIL: "Missing or invalid e-mail",
    BAD_ORG: "Missing or invalid organization",
    BAD_LEVEL: "Missing, invalid, or forbidden user level",
    DUP_EMAIL: "A user with that e-mail already exists",
    UNKNOWN: "An unspecified error occurred",
  };
  if (!map[err]) {
    return err;
  }
  return map[err] + " [" + err + "]";
}

function manageThisUser(email) {
  cldrAccount.zoomUser(email);
}

export {
  ALL_LOCALES,
  add,
  addError,
  clearErrors,
  errorsExist,
  getErrors,
  getLocaleName,
  getOrgLocales,
  hasPermission,
  manageThisUser,
  removeError,
  validateLocales,
  viewMounted,
};

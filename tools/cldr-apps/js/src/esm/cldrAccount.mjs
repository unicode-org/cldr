/*
 * cldrAccount: Survey Tool features for My Account Settings and List Users
 */
import * as cldrAjax from "./cldrAjax.mjs";
import * as cldrDom from "./cldrDom.mjs";
import * as cldrLoad from "./cldrLoad.mjs";
import * as cldrNotify from "./cldrNotify.mjs";
import * as cldrOrganizations from "./cldrOrganizations.mjs";
import * as cldrStatus from "./cldrStatus.mjs";
import * as cldrSurvey from "./cldrSurvey.mjs";
import * as cldrText from "./cldrText.mjs";
import * as cldrUserLevels from "./cldrUserLevels.mjs";

const CLDR_ACCOUNT_DEBUG = false;
const SHOW_GRAVATAR = !CLDR_ACCOUNT_DEBUG;

const WHAT_USER_LIST = "user_list"; // cf. SurveyAjax.WHAT_USER_LIST
const LIST_JUST = "justu"; // cf. UserList.LIST_JUST

// cf. UserList.java and SurveyMain.java for these constants
const LIST_ACTION_SETLEVEL = "set_userlevel_";
const LIST_ACTION_NONE = "-";
const LIST_ACTION_SHOW_PASSWORD = "showpassword_";
const LIST_ACTION_SEND_PASSWORD = "sendpassword_";
const LIST_ACTION_SETLOCALES = "set_locales_";
const LIST_ACTION_DELETE0 = "delete0_";
const LIST_ACTION_DELETE1 = "delete_";
const LIST_MAILUSER = "mailthem";
const LIST_MAILUSER_WHAT = "mailthem_t";
const LIST_MAILUSER_CONFIRM = "mailthem_c";
const LIST_MAILUSER_CONFIRM_CODE = "confirm";
const PREF_SHOWLOCKED = "p_showlocked";
const PREF_JUSTORG = "p_justorg";

const userListTableId = "userListTable";

const participatingUsersButton =
  "<button id='participatingUsersButton'>E-mail addresses of users who participated</button>";

const addUserButton = "<button id='addUserButton'>Add user</button>";

const cautionSessionDestruction =
  "<div class='fnotebox'>Changing user level or locales while a user is active will " +
  "result in destruction of their session. Check if they have been working recently.</div>\n";

// use class not id since this button is shown twice
const listMultipleUsersButton =
  "<button class='listMultipleUsers' type='button'>⋖ Show all users</button>\n";

const doActionButton =
  "<button type='button' class='doActionButton'>Do Action</button>\n";

const zoomImage =
  "<img alt='[zoom]' style='width: 16px; height: 16px; border: 0;' src='/cldr-apps/zoom.png' title='More on this user...' />";

const passwordActions = {
  // brackets around keys are for computed property names (ECMAScript 2015)
  [LIST_ACTION_SHOW_PASSWORD]: "Show password...",
  [LIST_ACTION_SEND_PASSWORD]: "Send password...",
};

const bulkActions = {
  [LIST_ACTION_NONE]: LIST_ACTION_NONE,
  [LIST_ACTION_SHOW_PASSWORD]: "Show password URL...",
  [LIST_ACTION_SEND_PASSWORD]: "Resend password...",
};

const bulkActionListButton =
  "<button id='bulkActionListButton' type='button'>list</button>\n";

const bulkActionChangeButtonDiv =
  "<div id='changeButton' style='display: none;'>" +
  "<hr /><i><b>Menus have been pre-filled.<br />" +
  "Confirm your choices and click Do Action.</b></i><br />" +
  doActionButton +
  "</div>\n";

const infoType = {
  // this MUST agree with UserRegistry.InfoType
  INFO_EMAIL: "E-mail",
  INFO_NAME: "Name",
  INFO_PASSWORD: "Password",
  INFO_ORG: "Organization",
};

/**
 * Is the table displaying only information about the current user?
 * This module is used both for "My Account", with isJustMe = true,
 * and "List Users" (through cldrListUsers.js), with isJustMe = false.
 */
let isJustMe = false;

/**
 * The email address of the "zoomed" or "My Account" user, or null if neither zoomed nor My Account.
 * The "List Users" table may be "zoomed" to display only a single user. In general,
 * zooming a user in the "List Users" page is not the same as a user's "My Account" view, since
 * one may zoom in on someone else's account. Zooming in on one's own account is essentially the
 * same as choosing My Account, Settings.
 */
let justUser = null;

let showLockedUsers = false;
let orgs = null;
let levelList = null;
let shownUsers = null;
let justOrg = null;
let byEmail = {};

let hideAllUsers = false;

let loadListMultipleOnce = false;
let loadListZoomUserOnce = false;

/**
 * Load the "List Users" page on behalf of cldrListUsers
 *
 * Change the hash from #list_users to #account; switching back and forth between
 * My Account and List Users is done with ajax; don't want distinct hash in menu bar.
 * Redirect after a short timeout to enable the callers of cldrListUsers to finish.
 */
function loadListUsers() {
  setTimeout(function () {
    loadListMultipleOnce = true;
    cldrStatus.setCurrentSpecial("account");
    cldrLoad.reloadV();
  }, 100 /* one tenth of a second */);
}

/**
 * Load the "List Users" page, zoomed in on a particular user
 * This is used when a new user (not the current user) has just been added
 */
function zoomUser(email) {
  setTimeout(function () {
    justUser = email;
    loadListZoomUserOnce = true;
    cldrStatus.setCurrentSpecial("account");
    cldrLoad.reloadV();
  }, 100 /* one tenth of a second */);
}

/**
 * Load the "My Accounts, Settings" page
 * -- called as special.load
 *
 * Also called by way of loadListUsers() to load the "List Users" page,
 * if loadListMultipleOnce has been set to true
 */
function load() {
  const me = cldrStatus.getSurveyUser();
  if (!me || !me.email) {
    pleaseLogIn();
  } else if (loadListMultipleOnce) {
    loadListMultipleOnce = false;
    listMultipleUsers();
  } else if (loadListZoomUserOnce) {
    loadListZoomUserOnce = false;
    listSingleUser(justUser);
  } else {
    listSingleUser(me.email);
  }
}

function pleaseLogIn() {
  const ourDiv = document.createElement("div");
  ourDiv.innerHTML = "Please log in to access account settings";
  cldrSurvey.hideLoader();
  cldrLoad.flipToOtherDiv(ourDiv);
}

/**
 * List account info for multiple users
 *
 * This may be in response to a "List ... Users" menu command,
 * or a "Show all users" button in a page either for a zoomed user or for My Account
 */
function listMultipleUsers() {
  justUser = null;
  isJustMe = false;
  reallyLoad();
}

/**
 * List account info for a single user -- omit all other users from the table
 *
 * This may be in response to a "My Account" menu command, or a zoom button in List Users
 *
 * @param {String} email - the email address of the user of interest
 */
function listSingleUser(email) {
  justUser = email;
  const me = cldrStatus.getSurveyUser();
  isJustMe = me && me.email && email === me.email;
  reallyLoad();
}

function reallyLoad() {
  getOrgsAndLevels()
    .catch((e) => cldrNotify.exception(e, `loading Account Settings page`))
    .then(reallyReallyLoad);
}

async function getOrgsAndLevels() {
  orgs = await cldrOrganizations.get();
  if (!orgs) {
    throw new Error("Organization names not received from server");
  }
  levelList = await cldrUserLevels.getLevelList();
  if (!levelList) {
    throw new Error("User levels not received from server");
  }
}

function reallyReallyLoad() {
  const xhrArgs = {
    url: getUrl(),
    handleAs: "json",
    load: loadHandler,
    error: errorHandler,
  };
  cldrAjax.sendXhr(xhrArgs);
}

function loadHandler(json) {
  const ourDiv = document.createElement("div");
  if (json.err) {
    ourDiv.innerHTML = json.err;
  } else {
    shownUsers = json.shownUsers;
    if (
      justUser &&
      shownUsers.length === 1 &&
      getSelectedAction(shownUsers[0]) === "change_INFO_EMAIL"
    ) {
      justUser = shownUsers[0].email;
    }
    ourDiv.innerHTML = getHtml(json);
  }
  cldrSurvey.hideLoader();
  cldrLoad.flipToOtherDiv(ourDiv);
  if (!json.err) {
    showUserActivity(json);
    setOnClicks();
  }
}

function errorHandler(err) {
  const ourDiv = document.createElement("div");
  ourDiv.innerHTML = err;
  cldrSurvey.hideLoader();
  cldrLoad.flipToOtherDiv(ourDiv);
}

function getHtml(json) {
  let html = "";
  if (isJustMe) {
    html += "<h2>My Account</h2>\n";
  } else {
    const org = json.org ? orgs.shortToDisplay[json.org] : "ALL";
    html += "<h2>Users for " + org + "</h2>\n";
  }
  html += getEmailNotification(json);
  if (!isJustMe) {
    html += "<p>" + addUserButton;
    if (json.canGetEmailList) {
      html += " " + participatingUsersButton;
    }
    html += "</p>\n";
    if (orgs && !justUser && cldrStatus.getPermissions()?.userIsAdmin) {
      html += getOrgFilterMenu();
    }
  }
  if (justUser) {
    html += "<p>" + listMultipleUsersButton + "</p>\n";
  }
  if (!isJustMe) {
    html += getLockedUsersControl(json);
    html += cautionSessionDestruction;
    html += getBulkActionMenu(json);
    html += getListHiderControls(json);
  }
  html += getTable(json);
  html += getInterestLocalesHtml(json);
  html += getDownloadCsvForm(json);
  if (json.exception) {
    html += "<p><i>Failure: " + json.exception + "</i></p>\n";
  }
  return html;
}

function getTable(json) {
  shownUsers = json.shownUsers; // redundant except for unit test
  byEmail = {};
  let html = getTableStart();
  for (let org of getSortedOrgsToShow()) {
    const orgDisplayName = orgs.shortToDisplay[org];
    html +=
      "<tr class='heading'><th class='partsection' colspan='6'><a name='" +
      org +
      "'><h4>" +
      orgDisplayName +
      "</h4></a></th></tr>\n";
    for (let userData of shownUsers) {
      if (org === userData.org) {
        const u = {
          data: userData,
        };
        byEmail[userData.email] = u;
        html += getUserTableRow(u, json);
      }
    }
  }
  html += getTableEnd(json);
  return html;
}

function getSortedOrgsToShow() {
  const sortedOrgs = [];
  const orgsFound = {};
  for (let userData of shownUsers) {
    orgsFound[userData.org] = true;
  }
  for (let displayName of orgs.sortedDisplayNames) {
    const shortName = orgs.displayToShort[displayName];
    if (orgsFound[shortName]) {
      sortedOrgs.push(shortName);
    }
  }
  return sortedOrgs;
}

function getTableStart() {
  return (
    "<form id='tableForm' action=''>\n" +
    doActionButton +
    "<table id='" +
    userListTableId +
    "' summary='User List' class='userlist' border='2'>\n" +
    "<thead><tr><th></th><th style='display: none;'>Organization / Level</th><th>Name/Email</th>" +
    "<th>Action</th><th>Locales</th><th>Seen</th></tr></thead>\n" +
    "<tbody>\n"
  );
}

function getTableEnd(json) {
  let html = "</tbody></table>" + "<br />\n";
  if (justUser) {
    html += listMultipleUsersButton;
  } else {
    html += numberOfUsersShown(shownUsers ? shownUsers.length : 0);
    html += getEmailControls(json);
  }
  html += doActionButton + "</form>\n";
  return html;
}

function numberOfUsersShown(number) {
  return (
    "<div style='font-size: 70%'>Number of users shown: " + number + "</div>\n"
  );
}

function submitTableForm() {
  const textArea = document.getElementsByTagName("textarea")[0];
  if (textArea) {
    textArea.value = textArea.value.replaceAll("\n", "<br>\n");
  }
  const formEl = document.getElementById("tableForm"); // maybe not event.target
  const data = new FormData(formEl);
  const xhrArgs = {
    url: getUrl(),
    postData: data,
    handleAs: "json",
    load: loadHandler,
    error: errorHandler,
  };
  cldrAjax.sendXhr(xhrArgs);
}

function getUserTableRow(u, json) {
  return (
    "<tr id='u@" +
    u.data.id +
    "'>\n" +
    // 1st column (no header): "zoom" icon, or empty for justUser/isJustme
    "<td>" +
    getFirstCol(u, json) +
    "</td>" +
    // 2nd column is hidden -- what's it for? Maybe "org", per showUserActivity?
    "<td style='display: none;'></td>" +
    // 3rd column: "Name/Email"; has gravatar icon, etc., filled in by showUserActivity
    "<td valign='top'></td>" +
    // 4th column: "Action"; menu, links to Upload XMl and User Activity
    "<td>" +
    getUserActions(u, json) +
    "</td>" +
    // 5th column: "Locales"
    "<td>" +
    getUserLocales(u) +
    "</td>" +
    // 6th column: "Seen"
    "<td>" +
    getUserSeen(u) +
    "</td>\n" +
    "</tr>\n"
  );
}

function getFirstCol(u, json) {
  let html = "";
  if (u.data.actions) {
    if (u.data.actions[LIST_ACTION_SHOW_PASSWORD]) {
      html += getPasswordLink(
        u.data.email,
        u.data.actions[LIST_ACTION_SHOW_PASSWORD]
      );
    } else if (u.data.actions[LIST_ACTION_SEND_PASSWORD]) {
      html += "Password sent. ";
      html += getPasswordLink(u.data.email, "");
    } else {
      for (const [action, text] of Object.entries(u.data.actions)) {
        html += text;
      }
    }
  }
  if (!justUser) {
    html +=
      "<a class='zoomUserButton' title='" +
      u.data.email +
      "'>" +
      zoomImage +
      "</a>";
  }
  return html;
}

function getPasswordLink(email, password) {
  return (
    "<a href='" +
    getLoginUrl(email, password) +
    "'>Login for " +
    email +
    "</a> <tt class='winner'>" +
    password +
    "</tt> "
  );
}

function getTransferLink(u) {
  if (cldrStatus.getPermissions()?.userIsManager) {
    return `<a title="Pick this option if you want to copy votes from someone else to ${u.data.name}"
      href="v?transferTo=${u.data.id}#transfervotes">Copy votes to this vetter</a><br />`;
  } else {
    return "";
  }
}

function getUserActions(u, json) {
  return (
    getUserActionMenu(u, json) +
    "<br />\n" +
    getXmlUploadLink(u) +
    "<br />\n" +
    getTransferLink(u) +
    getUserActivityLink(u)
  );
}

function getUserActionMenu(u, json) {
  const theirTag = u.data.id + "_" + u.data.email;
  let html = "<select class='userActionMenuSelect' name='" + theirTag + "'>\n";
  const theirLevel = u.data.userlevel;
  html += "<option value=''>" + LIST_ACTION_NONE + "</option>\n";
  if (json.userPerms?.canModifyUsers) {
    html += getChangeLevelOptions(u, theirLevel);
    html += "<option disabled='disabled'>" + LIST_ACTION_NONE + "</option>\n";
    for (const [action, text] of Object.entries(passwordActions)) {
      html += getPasswordOption(theirLevel, json, action, text);
    }
  }
  if (justUser) {
    html += getJustUserActionMenuOptions(u, json);
  }
  html += "</select>";
  return html;
}

function getChangeLevelOptions(u, theirLevel) {
  let html = "";
  // User shouldn’t be able to change their own user level
  if (!isJustMe) {
    const me = cldrStatus.getSurveyUser();
    if (!(me && me.email && u.data.email === me.email)) {
      for (let number in levelList) {
        const name = levelList[number].name;
        if (cldrUserLevels.match(name, cldrUserLevels.ANONYMOUS)) {
          continue;
        }
        // only allow mass LOCK
        if (justUser || cldrUserLevels.match(name, cldrUserLevels.LOCKED)) {
          html += doChangeUserOption(number, theirLevel);
        }
      }
    }
  }
  return html;
}

function doChangeUserOption(newNumber, oldNumber) {
  const s = levelList[newNumber].string; // e.g., "999: (LOCKED)"
  if (levelList[oldNumber].canCreateOrSetLevelTo) {
    return (
      "<option value='" +
      LIST_ACTION_SETLEVEL +
      newNumber +
      "'>Make " +
      s +
      "</option>\n"
    );
  } else {
    return "<option disabled='disabled'>Make " + s + "</option>\n";
  }
}

function getPasswordOption(theirLevel, json, action, text) {
  let html = "<option";
  if (json.preset_fromint == theirLevel && json.preset_do.equals(action)) {
    html += " selected='selected'";
  }
  html += " value='" + action + "'>" + text + "</option>\n";
  return html;
}

function getJustUserActionMenuOptions(u, json) {
  let html = "";
  if (u.data.havePermToChange) {
    html += getSetLocalesOption();
  }
  if (u.data.userCanDeleteUser) {
    html += "<option disabled='disabled'>" + LIST_ACTION_NONE + "</option>\n"; // separator
    html += getDeleteUserOptions(u, json);
  }
  if (json.userPerms?.canModifyUsers) {
    html += " <option disabled='disabled'>" + LIST_ACTION_NONE + "</option>\n"; // separator
    const selectedAction = getSelectedAction(u.data);
    for (const [info, title] of Object.entries(infoType)) {
      if (info === "INFO_ORG" && !cldrStatus.getPermissions().userIsAdmin) {
        continue;
      }
      // INFO_EMAIL makes change_INFO_EMAIL, etc.; must be mixed case
      const changeInfo = "change_" + info;
      html += " <option";
      if (changeInfo === selectedAction) {
        html += " selected='selected'";
      }
      html += " value='" + changeInfo + "'>Change " + title + "...</option>\n";
    }
  }
  return html;
}

/**
 * Get the most recently chosen value in the actions menu, based on the server response.
 *
 * If the server response includes an object "actions" with at least
 * one key, assume the first such key matches the most recently chosen item
 * in the actions menu.
 *
 * This awkward implementation is due to incomplete modernization of the old
 * implementation which was all java, no javascript.
 *
 * @return the value such as "change_INFO_EMAIL", or null
 */
function getSelectedAction(userData) {
  if (userData.actions) {
    const k = Object.keys(userData.actions);
    if (k.length > 0) {
      return k[0];
    }
  }
  return null;
}

function getSetLocalesOption() {
  return (
    "<option value='" + LIST_ACTION_SETLOCALES + "'>Set locales...</option>\n"
  );
}

function getDeleteUserOptions(u, json) {
  let html = "";
  if (u.data.actions[LIST_ACTION_DELETE0]) {
    html +=
      "<option value='" +
      LIST_ACTION_DELETE1 +
      "' selected='selected'>Confirm delete</option>\n";
  } else {
    html += "<option";
    if (
      json.preset_fromint == u.data.userlevel &&
      json.preset_do.equals(LIST_ACTION_DELETE0)
    ) {
      html += " selected='selected'";
    }
    html += " value='" + LIST_ACTION_DELETE0 + "'>Delete user...</option>\n";
  }
  return html;
}

function getUserLocales(u) {
  const MANAGER_LEVEL = cldrUserLevels.getUserLevel(
    cldrUserLevels.MANAGER,
    levelList
  );
  const theirLevel = u.data.userlevel;
  if (
    theirLevel <= MANAGER_LEVEL ||
    u.data.locales === "*" ||
    u.data.locales === "all" ||
    u.data.locales === "all locales"
  ) {
    return "<i>all locales</i>";
  } else if (!u.data.locales || u.data.locales === "no locales") {
    return "<i>no locales</i>";
  } else {
    return prettyLocaleList(u.data.locales);
  }
}

function getInterestLocalesHtml(json) {
  if (!isJustMe || !json.canSetInterestLocales || !byEmail[justUser]) {
    return "";
  }
  const u = byEmail[justUser];
  let html =
    "<hr /><h4>Notify me about these locale groups (just the language names, no underscores or dashes):</h4>\n";
  html += "<p id='changeInterestLocalesControls'>\n";
  if (json.changedLocalesMessage) {
    html += json.changedLocalesMessage;
  }
  html += getUserInterestLocales(u);
  html += " <a id='changeInterestLocales'>[Change this]</a>\n";
  html += "</p>\n";
  return html;
}

function changeInterestLocales(result) {
  if (!isJustMe || !byEmail[justUser]) {
    return;
  }
  const u = byEmail[justUser];
  const el = document.getElementById("changeInterestLocalesControls");
  if (!el) {
    return;
  }
  let html = "";
  if (result) {
    html += "Result: " + result + "<br />";
  }
  html += "<label>Locales: <input id='intLocs' ";
  if (u.data.intlocs) {
    html += "value='" + u.data.intlocs + "' ";
  }
  html +=
    "></input></label><button id='intLocsButton' type='button'>Set</button><br />\n";
  if (u.data.intlocs) {
    html += getUserInterestLocales(u);
  } else {
    html +=
      "<i>List languages only, separated by spaces. Example: <tt class='codebox'>en fr zh</tt>. Leave blank for 'all locales'.</i>\n";
  }
  el.innerHTML = html;
  const button = document.getElementById("intLocsButton");
  button.onclick = (event) => reallyChangeInterestLocales(event);
}

function getUserInterestLocales(u) {
  if (u.data.intlocs) {
    return prettyLocaleList(u.data.intlocs);
  }
  return "(no locales)";
}

function reallyChangeInterestLocales(event) {
  const el = document.getElementById("intLocs");
  if (!el) {
    return;
  }
  const interestLocales = el.value;
  const xhrArgs = {
    url: getInterestLocalesUrl(),
    handleAs: "json",
    postData: getInterestLocalesPostData(interestLocales),
    load: intLocsLoadHandler,
    error: intLocsErrorHandler,
  };
  cldrAjax.sendXhr(xhrArgs);
}

function intLocsLoadHandler(json) {
  if (json.email && json.email === justUser) {
    const u = byEmail[justUser];
    if (u) {
      u.data.intlocs = json.intlocs;
    }
  }
  if (!json.status) {
    json.status = "(no status)";
  }
  changeInterestLocales(json.status);
}

function intLocsErrorHandler(err) {
  changeInterestLocales("Error: " + err);
}

// locales could be either u.data.locales or u.data.intlocs
function prettyLocaleList(locales) {
  const map = cldrLoad.getTheLocaleMap();
  let html = "";
  locales.split(" ").forEach((loc) => {
    const info = map.getLocaleInfo(loc);
    let name = "";
    if (info && info.name) {
      name = info.name;
    } else {
      name = "unknown";
      if (CLDR_ACCOUNT_DEBUG) {
        console.log(
          "prettyLocaleList: unrecognized loc = [" +
            loc +
            "]; locales = [" +
            locales +
            "]"
        );
      }
    }
    html += " <tt class='codebox' title='" + name + "'>" + loc + "</tt> ";
  });
  return html;
}

function getUserSeen(u) {
  const when = u.data.active ? u.data.active : u.data.seen;
  if (!when) {
    return "";
  }
  const what = u.data.active ? "active" : "seen";
  let html = "<b>" + what + ": " + when + "</b>";
  if (what === "seen") {
    html += "<br /><font size='-2'>" + u.data.lastlogin + "</font></td>";
  }
  return html;
}

function showUserActivity(json) {
  const shownUsers = json.shownUsers;
  const table = document.getElementById(userListTableId);
  const rows = [];
  const theadChildren = cldrSurvey.getTagChildren(
    table.getElementsByTagName("thead")[0].getElementsByTagName("tr")[0]
  );
  cldrDom.setDisplayed(theadChildren[1], false);
  const rowById = [];
  for (let i in shownUsers) {
    const user = shownUsers[i];
    rowById[user.id] = parseInt(i);
    showOneUserActivity(user, rows);
  }

  const xhrArgs = {
    url: getUserActivityUrl(),
    handleAs: "json",
    load: actLoadHandlerClosure,
    err: actErrHandler,
  };

  cldrAjax.sendXhr(xhrArgs);

  function actLoadHandlerClosure(json) {
    actLoadHandler(json, rowById, rows);
  }
}

function showOneUserActivity(user, rows) {
  const tr = document.getElementById("u@" + user.id);
  if (!tr) {
    console.log("Missing tr for id " + user.id);
    return;
  }
  const rowChildren = cldrSurvey.getTagChildren(tr);
  cldrDom.removeAllChildNodes(rowChildren[1]); // org
  cldrDom.removeAllChildNodes(rowChildren[2]); // name
  if (!rowChildren[1]) {
    console.log("Missing rowChildren[1] for id " + user.id);
    return;
  }
  cldrDom.setDisplayed(rowChildren[1], false);
  const theUser = createUser(user);
  rowChildren[2].appendChild(theUser);
  rows.push({
    user: user,
    tr: tr,
    userDiv: theUser,
    seen: rowChildren[5],
    stats: [],
    total: 0,
  });
}

function actLoadHandler(json, rowById, rows) {
  const loc2name = setupLoc2Name(json, rowById, rows);
  for (let i in rows) {
    const userRow = rows[i];
    if (userRow.total > 0) {
      cldrDom.addClass(userRow.tr, "hadActivity");
      userRow.tr
        .getElementsByClassName("recentActivity")[0]
        .appendChild(document.createTextNode(" (" + userRow.total + ")"));
      userRow.seenSub = document.createElement("div");
      userRow.seenSub.className = "seenSub";
      userRow.seen.appendChild(userRow.seenSub);
      appendMiniChart(loc2name, userRow, 3);
      if (userRow.stats.length > 3) {
        makeUserCharts(loc2name, userRow);
      }
    } else {
      cldrDom.addClass(userRow.tr, "noActivity");
    }
  }
}

function makeUserCharts(loc2name, userRow) {
  const chartMore = cldrDom.createChunk("+", "span", "chartMore");
  const chartLess = cldrDom.createChunk("-", "span", "chartMore");
  chartMore.onclick = (function (chartMore, chartLess, userRow) {
    return function () {
      cldrDom.setDisplayed(chartMore, false);
      cldrDom.setDisplayed(chartLess, true);
      appendMiniChart(loc2name, userRow, userRow.stats.length);
      return false;
    };
  })(chartMore, chartLess, userRow);
  chartLess.onclick = (function (chartMore, chartLess, userRow) {
    return function () {
      cldrDom.setDisplayed(chartMore, true);
      cldrDom.setDisplayed(chartLess, false);
      appendMiniChart(loc2name, userRow, 3);
      return false;
    };
  })(chartMore, chartLess, userRow);
  userRow.seen.appendChild(chartMore);
  cldrDom.setDisplayed(chartLess, false);
  userRow.seen.appendChild(chartLess);
}

function setupLoc2Name(json, rowById, rows) {
  const loc2name = {};
  const stats = json.stats_bydayuserloc;
  if (!stats) {
    console.log("Missing json.stats_bydayuserloc in cldrAccount.setupLoc2Name");
    return loc2name;
  }
  const header = stats.header;
  for (let i in stats.data) {
    const row = stats.data[i];
    const submitter = row[header.SUBMITTER];
    const submitterRow = rowById[submitter];
    if (submitterRow !== undefined) {
      const userRow = rows[submitterRow];
      userRow.stats.push({
        day: row[header.DAY],
        count: row[header.COUNT],
        locale: row[header.LOCALE],
      });
      userRow.total = userRow.total + row[header.COUNT];
      loc2name[row[header.LOCALE]] = row[header.LOCALE_NAME];
    }
  }
  return loc2name;
}

function appendMiniChart(loc2name, userRow, count) {
  if (count > userRow.stats.length) {
    count = userRow.stats.length;
  }
  cldrDom.removeAllChildNodes(userRow.seenSub);
  let chartRow = null;
  for (let i = 0; i < count; i++) {
    const theStat = userRow.stats[i];
    chartRow = cldrDom.createChunk("", "div", "chartRow");
    const chartDay = cldrDom.createChunk(theStat.day, "span", "chartDay");
    const chartLoc = cldrDom.createChunk(theStat.locale, "span", "chartLoc");
    chartLoc.title = loc2name[theStat.locale];
    const chartCount = cldrDom.createChunk(theStat.count, "span", "chartCount");

    chartRow.appendChild(chartDay);
    chartRow.appendChild(chartLoc);
    chartRow.appendChild(chartCount);

    userRow.seenSub.appendChild(chartRow);
  }
  if (chartRow && count < userRow.stats.length) {
    chartRow.appendChild(document.createTextNode("..."));
  }
}

function actErrHandler(err) {
  console.log("Error getting user activity: " + err);
}

/**
 * Create a DOM object referring to a user.
 *
 * @param {JSON} user - user struct
 * @return {Object} new DOM object
 */
function createUser(user) {
  const userLevelLc = user.userlevelName.toLowerCase();
  const userLevelClass = "userlevel_" + userLevelLc;
  const userLevelStr = cldrText.get(userLevelClass);
  const div = cldrDom.createChunk(null, "div", "adminUserUser");
  if (SHOW_GRAVATAR) {
    div.appendChild(cldrSurvey.createGravatar(user));
  }
  div.userLevel = cldrDom.createChunk(userLevelStr, "i", userLevelClass);
  div.appendChild(div.userLevel);
  div.appendChild(
    (div.userName = cldrDom.createChunk(user.name, "span", "adminUserName"))
  );
  if (!user.orgName) {
    user.orgName = user.org;
  }
  div.appendChild(
    (div.userOrg = cldrDom.createChunk(
      user.orgName + " #" + user.id,
      "span",
      "adminOrgName"
    ))
  );
  div.appendChild(
    (div.userEmail = cldrDom.createChunk(
      user.email,
      "address",
      "adminUserAddress"
    ))
  );
  return div;
}

function getLockedUsersControl(json) {
  if (!json.canShowLocked) {
    return "";
  }
  const ch = showLockedUsers ? " checked='checked'" : "";
  return (
    "<input type='checkbox' id='showLocked'" +
    ch +
    " /> <label for='showLocked'>Show locked users</label><br />\n"
  );
}

function toggleShowLocked() {
  showLockedUsers = !showLockedUsers;
  reallyLoad();
}

function getOrgFilterMenu() {
  if (!justOrg) {
    justOrg = "all";
  }
  let html =
    "<label class='menutop-active'>Filter Organization " +
    "<select id='filterOrgSelect' class='menutop-other'>\n" +
    "<option value='all'>Show All</option>\n";
  for (let displayName of orgs.sortedDisplayNames) {
    const shortName = orgs.displayToShort[displayName];
    const sel = shortName === justOrg ? " selected='selected'" : "";
    html +=
      "<option value='" +
      shortName +
      "'" +
      sel +
      ">" +
      displayName +
      "</option>\n";
  }
  html += "</select>\n";
  html += "</label>\n";
  return html;
}

function filterOrg(org) {
  if (org !== justOrg) {
    justOrg = org;
    reallyLoad();
  }
}

/*
 * BULK ACTION menu-related functions
 */

/**
 * Provide a menu enabling the user to select an action for all the users of a chosen level
 */
function getBulkActionMenu(json) {
  if (justUser || !(json.userPerms && json.userPerms.canModifyUsers)) {
    return "";
  }
  let html =
    "<div class='pager' style='align: right; float: right; margin-left: 4px;'>\n" +
    "<form method='POST'>Set menus:<br />\n";
  html += getBulkActionMenuLevels();
  html += getBulkActionMenuActions();
  html += bulkActionListButton;
  html += bulkActionChangeButtonDiv;
  html += "</form></div>\n";
  return html;
}

function getBulkActionMenuLevels() {
  let html = "<label>all <select name='preset_from'>\n";
  html += "<option>" + LIST_ACTION_NONE + "</option>";
  // Example: <option class='user999' value='999'>999: (LOCKED)</option>
  for (let n in levelList) {
    if (!cldrUserLevels.match(levelList[n].name, cldrUserLevels.ANONYMOUS)) {
      html +=
        "<option class='user" +
        n +
        "' value='" +
        n +
        "'>" +
        levelList[n].string +
        "</option>\n";
    }
  }
  html += "</select></label><br />\n";
  return html;
}

function getBulkActionMenuActions() {
  let html = "<label>to <select name='preset_do'>\n";
  for (const [action, text] of Object.entries(bulkActions)) {
    html += "<option value='" + action + "'>" + text + "</option>\n";
  }
  html += "</select></label><br />\n";
  return html;
}

function submitBulkAction(event) {
  event.preventDefault();
  const data = new FormData(event.target.parentNode);
  const userlevel = data.get("preset_from");
  const action = data.get("preset_do");
  if (
    (userlevel || userlevel === "0") &&
    userlevel !== LIST_ACTION_NONE &&
    action &&
    action !== LIST_ACTION_NONE
  ) {
    bulkSelectAction(userlevel, action);
    document.getElementById("changeButton").style.display = "block";
  }
}

function bulkSelectAction(userlevel, action) {
  for (let i in shownUsers) {
    const user = shownUsers[i];
    if (user.userlevel.toString() === userlevel) {
      const selects = document.getElementsByName(user.id + "_" + user.email);
      if (selects) {
        selectOneUserAction(selects[0], action);
      }
    }
  }
}

function selectOneUserAction(sel, action) {
  for (let i = 0; i < sel.children.length; i++) {
    const c = sel.children[i];
    for (let j = 0; j < c.attributes.length; j++) {
      const a = c.attributes[j];
      if (a.name === "value" && a.value === action) {
        c.selected = "selected";
        return;
      }
    }
  }
}

/*
 * Email-related functions
 */

function getEmailNotification(json) {
  if (json.emailSendingMessage) {
    return "<h1>sending mail to users...</h4>\n";
  } else if (json.emailMismatchWarning) {
    return (
      "<h1 class='ferrbox'>" +
      cldrStatus.stopIcon() +
      " not sending mail - you did not confirm the email address. See form at bottom of page.</h1>\n"
    );
  } else {
    return "";
  }
}

function getEmailControls(json) {
  let html = "";
  if (json.emailStatus === "start") {
    html +=
      "<label><input type='checkbox' value='y' name='" +
      LIST_MAILUSER +
      "'>Check this box to compose a message to these " +
      json.emailUserCount +
      " users (excluding LOCKED users).</label><br />\n";
  } else if (json.emailStatus === "continue") {
    html += detailedEmailControls(json);
  }
  return html;
}

function detailedEmailControls(json) {
  let html = "<p><div class='pager'>";
  html += "<h4>Mailing " + json.emailUserCount + " users</h4>";
  if (json.emailDidConfirm) {
    if (json.emailSendingDisp) {
      html += "[Not implemented - see DisputePageManager]";
      return;
    } else {
      html += "<b>Mail sent.</b><br>";
    }
  } else {
    html += "<input type='hidden' name='" + LIST_MAILUSER + "' value='y'>";
  }
  html += "From: <b>(depends on recipient organization)</b><br>";
  if (json.emailSendWhat) {
    const message = json.emailSendWhat;
    html +=
      "<div class='odashbox' style='white-space:pre'>" + message + "</div>";
    if (!json.emailDidConfirm) {
      html += emailPleaseConfirm(json);
    }
  } else {
    html +=
      "<textarea NAME='" +
      LIST_MAILUSER_WHAT +
      "' id='body' rows='15' cols='85' style='width:100%'></textarea>";
  }
  html += "</div>\n";
  return html;
}

function emailPleaseConfirm(json) {
  let html =
    "<input type='hidden' name='" +
    LIST_MAILUSER_WHAT +
    "' value='" +
    json.emailSendWhat
      .replaceAll("&", "&amp;")
      .replaceAll("'", "&#39;")
      .replaceAll("\n", "<br>\n") +
    "'>";
  if (json.emailConfirmationMismatch) {
    html +=
      "<strong>" +
      cldrStatus.stopIcon() +
      "That confirmation didn't match. Try again.</strong><br>";
  }
  html +=
    "To confirm sending, type the confirmation code <tt class='codebox'>" +
    LIST_MAILUSER_CONFIRM_CODE +
    "</tt> in this box : <input name='" +
    LIST_MAILUSER_CONFIRM +
    "'>";
  return html;
}

function getListHiderControls(json) {
  if (!json.hideUserList) {
    return "";
  }
  const ch = hideAllUsers ? " checked='checked'" : "";
  return (
    "<input type='checkbox' id='hideAllUsers'" +
    ch +
    " /> <label for='hideAllUsers'>Hide the user list</label><br />\n"
  );
}

function toggleHideAllUsers() {
  hideAllUsers = !hideAllUsers;
  document.getElementById(userListTableId).style.display = hideAllUsers
    ? "none"
    : "block";
}

/*
 * URL-related functions
 */

/**
 * Get the main URL for ajax requests, both GET (e.g., for initial load)
 * and PUT (e.g., for submitTableForm)
 *
 * @return the URL string
 */
function getUrl() {
  // Allow cache (no cacheKill) -- except for development/debugging
  const allowCache = false;
  const p = new URLSearchParams();
  p.append("what", WHAT_USER_LIST);
  p.append(PREF_SHOWLOCKED, showLockedUsers);
  if (justOrg) {
    p.append(PREF_JUSTORG, justOrg);
  }
  if (justUser) {
    p.append(LIST_JUST, justUser);
  }
  p.append("s", cldrStatus.getSessionId());
  if (!allowCache) {
    p.append("cacheKill", cldrSurvey.cacheBuster());
  }
  return cldrAjax.makeUrl(p);
}

function getLoginUrl(email, password) {
  const p = new URLSearchParams();
  p.append("email", email);
  p.append("pw", password);
  // CAUTION: this is /survey not /SurveyAjax -- don't use cldrAjax.makeUrl until this is changed
  return cldrStatus.getContextPath() + "/survey?" + p.toString();
}

function getUserActivityUrl() {
  const p = new URLSearchParams();
  p.append("what", "stats_bydayuserloc");
  return cldrAjax.makeUrl(p);
}

function getInterestLocalesUrl() {
  return cldrAjax.makeApiUrl("intlocs", null);
}

function getInterestLocalesPostData(interestLocales) {
  return {
    sessionString: cldrStatus.getSessionId(),
    email: justUser,
    intlocs: interestLocales,
  };
}

function getXmlUploadLink(u) {
  // TODO: not jsp; see https://unicode-org.atlassian.net/browse/CLDR-14385
  // cf. similar link (without email) in MainMenu.vue
  const sessionId = cldrStatus.getSessionId();
  if (!sessionId) {
    return ""; // could be unit test
  }
  const p = new URLSearchParams();
  p.append("s", sessionId);
  p.append("email", u.data.email);
  const url = "upload.jsp?" + p.toString();
  return "<a href='" + url + "'>Upload XML...</a>";
}

function getUserActivityLink(u) {
  // cf. similar code in MainMenu.vue
  return (
    "<a class='recentActivity' href='v#recent_activity///" +
    u.data.id +
    "'>User Activity</a>"
  );
}

// cf. cldrRecentActivity.getDownloadMyVotesForm
function getDownloadCsvForm(json) {
  if (isJustMe || !json.userPerms || !json.userPerms.canModifyUsers) {
    return "";
  }
  // TODO: not jsp; see https://unicode-org.atlassian.net/browse/CLDR-14475
  return (
    "<hr />\n" +
    "<form method='POST' action='DataExport.jsp'>\n" +
    "  <input type='hidden' name='s' value='" +
    cldrStatus.getSessionId() +
    "' />\n" +
    "  <input type='hidden' name='do' value='list' />\n" +
    "  <input type='submit' class='csvDownload' value='Download .csv (including LOCKED)' />\n" +
    "</form>\n"
  );
}

function setOnClicks() {
  let el = document.getElementById("bulkActionListButton");
  if (el) {
    el.onclick = (event) => submitBulkAction(event);
  }
  el = document.getElementById("showLocked");
  if (el) {
    el.onclick = () => toggleShowLocked();
  }
  el = document.getElementById("hideAllUsers");
  if (el) {
    el.onclick = () => toggleHideAllUsers();
  }
  el = document.getElementById("changeInterestLocales");
  if (el) {
    el.onclick = () => changeInterestLocales(null);
  }
  el = document.getElementById("participatingUsersButton");
  if (el) {
    el.onclick = () => (window.location.href = "#list_emails");
  }
  el = document.getElementById("addUserButton");
  if (el) {
    el.onclick = () => (window.location.href = "#add_user");
  }
  el = document.getElementById("filterOrgSelect");
  if (el) {
    // onchange, not onclick
    el.onchange = (event) => filterOrg(event.target.value);
  }
  setDoActionClicks();
  setZoomOnClicks();
  setMultipleUsersOnClicks();
  setActionMenuOnChange();
}

function setDoActionClicks() {
  const els = document.getElementsByClassName("doActionButton");
  for (let i = 0; i < els.length; i++) {
    els[i].onclick = () => submitTableForm();
  }
}

function setZoomOnClicks() {
  const els = document.getElementsByClassName("zoomUserButton");
  for (let i = 0; i < els.length; i++) {
    const el = els[i];
    el.onclick = () => listSingleUser(el.title);
  }
}

function setMultipleUsersOnClicks() {
  const els = document.getElementsByClassName("listMultipleUsers");
  for (let i = 0; i < els.length; i++) {
    els[i].onclick = () => listMultipleUsers();
  }
}

function setActionMenuOnChange() {
  if (justUser) {
    // submit immediately on change; don't wait for user to press "Do Action" button
    const els = document.getElementsByClassName("userActionMenuSelect");
    // actually there's only one such element, since justUser is true
    for (let i = 0; i < els.length; i++) {
      // onchange, not onclick
      els[i].onchange = () => submitTableForm();
    }
  }
}

function setMockLevels(list) {
  levelList = list;
}

function setMockOrgs(o) {
  orgs = o;
}

export {
  createUser,
  load,
  loadListUsers,
  zoomUser,
  /*
   * The following are meant to be accessible for unit testing only:
   */
  getHtml,
  getTable,
  setMockLevels,
  setMockOrgs,
};

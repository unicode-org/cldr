"use strict";

/**
 * cldrAccount: Survey Tool features for My Account Settings and List Users
 * This is the new non-dojo version. For dojo, see special/users.js.
 *
 * Use an IIFE pattern to create a namespace for the public functions,
 * and to hide everything else, minimizing global scope pollution.
 */
const cldrAccount = (function () {
  const CLDR_ACCOUNT_DEBUG = true;
  const SHOW_GRAVATAR = !CLDR_ACCOUNT_DEBUG;

  const WHAT_USER_LIST = "user_list"; // cf. org.unicode.cldr.web.SurveyAjax.WHAT_USER_LIST
  const LIST_JUST = "justu"; // cf. org.unicode.cldr.web.UserList.LIST_JUST

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
  const GET_ORGS = "get_orgs";

  const cautionSessionDestruction =
    "<div class='fnotebox'>Changing user level or locales while a user is active will " +
    "result in destruction of their session. Check if they have been working recently.</div>\n";

  const listMultipleUsersButton =
    "<button type='button' onclick='cldrAccount.listMultipleUsers()'>⋖ Show all users</button>\n";

  const doActionButton =
    "<input type='submit' name='doBtn' value='Do Action' />\n";

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
    "<button type='button' onclick='cldrAccount.submitBulkAction(event)'>list</button>\n";

  // TODO: should this button be renamed from "Change" to "Do Action", for consistency, given that
  // its effect is the same as the "Do Action" buttons?
  const bulkActionChangeButtonDiv =
    "<div id='changeButton' style='display: none;'>" +
    "<hr /><i><b>Menus have been pre-filled.<br />" +
    "Confirm your choices and click Change.</b></i><br />" +
    "<button type='button' onclick='cldrAccount.submitTableForm(event)'>Change</button>\n" +
    "</div>\n";

  const infoType = {
    // this MUST agree with org.unicode.cldr.web.UserRegistry.InfoType
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
   * The email address of the "zoomed" or "My Acccount" user, or null if neither zoomed nor My Account.
   * The "List Users" table may be "zoomed" to display only a single user. In general,
   * zooming a user in the "List Users" page is not the same as a user's "My Account" view, since
   * one may zoom in on someone else's account. Zooming in on one's own account is essentially the
   * same as choosing My Account, Settings.
   */
  let justUser = null;

  let showLockedUsers = false;
  let orgList = null;
  let shownUsers = null;
  let justOrg = null;
  let byEmail = {};

  let loadListMultipleOnce = false;

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
   * Load the "My Accounts, Settings" page
   * -- called as special.load
   *
   * Also called by way of loadListUsers() to load the "List Users" page,
   * if loadListMultipleOnce has been set to true
   */
  function load() {
    cldrEvent.hideRightPanel();
    cldrInfo.showNothing();
    const me = cldrStatus.getSurveyUser();
    if (!me || !me.email) {
      pleaseLogIn();
    } else if (loadListMultipleOnce) {
      loadListMultipleOnce = false;
      listMultipleUsers();
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
    isJustMe = email === cldrStatus.getSurveyUser().email;
    reallyLoad();
  }

  function reallyLoad() {
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
    if (json.orgList) {
      orgList = json.orgList;
    }
    shownUsers = json.shownUsers;
    ourDiv.innerHTML = getHtml(json);
    cldrSurvey.hideLoader();
    cldrLoad.flipToOtherDiv(ourDiv);
    showUserActivity(json);
  }

  function errorHandler(err) {
    const ourDiv = document.createElement("div");
    ourDiv.innerHTML = err;
    cldrSurvey.hideLoader();
    cldrLoad.flipToOtherDiv(ourDiv);
  }

  function getHtml(json) {
    let html = "";
    html += emailMismatchWarning(json);
    if (!isJustMe) {
      html += getParticipatingUsersLink() + "<br />\n";
      html += getAddUserLink() + "<br />\n";
      if (orgList && !justUser) {
        html += getOrgFilterMenu();
      }
    }
    if (justUser) {
      html += listMultipleUsersButton;
    }
    if (isJustMe) {
      html += "<h2>My Account</h2>\n";
    } else {
      const org = json.org ? json.org : "ALL";
      html += "<h2>Users for " + org + "</h2>\n";
      html += getLockedUsersControl();
      html += cautionSessionDestruction;
      html += getBulkActionMenu(json);
    }
    html += getTable(json);
    html += getDownloadCsvForm(json);
    return html;
  }

  function getTable(json) {
    shownUsers = json.shownUsers;
    byEmail = {};
    let html = getTableStart();
    let oldOrg = "";
    for (let i in shownUsers) {
      const u = {
        data: shownUsers[i],
      };
      byEmail[u.data.email] = u;
      if (oldOrg !== u.data.org) {
        html +=
          "<tr class='heading'><th class='partsection' colspan='6'><a name='" +
          u.data.org +
          "'><h4>" +
          u.data.org +
          "</h4></a></th></tr>\n";
        oldOrg = u.data.org;
      }
      html += getUserTableRow(u, json);
    }
    html += getTableEnd();
    return html;
  }

  function getTableStart() {
    return (
      "<form id='tableForm' method='POST' onsubmit='cldrAccount.submitTableForm(event)'>\n" +
      doActionButton +
      "<table id='userListTable' summary='User List' class='userlist' border='2'>\n" +
      "<thead><tr><th></th><th style='display: none;'>Organization / Level</th><th>Name/Email</th>" +
      "<th>Action</th><th>Locales</th><th>Seen</th></tr></thead>\n" +
      "<tbody>\n"
    );
  }

  function getTableEnd() {
    let html = "</tbody></table>" + "<br />\n";
    if (justUser) {
      html += listMultipleUsersButton;
    } else if (!isJustMe) {
      html += numberOfUsersShown(shownUsers.length);
    }
    html += doActionButton + "</form>\n";
    return html;
  }

  function numberOfUsersShown(number) {
    return (
      "<div style='font-size: 70%'>Number of users shown: " +
      number +
      "</div>\n"
    );
  }

  function submitTableForm(event) {
    event.preventDefault();
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
      getUserLocales(u, json) +
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
      } else {
        for (const [action, text] of Object.entries(u.data.actions)) {
          html += text;
        }
      }
    }
    if (!justUser) {
      html +=
        "<a onclick='cldrAccount.listSingleUser(\"" +
        u.data.email +
        "\")'>" +
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

  function getUserActions(u, json) {
    return (
      getUserActionMenu(u, json) +
      "<br />\n" +
      getXmlUploadLink(u) +
      "<br />\n" +
      getUserActivityLink(u)
    );
  }

  function getUserActionMenu(u, json) {
    const theirTag = u.data.id + "_" + u.data.email;
    let html = "<select name='" + theirTag + "'";
    if (justUser) {
      // submit immediately on change; don't wait for user to press "Do Action" button
      html += " onchange='cldrAccount.submitTableForm(event)'";
    }
    html += ">\n";
    const theirLevel = u.data.userlevel;
    html += "<option value=''>" + LIST_ACTION_NONE + "</option>\n";
    html += getChangeLevelOptions(theirLevel, json.userPerms.levels);
    html += "<option disabled='disabled'>" + LIST_ACTION_NONE + "</option>\n";
    for (const [action, text] of Object.entries(passwordActions)) {
      html += getPasswordOption(theirLevel, json, action, text);
    }
    if (justUser) {
      html += getJustUserActionMenuOptions(u, json);
    }
    html += "</select>";
    return html;
  }

  function getChangeLevelOptions(theirLevel, levels) {
    let html = "";
    for (let number in levels) {
      if (levels[number].name === "anonymous") {
        continue;
      }
      // only allow mass LOCK
      if (justUser || levels[number].name === "locked") {
        html += doChangeUserOption(levels, number, theirLevel);
      }
    }
    return html;
  }

  function doChangeUserOption(levels, newNumber, oldNumber) {
    const s = levels[newNumber].string; // e.g., "999: (LOCKED)"
    if (levels[oldNumber].canCreateOrSetLevelTo) {
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
    html += " <option disabled='disabled'>" + LIST_ACTION_NONE + "</option>\n"; // separator

    const current = 0; // ?? InfoType.fromAction(action); -- json.preset_do?
    for (const [info, title] of Object.entries(infoType)) {
      if (info === "INFO_ORG" && !cldrStatus.getPermissions().userIsAdmin) {
        continue;
      }
      html += " <option";
      if (info === current) {
        html += " selected='selected'";
      }
      // INFO_EMAIL makes CHANGE_INFO_EMAIL, etc.
      html += " value='CHANGE_" + info + "'>Change " + title + "...</option>\n";
    }
    return html;
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

  function getUserLocales(u, json) {
    const UserRegistry_MANAGER = 2; // TODO -- get from json.userPerms.levels? See UserRegistry.MANAGER in java
    const theirLevel = u.data.userlevel;
    if (
      theirLevel <= UserRegistry_MANAGER ||
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
    let html = "<b>" + what + ": " + when + " ago</b>";
    if (what === "seen") {
      html += "<br /><font size='-2'>" + u.data.lastlogin + "</font></td>";
    }
    return html;
  }

  function showUserActivity(json) {
    const shownUsers = json.shownUsers;
    const table = document.getElementById("userListTable");
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
      const chartCount = cldrDom.createChunk(
        theStat.count,
        "span",
        "chartCount"
      );

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

  function emailMismatchWarning(json) {
    if (json.email_mismatch) {
      return (
        "<h1 class='ferrbox'>" +
        cldrStatus.stopIcon() +
        " not sending mail - you did not confirm the email address. See form at bottom of page.</h1>\n"
      );
    } else {
      return "";
    }
  }

  function getLockedUsersControl() {
    const ch = showLockedUsers ? " checked='checked'" : "";
    return (
      "<input type='checkbox' id='showLocked' onclick='cldrAccount.toggleShowLocked();'" +
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
      "<select class='menutop-other' onchange='cldrAccount.filterOrg(this.value);'>\n" +
      "<option value='all'>Show All</option>\n";
    orgList.forEach(function (org) {
      const sel = org === justOrg ? " selected='selected'" : "";
      html += "<option value='" + org + "'" + sel + ">" + org + "</option>\n";
    });
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
    if (justUser || !json.userPerms.canModifyUsers) {
      return "";
    }
    let html =
      "<div class='pager' style='align: right; float: right; margin-left: 4px;'>\n" +
      "<form method='POST'>Set menus:<br />\n";
    html += getBulkActionMenuLevels(json.userPerms.levels);
    html += getBulkActionMenuActions();
    html += bulkActionListButton;
    html += bulkActionChangeButtonDiv;
    html += "</form></div>\n";
    return html;
  }

  function getBulkActionMenuLevels(levels) {
    let html = "<label>all <select name='preset_from'>\n";
    html += "<option>" + LIST_ACTION_NONE + "</option>";
    // Example: <option class='user999' value='999'>999: (LOCKED)</option>
    for (let n in levels) {
      if (levels[n].name !== "anonymous") {
        html +=
          "<option class='user" +
          n +
          "' value='" +
          n +
          "'>" +
          levels[n].string +
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
    if (needOrgList()) {
      p.append(GET_ORGS, true);
    }
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
    return cldrStatus.getContextPath() + "/SurveyAjax?" + p.toString();
  }

  function needOrgList() {
    if (orgList) {
      return false; // already got orgList
    }
    // Only Admin needs orgList
    const perm = cldrStatus.getPermissions();
    return perm && perm.userIsAdmin;
  }

  function getLoginUrl(email, password) {
    const p = new URLSearchParams();
    p.append("email", email);
    p.append("uid", password);
    return cldrStatus.getContextPath() + "/survey?" + p.toString();
  }

  function getUserActivityUrl() {
    const p = new URLSearchParams();
    p.append("what", "stats_bydayuserloc");
    return cldrStatus.getContextPath() + "/SurveyAjax?" + p.toString();
  }

  function getParticipatingUsersLink() {
    // TODO: "Users Who Participated": see https://unicode-org.atlassian.net/browse/CLDR-14432
    return "<a class='notselected' href='v#tc-emaillist'>[TODO:] Email Address of Users Who Participated</a>";
  }

  function getAddUserLink() {
    // TODO: "Add User"; not jsp: see https://unicode-org.atlassian.net/browse/CLDR-14433
    return "<a href='/cldr-apps/adduser.jsp'>[TODO:] Add User</a>";
  }

  function getXmlUploadLink(u) {
    return (
      // TODO: not jsp
      // /cldr-apps/upload.jsp?s=" + cldrStatus.getSessionId() + "&email=" + u.data.email
      "<a href='?'>[TODO:] Upload XML...</a>"
    );
  }

  function getUserActivityLink(u) {
    return (
      // TODO: not jsp
      // "<a class='recentActivity' href='/cldr-apps/myvotes.jsp?user=" +
      // u.data.id +
      // "'>User Activity</a>"
      "<a class='recentActivity' href='?'>[TODO:] User Activity</a>"
    );
  }

  function getDownloadCsvForm(json) {
    if (!json.userPerms.canModifyUsers) {
      return "";
    }
    // TODO: not jsp; also, DataExport.jsp is broken, see https://unicode-org.atlassian.net/browse/CLDR-14475
    return (
      "<hr /><form method='POST' action='.../DataExport.jsp'>\n" +
      "<input type='submit' class='csvDownload' value='Download .csv (including LOCKED)' />\n" +
      "</form>"
    );
  }

  /*
   * Make only these functions accessible from other files
   */
  return {
    createUser,
    filterOrg,
    getTable,
    listSingleUser,
    load,
    loadListUsers,
    listMultipleUsers,
    showUserActivity,
    submitBulkAction,
    submitTableForm,
    toggleShowLocked,
    /*
     * The following are meant to be accessible for unit testing only:
     */
    test: {
      getHtml,
    },
  };
})();

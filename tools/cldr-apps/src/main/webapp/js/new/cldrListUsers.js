"use strict";

/**
 * cldrListUsers: Survey Tool feature for listing users
 * This is the new non-dojo version. For dojo, see special/users.js.
 *
 * Use an IIFE pattern to create a namespace for the public functions,
 * and to hide everything else, minimizing global scope pollution.
 */
const cldrListUsers = (function () {
  const SHOW_GRAVATAR = false; // for debugging; TODO: restore to true for production

  const WHAT_USER_LIST = "user_list"; // cf. org.unicode.cldr.web.SurveyAjax.WHAT_USER_LIST
  const LIST_JUST = "justu"; // public cldrListUsers.LIST_JUST; cf. org.unicode.cldr.web.UserList.LIST_JUST

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

  let showLockedUsers = false;
  let orgList = null;
  let justOrg = null;
  let justUser = null;
  let byEmail = {};

  // called as special.load
  function load() {
    cldrEvent.hideRightPanel();
    cldrInfo.showNothing();
    const xhrArgs = {
      url: getUrl(),
      handleAs: "json",
      load: loadHandler,
      error: errorHandler,
    };
    cldrAjax.sendXhr(xhrArgs);
  }

  function getUrl() {
    // Allow cache (no cacheKill) -- except for development/debugging
    const allowCache = false;
    const perm = cldrStatus.getPermissions();
    const getOrgs = perm && perm.userIsAdmin && !orgList;

    const p = new URLSearchParams();
    p.append("what", WHAT_USER_LIST);
    p.append(GET_ORGS, getOrgs);
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

  function loadHandler(json) {
    const ourDiv = document.createElement("div");
    if (json.orgList) {
      orgList = json.orgList;
    }
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
    const org = json.org ? json.org : "ALL";
    let html = "";
    html += emailMismatchWarning(json);
    html +=
      getParticipatingUsersLink() + "<br />\n" + getAddUserLink() + "<br />\n";
    if (orgList && !justUser) {
      html += getOrgFilterMenu();
    }
    html += "<h2>Users for " + org + "</h2>\n";
    html += getLockedUsersControl();
    html += cautionSessionDestruction;
    html += getPager(json);
    html += getTable(json, cldrListUsers);
    if (justUser) {
      html +=
        "<button type='button' onclick='cldrListUsers.showAll()'>Show all users</button>\n";
    } else {
      html +=
        "<div style='font-size: 70%'>Number of users shown: " +
        json.shownUsers.length +
        "</div>\n";
    }
    return html;
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

  function showAll() {
    justUser = null;
    load();
  }

  function getParticipatingUsersLink() {
    // TODO: "Users Who Participated": see https://unicode-org.atlassian.net/browse/CLDR-14432
    return "<a class='notselected' href='v#tc-emaillist'>[TODO:] Email Address of Users Who Participated</a>";
  }

  function getAddUserLink() {
    // TODO: "Add User": see https://unicode-org.atlassian.net/browse/CLDR-14433
    return "<a href='/cldr-apps/adduser.jsp'>[TODO:] Add User</a>";
  }

  function getLockedUsersControl() {
    const ch = showLockedUsers ? " checked='checked'" : "";
    return (
      "<input type='checkbox' id='showLocked' onclick='cldrListUsers.toggleShowLocked();'" +
      ch +
      "> <label for='showLocked'>Show locked users</label><br />\n"
    );
  }

  function toggleShowLocked() {
    showLockedUsers = !showLockedUsers;
    load();
  }

  function getOrgFilterMenu() {
    if (!justOrg) {
      justOrg = "all";
    }
    let html =
      "<label class='menutop-active'>Filter Organization " +
      "<select class='menutop-other' onchange='cldrListUsers.filterOrg(this.value);'>\n" +
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
      load();
    }
  }

  function getPager(json) {
    let html =
      "<div class='pager' style='align: right; float: right; margin-left: 4px;'>\n";
    html +=
      "<form method='POST' action='/cldr-apps/survey????!!!'>" + // TODO: action
      "Set menus:<br><label>all <select name='preset_from'>\n";
    html += "<option>-</option>";
    // Example: <option class='user999' value='999'>999: (LOCKED)</option>
    const levels = json.userPerms.levels;
    for (let number in levels) {
      const string = levels[number].string;
      html +=
        "<option class='user" +
        number +
        "' value='" +
        number +
        "'>" +
        string +
        "</option>\n";
    }
    html += "</select></label> </br>\n";
    html += "<label>to";
    html +=
      "<select name='preset_do'>\n" +
      "<option>-</option>\n" +
      "<option value='showpassword_'>Show password URL...</option>\n" +
      "<option value='sendpassword_'>Resend password...</option>\n" +
      "</select></label> <br>\n" +
      "<input type='submit' name='do' value='list'></form>\n" +
      "</div>\n";
    return html;
  }

  function getTable(json, special) {
    byEmail = {};
    let html = getTableStart();
    let oldOrg = "";
    for (let k in json.shownUsers) {
      const u = {
        data: json.shownUsers[k],
      };
      byEmail[u.data.email] = u;
      if (oldOrg !== u.data.org) {
        html +=
          "<tr class='heading'><th class='partsection' colspan='6'><a name='" +
          u.data.org +
          "'><h4>" +
          u.data.org +
          "</h4>";
        oldOrg = u.data.org;
      }
      html += getUserHtml(u, json, special);
    }
    html += getTableEnd();
    return html;
  }

  const doActionButton =
    "<input type='submit' name='doBtn' value='Do Action'>\n";

  function getTableStart() {
    return (
      "<form id='tableForm' method='POST' onsubmit='cldrListUsers.submitForm(event)'>\n" +
      doActionButton +
      "<table id='userListTable' summary='User List' class='userlist' border='2'>\n" +
      "<thead><tr><th></th><th style='display: none;'>Organization / Level</th><th>Name/Email</th>" +
      "<th>Action</th><th>Locales</th><th>Seen</th></tr></thead>\n" +
      "<tbody>\n"
    );
  }

  function submitForm(event) {
    event.preventDefault();
    const id = document.getElementById("tableForm");
    if (!id) {
      return;
    }
    const data = new FormData(id);

    // Even though this is for a POST request, include a query string for the "?what=...&s=..." part,
    // as with similar GET requests
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
    const url = cldrStatus.getContextPath() + "/SurveyAjax?" + p.toString();
    const xhrArgs = {
      url: url,
      postData: data,
      handleAs: "json",
      load: loadHandler,
      error: errorHandler,
    };
    cldrAjax.sendXhr(xhrArgs);
  }

  function getTableEnd() {
    return "</tbody></table>" + "<br />\n" + doActionButton + "</form>\n";
  }

  function getUserHtml(u, json, special) {
    return (
      "<tr id='u@" +
      u.data.id +
      "'>\n" +
      // 1st column (no header): "zoom" icon, or empty for justme
      "<td>" +
      getFirstCol(u, json, special) +
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

  function getFirstCol(u, json, special) {
    if (special === cldrAccount || justUser) {
      return "";
    }
    const zoomImage =
      "<img alt='[zoom]' style='width: 16px; height: 16px; border: 0;' src='/cldr-apps/zoom.png' title='More on this user...'>";

    let html = "";
    if (u.data.actions && u.data.actions.LIST_ACTION_SHOW_PASSWORD) {
      html += getPasswordLink(
        u.data.email,
        u.data.actions.LIST_ACTION_SHOW_PASSWORD
      );
    }
    html +=
      "<a onclick='cldrListUsers.zoomUser(\"" +
      u.data.email +
      "\")'>" +
      zoomImage +
      "</a>";
    return html;
  }

  function getPasswordLink(email, password) {
    return (
      "<a href='" +
      cldrStatus.getContextPath() +
      "/survey?email=" +
      email +
      "&uid=" +
      password +
      "'>Login for " +
      email +
      "</a> <tt class='winner'>" +
      password +
      "</tt> "
    );
  }

  function zoomUser(email) {
    justUser = email;
    load();
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

    let html = "<select name='" + theirTag + "'  ";
    if (justUser) {
      html += " onchange='cldrListUsers.submitForm(event)'";
    }
    html += ">\n";

    html += "  <option value=''>" + LIST_ACTION_NONE + "</option>\n";

    const theirLevel = u.data.userlevel;
    const levels = json.userPerms.levels;
    for (let number in levels) {
       // only allow mass LOCK
      if (justUser || levels[number].name === "locked") {
        html += doChangeUserOption(levels, number, theirLevel, false);
      }
    }
    html += " <option disabled>" + LIST_ACTION_NONE + "</option>\n";
    html += " <option ";
    if (
      json.preset_fromint == theirLevel &&
      json.preset_do.equals(LIST_ACTION_SHOW_PASSWORD)
    ) {
      html += " SELECTED ";
    }
    html +=
      " value='" + LIST_ACTION_SHOW_PASSWORD + "'>Show password...</option>\n";
    html += " <option ";
    if (
      json.preset_fromint == theirLevel &&
      json.preset_do.equals(LIST_ACTION_SEND_PASSWORD)
    ) {
      html += " SELECTED ";
    }
    html +=
      " value='" + LIST_ACTION_SEND_PASSWORD + "'>Send password...</option>\n";

    if (justUser) {
      if (u.data.havePermToChange) {
        html +=
          " <option value='" +
          LIST_ACTION_SETLOCALES +
          "'>Set locales...</option>\n";
      }
      if (u.data.userCanDeleteUser) {
        html += " <option>" + LIST_ACTION_NONE + "</option>\n";
        if (u.data.actions.LIST_ACTION_DELETE0) {
          html +=
            "   <option value='" +
            LIST_ACTION_DELETE1 +
            "' SELECTED>Confirm delete</option>\n";
        } else {
          html += " <option ";
          if (
            json.preset_fromint == theirLevel &&
            json.preset_do.equals(LIST_ACTION_DELETE0)
          ) {
            html += " SELECTED ";
          }
          html +=
            " value='" + LIST_ACTION_DELETE0 + "'>Delete user..</option>\n";
        }
      }
      if (justUser) {
        html += " <option disabled>" + LIST_ACTION_NONE + "</option>\n";
        /*** TODO:
            InfoType current = InfoType.fromAction(action);
            for (InfoType info : InfoType.values()) {
                if (info == InfoType.INFO_ORG && !(ctx.session.user.userlevel == UserRegistry.ADMIN)) {
                    continue;
                }
                html += " <option ";
                if (info == current) {
                  html += " SELECTED ";
                }
                html += " value='" + info.toAction() + "'>Change " + info.toString() + "...</option>\n";
            }
            ***/
      }
    }
    html += "  </select>";
    return html;
  }

  function getXmlUploadLink(u) {
    return (
      // TODO: not jsp
      "<a href='/cldr-apps/upload.jsp?s=" +
      cldrStatus.getSessionId() +
      "&email=" +
      u.data.email +
      "'>Upload XML...</a>"
    );
  }

  function getUserActivityLink(u) {
    return (
      // TODO: not jsp
      "<a class='recentActivity' href='/cldr-apps/myvotes.jsp?user=" +
      u.data.id +
      "'>User Activity</a>"
    );
  }

  function getUserLocales(u, json) {
    const UserRegistry_MANAGER = 2; // TODO -- get from json? See UserRegistry.MANAGER in java
    // const levels = json.userPerms.levels;
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
        console.log(
          "prettyLocaleList: unrecognized loc = [" +
            loc +
            "]; locales = [" +
            locales +
            "]"
        );
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

  function doChangeUserOption(levels, newNumber, oldNumber, selected) {
    const s = levels[newNumber].string; // e.g., "999: (LOCKED)"
    if (levels[oldNumber].canCreateOrSetLevelTo) {
      return (
        "  <option value='" +
        LIST_ACTION_SETLEVEL +
        newNumber +
        "'>Make " +
        s +
        "</option>\n"
      );
    } else {
      return "  <option disabled>Make " + s + "</option>\n";
    }
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
    for (let k in shownUsers) {
      const user = shownUsers[k];
      rowById[user.id] = parseInt(k);
      showOneUserActivity(user, rows);
    }

    const xhrArgs = {
      url: cldrStatus.getContextPath() + "/SurveyAjax?what=stats_bydayuserloc",
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
    for (let k in rows) {
      const userRow = rows[k];
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
    for (let k in stats.data) {
      const row = stats.data[k];
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
    for (let k = 0; k < count; k++) {
      const theStat = userRow.stats[k];
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

  /*** from old specials/users.js:

    const label = $("<label />");
    const showLocked = $("<input />", {
      type: "checkbox",
      checked: true,
    });
    label.text("Hide locked");
    showLocked.prependTo(label);
    label.appendTo(addto);

  let locked = [];
  let byEmail = {}; // email -> u:{}
  let byId = {}; // id -> u:{}

  showLocked.on("change", function () {
    for (let k in locked) {
      if (showLocked.is(":checked")) {
        locked[k].hide();
      } else {
        locked[k].show();
      }
    }
  });

  let lastHead;

  for (let k in data.users) {
    const u = {
      data: data.users[k],
    };
    byEmail[u.data.email] = u;
    byId[u.data.id] = u;
    if (!lastHead || lastHead !== u.data.org) {
      $("<h1>", { text: u.data.org }).appendTo(this);
      lastHead = u.data.org;
    }

    u.div = createUser(u.data);
    u.obj = $(u.div);
    if (u.data.userlevelName === "locked") {
      u.obj.hide();
      locked.push(u.obj);
    }
    $(this).append(u.obj);

    u.infoSpan = $("<span />");
    u.infoSpan.appendTo(u.obj);

    u.infoButton = $("<button />", {
      text: cldrText.get("users_infoVotesButton"),
    });
    u.infoButton.appendTo(u.obj);

    u.infoButton.on(
      "click",
      {
        u: u, // break closure
      },
      function (event) {
        var u = event.data.u;
        var xurl2 =
          cldrStatus.getContextPath() +
          "/SurveyAjax?&s=" +
          cldrStatus.getSessionId() +
          "&what=user_oldvotes&old_user_id=" +
          u.data.id;
        console.log(xurl2);
        $(u.infoSpan).removeClass("ferrbox");
        u.infoSpan.text("loading..");
        $.ajax({
          context: u.infoSpan,
          url: xurl2,
        })
          .done(function (data2) {
            if (
              !data2.user_oldvotes.data ||
              data2.user_oldvotes.data.length == 0
            ) {
              $(u.infoSpan).text("no old votes.");
            } else {
              // Crudely display the data. For now, just simplify slightly to make more legible.
              $(u.infoSpan).text(
                "old votes: " +
                  JSON.stringify(data2.user_oldvotes.data).replace(
                    /[\\\"]/g,
                    ""
                  )
              );
            }
          })
          .fail(function (err) {
            $(u.infoSpan).addClass("ferrbox");
            $(u.infoSpan).text(
              "Error loading users: Status " + JSON.stringify(err.status)
            );
          });
      }
    );

    u.loadOldVotes = $("<button />", {
      text: cldrText.get("users_loadVotesButton"),
    });
    u.loadOldVotes.appendTo(u.obj);

    u.loadOldVotes.on(
      "click",
      {
        u: u, // break closure
      },
      function (event) {
        var u = event.data.u;
        var oldUserEmail = prompt(
          "First, pardon the modality.\nNext, do you want to import votes to '#" +
            u.data.id +
            " " +
            u.data.email +
            "' FROM another user's old votes? Enter their email address below:"
        );
        if (!oldUserEmail) {
          return;
        }

        var oldUser = byEmail[oldUserEmail];

        if (!oldUser) {
          alert(
            "Could not find user " +
              oldUserEmail +
              " - double check the address."
          );
          return;
        }

        var oldLocale = prompt(
          "Enter the locale id to import FROM " +
            oldUser.data.name +
            " <" +
            oldUser.data.email +
            "> #" +
            oldUser.data.id
        );
        if (!oldLocale) {
          alert("Cancelled.");
          return;
        }
        if (!locmap.getLocaleInfo(oldLocale)) {
          alert("Not a valid locale id: " + oldLocale);
          return;
        }

        var newLocale = prompt(
          "Enter the locale id to import TO " + u.data.email,
          oldLocale
        );
        if (!newLocale) {
          alert("Cancelled.");
          return;
        }

        if (!locmap.getLocaleInfo(newLocale)) {
          alert("Not a valid locale id: " + newLocale);
          return;
        }

        if (
          !confirm(
            "Sure? Import FROM " +
              locmap.getLocaleName(oldLocale) +
              " @ " +
              oldUser.data.email +
              " TO " +
              locmap.getLocaleName(newLocale) +
              " @ " +
              u.data.email
          )
        ) {
          return;
        }

        var xurl3 =
          cldrStatus.getContextPath() +
          "/SurveyAjax?&s=" +
          cldrStatus.getSessionId() +
          "&what=user_xferoldvotes&from_user_id=" +
          oldUser.data.id +
          "&from_locale=" +
          oldLocale +
          "&to_user_id=" +
          u.data.id +
          "&to_locale=" +
          newLocale;
        console.log(xurl3);
        $(u.infoSpan).removeClass("ferrbox");
        u.infoSpan.text(
          "TRANSFER FROM " +
            locmap.getLocaleName(oldLocale) +
            " @ " +
            oldUser.data.email +
            " TO " +
            locmap.getLocaleName(newLocale) +
            " @ " +
            u.data.email
        );
        $.ajax({
          context: u.infoSpan,
          url: xurl3,
        })
          .done(function (data3) {
            if (data3.user_xferoldvotes) {
              $(u.infoSpan).text(JSON.stringify(data3.user_xferoldvotes));
            } else if (data3.err) {
              $(u.infoSpan).addClass("ferrbox");
              $(u.infoSpan).text("Error : " + data3.err);
            } else {
              $(u.infoSpan).addClass("ferrbox");
              $(u.infoSpan).text("Error : " + JSON.stringify(data3));
            }
          })
          .fail(function (err) {
            $(u.infoSpan).addClass("ferrbox");
            $(u.infoSpan).text(
              "Error transferring data: Status " +
                JSON.stringify(err.status)
            );
          });
      }
    );
  }
})
***/

  /*
   * Make only these functions accessible from other files
   */
  return {
    LIST_JUST,
    WHAT_USER_LIST,
    createUser,
    filterOrg,
    getTable,
    load,
    showAll,
    showUserActivity,
    submitForm,
    toggleShowLocked,
    zoomUser,
    /*
     * The following are meant to be accessible for unit testing only:
     */
    test: {
      getHtml,
    },
  };
})();

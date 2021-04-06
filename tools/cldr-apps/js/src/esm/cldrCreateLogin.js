/*
 * cldrCreateLogin: encapsulate the "Create and Login" part of the Admin Panel.
 */
import * as cldrAjax from "./cldrAjax.js";
import * as cldrInfo from "./cldrInfo.js";
import * as cldrLoad from "./cldrLoad.js";
import * as cldrStatus from "./cldrStatus.js";
import * as cldrSurvey from "./cldrSurvey.js";

// called as special.load
function load() {
  cldrInfo.showNothing();

  const ourDiv = document.createElement("div");
  ourDiv.setAttribute("id", "createLoginDiv");
  ourDiv.innerHTML = getHtml();
  // caution: ourDiv isn't added to DOM until we call flipToOtherDiv
  cldrSurvey.hideLoader();
  cldrLoad.flipToOtherDiv(ourDiv);
  fetchData();
}

const createLoginNote =
  "<p><i>Note: the organization chosen will be random, unless you change it at the bottom of this page.</i><br/>\n" +
  "<i>This account is for testing purposes and may be deleted without notice!</i></p>\n";

const formHtml =
  "<form id='createLoginForm' class='adduser' action='' method='POST'>" +
  "  <input type='hidden' name='action' value='new_and_login' />\n" +
  "  <input type='hidden' id='dump' name='dump' value='_' />\n" +
  "  <table>\n" +
  "    <tr>\n" +
  "      <th><label for='real'>User Name:</label></th>\n" +
  "      <td><input id='real' name='real' size='40' value='' /></td>\n" +
  "    </tr>\n" +
  "    <tr class='submit'>\n" +
  "      <td colspan='2'><button style='font-size: xx-large; margin-top: 1ex; margin-bottom: 1ex' type='submit'>Login</button></td>\n" +
  "    </tr>\n" +
  "  </table>\n" +
  "  <h3>More Options...</h3>\n" +
  "  <table>\n" +
  "    <tr>\n" +
  "      <th><label for='new_org'>Specific Organization:</label></th>\n" +
  "      <td><select id='choose_org' onchange=''></select></td>\n" +
  "      <td><input id='new_org' name='new_org' value='' /></td>\n" +
  "    </tr>\n" +
  "    <tr>\n" +
  "      <th><label for='new_userlevel'>Userlevel:</label></th>\n" +
  "      <td><select id='new_userlevel' name='new_userlevel'></select></td>\n" +
  "    </tr>\n" +
  "    <tr>\n" +
  "      <th><label for='new_locales'>Languages responsible:</label></th>\n" +
  "      <td><input name='new_locales' value='' /><br/>\n" +
  "        (Space separated. Examples: 'en de fr')</td>\n" +
  "    </tr>\n" +
  "    <tr>\n" +
  "      <th>Login Options</th>\n" +
  "      <td>\n" +
  "        <label for='new_and_login_autoProceed'><input name='new_and_login_autoProceed' type='checkbox' checked='true' />" +
  "          Log me in immediately after creating the account?</label>\n" +
  "        <label for='new_and_login_stayLoggedIn'><input name='new_and_login_stayLoggedIn' type='checkbox' checked='true' />" +
  "          Remember me the next time I login? (cookie)</label>\n" +
  "      </td>\n" +
  "    </tr>\n" +
  "  </table>\n" +
  "</form>\n";

function getHtml() {
  let html = "<h2>Add a Test Survey Tool user</h2>\n";
  html += createLoginNote;
  html += "<hr />\n";
  html += formHtml;
  return html;
}

function fetchData() {
  const ourUrl =
    cldrStatus.getContextPath() +
    "/SurveyAjax?what=admin_panel&do=create_login" +
    "&s=" +
    cldrStatus.getSessionId() +
    cldrSurvey.cacheKill();

  const xhrArgs = {
    url: ourUrl,
    handleAs: "json",
    load: loadHandler,
    error: errorHandler,
  };

  cldrAjax.sendXhr(xhrArgs);
}

function loadHandler(json) {
  if (json.err) {
    const div = document.getElementById("createLoginDiv");
    div.innerHTML = json.err;
    return;
  }
  setupFormActionUrl();
  setupChooseOrg(json);
  setupChooseLevel(json);
  setupUserName(json);
}

function setupFormActionUrl() {
  const id = "createLoginForm";
  const el = document.getElementById(id);
  if (!el) {
    console.log(id + " not found in setupFormActionUrl");
    return;
  }
  const vap = new URLSearchParams(window.location.search).get("vap");
  if (vap) {
    document.getElementById("dump").setAttribute("value", vap);
    el.setAttribute("action", "survey"); // No session id, use 'dump=<testpw>'
  } else {
    // May not work, if not authorized
    el.setAttribute("action", "survey?s=" + cldrStatus.getSessionId());
  }
}

function setupUserName(json) {
  const id = "real";
  const el = document.getElementById(id);
  if (!el) {
    console.log(id + " not found in setupUserName");
    return;
  }
  el.setAttribute("value", json.name);
  el.focus();
}

function setupChooseOrg(json) {
  const id = "choose_org";
  const id2 = "new_org";
  const el = document.getElementById(id);
  if (!el) {
    console.log(id + " not found in setupChooseOrg");
    return;
  }
  el.setAttribute(
    "onchange",
    "document.getElementById('" + id2 + "').value=this.value"
  );
  const el2 = document.getElementById(id2);
  if (!el2) {
    console.log(id2 + " not found in setupChooseOrg");
    return;
  }
  el2.setAttribute("value", json.defaultOrg);

  let html = "<option value='' selected='selected'>Choose...</option>\n";
  for (let i in json.orgs) {
    const org = json.orgs[i];
    const selected = org === json.defaultOrg ? " selected='true'" : "";
    html +=
      "<option value='" + org + "'" + selected + ">" + org + "</option>\n";
  }
  el.innerHTML = html;
}

function setupChooseLevel(json) {
  const id = "new_userlevel";
  const el = document.getElementById(id);
  if (!el) {
    console.log(id + " not found in setupChooseLevel");
    return;
  }
  let html = "<option value='' selected='selected'>Choose...</option>\n";
  for (let number in json.levels) {
    const selected =
      Number(number) === Number(json.defaultLevel) ? " selected='true'" : "";
    html +=
      "<option value='" +
      number +
      "'" +
      selected +
      ">" +
      json.levels[number].string +
      "</option>\n";
  }
  el.innerHTML = html;
}

function errorHandler(err) {
  theDiv.className = "ferrbox";
  theDiv.innerHTML =
    "Error while loading: <div style='border: 1px solid red;'>" +
    err +
    "</div>";
}

export {
  load,
  /*
   * The following are meant to be accessible for unit testing only:
   */
  getHtml,
};

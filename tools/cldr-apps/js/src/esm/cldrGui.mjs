/*
 * cldrGui: encapsulate GUI functions for Survey Tool
 */
import * as cldrAjax from "./cldrAjax.mjs";
import * as cldrDrag from "./cldrDrag.mjs";
import * as cldrEvent from "./cldrEvent.mjs";
import * as cldrForum from "./cldrForum.mjs";
import * as cldrInfo from "./cldrInfo.mjs";
import * as cldrLoad from "./cldrLoad.mjs";
import * as cldrMenu from "./cldrMenu.mjs";
import * as cldrProgress from "./cldrProgress.mjs";
import * as cldrStatus from "./cldrStatus.mjs";
import * as cldrSurvey from "./cldrSurvey.mjs";

import DashboardWidget from "../views/DashboardWidget.vue";
import MainHeader from "../views/MainHeader.vue";

import { createCldrApp } from "../cldrVueRouter.mjs";

import { notification } from "ant-design-vue";

const GUI_DEBUG = true;

const runGuiId = "st-run-gui";

let mainHeaderWrapper = null;
let dashboardWidgetWrapper = null;

let dashboardVisible = false;

/**
 * Set up the DOM and start executing Survey Tool as a single page app
 * This is also called from LoginButton.vue (on login)
 * and WaitingPanel.vue (on successful reload)
 * @returns Promise
 */
function run() {
  try {
    if (GUI_DEBUG) {
      console.log("This is cldrGui.run");
    }
    const guiContainer = document.getElementById(runGuiId);
    if (!guiContainer) {
      if (GUI_DEBUG) {
        console.log(
          "cldrGui.run doing nothing since '" + runGuiId + "' not found"
        );
        return Promise.reject(Error(`${runGuiId} element was not present.`));
      }
      return Promise.resolve();
    }
    while (guiContainer.firstChild) {
      guiContainer.removeChild(guiContainer.firstChild);
    }
    guiContainer.innerHTML = getBodyHtml();
    insertHeader();
    setOnClicks();
    window.addEventListener("resize", handleResize);
    cldrProgress.insertWidget("CompletionSpan");
    cldrInfo.initialize("ItemInfoContainer", "MainContentPane", "open-right");
  } catch (e) {
    return Promise.reject(e);
  }
  return ensureSession().then(completeStartupWithSession);
}

async function ensureSession() {
  if (haveSession()) {
    if (GUI_DEBUG) {
      console.log("cldrGui.ensureSession there is already a session");
    }
    return; // the session was already set
  }
  scheduleLoadingWithSessionId();
  if (GUI_DEBUG) {
    console.log("cldrGui.ensureSession making login request");
  }
  const url = "api/auth/login";
  const init = cldrAjax.makePostData({}); // no email/password
  return await cldrAjax
    .doFetch(url, init)
    .then(cldrAjax.handleFetchErrors)
    .then((r) => r.json())
    .then(loginCallback)
    .catch(loginFailure);
}

function loginCallback(logintoken) {
  if (logintoken.sessionId) {
    // logged in OK.
    cldrStatus.setSessionId(logintoken.sessionId);
    if (GUI_DEBUG) {
      console.log(
        "cldrGui.ensureSession login response.ok; logintoken.sessionId = " +
          logintoken.sessionId
      );
    }
  } else {
    loginFailure();
  }
}

function loginFailure() {
  throw new Error("SurveyTool did not create a session. Try back later.");
}

function haveSession() {
  if (cldrStatus.getSurveyUser() || cldrStatus.getSessionId()) {
    return true;
  }
  return false;
}

/**
 * Arrange for getInitialMenusEtc to be called soon after we've gotten the session id.
 * Add a short timeout to avoid interrupting the code that sets the session id.
 */
function scheduleLoadingWithSessionId() {
  cldrStatus.on("sessionId", () => {
    setTimeout(function () {
      cldrLoad.parseHashAndUpdate(cldrLoad.getHash());
      cldrMenu.getInitialMenusEtc();
    }, 100 /* one tenth of a second */);
  });
}

function completeStartupWithSession() {
  cldrSurvey.updateStatus();
  cldrLoad.showV();
  cldrEvent.startup();
}

/**
 * Create the Main Header Vue component
 *
 * Reference: https://v3.vuejs.org/api/application-api.html#mount
 */
function insertHeader() {
  try {
    const fragment = document.createDocumentFragment();
    mainHeaderWrapper = createCldrApp(MainHeader).mount(fragment);
    const el = document.createElement("header");
    const gui = document.getElementById(runGuiId);
    gui.insertBefore(el, gui.firstChild);
    el.parentNode.replaceChild(fragment, el);
  } catch (e) {
    console.error(
      "Error mounting main header vue " + e.message + " / " + e.name
    );
    notification.error({
      message: `${e.name} while loading MainHeader.vue`,
      description: `${e.message}`,
      duration: 0,
    });
  }
}

function setOnClicks() {
  for (let id of ["chgPagePrevTop", "chgPagePrevBot"]) {
    const el = document.getElementById(id);
    if (el) {
      el.onclick = () => cldrSurvey.chgPage(-1);
    }
  }
  for (let id of ["chgPageNextTop", "chgPageNextBot"]) {
    const el = document.getElementById(id);
    if (el) {
      el.onclick = () => cldrSurvey.chgPage(1);
    }
  }
  const el = document.getElementById("reloadForum");
  if (el) {
    el.onclick = () => cldrForum.reload();
  }
  let els = document.getElementsByClassName("open-dash");
  for (let i = 0; i < els.length; i++) {
    els[i].onclick = () => insertDashboard();
  }
}

const leftSidebar =
  "<div id='left-sidebar'>\n" +
  "  <div id='content-sidebar'>\n" +
  "    <div id='locale-info'>\n" +
  "      <div class='input-group input-group-sm'>\n" +
  "        <span class='input-group-addon  refresh-search'><span class='glyphicon glyphicon-search'></span></span>\n" +
  "        <input type='text' class='form-control local-search' placeholder='Locale' />\n" +
  "      </div>\n" +
  "      <span id='locale-clear' class='refresh-search'>x</span>\n" +
  "      \n" +
  "      <div class='input-group input-group-sm' id='locale-check-group'>\n" +
  "        <label class='checkbox-inline'>\n" +
  "          <input type='checkbox' id='show-read' /> Show read-only\n" +
  "        </label>\n" +
  "        <label class='checkbox-inline'>\n" +
  "          <input type='checkbox' id='show-locked' /> Show locked\n" +
  "        </label>\n" +
  "      </div>\n" +
  "    </div>\n" +
  "    <div id='locale-list'></div>\n" +
  "    <div id='locale-menu'></div>\n" +
  "  </div>\n" +
  "  <div id='dragger'>\n" +
  "    <span class='glyphicon glyphicon-chevron-right'></span>\n" +
  "    <div id='dragger-info'></div>\n" +
  "  </div>\n" +
  "</div>\n";

const st_notices =
  "          <div class='topnotices'>\n" +
  "            <div id='stchanged' style='display:none;' class='stchanged' title='st has changed'>\n" +
  "              <img alt='[warn]' style='width: 16px; height: 16px; border: 0;' src='/cldr-apps/warn.png' title='Locale Changed' />\n" +
  "              The locale, <span id='stchanged_loc'>YOUR LOCALE</span>, has changed due to your or other's actions.\n" +
  "              Please <a href='javascript:window.location.reload(true);'>refresh</a> the page to \n" +
  "              view the new changes..\n" +
  "              <button type='button' onclick='window.location.reload(true)'>Refresh</button>\n" +
  "            </div>\n" +
  "            <div id='betadiv' class='beta' style='display: none'></div>\n" +
  "          </div>\n" /* end of topnotices */ +
  "          <div id='st_err'></div>\n" +
  "          <div class='alert alert-warning alert-dismissable' id='alert-info' style='display:none;'>\n" +
  "            <button type='button' class='close'>&#xD7;</button>\n" +
  "            <div id='progress' class='progress-connecting'>\n" +
  "              <span style='display:none' id='specialHeader'>(Your message here)</span>\n" +
  "              <span id='progress_oneword' class='oneword'>Online</span>\n" +
  "              <span id='progress_ajax' class='ajaxword'>&#xA0;</span>\n" +
  "              <button id='progress-refresh' onclick='window.location.reload(true);'>Refresh</button>\n" +
  "            </div>\n" +
  "          </div>\n";

const topTitle =
  `
  <header id="toptitle" class="beware-left-sidebar">
    <div id="additional-top">
  ` +
  st_notices +
  `
    </div>
    <div id="title-locale-container" class="menu-container">
      <h1><a href="#/aa///" id="title-locale"></a></h1>
      <span id="title-dcontent-container"
        ><a
          href="http://cldr.unicode.org/translation/default-content"
          id="title-content"
        ></a
      ></span>
    </div>
    <div id="title-section-container" class="menu-container">
      <h1 id="section-current"></h1>
      <span style="display:none" id="title-section">
        <span>(section)</span>
        <span id="menu-section"></span>
      </span>
    </div>
    <div id="title-page-container" class="menu-container"></div>
    <nav id="nav-page">
      <span
        ><button id="chgPagePrevTop" class="cldr-nav-btn btn-primary" type="button">← Previous</button>
        <button id="chgPageNextTop" class="cldr-nav-btn btn-primary" type="button">Next →</button></span
      >
      <span class="counter-infos">
        <a id="reloadForum">Forum: <span class="
  ` +
  cldrForum.SUMMARY_CLASS +
  `
        "></span></a>
      </span>
      <span id="CompletionSpan"></span>
      <span>
        <button class="cldr-nav-btn btn-primary open-dash" type="button">Open Dashboard</button>
        <button class="cldr-nav-btn btn-primary open-right" type="button">Open Info Panel</button>
      </span>
    </nav>
  </header>
`;

const sideBySide = `
  <main id="main-row" class="sidebyside beware-left-sidebar">
    <div id="MainContentPane" class="sidebyside-column sidebyside-wide">
      <section id="VotingEtcSection">
        <header class="sidebyside-column-top"></header>
        <section class="sidebyside-scrollable">
          <div id="LoadingMessageSection">Please Wait<img src="loader.gif" alt="Please Wait" /></div>
          <div id="DynamicDataSection"></div>
          <div id="OtherSection"></div>
        </section>
      </section>
      <section id="DashboardSection"></section>
    </div>
    <div id="ItemInfoContainer" class="sidebyside-column sidebyside-narrow">
    </div>
  </main>
`;

const overlay = "<div id='overlay'></div>\n";

/**
 * The postModal div is used only by cldrForum.mjs, for dialog, with bootstrap
 */
const postModal =
  "<div class='modal fade' id='post-modal' tabindex='-1' role='dialog' aria-hidden='true'>\n" +
  "  <div class='modal-dialog modal-lg'>\n" +
  "    <div class='modal-content'>\n" +
  "      <div class='modal-header'>\n" +
  "        <button type='button' class='close' data-dismiss='modal' aria-hidden='true'>&#xD7;</button>\n" +
  "        <h4 class='modal-title'>Post</h4>\n" +
  "      </div>\n" +
  "      <div class='modal-body'></div>\n" +
  "    </div>\n" +
  "  </div>\n" +
  "</div>\n";

/**
 * The hiddenHtml div is used only by cldrTable.mjs, as sort of a template for making rows
 * It shouldn't really be in the DOM at all; obsolescent
 */
const hiddenHtml =
  "<div style='display: none' id='hidden-stuff'>\n" +
  "  <!--  hidden.html - prototypes for some things -->\n" +
  "  <h1>Hidden Stuff</h1>\n" +
  "  <h3>Prototype datarow:</h3>\n" +
  "  <table>\n" +
  "    <tbody>\n" +
  "      <tr id='proto-datarow' class='vother'>\n" +
  "        <td class='d-code codecell'></td>\n" +
  "        <td title='$flyovercomparison' class='d-disp comparisoncell'></td>\n" +
  "        <td title='$flyovernoopinion' class='d-no nocell'></td>\n" +
  "        <td class='d-dr statuscell'></td>\n" +
  "        <td title='$flyoverproposed' class='d-win proposedcell'></td>\n" +
  "        <td title='$flyoveradd' class='d-win addcell'></td>\n" +
  "        <td title='$flyoverother' class='d-win othercell'></td>\n" +
  "      </tr>\n" +
  "      <tr id='proto-parrow' class='d-parrow' >\n" +
  "        <th class='partsection' colspan='7'><a>Partition</a></th>\n" +
  "      </tr>\n" +
  "    </tbody>\n" +
  "  </table>\n" +
  "  <input id='proto-button' class='ichoice' type='radio' title='click to vote' />\n" +
  "  <button id='cancel-button' class='cancelbutton tip btn btn-danger' title='Delete'><span class='glyphicon glyphicon-minus'></span></button>\n" +
  "  <span id='proto-item'></span>\n" +
  "  <div id='proto-sortmode' class='d-sortmode'></div>\n" +
  "  <input id='proto-inputbox' class='inputbox' />\n" +
  "  <div id='proto-loading' class='loading' style='display:none;'>\n" +
  "    <p>Your message here.</p>\n" +
  "    <hr/>\n" +
  "    <span style='background-color: white; float: right;' class='loadbot'>(Stuck? <a href='javascript:window.location.reload(true);'>Refresh</a> the page.)</span>\n" +
  "  </div>\n" +
  "  <table>\n" +
  "    <thead id='voteInfoHead'>\n" +
  "      <tr>\n" +
  "        <th title='$flyovervorg' id='stui-htmlvorg'></th>\n" +
  "        <th title='$flyovervorgvote' id='stui-htmlvorgvote'></th>\n" +
  "       <th title='$flyovervdissenting' id='stui-htmlvdissenting'></th>\n" +
  "     </tr>\n" +
  "    </thead>\n" +
  "  </table>\n" +
  "  <table id='proto-datatable' class='data table table-bordered'>\n" +
  "    <thead>\n" +
  "      <tr class='headingb active'>\n" +
  /*  see CldrText.mjs for the following */
  "        <th title='$flyovercode' id='stui-htmlcode'></th>\n" +
  "        <th title='$flyovercomparison' id='stui-htmltranshint'></th>\n" +
  "        <th title='$flyovernoopinion' id='stui-htmlnoopinion' class='d-no'></th>\n" +
  "        <th title='$flyoverdraft' id='stui-htmldraft' class='d-status'></th>\n" +
  "        <th title='$flyoverproposed' id='stui-htmlproposed'></th>\n" +
  "        <th title='$flyoveradd' id='stui-htmltoadd'></th>\n" +
  "        <th title='$flyoverothers' id='stui-htmlothers'></th>\n" +
  "      </tr>\n" +
  "    </thead>\n" +
  "    <tbody>\n" +
  "    </tbody>\n" +
  "  </table>\n" +
  "  <div id='proto-datafix' class='data'>\n" +
  "    <div class='data-vertical'>\n" +
  "      <div class='d-disp comparisoncell'></div>\n" +
  "      <hr/>\n" +
  "      <div class='d-win proposedcell'></div>\n" +
  "      <div class='d-dr statuscell'></div>\n" +
  "      <hr/>\n" +
  "      <div class='d-win othercell'></div>\n" +
  "      <hr/>\n" +
  "      <div class='d-no nocell'></div>\n" +
  "    </div>\n" +
  "    <div class='data-vote'></div>\n" +
  "    <button type='button' class='close'>&#xD7;</button>\n" +
  "  </div>\n" +
  "  <div id='proto-datarowfix' class='vother'>\n" +
  "    <div class='d-disp comparisoncell'></div>\n" +
  "    <hr/>\n" +
  "    <div class='d-win proposedcell'></div>\n" +
  "    <div class='d-dr statuscell'></div>\n" +
  "    <hr/>\n" +
  "    <div class='d-win othercell'></div>\n" +
  "    <hr/>\n" +
  "    <div class='d-no nocell'></div>\n" +
  "  </div>\n" +
  "</div>\n";

function getBodyHtml() {
  return leftSidebar + topTitle + sideBySide + overlay + postModal + hiddenHtml;
}

function handleResize() {
  cldrEvent.resizeSidebar();
  // Could call updateWithStatus() here to make MainHeader or other components
  // revise their content/dimensions depending on window width/height
}

function updateWithStatus() {
  cldrProgress.fetchLocaleData(true); // skip if already loaded
  mainHeaderWrapper?.updateData();
}

/**
 * The user's coverage level has changed. Inform all widgets we know about that need
 * updating to reflect the change.
 *
 * @param {String} newLevel the new coverage level, such as "modern" or "comprehensive"
 *
 * This should be implemented in a more general way, maybe with listeners, as we
 * add more widgets/components that depend on coverage level.
 */
function updateWidgetsWithCoverage(newLevel) {
  if (dashboardVisible) {
    dashboardWidgetWrapper?.handleCoverageChanged(newLevel);
  }
  cldrProgress.updateWidgetsWithCoverage();
}

/**
 * Create or reopen the DashboardWidget Vue component
 */
function insertDashboard() {
  if (dashboardVisible) {
    return; // already inserted and visible
  }
  try {
    if (dashboardWidgetWrapper) {
      // already created/inserted but invisible
      dashboardWidgetWrapper.reopen();
    } else {
      const fragment = document.createDocumentFragment();
      dashboardWidgetWrapper = createCldrApp(DashboardWidget).mount(fragment);
      const d = document.getElementById("DashboardSection");
      d.replaceWith(fragment);
    }
    showDashboard();
  } catch (e) {
    console.error("Error mounting dashboard vue " + e.message + " / " + e.name);
  }
}

/**
 * Show the dashboard
 */
function showDashboard() {
  if (dashboardVisible) {
    return;
  }
  const vote = document.getElementById("VotingEtcSection");
  const dash = document.getElementById("DashboardSection");
  if (vote && dash) {
    vote.style.height = "50%";
    dash.style.height = "50%";
    dash.style.display = "flex";
    let els = document.getElementsByClassName("open-dash");
    for (let i = 0; i < els.length; i++) {
      els[i].style.display = "none";
    }
    dashboardVisible = true;
    cldrDrag.enable(vote, dash, true /* up/down */);
  }
}

/**
 * Hide the dashboard
 */
function hideDashboard() {
  if (!dashboardVisible) {
    return;
  }
  const vote = document.getElementById("VotingEtcSection");
  const dash = document.getElementById("DashboardSection");
  if (vote && dash) {
    vote.style.height = "100%";
    dash.style.display = "none";
    let els = document.getElementsByClassName("open-dash");
    for (let i = 0; i < els.length; i++) {
      els[i].style.display = "inline";
    }
    dashboardVisible = false;
  }
}

function dashboardIsVisible() {
  return dashboardVisible; // boolean
}

function updateDashboardRow(json) {
  if (dashboardVisible) {
    dashboardWidgetWrapper?.updatePath(json);
  }
}

function setToptitleVisibility(visible) {
  const topTitle = document.getElementById("toptitle");
  if (topTitle) {
    topTitle.style.display = visible ? "block" : "none";
  } else {
    console.log("setToptitleVisibility: topTitle not found!");
  }
}

/**
 * Update the Forum counter on top of the vetting page
 */
function refreshCounterVetting() {
  if (cldrStatus.isVisitor()) {
    // if the user is a visitor, don't display the counter information
    $("#nav-page .counter-infos").hide();
    return;
  }
  cldrForum.refreshSummary();
}

export {
  dashboardIsVisible,
  hideDashboard,
  insertDashboard,
  refreshCounterVetting,
  run,
  setToptitleVisibility,
  showDashboard,
  updateDashboardRow,
  updateWidgetsWithCoverage,
  updateWithStatus,
  /*
   * The following are meant to be accessible for unit testing only:
   */
  getBodyHtml,
};

/*
 * cldrGui: encapsulate GUI functions for Survey Tool
 */
import * as cldrEvent from "./cldrEvent.js";
import * as cldrForum from "./cldrForum.js";
import * as cldrLoad from "./cldrLoad.js";
import * as cldrStatus from "./cldrStatus.js";
import * as cldrSurvey from "./cldrSurvey.js";

const GUI_DEBUG = true;

/**
 * Set up the DOM and start executing Survey Tool as a single page app
 */
function run() {
  if (GUI_DEBUG) {
    console.log("Hello my name is cldrGui.run");
    debugParse();
  }

  const guiContainer = document.getElementById("st-run-gui");
  if (!guiContainer) {
    if (GUI_DEBUG) {
      console.log("run doing nothing since 'st-run-gui' not found");
    }
    return;
  }

  guiContainer.innerHTML = getBodyHtml();

  if (GUI_DEBUG) {
    debugElements();
  }
  setOnClicks();

  cldrSurvey.updateStatus();

  cldrLoad.showV();

  cldrEvent.startup();
}

function setOnClicks() {
  for (let id of ["chgPagePrevTop", "chgPagePrevBot"]) {
    document.getElementById(id).onclick = () => cldrSurvey.chgPage(-1);
  }
  for (let id of ["chgPageNextTop", "chgPageNextBot"]) {
    document.getElementById(id).onclick = () => cldrSurvey.chgPage(1);
  }
  document.getElementById("reloadForum").onclick = () => cldrForum.reload();
}

/**
 * The "navigation bar" at the top of the window
 *
 * The implementation depends on bootstrap for "navbar", etc.:
 *
 * Bootstrap v3.1.1 (http://getbootstrap.com)
 * Copyright 2011-2014 Twitter, Inc.
 * (as of 2021-02-06)
 *
 * -- especially for the gear menu ("pull-menu", "nav-pills", ...)
 * we might do better to implement the gear menu (id='manage-list') in a more
 * straightforward way (as its own special page, maybe with hash #gear)
 * without the clunky overlapping gui -- see cldrGear.js
 * -- and/or upgrade to more recent Bootstrap
 */
const navbar =
  "<div class='navbar navbar-fixed-top' role='navigation'>\n" +
  "  <div class='container-fluid'>\n" +
  "    <div class='collapse navbar-collapse'>\n" +
  "      <p id='st-version-phase' class='nav navbar-text'>Survey Tool ...</p>\n" +
  "      <ul class='nav navbar-nav'>\n" +
  "        <li class='pull-menu'>\n" +
  "          <a href='#'><span class='glyphicon glyphicon-cog'></span> <b class='caret'></b></a>\n" +
  "          <ul id='manage-list' class='nav nav-pills nav-stacked' style='display:none'></ul>\n" +
  "        </li>\n" +
  "        <li class='dropdown' id='title-coverage' style='display:none'>\n" +
  "          <a href='#' class='dropdown-toggle' data-toggle='dropdown'>Coverage: <span id='coverage-info'></span></a>\n" +
  "          <ul class='dropdown-menu'></ul>\n" +
  "        </li>\n" +
  "        <li>\n" +
  "          <a href='https://sites.google.com/site/cldr/translation' target='_blank'>Instructions</a>\n" +
  "        </li>\n" +
  "      </ul>\n" +
  "      <p class='navbar-text navbar-right'>\n" +
  "        <span id='flag-info'></span>\n" +
  "        <span id='st-session-message' class='v-status'></span>\n" +
  "        <span id='st-user-name' class='hasTooltip' title='ctx.session.user.email'></span>\n" +
  "        <span class='glyphicon glyphicon-user tip-log' title='ctx.session.user.org'></span>\n" +
  "        <span id='st-vote-count-menu'></span>\n" +
  "        | <a id='st-loginout-link' class='navbar-link'></a>\n" +
  "      </p>\n" +
  "      <p class='navbar-text navbar-right'>" +
  "        <a href='https://www.unicode.org/policies/privacy_policy.html'>This site uses cookies.</a>\n" +
  "      </p>\n" +
  "      <p id='st-special-header' class='specialmessage navbar-text navbar-right'></p>\n" +
  "    </div>\n" +
  "  </div>\n" +
  "</div>\n";

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
  /* start stnotices.jspf */
  "          <div class='topnotices'>\n" +
  "            <div class='unofficial' title='Not an official SurveyTool' >\n" +
  "              <img alt='[warn]' style='width: 16px; height: 16px; border: 0;' src='/cldr-apps/warn.png' title='Unofficial Site' />Unofficial\n" +
  "            </div>\n" +
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
/* end stnotices.jspf */

const mainContainer =
  /* Caution: "-container" is added to various strings by menubuttons.set()
       in cldrMenu.js, so be careful not to assume such attributes are unused! */
  "<div class='container-fluid' id='main-container'>\n" +
  "  <div class='row menu-position'>\n" +
  "    <div class='col-md-12'>\n" +
  "      <div id='toptitle'>\n" +
  "        <div id='additional-top'>\n" +
  st_notices +
  "        </div>\n" +
  "        <!-- top info -->\n" +
  "        <div id='title-locale-container' class='menu-container' style='display:none'>\n" +
  "          <h1><a href='#locales///' id='title-locale'></a></h1>\n" +
  "          <span id='title-dcontent-container'><a href='http://cldr.unicode.org/translation/default-content' id='title-content'></a></span>\n" +
  "        </div>\n" +
  "        <div id='title-section-container' class='menu-container'>\n" +
  "          <h1 id='section-current'></h1>\n" +
  "          <div style='display:none' id='title-section' data-dojo-type='dijit/form/DropDownButton'>\n" +
  "            <span>(section)</span>\n" +
  "            <div id='menu-section' data-dojo-type='dijit/DropDownMenu'></div>\n" +
  "          </div>\n" +
  "        </div> \n" +
  "        <div id='title-page-container' class='menu-container'></div>\n" +
  "        <div class='row' id='nav-page'>\n" +
  "          <div class='col-md-9'>\n" +
  "            <p class='nav-button'>\n" +
  "              <button id='chgPagePrevTop' type='button' class='btn btn-primary btn-xs'><span class='glyphicon glyphicon-arrow-left'></span> Previous</button>\n" +
  "              <button id='chgPageNextTop' type='button' class='btn btn-primary btn-xs'>Next <span class='glyphicon glyphicon-arrow-right'></span></button>\n" +
  "              <button type='button' class='btn btn-default btn-xs toggle-right'>Toggle Sidebar <span class='glyphicon glyphicon-align-right'></span></button>\n" +
  "            </p>\n" +
  "            <div class='progress nav-progress'>\n" +
  "              <div id='progress-voted' class='progress-bar progress-bar-info tip-log' title='Votes' style='width: 0%'></div>\n" +
  "              <div id='progress-abstain' class='progress-bar progress-bar-warning tip-log' title='Abstain' style='width: 0%'></div>\n" +
  "            </div>\n" +
  "            <div class='counter-infos'><a id='reloadForum'>Forum:</a>\n" +
  "              <span id='vForum'>...</span> ●\n" +
  "              Votes: <span id='count-voted'></span>\n" +
  "              - Abstain: <span id='count-abstain'></span>\n" +
  "              - Total: <span id='count-total'></span>\n" +
  "            </div>\n" +
  "          </div>\n" +
  "        </div>\n" +
  "      </div>\n" +
  "    </div>\n" +
  "  </div>\n" +
  "  <div class='row' id='main-row' style='padding-top:147px;'>\n" +
  "    <div class='col-md-9'>\n" +
  "      <div data-dojo-type='dijit/layout/ContentPane' id='MainContentPane' data-dojo-props=\"splitter:true, region:'center'\" >\n" +
  "        <div id='LoadingMessageSection'>Please Wait<img src='loader.gif' alt='Please Wait' /></div>\n" +
  "        <div id='DynamicDataSection'></div>\n" +
  "        <div id='nav-page-footer'>\n" +
  "          <p class='nav-button'>\n" +
  "            <button id='chgPagePrevBot' type='button' class='btn btn-primary btn-xs'><span class='glyphicon glyphicon-arrow-left'></span> Previous</button>\n" +
  "            <button id='chgPageNextBot' type='button' class='btn btn-primary btn-xs'>Next <span class='glyphicon glyphicon-arrow-right'></span></button>\n" +
  "            <button type='button' class='btn btn-default btn-xs toggle-right'>Toggle Sidebar <span class='glyphicon glyphicon-align-right'></span></button>\n" +
  "          </p>\n" +
  "        </div>\n" +
  "        <div id='OtherSection'></div>\n" +
  "      </div>\n" +
  "    </div>\n" +
  "    <div class='col-md-3'>\n" +
  "      <div id='itemInfo' class='right-info' style='overflow-y: auto' data-dojo-type='dijit/layout/ContentPane' data-dojo-props=\"splitter:true, region:'trailing'\" ></div>\n" +
  "    </div>\n" +
  "  </div>\n" +
  "  <div id='ressources' style='display:none'></div>\n" +
  "</div>\n";

const overlay = "<div id='overlay'></div>\n";

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
  /*  see CldrText.js for the following */
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
  return (
    navbar + leftSidebar + mainContainer + overlay + postModal + hiddenHtml
  );
}

function updateWithStatus() {
  displayVersionPhase();
  displaySpecialHeader();
  displaySessionMessage();
  displayUserName();
  displayVoteCountMenu();
  displayLogInOutLink();
}

function displayVersionPhase() {
  const el = document.getElementById("st-version-phase");
  if (el) {
    el.innerHTML =
      "Survey Tool " + cldrStatus.getNewVersion() + " " + cldrStatus.getPhase();
  }
}

function displaySpecialHeader() {
  const el = document.getElementById("st-special-header");
  if (el) {
    el.innerHTML = cldrStatus.getSpecialHeader();
  }
}

function displaySessionMessage() {
  const el = document.getElementById("st-session-message");
  if (el) {
    el.innerHTML = cldrStatus.getSessionMessage();
  }
}

function displayUserName() {
  const el = document.getElementById("st-user-name");
  if (el) {
    const user = cldrStatus.getSurveyUser();
    if (user && user.name) {
      el.innerHTML = user.name;
    }
  }
}

function displayVoteCountMenu() {
  const el = document.getElementById("st-vote-count-menu");
  if (el) {
    el.innerHTML = makeVoteCountMenu();
  }
}

function makeVoteCountMenu() {
  let html = "";
  const user = cldrStatus.getSurveyUser();
  if (user && user.voteCountMenu) {
    html +=
      "<select id='voteLevelChanged' title='vote with a different number of votes'>\n";
    for (let n of user.voteCountMenu) {
      const selectedOrNot = user.votecount === n ? " selected='selected'" : "";
      html +=
        "<option value='" +
        n +
        "'" +
        selectedOrNot +
        ">" +
        n +
        " votes</option>\n";
    }
    html += "</select>\n";
  }
  return html;
}

function displayLogInOutLink() {
  const el = document.getElementById("st-loginout-link");
  if (el) {
    const path = cldrStatus.getContextPath();
    if (cldrStatus.getSurveyUser()) {
      el.setAttribute("href", path + "/survey?do=logout");
      el.innerHTML =
        "<span class='glyphicon glyphicon-log-out tip-log' title='Logout'></span>";
    } else {
      el.setAttribute("href", path + "/login.jsp");
      el.innerHTML = "Login...";
    }
  }
}

function debugParse() {
  for (let h of [
    navbar,
    leftSidebar,
    mainContainer,
    overlay,
    postModal,
    hiddenHtml,
  ]) {
    if (!parseAsMimeType(h, "text/html")) {
      console.log("🐧 BAD HTML:\n" + h + "\n");
    }
    if (!parseAsMimeType(h, "application/xml")) {
      console.log("🐧 INVALID XML:\n" + h + "\n");
    }
  }
}

// Temporary debugging (cf. parseAsMimeType in TestCldrTest.js):
function parseAsMimeType(inputString, mimeType) {
  const doc = new DOMParser().parseFromString(inputString, mimeType);
  if (!doc) {
    console.log("no doc for " + mimeType + ", " + inputString);
    return null;
  }
  const outputString = new XMLSerializer().serializeToString(doc);
  if (!outputString) {
    console.log("no output string for " + mimeType + ", " + inputString);
    return null;
  }
  if (outputString.indexOf("error") !== -1) {
    console.log(
      "parser error for " + mimeType + ", " + inputString + ", " + outputString
    );
    return null;
  }
  return outputString;
}

function debugElements() {
  for (let id of [
    "DynamicDataSection",
    "nav-page",
    "progress",
    "progress-refresh",
    "st-loginout-link",
    "st-special-header",
    "st-session-message",
    "st-user-name",
    "st-version-phase",
    "st-vote-count-menu",
    "title-section-container",
  ]) {
    if (!document.getElementById(id)) {
      console.log(id + " is MISSING 🐞🐞🐞");
    }
  }
}

export {
  run,
  updateWithStatus,
  /*
   * The following are meant to be accessible for unit testing only:
   */
  getBodyHtml,
};

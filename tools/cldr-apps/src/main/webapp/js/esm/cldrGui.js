/*
 * cldrGui: encapsulate GUI functions for Survey Tool
 */
import * as cldrEvent from "./cldrEvent.js";
import * as cldrForum from "./cldrForum.js";
import * as cldrLoad from "./cldrLoad.js";
import * as cldrSurvey from "./cldrSurvey.js";

import { createApp } from "../../../../../js/node_modules/vue";
import MainHeader from "../../../../../js/src/views/MainHeader.vue";

const GUI_DEBUG = true;

const runGuiId = "st-run-gui";

let mainHeaderWrapper = null;

let rightPanelVisible = true;

/**
 * Set up the DOM and start executing Survey Tool as a single page app
 */
function run() {
  if (GUI_DEBUG) {
    console.log("Hello my name is cldrGui.run");
    debugParse();
  }
  const guiContainer = document.getElementById(runGuiId);
  if (!guiContainer) {
    if (GUI_DEBUG) {
      console.log(
        "cldrGui.run doing nothing since '" + runGuiId + "' not found"
      );
    }
    return;
  }
  guiContainer.innerHTML = getBodyHtml();
  insertHeader();
  if (GUI_DEBUG) {
    debugElements();
  }
  setOnClicks();
  window.addEventListener("resize", handleResize);
  $("body").on("click", ".toggle-right", toggleRightPanel);

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
    mainHeaderWrapper = createApp(MainHeader).mount(fragment);
    const el = document.createElement("header");
    const gui = document.getElementById(runGuiId);
    gui.insertBefore(el, gui.firstChild);
    el.parentNode.replaceChild(fragment, el);
  } catch (e) {
    console.error("Error in vue load " + e.message + " / " + e.name);
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
  <div id="toptitle" class="beware-left-sidebar">
    <div id="additional-top">
  ` +
  st_notices +
  `
    </div>
    <div id="title-locale-container" class="menu-container">
      <h1><a href="#locales///" id="title-locale"></a></h1>
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
    <div id="nav-page">
      <span
        ><button id="chgPagePrevTop" class="btn btn-sm" type="button" onclick="chgPage(-1)">← Previous</button>
        <button id="chgPageNextTop" class="btn btn-sm" type="button" onclick="chgPage(1)">Next →</button></span
      >
      <span>
        <a id="reloadForum">Forum:</a>
        <span id="vForum"><span id="forumSummary"> 0</span> </span> ● Votes:
        <span id="count-voted">0</span> - Abstain:
        <span id="count-abstain">0</span> - Total:
        <span id="count-total">0</span>
        <!-- TODO: progress meter -->
        <meter value="0.7">70%</meter>
        <span class="progress nav-progress" style="display:none">
          <span id="progress-voted" class="progress-bar progress-bar-info tip-log" title="Votes" style="width: 0%"></span>
          <span id="progress-abstain" class="progress-bar progress-bar-warning tip-log" title="Abstain" style="width: 0%"></span>
        </span>
        <button class="toggle-right btn btn-sm" type="button" style="margin-left: 3em">Toggle Sidebar</button>
      </span>
    </div>
  </div>
`;

const sideBySide = `
  <main id="main-row" class="sidebyside beware-left-sidebar">
    <div id="MainContentPane" class="sidebyside-column sidebyside-wide">
      <header class="sidebyside-column-top"></header>
      <section class="sidebyside-scrollable">
        <div id="LoadingMessageSection">Please Wait<img src="loader.gif" alt="Please Wait" /></div>
        <div id="DynamicDataSection"></div>
        <div id="OtherSection"></div>
      </section>
    </div>
    <div id="ItemInfoContainer" class="sidebyside-column sidebyside-narrow">
      <section id="itemInfo" class="sidebyside-scrollable"></section>
    </div>
  </main>
`;

const overlay = "<div id='overlay'></div>\n";

/**
 * The postModal div is used only by cldrForum.js, for dialog, with bootstrap
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
 * The hiddenHtml div is used only by cldrTable.js, as sort of a template for making rows
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
  return leftSidebar + topTitle + sideBySide + overlay + postModal + hiddenHtml;
}

function handleResize() {
  cldrEvent.resizeSidebar();
  // Could call updateWithStatus() here to make MainHeader or other components
  // revise their content/dimensions depending on window width/height
}

function updateWithStatus() {
  if (mainHeaderWrapper) {
    mainHeaderWrapper.updateData();
  }
}

function debugParse() {
  for (let h of [
    leftSidebar,
    topTitle,
    sideBySide,
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
    "title-section-container",
  ]) {
    if (!document.getElementById(id)) {
      console.log(id + " is MISSING 🐞🐞🐞");
    }
  }
}

/**
 * Show or hide the right panel
 */
function toggleRightPanel() {
  rightPanelVisible = !rightPanelVisible;
  if (rightPanelVisible) {
    showRightPanel();
  } else {
    hideRightPanel();
  }
}

/**
 * Show the right panel
 */
function showRightPanel() {
  const main = document.getElementById("MainContentPane");
  const info = document.getElementById("ItemInfoContainer");
  if (main && info) {
    main.style.width = "75%";
    info.style.width = "25%";
    info.style.display = "block";
  }
}

/**
 * Hide the right panel
 *
 * Called by toggleRightPanel, and also by the loadHandler() for isReport() true but isDashboard() false.
 * Otherwise, for the Date/Time, Zones, Numbers reports (especially Zones), the panel may invisibly prevent
 * clicking on the "view" buttons.
 */
function hideRightPanel() {
  const main = document.getElementById("MainContentPane");
  const info = document.getElementById("ItemInfoContainer");
  if (main && info) {
    main.style.width = "100%";
    info.style.display = "none";
  }
}

export {
  hideRightPanel,
  run,
  updateWithStatus,
  /*
   * The following are meant to be accessible for unit testing only:
   */
  getBodyHtml,
};

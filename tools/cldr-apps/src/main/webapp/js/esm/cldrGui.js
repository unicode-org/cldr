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

let mainHeaderWrapper = null;

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
      console.log("cldrGui.run doing nothing since 'st-run-gui' not found");
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
 * Create the Main Header Vue component as the first child of the body element
 *
 * Reference: https://v3.vuejs.org/api/application-api.html#mount
 */
function insertHeader() {
  try {
    const fragment = document.createDocumentFragment();
    mainHeaderWrapper = createApp(MainHeader).mount(fragment);
    const el = document.createElement("header");
    const gui = document.getElementById("st-run-gui");
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
  /* start stnotices.jspf */
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
/* end stnotices.jspf */

const mainContainer =
  /* Caution: "-container" is added to various strings by menubuttons.set()
       in cldrMenu.js, so be careful not to assume such attributes are unused!
       
    col-md-12, col-md-9, and col-md-3 are bootstrap-specific classes
     "md" means "medium", "12/9/3" = number of columns; but they're not really columns.
     col-md-12 means full (12/12) width. col-md-6 means half (6/12) width. col-md-4 means one third (4/12) width.
     "Column classes indicate the number of columns you’d like to use out of the possible 12 per row.
      So, if you want three equal-width columns across, you can use .col-4"
     https://getbootstrap.com/docs/4.1/layout/grid/
    "row" and "container-fluid" are also bootstrap classes
   */
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
  "  <div class='row' id='main-row' style='padding-top:88px;'>\n" +
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

const mainContainerLessBS =
  /* less bootstrap in this version, for testing
   */
  "<div id='main-container'>\n" +
  "  <div>\n" +
  "    <div class='col-md-12'>\n" +
  "      <div id='toptitle'>\n" +
  "        <div id='additional-top'>\n" +
  st_notices +
  "        </div>\n" +
  "        <!-- top info -->\n" +
  "        <span id='title-locale-container' class='menu-container' style='display:none'>\n" +
  "          <h1><a href='#locales///' id='title-locale'></a></h1>\n" +
  "          <span id='title-dcontent-container'><a href='http://cldr.unicode.org/translation/default-content' id='title-content'></a></span>\n" +
  "        </span>\n" +
  "        <span id='title-section-container' class='menu-container'>\n" +
  "          <h1 id='section-current'></h1>\n" +
  "          <span style='display:none' id='title-section'>\n" +
  "            <span>(section)</span>\n" +
  "            <span id='menu-section'></span>\n" +
  "          </span>\n" +
  "        </span> \n" +
  "        <span id='title-page-container' class='menu-container'></span>\n" +
  "        <div id='nav-page'>\n" +
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
  "  <div id='main-row'>\n" +
  "    <div class='col-md-9'>\n" +
  "      <div id='MainContentPane'>\n" +
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
  "      <div id='itemInfo' class='right-info' style='overflow-y: auto'></div>\n" +
  "    </div>\n" +
  "  </div>\n" +
  "  <div id='ressources' style='display:none'></div>\n" +
  "</div>\n";

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
        ><button id='chgPagePrevTop' type="button" onclick="chgPage(-1)">← Previous</button>
        <button id='chgPageNextTop' type="button" onclick="chgPage(1)">Next →</button></span
      >
      <span>
        <a onclick="cldrForum.reload();">Forum:</a>
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
        <button class="toggle-right" type="button">Toggle Sidebar</button>
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

const useMainContainerRadical = true;
const useMainContainerLessBS = false;

function getBodyHtml() {
  if (useMainContainerRadical) {
    return (
      leftSidebar + topTitle + sideBySide + overlay + postModal + hiddenHtml
    );
  } else if (useMainContainerLessBS) {
    return leftSidebar + mainContainerLessBS + overlay + postModal + hiddenHtml;
  }
  return leftSidebar + mainContainer + overlay + postModal + hiddenHtml;
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
    mainContainer,
    mainContainerLessBS,
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

let rightPanelVisible = true;

/**
 * Show or hide the right panel
 */
function toggleRightPanel() {
  if (useMainContainerRadical) {
    rightPanelVisible = !rightPanelVisible;
    if (rightPanelVisible) {
      showRightPanel();
    } else {
      hideRightPanel();
    }
  } else {
    var main = $("#main-row > .col-md-9");
    if (!main.length) {
      showRightPanel();
    } else {
      hideRightPanel();
    }
  }
}

/**
 * Show the right panel
 */
function showRightPanel() {
  if (useMainContainerRadical) {
    const main = document.getElementById("MainContentPane");
    const info = document.getElementById("ItemInfoContainer");
    if (main && info) {
      main.style.width = "75%";
      info.style.width = "25%";
      info.style.display = "block";
    }
  } else {
    $("#main-row > .col-md-12, #nav-page > .col-md-12")
      .addClass("col-md-9")
      .removeClass("col-md-12");
    $("#main-row #itemInfo").show();
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
  if (useMainContainerRadical) {
    const main = document.getElementById("MainContentPane");
    const info = document.getElementById("ItemInfoContainer");
    if (main && info) {
      main.style.width = "100%";
      info.style.display = "none";
    }
  } else {
    $("#main-row > .col-md-9, #nav-page > .col-md-9")
      .addClass("col-md-12")
      .removeClass("col-md-9");
    $("#main-row #itemInfo").hide();
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

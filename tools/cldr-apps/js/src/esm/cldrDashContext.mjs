/*
 * cldrDashContext: encapsulate code that determines how the Dashboard is combined with
 * other Survey Tool GUI components.
 */

import * as cldrDrag from "./cldrDrag.mjs";
import * as cldrLoad from "./cldrLoad.mjs";
import * as cldrNotify from "./cldrNotify.mjs";
import * as cldrStatus from "./cldrStatus.mjs";
import * as cldrVue from "./cldrVue.mjs";

import DashboardWidget from "../views/DashboardWidget.vue";

// Caution: these strings also occur literally in other files (.mjs/.vue/.css)
const OPEN_DASH_BUTTON_CLASS = "open-dash";
const DASH_SECTION_ID = "DashboardSection";
const NEIGHBOR_SECTION_ID = "VotingEtcSection";

/**
 * The application instance for the DashboardWidget component when it has been mounted
 */
let wrapper = null;

/**
 * Is the Dashboard currently displayed?
 */
let dashVisible = false;

/**
 * Does the user want the Dashboard to be displayed when appropriate?
 * It is hidden automatically when there is a "special" view for which
 * it is inappropriate. When returning to the non-special vetting view,
 * the Dashboard should be displayed again, unless the user has indicated, by clicking
 * its close box, that they don't want to see it.
 */
let dashWanted = true;

function isVisible() {
  return dashVisible; // boolean
}

/**
 * Should the Dashboard be shown?
 *
 * Default is true when called the first time.
 * Subsequently, remember whether the user has left it open or closed.
 *
 * @returns true if the Dashboard should be shown, else false
 */
function shouldBeShown() {
  if (!dashWanted) {
    return false;
  }
  const spec = cldrStatus.getCurrentSpecial();
  return !spec || spec === cldrLoad.GENERAL_SPECIAL;
}

function wireUpOpenButtons() {
  const els = document.getElementsByClassName(OPEN_DASH_BUTTON_CLASS);
  for (let i = 0; i < els.length; i++) {
    els[i].onclick = () => insert();
  }
}

/**
 * Create or reopen the DashboardWidget Vue component
 */
function insert() {
  if (dashVisible) {
    return; // already inserted and visible
  }
  try {
    if (wrapper) {
      // already created/inserted but invisible
      wrapper.reopen();
    } else {
      const el = document.getElementById(DASH_SECTION_ID);
      wrapper = cldrVue.mountReplace(DashboardWidget, el);
    }
    show();
  } catch (e) {
    cldrNotify.exception(e, "loading Dashboard");
    console.error("Error mounting dashboard vue " + e.message + " / " + e.name);
  }
}

/**
 * Show the Dashboard
 */
function show() {
  if (dashVisible) {
    return;
  }
  const vote = document.getElementById(NEIGHBOR_SECTION_ID);
  const dash = document.getElementById(DASH_SECTION_ID);
  if (vote && dash) {
    vote.style.height = "50%";
    dash.style.height = "50%";
    dash.style.display = "flex";
    const els = document.getElementsByClassName(OPEN_DASH_BUTTON_CLASS);
    for (let i = 0; i < els.length; i++) {
      els[i].style.display = "none";
    }
    dashVisible = dashWanted = true;
    cldrDrag.enable(vote, dash, true /* up/down */);
  }
}

/**
 * Hide the Dashboard
 *
 * @param {Boolean} userWantsHidden true if closing because user clicked close box (or equivalent),
 *       false if closing because switching to a "special" view where the Dashboard doesn't belong
 */
function hide(userWantsHidden) {
  if (userWantsHidden === undefined) {
    console.error("cldrDashContext.hide was called with undefined parameter");
  }
  if (!dashVisible) {
    return;
  }
  const vote = document.getElementById(NEIGHBOR_SECTION_ID);
  const dash = document.getElementById(DASH_SECTION_ID);
  if (vote && dash) {
    vote.style.height = "100%";
    dash.style.display = "none";
    const els = document.getElementsByClassName(OPEN_DASH_BUTTON_CLASS);
    for (let i = 0; i < els.length; i++) {
      els[i].style.display = "inline";
    }
    dashVisible = false;
    dashWanted = !userWantsHidden;
  }
}

function updateRow(json) {
  if (dashVisible) {
    wrapper?.updatePath(json);
  }
}

function updateWithCoverage(newLevel) {
  if (dashVisible) {
    wrapper?.handleCoverageChanged(newLevel);
  }
}

export {
  hide,
  insert,
  isVisible,
  shouldBeShown,
  updateRow,
  updateWithCoverage,
  wireUpOpenButtons,
};

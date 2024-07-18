/*
 * cldrDashContext: encapsulate code that determines how the Dashboard is combined with
 * other Survey Tool GUI components.
 */

import * as cldrDrag from "./cldrDrag.mjs";
import * as cldrNotify from "./cldrNotify.mjs";
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
 * Is the Info Panel currently displayed?
 */
let visible = false;

function isVisible() {
  return visible; // boolean
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
  if (visible) {
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
  if (visible) {
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
    visible = true;
    cldrDrag.enable(vote, dash, true /* up/down */);
  }
}

/**
 * Hide the Dashboard
 */
function hide() {
  if (!visible) {
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
    visible = false;
  }
}

function updateRow(json) {
  if (visible) {
    wrapper?.updatePath(json);
  }
}

function updateWithCoverage(newLevel) {
  if (visible) {
    wrapper?.handleCoverageChanged(newLevel);
  }
}

export {
  hide,
  insert,
  isVisible,
  updateRow,
  updateWithCoverage,
  wireUpOpenButtons,
};

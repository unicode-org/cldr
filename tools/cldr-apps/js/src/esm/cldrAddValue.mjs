/*
 * cldrAddValue: enable submitting a new value for a path
 */
import * as cldrNotify from "./cldrNotify.mjs";
import * as cldrSurvey from "./cldrSurvey.mjs";
import * as cldrTable from "./cldrTable.mjs";
import * as cldrVote from "./cldrVote.mjs";
import * as cldrVue from "./cldrVue.mjs";

import AddValue from "../views/AddValue.vue";

/**
 * Is the "Add Value" form currently visible?
 */
let formIsVisible = false;

let originalTrClassName = "";

function isFormVisible() {
  return formIsVisible;
}

function setFormIsVisible(visible, xpstrid) {
  formIsVisible = visible;
  const tr = getTrFromXpathStringId(xpstrid);
  if (tr) {
    if (visible) {
      originalTrClassName = tr.className;
    }
    tr.className = visible ? "tr_submit" : originalTrClassName;
  }
}

function addButton(containerEl, xpstrid) {
  try {
    const AddValueWrapper = cldrVue.mount(AddValue, containerEl);
    AddValueWrapper.setXpathStringId(xpstrid);
  } catch (e) {
    console.error(
      "Error loading Add Value Button vue " + e.message + " / " + e.name
    );
    cldrNotify.exception(e, "while loading AddValue");
  }
}

function getEnglish(xpstrid) {
  const theRow = getTheRowFromXpathStringId(xpstrid);
  return theRow?.displayName || "";
}

function getWinning(xpstrid) {
  const theRow = getTheRowFromXpathStringId(xpstrid);
  if (!theRow) {
    return "";
  }
  let theValue = cldrTable.getValidWinningValue(theRow);
  if (theValue === cldrSurvey.INHERITANCE_MARKER || theValue === null) {
    theValue = theRow.inheritedDisplayValue;
  }
  return theValue || "";
}

function sendRequest(xpstrid, newValue) {
  const tr = getTrFromXpathStringId(xpstrid);
  if (!tr) {
    return;
  }
  tr.inputTd = tr.querySelector(".othercell");
  const protoButton = document.getElementById("proto-button");
  cldrVote.handleWiredClick(
    tr,
    tr.theRow,
    "",
    newValue,
    cldrSurvey.cloneAnon(protoButton)
  );
}

function getTheRowFromXpathStringId(xpstrid) {
  const tr = getTrFromXpathStringId(xpstrid);
  return tr?.theRow;
}

function getTrFromXpathStringId(xpstrid) {
  const rowId = cldrTable.makeRowId(xpstrid);
  return document.getElementById(rowId);
}

export {
  addButton,
  getEnglish,
  getWinning,
  isFormVisible,
  sendRequest,
  setFormIsVisible,
};

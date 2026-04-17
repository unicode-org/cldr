/*
 * cldrAddValue: enable submitting a new value for a path
 */
import * as cldrChar from "./cldrChar.mjs";
import * as cldrNotify from "./cldrNotify.mjs";
import * as cldrSurvey from "./cldrSurvey.mjs";
import * as cldrTable from "./cldrTable.mjs";
import * as cldrVote from "./cldrVote.mjs";
import * as cldrVue from "./cldrVue.mjs";
import * as cldrXpathUtils from "./cldrXpathUtils.mjs";

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

function addValueButton(containerEl, xpstrid, xpath, overrideDir) {
  try {
    const dir = overrideDir || cldrSurvey.locInfo()?.dir;
    const addValueWrapper = cldrVue.mount(AddValue, containerEl, {
      dir,
    });
    const pathUsesUnicodeSet = cldrXpathUtils.pathUsesUnicodeSet(xpath);
    addValueWrapper.setXpathStringId(xpstrid, pathUsesUnicodeSet);
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

/**
 * Convert the given tag array to a text string
 *
 * @param {Array} tagArray - the given tag array
 * @returns the text string
 *
 * Example: ["abc", " ", "xyz"] becomes "abc xyz".
 */
function convertTagsToText(tagArray) {
  return tagArray.join("");
}

/**
 * Convert the given text string to a tag array
 *
 * @param {String} text - the given text string
 * @returns the tag array
 *
 * Each special character becomes a tag. Each sequence of other characters is combined into a tag.
 * Example: "abc xyz" becomes three tags: ["abc", " ", "xyz"].
 */
function convertTextToTags(text) {
  const tags = [];
  let combined = "";
  const charArray = cldrChar.split(text);
  for (let c of charArray) {
    if (cldrChar.isSpecial(c)) {
      if (combined.length != 0) {
        tags.push(combined);
        combined = "";
      }
      tags.push(c);
    } else {
      combined += c;
    }
  }
  if (combined.length != 0) {
    tags.push(combined);
  }
  return tags;
}

export {
  addValueButton,
  convertTagsToText,
  convertTextToTags,
  getEnglish,
  getWinning,
  isFormVisible,
  sendRequest,
  setFormIsVisible,
};

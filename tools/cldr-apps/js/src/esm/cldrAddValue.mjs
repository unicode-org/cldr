/*
 * cldrAddValue: enable submitting a new value for a path
 */
import * as cldrChar from "./cldrChar.mjs";
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

function addValueButton(containerEl, xpstrid, overrideDir) {
  try {
    const dir = overrideDir || cldrSurvey.locInfo()?.dir;
    const addValueWrapper = cldrVue.mount(AddValue, containerEl, {
      dir,
    });
    addValueWrapper.setXpathStringId(xpstrid);
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

/**
 * Get the tag index corresponding to the given text index if the given text were converted to tags.
 *
 * @param {String} text - the given text
 * @param {Number} givenTextIndex - the given text index
 * @returns the corresponding tag index
 *
 * Example: "abc xyz" becomes three tags: ["abc", " ", "xyz"].
 * Tag index 0 corresponds to text index 0, 1, and 2.
 * Tag index 1 corresponds to text index 3.
 * Tag index 2 corresponds to text index 4, 5, and 6.
 */
function tagIndexFromTextIndex(text, givenTextIndex) {
  const charArray = cldrChar.split(text);
  let combinedLength = 0;
  let textIndex = 0;
  let tagIndex = 0;
  let wasSpecial = false;
  for (let c of charArray) {
    if (cldrChar.isSpecial(c)) {
      combinedLength = 0;
      if (textIndex) {
        ++tagIndex;
      }
      wasSpecial = true;
    } else {
      ++combinedLength;
      if (wasSpecial) {
        ++tagIndex;
      }
      wasSpecial = false;
    }
    if (++textIndex > givenTextIndex) {
      break;
    }
  }
  return tagIndex;
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
  tagIndexFromTextIndex,
};

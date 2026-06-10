/*
 * cldrAddValue: enable displaying a value (for a CLDR path), or submitting (adding) a new value
 */
import * as cldrChar from "./cldrChar.mjs";
import * as cldrEscaper from "./cldrEscaper.mjs";
import * as cldrNotify from "./cldrNotify.mjs";
import * as cldrSurvey from "./cldrSurvey.mjs";
import * as cldrTable from "./cldrTable.mjs";
import * as cldrVote from "./cldrVote.mjs";
import * as cldrVue from "./cldrVue.mjs";
import * as cldrXpathUtils from "./cldrXpathUtils.mjs";

import AddValue from "../views/AddValue.vue";
import ValueTags from "../views/ValueTags.vue";

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
    cldrSurvey.cloneAnon(protoButton),
    false /* not an abstention */
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
 * Example: ["abc", "–", "xyz"] becomes "abc–xyz".
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
 * Each special character becomes a tag. Each sequence of other characters is combined into a tag (although
 * such a sequence is displayed as ordinary characters, not as a tag).
 * Example: "abc–xyz" becomes three tags: ["abc", "–", "xyz"]. (Here "–" is U+2013 EN DASH.)
 */
function convertTextToTags(text) {
  const tags = [];
  let combined = "";
  const charArray = cldrChar.split(text);
  for (let c of charArray) {
    if (cldrChar.shouldDisplayAsTag(c)) {
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

function addTagsReadyOnly(containerEl, value) {
  try {
    const valueTagWrapper = cldrVue.mount(ValueTags, containerEl);
    valueTagWrapper.setValue(value);
  } catch (e) {
    console.error("Error loading Value Tag vue " + e.message + " / " + e.name);
    cldrNotify.exception(e, "while loading ValueTags");
  }
}

function textForTag(tag) {
  const c = cldrChar.firstChar(tag);
  const shortName = cldrEscaper.getShortName(c);
  if (shortName) {
    return shortName;
  } else {
    return tag;
  }
}
function tagTooltipPlusClick(tag) {
  return tagTooltip(tag) + " Click to choose an alternative";
}

function tagTooltip(tag) {
  const c = cldrChar.firstChar(tag);
  const codePoint = cldrChar.firstCodePoint(tag);
  const usv = cldrChar.uPlus(codePoint);
  const info = cldrEscaper.getCharInfo(c);
  if (info) {
    return (
      info.name + " = " + info.shortName + " " + usv + ": " + info.description
    );
  } else {
    return cldrChar.name(codePoint) + " = " + usv;
  }
}

export {
  addTagsReadyOnly,
  addValueButton,
  convertTagsToText,
  convertTextToTags,
  getEnglish,
  getWinning,
  isFormVisible,
  sendRequest,
  setFormIsVisible,
  tagTooltip,
  tagTooltipPlusClick,
  textForTag,
};

/*
 * cldrBadLocaleIds: Survey Tool feature for reporting/fixing bad locale identifiers, such
 * as in the list of locales for a particular vetter.
 *
 * The precise meaning of "bad locale identifier" is determined by the back end. It might
 * mean one that does not match an existing CLDR locale, or is a default-content locale,
 * or is outside of the organization’s set of locales.
 */
import * as cldrAjax from "./cldrAjax.mjs";
import * as cldrLoad from "./cldrLoad.mjs";
import * as cldrNotify from "./cldrNotify.mjs";
import * as cldrStatus from "./cldrStatus.mjs";
import * as cldrText from "./cldrText.mjs";
import * as cldrXlsx from "./cldrXlsx.mjs";
import * as XLSX from "xlsx";

/**
 * URL used with GET for getting problems, or with POST for fixing
 */
const FIND_BAD_LOCALE_URL = "locales/invalid";
const FIX_BAD_LOCALE_URL = "locales/invalid/fix";
const UNKNOWN_LOCALE_NAME = "?";

const DEBUG = true;

let callbackSetData = null;
let problemCount = 0;
let theData = null;

async function refresh(viewCallbackSetData) {
  if (!hasPermission()) {
    viewCallbackSetData(null);
    return;
  }
  callbackSetData = viewCallbackSetData;
  const url = cldrAjax.makeApiUrl(FIND_BAD_LOCALE_URL);
  try {
    if (DEBUG) {
      console.log("Finding bad locales...");
    }
    const response = await cldrAjax.doFetch(url, null);
    const json = await response.json();
    if (response.ok) {
      setData(json);
      if (DEBUG) {
        console.log("Found " + problemCount + " bad locales.");
      }
    } else {
      throw new Error(json.message || "Unknown server response");
    }
  } catch (e) {
    console.error(e);
    window.alert("Error while getting bad locales:\n" + e);
    handleRefreshError(e);
  }
}

function setData(json) {
  if (callbackSetData) {
    theData = makeDataFromJson(json);
    callbackSetData(theData);
  }
  return json;
}

function makeDataFromJson(json) {
  const data = { problems: json.problems, localeToName: {}, totals: {} };
  for (let problem of json.problems) {
    const id = problem.id;
    if (!data.localeToName[id]) {
      const name = cldrLoad.getLocaleName(id);
      data.localeToName[id] = name === id ? UNKNOWN_LOCALE_NAME : name;
    }
    if (data.totals[problem.rejection]) {
      data.totals[problem.rejection]++;
    } else {
      data.totals[problem.rejection] = 1;
    }
  }
  problemCount = json.problems.length;
  return data;
}

function handleRefreshError(e) {
  cldrNotify.exception(e, "Loading stats about bad locale codes");
}

function hasPermission() {
  return cldrStatus.getPermissions()?.userIsAdmin;
}

async function fixAll() {
  const url = cldrAjax.makeApiUrl(FIX_BAD_LOCALE_URL);
  const patchData = {
    count: problemCount,
  };
  const init = cldrAjax.makePostData(patchData);
  try {
    if (DEBUG) {
      console.log("Fixing bad locales...");
    }
    const response = await cldrAjax.doFetch(url, init);
    const json = await response.json();
    if (response.ok) {
      handleFixSuccess(json);
    } else {
      const message = json.message || "Unknown server response";
      throw new Error(message);
    }
  } catch (e) {
    console.error(e);
    window.alert("Error while fixing bad locales\n" + e);
    handleFixError(e);
  }
}

function handleFixSuccess(json) {
  if (DEBUG) {
    console.log("Fixed bad locales.");
  }
  cldrNotify.open(
    cldrText.get("badLocalesFixSuccessHeader"),
    cldrText.get("badLocalesFixSuccessDetail")
  );
  setData(json);
  if (DEBUG) {
    console.log("Found and fixed " + problemCount + " bad locales.");
  }
  refresh(callbackSetData);
  return json;
}

function handleFixError(e) {
  cldrNotify.exception(e, "Trying to fix bad locale codes");
}

async function saveAsSheet() {
  if (DEBUG) {
    console.log("cldrBadLocaleIds.saveAsSheet");
  }
  const tableHeader = [];
  tableHeader.push(getHeaderRow());

  const tableBody = [];
  for (let problem of theData.problems) {
    tableBody.push(getBodyRow(problem));
  }

  const worksheetData = tableHeader.concat(tableBody);
  const worksheet = XLSX.utils.aoa_to_sheet(worksheetData);
  const workbook = XLSX.utils.book_new();

  const worksheetName = "InvalidLocales";

  XLSX.utils.book_append_sheet(workbook, worksheet, worksheetName);

  XLSX.writeFile(workbook, `${worksheetName}.xlsx`, {
    cellStyles: true,
  });
}

function getHeaderRow() {
  return [
    "Locale ID",
    "Locale Name",
    "Rejection",
    "Users",
    "Proposed Solution",
  ];
}

function getBodyRow(problem) {
  return [
    problem.id,
    theData.localeToName[problem.id],
    problem.rejection,
    problem.userCount,
    problem.solution,
  ];
}

export {
  fixAll,
  getBodyRow,
  getHeaderRow,
  hasPermission,
  refresh,
  saveAsSheet,
};

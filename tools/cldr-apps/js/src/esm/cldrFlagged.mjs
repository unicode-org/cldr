/*
 * cldrFlagged: Survey Tool feature for listing flagged items
 */
import * as cldrAjax from "./cldrAjax.mjs";
import * as cldrStatus from "./cldrStatus.mjs";
import * as cldrXlsx from "./cldrXlsx.mjs";
import * as XLSX from "xlsx";

const FLAGGED_DEBUG = false;

/**
 * If the server response is expected to be very fast, the interface can be simple.
 * Otherwise, if it is expected to take a long time, we show a button to begin the request,
 * show a progress bar, and show time elapsed when done.
 *
 * Flagged response is generally fast, so this can be true. Keep the option to make it false
 * for testing and as boilerplate for features that have slower responses.
 */
const FLAGGED_RESPONSE_IS_FAST = true;

class Status {
  static INIT = "INIT"; // before making a request
  static WAITING = "WAITING"; // waiting for response to first request
  static PROCESSING = "PROCESSING"; // in progress
  static SUCCEEDED = "SUCCEEDED"; // finished successfully
  static STOPPED = "STOPPED"; // stopped due to error or cancellation
}

class RequestType {
  static START = "START"; // start generating
  static CANCEL = "CANCEL"; // cancel (stop) generating
}

const COLUMN_TITLE_LOCALE_NAME = "Locale";
const COLUMN_TITLE_LOCALE_ID = "Code";
const COLUMN_TITLE_POSTED = "Posted";
const COLUMN_TITLE_PATH = "Path";
const COLUMN_TITLE_ORG = "Org.";
const COLUMN_TITLE_USER_ID = "User#";
const COLUMN_TITLE_USER_EMAIL = "Email";

const COLUMNS = [
  { title: COLUMN_TITLE_LOCALE_NAME, comment: "Locale name", default: null },
  { title: COLUMN_TITLE_LOCALE_ID, comment: "Locale code", default: null },
  { title: COLUMN_TITLE_POSTED, comment: "Days ago", default: null },
  {
    title: COLUMN_TITLE_PATH,
    comment: "The path that was flagged",
    default: null,
  },

  { title: COLUMN_TITLE_ORG, comment: "Organization", default: null },
  {
    title: COLUMN_TITLE_USER_ID,
    comment: "User's account number",
    default: null,
  },
  { title: COLUMN_TITLE_USER_EMAIL, comment: "User's email", default: null },
];

let columnCount = COLUMNS.length; // may get shortened

/**
 * Does the user have permission to view flagged-item info?
 *
 * All users have this permission (even not logged in!), although TC and Admin get more info
 */
let canView = true;

/** @function */
let callbackToSetData = null;

/**
 * Data used for constructing the Flagged Items table, derived
 * directly or indirectly from json received from the server
 */
let flaggedData = {};

let tableBody = null;

function viewMounted(setData) {
  callbackToSetData = setData;
  // const perm = cldrStatus.getPermissions();
  // canView = true;
}

function hasPermission() {
  return canView;
}

function start() {
  if (FLAGGED_DEBUG) {
    console.log("cldrFlagged.start");
  }
  const viewData = {
    message: "Waiting for initial data...",
    percent: 0,
    status: Status.WAITING,
  };
  callbackToSetData(viewData);
  makeRequest(RequestType.START);
}

function cancel() {
  if (FLAGGED_DEBUG) {
    console.log("cldrFlagged.cancel");
  }
  flaggedData = {};
  const viewData = { message: "Stopped", percent: 0, status: Status.STOPPED };
  callbackToSetData(viewData);
  makeRequest(RequestType.CANCEL);
}

function makeRequest(req) {
  if (req === RequestType.START) {
    if (FLAGGED_DEBUG) {
      console.log("cldrFlagged.makeRequest, fetching initial data");
    }
    flaggedData.startTime = Date.now();
    const xhrArgs = {
      url: getFlaggedUrl(),
      handleAs: "json",
      load: loadHandler,
      error: errorHandler,
    };
    cldrAjax.sendXhr(xhrArgs);
  } else if (req === RequestType.CANCEL) {
    // Since each of our requests takes only a short time for the server to handle,
    // there is no need to tell the server to cancel a request in progress.
    // Instead, just stop making further requests.
    return;
  }
}

function loadHandler(json) {
  if (json.err) {
    console.dir({ json });
    cldrNotify.error("Error loading Flagged Items data", json.err);
  } else if (callbackToSetData) {
    flaggedData.firstResponseTime = Date.now();
    storeResponseData(json);
  }
}

function errorHandler(err) {
  cldrNotify.exception(err, "Loading Flagged Items data");
}

function storeResponseData(json) {
  // Note: the response json format is no longer very well-suited for how it
  // is used here on the front end. In particular, it includes xpaths in
  // various representations: numeric "12345", unused on the front end;
  // string "//ldml/...", unused on the front end;
  // tab-separated path header, used on the front end but with ">" instead of tabs;
  // hexadecimal "2703e9d07ab2ef3a" used on the front end for links.
  // Also, the json is divided into "flagged" and "details" sections, and the
  // latter only has path strings ("//ldml/..."), so in order to display them
  // as path headers, we need to build a map xpathToHeaderMap using json.flagged.
  // Eventually the back end should be revised to contain only the needed info
  // in a more suitable format.
  const rowMap = {};
  const columnIndex = getIndexOfColumnsByTitle();
  if (json.flagged) {
    getXpathMaps(json.flagged);
    flaggedData.localeIdColumnIndex = columnIndex[COLUMN_TITLE_LOCALE_ID];
    flaggedData.pathColumnIndex = columnIndex[COLUMN_TITLE_PATH];
    // For authorized (TC) members, show detailed table (includes emails).
    // If not authorized, json.details is undefined.
    if (json.details?.data && json.details?.header) {
      storeDetails(rowMap, json.details, columnIndex);
    } else {
      storeBasic(rowMap, json.flagged, columnIndex);
    }
  } else {
    cldrNotify.error(
      "Bad server response",
      "For Flagged Items the response was invalid"
    );
    return;
  }
  tableBody = [];
  Object.entries(rowMap)
    .sort((a, b) => a[0].localeCompare(b[0]))
    .forEach(([sortKey, row]) => {
      tableBody.push(row);
    });
  showResults();
}

function storeBasic(rowMap, flagged, columnIndex) {
  columnCount = 4; // only 4 columns for "basic" (non-TC)
  const header = flagged.header;
  const rows = flagged.data;
  for (let r = 0; r < rows.length; r++) {
    const jsonRow = rows[r];
    const localeName = jsonRow[header.LOCALE_NAME];
    const localeId = jsonRow[header.LOCALE];
    const lastMod = jsonRow[header.LAST_MOD];
    const xpathCode = pathHeaderFromCode(jsonRow[header.XPATH_CODE]);
    const row = getDefaultRow();
    row[columnIndex[COLUMN_TITLE_LOCALE_NAME]] = localeName;
    row[columnIndex[COLUMN_TITLE_LOCALE_ID]] = localeId;
    row[columnIndex[COLUMN_TITLE_POSTED]] = daysAgo(lastMod);
    row[columnIndex[COLUMN_TITLE_PATH]] = xpathCode;
    const sortKey = localeName + " " + when;
    rowMap[sortKey] = [...row]; // clone the array since table will retain a reference
  }
}

function storeDetails(rowMap, details, columnIndex) {
  const colHead = {};
  for (let name in details.header) {
    colHead[details.header[name]] = name;
  }
  for (let jsonRow of details.data) {
    const row = getDefaultRow();
    let localeName = "?";
    let when = "?";
    for (let i in jsonRow) {
      const val = jsonRow[i];
      switch (colHead[i]) {
        case "LOCALE_NAME":
          row[columnIndex[COLUMN_TITLE_LOCALE_NAME]] = val;
          localeName = val;
          break;
        case "LOCALE":
          row[columnIndex[COLUMN_TITLE_LOCALE_ID]] = val;
          break;
        case "LAST_MOD":
          when = daysAgo(val);
          row[columnIndex[COLUMN_TITLE_POSTED]] = when;
          break;
        case "XPATH":
          row[columnIndex[COLUMN_TITLE_PATH]] =
            flaggedData.xpathToHeaderMap[val];
          break;
        case "ORG":
          row[columnIndex[COLUMN_TITLE_ORG]] = val;
          break;
        case "ID":
          row[columnIndex[COLUMN_TITLE_USER_ID]] = val;
          break;
        case "EMAIL":
          row[columnIndex[COLUMN_TITLE_USER_EMAIL]] = val;
          break;
        default:
          console.log("Unexpected column header: " + colHead[i]);
          break;
      }
    }
    const sortKey = localeName + " " + when;
    rowMap[sortKey] = [...row]; // clone the array since table will retain a reference
  }
}

function pathHeaderFromCode(code) {
  // Here "code" is already a path header, but it has tabs where we want " > " separators
  return code.replaceAll("\t", " > ");
}

function getXpathMaps(flagged) {
  const xpathToHeaderMap = {};
  const xpathHeaderToHashMap = {};
  const header = flagged.header;
  const rows = flagged.data;
  for (let r = 0; r < rows.length; r++) {
    const jsonRow = rows[r];
    const xpathHeader = pathHeaderFromCode(jsonRow[header.XPATH_CODE]);
    xpathToHeaderMap[jsonRow[header.XPATH_STRING]] = xpathHeader;
    xpathHeaderToHashMap[xpathHeader] = jsonRow[header.XPATH_STRHASH];
  }
  flaggedData.xpathToHeaderMap = xpathToHeaderMap;
  flaggedData.xpathHeaderToHashMap = xpathHeaderToHashMap;
}

function xpathLinkFromLocaleAndHeader(localeId, pathHeader) {
  const xpstrid = flaggedData.xpathHeaderToHashMap[pathHeader];
  return xpstrid ? "#/" + localeId + "//" + xpstrid : "?";
}

function daysAgo(lastMod) {
  const dateNow = new Date();
  const dateThen = new Date(Number(lastMod));
  return Math.round((dateNow - dateThen) / (1000 * 3600 * 24));
}

function showResults() {
  if (FLAGGED_DEBUG) {
    console.log("showResults, done, 100%");
  }
  const viewData = {
    localeIdColumnIndex: flaggedData.localeIdColumnIndex,
    pathColumnIndex: flaggedData.pathColumnIndex,
    message: getDoneMessage(),
    percent: 100,
    status: Status.SUCCEEDED,
    tableHeader: getHeaderRow(),
    tableComments: getHeaderComments(),
    tableBody: tableBody,
  };
  callbackToSetData(viewData);
}

// Tell the time (in minutes) it took to wait, and the time it took to finish (after the wait ended)
function getDoneMessage() {
  if (FLAGGED_RESPONSE_IS_FAST) {
    return "";
  }
  const finishTime = Date.now();
  const minutesWaiting = Math.floor(
    (flaggedData.firstResponseTime - flaggedData.startTime) / 1000
  );
  const minutesFurther = Math.floor(
    (finishTime - flaggedData.firstResponseTime) / 1000
  );
  const minutesTotal = minutesWaiting + minutesFurther;
  return (
    "Done. Time elapsed = " +
    minutesTotal +
    " minutes (" +
    minutesWaiting +
    " waiting for first response + " +
    minutesFurther +
    " additional)"
  );
}

function getFlaggedUrl() {
  const p = new URLSearchParams();
  p.append("what", "flagged"); // cf. WHAT_FLAGGED in SurveyAjax.java
  p.append("s", cldrStatus.getSessionId());
  return cldrAjax.makeUrl(p);
}

async function saveAsSheet() {
  if (FLAGGED_DEBUG) {
    console.log("cldrFlagged.saveAsSheet");
  }
  const tableHeader = [];
  tableHeader.push(getHeaderRow());
  const worksheetData = tableHeader.concat(tableBody);
  const worksheet = XLSX.utils.aoa_to_sheet(worksheetData);

  addColumnComments(worksheet);

  const workbook = XLSX.utils.book_new();

  XLSX.utils.book_append_sheet(workbook, worksheet, "flagged");

  XLSX.writeFile(workbook, `survey_flagged.xlsx`, {
    cellStyles: true,
  });
}

function getHeaderRow() {
  const row = [];
  let count = 0;
  for (let col of COLUMNS) {
    row.push(col.title);
    if (columnCount && ++count == columnCount) {
      break;
    }
  }
  return row;
}

function getHeaderComments() {
  const row = [];
  let count = 0;
  for (let col of COLUMNS) {
    row.push(col.comment);
    if (columnCount && ++count == columnCount) {
      break;
    }
  }
  return row;
}

function getIndexOfColumnsByTitle() {
  const columnIndex = {};
  let i = 0;
  for (let col of COLUMNS) {
    columnIndex[col.title] = i++;
  }
  return columnIndex;
}

function getDefaultRow() {
  const row = [];
  let count = 0;
  for (let col of COLUMNS) {
    row.push(col.default);
    if (columnCount && ++count == columnCount) {
      break;
    }
  }
  return row;
}

function addColumnComments(worksheet) {
  let columnNumber = 0;
  for (let col of COLUMNS) {
    const cell = XLSX.utils.encode_cell({ r: 0, c: columnNumber }); // A1, B1, C1, ...
    // include title with comment, for convenience if column is narrower than title
    cldrXlsx.pushComment(worksheet, cell, col.title + ": " + col.comment);
    ++columnNumber;
    if (columnCount && columnNumber == columnCount) {
      break;
    }
  }
}

export {
  FLAGGED_RESPONSE_IS_FAST,
  Status,
  cancel,
  hasPermission,
  saveAsSheet,
  start,
  viewMounted,
  xpathLinkFromLocaleAndHeader,
};

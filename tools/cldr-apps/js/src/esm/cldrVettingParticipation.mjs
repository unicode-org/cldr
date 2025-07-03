/*
 * cldrVettingParticipation: encapsulate Survey Tool Vetting Participation code.
 */
import * as cldrAjax from "./cldrAjax.mjs";
import * as cldrLoad from "./cldrLoad.mjs";
import * as cldrNotify from "./cldrNotify.mjs";
import * as cldrOrganizations from "./cldrOrganizations.mjs";
import * as cldrProgress from "./cldrProgress.mjs";
import * as cldrStatus from "./cldrStatus.mjs";
import * as cldrXlsx from "./cldrXlsx.mjs";
import * as XLSX from "xlsx";

const VP_DEBUG = false;

class Status {
  static INIT = "INIT"; // before making a request
  static WAITING = "WAITING"; // waiting for response to first request
  static PROCESSING = "PROCESSING"; // in progress
  static SUCCEEDED = "SUCCEEDED"; // finished successfully
  static STOPPED = "STOPPED"; // stopped due to error or cancellation
}

class RequestType {
  static START = "START"; // start generating
  static CONTINUE = "CONTINUE"; // continue generating
  static CANCEL = "CANCEL"; // cancel (stop) generating
}

const SECONDS_IN_MS = 1000;
const NORMAL_RETRY = 1 * SECONDS_IN_MS;

const COLUMN_TITLE_ORG = "Org";
const COLUMN_TITLE_LOCALE_NAME = "Locale";
const COLUMN_TITLE_LOCALE_ID = "Code";
const COLUMN_TITLE_COVERAGE_LEVEL = "Targ. Cover.";
const COLUMN_TITLE_LEVEL = "Level";
const COLUMN_TITLE_PROGRESS_PERCENT = "Done";
const COLUMN_TITLE_ABSTAIN_COUNT = "Abst.";
// TODO: EMP, MP, maybe using data from api/completion/locale/${locale}
// Reference: https://unicode-org.atlassian.net/browse/CLDR-18610
// const COLUMN_TITLE_EMP = "EMP";
// const COLUMN_TITLE_MP = "MP";
const COLUMN_TITLE_USER_ID = "Vetter#";
const COLUMN_TITLE_USER_EMAIL = "Email";
const COLUMN_TITLE_USER_NAME = "Name";
// TODO: change LastSeen to be the number of days since the user voted
// Reference: https://unicode-org.atlassian.net/browse/CLDR-18610
const COLUMN_TITLE_LAST_MOD = "Days ago";

const COLUMNS = [
  { title: COLUMN_TITLE_ORG, comment: "User organization", default: null },
  { title: COLUMN_TITLE_LOCALE_NAME, comment: "User locale", default: null },
  { title: COLUMN_TITLE_LOCALE_ID, comment: "User locale code", default: null },
  {
    title: COLUMN_TITLE_COVERAGE_LEVEL,
    comment: "Coverage level for this user's organization",
    default: "",
  },
  { title: COLUMN_TITLE_LEVEL, comment: "User level", default: null },
  {
    title: COLUMN_TITLE_PROGRESS_PERCENT,
    comment:
      "User's voting percent, exactly the percent from the second progress meter",
    default: "-",
  },
  {
    title: COLUMN_TITLE_ABSTAIN_COUNT,
    comment:
      "Number of abstains (= # of paths at the coverage level for the locale - # of paths the vetter has voted on)",
    default: 0,
  },
  // TODO: EMP, MP, data from api/completion/locale/${locale}
  // Reference: https://unicode-org.atlassian.net/browse/CLDR-18610
  /*
  {
    title: COLUMN_TITLE_EMP,
    comment: "Sum of errors + missing + provisional (for locale)",
    default: 0,
  },
  {
    title: COLUMN_TITLE_MP,
    comment: "Sum of missing + provisional (for locale)",
    default: 0,
  },
  */
  {
    title: COLUMN_TITLE_USER_ID,
    comment: "User's account number",
    default: null,
  },
  { title: COLUMN_TITLE_USER_EMAIL, comment: "User's email", default: null },
  { title: COLUMN_TITLE_USER_NAME, comment: "User's name", default: null },
  {
    title: COLUMN_TITLE_LAST_MOD,
    comment: "Days since the user last voted",
    default: null,
  },
];

/**
 * Does the user have permission to generate/view vetting participation info?
 */
let canGenerate = false;

/**
 * The most recent request type, RequestType.START/CONTINUE/CANCEL
 */
let latestReq = null;

/**
 * Data used for constructing the vetting participation table, derived
 * directly or indirectly from json received from the server
 */
let vpData = {};

/**
 * Worksheet data, used for both the html table in the Vue component and
 * the XLSX spreadsheet download
 */
let worksheetData = null;

/** @function */
let callbackToSetData = null;

function viewMounted(setData) {
  callbackToSetData = setData;
  const perm = cldrStatus.getPermissions();
  canGenerate = Boolean(perm?.userCanUseVettingParticipation);
}

function hasPermission() {
  return canGenerate;
}

function start() {
  if (VP_DEBUG) {
    console.log("cldrVettingParticipation.start");
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
  if (VP_DEBUG) {
    console.log("cldrVettingParticipation.cancel");
  }
  vpData = {};
  const viewData = { message: "Stopped", percent: 0, status: Status.STOPPED };
  callbackToSetData(viewData);
  makeRequest(RequestType.CANCEL);
}

function makeRequest(req) {
  latestReq = req;
  if (VP_DEBUG) {
    console.log(
      "cldrVettingParticipation.makeRequest, latestReq = " + latestReq
    );
  }
  if (req === RequestType.CANCEL) {
    // Since each of our requests takes only a short time for the server to handle,
    // there is no need to tell the server to cancel a request in progress.
    // Instead, just stop making further requests.
    return;
  } else if (req === RequestType.START) {
    if (VP_DEBUG) {
      console.log(
        "cldrVettingParticipation.makeRequest, fetching initial data"
      );
    }
    const p = new URLSearchParams();
    p.append("what", "vetting_participation");
    p.append("s", cldrStatus.getSessionId());
    const xhrArgs = {
      url: cldrAjax.makeUrl(p),
      handleAs: "json",
      load: loadHandler,
      error: errorHandler,
    };
    cldrAjax.sendXhr(xhrArgs);
  } else if (req === RequestType.CONTINUE) {
    if (VP_DEBUG) {
      console.log("cldrVettingParticipation.makeRequest, fetching more data");
    }
    // Currently, to generate the data once, we're called only once with RequestType.START
    // and only once with RequestType.CONTINUE. fetchMoreData itself generally involves a
    // large number of possibly overlapping http requests/responses.
    fetchMoreData();
  }
}

function loadHandler(json) {
  if (json.err) {
    console.dir({ json });
    cldrNotify.error("Error loading vetting participation", json.err);
  } else if (callbackToSetData) {
    // This json is the response to the initial request to what=vetting_participation
    storeInitialResponseData(json);

    const viewData = {
      message: "Fetching data...",
      percent: 1,
      status: Status.PROCESSING,
    };
    callbackToSetData(viewData);
    if (latestReq !== RequestType.CANCEL) {
      window.setTimeout(fetchStatus.bind(this), NORMAL_RETRY);
    }
  }
}

function errorHandler(err) {
  cldrNotify.exception(err, "Loading vetting participation data");
}

function fetchStatus() {
  if (
    !canGenerate ||
    "vetting_participation" !== cldrStatus.getCurrentSpecial()
  ) {
    canGenerate = false;
    if (VP_DEBUG) {
      console.log(
        "cldrVettingParticipation.fetchStatus resetting for other special"
      );
    }
  } else if (canGenerate) {
    if (VP_DEBUG) {
      console.log(
        "cldrVettingParticipation.fetchStatus; latestReq = " + latestReq
      );
    }
    makeRequest(RequestType.CONTINUE);
  }
}

function storeInitialResponseData(json) {
  const uidToUser = {};
  json.users.forEach((u) => {
    // u is an object with information about a user
    uidToUser[u.id] = u;
  });

  const localeToData = {};
  json.participation.forEach(({ locale, user, daysAgo }) => {
    // "user" here is an integer = user ID
    // daysAgo = how many days ago was the most recent vote by this user in this locale
    if (!localeToData[locale]) {
      localeToData[locale] = { daysAgo: {} };
    }
    localeToData[locale].daysAgo[user] = daysAgo;
  });
  vpData.org = json.org;
  vpData.localeToData = localeToData;
  vpData.uidToUser = uidToUser;
}

async function fetchMoreData() {
  preloadVotingResults();
  if (!wasCancelled()) {
    // Note: some code in preloadVotingResults may still be executing
    // while createTable is executing, due to promises for data which
    // createTable awaits.
    createTable();
  }
}

function preloadVotingResults() {
  let allToFetch = 0; // total count needed to fetch
  let fetched = 0; // number confirmed fetched
  for (const [id, user] of Object.entries(vpData.uidToUser)) {
    if (VP_DEBUG) {
      console.log("preloadVotingResults, outer loop, user id = " + id);
    }
    if (wasCancelled()) {
      return;
    }
    // "user" here is an object; id = user.id
    if (!isRegularVetter(user)) {
      continue;
    }
    user.data = {};
    for (const locale of user.locales.sort()) {
      if (VP_DEBUG) {
        console.log(
          "preloadVotingResults, inner loop, user id = " +
            id +
            ", locale = " +
            locale
        );
      }
      if (wasCancelled()) {
        return;
      }
      // Specifying "org" for the coverage level means that the server will determine
      // the coverage level based on the vetter's organization and the locale (where the
      // vetter is the user whose id is specified here, not the user requesting the data)
      user.data[locale] = cldrAjax.doFetch(
        `./api/summary/participation/for/${id}/${locale}/org`
      );
      allToFetch++;

      user.data[locale].then(() => {
        if (wasCancelled()) {
          return;
        }
        fetched++;
        const fetchPercent = cldrProgress.friendlyPercent(fetched, allToFetch);
        if (VP_DEBUG) {
          console.log(
            "preloadVotingResults, delayed effect, fetchPercent = " +
              fetchPercent +
              ", user id = " +
              id +
              ", locale = " +
              locale
          );
        }
        const viewData = {
          message: fetched + "/" + allToFetch + " " + locale,
          percent: fetchPercent,
          status: Status.PROCESSING,
        };
        callbackToSetData(viewData);
      });
    }
  }
}

async function createTable() {
  const columnIndex = getIndexOfColumnsByTitle();
  const rowMap = {};
  for (const [id, user] of Object.entries(vpData.uidToUser)) {
    if (VP_DEBUG) {
      console.log("createTable, outer loop, user id = " + id);
    }
    if (wasCancelled()) {
      return;
    }
    const row = getDefaultRow(id, user, columnIndex);
    if (user.allLocales || !user.locales) {
      continue;
    }
    for (const locale of user.locales.sort()) {
      if (VP_DEBUG) {
        console.log(
          "createTable, inner loop, user id = " + id + ", locale = " + locale
        );
      }
      const localeName = cldrLoad.getLocaleName(locale);
      row[columnIndex[COLUMN_TITLE_LOCALE_NAME]] = localeName;
      row[columnIndex[COLUMN_TITLE_LOCALE_ID]] = locale;

      // here is where we block waiting on the results from preloadVotingResults
      const data = await user.data[locale];
      const json = await data.json();
      const { votablePathCount, votedPathCount } = json.voterProgress;
      const { coverageLevel } = json;
      const perCent = cldrProgress.friendlyPercent(
        votedPathCount,
        votablePathCount
      );
      row[columnIndex[COLUMN_TITLE_PROGRESS_PERCENT]] = perCent + "%";
      row[columnIndex[COLUMN_TITLE_COVERAGE_LEVEL]] = (
        coverageLevel || ""
      ).toLowerCase();
      row[columnIndex[COLUMN_TITLE_ABSTAIN_COUNT]] =
        votablePathCount - votedPathCount;
      let daysAgo = vpData.localeToData[locale]?.daysAgo[id];
      if (daysAgo === undefined) {
        // distinguish "0" from undefined
        daysAgo = "♾️";
      }
      row[columnIndex[COLUMN_TITLE_LAST_MOD]] = daysAgo;
      const sortKey = localeName + " " + user.org + " " + id;
      rowMap[sortKey] = [...row]; // clone the array since worksheetData will retain a reference
    }
  }
  worksheetData = [];
  worksheetData.push(getHeaderRow());
  Object.entries(rowMap)
    .sort((a, b) => a[0].localeCompare(b[0]))
    .forEach(([sortKey, row]) => {
      worksheetData.push(row);
    });
  showResults();
}

function showResults() {
  if (VP_DEBUG) {
    console.log("showResults, done, 100%");
  }
  const viewData = {
    message: "Done",
    percent: 100,
    status: Status.SUCCEEDED,
    table: worksheetData,
  };
  callbackToSetData(viewData);
}

function wasCancelled() {
  return latestReq === RequestType.CANCEL;
}

async function saveAsSheet() {
  if (VP_DEBUG) {
    console.log("cldrVettingParticipation.saveAsSheet");
  }

  const worksheet = XLSX.utils.aoa_to_sheet(worksheetData);

  addColumnComments(worksheet);

  const workbook = XLSX.utils.book_new();

  const worksheetName = (vpData.org || "ALL").substring(0, 31);

  XLSX.utils.book_append_sheet(workbook, worksheet, worksheetName);

  await appendOrgSheet(workbook);

  XLSX.writeFile(workbook, `survey_participation.${vpData.org || "ALL"}.xlsx`, {
    cellStyles: true,
  });
}

/** append a sheet with Organization metadata (Locales.txt) */
async function appendOrgSheet(workbook) {
  const { shortToDisplay } = await cldrOrganizations.get();
  const tcOrgs = cldrOrganizations.getTcOrgs();
  const orgList = Object.keys(shortToDisplay).sort();

  // write Organizations list
  {
    const worksheetData = [["org", "name", "tc"]];
    for (const org of orgList) {
      worksheetData.push([
        org,
        shortToDisplay[org],
        (await tcOrgs).includes(org),
      ]);
    }

    const worksheet = XLSX.utils.aoa_to_sheet(worksheetData);

    cldrXlsx.pushComment(worksheet, { r: 0, c: 0 }, "Organization short name");
    cldrXlsx.pushComment(worksheet, { r: 0, c: 1 }, "Organization long name");
    cldrXlsx.pushComment(worksheet, { r: 0, c: 2 }, "true if TC organization");

    XLSX.utils.book_append_sheet(workbook, worksheet, "Organizations");
  }

  // write Coverage list
  {
    const orgToLocaleLevel = await cldrOrganizations.getOrgCoverage();

    const worksheetData = [
      ["org", "name", "tc", "locale", "localeName", "coverage"],
    ];
    for (const org of orgList) {
      const localeToCoverage = orgToLocaleLevel[org];
      for (const [locale, level] of Object.entries(localeToCoverage)) {
        worksheetData.push([
          org,
          shortToDisplay[org],
          (await tcOrgs).includes(org),
          locale,
          cldrLoad.getLocaleName(locale),
          level.toLowerCase(),
        ]);
      }
    }

    const worksheet = XLSX.utils.aoa_to_sheet(worksheetData);

    cldrXlsx.pushComment(worksheet, { r: 0, c: 0 }, "Organization short name");
    cldrXlsx.pushComment(worksheet, { r: 0, c: 1 }, "Organization long name");
    cldrXlsx.pushComment(worksheet, { r: 0, c: 2 }, "true if TC organization");
    cldrXlsx.pushComment(worksheet, { r: 0, c: 3 }, "locale id");
    cldrXlsx.pushComment(worksheet, { r: 0, c: 4 }, "locale name");
    cldrXlsx.pushComment(
      worksheet,
      { r: 0, c: 5 },
      "coverage goal from Locales.txt"
    );

    XLSX.utils.book_append_sheet(workbook, worksheet, "Coverage");
  }
}

function getHeaderRow() {
  const row = [];
  for (let col of COLUMNS) {
    row.push(col.title);
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

function getDefaultRow(id, user, columnIndex) {
  const row = [];
  for (let col of COLUMNS) {
    row.push(col.default);
  }
  row[columnIndex[COLUMN_TITLE_ORG]] = user.org;
  row[columnIndex[COLUMN_TITLE_LEVEL]] = user.userlevelName;
  row[columnIndex[COLUMN_TITLE_USER_ID]] = id;
  row[columnIndex[COLUMN_TITLE_USER_EMAIL]] = user.email;
  row[columnIndex[COLUMN_TITLE_USER_NAME]] = user.name;
  return row;
}

function addColumnComments(worksheet) {
  let columnNumber = 0;
  for (let col of COLUMNS) {
    const cell = XLSX.utils.encode_cell({ r: 0, c: columnNumber }); // A1, B1, C1, ...
    // include title with comment, for convenience if column is narrower than title
    cldrXlsx.pushComment(worksheet, cell, col.title + ": " + col.comment);
    ++columnNumber;
  }
}

/** are we tracking this user? */
function isRegularVetter(user) {
  if (user.allLocales || !user.locales) {
    return false;
  }
  return user.userlevelName === "vetter" || user.userlevelName === "guest";
}

export { Status, cancel, hasPermission, saveAsSheet, start, viewMounted };

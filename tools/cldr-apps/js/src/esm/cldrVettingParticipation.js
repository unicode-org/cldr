/*
 * cldrVettingParticipation: encapsulate Survey Tool Vetting Participation code.
 */
import * as cldrAccount from "./cldrAccount.js";
import * as cldrAjax from "./cldrAjax.js";
import * as cldrDom from "./cldrDom.js";
import * as cldrInfo from "./cldrInfo.js";
import * as cldrLoad from "./cldrLoad.js";
import * as cldrProgress from "./cldrProgress.js";
import * as cldrRetry from "./cldrRetry.js";
import * as cldrStatus from "./cldrStatus.js";
import * as cldrSurvey from "./cldrSurvey.js";
import * as cldrText from "./cldrText.js";
import * as cldrXlsx from "./cldrXlsx.js";
import * as XLSX from "xlsx";

const COLUMN_TITLE_ORG = "Org";
const COLUMN_TITLE_LOCALE_NAME = "Locale";
const COLUMN_TITLE_LOCALE_ID = "Code";
const COLUMN_TITLE_LEVEL = "Level";
const COLUMN_TITLE_VOTES = "Votes";
const COLUMN_TITLE_COVERAGE_COUNT = "Cldr Coverage Count";
const COLUMN_TITLE_VOTED_PATH_COUNT = "Progress Vote";
const COLUMN_TITLE_VOTABLE_PATH_COUNT = "Progress Count";
const COLUMN_TITLE_PROGRESS_PERCENT = "Progress Percent";
const COLUMN_TITLE_USER_ID = "Vetter#";
const COLUMN_TITLE_USER_EMAIL = "Email";
const COLUMN_TITLE_USER_NAME = "Name";
const COLUMN_TITLE_LAST_SEEN = "LastSeen";
const COLUMN_TITLE_COVERAGE_LEVEL = "Coverage";
const COLUMN_TITLE_VOTES_BY_TYPE = "Votes by Type";

const COLUMNS = [
  { title: COLUMN_TITLE_ORG, comment: "User organization", default: null },
  { title: COLUMN_TITLE_LOCALE_NAME, comment: "User locale", default: null },
  { title: COLUMN_TITLE_LOCALE_ID, comment: "User locale code", default: null },
  { title: COLUMN_TITLE_LEVEL, comment: "User level", default: null },
  {
    title: COLUMN_TITLE_VOTES,
    comment:
      "User vote count, total number of path values in this locale that have a vote from this vetter, possibly including paths that are above the organization's coverage target for the locale (such as comprehensive)",
    default: 0,
  },
  {
    title: COLUMN_TITLE_COVERAGE_COUNT,
    comment:
      "Total number of paths that are in CLDR's coverage target for this locale",
    default: 0,
  },
  {
    title: COLUMN_TITLE_VOTED_PATH_COUNT,
    comment:
      "User's voting progress, this is exactly the number from the second meter of the dashboard",
    // TODO: change to
    // "User's voting progress (number of items already voted on), exactly the numerator from the second progress meter",
    // Reference: https://unicode-org.atlassian.net/browse/CLDR-15850
    default: 0,
  },
  {
    title: COLUMN_TITLE_VOTABLE_PATH_COUNT,
    comment:
      "User's voting total, this is exactly the total from the second meter of the dashboard",
    // TODO: change to
    // "User's voting goal (total number of votable items), exactly the denominator from the second progress meter",
    // Reference: https://unicode-org.atlassian.net/browse/CLDR-15850
    default: 0,
  },
  {
    title: COLUMN_TITLE_PROGRESS_PERCENT,
    comment:
      "User's voting perent, this is exactly the percent from the second meter of the dashboard",
    // TODO: change to
    // "User's voting percent, exactly the percent from the second progress meter",
    // Reference: https://unicode-org.atlassian.net/browse/CLDR-15850
    default: "-",
  },
  {
    title: COLUMN_TITLE_COVERAGE_LEVEL,
    comment: "Coverage level for this user's organization",
    default: "",
  },
  {
    title: COLUMN_TITLE_USER_ID,
    comment: "User's account number",
    default: null,
  },
  { title: COLUMN_TITLE_USER_EMAIL, comment: "User's email", default: null },
  { title: COLUMN_TITLE_USER_NAME, comment: "User's name", default: null },
  {
    title: COLUMN_TITLE_LAST_SEEN,
    comment: "When the user last logged in",
    default: null,
  },
  {
    title: COLUMN_TITLE_VOTES_BY_TYPE,
    comment: "Vote counts by type",
    default: "",
  },
];

let nf = null; // Intl.NumberFormat initialized later

/**
 * Fetch the Vetting Participation data from the server, and "load" it
 *
 * Called as special.load
 */
function load() {
  cldrInfo.showMessage(cldrText.get("vetting_participationGuidance"));

  const url = getAjaxUrl();
  const xhrArgs = {
    url: url,
    handleAs: "json",
    load: loadHandler,
    error: errorHandler,
  };
  cldrAjax.sendXhr(xhrArgs);
}

function loadHandler(json) {
  if (json.err) {
    cldrRetry.handleDisconnect(
      json.err,
      json,
      "",
      "Loading vetting participation data"
    );
    return;
  }
  const ourDiv = document.createElement("div");
  loadVettingParticipation(json, ourDiv);
  cldrSurvey.hideLoader();
  cldrLoad.flipToOtherDiv(ourDiv);
}

function errorHandler(err) {
  cldrRetry.handleDisconnect(err, json, "", "Loading forum participation data");
}

/**
 * Get the AJAX URL to use for loading the Vetting Participation page
 */
function getAjaxUrl() {
  const p = new URLSearchParams();
  p.append("what", "vetting_participation");
  p.append("s", cldrStatus.getSessionId());
  return cldrAjax.makeUrl(p);
}

async function downloadVettingParticipation(opts) {
  const {
    missingLocalesForOrg,
    // languagesNotInCLDR,
    // hasAllLocales,
    localeToData,
    // totalCount,
    uidToUser,
    progressDiv,
    downloadButton,
  } = opts;
  downloadButton.disabled = true;
  cldrDom.removeAllChildNodes(progressDiv);
  const progBar = document.createElement("div");
  progBar.className = "bar";
  progBar.style.height = "1em";
  progBar.style.width = "0px";
  const progRemain = document.createElement("div");
  progRemain.className = "remain";
  progRemain.style.height = "1em";

  // TODO: pin to max width
  function setProgress(bar, total) {
    const remain = total - bar;
    progBar.style.width = `${bar}px`;
    progRemain.style.width = `${remain}px`;
  }

  progressDiv.appendChild(progBar);
  progressDiv.appendChild(progRemain);
  const statusMsg = document.createElement("i");
  progressDiv.appendChild(statusMsg);

  function setStatus(msg) {
    if (statusMsg.firstChild) {
      statusMsg.removeChild(statusMsg.firstChild);
    }
    statusMsg.appendChild(document.createTextNode(msg));
  }

  const wb = XLSX.utils.book_new();

  const ws_name = (missingLocalesForOrg || "ALL").substring(0, 31);

  const ws_data = [];
  ws_data.push(getHeaderRow());

  const columnIndex = getIndexOfColumnsByTitle();

  const userCount = Object.entries(uidToUser).length;

  let userNo = 0;
  setProgress(0, userCount);

  for (const [id, user] of Object.entries(uidToUser)) {
    userNo++;
    setProgress(userNo, userCount);
    const row = getDefaultRow(id, user, columnIndex);
    if (user.allLocales) {
      row[columnIndex[COLUMN_TITLE_LOCALE_NAME]] = "ALL";
      row[columnIndex[COLUMN_TITLE_LOCALE_ID]] = "*";
      row[columnIndex[COLUMN_TITLE_VOTED_PATH_COUNT]] = "-";
      row[columnIndex[COLUMN_TITLE_VOTABLE_PATH_COUNT]] = "-";
      ws_data.push(row);
    } else if (!user.locales) {
      // no locales?!
      row[columnIndex[COLUMN_TITLE_LOCALE_NAME]] = "NONE";
      row[columnIndex[COLUMN_TITLE_LOCALE_ID]] = "-";
      row[columnIndex[COLUMN_TITLE_VOTED_PATH_COUNT]] = "-";
      row[columnIndex[COLUMN_TITLE_VOTABLE_PATH_COUNT]] = "-";
      ws_data.push(row);
    } else {
      for (const locale of user.locales) {
        row[columnIndex[COLUMN_TITLE_LOCALE_NAME]] =
          cldrLoad.getLocaleName(locale);
        row[columnIndex[COLUMN_TITLE_LOCALE_ID]] = locale;
        row[columnIndex[COLUMN_TITLE_VOTES]] =
          localeToData[locale].participation[id] || 0;
        row[columnIndex[COLUMN_TITLE_COVERAGE_COUNT]] =
          localeToData[locale].cov_count || 0;

        if (user.userlevelName === "vetter" || user.userlevelName === "guest") {
          const level = "org";
          setStatus(`Fetch ${id}/${locale}/${level}`);
          const data = await cldrAjax.doFetch(
            `./api/summary/participation/for/${id}/${locale}/${level}`
          );
          const json = await data.json();
          const { votablePathCount, votedPathCount, typeCount } =
            json.voterProgress;
          const { coverageLevel } = json;
          row[columnIndex[COLUMN_TITLE_VOTED_PATH_COUNT]] = votedPathCount;
          row[columnIndex[COLUMN_TITLE_VOTABLE_PATH_COUNT]] = votablePathCount;
          const perCent = cldrProgress.friendlyPercent(
            votedPathCount,
            votablePathCount
          );
          row[columnIndex[COLUMN_TITLE_PROGRESS_PERCENT]] = `${perCent}%`;
          row[columnIndex[COLUMN_TITLE_COVERAGE_LEVEL]] = (
            coverageLevel || ""
          ).toLowerCase();
          row[14] = typeCountToString(typeCount);
        } else {
          // only guest and vetter users
          row[columnIndex[COLUMN_TITLE_VOTED_PATH_COUNT]] = "-";
          row[columnIndex[COLUMN_TITLE_VOTABLE_PATH_COUNT]] = "-";
        }
        ws_data.push([...row]); // clone the array because ws_data will retain a reference
      }
    }
  }

  setStatus("Write XLSX...");
  const ws = XLSX.utils.aoa_to_sheet(ws_data);

  addColumnComments(ws);

  XLSX.utils.book_append_sheet(wb, ws, ws_name);
  XLSX.writeFile(
    wb,
    `survey_participation.${missingLocalesForOrg || "ALL"}.xlsx`
  );
  cldrDom.removeAllChildNodes(progressDiv);
}

/**
 * Populate the given div, given the json for Vetting Participation
 *
 * @param json
 * @param ourDiv
 */
function loadVettingParticipation(json, ourDiv) {
  nf = new Intl.NumberFormat();
  const { missingLocalesForOrg, languagesNotInCLDR, hasAllLocales } = json;

  // crunch the numbers
  const { localeToData, totalCount, uidToUser } = calculateData(json);

  ourDiv.id = "vettingParticipation";
  const div = $(ourDiv);

  // Front matter
  div.append($("<h3>Locales and Vetting Participation</h3>"));
  div.append(
    $("<p/>", {
      text: `Total votes: ${nf.format(totalCount || 0)}`,
    })
  );
  const downloadButton = document.createElement("button");
  const progressDiv = document.createElement("div");
  progressDiv.className = "vvprogress";
  downloadButton.appendChild(document.createTextNode("Download… (.xlsx)"));
  downloadButton.onclick = () =>
    downloadVettingParticipation({
      // for now, throw in all data here.
      missingLocalesForOrg,
      languagesNotInCLDR,
      hasAllLocales,
      localeToData,
      totalCount,
      uidToUser,
      progressDiv,
      downloadButton,
    }).then(
      () => {
        downloadButton.disabled = false;
      },
      (err) => {
        console.error(err);
        downloadButton.disabled = false;
      }
    );
  div.append(downloadButton);
  div.append(progressDiv);

  if (missingLocalesForOrg) {
    div.append(
      $("<i/>", {
        text: `“No Coverage” locales indicate that there are no regular vetters assigned in the “${missingLocalesForOrg}” organization.`,
      })
    );
    if (hasAllLocales) {
      div.append(
        $("<p/>", {
          text: " The organiation has an asterisk (*) entry, indicating that all locales are allowed. “No coverage” means there is no coverage for a locale which is explicitly listed in Locales.txt. ",
        })
      );
    }
    if (languagesNotInCLDR && languagesNotInCLDR.length > 0) {
      div.append(
        $("<h4/>", {
          text: "Locales not in CLDR",
        })
      );
      div.append(
        $("<i/>", {
          text: `These locales are specified by Locales.txt for ${missingLocalesForOrg}, but do not exist in CLDR yet:`,
        })
      );
      for (const loc of languagesNotInCLDR) {
        div.append(
          $("<tt/>", {
            class: "fallback_code missingLoc",
            // Note: can't use locmap to get a translation here, because locmap only
            // has extant CLDR locales, and by definition 'loc' is not in CLDR yet.
            text: `${loc}`, // raw code
          })
        );
      }
    }
  }

  // Chapter 1

  const locmap = cldrLoad.getTheLocaleMap();
  const localeList = div.append($('<div class="locList" ></div>'));
  // console.dir(localeToData);
  for (const loc of Object.keys(localeToData).sort()) {
    const e = localeToData[loc]; // consistency
    const li = $('<div class="locRow"></div>');
    localeList.append(li);
    const locLabel = $(`<div class='locId'></div>`);
    locLabel.append(
      $("<a></a>", {
        text: locmap.getLocaleName(loc),
        href: cldrLoad.linkToLocale(loc),
      })
    );
    li.append(locLabel);
    if (e.count) {
      locLabel.append(
        $(`<i/>`, {
          text: nf.format(e.count),
          class: "count",
          title: "number of votes for this locale",
        })
      );
    } else {
      locLabel.append(
        $(`<i/>`, {
          text: nf.format(e.count || 0),
          class: "count missingLoc",
          title: "number of votes for this locale",
        })
      );
    }
    if (e.missing) {
      locLabel.append(
        $(`<i/>`, {
          class: "missingLoc",
          text: "(No Coverage)",
          title: `No regular vetters for ${missingLocalesForOrg}`,
        })
      );
    }
    const myUsers = getUsersFor(e, uidToUser);
    if (myUsers && myUsers.length > 0) {
      const theUserBox = $("<span/>", { class: "participatingUsers" });
      li.append(theUserBox);
      myUsers.forEach(function (u) {
        verb(u, theUserBox);
      });
    }
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
  row[columnIndex[COLUMN_TITLE_LAST_SEEN]] = user.time;
  return row;
}

function addColumnComments(ws) {
  let unicode = "A".charCodeAt(0);
  for (let col of COLUMNS) {
    const letter = String.fromCharCode(unicode++); // A, B, C, ... assume max 26 columns!
    const cell = letter + "1"; // A1, B1, C1, ...
    cldrXlsx.pushComment(ws, cell, col.comment);
  }
}

function verb(u, theUserBox) {
  const user = u.user;
  if (!user) {
    console.log("Empty user in verb");
    return;
  }
  const isVetter = u.isVetter;
  const count = u.count;

  const theU = $('<span class="participatingUser"></span>');
  theU.append($(cldrAccount.createUser(user)));
  if (user.allLocales) {
    theU.addClass("allLocales");
    theU.append(
      $("<span/>", {
        text: "*",
        title: "user can vote for all locales",
      })
    );
  }
  if (isVetter) {
    theU.addClass("vetter");
  }
  if (!count) {
    theU.addClass("noparticip");
  }
  theU.append(
    $("<span/>", {
      class: count ? "count" : "count noparticip",
      text: nf.format(count || 0),
      title: "number of this user’s votes",
    })
  );
  theUserBox.append(theU);
}

/**
 * Calculate the top level data,
 * returning localeToData, totalCount, uidToUser
 */
function calculateData(json) {
  // first, collect coverage
  const { participation, users, languagesMissing } = json;

  const localeToData = {};
  let totalCount = 0;
  function getLocale(loc) {
    const e = (localeToData[loc] = localeToData[loc] || {
      vetters: [],
      count: 0,
      participation: {},
    });
    return e;
  }
  const uidToUser = {};
  // collect users w/ coverage
  users.forEach((u) => {
    const { locales, id } = u;
    uidToUser[id] = u;
    (locales || []).forEach((loc) => getLocale(loc).vetters.push(id));
  });
  // collect missing
  (languagesMissing || []).forEach((loc) => (getLocale(loc).missing = true));
  participation.forEach(({ count, locale, user, cov_count }) => {
    const e = getLocale(locale);
    e.count += count;
    totalCount += count;
    e.participation[user] = count;
    e.cov_count = cov_count; // cov_count is currently per-locale data.
  });

  return { localeToData, totalCount, uidToUser };
}

/**
 * @param {Number[]} e.vetters - vetter ID int array
 * @param {Object} e.participation  - map of int (id) to count
 * @param {Object} uidToUser - map from ID to user
 */
function getUsersFor(e, uidToUser) {
  // collect all users
  const myUids = new Set(e.vetters.map((v) => Number(v)));
  const vetterSet = new Set(e.vetters.map((v) => Number(v)));
  for (const [id, p] of Object.entries(e.participation)) {
    // participation comes from the votes table
    // uidToUser comes from the users table
    // Sometimes uidToUser[id] is undefined (maybe user was deleted)
    // --- skip id in that case
    if (id in uidToUser) {
      myUids.add(Number(id));
    } else {
      // this does happen for me when logged in as admin on localhost
      console.log("getUsersFor bad id: " + id);
    }
  }
  let myUsers = Array.from(myUids.values());
  if (myUsers.length) {
    myUsers = myUsers.map(function (id) {
      return myMap(id, uidToUser, vetterSet, e);
    });
    myUsers = myUsers.sort(mySort);
  }
  return myUsers;
}

function myMap(id, uidToUser, vetterSet, e) {
  if (!uidToUser[id]) {
    // this no longer happens for me, since adding filter [if (id in uidToUser)] in getUsersFor
    console.log("myMap bad id: " + id);
  }
  return {
    user: uidToUser[id],
    isVetter: vetterSet.has(id),
    count: e.participation[id],
  };
}

function mySort(a, b) {
  if (!a.user || !a.user.name) {
    // this no longer happens for me, since adding filter [if (id in uidToUser)] in getUsersFor
    console.log("mySort bad user a: " + a);
    return 0;
  }
  if (!b.user || !b.user.name) {
    console.log("mySort bad user b: " + b);
    return 0;
  }
  return a.user.name.localeCompare(b.user.name);
}

function typeCountToString(typeCount) {
  let str = "";
  if (typeCount) {
    for (let key of Object.keys(typeCount).sort()) {
      if (str) {
        str += ";";
      }
      str += key + ":" + typeCount[key];
    }
  }
  return str;
}

export { load };

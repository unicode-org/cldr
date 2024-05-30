/*
 * cldrVettingParticipation: encapsulate Survey Tool Vetting Participation code.
 */
import * as cldrAccount from "./cldrAccount.mjs";
import * as cldrAjax from "./cldrAjax.mjs";
import * as cldrDom from "./cldrDom.mjs";
import * as cldrLoad from "./cldrLoad.mjs";
import * as cldrNotify from "./cldrNotify.mjs";
import * as cldrOrganizations from "./cldrOrganizations.mjs";
import * as cldrProgress from "./cldrProgress.mjs";
import * as cldrRetry from "./cldrRetry.mjs";
import * as cldrStatus from "./cldrStatus.mjs";
import * as cldrSurvey from "./cldrSurvey.mjs";
import * as cldrText from "./cldrText.mjs";
import * as cldrXlsx from "./cldrXlsx.mjs";
import * as XLSX from "xlsx";

const COLUMN_TITLE_ORG = "Org";
const COLUMN_TITLE_LOCALE_NAME = "Locale";
const COLUMN_TITLE_LOCALE_ID = "Code";
const COLUMN_TITLE_VETTERS_PER_LOCALE = "/Vetters";
const COLUMN_TITLE_LOCALES_PER_VETTER = "/Locales";
const COLUMN_TITLE_LEVEL = "Level";
const COLUMN_TITLE_VOTES = "Votes";
const COLUMN_TITLE_CLDR_COVERAGE_COUNT = "Cldr Coverage Count";
const COLUMN_TITLE_ORG_COVERAGE_COUNT = "Org Coverage Count";
const COLUMN_TITLE_VOTED_PATH_COUNT = "Progress Vote";
const COLUMN_TITLE_VOTABLE_PATH_COUNT = "Progress Count";
const COLUMN_TITLE_PROGRESS_PERCENT = "Progress Percent";
const COLUMN_TITLE_VOTES_DIRECT = "Direct Votes";
const COLUMN_TITLE_VOTES_NON_DIRECT = "Non Direct Votes";
const COLUMN_TITLE_VOTES_AUTO_IMPORT = "Auto-imported Votes";
const COLUMN_TITLE_VOTES_MANUAL_IMPORT = "Manually-imported Votes";
const COLUMN_TITLE_VOTES_BULK_UPLOAD = "Bulk-uploaded Votes";
const COLUMN_TITLE_VOTES_UNKNOWN = "Unknown-type Votes";
const COLUMN_TITLE_USER_ID = "Vetter#";
const COLUMN_TITLE_USER_EMAIL = "Email";
const COLUMN_TITLE_USER_NAME = "Name";
const COLUMN_TITLE_LAST_SEEN = "LastSeen";
const COLUMN_TITLE_COVERAGE_LEVEL = "Org Coverage";

const COLUMNS = [
  { title: COLUMN_TITLE_ORG, comment: "User organization", default: null },
  { title: COLUMN_TITLE_LOCALE_NAME, comment: "User locale", default: null },
  { title: COLUMN_TITLE_LOCALE_ID, comment: "User locale code", default: null },

  {
    title: COLUMN_TITLE_VETTERS_PER_LOCALE,
    comment: "Vetters per Locale",
    default: 0,
  },
  {
    title: COLUMN_TITLE_LOCALES_PER_VETTER,
    comment: "Locales per this Vetter",
    default: 0,
  },

  { title: COLUMN_TITLE_LEVEL, comment: "User level", default: null },
  {
    title: COLUMN_TITLE_ORG_COVERAGE_COUNT,
    comment:
      "Total number of paths that are in Org's coverage target for this locale",
    default: 0,
  },
  {
    title: COLUMN_TITLE_VOTED_PATH_COUNT,
    comment:
      "User's voting progress (number of items already voted on), exactly the numerator from the second progress meter",
    default: 0,
  },
  {
    title: COLUMN_TITLE_VOTABLE_PATH_COUNT,
    comment:
      "User's voting goal (total number of votable items), exactly the denominator from the second progress meter",
    default: 0,
  },
  {
    title: COLUMN_TITLE_PROGRESS_PERCENT,
    comment:
      "User's voting percent, exactly the percent from the second progress meter",
    default: "-",
  },
  {
    title: COLUMN_TITLE_VOTES_DIRECT,
    comment:
      "Number of direct votes by this user within the organization coverage level",
    default: "",
  },
  {
    title: COLUMN_TITLE_VOTES_NON_DIRECT,
    comment:
      "Number of votes by this user that aren't direct, at the organization coverage level",
    default: "",
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
  // hide/delete after this
  {
    title: COLUMN_TITLE_VOTES,
    comment:
      "User vote count, total number of path values in this locale that have a vote from this vetter, possibly including paths that are above the organization's coverage target for the locale (such as comprehensive)",
    default: 0,
  },
  {
    title: COLUMN_TITLE_CLDR_COVERAGE_COUNT,
    comment:
      "Total number of paths that are in CLDR's coverage target for this locale",
    default: 0,
  },
  {
    title: COLUMN_TITLE_VOTES_AUTO_IMPORT,
    comment:
      "Number of automatically-imported votes by this user within the organization coverage level",
    default: "",
  },
  {
    title: COLUMN_TITLE_VOTES_MANUAL_IMPORT,
    comment:
      "Number of manually-imported votes by this user within the organization coverage level",
    default: "",
  },
  {
    title: COLUMN_TITLE_VOTES_BULK_UPLOAD,
    comment:
      "Number of bulk-uploaded votes by this user within the organization coverage level",
    default: "",
  },
  {
    title: COLUMN_TITLE_VOTES_UNKNOWN,
    comment:
      "Number of votes of unknown type by this user within the organization coverage level",
    default: "",
  },
];

// Google Sheets ignores the hidden column property, so delete them instead
const DELETE_HIDDEN = true;

const HIDDEN_COLUMNS = [
  COLUMN_TITLE_VOTES,
  COLUMN_TITLE_VOTES_MANUAL_IMPORT,
  COLUMN_TITLE_VOTES_BULK_UPLOAD,
  COLUMN_TITLE_VOTES_AUTO_IMPORT,
  COLUMN_TITLE_VOTES_UNKNOWN,
];
const LAST_VISIBLE_COLUMN = COLUMN_TITLE_LAST_SEEN;
const LAST_HIDDEN_COLUMN = COLUMN_TITLE_VOTES_UNKNOWN;

const VOTE_TYPES = {
  DIRECT: COLUMN_TITLE_VOTES_DIRECT,
  AUTO_IMPORT: COLUMN_TITLE_VOTES_AUTO_IMPORT,
  MANUAL_IMPORT: COLUMN_TITLE_VOTES_MANUAL_IMPORT,
  BULK_UPLOAD: COLUMN_TITLE_VOTES_BULK_UPLOAD,
  UNKNOWN: COLUMN_TITLE_VOTES_UNKNOWN,
  NON_DIRECT: COLUMN_TITLE_VOTES_NON_DIRECT,
};

let nf = null; // Intl.NumberFormat initialized later

/**
 * Fetch the Vetting Participation data from the server, and "load" it
 *
 * Called as special.load
 */
function load() {
  if (!cldrStatus.getSurveyUser()) {
    // show error instead of hang if not logged in
    cldrLoad.flipToOtherDiv(document.createTextNode("Not logged in."));
    return;
  }
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
    console.dir({ json });
    cldrNotify.error("Err Loading Vetting Participation", json.err);
    return;
  }
  const ourDiv = document.createElement("div");
  loadVettingParticipation(json, ourDiv);
  cldrSurvey.hideLoader();
  cldrLoad.flipToOtherDiv(ourDiv);
}

function errorHandler(err) {
  cldrNotify.exception(err, "Loading vetting participation data");
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
  let allLocalesCount = 0;

  const vettersPerLocale = [];
  for (const [, { locales, allLocales }] of Object.entries(uidToUser)) {
    for (const locale of locales ?? []) {
      vettersPerLocale[locale] = (vettersPerLocale[locale] ?? 0) + 1;
    }
    if (allLocales) {
      allLocalesCount++;
    }
  }

  /** are we tracking this user? */
  function isRegularVetter(user) {
    if (user.allLocales) return false;
    if (!user.locales) return false;
    if (user.userlevelName === "vetter" || user.userlevelName === "guest")
      return true;
    return false; // some other level
  }

  // total count needed to fetch
  let allToFetch = 0;
  // number confirmed fetched
  let fetched = 0;

  /** preload voting results. we'll await the results later */
  for (const [id, user] of Object.entries(uidToUser)) {
    if (isRegularVetter(user)) {
      user.data = {};
      for (const locale of user.locales) {
        const level = "org";
        user.data[locale] = cldrAjax.doFetch(
          `./api/summary/participation/for/${id}/${locale}/${level}`
        );
        allToFetch++;

        user.data[locale].then(() => {
          fetched++;
          setProgress(fetched, allToFetch);
          const fetchPercent = cldrProgress.friendlyPercent(
            fetched,
            allToFetch
          );
          setStatus(`(${fetchPercent}% of ${allToFetch} fetched)`);
        });
      }
    }
  }

  for (const [id, user] of Object.entries(uidToUser)) {
    userNo++;
    const row = getDefaultRow(id, user, columnIndex);
    if (user.allLocales) {
      row[columnIndex[COLUMN_TITLE_LOCALE_NAME]] = "ALL";
      row[columnIndex[COLUMN_TITLE_LOCALE_ID]] = "*";
      row[columnIndex[COLUMN_TITLE_VOTED_PATH_COUNT]] = "-";
      row[columnIndex[COLUMN_TITLE_VOTABLE_PATH_COUNT]] = "-";
      row[columnIndex[COLUMN_TITLE_VETTERS_PER_LOCALE]] = allLocalesCount;
      ws_data.push(row);
    } else if (!user.locales) {
      // no locales?!
      row[columnIndex[COLUMN_TITLE_LOCALE_NAME]] = "NONE";
      row[columnIndex[COLUMN_TITLE_LOCALE_ID]] = "-";
      row[columnIndex[COLUMN_TITLE_VOTED_PATH_COUNT]] = "-";
      row[columnIndex[COLUMN_TITLE_VOTABLE_PATH_COUNT]] = "-";
      ws_data.push(row);
    } else {
      row[columnIndex[COLUMN_TITLE_LOCALES_PER_VETTER]] = user.locales.length;
      for (const locale of user.locales) {
        row[columnIndex[COLUMN_TITLE_VETTERS_PER_LOCALE]] =
          vettersPerLocale[locale];
        row[columnIndex[COLUMN_TITLE_LOCALE_NAME]] =
          cldrLoad.getLocaleName(locale);
        row[columnIndex[COLUMN_TITLE_LOCALE_ID]] = locale;
        row[columnIndex[COLUMN_TITLE_VOTES]] =
          localeToData[locale].participation[id] || 0;
        row[columnIndex[COLUMN_TITLE_ORG_COVERAGE_COUNT]] =
          localeToData[locale].org_count || 0;
        row[columnIndex[COLUMN_TITLE_CLDR_COVERAGE_COUNT]] =
          localeToData[locale].cldr_count || 0;

        if (user.userlevelName === "vetter" || user.userlevelName === "guest") {
          const level = "org";
          // here is where we block waiting on the results from above
          const data = await user.data[locale];
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
          row[columnIndex[COLUMN_TITLE_PROGRESS_PERCENT]] = perCent / 100.0;
          getVoteTypes(row, columnIndex, typeCount);
          row[columnIndex[COLUMN_TITLE_COVERAGE_LEVEL]] = (
            coverageLevel || ""
          ).toLowerCase();
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
  // set percent
  for (let r = 1; r < ws_data.length; r++) {
    const cell = XLSX.utils.encode_cell({
      r,
      c: columnIndex[COLUMN_TITLE_PROGRESS_PERCENT],
    });
    XLSX.utils.cell_set_number_format(ws[cell], "0%");
  }

  // omit hidden columns
  if (DELETE_HIDDEN) {
    ws["!ref"] = ws["!ref"].replace(
      XLSX.utils.encode_col(columnIndex[LAST_HIDDEN_COLUMN]),
      XLSX.utils.encode_col(columnIndex[LAST_VISIBLE_COLUMN])
    );
  } else {
    // hide these columns
    ws["!cols"] = [];
    for (const c of HIDDEN_COLUMNS) {
      ws["!cols"][columnIndex[c]] = [{ hidden: true, wch: 0 }];
    }
  }

  XLSX.utils.book_append_sheet(wb, ws, ws_name);

  // add the org sheet
  await appendOrgSheet(wb);

  XLSX.writeFile(
    wb,
    `survey_participation.${missingLocalesForOrg || "ALL"}.xlsx`,
    {
      cellStyles: true,
    }
  );
  cldrDom.removeAllChildNodes(progressDiv);
}

/** append a sheet with Organization metadata (Locales.txt) */
async function appendOrgSheet(wb) {
  const { shortToDisplay } = await cldrOrganizations.get();
  const tcOrgs = cldrOrganizations.getTcOrgs();
  const orgList = Object.keys(shortToDisplay).sort();

  // write Organizations list
  {
    const ws_data = [["org", "name", "tc"]];
    for (const org of orgList) {
      ws_data.push([org, shortToDisplay[org], (await tcOrgs).includes(org)]);
    }

    const ws = XLSX.utils.aoa_to_sheet(ws_data);

    cldrXlsx.pushComment(ws, { r: 0, c: 0 }, "Organization short name");
    cldrXlsx.pushComment(ws, { r: 0, c: 1 }, "Organization long name");
    cldrXlsx.pushComment(ws, { r: 0, c: 2 }, "true if TC organization");

    XLSX.utils.book_append_sheet(wb, ws, "Organizations");
  }

  // write Coverage list
  {
    const orgToLocaleLevel = await cldrOrganizations.getOrgCoverage();

    const ws_data = [["org", "name", "tc", "locale", "localeName", "coverage"]];
    for (const org of orgList) {
      const localeToCoverage = orgToLocaleLevel[org];
      for (const [locale, level] of Object.entries(localeToCoverage)) {
        ws_data.push([
          org,
          shortToDisplay[org],
          (await tcOrgs).includes(org),
          locale,
          cldrLoad.getLocaleName(locale),
          level.toLowerCase(),
        ]);
      }
    }

    const ws = XLSX.utils.aoa_to_sheet(ws_data);

    cldrXlsx.pushComment(ws, { r: 0, c: 0 }, "Organization short name");
    cldrXlsx.pushComment(ws, { r: 0, c: 1 }, "Organization long name");
    cldrXlsx.pushComment(ws, { r: 0, c: 2 }, "true if TC organization");
    cldrXlsx.pushComment(ws, { r: 0, c: 3 }, "locale id");
    cldrXlsx.pushComment(ws, { r: 0, c: 4 }, "locale name");
    cldrXlsx.pushComment(ws, { r: 0, c: 5 }, "coverage goal from Locales.txt");

    XLSX.utils.book_append_sheet(wb, ws, "Coverage");
  }
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
  div.append(
    $("<p/>", {
      text: cldrText.get("vetting_participationGuidance"),
    })
  );
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
        cldrNotify.exception(err, `Downloading Vetting Participation`);
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
  let columnNumber = 0;
  for (let col of COLUMNS) {
    const cell = XLSX.utils.encode_cell({ r: 0, c: columnNumber }); // A1, B1, C1, ...
    // include title with comment, for convenience if column is narrower than title
    cldrXlsx.pushComment(ws, cell, col.title + ": " + col.comment);
    ++columnNumber;
  }
}

function getVoteTypes(row, columnIndex, typeCount) {
  if (typeCount) {
    let nonDirect = 0;
    for (let key of Object.keys(VOTE_TYPES)) {
      if (key === "NON_DIRECT") continue; // not a type
      const title = VOTE_TYPES[key];
      row[columnIndex[title]] = typeCount[key] || 0;
      if (key !== "DIRECT") nonDirect += typeCount[key] || 0;
    }
    row[columnIndex[VOTE_TYPES.NON_DIRECT]] = nonDirect;
    for (let key of Object.keys(typeCount)) {
      if (!VOTE_TYPES[key]) {
        console.warn("Unrecognized vote type in server response: " + key);
      }
    }
  }
}

function verb(u, theUserBox) {
  const user = u.user;
  if (!user) {
    console.warn("Empty user in verb");
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
  participation.forEach(({ count, locale, user, cldr_count, org_count }) => {
    const e = getLocale(locale);
    e.count += count;
    totalCount += count;
    e.participation[user] = count;
    e.org_count = org_count;
    e.cldr_count = cldr_count;
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
      console.warn("getUsersFor bad id: " + id);
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
    console.warn("myMap bad id: " + id);
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
    console.warn("mySort bad user a: " + a);
    return 0;
  }
  if (!b.user || !b.user.name) {
    console.warn("mySort bad user b: " + b);
    return 0;
  }
  return a.user.name.localeCompare(b.user.name);
}

export { load };

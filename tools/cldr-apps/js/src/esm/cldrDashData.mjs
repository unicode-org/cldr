/*
 * cldrDashData: encapsulate dashboard data.
 */
import * as cldrAjax from "./cldrAjax.mjs";
import * as cldrClient from "./cldrClient.mjs";
import * as cldrCoverage from "./cldrCoverage.mjs";
import * as cldrNotify from "./cldrNotify.mjs";
import * as cldrProgress from "./cldrProgress.mjs";
import * as cldrStatus from "./cldrStatus.mjs";
import * as cldrSurvey from "./cldrSurvey.mjs";
import * as XLSX from "xlsx";

class DashData {
  /**
   * Construct a new DashData object
   *
   * @returns the new DashData object
   */
  constructor() {
    this.entries = []; // array of DashEntry objects
    this.cats = new Set(); // set of category names, such as "Error"
    this.catSize = {}; // number of entries in each category
    this.catFirst = {}; // first entry.xpstrid in each category
    // An object whose keys are xpstrid (xpath hex IDs like "db7b4f2df0427e4"), and whose values are DashEntry objects
    this.pathIndex = {};
    this.hiddenObject = null;
  }

  addEntriesFromJson(notifications) {
    for (let catData of notifications) {
      for (let group of catData.groups) {
        for (let e of group.entries) {
          this.addEntry(catData.category, group, e);
        }
      }
    }
  }

  /**
   * Create a new DashEntry in this DashData, or update the existing entry if already present based on e.xpstrid
   *
   * @param {String} cat the category such as "Error"
   * @param {Object} group (from json)
   * @param {Object} e (entry in old format, from json)
   */
  addEntry(cat, group, e) {
    this.addCategory(cat);
    this.catSize[cat]++;
    if (!this.catFirst[cat]) {
      this.catFirst[cat] = e.xpstrid;
    }
    if (this.pathIndex[e.xpstrid]) {
      const dashEntry = this.pathIndex[e.xpstrid];
      dashEntry.addCategory(cat);
      dashEntry.setWinning(e.winning);
      if (e.comment) {
        dashEntry.setComment(e.comment);
      }
      if (e.subtype) {
        dashEntry.setSubtype(e.subtype);
      }
    } else {
      const dashEntry = new DashEntry(e.xpstrid, e.code, e.english);
      dashEntry.setSectionPageHeader(group.section, group.page, group.header);
      dashEntry.addCategory(cat);
      dashEntry.setWinning(e.winning);
      dashEntry.setPreviousEnglish(e.previousEnglish);
      dashEntry.setComment(e.comment);
      dashEntry.setSubtype(e.subtype);
      dashEntry.setChecked(this.itemIsChecked(e));
      this.entries.push(dashEntry);
      this.pathIndex[e.xpstrid] = dashEntry;
    }
  }

  /**
   * If this category is not already present in the data, add it and
   * initialize its size to zero. No effect if the category is already present.
   *
   * @param {String} cat
   */
  addCategory(cat) {
    if (!this.cats.has(cat)) {
      this.cats.add(cat);
      this.catSize[cat] = 0;
    }
  }

  /**
   * Set the object indicating which paths the user wants hidden in the dashboard
   *
   * The format matches the json currently returned by the back end. The keys are
   * "subtype" strings such as "incorrectCasing"; the values are arrays of objects
   * each of which has an xpstrid and a value string
   *
   * @param {Object} hiddenObject
   * Example of json.hidden:
   * {
    "incorrectCasing": [
      {
        "value": "région micronésienne",
        "xpstrid": "fe9015c6c61370"
      },
      {
        "value": "régions éloignées de l’Océanie",
        "xpstrid": "744c18884a1547be"
      },
      {
        "value": "pseudo-accents",
        "xpstrid": "4ef00bbec7020af2"
      }
    ],
    "none": [
      {
        "value": "iakoute",
        "xpstrid": "79262aa5d69820da"
      },
      {
        "value": "hanifi",
        "xpstrid": "e8cecebdded8d76"
      },
      {
        "value": "Monde",
        "xpstrid": "73c7c09de32184"
      }
    ]
   */
  setHidden(hiddenObject) {
    this.hiddenObject = hiddenObject;
  }

  itemIsChecked(e) {
    if (!this.hiddenObject[e.subtype]) {
      return false;
    }
    const pathValueArray = this.hiddenObject[e.subtype];
    return pathValueArray.some(
      (p) => p.xpstrid === e.xpstrid && p.value === e.winning
    );
  }

  /**
   * After the user has voted, reset attributes for the entry that might have
   * changed or disappeared as a result of voting. Also update the totals for
   * this DashData.
   */
  cleanEntry(dashEntry) {
    this.removeEntryCats(dashEntry);
    dashEntry.clean();
  }

  removeEntry(dashEntry) {
    this.removeEntryCats(dashEntry);
    const index = this.entries.indexOf(dashEntry);
    this.entries.splice(index, 1);
  }

  removeEntryCats(dashEntry) {
    dashEntry.cats.forEach((cat) => {
      this.catSize[cat]--;
      if (!this.catSize[cat]) {
        this.cats.delete(cat);
        delete this.catSize[cat];
      }
      if (this.catFirst[cat] === dashEntry.xpstrid) {
        this.findCatFirst(cat);
      }
    });
  }

  findCatFirst(cat) {
    for (let dashEntry of this.entries) {
      if (dashEntry.cat === cat) {
        this.catFirst[cat] = dashEntry;
        return;
      }
    }
    delete this.catFirst[cat];
  }
}

class DashEntry {
  /**
   * Construct a new DashEntry object
   *
   * @param {String} xpstrid the xpath hex string id like "710b6e70773e5764"
   * @param {String} code the code like "long-one-nominative"
   * @param {String} english the English value like "{0} metric pint"
   *
   * @returns the new DashEntry object
   */
  constructor(xpstrid, code, english) {
    this.xpstrid = xpstrid;
    this.code = code;
    this.english = english;
    this.cats = new Set(); // set of notification category names
    this.winning = null;
    this.previousEnglish = null;
    this.comment = null;
    this.subtype = null;
    this.checked = false;
  }

  setSectionPageHeader(section, page, header) {
    this.section = section; // e.g., "Units"
    this.page = page; // e.g., "Volume"
    this.header = header; // e.g., "pint-metric"
  }

  setWinning(winning) {
    this.winning = winning; // like "{0} pinte métrique"
  }

  setPreviousEnglish(previousEnglish) {
    this.previousEnglish = previousEnglish; // e.g., "{0} metric pint"
  }

  setComment(comment) {
    this.comment = comment; // e.g., "&lt;missing placeholders&gt; Need at least 1 placeholder(s), but only have 0. Placeholders..."
  }

  setSubtype(subtype) {
    this.subtype = subtype; // e.g., "missingPlaceholders"
  }

  setChecked(checked) {
    this.checked = checked; // boolean; the user added a checkmark for this entry
  }

  addCategory(category) {
    this.cats.add(category); // e.g., "Error", "Disputed", "English_Changed
  }

  /**
   * After the user has voted, reset attributes that might have changed
   * or disappeared as a result of voting. Any updated values for these
   * attributes will be added based on the new server response.
   */
  clean() {
    this.cats = new Set();
    this.winning = null;
    this.comment = null;
    this.subtype = null;
  }
}

let fetchErr = "";

let viewSetDataCallback = null;

function doFetch(callback) {
  viewSetDataCallback = callback;
  fetchErr = "";
  const locale = cldrStatus.getCurrentLocale();
  const level = cldrCoverage.effectiveName(locale);
  if (!locale || !level) {
    fetchErr = "Please choose a locale and a coverage level first.";
    return;
  }
  const url = `api/summary/dashboard/${locale}/${level}`;
  cldrAjax
    .doFetch(url)
    .then(cldrAjax.handleFetchErrors)
    .then((data) => data.json())
    .then(setData)
    .catch((err) => {
      const msg = "Error loading Dashboard data: " + err;
      console.error(msg);
      fetchErr = msg;
    });
}

function getFetchError() {
  return fetchErr;
}

/**
 * Set the data for the Dashboard, converting from json to a DashData object
 *
 * The json data as received from the back end is ordered by category, then by section, page, header, code, ...
 * (but those are not ordered alphabetically).
 *
 * @param json  - an object with these elements:
 *   notifications - an array of objects (locally named "catData" meaning "all the data for one category"),
 *   each having these elements:
 *     category - a string like "Error" or "English_Changed"
 *     groups - an array of objects, each having these elements:
 *       header - a string
 *       page - a string
 *       section - a string
 *       entries - an array of objects, each having these elements:
 *         code - a string
 *         english - a string
 *         old - a string (baseline value; unused?)
 *         previousEnglish - a string
 *         winning  - a string, the winning value
 *         xpstrid - a string, the xpath hex string id
 *         comment - a string
 *         subtype - a string
 *         checked - a boolean, added by makePathIndex, not in json
 *
 *   Dashboard only uses data.notifications. There are additional fields
 *   data.* used by cldrProgress for progress meters.
 *
 * @return the modified/reorganized data as a DashData object
 */
function setData(json) {
  cldrProgress.updateVoterCompletion(json);
  const newData = convertData(json);
  if (viewSetDataCallback) {
    viewSetDataCallback(newData);
  }
  return newData;
}

function convertData(json) {
  const newData = new DashData();
  newData.setHidden(json.hidden);
  newData.addEntriesFromJson(json.notifications);
  return newData;
}

/**
 * A user has voted. Update the Dashboard data and index as needed.
 *
 * Even though the json is only for one path, it may have multiple notifications,
 * with different categories such as "Warning" and "English_Changed".
 *
 * Ensure that the data gets updated for (1) each new or modified notification,
 * and (2) each obsolete notification -- if a notification for this path occurs in
 * the (old) data but not in the (new) json, it's obsolete and must be removed.
 *
 * @param dashData - the DashData (new format) for all paths, to be updated
 * @param json - the response to a request by cldrTable.refreshSingleRow,
 *               containing notifications for a single path (old format)
 */
function updatePath(dashData, json) {
  try {
    if (json.xpstrid in dashData.pathIndex) {
      // We already have an entry for this path
      const dashEntry = dashData.pathIndex[json.xpstrid];
      if (!json.notifications?.length) {
        // The path no longer has any notifications, so remove the entry
        dashData.removeEntry(dashEntry);
        return dashData; // for unit test
      }
      // Clear attributes that might have changed or disappeared as a result of voting.
      // They will be updated/restored from the json.
      dashData.cleanEntry(dashEntry);
    }
    dashData.addEntriesFromJson(json.notifications);
  } catch (e) {
    cldrNotify.exception(e, "updating path for Dashboard");
  }
  return dashData; // for unit test
}

/**
 * Save the checkbox setting to the back end database for locale+xpstrid+value+subtype,
 * as a preference of the currrent user
 *
 * @param checked - boolean, currently unused since back end toggles existing preference
 * @param entry - the entry
 * @param locale - the locale string such as "am" for Amharic
 */
function saveEntryCheckmark(checked, entry, locale) {
  const url = getCheckmarkUrl(entry, locale);
  cldrAjax.doFetch(url).catch((err) => {
    console.error("Error setting dashboard checkmark preference: " + err);
  });
}

function getCheckmarkUrl(entry, locale) {
  const p = new URLSearchParams();
  p.append("what", "dash_hide"); // cf. WHAT_DASH_HIDE in SurveyAjax.java
  p.append("xpstrid", entry.xpstrid);
  p.append("value", entry.winning);
  p.append("subtype", entry.subtype);
  p.append("locale", locale);
  p.append("s", cldrStatus.getSessionId());
  p.append("cacheKill", cldrSurvey.cacheBuster());
  return cldrAjax.makeUrl(p);
}

/**
 * Download as XLSX spreadsheet
 *
 * @param {Object} data processed data to download
 * @param {String} locale locale id
 * @param {Function} cb takes one argument 'status': if falsy, done. otherwise, string value
 */
async function downloadXlsx(data, locale, cb) {
  const xpathMap = cldrSurvey.getXpathMap();
  const { coverageLevel, entries } = data;
  const coverageLevelName = (coverageLevel || "default").toLowerCase();
  const xlsSheetName = `${locale}.${coverageLevelName}`;
  const xlsFileName = `Dash_${locale}_${coverageLevelName}.xlsx`;

  // Fetch all XPaths in parallel since it'll take a while
  cb(`Loading rows…`);
  const allXpaths = [];
  for (let dashEntry of entries) {
    if (dashEntry.section === "Reports") {
      continue; // skip this
    }
    allXpaths.push(dashEntry.xpstrid);
    try {
      await xpathMap.get(dashEntry.xpstrid);
    } catch (e) {
      throw Error(
        `${e}:  Could not load XPath for ${JSON.stringify(dashEntry)}`
      );
    }
  }
  cb(`Loading xpaths…`);
  await Promise.all(allXpaths.map((x) => xpathMap.get(x)));
  cb(`Calculating…`);

  // Now we can expect that xpathMap can return immediately.

  const ws_data = [
    [
      "Category",
      "Header",
      "Page",
      "Section",
      "Code",
      "English",
      "Old",
      "Subtype",
      "Winning",
      "Xpstr", // hex - to hide
      "XPath", // fetched
      "URL", // formula
    ],
  ];

  for (let e of entries) {
    async function getXpath() {
      if (e.section === "Reports") return "-";
      try {
        return (await xpathMap.get(e.xpstrid)).path;
      } catch (ex) {
        throw Error(`${e}: Could not get xpath of ${JSON.stringify(e)}`);
      }
    }
    const xpath = await getXpath();
    const url = `https://st.unicode.org/cldr-apps/v#/${e.locale}/${e.page}/${e.xpstrid}`;
    const cats = Array.from(e.cats).join(", ");
    ws_data.push([
      cats,
      e.header,
      e.page,
      e.section,
      e.code,
      e.english,
      e.old,
      e.subtype,
      e.winning,
      e.xpstrid,
      xpath,
      url,
    ]);
  }
  const ws = XLSX.utils.aoa_to_sheet(ws_data);
  // cldrXlsx.pushComment(ws, "C1", `As of ${new Date().toISOString()}`);
  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, xlsSheetName);
  cb(`Writing…`);
  XLSX.writeFile(wb, xlsFileName);
  cb(null);
}

/**
 * @param {string} locale locale to list for
 * @returns {Array<CheckStatusSummary>}
 */
async function getLocaleErrors(locale) {
  const client = cldrClient.getClient();
  return await client.apis.voting.getLocaleErrors({ locale });
}

export {
  doFetch,
  downloadXlsx,
  getFetchError,
  getLocaleErrors,
  saveEntryCheckmark,
  setData,
  updatePath,
};

/*
 * cldrDash: encapsulate dashboard data.
 */
import * as cldrAjax from "./cldrAjax.js";
import * as cldrStatus from "./cldrStatus.js";
import * as cldrSurvey from "./cldrSurvey.js";

/**
 * An object whose keys are xpath hex IDs, and whose values are objects whose keys are notification categories
 * such as "Error" or "English_Changed" and values are "entry" objects with code, english, ..., xpath elements.
 *
 * There can be at most one item for an xpath within each category, but the same xpath may have items in
 * multiple categories. For example, the xpath "db7b4f2df0427e4" might have both "Error" and "Warning" items,
 * but there can't be more than one "Error" item for the same xpath, since the server will have combined them
 * into a single "Error" item with multiple error messages.
 */
let xpathIndex = {};

/**
 * Set the data for the Dashboard, add totals, and index it.
 *
 * The data as received from the back end is ordered by category, then by section, page, header, code, ...
 * (but those are not ordered alphabetically). It is presented to the user in that same order.
 *
 * @param data  - an object with these elements:
 *   notifications - an array of objects, each having these elements:
 *     notification - a category string like "Error" or "English_Changed"
 *     total - an integer (number of entries-within-entries), added by addCounts, not in json
 *     entries - an array of objects, each having these elements:
 *       header - a string
 *       page - a string
 *       section - a string
 *       entries - an array of objects, each having these elements:
 *         code - a string
 *         english - a string
 *         old - a string (baseline value; unused?)
 *         previousEnglish - a string
 *         winning  - a string
 *         xpath - a string
 *         comment - a string
 *         checked - a boolean, added by makeXpathIndex, not in json
 *
 * @return the modified data (with totals, etc., added)
 *
 * TODO: if the user chose a different locale while waiting for data,
 * don't show the dashboard for the old locale! This may be complicated
 * if multiple dashboard requests are overlapping -- ideally should tell
 * the back end to stop working on out-dated requests
 */
function setData(data) {
  addCounts(data);
  makeXpathIndex(data);
  return data;
}

/**
 * Calculate total counts; modify data by adding "total" for each category
 *
 * @param data
 */
function addCounts(data) {
  for (let e in data.notifications) {
    const n = data.notifications[e];
    n.total = 0;
    for (let g in n.entries) {
      n.total += n.entries[g].entries.length;
    }
  }
}

/**
 * Create the index; also set checked = true/false for all entries
 *
 * @param data
 */
function makeXpathIndex(data) {
  xpathIndex = {};
  for (let j in data.notifications) {
    const n = data.notifications[j];
    for (let g in n.entries) {
      for (let k in n.entries[g].entries) {
        const e = n.entries[g].entries[k];
        e.checked = itemIsChecked(data, e.xpath, n.notification);
        if (!xpathIndex[e.xpath]) {
          xpathIndex[e.xpath] = {};
        }
        if (xpathIndex[e.xpath][n.notification]) {
          console.error(
            "Duplicate in makeXpathIndex: " + e.xpath + ", " + n.notification
          );
        }
        xpathIndex[e.xpath][n.notification] = e;
      }
    }
  }
}

function itemIsChecked(data, xpath, category) {
  if (!data.hidden || !data.hidden[category]) {
    return false;
  }
  return data.hidden[category].includes(xpath);
}

/**
 * A user has voted. Update the Dashboard data and index as needed.
 *
 * @param data - the Dashboard data
 * @param json - the response to a request by cldrTable.refreshSingleRow
 *
 * Unfortunately, the Dashboard data and the row json are formatted completely differently.
 * json.issues contains category strings only, like ["Error", "Warning"].
 */
function updateRow(data, json) {
  try {
    for (let key in json.section.rows) {
      const row = json.section.rows[key];
      const xpath = row.xpstrid;
      const oldIssues =
        xpath in xpathIndex ? Object.keys(xpathIndex[xpath]).sort() : [];
      const newIssues = json.issues.sort();
      oldIssues.forEach((issue) => {
        const catData = getNotificationCategory(data, issue);
        if (newIssues.includes(issue)) {
          updateNotification(catData, xpath, issue, row);
        } else {
          removeNotification(catData, xpath, issue);
        }
      });
      newIssues.forEach((issue) => {
        if (!oldIssues.includes(issue)) {
          const catData = getNotificationCategory(data, issue);
          addNotification(catData, xpath, issue, row);
        }
      });
    }
  } catch (error) {
    console.error("Caught error in updateRow: " + error);
  }
}

function updateNotification(catData, xpath, issue, row) {
  for (let g in catData.entries) {
    const entries = catData.entries[g].entries;
    for (let k in entries) {
      if (entries[k].xpath === xpath) {
        const entry = entries[k];
        entry.winning = row.winningValue;
        entry.english = row.displayName;
        entry.comment = constructMessageFromRow(row, issue);
        return;
      }
    }
  }
}

function removeNotification(catData, xpath, issue) {
  for (let g in catData.entries) {
    const entries = catData.entries[g].entries;
    for (let k in entries) {
      if (entries[k].xpath === xpath) {
        entries.splice(k, 1);
        --catData.total;
        delete xpathIndex[xpath][issue];
        return;
      }
    }
  }
}

function addNotification(catData, xpath, issue, row) {
  const e = {
    xpath: xpath,
    code: row.code,
    winning: row.winningValue,
    english: row.displayName,
    comment: constructMessageFromRow(row, issue),
  };
  const ph = getPathHeader(xpath);
  const a = {
    header: ph.header,
    page: ph.page,
    section: ph.section,
    entries: [e],
  };
  catData.entries.push(a);
  catData.total++;
  if (!(xpath in xpathIndex)) {
    xpathIndex[xpath] = {};
  }
  xpathIndex[xpath][issue] = e;
}

function constructMessageFromRow(row, issue) {
  let message = "";
  row.items[row.winningVhash].tests.forEach((t) => {
    if (t.type === issue) {
      message += t.message + "<br />";
    }
  });
  return message;
}

function getNotificationCategory(data, issue) {
  const notifications = data.notifications;
  let i = null;
  for (let j in notifications) {
    if (issue === notifications[j].notification) {
      i = j;
      break;
    }
  }
  if (i === null) {
    const obj = {
      notification: issue,
      total: 0,
      entries: [],
    };
    notifications.push(obj); // TODO: insert in a particular order?
    i = notifications.length - 1;
  }
  return notifications[i];
}

function getPathHeader(xpstrid) {
  const xpathMap = cldrSurvey.getXpathMap();
  const result = xpathMap.getImmediately({
    hex: xpstrid,
  });
  if (result && result.ph) {
    // success
    return result.ph;
  }
  // failure; fall back on dummy header, and current section/page
  console.error("No path header from xpath map, using fallback");
  return {
    header: "[header]",
    page: cldrStatus.getCurrentPage(),
    section: cldrStatus.getCurrentSection(),
  };
}

/**
 * Save the checkbox setting to the back end database for a particular xpath/category,
 * as preference of the currrent user
 *
 * @param checked - boolean, currently unused since back end toggles existing preference
 * @param xpath - the hex xpath id
 * @param category - the notification cateogry string like "ERROR" or "ENGLISH_CHANGED"
 * @param locale - the locale string such as "am" for Amharic
 */
function saveEntryCheckmark(checked, xpath, category, locale) {
  const url = getCheckmarkUrl(xpath, category, locale);
  cldrAjax.doFetch(url).catch((err) => {
    console.error("Error setting dashboard checkmark preference: " + err);
  });
}

function getCheckmarkUrl(xpath, category, locale) {
  const p = new URLSearchParams();
  p.append("what", "dash_hide"); // cf. WHAT_DASH_HIDE in SurveyAjax.java
  p.append("path", xpath);
  p.append("choice", category);
  p.append("locale", locale);
  p.append("s", cldrStatus.getSessionId());
  p.append("cacheKill", cldrSurvey.cacheBuster());
  return cldrAjax.makeUrl(p);
}

export { saveEntryCheckmark, setData, updateRow };

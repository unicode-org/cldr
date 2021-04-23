/*
 * cldrDash: encapsulate dashboard data.
 */
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
 *     total - an integer (number of entries-within-entries; added by addCounts(), not in json)
 *     entries - an array of objects, each having these elements:
 *       header - a string
 *       page - a string
 *       section - a string
 *       entries - an array of objects, each having these elements, all strings:
 *         code, english, old, previousEnglish, winning, xpath, comment
 *
 * @return the modified data (with totals added)
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
 * Create the index
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
    json.section.rows.forEach((row) => {
      const xpath = row.xpstrid;
      const oldIssues =
        xpath in xpathIndex ? Object.keys(xpathIndex[xpath]).sort() : [];
      const newIssues = json.issues.sort();
      oldIssues.forEach((issue) => {
        if (newIssues.includes(issue)) {
          updateNotification(data, xpath, issue, row);
        } else {
          removeNotification(data, xpath, issue);
        }
      });
      newIssues.forEach((issue) => {
        if (!oldIssues.includes(issue)) {
          addNotification(data, xpath, issue, row);
        }
      });
    });
  } catch (error) {
    console.error("Caught error in updateRow: " + error);
  }
}

function updateNotification(data, xpath, issue, row) {
  // TODO: modify the existing item in-place; that will be  more efficient, and will
  // help to keep the items in their proper order, which is currently determined by the server
  removeNotification(data, xpath, issue);
  addNotification(data, xpath, issue, row);
}

function removeNotification(data, xpath, issue) {
  for (let j in data.notifications) {
    const n = data.notifications[j];
    if (issue === n.notification) {
      for (let g in n.entries) {
        const entries = n.entries[g].entries;
        for (let k in entries) {
          if (entries[k].xpath === xpath) {
            entries.splice(k, 1);
            if (--n.total === 0) {
              delete data.notifications[j];
            }
            delete xpathIndex[xpath][issue];
            return;
          }
        }
      }
    }
  }
}

function addNotification(data, xpath, issue, row) {
  const n = getNotificationCategory(data, issue);
  let comment = "";
  row.items[row.winningVhash].tests.forEach((t) => {
    if (t.type === issue) {
      comment += t.message + "<br />";
    }
  });
  const e = {
    xpath: xpath,
    code: row.code,
    winning: row.winningValue,
    english: row.displayName,
    comment: comment,
  };
  const ph = getPathHeader(xpath);
  const a = {
    header: ph.header,
    page: ph.page,
    section: ph.section,
    entries: [e],
  };
  n.entries.push(a);
  n.total++;
  console.log("addNotification added " + issue + ", new total = " + n.total);
  if (!(xpath in xpathIndex)) {
    xpathIndex[xpath] = {};
  }
  xpathIndex[xpath][issue] = e;
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
  return {
    header: "[header]",
    page: cldrStatus.getCurrentPage(),
    section: cldrStatus.getCurrentSection(),
  };
}

export { setData, updateRow };

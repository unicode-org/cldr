/*
 * cldrDash: encapsulate dashboard data.
 */
import * as cldrAjax from "./cldrAjax.js";
import * as cldrProgress from "./cldrProgress.js";
import * as cldrStatus from "./cldrStatus.js";
import * as cldrSurvey from "./cldrSurvey.js";

/**
 * An object whose keys are xpstrid (xpath hex IDs like "db7b4f2df0427e4"), and whose values are objects whose
 * keys are notification categories such as "Error" or "English_Changed" and values are "entry"
 * objects with code, english, ..., xpstrid, ... elements.
 *
 * Example: pathIndex[xpstrid][category] = entry
 *
 * There can be at most one notification for a path within each category, but the same path may have notifications in
 * multiple categories. For example, the path with xpstrid = "db7b4f2df0427e4" might have both "Error" and "Warning" notifications,
 * but there can't be more than one "Error" notification for the same path, since the server will have combined them
 * into a single "Error" notification with multiple error messages.
 */
let pathIndex = {};

/**
 * Set the data for the Dashboard, add "total" and "checked" fields, and index it.
 *
 * The json data as received from the back end is ordered by category, then by section, page, header, code, ...
 * (but those are not ordered alphabetically). It is presented to the user in that same order.
 *
 * @param data  - an object with these elements:
 *   notifications - an array of objects (locally named "catData" meaning "all the data for one category"),
 *   each having these elements:
 *     category - a string like "Error" or "English_Changed"
 *     total - an integer (number of entries in this category), added by addCounts, not in json
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
 * @return the modified data (with totals, etc., added)
 */
function setData(data) {
  cldrProgress.updateVoterCompletion(data);
  addCounts(data);
  makePathIndex(data);
  return data;
}

/**
 * Calculate total counts; modify data by adding "total" for each category
 *
 * @param data
 */
function addCounts(data) {
  for (let catData of data.notifications) {
    catData.total = 0;
    for (let group of catData.groups) {
      catData.total += group.entries.length;
    }
  }
}

/**
 * Create the index; also set checked = true/false for all entries
 *
 * @param data
 */
function makePathIndex(data) {
  pathIndex = {};
  for (let catData of data.notifications) {
    for (let group of catData.groups) {
      for (let entry of group.entries) {
        entry.checked = itemIsChecked(data, entry);
        if (!pathIndex[entry.xpstrid]) {
          pathIndex[entry.xpstrid] = {};
        } else if (pathIndex[entry.xpstrid][catData.category]) {
          console.error(
            "Duplicate in makePathIndex: " +
              entry.xpstrid +
              ", " +
              catData.category
          );
        }
        pathIndex[entry.xpstrid][catData.category] = entry;
      }
    }
  }
}

function itemIsChecked(data, entry) {
  if (!data.hidden || !data.hidden[entry.subtype]) {
    return false;
  }
  const pathValueArray = data.hidden[entry.subtype];
  return pathValueArray.some(
    (p) => p.xpstrid === entry.xpstrid && p.value === entry.winning
  );
}

/**
 * A user has voted. Update the Dashboard data and index as needed.
 *
 * Even though the json is only for one path, it may have multiple notifications,
 * with different categories such as "Warning" and "English_Changed",
 * affecting multiple Dashboard rows.
 *
 * Ensure that the data gets updated for (1) each new or modified notification,
 * and (2) each obsolete notification -- if a notification for this path occurs in
 * the (old) data but not in the (new) json, it's obsolete and must be removed.
 *
 * @param data - the Dashboard data for all paths, to be updated
 * @param json - the response to a request by cldrTable.refreshSingleRow,
 *               containing notifications for a single path
 */
function updatePath(data, json) {
  try {
    const updater = newPathUpdater(data, json);
    updater.oldCategories.forEach((category) => {
      if (updater.newCategories.includes(category)) {
        updateEntry(updater, category);
      } else {
        removeEntry(updater, category);
      }
    });
    updater.newCategories.forEach((category) => {
      if (!updater.oldCategories.includes(category)) {
        addEntry(updater, category);
      }
    });
  } catch (error) {
    console.error("Caught error in updatePath: " + error);
  }
  return data; // for unit test
}

function newPathUpdater(data, json) {
  const updater = {
    data: data,
    json: json,
    xpstrid: getSinglePathFromUpdate(json),
    oldCategories: [],
    newCategories: [],
    group: null,
  };
  for (let catData of json.notifications) {
    updater.newCategories.push(catData.category);
  }
  updater.newCategories = updater.newCategories.sort();
  if (updater.xpstrid in pathIndex) {
    updater.oldCategories = Object.keys(pathIndex[updater.xpstrid]).sort();
  }
  return updater;
}

function getSinglePathFromUpdate(json) {
  for (let row of Object.values(json.section.rows)) {
    if (row.xpstrid) {
      return row.xpstrid;
    }
  }
  throw "Missing path in getSinglePathFromUpdate";
}

function updateEntry(updater, category) {
  const catData = getDataForCategory(updater.data, category);
  for (let group of catData.groups) {
    const entries = group.entries;
    for (let i in entries) {
      if (entries[i].xpstrid === updater.xpstrid) {
        const newEntry = getNewEntry(updater, category);
        pathIndex[updater.xpstrid][category] = entries[i] = newEntry;
        return;
      }
    }
  }
}

function removeEntry(updater, category) {
  const catData = getDataForCategory(updater.data, category);
  for (let group of catData.groups) {
    const entries = group.entries;
    for (let i in entries) {
      if (entries[i].xpstrid === updater.xpstrid) {
        entries.splice(i, 1);
        --catData.total;
        delete pathIndex[updater.xpstrid][category];
        return;
      }
    }
  }
}

function addEntry(updater, category) {
  const newEntry = getNewEntry(updater, category); // sets updater.group
  const catData = getDataForCategory(updater.data, category);
  const group = getMatchingGroup(catData, updater.group);
  // TODO: insert in a particular order; see https://unicode-org.atlassian.net/browse/CLDR-15202
  group.entries.push(newEntry);
  catData.total++;
  if (!(updater.xpstrid in pathIndex)) {
    pathIndex[updater.xpstrid] = {};
  }
  pathIndex[updater.xpstrid][category] = newEntry;
}

function getNewEntry(updater, category) {
  for (let catData of updater.json.notifications) {
    if (catData.category === category) {
      for (let group of catData.groups) {
        for (let entry of group.entries) {
          if (entry.xpstrid === updater.xpstrid) {
            updater.group = group;
            entry.checked = itemIsChecked(updater.data, entry);
            return entry;
          }
        }
      }
    }
  }
  return null;
}

function getMatchingGroup(catData, groupToMatch) {
  for (let group of catData.groups) {
    if (groupsMatch(group, groupToMatch)) {
      return group;
    }
  }
  const newGroup = cloneGroup(groupToMatch);
  // TODO: insert in a particular order; see https://unicode-org.atlassian.net/browse/CLDR-15202
  catData.groups.push(newGroup);
  return newGroup;
}

function groupsMatch(groupA, groupB) {
  return (
    groupA.header === groupB.header &&
    groupA.page === groupB.page &&
    groupA.section === groupB.section
  );
}

function cloneGroup(group) {
  const newGroup = {
    header: group.header,
    page: group.page,
    section: group.section,
    entries: [],
  };
  return newGroup;
}

function getDataForCategory(data, category) {
  for (let catData of data.notifications) {
    if (catData.category === category) {
      return catData;
    }
  }
  const newCatData = {
    category: category,
    total: 0,
    groups: [],
  };
  // TODO: insert in a particular order; see https://unicode-org.atlassian.net/browse/CLDR-15202
  data.notifications.push(newCatData);
  return newCatData;
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

export { saveEntryCheckmark, setData, updatePath };

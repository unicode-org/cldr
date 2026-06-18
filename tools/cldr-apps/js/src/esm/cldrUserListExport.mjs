import * as XLSX from "xlsx";
import * as cldrAjax from "./cldrAjax.mjs";
import * as cldrCoverage from "./cldrCoverage.mjs";
import * as cldrProgress from "./cldrProgress.mjs";
import * as cldrXlsx from "./cldrXlsx.mjs";

// shim global fetch
const fetch = cldrAjax.doFetch;
const limit = 16_777_216; // big limit

async function fetchUserActivity(userId) {
  const url = `SurveyAjax?what=recent_items&user=${userId}&limit=${limit}`;
  const result = await fetch(url, {
    headers: {
      Accept: "application/json",
    },
  });
  const json = await result.json();
  const { data, header } = json.recent;

  return { data, header };
}

async function extractDataRow(r, header) {
  const localeName = r[header.LOCALE_NAME];
  const xpathId = r[header.XPATH_STRHASH];
  const locale = r[header.LOCALE];
  // fetch coverage if not present
  const coverage =
    (await cldrCoverage.getCoverageForPath(locale, xpathId)) || "unknown";
  const xpathCode = r[header.XPATH_CODE];
  const value = r[header.VALUE];
  const lastMod = new Date(r[header.LAST_MOD]); // TODO: convert to 'date'
  const surveyUrl = cldrXlsx.getSurveyUrl(locale, xpathId, null);

  return {
    localeName,
    xpathId,
    locale,
    coverage,
    xpathCode,
    value,
    lastMod,
    surveyUrl,
  };
}

/**
 * Download the user activity database
 */
async function downloadUserActivity(userId /*, session*/) {
  const { data, header } = await fetchUserActivity(userId);
  const wb = XLSX.utils.book_new();

  var ws_name = `SurveyTool#${userId}`;

  /* make worksheet */
  var ws_data = [
    [
      "Locale", // 0
      "XpathId", // 1
      "XpathCode", // 2
      "Value", // 3
      "When", // 4
      "URL", // 5
      "Coverage", // 6
    ],
  ];
  for (const r of data) {
    const {
      localeName,
      xpathId,
      xpathCode,
      value,
      lastMod,
      surveyUrl,
      coverage,
    } = await extractDataRow(r, header);
    ws_data.push([
      localeName,
      xpathId,
      xpathCode,
      value,
      lastMod,
      surveyUrl,
      coverage,
    ]);
  }
  var ws = XLSX.utils.aoa_to_sheet(ws_data);

  cldrXlsx.pushComment(ws, "A1", "Locale Name in English");
  cldrXlsx.pushComment(ws, "B1", "XPath String ID");
  cldrXlsx.pushComment(ws, "C1", "XPath Code");
  // D1 = Value
  cldrXlsx.pushComment(ws, "E1", "Date of last vote");

  XLSX.utils.book_append_sheet(wb, ws, ws_name);
  XLSX.writeFile(wb, `survey_recent_activity${userId}.xlsx`);
}

/**
 *
 * @param {Object} users - array with id, email, etc
 * @param {Function} callback called with (msg,percent)
 */
async function downloadAllUserActivity(users, callback) {
  const allToFetch = users.length;
  const wb = XLSX.utils.book_new();

  var ws_name = `SurveyToolAllUsers`;

  /* make worksheet */
  var ws_data = [
    [
      "UserId",
      "Email",
      "Org",
      "UserLevel",
      "LocaleId",
      "Locale",
      "XpathId",
      "XpathCode",
      "Value",
      "When",
      "URL",
      "Coverage",
    ],
  ];
  let fetched = 0;
  for (const { id: userId, email, org, userLevelName } of users) {
    const fetchPercent = cldrProgress.friendlyPercent(fetched, allToFetch);
    fetched++;
    callback(
      `Fetching #${userId}: ${fetched}/${allToFetch}, ${ws_data.length} rows`,
      fetchPercent
    );
    const { data, header } = await fetchUserActivity(userId);
    for (const r of data) {
      const {
        locale,
        localeName,
        xpathId,
        xpathCode,
        value,
        lastMod,
        surveyUrl,
        coverage,
      } = await extractDataRow(r, header);
      ws_data.push([
        userId,
        email,
        org,
        userLevelName,
        locale,
        localeName,
        xpathId,
        xpathCode,
        value,
        lastMod,
        surveyUrl,
        coverage,
      ]);
    }
  }
  var ws = XLSX.utils.aoa_to_sheet(ws_data);

  XLSX.utils.book_append_sheet(wb, ws, ws_name);
  XLSX.writeFile(wb, `${ws_name}.xlsx`, { compression: true });
  callback(`Wrote ${ws_name} with ${users.length} users`, 100);
}

export { downloadUserActivity, downloadAllUserActivity };

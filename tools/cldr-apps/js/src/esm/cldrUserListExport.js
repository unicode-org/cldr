import XLSX from "xlsx";
import * as cldrAjax from "./cldrAjax.js";
// shim global fetch
const fetch = cldrAjax.doFetch;

/**
 * Download the user activity database
 */
async function downloadUserActivity(userId /*, session*/) {
  //    http://127.0.0.1:9080/cldr-apps/SurveyAjax?what=recent_items&user=1289&limit=16777216
  const limit = 16_777_216; // big limit
  // const limit = 3; // testing
  const url = `SurveyAjax?what=recent_items&user=${userId}&limit=${limit}`;
  const result = await fetch(url, {
    headers: {
      Accept: "application/json",
    },
  });
  const json = await result.json();
  const { data, header } = json.recent;

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
    ],
  ];
  for (const r of data) {
    ws_data.push([
      r[header.LOCALE_NAME],
      r[header.XPATH_STRHASH],
      r[header.XPATH_CODE],
      r[header.VALUE],
      new Date(r[header.LAST_MOD]), // TODO: convert to 'date'
    ]);
  }
  var ws = XLSX.utils.aoa_to_sheet(ws_data);

  function pushComment(where, t) {
    ws[where].c = ws[where].c || [];
    ws[where].c.hidden = true;
    ws[where].c.push({ a: "SurveyTool", t });
  }
  pushComment("A1", "Locale Name in English");
  pushComment("B1", "XPath String ID");
  pushComment("C1", "XPath Code");
  // D1 = Value
  pushComment("E1", "Date of last vote");

  XLSX.utils.book_append_sheet(wb, ws, ws_name);
  XLSX.writeFile(wb, `survey_recent_activity${userId}.xlsx`);
}

export { downloadUserActivity };

/**
 * cldrReport: common functions for reports (r_*)
 */
import * as cldrClient from "./cldrClient.js";
import * as cldrDom from "./cldrDom.js";
import * as cldrGui from "./cldrGui.js";
import * as cldrLoad from "./cldrLoad.js";
import * as cldrSurvey from "./cldrSurvey.js";
import * as cldrText from "./cldrText.js";
import * as cldrVueRouter from "../cldrVueRouter.js";
import ReportResponse from "../views/ReportResponse.vue";
import XLSX from "xlsx";

let lastrr = null;

function reportLoadHandler(html, report) {
  cldrSurvey.hideLoader();
  cldrLoad.setLoading(false);
  const frag = window.document.createDocumentFragment();
  const div = window.document.createElement("div");
  frag.appendChild(div);
  const rr = cldrVueRouter.createCldrApp(ReportResponse, "n/a", { report });

  frag.appendChild(cldrDom.construct(html)); // add the rest of the report
  cldrLoad.flipToOtherDiv(frag);
  // Now, mount the ReportResponse. We can't mount it while it's invisible
  rr.mount(div);
  if (lastrr) {
    // unmount the last mounted report response.
    // We don't have a better place to unmount (no 'unload' path in cldrLoad),
    // however this will keep there from being more than 1 mounted response at any time.
    lastrr.unmount();
  }
  lastrr = rr;
  cldrGui.hideRightPanel();
}

function reportName(report) {
  return cldrText.get(`special_r_${report}`);
}

function reportClass(completed, acceptable) {
  if (completed && acceptable) {
    return "d-dr-approved";
  } else if (completed && !acceptable) {
    return "d-dr-contributed";
  } else {
    return "d-dr-missing";
  }
}

async function internalFetchAllReports(user) {
  const client = await cldrClient.getClient();
  const raw = (
    await client.apis.voting.getAllReports({
      user: user || "-",
    })
  ).body;
  return raw;
}

async function fetchAllReports(user) {
  const raw = await internalFetchAllReports(user);
  const types = await reportTypes();
  const byLocale = {};

  // collect all data
  raw.forEach(({ id, statuses }) => {
    for (const [locale, { acceptable, completed }] of Object.entries(
      statuses
    )) {
      const e = (byLocale[locale] = byLocale[locale] || {
        acceptable: 0,
        unacceptable: 0,
        totalVoters: 0,
        byReport: {},
      });
      e.totalVoters += 1;
      e.acceptable += acceptable?.length;
      e.unacceptable += completed?.length - acceptable?.length;

      // now the per-type fields
      for (const t of types) {
        e.byReport[t] = { acceptable: 0, unacceptable: 0, totalVoters: 0 };
      }
      acceptable.forEach((t) => {
        e.byReport[t].acceptable++;
        e.byReport[t].totalVoters++;
      });
      completed.forEach((t) => {
        if (acceptable.indexOf(t) !== -1) {
          e.byReport[t].unacceptable++;
          e.byReport[t].totalVoters++;
        }
      });
    }
  });

  return {
    types,
    raw,
    byLocale,
  };
}

/**
 * Get one locale's status
 * @param {String} locale
 * @returns
 */
async function getOneLocaleStatus(locale) {
  if (locale === "-") {
    throw Error(
      "Please call client.apis.voting.getReportLocaleStatus() directly with the “-” parameter."
    );
  }
  const client = await cldrClient.getClient();
  const { obj } = await client.apis.voting.getReportLocaleStatus({
    locale,
  });
  if (obj.locales.length !== 1) {
    throw Error(
      `getOneLocaleStatus(${locale}) expected an array of one item but got ${obj.locales.length}`
    );
  }
  return obj.locales[0].reports;
}

/**
 * Get a single report in a single locale
 * @param {String} locale such as zh
 * @param {String} report  such as compact
 * @returns
 */
async function getOneReportLocaleStatus(locale, onlyReport) {
  const reports = await getOneLocaleStatus(locale);
  const myReport = reports.filter(({ report }) => report === onlyReport)[0];
  return myReport;
}

/**
 * Get the report types
 * @returns an array of report types, sorted ['a','b','c']
 */
async function reportTypes() {
  const client = await cldrClient.getClient();
  return (await client.apis.voting.listReports()).body.sort();
}

async function downloadAllReports(user) {
  const { types, raw, byLocale } = await fetchAllReports(user);

  const wb = XLSX.utils.book_new();

  var ws_name = `SurveyTool Report on Reports ${user}`;

  /* make worksheet */
  var ws_data = [
    [
      "Locale", // 0
      "Code", // 1
      "Completion", // 2
      "Acceptable", // 3
      "Unacceptable", // 4
      "Total Votes",
    ],
  ];
  // dynamic columns
  for (const t of types) {
    const n = reportName(t);
    ws_data[0].push(`${n}-Acceptable`);
    ws_data[0].push(`${n}-Unacceptable`);
    ws_data[0].push(`${n}-Total`);
  }

  // for (const r of data) {
  //   ws_data.push([
  //     r[header.LOCALE_NAME],
  //     r[header.XPATH_STRHASH],
  //     r[header.XPATH_CODE],
  //     r[header.VALUE],
  //     new Date(r[header.LAST_MOD]), // TODO: convert to 'date'
  //   ]);
  // }

  // Now, push the rest of the data
  for (const loc of Object.keys(byLocale).sort()) {
    const { acceptable, unacceptable, totalVoters, byReport } = byLocale[loc];
    const row = [];
    row.push(loc);
    row.push(cldrLoad.getLocaleName(loc));
    row.push("?%");
    row.push(acceptable);
    row.push(unacceptable);
    row.push(totalVoters);

    for (const t of types) {
      // important: in same order as header!
      row.push(byReport[t].acceptable);
      row.push(byReport[t].unacceptable);
      row.push(byReport[t].totalVoters);
    }
    // todo: byReport

    ws_data.push(row);
  }

  var ws = XLSX.utils.aoa_to_sheet(ws_data);

  function pushComment(where, t) {
    ws[where].c = ws[where].c || [];
    ws[where].c.hidden = true;
    ws[where].c.push({ a: "SurveyTool", t });
  }
  pushComment("C1", `As of ${new Date().toISOString()}`);

  XLSX.utils.book_append_sheet(wb, ws, ws_name);
  XLSX.writeFile(wb, `survey_reports_${user}.xlsx`);
}

export {
  downloadAllReports,
  fetchAllReports,
  getOneLocaleStatus,
  getOneReportLocaleStatus,
  reportClass,
  reportLoadHandler,
  reportName,
  reportTypes,
};

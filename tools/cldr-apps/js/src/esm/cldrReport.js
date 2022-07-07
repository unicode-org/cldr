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
import * as cldrXlsx from "./cldrXlsx.js";
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

async function internalFetchAllReportStatus() {
  const client = await cldrClient.getClient();
  const raw = (
    await client.apis.voting.getReportLocaleStatus({
      locale: "-",
    })
  ).body;
  return raw;
}

async function fetchAllReports() {
  const { locales } = await internalFetchAllReportStatus();
  const types = await reportTypes();
  const byLocale = {};

  for (const { locale, reports, totalVoters } of locales) {
    for (const {
      report,
      status,
      acceptability,
      votersForAcceptable,
      votersForNotAcceptable,
      acceptableScore,
      notAcceptableScore,
    } of reports) {
      if (!byLocale[locale]) {
        byLocale[locale] = {
          byReport: {},
          totalVoters,
        };
      }
      if (!byLocale[locale].byReport[report]) {
        byLocale[locale].byReport[report] = {
          totalVoters: votersForAcceptable + votersForNotAcceptable,
          acceptable: votersForAcceptable,
          unacceptable: votersForNotAcceptable,
          acceptableScore,
          notAcceptableScore,
        };
      }
      const r = byLocale[locale].byReport[report];
      r.status = status;
      r.acceptability = acceptability;
    }
  }

  return {
    types,
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

/**
 * Write all reports as a spreadsheet (66 survey_reports.xlsx)
 */
async function downloadAllReports() {
  const { types, byLocale } = await fetchAllReports();
  const ws_data = [["Locale", "Code", "Total Votes"]];
  for (const t of types) {
    const n = reportName(t);
    ws_data[0].push(`${n}-Status`);
    ws_data[0].push(`${n}-Result`);
    ws_data[0].push(`${n}-AcceptableScore`);
    ws_data[0].push(`${n}-NotAcceptableScore`);
    ws_data[0].push(`${n}-Total`);
  }
  // add per-locale rows in sorted order
  for (const loc of Object.keys(byLocale).sort()) {
    const { totalVoters, byReport } = byLocale[loc];
    const row = [];
    row.push(loc);
    row.push(cldrLoad.getLocaleName(loc));
    row.push(totalVoters);
    for (const t of types) {
      const {
        status,
        acceptability,
        totalVoters,
        acceptableScore,
        notAcceptableScore,
      } = byReport[t];
      row.push(status);
      row.push(acceptability || "");
      row.push(acceptableScore || 0);
      row.push(notAcceptableScore || 0);
      row.push(totalVoters);
    }
    ws_data.push(row);
  }
  const ws = XLSX.utils.aoa_to_sheet(ws_data);
  cldrXlsx.pushComment(ws, "C1", `As of ${new Date().toISOString()}`);
  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, `SurveyTool Report on Reports`);
  XLSX.writeFile(wb, `survey_reports.xlsx`);
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

/**
 * cldrReport: common functions for reports (r_*)
 */
import * as cldrAjax from "./cldrAjax.mjs";
import * as cldrClient from "./cldrClient.mjs";
import * as cldrDom from "./cldrDom.mjs";
import * as cldrLoad from "./cldrLoad.mjs";
import * as cldrStatus from "./cldrStatus.mjs";
import * as cldrSurvey from "./cldrSurvey.mjs";
import * as cldrText from "./cldrText.mjs";
import * as cldrVue from "./cldrVue.mjs";
import * as cldrXlsx from "./cldrXlsx.mjs";
import ReportResponse from "../views/ReportResponse.vue";
import * as XLSX from "xlsx";

let lastrr = null;

function reportLoadHandler(html, report) {
  cldrSurvey.hideLoader();
  cldrLoad.setLoading(false);
  const frag = window.document.createDocumentFragment();
  const div = window.document.createElement("div");
  frag.appendChild(div);
  const rr = cldrVue.create(ReportResponse, "n/a", { report });

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
}

function reportName(report) {
  return cldrText.get(`special_r_${report}`);
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
    throw new Error(
      "Please call client.apis.voting.getReportLocaleStatus() directly with the “-” parameter."
    );
  }
  const client = await cldrClient.getClient();
  const { obj } = await client.apis.voting.getReportLocaleStatus({
    locale,
  });
  if (obj.locales.length !== 1) {
    throw new Error(
      `getOneLocaleStatus(${locale}) expected an array of one item but got ${obj.locales.length}`
    );
  }
  return obj.locales[0];
}

/**
 * Get a single report in a single locale
 * @param {String} locale such as zh
 * @param {String} report  such as compact
 * @returns
 */
async function getOneReportLocaleStatus(locale, onlyReport) {
  const { reports, canVote } = await getOneLocaleStatus(locale);
  const report = reports.filter(({ report }) => report === onlyReport)[0];
  return { report, canVote };
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

// View Section

// called as special.load
function load(section) {
  // section = r_datetime. We are only called if isReport(section) = true
  section = section.substring(2); // remove initial r_
  cldrSurvey.showLoader(null);
  const url = getUrl(section);
  cldrSurvey.hideLoader();
  const xhrArgs = {
    url: url,
    // TODO: get json from server and do the presentation on the front end
    handleAs: "text", // not "html" or "json"!
    load: loadHandler,
    error: errorHandler,
  };
  cldrAjax.sendXhr(xhrArgs);
}

function getUrl(section) {
  const locale = cldrStatus.getCurrentLocale();
  const report = section;
  return (
    cldrStatus.getContextPath() +
    `/api/voting/reports/locales/${locale}/reports/${report}.html?` +
    cldrSurvey.cacheKill()
  );
}

function loadHandler(html) {
  const section = cldrStatus.getCurrentSpecial().substring(2); // pull section name from current special
  reportLoadHandler(html, section);
}

function errorHandler(err) {
  cldrSurvey.hideLoader();
  cldrLoad.setLoading(false);
  const html =
    "<div style='padding-top: 4em; font-size: x-large !important;' class='ferrorbox warning'>" +
    "<span class='icon i-stop'>" +
    " &nbsp; &nbsp;</span>Error: could not load: " +
    err +
    "</div>";
  const frag = cldrDom.construct(html);
  cldrLoad.flipToOtherDiv(frag);
}

export {
  downloadAllReports,
  fetchAllReports,
  getOneLocaleStatus,
  getOneReportLocaleStatus,
  load,
  reportLoadHandler,
  reportName,
  reportTypes,
};

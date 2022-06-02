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

async function fetchAllReports(user) {
  const client = await cldrClient.getClient();
  const raw = (
    await client.apis.voting.getAllReports({
      user: user || "-",
    })
  ).body;

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
      });
      e.totalVoters += 1;
      e.acceptable += acceptable?.length;
      e.unacceptable += completed?.length - acceptable?.length;
    }
  });

  return {
    types: (await client.apis.voting.listReports()).body,
    raw,
    byLocale,
  };
}

export { reportLoadHandler, reportClass, reportName, fetchAllReports };

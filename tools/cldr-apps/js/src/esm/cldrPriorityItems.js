/*
 * cldrPriorityItems: for Survey Tool feature "Priority Items Summary", encapsulate code
 * including getting/updating data through interaction with other modules
 * and the server. The display logic is in VettingSummary.vue.
 */
import * as cldrAjax from "../esm/cldrAjax.js";
import * as cldrStatus from "../esm/cldrStatus.js";

const SECONDS_IN_MS = 1000;

const NORMAL_RETRY = 10 * SECONDS_IN_MS; // "Normal" retry: starting or about to start

const SUMMARY_URL = "api/summary";
const LIST_SNAPSHOTS_URL = SUMMARY_URL + "/snapshots";

const LOAD_START = "START";
const LOAD_NOSTART = "NOSTART";
const LOAD_FORCESTOP = "FORCESTOP";

const SNAP_NONE = "NONE";
const SNAP_CREATE = "CREATE";
const SNAP_SHOW = "SHOW";

const SNAPID_NOT_APPLICABLE = "NA";

class SummaryArgs {
  /**
   * Construct a new SummaryArgs object
   *
   * @param {SummaryArgs} defaultArgs -- default (latest) args, or null/undefined
   * @param {String} loadingPolicy -- LOAD_START, LOAD_NOSTART or LOAD_FORCESTOP
   * @param {String} snapshotPolicy -- SNAP_NONE, SNAP_CREATE, or SNAP_SHOW
   * @param {String} snapshotId -- a timestamp or SNAPID_NOT_APPLICABLE
   * @returns a new SummaryArgs
   */
  constructor(defaultArgs, loadingPolicy, snapshotPolicy, snapshotId) {
    this.loadingPolicy =
      loadingPolicy || defaultArgs?.loadingPolicy || LOAD_NOSTART;
    this.snapshotPolicy =
      snapshotPolicy || defaultArgs?.snapshotPolicy || SNAP_NONE;
    this.snapshotId =
      snapshotId || defaultArgs?.snapshotId || SNAPID_NOT_APPLICABLE;
  }
}

let latestArgs = new SummaryArgs();

let canSnap = false;

let callbackToSetData = null;
let callbackToSetSnapshots = null;

function canUseSnapshots() {
  return canSnap;
}

function viewCreated(setData, setSnap) {
  callbackToSetData = setData;
  callbackToSetSnapshots = setSnap;
  if (cldrStatus.getPermissions()?.userIsAdmin) {
    canSnap = true;
  }
  fetchStatus();
}

function fetchStatus() {
  if (canSnap) {
    listSnapshots();
  }
  requestSummary(new SummaryArgs(latestArgs, LOAD_NOSTART));
}

function start() {
  requestSummary(new SummaryArgs(null, LOAD_START));
}

function stop() {
  requestSummary(new SummaryArgs(latestArgs, LOAD_FORCESTOP));
}

function showSnapshot(snapshotId) {
  requestSummary(new SummaryArgs(null, LOAD_START, SNAP_SHOW, snapshotId));
}

function createSnapshot() {
  requestSummary(new SummaryArgs(null, LOAD_START, SNAP_CREATE));
}

function requestSummary(summaryArgs) {
  latestArgs = summaryArgs;
  cldrAjax
    .doFetch(SUMMARY_URL, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(summaryArgs),
    })
    .then(cldrAjax.handleFetchErrors)
    .then((r) => r.json())
    .then(setSummaryData)
    .catch((error) => console.log(error));
}

function setSummaryData(data) {
  if (!callbackToSetData) {
    return;
  }
  callbackToSetData(data);
  if (latestArgs.loadingPolicy === LOAD_NOSTART) {
    window.setTimeout(fetchStatus.bind(this), NORMAL_RETRY);
  }
}

function listSnapshots() {
  if (!callbackToSetSnapshots) {
    return;
  }
  cldrAjax
    .doFetch(LIST_SNAPSHOTS_URL)
    .then(cldrAjax.handleFetchErrors)
    .then((r) => r.json())
    .then(callbackToSetSnapshots)
    .catch((error) => console.log(error));
}

export {
  SNAPID_NOT_APPLICABLE,
  canUseSnapshots,
  createSnapshot,
  showSnapshot,
  start,
  stop,
  viewCreated,
};

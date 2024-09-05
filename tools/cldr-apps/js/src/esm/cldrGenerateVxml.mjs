/*
 * cldrGenerateVxml: for Survey Tool feature "Generate VXML". The display logic is in GenerateVxml.vue.
 */
import * as cldrAjax from "./cldrAjax.mjs";
import * as cldrAnnounce from "./cldrAnnounce.mjs";
import * as cldrNotify from "./cldrNotify.mjs";
import * as cldrStatus from "./cldrStatus.mjs";

const SECONDS_IN_MS = 1000;

const NORMAL_RETRY = 10 * SECONDS_IN_MS; // "Normal" retry: starting or about to start

const VXML_URL = "api/vxml";

// These must match the back end; used in requests
class LoadingPolicy {
  static START = "START"; // start generating vxml
  static CONTINUE = "CONTINUE"; // continue generating vxml
  static STOP = "STOP"; // stop (cancel) generating vxml
}

// These must match the back end; used in responses
class Status {
  static INIT = "INIT"; // before making a request (back end does not have INIT)
  static WAITING = "WAITING"; // waiting on other users/tasks
  static PROCESSING = "PROCESSING"; // in progress
  static READY = "READY"; // finished successfully
  static STOPPED = "STOPPED"; // due to error or cancellation (LoadingPolicy.STOP)
}

let canGenerate = false;

let callbackToSetData = null;

function canGenerateVxml() {
  return canGenerate;
}

function viewMounted(setData) {
  callbackToSetData = setData;
  const perm = cldrStatus.getPermissions();
  canGenerate = Boolean(perm?.userIsAdmin);
}

function start() {
  // Disable announcements during VXML generation to reduce risk of interference
  cldrAnnounce.enableAnnouncements(false);
  requestVxml(LoadingPolicy.START);
}

function fetchStatus() {
  if (!canGenerate || "generate_vxml" !== cldrStatus.getCurrentSpecial()) {
    canGenerate = false;
  } else if (canGenerate) {
    requestVxml(LoadingPolicy.CONTINUE);
  }
}

function stop() {
  requestVxml(LoadingPolicy.STOP);
}

function requestVxml(loadingPolicy) {
  const args = { loadingPolicy: loadingPolicy };
  const init = cldrAjax.makePostData(args);
  cldrAjax
    .doFetch(VXML_URL, init)
    .then(cldrAjax.handleFetchErrors)
    .then((r) => r.json())
    .then(setVxmlData)
    .catch((e) => {
      cldrNotify.exception(e, "generating VXML");
    });
}

function setVxmlData(data) {
  if (!callbackToSetData) {
    return;
  }
  callbackToSetData(data);
  if (data.status === Status.WAITING || data.status === Status.PROCESSING) {
    window.setTimeout(fetchStatus.bind(this), NORMAL_RETRY);
  } else if (data.status === Status.READY || data.status === Status.STOPPED) {
    cldrAnnounce.enableAnnouncements(true); // restore
  }
}

export { Status, canGenerateVxml, start, stop, viewMounted };

/*
 * cldrGenerateVxml: for Survey Tool feature "Generate VXML". The display logic is in GenerateVxml.vue.
 */
import * as cldrAjax from "./cldrAjax.mjs";
import * as cldrAnnounce from "./cldrAnnounce.mjs";
import * as cldrNotify from "./cldrNotify.mjs";
import * as cldrStatus from "./cldrStatus.mjs";

const SECONDS_IN_MS = 1000;

const REQUEST_TIMER = 5 * SECONDS_IN_MS; // Fetch status this often

const VXML_URL = "vxml";
const VXML_DOWNLOAD_URL = "vxml/download";

// These must match the back end; used in requests
class RequestType {
  static START = "START"; // start generating vxml
  static CONTINUE = "CONTINUE"; // continue generating vxml
  static CANCEL = "CANCEL"; // cancel (stop) generating vxml
}

// These must match the back end; used in responses
class Status {
  static INIT = "INIT"; // before making a request (back end does not have INIT)
  static WAITING = "WAITING"; // waiting on other users/tasks
  static PROCESSING = "PROCESSING"; // in progress
  static SUCCEEDED = "SUCCEEDED"; // finished successfully
  static STOPPED = "STOPPED"; // due to error, verification failure, or cancellation
}

let canGenerate = false;

let callbackToSetData = null;

function canGenerateVxml() {
  return canGenerate;
}

function viewMounted(setData) {
  callbackToSetData = setData;
  const perm = cldrStatus.getPermissions();
  canGenerate = Boolean(perm?.userCanGenerateVxml);
}

function start() {
  // Disable announcements during VXML generation to reduce risk of interference
  cldrAnnounce.enableAnnouncements(false);
  requestVxml(RequestType.START);
}

function fetchStatus() {
  if (!canGenerate || "generate_vxml" !== cldrStatus.getCurrentSpecial()) {
    canGenerate = false;
  } else if (canGenerate) {
    requestVxml(RequestType.CONTINUE);
  }
}

function cancel() {
  requestVxml(RequestType.CANCEL);
}

function requestVxml(requestType) {
  const args = { requestType: requestType };
  const url = cldrAjax.makeApiUrl(VXML_URL, null);
  const init = cldrAjax.makePostData(args);
  cldrAjax
    .doFetch(url, init)
    .then(cldrAjax.handleFetchErrors)
    .then((r) => r.json())
    .then(setVxmlData)
    .catch((e) => {
      cldrNotify.exception(e, "generating VXML");
    });
}

async function download(directory) {
  const dirName = directory.split("/").pop(); // only the part after slash
  const p = new URLSearchParams();
  p.append("dirName", dirName);
  const url = cldrAjax.makeApiUrl(VXML_DOWNLOAD_URL, p);
  try {
    const response = await cldrAjax.doFetch(url);
    if (!response.ok) {
      throw new Error(response.statusText);
    }
    const bytes = await response.bytes();
    const a = document.createElement("a");
    a.href = window.URL.createObjectURL(
      new Blob([bytes], { type: "application/octet-stream" })
    );
    a.download = dirName + ".zip";
    a.click();
  } catch (e) {
    cldrNotify.exception(e, "Downloading VXML");
  }
}

function setVxmlData(data) {
  if (!callbackToSetData) {
    return;
  }
  callbackToSetData(data);
  if (data.status === Status.WAITING || data.status === Status.PROCESSING) {
    window.setTimeout(fetchStatus.bind(this), REQUEST_TIMER);
  } else if (
    data.status === Status.SUCCEEDED ||
    data.status === Status.STOPPED
  ) {
    cldrAnnounce.enableAnnouncements(true); // restore
  }
}

export { Status, cancel, canGenerateVxml, download, start, viewMounted };

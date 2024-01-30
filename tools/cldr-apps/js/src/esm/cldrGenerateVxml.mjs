/*
 * cldrGenerateVxml: for Survey Tool feature "Generate VXML". The display logic is in GenerateVxml.vue.
 */
import * as cldrAjax from "./cldrAjax.mjs";
import * as cldrStatus from "./cldrStatus.mjs";

const SECONDS_IN_MS = 1000;

const NORMAL_RETRY = 10 * SECONDS_IN_MS; // "Normal" retry: starting or about to start

const VXML_URL = "api/vxml";

const LOAD_START = "START";
const LOAD_NOSTART = "NOSTART";
const LOAD_FORCESTOP = "FORCESTOP";

class VxmlArgs {
  /**
   * Construct a new VxmlArgs object
   *
   * @param {VxmlArgs} defaultArgs -- default (latest) args, or null/undefined
   * @param {String} loadingPolicy -- LOAD_START, LOAD_NOSTART or LOAD_FORCESTOP
   * @returns a new VxmlArgs
   */
  constructor(defaultArgs, loadingPolicy) {
    this.loadingPolicy =
      loadingPolicy || defaultArgs?.loadingPolicy || LOAD_NOSTART;
  }
}

let latestArgs = new VxmlArgs();

let canGenerate = false;

let callbackToSetData = null;

function canGenerateVxml() {
  return canGenerate;
}

function viewCreated(setData) {
  callbackToSetData = setData;
  const perm = cldrStatus.getPermissions();
  if (perm?.userIsAdmin) {
    canGenerate = true;
  }
  // fetchStatus();
}

function fetchStatus() {
  if (!canGenerate || "generate_vxml" !== cldrStatus.getCurrentSpecial()) {
    canGenerate = false;
    return;
  }
  if (canGenerate) {
    requestVxml(new VxmlArgs(latestArgs, LOAD_NOSTART));
  }
}

function start() {
  const sa = new VxmlArgs(null, LOAD_START);
  requestVxml(sa);
}

function stop() {
  requestVxml(new VxmlArgs(latestArgs, LOAD_FORCESTOP));
}

function requestVxml(vxmlArgs) {
  latestArgs = vxmlArgs;
  const init = cldrAjax.makePostData(vxmlArgs);
  cldrAjax
    .doFetch(VXML_URL, init)
    .then(cldrAjax.handleFetchErrors)
    .then((r) => r.json())
    .then(setVxmlData)
    .catch((error) => console.log(error));
}

function setVxmlData(data) {
  if (!callbackToSetData) {
    return;
  }
  callbackToSetData(data);
  // if (latestArgs.loadingPolicy !== LOAD_FORCESTOP) {
  //  window.setTimeout(fetchStatus.bind(this), NORMAL_RETRY);
  // }
}

export { canGenerateVxml, start, stop, viewCreated };

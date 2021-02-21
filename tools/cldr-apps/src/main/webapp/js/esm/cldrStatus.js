/*
 * cldrStatus: encapsulate data defining the current status of SurveyTool.
 */
import * as cldrLoad from "./cldrLoad.js";

function updateAll(status) {
  if (status.contextPath) {
    setContextPath(status.contextPath);
  }
  if (status.isPhaseBeta) {
    setIsPhaseBeta(status.isPhaseBeta);
  }
  if (status.isUnofficial) {
    setIsUnofficial(status.isUnofficial);
  }
  if (status.newVersion) {
    setNewVersion(status.newVersion);
  }
  if (status.organizationName) {
    setOrganizationName(status.organizationName);
  }
  if (status.permissions) {
    setPermissions(status.permissions);
  }
  if (status.phase) {
    setPhase(status.phase);
  }
  if (status.specialHeader) {
    setSpecialHeader(status.specialHeader);
  }
  if (status.sessionId) {
    setSessionId(status.sessionId);
  }
  if (status.sessionMessage) {
    setSessionMessage(status.sessionMessage);
  }
  if (status.user) {
    setSurveyUser(status.user);
  }
}

/**
 * When this changes from one non-null value to another, the server has restarted
 * a.k.a. org.unicode.cldr.web.SurveyMain.surveyRunningStamp
 * a.k.a. status.surveyRunningStamp
 */
let runningStamp = null;

/**
 * Get the running stamp, or 1 if not initialized
 *
 * Don't initialize in this method; harmless when called for cacheKillStamp before initialized
 *
 * @return an integer
 */
function getRunningStamp() {
  if (runningStamp === null) {
    return 1;
  }
  return runningStamp;
}

/**
 * Has runningStamp changed from one truthy value to another?
 * While answering this question, also initialize runningStamp if not already initialized
 * and the given stamp is truthy.
 *
 * @param stamp the given stamp, should be a truthy integer, else we return false
 * @return true if runningStamp is initialized and the given stamp is truthy and differs; else false
 */
function runningStampChanged(stamp) {
  if (!stamp) {
    return false;
  }
  if (runningStamp === null) {
    runningStamp = stamp;
  }
  return stamp !== runningStamp;
}

/**
 * A string such as '/cldr-apps'
 * It may be set from (on the server) HttpServletRequest.getContextPath()
 * a.k.a. status.contextPath
 */
let contextPath = "/cldr-apps";

function getContextPath() {
  return contextPath;
}

function setContextPath(path) {
  if (path || path === "") {
    contextPath = path;
  }
}

/**
 * A string such as '' (empty), or '821c2a2fc5c206d' (identifying an xpath),
 * or '12345' (identifying a user)
 * a.k.a. surveyCurrentId
 */
let currentId = "";

function getCurrentId() {
  return currentId;
}

function setCurrentId(id) {
  currentId = id ? id : "";
}

/**
 * A string such as '' (empty), 'Gregorian', 'Languages_K_N'
 * a.k.a. surveyCurrentPage
 */
let currentPage = "";

function getCurrentPage() {
  return currentPage;
}

function setCurrentPage(page) {
  if (page || page === "") {
    currentPage = page;
  }
}

/**
 * A string such as null, '' (empty), 'forum', 'search', 'r_vetting_json', etc.
 * sometimes identifying a js module such as special/forum.js, but not always;
 * 'r_vetting_json' identifies the Dashboard.
 * a.k.a. surveyCurrentSpecial
 */
let currentSpecial = null;

function getCurrentSpecial() {
  return currentSpecial;
}

function setCurrentSpecial(special) {
  currentSpecial = special;
}

/**
 * A string such as 'en', 'fr', etc., identifying a locale
 * a.k.a. surveyCurrentLocale
 * Caution: cldrLoad.updateHashAndMenus makes a distinction between null and
 * empty string "" for getCurrentLocale, seemingly with the assumption that
 * null is the original value. Later it may become empty string "", and
 * cldrLoad.updateHashAndMenus doesn't treat that the same way.
 */
let currentLocale = null;

function getCurrentLocale() {
  return currentLocale;
}

function setCurrentLocale(loc) {
  currentLocale = loc;
}

/**
 * A string such as 'French', etc., naming a locale
 * a.k.a. surveyCurrentLocaleName
 */
let currentLocaleName = null;

function getCurrentLocaleName() {
  return currentLocaleName;
}

function setCurrentLocaleName(name) {
  currentLocaleName = name;
}

/**
 * A string such as 'Timezones', etc., naming a data section
 * a.k.a. surveyCurrentSection
 */
let currentSection = "";

function getCurrentSection() {
  return currentSection;
}

function setCurrentSection(sec) {
  currentSection = sec;
}

/**
 * A string such as 'Current' or '42.1', identifying the new CLDR version being created with Survey Tool
 */
let newVersion = "Current";

function getNewVersion() {
  return newVersion;
}

function setNewVersion(version) {
  newVersion = version;
}

/**
 * Is this an "unofficial" (test or non-production) instance of Survey Tool? (Boolean)
 */
let isUnofficial = null;

function getIsUnofficial() {
  return isUnofficial;
}

function setIsUnofficial(i) {
  isUnofficial = i;
}

/**
 * Is this a "beta" phase of Survey Tool? (Boolean)
 */
let phase = "";

function getPhase() {
  return phase;
}

function setPhase(p) {
  phase = p;
}

/**
 * Is this a "beta" phase of Survey Tool? (Boolean)
 */
let isPhaseBeta = null;

function getIsPhaseBeta() {
  return isPhaseBeta;
}

function setIsPhaseBeta(i) {
  isPhaseBeta = i;
}

/**
 * A string such as 'B0752471848DE78128A234F992BB3116', etc., identifying an HttpSession
 * a.k.a. surveySessionId
 */
let sessionId = null;
let sessionIdChangeCallback = null;

function getSessionId() {
  return sessionId;
}

function setSessionId(i) {
  if (i !== sessionId) {
    sessionId = i;
    if (sessionIdChangeCallback) {
      sessionIdChangeCallback(sessionId);
      sessionIdChangeCallback = null;
    }
  }
}

function setSessionIdChangeCallback(func) {
  sessionIdChangeCallback = func;
}

/**
 * A message describing the current session
 */
let sessionMessage = null;

function getSessionMessage() {
  return sessionMessage;
}

function setSessionMessage(m) {
  sessionMessage = m;
}

/**
 * An object describing the current user
 */
let surveyUser = null;

function getSurveyUser() {
  return surveyUser;
}

function setSurveyUser(u) {
  surveyUser = u;
}

/**
 * A special message to be displayed in the header
 */
let specialHeader = "";

function getSpecialHeader() {
  return specialHeader;
}

function setSpecialHeader(s) {
  specialHeader = s;
}

/**
 * The name of the user's organization
 */
let organizationName = null;

function getOrganizationName() {
  return organizationName;
}

function setOrganizationName(n) {
  organizationName = n;
}

/**
 * An object describing the current user's permissions
 * a.k.a. surveyUserPerms
 */
let permissions = null;

function getPermissions() {
  return permissions;
}

function setPermissions(p) {
  permissions = p;
}

function stopIcon() {
  // 🛑️
  const src = getContextPath() + "/stop.png";
  return (
    "<img alt='[stop]' style='width: 16px; height: 16px; border: 0;' src='" +
    src +
    "' title='Test Error' />"
  );
}

function warnIcon() {
  // ⚠
  const src = getContextPath() + "/warn.png";
  return (
    "<img alt='[warn]' style='width: 16px; height: 16px; border: 0;' src='" +
    src +
    "' title='Test Warning' />"
  );
}

function logoIcon() {
  const src = getContextPath() + "/STLogo.png";
  return (
    "<img src='" +
    src +
    "' align='right' border='0' title='[logo]' alt='[logo]' />"
  );
}

function getSurvUrl() {
  return getContextPath() + "/survey";
}

function isVisitor() {
  return getSurveyUser() === null;
}

/**
 * Is the ST disconnected?
 */
let disconnected = false;

function isDisconnected() {
  return disconnected;
}

function setIsDisconnected(d) {
  disconnected = d;
}

/**
 * Are we in the Dashboard or not?
 *
 * @return true or false
 */
function isDashboard() {
  return getCurrentSpecial() === "r_vetting_json";
}

export {
  getContextPath,
  getCurrentId,
  getCurrentLocale,
  getCurrentLocaleName,
  getCurrentPage,
  getCurrentSection,
  getCurrentSpecial,
  getIsPhaseBeta,
  getIsUnofficial,
  getNewVersion,
  getOrganizationName,
  getPermissions,
  getPhase,
  getRunningStamp,
  getSessionId,
  getSessionMessage,
  getSpecialHeader,
  getSurvUrl,
  getSurveyUser,
  isDashboard,
  isDisconnected,
  isVisitor,
  logoIcon,
  runningStampChanged,
  setContextPath,
  setCurrentId,
  setCurrentLocale,
  setCurrentLocaleName,
  setCurrentPage,
  setCurrentSection,
  setCurrentSpecial,
  setIsDisconnected,
  setIsPhaseBeta,
  setIsUnofficial,
  setNewVersion,
  setOrganizationName,
  setPermissions,
  setPhase,
  setSessionId,
  setSessionIdChangeCallback,
  setSessionMessage,
  setSpecialHeader,
  setSurveyUser,
  stopIcon,
  updateAll,
  warnIcon,
};

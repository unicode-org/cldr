/*
 * cldrLocales: encapsulate functions concerning locales for Survey Tool
 * It's also the "special" named "locales", which happens to be the default page
 * users see when they login.
 */
import * as cldrAjax from "./cldrAjax.mjs";
import * as cldrDom from "./cldrDom.mjs";
import * as cldrEvent from "./cldrEvent.mjs";
import * as cldrLoad from "./cldrLoad.mjs";
import { LocaleMap } from "./cldrLocaleMap.mjs";
import * as cldrNotify from "./cldrNotify.mjs";
import * as cldrRetry from "./cldrRetry.mjs";
import * as cldrStatus from "./cldrStatus.mjs";
import * as cldrSurvey from "./cldrSurvey.mjs";
import * as cldrText from "./cldrText.mjs";
import * as cldrVue from "./cldrVue.mjs";

import WelcomeWidget from "../views/WelcomeWidget.vue";

// The current locale may be "USER" temporarily, meaning the back end should choose an appropriate locale
// for the current user. The real locale ID should be set when a server response contains it.
const USER_LOCALE_ID = "USER";

const VALIDATE_ALL_LOCALES = true;
const VALIDATE_ALL_LOCALES_VERBOSE = false;

const LOCALE_REGEX = new RegExp("^[a-zA-Z0-9_]*$");

let didValidateAll = false;

async function fetchMap() {
  // we use a simple load here, just basic fetch. no session.
  const url = getUrl();
  return await fetch(url)
    .then((r) => r.json())
    .then(setMap);
}

function getUrl() {
  // We use a simple URL here, no session, just loading the locale map
  const p = new URLSearchParams();
  p.append("what", "locmap");
  return cldrAjax.makeUrl(p);
}

function setMap(json) {
  if (json.err) {
    cldrRetry.handleDisconnect(json.err, json, "", "Loading locale map");
  } else if (cldrLoad.verifyJson(json, "locmap")) {
    const locmap = new LocaleMap(json.locmap);
    cldrLoad.setTheLocaleMap(locmap);
  }
}

/** App of the previous app - so we can unmount it */
let welcomePageApp = null;

// called as special.load
function load() {
  cldrSurvey.hideLoader();
  cldrLoad.setLoading(false);
  const theDiv = document.createElement("div");
  theDiv.className = "localeList";

  if (welcomePageApp != null) {
    try {
      welcomePageApp.unmount();
    } catch (e) {
      // ignore unmount error
    }
  }

  cldrLoad.flipToOtherDiv(theDiv);

  cldrVue.mountReplace(WelcomeWidget, theDiv, (app) => {
    welcomePageApp = app;
  });

  cldrEvent.filterAllLocale(); // filter for init data
  cldrEvent.forceSidebar();
  cldrStatus.setCurrentLocale(null);
}

// called as special.parseHash
function parseHash(pieces) {
  cldrStatus.setCurrentLocale("");
  if (pieces.length > 2) {
    cldrStatus.setCurrentPage(pieces[2]);
    if (pieces.length > 3) {
      let id = pieces[3];
      if (id.substr(0, 2) === "x@") {
        id = id.substr(2);
      }
      cldrStatus.setCurrentId(id);
    } else {
      cldrStatus.setCurrentId("");
    }
    return true;
  } else {
    return false;
  }
}

function isValid(loc) {
  // It seems safest to require the canonical id here. If canonicalizeLocaleId were
  // called here, a caller of isValid might go on to use a non-canonical id in other contexts,
  // maybe leading to errors such as treating the same locale as two different locales.
  const map = cldrLoad.localeMapReady() ? cldrLoad.getTheLocaleMap() : null;
  if (VALIDATE_ALL_LOCALES && !didValidateAll && map) {
    validateAllLocales(map.locmap.locales);
    didValidateAll = true;
  }
  if (typeof loc == "string" && map?.locmap?.locales[loc]) {
    return true;
  }
  notifyUnusableLocale(map, loc);
  return false;
}

function notifyUnusableLocale(map, loc) {
  const problem = cldrText.get("locale_id_unusable");
  let explanation;
  if (!map) {
    explanation = cldrText.get("locale_id_list_unavailable");
  } else if (!typeof loc == "string") {
    // Exclude String objects and other objects and types, to avoid confusion.
    explanation = cldrText.get("locale_id_not_string_primitive");
  } else if (LOCALE_REGEX.test(loc)) {
    explanation = cldrText.sub("locale_id_unrecognized", [loc]);
  } else {
    // Avoid including the bogus locale ID in the notification if it contains non-ASCII
    // characters or reserved punctuation marks, since it may be URL-encoded or
    // otherwise distorted, in which case confusing or even code injection (malware).
    explanation = cldrText.get("locale_id_disallowed");
  }
  cldrNotify.error(problem, explanation);
}

function validateAllLocales(locales) {
  let count = 0;
  let failureCount = 0;
  for (let loc in locales) {
    if (VALIDATE_ALL_LOCALES_VERBOSE) {
      console.log("validateAllLocales: " + count + " " + loc);
    }
    if (!LOCALE_REGEX.test(loc)) {
      console.error("validateAllLocales: validation failure: " + loc);
      ++failureCount;
    }
    ++count;
  }
  if (failureCount) {
    console.error("validateAllLocales: failureCount = " + failureCount);
  } else if (VALIDATE_ALL_LOCALES_VERBOSE) {
    console.log("validateAllLocales: success, zero failures");
  }
}

/**
 * Get all the locale IDs that allow voting.
 * Filter the list of all locales to exclude read-only, default-content, scratch.
 * (See LocaleNormalizer.LocaleRejection on the back end.)
 * When setting a user's locales, if their org has "*" for locales, meaning "all locales",
 * this is the list to choose from.
 *
 * @returns {Array} the filtered list of locale IDs
 */
function getAllVotable() {
  const locMap = cldrLoad.getTheLocaleMap();
  const allLocs = Object.keys(locMap.locmap.locales);
  let filtered = [];
  for (const loc of allLocs) {
    const locInfo = locMap.getLocaleInfo(loc);
    // readonly includes default-content; special_type includes scratch
    if (!locInfo.readonly && !locInfo.special_type) {
      filtered.push(loc);
    }
  }
  return filtered;
}

export { fetchMap, getAllVotable, isValid, load, parseHash, USER_LOCALE_ID };

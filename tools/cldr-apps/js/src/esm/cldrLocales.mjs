/*
 * cldrLocales: encapsulate functions concerning locales for Survey Tool
 */
import * as cldrAjax from "./cldrAjax.mjs";
import * as cldrEvent from "./cldrEvent.mjs";
import * as cldrLoad from "./cldrLoad.mjs";
import { LocaleMap } from "./cldrLocaleMap.mjs";
import * as cldrMenu from "./cldrMenu.mjs";
import * as cldrNotify from "./cldrNotify.mjs";
import * as cldrRetry from "./cldrRetry.mjs";
import * as cldrStatus from "./cldrStatus.mjs";
import * as cldrSurvey from "./cldrSurvey.mjs";
import * as cldrText from "./cldrText.mjs";

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

// called as special.load
function load() {
  cldrSurvey.hideLoader();
  cldrLoad.setLoading(false);
  const theDiv = document.createElement("div");
  theDiv.className = "localeList";

  // TODO: avoid duplication of some of this code here and in cldrMenu.mjs
  // Maybe a lot of code in cldrMenu and cldrLoad should be moved into cldrLocales
  cldrMenu.addTopLocale("root", theDiv);
  const locmap = cldrLoad.getTheLocaleMap();
  for (let n in locmap.locmap.topLocales) {
    const topLoc = locmap.locmap.topLocales[n];
    cldrMenu.addTopLocale(topLoc, theDiv);
  }
  cldrLoad.flipToOtherDiv(null);
  cldrEvent.filterAllLocale(); // filter for init data
  cldrEvent.forceSidebar();
  cldrStatus.setCurrentLocale(null);

  // When clicking on the locale name in the header of the main Page view,
  // the OtherSection div may be non-empty and needs to be hidden here
  const otherSection = document.getElementById("OtherSection");
  if (otherSection) {
    otherSection.style.display = "none";
  }
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

export { fetchMap, isValid, load, parseHash, USER_LOCALE_ID };

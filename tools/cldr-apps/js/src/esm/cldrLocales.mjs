/*
 * cldrLocales: encapsulate functions concerning locales for Survey Tool
 */
import * as cldrEvent from "./cldrEvent.mjs";
import * as cldrLoad from "./cldrLoad.mjs";
import * as cldrMenu from "./cldrMenu.mjs";
import * as cldrNotify from "./cldrNotify.mjs";
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
  const map = cldrLoad.getTheLocaleMap();
  if (VALIDATE_ALL_LOCALES && !didValidateAll && map?.locmap?.locales) {
    validateAllLocales(map.locmap.locales);
    didValidateAll = true;
  }
  if (map?.getLocaleInfo(loc)) {
    return true;
  }
  notifyUnusableLocale(map, loc);
  return false;
}

function notifyUnusableLocale(map, loc) {
  let explanation;
  if (!map) {
    explanation = cldrText.get("locale_id_list_unavailable");
  } else if (LOCALE_REGEX.test(loc)) {
    explanation = cldrText.sub("locale_id_unrecognized", loc);
  } else {
    // Avoid including the bogus locale ID in the notification if it contains non-ASCII
    // characters or reserved punctuation marks, since it may be URL-encoded or
    // otherwise distorted, in which case confusing or even code injection (malware).
    explanation = cldrText.get("locale_id_disallowed");
  }
  cldrNotify.error(cldrText.get("locale_id_unusable"), explanation);
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

export { isValid, load, parseHash, USER_LOCALE_ID };

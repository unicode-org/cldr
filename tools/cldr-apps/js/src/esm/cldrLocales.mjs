/*
 * cldrLocales: encapsulate functions concerning locales for Survey Tool
 */
import * as cldrEvent from "./cldrEvent.mjs";
import * as cldrLoad from "./cldrLoad.mjs";
import * as cldrMenu from "./cldrMenu.mjs";
import * as cldrStatus from "./cldrStatus.mjs";
import * as cldrSurvey from "./cldrSurvey.mjs";

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

export { load, parseHash };

"use strict";

/**
 * cldrLocales: encapsulate functions concerning locales for Survey Tool
 * This is the non-dojo version. For dojo, see CldrDojoLoad.js etc.
 *
 * Use an IIFE pattern to create a namespace for the public functions,
 * and to hide everything else, minimizing global scope pollution.
 */
const cldrLocales = (function () {
  // called as special.load
  function load() {
    cldrSurvey.hideLoader();
    cldrLoad.setLoading(false);
    const theDiv = document.createElement("div");
    theDiv.className = "localeList";

    // TODO: avoid duplication of some of this code here and in cldrMenu.js
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
    cldrStatus.setCurrentSpecial("locales"); // TODO: always redundant? it's already "locales"
    const message = cldrText.get("localesInitialGuidance");
    cldrInfo.showMessage(message);
    $("#itemInfo").html("");
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

  /*
   * Make only these functions accessible from other files:
   */
  return {
    load,
    parseHash,
    /*
     * The following are meant to be accessible for unit testing only:
     */
    // test: {
    //   f: f,
    // },
  };
})();

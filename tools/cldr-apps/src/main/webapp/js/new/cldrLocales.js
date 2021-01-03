"use strict";

/**
 * cldrLocales: encapsulate functions for choosing locales for Survey Tool
 * This is the non-dojo version. For dojo, see CldrDojoLoad.js
 *
 * Use an IIFE pattern to create a namespace for the public functions,
 * and to hide everything else, minimizing global scope pollution.
 */

const cldrLocales = (function () {
  function load() {
    cldrSurvey.hideLoader();
    cldrLoad.setLoading(false);
    const theDiv = document.createElement("div");
    theDiv.className = "localeList";

    cldrLoad.addTopLocale("root", theDiv);
    // top locales
    const locmap = cldrLoad.getTheLocaleMap();
    for (let n in locmap.locmap.topLocales) {
      const topLoc = locmap.locmap.topLocales[n];
      cldrLoad.addTopLocale(topLoc, theDiv);
    }
    cldrLoad.flipToOtherDiv(null);
    cldrEvent.filterAllLocale(); // filter for init data
    cldrEvent.forceSidebar();
    cldrStatus.setCurrentLocale(null);
    cldrStatus.setCurrentSpecial("locales");
    const message = cldrText.get("localesInitialGuidance");
    cldrInfo.showMessage(message);
    $("#itemInfo").html("");
  }

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
    /*
     * The following are meant to be accessible for unit testing only:
     */
    // test: {
    //   f: f,
    // },
  };
})();

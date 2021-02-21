"use strict";

/**
 * cldrAbout: encapsulate functions for the "About" page of Survey Tool
 *
 * Use an IIFE pattern to create a namespace for the public functions,
 * and to hide everything else, minimizing global scope pollution.
 * Ideally this should be a module (in the sense of using import/export),
 * but not all Survey Tool JavaScript code is capable yet of being in modules
 * and running in strict mode.
 */

const cldrAbout = (function () {
  const testCldrRetry = false; // danger, must not be true for production

  // called as special.load
  function load() {
    loadHandler({}); // null load
  }

  function loadHandler(json) {
    if (testCldrRetry && Math.random() > 0.5) {
      cldrRetry.handleDisconnect(
        "while loading the About page (testing)",
        json
      );
      return;
    }

    const ourDiv = document.createElement("div");
    ourDiv.innerHTML = getHtml(json);
    cldrSurvey.hideLoader();
    cldrLoad.flipToOtherDiv(ourDiv);

    // add Vue-based about box
    const app = document.createElement("div");
    ourDiv.appendChild(app);
    cldrBundle.showPanel('about', app);
  }

  function errorHandler(err) {
    const ourDiv = document.createElement("div");
    ourDiv.innerHTML = err;
    cldrSurvey.hideLoader();
    cldrLoad.flipToOtherDiv(ourDiv);
  }

  function getHtml(json) {
    let html = cldrStatus.logoIcon();
    return html;
  }

  /*
   * Make only these functions accessible from other files:
   */
  return {
    load,
    /*
     * The following are meant to be accessible for unit testing only:
     */
    test: {
      getHtml,
    },
  };
})();

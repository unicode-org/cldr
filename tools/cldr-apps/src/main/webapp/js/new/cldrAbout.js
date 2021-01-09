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
  const moreInfo =
    "<p class='hang'>For more information about the Survey Tool, " +
    "see <a href='http://www.unicode.org/cldr'>unicode.org/cldr</a>.</p>\n";

  const javaOsVersionKeys = [
    "java.version",
    "java.vendor",
    "java.vm.version",
    "java.vm.vendor",
    "java.vm.name",
    "os.name",
    "os.arch",
    "os.version",
  ];

  const testCldrRetry = false; // danger, must not be true for production
  const testVue = false;

  /**
   * Temporary test to confirm interpolation works
   * -- really we'll use .vue files rather than use js to write html
   */
  const vueTestHtml =
    "<h3 id='vue-test' style='color: orange;'> {{ message }} </h3>\n";

  // called as special.load
  function load() {
    cldrInfo.showNothing();
    const xhrArgs = {
      url: cldrStatus.getContextPath() + "/SurveyAjax?what=about",
      handleAs: "json",
      load: loadHandler,
      error: errorHandler,
    };
    cldrAjax.sendXhr(xhrArgs);
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

    if (testVue) {
      new Vue({
        el: "#vue-test",
        data: {
          message: "Hello Vue! ICU_VERSION: " + json["ICU_VERSION"],
        },
      });
    }
  }

  function errorHandler(err) {
    const ourDiv = document.createElement("div");
    ourDiv.innerHTML = err;
    cldrSurvey.hideLoader();
    cldrLoad.flipToOtherDiv(ourDiv);
  }

  function getHtml(json) {
    let html = cldrStatus.logoIcon();
    if (testVue) {
      html += vueTestHtml;
    }
    html += javaVersions(json) + otherVersions(json) + stInfo(json);
    if (json["hasDataSource"]) {
      html += dbInfo(json);
    }
    html += moreInfo;
    return html;
  }

  function javaVersions(json) {
    let html = "<h4 class='selected'>Java Versions</h4>\n";
    html += "<table class='userlist' border='2'>\n";
    for (let i in javaOsVersionKeys) {
      const label = javaOsVersionKeys[i];
      const key = label.replaceAll(".", "_");
      const val = json[key];
      // Style of rows alternates between classes 'row0' and 'row1', hence i % 2
      const rowClass = "row" + (i % 2);
      html +=
        "<tr class='" +
        rowClass +
        "'><th><tt>" +
        label +
        "</tt></th><td>" +
        val +
        "</td></tr>\n";
    }
    html += "</table>\n";
    return html;
  }

  function otherVersions(json) {
    const majorMinor =
      json["servletMajorVersion"] + "." + json["servletMinorVersion"];

    let i = 0;
    let html = "<h4 class='selected'>Other Versions</h4>\n";
    html += "<table class='userlist' border='2'>\n";

    html += "<tr class='row" + (i++ % 2) + "'>\n";
    html += "<th>CLDRFile.GEN_VERSION</th>";
    html += "<td>" + json["GEN_VERSION"] + "</td>\n";
    html += "</tr>\n";

    html += "<tr class='row" + (i++ % 2) + "'>\n";
    html += "<th>ICU</th>";
    html += "<td>" + json["ICU_VERSION"] + "</td>\n";
    html += "</tr>\n";

    html += "<tr class='row" + (i++ % 2) + "'>\n";
    html += "<th>Server</th>";
    html += "<td>" + json["serverInfo"] + "</td>\n";
    html += "</tr>\n";

    html += "<tr class='row" + (i++ % 2) + "'>\n";
    html += "<th>Java Servlet API</th>";
    html += "<td>" + majorMinor + "</td>\n";
    html += "</tr>\n";

    html += "</table>\n";
    return html;
  }

  function stInfo(json) {
    let i = 0;
    let html = "<h4 class='selected'>Survey Tool information</h4>\n";
    html += "<table class='userlist' border='2'>\n";

    html += "<tr class='row" + (i++ % 2) + "'>\n";
    html += "<th>SurveyMain.TRANS_HINT_LOCALE</th>";
    html += "<td>" + json["TRANS_HINT_LOCALE"] + "</td>\n";
    html += "</tr>\n";

    html += "<tr class='row" + (i++ % 2) + "'>\n";
    html += "<th>SurveyMain.TRANS_HINT_LANGUAGE_NAME</th>";
    html += "<td>" + json["TRANS_HINT_LANGUAGE_NAME"] + "</td>\n";
    html += "</tr>\n";

    html += "<tr class='row" + (i++ % 2) + "'>\n";
    html += "<th>CLDR_UTILITIES_HASH</th>";
    html += "<td>" + json["CLDR_UTILITIES_HASH"] + "</td>\n";
    html += "</tr>\n";

    html += "<tr class='row" + (i++ % 2) + "'>\n";
    html += "<th>CLDR_SURVEYTOOL_HASH</th>";
    html += "<td>" + json["CLDR_SURVEYTOOL_HASH"] + "</td>\n";
    html += "</tr>\n";

    html += "<tr class='row" + (i++ % 2) + "'>\n";
    html += "<th>CLDR_DATA_HASH</th>";
    html += "<td>" + json["CLDR_DATA_HASH"] + "</td>\n";
    html += "</tr>\n";

    html += "</table>\n";
    return html;
  }

  function dbInfo(json) {
    let i = 0;
    let html = "<h4 class='selected'>Database information</h4>\n";
    html += "<table class='userlist' border='2'>\n";

    html += "<tr class='row" + (i++ % 2) + "'>\n";
    html += "<th>Have Datasource?</th>";
    html += "<td>" + json["hasDataSource"] + "</td>\n";
    html += "</tr>\n";

    html += "<tr class='row" + (i++ % 2) + "'>\n";
    html += "<th>DB Kind / Info</th>";
    html += "<td>" + json["dbKind"] + "<br />" + json["dbInfo"] + "</td>\n";
    html += "</tr>\n";

    html += "</table>\n";
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

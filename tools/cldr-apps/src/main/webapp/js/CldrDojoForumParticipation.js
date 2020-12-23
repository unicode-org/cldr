"use strict";

/**
 * cldrForumParticipation: encapsulate Survey Tool Forum Participation code.
 *
 * Use an IIFE pattern to create a namespace for the public functions,
 * and to hide everything else, minimizing global scope pollution.
 * Ideally this should be a module (in the sense of using import/export),
 * but not all Survey Tool JavaScript code is capable yet of being in modules
 * and running in strict mode.
 *
 * Dependencies: SpecialPage; hideLoader; showInPop2
 */
const cldrForumParticipation = (function () {
  const tableId = "participationTable";
  const fileName = "participation.csv";
  const onclick =
    "cldrCsvFromTable.download(" +
    '"' +
    tableId +
    '"' +
    ", " +
    '"' +
    fileName +
    '"' +
    ")";

  /**
   * Fetch the Forum Participation data from the server, and "load" it
   *
   * @param params an object with various properties; see SpecialPage.js
   */
  function load(params) {
    /*
     * Set up the 'right sidebar'; cf. forum_participationGuidance
     */
    showInPop2(cldrText.get(params.name + "Guidance"), null, null, null, true);

    const url = getForumParticipationUrl();
    const errorHandler = function (err) {
      params.special.showError(params, null, {
        err: err,
        what: "Loading forum participation data",
      });
    };
    const loadHandler = function (json) {
      if (json.err) {
        if (params.special) {
          params.special.showError(params, json, {
            what: "Loading forum participation data",
          });
        }
        return;
      }
      const html = makeHtmlFromJson(json);
      const ourDiv = document.createElement("div");
      ourDiv.innerHTML = html;

      // No longer loading
      hideLoader(null);
      params.flipper.flipTo(params.pages.other, ourDiv);
    };
    const xhrArgs = {
      url: url,
      handleAs: "json",
      load: loadHandler,
      error: errorHandler,
    };
    cldrAjax.sendXhr(xhrArgs);
  }

  /**
   * Get the URL to use for loading the Forum Participation page
   */
  function getForumParticipationUrl() {
    const sessionId = cldrStatus.getSessionId();
    if (!sessionId) {
      console.log("Error: sessionId falsy in getForumParticipationUrl");
      return "";
    }
    return "SurveyAjax?what=forum_participation&s=" + sessionId;
  }

  /**
   * Make the html, given the json for Forum Participation
   *
   * @param json
   * @return the html
   */
  function makeHtmlFromJson(json) {
    let html = "<div>\n";
    if (json.org) {
      html += "<h4>Organization: " + json.org + "</h4>\n";
    }
    if (json.rows && json.rows.header && json.rows.data) {
      const myheaders = [
        "LOC", // headers in order
        "FORUM_TOTAL",
        "FORUM_ORG",
        "FORUM_REQUEST",
        "FORUM_DISCUSS",
        "FORUM_ACT",
      ];
      html += "<h4><a onclick='" + onclick + "'>Download CSV</a></h4>\n";
      html += "<table border='1' id='" + tableId + "'>\n";
      html += "<tr>\n";
      for (let header of [
        // same order as above
        cldrText.get("recentLoc"),
        cldrText.get("forum_participation_TOTAL"),
        cldrText.get("forum_participation_ORG"),
        cldrText.get("forum_participation_REQUEST"),
        cldrText.get("forum_participation_DISCUSS"),
        cldrText.get("forum_participation_ACT"),
      ]) {
        html += "<th>" + header + "</th>\n";
      }
      html += "</tr>\n";
      for (let row of json.rows.data) {
        html += "<tr>\n";
        for (let header of myheaders) {
          html += "<td>" + row[json.rows.header[header]] + "</td>\n";
        }
        html += "</tr>\n";
      }
      html += "</table>\n";
    }
    html += "</div>";
    return html;
  }

  /*
   * Make only these functions accessible from other files
   */
  return {
    load: load,
    /*
     * The following are meant to be accessible for unit testing only:
     */
    test: {
      makeHtmlFromJson: makeHtmlFromJson,
    },
  };
})();

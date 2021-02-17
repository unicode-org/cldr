"use strict";

/**
 * cldrRecentActivity: encapsulate the "Recent Activity" feature, a.k.a. "myvotes".
 *
 * This feature is reached in three ways:
 *  (1) "My Recent Activity" from the gear menu
 *  (2) "My Account Settings" from the gear menu,
 *      then click the "User Activity" link in the table
 *  (3) "List ... Users" from the gear menu, then zoom in on a single user,
 *      and click the "User Activity" link in the table; the user in
 *      question may not be the current user
 *
 * Use an IIFE pattern to create a namespace for the public functions,
 * and to hide everything else, minimizing global scope pollution.
 */
const cldrRecentActivity = (function () {
  // called as special.load
  function load() {
    const surveyUser = cldrStatus.getSurveyUser();
    if (!surveyUser || !surveyUser.id) {
      return;
    }
    const url = getUrl(surveyUser.id);
    cldrLoad.myLoad(url, "(loading recent activity)", loadWithJson);
  }

  function getUrl(id) {
    const p = new URLSearchParams();
    p.append("what", "recent_activity");
    p.append("user", id);
    p.append("s", cldrStatus.getSessionId());
    p.append("cacheKill", cldrSurvey.cacheBuster());
    return cldrAjax.makeUrl(p);
  }

  function loadWithJson(json) {
    cldrSurvey.hideLoader();
    cldrLoad.setLoading(false);
    const frag = cldrDom.construct(getHtml(json));
    cldrLoad.flipToOtherDiv(frag);
    cldrEvent.hideRightPanel();
    showRecent("submitItems", null, json.user);
    showAllItems("allMyItems", json.user);
  }

  function getHtml(json) {
    let html =
      "<i>All items shown are for the current release, CLDR " +
      json.newVersion +
      ". Votes before " +
      json.votesAfterDate +
      " are not shown.</i>\n" +
      "<hr />\n" +
      "<h3>The most recently submitted items for user " +
      json.user +
      "</h3>\n" +
      "<div id='submitItems'></div>\n" +
      "<hr />\n" +
      "<h3>All active locales for user " +
      json.user +
      "</h3>\n" +
      "<div id='allMyItems'></div>\n" +
      "<hr />\n" +
      "<form method='POST' action='DataExport.jsp'>\n" +
      "  <input type='hidden' name='s' value='" +
      cldrStatus.getSessionId() +
      "'>\n" +
      "  <input type='hidden' name='user' value='" +
      json.user +
      "'>\n" +
      "  <input type='hidden' name='do' value='mydata'>\n" +
      "  <input type='submit' class='csvDownload' value='Download all of my votes as .csv'>\n" +
      "</form>\n";

    return html;
  }

  function showRecent(divName, locale, user) {
    if (!locale) {
      locale = "";
    }
    if (!user) {
      user = "";
    }
    let div;
    if (divName.nodeType > 0) {
      div = divName;
    } else {
      div = document.getElementById(divName);
    }
    div.className = "recentList";
    div.update = function () {
      let ourUrl =
        cldrStatus.getContextPath() +
        "/SurveyAjax?what=recent_items&_=" +
        locale +
        "&user=" +
        user +
        "&limit=" +
        15;
      cldrSurvey.showLoader("Loading recent items");
      const xhrArgs = {
        url: ourUrl,
        handleAs: "json",
        load: loadHandler,
        error: errorHandler,
      };
      cldrAjax.queueXhr(xhrArgs);
    };
    div.update();

    // TODO: no long nested functions!
    function loadHandler(json) {
      try {
        if (json && json.recent) {
          const frag = document.createDocumentFragment();
          const header = json.recent.header;
          const data = json.recent.data;

          if (data.length == 0) {
            frag.appendChild(
              cldrDom.createChunk(cldrText.get("recentNone"), "i")
            );
          } else {
            const rowDiv = document.createElement("div");
            frag.appendChild(rowDiv);

            rowDiv.appendChild(
              cldrDom.createChunk(cldrText.get("recentLoc"), "b")
            );
            rowDiv.appendChild(
              cldrDom.createChunk(cldrText.get("recentXpathCode"), "b")
            );
            rowDiv.appendChild(
              cldrDom.createChunk(cldrText.get("recentValue"), "b")
            );
            rowDiv.appendChild(
              cldrDom.createChunk(cldrText.get("recentWhen"), "b")
            );
            for (let q in data) {
              const row = data[q];
              const loc = row[header.LOCALE];
              const locname = row[header.LOCALE_NAME];
              const org = row[header.ORG];
              const last_mod = row[header.LAST_MOD];
              const xpath = row[header.XPATH];
              let xpath_code = row[header.XPATH_CODE];
              const xpath_hash = row[header.XPATH_STRHASH];
              const value = row[header.VALUE];

              const rowDiv = document.createElement("div");
              frag.appendChild(rowDiv);
              rowDiv.appendChild(createLocLink(loc, locname, "recentLoc"));
              let xpathItem;
              xpath_code = xpath_code.replace(/\t/g, " / ");
              rowDiv.appendChild(
                (xpathItem = cldrDom.createChunk(
                  xpath_code,
                  "a",
                  "recentXpath"
                ))
              );
              xpathItem.href = "survey?_=" + loc + "&strid=" + xpath_hash;
              rowDiv.appendChild(
                cldrDom.createChunk(value, "span", "value recentValue")
              );
              rowDiv.appendChild(
                cldrDom.createChunk(
                  new Date(last_mod).toLocaleString(),
                  "span",
                  "recentWhen"
                )
              );
            }
          }
          cldrDom.removeAllChildNodes(div);
          div.appendChild(frag);
          cldrSurvey.hideLoader();
        } else {
          cldrRetry.handleDisconnect("Failed to load JSON recent items", json);
        }
      } catch (e) {
        console.log("Error in ajax get ", e.message);
        cldrRetry.handleDisconnect(
          " exception in getrecent: " + e.message,
          null
        );
      }
    }

    function errorHandler(err) {
      cldrRetry.handleDisconnect("Error in showrecent: " + err);
    }
  }

  function showAllItems(divName, user) {
    const div = document.getElementById(divName);
    div.className = "recentList";
    div.update = function () {
      const ourUrl =
        cldrStatus.getContextPath() + "/SurveyAjax?what=mylocales&user=" + user;
      cldrSurvey.showLoader("Loading recent items");

      const xhrArgs = {
        url: ourUrl,
        handleAs: "json",
        load: loadHandler,
        error: errorHandler,
      };
      cldrAjax.queueXhr(xhrArgs);
    };
    div.update();

    // TODO: no long nested functions!
    function loadHandler(json) {
      try {
        if (json && json.mine) {
          const frag = document.createDocumentFragment();
          const header = json.mine.header;
          const data = json.mine.data;
          if (data.length == 0) {
            frag.appendChild(
              cldrDom.createChunk(cldrText.get("recentNone"), "i")
            );
          } else {
            const rowDiv = document.createElement("div");
            frag.appendChild(rowDiv);

            rowDiv.appendChild(
              cldrDom.createChunk(cldrText.get("recentLoc"), "b")
            );
            rowDiv.appendChild(
              cldrDom.createChunk(cldrText.get("recentCount"), "b")
            );

            for (let q in data) {
              const row = data[q];
              const count = row[header.COUNT];
              const rowDiv = document.createElement("div");
              frag.appendChild(rowDiv);

              const loc = row[header.LOCALE];
              const locname = row[header.LOCALE_NAME];
              rowDiv.appendChild(createLocLink(loc, locname, "recentLoc"));
              rowDiv.appendChild(
                cldrDom.createChunk(count, "span", "value recentCount")
              );

              const sessionId = cldrStatus.getSessionId();
              if (sessionId) {
                const dlLink = cldrDom.createChunk(
                  cldrText.get("downloadXmlLink"),
                  "a",
                  "notselected"
                );
                dlLink.href =
                  "DataExport.jsp?do=myxml&_=" +
                  loc +
                  "&user=" +
                  user +
                  "&s=" +
                  sessionId;
                dlLink.target = "STDownload";
                rowDiv.appendChild(dlLink);
              }
            }
          }
          cldrDom.removeAllChildNodes(div);
          div.appendChild(frag);
          cldrSurvey.hideLoader();
        } else {
          cldrRetry.handleDisconnect("Failed to load JSON recent items", json);
        }
      } catch (e) {
        console.log("Error in ajax get ", e.message);
        cldrRetry.handleDisconnect(
          " exception in getrecent: " + e.message,
          null
        );
      }
    }

    function errorHandler(err) {
      cldrRetry.handleDisconnect("Error in showrecent: " + err);
    }
  }

  function createLocLink(loc, locName, className) {
    const cl = cldrDom.createChunk(locName, "a", "localeChunk " + className);
    cl.title = loc;
    cl.href = "survey?_=" + loc;
    return cl;
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
    //   f,
    // },
  };
})();

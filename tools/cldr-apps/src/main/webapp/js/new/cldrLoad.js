"use strict";

/**
 * cldrLoad: encapsulate functions for loading GUI content for Survey Tool
 * This is the non-dojo version. For dojo, see CldrDojoLoad.js
 *
 * Use an IIFE pattern to create a namespace for the public functions,
 * and to hide everything else, minimizing global scope pollution.
 * Ideally this should be a module (in the sense of using import/export),
 * but not all Survey Tool JavaScript code is capable yet of being in modules
 * and running in strict mode.
 */

const cldrLoad = (function () {
  /**
   * haveDialog: when true, it means a "dialog" of some kind is displayed.
   * Used for inhibiting $('#left-sidebar').hover in redesign.js.
   * Currently there are only two such dialogs, both for auto-import.
   */
  let haveDialog = false;

  /**
   * copy of menu data
   */
  let _thePages = null;

  let locmap = new LocaleMap(null); // TODO: is it really a singleton?
  // it may be modified below with locmap = new LocaleMap(json.locmap)

  let canmodify = {};

  let isLoading = false;

  /**
   * list of pages to use with the flipper
   * @property pages
   */
  let pages = {
    loading: "LoadingMessageSection",
    data: "DynamicDataSection",
    other: "OtherSection",
  };

  /**
   * List of buttons/titles to set.
   */
  const menubuttons = {
    locale: "title-locale",
    section: "title-section",
    page: "title-page",
    dcontent: "title-dcontent",

    // menubuttons.set is called by updateLocaleMenu and updateHashAndMenus
    set: function (x, y) {
      var cnode = document.getElementById(x + "-container");
      var wnode = pseudoDijitRegistryById(x);
      var dnode = document.getElementById(x);
      if (!cnode) {
        cnode = dnode; // for Elements that do their own stunts
      }
      if (y && y !== "-" && y !== "") {
        if (wnode != null) {
          wnode.set("label", y);
        } else {
          cldrSurvey.updateIf(x, y); // non widget
        }
        cldrSurvey.setDisplayed(cnode, true);
      } else {
        cldrSurvey.setDisplayed(cnode, false);
        if (wnode != null) {
          wnode.set("label", "-");
        } else {
          cldrSurvey.updateIf(x, "-"); // non widget
        }
      }
    },
  };

  const otherSpecial = new OtherSpecial();

  let flipper = null;

  // TODO: implement things like these without using dojo/dijit
  const dijitMenuSeparator = null;
  const dijitButton = null;
  const dojoxBusyButton = null;

  /**************************/

  /**
   * Call this once in the page. It expects to find a node #DynamicDataSection
   */
  function showV() {
    flipper = new Flipper([pages.loading, pages.data, pages.other]);

    var pucontent = document.getElementById("itemInfo");
    var theDiv = flipper.get(pages.data);
    theDiv.pucontent = pucontent;

    pucontent.appendChild(
      cldrSurvey.createChunk(cldrText.get("itemInfoBlank"), "i")
    );

    // click on the title to copy (permalink)
    cldrSurvey.clickToSelect(document.getElementById("ariScroller"));
    cldrSurvey.updateIf(
      "title-dcontent-link",
      cldrText.get("defaultContent_titleLink")
    );

    /*
     * Arrange for getInitialMenusEtc to be called after we've gotten the session id.
     */
    getM();
  }

  function getM() {
    const sessionId = cldrStatus.getSessionId();
    if (sessionId) {
      getInitialMenusEtc(sessionId);
    } else {
      setTimeout(getM, 100); // try again after 1/10 second
    }
  }

  function getInitialMenusEtc(sessionId) {
    parseHashAndUpdate(getHash()); // get the initial settings
    // load the menus - first.

    var theLocale = cldrStatus.getCurrentLocale();
    if (theLocale === null || theLocale == "") {
      theLocale = "root"; // Default.
    }
    var xurl =
      cldrStatus.getContextPath() +
      "/SurveyAjax?what=menus&_=" +
      theLocale +
      "&locmap=" +
      true +
      "&s=" +
      sessionId +
      cldrSurvey.cacheKill();
    myLoad(xurl, "initial menus for " + theLocale, function (json) {
      if (!verifyJson(json, "locmap")) {
        return;
      } else {
        locmap = new LocaleMap(json.locmap);
        if (cldrStatus.getCurrentLocale() === "USER" && json.loc) {
          cldrStatus.setCurrentLocale(json.loc);
        }
        // make this into a hashmap.
        if (json.canmodify) {
          for (var k in json.canmodify) {
            canmodify[json.canmodify[k]] = true;
          }
        }

        // update left sidebar with locale data
        var theDiv = document.createElement("div");
        theDiv.className = "localeList";

        addTopLocale("root", theDiv);
        // top locales
        for (var n in locmap.locmap.topLocales) {
          var topLoc = locmap.locmap.topLocales[n];
          addTopLocale(topLoc, theDiv);
        }
        $("#locale-list").html(theDiv.innerHTML);

        if (cldrStatus.isVisitor()) {
          $("#show-read").prop("checked", true);
        }
        //tooltip locale
        $("a.locName").tooltip();

        cldrEvent.filterAllLocale();
        // end of adding the locale data

        cldrSurvey.updateCovFromJson(json);
        // setup coverage level
        const surveyLevels = json.menus.levels;
        cldrSurvey.setSurveyLevels(surveyLevels);

        var levelNums = []; // numeric levels
        for (var k in surveyLevels) {
          levelNums.push({
            num: parseInt(surveyLevels[k].level),
            level: surveyLevels[k],
          });
        }
        levelNums.sort(function (a, b) {
          return a.num - b.num;
        });

        var store = [];

        store.push({
          label: "Auto",
          value: "auto",
          title: cldrText.get("coverage_auto_desc"),
        });

        store.push({
          type: "separator",
        });

        for (var j in levelNums) {
          // use given order
          if (levelNums[j].num == 0) continue; // none - skip
          if (levelNums[j].num < cldrSurvey.covValue("minimal")) {
            continue; // don't bother showing these
          }
          if (cldrStatus.getIsUnofficial() === false && levelNums[j].num == 101)
            continue; // hide Optional in production
          var level = levelNums[j].level;
          store.push({
            label: cldrText.get("coverage_" + level.name),
            value: level.name,
            title: cldrText.get("coverage_" + level.name + "_desc"),
          });
        }
        //coverage menu
        var patternCoverage = $("#title-coverage .dropdown-menu");
        if (store[0].value) {
          $("#coverage-info").text(store[0].label);
        }
        for (var index = 0; index < store.length; ++index) {
          var data = store[index];
          if (data.value) {
            var html =
              '<li><a class="coverage-list" data-value="' +
              data.value +
              '"href="#">' +
              data.label +
              "</a></li>";
            patternCoverage.append(html);
          }
        }
        patternCoverage.find("li a").click(function (event) {
          patternCoverageClick(event, theLocale, $(this));
        });
        if (json.canAutoImport) {
          doAutoImport();
        }

        reloadV();

        // watch for hashchange to make other changes; cf. old "/dojo/hashchange"
        window.addEventListener("hashchange", doHashChange);
      }
    });
  } // end getInitialMenusEtc

  function patternCoverageClick(event, theLocale, clickedElement) {
    event.stopPropagation();
    event.preventDefault();
    var newValue = clickedElement.data("value");
    var setUserCovTo = null;
    if (newValue == "auto") {
      setUserCovTo = null; // auto
    } else {
      setUserCovTo = newValue;
    }
    if (setUserCovTo === cldrSurvey.getSurveyUserCov()) {
      console.log("No change in user cov: " + setUserCovTo);
    } else {
      cldrSurvey.setSurveyUserCov(setUserCovTo);
      var updurl =
        cldrStatus.getContextPath() +
        "/SurveyAjax?what=pref&_=" +
        theLocale +
        "&pref=p_covlev&_v=" +
        cldrSurvey.getSurveyUserCov() +
        "&s=" +
        cldrStatus.getSessionId() +
        cldrSurvey.cacheKill(); // SurveyMain.PREF_COVLEV
      myLoad(
        updurl,
        "updating covlev to  " + cldrSurvey.getSurveyUserCov(),
        function (json) {
          if (!verifyJson(json, "pref")) {
            return;
          } else {
            cldrEvent.unpackMenuSideBar(json);
            if (
              cldrStatus.getCurrentSpecial() &&
              cldrSurvey.isReport(cldrStatus.getCurrentSpecial())
            ) {
              reloadV();
            }
            console.log("Server set covlev successfully.");
          }
        }
      );
    }
    // still update these.
    cldrSurvey.updateCoverage(flipper.get(pages.data)); // update CSS and 'auto' menu title
    updateHashAndMenus(false); // TODO: why? Maybe to show an item?
    $("#coverage-info").text(ucFirst(newValue));
    clickedElement.parents(".dropdown-menu").dropdown("toggle");
    if (!cldrStatus.isDashboard()) {
      cldrSurvey.refreshCounterVetting();
    }
    return false;
  }

  function doHashChange(event) {
    const changedHash = getHash();
    if (sliceHash(new URL(event.newURL).hash) !== changedHash) {
      console.log(
        "Error in doHashChange: expected " +
          event.newURL.hash +
          " === " +
          changedHash
      );
    }
    var oldLocale = trimNull(cldrStatus.getCurrentLocale());
    var oldSpecial = trimNull(cldrStatus.getCurrentSpecial());
    var oldPage = trimNull(cldrStatus.getCurrentPage());
    var oldId = trimNull(cldrStatus.getCurrentId());

    parseHashAndUpdate(changedHash);

    cldrStatus.setCurrentId(trimNull(cldrStatus.getCurrentId()));

    // did anything change?
    if (
      oldLocale != trimNull(cldrStatus.getCurrentLocale()) ||
      oldSpecial != trimNull(cldrStatus.getCurrentSpecial()) ||
      oldPage != trimNull(cldrStatus.getCurrentPage())
    ) {
      console.log("# hash changed, (loc, etc) reloadingV..");
      reloadV();
    } else if (
      oldId != cldrStatus.getCurrentId() &&
      cldrStatus.getCurrentId() != ""
    ) {
      console.log("# just ID changed, to " + cldrStatus.getCurrentId());
      // surveyCurrentID and the hash have already changed.
      // just call showInPop if the item is present. If not present, make sure it's visible.
      showCurrentId();
    }
  }

  function addTopLocale(topLoc, theDiv) {
    var topLocInfo = locmap.getLocaleInfo(topLoc);

    var topLocRow = document.createElement("div");
    topLocRow.className = "topLocaleRow";

    var topLocDiv = document.createElement("div");
    topLocDiv.className = "topLocale";
    appendLocaleLink(topLocDiv, topLoc, topLocInfo);

    var topLocList = document.createElement("div");
    topLocList.className = "subLocaleList";

    addSubLocales(topLocList, topLocInfo);

    topLocRow.appendChild(topLocDiv);
    topLocRow.appendChild(topLocList);
    theDiv.appendChild(topLocRow);
  }

  /**
   * Parse the hash string into surveyCurrent___ variables.
   * Expected to update document.title also.
   *
   * @param {String} hash
   */
  function parseHashAndUpdate(hash) {
    if (hash) {
      var pieces = hash.substr(0).split("/");
      if (pieces.length > 1) {
        cldrStatus.setCurrentLocale(pieces[1]); // could be null
        /*
         * TODO: find a way if possible to fix here when cldrStatus.getCurrentLocale() === "USER".
         * It may be necessary (and sufficient) to wait for server response, see "USER" elsewhere
         * in this file. cachedJson.loc and _thePages.loc are generally (always?) undefined here.
         * Reference: https://unicode.org/cldr/trac/ticket/11161
         */
      } else {
        cldrStatus.setCurrentLocale("");
      }
      const curLocale = cldrStatus.getCurrentLocale();
      if (pieces[0].length == 0 && curLocale != "" && curLocale != null) {
        if (pieces.length > 2) {
          cldrStatus.setCurrentPage(pieces[2]);
          if (pieces.length > 3) {
            let id = pieces[3];
            if (id.substr(0, 2) == "x@") {
              id = id.substr(2);
            }
            cldrStatus.setCurrentId(id);
          } else {
            cldrStatus.setCurrentId("");
          }
        } else {
          cldrStatus.setCurrentPage("");
          cldrStatus.setCurrentId("");
        }
        cldrStatus.setCurrentSpecial(null);
      } else {
        cldrStatus.setCurrentSpecial(pieces[0]);
        if (cldrStatus.getCurrentSpecial() == "") {
          cldrStatus.setCurrentSpecial("locales");
        }
        if (cldrStatus.getCurrentSpecial() == "locales") {
          // allow locales list to retain ID / Page string for passthrough.
          cldrStatus.setCurrentLocale("");
          if (pieces.length > 2) {
            cldrStatus.setCurrentPage(pieces[2]);
            if (pieces.length > 3) {
              let id = pieces[3];
              if (id.substr(0, 2) == "x@") {
                id = id.substr(2);
              }
              cldrStatus.setCurrentId(id);
            } else {
              cldrStatus.setCurrentId("");
            }
          } else {
            cldrStatus.setCurrentPage("");
            cldrStatus.setCurrentId("");
          }
        } else if (cldrSurvey.isReport(cldrStatus.getCurrentSpecial())) {
          // allow page and ID to fall through.
          if (pieces.length > 2) {
            cldrStatus.setCurrentPage(pieces[2]);
            if (pieces.length > 3) {
              cldrStatus.setCurrentId(pieces[3]);
            } else {
              cldrStatus.setCurrentId("");
            }
          } else {
            cldrStatus.setCurrentPage("");
            cldrStatus.setCurrentId("");
          }
        } else {
          otherSpecial.parseHash(cldrStatus.getCurrentSpecial(), hash, pieces);
        }
      }
    } else {
      cldrStatus.setCurrentLocale("");
      cldrStatus.setCurrentSpecial("locales");
      cldrStatus.setCurrentId("");
      cldrStatus.setCurrentPage("");
      cldrStatus.setCurrentSection("");
    }
    updateWindowTitle();

    // if there is no locale id, refresh the search.
    if (!cldrStatus.getCurrentLocale()) {
      cldrEvent.searchRefresh();
    }
  }

  function updateWindowTitle() {
    var t = cldrText.get("survey_title");
    const curLocale = cldrStatus.getCurrentLocale();
    if (curLocale && curLocale != "") {
      t = t + ": " + locmap.getLocaleName(curLocale);
    }
    const curSpecial = cldrStatus.getCurrentSpecial();
    if (curSpecial && curSpecial != "") {
      t = t + ": " + cldrText.get("special_" + curSpecial);
    }
    const curPage = cldrStatus.getCurrentPage();
    if (curPage && curPage != "") {
      t = t + ": " + curPage;
    }
    document.title = t;
  }

  // Compare the similar function updateCurrentId in cldrSurvey.js -- difference: replaceHash
  function updateCurrentId(id) {
    if (id == null) {
      id = "";
    }
    if (cldrStatus.getCurrentId() != id) {
      // don't set if already set.
      cldrStatus.setCurrentId(id);
      replaceHash(false); // usually don't want to save
    }
  }

  /**
   * Update hash (and title)
   *
   * @param doPush {Boolean} if true, do a push (instead of replace)
   *
   * Called by cldrForum.parseContent (doPush false), as well as locally.
   */
  function replaceHash(doPush) {
    if (!doPush) {
      doPush = false; // by default -replace.
    }
    var theId = cldrStatus.getCurrentId();
    if (theId == null) {
      theId = "";
    }
    var theSpecial = cldrStatus.getCurrentSpecial();
    if (theSpecial == null) {
      theSpecial = "";
    }
    var thePage = cldrStatus.getCurrentPage();
    if (thePage == null) {
      thePage = "";
    }
    var theLocale = cldrStatus.getCurrentLocale();
    if (theLocale == null) {
      theLocale = "";
    }
    var newHash = theSpecial + "/" + theLocale + "/" + thePage + "/" + theId;
    if (newHash != getHash()) {
      setHash(newHash, !doPush);
    }
  }

  /**
   * Verify that the JSON returned is as expected.
   *
   * @param json the returned json
   * @param subkey the key to look for,  json.subkey
   * @return true if OK, false if bad
   */
  function verifyJson(json, subkey) {
    if (!json) {
      console.log("!json");
      cldrSurvey.showLoader(
        "Error while  loading " +
          subkey +
          ":  <br><div style='border: 1px solid red;'>" +
          "no data!" +
          "</div>"
      );
      return false;
    } else if (json.err_code) {
      var msg_fmt = formatErrMsg(json, subkey);
      var loadingChunk;
      flipper.flipTo(
        pages.loading,
        (loadingChunk = cldrSurvey.createChunk(msg_fmt, "p", "errCodeMsg"))
      );
      var retryButton = cldrSurvey.createChunk(
        cldrText.get("loading_reload"),
        "button"
      );
      loadingChunk.appendChild(retryButton);
      retryButton.onclick = function () {
        window.location.reload(true);
      };
      return false;
    } else if (json.err) {
      console.log("json.err!" + json.err);
      cldrSurvey.showLoader(
        "Error while  loading " +
          subkey +
          ": <br><div style='border: 1px solid red;'>" +
          json.err +
          "</div>"
      );
      cldrSurvey.handleDisconnect("while loading " + subkey + "", json);
      return false;
    } else if (!json[subkey]) {
      console.log("!json.oldvotes");
      cldrSurvey.showLoader(
        "Error while  loading " +
          subkey +
          ": <br><div style='border: 1px solid red;'>" +
          "no data" +
          "</div>"
      );
      cldrSurvey.handleDisconnect("while loading- no " + subkey + "", json);
      return false;
    } else {
      return true;
    }
  }

  function showCurrentId() {
    const curSpecial = cldrStatus.getCurrentSpecial();
    if (curSpecial && curSpecial != "" && !cldrStatus.isDashboard()) {
      otherSpecial.handleIdChanged(curSpecial, showCurrentId);
    } else {
      const curId = cldrStatus.getCurrentId();
      if (curId != "") {
        var xtr = document.getElementById("r@" + curId);
        if (!xtr) {
          console.log("Warning could not load id " + curId + " does not exist");
          updateCurrentId(null);
        } else if (xtr.proposedcell && xtr.proposedcell.showFn) {
          // TODO: visible? coverage?
          cldrSurvey.showInPop(
            "",
            xtr,
            xtr.proposedcell,
            xtr.proposedcell.showFn,
            true
          );
          console.log("Changed to " + cldrStatus.getCurrentId());
          if (!cldrStatus.isDashboard()) {
            scrollToItem();
          }
        } else {
          console.log(
            "Warning could not load id " +
              curId +
              " - not setup - " +
              xtr.toString() +
              " pc=" +
              xtr.proposedcell +
              " sf = " +
              xtr.proposedcell.showFn
          );
        }
      }
    }
  }

  /**
   * Show the surveyCurrentId row
   */
  function scrollToItem() {
    const curId = cldrStatus.getCurrentId();
    if (curId != null && curId != "") {
      // TODO
      console.log("scrollToItem not implemented yet; curId = " + curId);
      /****
      require(["dojo/window"], function (win) {
        var xtr = document.getElementById("r@" + curId);
        if (xtr != null) {
          console.log("Scrolling to " + curId);
          win.scrollIntoView("r@" + curId);
        }
      });
      ***/
    }
  }

  function updateCoverageMenuTitle() {
    const cov = cldrSurvey.getSurveyUserCov();
    if (cov) {
      $("#cov-info").text(cldrText.get("coverage_" + cov));
    } else {
      $("#coverage-info").text(
        cldrText.sub("coverage_auto_msg", {
          surveyOrgCov: cldrText.get(
            "coverage_" + cldrSurvey.getSurveyOrgCov()
          ),
        })
      );
    }
  }

  function insertLocaleSpecialNote(theDiv) {
    var bund = locmap.getLocaleInfo(cldrStatus.getCurrentLocale());
    let msg = null;

    if (bund) {
      if (bund.readonly || bund.special_comment_raw) {
        if (bund.readonly) {
          if (bund.special_comment_raw) {
            msg = bund.special_comment_raw;
          } else {
            msg = cldrText.get("readonly_unknown");
          }
          msg = cldrText.sub("readonly_msg", {
            info: bund,
            locale: cldrStatus.getCurrentLocale(),
            msg: msg,
          });
        } else {
          // Not readonly, could be a scratch locale
          msg = bund.special_comment_raw;
        }
        if (msg) {
          msg = locmap.linkify(msg);
          var theChunk = cldrDomConstruct(msg);
          var subDiv = document.createElement("div");
          subDiv.appendChild(theChunk);
          subDiv.className = "warnText";
          theDiv.insertBefore(subDiv, theDiv.childNodes[0]);
        }
      } else if (bund.dcChild) {
        const html = cldrText.sub("defaultContentChild_msg", {
          name: bund.name,
          dcChild: bund.dcChild,
          locale: cldrStatus.getCurrentLocale(),
          dcChildName: locmap.getLocaleName(bund.dcChild),
        });
        var theChunk = cldrDomConstruct(html);
        var subDiv = document.createElement("div");
        subDiv.appendChild(theChunk);
        subDiv.className = "warnText";
        theDiv.insertBefore(subDiv, theDiv.childNodes[0]);
      }
    }
    if (cldrStatus.getIsPhaseBeta()) {
      const html = cldrText.sub("beta_msg", {
        info: bund,
        locale: cldrStatus.getCurrentLocale(),
        msg: msg,
      });
      var theChunk = cldrDomConstruct(html);
      var subDiv = document.createElement("div");
      subDiv.appendChild(theChunk);
      subDiv.className = "warnText";
      theDiv.insertBefore(subDiv, theDiv.childNodes[0]);
    }
  }

  function updateLocaleMenu() {
    const curLocale = cldrStatus.getCurrentLocale();
    if (curLocale != null && curLocale != "" && curLocale != "-") {
      cldrStatus.setCurrentLocaleName(locmap.getLocaleName(curLocale));
      var bund = locmap.getLocaleInfo(curLocale);
      if (bund) {
        if (bund.readonly) {
          cldrSurvey.addClass(
            document.getElementById(menubuttons.locale),
            "locked"
          );
        } else {
          cldrSurvey.removeClass(
            document.getElementById(menubuttons.locale),
            "locked"
          );
        }

        if (bund.dcChild) {
          menubuttons.set(
            menubuttons.dcontent,
            cldrText.sub("defaultContent_header_msg", {
              info: bund,
              locale: cldrStatus.getCurrentLocale(),
              dcChild: locmap.getLocaleName(bund.dcChild),
            })
          );
        } else {
          menubuttons.set(menubuttons.dcontent);
        }
      } else {
        cldrSurvey.removeClass(
          document.getElementById(menubuttons.locale),
          "locked"
        );
        menubuttons.set(menubuttons.dcontent);
      }
    } else {
      cldrStatus.setCurrentLocaleName("");
      cldrSurvey.removeClass(
        document.getElementById(menubuttons.locale),
        "locked"
      );
      menubuttons.set(menubuttons.dcontent);
    }
    menubuttons.set(menubuttons.locale, cldrStatus.getCurrentLocaleName());
  }

  /**
   * Show the "possible problems" section which has errors for the locale
   */
  function showPossibleProblems(
    flipper,
    flipPage,
    loc,
    session,
    effectiveCov,
    requiredCov
  ) {
    cldrStatus.setCurrentLocale(loc);

    var url =
      cldrStatus.getContextPath() +
      "/SurveyAjax?what=possibleProblems&_=" +
      cldrStatus.getCurrentLocale() +
      "&s=" +
      session +
      "&eff=" +
      effectiveCov +
      "&req=" +
      requiredCov +
      cldrSurvey.cacheKill();
    myLoad(url, "possibleProblems", function (json) {
      if (verifyJson(json, "possibleProblems")) {
        if (json.dataLoadTime) {
          cldrSurvey.updateIf("dynload", json.dataLoadTime);
        }

        var theDiv = flipper.flipToEmpty(flipPage);

        insertLocaleSpecialNote(theDiv);

        if (json.possibleProblems.length > 0) {
          var subDiv = cldrSurvey.createChunk("", "div");
          subDiv.className = "possibleProblems";

          var h3 = cldrSurvey.createChunk(
            cldrText.get("possibleProblems"),
            "h3"
          );
          subDiv.appendChild(h3);

          var div3 = document.createElement("div");
          var newHtml = "";
          newHtml += cldrSurvey.testsToHtml(json.possibleProblems);
          div3.innerHTML = newHtml;
          subDiv.appendChild(div3);
          theDiv.appendChild(subDiv);
        }
        var theInfo = cldrSurvey.createChunk("", "p", "special_general");
        theDiv.appendChild(theInfo);
        theInfo.innerHTML = cldrText.get("special_general"); // TODO replace with … ?
        cldrSurvey.hideLoader();
      }
    });
  }

  function reloadV() {
    if (cldrStatus.isDisconnected()) {
      unbust();
    }

    document.getElementById("DynamicDataSection").innerHTML = ""; //reset the data
    $("#nav-page").hide();
    $("#nav-page-footer").hide();
    isLoading = false;

    /*
     * Scroll back to top when loading a new page, to avoid a bug where, for
     * example, having scrolled towards bottom, we switch from a Section page
     * to the Forum page and the scrollbar stays where it was, making the new
     * content effectively invisible.
     */
    window.scrollTo(0, 0);

    const id = flipper.get(pages.data).id;
    cldrSurvey.setShower(id, ignoreReloadRequest);

    // assume parseHash was already called, if we are taking input from the hash

    ariDialogHide();

    updateHashAndMenus(true);

    const curLocale = cldrStatus.getCurrentLocale();
    if (curLocale != null && curLocale != "" && curLocale != "-") {
      var bund = locmap.getLocaleInfo(curLocale);
      if (bund !== null && bund.dcParent) {
        const html = cldrText.sub("defaultContent_msg", {
          name: bund.name,
          dcParent: bund.dcParent,
          locale: curLocale,
          dcParentName: locmap.getLocaleName(bund.dcParent),
        });
        var theChunk = cldrDomConstruct(html);
        var theDiv = document.createElement("div");
        theDiv.appendChild(theChunk);
        theDiv.className = "ferrbox";
        flipper.flipTo(pages.other, theDiv);
        return;
      }
    }

    var loadingChunk;
    flipper.flipTo(
      pages.loading,
      (loadingChunk = cldrSurvey.createChunk(
        cldrText.get("loading"),
        "i",
        "loadingMsg"
      ))
    );

    var itemLoadInfo = cldrSurvey.createChunk("", "div", "itemLoadInfo");

    // Create a little spinner to spin "..." so the user knows we are doing something..
    var spinChunk = cldrSurvey.createChunk("...", "i", "loadingMsgSpin");
    var spin = 0;
    var timerToKill = window.setInterval(function () {
      var spinTxt = "";
      spin++;
      switch (spin % 3) {
        case 0:
          spinTxt = ".  ";
          break;
        case 1:
          spinTxt = " . ";
          break;
        case 2:
          spinTxt = "  .";
          break;
      }
      cldrSurvey.removeAllChildNodes(spinChunk);
      spinChunk.appendChild(document.createTextNode(spinTxt));
    }, 1000);

    // Add the "..." until the Flipper flips
    flipper.addUntilFlipped(
      function () {
        var frag = document.createDocumentFragment();
        frag.appendChild(spinChunk);
        return frag;
      },
      function () {
        window.clearInterval(timerToKill);
      }
    );

    shower(itemLoadInfo); // first load

    // set up the "show-er" function so that if this locale gets reloaded,
    // the page will load again - except for the dashboard, where only the
    // row get updated
    if (!cldrStatus.isDashboard()) {
      const id2 = flipper.get(pages.data).id;
      cldrSurvey.setShower(id2, shower);
    }
  } // end reloadV

  function ignoreReloadRequest() {
    console.log(
      "reloadV()'s shower - ignoring reload request, we are in the middle of a load!"
    );
  }

  // now, load. Use a show-er function for indirection.
  function shower(itemLoadInfo) {
    if (isLoading) {
      console.log("reloadV inner shower: already isLoading, exiting.");
      return;
    }
    isLoading = true;
    var theDiv = flipper.get(pages.data);
    var theTable = theDiv.theTable;

    if (!theTable) {
      var theTableList = theDiv.getElementsByTagName("table");
      if (theTableList) {
        theTable = theTableList[0];
        theDiv.theTable = theTable;
      }
    }

    cldrSurvey.showLoader(cldrText.get("loading"));

    const curSpecial = cldrStatus.getCurrentSpecial();
    const curLocale = cldrStatus.getCurrentLocale();
    if (
      (curSpecial == null || curSpecial == "") &&
      curLocale != null &&
      curLocale != ""
    ) {
      const curPage = cldrStatus.getCurrentPage();
      if (
        (curPage == null || curPage == "") &&
        (cldrStatus.getCurrentId() == null || cldrStatus.getCurrentId() == "")
      ) {
        // the 'General Info' page.
        itemLoadInfo.appendChild(
          document.createTextNode(locmap.getLocaleName(curLocale))
        );
        showPossibleProblems(
          flipper,
          pages.other,
          curLocale,
          cldrStatus.getSessionId(),
          /* TODO: why twice? */
          cldrSurvey.covName(cldrSurvey.effectiveCoverage()),
          cldrSurvey.covName(cldrSurvey.effectiveCoverage())
        );
        cldrSurvey.showInPop2(
          cldrText.get("generalPageInitialGuidance"),
          null,
          null,
          null,
          true
        ); /* show the box the first time */
        isLoading = false;
      } else if (cldrStatus.getCurrentId() == "!") {
        var frag = document.createDocumentFragment();
        frag.appendChild(
          cldrSurvey.createChunk(
            cldrText.get("section_help"),
            "p",
            "helpContent"
          )
        );
        var infoHtml = cldrText.get("section_info_" + curPage);
        var infoChunk = document.createElement("div");
        infoChunk.innerHTML = infoHtml;
        frag.appendChild(infoChunk);
        flipper.flipTo(pages.other, frag);
        cldrSurvey.hideLoader();
        isLoading = false;
      } else if (!cldrSurvey.isInputBusy()) {
        /*
         * Make “all rows” requests only when !isInputBusy, to avoid wasted requests
         * if the user leaves the input box open for an extended time.
         */
        // (common case) this is an actual locale data page.
        const curId = cldrStatus.getCurrentId();
        const curPage = cldrStatus.getCurrentPage();
        const curLocale = cldrStatus.getCurrentLocale();
        itemLoadInfo.appendChild(
          document.createTextNode(
            locmap.getLocaleName(curLocale) + "/" + curPage + "/" + curId
          )
        );
        var url =
          cldrStatus.getContextPath() +
          "/SurveyAjax?what=getrow&_=" +
          curLocale +
          "&x=" +
          curPage +
          "&strid=" +
          curId +
          "&s=" +
          cldrStatus.getSessionId() +
          cldrSurvey.cacheKill();
        $("#nav-page").show(); // make top "Prev/Next" buttons visible while loading, cf. '#nav-page-footer' below
        myLoad(url, "section", function (json) {
          isLoading = false;
          cldrSurvey.showLoader(cldrText.get("loading2"));
          if (!verifyJson(json, "section")) {
            return;
          } else if (json.section.nocontent) {
            cldrStatus.setCurrentSection("");
            if (json.pageId) {
              cldrStatus.setCurrentPage(json.pageId);
            } else {
              cldrStatus.setCurrentPage("");
            }
            cldrSurvey.showLoader(null);
            updateHashAndMenus(); // find out why there's no content. (locmap)
          } else if (!json.section.rows) {
            console.log("!json.section.rows");
            cldrSurvey.showLoader(
              "Error while  loading: <br><div style='border: 1px solid red;'>" +
                "no rows" +
                "</div>"
            );
            cldrSurvey.handleDisconnect("while loading- no rows", json);
          } else {
            cldrSurvey.showLoader("loading..");
            if (json.dataLoadTime) {
              cldrSurvey.updateIf("dynload", json.dataLoadTime);
            }

            cldrStatus.setCurrentSection("");
            cldrStatus.setCurrentPage(json.pageId);
            updateHashAndMenus(); // now that we have a pageid
            if (!cldrStatus.getSurveyUser()) {
              cldrSurvey.showInPop2(
                cldrText.get("loginGuidance"),
                null,
                null,
                null,
                true
              ); /* show the box the first time */
            } else if (!json.canModify) {
              cldrSurvey.showInPop2(
                cldrText.get("readonlyGuidance"),
                null,
                null,
                null,
                true
              ); /* show the box the first time */
            } else {
              cldrSurvey.showInPop2(
                cldrText.get("dataPageInitialGuidance"),
                null,
                null,
                null,
                true
              ); /* show the box the first time */
            }
            if (!cldrSurvey.isInputBusy()) {
              cldrSurvey.showLoader(cldrText.get("loading3"));
              cldrTable.insertRows(
                theDiv,
                json.pageId,
                cldrStatus.getSessionId(),
                json
              ); // pageid is the xpath..
              cldrSurvey.updateCoverage(flipper.get(pages.data)); // make sure cov is set right before we show.
              flipper.flipTo(pages.data); // TODO now? or later?
              showCurrentId(); // already calls scroll
              cldrSurvey.refreshCounterVetting();
              $("#nav-page-footer").show(); // make bottom "Prev/Next" buttons visible after building table
            }
          }
        });
      }
    } else if (cldrStatus.getCurrentSpecial() == "oldvotes") {
      const curLocale = cldrStatus.getCurrentLocale();
      var url =
        cldrStatus.getContextPath() +
        "/SurveyAjax?what=oldvotes&_=" +
        curLocale +
        "&s=" +
        cldrStatus.getSessionId() +
        "&" +
        cldrSurvey.cacheKill();
      myLoad(url, "(loading oldvotes " + curLocale + ")", function (json) {
        isLoading = false;
        cldrSurvey.showLoader(cldrText.get("loading2"));
        if (!verifyJson(json, "oldvotes")) {
          return;
        } else {
          cldrSurvey.showLoader("loading..");
          if (json.dataLoadTime) {
            cldrSurvey.updateIf("dynload", json.dataLoadTime);
          }

          var theDiv = flipper.flipToEmpty(pages.other); // clean slate, and proceed..

          cldrSurvey.removeAllChildNodes(theDiv);

          var h2txt = cldrText.get("v_oldvotes_title");
          theDiv.appendChild(cldrSurvey.createChunk(h2txt, "h2", "v-title"));

          if (!json.oldvotes.locale) {
            cldrStatus.setCurrentLocale("");
            updateHashAndMenus();

            var ul = document.createElement("div");
            ul.className = "oldvotes_list";
            var data = json.oldvotes.locales.data;
            var header = json.oldvotes.locales.header;

            if (data.length > 0) {
              data.sort((a, b) =>
                a[header.LOCALE].localeCompare(b[header.LOCALE])
              );
              for (var k in data) {
                var li = document.createElement("li");

                var link = cldrSurvey.createChunk(
                  data[k][header.LOCALE_NAME],
                  "a"
                );
                link.href = "#" + data[k][header.LOCALE];
                (function (loc, link) {
                  return function () {
                    var clicky;
                    listenFor(
                      link,
                      "click",
                      (clicky = function (e) {
                        cldrStatus.setCurrentLocale(loc);
                        reloadV();
                        cldrSurvey.stStopPropagation(e);
                        return false;
                      })
                    );
                    link.onclick = clicky;
                  };
                })(data[k][header.LOCALE], link)();
                li.appendChild(link);
                li.appendChild(cldrSurvey.createChunk(" "));
                li.appendChild(
                  cldrSurvey.createChunk("(" + data[k][header.COUNT] + ")")
                );

                ul.appendChild(li);
              }

              theDiv.appendChild(ul);

              theDiv.appendChild(
                cldrSurvey.createChunk(
                  cldrText.get("v_oldvotes_locale_list_help_msg"),
                  "p",
                  "helpContent"
                )
              );
            } else {
              theDiv.appendChild(
                cldrSurvey.createChunk(cldrText.get("v_oldvotes_no_old"), "i")
              ); // TODO fix
            }
          } else {
            cldrStatus.setCurrentLocale(json.oldvotes.locale);
            updateHashAndMenus();
            var loclink;
            theDiv.appendChild(
              (loclink = cldrSurvey.createChunk(
                cldrText.get("v_oldvotes_return_to_locale_list"),
                "a",
                "notselected"
              ))
            );
            listenFor(loclink, "click", function (e) {
              cldrStatus.setCurrentLocale("");
              reloadV();
              cldrSurvey.stStopPropagation(e);
              return false;
            });
            theDiv.appendChild(
              cldrSurvey.createChunk(
                json.oldvotes.localeDisplayName,
                "h3",
                "v-title2"
              )
            );
            var oldVotesLocaleMsg = document.createElement("p");
            oldVotesLocaleMsg.className = "helpContent";
            oldVotesLocaleMsg.innerHTML = cldrText.sub(
              "v_oldvotes_locale_msg",
              {
                version: surveyLastVoteVersion,
                locale: json.oldvotes.localeDisplayName,
              }
            );
            theDiv.appendChild(oldVotesLocaleMsg);
            if (
              (json.oldvotes.contested && json.oldvotes.contested.length > 0) ||
              (json.oldvotes.uncontested &&
                json.oldvotes.uncontested.length > 0)
            ) {
              var frag = document.createDocumentFragment();
              const oldVoteCount =
                (json.oldvotes.contested ? json.oldvotes.contested.length : 0) +
                (json.oldvotes.uncontested
                  ? json.oldvotes.uncontested.length
                  : 0);
              var summaryMsg = cldrText.sub("v_oldvotes_count_msg", {
                count: oldVoteCount,
              });
              frag.appendChild(cldrSurvey.createChunk(summaryMsg, "div", ""));

              var navChunk = document.createElement("div");
              navChunk.className = "v-oldVotes-nav";
              frag.appendChild(navChunk);

              var uncontestedChunk = null;
              var contestedChunk = null;

              function addOldvotesType(type, jsondata, frag, navChunk) {
                var content = cldrSurvey.createChunk(
                  "",
                  "div",
                  "v-oldVotes-subDiv"
                );
                content.strid = "v_oldvotes_title_" + type; // v_oldvotes_title_contested or v_oldvotes_title_uncontested

                /* Normally this interface is for old "losing" (contested) votes only, since old "winning" (uncontested) votes
                 * are imported automatically. An exception is for TC users, for whom auto-import is disabled. The server-side
                 * code leaves json.oldvotes.uncontested undefined except for TC users.
                 * Show headings for "Winning/Losing" only if json.oldvotes.uncontested is defined and non-empty.
                 */
                if (
                  json.oldvotes.uncontested &&
                  json.oldvotes.uncontested.length > 0
                ) {
                  var title = cldrText.get(content.strid);
                  content.title = title;
                  content.appendChild(
                    cldrSurvey.createChunk(title, "h2", "v-oldvotes-sub")
                  );
                }

                content.appendChild(
                  cldrSurvey.showVoteTable(jsondata /* voteList */, type, json)
                );

                var submit = dojoxBusyButton({
                  label: cldrText.get("v_submit_msg"),
                  busyLabel: cldrText.get("v_submit_busy"),
                });

                submit.on("click", function (e) {
                  cldrSurvey.setDisplayed(navChunk, false);
                  var confirmList = []; // these will be revoted with current params

                  // explicit confirm list -  save us desync hassle
                  for (var kk in jsondata) {
                    if (jsondata[kk].box.checked) {
                      confirmList.push(jsondata[kk].strid);
                    }
                  }

                  var saveList = {
                    locale: cldrStatus.getCurrentLocale(),
                    confirmList: confirmList,
                  };

                  console.log(saveList.toString());
                  console.log(
                    "Submitting " +
                      type +
                      " " +
                      confirmList.length +
                      " for confirm"
                  );
                  const curLocale = cldrStatus.getCurrentLocale();
                  var url =
                    cldrStatus.getContextPath() +
                    "/SurveyAjax?what=oldvotes&_=" +
                    curLocale +
                    "&s=" +
                    cldrStatus.getSessionId() +
                    "&doSubmit=true&" +
                    cldrSurvey.cacheKill();
                  myLoad(
                    url,
                    "(submitting oldvotes " + curLocale + ")",
                    function (json) {
                      cldrSurvey.showLoader(cldrText.get("loading2"));
                      if (!verifyJson(json, "oldvotes")) {
                        cldrSurvey.handleDisconnect(
                          "Error submitting votes!",
                          json,
                          "Error"
                        );
                        return;
                      } else {
                        reloadV();
                      }
                    },
                    JSON.stringify(saveList),
                    {
                      "Content-Type": "application/json",
                    }
                  );
                });

                submit.placeAt(content);
                // hide by default
                cldrSurvey.setDisplayed(content, false);

                frag.appendChild(content);
                return content;
              }

              if (
                json.oldvotes.uncontested &&
                json.oldvotes.uncontested.length > 0
              ) {
                uncontestedChunk = addOldvotesType(
                  "uncontested",
                  json.oldvotes.uncontested,
                  frag,
                  navChunk
                );
              }
              if (
                json.oldvotes.contested &&
                json.oldvotes.contested.length > 0
              ) {
                contestedChunk = addOldvotesType(
                  "contested",
                  json.oldvotes.contested,
                  frag,
                  navChunk
                );
              }

              if (contestedChunk == null && uncontestedChunk != null) {
                cldrSurvey.setDisplayed(uncontestedChunk, true); // only item
              } else if (contestedChunk != null && uncontestedChunk == null) {
                cldrSurvey.setDisplayed(contestedChunk, true); // only item
              } else {
                // navigation
                navChunk.appendChild(
                  cldrSurvey.createChunk(cldrText.get("v_oldvotes_show"))
                );
                navChunk.appendChild(
                  cldrSurvey.createLinkToFn(
                    uncontestedChunk.strid,
                    function () {
                      cldrSurvey.setDisplayed(contestedChunk, false);
                      cldrSurvey.setDisplayed(uncontestedChunk, true);
                    },
                    "button"
                  )
                );
                navChunk.appendChild(
                  cldrSurvey.createLinkToFn(
                    contestedChunk.strid,
                    function () {
                      cldrSurvey.setDisplayed(contestedChunk, true);
                      cldrSurvey.setDisplayed(uncontestedChunk, false);
                    },
                    "button"
                  )
                );

                contestedChunk.appendChild(
                  cldrSurvey.createLinkToFn(
                    "v_oldvotes_hide",
                    function () {
                      cldrSurvey.setDisplayed(contestedChunk, false);
                    },
                    "button"
                  )
                );
                uncontestedChunk.appendChild(
                  cldrSurvey.createLinkToFn(
                    "v_oldvotes_hide",
                    function () {
                      cldrSurvey.setDisplayed(uncontestedChunk, false);
                    },
                    "button"
                  )
                );
              }

              theDiv.appendChild(frag);
            } else {
              theDiv.appendChild(
                cldrSurvey.createChunk(
                  cldrText.get("v_oldvotes_no_old_here"),
                  "i",
                  ""
                )
              );
            }
          }
        }
        cldrSurvey.hideLoader();
      });
    } else if (cldrStatus.getCurrentSpecial() == "mail") {
      var url =
        cldrStatus.getContextPath() +
        "/SurveyAjax?what=mail&s=" +
        cldrStatus.getSessionId() +
        "&fetchAll=true&" +
        cldrSurvey.cacheKill();
      myLoad(
        url,
        "(loading mail " + cldrStatus.getCurrentLocale() + ")",
        function (json) {
          cldrSurvey.hideLoader();
          isLoading = false;
          if (!verifyJson(json, "mail")) {
            return;
          } else {
            if (json.dataLoadTime) {
              cldrSurvey.updateIf("dynload", json.dataLoadTime);
            }

            var theDiv = flipper.flipToEmpty(pages.other); // clean slate, and proceed..

            cldrSurvey.removeAllChildNodes(theDiv);

            var listDiv = cldrSurvey.createChunk("", "div", "mailListChunk");
            var contentDiv = cldrSurvey.createChunk(
              "",
              "div",
              "mailContentChunk"
            );

            theDiv.appendChild(listDiv);
            theDiv.appendChild(contentDiv);

            cldrSurvey.setDisplayed(contentDiv, false);
            var header = json.mail.header;
            var data = json.mail.data;

            if (data.length == 0) {
              listDiv.appendChild(
                cldrSurvey.createChunk(
                  cldrText.get("mail_noMail"),
                  "p",
                  "helpContent"
                )
              );
            } else {
              for (var ii in data) {
                var row = data[ii];
                var li = cldrSurvey.createChunk(
                  row[header.QUEUE_DATE] + ": " + row[header.SUBJECT],
                  "li",
                  "mailRow"
                );
                if (row[header.READ_DATE]) {
                  cldrSurvey.addClass(li, "readMail");
                }
                if (header.USER !== undefined) {
                  li.appendChild(
                    document.createTextNode("(to " + row[header.USER] + ")")
                  );
                }
                if (row[header.SENT_DATE] !== false) {
                  li.appendChild(
                    cldrSurvey.createChunk("(sent)", "span", "winner")
                  );
                } else if (row[header.TRY_COUNT] >= 3) {
                  li.appendChild(
                    cldrSurvey.createChunk(
                      "(try#" + row[header.TRY_COUNT] + ")",
                      "span",
                      "loser"
                    )
                  );
                } else if (row[header.TRY_COUNT] > 0) {
                  li.appendChild(
                    cldrSurvey.createChunk(
                      "(try#" + row[header.TRY_COUNT] + ")",
                      "span",
                      "warning"
                    )
                  );
                }
                listDiv.appendChild(li);

                li.onclick = (function (li, row, header) {
                  return function () {
                    if (!row[header.READ_DATE]) {
                      myLoad(
                        cldrStatus.getContextPath() +
                          "/SurveyAjax?what=mail&s=" +
                          cldrStatus.getSessionId() +
                          "&markRead=" +
                          row[header.ID] +
                          "&" +
                          cldrSurvey.cacheKill(),
                        "Marking mail read",
                        function (json) {
                          if (!verifyJson(json, "mail")) {
                            return;
                          } else {
                            cldrSurvey.addClass(li, "readMail"); // mark as read when server answers
                            row[header.READ_DATE] = true; // close enough
                          }
                        }
                      );
                    }
                    cldrSurvey.setDisplayed(contentDiv, false);

                    cldrSurvey.removeAllChildNodes(contentDiv);

                    contentDiv.appendChild(
                      cldrSurvey.createChunk(
                        "Date: " + row[header.QUEUE_DATE],
                        "h2",
                        "mailHeader"
                      )
                    );
                    contentDiv.appendChild(
                      cldrSurvey.createChunk(
                        "Subject: " + row[header.SUBJECT],
                        "h2",
                        "mailHeader"
                      )
                    );
                    contentDiv.appendChild(
                      cldrSurvey.createChunk(
                        "Message-ID: " + row[header.ID],
                        "h2",
                        "mailHeader"
                      )
                    );
                    if (header.USER !== undefined) {
                      contentDiv.appendChild(
                        cldrSurvey.createChunk(
                          "To: " + row[header.USER],
                          "h2",
                          "mailHeader"
                        )
                      );
                    }
                    contentDiv.appendChild(
                      cldrSurvey.createChunk(
                        row[header.TEXT],
                        "p",
                        "mailContent"
                      )
                    );

                    cldrSurvey.setDisplayed(contentDiv, true);
                  };
                })(li, row, header);
              }
            }
          }
        }
      );
    } else if (cldrSurvey.isReport(cldrStatus.getCurrentSpecial())) {
      cldrSurvey.showLoader(null);
      cldrSurvey.showInPop2(
        cldrText.get("reportGuidance"),
        null,
        null,
        null,
        true,
        true
      ); /* show the box the first time */
      var url =
        cldrStatus.getContextPath() +
        "/SurveyAjax?what=report&x=" +
        cldrStatus.getCurrentSpecial() +
        "&_=" +
        cldrStatus.getCurrentLocale() +
        "&s=" +
        cldrStatus.getSessionId() +
        cldrSurvey.cacheKill();
      var errFunction = function errFunction(err) {
        console.log("Error: loading " + url + " -> " + err);
        cldrSurvey.hideLoader();
        isLoading = false;
        const html =
          "<div style='padding-top: 4em; font-size: x-large !important;' class='ferrorbox warning'>" +
          "<span class='icon i-stop'>" +
          " &nbsp; &nbsp;</span>Error: could not load: " +
          err +
          "</div>";
        const frag = cldrDomConstruct(html);
        flipper.flipTo(pages.other, frag);
      };
      if (cldrStatus.isDashboard()) {
        if (!cldrStatus.isVisitor()) {
          const loadHandler = function (json) {
            cldrSurvey.hideLoader();
            isLoading = false;
            // further errors are handled in JSON
            showReviewPage(json, function () {
              // show function - flip to the 'other' page.
              flipper.flipTo(pages.other, null);
            });
          };
          const xhrArgs = {
            url: url,
            handleAs: "json",
            load: loadHandler,
            error: errFunction,
          };
          cldrAjax.queueXhr(xhrArgs);
        } else {
          alert("Please login to access Dashboard");
          cldrStatus.setCurrentSpecial("");
          cldrStatus.setCurrentLocale("");
          reloadV();
        }
      } else {
        cldrSurvey.hideLoader();
        const loadHandler = function (html) {
          cldrSurvey.hideLoader();
          isLoading = false;
          const frag = cldrDomConstruct(html);
          flipper.flipTo(pages.other, frag);
          cldrEvent.hideRightPanel(); // CLDR-14365
        };
        const xhrArgs = {
          url: url,
          handleAs: "html",
          load: loadHandler,
          error: errFunction,
        };
        cldrAjax.queueXhr(xhrArgs);
      }
    } else if (cldrStatus.getCurrentSpecial() == "none") {
      // for now - redirect
      cldrSurvey.hideLoader();
      isLoading = false;
      window.location = cldrStatus.getSurvUrl(); // redirect home
    } else if (cldrStatus.getCurrentSpecial() == "locales") {
      cldrSurvey.hideLoader();
      isLoading = false;
      var theDiv = document.createElement("div");
      theDiv.className = "localeList";

      addTopLocale("root", theDiv);
      // top locales
      for (var n in locmap.locmap.topLocales) {
        var topLoc = locmap.locmap.topLocales[n];
        addTopLocale(topLoc, theDiv);
      }
      flipper.flipTo(pages.other, null);
      cldrEvent.filterAllLocale(); // filter for init data
      cldrEvent.forceSidebar();
      cldrStatus.setCurrentLocale(null);
      cldrStatus.setCurrentSpecial("locales");
      cldrSurvey.showInPop2(
        cldrText.get("localesInitialGuidance"),
        null,
        null,
        null,
        true
      ); /* show the box the first time */
      $("#itemInfo").html("");
    } else {
      otherSpecial.show(cldrStatus.getCurrentSpecial(), {
        flipper: flipper,
        pages: pages,
      });
    }
  } // end shower

  /**
   * Update the #hash and menus to the current settings.
   *
   * @param doPush {Boolean} if false, do not add to history
   */
  function updateHashAndMenus(doPush) {
    const sessionId = cldrStatus.getSessionId();
    const surveyUser = cldrStatus.getSurveyUser();
    const userID = surveyUser && surveyUser.id ? surveyUser.id : 0;
    const surveyUserPerms = cldrStatus.getPermissions();
    const surveyUserURL = {
      myAccountSetting: "survey?do=listu",
      disableMyAccount: "lock.jsp",
      recentActivity: "myvotes.jsp?user=" + userID + "&s=" + sessionId,
      xmlUpload: "upload.jsp?a=/cldr-apps/survey&s=" + sessionId,
      manageUser: "survey?do=list",
      flag: "tc-flagged.jsp?s=" + sessionId,
      about: "about.jsp",
      browse: "browse.jsp",
      adminPanel: "SurveyAjax?what=admin_panel&s=" + sessionId,
    };

    /**
     * 'name' - the js/special/___.js name
     * 'hidden' - true to hide the item
     * 'title' - override of menu name
     */
    var specialItems = new Array();
    if (surveyUser != null) {
      specialItems = [
        {
          divider: true,
        },

        {
          title: "Admin Panel",
          url: surveyUserURL.adminPanel,
          display: surveyUser && surveyUser.userlevelName === "ADMIN",
        },
        {
          divider: true,
          display: surveyUser && surveyUser.userlevelName === "ADMIN",
        },

        {
          title: "My Account",
        }, // My Account section

        {
          title: "Settings",
          level: 2,
          url: surveyUserURL.myAccountSetting,
          display: surveyUser && true,
        },
        {
          title: "Lock (Disable) My Account",
          level: 2,
          url: surveyUserURL.disableMyAccount,
          display: surveyUser && true,
        },

        {
          divider: true,
        },
        {
          title: "My Votes",
        }, // My Votes section

        /*
         * This indirectly references "special_oldvotes" in cldrText.js
         */
        {
          special: "oldvotes",
          level: 2,
          display: surveyUserPerms && surveyUserPerms.userCanImportOldVotes,
        },
        {
          title: "See My Recent Activity",
          level: 2,
          url: surveyUserURL.recentActivity,
        },
        {
          title: "Upload XML",
          level: 2,
          url: surveyUserURL.xmlUpload,
        },

        {
          divider: true,
        },
        {
          title: "My Organization(" + cldrStatus.getOrganizationName() + ")",
        }, // My Organization section

        {
          special: "vsummary" /* Cf. special_vsummary */,
          level: 2,
          display: surveyUserPerms && surveyUserPerms.userCanUseVettingSummary,
        },
        {
          title: "List " + cldrStatus.getOrganizationName() + " Users",
          level: 2,
          url: surveyUserURL.manageUser,
          display:
            surveyUserPerms &&
            (surveyUserPerms.userIsTC || surveyUserPerms.userIsVetter),
        },
        {
          special: "forum_participation" /* Cf. special_forum_participation */,
          level: 2,
          display: surveyUserPerms && surveyUserPerms.userCanMonitorForum,
        },
        {
          special:
            "vetting_participation" /* Cf. special_vetting_participation */,
          level: 2,
          display:
            surveyUserPerms &&
            (surveyUserPerms.userIsTC || surveyUserPerms.userIsVetter),
        },
        {
          title: "LOCKED: Note: your account is currently locked.",
          level: 2,
          display: surveyUserPerms && surveyUserPerms.userIsLocked,
          bold: true,
        },

        {
          divider: true,
        },
        {
          title: "Forum",
        }, // Forum section

        {
          special: "flagged",
          level: 2,
          hasFlag: true,
        },
        {
          special: "mail",
          level: 2,
          display: cldrStatus.getIsUnofficial(),
        },
        {
          special: "bulk_close_posts" /* Cf. special_bulk_close_posts */,
          level: 2,
          display: surveyUser && surveyUser.userlevelName === "ADMIN",
        },

        {
          divider: true,
        },
        {
          title: "Informational",
        }, // Informational section

        {
          special: "statistics",
          level: 2,
        },
        {
          title: "About",
          level: 2,
          url: surveyUserURL.about,
        },
        {
          title: "Lookup a code or xpath",
          level: 2,
          url: surveyUserURL.browse,
        },
        {
          title: "Error Subtypes",
          level: 2,
          url: "./tc-all-errors.jsp",
          display: surveyUserPerms && surveyUserPerms.userIsTC,
        },
        {
          divider: true,
        },
      ];
    }
    if (!doPush) {
      doPush = false;
    }
    replaceHash(doPush); // update the hash
    updateLocaleMenu();

    if (cldrStatus.getCurrentLocale() == null) {
      menubuttons.set(menubuttons.section);
      const curSpecial = cldrStatus.getCurrentSpecial();
      if (curSpecial != null) {
        var specialId = "special_" + curSpecial;
        menubuttons.set(menubuttons.page, cldrText.get(specialId));
      } else {
        menubuttons.set(menubuttons.page);
      }
      return; // nothing to do.
    }
    var titlePageContainer = document.getElementById("title-page-container");

    /**
     * Just update the titles of the menus. Internal to updateHashAndMenus
     */
    function updateMenuTitles(menuMap) {
      if (menubuttons.lastspecial === undefined) {
        menubuttons.lastspecial = null;

        // Set up the menu here?
        var parMenu = document.getElementById("manage-list");
        for (var k = 0; k < specialItems.length; k++) {
          var item = specialItems[k];
          (function (item) {
            if (item.display != false) {
              var subLi = document.createElement("li");
              if (item.special) {
                // special items so look up in cldrText.js
                item.title = cldrText.get("special_" + item.special);
                item.url = "#" + item.special;
                item.blank = false;
              }
              if (item.url) {
                var subA = document.createElement("a");

                if (item.hasFlag) {
                  // forum may need images attached to it
                  var Img = document.createElement("img");
                  Img.setAttribute("src", "flag.png");
                  Img.setAttribute("alt", "flag");
                  Img.setAttribute("title", "flag.png");
                  Img.setAttribute("border", 0);

                  subA.appendChild(Img);
                }
                subA.appendChild(document.createTextNode(item.title + " "));
                subA.href = item.url;

                if (item.blank != false) {
                  subA.target = "_blank";
                  subA.appendChild(
                    cldrSurvey.createChunk(
                      "",
                      "span",
                      "glyphicon glyphicon-share manage-list-icon"
                    )
                  );
                }

                if (item.level) {
                  // append it to appropriate levels
                  var level = item.level;
                  for (var i = 0; i < level - 1; i++) {
                    /*
                     * Indent by creating lists within lists, each list containing only one item.
                     * TODO: indent by a better method. Note that for valid html, ul should contain li;
                     * ul directly containing element other than li is generally invalid.
                     */
                    let ul = document.createElement("ul");
                    let li = document.createElement("li");
                    ul.setAttribute("style", "list-style-type:none");
                    ul.appendChild(li);
                    li.appendChild(subA);
                    subA = ul;
                  }
                }
                subLi.appendChild(subA);
              }
              if (!item.url && !item.divider) {
                // if it is pure text/html & not a divider
                if (!item.level) {
                  subLi.appendChild(document.createTextNode(item.title + " "));
                } else {
                  var subA = null;
                  if (item.bold) {
                    subA = document.createElement("b");
                  } else if (item.italic) {
                    subA = document.createElement("i");
                  } else {
                    subA = document.createElement("span");
                  }
                  subA.appendChild(document.createTextNode(item.title + " "));

                  var level = item.level;
                  for (var i = 0; i < level - 1; i++) {
                    let ul = document.createElement("ul");
                    let li = document.createElement("li");
                    ul.setAttribute("style", "list-style-type:none");
                    ul.appendChild(li);
                    li.appendChild(subA);
                    subA = ul;
                  }
                  subLi.appendChild(subA);
                }
              }
              if (item.divider) {
                subLi.className = "nav-divider";
              }
              parMenu.appendChild(subLi);
            }
          })(item);
        }
      }

      if (menubuttons.lastspecial) {
        cldrSurvey.removeClass(menubuttons.lastspecial, "selected");
      }

      updateLocaleMenu(menuMap);
      const curSpecial = cldrStatus.getCurrentSpecial();
      if (curSpecial != null && curSpecial != "") {
        var specialId = "special_" + curSpecial;
        $("#section-current").html(cldrText.get(specialId));
        cldrSurvey.setDisplayed(titlePageContainer, false);
      } else if (!menuMap) {
        cldrSurvey.setDisplayed(titlePageContainer, false);
      } else {
        const curPage = cldrStatus.getCurrentPage();
        if (menuMap.sectionMap[curPage]) {
          const curSection = curPage; // section = page
          cldrStatus.setCurrentSection(curSection);
          $("#section-current").html(menuMap.sectionMap[curSection].name);
          cldrSurvey.setDisplayed(titlePageContainer, false); // will fix title later
        } else if (menuMap.pageToSection[curPage]) {
          var mySection = menuMap.pageToSection[curPage];
          cldrStatus.setCurrentSection(mySection.id);
          $("#section-current").html(mySection.name);
          cldrSurvey.setDisplayed(titlePageContainer, false); // will fix title later
        } else {
          $("#section-current").html(cldrText.get("section_general"));
          cldrSurvey.setDisplayed(titlePageContainer, false);
        }
      }
    }

    /**
     * Update the menus
     */
    function updateMenus(menuMap) {
      // initialize menus
      if (!menuMap.menusSetup) {
        menuMap.menusSetup = true;
        menuMap.setCheck = function (menu, checked, disabled) {
          if (menu) {
            menu.set(
              "iconClass",
              checked ? "dijitMenuItemIcon menu-x" : "dijitMenuItemIcon menu-o"
            );
            menu.set("disabled", disabled);
          }
        };
        var menuSection = pseudoDijitRegistryById("menu-section");
        menuMap.section_general = newPseudoDijitMenuItem({
          label: cldrText.get("section_general"),
          iconClass: "dijitMenuItemIcon ",
          disabled: true,
          onClick: function () {
            if (
              cldrStatus.getCurrentPage() != "" ||
              (cldrStatus.getCurrentSpecial() != "" &&
                cldrStatus.getCurrentSpecial() != null)
            ) {
              cldrStatus.setCurrentId(""); // no id if jumping pages
              cldrStatus.setCurrentPage("");
              cldrStatus.setCurrentSection("");
              cldrStatus.setCurrentSpecial("");
              updateMenuTitles(menuMap);
              reloadV();
            }
          },
        });
        if (menuSection) {
          menuSection.addChild(menuMap.section_general);
        }
        for (var j in menuMap.sections) {
          (function (aSection) {
            aSection.menuItem = newPseudoDijitMenuItem({
              label: aSection.name,
              iconClass: "dijitMenuItemIcon",
              onClick: function () {
                cldrStatus.setCurrentId("!"); // no id if jumping pages
                cldrStatus.setCurrentPage(aSection.id);
                cldrStatus.setCurrentSpecial("");
                updateMenus(menuMap);
                updateMenuTitles(menuMap);
                reloadV();
              },
              disabled: true,
            });
            if (menuSection) {
              menuSection.addChild(aSection.menuItem);
            }
          })(menuMap.sections[j]);
        }

        if (menuSection) {
          menuSection.addChild(new dijitMenuSeparator());
        }
        menuMap.forumMenu = newPseudoDijitMenuItem({
          label: cldrText.get("section_forum"),
          iconClass: "dijitMenuItemIcon", // menu-chat
          disabled: true,
          onClick: function () {
            cldrStatus.setCurrentId("!"); // no id if jumping pages
            cldrStatus.setCurrentPage("");
            cldrStatus.setCurrentSpecial("forum");
            updateMenus(menuMap);
            updateMenuTitles(menuMap);
            reloadV();
          },
        });
        if (menuSection) {
          menuSection.addChild(menuMap.forumMenu);
        }
      }

      updateMenuTitles(menuMap);

      var myPage = null;
      var mySection = null;
      const curSpecial = cldrStatus.getCurrentSpecial();
      if (curSpecial == null || curSpecial == "") {
        // first, update display names
        const curPage = cldrStatus.getCurrentPage();
        if (menuMap.sectionMap[curPage]) {
          // page is really a section
          mySection = menuMap.sectionMap[curPage];
          myPage = null;
        } else if (menuMap.pageToSection[curPage]) {
          mySection = menuMap.pageToSection[curPage];
          myPage = mySection.pageMap[curPage];
        }
        if (mySection !== null) {
          // update menus under 'page' - peer pages
          if (!titlePageContainer.menus) {
            titlePageContainer.menus = {};
          }

          // hide all. TODO use a foreach model?
          for (var zz in titlePageContainer.menus) {
            var aMenu = titlePageContainer.menus[zz];
            if (aMenu) {
              aMenu.set("label", "-");
            } else {
              console.log("warning: aMenu is falsy in updateMenus");
            }
          }

          var showMenu = titlePageContainer.menus[mySection.id];

          if (!showMenu) {
            // doesn't exist - add it.
            var menuPage = newPseudoDijitDropDownMenu();
            for (var k in mySection.pages) {
              // use given order
              (function (aPage) {
                var pageMenu = (aPage.menuItem = newPseudoDijitMenuItem({
                  label: aPage.name,
                  iconClass:
                    aPage.id == cldrStatus.getCurrentPage()
                      ? "dijitMenuItemIcon menu-x"
                      : "dijitMenuItemIcon menu-o",
                  onClick: function () {
                    cldrStatus.setCurrentId(""); // no id if jumping pages
                    cldrStatus.setCurrentPage(aPage.id);
                    updateMenuTitles(menuMap);
                    reloadV();
                  },
                  disabled:
                    cldrSurvey.effectiveCoverage() <
                    parseInt(aPage.levs[cldrStatus.getCurrentLocale()]),
                }));
              })(mySection.pages[k]);
            }

            showMenu = newPseudoDijitDropDownButton({
              label: "-",
              dropDown: menuPage,
            });

            titlePageContainer.menus[
              mySection.id
            ] = mySection.pagesMenu = showMenu;
          }

          if (myPage !== null) {
            $("#title-page-container")
              .html("<h1>" + myPage.name + "</h1>")
              .show();
          } else {
            $("#title-page-container").html("").hide();
          }
          cldrSurvey.setDisplayed(showMenu, true);
          cldrSurvey.setDisplayed(titlePageContainer, true); // will fix title later
        }
      }

      menuMap.setCheck(
        menuMap.section_general,
        cldrStatus.getCurrentPage() == "" &&
          (cldrStatus.getCurrentSpecial() == "" ||
            cldrStatus.getCurrentSpecial() == null),
        false
      );

      // Update the status of the items in the Section menu
      for (var j in menuMap.sections) {
        var aSection = menuMap.sections[j];
        // need to see if any items are visible @ current coverage
        const curLocale = cldrStatus.getCurrentLocale();
        const curSection = cldrStatus.getCurrentSection();
        menuMap.setCheck(
          aSection.menuItem,
          curSection == aSection.id,
          cldrSurvey.effectiveCoverage() < aSection.minLev[curLocale]
        );

        // update the items in that section's Page menu
        if (curSection == aSection.id) {
          for (var k in aSection.pages) {
            var aPage = aSection.pages[k];
            if (!aPage.menuItem) {
              console.log("Odd - " + aPage.id + " has no menuItem");
            } else {
              menuMap.setCheck(
                aPage.menuItem,
                aPage.id == cldrStatus.getCurrentPage(),
                cldrSurvey.effectiveCoverage() < parseInt(aPage.levs[curLocale])
              );
            }
          }
        }
      }
      menuMap.setCheck(
        menuMap.forumMenu,
        cldrStatus.getCurrentSpecial() == "forum",
        cldrStatus.getSurveyUser() === null
      );
      cldrEvent.resizeSidebar();
    }

    const curLocale = cldrStatus.getCurrentLocale();
    if (_thePages == null || _thePages.loc != curLocale) {
      // show the raw IDs while loading.
      updateMenuTitles(null);

      if (curLocale != null && curLocale != "") {
        var needLocTable = false;

        var url =
          cldrStatus.getContextPath() +
          "/SurveyAjax?what=menus&_=" +
          curLocale +
          "&locmap=" +
          needLocTable +
          "&s=" +
          cldrStatus.getSessionId() +
          cldrSurvey.cacheKill();
        myLoad(url, "menus", function (json) {
          if (!verifyJson(json, "menus")) {
            return; // busted?
          }

          if (json.locmap) {
            locmap = new LocaleMap(locmap); // overwrite with real data
          }

          // make this into a hashmap.
          if (json.canmodify) {
            for (var k in json.canmodify) {
              canmodify[json.canmodify[k]] = true;
            }
          }

          cldrSurvey.updateCovFromJson(json);
          updateCoverageMenuTitle();
          cldrSurvey.updateCoverage(flipper.get(pages.data)); // update CSS and auto menu title
          unpackMenus(json);
          cldrEvent.unpackMenuSideBar(json);
          updateMenus(_thePages);
        });
      }
    } else {
      // go ahead and update
      updateMenus(_thePages);
    }
  } // updateHashAndMenus

  function unpackMenus(json) {
    var menus = json.menus;

    if (_thePages) {
      for (var k in menus.sections) {
        var oldSection = _thePages.sectionMap[menus.sections[k].id];
        for (var j in menus.sections[k].pages) {
          var oldPage = oldSection.pageMap[menus.sections[k].pages[j].id];

          // copy over levels
          oldPage.levs[json.loc] = menus.sections[k].pages[j].levs[json.loc];
        }
      }
    } else {
      // set up some hashes
      menus.haveLocs = {};
      menus.sectionMap = {};
      menus.pageToSection = {};
      for (var k in menus.sections) {
        menus.sectionMap[menus.sections[k].id] = menus.sections[k];
        menus.sections[k].pageMap = {};
        menus.sections[k].minLev = {};
        for (var j in menus.sections[k].pages) {
          menus.sections[k].pageMap[menus.sections[k].pages[j].id] =
            menus.sections[k].pages[j];
          menus.pageToSection[menus.sections[k].pages[j].id] =
            menus.sections[k];
        }
      }
      _thePages = menus;
    }

    for (var k in _thePages.sectionMap) {
      var min = 200;
      for (var j in _thePages.sectionMap[k].pageMap) {
        var thisLev = parseInt(
          _thePages.sectionMap[k].pageMap[j].levs[json.loc]
        );
        if (min > thisLev) {
          min = thisLev;
        }
      }
      _thePages.sectionMap[k].minLev[json.loc] = min;
    }

    _thePages.haveLocs[json.loc] = true;
  }

  function trimNull(x) {
    if (x == null) {
      return "";
    }
    try {
      x = x.toString().trim();
    } catch (e) {
      // do nothing
    }
    return x;
  }

  /**
   * Uppercase the first letter of a sentence
   * @return {String} string with first letter uppercase
   */
  function ucFirst(s) {
    return s.charAt(0).toUpperCase() + s.slice(1);
  }

  // replacement for dojo/dom-construct domConstruct.toDom
  function cldrDomConstruct(html) {
    const renderer = document.createElement("template");
    renderer.innerHTML = html;
    return renderer.content;
  }

  /**
   * Automatically import old winning votes
   */
  function doAutoImport() {
    var autoImportProgressDialog = newProgressDialog({
      title: cldrText.get("v_oldvote_auto_msg"),
      content: cldrText.get("v_oldvote_auto_progress_msg"),
    });
    if (autoImportProgressDialog) {
      autoImportProgressDialog.show();
    }
    haveDialog = true;
    cldrEvent.hideOverlayAndSidebar();
    /*
     * See WHAT_AUTO_IMPORT = "auto_import" in SurveyAjax.java
     */
    var url =
      cldrStatus.getContextPath() +
      "/SurveyAjax?what=auto_import&s=" +
      cldrStatus.getSessionId() +
      cldrSurvey.cacheKill();
    myLoad(url, "auto-importing votes", function (json) {
      if (autoImportProgressDialog) {
        autoImportProgressDialog.hide();
      }
      haveDialog = false;
      if (json.autoImportedOldWinningVotes) {
        var vals = {
          count: json.autoImportedOldWinningVotes,
        };
        var autoImportedDialog = newProgressDialog({
          title: cldrText.get("v_oldvote_auto_msg"),
          content: cldrText.sub("v_oldvote_auto_desc_msg", vals),
        });
        if (autoImportedDialog) {
          autoImportedDialog.addChild(
            new dijitButton({
              label: "OK",
              onClick: function () {
                haveDialog = false;
                autoImportedDialog.hide();
                reloadV();
              },
            })
          );
          autoImportedDialog.show();
        }
        haveDialog = true;
        cldrEvent.hideOverlayAndSidebar();
      }
    });
  }

  /**
   * @param postData optional - makes this a POST
   */
  function myLoad(url, message, handler, postData, headers) {
    const otime = new Date().getTime();
    console.log("MyLoad: " + url + " for " + message);
    const errorHandler = function (err) {
      console.log("Error: " + err);
      cldrSurvey.handleDisconnect(
        "Could not fetch " +
          message +
          " - error " +
          err +
          "\n url: " +
          url +
          "\n",
        null,
        "disconnect"
      );
    };
    const loadHandler = function (json) {
      console.log(
        "        " + url + " loaded in " + (new Date().getTime() - otime) + "ms"
      );
      try {
        handler(json);
        //resize height
        $("#main-row").css({
          height: $("#main-row>div").height(),
        });
      } catch (e) {
        console.log(
          "Error in ajax post [" + message + "]  " + e.message + " / " + e.name
        );
        cldrSurvey.handleDisconnect(
          "Exception while loading: " +
            message +
            " - " +
            e.message +
            ", n=" +
            e.name +
            " \nStack:\n" +
            (e.stack || "[none]"),
          null
        ); // in case the 2nd line doesn't work
      }
    };
    const xhrArgs = {
      url: url,
      handleAs: "json",
      load: loadHandler,
      error: errorHandler,
      postData: postData,
      headers: headers,
    };
    cldrAjax.queueXhr(xhrArgs);
  }

  function addSubLocales(parLocDiv, subLocInfo) {
    if (subLocInfo.sub) {
      for (var n in subLocInfo.sub) {
        var subLoc = subLocInfo.sub[n];
        addSubLocale(parLocDiv, subLoc);
      }
    }
  }

  function addSubLocale(parLocDiv, subLoc) {
    var subLocInfo = locmap.getLocaleInfo(subLoc);
    var subLocDiv = cldrSurvey.createChunk(null, "div", "subLocale");
    appendLocaleLink(subLocDiv, subLoc, subLocInfo);

    parLocDiv.appendChild(subLocDiv);
  }

  function appendLocaleLink(subLocDiv, subLoc, subInfo, fullTitle) {
    var name = locmap.getRegionAndOrVariantName(subLoc);
    if (fullTitle) {
      name = locmap.getLocaleName(subLoc);
    }
    var clickyLink = cldrSurvey.createChunk(name, "a", "locName");
    clickyLink.href = linkToLocale(subLoc);
    subLocDiv.appendChild(clickyLink);
    if (subInfo == null) {
      console.log("* internal: subInfo is null for " + name + " / " + subLoc);
    }
    if (subInfo.name_var) {
      cldrSurvey.addClass(clickyLink, "name_var");
    }
    clickyLink.title = subLoc; // remove auto generated "locName.title"

    if (subInfo.readonly) {
      cldrSurvey.addClass(clickyLink, "locked");
      cldrSurvey.addClass(subLocDiv, "hide");

      if (subInfo.special_comment) {
        clickyLink.title = subInfo.special_comment;
      } else if (subInfo.dcChild) {
        clickyLink.title = cldrText.sub("defaultContentChild_msg", {
          name: subInfo.name,
          dcChild: subInfo.dcChild,
          dcChildName: locmap.getLocaleName(subInfo.dcChild),
        });
      } else {
        clickyLink.title = cldrText.get("readonlyGuidance");
      }
    } else if (subInfo.special_comment) {
      // could be the sandbox locale, or some other comment.
      clickyLink.title = subInfo.special_comment;
    }

    if (canmodify && subLoc in canmodify) {
      cldrSurvey.addClass(clickyLink, "canmodify");
    } else {
      cldrSurvey.addClass(subLocDiv, "hide"); // not modifiable
    }
    return clickyLink;
  }

  function getThePages() {
    return _thePages;
  }

  function getTheLocaleMap() {
    return locmap;
  }

  // getHash and setHash are replacements for dojo/hash
  // https://dojotoolkit.org/reference-guide/1.10/dojo/hash.html
  // return "locales///";

  /**
   * Get the window location hash
   *
   * For example, if the current URL is "https:...#bar", return "bar".
   *
   * Typically the first value we return is "locales///"
   */
  function getHash() {
    // https://developer.mozilla.org/en-US/docs/Web/API/URL/hash
    // https://developer.mozilla.org/en-US/docs/Web/API/Window/location
    let hash = sliceHash(window.location.hash);
    console.log("getHash returning " + hash);
    return hash;
  }

  /**
   * Set the window location hash
   *
   * ... TODO: implement setHash with "replace"
   */
  function setHash(newHash, replace) {
    // To manipulate the value of the hash, simply call dojo/hash with the new value.
    // It will be added to the browser history stack and it will publish a /dojo/hashchange topic,
    // triggering anything subscribed

    // In order to not to add to the history stack, pass true as the second parameter (replace).
    // This will update the current browser URL and replace the current history state

    newHash = sliceHash(newHash);
    if (newHash !== sliceHash(window.location.hash)) {
      const oldUrl = window.location.href;
      const newUrl = oldUrl.split("#")[0] + "#" + newHash;
      console.log(
        "setHash going to " +
          newUrl +
          " - Called with: " +
          newHash +
          " - " +
          replace
      );
      window.location.href = newUrl;
    }
  }

  // given "#foo" or "foo", return "foo"
  function sliceHash(hash) {
    return hash.charAt(0) === "#" ? hash.slice(1) : hash;
  }

  // Note: "ARI" probably stands for "Abort, Retry, Ignore".
  function ariRetry() {
    ariDialogHide();
    window.location.reload(true);
  }

  function ariDialogShow() {
    // TODO: implement a replacement for dijit/Dialog
    // https://dojotoolkit.org/reference-guide/1.10/dijit/Dialog.html
    // "<div data-dojo-type='dijit/Dialog' data-dojo-id='ariDialog' title=...
    console.log("ariDialogShow not implemented yet!");
  }

  function ariDialogHide() {
    console.log("ariDialogHide not implemented yet!");
  }

  function dialogIsOpen() {
    return haveDialog;
  }

  function pseudoDijitRegistryById(id) {
    // TODO: implement a replacement for dijit/Registry byId()
    // https://dojotoolkit.org/reference-guide/1.10/dijit/registry.html
    console.log("pseudoDijitRegistryById not implemented yet! id = " + id);
    return null;
  }

  function newProgressDialog(args) {
    // TODO: implement a replacement for dijit/Dialog (or something simpler)
    console.log("newProgressDialog not implemented yet! args = " + args);
    return null;
  }

  function newPseudoDijitMenuItem(args) {
    // TODO: implement a replacement for dijit/MenuItem (or something simpler)
    console.log("newPseudoDijitMenuItem not implemented yet! args = " + args);
    return null;
  }

  function newPseudoDijitDropDownMenu(args) {
    // TODO: implement a replacement for dijit/DropDownMenu (or something simpler)
    console.log(
      "newPseudoDijitDropDownMenu not implemented yet! args = " + args
    );
    return null;
  }

  function newPseudoDijitDropDownButton(args) {
    // TODO: implement a replacement for dijit/DropDownButton (or something simpler)
    console.log(
      "newPseudoDijitDropDownButton not implemented yet! args = " + args
    );
    return null;
  }

  /*
   * Make only these functions accessible from other files:
   */
  return {
    showV: showV,
    reloadV: reloadV,
    getThePages: getThePages,
    getTheLocaleMap: getTheLocaleMap,
    myLoad: myLoad,
    appendLocaleLink: appendLocaleLink,
    replaceHash: replaceHash,
    showCurrentId: showCurrentId,
    insertLocaleSpecialNote: insertLocaleSpecialNote,
    ariRetry: ariRetry,
    ariDialogShow: ariDialogShow,
    dialogIsOpen: dialogIsOpen,

    /*
     * The following are meant to be accessible for unit testing only:
     */
    // test: {
    //   f: f,
    // },
  };
})();

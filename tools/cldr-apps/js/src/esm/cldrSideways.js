/*
 * cldrSideways: encapsulate Survey Tool code for the "sideways" menu,
 * which enables switching and comparing between a set of related locales,
 * such as: aa = Afar, aa_DJ = Afar (Djibouti), and aa_ER = Afar (Eritrea)
 */
import * as cldrCache from "./cldrCache.mjs";
import * as cldrDom from "./cldrDom.js";
import * as cldrEvent from "./cldrEvent.js";
import * as cldrLoad from "./cldrLoad.js";
import * as cldrStatus from "./cldrStatus.js";
import * as cldrSurvey from "./cldrSurvey.js";
import * as cldrText from "./cldrText.js";

/**
 * Array storing all only-1 sublocale
 */
const oneLocales = [];

/**
 * Timeout for showing sideways view
 */
let sidewaysShowTimeout = -1;

const sidewaysCache = new cldrCache.LRU();

const SIDEWAYS_AREA_CLASS = "sidewaysArea";

function loadMenu(frag, xpstrid) {
  const curLocale = cldrStatus.getCurrentLocale();
  if (!curLocale || oneLocales[curLocale]) {
    return;
  }
  const cachedData = sidewaysCache.get(makeCacheKey(curLocale, xpstrid));
  if (cachedData) {
    const sidewaysControl = document.createElement("div");
    sidewaysControl.className = SIDEWAYS_AREA_CLASS;
    frag.appendChild(sidewaysControl);
    setMenuFromData(sidewaysControl, cachedData);
  } else {
    fetchAndLoadMenu(frag, curLocale, xpstrid);
  }
}

function fetchAndLoadMenu(frag, curLocale, xpstrid) {
  const sidewaysControl = cldrDom.createChunk(
    cldrText.get("sideways_loading0"),
    "div",
    SIDEWAYS_AREA_CLASS
  );
  frag.appendChild(sidewaysControl);
  clearMyTimeout();
  sidewaysShowTimeout = window.setTimeout(function () {
    clearMyTimeout();
    if (
      curLocale !== cldrStatus.getCurrentLocale() ||
      xpstrid !== cldrStatus.getCurrentId()
    ) {
      return;
    }
    cldrDom.updateIf(sidewaysControl, cldrText.get("sideways_loading1"));
    cldrLoad.myLoad(
      getSidewaysUrl(curLocale, xpstrid),
      "sidewaysView",
      function (json) {
        sidewaysCache.set(makeCacheKey(curLocale, xpstrid), json);
        setMenuFromData(sidewaysControl, json);
      }
    );
  }, 2000); // wait 2 seconds before loading this.
}

function clearMyTimeout() {
  if (sidewaysShowTimeout != -1) {
    // https://www.w3schools.com/jsref/met_win_clearinterval.asp
    window.clearInterval(sidewaysShowTimeout);
    sidewaysShowTimeout = -1;
  }
}

function getSidewaysUrl(curLocale, xpstrid) {
  return (
    cldrStatus.getContextPath() +
    "/SurveyAjax?what=getsideways&_=" +
    curLocale +
    "&s=" +
    cldrStatus.getSessionId() +
    "&xpath=" +
    xpstrid +
    cldrSurvey.cacheKill()
  );
}

function setMenuFromData(sidewaysControl, json) {
  /*
   * Count the number of unique locales in json.others and json.novalue.
   */
  var relatedLocales = json.novalue.slice();
  for (var s in json.others) {
    for (var t in json.others[s]) {
      relatedLocales[json.others[s][t]] = true;
    }
  }
  // if there is 1 sublocale (+ 1 default), we do nothing
  if (Object.keys(relatedLocales).length <= 2) {
    oneLocales[cldrStatus.getCurrentLocale()] = true;
    cldrDom.updateIf(sidewaysControl, "");
  } else {
    if (!json.others) {
      cldrDom.updateIf(sidewaysControl, ""); // no sibling locales (or all null?)
    } else {
      cldrDom.updateIf(sidewaysControl, ""); // remove string

      var topLocale = json.topLocale;
      const locmap = cldrLoad.getTheLocaleMap();
      var curLocale = locmap.getRegionAndOrVariantName(topLocale);
      var readLocale = null;

      // merge the read-only sublocale to base locale
      var mergeReadBase = function mergeReadBase(list) {
        var baseValue = null;
        // find the base locale, remove it and store its value
        for (var l = 0; l < list.length; l++) {
          var loc = list[l][0];
          if (loc === topLocale) {
            baseValue = list[l][1];
            list.splice(l, 1);
            break;
          }
        }

        // replace the default locale(read-only) with base locale, store its name for label
        for (var l = 0; l < list.length; l++) {
          var loc = list[l][0];
          var bund = locmap.getLocaleInfo(loc);
          if (bund && bund.readonly) {
            readLocale = locmap.getRegionAndOrVariantName(loc);
            list[l][0] = topLocale;
            list[l][1] = baseValue;
            break;
          }
        }
      };

      // compare all sublocale values
      var appendLocaleList = function appendLocaleList(list, curValue) {
        var group = document.createElement("optGroup");
        var br = document.createElement("optGroup");
        group.appendChild(br);

        group.setAttribute("label", "Regional Variants for " + curLocale);
        group.setAttribute("title", "Regional Variants for " + curLocale);

        var escape = "\u00A0\u00A0\u00A0";
        var unequalSign = "\u2260\u00A0";

        for (var l = 0; l < list.length; l++) {
          var loc = list[l][0];
          var title = list[l][1];
          var item = document.createElement("option");
          item.setAttribute("value", loc);
          if (title == null) {
            item.setAttribute("title", "undefined");
          } else {
            item.setAttribute("title", title);
          }

          var str = locmap.getRegionAndOrVariantName(loc);
          if (loc === topLocale) {
            str = str + " (= " + readLocale + ")";
          }

          if (loc === cldrStatus.getCurrentLocale()) {
            str = escape + str;
            item.setAttribute("selected", "selected");
            item.setAttribute("disabled", "disabled");
          } else if (title != curValue) {
            str = unequalSign + str;
          } else {
            str = escape + str;
          }
          item.appendChild(document.createTextNode(str));
          group.appendChild(item);
        }
        popupSelect.appendChild(group);
      };

      var dataList = [];

      var popupSelect = document.createElement("select");
      for (var s in json.others) {
        for (var t in json.others[s]) {
          dataList.push([json.others[s][t], s]);
        }
      }

      /*
       * Set curValue = the value for cldrStatus.getCurrentLocale()
       */
      var curValue = null;
      for (let l = 0; l < dataList.length; l++) {
        var loc = dataList[l][0];
        if (loc === cldrStatus.getCurrentLocale()) {
          curValue = dataList[l][1];
          break;
        }
      }
      /*
       * Force the use of unequalSign in the regional comparison pop-up for locales in
       * json.novalue, by assigning a value that's different from curValue.
       */
      if (json.novalue) {
        const differentValue = curValue === "A" ? "B" : "A"; // anything different from curValue
        for (s in json.novalue) {
          dataList.push([json.novalue[s], differentValue]);
        }
      }
      mergeReadBase(dataList);

      // then sort by sublocale name
      dataList = dataList.sort(function (a, b) {
        return (
          locmap.getRegionAndOrVariantName(a[0]) >
          locmap.getRegionAndOrVariantName(b[0])
        );
      });
      appendLocaleList(dataList, curValue);

      var group = document.createElement("optGroup");
      popupSelect.appendChild(group);

      cldrDom.listenFor(popupSelect, "change", function (e) {
        var newLoc = popupSelect.value;
        if (newLoc !== cldrStatus.getCurrentLocale()) {
          cldrStatus.setCurrentLocale(newLoc);
          cldrLoad.reloadV();
        }
        return cldrEvent.stopPropagation(e);
      });

      sidewaysControl.appendChild(popupSelect);
    }
  }
}

function makeCacheKey(curLocale, xpstrid) {
  return curLocale + "-" + xpstrid;
}

function clearCache() {
  sidewaysCache.clear();
}

export { clearCache, loadMenu };

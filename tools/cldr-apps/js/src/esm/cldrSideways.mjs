/*
 * cldrSideways: encapsulate Survey Tool code for the "sideways" menu,
 * which enables switching and comparing between a set of related locales,
 * such as: aa = Afar, aa_DJ = Afar (Djibouti), and aa_ER = Afar (Eritrea)
 */
import * as cldrCache from "./cldrCache.mjs";
import * as cldrLoad from "./cldrLoad.mjs";
import * as cldrStatus from "./cldrStatus.mjs";
import * as cldrSurvey from "./cldrSurvey.mjs";

const SIDEWAYS_DEBUG = true;

const escape = "\u00A0\u00A0\u00A0"; // non-breaking spaces
const unequalSign = "\u2260\u00A0"; // U+2260 = "≠"

/**
 * Array storing all only-1 sublocales
 */
const oneLocales = [];

/**
 * Timeout for showing sideways view
 */
let sidewaysShowTimeout = -1;

const sidewaysCache = new cldrCache.LRU();

function loadMenu(regionalVariantsWrapper, xpstrid) {
  const curLocale = cldrStatus.getCurrentLocale();
  if (!curLocale || oneLocales[curLocale] || !xpstrid) {
    regionalVariantsWrapper.setData(null);
    if (SIDEWAYS_DEBUG) {
      console.log("cldrSideways.loadMenu, nothing to display");
    }
    return;
  }
  const cacheKey = makeCacheKey(curLocale, xpstrid);
  const cachedData = sidewaysCache.get(cacheKey);
  if (cachedData) {
    if (SIDEWAYS_DEBUG) {
      console.log("cldrSideways.loadMenu, using cached data");
    }
    regionalVariantsWrapper.setData(cachedData);
  } else {
    if (SIDEWAYS_DEBUG) {
      console.log("cldrSideways.loadMenu, fetching new data");
    }
    fetchAndLoadMenu(regionalVariantsWrapper, curLocale, xpstrid, cacheKey);
  }
}

function fetchAndLoadMenu(
  regionalVariantsWrapper,
  curLocale,
  xpstrid,
  cacheKey
) {
  clearMyTimeout();
  regionalVariantsWrapper.setLoading();
  sidewaysShowTimeout = window.setTimeout(function () {
    clearMyTimeout();
    if (
      curLocale !== cldrStatus.getCurrentLocale() ||
      xpstrid !== cldrStatus.getCurrentId()
    ) {
      if (SIDEWAYS_DEBUG) {
        console.log(
          "cldrSideways.fetchAndLoadMenu, locale or path changed, skipping"
        );
      }
      return;
    }
    cldrLoad.myLoad(
      getSidewaysUrl(curLocale, xpstrid),
      "sidewaysView",
      function (json) {
        setMenuFromData(regionalVariantsWrapper, json, cacheKey);
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

/**
 * Construct the data needed for the menu, using the json data received from the server,
 * and update the menu
 *
 * @param {Object} regionalVariantsWrapper the menu GUI component
 * @param {Object} json the data received from the serer
 * @param {String} cacheKey the key for caching the data
 */
function setMenuFromData(regionalVariantsWrapper, json, cacheKey) {
  /*
   * Count the number of unique locales in json.others and json.novalue.
   */
  const relatedLocales = json.novalue.slice();
  for (let s in json.others) {
    for (let t in json.others[s]) {
      relatedLocales[json.others[s][t]] = true;
    }
  }
  // if there is 1 sublocale (+ 1 default), show nothing
  if (Object.keys(relatedLocales).length <= 2) {
    oneLocales[cldrStatus.getCurrentLocale()] = true;
    regionalVariantsWrapper.setData(null);
  } else {
    if (!json.others) {
      regionalVariantsWrapper.setData(null);
    } else {
      const topLocale = json.topLocale;
      const locmap = cldrLoad.getTheLocaleMap();
      const curLocaleName = locmap.getRegionAndOrVariantName(topLocale);
      let readLocale = null;

      // merge the read-only sublocale to base locale
      var mergeReadBase = function mergeReadBase(list) {
        let baseValue = null;
        // find the base locale, remove it and store its value
        for (let l = 0; l < list.length; l++) {
          const loc = list[l][0];
          if (loc === topLocale) {
            baseValue = list[l][1];
            list.splice(l, 1);
            break;
          }
        }

        // replace the default locale(read-only) with base locale, store its name for label
        for (let l = 0; l < list.length; l++) {
          const loc = list[l][0];
          const bund = locmap.getLocaleInfo(loc);
          if (bund && bund.readonly) {
            readLocale = locmap.getRegionAndOrVariantName(loc);
            list[l][0] = topLocale;
            list[l][1] = baseValue;
            break;
          }
        }
      };

      // compare all sublocale values
      function appendLocaleList(list, curValue) {
        popupSelect.label = "Regional Variants for " + curLocaleName;

        for (let l = 0; l < list.length; l++) {
          const loc = list[l][0];
          const title = list[l][1];
          const item = { value: loc };
          let str = locmap.getRegionAndOrVariantName(loc);
          if (loc === topLocale) {
            str += " (= " + readLocale + ")";
          }

          if (loc === cldrStatus.getCurrentLocale()) {
            str = escape + str;
            item.disabled = true;
          } else if (title != curValue) {
            str = unequalSign + str;
            item.disabled = false;
          } else {
            str = escape + str;
            item.disabled = false;
          }
          item.str = str;
          popupSelect.items.push(item);
        }
      }

      const dataList = [];
      const popupSelect = {};
      popupSelect.items = [];
      for (let s in json.others) {
        for (let t in json.others[s]) {
          dataList.push([json.others[s][t], s]);
        }
      }

      /*
       * Set curValue = the value for cldrStatus.getCurrentLocale()
       */
      let curValue = null;
      for (let l = 0; l < dataList.length; l++) {
        const loc = dataList[l][0];
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
        for (let s in json.novalue) {
          dataList.push([json.novalue[s], differentValue]);
        }
      }
      mergeReadBase(dataList);

      // then sort by sublocale name
      const sortedDataList = dataList.sort(function (a, b) {
        return (
          locmap.getRegionAndOrVariantName(a[0]) >
          locmap.getRegionAndOrVariantName(b[0])
        );
      });
      appendLocaleList(sortedDataList, curValue);
      sidewaysCache.set(cacheKey, popupSelect);
      regionalVariantsWrapper.setData(popupSelect);
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

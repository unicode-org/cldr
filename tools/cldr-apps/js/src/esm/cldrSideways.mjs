/*
 * cldrSideways: encapsulate Survey Tool code for the "sideways" menu,
 * which enables switching and comparing between a set of related locales,
 * such as: aa = Afar, aa_DJ = Afar (Djibouti), and aa_ER = Afar (Eritrea)
 */
import * as cldrCache from "./cldrCache.mjs";
import * as cldrLoad from "./cldrLoad.mjs";
import * as cldrStatus from "./cldrStatus.mjs";
import * as cldrSurvey from "./cldrSurvey.mjs";

const SIDEWAYS_DEBUG = false;

const NON_BREAKING_SPACES = "\u00A0\u00A0\u00A0"; // non-breaking spaces
const UNEQUALS_SIGN = "\u2260\u00A0"; // U+2260 = "≠"

/**
 * Array storing all only-1 sublocales
 */
const oneLocales = [];

/**
 * Ordinarily, wait a couple seconds before fetching the data for the menu.
 * One reason for this delay is that the Info Panel is often shown for a given
 * row only briefly before the user moves on to the next row, and there's a
 * performance penalty if the data is fetched unnecessarily. At the end of the
 * delay, if the current row has changed, the fetch will be cancelled.
 * However, when the user chooses a related locale from the menu, there
 * should be no delay.
 */
const USUAL_DELAY_MILLISECONDS = 2000;
const ZERO_DELAY_MILLISECONDS = 0;

let fetchDelayMilliseconds = USUAL_DELAY_MILLISECONDS;

/**
 * Timeout ID for fetching and showing the menu
 */
let sidewaysShowTimeoutId = -1;

const sidewaysCache = new cldrCache.LRU();

let locmap = null;

let curLocale = null;

function loadMenu(regionalVariantsWrapper, xpstrid) {
  if (!locmap) {
    locmap = cldrLoad.getTheLocaleMap();
  }
  curLocale = cldrStatus.getCurrentLocale();
  if (!curLocale || oneLocales[curLocale] || !xpstrid) {
    regionalVariantsWrapper.setData(null, null);
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
    regionalVariantsWrapper.setData(curLocale, cachedData);
  } else {
    if (SIDEWAYS_DEBUG) {
      console.log("cldrSideways.loadMenu, fetching new data");
    }
    fetchAndLoadMenu(regionalVariantsWrapper, xpstrid, cacheKey);
  }
}

function fetchAndLoadMenu(regionalVariantsWrapper, xpstrid, cacheKey) {
  clearMyTimeout();
  regionalVariantsWrapper.setLoading();
  sidewaysShowTimeoutId = window.setTimeout(function () {
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
  }, fetchDelayMilliseconds);
  fetchDelayMilliseconds = USUAL_DELAY_MILLISECONDS;
}

function clearMyTimeout() {
  if (sidewaysShowTimeoutId != -1) {
    // https://www.w3schools.com/jsref/met_win_clearinterval.asp
    window.clearInterval(sidewaysShowTimeoutId);
    sidewaysShowTimeoutId = -1;
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
    oneLocales[curLocale] = true;
    regionalVariantsWrapper.setData(null, null);
  } else {
    if (!json.others) {
      regionalVariantsWrapper.setData(null, null);
    } else {
      setMenuFromNontrivialData(regionalVariantsWrapper, json, cacheKey);
    }
  }
}

function setMenuFromNontrivialData(regionalVariantsWrapper, json, cacheKey) {
  const dataList = initializeDataList(json.others);
  const curValue = getCurrentValue(dataList);

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
  const readLocale = mergeReadBase(dataList, json.topLocale);

  // then sort by sublocale name
  const sortedDataList = dataList.sort(function (a, b) {
    return (
      locmap.getRegionAndOrVariantName(a[0]) >
      locmap.getRegionAndOrVariantName(b[0])
    );
  });
  const popupSelect = appendLocaleList(
    sortedDataList,
    curValue,
    json.topLocale,
    readLocale
  );
  sidewaysCache.set(cacheKey, popupSelect);
  regionalVariantsWrapper.setData(curLocale, popupSelect);
}

function initializeDataList(others) {
  const dataList = [];
  for (let s in others) {
    for (let t in others[s]) {
      dataList.push([others[s][t], s]);
    }
  }
  return dataList;
}

/**
 * Get the value for the current locale
 */
function getCurrentValue(dataList) {
  for (let l = 0; l < dataList.length; l++) {
    const loc = dataList[l][0];
    if (loc === curLocale) {
      return dataList[l][1];
    }
  }
  return null;
}

// merge the read-only sublocale to base locale
function mergeReadBase(list, topLocale) {
  let readLocale = null;
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
  return readLocale;
}

function appendLocaleList(list, curValue, topLocale, readLocale) {
  const popupSelect = {
    items: [],
    label:
      "Regional Variants for " + locmap.getRegionAndOrVariantName(topLocale),
  };
  // compare all sublocale values
  for (let l = 0; l < list.length; l++) {
    const loc = list[l][0];
    const title = list[l][1];
    const item = { value: loc };
    let str = locmap.getRegionAndOrVariantName(loc);
    if (loc === topLocale) {
      str += " (= " + readLocale + ")";
    }

    if (loc === curLocale) {
      str = NON_BREAKING_SPACES + str;
      item.disabled = true;
    } else if (title != curValue) {
      str = UNEQUALS_SIGN + str;
      item.disabled = false;
    } else {
      str = NON_BREAKING_SPACES + str;
      item.disabled = false;
    }
    item.str = str;
    popupSelect.items.push(item);
  }
  return popupSelect;
}

function makeCacheKey(curLocale, xpstrid) {
  return curLocale + "-" + xpstrid;
}

function clearCache() {
  sidewaysCache.clear();
}

function goToLocale(localeId) {
  cldrStatus.setCurrentLocale(localeId);
  cldrLoad.reloadV();
  fetchDelayMilliseconds = ZERO_DELAY_MILLISECONDS;
}

export { clearCache, goToLocale, loadMenu };

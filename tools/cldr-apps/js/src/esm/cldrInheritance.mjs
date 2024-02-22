/*
 * cldrInheritance: encapsulate inheritance explainer.
 */
import * as cldrAjax from "./cldrAjax.mjs";
import * as cldrLoad from "./cldrLoad.mjs";
/**
 * null, or promise to reason explanations
 */
let reasonStrings = null;

/**
 * @returns Promise<Map<String,Object>> map from reason enum to object
 */
function getInheritanceReasonStrings() {
  if (reasonStrings === null) {
    reasonStrings = cldrAjax
      .doFetch(`api/xpath/inheritance/reasons`)
      .then((v) => v.json())
      .then((r) => r.reduce((p, v) => ((p[v.reason] = v), p), {}));
  }
  return reasonStrings;
}

/**
 * @param {String} locale
 * @param {String} xpath
 * @return Promise<Object[]> array of explanations
 */
async function explainInheritance(itemLocale, itemXpath) {
  const locMap = cldrLoad.getTheLocaleMap();
  const r = await cldrAjax.doFetch(
    `api/xpath/inheritance/locale/${itemLocale}/${itemXpath}`
  );
  const { items } = await r.json();
  let lastLocale = null;
  let lastPath = null;
  for (let i = 0; i < items.length; i++) {
    const { locale, xpath, reason, hidden } = items[i];
    if (hidden) {
      items[i].hidden = true;
    }
    if (reason !== "none") {
      // hide 'none'
      items[i].showReason = true;
    }
    // set newLocale whenever the locale changes (and isn't null)
    // this way we don’t repeat the locale message
    if (locale && lastLocale !== locale) {
      // Don't set newLocale for the first (current) locale.
      items[i].newLocale = locMap.getLocaleName(locale);
      lastLocale = locale;
    }
    if (xpath && lastPath !== xpath) {
      if (i === 0 || xpath !== itemXpath) {
        // Don't set newXpath for the first (current) xpath.
        items[i].newXpath = xpath;
      }
      lastPath = xpath;
    }
  }
  return items;
}

export { getInheritanceReasonStrings, explainInheritance };

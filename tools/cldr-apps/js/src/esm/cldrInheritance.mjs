/*
 * cldrInheritance: encapsulate inheritance explainer.
 */
import * as cldrAjax from "./cldrAjax.js";

/**
 * null, or promise to reason explanations
 */
let reasonStrings = null;

/**
 * @returns Promise<Map<String,String>> map from reason enum to string
 */
function getInheritanceReasonStrings() {
  if (reasonStrings === null) {
    reasonStrings = cldrAjax
      .doFetch(`api/xpath/inheritance/reasons`)
      .then((v) => v.json());
  }
  return reasonStrings;
}

/**
 * @param {String} locale
 * @param {String} xpath
 * @return Promise<Object[]> array of explanations
 */
async function explainInheritance(itemLocale, xpath) {
  const r = await cldrAjax.doFetch(
    `api/xpath/inheritance/locale/${itemLocale}/${xpath}`
  );
  const { items } = await r.json();
  let lastLocale = null;
  for (let i = 0; i < items.length; i++) {
    const { locale } = items[i];
    // set newLocale whenever the locale changes (and isn't null)
    // this way we don’t repeat the locale message
    if (locale && lastLocale !== locale) {
      if (i !== 0 || locale !== itemLocale) {
        // Don't set newLocale for the first (current) locale.
        items[i].newLocale = locale;
      }
      lastLocale = locale;
    }
  }
  return items;
}

export { getInheritanceReasonStrings, explainInheritance };

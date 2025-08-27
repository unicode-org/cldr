/*
 * cldrAddAlt: enable adding an "alt" path
 */
import * as cldrAjax from "./cldrAjax.mjs";
import * as cldrLoad from "./cldrLoad.mjs";
import * as cldrNotify from "./cldrNotify.mjs";
import * as cldrStatus from "./cldrStatus.mjs";
import * as cldrVue from "./cldrVue.mjs";

import AddAlt from "../views/AddAlt.vue";

function addAltButton(containerEl, xpstrid) {
  try {
    const addAltWrapper = cldrVue.mount(AddAlt, containerEl);
    addAltWrapper.setXpathStringId(xpstrid);
  } catch (e) {
    console.error("Error loading Add Alt vue " + e.message + " / " + e.name);
    cldrNotify.exception(e, "while loading AddAlt");
  }
}

async function getAlts(xpstrid, callbackFunction) {
  const localeId = cldrStatus.getCurrentLocale();
  if (!localeId) {
    return;
  }
  const url = cldrAjax.makeApiUrl(
    "xpath/alt/" + localeId + "/" + xpstrid,
    null
  );
  return await cldrAjax
    .doFetch(url)
    .then(cldrAjax.handleFetchErrors)
    .then((r) => r.json())
    .then(callbackFunction)
    .catch((e) => console.error(e));
}

async function addChosenAlt(xpstrid, alt, callbackFunction) {
  const localeId = cldrStatus.getCurrentLocale();
  if (!localeId) {
    return;
  }
  const url = cldrAjax.makeApiUrl("xpath/alt", null);
  const data = {
    alt: alt,
    localeId: localeId,
    hexId: xpstrid,
  };
  const init = cldrAjax.makePostData(data);
  try {
    const response = await cldrAjax.doFetch(url, init);
    if (response.ok) {
      callbackFunction(null);
    } else {
      const json = await response.json();
      const message = json.message || "Unknown server response";
      throw new Error(message);
    }
  } catch (e) {
    console.error(e);
    window.alert("Error while adding alt: \n\n" + e);
    callbackFunction(e);
  }
}

/**
 * Reload the page table so it will include the new row
 */
function reloadPage() {
  cldrLoad.reloadV(); // crude
}

export { addAltButton, getAlts, addChosenAlt, reloadPage };

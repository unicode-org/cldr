/*
 * cldrAddAlt: enable adding an "alt" path
 */
import * as cldrAjax from "./cldrAjax.mjs";
import * as cldrLoad from "./cldrLoad.mjs";
import * as cldrStatus from "./cldrStatus.mjs";

import AddAlt from "../views/AddAlt.vue";

import { createCldrApp } from "../cldrVueRouter.mjs";

import { notification } from "ant-design-vue";

function addButton(containerEl, xpstrid) {
  try {
    const fragment = document.createDocumentFragment();
    const addAltWrapper = createCldrApp(AddAlt).mount(fragment);
    const vueEl = document.createElement("section");
    containerEl.appendChild(vueEl);
    vueEl.replaceWith(fragment);
    addAltWrapper.setXpathStringId(xpstrid);
  } catch (e) {
    console.error("Error loading Add Alt vue " + e.message + " / " + e.name);
    notification.open({
      message: `${e.name} while loading AddAlt.vue`,
      description: `${e.message}`,
      duration: 0,
    });
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

export { addButton, getAlts, addChosenAlt, reloadPage };

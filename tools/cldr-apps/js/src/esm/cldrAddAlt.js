/*
 * cldrAddAlt: enable adding an "alt" path
 */
import * as cldrAjax from "./cldrAjax.js";

import AddAlt from "../views/AddAlt.vue";

import { createCldrApp } from "../cldrVueRouter";

let addAltWrapper = null;

function addButton(containerEl, xpstrid) {
  try {
    const fragment = document.createDocumentFragment();
    addAltWrapper = createCldrApp(AddAlt).mount(fragment);
    const vueEl = document.createElement("section");
    containerEl.appendChild(vueEl);
    vueEl.replaceWith(fragment);
    addAltWrapper.setXpathStringId(xpstrid);
  } catch (e) {
    console.error("Error loading Add Alt vue " + e.message + " / " + e.name);
    notification.error({
      message: `${e.name} while loading AddAlt.vue`,
      description: `${e.message}`,
      duration: 0,
    });
  }
}

async function getAlts(xpstrid, callbackFunction) {
  const url = cldrAjax.makeApiUrl("xpath/alt/" + xpstrid, null);
  return await cldrAjax
    .doFetch(url)
    .then(cldrAjax.handleFetchErrors)
    .then((r) => r.json())
    .then(callbackFunction)
    .catch((e) => console.error(`Error: ${e} ...`));
}

export { addButton, getAlts };

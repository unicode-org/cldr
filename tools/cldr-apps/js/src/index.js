import "./style.css";
// This is the entrypoint for the new SurveyTool app

import { createApp } from "vue";

import AboutPanel from "./views/AboutPanel.vue";
import WaitingPanel from "./views/WaitingPanel.vue";

function show(component, el) {
  const app = createApp(component);
  app.mount(el);
  return app;
}

let lastMounted = null;

/**
 * Mount a Vue component.
 * Will unmount the previously mounted object if called more than once.
 * @param {String} type - type of object to mount, such as 'retry' or 'about'
 * @param {Element} el - Element to mount on
 * @returns the Vue Application object
 */
function showPanel(type, el) {
  if (lastMounted) {
    lastMounted.unmount();
    lastMounted = null;
  }
  if ('about' == type) {
    lastMounted = show(AboutPanel, el);
  } else if('retry' == type) {
    lastMounted = show(WaitingPanel, el);
  } else {
    console.log('showPanel: Ignoring unknown type=' + type);
    // Do nothing here
  }
  return lastMounted;
}

// The following will show up in the cldrBundle global
export default {
  showPanel,
};

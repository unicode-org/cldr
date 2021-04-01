import { specialToComponent } from "./specialToComponentMap";

import { createApp } from "vue";

import { getCldrOpts } from "./getCldrOpts";

// components. See index.js for css imports.
// example: import {SomeComponent} from 'whatever'
import { Popover, Spin } from "ant-design-vue";

// local components
import CldrValue from "./views/CldrValue.vue";
import LoginButton from "./views/LoginButton.vue";

/**
 * The App created and mounted most recently. For .unmount …
 */
let lastMounted = null;
/**
 * The root Component instance used most recently.  For calling methods on.
 */
let lastRoot = null;

/**
 * Mount a Vue component.
 * Will unmount the previously mounted object if called more than once.
 * @param {String} type - type of object to mount, such as 'retry' or 'about'
 * @param {Element} el - Element to mount on
 * @returns the Vue Application object
 */
function showPanel(type, el, opts) {
  if (lastMounted) {
    lastMounted.unmount();
    lastMounted = null;
    lastRoot = null;
  }
  let component = specialToComponent(type);
  lastMounted = show(component, el, type, opts);
  return lastMounted;
}

/**
 * Handle a coverage level change.
 * @param {String} newLevel new coverage level
 * @returns true if handled
 */
function handleCoverageChanged(newLevel) {
  if (!lastRoot || !lastRoot.handleCoverageChanged) {
    return false;
  }
  return lastRoot.handleCoverageChanged(newLevel);
}

/**
 * Create the specified Vue app, and register components on it
 * @param {Component} component Vue component to mount
 * @param {String} specialPage name of special page as $specialPage
 * @param {Object} cldrOpts data to pass through as $cldrOpts
 * @param {Object} extraProps data to pass through as global properties
 * @returns {App} the App object
 */
function createCldrApp(component, specialPage, cldrOpts, extraProps) {
  const app = createApp(component, extraProps || {});

  // These are available on all components.
  app.config.globalProperties.$cldrOpts = getCldrOpts();
  app.config.globalProperties.$specialPage = specialPage || null;

  // There is no global registration in Vue3, so we re-register
  // the components here.
  setupComponents(app);

  return app;
}

/**
 * Create and mount the specified Vue route.
 * @param {Component} component Vue component to mount
 * @param {Element|String} el Element or String selector
 * @param {String} specialPage name of special page as $specialPage
 * @param {Object} cldrOpts data to pass through as $cldrOpts
 * @param {Object} extraProps data to pass through as global properties
 * @returns {App} the App object
 */
function show(component, el, specialPage, extraProps) {
  const app = createCldrApp(component, specialPage, extraProps);

  // Fire up the app..
  lastRoot = app.mount(el);

  return app;
}

/**
 * add any Vue components needed here.
 * @param {App} app
 */
function setupComponents(app) {
  // example:
  // app.component('SomeComponent', SomeComponent)
  app.component("Popover", Popover);
  app.component("a-spin", Spin);
  app.component("cldr-value", CldrValue);
  app.component("cldr-loginbutton", LoginButton);
}

export { showPanel, handleCoverageChanged, createCldrApp, setupComponents };

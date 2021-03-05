import { specialToComponent } from "./specialToComponentMap";

import { createApp } from "vue";

// components. See index.js for css imports.
// example: import {SomeComponent} from 'whatever'
import { Popover } from "ant-design-vue";

// so we can unmount a component
let lastMounted = null;

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
  }
  let component = specialToComponent(type);
  lastMounted = show(component, el, type, opts);
  return lastMounted;
}

/**
 * Create and mount the specified Vue route.
 * @param {Component} component Vue component to mount
 * @param {Element|String} el Element or String selector
 * @param {String} specialPage name of special page
 * @param {Object} cldrOpts data to pass through
 * @returns {App} the App object
 */
function show(component, el, specialPage, cldrOpts) {
  const app = createApp(component, {
    // These get passed through to the component
    specialPage,
    cldrOpts,
  });

  // There is no global registration in Vue3, so we re-register
  // the components here.
  setupComponents(app);

  // Fire up the app..
  app.mount(el);

  return app;
}

/**
 * add any Vue components needed here.
 * @param {App} app
 */
function setupComponents(app) {
  // example:
  // app.component('SomeComponent', SomeComponent)
  app.component('Popover', Popover);
}

export { showPanel };

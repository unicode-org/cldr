import { specialToComponent } from "./specialToComponentMap";

import { createApp } from "vue";

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
 *
 * @param {Component} component Vue component to mount
 * @param {Element|String} el Element or String selector
 * @param {String} specialPage name of special page
 * @param {Object} cldrOpts data to pass through
 * @returns
 */
function show(component, el, specialPage, cldrOpts) {
  const app = createApp(component, {
    // These get passed through to the component
    specialPage,
    cldrOpts,
  });
  app.mount(el);
  return app;
}

export { showPanel };

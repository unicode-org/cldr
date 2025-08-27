// Note: the order of imports is important here. Possibly a race condition in globals inside some of the .js files.
import { createApp } from "vue";
import { notification } from "ant-design-vue";

import * as cldrComponents from "./cldrComponents.mjs";
import * as cldrVueMap from "./cldrVueMap.mjs";

/**
 * The App created and mounted most recently. For .unmount …
 */
let lastMounted = null;

/**
 * Mount a Vue component.
 * Will unmount the previously mounted object if called more than once.
 *
 * @param {String} type - type of object to mount, such as 'retry' or 'about'
 * @param {Element} el - Element to mount on
 * @returns the Vue Application object
 */
function showPanel(type, el, opts) {
  if (lastMounted) {
    lastMounted.unmount();
    lastMounted = null;
  }
  const component = cldrVueMap.specialToComponent(type);
  lastMounted = show(component, el, type, opts);
  return lastMounted;
}

/**
 * Mount the specified Vue component, as a child of the given element
 *
 * If the given element already has any child elements, the Vue component will be appended after them
 *
 * @param {Component} component the Vue component to mount
 * @param {Element} el the element below which to mount it
 * @returns the application instance
 */
function mount(component, el, extraProps) {
  const fragment = document.createDocumentFragment();
  const app = create(component, null, extraProps).mount(fragment);
  const childEl = document.createElement("div");
  el.appendChild(childEl);
  childEl.replaceWith(fragment);
  return app;
}

/**
 * Mount the specified Vue component, as the first child of the given element
 *
 * If the given element already has at least one child, the Vue component will be
 * inserted before that child
 *
 * @param {Component} component the Vue component to mount
 * @param {Element} el the element below which to mount it
 * @returns the application instance
 */
function mountAsFirstChild(component, el) {
  const fragment = document.createDocumentFragment();
  const app = create(component).mount(fragment);
  const childEl = document.createElement("div");
  if (el.firstChild) {
    el.insertBefore(childEl, el.firstChild);
    childEl.parentNode.replaceChild(fragment, childEl);
  } else {
    el.appendChild(childEl);
    childEl.replaceWith(fragment);
  }
  return app;
}

/**
 * Mount the specified Vue component, replacing the given element
 *
 * @param {Component} component the Vue component to mount
 * @param {Element} el the element to be replaced
 * @returns the application instance
 */
function mountReplace(component, el) {
  const fragment = document.createDocumentFragment();
  const app = create(component).mount(fragment);
  el.replaceWith(fragment);
  return app;
}

/**
 * Create the specified Vue app, and register components on it
 *
 * @param {Component} component Vue component to mount
 * @param {String} specialPage name of special page as $specialPage
 * @param {Object} extraProps data to pass through as global properties
 * @returns {App} the App object
 */
function create(component, specialPage, extraProps) {
  const app = createApp(component, extraProps || {});

  // These are available on all components.
  app.config.globalProperties.$specialPage = specialPage || null;

  // Setup err handling
  app.config.errorHandler = (err, vm, info) => {
    console.error(err);
    notification.error({
      message: `Error: ${err.name} in ${specialPage || "vue component"}`,
      description: `${err.message}`,
      duration: 0, // keep open
    });
  };

  // There is no global registration in Vue3, so we re-register
  // the components here.
  cldrComponents.setup(app);

  return app;
}

/**
 * Create and mount the specified Vue component
 *
 * @param {Component} component Vue component to mount
 * @param {Element|String} el Element or String selector
 * @param {String} specialPage name of special page as $specialPage
 * @param {Object} extraProps data to pass through as global properties
 * @returns {App} the App object
 */
function show(component, el, specialPage, extraProps) {
  let app;
  try {
    app = create(component, specialPage, extraProps);
    app.mount(el);
  } catch (e) {
    errBox(e, !!app ? "showing" : "creating");
    return null;
  }
  return app;
}

/**
 * Show an error box for a Vue component
 *
 * @param {Error} e the error
 * @param {String} operation such as showing or creating
 */
function errBox(e, operation) {
  console.error(`There was a problem ${operation} “${specialPage}”`);
  console.error(e);
  notification.error({
    message: `Problem ${operation} “${specialPage}”`,
    description: `${e.message} — (click to reload)`,
    placement: "topLeft",
    duration: 0, // do not auto close
    onClick: () => window.location.reload(),
  });
}

export { create, mount, mountAsFirstChild, mountReplace, showPanel };

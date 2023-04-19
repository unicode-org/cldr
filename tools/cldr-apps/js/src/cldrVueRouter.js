// Note: the order of imports is important here. Possibly a race condition in globals inside some of the .js files.
import { createApp } from "vue";
import { notification } from "ant-design-vue";
import { specialToComponent } from "./specialToComponentMap";
import { getCldrOpts } from "./getCldrOpts";
import { setupComponents } from "./setupComponents";

/**
 * The App created and mounted most recently. For .unmount …
 */
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
 * Create the specified Vue app, and register components on it
 * @param {Component} component Vue component to mount
 * @param {String} specialPage name of special page as $specialPage
 * @param {Object} cldrOpts data to pass through as $cldrOpts
 * @param {Object} extraProps data to pass through as global properties
 * @returns {App} the App object
 */
function createCldrApp(component, specialPage, extraProps) {
  const app = createApp(component, extraProps || {});

  // These are available on all components.
  app.config.globalProperties.$cldrOpts = getCldrOpts();
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
  let app;
  try {
    app = createCldrApp(component, specialPage, extraProps);
    app.mount(el);
  } catch (e) {
    errBox(e, !!app ? "showing" : "creating");
    return null;
  }

  return app;
}

/**
 * Show an error box for a Vue component
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

export { showPanel, createCldrApp };

import { specialToComponent } from "./specialToComponentMap";

import { createApp } from "vue";

import { getCldrOpts } from "./getCldrOpts";

// See index.js for css imports.
// example: import {SomeComponent} from 'whatever'
import {
  // components
  Alert,
  Button,
  Checkbox,
  Form,
  Icon,
  Input,
  Popover,
  Progress,
  Radio,
  Spin,
  Steps,
  Tooltip,
  // functions
  notification,
} from "ant-design-vue";

// local components
import CldrValue from "./views/CldrValue.vue";
import LoginButton from "./views/LoginButton.vue";
import ReportResponse from "./views/ReportResponse.vue";

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

/**
 * add any Vue components needed here.
 * @param {App} app
 */
function setupComponents(app) {
  // example:
  // app.component('SomeComponent', SomeComponent)
  // Keep this list sorted
  app.component("a-alert", Alert);
  app.component("a-button", Button);
  app.component("a-checkbox", Checkbox);
  app.component("a-radio", Radio);
  app.component("a-radio-group", Radio.Group);
  app.component("a-form-item", Form.Item);
  app.component("a-form", Form);
  app.component("a-icon", Icon);
  app.component("a-input-password", Input.Password);
  app.component("a-input", Input);
  app.component("a-popover", Popover);
  app.component("a-progress", Progress);
  app.component("a-spin", Spin);
  app.component("a-step", Steps.Step);
  app.component("a-steps", Steps);
  app.component("a-tooltip", Tooltip);
  app.component("cldr-loginbutton", LoginButton);
  app.component("cldr-value", CldrValue);
  app.component("cldr-report-response", ReportResponse);
}

export { showPanel, createCldrApp, setupComponents };

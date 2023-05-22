import { App } from "vue";

/**
 * This file lists all of the components which are automatically available to
 * CLDR .vue templates.
 *
 * See ./index.js for css imports.
 */

// local components
import CldrValue from "./views/CldrValue.vue";
import LoginButton from "./views/LoginButton.vue";
import ReportResponse from "./views/ReportResponse.vue";
import SearchButton from "./views/SearchButton.vue";

// 3rd party component(s)

import {
  Alert,
  Button,
  Checkbox,
  Form,
  Input,
  List,
  Popover,
  Progress,
  Radio,
  Spin,
  Steps,
  Textarea,
  Tooltip,
  Timeline,
} from "ant-design-vue";
// Note: 'notification' is a function and is imported as a function in cldrVueRouter,
// or within a specific app.

/**
 * add any Vue components needed here.
 * @param {App} app
 */
function setupComponents(app) {
  // example:
  // app.component('SomeComponent', SomeComponent)
  // Keep this list sorted

  // we use a- to denote ant components and cldr- to denote cldr components
  app.component("a-alert", Alert);
  app.component("a-button", Button);
  app.component("a-checkbox", Checkbox);
  app.component("a-form-item", Form.Item);
  app.component("a-form", Form);
  app.component("a-input-password", Input.Password);
  app.component("a-input-search", Input.Search);
  app.component("a-input", Input);
  app.component("a-list-item-meta", List.Item.Meta);
  app.component("a-list-item", List.Item);
  app.component("a-list", List);
  app.component("a-popover", Popover);
  app.component("a-progress", Progress);
  app.component("a-radio-group", Radio.Group);
  app.component("a-radio", Radio);
  app.component("a-spin", Spin);
  app.component("a-step", Steps.Step);
  app.component("a-steps", Steps);
  app.component("a-textarea", Textarea);
  app.component("a-timeline-item", Timeline.Item);
  app.component("a-timeline", Timeline);
  app.component("a-tooltip", Tooltip);
  app.component("cldr-loginbutton", LoginButton);
  app.component("cldr-report-response", ReportResponse);
  app.component("cldr-searchbutton", SearchButton);
  app.component("cldr-value", CldrValue);
}

export { setupComponents };

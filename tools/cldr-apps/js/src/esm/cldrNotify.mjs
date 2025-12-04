/*
 * cldrNotify: encapsulate notifications, i.e., pop-up windows that notify the user
 * what's going on, either staying open until dismissed by the user or disappearing
 * automatically after a fixed number of seconds.
 *
 * Although it's possible for any .mjs file to import notification from ant-design-vue,
 * it's preferable to encapsulate the dependency here, to facilitate switching to a different
 * library if and when necessary or appropriate. For example, ant-design-vue might not work
 * for us anymore when we switch from Vue 3 to Vue 4 or stop using Vue entirely. Within our
 * Vue components, such encapsulation might also be beneficial, but to a lesser extent given
 * that those components are already heavily dependent on Vue 3 and ant-design-vue anyway.
 *
 * Encapsulation here can also facilitate consistency rather than arbitrary differences
 * such as 8 seconds here, 10 seconds there; top-left here and top-right there; ...
 */

import * as cldrVue from "./cldrVue.mjs";

import NotificationPopup from "../views/NotificationPopup.vue";

// Reference: https://www.antdv.com/components/notification
import { notification } from "ant-design-vue";
import { datadogLogs } from "@datadog/browser-logs";

/** if falsy, let sleeping dogs lie */
const hasDataDog = !!window.dataDogClientToken;

/**
 * For warnings and general notifications, automatically close after this many seconds
 */
const MEDIUM_DURATION = 8;

/**
 * For errors and exceptions, stay open until dismissed by the user
 */
const NO_TIMEOUT = 0;

/**
 * Display a general notification
 *
 * @param {String} message the title, displayed at the top
 * @param {String} description the more detailed description
 * @param {Function} onClick optional function called when clicking
 */
function open(message, description, onClick) {
  notification.open({
    message: message,
    description: description,
    duration: MEDIUM_DURATION,
    onClick,
  });
}

function logInfo(message, description) {
  if (hasDataDog) {
    datadogLogs.logger.info(message, { description });
  } else {
    console.log(message);
  }
}

/**
 * Display a warning notification
 *
 * @param {String} message the title, displayed at the top
 * @param {String} description the more detailed description
 */
function warning(message, description) {
  logWarning(message, description);
  notification.warning({
    message: message,
    description: description,
    duration: MEDIUM_DURATION,
  });
}

/**
 * Log a warning
 *
 * @param {String} message the title
 * @param {String} description the more detailed description
 */
function logWarning(message, description) {
  if (hasDataDog) {
    datadogLogs.logger.warn(message, { description });
  } else {
    console.warn(message);
  }
}

/**
 * Display an error notification, with no timeout
 *
 * @param {String} message the title, displayed at the top
 * @param {String} description the more detailed description
 */
function error(message, description) {
  logError(message, description);
  notification.error({
    message: message,
    description: description,
    duration: NO_TIMEOUT,
  });
}

/**
 * Log an error
 *
 * @param {String} message the title
 * @param {String} description the more detailed description
 */
function logError(message, description) {
  if (hasDataDog) {
    datadogLogs.logger.error(message, { description });
  } else {
    console.error(message);
  }
}

/**
 * Display an error notification, and when the user closes it, call the callback function
 */
function errorWithCallback(message, description, callback) {
  logError(message, description);
  notification.error({
    message: message,
    description: description,
    duration: NO_TIMEOUT,
    onClose: callback,
  });
}

/**
 * Log and Display an error notification for an exception
 *
 * @param {Object|String} e the Error that was thrown and caught
 * @param {String} context a description of where it was caught
 */
function exception(e, context) {
  e = logException(e, context);
  notification.error({
    message: "Internal error: " + e.name + " " + context,
    description: e.message,
    duration: NO_TIMEOUT,
  });
}

/** Just log an exception */
function logException(e, context) {
  if (typeof e === "string") {
    e = {
      name: "",
      message: e,
    };
  }
  if (hasDataDog) {
    datadogLogs.logger.error(context, {}, e);
  }
  console.error(e.message + " " + context);
  console.error(e); // log with stack trace
  return e;
}

/**
 * Display an error notification, possibly containing HTML, with a custom dialog
 *
 * @param {String} message the title, displayed at the top (plain text)
 * @param {String} description a description of the problem, possibly HTML
 * @param {Error} err an optional exception
 */
function openWithHtml(message, description, err) {
  if (err) console.error(err);
  if (hasDataDog) {
    datadogLogs.logger.error(message, { description }, err);
  }
  try {
    const NotificationPopupWrapper = cldrVue.mount(
      NotificationPopup,
      document.body
    );
    NotificationPopupWrapper.openWithMessageAndDescription(
      message,
      description
    );
  } catch (e) {
    console.error(
      "Error loading Notification Popup vue " + e.message + " / " + e.name
    );
    exception(e, "while loading NotificationPopup");
  }
}

export {
  error,
  errorWithCallback,
  exception,
  logError,
  logException,
  logInfo,
  logWarning,
  open,
  openWithHtml,
  warning,
};

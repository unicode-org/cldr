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

// Reference: https://www.antdv.com/components/notification
import { notification } from "ant-design-vue";

/**
 * Callers should use one of these constants rather than a literal number.
 *
 * Note: possibly it would be better (more consistent) to enforce NO_TIMEOUT for errors, and MEDIUM_DURATION
 * for warnings and general notifications.
 */
const NO_TIMEOUT = 0; // stay open until dismissed by the user
const MEDIUM_DURATION = 8; // automatically close after this many seconds

/**
 * Display a general notification
 *
 * @param {String} message the title, displayed at the top
 * @param {String} description the more detailed description
 * @param {Number} duration how many seconds to display, or NO_TIMEOUT
 */
function open(message, description, duration) {
  notification.open({
    message: message,
    description: description,
    duration: duration,
  });
}

/**
 * Display a warning notification
 *
 * @param {String} message the title, displayed at the top
 * @param {String} description the more detailed description
 * @param {Number} duration how many seconds to display, or NO_TIMEOUT
 */
function warning(message, description, duration) {
  notification.warning({
    message: message,
    description: description,
    duration: duration,
  });
}

/**
 * Display an error notification
 *
 * @param {String} message the title, displayed at the top
 * @param {String} description the more detailed description
 * @param {Number} duration how many seconds to display, or NO_TIMEOUT
 */
function error(message, description, duration) {
  notification.error({
    message: message,
    description: description,
    duration: duration,
  });
}

export { NO_TIMEOUT, MEDIUM_DURATION, error, open, warning };

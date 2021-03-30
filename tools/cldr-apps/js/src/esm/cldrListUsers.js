/*
 * cldrListUsers: Survey Tool feature for listing users
 */
import * as cldrAccount from "./cldrAccount.js";

/**
 * Load the "List Users" page
 * -- called as special.load
 *
 * Redirect to cldrAccount, configured for "List Users" instead of "My Account"
 */
function load() {
  cldrAccount.loadListUsers();
}

/**
 * Confirm this file compiles and runs
 */
function ping() {
  return "pong";
}

export {
  load,
  /*
   * The following are meant to be accessible for unit testing only:
   */
  ping,
};

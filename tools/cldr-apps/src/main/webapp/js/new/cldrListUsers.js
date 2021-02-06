"use strict";

/**
 * cldrListUsers: Survey Tool feature for listing users
 * This is the new non-dojo version. For dojo, see special/users.js.
 *
 * Use an IIFE pattern to create a namespace for the public functions,
 * and to hide everything else, minimizing global scope pollution.
 */
const cldrListUsers = (function () {
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

  /*
   * Make only these functions accessible from other files
   */
  return {
    load,
    /*
     * The following are meant to be accessible for unit testing only:
     */
    test: {
      ping,
    },
  };
})();

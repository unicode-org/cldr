/**
 * Example special module that shows a blank page.
 * Modify 'js/special/blank.js' below to reflect your special page's name.
 * @module blank
 */
define("js/special/admin.js", ["js/special/SpecialPage.js"], function (
  SpecialPage
) {
  var _super;

  function Page() {
    // constructor
  }

  // set up the inheritance before defining other functions
  _super = Page.prototype = new SpecialPage();

  Page.prototype.show = function show(params) {
    // set up the DIV you want to show the world
    var ourDiv = createChunk("Please access the Admin Panel using the gear menu.", "i", "warn");

    // No longer loading
    hideLoader(null);

    // Flip to the new DIV
    params.flipper.flipTo(params.pages.other, ourDiv);
  };

  return Page;
});

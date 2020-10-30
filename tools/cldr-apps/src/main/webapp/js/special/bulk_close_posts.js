'use strict';

/**
 * Based on specials/forum_participation.js
 *
 * TODO: use modern JavaScript -- import/export, not define
 * TODO: avoid dependencies on legacy code
 */
define("js/special/bulk_close_posts.js", ["js/special/SpecialPage.js"], function(SpecialPage) {
	var _super;

	function Page() {
		// constructor
	}

	// set up the inheritance before defining other functions
	_super = Page.prototype = new SpecialPage();

	Page.prototype.show = function show(params) {
		cldrStBulkClosePosts.load(params);
	};

	return Page;
});

'use strict';

/**
 * Based on specials/forum.js
 *
 * TODO: use modern JavaScript -- import/export, not define
 * TODO: avoid dependencies on legacy code
 * TODO: put code for progress bar, etc., in a shared module, share with, e.g., vsummary.js
 */
define("js/special/forum_participation.js", ["js/special/SpecialPage.js"], function(SpecialPage) {
	var _super;

	function Page() {
		// constructor
	}

	// set up the inheritance before defining other functions
	_super = Page.prototype = new SpecialPage();

	Page.prototype.show = function show(params) {
		cldrStForumParticipation.load(params);
	};

	return Page;
});

/**
 * Example special module that shows a blank page. 
 * Modify 'js/special/blank.js' below to reflect your special page's name.
 * @module blank
 */
define("js/special/blank.js", ["js/special/SpecialPage.js"], function(SpecialPage) {
	var _super;
	
	function Page() {
		// constructor
	}
	
	// set up the inheritance before defining other functions
	_super = Page.prototype = new SpecialPage();
	
	Page.prototype.show = function show(params) {
		// set up the DIV you want to show the world
		var ourDiv = createChunk("This is a blank page.","i","warn");
		
		// ourDiv.appendChild(...)
		
		// set up the 'right sidebar'
		showInPop2("This Is A Blank Page", null, null, null, true); /* show the box the first time */					
		
		// No longer loading
		hideLoader(null);

		// Flip to the new DIV
		params.flipper.flipTo(params.pages.other, ourDiv);
	};


	return Page;
});
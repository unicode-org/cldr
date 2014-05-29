/**
 * This is the base class for special pages. Loading it directly just prints an error.
 * 
 * @module SpecialPage
 */
define("js/special/SpecialPage.js", function() {
	/**
	 * Superclass of special pages
	 * @class SpecialPage
	 */
	function SpecialPage() {
		
	}
	
	/**
	 * parse a hash tag
	 * @function parseHash
	 */
	SpecialPage.prototype.parseHash = function parseHash(hash, pieces) {
		surveyCurrentPage='';
		surveyCurrentId=''; // for now
	};
	
	/**
	 * Show the page
	 * @function show
	 */
	SpecialPage.prototype.show = function show(params) {
		params.flipper.flipTo(params.pages.loading, loadingChunk = createChunk("Oops: special page '" + params.name + ".show' seems to be unimplemented.","i","ferrbox"));
	};
	
	return SpecialPage;
});
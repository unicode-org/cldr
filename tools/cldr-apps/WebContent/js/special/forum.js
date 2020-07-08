/**
 * Special Dojo "module" that shows a forum page.
 */
'use strict';

define("js/special/forum.js", ["js/special/SpecialPage.js", "dojo/request", "dojo/window"],
		function(SpecialPage, request, win) {
	var _super;

	function Page() {
		// constructor
	}

	// set up the inheritance before defining other functions
	_super = Page.prototype = new SpecialPage();

	/**
	 * parse a hash tag
	 *
	 * Reference: https://dojotoolkit.org/reference-guide/1.10/dojo/hash.html
	 *
	 * @function parseHash
	 */
	Page.prototype.parseHash = function parseHash(hash, pieces) {
		surveyCurrentPage='';
		if(pieces && pieces.length>3){
			if(!pieces[3] || pieces[3]=='') {
				surveyCurrentId='';
			} else {
				var id = new Number(pieces[3]);
				if(id == NaN) {
					surveyCurrentId = '';
				} else {
					surveyCurrentId = id.toString();
					this.handleIdChanged(surveyCurrentId);
				}
			}
		}
	};

	Page.prototype.handleIdChanged = function handleIdChanged(strid) {
		if(strid && strid != '') {
			var id = new Number(strid);
			if(id == NaN) {
				surveyCurrentId = '';
			} else {
				surveyCurrentId = id.toString();
			}
			var itemid = "fp"+id;
			var pdiv = document.getElementById(itemid);
			if(pdiv) {
				console.log("Scrolling " + itemid);
				win.scrollIntoView(pdiv);
				(function(o,itemid,pdiv){
						pdiv.style["background-color"]="yellow";
						window.setTimeout(function(){
							pdiv.style["background-color"]=null;
						}, 2000);
				})(this,itemid,pdiv);
			} else {
				console.log("No item "+itemid);
			}
		}
	};

	Page.prototype.show = function show(params) {

		if(surveyCurrentLocale=='') {
			hideLoader(null);
			params.flipper.flipTo(params.pages.other, createChunk(stui.str("generic_nolocale"),"p","helpContent"));
		} else {
			const forumName = locmap.getLocaleName(locmap.getLanguage(surveyCurrentLocale));
			const forumMessage = stui.sub("forum_msg", {
				forum: forumName,
				locale: surveyCurrentLocaleName
			});
			const userId = (surveyUser && surveyUser.id) ? surveyUser.id : 0;
			cldrStForum.loadForum(surveyCurrentLocale, userId, forumMessage, params);
		}
	};

	return Page;
});

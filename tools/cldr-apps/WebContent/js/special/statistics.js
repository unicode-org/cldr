/**
 * Statistics page
 * @module statistics
 */
define("js/special/statistics.js", ["js/special/SpecialPage.js", "dojo/number",
                                    "dijit/layout/TabContainer", "dijit/layout/ContentPane", "dojo/domReady!"],
                                    function(SpecialPage, dojoNumber, TabContainer, ContentPane) {
	var _super;

	function Page() {

	}

	_super = Page.prototype = new SpecialPage();

	Page.prototype.show = function show(params) {
		showInPop2(stui.str("statisticsGuidance"), null, null, null, true); /* show the box the first time */					
		hideLoader(null);
		isLoading=false;				
		var theDiv;
		var isNew = false;
		if(params.special.theDiv) {
			theDiv = params.special.theDiv;
		} else {
			theDiv = document.createElement("div");
			theDiv.className = params.name;
			isNew = true;
			params.special.theDiv = theDiv;
		}
		

		var statDiv = null;
		if(isNew) {
			
			theDiv.appendChild(createChunk("Statistics Overview"), "h2");
			var overviewDiv = createChunk(stui.str("loading"), "p", "helpContent");
			theDiv.appendChild(overviewDiv);
			
			theDiv.appendChild(createChunk("By Day"), "h2");
			statDiv = document.createElement("div");
			statDiv.id = "statistics_area";
			theDiv.appendChild(statDiv);


			queueXhr({
				url: contextPath + "/SurveyAjax?&what=stats_byloc",
				handleAs:"json",
				load: function(h){
					if(h.total_submitters) {
						updateIf(overviewDiv,  "Total submitters: "  + 
								dojoNumber.format(h.total_submitters) +
								", Total items: " + dojoNumber.format(h.total_items) 
								+ " ("+dojoNumber.format(h.total_new_items)+" new)");
					} else {
						//theResult.appendChild(createChunk("(search error)","i"));
					}
				},
				error: function(err, ioArgs){
					var msg ="Error: "+err.name + " - " + err.message;
//					theResult.appendChild(createChunk(msg,"i"));
				},
//				postData: searchTerm
			});
		}
		
		params.flipper.flipTo(params.pages.other, theDiv);
		// Now it's shown, can commence with dynamic load

		if(statDiv != null) { // if we are doing a new setup
			require(["js/raphael.js", "js/g.raphael.js", "js/g.line.js", "js/g.bar.js"], function() {
				// load raphael before calling this.
				window.showstats(statDiv.id);
			});
		}
	};

	return Page;
});
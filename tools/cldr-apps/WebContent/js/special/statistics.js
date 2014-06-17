/**
 * Statistics page
 * @module statistics
 */
define("js/special/statistics.js", ["js/special/SpecialPage.js", "dojo/number",
                                    //"dijit/layout/TabContainer", "dijit/layout/ContentPane",
                                    "dojox/charting/Chart", "dojox/charting/axis2d/Default", "dojox/charting/plot2d/StackedBars", "dojox/charting/widget/SelectableLegend", "dojox/charting/themes/Wetland",
                                    "dojo/domReady!"],
                                    function(SpecialPage, dojoNumber, 
                                    //TabContainer, ContentPane,
                                    Chart,axis2dDefault,StackedBars,SelectableLegend,Wetland
                                    ) {
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
		

//		var statDiv = null;
		if(isNew) {
			
			var linkToOld = createChunk("(Switch to the OLD statistics page)", "a", "notselected");
			linkToOld.href = 'statistics.jsp';
			theDiv.appendChild(linkToOld);
			
			theDiv.appendChild(createChunk("Statistics Overview", "h2"));
			var overviewDiv = createChunk(stui.str("loading"), "p", "helpContent");
			theDiv.appendChild(overviewDiv);
			
			theDiv.appendChild(createChunk("Submits By Day", "h2"));
			/*
			var helpText;
			theDiv.appendChild(helpText=createChunk("","p", "helpContent"));
			helpText.appendChild(document.createTextNode("Each line shows: "));
			helpText.appendChild(createChunk("  ","span","swatch color0"));
			helpText.appendChild(document.createTextNode("new votes,  and "));
			helpText.appendChild(createChunk("  ","span","swatch color1"));
			helpText.appendChild(document.createTextNode("imported unchanged votes. The counts at left" +
					" only reflect the new/changed votes."));
					*/
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
					theDiv.appendChild(createChunk(msg,"i", "ferrorbox"));
					console.log(err);
				},
//				postData: searchTerm
			});

			queueXhr({
				url: contextPath + "/SurveyAjax?what=stats_byday",
				handleAs: "json",
				error: function(err, ioArgs){
					var msg ="Error: "+err.name + " - " + err.message;
					theDiv.appendChild(createChunk(msg,"i", "ferrorbox"));
					console.log(err);
				},
				load: function(json) {
					if (json) {

						// munge data
						var header=json.byday.header;
						var data=json.byday.data;
						var header_new=json.byday_new.header;
						var data_new=json.byday_new.data;
						var count_old = [];
						var labels = [];
						var count_new = [];
						for(var i in data_new) {
							var newLabel = (data_new[i][header_new.LAST_MOD]).split(' ')[0];
							var newCount = Number(data_new[i][header_new.COUNT]);
							labels.push({value: Number(i)+1, text: newLabel}); // labels come from new data
							count_new.push(newCount);
							var oldLabel = (data[i][header.LAST_MOD]).split(' ')[0];
							if(newLabel == oldLabel) {
								// have old data
								var oldCount = Number(data[i][header.COUNT]);
								if(oldCount < newCount) {
									console.log("Preposterous: at " + newLabel + ": " + oldCount + " oldCount < " + newCount + "  newCount " );
									count_old.push(-1);
								} else {
									count_old.push(oldCount - newCount);
								}
							} else {
								console.log("Desync: " + newLabel + " / " + oldLabel);
								count_old.push(-1);
							}
						}
						var gdata = [];
						gdata.push(count_new);
						gdata.push(count_old);
						
				      var c = new Chart(statDiv);
				      c.addPlot("default", {type: StackedBars, hAxis: 'y', vAxis: 'x'})
				        .setTheme(Wetland)
				        .addAxis("x", {labels: labels, vertical: true})
				        .addAxis("y", {vertical: false})
				        .addSeries("New or changed votes in CLDR "+surveyVersion, count_new)
				        .addSeries("Old votes imported from CLDR "+surveyOldVersion, count_old)
				        .render();
				       var l = new SelectableLegend({chart: c});
					  l.placeAt(statDiv);
					}
				}
			});
		}
		
		params.flipper.flipTo(params.pages.other, theDiv);
		// Now it's shown, can commence with dynamic load
/*
 * Old and busted raphael graph
		if(statDiv != null) { // if we are doing a new setup
			require(["js/raphael.js", "js/g.raphael.js", "js/g.line.js", "js/g.bar.js"], function() {
				// load raphael before calling this.
				window.showstats(statDiv.id);
			});
		}
		*/
	};

	return Page;
});
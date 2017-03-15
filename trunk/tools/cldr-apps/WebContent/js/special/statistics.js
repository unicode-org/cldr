/**
 * Statistics page
 * @module statistics
 */
define("js/special/statistics.js", ["js/special/SpecialPage.js", "dojo/number",
                                    //"dijit/layout/TabContainer", "dijit/layout/ContentPane",
                                    "dojox/charting/Chart", "dojox/charting/axis2d/Default", 
                                    "dojox/charting/plot2d/StackedBars", "dojox/charting/widget/SelectableLegend", 
                                    "dojox/charting/themes/Distinctive", //http://archive.dojotoolkit.org/nightly/dojotoolkit/dojox/charting/tests/test_themes.html
                                    "dojox/charting/action2d/Tooltip",
                                    "dojo/domReady!"],
                                    function(SpecialPage, dojoNumber, 
                                    //TabContainer, ContentPane,
                                    Chart,axis2dDefault,StackedBars,SelectableLegend,Wetland,Tooltip
                                    ) {
	var _super;

	function Page() {
		this.sections = {};
		for(var k =0;k<this.sectionArray.length;k++) {
			var aSection = this.sectionArray[k];
			if(!aSection.url && aSection.url !== false) {
				aSection.url = contextPath + "/SurveyAjax?&what=stats_"+aSection.name;
			}
			this.sections[aSection.name]=aSection;
			if(aSection.isDefault) {
				this.curSection = aSection.name;
			}
		}
	}

	_super = Page.prototype = new SpecialPage();

	Page.prototype.sectionArray = [
		{
			isDefault: true,
			name: "overview",
			url: contextPath + "/SurveyAjax?&what=stats_byloc",
			show: function(json, theDiv, params) {
				theDiv.appendChild(createChunk("Total submitters: "  + 
						dojoNumber.format(json.total_submitters) +
						", Total items: " + dojoNumber.format(json.total_items) 
						+ " ("+dojoNumber.format(json.total_new_items)+" new)",
						"p", "helpContent"));
			}
		},
		{
			name: 'byday',
			show: function(json, theDiv, params) {
				var statDiv = theDiv;
				// munge data
				var header=json.byday.header;
				var data=json.byday.data;
				var header_new=json.byday_new.header;
				var data_new=json.byday_new.data;
				var count_old = [];
				var labels = [];
				var count_new = [];
				for(var i in data_new) {
					var newLabel = new Date(data_new[i][header_new.LAST_MOD]).toLocaleDateString();
					var newCount = Number(data_new[i][header_new.COUNT]);
					labels.push({value: Number(i)+1, text: newLabel}); // labels come from new data
					count_new.push(newCount);
					var oldLabel = new Date(data[i][header.LAST_MOD]).toLocaleDateString();
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
				.addAxis("x", {labels: labels, vertical: true, dropLabels: false, labelSizeChange: true, minorLabels: false, majorTickStep: 1})
				.addAxis("y", {vertical: false})
				.addSeries("Just New or changed votes in CLDR "+surveyVersion, count_new)
				.addSeries("+ Old votes from CLDR "+surveyOldVersion, count_old);
				
				var tip = new Tooltip(c, 'default', {
					text: function(o) {
						return '<span style="font-size: smaller;">'+labels[o.index].text +"</span><br>"+dojoNumber.format(count_new[o.index])+" new,<br>"+
						dojoNumber.format(count_old[o.index])+" imported";
					}
				});

				c.render();
				var l = new SelectableLegend({chart: c});
				l.placeAt(statDiv);
			}
		},
		{
			name: 'byloc',
			show: function(json, theDiv, params) {
				var labels = [];
				var count_all = [];
				
				var header = json.stats_byloc.header;			
				for(var k=0;k<json.stats_byloc.data.length;k++) {
					var row = json.stats_byloc.data[k];
					// json.stats_byloc.header.LOCALE/COUNT/LOCALE_NAME
					// json.stats_byloc.data
					var loc = row[header.LOCALE];
					labels.push({value: k+1, loc: loc, text: locmap.getLocaleName(loc)});
					count_all.push(row[header.COUNT]);
				}
				
				
				var statDiv = theDiv;
				statDiv.style.height = (json.stats_byloc.data.length*1)+'em';
				var c = new Chart(statDiv);
				c.addPlot("default", {type: StackedBars, hAxis: 'y', vAxis: 'x'})
				.setTheme(Wetland)
				.addAxis("x", {labels: labels, vertical: true, dropLabels: false, labelSizeChange: false, minorLabels: false, majorTickStep: 1})
				.addAxis("y", {vertical: false})
				.addSeries("all votes", count_all);
				var tip = new Tooltip(c, 'default', {
					text: function(o) {
						return labels[o.index].text +"   <br>"+count_all[o.index];
					}
				});
				c.render();
				var l = new SelectableLegend({chart: c});
				l.placeAt(statDiv);
			}
		},
		{
			name: 'recent',
			url: false,
			show: function(json, theDiv, params) {
				showRecent(theDiv);
//				<h3>Recently submitted items</h3>
//
//				<div id='submitItems'>
//				</div>
//				...
//
//				<script>
//				showRecent('submitItems')
//				</script>
			}
		}
	];

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
		
		if(isNew) {
			// for now - just show all sections sequentially.
//			var linkToOld = createChunk("(Switch to the OLD statistics page)", "a", "notselected");
//			linkToOld.href = 'statistics.jsp';
//			theDiv.appendChild(linkToOld);
			
			for(var k=0;k<this.sectionArray.length;k++) {
				var theSection = this.sectionArray[k];
				var subFragment = theDiv; //document.createDocumentFragment();
				

				var subDiv = document.createElement('div');
				var sectionId = subDiv.id = 'stats_' + theSection.name;
				var sectionTitle = stui.str(sectionId);
				subFragment.appendChild(createChunk(sectionTitle, "h2"));
				subDiv.className = 'statArea';
				subFragment.appendChild(subDiv);
				
				if(theSection.url){
					(function(theSection,subDiv,params){
						var loading = createChunk(stui.str("loading"), "p", "helpContent");
						subDiv.appendChild(loading);
						queueXhr({
							url: theSection.url,
							handleAs: 'json',
							load: function(json) {
								if(json.err) {
									updateIf(loading, "Error: " + json.err);
									console.log("Err loading " + theSection.name + " - " + json.err);
								} else {
									updateIf(loading,"");
									theSection.show(json, subDiv, params);
								}
							},
							error: function(err, ioArgs) {
								updateIf(loading, "Error: " + err.name + "-"+err.message);
								console.log("Err loading " + theSection.name + " - " + err);
							}
						});
					})(theSection,subDiv,params);
				} else {
					theSection.show(null, subDiv, params);
				}
				
				//theDiv.appendChild(subFragment);
			}
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
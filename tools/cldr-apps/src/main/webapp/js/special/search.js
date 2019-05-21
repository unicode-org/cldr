/**
 * Search module.
 * May still be under construction.
 * 
 * @module search
 */
define("js/special/search.js", ["js/special/SpecialPage.js"], function(SpecialPage) {
	var _super;
	
	/**
	 * @constructor
	 * @class search
	 */
	function Page() {
		this.searchCache = {};
	}

	_super = Page.prototype = new SpecialPage();
	
	/**
	 * @function show
	 */
	Page.prototype.show = function show(params) {
		// setup
		var searchCache = params.special.searchCache;
		
		hideLoader(null);
		isLoading=false;					
		var theDiv = document.createElement("div");
		theDiv.className = 'search';
	
		// install
		var theInput = document.createElement("input");
		theDiv.appendChild(theInput);
		
		var theSearch = createChunk(stui.str("search"), "button");
		theDiv.appendChild(theSearch);
		
		var theResult = document.createElement("div");
		theResult.className = 'results';
		theDiv.appendChild(theResult);
	
		
		var newLocale = surveyCurrentLocale;
		
		var showResults = function showResults(searchTerm) {
			var results=searchCache[searchTerm];
			removeAllChildNodes(theResult);
			if(newLocale!=surveyCurrentLocale) {
				var newName = locmap.getLocaleName(newLocale);
				theResult.appendChild(createChunk(newName, "h4"));
			}
			theResult.appendChild(createChunk(searchTerm, "h3"));
			
			if(results.length == 0) {
				theResult.appendChild(createChunk(stui.str("searchNoResults", "h3", "searchNoResults")));
			} else {
				for(var i=0;i<results.length;i++) {
					var result = results[i];
					
					var theLi = document.createElement("li");
					
					var appendLink = function appendLink(title, url, theClass) {
						var theA = createChunk(title, "a");
						if(url && newLocale!='' && newLocale!=null) {
							theA.href = url;
						}
						if(theClass!=null) {
							theA.className = theClass;
						}
						theLi.appendChild(theA);
					};
					
					if(result.xpath) {
						if(result.strid) {
							codeUrl = "#/"+newLocale+"//"+result.strid;
						}
						appendLink(result.xpath, codeUrl, "xpath");
					}
					
					if(result.ph) {
						result.ph.strid = result.strid;
						result = result.ph; // pick up as section
					}
					
					var codeUrl = null;
					
					if(result.section) {
						codeUrl =  "#/"+newLocale+"/"+result.section+"/!";
						if(result.page) {
							codeUrl = "#/"+newLocale+"/"+result.page+"/";
							if(result.strid) {
								codeUrl = "#/"+newLocale+"/"+result.page+"/"+result.strid;
							}
						}
					}
					
					if(result.section) {
						appendLink(result.section, codeUrl);
						if(result.page) {
							theLi.appendChild(createChunk("»"));
							appendLink(result.page, codeUrl);
							if(result.code) {
								theLi.appendChild(createChunk("»"));
								appendLink(result.code, codeUrl, "codebox");
							}
						}
					}
					
					if(result.loc) {
						params.exports.appendLocaleLink(theLi, result.loc, locmap.getLocaleInfo(result.loc), true);
					}
					 
					theResult.appendChild(theLi);
				}
			}
			
			theResult.last = searchTerm;
			theResult.loc = newLocale;
		};
		
		var showSearchTerm = function showSearchTerm(searchTerm) {
			if((searchTerm != theResult.last || theResult.loc != newLocale) && searchTerm != null) {
				theResult.last = null;
				theResult.loc = null;
				removeAllChildNodes(theResult);
				theResult.appendChild(createChunk(searchTerm, "h3"));
				
				if(!(searchTerm in searchCache)) {
					   var xurl = contextPath + "/SurveyAjax?&s="+surveySessionId+"&what=search"; // allow cache
					   if(newLocale!=null&&newLocale!='') {
						   xurl = xurl + "&_="+newLocale;
					   }
					   queueXhr({
					        url:xurl, // allow cache
				 	        handleAs:"json",
				 	        load: function(h){
				 	        	if(h.results) {
				 	        		searchCache[searchTerm] = h.results;
						 			showResults(searchTerm);
				 	        	} else {
				 	        		theResult.appendChild(createChunk("(search error)","i"));
				 	        	}
					        },
					        error: function(err, ioArgs){
					 			var msg ="Error: "+err.name + " - " + err.message;
			 	        		theResult.appendChild(createChunk(msg,"i"));
					        },
					        postData: searchTerm
					    });
				} else {
					showResults(searchTerm);
				}
				
	
			} else {
				//no change;
			}
		};
		
		var searchFn = function searchFn(e) {
			var searchTerm = theInput.value;
			
			if(searchTerm.indexOf(':')>0) {
				var segs = searchTerm.split(':');
				if(locmap.getLocaleInfo(segs[0])!=null) {
					newLocale = segs[0];
					// goto
					if(segs.length==1) {
						surveyCurrentSpecial='';
						surveyCurrentLocale=newLocale;
						reloadV();
						return;
					}
					searchTerm = segs[1];
				}
			}
			
			showSearchTerm(searchTerm);
	
			return stStopPropagation(e);
		};
		
		listenFor(theInput, "change", searchFn);
		listenFor(theSearch, "click", searchFn);
	
		params.flipper.flipTo(params.pages.other, theDiv);
		theInput.focus();
		surveyCurrentLocale=null;
		surveyCurrentSpecial='search';
		showInPop2(stui.str("searchGuidance"), null, null, null, true); /* show the box the first time */					
	};
	

	return Page;
});
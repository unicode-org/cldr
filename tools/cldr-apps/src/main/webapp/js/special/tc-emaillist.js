/**
 * Example special module that shows a blank page. 
 * Modify 'js/special/tc-emaillist.js' below to reflect your special page's name.
 * @module blank
 */
define("js/special/tc-emaillist.js", ["js/special/SpecialPage.js"], function(SpecialPage) {
	var _super;
	
	function Page() {
		// constructor
	}
	
	// set up the inheritance before defining other functions
	_super = Page.prototype = new SpecialPage();
	
	Page.prototype.show = function show(params) {
		// set up the DIV you want to show the world
		var ourDiv = document.createElement("div");
		ourDiv.appendChild(createChunk("Email address of users who participated:","h2"));
		var loader = createChunk("Loading... (This takes a while, please be patient!)","i","warn");
		ourDiv.appendChild(loader);
		loader.appendChild(createChunk("","div","loaderAnimIcon"));
		// set up the 'right sidebar'
		showInPop2("If you have permission, this will show users who participated in the SurveyTool for CLDR " + surveyVersion, null, null, null, true); /* show the box the first time */					
		
		// No longer loading
		hideLoader(null);
		
		var theTextarea = createChunk("", "textarea","userList");
		params.exports.clickToSelect(theTextarea);

		// Flip to the new DIV
		params.flipper.flipTo(params.pages.other, ourDiv);
		var xurl = contextPath + "/SurveyAjax?&s="+surveySessionId+"&what=participating_users"; // allow cache
	    queueXhr({
	        url:xurl, // allow cache
 	        handleAs:"json",
 	        load: function(h){
 	        	if(h.err) {
 	        		params.special.showError(params, h, {what: "Loading user list"});
 	        	} else if(h.participating_users) {
 	        		loader.appendChild(document.createTextNode(" ... parsing list..."));
 	        		var list="";
 	        		for(var i in h.participating_users.data) {
 	        			var row = h.participating_users.data[i];
 	        			var email=row[h.participating_users.header.EMAIL];
 	        			if(email != "admin@") {
 	        				list = list + email +", ";
 	        			}
 	        		} 	        		
 	        		ourDiv.removeChild(loader);
 	        		theTextarea.appendChild(document.createTextNode(list));
 	        		ourDiv.appendChild(theTextarea);
 	        	} else {
 	        		ourDiv.removeChild(loader);
 	        		alert('no data- or no users participated.');
 	        	}
	        },
	        error: function(err, ioArgs){
	        	params.special.showError(params, null, {err: err, what: "Loading participating users"});
	        }
	    });

	};


	return Page;
});
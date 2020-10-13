/**
 * Example special module that shows a forum page. 
 * Modify 'js/special/flagged.js' below to reflect your special page's name.
 * @module forum
 */
define("js/special/flagged.js", ["js/special/SpecialPage.js", "dojo/request", "js/modules/usermap.js"], 
		function(SpecialPage, request, UserMap) {
	var _super;
	
	function Page() {
		// constructor
	}
	
	// set up the inheritance before defining other functions
	_super = Page.prototype = new SpecialPage();

	/**
	 * parse a hash tag
	 * @function parseHash
	 */
	Page.prototype.parseHash = function parseHash(hash, pieces) {
		surveyCurrentPage='';
	};
	
	Page.prototype.handleIdChanged = function handleIdChanged(strid) {
	};

	Page.prototype.show = function show(params) {
		request
		.get('SurveyAjax?s='+surveySessionId+'&what=flagged', {handleAs: 'json'})
		.then(function(json) {
			if(json.err) {
	        	params.special.showError(params, json, {what: "Loading flagged data"});
	        	return;
			}
			// set up the 'right sidebar'
			const pucontent = showInPop2(stui.str(params.name+"Guidance"), null, null, null, true); /* show the box the first time */					
			
			var ourDiv = document.createElement("div");
			ourDiv.className = 'special_'+params.name;

			var header = json.flagged.header;
			var rows = json.flagged.data;
			var lastLocale = undefined;
			var mymap = new UserMap();
			
			/**
			 * Add in the user's data. Keep it small, vertically.
			 */
			function addUserInfo(err, args, o) {
				if(err || o.entry.err || !o.entry.user) {
				} else {
					console.log('Got user: ' + JSON.stringify(o.entry.user));
					var thinUser = mymap.createUserThin(o.entry.user);
					console.log('Got thinuser: ' + JSON.stringify(thinUser));
					//window._THIN = thinUser;
					//thinUser.style.display = 'inline'
					thinUser.appendTo(args.theRow);
//					$('<pre></pre>', { text: JSON.stringify(o.entry) }).appendTo(args.theRow);
				}
			}
			
			var totalCount = 0;
			var totalCountChunk = $('<span></span>', 
				{text: Number(0).toLocaleString()});
			var totalCountHeader = $('<h3></h3>',
				{text:stui.str('flaggedTotalCount')});
			totalCountChunk.appendTo(totalCountHeader);
			totalCountHeader.appendTo(ourDiv);
			var lastCount = 0;
			var lastCountChunk = null;
			for(var r=0;r<rows.length;r++) {
				var row = rows[r];
				
				if(row[header.LOCALE] !== lastLocale) {
					lastCount = 0;
					// emit a header
					var h4 = $('<h4></h4>', {class: 'flaggedLocaleHeader'});
					var asLink = $('<a></a>', {text: row[header.LOCALE_NAME], href: ("#/"+row[header.LOCALE])});
					asLink.appendTo(h4);
					lastCountChunk = $('<span></span>', 
						{text: ''});
					lastCountChunk.appendTo(h4);
					h4.appendTo(ourDiv);
					lastLocale = row[header.LOCALE]; // don't show the header next time
				} else {
				}
				lastCountChunk.text(Number(++lastCount).toLocaleString());
				totalCountChunk.text(Number(++totalCount).toLocaleString());
				
				var theRow = $('<div></div>', {class: 'flaggedItem'});
				var theDateChunk = $('<span></span>', { class: 'dateChunk', text: new Date(row[header.LAST_MOD]).toLocaleDateString()} );
				theDateChunk.appendTo(theRow);
				
				var theXpathChunk = $('<span></span>', {class: 'pathHeaderInfo'} );
				
				var theXpathLink = $('<a></a>', { text: row[header.XPATH_CODE].replace(/\t/, " : "), 
					href: ('#/' + row[header.LOCALE] + '//' + row[header.XPATH_STRHASH]) } );
				
				theXpathLink.appendTo(theXpathChunk);
				theXpathChunk.appendTo(theRow);
				
				if(surveyUser) {
					// if logged in- try to get user info
					var theirUserId = row[header.SUBMITTER];
					mymap.get(theirUserId, addUserInfo, {theRow: theRow});
				}
				
				theRow.appendTo(ourDiv);
			}
			//ourDiv.appendChild(createChunk(JSON.stringify(json.flagged), "tt"));
			
			// No longer loading
			hideLoader(null);
			params.flipper.flipTo(params.pages.other, ourDiv);
			params.special.handleIdChanged(surveyCurrentId); // rescroll.

			if(surveyUserPerms.userIsTC) {
				// For TC, show button (includes emails, so TC only)
				const csvButton = $('<form></form>', {action: "DataExport.jsp?do=flagged&s=" + surveySessionId});
				csvButton.append($('<input></input', {type: 'submit', value: stui.str('downloadCsvLink')}));
				$(pucontent).append(csvButton);
			}
		})
		.otherwise(function(err) {
        	params.special.showError(params, null, {err: err, what: "Loading forum data"});
		});
	};

	return Page;
});
/**
 * Example special module that shows a forum page. 
 * Modify 'js/special/forum.js' below to reflect your special page's name.
 * @module forum
 */
define("js/special/forum.js", ["js/special/SpecialPage.js", "dojo/request"], function(SpecialPage, request) {
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
	SpecialPage.prototype.parseHash = function parseHash(hash, pieces) {
		surveyCurrentPage='';
		console.log(JSON.stringify(pieces));
		if(pieces && pieces.length>3){
			if(!pieces[3] || pieces[3]=='') {
				surveyCurrentId='';
			} else {
				var id = new Number(pieces[3]);
				if(id == NaN) {
					surveyCurrentId = '';
				} else {
					surveyCurrentId = id.toString();
				}
			}
		}
	};

	Page.prototype.show = function show(params) {
	
		if(surveyCurrentLocale=='') {
			hideLoader(null);
			params.flipper.flipTo(params.pages.other, createChunk(stui.str("generic_nolocale"),"p","helpContent"));
		} else {
			request
			.get('SurveyAjax?s='+surveySessionId+'&what=forum_fetch&xpath=0&_='+surveyCurrentLocale, {handleAs: 'json'})
			.then(function(json) {
				if(json.err) {
		        	params.special.showError(params, json, {what: "Loading forum data"});
		        	return;
				}
				// set up the 'right sidebar'
				showInPop2(stui.str(params.name+"Guidance"), null, null, null, true); /* show the box the first time */					
				
				var ourDiv = document.createElement("div");
				
				var postButton = createChunk(stui.str("forumNewPostButton"), "button", "btn btn-default btn-sm");
				postButton.appendChild(createChunk("","span",""));
				listenFor(postButton, "click", function(e) {
					openReply({
						locale: surveyCurrentLocale,
						onReplyClose: function(postModal, form, formDidChange) {if(formDidChange){console.log('Reload- changed.');reloadV();}},
						//xpath: '',
						//replyTo: post.id,
						//replyData: post
					});
					stStopPropagation(e);
					return false;
				});
				ourDiv.appendChild(postButton);
				ourDiv.appendChild(document.createElement('hr'));
				if(json.ret.length == 0) {
					ourDiv.appendChild(createChunk(stui.str("forum_noposts"),"p","helpContent"));
				} else {
					ourDiv.appendChild(parseForumContent({ret: json.ret,
						replyButton: true,
						onReplyClose: function(postModal, form, formDidChange) {if(formDidChange){console.log('Reload- changed.');reloadV();}},
						noItemLink: false}));
				}
				
				// No longer loading
				hideLoader(null);
				params.flipper.flipTo(params.pages.other, ourDiv);
			})
			.otherwise(function(err) {
	        	params.special.showError(params, null, {err: err, what: "Loading forum data"});
			});
		}
	};


	return Page;
});
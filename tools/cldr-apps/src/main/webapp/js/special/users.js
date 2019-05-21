/**
 * Special module that shows a users page. 
 * @module users
 */
define("js/special/users.js", ["js/special/SpecialPage.js"], function(SpecialPage) {
	var _super;
	
	function Page() {
		// constructor
	}
	
	// set up the inheritance before defining other functions
	_super = Page.prototype = new SpecialPage();
	
	Page.prototype.show = function show(params) {
		// set up the DIV you want to show the world
		var ourDiv = createChunk("","div","");

		// set up the 'right sidebar'
		showInPop2(stui.str("users_guidance"), null, null, null, true); /* show the box the first time */					
		
		// No longer loading
		hideLoader(null);

		// Flip to the new DIV
		params.flipper.flipTo(params.pages.other, ourDiv);

		// set up the URL to fetch users
		
	    var xurl = contextPath + "/SurveyAjax?&s="+surveySessionId+"&what=user_list"; // allow cache

	    
	    
		$.ajax( {
			context: ourDiv,
			url: xurl
		})
		.done(function(data) {
			//$(this).text(JSON.stringify(data));
				var createCheckbox = function createCheckbox(opts, addto) {
					var label = $('<label />');
					var box = $('<input />', { type: 'checkbox', checked: opts.value || false });
					label.text(opts.name);
					box.prependTo(label);
					label.appendTo(addto);
					return box;
				}
				
			$('this').remove(); // clear
			// TODO: add descriptive text?
			var showLocked = createCheckbox({name: 'Hide locked', value: true}, this);
			
			var locked = [];
			
			var byEmail = {}; // email -> u:{}
			var byId = {}; // id -> u:{}
			
			showLocked.on("change", function() {
				for(var k in locked) {
					if(showLocked.is(':checked')) {
						locked[k].hide();
					} else {
						locked[k].show();
					}
				}
			});
			
			var lastHead;
			
			for(var k in data.users) {
				var u = {
					data: data.users[k]
				};
				byEmail[u.data.email] = u;
				byId[u.data.id] = u;
				if(!lastHead || lastHead !== u.data.org) {
					$('<h1>', {text: u.data.org}).appendTo(this);
					lastHead = u.data.org;
				}
				
				u.div = createUser(u.data);
				u.obj = $(u.div);
				if(u.data.userlevelName === "locked") {
					u.obj.hide();
					locked.push(u.obj);
				}
				$(this).append(u.obj);
				
				u.infoSpan = $('<span />');
				u.infoSpan.appendTo(u.obj);

				u.infoButton = $('<button />', {text: stui.str('users_infoVotesButton')});
				u.infoButton.appendTo(u.obj);
				
				u.infoButton.on('click',  {
					u: u // break closure
				},	function(event) {
						var u = event.data.u;
						var xurl2 = contextPath + "/SurveyAjax?&s="+surveySessionId+"&what=user_oldvotes&old_user_id="+u.data.id;
						console.log(xurl2);
						$(u.infoSpan).removeClass('ferrbox');
					    u.infoSpan.text('loading..');
						$.ajax( {
							context: u.infoSpan,
							url: xurl2
						})
						.done(function(data2) {
							if(!data2.user_oldvotes.data || data2.user_oldvotes.data.length == 0) {
								$(u.infoSpan).text('no old votes.');
							} else {
								// Crudely display the data. For now, just simplify slightly to make more legible.
								$(u.infoSpan).text('old votes: ' + JSON.stringify(data2.user_oldvotes.data).replace(/[\\\"]/g, ''));
							}
						})
						.fail(function(err) {
							$(u.infoSpan).addClass('ferrbox');
							$(u.infoSpan).text('Error loading users: Status ' + JSON.stringify(err.status));
						});
				});

				u.loadOldVotes = $('<button />', {text: stui.str('users_loadVotesButton')});
				u.loadOldVotes.appendTo(u.obj);
				
				u.loadOldVotes.on('click', {
					u: u // break closure
				}, function(event) {
					var u = event.data.u;
					var oldUserEmail = prompt("First, pardon the modality.\nNext, do you want to import votes to '#"+u.data.id+' '+u.data.email+"' FROM another user's old votes? Enter their email address below:");
					if(!oldUserEmail) {
						return;
					}
					
					var oldUser = byEmail[oldUserEmail];
					
					if(!oldUser) {
						alert('Could not find user '+oldUserEmail+' - double check the address.');
						return;
					}
					
					var oldLocale = prompt("Enter the locale id to import FROM " + oldUser.data.name + " <"+oldUser.data.email+"> #"+oldUser.data.id);
					if(!oldLocale) {
						alert('Cancelled.');
						return;
					}
					if(!locmap.getLocaleInfo(oldLocale)) {
						alert('Not a valid locale id: ' + oldLocale);
						return;
					}
					
					var newLocale = prompt("Enter the locale id to import TO " + u.data.email, oldLocale);
					if(!newLocale) {
						alert('Cancelled.');
						return;
					}

					if(!locmap.getLocaleInfo(newLocale)) {
						alert('Not a valid locale id: ' + newLocale);
						return;
					}

					if(!confirm("Sure? Import FROM " + locmap.getLocaleName(oldLocale) + " @ " + oldUser.data.email + " TO " + locmap.getLocaleName(newLocale) + " @ " + u.data.email)) {
						return;
					}
					
					
					var xurl3 = contextPath + "/SurveyAjax?&s="+surveySessionId+"&what=user_xferoldvotes&from_user_id="+oldUser.data.id+"&from_locale="+oldLocale+"&to_user_id="+u.data.id+"&to_locale="+newLocale;
					console.log(xurl3);
					$(u.infoSpan).removeClass('ferrbox');
				    u.infoSpan.text('TRANSFER FROM ' + locmap.getLocaleName(oldLocale) + " @ " + oldUser.data.email + " TO " + locmap.getLocaleName(newLocale) + " @ " + u.data.email);
					$.ajax( {
						context: u.infoSpan,
						url: xurl3
					})
					.done(function(data3) {
						if(data3.user_xferoldvotes) {
							$(u.infoSpan).text(JSON.stringify(data3.user_xferoldvotes));
						} else if(data3.err) {
							$(u.infoSpan).addClass('ferrbox');
							$(u.infoSpan).text('Error : ' + data3.err);
						} else {
							$(u.infoSpan).addClass('ferrbox');
							$(u.infoSpan).text('Error : ' + JSON.stringify(data3));
						}
					})	
					.fail(function(err) {
						$(u.infoSpan).addClass('ferrbox');
						$(u.infoSpan).text('Error transferring data: Status ' + JSON.stringify(err.status));
					});

					
				});
			}
		})
		.fail(function(err) {
			$(this).addClass('ferrbox');
			$(this).text('Error loading users: Status ' + JSON.stringify(err.status));
		});
		

	};


	return Page;
});
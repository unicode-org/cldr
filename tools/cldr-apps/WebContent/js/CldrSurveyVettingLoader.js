/*
 * CldrSurveyVettingLoader.js - split off from survey.js, for CLDR Survey Tool
 * showV() and reloadV() and related functions
 */

// TODO: replace with AMD [?] loading
dojo.require("dojo.i18n");
dojo.require("dojo.string");

/*
 * haveDialog: when true, it means a "dialog" of some kind is displayed.
 * Used for inhibiting $('#left-sidebar').hover in redesign.js.
 * Currently there are only two such dialogs, both for auto-import.
 *
 * TODO: make it a property of one of our own objects instead of "window".
 */
window.haveDialog = false;

/**
 * Utilities for the 'v.jsp' (dispatcher) page. Call this once in the page. It expects to find a node #DynamicDataSection
 * The function showV() itself is called only by v.jsp.
 */
function showV() {
	// REQUIRES
	require([
		"dojo/ready",
		"dojo/dom",
		"dojo/parser",
		"dijit/DropDownMenu",
		"dijit/form/DropDownButton",
		"dijit/MenuSeparator",
		"dijit/MenuItem",
		"dijit/form/TextBox",
		"dijit/form/Button",
		"dijit/CheckedMenuItem",
		"dijit/Dialog",
		"dijit/registry",
		"dijit/PopupMenuItem",
		"dijit/form/Select",
		"dojox/form/BusyButton",
		"dijit/layout/StackContainer",
		"dijit/TitlePane",
		"dojo/hash",
		"dojo/topic",
		"dojo/dom-construct",
		"dojo/number",
		"dojo/domReady!"
	],
	// HANDLES
	function(
		ready,
		dom,
		parser,
		DropDownMenu,
		DropDownButton,
		MenuSeparator,
		MenuItem,
		TextBox,
		Button,
		CheckedMenuItem,
		Dialog,
		registry,
		PopupMenuItem,
		Select,
		BusyButton,
		StackContainer,
		TitlePane,
		dojoHash,
		dojoTopic,
		domConstruct,
		dojoNumber
	) {
		loadStui(null, function(/*stui*/) {

			var appendLocaleLink = function appendLocaleLink(subLocDiv, subLoc, subInfo, fullTitle) {
				var name = locmap.getRegionAndOrVariantName(subLoc);
				if(fullTitle) {
					name = locmap.getLocaleName(subLoc);
				}
				var clickyLink = createChunk(name, "a", "locName");
				clickyLink.href = linkToLocale(subLoc);
				subLocDiv.appendChild(clickyLink);
				if(subInfo == null) {
					console.log("* internal: subInfo is null for " + name + " / " + subLoc);
				}
				if(subInfo.name_var) {
					addClass(clickyLink, "name_var");
				}
				clickyLink.title=subLoc; // remove auto generated "locName.title"

				if(subInfo.readonly) {
					addClass(clickyLink, "locked");
					addClass(subLocDiv, "hide");

					if(subInfo.readonly_why) {
						clickyLink.title = subInfo.readonly_why;
					} else if(subInfo.dcChild) {
						clickyLink.title = stui.sub("defaultContentChild_msg", { info: subInfo, locale: subLoc, dcChildName: locmap.getLocaleName(subInfo.dcChild)});
					} else {
						clickyLink.title = stui.str("readonlyGuidance");
					}
				} else if(window.canmodify && subLoc in window.canmodify) {
					addClass(clickyLink, "canmodify");
				}
				else {
					addClass(subLocDiv, "hide");
				}
				return clickyLink;
			};

			/**
			 * list of pages to use with the flipper
			 * @property pages
			 */
			var pages = {
					loading: "LoadingMessageSection",
					data: "DynamicDataSection",
					other: "OtherSection",
			};
			var flipper = new Flipper( [pages.loading, pages.data, pages.other] );

			var pucontent = dojo.byId("itemInfo");
			var theDiv = flipper.get(pages.data);
			theDiv.pucontent = pucontent;
			theDiv.stui = loadStui();

			pucontent.appendChild(createChunk(stui.str("itemInfoBlank"),"i"));

			/**
			 * List of buttons/titles to set.
			 * @property menubuttons
			 */
			var menubuttons = {
				locale: "title-locale",
				section: "title-section",
				page: "title-page",
				dcontent: "title-dcontent",

				set: function(x,y) {
					stdebug("menuset " + x + " = " + y);
					var cnode = dojo.byId(x+"-container");
					var wnode = this.getRegistry(x);
					var dnode = this.getDom(x);
					if(!cnode) cnode = dnode; // for Elements that do their own stunts
					if(y && y !== '-' && y !== '') {
						if(wnode != null) {
							wnode.set('label',y);
						} else  {
							updateIf(x,y); // non widget
						}
						setDisplayed(cnode, true);
					} else {
						setDisplayed(cnode, false);
						if(wnode != null) {
							wnode.set('label','-');
						} else  {
							updateIf(x,'-'); // non widget
						}
					}
				},
				getDom: function(x) {
					return dojo.byId(x);
				},
				getRegistry: function(x) {
					return registry.byId(x);
				},
				getContainer: function(x) {
					return dojo.byId(x+"-container");
				}
			};

			// TODO remove this debug item
			window.__FLIPPER = flipper;

			/**
			 * Manage additional special pages
			 * @class OtherSpecial
			 */
			function OtherSpecial() {
				// cached page list
				this.pages = {};
			}

			/**
			 * @function getSpecial
			 */
			OtherSpecial.prototype.getSpecial = function getSpecial(name) {
				return this.pages[name];
			};

			/**
			 * @function loadSpecial
			 */
			OtherSpecial.prototype.loadSpecial = function loadSpecial(name, onSuccess, onFailure) {
				var special = this.getSpecial(name);
				var otherThis = this;
				if(special) {
					stdebug("OS: Using cached special: "+ name);
					onSuccess(special);
				} else if (special === null) {
					stdebug("OS: cached NULL: " + name);
					onFailure("Special page failed to load: " + name);
				} else {
					stdebug("OS: Attempting load.." + name);
					try {
						require(["js/special/"+name+".js"], function(specialFn) {
							stdebug("OS: Loaded, instantiatin':" + name);
							var special = new specialFn();
							special.name = name;
							otherThis.pages[name] = special; // cache for next time

							stdebug("OS: SUCCESS! " + name);
							onSuccess(special);
						});
					} catch(e) {
						stdebug("OS: Load FAIL!:" + name + " - " + e.message + " - " + e);
						if(!otherThis.pages[name]) { // if the load didn't complete:
							otherThis.pages[name]=null; // mark as don't retry load.
						}
						onFailure(e);
					}
				}
			};

			/**
			 * @function parseHash
			 */
			OtherSpecial.prototype.parseHash = function parseHash(name, hash, pieces) {
				this.loadSpecial(name, function onSuccess(special) {
					special.parseHash(hash, pieces);
				}, function onFailure(e) {
					/*
					 * TODO: get rid of this console warning for name = "oldvotes".
					 * There's not a known problem with old votes. It's not clear why
					 * the warning occurs. See "window.parseHash(dojoHash())"
					 */
					console.log("OtherSpecial.parseHash: Failed to load " + name + " - " + e);
				});
			};

			/**
			 * @function handleIdChanged
			 */
			OtherSpecial.prototype.handleIdChanged = function handleIdChanged(name, id) {
				this.loadSpecial(name, function onSuccess(special) {
					special.handleIdChanged(id);
				}, function onFailure(e) {
					console.log("OtherSpecial.handleIdChanged: Failed to load " + name + " - " + e);
				});
			};

			/**
			 * @function showPage
			 */
			OtherSpecial.prototype.show = function show(name, params) {
				this.loadSpecial(name, function onSuccess(special) {
					// populate the params a little more
					params.otherSpecial = this;
					params.name = name;
					params.special = special;

					// add anything from scope..

					params.exports = {
							// All things that should be separate AMD modules..
							appendLocaleLink: appendLocaleLink,
							handleDisconnect: handleDisconnect,
							clickToSelect: clickToSelect
					};

					special.show(params);
				}, function onFailure(err) {

					// extended error
					var loadingChunk;
					var msg_fmt = stui.sub("v_bad_special_msg",
							{special: name });
					params.flipper.flipTo(params.pages.loading, loadingChunk = createChunk(msg_fmt,"p","errCodeMsg"));
					isLoading=false;
				});
			};

			/**
			 * instance of otherSpecial manager
			 * @property otherSpecial
			 */
			var otherSpecial = new OtherSpecial();

			/**
			 * parse the hash string into surveyCurrent___ variables.
			 * Expected to update document.title also.
			 * @method parseHash
			 * @param {String} id
			 */
			window.parseHash = function parseHash(hash) {
				function updateWindowTitle() {
					var t=stui.str('survey_title');
					if(surveyCurrentLocale && surveyCurrentLocale != '') {
						t = t + ': '+locmap.getLocaleName(surveyCurrentLocale);
					}
					if(surveyCurrentSpecial && surveyCurrentSpecial!='') {
						t = t + ': ' + stui.str('special_'+surveyCurrentSpecial);
					}
					if(surveyCurrentPage && surveyCurrentPage !='') {
						t = t + ': ' + surveyCurrentPage;
					}
					document.title = t;
				};
				if(hash) {
					var pieces = hash.substr(0).split("/");
					if(pieces.length > 1) {
						surveyCurrentLocale = pieces[1]; // could be null
						/*
						 * TODO: find a way if possible to fix here when surveyCurrentLocale === "USER".
						 * It may be necessary (and sufficient) to wait for server response, see "USER" elsewhere
						 * in this file. cachedJson.loc and _thePages.loc are generally (always?) undefined here.
						 * Reference: https://unicode.org/cldr/trac/ticket/11161
						 */
					} else {
						surveyCurrentLocale = '';
					}
					if(pieces[0].length==0 && surveyCurrentLocale!=''&&surveyCurrentLocale!=null) {
						if(pieces.length>2) {
							surveyCurrentPage = pieces[2];
							if(pieces.length>3){
								surveyCurrentId = pieces[3];
								if(surveyCurrentId.substr(0,2)=='x@') {
									surveyCurrentId=surveyCurrentId.substr(2);
								}
							} else {
								surveyCurrentId = '';
							}
						} else {
							surveyCurrentPage='';
							surveyCurrentId='';
						}
						window.surveyCurrentSpecial=null;
					} else {
						window.surveyCurrentSpecial = pieces[0];
						if(surveyCurrentSpecial=='') {
							surveyCurrentSpecial='locales';
						}
						if(surveyCurrentSpecial=='locales') {
							// allow locales list to retain ID / Page string for passthrough.
							surveyCurrentLocale='';
							if(pieces.length>2) {
								surveyCurrentPage = pieces[2];
								if(pieces.length>3){
									surveyCurrentId = pieces[3];
									if(surveyCurrentId.substr(0,2)=='x@') {
										surveyCurrentId=surveyCurrentId.substr(2);
									}
								} else {
									surveyCurrentId = '';
								}
							} else {
								surveyCurrentPage='';
								surveyCurrentId='';
							}
						} else if(isReport(surveyCurrentSpecial)) { // allow page and ID to fall through.
							if(pieces.length>2) {
								surveyCurrentPage = pieces[2];
								if(pieces.length>3){
									surveyCurrentId = pieces[3];
								} else {
									surveyCurrentId = '';
								}
							} else {
								surveyCurrentPage='';
								surveyCurrentId='';
							}
						} else {
							otherSpecial.parseHash(surveyCurrentSpecial, hash, pieces);
						}
					}
				} else {
					surveyCurrentLocale = '';
					surveyCurrentSpecial='locales';
					surveyCurrentId='';
					surveyCurrentPage='';
					surveyCurrentSection='';
				}
				updateWindowTitle();

				 // if there is no locale id, refresh the search.
				if(!surveyCurrentLocale) {
					searchRefresh();
				}
			};

			/**
			 * update hash (and title)
			 * @method replaceHash
			 * @param doPush {Boolean} if true, do a push (instead of replace)
			 */
			window.replaceHash = function replaceHash(doPush) {
				if(!doPush) doPush = false; // by default -replace.
				var theId = window.surveyCurrentId;
				if(theId == null) theId = '';
				var theSpecial = window.surveyCurrentSpecial;
				if(theSpecial == null) theSpecial = '';
				var thePage = window.surveyCurrentPage;
				if(thePage == null) thePage = '';
				var theLocale = window.surveyCurrentLocale;
				if(theLocale==null) theLocale = '';
				var newHash =  '#' + theSpecial + '/' + theLocale + '/' + thePage + '/' + theId;
				if(newHash != dojoHash()) {
					dojoHash(newHash , !doPush);
				}
			};

			window.updateCurrentId = function updateCurrentId(id) {
				if(id==null) id = '';
			    if(surveyCurrentId != id) { // don't set if already set.
				    surveyCurrentId = id;
				    replaceHash(false); // usually dont want to save
			    }
			};

			// (back to showV) some setup.
			// click on the title to copy (permalink)
			clickToSelect(dojo.byId("ariScroller"));
			updateIf("title-dcontent-link",stui.str("defaultContent_titleLink"));

			// TODO - rewrite using AMD
			/**
			 * @param postData optional - makes this a POST
			 */
			window.myLoad = function myLoad(url, message, handler, postData, headers) {
				var otime = new Date().getTime();
				console.log("MyLoad: " + url + " for " + message);
				var errorHandler = function(err, ioArgs){
					console.log('Error: ' + err + ' response ' + ioArgs.xhr.responseText);
					handleDisconnect("Could not fetch " + message + " - error " + err.name + " / " + err.message + "\n" + ioArgs.xhr.responseText + "\n url: " + url + "\n", null, "disconnect");
				};
				var loadHandler = function(json){
					console.log("        "+url+" loaded in "+(new Date().getTime()-otime)+"ms");
					try {
						handler(json);
						//resize height
						$('#main-row').css({height:$('#main-row>div').height()});
					}catch(e) {
						console.log("Error in ajax post ["+message+"]  " + e.message + " / " + e.name );
						handleDisconnect("Exception while  loading: " + message + " - "  + e.message + ", n="+e.name, null); // in case the 2nd line doesn't work
					}
				};
				var xhrArgs = {
						url: url,
						handleAs:"json",
						load: loadHandler,
						error: errorHandler,
						postData: postData,
						headers: headers
				};
				queueXhr(xhrArgs);
			};

			/**
			 * Verify that the JSON returned is as expected.
			 * @method verifyJson
			 * @param json the returned json
			 * @param subkey the key to look for,  json.subkey
			 * @return true if OK, false if bad
			 */
			function verifyJson(json, subkey) {
				if(!json) {
					console.log("!json");
					showLoader(null,"Error while  loading "+subkey+":  <br><div style='border: 1px solid red;'>" + "no data!" + "</div>");
					return false;
				} else if(json.err_code) {
					var msg_fmt = formatErrMsg(json, subkey);
					var loadingChunk;
					flipper.flipTo(pages.loading, loadingChunk = createChunk(msg_fmt, "p", "errCodeMsg"));
					var retryButton = createChunk(stui.str("loading_reload"),"button");
					loadingChunk.appendChild(retryButton);
					retryButton.onclick = function() { 	window.location.reload(true); };
					return false;
				} else if(json.err) {
					console.log("json.err!" + json.err);
					showLoader(null,"Error while  loading "+subkey+": <br><div style='border: 1px solid red;'>" + json.err + "</div>");
					handleDisconnect("while loading "+subkey+"" ,json);
					return false;
				} else if(!json[subkey]) {
					console.log("!json.oldvotes");
					showLoader(null,"Error while  loading "+subkey+": <br><div style='border: 1px solid red;'>" + "no data" + "</div>");
					handleDisconnect("while loading- no "+subkey+"",json);
					return false;
				} else {
					return true;
				}
			}

			window.showCurrentId = function() {
				if(surveyCurrentSpecial && surveyCurrentSpecial != '' && !isDashboard()) {
					otherSpecial.handleIdChanged(surveyCurrentSpecial, showCurrentId);
				} else {
					if(surveyCurrentId != '') {
					    var xtr = dojo.byId('r@' + surveyCurrentId);
					    if(!xtr) {
					        console.log("Warning could not load id " + surveyCurrentId + " does not exist");
					        window.updateCurrentId(null);
					    } else if(xtr.proposedcell && xtr.proposedcell.showFn) {
					        // TODO: visible? coverage?
					        window.showInPop("",xtr,xtr.proposedcell, xtr.proposedcell.showFn, true);
					        console.log("Changed to " + surveyCurrentId);
					        if(!isDashboard())
					        	scrollToItem();
					    } else {
					        console.log("Warning could not load id " + surveyCurrentId + " - not setup - " + xtr.toString() + " pc=" + xtr.proposedcell + " sf = " + xtr.proposedcell.showFn);
					    }
					}
				}
			};

			window.ariRetry = function() {
				ariDialog.hide();
				window.location.reload(true);
			};

			window.showARIDialog = function(why, json, word, oneword, p, what) {
				console.log('showARIDialog');
				p.parentNode.removeChild(p);

				if(didUnbust) {
					why = why + "\n\n" + stui.str('ari_force_reload');
				}

				// setup with why
				var ari_message;

				if(json && json.session_err) {
					ari_message = stui_str("ari_sessiondisconnect_message");
				} else {
					ari_message = stui.str('ari_message');
				}

				var ari_submessage = formatErrMsg(json, what);

				updateIf('ariMessage', ari_message.replace(/\n/g,"<br>"));
				updateIf('ariSubMessage', ari_submessage.replace(/\n/g,"<br>"));
				updateIf('ariScroller',window.location + '<br>' + why.replace(/\n/g,"<br>"));
				// TODO: update  ariMain and ariRetryBtn
				hideOverlayAndSidebar();

				ariDialog.show();
				var oneword = dojo.byId("progress_oneword");
				oneword.onclick = function() {
					if(disconnected) {
						ariDialog.show();
					}
				};
			};

			function updateCoverageMenuTitle() {
				if(surveyUserCov) {
					$('#coverage-info').text(stui.str('coverage_' + surveyUserCov));
				}
				else {
					$('#coverage-info').text(stui.sub('coverage_auto_msg', {surveyOrgCov: stui.str('coverage_' + surveyOrgCov)}));
				}
			}

			function updateLocaleMenu() {
	            if(surveyCurrentLocale!=null && surveyCurrentLocale!='' && surveyCurrentLocale!='-') {
	        		surveyCurrentLocaleName = locmap.getLocaleName( surveyCurrentLocale);
	        		var bund = locmap.getLocaleInfo(surveyCurrentLocale);
	        		if(bund) {
	        			if( bund.readonly) {
	        				addClass(menubuttons.getDom(menubuttons.locale), "locked");
	        			} else {
	            			removeClass(menubuttons.getDom(menubuttons.locale), "locked");
	        			}

	        			if(bund.dcChild) {
	        				menubuttons.set(menubuttons.dcontent, stui.sub("defaultContent_header_msg", {info: bund, locale: surveyCurrentLocale, dcChild: locmap.getLocaleName(bund.dcChild)}));
	        			} else {
	        				menubuttons.set(menubuttons.dcontent);
	        			}
	        		} else {
	        			removeClass(menubuttons.getDom(menubuttons.locale), "locked");
	    				menubuttons.set(menubuttons.dcontent);
	        		}
	            } else {
	            	surveyCurrentLocaleName = '';
	            	removeClass(menubuttons.getDom(menubuttons.locale), "locked");
					menubuttons.set(menubuttons.dcontent);
	            }
	            menubuttons.set(menubuttons.locale, surveyCurrentLocaleName);
			}

			/**
			 * Update the #hash and menus to the current settings.
			 * @method updateHashAndMenus
			 * @param doPush {Boolean} if false, do not add to history
			 */
			function updateHashAndMenus(doPush) {
				/**
				 * 'name' - the js/special/___.js name
				 * 'hidden' - true to hide the item
				 * 'title' - override of menu name
				 * @property specialItems
				 */
				var specialItems = new Array();
				if(surveyUser != null){
					specialItems = [
					    {divider: true},

						{title: 'Admin Panel', url: surveyUserURL.adminPanel, display: (surveyUser && surveyUser.userlevelName === 'ADMIN')},
						{divider: true, display: (surveyUser && surveyUser.userlevelName === 'ADMIN')},

					    {title: 'My Account'}, // My Account section

					    {title: 'Settings', level: 2, url: surveyUserURL.myAccountSetting, display: surveyUserPerms.userExist },
					    {title: 'Lock (Disable) My Account', level: 2, url: surveyUserURL.disableMyAccount, display: surveyUserPerms.userExist },

					    {divider: true},
					    {title: 'My Votes'}, // My Votes section

					    {special: 'oldvotes', level: 2, display: surveyUserPerms.userCanImportOldVotes },
					    {title: 'See My Recent Activity', level: 2, url: surveyUserURL.recentActivity },
					    {title: 'Upload XML', level: 2, url: surveyUserURL.xmlUpload },

					    {divider: true},
					    {title: 'My Organization('+organizationName+')'}, // My Organization section

					    {special: 'vsummary', level: 2, display: surveyUserPerms.userCanUseVettingSummary },
					    {title: 'List ' + org + ' Users', level: 2, url: surveyUserURL.manageUser, display: (surveyUserPerms.userIsTC || surveyUserPerms.userIsVetter) },
					    {title: 'LOCKED: Note: your account is currently locked.', level: 2,  display: surveyUserPerms.userIsLocked, bold: true},

					    {divider: true},
					    {title: 'Forum'}, // Forum section

					    {special: 'flagged', level: 2, img: surveyImgInfo.flag},
					    {title: 'RSS 2.0', level: 2, url: surveyUserURL.RSS, img: surveyImgInfo.RSS},
					    {special: 'mail', level: 2, display: !surveyOfficial },

					    {divider: true},
					    {title: 'Informational'}, // Informational section

					    {special: 'statistics', level: 2 },
					    {title: 'About', level: 2, url: surveyUserURL.about },
					    {title: 'Lookup a code or xpath', level: 2, url: surveyUserURL.browse, display: surveyUserPerms.hasDataSource },

					    {divider: true},
					];
				}
				if(!doPush) {doPush = false;}
				replaceHash(doPush); // update the hash
				updateLocaleMenu();

				if(surveyCurrentLocale==null) { // deadcode?
					menubuttons.set(menubuttons.section);
					if(surveyCurrentSpecial!=null) {
						var specialId = "special_"+surveyCurrentSpecial;
						menubuttons.set(menubuttons.page, stui_str(specialId));
					} else {
						menubuttons.set(menubuttons.page);
					}
					return; // nothing to do.
				}
				var titlePageContainer = dojo.byId("title-page-container");

				/**
				 * Just update the titles of the menus. Internal to updateHashAndMenus
				 * @method updateMenuTitles
				 */
				function updateMenuTitles(menuMap) {
					if(menubuttons.lastspecial === undefined) {
						menubuttons.lastspecial = null;

						// Set up the menu here?
						var parMenu = dojo.byId("manage-list");
						for(var k =0; k< specialItems.length; k++) {
							var item = specialItems[k];
							(function(item){
								if(item.display != false) {
									var subLi = document.createElement("li");
									if(item.special){ // special items so look up in stui.js
										item.title = stui.str('special_' + item.special);
										item.url = '#' + item.special;
										item.blank = false;
									}
									if(item.url){
										var subA = document.createElement("a");

										if(item.img){ // forum may need images attached to it
											var Img=document.createElement("img");
											Img.setAttribute('src', item.img.src);
											Img.setAttribute('alt', item.img.alt);
											Img.setAttribute('title', item.img.src);
											Img.setAttribute('border', item.img.border);

											subA.appendChild(Img);
										}
										subA.appendChild(document.createTextNode(item.title+' '));
										subA.href = item.url;

										if(item.blank != false){
											subA.target = '_blank';
											subA.appendChild(createChunk('','span','glyphicon glyphicon-share manage-list-icon'));
										}

										if(item.level){ // append it to appropriate levels
											var level = item.level;
											for(var i=0; i< level-1; i++){
												var sublevel = document.createElement("ul");
												sublevel.appendChild(subA);
												subA = sublevel;
											}
										}
										subLi.appendChild(subA);
									}
									if(!item.url && !item.divider){ // if it is pure text/html & not a divider
										if(!item.level){
											subLi.appendChild(document.createTextNode(item.title+' '));
										}else{
											var subA = null;
											if(item.bold){
												subA = document.createElement("b");
											}else if(item.italic){
												subA = document.createElement("i");
											}else{
												subA = document.createElement("span");
											}
											subA.appendChild(document.createTextNode(item.title+' '));

											var level = item.level;
											for(var i=0; i< level-1; i++){
												var sublevel = document.createElement("ul");
												sublevel.appendChild(subA);
												subA = sublevel;
											}
											subLi.appendChild(subA);
										}
									}
									if(item.divider) {
										subLi.className = 'nav-divider';
									}
									parMenu.appendChild(subLi);
								}
							})(item);
						}
					}

					if(menubuttons.lastspecial) {
						removeClass(menubuttons.lastspecial, "selected");
					}

					updateLocaleMenu(menuMap);
					if(surveyCurrentSpecial!= null && surveyCurrentSpecial != '') {
						var specialId = "special_"+surveyCurrentSpecial;
						$('#section-current').html(stui_str(specialId));
						setDisplayed(titlePageContainer, false);
					} else if(!menuMap) {
						setDisplayed(titlePageContainer, false);
					} else {
						if(menuMap.sectionMap[window.surveyCurrentPage]) {
							surveyCurrentSection = surveyCurrentPage; // section = page
							$('#section-current').html(menuMap.sectionMap[surveyCurrentSection].name);
							setDisplayed(titlePageContainer, false); // will fix title later
						} else if(menuMap.pageToSection[window.surveyCurrentPage]) {
							var mySection = menuMap.pageToSection[window.surveyCurrentPage];
							surveyCurrentSection = mySection.id;
							$('#section-current').html(mySection.name);
							setDisplayed(titlePageContainer, false); // will fix title later
						} else {
							$('#section-current').html(stui_str("section_general"));
							setDisplayed(titlePageContainer, false);
						}
					}
				}

				/**
				 * @method updateMenus
				 */
				function updateMenus(menuMap) {
					// initialize menus
					if(!menuMap.menusSetup) {
						menuMap.menusSetup=true;
						menuMap.setCheck = function(menu, checked,disabled) {
							menu.set('iconClass', checked ? "dijitMenuItemIcon menu-x" : "dijitMenuItemIcon menu-o");
							menu.set('disabled', disabled);
						};
						var menuSection = registry.byId("menu-section");
						menuMap.section_general = new MenuItem({
							label: stui_str("section_general"),
							iconClass:  "dijitMenuItemIcon ",
							disabled: true,
							onClick: function(){
								if(surveyCurrentPage!='' || (surveyCurrentSpecial!='' && surveyCurrentSpecial != null)) {
									surveyCurrentId = ''; // no id if jumping pages
									surveyCurrentPage = '';
									surveyCurrentSection = '';
									surveyCurrentSpecial = '';
									updateMenuTitles(menuMap);
									reloadV();
								}
							}
						});
						menuSection.addChild(menuMap.section_general);
						for(var j in menuMap.sections) {
							(function (aSection){
								aSection.menuItem = new MenuItem({
									label: aSection.name,
									iconClass: "dijitMenuItemIcon",
									onClick: function(){
											surveyCurrentId = '!'; // no id if jumping pages
											surveyCurrentPage = aSection.id;
											surveyCurrentSpecial = '';
											updateMenus(menuMap);
											updateMenuTitles(menuMap);
											reloadV();
									},
									disabled: true
								});

								menuSection.addChild(aSection.menuItem);
							})(menuMap.sections[j]);
						}

						menuSection.addChild(new MenuSeparator());

						menuMap.forumMenu = new MenuItem({
							label: stui_str("section_forum"),
							iconClass: "dijitMenuItemIcon", // menu-chat
							disabled: true,
							onClick: function(){
								surveyCurrentId = '!'; // no id if jumping pages
								surveyCurrentPage = '';
								surveyCurrentSpecial = 'forum';
								updateMenus(menuMap);
								updateMenuTitles(menuMap);
								reloadV();						}
						});
						menuSection.addChild(menuMap.forumMenu);
					}

					updateMenuTitles(menuMap);

					var myPage = null;
					var mySection = null;
					if(surveyCurrentSpecial==null || surveyCurrentSpecial=='') {
						// first, update display names
						if(menuMap.sectionMap[window.surveyCurrentPage]) { // page is really a section
							mySection = menuMap.sectionMap[surveyCurrentPage];
							myPage = null;
						} else if(menuMap.pageToSection[window.surveyCurrentPage]) {
							mySection = menuMap.pageToSection[surveyCurrentPage];
							myPage = mySection.pageMap[surveyCurrentPage];
						}
						if(mySection!==null) {
							// update menus under 'page' - peer pages
							if(!titlePageContainer.menus) {
								titlePageContainer.menus = {};
							}

							// hide all. TODO use a foreach model?
							for(var zz in titlePageContainer.menus) {
								var aMenu = titlePageContainer.menus[zz];
								aMenu.set('label','-');
							}

							var showMenu = titlePageContainer.menus[mySection.id];

							if(!showMenu) {
								// doesn't exist - add it.
								var menuPage = new DropDownMenu();
								for(var k in mySection.pages) { // use given order
									(function(aPage) {

										var pageMenu = aPage.menuItem =  new MenuItem({
											label: aPage.name,
											iconClass: (aPage.id == surveyCurrentPage) ? "dijitMenuItemIcon menu-x" : "dijitMenuItemIcon menu-o",
											onClick: function(){
												surveyCurrentId = ''; // no id if jumping pages
												surveyCurrentPage = aPage.id;
												updateMenuTitles(menuMap);
												reloadV();
											},
											disabled: (effectiveCoverage()<parseInt(aPage.levs[surveyCurrentLocale]))
										});
									})(mySection.pages[k]);
								}

								showMenu = new DropDownButton({label: '-', dropDown: menuPage});

								titlePageContainer.menus[mySection.id] = mySection.pagesMenu = showMenu;
							}

							if(myPage !== null) {
								$('#title-page-container').html('<h1>'+myPage.name+'</h1>').show();
							} else {
								$('#title-page-container').html('').hide();
							}
							setDisplayed(showMenu, true);
							setDisplayed(titlePageContainer, true); // will fix title later
						}
					}

					stdebug('Updating menus.. ecov = ' + effectiveCoverage());

					menuMap.setCheck(menuMap.section_general,  (surveyCurrentPage == '' && (surveyCurrentSpecial=='' || surveyCurrentSpecial==null)),false);

					// Update the status of the items in the Section menu
					for(var j in menuMap.sections) {
						var aSection = menuMap.sections[j];
						// need to see if any items are visible @ current coverage
						stdebug("for " + aSection.name + " minLev["+surveyCurrentLocale+"] = "+ aSection.minLev[surveyCurrentLocale]);
						menuMap.setCheck(aSection.menuItem,  (surveyCurrentSection == aSection.id),effectiveCoverage()<aSection.minLev[surveyCurrentLocale]);

						// update the items in that section's Page menu
						if(surveyCurrentSection == aSection.id) {
							for(var k in aSection.pages ) {
								var aPage = aSection.pages[k];
								if(!aPage.menuItem) {
									console.log("Odd - " + aPage.id + " has no menuItem");
								} else {
									menuMap.setCheck(aPage.menuItem,  (aPage.id == surveyCurrentPage),  (effectiveCoverage()<parseInt(aPage.levs[surveyCurrentLocale])));
								}
							}
						}
					}

					menuMap.setCheck(menuMap.forumMenu,  (surveyCurrentSpecial == 'forum'),(surveyUser	===null));
					resizeSidebar();
				}

				if(_thePages == null || _thePages.loc != surveyCurrentLocale ) {
					// show the raw IDs while loading.
					updateMenuTitles(null);

					if(surveyCurrentLocale!=null&&surveyCurrentLocale!='') {
						var needLocTable = false;

						var url = contextPath + "/SurveyAjax?_="+surveyCurrentLocale+"&s="+surveySessionId+"&what=menus&locmap="+needLocTable+cacheKill();
						myLoad(url, "menus", function(json) {
							if(!verifyJson(json, "menus")) {
								return; // busted?
							}

							if(json.locmap) {
								locmap = new LocaleMap(locmap); // overwrite with real data
							}

							// make this into a hashmap.
							if(json.canmodify) {
								var canmodify = {};
								for(var k in json.canmodify) {
									canmodify[json.canmodify[k]]=true;
								}
								window.canmodify = canmodify;
							}

							updateCovFromJson(json);
							updateCoverageMenuTitle();
							updateCoverage(flipper.get(pages.data)); // update CSS and auto menu title

							function unpackMenus(json) {
								var menus = json.menus;

								if(_thePages) {
									stdebug("Updating cov info into menus for " + json.loc);
									for(var k in menus.sections) {
										var oldSection = _thePages.sectionMap[menus.sections[k].id];
										for(var j in menus.sections[k].pages) {
											var oldPage = oldSection.pageMap[menus.sections[k].pages[j].id];

											// copy over levels
											oldPage.levs[json.loc] = menus.sections[k].pages[j].levs[json.loc];
										}
									}
								} else {
									stdebug("setting up new hashes for " + json.loc);
									// set up some hashes
									menus.haveLocs = {};
									menus.sectionMap = {};
									menus.pageToSection = {};
									for(var k in menus.sections) {
										menus.sectionMap[menus.sections[k].id] = menus.sections[k];
										menus.sections[k].pageMap = {};
										menus.sections[k].minLev = {};
										for(var j in menus.sections[k].pages) {
											menus.sections[k].pageMap[menus.sections[k].pages[j].id] = menus.sections[k].pages[j];
											menus.pageToSection[menus.sections[k].pages[j].id] = menus.sections[k];
										}
									}
									_thePages = menus;
								}

								stdebug("Calculating minimum section coverage for " + json.loc);
								for(var k in _thePages.sectionMap) {
									var min = 200;
									for(var j in _thePages.sectionMap[k].pageMap) {
										var thisLev = parseInt(_thePages.sectionMap[k].pageMap[j].levs[json.loc]);
										if(min > thisLev) {
											min = thisLev;
										}
									}
									_thePages.sectionMap[k].minLev[json.loc] = min;
								}

								_thePages.haveLocs[json.loc] = true;
							}

							unpackMenus(json);
							unpackMenuSideBar(json);
							updateMenus(_thePages);
						});
					}
				} else {
					// go ahead and update
					updateMenus(_thePages);
				}
			}

			window.insertLocaleSpecialNote = function insertLocaleSpecialNote(theDiv) {

				var bund = locmap.getLocaleInfo(surveyCurrentLocale);

				if(bund) {
					if(bund.readonly) {
						var msg = null;
						if(bund.readonly_why) {
							msg = bund.readonly_why_raw;
						} else {
							msg = stui.str("readonly_unknown");
						}
						var asHtml = stui.sub("readonly_msg", { info: bund, locale: surveyCurrentLocale, msg: msg});
						asHtml = locmap.linkify(asHtml);
						var theChunk = domConstruct.toDom(asHtml);
						var subDiv = document.createElement("div");
						subDiv.appendChild(theChunk);
						subDiv.className = 'warnText';
						theDiv.insertBefore(subDiv, theDiv.childNodes[0]);
					} else if(bund.dcChild) {
						var theChunk = domConstruct.toDom(stui.sub("defaultContentChild_msg", { info: bund, locale: surveyCurrentLocale, dcChildName: locmap.getLocaleName(bund.dcChild)}));
						var subDiv = document.createElement("div");
						subDiv.appendChild(theChunk);
						subDiv.className = 'warnText';
						theDiv.insertBefore(subDiv, theDiv.childNodes[0]);
					}
				}
				if (surveyBeta) {
					var theChunk = domConstruct.toDom(stui.sub("beta_msg", { info: bund, locale: surveyCurrentLocale, msg: msg}));
					var subDiv = document.createElement("div");
					subDiv.appendChild(theChunk);
					subDiv.className = 'warnText';
					theDiv.insertBefore(subDiv, theDiv.childNodes[0]);
				}
			};

			/**
			 * Show the "possible problems" section which has errors for the locale
			 * @method showPossibleProblems
			 */
			function showPossibleProblems(flipper,flipPage,loc, session, effectiveCov, requiredCov) {
				surveyCurrentLocale = loc;
				dojo.ready(function(){

					var url = contextPath + "/SurveyAjax?what=possibleProblems&_="+surveyCurrentLocale+"&s="+session+"&eff="+effectiveCov+"&req="+requiredCov+  cacheKill();
					myLoad(url, "possibleProblems", function(json) {
						if(verifyJson(json, 'possibleProblems')) {
							stdebug("json.possibleProblems OK..");
							if(json.dataLoadTime) {
								updateIf("dynload", json.dataLoadTime);
							}

							var theDiv = flipper.flipToEmpty(flipPage);

							insertLocaleSpecialNote(theDiv);

							if(json.possibleProblems.length > 0) {
								var subDiv = createChunk("","div");
								subDiv.className = "possibleProblems";

								var h3 = createChunk(stui_str("possibleProblems"), "h3");
								subDiv.appendChild(h3);

								var div3 = document.createElement("div");
								var newHtml = "";
								newHtml += testsToHtml(json.possibleProblems);
								div3.innerHTML = newHtml;
								subDiv.appendChild(div3);
								theDiv.appendChild(subDiv);
							}
							var theInfo = createChunk("","p","special_general");
							theDiv.appendChild(theInfo);
							theInfo.innerHTML = stui_str("special_general"); // TODO replace with … ?
							hideLoader(null);
						}
					});
				});
			}

			var isLoading = false;

			/**
			 * This is the main entrypoint to the 'new' view system, based in /v.jsp
			 * @method reloadV
			 */
			window.reloadV = function reloadV() {
				if(disconnected) {
					unbust();
				}

				document.getElementById('DynamicDataSection').innerHTML = '';//reset the data
				$('#nav-page').hide();
				$('#nav-page-footer').hide();
				isLoading = false;
				showers[flipper.get(pages.data).id]=function(){ console.log("reloadV()'s shower - ignoring reload request, we are in the middle of a load!"); };

				// assume parseHash was already called, if we are taking input from the hash
				ariDialog.hide();

				updateHashAndMenus(true);

				if(surveyCurrentLocale!=null && surveyCurrentLocale!=''&&surveyCurrentLocale!='-'){
					var bund = locmap.getLocaleInfo(surveyCurrentLocale);
					if(bund!==null && bund.dcParent) {
						var theChunk = domConstruct.toDom(stui.sub("defaultContent_msg", { info: bund, locale: surveyCurrentLocale, dcParentName: locmap.getLocaleName(bund.dcParent)}));
						var theDiv = document.createElement("div");
						theDiv.appendChild(theChunk);
						theDiv.className = 'ferrbox';
						flipper.flipTo(pages.other, theDiv);
						return;
					}
				}

				// todo dont even flip if it's quick.
				var loadingChunk;
				flipper.flipTo(pages.loading, loadingChunk = createChunk(stui_str("loading"), "i", "loadingMsg"));

				var itemLoadInfo = createChunk("","div","itemLoadInfo");

				// Create a little spinner to spin "..." so the user knows we are doing something..
				var spinChunk = createChunk("...","i","loadingMsgSpin");
				var spin = 0;
				var timerToKill = window.setInterval(function() {
					 var spinTxt = '';
					 spin++;
					 switch(spin%3) {
						 case 0: spinTxt = '.  '; break;
						 case 1: spinTxt = ' . '; break;
						 case 2: spinTxt = '  .'; break;
					 }
					 removeAllChildNodes(spinChunk);
					 spinChunk.appendChild(document.createTextNode(spinTxt));
				}, 1000);

				// Add the "..." until the Flipper flips
				flipper.addUntilFlipped(function() {
					var frag = document.createDocumentFragment();
					frag.appendChild(spinChunk);
					return frag;
				}, function() {
					window.clearInterval(timerToKill);
				});

				// now, load. Use a show-er function for indirection.
				var shower = function() {
					if(isLoading) {
						console.log("reloadV inner shower: already isLoading, exitting.");
						return;
					}
					isLoading = true;
					var theDiv = flipper.get(pages.data);
					var theTable = theDiv.theTable;

					if(!theTable) {
						var theTableList = theDiv.getElementsByTagName("table");
						if(theTableList) {
							theTable = theTableList[0];
							theDiv.theTable = theTable;
						}
					}

					showLoader(null, theDiv.stui.loading);

					if((surveyCurrentSpecial == null||surveyCurrentSpecial=='') && surveyCurrentLocale!=null && surveyCurrentLocale!='') {
						if((surveyCurrentPage==null || surveyCurrentPage=='') && (surveyCurrentId==null||surveyCurrentId=='')) {
							// the 'General Info' page.
							itemLoadInfo.appendChild(document.createTextNode(locmap.getLocaleName(surveyCurrentLocale)));
							showPossibleProblems(flipper, pages.other, surveyCurrentLocale, surveySessionId, covName(effectiveCoverage()), covName(effectiveCoverage()));
							showInPop2(stui.str("generalPageInitialGuidance"), null, null, null, true); /* show the box the first time */
							isLoading=false;
						} else if(surveyCurrentId=='!') {
							var frag = document.createDocumentFragment();
							frag.appendChild(createChunk(stui.str('section_help'),"p", "helpContent"));
							var infoHtml = stui.str('section_info_'+surveyCurrentPage);
							var infoChunk  = document.createElement("div");
							infoChunk.innerHTML = infoHtml;
							frag.appendChild(infoChunk);
							flipper.flipTo(pages.other, frag);
							hideLoader(null);
							isLoading=false;

						} else {
							// (common case) this is an actual locale data page.
							itemLoadInfo.appendChild(document.createTextNode(locmap.getLocaleName(surveyCurrentLocale) + '/' + surveyCurrentPage + '/' + surveyCurrentId));
							var url = contextPath + "/RefreshRow.jsp?json=t&_="+surveyCurrentLocale+"&s="+surveySessionId+"&x="+surveyCurrentPage+"&strid="+surveyCurrentId+cacheKill();
							$('#nav-page').show(); // make top "Prev/Next" buttons visible while loading, cf. '#nav-page-footer' below
							myLoad(url, "section", function(json) {
								isLoading=false;
								showLoader(theDiv.loader,stui.loading2);
								if(!verifyJson(json, 'section')) {
									return;
								} else if(json.section.nocontent) {
									surveyCurrentSection = '';
									if(json.pageId) {
										surveyCurrentPage = json.pageId;
									} else {
										surveyCurrentPage= '';
									}
									showLoader(null);
									updateHashAndMenus(); // find out why there's no content. (locmap)
								} else if(!json.section.rows) {
									console.log("!json.section.rows");
									showLoader(theDiv.loader,"Error while  loading: <br><div style='border: 1px solid red;'>" + "no rows" + "</div>");
									handleDisconnect("while loading- no rows",json);
								} else {
									stdebug("json.section.rows OK..");
									showLoader(theDiv.loader, "loading..");
									if(json.dataLoadTime) {
										updateIf("dynload", json.dataLoadTime);
									}

									surveyCurrentSection = '';
									surveyCurrentPage = json.pageId;
									updateHashAndMenus(); // now that we have a pageid
									if(!surveyUser) {
										showInPop2(stui.str("loginGuidance"), null, null, null, true); /* show the box the first time */
									} else if(!json.canModify) {
										showInPop2(stui.str("readonlyGuidance"), null, null, null, true); /* show the box the first time */
									} else {
										showInPop2(stui.str("dataPageInitialGuidance"), null, null, null, true); /* show the box the first time */
									}
									doUpdate(theDiv.id, function() {
										showLoader(theDiv.loader,stui.loading3);
										insertRows(theDiv,json.pageId,surveySessionId,json); // pageid is the xpath..
										updateCoverage(flipper.get(pages.data)); // make sure cov is set right before we show.
										flipper.flipTo(pages.data); // TODO now? or later?
										window.showCurrentId(); // already calls scroll
										refreshCounterVetting();
										$('#nav-page-footer').show(); // make bottom "Prev/Next" buttons visible after building table
									});
								}
							});
						}
					} else if(surveyCurrentSpecial =='oldvotes') {
						var url = contextPath + "/SurveyAjax?what=oldvotes&_="+surveyCurrentLocale+"&s="+surveySessionId+"&"+cacheKill();
						myLoad(url, "(loading oldvotes " + surveyCurrentLocale + ")", function(json) {
							isLoading=false;
							showLoader(null,stui.loading2);
							if(!verifyJson(json, 'oldvotes')) {
								return;
							} else {
								showLoader(null, "loading..");
								if(json.dataLoadTime) {
									updateIf("dynload", json.dataLoadTime);
								}

								var theDiv = flipper.flipToEmpty(pages.other); // clean slate, and proceed..

								removeAllChildNodes(theDiv);

								var h2txt = stui.str("v_oldvotes_title");
								theDiv.appendChild(createChunk(h2txt, "h2", "v-title"));

								if(!json.oldvotes.locale) {
									surveyCurrentLocale='';
									updateHashAndMenus();

									var ul = document.createElement("div");
									ul.className = "oldvotes_list";
									var data = json.oldvotes.locales.data;
									var header = json.oldvotes.locales.header;

									if(data.length > 0) {
										data.sort((a, b) => a[header.LOCALE].localeCompare(b[header.LOCALE]));
										for(var k in data) {
											var li = document.createElement("li");

											var link = createChunk(data[k][header.LOCALE_NAME],"a");
											link.href = "#"+data[k][header.LOCALE];
											(function(loc,link) {
												return (function() {
													var clicky;
												listenFor(link, "click", clicky = function(e) {
													surveyCurrentLocale = loc;
													reloadV();
													stStopPropagation(e);
													return false;
												});
												link.onclick = clicky;
												}); })(data[k][header.LOCALE],link)();
											li.appendChild(link);
											li.appendChild(createChunk(" "));
											li.appendChild(createChunk("("+data[k][header.COUNT]+")"));

											ul.appendChild(li);
										}

										theDiv.appendChild(ul);

										theDiv.appendChild(createChunk(stui.str("v_oldvotes_locale_list_help_msg"), "p", "helpContent"));
									} else {
										theDiv.appendChild(createChunk(stui.str("v_oldvotes_no_old"),"i")); // TODO fix
									}
								} else {
									surveyCurrentLocale=json.oldvotes.locale;
									updateHashAndMenus();
									var loclink;
									theDiv.appendChild(loclink=createChunk(stui.str("v_oldvotes_return_to_locale_list"),"a", "notselected"));
									listenFor(loclink, "click", function(e) {
										surveyCurrentLocale='';
										reloadV();
										stStopPropagation(e);
										return false;
									});
									theDiv.appendChild(createChunk(json.oldvotes.localeDisplayName, "h3","v-title2"));
									var oldVotesLocaleMsg = document.createElement("p");
									oldVotesLocaleMsg.className = "helpContent";
									oldVotesLocaleMsg.innerHTML = stui.sub("v_oldvotes_locale_msg", {version: surveyLastVoteVersion, locale: json.oldvotes.localeDisplayName});
									theDiv.appendChild(oldVotesLocaleMsg);
									if ((json.oldvotes.contested && json.oldvotes.contested.length > 0) || (json.oldvotes.uncontested && json.oldvotes.uncontested.length > 0)) {
										var frag = document.createDocumentFragment();
										const oldVoteCount = (json.oldvotes.contested ? json.oldvotes.contested.length : 0) +
										                     (json.oldvotes.uncontested ? json.oldvotes.uncontested.length : 0);
										var summaryMsg = stui.sub("v_oldvotes_count_msg", {count: oldVoteCount});
										frag.appendChild(createChunk(summaryMsg, "div", ""));

										var navChunk = document.createElement("div");
										navChunk.className = 'v-oldVotes-nav';
										frag.appendChild(navChunk);

										var uncontestedChunk = null;
										var contestedChunk = null;

										function addOldvotesType(type, jsondata, frag, navChunk) {
											var content = createChunk("", "div", "v-oldVotes-subDiv");
											content.strid = "v_oldvotes_title_" + type; // v_oldvotes_title_contested or v_oldvotes_title_uncontested

											/* Normally this interface is for old "losing" (contested) votes only, since old "winning" (uncontested) votes
											 * are imported automatically. An exception is for TC users, for whom auto-import is disabled. The server-side
											 * code leaves json.oldvotes.uncontested undefined except for TC users.
											 * Show headings for "Winning/Losing" only if json.oldvotes.uncontested is defined and non-empty.
											 */
											if ((json.oldvotes.uncontested && json.oldvotes.uncontested.length > 0)) {
												var title = stui.str(content.strid);
												content.title = title;
												content.appendChild(createChunk(title,"h2","v-oldvotes-sub"));
											}

											content.appendChild(showVoteTable(jsondata, type, json.BASELINE_LANGUAGE_NAME, json.oldvotes.dir));

											var submit = BusyButton({
												label: stui.str("v_submit_msg"),
												busyLabel: stui.str("v_submit_busy")
											});

											submit.on("click",function(e) {
												setDisplayed(navChunk, false);
												var confirmList= []; // these will be revoted with current params

												// explicit confirm list -  save us desync hassle
												for(var kk in jsondata ) {
													if(jsondata[kk].box.checked) {
														confirmList.push(jsondata[kk].strid);
													}
												}

												var saveList = {
														locale: surveyCurrentLocale,
														confirmList: confirmList,
												};

												console.log(saveList.toString());
												console.log("Submitting " + type + " " + confirmList.length + " for confirm");

												var url = contextPath + "/SurveyAjax?what=oldvotes&_="+surveyCurrentLocale+"&s="+surveySessionId+"&doSubmit=true&"+cacheKill();
												myLoad(url, "(submitting oldvotes " + surveyCurrentLocale + ")", function(json) {
													showLoader(theDiv.loader,stui.loading2);
													if(!verifyJson(json, 'oldvotes')) {
														handleDisconnect("Error submitting votes!", json, "Error");
														return;
													} else {
														reloadV();
													}
												},  JSON.stringify(saveList), { "Content-Type": "application/json"} );
											});

											submit.placeAt(content);
											// hide by default
											setDisplayed(content, false);

											frag.appendChild(content);
											return content;
										}

										if (json.oldvotes.uncontested && json.oldvotes.uncontested.length > 0){
											uncontestedChunk = addOldvotesType("uncontested",json.oldvotes.uncontested, frag, navChunk);
										}
										if (json.oldvotes.contested && json.oldvotes.contested.length > 0){
											contestedChunk = addOldvotesType("contested",json.oldvotes.contested, frag, navChunk);
										}

										if(contestedChunk==null && uncontestedChunk != null) {
											setDisplayed(uncontestedChunk, true); // only item
										} else if(contestedChunk!=null && uncontestedChunk == null) {
											setDisplayed(contestedChunk, true); // only item
										} else {
											// navigation
											navChunk.appendChild(createChunk(stui.str('v_oldvotes_show')));
											navChunk.appendChild(createLinkToFn(uncontestedChunk.strid, function() {
												setDisplayed(contestedChunk, false);
												setDisplayed(uncontestedChunk, true);
											}, 'button'));
											navChunk.appendChild(createLinkToFn(contestedChunk.strid, function() {
												setDisplayed(contestedChunk, true);
												setDisplayed(uncontestedChunk, false);
											}, 'button'));

											contestedChunk.appendChild(createLinkToFn("v_oldvotes_hide", function() {
												setDisplayed(contestedChunk, false);
											}, 'button'));
											uncontestedChunk.appendChild(createLinkToFn("v_oldvotes_hide", function() {
												setDisplayed(uncontestedChunk, false);
											}, 'button'));

										}

										theDiv.appendChild(frag);
									} else {
										theDiv.appendChild(createChunk(stui.str("v_oldvotes_no_old_here"),"i",""));
									}
								}
							}
							hideLoader(null);
						});
					} else if(surveyCurrentSpecial == 'mail') {
						var url = contextPath + "/SurveyAjax?what=mail&s="+surveySessionId+"&fetchAll=true&"+cacheKill();
						myLoad(url, "(loading mail " + surveyCurrentLocale + ")", function(json) {
							hideLoader(null,stui.loading2);
							isLoading=false;
							if(!verifyJson(json, 'mail')) {
								return;
							} else {
								if(json.dataLoadTime) {
									updateIf("dynload", json.dataLoadTime);
								}

								var theDiv = flipper.flipToEmpty(pages.other); // clean slate, and proceed..

								removeAllChildNodes(theDiv);

								var listDiv = createChunk("","div","mailListChunk");
								var contentDiv = createChunk("","div","mailContentChunk");

								theDiv.appendChild(listDiv);
								theDiv.appendChild(contentDiv);

								setDisplayed(contentDiv,false);
								var header = json.mail.header;
								var data = json.mail.data;

								if(data.length == 0) {
									listDiv.appendChild(createChunk(stui.str("mail_noMail"),"p","helpContent"));
								} else {
									for(var ii in data) {
										var row = data[ii];
										var li = createChunk(row[header.QUEUE_DATE] + ": " + row[header.SUBJECT], "li", "mailRow");
										if(row[header.READ_DATE]) {
											addClass(li,"readMail");
										}
										if(header.USER !== undefined) {
											li.appendChild(document.createTextNode("(to "+row[header.USER]+")"));
										}
										if(row[header.SENT_DATE] !== false) {
											li.appendChild(createChunk("(sent)", "span", "winner"));
										} else  if(row[header.TRY_COUNT]>=3) {
											li.appendChild(createChunk("(try#"+row[header.TRY_COUNT]+")", "span", "loser"));
										} else  if(row[header.TRY_COUNT]> 0) {
											li.appendChild(createChunk("(try#"+row[header.TRY_COUNT]+")", "span", "warning"));
										}
										listDiv.appendChild(li);

										li.onclick = (function(li,row,header) {
											return function() {
											 	  if(!row[header.READ_DATE])
													myLoad(contextPath + "/SurveyAjax?what=mail&s="+surveySessionId+"&markRead="+row[header.ID]+"&"+cacheKill(), 'Marking mail read', function(json) {
														if(!verifyJson(json, 'mail')) {
															return;
														} else {
															addClass(li, "readMail"); // mark as read when server answers
															row[header.READ_DATE]=true; // close enough
														}
													});

											 	  setDisplayed(contentDiv, false);

											 	  removeAllChildNodes(contentDiv);

											 	  contentDiv.appendChild(createChunk("Date: " + row[header.QUEUE_DATE], "h2", "mailHeader"));
											 	  contentDiv.appendChild(createChunk("Subject: " + row[header.SUBJECT], "h2", "mailHeader"));
											 	  contentDiv.appendChild(createChunk("Message-ID: " + row[header.ID], "h2", "mailHeader"));
											 	  if(header.USER !== undefined) {
												 	  contentDiv.appendChild(createChunk("To: " + row[header.USER], "h2", "mailHeader"));
												  }
											 	  contentDiv.appendChild(createChunk(row[header.TEXT], "p", "mailContent"));

											 	  setDisplayed(contentDiv, true);
												};
											})(li, row, header);
									}
								}

							}
						});
					} else if(isReport(surveyCurrentSpecial)) {
						showLoader(theDiv.loader);
						showInPop2(stui.str("reportGuidance"), null, null, null, true, true); /* show the box the first time */
						require([
						         "dojo/ready",
						         "dojo/dom",
						         "dojo/dom-construct",
						         "dojo/request",
						         "dojo/number",
						         "dojo/domReady!"
						         ],
						         // HANDLES
						         function(
						        		 ready,
						        		 dom,
						        		 dcons,
						        		 request,
						        		 dojoNumber
						        ) { ready(function(){

									var url = contextPath + "/EmbeddedReport.jsp?x="+surveyCurrentSpecial+"&_="+surveyCurrentLocale+"&s="+surveySessionId+cacheKill();
									var errFunction = function errFunction(err) {
										console.log("Error: loading " + url + " -> " + err);
										hideLoader(null,stui.loading2);
										isLoading=false;
										flipper.flipTo(pages.other, domConstruct.toDom("<div style='padding-top: 4em; font-size: x-large !important;' class='ferrorbox warning'><span class='icon i-stop'> &nbsp; &nbsp;</span>Error: could not load: " + err + "</div>"));
									};
									if(isDashboard()) {
										if(!isVisitor) {
											request
							    			.get(url, {handleAs: 'json'})
							    			.then(function(json) {
												hideLoader(null,stui.loading2);
												isLoading=false;
												// further errors are handled in JSON
												showReviewPage(json, function() {
													// show function - flip to the 'other' page.
													flipper.flipTo(pages.other, null);
												});
											})
											.otherwise(errFunction);
										}
										else {
											alert('Please login to access Dashboard');
											surveyCurrentSpecial = '';
											surveyCurrentLocale = '';
											reloadV();
										}
									}
									else {
										hideLoader(null,stui.loading2);

										request
						    			.get(url, {handleAs: 'html'})
						    			.then(function(html) {
						    				// errors are handled as HTML.
											hideLoader(null,stui.loading2);
											isLoading=false;
											flipper.flipTo(pages.other, domConstruct.toDom(html));
										})
										.otherwise(errFunction);
									}

						        });
						 });
					} else if(surveyCurrentSpecial == 'none') {
						// for now - redirect
						hideLoader(null);
						isLoading=false;
						window.location = survURL; // redirect home
					} else if(surveyCurrentSpecial == 'locales') {
						hideLoader(null);
						isLoading=false;
						var theDiv = document.createElement("div");
						theDiv.className = 'localeList';

						var addSubLocale = function addSubLocale(parLocDiv, subLoc) {
							var subLocInfo = locmap.getLocaleInfo(subLoc);
							var subLocDiv = createChunk(null, "div", "subLocale");
							appendLocaleLink(subLocDiv, subLoc, subLocInfo);

							parLocDiv.appendChild(subLocDiv);
						};

						var addSubLocales = function addSubLocales(parLocDiv, subLocInfo) {
							if(subLocInfo.sub) {
								for(var n in subLocInfo.sub) {
									var subLoc = subLocInfo.sub[n];
									addSubLocale(parLocDiv, subLoc);
								}
							}
						};

						/*
						 * TODO: there are two functions named addTopLocale, clarify why
						 */
						var addTopLocale = function addTopLocale(topLoc) {
							var topLocInfo = locmap.getLocaleInfo(topLoc);

							var topLocRow = document.createElement("div");
							topLocRow.className="topLocaleRow";

							var topLocDiv = document.createElement("div");
							topLocDiv.className="topLocale";
							appendLocaleLink(topLocDiv, topLoc, topLocInfo);

							var topLocList = document.createElement("div");
							topLocList.className="subLocaleList";

							addSubLocales(topLocList, topLocInfo);

							topLocRow.appendChild(topLocDiv);
							topLocRow.appendChild(topLocList);
							theDiv.appendChild(topLocRow);
						};

						addTopLocale("root");
						// top locales
						for(var n in locmap.locmap.topLocales) {
							var topLoc = locmap.locmap.topLocales[n];
							addTopLocale(topLoc);
						}
						flipper.flipTo(pages.other,null);
					    filterAllLocale();//filter for init data
						forceSidebar();
						surveyCurrentLocale=null;
						surveyCurrentSpecial='locales';
						showInPop2(stui.str("localesInitialGuidance"), null, null, null, true); /* show the box the first time */
						$('#itemInfo').html('');
					} else {
						otherSpecial.show(surveyCurrentSpecial, {flipper: flipper, pages: pages});
					}
				}; // end shower

				shower(); // first load

				// set up the "show-er" function so that if this locale gets reloaded, the page will load again - execept for the dashboard, where only the row get updated
				if(!isDashboard()) {
					showers[flipper.get(pages.data).id]=shower;
				}
			};  // end reloadV

			function trimNull(x) {
				if(x==null) {
					return '';
				}
				try {
					x = x.toString().trim();
				} catch(e) {
					// do nothing
				}
				return x;
			}

			ready(function(){
				window.parseHash(dojoHash()); // get the initial settings
				// load the menus - first.

				var theLocale = surveyCurrentLocale;
				if(surveyCurrentLocale===null || surveyCurrentLocale=='') {
					theLocale = 'und';
				}
				var xurl = contextPath + "/SurveyAjax?_="+theLocale+"&s="+surveySessionId+"&what=menus&locmap="+true+cacheKill();
				myLoad(xurl, "initial menus for " + surveyCurrentLocale, function(json) {
					if(!verifyJson(json,'locmap')) {
						return;
					} else {
						locmap = new LocaleMap(json.locmap);
						if (surveyCurrentLocale === "USER" && json.loc) {
							surveyCurrentLocale = json.loc; // reference: https://unicode.org/cldr/trac/ticket/11161
						}
						// make this into a hashmap.
						if(json.canmodify) {
							var canmodify = {};
							for(var k in json.canmodify) {
								canmodify[json.canmodify[k]]=true;
							}
							window.canmodify = canmodify;
						}

						//update left sidebar with locale data
						var theDiv = document.createElement("div");
						theDiv.className = 'localeList';

						var addSubLocale;

						addSubLocale = function addSubLocale(parLocDiv, subLoc) {
							var subLocInfo = locmap.getLocaleInfo(subLoc);
							var subLocDiv = createChunk(null, "div", "subLocale");
							appendLocaleLink(subLocDiv, subLoc, subLocInfo);

							parLocDiv.appendChild(subLocDiv);
						};

						var addSubLocales = function addSubLocales(parLocDiv, subLocInfo) {
							if(subLocInfo.sub) {
								for(var n in subLocInfo.sub) {
									var subLoc = subLocInfo.sub[n];
									addSubLocale(parLocDiv, subLoc);
								}
							}
						};

						var addTopLocale = function addTopLocale(topLoc) {
							var topLocInfo = locmap.getLocaleInfo(topLoc);

							var topLocRow = document.createElement("div");
							topLocRow.className="topLocaleRow";

							var topLocDiv = document.createElement("div");
							topLocDiv.className="topLocale";
							appendLocaleLink(topLocDiv, topLoc, topLocInfo);

							var topLocList = document.createElement("div");
							topLocList.className="subLocaleList";

							addSubLocales(topLocList, topLocInfo);

							topLocRow.appendChild(topLocDiv);
							topLocRow.appendChild(topLocList);
							theDiv.appendChild(topLocRow);
						};


						addTopLocale("root");
						// top locales
						for(var n in locmap.locmap.topLocales) {
							var topLoc = locmap.locmap.topLocales[n];
							addTopLocale(topLoc);
						}
						$('#locale-list').html(theDiv.innerHTML);

						if(isVisitor)
							$('#show-read').prop('checked', true);
						//tooltip locale
						$('a.locName').tooltip();

						filterAllLocale();
						//end of adding the locale data

						updateCovFromJson(json);
						// setup coverage level
						window.surveyLevels = json.menus.levels;

						var titleCoverage = dojo.byId("title-coverage"); // coverage label

						var levelNums = [];  // numeric levels
						for(var k in window.surveyLevels) {
							levelNums.push( { num: parseInt(window.surveyLevels[k].level), level: window.surveyLevels[k] } );
						}
						levelNums.sort(function(a,b){return a.num-b.num;});

						var store = [];

						store.push({
								label: 'Auto',
								value: 'auto',
								title: stui.str('coverage_auto_desc')
							});

						store.push({
							type: "separator"
						});

						for(var j in levelNums) { // use given order
							if(levelNums[j].num==0) continue; // none - skip
							if(levelNums[j].num < covValue('minimal')) continue; // don't bother showing these
							if(window.surveyOfficial && levelNums[j].num==101) continue; // hide Optional in production
							var level = levelNums[j].level;
							store.push({
									label: stui.str('coverage_'+ level.name),
									value: level.name,
									title: stui.str('coverage_'+ level.name + '_desc')
							});
						}
						//coverage menu
						var patternCoverage = $('#title-coverage .dropdown-menu');
					    if(store[0].value) {
						    $('#coverage-info').text(store[0].label);
					    }
						for (var index = 0; index < store.length; ++index) {
						    var data = store[index];
						    if(data.value) {
							    var html = '<li><a class="coverage-list" data-value="'+data.value+'"href="#">'+data.label+'</a></li>';
							    patternCoverage.append(html);
						    }
						}
						patternCoverage.find('li a').click(function(event){
							event.stopPropagation();
							event.preventDefault();
							var newValue = $(this).data('value');
							var setUserCovTo = null;
							if(newValue == 'auto') {
								setUserCovTo = null; // auto
							} else {
								setUserCovTo = newValue;
							}
							if(setUserCovTo === window.surveyUserCov) {
								console.log('No change in user cov: ' + setUserCovTo);
							} else {
								window.surveyUserCov = setUserCovTo;
								var updurl  = contextPath + "/SurveyAjax?_="+theLocale+"&s="+surveySessionId+"&what=pref&pref=p_covlev&_v="+window.surveyUserCov+cacheKill(); // SurveyMain.PREF_COVLEV
								myLoad(updurl, "updating covlev to  " + surveyUserCov, function(json) {
									if(!verifyJson(json,'pref')) {
										return;
									} else {
										unpackMenuSideBar(json);
										if(surveyCurrentSpecial && isReport(surveyCurrentSpecial))
											reloadV();
										console.log('Server set  covlev successfully.');
									}
								});
							}
							// still update these.
							updateCoverage(flipper.get(pages.data)); // update CSS and 'auto' menu title
							updateHashAndMenus(false); // TODO: why? Maybe to show an item?
							$('#coverage-info').text(newValue.ucFirst());
							$(this).parents('.dropdown-menu').dropdown('toggle');
							if(!isDashboard())
								refreshCounterVetting();
							return false;
						});
						// TODO have to move this out of the DOM.. (Move WHAT out of the DOM?)

						/**
						 * Automatically import old winning votes
						 *
						 * Requires Dialog; we already have "dijit/Dialog" in require() statement at start of showV
						 */
						function doAutoImport() {
							'use strict';
							var autoImportProgressDialog = new Dialog({
								title: stui.str("v_oldvote_auto_msg"),
								content: stui.str("v_oldvote_auto_progress_msg")
							});
							autoImportProgressDialog.show();
							window.haveDialog = true;
							hideOverlayAndSidebar();
							/*
							 * See WHAT_AUTO_IMPORT = "auto_import" in SurveyAjax.java
							 */
							var url = contextPath + "/SurveyAjax?s=" + surveySessionId + "&what=auto_import" + cacheKill();
							myLoad(url, "auto-importing votes", function(json) {
								autoImportProgressDialog.hide();
								window.haveDialog = false;
								if (json.autoImportedOldWinningVotes) {
									var vals = {
										count: dojoNumber.format(json.autoImportedOldWinningVotes)
									};
									var autoImportedDialog = new Dialog({
										title: stui.str("v_oldvote_auto_msg"),
										content: stui.sub("v_oldvote_auto_desc_msg", vals)
									});
									autoImportedDialog.addChild(new Button({
										label: "OK",
										onClick: function() {
											window.haveDialog = false;
											autoImportedDialog.hide();
											reloadV();
										}
									}));
									autoImportedDialog.show();
									window.haveDialog = true;
									hideOverlayAndSidebar();
								}
							});
						}

						if (json.canAutoImport) {
							doAutoImport();
						}

						window.reloadV(); // call it

						// watch for hashchange to make other changes..
						dojoTopic.subscribe("/dojo/hashchange", function(changedHash){
							var oldLocale = trimNull(surveyCurrentLocale);
							var oldSpecial = trimNull(surveyCurrentSpecial);
							var oldPage = trimNull(surveyCurrentPage);
							var oldId = trimNull(surveyCurrentId);

							window.parseHash(changedHash);

							surveyCurrentId = trimNull(surveyCurrentId);

							// did anything change?
							if(oldLocale!=trimNull(surveyCurrentLocale) ||
									oldSpecial!=trimNull(surveyCurrentSpecial) ||
									oldPage != trimNull(surveyCurrentPage) ) {
								console.log("# hash changed, (loc, etc) reloadingV..");
								reloadV();
							} else if(oldId != surveyCurrentId && surveyCurrentId != '') {
								console.log("# just ID changed, to " + surveyCurrentId);
							    // surveyCurrentID and the hash have already changed.
							    // just call showInPop if the item is present. If not present, make sure it's visible.
								window.showCurrentId();
							}
						});
					}
				}); // end myLoad
			}); // end ready
		}); // end loadStui
	}); // end require
} // end showV

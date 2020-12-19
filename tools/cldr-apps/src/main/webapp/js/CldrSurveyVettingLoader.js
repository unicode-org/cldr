/*
 * CldrSurveyVettingLoader.js - split off from survey.js, for CLDR Survey Tool
 * showV() and reloadV() and related functions
 */

/*
 * haveDialog: when true, it means a "dialog" of some kind is displayed.
 * Used for inhibiting $('#left-sidebar').hover in redesign.js.
 * Currently there are only two such dialogs, both for auto-import.
 *
 * TODO: make it a property of one of our own objects instead of "window".
 */
window.haveDialog = false;

/**
 * Call this once in the page. It expects to find a node #DynamicDataSection
 */
function showV() {
	// REQUIRES
	require([
		"dijit/DropDownMenu",
		"dijit/form/DropDownButton",
		"dijit/MenuSeparator",
		"dijit/MenuItem",
		"dijit/form/Button",
		"dijit/Dialog",
		"dijit/registry",
		"dojox/form/BusyButton",
		"dojo/hash",
		"dojo/topic",
	],
		// HANDLES
		function(
			dijitDropDownMenu,
			dijitDropDownButton,
			dijitMenuSeparator,
			dijitMenuItem,
			dijitButton,
			dijitDialog,
			dijitRegistry,
			dojoxBusyButton,
			dojoHash,
			dojoTopic,
		) {
				var appendLocaleLink = function appendLocaleLink(subLocDiv, subLoc, subInfo, fullTitle) {
					var name = locmap.getRegionAndOrVariantName(subLoc);
					if (fullTitle) {
						name = locmap.getLocaleName(subLoc);
					}
					var clickyLink = createChunk(name, "a", "locName");
					clickyLink.href = linkToLocale(subLoc);
					subLocDiv.appendChild(clickyLink);
					if (subInfo == null) {
						console.log("* internal: subInfo is null for " + name + " / " + subLoc);
					}
					if (subInfo.name_var) {
						addClass(clickyLink, "name_var");
					}
					clickyLink.title = subLoc; // remove auto generated "locName.title"

					if (subInfo.readonly) {
						addClass(clickyLink, "locked");
						addClass(subLocDiv, "hide");

						if (subInfo.special_comment) {
							clickyLink.title = subInfo.special_comment;
						} else if (subInfo.dcChild) {
							clickyLink.title = cldrText.sub("defaultContentChild_msg", {
								name: subInfo.name,
								dcChild: subInfo.dcChild,
								dcChildName: locmap.getLocaleName(subInfo.dcChild)
							});
						} else {
							clickyLink.title = cldrText.get("readonlyGuidance");
						}
					} else if (subInfo.special_comment) {
						// could be the sandbox locale, or some other comment.
						clickyLink.title = subInfo.special_comment;
					}

					if (window.canmodify && subLoc in window.canmodify) {
						addClass(clickyLink, "canmodify");
					} else {
						addClass(subLocDiv, "hide"); // not modifiable
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
				var flipper = new Flipper([pages.loading, pages.data, pages.other]);

				var pucontent = document.getElementById("itemInfo");
				var theDiv = flipper.get(pages.data);
				theDiv.pucontent = pucontent;

				pucontent.appendChild(createChunk(cldrText.get("itemInfoBlank"), "i"));

				/**
				 * List of buttons/titles to set.
				 * @property menubuttons
				 */
				var menubuttons = {
					locale: "title-locale",
					section: "title-section",
					page: "title-page",
					dcontent: "title-dcontent",

					// menubuttons.set is called by updateLocaleMenu and updateHashAndMenus
					set: function(x, y) {
						stdebug("menuset " + x + " = " + y);
						var cnode = document.getElementById(x + "-container");
						var wnode = dijitRegistry.byId(x);
						var dnode = document.getElementById(x);
						if (!cnode) {
							cnode = dnode; // for Elements that do their own stunts
						}
						if (y && y !== '-' && y !== '') {
							if (wnode != null) {
								wnode.set('label', y);
							} else {
								updateIf(x, y); // non widget
							}
							setDisplayed(cnode, true);
						} else {
							setDisplayed(cnode, false);
							if (wnode != null) {
								wnode.set('label', '-');
							} else {
								updateIf(x, '-'); // non widget
							}
						}
					},
				};

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
					if (special) {
						stdebug("OS: Using cached special: " + name);
						onSuccess(special);
					} else if (special === null) {
						stdebug("OS: cached NULL: " + name);
						onFailure("Special page failed to load: " + name);
					} else {
						stdebug("OS: Attempting load.." + name);
						try {
							require(["js/special/" + name + ".js"], function(specialFn) {
								stdebug("OS: Loaded, instantiatin':" + name);
								var special = new specialFn();
								special.name = name;
								otherThis.pages[name] = special; // cache for next time

								stdebug("OS: SUCCESS! " + name);
								onSuccess(special);
							});
						} catch (e) {
							stdebug("OS: Load FAIL!:" + name + " - " + e.message + " - " + e);
							if (!otherThis.pages[name]) { // if the load didn't complete:
								otherThis.pages[name] = null; // mark as don't retry load.
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
						var msg_fmt = cldrText.sub("v_bad_special_msg", {
							special: name
						});
						params.flipper.flipTo(params.pages.loading, loadingChunk = createChunk(msg_fmt, "p", "errCodeMsg"));
						isLoading = false;
					});
				};

				/**
				 * instance of otherSpecial manager
				 * @property otherSpecial
				 */
				var otherSpecial = new OtherSpecial();

				/**
				 * Parse the hash string into surveyCurrent___ variables.
				 * Expected to update document.title also.
				 *
				 * @param {String} id
				 */
				window.parseHash = function parseHash(hash) {
					function updateWindowTitle() {
						var t = cldrText.get('survey_title');
						const curLocale = cldrStatus.getCurrentLocale();
						if (curLocale && curLocale != '') {
							t = t + ': ' + locmap.getLocaleName(curLocale);
						}
						const curSpecial = cldrStatus.getCurrentSpecial();
						if (curSpecial && curSpecial != '') {
							t = t + ': ' + cldrText.get('special_' + curSpecial);
						}
						const curPage = cldrStatus.getCurrentPage();
						if (curPage && curPage != '') {
							t = t + ': ' + curPage;
						}
						document.title = t;
					};
					if (hash) {
						var pieces = hash.substr(0).split("/");
						if (pieces.length > 1) {
							cldrStatus.setCurrentLocale(pieces[1]); // could be null
							/*
							 * TODO: find a way if possible to fix here when cldrStatus.getCurrentLocale() === "USER".
							 * It may be necessary (and sufficient) to wait for server response, see "USER" elsewhere
							 * in this file. cachedJson.loc and _thePages.loc are generally (always?) undefined here.
							 * Reference: https://unicode.org/cldr/trac/ticket/11161
							 */
						} else {
							cldrStatus.setCurrentLocale('');
						}
						const curLocale = cldrStatus.getCurrentLocale();
						if (pieces[0].length == 0 && curLocale != '' && curLocale != null) {
							if (pieces.length > 2) {
								cldrStatus.setCurrentPage(pieces[2]);
								if (pieces.length > 3) {
									let id = pieces[3];
									if (id.substr(0, 2) == 'x@') {
										id = id.substr(2);
									}
									cldrStatus.setCurrentId(id);
								} else {
									cldrStatus.setCurrentId('');
								}
							} else {
								cldrStatus.setCurrentPage('');
								cldrStatus.setCurrentId('');
							}
							cldrStatus.setCurrentSpecial(null);
						} else {
							cldrStatus.setCurrentSpecial(pieces[0]);
							if (cldrStatus.getCurrentSpecial() == '') {
								cldrStatus.setCurrentSpecial('locales');
							}
							if (cldrStatus.getCurrentSpecial() == 'locales') {
								// allow locales list to retain ID / Page string for passthrough.
								cldrStatus.setCurrentLocale('');
								if (pieces.length > 2) {
									cldrStatus.setCurrentPage(pieces[2]);
									if (pieces.length > 3) {
										let id = pieces[3];
										if (id.substr(0, 2) == 'x@') {
											id = id.substr(2);
										}
										cldrStatus.setCurrentId(id);
									} else {
										cldrStatus.setCurrentId('');
									}
								} else {
									cldrStatus.setCurrentPage('');
									cldrStatus.setCurrentId('');
								}
							} else if (isReport(cldrStatus.getCurrentSpecial())) { // allow page and ID to fall through.
								if (pieces.length > 2) {
									cldrStatus.setCurrentPage(pieces[2]);
									if (pieces.length > 3) {
										cldrStatus.setCurrentId(pieces[3]);
									} else {
										cldrStatus.setCurrentId('');
									}
								} else {
									cldrStatus.setCurrentPage('');
									cldrStatus.setCurrentId('');
								}
							} else {
								otherSpecial.parseHash(cldrStatus.getCurrentSpecial(), hash, pieces);
							}
						}
					} else {
						cldrStatus.setCurrentLocale('');
						cldrStatus.setCurrentSpecial('locales');
						cldrStatus.setCurrentId('');
						cldrStatus.setCurrentPage('');
						cldrStatus.setCurrentSection('');
					}
					updateWindowTitle();

					// if there is no locale id, refresh the search.
					if (!cldrStatus.getCurrentLocale()) {
						searchRefresh();
					}
				};

				/**
				 * Update hash (and title)
				 *
				 * @param doPush {Boolean} if true, do a push (instead of replace)
				 *
				 * Called by cldrStForum.parseContent, as well as locally.
				 *
				 * TODO: avoid attaching this, or anything, to "window"! Define our own objects instead.
				 */
				window.replaceHash = function replaceHash(doPush) {
					if (!doPush) {
						doPush = false; // by default -replace.
					}
					var theId = cldrStatus.getCurrentId();
					if (theId == null) {
						theId = '';
					}
					var theSpecial = cldrStatus.getCurrentSpecial();
					if (theSpecial == null) {
						theSpecial = '';
					}
					var thePage = cldrStatus.getCurrentPage();
					if (thePage == null) {
						thePage = '';
					}
					var theLocale = cldrStatus.getCurrentLocale();
					if (theLocale == null) {
						theLocale = '';
					}
					var newHash = '#' + theSpecial + '/' + theLocale + '/' + thePage + '/' + theId;
					if (newHash != dojoHash()) {
						dojoHash(newHash, !doPush);
					}
				};

				window.updateCurrentId = function updateCurrentId(id) {
					if (id == null) {
						id = '';
					}
					if (cldrStatus.getCurrentId() != id) { // don't set if already set.
						cldrStatus.setCurrentId(id);
						replaceHash(false); // usually don't want to save
					}
				};

				// (back to showV) some setup.
				// click on the title to copy (permalink)
				clickToSelect(document.getElementById("ariScroller"));
				updateIf("title-dcontent-link", cldrText.get("defaultContent_titleLink"));

				// TODO - rewrite using AMD
				/**
				 * @param postData optional - makes this a POST
				 */
				window.myLoad = function myLoad(url, message, handler, postData, headers) {
					var otime = new Date().getTime();
					console.log("MyLoad: " + url + " for " + message);
					var errorHandler = function(err) {
						console.log('Error: ' + err);
						handleDisconnect("Could not fetch " + message + " - error " + err
							+ "\n url: " + url + "\n", null, "disconnect");
					};
					var loadHandler = function(json) {
						console.log("        " + url + " loaded in " + (new Date().getTime() - otime) + "ms");
						try {
							handler(json);
							//resize height
							$('#main-row').css({
								height: $('#main-row>div').height()
							});
						} catch (e) {
							console.log("Error in ajax post [" + message + "]  " + e.message + " / " + e.name);
							handleDisconnect("Exception while loading: " + message + " - " + e.message + ", n=" + e.name + ' \nStack:\n' + (e.stack || '[none]'), null); // in case the 2nd line doesn't work
						}
					};
					var xhrArgs = {
						url: url,
						handleAs: "json",
						load: loadHandler,
						error: errorHandler,
						postData: postData,
						headers: headers
					};
					cldrStAjax.queueXhr(xhrArgs);
				};

				/**
				 * Verify that the JSON returned is as expected.
				 *
				 * @param json the returned json
				 * @param subkey the key to look for,  json.subkey
				 * @return true if OK, false if bad
				 */
				function verifyJson(json, subkey) {
					if (!json) {
						console.log("!json");
						showLoader(null, "Error while  loading " + subkey + ":  <br><div style='border: 1px solid red;'>" + "no data!" + "</div>");
						return false;
					} else if (json.err_code) {
						var msg_fmt = formatErrMsg(json, subkey);
						var loadingChunk;
						flipper.flipTo(pages.loading, loadingChunk = createChunk(msg_fmt, "p", "errCodeMsg"));
						var retryButton = createChunk(cldrText.get("loading_reload"), "button");
						loadingChunk.appendChild(retryButton);
						retryButton.onclick = function() {
							window.location.reload(true);
						};
						return false;
					} else if (json.err) {
						console.log("json.err!" + json.err);
						showLoader(null, "Error while  loading " + subkey + ": <br><div style='border: 1px solid red;'>" + json.err + "</div>");
						handleDisconnect("while loading " + subkey + "", json);
						return false;
					} else if (!json[subkey]) {
						console.log("!json.oldvotes");
						showLoader(null, "Error while  loading " + subkey + ": <br><div style='border: 1px solid red;'>" + "no data" + "</div>");
						handleDisconnect("while loading- no " + subkey + "", json);
						return false;
					} else {
						return true;
					}
				}

				window.showCurrentId = function() {
					const curSpecial = cldrStatus.getCurrentSpecial();
					if (curSpecial && curSpecial != '' && !isDashboard()) {
						otherSpecial.handleIdChanged(curSpecial, showCurrentId);
					} else {
						const curId = cldrStatus.getCurrentId();
						if (curId != '') {
							var xtr = document.getElementById('r@' + curId);
							if (!xtr) {
								console.log("Warning could not load id " + curId + " does not exist");
								window.updateCurrentId(null);
							} else if (xtr.proposedcell && xtr.proposedcell.showFn) {
								// TODO: visible? coverage?
								window.showInPop("", xtr, xtr.proposedcell, xtr.proposedcell.showFn, true);
								console.log("Changed to " + cldrStatus.getCurrentId());
								if (!isDashboard())
									scrollToItem();
							} else {
								console.log("Warning could not load id " + curId + " - not setup - " + xtr.toString() + " pc=" + xtr.proposedcell + " sf = " + xtr.proposedcell.showFn);
							}
						}
					}
				};

				window.ariRetry = function() {
					if (!ariDialog) {
						console.log("Error: no ariDialog in window.ariRetry");
					} else {
						ariDialog.hide();
					}
					window.location.reload(true);
				};

				window.showARIDialog = function(why, json, word, oneword, p, what) {
					console.log('showARIDialog');
					p.parentNode.removeChild(p);

					if (didUnbust) {
						why = why + "\n\n" + cldrText.get('ari_force_reload');
					}

					// setup with why
					var ari_message;

					if (json && json.session_err) {
						ari_message = cldrText.get("ari_sessiondisconnect_message");
					} else {
						ari_message = cldrText.get('ari_message');
					}

					var ari_submessage = formatErrMsg(json, what);

					updateIf('ariMessage', ari_message.replace(/\n/g, "<br>"));
					updateIf('ariSubMessage', ari_submessage.replace(/\n/g, "<br>"));
					updateIf('ariScroller', window.location + '<br>' + why.replace(/\n/g, "<br>"));
					// TODO: update  ariMain and ariRetryBtn
					hideOverlayAndSidebar();

					if (!ariDialog) {
						console.log("Error: no ariDialog in window.showARIDialog 1");
					} else {
						ariDialog.show();
					}

					var oneword = document.getElementById("progress_oneword");
					oneword.onclick = function() {
						if (disconnected) {
							if (!ariDialog) {
								console.log("Error: no ariDialog in window.showARIDialog 2 onclick");
							} else {
								ariDialog.show();
							}
						}
					};
				};

				function updateCoverageMenuTitle() {
					if (surveyUserCov) {
						$('#coverage-info').text(cldrText.get('coverage_' + surveyUserCov));
					} else {
						$('#coverage-info').text(cldrText.sub('coverage_auto_msg', {
							surveyOrgCov: cldrText.get('coverage_' + surveyOrgCov)
						}));
					}
				}

				function updateLocaleMenu() {
					const curLocale = cldrStatus.getCurrentLocale();
					if (curLocale != null && curLocale != '' && curLocale != '-') {
						cldrStatus.setCurrentLocaleName(locmap.getLocaleName(curLocale));
						var bund = locmap.getLocaleInfo(curLocale);
						if (bund) {
							if (bund.readonly) {
								addClass(document.getElementById(menubuttons.locale), "locked");
							} else {
								removeClass(document.getElementById(menubuttons.locale), "locked");
							}

							if (bund.dcChild) {
								menubuttons.set(menubuttons.dcontent, cldrText.sub("defaultContent_header_msg", {
									info: bund,
									locale: cldrStatus.getCurrentLocale(),
									dcChild: locmap.getLocaleName(bund.dcChild)
								}));
							} else {
								menubuttons.set(menubuttons.dcontent);
							}
						} else {
							removeClass(document.getElementById(menubuttons.locale), "locked");
							menubuttons.set(menubuttons.dcontent);
						}
					} else {
						cldrStatus.setCurrentLocaleName('');
						removeClass(document.getElementById(menubuttons.locale), "locked");
						menubuttons.set(menubuttons.dcontent);
					}
					menubuttons.set(menubuttons.locale, cldrStatus.getCurrentLocaleName());
				}

				/**
				 * Update the #hash and menus to the current settings.
				 *
				 * @param doPush {Boolean} if false, do not add to history
				 */
				function updateHashAndMenus(doPush) {
					const sessionId = cldrStatus.getSessionId();
					const surveyUser = cldrStatus.getSurveyUser();
					const userID = (surveyUser && surveyUser.id) ? surveyUser.id : 0;
					const surveyUserPerms = cldrStatus.getPermissions();
					const surveyUserURL = {
						myAccountSetting: 'survey?do=listu',
						disableMyAccount: "lock.jsp",
						recentActivity: "myvotes.jsp?user=" + userID + "&s=" + sessionId,
						xmlUpload: "upload.jsp?a=/cldr-apps/survey&s=" + sessionId,
						manageUser: "survey?do=list",
						flag: "tc-flagged.jsp?s=" + sessionId,
						about: "about.jsp",
						browse: "browse.jsp",
						adminPanel: 'SurveyAjax?what=admin_panel&s=' + sessionId,
					};

					/**
					 * 'name' - the js/special/___.js name
					 * 'hidden' - true to hide the item
					 * 'title' - override of menu name
					 * @property specialItems
					 */
					var specialItems = new Array();
					if (surveyUser != null) {
						specialItems = [{
							divider: true
						},

						{
							title: 'Admin Panel',
							url: surveyUserURL.adminPanel,
							display: (surveyUser && surveyUser.userlevelName === 'ADMIN')
						},
						{
							divider: true,
							display: (surveyUser && surveyUser.userlevelName === 'ADMIN')
						},

						{
							title: 'My Account'
						}, // My Account section

						{
							title: 'Settings',
							level: 2,
							url: surveyUserURL.myAccountSetting,
							display: (surveyUser && true)
						},
						{
							title: 'Lock (Disable) My Account',
							level: 2,
							url: surveyUserURL.disableMyAccount,
							display: (surveyUser && true)
						},

						{
							divider: true
						},
						{
							title: 'My Votes'
						}, // My Votes section

						/*
						 * This indirectly references "special_oldvotes" in cldrText.js
						 */
						{
							special: 'oldvotes',
							level: 2,
							display: (surveyUserPerms && surveyUserPerms.userCanImportOldVotes)
						},
						{
							title: 'See My Recent Activity',
							level: 2,
							url: surveyUserURL.recentActivity
						},
						{
							title: 'Upload XML',
							level: 2,
							url: surveyUserURL.xmlUpload
						},

						{
							divider: true
						},
						{
							title: 'My Organization(' + cldrStatus.getOrganizationName() + ')'
						}, // My Organization section

						{
							special: 'vsummary', /* Cf. special_vsummary */
							level: 2,
							display: (surveyUserPerms && surveyUserPerms.userCanUseVettingSummary)
						},
						{
							title: 'List ' + cldrStatus.getOrganizationName() + ' Users',
							level: 2,
							url: surveyUserURL.manageUser,
							display: (surveyUserPerms && (surveyUserPerms.userIsTC || surveyUserPerms.userIsVetter))
						},
						{
							special: 'forum_participation', /* Cf. special_forum_participation */
							level: 2,
							display: (surveyUserPerms && surveyUserPerms.userCanMonitorForum)
						},
						{
							special: 'vetting_participation', /* Cf. special_vetting_participation */
							level: 2,
							display: (surveyUserPerms && (surveyUserPerms.userIsTC || surveyUserPerms.userIsVetter))
						},
						{
							title: 'LOCKED: Note: your account is currently locked.',
							level: 2,
							display: (surveyUserPerms && surveyUserPerms.userIsLocked),
							bold: true
						},

						{
							divider: true
						},
						{
							title: 'Forum'
						}, // Forum section

						{
							special: 'flagged',
							level: 2,
							hasFlag: true
						},
						{
							special: 'mail',
							level: 2,
							display: cldrStatus.getIsUnofficial()
						},
						{
							special: 'bulk_close_posts', /* Cf. special_bulk_close_posts */
							level: 2,
							display: (surveyUser && surveyUser.userlevelName === 'ADMIN')
						},

						{
							divider: true
						},
						{
							title: 'Informational'
						}, // Informational section

						{
							special: 'statistics',
							level: 2
						},
						{
							title: 'About',
							level: 2,
							url: surveyUserURL.about
						},
						{
							title: 'Lookup a code or xpath',
							level: 2,
							url: surveyUserURL.browse,
						},
						{
					         title: 'Error Subtypes',
					         level: 2,
					         url: './tc-all-errors.jsp',
					         display: (surveyUserPerms && surveyUserPerms.userIsTC)
					 	},
						{
							divider: true
						},
						];
					}
					if (!doPush) {
						doPush = false;
					}
					replaceHash(doPush); // update the hash
					updateLocaleMenu();

					if (cldrStatus.getCurrentLocale() == null) {
						menubuttons.set(menubuttons.section);
						const curSpecial = cldrStatus.getCurrentSpecial();
						if (curSpecial != null) {
							var specialId = "special_" + curSpecial;
							menubuttons.set(menubuttons.page, cldrText.get(specialId));
						} else {
							menubuttons.set(menubuttons.page);
						}
						return; // nothing to do.
					}
					var titlePageContainer = document.getElementById("title-page-container");

					/**
					 * Just update the titles of the menus. Internal to updateHashAndMenus
					 */
					function updateMenuTitles(menuMap) {
						if (menubuttons.lastspecial === undefined) {
							menubuttons.lastspecial = null;

							// Set up the menu here?
							var parMenu = document.getElementById("manage-list");
							for (var k = 0; k < specialItems.length; k++) {
								var item = specialItems[k];
								(function(item) {
									if (item.display != false) {
										var subLi = document.createElement("li");
										if (item.special) { // special items so look up in cldrText.js
											item.title = cldrText.get('special_' + item.special);
											item.url = '#' + item.special;
											item.blank = false;
										}
										if (item.url) {
											var subA = document.createElement("a");

											if (item.hasFlag) { // forum may need images attached to it
												var Img = document.createElement("img");
												Img.setAttribute('src', "flag.png");
												Img.setAttribute('alt', "flag");
												Img.setAttribute('title', "flag.png");
												Img.setAttribute('border', 0);

												subA.appendChild(Img);
											}
											subA.appendChild(document.createTextNode(item.title + ' '));
											subA.href = item.url;

											if (item.blank != false) {
												subA.target = '_blank';
												subA.appendChild(createChunk('', 'span', 'glyphicon glyphicon-share manage-list-icon'));
											}

											if (item.level) { // append it to appropriate levels
												var level = item.level;
												for (var i = 0; i < level - 1; i++) {
													/*
													 * Indent by creating lists within lists, each list containing only one item.
													 * TODO: indent by a better method. Note that for valid html, ul should contain li;
													 * ul directly containing element other than li is generally invalid.
													 */
													let ul = document.createElement("ul");
													let li = document.createElement("li");
													ul.setAttribute('style', 'list-style-type:none');
													ul.appendChild(li);
													li.appendChild(subA);
													subA = ul;
												}
											}
											subLi.appendChild(subA);
										}
										if (!item.url && !item.divider) { // if it is pure text/html & not a divider
											if (!item.level) {
												subLi.appendChild(document.createTextNode(item.title + ' '));
											} else {
												var subA = null;
												if (item.bold) {
													subA = document.createElement("b");
												} else if (item.italic) {
													subA = document.createElement("i");
												} else {
													subA = document.createElement("span");
												}
												subA.appendChild(document.createTextNode(item.title + ' '));

												var level = item.level;
												for (var i = 0; i < level - 1; i++) {
													let ul = document.createElement("ul");
													let li = document.createElement("li");
													ul.setAttribute('style', 'list-style-type:none');
													ul.appendChild(li);
													li.appendChild(subA);
													subA = ul;
												}
												subLi.appendChild(subA);
											}
										}
										if (item.divider) {
											subLi.className = 'nav-divider';
										}
										parMenu.appendChild(subLi);
									}
								})(item);
							}
						}

						if (menubuttons.lastspecial) {
							removeClass(menubuttons.lastspecial, "selected");
						}

						updateLocaleMenu(menuMap);
						const curSpecial = cldrStatus.getCurrentSpecial();
						if (curSpecial != null && curSpecial != '') {
							var specialId = "special_" + curSpecial;
							$('#section-current').html(cldrText.get(specialId));
							setDisplayed(titlePageContainer, false);
						} else if (!menuMap) {
							setDisplayed(titlePageContainer, false);
						} else {
							const curPage = cldrStatus.getCurrentPage();
							if (menuMap.sectionMap[curPage]) {
								const curSection = curPage; // section = page
								cldrStatus.setCurrentSection(curSection);
								$('#section-current').html(menuMap.sectionMap[curSection].name);
								setDisplayed(titlePageContainer, false); // will fix title later
							} else if (menuMap.pageToSection[curPage]) {
								var mySection = menuMap.pageToSection[curPage];
								cldrStatus.setCurrentSection(mySection.id);
								$('#section-current').html(mySection.name);
								setDisplayed(titlePageContainer, false); // will fix title later
							} else {
								$('#section-current').html(cldrText.get("section_general"));
								setDisplayed(titlePageContainer, false);
							}
						}
					}

					/**
					 * Update the menus
					 */
					function updateMenus(menuMap) {
						// initialize menus
						if (!menuMap.menusSetup) {
							menuMap.menusSetup = true;
							menuMap.setCheck = function(menu, checked, disabled) {
								menu.set('iconClass', checked ? "dijitMenuItemIcon menu-x" : "dijitMenuItemIcon menu-o");
								menu.set('disabled', disabled);
							};
							var menuSection = dijitRegistry.byId("menu-section");
							menuMap.section_general = new dijitMenuItem({
								label: cldrText.get("section_general"),
								iconClass: "dijitMenuItemIcon ",
								disabled: true,
								onClick: function() {
									if (cldrStatus.getCurrentPage() != '' || (cldrStatus.getCurrentSpecial() != '' && cldrStatus.getCurrentSpecial() != null)) {
										cldrStatus.setCurrentId(''); // no id if jumping pages
										cldrStatus.setCurrentPage('');
										cldrStatus.setCurrentSection('');
										cldrStatus.setCurrentSpecial('');
										updateMenuTitles(menuMap);
										reloadV();
									}
								}
							});
							menuSection.addChild(menuMap.section_general);
							for (var j in menuMap.sections) {
								(function(aSection) {
									aSection.menuItem = new dijitMenuItem({
										label: aSection.name,
										iconClass: "dijitMenuItemIcon",
										onClick: function() {
											cldrStatus.setCurrentId('!'); // no id if jumping pages
											cldrStatus.setCurrentPage(aSection.id);
											cldrStatus.setCurrentSpecial('');
											updateMenus(menuMap);
											updateMenuTitles(menuMap);
											reloadV();
										},
										disabled: true
									});

									menuSection.addChild(aSection.menuItem);
								})(menuMap.sections[j]);
							}

							menuSection.addChild(new dijitMenuSeparator());

							menuMap.forumMenu = new dijitMenuItem({
								label: cldrText.get("section_forum"),
								iconClass: "dijitMenuItemIcon", // menu-chat
								disabled: true,
								onClick: function() {
									cldrStatus.setCurrentId('!'); // no id if jumping pages
									cldrStatus.setCurrentPage('');
									cldrStatus.setCurrentSpecial('forum');
									updateMenus(menuMap);
									updateMenuTitles(menuMap);
									reloadV();
								}
							});
							menuSection.addChild(menuMap.forumMenu);
						}

						updateMenuTitles(menuMap);

						var myPage = null;
						var mySection = null;
						const curSpecial = cldrStatus.getCurrentSpecial();
						if (curSpecial == null || curSpecial == '') {
							// first, update display names
							const curPage = cldrStatus.getCurrentPage();
							if (menuMap.sectionMap[curPage]) { // page is really a section
								mySection = menuMap.sectionMap[curPage];
								myPage = null;
							} else if (menuMap.pageToSection[curPage]) {
								mySection = menuMap.pageToSection[curPage];
								myPage = mySection.pageMap[curPage];
							}
							if (mySection !== null) {
								// update menus under 'page' - peer pages
								if (!titlePageContainer.menus) {
									titlePageContainer.menus = {};
								}

								// hide all. TODO use a foreach model?
								for (var zz in titlePageContainer.menus) {
									var aMenu = titlePageContainer.menus[zz];
									aMenu.set('label', '-');
								}

								var showMenu = titlePageContainer.menus[mySection.id];

								if (!showMenu) {
									// doesn't exist - add it.
									var menuPage = new dijitDropDownMenu();
									for (var k in mySection.pages) { // use given order
										(function(aPage) {
											var pageMenu = aPage.menuItem = new dijitMenuItem({
												label: aPage.name,
												iconClass: (aPage.id == cldrStatus.getCurrentPage()) ? "dijitMenuItemIcon menu-x" : "dijitMenuItemIcon menu-o",
												onClick: function() {
													cldrStatus.setCurrentId(''); // no id if jumping pages
													cldrStatus.setCurrentPage(aPage.id);
													updateMenuTitles(menuMap);
													reloadV();
												},
												disabled: (effectiveCoverage() < parseInt(aPage.levs[cldrStatus.getCurrentLocale()]))
											});
										})(mySection.pages[k]);
									}

									showMenu = new dijitDropDownButton({
										label: '-',
										dropDown: menuPage
									});

									titlePageContainer.menus[mySection.id] = mySection.pagesMenu = showMenu;
								}

								if (myPage !== null) {
									/*
									 * TODO: if 'use strict' in this file, we get:
									 * Ignoring get or set of property that has [LenientThis] because the “this” object is incorrect.
									 */
									$('#title-page-container').html('<h1>' + myPage.name + '</h1>').show();
								} else {
									$('#title-page-container').html('').hide();
								}
								setDisplayed(showMenu, true);
								setDisplayed(titlePageContainer, true); // will fix title later
							}
						}

						stdebug('Updating menus.. ecov = ' + effectiveCoverage());

						menuMap.setCheck(menuMap.section_general, (cldrStatus.getCurrentPage() == '' && (cldrStatus.getCurrentSpecial() == '' || cldrStatus.getCurrentSpecial() == null)), false);

						// Update the status of the items in the Section menu
						for (var j in menuMap.sections) {
							var aSection = menuMap.sections[j];
							// need to see if any items are visible @ current coverage
							const curLocale = cldrStatus.getCurrentLocale();
							stdebug("for " + aSection.name + " minLev[" + curLocale + "] = " + aSection.minLev[curLocale]);
							const curSection = cldrStatus.getCurrentSection();
							menuMap.setCheck(aSection.menuItem, (curSection == aSection.id), effectiveCoverage() < aSection.minLev[curLocale]);

							// update the items in that section's Page menu
							if (curSection == aSection.id) {
								for (var k in aSection.pages) {
									var aPage = aSection.pages[k];
									if (!aPage.menuItem) {
										console.log("Odd - " + aPage.id + " has no menuItem");
									} else {
										menuMap.setCheck(aPage.menuItem, (aPage.id == cldrStatus.getCurrentPage()), (effectiveCoverage() < parseInt(aPage.levs[curLocale])));
									}
								}
							}
						}
						menuMap.setCheck(menuMap.forumMenu, (cldrStatus.getCurrentSpecial() == 'forum'), (cldrStatus.getSurveyUser() === null));
						resizeSidebar();
					}

					const curLocale = cldrStatus.getCurrentLocale();
					if (_thePages == null || _thePages.loc != curLocale) {
						// show the raw IDs while loading.
						updateMenuTitles(null);

						if (curLocale != null && curLocale != '') {
							var needLocTable = false;

							var url = cldrStatus.getContextPath() + "/SurveyAjax?what=menus&_=" + curLocale + "&locmap=" + needLocTable + "&s=" + cldrStatus.getSessionId() + cacheKill();
							myLoad(url, "menus", function(json) {
								if (!verifyJson(json, "menus")) {
									return; // busted?
								}

								if (json.locmap) {
									locmap = new LocaleMap(locmap); // overwrite with real data
								}

								// make this into a hashmap.
								if (json.canmodify) {
									var canmodify = {};
									for (var k in json.canmodify) {
										canmodify[json.canmodify[k]] = true;
									}
									window.canmodify = canmodify;
								}

								updateCovFromJson(json);
								updateCoverageMenuTitle();
								updateCoverage(flipper.get(pages.data)); // update CSS and auto menu title

								function unpackMenus(json) {
									var menus = json.menus;

									if (_thePages) {
										stdebug("Updating cov info into menus for " + json.loc);
										for (var k in menus.sections) {
											var oldSection = _thePages.sectionMap[menus.sections[k].id];
											for (var j in menus.sections[k].pages) {
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
										for (var k in menus.sections) {
											menus.sectionMap[menus.sections[k].id] = menus.sections[k];
											menus.sections[k].pageMap = {};
											menus.sections[k].minLev = {};
											for (var j in menus.sections[k].pages) {
												menus.sections[k].pageMap[menus.sections[k].pages[j].id] = menus.sections[k].pages[j];
												menus.pageToSection[menus.sections[k].pages[j].id] = menus.sections[k];
											}
										}
										_thePages = menus;
									}

									stdebug("Calculating minimum section coverage for " + json.loc);
									for (var k in _thePages.sectionMap) {
										var min = 200;
										for (var j in _thePages.sectionMap[k].pageMap) {
											var thisLev = parseInt(_thePages.sectionMap[k].pageMap[j].levs[json.loc]);
											if (min > thisLev) {
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

					var bund = locmap.getLocaleInfo(cldrStatus.getCurrentLocale());
					let msg = null;

					if (bund) {
						if (bund.readonly || bund.special_comment_raw) {
							if (bund.readonly) {
								if (bund.special_comment_raw) {
									msg = bund.special_comment_raw;
								} else {
									msg = cldrText.get("readonly_unknown");
								}
								msg = cldrText.sub("readonly_msg", {
									info: bund,
									locale: cldrStatus.getCurrentLocale(),
									msg: msg
								});
							} else {
								// Not readonly, could be a scratch locale
								msg = bund.special_comment_raw;
							}
							if (msg) {
								msg = locmap.linkify(msg);
								var theChunk = cldrDomConstruct(msg);
								var subDiv = document.createElement("div");
								subDiv.appendChild(theChunk);
								subDiv.className = 'warnText';
								theDiv.insertBefore(subDiv, theDiv.childNodes[0]);
							}
						} else if (bund.dcChild) {
							const html = cldrText.sub("defaultContentChild_msg", {
								name: bund.name,
								dcChild: bund.dcChild,
								locale: cldrStatus.getCurrentLocale(),
								dcChildName: locmap.getLocaleName(bund.dcChild)
							})
							var theChunk = cldrDomConstruct(html);
							var subDiv = document.createElement("div");
							subDiv.appendChild(theChunk);
							subDiv.className = 'warnText';
							theDiv.insertBefore(subDiv, theDiv.childNodes[0]);
						}
					}
					if (cldrStatus.getIsPhaseBeta()) {
						const html = cldrText.sub("beta_msg", {
							info: bund,
							locale: cldrStatus.getCurrentLocale(),
							msg: msg
						});
						var theChunk = cldrDomConstruct(html);
						var subDiv = document.createElement("div");
						subDiv.appendChild(theChunk);
						subDiv.className = 'warnText';
						theDiv.insertBefore(subDiv, theDiv.childNodes[0]);
					}
				};

				/**
				 * Show the "possible problems" section which has errors for the locale
				 */
				function showPossibleProblems(flipper, flipPage, loc, session, effectiveCov, requiredCov) {
					cldrStatus.setCurrentLocale(loc);

							var url = cldrStatus.getContextPath() + "/SurveyAjax?what=possibleProblems&_=" + cldrStatus.getCurrentLocale() + "&s=" + session + "&eff=" + effectiveCov + "&req=" + requiredCov + cacheKill();
							myLoad(url, "possibleProblems", function(json) {
								if (verifyJson(json, 'possibleProblems')) {
									stdebug("json.possibleProblems OK..");
									if (json.dataLoadTime) {
										updateIf("dynload", json.dataLoadTime);
									}

									var theDiv = flipper.flipToEmpty(flipPage);

									insertLocaleSpecialNote(theDiv);

									if (json.possibleProblems.length > 0) {
										var subDiv = createChunk("", "div");
										subDiv.className = "possibleProblems";

										var h3 = createChunk(cldrText.get("possibleProblems"), "h3");
										subDiv.appendChild(h3);

										var div3 = document.createElement("div");
										var newHtml = "";
										newHtml += testsToHtml(json.possibleProblems);
										div3.innerHTML = newHtml;
										subDiv.appendChild(div3);
										theDiv.appendChild(subDiv);
									}
									var theInfo = createChunk("", "p", "special_general");
									theDiv.appendChild(theInfo);
									theInfo.innerHTML = cldrText.get("special_general"); // TODO replace with … ?
									hideLoader(null);
								}
							});
						}

				var isLoading = false;

				window.reloadV = function reloadV() {
					if (disconnected) {
						unbust();
					}

					document.getElementById('DynamicDataSection').innerHTML = ''; //reset the data
					$('#nav-page').hide();
					$('#nav-page-footer').hide();
					isLoading = false;

					/*
					 * Scroll back to top when loading a new page, to avoid a bug where, for
					 * example, having scrolled towards bottom, we switch from a Section page
					 * to the Forum page and the scrollbar stays where it was, making the new
					 * content effectively invisible.
					 */
					window.scrollTo(0, 0);

					/*
					 * TODO: explain code related to "showers".
					 */
					showers[flipper.get(pages.data).id] = function() {
						console.log("reloadV()'s shower - ignoring reload request, we are in the middle of a load!");
					};

					// assume parseHash was already called, if we are taking input from the hash
					if (!ariDialog) {
						console.log("Error: no ariDialog in window.reloadV");
					} else {
						ariDialog.hide();
					}

					updateHashAndMenus(true);

					const curLocale = cldrStatus.getCurrentLocale();
					if (curLocale != null && curLocale != '' && curLocale != '-') {
						var bund = locmap.getLocaleInfo(curLocale);
						if (bund !== null && bund.dcParent) {
							const html = cldrText.sub("defaultContent_msg", {
								name: bund.name,
								dcParent: bund.dcParent,
								locale: curLocale,
								dcParentName: locmap.getLocaleName(bund.dcParent)
							});
							var theChunk = cldrDomConstruct(html);
							var theDiv = document.createElement("div");
							theDiv.appendChild(theChunk);
							theDiv.className = 'ferrbox';
							flipper.flipTo(pages.other, theDiv);
							return;
						}
					}

					// TODO: don't even flip if it's quick.
					var loadingChunk;
					flipper.flipTo(pages.loading, loadingChunk = createChunk(cldrText.get("loading"), "i", "loadingMsg"));

					var itemLoadInfo = createChunk("", "div", "itemLoadInfo");

					// Create a little spinner to spin "..." so the user knows we are doing something..
					var spinChunk = createChunk("...", "i", "loadingMsgSpin");
					var spin = 0;
					var timerToKill = window.setInterval(function() {
						var spinTxt = '';
						spin++;
						switch (spin % 3) {
							case 0:
								spinTxt = '.  ';
								break;
							case 1:
								spinTxt = ' . ';
								break;
							case 2:
								spinTxt = '  .';
								break;
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
						if (isLoading) {
							console.log("reloadV inner shower: already isLoading, exitting.");
							return;
						}
						isLoading = true;
						var theDiv = flipper.get(pages.data);
						var theTable = theDiv.theTable;

						if (!theTable) {
							var theTableList = theDiv.getElementsByTagName("table");
							if (theTableList) {
								theTable = theTableList[0];
								theDiv.theTable = theTable;
							}
						}

						showLoader(null, cldrText.get('loading'));

						const curSpecial = cldrStatus.getCurrentSpecial();
						const curLocale = cldrStatus.getCurrentLocale();
						if ((curSpecial == null || curSpecial == '') && curLocale != null && curLocale != '') {
							const curPage = cldrStatus.getCurrentPage();
							if ((curPage == null || curPage == '') && (cldrStatus.getCurrentId() == null || cldrStatus.getCurrentId() == '')) {
								// the 'General Info' page.
								itemLoadInfo.appendChild(document.createTextNode(locmap.getLocaleName(curLocale)));
								showPossibleProblems(flipper, pages.other, curLocale, cldrStatus.getSessionId(), covName(effectiveCoverage()), covName(effectiveCoverage()));
								showInPop2(cldrText.get("generalPageInitialGuidance"), null, null, null, true); /* show the box the first time */
								isLoading = false;
							} else if (cldrStatus.getCurrentId() == '!') {
								var frag = document.createDocumentFragment();
								frag.appendChild(createChunk(cldrText.get('section_help'), "p", "helpContent"));
								var infoHtml = cldrText.get('section_info_' + curPage);
								var infoChunk = document.createElement("div");
								infoChunk.innerHTML = infoHtml;
								frag.appendChild(infoChunk);
								flipper.flipTo(pages.other, frag);
								hideLoader(null);
								isLoading = false;

							} else if (!isInputBusy()) {
								/*
								 * Make “all rows” requests only when !isInputBusy, to avoid wasted requests
								 * if the user leaves the input box open for an extended time.
								 */
								// (common case) this is an actual locale data page.
								const curId = cldrStatus.getCurrentId();
								const curPage = cldrStatus.getCurrentPage();
								const curLocale = cldrStatus.getCurrentLocale();
								itemLoadInfo.appendChild(document.createTextNode(locmap.getLocaleName(curLocale) + '/' + curPage + '/' + curId));
								var url = cldrStatus.getContextPath() + "/SurveyAjax?what=getrow&_=" + curLocale + "&x=" + curPage + "&strid=" + curId + "&s=" + cldrStatus.getSessionId() + cacheKill();
								$('#nav-page').show(); // make top "Prev/Next" buttons visible while loading, cf. '#nav-page-footer' below
								myLoad(url, "section", function(json) {
									isLoading = false;
									showLoader(theDiv.loader, cldrText.get('loading2'));
									if (!verifyJson(json, 'section')) {
										return;
									} else if (json.section.nocontent) {
										cldrStatus.setCurrentSection('');
										if (json.pageId) {
											cldrStatus.setCurrentPage(json.pageId);
										} else {
											cldrStatus.setCurrentPage('');
										}
										showLoader(null);
										updateHashAndMenus(); // find out why there's no content. (locmap)
									} else if (!json.section.rows) {
										console.log("!json.section.rows");
										showLoader(theDiv.loader, "Error while  loading: <br><div style='border: 1px solid red;'>" + "no rows" + "</div>");
										handleDisconnect("while loading- no rows", json);
									} else {
										stdebug("json.section.rows OK..");
										showLoader(theDiv.loader, "loading..");
										if (json.dataLoadTime) {
											updateIf("dynload", json.dataLoadTime);
										}

										cldrStatus.setCurrentSection('');
										cldrStatus.setCurrentPage(json.pageId);
										updateHashAndMenus(); // now that we have a pageid
										if (!cldrStatus.getSurveyUser()) {
											showInPop2(cldrText.get("loginGuidance"), null, null, null, true); /* show the box the first time */
										} else if (!json.canModify) {
											showInPop2(cldrText.get("readonlyGuidance"), null, null, null, true); /* show the box the first time */
										} else {
											showInPop2(cldrText.get("dataPageInitialGuidance"), null, null, null, true); /* show the box the first time */
										}
										if (!isInputBusy()) {
											showLoader(theDiv.loader, cldrText.get('loading3'));
											cldrSurveyTable.insertRows(theDiv, json.pageId, cldrStatus.getSessionId(), json); // pageid is the xpath..
											updateCoverage(flipper.get(pages.data)); // make sure cov is set right before we show.
											flipper.flipTo(pages.data); // TODO now? or later?
											window.showCurrentId(); // already calls scroll
											refreshCounterVetting();
											$('#nav-page-footer').show(); // make bottom "Prev/Next" buttons visible after building table
										}
									}
								});
							}
						} else if (cldrStatus.getCurrentSpecial() == 'oldvotes') {
							const curLocale = cldrStatus.getCurrentLocale();
							var url = cldrStatus.getContextPath() + "/SurveyAjax?what=oldvotes&_=" + curLocale + "&s=" + cldrStatus.getSessionId() + "&" + cacheKill();
							myLoad(url, "(loading oldvotes " + curLocale + ")", function(json) {
								isLoading = false;
								showLoader(null, cldrText.get('loading2'));
								if (!verifyJson(json, 'oldvotes')) {
									return;
								} else {
									showLoader(null, "loading..");
									if (json.dataLoadTime) {
										updateIf("dynload", json.dataLoadTime);
									}

									var theDiv = flipper.flipToEmpty(pages.other); // clean slate, and proceed..

									removeAllChildNodes(theDiv);

									var h2txt = cldrText.get("v_oldvotes_title");
									theDiv.appendChild(createChunk(h2txt, "h2", "v-title"));

									if (!json.oldvotes.locale) {
										cldrStatus.setCurrentLocale('');
										updateHashAndMenus();

										var ul = document.createElement("div");
										ul.className = "oldvotes_list";
										var data = json.oldvotes.locales.data;
										var header = json.oldvotes.locales.header;

										if (data.length > 0) {
											data.sort((a, b) => a[header.LOCALE].localeCompare(b[header.LOCALE]));
											for (var k in data) {
												var li = document.createElement("li");

												var link = createChunk(data[k][header.LOCALE_NAME], "a");
												link.href = "#" + data[k][header.LOCALE];
												(function(loc, link) {
													return (function() {
														var clicky;
														listenFor(link, "click", clicky = function(e) {
															cldrStatus.setCurrentLocale(loc);
															reloadV();
															stStopPropagation(e);
															return false;
														});
														link.onclick = clicky;
													});
												})(data[k][header.LOCALE], link)();
												li.appendChild(link);
												li.appendChild(createChunk(" "));
												li.appendChild(createChunk("(" + data[k][header.COUNT] + ")"));

												ul.appendChild(li);
											}

											theDiv.appendChild(ul);

											theDiv.appendChild(createChunk(cldrText.get("v_oldvotes_locale_list_help_msg"), "p", "helpContent"));
										} else {
											theDiv.appendChild(createChunk(cldrText.get("v_oldvotes_no_old"), "i")); // TODO fix
										}
									} else {
										cldrStatus.setCurrentLocale(json.oldvotes.locale);
										updateHashAndMenus();
										var loclink;
										theDiv.appendChild(loclink = createChunk(cldrText.get("v_oldvotes_return_to_locale_list"), "a", "notselected"));
										listenFor(loclink, "click", function(e) {
											cldrStatus.setCurrentLocale('');
											reloadV();
											stStopPropagation(e);
											return false;
										});
										theDiv.appendChild(createChunk(json.oldvotes.localeDisplayName, "h3", "v-title2"));
										var oldVotesLocaleMsg = document.createElement("p");
										oldVotesLocaleMsg.className = "helpContent";
										oldVotesLocaleMsg.innerHTML = cldrText.sub("v_oldvotes_locale_msg", {
											version: surveyLastVoteVersion,
											locale: json.oldvotes.localeDisplayName
										});
										theDiv.appendChild(oldVotesLocaleMsg);
										if ((json.oldvotes.contested && json.oldvotes.contested.length > 0) || (json.oldvotes.uncontested && json.oldvotes.uncontested.length > 0)) {
											var frag = document.createDocumentFragment();
											const oldVoteCount = (json.oldvotes.contested ? json.oldvotes.contested.length : 0) +
												(json.oldvotes.uncontested ? json.oldvotes.uncontested.length : 0);
											var summaryMsg = cldrText.sub("v_oldvotes_count_msg", {
												count: oldVoteCount
											});
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
													var title = cldrText.get(content.strid);
													content.title = title;
													content.appendChild(createChunk(title, "h2", "v-oldvotes-sub"));
												}

												content.appendChild(showVoteTable(jsondata /* voteList */, type, json));

												var submit = dojoxBusyButton({
													label: cldrText.get("v_submit_msg"),
													busyLabel: cldrText.get("v_submit_busy")
												});

												submit.on("click", function(e) {
													setDisplayed(navChunk, false);
													var confirmList = []; // these will be revoted with current params

													// explicit confirm list -  save us desync hassle
													for (var kk in jsondata) {
														if (jsondata[kk].box.checked) {
															confirmList.push(jsondata[kk].strid);
														}
													}

													var saveList = {
														locale: cldrStatus.getCurrentLocale(),
														confirmList: confirmList,
													};

													console.log(saveList.toString());
													console.log("Submitting " + type + " " + confirmList.length + " for confirm");
													const curLocale = cldrStatus.getCurrentLocale();
													var url = cldrStatus.getContextPath() + "/SurveyAjax?what=oldvotes&_=" + curLocale + "&s=" + cldrStatus.getSessionId() + "&doSubmit=true&" + cacheKill();
													myLoad(url, "(submitting oldvotes " + curLocale + ")", function(json) {
														showLoader(theDiv.loader, cldrText.get('loading2'));
														if (!verifyJson(json, 'oldvotes')) {
															handleDisconnect("Error submitting votes!", json, "Error");
															return;
														} else {
															reloadV();
														}
													}, JSON.stringify(saveList), {
														"Content-Type": "application/json"
													});
												});

												submit.placeAt(content);
												// hide by default
												setDisplayed(content, false);

												frag.appendChild(content);
												return content;
											}

											if (json.oldvotes.uncontested && json.oldvotes.uncontested.length > 0) {
												uncontestedChunk = addOldvotesType("uncontested", json.oldvotes.uncontested, frag, navChunk);
											}
											if (json.oldvotes.contested && json.oldvotes.contested.length > 0) {
												contestedChunk = addOldvotesType("contested", json.oldvotes.contested, frag, navChunk);
											}

											if (contestedChunk == null && uncontestedChunk != null) {
												setDisplayed(uncontestedChunk, true); // only item
											} else if (contestedChunk != null && uncontestedChunk == null) {
												setDisplayed(contestedChunk, true); // only item
											} else {
												// navigation
												navChunk.appendChild(createChunk(cldrText.get('v_oldvotes_show')));
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
											theDiv.appendChild(createChunk(cldrText.get("v_oldvotes_no_old_here"), "i", ""));
										}
									}
								}
								hideLoader(null);
							});
						} else if (cldrStatus.getCurrentSpecial() == 'mail') {
							var url = cldrStatus.getContextPath() + "/SurveyAjax?what=mail&s=" + cldrStatus.getSessionId() + "&fetchAll=true&" + cacheKill();
							myLoad(url, "(loading mail " + cldrStatus.getCurrentLocale() + ")", function(json) {
								hideLoader(null, cldrText.get('loading2'));
								isLoading = false;
								if (!verifyJson(json, 'mail')) {
									return;
								} else {
									if (json.dataLoadTime) {
										updateIf("dynload", json.dataLoadTime);
									}

									var theDiv = flipper.flipToEmpty(pages.other); // clean slate, and proceed..

									removeAllChildNodes(theDiv);

									var listDiv = createChunk("", "div", "mailListChunk");
									var contentDiv = createChunk("", "div", "mailContentChunk");

									theDiv.appendChild(listDiv);
									theDiv.appendChild(contentDiv);

									setDisplayed(contentDiv, false);
									var header = json.mail.header;
									var data = json.mail.data;

									if (data.length == 0) {
										listDiv.appendChild(createChunk(cldrText.get("mail_noMail"), "p", "helpContent"));
									} else {
										for (var ii in data) {
											var row = data[ii];
											var li = createChunk(row[header.QUEUE_DATE] + ": " + row[header.SUBJECT], "li", "mailRow");
											if (row[header.READ_DATE]) {
												addClass(li, "readMail");
											}
											if (header.USER !== undefined) {
												li.appendChild(document.createTextNode("(to " + row[header.USER] + ")"));
											}
											if (row[header.SENT_DATE] !== false) {
												li.appendChild(createChunk("(sent)", "span", "winner"));
											} else if (row[header.TRY_COUNT] >= 3) {
												li.appendChild(createChunk("(try#" + row[header.TRY_COUNT] + ")", "span", "loser"));
											} else if (row[header.TRY_COUNT] > 0) {
												li.appendChild(createChunk("(try#" + row[header.TRY_COUNT] + ")", "span", "warning"));
											}
											listDiv.appendChild(li);

											li.onclick = (function(li, row, header) {
												return function() {
													if (!row[header.READ_DATE]) {
														myLoad(cldrStatus.getContextPath() + "/SurveyAjax?what=mail&s=" + cldrStatus.getSessionId() + "&markRead=" + row[header.ID] + "&" + cacheKill(), 'Marking mail read', function(json) {
															if (!verifyJson(json, 'mail')) {
																return;
															} else {
																addClass(li, "readMail"); // mark as read when server answers
																row[header.READ_DATE] = true; // close enough
															}
														});
													}
													setDisplayed(contentDiv, false);

													removeAllChildNodes(contentDiv);

													contentDiv.appendChild(createChunk("Date: " + row[header.QUEUE_DATE], "h2", "mailHeader"));
													contentDiv.appendChild(createChunk("Subject: " + row[header.SUBJECT], "h2", "mailHeader"));
													contentDiv.appendChild(createChunk("Message-ID: " + row[header.ID], "h2", "mailHeader"));
													if (header.USER !== undefined) {
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
						} else if (isReport(cldrStatus.getCurrentSpecial())) {
							showLoader(theDiv.loader);
							showInPop2(cldrText.get("reportGuidance"), null, null, null, true, true); /* show the box the first time */
							var url = cldrStatus.getContextPath() + "/SurveyAjax?what=report&x=" + cldrStatus.getCurrentSpecial()
								 + "&_=" + cldrStatus.getCurrentLocale() + "&s=" + cldrStatus.getSessionId() + cacheKill();
							var errFunction = function errFunction(err) {
								console.log("Error: loading " + url + " -> " + err);
								hideLoader(null, cldrText.get('loading2'));
								isLoading = false;
								const html = "<div style='padding-top: 4em; font-size: x-large !important;' class='ferrorbox warning'>"
									+ "<span class='icon i-stop'>"
									+ " &nbsp; &nbsp;</span>Error: could not load: " + err + "</div>";
								const frag = cldrDomConstruct(html);
								flipper.flipTo(pages.other, frag);
							};
							if (isDashboard()) {
								if (!cldrStatus.isVisitor()) {
									const loadHandler = function(json) {
										hideLoader(null, cldrText.get('loading2'));
										isLoading = false;
										// further errors are handled in JSON
										showReviewPage(json, function() {
											// show function - flip to the 'other' page.
											flipper.flipTo(pages.other, null);
										});
									};
									const xhrArgs = {
										url: url,
										handleAs: "json",
										load: loadHandler,
										error: errFunction,
									}
									cldrStAjax.queueXhr(xhrArgs);
								} else {
									alert('Please login to access Dashboard');
									cldrStatus.setCurrentSpecial('');
									cldrStatus.setCurrentLocale('');
									reloadV();
								}
							} else {
								hideLoader(null, cldrText.get('loading2'));
								const loadHandler = function(html) {
									hideLoader(null, cldrText.get('loading2'));
									isLoading = false;
									const frag = cldrDomConstruct(html);
									flipper.flipTo(pages.other, frag);
									hideRightPanel(); // CLDR-14365
								};
								const xhrArgs = {
									url: url,
									handleAs: "html",
									load: loadHandler,
									error: errFunction,
								}
								cldrStAjax.queueXhr(xhrArgs);
							}
						} else if (cldrStatus.getCurrentSpecial() == 'none') {
							// for now - redirect
							hideLoader(null);
							isLoading = false;
							window.location = cldrStatus.getSurvUrl(); // redirect home
						} else if (cldrStatus.getCurrentSpecial() == 'locales') {
							hideLoader(null);
							isLoading = false;
							var theDiv = document.createElement("div");
							theDiv.className = 'localeList';

							var addSubLocale = function addSubLocale(parLocDiv, subLoc) {
								var subLocInfo = locmap.getLocaleInfo(subLoc);
								var subLocDiv = createChunk(null, "div", "subLocale");
								appendLocaleLink(subLocDiv, subLoc, subLocInfo);

								parLocDiv.appendChild(subLocDiv);
							};

							var addSubLocales = function addSubLocales(parLocDiv, subLocInfo) {
								if (subLocInfo.sub) {
									for (var n in subLocInfo.sub) {
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
								topLocRow.className = "topLocaleRow";

								var topLocDiv = document.createElement("div");
								topLocDiv.className = "topLocale";
								appendLocaleLink(topLocDiv, topLoc, topLocInfo);

								var topLocList = document.createElement("div");
								topLocList.className = "subLocaleList";

								addSubLocales(topLocList, topLocInfo);

								topLocRow.appendChild(topLocDiv);
								topLocRow.appendChild(topLocList);
								theDiv.appendChild(topLocRow);
							};

							addTopLocale("root");
							// top locales
							for (var n in locmap.locmap.topLocales) {
								var topLoc = locmap.locmap.topLocales[n];
								addTopLocale(topLoc);
							}
							flipper.flipTo(pages.other, null);
							filterAllLocale(); //filter for init data
							forceSidebar();
							cldrStatus.setCurrentLocale(null);
							cldrStatus.setCurrentSpecial('locales');
							showInPop2(cldrText.get("localesInitialGuidance"), null, null, null, true); /* show the box the first time */
							$('#itemInfo').html('');
						} else {
							otherSpecial.show(cldrStatus.getCurrentSpecial(), {
								flipper: flipper,
								pages: pages
							});
						}
					}; // end shower

					shower(); // first load

					// set up the "show-er" function so that if this locale gets reloaded,
					// the page will load again - except for the dashboard, where only the
					// row get updated
					/*
					 * TODO: clarify the above comment, and relate it to the usage of "showers" in survey.js
					 * What does "this locale gets reloaded" mean?
					 * Typically (always?) id = "DynamicDataSection" here.
					 */
					if (!isDashboard()) {
						showers[flipper.get(pages.data).id] = shower;
					}
				}; // end reloadV

				function trimNull(x) {
					if (x == null) {
						return '';
					}
					try {
						x = x.toString().trim();
					} catch (e) {
						// do nothing
					}
					return x;
				}

				/*
				 * Arrange for getInitialMenusEtc to be called after we've gotten the session id.
				 * There should be a better way to do this. For now, getInitialMenusEtc is deeply
				 * nested in legacy code, and this implementation avoids any complex linkage between
				 * this code and the code that's responsible for getting the session id from
				 * the server -- that is, updateStatus() in survey.js, as of 2020-12-10
				 */
				getM();
				function getM() {
					const sessionId = cldrStatus.getSessionId();
					if (sessionId) {
						getInitialMenusEtc(sessionId);
					} else {
						setTimeout(getM, 100); // try again after 1/10 second
					}
				}

				function getInitialMenusEtc(sessionId) {
					window.parseHash(dojoHash()); // get the initial settings
					// load the menus - first.

					var theLocale = cldrStatus.getCurrentLocale();
					if (theLocale === null || theLocale == '') {
						theLocale = 'root'; // Default.
					}
					var xurl = cldrStatus.getContextPath() + "/SurveyAjax?what=menus&_=" + theLocale + "&locmap=" + true + "&s=" + sessionId + cacheKill();
					myLoad(xurl, "initial menus for " + theLocale, function(json) {
						if (!verifyJson(json, 'locmap')) {
							return;
						} else {
							locmap = new LocaleMap(json.locmap);
							if (cldrStatus.getCurrentLocale() === "USER" && json.loc) {
								cldrStatus.setCurrentLocale(json.loc);
							}
							// make this into a hashmap.
							if (json.canmodify) {
								var canmodify = {};
								for (var k in json.canmodify) {
									canmodify[json.canmodify[k]] = true;
								}
								window.canmodify = canmodify;
							}

							// update left sidebar with locale data
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
								if (subLocInfo.sub) {
									for (var n in subLocInfo.sub) {
										var subLoc = subLocInfo.sub[n];
										addSubLocale(parLocDiv, subLoc);
									}
								}
							};

							var addTopLocale = function addTopLocale(topLoc) {
								var topLocInfo = locmap.getLocaleInfo(topLoc);

								var topLocRow = document.createElement("div");
								topLocRow.className = "topLocaleRow";

								var topLocDiv = document.createElement("div");
								topLocDiv.className = "topLocale";
								appendLocaleLink(topLocDiv, topLoc, topLocInfo);

								var topLocList = document.createElement("div");
								topLocList.className = "subLocaleList";

								addSubLocales(topLocList, topLocInfo);

								topLocRow.appendChild(topLocDiv);
								topLocRow.appendChild(topLocList);
								theDiv.appendChild(topLocRow);
							};

							addTopLocale("root");
							// top locales
							for (var n in locmap.locmap.topLocales) {
								var topLoc = locmap.locmap.topLocales[n];
								addTopLocale(topLoc);
							}
							$('#locale-list').html(theDiv.innerHTML);

							if (cldrStatus.isVisitor())
								$('#show-read').prop('checked', true);
							//tooltip locale
							$('a.locName').tooltip();

							filterAllLocale();
							//end of adding the locale data

							updateCovFromJson(json);
							// setup coverage level
							window.surveyLevels = json.menus.levels;

							var titleCoverage = document.getElementById("title-coverage"); // coverage label

							var levelNums = []; // numeric levels
							for (var k in window.surveyLevels) {
								levelNums.push({
									num: parseInt(window.surveyLevels[k].level),
									level: window.surveyLevels[k]
								});
							}
							levelNums.sort(function(a, b) {
								return a.num - b.num;
							});

							var store = [];

							store.push({
								label: 'Auto',
								value: 'auto',
								title: cldrText.get('coverage_auto_desc')
							});

							store.push({
								type: "separator"
							});

							for (var j in levelNums) { // use given order
								if (levelNums[j].num == 0) continue; // none - skip
								if (levelNums[j].num < covValue('minimal')) continue; // don't bother showing these
								if (cldrStatus.getIsUnofficial() === false && levelNums[j].num == 101) continue; // hide Optional in production
								var level = levelNums[j].level;
								store.push({
									label: cldrText.get('coverage_' + level.name),
									value: level.name,
									title: cldrText.get('coverage_' + level.name + '_desc')
								});
							}
							//coverage menu
							var patternCoverage = $('#title-coverage .dropdown-menu');
							if (store[0].value) {
								$('#coverage-info').text(store[0].label);
							}
							for (var index = 0; index < store.length; ++index) {
								var data = store[index];
								if (data.value) {
									var html = '<li><a class="coverage-list" data-value="' + data.value + '"href="#">' + data.label + '</a></li>';
									patternCoverage.append(html);
								}
							}
							patternCoverage.find('li a').click(function(event) {
								event.stopPropagation();
								event.preventDefault();
								var newValue = $(this).data('value');
								var setUserCovTo = null;
								if (newValue == 'auto') {
									setUserCovTo = null; // auto
								} else {
									setUserCovTo = newValue;
								}
								if (setUserCovTo === window.surveyUserCov) {
									console.log('No change in user cov: ' + setUserCovTo);
								} else {
									window.surveyUserCov = setUserCovTo;
									var updurl = cldrStatus.getContextPath() + "/SurveyAjax?what=pref&_=" + theLocale + "&pref=p_covlev&_v=" + window.surveyUserCov + "&s=" + cldrStatus.getSessionId() + cacheKill(); // SurveyMain.PREF_COVLEV
									myLoad(updurl, "updating covlev to  " + surveyUserCov, function(json) {
										if (!verifyJson(json, 'pref')) {
											return;
										} else {
											unpackMenuSideBar(json);
											if (cldrStatus.getCurrentSpecial() && isReport(cldrStatus.getCurrentSpecial()))
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
								if (!isDashboard())
									refreshCounterVetting();
								return false;
							});

							/**
							 * Automatically import old winning votes
							 *
							 * Requires dijitDialog; we already have "dijit/Dialog" in require() statement at start of showV
							 */
							function doAutoImport() {
								'use strict';
								var autoImportProgressDialog = new dijitDialog({
									title: cldrText.get("v_oldvote_auto_msg"),
									content: cldrText.get("v_oldvote_auto_progress_msg")
								});
								autoImportProgressDialog.show();
								window.haveDialog = true;
								hideOverlayAndSidebar();
								/*
								 * See WHAT_AUTO_IMPORT = "auto_import" in SurveyAjax.java
								 */
								var url = cldrStatus.getContextPath() + "/SurveyAjax?what=auto_import&s=" + cldrStatus.getSessionId() + cacheKill();
								myLoad(url, "auto-importing votes", function(json) {
									autoImportProgressDialog.hide();
									window.haveDialog = false;
									if (json.autoImportedOldWinningVotes) {
										var vals = {
											count: json.autoImportedOldWinningVotes
										};
										var autoImportedDialog = new dijitDialog({
											title: cldrText.get("v_oldvote_auto_msg"),
											content: cldrText.sub("v_oldvote_auto_desc_msg", vals)
										});
										autoImportedDialog.addChild(new dijitButton({
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
							dojoTopic.subscribe("/dojo/hashchange", function(changedHash) {
								var oldLocale = trimNull(cldrStatus.getCurrentLocale());
								var oldSpecial = trimNull(cldrStatus.getCurrentSpecial());
								var oldPage = trimNull(cldrStatus.getCurrentPage());
								var oldId = trimNull(cldrStatus.getCurrentId());

								window.parseHash(changedHash);

								cldrStatus.setCurrentId(trimNull(cldrStatus.getCurrentId()));

								// did anything change?
								if (oldLocale != trimNull(cldrStatus.getCurrentLocale()) ||
									oldSpecial != trimNull(cldrStatus.getCurrentSpecial()) ||
									oldPage != trimNull(cldrStatus.getCurrentPage())) {
									console.log("# hash changed, (loc, etc) reloadingV..");
									reloadV();
								} else if (oldId != cldrStatus.getCurrentId() && cldrStatus.getCurrentId() != '') {
									console.log("# just ID changed, to " + cldrStatus.getCurrentId());
									// surveyCurrentID and the hash have already changed.
									// just call showInPop if the item is present. If not present, make sure it's visible.
									window.showCurrentId();
								}
							});
						}
					}); // end myLoad
				} // end getInitialMenusEtc
		}); // end require

	// replacement for dojo/dom-construct domConstruct.toDom
	function cldrDomConstruct(html) {
		const renderer = document.createElement('template');
		renderer.innerHTML = html;
		return renderer.content;
	}

} // end showV

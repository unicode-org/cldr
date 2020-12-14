'use strict';

/**
 * Based on forum_participation.js
 *
 * TODO: use modern JavaScript -- import/export, not define
 * TODO: avoid dependencies on legacy code
 * TODO: put code for progress bar, etc., in a shared module, share with, e.g., vsummary.js
 */
define("js/special/vetting_participation.js", ["js/special/SpecialPage.js"], function(SpecialPage) {
	var _super;
	
	'use strict';


	function Page() {
		// constructor
	}
	
	/**
	 * Fetch the Vetting Participation data from the server, and "load" it
	 *
	 * @param params an object with various properties; see SpecialPage.js
	 */
	function show(params) {
		/*
		 * Set up the 'right sidebar'; cf. vetting_participationGuidance
		 */
		showInPop2(stui.str(params.name + "Guidance"), null, null, null, true);

		const url = getVettingParticipationUrl();
		const errorHandler = function(err) {
			const responseText = cldrStAjax.errResponseText(err);
			params.special.showError(params, null, {err: err, what: "Loading vetting participation data" + responseText});
		};
		
		/**
		 * Calculate the top level data,
	     * returning localeToData, totalCount, uidToUser
		 */
		function calculateData(json) {
			// first, collect coverage
	        const {participation, users, languagesMissing} = json;

	        const localeToData = {};
	        let totalCount = 0;
	        function getLocale(loc) {
	            const e = localeToData[loc] = localeToData[loc] || {
	                vetters: [],
	                count: 0,
	                participation: {}
	            };
	            return e;
	        }
	        const uidToUser = {};
	        // collect users w/ coverage
	        users.forEach(u => {
	            const {locales, id} = u;
	            uidToUser[id] = u;
	            (locales||[]).forEach(loc => getLocale(loc).vetters.push(id));
	        });
	        // collect missing
	        (languagesMissing||[]).forEach(loc => (getLocale(loc).missing = true));
	        participation.forEach(({count, locale, user}) => {
	            const e = getLocale(locale);
	            e.count += count;
	            totalCount += count;
	            e.participation[user] = count;
	        });

			return {localeToData, totalCount, uidToUser};
		}
		
		/**
		 * @param {Number[]} e.vetters - vetter ID int array
	     * @param {Object} e.participation  - map of int (id) to count
         * @param {Object} uidToUser - map from ID to user
		 */
		function getUsersFor(e, uidToUser) {
            // collect all users
            const myUids = new Set(e.vetters.map(v => Number(v)));
            const vetterSet = new Set(e.vetters.map(v => Number(v)));
            for(const [id, p] of Object.entries(e.participation)) {
                myUids.add(Number(id));
            }
            const myUsers = Array.from(myUids.values())
                .map(id => ({
                    user: uidToUser[id],
                    isVetter: (vetterSet.has(id)),
                    count: e.participation[id]
                }))
                .sort((a,b) => a.user.name.localeCompare(b.user.name));
			return myUsers;
		}
		
		/**
		 * Main function. Calculate data, then output.
		 */
		const loadHandler = function(json) {
			if (json.err) {
				if (params.special) {
					params.special.showError(params, json, {what: "Loading vetting participation data"});
				}
				return;
			}
	        const nf = new Intl.NumberFormat();
	        // console.dir(o);
	        const {missingLocalesForOrg, languagesNotInCLDR} = json;

			// crunch the numbers
			const {localeToData, totalCount, uidToUser} = calculateData(json);

			const ourDiv = document.createElement("div");
			ourDiv.id = 'vettingParticipation';
			const div = $(ourDiv);

			// Front matter
	        div.append($('<h3>Locales and Vetting Participation</h3>'));
	        div.append($('<p/>', {
	            text: `Total votes: ${nf.format(totalCount||0)}`
	        }));
	        if(missingLocalesForOrg) {
	            div.append($('<i/>', {
	                text: `“No Coverage” locales indicate that there are no regular vetters assigned in the “${missingLocalesForOrg}” organization.`
	            }));
				if(languagesNotInCLDR) {
					div.append($('<h4/>', {
						text: 'Locales not in CLDR'
					}));
					div.append($('<i/>', {
						text: `These locales are specified by Locales.txt for ${missingLocalesForOrg}, but do not exist in CLDR yet:`
					}));
					for(const loc of languagesNotInCLDR) {
						div.append($('<tt/>', {
							class: 'fallback_code missingLoc',
							// Note: can't use locmap to get a translation here, because locmap only
							// has extant CLDR locales, and by definition 'loc' is not in CLDR yet.
							text: `${loc}` // raw code
						}));
					}
				}
			}
			
			// Chapter 1
			
	        const localeList = div.append($('<div class="locList" ></div>'));
	        // console.dir(localeToData);
	        for(const loc of Object.keys(localeToData).sort()) {
	            const e = localeToData[loc]; // consistency
	            const li = $('<div class="locRow"></div>');
	            localeList.append(li);
	            const locLabel = $(`<div class='locId'></div>`);
	            locLabel.append($('<a></a>', {
	                text: locmap.getLocaleName(loc),
	                href: linkToLocale(loc)
	            }));
	            li.append(locLabel);
	            if(e.count) {
	                locLabel.append($(`<i/>`, 
	                {
	                    text: nf.format(e.count),
	                    class: 'count',
	                    title: 'number of votes for this locale'
	                }));
	            } else {
	                locLabel.append($(`<i/>`, 
	                {
	                    text: nf.format(e.count || 0),
	                    class: 'count missingLoc',
	                    title: 'number of votes for this locale'
	                }));
	            }
	            if(e.missing) {
	                locLabel.append($(`<i/>`, {
	                    class: 'missingLoc',
	                    text: '(No Coverage)',
	                    title: `No regular vetters for ${missingLocalesForOrg}`
	                }));
	            }
	
				const myUsers = getUsersFor(e, uidToUser);
	            const theUserBox = $('<span/>', {class: 'participatingUsers'});
	            li.append(theUserBox);
	            myUsers.forEach(({user, isVetter, count}) => {
	                const theU = $('<span class="participatingUser"></span>');
	                // TODO: use regular user widget?
					theU.append($(createUser(user)));
//	                theU.append($('<span></span>', {text: user.name,
//	                                                class: 'userInfo',
//	                                                title: user.email}));
	                if(user.allLocales) {
	                    theU.addClass('allLocales');
	                    theU.append($('<span/>', {text: '*', title: 'user can vote for all locales'}));
	                }
	                if(isVetter) {
	                    theU.addClass('vetter');
	                }
	                if(!count) {
	                    theU.addClass('noparticip');
	                }
	                theU.append($('<span/>',
	                    {
	                        class: (count?'count':'count noparticip'),
	                        text: nf.format(count||0),
	                        title: 'number of this user’s votes'
	                    }));
	                theUserBox.append(theU);
	            });
	        }

			// Epilogue
			
			// No longer loading
			hideLoader(null);
			params.flipper.flipTo(params.pages.other, ourDiv);
		};
		const xhrArgs = {
			url: url,
			handleAs: 'json',
			load: loadHandler,
			error: errorHandler
		};
		cldrStAjax.sendXhr(xhrArgs);
	}

	/**
	 * Get the URL to use for loading the Vetting Participation page
	 */
	function getVettingParticipationUrl() {
		const sessionId = cldrStatus.getSessionId();
		if (!sessionId) {
			console.log('Error: sessionId falsy in getVettingParticipationUrl');
			return '';
		}
		return 'SurveyAjax?what=vetting_participation&s=' + sessionId;
	}
	
	// set up the inheritance before defining other functions
	_super = Page.prototype = new SpecialPage();

	Page.prototype.show = show;

	return Page;
});

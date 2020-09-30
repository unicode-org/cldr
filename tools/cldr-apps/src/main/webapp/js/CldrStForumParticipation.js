'use strict';

/**
 * cldrStForumParticipation: encapsulate Survey Tool Forum Participation code.
 *
 * Use an IIFE pattern to create a namespace for the public functions,
 * and to hide everything else, minimizing global scope pollution.
 * Ideally this should be a module (in the sense of using import/export),
 * but not all Survey Tool JavaScript code is capable yet of being in modules
 * and running in strict mode.
 *
 * Dependencies: surveyUser.id; surveySessionId; stui.str; SpecialPage; hideLoader; showInPop2
 */
const cldrStForumParticipation = (function() {
	const tableId = "participationTable";
	const fileName = "participation.csv";
	const onclick = "cldrStCsvFromTable.downloadCsv("
		+ "\"" + tableId + "\""
		+ ", "
		+ "\"" + fileName + "\""
		+ ")";

	/**
	 * Fetch the Forum Participation data from the server, and "load" it
	 *
	 * @param params an object with various properties; see SpecialPage.js
	 */
	function load(params) {
		/*
		 * Set up the 'right sidebar'; cf. forum_participationGuidance
		 */
		showInPop2(stui.str(params.name + "Guidance"), null, null, null, true);

		const userId = (surveyUser && surveyUser.id) ? surveyUser.id : 0;
		const url = getForumParticipationUrl();
		const errorHandler = function(err) {
			const responseText = cldrStAjax.errResponseText(err);
			params.special.showError(params, null, {err: err, what: "Loading forum participation data" + responseText});
		};
		const loadHandler = function(json) {
			if (json.err) {
				if (params.special) {
					params.special.showError(params, json, {what: "Loading forum participation data"});
				}
				return;
			}
			const html = makeHtmlFromJson(json);
			const ourDiv = document.createElement("div");
			ourDiv.innerHTML = html;

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
	 * Get the URL to use for loading the Forum Participation page
	 */
	function getForumParticipationUrl() {
		if (typeof surveySessionId === 'undefined') {
			console.log('Error: surveySessionId undefined in getForumParticipationUrl');
			return '';
		}
		return 'SurveyAjax?what=forum_participation&s=' + surveySessionId;
	}

	/**
	 * Make the html, given the json for Forum Participation
	 * 
	 * @param json
	 * @return the html 
	 */
	function makeHtmlFromJson(json) {
		let html = '<div>\n';
		if (json.org) {
			html += '<h4>Organization: ' + json.org + '</h4>\n';
		}
		if (json.headers && json.rows) {
			html += "<h4><a onclick='" + onclick + "'>Download CSV</a></h4>\n";
			html += "<table border='1' id='" + tableId + "'>\n";
			html += "<tr>\n";
			for (let header of json.headers) {
				html += "<th>" + header + "</th>\n";
			}
			html += "</tr>\n";
			for (let row of json.rows) {
				html += "<tr>\n";
				for (let cell of row) {
					html += "<td>" + cell + "</td>\n";
				}
				html += "</tr>\n";
			}
			html += "</table>\n";
		}
		html += '</div>';
		return html;
	}

	/*
	 * Make only these functions accessible from other files
	 */
	return {
		load: load,
		/*
		 * The following are meant to be accessible for unit testing only:
		 */
		test: {
			makeHtmlFromJson: makeHtmlFromJson,
		}
	};
})();

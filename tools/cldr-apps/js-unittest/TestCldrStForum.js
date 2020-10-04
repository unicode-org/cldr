'use strict';

{
	const assert = chai.assert;

	describe('cldrStForum.parseContent', function() {
		let json = null;

		it('should get json', function() {
			/*
			 * forumJson has been defined in forum_fr.js
			 */
			json = forumJson;
			assert(json != null);
		});

		it('should return correct result for this json', function() {
			this.timeout(5000);

			const posts = json.ret;
			assert(posts != null, "posts is not null");

			cldrStForum.test.setDisplayUtc(true); // so test can pass regardless of time zone

			const content = cldrStForum.parseContent(posts, 'info');
			assert(content != null, "content is not null");

			assert.equal(content.childElementCount, 1065, "content has 1065 children");

			assert(content.firstChild != null, "first child is not null");
			assert.equal(content.firstChild.id, "fthr_fr_CA|45347");
			const s1 = "n (Gaeilge) userlevel_tc[v38] 2020-02-06 17:26 UTCClosedClosetest"
				+ "n (Gaeilge) userlevel_tc[v38] 2020-02-06 17:28 UTCClosetest reply blah!";
			assert.equal(normalizeWhitespace(content.firstChild.textContent), normalizeWhitespace(s1));

			const firstSibling = content.firstChild.nextSibling;
			assert(firstSibling != null, "first sibling is not null");
			assert.equal(firstSibling.id, "fthr_fr_CA|45346");
			const s2 = "n (Gaeilge) userlevel_tc[v38] 2020-02-06 17:20 UTCClosedCloseFUrthermore";
			assert.equal(normalizeWhitespace(firstSibling.textContent), normalizeWhitespace(s2));

			const secondSibling = firstSibling.nextSibling;
			assert(secondSibling != null, "second sibling is not null");
			assert.equal(secondSibling.id, "fthr_fr_CA|45343");
			const s3 = "n (Gaeilge) userlevel_tc[v38] 2020-02-06 17:17 UTCClosedClosetest"
				+ "n (Gaeilge) userlevel_tc[v38] 2020-02-06 17:18 UTCCloseOK, replying to test in Dashboard"
				+ "n (Gaeilge) userlevel_tc[v38] 2020-02-06 17:19 UTCCloseAnd replying to reply";
			assert.equal(normalizeWhitespace(secondSibling.textContent), normalizeWhitespace(s3));

			const thirdSibling = secondSibling.nextSibling;
			assert(thirdSibling != null, "third sibling is not null");
			assert.equal(thirdSibling.id, "fthr_fr_CA|45341");
			const s4 = "n (Gaeilge) userlevel_tc[v38] 2020-02-06 17:12 UTCClosedCloseAnd another post from Dashboard."
				+ "n (Gaeilge) userlevel_tc[v38] 2020-02-06 17:14 UTCCloseAnd another reply, this time from Dashboard Fix pop-up!!!";
			assert.equal(normalizeWhitespace(thirdSibling.textContent), normalizeWhitespace(s4));

			assert(content.lastChild != null, "last child is not null");
			assert.equal(content.lastChild.id, "fthr_fr|363");
			const sLast = "n (Microsoft) userlevel_vetter[v28] 2015-05-19 15:58 UTCClosedClose...";
			assert.equal(normalizeWhitespace(content.lastChild.textContent), normalizeWhitespace(sLast));
		});
	});

	describe('cldrStForum.getForumSummaryHtml', function() {
		const html = cldrStForum.getForumSummaryHtml('aa', 1, true);

		it('should not return null or empty', function() {
			assert((html != null && html !== ''), "html is neither null nor empty");
		});

		const domParser = new DOMParser();
		const serializer = new XMLSerializer();
		const xmlDoc = domParser.parseFromString(html, 'application/xml');
		const xmlStr = serializer.serializeToString(xmlDoc);

		it('should return valid xml', function() {
			assert(xmlStr.indexOf('error') === -1, 'xml does not contain error'); // as in '... parsererror ...'
		});

		const htmlDoc = domParser.parseFromString(html, 'text/html');
		const htmlStr = serializer.serializeToString(xmlDoc);

		it('should return good html', function() {
			assert(htmlStr.indexOf('error') === -1, 'html does not contain error'); // as in '... parsererror ...'
		});

		it('should contain angle brackets', function() {
			assert((htmlStr.indexOf('<') !== -1) && (htmlStr.indexOf('>') !== -1), 'does contain angle brackets');
		});
	});

	/**
	 * Remove any leading or trailing whitespace, and replace any sequence of whitespace with a single space
	 */
	function normalizeWhitespace(s) {
		return s.replace(/^(\s*)|(\s*)$/g, '').replace(/\s+/g, ' ');
	}

	/**
	 * Does the given string parse OK with the given mime type?
	 */
	function markupParsesOk(inputString, mimeType) {
		const doc = new DOMParser().parseFromString(inputString, mimeType);
		if (!doc) {
			console.log('no doc for ' + mimeType + ', ' + inputString);
			return false;
		}
		const outputString = new XMLSerializer().serializeToString(doc);
		if (!outputString) {
			console.log('no output string for ' + mimeType + ', ' + inputString);
			return false;
		}
		if (outputString.indexOf('error') !== -1) {
			console.log('parser error for ' + mimeType + ', ' + inputString + ', ' + outputString);
			return false;
		}
		return true;
	}
}

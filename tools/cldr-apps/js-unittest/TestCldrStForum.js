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

			const content = cldrStForum.parseContent(posts, 'info');

			assert(content != null, "content is not null");

			assert.equal(content.childElementCount, 1065, "content has 1065 children");

			assert(content.firstChild != null, "first child is not null");

			assert.equal(content.firstChild.id, "fthr_fr_CA|45347");

			const s = "n "
				+ "(Gaeilge) userlevel_tc[v38] 2020-02-06 12:26Reviewtest【Closed】forum_replyn "
				+ "(Gaeilge) userlevel_tc[v38] 2020-02-06 12:28Re: Reviewtest reply blah!【Closed】forum_reply";

			assert.equal(normalizeWhitespace(s), normalizeWhitespace(content.firstChild.textContent));
		});
	});

	describe('cldrStForum.getForumSummaryHtml', function() {
		const html = cldrStForum.getForumSummaryHtml();

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

	describe('cldrStForum.test.postStatusMenu', function() {
		const html = cldrStForum.test.postStatusMenu();

		it('should not return null or empty', function() {
			assert((html != null && html !== ''), "html is neither null nor empty");
		});

		it('should return good html', function() {
			assert(markupParsesOk(html, 'text/html'), 'parses OK as text/html');
		});

		// not xml: status menu uses "<select required>". Could make it "<select required='required'>".

		it('should contain angle brackets', function() {
			assert((html.indexOf('<') !== -1) && (html.indexOf('>') !== -1), 'does contain angle brackets');
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

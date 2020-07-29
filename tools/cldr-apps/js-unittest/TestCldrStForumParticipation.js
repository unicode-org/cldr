'use strict';

{
	const assert = chai.assert;

	describe('cldrStForumParticipation.makeHtmlFromJson', function() {
		/*
		 * forumParticipationJson has been defined in forum_participation_json.js
		 */
		const json = forumParticipationJson;

		it('should get json', function() {
			assert(json != null);
		});

		const html = cldrStForumParticipation.test.makeHtmlFromJson(json);

		it('should not return null or empty', function() {
			assert((html != null && html !== ''), "html is neither null nor empty");
		});

		const domParser = new DOMParser();
		const serializer = new XMLSerializer();
		const xmlDoc = domParser.parseFromString(html, 'application/xml');
		const xmlStr = serializer.serializeToString(xmlDoc);

		it('should return valid xml', function() {
			assert(xmlStr.indexOf('error') === -1, 'xml does not contain error: ' + xmlStr); // as in '... parsererror ...'
		});

		const htmlDoc = domParser.parseFromString(html, 'text/html');
		const htmlStr = serializer.serializeToString(xmlDoc);

		it('should return good html', function() {
			assert(htmlStr.indexOf('error') === -1, 'html does not contain error: ' + htmlStr); // as in '... parsererror ...'
		});

		it('should contain angle brackets', function() {
			assert((htmlStr.indexOf('<') !== -1) && (htmlStr.indexOf('>') !== -1), 'does contain angle brackets: ' + htmlStr);
		});
	});
}

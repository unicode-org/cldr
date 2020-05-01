'use strict';

var assert = chai.assert;

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
	
	/**
	 * Remove any leading or trailing whitespace, and replace any sequence of whitespace with a single space
	 */
	function normalizeWhitespace(s) {
		return s.replace(/^(\s*)|(\s*)$/g, '').replace(/\s+/g, ' ');
	}
});

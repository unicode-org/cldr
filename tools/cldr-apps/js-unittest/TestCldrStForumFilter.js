'use strict';

{
	const assert = chai.assert;

	describe('cldrStForumFilter.createMenu', function() {
		it('should not return null', function() {
			cldrStForumFilter.setUserId(1);
			const actualOutput = cldrStForumFilter.createMenu(null);
			assert(actualOutput != null, "not null");
		});
	});

	describe('cldrStForumFilter.getFilteredThreadIds', function() {
		it('should return one thread for two related posts', function() {
			const posts = [
				{id: 1, threadId: 't1', parent: -1, poster: 100},
				{id: 2, threadId: 't1', parent: 1,  poster: 200},
			];
			const actualOutput = cldrStForumFilter.getFilteredThreadIds(posts, false);
			const expectedOutput = ['t1'];
			assert.deepEqual(actualOutput, expectedOutput);
		});
		it('should return one thread for three related posts', function() {
			const posts = [
				{id: 1, threadId: 't1', parent: -1, poster: 100},
				{id: 2, threadId: 't1', parent: 1,  poster: 200},
				{id: 3, threadId: 't1', parent: 2,  poster: 300},
			];
			const actualOutput = cldrStForumFilter.getFilteredThreadIds(posts, false);
			const expectedOutput = ['t1'];
			assert.deepEqual(actualOutput, expectedOutput);
		});
		it('should return two threads for two unrelated posts', function() {
			const posts = [
				{id: 1, threadId: 't1', parent: -1, poster: 100},
				{id: 2, threadId: 't2', parent: -1, poster: 200},
			];
			const actualOutput = cldrStForumFilter.getFilteredThreadIds(posts, false);
			const expectedOutput = ['t1', 't2'];
			assert.deepEqual(actualOutput, expectedOutput);
		});
	});
}

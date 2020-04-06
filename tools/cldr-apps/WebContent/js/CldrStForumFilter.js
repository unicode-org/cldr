'use strict';

/**
 * cldrStForumFilter: encapsulate filtering of forum threads.
 *
 * Use an IIFE pattern to create a namespace for the public functions,
 * and to hide everything else, minimizing global scope pollution.
 * Ideally cldrStForumFilter should be a module (in the sense of using import/export),
 * but not all Survey Tool JavaScript code is capable yet of being in modules
 * and running in strict mode.
 */
const cldrStForumFilter = (function() {

	const filters = [
		{name: 'All threads', func: passAll},
		{name: 'Threads you have posted to', func: youDidPost},
		{name: 'Threads you have NOT posted to', func: youDidNotPost},
	];

	var filterIndex = 0;
	
	var filterUserId = 0;

	function createMenu(userId) {
		filterUserId = userId;
		let select = document.createElement('select');
		select.id = 'forumFilterMenu';
		for (let i = 0; i < filters.length; i++) {
			let item = document.createElement('option');
			item.setAttribute('value', i);
			if (i === filterIndex) {
				item.setAttribute('selected', 'selected');
			}
			item.appendChild(document.createTextNode(filters[i].name));
			select.appendChild(item);
		}
		select.addEventListener('change', function() {
			let i = parseInt(select.value, 10);
			if (i !== filterIndex) {
				filterIndex = i;
				reloadV(); // TODO: this works, but it's an ugly dependency. Caller should specify the function as a callback?
			}
		});
		return select;
	}

	/**
	 * Filter the threads and assemble them into a new document fragment
	 *
	 * @param posts the array of post objects, from oldest to newest
	 * @param topicDivs the array of thread elements, indexed by threadId
	 * @return the new document fragment
	 */
	function assembleThreads(posts, topicDivs) {

		let filtered = {};
		Object.keys(topicDivs).forEach(function(threadId) {
			filtered[threadId] = true; // TODO
			// cldrStForumFilter.passes(post, topicDiv)
		});

		let newForumDiv = document.createDocumentFragment();

		for (let num = posts.length - 1; num >= 0; num--) {
			let post = posts[num];
			if (post.parent < 0 && post.threadId in filtered) {
				let topicDiv = topicDivs[post.threadId];
				newForumDiv.insertBefore(topicDiv, newForumDiv.firstChild);
			}
		}
		return newForumDiv;
	}

	function passes(post, topicDiv) {
		if (filters[filterIndex].func(post, topicDiv)) {
			console.log("cldrStForumFilter.passes true: " + filterIndex + ", " + post + ", " + topicDiv);
			return true;
		} else {
			console.log("cldrStForumFilter.passes false: " + filterIndex + ", " + post + ", " + topicDiv);
			return false;			
		}
	}

	function passAll(post, topicDiv) {
		return true;
	}

	function youDidPost(post, topicDiv) {
		if (post.posterInfo.id === filterUserId) {
			return true;
		}
		return false;
	}

	function youDidNotPost(post, topicDiv) {
		if (post.posterInfo.id !== filterUserId) {
			return true;
		}
		return false;
	}

	/*
	 * Make only these functions accessible from other files:
	 */
	return {
		createMenu: createMenu,
		assembleThreads: assembleThreads,
		passes: passes,
	};
})();

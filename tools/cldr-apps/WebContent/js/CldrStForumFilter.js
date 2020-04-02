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
		passes: passes,
	};
})();

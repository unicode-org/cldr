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

	/**
	 * An array of filter objects, each having a name and a boolean function
	 */
	const filters = [
		{name: 'All threads', func: passAll},
		{name: 'Open threads', func: passIfOpen},
		{name: 'Closed threads', func: passIfClosed},
		{name: 'Threads you have posted to', func: passIfYouPosted},
		{name: 'Threads you have NOT posted to', func: passIfYouDidNotPost},
		{name: 'Open threads you have not posted to', func: passIfOpenAndYouDidNotPost},
	];

	/**
	 * The index of the current filter in the "filters" array
	 */
	var filterIndex = 0;
	
	/**
	 * The id of the current user
	 */
	var filterUserId = 0;

	/**
	 * A function to call whenever a different filter is selected
	 */
	var filterReload = null;

	/**
	 * Get a popup menu from which the user can choose a filter, and set the
	 * user id and reload function
	 *
	 * @param userId the id of the current user, for setting filterUserId
	 * @param reloadFunction the reload function, for setting filterReload
	 * @return the select element containing the menu
	 */
	function createMenu(userId, reloadFunction) {
		filterUserId = userId;
		filterReload = reloadFunction;
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
				if (filterReload) {
					filterReload();
				}
			}
		});
		return select;
	}

	/**
	 * Get an array of all the threadId strings for threads that pass the current filter
	 *
	 * Assume each post has post.threadId.
	 *
	 * @param posts the array of post objects, from newest to oldest
	 * @param applyFilter true if the currently menu-selected filter should be applied
	 * @return the filtered array of threadId strings
	 */
	function getFilteredThreadIds(posts, applyFilter) {
		const threadsToPosts = getThreadsToPosts(posts);

		let filteredArray = [];
		Object.keys(threadsToPosts).forEach(function(threadId) {
			if (!applyFilter || threadPasses(threadsToPosts[threadId])) {
				filteredArray.push(threadId);
			}
		});
		return filteredArray;
	}

	/**
	 * Get an object mapping each threadId to an array of all the posts in that thread
	 *
	 * @param posts the array of post objects, from newest to oldest
	 * @return the mapping object
	 */
	function getThreadsToPosts(posts) {
		let threadsToPosts = {};
		posts.forEach(function(post) {
			let threadId = post.threadId;
			if (!(threadId in threadsToPosts)) {
				threadsToPosts[threadId] = [];
			}
			threadsToPosts[threadId].push(post);
		});
		return threadsToPosts;
	}

	/**
	 * Does the thread with the given array of posts pass the current filter?
	 *
	 * @param threadPosts the array of posts in the thread
	 * @return true or false
	 */
	function threadPasses(threadPosts) {
		return filters[filterIndex].func(threadPosts);
	}

	/**
	 * Pass all threads
	 *
	 * @param threadPosts the array of posts in the thread (unused)
	 * @return true
	 */
	function passAll(threadPosts) {
		return true;
	}

	/**
	 * Does the thread with the given array of posts include at least one post by the current user?
	 *
	 * Assume each post has post.poster.
	 * (Some but not all posts also have post.posterInfo.id; if so, it's equal to post.poster.)
	 *
	 * @param threadPosts the array of posts in the thread
	 * @return true or false
	 */
	function passIfYouPosted(threadPosts) {
		return threadPosts.some(post => post.poster && (post.poster === filterUserId));
	}

	/**
	 * Does the thread with the given array of posts include no posts by the current user?
	 *
	 * @param threadPosts the array of posts in the thread
	 * @return true or false
	 */
	function passIfYouDidNotPost(threadPosts) {
		return !passIfYouPosted(threadPosts);
	}

	/**
	 * Is the thread with the given array of posts open?
	 *
	 * @param threadPosts the array of posts in the thread
	 * @return true or false
	 */
	function passIfOpen(threadPosts) {
		return !passIfClosed(threadPosts);
	}

	/**
	 * Is the thread with the given array of posts closed?
	 *
	 * @param threadPosts the array of posts in the thread
	 * @return true or false
	 */
	function passIfClosed(threadPosts) {
		return threadPosts.some(post => post.forumStatus && (post.forumStatus === 'Closed'));
	}

	/**
	 * Is the thread with the given array of posts open and does it include no posts by the current user?
	 *
	 * @param threadPosts the array of posts in the thread
	 * @return true or false
	 */
	function passIfOpenAndYouDidNotPost(threadPosts) {
		return passIfYouDidNotPost(threadPosts) && passIfOpen(threadPosts);
	}

	/*
	 * Make only these functions accessible from other files
	 */
	return {
		createMenu: createMenu,
		getFilteredThreadIds: getFilteredThreadIds,
	};
})();

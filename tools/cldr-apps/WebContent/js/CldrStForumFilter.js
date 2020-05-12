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
	 * The index of 'Open threads' in the filters array
	 */
	const OPEN_THREADS_INDEX = 0;

	/**
	 * An array of filter objects, each having a name and a boolean function
	 */
	const filters = [
		{name: 'Open threads', func: passIfOpen, keepCount: true},
		{name: 'Your open threads', func: passIfOpenAndYouStarted, keepCount: true},
		{name: 'Open threads you have not posted to', func: passIfOpenAndYouDidNotPost, keepCount: true},

		{name: 'All threads', func: passAll, keepCount: false},
		{name: 'Closed threads', func: passIfClosed, keepCount: false},
		{name: 'Threads you have posted to', func: passIfYouPosted, keepCount: false},
		{name: 'Threads you have NOT posted to', func: passIfYouDidNotPost, keepCount: false},
	];

	/**
	 * The index of the current filter in the "filters" array
	 */
	let filterIndex = 0;

	/**
	 * The id of the current user
	 */
	let filterUserId = 0;

	/**
	 * A function to call whenever a different filter is selected
	 */
	let filterReload = null;

	/**
	 * An object mapping filter names to the counts of threads passing those filters
	 */
	let filterCounts = {};

	/**
	 * Set the user id (the "you" in "you posted")
	 *
	 * @param userId the id of the current user
	 */
	function setUserId(userId) {
		filterUserId = userId;
	}

	/**
	 * Get a popup menu from which the user can choose a filter, and set the reload function
	 *
	 * @param reloadFunction the reload function, for setting filterReload
	 * @return the select element containing the menu
	 */
	function createMenu(reloadFunction) {
		filterReload = reloadFunction;
		const select = document.createElement('select');
		select.id = 'forumFilterMenu';
		for (let i = 0; i < filters.length; i++) {
			const item = document.createElement('option');
			item.setAttribute('value', i);
			if (i === filterIndex) {
				item.setAttribute('selected', 'selected');
			}
			item.appendChild(document.createTextNode(filters[i].name));
			select.appendChild(item);
		}
		select.addEventListener('change', function() {
			const index = parseInt(select.value, 10);
			if (index !== filterIndex) {
				filterIndex = index;
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

		const filteredArray = [];
		Object.keys(threadsToPosts).forEach(function(threadId) {
			if (!applyFilter || threadPasses(threadsToPosts[threadId])) {
				filteredArray.push(threadId);
			}
		});
		if (applyFilter) {
			updateCounts(threadsToPosts, filteredArray.length);
		}
		return filteredArray;
	}

	/**
	 * Update the filterCounts map by calculating all the filter counts
	 * for filters with keepCount true
	 *
	 * @param threadsToPosts an object mapping each threadId to an array of all the posts in that thread
	 * @param countCurrentFilter the count for the current filter, already calculated
	 */
	function updateCounts(threadsToPosts, countCurrentFilter) {
		clearCounts();
		if (filters[filterIndex].keepCount) {
			filterCounts[filters[filterIndex].name] = countCurrentFilter;
		}
		Object.keys(threadsToPosts).forEach(function(threadId) {
			for (let i = 0; i < filters.length; i++) {
				if (filters[i].keepCount && i !== filterIndex) {
					if (threadPassesI(threadsToPosts[threadId], i)) {
						filterCounts[filters[i].name]++;
					}
				}
			}
		});
		simplifyCounts();
	}

	/**
	 * Initialize the filterCounts map by setting to zero all the filter counts
	 * for filters with keepCount true
	 */
	function clearCounts() {
		filters.forEach(function(filter) {
			if (filter.keepCount) {
				filterCounts[filter.name] = 0;
			}
		});
	}

	/**
	 * If the count for 'Open threads' is zero, simplify filterCounts, since then there is
	 * no need to display counts for 'Your open threads' or 'Open threads you have not posted to'
	 */
	function simplifyCounts() {
		if (filterCounts[filters[OPEN_THREADS_INDEX].name] === 0) {
			for (let i = 0; i < filters.length; i++) {
				if (i !== OPEN_THREADS_INDEX && filters[i].keepCount) {
					delete filterCounts[filters[i].name];
				}
			}
		}
	}

	/**
	 * Get an object mapping from certain filter names to the number
	 * of threads currently passing those filters
	 *
	 * This only works right if getFilteredThreadIds was called
	 */
	function getFilteredThreadCounts() {
		return filterCounts;
	}

	/**
	 * Get an object mapping each threadId to an array of all the posts in that thread
	 *
	 * @param posts the array of post objects, from newest to oldest
	 * @return the mapping object
	 */
	function getThreadsToPosts(posts) {
		const threadsToPosts = {};
		posts.forEach(function(post) {
			const threadId = post.threadId;
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
	 * Does the thread with the given array of posts pass the filter with the given index?
	 *
	 * @param threadPosts the array of posts in the thread
	 * @param i the index
	 * @return true or false
	 */
	function threadPassesI(threadPosts, i) {
		return filters[i].func(threadPosts);
	}

	/**************************/

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

	/**
	 * Is the thread with the given array of posts open and does it include no posts by the current user?
	 *
	 * @param threadPosts the array of posts in the thread
	 * @return true or false
	 */
	function passIfOpenAndYouDidNotPost(threadPosts) {
		return passIfYouDidNotPost(threadPosts) && passIfOpen(threadPosts);
	}

	/**
	 * Is the thread with the given array of posts open and was it started by the current user?
	 *
	 * @param threadPosts the array of posts in the thread
	 * @return true or false
	 */
	function passIfOpenAndYouStarted(threadPosts) {
		return passIfOpen(threadPosts) && passIfYouStarted(threadPosts);
	}

	/**
	 * Was the thread with the given array of posts started by the current user?
	 *
	 * @param threadPosts the array of posts in the thread
	 * @return true or false
	 */
	function passIfYouStarted(threadPosts) {
		/*
		 * The first (original) post in the thread is the last one in the array
		 */
		if (threadPosts.length < 1) {
			return false;
		}
		const post = threadPosts[threadPosts.length - 1];
		return post.poster === filterUserId;
	}

	/*
	 * Make only these functions accessible from other files
	 */
	return {
		setUserId: setUserId,
		createMenu: createMenu,
		getFilteredThreadIds: getFilteredThreadIds,
		getFilteredThreadCounts: getFilteredThreadCounts,
	};
})();

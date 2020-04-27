'use strict';

/**
 * cldrStForum: encapsulate main Survey Tool Forum code.
 *
 * Use an IIFE pattern to create a namespace for the public functions,
 * and to hide everything else, minimizing global scope pollution.
 * Ideally this should be a module (in the sense of using import/export),
 * but not all Survey Tool JavaScript code is capable yet of being in modules
 * and running in strict mode.
 *
 * Dependencies on external code:
 * window.surveyCurrentLocale, window.surveySessionId, window.surveyUser, window.locmap,
 * createGravitar, stui.str, listenFor, bootstrap.js, reloadV, ...
 *
 * TODO: possibly move these functions here from survey.js: showForumStuff, havePosts, updateInfoPanelForumPosts, appendForumStuff;
 * also some/all code from forum.js
 */
const cldrStForum = (function() {

	const FORUM_DEBUG = false;

	function forumDebug(s) {
		if (FORUM_DEBUG) {
			console.log(s);
		}
	}

	/**
	 * Did the form change? For callback, awkward onReplyClose + reloadV dependency.
	 * showPost makes it false; submitPost can make it true
	 */
	let formDidChange = false;

	/**
	 * Mapping from post id to post object, describing the set of
	 * posts in the most recently parsed json
	 */
	let postHash = {};

	/**
	 * If there are any existing posts concerning this xpath, open a Reply to an existing thread,
	 * otherwise open a new thread of posts concerning this xpath.
	 *
	 * Called only from review.js, for Dashboard only -- doesn't work outside dashboard due to dependencies on .data-review, etc.
	 *
	 * NOTE: this function uses JQuery (not Dojo) for ajax ($.get)
	 *
	 * Bootstrap.js post-modal: https://getbootstrap.com/docs/4.1/components/modal/
	 */
	function openPostFromDashboard() {
		var path = $(this).closest(".data-review").data('path');
		var choice = $(this).closest(".table-wrapper").data('type');
		var postModal = $('#post-modal');
		var locale = surveyCurrentLocale;
		var url = contextPath + "/SurveyAjax?what=forum_fetch&s=" + surveySessionId + "&xpath=" +
			$(this).closest('tr').data('path') + "&voteinfo&_=" + locale;

		showPost(postModal, null);

		$.get(url, function(data) {
			var posts = data.ret; // posts array, could be empty
			var content = '';
			content += '<form role="form" id="post-form">';
			content += '<div class="form-group">' +
				'<textarea name="text" class="form-control" placeholder="Write your post here"></textarea>' +
				'</div>\n';

			const isReply = (posts && posts.length > 0);
			const isOriginalPoster = userIsOriginalPoster(posts[0]);
			const userCanClose = canUserClose(isReply, isOriginalPoster);
			content += postStatusMenu(isReply, userCanClose, isOriginalPoster);

			/*
			 * This Submit button differs from the one in openPostOrReply() by having data-path and data-choice
			 */
			content += '<button data-path="' + path +
				'" data-choice="' + choice + '" class="btn btn-success submit-post btn-block">Submit</button>';

			content += '<input type="hidden" name="forum" value="true">';
			content += '<input type="hidden" name="_" value="' + surveyCurrentLocale + '">';
			content += '<input type="hidden" name="replyTo" value="-1">';
			content += '<input type="hidden" name="data-path" value="' + path + '">';
			content += '<input type="hidden" name="xpath" value="#' + path + '">'; // numeric
			content += '<label class="post-subj"><input name="subj" type="hidden" value="Review"></label>';
			content += '<input name="post" type="hidden" value="Post">';
			content += '<input name="isReview" type="hidden" value="1">';
			content += '</form>';

			content += '<div class="post"></div>';
			content += '<div class="forumDiv"></div>';

			postModal.find('.modal-body').html(content);

			if (posts) {
				const forumDiv = parseContent(posts, 'new');
				const postHolder = postModal.find('.modal-body').find('.forumDiv');
				postHolder[0].appendChild(forumDiv);
			}

			postModal.find('textarea').autosize();
			postModal.find('.submit-post').click(submitPost);
			setTimeout(function() {
				postModal.find('textarea').focus();
			}, 1000 /* one second */ );
		}, 'json');
	}

	/**
	 * Make a new forum post or a reply.
	 *
	 * This function (formerly named openReply) is used for the non-Dashboard "New Forum Post" button,
	 * which is not a reply. It is also used for Reply.
	 *
	 * Called from forum.js (new) and survey.js (reply)
	 *
	 * @param params the object containing various parameters: onReplyClose, locale, xpath, replyTo, replyData, ...
	 */
	function openPostOrReply(params) {
		const postModal = $('#post-modal');
		showPost(postModal, params.onReplyClose);

		const isReply = (params.replyTo && params.replyTo >= 0) ? true : false
		const replyTo = isReply ? params.replyTo : -1;
		const parentPost = (isReply && params.replyData) ? params.replyData : null;
		const isOriginalPoster = userIsOriginalPoster(parentPost);
		const userCanClose = canUserClose(isReply, isOriginalPoster);

		const xpath = params.xpath ? params.xpath : '';

		let content = '';

		content += '<form role="form" id="post-form">';
		content += '<div class="form-group">';
		content += '<div class="input-group"><span class="input-group-addon">Subject:</span>';
		content += '<input class="form-control" name="subj" type="text" value=""></div>';
		content += '<textarea name="text" class="form-control" placeholder="Write your post here"></textarea></div>';
		content += postStatusMenu(isReply, userCanClose, isOriginalPoster);
		content += '<button class="btn btn-success submit-post btn-block">Submit</button>';
		content += '<input type="hidden" name="forum" value="true">';
		content += '<input type="hidden" name="_" value="' + params.locale + '">';
		content += '<input type="hidden" name="xpath" value="' + xpath + '">';
		content += '<input type="hidden" name="replyTo" value="' + replyTo + '">';
		content += '</form>';

		content += '<div class="post"></div>';
		content += '<div class="forumDiv"></div>';

		postModal.find('.modal-body').html(content);

		let subject = '';
		if (isReply && parentPost) {
			subject = post2text(parentPost.subject);
			if (subject.substring(0, 3) != 'Re:') {
				subject = 'Re: ' + subject;
			}
		} else if (params.subject) {
			subject = params.subject;
		}
		postModal.find('input[name=subj]')[0].value = subject;

		if (parentPost) {
			const forumDiv = parseContent([parentPost], 'new');
			const postHolder = postModal.find('.modal-body').find('.forumDiv');
			postHolder[0].appendChild(forumDiv);
		}

		postModal.find('textarea').autosize();
		postModal.find('.submit-post').click(submitPost);
		setTimeout(function() {
			postModal.find('textarea').focus();
		}, 1000 /* one second */);
	}

	/**
	 * Show a modal window displaying a forum post
	 *
	 * @param postModal the bootstrap.js $('#post-modal') element
	 * @param onClose the callback function
	 *
	 * Called only by openPostFromDashboard and openPostOrReply, in this file
	 */
	function showPost(postModal, onClose) {
		formDidChange = false;
		// fire when the post window closes. Can reload posts, etc.
		postModal.on('hidden.bs.modal', function(e) {
			if (onClose) {
				let form = $('#post-form');
				let modal = $('#post-modal');
				onClose(modal, form, formDidChange);
			}
		});
		postModal.modal();
	}

	/**
	 * Get the html content for the Status menu
	 *
	 * @param isReply true if this post is a reply, else false
	 * @param userCanClose true if this user is allowed to close, else false
	 * @param isOriginalPoster true if the current user is the original poster in the thread
	 * @return the html
	 *
	 * Called only by openPostFromDashboard and openPostOrReply, in this file
	 *
	 * Compare SurveyForum.ForumStatus on server
	 */
	function postStatusMenu(isReply, userCanClose, isOriginalPoster) {
		let content = '<p id="forum-status-area">Status: ';

		content += '<select id="forum-status-menu" required>\n';
		content += '<option value="" disabled selected>Select one</option>\n';

		if (!isReply) {
			content += '<option value="Request">Request a change</option>\n';
		}
		content += '<option value="Question">Ask a question</option>\n';
		if (isReply && !isOriginalPoster) {
			content += '<option value="Agreed">Agree</option>\n';
			content += '<option value="Disputed">Disagree</option>\n';
		}
		if (userCanClose) {
			content += '<option value="Closed">Close</option>\n';
		}
		content += '</select></p>\n';
		return content;
	}

	/**
	 * Is the current user the original poster in the thread containing this post?
	 *
	 * @param post either this post or its parent, for getFirstPostInThread
	 * @returns true or false
	 */
	function userIsOriginalPoster(post) {
		if (!post) {
			return false;
		}
		if (typeof surveyUser !== 'undefined') {
			if (surveyUser === getFirstPostInThread(post).poster) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Is this user allowed to close the thread now?
	 *
	 * The user is only allowed if they are the original poster of the thread,
	 * or a TC (technical committee) member.
	 *
	 * @param isReply true if this post is a reply, else false
	 * @param isOriginalPoster true if the current user is the original poster in the thread
	 * @return true if this user is allowed to close, else false
	 */
	function canUserClose(isReply, isOriginalPoster) {
		return isReply && (isOriginalPoster || userIsTC());
	}

	/**
	 * Is the current user a TC (Technical Committee) member?
	 *
	 * @returns true or false
	 */
	function userIsTC() {
		if (typeof surveyUserPerms !== 'undefined' && surveyUserPerms.userIsTC) {
			return true;
		}
		return false;
	}

	/**
	 * Submit a forum post
	 *
	 * @param event
	 *
	 * Called by openPostFromDashboard and openPostOrReply, in this file
	 *
	 * NOTE: this function uses JQuery (not Dojo) for ajax
	 */
	function submitPost(event) {
		let forumStatus = document.getElementById('forum-status-menu').value;
		if (!forumStatus) {
			/*
			 * Normally this won't happen, since the menu has the attribute "required".
			 * The browser should prevent the form from being submitted, and it should ask
			 * the user to make a selection. If we do get here anyway, return silently.
			 */
			return;
		}
		var locale = surveyCurrentLocale;
		var url = contextPath + "/SurveyAjax";
		var form = $('#post-form');
		formDidChange = true;
		if ($('#post-form textarea[name=text]').val()) {
			$('#post-form button').fadeOut();
			$('#post-form .input-group').fadeOut(); // subject line
			$('#forum-status-area').fadeOut();
			var xpath = $('#post-form input[name=xpath]').val();
			var ajaxParams = {
				data: {
					s: surveySessionId,
					"_": surveyCurrentLocale,
					replyTo: $('#post-form input[name=replyTo]').val(),
					xpath: xpath,
					text: $('#post-form textarea[name=text]').val(),
					subj: $('#post-form input[name=subj]').val(), // "Review"
					forumStatus: forumStatus,
					what: "forum_post"
				},
				type: "POST",
				url: url,
				contentType: "application/x-www-form-urlencoded;",
				dataType: 'json',
				success: function(data) {
					let post = $('.post').first();
					if (data.err) {
						post.before("<p class='warn'>error: " + data.err + "</p>");
					} else if (data.ret && data.ret.length > 0) {
						const postModal = $('#post-modal');
						const postHolder = postModal.find('.modal-body').find('.post');
						const firstPostHolder = postHolder[0];
						const posts = data.ret;
						const forumDiv = parseContent(posts, 'new');
						firstPostHolder.insertBefore(forumDiv, firstPostHolder.firstChild);
						// reset
						post = $('.post').first();
						post.hide();
						post.show('highlight', {
							color: "#d9edf7"
						});
						$('#post-form textarea').val('');
						$('#post-form textarea').fadeOut();
						updateInfoPanelForumPosts(null);
					} else {
						post.before("<i>Your post was added, #" + data.postId + " but could not be shown.</i>");
					}
				},
				error: function(err) {
					var post = $('.post').first();
					post.before("<p class='warn'>error! " + err + "</p>");
				}
			};
			$.ajax(ajaxParams);
		}
		event.preventDefault();
		event.stopPropagation();
	}

	/**
	 * Create a DOM object referring to this set of forum posts
	 *
	 * @param posts the array of forum post objects, newest first
	 * @param context the string defining the context
	 *
	 * @return new DOM object
	 *
	 * TODO: shorten this function by moving code into subroutines. Also, postpone creating
	 * DOM elements until finished constructing the filtered list of threads, to make the code
	 * cleaner, faster, and more testable.
	 *
	 * Threading has been revised, so that the same locale+path can have multiple distinct threads,
	 * rather than always combining posts with the same locale+path into a single "thread".
	 * Reference: https://unicode-org.atlassian.net/browse/CLDR-13695
	 */
	function parseContent(posts, context) {

		const opts = getOptionsForContext(context);

		if (opts.fullSet) {
			postHash = {};
		}
		updatePostHash(posts);

		var postDivs = {}; //  postid -> div
		var topicDivs = {}; // xpath -> div or "#123" -> div

		// next, add threadIds and create the topic divs
		for (let num in posts) {
			var post = posts[num];
			post.threadId = getThreadId(post);

			if (!topicDivs[post.threadId]) {
				// add the topic div
				var topicDiv = document.createElement('div');
				topicDiv.className = 'well well-sm postTopic';
				var topicInfo = forumCreateChunk("", "h4", "postTopicInfo");
				if (opts.showItemLink) {
					topicDiv.appendChild(topicInfo);
					if (post.locale) {
						var localeLink = forumCreateChunk(locmap.getLocaleName(post.locale), "a", "localeName");
						if (post.locale != surveyCurrentLocale) {
							localeLink.href = linkToLocale(post.locale);
						}
						topicInfo.appendChild(localeLink);
					}
				}
				if (!post.xpath) {
					topicInfo.appendChild(forumCreateChunk(post2text(post.subject), "span", "topicSubject"));
				} else if (opts.showItemLink) {
					var itemLink = forumCreateChunk(forumStr("forum_item"), "a", "pull-right postItem glyphicon glyphicon-zoom-in");
					itemLink.href = "#/" + post.locale + "//" + post.xpath;
					topicInfo.appendChild(itemLink);
					(function(topicInfo) {
						var loadingMsg = forumCreateChunk(forumStr("loading"), "i", "loadingMsg");
						topicInfo.appendChild(loadingMsg);
						xpathMap.get({
							hex: post.xpath
						}, function(o) {
							if (o.result) {
								topicInfo.removeChild(loadingMsg);
								var itemPh = forumCreateChunk(xpathMap.formatPathHeader(o.result.ph), "span", "topicSubject");
								itemPh.title = o.result.path;
								topicInfo.appendChild(itemPh);
							}
						});
					})(topicInfo);
				}
				topicDivs[post.threadId] = topicDiv;
				topicDiv.id = "fthr_" + post.threadId;
			}
		}
		// Now, top to bottom, just create the post divs
		for (let num in posts) {
			var post = posts[num];

			var subpost = forumCreateChunk("", "div", "post");
			postDivs[post.id] = subpost;
			subpost.id = "fp" + post.id;

			var headingLine = forumCreateChunk("", "h4", "selected");

			// If post.posterInfo is undefined, don't crash; insert "[Poster no longer active]".
			if (!post.posterInfo) {
				headingLine.appendChild(forumCreateChunk("[Poster no longer active]", "span", ""));
			} else {
				/*
				 * TODO: encapsulate "createGravitar" dependency
				 */
				var gravitar;
				if (typeof createGravitar !== 'undefined') {
					gravitar = createGravitar(post.posterInfo);
				} else {
					gravitar = document.createTextNode('');
				}
				gravitar.className = "gravitar pull-left";
				subpost.appendChild(gravitar);
				/*
				 * TODO: encapsulate "surveyUser" dependency
				 */
				if (typeof surveyUser !== 'undefined' && post.posterInfo.id === surveyUser.id) {
					headingLine.appendChild(forumCreateChunk(forumStr("user_me"), "span", "forum-me"));
				} else {
					var usera = forumCreateChunk(post.posterInfo.name + ' ', "a", "");
					if (post.posterInfo.email) {
						usera.appendChild(forumCreateChunk("", "span", "glyphicon glyphicon-envelope"));
						usera.href = "mailto:" + post.posterInfo.email;
					}
					headingLine.appendChild(usera);
					headingLine.appendChild(document.createTextNode(' (' + post.posterInfo.org + ') '));
				}
				var userLevelChunk = forumCreateChunk(forumStr("userlevel_" + post.posterInfo.userlevelName), "span", "userLevelName label-info label");
				userLevelChunk.title = forumStr("userlevel_" + post.posterInfo.userlevelName + "_desc");
				headingLine.appendChild(userLevelChunk);
			}
			var date = fmtDateTime(post.date_long);
			if (post.version) {
				date = "[v" + post.version + "] " + date;
			}
			var dateChunk = forumCreateChunk(date, "span", "label label-primary pull-right forumLink");
			(function(post) {
				/*
				 * TODO: encapsulate "listenFor" and "reloadV" dependencies
				 */
				if (typeof listenFor === 'undefined') {
					return;
				}
				listenFor(dateChunk, "click", function(e) {
					if (post.locale && locmap.getLanguage(surveyCurrentLocale) != locmap.getLanguage(post.locale)) {
						surveyCurrentLocale = locmap.getLanguage(post.locale);
					}
					surveyCurrentPage = '';
					surveyCurrentId = post.id;
					replaceHash(false);
					if (surveyCurrentSpecial != 'forum') {
						surveyCurrentSpecial = 'forum';
						reloadV();
					}
					return stStopPropagation(e);
				});
			})(post);
			headingLine.appendChild(dateChunk);
			subpost.appendChild(headingLine);

			var subSubChunk = forumCreateChunk("", "div", "postHeaderInfoGroup");
			subpost.appendChild(subSubChunk); {
				var subChunk = forumCreateChunk("", "div", "postHeaderItem");
				subSubChunk.appendChild(subChunk);
				subChunk.appendChild(forumCreateChunk(post2text(post.subject), "b", "postSubject"));
			}

			// actual text
			var postText = post2text(post.text);
			var postContent = forumCreateChunk(postText, "div", "postContent");
			subpost.appendChild(postContent);
			
			subpost.appendChild(forumCreateChunk('【' + post.forumStatus + '】', 'div', ''));

			if (opts.showReplyButton) {
				var replyButton = forumCreateChunk(forumStr("forum_reply"), "button", "btn btn-default btn-sm");
				(function(post) {
					/*
					 * TODO: encapsulate "listenFor" dependency
					 */
					if (typeof listenFor === 'undefined') {
						return;
					}
					listenFor(replyButton, "click", function(e) {
						openPostOrReply({
							locale: surveyCurrentLocale,
							//xpath: '',
							replyTo: post.id,
							replyData: post,
							onReplyClose: opts.onReplyClose
						});
						stStopPropagation(e);
						return false;
					});
				})(post);
				subpost.appendChild(replyButton);
			}
		}
		// reparent any nodes that we can
		for (let num in posts) {
			var post = posts[num];
			if (post.parent != -1) {
				forumDebug("reparenting " + post.id + " to " + post.parent);
				if (postDivs[post.parent]) {
					if (!postDivs[post.parent].replies) {
						// add the "replies" area
						forumDebug("Adding replies area to " + post.parent);
						postDivs[post.parent].replies = forumCreateChunk("", "div", "postReplies");
						postDivs[post.parent].appendChild(postDivs[post.parent].replies);
					}
					// add to new location
					postDivs[post.parent].replies.appendChild(postDivs[post.id]);
				} else {
					// The parent of this post was deleted.
					forumDebug("The parent of post #" + post.id + " is " + post.parent + " but it was deleted or not visible");
					// link it in somewhere
					topicDivs[post.threadId].appendChild(postDivs[post.id]);
				}
			} else {
				// 'top level' post
				topicDivs[post.threadId].appendChild(postDivs[post.id]);
			}
		}
		return filterAndAssembleForumThreads(posts, topicDivs, opts.applyFilter, opts.showThreadCount);
	}

	/**
	 * Get an object whose properties define the parseContent options to be used for a particular
	 * context in which parseContent is called
	 *
	 * @param context the string defining the context:
	 *
	 *   'main' for the context in which "Forum" is chosen from the left sidebar
	 *
	 *   'info' for the "Info Panel" context (either main vetting view row, or Dashboard "Fix" button)
	 *
	 *   'new' for creation of a new post or reply
	 *
	 * @return an object with these properties:
	 *
	 *   showItemLink = true if there should be an "item" (xpath) link
	 *
	 *   showReplyButton = true if there should be a reply button
	 *
	 *   fullSet = true if this is a full set of posts
	 *
	 *   applyFilter = true if the currently menu-selected filter should be applied
	 *
	 *   showThreadCount = true to display the number of threads
	 *
	 *   onReplyClose = a callback function, or null
	 */
	function getOptionsForContext(context) {
		let opts = getDefaultParseOptions();
		if (context === 'main') {
			opts.showItemLink = true;
			opts.showReplyButton = true;
			opts.applyFilter = true;
			opts.showThreadCount = true;
			opts.onReplyClose = function(postModal, form, formDidChange) {
				if (formDidChange) {
					console.log('cldrStForum.getOptionsForContext calling reloadV for onReplyClose');
					/*
					 * TODO: encapsulate dependency on reloadV, or avoid this callback business entirely
					 */
					reloadV();
				}
			};
		} else if (context === 'info') {
			opts.showReplyButton = true;
		} else if (context === 'new') {
			/*
			 * posts may have zero, one, or more elements here, for parent(s),
			 * if any, of a new post in the process of being created
			 */
			opts.fullSet = false;
		} else {
			console.log('Unrecognized context in getOptionsForContext: ' + context)
		}
		return opts;
	}

	/**
	 * Get the default parseContent options
	 *
	 * @return a new object with the default properties
	 */
	function getDefaultParseOptions() {
		let opts = {};
		opts.showItemLink = false;
		opts.showReplyButton = false;
		opts.fullSet = true;
		opts.applyFilter = false;
		opts.showThreadCount = false;
		opts.onReplyClose = null;
		return opts;
	}

	/**
	 * Update the postHash mapping from post id to post object
	 *
	 * @param posts the array of posts
	 */
	function updatePostHash(posts) {
		for (let num in posts) {
			postHash[posts[num].id] = posts[num];
		}
	}

	/**
	 * Convert the given text by replacing some html with plain text
	 *
	 * @param the plain text
	 */
	function post2text(text) {
		if (text === undefined || text === null) {
			text = "(empty)";
		}
		var out = text;
		out = out.replace(/<p>/g, '\n');
		out = out.replace(/&quot;/g, '"');
		out = out.replace(/&lt;/g, '<');
		out = out.replace(/&gt;/g, '>');
		out = out.replace(/&amp;/g, '&');
		return out;
	}

	/**
	 * Create a DOM object with the specified text, tag, and HTML class.
	 *
	 * @param text textual content of the new object, or null for none
	 * @param tag which element type to create, or null for "span"
	 * @param className CSS className, or null for none.
	 * @return new DOM object
	 *
	 * This duplicated a function in survey.js; copied here to avoid the dependency, at least while testing/refactoring
	 */
	function forumCreateChunk(text, tag, className) {
		if (!tag) {
			tag = "span";
		}
		var chunk = document.createElement(tag);
		if (className) {
			chunk.className = className;
		}
		if (text) {
			chunk.appendChild(document.createTextNode(text));
		}
		return chunk;
	}

	/**
	 * Get the "thread id" for the given post.
	 *
	 * For a post with a parent, the thread id is the same as the thread id of the parent.
	 *
	 * For post without a parent, the thread id is like "aa|1234", where aa is the locale and 1234 is the post id.
	 *
	 * Caution: strangely, a post may have a different locale than the first post in its thread.
	 * For example, even though post 32034 is fr_CA, its child 32036 is fr.
	 * The thread id must use the locale of of the first post, for consistency.
	 *
	 * @param post the post object
	 * @return the thread id string
	 */
	function getThreadId(post) {
		const firstPost = getFirstPostInThread(post);
		return firstPost.locale + "|" + firstPost.id;
	}

	/**
	 * Get the first (original) post in the thread containing this post
	 *
	 * @param post the post object
	 * @return the first post in the thread
	 */
	function getFirstPostInThread(post) {
		while (post.parent >= 0 && postHash[post.parent]) {
			post = postHash[post.parent];
		}
		return post;
	}

	/**
	 * Filter the forum threads and assemble them into a new document fragment,
	 * ordering threads from newest to oldest, determining the time of each thread
	 * by the newest post it contains
	 *
	 * @param posts the array of post objects, from newest to oldest
	 * @param topicDivs the array of thread elements, indexed by threadId
	 * @param applyFilter true if the currently menu-selected filter should be applied
	 * @param showThreadCount true to display the number of threads
	 * @return the new document fragment
	 */
	function filterAndAssembleForumThreads(posts, topicDivs, applyFilter, showThreadCount) {

		let filteredArray = cldrStForumFilter.getFilteredThreadIds(posts, applyFilter);
		const forumDiv = document.createDocumentFragment();
		let countEl = null;
		if (showThreadCount) {
			countEl = document.createElement('h4');
			forumDiv.append(countEl);
		}
		let threadCount = 0;
		posts.forEach(function(post) {
			if (filteredArray.includes(post.threadId)) {
				++threadCount;
				/*
				 * Append the div for this threadId, then remove this threadId
				 * from filteredArray to prevent appending the same div again
				 * (which would move the div to the bottom, not duplicate it).
				 */
				forumDiv.append(topicDivs[post.threadId]);
				filteredArray = filteredArray.filter(id => (id !== post.threadId));
			}
		});
		if (showThreadCount) {
			countEl.innerHTML = threadCount + ' threads';
		}
		return forumDiv;
	}

	/**
	 * Convert the given short string into a human-readable string.
	 *
	 * TODO: encapsulate "stui" dependency better
	 *
	 * @param s the short string, like "forum_item" or "forum_reply"
	 * @return the human-readable string like "Item" or "Reply"
	 */
	function forumStr(s) {
		if (typeof stui !== 'undefined') {
			return stui.str(s);
		}
		return s;
	}

	/**
	 * Format a date and time for display in a forum post
	 *
	 * @param x the number of seconds since 1970-01-01
	 * @returns the formatted date and time as a string, like "2018-05-16 13:45"
	 */
	function fmtDateTime(x) {
		const d = new Date(x);

		function pad(n) {
			return (n < 10) ? '0' + n : n;
		}
		return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) +
			' ' + pad(d.getHours()) + ':' + pad(d.getMinutes());
	}

	/*
	 * Make only these functions accessible from other files:
	 */
	return {
		openPostFromDashboard: openPostFromDashboard,
		openPostOrReply: openPostOrReply,
		parseContent: parseContent,
	};
})();

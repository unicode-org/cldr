/*
 * Show the "Review" page, a.k.a. the "Dashboard"
 */

/**
 * Bind the event only on the review (Dashboard) page
 *
 * Called by showReviewPage and checkLineFix, both in this file 
 */
function bindReviewEvents() {
	$('.help-comment').popover({placement: 'right'}); // show the comment column
	$('.help-review-pop').popover({placement: 'left', trigger: 'hover'}); // help in the review menu
	$('.tip').tooltip(); // show tooltip

	refreshAffix(); // refresh the review menu
}

/*
 * Startup function
 */
$(function() {
	var dynamic = $('#main-container');
	dynamic.on('click', '.collapse-review', togglePart);
	dynamic.on('click', 'button.fix', toggleFix);
	dynamic.on('click', '.hide-review', toggleReviewLine);
	dynamic.on('click', '.show-items', toggleItems);
	dynamic.on('click', '.hide-review.done', toggleReview);

	$(window).scroll(function() {
		var left = $(this).scrollLeft();
		if (left != 0) {
			$('#itemInfo').css('left', 1020 - left);
		} else {
			$('#itemInfo').css('left', "");
		}
		$('.navbar-fixed-top').css('left', 0 - left);
	});
	$(window).resize(function() {
		$('#itemInfo').css('left', "");
		resizeSidebar()
	});

	$('body').on('click', '.show-examples', function() { // toggle the examples
		$('.d-example, .vote-help').slideToggle();
	});
});

/**
 * Handle the review (Dashboard) page with the json
 *
 * @param json
 * @param showFn
 *
 * Called only from CldrSurveyVettingLoader.js
 */
function showReviewPage(json, showFn) {
	var notificationsRoot = $('#OtherSection');

	if (json.err) {
		// Error value.
		notificationsRoot.html("<div style='padding-top: 4em; font-size: x-large !important;' class='ferrorbox warning'>" +
			"<span class='icon i-stop'> &nbsp; &nbsp;</span>Error: could not load: " +
			json.err + "</div>");
		showFn(); // calls the flipper to flip to the 'other' page.
		return;
	}

	var menuData = json.notification;
	var notifications = json.allNotifications;
	var menuRoot = $('#itemInfo');
	var hidden = json.hidden;
	var direction = json.direction;
	var lastVersion = surveyVersion - 1;

	// populate menu
	var activeMenu = true;
	let sidebarHtml = ''
	$.each(menuData, function(index, element) {
		// activate this menu first
		if (activeMenu) {
			sidebarHtml += '<li class="active">';
			activeMenu = false;
		} else {
			sidebarHtml += '<li>';
		}
		// inactivate the one with no element
		sidebarHtml += '<a href="#' + element.name + '">';
		sidebarHtml += element.name.replace('_', ' ') + ' (<span class="remaining-count">0</span>/<span class="total-count">' +
			element.count + '</span>)<div class="pull-right"><span class="glyphicon glyphicon-info-sign help-review-pop" data-content="' +
			element.description + '"></span></div></a></li>';
	});

	if (cldrStForum && surveyCurrentLocale && surveyUser && surveyUser.id) {
		const forumSummary = cldrStForum.getForumSummaryHtml(surveyCurrentLocale, surveyUser.id, true /* table */);
		sidebarHtml += "<li><a id='dashToForum' onclick='cldrStForum.reload();'>Forum</a></li>\n";
		sidebarHtml += "<li>" + forumSummary + "</li>\n";
	}
	const menuDom = document.createElement('ul');
	menuDom.className = 'nav nav-pills nav-stacked affix menu-review';
	menuDom.innerHTML = sidebarHtml;

	// populate body
	var html = '';
	if (notifications && notifications.length) {
		menuRoot.html(menuDom);
	} else {
		const cov = covName(effectiveCoverage());
		if (cov === 'comprehensive') {
			html = '<p>There are no items to review.</p>';
		} else {
			html = '<p>There are no items to review at the ' + cov + ' coverage level.</p>';
		}
	}

	$.each(notifications, function(index, element) {
		// header
		if (element != 'null') {
			$.each(element, function(index, element) {
				html += '<h3 class="collapse-review"><span id="' + index +
					'"></span><span class="glyphicon glyphicon-chevron-down chevron"></span>' +
					index.replace('_', ' ') +
					' (<span class="remaining-count">0</span>/<span class="total-count"></span>)<label class="pull-right show-items">' +
					'<button class="tip btn btn-default" data-toggle="button" title="Show hidden lines"><span class="glyphicon glyphicon-eye-open"></span>' +
					'</button></label></h3>';
				html += '<div class="table-wrapper" data-type="' + index +
					'"><table class="table table-responsive table-fixed-header table-review"><thead><tr><th>Code</th><th>English</th><th dir="' +
					direction + '">Baseline</th><th dir="' + direction +
					'">Winning ' + surveyVersion + '</th><th dir="' + direction +
					'">Action</th></tr></thead><tbody>';

				$.each(element, function(index, element) {
					// body
					var catName = index;
					$.each(element, function(index, element) {
						var subCatName = index;
						$.each(element, function(index, element) {
							if (index != 'null') {
								html += '<tr class="info"><td colspan="5"><b>' + catName + ' - ' + subCatName + '</b> : ' + index + '</td></tr>'; // blue banner
							} else {
								html += '<tr class="info"><td colspan="5"><b>' + catName + ' - ' + subCatName + '</b></td></tr>'; // blue banner
							}
							$.each(element, function(index, element) {
								var oldElement;
								if ('old' in element) {
									oldElement = element.old;
								} else {
									oldElement = '<i class="missing">missing</i>'; // TODO - markup as missing?
								}
								var engElement;
								if ('english' in element) {
									engElement = element.english;
								} else {
									engElement = '<i class="missing">missing</i>'; // TODO - markup as missing?
								}
								html += '<tr class="data-review" data-path=\'' +
									element.path +
									'\'"><td class="button-review"><span class="link-main"><a target="_blank" href="' +
									getUrlReview(element.id) + '"><span class="label label-info">' +
									element.code +
									' <span class="glyphicon glyphicon-share"></span></span></a></span></td><td>' +
									engElement + '</td><td dir="' + direction + '">' + oldElement + '</td><td dir="' + direction + '">' +
									element.winning + '</td>';

								// "Fix" and "Hide" sections
								html += '<td class="button-review"><div class="tip fix-parent" title="Fix">' +
									'<button type="button" class="btn btn-success fix" data-toggle="popover">' +
									'<span class="glyphicon glyphicon-pencil"></span></button></div> ' +
									'<button type="button" class="btn btn-info hide-review tip" title="Hide">' +
									'<span class="glyphicon glyphicon-eye-close"></span></button>';
								if (element.comment) {
									html += '<button class="btn btn-default help-comment" data-html="true" data-toggle="popover" data-content="' +
										element.comment + '"><span class="glyphicon glyphicon-info-sign"></span></button>';
								}
								html += '</td></tr>';
							});
						});
						html += '<tr class="empty"><td colspan="5"></td></tr>';
					});
				});
			});
		}
		html += '</tbody></table></div>';
	});

	if (surveyVersion === '37') { // TODO: get CheckCLDR.LIMITED_SUBMISSION from server
		html += '<p>This is a Limited-Submission release, so the list of items here is restricted. ' +
			'For more information, see the ' +
			'<a href="http://cldr.unicode.org/translation">Information Hub for Linguists</a></p>';
	}
	notificationsRoot.html(html);
	showFn(); // calls the flipper to flip to the 'other' page.

	// show the alert ms
	popupAlert('warning', 'It is important that you read' +
		' <a href="http://cldr.unicode.org/translation/vetting-view" target="_blank">Priority Items</a>' +
		' before starting!');

	// hack to solve anchor issue
	$('#itemInfo .nav a').click(function(event) {
		if (event.target.id === 'dashToForum') {
			return;
		}
		var href = $(this).attr('href').replace('#', '');
		var aTag = $('#' + href);
		if (aTag.length) {
			$('html,body').animate({
				scrollTop: aTag.offset().top
			}, 'slow');
		}
		event.preventDefault();
		event.stopPropagation();
	});

	$.each(hidden, function(index, element) {
		var cat = index;
		$.each(element, function(index, element) {
			$('div[data-type="' + cat + '"] tr[data-path="' + element + '"] .hide-review').click();
		});
	});
	$('.hide-review').addClass('done');
	refreshCounter();
	$('.menu-review li:visible').first().addClass('active');
	bindReviewEvents();
}

/**
 * Get the url to link to the vetting page
 *
 * @param id
 * @returns the url as a string
 *
 * Called only by showReviewPage in this file
 */
function getUrlReview(id) {
	return contextPath + '/v#/' + surveyCurrentLocale + '//' + id;
}

/**
 * Save the hide line
 *
 * Called only by the Startup function at the top of this file
 */
function toggleReview() {
	var url = contextPath + "/SurveyAjax?what=review_hide&s=" + surveySessionId;
	var path = $(this).parents('tr').data('path');
	var choice = $(this).parents('.table-wrapper').data('type');
	url += "&path=" + path + "&choice=" + choice + "&locale=" + surveyCurrentLocale;
	$.get(url, function(data) {
		refreshCounter();
	});
}

/**
 * Refresh the counter
 *
 * Called from several places in this file.
 *
 * TODO to optimize, do not update every label, only one need to -> refresh the counter from the review page
 */
function refreshCounter() {
	var menus = $('.menu-review a');
	menus.each(function(index) {
		var element = $(this);
		var href = element.attr('href');
		if (!href) {
			return; // skip, dashToForum
		}
		var id = href.slice(1, href.length);
		var selection = $('div[data-type="' + id + '"] tr.data-review');
		var total = selection.length;
		var remaining = selection.not('.hidden-line').length;
		var counterList = $('#' + id).closest('h3');

		element.children('.remaining-count').text(remaining);
		element.children('.total-count').text(total);
		if (total == 0) {
			element.closest('li').remove();
		}
		counterList.children('.remaining-count').text(remaining);
		counterList.children('.total-count').text(total);
	});
}

/**
 * Toggle the notifications type
 *
 * Called only by the Startup function at the top of this file
 */
function togglePart() {
	var table = $(this).next();
	var glyph = $(this).find('.glyphicon.chevron');

	glyph.toggleClass('glyphicon-chevron-down').toggleClass('glyphicon-chevron-right');
	table.slideToggle();

	refreshAffix();
}

/**
 * Open a slide with the fix button
 *
 * @param event
 *
 * Called only by the Startup function at the top of this file
 */
function toggleFix(event) {
	var tr = $(this).closest('tr');
	var button = $(this);
	var isPopover = button.parent().find('.popover').length === 1;
	$('button.fix').popover('destroy');
	toggleOverlay();
	if (!isPopover) {
		var url = contextPath + "/SurveyAjax?what=" + WHAT_GETROW +
			"&_=" + surveyCurrentLocale +
			"&s=" + surveySessionId +
			"&xpath=" + tr.data('path') +
			"&strid=" + surveyCurrentId + cacheKill() +
			"&dashboard=true";
		myLoad(url, "section", function(json) {
			isLoading = false;
			theDiv = document.createElement("div");
			theDiv.id = "popover-vote";
			if (json.section.nocontent) {
				surveyCurrentSection = '';
			} else if (!json.section.rows) {
				console.log("!json.section.rows");
				handleDisconnect("while loading- no rows", json);
			} else {
				stdebug("json.section.rows OK..");
				if (json.dataLoadTime) {
					updateIf("dynload", json.dataLoadTime);
				}
				if (!surveyUser) {
					showInPop2(stui.str("loginGuidance"), null, null, null, true); /* show the box the first time */
				} else if (!json.canModify) {
					showInPop2(stui.str("readonlyGuidance"), null, null, null, true); /* show the box the first time */
				} else {
					showInPop2(stui.str("dataPageInitialGuidance"), null, null, null, true); /* show the box the first time */
				}

				insertFixInfo(theDiv, json.pageId, surveySessionId, json);

				// display the popover
				if (button.parent().find('.popover:visible').length == 0) {
					button.popover('destroy');
				}
				button.popover({
					placement: "left",
					html: true,
					content: '<div></div>',
					title: showAllProblems(json.issues) + tr.children('td').first().html(),
					animation: false
				}).popover('show');
				button.data('issues', json.issues);

				// check if we have to suppress the line
				button.on('hidden.bs.popover', checkLineFix);

				// add the row to the popover
				var content = button.parent().find('.popover-content').get(0);
				content.appendChild(theDiv);

				// hack/redesign it
				designFixPanel();

				// correct the position of the popover
				fixPopoverVotePos();
			}
		});
	}
	refreshAffix();
	event.preventDefault();
	event.stopPropagation();
	return false;
}

/**
 * Hide or show line for the review (Dashboard) page
 *
 * This function is the click handler for the .hide-review button.
 */
function toggleReviewLine(event) {
	var line = $(this).closest('tr');
	var next = line.next();

	if (line.hasClass('hidden-line')) {
		line.removeClass('hidden-line');
		if (next.hasClass('fix-info')) {
			next.removeClass('hidden-line'); // for the fix menu
		}
		$(this).removeClass('btn-warning').addClass('btn-info');
		$(this).find('.glyphicon').removeClass('glyphicon-eye-open').addClass('glyphicon-eye-close');
		$(this).tooltip('hide').attr('title','Hide').tooltip('fixTitle');
	} else {
		line.addClass('hidden-line');
		if (next.hasClass('fix-info')) {
			next.addClass('hidden-line');
		}
		$(this).removeClass('btn-info').addClass('btn-warning');
		$(this).find('.glyphicon').removeClass('glyphicon-eye-close').addClass('glyphicon-eye-open');
		$(this).tooltip('hide').attr('title','Show').tooltip('fixTitle');
	}

	// hide blue banner
	var info = line.prevAll(".info:first"); // get the closet previous
	var hide = true;
	info.nextUntil('.info').each(function(index) { //check if all sublines are hidden or not a real line
		if (!($(this).hasClass('hidden-line') || $(this).hasClass('empty'))) {
			hide = false;
		}
	});

	if (hide) {
		info.addClass('hidden-line');
		info.nextUntil('.empty').last().next().addClass('hidden-line');
	} else {
		info.removeClass('hidden-line');
		info.nextUntil('.empty').last().next().removeClass('hidden-line');
	}

	var table = info.parents('table');
	if (table.find('tr:not(.hidden-line):not(.empty)').length > 1) { //there is only the header staying not hidden
		table.find('tr:first').removeClass('hidden-line');
	} else {
		table.find('tr:first').addClass('hidden-line');
	}
	refreshAffix();
	event.preventDefault();
	event.stopPropagation();
}

/**
 * Force to show the actual hidden line
 *
 * @param event
 *
 * Called only by the Startup function at the top of this file
 */
function toggleItems(event) {
	var input = $(this).children('button');
	var eyes = $('.show-items > button');
	if (input.hasClass('active')) {
		$('tr').removeClass('shown');
		eyes.removeClass('active');
		eyes.tooltip('hide').attr('data-original-title','Show hidden lines').tooltip('fixTitle');
	} else {
		$('tr').addClass('shown');
		eyes.addClass('active');
		eyes.tooltip('hide').attr('data-original-title','Hide selected lines').tooltip('fixTitle');
	}
	input.tooltip('show');

	refreshAffix();
	event.stopPropagation();
	event.preventDefault();
}

/**
 * Refresh affix (right menu).
 *
 * Called from several places in this file.
 */
function refreshAffix() {
	$('[data-spy="scroll"]').each(function() {
		/*
		 * TODO: explain assignment to $spy
		 */
		var $spy = $(this).scrollspy('refresh');
	});
}

/**
 * Add or remove line depending if we solved the issue or created new
 *
 * Called only by toggleFix in this file.
 */
function checkLineFix() {
	var line = $(this).closest('tr');
	var info = line.prevAll(".info:first");
	var path = line.data('path');
	var issues = $(this).data('issues');

	var lines = $('tr[data-path=' + path + ']');
	lines.each(function() {
		if ($(this).hasClass('success')) {
			$(this).fadeOut('slow', function() {
				var inf = $(this).prevAll(".info:first");
				$(this).remove();
				if (inf.next('.data-review').length == 0) {
					inf.next('.empty').remove();
					inf.remove();
				}
				if ($('.fix-parent .popover').length) {
					fixPopoverVotePos();
				}
				refreshCounter();
			});
		}
	})

	$.each(issues, function(index, element) {
		var elementRaw = element.replace(' ', '_');
		var otherLine = $('div[data-type=' + elementRaw + '] tr[data-path=' + path + ']');
		if (otherLine.length == 0) { //if line not present
			var newLine = line.clone();
			var found = false;
			$('div[data-type=' + elementRaw + '] .info').each(function() {
				if (info.html() == $(this).html()) {
					$(this).after(newLine);
					found = true;
				}
			});

			if (!found) {
				var html = '<tr class="info">' + info.html() + '</tr>' +
					newLine.wrap('<div>').parent().html() +
					'<tr class="empty"><td colspan="5"></td></tr>';
				var toInsert = $('div[data-type=' + elementRaw + '] > table > tbody');
				toInsert.prepend(html);
			}
		}
	});

	bindReviewEvents();
	refreshCounter();
}

/**
 * Refresh the Dashboard "Fix" panel
 *
 * @param json
 *
 * Called only by the loadHandler for refreshSingleRow in survey.js,
 * only if isDashboard() is true
 */
function refreshFixPanel(json) {
	var issues = json.issues;
	var theDiv = $('#popover-vote').get(0);
	theDiv.innerHTML = '';

	insertFixInfo(theDiv, json.pageId, surveySessionId,json);
	designFixPanel();
	fixPopoverVotePos();

	var line = $('.fix-parent .popover').closest('tr');
	var path = line.data('path');
	$('tr[data-path='+path+']').each(function() {
		var type = $(this).closest('.table-wrapper').data('type');
		if ($.inArray(type, issues) == -1) {
			$(this).addClass('success');
		} else {
			$(this).removeClass('success');
		}
	});

	$('.fix-parent .popover-title').html(showAllProblems(issues) + line.children('td').first().html());

	if ($('.data-vertical .vote-help').length == 0) {
		$('.data-vertical .vote-help').remove();
		$('.data-vertical .comparisoncell .d-example').after($('.data-vote .vote-help'));
		$('.vote-help').hide();
	} else {
		$('.data-vote .vote-help').remove();
	}

	line.find('button.fix').data('issues',issues);
}

/**
 * Show the vote summary part of the Fix panel
 *
 * @param cont
 *
 * Called only by showInPop2 in survey.js, only if isDashboard() is true
 */
function showHelpFixPanel(cont) {
	$('.fix-parent .data-vote').html('');
	$('.fix-parent .data-vote').append(cont);

	$('.data-vote > .span, .data-vote > .pClassExplain').remove();
	$('.data-vote > .span, .data-vote > .d-example').remove();

	var helpBox = $('.data-vote > *:not(.voteDiv)').add('.data-vote hr');
	$('.data-vote table:last').after(helpBox);

	if ($('.trInfo').length != 0) {
		$('.voteDiv').prepend('<hr/>');
		$('.voteDiv').prepend($('.trInfo').parent());
	}

	// move the element
	labelizeIcon();
}

/**
 * Insert the "Fix" information in the Dashboard window ("popover") linked to the "Fix" button
 *
 * @param theDiv
 * @param xpath
 * @param session
 * @param json
 *
 * Called by toggleFix and refreshFixPanel, both in this file
 */
function insertFixInfo(theDiv, xpath, session, json) {
	removeAllChildNodes(theDiv);
	window.insertLocaleSpecialNote(theDiv);
	/*
	 * Note: theTable isn't really a table, it's a div. It seems to have this name by analogy
	 * with the table in the main non-Dashboard interface. Likewise, the name "tr" is used
	 * below for elements that aren't really table rows.
	 */
	var theTable = cloneLocalizeAnon(document.getElementById("proto-datafix"));
	theTable.className = 'data dashboard';
	updateCoverage(theDiv);
	localizeFlyover(theTable); // Replace titles starting with $ with strings from stui

	var toAdd = cloneLocalizeAnon(document.getElementById("proto-datarowfix")); // loaded from "hidden.html", which see.
	theTable.toAdd = toAdd;
	theTable.myTRs = [];
	theDiv.theTable = theTable;
	theTable.theDiv = theDiv;

	theTable.json = json;
	theTable.xpath = xpath;
	theTable.session = session;

	var tbody = $(theTable).children('.data-vertical').get(0);

	if (!theTable.curSortMode) {
		/*
		 * TODO: merge this block with similar code in survey.js; some or all of this code might be unneeded
		 */
		theTable.curSortMode = theTable.json.displaySets["default"];
		// hack - choose one of these
		if (theTable.json.displaySets.codecal) {
			theTable.curSortMode = "codecal";
		} else if (theTable.json.displaySets.metazon) {
			theTable.curSortMode = "metazon";
		}
	}

	const k = theTable.json.displaySets[theTable.curSortMode].rows[0];
	if (!k) {
		console.log("k is null or undefined in insertFixInfo");
		return;
	}
	const theRow = theTable.json.section.rows[k];
	if (!theRow) {
		console.log("theRow is null or undefined in insertFixInfo; k = " + k);
		return;
	}
	removeAllChildNodes(tbody);
	/*
	 * Caution: in spite of the name here in the JavaScript, "tr" isn't a
	 * table row, it's a div. Each element it contains is also a div, not a td.
	 */
	var tr = theTable.myTRs[k];
	if (!tr) {
		tr = cloneAnon(theTable.toAdd);
		theTable.myTRs[k]=tr; // save for later use
	}
	tr.rowHash = k;
	tr.theTable = theTable;

	cldrSurveyTable.updateRow(tr,theRow);

	if (!tr.forumDiv) {
		tr.forumDiv = document.createElement("div");
		tr.forumDiv.className = "forumDiv";
	}
	appendForumStuff(tr,theRow, tr.forumDiv);
	tbody.appendChild(tr);
	theDiv.appendChild(theTable);
}

/**
 * Update the "Fix" window
 *
 * Called by toggleFix and refreshFixPanel, both in this file
 */
function designFixPanel() {
	var nocell = $('.fix-parent #popover-vote .data-vertical .nocell');
	var idnocell = nocell.find('input').attr('id');
	nocell.append('<span class="subSpan">Abstain</span>');

	/*
	 * statuscell itself is invisible in Dashboard Fix. Still, it plays an
	 * essential role. Its className is set by updateRowStatusCell, and then
	 * gives us statusClass here, which we append below, producing, for example,
	 * a green checkmark for "d-dr-approved".
	 */
	var statuscell = $('.fix-parent #popover-vote .data-vertical .statuscell').get(0);
	var statusClass = null;
	if (statuscell) {
		statusClass = statuscell.className;
		statuscell.className = '';
	}

	var comparisoncell = $('.fix-parent #popover-vote .data-vertical .comparisoncell');
	comparisoncell.find('.btn').remove();
	if (!comparisoncell.find('.subSpan').length) {
		comparisoncell.contents()
		.filter(function() {
			return this.nodeType === 3;
		})
		.wrap('<span class="subSpan"></span>');
	}
	var exampleButton = $('<button title="Show examples" class="btn btn-default show-examples"><span class="glyphicon glyphicon-list"></span></button>');
	comparisoncell.prepend(exampleButton);
	exampleButton.tooltip();

	// clean
	if (!idnocell) {
		nocell.next().remove(); // the hr
		nocell.html('');
	}

	// add status of the winning item
	if (statusClass) {
		$('.fix-parent .proposedcell .subSpan .winner').after('<div class="'+statusClass+'"></div>');
	}

	// replace default by success on the selected one
	$('#popover-vote input[type="radio"]:checked').closest('.btn').removeClass('btn-default').addClass('btn-info');

	// add some help
	$('.fix-parent .nocell .subSpan').append('<div class="help-vote"></div>');
	$('.fix-parent .comparisoncell .subSpan .help-vote').remove();
	$('.fix-parent .comparisoncell .subSpan').append('<div class="help-vote">English source</div>');
	$('.fix-parent .proposedcell .subSpan').append('<div class="help-vote">Winning translation</div>');
	$('.fix-parent .othercell .form-inline').after('<span class="subSpan"><div class="help-vote">Add a translation</div></span>');
	if ($('.fix-parent .othercell .d-item').length) {
		$('.fix-parent .othercell hr').first().after('<span class="subSpan"><div class="help-vote">Other translation(s)</div></span>');
	}

	// remove unnecessary header
	$('#popover-vote .warnText').remove();

	$('.d-example').hide();
	fixPopoverVotePos();
	labelizeIcon();

	$('.fix-parent .proposedcell').click();
	$('.data .close').click(function() {$(this).closest('.popover').prev().popover('hide');$('.tip').tooltip('hide');});
}

/**
 * Reposition the "Fix" popover manually
 *
 * Called locally and also by showInPop2 in survey.js
 */
function fixPopoverVotePos() {
	var button = $('.fix-parent #popover-vote').closest('.fix-parent').find('.fix');
	var popover = button.parent().find('.popover');
	var decal = 75;
	popover.css('top', button.position().top - decal);
	popover.css('z-index',998);
	popover.children('.arrow').css('top',decal + button.outerHeight(true)/2);
}

/**
 * Add to the radio button, a more button style
 *
 * @param button
 * @returns a newly created label element
 *
 * Note: this is not only for Dashboard.
 *
 * Called from survey.js and CldrSurveyVettinTable.js
 */
function wrapRadio(button) {
	var label = document.createElement('label');
	label.title = 'Vote';
	label.className = 'btn btn-default';
	label.appendChild(button);
	$(label).tooltip();
	return label;
}

/**
 * Display an array of issues inline
 *
 * @param issues the array
 * @returns the string
 *
 * Called by toggleFix and refreshFixPanel, both in this file
 */
function showAllProblems(issues) {
	var string = '';
	$.each(issues, function(index, element) {
		if (string) {
			string += ',';
		}
		string += ' ' + element;
	});
	return string;
}

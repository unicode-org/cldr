//startup fonction
$(function() {

    
    //for locale search
    $('body').on('click','#show-locked', {type:"lock"}, toggleLockedRead);
    $('body').on('click','#show-read', {type:"read"}, toggleLockedRead);
	$('#locale-info .local-search').keyup(filterAllLocale);
    $('.pull-menu > a').click(interceptPulldownLink);
   // $('#survey-menu > a').click(interceptSurveyLink);
    
    //local chooser intercept
    $('body').on('click','.locName',interceptLocale);
    //handle sidebar
    $('#left-sidebar').hover(function(){
    			if(!$('body').hasClass('disconnected') && !window.haveDialog) { // don't hover if another dialog is open.
    				$(this).addClass('active');
    				toggleOverlay();
    			} else {
    				// can't show sidebar - page is disconnected.
    			}
    		}, 
    		function() {
    			if(surveyCurrentLocale 
    					|| surveyCurrentSpecial!='locales') { // don't stick the sidebar open if we're not in the locale chooser.
	    			$(this).removeClass('active');
	    			toggleOverlay();
    			}
    		});
    $('.refresh-search').click(searchRefresh);
   
    
    
    //help bootstrap -> close popup when click outside
    $('body').on('click', function (e) {
        $('[data-toggle="popover"]').each(function () {
            //the 'is' for buttons that trigger popups
            //the 'has' for icons within a button that triggers a popup
            if (!$(this).is(e.target) && $(this).has(e.target).length === 0 && $('.popover').has(e.target).length === 0) {
                $(this).popover('hide');
            }
        });
    });
    
    //example on hover
    $('body').on('mouseenter','.vetting-page .infos-code, .vetting-page .subSpan',function(){
    	var example = $(this).closest('.d-disp,.d-item,.d-item-err,.d-item-warn').find('.d-example');
    	if(example)
    		$(this).popover({html:true, placement:"top",content:example.html()}).popover('show');
    });
    
    $('body').on('mouseleave','.vetting-page .infos-code, .vetting-page .subSpan',function(){
    	$(this).popover('hide');
    });
    resizeSidebar();
    
    $('body').on('click', '.toggle-right', toggleRightPanel);
    $('.tip-log').tooltip({placement:'bottom'});
    $('body').keydown(function(event) {
		/*
		 * Some browsers (e.g., Firefox) treat Backspace (or Delete on macOS) as a shortcut for
		 * going to the previous page in the browser's history. That's a problem when we have an
		 * input window open for the user to type a new candidate item, especially if the window
		 * is still visible but has lost focus. Prevent that behavior for backspace when the input
		 * window has lost focus. Formerly, key codes 37 (left arrow) and 39 (right arrow) were used
		 * here as shortcuts for chgPage(-1) and chgPage(1), respectively. However, that caused
		 * problems similar to the problem with Backspace. Reference: https://unicode.org/cldr/trac/ticket/11218
		 */
    	if ($(':focus').length === 0) {
            if (event.keyCode === 8) { // backspace
    	        event.preventDefault();
            }
    	}
    })
});

//size the sidebar relative to the header
function resizeSidebar() {
	var sidebar = $('#left-sidebar');
	var header = $('.navbar-fixed-top');

	sidebar.css('height', $(window).height() - header.height());
	sidebar.css('top', header.height());
}

//this function is used in CldrSurveyVettingLoader.js
var sentenceFilter;
//filter all the locale (first son, then parent so we can build the tree, and let the parent displayed if a son is matched)
function filterAllLocale(event) {
		if($(this).hasClass('local-search')) {
			$('a.locName').removeClass('active');
			$('#locale-list,#locale-menu').removeClass('active');
		}
		sentenceFilter = $('input.local-search').val().toLowerCase();
		$('.subLocaleList .locName').each(filterLocale);//filtersublocale
		$('.topLocale .locName').each(filterLocale);//filtertolocale
		
		if(event) {
			event.preventDefault();
			event.stopPropagation();
		}
		return false;
}

//filter (locked and read-only) with locale
function toggleLockedRead(event) {
	var type = event.data.type;
	if($(this).is(':checked')) {
		if(type == "read")
			$('.locName:not(.canmodify):not(.locked)').parent().removeClass('hide');
		else
			$('.locName.locked').parent().removeClass('hide');
	}
	else {
		if(type == "read")
			$('.locName:not(.canmodify):not(.locked)').parent().addClass('hide');
		else
			$('.locName.locked').parent().addClass('hide');
	}
		
	filterAllLocale();
}

//hide/show the locale matching the pattern and the checkbox
function filterLocale() {
	var text = $(this).text().toLowerCase();
	var parent = $(this).parent();
	if(text.indexOf(sentenceFilter) == 0 && (checkLocaleShow($(this), sentenceFilter.length) || sentenceFilter === text)) {
		parent.removeClass('hide');
		if(parent.hasClass('topLocale')) {
			parent.parent().removeClass('hide');
			parent.next().children('div').removeClass('hide');
		}
	}
	else {
		if(parent.hasClass('topLocale')) {
			if(parent.next().children('div').not('.hide').length == 0) {
				parent.addClass('hide');
				parent.parent().addClass('hide');
			}
			else {
				parent.removeClass('hide');
				parent.parent().removeClass('hide');
				//parent.next().children('div').removeClass('hide');
			}
		}
		else
			parent.addClass('hide');
	}
};

//should we show this locale considering the checkbox ?
function checkLocaleShow(element, size) {
	if(size > 0)
		return true;
	
	if(element.hasClass('locked') && $('#show-locked').is(':checked'))
		return true;
	
	if((!element.hasClass('canmodify') && $('#show-read').is(':checked') && !element.hasClass('locked')) || element.hasClass('canmodify'))
		return true;
	
	return false;
} 

//intercept the click of the locale name ->
function interceptLocale() {
	var name = $(this).text();
	var source = $(this).attr('title');
	
	$('input.local-search').val(name);
	$('a.locName').removeClass('active');
	$(this).addClass('active');
	filterAllLocale();
	$('#locale-list').addClass('active');
	$('#locale-menu').addClass('active');
	
}


//sidebar constructor
var cachedJson; //use a cache cause the coverage can change, so we might need to update the menu
function unpackMenuSideBar(json) {
	if(json.menus) {
		cachedJson = json;
	}
	else {
		var lName = json["_v"];
		if(!cachedJson)
			return;
		json = cachedJson;
		json.covlev_user = lName;
	}
	var menus = json.menus.sections;
	var levelName = json.covlev_user;
	
	if(!levelName || levelName === 'default') {
		levelName = json.covlev_org;
	}
	var menuRoot = $('#locale-menu');
	var level = 0;
	var levels = json.menus.levels;
	var reports = json.reports;
	
	//get the level number
	$.each(levels, function(index, element) {
		if(element.name == levelName)
			level = parseInt(element.level);
	});
	
	if (level === 0) {
		// We couldn't find the level name. Try again as if 'auto'.
		levelName = json.covlev_org;
		
		//get the level number
		$.each(levels, function(index, element) {
			if(element.name == levelName)
				level = parseInt(element.level);
		});
				
		if ( level === 0) {
			// Still couldn't.
			level = 10; // fall back to CORE.
		}
	}
	
	var html = '<ul>';
	if(!isVisitor) {
		//put the dashboard out
		var tmp = null;
		var reportHtml = '';
		$.each(reports,function(index,element){
			if(element.url != "r_vetting_json")
				reportHtml += '<li class="list-unstyled review-link" data-query="'+element.hasQuery+'" data-url="'+element.url+'"><div>'+element.display+'</div></li>';
			else
				tmp = element;
		});
		
		if(tmp)
			html += '<li class="list-unstyled review-link" data-query="'+tmp.hasQuery+'" data-url="'+tmp.url+'"><div>'+tmp.display+'<span class="pull-right glyphicon glyphicon-home" style="position:relative;top:2px;right:1px;"></span></div></li>';

		
		html += '<li class="list-unstyled open-menu"><div>Reports<span class="pull-right glyphicon glyphicon-chevron-right"></span></div><ul class="second-level">';
		html += reportHtml;
		html += '</ul></li>';
	}
	html += '<li class="list-unstyled" id="forum-link"><div>Forum<span class="pull-right glyphicon glyphicon-comment"></span></div></li>';
	html += '</ul>';
	
	html += '<ul>';
	$.each(menus, function(index, element) {
		var menuName = element.name;
		html += '<li class="list-unstyled open-menu"><div>'+menuName+'<span class="pull-right glyphicon glyphicon-chevron-right"></span></div><ul class="second-level">';
		$.each(element.pages, function(index, element){
			var pageName = element.name;
			var pageId = element.id;
			$.each(element.levs, function(index, element){
				if(parseInt(element) <= level)
					html += '<li class="sidebar-chooser list-unstyled" id="'+pageId+'"><div>'+pageName+'</div></li>';
				
			});
		});
		html += '</ul></li>';
	});
	
	html += '</ul>';


	menuRoot.html(html);
	menuRoot.find('.second-level').hide();
	
	//dont slide up and down infinitely
	$('.second-level').click(function(event) {
		event.stopPropagation();
		event.preventDefault();
	});
	
	//slide down the menu
	$('.open-menu').click(function(){
		$('#locale-menu .second-level').slideUp();
		$('.open-menu .glyphicon').removeClass('glyphicon-chevron-down').addClass('glyphicon-chevron-right');
		
		$(this).children('ul').slideDown();
		$(this).find('.glyphicon').removeClass('glyphicon-chevron-right').addClass('glyphicon-chevron-down');
	});
	
	//menu
	$('.sidebar-chooser').click(function(){
		window.surveyCurrentPage = $(this).attr('id');
		window.surveyCurrentSpecial = '';
		reloadV();
		$('#left-sidebar').removeClass('active');
		toggleOverlay();
	});
	
	//review link 
	$('.review-link').click(function() {
		$('#left-sidebar').removeClass('active');
		toggleOverlay();
		$('#OtherSection').hide();
		if($(this).data('query')) {
            window.location= survURL+'?'+$(this).data('url')+'&_=' + surveyCurrentLocale;
		}
		else {
			window.surveyCurrentSpecial = $(this).data('url');
			surveyCurrentId = '';
			surveyCurrentPage = '';
			reloadV();
		}
	});
	
	//forum link 
	$('#forum-link').click(function() {
		window.surveyCurrentSpecial = 'forum';
		surveyCurrentId = '';
		surveyCurrentPage = '';
		reloadV();
	});	
	
	if(surveyCurrentLocale) {
		$('a[data-original-title="'+surveyCurrentLocale+'"]').click();
		$('#title-coverage').show();
	}
	
	//reopen the menu to the current page
	if(surveyCurrentPage) {
		var menu = $('#locale-menu #'+surveyCurrentPage);
		menu.closest('.open-menu').click();
	}
}

//force to open the sidebar 
function forceSidebar() {
	searchRefresh();
	$('#left-sidebar').mouseenter();
}

//refresh the search field
function searchRefresh() {
	$('.local-search').val('');
	$('.local-search').keyup();
}

//toggle the overlay of the menu
var toToggleOverlay;
function toggleOverlay(){
	var overlay = $('#overlay');
	var sidebar = $('#left-sidebar');
	if(!sidebar.hasClass('active')) {
		overlay.removeClass('active');
		toToggleOverlay = true;

		setTimeout(function(){
			if(toToggleOverlay)
				overlay.css('z-index', '-10');
		},500 /* half a second */);
	}
	else {
		toToggleOverlay = false;
		overlay.css('z-index','1000');
		overlay.addClass('active');
	}
}

/**
 * @method hideOverlayAndSidebar
 * hide both the 
 */
function hideOverlayAndSidebar(){
	var sidebar = $('#left-sidebar');
	sidebar.removeClass('active');
	toggleOverlay();
}

//show the help popup in the center of the screen 
var oldTypePopup = '';
function popupAlert(type, content, head, aj, dur) {
	var ajax = (typeof aj === "undefined") ? "" : aj;
	var header = (typeof aj === "undefined") ? "" : head; 
	var duration = (typeof dur === "undefined") ? 4000 /* four seconds */ : dur; 
	var alert = $('#progress').closest('.alert');
	alert.removeClass('alert-warning').removeClass('alert-info').removeClass('alert-danger').removeClass('alert-success');
	alert.addClass('alert-'+type);
	$('#progress_oneword').html(content);
	$('#progress_ajax').html(ajax);
	$('#specialHeader').html(header);
	if(header != "")
		$('#specialHeader').show();
	else
		$('#specialHeader').hide();
	
	if(oldTypePopup != type) {
		if(!alert.is(':visible')) {
			alert.fadeIn();
			if(duration > 0)
				setTimeout(function() { alert.fadeOut();}, duration);
			
		}
		oldTypePopup = type;

	}
	
		
}
//set the content for the instruction menu
function setHelpContent(content) {
	$('#help-content').html(content);
}

function setManageContent(content) {
	$('#manage-content').html(content);
}

//create/update the pull down menu popover
function interceptPulldownLink(event) {
	var menu = $(this).closest('.pull-menu');
	menu.popover('destroy').
	popover({placement:"bottom", 
			 html:true, 
			 content:menu.children('ul').html(), 
			 trigger:"manual",
			 delay:1500,
			 template: '<div class="popover" onmouseover="$(this).mouseleave(function() {$(this).fadeOut(); });"><div class="arrow"></div><div class="popover-inner"><h3 class="popover-title"></h3><div class="popover-content"><p></p></div></div></div>'

    }).click(function(e) {
        e.preventDefault() ;
    }).popover('show');

	event.preventDefault();
	event.stopPropagation();
}


//test if we are in the dashboard
function isDashboard() {
	return surveyCurrentSpecial == "r_vetting_json";
}

//handle new value submission
function addValueVote(td, tr, theRow, newValue, newButton) {
     	tr.inputTd = td; // cause the proposed item to show up in the right box
		handleWiredClick(tr,theRow,"",{value: newValue},newButton);
}

//transform input + submit button to the add button for the "add translation"
function toAddVoteButton(btn) {
	btn.className = "btn btn-primary";
	btn.title = "Add";
	btn.type = "submit";
	btn.innerHTML = '<span class="glyphicon glyphicon-plus"></span>';
	$(btn).parent().popover('destroy');
	$(btn).tooltip('destroy').tooltip();
	$(btn).closest('form').next('.subSpan').show();
	
	$(btn).parent().children('input').remove();
}

//transform the add button to a submit
function toSubmitVoteButton(btn) {
	btn.innerHTML = '<span class="glyphicon glyphicon-ok-circle"></span>';
	btn.className = "btn btn-success vote-submit";
	btn.title = "Submit";
	
	
	$(btn).tooltip('destroy').tooltip();

	$(btn).closest('form').next('.subSpan').hide();
	return btn;
}



//add some label with a tooltip to every icon 
function labelizeIcon() {
	
	var icons = [
	             {
	            	 selector:'.d-dr-approved',
	                 type:'success',
	                 text:'Approved',
	                 title:'The "Proposed" (winning) value will be in the release.'
	             },
	             {
	            	 selector:'.d-dr-contributed',
	                 type:'success',
	                 text:'Contributed',
	                 title:'The "Proposed" (winning) value will be in the release (with a slightly lower status).'
	             },
	             {
	            	 selector:'.d-dr-provisional',
	                 type:'warning',
	                 text:'Provisional',
	                 title:'There is a "Proposed" (winning) value, but it doesn\'t have enough votes.'
	             },
	             {
	            	 selector:'.d-dr-unconfirmed',
	                 type:'warning',
	                 text:'Unconfirmed',
	                 title:'There is a "Proposed" (winning) value, but it doesn\'t have enough votes.'
	             },
	             {
	            	 selector:'.d-dr-inherited-provisional',
	                 type:'inherited-provisional',
	                 text:'Inherited and Provisional',
	                 title:'The "Proposed" (winning) value is inherited and provisional.'
	             },
	             {
	            	 selector:'.d-dr-inherited-unconfirmed',
	                 type:'inherited-unconfirmed',
	                 text:'Inherited and Unconfirmed',
	                 title:'The "Proposed" (winning) value is inherited and unconfirmed.'
	             },
	             {
	            	 selector:'.d-dr-missing',
	                 type:'warning',
	                 text:'Missing',
	                 title:'There is no winning value. The inherited value will be used.'
	             },
	             {
	            	 selector:'.i-star',
	                 type:'primary',
	                 text:'Baseline',
	                 title:'The baseline value, which was approved in the last release, plus latest XML fixes by the Technical Committee, if any.'
	             }
                 ];
	
		$.each(icons, function(index, element) {
			$(element.selector).each(function() {
				if($(this).next('.label').length !== 0)
					$(this).next().remove();
				$(this).after('<div class="label label-'+element.type+' label-icon">'+element.text+'</div>');
				$(this).next().tooltip({title:element.title});
			});		
		});
		
	

}

function initFeedBack() {
	var url = contextPath +'/feedback';
	$('#feedback > div').click(function(){
		$(this).hide();
		$(this).next().show();
		$('#feedback input').focus();
	});
	
	$('#closebutton').click(function() {
		var parent = $(this).parent();
		parent.hide();
		parent.prev().show();
		parent.prev().text('Feedback ?');
	});
	
	$('#feedback [type=submit]').click(function(event) {
		
		if($('#feedback textarea').val()) {
			$.post(url, $('#feedback form').serializeArray(),function(data) {
				$('#feedback textarea').val('');
				$('#feedback > div').text('Thank you !').show();
				$('#feedback > form').hide();
			});
		}
		event.stopPropagation();
		event.preventDefault();
		return false;
	});
}


function toggleRightPanel() {
	var main = $('#main-row > .col-md-9');
	if(!main.length) {
		showRightPanel();
	}
	else {
		hideRightPanel();
	}
}



function showRightPanel() {
	$('#main-row > .col-md-12, #nav-page > .col-md-12').addClass('col-md-9').removeClass('col-md-12');
	$('#main-row #itemInfo').show();
}



function hideRightPanel() {
	$('#main-row > .col-md-9, #nav-page > .col-md-9').addClass('col-md-12').removeClass('col-md-9');
	$('#main-row #itemInfo').hide();
}



function toggleHeader() {
	if($('.menu-position:visible').length)
		hideHeader();
	else
		showHeader();
}



function showHeader() {
	$('#main-row').css('padding-top','147px');
	$('.menu-position').show();
}

function hideHeader() {
	$('#main-row').css('padding-top','100px');
	$('.menu-position').hide();
}
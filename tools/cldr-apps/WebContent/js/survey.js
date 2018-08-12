// survey.js  -Copyright (C) 2012,2016 IBM Corporation and Others. All Rights Reserved.
// move anything that's not dynamically generated here.

// These need to be available @ bootstrap time.

/**
 * @module survey.js - SurveyTool main JavaScript stuff
 */

// TODO: replace with AMD [?] loading
dojo.require("dojo.i18n");
dojo.require("dojo.string");
window.haveDialog = false;

/*
 * INHERITANCE_MARKER indicates that the value of a candidate item is inherited.
 * Compare INHERITANCE_MARKER in CldrUtility.java.
 */
const INHERITANCE_MARKER = "↑↑↑";

/**
 * @class Object
 * @method keys
 */
if(!Object.prototype.keys && !Object.keys) {
	console.log("fixing missing Object.keys");
	Object.keys = function(x) {
		var r = [];
		for (var j in x) {
			r.push(j);
		}
		return r;
	};
}

/**
 * @class Array
 * @method isArray
 */
if(!Array.prototype.isArray && !Array.isArray) {
	console.log("fixing missing Array.isArray() ");
	Array.isArray = function(x) {
		if(x === null) return false;
		return x instanceof Array;   // if this doesn't work, we're in trouble.
	};
}

/**
 * @class String
 * @method trim
 */
if(!String.prototype.trim && !String.trim) {
	console.log("TODO fix broken String.trim() ");
	String.prototype.trim = function(x) {
		return x;
	};
}

/**
 * Format a date and time for display in a forum post.
 *
 * @param x the number of seconds since 1970-01-01
 * @returns the formatted date and time as a string
 *
 * Like "2018-05-16 13:45" per cldr-dev@unicode.org.
 */
function fmtDateTime(x) {
	const d = new Date(x);
    function pad(n) {
        return (n < 10) ? '0' + n : n;
    }
    return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) +
    	   ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes());
};

/**
 * @class GLOBAL
 */

function isReport(str) {
	return (str[0]=='r' && str[1]=='_');
}

/**
 * @method removeClass
 * remove a CSS class from a node
 * @param {Node} obj
 * @param {String} className
 */
function removeClass(obj, className) {
	if(!obj) return obj;
	if(obj.className.indexOf(className)>-1) {
		obj.className = obj.className.substring(className.length+1);
	}
	return obj;
}

/**
 * @method addClass
 * add a CSS class from a node
 * @param {Node} obj
 * @param {String} className
 */
function addClass(obj, className) {
	if(!obj) return obj;
	if(obj.className.indexOf(className)==-1) {
		obj.className = className+" "+obj.className;
	}
	return obj;
}

/**
 * @method post2text
 */
function post2text(text) {
	if(text===undefined || text===null) {
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
 * Remove all subnodes
 * @method removeAllChildNodes
 * @param {Node} td
 */
function removeAllChildNodes(td) {
	if(td==null) return;
	while(td.firstChild) {
		td.removeChild(td.firstChild);
	}
}

/**
 * set/remove style.display
 * @method setDIsplayed
 */
function setDisplayed(div, visible) {
	if(div===null) {
		console.log("setDisplayed: called on null");
		return;
	} else if(div.domNode) {
		setDisplayed(div.domNode, visible); // recurse, it's a dijit
	} else if(!div.style) {
		console.log("setDisplayed: called on malformed node " + div + " - no style! " + Object.keys(div));
	} else {
		if(visible) {
			div.style.display = '';
		} else {
			div.style.display = 'none';
		}
	}
}

/**
 * Section flipping class
 * @class Flipper
 */
function Flipper(ids) {
	// TODO amd require 'by'
	this._panes = [];
	this._killfn = [];
	this._map = {};
	for(var k in ids) {
		var id = ids[k];
		var node = dojo.byId(id);
		// TODO if node==null throw
		if(this._panes.length > 0) {
			setDisplayed(node,false); // hide it
		} else {
			this._visible = id;
		}
		this._panes.push(node);
		this._map[id]=node;
	}
}

/**
 * @method flipTo
 * @param {String} id
 * @param {Node} node - if non null - replace new page with this
 */
Flipper.prototype.flipTo = function(id, node) {
	if(!this._map[id]) return; // TODO throw
	if(this._visible == id && (node === null)) {
		return; // noop - unless adding s'thing
	}
	setDisplayed(this._map[this._visible], false);
	for(var k in this._killfn) {
		this._killfn[k]();
	}
	this._killfn = []; // pop?
	if(node!==null && node !== undefined) {
		removeAllChildNodes(this._map[id]);
		if(node.nodeType>0) {
			this._map[id].appendChild(node);
		} else for( var kk in node) {
			// it's an array, add all
			this._map[id].appendChild(node[kk]);
		}
	}
	setDisplayed(this._map[id], true);
	this._visible = id;
	return this._map[id];
};

/**
 * @method get
 * @param id page id or null for current
 * @returns
 */
Flipper.prototype.get = function(id) {
	if(id) {
		return this._map[id];
	} else {
		return this._map[this._visible];
	}
};

/**
 * @method flipToEmpty
 * @param id
 */
Flipper.prototype.flipToEmpty = function(id) {
	return this.flipTo(id, []);
};

/**
 * killFn is called on next flip
 * @method addKillFn
 * @param killFn the functino to call. No params.
 */
Flipper.prototype.addKillFn = function(killFn) {
	this._killfn.push(killFn);
};

/**
 * showfn is called, result is added to the div.  killfn is called when page is flipped.
 * @method addUntilFlipped
 * @param showFn
 * @param killFn
 */
Flipper.prototype.addUntilFlipped = function addUntilFlipped(showFn, killFn) {
	this.get().appendChild(showFn());
	this.addKillFn(killFn);
};

/*
 * LocaleMap
 * @class LocaleMap
 */
/**
 * ctor
 * @function LocaleMap
 * @param aLocmap the map object from json
 */
function LocaleMap(aLocmap) {
	this.locmap = aLocmap;
}

/**
 * Run the locale id through the idmap.
 * @function canonicalizeLocaleId
 * @param menuMap the map
 * @param locid or null for surveyCurrentLocale
 * @return canonicalized id, or unchanged
 */
LocaleMap.prototype.canonicalizeLocaleId = function canonicalizeLocaleId(locid) {
	if(locid === null) {
		locid = surveyCurrentLocale;
	}
	if(locid === null || locid === '') {
		return null;
	}

	if(this.locmap) {
		if(this.locmap.idmap && this.locmap.idmap[locid]) {
			locid = this.locmap.idmap[locid]; // canonicalize
		}
	}
	return locid;
};

window.linkToLocale = function linkToLocale(subLoc) {
	return "#/"+subLoc+"/"+surveyCurrentPage+"/"+surveyCurrentId;
};

/**
 * Linkify text like '@de' into some link to German.
 * @function linkify
 * @param str (html)
 * @return linkified str (html)
 */
LocaleMap.prototype.linkify = function linkify(str) {
	var out = "";
	var re = /@([a-zA-Z0-9_]+)/g;
	var match;
	var fromLast = 0;
	while((match = re.exec(str)) != null) {
		var bund = this.getLocaleInfo(match[1]);
		if(bund) {
			out = out + str.substring(fromLast,match.index); // pre match
			if ( match[1] == surveyCurrentLocale ) {
				out = out + this.getLocaleName(match[1]);
			} else {
				out = out + "<a href='"+linkToLocale(match[1])+"' title='"+match[1]+"'>" + this.getLocaleName(match[1]) + "</a>";
			}
		} else {
			out = out + match[0]; // no link.
		}
		fromLast = re.lastIndex;
	}
	out = out + str.substring(fromLast, str.length);
	return out;
};

/**
 * Return the locale info entry
 * @method getLocaleInfo
 * @param menuMap the map
 * @param locid the id - should already be canonicalized
 * @return the bundle or null
 */
LocaleMap.prototype.getLocaleInfo = function getLocaleInfo(locid) {
	if(this.locmap && this.locmap.locales && this.locmap.locales[locid]) {
		return this.locmap.locales[locid];
	} else {
		return null;
	}
};

/**
 * Return the locale name,
 * @method getLocaleName
 * @param menuMap the map
 * @param locid the id - will canonicalize
 * @return the display name - or else the id
 */
LocaleMap.prototype.getLocaleName = function getLocaleName(locid) {
	locid = this.canonicalizeLocaleId(locid);
	var bund = this.getLocaleInfo( locid);
	if(bund && bund.name ) {
		return bund.name;
	} else {
		return locid;
	}
};

/**
 * Return the locale name,
 * @method getLocaleName
 * @param menuMap the map
 * @param locid the id - will canonicalize
 * @return the display name - or else the id
 */
LocaleMap.prototype.getRegionAndOrVariantName = function getRegionAndOrVariantName(locid) {
	locid = this.canonicalizeLocaleId(locid);
	var bund = this.getLocaleInfo( locid);
	if(bund) {
		var ret = "";
		if (bund.name_rgn) {
			ret = ret + bund.name_rgn;
		}
		if (bund.name_var) {
			ret = ret + " ("+bund.name_var+")";
		}
		if(ret != "") {
			return ret; // region OR variant OR both
		}
		if(bund.name) {
			return bund.name; // fallback to name
		}
	}
	return locid; // fallbcak to locid
};

/**
 * Return the locale language
 * @method getLanguage
 * @param locid
 * @returns the language portion
 */
LocaleMap.prototype.getLanguage = function getLanguage(locid) {
	return locid.split('_')[0].split('-')[0];
};

/**
 * @class XpathMap
 * This manages xpid / strid / PathHeader etc mappings.
 * It is a cache, and gets populated with data 'volunteered' by page loads, so that
 * hopefully it doesn't need to do any network operations on its own.
 * However, it MAY BE capable of filling in any missing data via AJAX. This is why its operations are async.
 */
function XpathMap() {
	/**
	 * Maps strid (hash) to info struct.
	 * The info struct is the basic unit here, it looks like the following:
	 *   {
	 *       hex: '20fca8231d41',
	 *       path: '//ldml/shoeSize',
	 *       id:  1337,
	 *       ph: ['foo','bar','baz']  **TBD**
	 *   }
	 *
	 *  All other hashes here are just alternate indices into this data.
	 *
	 * @property XpathMap.stridToInfo
	 */
	this.stridToInfo = {};
	/**
	 * Map xpid (such as 1337) to info
	 * @property XpathMap.xpidToInfo
	 */
	this.xpidToInfo = {};
	/**
	 * Map xpath (//ldml/...) to info.
	 * @property XpathMap.xpathToInfo
	 */
	this.xpathToInfo = {};
};

/**
 * This function will do a search and then call the onResult function.
 * Priority order for search:  hex, id, then path.
 * @function get
 * @param search {Object} the object to search for
 * @param search.hex {String} optional - search by hex id
 * @param search.path {String} optional - search by xpath
 * @param search.id {Number} optional - search by number (String will be converted to Number)
 * @param onResult - will be called with one parameter that looks like this:
 *  { search, err, result } -  'search' is the input param,
 * 'err' if non-null is any error, and 'result' if non-null is the result info struct.
 * If there is an error, 'result' will be null. Please do not modify 'result'!
 */
XpathMap.prototype.get = function get(search, onResult) {
	// see if we have anything immediately
	result = null;
	if(!result && search.hex) {
		result = this.stridToInfo[search.hex];
	}
	if(!result && search.id) {
		if(typeof search.id !== Number) {
			search.id = new Number(search.id);
		}
		result = this.xpidToInfo[search.id];
	}
	if(!result && search.path) {
		result = this.xpathToInfo[search.path];
	}
	if(result) {
		onResult({search:search, result:result});
	} else {
		stdebug("XpathMap search failed for " + JSON.stringify(search) +  " - doing rpc");
		var querystr = null;
		if(search.hex) {
			querystr = search.hex;
		} else if(search.path) {
			querystr = search.path;
		} else if(search.id) {
			querystr = "#"+search.id;
		} else {
			querystr = ''; // error
		}
		require(["dojo/request"], function(request) {
			request
			.get('SurveyAjax', {handleAs: 'json',
								query: {
									what: 'getxpath',
									xpath: querystr
								}})
			.then(function(json) {
				if(json.getxpath) {
					xpathMap.put(json.getxpath); // store back first, then
					onResult({search: search, result:json.getxpath}); // call
				} else {
					onResult({search: search, err:'no results from server'});
				}
			})
			.otherwise(function(err) {
				onResult({search: search, err:err});
			});
		});
	}
};

/**
 * Contribute some data to the map.
 * @function contribute
 */
XpathMap.prototype.put = function put(info) {
	if(!info || !info.id || !info.path || !info.hex || !info.ph) {
		stdebug("XpathMap: rejecting incomplete contribution " + JSON.stringify(info));
	} else if(this.stridToInfo[info.hex]) {
		stdebug("XpathMap: rejecting duplicate contribution " + JSON.stringify(info));
	} else {
		this.stridToInfo[info.hex] =
			this.xpidToInfo[info.id] =
				this.xpathToInfo[info.path] =
					info;
		stdebug("XpathMap: adding contribution " + JSON.stringify(info));
	}
};

/**
 * Format a pathheader array.
 * @function formatPathHeader
 * @param ph {Object} pathheaer struct (Section/Page/Header/Code)
 * @return {String}
 */
XpathMap.prototype.formatPathHeader = function formatPathHeader(ph) {
	if(!ph) {
		return '';
	} else {
		var phArray = [ph.section, ph.page, ph.header, ph.code];
		return phArray.join(' | '); // type error - valid?
	}
};

/**
 * @class GLOBAL
 */
var xpathMap = new XpathMap();



/**
 * Global items
 * @class GLOBAL
 */

/**
 * SurveyToolUI (localization) object. Preloaded with a few strings while we wait for the resources to load.
 *
 * @property stui
 */
var stui = {
		online: "Online",
		error_restart: "(May be due to SurveyTool restart on server)",
		error: "Disconnected: Error", "details": "Details...",
		disconnected: "Disconnected",
		startup: "Starting up...",
		ari_sessiondisconnect_message: "Your session has been disconnected.",
		str : function(x) { if(stui[x]) return stui[x]; else return ""; },
		sub : function(x,y) { return dojo.string.substitute(stui.str(x), y);}

};

var stuidebug_enabled=(window.location.search.indexOf('&stui_debug=')>-1);


if(!stuidebug_enabled) {
	/**
	 * SurveyToolUI string loading function
	 * @method stui_str
	 */
	stui_str = function(x) {
	    if(stui) {
	    	return stui.str(x);
	    } else {
	    	return x;
	    }
	};
} else {
	stui_str = function(x) { return "stui["+x+"]"; };
}

/**
 * Is the keyboard 'busy'? i.e., it's a bad time to change the DOM
 * @method isInputBusy
 */
function isInputBusy() {
	if(!window.getSelection) return false;
	var sel = window.getSelection();
	if(sel && sel.anchorNode && sel.anchorNode.className && sel.anchorNode.className.indexOf("dijitInp")!=-1) {
		return true;
	}
	return false;
}

/**
 * Create a DOM object with the specified text, tag, and HTML class.
 * Applies (classname)+"_desc" as a tooltip (title).
 * @method createChunk
 * @param {String} text textual content of the new object, or null for none
 * @param {String} tag which element type to create, or null for "span"
 * @param {String} className CSS className, or null for none.
 * @return {Object} new DOM object
 */
function createChunk(text, tag, className) {
	if(!tag) {
		tag="span";
	}
	var chunk = document.createElement(tag);
	if(className) {
		chunk.className = className;
	}
	if(text) {
		chunk.appendChild(document.createTextNode(text));
	}
	return chunk;
}

/**
 * Uppercase the first letter of a sentence
 * @return {String} string with first letter uppercase
 */
String.prototype.ucFirst = function() {
    return this.charAt(0).toUpperCase() + this.slice(1);
};

/**
 * Create a 'link' that goes to a function. By default it's an 'a', but could be a button, etc.
 * @param strid  {String} string to be used with stui.str
 * @param fn {function} function, given the DOM obj as param
 * @param tag {String}  tag of new obj.  'a' by default.
 * @return {Element} newobj
 */
function createLinkToFn(strid, fn, tag) {
	if(!tag)  {
		tag = 'a';
	}
	var msg = stui.str(strid);
	var obj = document.createElement(tag);
	obj.appendChild(document.createTextNode(msg));
	if(tag=='a') {
		obj.href ='';
	}
	listenFor(obj, "click", function(e) {
		fn(obj);
		stStopPropagation(e);
		return false;
	});
	return obj;
}

function createGravitar(user) {
	if(user.emailHash) {
		var gravatar = document.createElement("img");
		gravatar.src = 'http://www.gravatar.com/avatar/'+user.emailHash+'?d=identicon&r=g&s=32';
		gravatar.title = 'gravatar - http://www.gravatar.com';
		return gravatar;
	} else {
		return document.createTextNode('');
	}
}

/**
 * Create a DOM object referring to a user.
 * @method createUser
 * @param {JSON} user - user struct
 * @return {Object} new DOM object
 */
function createUser(user) {
	var userLevelLc = user.userlevelName.toLowerCase();
	var userLevelClass = "userlevel_"+userLevelLc;
	var userLevelStr = stui_str(userLevelClass);
	var div = createChunk(null,"div","adminUserUser");
	div.appendChild(createGravitar(user));
	div.userLevel = createChunk(userLevelStr,"i",userLevelClass);
	div.appendChild(div.userLevel);
	div.appendChild(div.userName = createChunk(user.name,"span","adminUserName"));
	if(!user.orgName) {
	   user.orgName = user.org;
	}
	div.appendChild(div.userOrg = createChunk(user.orgName + ' #'+user.id,"span","adminOrgName"));
    div.appendChild(div.userEmail = createChunk(user.email,"address","adminUserAddress"));
	return div;
}

/**
 * Create a DOM object referring to this forum post
 * @param {Object} json - options
 * @param {Array} j.ret - forum post data
 * @return {Object} new DOM object
 */
function parseForumContent(json) {
	var forumDiv = document.createDocumentFragment();

	// json.ret has posts in reverse order (newest first).
	var postDivs={}; //  postid -> div
	var topicDivs={}; // xpath -> div or "#123" -> div
	var postHash={}; // postid -> item

	// first, collect the posts.
	for (num in json.ret) {
		postHash[json.ret[num].id]=json.ret[num];
	}

	// now, collect the threads
	function threadId(post) {
		if(post.parent >= 0 && postHash[post.parent]) {
			// if  the parent exists.
			return threadId(postHash[post.parent]);
		}
		if(post.xpath) {
			return post.locale + "|" + post.xpath; // item post
		} else {
			return post.locale + "|#"+post.id; // non-item post
		}
	}

	// next, add threadIds and create the topic divs
	for ( num in json.ret) {
		var post = json.ret[num];
		post.threadId = threadId(post);

		if(!topicDivs[post.threadId]) {
			// add the topic div
			var topicDiv = document.createElement('div');
			topicDiv.className = 'well well-sm postTopic';
			var topicInfo = createChunk("", "h4", "postTopicInfo");
			if(!json.noItemLink) {
				topicDiv.appendChild(topicInfo);
				if(post.locale) {
					var localeLink = createChunk(locmap.getLocaleName(post.locale), "a", "localeName");
					if(post.locale != surveyCurrentLocale) {
						localeLink.href = linkToLocale(post.locale);
						//localeLink.className = localeLink.className + " label label-warning";
					}
					topicInfo.appendChild(localeLink);
				}
			}
			if(!post.xpath) {
				topicInfo.appendChild(createChunk(post2text(post.subject), "span", "topicSubject"));
			} else {
				if(!json.noItemLink) {
					var itemLink = createChunk(stui.str("forum_item"), "a", "pull-right postItem glyphicon glyphicon-zoom-in");
					itemLink.href = "#/"+post.locale+"//"+post.xpath;
					topicInfo.appendChild(itemLink);
					(function(topicInfo){
						var loadingMsg = createChunk(stui.str("loading"), "i", "loadingMsg");
						topicInfo.appendChild(loadingMsg);
						xpathMap.get({hex: post.xpath}, function(o){
						if(o.result) {
							topicInfo.removeChild(loadingMsg);
							var itemPh = createChunk(xpathMap.formatPathHeader(o.result.ph), "span", "topicSubject");
							itemPh.title = o.result.path;
							topicInfo.appendChild(itemPh);
						}
					});})(topicInfo);
				}
			}
			topicDivs[post.threadId] = topicDiv;
			topicDiv.id = "fthr_"+post.threadId;

			// add to the div
			forumDiv.appendChild(topicDiv);
		}
	}
	// Now, top to bottom, just create the post divs
	for(num in json.ret) {
		var post = json.ret[num];

		var subpost = createChunk("","div","post"); // was: subpost
		// Don't add subpost to the DIV yet - will reparent into the topic Divs
		///  --forumDiv.appendChild(subpost);
		postDivs[post.id] = subpost;
		subpost.id = "fp" + post.id;

		var headingLine = createChunk("", "h4", "selected");

		// If post.posterInfo is undefined, don't crash; insert "[Poster no longer active]".
		if (!post.posterInfo) {
			headingLine.appendChild(createChunk("[Poster no longer active]", "span", ""));
		} else {
			var gravitar = createGravitar(post.posterInfo);
			gravitar.className = "gravitar pull-left";
			subpost.appendChild(gravitar);
			if (post.posterInfo.id == surveyUser.id) {
				headingLine.appendChild(createChunk(stui.str("user_me"), "span", "forum-me"));
			} else {
				var usera = createChunk(post.posterInfo.name+' ', "a", "");
				if(post.posterInfo.email) {
					usera.appendChild(createChunk("", "span", "glyphicon glyphicon-envelope"));
					usera.href = "mailto:" + post.posterInfo.email;
				}
				headingLine.appendChild(usera);
				headingLine.appendChild(document.createTextNode(' ('+post.posterInfo.org+') '));
			}
			var userLevelChunk = createChunk(stui.str("userlevel_"+post.posterInfo.userlevelName), "span", "userLevelName label-info label");
			userLevelChunk.title = stui.str("userlevel_"+post.posterInfo.userlevelName+"_desc");
			headingLine.appendChild(userLevelChunk);
		}
		var date = fmtDateTime(post.date_long);
		if (post.version) {
			date = "[v" + post.version + "] " + date;
		}
		var dateChunk = createChunk(date, "span", "label label-primary pull-right forumLink");
		(function(post) {
			listenFor(dateChunk, "click", function(e) {
				if (post.locale && locmap.getLanguage(surveyCurrentLocale) != locmap.getLanguage(post.locale)) {
					surveyCurrentLocale = locmap.getLanguage(post.locale);
				}
				surveyCurrentPage = '';
				surveyCurrentId = post.id;
				replaceHash(false);
				if(surveyCurrentSpecial != 'forum') {
					surveyCurrentSpecial = 'forum';
					reloadV();
				}
				return stStopPropagation(e);
			});
		})(post);
		headingLine.appendChild(dateChunk);
		subpost.appendChild(headingLine);

		var subSubChunk = createChunk("","div","postHeaderInfoGroup");
		subpost.appendChild(subSubChunk);
		{
			var subChunk = createChunk("","div","postHeaderItem");
			subSubChunk.appendChild(subChunk);
			subChunk.appendChild(createChunk(post2text(post.subject),"b","postSubject"));
		}

		// actual text
		var postText = post2text(post.text);
		var postContent;
		subpost.appendChild(postContent = createChunk(postText, "div","postContent"));
		if(json.replyButton) {
			var replyButton = createChunk(stui.str("forum_reply"), "button", "btn btn-default btn-sm");
			(function(post){ listenFor(replyButton, "click", function(e) {
				openReply({
					locale: surveyCurrentLocale,
					//xpath: '',
					replyTo: post.id,
					replyData: post,
					onReplyClose: json.onReplyClose
				});
				stStopPropagation(e);
				return false;
			});})(post);
			subpost.appendChild(replyButton);
		}

		// reply link
		if(json.replyStub) {
			var replyChunk = createChunk("Reply (leaves this page)","a","postReply");
			replyChunk.href = json.replyStub + post.id;
			subpost.appendChild(replyChunk);
		}
	}
	// reparent any nodes that we can
	for(num in json.ret) {
		var post = json.ret[num];
		if(post.parent != -1) {
			stdebug("reparenting " + post.id + " to " + post.parent);
			if(postDivs[post.parent]) {
				if(!postDivs[post.parent].replies) {
					// add the "replies" area
					stdebug("ADding replies area to " + post.parent );
					postDivs[post.parent].replies = createChunk("","div","postReplies");
					postDivs[post.parent].appendChild(postDivs[post.parent].replies);
				}
				// add to new location
				postDivs[post.parent].replies.appendChild(postDivs[post.id]);
			} else {
				// The parent of this post was deleted.
				stdebug("The parent of post #" + post.id + " is " + post.parent + " but it was deleted or not visible");
				// link it in somewhere
				topicDivs[post.threadId].appendChild(postDivs[post.id]);
			}
		} else {
			// 'top level' post, put it into the forumdiv.
			topicDivs[post.threadId].appendChild(postDivs[post.id]);
		}
	}

	// Now, bubble up recent posts to the top
	for(var num=json.ret.length-1;num>=0;num--) {
		var post = json.ret[num];
		var topicDiv = topicDivs[post.threadId];
		forumDiv.removeChild(topicDiv);
		forumDiv.insertBefore(topicDiv, forumDiv.firstChild);
	}
	return forumDiv;
}

/**
 * Used from within event handlers. cross platform 'stop propagation'
 * @method stStopPropagation
 * @param e event
 * @returns
 */
function stStopPropagation(e) {
	if(!e) {
		return false;
	} else if(e.stopPropagation) {
		return e.stopPropagation();
	} else if(e.cancelBubble) {
		return e.cancelBubble();
	} else {
		// hope for the best
		return false;
	}
}

/**
 * is the ST disconnected
 * @property disconnected
 */
var disconnected = false;

/**
 * Is debugging enabled?
 * @property stdebug_enabled
 */
var stdebug_enabled = (window.location.search.indexOf('&stdebug=') > -1);

/**
 * Queue of XHR requests waiting to go out
 * @property queueOfXhr
 */
var queueOfXhr=[];

/**
 * The current timeout for processing XHRs
 * @property queueOfXhrTimeout
 */
var queueOfXhrTimeout=null;


var myLoad0= null;
var myErr0 = null;

var processXhrQueue = function() {
	if(disconnected) return;
	if(!queueOfXhr || queueOfXhr.length==0) {
		queueOfXhr=[];
		stdebug("PXQ: 0");
		queueOfXhrTimeout=null;
		return; // nothing to do, reset.
	} else {
		var top =queueOfXhr.shift();

		top.load2 = top.load;
		top.err2 = top.err;
		top.load=function(){return myLoad0(top,arguments); };
		top.err=function(){return myErr0(top,arguments); };
		top.startTime = new Date().getTime();
		if(top.postData || top.content) {
			stdebug("PXQ("+queueOfXhr.length+"): dispatch POST " + top.url);
			dojo.xhrPost(top);
		} else {
			stdebug("PXQ("+queueOfXhr.length+"): dispatch GET " + top.url);
			dojo.xhrGet(top);
		}
	}
};

function xhrSetTime(top) {
	top.stopTime = new Date().getTime();
	top.tookTime = top.stopTime-top.startTime;
	stdebug("PXQ("+queueOfXhr.length+"): time took= " + top.tookTime);
}

var xhrQueueTimeout = 3;
myLoad0 = function(top,args) {
	xhrSetTime(top);
	stdebug("myLoad0!:" + top.url + " - a="+args.length);
	var r = top.load2(args[0],args[1]);
	queueOfXhrTimeout = setTimeout(processXhrQueue, xhrQueueTimeout);
	return r;
};

myErr0 = function(top,args) {
	stdebug("myErr0!:" + top.url+ " - a="+args.toString());
	var r = top.err2.call(args[0],args[1]);
	queueOfXhrTimeout = setTimeout(processXhrQueue, xhrQueueTimeout);
	return r;
};

/**
 * Queue the XHR request.  It will be a GET *unless* either postData or content are set.
 * @param xhr
 */
function queueXhr(xhr) {
	queueOfXhr.push(xhr);
	stdebug("pushed:  PXQ="+queueOfXhr.length + ", postData: " + xhr.postData);
	if(!queueOfXhrTimeout) {
		queueOfXhrTimeout = setTimeout(processXhrQueue, xhrQueueTimeout);
	}
}

function stdebug(x) {
	if(stdebug_enabled) {
		console.log(x);
	}
}

stdebug('stdebug is enabled.');

var timerID = -1;

/**
 * Update the item, if it exists
 * @method updateIf
 * @param id ID of DOM node, or a Node itself
 * @param txt text to replace with - should just be plaintext, but currently can be HTML
 */
function updateIf(id, txt) {
	var something;
    if(id instanceof Node) {
    	something = id;
    } else {
   		something = document.getElementById(id);
    }
    if(something != null) {
        something.innerHTML = txt;  // TODO shold only use for plain text
    }
}

/**
 * Add an event listener function to the object.
 * @method listenFor
 * @param {DOM} what object to listen to (or array of them)
 * @param {String} what event, bare name such as 'click'
 * @param {Function} fn function, of the form:  function(e) { 	doSomething();  	stStopPropagation(e);  	return false; }
 * @param {String} ievent IE name of an event, if not 'on'+what
 * @return {DOM} returns the object what (or the array)
 */
function listenFor(whatArray, event, fn, ievent) {
    function listenForOne(what, event, fn, ievent) {
    	if(!(what._stlisteners)) {
    		what._stlisteners={};
    	}

    	if(what.addEventListener) {
    		if(what._stlisteners[event]) {
    			if(what.removeEventListener) {
    				what.removeEventListener(event,what._stlisteners[event],false);
    			} else {
    				console.log("Err: no removeEventListener on " + what);
    			}
    		}
    		what.addEventListener(event,fn,false);
    	} else {
    		if(!ievent) {
    			ievent = "on"+event;
    		}
    		if(what._stlisteners[event]) {
    			what.detachEvent(ievent,what._stlisteners[event]);
    		}
    		what.attachEvent(ievent,fn);
    	}
    	what._stlisteners[event]=fn;

    	return what;
    }

    if(Array.isArray(whatArray)) {
        for(var k in whatArray) {
            listenForOne(whatArray[k], event, fn, ievent);
        }
        return whatArray;
    } else {
        return listenForOne(whatArray, event, fn, ievent);
    }
}

/**
 * On click, select all
 * @method clickToSelect
 * @param {Node} obj to listen to
 * @param {Node} targ to select
 */
function clickToSelect(obj, targ) {
	if(!targ) targ=obj;
	listenFor(obj, "click", function(e) {
		if(window.getSelection) {
			window.getSelection().selectAllChildren(targ);
		}
		stStopPropagation(e);
		return false;
	});
	return obj;
}

var wasBusted = false;
var wasOk = false;
var loadOnOk = null;
var clickContinue = null;
var surveyNextLocaleStamp = 0;
var surveyNextLocaleStampId = '';

 /**
  * Mark the page as busted. Don't do any more requests.
  * @method busted
  */
 function busted() {
	 disconnected = true;
	 stdebug("disconnected.");
	 addClass(document.getElementsByTagName("body")[0], "disconnected");
 }

 var didUnbust = false;

 function unbust() {
	 didUnbust = true;
	 console.log("Un-busting");
	 progressWord="unbusted";
	 disconnected = false;
	 saidDisconnect = false;
	 removeClass(document.getElementsByTagName("body")[0], "disconnected");
	 wasBusted = false;
	 queueOfXhr=[]; // clear queue
	 clearTimeout(queueOfXhrTimeout);
	 queueOfXhrTimeout = null;
	 hideLoader();
	 saidDisconnect = false;
	 updateStatus(); // will restart regular status updates
 }

// hashtable of items already verified
var alreadyVerifyValue = {};

var showers={};

var deferUpdates = 0;
var deferUpdateFn = {};

/**
 * Process all deferred updates
 * @method doDeferredUpdates
 */
function doDeferredUpdates() {
	if(deferUpdateFn==null || deferUpdates>0 || isInputBusy()) {
		return;
	}

	for(i in deferUpdateFn) {
		if(deferUpdateFn[i]) {
			var fn = deferUpdateFn[i];
			deferUpdateFn[i]=null;
			stdebug(".. calling deferred update fn ..");
			fn();
		}
	}
}

/**
 * Set whether we are deferring or not. For example, call setDefer(true) on entering a text field, and setDefer(false) leaving it.
 * @method setDefer
 * @param {boolean} defer
 */
function setDefer(defer) {
	if(defer) {
		deferUpdates++;
	} else {
		deferUpdates--;
	}
	doDeferredUpdates();
	stdebug("deferUpdates="+deferUpdates);
}

/**
 * Note an update as deferred.
 * @method deferUpdate
 * @param {String} what type of item to defer  (must be unique- will overwrite)
 * @param {Function} fn function to register
 */
function deferUpdate(what, fn) {
	deferUpdateFn[what]=fn;
}

/**
 * Note an update as not needing deferral
 * @method undeferUpdate
 * @param {String} what the type of item to undefer
 */
function undeferUpdate(what) {
	deferUpdate(what, null);
}

/**
 * Perform or queue an update. Note that there's data waiting if we are deferring.
 * @method doUpdate
 * @param {String} what
 * @param {Function} fn function to call, now or later
 */
function doUpdate(what,fn) {
	if(deferUpdates>0 || isInputBusy()) {
		updateAjaxWord(stui_str('newDataWaiting'));
		deferUpdate(what,fn);
	} else {
		fn();
		undeferUpdate(what);
	}
}

/**
 * Process that the locale has changed under us.
 * @method handleChangedLocaleStamp
 * @param {String} stamp timestamp
 * @param {String} name locale name
 */
function handleChangedLocaleStamp(stamp,name) {
	if(disconnected) return;
	if(stamp <= surveyNextLocaleStamp) {
		return;
	}
	if(Object.keys(showers).length==0) {
        updateIf('stchanged_loc',name);
        var locDiv = document.getElementById('stchanged');
        if(locDiv) {
            locDiv.style.display='block';
        }
	} else {
		for(i in showers) {
			var fn = showers[i];
			if(fn) {
				fn();
			}
		}
	}
	stdebug("Reloaded due to change: " + stamp);
    surveyNextLocaleStamp = stamp;
}

var progressWord = null;
var ajaxWord = null;
var specialHeader = null;

/**
 * Update the 'status' if need be.
 * @method showWord
 */
function showWord() {
	var p = dojo.byId("progress");
	var oneword = dojo.byId("progress_oneword");
	if(oneword==null) { // nowhere to show
		return;
	}
	if(disconnected
			|| (progressWord&&progressWord=="disconnected")
			|| (progressWord&&progressWord=="error")
			) { // top priority
		popupAlert('danger',stopIcon +  stui_str(progressWord));
		busted(); // no further processing.
	} else if(ajaxWord) {
		p.className = "progress-ok";
	} else if(!progressWord || progressWord == "ok") {
		if(specialHeader) {
			p.className = "progress-special";
		} else {
			p.className = "progress-ok";
		}
	} else if(progressWord=="startup") {
		p.className = "progress-ok";
		popupAlert('warning',stui_str('online'));
	}
}

/**
 * Update our progress
 * @method updateProgressWord
 * @param {String} prog the status to update
 */
function updateProgressWord(prog) {
	progressWord = prog;
	showWord();
}

/**
 * Update ajax loading status
 * @method updateAjaxWord
 * @param {String} ajax
 */
function updateAjaxWord(ajax) {
	ajaxWord = ajax;
	showWord();
}

var saidDisconnect=false;

/**
 * @method showARIDialog
 * @param why
 * @param json
 * @param word
 * @param oneword
 * @param p
 */
function showARIDialog(why, json, word, oneword, p) {
	console.log("Can't recover, not in /v or not loaded yet.");
	// has not been loaded yet.
}

/**
 * @method showARIDialog
 * @param why
 * @param json
 * @param word
 * @param oneword
 * @param p
 */
function ariRetry() {
	window.location.reload(true);
}

/**
 * Handle that ST has disconnected
 * @method handleDisconnect
 * @param why
 * @param json
 * @param word
 * @param what - what we were doing
 */
function handleDisconnect(why, json, word, what) {
	if(json && (json.err_code === 'E_NOT_LOGGED_IN')) {
		window.location = 'login.jsp?operationFailed'+window.location.hash;
		return;
	}
	if(!what) {
		what = "unknown";
	}
	if(!word) {
		word = "error"; // assume it's an error except for a couple of cases.
	}
	updateProgressWord(word);
	if(!saidDisconnect) {
		saidDisconnect=true;
		if(json&&json.err) {
			why = why + "\n The error message was: \n"+json.err;
		}
		console.log("Disconnect: " + why);
		var oneword = dojo.byId("progress_oneword");
		if(oneword) {
			oneword.title = "Disconnected: " + why;
			oneword.onclick = function() {
				var p = dojo.byId("progress");
				var subDiv = document.createElement('div');
				var chunk0 = document.createElement("i");
				chunk0.appendChild(document.createTextNode(stui_str("error_restart")));
				var chunk = document.createElement("textarea");
				chunk.className = "errorMessage";
				chunk.appendChild(document.createTextNode(why));
				chunk.rows="10";
				chunk.cols="40";
				subDiv.appendChild(chunk0);
				subDiv.appendChild(chunk);
				p.appendChild(subDiv);
				if(oneword.details) {
					setDisplayed(oneword.details, false);
				}
				oneword.onclick=null;
				return false;
			};
			var p = dojo.byId("progress");
			var subDiv = document.createElement('div');
			var detailsButton = document.createElement("button");
			detailsButton.type = "button";
			detailsButton.id = "progress-details";
			detailsButton.appendChild(document.createTextNode(stui_str("details")));
			detailsButton.onclick = oneword.onclick;
			subDiv.appendChild(detailsButton);
			oneword.details = detailsButton;
			p.appendChild(subDiv);
			showARIDialog(why, json, word, oneword, subDiv, what);
		}
		if(json) {
			stdebug("JSON: " + json.toString());
		}
	}
}

var updateParts = null;

var cacheKillStamp = surveyRunningStamp;

/**
 * Return a string to be used with a URL to avoid caching. Ignored by the server.
 * @method cacheKill
 * @returns {String} the URL fragment, append to the query
 */
function cacheKill() {
	if(!cacheKillStamp || cacheKillStamp<surveyRunningStamp) {
		cacheKillStamp=surveyRunningStamp;
	}
	cacheKillStamp++;

	return "&cacheKill="+cacheKillStamp;
}

/**
 * Note that there is special site news.
 * @param {String} newSpecialHeader site news
 */
function updateSpecialHeader(newSpecialHeader) {
	if(newSpecialHeader && newSpecialHeader.length>0) {
		specialHeader = newSpecialHeader;
	} else {
		specialHeader=null;
	}
	showWord();
}

function trySurveyLoad() {
	try {
		var url = contextPath + "/survey?"+cacheKill();
		console.log("Attempting to restart ST at " + url);
	    dojo.xhrGet({
	        url: url,
	        timeout: ajaxTimeout
	    });
	} catch(e){}
}

var lastJsonStatus = null;

function formatErrMsg(json, subkey) {
	if(!subkey) {
		subkey = "unknown";
	}
	var theCode = "E_UNKNOWN";
	if(json && json.session_err) {
		theCode = "E_SESSION_DISCONNECTED";
	}
	var msg_str = theCode;
	if(json && json.err_code) {
		msg_str = theCode = json.err_code;
		if(stui.str(json.err_code) == json.err_code) {
			console.log("** Unknown error code: " + json.err_code);
			msg_str = "E_UNKNOWN";
		}
	}
	if(json === null) {
		json = {}; // handle cases with no input data
	}
	return stui.sub(msg_str,
			{
				/* Possibilities include: err_what_section, err_what_locmap, err_what_menus,
					err_what_status, err_what_unknown, err_what_oldvotes, err_what_vote */
				json: json, what: stui.str('err_what_'+subkey), code: theCode, err_data: json.err_data,
				surveyCurrentLocale: surveyCurrentLocale,
				surveyCurrentId: surveyCurrentId,
				surveyCurrentSection: surveyCurrentSection,
				surveyCurrentPage: surveyCurrentPage
			} );
}

/**
 * Based on the last received packet of JSON, update our status
 * @method updateStatusBox
 * @param {Object} json received
 */
function updateStatusBox(json) {
	if(json.disconnected) {
		json.err_code = 'E_DISCONNECTED';
		handleDisconnect("Misc Disconnect", json,"disconnected"); // unknown
	} else if(json.err_code) {
		console.log('json.err_code == ' + json.err_code);
		if(json.err_code == "E_NOT_STARTED") {
			trySurveyLoad();
		}
		handleDisconnect(json.err_code, json, "disconnected", "status");
	} else if (json.SurveyOK==0) {
		console.log('json.surveyOK==0');
		trySurveyLoad();
		handleDisconnect("The SurveyTool server is not ready to accept connections, please retry. ", json,"disconnected"); // ST has restarted
	} else if (json.status && json.status.isBusted) {
		handleDisconnect("The SurveyTool server has halted due to an error: " + json.status.isBusted, json,"disconnected"); // Server down- not our fault. Hopefully.
	} else if(!json.status) {
		handleDisconnect("The SurveyTool erver returned a bad status",json);
	} else if(json.status.surveyRunningStamp!=surveyRunningStamp) {
		handleDisconnect("The SurveyTool server restarted since this page was loaded. Please retry.",json,"disconnected"); // desync
	} else if(json.status && json.status.isSetup==false && json.SurveyOK==1) {
		updateProgressWord("startup");
	} else {
		updateProgressWord("ok");
	}

	if(json.status) {
		lastJsonStatus = json.status;
		if(!updateParts) {
			var visitors = dojo.byId("visitors");
			updateParts = {
					visitors: visitors,
					ug: document.createElement("span"),
					load: document.createElement("span"),
					db: document.createElement("span")
			};
		}
		//"~1 users, 8pg/uptime: 38:44/load:28% db:0/1"

		var ugtext = "~";
		ugtext = ugtext + (json.status.users) + " users, ";
		if(json.status.guests > 0) {
			ugtext= ugtext + (json.status.guests) + " guests, ";
		}
		ugtext=ugtext+(json.status.pages)+"pg/"+json.status.uptime;
		removeAllChildNodes(updateParts.ug);
		updateParts.ug.appendChild(document.createTextNode(ugtext));

		removeAllChildNodes(updateParts.load);
		updateParts.load.appendChild(document.createTextNode("Load:"+json.status.sysload));

		removeAllChildNodes(updateParts.db);
		updateParts.db.appendChild(document.createTextNode("db:"+json.status.dbopen+"/"+json.status.dbused));

		var fragment = document.createDocumentFragment();
		fragment.appendChild(updateParts.ug);
		fragment.appendChild(document.createTextNode(" "));
		fragment.appendChild(updateParts.load);
		fragment.appendChild(document.createTextNode(" "));
		fragment.appendChild(updateParts.db);

		if(updateParts.visitors) {
			removeAllChildNodes(updateParts.visitors);
			updateParts.visitors.appendChild(fragment);
		}

		function standOutMessage(txt) {
			return "<b style='font-size: x-large; color: red;'>" + txt + "</b>";
		}

		if(window.kickMe) {
			json.timeTillKick = 0;
		} else if(window.kickMeSoon) {
			json.timeTillKick = 5000;
		}

		// really don't care if guest user gets 'kicked'. Doesn't matter.
		if( (surveyUser!==null) && json.timeTillKick && (json.timeTillKick>=0) && (json.timeTillKick < (60*1*1000) )) { // show countdown when 1 minute to go
			var kmsg = "Your session will end if not active in about "+ (parseInt(json.timeTillKick)/1000).toFixed(0) + " seconds.";
			console.log(kmsg);
			updateSpecialHeader(standOutMessage(kmsg));
		} else if((surveyUser!==null) && (( json.timeTillKick === 0) || (json.session_err))) {
			var kmsg  = stui_str("ari_sessiondisconnect_message");
			console.log(kmsg);
			updateSpecialHeader(standOutMessage(kmsg));
			disconnected=true;
  		    addClass(document.getElementsByTagName("body")[0], "disconnected");
  		    if(!json.session_err) json.session_err = "disconnected";
  		    handleDisconnect(kmsg,json,"Your session has been disconnected.");
		} else if(json.status.specialHeader && json.status.specialHeader.length>0) {
			updateSpecialHeader(json.status.specialHeader);
		} else {
			updateSpecialHeader(null);
		}
	}
}

/**
 * How often to fetch updates. Default 15s
 * @property timerSpeed
 */
var timerSpeed = 15000;

/**
 * How long to wait for AJAX updates.
 * @property ajaxTimeout
 */
var ajaxTimeout = 120000; // 2 minutes

var surveyVersion = 'Current';

/**
 * This is called periodically to fetch latest ST status
 * @method updateStatus
 */
function updateStatus() {
	if(disconnected) {
		stdebug("Not updating status - disconnected.");
		return;
	}

	doDeferredUpdates(); // do this periodically
	var surveyLocaleUrl = '';
	var surveySessionUrl = '';
	if(surveyCurrentLocale!==null && surveyCurrentLocale!= '') {
		surveyLocaleUrl = '&_='+surveyCurrentLocale;
	}
	if(surveySessionId && surveySessionId !==null) {
		surveySessionUrl = '&s='+surveySessionId;
	}
    dojo.xhrGet({
        url: contextPath + "/SurveyAjax?what=status"+surveyLocaleUrl+surveySessionUrl+cacheKill(),
        handleAs:"json",
        timeout: ajaxTimeout,
        load: function(json){
            if((json==null) || (json.status&&json.status.isBusted)) {
                wasBusted = true;
                busted();
                return; // don't thrash
            }
            var st_err =  document.getElementById('st_err');
            if(json.err != null && json.err.length > 0) {
               st_err.innerHTML=json.err;
               if(json.status&&json.status.surveyRunningStamp!=surveyRunningStamp) {
            	   st_err.innerHTML = st_err.innerHTML + " <b>Note: Lost connection with Survey Tool or it restarted.</b>";
                   updateStatusBox({disconnected: true});
               }
               st_err.className = "ferrbox";
               wasBusted = true;
               busted();
            } else {
            	if(json.status.newVersion) {
            		surveyVersion = json.status.newVersion;
            	}
            	if(json.status.surveyRunningStamp!=surveyRunningStamp) {
                    st_err.className = "ferrbox";
                    st_err.innerHTML="The SurveyTool has been restarted. Please reload this page to continue.";
                    wasBusted=true;
                    busted();
                    // TODO: show ARI for reconnecting
            	} else if(wasBusted == true &&
            			(!json.status.isBusted)
                      || (json.status.surveyRunningStamp!=surveyRunningStamp)) {
                    st_err.innerHTML="Note: Lost connection with Survey Tool or it restarted.";
                    if(clickContinue != null) {
                        st_err.innerHTML = st_err.innerHTML + " Please <a href='"+clickContinue+"'>click here</a> to continue.";
                    } else {
                    	st_err.innerHTML = st_err.innerHTML + " Please reload this page to continue.";
                    }
                    st_err.className = "ferrbox";
                    busted();
                } else {
                   st_err.className = "";
                   removeAllChildNodes(st_err);
                }
            }
            updateStatusBox(json);

            if(json.localeStamp) {
                if(surveyNextLocaleStamp==0) {
                	surveyNextLocaleStamp = json.localeStamp;
                    stdebug("STATUS0: " + json.localeStampName + "="+json.localeStamp);
                } else {
                	if(json.localeStamp > surveyNextLocaleStamp) {
                        stdebug("STATUS=: " + json.localeStampName + "="+json.localeStamp + " > " + surveyNextLocaleStamp);
                		handleChangedLocaleStamp(json.localeStamp, json.localeStampName);
                	} else {
                        stdebug("STATUS=: " + json.localeStampName + "="+json.localeStamp + " <= " + surveyNextLocaleStamp);
                	}
                }
            }

            if((wasBusted == false) && (json.status.isSetup) && (loadOnOk != null)) {
                window.location.replace(loadOnOk);
            } else {
            	setTimeout(updateStatus, timerSpeed);
            }
        },
        error: function(err, ioArgs){
            wasBusted = true;
            updateStatusBox({err: err.message, err_name: err.name, disconnected: true});
        }
    });
}

/**
 * Fire up the main timer loop to update status
 * @method setTimerOn
 */
function setTimerOn() {
    updateStatus();
    // an interval is not used - each status update does its own timeout.
}

/**
 * Change the update timer's speed
 * @method resetTimerSpeed
 * @param {Int} speed
 */
function resetTimerSpeed(speed) {
	timerSpeed = speed;
}

// set up window. Let Dojo call us, otherwise dojo won't load.
dojo.ready(function(){
	setTimerOn();
});

/**
 * Table mapping CheckCLDR.StatusAction into capabilites
 * @property statusActionTable
 */
var statusActionTable = {
    ALLOW:                    { vote: true, ticket: false, change: true  },
    ALLOW_VOTING_AND_TICKET:  { vote: true, ticket: true, change: false },
    ALLOW_VOTING_BUT_NO_ADD:  { vote: true, ticket: false, change: false },
    ALLOW_TICKET_ONLY:        { vote: false, ticket: true, change: true },
    DEFAULT:                  { vote: false, ticket: false, change: false}
};

/**
 * Parse a CheckCLDR.StatusAction and return the capabilities table
 * @method parseStatusAction
 * @param action
 * @returns {Object} capabilities
 */
function parseStatusAction(action) {
	if(!action) return statusActionTable.DEFAULT;
	var result = statusActionTable[action];
	if(!result) result = statusActionTable.DEFAULT;
	return result;
}

/**
 * Determine whether a JSONified array of CheckCLDR.CheckStatus is overall a warning or an error.
 * @param {Object} testResults - array of CheckCLDR.CheckStatus
 * @returns {String} 'Warning' or 'Error' or null
 */
function getTestKind(testResults) {
	if(!testResults) {
		return null;
	}
	var theKind =  null;
    for(var i=0;i<testResults.length;i++) {
        var tr = testResults[i];
        if(tr.type == 'Warning') {
        	theKind = tr.type;
        } else if(tr.type == 'Error') {
        	return tr.type;
        }
    }
    return theKind;
}

/**
 * Clone the node, removing the id
 * @param {Node} i
 * @returns {Node} new return, deep clone but with no ids
 */
function cloneAnon(i) {
	if(i==null) return null;
	var o = i.cloneNode(true);
	if(o.id) {
		o.id = null;
	}
	return o;
}

/**
 * like cloneAnon, but localizes by fetching stui-html string substitution.
 * @method localizeAnon
 * @param o
 */
function localizeAnon(o) {
	loadStui(null, function(stui) {
		if(o&&o.childNodes) for(var i=0;i<o.childNodes.length;i++) {
			var k = o.childNodes[i];
			if(k.id && k.id.indexOf("stui-html")==0) {
				var key = k.id.slice(5);
				if(stui.str(key)) {
					k.innerHTML=stui.str(key);
				}
				k.id=null;
			} else {
				localizeAnon(k);
			}
		}
	});
}

/**
 * Localize the flyover text by replacing $X with stui[Z]
 * @method localizeFlyover
 * @param {Node} o
 */
function localizeFlyover(o) {
	if(o&&o.childNodes) for(var i=0;i<o.childNodes.length;i++) {
		var k = o.childNodes[i];
		if(k.title && k.title.indexOf("$")==0) {
			var key = k.title.slice(1);
			if(stui.str(key)) {
				k.title=stui.str(key);
			} else {
				k.title=null;
			}
		} else {
			localizeFlyover(k);
		}
	}
}

/**
 * cloneAnon, then call localizeAnon
 * @method cloneLocalizeAnon
 * @param {Node} i
 * @returns {Node}
 */
function cloneLocalizeAnon(i) {
	var o = cloneAnon(i);
	if(o) localizeAnon(o);
	return o;
}

/**
 * Return an array of all children of the item which are tags
 * @method getTagChildren
 * @param {Node} tr
 * @returns {Array}
 */
function getTagChildren(tr) {
	var rowChildren = [];

	for(k in tr.childNodes) {
		var t = tr.childNodes[k];
		if(t.tagName) {
			rowChildren.push(t);
		}
	}
	return rowChildren;
}

/**
 * show the 'loading' sign
 * @method showLoader
 * @param loaderDiv ignored
 * @param {String} text text to use
 */
function showLoader(loaderDiv, text) {
	updateAjaxWord(text);
}

/**
 * Hide the 'loading' sign
 * @method hideLoader
 * @param loaderDiv ignored
 */
function hideLoader(loaderDiv) {
	updateAjaxWord(null);
}

/**
 * wire up the button to perform a submit
 * @method wireUpButton
 * @param button
 * @param tr
 * @param theRow
 * @param vHash
 * @param box
 */
function wireUpButton(button, tr, theRow, vHash,box) {
	if(box) {
		button.id="CHANGE_" + tr.rowHash;
		vHash="";
		box.onchange=function(){
			handleWiredClick(tr,theRow,vHash,box,button,'submit');
			return false;
		};
		box.onkeypress=function(e){
			if(!e || !e.keyCode)  {
				return true; // not getting the point here.
			} else if(e.keyCode == 13) {
				handleWiredClick(tr,theRow,vHash,box,button);
				return false;
			} else {
				return true;
			}
		};
	} else if(vHash==null) {
		button.id="NO_" + tr.rowHash;
		vHash="";
	} else {
		button.id = "v"+vHash+"_"+tr.rowHash;
	}
	listenFor(button,"click",
			function(e){ handleWiredClick(tr,theRow,vHash,box,button); stStopPropagation(e); return false; });

	// proposal issues
	if(tr.myProposal) {
		if(button == tr.myProposal.button) {
			button.className = "ichoice-x";
			button.checked = true;
			tr.lastOn = button;
		} else {
			button.className = "ichoice-o";
			button.checked = false;
		}
	} else if((theRow.voteVhash==vHash) && !box) {
		button.className = "ichoice-x";
		button.checked = true;
		tr.lastOn = button;
	} else {
		button.className = "ichoice-o";
		button.checked = false;
	}
}

/**
 * Append an icon to the div
 * @method addIcon
 * @param {Node} td
 * @Param {String} className name of icon's CSS class
 */
function addIcon(td, className) {
	var star = document.createElement("span");
	star.className=className;
	star.innerHTML="&nbsp; &nbsp;";
	td.appendChild(star);
	return star;
}

var gPopStatus = {
	unShow: null,
	lastShown: null,
	lastTr: null,
	popToken: 0
};

/**
 * @method showInPop2
 * This is the actual function is called to display the right-hand "info" panel.
 * It is defined dynamically because it depends on variables that aren't available at startup time.
 * @param {String} str the string to show at the top
 * @param {Node} tr the <TR> of the row
 * @param {Boolean} hideIfLast
 * @param {Function} fn
 * @param {Boolean} immediate
 */
function showInPop(str,tr, theObj, fn, immediate) {
}

/**
 * @method listenToPop
 * Make the object "theObj" cause the infowindow to show when clicked.
 * @param {String} str
 * @param {Node} tr the TR element that is clicked
 * @param {Node} theObj to listen to
 * @param {Function} fn the draw function
 * @returns {Function}
 */
function listenToPop(str, tr, theObj, fn) {
	var theFn;
	listenFor(theObj, "click",
			theFn = function(e) {
				showInPop(str, tr, theObj, fn, true);
				stStopPropagation(e);
				return false;
			});
	return theFn;
}


function getPopToken() {
	return gPopStatus.popToken;
}

function incrPopToken(x) {
	++gPopStatus.popToken;
	return gPopStatus.popToken;
}

// timeout for showing sideways view
var sidewaysShowTimeout = -1;

// array storing all only-1 sublocale
var oneLocales = [];

/**
 * @method showForumStuff
 * called when showing the popup each time
 * @param {Node} frag
 * @param {Node} forumDiv
 * @param {Node} tr
 *
 * This function is about 300 lines long!
 */
function showForumStuff(frag, forumDiv, tr) {
	var isOneLocale = false;
	if(oneLocales[surveyCurrentLocale]) {
		isOneLocale = true;
	}
	if(!isOneLocale) {
		var sidewaysControl = createChunk(stui.str("sideways_loading0"), "div", "sidewaysArea");
		frag.appendChild(sidewaysControl);

		function clearMyTimeout() {
			if(sidewaysShowTimeout != -1) {
				window.clearInterval(sidewaysShowTimeout);
				sidewaysShowTimeout = -1;
			}
		}
		clearMyTimeout();
		sidewaysShowTimeout = window.setTimeout(function() {
			clearMyTimeout();
			updateIf(sidewaysControl, stui.str("sideways_loading1"));

			var url = contextPath + "/SurveyAjax?what=getsideways&_="+surveyCurrentLocale+"&s="+surveySessionId+"&xpath="+tr.theRow.xpstrid +  cacheKill();
			myLoad(url, "sidewaysView", function(json) {
				// if there is 1 sublocale(+ 1 default), we do nothing
				var locale_count = Object.keys(json.others).length + json.novalue.length;
				if(locale_count <= 2){
					oneLocales[surveyCurrentLocale] = true;
					updateIf(sidewaysControl, "");
				}else{
					if(!json.others) {
						updateIf(sidewaysControl, ""); // no sibling locales (or all null?)
					} else {
						updateIf(sidewaysControl, ""); // remove string

						var topLocale = json.topLocale;
						var curLocale = locmap.getRegionAndOrVariantName(topLocale);
						var readLocale = null;

						// merge the read-only sublocale to base locale
						var mergeReadBase = function mergeReadBase(list){
							var baseValue = null;
							// find the base locale, remove it and store its value
							for(var l=0; l<list.length; l++){
								var loc = list[l][0];
								if(loc === topLocale) {
									baseValue = list[l][1];
									list.splice(l,1);
									break;
								}
							}

							// replace the default locale(read-only) with base locale, store its name for label
							for(var l=0; l<list.length; l++){
								var loc = list[l][0];
								var bund = locmap.getLocaleInfo(loc);
				        		if(bund && bund.readonly) {
				        			readLocale = locmap.getRegionAndOrVariantName(loc);
				        			list[l][0] = topLocale;
				        			list[l][1] = baseValue;
				        			break;
								}
							}
						}
						// compare all sublocale values
						var appendLocaleList = function appendLocaleList(list) {
							var group = document.createElement("optGroup");
							var br = document.createElement("optGroup");
							group.appendChild(br);

							group.setAttribute("label", "Regional Variants for " + curLocale);
							group.setAttribute("title", "Regional Variants for " + curLocale);

							var curValue = null;
							var escape = "\u00A0\u00A0\u00A0";
							var unequalSign = "\u2260\u00A0";

							// find the currenct locale name
							for(var l=0;l<list.length;l++){
								var loc = list[l][0];
								if(loc === surveyCurrentLocale) {
									curValue = list[l][1];
									break;
								}
							};

							for(var l=0;l<list.length;l++) {
								var loc = list[l][0];
								var title = list[l][1];
								var item = document.createElement("option");
								item.setAttribute("value", loc);
								if(title == null){
									item.setAttribute("title", "undefined");
								}else{
									item.setAttribute("title", title);
								}

								var str = locmap.getRegionAndOrVariantName(loc);
								if(loc === topLocale){
									str = str + " (= " + readLocale + ")";
								}

								if(loc === surveyCurrentLocale) {
									str = escape + str;
									item.setAttribute("selected", "selected");
				        			item.setAttribute("disabled","disabled");
								}else if(title != curValue){
									str = unequalSign + str;
								}else{
									str = escape + str;
								}
								item.appendChild(document.createTextNode(str));
								group.appendChild(item);
							}
							popupSelect.appendChild(group);
						};

						var dataList = [];

						var popupSelect = document.createElement("select");
						var inheritValue = null; // inherit value for no-value object
						for(var s in json.others) {
							for(var t in json.others[s]){
								if(json.others[s][t] === topLocale){
									inheritValue = s;
								}
								dataList.push([json.others[s][t], s]);
							}
						}
						if(json.novalue) {
							for(s in json.novalue){
								dataList.push([json.novalue[s], inheritValue]);
							}
						}
						mergeReadBase(dataList);

						// then sort by sublocale name
						dataList = dataList.sort(function(a,b) {
							return locmap.getRegionAndOrVariantName(a[0]) > locmap.getRegionAndOrVariantName(b[0]);
					    });
						appendLocaleList(dataList);

						var group = document.createElement("optGroup");
						popupSelect.appendChild(group);

						listenFor(popupSelect, "change", function(e) {
							var newLoc = popupSelect.value;
							if(newLoc !== surveyCurrentLocale) {
								surveyCurrentLocale = newLoc;
								reloadV();
							}
							return stStopPropagation(e);
						});

						sidewaysControl.appendChild(popupSelect);
					}
				}
			});
		}, 2000); // wait 2 seconds before loading this.
	}

	// prepend something
	var buttonTitle = "forumNewPostButton";
	var buttonClass = "forumNewButton btn btn-default btn-sm";
	var couldFlag = false;
	if(tr.theRow) {
		if(tr.theRow.voteVhash !== tr.theRow.winningVhash
				&& tr.theRow.voteVhash !== ''
				&& tr.theRow.canFlagOnLosing &&
				!tr.theRow.rowFlagged) {
			buttonTitle = "forumNewPostFlagButton";
			buttonClass = "forumNewPostFlagButton btn btn-default btn-sm";
			couldFlag=true;
		}
	}
	var newButton = createChunk(stui.str(buttonTitle), "button", buttonClass);
	if(!isDashboard()) {
		frag.appendChild(newButton);

		(function(theRow,couldFlag){listenFor(newButton, "click", function(e) {
				xpathMap.get({hex: theRow.xpstrid},
						function(o) {
							var subj = theRow.code + ' ' + theRow.xpstrid;
							if(o.result && o.result.ph) {
								subj = xpathMap.formatPathHeader(o.result.ph);
							}
							if(couldFlag) {
								subj = subj + " (Flag for review)";
							}
							openReply({
								locale: surveyCurrentLocale,
								xpath: theRow.xpstrid,
								subject: subj,
							});
				});
				stStopPropagation(e);
				return false;
		});})(tr.theRow, couldFlag);
	}
	var loader2 = createChunk(stui.str("loading"),"i");
	frag.appendChild(loader2);

	/**
	 * @method havePosts
	 * @param {Integer} nrPosts
	 */
	function havePosts(nrPosts) {
		setDisplayed(loader2,false); // not needed
		tr.forumDiv.forumPosts = nrPosts;

		if(nrPosts == 0) return; // nothing to do,

		var showButton = createChunk("Show " + tr.forumDiv.forumPosts  + " posts", "button", "forumShow");

		forumDiv.appendChild(showButton);

		var theListen = function(e) {
			setDisplayed(showButton, false);

			// callback.
			var ourUrl = tr.forumDiv.url + "&what=forum_fetch";
			var errorHandler = function(err, ioArgs) {
				console.log('Error in showForumStuff: ' + err + ' response '
						+ ioArgs.xhr.responseText);
				showInPop(
						stopIcon
								+ " Couldn't load forum post for this row- please refresh the page. <br>Error: "
								+ err + "</td>", tr, null);
				handleDisconnect("Could not showForumStuff:"+err, null);
				return true;
			};
			var loadHandler = function(json) {
				try {
					if (json) {
						if(json.ret) {
							forumDiv.appendChild(parseForumContent({ret: json.ret,
											replyButton: true,
											noItemLink: true}));
						}
					}
				} catch (e) {
					console.log("Error in ajax forum read ", e.message);
					console.log(" response: " + json);
					showInPop(stopIcon + " exception in ajax forum read: " + e.message, tr, null, true);
				}
			};
			var xhrArgs = {
				url : ourUrl,
				handleAs : "json",
				load : loadHandler,
				error : errorHandler
			};
			queueXhr(xhrArgs);
			stStopPropagation(e);
			return false;
		};
		listenFor(showButton, "click", theListen);
		listenFor(showButton, "mouseover", theListen);
	}

	// lazy load post count!
	// load async
	var ourUrl = tr.forumDiv.url + "&what=forum_count" + cacheKill() ;
	window.setTimeout(function() {
		var xhrArgs = {
				url: ourUrl,
				handleAs:"json",
				load: function(json) {
					if(json && json.forum_count !== undefined) {
						havePosts(parseInt(json.forum_count));
					} else {
						console.log("Some error loading post count??");
					}
				},
		};
		queueXhr(xhrArgs);
	}, 1900);
}

/**
 * @method appendForumStuff
 * called when initially setting up the section.
 * @param {Node} tr
 * @param {Node} theRow
 * @param {Node} forumDiv
 */
function appendForumStuff(tr, theRow, forumDiv) {
	removeAllChildNodes(forumDiv); // we may be updating.
	var theForum = 	locmap.getLanguage(surveyCurrentLocale);
	forumDiv.replyStub = contextPath + "/survey?forum=" + theForum + "&_=" + surveyCurrentLocale + "&replyto=";
	forumDiv.postUrl = forumDiv.replyStub + "x"+theRow;
	forumDiv.url = contextPath + "/SurveyAjax?xpath=" + theRow.xpid + "&_=" + surveyCurrentLocale + "&fhash="
		+ theRow.rowHash + "&vhash=" + "&s=" + tr.theTable.session
		+ "&voteinfo=t";
}

/**
 * change the current id.
 * @method updateCurrentId
 * @param id the id to set
 */
window.updateCurrentId = function updateCurrentId(id) {
	if(id==null) id = '';
    if(surveyCurrentId != id) { // don't set if already set.
	    surveyCurrentId = id;
    }
};

// window loader stuff
dojo.ready(function() {
	var unShow = null;
	var pucontent = dojo.byId("itemInfo");
	if(!pucontent) return;

	//pucontent.className = "oldFloater";
	var hideInterval=null;

	function parentOfType(tag, obj) {
		if(!obj) return null;
		if(obj.nodeName===tag) return obj;
		return parentOfType(tag, obj.parentElement);
	}

	function setLastShown(obj) {
		if(gPopStatus.lastShown && obj!=gPopStatus.lastShown) {
			removeClass(gPopStatus.lastShown,"pu-select");
			var partr = parentOfType('TR',gPopStatus.lastShown);
			if(partr) {
				removeClass(partr, 'selectShow');
			}
		}
		if(obj) {
			addClass(obj,"pu-select");
			var partr = parentOfType('TR',obj);
			if(partr) {
				addClass(partr, 'selectShow');
			}
		}
		gPopStatus.lastShown = obj;
	}

	function clearLastShown() {
		setLastShown(null);
	}

	var deferHelp = {};

	/**
	 * @method showInPop2
	 * This is the actual function called to display the right-hand "info" panel.
	 * It is defined dynamically because it depends on variables that aren't available at startup time.
	 * @param {String} str the string to show at the top
	 * @param {Node} tr the <TR> of the row
	 * @param {Boolean} hideIfLast
	 * @param {Function} fn
	 * @param {Boolean} immediate
	 */
	window.showInPop2 = function(str, tr, hideIfLast, fn, immediate, hide) {
		if(unShow) {
			unShow();
			unShow=null;
		}
		incrPopToken('newShow' + str);
		if(hideInterval) {
			clearTimeout(hideInterval);
			hideInterval=null;
		}

		if(tr && tr.sethash) {
			window.updateCurrentId(tr.sethash);
		}
		setLastShown(hideIfLast);

		var td = document.createDocumentFragment();

		// Always have help (if available).
		var theHelp = null;
		if(tr) {
			var theRow = tr.theRow;
			// this also marks this row as a 'help parent'
			theHelp = createChunk("","div","alert alert-info fix-popover-help vote-help");

			if(theRow.xpstrid) {
				var deferHelpSpan = document.createElement('span');
				theHelp.appendChild(deferHelpSpan);

				if(deferHelp[theRow.xpstrid]) {
					deferHelpSpan.innerHTML = deferHelp[theRow.xpstrid];
				} else {
					deferHelpSpan.innerHTML = "<i>"+stui.str("loading")+"</i>";

					// load async
					var url = contextPath + "/help?xpstrid="+theRow.xpstrid+"&_instance="+surveyRunningStamp;
					var xhrArgs = {
							url: url,
							handleAs:"text",
							load: function(html) {
								deferHelp[theRow.xpstrid] = html;
								deferHelpSpan.innerHTML = html;
								if(isDashboard()) {
									fixPopoverVotePos();
								}
							},
					};
					queueXhr(xhrArgs);
					// loader.
				}

				// extra attributes
				if(theRow.extraAttributes && Object.keys(theRow.extraAttributes).length>0) {
					var extraHeading = createChunk( stui.str("extraAttribute_heading"), "h3", "extraAttribute_heading");
					var extraContainer = createChunk("","div","extraAttributes");
					appendExtraAttributes(extraContainer, theRow);
					theHelp.appendChild(extraHeading);
					theHelp.appendChild(extraContainer);
				}
			}
		}
		if(theHelp) {
			td.appendChild(theHelp);
		}

		if(str) { // If a simple string, clone the string
			var div2 = document.createElement("div");
			div2.innerHTML=str;
			td.appendChild(div2);
		}
		// If a generator fn (common case), call it.
		if(fn!=null) {
			unShow=fn(td);

		}

		var theVoteinfo = null;
		if(tr&& tr.voteDiv) {
			theVoteinfo =  tr.voteDiv;
		}
		if(theVoteinfo) {
			td.appendChild(theVoteinfo.cloneNode(true));
		}
		if(tr&&tr.ticketLink) {
			td.appendChild(tr.ticketLink.cloneNode(true));
		}

		// forum stuff
		if(tr && tr.forumDiv) {
			var forumDiv = tr.forumDiv.cloneNode(true);
			showForumStuff(td, forumDiv, tr); // give a chance to update anything else
			td.appendChild(forumDiv);
		}

		if(tr && tr.theRow && tr.theRow.xpath) {
			td.appendChild(clickToSelect(createChunk(tr.theRow.xpath,"div","xpath")));
		}

		// SRL suspicious
		if(tr) {
			if(isDashboard()) {
				showHelpFixPanel(td);
			}
			else {
				removeAllChildNodes(pucontent);
				pucontent.appendChild(td);
			}
		}
		else {
			var clone = td.cloneNode(true);
			setHelpContent(td);
			if(!isDashboard()) {
				removeAllChildNodes(pucontent);
				pucontent.appendChild(clone);
			}

		}
		td=null;

		//for the voter
		 $('.voteInfo_voterInfo').hover(function() {
			 	var email = $(this).data('email').replace(' (at) ', '@');
			 	if(email !== '') {
			    	$(this).html('<a href="mailto:'+email+'" title="'+email+'" style="color:black"><span class="glyphicon glyphicon-envelope"></span></a>');
			    	$(this).closest('td').css('text-align','center');
			    	$(this).children('a').tooltip().tooltip('show');
			 	} else {
			    	$(this).html($(this).data('name'));
			    	$(this).closest('td').css('text-align','left');
			 	}
		    }, function() {
		    	$(this).html($(this).data('name'));
		    	$(this).closest('td').css('text-align','left');
		 });

	};
	// delay before show
	window.showInPop = function(str,tr,hideIfLast,fn,immediate) {
		if(hideInterval) {
			clearTimeout(hideInterval);
			hideInterval=null;
		}
		if(immediate) {
			return window.showInPop2(str,tr,hideIfLast,fn);
		}
	};
	window.resetPop = function() {
		lastShown = null;
	};
});

/**
 * Check if we need LRM/RLM marker to display
 * @param field choice field to append if needed
 * @param dir direction of current locale (control float direction0
 * @param value the value of votes (check &lrm; &rlm)
 */
function checkLRmarker(field, dir, value){
	if (value) {
		if ( value.indexOf("\u200E") > -1 ||  value.indexOf("\u200F") > -1 ) {
			value = value.replace(/\u200E/g, "<span class=\"visible-mark\">&lt;LRM&gt;</span>")
			 			 .replace(/\u200F/g, "<span class=\"visible-mark\">&lt;RLM&gt;</span>");
			var lrm = document.createElement("div");
			lrm.className = "lrmarker-container";
			lrm.innerHTML = value;
			field.appendChild(lrm);
		}
	}
}

/**
 * Append just an editable span representing a candidate voting item
 * @method appendItem
 * @param div {DOM} div to append to
 * @param value {String} string value
 * @param pClass {String} html class for the voting item
 * @param tr {DOM} ignored, but the tr the span belongs to
 * @return {DOM} the new span
 */
function appendItem(div,value, pClass, tr) {
	var text = document.createTextNode(value?value:stui.str("no value"));
	var span = document.createElement("span");
	span.appendChild(text);
	if(!value) {
		span.className = "selected";
	} else if(pClass) {
		span.className = pClass;
	} else {
		span.className = "value";
	}
	div.appendChild(span);

	return span;
}

function testsToHtml(tests) {
	var newHtml = "";
	if(!tests) return newHtml;
	for ( var i = 0; i < tests.length; i++) {
		var testItem = tests[i];
		newHtml += "<p class='trInfo tr_" + testItem.type;
		if(testItem.type == 'Warning') {
			newHtml += ' alert alert-warning fix-popover-help';
		}
		else if (testItem.type == 'Error') {
			newHtml += ' alert alert-danger fix-popover-help';
		}
		newHtml += "' title='" + testItem.type+"'>";
		if (testItem.type == 'Warning') {
			newHtml += warnIcon;
		} else if (testItem.type == 'Error') {
			newHtml += stopIcon;
		}
		newHtml += tests[i].message;
		newHtml += "</p>";
	}
	return newHtml;
}

function setDivClass(div,testKind) {
	if(!testKind) {
		div.className = "d-item";
	} else if(testKind=="Warning") {
		div.className = "d-item-warn";
	} else if(testKind=="Error") {
		div.className = "d-item-err";
	} else {
		div.className = "d-item";
	}
}
function findItemByValue(items, value) {
	if(!items) return null;
	for(var i in items) {
		if(value==items[i].value) {
			return items[i];
		}
	}
	return null;
}

/**
 * TODO remove
 * global config of table rows
 * @property surveyConfig
 */
var surveyConfig = null;

/**
 * Show an item that's not in the saved data, but has been proposed newly by the user.
 * @method showProposedItem
 */
function showProposedItem(inTd,tr,theRow,value,tests, json) {
	var children = getTagChildren(tr);
	var config = surveyConfig;

	// Find where our value went.
	var ourItem = findItemByValue(theRow.items,value);
	var testKind = getTestKind(tests);
	var ourDiv = null;
	var wrap;
	if(!ourItem) {
		ourDiv = document.createElement("div");
		var newButton = cloneAnon(dojo.byId('proto-button'));
		if(tr.myProposal) {
			children[config.othercell].removeChild(tr.myProposal);
		}
		tr.myProposal = ourDiv;
		tr.myProposal.value = value;
		tr.myProposal.button = newButton;
		if(newButton) {
			newButton.value=value;
			if(tr.lastOn) {
				tr.lastOn.checked = false;
				tr.lastOn.className = "ichoice-o";
			}
			wireUpButton(newButton,tr,theRow,"[retry]", {"value":value});
			wrap = wrapRadio(newButton);
			ourDiv.appendChild(wrap);
		}
		var h3 = document.createElement("span");
		var span=appendItem(h3, value, "value",tr);
		setLang(span);
		ourDiv.appendChild(h3);
		children[config.othercell].appendChild(tr.myProposal);

	} else {
		ourDiv = ourItem.div;
	}
	if(json&&!parseStatusAction(json.statusAction).vote) {
		ourDiv.className = "d-item-err";
		var input = $(inTd).closest('tr').find('.input-add');
		if(input) {
			input.closest('.form-group').addClass('has-error');
			input.popover('destroy').popover({placement:'bottom',html:true, content:testsToHtml(tests),trigger:'hover'}).popover('show');
			if(tr.myProposal)
				tr.myProposal.style.display = "none";
		}
		if(ourItem) {
			str = stui.sub("StatusAction_msg",
					[ stui_str("StatusAction_"+json.statusAction) ],"p", "");
			var str2 = stui.sub("StatusAction_popupmsg",
					[ stui_str("StatusAction_"+json.statusAction), theRow.code  ],"p", "");
			// show in modal popup (ouch!)
			alert(str2);

			// show this message in a sidebar also
			showInPop(stopIcon + str, tr, null, null, true);
		}
		return;
	} else if(json&&json.didNotSubmit) {
		ourDiv.className = "d-item-err";
		showInPop("(ERROR: Unknown error - did not submit this value.)", tr, null, null, true);
		return;
	} else {
		setDivClass(ourDiv,testKind);
	}

	if(testKind || !ourItem) {
		var div3 = document.createElement("div");
		var newHtml = "";
		newHtml += testsToHtml(tests);

		if(!ourItem) {
			var h3 = document.createElement("h3");
			var span=appendItem(h3, value, "value",tr);
			setLang(span);
			h3.className="span";
			div3.appendChild(h3);
		}
		var newDiv = document.createElement("div");
		div3.appendChild(newDiv);
		newDiv.innerHTML = newHtml;
		if(json&&(!parseStatusAction(json.statusAction).vote)) {
			div3.appendChild(createChunk(
					stui.sub("StatusAction_msg",
						[ stui_str("StatusAction_"+json.statusAction) ],"p", "")));
		}

		div3.popParent = tr;

		// will replace any existing function
		var ourShowFn = function(showDiv) {
			var retFn;
			if(ourItem && ourItem.showFn) {
				retFn =  ourItem.showFn(showDiv);
			} else {
				retFn = null;
			}
			if(tr.myProposal && (value == tr.myProposal.value)) { // make sure it wasn't submitted twice
				showDiv.appendChild(div3);
			}
			return retFn;
		};
		listenToPop(null, tr, ourDiv, ourShowFn);
		showInPop(null, tr, ourDiv, ourShowFn, true);
	}

	return false;
}

// returns a popinto function
function showItemInfoFn(theRow, item, vHash, newButton, div) {
	return function(td) {
		//div.className = 'd-item-selected';
		var isInherited = false;
		var h3 = document.createElement("div");
		var displayValue = item.value;
		if (item.value == INHERITANCE_MARKER) {
			displayValue = theRow.inheritedValue;
			isInherited = true;
		}

		var span = appendItem(h3, displayValue, item.pClass); /* no need to pass in 'tr' - clicking this span would have no effect. */
		setLang(span);
		h3.className="span";
		if(false) { // click to copy
			h3.onclick = function() {
				if(tr.inputBox) {
					tr.inputBox.value  = item.value;
				}
				return false;
			};
			h3.title = stui.clickToCopy;
		}
		td.appendChild(h3);

		if ( item.value) {
               h3.appendChild(createChunk(stui.sub("pClass_"+item.pClass, item ),"p","pClassExplain"));
		}
		if (   item.pClass === 'alias'
			|| item.pClass === 'fallback'
		    || item.pClass === 'fallback_root' ) {
			isInherited = true;
		}

		if ( isInherited && (theRow.inheritedLocale || theRow.inheritedXpid )) {
			var clickyLink = createChunk(stui.str('followAlias'), "a", 'followAlias');
			clickyLink.href = '#/'+ ( theRow.inheritedLocale || surveyCurrentLocale )+
				'//'+ ( theRow.inheritedXpid || theRow.xpstrid ); //linkToLocale(subLoc);
			h3.appendChild(clickyLink);
		}

		var newDiv = document.createElement("div");
		td.appendChild(newDiv);

		var newHtml = "";

		if (item.tests) {
			newHtml += testsToHtml(item.tests);
		} else {
			newHtml = "<i>no tests</i>";
		}

		newDiv.innerHTML = newHtml;

		if(item.example) {
			appendExample(td, item.example);
		}
	}; // end fn
}

function appendExample(parent, text, loc) {
	var div = document.createElement("div");
	div.className="d-example well well-sm";
	div.innerHTML=text;
	setLang(div, loc);
	parent.appendChild(div);
	return div;
}

/**
 * Append a Vetting item ( vote button, etc ) to the row.
 * @method addVitem
 * @param {DOM} td cell to append into
 * @param {DOM} tr which row owns the items
 * @param {JSON} theRow JSON content of this row's data
 * @param {JSON} item JSON of the specific item we are adding
 * @param {DOM} newButton     button prototype object
 */
function addVitem(td, tr, theRow, item, newButton) {
	var div = document.createElement("div");
	var isWinner = (td==tr.proposedcell);
	var testKind = getTestKind(item.tests);
	setDivClass(div,testKind);
	item.div = div; // back link
	if(item==null)  {
		return;
	}
	var displayValue = item.value;
	if (item.value == INHERITANCE_MARKER) {
		item.pClass = theRow.inheritedPClass == "winner" ? "fallback" : theRow.inheritedPClass;
		displayValue = theRow.inheritedValue;
	}
	var choiceField = document.createElement("div");
	var wrap;
	choiceField.className = "choice-field";
	if(newButton) {
		newButton.value=item.value;
		wireUpButton(newButton,tr,theRow,item.valueHash);
		wrap = wrapRadio(newButton);
		choiceField.appendChild(wrap);
	}
    var subSpan = document.createElement("span");
    subSpan.className = "subSpan";
	var span = appendItem(subSpan,displayValue,item.pClass,tr);
	choiceField.appendChild(subSpan);

	setLang(span);
	checkLRmarker(choiceField, span.dir, item.value);

	if(item.isOldValue==true && !isWinner) {
		addIcon(choiceField,"i-star");
	}
	if(item.votes && !isWinner) {
		/* Disable all usage of the "i-vote" (vote.png, check-mark in a square) icon pending
		 * clarification/documentation of its meaning/purpose.
		 * Comment out in two places: here in addVitem, and in updateRow.
		 * See https://unicode.org/cldr/trac/ticket/10521#comment:29
		 */
		// addIcon(choiceField,"i-vote");

		if(item.valueHash == theRow.voteVhash && theRow.canFlagOnLosing && !theRow.rowFlagged){
			var newIcon = addIcon(choiceField,"i-stop"); // DEBUG
		}
	}
	if(newButton &&
			theRow.voteVhash == item.valueHash &&
			theRow.items[theRow.voteVhash].votes &&
			theRow.items[theRow.voteVhash].votes[surveyUser.id] &&
			theRow.items[theRow.voteVhash].votes[surveyUser.id].overridedVotes) {
		var overrideTag = createChunk(theRow.items[theRow.voteVhash].votes[surveyUser.id].overridedVotes,"span","i-override");
		choiceField.appendChild(overrideTag);
	}

	div.appendChild(choiceField);

	var inheritedClassName = "fallback";
	var defaultClassName = "fallback_code";

    // wire up the onclick
	td.showFn = item.showFn = showItemInfoFn(theRow,item,item.valueHash,newButton,div);
	div.popParent = tr;
	listenToPop(null, tr, div, td.showFn);
	td.appendChild(div);

    if(item.example && item.value != item.examples ) {
		appendExample(div,item.example);
	}
}

function calcPClass(value, winner) {
	if(value==winner) {
		return "winner";
	} else {
		return "value";
	}
}

function appendExtraAttributes(container, theRow) {
	for(var attr in theRow.extraAttributes) {
		var attrval = theRow.extraAttributes[attr];
		var extraChunk = createChunk( attr+"="+attrval , "span", "extraAttribute");
		container.appendChild(extraChunk);
	}
}

/**
 * Update one row using data received from server.
 * 
 * @param tr the table row
 * @param theRow the data for the row
 * 
 * Cells (columns) in each row (basic viewing):
 * Code    English    Abstain    A    Winning    Add    Others
 * 
 * Or, in Dashboard:
 * Code    English    CLDR 33    Winning 34    Action
 */
function updateRow(tr, theRow) {
	tr.theRow = theRow;

	/*
	 * For convenience, set up two hashes, for reverse mapping from value or rawValue to item.
	 * 
	 * At the same time, find the item with rawValue === INHERITANCE_MARKER and if found mark it with
	 * "item.isVoteForBailey = true" and point to it with "tr.voteForBaileyItem = item".
	 */	
	tr.valueToItem = {}; // hash:  string value to item (which has a div)
	tr.rawValueToItem = {}; // hash:  string value to item (which has a div)
	for(var k in theRow.items) {
		var item = theRow.items[k];
		if(item.value) {
			tr.valueToItem[item.value] = item; // back link by value
			tr.rawValueToItem[item.rawValue] = item; // back link by value
			if(item.rawValue === INHERITANCE_MARKER) { // This is a vote for Bailey.
				item.isVoteForBailey = true;
				tr.voteForBaileyItem = item;
			}
		}
		if(item.isBailey) {
			tr.baileyItem = item; // This is the actual Bailey item (target of the votes)
		}
	}

	/*
	 * If there's a "soft" item, move/copy votes from "soft" to "hard" item.
	 * Report error "there is no Bailey Target item" if there's a "soft" item without a corresponding "hard" item.
	 * TODO: don't move votes here! See https://unicode.org/cldr/trac/ticket/11299
	 * The server should set up the data we need. Here on the client isn't the place for it.
	 * Also, it should be normal to have "soft" item but no "hard" item. There should only be a "hard" item
	 * if a user voted for it by entering the value manually.
	 * Currently updateInheritedValue adds isBailey item that looks "hard" but contains essential inheritance info.
	 * That inheritance info belongs in the "soft" item or, better, in the DataRow (theRow) instead of any CandidateItem (theRow.items).
	 * A hard item for Bailey should be like any other non-inheritance item.
	 */
	if(tr.voteForBaileyItem) {
		// Some people voted for Bailey. Move those votes over to the actual target.
		if(!tr.baileyItem) {
			console.error('For ' + theRow.xpstrid + ' - there is no Bailey Target item!');
			// don't delete the item
		} else {
			tr.baileyItem.votes = tr.baileyItem.votes || {};
			for(var k in tr.voteForBaileyItem.votes) {
				tr.baileyItem.votes[k] = tr.voteForBaileyItem.votes[k]; //  move vote from ↑↑↑ INHERITANCE_MARKER to explicit item
				tr.baileyItem.votes[k].isVoteForBailey = true;
				// no need to remove - will be handled specially below.
			}
		}
	}

	/*
	 * Update the vote info.
	 */
	if(theRow.voteResolver) {
		updateRowVoteInfo(tr, theRow);
	} else {
		tr.voteDiv = null;
	}

	tr.statusAction = parseStatusAction(theRow.statusAction);
	tr.canModify = (tr.theTable.json.canModify && tr.statusAction.vote);
	tr.ticketOnly = (tr.theTable.json.canModify && tr.statusAction.ticket);
	tr.canChange = (tr.canModify && tr.statusAction.change);

    /*
     * TODO: if theRow could be null or undefined here, shouldn't that have been checked far earlier??
     */
    if(!theRow || !theRow.xpid) {
		tr.innerHTML="<td><i>ERROR: missing row</i></td>";
		return;
	}
	if(!tr.xpstrid) {
		tr.xpid = theRow.xpid;
		tr.xpstrid = theRow.xpstrid;
		if(tr.xpstrid) {
			tr.id = "r@"+tr.xpstrid;
			tr.sethash = tr.xpstrid;
		}
	}

	var children = getTagChildren(tr);

	/*
	 * config = surveyConfig has fields indicating which cells (columns) to display...  
	 */
	var config = surveyConfig;
	var protoButton = dojo.byId('proto-button');
	if(!tr.canModify) {
		protoButton = null; // no voting at all.
	}

	children[config.statuscell].className = "d-dr-"+theRow.confirmStatus + " d-dr-status";

	if(!children[config.statuscell].isSetup) {
		listenToPop("", tr, children[config.statuscell]);

		children[config.statuscell].isSetup=true;
	}

	children[config.statuscell].title = stui.sub('draftStatus',[stui.str(theRow.confirmStatus)]);

	if(theRow.hasVoted) {
		children[config.nocell].title=stui.voTrue;
		children[config.nocell].className= "d-no-vo-true";
	} else {
		children[config.nocell].title=stui.voFalse;
		children[config.nocell].className= "d-no-vo-false";
	}

	/*
	 * Assemble the "code cell", a.k.a. the "Code" column.
	 */
	if(config.codecell) {
		updateRowCodeCell(tr, theRow, config, children);
	}

	/*
	 * Set up the "comparison cell", a.k.a. the "English" column.
	 */
	if(!children[config.comparisoncell].isSetup) {
		updateRowEnglishComparisonCell(tr, theRow, config, children);
	}
	
	/*
	 * Set up the "proposed cell", a.k.a. the "Winning" column.
	 *
	 * TODO: subroutine.
	 */
	removeAllChildNodes(children[config.proposedcell]); // win
	if(theRow.rowFlagged) {
		var flagIcon = addIcon(children[config.proposedcell], "s-flag");
		flagIcon.title = stui.str("flag_desc");
	} else if(theRow.canFlagOnLosing) {
		var flagIcon = addIcon(children[config.proposedcell], "s-flag-d");
		flagIcon.title = stui.str("flag_d_desc");
	}
	setLang(children[config.proposedcell]);
	tr.proposedcell = children[config.proposedcell];

	/*
	 * Still setting up the "proposed cell"...
	 * 
	 * TODO: fix bug in the following block.
	 * See https://unicode.org/cldr/trac/ticket/11299#comment:10
	 *
	 * When do we have theRow.winningVhash == "" and/or theRow.winningValue == ""?
	 * When both those conditions are true (and they often are), then regardless of whether isFallback is true or false,
	 * theFallbackValue gets set to the LAST element of theRow.items ... and which element is the LAST
	 * element is probably browser-dependent, see
	 * https://stackoverflow.com/questions/280713/elements-order-in-a-for-in-loop
	 * 
	 * Typically we'd expect winningVhash and winningValue both to be empty, or both to be non-empty, but
	 * there are strange exceptions, see example C in ticket 11299.
	 *
	 * There seems to be no reason for theFallbackValue; might as well directly
	 * assign theRow.winningVhash = k -- but that's a relatively minor issue.
	 * 
	 * Aside from having this bug, this block shouldn't exist since the server should
	 * be responsible for making sure winningVhash and winningValue are never empty.
	 */
	if(theRow.items && theRow.winningVhash == "") {
		// find the fallback value
		var theFallbackValue = null;
		for(var k in theRow.items) {
			// console.log("DEBUG: k = [" + k + "], value = [" + value + "], isFallback = " + theRow.items[k].isFallback);			
			if(theRow.items[k].isFallback || theRow.winningValue == "") {
				theFallbackValue = k;
			}
		}
		if(theFallbackValue !== null) {
			// console.log("DEBUG: changing winningVhash to theFallbackValue = [" + theFallbackValue + "]");
			theRow.winningVhash = theFallbackValue;
		}
	}
	/*
	 * Still setting up the "proposed cell"...
	 */
	if(theRow.items&&theRow.winningVhash) {
		addVitem(children[config.proposedcell],tr,theRow,theRow.items[theRow.winningVhash],cloneAnon(protoButton));
	} else {
		children[config.proposedcell].showFn = function(){};  // nothing else to show
	}

	listenToPop(null,tr,children[config.proposedcell], children[config.proposedcell].showFn);

	/*
	 * Set up the "err cell"
	 * 
	 * TODO: is this something that's normally hidden? Clarify.
	 * 
	 * "config.errcell" doesn't occur anywhere else so this may be dead code.
	 */
	if(config.errcell) {
		listenToPop(null,tr,children[config.errcell], children[config.proposedcell].showFn);
	}

	/*
	 * Set up the "other cell", a.k.a. the "Others" column.
	 * 
	 * TODO: subroutine
	 */
	var hadOtherItems  = false;
	removeAllChildNodes(children[config.othercell]); // other
	setLang(children[config.othercell]);

	//add button
	var formAdd = document.createElement("form");
	if(tr.canModify) {
		formAdd.role = "form";
		formAdd.className = "form-inline";
		var buttonAdd = document.createElement("div");
		var btn = document.createElement("button");
		buttonAdd.className = "button-add form-group";

		toAddVoteButton(btn);

		buttonAdd.appendChild(btn);
		formAdd.appendChild(buttonAdd);

		var input = document.createElement("input");
		var popup;
		input.className = "form-control input-add";
		input.placeholder = 'Add a translation';
		var copyWinning = document.createElement("button");
		copyWinning.className = "copyWinning btn btn-info btn-xs";
		copyWinning.title = "Copy Winning";
		copyWinning.type = "button";
		copyWinning.innerHTML = '<span class="glyphicon glyphicon-arrow-right"></span> Winning';
		copyWinning.onclick = function(e) {
			var theValue = null;
			if (theRow.items[theRow.winningVhash]) {
				theValue = theRow.items[theRow.winningVhash].value;
			}
			if (theValue === INHERITANCE_MARKER || theValue === null) {
				theValue = theRow.inheritedValue;
			}
			input.value = theValue || null;
			input.focus();
		}
		var copyEnglish = document.createElement("button");
		copyEnglish.className = "copyEnglish btn btn-info btn-xs";
		copyEnglish.title = "Copy English";
		copyEnglish.type = "button";
		copyEnglish.innerHTML = '<span class="glyphicon glyphicon-arrow-right"></span> English';
		copyEnglish.onclick = function(e) {
		    input.value = theRow.displayName || null;
		    input.focus();
		}
		btn.onclick = function(e) {
			//if no input, add one
			if($(buttonAdd).parent().find('input').length == 0) {

				//hide other
				$.each($('button.vote-submit'), function() {
					toAddVoteButton(this);
				});

				//transform the button
				toSubmitVoteButton(btn);
				$(buttonAdd).popover({content:' '}).popover('show');
				popup = $(buttonAdd).parent().find('.popover-content');
				popup.append(input);
				if (theRow.displayName) {
					popup.append(copyEnglish);
				}
				if ((theRow.items[theRow.winningVhash] && theRow.items[theRow.winningVhash].value) ||
						theRow.inheritedValue) {
					popup.append(copyWinning);
				}
				popup.closest('.popover').css('top', popup.closest('.popover').position().top - 19);
				input.focus();

				//enter pressed
				$(input).keydown(function (e) {
					var newValue = $(this).val();
					if(e.keyCode == 13) { //enter pressed
						if(newValue) {
							addValueVote(children[config.othercell], tr, theRow, newValue, cloneAnon(protoButton));
						}
						else {
							toAddVoteButton(btn);
						}
					} else if (e.keyCode === 27) {
						toAddVoteButton(btn);
					}
				});

			}
			else {
				var newValue = input.value;

				if(newValue) {
					addValueVote(children[config.othercell], tr, theRow, newValue, cloneAnon(protoButton));
				}
				else {
					toAddVoteButton(btn);
				}
				stStopPropagation(e);
				return false;
			}
			stStopPropagation(e);
			return false;
		};
	}

	/*
	/* Add the other vote info -- that is, vote info for the "Others" column.
	 */
	for(k in theRow.items) {
		if((k === theRow.winningVhash) // skip vote for winner
			/*
			 * TODO: the following "skip vote for ↑↑↑" (INHERITANCE_MARKER) is dubious,
			 *  see https://unicode.org/cldr/trac/ticket/11299
			 */
		   || (theRow.items[k].isVoteForBailey)) { // skip vote for ↑↑↑
					continue;
				}
		hadOtherItems=true;
		addVitem(children[config.othercell],tr,theRow,theRow.items[k],cloneAnon(protoButton));
		children[config.othercell].appendChild(document.createElement("hr"));
	}

	if(!hadOtherItems /*!onIE*/) {
		listenToPop(null, tr, children[config.othercell]);
	}
	if(tr.myProposal && tr.myProposal.value && !findItemByValue(theRow.items, tr.myProposal.value)) {
		// add back my proposal
		children[config.othercell].appendChild(tr.myProposal);
	} else {
		tr.myProposal=null; // not needed
	}

	/*
	 * If the user can make changes, add "+" button for adding new candidate item.
	 * 
	 * Note the call to isDashboard(): this code is for Dashboard as well as the basic navigation interface.
	 */
	if(tr.canChange) {
		if(isDashboard()) {
			children[config.othercell].appendChild(document.createElement('hr'));
			children[config.othercell].appendChild(formAdd);//add button
		}
		else {
			removeAllChildNodes(children[config.addcell]);
			children[config.addcell].appendChild(formAdd);//add button
		}
	}

	/*
	 * Set up the "no cell", a.k.a. the "A" column.
	 * If the user can make changes, add an "abstain" button;
	 * else, possibly add a ticket link, or else hide the column.
	 * 
	 * TODO: subroutine
	 */
	if(tr.canModify) {
		removeAllChildNodes(children[config.nocell]); // no opinion
		var noOpinion = cloneAnon(protoButton);
		var wrap;
		wireUpButton(noOpinion,tr, theRow, null);
		noOpinion.value=null;
		wrap = wrapRadio(noOpinion);
		children[config.nocell].appendChild(wrap);
		listenToPop(null, tr, children[config.nocell]);
	}  else if (tr.ticketOnly) { // ticket link
    	if(!tr.theTable.json.canModify) { // only if hidden in the header
    		setDisplayed(children[config.nocell], false);
    	}
		children[config.proposedcell].className="d-change-confirmonly";

		var surlink = document.createElement("div");
		surlink.innerHTML = '<span class="glyphicon glyphicon-list-alt"></span>&nbsp;&nbsp;';
		surlink.className = 'alert alert-info fix-popover-help';

		var link = createChunk(stui.str("file_a_ticket"),"a");
		var newUrl = "http://unicode.org/cldr/trac"+"/newticket?component=data&summary="+surveyCurrentLocale+":"+theRow.xpath+"&locale="+surveyCurrentLocale+"&xpath="+theRow.xpstrid+"&version="+surveyVersion;
		link.href = newUrl;
		link.target = "cldr-target-trac";
		theRow.proposedResults = createChunk(stui.str("file_ticket_must"), "a","fnotebox");
		theRow.proposedResults.href = newUrl;
		if(!window.surveyOfficial) {
			link.appendChild(createChunk(" (Note: this is not the production SurveyTool! Do not submit a ticket!) ","p"));
			link.href = link.href + "&description=NOT+PRODUCTION+SURVEYTOOL!";
		}
		children[config.proposedcell].appendChild(createChunk(stui.str("file_ticket_notice"), "i", "fnotebox"));
		surlink.appendChild(link);
		tr.ticketLink = surlink;
	} else  { // no change possible
    	if(!tr.theTable.json.canModify) { // only if hidden in the header
    		setDisplayed(children[config.nocell], false);
    	}
	}
	
	/*
	 * Do something related to coverage.
	 * TODO: explain.
	 */
	tr.className='vother cov'+theRow.coverageValue;
	
	/*
	 * Show the current ID.
	 * TODO: explain.
	 */
	if(surveyCurrentId!== '' && surveyCurrentId === tr.id) {
		window.showCurrentId(); // refresh again - to get the updated voting status.
	}
}

/**
 * Update the vote info for this row.
 *
 * Set up the "vote div".
 *
 * @param tr the table row
 * @param theRow the data from the server for this row
 * 
 * Called by updateRow.
 */
function updateRowVoteInfo(tr, theRow) {
	'use strict';
	var vr = theRow.voteResolver;
	var div = tr.voteDiv = document.createElement("div");
	tr.voteDiv.className = "voteDiv";

	if(theRow.voteVhash &&
			theRow.voteVhash!=='' && surveyUser) {
		var voteForItem = theRow.items[theRow.voteVhash];
		if(voteForItem && voteForItem.votes && voteForItem.votes[surveyUser.id] &&
				voteForItem.votes[surveyUser.id].overridedVotes) {
			tr.voteDiv.appendChild(createChunk(stui.sub("override_explain_msg",
					{overrideVotes:voteForItem.votes[surveyUser.id].overridedVotes, votes: surveyUser.votecount}
				),"p","helpContent"));
		}
		if(theRow.voteVhash !== theRow.winningVhash
			&& theRow.canFlagOnLosing) {
				if(!theRow.rowFlagged) {
					var newIcon = addIcon(tr.voteDiv,"i-stop");
					tr.voteDiv.appendChild(createChunk(stui.sub("mustflag_explain_msg", { }), "p", "helpContent"));
				} else {
					var newIcon = addIcon(tr.voteDiv,"i-flag");
					tr.voteDiv.appendChild(createChunk(stui.str("flag_desc", "p", "helpContent")));
				}
		}
	}
	if(!theRow.rowFlagged && theRow.canFlagOnLosing) {
		var newIcon = addIcon(tr.voteDiv,"i-flag-d");
		tr.voteDiv.appendChild(createChunk(stui.str("flag_d_desc", "p", "helpContent")));
	}
	var haveWinner = false;
	var haveLast = false;

	// next, the org votes
	var perValueContainer = div; // IF NEEDED: >>  = document.createElement("div");  perValueContainer.className = "perValueContainer";
	var n = 0;
	while(n < vr.value_vote.length) {
		var value = vr.value_vote[n++];
		if(value==null) continue;
		var vote = vr.value_vote[n++];
		var item = tr.rawValueToItem[value]; // backlink to specific item in hash
		if(item==null) continue;
		var vdiv = createChunk(null, "table", "voteInfo_perValue table table-vote");
		if(n > 2) {
			var valdiv = createChunk(null, "div", "value-div");
		}
		else {
			var valdiv = createChunk(null, "div", "value-div first")
		}
		// heading row
		var vrow = createChunk(null, "tr", "voteInfo_tr voteInfo_tr_heading");
		if(!item.isVoteForBailey && (!item.votes || Object.keys(item.votes).length==0)) {

		} else {
			vrow.appendChild(createChunk(stui.str("voteInfo_orgColumn"),"td","voteInfo_orgColumn voteInfo_td"));
		}
		var isection = createChunk(null, "div", "voteInfo_iconBar");
		var vvalue = createChunk("User", "td", "voteInfo_valueTitle voteInfo_td");
		var vbadge = createChunk(vote, "span", "badge");
		if(value==vr.winningValue) {
			appendIcon(isection,"voteInfo_winningItem d-dr-"+theRow.voteResolver.winningStatus);
		}

		if(value==vr.lastReleaseValue) {
			appendIcon(isection,"voteInfo_lastRelease i-star");
		}

		/* Disable all usage of the "i-vote" (vote.png, check-mark in a square) icon pending
		 * clarification/documentation of its meaning/purpose.
		 * Comment out in two places: here in updateRow, and in addVitem.
		 * See https://unicode.org/cldr/trac/ticket/10521#comment:29
		 */
		/***
		if(value != vr.winningValue) {
				appendIcon(isection,"i-vote");
		}
		***/

		setLang(valdiv);
		if (value == INHERITANCE_MARKER) {
			appendItem(valdiv, stui.str("voteInfo_acceptInherited"), "fallback", tr);
			valdiv.appendChild(createChunk(stui.str('voteInfo_baileyVoteList'), 'p'));
		} else {
		    appendItem(valdiv, value, calcPClass(value, vr.winningValue), tr);
		}
		valdiv.appendChild(isection);
		vrow.appendChild(vvalue);

		var cell = createChunk(null,"td","voteInfo_voteTitle voteInfo_voteCount voteInfo_td"+"");
		cell.appendChild(vbadge);
		vrow.appendChild(cell);
		vdiv.appendChild(vrow);

		var createVoter = function(v) {
			if(v==null) {
				return createChunk("(missing information)!","i","stopText");
			}
			var div = createChunk(v.name || stui.str('emailHidden'),"td","voteInfo_voterInfo voteInfo_td");
			div.setAttribute('data-name', v.name || stui.str('emailHidden'));
			div.setAttribute('data-email', v.email || '');
			return div;
		};

		if(!item.votes || Object.keys(item.votes).length==0) {
			var vrow = createChunk(null, "tr", "voteInfo_tr voteInfo_orgHeading");
			vrow.appendChild(createChunk(stui.str("voteInfo_noVotes"),"td","voteInfo_noVotes voteInfo_td"));
			vrow.appendChild(createChunk(null, "td","voteInfo_noVotes voteInfo_td"));
			vdiv.appendChild(vrow);

		} else if(item.isVoteForBailey) {
			// show the raw votes.
			for(var kk in item.votes) {
				var thevote = item.votes[kk];
				var vrow = createChunk(null, "tr", "voteInfo_tr voteInfo_orgHeading fallback");
				vrow.appendChild(createChunk(thevote.org,"td","voteInfo_orgColumn voteInfo_td"));
				vrow.appendChild(createVoter(thevote)); // voteInfo_td
				vdiv.appendChild(vrow);
			}
		} else {
			for(org in theRow.voteResolver.orgs) {
				var theOrg = vr.orgs[org];
				var vrRaw = {};
				var orgVoteValue = theOrg.votes[value];
				if(orgVoteValue !== undefined && orgVoteValue > 0) { // someone in the org actually voted for it
					var topVoter = null; // top voter for this item
					var orgsVote = (theOrg.orgVote == value);
					var topVoterTime = 0; // Calculating the latest time for a user from same org

					if(orgsVote) {
						// find a top-ranking voter to use for the top line
						for(var voter in item.votes) {
							if(item.votes[voter].org==org && item.votes[voter].votes==theOrg.votes[value]) {
								if(topVoterTime != 0){
									// Get the latest time vote only
									if(vr.nameTime[item.votes[topVoter].name] < vr.nameTime[item.votes[voter].name]){
										topVoter = voter;
										console.log(item);
										console.log(vr.nameTime[item.votes[topVoter].name]);
										topVoterTime = vr.nameTime[item.votes[topVoter].name];
									}
								}
								else{
									topVoter = voter;
									console.log(item);
									console.log(vr.nameTime[item.votes[topVoter].name]);
									topVoterTime = vr.nameTime[item.votes[topVoter].name];
								}
							}
						}
					} else {
						// just find someone in the right org..
						for(var voter in item.votes) {
							if(item.votes[voter].org==org) {
								topVoter = voter;
								break;
							}
						}
					}

					// ORG SUBHEADING row
					var baileyClass = (item.votes[topVoter] && item.votes[topVoter].isVoteForBailey)?" fallback":"";
					var vrow = createChunk(null, "tr", "voteInfo_tr voteInfo_orgHeading");
					vrow.appendChild(createChunk(org,"td","voteInfo_orgColumn voteInfo_td"));
					if (item.votes[topVoter]) {
					     vrow.appendChild(createVoter(item.votes[topVoter])); // voteInfo_td
					} else {
						vrow.appendChild(createVoter(null));
					}
					if(orgsVote) {
						var cell = createChunk(null,"td","voteInfo_orgsVote voteInfo_voteCount voteInfo_td"+baileyClass);
						cell.appendChild(createChunk(orgVoteValue, "span", "badge"));
						vrow.appendChild(cell);
					}else
						vrow.appendChild(createChunk(orgVoteValue,"td","voteInfo_orgsNonVote voteInfo_voteCount voteInfo_td"+baileyClass));
					vdiv.appendChild(vrow);

					//now, other rows:
					for(var voter in item.votes) {
						if(item.votes[voter].org!=org ||  // wrong org or
								voter==topVoter) { // already done
							continue; // skip
						}
						// OTHER VOTER row
						var baileyClass = (item.votes[voter].isVoteForBailey)?" fallback":"";
						var vrow = createChunk(null, "tr", "voteInfo_tr");
						vrow.appendChild(createChunk("","td","voteInfo_orgColumn voteInfo_td")); // spacer
						vrow.appendChild(createVoter(item.votes[voter])); // voteInfo_td
						vrow.appendChild(createChunk(item.votes[voter].votes,"td","voteInfo_orgsNonVote voteInfo_voteCount voteInfo_td"+baileyClass));
						vdiv.appendChild(vrow);
					}
				}
			}
		}
		perValueContainer.appendChild(valdiv);
		perValueContainer.appendChild(vdiv);
	}

	if(vr.requiredVotes) {
		var msg = stui.sub("explainRequiredVotes", {requiredVotes: vr.requiredVotes  /* , votecount: surveyUser.votecount */ });
		perValueContainer.appendChild(createChunk(msg,"p", "alert alert-warning fix-popover-help"));
	}

	// done with voteresolver table

	if(stdebug_enabled) {
		tr.voteDiv.appendChild(createChunk(vr.raw,"p","debugStuff"));
	}	
}

/*
 * Update the "Code" cell (column) of this row
 * 
 * @param tr the table row
 * @param theRow the data from the server for this row
 * @param config
 * @param children
 */
function updateRowCodeCell(tr, theRow, config, children) {
	'use strict';
	children[config.codecell].appendChild(createChunk('|>'));
	removeAllChildNodes(children[config.codecell]);
	children[config.codecell].appendChild(createChunk('<|'));
	removeAllChildNodes(children[config.codecell]);
	var codeStr = theRow.code;
	if (theRow.coverageValue == 101 && !stdebug_enabled) {
		codeStr = codeStr + " (optional)";
	}
	children[config.codecell].appendChild(createChunk(codeStr));
	if (tr.theTable.json.canModify) { // pointless if can't modify.
		children[config.codecell].className = "d-code";
		if (!tr.forumDiv) {
			tr.forumDiv = document.createElement("div");
			tr.forumDiv.className = "forumDiv";
		}
		appendForumStuff(tr, theRow, tr.forumDiv);
	}
	// extra attributes
	if (theRow.extraAttributes && Object.keys(theRow.extraAttributes).length > 0) {
		appendExtraAttributes(children[config.codecell], theRow);
	}
	if (stdebug_enabled) {
		var anch = document.createElement("i");
		anch.className = "anch";
		anch.id = theRow.xpid;
		children[config.codecell].appendChild(anch);
		anch.appendChild(document.createTextNode("#"));
		var go = document.createElement("a");
		go.className = "anch-go";
		go.appendChild(document.createTextNode("zoom"));
		go.href = window.location.pathname + "?_=" + surveyCurrentLocale + "&x=r_rxt&xp=" + theRow.xpid;
		children[config.codecell].appendChild(go);
		var js = document.createElement("a");
		js.className = "anch-go";
		js.appendChild(document.createTextNode("{JSON}"));
		js.popParent = tr;
		listenToPop(JSON.stringify(theRow), tr, js);
		children[config.codecell].appendChild(js);
		children[config.codecell].appendChild(createChunk(" c=" + theRow.coverageValue));
	}
	if (!children[config.codecell].isSetup) {
		var xpathStr = "";
		if (stdebug_enabled) {
			xpathStr = "XPath: " + theRow.xpath;
		}
		listenToPop(xpathStr, tr, children[config.codecell]);
		children[config.codecell].isSetup = true;
	}
}

/**
 * Update the "comparison cell", a.k.a. the "English" column, of this row
 * 
 * @param tr the table row
 * @param theRow the data from the server for this row
 * @param config
 * @param children
 */
function updateRowEnglishComparisonCell(tr, theRow, config, children) {
	'use strict';
	if(theRow.displayName) {
		var hintPos = theRow.displayName.indexOf('[translation hint');
		var hasExample = false;

		if(theRow.displayExample) {
			hasExample = true;
		}

		if(hintPos != -1) {
			theRow.displayExample = theRow.displayName.substr(hintPos, theRow.displayName.length) + (theRow.displayExample ? theRow.displayExample.replace(/\[translation hint.*?\]/g,"") : '');
			theRow.displayName = theRow.displayName.substr(0, hintPos);
		}

		children[config.comparisoncell].appendChild(createChunk(theRow.displayName, 'span', 'subSpan'));
		setLang(children[config.comparisoncell], surveyBaselineLocale);
		if(theRow.displayExample) {
			appendExample(children[config.comparisoncell], theRow.displayExample, surveyBaselineLocale);
		}

		if(hintPos != -1 || hasExample) {
			var infos = document.createElement("div");
			infos.className = 'infos-code';

			if(hintPos != -1) {
				var img = document.createElement("img");
				img.src = 'hint.png';
				img.alt = 'Translation hint';
				infos.appendChild(img);
			}

			if(hasExample) {
				var img = document.createElement("img");
				img.src = 'example.png';
				img.alt = 'Example';
				infos.appendChild(img);
			}

			children[config.comparisoncell].appendChild(infos);
		}
	} else {
		children[config.comparisoncell].appendChild(document.createTextNode(""));
	}
	/* The next line (listenToPop...) had been commented out, for unknown reasons.
	 * Restored (uncommented) for http://unicode.org/cldr/trac/ticket/10573 so that
	 * the right-side panel info changes when you click on the English column.
	 */
	listenToPop(null, tr, children[config.comparisoncell]);
	children[config.comparisoncell].isSetup=true;
}

function findPartition(partitions,partitionList,curPartition,i) {
	if(curPartition &&
			i>=curPartition.start &&
			i<curPartition.limit) {
		return curPartition;
	}
	for(var j in partitionList) {
		var p = partitions[j];
		if(i>=p.start &&
			i<p.limit) {
				return p;
			}
	}
	return null;
}

function insertRowsIntoTbody(theTable,tbody) {
	theTable.hitCount++;
	var theRows = theTable.json.section.rows;
	var toAdd = theTable.toAdd;
	var parRow = dojo.byId('proto-parrow');
	removeAllChildNodes(tbody);

	var theSort = theTable.json.displaySets[theTable.curSortMode];
	var partitions = theSort.partitions;
	var rowList = theSort.rows;
	var partitionList = Object.keys(partitions);
	var curPartition = null;
	for(i in rowList ) {
		var k = rowList[i];
		var theRow = theRows[k];
		var dir = theRow.dir;
		overridedir = (dir != null ? dir : null);
		//no partition in the dashboard
		if(!isDashboard()) {
			var newPartition = findPartition(partitions,partitionList,curPartition,i);

			if(newPartition != curPartition) {
				if(newPartition.name != "") {
					var newPar = cloneAnon(parRow);
					var newTd = getTagChildren(newPar);
					var newHeading = getTagChildren(newTd[0]);
					newHeading[0].innerHTML = newPartition.name;
					newHeading[0].id = newPartition.name;
					tbody.appendChild(newPar);
					newPar.origClass = newPar.className;
					newPartition.tr = newPar; // heading
				}
				curPartition = newPartition;
			}

			var theRowCov = parseInt(theRow.coverageValue);
			if(!newPartition.minCoverage || newPartition.minCoverage > theRowCov) {
				newPartition.minCoverage = theRowCov;
                if(newPartition.tr) {
                    // only set coverage of the header if there's a header
            	    newPartition.tr.className = newPartition.origClass+" cov"+newPartition.minCoverage;
                }
			}
		}

		var tr = theTable.myTRs[k];
		if(!tr) {
			tr = cloneAnon(toAdd);
			theTable.myTRs[k]=tr; // save for later use
		}

		tr.rowHash = k;
		tr.theTable = theTable;
		if(!theRow) {
			console.log("Missing row " + k);
		}
		// update the xpath map
		xpathMap.put({id: theRow.xpid,
					  hex: theRow.xpstrid,
					  path: theRow.xpath,
					  ph: {
					       section: surveyCurrentSection, // Section: Timezones
					       page: surveyCurrentPage,    // Page: SEAsia ( id, not name )
					       header: curPartition.name,    // Header: Borneo
					       code: theRow.code           // Code: standard-long
					  	}
					});

		// refresh the tr's contents
		updateRow(tr,theRow);

		// add the tr to the table
		tbody.appendChild(tr);
	}
}

function reSort(theTable,k) {
	if(theTable.curSortMode==k) {
		return; // no op
	}
	theTable.curSortMode=k;
	insertRowsIntoTbody(theTable,theTable.getElementsByTagName("tbody")[0]);
	var lis = theTable.sortMode.getElementsByTagName("li");
	for(i in lis) {
		var li = lis[i];
		if(li.mode==k) {
			li.className="selected";
		} else {
			li.className = "notselected";
		}
	}
}

/**
 *
 * Setup the 'sort' popup menu.
 */
function setupSortmode(theTable) {
	var theSortmode = theTable.sortMode;
	// ignore what's there
	removeAllChildNodes(theSortmode);
	var listOfLists = Object.keys(theTable.json.displaySets);
	var itemCount = Object.keys(theTable.json.section.rows).length;
	var size = document.createElement("span");
	size.className="d-sort-size";
	var ul = document.createElement("ul");
	if(itemCount>0) {
		for(i in listOfLists) {
			var k = listOfLists[i];
			if(k=="default") continue;

			var a = document.createElement("li");
			a.onclick = (function() {
				var kk = k;
				return function() {
					reSort(theTable, kk);
				};
			})();
			a.appendChild(document.createTextNode(theTable.json.displaySets[k].displayName));
			a.mode=k;
			if(k==theTable.curSortMode) {
				a.className="selected";
			} else {
				a.className = "notselected";
			}
			ul.appendChild(a);
		}
	}
	theTable.json.section.itemCount = itemCount;

	if(itemCount==0 && theTable.json.section.skippedDueToCoverage) {
		size.appendChild(document.createTextNode(
				stui.sub("itemCountAllHidden", theTable.json.section)
				));
		size.className = "d-sort-size0";
	} else if(itemCount==0) {
		size.appendChild(document.createTextNode(
				stui.sub("itemCountNone", theTable.json.section)
				));
		size.className = "d-sort-size0";
	} else if(theTable.json.section.skippedDueToCoverage) {
		size.appendChild(document.createTextNode(
				stui.sub("itemCountHidden",theTable.json.section)
				));
	} else {
		size.appendChild(document.createTextNode(
				stui.sub("itemCount", theTable.json.section)));
	}
}

/**
 * get numeric, given string
 * @method covValue
 * @param {String} lev
 * @return {Number} or 0
 */
function covValue(lev) {
	lev = lev.toUpperCase();
	if(window.surveyLevels && window.surveyLevels[lev]) {
		return parseInt(window.surveyLevels[lev].level);
	} else {
		return 0;
	}
}

function covName(lev) {
	if(!window.surveyLevels) return null;

	for(var k in window.surveyLevels) {
		if(parseInt(window.surveyLevels[k].level) == lev) {
			return k.toLowerCase();
		}
	}
	return null;
}

function effectiveCoverage() {
	if(!window.surveyOrgCov) {
		throw new Error("surveyOrgCov not yet initialized");
	}

	if(surveyUserCov) {
		return covValue(surveyUserCov);
	} else {
		return covValue(surveyOrgCov);
	}
}

function updateCovFromJson(json) {

	if(json.covlev_user && json.covlev_user != 'default') {
		window.surveyUserCov = json.covlev_user;
	} else {
		window.surveyUserCov = null;
	}

	if(json.covlev_org) {
		window.surveyOrgCov = json.covlev_org;
	} else {
		window.surveyOrgCov = null;
	}
}

/**
 * Update the coverage classes, show and hide things in and out of coverage
 * @method updateCoverage
 */
function updateCoverage(theDiv) {
	if(theDiv == null) return;
	var theTable = theDiv.theTable;
	if(theTable==null) return;
	if(!theTable.origClass) {
		theTable.origClass = theTable.className;
	}
	if(window.surveyLevels!=null) {
		var effective = effectiveCoverage();
		var newStyle = theTable.origClass;
		for(var k in window.surveyLevels) {
			var level = window.surveyLevels[k];

			if(effective <  parseInt(level.level)) {
				newStyle = newStyle + " hideCov"+level.level;
			}
		}
		if(newStyle != theTable.className) {
			theTable.className = newStyle;
		}
	}
}

/**
 * Prepare rows to be inserted into theDiv
 * @method insertRows
 */
function insertRows(theDiv,xpath,session,json) {
	var theTable = theDiv.theTable;
	var doInsertTable = null;

	removeAllChildNodes(theDiv);
	window.insertLocaleSpecialNote(theDiv);
	//recreated table in every case
	theTable = cloneLocalizeAnon(dojo.byId('proto-datatable'));
	if(isDashboard())
		theTable.className += ' dashboard';
	else
		theTable.className += ' vetting-page';
	updateCoverage(theDiv);
	localizeFlyover(theTable);
	theTable.theadChildren = getTagChildren(theTable.getElementsByTagName("tr")[0]);
	var toAdd = dojo.byId('proto-datarow');  // loaded from "hidden.html", which see.
	var rowChildren = getTagChildren(toAdd);
	theTable.config = surveyConfig ={};
	for(var c in rowChildren) {
		rowChildren[c].title = theTable.theadChildren[c].title;
		if(rowChildren[c].id) {
			surveyConfig[rowChildren[c].id] = c;
			stdebug("  config."+rowChildren[c].id+" = children["+c+"]");
			if(false&&stdebug_enabled) {
				removeAllChildNodes(rowChildren[c]);
				rowChildren[c].appendChild(createChunk("config."+rowChildren[c].id+"="+c));
			}
		} else {
			stdebug("(proto-datarow #"+c+" has no id");
		}
	}
	if(stdebug_enabled) stdebug("Table Config: " + JSON.stringify(theTable.config));

	theTable.toAdd = toAdd;
	if(!json.canModify) {
		setDisplayed(theTable.theadChildren[theTable.config.nocell], false);
	}
	theTable.sortMode = cloneAnon(dojo.byId('proto-sortmode'));
	theDiv.appendChild(theTable.sortMode);
	theTable.myTRs = [];
	theDiv.theTable = theTable;
	theTable.theDiv = theDiv;
	doInsertTable=theTable;

	// append header row
	theTable.json = json;
	theTable.xpath = xpath;
	theTable.hitCount=0;
	theTable.session = session;

	if(!theTable.curSortMode) {
		theTable.curSortMode = theTable.json.displaySets["default"];
		// hack - choose one of these
		if(theTable.json.displaySets.codecal) {
			theTable.curSortMode = "codecal";
		} else if(theTable.json.displaySets.metazon) {
			theTable.curSortMode = "metazon";
		}
	}
	setupSortmode(theTable);

	var tbody = theTable.getElementsByTagName("tbody")[0];
	insertRowsIntoTbody(theTable,tbody);
	if(doInsertTable) {
		theDiv.appendChild(doInsertTable);
	} else {
		setDisplayed(theTable, true);
	}
	hideLoader(theDiv.loader);
}

function loadStui(loc, cb) {
	if(!stui.ready) {
		require(["dojo/i18n!./surveyTool/nls/stui.js"], function(stuibundle){
			if(!stuidebug_enabled) {
				stui.str = function(x) { if(stuibundle[x]) return stuibundle[x]; else return x; };
				stui.sub = function(x,y) { return dojo.string.substitute(stui.str(x), y);};
			} else {
				stui.str = stui_str; // debug
				stui.sub = function(x,y) { return stui_str(x) + '{' +  Object.keys(y) + '}'; };
			}
			stuibundle.htmlbaseline = stui.htmlbaseline = BASELINE_LANGUAGE_NAME;
			stui.ready=true;
			if(cb) cb(stui);
		});
	} else {
		if(cb) cb(stui);
	}
	return stui;
}
function firstword(str) {
	return str.split(" ")[0];
}

function appendIcon(toElement, className) {
	var e = createChunk(null, "div", className);
	toElement.appendChild(e);
	return e;
}

function hideAfter(whom, when) {
	if(!when) {
		when=10000;
	}
	setTimeout(function() {
		whom.style.opacity="0.8";
	}, when/3);
	setTimeout(function() {
		whom.style.opacity="0.5";
	}, when/2);
	setTimeout(function() {
		setDisplayed(whom, false);
	}, when);
	return whom;
}

function appendInputBox(parent, which) {
	var label = createChunk(stui.str(which), "div", which);
	var input = document.createElement("input");
	input.stChange = function(onOk,onErr){};
	var change = createChunk(stui.str("appendInputBoxChange"), "button", "appendInputBoxChange");
	var cancel = createChunk(stui.str("appendInputBoxCancel"), "button", "appendInputBoxCancel");
	var notify = document.createElement("div");
	notify.className="appendInputBoxNotify";
	input.className = "appendInputBox";
	label.appendChild(change);
	label.appendChild(cancel);
	label.appendChild(notify);
	label.appendChild(input);
	parent.appendChild(label);
	input.label = label;

	var doChange = function() {
		addClass(label, "d-item-selected");
		removeAllChildNodes(notify);
		notify.appendChild(createChunk(stui.str("loading"),'i'));
		var onOk = function(msg) {
			removeClass(label, "d-item-selected");
			removeAllChildNodes(notify);
			notify.appendChild(hideAfter(createChunk(msg,"span","okayText")));
		};
		var onErr = function(msg) {
			removeClass(label, "d-item-selected");
			removeAllChildNodes(notify);
			notify.appendChild(createChunk(msg,"span","stopText"));
		};

		input.stChange(onOk,onErr);
	};

	var changeFn = function(e) {
		doChange();
		stStopPropagation(e);
		return false;
	};
	var cancelFn = function(e) {
		input.value="";
		doChange();
		stStopPropagation(e);
		return false;
	};
	var keypressFn = function(e) {
		if(!e || !e.keyCode)  {
			return true; // not getting the point here.
		} else if(e.keyCode == 13) {
			doChange();
			return false;
		} else {
			return true;
		}
	};
	listenFor(change, "click", changeFn);
	listenFor(cancel, "click", cancelFn);
	listenFor(input, "keypress", keypressFn);
	return input;
}

/**
 * Show the surveyCurrentId row
 * @method scrollToItem
 */
function scrollToItem() {
	if(surveyCurrentId!=null && surveyCurrentId!='') {
		require(["dojo/window"], function(win) {
			var xtr = dojo.byId("r@"+surveyCurrentId);
			if(xtr!=null) {
				console.log("Scrolling to " + surveyCurrentId);
				win.scrollIntoView("r@"+surveyCurrentId);
			}
		});
	}
}

/**
 * copy of menu data
 * @property _thePages
 */
var _thePages = null;

window.locmap = new LocaleMap(null);

/**
 * @method locInfo
 * @param loc  optional
 * @returns locale bundle
 */
function locInfo(loc) {
	if(!loc) {
		loc = window.surveyCurrentLocale;
	}
	return locmap.getLocaleInfo(loc);
}

var overridedir = null;
function setLang(node, loc) {
	var info = locInfo(loc);

	if(overridedir){
		node.dir = overridedir;
	} else if (info.dir) {
		node.dir = info.dir;
	}

	if(info.bcp47) {
		node.lang = info.bcp47;
	}
}

/**
 * Utilities for the 'v.jsp' (new dispatcher) page.  Call this once in the page. It expects to find a node #DynamicDataSection
 * @method showV
 */
function showV() {
	// REQUIRES
	require([
	         "dojo/ready",
	         "dojo/dom",
	         "dojo/parser",
	         "dijit/DropDownMenu",
	         "dijit/form/DropDownButton",
	         "dijit/MenuSeparator",
	         "dijit/MenuItem",
	         "dijit/form/TextBox",
	         "dijit/form/Button",
	         "dijit/CheckedMenuItem",
	         "dijit/Dialog",
	         "dijit/registry",
	         "dijit/PopupMenuItem",
	         "dijit/form/Select",
	         "dojox/form/BusyButton",
	         "dijit/layout/StackContainer",
	         "dijit/TitlePane",
	         "dojo/hash",
	         "dojo/topic",
	         "dojo/dom-construct",
	         "dojo/number",
	         "dojo/domReady!"
	         ],
	         // HANDLES
	         function(
	        		 ready,
	        		 dom,
	        		 parser,
	        		 DropDownMenu,
	        		 DropDownButton,
	        		 MenuSeparator,
	        		 MenuItem,
	        		 TextBox,
	        		 Button,
	        		 CheckedMenuItem,
	        		 Dialog,
	        		 registry,
	        		 PopupMenuItem,
	        		 Select,
	        		 BusyButton,
	        		 StackContainer,
	        		 TitlePane,
	        		 dojoHash,
	        		 dojoTopic,
	        		 domConstruct,
	        		 dojoNumber
	         ) {
		loadStui(null, function(/*stui*/) {

		var appendLocaleLink = function appendLocaleLink(subLocDiv, subLoc, subInfo, fullTitle) {
			var name = locmap.getRegionAndOrVariantName(subLoc);
			if(fullTitle) {
				name = locmap.getLocaleName(subLoc);
			}
			var clickyLink = createChunk(name, "a", "locName");
			clickyLink.href = linkToLocale(subLoc);
			subLocDiv.appendChild(clickyLink);
			if(subInfo == null) {
				console.log("* internal: subInfo is null for " + name + " / " + subLoc);
			}
			if(subInfo.name_var) {
				addClass(clickyLink, "name_var");
			}
			clickyLink.title=subLoc; // remove auto generated "locName.title"

			if(subInfo.readonly) {
				addClass(clickyLink, "locked");
				addClass(subLocDiv, "hide");

				if(subInfo.readonly_why) {
					clickyLink.title = subInfo.readonly_why;
				} else if(subInfo.dcChild) {
					clickyLink.title = stui.sub("defaultContentChild_msg", { info: subInfo, locale: subLoc, dcChildName: locmap.getLocaleName(subInfo.dcChild)});
				} else {
					clickyLink.title = 	stui.str("readonlyGuidance");
				}
			} else if(window.canmodify && subLoc in window.canmodify) {
				addClass(clickyLink, "canmodify");
			}
			else {
				addClass(subLocDiv, "hide");
			}
			return clickyLink;
		};

		/**
		 * list of pages to use with the flipper
		 * @property pages
		 */
		var pages = {
				loading: "LoadingMessageSection",
				data: "DynamicDataSection",
				other: "OtherSection",
		};
		var flipper = new Flipper( [pages.loading, pages.data, pages.other] );

		var pucontent = dojo.byId("itemInfo");
		var theDiv = flipper.get(pages.data);
		theDiv.pucontent = pucontent;
		theDiv.stui = loadStui();

		pucontent.appendChild(createChunk(stui.str("itemInfoBlank"),"i"));

		/**
		 * List of buttons/titles to set.
		 * @property menubuttons
		 */
		var menubuttons = {
			locale: "title-locale",
			section: "title-section",
			page: "title-page",
			dcontent: "title-dcontent",

			set: function(x,y) {
				stdebug("menuset " + x + " = " + y);
				var cnode = dojo.byId(x+"-container");
				var wnode = this.getRegistry(x);
				var dnode = this.getDom(x);
				if(!cnode) cnode = dnode; // for Elements that do their own stunts
				if(y && y !== '-' && y !== '') {
					if(wnode != null) {
						wnode.set('label',y);
					} else  {
						updateIf(x,y); // non widget
					}
					setDisplayed(cnode, true);
				} else {
					setDisplayed(cnode, false);
					if(wnode != null) {
						wnode.set('label','-');
					} else  {
						updateIf(x,'-'); // non widget
					}
				}
			},
			getDom: function(x) {
				return dojo.byId(x);
			},
			getRegistry: function(x) {
				return registry.byId(x);
			},
			getContainer: function(x) {
				return dojo.byId(x+"-container");
			}
		};

		// TODO remove this debug item
		window.__FLIPPER = flipper;

		/**
		 * Manage additional special pages
		 * @class OtherSpecial
		 */
		function OtherSpecial() {
			// cached page list
			this.pages = {};
		}

		/**
		 * @function getSpecial
		 */
		OtherSpecial.prototype.getSpecial = function getSpecial(name) {
			return this.pages[name];
		};

		/**
		 * @function loadSpecial
		 */
		OtherSpecial.prototype.loadSpecial = function loadSpecial(name, onSuccess, onFailure) {
			var special = this.getSpecial(name);
			var otherThis = this;
			if(special) {
				stdebug("OS: Using cached special: "+ name);
				onSuccess(special);
			} else if (special === null) {
				stdebug("OS: cached NULL: " + name);
				onFailure("Special page failed to load: " + name);
			} else {
				stdebug("OS: Attempting load.." + name);
				try {
					require(["js/special/"+name+".js"], function(specialFn) {
						stdebug("OS: Loaded, instantiatin':" + name);
						var special = new specialFn();
						special.name = name;
						otherThis.pages[name] = special; // cache for next time

						stdebug("OS: SUCCESS! " + name);
						onSuccess(special);
					});
				} catch(e) {
					stdebug("OS: Load FAIL!:" + name + " - " + e.message + " - " + e);
					if(!otherThis.pages[name]) { // if the load didn't complete:
						otherThis.pages[name]=null; // mark as don't retry load.
					}
					onFailure(e);
				}
			}
		};

		/**
		 * @function parseHash
		 */
		OtherSpecial.prototype.parseHash = function parseHash(name, hash, pieces) {
			this.loadSpecial(name, function onSuccess(special) {
				special.parseHash(hash, pieces);
			}, function onFailure(e) {
				console.log("OtherSpecial.parseHash: Failed to load " + name + " - " + e);
			});
		};

		/**
		 * @function handleIdChanged
		 */
		OtherSpecial.prototype.handleIdChanged = function handleIdChanged(name, id) {
			this.loadSpecial(name, function onSuccess(special) {
				special.handleIdChanged(id);
			}, function onFailure(e) {
				console.log("OtherSpecial.handleIdChanged: Failed to load " + name + " - " + e);
			});
		};

		/**
		 * @function showPage
		 */
		OtherSpecial.prototype.show = function show(name, params) {
			this.loadSpecial(name, function onSuccess(special) {
				// populate the params a little more
				params.otherSpecial = this;
				params.name = name;
				params.special = special;

				// add anything from scope..

				params.exports = {
						// All things that should be separate AMD modules..
						appendLocaleLink: appendLocaleLink,
						handleDisconnect: handleDisconnect,
						clickToSelect: clickToSelect
				};

				special.show(params);
			}, function onFailure(err) {

				// extended error
				var loadingChunk;
				var msg_fmt = stui.sub("v_bad_special_msg",
						{special: name });
				params.flipper.flipTo(params.pages.loading, loadingChunk = createChunk(msg_fmt,"p","errCodeMsg"));
				isLoading=false;
			});
		};

		/**
		 * instance of otherSpecial manager
		 * @property otherSpecial
		 */
		var otherSpecial = new OtherSpecial();

		/**
		 * parse the hash string into surveyCurrent___ variables.
		 * Expected to update document.title also.
		 * @method parseHash
		 * @param {String} id
		 */
		window.parseHash = function parseHash(hash) {
			function updateWindowTitle() {
				var t=stui.str('survey_title');
				if(surveyCurrentLocale && surveyCurrentLocale != '') {
					t = t + ': '+locmap.getLocaleName(surveyCurrentLocale);
				}
				if(surveyCurrentSpecial && surveyCurrentSpecial!='') {
					t = t + ': ' + stui.str('special_'+surveyCurrentSpecial);
				}
				if(surveyCurrentPage && surveyCurrentPage !='') {
					t = t + ': ' + surveyCurrentPage;
				}
				document.title = t;
			};
			if(hash) {
				var pieces = hash.substr(0).split("/");
				if(pieces.length > 1) {
					surveyCurrentLocale = pieces[1]; // could be null
				} else {
					surveyCurrentLocale = '';
				}
				if(pieces[0].length==0 && surveyCurrentLocale!=''&&surveyCurrentLocale!=null) {
					if(pieces.length>2) {
						surveyCurrentPage = pieces[2];
						if(pieces.length>3){
							surveyCurrentId = pieces[3];
							if(surveyCurrentId.substr(0,2)=='x@') {
								surveyCurrentId=surveyCurrentId.substr(2);
							}
						} else {
							surveyCurrentId = '';
						}
					} else {
						surveyCurrentPage='';
						surveyCurrentId='';
					}
					window.surveyCurrentSpecial=null;
				} else {
					window.surveyCurrentSpecial = pieces[0];
					if(surveyCurrentSpecial=='') {
						surveyCurrentSpecial='locales';
					}
					if(surveyCurrentSpecial=='locales') {
						// allow locales list to retain ID / Page string for passthrough.
						surveyCurrentLocale='';
						if(pieces.length>2) {
							surveyCurrentPage = pieces[2];
							if(pieces.length>3){
								surveyCurrentId = pieces[3];
								if(surveyCurrentId.substr(0,2)=='x@') {
									surveyCurrentId=surveyCurrentId.substr(2);
								}
							} else {
								surveyCurrentId = '';
							}
						} else {
							surveyCurrentPage='';
							surveyCurrentId='';
						}
					} else if(isReport(surveyCurrentSpecial)) { // allow page and ID to fall through.
						if(pieces.length>2) {
							surveyCurrentPage = pieces[2];
							if(pieces.length>3){
								surveyCurrentId = pieces[3];
							} else {
								surveyCurrentId = '';
							}
						} else {
							surveyCurrentPage='';
							surveyCurrentId='';
						}
					} else {
						otherSpecial.parseHash(surveyCurrentSpecial, hash, pieces);
					}
				}
			} else {
				surveyCurrentLocale = '';
				surveyCurrentSpecial='locales';
				surveyCurrentId='';
				surveyCurrentPage='';
				surveyCurrentSection='';
			}
			updateWindowTitle();

			 // if there is no locale id, refresh the search.
			if(!surveyCurrentLocale) {
				searchRefresh();
			}
		};


		/**
		 * update hash (and title)
		 * @method replaceHash
		 * @param doPush {Boolean} if true, do a push (instead of replace)
		 */
		window.replaceHash = function replaceHash(doPush) {
			if(!doPush) doPush = false; // by default -replace.
			var theId = window.surveyCurrentId;
			if(theId == null) theId = '';
			var theSpecial = window.surveyCurrentSpecial;
			if(theSpecial == null) theSpecial = '';
			var thePage = window.surveyCurrentPage;
			if(thePage == null) thePage = '';
			var theLocale = window.surveyCurrentLocale;
			if(theLocale==null) theLocale = '';
			var newHash =  '#' + theSpecial + '/' + theLocale + '/' + thePage + '/' + theId;
			if(newHash != dojoHash()) {
				dojoHash(newHash , !doPush);
			}
		};

		window.updateCurrentId = function updateCurrentId(id) {
			if(id==null) id = '';
		    if(surveyCurrentId != id) { // don't set if already set.
			    surveyCurrentId = id;
			    replaceHash(false); // usually dont want to save
		    }
		};

		// (back to showV) some setup.
		// click on the title to copy (permalink)
		clickToSelect(dojo.byId("ariScroller"));
		updateIf("title-dcontent-link",stui.str("defaultContent_titleLink"));

		// TODO - rewrite using AMD
		/**
		 * @param postData optional - makes this a POST
		 */
		window.myLoad = function myLoad(url, message, handler, postData, headers) {
			var otime = new Date().getTime();
			console.log("MyLoad: " + url + " for " + message);
			var errorHandler = function(err, ioArgs){
				console.log('Error: ' + err + ' response ' + ioArgs.xhr.responseText);
				handleDisconnect("Could not fetch " + message + " - error " + err.name + " / " + err.message + "\n" + ioArgs.xhr.responseText + "\n url: " + url + "\n", null, "disconnect");
			};
			var loadHandler = function(json){
				console.log("        "+url+" loaded in "+(new Date().getTime()-otime)+"ms");
				try {
					handler(json);
					//resize height
					$('#main-row').css({height:$('#main-row>div').height()});
				}catch(e) {
					console.log("Error in ajax post ["+message+"]  " + e.message + " / " + e.name );
					handleDisconnect("Exception while  loading: " + message + " - "  + e.message + ", n="+e.name, null); // in case the 2nd line doesn't work
				}
			};
			var xhrArgs = {
					url: url,
					handleAs:"json",
					load: loadHandler,
					error: errorHandler,
					postData: postData,
					headers: headers
			};
			queueXhr(xhrArgs);
		};

		/**
		 * Verify that the JSON returned is as expected.
		 * @method verifyJson
		 * @param json the returned json
		 * @param subkey the key to look for,  json.subkey
		 * @return true if OK, false if bad
		 */
		function verifyJson(json, subkey) {
			if(!json) {
				console.log("!json");
				showLoader(null,"Error while  loading "+subkey+":  <br><div style='border: 1px solid red;'>" + "no data!" + "</div>");
				return false;
			} else if(json.err_code) {
				var msg_fmt = formatErrMsg(json, subkey);
				var loadingChunk;
				flipper.flipTo(pages.loading, loadingChunk = createChunk(msg_fmt, "p", "errCodeMsg"));
				var retryButton = createChunk(stui.str("loading_reload"),"button");
				loadingChunk.appendChild(retryButton);
				retryButton.onclick = function() { 	window.location.reload(true); };
				return false;
			} else if(json.err) {
				console.log("json.err!" + json.err);
				showLoader(null,"Error while  loading "+subkey+": <br><div style='border: 1px solid red;'>" + json.err + "</div>");
				handleDisconnect("while loading "+subkey+"" ,json);
				return false;
			} else if(!json[subkey]) {
				console.log("!json.oldvotes");
				showLoader(null,"Error while  loading "+subkey+": <br><div style='border: 1px solid red;'>" + "no data" + "</div>");
				handleDisconnect("while loading- no "+subkey+"",json);
			} else {
				return true;
			}
		}

		window.showCurrentId = function() {
			if(surveyCurrentSpecial && surveyCurrentSpecial != '' && !isDashboard()) {
				otherSpecial.handleIdChanged(surveyCurrentSpecial, showCurrentId);
			} else {
				if(surveyCurrentId != '') {
				    var xtr = dojo.byId('r@' + surveyCurrentId);
				    if(!xtr) {
				        console.log("Warning could not load id " + surveyCurrentId + " does not exist");
				        window.updateCurrentId(null);
				    } else if(xtr.proposedcell && xtr.proposedcell.showFn) {
				        // TODO: visible? coverage?
				        window.showInPop("",xtr,xtr.proposedcell, xtr.proposedcell.showFn, true);
				        console.log("Changed to " + surveyCurrentId);
				        if(!isDashboard())
				        	scrollToItem();
				    } else {
				        console.log("Warning could not load id " + surveyCurrentId + " - not setup - " + xtr.toString() + " pc=" + xtr.proposedcell + " sf = " + xtr.proposedcell.showFn);
				    }
				}
			}
		};


		window.ariRetry = function() {
			ariDialog.hide();
			window.location.reload(true);
		};

		window.showARIDialog = function(why, json, word, oneword, p, what) {
			console.log('showARIDialog');
			p.parentNode.removeChild(p);

			if(didUnbust) {
				why = why + "\n\n" + stui.str('ari_force_reload');
			}

			// setup with why
			var ari_message;

			if(json && json.session_err) {
				ari_message = stui_str("ari_sessiondisconnect_message");
			} else {
				ari_message = stui.str('ari_message');
			}

			var ari_submessage = formatErrMsg(json, what);

			updateIf('ariMessage', ari_message.replace(/\n/g,"<br>"));
			updateIf('ariSubMessage', ari_submessage.replace(/\n/g,"<br>"));
			updateIf('ariScroller',window.location + '<br>' + why.replace(/\n/g,"<br>"));
			// TODO: update  ariMain and ariRetryBtn
			hideOverlayAndSidebar();

			ariDialog.show();
			var oneword = dojo.byId("progress_oneword");
			oneword.onclick = function() {
				if(disconnected) {
					ariDialog.show();
				}
			};
		};

		function updateCoverageMenuTitle() {
			var cov = '';
			if(surveyUserCov) {
				$('#coverage-info').text(stui.str('coverage_' + surveyUserCov));
			}
			else {
				$('#coverage-info').text(stui.sub('coverage_auto_msg', {surveyOrgCov: stui.str('coverage_' + surveyOrgCov)}));
			}
		}

		function updateLocaleMenu() {
            if(surveyCurrentLocale!=null && surveyCurrentLocale!='' && surveyCurrentLocale!='-') {
        		surveyCurrentLocaleName = locmap.getLocaleName( surveyCurrentLocale);
        		var bund = locmap.getLocaleInfo(surveyCurrentLocale);
        		if(bund) {
        			if( bund.readonly) {
        				addClass(menubuttons.getDom(menubuttons.locale), "locked");
        			} else {
            			removeClass(menubuttons.getDom(menubuttons.locale), "locked");
        			}

        			if(bund.dcChild) {
        				menubuttons.set(menubuttons.dcontent, stui.sub("defaultContent_header_msg", {info: bund, locale: surveyCurrentLocale, dcChild: locmap.getLocaleName(bund.dcChild)}));
        			} else {
        				menubuttons.set(menubuttons.dcontent);
        			}
        		} else {
        			removeClass(menubuttons.getDom(menubuttons.locale), "locked");
    				menubuttons.set(menubuttons.dcontent);
        		}
            } else {
            	surveyCurrentLocaleName = '';
            	removeClass(menubuttons.getDom(menubuttons.locale), "locked");
				menubuttons.set(menubuttons.dcontent);
            }
            menubuttons.set(menubuttons.locale, surveyCurrentLocaleName);
		}
		/**
		 * Update the #hash and menus to the current settings.
		 * @method updateHashAndMenus
		 * @param doPush {Boolean} if false, do not add to history
		 */
		function updateHashAndMenus(doPush) {
			/**
			 * 'name' - the js/special/___.js name
			 * 'hidden' - true to hide the item
			 * 'title' - override of menu name
			 * @property specialItems
			 */
			var specialItems = new Array();
			if(surveyUser != null){
				specialItems = [
				    {divider: true},

					{title: 'Admin Panel', url: surveyUserURL.adminPanel, display: (surveyUser && surveyUser.userlevelName === 'ADMIN')},
					{divider: true, display: (surveyUser && surveyUser.userlevelName === 'ADMIN')},

				    {title: 'My Account'}, // My Account section

				    {title: 'Settings', level: 2, url: surveyUserURL.myAccountSetting, display: surveyUserPerms.userExist },
				    {title: 'Lock (Disable) My Account', level: 2, url: surveyUserURL.disableMyAccount, display: surveyUserPerms.userExist },

				    {divider: true},
				    {title: 'My Votes'}, // My Votes section

				    {special: 'oldvotes', level: 2, display: surveyUserPerms.userCanImportOldVotes },
				    {title: 'See My Recent Activity', level: 2, url: surveyUserURL.recentActivity },
				    {title: 'Upload XML', level: 2, url: surveyUserURL.xmlUpload },

				    {divider: true},
				    {title: 'My Organization('+organizationName+')'}, // My Organization section

				    {special: 'vsummary', level: 2, display: surveyUserPerms.userCanUseVettingSummary },
				    {title: 'List ' + org + ' Users', level: 2, url: surveyUserURL.manageUser, display: (surveyUserPerms.userIsTC || surveyUserPerms.userIsVetter) },
				    {title: 'LOCKED: Note: your account is currently locked.', level: 2,  display: surveyUserPerms.userIsLocked, bold: true},

				    {divider: true},
				    {title: 'Forum'}, // Forum section

				    {special: 'flagged', level: 2, img: surveyImgInfo.flag},
				    {title: 'RSS 2.0', level: 2, url: surveyUserURL.RSS, img: surveyImgInfo.RSS},
				    {special: 'mail', level: 2, display: !surveyOfficial },

				    {divider: true},
				    {title: 'Informational'}, // Informational section

				    {special: 'statistics', level: 2 },
				    {title: 'About', level: 2, url: surveyUserURL.about },
				    {title: 'Lookup a code or xpath', level: 2, url: surveyUserURL.browse, display: surveyUserPerms.hasDataSource },

				    {divider: true},
				];
			}
			if(!doPush) {doPush = false;}
			replaceHash(doPush); // update the hash
			updateLocaleMenu();

			if(surveyCurrentLocale==null) { // deadcode?
				menubuttons.set(menubuttons.section);
				if(surveyCurrentSpecial!=null) {
					var specialId = "special_"+surveyCurrentSpecial;
					menubuttons.set(menubuttons.page, stui_str(specialId));
				} else {
					menubuttons.set(menubuttons.page);
				}
				return; // nothing to do.
			}
			var titlePageContainer = dojo.byId("title-page-container");

			/**
			 * Just update the titles of the menus. Internal to updateHashAndMenus
			 * @method updateMenuTitles
			 */
			function updateMenuTitles(menuMap) {
				if(menubuttons.lastspecial === undefined) {
					menubuttons.lastspecial = null;

					// Set up the menu here?
					var parMenu = dojo.byId("manage-list");
					for(var k =0; k< specialItems.length; k++) {
						var item = specialItems[k];
						(function(item){
							if(item.display != false) {
								var subLi = document.createElement("li");
								if(item.special){ // special items so look up in stui.js
									item.title = stui.str('special_' + item.special);
									item.url = '#' + item.special;
									item.blank = false;
								}
								if(item.url){
									var subA = document.createElement("a");

									if(item.img){ // forum may need images attached to it
										var Img=document.createElement("img");
										Img.setAttribute('src', item.img.src);
										Img.setAttribute('alt', item.img.alt);
										Img.setAttribute('title', item.img.src);
										Img.setAttribute('border', item.img.border);

										subA.appendChild(Img);
									}
									subA.appendChild(document.createTextNode(item.title+' '));
									subA.href = item.url;

									if(item.blank != false){
										subA.target = '_blank';
										subA.appendChild(createChunk('','span','glyphicon glyphicon-share manage-list-icon'));
									}

									if(item.level){ // append it to appropriate levels
										var level = item.level;
										for(var i=0; i< level-1; i++){
											var sublevel = document.createElement("ul");
											sublevel.appendChild(subA);
											subA = sublevel;
										}
									}
									subLi.appendChild(subA);
								}
								if(!item.url && !item.divider){ // if it is pure text/html & not a divider
									if(!item.level){
										subLi.appendChild(document.createTextNode(item.title+' '));
									}else{
										var subA = null;
										if(item.bold){
											subA = document.createElement("b");
										}else if(item.italic){
											subA = document.createElement("i");
										}else{
											subA = document.createElement("span");
										}
										subA.appendChild(document.createTextNode(item.title+' '));

										var level = item.level;
										for(var i=0; i< level-1; i++){
											var sublevel = document.createElement("ul");
											sublevel.appendChild(subA);
											subA = sublevel;
										}
										subLi.appendChild(subA);
									}
								}
								if(item.divider) {
									subLi.className = 'nav-divider';
								}
								parMenu.appendChild(subLi);
							}
						})(item);
					}
				}

				if(menubuttons.lastspecial) {
					removeClass(menubuttons.lastspecial, "selected");
				}

				updateLocaleMenu(menuMap);
				if(surveyCurrentSpecial!= null && surveyCurrentSpecial != '') {
					var specialId = "special_"+surveyCurrentSpecial;
					$('#section-current').html(stui_str(specialId));
					setDisplayed(titlePageContainer, false);
				} else if(!menuMap) {
					setDisplayed(titlePageContainer, false);
				} else {
					if(menuMap.sectionMap[window.surveyCurrentPage]) {
						surveyCurrentSection = surveyCurrentPage; // section = page
						$('#section-current').html(menuMap.sectionMap[surveyCurrentSection].name);
						setDisplayed(titlePageContainer, false); // will fix title later
					} else if(menuMap.pageToSection[window.surveyCurrentPage]) {
						var mySection = menuMap.pageToSection[window.surveyCurrentPage];
						surveyCurrentSection = mySection.id;
						$('#section-current').html(mySection.name);
						setDisplayed(titlePageContainer, false); // will fix title later
					} else {
						$('#section-current').html(stui_str("section_general"));
						setDisplayed(titlePageContainer, false);
					}
				}
			}

			/**
			 * @method updateMenus
			 */
			function updateMenus(menuMap) {
				// initialize menus
				if(!menuMap.menusSetup) {
					menuMap.menusSetup=true;
					menuMap.setCheck = function(menu, checked,disabled) {
						menu.set('iconClass', checked ? "dijitMenuItemIcon menu-x" : "dijitMenuItemIcon menu-o");
						menu.set('disabled', disabled);
					};
					var menuSection = registry.byId("menu-section");
					menuMap.section_general = new MenuItem({
						label: stui_str("section_general"),
						iconClass:  "dijitMenuItemIcon ",
						disabled: true,
						onClick: function(){
							if(surveyCurrentPage!='' || (surveyCurrentSpecial!='' && surveyCurrentSpecial != null)) {
								surveyCurrentId = ''; // no id if jumping pages
								surveyCurrentPage = '';
								surveyCurrentSection = '';
								surveyCurrentSpecial = '';
								updateMenuTitles(menuMap);
								reloadV();
							}
						}
					});
					menuSection.addChild(menuMap.section_general);
					for(var j in menuMap.sections) {
						(function (aSection){
							aSection.menuItem = new MenuItem({
								label: aSection.name,
								iconClass: "dijitMenuItemIcon",
								onClick: function(){
										surveyCurrentId = '!'; // no id if jumping pages
										surveyCurrentPage = aSection.id;
										surveyCurrentSpecial = '';
										updateMenus(menuMap);
										updateMenuTitles(menuMap);
										reloadV();
								},
								disabled: true
							});

							menuSection.addChild(aSection.menuItem);
						})(menuMap.sections[j]);
					}

					menuSection.addChild(new MenuSeparator());

					menuMap.forumMenu = new MenuItem({
						label: stui_str("section_forum"),
						iconClass: "dijitMenuItemIcon", // menu-chat
						disabled: true,
						onClick: function(){
							surveyCurrentId = '!'; // no id if jumping pages
							surveyCurrentPage = '';
							surveyCurrentSpecial = 'forum';
							updateMenus(menuMap);
							updateMenuTitles(menuMap);
							reloadV();						}
					});
					menuSection.addChild(menuMap.forumMenu);
				}

				updateMenuTitles(menuMap);

				var myPage = null;
				var mySection = null;
				if(surveyCurrentSpecial==null || surveyCurrentSpecial=='') {
					// first, update display names
					if(menuMap.sectionMap[window.surveyCurrentPage]) { // page is really a section
						mySection = menuMap.sectionMap[surveyCurrentPage];
						myPage = null;
					} else if(menuMap.pageToSection[window.surveyCurrentPage]) {
						mySection = menuMap.pageToSection[surveyCurrentPage];
						myPage = mySection.pageMap[surveyCurrentPage];
					}
					if(mySection!==null) {
						// update menus under 'page' - peer pages
						if(!titlePageContainer.menus) {
							titlePageContainer.menus = {};
						}

						// hide all. TODO use a foreach model?
						for(var zz in titlePageContainer.menus) {
							var aMenu = titlePageContainer.menus[zz];
							aMenu.set('label','-');
						}

						var showMenu = titlePageContainer.menus[mySection.id];

						if(!showMenu) {
							// doesn't exist - add it.
							var menuPage = new DropDownMenu();
							for(var k in mySection.pages) { // use given order
								(function(aPage) {

									var pageMenu = aPage.menuItem =  new MenuItem({
										label: aPage.name,
										iconClass: (aPage.id == surveyCurrentPage) ? "dijitMenuItemIcon menu-x" : "dijitMenuItemIcon menu-o",
										onClick: function(){
											surveyCurrentId = ''; // no id if jumping pages
											surveyCurrentPage = aPage.id;
											updateMenuTitles(menuMap);
											reloadV();
										},
										disabled: (effectiveCoverage()<parseInt(aPage.levs[surveyCurrentLocale]))
									});
								})(mySection.pages[k]);
							}

							showMenu = new DropDownButton({label: '-', dropDown: menuPage});

							titlePageContainer.menus[mySection.id] = mySection.pagesMenu = showMenu;
						}

						if(myPage !== null) {
							$('#title-page-container').html('<h1>'+myPage.name+'</h1>').show();
						} else {
							$('#title-page-container').html('').hide();
						}
						setDisplayed(showMenu, true);
						setDisplayed(titlePageContainer, true); // will fix title later
					}
				}

				stdebug('Updating menus.. ecov = ' + effectiveCoverage());

				menuMap.setCheck(menuMap.section_general,  (surveyCurrentPage == '' && (surveyCurrentSpecial=='' || surveyCurrentSpecial==null)),false);

				// Update the status of the items in the Section menu
				for(var j in menuMap.sections) {
					var aSection = menuMap.sections[j];
					// need to see if any items are visible @ current coverage
					stdebug("for " + aSection.name + " minLev["+surveyCurrentLocale+"] = "+ aSection.minLev[surveyCurrentLocale]);
					menuMap.setCheck(aSection.menuItem,  (surveyCurrentSection == aSection.id),effectiveCoverage()<aSection.minLev[surveyCurrentLocale]);

					// update the items in that section's Page menu
					if(surveyCurrentSection == aSection.id) {
						for(var k in aSection.pages ) {
							var aPage = aSection.pages[k];
							if(!aPage.menuItem) {
								console.log("Odd - " + aPage.id + " has no menuItem");
							} else {
								menuMap.setCheck(aPage.menuItem,  (aPage.id == surveyCurrentPage),  (effectiveCoverage()<parseInt(aPage.levs[surveyCurrentLocale])));
							}
						}
					}
				}

				menuMap.setCheck(menuMap.forumMenu,  (surveyCurrentSpecial == 'forum'),(surveyUser	===null));
				resizeSidebar();
			}

			if(_thePages == null || _thePages.loc != surveyCurrentLocale ) {
				// show the raw IDs while loading.
				updateMenuTitles(null);

				if(surveyCurrentLocale!=null&&surveyCurrentLocale!='') {
					var needLocTable = false;

					var url = contextPath + "/SurveyAjax?_="+surveyCurrentLocale+"&s="+surveySessionId+"&what=menus&locmap="+needLocTable+cacheKill();
					myLoad(url, "menus", function(json) {
						if(!verifyJson(json, "menus")) {
							return; // busted?
						}

						if(json.locmap) {
							locmap = new LocaleMap(locmap); // overwrite with real data
						}

						// make this into a hashmap.
						if(json.canmodify) {
							var canmodify = {};
							for(var k in json.canmodify) {
								canmodify[json.canmodify[k]]=true;
							}
							window.canmodify = canmodify;
						}

						updateCovFromJson(json);
						updateCoverageMenuTitle();
						updateCoverage(flipper.get(pages.data)); // update CSS and auto menu title

						function unpackMenus(json) {
							var menus = json.menus;

							if(_thePages) {
								stdebug("Updating cov info into menus for " + json.loc);
								for(var k in menus.sections) {
									var oldSection = _thePages.sectionMap[menus.sections[k].id];
									for(var j in menus.sections[k].pages) {
										var oldPage = oldSection.pageMap[menus.sections[k].pages[j].id];

										// copy over levels
										oldPage.levs[json.loc] = menus.sections[k].pages[j].levs[json.loc];
									}
								}
							} else {
								stdebug("setting up new hashes for " + json.loc);
								// set up some hashes
								menus.haveLocs = {};
								menus.sectionMap = {};
								menus.pageToSection = {};
								for(var k in menus.sections) {
									menus.sectionMap[menus.sections[k].id] = menus.sections[k];
									menus.sections[k].pageMap = {};
									menus.sections[k].minLev = {};
									for(var j in menus.sections[k].pages) {
										menus.sections[k].pageMap[menus.sections[k].pages[j].id] = menus.sections[k].pages[j];
										menus.pageToSection[menus.sections[k].pages[j].id] = menus.sections[k];
									}
								}
								_thePages = menus;
							}

							stdebug("Calculating minimum section coverage for " + json.loc);
							for(var k in _thePages.sectionMap) {
								var min = 200;
								for(var j in _thePages.sectionMap[k].pageMap) {
									var thisLev = parseInt(_thePages.sectionMap[k].pageMap[j].levs[json.loc]);
									if(min > thisLev) {
										min = thisLev;
									}
								}
								_thePages.sectionMap[k].minLev[json.loc] = min;
							}

							_thePages.haveLocs[json.loc] = true;
						}

						unpackMenus(json);
						unpackMenuSideBar(json);
						updateMenus(_thePages);
					});
				}
			} else {
				// go ahead and update
				updateMenus(_thePages);
			}
		}

		window.insertLocaleSpecialNote = function insertLocaleSpecialNote(theDiv) {
			if(surveyBeta) {
				var theChunk = domConstruct.toDom(stui.sub("beta_msg", { info: bund, locale: surveyCurrentLocale, msg: msg}));
				var subDiv = document.createElement("div");
				subDiv.appendChild(theChunk);
				subDiv.className = 'warnText';
				theDiv.appendChild(subDiv);
			}

			var bund = locmap.getLocaleInfo(surveyCurrentLocale);

			if(bund) {
				if(bund.readonly) {
					var msg = null;
					if(bund.readonly_why) {
						msg = bund.readonly_why_raw;
					} else {
						msg = stui.str("readonly_unknown");
					}
					var asHtml = stui.sub("readonly_msg", { info: bund, locale: surveyCurrentLocale, msg: msg});
					asHtml = locmap.linkify(asHtml);
					var theChunk = domConstruct.toDom(asHtml);
					var subDiv = document.createElement("div");
					subDiv.appendChild(theChunk);
					subDiv.className = 'warnText';
					theDiv.appendChild(subDiv);
				} else if(bund.dcChild) {
					var theChunk = domConstruct.toDom(stui.sub("defaultContentChild_msg", { info: bund, locale: surveyCurrentLocale, dcChildName: locmap.getLocaleName(bund.dcChild)}));
					var subDiv = document.createElement("div");
					subDiv.appendChild(theChunk);
					subDiv.className = 'warnText';
					theDiv.appendChild(subDiv);
				}
			}
		};

		/**
		 * Show the "possible problems" section which has errors for the locale
		 * @method showPossibleProblems
		 */
		function showPossibleProblems(flipper,flipPage,loc, session, effectiveCov, requiredCov) {
			surveyCurrentLocale = loc;
			dojo.ready(function(){

				var url = contextPath + "/SurveyAjax?what=possibleProblems&_="+surveyCurrentLocale+"&s="+session+"&eff="+effectiveCov+"&req="+requiredCov+  cacheKill();
				myLoad(url, "possibleProblems", function(json) {
					if(verifyJson(json, 'possibleProblems')) {
						stdebug("json.possibleProblems OK..");
						if(json.dataLoadTime) {
							updateIf("dynload", json.dataLoadTime);
						}

						var theDiv = flipper.flipToEmpty(flipPage);

						insertLocaleSpecialNote(theDiv);

						if(json.possibleProblems.length > 0) {
							var subDiv = createChunk("","div");
							subDiv.className = "possibleProblems";

							var h3 = createChunk(stui_str("possibleProblems"), "h3");
							subDiv.appendChild(h3);

							var div3 = document.createElement("div");
							var newHtml = "";
							newHtml += testsToHtml(json.possibleProblems);
							div3.innerHTML = newHtml;
							subDiv.appendChild(div3);
							theDiv.appendChild(subDiv);
						}
						var theInfo = createChunk("","p","special_general");
						theDiv.appendChild(theInfo);
						theInfo.innerHTML = stui_str("special_general"); // TODO replace with … ?
						hideLoader(null);
					}
				});
			});
		}

		var isLoading = false;

		/**
		 * This is the main entrypoint to the 'new' view system, based in /v.jsp
		 * @method reloadV
		 */
		window.reloadV = function reloadV() {
			if(disconnected) {
				unbust();
			}

			document.getElementById('DynamicDataSection').innerHTML = '';//reset the data
			$('#nav-page').hide();
			isLoading = false;
			showers[flipper.get(pages.data).id]=function(){ console.log("reloadV()'s shower - ignoring reload request, we are in the middle of a load!"); };

			// assume parseHash was already called, if we are taking input from the hash
			ariDialog.hide();

			updateHashAndMenus(true);

			if(surveyCurrentLocale!=null && surveyCurrentLocale!=''&&surveyCurrentLocale!='-'){
				var bund = locmap.getLocaleInfo(surveyCurrentLocale);
				if(bund!==null && bund.dcParent) {
					var theChunk = domConstruct.toDom(stui.sub("defaultContent_msg", { info: bund, locale: surveyCurrentLocale, dcParentName: locmap.getLocaleName(bund.dcParent)}));
					var theDiv = document.createElement("div");
					theDiv.appendChild(theChunk);
					theDiv.className = 'ferrbox';
					flipper.flipTo(pages.other, theDiv);
					return;
				}
			}

			// todo dont even flip if it's quick.
			var loadingChunk;
			flipper.flipTo(pages.loading, loadingChunk = createChunk(stui_str("loading"), "i", "loadingMsg"));

			var itemLoadInfo = createChunk("","div","itemLoadInfo");

			// Create a little spinner to spin "..." so the user knows we are doing something..
			var spinChunk = createChunk("...","i","loadingMsgSpin");
			var spin = 0;
			var timerToKill = window.setInterval(function() {
				 var spinTxt = '';
				 spin++;
				 switch(spin%3) {
					 case 0: spinTxt = '.  '; break;
					 case 1: spinTxt = ' . '; break;
					 case 2: spinTxt = '  .'; break;
				 }
				 removeAllChildNodes(spinChunk);
				 spinChunk.appendChild(document.createTextNode(spinTxt));
			}, 1000);

			// Add the "..." until the Flipper flips
			flipper.addUntilFlipped(function() {
				var frag = document.createDocumentFragment();
				frag.appendChild(spinChunk);
				return frag;
			}, function() {
				window.clearInterval(timerToKill);
			});

			// now, load. Use a show-er function for indirection.
			var shower = function() {
				if(isLoading) {
					console.log("reloadV inner shower: already isLoading, exitting.");
					return;
				}
				isLoading = true;
				var theDiv = flipper.get(pages.data);
				var theTable = theDiv.theTable;

				if(!theTable) {
					var theTableList = theDiv.getElementsByTagName("table");
					if(theTableList) {
						theTable = theTableList[0];
						theDiv.theTable = theTable;
					}
				}

				showLoader(null, theDiv.stui.loading);

				if((surveyCurrentSpecial == null||surveyCurrentSpecial=='') && surveyCurrentLocale!=null && surveyCurrentLocale!='') {
					if((surveyCurrentPage==null || surveyCurrentPage=='') && (surveyCurrentId==null||surveyCurrentId=='')) {
						// the 'General Info' page.
						itemLoadInfo.appendChild(document.createTextNode(locmap.getLocaleName(surveyCurrentLocale)));
						showPossibleProblems(flipper, pages.other, surveyCurrentLocale, surveySessionId, covName(effectiveCoverage()), covName(effectiveCoverage()));
						showInPop2(stui.str("generalPageInitialGuidance"), null, null, null, true); /* show the box the first time */
						isLoading=false;
					} else if(surveyCurrentId=='!') {
						var frag = document.createDocumentFragment();
						frag.appendChild(createChunk(stui.str('section_help'),"p", "helpContent"));
						var infoHtml = stui.str('section_info_'+surveyCurrentPage);
						var infoChunk  = document.createElement("div");
						infoChunk.innerHTML = infoHtml;
						frag.appendChild(infoChunk);
						flipper.flipTo(pages.other, frag);
						hideLoader(null);
						isLoading=false;

					} else {
						// (common case) this is an actual locale data page.
						itemLoadInfo.appendChild(document.createTextNode(locmap.getLocaleName(surveyCurrentLocale) + '/' + surveyCurrentPage + '/' + surveyCurrentId));
						var url = contextPath + "/RefreshRow.jsp?json=t&_="+surveyCurrentLocale+"&s="+surveySessionId+"&x="+surveyCurrentPage+"&strid="+surveyCurrentId+cacheKill();
						$('#nav-page').show();
						myLoad(url, "section", function(json) {
							isLoading=false;
							showLoader(theDiv.loader,stui.loading2);
							if(!verifyJson(json, 'section')) {
								return;
							} else if(json.section.nocontent) {
								surveyCurrentSection = '';
								if(json.pageId) {
									surveyCurrentPage = json.pageId;
								} else {
									surveyCurrentPage= '';
								}
								showLoader(null);
								updateHashAndMenus(); // find out why there's no content. (locmap)
							} else if(!json.section.rows) {
								console.log("!json.section.rows");
								showLoader(theDiv.loader,"Error while  loading: <br><div style='border: 1px solid red;'>" + "no rows" + "</div>");
								handleDisconnect("while loading- no rows",json);
							} else {
								stdebug("json.section.rows OK..");
								showLoader(theDiv.loader, "loading..");
								if(json.dataLoadTime) {
									updateIf("dynload", json.dataLoadTime);
								}

								surveyCurrentSection = '';
								surveyCurrentPage = json.pageId;
								updateHashAndMenus(); // now that we have a pageid
								if(!surveyUser) {
									showInPop2(stui.str("loginGuidance"), null, null, null, true); /* show the box the first time */
								} else if(!json.canModify) {
									showInPop2(stui.str("readonlyGuidance"), null, null, null, true); /* show the box the first time */
								} else {
									showInPop2(stui.str("dataPageInitialGuidance"), null, null, null, true); /* show the box the first time */
								}
								doUpdate(theDiv.id, function() {
									showLoader(theDiv.loader,stui.loading3);
									insertRows(theDiv,json.pageId,surveySessionId,json); // pageid is the xpath..
									updateCoverage(flipper.get(pages.data)); // make sure cov is set right before we show.
									flipper.flipTo(pages.data); // TODO now? or later?
									window.showCurrentId(); // already calls scroll
									//refresh counter and add navigation at bottom
									refreshCounterVetting();
									$('.vetting-page').after($('#nav-page .nav-button').clone());
								});
							}
						});
					}
				} else if(surveyCurrentSpecial =='oldvotes') {
					var url = contextPath + "/SurveyAjax?what=oldvotes&_="+surveyCurrentLocale+"&s="+surveySessionId+"&"+cacheKill();
					myLoad(url, "(loading oldvotes " + surveyCurrentLocale + ")", function(json) {
						isLoading=false;
						showLoader(null,stui.loading2);
						if(!verifyJson(json, 'oldvotes')) {
							return;
						} else {
							showLoader(null, "loading..");
							if(json.dataLoadTime) {
								updateIf("dynload", json.dataLoadTime);
							}

							var theDiv = flipper.flipToEmpty(pages.other); // clean slate, and proceed..

							removeAllChildNodes(theDiv);

							// changed h2txt, v_oldvotes_title per https://unicode.org/cldr/trac/ticket/11135
							// TODO: simplify if votesafter, newVersion no longer used
							// var h2var = {votesafter:json.oldvotes.votesafter, newVersion:json.status.newVersion};
							// var h2txt = stui.sub("v_oldvotes_title",h2var);
							var h2txt = stui.str("v_oldvotes_title");
							theDiv.appendChild(createChunk(h2txt, "h2", "v-title"));

							if(!json.oldvotes.locale) {
								surveyCurrentLocale='';
								updateHashAndMenus();

								var ul = document.createElement("div");
								ul.className = "oldvotes_list";
								var data = json.oldvotes.locales.data;
								var header = json.oldvotes.locales.header;

								if(data.length > 0) {
									data.sort((a, b) => a[header.LOCALE].localeCompare(b[header.LOCALE]));
									for(var k in data) {
										var li = document.createElement("li");

										var link = createChunk(data[k][header.LOCALE_NAME],"a");
										link.href = "#"+data[k][header.LOCALE];
										(function(loc,link) {
											return (function() {
												var clicky;
											listenFor(link, "click", clicky = function(e) {
												surveyCurrentLocale = loc;
												reloadV();
												stStopPropagation(e);
												return false;
											});
											link.onclick = clicky;
											}); })(data[k][header.LOCALE],link)();
										li.appendChild(link);
										li.appendChild(createChunk(" "));
										li.appendChild(createChunk("("+data[k][header.COUNT]+")"));

										ul.appendChild(li);
									}

									theDiv.appendChild(ul);

									theDiv.appendChild(createChunk(stui.str("v_oldvotes_locale_list_help_msg"), "p", "helpContent"));
								} else {
									theDiv.appendChild(createChunk(stui.str("v_oldvotes_no_old"),"i")); // TODO fix
								}
							} else {
								surveyCurrentLocale=json.oldvotes.locale;
								updateHashAndMenus();
								var loclink;
								theDiv.appendChild(loclink=createChunk(stui.str("v_oldvotes_return_to_locale_list"),"a", "notselected"));
								listenFor(loclink, "click", function(e) {
									surveyCurrentLocale='';
									reloadV();
									stStopPropagation(e);
									return false;
								});
								theDiv.appendChild(createChunk(json.oldvotes.localeDisplayName, "h3","v-title2"));
								var oldVotesLocaleMsg = document.createElement("p");
								oldVotesLocaleMsg.className = "helpContent";
								var ovLocMsg = stui.sub("v_oldvotes_locale_msg", {version: surveyLastVoteVersion, locale: json.oldvotes.localeDisplayName});
								if (!json.oldvotes.uncontested || json.oldvotes.uncontested.length == 0) {
									ovLocMsg = stui.sub("v_oldvotes_winning_already_imported", {version: surveyLastVoteVersion}) + " " + ovLocMsg;
								}
								oldVotesLocaleMsg.innerHTML = ovLocMsg;
								theDiv.appendChild(oldVotesLocaleMsg);
								if ((json.oldvotes.contested && json.oldvotes.contested.length > 0) || (json.oldvotes.uncontested && json.oldvotes.uncontested.length > 0)) {

									function showVoteTable(voteList, type) {
										var t = document.createElement("table");
										t.id = 'oldVotesAcceptList';
										var th = document.createElement("thead");
										var tb = document.createElement("tbody");

										var tr = document.createElement("tr");
										tr.appendChild(createChunk(stui.str("v_oldvotes_path"),"th","code"));
										tr.appendChild(createChunk(json.BASELINE_LANGUAGE_NAME,"th","v-comp"));
										tr.appendChild(createChunk(stui.sub("v_oldvotes_winning_msg", {version: surveyLastVoteVersion}),"th","v-win"));
										tr.appendChild(createChunk(stui.str("v_oldvotes_mine"),"th","v-mine"));

										var accept;
										tr.appendChild(accept=createChunk(stui.str("v_oldvotes_accept"),"th","v-accept"));
										th.appendChild(tr);
										t.appendChild(th);
										var oldPath = '';
										var oldSplit = [];
										for(var k in voteList) {
											var row = voteList[k];
											var tr = document.createElement("tr");
											var tdp;
											var rowTitle = '';

											// delete common substring
											var pathSplit = row.pathHeader.split('	');
											for(var nn in pathSplit) {
												if(pathSplit[nn] != oldSplit[nn]) {
													break;
												}
											}
											if(nn != pathSplit.length-1) {
												// need a header row.
												var trh = document.createElement('tr');
												trh.className='subheading';
												var tdh = document.createElement('th');
												tdh.colSpan = 5;
												for(var nn in pathSplit) {
													if(nn < pathSplit.length-1) {
														tdh.appendChild(createChunk(pathSplit[nn],"span","pathChunk"));
													}
												}
												trh.appendChild(tdh);
												tb.appendChild(trh);
											}
											oldSplit = pathSplit;
											rowTitle = pathSplit[pathSplit.length - 1];

											tdp = createChunk("","td","v-path");

											var dtpl = createChunk(rowTitle, "a");
											dtpl.href = "v#/"+surveyCurrentLocale+"//"+row.strid;
											dtpl.target='_CLDR_ST_view';
											tdp.appendChild(dtpl);

											tr.appendChild(tdp);
											var td00 = createChunk(row.baseValue,"td","v-comp"); // english
											tr.appendChild(td00);
											var td0 = createChunk("","td","v-win");
											if(row.winValue) {
												var span0 = appendItem(td0, row.winValue, "winner");
												span0.dir = json.oldvotes.dir;
											}
											tr.appendChild(td0);
											var td1 = createChunk("","td","v-mine");
											var label  = createChunk("","label","");
											var span1 = appendItem(label, row.myValue, "value");
											td1.appendChild(label);
											span1.dir = json.oldvotes.dir;
											tr.appendChild(td1);
											var td2 = createChunk("","td","v-accept");
											var box = createChunk("","input","");
											box.type="checkbox";
											if(type=='uncontested') { // uncontested true by default
												box.checked=true;
											}
											row.box = box; // backlink
											td2.appendChild(box);
											tr.appendChild(td2);

											(function(tr,box,tdp){return function(){
                                                // allow click anywhere
											    listenFor(tr, "click", function(e) {
													box.checked = !box.checked;
													stStopPropagation(e);
													return false;
												});
                                                // .. but not on the path.  Also listem to the box and do nothing
												listenFor([tdp,box], "click", function(e) {
													stStopPropagation(e);
													return false;
												});
											};})(tr,box,tdp)();

											tb.appendChild(tr);
										}
										t.appendChild(tb);

										t.appendChild(createLinkToFn("v_oldvotes_none", function() {
											for(var k in json.oldvotes[type]) {
												var row = json.oldvotes[type][k];
												row.box.checked = false;
											}
										}, "button"));
										return t;
									}

									var frag = document.createDocumentFragment();

									const oldVoteCount = (json.oldvotes.contested ? json.oldvotes.contested.length : 0) +
									                     (json.oldvotes.uncontested ? json.oldvotes.uncontested.length : 0);
									var summaryMsg = stui.sub("v_oldvotes_count_msg", {count: oldVoteCount});
									frag.appendChild(createChunk(summaryMsg, "div", ""));

									var navChunk = document.createElement("div");
									navChunk.className = 'v-oldVotes-nav';
									frag.appendChild(navChunk);

									var uncontestedChunk = null;
									var contestedChunk = null;

									function addOldvotesType(type, jsondata, frag, navChunk) {
										var content = createChunk("", "div", "v-oldVotes-subDiv");
										content.strid = "v_oldvotes_title_" + type; // v_oldvotes_title_contested or v_oldvotes_title_uncontested

										/* Normally this interface is for old "losing" (contested) votes only, since old "winning" (uncontested) votes
										 * are imported automatically. An exception is for TC users, for whom auto-import is disabled. The server-side
										 * code leaves json.oldvotes.uncontested undefined except for TC users.
										 * Show headings for "Winning/Losing" only if json.oldvotes.uncontested is defined and non-empty.
										 */
										if ((json.oldvotes.uncontested && json.oldvotes.uncontested.length > 0)) {
											var title = stui.str(content.strid);
											content.title = title;
											content.appendChild(createChunk(title,"h2","v-oldvotes-sub"));
										}

										content.appendChild(showVoteTable(jsondata, type));

										var submit = BusyButton({
											label: stui.str("v_submit_msg"),
											busyLabel: stui.str("v_submit_busy")
										});

										submit.on("click",function(e) {
											setDisplayed(navChunk, false);
											var confirmList= []; // these will be revoted with current params
											var deleteList = []; // these will be deleted

											// explicit confirm list -  save us desync hassle
											for(var kk in jsondata ) {
												if(jsondata[kk].box.checked) {
													confirmList.push(jsondata[kk].strid);
												}
											}

											var saveList = {
													locale: surveyCurrentLocale,
													confirmList: confirmList,
													deleteList: deleteList
											};

											console.log(saveList.toString());
											console.log("Submitting " + type + " " +  confirmList.length + " for confirm and " + deleteList.length + " for deletion");

											var url = contextPath + "/SurveyAjax?what=oldvotes&_="+surveyCurrentLocale+"&s="+surveySessionId+"&doSubmit=true&"+cacheKill();
											myLoad(url, "(submitting oldvotes " + surveyCurrentLocale + ")", function(json) {
												showLoader(theDiv.loader,stui.loading2);
												if(!verifyJson(json, 'oldvotes')) {
													handleDisconnect("Error submitting votes!", json, "Error");
													return;
												} else {
													reloadV();
												}
											},  JSON.stringify(saveList), { "Content-Type": "application/json"} );
										});

										submit.placeAt(content);
										// hide by default
										setDisplayed(content, false);

										frag.appendChild(content);
										return content;
									}

									if (json.oldvotes.uncontested && json.oldvotes.uncontested.length > 0){
										uncontestedChunk = addOldvotesType("uncontested",json.oldvotes.uncontested, frag, navChunk);
									}
									if (json.oldvotes.contested && json.oldvotes.contested.length > 0){
										contestedChunk = addOldvotesType("contested",json.oldvotes.contested, frag, navChunk);
									}

									if(contestedChunk==null && uncontestedChunk != null) {
										setDisplayed(uncontestedChunk, true); // only item
									} else if(contestedChunk!=null && uncontestedChunk == null) {
										setDisplayed(contestedChunk, true); // only item
									} else {
										// navigation
										navChunk.appendChild(createChunk(stui.str('v_oldvotes_show')));
										navChunk.appendChild(createLinkToFn(uncontestedChunk.strid, function() {
											setDisplayed(contestedChunk, false);
											setDisplayed(uncontestedChunk, true);
										}, 'button'));
										navChunk.appendChild(createLinkToFn(contestedChunk.strid, function() {
											setDisplayed(contestedChunk, true);
											setDisplayed(uncontestedChunk, false);
										}, 'button'));

										contestedChunk.appendChild(createLinkToFn("v_oldvotes_hide", function() {
											setDisplayed(contestedChunk, false);
										}, 'button'));
										uncontestedChunk.appendChild(createLinkToFn("v_oldvotes_hide", function() {
											setDisplayed(uncontestedChunk, false);
										}, 'button'));

									}

									theDiv.appendChild(frag);
								} else {
									theDiv.appendChild(createChunk(stui.str("v_oldvotes_no_old_here"),"i",""));
								}
							}
						}
						hideLoader(null);
					});
				} else if(surveyCurrentSpecial == 'mail') {
					var url = contextPath + "/SurveyAjax?what=mail&s="+surveySessionId+"&fetchAll=true&"+cacheKill();
					myLoad(url, "(loading mail " + surveyCurrentLocale + ")", function(json) {
						hideLoader(null,stui.loading2);
						isLoading=false;
						if(!verifyJson(json, 'mail')) {
							return;
						} else {
							if(json.dataLoadTime) {
								updateIf("dynload", json.dataLoadTime);
							}

							var theDiv = flipper.flipToEmpty(pages.other); // clean slate, and proceed..

							removeAllChildNodes(theDiv);

							var listDiv = createChunk("","div","mailListChunk");
							var contentDiv = createChunk("","div","mailContentChunk");

							theDiv.appendChild(listDiv);
							theDiv.appendChild(contentDiv);

							setDisplayed(contentDiv,false);
							var header = json.mail.header;
							var data = json.mail.data;

							if(data.length == 0) {
								listDiv.appendChild(createChunk(stui.str("mail_noMail"),"p","helpContent"));
							} else {
								for(var ii in data) {
									var row = data[ii];
									var li = createChunk(row[header.QUEUE_DATE] + ": " + row[header.SUBJECT], "li", "mailRow");
									if(row[header.READ_DATE]) {
										addClass(li,"readMail");
									}
									if(header.USER !== undefined) {
										li.appendChild(document.createTextNode("(to "+row[header.USER]+")"));
									}
									if(row[header.SENT_DATE] !== false) {
										li.appendChild(createChunk("(sent)", "span", "winner"));
									} else  if(row[header.TRY_COUNT]>=3) {
										li.appendChild(createChunk("(try#"+row[header.TRY_COUNT]+")", "span", "loser"));
									} else  if(row[header.TRY_COUNT]> 0) {
										li.appendChild(createChunk("(try#"+row[header.TRY_COUNT]+")", "span", "warning"));
									}
									listDiv.appendChild(li);

									li.onclick = (function(li,row,header) {
										return function() {
										 	  if(!row[header.READ_DATE])
												myLoad(contextPath + "/SurveyAjax?what=mail&s="+surveySessionId+"&markRead="+row[header.ID]+"&"+cacheKill(), 'Marking mail read', function(json) {
													if(!verifyJson(json, 'mail')) {
														return;
													} else {
														addClass(li, "readMail"); // mark as read when server answers
														row[header.READ_DATE]=true; // close enough
													}
												});

										 	  setDisplayed(contentDiv, false);

										 	  removeAllChildNodes(contentDiv);

										 	  contentDiv.appendChild(createChunk("Date: " + row[header.QUEUE_DATE], "h2", "mailHeader"));
										 	  contentDiv.appendChild(createChunk("Subject: " + row[header.SUBJECT], "h2", "mailHeader"));
										 	  contentDiv.appendChild(createChunk("Message-ID: " + row[header.ID], "h2", "mailHeader"));
										 	  if(header.USER !== undefined) {
											 	  contentDiv.appendChild(createChunk("To: " + row[header.USER], "h2", "mailHeader"));
											  }
										 	  contentDiv.appendChild(createChunk(row[header.TEXT], "p", "mailContent"));

										 	  setDisplayed(contentDiv, true);
											};
										})(li, row, header);
								}
							}

						}
					});
				} else if(isReport(surveyCurrentSpecial)) {
					showLoader(theDiv.loader);
					showInPop2(stui.str("reportGuidance"), null, null, null, true, true); /* show the box the first time */
					require([
					         "dojo/ready",
					         "dojo/dom",
					         "dojo/dom-construct",
					         "dojo/request",
					         "dojo/number",
					         "dojo/domReady!"
					         ],
					         // HANDLES
					         function(
					        		 ready,
					        		 dom,
					        		 dcons,
					        		 request,
					        		 dojoNumber
					        ) { ready(function(){

								var url = contextPath + "/EmbeddedReport.jsp?x="+surveyCurrentSpecial+"&_="+surveyCurrentLocale+"&s="+surveySessionId+cacheKill();
								var errFunction = function errFunction(err) {
									console.log("Error: loading " + url + " -> " + err);
									hideLoader(null,stui.loading2);
									isLoading=false;
									flipper.flipTo(pages.other, domConstruct.toDom("<div style='padding-top: 4em; font-size: x-large !important;' class='ferrorbox warning'><span class='icon i-stop'> &nbsp; &nbsp;</span>Error: could not load: " + err + "</div>"));
								};
								if(isDashboard()) {
									if(!isVisitor) {
										request
						    			.get(url, {handleAs: 'json'})
						    			.then(function(json) {
											hideLoader(null,stui.loading2);
											isLoading=false;
											// further errors are handled in JSON
											showReviewPage(json, function() {
												// show function - flip to the 'other' page.
												flipper.flipTo(pages.other, null);
											});
										})
										.otherwise(errFunction);
									}
									else {
										alert('Please login to access Dashboard');
										surveyCurrentSpecial = '';
										surveyCurrentLocale = '';
										reloadV();
									}
								}
								else {
									hideLoader(null,stui.loading2);

									request
					    			.get(url, {handleAs: 'html'})
					    			.then(function(html) {
					    				// errors are handled as HTML.
										hideLoader(null,stui.loading2);
										isLoading=false;
										flipper.flipTo(pages.other, domConstruct.toDom(html));
									})
									.otherwise(errFunction);
								}

					        });
					 });
				} else if(surveyCurrentSpecial == 'none') {
					// for now - redirect
					hideLoader(null);
					isLoading=false;
					window.location = survURL; // redirect home
				} else if(surveyCurrentSpecial == 'locales') {
					hideLoader(null);
					isLoading=false;
					var theDiv = document.createElement("div");
					theDiv.className = 'localeList';

					var addSubLocale = function addSubLocale(parLocDiv, subLoc) {
						var subLocInfo = locmap.getLocaleInfo(subLoc);
						var subLocDiv = createChunk(null, "div", "subLocale");
						appendLocaleLink(subLocDiv, subLoc, subLocInfo);

						parLocDiv.appendChild(subLocDiv);
					};

					var addSubLocales = function addSubLocales(parLocDiv, subLocInfo) {
						if(subLocInfo.sub) {
							for(var n in subLocInfo.sub) {
								var subLoc = subLocInfo.sub[n];
								addSubLocale(parLocDiv, subLoc);
							}
						}
					};

					var addTopLocale = function addTopLocale(topLoc) {
						var topLocInfo = locmap.getLocaleInfo(topLoc);

						var topLocRow = document.createElement("div");
						topLocRow.className="topLocaleRow";

						var topLocDiv = document.createElement("div");
						topLocDiv.className="topLocale";
						appendLocaleLink(topLocDiv, topLoc, topLocInfo);

						var topLocList = document.createElement("div");
						topLocList.className="subLocaleList";

						addSubLocales(topLocList, topLocInfo);

						topLocRow.appendChild(topLocDiv);
						topLocRow.appendChild(topLocList);
						theDiv.appendChild(topLocRow);
					};

					addTopLocale("root");
					// top locales
					for(var n in locmap.locmap.topLocales) {
						var topLoc = locmap.locmap.topLocales[n];
						addTopLocale(topLoc);
					}
					flipper.flipTo(pages.other,null);
				    filterAllLocale();//filter for init data
					forceSidebar();
					surveyCurrentLocale=null;
					surveyCurrentSpecial='locales';
					showInPop2(stui.str("localesInitialGuidance"), null, null, null, true); /* show the box the first time */
					$('#itemInfo').html('');
				} else {
					otherSpecial.show(surveyCurrentSpecial, {flipper: flipper, pages: pages});
				}
			}; // end shower

			shower(); // first load

			// set up the "show-er" function so that if this locale gets reloaded, the page will load again - execept for the dashboard, where only the row get updated
			if(!isDashboard()) {
				showers[flipper.get(pages.data).id]=shower;
			}
		};  // end reloadV

		function trimNull(x) {
			if(x==null) {
				return '';
			}
			try {
				x = x.toString().trim();
			} catch(e) {
				// do nothing
			}
			return x;
		}

		ready(function(){
			window.parseHash(dojoHash()); // get the initial settings
			// load the menus - first.

			var theLocale = surveyCurrentLocale;
			if(surveyCurrentLocale===null || surveyCurrentLocale=='') {
				theLocale = 'und';
			}
			var xurl = contextPath + "/SurveyAjax?_="+theLocale+"&s="+surveySessionId+"&what=menus&locmap="+true+cacheKill();
			myLoad(xurl, "initial menus for " + surveyCurrentLocale, function(json) {
				if(!verifyJson(json,'locmap')) {
					return;
				} else {
					locmap = new LocaleMap(json.locmap);

					// make this into a hashmap.
					if(json.canmodify) {
						var canmodify = {};
						for(var k in json.canmodify) {
							canmodify[json.canmodify[k]]=true;
						}
						window.canmodify = canmodify;
					}

					//update left sidebar with locale data
					var theDiv = document.createElement("div");
					theDiv.className = 'localeList';

					var addSubLocale;

					addSubLocale = function addSubLocale(parLocDiv, subLoc) {
						var subLocInfo = locmap.getLocaleInfo(subLoc);
						var subLocDiv = createChunk(null, "div", "subLocale");
						appendLocaleLink(subLocDiv, subLoc, subLocInfo);

						parLocDiv.appendChild(subLocDiv);
					};

					var addSubLocales = function addSubLocales(parLocDiv, subLocInfo) {
						if(subLocInfo.sub) {
							for(var n in subLocInfo.sub) {
								var subLoc = subLocInfo.sub[n];
								addSubLocale(parLocDiv, subLoc);
							}
						}
					};

					var addTopLocale = function addTopLocale(topLoc) {
						var topLocInfo = locmap.getLocaleInfo(topLoc);

						var topLocRow = document.createElement("div");
						topLocRow.className="topLocaleRow";

						var topLocDiv = document.createElement("div");
						topLocDiv.className="topLocale";
						appendLocaleLink(topLocDiv, topLoc, topLocInfo);

						var topLocList = document.createElement("div");
						topLocList.className="subLocaleList";

						addSubLocales(topLocList, topLocInfo);

						topLocRow.appendChild(topLocDiv);
						topLocRow.appendChild(topLocList);
						theDiv.appendChild(topLocRow);
					};


					addTopLocale("root");
					// top locales
					for(var n in locmap.locmap.topLocales) {
						var topLoc = locmap.locmap.topLocales[n];
						addTopLocale(topLoc);
					}
					$('#locale-list').html(theDiv.innerHTML);

					if(isVisitor)
						$('#show-read').prop('checked', true);
					//tooltip locale
					$('a.locName').tooltip();

					filterAllLocale();
					//end of adding the locale data

					if (json.autoImportedOldWinningVotes) {
						var vals = { count: dojoNumber.format(json.autoImportedOldWinningVotes) };
						var autoImportedDialog = new Dialog({
							title: stui.sub("v_oldvote_auto_msg", vals),
							content: stui.sub("v_oldvote_auto_desc_msg", vals)
						});
						autoImportedDialog.addChild(new Button({
							label: "OK",
							onClick: function() {
								window.haveDialog = false;
								autoImportedDialog.hide();
								reloadV();
							}
						}));
						autoImportedDialog.show();
						window.haveDialog = true;
						hideOverlayAndSidebar();
					}

					updateCovFromJson(json);
					// setup coverage level
					window.surveyLevels = json.menus.levels;

					var titleCoverage = dojo.byId("title-coverage"); // coverage label

					var levelNums = [];  // numeric levels
					for(var k in window.surveyLevels) {
						levelNums.push( { num: parseInt(window.surveyLevels[k].level), level: window.surveyLevels[k] } );
					}
					levelNums.sort(function(a,b){return a.num-b.num;});

					var store = [];

					store.push({
							label: 'Auto',
							value: 'auto',
							title: stui.str('coverage_auto_desc')
						});

					store.push({
						type: "separator"
					});

					for(var j in levelNums) { // use given order
						if(levelNums[j].num==0) continue; // none - skip
						if(levelNums[j].num < covValue('minimal')) continue; // don't bother showing these
						if(window.surveyOfficial && levelNums[j].num==101) continue; // hide Optional in production
						var level = levelNums[j].level;
						store.push({
								label: stui.str('coverage_'+ level.name),
								value: level.name,
								title: stui.str('coverage_'+ level.name + '_desc')
						});
					}
					//coverage menu
					var patternCoverage = $('#title-coverage .dropdown-menu');
				    if(store[0].value) {
					    $('#coverage-info').text(store[0].label);
				    }
					for (var index = 0; index < store.length; ++index) {
					    var data = store[index];
					    if(data.value) {
						    var html = '<li><a class="coverage-list" data-value="'+data.value+'"href="#">'+data.label+'</a></li>';
						    patternCoverage.append(html);
					    }
					}
					patternCoverage.find('li a').click(function(event){
						event.stopPropagation();
						event.preventDefault();
						var newValue = $(this).data('value');
						var setUserCovTo = null;
						if(newValue == 'auto') {
							setUserCovTo = null; // auto
						} else {
							setUserCovTo = newValue;
						}
						if(setUserCovTo === window.surveyUserCov) {
							console.log('No change in user cov: ' + setUserCovTo);
						} else {
							window.surveyUserCov = setUserCovTo;
							var updurl  = contextPath + "/SurveyAjax?_="+theLocale+"&s="+surveySessionId+"&what=pref&pref=p_covlev&_v="+window.surveyUserCov+cacheKill(); // SurveyMain.PREF_COVLEV
							myLoad(updurl, "updating covlev to  " + surveyUserCov, function(json) {
								if(!verifyJson(json,'pref')) {
									return;
								} else {
									unpackMenuSideBar(json);
									if(surveyCurrentSpecial && isReport(surveyCurrentSpecial))
										reloadV();
									console.log('Server set  covlev successfully.');
								}
							});
						}
						// still update these.
						updateCoverage(flipper.get(pages.data)); // update CSS and 'auto' menu title
						updateHashAndMenus(false); // TODO: why? Maybe to show an item?
						$('#coverage-info').text(newValue.ucFirst());
						$(this).parents('.dropdown-menu').dropdown('toggle');
						if(!isDashboard())
							refreshCounterVetting();
						return false;
					});
					// TODO have to move this out of the DOM..

				window.reloadV(); // call it

				// watch for hashchange to make other changes..
				dojoTopic.subscribe("/dojo/hashchange", function(changedHash){
					var oldLocale = trimNull(surveyCurrentLocale);
					var oldSpecial = trimNull(surveyCurrentSpecial);
					var oldPage = trimNull(surveyCurrentPage);
					var oldId = trimNull(surveyCurrentId);

					window.parseHash(changedHash);

					surveyCurrentId = trimNull(surveyCurrentId);

					// did anything change?
					if(oldLocale!=trimNull(surveyCurrentLocale) ||
							oldSpecial!=trimNull(surveyCurrentSpecial) ||
							oldPage != trimNull(surveyCurrentPage) ) {
						console.log("# hash changed, (loc, etc) reloadingV..");
						reloadV();
					} else if(oldId != surveyCurrentId && surveyCurrentId != '') {
						console.log("# just ID changed, to " + surveyCurrentId);
					    // surveyCurrentID and the hash have already changed.
					    // just call showInPop if the item is present. If not present, make sure it's visible.
						window.showCurrentId();
					}
				});
				}
			});
			});
		}); // end stui  load
	});  // end require()
} // end showV

/**
 * reload a specific row
 * @method refreshRow2
 */
function refreshRow2(tr,theRow,vHash,onSuccess, onFailure) {
	showLoader(tr.theTable.theDiv.loader,stui.loadingOneRow);
	// vHash not used.
    var ourUrl = contextPath + "/RefreshRow.jsp?what="+WHAT_GETROW+"&xpath="+theRow.xpid +"&_="+surveyCurrentLocale+"&fhash="+tr.rowHash+/*"&vhash="+vHash+*/"&s="+tr.theTable.session +"&json=t&automatic=t";

    if(isDashboard()) {
    	ourUrl += "&dashboard=true";
    }

    var loadHandler = function(json){
        try {
	    		if(json&&json.dataLoadTime) {
	    			//updateIf("dynload", json.dataLoadTime);
	    		}
        		if(json.section.rows[tr.rowHash]) {
        			theRow = json.section.rows[tr.rowHash];
        			tr.theTable.json.section.rows[tr.rowHash] = theRow;
        			updateRow(tr, theRow);

        			//style the radios
        			//wrapRadios(tr);
        			hideLoader(tr.theTable.theDiv.loader);
        			onSuccess(theRow);
        			if(isDashboard()) {
        				refreshFixPanel(json);
        			}
        			else {
        				refreshCounterVetting();
        			}
        		} else {
        	        tr.className = "ferrbox";
        	        console.log("could not find " + tr.rowHash + " in " + json);
        	        onFailure("refreshRow2: Could not refresh this single row: Server failed to return xpath #"+theRow.xpid+" for locale "+surveyCurrentLocale);
        		}
           }catch(e) {
               console.log("Error in ajax post [refreshRow2] ",e.message);
           }
    };
    var errorHandler = function(err, ioArgs){
    	console.log('Error: ' + err + ' response ' + ioArgs.xhr.responseText);
        tr.className = "ferrbox";
        tr.innerHTML="Error while  loading: "+err.name + " <br> " + err.message + "<div style='border: 1px solid red;'>" + ioArgs.xhr.responseText + "</div>";
        onFailure("err",err,ioArgs);
    };
    var xhrArgs = {
            url: ourUrl+cacheKill(),
            handleAs:"json",
            load: loadHandler,
            error: errorHandler,
            timeout: ajaxTimeout
        };
    queueXhr(xhrArgs);
}

/**
* bottleneck for voting buttons
 * @method handleWiredClick
 */
function handleWiredClick(tr,theRow,vHash,box,button,what) {
	var value="";
	var valToShow;
	if(tr.wait) {
		return;
	}
	if(box) {
		valToShow=box.value;
		value = box.value;
		if(value.length ==0 ) {
			if(box.focus) {
				box.focus();
				myUnDefer();
			}
			return; // nothing entered.
		}
	} else {
		valToShow=button.value;
	}
	if(!what) {
		what='submit';
	}
	if(what=='submit') {
		button.className="ichoice-x-ok";  // TODO: ichoice-inprogress?  spinner?
		showLoader(tr.theTable.theDiv.loader,stui.voting);
	} else {
		showLoader(tr.theTable.theDiv.loader, stui.checking);
	}

	// select
	updateCurrentId(theRow.xpstrid);

	// and scroll
	showCurrentId();

	if(tr.myProposal) {
		// move these 2 up if needed
		var children = getTagChildren(tr);
		var config = tr.theTable.config;

		children[config.othercell].removeChild(tr.myProposal);
		tr.myProposal = null; // mark any pending proposal as invalid.
	}

	var myUnDefer = function() {
		tr.wait=false;
		setDefer(false);
	};
	tr.wait=true;
	resetPop(tr);
	setDefer(true);
	theRow.proposedResults = null;

	console.log("Vote for " + tr.rowHash + " v='"+vHash+"', value='"+value+"'");
	var ourContent = {
			what: what,
			xpath: tr.xpid,
			"_": surveyCurrentLocale,
			fhash: tr.rowHash,
			vhash: vHash,
			s: tr.theTable.session
	};

	var ourUrl = contextPath + "/SurveyAjax"; // ?what="+what+"&xpath="+tr.xpid +"&_="+surveyCurrentLocale+"&fhash="+tr.rowHash+"&vhash="+vHash+"&s="+tr.theTable.session;

	// vote reduced
	var voteReduced = document.getElementById("voteReduced");
	if(voteReduced) {
		ourContent.voteReduced = voteReduced.value;
	}

	var loadHandler = function(json){
		try {
			if(json.err && json.err.length >0) {
				tr.className='tr_err';
				handleDisconnect('Error submitting a vote', json);
				tr.innerHTML = "<td colspan='4'>"+stopIcon + " Could not check value. Try reloading the page.<br>"+json.err+"</td>";
				myUnDefer();
				handleDisconnect('Error submitting a vote', json);
			} else {
				if(json.submitResultRaw) { // if submitted..
					tr.className='tr_checking2';
					refreshRow2(tr,theRow,vHash,function(theRow) {
						// submit went through. Now show the pop.
						button.className='ichoice-o';
						button.checked=false;
						hideLoader(tr.theTable.theDiv.loader);
						if(json.testResults && (json.testWarnings || json.testErrors)) {
							// tried to submit, have errs or warnings.
							showProposedItem(tr.inputTd,tr,theRow,valToShow,json.testResults); // TODO: use  inputTd= (getTagChildren(tr)[tr.theTable.config.changecell])
						}
						if(box) {
							box.value=""; // submitted - dont show.
						}
						myUnDefer();
					}, function(err) {
						myUnDefer();
						handleDisconnect(err, json);
					}); // end refresh-loaded-fcn
					// end: async
				} else {
					// Did not submit. Show errors, etc
					if((json.statusAction&&json.statusAction!='ALLOW')
						|| (json.testResults && (json.testWarnings || json.testErrors ))) {
						showProposedItem(tr.inputTd,tr,theRow,valToShow,json.testResults,json);
						// TODO: use  inputTd= (getTagChildren(tr)[tr.theTable.config.changecell])
					} // else no errors, not submitted.  Nothing to do.
					if(box) {
						box.value=""; // submitted - dont show.
					}
					button.className='ichoice-o';
					button.checked = false;
					hideLoader(tr.theTable.theDiv.loader);
					myUnDefer();
				}
			}
		}catch(e) {
			tr.className='tr_err';
			tr.innerHTML = stopIcon + " Could not check value. Try reloading the page.<br>"+e.message;
			console.log("Error in ajax post [handleWiredClick] ",e.message);
			myUnDefer();
			handleDisconnect("handleWiredClick:"+e.message, json);
		}
	};
	var errorHandler = function(err, ioArgs){
		console.log('Error: ' + err + ' response ' + ioArgs.xhr.responseText);
		handleDisconnect('Error: ' + err + ' response ' + ioArgs.xhr.responseText, null);
		theRow.className = "ferrbox";
		theRow.innerHTML="Error while  loading: "+err.name + " <br> " + err.message + "<div style='border: 1px solid red;'>" + ioArgs.xhr.responseText + "</div>";
		myUnDefer();
	};
	if(box) {
		stdebug("this is a post: " + value);
		ourContent.value = value;
	}
	var xhrArgs = {
			url: ourUrl,
			handleAs:"json",
			content: ourContent,
			timeout: ajaxTimeout,
			load: loadHandler,
			error: errorHandler
	};
	queueXhr(xhrArgs);
}

/**
* bottleneck for cancel buttons
 * @method handleWiredClick
 */
function handleCancelWiredClick(tr,theRow,vHash,button) {
	var value="";
	var valToShow;
	if(tr.wait) {
		return;
	}

	valToShow=button.value;

	var what = 'delete';

	// select
	updateCurrentId(theRow.xpstrid);
	// and scroll
	showCurrentId();

	var myUnDefer = function() {
		tr.wait=false;
		setDefer(false);
	};
	tr.wait=true;
	resetPop(tr);
	setDefer(true);
	theRow.proposedResults = null;

	console.log("Delete " + tr.rowHash + " v='"+vHash+"', value='"+value+"'");
	var ourUrl = contextPath + "/SurveyAjax?what="+what+"&xpath="+tr.xpid +"&_="+surveyCurrentLocale+"&fhash="+tr.rowHash+"&vhash="+vHash+"&s="+tr.theTable.session;
	var loadHandler = function(json){
		try {
			if(json.err && json.err.length >0) {
				tr.className='tr_err';
				handleDisconnect('Error deleting a value', json);
				tr.innerHTML = "<td colspan='4'>"+stopIcon + " Could not check value. Try reloading the page.<br>"+json.err+"</td>";
				myUnDefer();
				handleDisconnect('Error deleting a value', json);
			} else {
				if(json.deleteResultRaw) { // if deleted..
					tr.className='tr_checking2';
					refreshRow2(tr,theRow,vHash,function(theRow){
						// delete went through. Now show the pop.
						hideLoader(tr.theTable.theDiv.loader);
						myUnDefer();
					}, function(err) {
						myUnDefer();
						handleDisconnect(err, json);
					}); // end refresh-loaded-fcn
					// end: async
				} else {
					// Did not submit. Show errors, etc
					if((json.statusAction&&json.statusAction!='ALLOW')
						|| (json.testResults && (json.testWarnings || json.testErrors ))) {
						showProposedItem(tr.inputTd,tr,theRow,valToShow,json.testResults,json);
						// TODO: use  inputTd= (getTagChildren(tr)[tr.theTable.config.changecell])
					} // else no errors, not submitted. Nothing to do.
					hideLoader(tr.theTable.theDiv.loader);
					myUnDefer();
				}
			}
		}catch(e) {
			tr.className='tr_err';
			tr.innerHTML = stopIcon + " Could not check value. Try reloading the page.<br>"+e.message;
			console.log("Error in ajax post [handleCancelWiredClick] ",e.message);
			myUnDefer();
			handleDisconnect("handleCancelWiredClick:"+e.message, json);
		}
	};
	var errorHandler = function(err, ioArgs){
		console.log('Error: ' + err + ' response ' + ioArgs.xhr.responseText);
		handleDisconnect('Error: ' + err + ' response ' + ioArgs.xhr.responseText, null);
		theRow.className = "ferrbox";
		theRow.innerHTML="Error while  loading: "+err.name + " <br> " + err.message + "<div style='border: 1px solid red;'>" + ioArgs.xhr.responseText + "</div>";
		myUnDefer();
	};
	var xhrArgs = {
			url: ourUrl+cacheKill(),
			handleAs:"json",
			timeout: ajaxTimeout,
			load: loadHandler,
			error: errorHandler
	};
	queueXhr(xhrArgs);
}

// TODO move admin panel to separate module
/**
 * Load the Admin Panel
 * @method loadAdminPanel
 */
function loadAdminPanel() {
	if(!vap) return;
	loadStui();
	var adminStuff=dojo.byId("adminStuff");
	if(!adminStuff) return;

	var content = document.createDocumentFragment();

	var list = document.createElement("ul");
	list.className="adminList";
	content.appendChild(list);

	function loadOrFail(urlAppend,theDiv, loadHandler, postData) {
		var ourUrl = contextPath + "/AdminAjax.jsp?vap="+vap+"&"+urlAppend;
		var errorHandler = function(err, ioArgs){
			console.log('adminload ' + urlAppend + ' Error: ' + err + ' response ' + ioArgs.xhr.responseText);
			theDiv.className = "ferrbox";
			theDiv.innerHTML="Error while  loading: "+err.name + " <br> " + err.message + "<div style='border: 1px solid red;'>" + ioArgs.xhr.responseText + "</div>";
		};
		var xhrArgs = {
				url: ourUrl+cacheKill(),
				handleAs:"json",
				load: loadHandler,
				error: errorHandler,
				postData: postData
		};
		if(!loadHandler) {
			xhrArgs.handleAs = "text";
			xhrArgs.load = function(text) {
				theDiv.innerHTML = text;
			};
		}
		if(xhrArgs.postData) {
			console.log("admin post: ourUrl: " + ourUrl + " data:" + postData);
            xhrArgs.headers = { "Content-Type": "text/plain"};
			dojo.xhrPost(xhrArgs);
		} else {
			console.log("admin get: ourUrl: " + ourUrl );
			dojo.xhrGet(xhrArgs);
		}
	}
	var panelLast = null;
	var panels={};
	var panelFirst = null;

	function panelSwitch(name) {
		if(panelLast) {
			panelLast.div.style.display='none';
			panelLast.listItem.className='notselected';
			panelLast=null;
		}
		if(name&&panels[name]) {
			panelLast=panels[name];
			panelLast.listItem.className='selected';
			panelLast.fn(panelLast.udiv);
			panelLast.div.style.display='block';
			window.location.hash="#!"+name;
		}
	}

	function addAdminPanel(type, fn) {
		var panel = panels[type]={type: type, name: stui.str(type), desc: stui.str(type+"_desc"), fn: fn};
		panel.div = document.createElement("div");
		panel.div.style.display='none';
		panel.div.className='adminPanel';

		var h = document.createElement("h3");
		h.className="adminTitle";
		h.appendChild(document.createTextNode(panel.desc));
		panel.div.appendChild(h);

		panel.udiv = document.createElement("div");
		panel.div.appendChild(panel.udiv);

		panel.listItem = document.createElement("li");
		panel.listItem.appendChild(document.createTextNode(panel.name));
		panel.listItem.title = panel.desc;
		panel.listItem.className="notselected";
		panel.listItem.onclick=function(e){panelSwitch(panel.type);return false;};
		list.appendChild(panel.listItem);

		content.appendChild(panel.div);

		if(!panelFirst) {
			panelFirst = panel;
		}
	}

	addAdminPanel("admin_users", function(div) {
		var frag = document.createDocumentFragment();

		var u = document.createElement("div");
		u.appendChild(document.createTextNode("Loading..."));
		frag.appendChild(u);

		removeAllChildNodes(div);
		div.appendChild(frag);
		loadOrFail("do=users", u, function(json) {
			var frag2 = document.createDocumentFragment();

			if(!json || !json.users || Object.keys(json.users)==0) {
				frag2.appendChild(document.createTextNode(stui.str("No users.")));
			} else {
				for(sess in json.users) {
					var cs = json.users[sess];
					var user = createChunk(null,"div","adminUser");
					user.appendChild(createChunk("Session: " + sess, "span","adminUserSession"));
					if(cs.user) {
						user.appendChild(createUser(cs.user));
					} else {
						user.appendChild(createChunk("(anonymous)","div","adminUserUser"));
					}
					user.appendChild(createChunk("Last: " + cs.last  + "LastAction: " + cs.lastAction + ", IP: " + cs.ip + ", ttk:"
								+ (parseInt(cs.timeTillKick)/1000).toFixed(1)+"s", "span","adminUserInfo"));

					var unlinkButton = createChunk(stui.str("admin_users_action_kick"), "button", "admin_users_action_kick");
					user.appendChild(unlinkButton);
					unlinkButton.onclick = function(e ) {
						unlinkButton.className = 'deactivated';
						unlinkButton.onclick = null;
						loadOrFail("do=unlink&s="+cs.id, unlinkButton, function(json) {
							removeAllChildNodes(unlinkButton);
							if(json.removing==null) {
								unlinkButton.appendChild(document.createTextNode('Already Removed'));
							} else {
								unlinkButton.appendChild(document.createTextNode('Removed.'));
							}
						});
						return stStopPropagation(e);
					};
					frag2.appendChild(user);
					frag2.appendChild(document.createElement("hr"));
				}
			}
			removeAllChildNodes(u);
			u.appendChild(frag2);
		});
	});

	addAdminPanel("admin_threads", function(div) {
		var frag = document.createDocumentFragment();

		div.className="adminThreads";
		var u = createChunk("Loading...","div","adminThreadList");
		var stack = createChunk(null,"div","adminThreadStack");
		frag.appendChild(u);
		frag.appendChild(stack);
		var c2s = createChunk(stui.str("clickToSelect"),"button","clickToSelect");
		clickToSelect(c2s,stack);

		removeAllChildNodes(div);
		div.appendChild(c2s);
		var clicked = null;

		div.appendChild(frag);
		loadOrFail("do=threads", u, function(json) {
			if(!json || !json.threads || Object.keys(json.threads.all)==0) {
				removeAllChildNodes(u);
				u.appendChild(document.createTextNode(stui.str("No threads.")));
			} else {
				var frag2 = document.createDocumentFragment();
				removeAllChildNodes(stack);
				stack.innerHTML = stui.str("adminClickToViewThreads");
				deadThreads={};
				if(json.threads.dead) {
					var header =createChunk(stui.str("adminDeadThreadsHeader"), "div", "adminDeadThreadsHeader");
					var deadul = createChunk("","ul","adminDeadThreads");
					for(var jj=0;jj<json.threads.dead.length;jj++) {
						var theThread = json.threads.dead[jj];
						var deadLi = createChunk("#"+theThread.id, "li");
						//deadLi.appendChild(createChunk(theThread.text,"pre"));
						deadThreads[theThread.id] = theThread.text;
						deadul.appendChild(deadLi);
					}
					header.appendChild(deadul);
					stack.appendChild(header);
				}
				for(id in json.threads.all) {
					var t = json.threads.all[id];
					var thread = createChunk(null,"div","adminThread");
					var tid;
					thread.appendChild(tid=createChunk(id,"span","adminThreadId"));
					if(deadThreads[id]) {
						tid.className = tid.className+" deadThread";
					}
					thread.appendChild(createChunk(t.name,"span","adminThreadName"));
					thread.appendChild(createChunk(stui.str(t.state),"span","adminThreadState_"+t.state));
					thread.onclick=(function (t,id){return (function() {
						stack.innerHTML = "<b>"+id+":"+t.name+"</b>\n";
						if(deadThreads[id]) {
							stack.appendChild(createChunk(deadThreads[id],"pre","deadThreadInfo"));
						}
						stack.appendChild(createChunk("\n\n{{{\n","span","textForTrac"));
						for(var q in t.stack) {
							stack.innerHTML = stack.innerHTML + t.stack[q] + "\n";
						}
						stack.appendChild(createChunk("}}}\n\n","span","textForTrac"));
					});})(t,id);
					frag2.appendChild(thread);
				}

				removeAllChildNodes(u);
				u.appendChild(frag2);
			}
		});
	});

	addAdminPanel("admin_exceptions", function(div) {
		var frag = document.createDocumentFragment();

		div.className="adminThreads";
		var v = createChunk(null,"div","adminExceptionList");
		var stack = createChunk(null,"div","adminThreadStack");
		frag.appendChild(v);
		var u = createChunk(null,"div");
		v.appendChild(u);
		frag.appendChild(stack);

		var c2s = createChunk(stui.str("clickToSelect"),"button","clickToSelect");
		clickToSelect(c2s,stack);

		removeAllChildNodes(div);
		div.appendChild(c2s);
		var clicked = null;

		var last = -1;

		var exceptions = [];

		var exceptionNames = {};

		div.appendChild(frag);
		var more = createChunk(stui_str("more_exceptions"),"p","adminExceptionMore adminExceptionFooter");
		var loading = createChunk(stui_str("loading"),"p","adminExceptionFooter");

		v.appendChild(loading);
		var loadNext  =function(from) {
			var append = "do=exceptions";
			if(from) {
				append = append + "&before="+from;
			}
			console.log("Loading: " + append);
			loadOrFail(append, u, function(json) {
				if(!json || !json.exceptions || !json.exceptions.entry) {
					if(!from) {
						v.appendChild(createChunk(stui_str("no_exceptions"),"p","adminExceptionFooter"));
					} else {
						v.removeChild(loading);
						v.appendChild(createChunk(stui_str("last_exception"),"p","adminExceptionFooter"));
						// just the last one.
					}
				} else {
					if(json.exceptions.entry.time == from) {
						console.log("Asked for <"+from + " but got ="+from);
						v.removeChild(loading);
						return; //
					}
					var frag2 = document.createDocumentFragment();
					if(!from) {
						removeAllChildNodes(stack);
						stack.innerHTML = stui.str("adminClickToViewExceptions");
					}
					// TODO: if(json.threads.dead) frag2.appendChunk(json.threads.dead.toString(),"span","adminDeadThreads");
					last = json.exceptions.lastTime;
					if(json.exceptions.entry) {
						var e = json.exceptions.entry;
						exceptions.push(json.exceptions.entry);
						var exception = createChunk(null,"div","adminException");
						if(e.header&&e.header.length < 80) {
							exception.appendChild(createChunk(e.header,"span","adminExceptionHeader"));
						} else {
							var t;
							exception.appendChild(t=createChunk(e.header.substring(0,80)+"...","span","adminExceptionHeader"));
							t.title=e.header;
						}
						exception.appendChild(createChunk(e.DATE,"span","adminExceptionDate"));
						var clicky=(function (e) {
							return (function(ee) {
								var frag3 = document.createDocumentFragment();
								frag3.appendChild(createChunk("{{{\n","span","textForTrac"));
								frag3.appendChild(createChunk(e.header,"span","adminExceptionHeader"));
								frag3.appendChild(createChunk("}}}\n","span","textForTrac"));
								frag3.appendChild(createChunk(e.DATE,"span","adminExceptionDate"));

								if(e.UPTIME) {
									frag3.appendChild(createChunk(e.UPTIME,"span","adminExceptionUptime"));
								}
								if(e.CTX) {
									frag3.appendChild(createChunk(e.CTX,"span","adminExceptionUptime"));
								}
								for(var q in e.fields) {
									var f = e.fields[q];
									var k = Object.keys(f);
									frag3.appendChild(createChunk("\n'''"+k[0]+"'''\n"+"{{{\n","span","textForTrac"));
									frag3.appendChild(createChunk(f[k[0]],"pre","adminException"+k[0]));
									frag3.appendChild(createChunk("}}}\n","span","textForTrac"));
								}

								if(e.LOGSITE) {
									frag3.appendChild(createChunk("'''LOGSITE'''\n{{{\n","span","textForTrac"));
									frag3.appendChild(createChunk(e.LOGSITE,"pre","adminExceptionLogsite"));
									frag3.appendChild(createChunk("}}}\n","span","textForTrac"));
								}
								removeAllChildNodes(stack);
								stack.appendChild(frag3);
								stStopPropagation(ee);
								return false;
							});
						})(e);
						listenFor(exception, "click", clicky);
						var head = exceptionNames[e.header];
						if(head) {
							if(!head.others) {
								head.others=[];
								head.count = document.createTextNode("");
								var countSpan = document.createElement("span");
								countSpan.appendChild(head.count);
								countSpan.className = "adminExceptionCount";
								listenFor(countSpan, "click", function(e) {
									// prepare div
									if(!head.otherdiv) {
										head.otherdiv = createChunk(null,"div","adminExceptionOtherList");
										head.otherdiv.appendChild(createChunk(stui.str("adminExceptionDupList"),"h4"));
										for(k in head.others) {
											head.otherdiv.appendChild(head.others[k]);
										}
									}
									removeAllChildNodes(stack);
									stack.appendChild(head.otherdiv);
									stStopPropagation(e);
									return false;
								});
								head.appendChild(countSpan);
							}
							head.others.push(exception);
							head.count.nodeValue = stui.sub("adminExceptionDup", [ head.others.length ]);
							head.otherdiv=null; // reset
						} else {
							frag2.appendChild(exception);
							exceptionNames[e.header] = exception;
						}
					}
					u.appendChild(frag2);

					if(json.exceptions.entry && json.exceptions.entry.time) {
						if(exceptions.length>0 && (exceptions.length % 8 == 0)) {
							v.removeChild(loading);
							v.appendChild(more);
							more.onclick = more.onmouseover = function() {
								v.removeChild(more);
								v.appendChild(loading);
								loadNext(json.exceptions.entry.time);
								return false;
							};
						} else {
							setTimeout(function(){loadNext(json.exceptions.entry.time);},500);
						}
					}
				}
			});
		};
		loadNext(); // load the first exception
	});

	addAdminPanel("admin_settings", function(div) {
		var frag = document.createDocumentFragment();

		div.className="adminSettings";
		var u = createChunk("Loading...","div","adminSettingsList");
		frag.appendChild(u);
		loadOrFail("do=settings", u, function(json) {
			if(!json || !json.settings || Object.keys(json.settings.all)==0) {
				removeAllChildNodes(u);
				u.appendChild(document.createTextNode(stui.str("nosettings")));
			} else {
				var frag2 = document.createDocumentFragment();
				for(id in json.settings.all) {
					var t = json.settings.all[id];

					var thread = createChunk(null,"div","adminSetting");

					thread.appendChild(createChunk(id,"span","adminSettingId"));
					if(id == "CLDR_HEADER" ) 	{
						(function(theHeader,theValue) {
						var setHeader = null;
						setHeader = appendInputBox(thread, "adminSettingsChangeTemp");
						setHeader.value = theValue;
						setHeader.stChange=function(onOk,onErr) {
							loadOrFail("do=settings_set&setting="+theHeader, u, function(json) {
								if(!json || !json.settings_set || !json.settings_set.ok) {
									onErr(stui_str("failed"));
									onErr(json.settings_set.err);
								} else {
									if(json.settings_set[theHeader]) {
										setHeader.value = json.settings_set[theHeader];
										if(theHeader=="CLDR_HEADER") {
											updateSpecialHeader(setHeader.value);
										}
									} else {
										setHeader.value = "";
										if(theHeader=="CLDR_HEADER") {
											updateSpecialHeader(null);
										}
									}
									onOk(stui_str("changed"));
								}
							}, setHeader.value);
							return false;
						 };
						})(id,t); // call it

						if(id=="CLDR_HEADER") {
							updateSpecialHeader(t);
						}
					} else {
						thread.appendChild(createChunk(t,"span","adminSettingValue"));
					}
					frag2.appendChild(thread);
				}
				removeAllChildNodes(u);
				u.appendChild(frag2);
			}
		});

		removeAllChildNodes(div);
		div.appendChild(frag);
	});

	addAdminPanel("admin_ops", function(div) {
		var frag = document.createDocumentFragment();

		div.className="adminThreads";

		var baseUrl = contextPath + "/AdminPanel.jsp?vap="+vap+"&do=";
		var hashSuff  = ""; //  "#" + window.location.hash;

		var actions = ["rawload"];
		for(var k in actions) {
			var action = actions[k];
			var newUrl = baseUrl + action + hashSuff;
			var b = createChunk(stui_str(action), "button");
			b.onclick = function() {window.location = newUrl;  return false; };
			frag.appendChild(b);
		}
		removeAllChildNodes(div);
		div.appendChild(frag);

	});

	// last panel loaded.
	// If it's in the hashtag, use it, otherwise first.
	if(window.location.hash && window.location.hash.indexOf("#!")==0) {
		panelSwitch(window.location.hash.substring(2));
	}
	if(!panelLast) { // not able to load anything.
		panelSwitch(panelFirst.type);
	}
	adminStuff.appendChild(content);
}

/**
 * @method showstats
 * Show the statistics area in the named element
 * @param {String} hname the name of the element to draw into
 */
function showstats(hname) {
	dojo.ready(loadStui(null, function(/*stui*/) {
		var ourUrl = contextPath + "/SurveyAjax?what=stats_byday";
		var errorHandler = function(err, ioArgs) {
			handleDisconnect('Error in showstats: ' + err + ' response '
			+ ioArgs.xhr.responseText);
		};
		showLoader(null, "Loading statistics");
		var loadHandler = function(json) {
			try {
				if (json) {
					var r = Raphael(hname);
					var header=json.byday.header;
					var data=json.byday.data;
					var header_new=json.byday_new.header;
					var data_new=json.byday_new.data;
					var labels_old = [];
					var count_old = [];
					var labels = [];
					var count_new = [];
					for(var i in data_new) {
						var newLabel = new Date(data_new[i][header_new.LAST_MOD]).toLocaleDateString();
						var newCount = data_new[i][header_new.COUNT];
						labels.push(newLabel); // labels come from new data
						count_new.push(newCount);
						var oldLabel = new Date(data[i][header.LAST_MOD]).toLocaleDateString();
						if(newLabel == oldLabel) {
							// have old data
							var oldCount = data[i][header.COUNT];
							if(oldCount < newCount) {
								console.log("Preposterous: at " + newLabel + ": " + oldCount + " oldCount < " + newCount + "  newCount " );
								count_old.push(-1);
							} else {
								count_old.push(oldCount - newCount);
							}
						} else {
							console.log("Desync: " + newLabel + " / " + oldLabel);
							count_old.push(-1);
						}
					}
					var gdata = [];
					gdata.push(count_new);
					gdata.push(count_old);
					showLoader(null, "Drawing");
					// this: 0,id,node,paper,attrs,transformations,_,prev,next,type,bar,value,events
					// this.bar ["0", "id", "node", "paper", "attrs", "transformations", "_", "prev", "next", "type", "x", "y", "w", "h", "value"]
                    var fin = function () {
                    	this.flag = r.g.popup(this.bar.x, this.bar.y, this.bar.value || "0").insertBefore(this);
                    },
                    fout = function () {
                    	this.flag.animate({opacity: 0}, 300, function () {this.remove();});
                    };
					var labels2 = [];
					labels2.push(labels);
					var hei = 500;
					var offh = 10;
					var toffh = 30;
					var toffv=10+(hei/(2*labels.length));
					console.log("Drawing in : " + hname + " - "  + count_new.toString());
					r.g.hbarchart(100,offh,600,hei, gdata,  {
						stacked: true,
						colors: ["#8aa717","#1751a7"]
					})
					.hover(fin,fout);
					for(var i in labels) {
						r.text(toffh,toffv+(i*(hei/labels.length)), (labels[i].split(" ")[0])+"\n"+count_new[i]  );
					}
					hideLoader(null);
				} else {
					handleDisconnect("Failed to load JSON stats",json);
				}
			} catch (e) {
			console.log("Error in ajax get ", e.message);
			console.log(" response: " + text);
			handleDisconnect(" exception in getstats: " + e.message,null);
			}
		};
		var xhrArgs = {
				url : ourUrl,
				handleAs : "json",
				load : loadHandler,
				error : errorHandler
			};
		queueXhr(xhrArgs);
	}));
}

/**
 * @method refreshCounterVetting
 * Update the counter on top of the vetting page
 */
function refreshCounterVetting() {
	if(isVisitor) {
		//if the user is a visitor, don't display the counter informations
		$('#nav-page .counter-infos, #nav-page .nav-progress').hide();
		return;
	}

	var inputs = $('.vetting-page input:visible:checked');
	var total = inputs.length;
	var abstain = inputs.filter(function() { return this.id.substr(0,2) === 'NO';}).length;
	var voted = total - abstain;

	document.getElementById('count-total').innerHTML = total;
	document.getElementById('count-abstain').innerHTML = abstain;
	document.getElementById('count-voted').innerHTML = voted;
	if(total === 0) {
		total = 1;
	}
	document.getElementById('progress-voted').style.width = voted*100/total + '%';
	document.getElementById('progress-abstain').style.width = abstain*100/total + '%';
}

/**
 * @method chgPage
 * Go to the next (1) or the previous page (1) during the vetting
 * @param {Integer} shift next page (1) or previous (-1)
 */
function chgPage(shift) {
	//no page, or wrong shift
	if(!_thePages || (shift !== -1 && shift !== 1))
		return;

	var menus = getMenusFilteredByCov();
	var parentIndex = 0;
	var index = 0;
	var parent = _thePages.pageToSection[surveyCurrentPage].id;

	//get the parent index
	for(var m in menus) {
		var menu = menus[m];
		if(menu.id === parent) {
			parentIndex = parseInt(m);
			break;
		}
	}

	for(var m in menus[parentIndex].pagesFiltered) {
		var menu = menus[parentIndex].pagesFiltered[m];
		if(menu.id === surveyCurrentPage) {
			index = parseInt(m);
			break;
		}
	}
	//go to the next one
	index += parseInt(shift);

	if(index >= menus[parentIndex].pagesFiltered.length) {
		parentIndex++;
		index = 0;
		if(parentIndex >= menus.length) {
			parentIndex = 0;
		}
	}

	if(index < 0) {
		parentIndex--;
		if(parentIndex < 0) {
			parentIndex = menus.length - 1;
		}
		index = menus[parentIndex].pagesFiltered.length - 1;
	}
	surveyCurrentSection = menus[parentIndex].id;
	surveyCurrentPage = menus[parentIndex].pagesFiltered[index].id;

	reloadV();

	var sidebar = $('#locale-menu #'+surveyCurrentPage);
	sidebar.closest('.open-menu').click();
}

/**
 * @method getMenusFilteredByCov
 * Get all the menus under this coverage
 * @return {Array} list of all the menus under this coverage
 */
function getMenusFilteredByCov() {
	if (!_thePages) {
		return;
	}
	//get name of current coverage
	var cov = surveyUserCov;
	if(!cov) {
		cov = surveyOrgCov;
	}

	//get the value
	var val = covValue(cov);
	var sections = _thePages.sections;
	var menus = [];
	//add filtered pages
	for(var s in sections) {
		var section = sections[s];
		var pages = section.pages;
		var sectionContent = [];
		for(var p in pages) {
			var page = pages[p];
			var key = Object.keys(page.levs).pop();
			if(parseInt(page.levs[key]) <= val)
				sectionContent.push(page);
		}

		if(sectionContent.length) {
			section.pagesFiltered = sectionContent;
			menus.push(section);
		}
	}
	return menus;
}


///////////////////
// for vetting
function changeStyle(hideRegex) {
    for (m in document.styleSheets) {
        var theRules;
        if (document.styleSheets[m].cssRules) {
            theRules = document.styleSheets[m].cssRules;
        } else if (document.styleSheets[m].rules) {
            theRules = document.styleSheets[m].rules;
        }
        for (n in theRules) {
            var rule = theRules[n];
            var sel = rule.selectorText;
            if (sel != undefined && sel.match(/vv/))   {
                var theStyle = rule.style;
                if (sel.match(hideRegex)) {
                    if (theStyle.display == 'table-row') {
                        theStyle.display = null;
                    }
                } else {
                    if (theStyle.display != 'table-row') {
                        theStyle.display = 'table-row';

                    }
                }
            }
        }
    }
}

function setStyles() {
    var hideRegexString = "X1234X";
    for (var i=0; i < document.checkboxes.elements.length; i++){
        var item = document.checkboxes.elements[i];
        if (!item.checked) {
            hideRegexString += "|";
            hideRegexString += item.name;
        }
    }
    var hideRegex = new RegExp(hideRegexString);
    changeStyle(hideRegex);
}

function createLocLink(loc, locName, className) {
    var cl = createChunk(locName, "a", "localeChunk "+className);
    cl.title=loc;
    cl.href = "survey?_="+loc;
    return cl;
}

function showAllItems(divName, user) {
	dojo.ready(function() {
		loadStui();
		var div = dojo.byId(divName);
		div.className = "recentList";
		div.update = function() {
		var ourUrl = contextPath + "/SurveyAjax?what=mylocales&user="+user;
		var errorHandler = function(err, ioArgs) {
			handleDisconnect('Error in showrecent: ' + err + ' response '
			+ ioArgs.xhr.responseText);
		};
		showLoader(null, "Loading recent items");
		var loadHandler = function(json) {
			try {
				if (json && json.mine) {
					var frag = document.createDocumentFragment();
					var header = json.mine.header;
					var data = json.mine.data;
					if(data.length==0) {
						frag.appendChild(createChunk(stui_str("recentNone"),"i"));
					} else {
						var rowDiv = document.createElement("div");
						frag.appendChild(rowDiv);

						rowDiv.appendChild(createChunk(stui_str("recentLoc"),"b"));
						rowDiv.appendChild(createChunk(stui_str("recentCount"),"b"));

						for(var q in data) {
							var row = data[q];

							var count = row[header.COUNT];

							var rowDiv = document.createElement("div");
							frag.appendChild(rowDiv);

							var loc = row[header.LOCALE];
							var locname = row[header.LOCALE_NAME];
                                                        rowDiv.appendChild(createLocLink(loc, locname, "recentLoc"));
							rowDiv.appendChild(createChunk(count,"span","value recentCount"));

                            if(surveySessionId!=null) {
                                var dlLink = createChunk(stui_str("downloadXmlLink"),"a","notselected");
                                dlLink.href = "DataExport.jsp?do=myxml&_="+loc+"&user="+user+"&s="+surveySessionId;
                                dlLink.target="STDownload";
                                rowDiv.appendChild(dlLink);
                            }
						}
					}

					removeAllChildNodes(div);
					div.appendChild(frag);
					hideLoader(null);
				} else {
					handleDisconnect("Failed to load JSON recent items",json);
				}
			} catch (e) {
				console.log("Error in ajax get ", e.message);
				console.log(" response: " + text);
				handleDisconnect(" exception in getrecent: " + e.message,null);
			}
		};
		var xhrArgs = {
				url : ourUrl,
				handleAs : "json",
				load : loadHandler,
				error : errorHandler
			};
		queueXhr(xhrArgs);
		};
		div.update();
	});
}

function showRecent(divName, locale, user) {
	if(!locale) {
		locale='';
	}
	if(!user) {
		user='';
	}
	dojo.ready(function() {
		loadStui();
		var div;

		if(divName.nodeType>0 ) {
			div = divName;
		} else {
			div = dojo.byId(divName);
		}
		div.className = "recentList";
		div.update = function() {
		var ourUrl = contextPath + "/SurveyAjax?what=recent_items&_="+locale+"&user="+user+"&limit="+15;
		var errorHandler = function(err, ioArgs) {
			handleDisconnect('Error in showrecent: ' + err + ' response '
			+ ioArgs.xhr.responseText);
		};
		showLoader(null, "Loading recent items");
		var loadHandler = function(json) {
			try {
				if (json&&json.recent) {
					var frag = document.createDocumentFragment();
					var header = json.recent.header;
					var data = json.recent.data;

					if(data.length==0) {
						frag.appendChild(createChunk(stui_str("recentNone"),"i"));
					} else {
						var rowDiv = document.createElement("div");
						frag.appendChild(rowDiv);

						rowDiv.appendChild(createChunk(stui_str("recentLoc"),"b"));
						rowDiv.appendChild(createChunk(stui_str("recentXpathCode"),"b"));
						rowDiv.appendChild(createChunk(stui_str("recentValue"),"b"));
						rowDiv.appendChild(createChunk(stui_str("recentWhen"),"b"));

						for(var q in data) {
							var row = data[q];

							var loc = row[header.LOCALE];
                                                        var locname = row[header.LOCALE_NAME];
							var org = row[header.ORG];
							var last_mod = row[header.LAST_MOD];
							var xpath = row[header.XPATH];
							var xpath_code = row[header.XPATH_CODE];
							var xpath_hash = row[header.XPATH_STRHASH];
							var value = row[header.VALUE];

							var rowDiv = document.createElement("div");
							frag.appendChild(rowDiv);
							rowDiv.appendChild(createLocLink(loc,locname, "recentLoc"));
							var xpathItem;
							xpath_code = xpath_code.replace(/\t/g," / ");
							rowDiv.appendChild(xpathItem = createChunk(xpath_code,"a","recentXpath"));
							xpathItem.href = "survey?_="+loc+"&strid="+xpath_hash;
							rowDiv.appendChild(createChunk(value,"span","value recentValue"));
							rowDiv.appendChild(createChunk(new Date(last_mod).toLocaleString(),"span","recentWhen"));
						}
					}

					removeAllChildNodes(div);
					div.appendChild(frag);
					hideLoader(null);
				} else {
					handleDisconnect("Failed to load JSON recent items",json);
				}
			} catch (e) {
			console.log("Error in ajax get ", e.message);
			console.log(" response: " + text);
			handleDisconnect(" exception in getrecent: " + e.message,null);
			}
		};
		var xhrArgs = {
				url : ourUrl,
				handleAs : "json",
				load : loadHandler,
				error : errorHandler
			};
		queueXhr(xhrArgs);
		};
	div.update();
	});
}

// for the admin page
function showUserActivity(list, tableRef) {
	loadStui(null, function(/*stui*/) {
	require([
	         "dojo/ready",
	         "dojo/dom",
	         "dojo/dom-construct",
	         "dojo/request",
	         "dojo/number",
	         "dojo/domReady!"
	         ],
	         // HANDLES
	         function(
	        		 ready,
	        		 dom,
	        		 dcons,
	        		 request,
	        		 dojoNumber
	        ) { ready(function(){

	        	window._userlist = list; // DEBUG
	        	var table = dom.byId(tableRef);

	        	var rows = [];
	        	var theadChildren = getTagChildren(table.getElementsByTagName("thead")[0].getElementsByTagName("tr")[0]);

	        	setDisplayed(theadChildren[1],false);
	        	var rowById = [];

	        	for(var k in list ) {
	        		var user = list[k];
	        		var tr = dom.byId('u@' + user.id);

	        		rowById[user.id] = parseInt(k); // ?!

	        		var rowChildren = getTagChildren(tr);

	        		removeAllChildNodes(rowChildren[1]); // org
	        		removeAllChildNodes(rowChildren[2]); // name

	        		var theUser;
		        	setDisplayed(rowChildren[1],false);
	        		rowChildren[2].appendChild(theUser = createUser(user));

	        		rows.push( {user: user, tr: tr, userDiv: theUser, seen: rowChildren[5], stats: [], total: 0  } );
	        	}

	        	window._rrowById = rowById;

	        	var loc2name={};
        		request
    			.get(contextPath + "/SurveyAjax?what=stats_bydayuserloc", {handleAs: 'json'})
    			.then(function(json) {
    				/* COUNT: 1120,  DAY: 2013-04-30, LOCALE: km, LOCALE_NAME: khmer, SUBMITTER: 2 */
    				var stats = json.stats_bydayuserloc;
    				var header = stats.header;
    				for(var k in stats.data) {
    					var row = stats.data[k];
    					var submitter = row[header.SUBMITTER];
    					var submitterRow = rowById[submitter];
    					if(submitterRow !== undefined) {
    						var userRow = rows[submitterRow];
    						userRow.stats.push({day: row[header.DAY], count: row[header.COUNT], locale: row[header.LOCALE]});
    						userRow.total = userRow.total + row[header.COUNT];
    						loc2name[row[header.LOCALE]]=row[header.LOCALE_NAME];
    					}
    				}

    				function appendMiniChart(userRow, count) {
    					if(count > userRow.stats.length) {
    						count = userRow.stats.length;
    					}
    					removeAllChildNodes(userRow.seenSub);
						for(var k=0;k<count;k++) {
							var theStat = userRow.stats[k];
							var chartRow = createChunk('','div','chartRow');

							var chartDay = createChunk(theStat.day, 'span', 'chartDay');
							var chartLoc = createChunk(theStat.locale, 'span', 'chartLoc');
							chartLoc.title = loc2name[theStat.locale];
							var chartCount = createChunk(dojoNumber.format(theStat.count), 'span', 'chartCount');

							chartRow.appendChild(chartDay);
							chartRow.appendChild(chartLoc);
							chartRow.appendChild(chartCount);

							userRow.seenSub.appendChild(chartRow);
						}
						if(count < userRow.stats.length) {
							chartRow.appendChild(document.createTextNode('...'));
						}
    				}

    				for(var k in rows) {
    					var userRow = rows[k];
						if(userRow.total > 0) {
							addClass(userRow.tr, "hadActivity");
							userRow.tr.getElementsByClassName('recentActivity')[0].appendChild(document.createTextNode(' ('+dojoNumber.format(userRow.total)+')'));

							userRow.seenSub = document.createElement('div');
							userRow.seenSub.className = 'seenSub';
							userRow.seen.appendChild(userRow.seenSub);

							appendMiniChart(userRow, 3);
							if(userRow.stats.length > 3) {
								var chartMore, chartLess;
								chartMore = createChunk('+', 'span','chartMore');
								chartLess = createChunk('-', 'span','chartMore');
								chartMore.onclick = (function(chartMore, chartLess, userRow) {
									return function () {
										setDisplayed(chartMore, false);
										setDisplayed(chartLess, true);
										appendMiniChart(userRow, userRow.stats.length);
										return false;
									};})(chartMore, chartLess, userRow);
								chartLess.onclick = (function(chartMore, chartLess, userRow) {
									return function () {
										setDisplayed(chartMore, true);
										setDisplayed(chartLess, false);
										appendMiniChart(userRow, 3);
										return false;
									};})(chartMore, chartLess, userRow);
								userRow.seen.appendChild(chartMore);
								setDisplayed(chartLess, false);
								userRow.seen.appendChild(chartLess);
							}

						} else {
							addClass(userRow.tr, "noActivity");
						}
    				}
    			});
	        });
		});
	});
}

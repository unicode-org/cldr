/*
 * survey.js - Copyright (C) 2012,2016 IBM Corporation and Others. All Rights Reserved.
 * SurveyTool main JavaScript stuff
 */

/*
 * INHERITANCE_MARKER indicates that the value of a candidate item is inherited.
 * Compare INHERITANCE_MARKER in CldrUtility.java.
 */
const INHERITANCE_MARKER = "↑↑↑";

/**
 * Is the given string for a report, that is, does it start with "r_"?
 *
 * @class GLOBAL
 *
 * @param str the string
 * @return true if starts with "r_", else false
 *
 * This function is only actually used locally in survey.js.
 */
function isReport(str) {
	return (str[0] == 'r' && str[1] == '_');
}

/**
 * Remove a CSS class from a node
 *
 * @param {Node} obj
 * @param {String} className
 */
function removeClass(obj, className) {
	if (!obj) {
		return obj;
	}
	if (obj.className.indexOf(className) > -1) {
		obj.className = obj.className.substring(className.length + 1);
	}
	return obj;
}

/**
 * Add a CSS class from a node
 *
 * @param {Node} obj
 * @param {String} className
 */
function addClass(obj, className) {
	if (!obj) {
		return obj;
	}
	if (obj.className.indexOf(className) == -1) {
		obj.className = className + " " + obj.className;
	}
	return obj;
}

/**
 * Remove all subnodes
 *
 * @param {Node} td
 */
function removeAllChildNodes(td) {
	if (td == null) {
		return;
	}
	while (td.firstChild) {
		td.removeChild(td.firstChild);
	}
}

/**
 * Set/remove style.display
 */
function setDisplayed(div, visible) {
	if (div === null) {
		console.log("setDisplayed: called on null");
		return;
	} else if (div.domNode) {
		setDisplayed(div.domNode, visible); // recurse, it's a dijit
	} else if (!div.style) {
		console.log("setDisplayed: called on malformed node " + div + " - no style! " + Object.keys(div));
	} else {
		if (visible) {
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
	for (var k in ids) {
		var id = ids[k];
		var node = document.getElementById(id);
		// TODO if node==null throw
		if (this._panes.length > 0) {
			setDisplayed(node, false); // hide it
		} else {
			this._visible = id;
		}
		this._panes.push(node);
		this._map[id] = node;
	}
}

/**
 * @param {String} id
 * @param {Node} node - if non null - replace new page with this
 */
Flipper.prototype.flipTo = function(id, node) {
	if (!this._map[id]) {
		return; // TODO throw
	}
	if (this._visible == id && (node === null)) {
		return; // noop - unless adding s'thing
	}
	setDisplayed(this._map[this._visible], false);
	for (var k in this._killfn) {
		this._killfn[k]();
	}
	this._killfn = []; // pop?
	if (node !== null && node !== undefined) {
		removeAllChildNodes(this._map[id]);
		if (node.nodeType > 0) {
			this._map[id].appendChild(node);
		} else {
			for (var kk in node) {
				// it's an array, add all
				this._map[id].appendChild(node[kk]);
			}
		}
	}
	setDisplayed(this._map[id], true);
	this._visible = id;
	return this._map[id];
};

/**
 * @param id page id or null for current
 * @returns
 */
Flipper.prototype.get = function(id) {
	if (id) {
		return this._map[id];
	} else {
		return this._map[this._visible];
	}
};

/**
 * @param id
 */
Flipper.prototype.flipToEmpty = function(id) {
	return this.flipTo(id, []);
};

/**
 * killFn is called on next flip
 *
 * @param killFn the function to call. No params.
 */
Flipper.prototype.addKillFn = function(killFn) {
	this._killfn.push(killFn);
};

/**
 * showfn is called, result is added to the div.  killfn is called when page is flipped.
 *
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
 *
 * @param aLocmap the map object from json
 */
function LocaleMap(aLocmap) {
	this.locmap = aLocmap;
}

/**
 * Run the locale id through the idmap.
 *
 * @param menuMap the map
 * @param locid or null for surveyCurrentLocale
 * @return canonicalized id, or unchanged
 */
LocaleMap.prototype.canonicalizeLocaleId = function canonicalizeLocaleId(locid) {
	if (locid === null) {
		locid = surveyCurrentLocale;
	}
	if (locid === null || locid === '') {
		return null;
	}

	if (this.locmap) {
		if (this.locmap.idmap && this.locmap.idmap[locid]) {
			locid = this.locmap.idmap[locid]; // canonicalize
		}
	}
	return locid;
};

window.linkToLocale = function linkToLocale(subLoc) {
	return "#/" + subLoc + "/" + surveyCurrentPage + "/" + surveyCurrentId;
};

/**
 * Linkify text like '@de' into some link to German.
 *
 * @param str (html)
 * @return linkified str (html)
 */
LocaleMap.prototype.linkify = function linkify(str) {
	var out = "";
	var re = /@([a-zA-Z0-9_]+)/g;
	var match;
	var fromLast = 0;
	while ((match = re.exec(str)) != null) {
		var bund = this.getLocaleInfo(match[1]);
		if (bund) {
			out = out + str.substring(fromLast, match.index); // pre match
			if (match[1] == surveyCurrentLocale) {
				out = out + this.getLocaleName(match[1]);
			} else {
				out = out + "<a href='" + linkToLocale(match[1]) + "' title='" + match[1] + "'>" + this.getLocaleName(match[1]) + "</a>";
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
 *
 * @param menuMap the map
 * @param locid the id - should already be canonicalized
 * @return the bundle or null
 */
LocaleMap.prototype.getLocaleInfo = function getLocaleInfo(locid) {
	if (this.locmap && this.locmap.locales && this.locmap.locales[locid]) {
		return this.locmap.locales[locid];
	} else {
		return null;
	}
};

/**
 * Return the locale name,
 *
 * @param menuMap the map
 * @param locid the id - will canonicalize
 * @return the display name - or else the id
 */
LocaleMap.prototype.getLocaleName = function getLocaleName(locid) {
	locid = this.canonicalizeLocaleId(locid);
	var bund = this.getLocaleInfo(locid);
	if (bund && bund.name) {
		return bund.name;
	} else {
		return locid;
	}
};

/**
 * Return the locale name,
 *
 * @param menuMap the map
 * @param locid the id - will canonicalize
 * @return the display name - or else the id
 */
LocaleMap.prototype.getRegionAndOrVariantName = function getRegionAndOrVariantName(locid) {
	locid = this.canonicalizeLocaleId(locid);
	var bund = this.getLocaleInfo(locid);
	if (bund) {
		var ret = "";
		if (bund.name_rgn) {
			ret = ret + bund.name_rgn;
		}
		if (bund.name_var) {
			ret = ret + " (" + bund.name_var + ")";
		}
		if (ret != "") {
			return ret; // region OR variant OR both
		}
		if (bund.name) {
			return bund.name; // fallback to name
		}
	}
	return locid; // fallbcak to locid
};

/**
 * Return the locale language
 *
 * @param locid
 * @returns the language portion
 */
LocaleMap.prototype.getLanguage = function getLanguage(locid) {
	return locid.split('_')[0].split('-')[0];
};

/**
 * @class XpathMap
 * This manages xpathId / strid / PathHeader etc mappings.
 * It is a cache, and gets populated with data 'volunteered' by page loads, so that
 * hopefully it doesn't need to do any network operations on its own.
 * However, it MAY BE capable of filling in any missing data via AJAX. This is why its operations are async.
 */
function XpathMap() {
	/**
	 * Maps strid (hash) to info struct.
	 * The info struct is the basic unit here, it looks like the following:
	 *   {
	 *	   hex: '20fca8231d41',
	 *	   path: '//ldml/shoeSize',
	 *	   id:  1337,
	 *	   ph: ['foo','bar','baz']  **TBD**
	 *   }
	 *
	 *  All other hashes here are just alternate indices into this data.
	 *
	 * @property XpathMap.stridToInfo
	 */
	this.stridToInfo = {};
	/**
	 * Map xpathId (such as 1337) to info
	 * @property XpathMap.xpidToInfo
	 */
	this.xpidToInfo = {};
	/**
	 * Map xpath (//ldml/...) to info.
	 * @property XpathMap.xpathToInfo
	 */
	this.xpathToInfo = {};
}

/**
 * This function will do a search and then call the onResult function.
 * Priority order for search: hex, id, then path.
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
	if (!result && search.hex) {
		result = this.stridToInfo[search.hex];
	}
	if (!result && search.id) {
		if (typeof search.id !== Number) {
			search.id = new Number(search.id);
		}
		result = this.xpidToInfo[search.id];
	}
	if (!result && search.path) {
		result = this.xpathToInfo[search.path];
	}
	if (result) {
		onResult({
			search: search,
			result: result
		});
	} else {
		stdebug("XpathMap search failed for " + JSON.stringify(search) + " - doing rpc");
		var querystr = null;
		if (search.hex) {
			querystr = search.hex;
		} else if (search.path) {
			querystr = search.path;
		} else if (search.id) {
			querystr = "#" + search.id;
		} else {
			querystr = ''; // error
		}
		require(["dojo/request"], function(request) {
			request
				.get('SurveyAjax', {
					handleAs: 'json',
					query: {
						what: 'getxpath',
						xpath: querystr
					}
				})
				.then(function(json) {
					if (json.getxpath) {
						xpathMap.put(json.getxpath); // store back first, then
						onResult({
							search: search,
							result: json.getxpath
						}); // call
					} else {
						onResult({
							search: search,
							err: 'no results from server'
						});
					}
				})
				.otherwise(function(err) {
					onResult({
						search: search,
						err: err
					});
				});
		});
	}
};

/**
 * Contribute some data to the map.
 * @function contribute
 */
XpathMap.prototype.put = function put(info) {
	if (!info || !info.id || !info.path || !info.hex || !info.ph) {
		stdebug("XpathMap: rejecting incomplete contribution " + JSON.stringify(info));
	} else if (this.stridToInfo[info.hex]) {
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
	if (!ph) {
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
	error: "Disconnected: Error",
	"details": "Details...",
	disconnected: "Disconnected",
	startup: "Starting up...",
	ari_sessiondisconnect_message: "Your session has been disconnected.",
};

/*
 * These temporary versions of stui.str and stui.sub are "while we wait for the resources to load";
 * see loadStui() later in this file.
 * TODO: modernize with modules, avoid duplication here and later of stui.str, stui.sub!
 *
 * https://dojotoolkit.org/reference-guide/1.10/dojo/string.html
 */
require(["dojo/string"], function(string) {
	stui.str = function(x) {
		if (stui[x]) {
			return stui[x];
		}
		else {
			return "";
		}
	};
	stui.sub = function(x, y) {
		let template = stui.str(x);
		if (!template) {
			return "";
		}
		return string.substitute(template, y);
	};
});

var stuidebug_enabled = (window.location.search.indexOf('&stui_debug=') > -1);

if (!stuidebug_enabled) {
	/**
	 * SurveyToolUI string loading function
	 */
	stui_str = function(x) {
		if (stui) {
			return stui.str(x);
		} else {
			return x;
		}
	};
} else {
	stui_str = function(x) {
		return "stui[" + x + "]";
	};
}

/**
 * Is the keyboard or input widget 'busy'? i.e., it's a bad time to change the DOM
 *
 * @return true if window.getSelection().anchorNode.className contains "dijitInp" or "popover-content",
 *		 else false
 *
 * "popover-content" identifies the little input window, created using bootstrap, that appears when the
 * user clicks an add ("+") button. Added "popover-content" per https://unicode.org/cldr/trac/ticket/11265.
 *
 * TODO: clarify dependence on "dijitInp"; is that still used here, and if so, when?
 * Add automated regression testing to anticipate future changes to bootstrap/dojo/dijit/etc.
 *
 * Called only from CldrSurveyVettingLoader.js
 */
function isInputBusy() {
	if (!window.getSelection) {
		return false;
	}
	var sel = window.getSelection();
	if (sel && sel.anchorNode && sel.anchorNode.className) {
		if (sel.anchorNode.className.indexOf("dijitInp") != -1) {
			return true;
		}
		if (sel.anchorNode.className.indexOf("popover-content") != -1) {
			return true;
		}
	}
	return false;
}

/**
 * Create a DOM object with the specified text, tag, and HTML class.
 * Applies (classname)+"_desc" as a tooltip (title).
 * TODO: clarify whether the "_desc" tooltip (title) is ever created, it doesn't appear to be.
 *
 * @param {String} text textual content of the new object, or null for none
 * @param {String} tag which element type to create, or null for "span"
 * @param {String} className CSS className, or null for none.
 * @return {Object} new DOM object
 */
function createChunk(text, tag, className) {
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
	if (!tag) {
		tag = 'a';
	}
	var msg = stui.str(strid);
	var obj = document.createElement(tag);
	obj.appendChild(document.createTextNode(msg));
	if (tag == 'a') {
		obj.href = '';
	}
	listenFor(obj, "click", function(e) {
		fn(obj);
		stStopPropagation(e);
		return false;
	});
	return obj;
}

function createGravitar(user) {
	if (user.emailHash) {
		var gravatar = document.createElement("img");
		gravatar.src = 'https://www.gravatar.com/avatar/' + user.emailHash + '?d=identicon&r=g&s=32';
		gravatar.title = 'gravatar - http://www.gravatar.com';
		return gravatar;
	} else {
		return document.createTextNode('');
	}
}

/**
 * Create a DOM object referring to a user.
 *
 * @param {JSON} user - user struct
 * @return {Object} new DOM object
 */
function createUser(user) {
	var userLevelLc = user.userlevelName.toLowerCase();
	var userLevelClass = "userlevel_" + userLevelLc;
	var userLevelStr = stui_str(userLevelClass);
	var div = createChunk(null, "div", "adminUserUser");
	div.appendChild(createGravitar(user));
	div.userLevel = createChunk(userLevelStr, "i", userLevelClass);
	div.appendChild(div.userLevel);
	div.appendChild(div.userName = createChunk(user.name, "span", "adminUserName"));
	if (!user.orgName) {
		user.orgName = user.org;
	}
	div.appendChild(div.userOrg = createChunk(user.orgName + ' #' + user.id, "span", "adminOrgName"));
	div.appendChild(div.userEmail = createChunk(user.email, "address", "adminUserAddress"));
	return div;
}

/**
 * Used from within event handlers. cross platform 'stop propagation'
 *
 * @param e event
 * @returns true or false
 *
 * Called from numerous js files
 */
function stStopPropagation(e) {
	if (!e) {
		return false;
	} else if (e.stopPropagation) {
		return e.stopPropagation();
	} else if (e.cancelBubble) {
		return e.cancelBubble();
	} else {
		// hope for the best
		return false;
	}
}

/**
 * Is the ST disconnected?
 *
 * @property disconnected
 *
 * TODO: move "disconnected" from global "window" namespace to our own object, and encapsulate
 * with getter and setter
 */
var disconnected = false;

/**
 * Is debugging enabled?
 *
 * @property stdebug_enabled
 */
var stdebug_enabled = (window.location.search.indexOf('&stdebug=') > -1);

function stdebug(x) {
	if (stdebug_enabled) {
		console.log(x);
	}
}

stdebug('stdebug is enabled.');

/**
 * Update the item, if it exists
 *
 * @param id ID of DOM node, or a Node itself
 * @param txt text to replace with - should just be plaintext, but currently can be HTML
 */
function updateIf(id, txt) {
	var something;
	if (id instanceof Node) {
		something = id;
	} else {
		something = document.getElementById(id);
	}
	if (something != null) {
		something.innerHTML = txt; // TODO shold only use for plain text
	}
}

/**
 * Add an event listener function to the object.
 *
 * @param {DOM} what object to listen to (or array of them)
 * @param {String} what event, bare name such as 'click'
 * @param {Function} fn function, of the form:  function(e) { 	doSomething();  	stStopPropagation(e);  	return false; }
 * @param {String} ievent IE name of an event, if not 'on'+what
 * @return {DOM} returns the object what (or the array)
 */
function listenFor(whatArray, event, fn, ievent) {
	function listenForOne(what, event, fn, ievent) {
		if (!(what._stlisteners)) {
			what._stlisteners = {};
		}

		if (what.addEventListener) {
			if (what._stlisteners[event]) {
				if (what.removeEventListener) {
					what.removeEventListener(event, what._stlisteners[event], false);
				} else {
					console.log("Err: no removeEventListener on " + what);
				}
			}
			what.addEventListener(event, fn, false);
		} else {
			if (!ievent) {
				ievent = "on" + event;
			}
			if (what._stlisteners[event]) {
				what.detachEvent(ievent, what._stlisteners[event]);
			}
			what.attachEvent(ievent, fn);
		}
		what._stlisteners[event] = fn;

		return what;
	}

	if (Array.isArray(whatArray)) {
		for (var k in whatArray) {
			listenForOne(whatArray[k], event, fn, ievent);
		}
		return whatArray;
	} else {
		return listenForOne(whatArray, event, fn, ievent);
	}
}

/**
 * On click, select all
 *
 * @param {Node} obj to listen to
 * @param {Node} targ to select
 */
function clickToSelect(obj, targ) {
	if (!targ) {
		targ = obj;
	}
	listenFor(obj, "click", function(e) {
		if (window.getSelection) {
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
	progressWord = "unbusted";
	disconnected = false;
	saidDisconnect = false;
	removeClass(document.getElementsByTagName("body")[0], "disconnected");
	wasBusted = false;
	cldrStAjax.clearXhr();
	hideLoader();
	saidDisconnect = false;
	updateStatus(); // will restart regular status updates
}

// hashtable of items already verified
var alreadyVerifyValue = {};

var showers={};

/**
 * Process that the locale has changed under us.
 *
 * @param {String} stamp timestamp
 * @param {String} name locale name
 */
function handleChangedLocaleStamp(stamp, name) {
	if (disconnected) {
		return;
	}
	if (stamp <= surveyNextLocaleStamp) {
		return;
	}
	/*
	 * For performance, postpone the all-row WHAT_GETROW update if multiple
	 * requests (e.g., vote or single-row WHAT_GETROW requests) are pending.
	 */
	if (cldrStAjax.queueCount() > 1) {
		return;
	}
	if (Object.keys(showers).length == 0) {
		/*
		 * TODO: explain this code. When, if ever, is it executed, and why?
		 * Typically Object.keys(showers).length != 0.
		 */
		updateIf('stchanged_loc', name);
		var locDiv = document.getElementById('stchanged');
		if (locDiv) {
			locDiv.style.display = 'block';
		}
	} else {
		for (i in showers) {
			var fn = showers[i];
			if (fn) {
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
 */
function showWord() {
	var p = document.getElementById("progress");
	var oneword = document.getElementById("progress_oneword");
	if (oneword == null) { // nowhere to show
		return;
	}
	if (disconnected ||
		(progressWord && progressWord == "disconnected") ||
		(progressWord && progressWord == "error")
	) { // top priority
		popupAlert('danger', stopIcon + stui_str(progressWord));
		busted(); // no further processing.
	} else if (ajaxWord) {
		p.className = "progress-ok";
	} else if (!progressWord || progressWord == "ok") {
		if (specialHeader) {
			p.className = "progress-special";
		} else {
			p.className = "progress-ok";
		}
	} else if (progressWord == "startup") {
		p.className = "progress-ok";
		popupAlert('warning', stui_str('online'));
	}
}

/**
 * Update our progress
 *
 * @param {String} prog the status to update
 */
function updateProgressWord(prog) {
	progressWord = prog;
	showWord();
}

/**
 * Update ajax loading status
 *
 * @param {String} ajax
 */
function updateAjaxWord(ajax) {
	ajaxWord = ajax;
	showWord();
}

var saidDisconnect = false;

/**
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
 *
 * @param why
 * @param json
 * @param word
 * @param what - what we were doing
 */
function handleDisconnect(why, json, word, what) {
	if (json && (json.err_code === 'E_NOT_LOGGED_IN')) {
		window.location = 'login.jsp?operationFailed' + window.location.hash;
		return;
	}
	if (!what) {
		what = "unknown";
	}
	if (!word) {
		word = "error"; // assume it's an error except for a couple of cases.
	}
	updateProgressWord(word);
	if (!saidDisconnect) {
		saidDisconnect = true;
		if (json && json.err) {
			why = why + "\n The error message was: \n" + json.err;
			if (json.err.fileName) {
				why = why + "\nFile: " + json.err.fileName;
				if (json.err.lineNumber) {
					why = why + "\nLine: " + json.err.lineNumber;
				}
			}
		}
		console.log("Disconnect: " + why);
		var oneword = document.getElementById("progress_oneword");
		if (oneword) {
			oneword.title = "Disconnected: " + why;
			oneword.onclick = function() {
				var p = document.getElementById("progress");
				var subDiv = document.createElement('div');
				var chunk0 = document.createElement("i");
				chunk0.appendChild(document.createTextNode(stui_str("error_restart")));
				var chunk = document.createElement("textarea");
				chunk.className = "errorMessage";
				chunk.appendChild(document.createTextNode(why));
				chunk.rows = "10";
				chunk.cols = "40";
				subDiv.appendChild(chunk0);
				subDiv.appendChild(chunk);
				p.appendChild(subDiv);
				if (oneword.details) {
					setDisplayed(oneword.details, false);
				}
				oneword.onclick = null;
				return false;
			};
			var p = document.getElementById("progress");
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
		if (json) {
			stdebug("JSON: " + json.toString());
		}
	}
}

var updateParts = null;

/*
 * TODO: Avoid browser console showing "ReferenceError: surveyRunningStamp is not defined" here.
 * surveyRunningStamp is undefined unless ajax_status.jsp is included.
 * submit.jsp (or SurveyAjax.handleBulkSubmit) includes survey.js but not ajax_status.jsp.
 */
var cacheKillStamp = surveyRunningStamp;

/**
 * Return a string to be used with a URL to avoid caching. Ignored by the server.
 *
 * @returns {String} the URL fragment, append to the query
 */
function cacheKill() {
	if (!cacheKillStamp || cacheKillStamp < surveyRunningStamp) {
		cacheKillStamp = surveyRunningStamp;
	}
	cacheKillStamp++;

	return "&cacheKill=" + cacheKillStamp;
}

/**
 * Note that there is special site news.
 *
 * @param {String} newSpecialHeader site news
 */
function updateSpecialHeader(newSpecialHeader) {
	if (newSpecialHeader && newSpecialHeader.length > 0) {
		specialHeader = newSpecialHeader;
	} else {
		specialHeader = null;
	}
	showWord();
}

function trySurveyLoad() {
	try {
		var url = contextPath + "/survey?" + cacheKill();
		console.log("Attempting to restart ST at " + url);
		cldrStAjax.sendXhr({
			url: url,
			timeout: ajaxTimeout
		});
	} catch (e) {}
}

var lastJsonStatus = null;

/*
 * TODO: formatErrMsg is called only in CldrSurveyVettingLoader.js, so move it there
 */
function formatErrMsg(json, subkey) {
	if (!subkey) {
		subkey = "unknown";
	}
	var theCode = "E_UNKNOWN";
	if (json && json.session_err) {
		theCode = "E_SESSION_DISCONNECTED";
	}
	var msg_str = theCode;
	if (json && json.err_code) {
		msg_str = theCode = json.err_code;
		if (stui.str(json.err_code) == json.err_code) {
			console.log("** Unknown error code: " + json.err_code);
			msg_str = "E_UNKNOWN";
		}
	}
	if (json === null) {
		json = {}; // handle cases with no input data
	}
	return stui.sub(msg_str, {
		/* Possibilities include: err_what_section, err_what_locmap, err_what_menus,
			err_what_status, err_what_unknown, err_what_oldvotes, err_what_vote */
		json: json,
		what: stui.str('err_what_' + subkey),
		code: theCode,
		err_data: json.err_data,
		surveyCurrentLocale: surveyCurrentLocale,
		surveyCurrentId: surveyCurrentId,
		surveyCurrentSection: surveyCurrentSection,
		surveyCurrentPage: surveyCurrentPage
	});
}

/**
 * Based on the last received packet of JSON, update our status
 *
 * @param {Object} json received
 */
function updateStatusBox(json) {
	if (json.disconnected) {
		json.err_code = 'E_DISCONNECTED';
		handleDisconnect("Misc Disconnect", json, "disconnected"); // unknown
	} else if (json.err_code) {
		console.log('json.err_code == ' + json.err_code);
		if (json.err_code == "E_NOT_STARTED") {
			trySurveyLoad();
		}
		handleDisconnect(json.err_code, json, "disconnected", "status");
	} else if (json.SurveyOK == 0) {
		console.log('json.surveyOK==0');
		trySurveyLoad();
		handleDisconnect("The SurveyTool server is not ready to accept connections, please retry. ", json, "disconnected"); // ST has restarted
	} else if (json.status && json.status.isBusted) {
		handleDisconnect("The SurveyTool server has halted due to an error: " + json.status.isBusted, json, "disconnected"); // Server down- not our fault. Hopefully.
	} else if (!json.status) {
		handleDisconnect("The SurveyTool erver returned a bad status", json);
	} else if (json.status.surveyRunningStamp != surveyRunningStamp) {
		handleDisconnect("The SurveyTool server restarted since this page was loaded. Please retry.", json, "disconnected"); // desync
	} else if (json.status && json.status.isSetup == false && json.SurveyOK == 1) {
		updateProgressWord("startup");
	} else {
		updateProgressWord("ok");
	}

	if (json.status) {
		lastJsonStatus = json.status;
		if (!updateParts) {
			var visitors = document.getElementById("visitors");
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
		if (json.status.guests > 0) {
			ugtext = ugtext + (json.status.guests) + " guests, ";
		}
		ugtext = ugtext + (json.status.pages) + "pg/" + json.status.uptime;
		removeAllChildNodes(updateParts.ug);
		updateParts.ug.appendChild(document.createTextNode(ugtext));

		removeAllChildNodes(updateParts.load);
		updateParts.load.appendChild(document.createTextNode("Load:" + json.status.sysload));

		removeAllChildNodes(updateParts.db);
		updateParts.db.appendChild(document.createTextNode("db:" + json.status.dbopen + "/" + json.status.dbused));

		var fragment = document.createDocumentFragment();
		fragment.appendChild(updateParts.ug);
		fragment.appendChild(document.createTextNode(" "));
		fragment.appendChild(updateParts.load);
		fragment.appendChild(document.createTextNode(" "));
		fragment.appendChild(updateParts.db);

		if (updateParts.visitors) {
			removeAllChildNodes(updateParts.visitors);
			updateParts.visitors.appendChild(fragment);
		}

		function standOutMessage(txt) {
			return "<b style='font-size: x-large; color: red;'>" + txt + "</b>";
		}

		if (window.kickMe) {
			json.millisTillKick = 0;
		} else if (window.kickMeSoon) {
			json.millisTillKick = 5000;
		}

		// really don't care if guest user gets 'kicked'. Doesn't matter.
		if ((surveyUser !== null) && json.millisTillKick && (json.millisTillKick >= 0) && (json.millisTillKick < (60 * 1 * 1000))) { // show countdown when 1 minute to go
			var kmsg = "Your session will end if not active in about " + (parseInt(json.millisTillKick) / 1000).toFixed(0) + " seconds.";
			console.log(kmsg);
			updateSpecialHeader(standOutMessage(kmsg));
		} else if ((surveyUser !== null) && ((json.millisTillKick === 0) || (json.session_err))) {
			var kmsg = stui_str("ari_sessiondisconnect_message");
			console.log(kmsg);
			updateSpecialHeader(standOutMessage(kmsg));
			disconnected = true;
			addClass(document.getElementsByTagName("body")[0], "disconnected");
			if (!json.session_err) {
				json.session_err = "disconnected";
			}
			handleDisconnect(kmsg, json, "Your session has been disconnected.");
		} else if (json.status.specialHeader && json.status.specialHeader.length > 0) {
			updateSpecialHeader(json.status.specialHeader);
		} else {
			updateSpecialHeader(null);
		}
	}
}

/**
 * How often to fetch updates. Default 15s.
 * Used only for delay in calling updateStatus.
 * May be changed by resetTimerSpeed -- but resetTimerSpeed is NEVER called
 * @property timerSpeed
 */
var timerSpeed = 15000; // 15 seconds

/**
 * How long to wait for AJAX updates.
 * @property ajaxTimeout
 */
var ajaxTimeout = 120000; // 2 minutes

var surveyVersion = 'Current';

/**
 * This is called periodically to fetch latest ST status
 */
function updateStatus() {
	if (disconnected) {
		stdebug("Not updating status - disconnected.");
		return;
	}

	var surveyLocaleUrl = '';
	var surveySessionUrl = '';
	if (surveyCurrentLocale !== null && surveyCurrentLocale != '') {
		surveyLocaleUrl = '&_=' + surveyCurrentLocale;
	}
	if (surveySessionId && surveySessionId !== null) {
		surveySessionUrl = '&s=' + surveySessionId;
	}

	cldrStAjax.sendXhr({
		url: contextPath + "/SurveyAjax?what=status" + surveyLocaleUrl + surveySessionUrl + cacheKill(),
		handleAs: "json",
		timeout: ajaxTimeout,
		load: function(json) {
			if ((json == null) || (json.status && json.status.isBusted)) {
				wasBusted = true;
				busted();
				return; // don't thrash
			}
			var st_err = document.getElementById('st_err');
			if (!st_err) {
				/*
				 * This happens if updateStatus is called for a page like about.jsp, browse.jsp;
				 * it shouldn't be called in such cases.
				 */
				return;
			}
			if (json.err != null && json.err.length > 0) {
				st_err.innerHTML = json.err;
				if (json.status && json.status.surveyRunningStamp != surveyRunningStamp) {
					st_err.innerHTML = st_err.innerHTML + " <b>Note: Lost connection with Survey Tool or it restarted.</b>";
					updateStatusBox({
						disconnected: true
					});
				}
				st_err.className = "ferrbox";
				wasBusted = true;
				busted();
			} else {
				if (json.status.newVersion) {
					surveyVersion = json.status.newVersion;
				}
				if (json.status.surveyRunningStamp != surveyRunningStamp) {
					st_err.className = "ferrbox";
					st_err.innerHTML = "The SurveyTool has been restarted. Please reload this page to continue.";
					wasBusted = true;
					busted();
					// TODO: show ARI for reconnecting
				} else if (wasBusted == true &&
					(!json.status.isBusted) ||
					(json.status.surveyRunningStamp != surveyRunningStamp)) {
					st_err.innerHTML = "Note: Lost connection with Survey Tool or it restarted.";
					if (clickContinue != null) {
						st_err.innerHTML = st_err.innerHTML + " Please <a href='" + clickContinue + "'>click here</a> to continue.";
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

			if (json.localeStamp) {
				if (surveyNextLocaleStamp == 0) {
					surveyNextLocaleStamp = json.localeStamp;
					stdebug("STATUS0: " + json.localeStampName + "=" + json.localeStamp);
				} else {
					if (json.localeStamp > surveyNextLocaleStamp) {
						stdebug("STATUS=: " + json.localeStampName + "=" + json.localeStamp + " > " + surveyNextLocaleStamp);
						handleChangedLocaleStamp(json.localeStamp, json.localeStampName);
					} else {
						stdebug("STATUS=: " + json.localeStampName + "=" + json.localeStamp + " <= " + surveyNextLocaleStamp);
					}
				}
			}

			if ((wasBusted == false) && (json.status.isSetup) && (loadOnOk != null)) {
				window.location.replace(loadOnOk);
			} else {
				setTimeout(updateStatus, timerSpeed);
			}
		},
		error: function(err) {
			wasBusted = true;
			updateStatusBox({
				err: err.message,
				err_name: err.name,
				disconnected: true
			});
		}
	});
}

/**
 * Fire up the main timer loop to update status
 */
function setTimerOn() {
	updateStatus();
	// an interval is not used - each status update does its own timeout.
}

/**
 * Change the update timer's speed
 *
 * @param {Int} speed
 * TODO: this is never called?!
 */
function resetTimerSpeed(speed) {
	timerSpeed = speed;
}

// set up window. Let Dojo call us, otherwise dojo won't load.
require(["dojo/ready"], function(ready) {
	ready(function() {
		let name = window.location.pathname;
		if (name.includes('about.jsp') || name.includes('browse.jsp')) {
			/*
			 * Skip timer for about.jsp and browse.jsp; calling updateStatus for
			 * those pages would needlessly waste time on the server.
			 */
		} else {
			setTimerOn();
		}
	});
});

/**
 * Table mapping CheckCLDR.StatusAction into capabilities
 * @property statusActionTable
 */
var statusActionTable = {
	ALLOW: {
		vote: true,
		ticket: false,
		change: true
	},
	ALLOW_VOTING_AND_TICKET: {
		vote: true,
		ticket: true,
		change: false
	},
	ALLOW_VOTING_BUT_NO_ADD: {
		vote: true,
		ticket: false,
		change: false
	},
	ALLOW_TICKET_ONLY: {
		vote: false,
		ticket: true,
		change: true
	},
	DEFAULT: {
		vote: false,
		ticket: false,
		change: false
	}
};

/**
 * Parse a CheckCLDR.StatusAction and return the capabilities table
 *
 * @param action
 * @returns {Object} capabilities
 */
function parseStatusAction(action) {
	if (!action) {
		return statusActionTable.DEFAULT;
	}
	var result = statusActionTable[action];
	if (!result) {
		result = statusActionTable.DEFAULT;
	}
	return result;
}

/**
 * Determine whether a JSONified array of CheckCLDR.CheckStatus is overall a warning or an error.
 *
 * @param {Object} testResults - array of CheckCLDR.CheckStatus
 * @returns {String} 'Warning' or 'Error' or null
 *
 * Note: when a user votes, the response to the POST request includes json.testResults,
 * which often (always?) includes a warning "Needed to meet ... coverage level", possibly
 * in addition to other warnings or errors. We then return Warning, resulting in
 * div.className = "d-item-warn" and a temporary yellow background for the cell, which, however,
 * goes back to normal color (with "d-item") after a multiple-row response is received.
 * The message "Needed to meet ..." may not actually be displayed, in which case the yellow
 * background is distracting; its purpose should be clarified.
 */
function getTestKind(testResults) {
	if (!testResults) {
		return null;
	}
	var theKind = null;
	for (var i = 0; i < testResults.length; i++) {
		var tr = testResults[i];
		if (tr.type == 'Warning') {
			theKind = tr.type;
		} else if (tr.type == 'Error') {
			return tr.type;
		}
	}
	return theKind;
}

/**
 * Clone the node, removing the id
 *
 * @param {Node} i
 * @returns {Node} new return, deep clone but with no ids
 */
function cloneAnon(i) {
	if (i == null) {
		return null;
	}
	var o = i.cloneNode(true);
	if (o.id) {
		o.removeAttribute('id');
	}
	return o;
}

/**
 * like cloneAnon, but localizes by fetching stui-html string substitution.
 *
 * @param o
 */
function localizeAnon(o) {
	loadStui(null, function(stui) {
		if (o && o.childNodes) {
			for (var i = 0; i < o.childNodes.length; i++) {
				var k = o.childNodes[i];
				if (k.id && k.id.indexOf("stui-html") == 0) {
					var key = k.id.slice(5);
					if (stui.str(key)) {
						k.innerHTML = stui.str(key);
					}
					k.removeAttribute('id');
				} else {
					localizeAnon(k);
				}
			}
		}
	});
}

/**
 * Localize the flyover text by replacing $X with stui[Z]
 *
 * @param {Node} o
 */
function localizeFlyover(o) {
	if (o && o.childNodes) {
		for (var i = 0; i < o.childNodes.length; i++) {
			var k = o.childNodes[i];
			if (k.title && k.title.indexOf("$") == 0) {
				var key = k.title.slice(1);
				if (stui.str(key)) {
					k.title = stui.str(key);
				} else {
					k.title = null;
				}
			} else {
				localizeFlyover(k);
			}
		}
	}
}

/**
 * cloneAnon, then call localizeAnon
 *
 * @param {Node} i
 * @returns {Node}
 */
function cloneLocalizeAnon(i) {
	var o = cloneAnon(i);
	if (o) {
		localizeAnon(o);
	}
	return o;
}

/**
 * Return an array of all children of the item which are tags
 *
 * @param {Node} tr
 * @returns {Array}
 */
function getTagChildren(tr) {
	var rowChildren = [];
	for (var k in tr.childNodes) {
		var t = tr.childNodes[k];
		if (t.tagName) {
			rowChildren.push(t);
		}
	}
	return rowChildren;
}

/**
 * Show the 'loading' sign
 *
 * @param loaderDiv ignored
 * @param {String} text text to use
 */
function showLoader(loaderDiv, text) {
	updateAjaxWord(text);
}

/**
 * Hide the 'loading' sign
 *
 * @param loaderDiv ignored
 */
function hideLoader(loaderDiv) {
	updateAjaxWord(null);
}

/**
 * Wire up the button to perform a submit
 *
 * @param button
 * @param tr
 * @param theRow
 * @param vHash
 * @param box
 */
function wireUpButton(button, tr, theRow, vHash, box) {
	if (box) {
		button.id = "CHANGE_" + tr.rowHash;
		vHash = "";
		box.onchange = function() {
			handleWiredClick(tr, theRow, vHash, box, button, 'submit');
			return false;
		};
		box.onkeypress = function(e) {
			if (!e || !e.keyCode) {
				return true; // not getting the point here.
			} else if (e.keyCode == 13) {
				handleWiredClick(tr, theRow, vHash, box, button);
				return false;
			} else {
				return true;
			}
		};
	} else if (vHash == null) {
		button.id = "NO_" + tr.rowHash;
		vHash = "";
	} else {
		button.id = "v" + vHash + "_" + tr.rowHash;
	}
	listenFor(button, "click",
		function(e) {
			handleWiredClick(tr, theRow, vHash, box, button);
			stStopPropagation(e);
			return false;
		});

	// proposal issues
	if (tr.myProposal) {
		if (button == tr.myProposal.button) {
			button.className = "ichoice-x";
			button.checked = true;
			tr.lastOn = button;
		} else {
			button.className = "ichoice-o";
			button.checked = false;
		}
	} else if ((theRow.voteVhash == vHash) && !box) {
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
 *
 * @param {Node} td
 * @Param {String} className name of icon's CSS class
 */
function addIcon(td, className) {
	var star = document.createElement("span");
	star.className = className;
	star.innerHTML = "&nbsp; &nbsp;";
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
 * This is the actual function is called to display the right-hand "info" panel.
 * It is defined dynamically because it depends on variables that aren't available at startup time.
 *
 * @param {String} str the string to show at the top
 * @param {Node} tr the <TR> of the row
 * @param {Boolean} hideIfLast
 * @param {Function} fn
 * @param {Boolean} immediate
 */
function showInPop(str, tr, theObj, fn, immediate) {}

/**
 * Make the object "theObj" cause the infowindow to show when clicked.
 *
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

/**
 * Timeout for showing sideways view
 */
var sidewaysShowTimeout = -1;

/**
 *  Array storing all only-1 sublocale
 */
var oneLocales = [];

/**
 * Called when showing the popup each time
 *
 * @param {Node} frag
 * @param {Node} forumDivClone = tr.forumDiv.cloneNode(true)
 * @param {Node} tr
 *
 * TODO: shorten this function
 */
function showForumStuff(frag, forumDivClone, tr) {
	var isOneLocale = false;
	if (oneLocales[surveyCurrentLocale]) {
		isOneLocale = true;
	}
	if (!isOneLocale) {
		var sidewaysControl = createChunk(stui.str("sideways_loading0"), "div", "sidewaysArea");
		frag.appendChild(sidewaysControl);

		function clearMyTimeout() {
			if (sidewaysShowTimeout != -1) {
				window.clearInterval(sidewaysShowTimeout);
				sidewaysShowTimeout = -1;
			}
		}
		clearMyTimeout();
		sidewaysShowTimeout = window.setTimeout(function() {
			clearMyTimeout();
			updateIf(sidewaysControl, stui.str("sideways_loading1"));

			var url = contextPath + "/SurveyAjax?what=getsideways&_=" + surveyCurrentLocale + "&s=" + surveySessionId + "&xpath=" + tr.theRow.xpstrid + cacheKill();
			myLoad(url, "sidewaysView", function(json) {
				/*
				 * Count the number of unique locales in json.others and json.novalue.
				 */
				var relatedLocales = json.novalue.slice();
				for (var s in json.others) {
					for (var t in json.others[s]) {
						relatedLocales[json.others[s][t]] = true;
					}
				}
				// if there is 1 sublocale (+ 1 default), we do nothing
				if (Object.keys(relatedLocales).length <= 2) {
					oneLocales[surveyCurrentLocale] = true;
					updateIf(sidewaysControl, "");
				} else {
					if (!json.others) {
						updateIf(sidewaysControl, ""); // no sibling locales (or all null?)
					} else {
						updateIf(sidewaysControl, ""); // remove string

						var topLocale = json.topLocale;
						var curLocale = locmap.getRegionAndOrVariantName(topLocale);
						var readLocale = null;

						// merge the read-only sublocale to base locale
						var mergeReadBase = function mergeReadBase(list) {
							var baseValue = null;
							// find the base locale, remove it and store its value
							for (var l = 0; l < list.length; l++) {
								var loc = list[l][0];
								if (loc === topLocale) {
									baseValue = list[l][1];
									list.splice(l, 1);
									break;
								}
							}

							// replace the default locale(read-only) with base locale, store its name for label
							for (var l = 0; l < list.length; l++) {
								var loc = list[l][0];
								var bund = locmap.getLocaleInfo(loc);
								if (bund && bund.readonly) {
									readLocale = locmap.getRegionAndOrVariantName(loc);
									list[l][0] = topLocale;
									list[l][1] = baseValue;
									break;
								}
							}
						}

						// compare all sublocale values
						var appendLocaleList = function appendLocaleList(list, curValue) {
							var group = document.createElement("optGroup");
							var br = document.createElement("optGroup");
							group.appendChild(br);

							group.setAttribute("label", "Regional Variants for " + curLocale);
							group.setAttribute("title", "Regional Variants for " + curLocale);

							var escape = "\u00A0\u00A0\u00A0";
							var unequalSign = "\u2260\u00A0";

							for (var l = 0; l < list.length; l++) {
								var loc = list[l][0];
								var title = list[l][1];
								var item = document.createElement("option");
								item.setAttribute("value", loc);
								if (title == null) {
									item.setAttribute("title", "undefined");
								} else {
									item.setAttribute("title", title);
								}

								var str = locmap.getRegionAndOrVariantName(loc);
								if (loc === topLocale) {
									str = str + " (= " + readLocale + ")";
								}

								if (loc === surveyCurrentLocale) {
									str = escape + str;
									item.setAttribute("selected", "selected");
									item.setAttribute("disabled", "disabled");
								} else if (title != curValue) {
									str = unequalSign + str;
								} else {
									str = escape + str;
								}
								item.appendChild(document.createTextNode(str));
								group.appendChild(item);
							}
							popupSelect.appendChild(group);
						};

						var dataList = [];

						var popupSelect = document.createElement("select");
						for (var s in json.others) {
							for (var t in json.others[s]) {
								dataList.push([json.others[s][t], s]);
							}
						}

						/*
						 * Set curValue = the value for surveyCurrentLocale
						 */
						var curValue = null;
						for (var l = 0; l < dataList.length; l++) {
							var loc = dataList[l][0];
							if (loc === surveyCurrentLocale) {
								curValue = dataList[l][1];
								break;
							}
						}
						/*
						 * Force the use of unequalSign in the regional comparison pop-up for locales in
						 * json.novalue, by assigning a value that's different from curValue.
						 *
						 * Formerly the inherited value (based on topLocale) was used here; that was a bug.
						 * If the server doesn't know the winning value, then the client shouldn't pretend to know.
						 * The server code has been fixed to resolve most such cases.
						 *
						 * Reference: https://unicode.org/cldr/trac/ticket/11688
						 */
						if (json.novalue) {
							const differentValue = (curValue === 'A') ? 'B' : 'A'; // anything different from curValue
							for (s in json.novalue) {
								dataList.push([json.novalue[s], differentValue]);
							}
						}
						mergeReadBase(dataList);

						// then sort by sublocale name
						dataList = dataList.sort(function(a, b) {
							return locmap.getRegionAndOrVariantName(a[0]) > locmap.getRegionAndOrVariantName(b[0]);
						});
						appendLocaleList(dataList, curValue);

						var group = document.createElement("optGroup");
						popupSelect.appendChild(group);

						listenFor(popupSelect, "change", function(e) {
							var newLoc = popupSelect.value;
							if (newLoc !== surveyCurrentLocale) {
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

	if (tr.theRow) {
		const theRow = tr.theRow;
		const couldFlag = theRow.canFlagOnLosing &&
				theRow.voteVhash !== theRow.winningVhash &&
				theRow.voteVhash !== '' &&
				!theRow.rowFlagged;
		const myValue = theRow.hasVoted ? getUsersValue(theRow) : null;
		cldrStForum.addNewPostButtons(frag, surveyCurrentLocale, couldFlag, theRow.xpstrid, theRow.code, myValue);
	}

	var loader2 = createChunk(stui.str("loading"), "i");
	frag.appendChild(loader2);

	/**
	 * @param {Integer} nrPosts
	 */
	function havePosts(nrPosts) {
		setDisplayed(loader2, false); // not needed
		tr.forumDiv.forumPosts = nrPosts;

		if (nrPosts == 0) {
			return; // nothing to do,
		}

		var showButton = createChunk("Show " + tr.forumDiv.forumPosts + " posts", "button", "forumShow");

		forumDivClone.appendChild(showButton);

		var theListen = function(e) {
			setDisplayed(showButton, false);
			updateInfoPanelForumPosts(tr);
			stStopPropagation(e);
			return false;
		};
		listenFor(showButton, "click", theListen);
		listenFor(showButton, "mouseover", theListen);
	}

	// lazy load post count!
	// load async
	var ourUrl = tr.forumDiv.url + "&what=forum_count" + cacheKill();
	window.setTimeout(function() {
		var xhrArgs = {
			url: ourUrl,
			handleAs: "json",
			load: function(json) {
				if (json && json.forum_count !== undefined) {
					havePosts(parseInt(json.forum_count));
				} else {
					console.log("Some error loading post count??");
				}
			},
		};
		cldrStAjax.queueXhr(xhrArgs);
	}, 1900);

	function getUsersValue(theRow) {
		'use strict';
		if (surveyUser && surveyUser.id) {
			if (theRow.voteVhash && theRow.voteVhash !== '') {
				const item = theRow.items[theRow.voteVhash];
				if (item && item.votes && item.votes[surveyUser.id]) {
					if (item.value === INHERITANCE_MARKER) {
						return theRow.inheritedValue;
					}
					return item.value;
				}
			}
		}
		return null;
	}
}

/**
 * Update the forum posts in the Info Panel
 *
 * This includes the version of the Info Panel displayed in the Dashboard "Fix" window
 *
 * @param tr the table-row element with which the forum posts are associated,
 *		and whose info is shown in the Info Panel; or null, to get the
 *		tr from surveyCurrentId
 */
function updateInfoPanelForumPosts(tr) {
	if (!tr) {
		if (surveyCurrentId !== '') {
			/*
			 * TODO: encapsulate this usage of 'r@' somewhere
			 */
			tr = document.getElementById('r@' + surveyCurrentId);
		} else {
			/*
			 * This is normal when adding a post in the main forum interface, which has no Info Panel).
			 */
			return;
		}
	}
	if (!tr || !tr.forumDiv || !tr.forumDiv.url) {
		/*
		 * This is normal for updateInfoPanelForumPosts(null) called by success handler
		 * for submitPost, from Dashboard, since Fix window is no longer open
		 */
		return;
	}
	let ourUrl = tr.forumDiv.url + "&what=forum_fetch";

	let errorHandler = function(err) {
		let responseText = cldrStAjax.errResponseText(err);
		console.log('Error in showForumStuff: ' + err + ' response ' + responseText);
		showInPop(stopIcon +
			" Couldn't load forum post for this row- please refresh the page. <br>Error: " +
			err + "</td>", tr, null);
		handleDisconnect("Could not showForumStuff:" + err, null);
	};

	let loadHandler = function(json) {
		try {
			if (json && json.ret) {
				const posts = json.ret;
				const content = cldrStForum.parseContent(posts, 'info');
				/*
				 * Reality check: the json should refer to the same path as tr, which in practice
				 * always matches surveyCurrentId. If not, log a warning and substitute "Please reload"
				 * for the content.
				 */
				let xpstrid = posts[0].xpath;
				if (xpstrid !== tr.xpstrid || xpstrid !== surveyCurrentId) {
					console.log('Warning: xpath strid mismatch in updateInfoPanelForumPosts loadHandler:');
					console.log('posts[0].xpath = ' + posts[0].xpath);
					console.log('tr.xpstrid = ' + tr.xpstrid);
					console.log('surveyCurrentId = ' + surveyCurrentId);

					content = "Please reload";
				}
				/*
				 * Update the element whose class is 'forumDiv'.
				 * Note: When updateInfoPanelForumPosts is called by the mouseover event handler for
				 * the "Show n posts" button set up by havePosts, a clone of tr.forumDiv is created
				 * (for mysterious reasons) by that event handler, and we could pass forumDivClone
				 * as a parameter to updateInfoPanelForumPosts, then do forumDivClone.appendChild(content)
				 * here, which is essentially how it formerly worked. However, that wouldn't work when
				 * we're called by the success handler for submitPost. This works in all cases.
				 */
				$('.forumDiv').first().html(content);
			}
		} catch (e) {
			console.log("Error in ajax forum read ", e.message);
			console.log(" response: " + json);
			showInPop(stopIcon + " exception in ajax forum read: " + e.message, tr, null, true);
		}
	};

	let xhrArgs = {
		url: ourUrl,
		handleAs: "json",
		load: loadHandler,
		error: errorHandler
	};
	cldrStAjax.queueXhr(xhrArgs);
}

/**
 * Called when initially setting up the section.
 *
 * @param {Node} tr
 * @param {Node} theRow
 * @param {Node} forumDiv
 */
function appendForumStuff(tr, theRow, forumDiv) {

	cldrStForum.setUserCanPost(tr.theTable.json.canModify);

	removeAllChildNodes(forumDiv); // we may be updating.
	var theForum = locmap.getLanguage(surveyCurrentLocale);
	forumDiv.replyStub = contextPath + "/survey?forum=" + theForum + "&_=" + surveyCurrentLocale + "&replyto=";
	forumDiv.postUrl = forumDiv.replyStub + "x" + theRow;
	/*
	 * Note: SurveyAjax requires a "what" parameter for SurveyAjax.
	 * It is not supplied here, but may be added later with code such as:
	 *	var ourUrl = tr.forumDiv.url + "&what=forum_count" + cacheKill() ;
	 *	var ourUrl = tr.forumDiv.url + "&what=forum_fetch";
	 * Unfortunately that means "what" is not the first argument, as it would
	 * be ideally for human readability of request urls.
	 */
	forumDiv.url = contextPath + "/SurveyAjax?xpath=" + theRow.xpathId + "&_=" + surveyCurrentLocale + "&fhash=" +
		theRow.rowHash + "&vhash=" + "&s=" + tr.theTable.session +
		"&voteinfo=t";
}

/**
 * Change the current id
 *
 * @param id the id to set
 */
window.updateCurrentId = function updateCurrentId(id) {
	if (id == null) {
		id = '';
	}
	if (surveyCurrentId != id) { // don't set if already set.
		surveyCurrentId = id;
	}
};

// window loader stuff
require(["dojo/ready"], function(ready) {
	ready(function() {
		var unShow = null;
		var pucontent = document.getElementById("itemInfo");
		if (!pucontent) {
			return;
		}

		var hideInterval = null;

		function parentOfType(tag, obj) {
			if (!obj) return null;
			if (obj.nodeName === tag) return obj;
			return parentOfType(tag, obj.parentElement);
		}

		function setLastShown(obj) {
			if (gPopStatus.lastShown && obj != gPopStatus.lastShown) {
				removeClass(gPopStatus.lastShown, "pu-select");
				var partr = parentOfType('TR', gPopStatus.lastShown);
				if (partr) {
					removeClass(partr, 'selectShow');
				}
			}
			if (obj) {
				addClass(obj, "pu-select");
				var partr = parentOfType('TR', obj);
				if (partr) {
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
		 * This is the actual function called to display the right-hand "info" panel.
		 * It is defined dynamically because it depends on variables that aren't available at startup time.
		 *
		 * @param {String} str the string to show at the top
		 * @param {Node} tr the <TR> of the row
		 * @param {Boolean} hideIfLast
		 * @param {Function} fn
		 * @param {Boolean} immediate
		 * @returns {Node} a reference to the right hand panel, if not in Dashboard mode
		 */
		window.showInPop2 = function(str, tr, hideIfLast, fn, immediate, hide) {
			if (unShow) {
				unShow();
				unShow = null;
			}
			incrPopToken('newShow' + str);
			if (hideInterval) {
				clearTimeout(hideInterval);
				hideInterval = null;
			}

			if (tr && tr.sethash) {
				window.updateCurrentId(tr.sethash);
			}
			setLastShown(hideIfLast);

			/*
			 * This is the temporary fragment used for the
			 * "info panel" contents. 
			 */
			var fragment = document.createDocumentFragment();

			// Always have help (if available).
			var theHelp = null;
			if (tr) {
				var theRow = tr.theRow;
				// this also marks this row as a 'help parent'
				theHelp = createChunk("", "div", "alert alert-info fix-popover-help vote-help");

				if (theRow.xpstrid) {
					var deferHelpSpan = document.createElement('span');
					theHelp.appendChild(deferHelpSpan);

					if (deferHelp[theRow.xpstrid]) {
						deferHelpSpan.innerHTML = deferHelp[theRow.xpstrid];
					} else {
						deferHelpSpan.innerHTML = "<i>" + stui.str("loading") + "</i>";

						// load async
						var url = contextPath + "/help?xpstrid=" + theRow.xpstrid + "&_instance=" + surveyRunningStamp;
						var xhrArgs = {
							url: url,
							handleAs: "text",
							load: function(html) {
								deferHelp[theRow.xpstrid] = html;
								deferHelpSpan.innerHTML = html;
								if (isDashboard()) {
									fixPopoverVotePos();
								}
							},
						};
						cldrStAjax.queueXhr(xhrArgs);
						// loader.
					}

					// extra attributes
					if (theRow.extraAttributes && Object.keys(theRow.extraAttributes).length > 0) {
						var extraHeading = createChunk(stui.str("extraAttribute_heading"), "h3", "extraAttribute_heading");
						var extraContainer = createChunk("", "div", "extraAttributes");
						appendExtraAttributes(extraContainer, theRow);
						theHelp.appendChild(extraHeading);
						theHelp.appendChild(extraContainer);
					}
				}
			}
			if (theHelp) {
				fragment.appendChild(theHelp);
			}

			if (str) { // If a simple string, clone the string
				var div2 = document.createElement("div");
				div2.innerHTML = str;
				fragment.appendChild(div2);
			}
			// If a generator fn (common case), call it.
			if (fn != null) {
				unShow = fn(fragment);

			}

			var theVoteinfo = null;
			if (tr && tr.voteDiv) {
				theVoteinfo = tr.voteDiv;
			}
			if (theVoteinfo) {
				fragment.appendChild(theVoteinfo.cloneNode(true));
			}
			if (tr && tr.ticketLink) {
				fragment.appendChild(tr.ticketLink.cloneNode(true));
			}

			// forum stuff
			if (tr && tr.forumDiv) {
				/*
				 * The name forumDivClone is a reminder that forumDivClone !== tr.forumDiv.
				 * TODO: explain the reason for using cloneNode here, rather than using
				 * tr.forumDiv directly. Would it work as well to set tr.forumDiv = forumDivClone,
				 * after cloning?
				 */
				var forumDivClone = tr.forumDiv.cloneNode(true);
				showForumStuff(fragment, forumDivClone, tr); // give a chance to update anything else
				fragment.appendChild(forumDivClone);
			}

			if (tr && tr.theRow && tr.theRow.xpath) {
				fragment.appendChild(clickToSelect(createChunk(tr.theRow.xpath, "div", "xpath")));
			}

			// Now, copy or append the 'fragment' to the
			// appropriate spot. This depends on how we were called.
			if (tr) {
				if (isDashboard()) {
					showHelpFixPanel(fragment);
				} else {
					removeAllChildNodes(pucontent);
					pucontent.appendChild(fragment);
				}
			} else {
				if (!isDashboard()) {
					// show, for example, dataPageInitialGuidance in Info Panel
					var clone = fragment.cloneNode(true);
					removeAllChildNodes(pucontent);
					pucontent.appendChild(clone);
				}
			}
			fragment = null;

			// for the voter
			$('.voteInfo_voterInfo').hover(function() {
				var email = $(this).data('email').replace(' (at) ', '@');
				if (email !== '') {
					$(this).html('<a href="mailto:' + email + '" title="' + email +
							'" style="color:black"><span class="glyphicon glyphicon-envelope"></span></a>');
					$(this).closest('td').css('text-align', 'center');
					$(this).children('a').tooltip().tooltip('show');
				} else {
					$(this).html($(this).data('name'));
					$(this).closest('td').css('text-align', 'left');
				}
			}, function() {
				$(this).html($(this).data('name'));
				$(this).closest('td').css('text-align', 'left');
			});
			if(!isDashboard()) {
				return pucontent;
			} else {
				return null;
			}
		};
		// delay before show
		window.showInPop = function(str, tr, hideIfLast, fn, immediate) {
			if (hideInterval) {
				clearTimeout(hideInterval);
				hideInterval = null;
			}
			if (immediate) {
				return window.showInPop2(str, tr, hideIfLast, fn);
			}
		};
		window.resetPop = function() {
			lastShown = null;
		};
	});
});

/**
 * Check if we need LRM/RLM marker to display
 * @param field choice field to append if needed
 * @param dir direction of current locale (control float direction0
 * @param value the value of votes (check &lrm; &rlm)
 */
function checkLRmarker(field, dir, value) {
	if (value) {
		if (value.indexOf("\u200E") > -1 || value.indexOf("\u200F") > -1) {
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
 *
 * @param div {DOM} div to append to
 * @param value {String} string value
 * @param pClass {String} html class for the voting item
 * @param tr {DOM} ignored, but the tr the span belongs to
 * @return {DOM} the new span
 */
function appendItem(div, value, pClass, tr) {
	if (!value) {
		return;
	}
	var text = document.createTextNode(value);
	var span = document.createElement("span");
	span.appendChild(text);
	if (!value) {
		span.className = "selected";
	} else if (pClass) {
		span.className = pClass;
	} else {
		span.className = "value";
	}
	div.appendChild(span);
	return span;
}

function testsToHtml(tests) {
	var newHtml = "";
	if (!tests) {
		return newHtml;
	}
	for (var i = 0; i < tests.length; i++) {
		var testItem = tests[i];
		newHtml += "<p class='trInfo tr_" + testItem.type;
		if (testItem.type == 'Warning') {
			newHtml += ' alert alert-warning fix-popover-help';
		} else if (testItem.type == 'Error') {
			newHtml += ' alert alert-danger fix-popover-help';
		}
		newHtml += "' title='" + testItem.type + "'>";
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

function setDivClass(div, testKind) {
	if (!testKind) {
		div.className = "d-item";
	} else if (testKind == "Warning") {
		div.className = "d-item-warn";
	} else if (testKind == "Error") {
		div.className = "d-item-err";
	} else {
		div.className = "d-item";
	}
}

function findItemByValue(items, value) {
	if (!items) {
		return null;
	}
	for (var i in items) {
		if (value == items[i].value) {
			return items[i];
		}
	}
	return null;
}

/**
 * Show an item that's not in the saved data, but has been proposed newly by the user.
 * Called only by loadHandler in handleWiredClick.
 * Used for "+" button, both in Dashboard Fix pop-up window and in regular (non-Dashboard) table.
 */
function showProposedItem(inTd, tr, theRow, value, tests, json) {

	// Find where our value went.
	var ourItem = findItemByValue(theRow.items, value);
	var testKind = getTestKind(tests);
	var ourDiv = null;
	var wrap;
	if (!ourItem) {
		/*
		 * This may happen if, for example, the user types a space (" ") in
		 * the input pop-up window and presses Enter. The value has been rejected
		 * by the server. Then we show an additional pop-up window with the error message
		 * from the server like "Input Processor Error: DAIP returned a 0 length string"
		 */
		ourDiv = document.createElement("div");
		var newButton = cloneAnon(document.getElementById('proto-button'));
		const otherCell = tr.querySelector('.othercell');
		if (otherCell && tr.myProposal) {
			otherCell.removeChild(tr.myProposal);
		}
		tr.myProposal = ourDiv;
		tr.myProposal.value = value;
		tr.myProposal.button = newButton;
		if (newButton) {
			newButton.value = value;
			if (tr.lastOn) {
				tr.lastOn.checked = false;
				tr.lastOn.className = "ichoice-o";
			}
			wireUpButton(newButton, tr, theRow, "[retry]", {
				"value": value
			});
			wrap = wrapRadio(newButton);
			ourDiv.appendChild(wrap);
		}
		var h3 = document.createElement("span");
		var span = appendItem(h3, value, "value", tr);
		setLang(span);
		ourDiv.appendChild(h3);
		if (otherCell) {
			otherCell.appendChild(tr.myProposal);
		}
	} else {
		ourDiv = ourItem.div;
	}
	if (json && !parseStatusAction(json.statusAction).vote) {
		ourDiv.className = "d-item-err";

		const replaceErrors = (json.statusAction === 'FORBID_PERMANENT_WITHOUT_FORUM');
		if (replaceErrors) {
			/*
			 * Special case: for clarity, replace any warnings/errors that may be
			 * in tests[] with a single error message for this situation.
			 */
			tests = [{
				type: 'Error',
				message: stui_str("StatusAction_" + json.statusAction)
			}];
		}

		var input = $(inTd).closest('tr').find('.input-add');
		if (input) {
			input.closest('.form-group').addClass('has-error');
			input.popover('destroy').popover({
				placement: 'bottom',
				html: true,
				content: testsToHtml(tests),
				trigger: 'hover'
			}).popover('show');
			if (tr.myProposal)
				tr.myProposal.style.display = "none";
		}
		if (ourItem || (replaceErrors && value === '' /* Abstain */)) {
			str = stui.sub("StatusAction_msg",
				[stui_str("StatusAction_" + json.statusAction)], "p", "");
			var str2 = stui.sub("StatusAction_popupmsg",
				[stui_str("StatusAction_" + json.statusAction), theRow.code], "p", "");
			// show in modal popup (ouch!)
			alert(str2);

			// show this message in a sidebar also
			showInPop(stopIcon + str, tr, null, null, true);
		}
		return;
	} else if (json && json.didNotSubmit) {
		ourDiv.className = "d-item-err";
		showInPop("(ERROR: Unknown error - did not submit this value.)", tr, null, null, true);
		return;
	} else {
		setDivClass(ourDiv, testKind);
	}

	if (testKind || !ourItem) {
		var div3 = document.createElement("div");
		var newHtml = "";
		newHtml += testsToHtml(tests);

		if (!ourItem) {
			var h3 = document.createElement("h3");
			var span = appendItem(h3, value, "value", tr);
			setLang(span);
			h3.className = "span";
			div3.appendChild(h3);
		}
		var newDiv = document.createElement("div");
		div3.appendChild(newDiv);
		newDiv.innerHTML = newHtml;
		if (json && (!parseStatusAction(json.statusAction).vote)) {
			div3.appendChild(createChunk(
				stui.sub("StatusAction_msg",
					[stui_str("StatusAction_" + json.statusAction)], "p", "")));
		}

		div3.popParent = tr;

		// will replace any existing function
		var ourShowFn = function(showDiv) {
			var retFn;
			if (ourItem && ourItem.showFn) {
				retFn = ourItem.showFn(showDiv);
			} else {
				retFn = null;
			}
			if (tr.myProposal && (value == tr.myProposal.value)) { // make sure it wasn't submitted twice
				showDiv.appendChild(div3);
			}
			return retFn;
		};
		listenToPop(null, tr, ourDiv, ourShowFn);
		showInPop(null, tr, ourDiv, ourShowFn, true);
	}

	return false;
}

/**
 * Return a function that will show info for the given item in the Info Panel.
 *
 * @param theRow the data row
 * @param item the candidate item
 * @returns the function
 *
 * Called only by addVitem.
 */
function showItemInfoFn(theRow, item) {
	return function(td) {
		var h3 = document.createElement("div");
		var displayValue = item.value;
		if (item.value === INHERITANCE_MARKER) {
			displayValue = theRow.inheritedValue;
		}

		var span = appendItem(h3, displayValue, item.pClass); /* no need to pass in 'tr' - clicking this span would have no effect. */
		setLang(span);
		h3.className = "span";
		td.appendChild(h3);

		if (item.value) {
			/*
			 * Strings produced here, used as keys for stui.js, may include:
			 *  "pClass_winner", "pClass_alias", "pClass_fallback", "pClass_fallback_code", "pClass_fallback_root", "pClass_loser".
			 *  See getPClass in DataSection.java.
			 *
			 *  TODO: why not show stars, etc., here?
			 */
			h3.appendChild(createChunk(stui.sub("pClass_" + item.pClass, item), "p", "pClassExplain"));
		}

		if (item.value === INHERITANCE_MARKER) {
			addJumpToOriginal(theRow, h3);
		}

		var newDiv = document.createElement("div");
		td.appendChild(newDiv);

		if (item.tests) {
			newDiv.innerHTML = testsToHtml(item.tests);
		} else {
			newDiv.innerHTML = "<i>no tests</i>";
		}

		if (item.example) {
			appendExample(td, item.example);
		}
	}; // end function(td)
}

/**
 * Add a link in the Info Panel for "Jump to Original" (stui.str('followAlias')),
 * if theRow.inheritedLocale or theRow.inheritedXpid is defined.
 *
 * Normally at least one of theRow.inheritedLocale and theRow.inheritedXpid should be
 * defined whenever we have an INHERITANCE_MARKER item. Otherwise an error is reported
 * by checkRowConsistency.
 *
 * This is currently (2018-12-01) the only place inheritedLocale or inheritedXpid is used on the client.
 * An alternative would be for the server to send the link (clickyLink.href), instead of inheritedLocale
 * and inheritedXpid, to the client, avoiding the need for the client to know so much, including the need
 * to replace 'code-fallback' with 'root' or when to use surveyCurrentLocale in place of inheritedLocale
 * or use xpstrid in place of inheritedXpid.
 *
 * @param theRow the row
 * @param el the element to which to append the link
 */
function addJumpToOriginal(theRow, el) {
	if (theRow.inheritedLocale || theRow.inheritedXpid) {
		var loc = theRow.inheritedLocale;
		var xpstrid = theRow.inheritedXpid || theRow.xpstrid;
		if (!loc) {
			loc = surveyCurrentLocale;
		} else if (loc === 'code-fallback') {
			/*
			 * Never use 'code-fallback' in the link, use 'root' instead.
			 * On the server, 'code-fallback' sometimes goes by the name XMLSource.CODE_FALLBACK_ID.
			 * Reference: https://unicode.org/cldr/trac/ticket/11622
			 */
			loc = 'root';
		}
		if ((xpstrid === theRow.xpstrid) && // current hash
			(loc === window.surveyCurrentLocale)) { // current locale
			// i.e., following the alias would come back to the current item
			el.appendChild(createChunk(stui.str('noFollowAlias'), "span", 'followAlias'));
		} else {
			var clickyLink = createChunk(stui.str('followAlias'), "a", 'followAlias');
			clickyLink.href = '#/' + loc + '//' + xpstrid;
			el.appendChild(clickyLink);
		}
	}
}

function appendExample(parent, text, loc) {
	var div = document.createElement("div");
	div.className = "d-example well well-sm";
	div.innerHTML = text;
	setLang(div, loc);
	parent.appendChild(div);
	return div;
}

/**
 * Append a Vetting item ( vote button, etc ) to the row.
 *
 * @param {DOM} td cell to append into
 * @param {DOM} tr which row owns the items
 * @param {JSON} theRow JSON content of this row's data
 * @param {JSON} item JSON of the specific item we are adding
 * @param {DOM} newButton	 button prototype object
 */
function addVitem(td, tr, theRow, item, newButton) {
	var displayValue = item.value;
	if (displayValue === INHERITANCE_MARKER) {
		displayValue = theRow.inheritedValue;
	}
	if (!displayValue) {
		return;
	}
	var div = document.createElement("div");
	var isWinner = (td == tr.proposedcell);
	var testKind = getTestKind(item.tests);
	setDivClass(div, testKind);
	item.div = div; // back link


	var choiceField = document.createElement("div");
	var wrap;
	choiceField.className = "choice-field";
	if (newButton) {
		newButton.value = item.value;
		wireUpButton(newButton, tr, theRow, item.valueHash);
		wrap = wrapRadio(newButton);
		choiceField.appendChild(wrap);
	}
	var subSpan = document.createElement("span");
	subSpan.className = "subSpan";
	var span = appendItem(subSpan, displayValue, item.pClass, tr);
	choiceField.appendChild(subSpan);

	setLang(span);
	checkLRmarker(choiceField, span.dir, item.value);

	if (item.isBaselineValue == true) {
		appendIcon(choiceField, "i-star", stui.str("voteInfo_baseline_desc"));
	}
	if (item.votes && !isWinner) {
		if (item.valueHash == theRow.voteVhash && theRow.canFlagOnLosing && !theRow.rowFlagged) {
			var newIcon = addIcon(choiceField, "i-stop"); // DEBUG
		}
	}

	/*
	 * Note: history is maybe only defined for debugging; won't normally display it in production.
	 * See DataSection.USE_CANDIDATE_HISTORY which currently should be false for production, so
	 * that item.history will be undefined.
	 */
	if (item.history) {
		const historyText = " ☛" + item.history;
		const historyTag = createChunk(historyText, "span", "");
		choiceField.appendChild(historyTag);
		listenToPop(historyText, tr, historyTag);
	}

	if (newButton &&
		theRow.voteVhash == item.valueHash &&
		theRow.items[theRow.voteVhash].votes &&
		theRow.items[theRow.voteVhash].votes[surveyUser.id] &&
		theRow.items[theRow.voteVhash].votes[surveyUser.id].overridedVotes) {
		var overrideTag = createChunk(theRow.items[theRow.voteVhash].votes[surveyUser.id].overridedVotes, "span", "i-override");
		choiceField.appendChild(overrideTag);
	}

	div.appendChild(choiceField);

	// wire up the onclick function for the Info Panel
	td.showFn = item.showFn = showItemInfoFn(theRow, item);
	div.popParent = tr;
	listenToPop(null, tr, div, td.showFn);
	td.appendChild(div);

	if (item.example && item.value != item.examples) {
		appendExample(div, item.example);
	}
}

function appendExtraAttributes(container, theRow) {
	for (var attr in theRow.extraAttributes) {
		var attrval = theRow.extraAttributes[attr];
		var extraChunk = createChunk(attr + "=" + attrval, "span", "extraAttribute");
		container.appendChild(extraChunk);
	}
}

/**
 * Get numeric, given string
 *
 * @param {String} lev
 * @return {Number} or 0
 */
function covValue(lev) {
	lev = lev.toUpperCase();
	if (window.surveyLevels && window.surveyLevels[lev]) {
		return parseInt(window.surveyLevels[lev].level);
	} else {
		return 0;
	}
}

function covName(lev) {
	if (!window.surveyLevels) {
		return null;
	}
	for (var k in window.surveyLevels) {
		if (parseInt(window.surveyLevels[k].level) == lev) {
			return k.toLowerCase();
		}
	}
	return null;
}

function effectiveCoverage() {
	if (!window.surveyOrgCov) {
		throw new Error("surveyOrgCov not yet initialized");
	}

	if (surveyUserCov) {
		return covValue(surveyUserCov);
	} else {
		return covValue(surveyOrgCov);
	}
}

function updateCovFromJson(json) {
	if (json.covlev_user && json.covlev_user != 'default') {
		window.surveyUserCov = json.covlev_user;
	} else {
		window.surveyUserCov = null;
	}

	if (json.covlev_org) {
		window.surveyOrgCov = json.covlev_org;
	} else {
		window.surveyOrgCov = null;
	}
}

/**
 * Update the coverage classes, show and hide things in and out of coverage
 */
function updateCoverage(theDiv) {
	if (theDiv == null) return;
	var theTable = theDiv.theTable;
	if (theTable == null) return;
	if (!theTable.origClass) {
		theTable.origClass = theTable.className;
	}
	if (window.surveyLevels != null) {
		var effective = effectiveCoverage();
		var newStyle = theTable.origClass;
		for (var k in window.surveyLevels) {
			var level = window.surveyLevels[k];

			if (effective < parseInt(level.level)) {
				newStyle = newStyle + " hideCov" + level.level;
			}
		}
		if (newStyle != theTable.className) {
			theTable.className = newStyle;
		}
	}
}

function loadStui(loc, cb) {
	if (!stui.ready) {
		/*
		 * https://dojotoolkit.org/reference-guide/1.10/dojo/string.html
		 * https://dojotoolkit.org/reference-guide/1.10/dojo/i18n.html
		 */
		require(["dojo/string", "dojo/i18n!./surveyTool/nls/stui.js"],
		function(string, stuibundle) {
			if (!stuidebug_enabled) {
				stui.str = function(x) {
					if (stuibundle[x]) return stuibundle[x];
					else return x;
				};
				stui.sub = function(x, y) {
					let template = stui.str(x);
					if (!template) {
						return "";
					}
					return string.substitute(template, y);
				};
			} else {
				stui.str = stui_str; // debug
				stui.sub = function(x, y) {
					return stui_str(x) + '{' + Object.keys(y) + '}';
				};
			}
			stuibundle.htmltranshint = stui.htmltranshint = TRANS_HINT_LANGUAGE_NAME;
			stui.ready = true;
			if (cb) {
				cb(stui);
			}
		});
	} else {
		if (cb) {
			cb(stui);
		}
	}
	return stui;
}

function firstword(str) {
	return str.split(" ")[0];
}

function appendIcon(toElement, className, title) {
	var e = createChunk(null, "div", className);
	e.title = title;
	toElement.appendChild(e);
	return e;
}

function hideAfter(whom, when) {
	if (!when) {
		when = 10000;
	}
	setTimeout(function() {
		whom.style.opacity = "0.8";
	}, when / 3);
	setTimeout(function() {
		whom.style.opacity = "0.5";
	}, when / 2);
	setTimeout(function() {
		setDisplayed(whom, false);
	}, when);
	return whom;
}

function appendInputBox(parent, which) {
	var label = createChunk(stui.str(which), "div", which);
	var input = document.createElement("input");
	input.stChange = function(onOk, onErr) {};
	var change = createChunk(stui.str("appendInputBoxChange"), "button", "appendInputBoxChange");
	var cancel = createChunk(stui.str("appendInputBoxCancel"), "button", "appendInputBoxCancel");
	var notify = document.createElement("div");
	notify.className = "appendInputBoxNotify";
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
		notify.appendChild(createChunk(stui.str("loading"), 'i'));
		var onOk = function(msg) {
			removeClass(label, "d-item-selected");
			removeAllChildNodes(notify);
			notify.appendChild(hideAfter(createChunk(msg, "span", "okayText")));
		};
		var onErr = function(msg) {
			removeClass(label, "d-item-selected");
			removeAllChildNodes(notify);
			notify.appendChild(createChunk(msg, "span", "stopText"));
		};

		input.stChange(onOk, onErr);
	};

	var changeFn = function(e) {
		doChange();
		stStopPropagation(e);
		return false;
	};
	var cancelFn = function(e) {
		input.value = "";
		doChange();
		stStopPropagation(e);
		return false;
	};
	var keypressFn = function(e) {
		if (!e || !e.keyCode) {
			return true; // not getting the point here.
		} else if (e.keyCode == 13) {
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
 */
function scrollToItem() {
	if (surveyCurrentId != null && surveyCurrentId != '') {
		require(["dojo/window"], function(win) {
			var xtr = document.getElementById("r@" + surveyCurrentId);
			if (xtr != null) {
				console.log("Scrolling to " + surveyCurrentId);
				win.scrollIntoView("r@" + surveyCurrentId);
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
 * @param loc  optional
 * @returns locale bundle
 */
function locInfo(loc) {
	if (!loc) {
		loc = window.surveyCurrentLocale;
	}
	return locmap.getLocaleInfo(loc);
}

var overridedir = null;

function setLang(node, loc) {
	var info = locInfo(loc);

	if (overridedir) {
		node.dir = overridedir;
	} else if (info && info.dir) {
		node.dir = info.dir;
	}

	if (info && info.bcp47) {
		node.lang = info.bcp47;
	}
}

/**
 * Get a table showing old votes available for importing, along with
 * controls for choosing which votes to import.
 *
 * @param voteList the array of old votes
 * @param type "contested" for losing votes or "uncontested" for winning votes
 * @param translationHintsLanguage a string indicating the translation hints language, generally "English"
 * @param dir the direction, such as "ltr" for left-to-right
 * @returns a new div element containing the table and controls
 *
 * Called only by addOldvotesType
 */
function showVoteTable(voteList, type, translationHintsLanguage, dir) {
	'use strict';
	var voteTableDiv = document.createElement("div");
	var t = document.createElement("table");
	t.id = 'oldVotesAcceptList';
	voteTableDiv.appendChild(t);
	var th = document.createElement("thead");
	var tb = document.createElement("tbody");
	var tr = document.createElement("tr");
	tr.appendChild(createChunk(stui.str("v_oldvotes_path"), "th", "code"));
	tr.appendChild(createChunk(translationHintsLanguage, "th", "v-comp"));
	tr.appendChild(createChunk(stui.sub("v_oldvotes_winning_msg", {
		version: surveyLastVoteVersion
	}), "th", "v-win"));
	tr.appendChild(createChunk(stui.str("v_oldvotes_mine"), "th", "v-mine"));
	tr.appendChild(createChunk(stui.str("v_oldvotes_accept"), "th", "v-accept"));
	th.appendChild(tr);
	t.appendChild(th);
	var oldPath = '';
	var oldSplit = [];
	var mainCategories = [];
	for (var k in voteList) {
		var row = voteList[k];
		var tr = document.createElement("tr");
		var tdp;
		var rowTitle = '';

		// delete common substring
		var pathSplit = row.pathHeader.split('	');
		for (var nn in pathSplit) {
			if (pathSplit[nn] != oldSplit[nn]) {
				break;
			}
		}
		if (nn != pathSplit.length - 1) {
			// need a header row.
			var trh = document.createElement('tr');
			trh.className = 'subheading';
			var tdh = document.createElement('th');
			tdh.colSpan = 5;
			for (var nn in pathSplit) {
				if (nn < pathSplit.length - 1) {
					tdh.appendChild(createChunk(pathSplit[nn], "span", "pathChunk"));
				}
			}
			trh.appendChild(tdh);
			tb.appendChild(trh);
		}
		if (mainCategories.indexOf(pathSplit[0]) === -1) {
			mainCategories.push(pathSplit[0]);
		}
		oldSplit = pathSplit;
		rowTitle = pathSplit[pathSplit.length - 1];

		tdp = createChunk("", "td", "v-path");

		var dtpl = createChunk(rowTitle, "a");
		dtpl.href = "v#/" + surveyCurrentLocale + "//" + row.strid;
		dtpl.target = '_CLDR_ST_view';
		tdp.appendChild(dtpl);

		tr.appendChild(tdp);
		var td00 = createChunk(row.baseValue, "td", "v-comp"); // english
		tr.appendChild(td00);
		var td0 = createChunk("", "td", "v-win");
		if (row.winValue) {
			var span0 = appendItem(td0, row.winValue, "winner");
			span0.dir = dir;
		}
		tr.appendChild(td0);
		var td1 = createChunk("", "td", "v-mine");
		var label = createChunk("", "label", "");
		var span1 = appendItem(label, row.myValue, "value");
		td1.appendChild(label);
		span1.dir = dir;
		tr.appendChild(td1);
		var td2 = createChunk("", "td", "v-accept");
		var box = createChunk("", "input", "");
		box.type = "checkbox";
		if (type == 'uncontested') { // uncontested true by default
			box.checked = true;
		}
		row.box = box; // backlink
		td2.appendChild(box);
		tr.appendChild(td2);

		(function(tr, box, tdp) {
			return function() {
				// allow click anywhere
				listenFor(tr, "click", function(e) {
					box.checked = !box.checked;
					stStopPropagation(e);
					return false;
				});
				// .. but not on the path.  Also listen to the box and do nothing
				listenFor([tdp, box], "click", function(e) {
					stStopPropagation(e);
					return false;
				});
			};
		})(tr, box, tdp)();

		tb.appendChild(tr);
	}
	t.appendChild(tb);
	addImportVotesFooter(voteTableDiv, voteList, mainCategories);
	return voteTableDiv;
}

/**
 * Add to the given div a footer with buttons for choosing all or none
 * of the old votes, and with checkboxes for choosing all or none within
 * each of two or more main categories such as "Locale Display Names".
 *
 * @param voteTableDiv the div to add to
 * @param voteList the list of old votes
 * @param mainCategories the list of main categories
 *
 * Called only by showVoteTable
 *
 * Reference: https://unicode.org/cldr/trac/ticket/11517
 */
function addImportVotesFooter(voteTableDiv, voteList, mainCategories) {
	'use strict';
	voteTableDiv.appendChild(createLinkToFn("v_oldvotes_all", function() {
		for (var k in voteList) {
			voteList[k].box.checked = true;
		}
		for (var cat in mainCategories) {
			$("#cat" + cat).prop('checked', true);
		}
	}, "button"));

	voteTableDiv.appendChild(createLinkToFn("v_oldvotes_none", function() {
		for (var k in voteList) {
			voteList[k].box.checked = false;
		}
		for (var cat in mainCategories) {
			$("#cat" + cat).prop('checked', false);
		}
	}, "button"));

	if (mainCategories.length > 1) {
		voteTableDiv.appendChild(document.createTextNode(stui.str("v_oldvotes_all_section")));
		for (var cat in mainCategories) {
			let mainCat = mainCategories[cat];
			var checkbox = document.createElement("input");
			checkbox.type = "checkbox";
			checkbox.id = "cat" + cat;
			voteTableDiv.appendChild(checkbox);
			voteTableDiv.appendChild(document.createTextNode(mainCat + ' '));
			listenFor(checkbox, "click", function(e) {
				for (var k in voteList) {
					var row = voteList[k];
					if (row.pathHeader.startsWith(mainCat)) {
						row.box.checked = this.checked;
					}
				}
				stStopPropagation(e);
				return false;
			});
		}
	}
}

/**
 * Reload a specific row
 *
 * Called by loadHandler in handleWiredClick
 */
function refreshSingleRow(tr, theRow, onSuccess, onFailure) {

	showLoader(tr.theTable.theDiv.loader, stui.loadingOneRow);

	var ourUrl = contextPath + "/SurveyAjax?what=" + WHAT_GETROW +
		"&_=" + surveyCurrentLocale +
		"&xpath=" + theRow.xpathId +
		"&fhash=" + tr.rowHash +
		"&s=" + surveySessionId +
		"&automatic=t";

	if (isDashboard()) {
		ourUrl += "&dashboard=true";
	}

	var loadHandler = function(json) {
		try {
			if (json.section.rows[tr.rowHash]) {
				theRow = json.section.rows[tr.rowHash];
				tr.theTable.json.section.rows[tr.rowHash] = theRow;
				cldrSurveyTable.updateRow(tr, theRow);

				hideLoader(tr.theTable.theDiv.loader);
				onSuccess(theRow);
				if (isDashboard()) {
					refreshFixPanel(json);
				} else {
					window.showInPop("", tr, tr.proposedcell, tr.proposedcell.showFn, true /* immediate */ );
					refreshCounterVetting();
				}
			} else {
				tr.className = "ferrbox";
				console.log("could not find " + tr.rowHash + " in " + json);
				onFailure("refreshSingleRow: Could not refresh this single row: Server failed to return xpath #" + theRow.xpathId +
						" for locale " + surveyCurrentLocale);
			}
		} catch (e) {
			console.log("Error in ajax post [refreshSingleRow] ", e.message);
		}
	};
	var errorHandler = function(err) {
		let responseText = cldrStAjax.errResponseText(err);
		console.log('Error: ' + err + ' response ' + responseText);
		tr.className = "ferrbox";
		tr.innerHTML = "Error while  loading: " + err.name + " <br> " +
			err.message + "<div style='border: 1px solid red;'>" +
			responseText +  "</div>";
		onFailure("err", err);
	};
	var xhrArgs = {
		url: ourUrl + cacheKill(),
		handleAs: "json",
		load: loadHandler,
		error: errorHandler,
		timeout: ajaxTimeout
	};
	cldrStAjax.queueXhr(xhrArgs);
}

/**
 * Bottleneck for voting buttons
 */
function handleWiredClick(tr, theRow, vHash, box, button, what) {
	var value = "";
	var valToShow;
	if (tr.wait) {
		return;
	}
	if (box) {
		valToShow = box.value;
		value = box.value;
		if (value.length == 0) {
			if (box.focus) {
				box.focus();
				myUnDefer();
			}
			return; // nothing entered.
		}
	} else {
		valToShow = button.value;
	}
	if (!what) {
		what = 'submit';
	}
	if (what == 'submit') {
		button.className = "ichoice-x-ok"; // TODO: ichoice-inprogress? spinner?
		showLoader(tr.theTable.theDiv.loader, stui.voting);
	} else {
		showLoader(tr.theTable.theDiv.loader, stui.checking);
	}

	// select
	updateCurrentId(theRow.xpstrid);

	// and scroll
	showCurrentId();

	if (tr.myProposal) {
		const otherCell = tr.querySelector('.othercell');
		if (otherCell) {
			otherCell.removeChild(tr.myProposal);
		}
		tr.myProposal = null; // mark any pending proposal as invalid.
	}

	var myUnDefer = function() {
		tr.wait = false;
	};
	tr.wait = true;
	resetPop(tr);
	theRow.proposedResults = null;

	console.log("Vote for " + tr.rowHash + " v='" + vHash + "', value='" + value + "'");
	var ourContent = {
		what: what,
		xpath: tr.xpathId,
		"_": surveyCurrentLocale,
		fhash: tr.rowHash,
		vhash: vHash,
		s: tr.theTable.session
	};

	var ourUrl = contextPath + "/SurveyAjax";

	var voteLevelChanged = document.getElementById("voteLevelChanged");
	if (voteLevelChanged) {
		ourContent.voteLevelChanged = voteLevelChanged.value;
	}

	var originalTrClassName = tr.className;
	tr.className = 'tr_checking1';

	var loadHandler = function(json) {
		/*
		 * Restore tr.className, so it stops being 'tr_checking1' immediately on receiving
		 * any response. It may change again below, such as to 'tr_err' or 'tr_checking2'.
		 */
		tr.className = originalTrClassName;
		try {
			if (json.err && json.err.length > 0) {
				tr.className = 'tr_err';
				handleDisconnect('Error submitting a vote', json);
				tr.innerHTML = "<td colspan='4'>" + stopIcon + " Could not check value. Try reloading the page.<br>" + json.err + "</td>";
				myUnDefer();
				handleDisconnect('Error submitting a vote', json);
			} else {
				if (json.submitResultRaw) { // if submitted..
					tr.className = 'tr_checking2';
					refreshSingleRow(tr, theRow, function(theRow) {
						// submit went through. Now show the pop.
						button.className = 'ichoice-o';
						button.checked = false;
						hideLoader(tr.theTable.theDiv.loader);
						if (json.testResults && (json.testWarnings || json.testErrors)) {
							// tried to submit, have errs or warnings.
							showProposedItem(tr.inputTd, tr, theRow, valToShow, json.testResults);
						}
						if (box) {
							box.value = ""; // submitted - dont show.
						}
						myUnDefer();
					}, function(err) {
						myUnDefer();
						handleDisconnect(err, json);
					}); // end refresh-loaded-fcn
					// end: async
				} else {
					// Did not submit. Show errors, etc
					if ((json.statusAction && json.statusAction != 'ALLOW') ||
						(json.testResults && (json.testWarnings || json.testErrors))) {
						showProposedItem(tr.inputTd, tr, theRow, valToShow, json.testResults, json);
					} // else no errors, not submitted.  Nothing to do.
					if (box) {
						box.value = ""; // submitted - dont show.
					}
					button.className = 'ichoice-o';
					button.checked = false;
					hideLoader(tr.theTable.theDiv.loader);
					myUnDefer();
				}
			}
		} catch (e) {
			tr.className = 'tr_err';
			tr.innerHTML = stopIcon + " Could not check value. Try reloading the page.<br>" + e.message;
			console.log("Error in ajax post [handleWiredClick] ", e.message);
			myUnDefer();
			handleDisconnect("handleWiredClick:" + e.message, json);
		}
	};
	var errorHandler = function(err) {
		/*
		 * Restore tr.className, so it stops being 'tr_checking1' immediately on receiving
		 * any response. It may change again below, such as to 'tr_err'.
		 */
		tr.className = originalTrClassName;
		let responseText = cldrStAjax.errResponseText(err);
		console.log('Error: ' + err + ' response ' + responseText);
		handleDisconnect('Error: ' + err + ' response ' + responseText, null);
		theRow.className = "ferrbox";
		theRow.innerHTML = "Error while  loading: " + err.name + " <br> " + err.message +
		"<div style='border: 1px solid red;'>" + responseText + "</div>";
		myUnDefer();
	};
	if (box) {
		stdebug("this is a post: " + value);
		ourContent.value = value;
	}
	var xhrArgs = {
		url: ourUrl,
		handleAs: "json",
		content: ourContent,
		timeout: ajaxTimeout,
		load: loadHandler,
		error: errorHandler
	};
	cldrStAjax.queueXhr(xhrArgs);
}

/**
 * Load the Admin Panel
 *
 * TODO move admin panel code to separate module
 */
function loadAdminPanel() {
	if (!vap) return;
	loadStui();
	var adminStuff = document.getElementById("adminStuff");
	if (!adminStuff) return;
	
	// make sure the stui strings are loaded first
    loadStui(null, function(stui) { 
	
		var content = document.createDocumentFragment();
	
		var list = document.createElement("ul");
		list.className = "adminList";
		content.appendChild(list);
	
		function loadOrFail(urlAppend, theDiv, loadHandler, postData) {
			var ourUrl = contextPath + "/AdminAjax.jsp?vap=" + vap + "&" + urlAppend;
			var errorHandler = function(err) {
				let responseText = cldrStAjax.errResponseText(err);
				console.log('adminload ' + urlAppend + ' Error: ' + err + ' response ' + responseText);
				theDiv.className = "ferrbox";
				theDiv.innerHTML = "Error while  loading: " + err.name + " <br> " +
					err.message + "<div style='border: 1px solid red;'>" + responseText + "</div>";
			};
			var xhrArgs = {
				url: ourUrl + cacheKill(),
				handleAs: "json",
				load: loadHandler,
				error: errorHandler,
				postData: postData
			};
			if (!loadHandler) {
				xhrArgs.handleAs = "text";
				xhrArgs.load = function(text) {
					theDiv.innerHTML = text;
				};
			}
			if (xhrArgs.postData) {
				/*
				 * Make a POST request
				 */
				console.log("admin post: ourUrl: " + ourUrl + " data:" + postData);
				xhrArgs.headers = {
					"Content-Type": "text/plain"
				};
			} else {
				/*
				 * Make a GET request
				 */
				console.log("admin get: ourUrl: " + ourUrl);
			}
			cldrStAjax.sendXhr(xhrArgs);
		}
		var panelLast = null;
		var panels = {};
		var panelFirst = null;
	
		function panelSwitch(name) {
			if (panelLast) {
				panelLast.div.style.display = 'none';
				panelLast.listItem.className = 'notselected';
				panelLast = null;
			}
			if (name && panels[name]) {
				panelLast = panels[name];
				panelLast.listItem.className = 'selected';
				panelLast.fn(panelLast.udiv);
				panelLast.div.style.display = 'block';
				window.location.hash = "#!" + name;
			}
		}
	
		function addAdminPanel(type, fn) {
			var panel = panels[type] = {
				type: type,
				name: stui.str(type) || type,
				desc: stui.str(type + "_desc") || '(no description - missing from stui)',
				fn: fn
			};
			panel.div = document.createElement("div");
			panel.div.style.display = 'none';
			panel.div.className = 'adminPanel';
	
			var h = document.createElement("h3");
			h.className = "adminTitle";
			h.appendChild(document.createTextNode(panel.desc || type));
			panel.div.appendChild(h);
	
			panel.udiv = document.createElement("div");
			panel.div.appendChild(panel.udiv);
	
			panel.listItem = document.createElement("li");
			panel.listItem.appendChild(document.createTextNode(panel.name || type));
			panel.listItem.title = panel.desc || type;
			panel.listItem.className = "notselected";
			panel.listItem.onclick = function(e) {
				panelSwitch(panel.type);
				return false;
			};
			list.appendChild(panel.listItem);
	
			content.appendChild(panel.div);
	
			if (!panelFirst) {
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
	
				if (!json || !json.users || Object.keys(json.users) == 0) {
					frag2.appendChild(document.createTextNode(stui.str("No users.")));
				} else {
					for (sess in json.users) {
						var cs = json.users[sess];
						var user = createChunk(null, "div", "adminUser");
						user.appendChild(createChunk("Session: " + sess, "span", "adminUserSession"));
						if (cs.user) {
							user.appendChild(createUser(cs.user));
						} else {
							user.appendChild(createChunk("(anonymous)", "div", "adminUserUser"));
						}
						/*
						 * cs.lastBrowserCallMillisSinceEpoch = time elapsed in millis since server heard from client
						 * cs.lastActionMillisSinceEpoch = time elapsed in millis since user did active action
						 * cs.millisTillKick = how many millis before user will be kicked if inactive
						 */
						user.appendChild(createChunk(
							"LastCall: " + cs.lastBrowserCallMillisSinceEpoch +
							", LastAction: " + cs.lastActionMillisSinceEpoch +
							", IP: " + cs.ip +
							", ttk:" + (parseInt(cs.millisTillKick) / 1000).toFixed(1) + "s",
							"span", "adminUserInfo"));
	
						var unlinkButton = createChunk(stui.str("admin_users_action_kick"), "button", "admin_users_action_kick");
						user.appendChild(unlinkButton);
						unlinkButton.onclick = function(e) {
							unlinkButton.className = 'deactivated';
							unlinkButton.onclick = null;
							loadOrFail("do=unlink&s=" + cs.id, unlinkButton, function(json) {
								removeAllChildNodes(unlinkButton);
								if (json.removing == null) {
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
	
			div.className = "adminThreads";
			var u = createChunk("Loading...", "div", "adminThreadList");
			var stack = createChunk(null, "div", "adminThreadStack");
			frag.appendChild(u);
			frag.appendChild(stack);
			var c2s = createChunk(stui.str("clickToSelect"), "button", "clickToSelect");
			clickToSelect(c2s, stack);
	
			removeAllChildNodes(div);
			div.appendChild(c2s);
			var clicked = null;
	
			div.appendChild(frag);
			loadOrFail("do=threads", u, function(json) {
				if (!json || !json.threads || Object.keys(json.threads.all) == 0) {
					removeAllChildNodes(u);
					u.appendChild(document.createTextNode(stui.str("No threads.")));
				} else {
					var frag2 = document.createDocumentFragment();
					removeAllChildNodes(stack);
					stack.innerHTML = stui.str("adminClickToViewThreads");
					deadThreads = {};
					if (json.threads.dead) {
						var header = createChunk(stui.str("adminDeadThreadsHeader"), "div", "adminDeadThreadsHeader");
						var deadul = createChunk("", "ul", "adminDeadThreads");
						for (var jj = 0; jj < json.threads.dead.length; jj++) {
							var theThread = json.threads.dead[jj];
							var deadLi = createChunk("#" + theThread.id, "li");
							//deadLi.appendChild(createChunk(theThread.text,"pre"));
							deadThreads[theThread.id] = theThread.text;
							deadul.appendChild(deadLi);
						}
						header.appendChild(deadul);
						stack.appendChild(header);
					}
					for (id in json.threads.all) {
						var t = json.threads.all[id];
						var thread = createChunk(null, "div", "adminThread");
						var tid;
						thread.appendChild(tid = createChunk(id, "span", "adminThreadId"));
						if (deadThreads[id]) {
							tid.className = tid.className + " deadThread";
						}
						thread.appendChild(createChunk(t.name, "span", "adminThreadName"));
						thread.appendChild(createChunk(stui.str(t.state), "span", "adminThreadState_" + t.state));
						thread.onclick = (function(t, id) {
							return (function() {
								stack.innerHTML = "<b>" + id + ":" + t.name + "</b>\n";
								if (deadThreads[id]) {
									stack.appendChild(createChunk(deadThreads[id], "pre", "deadThreadInfo"));
								}
								stack.appendChild(createChunk("\n\n```\n", "pre", "textForTrac"));
								for (var q in t.stack) {
									stack.innerHTML = stack.innerHTML + t.stack[q] + "\n";
								}
								stack.appendChild(createChunk("```\n\n", "pre", "textForTrac"));
							});
						})(t, id);
						frag2.appendChild(thread);
					}
	
					removeAllChildNodes(u);
					u.appendChild(frag2);
				}
			});
		});
	
		addAdminPanel("admin_exceptions", function(div) {
			var frag = document.createDocumentFragment();
	
			div.className = "adminThreads";
			var v = createChunk(null, "div", "adminExceptionList");
			var stack = createChunk(null, "div", "adminThreadStack");
			frag.appendChild(v);
			var u = createChunk(null, "div");
			v.appendChild(u);
			frag.appendChild(stack);
	
			var c2s = createChunk(stui.str("clickToSelect"), "button", "clickToSelect");
			clickToSelect(c2s, stack);
	
			removeAllChildNodes(div);
			div.appendChild(c2s);
			var clicked = null;
	
			var last = -1;
	
			var exceptions = [];
	
			var exceptionNames = {};
	
			div.appendChild(frag);
			var more = createChunk(stui_str("more_exceptions"), "p", "adminExceptionMore adminExceptionFooter");
			var loading = createChunk(stui_str("loading"), "p", "adminExceptionFooter");
	
			v.appendChild(loading);
			var loadNext = function(from) {
				var append = "do=exceptions";
				if (from) {
					append = append + "&before=" + from;
				}
				console.log("Loading: " + append);
				loadOrFail(append, u, function(json) {
					if (!json || !json.exceptions || !json.exceptions.entry) {
						if (!from) {
							v.appendChild(createChunk(stui_str("no_exceptions"), "p", "adminExceptionFooter"));
						} else {
							v.removeChild(loading);
							v.appendChild(createChunk(stui_str("last_exception"), "p", "adminExceptionFooter"));
							// just the last one.
						}
					} else {
						if (json.exceptions.entry.time == from) {
							console.log("Asked for <" + from + " but got =" + from);
							v.removeChild(loading);
							return; //
						}
						var frag2 = document.createDocumentFragment();
						if (!from) {
							removeAllChildNodes(stack);
							stack.innerHTML = stui.str("adminClickToViewExceptions");
						}
						// TODO: if(json.threads.dead) frag2.appendChunk(json.threads.dead.toString(),"span","adminDeadThreads");
						last = json.exceptions.lastTime;
						if (json.exceptions.entry) {
							var e = json.exceptions.entry;
							exceptions.push(json.exceptions.entry);
							var exception = createChunk(null, "div", "adminException");
							if (e.header && e.header.length < 80) {
								exception.appendChild(createChunk(e.header, "span", "adminExceptionHeader"));
							} else {
								var t;
								exception.appendChild(t = createChunk(e.header.substring(0, 80) + "...", "span", "adminExceptionHeader"));
								t.title = e.header;
							}
							exception.appendChild(createChunk(e.DATE, "span", "adminExceptionDate"));
							var clicky = (function(e) {
								return (function(ee) {
									var frag3 = document.createDocumentFragment();
									frag3.appendChild(createChunk(e.header, "span", "adminExceptionHeader"));
									frag3.appendChild(createChunk(e.DATE, "span", "adminExceptionDate"));
	
									if (e.UPTIME) {
										frag3.appendChild(createChunk(e.UPTIME, "span", "adminExceptionUptime"));
									}
									if (e.CTX) {
										frag3.appendChild(createChunk(e.CTX, "span", "adminExceptionUptime"));
									}
									for (var q in e.fields) {
										var f = e.fields[q];
										var k = Object.keys(f);
										frag3.appendChild(createChunk(k[0], "h4", "textForTrac"));
										frag3.appendChild(createChunk("\n```", "pre", "textForTrac"));
										frag3.appendChild(createChunk(f[k[0]], "pre", "adminException" + k[0]));
										frag3.appendChild(createChunk("```\n", "pre", "textForTrac"));
									}
	
									if (e.LOGSITE) {
										frag3.appendChild(createChunk("LOGSITE\n", "h4", "textForTrac"));
										frag3.appendChild(createChunk("\n```", "pre", "textForTrac"));
										frag3.appendChild(createChunk(e.LOGSITE, "pre", "adminExceptionLogsite"));
										frag3.appendChild(createChunk("```\n", "pre", "textForTrac"));
									}
									removeAllChildNodes(stack);
									stack.appendChild(frag3);
									stStopPropagation(ee);
									return false;
								});
							})(e);
							listenFor(exception, "click", clicky);
							var head = exceptionNames[e.header];
							if (head) {
								if (!head.others) {
									head.others = [];
									head.count = document.createTextNode("");
									var countSpan = document.createElement("span");
									countSpan.appendChild(head.count);
									countSpan.className = "adminExceptionCount";
									listenFor(countSpan, "click", function(e) {
										// prepare div
										if (!head.otherdiv) {
											head.otherdiv = createChunk(null, "div", "adminExceptionOtherList");
											head.otherdiv.appendChild(createChunk(stui.str("adminExceptionDupList"), "h4"));
											for (k in head.others) {
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
								head.count.nodeValue = stui.sub("adminExceptionDup", [head.others.length]);
								head.otherdiv = null; // reset
							} else {
								frag2.appendChild(exception);
								exceptionNames[e.header] = exception;
							}
						}
						u.appendChild(frag2);
	
						if (json.exceptions.entry && json.exceptions.entry.time) {
							if (exceptions.length > 0 && (exceptions.length % 8 == 0)) {
								v.removeChild(loading);
								v.appendChild(more);
								more.onclick = more.onmouseover = function() {
									v.removeChild(more);
									v.appendChild(loading);
									loadNext(json.exceptions.entry.time);
									return false;
								};
							} else {
								setTimeout(function() {
									loadNext(json.exceptions.entry.time);
								}, 500);
							}
						}
					}
				});
			};
			loadNext(); // load the first exception
		});
	
		addAdminPanel("admin_settings", function(div) {
			var frag = document.createDocumentFragment();
	
			div.className = "adminSettings";
			var u = createChunk("Loading...", "div", "adminSettingsList");
			frag.appendChild(u);
			loadOrFail("do=settings", u, function(json) {
				if (!json || !json.settings || Object.keys(json.settings.all) == 0) {
					removeAllChildNodes(u);
					u.appendChild(document.createTextNode(stui.str("nosettings")));
				} else {
					var frag2 = document.createDocumentFragment();
					for (id in json.settings.all) {
						var t = json.settings.all[id];
	
						var thread = createChunk(null, "div", "adminSetting");
	
						thread.appendChild(createChunk(id, "span", "adminSettingId"));
						if (id == "CLDR_HEADER") {
							(function(theHeader, theValue) {
								var setHeader = null;
								setHeader = appendInputBox(thread, "adminSettingsChangeTemp");
								setHeader.value = theValue;
								setHeader.stChange = function(onOk, onErr) {
									loadOrFail("do=settings_set&setting=" + theHeader, u, function(json) {
										if (!json || !json.settings_set || !json.settings_set.ok) {
											onErr(stui_str("failed"));
											onErr(json.settings_set.err);
										} else {
											if (json.settings_set[theHeader]) {
												setHeader.value = json.settings_set[theHeader];
												if (theHeader == "CLDR_HEADER") {
													updateSpecialHeader(setHeader.value);
												}
											} else {
												setHeader.value = "";
												if (theHeader == "CLDR_HEADER") {
													updateSpecialHeader(null);
												}
											}
											onOk(stui_str("changed"));
										}
									}, setHeader.value);
									return false;
								};
							})(id, t); // call it
	
							if (id == "CLDR_HEADER") {
								updateSpecialHeader(t);
							}
						} else {
							thread.appendChild(createChunk(t, "span", "adminSettingValue"));
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
	
			div.className = "adminThreads";
	
			var baseUrl = contextPath + "/AdminPanel.jsp?vap=" + vap + "&do=";
			var hashSuff = ""; //  "#" + window.location.hash;
	
			var actions = ["rawload"];
			for (var k in actions) {
				var action = actions[k];
				var newUrl = baseUrl + action + hashSuff;
				var b = createChunk(stui_str(action), "button");
				b.onclick = function() {
					window.location = newUrl;
					return false;
				};
				frag.appendChild(b);
			}
			removeAllChildNodes(div);
			div.appendChild(frag);
	
		});
	
		// last panel loaded.
		// If it's in the hashtag, use it, otherwise first.
		if (window.location.hash && window.location.hash.indexOf("#!") == 0) {
			panelSwitch(window.location.hash.substring(2));
		}
		if (!panelLast) { // not able to load anything.
			panelSwitch(panelFirst.type);
		}
		adminStuff.appendChild(content);
	});
}

/**
 * Update the counter on top of the vetting page
 */
function refreshCounterVetting() {
	if (isVisitor || isDashboard()) {
		// if the user is a visitor, or this is the Dashboard, don't display the counter informations
		$('#nav-page .counter-infos, #nav-page .nav-progress').hide();
		return;
	}

	var inputs = $('.vetting-page input:visible:checked');
	var total = inputs.length;
	var abstain = inputs.filter(function() {
		return this.id.substr(0, 2) === 'NO';
	}).length;
	var voted = total - abstain;

	document.getElementById('count-total').innerHTML = total;
	document.getElementById('count-abstain').innerHTML = abstain;
	document.getElementById('count-voted').innerHTML = voted;
	if (total === 0) {
		total = 1;
	}
	document.getElementById('progress-voted').style.width = voted * 100 / total + '%';
	document.getElementById('progress-abstain').style.width = abstain * 100 / total + '%';

	if (cldrStForum && surveyCurrentLocale && surveyUser && surveyUser.id) {
		const forumSummary = cldrStForum.getForumSummaryHtml(surveyCurrentLocale, surveyUser.id, false);
		document.getElementById('vForum').innerHTML = forumSummary;
	}
}

/**
 * Go to the next (1) or the previous page (1) during the vetting
 *
 * @param {Integer} shift next page (1) or previous (-1)
 */
function chgPage(shift) {
	// no page, or wrong shift
	if (!_thePages || (shift !== -1 && shift !== 1)) {
		return;
	}

	var menus = getMenusFilteredByCov();
	var parentIndex = 0;
	var index = 0;
	var parent = _thePages.pageToSection[surveyCurrentPage].id;

	// get the parent index
	for (var m in menus) {
		var menu = menus[m];
		if (menu.id === parent) {
			parentIndex = parseInt(m);
			break;
		}
	}

	for (var m in menus[parentIndex].pagesFiltered) {
		var menu = menus[parentIndex].pagesFiltered[m];
		if (menu.id === surveyCurrentPage) {
			index = parseInt(m);
			break;
		}
	}
	// go to the next one
	index += parseInt(shift);

	if (index >= menus[parentIndex].pagesFiltered.length) {
		parentIndex++;
		index = 0;
		if (parentIndex >= menus.length) {
			parentIndex = 0;
		}
	}

	if (index < 0) {
		parentIndex--;
		if (parentIndex < 0) {
			parentIndex = menus.length - 1;
		}
		index = menus[parentIndex].pagesFiltered.length - 1;
	}
	surveyCurrentSection = menus[parentIndex].id;
	surveyCurrentPage = menus[parentIndex].pagesFiltered[index].id;

	reloadV();

	var sidebar = $('#locale-menu #' + surveyCurrentPage);
	sidebar.closest('.open-menu').click();
}

/**
 * Get all the menus under this coverage
 *
 * @return {Array} list of all the menus under this coverage
 */
function getMenusFilteredByCov() {
	if (!_thePages) {
		return;
	}
	// get name of current coverage
	var cov = surveyUserCov;
	if (!cov) {
		cov = surveyOrgCov;
	}

	// get the value
	var val = covValue(cov);
	var sections = _thePages.sections;
	var menus = [];
	// add filtered pages
	for (var s in sections) {
		var section = sections[s];
		var pages = section.pages;
		var sectionContent = [];
		for (var p in pages) {
			var page = pages[p];
			var key = Object.keys(page.levs).pop();
			if (parseInt(page.levs[key]) <= val)
				sectionContent.push(page);
		}
		if (sectionContent.length) {
			section.pagesFiltered = sectionContent;
			menus.push(section);
		}
	}
	return menus;
}

///////////////////

/**
 * For vetting
 *
 * @param hideRegex
 */
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
			if (sel != undefined && sel.match(/vv/)) {
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
	for (var i = 0; i < document.checkboxes.elements.length; i++) {
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
	var cl = createChunk(locName, "a", "localeChunk " + className);
	cl.title = loc;
	cl.href = "survey?_=" + loc;
	return cl;
}

function showAllItems(divName, user) {
	require(["dojo/ready"], function(ready) {
		ready(function() {
			loadStui();
			var div = document.getElementById(divName);
			div.className = "recentList";
			div.update = function() {
				var ourUrl = contextPath + "/SurveyAjax?what=mylocales&user=" + user;
				var errorHandler = function(err) {
					let responseText = cldrStAjax.errResponseText(err);
					handleDisconnect('Error in showrecent: ' + err + ' response ' + responseText);
				};
				showLoader(null, "Loading recent items");
				var loadHandler = function(json) {
					try {
						if (json && json.mine) {
							var frag = document.createDocumentFragment();
							var header = json.mine.header;
							var data = json.mine.data;
							if (data.length == 0) {
								frag.appendChild(createChunk(stui_str("recentNone"), "i"));
							} else {
								var rowDiv = document.createElement("div");
								frag.appendChild(rowDiv);

								rowDiv.appendChild(createChunk(stui_str("recentLoc"), "b"));
								rowDiv.appendChild(createChunk(stui_str("recentCount"), "b"));

								for (var q in data) {
									var row = data[q];

									var count = row[header.COUNT];

									var rowDiv = document.createElement("div");
									frag.appendChild(rowDiv);

									var loc = row[header.LOCALE];
									var locname = row[header.LOCALE_NAME];
									rowDiv.appendChild(createLocLink(loc, locname, "recentLoc"));
									rowDiv.appendChild(createChunk(count, "span", "value recentCount"));

									if (surveySessionId != null) {
										var dlLink = createChunk(stui_str("downloadXmlLink"), "a", "notselected");
										dlLink.href = "DataExport.jsp?do=myxml&_=" + loc + "&user=" + user + "&s=" + surveySessionId;
										dlLink.target = "STDownload";
										rowDiv.appendChild(dlLink);
									}
								}
							}

							removeAllChildNodes(div);
							div.appendChild(frag);
							hideLoader(null);
						} else {
							handleDisconnect("Failed to load JSON recent items", json);
						}
					} catch (e) {
						console.log("Error in ajax get ", e.message);
						console.log(" response: " + text);
						handleDisconnect(" exception in getrecent: " + e.message, null);
					}
				};
				var xhrArgs = {
					url: ourUrl,
					handleAs: "json",
					load: loadHandler,
					error: errorHandler
				};
				cldrStAjax.queueXhr(xhrArgs);
			};
			div.update();
		});
	});
}

function showRecent(divName, locale, user) {
	if (!locale) {
		locale = '';
	}
	if (!user) {
		user = '';
	}
	require(["dojo/ready"], function(ready) {
		ready(function() {
			loadStui();
			var div;
			if (divName.nodeType > 0) {
				div = divName;
			} else {
				div = document.getElementById(divName);
			}
			div.className = "recentList";
			div.update = function() {
				var ourUrl = contextPath + "/SurveyAjax?what=recent_items&_=" + locale + "&user=" + user + "&limit=" + 15;
				var errorHandler = function(err) {
					let responseText = cldrStAjax.errResponseText(err);
					handleDisconnect('Error in showrecent: ' + err + ' response ' + responseText);
				};
				showLoader(null, "Loading recent items");
				var loadHandler = function(json) {
					try {
						if (json && json.recent) {
							var frag = document.createDocumentFragment();
							var header = json.recent.header;
							var data = json.recent.data;

							if (data.length == 0) {
								frag.appendChild(createChunk(stui_str("recentNone"), "i"));
							} else {
								var rowDiv = document.createElement("div");
								frag.appendChild(rowDiv);

								rowDiv.appendChild(createChunk(stui_str("recentLoc"), "b"));
								rowDiv.appendChild(createChunk(stui_str("recentXpathCode"), "b"));
								rowDiv.appendChild(createChunk(stui_str("recentValue"), "b"));
								rowDiv.appendChild(createChunk(stui_str("recentWhen"), "b"));

								for (var q in data) {
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
									rowDiv.appendChild(createLocLink(loc, locname, "recentLoc"));
									var xpathItem;
									xpath_code = xpath_code.replace(/\t/g, " / ");
									rowDiv.appendChild(xpathItem = createChunk(xpath_code, "a", "recentXpath"));
									xpathItem.href = "survey?_=" + loc + "&strid=" + xpath_hash;
									rowDiv.appendChild(createChunk(value, "span", "value recentValue"));
									rowDiv.appendChild(createChunk(new Date(last_mod).toLocaleString(), "span", "recentWhen"));
								}
							}
							removeAllChildNodes(div);
							div.appendChild(frag);
							hideLoader(null);
						} else {
							handleDisconnect("Failed to load JSON recent items", json);
						}
					} catch (e) {
						console.log("Error in ajax get ", e.message);
						console.log(" response: " + text);
						handleDisconnect(" exception in getrecent: " + e.message, null);
					}
				};
				var xhrArgs = {
					url: ourUrl,
					handleAs: "json",
					load: loadHandler,
					error: errorHandler
				};
				cldrStAjax.queueXhr(xhrArgs);
			};
			div.update();
		});
	});
}

/**
 * For the admin page
 *
 * @param list
 * @param tableRef
 * @returns
 */
function showUserActivity(list, tableRef) {
	loadStui(null, function( /* stui */ ) {
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
			) {
				ready(function() {
					window._userlist = list; // DEBUG
					var table = dom.byId(tableRef);

					var rows = [];
					var theadChildren = getTagChildren(table.getElementsByTagName("thead")[0].getElementsByTagName("tr")[0]);

					setDisplayed(theadChildren[1], false);
					var rowById = [];

					for (var k in list) {
						var user = list[k];
						var tr = dom.byId('u@' + user.id);

						rowById[user.id] = parseInt(k); // ?!

						var rowChildren = getTagChildren(tr);

						removeAllChildNodes(rowChildren[1]); // org
						removeAllChildNodes(rowChildren[2]); // name

						var theUser;
						setDisplayed(rowChildren[1], false);
						rowChildren[2].appendChild(theUser = createUser(user));

						rows.push({
							user: user,
							tr: tr,
							userDiv: theUser,
							seen: rowChildren[5],
							stats: [],
							total: 0
						});
					}

					window._rrowById = rowById;

					var loc2name = {};
					request.get(contextPath + "/SurveyAjax?what=stats_bydayuserloc", {
						handleAs: 'json'
					}).then(function(json) {
						/* COUNT: 1120,  DAY: 2013-04-30, LOCALE: km, LOCALE_NAME: khmer, SUBMITTER: 2 */
						var stats = json.stats_bydayuserloc;
						var header = stats.header;
						for (var k in stats.data) {
							var row = stats.data[k];
							var submitter = row[header.SUBMITTER];
							var submitterRow = rowById[submitter];
							if (submitterRow !== undefined) {
								var userRow = rows[submitterRow];
								userRow.stats.push({
									day: row[header.DAY],
									count: row[header.COUNT],
									locale: row[header.LOCALE]
								});
								userRow.total = userRow.total + row[header.COUNT];
								loc2name[row[header.LOCALE]] = row[header.LOCALE_NAME];
							}
						}

						function appendMiniChart(userRow, count) {
							if (count > userRow.stats.length) {
								count = userRow.stats.length;
							}
							removeAllChildNodes(userRow.seenSub);
							for (var k = 0; k < count; k++) {
								var theStat = userRow.stats[k];
								var chartRow = createChunk('', 'div', 'chartRow');

								var chartDay = createChunk(theStat.day, 'span', 'chartDay');
								var chartLoc = createChunk(theStat.locale, 'span', 'chartLoc');
								chartLoc.title = loc2name[theStat.locale];
								var chartCount = createChunk(dojoNumber.format(theStat.count), 'span', 'chartCount');

								chartRow.appendChild(chartDay);
								chartRow.appendChild(chartLoc);
								chartRow.appendChild(chartCount);

								userRow.seenSub.appendChild(chartRow);
							}
							if (count < userRow.stats.length) {
								chartRow.appendChild(document.createTextNode('...'));
							}
						}

						for (var k in rows) {
							var userRow = rows[k];
							if (userRow.total > 0) {
								addClass(userRow.tr, "hadActivity");
								userRow.tr.getElementsByClassName('recentActivity')[0].appendChild(document.createTextNode(' (' +
									dojoNumber.format(userRow.total) + ')'));

								userRow.seenSub = document.createElement('div');
								userRow.seenSub.className = 'seenSub';
								userRow.seen.appendChild(userRow.seenSub);

								appendMiniChart(userRow, 3);
								if (userRow.stats.length > 3) {
									var chartMore, chartLess;
									chartMore = createChunk('+', 'span', 'chartMore');
									chartLess = createChunk('-', 'span', 'chartMore');
									chartMore.onclick = (function(chartMore, chartLess, userRow) {
										return function() {
											setDisplayed(chartMore, false);
											setDisplayed(chartLess, true);
											appendMiniChart(userRow, userRow.stats.length);
											return false;
										};
									})(chartMore, chartLess, userRow);
									chartLess.onclick = (function(chartMore, chartLess, userRow) {
										return function() {
											setDisplayed(chartMore, true);
											setDisplayed(chartLess, false);
											appendMiniChart(userRow, 3);
											return false;
										};
									})(chartMore, chartLess, userRow);
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

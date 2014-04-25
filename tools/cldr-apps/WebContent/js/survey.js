// survey.js  -Copyright (C) 2012-2014 IBM Corporation and Others. All Rights Reserved.
// move anything that's not dynamically generated here.

// These need to be available @ bootstrap time.

/**
 * @module survey.js - SurveyTool main JavaScript stuff
 */
 

// TODO: replace with AMD [?] loading
dojo.require("dojo.i18n");
dojo.require("dojo.string");
dojo.requireLocalization("surveyTool", "stui");

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
	if(obj.className.indexOf(className)>-1) {
		obj.className = obj.className.substring(className.length+1);
	}
}

/**
 * @method addClass
 * add a CSS class from a node
 * @param {Node} obj
 * @param {String} className
 */
function addClass(obj, className) {
	if(obj.className.indexOf(className)==-1) {
		obj.className = className+" "+obj.className;
	}
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
//	} else if(!div.style.display) {
//		console.log("setDisplayed: called on malformed node " + div + " - no display! " + Object.keys(div.style));
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
};

var stuidebug_enabled=(window.location.search.indexOf('&stui_debug=')>-1);


if(!stuidebug_enabled) {
	/**
	 * SurveyToolUI string loading function
	 * @method stui_str
	 */
	stui_str = function(x) {
	    if(stui && stui[x]) {
	    	return stui[x];
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
		//chunk.title=stui_str(firstword(className)+"_desc");
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
}

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

/**
 * Create a DOM object referring to a user.
 * @method createUser
 * @param {JSON} user - user struct
 * @return {Object} new DOM object
 */
function createUser(user) {
	var div = createChunk(null,"div","adminUserUser");
	if(user.emailHash) {
		var gravatar = document.createElement("img");
		gravatar.src = 'http://www.gravatar.com/avatar/'+user.emailHash+'?d=identicon&r=g&s=32';
		gravatar.title = 'gravatar - http://www.gravatar.com';
		gravatar.align='laft';		
		div.appendChild(gravatar);
	}
	div.appendChild(createChunk(stui_str("userlevel_"+user.userlevelName.toLowerCase(0)),"i","userlevel_"+user.userlevelName.toLowerCase()));
	div.appendChild(createChunk(user.name,"span","adminUserName"));
	div.appendChild(createChunk(user.orgName + ' #'+user.id,"span","adminOrgName"));
	div.appendChild(createChunk(user.email,"address","adminUserAddress"));
	return div;
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
var stdebug_enabled=(window.location.search.indexOf('&stdebug=')>-1);

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
		if(top.postData) {
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
        //console.log("STATUS>: " + json.localeStampName + "="+json.localeStamp);
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
		//popupAlert('warning',ajaxWord);
	} else if(!progressWord || progressWord == "ok") {
		if(specialHeader) {
			p.className = "progress-special";
			popupAlert('success',specialHeader);
		} else {
			p.className = "progress-ok";
			popupAlert('warning',stui_str('online'));
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
			{
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
		var url = contextPath + "/survey"+cacheKill();
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
			msg_str = "E_UNKNOWN";
		}
	}
	return stui.sub(msg_str,
			{
				json: json, what: stui.str('err_what_'+subkey), code: theCode,
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
//	stdebug("UpdateStatus...");
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
            	}else if(wasBusted == true && 
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
//            var st_err =  document.getElementById('st_err');
            wasBusted = true;
//            st_err.className = "ferrbox";
//            st_err.innerHTML="Disconnected from Survey Tool: "+err.name + " <br> " + err.message;
            updateStatusBox({err: err.message, err_name: err.name, disconnected: true});
//            updateIf('uptime','down');
//            updateIf('visitors','nobody');
        }
    });
}

/**
 * Fire up the main timer loop to update status
 * @method setTimerOn
 */
function setTimerOn() {
    updateStatus();
//    timerID = setInterval(updateStatus, timerSpeed);
    
}

/**
 * Change the update timer's speed
 * @method resetTimerSpeed
 * @param {Int} speed
 */
function resetTimerSpeed(speed) {
	timerSpeed = speed;
//	clearInterval(timerID);
//	timerID = setInterval(updateStatus, timerSpeed);
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
    ALLOW: 									   { vote: true, ticket: false, change: true  }, 
    ALLOW_VOTING_AND_TICKET:   { vote: true, ticket: true, change: false },
    ALLOW_VOTING_BUT_NO_ADD: { vote: true, ticket: false, change: false },
    ALLOW_TICKET_ONLY : { vote: false, ticket: true, change: true },
    //FORBID_ERRORS: {}, 
    //FORBID_READONLY:{}, 
    //FORBID_COVERAGE:{}
    DEFAULT: { vote: false, ticket: false, change: false}
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
	if(o&&o.childNodes) for(var i=0;i<o.childNodes.length;i++) {
		var k = o.childNodes[i];
		if(k.id && k.id.indexOf("stui-html")==0) {
			var key = k.id.slice(5);
			if(stui[key]) {
				k.innerHTML=stui[key];
			}
			k.id=null;
		} else {
			localizeAnon(k);
		}
	}
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
			if(stui[key]) {
				k.title=stui[key];
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
//			} else if(e.keyCode ==9) { // TAB
//				handleWiredClick(tr,theRow,vHash,box,button); 
//				return false;
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
 * wire up the button to perform a cancel
 * @method wireUpButton
 * @param button
 * @param tr
 * @param theRow
 * @param vHash
 * @param box
 */
function wireUpCancelButton(button, tr, theRow, vHash) {
	if(vHash==null) {
		button.id="C_NO_" + tr.rowHash;
		vHash="";
	} else {
		button.id = "c"+vHash+"_"+tr.rowHash;
	}
	listenFor(button,"click",
			function(e){ handleCancelWiredClick(tr,theRow,vHash,button); stStopPropagation(e); return false; });
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
	//stdebug("PT@"+gPopStatus.popToken+" - " + x);
	return gPopStatus.popToken;
}


//function hidePopHandler(e){ 		
//	window.hidePop(null);
//	stStopPropagation(e); return false; 
//}

// timeout for showing sideways view
var sidewaysShowTimeout = -1;

/**
 * @method showForumStuff
 * called when showing the popup each time
 * @param {Node} frag
 * @param {Node} forumDiv
 * @param {Node} tr
 */
function showForumStuff(frag, forumDiv, tr) {
	{
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
				//updateIf(sidewaysControl, JSON.stringify(json));
				if(!json.others) {
					updateIf(sidewaysControl, ""); // no sibling locales (or all null?)
				} else {
					var theMsg = null;
					if(Object.keys(json.others).length == 1) {
						theMsg = stui.str("sideways_same");
						addClass(sidewaysControl, "sideways_same");
					} else {
						theMsg = stui.str("sideways_diff");
						addClass(sidewaysControl, "sideways_diff");
					}
					updateIf(sidewaysControl, ""); // remove string
					
					//var popupArea = document.createElement("div");
					//addClass(popupArea, "sideways_popup");
					//sidewaysControl.appendChild(popupArea); // will be initially hidden
					var appendLocaleList = function appendLocaleList(list, name, title) {
						var group = document.createElement("optGroup");
						group.setAttribute("label", name +  " (" + list.length + ")");
						group.setAttribute("title", title);
						list.sort(); // at least sort the locale ids
						for(var l=0;l<list.length;l++) {
							var loc = list[l];
							var item = document.createElement("option");
							item.setAttribute("value",loc);
							var str = locmap.getRegionAndOrVariantName(loc);
							if(loc === surveyCurrentLocale) {
								str = str + ": " + theMsg;
								item.setAttribute("selected", "selected");
								item.setAttribute("title",'"'+s+'"');
							} else {
				        		var bund = locmap.getLocaleInfo(loc);
				        		if(bund && bund.readonly) {
			        				addClass(item, "locked");
			        				item.setAttribute("disabled","disabled");
			        				item.setAttribute("title",stui.str("readonlyGuidance"));
				        		} else {
				        			item.setAttribute("title",'"'+s+'"');
				        		}
							}							
							item.appendChild(document.createTextNode(str));
							group.appendChild(item);
						}
						popupSelect.appendChild(group);
					}
					
					var popupSelect = document.createElement("select");
					for(var s in json.others) {
						appendLocaleList(json.others[s], s, s);
						//console.log("k:" + s + " in " + JSON.stringify(json.others));
					}
					if(json.novalue) {
						appendLocaleList(json.novalue,  stui.str("sideways_noValue"),  stui.str("sideways_noValue"));
					}					
					
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
			});
		}, 2000); // wait 2 seconds before loading this.
	}
	
	// prepend something
	var buttonTitle = "forumNewPostButton";
	var buttonClass = "forumNewButton";
	if(tr.theRow) {
		if(tr.theRow.voteVhash !== tr.theRow.winningVhash 
				&& tr.theRow.canFlagOnLosing && 
				!tr.theRow.rowFlagged) {
			buttonTitle = "forumNewPostFlagButton";
			buttonClass = "forumNewPostFlagButton";
		}
	}
	var newButton = createChunk(stui.str(buttonTitle), "button", buttonClass);
	frag.appendChild(newButton);
	
	listenFor(newButton, "click", function(e) {
		//window.blur(); // submit anything unsubmitted
		window.open(tr.forumDiv.postUrl);
		stStopPropagation(e);
		return true;
	});
	
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
							for(num in json.ret) {
								var post = json.ret[num];
//								userChunk.style.float = "right";
//								forumDiv.appendChild(createChunk("Id:","b"));
//								forumDiv.appendChild(document.createElement("br"));
								
								var subpost = createChunk("","div","subpost");
								forumDiv.appendChild(subpost);
								
								var headerLine = createChunk("","div","postHeaderLine");
								subpost.appendChild(headerLine);
								
								var userChunk = createUser(post.posterInfo);
								headerLine.appendChild(userChunk);
								
								var subSubChunk = createChunk("","div","postHeaderInfoGroup");
								headerLine.appendChild(subSubChunk);
								{
									var subChunk = createChunk("","div","postHeaderItem");
									subSubChunk.appendChild(subChunk);
									subChunk.appendChild(createChunk("Date:","b"));
									subChunk.appendChild(createChunk(post.date,"span","postHeader"));
									subChunk.appendChild(document.createElement("br"));
								}
								{
									var subChunk = createChunk("","div","postHeaderItem");
									subSubChunk.appendChild(subChunk);
									subChunk.appendChild(createChunk("Subject:","b"));
									subChunk.appendChild(createChunk(post.subject,"span","postHeader"));
									subChunk.appendChild(document.createElement("br"));
								}
																
								// actual text
								subpost.appendChild(createChunk(post.text, "div","postContent"));
								
								// reply link
								var replyChunk = createChunk("Reply (leaves this page)","a","postReply");
								replyChunk.href = tr.forumDiv.replyStub + post.id;
								subpost.appendChild(replyChunk);
								
							}
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
			// window.xhrArgs = xhrArgs;
			// console.log('xhrArgs = ' + xhrArgs);
			queueXhr(xhrArgs);
				
			
			stStopPropagation(e);
			return false;
		};
		listenFor(showButton, "click", theListen);
		listenFor(showButton, "mouseover", theListen);
	}

	// lazy load post count!
	{
		// load async
		var ourUrl = tr.forumDiv.url + "&what=forum_count" + cacheKill() ;
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
	}
	
	
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
	forumDiv.postUrl = forumDiv.replyStub + "x"+theRow.xpid;
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
//	    replaceHash();
    }
};

// window loader stuff
dojo.ready(function() {
	var unShow = null;
//	var lastShown = null;
	var pucontent = dojo.byId("itemInfo");

//	var nudgev=0;
//	var nudgehpost=0;
//	var nudgevpost=0;
//	var hardleft = 10;
//	var hardtop = 10;
//	var pupeak_height = pupeak.offsetHeight;
	if(!pucontent) return;

	//pucontent.className = "oldFloater";
	var hideInterval=null;
	
	function parentOfType(tag, obj) {
		if(!obj) return null;
//		console.log('POT ' + tag + '-' + obj + '=' + obj.nodeName);
		if(obj.nodeName===tag) return obj;
		return parentOfType(tag, obj.parentElement);
	}
	
	function setLastShown(obj) {
		if(gPopStatus.lastShown && obj!=gPopStatus.lastShown) {
			removeClass(gPopStatus.lastShown,"pu-select");
			//addClass(gPopStatus.lastShown,"pu-deselect");
			var partr = parentOfType('TR',gPopStatus.lastShown);
			if(partr) {
//				console.log('Removing select from ' + partr + ' ' + partr.id);
				removeClass(partr, 'selectShow');
			}
		}
		if(obj) {
			//removeClass(obj,"pu-deselect");
			addClass(obj,"pu-select");
			var partr = parentOfType('TR',obj);
			if(partr) {
//				console.log('Adding select  to ' + partr + ' ' + partr.id);
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
	 * This is the actual function is called to display the right-hand "info" panel.
	 * It is defined dynamically because it depends on variables that aren't available at startup time. 
	 * @param {String} str the string to show at the top
	 * @param {Node} tr the <TR> of the row
	 * @param {Boolean} hideIfLast 
	 * @param {Function} fn 
	 * @param {Boolean} immediate
	 */
	window.showInPop2 = function(str, tr, hideIfLast, fn, immediate, hide) {
//		if(hideIfLast&&lastShown==hideIfLast) {
//			return; // keep up
//		}
		
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
				
			if(theRow.xpstrid /*&& theRow.displayHelp*/) {
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
				
				
//				tr.helpDiv.innerHTML += theRow.displayHelp;
				
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
		    	$(this).html('<a href="mailto:'+email+'" title="'+email+'" style="color:black"><span class="glyphicon glyphicon-envelope"></span></a>');
		    	$(this).children('a').tooltip().tooltip('show');
		    }, function() {
		    	$(this).html($(this).data('name'));
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
			} else {
//				hideInterval=setTimeout(function() {/
//					window.showInPop2(str,tr,hideIfLast,fn);
//				}, 2500);
			}
		};
	
//	window.hidePop = function() {
//		if(hideInterval) {
//			clearTimeout(hideInterval);
//		}
//		hideInterval=setTimeout(function() {
//			if(false) {
//				//pucontent.style.display="none";
//			} else {
//				// SRL suspicious
//				removeAllChildNodes(pucontent);
////				pupeak.style.display="none";
//			}
//			clearLastShown();
//			incrPopToken('newHide');
//		}, 2000);
//	};
	window.resetPop = function() {
		lastShown = null;
	};
});

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
	if(!value) { span.className = "selected"; } else if(pClass) {
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
		newHtml += "' title='" + testItem.type+"'>";
		if (testItem.type == 'Warning') {
			newHtml += warnIcon;
			// what='warn';
		} else if (testItem.type == 'Error') {
			//td.className = "tr_err";
			newHtml += stopIcon;
//			what = 'error';
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
		//(createChunk("(unknown testKind "+testKind+")" ,"i"));
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
	
//	stdebug("Searching for our value " + value );
	// Find where our value went.
	var ourItem = findItemByValue(theRow.items,value);
	
	var testKind = getTestKind(tests);
	var ourDiv = null;
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
			ourDiv.appendChild(newButton);
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
		if(ourItem) {
			str = stui.sub("StatusAction_msg",
					[ stui_str("StatusAction_"+json.statusAction) ],"p", "");
			showInPop(str, tr, null, null, true);
		}
	} else if(json&&json.didNotSubmit) {
		ourDiv.className = "d-item-err";
		showInPop("(ERROR: Unknown error - did not submit this value.)", tr, null, null, true);
		return;
	} else {
		setDivClass(ourDiv,testKind);
	}
//	theRow.proposedResults = null;

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
//		theRow.proposedResults = div3;
//		theRow.proposedResults.value = value;
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

		var h3 = document.createElement("div");
		var span = appendItem(h3, item.value, item.pClass); /* no need to pass in 'tr' - clicking this span would have no effect. */
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
		
		//return function(){ var d2 = div; return function(){ 	d2.className="d-item";  };}();
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
 * @param {String} vHash     stringid of the item
 * @param {DOM} newButton     button prototype object
 * @param {DOM} cancelButton     cancel button object
 */
function addVitem(td, tr, theRow, item, vHash, newButton, cancelButton) {
//	var canModify = tr.theTable.json.canModify;
	var div = document.createElement("div");
	var isWinner = (td==tr.proposedcell);
	var testKind = getTestKind(item.tests);
	setDivClass(div,testKind);
	item.div = div; // back link
	if(item==null)  {
//		div.innerHTML = "<i>null: "+theRow.winningVhash+" </i>";
		return;
	}
	var choiceField = document.createElement("div");
	choiceField.className = "choice-field";
	if(newButton) {
		newButton.value=item.value;
		wireUpButton(newButton,tr,theRow,vHash);
		choiceField.appendChild(newButton);
	}
    var subSpan = document.createElement("span");
    subSpan.className = "subSpan";
	var span = appendItem(subSpan,item.value,item.pClass,tr);
	choiceField.appendChild(subSpan);
	
	setLang(span);
	
	if(item.isOldValue==true && !isWinner) {
		addIcon(choiceField,"i-star");
	}
	if(item.votes && !isWinner) {
		addIcon(choiceField,"i-vote");

		if(vHash == theRow.voteVhash && theRow.canFlagOnLosing && !theRow.rowFlagged){
			var newIcon = addIcon(choiceField,"i-stop"); // DEBUG
			/*
			listenFor(newIcon, "click", function(e) {
				//window.blur(); // submit anything unsubmitted
				// TODO!
				window.open(tr.forumDiv.postUrl);
				stStopPropagation(e);
				return true;
			});
			 */
		}
	}
	if(newButton && 
			theRow.voteVhash == vHash &&
			vHash !== '' &&  // not 'no opinion'
			theRow.items[theRow.voteVhash].votes[surveyUser.id].overridedVotes) {
		var overrideTag = createChunk(theRow.items[theRow.voteVhash].votes[surveyUser.id].overridedVotes,"span","i-override");		
		choiceField.appendChild(overrideTag);
	}
	
	div.appendChild(choiceField);

	var inheritedClassName = "fallback";
	var defaultClassName = "fallback_code";
	
	if(cancelButton && !item.votes && item.isOldValue==false && 
	   item.pClass.substring(0, inheritedClassName.length)!=inheritedClassName && 
	   item.pClass.substring(0, defaultClassName.length)!=defaultClassName) {
		cancelButton.value=item.value;
		wireUpCancelButton(cancelButton,tr,theRow,vHash);
		choiceField.appendChild(cancelButton);
		$(cancelButton).tooltip();
	}

    // wire up the onclick
	td.showFn = item.showFn = showItemInfoFn(theRow,item,vHash,newButton,div);
	div.popParent = tr;
	listenToPop(null, tr, div, td.showFn);
	td.appendChild(div);
	
    if(item.example && item.value != item.examples ) {
		appendExample(div,item.example);
//		example.popParent = tr;
//		listenToPop(null,tr,example,td.showFn);
	}
	
	/*if(tr.canChange) {
	    var oldClassName = span.className = span.className + " editableHere";
	    ///span.title = span.title  + " " + stui_str("clickToChange");
	    var ieb = null;
	    var editInPlace = function(e) {
	        require(["dojo/ready", "dijit/InlineEditBox", "dijit/form/TextBox", "dijit/registry"],
	        function(ready, InlineEditBox, TextBox, registry) {
	    	    var spanId = span.id = dijit.registry.getUniqueId(); // bump the #, probably leaks something in dojo?
	    	    stdebug("Ready for " + spanId);
	            ready(function(){
	            if(!ieb) {
	                ieb = new InlineEditBox({
	                	dir: locInfo().dir,
	                	lang: locInfo().bcp47,
	                	editor: TextBox, 
	                	editorParams:  { dir: locInfo().dir, lang: locInfo().bcp47 },
	                	autoSave: true, 
	                    onChange: function (newValue) {
	                                  //  console.log("Destroyed -> "+ newValue);
	                                   // remove dojo stuff..
	                                   //removeAllChildNodes(subSpan);
	                                   // reattach the span
	                                   //subSpan.appendChild(span);
	                                   //span.className = oldClassName;
	                               tr.inputTd = td; // cause the proposed item to show up in the right box
                       				handleWiredClick(tr,theRow,"",{value: newValue},newButton); 
	                                   //ieb.destroy();
	                               },
                               onShow: function() {
                            	   setDefer(true);
                               },
                               onHide: function() {
                            	   setDefer(false);
                               }
	                   }, spanId);
	                		tr.iebs.push(ieb);
	                       stdebug("Minted  " + spanId);
	                   } else {
//	                       console.log("Leaving alone " + spanId);
	                   }
	            });
	        });
	    	stStopPropagation(e);
	    	return false;
	    };
	    
	    //listenFor(span, "mouseover", editInPlace);
	}*/
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

function updateRow(tr, theRow) {
	tr.theRow = theRow;
	tr.valueToItem = {}; // hash:  string value to item (which has a div)
	tr.rawValueToItem = {}; // hash:  string value to item (which has a div)
	for(var k in theRow.items) {
		var item = theRow.items[k];
		if(item.value) {
			tr.valueToItem[item.value] = item; // back link by value
			tr.rawValueToItem[item.rawValue] = item; // back link by value
		}
	}
	
	// update the vote info
	if(theRow.voteResolver) {
		var vr = theRow.voteResolver;
		var div = tr.voteDiv = document.createElement("div");
		tr.voteDiv.className = "voteDiv";
		
		//tr.voteDiv.appendChild(document.createElement("hr"));
		
		
		if(theRow.voteVhash && 
				theRow.voteVhash!=='') {
			var voteForItem = theRow.items[theRow.voteVhash];
			if(voteForItem.votes && voteForItem.votes[surveyUser.id].overridedVotes) {
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
		
		// TODO: lazy evaluate this clause?
		if(true /*theRow.voteResolver.orgs && Object.keys(theRow.voteResolver.orgs).length > 0*/) {
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
				console.log(n);
				if(n > 2)
					var valdiv = createChunk(null, "div", "value-div");
				else
					var valdiv = createChunk(null, "div", "value-div first")
				// heading row
					
				{
					//var valueExtra = (value==vr.winningValue)?(" voteInfo_iconValue voteInfo_winningItem d-dr-"+theRow.voteResolver.winningStatus):"";
					//var voteExtra = (value==vr.lastReleaseValue)?(" voteInfo_lastRelease"):"";
					var vrow = createChunk(null, "tr", "voteInfo_tr voteInfo_tr_heading");
					if(!item.votes || Object.keys(item.votes).length==0) {
						//vrow.appendChild(createChunk("","div","voteInfo_orgColumn voteInfo_td"));
					} else {
						vrow.appendChild(createChunk(stui.str("voteInfo_orgColumn"),"td","voteInfo_orgColumn voteInfo_td"));
					}
					var isection = createChunk(null, "div", "voteInfo_iconBar");
					//vrow.appendChild(isection);
					
					var vvalue = createChunk("User", "td", "voteInfo_valueTitle voteInfo_td");
					var vbadge = createChunk(vote, "span", "badge");
					if(value==vr.winningValue) {
						appendIcon(isection,"voteInfo_winningItem d-dr-"+theRow.voteResolver.winningStatus);
					}
					
					if(value==vr.lastReleaseValue) {
						appendIcon(isection,"voteInfo_lastRelease i-star");
					}
					
					if(value != vr.winningValue) {
							appendIcon(isection,"i-vote");
					}
					
					setLang(valdiv);
					appendItem(valdiv, value, calcPClass(value, vr.winningValue), tr);
					valdiv.appendChild(isection);
					vrow.appendChild(vvalue);
					
					var cell = createChunk(null,"td","voteInfo_voteTitle voteInfo_voteCount voteInfo_td"+"");
					cell.appendChild(vbadge);
					vrow.appendChild(cell);
					vdiv.appendChild(vrow);
				}
				
				var createVoter = function(v) {
					if(v==null) {
						return createChunk("(NULL)!","i","stopText");
					}
					var div = createChunk(v.name,"td","voteInfo_voterInfo voteInfo_td");
					div.setAttribute('data-name',v.name);
					div.setAttribute('data-email',v.email);
					return div;
				};
				
				if(!item.votes || Object.keys(item.votes).length==0) {
					var vrow = createChunk(null, "tr", "voteInfo_tr voteInfo_orgHeading");
					//vrow.appendChild(createChunk("","div","voteInfo_orgColumn voteInfo_td"));
					vrow.appendChild(createChunk(stui.str("voteInfo_noVotes"),"td","voteInfo_noVotes voteInfo_td"));
					
					//vrow.appendChild(createChunk("","div","voteInfo_orgColumn voteInfo_td"));
					vrow.appendChild(createChunk(null, "td","voteInfo_noVotes voteInfo_td"));
					vdiv.appendChild(vrow);
					
				} else {
					for(org in theRow.voteResolver.orgs) {
						var theOrg = vr.orgs[org];
						var orgVoteValue = theOrg.votes[value];
						if(orgVoteValue) { // someone in the org actually voted for it
							var topVoter = null; // top voter for this item
							var orgsVote = (theOrg.orgVote == value);
							if(orgsVote) {
								// find a top-ranking voter to use for the top line
								for(var voter in item.votes) {
									if(item.votes[voter].org==org && item.votes[voter].votes==theOrg.votes[value]) {
										topVoter = voter;
										break;
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
							{
								var vrow = createChunk(null, "tr", "voteInfo_tr voteInfo_orgHeading");
								vrow.appendChild(createChunk(org,"td","voteInfo_orgColumn voteInfo_td"));
								//var isection = createChunk(null, "td", "voteInfo_iconBar");
								//vrow.appendChild(isection);
								vrow.appendChild(createVoter(item.votes[topVoter])); // voteInfo_td
								if(orgsVote) {
									var cell = createChunk(null,"td","voteInfo_orgsVote voteInfo_voteCount voteInfo_td");
									cell.appendChild(createChunk(orgVoteValue, "span", "badge"));
									vrow.appendChild(cell);
								}else
									vrow.appendChild(createChunk(orgVoteValue,"td","voteInfo_orgsNonVote voteInfo_voteCount voteInfo_td"));
								vdiv.appendChild(vrow);
							}
							
							//now, other rows:
							for(var voter in item.votes) {
								if(item.votes[voter].org!=org ||  // wrong org or
										voter==topVoter) { // already done
									continue; // skip
								}
								// OTHER VOTER row
								{
									var vrow = createChunk(null, "tr", "voteInfo_tr");
									vrow.appendChild(createChunk("","td","voteInfo_orgColumn voteInfo_td")); // spacer
									//var isection = createChunk(null, "td", "voteInfo_iconBar");
									//vrow.appendChild(isection);
									vrow.appendChild(createVoter(item.votes[voter])); // voteInfo_td
									vrow.appendChild(createChunk(item.votes[voter].votes,"td","voteInfo_orgsNonVote voteInfo_voteCount voteInfo_td"));
									vdiv.appendChild(vrow);
								}
							}
						} else {
							// omit this org - not relevant for this value.
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
			
		} else {
			// ? indicate approved, last release value?
		}
		
		// KEY
		// approved and last release status
		{
			/*var kdiv = createChunk(null,"div","voteInfo_key");
			tr.voteDiv.appendChild(createChunk(stui.str("voteInfo_key"),"h3","voteInfo_key_title"));
			var disputedText = (theRow.voteResolver.isDisputed)?stui.str("winningStatus_disputed"):"";
			kdiv.appendChild(createChunk(
						stui.sub("winningStatus_msg",
								[ stui.str(theRow.voteResolver.winningStatus), disputedText ])
						, "div", "voteInfo_winningKey d-dr-"+theRow.voteResolver.winningStatus+" winningStatus"));
			
			kdiv.appendChild(createChunk(
					stui.sub("lastReleaseStatus_msg",
							[ stui.str(theRow.voteResolver.lastReleaseStatus) ])
					, "div", "i-star voteInfo_iconValue"));

			
			tr.voteDiv.appendChild(kdiv);
			
			var surlink = document.createElement("div");
			surlink.className = "alert alert-info fix-popover-help";
			
			var link = createChunk(stui.str("voteInfo_moreInfo"),"a", null);
			var theUrl = "http://cldr.unicode.org/index/survey-tool/guide#TOC-Key";
			link.href = theUrl;
			surlink.appendChild(link);
			tr.voteDiv.appendChild(surlink);*/

		}

		// done with voteresolver table
		
		if(stdebug_enabled) {
			tr.voteDiv.appendChild(createChunk(vr.raw,"p","debugStuff"));
		}
	} else {
		tr.voteDiv = null;
	}
	
	var statusAction = tr.statusAction = parseStatusAction(theRow.statusAction);
	var canModify = tr.canModify =  tr.theTable.json.canModify && statusAction.vote;
    var ticketOnly = tr.ticketOnly = tr.theTable.json.canModify && statusAction.ticket;
    /* var canChange = */ tr.canChange = canModify && statusAction.change;
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
	
	var config = surveyConfig;
	var protoButton = dojo.byId('proto-button');
	var cancelButton = dojo.byId('cancel-button');
	if(!canModify) {
		protoButton = null; // no voting at all.
		cancelButton = null;
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
	
	if(config.codecell) {

		children[config.codecell].appendChild(createChunk('|>'));
				removeAllChildNodes(children[config.codecell]);
				children[config.codecell].appendChild(createChunk('<|'));
						removeAllChildNodes(children[config.codecell]);
		var codeStr = theRow.code;
		if(theRow.coverageValue==101 && !stdebug_enabled) {
			codeStr = codeStr + " (optional)";
		}
		children[config.codecell].appendChild(createChunk(codeStr));
		if(tr.theTable.json.canModify) { // pointless if can't modify.
	
			children[config.codecell].className = "d-code";			
	
			
			if(!tr.forumDiv) {
				tr.forumDiv = document.createElement("div");
				tr.forumDiv.className = "forumDiv";
			}			
			
			appendForumStuff(tr,theRow, tr.forumDiv);
		}
		
		// extra attributes
		if(theRow.extraAttributes && Object.keys(theRow.extraAttributes).length>0) {
			appendExtraAttributes(children[config.codecell], theRow);
		}
		

		if(stdebug_enabled) {
			var anch = document.createElement("i");
			anch.className="anch";
			anch.id=theRow.xpid;
//			anch.href="#"+anch.id;
			children[config.codecell].appendChild(anch);
			anch.appendChild(document.createTextNode("#"));

			var go = document.createElement("a");
			go.className="anch-go";
			go.appendChild(document.createTextNode("zoom"));
			go.href=window.location.pathname + "?_="+surveyCurrentLocale+"&x=r_rxt&xp="+theRow.xpid;
			children[config.codecell].appendChild(go);
			
			var js = document.createElement("a");
			js.className="anch-go";
			js.appendChild(document.createTextNode("{JSON}"));
			js.popParent=tr;
//			js.href="#";
			listenToPop(JSON.stringify(theRow),tr,js);
			children[config.codecell].appendChild(js);
			children[config.codecell].appendChild(createChunk(" c="+theRow.coverageValue));
		}
//		listenFor(children[config.codecell],"click",
//				function(e){ 		
//					showInPop("XPath: " + theRow.xpath, children[config.codecell]);
//					stStopPropagation(e); return false; 
//				});
		if(!children[config.codecell].isSetup) {
			var xpathStr = "";
			if( /* (!window.surveyOfficial) || */ stdebug_enabled) {
				xpathStr = "XPath: " + theRow.xpath;
			}
			listenToPop(xpathStr, tr, children[config.codecell]);
			children[config.codecell].isSetup = true;
		}
	//	tr.anch = anch;
	}
	if(tr.iebs) {
		for(var qq in tr.iebs) {
			stdebug("Destroying " + tr.iebs[qq]);
			stdebug("Destroying ieb " + tr.iebs[qq].id);
			tr.iebs[qq].destroy();
		}
	}
	tr.iebs=[];
	
	if(!children[config.comparisoncell].isSetup) {
		if(theRow.displayName) {
			children[config.comparisoncell].appendChild(document.createTextNode(theRow.displayName));
			setLang(children[config.comparisoncell], surveyBaselineLocale);
			if(theRow.displayExample) {
				var theExample = appendExample(children[config.comparisoncell], theRow.displayExample, surveyBaselineLocale);
				//listenToPop(null,tr,theExample);
			}
		} else {
			children[config.comparisoncell].appendChild(document.createTextNode(""));
		}
		//listenToPop(null,tr,children[config.comparisoncell]);
		children[config.comparisoncell].isSetup=true;
	}
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
	if(theRow.items && theRow.winningVhash == "") {
		// find the bailey value
		var theBaileyValue = null;
		for(var k in theRow.items) {
			if(theRow.items[k].isBailey) {
				theBaileyValue = k;
			}
		}
		if(theBaileyValue !== null) {
			theRow.winningVhash = theBaileyValue;
			theRow.items[theBaileyValue].pClass = "fallback";
		}
	}
	if(theRow.items&&theRow.winningVhash) {
		addVitem(children[config.proposedcell],tr,theRow,theRow.items[theRow.winningVhash],theRow.winningVhash,cloneAnon(protoButton), null);
	} else {
		children[config.proposedcell].showFn = function(){};  // nothing else to show
	}
	
	listenToPop(null,tr,children[config.proposedcell], children[config.proposedcell].showFn);
	if(config.errcell)
		listenToPop(null,tr,children[config.errcell], children[config.proposedcell].showFn);
	//listenFor(children[config.errcell],"mouseover",function(e){return children[config.errcell]._onmove(e);});
	
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
		input.className = "form-control";
		input.placeholder = 'Add a translation';
		btn.onclick = function(e) {
			//if no input, add one
			if($(buttonAdd).find('input').length == 0) {
				
				//hide other
				$.each($('button.vote-submit'), function() {
					toAddVoteButton(this);
				});
				
				//transform the button
				buttonAdd.appendChild(input);
				toSubmitVoteButton(btn).onclick = function(event) {
					var newValue = input.value;
					if(newValue) {
						addValueVote(children[config.othercell], tr, theRow, newValue, cloneAnon(protoButton));					
					}
					else {
						toAddVoteButton(btn);
					}
					stStopPropagation(event);
					return false;
				};
				
				input.focus();
				
				
				//enter pressed
				$(input).keydown(function (e) {
					var newValue = $(this).val();
					if(e.keyCode == 13) {
						if(newValue) {
							addValueVote(children[config.othercell], tr, theRow, newValue, cloneAnon(protoButton));			
						}
						else {
							toAddVoteButton(btn);
						}
					}
				});
				
			}
			stStopPropagation(e);
			return false;
		};
	}
	
	
	
	//add the other vote info
	for(k in theRow.items) {
		if(k == theRow.winningVhash) {
			continue; // skip the winner
		}
		hadOtherItems=true;
		children[config.othercell].appendChild(document.createElement("hr"));
		addVitem(children[config.othercell],tr,theRow,theRow.items[k],k,cloneAnon(protoButton), cloneAnon(cancelButton));
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
	
	children[config.othercell].appendChild(document.createElement('hr'));
	children[config.othercell].appendChild(formAdd);//add button	

	if(canModify) {
		removeAllChildNodes(children[config.nocell]); // no opinion
		var noOpinion = cloneAnon(protoButton);
		wireUpButton(noOpinion,tr, theRow, null);
		noOpinion.value=null;
		children[config.nocell].appendChild(noOpinion);
		listenToPop(null, tr, children[config.nocell]);
	}  else if(ticketOnly) { // ticket link
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
	
	tr.className='vother cov'+theRow.coverageValue;
	if(surveyCurrentId!== '' && surveyCurrentId === tr.id) {
		window.showCurrentId(); // refresh again - to get the updated voting status.
	}
	
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
	//console.log("rows: " + Object.keys(theTable.myTRs)  + ", hitcount: " + theTable.hitCount);
	var partitionList = Object.keys(partitions);
	var curPartition = null;
	for(i in rowList ) {
		
		var k = rowList[i];
		var theRow = theRows[k];
		
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
		updateRow(tr,theRow);
		
		tbody.appendChild(tr);

	}
	
	
	//console.log("POST rows: " + Object.keys(theTable.myTRs)  + ", hitcount: " + theTable.hitCount);
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
	//theSortmode.appendChild(size);
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
		//theSortmode.appendChild(ul);
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
//		var minfo = dojo.byId("info_menu_p_covlev");
//		if(minfo) {
//			minfo.innerHTML = theTable.json.section.skippedDueToCoverage + " hidden";
//		}
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
		throw new Error( "surveyOrgCov not yet initialized");
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
	
/*	
	store.push({label: '-',  // stui.str("coverage_auto_msg"), // stui.str('coverage_'+ level.name)
		selected: false,
		value: 0});
	*/
	
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
	if(!theTable) {
		theTable = cloneLocalizeAnon(dojo.byId('proto-datatable'));
		if(isDashboard())
			theTable.className += ' dashboard';
		updateCoverage(theDiv);
		localizeFlyover(theTable);
		theTable.theadChildren = getTagChildren(theTable.getElementsByTagName("tr")[0]);
		var toAdd = dojo.byId('proto-datarow');  // loaded from "hidden.html", which see.
		/*if(!surveyConfig)*/ {
			var rowChildren = getTagChildren(toAdd);
			theTable.config = surveyConfig ={};
			for(var c in rowChildren) {
				rowChildren[c].title = theTable.theadChildren[c].title;//console.log(theTable.theadChildren[c].title);
				if(rowChildren[c].id) {
					surveyConfig[rowChildren[c].id] = c;
					stdebug("  config."+rowChildren[c].id+" = children["+c+"]");
					if(false&&stdebug_enabled) {
						removeAllChildNodes(rowChildren[c]);
						rowChildren[c].appendChild(createChunk("config."+rowChildren[c].id+"="+c));
					}
					//rowChildren[c].id=null;
				} else {
					stdebug("(proto-datarow #"+c+" has no id");
				}
			}
			if(stdebug_enabled) stdebug("Table Config: " + JSON.stringify(theTable.config));
		}
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
	} else {
		theDiv.appendChild(theDiv.theTable);
	}
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
//		if(theDiv.theLoadingMessage) {
//			theDiv.theLoadingMessage.style.display="none";
//			theDiv.removeChild(theDiv.theLoadingMessage);
//			theDiv.theLoadingMessage=null;
//		}
	} else {
		setDisplayed(theTable, true);
//		if(theDiv.theLoadingMessage) {
//			theDiv.theLoadingMessage.style.display="none";
//			theDiv.removeChild(theDiv.theLoadingMessage);
//			theDiv.theLoadingMessage=null;
//		}
	}

	
	hideLoader(theDiv.loader);
	wrapRadios();
}

function loadStui(loc) {
	if(!stui.ready) {
		stui  = dojo.i18n.getLocalization("surveyTool", "stui");
		if(!stuidebug_enabled) {
			stui.str = function(x) { if(stui[x]) return stui[x]; else return x; };
			stui.sub = function(x,y) { return dojo.string.substitute(stui.str(x), y);};
		} else {
			stui.str = stui_str; // debug
			stui.sub = function(x,y) { return stui_str(x) + '{' +  Object.keys(y) + '}'; };
		}
		stui.htmlbaseline = BASELINE_LANGUAGE_NAME;
		stui.ready=true;
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

function setLang(node, loc) {
	var info = locInfo(loc);
	
	if(info.dir) {
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
	        		 dojoHash,
	        		 dojoTopic,
	        		 domConstruct,
	        		 dojoNumber
	         ) {


		var appendLocaleLink = function appendLocaleLink(subLocDiv, subLoc, subInfo, fullTitle) {
			var name = locmap.getRegionAndOrVariantName(subLoc);
			if(fullTitle) {
				name = locmap.getLocaleName(subLoc);
			}
			var clickyLink = createChunk(name, "a", "locName");
			clickyLink.href = "#/"+subLoc+"/"+surveyCurrentPage+"/"+surveyCurrentId;
			subLocDiv.appendChild(clickyLink);
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
			else
				addClass(subLocDiv, "hide");
			return clickyLink;
		};

		/* trace for dijit leak */
		if(!surveyOfficial) window.TRL=function() {
			var sec = 5;
			console.log("Tracing dijit registry leaks every "+sec+"s");
			window.setInterval(function() {
				document.title = "[dijit:"+registry.length+"] | ";
			}, 1000 * sec);
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

		{
			var pucontent = dojo.byId("itemInfo");
			var theDiv = flipper.get(pages.data);
			theDiv.pucontent = pucontent;
			theDiv.stui = loadStui();
			
			pucontent.appendChild(createChunk(stui.str("itemInfoBlank"),"i"));
		}

		/**
		 * List of buttons/titles to set.
		 * @property menubuttons
		 */
		var menubuttons = {
			locale: "title-locale",
			section: "title-section",
			page: "title-page",
//			item: "title-item",
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
		 * parse the hash string into surveyCurrent___ variables. 
		 * @method parseHash
		 * @param {String} id
		 */
		window.parseHash = function parseHash(hash) {
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
					} else if(surveyCurrentSpecial=='search') {
						surveyCurrentPage='';
						surveyCurrentId=''; // for now
					} else {
						surveyCurrentPage = '';
						surveyCurrentId = '';
					}
				}
			} else {
				surveyCurrentLocale = '';
				surveyCurrentSpecial='locales';
				surveyCurrentId='';
				surveyCurrentPage='';
				surveyCurrentSection='';
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
//			itemBox  = dojo.byId("title-item");
//			if(itemBox!=null) {
//			    if(theLocale=='') {
//			    	menubuttons.set(menubuttons.item);
//    			    //itemBox.set('value', '');
//			    } else if(theId=='' && thePage!='') {
//			    	menubuttons.set(menubuttons.item, theLocale+'/'+thePage+'/');
//			        //itemBox.set('value', theLocale+'/'+thePage+'/');
//			    } else {
//			    	menubuttons.set(menubuttons.item,theLocale+'//'+theId);
//    			    //itemBox.set('value', theLocale+'//'+theId);
//			    }
//			}
			//document.title = document.title.split('|')[0] + " | " + '/' + theLocale + '/' + thePage;
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
//		clickToSelect(dojo.byId("title-item"));
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
				console.log(msg_fmt);
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
		};
		
		
		window.ariRetry = function() {
//			if(didUnbust) {
				ariDialog.hide();
				//flipper.flipTo(pages.loading, loadingChunk = createChunk(stui_str("loading_reloading"), "i", "loadingMsg"));
				window.location.reload(true);
//			} else {
//				flipper.flipTo(pages.loading, loadingChunk = createChunk(stui_str("loading_retrying"), "i", "loadingMsg"));
//				unbust(); // low level unbust
//				ariDialog.hide(); // hide abort, retry, ignore dialog
//				reloadV(); // may end right up busted, but oh well
//			}
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
			
			ariDialog.show();
			var oneword = dojo.byId("progress_oneword");
			oneword.onclick = function() {
				if(disconnected) {
					ariDialog.show();
				}
			};
		};
		
		function updateCoverageMenuTitle() {
			$('#coverage-info').text(stui.sub('coverage_auto_msg', {surveyOrgCov: stui.str('coverage_' + surveyOrgCov)}));
		}
		function updateCoverageMenuValue() 	
		{
			/*var menuSelect = registry.byId('menu-select');
			if(surveyUserCov !== null) {
				console.log('Setting menu to value ' + surveyUserCov  );
				menuSelect.setValue(surveyUserCov); // user cov
			} else {
				console.log('Setting menu to value auto');
				menuSelect.setValue('auto'); // org cov
			}
			console.log("Menu value is now: "   + menuSelect.getValue());*/
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
			if(!doPush) {doPush = false;}
			replaceHash(doPush); // update the hash
			updateLocaleMenu();

			if(surveyCurrentLocale==null) {
				menubuttons.set(menubuttons.section);
				if(surveyCurrentSpecial!=null) {
					menubuttons.set(menubuttons.page, stui_str("special_"+surveyCurrentSpecial));
				} else {
					menubuttons.set(menubuttons.page);
				}
//				menubuttons.set(menubuttons.item);
				return; // nothing to do.
			}
			var titlePageContainer = dojo.byId("title-page-container");

			/**
			 * Just update the titles of the menus. Internal to updateHashAndMenus
			 * @method updateMenuTitles
			 */
			function updateMenuTitles(menuMap) {
				updateLocaleMenu(menuMap);
				if(surveyCurrentSpecial!= null && surveyCurrentSpecial != '') {
//					menubuttons.set(menubuttons.section /*,stui_str("section_special") */);
					//menubuttons.set(menubuttons.section,stui_str("special_"+surveyCurrentSpecial));
					switch(surveyCurrentSpecial) {
						case "r_vetting_json":
							$('#section-current').html(stui_str('Dashboard'));
							break;
						
						default:
						$('#section-current').html(stui_str("special_"+surveyCurrentSpecial));
							break;
					}
					setDisplayed(titlePageContainer, false);
				} else if(!menuMap) {
					//menubuttons.set(menubuttons.section);
					setDisplayed(titlePageContainer, false);
//					menubuttons.set(menubuttons.page, surveyCurrentPage); 
				} else {
					if(menuMap.sectionMap[window.surveyCurrentPage]) {
						surveyCurrentSection = surveyCurrentPage; // section = page
						//menubuttons.set(menubuttons.section, menuMap.sectionMap[surveyCurrentSection].name);
						$('#section-current').html(menuMap.sectionMap[surveyCurrentSection].name);
						setDisplayed(titlePageContainer, false); // will fix title later
					} else if(menuMap.pageToSection[window.surveyCurrentPage]) {
						var mySection = menuMap.pageToSection[window.surveyCurrentPage];
						//var myPage = mySection.pageMap[window.surveyCurrentPage];
						surveyCurrentSection = mySection.id;
						//menubuttons.set(menubuttons.section, mySection.name);
						$('#section-current').html(mySection.name);
						setDisplayed(titlePageContainer, false); // will fix title later
//						menubuttons.set(menubuttons.page, myPage.name);
					} else {
						//menubuttons.set(menubuttons.section, stui_str("section_general"));
						$('#section-current').html(stui_str("section_general"));
						setDisplayed(titlePageContainer, false);
//						menubuttons.set(menubuttons.page);
					}
				}
				/*if(surveyCurrentSpecial=='' || surveyCurrentSpecial===null) {
					dojo.byId('st-link').href = dojo.byId('title-locale').href = '#locales//'+surveyCurrentPage+'/'+surveyCurrentId;
				} else {
					dojo.byId('st-link').href = dojo.byId('title-locale').href = '#locales///';
				}*/
			}

			/**
			 * @method updateMenus
			 */
			function updateMenus(menuMap) {
				// initializE menus

				if(!menuMap.menusSetup) {
					menuMap.menusSetup=true;
					menuMap.setCheck = function(menu, checked,disabled) {
						menu.set('iconClass',   (checked)?"dijitMenuItemIcon menu-x":"dijitMenuItemIcon menu-o");
						menu.set('disabled', disabled);
					};
					var menuSection = registry.byId("menu-section");
					//menuSection.destroyDescendants(false);
					menuMap.section_general = new MenuItem({
						label: stui_str("section_general"),
						//checked:   (surveyCurrentPage == ''),
						iconClass:  "dijitMenuItemIcon ",
						disabled: true,
						//    iconClass:"dijitEditorIcon dijitEditorIconSave",
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
							// TODO:  make this a real section
							window.open(survURL + '?forum=' + locmap.getLanguage(surveyCurrentLocale));
						}
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
							//setDisplayed(aMenu, false);
						}
						

						var showMenu = titlePageContainer.menus[mySection.id];

						if(!showMenu) {
							// doesn't exist - add it.

							var menuPage = new DropDownMenu();
							
							for(var k in mySection.pages) { // use given order
								(function(aPage) {
		
									var pageMenu = aPage.menuItem =  new MenuItem({
										label: aPage.name,
										
										iconClass:  (aPage.id == surveyCurrentPage)?"dijitMenuItemIcon menu-x":"dijitMenuItemIcon menu-o",
//										checked:   (aPage.id == surveyCurrentPage),
										//    iconClass:"dijitEditorIcon dijitEditorIconSave",
										onClick: function(){ 
											surveyCurrentId = ''; // no id if jumping pages
											surveyCurrentPage = aPage.id;
											updateMenuTitles(menuMap);
											reloadV();
										},
										disabled: (effectiveCoverage()<parseInt(aPage.levs[surveyCurrentLocale]))
									});
									//menuPage.addChild(pageMenu);
								})(mySection.pages[k]);
							}

							var theButton = new DropDownButton({label: '-', dropDown: menuPage});

							
							//theButton.placeAt(titlePageContainer);
							//console.log(myPage.name);
							//console.log(theButton);

							showMenu = theButton;
							
							titlePageContainer.menus[mySection.id] = mySection.pagesMenu = showMenu;
						}
						
						if(myPage !== null) {
							//showMenu.set('label', myPage.name);
							$('#title-page-container').html('<h1>'+myPage.name+'</h1>').show();
						} else {
							//showMenu.set('label', stui.str('section_subpages')); // no page selected
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
						
						{
							
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
							updateCoverageMenuValue();
							updateCoverage(flipper.get(pages.data)); // update CSS and auto menu title
						}
						
						
						function unpackMenus(json) {
							var menus = json.menus;
							
							if(_thePages) {
								stdebug("Updating cov info into menus for " + json.loc);
								for(var k in menus.sections) {
									var oldSection = _thePages.sectionMap[menus.sections[k].id];
									// _thePages.sections[k].minCov[locale] = ...
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
			var bund = locmap.getLocaleInfo(surveyCurrentLocale);
			
			if(bund) {
				if(bund.readonly) {
					var msg = null;
					if(bund.readonly_why) {
						msg = bund.readonly_why;
					} else {
						msg = stui.str("readonly_unknown");
					}
					var theChunk = domConstruct.toDom(stui.sub("readonly_msg", { info: bund, locale: surveyCurrentLocale, msg: msg}));
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
						//showLoader(theDiv.loader, "loading..");
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
						} else if(surveyCurrentPage=='' && surveyCurrentId=='') {
							// "no problems"
						}
						var theInfo;
						theDiv.appendChild(theInfo = createChunk("","p","special_general"));
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
			//flipper.flipTo(pages.loading, loadingChunk = createChunk(stui_str("loading"), "i", "loadingMsg"));
			var loadingPane = flipper.get(pages.loading);

			var itemLoadInfo = createChunk("","div","itemLoadInfo");			
			//loadingPane.appendChild(itemLoadInfo);
			
			var serverLoadInfo = createChunk("","div","serverLoadInfo");			
			//loadingPane.appendChild(serverLoadInfo);

			var lastServerLoadTxt  = '';
			var startTime = new Date().getTime();
			
			{
				window.setTimeout(function(){
						 updateStatus(); // will restart regular status updates
				}, 5000); // get a status update about 5s in.
				
				var timerToKill = null;
				flipper.addUntilFlipped(function() {
//					console.log("Starting throbber");
					var frag = document.createDocumentFragment();
					var k = 0;
					timerToKill = window.setInterval(function() {
						k++;
						//loadingChunk.style.opacity =   0.5 + ((k%10) * 0.05);
//						console.log("Throb to " + loadingChunk.style.opacity);
						
						// update server load txt?
						if(lastJsonStatus) {
							lastJsonStatus.sysloadpct =  dojoNumber.format(parseFloat( lastJsonStatus.sysload), {places: 0, type: "percent"});
							
							var now = new Date().getTime();
							var waitms = now - startTime;
							var waits = waitms / 1000.0;
							
							lastJsonStatus.waitTime = dojoNumber.format(waits, { round: 0, fractional: false});
							
							var newLoadTxt = stui.sub("jsonStatus_msg",lastJsonStatus);
							
							if(waits > 5 && newLoadTxt != lastServerLoadTxt) {
								removeAllChildNodes(serverLoadInfo);
								serverLoadInfo.appendChild(document.createTextNode(newLoadTxt));
								lastServerLoadTxt = newLoadTxt;
							}
						}
						
					}, 100);
					
					return frag;
				}, function() {
//					console.log("Kill throbber");
					window.clearInterval(timerToKill);
				});
			}

			// now, load. Use a show-er function for indirection.
			var shower = null;

			shower = function() {
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
								//flipper.flipTo(pages.other, createChunk(stui_str("loading_nocontent"),"i","loadingMsg"));
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
							
							var h2var = {votesafter:json.oldvotes.votesafter, newVersion:json.status.newVersion};
							var h2txt = stui.sub("v_oldvotes_title",h2var);
							theDiv.appendChild(createChunk(h2txt, "h2", "v-title"));
							
							if(!json.oldvotes.locale) {
								surveyCurrentLocale='';
								updateHashAndMenus();
								
								var ul = document.createElement("div");
								ul.className = "oldvotes_list";
								var data = json.oldvotes.locales.data;
								var header = json.oldvotes.locales.header;
								
								if(data.length > 0) {
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
									
									theDiv.appendChild(createChunk(stui.sub("v_oldvotes_locale_list_help_msg", {version: surveyOldVersion}),"p", "helpContent")); 
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
								//loclink.href='#';
								theDiv.appendChild(createChunk(json.oldvotes.localeDisplayName,"h3","v-title2"));
								theDiv.appendChild(createChunk(stui.sub("v_oldvotes_locale_msg", {version: surveyOldVersion, locale: json.oldvotes.localeDisplayName}), "p", "helpContent"));
								if(json.oldvotes.contested.length > 0 || json.oldvotes.uncontested.length > 0) {

									function showVoteTable(voteList, type) {
										var t = document.createElement("table");
										t.id = 'oldVotesAcceptList';
										var th = document.createElement("thead");
										var tb = document.createElement("tbody");
										{
											var tr = document.createElement("tr");
											tr.appendChild(createChunk(stui.str("v_oldvotes_path"),"th","code"));
											tr.appendChild(createChunk(json.BASELINE_LANGUAGE_NAME,"th","v-comp"));
											tr.appendChild(createChunk(stui.sub("v_oldvotes_winning_msg", {version: surveyOldVersion}),"th","v-win"));
											tr.appendChild(createChunk(stui.str("v_oldvotes_mine"),"th","v-mine"));
											var accept;
											tr.appendChild(accept=createChunk(stui.str("v_oldvotes_accept"),"th","v-accept"));
/*
											accept.appendChild(createLinkToFn("v_oldvotes_all", function() {
												for(var k in json.oldvotes.contested) {
													var row = json.oldvotes.contested[k];
													row.box.checked = true;
												}
											}));
											accept.appendChild(createLinkToFn("v_oldvotes_none", function() {
												for(var k in json.oldvotes.contested) {
													var row = json.oldvotes.contested[k];
													row.box.checked = false;
												}
											}));
*/
											th.appendChild(tr);
										}
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
											} else {
												//tr.appendChild(createChunk("","td","v-win"));
											}
											tr.appendChild(td0);
											var td1 = createChunk("","td","v-mine");
											var label  = createChunk("","label","");
											//label["for"] ='c_'+row.strid;
											var span1 = appendItem(label, row.myValue, "value");
											td1.appendChild(label);
											span1.dir = json.oldvotes.dir;
											tr.appendChild(td1);
											var td2 = createChunk("","td","v-accept");
											var box = createChunk("","input","");
											//box.name='c_'+row.strid;
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

													//box.checked = !box.checked;

													stStopPropagation(e);
													return false;
												});
											};})(tr,box,tdp)();

											tb.appendChild(tr);
										}
										t.appendChild(tb);
										return t;
									}

									
									var frag = document.createDocumentFragment();

									var summaryMsg = stui.sub("v_oldvotes_count_msg",{uncontested:json.oldvotes.uncontested.length,  contested: json.oldvotes.contested.length });
																		
									frag.appendChild(createChunk(summaryMsg, "div", "helpHtml"));

									if(json.oldvotes.bad > 0) {
										var summaryMsg2 = stui.sub("v_oldvotes_bad_msg",json.oldvotes);
										
										frag.appendChild(createChunk(summaryMsg2, "div", "helpHtml"));
									}

									var navChunk = document.createElement("div");
									navChunk.className = 'v-oldVotes-nav';
									frag.appendChild(navChunk);
									
									var uncontestedChunk = null;
									var contestedChunk = null;
									
									function addOldvotesType(type, jsondata, frag, navChunk) {
										var content = createChunk("","div","v-oldVotes-subDiv");
										
										content.strid = "v_oldvotes_title_"+type;

										var title = stui.str(content.strid);
										
										content.title = title;
										
										content.appendChild(createChunk(title,"h2","v-oldvotes-sub"));
										
										var descr = stui.sub("v_oldvotes_desc_"+type+"_msg", {version: surveyOldVersion});
										content.appendChild(createChunk(descr, "p", "helpContent"));
										
										
										content.appendChild(showVoteTable(jsondata, type));

										var submit = BusyButton({
//											id: 'oldVotesSubmit',
											label: stui.sub("v_submit_msg", {type: title}),
											busyLabel: stui.str("v_submit_busy")
										});


										submit.on("click",function(e) {
											setDisplayed(navChunk, false);
											var confirmList= []; // these will be revoted with current params
											var deleteList = []; // these will be deleted

											// explicit confirm/delete list -  save us desync hassle
											for(var kk in jsondata ) {
												if(jsondata[kk].box.checked) {
													confirmList.push(jsondata[kk].strid);
//												} else {
//													deleteList.push(jsondata[kk].strid);
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
									
									
									
									if(json.oldvotes.uncontested.length > 0){
										uncontestedChunk = addOldvotesType("uncontested",json.oldvotes.uncontested, frag, navChunk);
									}
									if(json.oldvotes.contested.length > 0){
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
								} else if(json.oldvotes.bad > 0) {
									if(json.oldvotes.bad > 0) {
										var summaryMsg2 = stui.sub("v_oldvotes_only_bad_msg",json.oldvotes);
										
										theDiv.appendChild(createChunk(summaryMsg2, "div", "helpHtml"));
									}
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
								if(isDashboard()) {
									request
					    			.get(url, {handleAs: 'json'})
					    			.then(function(json) {
										hideLoader(null,stui.loading2);
										isLoading=false;
										showReviewPage(json);
									});
								}
								else {
									request
					    			.get(url, {handleAs: 'html'})
					    			.then(function(html) {
										hideLoader(null,stui.loading2);
										isLoading=false;
										flipper.flipTo(pages.other, domConstruct.toDom(html));
									});
								}
								
					        });
					 });
				} else if(surveyCurrentSpecial == 'none') {
					//for now - redurect
					hideLoader(null);
					isLoading=false;
					window.location = survURL; // redirect home
				} else if(surveyCurrentSpecial == 'locales') {
					hideLoader(null);
					isLoading=false;					
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
					flipper.flipTo(pages.other,null);
				    filterAllLocale();//filter for init data
					forceSidebar();
					surveyCurrentLocale=null;
					surveyCurrentSpecial='locales';
					showInPop2(stui.str("localesInitialGuidance"), null, null, null, true); /* show the box the first time */					
					$('#itemInfo').html('');
				} else if(surveyCurrentSpecial=='search') {
					// setup
					var searchCache = window.searchCache;
					if(!searchCache) {
						searchCache = window.searchCache = {};
					}
					
					hideLoader(null);
					isLoading=false;					
					var theDiv = document.createElement("div");
					theDiv.className = 'search';

					// install
					var theInput = document.createElement("input");
					theDiv.appendChild(theInput);
					
					var theSearch = createChunk(stui.str("search"), "button");
					theDiv.appendChild(theSearch);
					
					var theResult = document.createElement("div");
					theResult.className = 'results';
					theDiv.appendChild(theResult);

					
					var newLocale = surveyCurrentLocale;
					
					var showResults = function showResults(searchTerm) {
						var results=searchCache[searchTerm];
						removeAllChildNodes(theResult);
						if(newLocale!=surveyCurrentLocale) {
							var newName = locmap.getLocaleName(newLocale);
							theResult.appendChild(createChunk(newName, "h4"));
						}
						theResult.appendChild(createChunk(searchTerm, "h3"));
						
						if(results.length == 0) {
							theResult.appendChild(createChunk(stui.str("searchNoResults", "h3", "searchNoResults")));
						} else {
							for(var i=0;i<results.length;i++) {
								var result = results[i];
								
								var theLi = document.createElement("li");
								
								var appendLink = function appendLink(title, url, theClass) {
									var theA = createChunk(title, "a");
									if(url && newLocale!='' && newLocale!=null) {
										theA.href = url;
									}
									if(theClass!=null) {
										theA.className = theClass;
									}
									theLi.appendChild(theA);
								};
								
								if(result.xpath) {
									if(result.strid) {
										codeUrl = "#/"+newLocale+"//"+result.strid;
									}
									appendLink(result.xpath, codeUrl, "xpath");
								}
								
								if(result.ph) {
									result.ph.strid = result.strid;
									result = result.ph; // pick up as section
								}
								
								var codeUrl = null;
								
								if(result.section) {
									codeUrl =  "#/"+newLocale+"/"+result.section+"/!";
									if(result.page) {
										codeUrl = "#/"+newLocale+"/"+result.page+"/";
										if(result.strid) {
											codeUrl = "#/"+newLocale+"/"+result.page+"/"+result.strid;
										}
									}
								}
								
								if(result.section) {
									appendLink(result.section, codeUrl);
									if(result.page) {
										theLi.appendChild(createChunk("»"));
										appendLink(result.page, codeUrl);
										if(result.code) {
											theLi.appendChild(createChunk("»"));
											appendLink(result.code, codeUrl, "codebox");
										}
									}
								}
								
								if(result.loc) {
									appendLocaleLink(theLi, result.loc, locmap.getLocaleInfo(result.loc), true);
								}
								
								theResult.appendChild(theLi);
							}
						}
						
						theResult.last = searchTerm;
						theResult.loc = newLocale;
					};
					
					var showSearchTerm = function showSearchTerm(searchTerm) {
						if((searchTerm != theResult.last || theResult.loc != newLocale) && searchTerm != null) {
							theResult.last = null;
							theResult.loc = null;
							removeAllChildNodes(theResult);
							theResult.appendChild(createChunk(searchTerm, "h3"));
							
							if(!(searchTerm in searchCache)) {
								   var xurl = contextPath + "/SurveyAjax?&s="+surveySessionId+"&what=search"; // allow cache
								   if(newLocale!=null&&newLocale!='') {
									   xurl = xurl + "&_="+newLocale;
								   }
								   queueXhr({
								        url:xurl, // allow cache
							 	        handleAs:"json",
							 	        load: function(h){
							 	        	if(h.results) {
							 	        		searchCache[searchTerm] = h.results;
									 			showResults(searchTerm);
							 	        	} else {
							 	        		theResult.appendChild(createChunk("(search error)","i"));
							 	        	}
								        },
								        error: function(err, ioArgs){
								 			var msg ="Error: "+err.name + " - " + err.message;
						 	        		theResult.appendChild(createChunk(msg,"i"));
								        },
								        postData: searchTerm
								    });
							} else {
								showResults(searchTerm);
							}
							

						} else {
							//no change;
						}
					};
					
					var searchFn = function searchFn(e) {
						var searchTerm = theInput.value;
						
						if(searchTerm.indexOf(':')>0) {
							var segs = searchTerm.split(':');
							if(locmap.getLocaleInfo(segs[0])!=null) {
								newLocale = segs[0];
								// goto
								if(segs.length==1) {
									surveyCurrentSpecial='';
									surveyCurrentLocale=newLocale;
									reloadV();
									return;
								}
								searchTerm = segs[1];
							}
						}
						
						showSearchTerm(searchTerm);

						return stStopPropagation(e);
					};
					
					listenFor(theInput, "change", searchFn);
					listenFor(theSearch, "click", searchFn);

					flipper.flipTo(pages.other, theDiv);
					theInput.focus();
					surveyCurrentLocale=null;
					surveyCurrentSpecial='search';
					showInPop2(stui.str("searchGuidance"), null, null, null, true); /* show the box the first time */					
				} else {
					var msg_fmt = stui.sub("v_bad_special_msg",
							{special: surveyCurrentSpecial });

					var loadingChunk;
					flipper.flipTo(pages.loading, loadingChunk = createChunk(msg_fmt, "p", "errCodeMsg"));
					var retryButton = createChunk(stui.str("loading_reload"),"button");
					loadingChunk.appendChild(retryButton);
					retryButton.onclick = function() { 	window.location.reload(true); };
					showLoader(theDiv.loader);
					isLoading=false;
				}
			}; // end shower

			shower(); // first load
//			flipper.get(pages.data).shower = shower;
			
			// set up the "show-er" function so that if this locale gets reloaded, the page will load again - execept for the dashboard, where only the row get updated
			if(!isDashboard())
				showers[flipper.get(pages.data).id]=shower;
			//else
			//	showers[flipper.get(pages.data).id]= function() {popupAlert('warning','Change has been made to your locale, consider <a href="#" onclick="window.location.reload()">reloading</a> !');};

		};  // end reloadV

		function trimNull(x) {
			if(x==null) return '';
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
					
				
					// any special message? "oldVotesRemind":{"count":60,"pref":"oldVoteRemind24", "remind":"* | ##"}
					if(json.oldVotesRemind && surveyCurrentSpecial!='oldvotes') {
						var vals = { count: dojoNumber.format(json.oldVotesRemind.count) };

						function updPrefTo(target) {
							var updurl  = contextPath + "/SurveyAjax?_="+theLocale+"&s="+surveySessionId+"&what=pref&pref=oldVoteRemind&_v="+target+cacheKill();                             myLoad(updurl, "updating coldremind " + target, function(json2) {
								if(!verifyJson(json2,'pref')) {
									return;
								} else {
									console.log('Server set  coldremind successfully.');
								}
							});
						}						
						var oldVoteRemindDialog = new Dialog({
							title: stui.sub("v_oldvote_remind_msg",vals), 
							content: stui.sub("v_oldvote_remind_desc_msg", vals)});

						oldVoteRemindDialog.addChild(new Button({
							label: stui.str("v_oldvote_remind_yes"),
							onClick: function() {
								updPrefTo(new Date().getTime() + (1000 * 3600));// hide for 1 hr
								oldVoteRemindDialog.hide();
								surveyCurrentSpecial="oldvotes";
								surveyCurrentLocale='';
								surveyCurrentPage='';
								surveyCurrentSection='';
								reloadV();
							}

						}));
						oldVoteRemindDialog.addChild(new Button({
							label: stui.str("v_oldvote_remind_no"),
							onClick: function() {
								updPrefTo(new Date().getTime() + (1000 * 86400)); // hide for 24 hours
								oldVoteRemindDialog.hide();
							}                            	
						}));
						oldVoteRemindDialog.addChild(new Button({
							label: stui.str("v_oldvote_remind_dontask"),
							onClick: function() {
								updPrefTo('*'); // hide permanently
								oldVoteRemindDialog.hide();
							}
						}));

						var now = new Date();
						if(json.oldVotesRemind.remind && now.getTime()<=parseInt(json.oldVotesRemind.remind)) {
							console.log("Have " + json.oldVotesRemind.count + " old votes, but will remind again in " + (parseInt(json.oldVotesRemind.remind)-now.getTime())/1000 + " seconds.");
						} else {
							oldVoteRemindDialog.show();
							console.log("Showed oldVotesRemind 6");
						}
					} else {
						stdebug("Did not need to showoldvotesremind : " + Object.keys(json).toString());
					}

					updateCovFromJson(json);
					// setup coverage level
					//if(!window.surveyLevels) {
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
										console.log('Server set  covlev successfully.');
									}
								});
							}
							// still update these.
							updateCoverage(flipper.get(pages.data)); // update CSS and 'auto' menu title
							updateHashAndMenus(false); // TODO: why? Maybe to show an item?
							$('#coverage-info').text(newValue.ucFirst());
							$(this).parents('.dropdown-menu').dropdown('toggle');
							return false;

						});
						// TODO have to move this out of the DOM..
						/*var covMenu = flipper.get(pages.data).covMenu = new Select({name: "menu-select", 
								id: 'menu-select',
								title: stui.str('coverage_menu_desc'),
								options: store,
								onChange: function(newValue) {
									
								}
								});
						covMenu.placeAt(titleCoverage);*/
					//}	

						
						
					
				window.reloadV(); // call it
			
				// watch for hashchange to make other changes.. 
				dojoTopic.subscribe("/dojo/hashchange", function(changedHash){
					//alert("hashChange…" + changedHash);
					if(true) {
						
						
						var oldLocale = trimNull(surveyCurrentLocale);
						var oldSpecial = trimNull(surveyCurrentSpecial);
						var oldPage = trimNull(surveyCurrentPage);
						var oldId = trimNull(surveyCurrentId);
						
						window.parseHash(changedHash);
						
						surveyCurrentId = trimNull(surveyCurrentId);
						
						// did anything change?
						if(oldLocale!=trimNull(surveyCurrentLocale) ||								oldSpecial!=trimNull(surveyCurrentSpecial) ||								oldPage != trimNull(surveyCurrentPage) ) {
							console.log("# hash changed, (loc, etc) reloadingV..");
							reloadV();
						} else if(oldId != surveyCurrentId && surveyCurrentId != '') {
							console.log("# just ID changed, to " + surveyCurrentId);
						    // surveyCurrentID and the hash have already changed.
						    // just call showInPop if the item is present. If not present, make sure it's visible.
							window.showCurrentId();						
						}
					} else {
					    console.log("Ignoring hash change " + changedHash);
					}
				});
				}
		});
		});

	});  // end require()
} // end showV


/**
 * reload a specific row
 * @method refreshRow2
 */
function refreshRow2(tr,theRow,vHash,onSuccess, onFailure) {
	showLoader(tr.theTable.theDiv.loader,stui.loadingOneRow);
    var ourUrl = contextPath + "/RefreshRow.jsp?what="+WHAT_GETROW+"&xpath="+theRow.xpid +"&_="+surveyCurrentLocale+"&fhash="+tr.rowHash+"&vhash="+vHash+"&s="+tr.theTable.session +"&json=t&automatic=t";
    
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
        			wrapRadios();
        			
        			hideLoader(tr.theTable.theDiv.loader);
        			onSuccess(theRow);
        			if(isDashboard()) {
        				refreshFixPanel(json);
        			}
        		} else {
        	        tr.className = "ferrbox";
//        	        tr.innerHTML="No content found "+tr.rowHash+ "  while  loading"; // this just obscures the row
        	        console.log("could not find " + tr.rowHash + " in " + json);
        	        onFailure("refreshRow2: Could not refresh this single row: Server failed to return xpath #"+theRow.xpid+" for locale "+surveyCurrentLocale);
        		}
           }catch(e) {
               console.log("Error in ajax post [refreshRow2] ",e.message);
 //              e_div.innerHTML = "<i>Internal Error: " + e.message + "</i>";
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
            //postData: value,
            handleAs:"json",
            load: loadHandler,
            error: errorHandler,
            timeout: ajaxTimeout
        };
    //window.xhrArgs = xhrArgs;
    //console.log('xhrArgs = ' + xhrArgs + ", url: " + ourUrl);
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
//		if(tr.inputTd) {
//    		tr.inputTd.className="d-change"; // TODO: use  (getTagChildren(tr)[tr.theTable.config.changecell])
//    	}
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
	var ourUrl = contextPath + "/SurveyAjax?what="+what+"&xpath="+tr.xpid +"&_="+surveyCurrentLocale+"&fhash="+tr.rowHash+"&vhash="+vHash+"&s="+tr.theTable.session;
	
	// vote reduced
	var voteReduced =	dijit.registry.byId('voteReduced');
	if(voteReduced) {
		ourUrl = ourUrl + "&voteReduced="+voteReduced.value;
	}
	
//	tr.className='tr_checking';
	var loadHandler = function(json){
		try {
			// var newHtml = "";
			if(json.err && json.err.length >0) {
				tr.className='tr_err';
				// v_tr.className="tr_err";
				// v_tr2.className="tr_err";
//				showLoader(tr.theTable.theDiv.loader,"Error!");
				handleDisconnect('Error submitting a vote', json);
				tr.innerHTML = "<td colspan='4'>"+stopIcon + " Could not check value. Try reloading the page.<br>"+json.err+"</td>";
				// e_div.innerHTML = newHtml;
				myUnDefer();
				handleDisconnect('Error submitting a vote', json);
			} else {
				if(json.submitResultRaw) { // if submitted..
					tr.className='tr_checking2';
					refreshRow2(tr,theRow,vHash,function(theRow){
//						tr.inputTd.className="d-change"; // TODO: use  inputTd=(getTagChildren(tr)[tr.theTable.config.changecell])

						// submit went through. Now show the pop.
						button.className='ichoice-o';
						button.checked=false;
						hideLoader(tr.theTable.theDiv.loader);
						if(json.testResults && (json.testWarnings || json.testErrors)) {
							// tried to submit, have errs or warnings.
							showProposedItem(tr.inputTd,tr,theRow,valToShow,json.testResults); // TODO: use  inputTd= (getTagChildren(tr)[tr.theTable.config.changecell])
						} else {
							//  submit OK.
						}
						if(box) {
							box.value=""; // submitted - dont show.
						}
						//tr.className = 'vother';
						myUnDefer();
					}, function(err) {
						myUnDefer();
						handleDisconnect(err, json);
					}); // end refresh-loaded-fcn
					// end: async
				} else {
					// Did not submit. Show errors, etc
					if(
							(json.statusAction&&json.statusAction!='ALLOW')
						|| (json.testResults && (json.testWarnings || json.testErrors ))) {
						showProposedItem(tr.inputTd,tr,theRow,valToShow,json.testResults,json); // TODO: use  inputTd= (getTagChildren(tr)[tr.theTable.config.changecell])
					} else {
						// no errors, not submitted.  Nothing to do.
					}
					if(box) {
						box.value=""; // submitted - dont show.
					}
					//tr.className='vother';
					button.className='ichoice-o';
					button.checked = false;
					hideLoader(tr.theTable.theDiv.loader);
					myUnDefer();
				}
			}
		}catch(e) {
			tr.className='tr_err';
			// v_tr.className="tr_err";
			// v_tr2.className="tr_err";
			tr.innerHTML = stopIcon + " Could not check value. Try reloading the page.<br>"+e.message;
			console.log("Error in ajax post [handleWiredClick] ",e.message);
			//              e_div.innerHTML = "<i>Internal Error: " + e.message + "</i>";
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
	var xhrArgs = {
			url: ourUrl+cacheKill(),
			handleAs:"json",
			timeout: ajaxTimeout,
			load: loadHandler,
			error: errorHandler
	};
	//window.xhrArgs = xhrArgs;
	//stdebug('xhrArgs = ' + xhrArgs + ", url: " + ourUrl);
	if(box) {
		stdebug("this is a psot: " + value);
		xhrArgs.postData = value;
	}
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
//	tr.className='tr_checking';
	var loadHandler = function(json){
		try {
			// var newHtml = "";
			if(json.err && json.err.length >0) {
				tr.className='tr_err';
				// v_tr.className="tr_err";
				// v_tr2.className="tr_err";
//				showLoader(tr.theTable.theDiv.loader,"Error!");
				handleDisconnect('Error deleting a value', json);
				tr.innerHTML = "<td colspan='4'>"+stopIcon + " Could not check value. Try reloading the page.<br>"+json.err+"</td>";
				// e_div.innerHTML = newHtml;
				myUnDefer();
				handleDisconnect('Error deleting a value', json);
			} else {
				if(json.deleteResultRaw) { // if deleted..
					tr.className='tr_checking2';
					refreshRow2(tr,theRow,vHash,function(theRow){

						// delete went through. Now show the pop.
						//button.className='ichoice-o';
						hideLoader(tr.theTable.theDiv.loader);
						//tr.className = 'vother';
						myUnDefer();
					}, function(err) {
						myUnDefer();
						handleDisconnect(err, json);
					}); // end refresh-loaded-fcn
					// end: async
				} else {
					// Did not submit. Show errors, etc
					if(
							(json.statusAction&&json.statusAction!='ALLOW')
						|| (json.testResults && (json.testWarnings || json.testErrors ))) {
						showProposedItem(tr.inputTd,tr,theRow,valToShow,json.testResults,json); // TODO: use  inputTd= (getTagChildren(tr)[tr.theTable.config.changecell])
					} else {
						// no errors, not submitted.  Nothing to do.
					}
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
		
		//frag.appendChild(document.createTextNode("hello"));
		
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
					user.appendChild(createChunk("Last: " + cs.last  + "LastAction: " + cs.lastAction + ", IP: " + cs.ip + ", ttk:"+(parseInt(cs.timeTillKick)/1000).toFixed(1)+"s", "span","adminUserInfo"));
					
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
	//				if(json.threads.dead) {
	//					frag2.appendChunk(json.threads.dead.toString(),"span","adminDeadThreads");
	//					// TODO
	//				}
					last = json.exceptions.lastTime;
					if(json.exceptions.entry) {
						var e = json.exceptions.entry;
						exceptions.push(json.exceptions.entry);
						var exception = createChunk(null,"div","adminException");
						//exception.e = e;
						if(e.header&&e.header.length < 80) {
							exception.appendChild(createChunk(e.header,"span","adminExceptionHeader"));
						} else {
							var t;
							exception.appendChild(t=createChunk(e.header.substring(0,80)+"...","span","adminExceptionHeader"));
							t.title=e.header;
						}
						exception.appendChild(createChunk(e.DATE,"span","adminExceptionDate"));
						var clicky=(function (e){return (function(ee) {
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
						});})(e);
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
					
					
	//				removeAllChildNodes(u);
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
					} else {
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
//				if(!setHeader) {
//					// not setup yet, too bad.
//				} if(json.settings.all.CLDR_HEADER) {
//					setHeader.value = json.settings.all.CLDR_HEADER;
//				} else {
//					setHeader.value = "";
//				}
//				
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

		var actions = [
		               				"rawload"
		               ];
		
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
	dojo.ready(function() {
		loadStui();
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
					var labels = [];
					var count = [];
					for(var i in data) {
						labels.push(data[i][header.LAST_MOD]);
						count.push(data[i][header.COUNT]);
					}
					var gdata = [];
					gdata.push(count);
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
					console.log("Drawing in : " + hname + " - "  + count.toString());
					r.g.hbarchart(100,offh,600,hei, gdata )
						.hover(fin,fout);
						//.label(labels2);
					for(var i in labels) {
						r.text(toffh,toffv+(i*(hei/labels.length)), (labels[i].split(" ")[0])+"\n"+count[i]  );
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
	});
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
				if (json&&json.mine) {
					
					var frag = document.createDocumentFragment();

					var header = json.mine.header;
					var data = json.mine.data;

					
					if(data.length==0) {
						frag.appendChild(createChunk(stui_str("recentNone"),"i"));
					} else {
						{
							var rowDiv = document.createElement("div");
							frag.appendChild(rowDiv);
							
							rowDiv.appendChild(createChunk(stui_str("recentLoc"),"b"));
							rowDiv.appendChild(createChunk(stui_str("recentCount"),"b"));
							//rowDiv.appendChild(createChunk(stui_str("downloadXml"),"b"));
						}
						
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
		var div = dojo.byId(divName);
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
						{
							var rowDiv = document.createElement("div");
							frag.appendChild(rowDiv);
							
							rowDiv.appendChild(createChunk(stui_str("recentLoc"),"b"));
							rowDiv.appendChild(createChunk(stui_str("recentXpath"),"b"));
							rowDiv.appendChild(createChunk(stui_str("recentValue"),"b"));
							rowDiv.appendChild(createChunk(stui_str("recentWhen"),"b"));
						}
						
						for(var q in data) {
							var row = data[q];
							
							var loc = row[header.LOCALE];
                                                        var locname = row[header.LOCALE_NAME];
							var org = row[header.ORG];
							var last_mod = row[header.LAST_MOD];
							var xpath = row[header.XPATH];
							var xpath_hash = row[header.XPATH_STRHASH];
							var value = row[header.VALUE];
							
							var rowDiv = document.createElement("div");
							frag.appendChild(rowDiv);
							
                                                        rowDiv.appendChild(createLocLink(loc,locname, "recentLoc"));
							var xpathItem;
							rowDiv.appendChild(xpathItem = createChunk(xpath,"a","recentXpath"));
							xpathItem.href = "survey?_="+loc+"&strid="+xpath_hash;
							rowDiv.appendChild(createChunk(value,"span","value recentValue"));
							rowDiv.appendChild(createChunk(last_mod,"span","recentWhen"));
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
	        	
	        	loadStui();
	        	
	        	window._userlist = list; // DEBUG
	        	var table = dom.byId(tableRef);
	        	
	        	var rows = [];
	        	var theadChildren = getTagChildren(table.getElementsByTagName("thead")[0].getElementsByTagName("tr")[0]);
	        	
	        	setDisplayed(theadChildren[1],false);
	        	var rowById = [];
	        	
	        	for(var k in list ) {
	        		var user = list[k];
	        		//console.log("Info for user " + JSON.stringify(user));
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
    				/*
    				  COUNT: 1120,  DAY: 2013-04-30, LOCALE: km, LOCALE_NAME: khmer, SUBMITTER: 2
    				  */
    			//	console.log(JSON.stringify(json))
    				var stats = json.stats_bydayuserloc;
    				var header = stats.header;
    				for(var k in stats.data) {
    					var row = stats.data[k];
    					var submitter = row[header.SUBMITTER];
    					var submitterRow = rowById[submitter];
    					if(submitterRow !== undefined) {
    						var userRow = rows[submitterRow];
    						
//    						console.log(userRow.user.name + " = " + row);
    						// Kotoistus-koordinaattori  = 292,2013-04-30,fi,3330,Finnish
    						
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

   		/*
   		 *  If we need any per item load:
   		 *	        	// now lazy load each item.
	        	var loadmore = null; 
	        	var loadInterval = null;
	        	var processRow = 0;
	        	loadmore = function() {
	        		console.log('loadmore r#' + processRow + '/' + rows.length);	
    				window.clearTimeout(loadInterval);
    				
    				var row = rows[processRow];
    				
	        		request
	        			.get(contextPath + "/SurveyAjax?what=recent_items&_="+1+"&user="+2+"&limit="+15, {handleAs: 'json'})
	        			.then(function(json) {
	        				console.log('..loaded');
	        				// fetch next
	        				if( (++processRow) < rows.length ) {
	        					loadInterval = window.setTimeout(loadmore, 1000);
	        				} else {
	        					console.log('loadmore done');
	        				}
	        			});
	        	};
	        	loadInterval = window.setTimeout(loadmore, 1000);
*/	        
	        });
		});
}

// survey.js  -Copyright (C) 2012-2013 IBM Corporation and Others. All Rights Reserved.
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
if(!Object.keys) {
	Object.keys = function(x) {
		var r = [];
		for (j in x) {
			r.push(j);
		}
		return r;
	};
}

/**
 * @class GLOBAL
 */

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
			node.style.display='none'; // hide it
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
	if(this._visible == id && (node == null)) {
		return; // noop - unless adding s'thing
	}
	this._map[this._visible].style.display='none';
	for(var k in this._killfn) {
		this._killfn[k]();
	}
	this._killfn = []; // pop?
	if(node!=null) {
		removeAllChildNodes(this._map[id]);
		if(node.nodeType>0) {
			this._map[id].appendChild(node);
		} else for( var k in node) {
			// it's an array, add all 
			this._map[id].appendChild(node[k]);
		}
	}
	this._map[id].style.display=null;
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
 * showfn is called, result is added to the div.  killfn is called when page is flipped.
 * @method addUntilFlipped
 * @param showFn
 * @param killFn
 */
Flipper.prototype.addUntilFlipped = function addUntilFlipped(showFn, killFn) {
	this.get().appendChild(showFn());
	this._killfn.push(killFn);
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
		startup: "Starting up..."
};

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
		chunk.title=stui_str(firstword(className)+"_desc");
	}
	if(text) {
		chunk.appendChild(document.createTextNode(text));
	}
	return chunk;
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
		obj.href ='#';
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
	div.appendChild(createChunk(stui_str("userlevel_"+user.userlevelname),"i","userlevel_"+user.userlevelname));
	div.appendChild(createChunk(user.name,"span","adminUserName"));
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
 * @param id ID of DOM node
 * @param txt text to replace with - should just be plaintext, but currently can be HTML
 */
function updateIf(id, txt) {
    var something = document.getElementById(id);
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
}

var wasBusted = false;
var wasOk = false;
var loadOnOk = null;
var clickContinue = null;
 var surveyNextLocaleStamp = 0;
 
 /**
  * Mark the page as busted. Don't do any more requests.
  * @method busted
  */
 function busted() {
	 disconnected = true;
	 //console.log("disconnected.");
	 document.getElementsByTagName("body")[0].className="disconnected "+document.getElementsByTagName("body")[0].className; // hide buttons when disconnected.
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
	if(deferUpdateFn==null) {
		return;
	}
	for(i in deferUpdateFn) {
		if(deferUpdateFn[i]) {
			var fn = deferUpdateFn[i];
			deferUpdateFn[i]=null;
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
	if(deferUpdates<=0) {
		doDeferredUpdates();
	}
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
	if(deferUpdates>0) {
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
			showers[i]();
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
		oneword.innerHTML = stopIcon +  stui_str(progressWord);
		p.className = "progress-disconnected";
		busted(); // no further processing.
	} else if(ajaxWord) {
		p.className = "progress-ok";
		oneword.innerHTML = ajaxWord;
	} else if(!progressWord || progressWord == "ok") {
		if(specialHeader) {
			p.className = "progress-special";
			oneword.innerHTML = specialHeader; // only show if set
		} else {
			p.className = "progress-ok";
			oneword.innerHTML = stui_str('online');
		}
	} else if(progressWord=="startup") {
		p.className = "progress-ok";
		oneword.innerHTML = stui_str('startup');
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
 * Handle that ST has disconnected
 * @method handleDisconnect
 * @param why
 * @param json
 * @param word
 */
function handleDisconnect(why, json, word) {
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
				var chunk0 = document.createElement("i");
				chunk0.appendChild(document.createTextNode(stui_str("error_restart")));
				var chunk = document.createElement("textarea");
				chunk.className = "errorMessage";
				chunk.appendChild(document.createTextNode(why));
				chunk.rows="10";
				chunk.cols="40";				
				p.appendChild(chunk0);
				p.appendChild(chunk);
				if(oneword.details) {
					oneword.details.style.display="none";
				}
				oneword.onclick=null;
				return false;
			};
			{
				var p = dojo.byId("progress");	
				var detailsButton = document.createElement("button");
				detailsButton.type = "button";
				detailsButton.id = "progress-details";
				detailsButton.appendChild(document.createTextNode(stui_str("details")));
				detailsButton.onclick = oneword.onclick;
				p.appendChild(detailsButton);
				oneword.details = detailsButton;
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

/**
 * Based on the last received packet of JSON, update our status
 * @method updateStatusBox
 * @param {Object} json received 
 */
function updateStatusBox(json) {
	if(json.disconnected) {
		handleDisconnect("Misc Disconnect", json,"disconnected"); // unknown 
	} else if (json.SurveyOK==0) {
		handleDisconnect("Not running: ", json,"disconnected"); // ST has restarted
	} else if (json.status && json.status.isBusted) {
		handleDisconnect("Server says: busted " + json.status.isBusted, json,"disconnected"); // Server down- not our fault. Hopefully.
	} else if(!json.status) {
		handleDisconnect("!json.status",json);
	} else if(json.status.surveyRunningStamp!=surveyRunningStamp) {
		handleDisconnect("Server restarted since page was loaded",json,"disconnected"); // desync
	} else if(json.status && json.status.isSetup==false && json.SurveyOK==1) {
		updateProgressWord("startup");
	} else {
		updateProgressWord("ok");
	}
	
	if(json.status) {
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
		if(json.status.specialHeader && json.status.specialHeader.length>0) {
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

/**
 * This is called periodically to fetch latest ST status
 * @method updateStatus
 */
function updateStatus() {
	if(disconnected) return;
//	stdebug("UpdateStatus...");
    dojo.xhrGet({
        url: contextPath + "/SurveyAjax?what=status"+surveyLocaleUrl+cacheKill(),
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
            	if(json.status.surveyRunningStamp!=surveyRunningStamp) {
                    st_err.className = "ferrbox";
                    st_err.innerHTML="The SurveyTool has been restarted. Please reload this page to continue.";
                    wasBusted=true;
                    busted();
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
			tr.lastOn = button;
		} else {
			button.className = "ichoice-o";
		}
	} else if((theRow.voteVhash==vHash) && !box) {
		button.className = "ichoice-x";
		tr.lastOn = button;
	} else {
		button.className = "ichoice-o";
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

function showInPop(str,tr, theObj, fn, immediate) {
}

function listenToPop(str, tr, theObj, fn) {
	listenFor(theObj, "click",
			function(e) {
				showInPop(str, tr, theObj, fn, true);
				stStopPropagation(e);
				return false;
			});
}


function getPopToken() {
	return gPopStatus.popToken;
}

function incrPopToken(x) {
	++gPopStatus.popToken;
	//stdebug("PT@"+gPopStatus.popToken+" - " + x);
	return gPopStatus.popToken;
}


function hidePopHandler(e){ 		
	window.hidePop(null);
	stStopPropagation(e); return false; 
}
function removeClass(obj, className) {
	if(obj.className.indexOf(className)>-1) {
		obj.className = obj.className.substring(className.length+1);
	}
}
function addClass(obj, className) {
	if(obj.className.indexOf(className)==-1) {
		obj.className = className+" "+obj.className;
	}
}


// called when showing the popup each time
function showForumStuff(frag, forumDiv, tr) {
	// prepend something
	var newButton = createChunk("New Post (leaves this page)", "button", "forumNewButton");
	frag.appendChild(newButton);
	
	listenFor(newButton, "click", function(e) {
		window.blur(); // submit anything unsubmitted
		window.location = tr.forumDiv.postUrl;
		stStopPropagation(e);
		return true;
	});

//	console.log("showingForumStuff: " + tr.forumDiv.forumPosts);
	if(tr.forumDiv.forumPosts > 0) {
		var showButton = createChunk("Show " + tr.forumDiv.forumPosts  + " posts", "button", "forumShow");
		
		forumDiv.appendChild(showButton);
		
		var theListen = function(e) {
			showButton.style.display = "none";
			
			// callback.
			var ourUrl = tr.forumDiv.url + "&what=forum_fetch";
			var errorHandler = function(err, ioArgs) {
				console.log('Error in showForumStuff: ' + err + ' response '
						+ ioArgs.xhr.responseText);
				showInPop(
						stopIcon
								+ " Couldn't load forum post for this row- please refresh the page. <br>Error: "
								+ err + "</td>", tr, null);
				handleDisconnect("Could not showForumStuff:"+err, null)
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
	
	
	//	if(tr.theRow.forumPosts > (-1)) {
//		td.appendChild(document.createTextNode("Forum posts: " + tr.theRow.forumPosts ));
//	}
}

// called when initially setting up the section
function appendForumStuff(tr, theRow, forumDiv) {
	removeAllChildNodes(forumDiv); // we may be updating.
	forumDiv.replyStub = contextPath + "/survey?forum=&_=" + surveyCurrentLocale + "&replyto=";
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
	
	function setLastShown(obj) {
		if(gPopStatus.lastShown && obj!=gPopStatus.lastShown) {
			removeClass(gPopStatus.lastShown,"pu-select");
			//addClass(gPopStatus.lastShown,"pu-deselect");
		}
		if(obj) {
			//removeClass(obj,"pu-deselect");
			addClass(obj,"pu-select");
		}
		gPopStatus.lastShown = obj;
	}
	
	function clearLastShown() {
		setLastShown(null);
	}
	
//	listenFor(pucontent, "mouseover", function() {
//		clearTimeout(hideInterval);
//		hideInterval=null;
//	});
//	listenFor(pucontent, "mouseout", hidePopHandler);
	
	window.showInPop2 = function(str, tr, hideIfLast, fn, immediate) {
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
//		if(hideIfLast && lastShown==hideIfLast) {
//			lastShown=null;
//			
//			pucontent.style.display="none";
//			
//			return;
//		}
		if(tr && tr.sethash) {
			window.updateCurrentId(tr.sethash);
		}
		setLastShown(hideIfLast);

		var td = document.createDocumentFragment();

		// Always have help (if available).
		var theHelp = null;
		if(tr&& tr.helpDiv) {
			theHelp =  tr.helpDiv;
		}
		if(theHelp) {
			td.appendChild(theHelp.cloneNode(true));
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

		// forum stuff
		if(tr && tr.forumDiv) {
			var forumDiv = tr.forumDiv.cloneNode(true);
			showForumStuff(td, forumDiv, tr); // give a chance to update anything else
			td.appendChild(forumDiv);
		}

		
		// SRL suspicious
		removeAllChildNodes(pucontent);
		pucontent.appendChild(td);
		td=null;
		
	};
	if(false) {
		window.showInPop = window.showInPop2;
	} else {
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
	}
	window.hidePop = function() {
		if(hideInterval) {
			clearTimeout(hideInterval);
		}
		hideInterval=setTimeout(function() {
			if(false) {
				//pucontent.style.display="none";
			} else {
				// SRL suspicious
				removeAllChildNodes(pucontent);
//				pupeak.style.display="none";
			}
			clearLastShown();
			incrPopToken('newHide');
		}, 2000);
	};
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
	
	// clicking on some item will attempt to jump to that item.
	if(false && tr && value) { // TODO: not working yet.
		addClass(span, "rolloverspan");
		var fn = null;
		listenFor(span, "mouseover",
			 fn = 	function(e) {
			console.log("Clicked on " + value + " - item is " + item.toString());
					var item = tr.theRow.valueToItem[value];
					if(item && item.showFn) {
						showInPop("", tr, item.div, item.showFn, true);
						stStopPropagation(e);
						return false;
					} else {
						return true;
					}
				});
		
		//span.onclick = fn;
//	} else {
//		console.log("no tr or no value: " + value);
	}
	
	return span;
}

function testsToHtml(tests) {
	var newHtml = "";
	if(!tests) return newHtml;
	for ( var i = 0; i < tests.length; i++) {
		var testItem = tests[i];
		newHtml += "<p class='tr_" + testItem.type + "' title='" + testItem.type
				+ "'>";
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
				tr.lastOn.className = "ichoice-o";
			}
			wireUpButton(newButton,tr,theRow,"[retry]", {"value":value});
			ourDiv.appendChild(newButton);
		}
		var h3 = document.createElement("span");
		var span=appendItem(h3, value, "value",tr);
		span.dir = tr.theTable.json.dir;
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
			span.dir = tr.theTable.json.dir;
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

		var h3 = document.createElement("h3");
		var span = appendItem(h3, item.value, item.pClass); /* no need to pass in 'tr' - clicking this span would have no effect. */
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
		
		if(item.inExample) {
			appendExample(td, item.inExample);
//		} else if(item.example) {
//			appendExample(td, item.example);
		}
		
		//return function(){ var d2 = div; return function(){ 	d2.className="d-item";  };}();
	}; // end fn
}

function popInfoInto(tr, theRow, theChild, immediate) {
	showInPop("NOT USED.", tr, theChild); // empty
	return; 
	
	//if(theRow.voteInfoText) {
	//	showInPop(theRow.voteInfoText, tr, theChild, null, immediate);
	//	return;
	//}
	showInPop("<i>" + stui.str("loading") + "</i>", tr, theChild);
	var popShowingToken = getPopToken();
	stdebug('Got token ' + popShowingToken);
//	var what = WHAT_GETROW;
	var ourUrl = contextPath + "/RefreshRow.jsp?what=" + WHAT_GETROW
			+ "&xpath=" + theRow.xpid + "&_=" + surveyCurrentLocale + "&fhash="
			+ theRow.rowHash + "&vhash=" + "&s=" + tr.theTable.session
			+ "&voteinfo=t";
	var errorHandler = function(err, ioArgs) {
		console.log('Error in refreshRow: ' + err + ' response '
				+ ioArgs.xhr.responseText);
		showInPop(
				stopIcon
						+ " Couldn't reload this row- please refresh the page. <br>Error: "
						+ err + "</td>", tr, theChild);
		handleDisconnect("Could not refresh row:"+err, null)
		return true;
	};
	var loadHandler = function(text) {
		try {
			if (text) {
				theRow.voteInfoText = text;
			} else {
				theRow.voteInfoText = stopIcon + stui.noVotingInfo;
			}
			if(getPopToken()==popShowingToken) {
				showInPop(theRow.voteInfoText, tr, theChild, null, true);
//				stdebug("success with token " + popShowingToken);
			} else { // else, something else happened meanwhile.
//				stdebug("our token was " + popShowingToken + " but now at " + getPopToken() );
			}
		} catch (e) {
			console.log("Error in ajax get ", e.message);
			console.log(" response: " + text);
			showInPop(stopIcon + " exception: " + e.message, tr, theChild, true);
		}
	};
	var xhrArgs = {
		url : ourUrl,
		handleAs : "text",
		load : loadHandler,
		error : errorHandler
	};
	// window.xhrArgs = xhrArgs;
	// console.log('xhrArgs = ' + xhrArgs);
	queueXhr(xhrArgs);
}


function appendExample(parent, text) {
	var div = document.createElement("div");
	div.className="d-example";
	div.innerHTML=text;
	parent.appendChild(div);
	return div;
}

var spanSerial = 0;

/**
 * Append a Vetting item ( vote button, etc ) to the row.
 * @method AddVitem
 * @param {DOM} td cell to append into
 * @param {DOM} tr which row owns the items
 * @param {JSON} theRow JSON content of this row's data
 * @param {JSON} item JSON of the specific item we are adding
 * @param {String} vHash     stringid of the item
 * @param {DOM} newButton     button prototype object
 */
function addVitem(td, tr,theRow,item,vHash,newButton) {
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
	
	if(newButton) {
		newButton.value=item.value;
		wireUpButton(newButton,tr,theRow,vHash);
		div.appendChild(newButton);
	}
    var subSpan = document.createElement("span");
    subSpan.className = "subSpan";
	var span = appendItem(subSpan,item.value,item.pClass,tr);
	div.appendChild(subSpan);
	
	span.dir = tr.theTable.json.dir;
	
	if(item.isOldValue==true && !isWinner) {
		addIcon(div,"i-star");
	}
	if(item.votes && !isWinner) {
		addIcon(div,"i-vote");
	}

    // wire up the onclick
	td.showFn = item.showFn = showItemInfoFn(theRow,item,vHash,newButton,div);
	div.popParent = tr;
	listenToPop(null, tr, div, td.showFn);
	td.appendChild(div);
	
	if(item.inExample) {
		//addIcon(div,"i-example-zoom").onclick = div.onclick;
	} else if(item.example) {
		var example = appendExample(div,item.example);
//		example.popParent = tr;
//		listenToPop(null,tr,example,td.showFn);
	}
	
	if(tr.theTable.json.canModify) {
	    var oldClassName = span.className = span.className + " editableHere";
	    ///span.title = span.title  + " " + stui_str("clickToChange");
	    var ieb = null;
    	    var spanId = span.id = "v_"+(spanSerial++); // bump the #, probably leaks something in dojo?
	    var editInPlace = function(e) {
	        require(["dojo/ready", "dijit/InlineEditBox", "dijit/form/TextBox", "dijit/registry"],
	        function(ready, InlineEditBox, TextBox) {
	            ready(function(){
	            if(!ieb) {
	                ieb = new InlineEditBox({editor: TextBox, autoSave: true, 
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
	                               }
	                   }, spanId);
//	                       console.log("Minted  " + spanId);
	                   } else {
//	                       console.log("Leaving alone " + spanId);
	                   }
	            });
	        });
	    	stStopPropagation(e);
	    	return false;
	    };
	    
	    listenFor(td, "mouseover", editInPlace);
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

function updateRow(tr, theRow) {
	
	tr.valueToItem = {}; // hash:  string value to item (which has a div)
	tr.rawValueToItem = {}; // hash:  string value to item (which has a div)
	for(k in theRow.items) {
		var item = theRow.items[k];
		if(item.value) {
			tr.valueToItem[item.value] = item; // back link by value
			tr.rawValueToItem[item.rawValue] = item; // back link by value
		}
	}
	
	if(!tr.helpDiv && theRow.displayHelp) {
		// this also marks this row as a 'help parent'
		tr.helpDiv = cloneAnon(dojo.byId("proto-help"));
		tr.helpDiv.innerHTML += theRow.displayHelp;
		
		// extra attributes
		if(theRow.extraAttributes && Object.keys(theRow.extraAttributes).length>0) {
			var extraHeading = createChunk( stui.str("extraAttribute_heading"), "h3", "extraAttribute_heading");
			var extraContainer = createChunk("","div","extraAttributes");
			appendExtraAttributes(extraContainer, theRow);
			tr.helpDiv.appendChild(extraHeading);
			tr.helpDiv.appendChild(extraContainer);
		}
	}
	
	// update the vote info
	if(theRow.voteResolver) {
		var vr = theRow.voteResolver;
		var div = tr.voteDiv = document.createElement("div");
		tr.voteDiv.className = "voteDiv";
		
		tr.voteDiv.appendChild(document.createElement("hr"));
		
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
				var vdiv = createChunk(null, "div", "voteInfo_perValue");
				
				// heading row
				{
					//var valueExtra = (value==vr.winningValue)?(" voteInfo_iconValue voteInfo_winningItem d-dr-"+theRow.voteResolver.winningStatus):"";
					//var voteExtra = (value==vr.lastReleaseValue)?(" voteInfo_lastRelease"):"";
					var vrow = createChunk(null, "div", "voteInfo_tr voteInfo_tr_heading");
					if(!item.votes || Object.keys(item.votes).length==0) {
						//vrow.appendChild(createChunk("","div","voteInfo_orgColumn voteInfo_td"));
					} else {
						vrow.appendChild(createChunk(stui.str("voteInfo_orgColumn"),"div","voteInfo_orgColumn voteInfo_td"));
					}
					var isection = createChunk(null, "div", "voteInfo_iconBar");
					vrow.appendChild(isection);
					
					var vvalue = createChunk(null, "div", "voteInfo_valueTitle voteInfo_td"+"");
					
					if(value==vr.winningValue) {
						appendIcon(isection,"voteInfo_winningItem d-dr-"+theRow.voteResolver.winningStatus);
					}
					if(value==vr.lastReleaseValue) {
						appendIcon(isection,"voteInfo_lastRelease i-star");
					}
					
					appendItem(vvalue, value, calcPClass(value, vr.winningValue), tr);
					vrow.appendChild(vvalue);
					vrow.appendChild(createChunk(vote,"div","voteInfo_voteTitle voteInfo_td"+""));
					vdiv.appendChild(vrow);
				}
				
				var createVoter = function(v) {
					var div = createChunk(v.email,"div","voteInfo_voterInfo voteInfo_td");
					div.title = v.name + " ("+v.org+")";
					return div;
				};
				
				if(!item.votes || Object.keys(item.votes).length==0) {
					var vrow = createChunk(null, "div", "voteInfo_tr voteInfo_orgHeading");
					//vrow.appendChild(createChunk("","div","voteInfo_orgColumn voteInfo_td"));
					vrow.appendChild(createChunk(stui.str("voteInfo_noVotes"),"div","voteInfo_noVotes voteInfo_td"));
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
								var vrow = createChunk(null, "div", "voteInfo_tr voteInfo_orgHeading");
								vrow.appendChild(createChunk(org,"div","voteInfo_orgColumn voteInfo_td"));
								var isection = createChunk(null, "div", "voteInfo_iconBar");
								vrow.appendChild(isection);
								vrow.appendChild(createVoter(item.votes[topVoter])); // voteInfo_td
								vrow.appendChild(createChunk(orgVoteValue,"div",(orgsVote?"voteInfo_orgsVote ":"voteInfo_orgsNonVote ")+"voteInfo_voteCount voteInfo_td"));
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
									var vrow = createChunk(null, "div", "voteInfo_tr");
									vrow.appendChild(createChunk("","div","voteInfo_orgColumn voteInfo_td")); // spacer
									var isection = createChunk(null, "div", "voteInfo_iconBar");
									vrow.appendChild(isection);
									vrow.appendChild(createVoter(item.votes[voter])); // voteInfo_td
									vrow.appendChild(createChunk(item.votes[voter].votes,"div","voteInfo_orgsNonVote voteInfo_voteCount voteInfo_td"));
									vdiv.appendChild(vrow);
								}
							}
						} else {
							// omit this org - not relevant for this value.
						}
					}
				}
				perValueContainer.appendChild(vdiv);
			}
		} else {
			// ? indicate approved, last release value?
		}
		
		// KEY
		// approved and last release status
		{
			var kdiv = createChunk(null,"div","voteInfo_key");
			tr.voteDiv.appendChild(createChunk(stui.str("voteInfo_key"),"h3","voteInfo_key_title"));
			var disputedText = (theRow.voteResolver.isDisputed)?stui.str("winningStatus_disputed"):"";
			//var p = document.createElement("p");
//			tr.voteDiv.appendChild(createChunk(stui.str("voteInfo_established"),"p","warnText nobg"));
//			tr.voteDiv.appendChild(createChunk(stui.str("voteInfo_lastRelease"),"p","voteInfo_lastReleaseKey voteInfo_iconValue"));
			kdiv.appendChild(createChunk(
						stui.sub("winningStatus_msg",
								[ stui.str(theRow.voteResolver.winningStatus), disputedText ])
						, "div", "voteInfo_winningKey d-dr-"+theRow.voteResolver.winningStatus+" winningStatus"));
//			appendItem(p, theRow.voteResolver.winningValue, "winner",tr);
			
			kdiv.appendChild(createChunk(
					stui.sub("lastReleaseStatus_msg",
							[ stui.str(theRow.voteResolver.lastReleaseStatus) ])
					, "div",  /* "d-dr-"+theRow.voteResolver.lastReleaseStatus+ */ "voteInfo_lastReleaseKey voteInfo_iconValue"));
//			appendItem(p, theRow.voteResolver.lastReleaseValue, "value",tr);
//			p.appendChild(createChunk(
//					stui.sub("lastReleaseStatus1_msg",
//							[ stui.str(theRow.voteResolver.lastReleaseStatus) ])
//					, "b", /* "g"+theRow.voteResolver.lastReleaseStatus+ */ "  lastReleaseStatus1"));

			
			tr.voteDiv.appendChild(kdiv);
		}
		if(theRow.voteResolver.isEstablished) {
			tr.voteDiv.appendChild(createChunk(stui.str("voteInfo_established"),"p","warnText nobg"));
		}
		// done with voteresolver table
		
		if(stdebug_enabled) {
			tr.voteDiv.appendChild(createChunk(vr.raw,"p","debugStuff"));
		}
	} else {
		tr.voteDiv = null;
	}
	
	var statusAction = parseStatusAction(theRow.statusAction);
	var canModify = tr.theTable.json.canModify && statusAction.vote;
    var ticketOnly = canModify && statusAction.ticket;
    var canChange = canModify && statusAction.change;
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
	if(!canModify) {
		protoButton = null; // no voting at all.
	}
	
	var doPopInfo = function(e) {
		popInfoInto(tr,theRow,children[config.statuscell],false);
		stStopPropagation(e); return false; 
	};
	var doPopInfoNow = function(e) {
		popInfoInto(tr,theRow,children[config.statuscell],true);
		stStopPropagation(e); return false; 
	};
		
//	if(theRow.hasErrors) {
////		children[config.errcell].className = "d-st-stop";
////		children[config.errcell].title = stui.testError;
//		children[config.proposedcell].className = 'd-win-err';
//	} else if(theRow.hasWarnings) {
////		children[config.errcell].className = "d-st-warn";
////		children[config.errcell].title = stui.testWarn;
//		children[config.proposedcell].className = 'd-win-warn';
//	} else {
////		children[config.errcell].className = "d-st-okay";
////		children[config.errcell].title = stui.testOkay;
//		children[config.proposedcell].className = 'd-win';
//	}
	
	children[config.statuscell].className = "d-dr-"+theRow.confirmStatus + " d-dr-status";
	if(!children[config.statuscell].isSetup) {
		listenToPop("", tr, children[config.statuscell]);

//		listenFor(children[config.statuscell],"mouseover",
//				doPopInfo);
//		listenFor(children[config.statuscell],"click",  // TODO: change t empty
//				doPopInfoNow);
		children[config.statuscell].isSetup=true;
	}
	children[config.statuscell].title = stui.sub('draftStatus',[stui.str(theRow.confirmStatus)]);

	if(theRow.hasVoted) {
//		children[config.votedcell].className = "d-vo-true";
//		children[config.votedcell].title=stui.voTrue;
		children[config.nocell].title=stui.voTrue;
		children[config.nocell].className= "d-no-vo-true";
	} else {
//		children[config.votedcell].className = "d-vo-false";
//		children[config.votedcell].title=stui.voFalse;
		children[config.nocell].title=stui.voFalse;
		children[config.nocell].className= "d-no-vo-false";
	}
//	if(!children[config.votedcell].isSetup) {
//		listenFor(children[config.votedcell],"mouseover",
//				doPopInfo);
//		listenFor(children[config.votedcell],"click",
//				doPopInfoNow);
//		children[config.votedcell].isSetup=true;
//	}

	if(!tr.anch || stdebug_enabled) {
		if(tr.anch) { // clear out old (only for debug)
			removeAllChildNodes(children[config.codecell]);
		}
		var codeStr = theRow.code;
		if(theRow.coverageValue==101 && !stdebug_enabled) {
			codeStr = codeStr + " (optional)";
		}
		children[config.codecell].appendChild(createChunk(codeStr));
		
		
		if(tr.theTable.json.canModify) { // pointless if can't modify.
	
			if(theRow.forumPosts > 0) {
				children[config.codecell].className = "d-code hasPosts";		
			} else {
				children[config.codecell].className = "d-code";			
			}
	
			
			if(!tr.forumDiv) {
				tr.forumDiv = document.createElement("div");
				tr.forumDiv.className = "forumDiv";
			}			
			tr.forumDiv.forumPosts = theRow.forumPosts;
			tr.forumDiv.forumUpdate = null;
			
			appendForumStuff(tr,theRow, tr.forumDiv);
		}
		
		// extra attributes
		if(theRow.extraAttributes && Object.keys(theRow.extraAttributes).length>0) {
			appendExtraAttributes(children[config.codecell], theRow);
		}
		var anch = document.createElement("a");
		anch.className="anch";
		anch.id=theRow.xpid;
		anch.href="#"+anch.id;
		children[config.codecell].appendChild(anch);
		if(stdebug_enabled) {
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
			js.href="#";
			listenToPop(JSON.stringify(theRow),tr,js);
			children[config.codecell].appendChild(js);
			children[config.codecell].appendChild(createChunk(" c="+theRow.coverageValue));
		}
//		listenFor(children[config.codecell],"click",
//				function(e){ 		
//					showInPop("XPath: " + theRow.xpath, children[config.codecell]);
//					stStopPropagation(e); return false; 
//				});
		var xpathStr = "";
		if((!window.surveyOfficial) || stdebug_enabled) {
			xpathStr = "XPath: " + theRow.xpath;
		}
		listenToPop(xpathStr, tr, children[config.codecell]);
		tr.anch = anch;
	}
	
	
	if(!children[config.comparisoncell].isSetup) {
		if(theRow.displayName) {
			children[config.comparisoncell].appendChild(document.createTextNode(theRow.displayName));
			if(theRow.displayExample) {
				var theExample = appendExample(children[config.comparisoncell], theRow.displayExample);
				listenToPop(null,tr,theExample);
			}
		} else {
			children[config.comparisoncell].appendChild(document.createTextNode(""));
		}
		listenToPop(null,tr,children[config.comparisoncell]);
		children[config.comparisoncell].isSetup=true;
	}
	removeAllChildNodes(children[config.proposedcell]); // win
	tr.proposedcell = children[config.proposedcell];
	if(theRow.items&&theRow.winningVhash) {
		addVitem(children[config.proposedcell],tr,theRow,theRow.items[theRow.winningVhash],theRow.winningVhash,cloneAnon(protoButton));
	} else {
		children[config.proposedcell].showFn = null;  // nothing else to show
	}
	listenToPop(null,tr,children[config.proposedcell], children[config.proposedcell].showFn);
	tr.selectThisRow = function(e) {
		// select the proposed cell:
		//showInPop(null, tr, tr.proposedcell, tr.proposedcell.showFn, true);
		
		// select the 'approved' cell
		//popInfoInto(tr,theRow,children[config.statuscell],true);

		// select the approved cell - no message.
		showInPop('', tr, children[config.statuscell], null, true);


		stStopPropagation(e); return false; 
	};

	listenToPop(null,tr,children[config.errcell], children[config.proposedcell].showFn);
	//listenFor(children[config.errcell],"mouseover",function(e){return children[config.errcell]._onmove(e);});
	
	var hadOtherItems  = false;
	removeAllChildNodes(children[config.othercell]); // other
	for(k in theRow.items) {
		if(k == theRow.winningVhash) {
			continue; // skip the winner
		}
		hadOtherItems=true;
		addVitem(children[config.othercell],tr,theRow,theRow.items[k],k,cloneAnon(protoButton));
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
	if(!children[config.changecell].isSetup) {
		removeAllChildNodes(children[config.changecell]);
		tr.inputTd = children[config.changecell]; // TODO: use  (getTagChildren(tr)[tr.theTable.config.changecell])
		
		if(ticketOnly) { // ticket link
			children[config.changecell].className="d-change-confirmonly";
			var link = createChunk(stui.str("file_a_ticket"),"a");
			var newUrl = BUG_URL_BASE+"/newticket?component=data&summary="+surveyCurrentLocale+":"+theRow.xpath+"&locale="+surveyCurrentLocale+"&xpath="+theRow.xpstrid+"&version="+surveyVersion;
				link.href = newUrl;
				link.target = TARGET_DOCS;
				theRow.proposedResults = createChunk(stui.str("file_ticket_must"), "a","fnotebox");
				theRow.proposedResults.href = newUrl;
				if(!window.surveyOfficial) {
					children[config.changecell].appendChild(createChunk(" (Note: this is not the production SurveyTool!) ","p"));
					link.href = link.href + "&description=NOT+PRODUCTION+SURVEYTOOL!";
				}
			children[config.changecell].appendChild(link);
        } else if(!canChange) { // nothing
        	//if(!canModify) {  // if not showing any other votes..
        	if(!tr.theTable.json.canModify) { // only if hidden in the header
        		children[config.changecell].style.display="none"; // hide the cell 
        	}
        	//}
		} else { // can change
			var changeButton = cloneAnon(protoButton);
			children[config.changecell].appendChild(changeButton);
			var changeBox = cloneAnon(dojo.byId("proto-inputbox"));
			wireUpButton(changeButton,tr, theRow, "[change]",changeBox);
			tr.inputBox = changeBox;
			
			changeBox.onfocus = function() {
				setDefer(true);
				return true;
			};
			changeBox.onblur = function() {
				setDefer(false);
				return true;
			};
			
			children[config.changecell].appendChild(changeBox);
			children[config.changecell].isSetup=true;
			children[config.changecell].theButton = changeButton;
			listenToPop(null, tr, children[config.changecell]);
		}
		
	} else {
		if(children[config.changecell].theButton) {
			children[config.changecell].theButton.className="ichoice-o";
		}
	}
	*/		
	
	if(canModify) {
		removeAllChildNodes(children[config.nocell]); // no opinion
		var noOpinion = cloneAnon(protoButton);
		wireUpButton(noOpinion,tr, theRow, null);
		noOpinion.value=null;
		children[config.nocell].appendChild(noOpinion);
		listenToPop(null, tr, children[config.nocell]);
	} else if (!ticketOnly) {
    	if(!tr.theTable.json.canModify) { // only if hidden in the header
    		children[config.nocell].style.display="none";
    	}
	}
	
	tr.className='vother cov'+theRow.coverageValue;
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
		
		var k = rowList[i];
		var theRow = theRows[k];
		
		var theRowCov = parseInt(theRow.coverageValue);
		if(!newPartition.minCoverage || newPartition.minCoverage > theRowCov) {
			newPartition.minCoverage = theRowCov;
                        if(newPartition.tr) {
                            // only set coverage of the header if there's a header
			    newPartition.tr.className = newPartition.origClass+" cov"+newPartition.minCoverage;
                        }
		}
		
		var tr = theTable.myTRs[k];
		if(!tr) {
			//console.log("new " + k);
			tr = cloneAnon(toAdd);
			theTable.myTRs[k]=tr; // save for later use
		}
//		tr.id="r_"+k;
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
	theSortmode.appendChild(size);
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
		theSortmode.appendChild(ul);
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
	if(window.surveyCurrentCoverage!=null && window.surveyLevels!=null) {
		var newStyle = theTable.origClass;
		for(var k in window.surveyLevels) {
			var level = window.surveyLevels[k];
			
			if(window.surveyCurrentCoverage <  parseInt(level.level)) {
				newStyle = newStyle + " hideCov"+level.level;
			}
		}
		if(newStyle != theTable.className) {
			theTable.className = newStyle;
//			console.log("Table style: "  + newStyle + " for lev " + surveyCurrentCoverage);
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
	if(!theTable) {
		theTable = cloneLocalizeAnon(dojo.byId('proto-datatable'));
		updateCoverage(theDiv);
		localizeFlyover(theTable);
		theTable.theadChildren = getTagChildren(theTable.getElementsByTagName("tr")[0]);
		var toAdd = dojo.byId('proto-datarow');
		/*if(!surveyConfig)*/ {
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
					rowChildren[c].id=null;
				} else {
					stdebug("(proto-datarow #"+c+" has no id");
				}
			}
			if(stdebug_enabled) stdebug("Table Config: " + JSON.stringify(theTable.config));
		}
		theTable.toAdd = toAdd;

		if(!json.canModify) {
				theTable.theadChildren[theTable.config.nocell].style.display="none";
		}
		theTable.sortMode = cloneAnon(dojo.byId('proto-sortmode'));
		theDiv.appendChild(theTable.sortMode);
		theTable.myTRs = [];
		theDiv.theTable = theTable;
		theTable.theDiv = theDiv;
		doInsertTable=theTable;
//		listenFor(theTable,"mouseout",
//				hidePopHandler);
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
		theTable.style.display = '';
//		if(theDiv.theLoadingMessage) {
//			theDiv.theLoadingMessage.style.display="none";
//			theDiv.removeChild(theDiv.theLoadingMessage);
//			theDiv.theLoadingMessage=null;
//		}
	}

	
	hideLoader(theDiv.loader);
}

function loadStui(loc) {
	if(!stui.ready) {
		stui  = dojo.i18n.getLocalization("surveyTool", "stui");
		stui.sub = function(x,y) { return dojo.string.substitute(stui[x], y);};
		stui.str = function(x) { if(stui[x]) return stui[x]; else return x; };
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
		whom.style.display="none";
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
 * Show the "possible problems" section which has errors for the locale
 * @method showPossibleProblems
 */
function showPossibleProblems(container,loc, session, effectiveCov, requiredCov) {
	surveyCurrentLocale = loc;
	dojo.ready(function(){
		var theDiv = dojo.byId(container);

		theDiv.stui = loadStui();
		theDiv.theLoadingMessage = createChunk(stui_str("loading"), "i", "loadingMsg");
		theDiv.appendChild(theDiv.theLoadingMessage);

		var errorHandler = function(err, ioArgs){
			console.log('Error: ' + err + ' response ' + ioArgs.xhr.responseText);
			showLoader(theDiv.loader,stopIcon + "<h1>Could not refresh the page - you may need to <a href='javascript:window.location.reload(true);'>refresh</a> the page if the SurveyTool has restarted..</h1> <hr>Error while fetching : "+err.name + " <br> " + err.message + "<div style='border: 1px solid red;'>" + ioArgs.xhr.responseText + "</div>");
		};
		var loadHandler = function(json){
			try {
				//showLoader(theDiv.loader,stui.loading2);
				theDiv.removeChild(theDiv.theLoadingMessage);
				if(!json) {
					console.log("!json");
					showLoader(theDiv.loader,"Error while  loading: <br><div style='border: 1px solid red;'>" + "no data!" + "</div>");
				} else if(json.err) {
					console.log("json.err!" + json.err);
					showLoader(theDiv.loader,"Error while  loading: <br><div style='border: 1px solid red;'>" + json.err + "</div>");
					handleDisconnect("while loading",json);
				} else if(!json.possibleProblems) {
					console.log("!json.possibleProblems");
					showLoader(theDiv.loader,"Error while  loading: <br><div style='border: 1px solid red;'>" + "no section" + "</div>");
					handleDisconnect("while loading- no possibleProblems result",json);
				} else {
					stdebug("json.possibleProblems OK..");
					//showLoader(theDiv.loader, "loading..");
					if(json.dataLoadTime) {
						updateIf("dynload", json.dataLoadTime);
					}

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
				}

			}catch(e) {
				console.log("Error in ajax post [surveyAjax]  " + e.message + " / " + e.name );
				handleDisconnect("Exception while  loading: " + e.message + ", n="+e.name, null); // in case the 2nd line doesn't work
//				showLoader(theDiv.loader,"Exception while  loading: "+e.name + " <br> " +  "<div style='border: 1px solid red;'>" + e.message+ "</div>");
//				console.log("Error in ajax post   " + e.message);
			}
		};

		var xhrArgs = {
				url: contextPath + "/SurveyAjax?what=possibleProblems&_="+surveyCurrentLocale+"&s="+session+"&eff="+effectiveCov+"&req="+requiredCov+  cacheKill(),
				handleAs:"json",
				load: loadHandler,
				error: errorHandler
		};
		//window.xhrArgs = xhrArgs;
//		console.log('xhrArgs = ' + xhrArgs);
		queueXhr(xhrArgs);
	});
}



/**
 * copy of menu data
 * @property _thePages
 */
var _thePages = null;


/**
 * Utilities for the 'v.jsp' (new dispatcher) page.  Call this once in the page. It expects to find a node #DynamicDataSection
 * @method showV
 */
function showV() {
	window.surveyCurrentCoverage = 100; // comprehensive by default. TODO fix

	// REQUIRES
	require([
	         "dojo/ready",
	         "dojo/dom",
	         "dojo/parser", 
	         "dijit/DropDownMenu",
	         "dijit/form/DropDownButton",
	         "dijit/MenuItem",
	         "dijit/form/TextBox",
	         "dijit/form/Button",
	         "dijit/CheckedMenuItem",
	         "dijit/registry",
	         "dijit/PopupMenuItem",
	         "dijit/form/Select",
	         "dojox/form/BusyButton",
	         "dijit/layout/StackContainer",
	         "dojo/hash",
	         "dojo/topic",
	         "dojo/domReady!"
	         ],
	         // HANDLES
	         function(
	        		 ready,
	        		 dom,
	        		 parser,
	        		 DropDownMenu,
	        		 DropDownButton,
	        		 MenuItem,
	        		 TextBox,
	        		 Button,
	        		 CheckedMenuItem,
	        		 registry,
	        		 PopupMenuItem,
	        		 Select,
	        		 BusyButton,
	        		 StackContainer,
	        		 dojoHash,
	        		 dojoTopic
	         ) {

		var pages = { 
				loading: "LoadingMessageSection",
				data: "DynamicDataSection",
				other: "OtherSection"
		};
		var flipper = new Flipper( [pages.loading, pages.data, pages.other] );
		
		// TODO remove debug
		window.__FLIPPER = flipper;
		
		/**
		 * parse the hash string., but don't go anywhere.
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
						surveyCurrentSpecial='none';
					}
					surveyCurrentPage = '';
					surveyCurrentId = '';
				}
			} else {
				surveyCurrentLocale = '';
				surveyCurrentSpecial='none';
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
			    if(theLocale=='') {
			    	updateIf("title-item",'');
    			    //itemBox.set('value', '');
			    } else if(theId=='' && thePage!='') {
			    	updateIf("title-item", theLocale+'/'+thePage+'/');
			        //itemBox.set('value', theLocale+'/'+thePage+'/');
			    } else {
			    	updateIf("title-item",theLocale+'//'+theId);
    			    //itemBox.set('value', theLocale+'//'+theId);
			    }
//			}
			document.title = document.title.split('|')[0] + " | " + theSpecial + '/' + theLocale + '/' + thePage + '/' + theId;
		};
		
		// click on the title to copy (permalink)
		clickToSelect(dojo.byId("title-item"));

		window.updateCurrentId = function updateCurrentId(id) {
			if(id==null) id = '';
		    if(surveyCurrentId != id) { // don't set if already set.
			    surveyCurrentId = id;
			    replaceHash(false); // usually dont want to save
		    }
		};
		
		// TODO - rewrite using AMD
		/**
		 * @param postData optional - makes this a POST
		 */
		function myLoad(url, message, handler, postData, headers) {
			console.log("Loading " + url + " for " + message);
			var errorHandler = function(err, ioArgs){
				console.log('Error: ' + err + ' response ' + ioArgs.xhr.responseText);
				showLoader(null,stopIcon + "<h1>Could not refresh the page - you may need to <a href='javascript:window.location.reload(true);'>refresh</a> the page if the SurveyTool has restarted..</h1> <hr>Error while fetching : "+err.name + " for " + message + " <br> " + err.message + "<div style='border: 1px solid red;'>" + ioArgs.xhr.responseText + "</div>");
			};
			var loadHandler = function(json){
				try {
					handler(json);
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
		}
		
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
			        scrollToItem();
			    } else {
			        console.log("Warning could not load id " + surveyCurrentId + " - not setup - " + xtr.toString() + " pc=" + xtr.proposedcell + " sf = " + xtr.proposedcell.showFn);
			    }
			}
		};
		
		/**
		 * Update the #hash and menus to the current settings.
		 * @method updateHashAndMenus
		 * @param doPush {Boolean} if false, do not add to history
		 */
		function updateHashAndMenus(doPush) {
			if(!doPush) {doPush = false;}
			replaceHash(doPush); // update the hash
			updateIf("title-locale", surveyCurrentLocaleName);
			

			if(surveyCurrentLocale==null) {
				updateIf("title-section","-");
				if(surveyCurrentSpecial!=null) {
					updateIf("title-page",stui_str("special_"+surveyCurrentSpecial));
				} else {
					updateIf("title-page","-");
				}
				updateIf("title-id","");
				// update special title?
				return; // nothing to do.
			}
			/**
			 * Just update the titles of the menus. Internal to updateHashAndMenus
			 * @method updateMenuTitles
			 */
			function updateMenuTitles(menuMap) {
				if(surveyCurrentSpecial!= null) {
					updateIf("title-section",stui_str("section_special"));
					updateIf("title-page",stui_str("special_"+surveyCurrentSpecial));
				} else if(!menuMap) {
					updateIf("title-section", "-");
					updateIf("title-page", surveyCurrentPage); 
				} else {
					if(menuMap.pageToSection[window.surveyCurrentPage]) {
						var mySection = menuMap.pageToSection[window.surveyCurrentPage];
						var myPage = mySection.pageMap[window.surveyCurrentPage];
						surveyCurrentSection = mySection.id;
						updateIf("title-section", mySection.name);
						updateIf("title-page", myPage.name);
					} else {
						updateIf("title-section", "-");
						updateIf("title-page", "-");
					}
				}
			}

			/**
			 * @method updateMenus
			 */
			function updateMenus(menuMap) {
				updateMenuTitles(menuMap);

				var myPage = null;
				var mySection = null;
				if(surveyCurrentSpecial==null) {
					// first, update display names
					if(menuMap.pageToSection[window.surveyCurrentPage]) {
						mySection = menuMap.pageToSection[surveyCurrentPage];
						myPage = mySection.pageMap[surveyCurrentPage];
						// update menus under 'page' - peer pages
						var menuPage = registry.byId("menu-page");
						menuPage.destroyDescendants(false);
						for(var k in mySection.pages) { // use given order
							(function(aPage) {
								var pageMenu = new CheckedMenuItem({
									label: aPage.name,
									checked:   (aPage.id == surveyCurrentPage),
									//    iconClass:"dijitEditorIcon dijitEditorIconSave",
									onClick: function(){ 
										surveyCurrentId = ''; // no id if jumping pages
										surveyCurrentPage = aPage.id;
										updateMenuTitles(menuMap);
										reloadV();
									},
									disabled: (window.surveyCurrentCoverage!=null && 
											parseInt(window.surveyCurrentCoverage)<parseInt(aPage.levs[surveyCurrentLocale]))
								});
								menuPage.addChild(pageMenu);
							})(mySection.pages[k]);

						}
					}				

				}

				var menuSection = registry.byId("menu-section");
				menuSection.destroyDescendants(false);
				for(var j in menuMap.sections) {
					(function (aSection){
						var dropDown = new DropDownMenu();
						for(var k in aSection.pages) { // use given order
							(function(aPage) {
								var pageMenu = new CheckedMenuItem({
									label: aPage.name,
									checked:   (aPage.id == surveyCurrentPage),
									//    iconClass:"dijitEditorIcon dijitEditorIconSave",
									disabled: (window.surveyCurrentCoverage!=null && 
											parseInt(window.surveyCurrentCoverage)<parseInt(aPage.levs[surveyCurrentLocale])),
									onClick: function(){ 
										surveyCurrentId = ''; // no id if jumping pages
										surveyCurrentPage = aPage.id;
										updateMenuTitles(menuMap);
										reloadV();
									},
								});
								dropDown.addChild(pageMenu);
							})(aSection.pages[k]);

						}
						var sectionMenuItem = new PopupMenuItem({
							label: aSection.name,
							popup: dropDown,
						});
						menuSection.addChild(sectionMenuItem);
					})(menuMap.sections[j]);
				}
			}

			if(_thePages == null || _thePages.loc != surveyCurrentLocale ) {
				// show the raw IDs while loading.
				updateMenuTitles(null);
				
				if(surveyCurrentLocale!=null&&surveyCurrentLocale!='') {
				
					var url = contextPath + "/SurveyAjax?_="+surveyCurrentLocale+"&s="+surveySessionId+"&what=menus"+cacheKill();
					myLoad(url, "menus for " + surveyCurrentLocale, function(json) {
						{
							if(!window.surveyLevels) {
								window.surveyLevels = json.menus.levels;

								var titleCoverage = dojo.byId("title-coverage"); // coverage label

								var levelNums = [];  // numeric levels
								for(var k in window.surveyLevels) {
									levelNums.push( { num: parseInt(window.surveyLevels[k].level), level: window.surveyLevels[k] } );
								}
								levelNums.sort(function(a,b){return a.num-b.num;});

								var store = [];

								store.push({label: "Auto",
									disabled: true,
									selected: false,
									value: 0});

								for(var j in levelNums) { // use given order
									if(levelNums[j].num==0) continue;
									if(window.surveyOfficial && levelNums[j].num==101) continue; // hide Optional in production
									var level = levelNums[j].level;
									var isSelected = false;
									if(window.surveyCurrentCoverage) {
										isSelected = (parseInt(window.surveyCurrentCoverage)==levelNums[j].num);
									}
//									console.log("selected " + isSelected + " for " + level.name + " vs " + window.surveyCurrentCoverage);
									store.push({label: level.name, 
														selected: isSelected,
														value: level.level});
								}
								// TODO have to move this out of the DOM..
								var covMenu = flipper.get(pages.data).covMenu = new Select({name: "menu-select", 
																				options: store,
																				onChange: function(newValue) {
																					window.surveyCurrentCoverage = parseInt(newValue);
																					updateCoverage(flipper.get(pages.data));
																					updateHashAndMenus(false);
																				}
																				});
								covMenu.placeAt(titleCoverage);
							}

							updateCoverage(flipper.get(pages.data)); // TODO
						}
						
						var menus = json.menus;
						// set up some hashes
						menus.sectionMap = {};
						menus.pageToSection = {};
						for(var k in menus.sections) {
							menus.sectionMap[menus.sections[k].id] = menus.sections[k];
							menus.sections[k].pageMap = {};
							for(var j in menus.sections[k].pages) {
								menus.sections[k].pageMap[menus.sections[k].pages[j].id] = menus.sections[k].pages[j];
								menus.pageToSection[menus.sections[k].pages[j].id] = menus.sections[k];
							}
						}
						updateMenus(menus);
						_thePages = menus;
					});
				} else {
					_thePages = null;
				}
			} else {
				// go ahead and update
				updateMenus(_thePages);
			}

		}
		
		window.reloadV = function reloadV() {
			// assume parseHash was already called, if we are taking input from the hash

			window.surveyCurrentLocaleName = '-'; // so it's not stale
			
			var pucontent = dojo.byId("itemInfo");

			{
				var theDiv = flipper.get(pages.data);
				theDiv.pucontent = pucontent;
				theDiv.stui = loadStui();
			}
			
			var loadingChunk;
			flipper.flipTo(pages.loading, loadingChunk = createChunk(stui_str("loading"), "i", "loadingMsg"));
			{
				var timerToKill = null;
				flipper.addUntilFlipped(function() {
//					console.log("Starting throbber");
					var frag = document.createDocumentFragment();
					var k = 0;
					timerToKill = window.setInterval(function() {
						k++;
						loadingChunk.style.opacity =   0.5 + ((k%10) * 0.05);
//						console.log("Throb to " + loadingChunk.style.opacity);
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
				updateHashAndMenus(true);

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
				
				if(surveyCurrentSpecial == null && surveyCurrentLocale!=null && surveyCurrentLocale!='') {
					if(surveyCurrentPage==null || surveyCurrentPage=='') {
						flipper.get(pages.loading).appendChild(document.createElement('br'));
						flipper.get(pages.loading).appendChild(document.createTextNode(surveyCurrentLocale));
						showPossibleProblems(flipper.flipToEmpty(pages.other), surveyCurrentLocale, surveySessionId, "modern", "modern");
					} else {
						flipper.get(pages.loading).appendChild(document.createElement('br'));
						flipper.get(pages.loading).appendChild(document.createTextNode(surveyCurrentLocale + '/' + surveyCurrentPage + '/' + surveyCurrentId));
						var url = contextPath + "/RefreshRow.jsp?json=t&_="+surveyCurrentLocale+"&s="+surveySessionId+"&x="+surveyCurrentPage+"&strid="+surveyCurrentId+cacheKill();
						myLoad(url, "(loading vrows)", function(json) {
							showLoader(theDiv.loader,stui.loading2);
							if(!json) {
								console.log("!json");
								showLoader(theDiv.loader,"Error while  loading: <br><div style='border: 1px solid red;'>" + "no data!" + "</div>");
							} else if(json.err) {
								console.log("json.err!" + json.err);
								showLoader(theDiv.loader,"Error while  loading: <br><div style='border: 1px solid red;'>" + json.err + "</div>");
								handleDisconnect("while loading",json);
							} else if(!json.section) {
								console.log("!json.section");
								showLoader(theDiv.loader,"Error while  loading: <br><div style='border: 1px solid red;'>" + "no section" + "</div>");
								handleDisconnect("while loading- no section",json);
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
								surveyCurrentLocaleName = json.localeDisplayName;
								updateHashAndMenus();

								showInPop2("", null, null, null, true); /* show the box the first time */
								doUpdate(theDiv.id, function() {
									showLoader(theDiv.loader,stui.loading3);
									insertRows(theDiv,json.pageId,surveySessionId,json); // pageid is the xpath..
									flipper.flipTo(pages.data); // TODO now? or later?
									window.showCurrentId(); // already calls scroll
								});
							}
						});
					}
				} else if(surveyCurrentSpecial =='oldvotes') {
					var theDiv = flipper.flipToEmpty(pages.other); // clean slate, and proceed..
					var url = contextPath + "/SurveyAjax?what=oldvotes&_="+surveyCurrentLocale+"&s="+surveySessionId+"&"+cacheKill();
					myLoad(url, "(loading oldvotes " + surveyCurrentLocale + ")", function(json) {
						showLoader(theDiv.loader,stui.loading2);
						if(!verifyJson(json, 'oldvotes')) {
							return;
						} else {
							showLoader(theDiv.loader, "loading..");
							if(json.dataLoadTime) {
								updateIf("dynload", json.dataLoadTime);
							}
							
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
										li.appendChild(createChunk(data[k][header.COUNT]));
										
										ul.appendChild(li);
									}
									
									theDiv.appendChild(ul);
									
									theDiv.appendChild(createChunk(stui.str("v_oldvotes_locale_list_help"),"i")); // TODO fix
								} else {
									theDiv.appendChild(createChunk(stui.str("v_oldvotes_no_old"),"i")); // TODO fix
								}
							} else {
								surveyCurrentLocale=json.oldvotes.locale;
								surveyCurrentLocaleName=json.oldvotes.localeDisplayName;
								updateHashAndMenus();
								var loclink;
								theDiv.appendChild(loclink=createChunk(stui.str("v_oldvotes_return_to_locale_list"),"a"));
								listenFor(loclink, "click", function(e) {
									surveyCurrentLocale='';
									reloadV();
									stStopPropagation(e);
									return false;
								});
								loclink.href='#';
								theDiv.appendChild(createChunk(json.oldvotes.localeDisplayName,"h3","v-title2"));

								if(json.oldvotes.contested.length > 0 || json.oldvotes.uncontested > 0) {
									theDiv.appendChild(createChunk(stui.sub("v_oldvotes_uncontested",{uncontested:json.oldvotes.uncontested,  contested: json.oldvotes.contested.length }),"p","info"));

									if(json.oldvotes.contested.length > 0) {

										var t = document.createElement("table");
										t.id = 'oldVotesAcceptList';
										var th = document.createElement("thead");
										var tb = document.createElement("tbody");
										{
											var tr = document.createElement("tr");
											tr.appendChild(createChunk(stui.str("v_oldvotes_path"),"th","code"));
											tr.appendChild(createChunk(stui.str("v_oldvotes_winning"),"th","v-win"));
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
										for(var k in json.oldvotes.contested) {
											var row = json.oldvotes.contested[k];
											var tr = document.createElement("tr");
											var tdp;
											tr.appendChild(tdp = createChunk(row.pathHeader,"td","v-path"));
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
										theDiv.appendChild(t);

										theDiv.appendChild(createChunk(stui.sub("v_oldvotes_uncontested",{uncontested:json.oldvotes.uncontested,  contested: json.oldvotes.contested.length }),"p","info"));
									} else {
										theDiv.appendChild(createChunk(stui.str("v_oldvotes_no_contested"),"i",""));
									}
									var submit = BusyButton({
//										id: 'oldVotesSubmit',
										label: stui.str("v_submit"),
										busyLabel: stui.str("v_submit_busy")
									});

									submit.placeAt(theDiv);


									submit.on("click",function(e) {
										var confirmList= []; // these will be revoted with current params
										var deleteList = []; // these will be deleted

										// explicit confirm/delete list -  save us desync hassle
										for(var kk in json.oldvotes.contested ) {
											if(json.oldvotes.contested[kk].box.checked) {
												confirmList.push(json.oldvotes.contested[kk].strid);
											} else {
												deleteList.push(json.oldvotes.contested[kk].strid);
											}
										}

										var saveList = {
												locale: surveyCurrentLocale,
												confirmList: confirmList,
												deleteList: deleteList
										};

										console.log(saveList.toString());
										console.log("Submitting " + confirmList.length + " for confirm and " + deleteList.length + " for deletion");

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

								} else {
									theDiv.appendChild(createChunk(stui.str("v_oldvotes_no_old_here"),"i",""));
								}
							}
						}
						hideLoader(null);
					});
				} else if(surveyCurrentSpecial == 'none') {
					//for now - redurect
					hideLoader(null);
					window.location = survURL; // redirect home
				} else {
					var msg = stui.sub("v_bad_special_msg",
							{special: surveyCurrentSpecial });
					flipper.flipTo(pages.loader, createChunk(msg /* , ?? , ?? sterror? */));
					showLoader(theDiv.loader, msg);
				}
			}; // end shower

			shower(); // first load
			theDiv.shower = shower;
			showers[theDiv.id]=shower;

		};  // end reloadV

		ready(function(){
			window.parseHash(dojoHash()); // get the initial settings
			window.reloadV(); // call it

			function trimNull(x) {
				if(x==null) return '';
				x = x.toString().trim();
				return x;
			}
			
			dojoTopic.subscribe("/dojo/hashchange", function(changedHash){
				//alert("hashChange…" + changedHash);
				if(true) {
					
					
					var oldLocale = trimNull(surveyCurrentLocale);
					var oldSpecial = trimNull(surveyCurrentSpecial);
					var oldPage = trimNull(surveyCurrentPage);
					var oldId = trimNull(surveyCurrentId);
					
					window.parseHash(changedHash);
					
					surveyCurrentID = trimNull(surveyCurrentId);
					
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
				} else {
				    console.log("Ignoring hash change " + changedHash);
				}
			});
			
//			// wire up the 'ID' box
//			registry.byId("title-item").set('onChange',
//			    function(v){
//					if(ignoreIdChange) return;
//			        v = trimNull(v);
////			        console.log('User entered: ' + v);
//                    var parts = v.split('/');
//                    var newLoc = '';
//                    var newPage = '';
//                    var newId = '';
//                    if(parts.length>0 && parts[0]!='') {
//                        newLoc = trimNull(parts[0]);
//                        if(parts.length>1 && parts[1]!='') {
//                            newPage = trimNull(parts[1]);
//                            if(parts.length>2 && parts[2]!='') {
//                                newId = trimNull(parts[2]);
//                            }
//                        }
//                    }
//                    
//                    if(parts.length>1) {
//                        if((newLoc!=surveyCurrentLocale && newLoc!='') || (newPage!=surveyCurrentPage && newPage!='')) {
//                            if(newLoc != '') {
//                                surveyCurrentLocale=newLoc;
//                            }
//                            surveyCurrentPage = newPage;
//                            surveyCurrentId = newId;
//                            reloadV();
//                        } else if(newId!=surveyCurrentId) {
//                            surveyCurrentId = newId;
//                            updateHashAndMenus();
//                            scrollToItem();
//                        } else {
//                            console.log("Ignoring user changed id: " + v);
//                            updateHashAndMenus(); // reject entry
//                        }
//			        // if it is a current page
//			        } else if(_thePages!=null && _thePages.pageToSection[v] &&
//			            _thePages.pageToSection[v].pageMap[v]) {
//			                surveyCurrentPage = v;
//			                surveyCurrentId='';
//			                reloadV();
//	                } else if(v.substr(0,1)=='#') {
//		                surveyCurrentId=v.substr(1);
//		                updateHashAndMenus();
//		                //reloadV();
//						scrollToItem();
//		            } else {
//                        console.log("Ignoring user changed (short) id: " + v);
//                        updateHashAndMenus(); // reject entry
//		            }
//			    }
//			);
		});

	});  // end require()
} // end showV


/**
 * reload a specific row
 * @method refreshRow2
 */
function refreshRow2(tr,theRow,vHash,onSuccess, onFailure) {
	showLoader(tr.theTable.theDiv.loader,stui.loadingOneRow);
    var ourUrl = contextPath + "/RefreshRow.jsp?what="+WHAT_GETROW+"&xpath="+theRow.xpid +"&_="+surveyCurrentLocale+"&fhash="+tr.rowHash+"&vhash="+vHash+"&s="+tr.theTable.session +"&json=t";
    var loadHandler = function(json){
        try {
	    		if(json&&json.dataLoadTime) {
	    			updateIf("dynload", json.dataLoadTime);
	    		}
        		if(json.section.rows[tr.rowHash]) {
        			theRow = json.section.rows[tr.rowHash];
        			tr.theTable.json.section.rows[tr.rowHash] = theRow;
        			updateRow(tr, theRow);
        			hideLoader(tr.theTable.theDiv.loader);
        			onSuccess(theRow);
        		} else {
        	        tr.className = "ferrbox";
        	        tr.innerHTML="No content found "+tr.rowHash+ "  while  loading";
        	        console.log("could not find " + tr.rowHash + " in " + json);
        	        onFailure("no content");
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
						hideLoader(tr.theTable.theDiv.loader);
						if(json.testResults && (json.testWarnings || json.testErrors)) {
							// tried to submit, have errs or warnings.
							showProposedItem(tr.inputTd,tr,theRow,valToShow,json.testResults); // TODO: use  inputTd= (getTagChildren(tr)[tr.theTable.config.changecell])
						} else {
							hidePop(tr);
							// TODO: hidden after submit - should instead update.
						}
						if(box) {
							box.value=""; // submitted - dont show.
						}
						//tr.className = 'vother';
						myUnDefer();
					}, myUnDefer); // end refresh-loaded-fcn
					// end: async
				} else {
					// Did not submit. Show errors, etc
					if(
							(json.statusAction&&json.statusAction!='ALLOW')
						|| (json.testResults && (json.testWarnings || json.testErrors ))) {
						showProposedItem(tr.inputTd,tr,theRow,valToShow,json.testResults,json); // TODO: use  inputTd= (getTagChildren(tr)[tr.theTable.config.changecell])
					} else {
						hidePop(tr);
						// TODO: not submitted, but no errors.  Refresh row and show?
					}
					if(box) {
						box.value=""; // submitted - dont show.
					}
					//tr.className='vother';
					button.className='ichoice-o';
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
					user.appendChild(createChunk("Last: " + cs.last + ", IP: " + cs.ip, "span","adminUserInfo"));
					
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
				if(json.threads.dead) {
					frag2.appendChunk(json.threads.dead.toString(),"span","adminDeadThreads");
					// TODO
				}
				for(id in json.threads.all) {
					var t = json.threads.all[id];
					var thread = createChunk(null,"div","adminThread");
					thread.appendChild(createChunk(id,"span","adminThreadId"));
					thread.appendChild(createChunk(t.name,"span","adminThreadName"));
					thread.appendChild(createChunk(stui.str(t.state),"span","adminThreadState_"+t.state));
					thread.onclick=(function (t,id){return (function() {
						stack.innerHTML = "<b>"+id+":"+t.name+"</b>\n";
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

//////////////////
/// stats
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

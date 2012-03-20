// survey.js  -Copyright (C) 2012 IBM Corporation and Others. All Rights Reserved.
// move anything that's not dynamically generated here.

// These need to be available @ bootstrap time.

dojo.require("dojo.i18n");
dojo.require("dojo.string");
dojo.requireLocalization("surveyTool", "stui");

var stui = {online: "Online", 			disconnected: "Disconnected", startup: "Starting up..."};

function dismissChangeNotice() {
	surveyCurrentLocaleStamp = surveyNextLocaleStamp;
    var locDiv = document.getElementById('stchanged');
    if(locDiv) {
        locDiv.style.display='none';
    }
}

function removeAllChildNodes(td) {
	while(td.firstChild) {
		td.removeChild(td.firstChild);
	}
}

function readyToSubmit(fieldhash) {
//    var ch_input = document.getElementById('ch_'+fieldhash);
//    var submit_btn = document.getElementById('submit_'+fieldhash);
//    var cancel_btn = document.getElementById('cancel_'+fieldhash);
    
//    ch_input.disabled='1';
//    submit_btn.style.display='block';
//    cancel_btn.style.display='block';
}

function hideSubmit(fieldhash) {
//    var ch_input = document.getElementById('ch_'+fieldhash);
//    var submit_btn = document.getElementById('submit_'+fieldhash);
    var cancel_btn = document.getElementById('cancel_'+fieldhash);

//    submit_btn.style.display='none';
    cancel_btn.style.display='none';
//    ch_input.disabled=null;
}

function isubmit(fieldhash,xpid,locale,session) {
    var ch_input = document.getElementById('ch_'+fieldhash);
//    var submit_btn = document.getElementById('submit_'+fieldhash);
//    var cancel_btn = document.getElementById('cancel_'+fieldhash);
    
    if(!ch_input) {
    	console.log("Not a field hash: submit " + fieldhash);
    	return;
    }
    hideSubmit(fieldhash);
    ch_input.disabled=null;
    do_change(fieldhash, ch_input.value, '', xpid,locale,session,'submit');
}

function icancel(fieldhash,xpid,locale,session) {
    var ch_input = document.getElementById('ch_'+fieldhash);
//    var chtd_input = document.getElementById('chtd_'+fieldhash);
//    var submit_btn = document.getElementById('submit_'+fieldhash);
    var cancel_btn = document.getElementById('cancel_'+fieldhash);
    
    if(!ch_input) {
        console.log("Not a field hash: submit " + fieldhash);
        return;
    }
    cancel_btn.style.display='none';
//    submit_btn.style.display='none';
    ch_input.disabled=null;
//    chtd_input.style.width='25%';
    ch_input.className='inputboxbig';
    alreadyVerifyValue[fieldhash]=null;
}

var timerID = -1;

function updateIf(id, txt) {
    var something = document.getElementById(id);
    if(something != null) {
        something.innerHTML = txt;
    }
}

// work around IE8 fail
function listenFor(what, event, fn, ievent) {
	if(what.addEventListener) {
		what.addEventListener(event,fn,false);
	} else {
		if(!ievent) {
			ievent = "on"+event;
		}
		what.attachEvent(ievent,fn);
	}
}

// ?!!!!
if(!Object.keys) {
	Object.keys = function(x) {
		var r = [];
		for (j in x) {
			r.push(j);
		}
		return r;
	};
}

function getAbsolutePosition (x) {
    var hh = 0;
    var vv = 0;
    for(var xx=x;xx.offsetParent;xx=xx.offsetParent) {
        hh += xx.offsetLeft;
        vv += xx.offsetTop;
    }
    return {left:hh, top: vv};
}


var wasBusted = false;
var wasOk = false;
var loadOnOk = null;
var clickContinue = null;
 var surveyCurrentLocaleStamp = 0;
 var surveyNextLocaleStamp = 0;
 
 function busted() {
	 document.getElementsByTagName("body")[0].className="disconnected"; // hide buttons when disconnected.
 }

// hashtable of items already verified
var alreadyVerifyValue = {};

var showers={};

var deferUpdates = false;
var deferUpdateFn = {};


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

function deferUpdate(what, fn) {
	deferUpdateFn[what]=fn;
}

function undeferUpdate(what) {
	deferUpdateFn[what]=null;
}

function doUpdate(what,fn) {
	if(deferUpdates) {
		updateAjaxWord(stui.newDataWaiting);
		deferUpdate(what,fn);
	} else {
		fn();
		undeferUpdate(what);
	}
}

function handleChangedLocaleStamp(stamp,name) {
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
	console.log("Reloaded due to change: " + stamp);
    surveyNextLocaleStamp = stamp;
}

////    updateStatusBox({err: err.message, err_name: err.name, disconnected: true});
/*
 * {
"status": {
"memfree": 378.6183984375,
"lockOut": false,
"pages": 16,
"specialHeader": "Welcome to SurveyTool@oc7426272865.ibm.com. Please edit /home/srl/apache-tomcat-7.0.8/cldr/cldr.properties to change CLDR_HEADER (to change this message), or comment it out entirely.",
"dbopen": 0,
"users": 1,
"uptime": "uptime: 49:47",
"isUnofficial": true,
"memtotal": 492.274,
"sysprocs": 8,
"isSetup": true,
"sysload": 0.33,
"dbused": 1439,
"guests": 0,
"surveyRunningStamp": 1331941056940
},
"isSetup": "0",
"err": "",
"visitors": "",
"SurveyOK": "1",
"uptime": "",
"isBusted": "0",
"progress": "(obsolete-progress)"
}
 */

var progressWord = null;
var ajaxWord = null;

function showWord() {
	var p = dojo.byId("progress");	
	var oneword = dojo.byId("progress_oneword");
	if(progressWord&&progressWord=="disconnected") { // top priority
		oneword.innerHTML = stopIcon +  stui.disconnected;
		p.className = "progress-disconnected";
		busted();
	} else if(ajaxWord) {
		p.className = "progress-ok";
		oneword.innerHTML = ajaxWord;
	} else if(!progressWord || progressWord == "ok") {
		p.className = "progress-ok";
		oneword.innerHTML = stui.online;
	} else if(progressWord=="startup") {
		p.className = "progress-ok";
		oneword.innerHTML = stui.startup;
	}
}

function updateProgressWord(prog) {
	progressWord = prog;
	showWord();
}

function updateAjaxWord(ajax) {
	ajaxWord = ajax;
	showWord();
}

function updateStatusBox(json) {
	if(json.disconnected || (json.SurveyOK==0) || (json.status && json.status.isBusted)
				|| !json.status || (json.status.surveyRunningStamp!=surveyRunningStamp)) {
		updateProgressWord("disconnected");
	} else if(json.status && json.status.isSetup==false && json.SurveyOK==1) {
		updateProgressWord("startup");
	} else {
		updateProgressWord("ok");
	}
}

function updateStatus() {
    dojo.xhrGet({
        url: contextPath + "/SurveyAjax?what=status"+surveyLocaleUrl,
        handleAs:"json",
        load: function(json){
            if(json.status&&json.status.isBusted) {
                wasBusted = true;
                busted();
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
                if(surveyCurrentLocaleStamp==0) {
                	surveyCurrentLocaleStamp = json.localeStamp;
                    //console.log("STATUS0: " + json.localeStampName + "="+json.localeStamp);
                } else {
                	if(json.localeStamp > surveyCurrentLocaleStamp) {
                		handleChangedLocaleStamp(json.localeStamp, json.localeStampName);
                	} else {
                        //console.log("STATUS=: " + json.localeStampName + "="+json.localeStamp);
                	}
                }
            }
            
            if((wasBusted == false) && (json.status.isSetup) && (loadOnOk != null)) {
                window.location.replace(loadOnOk);
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

function updateTestResults(fieldhash, testResults, what) {
    var e_div = document.getElementById('e_'+fieldhash);
    var v_td = document.getElementById('i_'+fieldhash);
    var v_tr = document.getElementById('r_'+fieldhash);
    var v_tr2 = document.getElementById('r2_'+fieldhash);
    var newHtml = "";
    e_div.className="";
    v_td.className="v_warn";
    if(v_tr!=null) {
            v_tr.className="";
    }
    v_tr2.className="tr_warn";
    newHtml = "";
    
    if(testResults)
    for(var i=0;i<testResults.length;i++) {
        var tr = testResults[i];
        newHtml += "<p class='tr_"+tr.type+"' title='"+tr.type+"'>";
        if(tr.type == 'Warning') {
            newHtml += warnIcon;
            //what='warn';
        } else if(tr.type == 'Error') {
            v_tr2.className="tr_err";
            newHtml += stopIcon;
            what='error';
        }
        newHtml += testResults[i].message;
        newHtml += "</p>";
    }
    e_div.innerHTML = newHtml;
    return what;
}


function refreshRow(fieldhash, xpid, locale, session) {
    var v_tr = document.getElementById('r_'+fieldhash);
    var e_div = document.getElementById('e_'+fieldhash);
    var what = WHAT_GETROW;
    var ourUrl = contextPath + "/RefreshRow.jsp?what="+what+"&xpath="+xpid +"&_="+locale+"&fhash="+fieldhash+"&vhash="+''+"&s="+session;
    var ourUrlVI =  ourUrl+"&voteinfo=t";
    //console.log("refreshRow('" + fieldhash +"','"+value+"','"+vhash+"','"+xpid+"','"+locale+"','"+session+"')");
    //console.log(" url = " + ourUrl);
    var errorHandler = function(err, ioArgs){
        console.log('Error in refreshRow: ' + err + ' response ' + ioArgs.xhr.responseText);
        v_tr.className="tr_err";
        v_tr.innerHTML = "<td class='v_error' colspan=8>" + stopIcon + " Couldn't reload this row- please refresh the page. <br>Error: " + err+"</td>";
        removeAllChildNodes(e_div);
//        var st_err =  document.getElementById('st_err');
//        wasBusted = true;
//        st_err.className = "ferrbox";
//        st_err.innerHTML="Disconnected from Survey Tool while processing a field: "+err.name + " <br> " + err.message;
//        updateIf('progress','<hr><i>(disconnected from Survey Tool)</i></hr>');
//        updateIf('uptime','down');
//        updateIf('visitors','nobody');
    };
    var loadHandler = function(text){
        try {
//             var newHtml = "";
             if(text) {
                 v_tr.className='topbar';
                 v_tr.innerHTML = text;
                 removeAllChildNodes(e_div);
                 e_div.className="";
             } else {
                 v_tr.className='';
                 v_tr.innerHTML = "<td colspan=4>" + stopIcon + " Couldn't reload this row- please refresh the page.</td>";
             }
           }catch(e) {
               console.log("Error in ajax get ",e.message);
               console.log(" response: " + text);
               e_div.innerHTML = "<i>JavaScript Error: " + e.message + "</i>";
           }
    };
    var xhrArgs = {
            url: ourUrl,
            handleAs:"text",
            load: loadHandler,
            error: errorHandler
        };
    window.xhrArgs = xhrArgs;
    //console.log('xhrArgs = ' + xhrArgs);
    dojo.xhrGet(xhrArgs);
    
    var voteinfo_div = document.getElementById('voteresults_'+fieldhash);
    if(voteinfo_div) {
    	voteinfo_div.innerHTML="<i>Updating...</i>";
	    var loadHandlerVI = function(text){
	        try {
//	             var newHtml = "";
	             if(text) {
	                 voteinfo_div.innerHTML = text;
	                 voteinfo_div.className="";
	             } else {
	                 voteinfo_div.className='';
	                 voteinfo_div.innerHTML = "<td colspan=4>" + stopIcon + " Couldn't reload this row- please refresh the page.</td>";
	             }
	           }catch(e) {
	               console.log("Error in ajax get ",e.message);
	               console.log(" response: " + text);
	               voteinfo_div.innerHTML = "<i>Internal Error: " + e.message + "</i>";
	           }
	    };
        var xhrArgsVI = {
                url: ourUrlVI,
                handleAs:"text",
                load: loadHandlerVI,
                error: errorHandler
            };
        //console.log('urlVI = ' + ourUrlVI);
	    dojo.xhrGet(xhrArgsVI);
    }
    
}

// for validating CLDR data values
// do_change(hash, value, xpid, locale)
// divs:    e_HASH = div under teh item
//           v_HASH - the entire td
function do_change(fieldhash, value, vhash,xpid, locale, session,what) {
	var e_div = document.getElementById('e_'+fieldhash);
//	var v_td = document.getElementById('i_'+fieldhash);
    var v_tr = document.getElementById('r_'+fieldhash);
    var v_tr2 = document.getElementById('r2_'+fieldhash);
    alreadyVerifyValue[fieldhash]=null;
    if(what==null) {
		 what = WHAT_SUBMIT;
	}
	if((!vhash || vhash.length==0) && (!value || value.length==0)) {
		return;
	}
	var ourUrl = contextPath + "/SurveyAjax?what="+what+"&xpath="+xpid +"&_="+locale+"&fhash="+fieldhash+"&vhash="+vhash+"&s="+session;
	//console.log("do_change('" + fieldhash +"','"+value+"','"+vhash+"','"+xpid+"','"+locale+"','"+session+"')");
//	console.log(" what = " + what);
//	console.log(" url = " + ourUrl);
    hideSubmit(fieldhash);
	e_div.innerHTML = '<i>Checking...</i>';
	e_div.className="";
	if(v_tr!=null) {
	    v_tr.className="tr_checking";
	}
    v_tr2.className="tr_checking";
//    var st_err =  document.getElementById('st_err');
    var errorHandler = function(err, ioArgs){
    	console.log('Error: ' + err + ' response ' + ioArgs.xhr.responseText);
    	removeAllChildNodes(e_div);
        e_div.className="";
//      v_td.className="v_warn";
        v_tr.className="";
        v_tr2.className="tr_err";
//        var st_err =  document.getElementById('st_err');
//        wasBusted = true;
//        st_err.className = "ferrbox";
//        st_err.innerHTML="Disconnected from Survey Tool while processing a field: "+err.name + " <br> " + err.message;
//        updateIf('progress','<hr><i>(disconnected from Survey Tool)</i></hr>');
//        updateIf('uptime','down');
//        updateIf('visitors','nobody');
    };
    var loadHandler = function(json){
        try {
             var newHtml = "";
             if(json.err && json.err.length >0) {
                 v_tr.className="tr_err";
                 v_tr2.className="tr_err";
                 newHtml = stopIcon + " Could not check value. Try reloading the page.<br>"+json.err;
                 e_div.innerHTML = newHtml;
             } else if(json.testResults && json.testResults.length == 0) {
            	 if(what == 'verify') {
	                 e_div.className="";
                     v_tr.className="tr_submit";
                     v_tr2.className="tr_submit";
	                 e_div.innerHTML = newHtml;
                     readyToSubmit(fieldhash);
            	 } else {
                     e_div.className="";
                     v_tr.className="tr_submit";
                     v_tr2.className="tr_submit";
                     newHtml = "<i>Vote Accepted:</i>";
                     e_div.innerHTML = newHtml;
            	 }
             } else {
                 var update = updateTestResults(fieldhash,json.testResults,what);
                 if (update == 'verify') {
                	 readyToSubmit(fieldhash);
                 }
             }
             if(json.submitResultRaw) {
                 e_div.innerHTML = e_div.innerHTML + "<b>Updating...</b><!-- <br><b>SUBMIT RESULTS:</b> <tt>" + json.submitResultRaw+"</tt> <b>Refreshing row...</b> -->";
                 refreshRow(fieldhash, xpid, locale, session);
             }
           }catch(e) {
               console.log("Error in ajax post [do_change]  ",e.message);
               e_div.innerHTML = "<i>Internal Error: " + e.message + "</i>";
           }
    };
    var xhrArgs = {
            url: ourUrl,
            postData: value,
            handleAs:"json",
            load: loadHandler,
            error: errorHandler
        };
    window.xhrArgs = xhrArgs;
//    console.log('xhrArgs = ' + xhrArgs);
    dojo.xhrPost(xhrArgs);
}



var timerSpeed = 15000;

function setTimerOn() {
    updateStatus();
    timerID = setInterval(updateStatus, timerSpeed);
}

function resetTimerSpeed(speed) {
	timerSpeed = speed;
	clearInterval(timerID);
	timerID = setInterval(updateStatus, timerSpeed);
}

listenFor(window,'load',setTimerOn);

function cloneAnon(i) {
	if(i==null) return null;
	var o = i.cloneNode(true);
	if(o.id) {
		o.id = null;
	}
	return o;
}

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

function cloneLocalizeAnon(i) {
	var o = cloneAnon(i);
	if(o) localizeAnon(o);
	return o;
}

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

function showLoader(loaderDiv, text) {
	updateAjaxWord(text);
//	updateIf("progress_ajax",text);
//	console.log("Load: " + text);
//	return;
//	
//	var para = loaderDiv.getElementsByTagName("p");
//	if(para) {
//		para=para[0];
//	} else {
//		para = loaderDiv;
//	}
//	para.innerHTML = text;
//	loaderDiv.style.display="";
}

function hideLoader(loaderDiv) {
	updateAjaxWord(null);
//	updateIf("progress_ajax","");
//	loaderDiv.style.display="none";
}

function wireUpButton(button, tr, theRow, vHash,box) {
	if(box) {
		button.id="CHANGE_" + tr.rowHash;
		vHash="";
		box.onchange=function(){ 
			handleWiredClick(tr,theRow,vHash,box,button,'submit'); 
			return false; 
		};
		box.onkeypress=function(e){ 
			if(e.keyCode == 13) {
				handleWiredClick(tr,theRow,vHash,box,button); 
				return false;
//			} else if(e.keyCode ==9) {
// TAB			
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
			function(e){ handleWiredClick(tr,theRow,vHash,box,button); e.stopPropagation(); return false; });
	
	if(theRow.voteVhash==vHash && !box) {
		button.className = "ichoice-x";
	} else {
		button.className = "ichoice-o";
	}
}

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
		lastTr: null
};

function showInPopOld(tr,theRow, str, hideIfLast, fn) {
	if(gPopStatus.unShow) {
		gPopStatus.unShow();
		gPopStatus.unShow=null;
	}
	if(hideIfLast && gPopStatus.lastShown==hideIfLast) {
		if(gPopStatus.lastTr) {
			gPopStatus.lastTr.infoRow.className="d-inforow-hid";
		}
		gPopStatus.lastShown=null;
		return;
	}
	gPopStatus.lastShown = hideIfLast;
	
//	tr.unShow=(function(){ var d2 = div; return function(){	console.log("hiding " + d2); 	d2.className="d-item";  };})();
//	div.className = 'd-item-selected';
	
	var td_real = tr.infoRow.getElementsByTagName("td")[0];
	var td = document.createDocumentFragment();

	if(str) {
		var div2 = document.createElement("div");
		div2.innerHTML=str;
		td.appendChild(div2);
	}
	if(fn!=null) {
		gPopStatus.unShow=fn(td);
	}
	// always append this
	if(theRow) {
		appendHelp(td,theRow);
	}

	removeAllChildNodes(td_real);
	td_real.appendChild(td);
	td=null;
	
	if(gPopStatus.lastTr && gPopStatus.lastTr != tr) {
		gPopStatus.lastTr.infoRow.className="d-inforow-hid"; // hide old row
	}
	tr.infoRow.className = "d-inforow";
	gPopStatus.lastTr = tr;
	gPopStatus.infoRow = theRow;
}

function hidePopOld(tr) {
	if(gPopStatus.unShow) {
		gPopStatus.unShow();
		gPopStatus.unShow=null;
	}
	if(gPopStatus.lastTr) {
		gPopStatus.lastTr.infoRow.className="d-inforow-hid";
	}
	gPopStatus.lastShown=null;
}
function resetPopOld(tr) {
	gPopStatus.lastShown=null;
}

// new pop stuff
dojo.ready(function() {
	var unShow = null;
	var lastShown = null;
	var pucontent = dojo.byId("pu-content");
	var puouter = dojo.byId("pu-outer");
	var pupeak = dojo.byId("pu-peak");
	var nudgeh=10;
	var nudgev=-4;
	var nudgehpost=-65;
	var nudgevpost=0;
	var hardleft = 10;
	var hardtop = 10;
	
	if(false) {
	window.showInPop = function(tr,theRow, str, hideIfLast, fn) {
		if(unShow) {
			unShow();
			unShow=null;
		}
		if(hideIfLast && lastShown==hideIfLast) {
			// HIDEPOP
			tr.lastShown=null;
			
			puouter.style.display="none";
			
			return;
		}
		lastShown = hideIfLast;

		var td = document.createDocumentFragment();

		if(str) {
			td.innerHTML=str;
		}
		if(fn!=null) {
			unShow=fn(td);
		}
		// always append this
		if(theRow) {
			appendHelp(td,theRow);
		}

		removeAllChildNodes(pucontent);
		pucontent.appendChild(td);
		td=null;
		
		if(hideIfLast) {
			var loc = getAbsolutePosition(hideIfLast);
			var newTop = (loc.top+hideIfLast.offsetHeight+nudgev);
			if(newTop<hardtop) newTop=hardtop;
			puouter.style.top = (newTop+nudgevpost)+"px";
			newLeft = (loc.left+nudgeh);
			if(newLeft<hardleft) {
				pupeak.style.left = (newLeft - hardleft)+"px";
				newLeft = hardleft;
			} else {
				pupeak.style.left = "0px";
			}
			puouter.style.left = (newLeft+nudgehpost)+"px";
			puouter.style.display="block";
		} else {
			alert("Errow showPop with no td");
		}
		//tr.infoRow.className = "d-inforow";
	};
	window.hidePop = function() {
		puouter.style.display="none";
		lastShown = null;
	};
	window.resetPop = function() {
		lastShown = null;
	};
	} else {
		window.showInPop = showInPopOld;
		window.hidePop = hidePopOld;
		window.resetPop = resetPopOld;
	}
	/* old system */
});


function testsToHtml(tests) {
	var newHtml = "";
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
function appendHelp(td,theRow) {
	if(theRow.displayHelp) {
		var helpDiv = cloneAnon(dojo.byId("proto-help"));
		helpDiv.innerHTML += theRow.displayHelp;
		td.appendChild(helpDiv);
	}
}

function showProposedItem(inTd,tr,theRow,value,tests) {
	if(!tests || tests.length==0) return false;

	showInPop(tr, theRow, null, inTd, function(td) {
		var newHtml = "";
		newHtml += testsToHtml(tests);

		var h3 = document.createElement("h3");
		var span = document.createElement("span");
		span.className="value";
		span.dir = tr.theTable.json.dir;
		span.appendChild(document.createTextNode(value));
		h3.appendChild(span);
		h3.className="span";
		td.appendChild(h3);
		var newDiv = document.createElement("div");
		td.appendChild(newDiv);

		newDiv.innerHTML = newHtml;

		return null;
	});


	if(tests) {
		var hadWarn = false;
		var hadErr = false;
		for(var i=0;i<tests.length;i++) {
			var testItem = tests[i];
			if(testItem.type == 'Warning') hadWarn = true;
			if(testItem.type == 'Error') hadErr = true;
		}
		if(hadErr) {
			tr.inputTd.className="d-change-err";
			return true;
		} else if(hadWarn) {
			tr.inputTd.className="d-change-warn";
			return false;
		}
		return false;
	}
}

function showItemInfo(td, tr, theRow, item, vHash, newButton, div) {
	showInPop(tr, theRow, "", div, function(td) {
		div.className = 'd-item-selected';

		var h3 = document.createElement("h3");
		h3.appendChild(cloneAnon(div.getElementsByTagName("span")[0]));
		h3.className="span";
		td.appendChild(h3);
		
		
		
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
		} else if(item.example) {
			appendExample(td, item.example);
		}
		
		return function(){ var d2 = div; return function(){ 	d2.className="d-item";  };}();
	}); // end fn
}



function popInfoInto(tr, theRow, theChild) {
//	var what = WHAT_GETROW;
	var ourUrl = contextPath + "/RefreshRow.jsp?what=" + WHAT_GETROW
			+ "&xpath=" + theRow.xpid + "&_=" + surveyCurrentLocale + "&fhash="
			+ theRow.rowHash + "&vhash=" + "&s=" + tr.theTable.session
			+ "&voteinfo=t";
	var errorHandler = function(err, ioArgs) {
		console.log('Error in refreshRow: ' + err + ' response '
				+ ioArgs.xhr.responseText);
		showInPop(
				tr,
				theRow,
				stopIcon
						+ " Couldn't reload this row- please refresh the page. <br>Error: "
						+ err + "</td>");
		return true;
	};
	var loadHandler = function(text) {
		try {
			if (text) {
				showInPop(tr, theRow, text, theChild);
			} else {
				showInPop(tr, theRow, stopIcon + stui.noVotingInfo, theChild);
			}
		} catch (e) {
			console.log("Error in ajax get ", e.message);
			console.log(" response: " + text);
			showInPop(tr, theRow, stopIcon + " exception: " + e.message, theChild);
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
	dojo.xhrGet(xhrArgs);
}


function appendExample(parent, text) {
	var div = document.createElement("div");
	div.className="d-example";
	div.innerHTML=text;
	parent.appendChild(div);
	return div;
}

function addVitem(td, tr,theRow,item,vHash,newButton) {
//	var canModify = tr.theTable.json.canModify;
	var div = document.createElement("div");
	div.className = "d-item";
	
	if(item==null)  {
		div.innerHTML = "<i>null: "+theRow.winningVhash+" </i>";
		return;
	}
	var text = document.createTextNode(item.value);
	var span = document.createElement("span");
	span.className = item.pClass;
	span.dir = tr.theTable.json.dir;
	span.appendChild(text);
	if(newButton) {
		newButton.value=item.value;
		wireUpButton(newButton,tr,theRow,vHash);
		div.appendChild(newButton);
	}
	div.appendChild(span);
	if(item.isOldValue==true) {
		addIcon(div,"i-star");
	}
	if(item.votes) {
		addIcon(div,"i-vote");
	}
	div.onclick = function(){ showItemInfo(td,tr,theRow,item,vHash,newButton,div); return false;};
	td.appendChild(div);
	
	if(item.inExample) {
		addIcon(div,"i-example-zoom").onclick = div.onclick;
	} else if(item.example) {
		appendExample(td,item.example).onclick = div.onclick;
	}
}

function updateRow(tr, theRow) {
	var canModify = tr.theTable.json.canModify;
	// TODO: old inrow
	if(!tr.infoRow) { 
		var toAddI = dojo.byId('proto-inforow');
		tr.infoRow = cloneAnon(toAddI);
		tr.infoRow.getElementsByTagName("button")[0].onclick=function(){if(tr.unShow) { tr.unShow(); tr.unShow=null;}  tr.infoRow.className="d-inforow-hid"; };
	}
	if(!theRow || !theRow.xpid) {
		tr.innerHTML="<td><i>ERROR: missing row</i></td>";
		return;
	}
	tr.xpid = theRow.xpid;
	var children = getTagChildren(tr);
	var protoButton = dojo.byId('proto-button');
	if(!canModify) {
		protoButton = null;
	}
	
	var doPopInfo = function() {
		popInfoInto(tr,theRow,children[1]);
	};
	
	for(var i=0;i<children.length;i++) {
		children[i].title = tr.theTable.theadChildren[i].title;
	}
	
	if(theRow.hasErrors) {
		children[0].className = "d-st-stop";
		children[0].title = stui.testError;
	} else if(theRow.hasWarnings) {
		children[0].className = "d-st-warn";
		children[0].title = stui.testWarn;
	} else {
		children[0].className = "d-st-okay";
		children[0].title = stui.testOkay;
	}
	
	children[1].className = "d-dr-"+theRow.confirmStatus;
	children[1].onclick = doPopInfo;
	children[1].title = stui.sub('draftStatus',[stui.str(theRow.confirmStatus)]);

	if(theRow.hasVoted) {
		children[2].className = "d-vo-true";
		children[2].title=stui.voTrue;
	} else {
		children[2].className = "d-vo-false";
		children[2].title=stui.voFalse;
	}
	children[2].onclick = doPopInfo;
	children[3].innerHTML=theRow.prettyPath;
	
	children[3].onclick = function() {
		showInPop(tr, theRow, "XPath: " + theRow.xpath, children[3]);
		return false;
	};
	
	if(!children[4].isSetup) {
		children[4].appendChild(document.createTextNode(theRow.displayName));
		if(theRow.displayExample) {
			appendExample(children[4], theRow.displayExample);
		}
		children[4].isSetup=true;
	}
	removeAllChildNodes(children[5]); // win
	if(theRow.items&&theRow.winningVhash) {
		addVitem(children[5],tr,theRow,theRow.items[theRow.winningVhash],theRow.winningVhash,cloneAnon(protoButton));
		/* tr.onclick = */ children[0].onclick = children[5].getElementsByTagName("div")[0].onclick;
	} else {
		/* tr.onclick = */ children[0].onclick = function() {showInPop(tr,theRow,"",tr); return false;};
	}
	removeAllChildNodes(children[6]); // other
	for(k in theRow.items) {
		if(k == theRow.winningVhash) {
			continue;
		}
		addVitem(children[6],tr,theRow,theRow.items[k],k,cloneAnon(protoButton));
	}

	if(!children[7].isSetup) {
		removeAllChildNodes(children[7]);
		tr.inputTd = children[7];
		
		if(!canModify) {
			children[7].style.display="none";
		} else if(theRow.confirmOnly) {
			children[7].className="d-change-confirmonly";
		} else {
			var changeButton = cloneAnon(protoButton);
			children[7].appendChild(changeButton);
			var changeBox = cloneAnon(dojo.byId("proto-inputbox"));
			wireUpButton(changeButton,tr, theRow, "[change]",changeBox);
			tr.inputBox = changeBox;
			
			changeBox.onfocus = function() {
				deferUpdates = true;
				return true;
			};
			changeBox.onblur = function() {
				deferUpdates = false;
				doDeferredUpdates();
			};
			
			children[7].appendChild(changeBox);
			children[7].isSetup=true;
			children[7].theButton = changeButton;
		}
	} else {
		if(children[7].theButton) {
			children[7].theButton.className="ichoice-o";
		}
	}
			
	
	if(canModify) {
		removeAllChildNodes(children[8]); // no opinion
		var noOpinion = cloneAnon(protoButton);
		wireUpButton(noOpinion,tr, theRow, null);
		noOpinion.value=null;
		children[8].appendChild(noOpinion);
	} else {
		children[8].style.display="none";
	}
	
	tr.className='vother';
	
	
}

function findPartition(partitions,partitionList,curPartition,i) {
	if(curPartition && 
			i>=curPartition.start &&
			i<curPartition.limit) {
		return curPartition;
	}
	for(var j in partitionList) {
		var name = partitionList[j];
		var p = partitions[name];
		if(i>=p.start &&
			i<p.limit) {
				p.name = name;
				return p;
			}
	}
	return null;
}

function insertRowsIntoTbody(theTable,tbody) {
	theTable.hitCount++;
	var theRows = theTable.json.section.rows;
	var toAdd = dojo.byId('proto-datarow');
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
			}
			curPartition = newPartition;
		}
		
		var k = rowList[i];
		var theRow = theRows[k];
		
		var tr = theTable.myTRs[k];
		if(!tr) {
			//console.log("new " + k);
			tr = cloneAnon(toAdd);
			theTable.myTRs[k]=tr; // save for later use
		}
		tr.id="r_"+k;
		tr.rowHash = k;
		tr.theTable = theTable;
		if(!theRow) {
			console.log("Missing row " + k);
		}
		updateRow(tr,theRow);
		
		tbody.appendChild(tr);

		// TODO: inforow
		tbody.appendChild(tr.infoRow);
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
function setupSortmode(theTable) {
	var theSortmode = theTable.sortMode;
	// ignore what's there
	removeAllChildNodes(theSortmode);
	var listOfLists = Object.keys(theTable.json.displaySets);
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
		theSortmode.appendChild(a);
	}
	
	var size = document.createElement("div");
	size.className="d-sort-size";
	var itemCount = Object.keys(theTable.json.section.rows).length;
	if(itemCount==0 && theTable.json.section.skippedDueToCoverage) {
		size.appendChild(document.createTextNode(
				stui.sub("itemCountAllHidden", [itemCount,theTable.json.section.skippedDueToCoverage])
				
				));
	} else if(itemCount==0) {
		size.appendChild(document.createTextNode(
				stui.sub("itemCountNone", [])
				
				));
	} else if(theTable.json.section.skippedDueToCoverage) {
		size.appendChild(document.createTextNode(
				stui.sub("itemCountHidden", [itemCount,theTable.json.section.skippedDueToCoverage])
				
				));
//		var minfo = dojo.byId("info_menu_p_covlev");
//		if(minfo) {
//			minfo.innerHTML = theTable.json.section.skippedDueToCoverage + " hidden";
//		}
	} else {
		size.appendChild(document.createTextNode(
				stui.sub("itemCount", [Object.keys(theTable.json.section.rows).length])));
	}
	theSortmode.appendChild(size);
}

function insertRows(theDiv,xpath,session,json) {
	var theTable = theDiv.theTable;

	var doInsertTable = null;
	
	if(!theTable) {
		theTable = cloneLocalizeAnon(dojo.byId('proto-datatable'));
		localizeFlyover(theTable);
		theTable.theadChildren = getTagChildren(theTable.getElementsByTagName("tr")[0]);
		if(!json.canModify) {
				theTable.theadChildren[7].style.display=theTable.theadChildren[8].style.display="none";
		}
		theTable.sortMode = cloneAnon(dojo.byId('proto-sortmode'));
		theDiv.appendChild(theTable.sortMode);
		theTable.myTRs = [];
		theDiv.theTable = theTable;
		theTable.theDiv = theDiv;
		doInsertTable=theTable;
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
	}
	hideLoader(theDiv.loader);
}

// move this into showRows to handle multiple containers.

////////
/// showRows() ..
function showRows(container,xpath,session,coverage) {
 dojo.addOnLoad(function(){
	if(!coverage) coverage="";
	var theDiv = dojo.byId(container);
	
	stui = theDiv.stui  = dojo.i18n.getLocalization("surveyTool", "stui", "en" /* TODO: lame */);
	
	stui.sub = function(x,y) { return dojo.string.substitute(stui[x], y);};
	
	stui.str = function(x) { if(stui[x]) return stui[x]; else return x; };
	
	var shower = null;
	var theTable = theDiv.theTable;
	shower = function() {
	
		if(!theTable) {
			var theTableList = theDiv.getElementsByTagName("table");
			if(theTableList) {
				theTable = theTableList[0];
				theDiv.theTable = theTable;
			}
		}

		var theLoader = theDiv.loader;
		if(!theLoader) {
			theLoader =  cloneAnon(dojo.byId("proto-loading"));
			theDiv.appendChild(theLoader);
			theDiv.loader = theLoader;
		}
		
		showLoader(theDiv.loader, theDiv.stui.loading);
		
		dojo.ready(function() {
		    var errorHandler = function(err, ioArgs){
		    	console.log('Error: ' + err + ' response ' + ioArgs.xhr.responseText);
		        showLoader(theDiv.loader,stopIcon + "<h1>Could not refresh the page - you may need to <a href='javascript:window.location.reload(true);'>refresh</a> the page if the SurveyTool has restarted..</h1> <hr>Error while fetching : "+err.name + " <br> " + err.message + "<div style='border: 1px solid red;'>" + ioArgs.xhr.responseText + "</div>");
		    };
		    var loadHandler = function(json){
		        try {
		        	showLoader(theDiv.loader,stui.loading2);
		        	if(!json) {
		        		console.log("!json");
				        showLoader(theDiv.loader,"Error while  loading: <br><div style='border: 1px solid red;'>" + "no data!" + "</div>");
		        	} else if(json.err) {
		        		console.log("json.err!");
				        showLoader(theDiv.loader,"Error while  loading: <br><div style='border: 1px solid red;'>" + json.err + "</div>");
				    } else if(!json.section) {
		        		console.log("!json.section");
				        showLoader(theDiv.loader,"Error while  loading: <br><div style='border: 1px solid red;'>" + "no section" + "</div>");
				    } else if(!json.section.rows) {
		        		console.log("!json.section.rows");
				        showLoader(theDiv.loader,"Error while  loading: <br><div style='border: 1px solid red;'>" + "no rows" + "</div>");				        
		        	} else {
		        		console.log("json.section.rows OK..");
		        		showLoader(theDiv.loader, "loading..");
		        		if(json.dataLoadTime) {
		        			updateIf("dynload", json.dataLoadTime);
		        		}
		        		doUpdate(theDiv.id, function() {
		        				showLoader(theDiv.loader,stui.loading3);
		        				insertRows(theDiv,xpath,session,json);
		        		});
		        	}
		        	
		           }catch(e) {
		               console.log("Error in ajax post [showRows]  " + e.message + " / " + e.name );
				        showLoader(theDiv.loader,"Exception while  loading: " + e.message + ", n="+e.name); // in case the 2nd line doesn't work
//				        showLoader(theDiv.loader,"Exception while  loading: "+e.name + " <br> " +  "<div style='border: 1px solid red;'>" + e.message+ "</div>");
//			               console.log("Error in ajax post [showRows]  " + e.message);
		           }
		    };
		    var xhrArgs = {
		            url: contextPath + "/RefreshRow.jsp?json=t&_="+surveyCurrentLocale+"&s="+session+"&xpath="+xpath+"&p_covlev="+coverage,
		            handleAs:"json",
		            load: loadHandler,
		            error: errorHandler
		        };
		    window.xhrArgs = xhrArgs;
		    console.log('xhrArgs = ' + xhrArgs);
		    dojo.xhrGet(xhrArgs);
		});
	};
	
	shower(); // first load
	theDiv.shower = shower;
	showers[theDiv.id]=shower;
//	console.log("Wrote shower " + theDiv.id + " as " + shower);
  });
}

function refreshRow2(tr,theRow,vHash,onSuccess) {
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
        			onSuccess();
        		} else {
        	        tr.className = "ferrbox";
        	        tr.innerHTML="No content found "+tr.rowHash+ "  while  loading";
        	        console.log("could not find " + tr.rowHash + " in " + json);
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
    };
    var xhrArgs = {
            url: ourUrl,
            //postData: value,
            handleAs:"json",
            load: loadHandler,
            error: errorHandler
        };
    window.xhrArgs = xhrArgs;
    console.log('xhrArgs = ' + xhrArgs + ", url: " + ourUrl);
    dojo.xhrGet(xhrArgs);
}

function handleWiredClick(tr,theRow,vHash,box,button,what) {
	resetPop(tr);
	var value="";
	var valToShow;
	if(box) {
		valToShow=box.value;
		value = box.value;
		if(value.length ==0 ) {
			box.focus();
			return; // nothing entered.
		}
		tr.inputTd.className="d-change";
	} else {
		valToShow=button.value;
	}
	if(!what) {
		what='submit';
	}
	if(what=='submit') {
		button.className="ichoice-x";  // TODO: ichoice-inprogress?  spinner?
		showLoader(tr.theTable.theDiv.loader,stui.voting);
	} else {
		showLoader(tr.theTable.theDiv.loader, stui.checking);
	}


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
				showLoader(tr.theTable.theDiv.loader,"Error!");
				tr.innerHTML = "<td colspan='4'>"+stopIcon + " Could not check value. Try reloading the page.<br>"+json.err+"</td>";
				// e_div.innerHTML = newHtml;
			} else {
				if(what=='verify') { // TODO: obsolete
//					if(json.testResults) {
//					if(json.testWarnings || json.testErrors) {
//					if(showProposedItem(tr.inputTd,tr,theRow,valToShow,json.testResults)) {
//					tr.className = 'vother';
//					button.className='ichoice-o';
//					hideLoader(tr.theTable.theDiv.loader);
//					return; // had error
//					}
//					} else {
//					hidePop(tr);
//					}
//					} else {
//					hidePop(tr);
//					}
//					hideLoader(tr.theTable.theDiv.loader);
//					return;
//					// END obsolete
					return;
				}
				if(json.submitResultRaw) { // if submitted..
					tr.className='tr_checking2';
					refreshRow2(tr,theRow,vHash,function(){
						tr.inputTd.className="d-change";

						// submit went through. Now show the pop.
						button.className='ichoice-o';
						hideLoader(tr.theTable.theDiv.loader);
						if(json.testResults && (json.testWarnings || json.testErrors)) {
							showProposedItem(tr.inputTd,tr,theRow,valToShow,json.testResults);
							if(json.testErrors && box) {
								setTimeout(function(){box.focus();},100); // make sure the box gets focus
								box.focus();
							}
						} else {
							if(box) {
								box.value=""; // submitted - dont show.
							}
							hidePop(tr);
						}
						//tr.className = 'vother';
					}); // end refresh-loaded-fcn
					// end: async
				} else {
					// Did not submit. Show errors, etc
					if(json.testResults && (json.testWarnings || json.testErrors)) {
						showProposedItem(tr.inputTd,tr,theRow,valToShow,json.testResults);
					} else {
						hidePop(tr);
					}
					//tr.className='vother';
					button.className='ichoice-o';
					hideLoader(tr.theTable.theDiv.loader);
				}
			}
		}catch(e) {
			tr.className='tr_err';
			// v_tr.className="tr_err";
			// v_tr2.className="tr_err";
			tr.innerHTML = stopIcon + " Could not check value. Try reloading the page.<br>"+e.message;
			console.log("Error in ajax post [handleWiredClick] ",e.message);
			//              e_div.innerHTML = "<i>Internal Error: " + e.message + "</i>";
		}
	};
	var errorHandler = function(err, ioArgs){
		console.log('Error: ' + err + ' response ' + ioArgs.xhr.responseText);
		theRow.className = "ferrbox";
		theRow.innerHTML="Error while  loading: "+err.name + " <br> " + err.message + "<div style='border: 1px solid red;'>" + ioArgs.xhr.responseText + "</div>";
	};
	var xhrArgs = {
			url: ourUrl,
			postData: value,
			handleAs:"json",
			load: loadHandler,
			error: errorHandler
	};
	window.xhrArgs = xhrArgs;
	console.log('xhrArgs = ' + xhrArgs + ", url: " + ourUrl);
	if(box) {
		dojo.xhrPost(xhrArgs);
	} else {
		dojo.xhrGet(xhrArgs); // value ignored
	}
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

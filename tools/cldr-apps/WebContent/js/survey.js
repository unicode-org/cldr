// survey.js  -Copyright (C) 2012 IBM Corporation and Others. All Rights Reserved.
// move anything that's not dynamically generated here.


function dismissChangeNotice() {
	surveyCurrentLocaleStamp = surveyNextLocaleStamp;
    var locDiv = document.getElementById('stchanged');
    if(locDiv) {
        locDiv.style.display='none';
    }
}

function readyToSubmit(fieldhash) {
    var ch_input = document.getElementById('ch_'+fieldhash);
    var submit_btn = document.getElementById('submit_'+fieldhash);
    var cancel_btn = document.getElementById('cancel_'+fieldhash);
    
//    ch_input.disabled='1';
//    submit_btn.style.display='block';
//    cancel_btn.style.display='block';
}

function hideSubmit(fieldhash) {
    var ch_input = document.getElementById('ch_'+fieldhash);
    var submit_btn = document.getElementById('submit_'+fieldhash);
    var cancel_btn = document.getElementById('cancel_'+fieldhash);

//    submit_btn.style.display='none';
    cancel_btn.style.display='none';
//    ch_input.disabled=null;
}

function isubmit(fieldhash,xpid,locale,session) {
    var ch_input = document.getElementById('ch_'+fieldhash);
    var submit_btn = document.getElementById('submit_'+fieldhash);
    var cancel_btn = document.getElementById('cancel_'+fieldhash);
    
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
    var chtd_input = document.getElementById('chtd_'+fieldhash);
    var submit_btn = document.getElementById('submit_'+fieldhash);
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

// hashtable of items already verified
var alreadyVerifyValue = {};

var showers={};

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

function updateStatus() {
    dojo.xhrGet({
        url: contextPath + "/SurveyAjax?what=status"+surveyLocaleUrl,
        handleAs:"json",
        load: function(json){
            if(json.isBusted == 1) {
                wasBusted = true;
            }
            var st_err =  document.getElementById('st_err');
            if(json.err != null && json.err.length > 0) {
               st_err.innerHTML=json.err;
               if(json.surveyRunningStamp!=surveyRunningStamp) {
            	   st_err.innerHTML = st_err.innerHTML + " <b>Note: Lost connection with Survey Tool or it restarted.</b>"
               }
               st_err.className = "ferrbox";
               wasBusted = true;
            } else {
            	if(json.surveyRunningStamp!=surveyRunningStamp) {
                    st_err.className = "ferrbox";
                    st_err.innerHTML="The SurveyTool has been restarted. Please reload this page to continue.";
                    wasBusted=true;
            	}else if(wasBusted == true && (json.isBusted == 0) && (json.isSetup == 1)
                      || (json.surveyRunningStamp!=surveyRunningStamp)) {
                    st_err.innerHTML="Note: Lost connection with Survey Tool or it restarted.";
                    if(clickContinue != null) {
                        st_err.innerHTML = st_err.innerHTML + " Please <a href='"+clickContinue+"'>click here</a> to continue.";
                    } else {
                    	st_err.innerHTML = st_err.innerHTML + " Please reload this page to continue.";
                    }
                    st_err.className = "ferrbox";
                } else {
                   st_err.className = "";
                   st_err.innerHTML="";
                }
            }
            updateIf('progress',json.progress);
            updateIf('uptime',json.uptime);
            updateIf('visitors',json.visitors);
            
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
            
            if((wasBusted == false) && (json.isSetup == 1) && (loadOnOk != null)) {
                window.location.replace(loadOnOk);
            }
        },
        error: function(err, ioArgs){
            var st_err =  document.getElementById('st_err');
            wasBusted = true;
            st_err.className = "ferrbox";
            st_err.innerHTML="Disconnected from Survey Tool: "+err.name + " <br> " + err.message;
            updateIf('progress','<hr><i>(disconnected from Survey Tool)</i></hr>');
            updateIf('uptime','down');
            updateIf('visitors','nobody');
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
        e_div.innerHTML="";
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
             var newHtml = "";
             if(text) {
                 v_tr.className='topbar';
                 v_tr.innerHTML = text;
                 e_div.innerHTML = "";
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
	             var newHtml = "";
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
	var v_td = document.getElementById('i_'+fieldhash);
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
    var st_err =  document.getElementById('st_err');
    var errorHandler = function(err, ioArgs){
    	console.log('Error: ' + err + ' response ' + ioArgs.xhr.responseText);
        e_div.innerHTML = '';
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
	var para = loaderDiv.getElementsByTagName("p");
	if(para) {
		para=para[0];
	} else {
		para = loaderDiv;
	}
	para.innerHTML = text;
	loaderDiv.style.display="";
}

function hideLoader(loaderDiv) {
	loaderDiv.style.display="none";
}

function wireUpButton(button, tr, theRow, vHash,box) {
	if(box) {
		button.id="CHANGE_" + tr.rowHash;
		vHash="";
		box.onchange=function(){ handleWiredClick(tr,theRow,vHash,box,button,'verify'); return false; };
		box.onkeypress=function(){ 
			if(event.keyCode == 13) {
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

function showInPop(tr,theRow, str) {
	tr.lastShown = null;
	if(tr.unShow) tr.unShow();
	tr.unShow=null;
//	tr.unShow=(function(){ var d2 = div; return function(){	console.log("hiding " + d2); 	d2.className="d-item";  };})();
//	div.className = 'd-item-selected';
	var td = tr.infoRow.getElementsByTagName("td")[0];
	td.innerHTML=str;
	tr.infoRow.className = "d-inforow";
}

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

function showProposedItem(inTd,tr,theRow,value,tests) {
	if(tr.unShow) {
		tr.unShow();
	}
	var newHtml = "";
	if (tests && tests.length>0) {
		newHtml += testsToHtml(tests);
	} else {
		// no tests, bail.
		tr.inputTd.className="d-change";
		if(tr.lastShown==inTd) {
			tr.infoRow.className = "d-inforow-hid";
			tr.lastShown=null;
		}
		return false;
	}
	tr.lastShown = inTd;
//	tr.unShow=(function(){ var d2 = div; return function(){ 	d2.className="d-item";  };})();
	tr.unShow=null;
//	div.className = 'd-item-selected';
	var td = tr.infoRow.getElementsByTagName("td")[0];
	td.innerHTML="";

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

	tr.infoRow.className = "d-inforow";
	
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
	}
	tr.inputTd.className="d-change";
	return false;
}

function showItemInfo(td, tr, theRow, item, vHash, newButton, div) {
	if (tr.lastShown == div) {
		if(tr.unShow) {
			tr.unShow();
		}
		tr.unShow=null;
		tr.lastShown = null;
		tr.infoRow.className = "d-inforow-hid";
		return true;
	} else {
		if(tr.unShow) {
			tr.unShow();
		}
		tr.lastShown = div;
		tr.unShow=(function(){ var d2 = div; return function(){ 	d2.className="d-item";  };})();
		div.className = 'd-item-selected';
		var td = tr.infoRow.getElementsByTagName("td")[0];

		td.innerHTML="";
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
		

		tr.infoRow.className = "d-inforow";
//		tr.infoRow.onClick = function() {
//			showItemInfo(td, tr, theRow, item, vHash, newButton, div);
//		};
		return true;
	}
}



function popInfoInto(tr, theRow, theChild) {
	if (tr.lastShown == theChild) {
		if (tr.unShow) {
			tr.unShow();
		}
		tr.unShow = null;
		tr.lastShown = null;
		tr.infoRow.className = "d-inforow-hid";
		return true;
	}
	var what = WHAT_GETROW;
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
				showInPop(tr, theRow, text);
				tr.lastShown = theChild;
			} else {
				showInPop(tr, theRow, stopIcon + " (no voting info received)");
			}
		} catch (e) {
			console.log("Error in ajax get ", e.message);
			console.log(" response: " + text);
			showInPop(tr, theRow, stopIcon + " exception: " + e.message);
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
	newButton.value=item.value;
	wireUpButton(newButton,tr,theRow,vHash);
	div.appendChild(newButton);
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
	
	var doPopInfo = function() {
		popInfoInto(tr,theRow,children[1]);
	};
	
	children[0].innerHTML = "";
	if(theRow.hasWarnings) {
		children[0].className = "d-st-warn";
	} else if(theRow.hasErrors) {
		children[0].className = "d-st-stop";
	} else {
		children[0].className = "d-st-okay";
	}
	
	children[1].innerHTML = "";
	children[1].className = "d-dr-"+theRow.confirmStatus;
	children[1].onclick = doPopInfo;

	children[2].innerHTML = "";
	if(theRow.hasVoted) {
		children[2].className = "d-vo-true";
	} else {
		children[2].className = "d-vo-false";
	}
	children[2].onclick = doPopInfo;
	children[3].innerHTML=theRow.prettyPath;
	children[4].innerHTML=theRow.displayName;
	if(theRow.displayExample) {
		appendExample(children[4], theRow.displayExample);
	}
	children[5].innerHTML=""; // win
	if(theRow.items&&theRow.winningVhash) {
		addVitem(children[5],tr,theRow,theRow.items[theRow.winningVhash],theRow.winningVhash,cloneAnon(protoButton));
		children[0].onclick = children[5].getElementsByTagName("div")[0].onclick;
	} else {
		children[0].onclick = null;
	}
	children[6].innerHTML=""; // other
	for(k in theRow.items) {
		if(k == theRow.winningVhash) {
			continue;
		}
		addVitem(children[6],tr,theRow,theRow.items[k],k,cloneAnon(protoButton));
	}

	if(!children[7].isSetup) {
		children[7].innerHTML="";
		var changeButton = cloneAnon(protoButton);
		children[7].appendChild(changeButton);
		var changeBox = cloneAnon(dojo.byId("proto-inputbox"));
		wireUpButton(changeButton,tr, theRow, "[change]",changeBox);
		tr.inputBox = changeBox;
		children[7].appendChild(changeBox);
		children[7].isSetup=true;
		children[7].theButton = changeButton;
		tr.inputTd = children[7];
	} else {
		children[7].theButton.className="ichoice-o";
	}
			
	children[8].innerHTML=""; // no opinion
	var noOpinion = cloneAnon(protoButton);
	wireUpButton(noOpinion,tr, theRow, null);
	noOpinion.value=null;
	children[8].appendChild(noOpinion);
	
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
	tbody.innerHTML="";
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
	theSortmode.innerHTML="";
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
		a.appendChild(document.createTextNode(k));
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
	size.appendChild(document.createTextNode("Items: " + Object.keys(theTable.json.section.rows).length));
	if(theTable.json.section.skippedDueToCoverage) {
		size.appendChild(document.createTextNode("(Hidden due to coverage: " + theTable.json.section.skippedDueToCoverage +")"));
//		var minfo = dojo.byId("info_menu_p_covlev");
//		if(minfo) {
//			minfo.innerHTML = theTable.json.section.skippedDueToCoverage + " hidden";
//		}
	}
	theSortmode.appendChild(size);
}

function insertRows(theDiv,xpath,session,json) {
	var theTable = theDiv.theTable;

	if(!theTable) {
		theTable = cloneAnon(dojo.byId('proto-datatable'));
		theTable.sortMode = cloneAnon(dojo.byId('proto-sortmode'));
		theDiv.appendChild(theTable.sortMode);
		theTable.myTRs = [];
		theDiv.theTable = theTable;
		theTable.theDiv = theDiv;
  		theDiv.appendChild(theTable);
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
	hideLoader(theDiv.loader);
}

// move this into showRows to handle multiple containers.

////////
/// showRows() ..
function showRows(container,xpath,session,coverage) {
	if(!coverage) coverage="";
	var theDiv = dojo.byId(container);
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
		
		showLoader(theDiv.loader, "Loading...");
		
		dojo.ready(function() {
		    var errorHandler = function(err, ioArgs){
		    	console.log('Error: ' + err + ' response ' + ioArgs.xhr.responseText);
		        showLoader(theDiv.loader,stopIcon + "<h1>Could not refresh the page - you may need to <a href='javascript:window.location.reload(true);'>refresh</a> the page if the SurveyTool has restarted..</h1> <hr>Error while fetching : "+err.name + " <br> " + err.message + "<div style='border: 1px solid red;'>" + ioArgs.xhr.responseText + "</div>");
		    };
		    var loadHandler = function(json){
		        try {
		        	showLoader(theDiv.loader,"Analyzing response..");
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
		        		showLoader(theDiv.loader, "Loaded " + Object.keys(json.section.rows).length + " rows");
		        		insertRows(theDiv,xpath,session,json);
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
		    //console.log('xhrArgs = ' + xhrArgs);
		    dojo.xhrGet(xhrArgs);
		});
	};
	
	shower(); // first load
	theDiv.shower = shower;
	showers[theDiv.id]=shower;
//	console.log("Wrote shower " + theDiv.id + " as " + shower);
}

function refreshRow2(tr,theRow,vHash,onSuccess) {
	showLoader(tr.theTable.theDiv.loader,"Loading 1 row");
    var ourUrl = contextPath + "/RefreshRow.jsp?what="+WHAT_GETROW+"&xpath="+theRow.xpid +"&_="+surveyCurrentLocale+"&fhash="+tr.rowHash+"&vhash="+vHash+"&s="+tr.theTable.session +"&json=t";
    var loadHandler = function(json){
        try {
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
	if(!what) {
		what='submit';
	}
	if(what=='submit') {
		button.className="ichoice-x";  // TODO: ichoice-inprogress?  spinner?
		showLoader(tr.theTable.theDiv.loader,"Voting");
	} else {
		showLoader(tr.theTable.theDiv.loader, "Checking");
	}
	
	
	var value="";
	if(box) {
		value = box.value;
		tr.inputTd.className="d-change";
	}
	console.log("Vote for " + tr.rowHash + " v='"+vHash+"', value='"+value+"'");
	var ourUrl = contextPath + "/SurveyAjax?what="+what+"&xpath="+tr.xpid +"&_="+surveyCurrentLocale+"&fhash="+tr.rowHash+"&vhash="+vHash+"&s="+tr.theTable.session;
	tr.className='tr_checking';
    var loadHandler = function(json){
        try {
            // var newHtml = "";
             if(json.err && json.err.length >0) {
            	 tr.className='tr_err';
                // v_tr.className="tr_err";
                // v_tr2.className="tr_err";
                tr.innerHTML = stopIcon + " Could not check value. Try reloading the page.<br>"+json.err;
                // e_div.innerHTML = newHtml;
             } else {
            	 if(json.testResults) {
            		 var valToShow = "";
            		 if(box) {
            			 valToShow=box.value;
            		 } else {
            			 valToShow=button.value;
            		 }
            		 if(showProposedItem(tr.inputTd,tr,theRow,valToShow,json.testResults)) {
            			 tr.className = 'vother';
            			 button.className='ichoice-o';
            			 hideLoader(tr.theTable.theDiv.loader);
            			 return; // had error
            		 }
            	 } else if(tr.lastShown && tr.lastShown==tr.inputTd) {
            		 // if we were watching warnings and there aren't any,hide them.
            			tr.lastShown = null;
            			tr.infoRow.className = "d-inforow-hid";
            	 }
	             if(json.submitResultRaw) {
	            	 tr.className='tr_checking2';
	                 refreshRow2(tr,theRow,vHash,function(){
	                	 if(box) {
	                		 box.value="";
	                	 }
	                	 tr.inputTd.className="d-change";
	                 });
	                 // end: async
	             } else {
	            	 tr.className='vother';
	            	 button.className='ichoice-o-okay';
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
    for (i=0; i < document.checkboxes.elements.length; i++){
        var item = document.checkboxes.elements[i];
        if (!item.checked) {
            hideRegexString += "|";
            hideRegexString += item.name;
        }
    }
    var hideRegex = new RegExp(hideRegexString);
    changeStyle(hideRegex);
}

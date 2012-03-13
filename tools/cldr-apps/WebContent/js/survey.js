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

var wasBusted = false;
var wasOk = false;
var loadOnOk = null;
var clickContinue = null;
 var surveyCurrentLocaleStamp = 0;
 var surveyNextLocaleStamp = 0;

// hashtable of items already verified
var alreadyVerifyValue = {};

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
                        //console.log("STATUS>: " + json.localeStampName + "="+json.localeStamp);
                        updateIf('stchanged_loc',json.localeStampName);
                        var locDiv = document.getElementById('stchanged');
                        if(locDiv) {
                            locDiv.style.display='block';
                        }
                        surveyNextLocaleStamp = json.localeStamp;
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
	console.log("do_change('" + fieldhash +"','"+value+"','"+vhash+"','"+xpid+"','"+locale+"','"+session+"')");
//	console.log(" what = " + what);
	console.log(" url = " + ourUrl);
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
               console.log("Error in ajax post ",e.message);
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
    console.log('xhrArgs = ' + xhrArgs);
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

window.addEventListener('load',setTimerOn,false);

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


function wireUpButton(button, tr, theRow, vHash) {
	if(vHash==null) {
		button.id="NO_" + tr.rowHash;
		vHash="";
	} else {
		button.id = "v"+vHash+"_"+tr.rowHash;
	}
	button.onclick=function(){ handleWiredClick(tr,theRow,vHash); }
	
	if(theRow.voteVhash==vHash) {
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
	span.appendChild(text);
	
	wireUpButton(newButton,tr,theRow,vHash);
	div.appendChild(newButton);
	div.appendChild(span);
	if(item.isOldValue==true) {
		addIcon(div,"i-star");
	}
	td.appendChild(div);
}

function updateRow(tr, theRow) {
	
	tr.xpid = theRow.xpid;
	var children = getTagChildren(tr);
	var protoButton = dojo.byId('proto-button');
	
	children[0].innerHTML = "";
	if(theRow.hasWarnings) {
		children[0].className = "d-st-warn";
	} else {
		children[0].className = "d-st-okay";
	}
	
	children[1].innerHTML = "";
	children[1].className = "d-dr-"+theRow.confirmStatus;
	children[2].innerHTML = "";
	if(theRow.hasVoted) {
		children[2].className = "d-vo-true";
	} else {
		children[2].className = "d-vo-false";
	}
	children[3].innerText=theRow.prettyPath;
	children[4].innerText=theRow.displayName;
	children[5].innerHTML=""; // win
	if(theRow.items&&theRow.winningVhash) {
		addVitem(children[5],tr,theRow,theRow.items[theRow.winningVhash],theRow.winningVhash,cloneAnon(protoButton));
	}
	children[6].innerHTML=""; // other
	for(k in theRow.items) {
		if(k == theRow.winningVhash) {
			continue;
		}
		addVitem(children[6],tr,theRow,theRow.items[k],k,cloneAnon(protoButton));
	}
	
	children[7].innerHTML="";
	var changeButton = cloneAnon(protoButton);
	wireUpButton(changeButton,tr, theRow, "[change]");
	children[7].appendChild(changeButton);
	children[7].appendChild(document.createElement("input"));
			
	children[8].innerHTML=""; // no opinion
	var noOpinion = cloneAnon(protoButton);
	wireUpButton(noOpinion,tr, theRow, null);
	children[8].appendChild(noOpinion);
}

function insertRows(theDiv,xpath,session,theRows) {
	theDiv.innerHTML=""; // empty it
	var theTable = cloneAnon(dojo.byId('proto-datatable'));
	theDiv.appendChild(theTable);
	var toAdd = dojo.byId('proto-datarow');
	// append header row
	
	theTable.session = session;
	theTable.xpath = xpath;
	theTable.theRows = theRows;
	
	for(k in theRows) {
		var theRow = theRows[k];
		
		
		var tr = cloneAnon(toAdd);
		tr.id="r_"+k;
		tr.rowHash = k;
		tr.theTable = theTable;
		updateRow(tr,theRow);
		
		
		
//		var tr = document.createElement("tr");
//		
//		var st = document.createElement("td");
//		st.appendChild(document.createTextNode(k));
//		tr.appendChild(st);
//		var dr = document.createElement("td");
//		tr.appendChild(dr);
//		var vo = document.createElement("td");
//		tr.appendChild(vo);
//		var code = document.createElement("td");
//		tr.appendChild(code);
//		var displayName = document.createElement("td");
//		var display = document.createTextNode(theRow.displayName);
//		displayName.appendChild(display);
//		tr.appendChild(displayName);
//		var win = document.createElement("td");
//		tr.appendChild(win);
//		var prop = document.createElement("td");
//		tr.appendChild(prop);
//		var no = document.createElement("td");
//		tr.appendChild(no);
//		
		theTable.appendChild(tr);
	}
}

// move this into showRows to handle multiple containers.
var theRows = null;


////////
/// showRows() ..
function showRows(container, xpath, session) {
	var theDiv = dojo.byId(container);

	theDiv.innerHTML="<i>loading..</i>";
	
	dojo.ready(function() {
	    var errorHandler = function(err, ioArgs){
	    	console.log('Error: ' + err + ' response ' + ioArgs.xhr.responseText);
	        theDiv.className = "ferrbox";
	        theDiv.innerHTML="Error while  loading: "+err.name + " <br> " + err.message + "<div style='border: 1px solid red;'>" + ioArgs.xhr.responseText + "</div>";
	    };
	    var loadHandler = function(json){
	        try {
	        	
	        	if(!json.section) {
	        		theDiv.innerHTML="<i>Err, no section</i>";
	        	} else {
	        		theRows = json.section;
	        		theDiv.innerHTML="<i>Loaded " + Object.keys(theRows).length + " rows</i>";
	        		
	        		insertRows(theDiv,xpath,session,theRows);
	        	}
	        	
	           }catch(e) {
	               console.log("Error in ajax post ",e.message);
  	   	           theDiv.className = "ferrbox";
	               theDiv.innerHTML = "<i>Internal Error: " + e.message + "</i>";
	           }
	    };
	    var xhrArgs = {
	            url: contextPath + "/RefreshRow.jsp?json=t&_="+surveyCurrentLocale+"&s="+session+"&xpath="+xpath,
	            handleAs:"json",
	            load: loadHandler,
	            error: errorHandler
	        };
	    window.xhrArgs = xhrArgs;
	    console.log('xhrArgs = ' + xhrArgs);
	    dojo.xhrGet(xhrArgs);
	});
}

function handleWiredClick(tr,theRow,vHash) {
	var ourUrl = contextPath + "/SurveyAjax?what="+"submit"+"&xpath="+tr.xpid +"&_="+surveyCurrentLocale+"&fhash="+tr.rowHash+"&vhash="+vHash+"&s="+tr.theTable.session;
	
    var loadHandler = function(json){
        try {
            // var newHtml = "";
             if(json.err && json.err.length >0) {
                // v_tr.className="tr_err";
                // v_tr2.className="tr_err";
                // newHtml = stopIcon + " Could not check value. Try reloading the page.<br>"+json.err;
                // e_div.innerHTML = newHtml;
             } else 
             if(json.submitResultRaw) {
                 refreshRow2(tr,theRow,vHash);
             }
           }catch(e) {
               console.log("Error in ajax post ",e.message);
 //              e_div.innerHTML = "<i>Internal Error: " + e.message + "</i>";
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
    console.log('xhrArgs = ' + xhrArgs);
    dojo.xhrPost(xhrArgs);
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


var running = true;


//// a few util functions
var surveyRunningStamp = 0;
var surveyUser = {};
var contextPath="../smoketest";
var surveyLocaleUrl=contextPath;
var disconnected = true;
var BASELINE_LANGUAGE_NAME='English';
//function createChunk(text, tag, className) {
//	if(!tag) {
//		tag="span";
//	}
//	var chunk = document.createElement(tag);
//	if(className) {
//		chunk.className = className;
//		//chunk.title=stui_str(className+"_desc");
//	}
//	if(text) {
//		chunk.appendChild(document.createTextNode(text));
//	}
//	return chunk;
//}
//function removeAllChildNodes(td) {
//	if(td==null) return;
//	while(td.firstChild) {
//		td.removeChild(td.firstChild);
//	}
//}


/// end util functions from survey.js


// TODO:  refactor, merge into survey.js
function appendAdminPanel(survey,config,adminStuff) {
	var vap = config.vap;
	var contextPath = config.url;
	
	if(!vap) return;
	loadStui();
	if(!adminStuff) return;
	
	var content = document.createDocumentFragment();
	
	var list = document.createElement("ul");
	list.className="adminList";
	content.appendChild(list);
	
	
	removeAllChildNodes(adminStuff);
	var urlcache={};
	function loadOrFail(urlAppend,theDiv, loadHandler, postData) {
		var cachedData = urlcache[urlAppend];
		if(cachedData) {
			console.log("Cached:"+urlAppend);
			loadHandler(cachedData);
			return;
		}
		var ourUrl = contextPath + "/AdminAjax.jsp?vap="+vap+"&"+urlAppend;
		var errorHandler = function(err, ioArgs){
			console.log('adminload ' + urlAppend + ' Error: ' + err + ' response ' + ioArgs.xhr.responseText);
			theDiv.className = "ferrbox";
			theDiv.innerHTML="Error while  loading: "+err.name + " <br> " + err.message + "<div style='border: 1px solid red;'>" + ioArgs.xhr.responseText + "</div>";
		};
		var xhrArgs = {
				url: ourUrl+cacheKill(),
				handleAs:"json",
				load: function(json) {
					urlcache[urlAppend]=json;
					console.log("Loaded:"+urlAppend);
					loadHandler(json);
				},
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
			//window.location.hash="#!"+name;
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
		panelSwitch(type);
	}
	
	

/*	function createUser(user) {
		var div = createChunk(null,"div","adminUserUser");
		div.appendChild(createChunk(stui.str("userlevel_"+user.userlevelname),"i","userlevel_"+user.userlevelname));
		div.appendChild(createChunk(user.name,"span","adminUserName"));
		div.appendChild(createChunk(user.email,"address","adminUserAddress"));
		return div;
	}
*/	
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
				frag2.appendChild(document.createTextNode(stui.str("No users.")))
			} else {
				for(sess in json.users) {
					var cs = json.users[sess];
					var user = createChunk(null,"div","adminUser");
					user.appendChild(createChunk("Session: " + sess, "span","adminUserSession"));
					if(cs.user) {
					    if(cs.user.userlevelName === undefined) {
						cs.user.userlevelName = cs.user.userlevelname;
					    }
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
						if(exceptions.length>0 /*&& (exceptions.length % 8 == 0)*/) {
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

	
	// last panel loaded.
	// If it's in the hashtag, use it, otherwise first.
//	if(window.location.hash && window.location.hash.indexOf("#!")==0) {
//		panelSwitch(window.location.hash.substring(2));
//	}
	if(panelFirst) { // not able to load anything.
		panelSwitch(panelFirst.type);
	}
	adminStuff.appendChild(content);
}

function appendStatusItem(desc, div,initial) {
	var runningText = createChunk(desc+":","p","aStatusItem");
	var running = createChunk(initial,"b");
	runningText.appendChild(running);
	div.appendChild(runningText);
	return running;
}

function appendCheckbox(div,desc,fn) {
	var details = document.createElement("input");
	details.type="checkbox";
	var label = document.createElement("label");
	label.appendChild(details);
	label.appendChild(document.createTextNode(desc));

	details.onclick = function() {
		details.checked = fn(details.checked);
		return true;
	};
	div.appendChild(label);
	details.label = label;
	return details;
}
function appendButton(div, desc, fn) {
	var details = createChunk(desc,"input");
	details.value=desc;
	details.type="button";
	details.onclick = fn;
	div.appendChild(details);
	return details;
}
function replaceText(o, text,className) {
	removeAllChildNodes(o);
	if(text!=undefined) {
		o.appendChild(document.createTextNode(text));
	}
	if(className) {
		o.className=className;
	} else {
		o.className="";
	}
}

var refreshRate = surveyconfig.refreshRate;

function doUpdates(survey, config, div) {
	
	// set up items
	var checkstatus = appendStatusItem("Status",div,"Loading");
	var ping = appendStatusItem("Ping Time",div,"?");
	var running = appendStatusItem("Running",div);
	var guests = appendStatusItem("Guests",div);
	var specialHeader = appendStatusItem("Special Header",div);
	var mem = appendStatusItem("Memory",div);
	var uptime = appendStatusItem("Uptime",div);
	var load = appendStatusItem("Load",div);
	var pages = appendStatusItem("Pages",div);
	var newVersion = appendStatusItem("Version",div);
	var currev = appendStatusItem("SVN Version",div);
	var phase = appendStatusItem("Phase",div);
	var environment = appendStatusItem("Environment",div);

	var div3 = document.createElement("div");
	div3.className="aAdmin";
	div3.style.display='none';

	var details = appendCheckbox(div, "Details",function(checked) {
		if(checked) {
			div3.style.display=null;
		} else {
			div3.style.display='none';
		}
		return checked;
	});
	var pauseDetails = appendCheckbox(div, "Pause Details",function(checked) {
		if(checked) {
			//div3.style.display=null;
		} else {
			// reload?
		}
		return checked;
	});
	pauseDetails.checked=true;
	var refreshDetails = appendButton(div, "Refresh Details", function() {
		appendAdminPanel(survey,config,div3);
		return false;
	})
	
	div.appendChild(div3);	
	
	var xhrArgs = null;
	var refreshTask = null;
	
	function errorHandler(err) {
		replaceText(checkstatus,"[Error: XHRGet failed: " + err + "]","fallback_code");
		refreshTask = setTimeout(function() {
		    xhrArgs.loadStart = (new Date).getTime();
			dojo.xhrGet(xhrArgs);
		}, refreshRate*1000);
	}
	function loadHandler(json) {
		var pingTime = (new Date).getTime()-xhrArgs.loadStart;
		var pingClass="";
		if(pingTime<250) {
			pingClass="winner";
		} else if(pingTime>1500) {
			pingClass="fallback_code";
		} 
		replaceText(ping,""+(pingTime)+"ms",pingClass);
		replaceText(checkstatus,"Loaded","winner");
		try {
			if(!json) {
				replaceText(running,"!json - unknown status.","fallback_code");
			} else {
				if(json.SurveyOK==0) {
					if(json.err=="The Survey Tool is not running.") {
						json.err = json.err + " (It may not have been accessed yet.)";
					}
					replaceText(running, "No: " + json.err,"fallback_code");
				} else {
					replaceText(running, "Yes","winner");
					if(!pauseDetails.checked) {
						appendAdminPanel(survey,config,div3);
					}
					
					replaceText(guests,json.status.guests + " guests, " + json.status.users + " users");
					replaceText(specialHeader, json.status.specialHeader);
					replaceText(uptime, json.status.uptime);
					replaceText(currev, json.status.currev);
					replaceText(pages, json.status.pages);
					replaceText(newVersion, json.status.newVersion);
					replaceText(phase, json.status.phase);
					replaceText(load, json.status.sysload + " ("+json.status.sysprocs + " processors)",
							(json.status.sysload>=1?"fallback_code":""));
					replaceText(environment, json.status.environment + " " + (json.status.isUnofficial?"(Unofficial)":"(Official)"));
					
					replaceText(mem,Math.round(json.status.memfree) + "M free/"+Math.round(json.status.memtotal)+"M total");
					
					// {"progress":"(obsolete-progress)","SurveyOK":"1",
					//"status":{"sysload":5.93359375,"users":0,"sysprocs":2,"surveyRunningStamp":1338829990719,"isSetup":true,"pages":1,
					//"guests":1,"uptime":"uptime: 32:36","memtotal":575.14,"dbopen":0,"environment":"LOCAL","isUnofficial":true,
					//"memfree":206.7321640625,"dbused":196,"lockOut":false,"specialHeader":"Welcome to Steven's special DEVELOPMENT SurveyTool."
					//},"isBusted":"0","isSetup":"0","visitors":"","err":"","uptime":""}
					
				}
			}
			
			
			// reload
			replaceText(checkstatus, "Loaded - refresh every " + refreshRate + "s","winner");
			refreshTask = setTimeout(function() {
			    xhrArgs.loadStart = (new Date).getTime();
				dojo.xhrGet(xhrArgs);
			}, refreshRate*1000);
			
			// {"progress":"(obsolete-progress)","SurveyOK":"0","isBusted":"0","isSetup":"0","visitors":"","err":"The Survey Tool is not running.","uptime":""}
		} catch(e) {
			div.appendChild(createChunk("[Exception: " + e.toString() + "]","p"));
		}
	}
	
	
    xhrArgs = {
            url: config.url + "/SurveyAjax?what=status",
            handleAs:"json",
            load: loadHandler,
            error: errorHandler
        };
    
    xhrArgs.loadStart = (new Date).getTime();
    dojo.xhrGet(xhrArgs);
}
function createLink(url, text) {
	var link = document.createElement("a");
	link.href = url;
	link.appendChild(document.createTextNode(text));
	return link;
}

function appendConsole(surveyConsoles, survey) {
	console.log("Loading surveyConsole " + survey);
	var div = document.createElement("div");
	div.id = "survey_"+survey;
	div.className="aConsole";
	var config = surveyconfig.instances[survey];

	var name  = document.createElement("div");
	name.className="title";
	name.appendChild(createChunk( survey,"span","instanceTitle"));
	name.appendChild(createLink(config.url,config.url));
	name.appendChild(createLink(config.url+"/AdminPanel.jsp?vap="+config.vap,"[AdminPanel]"));
	name.appendChild(createLink(config.url+"/survey?email=admin@&pw="+config.vap,"[admin@ login]"));
	
	
	div.appendChild(name);
	
	var div2 = document.createElement("div");
	div2.className="aInstance";
	div.appendChild(div2);	


	surveyConsoles.appendChild(div);
	
	doUpdates(survey, config,div2);
}


function surveyUpdater() {
    console.log('surveyUpdater loading');
    require(["dojo/query", "dojo/request", "dojo/dom", "dojo/dom-construct", "dojox/form/BusyButton", 
	     "dojo/window", "dojox/widget/Standby", "dijit/form/Select",
	     "dojo/domReady!"],
	    function surveyUpdater2(query,request,dom, dcons, BusyButton, win, Standby, Select) {
	function setText(node, text) {
	    var node2 = query(node);
	    if(node2==null) return node;
	    node2 = node2[0]; // it's a list
	    
	    var textNode = document.createTextNode(text);
	    while(node2.firstChild != null) {
		node2.removeChild(node2.firstChild);
	    }
	    node2.appendChild(textNode);
	}
	
	var updateBox = dom.byId('updateBox');
	var updateStatus = dom.byId('updateStatus');
	var updateFrame = dom.byId('updateFrame');
	setText('#updateStatus', 'Loading..');
	var standby = new Standby({target:dom.byId('basic')});
	document.body.appendChild(standby.domNode);
	standby.startup();
	var updWatch = -1;
	var doUpdate = null;
	function doStop() { 
	    standby.hide();
	    if(updWatch != -1) {
		clearInterval(updWatch);
		updWatch = -1;
	    }
	    if(doUpdate) {
		doUpdate.cancel();
	    }
	    updateFrame.className = '';
	}

	function doRequest(input, fn) {
	    request
		.post(surveyconfig.updater.jsonURL, {data: JSON.stringify(input),
						     handleAs: 'json'})
		.then(function(json) {
		    window._JSON = json; // DEBUGGING
		    if(json.err) {
			setText("#updateStatus", 'err='+json.err);
			doStop();
		    } else {
			fn(json);
		    }
			})
		.otherwise(function(err) {
		    setText('#updateStatus', 'error: ' + err);
		    doStop();
		});
	}
	doRequest(
	    {what: 'status'},
	    function(json) {
		if(json.ready) {
		    setText("#updateStatus", "Ready to update - choose an action and click Perform Action.");
		    var opts = [ { label: "reload this page", value: "nochoice", selected: true } ];
		    for(var k in json.options) {
			opts.push({label: json.options[k], value: k});
		    }
		    var choiceMenu = new Select({ name: "choiceMenu", options: opts });
		    choiceMenu.placeAt(dojo.byId("updateBox"));
		    doUpdate = new BusyButton({
			id: 'updateButton',
			label: 'Perform Action',
			busyLabel: 'Processing...'
		    });
		    doUpdate.on("click", function(e) {
			updateFrame.className='active';
			doUpdate.makeBusy();
			if(choiceMenu.get("value")=="nochoice") {
			    console.log("Nothing to do");
			    doUpdate.cancel();
			    doStop();
			    

			    document.location.reload(false); // argh
			    return true;
			}
			standby.show();
			setText('#updateTxt', "Running command...");
			doRequest({what: 'update', type: choiceMenu.get("value")},
			 function(json2) {
			     setText('#updateStatus', json2.result);

			     updWatch = setInterval(function() {
				 doRequest({what: 'readlog', pid: json2.pid},
				   function(json3) {
				       if(json3.running) {
					   setText('#updateStatus', 'pid ' + json2.pid + ' running' );
				       } else {
					   setText('#updateStatus', 'pid ' + json2.pid + ' Done.' );
					   doStop();
				       }

				       setText('#updateTxt', json3.txt);
				       win.scrollIntoView("updateStatus");
				   });
			     }, 5000);


			 });
		    });
		    doUpdate.placeAt(updateBox);
		    
		} else {
		    setText("#updateStatus", "Not ready to update.");
		}
	    });
    });
}

function surveyConsoles() {
	if(!window.surveyconfig) {
		alert('Error- surveyconfig not defined!');
	} else if(!window.dojo) {
		alert('Error- dojo not defined!');
	} else {
		document.write('<div id="surveyConsoles"></div>');
		dojo.ready(function() {
			var surveyConsoles = dojo.byId("surveyConsoles");
			for(var survey in surveyconfig.instances) {
				appendConsole(surveyConsoles,survey);
			}
		});

	    surveyUpdater();
	}
}


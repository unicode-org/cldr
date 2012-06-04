
var running = true;


//// a few util functions
function createChunk(text, tag, className) {
	if(!tag) {
		tag="span";
	}
	var chunk = document.createElement(tag);
	if(className) {
		chunk.className = className;
		//chunk.title=stui_str(className+"_desc");
	}
	if(text) {
		chunk.appendChild(document.createTextNode(text));
	}
	return chunk;
}
function removeAllChildNodes(td) {
	if(td==null) return;
	while(td.firstChild) {
		td.removeChild(td.firstChild);
	}
}


/// end util functions from survey.js

function appendStatusItem(desc, div,initial) {
	var runningText = createChunk(desc+":","p","aStatusItem");
	var running = createChunk(initial,"b");
	runningText.appendChild(running);
	div.appendChild(runningText);
	return running;
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
	var environment = appendStatusItem("Environment",div);
	
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
		replaceText(ping,""+((new Date).getTime()-xhrArgs.loadStart)+"ms","winner");
		replaceText(checkstatus,"Loaded","winner");
		try {
			if(!json) {
				replaceText(running,"!json - unknown status.","fallback_code");
			} else {
				if(json.SurveyOK==0) {
					replaceText(running, "No: " + json.err,"fallback_code");
				} else {
					replaceText(running, "Yes","winner");
					
					replaceText(guests,json.status.guests + " guests, " + json.status.users + " users");
					replaceText(specialHeader, json.status.specialHeader);
					replaceText(uptime, json.status.uptime);
					replaceText(load, json.status.sysload + " ("+json.status.sysprocs + " processors)");
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

function appendConsole(surveyConsoles, survey) {
	console.log("Loading surveyConsole " + survey);
	var div = document.createElement("div");
	div.id = "survey_"+survey;
	div.className="aConsole";
	var config = surveyconfig.instances[survey];

	var name  = document.createElement("h3");
	var link = document.createElement("a");
	link.href = config.url;
	link.appendChild(document.createTextNode(survey));
	name.appendChild(link);
	name.appendChild(document.createTextNode( " " + config.url));
	div.appendChild(name);
	
	var div2 = document.createElement("div");
	div2.className="aInstance";
	
	
	
	div.appendChild(div2);
	
	surveyConsoles.appendChild(div);
	
	doUpdates(survey,config,div2);
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
	}
}
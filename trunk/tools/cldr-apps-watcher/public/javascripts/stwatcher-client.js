
function stwatcher_interval(_config) {
    var theInterval = -1;
    var theInterval2 = -1;
    var refreshTime = 0;

    var stw_config;

    if(_config) {
	stw_config = _config;
    } else if(window.default_stw_config) {
	stw_config = default_stw_config;
    } else {
	stw_config  = {
	    reloadMode: false,
	    jsonurl: '/latest.json',
	    polltime: 30
	};
    }

    var n = stw_config.polltime;
    refreshTime = n*1000;
    console.log("Interval = " + n + " sec");
    // interval for next time

require(["dojo/query", "dojo/request", "dojo/dom", "dojo/dom-construct", "dojo/main", "dojox/charting/Chart", "dojox/charting/axis2d/Default", "dojox/charting/plot2d/Lines", "dojox/charting/action2d/MouseIndicator", "dojox/charting/action2d/MouseZoomAndPan", "dojox/charting/action2d/TouchZoomAndPan", "dojox/charting/plot2d/Indicator", "dojo/domReady!"],
 function stwatcherclient(query,request,dom, dcons, main, Chart, Default, Lines, MouseIndicator, MouseZoomAndPan, TouchZoomAndPan, Indicator) {
     
     
    var powered = dom.byId('powered');
    powered.appendChild(document.createTextNode(" and dōjō " + main.version));

    function fmtNs(val) {
        if(val > 1e9) {
            val = val / 1e9;
            return val.toFixed(3) + "s";
        } else if(val > 1e6) {
            val = val / 1e6;
            return val.toFixed(3) + "ms";
        } else if(val > 1e3) {
            val = val / 1e3;
            return val.toFixed(3) + "µs";
        } else {
            return val + "ns";
        }
    }

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

    setText("#status", "Loading..");
    
    var div = query('#SurveyWatcher');
            
    div.empty();
    var chart1=null;
    
    
    function show_history(server, obj, hostInfo) {
        setText('#chartTitle', "History: " + server + "@" + obj.host);
        var hdiv = query("#SurveyChart");
        if(chart1===null) {
            hdiv.empty();
        }
        setText('#chartstatus', 'Please Wait: Fetching chart info');
        request
            .get('history.json?server='+server, {handleAs: 'json'})
            .then(function(json) {
                window._HISTORY = json; // DEBUGGING
                if(json.err) {                    
                    setText('#chartstatus', 'Err: ' + json.err);
                } else {
                    setText('#chartstatus', 'Chart loaded: ' + json.data.length + ' rows');
                    var isNew = (chart1 === null);
                    
                    if(!isNew) {
                        chart1.destroy();
                    }
                    
                    var data = [];
                    var data2 = [];
                    var outages = [];
                    
                    for(var k in json.data ) {
                        var row = json.data[k].value;
                        var when = new Date(row.when).getTime();
                        var usersn = -3.0;
                        if(row.users) {
                            usersn = parseInt(row.users);
                            data.push({x:when, y:usersn});
                        } else {
                            usersn = 0;
                            data.push({x:when, y:-3.0});
                        }
                        var loadn = -1.0;
                        
                        if(row.isBusted || row.busted) {
                            loadn = -3.0;
                            outages.push(when);
                            data2.push({x:when, y:loadn});
                        } else if(row.load === null) {
                            loadn = -3.0;
                            outages.push(when);
                            data2.push({x:when, y:loadn});
                        } else {
                            loadn = parseFloat(row.load.split(' ')[0]);
                            data2.push({x:when, y:loadn});
                        }
                        
                    }
                    
                    chart1 = new Chart("SurveyChart");
                    chart1.addPlot("default", {type: Lines});
                    chart1.addAxis("x", {minorLabels: false, labelFunc: function(xx){
                        return new Date(parseInt(xx)).toLocaleString();
                    }});
                    chart1.addAxis("y", {vertical: true, min: -0});
                    chart1.addSeries("Series 1", data);
                    chart1.addSeries("Series V", data2, {plot: "default", stroke: {color: "blue"}});
                    chart1.addPlot("threshold", { type: Indicator,
                          lineStroke: { color: "red", style: "ShortDash"},
                          labels: "none",
                          labelFunc: function(xx) {return "Ⓧ" /*new Date(outages[xx]).format("HH:MM");*/},
                          values: outages});

                                           
//                    new MouseIndicator(chart1, "default", { series: "Series 1",
//                      font: "normal normal bold 12pt Tahoma",
//                      fillFunc: function(v){
//                        return v.y>55?"green":"red";
//                      },
//                      autoScroll: true,
//                      labelFunc: function(v){
//                        return v;
//                      }});
                  new MouseZoomAndPan(chart1, "default", { axis: "x" });
                  //new TouchZoomAndPan(chart1, "default", { axis: "x" });
                    chart1.render();
                    
                    
                    /*{
                    "busted": null,
                    "dbused": 1659,
                    "guests": 0,
                    "id": 6141,
                    "info": "Data Submission 24 LOCAL",
                    "isBusted": false,
                    "isSetup": true,
                    "load": "0.71 cpu=8",
                    "mem": "560.23725/1079.296",
                    "ns": 333093000,
                    "pages": null,
                    "probation": false,
                    "server": "naf",
                    "stamp": "2013-05-25T04:46:48.000Z",
                    "statusCode": 200,
                    "uptime": "uptime: 1:22:59",
                    "users": 0,
                    "when": "2013-05-25T06:09:48.000Z"
                    },
                    */
                    
                }                
            })
            .otherwise(function(err) {
                setText("#chartstatus",  "Error Loading! " + err);
            });
        
    }
    
    function consumeType(obj, newlist, now, json) {
        var keys = Object.keys(newlist);
        for(var k in keys) {
            var e = keys[k];
	    if(stw_config.reloadOnly && e != stw_config.reloadOnly) continue;
            var newEntry = newlist[e];
            var oldEntry = null;
            var odiv = obj.div;
            if(!obj.list[e]) {
                // new
                oldEntry = {
                    subDiv: dcons.create("li",{innerHTML: "<h4>"+e+"</h4>"}),
                };
                odiv.appendChild(oldEntry.subDiv);
                obj.list[e] = oldEntry;
            } else {
                oldEntry = obj.list[e];
            }
            
            obj.update(obj, e, newEntry, now, json);
        }
    };
    
    var servers = {
        list: {},
        div: dcons.create("div",{id: "servers", class: "group", innerHTML: "<h3>Servers</h3>" }),
        update: function(obj, e, newEntry, now, json) {
            var oldEntry = obj.list[e];
            var txtstatus = "unknown";
	    
            if(newEntry.lastKnownStatus) {
		txtstatus = (newEntry.lastKnownStatus.up ? "up" : "down");
	    }
            oldEntry.subDiv.className = txtstatus;

	    if(stw_config.reloadOnly) {
		oldEntry.subDiv.getElementsByTagName('h4')[0].innerHTML = txtstatus;
	    }

            var links = oldEntry.links;
            if(!links) {
                links = dcons.create("ul",{class: "links"});
                oldEntry.links = links;
                
                obj.host = newEntry.host;
                obj.hostEnt = json.hosts[obj.host];
		if(stw_config.reloadOnly) {
                    var urlLink = dcons.create("button",{innerHTML: "Try Now"});
		    urlLink.onclick = function() {
			window.location.reload(true);
		    };
		    links.appendChild(urlLink);
		} else {
		    
                    {
			var urlLink = dcons.create("a",{href: obj.hostEnt.servers[e].url, innerHTML: "go"});
			var urlLi = dcons.create("li");
			urlLi.appendChild(urlLink);
			links.appendChild(urlLi);
                    }
                    {
			var urlLink = dcons.create("button",{innerHTML: "history"});
			var urlLi = dcons.create("li");
			urlLi.appendChild(urlLink);
			links.appendChild(urlLi);
			
			urlLink.onclick = function() {
                        show_history(e, obj, json.hosts[obj.host]);
                            return false;
			};
                    }
		}
                
                oldEntry.subDiv.appendChild(links);
            }
            
            var txt = oldEntry.txt;
            if(!txt) {
                txt = dcons.create("i",{class: "info"});
                oldEntry.txt = txt;
                oldEntry.subDiv.appendChild(txt);
            }
            
            if(newEntry.latestStatus && newEntry.latestStatus.update) {
                var u = newEntry.latestStatus.update;
                var j = newEntry.latestStatus.json;
                var statusTxt = "";
                if(u.users != null) {
                    statusTxt = statusTxt + u.users+"u/";
                }
                if(u.guests != null) {
                    statusTxt = statusTxt + u.guests+"g/";
                }
                if(u.load != null) {
                    statusTxt = statusTxt + "l="+u.load +"/";
                }
                if(u.isSetup == false) {
                    statusTxt = statusTxt + "(not running)/";
                }
                if(j) {
                    if(j.status) {
                        if(j.status.currev !== null) {
                            statusTxt = statusTxt + "r"+j.status.currev+"/";
                        }
                    }
                }
                setText(txt, statusTxt);
            } else {
                setText(txt, "");
            }
        }
    };
    
     if(stw_config.reloadOnly) { // 'only' mode- no header.
	 servers.div.innerHTML="<h3>Status:</h3>";
     }
    div.adopt(servers.div);
    
    var hosts = {
        list: {},
        div: dcons.create("div",{id: "hosts", class: "group", innerHTML: "<h3>Hosts</h3>" }),
        update: function(obj, e, newEntry, now, json) {
            var oldEntry = obj.list[e];
            if(newEntry.stealth) {
                oldEntry.subDiv.className = "stealth";
            } else if(newEntry.latestPing) {
                oldEntry.subDiv.className = (newEntry.latestPing.alive ? "up" : "down");
            } else {
                oldEntry.subDiv.className = "unknown";
            }
            
            var txt = oldEntry.txt;
            if(!txt) {
                txt = dcons.create("i",{class: "info"});
                oldEntry.txt = txt;
                oldEntry.subDiv.appendChild(txt);
            }
            if(newEntry.latestPing) {
                if(newEntry.latestPing.alive) {
                    setText(txt, "Ping: " + fmtNs(newEntry.latestPing.ns));
                } else {
                //setText(txt, "Ping: " + fmtNs(newEntry.latestPing.ns));
                    setText(txt, "Ping Timeout: " + fmtNs(newEntry.latestPing.ns));
                }
            } else {
                setText(txt, "(no ping)");
            }
        }
    };

     if(!stw_config.reloadOnly) { // 'only' mode- no hosts.
	 div.adopt(hosts.div);
     }
    
    var lastTime=-1;
    var drift =0;
    
    stw_update = function() {
        request
            .get(stw_config.jsonurl, {handleAs: 'json'})
            .then(function(json) {
                var now = new Date().getTime();
                drift = now - json.now; // loading 'delay' (or clock skew)
                
                lastTime = now;
                setText("#age", "Updated just now");

                if(theInterval2 === -1) {
                    theInterval2 = setInterval(function(){
                        var now2 = new Date().getTime();
                        var age = (now2-lastTime);
                        var till = refreshTime - age;
                        var tillMsg = ", will refresh soon";
                        if(till>0) {
                            tillMsg = ", will refresh in "+ fmtNs(till*1e6);
                        }
                        var ageMsg = "just now";
                        if(age > 250) {
                            ageMsg = fmtNs(age*1e6) + " ago";
                        }
                        setText("#age", "Updated "+ageMsg +tillMsg);
                    }, 5*1000);
                }
                
                setText("#status", "skew=" + drift+"ms");
                window._JSON = json; // DEBUGGING
                
                consumeType(servers, json.servers, now, json);
                consumeType(hosts, json.hosts, now, json);
            })
            .otherwise(function(err) {
                setText("#status",  "Error Loading! (will retry) " + err);
                //clearInterval(theInterval); // do not keep thrashing
            });
    };
    
    setText("#status", "Loading...");

    stw_update();
    theInterval = setInterval(function(){console.log('Update!'); stw_update();}, refreshTime);
});
}

stwatcher_interval();

/**
 * Vetting Summary
 * @module vsummary
 */
define("js/special/vsummary.js", ["js/special/SpecialPage.js"], function(SpecialPage) {
	var _super;
	
	function Page() {
		// constructor
	}
	
	// set up the inheritance before defining other functions
	_super = Page.prototype = new SpecialPage();

//    public enum Status {
//        /** Waiting on other users/tasks */
//        WAITING,
//        /** Processing in progress */
//        PROCESSING,
//        /** Contents are available */
//        READY,
//        /** Stopped, due to some err */
//        STOPPED

	
	Page.prototype.show = function show(params) {
		// set up the 'right sidebar'
		showInPop2(stui.str("vsummaryGuidance"), null, null, null, true); /* show  right hand side */		

		// overall frag
		var ourDiv = document.createDocumentFragment();
		ourDiv.appendChild(document.createElement('p')); // break line
		
		// navigation of the summary
		var vsNav = document.createElement("div");
		vsNav.className = "vsNav";
		ourDiv.appendChild(vsNav);
		
		var vsProgress = document.createElement('div');
		vsProgress.className = 'vvprogress';
		vsProgress.style.display = 'none';
		
		vsProgress.appendChild(document.createTextNode("Progress:"));
		
		var vsBar = document.createElement('div');
		vsBar.className = 'bar';
		vsProgress.appendChild(vsBar);
		var vsRemain = document.createElement('div');
		vsRemain.className = 'remain';
		vsProgress.appendChild(vsRemain);
		
		// Reload button
		var vsReload = document.createElement('button');
		vsReload.className = 'glyphicon glyphicon-refresh';
		vsReload.title = stui.str("vsReload");
		vsReload.appendChild(document.createTextNode(vsReload.title));
		vsNav.appendChild(vsReload);

		// Stop button
		var vsStop = document.createElement('button');
		vsStop.className = 'glyphicon glyphicon-remove';
		vsStop.title = stui.str("vsStop");
		vsStop.appendChild(document.createTextNode(vsStop.title));
		vsNav.appendChild(vsStop);
		vsNav.appendChild(vsProgress);

		// Status area
		var vsStatus = document.createElement('span');
		vsStatus.className = 'vsStatus warn';
		vsNav.appendChild(vsStatus);
		
		var vsContent = document.createElement('div');
		vsContent.className = 'vsContent';
		vsContent.appendChild(document.createTextNode(stui.str('vsContent_initial')));
		ourDiv.appendChild(vsContent);
		
		var startTime = new Date();
		
		/**
		 * @function duration
		 * @param {Date} d1
		 * @param {Date} d2
		 * @return {String}
		 */
		function duration(d1,d2) {
			var diff =  (d2.getTime()-d1.getTime());
			diff = diff / 1000.0;
			if(diff < 5) {
				return ''; // now
			}
			if(diff < 120) {
				return 'Time taken: ' + (diff).toFixed(1) + ' seconds';
			}
			diff = diff / 60.0;
			if(diff < 60) {
				return 'Time taken: ' + (diff).toFixed(1) + ' minutes';
			}
			diff = diff / 60.0;
			return 'Time taken: ' + (diff).toFixed(1) + ' hours';
		}
		
		function status(cls,txt) {
			vsStatus.className = 'vsStatus '+cls;
			updateIf(vsStatus, txt + duration(startTime,new Date()));
		}
		
		status('','Setting up...');
		
		// No longer loading
		hideLoader(null);

		// Flip to the new DIV
		params.flipper.flipTo(params.pages.other, ourDiv);
		
		vsReload.disabled = true;
		
		var xurl = contextPath + "/SurveyAjax?&s="+surveySessionId+"&what=vsummary"; // allow cache
		
		/**
		 * refreshInterval: returned by setTimer, only for doReload('NOSTART'): a number, representing the
		 * ID value of the timer that is set. Use this value with the clearTimeout() method to cancel the timer.
		 */
		var refreshInterval = -1;
		
		function stopRefresh() {
			if(refreshInterval>=0) {
				clearTimeout(refreshInterval);
				refreshInterval=-1;
			}
			vsReload.disabled=false;
		}
		
		var doReload;
		
		vsReload.onclick = function(e) {
			startTime = new Date();
			vsContent.className = 'vsContent vsStale';
			doReload('FORCERESTART');
			return stStopPropagation(e);
		};
		vsStop.onclick = function(e) {
			startTime = new Date();
			doReload('FORCESTOP');
			return stStopPropagation(e);
		};
		
		/**
		 * processVVJson
		 * Called only by the load param for dojo.xhrGet in doReload.
		 */
		function processVVJson(json, onSuccess, onFailure) {
			if(json.err) {
	        	params.special.showError(params, json, {
	        		what: "Response from Vetting Viewer"});
			} else {
				var ahead='';
				if(json.output && json.output.length>0) {
					status('glyphicon glyphicon-ok','Loaded');
					vsContent.className = 'vsContent';
					updateIf(vsContent,json.output);
					vsProgress.style.display = 'none';
				} else if(!json.jstatus.t_running) {
					if(json.jstatus.t_status) {
						status('stop','Not running: ' + json.jstatus.t_status+' ');
					} else {
						status('stop','Not running.');
					}
					vsProgress.style.display = 'none';
				} else {
					var delay = 10000; // ten seconds
					if(json.jstatus.locale != json.jstatus.t_locale) {
						status('warn','Your other task is processing. '+ahead);
						vsProgress.style.display = 'none';
					} else {
						var clzz = '';
						if ( json.jstatus.t_statuscode == 'WAITING' ) {
							delay = 5000; // five seconds; quicker check when waiting to begin
							clzz = 'glyphicon glyphicon-time';
							if(json.jstatus.t_waiting && json.jstatus.t_waiting>0) {
								ahead = json.jstatus.t_waiting + ' total tasks are waiting. ';
							}
						} else if ( json.jstatus.t_statuscode == 'STOPPED') {
							clzz = 'glyphicon glyphicon-exclamation';
						} else if ( json.jstatus.t_statuscode == 'PROCESSING') {
							clzz = 'glyphicon glyphicon-cog';
						} else if ( json.jstatus.t_statuscode == 'READY') {
							clzz = 'glyphicon glyphicon-ok';
						} else {
							clzz = '';
						}
						vsProgress.style.display = '';
						var twid = 300.0;
						var barWid = (json.jstatus.t_progress / json.jstatus.t_progressmax) * twid;
						vsBar.style.width = (barWid).toFixed(0)+'px';
						vsRemain.style.width = (twid-barWid).toFixed(0)+'px';
						status(clzz, 'Your task is ' + json.jstatus.t_statuscode+
								' ('+ json.jstatus.t_status+') '+
								' '+ahead);
					}
					refreshInterval = setTimeout(function() {
						doReload('NOSTART');
					}, delay);
				}
			}
		}
		
		doReload = function doReload(policy) {
			status('glyphicon glyphicon-flash','Checking...');
			// stop old task if present
			stopRefresh();
			vsReload.disabled = true;
						
			dojo.xhrGet({
				url: xurl + '&loadingpolicy='+policy,
				handleAs: 'json',
				load: function(json) {
					stopRefresh();
					processVVJson(json, 
							function(){}, 
							function(){});
				},
				error: function(err, ioArgs) {
					stopRefresh();
		        	params.special.showError(params, null, {err: err, ioArgs: ioArgs,
		        		what: "Starting Vetting Viewer: " + policy});
				}
			});
		};
		
		doReload('NOSTART');
	};


	return Page;
});
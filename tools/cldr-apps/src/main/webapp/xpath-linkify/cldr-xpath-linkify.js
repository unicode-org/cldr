require(["dojo/query", "dojo/request", "dojo/dom", "dojo/dom-construct", "dojo/on",
         "dijit/TooltipDialog",
         "dijit/popup",
         "dojo/domReady!"],
 function cldrxpathlinkify(query,request,dom, dcons, on, TooltipDialog, popup) {

	// C&P from survey.js for now.
	
	function stdebug(x) { console.log(x); }
	
	/**
	 * @class XpathMap
	 * This manages xpid / strid / PathHeader etc mappings.
	 * It is a cache, and gets populated with data 'volunteered' by page loads, so that 
	 * hopefully it doesn't need to do any network operations on its own.
	 * However, it MAY BE capable of filling in any missing data via AJAX. This is why its operations are async.
	 */
	function XpathMap() {
		/**
		 * Maps strid (hash) to info struct.
		 * The info struct is the basic unit here, it looks like the following:
		 *   { 
		 *       hex: '20fca8231d41',
		 *       path: '//ldml/shoeSize',
		 *       id:  1337,
		 *       ph: ['foo','bar','baz']  **TBD**
		 *   }
		 *   
		 *  All other hashes here are just alternate indices into this data.
		 *  
		 * @property XpathMap.stridToInfo
		 */
		this.stridToInfo = {};
		/**
		 * Map xpid (such as 1337) to info
		 * @property XpathMap.xpidToInfo
		 */
		this.xpidToInfo = {};
		/**
		 * Map xpath (//ldml/...) to info.
		 * @property XpathMap.xpathToInfo
		 */
		this.xpathToInfo = {};

		var where=window.location.toString();
		this.url = where.substring(0,where.lastIndexOf('/xpath-linkify')+1); // chop "xpath-linkify"
		
		this.theClass = 'cldr-xpath-linkify';
	};

	/**
	 * This function will do a search and then call the onResult function. 
	 * Priority order for search:  hex, id, then path.
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
		if(!result && search.hex) {
			result = this.stridToInfo[search.hex];
		}
		if(!result && search.id) {
			if(typeof search.id !== Number) {
				search.id = new Number(search.id);
			}
			result = this.xpidToInfo[search.id];
		}
		if(!result && search.path) {
			result = this.xpathToInfo[search.path];
		}
		if(result) {
			onResult({search:search, result:result});
		} else {
			stdebug("XpathMap search failed for " + JSON.stringify(search) +  " - doing rpc");
			var querystr = null;
			if(search.hex) {
				querystr = search.hex;
			} else if(search.path) {
				querystr = search.path;
			} else if(search.id) {
				querystr = "#"+search.id;
			} else {
				querystr = ''; // error
			}
			(function(xpm){
				var requrl = xpm.url+'SurveyAjax';
				stdebug('Fetch: ' + requrl);
				require(["dojo/request"], function(request) {
					request
					.get(requrl, {handleAs: 'json',
										query: {
											what: 'getxpath',
											xpath: querystr
										}})
					.then(function(json) {
						if(json.getxpath) {
							xpm.put(json.getxpath); // store back first, then
							onResult({search: search, result:json.getxpath}); // call
						} else {
							onResult({search: search, err:'no results from server'});
						}
					})
					.otherwise(function(err) {
						onResult({search: search, err:err});
					});
				});
			})(this);
		}
	};

	/**
	 * Contribute some data to the map.
	 * @function contribute
	 */
	XpathMap.prototype.put = function put(info) {
		if(!info || !info.id || !info.path || !info.hex || !info.ph) {
			stdebug("XpathMap: rejecting incomplete contribution " + JSON.stringify(info));
		} else if(this.stridToInfo[info.hex]) {
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
		if(!ph) {
			return '';
		} else {
			var phArray = [ph.section, ph.page, ph.header, ph.code];
			return phArray.join(' | '); // type error - valid?
		}
	};

	/**
	 * @class GLOBAL
	 */
	var xpathMap = null;
	
	xpathMap = new XpathMap();

	function linkify(opts) {
		if(opts) {
			if(opts.url) {
				xpathMap.url = opts.url;
			}
		}
		// OK, do it.
		var items = document.getElementsByClassName(xpathMap.theClass);
		for(var i=0;i<items.length;i++) {
			var node = items[i];
			(function(node){
				var xpath = node.textContent;
				console.log("Linkify: " + xpath);
				
				node.xpathDialog = null;
				
				function showPopup() {
					if(node.xpathDialog != null) {
						// already created
						popup.open({
							popup: node.xpathDialog,
							around: node
						});
					} else {
						xpathMap.get({hex:xpath}, function(o) {
							if(o.result && o.result.path) {
								node.xpathDialog = new TooltipDialog({  
									style: "width: 600px;",
									content: "<span style='font-size: smaller;'><b>XPath:</b> "+o.result.path+"<br>"+
												"<b>Where:</b> "+xpathMap.formatPathHeader(o.result.ph)+"</span>",
									onMouseLeave: function() {
										popup.close(node.xpathDialog);
									}
								});
							} else {
								node.xpathDialog = new TooltipDialog({  
									style: "width: 200px;",
									content: "<i>Error: "+o.err+"</i><br>If the server is down, you must refresh this page to try again.",
									onMouseLeave: function() {
										popup.close(node.xpathDialog);
									}
								});
							}
							popup.open({
								popup: node.xpathDialog,
								around: node
							});
						});
					}
				}
				// ok, wire it up
				on(node, 'mouseover', showPopup); // static
				//on(node, 'onchange', showPopup); // input fields?
			})(node);
		}
	}
	
	linkify(window.xpathLinkifyOpts);
});
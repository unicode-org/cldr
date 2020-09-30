'use strict';

/**
 * cldrStAjax: encapsulate client-server communication.
 * Use an IIFE pattern to create a namespace for the public functions,
 * and to hide everything else, minimizing global scope pollution.
 * Ideally cldrStAjax should be a module (in the sense of using import/export),
 * but not all Survey Tool JavaScript code is capable yet of being in modules
 * and running in strict mode.
 */
const cldrStAjax = (function() {

	const ST_AJAX_DEBUG = false;

	/**
	 * Queue of XHR requests waiting to go out
	 */
	var queueOfXhr = [];

	/**
	 * The current timeout for processing XHRs
	 * (Returned by setTimer: a number, representing the ID value of the timer that is set.
	 * Use this value with the clearTimeout() method to cancel the timer.)
	 */
	var queueOfXhrTimeout = null;

	/**
	 * Queue the XHR request. It will be a GET *unless* either postData or content are set.
	 *
	 * @param xhr the object, generally like:
	 * {
	 *   url: url,
	 *   handleAs: "json",
	 *   load: loadHandler,
	 *   error: errorHandler,
	 *   postData: postData, (or sometimes "content" instead of "postData")
	 *   content: ourContent,
	 *   timeout: ajaxTimeout,
	 *   headers: headers, (rarely used, but in loadOrFail it's {"Content-Type": "text/plain"})
	 * }
	 */
	function queueXhr(xhr) {
		queueOfXhr.push(xhr);
		if (ST_AJAX_DEBUG) {
			console.log("pushed: PXQ=" + queueOfXhr.length + ", postData: " + xhr.postData);			
		}
		if (!queueOfXhrTimeout) {
			queueOfXhrTimeout = setTimeout(processXhrQueue, xhrQueueTimeout);
		}
	}

	/**
	 * Clear the queue
	 */
	function clearXhr() {
		queueOfXhr = []; // clear queue
		clearTimeout(queueOfXhrTimeout);
		queueOfXhrTimeout = null;
	}

	/**
	 * Process the queue
	 */
	function processXhrQueue() {
		/*
		 * TODO: getter/setter for global variable "disconnected" in survey.js 
		 */
		if (disconnected) {
			return;
		}
		if (!queueOfXhr || queueOfXhr.length == 0) {
			queueOfXhr = [];
			if (ST_AJAX_DEBUG) {
				console.log("PXQ: 0");
			}
			queueOfXhrTimeout = null;
			return; // nothing to do, reset.
		}

		var xhr = queueOfXhr.shift();

		xhr.load2 = xhr.load;
		xhr.err2 = xhr.error;

		xhr.load = function(data) {
			myLoad0(xhr, data);
		};
		xhr.error = function(err) {
			myErr0(xhr, err);
		};
		xhr.startTime = new Date().getTime();

		sendXhr(xhr);
	}

	/**
	 * xhrQueueTimeout is a constant, 3 milliseconds, used only by
	 * myLoad0, myErr0, and queueXhr, in calls to setTimeout for processXhrQueue.
	 * TODO: explain, why 3 milliseconds?
	 */
	const xhrQueueTimeout = 3;

	/**
	 * Run the load handler (load2) and schedule the next request
	 *
	 * @param xhr the request parameters plus such things as xhr.load2
	 * @param data the data (typically json)
	 */
	function myLoad0(xhr, data) {
		if (ST_AJAX_DEBUG) {
			xhr.stopTime = new Date().getTime();
			xhr.tookTime = xhr.stopTime - xhr.startTime;
			console.log("PXQ(" + queueOfXhr.length + "): time took= " + xhr.tookTime);
			console.log("myLoad0!:" + xhr.url);
		}
		xhr.load2(data);
		queueOfXhrTimeout = setTimeout(processXhrQueue, xhrQueueTimeout);
	}

	/**
	 * Run the error handler (err2) and schedule the next request
	 *
	 * @param xhr the request parameters plus such things as xhr.err2
	 * @param err the Error object plus things dojo may add like err.response.text
	 */
	function myErr0(xhr, err) {
		if (ST_AJAX_DEBUG) {
			console.log("myErr0!:" + xhr.url);
		}
		/*
		 * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Error
		 * Standard properties
		 * 	Error.prototype.message
		 * 	Error.prototype.name
		 * Some ST errorHandler functions do access err.name (e.g., "RequestError") and/or err.message.
		 */
		xhr.err2(err);
		queueOfXhrTimeout = setTimeout(processXhrQueue, xhrQueueTimeout);
	}

	/**
	 * Send a request immediately.
	 *
	 * Called by processXhrQueue for events from queue, and also directly from
	 * other modules to send requests without using the queue.
	 *
	 * @param xhr the request parameters
	 *
	 * https://dojotoolkit.org/reference-guide/1.10/dojo/request.html#dojo-request
	 */
	function sendXhr(xhr) {
		require(["dojo/request"], function(request) {
			let options = {};
			if (xhr.handleAs) {
				options.handleAs = xhr.handleAs;
			}
			if (xhr.postData || xhr.content) {
				options.method = 'POST';
				options.data = xhr.postData ? xhr.postData : xhr.content;
			}
			request(xhr.url, options).then(function(data) {
				xhr.load(data);
			}, function(err) {
				xhr.error(err);
			}, function(evt) {
				// handle a progress event
			});
		});
	}

	/**
	 * Get the response text from an err object
	 *
	 * err.response seems to be a dojo thing, not defined by JavaScript itself. So, encapsulate it here.
	 *
	 * Note that earlier dojo versions instead sent second parameter "ioargs" to error handlers,
	 * which could use it for ioargs.xhr.responseText.
	 *
	 * @param err the Error object plus things dojo may add like err.response.text
	 */
	function errResponseText(err) {
		return (err && err.response && err.response.text) ? err.response.text : '';
	}

	/**
	 * How many requests are in the queue?
	 *
	 * @return the number of requests in the queue
	 */
	function queueCount() {
		const count = queueOfXhr ? queueOfXhr.length : 0;
		if (ST_AJAX_DEBUG) {
			console.log("queueCount: " + count);
		}
		return count;
	}

	/*
	 * Make only these functions accessible from other files:
	 */
	return {
		queueXhr: queueXhr,
		clearXhr: clearXhr,
		sendXhr: sendXhr,
		errResponseText: errResponseText,
		queueCount: queueCount,
	};
})();

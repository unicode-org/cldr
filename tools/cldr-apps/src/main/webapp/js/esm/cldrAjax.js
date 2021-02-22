/*
 * cldrAjax: encapsulate client-server communication.
 */
import * as cldrStatus from "./cldrStatus.js";

const ST_AJAX_DEBUG = true;

/**
 * xhrQueueTimeout is a constant, 3 milliseconds, used only by
 * myLoad0, myErr0, and queueXhr, in calls to setTimeout for processXhrQueue.
 * Why 3 milliseconds?
 */
const xhrQueueTimeout = 3;

/**
 * Queue of XHR requests waiting to go out
 */
let queueOfXhr = [];

/**
 * The current timeout for processing XHRs
 * (Returned by setTimer: a number, representing the ID value of the timer that is set.
 * Use this value with the clearTimeout() method to cancel the timer.)
 */
let queueOfXhrTimeout = null;

/**
 * Queue the XHR request. It will be a GET *unless* either postData or content are set.
 *
 * @param xhrArgs the object, generally like:
 * {
 *   url: url,
 *   handleAs: "json",
 *   load: loadHandler,
 *   error: errorHandler,
 *   postData: postData, (or sometimes "content" instead of "postData")
 *   content: ourContent,
 *   headers: headers, (rarely used, but in loadOrFail it's {"Content-Type": "text/plain"}),
 *   timeout: mediumTimeout(),
 * }
 */
function queueXhr(xhrArgs) {
  queueOfXhr.push(xhrArgs);
  if (ST_AJAX_DEBUG) {
    console.log(
      "pushed: PXQ=" + queueOfXhr.length + ", postData: " + xhrArgs.postData
    );
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
  if (cldrStatus.isDisconnected()) {
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

  var xhrArgs = queueOfXhr.shift();

  xhrArgs.load2 = xhrArgs.load;
  xhrArgs.err2 = xhrArgs.error;

  xhrArgs.load = function (data) {
    myLoad0(xhrArgs, data);
  };
  xhrArgs.error = function (err) {
    myErr0(xhrArgs, err);
  };
  xhrArgs.startTime = new Date().getTime();

  sendXhr(xhrArgs);
}

/**
 * Run the load handler (load2) and schedule the next request
 *
 * @param xhrArgs the request parameters plus such things as xhrArgs.load2
 * @param data the data (typically json)
 */
function myLoad0(xhrArgs, data) {
  if (ST_AJAX_DEBUG) {
    xhrArgs.stopTime = new Date().getTime();
    xhrArgs.tookTime = xhrArgs.stopTime - xhrArgs.startTime;
    console.log(
      "PXQ(" + queueOfXhr.length + "): time took= " + xhrArgs.tookTime
    );
    console.log("myLoad0!:" + xhrArgs.url);
  }
  if (xhrArgs.load2) {
    xhrArgs.load2(data);
  }
  queueOfXhrTimeout = setTimeout(processXhrQueue, xhrQueueTimeout);
}

/**
 * Run the error handler (err2) and schedule the next request
 *
 * @param xhrArgs the request parameters plus such things as xhrArgs.err2
 * @param err the error-message string
 */
function myErr0(xhrArgs, err) {
  if (ST_AJAX_DEBUG) {
    console.log("myErr0!:" + xhrArgs.url);
  }
  if (xhrArgs.err2) {
    xhrArgs.err2(err);
  }
  queueOfXhrTimeout = setTimeout(processXhrQueue, xhrQueueTimeout);
}

/**
 * Send a request immediately.
 *
 * Called by processXhrQueue for events from queue, and also directly from
 * other modules to send requests without using the queue.
 *
 * @param xhrArgs the request parameters
 */
function sendXhr(xhrArgs) {
  let options = {};
  if (xhrArgs.handleAs) {
    options.handleAs = xhrArgs.handleAs;
  }
  if (xhrArgs.postData || xhrArgs.content) {
    options.method = "POST";
    options.data = xhrArgs.postData ? xhrArgs.postData : xhrArgs.content;
  } else {
    options.method = "GET";
  }
  const request = new XMLHttpRequest();
  if (typeof USE_DOJO !== "undefined") {
    if (xhrArgs.url.indexOf("?") == -1) {
      xhrArgs.url += "?USE_DOJO=" + USE_DOJO;
    } else {
      xhrArgs.url += "&USE_DOJO=" + USE_DOJO;
    }
  }
  request.open(options.method, xhrArgs.url);
  request.responseType = options.handleAs ? options.handleAs : "text";
  request.timeout = xhrArgs.timeout ? xhrArgs.timeout : 0;
  request.onreadystatechange = function () {
    onChange(request, xhrArgs);
  };
  if (options.method === "POST") {
    setPostDataAndHeader(request, options);
  }
  request.send(options.data);
}

function onChange(request, xhrArgs) {
  if (request.readyState !== XMLHttpRequest.DONE) {
    return;
  }
  if (request.status >= 200 && request.status < 400 && request.response) {
    if (xhrArgs.load) {
      xhrArgs.load(request.response);
    }
  } else if (xhrArgs.error) {
    xhrArgs.error(makeErrorMessage(request, xhrArgs.url));
  }
}

function setPostDataAndHeader(request, options) {
  if (options.data instanceof FormData) {
    // content-type header automatically gets set to "multipart/form-data" by the browser
    return;
  }
  if (typeof options.data === "string") {
    request.setRequestHeader("content-type", "text/plain");
  } else if (typeof options.data === "object") {
    options.data = objToQuery(options.data);
    request.setRequestHeader(
      "content-type",
      "application/x-www-form-urlencoded"
    );
  } else {
    console.log(
      "Error in setPostDataAndHeader: unexpected typeof options.data: " +
        typeof options.data
    );
  }
}

/**
 * Given an object, return a query string for use in a POST request body
 *
 * @param obj like {what: 'submit', value: '好'}
 * @return a string like 'what=submit&value=好'
 *
 * SurveyAjax accepts POST requests with objToQuery + application/x-www-form-urlencoded,
 * but mysteriously fails with JSON.stringify + application/json
 */
function objToQuery(obj) {
  let q = [];
  for (let key in obj) {
    q.push(encodeURIComponent(key) + "=" + encodeURIComponent(obj[key]));
  }
  return q.join("&");
}

function makeErrorMessage(request, url) {
  let msg =
    "Status " + request.status + " " + request.statusText + "; URL: " + url;
  if (!request.status) {
    msg += " Status zero -- no response; timed out?";
  } else if (
    request.status >= 200 &&
    request.status < 400 &&
    !request.response
  ) {
    // this happens if server does ctx.println(msg) before sending json
    msg += " Missing response; responseType = " + request.responseType;
  } else if (request.responseType === "text" || request.responseType === "") {
    if (request.responseText) {
      msg += " Response: " + request.responseText;
    }
  } else if (request.responseType === "json") {
    if (request.response) {
      msg += JSON.stringify(request.response);
    }
  } else {
    msg += " Response type: " + request.responseType;
    if (ST_AJAX_DEBUG) {
      console.log("makeErrorMessage got responseType=" + request.responseType);
    }
  }
  return msg;
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

/**
 * Used for XMLHttpRequest = the number of milliseconds a request can take before being terminated.
 * Zero (the default unless we set it) means there is no timeout.
 */
function mediumTimeout() {
  return 120000; // 2 minutes
}

/**
 * Make an AJAX URL for Survey Tool using the given URLSearchParams
 *
 * @param {URLSearchParams} p
 * @return the URL string
 */
function makeUrl(p) {
  return cldrStatus.getContextPath() + "/SurveyAjax?" + p.toString();
}

export { clearXhr, makeUrl, mediumTimeout, sendXhr, queueCount, queueXhr };

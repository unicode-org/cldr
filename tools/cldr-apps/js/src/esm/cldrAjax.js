/*
 * cldrAjax: encapsulate client-server communication.
 */
import * as cldrStatus from "./cldrStatus.js";

const ST_AJAX_DEBUG = true;

/**
 * Call the standard js fetch function, possibly with additional handling suitable
 * for Survey Tool such as setting session headers and debugging
 *
 * Session headers are automatically added unless init.headers is already set
 *
 * @param {String} resource the url or other resource, or Request object
 * @param {Object} init an object containing any custom settings for the request
 * @returns a Promise that resolves to a Response object
 */
function doFetch(resource, init) {
  if (ST_AJAX_DEBUG) {
    console.log("cldrAjax.doFetch: " + resource);
  }
  if (!init) {
    init = {};
  }
  if (!init.headers) {
    init.headers = cldrStatus.sessionHeaders();
  }
  return fetch(resource, init);
}

/**
 * Send a request
 *
 * It will be a GET *unless* either postData or content are set.
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
function sendXhr(xhrArgs) {
  let options = {};
  if (xhrArgs.handleAs) {
    options.handleAs = xhrArgs.handleAs;
  }
  if (xhrArgs.postData || xhrArgs.content) {
    options.method = "POST";
    options.data = xhrArgs.postData ? xhrArgs.postData : xhrArgs.content;
    if (typeof options.data === "object" && xhrArgs.url.includes("api/")) {
      options.makeJsonPost = true;
    }
  } else {
    options.method = "GET";
  }
  const request = new XMLHttpRequest();
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
    if (options.makeJsonPost) {
      options.data = JSON.stringify(options.data);
      request.setRequestHeader("content-type", "application/json");
    } else {
      options.data = objToQuery(options.data);
      request.setRequestHeader(
        "content-type",
        "application/x-www-form-urlencoded"
      );
    }
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
 * Used for XMLHttpRequest = the number of milliseconds a request can take before being terminated.
 * Zero (the default unless we set it) means there is no timeout.
 */
function mediumTimeout() {
  return 120000; // 2 minutes
}

/**
 * Make a SurveyAjax URL for Survey Tool using the given URLSearchParams
 *
 * @param {URLSearchParams} p
 * @return the URL string
 */
function makeUrl(p) {
  return cldrStatus.getContextPath() + "/SurveyAjax?" + p.toString();
}

/**
 * Make an API ajax URL for Survey Tool using the given URLSearchParams
 *
 * @param {String} api the remainder of the url to follow /api/, not including the query string
 * @param {URLSearchParams} p the parameters for the query string; or null (or empty) for none
 * @return the URL string
 */
function makeApiUrl(api, p) {
  const queryString = p ? "?" + p.toString() : "";
  return cldrStatus.getContextPath() + "/api/" + api + queryString;
}

export { doFetch, makeApiUrl, makeUrl, mediumTimeout, sendXhr };

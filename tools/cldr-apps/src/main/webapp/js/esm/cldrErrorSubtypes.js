/*
 * cldrErrorSubtypes: encapsulate functions for the "Error Subtypes" page of Survey Tool
 */
import * as cldrAjax from "./cldrAjax.js";
import * as cldrInfo from "./cldrInfo.js";
import * as cldrLoad from "./cldrLoad.js";
import * as cldrStatus from "./cldrStatus.js";
import * as cldrSurvey from "./cldrSurvey.js";

const instructions =
  "<p>\n" +
  "<i>Instructions</i>: This page shows the status of the subtype-to-URL mapping data.\n" +
  "Each line here shows the CLDR error subtypes.<br />\n" +
  "<b>Code</b> - this is the code <br/>\n" +
  "<b>url</b> - this is the URL specified in the subtypeMapping.txt file<br/>\n" +
  "<b>Status</b> - this shows whether the URL was fetched successfully. (200 indicates success.)\n" +
  "Click the 'reload' 🔄 button to re-check the URL.\n" +
  "</p>\n";

const coupleSeconds = 2000;
const redirectSoon = "<p>(Redirect in a couple seconds)</p>";

const mainId = "errorSubtypes";

// called as special.load
function load() {
  cldrInfo.showNothing();
  const xhrArgs = {
    url: getUrl(),
    handleAs: "json",
    load: loadHandler,
    error: errorHandler,
  };
  cldrAjax.sendXhr(xhrArgs);
}

function getUrl() {
  return cldrStatus.getContextPath() + "/SurveyAjax?what=error_subtypes";
}

function loadHandler(json) {
  const ourDiv = document.createElement("div");
  ourDiv.setAttribute("id", mainId);
  ourDiv.innerHTML = getHtml(json);
  cldrSurvey.hideLoader();
  cldrLoad.flipToOtherDiv(ourDiv);
  setOnClicks();
}

function errorHandler(err) {
  const ourDiv = document.createElement("div");
  ourDiv.innerHTML = err;
  cldrSurvey.hideLoader();
  cldrLoad.flipToOtherDiv(ourDiv);
}

function getHtml(json) {
  let html = "<p><b>CLDR_SUBTYPE_URL</b> " + json.CLDR_SUBTYPE_URL + "</p>\n";
  html += "<p><a id='reloadErrorSubtypeMap'>🔄 Reload Map</a>\n";
  html += json.urls ? "Map OK! (may be cached)" : "<b>Could not load map.</b>";
  html += "</p>";
  if (!json.urls) {
    return html;
  }
  html += instructions;
  html += "<p><a id='recheckAllUrls'>🔄 Recheck all URLs</a></p>\n";
  html += "<hr />\n";
  html += "<pre>" + json.COMMENT + " " + json.BEGIN_MARKER + "</pre>\n";
  for (let i in json.urls) {
    html += getStatusHtml(json.urls[i]);
  }
  html += getUnhandledHtml(json);
  html += "<pre>" + json.COMMENT + " " + json.END_MARKER + "</pre>\n";
  return html;
}

function getStatusHtml(stat) {
  let html =
    "<pre>#------------------</pre>\n" +
    "<pre><a title='HTTP:" +
    stat.status +
    "' href='" +
    stat.url +
    "'>" +
    stat.url +
    "</a></pre>\n";
  if (stat.strings) {
    for (let j = 0; j < stat.strings.length; j++) {
      const string = stat.strings[j];
      const name = stat.names[j];
      html += "<b><pre title='" + string + "'>" + name + ",</pre></b>\n";
    }
  } else {
    html +=
      "# URL failed to fetch: " +
      stat.status +
      " <a class='recheckOneUrl' title='" +
      stat.url +
      "'>🔄</a><br/>\n";
  }
  return html;
}

function getUnhandledHtml(json) {
  let html = "";
  if (!json.unhandled) {
    html += "<p><b># All types handled!</b></p>\n";
  } else {
    html += "<h2># Missing these subtypes:</h2>\n";
    for (let j = 0; j < json.unhandled.strings.length; j++) {
      const string = json.unhandled.strings[j];
      const name = json.unhandled.names[j];
      html += "<b><pre title='" + string + "'>" + name + ",</pre></b>\n";
    }
  }
  return html;
}

function reloadMap() {
  const el = getMainEl();
  if (el) {
    el.innerHTML = "<h1>Reloading URL map...</h1>";
  }
  const xhrArgs = {
    url: getUrl() + "&flush=MAP",
    handleAs: "json",
    load: reloadMapHandler,
    error: errorHandler,
  };
  cldrAjax.sendXhr(xhrArgs);
}

function reloadMapHandler(json) {
  const el = getMainEl();
  if (!el) {
    return;
  }
  let html = "<h1>Reloaded URL map</h1>";
  if (json.err) {
    html += "<p>Error: " + json.err + "</p>";
    if (json.stack) {
      html += "<p>Stack:<br /><pre>" + json.stack + "</pre></p>";
    }
  } else {
    html += "<p>" + json.status + "</p>";
  }
  html += redirectSoon;
  el.innerHTML = html;
  window.setTimeout(load, coupleSeconds);
}

function setOnClicks() {
  let el = document.getElementById("reloadErrorSubtypeMap");
  if (el) {
    el.onclick = () => reloadMap();
  }
  el = document.getElementById("recheckAllUrls");
  if (el) {
    el.onclick = () => recheckAll();
  }
  const recheckOneUrlElements = document.getElementsByClassName(
    "recheckOneUrl"
  );
  for (let i = 0; i < recheckOneUrlElements.length; i++) {
    const el = recheckOneUrlElements[i];
    // el.title === stat.url
    el.onclick = () => recheckOneUrl(el.title);
  }
}

function recheckAll() {
  const el = getMainEl();
  if (el) {
    el.innerHTML = "<h1>Flushing cache...</h1>";
  }
  const xhrArgs = {
    url: getUrl() + "&flush=true",
    handleAs: "json",
    load: recheckHandler,
    error: errorHandler,
  };
  cldrAjax.sendXhr(xhrArgs);
}

function recheckOneUrl(oneUrl) {
  const el = getMainEl();
  if (el) {
    el.innerHTML = "<h1>Flushing " + oneUrl + " from cache...</h1>";
  }
  const xhrArgs = {
    url: getUrl() + "&flush=" + oneUrl,
    handleAs: "json",
    load: recheckHandler,
    error: errorHandler,
  };
  cldrAjax.sendXhr(xhrArgs);
}

function recheckHandler(json) {
  const el = getMainEl();
  if (!el) {
    return;
  }
  el.innerHTML = "<h1>" + json.status + "</h1>" + redirectSoon;
  window.setTimeout(load, coupleSeconds);
}

function getMainEl() {
  const el = document.getElementById(mainId);
  if (!el) {
    console.log("Error: id " + mainId + " not found!");
  }
  return el;
}

export {
  load,
  /*
   * The following are meant to be accessible for unit testing only:
   */
  getHtml,
};

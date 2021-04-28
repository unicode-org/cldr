import * as cldrAjax from "./cldrAjax.js";
import * as cldrSurvey from "./cldrSurvey.js";

const CLDR_XPATH_DEBUG = false;

/**
 * This manages xpathId / strid / PathHeader etc mappings.
 * It is a cache, and gets populated with data 'volunteered' by page loads, so that
 * hopefully it doesn't need to do any network operations on its own.
 * However, it MAY BE capable of filling in any missing data via AJAX. This is why its operations are async.
 */
function XpathMap() {
  /**
   * Maps strid (hash) to info struct.
   * The info struct is the basic unit here, it looks like the following:
   *   {
   *	   hex: '20fca8231d41',
   *	   path: '//ldml/shoeSize',
   *	   id:  1337,
   *	   ph: ['foo','bar','baz']  **TBD**
   *   }
   *
   *  All other hashes here are just alternate indices into this data.
   *
   * @property XpathMap.stridToInfo
   */
  this.stridToInfo = {};
  /**
   * Map xpathId (such as 1337) to info
   * @property XpathMap.xpidToInfo
   */
  this.xpidToInfo = {};
  /**
   * Map xpath (//ldml/...) to info.
   * @property XpathMap.xpathToInfo
   */
  this.xpathToInfo = {};
}

/**
 * This function will do a search and then call the onResult function.
 * Priority order for search: hex, id, then path.
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
  let result = this.getImmediately(search);
  if (result) {
    onResult({
      search: search,
      result: result,
    });
  } else {
    if (CLDR_XPATH_DEBUG) {
      console.log(
        "XpathMap search failed for " + JSON.stringify(search) + " - doing rpc"
      );
    }
    var querystr = null;
    if (search.hex) {
      querystr = search.hex;
    } else if (search.path) {
      querystr = search.path;
    } else if (search.id) {
      querystr = "#" + search.id;
    } else {
      querystr = ""; // error
    }
    const loadHandler = function (json) {
      if (json.getxpath) {
        const xpathMap = cldrSurvey.getXpathMap();
        xpathMap.put(json.getxpath); // store back first, then
        onResult({
          search: search,
          result: json.getxpath,
        }); // call
      } else {
        onResult({
          search: search,
          err: "no results from server",
        });
      }
    };

    const errorHandler = function (err) {
      onResult({
        search: search,
        err: err,
      });
    };

    const xhrArgs = {
      url: "SurveyAjax?what=getxpath&xpath=" + querystr,
      handleAs: "json",
      load: loadHandler,
      err: errorHandler,
    };
    cldrAjax.sendXhr(xhrArgs);
  }
};

XpathMap.prototype.getImmediately = function getImmediately(search) {
  let result = null;
  if (search.hex) {
    result = this.stridToInfo[search.hex];
  }
  if (!result && search.id) {
    if (typeof search.id !== Number) {
      search.id = new Number(search.id);
    }
    result = this.xpidToInfo[search.id];
  }
  if (!result && search.path) {
    result = this.xpathToInfo[search.path];
  }
  return result;
};

/**
 * Contribute some data to the map.
 * @function contribute
 */
XpathMap.prototype.put = function put(info) {
  if (!info || !info.id || !info.path || !info.hex || !info.ph) {
    if (CLDR_XPATH_DEBUG) {
      console.log(
        "XpathMap: rejecting incomplete contribution " + JSON.stringify(info)
      );
    }
  } else if (this.stridToInfo[info.hex]) {
    if (CLDR_XPATH_DEBUG) {
      console.log(
        "XpathMap: rejecting duplicate contribution " + JSON.stringify(info)
      );
    }
  } else {
    this.stridToInfo[info.hex] = this.xpidToInfo[info.id] = this.xpathToInfo[
      info.path
    ] = info;
    if (CLDR_XPATH_DEBUG) {
      console.log("XpathMap: adding contribution " + JSON.stringify(info));
    }
  }
};

/**
 * Format a pathheader array.
 * @function formatPathHeader
 * @param ph {Object} pathheaer struct (Section/Page/Header/Code)
 * @return {String}
 */
XpathMap.prototype.formatPathHeader = function formatPathHeader(ph) {
  if (!ph) {
    return "";
  } else {
    var phArray = [ph.section, ph.page, ph.header, ph.code];
    return phArray.join(" | "); // type error - valid?
  }
};

export { XpathMap };

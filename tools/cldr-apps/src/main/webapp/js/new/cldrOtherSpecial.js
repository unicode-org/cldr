// "use strict";
// TODO: modernize, make strict, possibly a class
/**
 * Manage additional special pages
 * @class OtherSpecial
 */
function OtherSpecial() {
  // cached page list
  this.pages = {};
}

const OTHER_SPECIAL_DEBUG = true;

/**
 * @function getSpecial
 */
OtherSpecial.prototype.getSpecial = function getSpecial(name) {
  return this.pages[name];
};

/**
 * @function loadSpecial
 */
OtherSpecial.prototype.loadSpecial = function loadSpecial(
  name,
  onSuccess,
  onFailure
) {
  var special = this.getSpecial(name);
  var otherThis = this;
  if (special) {
    if (OTHER_SPECIAL_DEBUG) {
      console.log("OS: Using cached special: " + name);
    }
    onSuccess(special);
  } else if (special === null) {
    if (OTHER_SPECIAL_DEBUG) {
      console.log("OS: cached NULL: " + name);
    }
    onFailure("Special page failed to load: " + name);
  } else {
    if (OTHER_SPECIAL_DEBUG) {
      console.log("OS: Attempting load.." + name);
    }
    /***
        try {
          require(["js/special/" + name + ".js"], function (specialFn) {
            if (OTHER_SPECIAL_DEBUG) {
    	      console.log("OS: Loaded, instantiatin':" + name);
    	    }
            var special = new specialFn();
            special.name = name;
            otherThis.pages[name] = special; // cache for next time

            if (OTHER_SPECIAL_DEBUG) {
    	      console.log("OS: SUCCESS! " + name);
    	    }
            onSuccess(special);
          });
        } catch (e) {
          if (OTHER_SPECIAL_DEBUG) {
    	    console.log("OS: Load FAIL!:" + name + " - " + e.message + " - " + e);
    	  }
          if (!otherThis.pages[name]) {
            // if the load didn't complete:
            otherThis.pages[name] = null; // mark as don't retry load.
          }
          onFailure(e);
        }
        ***/
  }
};

/**
 * @function parseHash
 */
OtherSpecial.prototype.parseHash = function parseHash(name, hash, pieces) {
  this.loadSpecial(
    name,
    function onSuccess(special) {
      special.parseHash(hash, pieces);
    },
    function onFailure(e) {
      /*
       * TODO: get rid of this console warning for name = "oldvotes".
       * There's not a known problem with old votes. It's not clear why
       * the warning occurs...
       */
      console.log("OtherSpecial.parseHash: Failed to load " + name + " - " + e);
    }
  );
};

/**
 * @function handleIdChanged
 */
OtherSpecial.prototype.handleIdChanged = function handleIdChanged(name, id) {
  this.loadSpecial(
    name,
    function onSuccess(special) {
      special.handleIdChanged(id);
    },
    function onFailure(e) {
      console.log(
        "OtherSpecial.handleIdChanged: Failed to load " + name + " - " + e
      );
    }
  );
};

/**
 * @function showPage
 */
OtherSpecial.prototype.show = function show(name, params) {
  this.loadSpecial(
    name,
    function onSuccess(special) {
      // populate the params a little more
      params.otherSpecial = this;
      params.name = name;
      params.special = special;

      // add anything from scope..

      params.exports = {
        appendLocaleLink: cldrLoad.appendLocaleLink,
        handleDisconnect: cldrSurvey.handleDisconnect,
        clickToSelect: cldrDom.clickToSelect,
      };

      special.show(params);
    },
    function onFailure(err) {
      // extended error
      var loadingChunk;
      var msg_fmt = cldrText.sub("v_bad_special_msg", {
        special: name,
      });
      params.flipper.flipTo(
        params.pages.loading,
        (loadingChunk = cldrDom.createChunk(msg_fmt, "p", "errCodeMsg"))
      );
      isLoading = false;
    }
  );
};

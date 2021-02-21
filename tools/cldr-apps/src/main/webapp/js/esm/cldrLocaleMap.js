import * as cldrLoad from "./cldrLoad.js";
import * as cldrStatus from "./cldrStatus.js";

/**
 * @param aLocmap the map object from json
 */
function LocaleMap(aLocmap) {
  this.locmap = aLocmap;
}

/**
 * Run the locale id through the idmap.
 *
 * @param menuMap the map
 * @param locid or null for cldrStatus.getCurrentLocale()
 * @return canonicalized id, or unchanged
 */
LocaleMap.prototype.canonicalizeLocaleId = function canonicalizeLocaleId(
  locid
) {
  if (locid === null) {
    locid = cldrStatus.getCurrentLocale();
  }
  if (locid === null || locid === "") {
    return null;
  }

  if (this.locmap) {
    if (this.locmap.idmap && this.locmap.idmap[locid]) {
      locid = this.locmap.idmap[locid]; // canonicalize
    }
  }
  return locid;
};

/**
 * Linkify text like '@de' into some link to German.
 *
 * @param str (html)
 * @return linkified str (html)
 */
LocaleMap.prototype.linkify = function linkify(str) {
  var out = "";
  var re = /@([a-zA-Z0-9_]+)/g;
  var match;
  var fromLast = 0;
  while ((match = re.exec(str)) != null) {
    var bund = this.getLocaleInfo(match[1]);
    if (bund) {
      out = out + str.substring(fromLast, match.index); // pre match
      if (match[1] == cldrStatus.getCurrentLocale()) {
        out = out + this.getLocaleName(match[1]);
      } else {
        out =
          out +
          "<a href='" +
          cldrLoad.linkToLocale(match[1]) +
          "' title='" +
          match[1] +
          "'>" +
          this.getLocaleName(match[1]) +
          "</a>";
      }
    } else {
      out = out + match[0]; // no link.
    }
    fromLast = re.lastIndex;
  }
  out = out + str.substring(fromLast, str.length);
  return out;
};

/**
 * Return the locale info entry
 *
 * @param menuMap the map
 * @param locid the id - should already be canonicalized
 * @return the bundle or null
 */
LocaleMap.prototype.getLocaleInfo = function getLocaleInfo(locid) {
  if (this.locmap && this.locmap.locales && this.locmap.locales[locid]) {
    return this.locmap.locales[locid];
  } else {
    return null;
  }
};

/**
 * Return the locale name,
 *
 * @param menuMap the map
 * @param locid the id - will canonicalize
 * @return the display name - or else the id
 */
LocaleMap.prototype.getLocaleName = function getLocaleName(locid) {
  locid = this.canonicalizeLocaleId(locid);
  var bund = this.getLocaleInfo(locid);
  if (bund && bund.name) {
    return bund.name;
  } else {
    return locid;
  }
};

/**
 * Return the locale name,
 *
 * @param menuMap the map
 * @param locid the id - will canonicalize
 * @return the display name - or else the id
 */
LocaleMap.prototype.getRegionAndOrVariantName = function getRegionAndOrVariantName(
  locid
) {
  locid = this.canonicalizeLocaleId(locid);
  var bund = this.getLocaleInfo(locid);
  if (bund) {
    var ret = "";
    if (bund.name_rgn) {
      ret = ret + bund.name_rgn;
    }
    if (bund.name_var) {
      ret = ret + " (" + bund.name_var + ")";
    }
    if (ret != "") {
      return ret; // region OR variant OR both
    }
    if (bund.name) {
      return bund.name; // fallback to name
    }
  }
  return locid; // fallbcak to locid
};

/**
 * Return the locale language
 *
 * @param locid
 * @returns the language portion
 */
LocaleMap.prototype.getLanguage = function getLanguage(locid) {
  return locid.split("_")[0].split("-")[0];
};

export { LocaleMap };

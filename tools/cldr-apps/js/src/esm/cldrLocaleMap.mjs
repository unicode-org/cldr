import * as cldrLoad from "./cldrLoad.mjs";
import * as cldrStatus from "./cldrStatus.mjs";

/**
 * @param aLocmap the map object from json
 */
class LocaleMap {
  constructor(aLocmap) {
    this.locmap = aLocmap;
  }

  /**
   * Run the locale id through the idmap.
   *
   * @param menuMap the map
   * @param locid or null for cldrStatus.getCurrentLocale()
   * @return canonicalized id, or unchanged
   */
  canonicalizeLocaleId(locid) {
    if (!locid) {
      locid = cldrStatus.getCurrentLocale();
      if (!locid) {
        return null;
      }
    }
    if (this.locmap?.idmap && this.locmap.idmap[locid]) {
      locid = this.locmap.idmap[locid]; // canonicalize
    }
    return locid;
  }

  /**
   * Linkify text like '@de' into some link to German.
   *
   * @param str (html)
   * @return linkified str (html)
   */
  linkify(str) {
    let out = "";
    const re = /@([a-zA-Z0-9_]+)/g;
    let match;
    let fromLast = 0;
    while ((match = re.exec(str)) != null) {
      const bund = this.getLocaleInfo(match[1]);
      if (bund) {
        out += str.substring(fromLast, match.index); // pre match
        if (match[1] == cldrStatus.getCurrentLocale()) {
          out += this.getLocaleName(match[1]);
        } else {
          out +=
            "<a href='" +
            cldrLoad.linkToLocale(match[1]) +
            "' title='" +
            match[1] +
            "'>" +
            this.getLocaleName(match[1]) +
            "</a>";
        }
      } else {
        out += match[0]; // no link.
      }
      fromLast = re.lastIndex;
    }
    return out + str.substring(fromLast, str.length);
  }

  /**
   * Return the locale info entry
   *
   * @param locid the id - should already be canonicalized
   * @return the bundle or null
   */
  getLocaleInfo(locid) {
    if (this.locmap?.locales && this.locmap.locales[locid]) {
      return this.locmap.locales[locid];
    } else {
      return null;
    }
  }

  /**
   * Return the locale name
   *
   * @param locid the id - will canonicalize
   * @return the display name - or else the id
   */
  getLocaleName(locid) {
    locid = this.canonicalizeLocaleId(locid);
    const bund = this.getLocaleInfo(locid);
    return bund?.name ? bund.name : locid;
  }

  /**
   * Return the locale name
   *
   * @param locid the id - will canonicalize
   * @return the display name - or else the id
   */
  getRegionAndOrVariantName(locid) {
    locid = this.canonicalizeLocaleId(locid);
    const bund = this.getLocaleInfo(locid);
    if (bund) {
      let ret = "";
      if (bund.name_rgn) {
        ret += bund.name_rgn;
      }
      if (bund.name_var) {
        ret += " (" + bund.name_var + ")";
      }
      if (ret) {
        return ret; // region OR variant OR both
      }
      if (bund.name) {
        return bund.name; // fallback to name
      }
    }
    return locid; // fallback to locid
  }

  /**
   * Return the locale language
   *
   * @param locid
   * @returns the language portion
   */
  getLanguage(locid) {
    return locid.split("_")[0].split("-")[0];
  }
}

export { LocaleMap };

import * as cldrEvent from "./esm/cldrEvent.js";
import * as cldrLoad from "./esm/cldrLoad.js";
import * as cldrStatus from "./esm/cldrStatus.js";
import * as cldrSurvey from "./esm/cldrSurvey.js";

function getCldrOpts() {
  const locale = cldrStatus.getCurrentLocale();
  const locmap = cldrLoad.getTheLocaleMap();
  let localeDir = null;
  if (locale) {
    const localeInfo = locmap.getLocaleInfo(locale);
    if (localeInfo) {
      localeDir = localeInfo.dir;
    }
  }
  return {
    // modules
    cldrLoad,
    cldrEvent, // Vue could call into these, if need be
    cldrStatus,
    cldrSurvey,

    // additional variables
    locale,
    locmap,
    localeDir,
    sessionId: cldrStatus.getSessionId(),
  };
}

export { getCldrOpts };

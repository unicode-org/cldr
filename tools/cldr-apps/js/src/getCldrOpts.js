import * as cldrEvent from "./esm/cldrEvent.js";
import * as cldrLoad from "./esm/cldrLoad.js";
import * as cldrStatus from "./esm/cldrStatus.js";
import * as cldrSurvey from "./esm/cldrSurvey.js";

function getCldrOpts() {
  const locale = cldrStatus.getCurrentLocale();
  const locmap = cldrLoad.getTheLocaleMap();
  const localeDir = cldrLoad.getLocaleDir(locale);
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

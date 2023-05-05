import * as cldrEvent from "./esm/cldrEvent.mjs";
import * as cldrLoad from "./esm/cldrLoad.mjs";
import * as cldrStatus from "./esm/cldrStatus.mjs";
import * as cldrSurvey from "./esm/cldrSurvey.mjs";

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

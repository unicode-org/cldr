/**
 * cldrCoverageReset: encapsulate Survey Tool code for auto-resetting coverage
 */
import * as cldrCoverage from "./cldrCoverage.mjs";
import * as cldrGui from "./cldrGui.mjs";
import * as cldrLoad from "./cldrLoad.mjs";
import * as cldrMenu from "./cldrMenu.mjs";
import * as cldrNotify from "./cldrNotify.mjs";
import * as cldrText from "./cldrText.mjs";

const TEST_DANGER = false && cldrStatus.getIsUnofficial(); // false for production, true only when testing

const MILLIS_UNTIL_RESET_COVERAGE = TEST_DANGER
  ? 10 * 1000 /* ten seconds = 10,000 milliseconds */
  : 10 * 60 * 60 * 1000; /* ten hours = 36,000,000 milliseconds */

let millisWhenReset = 0;

function resetIfLongSinceAction(millisSinceAction) {
  if (cldrCoverage.getSurveyUserCov()) {
    if (parseInt(millisSinceAction) > MILLIS_UNTIL_RESET_COVERAGE) {
      const now = new Date().getTime();
      if (now > millisWhenReset + MILLIS_UNTIL_RESET_COVERAGE) {
        millisWhenReset = now;
        cldrMenu.setCoverageLevel("auto");
        cldrLoad.handleCoverageChanged();
        cldrGui.updateWithStatus();
        cldrGui.updateWidgetsWithCoverage();
        cldrNotify.open(
          cldrText.get("coverage_reset_msg"),
          cldrText.get("coverage_reset_desc")
        );
      }
    }
  }
}

export { resetIfLongSinceAction };

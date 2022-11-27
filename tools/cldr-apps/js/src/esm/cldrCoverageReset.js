/**
 * cldrCoverageReset: encapsulate Survey Tool code for auto-resetting coverage
 */
import * as cldrCoverage from "../esm/cldrCoverage.js";
import * as cldrGui from "../esm/cldrGui.js";
import * as cldrLoad from "../esm/cldrLoad.js";
import * as cldrMenu from "../esm/cldrMenu.js";
import * as cldrText from "../esm/cldrText.js";

import { notification } from "ant-design-vue";

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
        notification.open({
          message: cldrText.get("coverage_reset_msg"),
          description: cldrText.get("coverage_reset_desc"),
          duration: 10,
        });
      }
    }
  }
}

export { resetIfLongSinceAction };

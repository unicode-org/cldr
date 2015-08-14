package org.unicode.cldr.tool;

import org.unicode.cldr.util.CldrUtility;

/**
 * Constants specific to CLDR tools.
 * Not to be used with the Survey Tool.
 * Moved here from CldrUtilities
 * @author srl
 *
 */
public class ToolConstants {

    // We are now having charts point to the appropriate source, so this may take some tweaking!
    public enum ChartStatus {
        beta, // at the start of the release
        trunk, // before the release is tagged
        release // for release version
    }

    // Change the following for each release depending on the phase

    private static final String DEFAULT_CHART_VERSION = "28";
    private static final String PREVIOUS_DEFAULT_CHART_VERSION = "27";
    private static final ChartStatus DEFAULT_CHART_STATUS = ChartStatus.beta;

    // DON'T CHANGE ANY OF THE FOLLOWING; THEY ARE DRIVEN BY THE ABOVE

    // allows overriding with -D
    public static final String CHART_VERSION = CldrUtility.getProperty("CHART_VERSION", DEFAULT_CHART_VERSION);
    public static final ChartStatus CHART_STATUS = ChartStatus.valueOf(CldrUtility.getProperty("CHART_STATUS", DEFAULT_CHART_STATUS.toString()));
    
    // build from the above
    public static final boolean BETA = CHART_STATUS == ChartStatus.beta;
    public static final String LAST_CHART_VERSION = Integer.parseInt(CHART_VERSION) + ".0"; // must have 1 decimal
    public static final String PREVIOUS_CHART_VERSION = Integer.parseInt(PREVIOUS_DEFAULT_CHART_VERSION) + ".0";
    public static final String CHART_DISPLAY_VERSION = CHART_VERSION + (BETA ? "β" : "");
    public static final String CHART_SOURCE = "http://unicode.org/repos/cldr/"
        + (CHART_STATUS != ChartStatus.release ? "trunk/" : "tags/release-" + CHART_VERSION + "/");
}

package org.unicode.cldr.tool;

import java.util.List;

import org.unicode.cldr.util.CldrUtility;

import com.google.common.collect.ImmutableList;

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

    private static final String DEFAULT_CHART_VERSION = "35";
    public static final String CHART_DISPLAY_VERSION = "35.1β";
    public static final String LAST_RELEASE_VERSION = "34.0";
    private static final ChartStatus DEFAULT_CHART_STATUS = ChartStatus.beta;

    // DON'T CHANGE ANY OF THE FOLLOWING; THEY ARE DRIVEN BY THE ABOVE

    // allows overriding with -D
    public static final String CHART_VERSION = CldrUtility.getProperty("CHART_VERSION", DEFAULT_CHART_VERSION);
    public static final String LAST_CHART_VERSION = Integer.parseInt(CHART_VERSION) + ".0"; // must have 1 decimal
    public static final ChartStatus CHART_STATUS = !CHART_VERSION.equals(DEFAULT_CHART_VERSION) ? ChartStatus.release
        : ChartStatus.valueOf(CldrUtility.getProperty("CHART_STATUS", DEFAULT_CHART_STATUS.toString()));

    // build from the above
    public static final boolean BETA = CHART_STATUS == ChartStatus.beta;
    public static final String CHART_SOURCE = "http://unicode.org/repos/cldr/"
        + (CHART_STATUS != ChartStatus.release ? "trunk/" : "tags/release-" + CHART_VERSION + "/");

    public static final List<String> CLDR_VERSIONS = ImmutableList.of(
        "1.1.1",
        "1.2.0",
        "1.3.0",
        "1.4.1",
        "1.5.1",
        "1.6.1",
        "1.7.2",
        "1.8.1",
        "1.9.1",
        "2.0.1",
        "21.0",
        "22.1",
        "23.1",
        "24.0",
        "25.0",
        "26.0",
        "27.0",
        "28.0",
        "29.0",
        "30.0",
        "31.0",
        "32.0",
        "33.0",
        "34.0"
    // add to this once the release is final!
    );
    public static final String PREVIOUS_CHART_VERSION;
    static {
        String last = "";
        for (String current : CLDR_VERSIONS) {
            if (current.equals(LAST_CHART_VERSION)) {
                break;
            }
            last = current;
        }
        PREVIOUS_CHART_VERSION = last;
    }
}

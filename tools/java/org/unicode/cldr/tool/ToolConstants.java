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

    public static final String CHART_VERSION = "25";
    public static final String LAST_CHART_VERSION = "24.0"; // must have 1 decimal
    public static final String CHART_DISPLAY_VERSION = CHART_VERSION + (CldrUtility.BETA ? "β" : "");

}

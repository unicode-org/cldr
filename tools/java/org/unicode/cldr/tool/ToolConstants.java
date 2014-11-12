package org.unicode.cldr.tool;

/**
 * Constants specific to CLDR tools.
 * Not to be used with the Survey Tool.
 * Moved here from CldrUtilities
 * @author srl
 *
 */
public class ToolConstants {
    public static final boolean BETA = true;

    public static final String CHART_VERSION = "27";
    public static final String LAST_CHART_VERSION = "26.0"; // must have 1 decimal
    public static final String CHART_DISPLAY_VERSION = CHART_VERSION + (BETA ? "β" : "");

}

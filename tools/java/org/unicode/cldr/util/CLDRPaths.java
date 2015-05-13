package org.unicode.cldr.util;

import org.unicode.cldr.tool.ToolConstants;

/**
 * Constant paths (moved here from CldrUtility).
 * These must not be used by any code destined for the SurveyTool, as this class will not be included.
 * @author srl
 *
 */

public class CLDRPaths {
    /** default working directory for Eclipse is . = ${workspace_loc:cldr}, which is <CLDR>/tools/java/ */
    // set the base directory with -Dcldrdata=<value>
    // if the main is different, use -Dcldrmain=<value>

    public static final String BASE_DIRECTORY = CldrUtility.getPath(CldrUtility.getProperty("CLDR_DIR", null)); // new
    // File(Utility.getProperty("CLDR_DIR",
    // null)).getPath();
    // // get up to
    // <CLDR>
    public static final String COMMON_DIRECTORY = CldrUtility.getPath(BASE_DIRECTORY, "common/");
    public static final String COLLATION_DIRECTORY = CldrUtility.getPath(COMMON_DIRECTORY, "collation/");
    public static final String MAIN_DIRECTORY = CldrUtility.getProperty("CLDR_MAIN",
        CldrUtility.getPath(CLDRPaths.COMMON_DIRECTORY, "main"));
    public static final String SEED_DIRECTORY = CldrUtility.getProperty("CLDR_SEED",
        CldrUtility.getPath(CLDRPaths.COMMON_DIRECTORY, "../seed/main"));
    public static final String SEED_COLLATION_DIRECTORY = CldrUtility.getPath(SEED_DIRECTORY, "../collation/");
    public static final String EXEMPLARS_DIRECTORY = CldrUtility.getPath(CLDRPaths.BASE_DIRECTORY, "exemplars/main/");
    public static final String RBNF_DIRECTORY = CldrUtility.getPath(CLDRPaths.COMMON_DIRECTORY, "rbnf/");
    public static final String TMP_DIRECTORY = CldrUtility.getPath(CldrUtility.getProperty("CLDR_TMP_DIR",
        CldrUtility.getPath(BASE_DIRECTORY, "../cldr-tmp/")));
    public static final String AUX_DIRECTORY = CldrUtility.getPath(CldrUtility.getProperty("CLDR_TMP_DIR",
        CldrUtility.getPath(BASE_DIRECTORY, "../cldr-aux/")));
    public static final String TMP2_DIRECTORY = CldrUtility.getPath(CldrUtility.getProperty("CLDR_TMP_DIR",
        CldrUtility.getPath(BASE_DIRECTORY, "../cldr-tmp2/")));
    // external data
    public static final String EXTERNAL_DIRECTORY = CldrUtility.getPath(CldrUtility.getProperty("UCD_DIR", BASE_DIRECTORY) + "/../");
    public static final String ARCHIVE_DIRECTORY = CldrUtility.getPath(CldrUtility.getProperty("ARCHIVE", BASE_DIRECTORY));
    public static final String LAST_DIRECTORY = ARCHIVE_DIRECTORY + "cldr-" +
        ToolConstants.LAST_CHART_VERSION +
        "/";
    public static final String GEN_DIRECTORY = CldrUtility.getPath(CldrUtility.getProperty("CLDR_GEN_DIR",
        CldrUtility.getPath(EXTERNAL_DIRECTORY, "Generated/cldr/")));
    public static final String DATA_DIRECTORY = CldrUtility.getPath(CldrUtility.getProperty("CLDR_EXT_DATA_DIR",
        CldrUtility.getPath(GEN_DIRECTORY, "../../Data/")));
    public static final String ICU_DATA_DIR = CldrUtility.getPath(CldrUtility.getProperty("ICU_DATA_DIR", null)); // eg
    public static final String BIRTH_DATA_DIR = CldrUtility.getPath(BASE_DIRECTORY, "tools/java/org/unicode/cldr/util/data/births/");
    /**
     * @deprecated please use XMLFile and CLDRFILE getSupplementalDirectory()
     * @see DEFAULT_SUPPLEMENTAL_DIRECTORY
     */
    public static final String SUPPLEMENTAL_DIRECTORY = CldrUtility.getPath(COMMON_DIRECTORY, "supplemental/");
    /**
     * Only the default, if no other directory is specified.
     */
    public static final String DEFAULT_SUPPLEMENTAL_DIRECTORY = CldrUtility.getPath(COMMON_DIRECTORY, "supplemental/");
    public static final String CHART_DIRECTORY = CldrUtility.getPath(AUX_DIRECTORY + "charts/", ToolConstants.CHART_VERSION);
    public static final String LOG_DIRECTORY = CldrUtility.getPath(TMP_DIRECTORY, "logs/");
    public static final String TEST_DIR = CldrUtility.getPath(CLDRPaths.BASE_DIRECTORY, "test/");
    /** If the generated BAT files are to work, this needs to be set right */
    public static final String COMPARE_PROGRAM = "\"C:\\Program Files (x86)\\Compare It!\\wincmp3.exe\"";
    /**
     * @deprecated Don't use this from any code that is run from the .JAR (SurveyTool, tests, etc).
     *             If it must be used, add a comment next to the usage to explain why it is needed.
     */
    public static final String UTIL_DATA_DIR = FileReaders.getRelativeFileName(
        CldrUtility.class, "data/");

}

package org.unicode.cldr.util;

import java.util.List;

import org.unicode.cldr.tool.ToolConstants;

import com.google.common.collect.ImmutableList;

/**
 * Constant paths (moved here from CldrUtility).
 * These must not be used by any code destined for the SurveyTool, as this class will not be included.
 * @author srl
 *
 */

public class CLDRPaths {
    private static final String CASING_SUBDIR = "casing/";
    private static final String VALIDITY_SUBDIR = "validity/";
    private static final String ANNOTATIONS_DERIVED_SUBDIR = "annotationsDerived/";
    private static final String COLLATION_SUBDIR = "collation/";
    private static final String RBNF_SUBDIR = "rbnf/";
    private static final String TRANSFORMS_SUBDIR = "transforms/";
    private static final String MAIN_SUBDIR = "main/";
    private static final String SUBDIVISIONS_SUBDIR = "subdivisions/";
    private static final String ANNOTATIONS_SUBDIR = "annotations/";
    
    /** default working directory for Eclipse is . = ${workspace_loc:cldr}, which is <CLDR>/tools/java/ */
    // set the base directory with -Dcldrdata=<value>
    // if the main is different, use -Dcldrmain=<value>

    public static final String BASE_DIRECTORY = CldrUtility.getPath(CldrUtility.getProperty("CLDR_DIR", null)); // new
    public static final String LOCAL_DIRECTORY = CldrUtility.getPath(CldrUtility.getProperty("LOCAL_DIR", BASE_DIRECTORY + "/../"));
    public static final String EXTERNAL_DIRECTORY = CldrUtility.getPath(CldrUtility.getProperty("UCD_DIR", LOCAL_DIRECTORY));
    // File(Utility.getProperty("CLDR_DIR",
    // null)).getPath();
    // // get up to
    // <CLDR>
    public static final String COMMON_DIRECTORY = CldrUtility.getPath(BASE_DIRECTORY, "common/");
    public static final String COLLATION_DIRECTORY = CldrUtility.getPath(COMMON_DIRECTORY, COLLATION_SUBDIR);
    public static final String CASING_DIRECTORY = CldrUtility.getPath(COMMON_DIRECTORY, CASING_SUBDIR);
    public static final String MAIN_DIRECTORY = CldrUtility.getProperty("CLDR_MAIN",
        CldrUtility.getPath(CLDRPaths.COMMON_DIRECTORY, MAIN_SUBDIR));

    public static final String RBNF_DIRECTORY = CldrUtility.getPath(CLDRPaths.COMMON_DIRECTORY, RBNF_SUBDIR);
    public static final String TRANSFORMS_DIRECTORY = CldrUtility.getPath(CLDRPaths.COMMON_DIRECTORY, TRANSFORMS_SUBDIR);
    public static final String ANNOTATIONS_DIRECTORY = CldrUtility.getPath(CLDRPaths.COMMON_DIRECTORY, ANNOTATIONS_SUBDIR);
    public static final String SUBDIVISIONS_DIRECTORY = CldrUtility.getPath(CLDRPaths.COMMON_DIRECTORY, SUBDIVISIONS_SUBDIR);
    public static final String ANNOTATIONS_DERIVED_DIRECTORY = CldrUtility.getPath(CLDRPaths.COMMON_DIRECTORY, ANNOTATIONS_DERIVED_SUBDIR);
    public static final String VALIDITY_DIRECTORY = CldrUtility.getPath(CLDRPaths.COMMON_DIRECTORY, VALIDITY_SUBDIR);

    public static final String SEED_DIRECTORY1 = CldrUtility.getProperty("CLDR_SEED",
        CldrUtility.getPath(CLDRPaths.COMMON_DIRECTORY, "../" + "seed/"));
    public static final String SEED_DIRECTORY = CldrUtility.getPath(SEED_DIRECTORY1, MAIN_SUBDIR);
    public static final String SEED_COLLATION_DIRECTORY = CldrUtility.getPath(SEED_DIRECTORY1, COLLATION_SUBDIR);
    public static final String SEED_CASING_DIRECTORY = CldrUtility.getPath(SEED_DIRECTORY1, CASING_SUBDIR);
    public static final String SEED_ANNOTATIONS_DIRECTORY = CldrUtility.getPath(SEED_DIRECTORY1, ANNOTATIONS_SUBDIR);

    public static final String EXEMPLARS_DIRECTORY = CldrUtility.getPath(CLDRPaths.BASE_DIRECTORY, "exemplars/" + MAIN_SUBDIR);

    public static final String TMP_DIRECTORY = CldrUtility.getPath(CldrUtility.getProperty("CLDR_TMP_DIR",
        CldrUtility.getPath(BASE_DIRECTORY, "../cldr-tmp/")));
    public static final String AUX_DIRECTORY = CldrUtility.getPath(CldrUtility.getProperty("CLDR_TMP_DIR",
        CldrUtility.getPath(BASE_DIRECTORY, "../cldr-aux/")));
    public static final String TMP2_DIRECTORY = CldrUtility.getPath(CldrUtility.getProperty("CLDR_TMP_DIR",
        CldrUtility.getPath(BASE_DIRECTORY, "../cldr-tmp2/")));
    // external data
    public static final String ARCHIVE_DIRECTORY = CldrUtility.getPath(CldrUtility.getProperty("ARCHIVE",
        CldrUtility.getPath(BASE_DIRECTORY, "../cldr-archive/")));
    public static final String LAST_DIRECTORY = ARCHIVE_DIRECTORY + "cldr-" +
        ToolConstants.PREVIOUS_CHART_VERSION +
        "/";
    public static final String LAST_RELEASE_DIRECTORY = ARCHIVE_DIRECTORY + "cldr-" +
        ToolConstants.LAST_RELEASE_VERSION +
        "/";
    public static final String CLDR_PRIVATE_DIRECTORY = CldrUtility.getPath(CldrUtility.getProperty("CLDR_PRIVATE_DATA",
        CldrUtility.getPath(BASE_DIRECTORY, "../cldr-private/")));

    public static final String LAST_COMMON_DIRECTORY = CldrUtility.getPath(CLDRPaths.LAST_DIRECTORY, "common/");
    public static final String LAST_TRANSFORMS_DIRECTORY = CldrUtility.getPath(CLDRPaths.LAST_COMMON_DIRECTORY, TRANSFORMS_SUBDIR);

    public static final String GEN_DIRECTORY = CldrUtility.getPath(CldrUtility.getProperty("CLDR_GEN_DIR",
        CldrUtility.getPath(LOCAL_DIRECTORY, "Generated/cldr/")));
    public static final String DATA_DIRECTORY = CldrUtility.getPath(CldrUtility.getProperty("CLDR_EXT_DATA_DIR",
        CldrUtility.getPath(LOCAL_DIRECTORY, "Data/")));

    public static final String UCD_DATA_DIRECTORY = CldrUtility.getPath(EXTERNAL_DIRECTORY + "unicodetools/draft/");
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

    public enum DIRECTORIES {
        common_dtd, common_properties, common_uca,

        common_bcp47(DtdType.ldmlBCP47),

        common_annotations(DtdType.ldml), common_casing(DtdType.ldml), common_collation(DtdType.ldml), common_main(DtdType.ldml), common_rbnf(
            DtdType.ldml), common_segments(DtdType.ldml), common_subdivisions(DtdType.ldml),

        common_supplemental(DtdType.supplementalData), common_transforms(DtdType.supplementalData), common_validity(DtdType.supplementalData),

        keyboards_android(DtdType.keyboard, DtdType.platform), keyboards_chromeos(DtdType.keyboard, DtdType.platform), keyboards_dtd(DtdType.keyboard,
            DtdType.platform), keyboards_osx(DtdType.keyboard,
                DtdType.platform), keyboards_und(DtdType.keyboard, DtdType.platform), keyboards_windows(DtdType.keyboard, DtdType.platform),
                ;

        public final List<DtdType> dtdType;

        private DIRECTORIES(DtdType... dtdType) {
            this.dtdType = ImmutableList.copyOf(dtdType);
        }
    }

    @Deprecated //use DtdType.ldml.directories
//    public static final Set<String> LDML_DIRECTORIES = ImmutableSet.of(
//        "main",
//        "annotations",
//        "casing",
//        "collation",
//        "rbnf",
//        "segments",
//        "subdivisions"
//        );
    public static final String UNICODE_VERSION = "10.0.0";
    public static final String TEST_DATA = COMMON_DIRECTORY + "testData/";
}

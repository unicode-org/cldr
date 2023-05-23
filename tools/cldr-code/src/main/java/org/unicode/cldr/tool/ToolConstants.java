package org.unicode.cldr.tool;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.ibm.icu.util.VersionInfo;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;

/**
 * Constants specific to CLDR tools. Not to be used with the Survey Tool. Moved here from
 * CldrUtilities
 *
 * @author srl
 */
public class ToolConstants {

    // We are now having charts point to the appropriate source, so this may take some tweaking!
    public enum ChartStatus {
        beta, // at the start of the release
        trunk, // before the release is tagged
        release // for release version
    }

    // TODO change this to CldrVersion, add add in the ShowLocaleCoverage years.
    public static final List<String> CLDR_VERSIONS =
            ImmutableList.of(
                    "1.1", "1.1.1", "1.2", "1.3", "1.4", "1.4.1", "1.5.0.1", "1.5.1", "1.6.1",
                    "1.7.2", "1.8.1", "1.9.1", "2.0.1", "21.0", "22.1", "23.1", "24.0", "25.0",
                    "26.0", "27.0", "28.0", "29.0", "30.0", "31.0", "32.0", "33.0", "33.1", "34.0",
                    "35.0", "35.1", "36.0", "36.1", "37.0", "38.0", "38.1", "39.0", "40.0", "41.0",
                    "42.0", "43.0"
                    // add to this once the release is final!
                    );
    public static final Set<VersionInfo> CLDR_VERSIONS_VI =
            ImmutableSet.copyOf(
                    CLDR_VERSIONS.stream()
                            .map(x -> VersionInfo.getInstance(x))
                            .collect(Collectors.toList()));

    public static final String DEV_VERSION = "44";
    public static final VersionInfo DEV_VERSION_VI = VersionInfo.getInstance(DEV_VERSION);

    public static final Set<String> CLDR_RELEASE_VERSION_SET =
            ImmutableSet.copyOf(ToolConstants.CLDR_VERSIONS);
    public static final Set<String> CLDR_RELEASE_AND_DEV_VERSION_SET =
            ImmutableSet.<String>builder()
                    .addAll(CLDR_RELEASE_VERSION_SET)
                    .add(DEV_VERSION)
                    .build();

    public static VersionInfo previousVersion(VersionInfo version) {
        VersionInfo last = null;
        for (VersionInfo current : CLDR_VERSIONS_VI) {
            if (current.equals(version)) {
                break;
            }
            last = current;
        }
        return last;
    }

    public static String previousVersion(String version, int minFields) {
        VersionInfo result = previousVersion(VersionInfo.getInstance(version));
        return result.getVersionString(minFields, 2);
    }

    public static String previousVersion(String version) {
        return previousVersion(version, 2);
    }

    public static String getBaseDirectory(VersionInfo vi) {
        if (vi.equals(DEV_VERSION_VI)) {
            return CLDRPaths.BASE_DIRECTORY;
        } else if (CLDR_VERSIONS_VI.contains(vi)) {
            return CLDRPaths.ARCHIVE_DIRECTORY + "cldr-" + vi.getVersionString(2, 3) + "/";
        } else {
            throw new IllegalArgumentException(
                    "not a known version: "
                            + vi.getVersionString(2, 2)
                            + ", must be in: "
                            + CLDR_VERSIONS_VI);
        }
    }

    public static String getBaseDirectory(String version) {
        VersionInfo vi = VersionInfo.getInstance(version);
        return getBaseDirectory(vi);
    }

    // allows overriding with -D
    public static final VersionInfo CHART_VI =
            VersionInfo.getInstance(CldrUtility.getProperty("CHART_VERSION", DEV_VERSION));
    public static final String CHART_VERSION = CHART_VI.getVersionString(1, 2);

    public static final String PREV_CHART_VERSION_RAW =
            CldrUtility.getProperty("PREV_CHART_VERSION", previousVersion(CHART_VERSION));
    public static final VersionInfo PREV_CHART_VI = VersionInfo.getInstance(PREV_CHART_VERSION_RAW);
    public static final String PREV_CHART_VERSION = PREV_CHART_VI.getVersionString(1, 2);
    public static final String PREV_CHART_VERSION_WITH0 =
            PREV_CHART_VI.getVersionString(2, 2); // must have 1 decimal

    public static final ChartStatus CHART_STATUS =
            ChartStatus.valueOf(
                    CldrUtility.getProperty(
                            "CHART_STATUS",
                            CLDR_VERSIONS_VI.contains(CHART_VI) ? "release" : "beta"));
    public static final boolean BETA = CHART_STATUS == ChartStatus.beta;

    // DON'T CHANGE ANY OF THE FOLLOWING DEFINITIONS; THEY ARE DRIVEN BY THE ABOVE

    public static final String CHART_DISPLAY_VERSION =
            CHART_VI.getVersionString(2, 2) + (BETA ? "β" : "");

    public static final String LAST_RELEASE_VERSION = CLDR_VERSIONS.get(CLDR_VERSIONS.size() - 1);
    public static final VersionInfo LAST_RELEASE_VI = VersionInfo.getInstance(LAST_RELEASE_VERSION);
    public static final String LAST_RELEASE_VERSION_WITH0 =
            LAST_RELEASE_VI.getVersionString(2, 2); // must have 1 decimal

    // public static final String CHART_SOURCE_DIRECTORY = CLDR_VERSIONS.contains(CHART_VERSION) ?
    // ""

    public static final String CHART_SOURCE =
            "http://unicode.org/repos/cldr/"
                    + (CHART_STATUS != ChartStatus.release
                            ? "trunk/"
                            : "tags/release-" + CHART_VERSION + "/");
}

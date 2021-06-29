package org.unicode.cldr.tool;

import java.io.File;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.ibm.icu.impl.locale.XCldrStub.ImmutableMap;
import com.ibm.icu.util.VersionInfo;

/**
 * Enums that should exactly match what is in cldr-archive; eg, v2_0_1 means that there is a folder "cldr-2.0.1"
 * @author markdavis
 *
 */
// TODO compute the VersionInfo for each at creation time, and stash as field.
public enum CldrVersion {
    unknown,
    v1_1_1, v1_2, v1_3, v1_4_1, v1_5_1, v1_6_1, v1_7_2, v1_8_1, v1_9_1, v2_0_1,
    v21_0, v22_1, v23_1, v24_0, v25_0, v26_0, v27_0, v28_0, v29_0, v30_0, v31_0, v32_0, v33_0, v33_1, v34_0,
    v35_0, v35_1, v36_0, v36_1, v37_0, v38_0, v38_1, v39_0,
    baseline;

    private final String baseDirectory;
    private final String dotName;
    private final VersionInfo versionInfo;

    /**
     * Get the closest available version (successively dropping lower-significance values)
     * We do this because the archive might contain a dot-dot version
     * but have a folder called by the round(er) version number.
     */
    public static CldrVersion from(VersionInfo versionInfo) {
        if (versionInfo == null) {
            return unknown;
        }
        while (true) {
            CldrVersion result = versionInfoToCldrVersion.get(versionInfo);
            if (result != null) {
                return result;
            }
            versionInfo = versionInfo.getMilli() != 0 ? VersionInfo.getInstance(versionInfo.getMajor(), versionInfo.getMinor())
                    : versionInfo.getMinor() != 0 ? VersionInfo.getInstance(versionInfo.getMajor())
                        : unknown.versionInfo; // will always terminate with unknown.
        }
    }

    public static CldrVersion from(String versionString) {
        return valueOf(versionString.charAt(0) < 'A' ? "v" + versionString.replace('.', '_') : versionString);
    }

    public VersionInfo getVersionInfo() {
        return versionInfo;
    }
    @Override
    public String toString() {
        return dotName;
    }
    public String getBaseDirectory() {
        return baseDirectory;
    }

    public boolean isOlderThan(CldrVersion other) {
        return compareTo(other) < 0;
    }

    private CldrVersion() {
        String oldName = name();
        if (oldName.charAt(0) == 'v') {
            dotName = oldName.substring(1).replace('_', '.');
            versionInfo = VersionInfo.getInstance(dotName);
            baseDirectory = CLDRPaths.ARCHIVE_DIRECTORY + "cldr-" + toString() + "/";
        } else {
            dotName = oldName;
            baseDirectory = CLDRPaths.BASE_DIRECTORY;
            SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();
            versionInfo = "baseline".equals(oldName) ? sdi.getCldrVersion() : VersionInfo.getInstance(0);
        }
    }

    public static final CldrVersion LAST_RELEASE_VERSION = values()[values().length-2];
    public static final List<CldrVersion> CLDR_VERSIONS_ASCENDING;
    public static final List<CldrVersion> CLDR_VERSIONS_DESCENDING;
    private static final Map<VersionInfo, CldrVersion> versionInfoToCldrVersion;
    static {
        EnumSet<CldrVersion> temp = EnumSet.allOf(CldrVersion.class);
        CLDR_VERSIONS_ASCENDING = ImmutableList.copyOf(temp);
        CLDR_VERSIONS_DESCENDING = ImmutableList.copyOf(Lists.reverse(CLDR_VERSIONS_ASCENDING));
        Map<VersionInfo, CldrVersion> temp2 = new LinkedHashMap<>();
        for (CldrVersion item : CLDR_VERSIONS_ASCENDING) {
            VersionInfo version2 = item.versionInfo;
            temp2.put(version2, item);
            if (version2.getMilli() != 0) {
                version2 = VersionInfo.getInstance(version2.getMajor(), version2.getMinor());
                if (!temp2.containsKey(version2)) {
                    temp2.put(version2, item);
                }
            }
            if (version2.getMinor() != 0) {
                version2 = VersionInfo.getInstance(version2.getMajor());
                if (!temp2.containsKey(version2)) {
                    temp2.put(version2, item);
                }
            }
        }
        versionInfoToCldrVersion = ImmutableMap.copyOf(temp2);
    }

    public List<File> getPathsForFactory() {
        return ImmutableList.copyOf(versionInfo != null && versionInfo.getMajor() < 27
            ? new File[] { new File(getBaseDirectory() + "common/main/") }
        : new File[] {
            new File(getBaseDirectory() + "common/main/"),
            new File(getBaseDirectory() + "common/annotations/") });
    }

    /**
     * For testing
     */
    public static void checkVersions() {
//        System.out.println(Arrays.asList(CldrVersion.values()));

        Set<VersionInfo> allFileVersions = new TreeSet<>();
        Set<VersionInfo> allTc = new TreeSet<>();
        Set<VersionInfo> missingEnums = new TreeSet<>();
        Set<CldrVersion> extraEnums = EnumSet.copyOf(CLDR_VERSIONS_ASCENDING);
        extraEnums.remove(CldrVersion.baseline);
        extraEnums.remove(CldrVersion.unknown);

        for (String subdir : new File(CLDRPaths.ARCHIVE_DIRECTORY).list()) {
            if (subdir.startsWith("cldr-")) {
                String versionString = subdir.substring("cldr-".length());
                VersionInfo versionInfo = VersionInfo.getInstance(versionString);
                allFileVersions.add(versionInfo);
                try {
                    CldrVersion found = CldrVersion.from(versionString);
                    extraEnums.remove(found);
                } catch (Exception e) {
                    missingEnums.add(versionInfo);
                }
            }
        }
        Set<String> errorMessages = new LinkedHashSet<>();

        // get versions from ToolConstants
        for (String tc : ToolConstants.CLDR_VERSIONS) {
            VersionInfo versionInfo = VersionInfo.getInstance(tc);
            allTc.add(versionInfo);
        }
        // same?
        if (!allTc.equals(allFileVersions)) {
            LinkedHashSet<VersionInfo> tcMFile = new LinkedHashSet<>(allTc);
            tcMFile.removeAll(allFileVersions);
            if (!tcMFile.isEmpty()) {
                errorMessages.add("Extra ToolConstants.CLDR_VERSIONS compared to " + CLDRPaths.ARCHIVE_DIRECTORY + ": " + tcMFile);
            }
            LinkedHashSet<VersionInfo> fileMTc = new LinkedHashSet<>(allFileVersions);
            fileMTc.removeAll(allTc);
            if (!fileMTc.isEmpty()) {
                errorMessages.add("Extra folders in " + CLDRPaths.ARCHIVE_DIRECTORY + " compared to ToolConstants.CLDR_VERSIONS: " + fileMTc);
            }
        }

        // Are there extra enums complete?
        if (!extraEnums.isEmpty()) {
            errorMessages.add("Extra enums compared to " + CLDRPaths.ARCHIVE_DIRECTORY + ": " + extraEnums);
        }
        // Is the archive complete?
        if (!missingEnums.isEmpty()) {
            StringBuilder temp = new StringBuilder();
            allFileVersions.forEach(v -> temp.append(", v" + v.getVersionString(2, 4).replace('.', '_')));
            errorMessages.add("Missing enums " + missingEnums + ", should be:\ntrunk" + temp + ", unknown");
        }
        if (!errorMessages.isEmpty()) {
            throw new IllegalArgumentException(errorMessages.toString());
        }
    }
}
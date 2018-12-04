package org.unicode.cldr.tool;

import java.io.File;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.ibm.icu.impl.locale.XCldrStub.ImmutableMap;
import com.ibm.icu.util.VersionInfo;

public enum CldrVersion {
    unknown, 
    v1_1_1, v1_2, v1_3, v1_4_1, v1_5_1, v1_6_1, v1_7_2, v1_8_1, v1_9_1, v2_0_1, 
    v21_0, v22_1, v23_1, v24_0, v25_0, v26_0, v27_0, v28_0, v29_0, v30_0, v31_0, v32_0, v33_0, v33_1, v34_0, 
    trunk;

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
    };

    public static CldrVersion from(String versionString) {
        return valueOf(versionString.charAt(0) < 'A' ? "v" + versionString.replace('.', '_') : versionString);
    };

    public VersionInfo getVersionInfo() {
        return versionInfo;
    }
    public String toString() {
        return dotName;
    };
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
            versionInfo = "trunk".equals(oldName) ? VersionInfo.getInstance(35) : VersionInfo.getInstance(0);
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

        // trunk version ok
        SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();
        if (!Objects.equal(CldrVersion.trunk.getVersionInfo(), sdi.getCldrVersion())) {
            throw new IllegalArgumentException("Trunk version incorrect: " + CldrVersion.trunk.getVersionInfo() + ", " + sdi.getCldrVersion());
        }

        Set<VersionInfo> all = new TreeSet<>();
        Set<VersionInfo> missingEnums = new TreeSet<>();
        Set<CldrVersion> extraEnums = EnumSet.copyOf(CLDR_VERSIONS_ASCENDING);
        extraEnums.remove(CldrVersion.trunk);
        extraEnums.remove(CldrVersion.unknown);

        for (String subdir : new File(CLDRPaths.ARCHIVE_DIRECTORY).list()) {
            if (subdir.startsWith("cldr-")) {
                String versionString = subdir.substring("cldr-".length());
                VersionInfo versionInfo = VersionInfo.getInstance(versionString);
                all.add(versionInfo);
                try {
                    CldrVersion found = CldrVersion.from(versionString);
                    extraEnums.remove(found);
                } catch (Exception e) {
                    missingEnums.add(versionInfo);
                }
            }
        }
        // Is the archive complete?
        if (!extraEnums.isEmpty()) {
            throw new IllegalArgumentException("Extra enums compared to " + CLDRPaths.ARCHIVE_DIRECTORY + ": " + extraEnums);
        }
        if (!missingEnums.isEmpty()) {
            StringBuilder temp = new StringBuilder();
            all.forEach(v -> temp.append(", v" + v.getVersionString(2, 4).replace('.', '_')));
            throw new IllegalArgumentException("Missing enums " + missingEnums + ", should be:\ntrunk" + temp + ", unknown");
        }
        // Does it match ToolConstants?

    }
}
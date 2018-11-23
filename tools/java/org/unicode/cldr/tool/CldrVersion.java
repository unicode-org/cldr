package org.unicode.cldr.tool;

import java.io.File;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.util.CLDRPaths;

import com.google.common.collect.Ordering;
import com.ibm.icu.util.VersionInfo;

public enum CldrVersion {
    trunk,
    v34_0, v33_1, v33_0, v32_0, v31_0, v30_0, v29_0, v28_0, v27_0, v26_0, v25_0, v24_0, v23_1, v22_1, v21_0,
    v2_0_1, v1_9_1, v1_8_1, v1_7_2, v1_6_1, v1_5_1, v1_4_1, v1_3, v1_2, v1_1_1,
    unknown;

    private final String baseDirectory;
    private final String dotName;

    public String toString() {
        return dotName;
    };
    public static CldrVersion from(VersionInfo versionInfo) {
        return valueOf("v" + versionInfo.getVersionString(2, 4).replace('.', '_'));
    };
    public static CldrVersion from(String versionString) {
        return valueOf(versionString.charAt(0) < 'A' ? "v" + versionString.replace('.', '_') : versionString);
    };
    public String getBaseDirectory() {
        return baseDirectory;
    }
    private CldrVersion() {
        String oldName = name();
        if (oldName.charAt(0) == 'v') {
            dotName = oldName.substring(1).replace('_', '.');
            baseDirectory = CLDRPaths.ARCHIVE_DIRECTORY + "cldr-" + toString() + "/";
        } else {
            dotName = oldName;
            baseDirectory = CLDRPaths.BASE_DIRECTORY;
        }
    }

    /**
     * For testing
     */
    public static void checkVersions() {
        Set<VersionInfo> all = new TreeSet<>(Ordering.natural().reversed());
        Set<VersionInfo> missing = new TreeSet<>(Ordering.natural().reversed());
        for (String subdir : new File(CLDRPaths.ARCHIVE_DIRECTORY).list()) {
            if (subdir.startsWith("cldr-")) {
                String versionString = subdir.substring("cldr-".length());
                VersionInfo versionInfo = VersionInfo.getInstance(versionString);
                all.add(versionInfo);
                try {
                    CldrVersion.from(versionString);
                } catch (Exception e) {
                    missing.add(versionInfo);
                }
            }
        }
        if (!missing.isEmpty()) {
            StringBuilder temp = new StringBuilder();
            all.forEach(v -> temp.append(", v" + v.getVersionString(2, 4).replace('.', '_')));
            throw new IllegalArgumentException("Missing items " + missing + ", should be:\ntrunk" + temp + ", unknown");
        }
    }
}
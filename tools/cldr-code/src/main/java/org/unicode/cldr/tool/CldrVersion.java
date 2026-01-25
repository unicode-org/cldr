package org.unicode.cldr.tool;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.ibm.icu.impl.locale.XCldrStub.ImmutableMap;
import com.ibm.icu.util.VersionInfo;
import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;

/**
 * Enums that should exactly match what is in cldr-archive; eg, v2_0_1 means that there is a folder
 * "cldr-2.0.1"
 *
 * @author markdavis
 * @see CheckoutArchive for a tool to automatically populate the cldr-archive
 */
// TODO merge with all other copies of the CLDR version and replace with supplemental metadata,
// CLDR-9149
public enum CldrVersion {
    unknown(""),
    v1_1("2004-06-08"),
    v1_1_1("2004-07-29"),
    v1_2("2004-11-04"),
    v1_3("2005-06-02"),
    v1_4("2006-07-17"),
    v1_4_1("2006-11-03"),
    v1_5_0_1("2007-07-31"),
    v1_5_1("2007-12-21"),
    v1_6_1("2008-07-23"),
    v1_7_2("2009-12-10"),
    v1_8_1("2010-04-29"),
    v1_9_1("2011-03-11"),
    v2_0_1("2011-07-18"),
    v21_0("2012-02-10"),
    v22_1("2012-10-26"),
    v23_1("2013-05-15"),
    v24_0("2013-09-18"),
    v25_0("2014-03-19"),
    v26_0("2014-09-18"),
    v27_0("2015-03-19"),
    v28_0("2015-09-17"),
    v29_0("2016-03-16"),
    v30_0("2016-10-05"),
    v31_0("2017-03-20"),
    v32_0("2017-11-01"),
    v33_0("2018-03-26"),
    v33_1("2018-06-20"),
    v34_0("2018-10-15"),
    v35_0("2019-03-27"),
    v35_1("2019-04-17"),
    v36_0("2019-10-04"),
    v36_1("2020-03-11"),
    v37_0("2020-04-23"),
    v38_0("2020-10-28"),
    v38_1("2020-12-17"),
    v39_0("2021-04-07"),
    v40_0("2021-10-27"),
    v41_0("2022-04-06"),
    v42_0("2022-10-19"),
    v43_0("2023-04-12"),
    v44_0("2023-10-31"),
    v44_1("2023-12-13"),
    v45_0("2024-04-17"),
    v46_0("2024-10-24"),
    v46_1("2024-12-18"),
    v47_0("2025-03-13"),
    v48_0("2025-10-29"),
    /**
     * @see CLDRFile#GEN_VERSION
     */
    baseline("");

    private final String baseDirectory;
    private final String dotName;
    private final VersionInfo versionInfo;
    private final Instant date;

    /**
     * Get the closest available version (successively dropping lower-significance values) We do
     * this because the archive might contain a dot-dot version but have a folder called by the
     * round(er) version number.
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
            versionInfo =
                    versionInfo.getMilli() != 0
                            ? VersionInfo.getInstance(
                                    versionInfo.getMajor(), versionInfo.getMinor())
                            : versionInfo.getMinor() != 0
                                    ? VersionInfo.getInstance(versionInfo.getMajor())
                                    : unknown.versionInfo; // will always terminate with unknown.
        }
    }

    public static CldrVersion from(String versionString) {
        // treat 'current' as baseline
        if (versionString.equals(CLDRFile.GEN_VERSION)
                || versionString.equals(CLDRFile.GEN_VERSION + ".0")) {
            return CldrVersion.baseline;
        }
        return valueOf(
                versionString.charAt(0) < 'A'
                        ? "v" + versionString.replace('.', '_')
                        : versionString);
    }

    public Instant getDate() {
        return date;
    }

    static final ZoneId Z = ZoneId.of("GMT");

    public int getYear() {
        return ZonedDateTime.ofInstant(date, Z).getYear();
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

    private CldrVersion(String date) {
        String oldName = name();
        if (oldName.charAt(0) == 'v') {
            dotName = oldName.substring(1).replace('_', '.');
            versionInfo = VersionInfo.getInstance(dotName);
            baseDirectory = CLDRPaths.ARCHIVE_DIRECTORY + "cldr-" + toString() + "/";
        } else {
            dotName = oldName;
            baseDirectory = CLDRPaths.BASE_DIRECTORY;
            final VersionInfo cldrVersion = VersionInfo.getInstance(CLDRFile.GEN_VERSION);
            versionInfo = "baseline".equals(oldName) ? cldrVersion : VersionInfo.getInstance(0);
        }
        this.date = date.isEmpty() ? null : Instant.parse(date + "T00:00:00Z");
    }

    public static final CldrVersion LAST_RELEASE_VERSION = values()[values().length - 2];
    public static final List<CldrVersion> CLDR_VERSIONS_ASCENDING;
    public static final List<CldrVersion> CLDR_VERSIONS_DESCENDING;
    private static final Map<VersionInfo, CldrVersion> versionInfoToCldrVersion;
    public static final List<CldrVersion> LAST_RELEASE_EACH_YEAR;

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

        List<CldrVersion> lastReleaseEachYear = new ArrayList<>();
        int lastYear = -1;
        ArrayList<CldrVersion> descending = new ArrayList<>(Arrays.asList(CldrVersion.values()));
        Collections.reverse(descending);
        for (CldrVersion v : descending) {
            if (v == CldrVersion.baseline || v == CldrVersion.unknown) {
                continue;
            }
            int year = v.getYear();
            if (year != lastYear) {
                lastReleaseEachYear.add(v);
                lastYear = year;
            }
        }
        LAST_RELEASE_EACH_YEAR = List.copyOf(lastReleaseEachYear);
    }

    public List<File> getPathsForFactory() {
        return ImmutableList.copyOf(
                versionInfo != null && versionInfo.getMajor() < 27
                        ? new File[] {new File(getBaseDirectory() + "common/main/")}
                        : new File[] {
                            new File(getBaseDirectory() + "common/main/"),
                            new File(getBaseDirectory() + "common/annotations/")
                        });
    }

    /** For testing */
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
                errorMessages.add(
                        "Extra ToolConstants.CLDR_VERSIONS compared to "
                                + CLDRPaths.ARCHIVE_DIRECTORY
                                + ": "
                                + tcMFile);
            }
            LinkedHashSet<VersionInfo> fileMTc = new LinkedHashSet<>(allFileVersions);
            fileMTc.removeAll(allTc);
            if (!fileMTc.isEmpty()) {
                errorMessages.add(
                        "Extra folders in "
                                + CLDRPaths.ARCHIVE_DIRECTORY
                                + " compared to ToolConstants.CLDR_VERSIONS: "
                                + fileMTc);
            }
        }

        // Are there extra enums complete?
        if (!extraEnums.isEmpty()) {
            errorMessages.add(
                    "Extra enums compared to " + CLDRPaths.ARCHIVE_DIRECTORY + ": " + extraEnums);
        }
        // Is the archive complete?
        if (!missingEnums.isEmpty()) {
            StringBuilder temp = new StringBuilder();
            allFileVersions.forEach(
                    v -> temp.append(", v" + v.getVersionString(2, 4).replace('.', '_')));
            errorMessages.add(
                    "Missing enums " + missingEnums + ", should be:\ntrunk" + temp + ", unknown");
        }
        if (!errorMessages.isEmpty()) {
            throw new IllegalArgumentException(errorMessages.toString());
        }
    }
}

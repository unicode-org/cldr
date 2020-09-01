package org.unicode.cldr.tool;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;

import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CLDRTool;
import org.unicode.cldr.util.PathUtilities;

@CLDRTool(alias = "CompareFilesBetweenReleases", description = "Print a report of which files changed since the last release")
public class CompareFilesBetweenReleases2 {

    //private static final String STAGING_DIRECTORY = CldrUtility.getPath(CLDRPaths.BASE_DIRECTORY, "../cldr-staging/production");
    private static final String RELEASE_DIRECTORY = CLDRPaths.ARCHIVE_DIRECTORY + "cldr-" + ToolConstants.LAST_RELEASE_VERSION + "/";

    public static void main(String[] args) throws IOException {
        final File stagingFile = new File(CLDRPaths.BASE_DIRECTORY);
        final File lastFile = new File(RELEASE_DIRECTORY);
        System.out.println("Comparing " + lastFile.getAbsolutePath() + " vs " + stagingFile.getAbsolutePath());
        Set<String> staging = getFiles(stagingFile, SKIP);
        Set<String> lastRelease = getFiles(lastFile, SKIP);

        // now, check common <-> seed
        Set<String> stagingCommon   = getFiles(new File(stagingFile, "common"), Collections.emptySet());
        Set<String> stagingSeed     = getFiles(new File(stagingFile, "seed"),   Collections.emptySet());
        Set<String> lastCommon      = getFiles(new File(lastFile,    "common"), Collections.emptySet());
        Set<String> lastSeed        = getFiles(new File(lastFile,    "seed"),   Collections.emptySet());

        Set<String> seedToCommon    = lastSeed.stream()
                                        .distinct()
                                        .filter(stagingCommon::contains)
                                        .map((String s) -> "/common" + s)
                                        .collect(Collectors.toCollection(() -> new TreeSet<String>()));
        Set<String> commonToSeed    = lastCommon.stream()
                                        .distinct()
                                        .filter(stagingSeed::contains)
                                        .map((String s) -> "/seed" + s)
                                        .collect(Collectors.toCollection(() -> new TreeSet<String>()));
        // this is like commonToSeed but has /common in it, for exclusion
        Set<String> commonToSeedExclude    = lastCommon.stream()
                                        .distinct()
                                        .filter(stagingSeed::contains)
                                        .map((String s) -> "/common" + s)
                                        .collect(Collectors.toCollection(() -> new TreeSet<String>()));
        if( !seedToCommon.isEmpty() ) {
            System.out.println("\nMoved from Seed to Common:\n");
            seedToCommon.forEach((final String f) -> System.out.println(f));
        }
        if( !commonToSeed.isEmpty() ) {
            System.out.println("\nMoved from Common to Seed:\n");
            commonToSeed.forEach((final String f) -> System.out.println(f));
        }

        System.out.println("\nIn master, but not "+ToolConstants.LAST_RELEASE_VERSION+":\n");
        showDiff(staging, lastRelease, seedToCommon);
        System.out.println("\nIn "+ToolConstants.LAST_RELEASE_VERSION+", but not master:\n");
        showDiff(lastRelease, staging, commonToSeedExclude);
    }

    private static void showDiff(Set<String> staging, Set<String> lastRelease, Set<String> skip) {
        Set<String> staging_release = new LinkedHashSet<>(staging);
        staging_release.removeAll(lastRelease);
        int skippedItems = 0;
        for (String file : staging_release) {
            if(skip.contains(file)) {
                skippedItems++;
                continue;
            }
            System.out.println(file);
        }
        if(skippedItems > 0) {
            System.out.println("(plus " + skippedItems + " skipped item(s) listed above)");
        }
    }

    private static Set<String> getFiles(File base, Set<String> skip) throws IOException {
        Set<String> result = new TreeSet<>();
        int baseLen = PathUtilities.getNormalizedPathString(base).length();
        getFiles(baseLen, base, result, skip);
        return result;
    }

    static final Set<String> SKIP = ImmutableSet.of("seed", "exemplars", "docs", "tools");

    private static void getFiles(int baseLen, File subdir, Set<String> names, Set<String> skip) throws IOException {
        String name = subdir.getName();
        if (subdir.isDirectory()) {
            if (skip.contains(name)
                || name.startsWith(".")) {
                return;
            }
            for (File file : subdir.listFiles()) {
                getFiles(baseLen, file, names, skip);
            }
        } else {
            // Only add files.
            if (name.startsWith(".")) {
                return;
            }
            String fullName = PathUtilities.getNormalizedPathString(subdir);
            names.add(fullName.substring(baseLen));
        }
    }
}

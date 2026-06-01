package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import java.io.File;
import java.util.regex.Matcher;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.tool.GenerateExampleDependencies;

public class TestCLDRPaths {
    public static String HAS_CLDR_ARCHIVE = "HAS_CLDR_ARCHIVE";

    @Test
    void TestCanUseArchiveDirectory() {
        if (!canUseArchiveDirectory()) {
            // We print this warning once as part of the unit tests.
            System.err.println(
                    String.format(
                            "WARNING: -D%s=false, so skipping tests which use cldr-archive\n"
                                    + "See <%s>",
                            HAS_CLDR_ARCHIVE, CLDRURLS.CLDR_ARCHIVE));
        }
    }

    /**
     * @return true if it's OK to read CLDRPaths.ARCHIVE_DIRECTORY, false to skip.
     */
    public static final boolean canUseArchiveDirectory() {
        if (!CLDRConfig.getInstance().getProperty("HAS_CLDR_ARCHIVE", true)) {
            return false; // skip due to property
        }

        final File archiveDir = new File(CLDRPaths.ARCHIVE_DIRECTORY);
        if (!archiveDir.isDirectory()) {
            throw new IllegalArgumentException(
                    String.format(
                            "Could not read archive directory -DARCHIVE=%s … You must either: 1) follow instructions at <%s>, "
                                    + "or 2) skip cldr-archive tests by setting -D%s=false",
                            archiveDir.getAbsolutePath(), CLDRURLS.CLDR_ARCHIVE, HAS_CLDR_ARCHIVE));
        }
        return true; // OK to use
    }

    @Test
    void TestReadPrevSDI() {
        // This is also an example of a test that's skipped (by the next line) if
        // cldr-archive isn't available.
        assumeTrue(canUseArchiveDirectory());
        SupplementalDataInfo SDI_LAST =
                SupplementalDataInfo.getInstance(
                        CLDRPaths.LAST_RELEASE_DIRECTORY + "common/supplemental/");
        assertNotNull(SDI_LAST);
    }

    @Test
    void testGenerateExampleDependencies() {
        // There was a bug involving GenerateExampleDependencies. A test case involved these two
        // paths. When working correctly, dependencies should include a mapping from pathA to pathB,
        // in multiple locales including "aa". (This means that an example for pathB may depend on
        // the value for pathA.) (The bug was fixed by ensuring that the ExampleGenerator used by
        // GenerateExampleDependencies creates its ICUServiceBuilder instance using the provided
        // RecordingCLDRFile, not an ordinary CLDRFile.) It is possible that in the future, this
        // dependency might no longer be required, and then this test should be revised accordingly
        // to test different paths and/or a different locale.
        final String pathA =
                "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/eras/eraAbbr/era[@type=\"([^\"]*+)\"]";
        final String pathB =
                "//ldml/dates/calendars/calendar[@type=\"([^\"]*+)\"]/dateFormats/dateFormatLength[@type=\"([^\"]*+)\"]/dateFormat[@type=\"([^\"]*+)\"]/pattern[@type=\"([^\"]*+)\"]";
        final String localeId = "aa";

        final Multimap<String, String> dependencies = TreeMultimap.create();
        final Matcher fileFilter = PatternCache.get("^" + localeId + "$").matcher("");
        new GenerateExampleDependencies(fileFilter, false /* not verbose */)
                .addDependenciesForLocale(dependencies, localeId);
        assertFalse(dependencies.isEmpty(), "Dependencies should not be empty");
        assertTrue(dependencies.containsKey(pathA), "Dependencies should contain " + pathA);
        assertTrue(
                dependencies.get(pathA).contains(pathB),
                "Dependencies for " + pathA + " should contain " + pathB);
    }
}

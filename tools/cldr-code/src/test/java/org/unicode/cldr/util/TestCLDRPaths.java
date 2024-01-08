package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import org.junit.jupiter.api.Test;

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
}

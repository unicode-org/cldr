package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import org.junit.jupiter.api.Test;

public class TestCLDRPaths {

    @Test
    void TestCanUseArchiveDirectory() {
        if (!canUseArchiveDirectory()) {
            // We print this warning as part of the unit tests.
            System.err.println(
                    "WARNING: skipping using cldr-archive. Ideally, -DNO_CLDR_ARCHIVE=false and see <https://cldr.unicode.org/development/creating-the-archive>");
        }
    }

    /**
     * @return true if it's OK to read CLDRPaths.ARCHIVE_DIRECTORY, false to skip.
     */
    public static final boolean canUseArchiveDirectory() {
        if (CLDRConfig.getInstance().getProperty("NO_CLDR_ARCHIVE", false)) {
            return false; // skip, NO_CLDR_ARCHIVE is set.
        }

        final File archiveDir = new File(CLDRPaths.ARCHIVE_DIRECTORY);
        if (!archiveDir.isDirectory()) {
            throw new IllegalArgumentException(
                    String.format(
                            "Could not read archive directory %s. "
                                    + "Please: "
                                    + "1) setup the archive, <https://cldr.unicode.org/development/creating-the-archive>, "
                                    + "2) set the -DARCHIVE= property to the correct archive location, or "
                                    + "3) inhibit reading of cldr-archive with -DNO_CLDR_ARCHIVE=true",
                            archiveDir.getAbsolutePath()));
        }
        return true; // OK to use
    }

    @Test
    void TestReadPrevSDI() {
        assumeTrue(canUseArchiveDirectory());
        SupplementalDataInfo SDI_LAST =
                SupplementalDataInfo.getInstance(
                        CLDRPaths.LAST_RELEASE_DIRECTORY + "common/supplemental/");
        assertNotNull(SDI_LAST);
    }
}

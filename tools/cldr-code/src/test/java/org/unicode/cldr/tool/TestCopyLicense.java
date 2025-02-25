package org.unicode.cldr.tool;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Files;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.util.CldrUtility;

/**
 * Note: CLDR-18205: There are two copies of LICENSE to work around Eclipse bug
 * https://github.com/eclipse-m2e/m2e-core/issues/1912
 */
public class TestCopyLicense {
    @Test
    void TestLicenseFileCopy() {
        String resourceLicense;
        String rootLicense;
        final String HELP = "Run CopyLicense";
        try {
            rootLicense = Files.readString(CopyLicense.ROOT_PATH);
        } catch (Throwable t) {
            fail("Could not read " + CopyLicense.ROOT_PATH + " !", t);
            return;
        }
        try {
            resourceLicense =
                    CldrUtility.getUTF8Data(CldrUtility.LICENSE)
                            .lines()
                            .collect(Collectors.joining("\n"));
        } catch (Throwable t) {
            fail("Could not read " + CopyLicense.RESOURCE_PATH + " - " + HELP, t);
            return;
        }
        assertArrayEquals(
                rootLicense.split("\n"),
                resourceLicense.split("\n"),
                () -> "License mismatch - " + HELP);
    }
}

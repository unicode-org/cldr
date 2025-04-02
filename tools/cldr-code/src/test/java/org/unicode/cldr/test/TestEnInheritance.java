package org.unicode.cldr.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.unittest.TestPaths;
import org.unicode.cldr.util.*;

/**
 * Check modern coverage inherited values in en that are marked with up arrows or are null; all
 * values should be explicit in en
 */
public class TestEnInheritance {
    private static final Logger logger = Logger.getLogger(TestEnInheritance.class.getName());

    private static final String LOCALE_ID = "en";

    private final CLDRConfig config = CLDRConfig.getInstance();
    private final Factory factory = config.getCommonAndSeedAndMainAndAnnotationsFactory();
    private final CLDRFile cldrFile = factory.make(LOCALE_ID, false);
    private CLDRFile cldrFileResolved = null;

    @Test
    void testInheritance() {
        final SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();
        final CoverageLevel2 coverageLevel = CoverageLevel2.getInstance(sdi, LOCALE_ID);
        int pathsWithNullValue = 0, pathsWithMarkerValue = 0;
        for (final String path : cldrFile.fullIterable()) {
            if (coverageLevel.getLevel(path).getLevel() <= Level.MODERN.getLevel()) {
                String value = cldrFile.getStringValue(path);
                if (value == null) {
                    // Do not complain about
                    // //ldml/dates/timeZoneNames/metazone[@type="Gulf"]/short/standard
                    if (!cldrFile.getExtraPaths().contains(path)
                            || !TestPaths.extraPathAllowsNullValue(path)) {
                        complain("null value", ++pathsWithNullValue, path);
                    }
                } else if (CldrUtility.INHERITANCE_MARKER.equals(value)) {
                    complain("inheritance marker", ++pathsWithMarkerValue, path);
                }
            }
        }
        assertEquals(0, pathsWithNullValue + pathsWithMarkerValue, "failures");
    }

    private void complain(String description, int count, String path) {
        if (cldrFileResolved == null) {
            cldrFileResolved = factory.make(LOCALE_ID, true);
        }
        String resolvedValue = cldrFileResolved.getStringValue(path);
        logger.severe(
                this.getClass().getSimpleName()
                        + " -- "
                        + path
                        + " -- "
                        + description
                        + " "
                        + count
                        + " -- Resolved value: ["
                        + resolvedValue
                        + "]");
    }
}

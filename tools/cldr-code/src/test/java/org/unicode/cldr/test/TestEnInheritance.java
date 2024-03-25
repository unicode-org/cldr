package org.unicode.cldr.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.util.*;

/**
 * Check modern coverage inherited values in en that are marked with up arrows or are blank; all
 * values should be explicit in en
 */
public class TestEnInheritance {
    private static final Logger logger = Logger.getLogger(TestEnInheritance.class.getName());

    private static final String LOCALE_ID = "en";

    @Test
    void testEn() {
        final CLDRConfig config = CLDRConfig.getInstance();
        final CLDRFile cldrFile = config.getCLDRFile(LOCALE_ID, false);
        final SupplementalDataInfo sdi = SupplementalDataInfo.getInstance();
        final CoverageLevel2 coverageLevel = CoverageLevel2.getInstance(sdi, LOCALE_ID);
        int pathsWithNullValue = 0, pathsWithBlankValue = 0, pathsWithMarkerValue = 0;
        for (final String path : cldrFile.fullIterable()) {
            if (coverageLevel.getLevel(path).getLevel() <= Level.MODERN.getLevel()) {
                String value = cldrFile.getStringValue(path);
                if (value == null) {
                    complain("null value", ++pathsWithNullValue, path);
                } else if (value.isBlank()
                        && !DisplayAndInputProcessor.FSR_START_PATH.equals(path)
                        && !DisplayAndInputProcessor.NSR_START_PATH.equals(path)) {
                    complain("blank value", ++pathsWithBlankValue, path);
                } else if (CldrUtility.INHERITANCE_MARKER.equals(value)) {
                    complain("inheritance marker", ++pathsWithMarkerValue, path);
                }
            }
        }
        assertEquals(0, pathsWithNullValue, "null values");
        assertEquals(0, pathsWithBlankValue, "blank values");
        assertEquals(0, pathsWithMarkerValue, "inheritance marker values");
    }

    private void complain(String description, int count, String path) {
        logger.severe(
                this.getClass().getSimpleName() + " -- " + description + " " + count + " " + path);
    }
}

package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class TestStandardCodes {
    static final StandardCodes sc = StandardCodes.make();

    @ParameterizedTest(name = "{index}: getTargetCoverageLevel {0}")
    @CsvSource({
        // This test will be sensitive to changes in Locales.txt
        "doi,BASIC",  // CLDR locale
        "nn,MODERN",  // CLDR locale
        "hnj,MODERN", // Maximum coverage (hmong)
        "br,MODERATE",  // Maximum (Breton)
        "zxx,BASIC",  // "all others: BASIC"
    })
    void testTargetCoverageLevel(final String locale, final String level) {
        assertNotNull(sc, "StandardCodes");
        final Level expectLevel = Level.fromString(level);
        final Level actualLevel = sc.getTargetCoverageLevel(locale);
        assertEquals(expectLevel, actualLevel,
            () -> String.format("Expected getTargetCoverageLevel(%s)=%s but was %s",
                locale, expectLevel, actualLevel));
    }
}

package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

public class TestCalculatedCoverageLevels {
    @Test
    void TestInstantiate() {
        CalculatedCoverageLevels ccl = CalculatedCoverageLevels.getInstance();
        assertNotNull(ccl, "failed: CalculatedCoverageLevels.getInstance()");
    }

    @Test
    void TestBasic() {
        assertAll(
                "Test several cases",
                () ->
                        assertTrue(
                                CalculatedCoverageLevels.getInstance().isLocaleAtLeastBasic("en"),
                                "en=true"),
                () ->
                        assertTrue(
                                CalculatedCoverageLevels.getInstance().isLocaleAtLeastBasic("fr"),
                                "fr=true"),
                () ->
                        assertFalse(
                                CalculatedCoverageLevels.getInstance().isLocaleAtLeastBasic("und"),
                                "und=false"));
    }

    public static Stream<Arguments> allLocales() {
        return CLDRConfig.getInstance().getFullCldrFactory().getAvailable().stream()
                .map(str -> arguments(str));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allLocales")
    @Disabled
    /**
     * Test compares coverage level to the Locales.txt goals. Skipped because it fails (for cv, as
     * of v43), but there may be a useful form for it in the future
     *
     * @param locale
     */
    void TestCoverageLevelsVsCldrOrg(final String locale) {
        final Level coverageLevel = CalculatedCoverageLevels.getInstance().getLevels().get(locale);
        assumeTrue(
                coverageLevel != null, () -> "CalculatedCoverageLevel not available for " + locale);
        final Level localeCoverageLevel =
                StandardCodes.make().getHighestLocaleCoverageLevel("Cldr", locale);
        assumeTrue(
                localeCoverageLevel != null, () -> "Cldr membership not available for " + locale);
        assertTrue(
                localeCoverageLevel.compareTo(coverageLevel) >= 0,
                () ->
                        String.format(
                                "Expected Locales.txt:Cldr=%s at least coverageLevels=%s",
                                coverageLevel.name(), localeCoverageLevel.name()));
    }

    @ParameterizedTest
    @CsvSource({
        "en_US, modern",
        "en, modern",
        "mul, null",
        "root, modern",
        "sr_Cyrl, modern",
        "sr_Cyrl_XK, modern",
        "sr, modern",
    })
    void TestEffective(final String loc, String l) {
        Level level = null;
        if (!l.equals("null")) {
            level = Level.fromString(l.toUpperCase());
        }
        CalculatedCoverageLevels ccl =
                CalculatedCoverageLevels
                        .getInstance(); // TODO: data sensitivity.  Ideally would use a private
        // instance of CCL from a static file.
        assertEquals(level, ccl.getEffectiveCoverageLevel(loc));
    }
}

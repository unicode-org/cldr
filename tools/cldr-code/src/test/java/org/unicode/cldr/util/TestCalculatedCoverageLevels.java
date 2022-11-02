package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TestCalculatedCoverageLevels {
    @Test
    void TestInstantiate() {
        CalculatedCoverageLevels ccl = CalculatedCoverageLevels.getInstance();
        assertNotNull(ccl, "failed: CalculatedCoverageLevels.getInstance()");
    }

    @Test
    void TestBasic() {
        assertAll("Test several cases",
        () -> assertTrue(CalculatedCoverageLevels.getInstance().isLocaleAtLeastBasic("en"), "en=true"),
        () -> assertTrue(CalculatedCoverageLevels.getInstance().isLocaleAtLeastBasic("fr"), "fr=true"),
        () -> assertFalse(CalculatedCoverageLevels.getInstance().isLocaleAtLeastBasic("und"), "und=false"));
    }
}

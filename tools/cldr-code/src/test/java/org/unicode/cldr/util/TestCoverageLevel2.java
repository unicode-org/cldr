package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.test.CoverageLevel2;

public class TestCoverageLevel2 {

    final int ITERATIONS = 100000; // keep this low for normal testing

    private static SupplementalDataInfo sdi;

    @BeforeAll
    private static void setup() {
        sdi = CLDRConfig.getInstance().getSupplementalDataInfo();
        CoverageLevel2 c = CoverageLevel2.getInstance(sdi, "fr_CA");
    }

    @Test
    public void TestCoveragePerf() {
        for (int i = 0; i < ITERATIONS; i++) {
            CoverageLevel2 c = CoverageLevel2.getInstance(sdi, "fr_CA");
            assertEquals(
                    Level.MODERATE,
                    c.getLevel(
                            "//ldml/characters/parseLenients[@scope=\"number\"][@level=\"lenient\"]/parseLenient[@sample=\",\"]"));
        }
    }
}

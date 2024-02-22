package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class TestIso3166Data {
    @Test
    void TestBasic() {
        // very basic test.
        assertNotNull(Iso3166Data.getIsoDescription());
        assertNotNull(Iso3166Data.getIsoStatus());
    }

    @Test
    void testRegionNotForTranslation() {
        // Verify that all region code(s) in getRegionCodesNotForTranslation() are
        // listed as exceptionally reserved
        Iso3166Data.getRegionCodesNotForTranslation()
                .forEach(
                        (region) ->
                                assertEquals(
                                        Iso3166Data.Iso3166Status.exceptionally_reserved,
                                        Iso3166Data.getIsoStatus().get(region),
                                        () -> ("Wrong ISO-3166 status for " + region)));
    }
}

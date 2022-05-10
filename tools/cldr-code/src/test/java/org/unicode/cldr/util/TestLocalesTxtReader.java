package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import org.junit.jupiter.api.Test;

public class TestLocalesTxtReader {

    @Test
    void TestRegularLocalesTxt() {
        LocalesTxtReader rdr = new LocalesTxtReader().read(StandardCodes.make());
        assertNotNull(rdr);
        assertAll("LocalesTxtReader assertions",
            // These are not used yet
            // () -> assertTrue(rdr.organization_locale_match.isEmpty(),
            // () -> assertTrue(rdr.organization_locale_weight.isEmpty(),

            () -> assertFalse(rdr.platform_level_locale.isEmpty()),
            () -> assertFalse(rdr.platform_locale_level.isEmpty()),
            () -> assertFalse(rdr.platform_locale_levelString.isEmpty()));
    }

    @Test
    void TestCannedLocalesTxt() throws IOException {
        try (BufferedReader r = FileReaders.openFile(TestLocalesTxtReader.class, "TestLocales.txt");) {
            LocalesTxtReader rdr = new LocalesTxtReader().read(StandardCodes.make(), r);
            assertNotNull(rdr);
            assertAll("LocalesTxtReader basic assertions",
                () -> assertFalse(rdr.organization_locale_match.isEmpty()),
                () -> assertFalse(rdr.organization_locale_weight.isEmpty()),
                () -> assertFalse(rdr.platform_level_locale.isEmpty()),
                () -> assertFalse(rdr.platform_locale_level.isEmpty()),
                () -> assertFalse(rdr.platform_locale_levelString.isEmpty()),
                () -> assertTrue(rdr.organization_locale_match.containsKey(Organization.adlam)),
                () -> assertTrue(rdr.organization_locale_weight.containsKey(Organization.adlam)),
                () -> assertTrue(rdr.platform_locale_level.containsKey(Organization.adlam)),
                () -> assertTrue(rdr.platform_locale_level.containsKey(Organization.wod_nko)),
                () -> assertTrue(rdr.platform_locale_level.containsKey(Organization.wikimedia)));

            Map<String, Integer> adlam_weight = rdr.organization_locale_weight.get(Organization.adlam);
            Map<String, Set<String>> adlam_match = rdr.organization_locale_match.get(Organization.adlam);
            Map<String, Level> nko_level = rdr.platform_locale_level.get(Organization.wod_nko);
            Map<String, Level> adlam_level = rdr.platform_locale_level.get(Organization.adlam);
            Map<String, Level> wikimedia_level = rdr.platform_locale_level.get(Organization.wikimedia);

            assertAll("verify maps",
                () -> assertNotNull(adlam_weight),
                () -> assertFalse(adlam_weight.isEmpty()),
                () -> assertNotNull(adlam_match),
                () -> assertFalse(adlam_match.isEmpty()),
                () -> assertNotNull(nko_level),
                () -> assertFalse(nko_level.isEmpty()),
                () -> assertNotNull(adlam_level),
                () -> assertFalse(adlam_level.isEmpty()),
                () -> assertNotNull(wikimedia_level),
                () -> assertFalse(wikimedia_level.isEmpty()));

            final Set<String> adlamPaths = adlam_match.get(StandardCodes.ALL_LOCALES);
            assertAll("verify values",
                () -> assertEquals(4, adlam_weight.get(StandardCodes.ALL_LOCALES)),
                () -> assertEquals(ImmutableSet.of("annotations1","annotations2","characterLabel1","characterLabel2"), adlamPaths, "adlam: *"),
                () -> assertEquals(Level.MODERATE, adlam_level.get(StandardCodes.ALL_LOCALES), "adlam: *"),
                () -> assertEquals(Level.MODERN, nko_level.get("nqo"), "wod_nko: nqo"),
                () -> assertEquals(Level.BASIC, wikimedia_level.get(StandardCodes.ALL_LOCALES), "wikimedia: *"));

        }
    }
}

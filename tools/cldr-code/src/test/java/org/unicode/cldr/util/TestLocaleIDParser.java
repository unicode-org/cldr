package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.unicode.cldr.util.SupplementalDataInfo.ParentLocaleComponent;

public class TestLocaleIDParser {
    @ParameterizedTest(name = "{index}: {0}")
    @CsvSource({
        // Right hand: pipe separated chain
        // original,original|parent|parent|root
        "en_US_someVariant,en_US_someVariant|en_US|en|root",
        "zh_Hans_CN,zh_Hans_CN|zh_Hans|zh|root",
        "zh_Hant_HK,zh_Hant_HK|zh_Hant|root"
    })
    void TestNormalFallback(final String locid, final String chain) {
        String loc = locid;
        for (final String link : chain.split("\\|")) {
            assertEquals(link, loc, "Fallback chain for " + locid);
            final String newLoc = LocaleIDParser.getParent(loc);
            // make sure we are not stuck
            assertNotEquals(loc, newLoc, "Error: getParent() returned the same value");
            loc = newLoc;
        }
        assertNull(
                loc, locid + ": Test error: Expected chain to fall back to 'root' and then null");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @CsvSource({
        // Right hand: pipe separated chain
        // original,original|parent|parent|root
        "en_US_someVariant,en_US_someVariant|en_US|en|root",
        "zh_Hans_CN,zh_Hans_CN|zh_Hans|zh|root",
        "zh_Hant_HK,zh_Hant_HK|zh_Hant|zh|root"
    })
    void TestCollationFallback(final String locid, final String chain) {
        String loc = locid;
        for (final String link : chain.split("\\|")) {
            assertEquals(link, loc, "Fallback chain for " + locid);
            final String newLoc = LocaleIDParser.getParent(loc, ParentLocaleComponent.collations);
            // make sure we are not stuck
            assertNotEquals(loc, newLoc, "Error: getParent() returned the same value");
            loc = newLoc;
        }
        assertNull(
                loc, locid + ": Test error: Expected chain to fall back to 'root' and then null");
    }
}

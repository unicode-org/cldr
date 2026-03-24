package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.unicode.cldr.icu.LDMLConstants;

public class TestExtraPaths {
    private final String PATH = "//ldml/localeDisplayNames/keys/key[@type=\"calendar\"]";

    static Set<String> getExtraPathsFor(String localeId) {
        return CLDRConfig.getInstance().getCLDRFile(localeId, false).getRawExtraPaths();
    }

    @ParameterizedTest(name = "{index} locale={0}")
    @ValueSource(strings = {"de", "ru"})
    public void testKeys(final String localeId) {
        final Set<String> paths = getExtraPathsFor(localeId);
        final String keys[] = {
            // These are the keys that were available before v49
            // make sure all remain available
            "calendar",
            "cf",
            "collation",
            "currency",
            "em",
            "hc",
            "lb",
            "lw",
            "ms",
            "numbers",
            "ss",
            // New keys of interest
            "colAlternate",
            "colBackwards",
            "colCaseFirst",
            "colCaseLevel",
            "colNormalization",
            "colNumeric",
            "colReorder",
            "colStrength",
            "d0",
            "dx",
            "fw",
            "h0",
            "i0",
            "k0",
            "kv",
            "m0",
            "mu",
            "rg",
            "s0",
            "rg",
            "sd",
            "ss",
            "t",
            "t0",
            "timezone",
            "va",
            "x",
            "x0",
        };
        XPathParts xpath = XPathParts.getFrozenInstance(PATH).cloneAsThawed();
        final Set<String> missing = new TreeSet<>();
        for (final String k : keys) {
            xpath.setAttribute(-1, LDMLConstants.TYPE, k);
            final String path = xpath.toString();
            if (!paths.contains(path)) missing.add(path);
        }
        assertTrue(missing.isEmpty(), () -> "Missing xpaths: " + missing);
    }
}

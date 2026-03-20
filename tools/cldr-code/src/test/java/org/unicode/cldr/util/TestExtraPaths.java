package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.unicode.cldr.icu.LDMLConstants;

public class TestExtraPaths {
    private final String PATH = "//ldml/localeDisplayNames/keys/key[@type=\"calendar\"]";

    static Set<String> getExtraPathsFor(String localeId) {
        final CLDRFile file = CLDRConfig.getInstance().getCLDRFile(localeId, false);
        return getExtraPathsFor(file);
    }

    static Set<String> getExtraPathsFor(CLDRFile file) {
        Set<String> toAddTo = new TreeSet<>();
        ExtraPaths.addConstant(toAddTo);
        ExtraPaths.addLocaleDependent(toAddTo, file.iterableWithoutExtras(), file.getLocaleID());
        return ImmutableSet.copyOf(toAddTo);
    }

    private static Supplier<Set<String>> ruExtraPaths =
            Suppliers.memoize(() -> getExtraPathsFor("ru"));
    private static Supplier<Set<String>> deExtraPaths =
            Suppliers.memoize(() -> getExtraPathsFor("de"));

    static Set<String> getCachedPathsFor(String localeId) {
        if (localeId.equals("de")) return deExtraPaths.get();
        if (localeId.equals("ru")) return ruExtraPaths.get();
        throw new IllegalArgumentException("No cached paths for " + localeId);
    }

    @Test
    @Disabled // normally off - just dumps all of the paths
    public void testDump() {
        Set<String> ru = getCachedPathsFor("ru");
        System.out.println("ru\n--\n" + String.join("\n", ru.toArray(new String[0])));
        Set<String> de = getCachedPathsFor("de");
        System.out.println("de\n--\n" + String.join("\n", de.toArray(new String[0])));
    }

    @ParameterizedTest(name = "{index} locale={0}")
    @ValueSource(strings = {"de", "ru"})
    public void testKeys(final String localeId) {
        final Set<String> paths = getCachedPathsFor(localeId);
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

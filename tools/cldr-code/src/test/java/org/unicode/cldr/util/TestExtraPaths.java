package org.unicode.cldr.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.unicode.cldr.icu.LDMLConstants;
import org.unicode.cldr.test.CoverageLevel2;

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

        {
            final String typeKeys[] = {
                // big static list removed from ExtraPaths
                "//ldml/localeDisplayNames/types/type[@key=\"calendar\"][@type=\"buddhist\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"calendar\"][@type=\"chinese\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"calendar\"][@type=\"coptic\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"calendar\"][@type=\"dangi\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"calendar\"][@type=\"ethiopic-amete-alem\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"calendar\"][@type=\"ethiopic\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"calendar\"][@type=\"gregorian\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"calendar\"][@type=\"hebrew\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"calendar\"][@type=\"indian\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"calendar\"][@type=\"islamic-civil\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"calendar\"][@type=\"islamic\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"calendar\"][@type=\"islamic-tbla\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"calendar\"][@type=\"islamic-umalqura\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"calendar\"][@type=\"japanese\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"calendar\"][@type=\"persian\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"calendar\"][@type=\"roc\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"collation\"][@type=\"compat\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"collation\"][@type=\"dictionary\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"collation\"][@type=\"ducet\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"collation\"][@type=\"phonebook\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"collation\"][@type=\"phonetic\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"collation\"][@type=\"pinyin\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"collation\"][@type=\"search\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"collation\"][@type=\"standard\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"collation\"][@type=\"stroke\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"collation\"][@type=\"traditional\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"collation\"][@type=\"unihan\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"collation\"][@type=\"zhuyin\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"cf\"][@type=\"account\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"cf\"][@type=\"standard\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"em\"][@type=\"default\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"em\"][@type=\"emoji\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"em\"][@type=\"text\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"hc\"][@type=\"h11\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"hc\"][@type=\"h12\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"hc\"][@type=\"h23\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"hc\"][@type=\"h24\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"lb\"][@type=\"loose\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"lb\"][@type=\"normal\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"lb\"][@type=\"strict\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"lw\"][@type=\"breakall\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"lw\"][@type=\"keepall\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"lw\"][@type=\"normal\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"lw\"][@type=\"phrase\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"ms\"][@type=\"metric\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"ms\"][@type=\"uksystem\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"ms\"][@type=\"ussystem\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"ss\"][@type=\"none\"][@scope=\"core\"]",
                "//ldml/localeDisplayNames/types/type[@key=\"ss\"][@type=\"standard\"][@scope=\"core\"]"
            };

            for (final String path : typeKeys) {
                if (!paths.contains(path)) missing.add(path);
            }
        }

        assertTrue(missing.isEmpty(), () -> "Missing xpaths: " + missing);
    }

    @ParameterizedTest(name = "{index} locale={0}.xml")
    @ValueSource(strings = {"de", "ru"})
    public void testMissingKeyAliases(final String localeId) {
        // test that we don't have any paths *in locale* which use the aliases instead of the short
        // code
        final Set<String> paths = getExtraPathsFor(localeId);
        final Set<String> missing = new TreeSet<>();
        final CLDRFile f = CLDRConfig.getInstance().getCLDRFile(localeId, true);
        // Relation<R2<String,String>,String> aliases =
        // CLDRConfig.getInstance().getSupplementalDataInfo().getBcp47Aliases();
        for (Iterator<String> i =
                        f.iteratorWithoutExtras("//ldml/localeDisplayNames/keys/key", null);
                i.hasNext(); ) {
            final String path = i.next();
            if (!paths.contains(path)) {
                missing.add(path);
            }
        }
        assertTrue(missing.isEmpty(), () -> "In XML but not in extraPaths: " + missing);
    }

    @ParameterizedTest(name = "{index} locale={0}.xml")
    @ValueSource(strings = {"de", "ru"})
    public void testMissingKeyTypes(final String localeId) {
        // test that we don't have any paths *in locale* which use the aliases instead of the short
        // code
        final Set<String> paths = getExtraPathsFor(localeId);
        final Set<String> missing = new TreeSet<>();
        final CLDRFile f = CLDRConfig.getInstance().getCLDRFile(localeId, true);
        // Relation<R2<String,String>,String> aliases =
        // CLDRConfig.getInstance().getSupplementalDataInfo().getBcp47Aliases();
        for (Iterator<String> i =
                        f.iteratorWithoutExtras("//ldml/localeDisplayNames/types/type", null);
                i.hasNext(); ) {
            final String path = i.next();
            if (!paths.contains(path)) {
                missing.add(path);
            }
        }
        assertTrue(missing.isEmpty(), () -> "In XML but not in extraPaths: " + missing);
    }

    @ParameterizedTest(name = "{index} locale={0}.xml")
    @ValueSource(strings = {"de", "ru"})
    public void testTypeCoverageVs48(final String localeId) {
        CoverageLevel2 cov =
                CoverageLevel2.getInstance(
                        CLDRConfig.getInstance().getSupplementalDataInfo(), localeId);
        final CLDRFile f = CLDRConfig.getInstance().getCLDRFile(localeId, true);
        for (Iterator<String> i = f.iterator("//ldml/localeDisplayNames/types/type", null);
                i.hasNext(); ) {
            final String path = i.next();
            final Level l = cov.getLevel(path);
            if (path.endsWith("[@scope=\"core\"]")) {
                assertEquals(Level.MODERN, l, () -> localeId + ":" + path);
                // System.out.println("Core " + l + " " + path);
            } else {
                // System.out.println("NonCore " + l + " " + path);
            }
        }
    }
}

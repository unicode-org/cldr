package org.unicode.cldr.unittest;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import org.unicode.cldr.icu.dev.test.TestFmwk;
import org.unicode.cldr.test.CheckAltOnly;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.Options;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;

/** Unit tests for CheckAltOnly.java */
public class TestCheckAltOnly extends TestFmwk {
    public static void main(String[] args) {
        new TestCheckAltOnly().run(args);
    }

    /**
     * Test with a data item for which CheckAltOnly was originally created:
     *
     * <p>wo.xml: <territory type="HK" alt="short">Ooŋ Koŋ</territory>
     *
     * <p>That should trigger an error if the corresponding path without alt isn't present.
     */
    public void testAltWithoutNonAlt() {
        final String testLocale = "wo";
        final String altPath =
                "//ldml/localeDisplayNames/territories/territory[@type=\"HK\"][@alt=\"short\"]";
        final String value = "Ooŋ Koŋ";

        final XMLSource source = new SimpleXMLSource(testLocale);

        source.putValueAtPath(altPath, value);

        final CLDRFile cldrFile = new CLDRFile(source);
        final TestFactory factory = new TestFactory();
        factory.addFile(cldrFile);

        /*
         * Expect an error for altPath
         */
        checkFile(factory, testLocale, altPath);
    }

    private enum TestSubLocaleMode {
        PARENT_PRESENT,
        PARENT_ABSENT,
    }

    private enum TestPathType {
        CONSTRUCTED,
        NOT_CONSTRUCTED,
    }

    /**
     * Test CheckAltOnly for situations involving sub-locales
     *
     * <p>yo_BJ.xml: <language type="en_US" alt="short">Èdè Gɛ̀ɛ́sì (US)</language>
     *
     * <p>yo.xml: <language type="en_US">↑↑↑</language> <language type="en_US" alt="short">Èdè
     * Gẹ̀ẹ́sì (US)</language>
     *
     * <p>There are two important factors: (1) whether there is a constructed value for the non-alt
     * path, in which case, no error (2) whether the non-alt path is present in the parent locale,
     * in which case, no error
     *
     * <p>In the actual data this is based on, there was a constructed value for the
     * languages/language path. We also test for a different path (...territories/territory...),
     * without a constructed value.
     *
     * <p>As in the actual data, the values (Gɛ̀ɛ́sì vs Gẹ̀ẹ́sì) are slightly different, but that
     * fact is probably not important for this test.
     *
     * <p>In the actual data, the non-alt value in the parent was "↑↑↑"
     * (CldrUtility.INHERITANCE_MARKER), and if the path had not been the type that gets a
     * constructed value, inheritance marker would have resulted in the inheritance from
     * code-fallback. We also test with a value other than inheritance marker. If a value is present
     * for the non-alt path in "yo" there should be no error regardless of whether the value is
     * inheritance marker.
     */
    public void testSubLocale() {
        final String altPathConstructed =
                "//ldml/localeDisplayNames/languages/language[@type=\"en_US\"][@alt=\"short\"]";
        final String altPathNotConstructed =
                "//ldml/localeDisplayNames/territories/territory[@type=\"CI\"][@alt=\"variant\"]";

        reallyTestSubLocale(
                TestSubLocaleMode.PARENT_ABSENT,
                altPathConstructed,
                TestPathType.CONSTRUCTED,
                null);
        reallyTestSubLocale(
                TestSubLocaleMode.PARENT_ABSENT,
                altPathNotConstructed,
                TestPathType.NOT_CONSTRUCTED,
                null);

        reallyTestSubLocale(
                TestSubLocaleMode.PARENT_PRESENT,
                altPathConstructed,
                TestPathType.CONSTRUCTED,
                CldrUtility.INHERITANCE_MARKER);
        reallyTestSubLocale(
                TestSubLocaleMode.PARENT_PRESENT,
                altPathNotConstructed,
                TestPathType.NOT_CONSTRUCTED,
                CldrUtility.INHERITANCE_MARKER);

        reallyTestSubLocale(
                TestSubLocaleMode.PARENT_PRESENT,
                altPathNotConstructed,
                TestPathType.NOT_CONSTRUCTED,
                "notInheritanceMarker");
    }

    private void reallyTestSubLocale(
            TestSubLocaleMode mode, String altPath, TestPathType pathType, String valueParNonAlt) {
        final String subLocale = "yo_BJ";
        final String parLocale = "yo";
        final String nonAltPath = XPathParts.getPathWithoutAlt(altPath);
        final String valueSubAlt = "Èdè Gɛ̀ɛ́sì";
        final String valueParAlt = "Èdè Gẹ̀ẹ́sì";

        final XMLSource parSource = new SimpleXMLSource(parLocale);
        final XMLSource subSource = new SimpleXMLSource(subLocale);

        subSource.putValueAtPath(altPath, valueSubAlt);

        if (TestSubLocaleMode.PARENT_PRESENT.equals(mode)) {
            parSource.putValueAtPath(altPath, valueParAlt);
            parSource.putValueAtPath(nonAltPath, valueParNonAlt);
        }

        final CLDRFile parCldrFile = new CLDRFile(parSource);
        final CLDRFile subCldrFile = new CLDRFile(subSource);
        final TestFactory factory = new TestFactory();
        factory.addFile(parCldrFile);
        factory.addFile(subCldrFile);

        if (TestSubLocaleMode.PARENT_PRESENT.equals(mode)
                || TestPathType.CONSTRUCTED.equals(pathType)) {
            /*
             * Expect no errors
             */
            checkFile(factory, subLocale);
        } else {
            /*
             * Expect error for altPath
             */
            checkFile(factory, subLocale, altPath);
        }
    }

    private void checkFile(TestFactory factory, String localeId, String... expectedErrors) {
        final CheckAltOnly ch = new CheckAltOnly(factory);
        ch.setEnglishFile(CLDRConfig.getInstance().getEnglish());

        final CLDRFile cldrFile = factory.make(localeId, true /* resolved */);
        List<CheckStatus> possibleErrors = new ArrayList<>();
        Options options = new Options();
        ch.setCldrFileToCheck(cldrFile, options, possibleErrors);
        if (!possibleErrors.isEmpty()) {
            errln("Possible errors from setCldrFileToCheck: " + possibleErrors);
            possibleErrors.clear();
        }
        Map<String, List<CheckStatus>> found = new HashMap<>();
        for (String path : cldrFile) {
            String value = cldrFile.getStringValue(path);
            ch.check(path, path, value, options, possibleErrors);
            if (!possibleErrors.isEmpty()) {
                found.put(path, ImmutableList.copyOf(possibleErrors));
                possibleErrors.clear();
            }
        }
        Set<String> expected = new TreeSet<>(Arrays.asList(expectedErrors));
        for (Entry<String, List<CheckStatus>> entry : found.entrySet()) {
            String path = entry.getKey();
            if (expected.contains(path)) {
                expected.remove(path);
            } else {
                errln(localeId + " unexpected error: " + path + " : " + entry.getValue());
            }
        }
        assertEquals(localeId + " expected to be errors: ", Collections.emptySet(), expected);
    }
}

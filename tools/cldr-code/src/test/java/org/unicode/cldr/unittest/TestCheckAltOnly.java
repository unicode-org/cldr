package org.unicode.cldr.unittest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.test.CheckAltOnly;
import org.unicode.cldr.test.CheckCLDR.CheckStatus;
import org.unicode.cldr.test.CheckCLDR.Options;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;

import com.google.common.collect.ImmutableList;
import com.ibm.icu.dev.test.TestFmwk;

/**
 * Unit tests for CheckAltOnly.java
 */
public class TestCheckAltOnly extends TestFmwk {
    public static void main(String[] args) {
        new TestCheckAltOnly().run(args);
    }

    /**
     * Test with a data item for which CheckAltOnly was originally created:
     *
     * wo.xml:
     *   <territory type="HK" alt="short">Ooŋ Koŋ</territory>
     *
     * That should trigger an error if the corresponding path without alt isn't present.
     */
    public void testAltWithoutNonAlt() {
        final String testLocale = "wo";
        final String altPath = "//ldml/localeDisplayNames/territories/territory[@type=\"HK\"][@alt=\"short\"]";
        final String value = "Ooŋ Koŋ";

        final XMLSource source = new SimpleXMLSource(testLocale);

        source.putValueAtPath(altPath, value);

        final CLDRFile cldrFile = new CLDRFile(source);
        final TestFactory factory = new TestFactory();
        factory.addFile(cldrFile);

        final CheckAltOnly ch = new CheckAltOnly(factory);
        ch.setEnglishFile(CLDRConfig.getInstance().getEnglish());

        final CLDRFile resolved = factory.make(testLocale, true);
        /*
         * Expect an error for altPath
         */
        checkFile(ch, cldrFile, resolved, altPath);
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
     * yo_BJ.xml:
     *   <language type="en_US" alt="short">Èdè Gɛ̀ɛ́sì (US)</language>
     *
     * yo.xml:
     *   <language type="en_US">↑↑↑</language>
     *   <language type="en_US" alt="short">Èdè Gẹ̀ẹ́sì (US)</language>
     *
     * There are two important factors:
     * (1) whether there is a constructed value for the non-alt path, in which case, no error
     * (2) whether the non-alt path is present in the parent locale, in which case, no error
     *
     * In the actual data this is based on, there was a constructed value for the languages/language
     * path. We also test for a different path (...territories/territory...), without a constructed value.
     *
     * As in the actual data, the values (Gɛ̀ɛ́sì vs Gẹ̀ẹ́sì) are slightly different, and the value in
     * the parent is "↑↑↑" (CldrUtility.INHERITANCE_MARKER), but those facts are probably not important
     * for this test.
     */
    public void testSubLocale() {
        final String altPathConstructed = "//ldml/localeDisplayNames/languages/language[@type=\"en_US\"][@alt=\"short\"]";
        final String altPathNotConstructed = "//ldml/localeDisplayNames/territories/territory[@type=\"CI\"][@alt=\"variant\"]";

        reallyTestSubLocale(TestSubLocaleMode.PARENT_PRESENT, altPathConstructed, TestPathType.CONSTRUCTED);
        reallyTestSubLocale(TestSubLocaleMode.PARENT_ABSENT, altPathConstructed, TestPathType.CONSTRUCTED);
        reallyTestSubLocale(TestSubLocaleMode.PARENT_PRESENT, altPathNotConstructed, TestPathType.NOT_CONSTRUCTED);
        reallyTestSubLocale(TestSubLocaleMode.PARENT_ABSENT, altPathNotConstructed, TestPathType.NOT_CONSTRUCTED);
    }

    private void reallyTestSubLocale(TestSubLocaleMode mode, String altPath, TestPathType pathType) {
        final String subLocale = "yo_BJ";
        final String parLocale = "yo";
        final String nonAltPath = XPathParts.getPathWithoutAlt(altPath);
        final String valueSubAlt = "Èdè Gɛ̀ɛ́sì";
        final String valueParAlt = "Èdè Gẹ̀ẹ́sì";
        final String valueParNonAlt = CldrUtility.INHERITANCE_MARKER;

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

        final CheckAltOnly ch = new CheckAltOnly(factory);
        ch.setEnglishFile(CLDRConfig.getInstance().getEnglish());

        final CLDRFile resolved = factory.make(subLocale, true);
        if (TestSubLocaleMode.PARENT_PRESENT.equals(mode) || TestPathType.CONSTRUCTED.equals(pathType)) {
            /*
             * Expect no errors
             */
            checkFile(ch, subCldrFile, resolved);
        } else {
            /*
             * Expect error for altPath
             */
            checkFile(ch, subCldrFile, resolved, altPath);
        }
    }

    private void checkFile(CheckAltOnly ch, CLDRFile cldrFile, CLDRFile cldrFileResolved, String... expectedErrors) {
        List<CheckStatus> possibleErrors = new ArrayList<>();
        Options options = new Options();
        ch.setCldrFileToCheck(cldrFile, options, possibleErrors);
        Map<String,List<CheckStatus>> found = new HashMap<>();
        for (String path : cldrFileResolved) {
            String value = cldrFileResolved.getStringValue(path);
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
                errln(cldrFile.getLocaleID() + " unexpected error: " + path + " : " + entry.getValue());
            }
        }
        assertEquals(cldrFile.getLocaleID() + " expected to be errors: ", Collections.emptySet(), expected);
    }
}

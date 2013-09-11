package org.unicode.cldr.unittest;

import java.util.Map;
import java.util.Set;

import org.unicode.cldr.test.CheckDates;
import org.unicode.cldr.test.ExampleGenerator;
import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.LanguageTagCanonicalizer;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.SimpleXMLSource;
import org.unicode.cldr.util.SupplementalDataInfo;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.util.ULocale;

public class TestLocale extends TestFmwk {
    static TestInfo testInfo = TestInfo.getInstance();

    public static void main(String[] args) {
        new TestLocale().run(args);
    }

    public void TestConsistency() {
        LanguageTagParser ltp = new LanguageTagParser();
        SupplementalDataInfo supplementalDataInfo = testInfo.getSupplementalDataInfo();
        Set<String> defaultContentLocales = supplementalDataInfo.getDefaultContentLocales();
        Map<String, String> likelySubtags = supplementalDataInfo.getLikelySubtags();

        for (String locale : testInfo.getCldrFactory().getAvailable()) {
            if (locale.equals("root")) {
                continue;
            }
            ltp.set(locale);
            boolean isDefaultContent = defaultContentLocales.contains(locale);
            boolean hasScript = !ltp.getScript().isEmpty();
            boolean hasRegion = !ltp.getRegion().isEmpty();
            String language = ltp.getLanguage();
            String maximized = likelySubtags.get(language);
            boolean hasLikelySubtag = maximized != null;

            // verify that the parent locales are consistent with the default locales, for scripts
            // that is, if zh-Hant has a parent of root, then it is not the default content locale, and vice versa

            if (hasScript && !hasRegion) {
                boolean parentIsRoot = "root".equals(supplementalDataInfo.getExplicitParentLocale(locale));
                if (parentIsRoot == isDefaultContent) {
                    errln("Inconsistency between parentLocales and defaultContents: " + locale
                        + (parentIsRoot ? " +" : " -") + "parentIsRoot"
                        + (isDefaultContent ? " +" : " -") + "isDefaultContent");
                }

                // we'd better have a separate likelySubtag
                if (parentIsRoot && !hasLikelySubtag) {
                    errln("Missing likely subtags for: " + locale + " "
                        + TestInheritance.suggestLikelySubtagFor(locale));
                }
            }

            // verify that likelySubtags has all the languages

            if (!hasScript && !hasRegion) {
                if (!hasLikelySubtag) {
                    errln("Missing likely subtags for: " + locale + " "
                        + TestInheritance.suggestLikelySubtagFor(locale));
                }
            }
        }
    }

    public void TestCanonicalizer() {
        LanguageTagCanonicalizer canonicalizer = new LanguageTagCanonicalizer();
        String[][] tests = {
            { "iw", "he" },
            { "no-YU", "nb_RS" },
            { "no", "nb" },
            { "eng-833", "en_IM" },
        };
        for (String[] pair : tests) {
            String actual = canonicalizer.transform(pair[0]);
            assertEquals("Canonical", pair[1], actual);
        }
    }

    public void TestBrackets() {
        String[][] tests = {
            { "language", "en", "Anglish (abc)", "en", "Anglish [abc]",
                "〖?Anglish [abc]?❬ (U.S. [ghi])❭〗〖?Anglish [abc]?❬ (Latine [def])❭〗〖?Anglish [abc]?❬ (Latine [def], U.S. [ghi])❭〗〖❬Langue: ❭?Anglish (abc)?〗" },
            { "script", "Latn", "Latine (def)", "en_Latn", "Anglish [abc] (Latine [def])",
                "〖❬Anglish [abc] (❭?Latine [def]?❬)❭〗〖❬Anglish [abc] (❭?Latine [def]?❬, U.S. [ghi])❭〗〖❬Scripte: ❭?Latine (def)?〗" },
            { "territory", "US", "U.S. (ghi)", "en_Latn_US", "Anglish [abc] (Latine [def], U.S. [ghi])",
                "〖❬Anglish [abc] (❭?U.S. [ghi]?❬)❭〗〖❬Anglish [abc] (Latine [def], ❭?U.S. [ghi]?❬)❭〗〖❬Territorie: ❭?U.S. (ghi)?〗" },
            { null, null, null, "en_US", "Anglish [abc] (U.S. [ghi])", null },
            { "variant", "foobar", "foo (jkl)", "en_foobar", "Anglish [abc] (foo [jkl])", null },
            { "key", "co", "sort (mno)", "en_foobar@co=FOO", "Anglish [abc] (foo [jkl], sort [mno]=FOO)", null },
            { "type|key", "FII|co", "sortfii (mno)", "en_foobar@co=FII", "Anglish [abc] (foo [jkl], sortfii [mno])", null },
        };
        // load up a dummy source
        SimpleXMLSource dxs = new SimpleXMLSource("xx");
        for (String[] row : tests) {
            if (row[0] == null) {
                continue;
            }
            int typeCode = CLDRFile.typeNameToCode(row[0]);
            String path = CLDRFile.getKey(typeCode, row[1]);
            dxs.putValueAtDPath(path, row[2]);
        }
        // create a cldrfile from it and test
        SimpleXMLSource root = new SimpleXMLSource("root");
        root.putValueAtDPath("//ldml/localeDisplayNames/localeDisplayPattern/localePattern", "{0} ({1})");
        root.putValueAtDPath("//ldml/localeDisplayNames/localeDisplayPattern/localeSeparator", "{0}, {1}");
        root.putValueAtDPath("//ldml/localeDisplayNames/codePatterns/codePattern[@type=\"language\"]", "Langue: {0}");
        root.putValueAtDPath("//ldml/localeDisplayNames/codePatterns/codePattern[@type=\"script\"]", "Scripte: {0}");
        root.putValueAtDPath("//ldml/localeDisplayNames/codePatterns/codePattern[@type=\"territory\"]", "Territorie: {0}");
        CLDRFile f = new CLDRFile(dxs, root);
        ExampleGenerator eg = new ExampleGenerator(f, testInfo.getEnglish(), CldrUtility.DEFAULT_SUPPLEMENTAL_DIRECTORY);
        for (String[] row : tests) {
            if (row[0] != null) {
                int typeCode = CLDRFile.typeNameToCode(row[0]);
                String standAlone = f.getName(typeCode, row[1]);
                assertEquals("stand-alone " + row[3], row[2], standAlone);
                if (row[5] != null) {
                    String path = CLDRFile.getKey(typeCode, row[1]);
                    String example = eg.getExampleHtml(path, "?" + row[2] + "?");
                    assertEquals("example " + row[3], row[5], ExampleGenerator.simplify(example));
                }
            }
            String displayName = f.getName(row[3], true, "{0}={1}", "{0} ({1})", "{0}, {1}");
            assertEquals("locale " + row[3], row[4], displayName);
        }
    }

    public void TestLocaleNamePattern() {
        assertEquals("Locale name", "Chinese", testInfo.getEnglish().getName("zh"));
        assertEquals("Locale name", "Chinese (United States)", testInfo.getEnglish().getName("zh-US"));
        assertEquals("Locale name", "Chinese (Arabic, United States)", testInfo.getEnglish().getName("zh-Arab-US"));
        CLDRFile japanese = testInfo.getCldrFactory().make("ja", true);
        assertEquals("Locale name", "中国語", japanese.getName("zh"));
        assertEquals("Locale name", "中国語 (アメリカ合衆国)", japanese.getName("zh-US"));
        assertEquals("Locale name", "中国語 (アラビア文字\u3001アメリカ合衆国)", japanese.getName("zh-Arab-US"));
    }

    public void TestExtendedLanguage() {
        assertEquals("Extended language translation", "Simplified Chinese", testInfo.getEnglish().getName("zh_Hans"));
        assertEquals("Extended language translation", "Simplified Chinese (Singapore)",
            testInfo.getEnglish().getName("zh_Hans_SG"));
        assertEquals("Extended language translation", "American English", testInfo.getEnglish().getName("en-US"));
        assertEquals("Extended language translation", "American English (Arabic)",
            testInfo.getEnglish().getName("en-Arab-US"));
    }

    public void TestNarrowEnough() {
        BreakIterator bi = BreakIterator.getCharacterInstance(ULocale.ENGLISH);
        assertEquals("Narrow Enough", 1, CheckDates.isNarrowEnough("a", bi));
        assertEquals("Narrow Enough", 2, CheckDates.isNarrowEnough("ab", bi));
        assertEquals("Narrow Enough", 2, CheckDates.isNarrowEnough("abc", bi));
        assertEquals("Narrow Enough", 4, CheckDates.isNarrowEnough("a\u0308b\u0308", bi));
        assertEquals("Narrow Enough", 4, CheckDates.isNarrowEnough("a\u0308b\u0308c\u0308", bi));
    }
}
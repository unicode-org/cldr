package org.unicode.cldr.unittest;

import java.util.Map;
import java.util.Set;

import org.unicode.cldr.test.CheckDates;
import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.LanguageTagCanonicalizer;
import org.unicode.cldr.util.LanguageTagParser;
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

    public void TestLocaleNamePattern() {
        assertEquals("Locale name", "Chinese", testInfo.getEnglish().getName("zh"));
        assertEquals("Locale name", "Chinese (United States)", testInfo.getEnglish().getName("zh-US"));
        assertEquals("Locale name", "Chinese (Arabic, United States)", testInfo.getEnglish().getName("zh-Arab-US"));
        CLDRFile japanese = testInfo.getCldrFactory().make("ja", true);
        assertEquals("Locale name", "中国語", japanese.getName("zh"));
        assertEquals("Locale name", "中国語(アメリカ合衆国)", japanese.getName("zh-US"));
        assertEquals("Locale name", "中国語(アラビア文字\uFF0Cアメリカ合衆国)", japanese.getName("zh-Arab-US"));
    }

    public void TestExtendedLanguage() {
        assertEquals("Extended language translation", "Simplified Chinese", testInfo.getEnglish().getName("zh_Hans"));
        assertEquals("Extended language translation", "Simplified Chinese (Singapore)",
            testInfo.getEnglish().getName("zh_Hans_SG"));
        assertEquals("Extended language translation", "U.S. English", testInfo.getEnglish().getName("en-US"));
        assertEquals("Extended language translation", "U.S. English (Arabic)",
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
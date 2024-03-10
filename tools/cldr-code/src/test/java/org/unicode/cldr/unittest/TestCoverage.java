package org.unicode.cldr.unittest;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CoreCoverageInfo;
import org.unicode.cldr.util.CoreCoverageInfo.CoreItems;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;

public class TestCoverage extends TestFmwkPlus {
    private static final boolean DEBUG = false;
    private static final boolean SHOW_LSR_DATA = false;

    static final StandardCodes sc = StandardCodes.make();
    static final CLDRConfig testInfo = CLDRConfig.getInstance();
    static final SupplementalDataInfo sdi = testInfo.getSupplementalDataInfo();

    public static void main(String[] args) {
        new TestCoverage().run(args);
    }

    static Set<CoreItems> all = Collections.unmodifiableSet(EnumSet.allOf(CoreItems.class));
    static Set<CoreItems> none = Collections.unmodifiableSet(EnumSet.noneOf(CoreItems.class));

    public void TestBasic() {
        CLDRFile engCldrFile = testInfo.getEnglish();
        Multimap<CoreItems, String> errors = LinkedHashMultimap.create();
        Set<CoreItems> coreCoverage = CoreCoverageInfo.getCoreCoverageInfo(engCldrFile, errors);
        if (!assertEquals("English should be complete", all, coreCoverage)) {
            showDiff("Missing", all, coreCoverage);
        }
        CLDRFile skimpyLocale = testInfo.getCldrFactory().make("asa", false);
        errors.clear();
        coreCoverage = CoreCoverageInfo.getCoreCoverageInfo(skimpyLocale, errors);
        if (!assertEquals("Skimpy locale should not be complete", none, coreCoverage)) {
            showDiff("Missing", all, coreCoverage);
            showDiff("Extra", coreCoverage, none);
        }
    }

    public void TestSelected() {
        Object[][] tests = {
            {
                "en",
                "//ldml/localeDisplayNames/subdivisions/subdivision[@type=\"gbeng\"]",
                Level.MODERN,
                8
            },
            {
                "en",
                "//ldml/numbers/minimalPairs/ordinalMinimalPairs[@ordinal=\"other\"]",
                Level.MODERATE,
                20
            },
            {
                "en",
                "//ldml/numbers/minimalPairs/pluralMinimalPairs[@count=\"other\"]",
                Level.MODERATE,
                20
            },
        };
        PathHeader.Factory phf = PathHeader.getFactory(testInfo.getEnglish());
        for (Object[] test : tests) {
            String localeId = (String) test[0];
            String path = (String) test[1];
            Level expectedLevel = (Level) test[2];
            int expectedVotes = (Integer) test[3];
            CoverageLevel2 coverageLevel = CoverageLevel2.getInstance(sdi, localeId);
            Level level = coverageLevel.getLevel(path);
            PathHeader ph = phf.fromPath(path);
            assertEquals(localeId + " : " + path + " : ", expectedLevel, level);
            CLDRLocale loc = CLDRLocale.getInstance(localeId);
            int actualVotes = sdi.getRequiredVotes(loc, ph);
            assertEquals(localeId + " : " + path + " : ", expectedVotes, actualVotes);
        }
    }

    final ImmutableSet<CoreItems> nonCore =
            ImmutableSet.copyOf(
                    Sets.difference(
                            CoreItems.ALL, Set.copyOf(CoreItems.LEVEL_TO_ITEMS.get(Level.CORE))));

    public void TestLocales() {
        long start = System.currentTimeMillis();

        Factory fullCldrFactory = testInfo.getFullCldrFactory();
        CLDRFile testFile0 = fullCldrFactory.make("yi", true);
        checkLocale(testFile0);

        for (String locale : fullCldrFactory.getAvailable()) {
            if (CLDRLocale.getInstance(locale).getParent() != CLDRLocale.ROOT) {
                continue;
            }
            CLDRFile testFile = fullCldrFactory.make(locale, true);
            checkLocale(testFile);
        }
        long end = System.currentTimeMillis();
        logln("Elapsed:\t" + (end - start));
    }

    public Multimap<CoreItems, String> checkLocale(CLDRFile testFile) {
        Multimap<CoreItems, String> errors = TreeMultimap.create();
        String locale = testFile.getLocaleID();

        try {
            CoreCoverageInfo.getCoreCoverageInfo(testFile, errors);
        } catch (Exception e) {
            errln(
                    "Failure for locale: "
                            + getLocaleAndName(locale)
                            + ", can't access CoreCoverageInfo.getCoreCoverageInfo");
            e.printStackTrace();
            return errors;
        }
        // Tried errors.removeAll(nonCore), but doesn't remove all keys; rather all values for 1 key
        for (CoreItems x : nonCore) {
            errors.removeAll(x);
        }
        assertEquals("Core items " + getLocaleAndName(locale), ImmutableMultimap.of(), errors);
        return errors;
    }

    private String getLocaleAndName(String locale) {
        return locale + "\t" + testInfo.getEnglish().getName(locale);
    }

    private String showColumn(Set items) {
        StringBuilder result = new StringBuilder();
        for (CoreItems x : CoreItems.values()) {
            result.append("\t");
            if (items.contains(x)) {
                result.append(x);
            }
        }
        return result.toString();
    }

    public void showDiff(String title, Set<CoreItems> all, Set<CoreItems> coreCoverage) {
        Set<CoreItems> diff = EnumSet.copyOf(all);
        diff.removeAll(coreCoverage);
        if (diff.size() != 0) {
            errln("\t" + title + ": " + diff);
        }
    }

    public void testBeaufort() {
        // locale, path, expected coverage
        String[][] tests = {
            {
                "am",
                "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"speed-beaufort\"]/displayName",
                "comprehensive"
            },
            {
                "de",
                "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"speed-beaufort\"]/displayName",
                "modern"
            },
        };
        for (String[] test : tests) {
            String locale = test[0];
            String path = test[1];
            Level expected = Level.fromString(test[2]);
            CoverageLevel2 coverageLevel = CoverageLevel2.getInstance(sdi, locale);
            Level actual = coverageLevel.getLevel(path);
            assertEquals(String.format("locale:%s, path:%s", locale, path), expected, actual);
        }
    }
}

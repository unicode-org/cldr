package org.unicode.cldr.unittest;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CoreCoverageInfo;
import org.unicode.cldr.util.CoreCoverageInfo.CoreItems;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XMLSource;

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

    public void TestLocales() {
        long start = System.currentTimeMillis();
        logln("Status\tLocale\tName\tLevel\tCount" + showColumn(all) + "\tError Messages");
        Multimap<CoreItems, String> errors = LinkedHashMultimap.create();

        Factory fullCldrFactory = testInfo.getFullCldrFactory();
        for (String locale : fullCldrFactory.getAvailable()) {
            if (!XMLSource.ROOT_ID.equals(LocaleIDParser.getParent(locale))) {
                continue;
            }
            Level level = sc.getLocaleCoverageLevel(Organization.cldr, locale);
            if (level == Level.UNDETERMINED || level == Level.CORE) {
                level = Level.BASIC;
            }
            final ImmutableSet<CoreItems> targetCoreItems =
                    ImmutableSet.copyOf(CoreItems.LEVEL_TO_ITEMS.get(level));

            CLDRFile testFile = fullCldrFactory.make(locale, true);
            errors.clear();
            try {
                CoreCoverageInfo.getCoreCoverageInfo(testFile, errors);
            } catch (Exception e) {
                errln("Failure for locale: " + getLocaleAndName(locale));
                e.printStackTrace();
                continue;
            }
            final Set<CoreItems> coreMissing = Sets.intersection(errors.keySet(), targetCoreItems);
            final String message =
                    "\t"
                            + getLocaleAndName(locale) //
                            + "\t"
                            + level //
                            + "\t"
                            + coreMissing.size() //
                            + "\t"
                            + coreMissing
                            + "\t"
                            + errors.entries().stream()
                                    .filter(x -> coreMissing.contains(x.getKey()))
                                    .collect(Collectors.toUnmodifiableSet());
            if (!coreMissing.isEmpty()) {
                warnln(message);
            } else {
                logln("OK" + message);
            }
        }
        long end = System.currentTimeMillis();
        logln("Elapsed:\t" + (end - start));
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
}

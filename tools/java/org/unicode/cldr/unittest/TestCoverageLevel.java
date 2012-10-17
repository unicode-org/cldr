package org.unicode.cldr.unittest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.test.CoverageLevel;
import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.Counter2;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.RegexLookup.Finder;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo.CurrencyDateInfo;
import org.unicode.cldr.util.SupplementalDataInfo.OfficialStatus;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;
import org.unicode.cldr.util.Timer;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.text.CompactDecimalFormat;
import com.ibm.icu.text.CompactDecimalFormat.CompactStyle;
import com.ibm.icu.text.Transform;
import com.ibm.icu.util.ULocale;

public class TestCoverageLevel extends TestFmwk {

    private static TestInfo testInfo = TestInfo.getInstance();

    public static void main(String[] args) throws IOException {
        new TestCoverageLevel().getStarred("en", true);
        if (true) return;
        new TestCoverageLevel().getOrgs();
        new TestCoverageLevel().run(args);
    }

    private void getOrgs() {
        StandardCodes sc = StandardCodes.make();
        Set<R4<String, Level, String, String>> sorted = new TreeSet<R4<String, Level, String, String>>();
        Map<String, String> mapped = new TreeMap();
        for (String org : sc.getLocaleCoverageOrganizations()) {
            for (String locale : sc.getLocaleCoverageLocales(org)) {
                Level level = sc.getLocaleCoverageLevel(org, locale);
                final String name = testInfo.getEnglish().getName(locale);
                final R4<String, Level, String, String> row = Row.of(org, level, locale, name);
                mapped.put(name, locale);
                sorted.add(row);
            }
        }
        for (R4<String, Level, String, String> item : sorted) {
            System.out.println(item);
        }

        for (String org : sc.getLocaleCoverageOrganizations()) {
            System.out.print("\t" + org);
        }
        System.out.println();

        for (Entry<String, String> entry : mapped.entrySet()) {
            String name = entry.getKey();
            String locale = entry.getValue();
            System.out.print(name);
            for (String org : sc.getLocaleCoverageOrganizations()) {
                Level level = sc.getLocaleCoverageLevel(org, locale);
                System.out.print("\t" + (level == Level.UNDETERMINED ? "n/a" : level));
            }
            System.out.println();
        }
    }

    public void TestNewVsOld() {
        checkNewVsOld("en");
    }

    private void checkNewVsOld(String locale) {
        CoverageLevel coverageLevel1 = new CoverageLevel(testInfo.getCldrFactory());

        Map options = new TreeMap();
        List possibleErrors = new ArrayList();
        ULocale ulocale = new ULocale(locale);

        CLDRFile cldrFileToCheck = testInfo.getCldrFactory().make(locale, true);
        coverageLevel1.setFile(cldrFileToCheck, options, null, possibleErrors);

        Map<Row.R2<Level, Level>, Relation<String, String>> failures = new TreeMap<Row.R2<Level, Level>, Relation<String, String>>(); // Relation.of(new
                                                                                                                                      // HashMap<Row.R2<Level,
                                                                                                                                      // Integer>,
                                                                                                                                      // Set<Relation<String,String>>>(),
                                                                                                                                      // HashSet.class);

        int failureCount = 0;
        int successCount = 0;
        int count = 0;
        PathStarrer pathStarrer = new PathStarrer();

        for (String path : cldrFileToCheck) {
            ++count;
            if (0 == (count & 0xFF)) {
                logln(count + ", " + successCount + ", " + failureCount);
            }
            String fullPath = cldrFileToCheck.getFullXPath(path);
            if (fullPath == null) {
                continue;
            }
            // if (path.contains("ethiopic")) {
            // System.out.println("?");
            // }
            Level level = coverageLevel1.getCoverageLevel(fullPath);

            Level newLevel = Level.fromLevel(testInfo.getSupplementalDataInfo().getCoverageValue(path, ulocale));
            if (newLevel != level) {
                Level exceptionLevel = exceptions.get(fullPath);
                if (newLevel == exceptionLevel) {
                    level = exceptionLevel;
                    ++successCount;
                } else {
                    ++failureCount;
                }
            } else {
                ++successCount;
            }
            R2<Level, Level> key = Row.of(level, newLevel);
            String starredPath = pathStarrer.set(path);
            Relation<String, String> starredToAttributes = failures.get(key);
            if (starredToAttributes == null) {
                failures.put(key, starredToAttributes = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class));
            }
            starredToAttributes.put(starredPath, pathStarrer.getAttributesString("|"));
        }
        if (failureCount != 0) {
            errln("Differences between new and old: " + failureCount + ", same: " + successCount);
        } else {
            logln("Same value: " + successCount);
        }
        if (params.verbose) {
            for (int i = 0; i < 2; ++i) {
                for (Entry<R2<Level, Level>, Relation<String, String>> entry : failures.entrySet()) {
                    R2<Level, Level> levels = entry.getKey();
                    Relation<String, String> starredToAttributes = entry.getValue();
                    Level oldLevel = levels.get0();
                    Level newLevel = levels.get1();
                    if ((i == 0) == (oldLevel == newLevel)) {
                        continue;
                    }
                    // logln("\tCount:\t" + starredToAttributes.values().size() + ",\tOld level:\t" + oldLevel + " (=" +
                    // oldLevelInt + ")" + ",\tNew level:\t" + newLevel);
                    // int maxCount = 10;
                    for (Entry<String, Set<String>> s : starredToAttributes.keyValuesSet()) {
                        // if (--maxCount < 0) {
                        // logln("\t...");
                        // break;
                        // }
                        String valueSample = s.getValue().toString();
                        if (valueSample.length() > 100) {
                            valueSample = valueSample.substring(0, 100) + "…";
                        }
                        logln((oldLevel == null ? "?" : oldLevel.getLevel())
                            + "\t" + (newLevel == null ? "?" : newLevel.getLevel())
                            + "\t" + s.getKey()
                            + "\t" + valueSample);
                    }
                }
            }
        }
    }

    private static void getStarred(String locale, boolean longForm) {
        ULocale ulocale = new ULocale(locale);
        CoverageLevel2 coverageLevel2 = CoverageLevel2.getInstance(locale);

        CLDRFile cldrFileToCheck = testInfo.getCldrFactory().make(locale, true);

        Map<Level, Relation<String, String>> data = new TreeMap<Level, Relation<String, String>>(); // Relation.of(new
                                                                                                    // HashMap<Row.R2<Level,
                                                                                                    // Integer>,
                                                                                                    // Set<Relation<String,String>>>(),
                                                                                                    // HashSet.class);

        int count = 0;
        PathStarrer pathStarrer = new PathStarrer();
        Status status = new Status();

        for (String path : cldrFileToCheck) {
            ++count;
            if (path.contains("/alias")) {
                continue;
            }
            String localeFound = cldrFileToCheck.getSourceLocaleID(path, status);
            if (status.pathWhereFound != path) {
                continue;
            }

            String fullPath = cldrFileToCheck.getFullXPath(path);
            if (fullPath == null) {
                continue;
            }
            // if (path.contains("ethiopic")) {
            // System.out.println("?");
            // }
            Level level = coverageLevel2.getLevel(path);

            // R2<Level, Level> key = Row.of(level, newLevel);
            String starredPath = pathStarrer.set(path);
            Relation<String, String> starredToAttributes = data.get(level);
            if (starredToAttributes == null) {
                data.put(level, starredToAttributes = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class));
            }
            starredToAttributes.put(starredPath, pathStarrer.getAttributesString("|"));
        }
        RegexLookup<Transform<String, String>> longTransLookup = new RegexLookup<Transform<String, String>>()
            .add("^//ldml/localeDisplayNames/languages/language", new TypeName(CLDRFile.LANGUAGE_NAME))
            .add("^//ldml/localeDisplayNames/scripts/script", new TypeName(CLDRFile.SCRIPT_NAME))
            .add("^//ldml/localeDisplayNames/territories/territory", new TypeName(CLDRFile.TERRITORY_NAME))
            .add("^//ldml/numbers/currencies/currency", new TypeName(CLDRFile.CURRENCY_NAME));

        for (Entry<Level, Relation<String, String>> entry : data.entrySet()) {
            final Level level = entry.getKey();
            for (Entry<String, Set<String>> entry2 : entry.getValue().keyValuesSet()) {
                final String key = entry2.getKey();
                final Set<String> value = entry2.getValue();
                Transform<String, String> longTrans = null;
                if (longForm && level.compareTo(Level.MODERN) <= 0) {
                    longTrans = longTransLookup.get(key);
                }
                if (longTrans != null) {
                    for (String s : value) {
                        int barPos = s.indexOf('|');
                        String codePart = barPos < 0 ? s : s.substring(0, barPos);
                        System.out.println(level.getLevel() + "\t" + level + "\t" + key + "\t" + s + "\t"
                            + longTrans.transform(codePart));
                    }
                } else {
                    System.out.println(level.getLevel() + "\t" + level + "\t" + key + "\t" + value);
                }
            }
        }
    }

    enum LanguageStatus {
        Lit100M("P1"), Lit10MandOfficial("P2"), Lit1MandOneThird("P3");
        final String name;

        LanguageStatus(String name) {
            this.name = name;
        }
    }

    static Relation<String, LanguageStatus> languageStatus = Relation.of(new HashMap<String, Set<LanguageStatus>>(),
        TreeSet.class);
    static Counter2<String> languageLiteratePopulation = new Counter2();
    static Map<String, Date> currencyToLast = new HashMap();
    static Set<String> officialSomewhere = new HashSet();

    static {
        Counter2<String> territoryLiteratePopulation = new Counter2();
        LanguageTagParser parser = new LanguageTagParser();
        // cf http://cldr.unicode.org/development/development-process/design-proposals/languages-to-show-for-translation
        for (String language : testInfo.getSupplementalDataInfo().getLanguagesForTerritoriesPopulationData()) {
            String base = parser.set(language).getLanguage();
            boolean isOfficial = false;
            double languageLiterate = 0;
            for (String territory : testInfo.getSupplementalDataInfo().getTerritoriesForPopulationData(language)) {
                PopulationData pop = testInfo.getSupplementalDataInfo().getLanguageAndTerritoryPopulationData(language,
                    territory);
                OfficialStatus officialStatus = pop.getOfficialStatus();
                if (officialStatus.compareTo(OfficialStatus.de_facto_official) >= 0) {
                    isOfficial = true;
                    languageStatus.put(base + "_" + territory, LanguageStatus.Lit10MandOfficial);
                    officialSomewhere.add(base);
                }
                double litPop = pop.getLiteratePopulation();
                languageLiterate += litPop;
                territoryLiteratePopulation.add(territory, litPop);
                languageLiteratePopulation.add(base + "_" + territory, litPop);
            }
            languageLiteratePopulation.add(base, languageLiterate);
            if (languageLiterate > 100000000) {
                languageStatus.put(base, LanguageStatus.Lit100M);
            }
            if (languageLiterate > 10000000 && isOfficial) {
                languageStatus.put(base, LanguageStatus.Lit10MandOfficial);
            }
        }
        for (String language : testInfo.getSupplementalDataInfo().getLanguagesForTerritoriesPopulationData()) {
            if (languageLiteratePopulation.getCount(language) < 1000000) {
                continue;
            }
            String base = parser.set(language).getLanguage();
            for (String territory : testInfo.getSupplementalDataInfo().getTerritoriesForPopulationData(language)) {
                PopulationData pop = testInfo.getSupplementalDataInfo().getLanguageAndTerritoryPopulationData(language,
                    territory);
                double litPop = pop.getLiteratePopulation();
                double total = territoryLiteratePopulation.getCount(territory);
                if (litPop > total / 3) {
                    languageStatus.put(base, LanguageStatus.Lit1MandOneThird);
                }
            }
        }
        for (String territory : testInfo.getStandardCodes().getAvailableCodes("territory")) {
            Set<CurrencyDateInfo> cdateInfo = testInfo.getSupplementalDataInfo().getCurrencyDateInfo(territory);
            if (cdateInfo == null) {
                continue;
            }
            for (CurrencyDateInfo dateInfo : cdateInfo) {
                String currency = dateInfo.getCurrency();
                Date last = dateInfo.getEnd();
                Date old = currencyToLast.get(currency);
                if (old == null || old.compareTo(last) < 0) {
                    currencyToLast.put(currency, last);
                }
            }
        }
    }

    static CompactDecimalFormat cdf = CompactDecimalFormat.getInstance(ULocale.ENGLISH, CompactStyle.SHORT);

    static String isBigLanguage(String lang) {
        Set<LanguageStatus> status = languageStatus.get(lang);
        Double size = languageLiteratePopulation.getCount(lang);
        String sizeString = size == null ? "?" : cdf.format(size);
        String off = officialSomewhere.contains(lang) ? "o" : "";
        if (status == null || status.isEmpty()) {
            return "P4-" + sizeString + off;
        }
        return status.iterator().next().name + "-" + sizeString + off;
    }

    static final Date NOW = new Date();

    static class TypeName implements Transform<String, String> {
        private final int field;
        private final Map<String, R2<List<String>, String>> dep;

        public TypeName(int field) {
            this.field = field;
            switch (field) {
            case CLDRFile.LANGUAGE_NAME:
                dep = testInfo.getSupplementalDataInfo().getLocaleAliasInfo().get("language");
                break;
            case CLDRFile.TERRITORY_NAME:
                dep = testInfo.getSupplementalDataInfo().getLocaleAliasInfo().get("territory");
                break;
            case CLDRFile.SCRIPT_NAME:
                dep = testInfo.getSupplementalDataInfo().getLocaleAliasInfo().get("script");
                break;
            default:
                dep = null;
                break;
            }
        }

        public String transform(String source) {
            String result = testInfo.getEnglish().getName(field, source);
            String extra = "";
            if (field == CLDRFile.LANGUAGE_NAME) {
                String lang = isBigLanguage(source);
                extra = lang == null ? "X" : lang;
            } else if (field == CLDRFile.CURRENCY_NAME) {
                Date last = currencyToLast.get(source);
                extra = last == null ? "?" : last.compareTo(NOW) < 0 ? "old" : "";
            }
            R2<List<String>, String> depValue = dep == null ? null : dep.get(source);
            if (depValue != null) {
                extra += extra.isEmpty() ? "" : "-";
                extra += depValue.get1();
            }
            return result + (extra.isEmpty() ? "" : "\t" + extra);
        }
    }

    public void TestTime() {
        checkTime("en");
    }

    private void checkTime(String locale) {
        Map options = new TreeMap();
        List possibleErrors = new ArrayList();
        ULocale ulocale = new ULocale(locale);

        CLDRFile cldrFileToCheck = testInfo.getCldrFactory().make(locale, true);
        CoverageLevel coverageLevel1 = new CoverageLevel(testInfo.getCldrFactory());

        Timer t = new Timer();
        coverageLevel1.setFile(cldrFileToCheck, options, null, possibleErrors);
        for (String path : cldrFileToCheck) {
            String fullPath = cldrFileToCheck.getFullXPath(path);
            if (fullPath == null) {
                continue;
            }
            Level level = coverageLevel1.getCoverageLevel(fullPath);
        }
        long oldTime = t.getDuration();
        logln("Old time:\t" + t.toString());

        t.start();
        coverageLevel1.setFile(cldrFileToCheck, options, null, possibleErrors);
        for (String path : cldrFileToCheck) {
            String fullPath = cldrFileToCheck.getFullXPath(path);
            if (fullPath == null) {
                continue;
            }
            int newLevel = testInfo.getSupplementalDataInfo().getCoverageValue(path, ulocale);
        }
        double newTime = t.getDuration();
        if (newTime > 2 * oldTime) {
            errln("New Coverage Level time too big " + t.toString(1, oldTime));
        } else {
            logln("Old time:\t" + t.toString(1, oldTime));
        }
    }

    RegexLookup<Level> exceptions = RegexLookup.of(null, new Transform<String, Level>() {
        public Level transform(String source) {
            return Level.fromLevel(Integer.parseInt(source));
        }
    }, null)
        .loadFromFile(TestCoverageLevel.class, "TestCoverageLevel.txt");
    {
        for (R2<Finder, Level> x : exceptions) {
            System.out.println(x.get0().toString() + " => " + x.get1());
        }
    }
}

package org.unicode.cldr.unittest;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R4;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.tool.LikelySubtags;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CoreCoverageInfo;
import org.unicode.cldr.util.CoreCoverageInfo.CoreItems;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.XPathParts;

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

    static class CoverageStatus {

        private Level level;
        private boolean inRoot;
        private boolean inId;
        private Level languageLevel;
        private String displayName;

        public CoverageStatus(
                Level level,
                boolean inRoot,
                boolean inId,
                Level languageLevel,
                String displayName) {
            this.level = level;
            this.inRoot = inRoot;
            this.inId = inId;
            this.languageLevel = languageLevel == null ? Level.UNDETERMINED : languageLevel;
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return (inRoot ? "root" : "x")
                    + "\t"
                    + (inId ? "ids" : "x")
                    + "\t"
                    + stringForm(languageLevel)
                    + "\t"
                    + stringForm(level)
                    + "\t"
                    + displayName;
        }

        private String stringForm(Level level2) {
            if (level == null) {
                return "υnd";
            }
            switch (level2) {
                case UNDETERMINED:
                    return "υnd";
                case COMPREHENSIVE:
                    return "ϲomp";
                default:
                    return level2.toString();
            }
        }
    }

    public void testLSR() {
        SupplementalDataInfo supplementalData = testInfo.getSupplementalDataInfo();
        Factory factory = testInfo.getCldrFactory();
        CLDRFile root = factory.make("root", true);
        CoverageLevel2 coverageLevel =
                CoverageLevel2.getInstance(supplementalData, "yyy"); // non-existant locale

        Set<String> langsRoot = new TreeSet<>();
        Set<String> scriptsRoot = new TreeSet<>();
        Set<String> regionsRoot = new TreeSet<>();

        // Get root LSR codes

        for (String path : root) {
            if (!path.startsWith("//ldml/localeDisplayNames/")) {
                continue;
            }
            XPathParts parts = XPathParts.getFrozenInstance(path);
            String code = parts.getAttributeValue(3, "type");
            if (code == null || code.contains("_")) {
                continue;
            }
            switch (parts.getElement(3)) {
                case "language":
                    langsRoot.add(code);
                    break;
                case "script":
                    scriptsRoot.add(code);
                    break;
                case "territory":
                    regionsRoot.add(code);
                    break;
            }
        }
        langsRoot = ImmutableSet.copyOf(langsRoot);
        scriptsRoot = ImmutableSet.copyOf(scriptsRoot);
        regionsRoot = ImmutableSet.copyOf(regionsRoot);

        // get CLDR locale IDs' codes

        Map<String, Level> langs = new TreeMap<>();
        Map<String, Level> scripts = new TreeMap<>();
        Map<String, Level> regions = new TreeMap<>();
        LikelySubtags likely = new LikelySubtags();

        LanguageTagParser ltp = new LanguageTagParser();
        for (String locale : factory.getAvailable()) {
            Level languageLevel = sc.getLocaleCoverageLevel(Organization.cldr, locale);
            if (languageLevel == null || languageLevel == Level.UNDETERMINED) {
                languageLevel = Level.CORE;
            }
            ltp.set(locale);
            likely.maximize(ltp);
            addBestLevel(langs, ltp.getLanguage(), languageLevel);
            addBestLevel(scripts, ltp.getScript(), languageLevel);
            addBestLevel(regions, ltp.getRegion(), languageLevel);
        }
        regions.remove("");
        scripts.remove("");

        // get the data

        Map<String, CoverageStatus> data = new TreeMap<>();

        ImmutableMap<Integer, R4<String, Map<String, Level>, Set<String>, Level>> typeToInfo =
                ImmutableMap.of(
                        CLDRFile.LANGUAGE_NAME,
                        Row.of("language", langs, langsRoot, Level.MODERN),
                        CLDRFile.SCRIPT_NAME,
                        Row.of("script", scripts, scriptsRoot, Level.MODERATE),
                        CLDRFile.TERRITORY_NAME,
                        Row.of("region", regions, regionsRoot, Level.MODERATE));

        for (Entry<Integer, R4<String, Map<String, Level>, Set<String>, Level>> typeAndInfo :
                typeToInfo.entrySet()) {
            int type = typeAndInfo.getKey();
            String name = typeAndInfo.getValue().get0();
            Map<String, Level> idPartMap = typeAndInfo.getValue().get1();
            Set<String> setRoot = typeAndInfo.getValue().get2();
            Level targetLevel = typeAndInfo.getValue().get3();
            for (String code : Sets.union(idPartMap.keySet(), setRoot)) {
                String displayName = testInfo.getEnglish().getName(type, code);
                String path = CLDRFile.getKey(type, code);
                Level level = coverageLevel.getLevel(path);
                data.put(
                        name + "\t" + code,
                        new CoverageStatus(
                                level,
                                setRoot.contains(code),
                                idPartMap.containsKey(code),
                                idPartMap.get(code),
                                displayName));
            }
        }
        if (SHOW_LSR_DATA) {

            System.out.println(
                    "\nType\tCode\tIn Root\tIn CLDR Locales\tCLDR TargeLevel\tRoot Path Level\tCombinations");
            for (Entry<String, CoverageStatus> entry : data.entrySet()) {
                System.out.println(entry.getKey() + "\t" + entry.getValue());
            }
            System.out.println();
            for (Entry<String, CoverageStatus> entry : data.entrySet()) {
                final String key = entry.getKey();
                if (!key.startsWith("language")) {
                    continue;
                }
                final CoverageStatus value = entry.getValue();
                if (value.inId) {
                    continue;
                }
                String[] parts = key.split("\t");
                PopulationData population = sdi.getBaseLanguagePopulationData(parts[1]);
                if (population == null) {
                    System.out.println(key + "\t" + value.displayName + "\t" + value + "\t-1\t-1");
                } else {
                    System.out.println(
                            key
                                    + "\t"
                                    + value.displayName
                                    + "\t"
                                    + value
                                    + "\t"
                                    + population.getPopulation()
                                    + "\t"
                                    + population.getLiteratePopulation());
                }
            }
        }

        Set<String> ids = new TreeSet<>();
        Set<String> missing = new TreeSet<>();
        for (Entry<String, CoverageStatus> entry : data.entrySet()) {
            final String key = entry.getKey();
            if (!key.startsWith("language")) {
                continue;
            }
            final CoverageStatus value = entry.getValue();
            if (value.inId) {
                String[] parts = key.split("\t");
                ids.add(parts[1]);
                if (!value.inRoot) {
                    missing.add(parts[1]);
                }
            }
        }
        if (!assertEquals(
                "Language subtags that are in a CLDR locale's ID are in root ("
                        + missing.size()
                        + ")",
                "",
                Joiner.on(' ').join(missing))) {
            warnln(
                    "Full set for resetting $language in attributeValueValidity.xml ("
                            + ids.size()
                            + "):"
                            + breakLines(ids, "\n                "));
        }
    }

    private String breakLines(Set<String> ids, String indent) {
        StringBuilder result = new StringBuilder();
        int lastFirstChar = 0;
        for (String id : ids) {
            int firstChar = id.codePointAt(0);
            result.append(firstChar == lastFirstChar ? " " : indent);
            result.append(id);
            lastFirstChar = firstChar;
        }
        return result.toString();
    }

    private void addBestLevel(Map<String, Level> codeToBestLevel, String code, Level level) {
        if (level != Level.UNDETERMINED) {
            int debug = 0;
        }
        Level old = codeToBestLevel.get(code);
        if (old == null) {
            codeToBestLevel.put(code, level);
        } else if (level.compareTo(old) > 0) {
            codeToBestLevel.put(code, level);
        } else if (level != old) {
            int debug = 0;
        }
    }
}

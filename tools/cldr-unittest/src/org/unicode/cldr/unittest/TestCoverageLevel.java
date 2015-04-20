package org.unicode.cldr.unittest;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.ChainedMap;
import org.unicode.cldr.util.ChainedMap.M4;
import org.unicode.cldr.util.Counter2;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.StandardCodes.CodeType;
import org.unicode.cldr.util.StandardCodes.LstrField;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.SupplementalDataInfo.CurrencyNumberInfo;
import org.unicode.cldr.util.XMLSource;
import org.unicode.cldr.util.PathHeader.Factory;
import org.unicode.cldr.util.PathHeader.PageId;
import org.unicode.cldr.util.PathHeader.SectionId;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.RegexLookup;
import org.unicode.cldr.util.RegexLookup.Finder;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.CurrencyDateInfo;
import org.unicode.cldr.util.SupplementalDataInfo.OfficialStatus;
import org.unicode.cldr.util.SupplementalDataInfo.PopulationData;
import org.unicode.cldr.util.XPathParts;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.dev.util.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.text.CompactDecimalFormat;
import com.ibm.icu.text.CompactDecimalFormat.CompactStyle;
import com.ibm.icu.text.Transform;
import com.ibm.icu.util.ULocale;

public class TestCoverageLevel extends TestFmwkPlus {

    private static TestInfo testInfo = TestInfo.getInstance();
    private static final StandardCodes STANDARD_CODES = testInfo.getStandardCodes();
    private static final CLDRFile ENGLISH = testInfo.getEnglish();
    private static final SupplementalDataInfo SDI = testInfo.getSupplementalDataInfo();

    public static void main(String[] args) {
        // TestCoverageLevel.getStarred(true, "en", "de");
        // new TestCoverageLevel().getOrgs();
        new TestCoverageLevel().run(args);
    }

    public void oldTestInvariantPaths() {
        org.unicode.cldr.util.Factory factory = testInfo.getCldrFactory();
        PathStarrer pathStarrer = new PathStarrer().setSubstitutionPattern("*");
        SupplementalDataInfo sdi = SupplementalDataInfo
            .getInstance(CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY);

        Set<String> allPaths = new HashSet<String>();
        M4<String, String, Level, Boolean> starredToLocalesToLevels = ChainedMap
            .of(new TreeMap<String, Object>(),
                new TreeMap<String, Object>(),
                new TreeMap<Level, Object>(), Boolean.class);

        for (String locale : factory.getAvailableLanguages()) {
            logln(locale);
            CLDRFile cldrFileToCheck = factory.make(locale, true);
            for (String path : cldrFileToCheck.fullIterable()) {
                allPaths.add(path);
                String starred = pathStarrer.set(path);
                Level level = sdi.getCoverageLevel(path, locale);
                starredToLocalesToLevels.put(starred, locale, level, true);
            }
        }

        Set<Level> levelsFound = EnumSet.noneOf(Level.class);
        Set<String> localesWithUniqueLevels = new TreeSet<String>();
        for (Entry<String, Map<String, Map<Level, Boolean>>> entry : starredToLocalesToLevels) {
            String starred = entry.getKey();
            Map<String, Map<Level, Boolean>> localesToLevels = entry.getValue();
            int maxLevelCount = 0;
            double localeCount = 0;
            levelsFound.clear();
            localesWithUniqueLevels.clear();

            for (Entry<String, Map<Level, Boolean>> entry2 : localesToLevels
                .entrySet()) {
                String locale = entry2.getKey();
                Map<Level, Boolean> levels = entry2.getValue();
                levelsFound.addAll(levels.keySet());
                if (levels.size() > maxLevelCount) {
                    maxLevelCount = levels.size();
                }
                if (levels.size() == 1) {
                    localesWithUniqueLevels.add(locale);
                }
                localeCount++;
            }
            System.out.println(maxLevelCount
                + "\t"
                + localesWithUniqueLevels.size()
                / localeCount
                + "\t"
                + starred
                + "\t"
                + CollectionUtilities.join(levelsFound, ", ")
                + "\t"
                + (maxLevelCount == 1 ? "all" : localesWithUniqueLevels
                    .size() == 0 ? "none" : CollectionUtilities.join(
                        localesWithUniqueLevels, ", ")));
        }
    }

    private static void getStarred(boolean longForm, String... locales) {
        Map<Level, Relation<String, String>> data = new TreeMap<Level, Relation<String, String>>(); // Relation.of(new
        SupplementalDataInfo sdi = SupplementalDataInfo
            .getInstance(CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY);
        for (String locale : locales) {
            CLDRFile cldrFileToCheck = testInfo.getCldrFactory().make(locale,
                true);

            // HashMap<Row.R2<Level,
            // Integer>,
            // Set<Relation<String,String>>>(),
            // HashSet.class);

            PathStarrer pathStarrer = new PathStarrer();

            for (String path : cldrFileToCheck) {
                if (path.contains("/alias")) {
                    continue;
                }
                String value = cldrFileToCheck.getStringValue(path);
                // cldrFileToCheck.getSourceLocaleID(path, status);
                // if (status.pathWhereFound != path) {
                // continue;
                // }

                String fullPath = cldrFileToCheck.getFullXPath(path);
                if (fullPath == null) {
                    continue;
                }
                // if (path.contains("ethiopic")) {
                // System.out.println("?");
                // }
                Level level = sdi.getCoverageLevel(path, locale);

                // R2<Level, Level> key = Row.of(level, newLevel);
                String starredPath = pathStarrer.set(path);
                Relation<String, String> starredToAttributes = data.get(level);
                if (starredToAttributes == null) {
                    data.put(
                        level,
                        starredToAttributes = Relation.of(
                            new TreeMap<String, Set<String>>(),
                            TreeSet.class));
                }
                starredToAttributes.put(starredPath,
                    pathStarrer.getAttributesString("|") + "=‹" + value
                    + "›");
            }
        }
        RegexLookup<Transform<String, String>> longTransLookup = new RegexLookup<Transform<String, String>>()
            .add("^//ldml/localeDisplayNames/languages/language",
                new TypeName(CLDRFile.LANGUAGE_NAME))
                .add("^//ldml/localeDisplayNames/scripts/script",
                    new TypeName(CLDRFile.SCRIPT_NAME))
                    .add("^//ldml/localeDisplayNames/territories/territory",
                        new TypeName(CLDRFile.TERRITORY_NAME))
                        .add("^//ldml/numbers/currencies/currency",
                            new TypeName(CLDRFile.CURRENCY_NAME));

        for (Entry<Level, Relation<String, String>> entry : data.entrySet()) {
            final Level level = entry.getKey();
            for (Entry<String, Set<String>> entry2 : entry.getValue()
                .keyValuesSet()) {
                final String key = entry2.getKey();
                final Set<String> value = entry2.getValue();
                Transform<String, String> longTrans = null;
                if (longForm && level.compareTo(Level.MODERN) <= 0) {
                    longTrans = longTransLookup.get(key);
                }
                if (longTrans != null) {
                    for (String s : value) {
                        int barPos = s.indexOf('|');
                        String codePart = barPos < 0 ? s : s.substring(0,
                            barPos);
                        System.out.println(level.getLevel() + "\t" + level
                            + "\t" + key + "\t" + s + "\t"
                            + longTrans.transform(codePart));
                    }
                } else {
                    System.out.println(level.getLevel() + "\t" + level + "\t"
                        + key + "\t" + value);
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

    static Relation<String, LanguageStatus> languageStatus = Relation.of(
        new HashMap<String, Set<LanguageStatus>>(), TreeSet.class);
    static Counter2<String> languageLiteratePopulation = new Counter2<String>();
    static Map<String, Date> currencyToLast = new HashMap<String, Date>();
    static Set<String> officialSomewhere = new HashSet<String>();

    static {
        Counter2<String> territoryLiteratePopulation = new Counter2<String>();
        LanguageTagParser parser = new LanguageTagParser();
        // cf
        // http://cldr.unicode.org/development/development-process/design-proposals/languages-to-show-for-translation
        for (String language : SDI
            .getLanguagesForTerritoriesPopulationData()) {
            String base = parser.set(language).getLanguage();
            boolean isOfficial = false;
            double languageLiterate = 0;
            for (String territory : SDI
                .getTerritoriesForPopulationData(language)) {
                PopulationData pop = SDI
                    .getLanguageAndTerritoryPopulationData(language,
                        territory);
                OfficialStatus officialStatus = pop.getOfficialStatus();
                if (officialStatus.compareTo(OfficialStatus.de_facto_official) >= 0) {
                    isOfficial = true;
                    languageStatus.put(base + "_" + territory,
                        LanguageStatus.Lit10MandOfficial);
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
        for (String language : SDI
            .getLanguagesForTerritoriesPopulationData()) {
            if (languageLiteratePopulation.getCount(language) < 1000000) {
                continue;
            }
            String base = parser.set(language).getLanguage();
            for (String territory : SDI
                .getTerritoriesForPopulationData(language)) {
                PopulationData pop = SDI
                    .getLanguageAndTerritoryPopulationData(language,
                        territory);
                double litPop = pop.getLiteratePopulation();
                double total = territoryLiteratePopulation.getCount(territory);
                if (litPop > total / 3) {
                    languageStatus.put(base, LanguageStatus.Lit1MandOneThird);
                }
            }
        }
        for (String territory : STANDARD_CODES.getAvailableCodes(
            "territory")) {
            Set<CurrencyDateInfo> cdateInfo = SDI.getCurrencyDateInfo(territory);
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

    static CompactDecimalFormat cdf = CompactDecimalFormat.getInstance(
        ULocale.ENGLISH, CompactStyle.SHORT);

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
                dep = SDI.getLocaleAliasInfo()
                .get("language");
                break;
            case CLDRFile.TERRITORY_NAME:
                dep = SDI.getLocaleAliasInfo()
                .get("territory");
                break;
            case CLDRFile.SCRIPT_NAME:
                dep = SDI.getLocaleAliasInfo()
                .get("script");
                break;
            default:
                dep = null;
                break;
            }
        }

        public String transform(String source) {
            String result = ENGLISH.getName(field, source);
            String extra = "";
            if (field == CLDRFile.LANGUAGE_NAME) {
                String lang = isBigLanguage(source);
                extra = lang == null ? "X" : lang;
            } else if (field == CLDRFile.CURRENCY_NAME) {
                Date last = currencyToLast.get(source);
                extra = last == null ? "?" : last.compareTo(NOW) < 0 ? "old"
                    : "";
            }
            R2<List<String>, String> depValue = dep == null ? null : dep
                .get(source);
            if (depValue != null) {
                extra += extra.isEmpty() ? "" : "-";
                extra += depValue.get1();
            }
            return result + (extra.isEmpty() ? "" : "\t" + extra);
        }
    }

    RegexLookup<Level> exceptions = RegexLookup.of(null,
        new Transform<String, Level>() {
        public Level transform(String source) {
            return Level.fromLevel(Integer.parseInt(source));
        }
    }, null).loadFromFile(TestCoverageLevel.class,
        "TestCoverageLevel.txt");

    public void TestExceptions() {
        for (Map.Entry<Finder, Level> x : exceptions) {
            logln(x.getKey().toString() + " => " + x.getValue());
        }
    }

    public void TestNarrowCurrencies() {
        String path = "//ldml/numbers/currencies/currency[@type=\"USD\"]/symbol[@alt=\"narrow\"]";
        String value = ENGLISH.getStringValue(path);
        assertEquals("Narrow $", "$", value);
        SupplementalDataInfo sdi = SupplementalDataInfo
            .getInstance(CLDRPaths.DEFAULT_SUPPLEMENTAL_DIRECTORY);
        Level level = sdi.getCoverageLevel(path, "en");
        assertEquals("Narrow $", Level.BASIC, level);
    }

    static final EnumSet<PageId> SKIP_PAGE_OK = EnumSet.of(PageId.Dangi,
        PageId.Islamic, PageId.Islamic_Civil, PageId.Islamic_Rgsa,
        PageId.Islamic_Tbla, PageId.Islamic_Umalqura, PageId.Buddhist,
        PageId.Chinese, PageId.Coptic, PageId.Ethiopic,
        PageId.Ethiopic_Amete_Alem, PageId.Hebrew, PageId.Indian,
        PageId.Japanese, PageId.Persian, PageId.ROC, PageId.Transforms,
        PageId.Identity, PageId.Version);

    public void TestEnglishModern() {
        if (logKnownIssue("Cldrbug:7135",
            "Problems with TestCoverageLevel test")) {
            return;
        }
        SupplementalDataInfo sdi = SDI;
        Factory phf = PathHeader.getFactory(ENGLISH);
        Relation<Row.R3, String> bad = Relation.of(
            new TreeMap<Row.R3, Set<String>>(), TreeSet.class);
        Relation<Row.R3, String> all = Relation.of(
            new TreeMap<Row.R3, Set<String>>(), TreeSet.class);
        XPathParts parts = new XPathParts();

        main: for (String path : ENGLISH.fullIterable()) {
            PathHeader ph = phf.fromPath(path);
            SectionId section = ph.getSectionId();
            PageId page = ph.getPageId();
            String header = ph.getHeader();
            String code = ph.getCode();
            R3<SectionId, PageId, String> row = Row.of(section, page, header);
            all.put(row, code);
            Level coverageLevel = sdi.getCoverageLevel(path, "en");

            if (coverageLevel.compareTo(Level.COMPREHENSIVE) <= 0) {
                continue;
            }

            if (SKIP_PAGE_OK.contains(page)) {
                continue;
            }
            if (header.equals("Alias")) {
                continue;
            }
            switch (page) {
            case Numbering_Systems:
                if (header.startsWith("Standard Patterns when")
                    && !header.contains("Latin")) {
                    continue main;
                }
                break;
            case Compact_Decimal_Formatting:
                if ((header.startsWith("Short Formats when") || header
                    .startsWith("Long Formats when"))
                    && !header.contains("Latin")) {
                    continue main;
                }
                break;
            case Number_Formatting_Patterns:
                if ((header.startsWith("Currency Spacing when")
                    || header.startsWith("Standard Patterns when using")
                    || header.startsWith("Miscellaneous Patterns when") || header
                    .startsWith("Currency Unit Patterns when"))
                    && !header.contains("Latin")) {
                    continue main;
                }
                break;
            case Symbols:
                if ((header.startsWith("Symbols when using"))
                    && !header.contains("Latin")) {
                    continue main;
                }
                break;
            }

            if (SDI.hasDeprecatedItem("ldml",
                parts.set(path))) {
                continue;
            }

            bad.put(row, code);
        }
        all.removeAll(bad);
        for (Entry<R3, Set<String>> item : bad.keyValuesSet()) {
            errln(item.getKey() + "\t" + item.getValue());
        }
        for (Entry<R3, Set<String>> item : all.keyValuesSet()) {
            logln(item.getKey() + "\t" + item.getValue());
        }
    }

    /**
     * Check that English paths are, except for known cases, at least modern coverage.
     * We filter out:
     * <pre>
     *  * deprecated languages/scripts/territories
     *  * old currencies
     *  * anything from CODE_FALLBACK_ID
     *  * anything with a lateral inheritance (path changes)
     * </pre>
     */
    @SuppressWarnings("unchecked")
    public void TestEnglish() {
        if (logKnownIssue("cldrbug:8397", "English Paths without modern coverage")) {
            return;
        }
        Status status = new Status();
        String localeDisplayNames = "//ldml/localeDisplayNames/";
        Map<String, Map<String, R2<List<String>, String>>> aliasInfo = SDI.getLocaleAliasInfo();
        
        Date nowMinus5 = new Date(NOW.getYear()-1,NOW.getMonth(), NOW.getDate());
        Set<String> modernCurrencies = SDI.getCurrentCurrencies(SDI.getCurrencyTerritories(), nowMinus5, NOW);

        for (String path : ENGLISH.fullIterable()) {
            if (path.startsWith(localeDisplayNames)) {
                XPathParts parts = XPathParts.getFrozenInstance(path);
                String element = parts.getElement(-1);
                if (element.equals("currency")) {
                    String type = parts.getAttributeValue(-1, "type");
                    if (!modernCurrencies.contains(type)) {
                        continue; // old currency or not tender, so we don't care
                    }
                } else {
                    Map<String, R2<List<String>, String>> typeToInfo = aliasInfo.get(element);
                    if (typeToInfo != null) {
                        String type = parts.getAttributeValue(-1, "type");
                        R2<List<String>, String> info = typeToInfo.get(type);
                        if (info != null) {
                            // deprecated element, so we don't care
                            continue;
                        }
                    }
                }
            }
//            if (!path.contains("@alt")) {
//                continue;
//            }
            String locale = ENGLISH.getSourceLocaleID(path, status);
            if (locale.equals(XMLSource.CODE_FALLBACK_ID) // don't worry about Code Fallback
                || !path.equals(status.pathWhereFound)) { // don't worry about lateral inheritance
                continue;
            }
            Level level = SDI.getCoverageLevel(path, "en");
            String value = ENGLISH.getStringValue(path);
            assertRelation(locale + ":" + path + "=" + value, true, Level.MODERN, GEQ, level);
        }
    }
}

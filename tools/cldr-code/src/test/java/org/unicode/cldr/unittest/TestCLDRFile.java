package org.unicode.cldr.unittest;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Output;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.tool.LikelySubtags;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.DraftStatus;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.GlossonymConstructor;
import org.unicode.cldr.util.GrammarInfo;
import org.unicode.cldr.util.GrammarInfo.GrammaticalFeature;
import org.unicode.cldr.util.GrammarInfo.GrammaticalTarget;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.PageId;
import org.unicode.cldr.util.PathHeader.SectionId;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.PatternPlaceholders;
import org.unicode.cldr.util.PatternPlaceholders.PlaceholderStatus;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;

/**
 * This is the original TestFwmk test case for CLDRFile.
 *
 * @see {@link org.unicode.cldr.util.TestCLDRFile}
 * @see {@link org.unicode.cldr.util.CLDRFile}
 */
public class TestCLDRFile extends TestFmwk {
    private static final boolean DISABLE_TIL_WORKS = false;

    static CLDRConfig testInfo = CLDRConfig.getInstance();
    static SupplementalDataInfo sdi = testInfo.getSupplementalDataInfo();

    public static void main(String[] args) {
        new TestCLDRFile().run(args);
    }

    public void testFallbackNames() {
        String[][] tests = {
            {"zh-Hanb", "Chinese (Han with Bopomofo)"},
            {"aaa", "Ghotuo"},
            {"zh-RR", "Chinese (RR)"},
            {"new_Newa_NP", "Newari (Newa, Nepal)"},
        };
        CLDRFile english = testInfo.getEnglish();
        for (String[] test : tests) {
            assertEquals("", test[1], english.getName(test[0]));
        }
    }

    // verify for all paths, if there is a count="other", then there is a
    // count="x", for all x in keywords
    public void testPlurals() {
        for (String locale : new String[] {"fr", "en", "root", "ar", "ja"}) {
            checkPlurals(locale);
        }
    }

    static final Pattern COUNT_MATCHER = Pattern.compile("\\[@count=\"([^\"]+)\"]");

    static final UnicodeSet DIGITS = new UnicodeSet('0', '9').freeze();

    private void checkPlurals(String locale) {
        CLDRFile cldrFile = testInfo.getCLDRFile(locale, true);
        Matcher m = COUNT_MATCHER.matcher("");
        Relation<String, String> skeletonToKeywords =
                Relation.of(
                        new TreeMap<String, Set<String>>(cldrFile.getComparator()), TreeSet.class);
        PluralInfo plurals = sdi.getPlurals(PluralType.cardinal, locale);
        for (String path : cldrFile.fullIterable()) {
            if (!path.contains("@count")) {
                continue;
            }
            if (!m.reset(path).find()) {
                throw new IllegalArgumentException();
            }
            if (DIGITS.containsAll(m.group(1))) {
                continue;
            }
            String skeleton = path.substring(0, m.start(1)) + ".*" + path.substring(m.end(1));
            skeletonToKeywords.put(skeleton, m.group(1));
        }
        Set<String> normalKeywords = plurals.getAdjustedCountStrings();

        for (Entry<String, Set<String>> entry : skeletonToKeywords.keyValuesSet()) {
            final String abbreviatedPath = entry.getKey();
            Set<String> expected = normalKeywords;
            if (abbreviatedPath.startsWith("//ldml/numbers/minimalPairs/pluralMinimalPairs")) {
                expected = plurals.getCanonicalKeywords();
            }
            assertEquals(
                    "Incorrect keywords: " + locale + ", " + abbreviatedPath,
                    expected,
                    entry.getValue());
        }
    }

    static Factory cldrFactory = testInfo.getCldrFactory();

    static class LocaleInfo {
        final String locale;
        final CLDRFile cldrFile;
        final Set<String> paths = new HashSet<>();

        LocaleInfo(String locale) {
            this.locale = locale;
            cldrFile = testInfo.getCLDRFile(locale, true);
            for (String path : cldrFile.fullIterable()) {
                Level level = sdi.getCoverageLevel(path, locale);
                if (level.compareTo(Level.COMPREHENSIVE) > 0) {
                    continue;
                }
                if (path.contains("[@count=") && !path.contains("[@count=\"other\"]")) {
                    continue;
                }
                paths.add(path);
            }
        }
    }

    static final boolean DEBUG = false;
    static final boolean DEBUG_testExtraPaths = true;

    public void testExtraPaths() {
        // for debugging
        final CLDRFile german = CLDRConfig.getInstance().getCldrFactory().make("de", true);
        getLogger().fine("");
        Set<String> sorted = new TreeSet<>(german.getExtraPaths());
        PathHeader.Factory phf = PathHeader.getFactory();
        PatternPlaceholders pph = PatternPlaceholders.getInstance();

        for (String path : sorted) {
            if (DEBUG_testExtraPaths
                    && path.equals(
                            "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"acceleration-g-force\"]/unitPattern")) {
                List<String> failures = new ArrayList<>();
                phf.fromPath(path, failures);
            }
            PathHeader ph = phf.fromPath(path);
            if (ph.getPageId() != PageId.Deprecated) {
                assertNotEquals(
                        "bad placeholder: " + path + " ; " + ph,
                        SectionId.Special,
                        ph.getSectionId());
            }
            PlaceholderStatus phStatus = pph.getStatus(path);

            PlaceholderStatus expected =
                    path.contains("/metazone")
                                    || path.contains("/timeZoneNames")
                                    || path.contains("/gender")
                                    || path.startsWith("//ldml/numbers/currencies/currency")
                                    || path.startsWith("//ldml/personNames/sampleName")
                            ? PlaceholderStatus.DISALLOWED
                            : path.contains("/compoundUnitPattern1")
                                    ? PlaceholderStatus.REQUIRED
                                    : PlaceholderStatus.LOCALE_DEPENDENT;
            if (!assertEquals(path, expected, phStatus)) {
                int debug = 0;
            }

            if (DEBUG) {
                if (GrammaticalFeature.pathHasFeature(path) != null || path.endsWith("/gender")) {
                    System.out.println(path + "\t" + german.getStringValue(path));
                    String newPath = path.replace("[@case=\"accusative\"]", "");
                    if (!newPath.contentEquals(path) && !sorted.contains(newPath)) {
                        System.out.println(newPath + "\t" + german.getStringValue(newPath));
                    }
                }
            }
        }

        Set<String> badCoverage = new TreeSet<>();
        Counter<String> extraPaths = new Counter<>();
        final Factory fullCldrFactory = CLDRConfig.getInstance().getFullCldrFactory();

        for (String locale : sdi.hasGrammarInfo()) {
            if (sdi.getGrammarInfo(locale).hasInfo(GrammaticalTarget.nominal)) {
                if (!fullCldrFactory.getAvailable().contains(locale)) {
                    if (CLDRConfig.SKIP_SEED) {
                        continue; // don't check if skipping seed, for production
                    }
                }
                final CLDRFile cldrFile = fullCldrFactory.make(locale, true);
                Set<String> sorted2 = new TreeSet<>(cldrFile.getExtraPaths());
                for (String path : sorted2) {
                    if (path.contains("/gender")
                            || path.contains("@gender")
                            || path.contains("@case")) {
                        Level level = sdi.getCoverageLevel(path, locale);
                        if (level.compareTo(Level.MODERN) > 0) {
                            badCoverage.add(path);
                        }
                        extraPaths.add(locale, 1);
                    }
                }
            }
        }
        getLogger().fine("Units with grammar info: " + GrammarInfo.getUnitsToAddGrammar().size());
        getLogger().fine("Inflection Paths");
        for (R2<Long, String> locale : extraPaths.getEntrySetSortedByCount(false, null)) {
            getLogger().fine(locale.get0() + "\t" + locale.get1());
        }
        if (!badCoverage.isEmpty()) {
            errln("Paths not at modern: " + Joiner.on("\n\t").join(badCoverage));
        }

        //        Set<String> validUnits =
        // Validity.getInstance().getStatusToCodes(LstrType.unit).get(Validity.Status.regular);
        //        validUnits.forEach(System.out::println);
        //
        //        grammarInfo = testInfo.getSupplementalDataInfo().getGrammarInfo();
        //        for (Entry<String, GrammarInfo> entry : grammarInfo.entrySet()) {
        //            System.out.println(entry);
        //        }

        //        CLDRFile toCheck = testInfo.getCldrFactory().make("de", true); //
        // testInfo.getFullCldrFactory().make("en", false);
        //        Set<String> sorted = new TreeSet<>(CLDRFile.getComparator(DtdType.ldml));
        //        sorted.addAll(toCheck.getExtraPaths());
        //        for (String path : sorted) {
        //            XPathParts parts = XPathParts.getFrozenInstance(path);
        //            assertEquals("parts: ", parts.toString(), path);
        //            System.out.println(path);
        //        }
        //        int debug = 0;

        Map<String, LocaleInfo> localeInfos = new LinkedHashMap<>();
        Relation<String, String> missingPathsToLocales =
                Relation.of(
                        new TreeMap<String, Set<String>>(CLDRFile.getComparator(DtdType.ldml)),
                        TreeSet.class);
        Relation<String, String> extraPathsToLocales =
                Relation.of(
                        new TreeMap<String, Set<String>>(CLDRFile.getComparator(DtdType.ldml)),
                        TreeSet.class);

        for (String locale : new String[] {"en", "root", "fr", "ar", "ja"}) {
            localeInfos.put(locale, new LocaleInfo(locale));
        }
        LocaleInfo englishInfo = localeInfos.get("en");
        for (String path : englishInfo.paths) {
            if (path.startsWith("//ldml/identity/")
                    || path.startsWith("//ldml/numbers/currencies/currency[@type=")
                    // || path.startsWith("//ldml/dates/calendars/calendar") &&
                    // !path.startsWith("//ldml/dates/calendars/calendar[@type=\"gregorian\"]")
                    // ||
                    // path.startsWith("//ldml/numbers/currencyFormats[@numberSystem=")
                    // &&
                    // !path.startsWith("//ldml/numbers/currencyFormats[@numberSystem=\"latn\"]")
                    || (path.contains("[@count=") && !path.contains("[@count=\"other\"]"))
                    || (path.contains("[@ordinal=") && !path.contains("[@ordinal=\"other\"]"))
                    || path.contains("dayPeriod[@type=\"noon\"]")) {
                continue;
            }
            for (LocaleInfo localeInfo : localeInfos.values()) {
                if (localeInfo == englishInfo) {
                    continue;
                }
                if (!localeInfo.paths.contains(path)) {
                    if (path.startsWith("//ldml/dates/calendars/calendar")
                                    && !(path.contains("[@type=\"generic\"]")
                                            || path.contains("[@type=\"gregorian\"]"))
                            || (path.contains("/eras/")
                                    && path.contains("[@alt=\"variant\"]")) // it is OK
                            // for
                            // just
                            // "en"
                            // to
                            // have
                            // /eras/.../era[@type=...][@alt="variant"]
                            || path.contains("[@type=\"japanese\"]")
                            || path.contains("[@type=\"coptic\"]")
                            || path.contains("[@type=\"hebrew\"]")
                            || path.contains("[@type=\"islamic-rgsa\"]")
                            || path.contains("[@type=\"islamic-umalqura\"]")
                            || path.contains("/relative[@type=\"-2\"]")
                            || path.contains("/relative[@type=\"2\"]")
                            || path.startsWith("//ldml/contextTransforms/contextTransformUsage")
                            || path.contains("[@alt=\"variant\"]")
                            || path.contains("[@alt=\"formal\"]")
                            || (path.contains("dayPeriod[@type=")
                                    && (path.endsWith("1\"]")
                                            || path.endsWith("\"am\"]")
                                            || path.endsWith("\"pm\"]")
                                            || path.endsWith(
                                                    "\"midnight\"]"))) // morning1, afternoon1, ...
                            || (localeInfo.locale.equals("root")
                                    && (path.startsWith(
                                                    "//ldml/characters/exemplarCharacters[@type=\"index\"]")
                                            || (path.startsWith("//ldml/units/unitLength") // two
                                                    // aliased
                                                    // paths in
                                                    // root
                                                    && (path.contains("[@type=\"energy-foodcalorie")
                                                            || path.contains(
                                                                    "[@type=\"graphics-dot")))))
                    // //ldml/characters/exemplarCharacters[@type="index"][root]
                    ) {
                        continue;
                    }
                    String localeAndStatus =
                            localeInfo.locale + (englishInfo.cldrFile.isHere(path) ? "" : "*");
                    missingPathsToLocales.put(path, localeAndStatus);
                    // English contains the path, and the target locale doesn't.
                    // The * means that the value is inherited (eg from root).
                }
            }
        }

        for (LocaleInfo localeInfo : localeInfos.values()) {
            if (localeInfo == englishInfo) {
                continue;
            }
            for (String path : localeInfo.paths) {
                if (path.contains("[@numberSystem=\"arab\"]")
                        || path.contains("[@type=\"japanese\"]")
                        || path.contains("[@type=\"coptic\"]")
                        || path.contains("[@type=\"hebrew\"]")
                        || path.contains("[@type=\"islamic-rgsa\"]")
                        || path.contains("[@type=\"islamic-umalqura\"]")
                        || path.contains("/relative[@type=\"-2\"]")
                        || path.contains("/relative[@type=\"2\"]")) {
                    continue;
                }
                if (!englishInfo.paths.contains(path)) {
                    String localeAndStatus =
                            localeInfo.locale + (localeInfo.cldrFile.isHere(path) ? "" : "*");
                    extraPathsToLocales.put(path, localeAndStatus);
                    // English doesn't contains the path, and the target locale does.
                    // The * means that the value is inherited (eg from root).
                }
            }
        }

        for (Entry<String, Set<String>> entry : missingPathsToLocales.keyValuesSet()) {
            String path = entry.getKey();
            Set<String> locales = entry.getValue();
            Status status = new Status();
            String originalLocale = englishInfo.cldrFile.getSourceLocaleID(path, status);
            String engName =
                    "en"
                            + (englishInfo.cldrFile.isHere(path)
                                    ? ""
                                    : " (source_locale:"
                                            + originalLocale
                                            + (path.equals(status.pathWhereFound)
                                                    ? ""
                                                    : ", source_path: " + status)
                                            + ")");
            if (path.startsWith("//ldml/localeDisplayNames/")
                    || path.contains("[@alt=\"accounting\"]")
                    || path.contains("[@alt=\"alphaNextToNumber\"]") // CLDR-14336
                    || path.contains("[@alt=\"ascii\"]") // CLDR-16606
                    || path.contains("[@alt=\"noCurrency\"]") // CLDR-14336
                    || path.startsWith("//ldml/personNames/") // CLDR-15384
            ) {
                logln("+" + engName + ", -" + locales + "\t" + path);
            } else {
                errln("+" + engName + ", -" + locales + "\t" + path);
            }
        }
        for (Entry<String, Set<String>> entry : extraPathsToLocales.keyValuesSet()) {
            String path = entry.getKey();
            Set<String> locales = entry.getValue();
            if (path.startsWith("//ldml/localeDisplayNames/")
                    || path.startsWith("//ldml/numbers/otherNumberingSystems/")
            // || path.contains("[@alt=\"accounting\"]")
            ) {
                logln("-en, +" + locales + "\t" + path);
            } else {
                logln("-en, +" + locales + "\t" + path);
            }
        }

        // for (String locale : new String[] { "fr", "ar", "ja" }) {
        // CLDRFile cldrFile = cldrFactory.make(locale, true);
        // Set<String> s = (Set<String>) cldrFile.getExtraPaths(new
        // TreeSet<String>());
        // System.out.println("Extras for " + locale);
        // for (String path : s) {
        // System.out.println(path + " => " + cldrFile.getStringValue(path));
        // }
        // System.out.println("Already in " + locale);
        // for (Iterator<String> it =
        // cldrFile.iterator(PatternCache.get(".*\\[@count=.*").matcher(""));
        // it.hasNext();) {
        // String path = it.next();
        // System.out.println(path + " => " + cldrFile.getStringValue(path));
        // }
        // }
    }

    // public void testDraftFilter() {
    // Factory cldrFactory = Factory.make(CldrUtility.MAIN_DIRECTORY, ".*",
    // DraftStatus.approved);
    // checkLocale(cldrFactory.make("root", true));
    // checkLocale(cldrFactory.make("ee", true));
    // }

    public void checkLocale(CLDRFile cldr) {
        Matcher m = PatternCache.get("gregorian.*eras").matcher("");
        for (Iterator<String> it = cldr.iterator("", new UTF16.StringComparator());
                it.hasNext(); ) {
            String path = it.next();
            if (m.reset(path).find() && !path.contains("alias")) {
                errln(
                        cldr.getLocaleID()
                                + "\t"
                                + cldr.getStringValue(path)
                                + "\t"
                                + cldr.getFullXPath(path));
            }
            if (path == null) {
                errln("Null path");
            }
            String fullPath = cldr.getFullXPath(path);
            if (fullPath.contains("@draft")) {
                errln("File can't contain draft elements");
            }
        }
    }

    // public void testTimeZonePath() {
    // Factory cldrFactory = Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
    // String tz = "Pacific/Midway";
    // CLDRFile cldrFile = cldrFactory.make("lv", true);
    // String retVal = cldrFile.getStringValue(
    // "//ldml/dates/timeZoneNames/zone[@type=\"" + tz + "\"]/exemplarCity"
    // , true).trim();
    // errln(retVal);
    // }

    public void testSimple() {
        double deltaTime = System.currentTimeMillis();
        CLDRFile english = testInfo.getEnglish();
        deltaTime = System.currentTimeMillis() - deltaTime;
        logln("Creation: Elapsed: " + deltaTime / 1000.0 + " seconds");

        deltaTime = System.currentTimeMillis();
        english.getStringValue("//ldml");
        deltaTime = System.currentTimeMillis() - deltaTime;
        logln("Creation: Elapsed: " + deltaTime / 1000.0 + " seconds");

        deltaTime = System.currentTimeMillis();
        english.getStringValue("//ldml");
        deltaTime = System.currentTimeMillis() - deltaTime;
        logln("Caching: Elapsed: " + deltaTime / 1000.0 + " seconds");

        deltaTime = System.currentTimeMillis();
        for (int j = 0; j < 2; ++j) {
            for (Iterator<String> it = english.iterator(); it.hasNext(); ) {
                String dpath = it.next();
                String value = english.getStringValue(dpath);
                Set<String> paths = english.getPathsWithValue(value, "", null, null);
                if (paths.size() == 0) {
                    continue;
                }
                if (!paths.contains(dpath)) {
                    if (DISABLE_TIL_WORKS) {
                        errln("Missing " + dpath + " in " + pathsWithValues(value, paths));
                    }
                }
                if (paths.size() > 1) {
                    Set<String> nonAliased = getNonAliased(paths, english);
                    if (nonAliased.size() > 1) {
                        logln(pathsWithValues(value, nonAliased));
                    }
                }
            }
        }
        deltaTime = System.currentTimeMillis() - deltaTime;
        logln("Elapsed: " + deltaTime / 1000.0 + " seconds");
    }

    private String pathsWithValues(String value, Set<String> paths) {
        return paths.size()
                + " paths with: <"
                + value
                + ">\t\tPaths: "
                + paths.iterator().next()
                + ",...";
    }

    private Set<String> getNonAliased(Set<String> paths, CLDRFile file) {
        Set<String> result = new LinkedHashSet<>();
        for (String path : paths) {
            if (file.isHere(path)) {
                result.add(path);
            }
        }
        return result;
    }

    public void testResolution() {
        CLDRFile german = testInfo.getCLDRFile("de", true);
        // Test direct lookup.
        String xpath = "//ldml/localeDisplayNames/localeDisplayPattern/localeSeparator";
        String id = german.getSourceLocaleID(xpath, null);
        if (!id.equals("de")) {
            errln("Expected de but was " + id + " for " + xpath);
        }

        // Test aliasing.
        xpath =
                "//ldml/dates/calendars/calendar[@type=\"islamic-civil\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\"yyyyMEd\"]";
        id = german.getSourceLocaleID(xpath, null);
        if (!id.equals("de")) {
            errln("Expected de but was " + id + " for " + xpath);
        }

        // Test lookup that falls to root.
        xpath =
                "//ldml/dates/calendars/calendar[@type=\"coptic\"]/months/monthContext[@type=\"stand-alone\"]/monthWidth[@type=\"narrow\"]/month[@type=\"5\"]";
        id = german.getSourceLocaleID(xpath, null);
        if (!id.equals("root")) {
            errln("Expected root but was " + id + " for " + xpath);
        }
    }

    static final NumberFormat percent = NumberFormat.getPercentInstance();

    static final class Size {
        int items;
        int chars;

        public void add(String topValue) {
            items++;
            chars += topValue.length();
        }

        public String over(Size base) {
            return "items: "
                    + items
                    + "("
                    + percent.format(items / (0.0 + base.items))
                    + "); "
                    + "chars: "
                    + chars
                    + "("
                    + percent.format(chars / (0.0 + base.chars))
                    + ")";
        }
    }

    public void testGeorgeBailey() {
        PathHeader.Factory phf = PathHeader.getFactory(testInfo.getEnglish());
        for (String locale : Arrays.asList("de", "de_AT", "en", "nl")) {
            CLDRFile cldrFile = testInfo.getCLDRFile(locale, true);

            CLDRFile cldrFileUnresolved = testInfo.getCLDRFile(locale, false);
            Status status = new Status();
            Output<String> localeWhereFound = new Output<>();
            Output<String> pathWhereFound = new Output<>();

            Map<String, String> diff = new TreeMap<>(CLDRFile.getComparator(DtdType.ldml));

            Size countSuperfluous = new Size();
            Size countExtraLevel = new Size();
            Size countOrdinary = new Size();

            for (String path : cldrFile.fullIterable()) {
                String baileyValue =
                        cldrFile.getBaileyValue(path, pathWhereFound, localeWhereFound);
                String topValue = cldrFileUnresolved.getStringValue(path);
                String resolvedValue = cldrFile.getStringValue(path);
                String unresolvedConstructedValue = cldrFileUnresolved.getConstructedValue(path);
                String resolvedConstructedValue = cldrFile.getConstructedValue(path);

                // assertEquals("x≠y", "x", "y"); // expected x, got y
                if (unresolvedConstructedValue != null) {
                    assertEquals(
                            "uc≠rc\t" + locale + "\t" + phf.fromPath(path),
                            unresolvedConstructedValue,
                            resolvedConstructedValue);
                }

                // if there is a value, then either it is at the top level or it
                // is the bailey value.
                // OR it is INHERITANCE_MARKER

                if (resolvedValue != null) {
                    if (topValue != null && !CldrUtility.INHERITANCE_MARKER.equals(topValue)) {
                        if (!topValue.equals(cldrFileUnresolved.getConstructedValue(path))) {
                            assertEquals(
                                    "top≠resolved\t" + locale + "\t" + phf.fromPath(path),
                                    topValue,
                                    resolvedValue);
                        }
                    } else {
                        String locale2 = cldrFile.getSourceLocaleID(path, status);
                        if (!assertEquals(
                                "bailey value≠\t" + locale + "\t" + phf.fromPath(path),
                                resolvedValue,
                                baileyValue)) {
                            baileyValue =
                                    cldrFile.getBaileyValue(path, pathWhereFound, localeWhereFound);
                            topValue = cldrFileUnresolved.getStringValue(path);
                        }
                        if (!assertEquals(
                                "bailey locale≠\t" + locale + "\t" + phf.fromPath(path),
                                locale2,
                                localeWhereFound.value)) {
                            baileyValue =
                                    cldrFile.getBaileyValue(path, pathWhereFound, localeWhereFound);
                            topValue = cldrFileUnresolved.getStringValue(path);
                        }
                        if (!assertEquals(
                                "bailey path≠\t" + locale + "\t" + phf.fromPath(path),
                                status.pathWhereFound,
                                pathWhereFound.value)) {
                            baileyValue =
                                    cldrFile.getBaileyValue(path, pathWhereFound, localeWhereFound);
                            topValue = cldrFileUnresolved.getStringValue(path);
                        }
                    }
                }

                if (topValue != null) {
                    if (CldrUtility.equals(topValue, baileyValue)) {
                        countSuperfluous.add(topValue);
                    } else if (sdi.getCoverageLevel(path, locale).compareTo(Level.MODERN) > 0) {
                        countExtraLevel.add(topValue);
                    }
                    countOrdinary.add(topValue);
                }
            }
            logln("Superfluous (" + locale + "):\t" + countSuperfluous.over(countOrdinary));
            logln(">Modern (" + locale + "):\t" + countExtraLevel.over(countOrdinary));
            for (Entry<String, String> entry : diff.entrySet()) {
                logln(locale + "\t" + phf.fromPath(entry.getKey()) + ";\t" + entry.getValue());
            }
        }
    }

    public void TestConstructedValue() {
        CLDRFile eng = CLDRConfig.getInstance().getEnglish();

        String prefix = GlossonymConstructor.PATH_PREFIX;
        String display = eng.getConstructedValue(prefix + "zh_Hans" + "\"]");
        assertEquals("contructed value", "Chinese (Simplified)", display);
        display = eng.getConstructedValue(prefix + "es_US" + "\"]");
        assertEquals("contructed value", "Spanish (United States)", display);
        display = eng.getConstructedValue(prefix + "es_US" + "\"][@alt=\"short\"]");
        assertEquals("contructed value", "Spanish (US)", display);
        display = eng.getConstructedValue(prefix + "es" + "\"]");
        assertEquals("contructed value", null, display);
        display = eng.getConstructedValue(prefix + "missing" + "\"]");
        assertEquals("contructed value", null, display);
    }

    public void TestFileLocations() {
        File mainDir = new File(CLDRPaths.MAIN_DIRECTORY);
        if (!mainDir.isDirectory()) {
            throw new IllegalArgumentException(
                    "MAIN_DIRECTORY is not a directory: " + CLDRPaths.MAIN_DIRECTORY);
        }
        File mainCollationDir = new File(CLDRPaths.COLLATION_DIRECTORY);
        if (!mainCollationDir.isDirectory()) {
            throw new IllegalArgumentException(
                    "COLLATION_DIRECTORY is not a directory: " + CLDRPaths.COLLATION_DIRECTORY);
        }
        if (CLDRConfig.SKIP_SEED) {
            return;
        }
        File seedDir = new File(CLDRPaths.SEED_DIRECTORY);
        if (!seedDir.isDirectory()) {
            throw new IllegalArgumentException(
                    "SEED_DIRECTORY is not a directory: " + CLDRPaths.SEED_DIRECTORY);
        }
        File seedCollationDir = new File(CLDRPaths.SEED_COLLATION_DIRECTORY);
        if (!seedCollationDir.isDirectory()) {
            throw new IllegalArgumentException(
                    "SEED_COLLATION_DIRECTORY is not a directory: "
                            + CLDRPaths.SEED_COLLATION_DIRECTORY);
        }

        File[] md = {mainDir, mainCollationDir};
        File[] sd = {seedDir, seedCollationDir};
        Factory mf = SimpleFactory.make(md, ".*", DraftStatus.unconfirmed);
        Factory sf = SimpleFactory.make(sd, ".*", DraftStatus.unconfirmed);
        Set<CLDRLocale> mainLocales = mf.getAvailableCLDRLocales();
        Set<CLDRLocale> seedLocales = sf.getAvailableCLDRLocales();
        mainLocales.retainAll(seedLocales);
        mainLocales.remove(CLDRLocale.getInstance("root")); // allow multiple roots
        if (!mainLocales.isEmpty()) {
            errln(
                    "CLDR locale files located in both common and seed ==> "
                            + mainLocales.toString());
        }
    }

    public void TestForStrayFiles() {
        TreeSet<String> mainList =
                new TreeSet<>(Arrays.asList(new File(CLDRPaths.MAIN_DIRECTORY).list()));

        for (String dir : DtdType.ldml.directories) {
            Set<String> dirFiles =
                    new TreeSet<>(
                            Arrays.asList(
                                    new File(CLDRPaths.BASE_DIRECTORY + "common/" + dir).list()));
            if (!mainList.containsAll(dirFiles)) {
                dirFiles.removeAll(mainList);
                errln(dir + "/ has extra files not in main/: " + dirFiles);
            }
        }
    }

    public void TestFileIds() {
        Output<Map<String, Multimap<LdmlDir, Source>>> localeToDirToSource = new Output<>();
        Map<LdmlDir, Multimap<String, Source>> dirToLocaleToSource = getFiles(localeToDirToSource);

        for (Entry<String, Multimap<LdmlDir, Source>> e : localeToDirToSource.value.entrySet()) {
            String locale = e.getKey();
            if (locale.equals("root")) {
                continue; // allow multiple root locales
            }
            Map<LdmlDir, Collection<Source>> value = e.getValue().asMap();
            for (Entry<LdmlDir, Collection<Source>> e2 : value.entrySet()) {
                LdmlDir dir = e2.getKey();
                Collection<Source> sources = e2.getValue();
                if (sources.size() != 1) {
                    errln(
                            "Can only one have 1 instance of "
                                    + locale
                                    + " in "
                                    + dir
                                    + ", but have in "
                                    + sources);
                }
            }
        }

        LikelySubtags likelySubtags = new LikelySubtags();

        for (Entry<LdmlDir, Multimap<String, Source>> dirAndLocaleToSource :
                dirToLocaleToSource.entrySet()) {
            LdmlDir ldmlDir = dirAndLocaleToSource.getKey();
            Multimap<String, Source> localesToDirs = dirAndLocaleToSource.getValue();
            for (Entry<String, Source> localeAndDir : localesToDirs.entries()) {
                String loc = localeAndDir.getKey();
                if (loc.equals("root")) {
                    continue;
                }
                Source source = localeAndDir.getValue();
                String parent = LocaleIDParser.getParent(loc);
                String parent2 = LanguageTagParser.getSimpleParent(loc);
                if (parent2.isEmpty()) {
                    parent2 = "root";
                }
                String likely = likelySubtags.minimize(loc);
                if (!localesToDirs.containsKey(parent)) {
                    //                    if (ldmlDir == LdmlDir.rbnf && source == Source.common &&
                    //                        parent.equals("en_001") && loc.equals("en_IN") &&
                    //                        logKnownIssue("cldrbug:10456", "Missing parent
                    // (en_001) for en_IN in common/rbnf")) {
                    //                            continue;
                    //                    }
                    errln(
                            "Missing parent ("
                                    + parent
                                    + ") for "
                                    + loc
                                    + "  in "
                                    + source
                                    + "/"
                                    + ldmlDir
                                    + "; likely="
                                    + likely);
                }
                if (!Objects.equals(parent, parent2) && !localesToDirs.containsKey(parent2)) {
                    errln(
                            "Missing simple parent ("
                                    + parent2
                                    + ") for "
                                    + loc
                                    + "  in "
                                    + source
                                    + "/"
                                    + ldmlDir
                                    + "; likely="
                                    + likely);
                }
            }

            // establish that the parent of locale is somewhere in the same
            //                assertEquals(dir + " locale file has minimal id: ", min, loc);
            //            if (!dir.endsWith("exemplars")) {
            //                continue;
            //            }
            //            String trans = ltc.transform(loc);
            //            System.out.println("\t" + min + "\t" + loc + "\t" + trans);
        }
    }

    enum Source {
        common,
        seed,
        exemplars
    }

    enum LdmlDir {
        main,
        annotations,
        annotationsDerived,
        casing,
        collation,
        rbnf,
        segments,
        subdivisions
    }

    /**
     * Returns a map from directory (eg main) to its parent (eg seed) and to their children (locales
     * in seed/main)
     */
    private Map<LdmlDir, Multimap<String, Source>> getFiles(
            Output<Map<String, Multimap<LdmlDir, Source>>> localeToDirToSource) {

        Map<LdmlDir, Multimap<String, Source>> _dirToLocaleToSource = new TreeMap<>();
        Map<String, Multimap<LdmlDir, Source>> _localeToDirToSource = new TreeMap<>();

        for (String base : new File(CLDRPaths.BASE_DIRECTORY).list()) {
            Source source;
            try {
                source = Source.valueOf(base);
            } catch (Exception e) {
                continue;
            }
            String fullBase = CLDRPaths.BASE_DIRECTORY + base;
            File fullBaseFile = new File(fullBase);
            if (!fullBaseFile.isDirectory()) {
                continue;
            }

            for (String sub1 : fullBaseFile.list()) {
                if (!DtdType.ldml.directories.contains(sub1)) {
                    continue;
                }
                LdmlDir ldmlDir = LdmlDir.valueOf(sub1);
                String dir = fullBase + "/" + ldmlDir;
                for (String loc : new File(dir).list()) {
                    if (!loc.endsWith(".xml")) {
                        continue;
                    }
                    loc = loc.substring(0, loc.length() - 4);

                    put(_localeToDirToSource, loc, ldmlDir, source);
                    put(_dirToLocaleToSource, ldmlDir, loc, source);
                }
            }
        }
        localeToDirToSource.value =
                ImmutableMap.copyOf(_localeToDirToSource); // TODO protect subtrees
        return ImmutableMap.copyOf(_dirToLocaleToSource);
    }

    private <A, B, C> void put(Map<A, Multimap<B, C>> aToBToC, A a, B b, C c) {
        Multimap<B, C> dirToSource = aToBToC.get(a);
        if (dirToSource == null) {
            aToBToC.put(a, dirToSource = (Multimap<B, C>) TreeMultimap.create());
        }
        dirToSource.put(b, c);
    }

    public void TestSwissHighGerman() {
        CLDRFile swissHighGerman = testInfo.getCommonSeedExemplarsFactory().make("de_CH", true);
        for (String xpath : swissHighGerman) {
            if (xpath.equals("//ldml/characters/exemplarCharacters[@type=\"auxiliary\"]")) {
                continue;
            }
            String value = swissHighGerman.getStringValue(xpath);
            if (value.indexOf('ß') >= 0) {
                warnln("«" + value + "» contains ß at " + xpath);
            }
        }
    }

    public void TestExtraPaths() {
        List<String> testCases =
                Arrays.asList(
                        "//ldml/localeDisplayNames/languages/language[@type=\"ccp\"]",
                        "//ldml/dates/calendars/calendar[@type=\"generic\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"Gy\"]/greatestDifference[@id=\"G\"]");
        CLDRFile af = testInfo.getCldrFactory().make("af", true);
        Set<String> missing = new HashSet<>(testCases);
        CoverageLevel2 coverageLevel2 = CoverageLevel2.getInstance("af");
        PathHeader.Factory pathHeaderFactory = PathHeader.getFactory(testInfo.getEnglish());
        Status status = new Status();

        for (String xpath : af) {
            if (missing.contains(xpath)) {
                String value = af.getStringValue(xpath);
                String source = af.getSourceLocaleID(xpath, status);
                Level level = coverageLevel2.getLevel(xpath);
                PathHeader ph = pathHeaderFactory.fromPath(xpath);
                getLogger()
                        .fine(
                                ""
                                        + "\nPathHeader:\t"
                                        + ph
                                        + "\nValue:\t"
                                        + value
                                        + "\nLevel:\t"
                                        + level
                                        + "\nReq. Locale:\t"
                                        + "af"
                                        + "\nSource Locale:\t"
                                        + source
                                        + "\nReq. XPath:\t"
                                        + xpath
                                        + "\nSource Path:\t"
                                        + status);
                missing.remove(xpath);
            }
        }
        assertTrue("Should be empty", missing.isEmpty());
    }

    public void TestExtraPaths13954() {
        CLDRFile es = cldrFactory.make("es", true);
        CLDRFile es_US = cldrFactory.make("es_US", true);
        if (!es_US.getRawExtraPaths().containsAll(es.getRawExtraPaths())) {
            errln(
                    "Failure: "
                            + Joiner.on('\n')
                                    .join(
                                            Sets.difference(
                                                    es.getRawExtraPaths(),
                                                    es_US.getRawExtraPaths())));
        }
    }
}

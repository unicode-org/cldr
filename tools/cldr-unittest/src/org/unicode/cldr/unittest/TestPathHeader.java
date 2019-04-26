package org.unicode.cldr.unittest;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;

import org.unicode.cldr.test.CoverageLevel2;
import org.unicode.cldr.test.ExampleGenerator;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Containment;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.DtdData;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LanguageTagParser;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.Pair;
import org.unicode.cldr.util.PathDescription;
import org.unicode.cldr.util.PathHeader;
import org.unicode.cldr.util.PathHeader.PageId;
import org.unicode.cldr.util.PathHeader.SectionId;
import org.unicode.cldr.util.PathHeader.SurveyToolStatus;
import org.unicode.cldr.util.PathStarrer;
import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.PatternPlaceholders;
import org.unicode.cldr.util.PatternPlaceholders.PlaceholderInfo;
import org.unicode.cldr.util.PatternPlaceholders.PlaceholderStatus;
import org.unicode.cldr.util.PrettyPath;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo;
import org.unicode.cldr.util.SupplementalDataInfo.PluralType;
import org.unicode.cldr.util.With;
import org.unicode.cldr.util.XMLFileReader;
import org.unicode.cldr.util.XPathParts;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R2;

public class TestPathHeader extends TestFmwkPlus {
    private static final DtdType DEBUG_DTD_TYPE = null; // DtdType.supplementalData;
    private static final String COMMON_DIR = CLDRPaths.BASE_DIRECTORY + "common/";
    private static final boolean DEBUG = false;

    public static void main(String[] args) {
        new TestPathHeader().run(args);
    }

    static final CLDRConfig info = CLDRConfig.getInstance();
    static final Factory factory = info.getCommonAndSeedAndMainAndAnnotationsFactory();
    static final CLDRFile english = factory.make("en", true);
    static final SupplementalDataInfo supplemental = info
        .getSupplementalDataInfo();
    static PathHeader.Factory pathHeaderFactory = PathHeader
        .getFactory(english);
    private EnumSet<PageId> badZonePages = EnumSet.of(PageId.UnknownT);

    public void tempTestAnnotation() {
        // NEW:     <annotation cp="😀">face | grin</annotation>
        //          <annotation cp="😀" type="tts">grinning face</annotation>

        final String path1 = "//ldml/annotations/annotation[@cp=\"🚻\"]";
        PathHeader ph1 = pathHeaderFactory.fromPath(path1);
        logln(ph1.toString() + "\t" + path1);
        final String path2 = "//ldml/annotations/annotation[@cp=\"🚻\"][@type=\"tts\"]";
        PathHeader ph2 = pathHeaderFactory.fromPath(path2);
        logln(ph2.toString() + "\t" + path2);
        final String path3 = "//ldml/annotations/annotation[@cp=\"🚱\"]";
        PathHeader ph3 = pathHeaderFactory.fromPath(path2);
        logln(ph3.toString() + "\t" + path3);

        assertNotEquals("pathheader", ph1, ph2);
        assertNotEquals("pathheader", ph1.toString(), ph2.toString());
        assertRelation("pathheader", true, ph1, TestFmwkPlus.LEQ, ph3);
        assertRelation("pathheader", true, ph3, TestFmwkPlus.LEQ, ph2);
    }

    public void tempTestCompletenessLdmlDtd() {
        // List<String> failures = null;
        pathHeaderFactory.clearCache();
        PathChecker pathChecker = new PathChecker();
        for (String directory : DtdType.ldml.directories) {
            Factory factory2 = CLDRConfig.getInstance().getMainAndAnnotationsFactory();
            Set<String> source = factory2.getAvailable();
            for (String file : getFilesToTest(source, "root", "en", "da")) {
                if (DEBUG) warnln(" TestCompletenessLdmlDtd: " + directory + ", " + file);
                DtdData dtdData = null;
                CLDRFile cldrFile = factory2.make(file, true);
                for (String path : cldrFile.fullIterable()) {
                    pathChecker.checkPathHeader(cldrFile.getDtdData(), path);
                }
            }
        }
        Set<String> missing = pathHeaderFactory.getUnmatchedRegexes();
        if (missing.size() != 0) {
            for (String e : missing) {
                errln("Path Regex never matched:\t" + e);
            }
        }
    }

    private Collection<String> getFilesToTest(Collection<String> source, String... doFirst) {
        LinkedHashSet<String> files = new LinkedHashSet<>(Arrays.asList(doFirst));
        files.retainAll(source); // put first
        files.addAll(new HashSet<>(source)); // now add others semi-randomly
        int max = Math.min(30, files.size());
        if (getInclusion() == 10 || files.size() <= max) {
            return files;
        }
        ArrayList<String> shortFiles = new ArrayList<>(files);
        if (getInclusion() > 5) {
            max += (files.size() - 30) * (getInclusion() - 5) / 10; // use proportional amount
        }
        return shortFiles.subList(0, max);
    }

    public void TestCompleteness() {
        PathHeader.Factory pathHeaderFactory2 = PathHeader.getFactory(english);
        // List<String> failures = null;
        pathHeaderFactory2.clearCache();
        Multimap<PathHeader.PageId, PathHeader.SectionId> pageUniqueness = TreeMultimap.create();
        Multimap<String, Pair<PathHeader.SectionId, PathHeader.PageId>> headerUniqueness = TreeMultimap.create();
        Set<String> toTest;
        switch (getInclusion()) {
        default:
            toTest = StandardCodes.make().getLocaleCoverageLocales(Organization.cldr);
            break;
        case 10:
            toTest = factory.getAvailable();
            break;
        }
        toTest = ImmutableSet.<String> builder().add("en").addAll(toTest).build();
        Set<String> seenPaths = new HashSet<>();
        Set<String> localSeenPaths = new TreeSet<>();
        for (String locale : toTest) {
            localSeenPaths.clear();
            for (String p : factory.make(locale, true).fullIterable()) {
                if (p.startsWith("//ldml/identity/")) {
                    continue;
                }
                if (seenPaths.contains(p)) {
                    continue;
                }
                seenPaths.add(p);
                localSeenPaths.add(p);
                // if (p.contains("symbol[@alt") && failures == null) {
                // PathHeader result = pathHeaderFactory2.fromPath(p, failures = new
                // ArrayList<String>());
                // logln("Matching " + p + ": " + result + "\t" +
                // result.getSurveyToolStatus());
                // for (String failure : failures) {
                // logln("\t" + failure);
                // }
                // }
                PathHeader ph = pathHeaderFactory2.fromPath(p);
                if (ph == null) {
                    errln("Failed to create path from: " + p);
                    continue;
                }
                final SectionId sectionId = ph.getSectionId();
                if (sectionId != SectionId.Special) {
                    pageUniqueness.put(ph.getPageId(), sectionId);
                    headerUniqueness.put(ph.getHeader(), new Pair<>(sectionId, ph.getPageId()));
                }
            }
            if (!localSeenPaths.isEmpty()) {
                logln(locale + ": checked " + localSeenPaths.size() + " new paths");
            }
        }
        Set<String> missing = pathHeaderFactory2.getUnmatchedRegexes();
        if (missing.size() != 0) {
            for (String e : missing) {
                if (e.contains("//ldml/")) {
                    if (e.contains("//ldml/rbnf/") || e.contains("//ldml/segmentations/") || e.contains("//ldml/collations/")) {
                        continue;
                    }
                    logln("Path Regex never matched:\t" + e);
                }
            }
        }

        for (Entry<PageId, Collection<SectionId>> e : pageUniqueness.asMap().entrySet()) {
            Collection<SectionId> values = e.getValue();
            if (values.size() != 1) {
                warnln("Duplicate page in section: " + CldrUtility.toString(e));
            }
        }

        for (Entry<String, Collection<Pair<SectionId, PageId>>> e : headerUniqueness.asMap().entrySet()) {
            Collection<Pair<SectionId, PageId>> values = e.getValue();
            if (values.size() != 1) {
                warnln("Duplicate header in (section,page): " + CldrUtility.toString(e));
            }
        }
    }

    public void Test6170() {
        String p1 = "//ldml/units/unitLength[@type=\"narrow\"]/unit[@type=\"speed-kilometer-per-hour\"]/unitPattern[@count=\"other\"]";
        String p2 = "//ldml/units/unitLength[@type=\"narrow\"]/unit[@type=\"area-square-meter\"]/unitPattern[@count=\"other\"]";
        PathHeader ph1 = pathHeaderFactory.fromPath(p1);
        PathHeader ph2 = pathHeaderFactory.fromPath(p2);
        int comp12 = ph1.compareTo(ph2);
        int comp21 = ph2.compareTo(ph1);
        assertEquals("comp ph", comp12, -comp21);
    }

    public void TestVariant() {
        PathHeader p1 = pathHeaderFactory
            .fromPath("//ldml/localeDisplayNames/languages/language[@type=\"ug\"][@alt=\"variant\"]");
        PathHeader p2 = pathHeaderFactory
            .fromPath("//ldml/localeDisplayNames/languages/language[@type=\"ug\"]");
        assertNotEquals("variants", p1, p2);
        assertNotEquals("variants", p1.toString(), p2.toString());
        // Code Lists Languages Arabic Script ug-variant
    }

    public void Test4587() {
        String test = "//ldml/dates/timeZoneNames/metazone[@type=\"Pacific/Wallis\"]/short/standard";
        PathHeader ph = pathHeaderFactory.fromPath(test);
        if (ph == null) {
            errln("Failure with " + test);
        } else {
            logln(ph + "\t" + test);
        }
    }

    public void TestMiscPatterns() {
        String test = "//ldml/numbers/miscPatterns[@numberSystem=\"arab\"]/pattern[@type=\"atLeast\"]";
        PathHeader ph = pathHeaderFactory.fromPath(test);
        assertNotNull("MiscPatterns path not found", ph);
        if (false)
            System.out.println(english.getStringValue(test));
    }

    public void TestPluralOrder() {
        Set<PathHeader> sorted = new TreeSet<PathHeader>();
        for (String locale : new String[] { "ru", "ar", "ja" }) {
            sorted.clear();
            CLDRFile cldrFile = info.getCLDRFile(locale, true);
            CoverageLevel2 coverageLevel = CoverageLevel2.getInstance(locale);
            for (String path : cldrFile.fullIterable()) {
                if (!path.contains("@count")) {
                    continue;
                }
                Level level = coverageLevel.getLevel(path);
                if (Level.MODERN.compareTo(level) < 0) {
                    continue;
                }
                PathHeader p = pathHeaderFactory.fromPath(path);
                sorted.add(p);
            }
            for (PathHeader p : sorted) {
                logln(locale + "\t" + p + "\t" + p.getOriginalPath());
            }
        }
    }

    static final String APPEND_TIMEZONE = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/appendItems/appendItem[@request=\"Timezone\"]";
    static final String APPEND_TIMEZONE_END = "/dateTimeFormats/appendItems/appendItem[@request=\"Timezone\"]";
    static final String BEFORE_PH = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\"ms\"]";
    static final String AFTER_PH = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/intervalFormats/intervalFormatItem[@id=\"d\"]/greatestDifference[@id=\"d\"]";

    public void TestAppendTimezone() {
        CLDRFile cldrFile = info.getEnglish();
        CoverageLevel2 coverageLevel = CoverageLevel2.getInstance("en");
        assertEquals("appendItem:Timezone", Level.MODERATE,
            coverageLevel.getLevel(APPEND_TIMEZONE));

        PathHeader ph = pathHeaderFactory.fromPath(APPEND_TIMEZONE);
        assertEquals("appendItem:Timezone pathheader", "Timezone", ph.getCode());
        // check that they are in the right place (they weren't before!)
        PathHeader phBefore = pathHeaderFactory.fromPath(BEFORE_PH);
        PathHeader phAfter = pathHeaderFactory.fromPath(AFTER_PH);
        assertTrue(phBefore, LEQ, ph);
        assertTrue(ph, LEQ, phAfter);

        PathDescription pathDescription = new PathDescription(supplemental,
            english, null, null, PathDescription.ErrorHandling.CONTINUE);
        String description = pathDescription.getDescription(APPEND_TIMEZONE,
            "tempvalue", null, null);
        assertTrue("appendItem:Timezone pathDescription",
            description.contains("“Timezone”"));

        PatternPlaceholders patternPlaceholders = PatternPlaceholders
            .getInstance();
        PlaceholderStatus status = patternPlaceholders
            .getStatus(APPEND_TIMEZONE);
        assertEquals("appendItem:Timezone placeholders",
            PlaceholderStatus.REQUIRED, status);

        Map<String, PlaceholderInfo> placeholderInfo = patternPlaceholders
            .get(APPEND_TIMEZONE);
        PlaceholderInfo placeholderInfo2 = placeholderInfo.get("{1}");
        if (assertNotNull("appendItem:Timezone placeholders", placeholderInfo2)) {
            assertEquals("appendItem:Timezone placeholders",
                "APPEND_FIELD_FORMAT", placeholderInfo2.name);
            assertEquals("appendItem:Timezone placeholders", "Pacific Time",
                placeholderInfo2.example);
        }
        ExampleGenerator eg = new ExampleGenerator(cldrFile, cldrFile,
            CLDRPaths.SUPPLEMENTAL_DIRECTORY);
        String example = eg.getExampleHtml(APPEND_TIMEZONE,
            cldrFile.getStringValue(APPEND_TIMEZONE));
        String result = ExampleGenerator.simplify(example, false);
        assertEquals("", "〖❬6:25:59 PM❭ ❬GMT❭〗", result);
    }

    public void TestOptional() {
        if (true) return;
        Map<PathHeader, String> sorted = new TreeMap<PathHeader, String>();
        XPathParts parts = new XPathParts();
        for (String locale : new String[] { "af" }) {
            sorted.clear();
            CLDRFile cldrFile = info.getCLDRFile(locale, true);
            CoverageLevel2 coverageLevel = CoverageLevel2.getInstance(locale);
            for (String path : cldrFile.fullIterable()) {
                // if (!path.contains("@count")) {
                // continue;
                // }
                Level level = coverageLevel.getLevel(path);
                if (supplemental.isDeprecated(DtdType.ldml, path)) {
                    continue;
                }

                if (Level.OPTIONAL.compareTo(level) != 0) {
                    continue;
                }

                PathHeader p = pathHeaderFactory.fromPath(path);
                final SurveyToolStatus status = p.getSurveyToolStatus();
                if (status == SurveyToolStatus.DEPRECATED) {
                    continue;
                }
                sorted.put(
                    p,
                    locale + "\t" + status + "\t" + p + "\t"
                        + p.getOriginalPath());
            }
            Set<String> codes = new LinkedHashSet<String>();
            PathHeader old = null;
            String line = null;
            for (Entry<PathHeader, String> s : sorted.entrySet()) {
                PathHeader p = s.getKey();
                String v = s.getValue();
                if (old == null) {
                    line = v;
                    codes.add(p.getCode());
                } else if (p.getSectionId() == old.getSectionId()
                    && p.getPageId() == old.getPageId()
                    && p.getHeader().equals(old.getHeader())) {
                    codes.add(p.getCode());
                } else {
                    logln(line + "\t" + codes.toString());
                    codes.clear();
                    line = v;
                    codes.add(p.getCode());
                }
                old = p;
            }
            logln(line + "\t" + codes.toString());
        }
    }

    public void TestPluralCanonicals() {
        Relation<String, String> data = Relation.of(
            new LinkedHashMap<String, Set<String>>(), TreeSet.class);
        for (String locale : factory.getAvailable()) {
            if (locale.contains("_")) {
                continue;
            }
            PluralInfo info = supplemental.getPlurals(PluralType.cardinal,
                locale);
            Set<String> keywords = info.getCanonicalKeywords();
            data.put(keywords.toString(), locale);
        }
        for (Entry<String, Set<String>> entry : data.keyValuesSet()) {
            logln(entry.getKey() + "\t" + entry.getValue());
        }
    }

    public void TestPluralPaths() {
        // do the following line once, when the file is opened
        Set<String> filePaths = pathHeaderFactory.pathsForFile(english);

        // check that English doesn't contain few or many
        verifyContains(PageId.Duration, filePaths, "few", false);
        verifyContains(PageId.C_NAmerica, filePaths, "many", false);
        verifyContains(PageId.C_SAmerica, filePaths, "many", false);
        verifyContains(PageId.C_NWEurope, filePaths, "many", false);
        verifyContains(PageId.C_SEEurope, filePaths, "many", false);
        verifyContains(PageId.C_NAfrica, filePaths, "many", false);
        verifyContains(PageId.C_WAfrica, filePaths, "many", false);
        verifyContains(PageId.C_SAfrica, filePaths, "many", false);
        verifyContains(PageId.C_EAfrica, filePaths, "many", false);
        verifyContains(PageId.C_CAsia, filePaths, "many", false);
        verifyContains(PageId.C_WAsia, filePaths, "many", false);
        verifyContains(PageId.C_SEAsia, filePaths, "many", false);
        verifyContains(PageId.C_Oceania, filePaths, "many", false);
        verifyContains(PageId.C_Unknown, filePaths, "many", false);

        // check that Arabic does contain few and many
        filePaths = pathHeaderFactory.pathsForFile(info.getCLDRFile("ar", true));

        verifyContains(PageId.Duration, filePaths, "few", true);
        verifyContains(PageId.C_NAmerica, filePaths, "many", true);
        verifyContains(PageId.C_SAmerica, filePaths, "many", true);
        verifyContains(PageId.C_NWEurope, filePaths, "many", true);
        verifyContains(PageId.C_SEEurope, filePaths, "many", true);
        verifyContains(PageId.C_NAfrica, filePaths, "many", true);
        verifyContains(PageId.C_WAfrica, filePaths, "many", true);
        verifyContains(PageId.C_SAfrica, filePaths, "many", true);
        verifyContains(PageId.C_EAfrica, filePaths, "many", true);
        verifyContains(PageId.C_CAsia, filePaths, "many", true);
        verifyContains(PageId.C_WAsia, filePaths, "many", true);
        verifyContains(PageId.C_SEAsia, filePaths, "many", true);
        verifyContains(PageId.C_Oceania, filePaths, "many", true);
        verifyContains(PageId.C_Unknown, filePaths, "many", true);
    }

    public void TestCoverage() {
        Map<Row.R2<SectionId, PageId>, Counter<Level>> data = new TreeMap<Row.R2<SectionId, PageId>, Counter<Level>>();
        CLDRFile cldrFile = english;
        for (String path : cldrFile.fullIterable()) {
            if (supplemental.isDeprecated(DtdType.ldml, path)) {
                errln("Deprecated path in English: " + path);
                continue;
            }
            Level level = supplemental.getCoverageLevel(path,
                cldrFile.getLocaleID());
            PathHeader p = pathHeaderFactory.fromPath(path);
            SurveyToolStatus status = p.getSurveyToolStatus();

            boolean hideCoverage = level == Level.OPTIONAL;
            boolean hidePathHeader = status == SurveyToolStatus.DEPRECATED
                || status == SurveyToolStatus.HIDE;
            if (hidePathHeader != hideCoverage) {
                String message = "PathHeader: " + status + ", Coverage: "
                    + level + ": " + path;
                if (hidePathHeader && !hideCoverage) {
                    errln(message);
                } else if (!hidePathHeader && hideCoverage) {
                    logln(message);
                }
            }
            final R2<SectionId, PageId> key = Row.of(p.getSectionId(),
                p.getPageId());
            Counter<Level> counter = data.get(key);
            if (counter == null) {
                data.put(key, counter = new Counter<Level>());
            }
            counter.add(level, 1);
        }
        StringBuffer b = new StringBuffer("\t");
        for (Level level : Level.values()) {
            b.append("\t" + level);
        }
        logln(b.toString());
        for (Entry<R2<SectionId, PageId>, Counter<Level>> entry : data
            .entrySet()) {
            b.setLength(0);
            b.append(entry.getKey().get0() + "\t" + entry.getKey().get1());
            Counter<Level> counter = entry.getValue();
            long total = 0;
            for (Level level : Level.values()) {
                total += counter.getCount(level);
                b.append("\t" + total);
            }
            logln(b.toString());
        }
    }

    public void Test00AFile() {
        final String localeId = "en";
        Counter<Level> counter = new Counter<Level>();
        Map<String, PathHeader> uniqueness = new HashMap<String, PathHeader>();
        Set<String> alreadySeen = new HashSet<String>();
        check(localeId, true, uniqueness, alreadySeen);
        // check paths
        for (Entry<SectionId, Set<PageId>> sectionAndPages : PathHeader.Factory
            .getSectionIdsToPageIds().keyValuesSet()) {
            final SectionId section = sectionAndPages.getKey();
            if (section == SectionId.Supplemental || section == SectionId.BCP47) {
                continue;
            }
            logln(section.toString());
            for (PageId page : sectionAndPages.getValue()) {
                final Set<String> cachedPaths = PathHeader.Factory
                    .getCachedPaths(section, page);
                if (cachedPaths == null) {
                    if (!badZonePages.contains(page) && page != PageId.Unknown) {
                        errln("Null pages for: " + section + "\t" + page);
                    }
                } else if (section == SectionId.Special
                    && page == PageId.Unknown) {
                    // skip
                } else if (section == SectionId.Timezones
                    && page == PageId.UnknownT) {
                    // skip
                } else if (section == SectionId.Misc
                    && page == PageId.Transforms) {
                    // skip
                } else {

                    int count2 = cachedPaths.size();
                    if (count2 == 0) {
                        warnln("Missing pages for: " + section + "\t" + page);
                    } else {
                        counter.clear();
                        for (String s : cachedPaths) {
                            Level coverage = supplemental.getCoverageLevel(s,
                                localeId);
                            counter.add(coverage, 1);
                        }
                        String countString = "";
                        int total = 0;
                        for (Level item : Level.values()) {
                            long count = counter.get(item);
                            if (count != 0) {
                                if (!countString.isEmpty()) {
                                    countString += ",\t+";
                                }
                                total += count;
                                countString += item + "=" + total;
                            }
                        }
                        logln("\t" + page + "\t" + countString);
                        if (page.toString().startsWith("Unknown")) {
                            logln("\t\t" + cachedPaths);
                        }
                    }
                }
            }
        }
    }

    public void TestMetazones() {

        CLDRFile nativeFile = info.getEnglish();
        Set<PathHeader> pathHeaders = getPathHeaders(nativeFile);
        // String oldPage = "";
        String oldHeader = "";
        for (PathHeader entry : pathHeaders) {
            final String page = entry.getPage();
            // if (!oldPage.equals(page)) {
            // logln(page);
            // oldPage = page;
            // }
            String header = entry.getHeader();
            if (!oldHeader.equals(header)) {
                logln(page + "\t" + header);
                oldHeader = header;
            }
        }
    }

    public Set<PathHeader> getPathHeaders(CLDRFile nativeFile) {
        Set<PathHeader> pathHeaders = new TreeSet<PathHeader>();
        for (String path : nativeFile.fullIterable()) {
            PathHeader p = pathHeaderFactory.fromPath(path);
            pathHeaders.add(p);
        }
        return pathHeaders;
    }

    public void verifyContains(PageId pageId, Set<String> filePaths,
        String substring, boolean contains) {
        String path;
        path = findOneContaining(allPaths(pageId, filePaths), substring);
        if (contains) {
            if (path == null) {
                errln("No path contains <" + substring + ">");
            }
        } else {
            if (path != null) {
                errln("Path contains <" + substring + ">\t" + path);
            }
        }
    }

    private String findOneContaining(Collection<String> allPaths,
        String substring) {
        for (String path : allPaths) {
            if (path.contains(substring)) {
                return path;
            }
        }
        return null;
    }

    public Set<String> allPaths(PageId pageId, Set<String> filePaths) {
        Set<String> result = PathHeader.Factory.getCachedPaths(
            pageId.getSectionId(), pageId);
        result.retainAll(filePaths);
        return result;
    }

    public void TestUniqueness() {
        CLDRFile nativeFile = info.getEnglish();
        Map<PathHeader, String> headerToPath = new HashMap<PathHeader, String>();
        Map<String, String> headerVisibleToPath = new HashMap<String, String>();
        for (String path : nativeFile.fullIterable()) {
            PathHeader p = pathHeaderFactory.fromPath(path);
            if (p.getSectionId() == SectionId.Special) {
                continue;
            }
            String old = headerToPath.get(p);
            if (old == null) {
                headerToPath.put(p, path);
            } else if (!old.equals(path)) {
                if (true) { // for debugging
                    pathHeaderFactory.clearCache();
                    List<String> failuresOld = new ArrayList<>();
                    pathHeaderFactory.fromPath(old, failuresOld);
                    List<String> failuresPath = new ArrayList<>();
                    pathHeaderFactory.fromPath(path, failuresPath);
                }
                errln("Collision with path " + p + "\t" + old + "\t" + path);
            }
            final String visible = p.toString();
            old = headerVisibleToPath.get(visible);
            if (old == null) {
                headerVisibleToPath.put(visible, path);
            } else if (!old.equals(path)) {
                errln("Collision with path " + visible + "\t" + old + "\t"
                    + path);
            }
        }
    }

    public void TestStatus() {
        CLDRFile nativeFile = info.getEnglish();
        PathStarrer starrer = new PathStarrer();
        EnumMap<SurveyToolStatus, Relation<String, String>> info2 = new EnumMap<SurveyToolStatus, Relation<String, String>>(
            SurveyToolStatus.class);
        Set<String> nuked = new HashSet<String>();
        PrettyPath pp = new PrettyPath();
        XPathParts parts = new XPathParts();
        Set<String> deprecatedStar = new HashSet<String>();
        Set<String> differentStar = new HashSet<String>();

        for (String path : nativeFile.fullIterable()) {

            PathHeader p = pathHeaderFactory.fromPath(path);
            final SurveyToolStatus surveyToolStatus = p.getSurveyToolStatus();

            if (p.getSectionId() == SectionId.Special
                && surveyToolStatus == SurveyToolStatus.READ_WRITE) {
                errln("SurveyToolStatus should not be " + surveyToolStatus
                    + ": " + p);
            }

            final SurveyToolStatus tempSTS = surveyToolStatus == SurveyToolStatus.DEPRECATED ? SurveyToolStatus.HIDE
                : surveyToolStatus;
            String starred = starrer.set(path);
            List<String> attr = starrer.getAttributes();
            if (surveyToolStatus != SurveyToolStatus.READ_WRITE) {
                nuked.add(starred);
            }

            // check against old
            SurveyToolStatus oldStatus = SurveyToolStatus.READ_WRITE;

            if (tempSTS != oldStatus
                && oldStatus != SurveyToolStatus.READ_WRITE
                && !path.endsWith(APPEND_TIMEZONE_END)) {
                if (!differentStar.contains(starred)) {
                    errln("Different from old:\t" + oldStatus + "\tnew:\t"
                        + surveyToolStatus + "\t" + path);
                    differentStar.add(starred);
                }
            }

            // check against deprecated
            boolean isDeprecated = supplemental.isDeprecated(DtdType.ldml, path);
            if (isDeprecated != (surveyToolStatus == SurveyToolStatus.DEPRECATED)) {
                if (!deprecatedStar.contains(starred)) {
                    errln("Different from DtdData deprecated:\t"
                        + isDeprecated + "\t" + surveyToolStatus + "\t"
                        + path);
                    deprecatedStar.add(starred);
                }
            }

            Relation<String, String> data = info2.get(surveyToolStatus);
            if (data == null) {
                info2.put(
                    surveyToolStatus,
                    data = Relation.of(new TreeMap<String, Set<String>>(),
                        TreeSet.class));
            }
            data.put(starred, CollectionUtilities.join(attr, "|"));
        }
        for (Entry<SurveyToolStatus, Relation<String, String>> entry : info2
            .entrySet()) {
            final SurveyToolStatus status = entry.getKey();
            for (Entry<String, Set<String>> item : entry.getValue()
                .keyValuesSet()) {
                final String starred = item.getKey();
                if (status == SurveyToolStatus.READ_WRITE
                    && !nuked.contains(starred)) {
                    continue;
                }
                logln(status + "\t" + starred + "\t" + item.getValue());
            }
        }
    }

    public void TestPathsNotInEnglish() {
        Set<String> englishPaths = new HashSet<String>();
        for (String path : english.fullIterable()) {
            englishPaths.add(path);
        }
        Set<String> alreadySeen = new HashSet<String>(englishPaths);

        for (String locale : factory.getAvailable()) {
            CLDRFile nativeFile = info.getCLDRFile(locale, false);
            CoverageLevel2 coverageLevel2 = null;
            for (String path : nativeFile.fullIterable()) {
                if (alreadySeen.contains(path) || path.contains("@count")) {
                    continue;
                }
                if (coverageLevel2 == null) {
                    coverageLevel2 = CoverageLevel2.getInstance(locale);
                }
                Level level = coverageLevel2.getLevel(path);
                if (Level.COMPREHENSIVE.compareTo(level) < 0) {
                    continue;
                }
                logln("Path not in English\t" + locale + "\t" + path);
                alreadySeen.add(path);
            }
        }
    }

    public void TestPathDescriptionCompleteness() {
        PathDescription pathDescription = new PathDescription(supplemental,
            english, null, null, PathDescription.ErrorHandling.CONTINUE);
        Matcher normal = PatternCache.get(
            "http://cldr.org/translation/[a-zA-Z0-9_]").matcher("");
        // http://cldr.unicode.org/translation/plurals#TOC-Minimal-Pairs
        Set<String> alreadySeen = new HashSet<String>();
        PathStarrer starrer = new PathStarrer();

        checkPathDescriptionCompleteness(pathDescription, normal,
            "//ldml/numbers/defaultNumberingSystem", alreadySeen, starrer);
        for (PathHeader pathHeader : getPathHeaders(english)) {
            final SurveyToolStatus surveyToolStatus = pathHeader
                .getSurveyToolStatus();
            if (surveyToolStatus == SurveyToolStatus.DEPRECATED
                || surveyToolStatus == SurveyToolStatus.HIDE) {
                continue;
            }
            String path = pathHeader.getOriginalPath();
            checkPathDescriptionCompleteness(pathDescription, normal, path,
                alreadySeen, starrer);
        }
    }

    public void checkPathDescriptionCompleteness(
        PathDescription pathDescription, Matcher normal, String path,
        Set<String> alreadySeen, PathStarrer starrer) {
        String value = english.getStringValue(path);
        String description = pathDescription.getDescription(path, value, null,
            null);
        String starred = starrer.set(path);
        if (alreadySeen.contains(starred)) {
            return;
        } else if (description == null) {
            errln("Path has no description:\t" + value + "\t" + path);
        } else if (!description.contains("http://")) {
            errln("Description has no URL:\t" + description + "\t" + value
                + "\t" + path);
        } else if (!normal.reset(description).find()) {
            errln("Description has generic URL, fix to be specific:\t"
                + description + "\t" + value + "\t" + path);
        } else if (description == PathDescription.MISSING_DESCRIPTION) {
            errln("Fallback Description:\t" + value + "\t" + path);
        } else {
            return;
        }
        // Add if we had a problem, keeping us from being overwhelmed with
        // errors.
        alreadySeen.add(starred);
    }

    public void TestTerritoryOrder() {
        final Set<String> goodAvailableCodes = CLDRConfig.getInstance()
            .getStandardCodes().getGoodAvailableCodes("territory");
        Set<String> results = showContained("001", 0, new HashSet<String>(
            goodAvailableCodes));
        results.remove("ZZ");
        for (String territory : results) {
            String sub = Containment.getSubcontinent(territory);
            String cont = Containment.getContinent(territory);
            errln("Missing\t" + getNameAndOrder(territory) + "\t"
                + getNameAndOrder(sub) + "\t" + getNameAndOrder(cont));
        }
    }

    private Set<String> showContained(String territory, int level,
        Set<String> soFar) {
        if (!soFar.contains(territory)) {
            return soFar;
        }
        soFar.remove(territory);
        Set<String> contained = supplemental.getContained(territory);
        if (contained == null) {
            return soFar;
        }
        for (String containedItem : contained) {
            logln(level + "\t" + getNameAndOrder(territory) + "\t"
                + getNameAndOrder(containedItem));
        }
        for (String containedItem : contained) {
            showContained(containedItem, level + 1, soFar);
        }
        return soFar;
    }

    private String getNameAndOrder(String territory) {
        return territory + "\t"
            + english.getName(CLDRFile.TERRITORY_NAME, territory) + "\t"
            + Containment.getOrder(territory);
    }

    public void TestZCompleteness() {
        Map<String, PathHeader> uniqueness = new HashMap<String, PathHeader>();
        Set<String> alreadySeen = new HashSet<String>();
        LanguageTagParser ltp = new LanguageTagParser();
        int count = 0;
        for (String locale : factory.getAvailable()) {
            if (!ltp.set(locale).getRegion().isEmpty()) {
                continue;
            }
            check(locale, false, uniqueness, alreadySeen);
            ++count;
        }
        logln("Count:\t" + count);
    }

    public void check(String localeID, boolean resolved,
        Map<String, PathHeader> uniqueness, Set<String> alreadySeen) {
        CLDRFile nativeFile = info.getCLDRFile(localeID, resolved);
        int count = 0;
        for (String path : nativeFile) {
            if (alreadySeen.contains(path)) {
                continue;
            }
            alreadySeen.add(path);
            final PathHeader pathHeader = pathHeaderFactory.fromPath(path);
            ++count;
            if (pathHeader == null) {
                errln("Null pathheader for " + path);
            } else {
                String visible = pathHeader.toString();
                PathHeader old = uniqueness.get(visible);
                if (pathHeader.getSectionId() == SectionId.Timezones) {
                    final PageId pageId = pathHeader.getPageId();
                    if (badZonePages.contains(pageId)
                        && !pathHeader.getCode().equals("Unknown")) {
                        String msg = "Bad page ID:\t" + pageId + "\t" + pathHeader + "\t" + path;
                        if (!logKnownIssue("cldrbug:7802", "ICU/CLDR time zone data sync problem - " + msg)) {
                            errln("Bad page ID:\t" + pageId + "\t" + pathHeader
                                + "\t" + path);
                        }
                    }
                }
                if (old == null) {
                    if (pathHeader.getSection().equals("Special")) {
                        if (pathHeader.getSection().equals("Unknown")) {
                            errln("PathHeader has fallback: " + visible + "\t"
                                + pathHeader.getOriginalPath());
                            // } else {
                            // logln("Special:\t" + visible + "\t" +
                            // pathHeader.getOriginalPath());
                        }
                    }
                    uniqueness.put(visible, pathHeader);
                } else if (!old.equals(pathHeader)) {
                    if (pathHeader.getSectionId() == SectionId.Special) {
                        logln("Special PathHeader not unique: " + visible
                            + "\t" + pathHeader.getOriginalPath() + "\t"
                            + old.getOriginalPath());
                    } else {
                        errln("PathHeader not unique: " + visible + "\t"
                            + pathHeader.getOriginalPath() + "\t"
                            + old.getOriginalPath());
                    }
                }
            }
        }
        logln(localeID + "\t" + count);
    }

    public void TestContainment() {
        Map<String, Map<String, String>> metazoneToRegionToZone = supplemental
            .getMetazoneToRegionToZone();
        Map<String, String> metazoneToContinent = supplemental
            .getMetazoneToContinentMap();
        for (String metazone : metazoneToRegionToZone.keySet()) {
            Map<String, String> regionToZone = metazoneToRegionToZone
                .get(metazone);
            String worldZone = regionToZone.get("001");
            String territory = Containment.getRegionFromZone(worldZone);
            if (territory == null) {
                territory = "ZZ";
            }
            String cont = Containment.getContinent(territory);
            int order = Containment.getOrder(territory);
            String sub = Containment.getSubcontinent(territory);
            String revision = PathHeader.getMetazonePageTerritory(metazone);
            String continent = metazoneToContinent.get(metazone);
            if (continent == null) {
                continent = "UnknownT";
            }
            // Russia, Antarctica => territory
            // in Australasia, Asia, S. America => subcontinent
            // in N. America => N. America (grouping of 3 subcontinents)
            // in everything else => continent

            if (territory.equals("RU")) {
                assertEquals("Russia special case", "RU", revision);
            } else if (territory.equals("US")) {
                assertEquals("N. America special case", "003", revision);
            } else if (territory.equals("BR")) {
                assertEquals("S. America special case", "005", revision);
            }
            if (isVerbose()) {
                String name = english.getName(CLDRFile.TERRITORY_NAME, cont);
                String name2 = english.getName(CLDRFile.TERRITORY_NAME, sub);
                String name3 = english.getName(CLDRFile.TERRITORY_NAME,
                    territory);
                String name4 = english.getName(CLDRFile.TERRITORY_NAME,
                    revision);

                logln(metazone + "\t" + continent + "\t" + name + "\t" + name2
                    + "\t" + name3 + "\t" + order + "\t" + name4);
            }
        }
    }

    public void TestZ() {
        PathStarrer pathStarrer = new PathStarrer();
        pathStarrer.setSubstitutionPattern("%A");

        Set<PathHeader> sorted = new TreeSet<PathHeader>();
        Map<String, String> missing = new TreeMap<String, String>();
        Map<String, String> skipped = new TreeMap<String, String>();
        Map<String, String> collide = new TreeMap<String, String>();

        logln("Traversing Paths");
        for (String path : english) {
            PathHeader pathHeader = pathHeaderFactory.fromPath(path);
            String value = english.getStringValue(path);
            if (pathHeader == null) {
                final String starred = pathStarrer.set(path);
                missing.put(starred, value + "\t" + path);
                continue;
            }
            if (pathHeader.getSection().equalsIgnoreCase("skip")) {
                final String starred = pathStarrer.set(path);
                skipped.put(starred, value + "\t" + path);
                continue;
            }
            sorted.add(pathHeader);
        }
        logln("\nConverted:\t" + sorted.size());
        String lastHeader = "";
        String lastPage = "";
        String lastSection = "";
        List<String> threeLevel = new ArrayList<String>();
        Status status = new Status();
        CoverageLevel2 coverageLevel2 = CoverageLevel2.getInstance("en");

        for (PathHeader pathHeader : sorted) {
            String original = pathHeader.getOriginalPath();
            if (!original.equals(status.pathWhereFound)) {
                continue;
            }
            if (!lastSection.equals(pathHeader.getSection())) {
                logln("");
                threeLevel.add(pathHeader.getSection());
                threeLevel.add("\t" + pathHeader.getPage());
                threeLevel.add("\t\t" + pathHeader.getHeader());
                lastSection = pathHeader.getSection();
                lastPage = pathHeader.getPage();
                lastHeader = pathHeader.getHeader();
            } else if (!lastPage.equals(pathHeader.getPage())) {
                logln("");
                threeLevel.add("\t" + pathHeader.getPage());
                threeLevel.add("\t\t" + pathHeader.getHeader());
                lastPage = pathHeader.getPage();
                lastHeader = pathHeader.getHeader();
            } else if (!lastHeader.equals(pathHeader.getHeader())) {
                logln("");
                threeLevel.add("\t\t" + pathHeader.getHeader());
                lastHeader = pathHeader.getHeader();
            }
            logln(pathHeader + "\t" + coverageLevel2.getLevel(original) + "\t"
                + english.getStringValue(pathHeader.getOriginalPath())
                + "\t" + pathHeader.getOriginalPath());
        }
        if (collide.size() != 0) {
            errln("\nCollide:\t" + collide.size());
            for (Entry<String, String> item : collide.entrySet()) {
                errln("\t" + item);
            }
        }
        if (missing.size() != 0) {
            errln("\nMissing:\t" + missing.size());
            for (Entry<String, String> item : missing.entrySet()) {
                errln("\t" + item.getKey() + "\tvalue:\t" + item.getValue());
            }
        }
        if (skipped.size() != 0) {
            errln("\nSkipped:\t" + skipped.size());
            for (Entry<String, String> item : skipped.entrySet()) {
                errln("\t" + item);
            }
        }
        Counter<PathHeader.Factory.CounterData> counterData = pathHeaderFactory
            .getInternalCounter();
        logln("\nInternal Counter:\t" + counterData.size());
        for (PathHeader.Factory.CounterData item : counterData.keySet()) {
            logln("\t" + counterData.getCount(item) + "\t" + item.get2() // externals
                + "\t" + item.get3() + "\t" + item.get0() // internals
                + "\t" + item.get1());
        }
        logln("\nMenus/Headers:\t" + threeLevel.size());
        for (String item : threeLevel) {
            logln(item);
        }
        LinkedHashMap<String, Set<String>> sectionsToPages = pathHeaderFactory
            .getSectionsToPages();
        logln("\nMenus:\t" + sectionsToPages.size());
        for (Entry<String, Set<String>> item : sectionsToPages.entrySet()) {
            final String section = item.getKey();
            for (String page : item.getValue()) {
                logln("\t" + section + "\t" + page);
                int count = 0;
                for (String path : pathHeaderFactory.filterCldr(section, page,
                    english)) {
                    count += 1; // just count them.
                }
                logln("\t" + count);
            }
        }
    }

    public void TestOrder() {
        String[] paths = {
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\"format\"]/dayPeriodWidth[@type=\"narrow\"]/dayPeriod[@type=\"noon\"]",
            "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\"format\"]/dayPeriodWidth[@type=\"narrow\"]/dayPeriod[@type=\"afternoon1\"]",
        };
        PathHeader pathHeaderLast = null;
        for (String path : paths) {
            PathHeader pathHeader = pathHeaderFactory.fromPath(path);
            if (pathHeaderLast != null) {
                assertRelation("ordering", true, pathHeaderLast, LEQ, pathHeader);
            }
            pathHeaderLast = pathHeader;
        }

    }

    public void Test8414() {
        PathDescription pathDescription = new PathDescription(supplemental,
            english, null, null, PathDescription.ErrorHandling.CONTINUE);

        String prefix = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dayPeriods/dayPeriodContext[@type=\"";
        String suffix = "\"]/dayPeriodWidth[@type=\"wide\"]/dayPeriod[@type=\"morning1\"]";

        final String path0 = prefix + "format" + suffix;
        final String path1 = prefix + "stand-alone" + suffix;
        String v0 = english.getStringValue(path0);
        String v1 = english.getStringValue(path1);
        String p0 = pathDescription.getDescription(path0, v0, null, null);
        String p1 = pathDescription.getDescription(path1, v1, null, null);
        assertTrue("Check pd for format", p0.contains("in the morning"));
        assertTrue("Check pd for stand-alone", !p1.contains("in the morning"));
    }

    public void TestCompletenessNonLdmlDtd() {
        PathChecker pathChecker = new PathChecker();
        Set<String> directories = new LinkedHashSet<>();
        XPathParts parts = new XPathParts();
        Multimap<String, String> pathValuePairs = LinkedListMultimap.create();
        // get all the directories containing non-Ldml dtd files
        for (DtdType dtdType : DtdType.values()) {
            if (dtdType == DtdType.ldml || dtdType == DtdType.ldmlICU) {
                continue;
            }
            DtdData dtdData = DtdData.getInstance(dtdType);
            for (String dir : dtdType.directories) {
                if (DEBUG_DTD_TYPE != null && !DEBUG_DTD_TYPE.directories.contains(dir)) {
                    continue;
                }
                File dir2 = new File(COMMON_DIR + dir);
                logln(dir2.getName());
                for (String file : dir2.list()) {
                    // don't need to restrict with getFilesToTest(Arrays.asList(dir2.list()), "root", "en")) {
                    if (!file.endsWith(".xml")) {
                        continue;
                    }
                    if (DEBUG) warnln(" TestCompletenessNonLdmlDtd: " + dir + ", " + file);
                    logln(" \t" + file);
                    for (Pair<String, String> pathValue : XMLFileReader.loadPathValues(
                        dir2 + "/" + file, new ArrayList<Pair<String, String>>(), true)) {
                        final String path = pathValue.getFirst();
                        final String value = pathValue.getSecond();
//                        logln("\t\t" + path);
                        if (path.startsWith("//supplementalData/weekData/weekOf")) {
                            int debug = 0;
                        }
                        pathChecker.checkPathHeader(dtdData, path);
                    }
                    ;
                }
            }
        }
    }

    private class PathChecker {
        PathHeader.Factory phf = pathHeaderFactory;
        PathStarrer starrer = new PathStarrer().setSubstitutionPattern("%A");

        Set<String> badHeaders = new TreeSet<>();
        Map<PathHeader, PathHeader> goodHeaders = new HashMap<>();
        Set<PathHeader> seenBad = new HashSet<>();
        {
            phf.clearCache();
        }

        public void checkPathHeader(DtdData dtdData, String rawPath) {
            XPathParts pathPlain = XPathParts.getFrozenInstance(rawPath);
            if (dtdData.isMetadata(pathPlain)) {
                return;
            }
            if (dtdData.isDeprecated(pathPlain)) {
                return;
            }
            Multimap<String, String> extras = HashMultimap.create();
            Set<String> fixedPaths = dtdData.getRegularizedPaths(pathPlain, extras);
            if (fixedPaths != null) {
                for (String fixedPath : fixedPaths) {
                    checkSubpath(fixedPath);
                }
            }
            for (String path : extras.keySet()) {
                checkSubpath(path);
            }
        }

        public void checkSubpath(String path) {
            String message = ": Can't compute path header";
            PathHeader ph = null;
            try {
                ph = phf.fromPath(path);
                if (seenBad.contains(ph)) {
                    return;
                }
                if (ph.getPageId() == PageId.Deprecated) {
                    return; // don't care
                }
                if (ph.getPageId() != PageId.Unknown) {
                    PathHeader old = goodHeaders.put(ph, ph);
                    if (old != null && !path.equals(old.getOriginalPath())) {
                        errln("Duplicate path header for: " + ph
                            + "\n\t\t " + path
                            + "\n\t\t≠" + old.getOriginalPath());
                        seenBad.add(ph);
                    }
                    return;
                }
                // for debugging
                phf.clearCache();
                List<String> failures = new ArrayList<>();
                ph = phf.fromPath(path, failures);
                message = ": Unknown path header" + failures;
            } catch (Exception e) {
                message = ": Exception in path header: " + e.getMessage();
            }
            String star = starrer.set(path);
            if (badHeaders.add(star)) {
                errln(star + message + ", " + ph);
            }
        }
    }

    public void TestSupplementalItems() {
        //      <weekOfPreference ordering="weekOfYear weekOfMonth" locales="am az bs cs cy da el et hi ky lt mk sk ta th"/>
        // logln(pathHeaderFactory.getRegexInfo());
        CLDRFile supplementalFile = CLDRConfig.getInstance().getSupplementalFactory().make("supplementalData", false);
        List<String> failures = new ArrayList<>();
        Multimap<String, String> pathValuePairs = LinkedListMultimap.create();
        for (String test : With.in(supplementalFile.iterator("//supplementalData/weekData"))) {
            failures.clear();
            XPathParts parts = XPathParts.getTestInstance(supplementalFile.getFullXPath(test));
            supplementalFile.getDtdData().getRegularizedPaths(parts, pathValuePairs);
            for (Entry<String, Collection<String>> entry : pathValuePairs.asMap().entrySet()) {
                final String normalizedPath = entry.getKey();
                final Collection<String> normalizedValue = entry.getValue();
                PathHeader ph = pathHeaderFactory.fromPath(normalizedPath, failures);
                if (ph == null || ph.getSectionId() == SectionId.Special) {
                    errln("Failure with " + test + " => " + normalizedPath + " = " + normalizedValue);
                } else {
                    logln(ph + "\t" + test + " = " + normalizedValue);
                }
            }
        }
    }

    public void test10232() {
        String[][] tests = {
            { "MMM", "Formats - Flexible - Date Formats" },
            { "dMM", "Formats - Flexible - Date Formats" },
            { "h", "Formats - Flexible - 12 Hour Time Formats" },
            { "hm", "Formats - Flexible - 12 Hour Time Formats" },
            { "Ehm", "Formats - Flexible - 12 Hour Time Formats" },
            { "H", "Formats - Flexible - 24 Hour Time Formats" },
            { "Hm", "Formats - Flexible - 24 Hour Time Formats" },
            { "EHm", "Formats - Flexible - 24 Hour Time Formats" },
        };
        for (String[] test : tests) {
            String path = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/availableFormats/dateFormatItem[@id=\""
                + test[0] + "\"]";
            PathHeader pathHeader = pathHeaderFactory.fromPath(path);
            assertEquals("flexible formats", test[1] + "|" + test[0], pathHeader.getHeader() + "|" + pathHeader.getCode());
        }
    }
    
    // Moved from TestAnnotations and generalized
    public void testPathHeaderSize() {
        String locale = "ar"; // choose one with lots of plurals
        int maxSize = 700;
        boolean showTable = false; // only printed if test fails or verbose

        Factory factory = CLDRConfig.getInstance().getCommonAndSeedAndMainAndAnnotationsFactory();
        CLDRFile english = factory.make(locale, true);

        PathHeader.Factory phf = PathHeader.getFactory(CLDRConfig.getInstance().getEnglish());
        Counter<PageId> counterPageId = new Counter<>();
        Counter<PageId> counterPageIdAll = new Counter<>();
        for (String path : english) {
            Level level = CLDRConfig.getInstance().getSupplementalDataInfo().getCoverageLevel(path, locale);
            PathHeader ph = phf.fromPath(path);
            if (level.compareTo(Level.MODERN) <= 0) {
                counterPageId.add(ph.getPageId(), 1);
            }
            counterPageIdAll.add(ph.getPageId(), 1);
        }
        Set<R2<Long, PageId>> entrySetSortedByCount = counterPageId.getEntrySetSortedByCount(false, null);
        for (R2<Long, PageId> sizeAndPageId : entrySetSortedByCount) {
            long size = sizeAndPageId.get0();
            PageId pageId = sizeAndPageId.get1();
            if (!assertTrue(pageId.getSectionId() + "/" + pageId + " size (" + size
                + ") < " + maxSize + "?", size < maxSize)) {
                showTable = true;
            }
            // System.out.println(pageId + "\t" + size);
        }
        if (showTable || isVerbose()) {
            for (R2<Long, PageId> sizeAndPageId : entrySetSortedByCount) {
                PageId pageId = sizeAndPageId.get1();
                System.out.println(pageId.getSectionId() + "\t" + pageId + "\t" + sizeAndPageId.get0() + "\t" + counterPageIdAll.get(pageId));
            }
        }
    }
}

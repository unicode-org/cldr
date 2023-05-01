package org.unicode.cldr.unittest;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Counter;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.Organization;
import org.unicode.cldr.util.SimpleFactory;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.cldr.util.XMLSource;

import com.google.common.base.Joiner;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.util.Output;

// make this a JUnit test?
public class TestAliases extends TestFmwk {
    static final CLDRConfig config = CLDRConfig.getInstance();

    public static void main(String[] args) {
        new TestAliases().run(args);
    }

    public void testAlias() {
        String[][] testCases = {
            { "//ldml/foo[@fii=\"abc\"]", "//ldml" },
            { "//ldml/foo[@fii=\"ab/c\"]", "//ldml" },
            { "//ldml/foo[@fii=\"ab/[c\"]", "//ldml" },
        };
        for (String[] pair : testCases) {
            if (!XMLSource.Alias.stripLastElement(pair[0]).equals(pair[1])) {
                errln(Arrays.asList(pair).toString());
            }
        }
    }

    /** Check on
     * http://unicode.org/cldr/trac/ticket/9477
     *      <field type="quarter-narrow">
     *           <alias source="locale" path="../field[@type='quarter-short']"/>
     *      </field>
            <field type="quarter-short">
                <relativeTime type="future">
                    <relativeTimePattern count="one">in {0} qtr.</relativeTimePattern>
                    <relativeTimePattern count="other">in {0} qtrs.</relativeTimePattern>
                </relativeTime>
            </field>
     */
    public void testCountBase() {
        String[][] testCases = {
            { "en",
                "//ldml/numbers/currencyFormats/currencyFormatLength[@type=\"short\"]/currencyFormat[@type=\"standard\"]/pattern[@type=\"1000\"][@count=\"one\"]",
                "en",
                "//ldml/numbers/currencyFormats[@numberSystem=\"latn\"]/currencyFormatLength[@type=\"short\"]/currencyFormat[@type=\"standard\"]/pattern[@type=\"1000\"][@count=\"one\"]"
            },
            { "en", "//ldml/characterLabels/characterLabelPattern[@type=\"strokes\"][@count=\"one\"]",
                "en", "//ldml/characterLabels/characterLabelPattern[@type=\"strokes\"][@count=\"one\"]",
            },
            { "ak", "//ldml/characterLabels/characterLabelPattern[@type=\"strokes\"][@count=\"one\"]",
                "root", "//ldml/characterLabels/characterLabelPattern[@type=\"strokes\"][@count=\"other\"]",
            }
        };
        Status status = new Status();

        for (String[] row : testCases) {
            String locale = row[0];
            CLDRFile factory = config.getCldrFactory().make(locale, true);
            String originalPath = row[1];
            String expectedLocale = row[2];
            String expectedPath = row[3];
            String actualLocale = factory.getSourceLocaleID(originalPath, status);
            String actualPath = status.pathWhereFound;
            assertEquals("path", expectedPath, actualPath);
            assertEquals("locale", expectedLocale, actualLocale);
        }
    }

    static final CLDRFile en = config.getEnglish();

    public void testCountFull() {
        Status status = new Status();
        Set<String> sorted = new TreeSet<>();
        Matcher countMatcher = Pattern.compile("\\[@count=\"([^\"]*)\"\\]").matcher("");
        for (String path : en.fullIterable()) {
            if (!path.contains("@count") || !path.contains("/field")) { // TODO remove /field
                continue;
            }
            if (path.equals("//ldml/dates/fields/field[@type=\"wed-narrow\"]/relativeTime[@type=\"future\"]/relativeTimePattern[@count=\"one\"]")) {
                int debug = 0;
            }
            String actualLocale = en.getSourceLocaleID(path, status);
            String actualPath = status.pathWhereFound;
            if ("en".equals(actualLocale) && path.equals(actualPath)) {
                continue;
            }

            String value = en.getStringValue(path);
            String fullpath = en.getFullXPath(path);

            countMatcher.reset(path).find();
            String sourceCount = countMatcher.group(1);
            countMatcher.reset(actualPath).find();
            String actualCount = countMatcher.group(1);

            if (assertEquals(path, sourceCount, actualCount)) {
                continue;
            }
            en.getSourceLocaleID(path, status); // for debugging

            sorted.add("locale:\t" + actualLocale
                + "\nsource path:\t" + path
                + "\nsource fpath:\t" + (path.equals(fullpath) ? "=" : fullpath)
                + "\nactual path:\t" + (path.equals(actualPath) ? "=" : actualPath)
                + "\nactual value:\t" + value);
        }
        sorted.forEach(x -> System.out.println(x));
    }

    /**
     * Change to "testEmitChanged()" to emit a file of current inheritance.
     * @throws IOException
     */
    public void checkEmitChanged() throws IOException {
        Status status = new Status();
        Set<String> sorted = new TreeSet<>();
        try (PrintWriter out = FileUtilities.openUTF8Writer(CLDRPaths.AUX_DIRECTORY + "temp", "inheritance0.txt")) {
            for (CLDRFile factory : Arrays.asList(
                config.getCldrFactory().make("root", true),
                en,
                config.getCldrFactory().make("en_001", true), // example with double inheritance
                config.getCldrFactory().make("ak", true) // example with few strings
                )) {
                sorted.clear();
                out.println("source locale\tactual locale\tsource path\tactual path");
                String locale = factory.getLocaleID();
                for (String path : factory.fullIterable()) {
                    if (path.contains("calendar[@type=")
                        && !(path.contains("calendar[@type=\"gregorian")
                            || path.contains("calendar[@type=\"generic"))) {
                        continue;
                    }
                    if (path.contains("[@numberSystem=")
                        && !(path.contains("[@numberSystem=\"latn")
                            || path.contains("[@numberSystem=\"deva"))) {
                        continue;
                    }
                    String actualLocale = factory.getSourceLocaleID(path, status);
                    String actualPath = status.pathWhereFound;
                    if (path.equals(actualPath)) {
                        continue;
                    }

                    sorted.add(
                        locale
                        + "\t" + (locale.equals(actualLocale) ? "=" : actualLocale)
                        + "\t" + path
                        + "\t" + (path.equals(actualPath) ? "=" : actualPath));
                }
                System.out.println(locale + "\t" + sorted.size());
                sorted.forEach(x -> out.println(x));
            }
        }
    }

    /** Test that <alias> elements only are in root.html
     * For speed in testing, just checks for the presence of "<alias" to avoid doing XML parse.
     */
    public void TestOnlyRootHasAliases() {
        for (String dirString : DtdType.ldml.directories) {
            checkForAliases(CLDRPaths.COMMON_DIRECTORY + dirString);
        }
    }

    /** Test that <alias> elements only are in root.html
     * For speed in testing, just checks for the presence of "<alias" to avoid doing XML parse.
     */
    private void checkForAliases(String dirString) {
        File file = new File(dirString);
        if (file.isDirectory()) {
            for (String subfile : file.list()) {
                checkForAliases(dirString + "/" + subfile);
            }
        } else {
            String name = file.getName();
            if (!name.equals("root.xml") && name.endsWith(".xml")) {
                for (String line : FileUtilities.in(FileUtilities.openFile(file))) {
                    if (line.contains("<alias")) {
                        errln(file + " contains <alias…");
                    }
                }
            }
        }
    }


    /**
     * Verify that when a path's value = its bailey value,
     * then replacing it by inheritance marker should have no effect on the resolved value.
     * If it fails, then there is something wrong with either the inheritance or the bailey value.
     */
    public void TestInheritanceReplacement() {
        Factory std = CLDRConfig.getInstance().getCldrFactory();

        String section = "Alias";
        final String inheritingPath = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"stand-alone\"]/monthWidth[@type=\"wide\"]/month[@type=\"1\"]";
        final String formatPath = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"1\"]";
        new TestValueSet("root", "en", "en_001")
        .add("en", inheritingPath, "X")
        .add("en", formatPath, "X")
        .add("en_001", inheritingPath, "X")
        .add("en_001", formatPath, "Y")
        .checkReplacements(section, std);

        section = "Lateral-alt";
        final String altPath = "//ldml/localeDisplayNames/languages/language[@type=\"ug\"][@alt=\"variant\"]";
        final String noAlt = "//ldml/localeDisplayNames/languages/language[@type=\"ug\"]";
        new TestValueSet("root", "en", "en_001")
        .add("en", altPath, "Uighur")
        .add("en", noAlt, "Uighur")
        .add("en_001", altPath, "Uighur")
        .add("en_001", noAlt, "UyghurX")
        .checkReplacements(section, std);

        section = "Lateral-count";
        final String onepath = "//ldml/units/unitLength[@type=\"short\"]/unit[@type=\"duration-hour\"]/unitPattern[@count=\"one\"]";
        final String otherpath = "//ldml/units/unitLength[@type=\"short\"]/unit[@type=\"duration-hour\"]/unitPattern[@count=\"other\"]";
        new TestValueSet("root", "en", "en_001")
        .add("en", onepath, "{0} hrx")
        .add("en", otherpath, "{0} hrx")
        .add("en_001", onepath, "{0} hrx")
        .add("en_001", otherpath, "{0} hrxs")
        .checkReplacements(section, std);

        section = "Grammar";
        final String genPath = "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"duration-day-person\"]/unitPattern[@count=\"one\"][@case=\"genitive\"]";
        final String nomPath = "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"duration-day-person\"]/unitPattern[@count=\"one\"]";
        new TestValueSet("root", "de", "de_AT")
        .add("de", genPath, "{0} Tagx")
        .add("de", nomPath, "{0} Tagx")
        .add("de_AT", genPath, "{0} Tagx")
        .add("de_AT", nomPath, "{0} Tagxx")
        .checkReplacements(section, std);

        section = "Constructed";
        final String basePath = "//ldml/localeDisplayNames/languages/language[@type=\"nl\"]";
        final String regPath = "//ldml/localeDisplayNames/languages/language[@type=\"nl_BE\"]";
        new TestValueSet("root", "fr", "fr_CA")
        .add("fr", basePath, "dutch")
        .add("fr", regPath, "flamandx")
        .add("fr_CA", basePath, "{0} dutch")
        .add("fr_CA", regPath, "{0} flamandxs")
        .checkReplacements(section, std);
    }

    static final class LocalePathValue {
        String locale;
        String path;
        String value;
        public LocalePathValue(String locale, String path, String value) {
            this.locale = locale;
            this.path = path;
            this.value = value;
        }
        @Override
        public String toString() {
            return locale + "\t" + path + "\t" + value;
        }
    }

    class TestValueSet {
        Set<LocalePathValue> testValues = new LinkedHashSet<>();
        ImmutableList<String> locales;
        Set<String> paths = new LinkedHashSet<>();

        public TestValueSet(String... locales) {
            this.locales = ImmutableList.copyOf(locales);
        }

        public TestValueSet add(String locale, String path, String value) {
            if (!locales.contains(locale)) {
                throw new IllegalArgumentException(locale + " must be in " + locales);
            }
            testValues.add(new LocalePathValue(locale, path, value));
            paths.add(path);
            return this;
        }


        public TestValueSet checkReplacements(String section, Factory std) {
            TestFactory testFactory = copyIntoTestFactory(std);
            setValuesIn(testFactory);
            show("\n\t\t" + section + ", BEFORE replacing", testFactory);
            TestFactory modifiedFactory = replaceIfInheritedEqual(testFactory, null);
            check(section + ", AFTER replacing with null", modifiedFactory);

            TestFactory testFactory2 = copyIntoTestFactory(std);
            setValuesIn(testFactory2);
            //check("\nBefore replacing with ↑↑↑", testFactory2);
            modifiedFactory = replaceIfInheritedEqual(testFactory, CldrUtility.INHERITANCE_MARKER);
            check(section + ", AFTER replacing with " + CldrUtility.INHERITANCE_MARKER, modifiedFactory);
            return this;
        }

        public TestFactory copyIntoTestFactory(Factory std) {
            TestFactory testFactory = new TestFactory();
            testFactory.setSupplementalDirectory(std.getSupplementalDirectory());

            for (String locale : locales) {
                CLDRFile cldrFile = std.make(locale, false);
                testFactory.addFile(cldrFile.cloneAsThawed());
            }
            return testFactory;
        }

        public TestFactory replaceIfInheritedEqual(TestFactory testFactory, String replacement) {
            Output<String> pathWhereFound = new Output<>();
            Output<String> localeWhereFound = new Output<>();
            HashMultimap<String, String>actions = HashMultimap.create();

            // Gather all the values that equal their baileys

            for (LocalePathValue localePathValue : testValues) {
                CLDRFile cldrFile = testFactory.make(localePathValue.locale, true);
                String value = cldrFile.getStringValueWithBailey(localePathValue.path);
                String baileyValue = cldrFile.getBaileyValue(localePathValue.path, pathWhereFound, localeWhereFound);
                if (Objects.equals(value, baileyValue)) {
                    actions.put(localePathValue.locale, localePathValue.path);
                }
            }

            // Now replace them all
            // NOTE: Removing or adding new values might invalidate resolved files, so we create a new factory

            TestFactory testFactory1 = new TestFactory();
            testFactory1.setSupplementalDirectory(testFactory.getSupplementalDirectory());
            for (String locale : locales) {
                final CLDRFile cldrFile = testFactory.make(locale, false).cloneAsThawed();
                Set<String> items = actions.get(locale);
                if (items != null) {
                    for (String path : items) {
                        if (replacement == null) {
                            cldrFile.remove(path);
                        } else {
                            cldrFile.add(path, replacement);
                        }
                    }
                }
                testFactory1.addFile(cldrFile);
            }
            return testFactory1;

        }

        public void show(String title, TestFactory testFactory) {
            logln(title);
            CLDRFile cldrFileRoot = testFactory.make("root", true);
            for (String path : paths) {
                String rawValue = cldrFileRoot.getUnresolved().getStringValue(path);
                logln("root" + "\t" + path + "\t" + rawValue);
            }
            for (LocalePathValue entry : testValues) {
                CLDRFile cldrFile = testFactory.make(entry.locale, true);
                String rawValue = cldrFile.getUnresolved().getStringValue(entry.path);
                logln(entry.locale + "\t" + entry.path + "\t" + rawValue);
            }
        }

        public void check(String title, TestFactory testFactory) {
            logln(title);
            for (LocalePathValue entry : testValues) {
                CLDRFile cldrFile = testFactory.make(entry.locale, true);
                String rawValue = cldrFile.getUnresolved().getStringValue(entry.path);
                String resolvedValue = cldrFile.getStringValueWithBailey(entry.path);
                assertEquals(entry.toString() + "\t" + rawValue, entry.value, resolvedValue);
            }
        }

        public TestValueSet setValuesIn(TestFactory testFactory) {
            for (LocalePathValue entry : testValues) {
                CLDRFile cldrFile = testFactory.make(entry.locale, true);
                cldrFile.getUnresolved().add(entry.path, entry.value);
            }
            return this;
        }
    }

    public void testLateralInheritance() {
//        {
//            Multimap<String, SuspiciousData> suspicious = getSuspicious(CLDRConfig.getInstance().getCLDRFile("fr_CA", true));
//            if (!suspicious.isEmpty()) {
//                errln("fr_CA" + "\n\t" + Joiner.on("\n\t").join(suspicious.entries()));
//            }
//        }

        Map<String, Map<String, Counter<String>>> allCount = new LinkedHashMap<>();
        Map<String, Breakdown> breakdownMap = new LinkedHashMap<>();

        for (String ldmlDirectory : DtdType.ldml.directories) {
            Map<String, Counter<String>> localeSuspiciousCount = new LinkedHashMap<>();

            final Factory factory = SimpleFactory.make(CLDRPaths.COMMON_DIRECTORY + ldmlDirectory, ".*");
            for (String locale : factory.getAvailable()) {
                if (!"root".equals(LocaleIDParser.getParent(locale))) { // just L1 locales
                    continue;
                }

                CLDRFile cldrFile = factory.make(locale, true);
                Multimap<String, SuspiciousData> suspicious = getSuspicious(cldrFile, breakdownMap);
                if (!suspicious.isEmpty()) {
                    errln("\t" + locale + "\t" + suspicious.entries().size()
                        + "\n\t" + Joiner.on("\n\t").join(suspicious.entries()));
                    Counter<String> c = new Counter<>();
                    for (Entry<String, Collection<SuspiciousData>> entry : suspicious.asMap().entrySet()) {
                        localeSuspiciousCount.put(locale, c.add(entry.getKey(), entry.getValue().size()));
                    }
                }
            }
            if (!localeSuspiciousCount.isEmpty()) {
                allCount.put(ldmlDirectory, localeSuspiciousCount);
            }
        }
        for (Entry<String, Map<String, Counter<String>>> entry : allCount.entrySet()) {
            String dir = entry.getKey();
            for (Entry<String, Counter<String>> entry2 : entry.getValue().entrySet()) {
                String locale = entry2.getKey();
                Counter<String> c = entry2.getValue();
                for (R2<Long, String> entry3 : c.getEntrySetSortedByCount(false, null)) {
                    System.out.println(dir + "\t" + locale + "\t" + entry3.get0() + "\t" + entry3.get1());
                }
            }
        }
        CLDRFile english = config.getEnglish();
        System.out.println("\nlocale\tName\tTarget (Org=cldr)\t" + Breakdown.header());
        for (Entry<String, Breakdown> entry : breakdownMap.entrySet()) {
            final String locale = entry.getKey();
            Level target = StandardCodes.make().getLocaleCoverageLevel(Organization.cldr, locale);
            String targetString = target.toString();
            switch(target) {
            case COMPREHENSIVE: targetString = Level.MODERN.toString(); break; // TODO change once English coverage is fixed.
            case UNDETERMINED: targetString = "*"+Level.BASIC.toString(); break;
            }
            // target == Level.UNDETERMINED ? "-" :
            System.out.println(locale
                + "\t" + english.getName(locale)
                + "\t" + targetString
                + "\t" + entry.getValue());
        }

    }

    private static final Joiner TAB_JOINER = Joiner.on('\t');

    static final class Breakdown {
        int notTargetNull;
        int notTargetNotNull;
        int targetNull;
        int notInheritanceMarker;
        int iMarkerSamePath;
        int iMarkerDiffPathRoot;
        int iMarkerDiffPathNotRoot;
        @Override
        public String toString() {
            return TAB_JOINER.join(notTargetNull, notTargetNotNull, targetNull, notInheritanceMarker, iMarkerSamePath, iMarkerDiffPathRoot, iMarkerDiffPathNotRoot);
        }
        public static String header() {
            return TAB_JOINER.join(">Target&Null", ">Target&!Null", "≤Target&Null", "!InheritanceMarker", "iMarker&=Path", "iMarker&≠Path&Root", "iMarker&≠Path&!Root");
        }
    }

    static final class SuspiciousData implements Comparable<SuspiciousData> {
        final String pathRequested;
        final String pathFound;
        final String valueFound;

        public SuspiciousData(String pathRequested, String pathFound, String valueFound) {
            this.pathRequested = pathRequested;
            this.pathFound = pathFound;
            this.valueFound = valueFound;
        }
        @Override
        public int compareTo(SuspiciousData o) {
            return ComparisonChain.start()
                .compare(pathRequested, o.pathRequested)
                .compare(pathFound, o.pathFound)
                .compare(valueFound, o.valueFound)
                .result();
        }
        @Override
        public String toString() {
            return "\t" + valueFound + "\n\t\t" + pathRequested + "\n\t\t" + pathFound;
        }
    }

    public Multimap<String,SuspiciousData> getSuspicious(CLDRFile cldrFile, Map<String, Breakdown> breakdownMap) {
        Multimap<String,SuspiciousData> suspicious = TreeMultimap.create();
        if (!cldrFile.isResolved()) {
            throw new IllegalArgumentException();
        }
        CLDRFile unresolvedCldrFile = cldrFile.getUnresolved();
        String locale = cldrFile.getLocaleID();
        Level target = StandardCodes.make().getLocaleCoverageLevel(Organization.cldr, locale);
        switch(target) {
        case COMPREHENSIVE: target = Level.MODERN; break; // TODO change once English coverage is fixed.
        case UNDETERMINED: target = Level.BASIC; break;
        }

        Output<String> foundPath = new Output<>();
        Output<String> foundLocale = new Output<>();
        Breakdown breakdown = breakdownMap.get(locale);
        if (breakdown == null) {
            breakdownMap.put(locale, breakdown = new Breakdown());
        }
        for (String path : cldrFile) { // TODO we only need to look at the actual items, not nulls. But right now we are catching them.
            String unresolvedValue = unresolvedCldrFile.getStringValue(path);

            Level level = SupplementalDataInfo.getInstance().getCoverageLevel(path, locale);
            if (target.compareTo(level) < 0) {
                if (unresolvedValue == null) {
                    breakdown.notTargetNull++;
                } else {
                    breakdown.notTargetNotNull++;
                }
                continue; // only worry about modern level paths
            }
            if (unresolvedValue == null) {
                breakdown.targetNull++;
                continue;
            }

            if (!CldrUtility.INHERITANCE_MARKER.equals(unresolvedValue)) {
                breakdown.notInheritanceMarker++;
                continue;
            }
            cldrFile.getBaileyValue(path, foundPath, foundLocale);
            if (path.equals(foundPath.value)) {
                breakdown.iMarkerSamePath++;
                // if we find it in the same path (vertical) we are ok
                continue;
            }
            // at this point, it is horizontal (path) inheritance
            // if there is nothing between us and a root value, then

            if ("root".equals(foundLocale.value) || "code-fallback".equals(foundLocale.value)) {
                breakdown.iMarkerDiffPathRoot++;
                // if we fall all the way back to root that's ok.
                continue;
            }
            // otherwise
            breakdown.iMarkerDiffPathNotRoot++;
            suspicious.put(foundLocale.value, new SuspiciousData(path, foundPath.value, cldrFile.getStringValueWithBailey(path)));
        }
        return suspicious;
    }
}

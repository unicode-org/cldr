package org.unicode.cldr.unittest;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedHashSet;
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
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.XMLSource;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.ibm.icu.dev.test.TestFmwk;
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

        logln("Alias");
        final String formatPath = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"format\"]/monthWidth[@type=\"wide\"]/month[@type=\"1\"]";
        final String inheritingPath = "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/months/monthContext[@type=\"stand-alone\"]/monthWidth[@type=\"wide\"]/month[@type=\"1\"]";
        new TestValueSet("root", "en", "en_001")
        .add("en", inheritingPath, "X")
        .add("en", formatPath, "X")
        .add("en_001", inheritingPath, "Y")
        .add("en_001", formatPath, "X")
        .checkReplacements(std);

        logln("Lateral-alt");
        final String altPath = "//ldml/localeDisplayNames/languages/language[@type=\"ug\"][@alt=\"variant\"]";
        final String noAlt = "//ldml/localeDisplayNames/languages/language[@type=\"ug\"]";
        new TestValueSet("root", "en", "en_001")
        .add("en", altPath, "Uighur")
        .add("en", noAlt, "Uyghur")
        .add("en_001", altPath, "Uighur")
        .add("en_001", noAlt, "UyghurX")
        .checkReplacements(std);

        logln("Constructed");
        final String basePath = "//ldml/localeDisplayNames/languages/language[@type=\"nl\"]";
        final String regPath = "//ldml/localeDisplayNames/languages/language[@type=\"nl_BE\"]";
        new TestValueSet("root", "fr", "fr_CA")
        .add("fr", basePath, "dutch")
        .add("fr", regPath, "flamandx")
        .add("fr_CA", basePath, "{0} dutch")
        .add("fr_CA", regPath, "{0} flamandxs")
        .checkReplacements(std);

        logln("Lateral-count");
        final String onepath = "//ldml/units/unitLength[@type=\"short\"]/unit[@type=\"duration-hour\"]/unitPattern[@count=\"one\"]";
        final String otherpath = "//ldml/units/unitLength[@type=\"short\"]/unit[@type=\"duration-hour\"]/unitPattern[@count=\"other\"]";
        new TestValueSet("root", "en", "en_001")
        .add("en", onepath, "{0} hrx")
        .add("en", otherpath, "{0} hrx")
        .add("en_001", onepath, "{0} hrx")
        .add("en_001", otherpath, "{0} hrxs")
        .checkReplacements(std);

        logln("Grammar");
        final String genPath = "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"duration-day-person\"]/unitPattern[@count=\"one\"][@case=\"genitive\"]";
        final String nomPath = "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"duration-day-person\"]/unitPattern[@count=\"one\"]";
        new TestValueSet("root", "de", "de_AT")
        .add("de", genPath, "{0} Tagx")
        .add("de", nomPath, "{0} Tagx")
        .add("de_AT", genPath, "{0} Tagx")
        .add("de_AT", nomPath, "{0} Tagxx")
        .checkReplacements(std);
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


        public TestValueSet checkReplacements(Factory std) {
            TestFactory testFactory = copyIntoTestFactory(std);
            setValuesIn(testFactory);
            check("Before replacing", testFactory);
            TestFactory modifiedFactory = replaceIfInheritedEqual(testFactory, null);
            check("After replacing with null", modifiedFactory);

            TestFactory testFactory2 = copyIntoTestFactory(std);
            setValuesIn(testFactory2);
            //check("\nBefore replacing with ↑↑↑", testFactory2);
            modifiedFactory = replaceIfInheritedEqual(testFactory, CldrUtility.INHERITANCE_MARKER);
            check("After replacing with " + CldrUtility.INHERITANCE_MARKER, modifiedFactory);
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

        public void check(String title, TestFactory testFactory) {
            logln(title);
            CLDRFile cldrFileRoot = testFactory.make("root", true);
            for (String path : paths) {
                String value = cldrFileRoot.getUnresolved().getStringValue(path);
                assertEquals("root" + "/t" + path, value, value);
            }

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
}

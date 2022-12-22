package org.unicode.cldr.unittest;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.XMLSource;

import com.google.common.collect.HashMultimap;
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


    final String onepath = "//ldml/units/unitLength[@type=\"short\"]/unit[@type=\"duration-hour\"]/unitPattern[@count=\"one\"]";
    final String otherpath = "//ldml/units/unitLength[@type=\"short\"]/unit[@type=\"duration-hour\"]/unitPattern[@count=\"other\"]";
    final List<String> paths = Arrays.asList(onepath, otherpath);

    /**
     * Verify that when a path's value = its bailey value,
     * then replacing it by inheritance marker should have no effect on the resolved value.
     * If it fails, then there is something wrong with either the inheritance or the bailey value.
     */
    public void TestInheritanceReplacement() {
        Factory std = CLDRConfig.getInstance().getCldrFactory();
        CLDRFile root = std.make("root", false);
        CLDRFile en = std.make("en", false);
        CLDRFile en001 = std.make("en_001", false);


        TestFactory testFactory = new TestFactory();
        CLDRFile enUnresolved = en.cloneAsThawed();
        CLDRFile en001Unresolved = en001.cloneAsThawed();

        testFactory.addFile(root);
        testFactory.addFile(enUnresolved);
        testFactory.addFile(en001Unresolved);

        // TODO make this more data driven so we can try different sets of path/value pairs without duplicating too much
        // That is, the values here are the values that we should get after replacement in checkValues

        enUnresolved.add(onepath, "{0} hr");
        enUnresolved.add(otherpath, "{0} hr");

        en001Unresolved.add(onepath, "{0} hr");
        en001Unresolved.add(otherpath, "{0} hrs");


        CLDRFile enResolved = testFactory.make("en", true);
        CLDRFile en001Resolved = testFactory.make("en_001", true);
        List<CLDRFile> cldrSourceFiles = Arrays.asList(enResolved, en001Resolved);
        showValues("\nBefore replacing with ↑↑↑", cldrSourceFiles);

        Output<String> pathWhereFound = new Output<>();
        Output<String> localeWhereFound = new Output<>();


        checkValues(onepath, otherpath, enResolved, en001Resolved);
        HashMultimap<CLDRFile, String>actions = HashMultimap.create();

        // Gather all the values that equal their baileys

        for (CLDRFile cldrFile : cldrSourceFiles) {
            for (String path : paths) {
                String value = cldrFile.getStringValue(path);
                String baileyValue = cldrFile.getBaileyValue(path, pathWhereFound, localeWhereFound);
                if (Objects.equals(value, baileyValue)) {
                    actions.put(cldrFile, path);
                }
            }
        }

        // Now replace them all

        for (Entry<CLDRFile, String> action : actions.entries()) {
            final CLDRFile cldrFile = action.getKey();
            final String path = action.getValue();
            cldrFile.getUnresolved().add(path, CldrUtility.INHERITANCE_MARKER);
        }

        showValues("\nAfter replacing with ↑↑↑", cldrSourceFiles);

        // the values should all be the same as the last time we checked them

        checkValues(onepath, otherpath, enResolved, en001Resolved);
    }

    private void showValues(String title, Collection<CLDRFile> resolvedCLDRFiles) {
        logln(title);
        Output<String> pathWhereFound = new Output<>();
        Output<String> localeWhereFound = new Output<>();
        for (CLDRFile cldrFile : resolvedCLDRFiles) {
            if (!cldrFile.isResolved()) {
                throw new IllegalArgumentException("File must be resolved");
            }
            for (String path : paths) {
                logln(cldrFile.getLocaleID() + path
                    + "\tUnresolved=" + cldrFile.getUnresolved().getStringValue(path)
                    + "\tResolvedWB=" + cldrFile.getStringValueWithBailey(path)
                    + "\tBailey=" + cldrFile.getBaileyValue(otherpath, pathWhereFound, localeWhereFound)
                    );
            }
        }
    }

    public void checkValues(final String onepath, final String otherpath, CLDRFile enResolved,  CLDRFile en001Resolved) {
        if (!enResolved.isResolved() || !en001Resolved.isResolved()) {
            throw new IllegalArgumentException("File must be resolved");
        }
        Output<String> pathWhereFound = new Output<>();
        Output<String> localeWhereFound = new Output<>();

        assertEquals("en, one", "{0} hr", enResolved.getStringValueWithBailey(onepath));
        assertEquals("en, other", "{0} hr", enResolved.getStringValueWithBailey(otherpath));
        assertEquals("en, one, bailey", "{0} h", enResolved.getBaileyValue(otherpath, pathWhereFound, localeWhereFound));
        assertEquals("en, other, bailey", "{0} h", enResolved.getBaileyValue(otherpath, pathWhereFound, localeWhereFound));

        assertEquals("en_001, one", "{0} hr", en001Resolved.getStringValueWithBailey(onepath));
        assertEquals("en_001, other", "{0} hrs", en001Resolved.getStringValueWithBailey(otherpath));
        assertEquals("en_001, one, bailey", "{0} hr", en001Resolved.getBaileyValue(otherpath, pathWhereFound, localeWhereFound));
        assertEquals("en_001, other, bailey", "{0} hr", en001Resolved.getBaileyValue(otherpath, pathWhereFound, localeWhereFound));
    }
}

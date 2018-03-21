package org.unicode.cldr.unittest;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRFile.Status;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.XMLSource;

import com.ibm.icu.dev.test.TestFmwk;

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
}

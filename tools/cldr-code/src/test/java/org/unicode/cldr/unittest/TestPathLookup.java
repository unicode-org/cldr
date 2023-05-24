package org.unicode.cldr.unittest;

import com.google.common.base.Joiner;
import java.util.Arrays;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.DowngradePaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.LocalePathValueListMatcher;

public class TestPathLookup extends TestFmwkPlus {
    private static final boolean DEBUG = false;

    public static void main(String[] args) {
        new TestPathLookup().run(args);
    }

    final CLDRConfig config = CLDRConfig.getInstance();
    final Factory factory = config.getCldrFactory();

    public void testLocalePathValueListMatcher() {
        LocalePathValueListMatcher matcher =
                LocalePathValueListMatcher.load(
                        Arrays.asList(
                                " ", // purposeful blank line
                                "# de ; //ldml/units/unitLength[@type=\"long\"]/junk ; k.*",
                                " ; //ldml/units/unitLength[@type=\"long\"]/junk ",
                                "f.* ; //ldml/units/unitLength[@type=\"long\"]/foobar ; k.*")
                                .stream());

        String[][] tests = {
            {"fr", "//ldml/units/unitLength[@type=\"long\"]/foobar", "kg", "true"},
            {"de", "//ldml/units/unitLength[@type=\"long\"]/foobar", "kg", "false"},
            {"fr", "//ldml/units/unitLength[@type=\"long\"]/foobar", "meter", "false"},
            {"fr", "//ldml/units/unitLength[@type=\"long\"]/fii", "kg", "false"},
            {"de", "//ldml/units/unitLength[@type=\"long\"]/junk", "kg", "true"},
        };
        for (String[] test : tests) {
            String locale = test[0];
            String path = test[1];
            String value = test[2];
            boolean expected = Boolean.valueOf(test[3]);
            boolean actual = matcher.lookingAt(locale, path, value);
            assertEquals(Joiner.on(" , ").join(test), expected, actual);
        }
    }

    public void testLocalePathValueListMatcherSyntax() {

        String[][] tests = {
            {"", "ok"},
            {"#", "ok"},
            {"fr( ; ", "Unclosed group near index 3\nfr("},
            {"fr", "Match lines must have at least locale ; path: «fr»"},
            {
                "fr; path ; value ; too-long",
                "Match lines must have a maximum of 3 fields (locale; path; value): «fr; path ; value ; too-long»"
            },
        };
        for (String[] test : tests) {
            String line = test[0];
            String expected = test[1];
            String actual = "ok";
            try {
                LocalePathValueListMatcher matcher =
                        LocalePathValueListMatcher.load(Arrays.asList(line).stream());
            } catch (Exception e) {
                actual = e.getMessage();
            }
            assertEquals(line, expected, actual);
        }
        // check for bad syntax

    }

    /**
     * These tests are for use when debugging Downgrade. They depend on the current value of the
     * downgrade.txt file, so are not included as routine tests.
     */
    public void testDowngrade() {
        if (!DEBUG) {
            return;
        }
        assertTrue(
                "Downgrade according to current data file",
                DowngradePaths.lookingAt(
                        "en",
                        "//ldml/units/unitLength[@type=\"long\"]/unit[@type=\"graphics-dot\"]/displayName",
                        null));

        int count = countMatches(factory.make("cs", false));
        assertEquals("Count of matches in cs.xml", 15, count);
    }

    public int countMatches(CLDRFile cldrFile) {
        String locale = cldrFile.getLocaleID();
        int count = 0;
        for (String path : cldrFile) {
            String value = cldrFile.getStringValue(path);
            if (DowngradePaths.lookingAt("en", path, value)) {
                logln(String.format("%s\t%s\t%s", locale, path, value));
                ++count;
            }
        }
        return count;
    }
}

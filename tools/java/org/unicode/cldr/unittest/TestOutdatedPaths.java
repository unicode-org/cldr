package org.unicode.cldr.unittest;

import java.util.Set;
import java.util.TreeSet;

import org.unicode.cldr.test.OutdatedPaths;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CldrUtility;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.PathHeader;

import com.ibm.icu.dev.test.TestFmwk;

public class TestOutdatedPaths extends TestFmwk {
    public static void main(String[] args) {
        new TestOutdatedPaths().run(args);
    }

    OutdatedPaths paths = new OutdatedPaths();

    public void TestBasic() {
        assertEquals("English should have none", 0, paths.countOutdated("en"));
        assertEquals("French should have at least one", 518, paths.countOutdated("fr")); // update this when
                                                                                         // GenerateBirth is rerun.
        assertTrue(
            "",
            paths
                .isOutdated(
                    "fr",
                    "//ldml/dates/calendars/calendar[@type=\"gregorian\"]/dateTimeFormats/dateTimeFormatLength[@type=\"full\"]/dateTimeFormat[@type=\"standard\"]/pattern[@type=\"standard\"]"));
    }

    public void TestShow() {
        Factory factory = Factory.make(CldrUtility.MAIN_DIRECTORY, ".*");
        CLDRFile fr = factory.make("fr", false);
        PathHeader.Factory pathHeaders = PathHeader.getFactory(factory.make("en", false));
        Set<PathHeader> sorted = new TreeSet();
        for (String s : fr) {
            if (paths.isOutdated("fr", s)) {
                sorted.add(pathHeaders.fromPath(s));
            }
        }
        for (PathHeader p : sorted) {
            System.out.println(p);
        }
    }

}

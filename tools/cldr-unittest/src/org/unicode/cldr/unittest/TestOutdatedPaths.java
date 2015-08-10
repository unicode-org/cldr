package org.unicode.cldr.unittest;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.unicode.cldr.test.OutdatedPaths;
import org.unicode.cldr.unittest.TestAll.TestInfo;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Factory;
import org.unicode.cldr.util.PathHeader;

public class TestOutdatedPaths extends TestFmwkPlus {

    static TestInfo testInfo = TestInfo.getInstance();

    public static void main(String[] args) {
        new TestOutdatedPaths().run(args);
    }

    OutdatedPaths outdatedPaths = new OutdatedPaths();

    public void TestBasic() {
        assertEquals("English should have none", 0,
            outdatedPaths.countOutdated("en"));

        // Update the number when GenerateBirth is rerun.

        assertTrue("French should have at least one",
            outdatedPaths.countOutdated("fr") > 10);

        // If this path is not outdated, find another one

        // assertTrue(
        // "Test one path known to be outdated. Use TestShow -v to find a path, and verify that it is outdated",
        // outdatedPaths
        // .isOutdated(
        // "fr",
        // "//ldml/dates/fields/field[@type=\"week\"]/relative[@type=\"-1\"]"));
    }

    // use for debugging
    public void TestShow() {
        if (isVerbose()) {
            String locale = "fr";
            CLDRFile fr = testInfo.getCLDRFile(locale, false);
            PathHeader.Factory pathHeaders = PathHeader.getFactory(testInfo.getEnglish());
            
            Map<PathHeader, String> sorted = new TreeMap<PathHeader, String>();
            logln("Count:\t" + outdatedPaths.countOutdated(locale));
            for (String spath : fr) {
                if (outdatedPaths.isOutdated(locale, spath)) {
                    sorted.put(pathHeaders.fromPath(spath), "");
                }
            }
            for (Entry<PathHeader, String> entry : sorted.entrySet()) {
                PathHeader p = entry.getKey();
                String originalPath = p.getOriginalPath();
                logln("Eng: "
                    + outdatedPaths.getPreviousEnglish(originalPath)
                    + "\t=>\t"
                    + TestAll.TestInfo.getInstance().getEnglish()
                        .getStringValue(originalPath) + "\tNative: "
                    + fr.getStringValue(originalPath) + "\tPath: "
                    + p.toString() + "\tXML Path: " + originalPath);
            }
        }
    }

}

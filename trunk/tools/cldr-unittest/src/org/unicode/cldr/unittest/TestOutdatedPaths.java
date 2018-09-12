package org.unicode.cldr.unittest;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.unicode.cldr.test.OutdatedPaths;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.PathHeader;

public class TestOutdatedPaths extends TestFmwkPlus {

    static CLDRConfig testInfo = CLDRConfig.getInstance();

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
            PathHeader.Factory pathHeaders = PathHeader.getFactory(testInfo.getEnglish());
            checkShow(pathHeaders, "fr");
            checkShow(pathHeaders, "de");
        }
    }

    private void checkShow(PathHeader.Factory pathHeaders, String locale) {
        CLDRFile cldrFile = testInfo.getMainAndAnnotationsFactory().make(locale, true);

        Map<PathHeader, String> sorted = new TreeMap<PathHeader, String>();
        logln("Count:\t" + outdatedPaths.countOutdated(locale));
        for (String spath : cldrFile) {
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
                + CLDRConfig.getInstance().getEnglish()
                    .getStringValue(originalPath)
                + "\tNative(" + locale
                + "): "
                + cldrFile.getStringValue(originalPath) + "\tPath: "
                + p.toString() + "\tXML Path: " + originalPath);
        }
    }

}

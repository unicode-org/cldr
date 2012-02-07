package org.unicode.cldr.unittest;

import org.unicode.cldr.test.OutdatedPaths;

import com.ibm.icu.dev.test.TestFmwk;

public class TestOutdatedPaths extends TestFmwk {
    public static void main(String[] args) {
        new TestOutdatedPaths().run(args);
    }

    public void TestBasic() {
        OutdatedPaths paths = new OutdatedPaths();
        assertEquals("English should have none", 0, paths.countOutdated("en"));
        assertEquals("French should have at least one", 455, paths.countOutdated("fr")); // update this when GenerateBirth is rerun.
    }
}

package org.unicode.cldr.unittest;

import java.util.stream.IntStream;
import org.unicode.cldr.icu.dev.test.TestFmwk;
import org.unicode.cldr.util.PathStarrer;

public class TestPathStarrer extends TestFmwk {

    public static void main(String[] args) {
        new TestPathStarrer().run(args);
    }

    /**
     * Stop reporting failures after the first failure for a given test (testNonParallel or
     * testParallel). In the case of parallel testing, several failures may still be reported before
     * this takes effect.
     */
    private boolean alreadyReportedFailure = false;

    public void testNonParallel() {
        // For non-parallel testing, the size of TEST_COUNT should make little difference; don't
        // waste a lot of time.
        final int TEST_COUNT = 1000;
        final PathStarrer pathStarrer = new PathStarrer().setSubstitutionPattern("*");
        alreadyReportedFailure = false; // reset
        for (int pass = 0; pass < 2; pass++) {
            IntStream.range(0, TEST_COUNT).forEach(i -> doOnePath(pathStarrer, i));
        }
    }

    // TODO: enable testParallel. Currently it fails since PathStarrer is not thread-safe.
    // Reference: https://unicode-org.atlassian.net/browse/CLDR-18697

    /*
    public void testParallel() {
        // For parallel testing, the size of TEST_COUNT makes a big difference to whether the test
        // passes or fails. In part, we are testing whether PathStarrer is thread-safe. If it
        // isn't, much randomness is involved. A large number is required here to make failure
        // likely if PathStarrer is not thread-safe.
        final int TEST_COUNT = 1000000;
        final PathStarrer pathStarrer = new PathStarrer().setSubstitutionPattern("*");
        alreadyReportedFailure = false; // reset
        IntStream.range(0, TEST_COUNT).parallel().forEach(i -> doOnePath(pathStarrer, i));
    }
     */

    private void doOnePath(PathStarrer pathStarrer, int i) {
        if (alreadyReportedFailure) {
            return;
        }
        String path = "//" + i;
        String starred = pathStarrer.set(path);
        assertEquals("Path should match", path, starred);
        if (!path.equals(starred)) {
            alreadyReportedFailure = true;
        }
    }
}

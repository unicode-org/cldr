package org.unicode.cldr.unittest;

import java.util.stream.IntStream;
import org.unicode.cldr.icu.dev.test.TestFmwk;
import org.unicode.cldr.util.ThreadSafeMapOfMapOfMap;

public class TestThreadSafeMapOfMapOfMap extends TestFmwk {

    public static void main(String[] args) {
        new TestThreadSafeMapOfMapOfMap().run(args);
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
        final ThreadSafeMapOfMapOfMap<String, String, String, String> map =
                new ThreadSafeMapOfMapOfMap<>();
        alreadyReportedFailure = false; // reset
        for (int pass = 0; pass < 2; pass++) {
            boolean firstPass = (pass == 0);
            IntStream.range(0, TEST_COUNT).forEach(i -> doOnePath(map, firstPass, i));
        }
    }

    public void testParallel() {
        // For parallel testing, the size of TEST_COUNT makes a big difference to whether the test
        // passes or fails. In part, we are testing whether ExampleCache is thread-safe. If it
        // isn't, much randomness is involved. A large number is required here to make failure
        // likely if ExampleCache is not thread-safe.
        final int TEST_COUNT = 1000000;
        final ThreadSafeMapOfMapOfMap<String, String, String, String> map =
                new ThreadSafeMapOfMapOfMap<>();
        alreadyReportedFailure = false; // reset
        for (int pass = 0; pass < 2; pass++) {
            boolean firstPass = (pass == 0);
            IntStream.range(0, TEST_COUNT).parallel().forEach(i -> doOnePath(map, firstPass, i));
        }
    }

    private void doOnePath(
            ThreadSafeMapOfMapOfMap<String, String, String, String> map, boolean firstPass, int i) {
        if (alreadyReportedFailure) {
            return;
        }
        String k1 = "k1-" + i;
        String k2 = "k2-" + i;
        String k3 = "k3-" + i;
        String example = getExampleImmediately(k1);
        String result;
        if (firstPass) {
            result = map.computeIfAbsent(k1, k2, k3, (kk1, kk2, kk3) -> getExampleImmediately(kk1));
            assertEquals("Example should match immediately", example, result);
            if (!example.equals(result)) {
                alreadyReportedFailure = true;
            }
            result = map.computeIfAbsent(k1, k2, k3, (kk1, kk2, kk3) -> getExampleSoon(kk1));
            assertEquals("Example should match soon", example, result);
        } else {
            result = map.computeIfAbsent(k1, k2, k3, (kk1, kk2, kk3) -> getExampleLater(kk1));
            assertEquals("Example should match later", example, result);
        }
        if (!example.equals(result)) {
            alreadyReportedFailure = true;
        }
    }

    private static String getExampleImmediately(String k) {
        return "e" + k;
    }

    private String getExampleSoon(String k) {
        return "soon" + k;
    }

    private String getExampleLater(String k) {
        return "later" + k;
    }
}

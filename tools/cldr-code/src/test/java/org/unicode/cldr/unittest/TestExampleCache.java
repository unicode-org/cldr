package org.unicode.cldr.unittest;

import java.util.stream.IntStream;
import org.unicode.cldr.icu.dev.test.TestFmwk;
import org.unicode.cldr.test.ExampleCache;

public class TestExampleCache extends TestFmwk {

    public static void main(String[] args) {
        new TestExampleCache().run(args);
    }

    /**
     * Stop reporting failures after the first failure for a given test (testNonParallel or
     * testParallel). In the case of parallel testing, several failures may still be reported before
     * this takes effect.
     */
    private boolean alreadyReportedFailure = false;

    public void testNonParallel() {
        // For non-parallel testing, the size of PATH_COUNT should make little difference; don't
        // waste a lot of time.
        final int PATH_COUNT = 1000;
        final ExampleCache cache = new ExampleCache();
        cache.setCachingEnabled(true);
        alreadyReportedFailure = false; // reset
        for (int pass = 0; pass < 2; pass++) {
            boolean firstPass = (pass == 0);
            IntStream.range(0, PATH_COUNT).forEach(i -> doOnePath(cache, firstPass, i));
        }
    }

    public void testParallel() {
        // For parallel testing, the size of PATH_COUNT makes a big difference to whether the test
        // passes or fails. In part, we are testing whether ExampleCache is thread-safe. If it
        // isn't, much randomness is involved. A large number is required here to make failure
        // likely if ExampleCache is not thread-safe.
        final int PATH_COUNT = 3000000;
        final ExampleCache cache = new ExampleCache();
        cache.setCachingEnabled(true);
        alreadyReportedFailure = false; // reset
        for (int pass = 0; pass < 2; pass++) {
            boolean firstPass = (pass == 0);
            IntStream.range(0, PATH_COUNT).parallel().forEach(i -> doOnePath(cache, firstPass, i));
        }
    }

    private void doOnePath(ExampleCache cache, boolean firstPass, int i) {
        if (alreadyReportedFailure) {
            return;
        }
        String xpath = "//p" + i;
        String value = "v" + i;
        String example = getExampleImmediately(value);
        String result;
        if (firstPass) {
            result = cache.computeIfAbsent(xpath, value, (star, x, v) -> getExampleImmediately(v));
            assertEquals("Example should match immediately", example, result);
            if (!example.equals(result)) {
                alreadyReportedFailure = true;
            }
            result = cache.computeIfAbsent(xpath, value, (star, x, v) -> getExampleSoon(v));
            assertEquals("Example should match soon", example, result);
        } else {
            result = cache.computeIfAbsent(xpath, value, (star, x, v) -> getExampleLater(v));
            assertEquals("Example should match later", example, result);
        }
        if (!example.equals(result)) {
            alreadyReportedFailure = true;
        }
    }

    private static String getExampleImmediately(String value) {
        return "e" + value;
    }

    private String getExampleSoon(String value) {
        return "soon" + value;
    }

    private String getExampleLater(String value) {
        return "later" + value;
    }
}

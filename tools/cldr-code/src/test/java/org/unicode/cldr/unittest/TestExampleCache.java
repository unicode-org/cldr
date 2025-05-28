package org.unicode.cldr.unittest;

import java.util.stream.IntStream;
import org.unicode.cldr.icu.dev.test.TestFmwk;
import org.unicode.cldr.test.ExampleCache;
import org.unicode.cldr.util.ThreadSafeMapOfMapOfMap;

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
        final ExampleCache exCache = new ExampleCache();
        exCache.setCachingEnabled(true);
        alreadyReportedFailure = false; // reset
        for (int pass = 0; pass < 2; pass++) {
            boolean firstPass = (pass == 0);
            IntStream.range(0, PATH_COUNT).forEach(i -> doOnePath(exCache, firstPass, i));
        }
    }

    public void testParallel() {
        // For parallel testing, the size of PATH_COUNT makes a big difference to whether the test
        // passes or fails. In part, we are testing whether ExampleCache is thread-safe. If it
        // isn't, much randomness is involved. A large number is required here to make failure
        // likely if ExampleCache is not thread-safe.
        final int PATH_COUNT = 1000000;
        final ExampleCache exCache = new ExampleCache();
        exCache.setCachingEnabled(true);
        alreadyReportedFailure = false; // reset
        for (int pass = 0; pass < 2; pass++) {
            boolean firstPass = (pass == 0);
            IntStream.range(0, PATH_COUNT)
                    .parallel()
                    .forEach(i -> doOnePath(exCache, firstPass, i));
            ThreadSafeMapOfMapOfMap.verbose = true;
        }
        ThreadSafeMapOfMapOfMap.verbose = false;
    }

    private void doOnePath(ExampleCache exCache, boolean firstPass, int i) {
        if (alreadyReportedFailure) {
            return;
        }
        String xpath = "//p" + i;
        String value = "v" + i;
        String example = "e" + i;
        String result = exCache.getExample(xpath, value);
        if (firstPass) {
            assertNull("Example should be null before putExample", result);
            if (result != null) {
                alreadyReportedFailure = true;
            }
            exCache.putExample(xpath, value, example);
            result = exCache.getExample(xpath, value);
            assertEquals("Example should match immediately", example, result);
        } else {
            assertEquals("Example should match later", example, result);
        }
        if (!example.equals(result)) {
            alreadyReportedFailure = true;
        }
    }
}

package org.unicode.cldr.unittest;

import java.util.stream.IntStream;
import org.unicode.cldr.icu.dev.test.TestFmwk;
import org.unicode.cldr.test.ExampleCache;
import org.unicode.cldr.util.ThreadSafeMapOfMapOfMap;

public class TestExampleCache extends TestFmwk {

    public static void main(String[] args) {
        new TestExampleCache().run(args);
    }

    public void testNonParallel() {
        // For non-parallel testing, the size of PATH_COUNT should make little difference; don't
        // waste a lot of time.
        final int PATH_COUNT = 10000;
        final ExampleCache exCache = new ExampleCache();
        exCache.setCachingEnabled(true);
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
        String xpath = "//p" + i;
        String value = "v" + i;
        String example = "e" + i;
        String result = exCache.getExample(xpath, value);
        if (firstPass) {
            assertNull("Example should be null before putExample", result);
            exCache.putExample(xpath, value, example);
            result = exCache.getExample(xpath, value);
            assertEquals("Example should match immediately", example, result);
        } else {
            assertEquals("Example should match later", example, result);
        }
    }
}

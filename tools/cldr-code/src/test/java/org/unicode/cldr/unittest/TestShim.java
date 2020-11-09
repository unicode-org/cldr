package org.unicode.cldr.unittest;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.unicode.cldr.util.TestShimUtils;

// NOTE: When not under Maven, you'll see compile errs in eclipse.
// Ignore them.

/**
 * a JUnit test that calls TestAll.
 */
class TestShim {
    @Test
    public void TestAll() {
        String args[] = TestShimUtils.getArgs(TestShim.class, "-n -q");
        // regular main() will System.exit() which is not too friendly.
        // call this instead.
        int errCount = TestAll.runTests(args);
        assertEquals(0, errCount, "Test had errors");
    }

    @Test
    public void TestTestShimUtilTest() {
        // Note: checks the system property corresponding to java.lang
        // We expect the system property "java.lang.testArgs" will not be set,
        // and so the default "-a -b -c" will be used.
        String args[] = TestShimUtils.getArgs(java.lang.String.class, "-a -b -c");
        String expectArgs[] = { "-a", "-b", "-c" };
        assertArrayEquals(args, expectArgs, "Expected arg parse");
    }
}
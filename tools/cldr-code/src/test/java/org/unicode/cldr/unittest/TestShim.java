package org.unicode.cldr.unittest;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.unicode.cldr.util.ShimmedMain;

// NOTE: When not under Maven, you'll see compile errs in eclipse.
// Ignore them.

/** a JUnit test that calls TestAll. */
public class TestShim extends ShimmedMain {

    public TestShim() {
        super(TestAll.class);
    }

    @Test
    public void TestAll() {
        assertEquals(0, runTests(), " had errors");
    }

    /** test for the getArgs function */
    @Test
    public void TestTestShimUtilTest() {
        // Note: checks the system property corresponding to java.lang
        // We expect the system property "java.lang.testArgs" will not be set,
        // and so the default "-a -b -c" will be used.
        String args[] = getArgs(java.lang.String.class, "-a -b -c");
        String expectArgs[] = {"-a", "-b", "-c"};
        assertArrayEquals(args, expectArgs, "Expected arg parse");
    }
}

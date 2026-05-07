package org.unicode.cldr.unittest;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.PrintWriter;

import org.junit.jupiter.api.Test;
import org.unicode.cldr.icu.dev.test.TestFmwk;
import org.unicode.cldr.icu.dev.test.TestFmwk.TestParams;
import org.unicode.cldr.util.ShimmedMain;

// NOTE: When not under Maven, you'll see compile errs in eclipse.
// Ignore them.

/** a JUnit test that calls TestAll. */
public class TestShim extends ShimmedMain {
    private static final Class<TestAll> OUR_CLASS = TestAll.class;
    // use the same args as TestAll
    private static final String[] args = ShimmedMain.getArgs(OUR_CLASS);
    private static final TestParams params =
            TestParams.create(args, new PrintWriter(System.err)).init();

    public static TestParams getParams() {
        return params;
    }

    /** return the default 'inclusion' level.
     * @see {@link TestFmwk#getInclusion()}
     */
    public static int getInclusion() {
        return getParams().inclusion;
    }

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

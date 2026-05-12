package org.unicode.cldr.unittest;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.TreeSet;
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

    @Test
    public void TestAllIsAll() {
        /** TODO CLDR-13465 only need this until all tests are passing. */
        final Set<String> oldListOfAll = new TreeSet<String>();
        for (final String s : TestAll.ALMOST_ALL_TESTS) {
            oldListOfAll.add(s);
        }
        final Set<String> whatsOnDisk = new TreeSet<String>();
        final String ACTUALLY_ALL[] = ShimmedMain.findAllTests(TestAll.class.getPackage());
        for (final String s : ACTUALLY_ALL) {
            whatsOnDisk.add(s);
        }

        final Set<String> presentButNotBeingRun = new TreeSet<String>(whatsOnDisk);
        presentButNotBeingRun.removeAll(oldListOfAll);
        final Set<String> doesNotExistButListed = new TreeSet<String>(oldListOfAll);
        doesNotExistButListed.removeAll(whatsOnDisk);
        assertAll(
                () ->
                        assertTrue(
                                presentButNotBeingRun.isEmpty(),
                                () -> "Not in TestAll’s list:" + presentButNotBeingRun.toString()),
                () ->
                        assertTrue(
                                doesNotExistButListed.isEmpty(),
                                () ->
                                        "Doesn't exist but in TestAll’s list:"
                                                + doesNotExistButListed.toString()));
    }
}

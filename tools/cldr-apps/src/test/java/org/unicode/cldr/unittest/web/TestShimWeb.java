package org.unicode.cldr.unittest.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.jcip.annotations.NotThreadSafe;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.util.ShimmedMain;

// NOTE: When not under Maven, you'll see compile errs in eclipse.
// Ignore them.

/** a JUnit test that calls TestAll. */
@NotThreadSafe
class TestShimWeb extends ShimmedMain {

    public TestShimWeb() {
        super(TestAll.class);
    }

    @Test
    public void TestAll() {
        assertEquals(0, runTests(), "Web Test had errors");
    }
}

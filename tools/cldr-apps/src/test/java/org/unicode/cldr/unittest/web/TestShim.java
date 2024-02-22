package org.unicode.cldr.unittest.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.PrintWriter;
import net.jcip.annotations.NotThreadSafe;
import org.junit.jupiter.api.Test;
import org.unicode.cldr.util.TestShimUtils;

// NOTE: When not under Maven, you'll see compile errs in eclipse.
// Ignore them.

/** a JUnit test that calls TestAll. */
@NotThreadSafe
class TestShim {
    @Test
    public void TestAll() {
        String args[] = TestShimUtils.getArgs(TestShim.class, "-n -q");
        // regular main() will System.exit() which is not too friendly.
        // call this instead.
        int errCount = TestAll.main(args, new PrintWriter(System.out)); // TODO: parameterize
        assertEquals(0, errCount, "Web Test had errors");
    }
}

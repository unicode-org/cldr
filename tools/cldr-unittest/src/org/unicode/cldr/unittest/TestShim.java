package org.unicode.cldr.unittest;

import java.io.PrintWriter;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * a JUnit test that calls TestAll.
 */
class TestShim {
    @Test
    public void TestAll() {
        String args[] = { "-n", "-q" }; // TODO: parameterize
        // regular main() will System.exit() which is not too friendly.
        // call this instead.
        int errCount = TestAll.runTests(args);
        assertEquals(errCount, 0, "Test had errors");
    }
}
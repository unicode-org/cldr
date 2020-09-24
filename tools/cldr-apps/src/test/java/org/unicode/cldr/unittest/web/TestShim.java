package org.unicode.cldr.unittest.web;

import java.io.PrintWriter;

import org.junit.jupiter.api.Test;

/**
 * a JUnit test that calls TestAll.
 */
class TestShim {
    @Test
    public void TestAll() {
        String args[] = { "-q" };
        // regular main() will System.exit() which is not too friendly.
        // call this instead.
        TestAll.main(args, new PrintWriter(System.out)); // TODO: parameterize
    }
}
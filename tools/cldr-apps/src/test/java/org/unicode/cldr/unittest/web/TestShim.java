package org.unicode.cldr.unittest.web;

import java.io.PrintWriter;

import org.junit.jupiter.api.Test;

import org.unicode.cldr.util.TestShimUtils;

/**
 * a JUnit test that calls TestAll.
 */
class TestShim {
    @Test
    public void TestAll() {
        String args[] = TestShimUtils.getArgs(TestShim.class, "-n -q");
        // regular main() will System.exit() which is not too friendly.
        // call this instead.
        TestAll.main(args, new PrintWriter(System.out)); // TODO: parameterize
    }
}
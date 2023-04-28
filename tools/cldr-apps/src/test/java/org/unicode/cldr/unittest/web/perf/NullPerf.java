/*
 **********************************************************************
 * Copyright (c) 2002-2011, International Business Machines
 * Corporation and others.  All Rights Reserved.
 **********************************************************************
 */
package org.unicode.cldr.unittest.web.perf;

/** A class for testing null performance. */
public class NullPerf extends PerfTest {
    // vars here

    public static void main(String[] args) throws Exception {
        new NullPerf().run(args);
    }

    @Override
    protected void setup(String[] args) {
        // We only take one argument, the pattern
        // if (args.length != 1) {
        // throw new RuntimeException("Please supply UnicodeSet pattern");
        // }

    }

    PerfTest.Function testNothingTenTimes() {
        return new PerfTest.Function() {
            // setup here

            @Override
            public void call() {
                for (int i = 0; i < 10; i++)
                    ;
            }

            @Override
            public long getOperationsPerIteration() {
                return 10;
            }
        };
    }
}

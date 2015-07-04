package org.unicode.cldr.util;

import java.util.LinkedHashSet;
import java.util.Set;

import org.unicode.cldr.util.StringRange.Adder;

import com.ibm.icu.dev.test.TestFmwk;

public class StringRangeTest extends TestFmwk {
    public static void main(String[] args) {
        new StringRangeTest().run(args);
    }

    public void TestSimple() {
        String[][] tests = {
            {"a", "cd", "Must have start-length ≥ end-length", 
                "", ""},
            {"a", "", "Must have end-length > 0", 
                "", ""},
            {"ab", "cd", "[ab, ac, ad, bb, bc, bd, cb, cc, cd]", 
                "ab-ad bb-bd cb-cd", "ab-d bb-d cb-d"},
            {"👦🏻", "👦🏿", "[👦🏻, 👦🏼, 👦🏽, 👦🏾, 👦🏿]", 
                    "👦🏻-👦🏿", "👦🏻-🏿"},
            {"qax👦🏻", "cx👦🏿", "[qax👦🏻, qax👦🏼, qax👦🏽, qax👦🏾, qax👦🏿, qbx👦🏻, qbx👦🏼, qbx👦🏽, qbx👦🏾, qbx👦🏿, qcx👦🏻, qcx👦🏼, qcx👦🏽, qcx👦🏾, qcx👦🏿]", 
                "qax👦🏻-qax👦🏿 qbx👦🏻-qbx👦🏿 qcx👦🏻-qcx👦🏿", "qax👦🏻-🏿 qbx👦🏻-🏿 qcx👦🏻-🏿"},
        };
        final StringBuilder b = new StringBuilder();
        Adder myAdder = new Adder() { // for testing: doesn't do quoting, etc
            @Override
            public void add(String start, String end) {
                if (b.length() != 0) {
                    b.append(' ');
                }
                b.append(start);
                if (end != null) {
                    b.append('-').append(end);
                }
            }
        };

        for (String[] test : tests) {
            Set<String> output = new LinkedHashSet<>();
            final String start = test[0];
            final String end = test[1];
            String expectedExpand = test[2];
            String expectedCompact = test[3];
            String expectedMostCompact = test[4];
            try {
                StringRange.expand(start, end, output);
                assertEquals("Expand " + start + "-" + end, expectedExpand, output.toString());
            } catch (Exception e) {
                assertEquals("Expand " + start + "-" + end, expectedExpand, e.getMessage());
                continue;
            }
            b.setLength(0);
            try {
                StringRange.compact(output, myAdder, false);
                assertEquals("Compact " + output.toString(), expectedCompact, b.toString());
            } catch (Exception e) {
                assertEquals("Compact " + output.toString(), expectedCompact, e.getMessage());
            }
            b.setLength(0);
            try {
                StringRange.compact(output, myAdder, true);
                assertEquals("Compact+ " + output.toString(), expectedMostCompact, b.toString());
            } catch (Exception e) {
                assertEquals("Compact+ " + output.toString(), expectedCompact, e.getMessage());
            }
        }
    }
}

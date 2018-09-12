package org.unicode.cldr.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.StringRange.Adder;
import org.unicode.cldr.util.Validity.Status;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.NumberFormat;

public class StringRangeTest extends TestFmwk {
    public static void main(String[] args) {
        new StringRangeTest().run(args);
    }

    static String show(Set<String> output) {
        StringBuilder b = new StringBuilder();
        for (String s : output) {
            append(b, s);
        }
        return b.toString();
    }

    static void append(StringBuilder b, String start) {
        if (start.codePointCount(0, start.length()) == 1) {
            b.append(start);
        } else {
            b.append('{').append(start).append('}');
        }
    }

    public void TestSimple() {
        String[][] tests = {
            { "a", "cd",
                "Must have start-length ≥ end-length",
                "", ""
            },
            { "a", "",
                "Must have end-length > 0",
                "", ""
            },
            { "ab", "ad",
                "{ab}{ac}{ad}",
                "{ab}-{ad}",
                "{ab}-d",
                "{ab}-{ad}",
                "{ab}-d"
            },
            { "ab", "cd",
                "{ab}{ac}{ad}{bb}{bc}{bd}{cb}{cc}{cd}",
                "{ab}-{ad} {bb}-{bd} {cb}-{cd}",
                "{ab}-d {bb}-d {cb}-d",
                "{ab}-{cd}",
                "{ab}-{cd}"
            },
            { "👦🏻", "👦🏿",
                "{👦🏻}{👦🏼}{👦🏽}{👦🏾}{👦🏿}",
                "{👦🏻}-{👦🏿}",
                "{👦🏻}-🏿",
                "{👦🏻}-{👦🏿}",
                "{👦🏻}-🏿"
            },
            { "qax👦🏻", "cx👦🏿",
                "{qax👦🏻}{qax👦🏼}{qax👦🏽}{qax👦🏾}{qax👦🏿}{qbx👦🏻}{qbx👦🏼}{qbx👦🏽}{qbx👦🏾}{qbx👦🏿}{qcx👦🏻}{qcx👦🏼}{qcx👦🏽}{qcx👦🏾}{qcx👦🏿}",
                "{qax👦🏻}-{qax👦🏿} {qbx👦🏻}-{qbx👦🏿} {qcx👦🏻}-{qcx👦🏿}",
                "{qax👦🏻}-🏿 {qbx👦🏻}-🏿 {qcx👦🏻}-🏿",
                "{qax👦🏻}-{qcx👦🏿}",
                "{qax👦🏻}-{cx👦🏿}"
            },
        };
        final StringBuilder b = new StringBuilder();
        Adder myAdder = new Adder() { // for testing: doesn't do quoting, etc
            @Override
            public void add(String start, String end) {
                if (b.length() != 0) {
                    b.append(' ');
                }
                append(b, start);
                if (end != null) {
                    b.append('-');
                    append(b, end);
                }
            }
        };

        for (String[] test : tests) {
            Set<String> output = new LinkedHashSet<>();
            final String start = test[0];
            final String end = test[1];
            String expectedExpand = test[2];
            try {
                StringRange.expand(start, end, output);
                assertEquals("Expand " + start + "-" + end, expectedExpand, show(output));
            } catch (Exception e) {
                assertEquals("Expand " + start + "-" + end, expectedExpand, e.getMessage());
                continue;
            }
            int expectedIndex = 3;
            for (Boolean more : Arrays.asList(false, true)) {
                for (Boolean shorterPairs : Arrays.asList(false, true)) {
                    b.setLength(0);
                    String expectedCompact = test[expectedIndex++];
                    final String message = "Compact " + output.toString() + ", " + shorterPairs + ", " + more + "\n\t";
                    try {
                        StringRange.compact(output, myAdder, shorterPairs, more);
                        assertEquals(message, expectedCompact, b.toString());
                    } catch (Exception e) {
                        assertEquals(message, null, e.getMessage());
                    }
                }
            }
        }
    }

    static final Splitter ONSPACE = Splitter.on(' ').omitEmptyStrings();
    static final Splitter ONTILDE = Splitter.on('~').omitEmptyStrings();

    public void TestWithValidity() {
        final StringBuilder b = new StringBuilder();
        Adder myAdder = new Adder() { // for testing: doesn't do quoting, etc
            @Override
            public void add(String start, String end) {
                if (b.length() != 0) {
                    b.append(' ');
                }
                b.append(start);
                if (end != null) {
                    b.append('~');
                    b.append(end);
                }
            }
        };

        Validity validity = Validity.getInstance();
        NumberFormat pf = NumberFormat.getPercentInstance();

        for (LstrType type : LstrType.values()) {
            final Map<Status, Set<String>> statusToCodes = validity.getStatusToCodes(type);
            for (Entry<Status, Set<String>> entry2 : statusToCodes.entrySet()) {
                Set<String> values = entry2.getValue();
                String raw = Joiner.on(" ").join(values);
                double rawsize = raw.length();
                for (Boolean more : Arrays.asList(false, true)) {
                    for (Boolean shorterPairs : Arrays.asList(false, true)) {
                        Status key = entry2.getKey();
//                if (key != Status.deprecated) continue;
                        b.setLength(0);
                        if (more) {
                            StringRange.compact(values, myAdder, shorterPairs, true);
                        } else {
                            StringRange.compact(values, myAdder, shorterPairs);
                        }
                        String compacted2 = b.toString();
                        logln(type + ":" + key + ":\t" + compacted2.length() + "/" + raw.length() + " = "
                            + pf.format(compacted2.length() / rawsize - 1.00000000000001) + "\t" + compacted2);
                        Set<String> restored = new HashSet<>();
                        for (String part : ONSPACE.split(compacted2)) {
                            Iterator<String> mini = ONTILDE.split(part).iterator();
                            String main = mini.next();
                            if (mini.hasNext()) {
                                StringRange.expand(main, mini.next(), restored);
                            } else {
                                restored.add(main);
                            }
                        }
                        assertEquals(type + ":" + key + "," + more + "," + shorterPairs, values, restored);
                    }
                }
            }
        }
    }
}

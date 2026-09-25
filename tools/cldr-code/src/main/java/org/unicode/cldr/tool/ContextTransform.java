package org.unicode.cldr.tool;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.UnicodeSet;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.util.SupplementalDataInfo;

public class ContextTransform {

    /** Implement matching as in RB Translit; Matches at a cursor offset, within bounds ( */
    public static class ContextMatcher {
        private final Pattern precursor;
        private final Pattern atCursor;
        private final Pattern postcursor;

        private ContextMatcher(Pattern precursor, Pattern atCursor, Pattern postcursor) {
            this.precursor = precursor;
            this.atCursor = atCursor;
            this.postcursor = postcursor;
        }

        static ContextMatcher fromPattern(String pattern) {
            Pattern precursor;
            Pattern atCursor;
            Pattern postcursor;
            // simple implementation to start with
            int preCoreBoundary = pattern.indexOf("}");
            int corePostBoundary = pattern.indexOf("{");
            if (preCoreBoundary <= 0) { // if } is at the start, we ignore also
                precursor = null;
            } else {
                precursor = Pattern.compile("(?<=" + pattern.substring(0, preCoreBoundary) + ")");
            }
            if (corePostBoundary < 0) {
                corePostBoundary = pattern.length(); // must have something between } and {
            }
            String core = pattern.substring(preCoreBoundary + 1, corePostBoundary);
            atCursor = Pattern.compile(core);

            String post = null;
            if (corePostBoundary >= pattern.length() - 1) {
                postcursor = null;
            } else {
                post = pattern.substring(corePostBoundary + 1);
                postcursor = Pattern.compile(post);
            }
            return new ContextMatcher(precursor, atCursor, postcursor);
        }

        Matcher match(CharSequence source, int lowerBound, int cursor, int upperBound) {
            Matcher atMatch =
                    atCursor.matcher(source).region(cursor, upperBound).useAnchoringBounds(false);
            if (!atMatch.lookingAt()) {
                return null;
            }
            if (postcursor != null) {
                Matcher postmatch =
                        postcursor
                                .matcher(source)
                                .region(atMatch.end(), upperBound)
                                .useAnchoringBounds(false);
                if (!postmatch.lookingAt()) {
                    return null;
                }
            }
            if (precursor != null) {
                Matcher prematch =
                        precursor
                                .matcher(source)
                                .region(cursor, cursor)
                                .useAnchoringBounds(false)
                                .useTransparentBounds(true);
                if (!prematch.lookingAt()) {
                    return null;
                }
            }
            return atMatch;
        }

        @Override
        public String toString() {
            return (precursor == null ? "" : precursor + "}")
                    + atCursor
                    + (postcursor == null ? "" : "{" + postcursor);
        }
    }

    static final UnicodeSet NEEDS_ESCAPING =
            new UnicodeSet("[-[:C:]\\[\\]\\\\[:whitespace:]-[\\u0020]]").freeze();

    public static String toJavaRegex(UnicodeSet uSet) {
        // for now, just code points
        // TODO, pull out $ (FFFF), and strings, into an alternation
        // the strings need to be in decreasing length
        // use single letter escapes
        // handle edge cases
        StringBuilder b = new StringBuilder();
        boolean hasFFFF = uSet.contains(0xFFFF);
        boolean hasStrings = uSet.hasStrings();
        if (hasStrings || hasFFFF) {
            b.append('(');
            if (hasFFFF) {
                b.append("^|$|");
            }
            if (hasStrings) {
                TreeSet<String> lengthFirst = new TreeSet<>(SupplementalDataInfo.LENGTH_FIRST);
                lengthFirst.addAll(uSet.strings());
                lengthFirst.stream()
                        .forEach(
                                x -> {
                                    append(b, x);
                                    b.append('|');
                                });
            }
        }
        b.append('[');
        uSet.rangeStream()
                .forEach(
                        y -> {
                            int start = y.codepoint;
                            int end = y.codepointEnd;
                            append(b, start);
                            if (end != start) {
                                b.append('-');
                                append(b, end);
                            }
                        });
        if (hasStrings) {
            b.append(')');
        }
        return b.append(']').toString();
    }

    private static void append(StringBuilder b, CharSequence s) {
        s.codePoints().forEach(x -> append(b, x));
    }

    private static void append(StringBuilder b, int x) {
        if (NEEDS_ESCAPING.contains(x)) {
            if (x <= 0xFF) {
                b.append("\\x").append(Utility.hex(x, 2));
            } else if (x <= 0xFFFF) {
                b.append("\\u").append(Utility.hex(x, 4));
            } else {
                b.append("\\x{").append(Utility.hex(x, 2)).append('}');
            }
        } else {
            b.appendCodePoint(x);
        }
    }

    public static void main(String[] args) {
        testWithoutProperties();
        testContextMatcher();
    }

    static void testWithoutProperties() {
        List<List<String>> toJava =
                List.of(
                        List.of(
                                "[\\p{whitespace}\u001f-\u007f\\u0609\\u200B\\x{13430}{ab}{x\\x19z}$]",
                                ""));

        final UnicodeSet TEST_NEGATIVE =
                new UnicodeSet("[\u1234{abcdefg}]").freeze(); // in no exampes
        toJava.stream()
                .forEach(
                        j -> {
                            UnicodeSet test = new UnicodeSet(j.get(0));
                            String expected = j.get(1);
                            String actual = toJavaRegex(test);
                            assertEquals(j.get(0), expected, actual);
                            Matcher matcher = Pattern.compile(actual).matcher("");
                            test.stream()
                                    .forEach(
                                            y ->
                                                    assertEquals(
                                                            "", matcher.reset(y).matches(), true));

                            TEST_NEGATIVE.stream()
                                    .forEach(
                                            z ->
                                                    assertEquals(
                                                            "", matcher.reset(z).matches(), false));
                        });
    }

    private static void testContextMatcher() {
        // System.out.println(fixJavaRegex("[\\p{whitespace}-[:C:]]*"));
        UnicodeSet foo = new UnicodeSet("[$]");
        System.out.println("{" + Utility.hex(foo.charAt(0), 4) + "}");

        List<List<String>> tests =
                List.of(
                        List.of("}b{", "b", "xby", "-T-"),
                        List.of("b", "b", "xby", "-T-"),
                        List.of("a}b{c", "(?<=a)}b{c", "abc", "-T-"));
        tests.stream()
                .forEach(
                        x -> {
                            String test = x.get(0);
                            String expected = x.get(1);

                            String trial = x.get(2);
                            String expectedTrial = x.get(3);

                            ContextMatcher cm = ContextMatcher.fromPattern(test);
                            String actual = cm.toString();
                            assertEquals(test, expected, actual);

                            int trialLength = trial.length();
                            for (int i = 0; i < trialLength; ++i) {
                                Matcher m = cm.match(trial, 0, i, trialLength);
                                char expectedT = expectedTrial.charAt(i);
                                boolean ok = (m == null) == (expectedT == '-');
                                if (ok) {
                                    // ok
                                } else {
                                    cm.match(trial, 0, i, trialLength);
                                    System.out.println(
                                            "FAIL: "
                                                    + test
                                                    + " expected="
                                                    + expectedT
                                                    + " actual="
                                                    + String.valueOf(m));
                                }
                            }
                        });
    }

    private static <T> void assertEquals(String message, T expected, T actual) {
        System.out.println(
                Objects.equals(expected, actual)
                        + ": "
                        + message
                        + " expected="
                        + expected
                        + " actual="
                        + actual);
    }
}

package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A simple class that allows more flexible regex matching, including negative matching and
 * requiring multiple patterns to match
 */
public class XPattern {
    private final List<Pattern> patterns;
    private final List<Boolean> positive;

    /**
     * Sequence of patterns prefixed and separated by &&& or !!!. An initial &&& can be omitted. The
     * &&& indicates there must be a positive match after, while !!! indicates there must be a
     * negative match after. If neither occurs, then there is just a simple pattern match. Examples:
     *
     * <ul>
     *   <li>A!!!B&&&C — A must match, B must not match, C must match
     *   <li>!!!A — A must not match
     * </ul>
     */
    static Pattern SEP = Pattern.compile("(&{3}|!{3}|-{3})");

    private XPattern(String xPattern) {
        final List<Pattern> patterns = new ArrayList<>();
        final List<Boolean> positive = new ArrayList<>();
        ;

        Matcher matcher = SEP.matcher(xPattern);
        int lastEnd = 0;

        while (matcher.find()) {
            // add patterns
            if (matcher.start() > lastEnd) {
                if (lastEnd == 0) {
                    positive.add(true);
                }
                String patternText = xPattern.substring(lastEnd, matcher.start());
                patterns.add(Pattern.compile(patternText));
            } else if (lastEnd != 0) {
                throw new IllegalArgumentException(
                        "!!! and &&& must be followed by a regex Pattern");
            }

            // set the next value
            positive.add(
                    matcher.group(0).equals("&&&")); // set to positive if &&&, negative otherwise
            lastEnd = matcher.end();
        }

        // process final pattern
        if (lastEnd < xPattern.length()) {
            if (lastEnd == 0) {
                positive.add(true);
            }
            patterns.add(Pattern.compile(xPattern.substring(lastEnd)));
        } else {
            throw new IllegalArgumentException("!!! and &&& must be followed by a regex Pattern");
        }
        if (patterns.size() != positive.size()) {
            throw new InternalCldrException("Internal error; sizes don't match");
        }
        this.patterns = List.copyOf(patterns);
        this.positive = List.copyOf(positive);
    }

    public static XPattern compile(String xPattern) {
        return new XPattern(xPattern);
    }

    public XMatcher matcher(String source) {
        return new XMatcher(source);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < patterns.size(); ++i) {
            if (!positive.get(i)) {
                sb.append("!!!");
            } else if (sb.length() != 0) {
                sb.append("&&&");
            }
            sb.append(patterns.get(i));
        }
        return sb.toString();
    }

    public class XMatcher {
        private final List<Matcher> matchers;

        private XMatcher(String source) {
            this.matchers =
                    patterns.stream()
                            .map(x -> x.matcher(source))
                            .collect(Collectors.toUnmodifiableList());
        }

        public XMatcher reset(String source) {
            matchers.stream().forEach(Matcher::reset);
            return this;
        }

        public boolean match() {
            for (int i = 0; i < matchers.size(); ++i) {
                if (matchers.get(i).matches() != positive.get(i)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < matchers.size(); ++i) {
                if (!positive.get(i)) {
                    sb.append("!!!");
                } else if (sb.length() != 0) {
                    sb.append("&&&");
                }
                sb.append(matchers.get(i));
            }
            return sb.toString();
        }
    }
}

package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;

public class BoundaryTransform implements BiFunction<String, Integer, String> {
    private static final Splitter SEMI = Splitter.on(';').trimResults().omitEmptyStrings();
    private static final Splitter SEPARATOR = Splitter.on('|');
    private static final Splitter RESULTS_IN = Splitter.on('→').trimResults();
    private static final Joiner JOIN_SEMI = Joiner.on(";");

    public static class MatchOutput {
        public String s;
        public int start;
        public int boundary;
        public int end;
    }

    private static class BoundaryRule {

        private final Pattern beforeBoundary;
        private final Pattern afterBoundary;
        private final boolean hasB;
        private final boolean hasA;
        private final String replaceBy;

        /*
         * TODO, change to real syntax
         */
        private BoundaryRule(String rawPattern, String replaceBy) {
            Iterator<String> split = SEPARATOR.split(rawPattern).iterator();
            String pre = split.next();
            String before = split.next();
            String after = split.next();
            String post = split.next();

            hasB = !before.isBlank();
            hasA = !after.isBlank();
            this.beforeBoundary = Pattern.compile(
                pre +
                (hasB ? "(?<b>" + before + ")" : "")
                + "$");
            this.afterBoundary = Pattern.compile(
                (hasA ? "(?<a>" + after + ")" : "")
                + post);
            this.replaceBy = replaceBy;
        }

        private String matches(String s, int start, int boundary, int end) {
            Matcher matchAfter = afterBoundary.matcher(s)
                .region(boundary, end);
            if (!matchAfter.lookingAt()) {
                return null;
            }

            Matcher matchBefore = beforeBoundary.matcher(s)
                .region(start, boundary)
                ;
            if (!matchBefore.find()) {
                return null;
            }

            return s.substring(0, hasB ? matchBefore.start("b") : boundary)
                + replaceBy
                + s.substring(hasA ? matchAfter.end("a") : boundary);
        }
        @Override
        public String toString() {
            return beforeBoundary + "|" + afterBoundary + "→" + replaceBy;
        }
    }

    private List<BoundaryRule> rules;

    private BoundaryTransform(List<BoundaryRule> rules) {
        this.rules = rules;
    }

    public static BoundaryTransform from(String rulesPattern) {
        List<BoundaryRule> rules = new ArrayList<>();
        for (String rulePattern : SEMI.split(rulesPattern)) {
            // HACK for now
            Iterator<String> it = RESULTS_IN.split(rulePattern).iterator();
            String source = it.next();
            String result = it.next();
            rules.add(new BoundaryRule(source, result));
        }
        return new BoundaryTransform(rules);
    }

    @Override
    public String apply(String t, Integer u) {
        int boundary = u;
        for (BoundaryRule rule : rules) {
            String result = rule.matches(t, 0, boundary, t.length());
            if (result != null) {
                return result;
            }
        }
        return t;
    }
    @Override
    public String toString() {
        return JOIN_SEMI.join(rules);
    }

    private static Map<String, BoundaryTransform> LOCALE_TO_BOUNDARY_TRANSFORM = ImmutableMap.
        <String, BoundaryTransform>builder()
        .put("pt", BoundaryTransform.from(
            "|||s→s"))
        .put("el", BoundaryTransform.from(
            "|ο-|μέ|→όμε;"
                + "|-|βατ$|→βάτ;"
                + "|-|μπαρ$|→μπάρ;"
                + "|-|χερτζ$|→χέρτζ;"
                + "|ο-|λίτρ|→όλιτρ;"
            + "|-||→"))
        .build();
    public static BoundaryTransform getTransform(String locale) { // TODO do inheritance
        return LOCALE_TO_BOUNDARY_TRANSFORM.get(locale);
    }
}

package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class BoundaryTransform implements BiFunction<String, Integer, String> {
    private static final Splitter SEMI = Splitter.on(';').omitEmptyStrings();
    private static final Joiner JOIN_SEMI = Joiner.on(";");

    public static final char START_TO_REPLACE = '⦅';
    public static final char BOUNDARY = '❙';
    public static final char END_TO_REPLACE = '⦆';
    public static final char START_REPLACE_BY = '→';

    public static final Pattern NAMED_GROUP = Pattern.compile("\\Q(?<\\E([A-Za-z0-9]+)>");

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
        private final Set<String> namedGroupsBefore;
        private final Set<String> namedGroupsAfter;

        /*
         * TODO, change to real syntax
         */
        private BoundaryRule(String rawPattern) {
            int lastFound = 0;
            String contextBefore = "";
            String replaceBefore = "";
            String replaceAfter = "";
            String contextAfter = "";
            String replaceBy = "";
            boolean haveBoundary = false;
            int boundaryStart = -1;
            boolean haveEndToReplace = false;
            boolean haveArrow = false;
            for (int i = 0; i < rawPattern.length(); ++i) {
                // nothing depends on code points, so handle simply
                switch(rawPattern.charAt(i)) {
                case START_TO_REPLACE:
                    contextBefore = rawPattern.substring(lastFound, i);
                    lastFound = i+1;
                    break;
                case BOUNDARY:
                    haveBoundary = true;
                    boundaryStart = i;
                    replaceBefore = rawPattern.substring(lastFound, i);
                    lastFound = i+1;
                    break;
                case END_TO_REPLACE:
                    if (!haveBoundary) {
                        throw new IllegalArgumentException("Syntax is: contextBefore ⦅ replaceBefore ❙ replaceAfter ⦆ contextAfter → replaceBy; must have ❙ and →");
                    }
                    haveEndToReplace = true;
                    replaceAfter = rawPattern.substring(lastFound, i);
                    lastFound = i+1;
                    break;
                case START_REPLACE_BY:
                    if (!haveBoundary) {
                        throw new IllegalArgumentException("Syntax is: contextBefore ⦅ replaceBefore ❙ replaceAfter ⦆ contextAfter → replaceBy; must have ❙ and →");
                    }
                    haveArrow = true;
                    if (!haveEndToReplace) {
                        replaceAfter = rawPattern.substring(lastFound, i);
                    } else {
                        contextAfter = rawPattern.substring(lastFound, i);
                    }
                    lastFound = i+1;
                    break;
                }
            }
            if (!haveArrow) {
                throw new IllegalArgumentException("Syntax is: contextBefore ⦅ replaceBefore ❙ replaceAfter ⦆ contextAfter → replaceBy; must have ❙ and →");
            }
            replaceBy = rawPattern.substring(lastFound);

            hasB = !replaceBefore.isEmpty();
            hasA = !replaceAfter.isEmpty();
            this.beforeBoundary = Pattern.compile(
                contextBefore +
                (hasB ? "(?<b>" + replaceBefore + ")" : "")
                + "$");
            this.afterBoundary = Pattern.compile(
                (hasA ? "(?<a>" + replaceAfter + ")" : "")
                + contextAfter);
            this.replaceBy = replaceBy;

            // get named groups
            this.namedGroupsBefore = getNamedGroups(rawPattern.substring(0,boundaryStart));
            this.namedGroupsAfter = getNamedGroups(rawPattern.substring(boundaryStart + 1));
            if (this.namedGroupsBefore.contains("b") || this.namedGroupsAfter.contains("a")) {
                throw new IllegalArgumentException("Can't use named groups <b> or <a>");
            }
            //TODO optimize: if replaceBy == replaceBefore == replaceAfter, then matching doesn't
            // change the result (it just blocks further rules).
        }

        private Set<String> getNamedGroups(String rawPattern) {
            Set<String> _namedGroups = new TreeSet<>();
            Matcher namedGroup = NAMED_GROUP.matcher(rawPattern);
            while(namedGroup.find()) {
                _namedGroups.add(namedGroup.group(1));
            }
            return ImmutableSet.copyOf(_namedGroups);
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

            // fix namedGroups
            String toReplaceBy = replaceBy;
            for (String namedGroup : namedGroupsBefore) {
                toReplaceBy = toReplaceBy.replace("${" + namedGroup + "}", matchBefore.group(namedGroup));
            }
            for (String namedGroup : namedGroupsAfter) {
                toReplaceBy = toReplaceBy.replace("${" + namedGroup + "}", matchAfter.group(namedGroup));
            }

            return s.substring(0, hasB ? matchBefore.start("b") : boundary)
                + toReplaceBy
                + s.substring(hasA ? matchAfter.end("a") : boundary);
        }
        @Override
        public String toString() {
            return beforeBoundary.toString() + BOUNDARY + afterBoundary + START_REPLACE_BY + replaceBy;
        }
    }

    private List<BoundaryRule> rules;

    private BoundaryTransform(List<BoundaryRule> rules) {
        this.rules = rules;
    }

    /**
     * The format is:
     * ruleset = rule (';' rule)*
     * rule = before '❙' after '→' replaceBy
     * before = (contextBefore '⦅')? replaceBefore
     * after = replaceAfter ('⦆' contextAfter)?
     * @param rulesPattern
     * @return
     */
    public static BoundaryTransform from(String rulesPattern) {
        List<BoundaryRule> rules = new ArrayList<>();
        for (String rulePattern : SEMI.split(rulesPattern)) {
            rules.add(new BoundaryRule(rulePattern));
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

    // rule = contextBefore ⦅ replaceBefore ❙ replaceAfter ⦆ contextAfter → replaceBy

    // TODO build with data
    // TODO consider for general having ❙, ❴, or ❵. That is:
    //   ❴ is only applied at the start of a placeholder
    //   ❵ is only applied at the end of a placeholder
    //   ❙ is applied at both positions


    public enum BoundaryUsage {general, unitPrefix}

    private static Map<String, BoundaryTransform> GENERAL_LOCALE_TO_BOUNDARY_TRANSFORM = ImmutableMap.
        <String, BoundaryTransform>builder()
        .put("it", BoundaryTransform.from(""
            + " e⦅ ❙⦆[eE]($|[^dD])→d ;" // ed before e (with exception)
            + " o⦅ ❙⦆[oO]($|[^dD])→d ;" // od before o (with exception)
            ))
        .put("es", BoundaryTransform.from(""
            + " ⦅y ❙⦆([iI]|[hH][iI]($|[^aAeE]))→e ;" // y
            + " ⦅o ❙⦆([uU8]|[hH][oU]|11($| ))→u ;" // o
            ))
        .put("he", BoundaryTransform.from(""
            + " \\u05D5⦅❙⦆\\P{sc=Hebr}→-;" // y
            ))
        .put("en", BoundaryTransform.from(
            " a⦅ ❙⦆[aeiou]→n ;" // doesn't handle "an hour", or "an underground"
            ))
        .put("zh", BoundaryTransform.from(
            "[\\p{L}&&\\p{sc=hani}]⦅❙⦆[\\p{L}&&\\P{sc=hani}]→ ;" // add space at chinese/non-chinese letter junction
           + "[\\p{L}&&\\P{sc=hani}]⦅❙⦆[\\p{L}&&\\p{sc=hani}]→ ;" // add space at non-chinese/chinese letter junction
            ))
        .build();

    private static Map<String, BoundaryTransform> UNITS_LOCALE_TO_BOUNDARY_TRANSFORM = ImmutableMap.
        <String, BoundaryTransform>builder()
        .put("pt", BoundaryTransform.from(
            "[aáàâãeéêiíoóòôõuú]⦅❙⦆s→s"))
        .put("el", BoundaryTransform.from(
            "ο-❙μέ→όμε;"
                + "-❙βατ⦆→βάτ;"
                + "-❙μπαρ$→μπάρ;"
                + "-❙χερτζ$→χέρτζ;"
                + "ο-❙λίτρ→όλιτρ;"
            + "-❙→"))
        .put("fil", BoundaryTransform.from(
            "⦅(?<before>.*)❙na ⦆→na ${before};")) // move 'na ' to the front
        .build();

    private static Map<BoundaryUsage, Map<String, BoundaryTransform>> usageTo= ImmutableMap.of(
        BoundaryUsage.unitPrefix, UNITS_LOCALE_TO_BOUNDARY_TRANSFORM,
        BoundaryUsage.general, GENERAL_LOCALE_TO_BOUNDARY_TRANSFORM);

    public static BoundaryTransform getTransform(String locale, BoundaryUsage usage) { // TODO do inheritance
        Map<String, BoundaryTransform> localeToBoundary = usageTo.get(usage);
        if (localeToBoundary == null) {
            return null;
        }
        while (true) {
            BoundaryTransform result = localeToBoundary.get(locale);
            if (result != null) {
                return result;
            }
            // TODO use real parent
            locale = LanguageTagParser.getSimpleParent(locale);
            if (locale == null || locale.equals("root")) {
                return null;
            }
        }
    }
}

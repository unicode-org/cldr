package org.unicode.cldr.tool;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import org.unicode.cldr.tool.XCoverageLevel.AttributesMatcher.Builder;
import org.unicode.cldr.util.CLDRLocale;
import org.unicode.cldr.util.Joiners;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.NestedMap.ImmutableMap2;
import org.unicode.cldr.util.NestedMap.Map2;
import org.unicode.cldr.util.Splitters;

class XCoverageLevel {
    public static final boolean DEBUG = System.getProperty("debug") != null;
    public static Set<String> TEST_PATHS =
            ImmutableSet.of("//ldml/localeDisplayNames/languages/language[@type]");
    private static final boolean SHOW_ADD = false;

    private static final String BAD_LINE =
            "Lines must be x=y where x is path, level, elseLevel, attrN (for N in 0..5), %<variable>\n";

    private final ImmutableMap2<String, AttributesMatcher, Level>
            pathChassisToAttributeMatcherToLevel;
    private final ImmutableMap<String, String> variableToValue;

    public XCoverageLevel(
            ImmutableMap2<String, AttributesMatcher, Level> pathChassisToAttributeMatcherToLevel,
            ImmutableMap<String, String> variableToValue) {
        this.pathChassisToAttributeMatcherToLevel = pathChassisToAttributeMatcherToLevel;
        this.variableToValue = variableToValue;
    }

    public static XCoverageLevel fromMap2(
            Map2<String, AttributesMatcher, Level> pathChassisToAttributeMatcherToLevel,
            Map<String, String> variableToValue) {
        return new XCoverageLevel(
                pathChassisToAttributeMatcherToLevel.createImmutable(),
                ImmutableMap.copyOf(variableToValue));
    }

    /** Subtract any path rules and/or rules that are identical; inverse of addMissing */
    public XCoverageLevel subtractSame(XCoverageLevel toRemoveIfSame) {
        XDelta delta = getDelta(toRemoveIfSame);
        Map2<String, AttributesMatcher, Level> filteredRules = Map2.create(LinkedHashMap::new);
        pathChassisToAttributeMatcherToLevel.stream()
                .filter(x3 -> !delta.sameRules.contains(x3.getKey1()))
                .forEach(x2 -> filteredRules.put(x2));
        Map<String, String> filteredVariables = Maps.newLinkedHashMap();
        variableToValue.entrySet().stream()
                .filter(x -> !delta.sameVariable.contains(x.getKey()))
                .forEach(x1 -> filteredVariables.put(x1.getKey(), x1.getValue()));
        XCoverageLevel reduced = fromMap2(filteredRules, filteredVariables);
        return reduced;
    }

    /** Add path rules and/or variables that are missing; inverse of subtractSame */
    public XCoverageLevel addMissing(XCoverageLevel toAddIfNotInThis) {
        final Map2<String, AttributesMatcher, Level>

                // handle rules
                pathChassisToAttributeMatcherToLevel =
                Map2.create(TreeMap::new, LinkedHashMap::new);
        pathChassisToAttributeMatcherToLevel.putAll(
                toAddIfNotInThis.pathChassisToAttributeMatcherToLevel);
        pathChassisToAttributeMatcherToLevel.putAll(this.pathChassisToAttributeMatcherToLevel);

        // handle variables
        final Map<String, String> variableToValue = Maps.newTreeMap();
        variableToValue.putAll(toAddIfNotInThis.variableToValue);
        variableToValue.putAll(this.variableToValue);

        return XCoverageLevel.fromMap2(pathChassisToAttributeMatcherToLevel, variableToValue);
    }

    @Override
    public boolean equals(Object obj) {
        XCoverageLevel other = (XCoverageLevel) obj;
        return pathChassisToAttributeMatcherToLevel.equals(
                        other.pathChassisToAttributeMatcherToLevel)
                && variableToValue.equals(other.variableToValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pathChassisToAttributeMatcherToLevel, variableToValue);
    }

    private static final class XDelta {
        public final ImmutableSet<String> sameRules;
        public final ImmutableSet<String> rulesInMe;
        public final ImmutableSet<String> rulesInOther;
        public final ImmutableSet<String> sameVariable;
        public final ImmutableSet<String> variableInMe;
        public final ImmutableSet<String> variableInOther;

        public XDelta(
                Set<String> sameChassis,
                Set<String> rulesInMe,
                Set<String> rulesInOther,
                Set<String> sameVariable,
                Set<String> variableToValueInMe,
                Set<String> variableToValueInOther) {

            this.sameRules = ImmutableSet.copyOf(sameChassis);
            this.rulesInMe = ImmutableSet.copyOf(rulesInMe);
            this.rulesInOther = ImmutableSet.copyOf(rulesInOther);
            this.sameVariable = ImmutableSet.copyOf(sameVariable);
            this.variableInMe = ImmutableSet.copyOf(variableToValueInMe);
            this.variableInOther = ImmutableSet.copyOf(variableToValueInOther);
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append("SAME Rules: " + sameRules.size()).append('\n');
            result.append("MY Rules: " + rulesInMe.size()).append('\n');
            result.append("OTHER Rules: " + rulesInMe.size()).append('\n');
            result.append("SAME Variables: " + sameVariable.size()).append('\n');
            result.append("MY Variables: " + variableInMe.size()).append('\n');
            result.append("OTHER Variables: " + variableInOther.size()).append('\n');

            return result.toString();
        }
    }

    private XDelta getDelta(XCoverageLevel other) {
        Set<String> sameRules = Sets.newTreeSet();
        Set<String> rulesInMe = Sets.newTreeSet();
        Set<String> rulesInOther = Sets.newTreeSet();

        Set<String> sameVariable = Sets.newTreeSet();
        Set<String> variableInMe = Sets.newTreeSet();
        Set<String> variableInOther = Sets.newTreeSet();

        SetView<String> chassisSet =
                Sets.union(
                        pathChassisToAttributeMatcherToLevel.keySet(),
                        other.pathChassisToAttributeMatcherToLevel.keySet());
        for (String chassis : chassisSet) {
            Map<AttributesMatcher, Level> inMe =
                    pathChassisToAttributeMatcherToLevel.getMap(chassis);
            Map<AttributesMatcher, Level> inOther =
                    other.pathChassisToAttributeMatcherToLevel.getMap(chassis);
            if (!Objects.equals(inMe, inOther)) {
                Objects.equals(inMe, inOther); // for debugging
                if (inMe != null) {
                    rulesInMe.add(chassis);
                }
                if (inOther != null) {
                    rulesInOther.add(chassis);
                }
            } else {
                sameRules.add(chassis);
            }
        }
        SetView<String> variableSet =
                Sets.union(variableToValue.keySet(), other.variableToValue.keySet());
        for (String variable : variableSet) {
            String inMe = variableToValue.get(variable);
            String inOther = other.variableToValue.get(variable);
            if (!Objects.equals(inMe, inOther)) {
                if (inMe != null) {
                    variableInMe.add(variable);
                }
                if (inOther != null) {
                    variableInOther.add(variable);
                }
            } else {
                sameVariable.add(variable);
            }
        }

        return new XDelta(
                sameRules, rulesInMe, rulesInOther, sameVariable, variableInMe, variableInOther);
    }

    public Level getCoverage(String path) {
        SplitPath splitPath = SplitPath.from(path);
        List<String> attributes = splitPath.getAttributeValues();
        String chassis = splitPath.getChassis();
        if (DEBUG && TEST_PATHS.contains(chassis)) {
            int debug = 0;
        }
        Map<AttributesMatcher, Level> matching =
                pathChassisToAttributeMatcherToLevel.getMap(chassis);
        if (matching != null) {
            for (Entry<AttributesMatcher, Level> entry : matching.entrySet()) {
                AttributesMatcher key = entry.getKey();
                if (key.hasMatch(attributes)) {
                    return entry.getValue();
                }
            }
        }
        return Level
                .UNDETERMINED; // we only need a elseLevel if the value is not comprehensive. But
        // for now we signal failures
    }

    public static class SetMatcher {
        final Boolean positive; // if false, must not match
        final ImmutableSet<String> matches;

        public SetMatcher(boolean positive, String patternString) {
            this.positive = positive;
            this.matches = ImmutableSet.copyOf(Splitters.COMMA.split(patternString));
        }

        public boolean contains(String attributeValue) {
            return matches.contains(attributeValue) == positive;
        }

        @Override
        public String toString() {
            return (positive ? "" : "!") + Joiners.COMMA.join(matches);
        }

        @Override
        public boolean equals(Object obj) {
            SetMatcher other = (SetMatcher) obj;
            return positive == other.positive && matches.equals(other.matches);
        }

        @Override
        public int hashCode() {
            return Objects.hash(positive, matches);
        }

        public Object toString(Map<String, String> valueToVariable) {
            String value = Joiners.COMMA.join(matches);
            String value2 = valueToVariable.getOrDefault(value, value);
            return (positive ? "" : "!") + value2;
        }
    }

    public static class AttributesMatcher {
        static final AttributesMatcher EMPTY = new Builder().build();

        final Map<Integer, SetMatcher> attrNumToSetMatcher;

        private AttributesMatcher(ImmutableMap<Integer, SetMatcher> patterns) {
            this.attrNumToSetMatcher = patterns;
        }

        @Override
        public boolean equals(Object obj) {
            AttributesMatcher other = (AttributesMatcher) obj;
            return attrNumToSetMatcher.equals(other.attrNumToSetMatcher);
        }

        @Override
        public int hashCode() {
            return Objects.hash(attrNumToSetMatcher);
        }

        static class Builder {
            private final Map<Integer, SetMatcher> _attrNumToSetMatcher = Maps.newTreeMap();

            public void add(int attributeNumber, boolean positive, String patternString) {
                _attrNumToSetMatcher.put(attributeNumber, new SetMatcher(positive, patternString));
            }

            /**
             * Builds an AttributesMatcher. After building the builder is cleared, so that the
             * builder can be reused can be added
             */
            public AttributesMatcher build() {
                AttributesMatcher result =
                        new AttributesMatcher(ImmutableMap.copyOf(_attrNumToSetMatcher));
                _attrNumToSetMatcher.clear();
                return result;
            }

            @Override
            public String toString() {
                return _attrNumToSetMatcher + "\n\t" + _attrNumToSetMatcher;
            }

            public boolean isEmpty() {
                return _attrNumToSetMatcher.isEmpty();
            }
        }

        boolean hasMatch(List<String> attributeValues) {
            return attrNumToSetMatcher.entrySet().stream()
                    .allMatch(
                            entry ->
                                    entry.getValue().contains(attributeValues.get(entry.getKey())));
        }

        public boolean isEmpty() {
            return attrNumToSetMatcher.isEmpty();
        }

        @Override
        public String toString() {
            return toString(Map.of());
        }

        public String toString(Map<String, String> valueToVariable) {
            StringBuilder result = new StringBuilder();
            for (Entry<Integer, SetMatcher> entry : attrNumToSetMatcher.entrySet()) {
                result.append(" attr")
                        .append(entry.getKey())
                        .append('=')
                        .append(entry.getValue().toString(valueToVariable))
                        .append('\n');
            }
            return result.toString();
        }
    }

    public static XCoverageLevel fromLocaleCompacted(Path directory, String locale) {
        Path filepath = directory.resolve(locale + ".txt");

        XCoverageLevel root = XCoverageLevel.fromFile(filepath);
        if (locale.equals("root")) {
            return root;
        }
        String baseLanguage = CLDRLocale.getInstance(locale).getLanguage();
        XCoverageLevel baseMinus = fromLocale(directory, locale);
        // we layer base on top of root
        XCoverageLevel base = baseMinus.addMissing(root);

        if (baseLanguage.equals(locale)) {
            return base;
        } else {
            // we layer on top of a base locale, which is layered on top of root
            XCoverageLevel childMinus = fromLocale(directory, locale);
            return childMinus.addMissing(base);
        }
    }

    public static XCoverageLevel fromLocale(Path directory, String locale) {
        Path filepath = directory.resolve(locale + ".txt");
        return XCoverageLevel.fromFile(filepath);
    }

    private static XCoverageLevel fromFile(Path filepath) {
        try {
            return fromLines(Files.readAllLines(filepath));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("From " + filepath, e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static XCoverageLevel fromLines(Iterable<String> allLines) {

        Map<String, String> variableToValue = Maps.newTreeMap();
        final Map2<String, AttributesMatcher, Level> pathChassisToAttributeMatcherToLevel =
                Map2.create(LinkedHashMap::new);

        String lastPath = null;
        Level lastLevel = null;
        Level nextLevel = null;
        AttributesMatcher.Builder amBuilder = new AttributesMatcher.Builder();
        int lineNumber = 0;
        for (String line : allLines) {
            ++lineNumber;
            // #x is a comment
            line = line.trim();
            if (line.startsWith("#") || line.isBlank()) {
                continue;
            }
            String type;
            String result;
            int eq = line.indexOf('=');
            if (eq < 0) {
                type = line;
                result = null;
            } else {
                type = line.substring(0, eq);
                result = line.substring(eq + 1);
            }

            if (DEBUG && TEST_PATHS.contains(lastPath)) {
                int debug = 0;
            }
            switch (type) {
                case "path":
                    if (lastPath != null) {
                        addPath(
                                pathChassisToAttributeMatcherToLevel,
                                lastPath,
                                lastLevel,
                                amBuilder);
                    }
                    lastPath = result;
                    break;
                case "level":
                    nextLevel = Level.fromString(result);
                    if (lastLevel != null && lastLevel.compareTo(nextLevel) > 0) {
                        throw new IllegalArgumentException(
                                "Levels for a path must be strictly increasing: L"
                                        + lineNumber
                                        + ": "
                                        + line);
                    }
                    addPath(pathChassisToAttributeMatcherToLevel, lastPath, nextLevel, amBuilder);
                    lastLevel = nextLevel;
                    break;
                case "elseLevel":
                    lastLevel = Level.fromString(result);
                    addPath(
                            pathChassisToAttributeMatcherToLevel,
                            lastPath,
                            lastLevel,
                            amBuilder); // attributesMatchers is [] at this point
                    lastPath = null;
                    lastLevel = null;
                    break;
                default:
                    if (type.startsWith("attr")) {
                        Integer attrNum = Integer.valueOf(type.substring(4));
                        addWithVariableReplacement(variableToValue, amBuilder, attrNum, result);
                    } else if (type.startsWith("%")) {
                        variableToValue.put(type, result);
                    } else {
                        throw new IllegalArgumentException(
                                BAD_LINE + " L" + lineNumber + ": " + line);
                    }
            }
        }
        return new XCoverageLevel(
                pathChassisToAttributeMatcherToLevel.createImmutable(),
                ImmutableMap.copyOf(variableToValue));
    }

    private static void addWithVariableReplacement(
            Map<String, String> variableToValue,
            AttributesMatcher.Builder amBuilder,
            int i,
            String result) {
        boolean positive = true;
        if (result.startsWith("!")) {
            positive = false;
            result = result.substring(1);
        }
        String vresult = variableToValue.get(result);
        amBuilder.add(i, positive, vresult == null ? result : vresult);
    }

    private static void addPath(
            final Map2<String, AttributesMatcher, Level> pathChassisToAttributeMatcherToLevel,
            String lastPath,
            Level lastLevel,
            Builder amBuilder) {
        if (DEBUG && (SHOW_ADD || TEST_PATHS.contains(lastPath))) {}

        pathChassisToAttributeMatcherToLevel.put(lastPath, amBuilder.build(), lastLevel);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        result.append(
                "# DRAFT data for coverage. For the file format, see the readme.md in this directory.\n\n"
                        + "# Variables\n\n");

        Map<String, String> valueToVariable = Maps.newTreeMap();

        for (Entry<String, String> entry : variableToValue.entrySet()) {
            String value = entry.getValue();
            String variable = entry.getKey();
            result.append(variable).append('=').append(value).append('\n');
            valueToVariable.put(value, variable);
        }

        result.append("\n# Rules\n");

        for (Entry<String, Map<AttributesMatcher, Level>> entry :
                pathChassisToAttributeMatcherToLevel.getMapMap().entrySet()) {
            result.append('\n');
            result.append("path=").append(entry.getKey()).append('\n');
            getString(entry, valueToVariable, result);
        }

        return result.toString();
    }

    private void getString(
            Entry<String, Map<AttributesMatcher, Level>> entry,
            Map<String, String> valueToVariable,
            StringBuilder result) {
        Set<Entry<AttributesMatcher, Level>> matchersAndLevel = entry.getValue().entrySet();
        for (Entry<AttributesMatcher, Level> entry2 : matchersAndLevel) {
            AttributesMatcher key = entry2.getKey();
            Level level = entry2.getValue();
            if (key.isEmpty()) {
                result.append("  elseLevel=").append(level).append('\n');
            } else {
                result.append(key.toString(valueToVariable));
                result.append("  level=").append(level).append('\n');
            }
        }
    }

    public ImmutableMap<String, String> getInternalVariables() {
        return variableToValue;
    }

    public ImmutableMap2<String, AttributesMatcher, Level> getInternalMapping() {
        return pathChassisToAttributeMatcherToLevel;
    }

    public String getPathData(String chassis) {
        Map<AttributesMatcher, Level> map = pathChassisToAttributeMatcherToLevel.getMap(chassis);
        if (map == null) {
            return "NO DATA";
        }
        StringBuilder result = new StringBuilder();
        Level lastLevel = null;
        for (Entry<AttributesMatcher, Level> entry : map.entrySet()) {
            AttributesMatcher am = entry.getKey();
            Level level = entry.getValue();
            if (level != lastLevel) {
                result.append("level=" + level).append('\n');
                lastLevel = level;
            }
            result.append(am).append('\n');
        }
        return result.toString();
    }

    public boolean isEmpty() {
        return variableToValue.isEmpty() && pathChassisToAttributeMatcherToLevel.size() == 0;
    }
}

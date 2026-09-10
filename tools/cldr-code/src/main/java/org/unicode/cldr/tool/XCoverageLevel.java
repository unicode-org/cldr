package org.unicode.cldr.tool;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
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
import org.unicode.cldr.tool.GenerateXCoverage.Variables;
import org.unicode.cldr.tool.XCoverageLevel.AttributesMatcher.Builder;
import org.unicode.cldr.util.Joiners;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.NestedMap.ImmutableMap2;
import org.unicode.cldr.util.NestedMap.Map2;
import org.unicode.cldr.util.Splitters;

class XCoverageLevel {
    public static final boolean DEBUG = System.getProperty("debug") != null;
    public static Set<String> TEST_PATHS =
            ImmutableSet.of("//ldml/dates/calendars/calendar[@type]/dateTimeFormats/availableFormats/dateFormatItem[@id]");
    private static final boolean SHOW_ADD = false;

    private static final String BAD_LINE =
            "Lines must be x=y where x is path, level, elseLevel, attrN (for N in 0..5), %<variable>\n";

    private final ImmutableMap2<String, AttributesMatcher, Level>
            pathChassisToAttributeMatcherToLevel;
    private final ImmutableMap<String,String> variableToValue;

    public XCoverageLevel(
            ImmutableMap2<String, AttributesMatcher, Level> pathChassisToAttributeMatcherToLevel, ImmutableMap<String, String> variableToValue) {
        this.pathChassisToAttributeMatcherToLevel = pathChassisToAttributeMatcherToLevel;
        this.variableToValue = variableToValue;
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
        final Map<Integer, SetMatcher> attrNumToSetMatcher;

        private AttributesMatcher(ImmutableMap<Integer, SetMatcher> patterns) {
            this.attrNumToSetMatcher = patterns;
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
                result.append(" attr").append(entry.getKey()).append('=').append(entry.getValue().toString(valueToVariable)).append('\n');
            }
            return result.toString();
        }
    }

    static XCoverageLevel fromFile(Path filepath) {
        final Map2<String, AttributesMatcher, Level> pathChassisToAttributeMatcherToLevel =
                Map2.create(LinkedHashMap::new);

        Map<String, String> variableToValue = Maps.newTreeMap();

        String lastPath = null;
        Level lastLevel = null;
        Level nextLevel = null;
        AttributesMatcher.Builder amBuilder = new AttributesMatcher.Builder();
        try {
            int lineNumber = 0;
            for (String line : Files.readAllLines(filepath)) {
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
                        addPath(
                                pathChassisToAttributeMatcherToLevel,
                                lastPath,
                                nextLevel,
                                amBuilder);
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
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new XCoverageLevel(pathChassisToAttributeMatcherToLevel.createImmutable(), ImmutableMap.copyOf(variableToValue));
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
        if (DEBUG && (SHOW_ADD || TEST_PATHS.contains(lastPath))) {
            System.out.println("ADDING: " + lastPath + "\n\t" + lastLevel + "\t" + amBuilder);
        }
        pathChassisToAttributeMatcherToLevel.put(lastPath, amBuilder.build(), lastLevel);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        
        result.append("# DRAFT data for coverage. For the file format, see the readme.md in this directory.\n\n" +
            "# Variables\n\n");

        Map<String,String> valueToVariable = Maps.newTreeMap();
        
        for (Entry<String,String> entry : variableToValue.entrySet()) {
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

    private void getString(Entry<String, Map<AttributesMatcher, Level>> entry, Map<String, String> valueToVariable, StringBuilder result) {
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
}

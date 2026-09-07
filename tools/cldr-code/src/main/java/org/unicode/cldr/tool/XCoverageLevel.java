package org.unicode.cldr.tool;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
    final Variables variableToValue = new Variables();

    public XCoverageLevel(
            ImmutableMap2<String, AttributesMatcher, Level> pathChassisToAttributeMatcherToLevel) {
        this.pathChassisToAttributeMatcherToLevel = pathChassisToAttributeMatcherToLevel;
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

    static class SetMatcher {
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
            return (positive ? "∈" : "∉") + "[" + Joiners.COMMA.join(matches) + "]";
        }

        @Override
        public int hashCode() {
            return Objects.hash(positive, matches);
        }
    }

    static class AttributesMatcher {
        final Map<Integer, SetMatcher> patterns;

        private AttributesMatcher(ImmutableMap<Integer, SetMatcher> patterns) {
            this.patterns = patterns;
        }

        static class Builder {
            private final Map<Integer, SetMatcher> processedData = Maps.newTreeMap();

            public void add(int attributeNumber, boolean positive, String patternString) {
                processedData.put(attributeNumber, new SetMatcher(positive, patternString));
            }

            /**
             * Builds an AttributesMatcher. After building the builder is cleared, so that the
             * builder can be reused can be added
             */
            public AttributesMatcher build() {
                AttributesMatcher result =
                        new AttributesMatcher(ImmutableMap.copyOf(processedData));
                processedData.clear();
                return result;
            }

            @Override
            public String toString() {
                return processedData + "\n\t" + processedData;
            }

            public boolean isEmpty() {
                return processedData.isEmpty();
            }
        }

        boolean hasMatch(List<String> attributeValues) {
            return patterns.entrySet().stream()
                    .allMatch(
                            entry ->
                                    entry.getValue().contains(attributeValues.get(entry.getKey())));
            //            for (Entry<Integer, SetMatcher> entry : patterns.entrySet()) {
            //                if (!entry.getValue().contains(attributeValues.get(entry.getKey()))) {
            //                    return false;
            //                }
            //            }
            //            return true;
        }

        @Override
        public String toString() {
            return patterns.toString();
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
        return new XCoverageLevel(pathChassisToAttributeMatcherToLevel.createImmutable());
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
        for (Entry<String, Map<AttributesMatcher, Level>> entry :
                pathChassisToAttributeMatcherToLevel.getMapMap().entrySet()) {
            result.append(entry.getKey()).append("\n");
            for (Entry<AttributesMatcher, Level> entry2 : entry.getValue().entrySet()) {
                result.append("\t" + entry2.getKey() + "\t" + entry2.getValue() + "\n");
            }
        }
        return result.toString();
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

package org.unicode.cldr.tool;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.unicode.cldr.tool.GenerateXCoverage.Variables;
import org.unicode.cldr.tool.XCoverageLevel.AttributesMatcher.Builder;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.NestedMap.ImmutableMap2;
import org.unicode.cldr.util.NestedMap.Map2;
import org.unicode.cldr.util.Splitters;

class XCoverageLevel {
    public static final boolean DEBUG = false;
    public static Set<String> TEST_PATHS =
            ImmutableSet.of("//ldml/characters/exemplarCharacters[@type]");
    private static final boolean SHOW_ADD = false;

    private static final String BAD_LINE =
            "Lines must be of the form x=y where x is path, level, finalLevel, attrN (for N in 0..5)\n";

    private final ImmutableMap2<String, AttributesMatcher, Level>
            pathChassisToAttributeMatcherToLevel;
    final Variables variableToValue = new Variables();

    public XCoverageLevel(
            ImmutableMap2<String, AttributesMatcher, Level> pathChassisToAttributeMatcherToLevel) {
        this.pathChassisToAttributeMatcherToLevel = pathChassisToAttributeMatcherToLevel;
    }

    Level getCoverage(String path) {
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
                .UNDETERMINED; // we only need a finalLevel if the value is not comprehensive. But
        // for now we signal failures
    }

    static class AttributesMatcher {
        final ImmutableMultimap<Integer, String> patterns;

        static class Builder {
            private final Multimap<Integer, String> processedData = TreeMultimap.create();

            public void add(int attributeNumber, String patternString) {
                List<String> stringList = Splitters.COMMA.splitToList(patternString);
                processedData.putAll(attributeNumber, stringList);
            }

            /**
             * Builds an AttributesMatcher. After building the builder is cleared, so that the
             * builder can be reused can be added
             */
            public AttributesMatcher build() {
                AttributesMatcher result =
                        new AttributesMatcher(ImmutableMultimap.copyOf(processedData));
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

        private AttributesMatcher(ImmutableMultimap<Integer, String> patterns) {
            this.patterns = patterns;
        }

        boolean hasMatch(List<String> attributeValues) {
            for (Entry<Integer, Collection<String>> entry : patterns.asMap().entrySet()) {
                String attributeValue = attributeValues.get(entry.getKey());
                if (!entry.getValue().contains(attributeValue)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            return patterns.toString();
        }
    }

    static XCoverageLevel fromFile(Path filepath) {
        final Map2<String, AttributesMatcher, Level> pathChassisToAttributeMatcherToLevel =
                Map2.create(LinkedHashMap::new);

        final Variables variableToValue = new Variables();

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
                        if (lastLevel != null) {
                            if (lastLevel.compareTo(nextLevel) >= 0) {
                                throw new IllegalArgumentException(
                                        "Levels for a path must be strictly increasing: L"
                                                + lineNumber
                                                + ": "
                                                + line);
                            }
                            addPath(
                                    pathChassisToAttributeMatcherToLevel,
                                    lastPath,
                                    lastLevel,
                                    amBuilder);
                        }
                        lastLevel = nextLevel;
                        break;
                    case "or":
                        addPath(
                                pathChassisToAttributeMatcherToLevel,
                                lastPath,
                                lastLevel,
                                amBuilder);
                        break;
                    case "finalLevel":
                        if (lastPath != null && lastLevel != null) {
                            addPath(
                                    pathChassisToAttributeMatcherToLevel,
                                    lastPath,
                                    lastLevel,
                                    amBuilder);
                        }
                        lastLevel = Level.fromString(result);
                        addPath(
                                pathChassisToAttributeMatcherToLevel,
                                lastPath,
                                lastLevel,
                                amBuilder); // attributesMatchers is [] at this point
                        lastPath = null;
                        lastLevel = null;
                        break;
                    case "attr0":
                        addWithVariableReplacement(variableToValue, amBuilder, 0, result);
                        break;
                    case "attr1":
                        addWithVariableReplacement(variableToValue, amBuilder, 1, result);
                        break;
                    case "attr2":
                        addWithVariableReplacement(variableToValue, amBuilder, 2, result);
                        break;
                    case "attr3":
                        addWithVariableReplacement(variableToValue, amBuilder, 3, result);
                        break;
                    case "attr4":
                        addWithVariableReplacement(variableToValue, amBuilder, 4, result);
                        break;
                    case "attr5":
                        addWithVariableReplacement(variableToValue, amBuilder, 5, result);
                        break;
                    default:
                        if (type.startsWith("%")) {
                            variableToValue.add(type, result);
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
            Variables variableToValue, AttributesMatcher.Builder amBuilder, int i, String result) {
        String vresult = variableToValue.getValue(result);
        amBuilder.add(i, vresult == null ? result : vresult);
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

    public static <T> void setWithNullPadding(List<T> list, int index, T value) {
        if (index >= list.size()) {
            list.addAll(Collections.nCopies(index - list.size() + 1, null));
        }
        list.set(index, value);
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

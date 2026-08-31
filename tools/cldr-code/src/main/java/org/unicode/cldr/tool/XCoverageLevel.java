package org.unicode.cldr.tool;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.tool.InvestigateCoverage.Variables;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.NestedMap.ImmutableMap2;
import org.unicode.cldr.util.NestedMap.Map2;

class XCoverageLevel {
    public static final boolean DEBUG = false;
    public static Set<String> TEST_PATHS =
            ImmutableSet.of(
                    "/ldml/dates/calendars/calendar[@type]/dayPeriods/dayPeriodContext[@type]/dayPeriodWidth[@type]/dayPeriod[@type]");
    private static final boolean SHOW_ADD = false;

    private static final String BAD_LINE =
            "Lines must be of the form x=y where x is path, level, finalLevel, attrN (for N in 0..5)\n";

    private final ImmutableMap2<String, AttributesMatcher, Level>
            pathScaffoldToAttributeMatcherToLevel;
    final Variables variableToValue = new Variables();

    public XCoverageLevel(
            ImmutableMap2<String, AttributesMatcher, Level> pathScaffoldToAttributeMatcherToLevel) {
        this.pathScaffoldToAttributeMatcherToLevel = pathScaffoldToAttributeMatcherToLevel;
    }

    Level getCoverage(String path) {
        SplitPath splitPath = SplitPath.from(path);
        List<String> attributes = splitPath.getAttributeValues();
        String scaffold = splitPath.getChassis();
        if (DEBUG && TEST_PATHS.contains(scaffold)) {
            int debug = 0;
        }
        Map<AttributesMatcher, Level> matching =
                pathScaffoldToAttributeMatcherToLevel.getMap(scaffold);
        if (matching != null) {
            for (Entry<AttributesMatcher, Level> entry : matching.entrySet()) {
                AttributesMatcher key = entry.getKey();
                if (key == AttributesMatcher.EMPTY || key.hasMatch(attributes)) {
                    return entry.getValue();
                }
            }
        }
        return Level
                .UNDETERMINED; // we only need a finalLevel if the value is not comprehensive. But
        // for now we signal failures
    }

    static class AttributesMatcher {
        // Each sublist is a list of Patterns or null. Null means that attribute is not tested.
        final List<List<Pattern>> patterns;
        static final AttributesMatcher EMPTY = new AttributesMatcher(List.of());

        static class Builder {
            private final Map<Integer, Pattern> rawData = Maps.newLinkedHashMap();
            private final List<List<Pattern>> processedData = Lists.newArrayList();

            public void add(int attributeNumber, String patternString) {
                if (rawData.containsKey(attributeNumber)) {
                    // format data into a list, with nulls for unused attributes
                    addList();
                }
                rawData.put(attributeNumber, Pattern.compile(patternString));
            }

            /**
             * Builds an AttributesMatcher. After building the builder is cleared, so that new items
             * can be added
             */
            public AttributesMatcher build() {
                addList();
                AttributesMatcher result = new AttributesMatcher(List.copyOf(processedData));
                clear();
                return result;
            }

            public void clear() {
                rawData.clear();
                processedData.clear();
            }

            private void addList() {
                // flatten the rawData into a list, with nulls for missing intervening values
                List<Pattern> rawList = new ArrayList<>();
                for (Entry<Integer, Pattern> entry : rawData.entrySet()) {
                    setWithNullPadding(rawList, entry.getKey(), entry.getValue());
                }
                // now add to the processed data
                processedData.add(Collections.unmodifiableList(new ArrayList<>(rawList)));
                rawData.clear(); // get ready for next case
            }

            @Override
            public String toString() {
                return rawData + "\n\t" + processedData;
            }
        }

        private AttributesMatcher(List<List<Pattern>> patterns) {
            this.patterns = patterns;
        }

        boolean hasMatch(List<String> attributeValues) {
            main:
            for (List<Pattern> patternList : patterns) {
                for (int i = 0; i < patternList.size(); ++i) {
                    String attributeValue = attributeValues.get(i);
                    Pattern pattern = patternList.get(i);
                    if (pattern == null) { // null patterns are ignored
                        continue;
                    }
                    Matcher m = pattern.matcher(attributeValue);
                    if (!m.matches()) {
                        continue main;
                    }
                }
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return patterns.toString();
        }
    }

    static XCoverageLevel fromFile(Path filepath) {
        final Map2<String, AttributesMatcher, Level> pathScaffoldToAttributeMatcherToLevel =
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
                if (line.startsWith("#") || line.isBlank()) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq < 0) {
                    throw new IllegalArgumentException(BAD_LINE + " L" + lineNumber + ": " + line);
                }
                String type = line.substring(0, eq);
                String result = line.substring(eq + 1);
                if (DEBUG && TEST_PATHS.contains(lastPath)) {
                    int debug = 0;
                }
                switch (type) {
                    case "path":
                        if (lastPath != null && lastLevel != null) {
                            addPath(
                                    pathScaffoldToAttributeMatcherToLevel,
                                    lastPath,
                                    lastLevel,
                                    amBuilder.build());
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
                                    pathScaffoldToAttributeMatcherToLevel,
                                    lastPath,
                                    lastLevel,
                                    amBuilder.build());
                        }
                        lastLevel = nextLevel;
                        break;
                    case "finalLevel":
                        if (lastPath != null && lastLevel != null) {
                            addPath(
                                    pathScaffoldToAttributeMatcherToLevel,
                                    lastPath,
                                    lastLevel,
                                    amBuilder.build());
                        }
                        lastLevel = Level.fromString(result);
                        addPath(
                                pathScaffoldToAttributeMatcherToLevel,
                                lastPath,
                                lastLevel,
                                AttributesMatcher.EMPTY);
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
        return new XCoverageLevel(pathScaffoldToAttributeMatcherToLevel.createImmutable());
    }

    private static void addWithVariableReplacement(
            Variables variableToValue, AttributesMatcher.Builder amBuilder, int i, String result) {
        String vresult = variableToValue.getValue(result);
        amBuilder.add(i, vresult == null ? result : vresult);
    }

    private static void addPath(
            final Map2<String, AttributesMatcher, Level> pathScaffoldToAttributeMatcherToLevel,
            String lastPath,
            Level lastLevel,
            AttributesMatcher am) {
        if (DEBUG && (SHOW_ADD || TEST_PATHS.contains(lastPath))) {
            System.out.println("ADDING: " + lastPath + "\n\t" + lastLevel + "\t" + am);
        }
        pathScaffoldToAttributeMatcherToLevel.put(lastPath, am, lastLevel);
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
                pathScaffoldToAttributeMatcherToLevel.getMapMap().entrySet()) {
            result.append(entry.getKey()).append("\n");
            for (Entry<AttributesMatcher, Level> entry2 : entry.getValue().entrySet()) {
                result.append("\t" + entry2.getKey() + "\t" + entry2.getValue() + "\n");
            }
        }
        return result.toString();
    }

    public String getPathData(String scaffold) {
        Map<AttributesMatcher, Level> map = pathScaffoldToAttributeMatcherToLevel.getMap(scaffold);
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

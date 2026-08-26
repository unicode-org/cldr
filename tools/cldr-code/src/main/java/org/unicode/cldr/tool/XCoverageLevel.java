package org.unicode.cldr.tool;

import com.google.common.collect.ImmutableList;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.unicode.cldr.tool.InvestigateCoverage.Variables;
import org.unicode.cldr.util.Level;
import org.unicode.cldr.util.NestedMap.ImmutableMap2;
import org.unicode.cldr.util.NestedMap.Map2;
import org.unicode.cldr.util.NestedMap.Map3;
import org.unicode.cldr.util.NestedMap.Multimap2;

class XCoverageLevel {
    private static final boolean DEBUG = true;

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
        String scaffold = splitPath.getScaffold();
        Map<AttributesMatcher, Level> matching =
                pathScaffoldToAttributeMatcherToLevel.getMap(scaffold);
        if (matching == null) {
            System.out.println("Can't find " + scaffold);
        } else {
        for (Entry<AttributesMatcher, Level> entry : matching.entrySet()) {
            if (entry.getKey().hasMatch(attributes)) {
                return entry.getValue();
            }
        }
        }
        return Level.COMPREHENSIVE; // we only need a finalLevel if the value is not comprehensive
    }

    static class AttributesMatcher {
        // Each sublist is a list of Patterns or null. Null means that attribute is not tested.
        final List<List<Pattern>> patterns;

        static class Builder {
            private final Map<Integer, Pattern> rawData = Maps.newLinkedHashMap();
            private final List<List<Pattern>> processedData = Lists.newArrayList();
            private final List<Pattern> rawList = new ArrayList<>(Collections.nCopies(5, null));

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
                if (!rawData.isEmpty()) {
                    addList();
                }
                AttributesMatcher result = new AttributesMatcher(List.copyOf(processedData));
                clear();
                return result;
            }

            public void clear() {
                rawData.clear();
                processedData.clear();
                setElementsToNull(rawList);

            }

            private static void setElementsToNull(List<Pattern> list) {
                for (int i = 0; i < list.size(); ++i) {
                    list.set(i, null);
                }
            }

            private void addList() {
                setElementsToNull(rawList);
                for (int i = 0; i < rawList.size(); ++i) {
                    rawList.set(i, rawData.get(i));
                }
                rawData.clear();

                // now add to the processed data
                processedData.add(Collections.unmodifiableList(new ArrayList<>(rawList)));
            }

            public AttributesMatcher empty() {
                return new AttributesMatcher(List.of());
            }
            
            @Override
            public String toString() {
                return rawList + "\n\t" + rawData + "\n\t" + processedData;
                    
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
                switch (type) {
                    case "path":
                        if (lastPath!=null && lastLevel != null) {
                           addPath(pathScaffoldToAttributeMatcherToLevel, lastPath, lastLevel, amBuilder.build());
                        }
                        lastPath = result;
                        break;
                    case "level":
                        nextLevel = Level.fromString(result);
                        if (lastLevel != null) {
                            if (lastLevel.compareTo(nextLevel) >= 0 ) {
                                throw new IllegalArgumentException("Levels for a path must be strictly increasing: L" + lineNumber + ": " + line);
                            }
                            addPath(pathScaffoldToAttributeMatcherToLevel, lastPath, lastLevel, amBuilder.build());
                        }
                        lastLevel = nextLevel;
                        break;
                    case "finalLevel":
                        addPath(pathScaffoldToAttributeMatcherToLevel, lastPath, lastLevel, amBuilder.build());
                        lastLevel = Level.fromString(result);
                        pathScaffoldToAttributeMatcherToLevel.put(
                                lastPath, amBuilder.empty(), lastLevel);
                        lastPath = null;
                        lastLevel = null;
                        break;
                    case "attr0":
                        amBuilder.add(0, result);
                        break;
                    case "attr1":
                        amBuilder.add(1, result);
                        break;
                    case "attr2":
                        amBuilder.add(2, result);
                        break;
                    case "attr3":
                        amBuilder.add(3, result);
                        break;
                    case "attr4":
                        amBuilder.add(4, result);
                        break;
                    case "attr5":
                        amBuilder.add(5, result);
                        break;
                   default:
                        if (type.startsWith("%")) {
                            variableToValue.add(type, result);
                        } else {
                            throw new IllegalArgumentException(BAD_LINE + " L" + lineNumber + ": " + line);
                        }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new XCoverageLevel(pathScaffoldToAttributeMatcherToLevel.createImmutable());
    }

    private static void addPath(final Map2<String, AttributesMatcher, Level> pathScaffoldToAttributeMatcherToLevel, String lastPath, Level lastLevel,
        AttributesMatcher am) {
        if (DEBUG) {
            System.out.println(lastPath + "\n\t" + lastLevel + "\n\t" + am);
        }
        pathScaffoldToAttributeMatcherToLevel.put(
                    lastPath, am, lastLevel);
    }
}

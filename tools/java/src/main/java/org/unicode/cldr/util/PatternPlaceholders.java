package org.unicode.cldr.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.unicode.cldr.tool.GenerateXMB;
import org.unicode.cldr.util.RegexLookup.Merger;

import com.ibm.icu.text.Transform;

public class PatternPlaceholders {

    public enum PlaceholderStatus {
        DISALLOWED("No placeholders allowed."), OPTIONAL("Zero or one placeholder allowed."), MULTIPLE("Zero or more placeholders allowed."), LOCALE_DEPENDENT(
            "Varies by locale."), REQUIRED("Placeholder required");
        final String message;

        private PlaceholderStatus(String message) {
            this.message = message;
        }
    }

    private static class PlaceholderData {
        PlaceholderStatus status = PlaceholderStatus.REQUIRED;
        Map<String, PlaceholderInfo> data = new LinkedHashMap<String, PlaceholderInfo>();

        public void add(String id, String name, String example) {
            PlaceholderInfo row = new PlaceholderInfo(name, example);
            data.put(id, row);
        }
    }

    public static class PlaceholderInfo {
        public String name;
        public String example;

        private PlaceholderInfo(String name, String example) {
            this.name = name;
            this.example = example;
        }

        @Override
        public String toString() {
            return name + " (" + example + ")";
        }
    }

    private static final class MyMerger implements Merger<PlaceholderData> {
        @Override
        public PlaceholderData merge(PlaceholderData a, PlaceholderData into) {
            // check unique
            for (String key : a.data.keySet()) {
                if (into.data.containsKey(key)) {
                    throw new IllegalArgumentException("Duplicate placeholder: " + key);
                }
            }
            into.data.putAll(a.data);
            if (into.status != a.status) {
                throw new IllegalArgumentException("Different optional status");
            }
            return into;
        }
    }

    private static final class MapTransform implements Transform<String, PlaceholderData> {

        @Override
        public PlaceholderData transform(String source) {
            PlaceholderData result = new PlaceholderData();
            try {
                String[] parts = source.split("\\s*;\\s+");
                for (String part : parts) {
                    switch (part) {
                    case "optional":
                        result.status = PlaceholderStatus.OPTIONAL;
                        continue;
                    case "locale":
                        result.status = PlaceholderStatus.LOCALE_DEPENDENT;
                        continue;
                    case "multiple":
                        result.status = PlaceholderStatus.MULTIPLE;
                        continue;
                    default:
                        int equalsPos = part.indexOf('=');
                        String id = part.substring(0, equalsPos).trim();
                        String name = part.substring(equalsPos + 1).trim();
                        int spacePos = name.indexOf(' ');
                        String example;
                        if (spacePos >= 0) {
                            example = name.substring(spacePos + 1).trim();
                            name = name.substring(0, spacePos).trim();
                        } else {
                            example = "";
                        }

                        PlaceholderInfo old = result.data.get(id);
                        if (old != null) {
                            throw new IllegalArgumentException("Key occurs twice: " + id + "=" + old + "!=" + name);
                        }
                        result.add(id, name, example);
                    }
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse " + source, e);
            }
            for (Entry<String, PlaceholderInfo> entry : result.data.entrySet()) {
                if (GenerateXMB.DEBUG) System.out.println(entry);
            }
            return result;
        }

    }

    private RegexLookup<PlaceholderData> patternPlaceholders;

    private static PatternPlaceholders SINGLETON;

    private PatternPlaceholders() {
    }

    public static PatternPlaceholders getInstance() {
        if (SINGLETON == null) {
            SINGLETON = new PatternPlaceholders();
            SINGLETON.patternPlaceholders = RegexLookup.of(new MapTransform())
                .setValueMerger(new MyMerger())
                .loadFromFile(PatternPlaceholders.class, "data/Placeholders.txt");
        }
        return SINGLETON;
    }

    public Map<String, PlaceholderInfo> get(String path) {
        // TODO change the original map to be unmodifiable, to avoid this step. Need to add a "finalize" to the lookup.
        final PlaceholderData map = patternPlaceholders.get(path);
        return map == null ? null : Collections.unmodifiableMap(map.data);
    }

    public PlaceholderStatus getStatus(String path) {
        // TODO change the original map to be unmodifiable, to avoid this step. Need to add a "finalize" to the lookup.
        final PlaceholderData map = patternPlaceholders.get(path);
        return map == null ? PlaceholderStatus.DISALLOWED : map.status;
    }
}
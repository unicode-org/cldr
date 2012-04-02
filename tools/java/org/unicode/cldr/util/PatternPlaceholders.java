package org.unicode.cldr.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.unicode.cldr.tool.GenerateXMB;
import org.unicode.cldr.util.RegexLookup.Merger;

import com.ibm.icu.text.Transform;

public class PatternPlaceholders {
    private static final class MyMerger implements Merger<Map<String, String>> {
        @Override
        public Map<String, String> merge(Map<String, String> a, Map<String, String> into) {
            // check unique
            for (String key : a.keySet()) {
                if (into.containsKey(key)) {
                    throw new IllegalArgumentException("Duplicate placeholder: " + key);
                }
            }
            into.putAll(a);
            return into;
        }
    }

    private static final class MapTransform implements Transform<String, Map<String,String>> {

        @Override
        public Map<String, String> transform(String source) {
            Map<String, String> result = new LinkedHashMap<String, String>();
            try {
                String[] parts = source.split(";\\s+");
                for (String part : parts) {
                    int equalsPos = part.indexOf('=');
                    String id = part.substring(0, equalsPos).trim();
                    String name = part.substring(equalsPos+1).trim();
                    int spacePos = name.indexOf(' ');
                    String example;
                    if (spacePos >= 0) {
                        example = name.substring(spacePos+1).trim();
                        name = name.substring(0,spacePos).trim();
                    } else {
                        example = "";
                    }

                    String old = result.get(id);
                    if (old != null) {
                        throw new IllegalArgumentException("Key occurs twice: " + id + "=" + old + "!=" + name);
                    }
                    // <ph name='x'><ex>xxx</ex>yyy</ph>
                    result.put(id, "<ph name='" + name + "'><ex>" + example+ "</ex>" + id +  "</ph>");
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to parse " + source, e);
            }
            for (Entry<String, String> entry : result.entrySet()) {
                if (GenerateXMB.DEBUG) System.out.println(entry);
            }
            return result;
        }

    }

    private RegexLookup<Map<String, String>> patternPlaceholders 
    = RegexLookup.of(new MapTransform())
    .setValueMerger(new MyMerger())
    .loadFromFile(PatternPlaceholders.class, "data/Placeholders.txt");
    
    private static PatternPlaceholders SINGLETON = new PatternPlaceholders();
    private PatternPlaceholders() {}
    
    public static PatternPlaceholders getInstance() {
        return SINGLETON;
    }

    public Map<String, String> get(String path) {
        // TODO change the original map to be unmodifiable, to avoid this step. Need to add a "finalize" to the lookup.
        final Map<String, String> map = patternPlaceholders.get(path);
        return map == null ? null : Collections.unmodifiableMap(map);
    }
}
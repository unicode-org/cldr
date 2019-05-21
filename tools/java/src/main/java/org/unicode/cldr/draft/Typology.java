package org.unicode.cldr.draft;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.unicode.cldr.util.PatternCache;
import org.unicode.cldr.util.SemiFileReader;

import com.ibm.icu.dev.util.UnicodeMap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UProperty.NameChoice;
import com.ibm.icu.text.UnicodeSet;

public class Typology {
    private static final Set<String> NOLABELS = Collections.unmodifiableSet(new HashSet<String>(Arrays
        .asList("NOLABEL")));
    public static final UnicodeSet SKIP = new UnicodeSet("[[:C:]-[:Cf:]-[:Cc:]]").freeze();
    // static UnicodeMap<String> reasons = new UnicodeMap<String>();
    public static Map<String, UnicodeSet> label_to_uset = new TreeMap<String, UnicodeSet>();
    public static UnicodeMap<Set<String>> char2labels = new UnicodeMap<Set<String>>();
    // static {
    // label_to_uset.put("S", new UnicodeSet("[:S:]").freeze());
    // label_to_uset.put("L", new UnicodeSet("[:L:]").freeze());
    // label_to_uset.put("M", new UnicodeSet("[:M:]").freeze());
    // label_to_uset.put("N", new UnicodeSet("[:N:]").freeze());
    // label_to_uset.put("C", new UnicodeSet("[:C:]").freeze());
    // label_to_uset.put("Z", new UnicodeSet("[:Z:]").freeze());
    // label_to_uset.put("P", new UnicodeSet("[:P:]").freeze());
    // }
    static Set<String> skiplabels = new HashSet<String>(Arrays.asList("", "Symbol", "Punctuation", "Letter", "S", "L", "M",
        "N", "C", "Z", "P"));

    public static Map<String, UnicodeSet> full_path_to_uset = new TreeMap<String, UnicodeSet>();
    public static Map<String, UnicodeSet> path_to_uset = new TreeMap<String, UnicodeSet>();
    // static Map<List<String>,UnicodeSet> path_to_uset = new TreeMap<List<String>,UnicodeSet>();
    public static Relation<String, String> labelToPaths = Relation.of(new TreeMap<String, Set<String>>(), TreeSet.class);
    public static Map<String, Map<String, UnicodeSet>> label_parent_uset = new TreeMap<String, Map<String, UnicodeSet>>();

    // public static Relation<String, String> pathToList = new Relation(new TreeMap(), TreeSet.class);

    static class MyReader extends SemiFileReader {
        // 0000 Cc [Control] [X] [X] [X] <control>
        public final static Pattern SPLIT = PatternCache.get("\\s*\t\\s*");
        public final static Pattern NON_ALPHANUM = PatternCache.get("[^0-9A-Za-z]+");

        protected String[] splitLine(String line) {
            return SPLIT.split(line);
        }

        StringBuilder temp_path = new StringBuilder();

        @Override
        protected boolean handleLine(int lineCount, int startRaw, int endRaw, String[] items) {
            temp_path.setLength(0);
            temp_path.append('/');
            for (int i = 2; i < items.length - 1; ++i) {
                String item = items[i];
                // if (item.equals("[X]")) continue;

                // if (!item.startsWith("[") || !item.endsWith("]")) {
                // throw new IllegalArgumentException(i + "\t" + item);
                // }
                // item = item.substring(1, item.length()-1);
                if (item.length() == 0) continue;
                item = NON_ALPHANUM.matcher(item).replaceAll("_");
                temp_path.append('/').append(item);
            }
            String fullPath = temp_path.toString();

            // store
            {
                fullPath = fullPath.intern();
                UnicodeSet uset = full_path_to_uset.get(fullPath);
                if (uset == null) {
                    full_path_to_uset.put(fullPath, uset = new UnicodeSet());
                }
                uset.addAll(startRaw, endRaw);
            }

            String[] labels = fullPath.split("/");
            String path = "";
            Set<String> labelSet = new TreeSet<String>();
            for (String item : labels) {
                if (skiplabels.contains(item)) {
                    continue;
                }
                labelSet.add(item);
                UnicodeSet uset = label_to_uset.get(item);
                if (uset == null) {
                    label_to_uset.put(item, uset = new UnicodeSet());
                }
                uset.add(startRaw, endRaw);

                // labelToPath.put(item, path);

                path = (path + "/" + item).intern();

                uset = path_to_uset.get(path);
                if (uset == null) {
                    path_to_uset.put(path, uset = new UnicodeSet());
                }
                uset.addAll(startRaw, endRaw);
            }
            char2labels.putAll(startRaw, endRaw, Collections.unmodifiableSet(labelSet));
            return true;
        }

        Map<List<String>, List<String>> listCache = new HashMap<List<String>, List<String>>();
        Map<Set<String>, Set<String>> setCache = new HashMap<Set<String>, Set<String>>();

        private <T> T intern(Map<T, T> cache, T list) {
            T old = cache.get(list);
            if (old != null) return old;
            cache.put(list, list);
            return list;
        }

    }

    static {
        new MyReader().process(Typology.class, "Categories.txt"); // "09421-u52m09xxxx.txt"

        // fix the paths
        Map<String, UnicodeSet> temp = new TreeMap<String, UnicodeSet>();
        for (int i = 0; i < UCharacter.CHAR_CATEGORY_COUNT; ++i) {
            UnicodeSet same = new UnicodeSet()
                .applyIntPropertyValue(UProperty.GENERAL_CATEGORY, i);
            String gcName = UCharacter.getPropertyValueName(UProperty.GENERAL_CATEGORY, i, NameChoice.SHORT);
            // System.out.println("\n" + gcName);
            String prefix = gcName.substring(0, 1);

            for (String path : path_to_uset.keySet()) {
                UnicodeSet uset = path_to_uset.get(path);
                if (!same.containsSome(uset)) {
                    continue;
                }
                String path2 = prefix + path;
                temp.put(path2, new UnicodeSet(uset).retainAll(same));
                String[] labels = path2.split("/");
                String parent = "";
                for (int j = 0; j < labels.length; ++j) {
                    labelToPaths.put(labels[j], path2);
                    if (j == 0) {
                        continue;
                    }
                    Map<String, UnicodeSet> map = label_parent_uset.get(labels[j]);
                    if (map == null) {
                        label_parent_uset.put(labels[j], map = new TreeMap<String, UnicodeSet>());
                    }
                    UnicodeSet uset2 = map.get(parent);
                    if (uset2 == null) {
                        map.put(parent, uset2 = new UnicodeSet());
                    }
                    uset2.addAll(uset);
                    parent += labels[j] + "/";
                }
            }
        }
        // Set<String> labelUsetKeys = label_to_uset.keySet();
        // Set<String> labelToPathKeys = labelToPath.keySet();
        // if (!labelUsetKeys.equals(labelToPathKeys)) {
        // TreeSet<String> uset_path = new TreeSet<String>(labelUsetKeys);
        // uset_path.removeAll(labelToPathKeys);
        // System.out.println("\nuset - path labels\t" + uset_path);
        // TreeSet<String> path_uset = new TreeSet<String>(labelToPathKeys);
        // path_uset.removeAll(labelUsetKeys);
        // System.out.println("\npath -uset labels\t" + path_uset);
        // }
        UnicodeSet nolabels = char2labels.getSet(null);
        nolabels.removeAll(SKIP);
        char2labels.putAll(nolabels, NOLABELS);
        char2labels.freeze();
        label_to_uset.put(NOLABELS.iterator().next(), nolabels);
        label_to_uset = freezeMapping(label_to_uset);
        path_to_uset = freezeMapping(temp);
        labelToPaths.freeze();
        // invert
    }

    private static Map<String, UnicodeSet> freezeMapping(Map<String, UnicodeSet> map) {
        for (String key : map.keySet()) {
            UnicodeSet uset = map.get(key);
            uset.freeze();
        }
        return Collections.unmodifiableMap(map);
    }

    public static Set<String> addLabelsToOutput(UnicodeSet source, Set<String> output) {
        for (String it : source) {
            addLabelsToOutput(it, output);
        }
        return output;
    }

    public static Set<String> addLabelsToOutput(String s, Set<String> output) {
        // TODO
        Set<String> value = getLabels(s.codePointAt(0));
        output.addAll(value);
        return output;
    }

    public static Set<String> getLabels(int codepoint) {
        return char2labels.getValue(codepoint);
    }

    public static UnicodeSet getSet(String label) {
        return label_to_uset.get(label);
    }

    public static Set<String> getLabels() {
        return label_to_uset.keySet();
    }

    public static Set<Entry<String, UnicodeSet>> getLabelAndSet() {
        return label_to_uset.entrySet();
    }

    public static void main(String[] args) {
        for (Entry<String, UnicodeSet> s : getLabelAndSet()) {
            System.out.println(s.getKey() + "\t" + s.getValue().toPattern(false));
        }
    }
}

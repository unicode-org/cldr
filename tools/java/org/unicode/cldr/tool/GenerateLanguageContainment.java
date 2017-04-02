package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.Iso639Data;
import org.unicode.cldr.util.Iso639Data.Type;
import org.unicode.cldr.util.StandardCodes;
import org.unicode.cldr.util.StandardCodes.LstrField;
import org.unicode.cldr.util.StandardCodes.LstrType;
import org.unicode.cldr.util.Validity;
import org.unicode.cldr.util.Validity.Status;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.Row.R2;

public class GenerateLanguageContainment {
    private static final CLDRConfig CONFIG = CLDRConfig.getInstance();
    static final Splitter TAB = Splitter.on('\t').trimResults();
    static final CLDRFile ENGLISH = CONFIG.getEnglish();
    static final String relDir = "../util/data/languages/";
    static final Map<String, R2<List<String>, String>> ALIAS_MAP = CONFIG
        .getSupplementalDataInfo()
        .getLocaleAliasInfo()
        .get("language");
    static final Map<String, String> entityToLabel = loadTsvPairsUnique(GenerateLanguageContainment.class, relDir + "entityToLabel.tsv", 
        null, null, null);

    static final Function<String,String> NAME = code -> code.equals("mul") ? "root" : ENGLISH.getName(code) + " (" + code + ")";
    
    static final Map<String, String> entityToCode = loadTsvPairsUnique(GenerateLanguageContainment.class, relDir + "entityToCode.tsv",
        code -> {
            code = code.replace("\"","");
            R2<List<String>, String> v = ALIAS_MAP.get(code); 
            String result = v == null 
                ? code : 
                    v.get0().get(0);
            result = result.contains("_") 
                ? code 
                    : result;
            return result;
        },
        null, NAME);

    static final Multimap<String,String> codeToEntity = ImmutableMultimap.copyOf(
        Multimaps.invertFrom(Multimaps.forMap(entityToCode), LinkedHashMultimap.create()));

    static final Multimap<String, String> childToParent = loadTsvPairs(GenerateLanguageContainment.class, relDir + "childToParent.tsv", 
        code -> getEntityName(code), code -> getEntityName(code));

    static final Set<String> COLLECTIONS;
    static {
        Map<String, Map<LstrField, String>> languages = StandardCodes.getEnumLstreg().get(LstrType.language);
        Builder<String> _collections = ImmutableSet.<String>builder();
        for (Entry<String, Map<LstrField, String>> e : languages.entrySet()) {
            String scope = e.getValue().get(LstrField.Scope);
            if (scope != null
                && "Collection".equalsIgnoreCase(scope)) {
                _collections.add(e.getKey());
            }
        }
        COLLECTIONS = _collections.build();
    }

    static class Tree {
        Set<String> leaves = new LinkedHashSet<>();
        void add(List<String> chain) {
            Collections.reverse(chain);
        }
    }

    public static void main(String[] args) {
        Map<Status, Set<String>> table = Validity.getInstance().getStatusToCodes(LstrType.language);
        TreeMultimap<String, String> _parentToChild = TreeMultimap.create();
        TreeSet<String> missing = new TreeSet<>(table.get(Status.regular));
        _parentToChild.put("mul", "und");
        for (String code : table.get(Status.regular)) {
            Type type = Iso639Data.getType(code);
            if (type != Type.Living) {
                continue;
            }

//            if (COLLECTIONS.contains(code)) {
//                continue;
//            }
            Collection<String> entities = codeToEntity.get(code);
            if (entities.isEmpty()) {
                continue;
            }
            for (String entity : entities) {
                if (childToParent.get(entity).isEmpty()) {
                    continue;
                }
                List<String> chain = getAncestors(entity);
                String last = null;
                for (String link : chain) {
                    if (last != null) {
                        _parentToChild.put(link, last);
                    }
                    last = link;
                }
            }
        }
        Multimap<String, String> parentToChild = ImmutableMultimap.copyOf(_parentToChild);
        Writer out = new PrintWriter(System.out);
        print(out, parentToChild, new ArrayList<String>(Arrays.asList("mul")));
        System.out.println(out);
        System.out.println("DROPPED_PARENTS: ");
        for (Entry<String, String> e : DROPPED_PARENTS_TO_CHILDREN.entries()) {
            System.out.println(NAME.apply(e.getKey()) + "\t" + NAME.apply(e.getValue())
            );
        }


//        for (Entry<String,String> entry : childToParent.entries()) {
//            String childNames = getName(entityToCode, entityToLabel, entry.getKey());
//            String parentNames = getName(entityToCode, entityToLabel, entry.getValue());
//            System.out.println(entry.getKey() + "\t" + entry.getValue() + "\t" + childNames + "\t" + parentNames);
//        }
    }

    private static void print(Writer out, Multimap<String, String> parentToChild, List<String> line) {
        String current = line.get(line.size()-1);
        Collection<String> children = parentToChild.get(current);
        if (children.isEmpty()) {
            try {
                String sep = "";
                for (String item : line) {
                    out.append(sep).append(NAME.apply(item));
                    sep = " > ";
                }
                out.append('\n');
                out.flush();
            } catch (IOException e) {}
        } else {
            for (String child : children) {
                line.add(child);
                print(out, parentToChild, line);
                line.remove(line.size()-1);
            }
        }
    }

    static final Multimap<String,String> DROPPED_PARENTS_TO_CHILDREN = TreeMultimap.create();

    private static List<String> getAncestors(String leaf) {
        List<String> chain = new ArrayList<>();
        while (true) {
            String code = entityToCode.get(leaf);
            if (code != null) {
                chain.add(code);
            }
            Collection<String> parents = childToParent.get(leaf);
            if (parents.isEmpty()) {
                // clean up duplicates
                chain = new ArrayList<>(new LinkedHashSet<>(chain));
                // wikipedia has non-collections as parents. Remove those if they are not first.
                String last = chain.get(0);
                for (int i = 1; i < chain.size(); ++i) {
                    String item = chain.get(i); 
                    if (!COLLECTIONS.contains(item)) {
                        chain.set(i, item.equals("zh") ? "zhx" : ""); 
                        DROPPED_PARENTS_TO_CHILDREN.put(item, last);
                    } else {
                        last = item;
                    }
                }
                chain.removeIf(x -> x.isEmpty());
                if ("zh".equals(chain.get(0))) {
                    chain.add(1,"zhx");
                }
                last = chain.get(chain.size()-1);
                if (!"mul".equals(last)) {
                    chain.add("mul"); // make sure we have root.
                }
                if (chain.size() == 2) {
                    chain.add(1,"und");
                }
                return chain;
            }
            leaf = getBest(parents);
        }
    }

    private static String getBest(Collection<String> parents) {
        for (String parent : parents) {
            String code = entityToCode.get(parent);
            if (code == null) continue;
            Type type = Iso639Data.getType(code);
            if (type != Type.Living) {
                continue;
            }
            return parent;
        }
        // failed
        return parents.iterator().next();
    }

    private static String getEntityName(String key) {
        String code = entityToCode.get(key);
        if (code != null) {
            try {
                String name = NAME.apply(code);
                if (name != null) {
                    return name;
                }
            } catch (Exception e) {}
        }
        String name = entityToLabel.get(key); 
        if (name != null) {
            return name;
        }
        int last = key.lastIndexOf('/');
        return key.substring(last+1, key.length()-1);
    }

    private static Multimap<String, String> loadTsvPairs(Class<?> class1, String file, 
        Function<String, String> keyMapper, Function<String, String> valueMapper) {
        String rel = FileUtilities.getRelativeFileName(class1, file);
        System.out.println(rel);
        ImmutableMultimap.Builder<String, String> _keyToValues = ImmutableMultimap.builder();
        for (String line : FileUtilities.in(class1, file)) {
            if (line.startsWith("?") || line.isEmpty()) continue;
            List<String> parts = TAB.splitToList(line);
            String key = parts.get(0);
            String value = parts.get(1);
            _keyToValues.put(key, value);
        }
        ImmutableMultimap<String, String> result = _keyToValues.build();
        showDups(result, keyMapper, valueMapper);
        return result;
    }

    private static Map<String, String> loadTsvPairsUnique(Class<?> class1, String file, 
        Function <String, String> fixValue, 
        Function <String, String> keyMapper, Function <String, String> valueMapper) {
        String rel = FileUtilities.getRelativeFileName(class1, file);
        System.out.println(rel);
        Map<String, String> _keyToValue = new TreeMap<>();
        Multimap<String, String> _keyToValues = TreeMultimap.create();
        for (String line : FileUtilities.in(class1, file)) {
            if (line.startsWith("?") || line.isEmpty()) continue;
            List<String> parts = TAB.splitToList(line);
            String key = parts.get(0);
            String value = parts.get(1);
            if (fixValue != null) {
                value = fixValue.apply(value);
            }
            _keyToValues.put(key, value);
            _keyToValue.put(key, value);
        }
        showDups(_keyToValues, keyMapper, valueMapper);
        return ImmutableMap.copyOf(_keyToValue);
    }

    private static void showDups(Multimap<String, String> _keyToValues, 
        Function<String, String> keyMapper, Function<String, String> valueMapper) {
        for (Entry<String, Collection<String>> entry : _keyToValues.asMap().entrySet()) {
            Collection<String> valueSet = entry.getValue();
            if (valueSet.size() > 1) {
                String key = entry.getKey();
                key = keyMapper == null ? key : keyMapper.apply(key);
                if (valueMapper != null) {
                    Set<String> result = new LinkedHashSet<>();
                    valueSet.stream().map(valueMapper).forEach(x -> result.add(x));
                    valueSet = result;
                }
                System.out.println("Multiple values: " + key + "\t" + valueSet);
            }
        }
    }
}

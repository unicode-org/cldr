package org.unicode.cldr.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;

import org.unicode.cldr.draft.FileUtilities;
import org.unicode.cldr.util.CLDRConfig;
import org.unicode.cldr.util.CLDRFile;
import org.unicode.cldr.util.CLDRPaths;
import org.unicode.cldr.util.Containment;
import org.unicode.cldr.util.DtdType;
import org.unicode.cldr.util.Iso639Data;
import org.unicode.cldr.util.Iso639Data.Type;
import org.unicode.cldr.util.SimpleXMLSource;
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
import com.ibm.icu.dev.util.CollectionUtilities;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.util.ICUUncheckedIOException;

public class GenerateLanguageContainment {
    private static final boolean ONLY_LIVING = false;
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

    static final Function<String, String> NAME = code -> code.equals("mul") ? "root" : ENGLISH.getName(code) + " (" + code + ")";

    static final Map<String, String> entityToCode = loadTsvPairsUnique(GenerateLanguageContainment.class, relDir + "entityToCode.tsv",
        code -> {
            code = code.replace("\"", "");
            R2<List<String>, String> v = ALIAS_MAP.get(code);
            String result = v == null
                ? code : v.get0().get(0);
            result = result.contains("_")
                ? code
                : result;
            return result;
        },
        null, NAME);

    static final Multimap<String, String> codeToEntity = ImmutableMultimap.copyOf(
        Multimaps.invertFrom(Multimaps.forMap(entityToCode), LinkedHashMultimap.create()));

    static final Multimap<String, String> childToParent = loadTsvPairs(GenerateLanguageContainment.class, relDir + "childToParent.tsv",
        code -> getEntityName(code), code -> getEntityName(code));

    static final Set<String> COLLECTIONS;
    static {
        Map<String, Map<LstrField, String>> languages = StandardCodes.getEnumLstreg().get(LstrType.language);
        Builder<String> _collections = ImmutableSet.<String> builder();
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

    static final Multimap<String, String> EXTRA_PARENT_CHILDREN = ImmutableMultimap.<String, String> builder()
        .put("mul", "art") // we add art programmatically
        .put("gmw", "ksh")
        .put("gmw", "wae")
        .put("mul", "tai")
        .put("tai", "th")
        .put("tai", "lo")
        .put("roa", "cpf")
        .put("roa", "cpp")
        .put("ber", "zgh")
        .put("sdv", "saq")
        .put("sw", "swc")
        .put("alv", "agq")
        .put("bnt", "asa")
        .put("bnt", "bez")
        .put("bnt", "cgg")
        .put("bnt", "ebu")
        .put("bnt", "ksb")
        .put("bnt", "lag")
        .put("bnt", "rof")
        .put("bnt", "sbp")
        .put("ngb", "sg")
        .put("alv", "ngb")
        .put("bnt", "jmc")
        .put("bnt", "mer")
        .put("bnt", "mgh")
        .put("bnt", "nmg")
        .put("bnt", "rwk")
        .put("bnt", "seh")
        .put("bnt", "vun")
        .put("bnt", "xog")
        .put("alv", "yav")
        .put("son", "khq")
        .put("euq", "eu")
        .put("mul", "euq")
        .put("mul", "jpx")
        .put("jpx", "ja")
        .put("ira", "lrc")
        .put("grk", "el")
        .put("grk", "grc")
        .put("grk", "gmy")
        .build();

    static final Multimap<String, String> REMOVE_PARENT_CHILDREN = ImmutableMultimap.<String, String> builder()
        .put("mul", "und") // anomaly
        .put("mul", "crp")
        .put("crp", "*") // general Creole group interferes with French/Spanish/... language grouping
        .put("sit", "zh") // other cases where we have to remove items we add in different place above.
        .put("inc", "rmg")
        .put("sla", "cu")
        .put("ine", "gmy")
        .put("ine", "el")
        .put("ine", "grc")
        .build();

    public static void main(String[] args) {
        if (true) {
            // check on items
            for (String check : Arrays.asList("sw", "km", "ksh", "wae", "kea", "mfe", "th", "lo")) {
                System.out.println("Checking " + ENGLISH.getName(check) + "[" + check + "]");
                Collection<String> entities = codeToEntity.get(check);
                if (entities.isEmpty()) {
                    System.out.println("no code for " + check + ": " + entities);
                    continue;
                }
                for (String entity : entities) {
                    Set<List<String>> ancestors = getAllAncestors(entity);
                    showEntityLists(entity + " parents ", ancestors);
                    System.out.println();
                }
            }
        }

        Map<Status, Set<String>> table = Validity.getInstance().getStatusToCodes(LstrType.language);
        TreeMultimap<String, String> _parentToChild = TreeMultimap.create();
        TreeSet<String> missing = new TreeSet<>(table.get(Status.regular));
        _parentToChild.put("mul", "und");
        for (String code : table.get(Status.regular)) {
            if (ONLY_LIVING) {
                Type type = Iso639Data.getType(code);
                if (type != Type.Living) {
                    continue;
                }
            }
            if (code.compareTo("hdz") > 0) {
                int debug = 0;
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
                Set<Set<String>> chains = getAncestors(entity);
                if (chains.size() > 1) {
                    int debug = 0;
                }
                for (Set<String> chain : chains) {
                    String last = null;
                    for (String link : chain) {
                        if (last != null) {
                            _parentToChild.put(link, last);
                        }
                        last = link;
                    }
                }
            }
        }

        for (Entry<String, Collection<String>> entity : REMOVE_PARENT_CHILDREN.asMap().entrySet()) {
            String key = entity.getKey();
            for (String value : entity.getValue()) {
                if (value.equals("*")) {
                    _parentToChild.removeAll(key);
                } else {
                    _parentToChild.remove(key, value);
                }
            }
        }

        _parentToChild.putAll(EXTRA_PARENT_CHILDREN);

        // special code for artificial
        for (String code : Iso639Data.getAvailable()) {
            Type type = Iso639Data.getType(code);
            if (type == Type.Constructed) {
                _parentToChild.put("art", code);
            }
        }

        Multimap<String, String> parentToChild = ImmutableMultimap.copyOf(_parentToChild);
        Multimap<String, String> childToParent = ImmutableMultimap.copyOf(Multimaps.invertFrom(parentToChild, TreeMultimap.create()));
        System.out.println("Checking " + "he" + "\t" + Containment.getAllDirected(childToParent, "he"));

        PrintWriter out = new PrintWriter(System.out);
        print(out, parentToChild, new ArrayList<String>(Arrays.asList("mul")));
        System.out.println(out);
        SimpleXMLSource xmlSource = new SimpleXMLSource("languageGroup");
        xmlSource.setNonInheriting(true); // should be gotten from DtdType...
        CLDRFile newFile = new CLDRFile(xmlSource);
        newFile.setDtdType(DtdType.supplementalData);
        newFile.add("//" + DtdType.supplementalData + "/version[@number='$Revision$']", "");
        printXML(newFile, parentToChild);

        try (PrintWriter outFile = FileUtilities.openUTF8Writer(CLDRPaths.SUPPLEMENTAL_DIRECTORY, "languageGroup.xml")) {
            newFile.write(outFile);
        } catch (IOException e1) {
            throw new ICUUncheckedIOException("Can't write to languageGroup.xml", e1);
        }

//        for (Entry<String,String> entry : childToParent.entries()) {
//            String childNames = getName(entityToCode, entityToLabel, entry.getKey());
//            String parentNames = getName(entityToCode, entityToLabel, entry.getValue());
//            System.out.println(entry.getKey() + "\t" + entry.getValue() + "\t" + childNames + "\t" + parentNames);
//        }
    }

    private static void showEntityLists(String title, Set<List<String>> ancestors) {
        ancestors.forEach(new Consumer<List<String>>() {
            @Override
            public void accept(List<String> item) {
                item.forEach(new Consumer<String>() {
                    @Override
                    public void accept(String t) {
                        System.out.println(t + "\t" + entityToCode.get(t) + "\t" + entityToLabel.get(t));
                    }
                });
                System.out.println();
            }
        });
    }

    private static void printXML(CLDRFile newFile, Multimap<String, String> parentToChild) {
        printXML(newFile, parentToChild, "mul");
    }

    private static void printXML(CLDRFile newFile, Multimap<String, String> parentToChild, String base) {
        Collection<String> children = parentToChild.get(base);
        if (children.isEmpty()) {
            return;
        }
        if (base.equals("und")) {
            // skip, no good info
        } else {
            newFile.add("//" + DtdType.supplementalData + "/languageGroups/languageGroup[@parent=\"" + base + "\"]", CollectionUtilities.join(children, " "));
//            System.out.println("\t<languageGroup parent='" 
//                + base + "'>" 
//                + CollectionUtilities.join(children, " ") + "</languageGroup>");
        }
        for (String child : children) {
            printXML(newFile, parentToChild, child);
        }
    }

    private static void print(Writer out, Multimap<String, String> parentToChild, List<String> line) {
        String current = line.get(line.size() - 1);
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
            } catch (IOException e) {
            }
        } else {
            for (String child : children) {
                line.add(child);
                print(out, parentToChild, line);
                line.remove(line.size() - 1);
            }
        }
    }

    private static Set<Set<String>> getAncestors(String leaf) {
        Set<List<String>> items = Containment.getAllDirected(childToParent, leaf);
        Set<Set<String>> itemsFixed = new LinkedHashSet<>();
        main: for (List<String> item : items) {
            Set<String> chain = new LinkedHashSet<>();
            for (String id : item) {
                String code = entityToCode.get(id);
                if (code == null) {
                    continue;
                }

                // skip leaf nodes after the first

                if (!chain.isEmpty() && !COLLECTIONS.contains(code)) {
                    if (code.equals("zh")) {
                        code = "zhx"; // rewrite collections usage
                    } else {
                        log("Skipping inheritance from\t" + chain + "\t" + code + "\tfrom\t" + items);
                        continue;
                    }
                }

                // check for cycle, and skip if we have one

                boolean changed = chain.add(code);
                if (!changed) {
                    log("Cycle in\t" + chain + "\tfrom\t" + items);
                    continue main;
                }
            }
            if (chain.size() > 1) {
                chain.add("mul"); // root
                itemsFixed.add(chain);
            }
        }
        // remove subsets
        // eg [[smp, he, mul], [smp, he, sem, afa, mul]]
        // => [[smp, he, sem, afa, mul]]
        if (itemsFixed.size() > 1) {
            Set<Set<String>> removals = new HashSet<>();
            for (Set<String> chain1 : itemsFixed) {
                for (Set<String> chain2 : itemsFixed) {
                    if (chain1.containsAll(chain2) && !chain2.containsAll(chain1)) {
                        removals.add(chain2);
                    }
                }
            }
            itemsFixed.removeAll(removals);
        }
        return itemsFixed;
//        while (true) {
//            String code = entityToCode.get(leaf);
//            if (code != null) {
//                chain.add(code);
//            }
//            Collection<String> parents = childToParent.get(leaf);
//            if (parents.isEmpty()) {
//                // clean up duplicates
//                chain = new ArrayList<>(new LinkedHashSet<>(chain));
//                // wikipedia has non-collections as parents. Remove those if they are not first.
//                break;
//            }
//            leaf = getBest(parents);
//        }
//        String last = chain.get(0);
//        for (int i = 1; i < chain.size(); ++i) {
//            String item = chain.get(i); 
//            if (!COLLECTIONS.contains(item)) {
//                chain.set(i, item.equals("zh") ? "zhx" : ""); 
//                DROPPED_PARENTS_TO_CHILDREN.put(item, last);
//            } else {
//                last = item;
//            }
//        }
//        chain.removeIf(x -> x.isEmpty());
//        if ("zh".equals(chain.get(0))) {
//            chain.add(1,"zhx");
//        }
//        last = chain.get(chain.size()-1);
//        if (!"mul".equals(last)) {
//            chain.add("mul"); // make sure we have root.
//        }
//        if (chain.size() == 2) {
//            chain.add(1,"und");
//        }
//        return chain;
    }

    private static void log(String string) {
        System.out.println(string);
//        for (Entry<String, String> e : DROPPED_PARENTS_TO_CHILDREN.entries()) {
//            System.out.println(NAME.apply(e.getKey()) + "\t" + NAME.apply(e.getValue())
//                );
//        }
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
            } catch (Exception e) {
            }
        }
        String name = entityToLabel.get(key);
        if (name != null) {
            return name;
        }
        int last = key.lastIndexOf('/');
        return key.substring(last + 1, key.length() - 1);
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
        showDups(file, result, keyMapper, valueMapper);
        return result;
    }

    private static Map<String, String> loadTsvPairsUnique(Class<?> class1, String file,
        Function<String, String> fixValue,
        Function<String, String> keyMapper, Function<String, String> valueMapper) {
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
            String oldValue = _keyToValue.get(key);
            if (oldValue == null || oldValue.equals("kxm")) {
                _keyToValue.put(key, value);
            }
        }
        _keyToValue = ImmutableMap.copyOf(_keyToValue);
        showDups(file, _keyToValues, keyMapper, valueMapper);
        return _keyToValue;
    }

    private static void showDups(String file, Multimap<String, String> _keyToValues,
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
                log(file + "\tMultiple values: " + key + "\t" + valueSet);
            }
        }
    }

    static Set<List<String>> getAllAncestors(String lang) {
        return Containment.getAllDirected(childToParent, lang);
    }
}

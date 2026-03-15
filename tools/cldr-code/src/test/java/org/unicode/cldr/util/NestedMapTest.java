package org.unicode.cldr.util;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.collect.TreeMultimap;
import com.ibm.icu.impl.Relation;
import com.ibm.icu.impl.Row.R2;
import com.ibm.icu.util.OutputInt;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.unicode.cldr.icu.dev.test.TestFmwk;
import org.unicode.cldr.util.NestedMap.Entry3;
import org.unicode.cldr.util.NestedMap.Entry4;
import org.unicode.cldr.util.NestedMap.ImmutableMap2;
import org.unicode.cldr.util.NestedMap.ImmutableMap3;
import org.unicode.cldr.util.NestedMap.Map2;
import org.unicode.cldr.util.NestedMap.Map3;
import org.unicode.cldr.util.NestedMap.Multimap2;
import org.unicode.cldr.util.StandardCodes.LstrType;

public class NestedMapTest extends TestFmwk {
    private static final CLDRConfig CONFIG = CLDRConfig.getInstance();
    private static final CLDRFile ENGLISH = CONFIG.getEnglish();
    private static final NameGetter NAME_GETTER = new NameGetter(ENGLISH);
    private static final SupplementalDataInfo SDI = SupplementalDataInfo.getInstance();
    private static final boolean SHOW = false;
    private static final boolean SKIP = true;

    public enum MapType {
        Tree(TreeMap::new),
        Hash(HashMap::new),
        LinkedHash(LinkedHashMap::new),
        ConcurrentHash(ConcurrentHashMap::new);

        private final Supplier<Map<Object, Object>> supplier;

        private MapType(Supplier<Map<Object, Object>> x) {
            this.supplier = x;
        }
    }

    public enum Mutability {
        mutable,
        immutable
    }

    public enum Order {
        az,
        za,
        na
    }

    public enum Reason {
        overlong,
        deprecated,
        macrolanguage,
        bibliographic,
        legacy
    }

    public static void main(String[] args) {
        new NestedMapTest().run(args);
    }

    @SuppressWarnings("deprecation")
    public void testAM2() {
        Map2<String, String, Integer> map2 = Map2.create(LinkedHashMap::new, TreeMap::new);
        List<List<String>> tests =
                List.of(
                        List.of("South America", "Widget", "1"),
                        List.of("South America", "Widget2", "2"),
                        List.of("South America", "Widget3", "3"));

        // load and check size
        tests.stream().forEach(x -> map2.put(x.get(0), x.get(1), Integer.valueOf(x.get(2))));
        assertEquals("size: " + map2, tests.size(), map2.size());

        //        List<String> first = tests.get(0);
        //        map2.remove(first.get(0), first.get(1), Integer.valueOf(first.get(2))); // should
        // have no effect
        //        assertEquals("size: " + map2, tests.size()-1, map2.size());

        // verify gets

        tests.stream()
                .forEach(
                        x -> {
                            String key1 = x.get(0);
                            String key2 = x.get(1);
                            String value = x.get(2);
                            Integer actual = map2.get(key1, key2);
                            assertEquals("get" + map2, Integer.valueOf(value), actual);

                            Set<String> expected1 =
                                    tests.stream().map(y -> y.get(0)).collect(Collectors.toSet());
                            Set<String> actual1 = map2.keySet();
                            assertEquals("keySet" + map2, expected1, actual1);

                            Set<String> expected2 =
                                    tests.stream()
                                            .filter(
                                                    y -> {
                                                        return y.get(0).equals(key1);
                                                    })
                                            .map(y -> y.get(1))
                                            .collect(Collectors.toSet());
                            Set<String> actual2 = map2.keySet2(key1);
                            assertEquals("keySet2" + map2, expected2, actual2);
                        });

        // verify stream
        map2.stream()
                .forEach(
                        x -> {
                            Integer expected = map2.get(x.getKey1(), x.getKey2());
                            assertEquals("get" + map2, expected, x.getValue());
                        });

        // verify immutable
        ImmutableMap2<String, String, Integer> iMap2 = map2.createImmutable();
        assertEquals("immutable equal: " + map2, map2, iMap2);

        map2.stream()
                .forEach(
                        x -> {
                            try {
                                iMap2.put(x.getKey1(), x.getKey2(), -2);
                                errln("Immutable should disallow put");
                            } catch (Exception e) {
                            }
                            try {
                                iMap2.remove(x.getKey1(), x.getKey2(), -2);
                                errln("Immutable should disallow remove");
                            } catch (Exception e) {
                            }
                            Integer expected = map2.get(x.getKey1(), x.getKey2());
                            assertEquals(
                                    "get" + map2, expected, iMap2.get(x.getKey1(), x.getKey2()));
                        });

        // verify remove
        OutputInt count = new OutputInt();
        iMap2.stream()
                .forEach(
                        x -> {
                            count.value = map2.size();
                            String k1 = x.getKey1();
                            String k2 = x.getKey2();
                            map2.remove(k1, k2, -1); // should have no effect
                            assertEquals(
                                    "A. remove: " + k1 + "," + k2 + ",-1: " + map2,
                                    count.value,
                                    map2.size());
                            count.value--;
                            Integer v = x.getValue();
                            map2.remove(k1, k2, v); // should remove 1
                            assertEquals(
                                    "B. remove: " + k1 + "," + k2 + "," + v + ": " + map2,
                                    count.value,
                                    map2.size());

                            map2.put(k1, k2, v); // restore from copy
                            assertEquals("C. restore size: " + map2, count.value + 1, map2.size());
                            map2.removeAll(k1); // should remove all
                            assertEquals("D. remove: " + k1 + ": " + map2, 0, map2.size());
                            // restore all
                            map2.putAll(iMap2);
                            assertEquals("C. restore size: " + map2, tests.size(), map2.size());
                        });
    }

    public void testAM3() {
        Map3<String, String, String, Integer> map3 = Map3.create(LinkedHashMap::new);
        Integer expected3 = 3;
        map3.put("South America", "Widget", "Junk", expected3);
        Integer actual3 = map3.get("South America", "Widget", "Junk");
        assertEquals("get", expected3, actual3);
        assertEquals("size", 1, map3.size());

        map3.removeAll("South America", "Widget", "Junk");
        assertEquals("size", 0, map3.size());
    }

    public void testAMM2() {
        Multimap2<String, String, String> multimap2 = Multimap2.create(LinkedHashMap::new);
        String expected = "Junk";
        multimap2.put("South America", "Widget", expected);
        multimap2.put("South America", "Widget", "Junk1");
        multimap2.put("South America", "Widget", "Junk2");
        multimap2.put("South America", "Widget2", "Junk");
        assertEquals("size", 4, multimap2.size());

        Set<String> actualMM = multimap2.get("South America", "Widget");
        assertEquals("get", Set.of("Junk", "Junk1", "Junk2"), actualMM);

        multimap2.remove("South America", "Widget", "Junk");
        assertEquals("size", 3, multimap2.size());

        multimap2.removeAll("South America", "Widget");
        assertEquals("size", 1, multimap2.size());
    }

    public void testBasic() {
        List<String> key1s = List.of("b", "a", "b");
        List<Double> key2s = List.of(993.2d, 5.3d, 99d);
        Boolean v1 = true;
        List<String> actualParts = List.of("[b, 993.2, true]", "[a, 5.3, true]", "[b, 99.0, true]");
        // tree order is 1,2,0

        Map<Supplier<Map<Object, Object>>, List<Integer>> supplierAndOrders =
                ImmutableMap.of(
                        TreeMap::new,
                        List.of(1, 2, 0),
                        //
                        LinkedHashMap::new,
                        List.of(0, 2, 1), // keys are ordered together
                        //
                        HashMap::new,
                        List.of(1, 0, 2),
                        //
                        ConcurrentHashMap::new,
                        List.of(1, 0, 2));
        for (Entry<Supplier<Map<Object, Object>>, List<Integer>> supplierAndOrder :
                supplierAndOrders.entrySet()) {
            Supplier<Map<Object, Object>> supplier = supplierAndOrder.getKey();
            List<Integer> order = supplierAndOrder.getValue();

            Map2<String, Double, Boolean> nm2 = Map2.create(supplier);

            for (int i = 0; i < key1s.size(); ++i) {
                String key1 = key1s.get(i);
                Double key2 = key2s.get(i);
                String stringified = join(i, actualParts, order);
                nm2.put(key1, key2, v1);
                Boolean result = nm2.get(key1, key2);
                assertEquals(String.valueOf(i), v1, result);

                String actual = Joiners.VBAR.join(nm2.stream().collect(Collectors.toList()));
                assertEquals(String.valueOf(i) + " " + order, stringified, actual);
            }

            ImmutableMap2<String, Double, Boolean> nm2_immutable = nm2.createImmutable();

            nm2.equals(nm2_immutable);
            if (!assertEquals("immutable equals?", nm2, nm2_immutable)) {}
        }
    }

    public void testUsageExample() {
        Map3<String, String, Integer, Double> salesData = Map3.create(LinkedHashMap::new);

        salesData.put("South America", "Widget", 2024, 150000.75);
        Entry4<String, String, Integer, Double> first = salesData.stream().findFirst().get();

        salesData.put("North America", "Thangs", 2023, 314159.75);

        ImmutableMap3<String, String, Integer, Double> salesDataIM = salesData.createImmutable();

        Double value = salesDataIM.get(first.getKey1(), first.getKey2(), first.getKey3());

        List<Entry4<String, String, Integer, Double>> contents =
                salesDataIM.stream().collect(Collectors.toList());

        assertEquals("Test immutability", salesData, salesDataIM);
        assertEquals("Test ordering with LinkedHashMap", value, first.getValue());
        Entry4<String, String, Integer, Double> contentFirst = contents.get(0);
        assertEquals("Test ordering with stream", contentFirst, first);
    }

    private String join(int i, List<String> actualParts, List<Integer> order) {
        List<String> result = new ArrayList<>();
        for (int j : order) {
            if (j > i) continue;
            result.add(actualParts.get(j));
        }
        return Joiners.VBAR.join(result);
    }

    public void testCore() {
        // Create a 3-level map with type-safety:
        // Level 1: String (Region)
        // Level 2: String (Product)
        // Level 3: Integer (Year)
        // Value: Double (Sales Amount)
        for (MapType tt : MapType.values()) {
            Map3<String, String, Integer, Double> salesData = Map3.create(tt.supplier);

            // --- Type-Safe PUT operations ---
            // We make sure that the items are in reverse order
            // That way the linked-hash map is easily testable
            salesData.put("South America", "Widget", 2024, 150000.75);
            salesData.put("South America", "Widget", 2020, 0d);
            salesData.put("South Africa", "Gadget", 2025, 120000.00);
            salesData.put("North Africa", "Widget", 2025, 175000.00);
            salesData.put("Europe", "Gadget", 2025, 95000.50);
            salesData.put("Asia", "Gadget3", 2023, 666.00);

            String streamTest = tryOperations(tt, Mutability.mutable, salesData, null);

            // Immutable
            ImmutableMap3<String, String, Integer, Double> salesDataIM =
                    salesData.createImmutable();
            tryOperations(tt, Mutability.immutable, salesDataIM, streamTest);

            List<Entry4<String, String, Integer, Double>> contents =
                    salesData.stream().collect(Collectors.toList());
            if (SHOW) {
                System.out.println(Joiners.N.join(contents));
            }
        }
    }

    private String tryOperations(
            MapType tt,
            Mutability mutability,
            Map3<String, String, Integer, Double> salesData,
            String streamTest) {

        logln(tt + ", " + mutability);

        // The following line would cause a COMPILE ERROR due to incorrect type,
        // demonstrating the benefit of the shims.
        // salesData.put("Asia", "Widget", "2024", 99.99);
        //    Compile Error! "2024" is not an Integer.

        logln(salesData.toString());

        // --- Type-Safe GET operations ---
        // The result is automatically cast to Double, no manual cast needed.
        Double sales = salesData.get("Asia", "Gadget3", 2023);
        assertEquals("Sales for Asia, Gadget3, 2003", 666d, sales);

        // --- Type-Safe REMOVE operations ---
        String result;
        int size = salesData.size();
        try {
            salesData.removeAll("Europe", "Gadget", 2025);
            result = String.valueOf(size - salesData.size());
        } catch (Exception e) {
            result = e.getClass().toString();
        }
        String expected =
                mutability == Mutability.mutable
                        ? "1"
                        : "class java.lang.UnsupportedOperationException";
        assertEquals("put", expected, result);

        assertNull("Data after removal", salesData.get("Europe", "Gadget", 2025));

        // check order
        List<String> list =
                salesData.stream().map(x -> x.getKey1()).collect(Collectors.toUnmodifiableList());
        Order order = getOrder(list);
        switch (tt) {
            case ConcurrentHash:
            case Hash:
                // should be unordered (normally)
                assertEquals(tt.name(), Order.na, order);
                break;
            case LinkedHash:
                // must be za (from the way we set up the data
                assertEquals(tt.name(), Order.za, order);
                break;
            case Tree:
                // must be az (because sorted)
                assertEquals(tt.name(), Order.az, order);
                break;
        }

        // Streaming
        String data = salesData.stream().map(x -> x.toString()).collect(Collectors.joining(", "));
        if (streamTest != null) {
            assertEquals("StreamTest", streamTest, data);
        } else {
            logln("StreamTest: " + data);
        }
        return data;
    }

    public void testOrder() {
        List<String> test =
                List.of("South America", "Asia", "Europe", "South Africa", "North Africa");
        assertEquals("Order ok", Order.na, getOrder(test));
    }

    private <T extends Comparable<T>> Order getOrder(Collection<T> list) {
        Set<Order> options = EnumSet.allOf(Order.class);
        T lastItem = null;
        for (T item : list) {
            if (lastItem != null) {
                int intOrder = lastItem.compareTo(item);
                if (intOrder < 0) {
                    options.remove(Order.za);
                    options.remove(Order.na);
                } else if (intOrder > 0) {
                    options.remove(Order.az);
                    options.remove(Order.na);
                }
            }
            lastItem = item;
        }
        return options.isEmpty() ? Order.na : options.iterator().next();
    }

    public void testWithSupplementatalAliases() {
        NestedMap.Map3<LstrType, String, Reason, List<String>> aliases =
                NestedMap.Map3.create(LinkedHashMap::new);
        Map<String, Map<String, R2<List<String>, String>>> localeAliasInfo =
                SDI.getLocaleAliasInfo();
        localeAliasInfo.entrySet().stream()
                .forEach(
                        x ->
                                x.getValue().entrySet().stream()
                                        .forEach(
                                                y ->
                                                        aliases.put(
                                                                get(x.getKey()),
                                                                y.getKey(),
                                                                Reason.valueOf(y.getValue().get1()),
                                                                y.getValue().get0())));
        ImmutableMap3<LstrType, String, Reason, List<String>> im = aliases.createImmutable();
        if (SHOW) {
            im.stream().forEach(System.out::println);
        }
    }

    public static class Bcp47KeyInfo {
        public final String extension;
        public final String description;
        public final String deprecated;
        public final String preferred;
        public final Set<String> aliases;
        public final String valueType;
        public final String since;

        public Bcp47KeyInfo(
                String extension,
                String description,
                String deprecated,
                String preferred,
                Set<String> aliases,
                String valueType,
                String since) {
            this.extension = extension == null ? "u" : extension;
            this.description = description;
            this.deprecated = deprecated;
            this.preferred = preferred;
            this.aliases = aliases;
            this.valueType = valueType;
            this.since = since;
        }

        static Bcp47KeyInfo from(Map<String, String> keyMap) {
            if (!FIELDS.containsAll(keyMap.keySet())) {
                throw new IllegalArgumentException(
                        "Unexpected attribute " + Sets.difference(keyMap.keySet(), FIELDS));
            }
            return new Bcp47KeyInfo(
                    keyMap.get("extension"),
                    keyMap.get("description"),
                    keyMap.get("deprecated"),
                    keyMap.get("preferred"),
                    splitAttributeValues(keyMap.get("alias")),
                    keyMap.get("valueType"),
                    keyMap.get("since"));
        }

        @Override
        public String toString() {
            return Joiners.TAB.join(
                    extension, description, deprecated, preferred, aliases, valueType, since);
        }

        // could get from DTDData
        public static ImmutableSet<String> FIELDS =
                ImmutableSet.copyOf(
                        Splitter.on(", ")
                                .split(
                                        "name, extension, description, deprecated, preferred, alias, valueType, since"));
    }

    public static class Bcp47TypeInfo {
        public final String description;
        public final String deprecated;
        public final String preferred;
        public final Set<String> aliases;
        public final String since;
        public final String iana;
        public final String region;

        public Bcp47TypeInfo(
                String description,
                String deprecated,
                String preferred,
                Set<String> aliases,
                String since,
                String iana,
                String valueType) {
            this.description = description;
            this.deprecated = deprecated;
            this.preferred = preferred;
            this.aliases = aliases;
            this.since = since;
            this.iana = iana;
            this.region = valueType;
        }

        static Bcp47TypeInfo from(Map<String, String> keyMap) {
            if (!FIELDS.containsAll(keyMap.keySet())) {
                throw new IllegalArgumentException(
                        "Unexpected attribute " + Sets.difference(keyMap.keySet(), FIELDS));
            }
            return new Bcp47TypeInfo(
                    keyMap.get("description"),
                    keyMap.get("deprecated"),
                    keyMap.get("preferred"),
                    splitAttributeValues(keyMap.get("alias")),
                    keyMap.get("since"),
                    keyMap.get("iana"),
                    keyMap.get("region"));
        }

        @Override
        public String toString() {
            return Joiners.TAB.join(
                    description, deprecated, preferred, aliases, since, iana, region);
        }

        // could get from DTDData
        public static ImmutableSet<String> FIELDS =
                ImmutableSet.copyOf(
                        Splitter.on(", ")
                                .split(
                                        "name, description, deprecated, preferred, alias, since, iana, region"));
    }

    public void testWithBCP47() {

        // read the data from the supplemental bcp47 data, but check for missing too!

        Map2<String, String, Bcp47TypeInfo> bcp47KeyTypeMap = Map2.create(TreeMap::new);
        Map<String, Bcp47KeyInfo> bcp47KeyMap = new TreeMap<>();
        Set<String> keySeen = new HashSet<>();

        // boilerplate for walking through files, then path-value pairs

        Path startPath = Paths.get(CLDRPaths.COMMON_DIRECTORY + "bcp47");
        try (Stream<Path> walk = Files.walk(startPath)) {
            walk.filter(Files::isRegularFile) // Filter to process only files
                    .forEach(
                            x -> {
                                if (!x.endsWith(".xml")) {
                                    return;
                                }
                                // here is are the guts
                                for (Pair<String, String> pathValue :
                                        XMLFileReader.loadPathValues(
                                                x.toString(), new ArrayList<>(), false)) {
                                    XPathParts parts =
                                            XPathParts.getFrozenInstance(pathValue.getFirst());
                                    if (!parts.getElement(1).equals("keyword")) {
                                        continue;
                                    }
                                    // we know we have a key
                                    String key = parts.getAttributeValue(2, "name");
                                    if (!keySeen.contains(key)) {
                                        bcp47KeyMap.put(
                                                key, Bcp47KeyInfo.from(parts.getAttributes(2)));
                                        keySeen.add(key); // don't bother repeating once we have one
                                    }
                                    String subtype = parts.getAttributeValue(3, "name");
                                    bcp47KeyTypeMap.put(
                                            key,
                                            subtype,
                                            Bcp47TypeInfo.from(parts.getAttributes(3)));
                                }
                                ;
                            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (SHOW) {
            System.out.println(
                    "\n" + Joiners.TAB.join(Bcp47KeyInfo.FIELDS) + "\tne getKeyName\tde coverage");
            List.of("u", "t").stream()
                    .forEach(
                            key -> {
                                int coverageValue = SDI.getCoverageValue(getKeyPath(key), "de");
                                System.out.println(
                                        key
                                                + "\t\t\t\t\t\t\t\t"
                                                + ENGLISH.getKeyName(key)
                                                + "\t"
                                                + coverageValue);
                            });
            bcp47KeyMap.entrySet().stream()
                    .forEach( //
                            x -> {
                                String key = x.getKey();
                                int coverageValue = SDI.getCoverageValue(getKeyPath(key), "en");
                                System.out.println(
                                        Joiners.TAB.join(key, x.getValue())
                                                + "\t"
                                                + ENGLISH.getKeyName(key)
                                                + "\t"
                                                + coverageValue);
                            });
            System.out.println(
                    "\nkey\t"
                            + Joiners.TAB.join(Bcp47TypeInfo.FIELDS)
                            + "\ten getKeyValueName\tde coverage");
            final Set<String> SKIP_KEYS = Set.of("cu", "tz");
            final Set<String> SKIP_TYPE =
                    Set.of(
                            "SCRIPT_CODE",
                            "FALSE",
                            "TRUE",
                            "REORDER_CODE",
                            "RG_KEY_VALUE",
                            "SUBDIVISION_CODE",
                            "CODEPOINTS",
                            "PRIVATE_USE");
            bcp47KeyTypeMap.stream()
                    .forEach( //
                            x -> {
                                String key1 = x.getKey1();
                                String key2 = x.getKey2();
                                if (SKIP_KEYS.contains(key1) || SKIP_TYPE.contains(key2)) {
                                    return;
                                }
                                int coverageValue =
                                        SDI.getCoverageValue(getKeyTypePath(key1, key2), "de");
                                System.out.println(
                                        Joiners.TAB.join(key1, key2, x.getValue())
                                                + "\t"
                                                + ENGLISH.getKeyValueName(key1, key2)
                                                + "\t"
                                                + coverageValue);
                            });
        }
    }

    // copied out of CLDRFile. Should be accessible
    public String getKeyPath(String key) {
        return "//ldml/localeDisplayNames/keys/key[@type=\"" + key + "\"]";
    }

    public String getKeyTypePath(String key, String value) {
        return "//ldml/localeDisplayNames/types/type[@key=\""
                + key
                + "\"][@type=\""
                + value
                + "\"]";
    }

    private static ImmutableSet<String> splitAttributeValues(String attributeValue) {
        return attributeValue == null
                ? null
                : ImmutableSet.copyOf(Splitter.on(' ').omitEmptyStrings().split(attributeValue));
    }

    @SuppressWarnings("unused")
    private <K, V> ImmutableMultimap<K, V> addTo(
            Relation<K, V> relation, Supplier<Multimap<K, V>> target) {
        Multimap<K, V> temp = target.get();
        relation.entrySet().stream().forEach(x -> temp.put(x.getKey(), x.getValue()));
        return ImmutableMultimap.copyOf(temp);
    }

    static LstrType get(String item) {
        switch (item) {
            case "territory":
                return LstrType.region;
            default:
                return LstrType.valueOf(item);
        }
    }

    enum UN_M49 {
        // there is always a global, and an ISO_alpha2_Code.
        // Antarctica is odd, so we include it in Oceania
        // That being done, there is always a Region_Code
        // If there is an Intermediate Region Code, then it is the subcontinent, and the Subregion
        // code is a subcontinent group
        // Otherwise the Subregion code is the subcontinent
        // 1    World   2   Africa  202 Sub-Saharan Africa  14  Eastern Africa  British Indian Ocean
        // Territory  86  IO  IOT

        Global_Code,
        Global_Name,
        Region_Code,
        Region_Name,
        Sub_region_Code,
        Sub_region_Name,
        Intermediate_Region_Code,
        Intermediate_Region_Name,
        Country_or_Area,
        M49_Code,
        ISO_alpha2_Code,
        ISO_alpha3_Code;
        static final Set<UN_M49> CODES =
                ImmutableSet.of(
                        Global_Code,
                        Region_Code,
                        Sub_region_Code,
                        Intermediate_Region_Code,
                        ISO_alpha2_Code);
    }

    public void testWithCountainment() throws IOException {
        if (SKIP) {
            return;
        }
        final Multimap<String, String> containerToContainedsExternal = TreeMultimap.create();
        Path path = Paths.get(CLDRPaths.UTIL_SRC_DATA_DIR, "data/external/UnCodes.tsv");

        Files.lines(path)
                .forEach(
                        line -> {
                            List<String> parts = Splitters.TAB.splitToList(line);
                            main:
                            for (UN_M49 item : UN_M49.CODES) {
                                String container = parts.get(item.ordinal());
                                if (container.isEmpty()) {
                                    continue;
                                }
                                for (UN_M49 item2 : UN_M49.CODES) {
                                    if (item2.compareTo(item) <= 0) {
                                        continue;
                                    }
                                    String contained = parts.get(item2.ordinal());
                                    if (contained.isEmpty()) {
                                        continue;
                                    }
                                    containerToContainedsExternal.put(container, contained);
                                    continue main;
                                }
                            }
                        });

        // now get internal
        final Multimap<String, String> containerToContainedsCldr = TreeMultimap.create();
        SDI.getContainmentCore().entrySet().stream()
                .forEach(x -> containerToContainedsCldr.put(x.getKey(), x.getValue()));

        SetView<Entry<String, String>> externalButNotInternal =
                Sets.difference(
                        ImmutableSet.copyOf(containerToContainedsExternal.entries()),
                        ImmutableSet.copyOf(containerToContainedsCldr.entries()));
        SetView<Entry<String, String>> extra =
                Sets.difference(
                        ImmutableSet.copyOf(containerToContainedsCldr.entries()),
                        ImmutableSet.copyOf(containerToContainedsExternal.entries()));

        if (!assertEquals("Missing from Internal", "[]", externalButNotInternal.toString())) {
            externalButNotInternal.stream()
                    .forEach(
                            x ->
                                    System.out.println(
                                            NAME_GETTER.getNameFromTypeEnumCode(
                                                            NameType.TERRITORY, x.getKey())
                                                    + " > "
                                                    + NAME_GETTER.getNameFromTypeEnumCode(
                                                            NameType.TERRITORY, x.getValue())));
        }
        if (!assertEquals("Extra in Internal", "[]", extra.toString())) {
            extra.stream()
                    .forEach(
                            x ->
                                    System.out.println(
                                            NAME_GETTER.getNameFromTypeEnumCode(
                                                            NameType.TERRITORY, x.getKey())
                                                    + " > "
                                                    + NAME_GETTER.getNameFromTypeEnumCode(
                                                            NameType.TERRITORY, x.getValue())));
        }
    }

    enum TestEnum {
        a,
        b,
        c,
        d
    }

    public void testStreaming() {
        // create test data
        Set<Entry3<String, TestEnum, String>> testData3 = new LinkedHashSet<>();
        Set<Entry4<String, TestEnum, Integer, String>> testData4 = new LinkedHashSet<>();

        for (String s : List.of("VT", "AL")) {
            for (TestEnum e : List.of(TestEnum.d, TestEnum.a)) {
                testData3.add(new Entry3<>(s, e, s + e));
                for (Integer i : List.of(5, -1, 10)) {
                    testData4.add(new Entry4<>(s, e, i, s + e + i));
                }
            }
        }
        // create maps from that

        Map2<String, TestEnum, String> map2 = Map2.create();
        testData3.stream().forEach(x -> map2.put(x));
        Map2<String, TestEnum, String> immutableMap2 = map2.createImmutable();

        Map3<String, TestEnum, Integer, String> map3 = Map3.create();
        testData4.stream().forEach(x -> map3.put(x));
        Map3<String, TestEnum, Integer, String> immutableMap3 = map3.createImmutable();

        // now stream the results and ensure that the data is the same

        Set<Entry3<String, TestEnum, String>> streamedSet2 =
                map2.stream().collect(Collectors.toSet());
        assertEquals("map2 stream", testData3, streamedSet2);

        Set<Entry3<String, TestEnum, String>> streamedSetOfImmutable2 =
                immutableMap2.stream().collect(Collectors.toSet());
        assertEquals("map2 stream immutable", testData3, streamedSetOfImmutable2);

        Set<Entry4<String, TestEnum, Integer, String>> streamedSet3 =
                map3.stream().collect(Collectors.toSet());
        assertEquals("map3 stream", testData4, streamedSet3);

        Set<Entry4<String, TestEnum, Integer, String>> streamedSetOfImmutable3 =
                immutableMap3.stream().collect(Collectors.toSet());
        assertEquals("map3 stream immutable", testData4, streamedSetOfImmutable3);
    }
}

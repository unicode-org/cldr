package org.unicode.cldr.util;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A NestedMap is a generic, nested map structure that can handle a variable number of levels. This
 * class provides the core logic and is wrapped by type-safe shims. For example:
 *
 * <ul>
 *   <li>For map&lt;key1, key2, value>: use Map2&lt;K1, K2, V> and ImmutableMap2&lt;K1, K2, V>,
 *       streaming with Entry3&lt;K1, K2, V>
 *   <li>For map&lt;key1, key2, key3, value>: use Map3&lt;K1, K2, K3, V> and ImmutableMap3&lt;K1,
 *       K2, K3, V>, streaming with Entry3&lt;K1, K2, L3, V>
 *   <li>For multimap&lt;key1, key2, value>: Multimap2&lt;K1, K2, V> and ImmutableMultimap3&lt;K1,
 *       K2, V>, streaming with Entry3&lt;K1, K2, V>.
 *       <ul>
 *         <li>Under the covers, a MultimapN is just a MapN+1, with Boolean as the V, and TRUE as
 *             the value.)
 *       </ul>
 * </ul>
 *
 * Usage examples:
 *
 * <pre>
 * Map3&lt;String, String, Integer, Double> salesData = Map3.create(SomeMap::new);
 *
 * salesData.put("South America", "Widget", 2024, 150000.75);
 *
 * ImmutableMap3&lt;String, String, Integer, Double> salesDataIM  = salesData.createImmutable();
 *
 * List&lt;Entry4&lt;String, String, Integer, Double>> contents  = salesData.stream().collect(Collectors.toList());
 * </pre>
 *
 * Where SomeMap::new can be replaced by any Map::new for the desired behaviors (or other Map
 * suppliers):
 *
 * <ul>
 *   <li>TreeMap::new
 *   <li>HashMap::new
 *   <li>LinkedHashMap::new,
 *   <li>ConcurrentHashMap::new — for thread-safety across all levels
 * </ul>
 *
 * NOTE: neither keys nor values can be null.
 *
 * <p>Future possible extensions:
 *
 * <ul>
 *   <li>The current implementation is limited to what CLDR needs. Addition methods like contains()
 *       could be added as needed.
 *   <li>It is easy to add additional levels in the future, using the same template.
 *   <li>It would also be easy to add mixed levels, eg Map3.create(SomeMap::new, SomeOtherMap::new)
 *   <li>Future possible extension: add the ability to have different comparators for different
 *       levels of TreeMaps
 * </ul>
 */
public class NestedMap {

    /**
     * Each shim will have a uniform depth, aka keyCount. For example, a Map2<K1,K2,V> will will be
     * a map of maps. The keyCount is the number of keys.
     */
    private final int keyCount;

    /** The root is created from the first supplier in the constructor */
    private final Map<Object, Object> root;

    /**
     * The mapFactories are supplied by the constructor, and are used to create maps at each level.
     * The size is identical to the keyCount.
     */
    private final List<Supplier<Map<Object, Object>>> mapFactories;

    // Examples: TreeMap::new, HashMap::new, ConcurrentHashMap::new
    /**
     * Used by subclasses
     *
     * @param keyCount This is the number of keys; so for <key1, key2, value> it would be 2.
     * @param mapFactories A list of suppliers, one for each key. If the length of the list is less
     *     that
     */
    @SafeVarargs
    private NestedMap(int keyCount, Supplier<Map<Object, Object>>... mapFactories) {
        if (mapFactories.length > keyCount) {
            throw new IllegalArgumentException("Too many mapFactories");
        }
        this.keyCount = keyCount;
        if (mapFactories.length == keyCount) {
            this.mapFactories = List.of(mapFactories);
        } else { // if the list is too short, fill out with copies of the last
            List<Supplier<Map<Object, Object>>> temp = new ArrayList<>();
            Supplier<Map<Object, Object>> toDuplicate;
            if (mapFactories.length == 0) {
                toDuplicate = HashMap::new;
            } else {
                temp.addAll(Arrays.asList(mapFactories));
                toDuplicate = temp.get(temp.size() - 1);
            }
            while (temp.size() < keyCount) {
                temp.add(toDuplicate);
            }
            this.mapFactories = List.copyOf(temp);
        }
        this.root = this.mapFactories.get(0).get();
    }

    /**
     * Used for creating an immutable nested map.
     *
     * @param keyCount This is the number of keys; so for <key1, key2, value> it would be 2.
     * @param mapFactories A list of suppliers, one for each key. If the length of the list is less
     *     that
     */
    private NestedMap(int keyCount, Map<Object, Object> immutableRoot) {
        this.keyCount = keyCount;
        this.root = immutableRoot;
        this.mapFactories = null; // will never change
    }

    // --- CORE API METHODS ---

    /**
     * If called by a shim with not all the keys, the shim will return the a keyset for the last
     * map. That allows for the shim to return the set of possible keys for the next level.
     */
    @SuppressWarnings("unchecked")
    private Object getInternal(Object... keys) {
        if (keys.length == 0 || keys.length > keyCount) {
            // this should never happen if called from a valid shim
            throw new IllegalArgumentException("Incorrect number of keys");
        }

        Object current = root;
        for (Object key : keys) {
            if (!(current instanceof Map)) return null;
            current = ((Map<Object, Object>) current).get(key);
            if (current == null) return null;
        }
        return keys.length == keyCount ? current : ((Map<Object, Object>) current).keySet();
    }

    @SuppressWarnings("unchecked")
    private void putInternal(Object... keysAndValue) {
        if (this.mapFactories == null) { // attempt to modify immutable
            throw new UnsupportedOperationException("Cannot modify immutable object");
        }
        if (keysAndValue.length != keyCount + 1) {
            // this should never happen if called from a valid shim
            throw new IllegalArgumentException("At least one key and one value must be provided.");
        }
        for (Object keyOrValue : keysAndValue) {
            if (keyOrValue == null) {
                throw new IllegalArgumentException("Cannot store null key or value");
            }
        }

        Map<Object, Object> currentMap = this.root;
        // iterate through all the maps except for the last one
        for (int i = 0; i < keysAndValue.length - 2; i++) {
            Object key = keysAndValue[i];
            Supplier<Map<Object, Object>> factory = mapFactories.get(i);
            currentMap = (Map<Object, Object>) currentMap.computeIfAbsent(key, k -> factory.get());
        }
        Object finalKey = keysAndValue[keysAndValue.length - 2];
        Object value = keysAndValue[keysAndValue.length - 1];
        currentMap.put(finalKey, value);
    }

    /**
     * Deeply removes a value at the end of a key chain and prunes empty parent maps. If the
     * keysAndPossibleValue is not a long as the keyCount, then prunes the end.
     */
    private void removeInternal(Object... keysAndPossibleValue) {
        if (this.mapFactories == null) { // attempt to modify immutable
            throw new UnsupportedOperationException("Cannot modify immutable object");
        }
        if (keysAndPossibleValue.length == 0 || keysAndPossibleValue.length > keyCount + 1) {
            // this should never happen if called from a valid shim
            throw new IllegalArgumentException(
                    "Must have at least 1 key, and no more keys than the keyCount");
        }
        recursiveRemove(root, keysAndPossibleValue, 0);
    }

    @SuppressWarnings("unchecked")
    private Object recursiveRemove(
            Map<Object, Object> currentMap, Object[] keysAndPossibleValue, int index) {
        Object key = keysAndPossibleValue[index];

        // depends on computeIfPresent
        // If the value for the specified key is present and non-null, attempts to compute a new
        // mapping given the key and its current mapped value.
        // If the remapping function returns null, the mapping is removed.

        ((Map<Object, Object>) currentMap)
                .computeIfPresent(
                        key,
                        (k, value) -> {
                            // BASE CASE: We are at the last key or value
                            if (index == keysAndPossibleValue.length - 1) {
                                // We return null so that the containing map removes this item,
                                // which may be a map
                                return null;
                            }
                            // Recursive Case: We are not at the leaf
                            if (index < keyCount - 1) {
                                Map<Object, Object> childMap = (Map<Object, Object>) value;
                                recursiveRemove(childMap, keysAndPossibleValue, index + 1);

                                // If the child map is now empty, return null to prune it
                                return childMap.isEmpty() ? null : childMap;
                            }
                            // If it's not a map and not the leaf, the path is invalid; keep as is
                            return Objects.equals(value, keysAndPossibleValue[index + 1])
                                    ? null
                                    : value;
                        });
        // return null to remove
        return null;
    }

    @Override
    public String toString() {
        return root.toString();
    }

    @Override
    public boolean equals(Object obj) {
        return root.equals(((NestedMap) obj).root);
    }

    @Override
    public int hashCode() {
        return root.hashCode();
    }

    private int size() {
        // NOTE: we could cache this value, and update with any change resulting from get or remove.
        // That is a possible optimization at the cost of some more bookkeeping
        return recursiveSize(keyCount - 1, this.root);
    }

    @SuppressWarnings("unchecked")
    private int recursiveSize(int level, Map<Object, Object> root2) {
        return level == 0
                ? root2.size()
                : root2.values().stream()
                        .mapToInt(x -> recursiveSize(level - 1, (Map<Object, Object>) x))
                        .sum();
    }

    private MapDifference<Object, Object> difference(NestedMap other) {
        return Maps.difference(root, other.root);
    }

    // --- IMMUTABILITY ---

    /**
     * Creates a deep, immutable copy of this map. The resulting map is read-only and thread-safe
     * for reads.
     *
     * @return A new ImmutableNestedMap instance.
     */
    private NestedMap toImmutable() {
        return new NestedMap(keyCount, CldrUtility.protectCollection(this.root));
    }

    /**
     * Returns a lazy stream of all logical entries in the nested map. Each entry is a list of
     * objects, where the last element is the value.
     */
    private Stream<List<Object>> stream() {
        // Note: the FlatArraySpliterator is much simpler to understand, but has some limitations.
        // Notably, it doesn't parallelize.
        return StreamSupport.stream(new FlatArraySpliterator(root, keyCount), false)
                .map(array -> Arrays.asList(array));
    }

    @SuppressWarnings("unchecked")
    public static class Entry3<K1, K2, V> { // With Java17 we could use records
        private final List<Object> list;

        public Entry3(K1 k1, K2 k2, V v) {
            this.list = List.of(k1, k2, v);
        }

        /* not type-safe, not for general use */
        public Entry3(List<Object> list) {
            if (list == null || list.size() != 3) {
                throw new IllegalArgumentException();
            }
            this.list = list;
        }

        public List<Object> getList() {
            return list;
        }

        public K1 getKey1() {
            return (K1) list.get(0);
        }

        public K2 getKey2() {
            return (K2) list.get(1);
        }

        public V getValue() {
            return (V) list.get(2);
        }

        @Override
        public String toString() {
            return list.toString();
        }

        public boolean equals(Object obj) {
            return this == obj
                    || obj instanceof Entry3 && this.list.equals(((Entry3<K1, K2, V>) obj).list);
        }

        @Override
        public int hashCode() {
            return list.hashCode();
        }
    }

    @SuppressWarnings("unchecked")
    public static class Entry4<K1, K2, K3, V> { // With Java17 we could use records
        private List<Object> list;

        public Entry4(K1 k1, K2 k2, K3 k3, V v) {
            this.list = List.of(k1, k2, k3, v);
        }

        /* Not type-safe */
        public Entry4(List<Object> list) {
            if (list == null || list.size() != 4) {
                throw new IllegalArgumentException();
            }
            this.list = list;
        }

        public K1 getKey1() {
            return (K1) list.get(0);
        }

        public K2 getKey2() {
            return (K2) list.get(1);
        }

        public K3 getKey3() {
            return (K3) list.get(2);
        }

        public V getValue() {
            return (V) list.get(3);
        }

        @Override
        public String toString() {
            return list.toString();
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj
                    || obj instanceof Entry4
                            && this.list.equals(((Entry4<K1, K2, K3, V>) obj).list);
        }

        @Override
        public int hashCode() {
            return list.hashCode();
        }
    }

    @SuppressWarnings("unchecked")
    public static class Map2<K1, K2, V> {
        private final NestedMap engine;

        private Map2(NestedMap engine) {
            this.engine = engine;
        }

        /**
         * Create a nested map with 2 keys, and 0..2 suppliers
         *
         * @param suppliers Common ones are Treemap::new, HashMap::new, ConcurrentHashMap::new, etc.
         *     The default is a HashMap::new. If the number of suppliers is not insufficient, the
         *     last supplier is used to fill it out.
         *     <p>Examples So Map2.create(TreeMap::new) is equivalent to Map2.create(TreeMap::new,
         *     TreeMap::new)<br>
         *     So Map2.create() is equivalent to Map2.create(HashMap::new, HashMap::new)
         * @return
         */
        @SafeVarargs
        public static <K1, K2, V> Map2<K1, K2, V> create(
                Supplier<Map<Object, Object>>... suppliers) {
            return new Map2<>(new NestedMap(2, suppliers));
        }

        @Override
        public String toString() {
            return engine.toString();
        }

        @Override
        public boolean equals(Object obj) {
            return engine.equals(((Map2<K1, K2, V>) obj).engine);
        }

        @Override
        public int hashCode() {
            return engine.hashCode();
        }

        public int size() {
            return engine.size();
        }

        public V get(K1 key1, K2 key2) {
            return (V) engine.getInternal(key1, key2);
        }

        public Set<K2> keySet2(K1 key1) {
            return (Set<K2>) engine.getInternal(key1);
        }

        public Set<K2> keySet() {
            return (Set<K2>) engine.root.keySet();
        }

        public void put(K1 key1, K2 key2, V value) {
            engine.putInternal(key1, key2, value);
        }

        public void put(Entry3<K1, K2, V> entry3) {
            engine.putInternal(entry3.getKey1(), entry3.getKey2(), entry3.getValue());
        }

        public void putAll(Map2<K1, K2, V> other) {
            // Might optimize later
            other.stream().forEach(x -> put(x.getKey1(), x.getKey2(), x.getValue()));
        }

        public void remove(K1 key1, K2 key2, V value) {
            engine.removeInternal(key1, key2, value);
        }

        public void removeAll(K1 key1, K2 key2) {
            engine.removeInternal(key1, key2);
        }

        public void removeAll(K1 key1) {
            engine.removeInternal(key1);
        }

        public Stream<Entry3<K1, K2, V>> stream() {
            return engine.stream().map(list -> new Entry3<K1, K2, V>(list));
        }

        public ImmutableMap2<K1, K2, V> createImmutable() {
            return new ImmutableMap2<K1, K2, V>(engine);
        }

        /** For debugging only */
        public MapDifference<Object, Object> difference(Map2<K1, K2, V> other) {
            return engine.difference(other.engine);
        }
    }

    public static class ImmutableMap2<K1, K2, V> extends Map2<K1, K2, V> {

        private ImmutableMap2(NestedMap engine) {
            super(engine.toImmutable());
        }
    }

    @SuppressWarnings("unchecked")
    public static class Map3<K1, K2, K3, V> {
        private final NestedMap engine;

        private Map3(NestedMap engine) {

            this.engine = engine;
        }

        /**
         * Create a nested map with 3 keys, and 0..3 Suppliers
         *
         * @param suppliers Common ones are Treemap::new, HashMap::new, ConcurrentHashMap::new, etc.
         *     The default is a HashMap::new. If the number of suppliers is not insufficient, the
         *     last supplier is used to fill it out.
         *     <p>Examples So Map3.create(TreeMap::new) is equivalent to Map2.create(TreeMap::new,
         *     TreeMap::new, TreeMap::new)<br>
         *     So Map3.create() is equivalent to Map2.create(HashMap::new, HashMap::new,
         *     HashMap::new)
         */
        @SafeVarargs
        public static <K1, K2, K3, V> Map3<K1, K2, K3, V> create(
                Supplier<Map<Object, Object>>... suppliers) {
            return new Map3<>(new NestedMap(3, suppliers));
        }

        public V get(K1 key1, K2 key2, K3 key3) {
            return (V) engine.getInternal(key1, key2, key3);
        }

        public Set<K3> keySet3(K1 key1, K2 key2) {
            return (Set<K3>) engine.getInternal(key1, key2);
        }

        public Set<K2> keySet2(K1 key1) {
            return (Set<K2>) engine.getInternal(key1);
        }

        public Set<K2> keySet() {
            return (Set<K2>) engine.root.keySet();
        }

        public void put(K1 key1, K2 key2, K3 key3, V value) {
            engine.putInternal(key1, key2, key3, value);
        }

        public void put(Entry4<K1, K2, K3, V> entry3) {
            engine.putInternal(
                    entry3.getKey1(), entry3.getKey2(), entry3.getKey3(), entry3.getValue());
        }

        public void remove(K1 key1, K2 key2, K3 key3, V value) {
            engine.removeInternal(key1, key2, key3, value);
        }

        public void removeAll(K1 key1, K2 key2, K3 key3) {
            engine.removeInternal(key1, key2);
        }

        public void removeAll(K1 key1, K2 key2) {
            engine.removeInternal(key1, key2);
        }

        public void removeAll(K1 key1) {
            engine.removeInternal(key1);
        }

        @Override
        public String toString() {
            return engine.toString();
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || engine.equals(((Map3<K1, K2, K3, V>) obj).engine);
        }

        @Override
        public int hashCode() {
            return engine.hashCode();
        }

        public int size() {
            return engine.size();
        }

        public Stream<Entry4<K1, K2, K3, V>> stream() {
            return engine.stream().map(list -> new Entry4<K1, K2, K3, V>(list));
        }

        public ImmutableMap3<K1, K2, K3, V> createImmutable() {
            return new ImmutableMap3<K1, K2, K3, V>(engine);
        }

        /** For debugging only */
        public MapDifference<Object, Object> difference(Map3<K1, K2, K3, V> other) {
            return engine.difference(other.engine);
        }
    }

    public static class ImmutableMap3<K1, K2, K3, V> extends Map3<K1, K2, K3, V> {

        private ImmutableMap3(NestedMap engine) {
            super(engine.toImmutable());
        }
    }

    // NOTE: consider adding a Multimap1<K, V>, because guava doesn't supply a
    // concurrent multimap.
    // A concurrent Set is created with
    // Set<String> concurrentSet = ConcurrentHashMap.newKeySet();

    public static class Multimap2<K1, K2, V> {
        // a Multimap under the covers is a Map3<K1, K2, V, TRUE>
        private final NestedMap engine;

        private Multimap2(NestedMap engine) {
            this.engine = engine;
        }

        /**
         * Takes Treemap::new, HashMap::new, ConcurrentHashMap::new, and other suppliers. Note: the
         * final supplier should be one suitable for producing Map<V, Boolean>.
         */
        /**
         * Create a multimap map with 2 keys, and 0..3 suppliers
         *
         * @param suppliers Common ones are Treemap::new, HashMap::new, ConcurrentHashMap::new, etc.
         *     The default is a HashMap::new. If the number of suppliers is not insufficient, the
         *     last supplier is used to fill it out.
         *     <p>Examples So Multimap2.create(TreeMap::new) is equivalent to
         *     Multimap2.create(TreeMap::new, TreeMap::new, TreeMap::new)<br>
         *     So Multimap2.create() is equivalent to Map2.create(HashMap::new, HashMap::new,
         *     HashMap::new)<br>
         *     Note: the final supplier should be one suitable for producing Map<V, Boolean>
         */
        @SafeVarargs
        public static <K1, K2, V> Multimap2<K1, K2, V> create(
                Supplier<Map<Object, Object>>... suppliers) {
            return new Multimap2<>(new NestedMap(3, suppliers));
        }

        public void put(K1 key1, K2 key2, V value) {
            engine.putInternal(key1, key2, value, Boolean.TRUE);
        }

        public void put(Entry3<K1, K2, V> entry3) {
            engine.putInternal(entry3.getKey1(), entry3.getKey2(), entry3.getValue(), Boolean.TRUE);
        }

        @SuppressWarnings("unchecked")
        public Set<V> get(K1 key1, K2 key2) {
            return (Set<V>) engine.getInternal(key1, key2);
        }

        @SuppressWarnings("unchecked")
        public Set<K2> keySet2(K1 key1) {
            return (Set<K2>) engine.getInternal(key1);
        }

        @SuppressWarnings("unchecked")
        public Set<K1> keySet() {
            return (Set<K1>) engine.root.keySet();
        }

        public boolean contains(K1 key1, K2 key2, V value) {
            return engine.getInternal(key1, key2, value) != null;
        }

        public void remove(K1 key1, K2 key2, V value) {
            engine.removeInternal(key1, key2, value);
        }

        public void removeAll(K1 key1, K2 key2) {
            engine.removeInternal(key1, key2);
        }

        public void removeAll(K1 key1) {
            engine.removeInternal(key1);
        }

        @Override
        public String toString() {
            return engine.toString();
        }

        @SuppressWarnings("rawtypes")
        @Override
        public boolean equals(Object obj) {
            return this == obj || engine.equals(((Multimap2) obj).engine);
        }

        @Override
        public int hashCode() {
            return engine.hashCode();
        }

        public Stream<Entry3<K1, K2, V>> stream() {
            return engine.stream().map(list -> new Entry3<K1, K2, V>(list));
        }

        public ImmutableMultimap2<K1, K2, V> createImmutable() {
            return new ImmutableMultimap2<K1, K2, V>(engine);
        }

        public Object size() {
            return engine.size();
        }
    }

    public static class ImmutableMultimap2<K1, K2, V> extends Multimap2<K1, K2, V> {

        private ImmutableMultimap2(NestedMap engine) {
            super(engine.toImmutable());
        }
    }
}

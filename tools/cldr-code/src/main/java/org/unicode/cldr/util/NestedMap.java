package org.unicode.cldr.util;

import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
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
 * Future possible extensions:
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
     * Each subclass will have a uniform depth. For example, a Map2<K1,K2,V> will will be a map of
     * maps.
     */
    private final int depth;

    private final Map<Object, Object> root;
    private final List<Supplier<Map<Object, Object>>> mapFactories;

    // Examples: TreeMap::new, HashMap::new, ConcurrentHashMap::new
    @SafeVarargs
    private NestedMap(int depth, Supplier<Map<Object, Object>>... mapFactories) {
        if (mapFactories.length > depth) {
            throw new IllegalArgumentException("Too many mapFactories");
        }
        this.depth = depth;
        if (mapFactories.length == depth - 1) {
            this.mapFactories = List.of(mapFactories);
        } else {
            List<Supplier<Map<Object, Object>>> temp = new ArrayList<>();
            temp.addAll(Arrays.asList(mapFactories));
            Supplier<Map<Object, Object>> last = temp.get(temp.size() - 1);
            while (temp.size() < depth - 1) {
                temp.add(last);
            }
            this.mapFactories = List.copyOf(temp);
        }
        this.root = this.mapFactories.get(0).get();
    }

    // Used for creating an immutable map.
    private NestedMap(int depth, Map<Object, Object> immutableRoot) {
        this.depth = depth;
        this.root = immutableRoot;
        this.mapFactories = null; // will never change
    }

    // --- CORE API METHODS ---
    /** Always called by submaps with all of the keys; eg, 2 keys with Map2 */
    @SuppressWarnings("unchecked")
    private Object getInternal(Object... keys) {
        int limit = keys.length;
        Map<Object, Object> currentMap = this.root;
        Object nextLevel = null;
        for (int i = 0; i < limit; i++) {
            nextLevel = currentMap.get(keys[i]);
            if (nextLevel instanceof Map) {
                currentMap = (Map<Object, Object>) nextLevel;
            } else {
                return nextLevel;
            }
        }
        return nextLevel;
    }

    @SuppressWarnings("unchecked")
    private void putInternal(Object... keysAndValue) {
        if (keysAndValue.length < 2) {
            throw new IllegalArgumentException("At least one key and one value must be provided.");
        }
        Map<Object, Object> currentMap = this.root;
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
     * keysAndPossibleValue is not a long as the depth, then prunes the end.
     */
    public void removeInternal(Object... keysAndPossibleValue) {
        if (keysAndPossibleValue == null || keysAndPossibleValue.length == 0) {
            return;
        }
        recursiveRemove(root, keysAndPossibleValue, 0);
    }

    @SuppressWarnings("unchecked")
    private static Object recursiveRemove(
            Map<Object, Object> currentMap, Object[] keys, int index) {
        Object key = keys[index];

        // depends on computeIfPresent
        // If the value for the specified key is present and non-null, attempts to compute a new
        // mapping given the key and its current mapped value.
        // If the remapping function returns null, the mapping is removed.

        ((Map<Object, Object>) currentMap)
                .computeIfPresent(
                        key,
                        (k, value) -> {
                            // BASE CASE: We are at the last key or value
                            if (index == keys.length - 1) {
                                // We return null to remove the leaf object (C) from the map (B)
                                return null;
                            }
                            // Recursive Case: The value is another map
                            if (value instanceof Map) {
                                Map<Object, Object> childMap = (Map<Object, Object>) value;
                                recursiveRemove(childMap, keys, index + 1);

                                // If the child map is now empty, return null to prune it
                                return childMap.isEmpty() ? null : childMap;
                            }
                            // If it's not a map and not the leaf, the path is invalid; keep as is
                            return Objects.equals(value, keys[index + 1]) ? null : value;
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
        return recursiveSize(depth - 1, this.root);
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
        return new NestedMap(depth, CldrUtility.protectCollection(this.root));
    }

    /**
     * Returns a LAZY stream of all terminal entries in the nested map. The stream is built
     * on-the-fly and does not store all entries in memory. Each entry is a list of objects, where
     * the last element is the value.
     */
    private Stream<List<Object>> stream() {
        return StreamSupport.stream(new NestedMapSpliterator(this.root), false);
    }

    /**
     * A custom Spliterator that traverses the nested map structure lazily. It uses a stack of
     * iterators to perform a depth-first traversal.
     */
    private static class NestedMapSpliterator
            extends Spliterators.AbstractSpliterator<List<Object>> {
        private final Deque<Iterator<Map.Entry<Object, Object>>> iteratorStack = new ArrayDeque<>();
        private final Deque<Object> keyPath = new ArrayDeque<>();

        NestedMapSpliterator(Map<Object, Object> root) {
            super(
                    Long.MAX_VALUE,
                    Spliterator.ORDERED); // Assumes ORDERED for TreeMap, harmless for others.
            if (root != null && !root.isEmpty()) {
                iteratorStack.push(root.entrySet().iterator());
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean tryAdvance(Consumer<? super List<Object>> action) {
            while (!iteratorStack.isEmpty()) {
                Iterator<Map.Entry<Object, Object>> currentIterator = iteratorStack.peek();

                if (currentIterator.hasNext()) {
                    Map.Entry<Object, Object> entry = currentIterator.next();
                    Object key = entry.getKey();
                    Object value = entry.getValue();

                    keyPath.addLast(key);

                    if (value instanceof Map) {
                        // Go deeper: push the new map's iterator onto the stack.
                        iteratorStack.push(((Map<Object, Object>) value).entrySet().iterator());
                    } else {
                        // Terminal value found: build the result and consume it.
                        List<Object> result = new ArrayList<>(keyPath);
                        result.add(value);
                        action.accept(result);
                        keyPath.removeLast(); // Backtrack path after consuming.
                        return true;
                    }
                } else {
                    // This level is exhausted: backtrack.
                    iteratorStack.pop();
                    if (!keyPath.isEmpty()) {
                        keyPath.removeLast();
                    }
                }
            }
            // The entire map has been traversed.
            return false;
        }
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

        /** Takes Treemap::new, HashMap::new, ConcurrentHashMap::new, and other suppliers */
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

        public void put(K1 key1, K2 key2, V value) {
            engine.putInternal(key1, key2, value);
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

        @Override
        public void put(K1 key1, K2 key2, V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void remove(K1 key1, K2 key2, V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeAll(K1 key1, K2 key2) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeAll(K1 key1) {
            throw new UnsupportedOperationException();
        }
    }

    @SuppressWarnings("unchecked")
    public static class Map3<K1, K2, K3, V> {
        private final NestedMap engine;

        private Map3(NestedMap engine) {

            this.engine = engine;
        }

        /** Takes Treemap::new, HashMap::new, ConcurrentHashMap::new, and other suppliers */
        @SafeVarargs
        public static <K1, K2, K3, V> Map3<K1, K2, K3, V> create(
                Supplier<Map<Object, Object>>... suppliers) {
            return new Map3<>(new NestedMap(3, suppliers));
        }

        public V get(K1 key1, K2 key2, K3 key3) {
            return (V) engine.getInternal(key1, key2, key3);
        }

        public void put(K1 key1, K2 key2, K3 key3, V value) {
            engine.putInternal(key1, key2, key3, value);
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

        @Override
        public void put(K1 key1, K2 key2, K3 key3, V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void remove(K1 key1, K2 key2, K3 key3, V value) {
            throw new UnsupportedOperationException();
        }

        public void removeAll(K1 key1, K2 key2, K3 key3) {
            throw new UnsupportedOperationException();
        }

        public void removeAll(K1 key1, K2 key2) {
            throw new UnsupportedOperationException();
        }

        public void removeAll(K1 key1) {
            throw new UnsupportedOperationException();
        }
    }

    public static class Multimap2<K1, K2, V> {
        private final NestedMap engine;

        private Multimap2(NestedMap engine) {
            this.engine = engine;
        }

        /** Takes Treemap::new, HashMap::new, ConcurrentHashMap::new, and other suppliers */
        @SafeVarargs
        public static <K1, K2, V> Multimap2<K1, K2, V> create(
                Supplier<Map<Object, Object>>... suppliers) {
            return new Multimap2<>(new NestedMap(3, suppliers));
        }

        public void put(K1 key1, K2 key2, V value) {
            engine.putInternal(key1, key2, value, Boolean.TRUE);
        }

        @SuppressWarnings("unchecked")
        public Set<V> get(K1 key1, K2 key2) {
            Map<V, Boolean> map = (Map<V, Boolean>) engine.getInternal(key1, key2);
            return map == null ? null : map.keySet();
        }

        public boolean contains(K1 key1, K2 key2, V value) {
            return engine.getInternal(key1, key2) != null;
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

        public void put(K1 key1, K2 key2, V value) {
            throw new UnsupportedOperationException();
        }

        public void remove(K1 key1, K2 key2, V value) {
            throw new UnsupportedOperationException();
        }

        public void removeAll(K1 key1, K2 key2) {
            throw new UnsupportedOperationException();
        }

        public void removeAll(K1 key1) {
            throw new UnsupportedOperationException();
        }
    }
}

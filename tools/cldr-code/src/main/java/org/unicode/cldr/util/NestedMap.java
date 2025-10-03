package org.unicode.cldr.util;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * ENGINE CLASS A generic, nested map structure that can handle a variable number of levels. This
 * class provides the core logic and is wrapped by the type-safe shims.
 *
 * <p>Various nested Multisets can be created by using an extra layer with Boolean. Multimap2<K1,
 * K2, V> == NestedMap3<K1, K2, V, Boolean>
 */
public class NestedMap {

    protected final Map<Object, Object> root;
    private final Supplier<Map<Object, Object>> mapFactory;

    // Examples: TreeMap::new, HashMap::new, ConcurrentHashMap::new
    private NestedMap(Supplier<Map<Object, Object>> mapFactory) {
        this.mapFactory = Objects.requireNonNull(mapFactory);
        this.root = this.mapFactory.get();
    }

    public NestedMap(Map<Object, Object> immutableRoot) {
        this.root = immutableRoot;
        this.mapFactory = null; // will never change
    }

    @SuppressWarnings("unchecked")
    // TODO: add comparators for TreeMaps
    //    public static NestedMap createWithSortedMaps(Comparator<Object>... comparator) {
    //        return new NestedMap(() -> (Map<Object, Object>) (Map<?, ?>) new
    // TreeMap<>(comparator));
    //    }

    // --- CORE API METHODS ---
    public void put(Object... keysAndValue) {
        if (keysAndValue.length < 2) {
            throw new IllegalArgumentException("At least one key and one value must be provided.");
        }
        Map<Object, Object> currentMap = this.root;
        for (int i = 0; i < keysAndValue.length - 2; i++) {
            Object key = keysAndValue[i];
            currentMap =
                    (Map<Object, Object>) currentMap.computeIfAbsent(key, k -> mapFactory.get());
        }
        Object finalKey = keysAndValue[keysAndValue.length - 2];
        Object value = keysAndValue[keysAndValue.length - 1];
        currentMap.put(finalKey, value);
    }

    public Object get(Object... keys) {
        if (keys.length == 0) return null;
        Map<Object, Object> currentMap = this.root;
        for (int i = 0; i < keys.length - 1; i++) {
            Object nextLevel = currentMap.get(keys[i]);
            if (nextLevel instanceof Map) {
                currentMap = (Map<Object, Object>) nextLevel;
            } else {
                return null;
            }
        }
        return currentMap.get(keys[keys.length - 1]);
    }

    public Object remove(Object... keys) {
        if (keys.length == 0) return null;
        Map<Object, Object> currentMap = this.root;
        for (int i = 0; i < keys.length - 1; i++) {
            Object nextLevel = currentMap.get(keys[i]);
            if (nextLevel instanceof Map) {
                currentMap = (Map<Object, Object>) nextLevel;
            } else {
                return null;
            }
        }
        return currentMap.remove(keys[keys.length - 1]);
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

    public MapDifference<Object, Object> difference(NestedMap other) {
        return Maps.difference(root, other.root);
    }

    // --- IMMUTABILITY ---

    /**
     * Creates a deep, immutable copy of this map. The resulting map is read-only and thread-safe
     * for reads.
     *
     * @return A new ImmutableNestedMap instance.
     */
    public NestedMap toImmutable() {
        Map<Object, Object> immutableRoot = deepUnmodifiableCopy(this.root, this.mapFactory);
        return new NestedMap(immutableRoot);
    }

    /**
     * Recursively performs a deep copy of a map structure, wrapping every level in an unmodifiable
     * map view.
     */
    private static Map<Object, Object> deepUnmodifiableCopy(
            Map<Object, Object> original, Supplier<Map<Object, Object>> mapFactory) {
        if (original == null) return null;

        // Create a new map of the same underlying type (e.g., TreeMap) to preserve order.
        Map<Object, Object> copy = mapFactory.get();
        for (Map.Entry<Object, Object> entry : original.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                copy.put(
                        entry.getKey(),
                        deepUnmodifiableCopy((Map<Object, Object>) value, mapFactory));
            } else {
                copy.put(entry.getKey(), value);
            }
        }
        return ImmutableMap.copyOf(copy);
    }

    // Streaming interface

    /**
     * Returns a LAZY stream of all terminal entries in the nested map. The stream is built
     * on-the-fly and does not store all entries in memory. Each entry is a list of objects, where
     * the last element is the value.
     */
    public Stream<List<Object>> stream() {
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
        private List<Object> list;

        public Entry3(List<Object> list) {
            if (list == null || list.size() != 3) {
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

        public V getValue() {
            return (V) list.get(3);
        }

        @Override
        public String toString() {
            return list.toString();
        }

        public boolean equals(Object obj) {
            return this == obj || obj instanceof Entry3 && this.list == ((Entry3) obj).list;
        }

        @Override
        public int hashCode() {
            return list.hashCode();
        }
    }

    @SuppressWarnings("unchecked")
    public static class Entry4<K1, K2, K3, V> { // With Java17 we could use records
        private List<Object> list;

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
            return this == obj || obj instanceof Entry4 && this.list == ((Entry4) obj).list;
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
        public static <K1, K2, V> Map2<K1, K2, V> create(Supplier<Map<Object, Object>> supplier) {
            return new Map2<>(new NestedMap(supplier));
        }

        @Override
        public String toString() {
            return engine.toString();
        }

        @Override
        public boolean equals(Object obj) {
            return engine.equals(((Map2) obj).engine);
        }

        @Override
        public int hashCode() {
            return engine.hashCode();
        }

        public void put(K1 key1, K2 key2, V value) {
            engine.put(key1, key2, value);
        }

        public V get(K1 key1, K2 key2) {
            return (V) engine.get(key1, key2);
        }

        public V remove(K1 key1, K2 key2) {
            return (V) engine.remove(key1, key2);
        }

        public Stream<Entry3<K1, K2, V>> stream() {
            return engine.stream().map(list -> new Entry3<K1, K2, V>(list));
        }

        public ImmutableMap2<K1, K2, V> createImmutable() {
            return new ImmutableMap2<K1, K2, V>(engine);
        }
    }

    public static class ImmutableMap2<K1, K2, V> extends Map2<K1, K2, V> {

        private ImmutableMap2(NestedMap engine) {

            super(engine.toImmutable());
        }

        public void put(K1 key1, K2 key2, V value) {
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
        public static <K1, K2, K3, V> Map3<K1, K2, K3, V> create(
                Supplier<Map<Object, Object>> supplier) {
            return new Map3<>(new NestedMap(supplier));
        }

        public void put(K1 key1, K2 key2, K3 key3, V value) {
            engine.put(key1, key2, key3, value);
        }

        public V get(K1 key1, K2 key2, K3 key3) {
            return (V) engine.get(key1, key2, key3);
        }

        public V remove(K1 key1, K2 key2, K3 key3) {
            return (V) engine.remove(key1, key2, key3);
        }

        @Override
        public String toString() {
            return engine.toString();
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || engine.equals(((Map3) obj).engine);
        }

        @Override
        public int hashCode() {
            return engine.hashCode();
        }

        public Stream<Entry4<K1, K2, K3, V>> stream() {
            return engine.stream().map(list -> new Entry4<K1, K2, K3, V>(list));
        }

        public ImmutableMap3<K1, K2, K3, V> createImmutable() {
            return new ImmutableMap3<K1, K2, K3, V>(engine);
        }
    }

    public static class ImmutableMap3<K1, K2, K3, V> extends Map3<K1, K2, K3, V> {

        private ImmutableMap3(NestedMap engine) {

            super(engine.toImmutable());
        }

        public void put(K1 key1, K2 key2, K3 key3, V value) {
            throw new UnsupportedOperationException();
        }
    }

    public static class Multimap2<K1, K2, K3> {
        private final NestedMap engine;

        private Multimap2(NestedMap engine) {
            this.engine = engine;
        }

        /** Takes Treemap::new, HashMap::new, ConcurrentHashMap::new, and other suppliers */
        public static <K1, K2, K3> Multimap2<K1, K2, K3> create(
                Supplier<Map<Object, Object>> supplier) {
            return new Multimap2<>(new NestedMap(supplier));
        }

        public void put(K1 key1, K2 key2, K3 key3) {
            engine.put(key1, key2, key3, true);
        }

        public boolean contains(K1 key1, K2 key2, K3 key3) {
            return engine.get(key1, key2, key3) != null;
        }

        public boolean remove(K1 key1, K2 key2, K3 key3) {
            return engine.remove(key1, key2, key3) != null;
        }

        @Override
        public String toString() {
            return engine.toString();
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || engine.equals(((Multimap2) obj).engine);
        }

        @Override
        public int hashCode() {
            return engine.hashCode();
        }

        public Stream<Entry3<K1, K2, K3>> stream() {
            return engine.stream().map(list -> new Entry3<K1, K2, K3>(list));
        }

        public ImmutableMultimap2<K1, K2, K3> createImmutable() {
            return new ImmutableMultimap2<K1, K2, K3>(engine);
        }
    }

    public static class ImmutableMultimap2<K1, K2, K3> extends Multimap2<K1, K2, K3> {

        private ImmutableMultimap2(NestedMap engine) {
            super(engine.toImmutable());
        }

        public void add(K1 key1, K2 key2, K3 key3) {
            throw new UnsupportedOperationException();
        }
    }

    /*
        // --- TBD: flesh out similar to the above ---

        public static class NestedMap4<K1, K2, K3, K4, V> {
            private final NestedMap engine;

            private NestedMap4(NestedMap engine) {
                this.engine = engine;
            }

            public static <K1, K2, K3, K4, V> NestedMap4<K1, K2, K3, K4, V> createWithTreeMaps() {
                return new NestedMap4<>(new NestedMap(TreeMap::new));
            }

            public static <K1, K2, K3, K4, V> NestedMap4<K1, K2, K3, K4, V> createWithHashMaps() {
                return new NestedMap4<>(new NestedMap(HashMap::new));
            }

            public static <K1, K2, K3, K4, V>
                    NestedMap4<K1, K2, K3, K4, V> createWithConcurrentHashMaps() {
                return new NestedMap4<>(new NestedMap(ConcurrentHashMap::new));
            }

            public void put(K1 key1, K2 key2, K3 key3, K4 key4, V value) {
                engine.put(key1, key2, key3, key4, value);
            }

            @SuppressWarnings("unchecked")
            public V get(K1 key1, K2 key2, K3 key3, K4 key4) {
                return (V) engine.get(key1, key2, key3, key4);
            }

            @SuppressWarnings("unchecked")
            public V remove(K1 key1, K2 key2, K3 key3, K4 key4) {
                return (V) engine.remove(key1, key2, key3, key4);
            }

            @Override
            public String toString() {
                return engine.toString();
            }
    <<<<<<< Updated upstream

    =======


            // TODO finish up along the lines of NestedMap2

        }

    <<<<<<< Updated upstream
        // --- SHIM FOR 5-LEVEL MAP ---
    =======
        // --- TBD: flesh out similar to the above ---

        public static class NestedMap5<K1, K2, K3, K4, K5, V> {
            private final NestedMap engine;

            private NestedMap5(NestedMap engine) {
                this.engine = engine;
            }

            public static <
                            K1 extends Comparable<K1>,
                            K2 extends Comparable<K2>,
                            K3 extends Comparable<K3>,
                            K4 extends Comparable<K4>,
                            K5 extends Comparable<K5>,
                            V>
                    NestedMap5<K1, K2, K3, K4, K5, V> createWithTreeMaps() {
                return new NestedMap5<>(new NestedMap(TreeMap::new));
            }

            public static <K1, K2, K3, K4, K5, V>
                    NestedMap5<K1, K2, K3, K4, K5, V> createWithHashMaps() {
                return new NestedMap5<>(new NestedMap(HashMap::new));
            }

            public static <K1, K2, K3, K4, K5, V>
                    NestedMap5<K1, K2, K3, K4, K5, V> createWithConcurrentHashMaps() {
                return new NestedMap5<>(new NestedMap(ConcurrentHashMap::new));
            }

            public void put(K1 key1, K2 key2, K3 key3, K4 key4, K5 key5, V value) {
                engine.put(key1, key2, key3, key4, key5, value);
            }

            @SuppressWarnings("unchecked")
            public V get(K1 key1, K2 key2, K3 key3, K4 key4, K5 key5) {
                return (V) engine.get(key1, key2, key3, key4, key5);
            }

            @SuppressWarnings("unchecked")
            public V remove(K1 key1, K2 key2, K3 key3, K4 key4, K5 key5) {
                return (V) engine.remove(key1, key2, key3, key4, key5);
            }

            @Override
            public String toString() {
                return engine.toString();
            }
    <<<<<<< Updated upstream

            // TODO finish up along the lines of NestedMap2
        }

        // --- SHIM FOR 6-LEVEL MAP ---
    =======
        }

        // --- TBD: flesh out similar to the above ---

        public static class NestedMap6<K1, K2, K3, K4, K5, K6, V> {
            private final NestedMap engine;

            private NestedMap6(NestedMap engine) {
                this.engine = engine;
            }

            public static <K1, K2, K3, K4, K5, K6, V>
                    NestedMap6<K1, K2, K3, K4, K5, K6, V> createWithTreeMaps() {
                return new NestedMap6<>(new NestedMap(TreeMap::new));
            }

            public static <K1, K2, K3, K4, K5, K6, V>
                    NestedMap6<K1, K2, K3, K4, K5, K6, V> createWithHashMaps() {
                return new NestedMap6<>(new NestedMap(HashMap::new));
            }

            public static <K1, K2, K3, K4, K5, K6, V>
                    NestedMap6<K1, K2, K3, K4, K5, K6, V> createWithConcurrentHashMaps() {
                return new NestedMap6<>(new NestedMap(ConcurrentHashMap::new));
            }

            public void put(K1 key1, K2 key2, K3 key3, K4 key4, K5 key5, K6 key6, V value) {
                engine.put(key1, key2, key3, key4, key5, key6, value);
            }

            @SuppressWarnings("unchecked")
            public V get(K1 key1, K2 key2, K3 key3, K4 key4, K5 key5, K6 key6) {
                return (V) engine.get(key1, key2, key3, key4, key5, key6);
            }

            @SuppressWarnings("unchecked")
            public V remove(K1 key1, K2 key2, K3 key3, K4 key4, K5 key5, K6 key6) {
                return (V) engine.remove(key1, key2, key3, key4, key5, key6);
            }

            @Override
            public String toString() {
                return engine.toString();
            }

    <<<<<<< Updated upstream
            // TODO finish up along the lines of NestedMap2

        }
    =======
            // --- TBD: flesh out similar to the above ---
        }
        */
}

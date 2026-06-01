package org.unicode.cldr.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A thread-safe implementation of a Map of Map, of Map. This class uses ConcurrentHashMap at each
 * level to ensure that operations are thread-safe and can be performed concurrently without
 * external synchronization.
 *
 * @param <K1> The type of the first-level key.
 * @param <K2> The type of the second-level key.
 * @param <K3> The type of the third-level key.
 * @param <V> The type of the value.
 */
public class ThreadSafeMapOfMapOfMap<K1, K2, K3, V> {
    @FunctionalInterface
    public interface TriFunction<K1, K2, K3, V> {
        V apply(K1 key1, K2 key2, K3 key3);
    }

    // The outermost map, mapping K1 to a ConcurrentMap of K2 to a ConcurrentMap of K3 to V.
    private final ConcurrentMap<K1, ConcurrentMap<K2, ConcurrentMap<K3, V>>> map1;

    /** Constructs a new ThreadSafeMapOfMapOfMap. */
    public ThreadSafeMapOfMapOfMap() {
        this.map1 = new ConcurrentHashMap<>();
    }

    /**
     * Retrieves a value from the nested map at the specified keys. If it is absent from the map,
     * use the provided function to compute the example html and add it to the map. If an
     * intermediate map does not exist, it will be created.
     *
     * @param key1 The first-level key.
     * @param key2 The second-level key.
     * @param key3 The third-level key.
     * @return The value associated with the keys.
     */
    public V computeIfAbsent(K1 key1, K2 key2, K3 key3, TriFunction<K1, K2, K3, V> f) {
        // Get or create the second-level and third-level maps.
        // computeIfAbsent is thread-safe and ensures that the mapping function
        // is applied only once if the key is absent.
        return map1.computeIfAbsent(key1, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(key2, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(key3, k -> f.apply(key1, key2, k));
    }

    /**
     * Removes a value from the nested map at the specified keys. The intermediate map is not
     * removed even if it becomes empty.
     *
     * @param key1 The first-level key.
     * @param key2 The second-level key, or null to remove the first-level key.
     * @param key3 The third-level key, or null to clear the third-level map for key1 and key2.
     * @return The value that was removed, or null if the key path did not exist or key2 or key3 is
     *     null.
     */
    public V remove(K1 key1, K2 key2, K3 key3) {
        ConcurrentMap<K2, ConcurrentMap<K3, V>> map2 = map1.get(key1);
        if (map2 == null) {
            return null;
        }
        if (key2 == null) {
            map1.remove(key1);
            return null;
        }
        ConcurrentMap<K3, V> map3 = map2.get(key2);
        if (map3 == null) {
            return null;
        }
        if (key3 == null) {
            map3.clear();
            return null;
        }
        return map3.remove(key3);
    }

    /** Clears all mappings from this nested map. */
    public void clear() {
        map1.clear();
    }
}

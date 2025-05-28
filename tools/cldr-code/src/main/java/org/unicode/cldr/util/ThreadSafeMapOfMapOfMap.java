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

    public static boolean verbose = false;

    // The outermost map, mapping K1 to a ConcurrentMap of K2 to a ConcurrentMap of K3 to V.
    private final ConcurrentMap<K1, ConcurrentMap<K2, ConcurrentMap<K3, V>>> map1;

    /** Constructs a new ThreadSafeMapOfMapOfMap. */
    public ThreadSafeMapOfMapOfMap() {
        this.map1 = new ConcurrentHashMap<>();
    }

    /**
     * Puts a value into the nested map at the specified keys. If the intermediate map does not
     * exist, it will be created.
     *
     * @param key1 The first-level key.
     * @param key2 The second-level key.
     * @param key3 The third-level key.
     * @param value The value to be stored.
     */
    public void put(K1 key1, K2 key2, K3 key3, V value) {
        // Get or create the second-level and third-level maps.
        // computeIfAbsent is thread-safe and ensures that the mapping function
        // is applied only once if the key is absent.
        ConcurrentMap<K2, ConcurrentMap<K3, V>> map2 =
                map1.computeIfAbsent(key1, k -> new ConcurrentHashMap<>());
        ConcurrentMap<K3, V> map3 = map2.computeIfAbsent(key2, k -> new ConcurrentHashMap<>());

        // Put the value into the third-level map.
        map3.put(key3, value);
    }

    /**
     * Retrieves a value from the nested map at the specified keys.
     *
     * @param key1 The first-level key.
     * @param key2 The second-level key.
     * @param key3 The third-level key.
     * @return The value associated with the keys, or null if any key path does not exist.
     */
    public V get(K1 key1, K2 key2, K3 key3) {
        // Get the second-level map. If it doesn't exist, return null.
        ConcurrentMap<K2, ConcurrentMap<K3, V>> map2 = map1.get(key1);
        if (map2 == null) {
            if (verbose) {
                System.out.println("ThreadSafeMapOfMap.get map2 = null for key1 = " + key1);
            }
            return null;
        }
        // Get the third-level map. If it doesn't exist, return null.
        ConcurrentMap<K3, V> map3 = map2.get(key2);
        if (map3 == null) {
            if (verbose) {
                System.out.println("ThreadSafeMapOfMap.get map3 = null for key2 = " + key2);
            }
            return null;
        }
        // Get the value from the third-level map.
        V value = map3.get(key3);
        if (verbose && value == null) {
            System.out.println("ThreadSafeMapOfMap.get value = null for key3 = " + key3);
        }
        return value;
    }

    /**
     * Removes a value from the nested map at the specified keys. The intermediate map is not
     * removed even if it becomes empty.
     *
     * @param key1 The first-level key.
     * @param key2 The second-level key.
     * @param key3 The third-level key, or null.
     * @return The value that was removed, or null if the key path did not exist.
     */
    public V remove(K1 key1, K2 key2, K3 key3) {
        ConcurrentMap<K2, ConcurrentMap<K3, V>> map2 = map1.get(key1);
        if (map2 == null) {
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

    /**
     * Checks if the nested map contains a value at the specified keys.
     *
     * @param key1 The first-level key.
     * @param key2 The second-level key.
     * @param key3 The third-level key.
     * @return true if the value exists, false otherwise.
     */
    public boolean containsKey(K1 key1, K2 key2, K3 key3) {
        ConcurrentMap<K2, ConcurrentMap<K3, V>> map2 = map1.get(key1);
        if (map2 == null) {
            return false;
        }
        ConcurrentMap<K3, V> map3 = map2.get(key2);
        if (map3 == null) {
            return false;
        }
        return map3.containsKey(key3);
    }

    /**
     * Checks if the nested map is empty.
     *
     * @return true if the map contains no key-value mappings, false otherwise.
     */
    public boolean isEmpty() {
        return map1.isEmpty();
    }

    /** Clears all mappings from this nested map. */
    public void clear() {
        map1.clear();
    }
}

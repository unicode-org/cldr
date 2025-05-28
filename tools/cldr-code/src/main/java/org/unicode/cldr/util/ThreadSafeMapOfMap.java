package org.unicode.cldr.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A thread-safe implementation of a Map of Map.
 * This class uses ConcurrentHashMap at each level to ensure that
 * operations are thread-safe and can be performed concurrently
 * without external synchronization.
 *
 * @param <K1> The type of the first-level key.
 * @param <K2> The type of the second-level key.
 * @param <V>  The type of the value.
 */
public class ThreadSafeMapOfMap<K1, K2, V> {

    // The outermost map, mapping K1 to a ConcurrentMap of K2 to V.
    private final ConcurrentMap<K1, ConcurrentMap<K2, V>> map1;

    /**
     * Constructs a new ThreadSafeMapOfMap.
     */
    public ThreadSafeMapOfMap() {
        this.map1 = new ConcurrentHashMap<>();
    }

    /**
     * Puts a value into the nested map at the specified keys.
     * If the intermediate map does not exist, it will be created.
     *
     * @param key1 The first-level key.
     * @param key2 The second-level key.
     * @param value The value to be stored.
     */
    public void put(K1 key1, K2 key2, V value) {
        // Get or create the second-level map.
        // computeIfAbsent is thread-safe and ensures that the mapping function
        // is applied only once if the key is absent.
        ConcurrentMap<K2, V> map2 = map1.computeIfAbsent(key1, k -> new ConcurrentHashMap<>());

        // Put the value into the second-level map.
        map2.put(key2, value);
    }

    /**
     * Retrieves a value from the nested map at the specified keys.
     *
     * @param key1 The first-level key.
     * @param key2 The second-level key.
     * @return The value associated with the keys, or null if any key path does not exist.
     */
    public V get(K1 key1, K2 key2) {
        // Get the second-level map. If it doesn't exist, return null.
        ConcurrentMap<K2, V> map2 = map1.get(key1);
        if (map2 == null) {
            return null;
        }

        // Get the value from the second-level map.
        return map2.get(key2);
    }

    /**
     * Removes a value from the nested map at the specified keys.
     * The intermediate map is not removed even if it becomes empty.
     *
     * @param key1 The first-level key.
     * @param key2 The second-level key.
     * @return The value that was removed, or null if the key path did not exist.
     */
    public V remove(K1 key1, K2 key2) {
        ConcurrentMap<K2, V> map2 = map1.get(key1);
        if (map2 == null) {
            return null;
        }
        return map2.remove(key2);
    }

    /**
     * Removes the entire first-level map associated with key1.
     *
     * @param key1 The first-level key.
     * @return The ConcurrentMap<K2, V> that was removed, or null if the key did not exist.
     */
    public ConcurrentMap<K2, V> removeFirstLevelMap(K1 key1) {
        return map1.remove(key1);
    }

    /**
     * Checks if the nested map contains a value at the specified keys.
     *
     * @param key1 The first-level key.
     * @param key2 The second-level key.
     * @return true if the value exists, false otherwise.
     */
    public boolean containsKey(K1 key1, K2 key2) {
        ConcurrentMap<K2, V> map2 = map1.get(key1);
        if (map2 == null) {
            return false;
        }
        return map2.containsKey(key2);
    }

    /**
     * Returns the approximate number of key-value mappings in this nested map.
     * This is not guaranteed to be exact due to the concurrent nature of the underlying maps.
     *
     * @return The approximate size.
     */
    public int size() {
        int totalSize = 0;
        for (ConcurrentMap<K2, V> map2 : map1.values()) {
            totalSize += map2.size();
        }
        return totalSize;
    }

    /**
     * Checks if the nested map is empty.
     *
     * @return true if the map contains no key-value mappings, false otherwise.
     */
    public boolean isEmpty() {
        return map1.isEmpty();
    }

    /**
     * Clears all mappings from this nested map.
     */
    public void clear() {
        map1.clear();
    }
}

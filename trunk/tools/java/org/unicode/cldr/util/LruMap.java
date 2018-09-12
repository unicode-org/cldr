package org.unicode.cldr.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A Map that keeps a fixed number of key-value pairs and kicks pairs out in least-recently-used order.
 *
 * @deprecated This class has been deprecated; the project now includes GUAVA. which  offer Cache implementations
 * that can be used instead
 *
 * @author jchye
 *
 * @param <K>
 *            the key type
 * @param <V>
 *            the value type
 *
 */
@Deprecated
public class LruMap<K, V> extends LinkedHashMap<K, V> {

    private static final boolean DEBUG_LRU_MAP = false;
    private static final long serialVersionUID = -9176469448381227725L;
    private int cacheSize;

    @Deprecated
    public LruMap(int cacheSize) {
        super(cacheSize, 1, true);
        this.cacheSize = cacheSize;
        if (DEBUG_LRU_MAP) {
            System.out.println(System.currentTimeMillis() + " " + getClass().getCanonicalName() + ": Instantiated LRUMap with size " + cacheSize);
        }
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        boolean shouldRemove = (size() > this.cacheSize);
        if (DEBUG_LRU_MAP) {
            if (shouldRemove) {
                System.out.println(System.currentTimeMillis() + " " + getClass().getCanonicalName() + ": removing entry for key " + eldest.getKey().toString());
                System.out.println(System.currentTimeMillis() + " " + getClass().getCanonicalName() + ": Old Map size is: " + size());
            }
        }
        return shouldRemove;
    }

    @Override
    public V put(K key, V value) {
        if (DEBUG_LRU_MAP) {
            System.out.println(System.currentTimeMillis() + " " + getClass().getCanonicalName() + ": Adding value for key " + key.toString());
            System.out.println(System.currentTimeMillis() + " " + getClass().getCanonicalName() + "  Old map size is: " + size());
        }
        return super.put(key, value);

    }
}

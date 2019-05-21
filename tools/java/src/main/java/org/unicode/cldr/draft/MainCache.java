package org.unicode.cldr.draft;

import java.util.HashMap;
import java.util.Map;

public abstract class MainCache {
    private static Map<Class<?>, Map<Object, Object>> cache = new HashMap<Class<?>, Map<Object, Object>>();

    protected synchronized Object get(Object key) {
        Class<?> classKey = this.getClass();
        Map<Object, Object> submap = cache.get(classKey);
        if (submap == null) {
            cache.put(classKey, submap = new HashMap<Object, Object>());
        }
        Object result = submap.get(key);
        if (result == null) {
            result = createObject(key);
            submap.put(key, result);
        }
        return result;
    }

    abstract protected Object createObject(Object key);
}

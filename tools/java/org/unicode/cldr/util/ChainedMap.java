package org.unicode.cldr.util;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.impl.Row.R4;

public class ChainedMap {

    public static class M3<K2, K1, V> extends ChainedMap implements Iterable<Map.Entry<K2, Map<K1, V>>> {
        @SuppressWarnings("unchecked")
        private M3(Map<K2, Object> map2, Map<K1, Object> map1, Class<V> valueClass) {
            super(map2, map1);
        }

        private M3(Map<?, ?> map2, Constructor<Map<Object, Object>>[] constructors, int indexStart) {
            super(map2, constructors, indexStart);
        }

        @SuppressWarnings("unchecked")
        public V get(K2 key2, K1 key1) {
            return (V) super.handleGet(key2, key1);
        }

        @SuppressWarnings("unchecked")
        public Map<K1, V> get(K2 key2) {
            Map<K1, V> map = (Map<K1, V>) super.mapBase.get(key2);
            return map == null ? null : Collections.unmodifiableMap(map);
        }

        @SuppressWarnings("unchecked")
        public V put(K2 key2, K1 key1, V value) {
            return (V) super.handlePut(value, key2, key1);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Iterator<Entry<K2, Map<K1, V>>> iterator() {
            return (Iterator<Entry<K2, Map<K1, V>>>) super.iterator();
        }

        public Iterable<Row.R3<K2, K1, V>> rows() {
            List<R3<K2, K1, V>> result = new ArrayList<R3<K2, K1, V>>();
            for (Entry<Object, Object> entry0 : super.mapBase.entrySet()) {
                for (Entry<Object, Object> entry1 : ((Map<Object, Object>) entry0.getValue()).entrySet()) {
                    R3<K2, K1, V> item = (R3<K2, K1, V>) Row.of(entry0.getKey(), entry1.getKey(), entry1.getValue());
                    result.add(item);
                }
            }
            return result;
        }

        public Set<K2> keySet() {
            return (Set<K2>) super.mapBase.keySet();
        }
    }

    public static class M4<K3, K2, K1, V> extends ChainedMap implements Iterable<Map.Entry<K3, Map<K2, Map<K1, V>>>> {
        @SuppressWarnings("unchecked")
        private M4(Map<K3, Object> map3, Map<K2, Object> map2, Map<K1, Object> map1, Class<V> valueClass) {
            super(map3, map2, map1);
        }

        @SuppressWarnings("unchecked")
        public V get(K3 key3, K2 key2, K1 key1) {
            return (V) super.handleGet(key3, key2, key1);
        }

        @SuppressWarnings("unchecked")
        public M3<K2, K1, V> get(K3 key3) {
            return new M3((Map<?, ?>) super.handleGet(key3), super.mapConstructors, super.indexStart + 1);
        }

        @SuppressWarnings("unchecked")
        public V put(K3 key3, K2 key2, K1 key1, V value) {
            return (V) super.handlePut(value, key3, key2, key1);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Iterator<Entry<K3, Map<K2, Map<K1, V>>>> iterator() {
            return (Iterator<Entry<K3, Map<K2, Map<K1, V>>>>) super.iterator();
        }

        public Iterable<Row.R4<K3, K2, K1, V>> rows() {
            List<R4<K3, K2, K1, V>> result = new ArrayList<R4<K3, K2, K1, V>>();
            for (Entry<Object, Object> entry0 : super.mapBase.entrySet()) {
                for (Entry<Object, Object> entry1 : ((Map<Object, Object>) entry0.getValue()).entrySet()) {
                    for (Entry<Object, Object> entry2 : ((Map<Object, Object>) entry1.getValue()).entrySet()) {
                        R4<K3, K2, K1, V> item = (R4<K3, K2, K1, V>) Row.of(entry0.getKey(), entry1.getKey(), entry2.getValue(), entry2.getValue());
                        result.add(item);
                    }
                }
            }
            return result;
        }

        public Set<K3> keySet() {
            return (Set<K3>) super.mapBase.keySet();
        }
    }

    private final Map<Object, Object> mapBase;
    private final Constructor<Map<Object, Object>>[] mapConstructors;
    private final int indexStart;

    @SuppressWarnings("unchecked")
    private ChainedMap(Map<? extends Object, ? extends Object>... maps) {
        this(maps[0], constructorList(maps), 0);
    }

    @SuppressWarnings("unchecked")
    private ChainedMap(Map<?, ?> mapBase, Constructor<Map<Object, Object>>[] mapConstructors, int indexStart) {
        this.mapBase = (Map<Object, Object>) mapBase;
        this.mapConstructors = mapConstructors;
        this.indexStart = indexStart;
    }

    private static Constructor<Map<Object, Object>>[] constructorList(Map<? extends Object, ? extends Object>... maps) {
        Constructor<Map<Object, Object>>[] tempMapConstructors = new Constructor[maps.length];
        items: for (int i = 0; i < maps.length; ++i) {
            for (Constructor<?> constructor : maps[i].getClass().getConstructors()) {
                if (constructor.getParameterTypes().length == 0) {
                    tempMapConstructors[i] = (Constructor<Map<Object, Object>>) constructor;
                    continue items;
                }
            }
            throw new IllegalArgumentException("Couldn't create empty constructor for " + maps[i]);
        }
        return tempMapConstructors;
    }

    @SuppressWarnings("unchecked")
    public static <K2, K1, V> M3<K2, K1, V> of(Map<K2, Object> map2, Map<K1, Object> map1, Class<V> valueClass) {
        return new M3<K2, K1, V>(map2, map1, valueClass);
    }

    @SuppressWarnings("unchecked")
    public static <K3, K2, K1, V> M4<K3, K2, K1, V> of(Map<K3, Object> map3, Map<K2, Object> map2, Map<K1, Object> map1, Class<V> valueClass) {
        return new M4<K3, K2, K1, V>(map3, map2, map1, valueClass);
    }

    private Object iterator() {
        return mapBase.entrySet().iterator();
    }

    @SuppressWarnings("unchecked")
    private Object handleGet(Object... keys) {
        Map<Object, Object> map = mapBase;
        int last = keys.length - 1;
        for (int i = 0; i < last; ++i) {
            Object key = keys[i];
            map = (Map<Object, Object>) map.get(key);
        }
        return map.get(keys[last]);
    }

    @SuppressWarnings("unchecked")
    private Object handlePut(Object value, Object... keys) {
        Map<Object, Object> map = mapBase;
        int last = keys.length - 1;
        for (int i = indexStart; i < last; ++i) {
            Object key = keys[i];
            Map<Object, Object> map2 = (Map<Object, Object>) map.get(key);
            if (map2 == null) {
                try {
                    map.put(key, map2 = mapConstructors[i].newInstance());
                } catch (Exception e) {
                    throw new IllegalArgumentException("Cannot create map with " + mapConstructors[i], e);
                }
            }
            map = map2;
        }
        return map.put(keys[last], value);
    }

    public static void main(String[] args) {
        M3<String, Integer, Double> foo = ChainedMap.of(new TreeMap<String, Object>(), new TreeMap<Integer, Object>(), Double.class);
        System.out.println(foo.put("abc", 3, 1.5));
        System.out.println(foo.get("abc", 3));

        System.out.println(foo.put("Def", 3, 1.5));
        System.out.println(foo.put("ghi", 3, 2.0));
        System.out.println(foo.put("ghi", 3, 3.0));
        System.out.println(foo.put("abc", 4, 1.5));
        for (Entry<String, Map<Integer, Double>> entry : foo) {
            System.out.println(entry);
        }
        for (R3<String, Integer, Double> entry : foo.rows()) {
            System.out.println(entry);
        }
    }
}

package org.unicode.cldr.util;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.impl.Row.R4;
import com.ibm.icu.impl.Row.R5;

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

        @SuppressWarnings("unchecked")
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

        @SuppressWarnings("unchecked")
        public Set<K2> keySet() {
            return (Set<K2>) super.mapBase.keySet();
        }
    }

    public static class M4<K3, K2, K1, V> extends ChainedMap implements Iterable<Map.Entry<K3, Map<K2, Map<K1, V>>>> {
        @SuppressWarnings("unchecked")
        private M4(Map<K3, Object> map3, Map<K2, Object> map2, Map<K1, Object> map1, Class<V> valueClass) {
            super(map3, map2, map1);
        }

        private M4(Map<?, ?> map2, Constructor<Map<Object, Object>>[] constructors, int indexStart) {
            super(map2, constructors, indexStart);
        }

        @SuppressWarnings("unchecked")
        public V get(K3 key3, K2 key2, K1 key1) {
            return (V) super.handleGet(key3, key2, key1);
        }

        public M3<K2, K1, V> get(K3 key3) {
            final Map<?, ?> submap = (Map<?, ?>) super.handleGet(key3);
            return submap == null ? null : new M3<K2, K1, V>(submap, super.mapConstructors, super.indexStart + 1);
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

        @SuppressWarnings("unchecked")
        public Iterable<Row.R4<K3, K2, K1, V>> rows() {
            List<R4<K3, K2, K1, V>> result = new ArrayList<R4<K3, K2, K1, V>>();
            for (Entry<Object, Object> entry0 : super.mapBase.entrySet()) {
                for (Entry<Object, Object> entry1 : ((Map<Object, Object>) entry0.getValue()).entrySet()) {
                    for (Entry<Object, Object> entry2 : ((Map<Object, Object>) entry1.getValue()).entrySet()) {
                        R4<K3, K2, K1, V> item = (R4<K3, K2, K1, V>) Row.of(entry0.getKey(), entry1.getKey(), entry2.getKey(), entry2.getValue());
                        result.add(item);
                    }
                }
            }
            return result;
        }

        @SuppressWarnings("unchecked")
        public Set<K3> keySet() {
            return (Set<K3>) super.mapBase.keySet();
        }
    }

    public static class M5<K4, K3, K2, K1, V> extends ChainedMap implements Iterable<Map.Entry<K4, Map<K3, Map<K2, Map<K1, V>>>>> {
        @SuppressWarnings("unchecked")
        private M5(Map<K4, Object> map4, Map<K3, Object> map3, Map<K2, Object> map2, Map<K1, Object> map1, Class<V> valueClass) {
            super(map4, map3, map2, map1);
        }

        @SuppressWarnings("unchecked")
        public V get(K4 key4, K3 key3, K2 key2, K1 key1) {
            return (V) super.handleGet(key4, key3, key2, key1);
        }

        public M4<K3, K2, K1, V> get(K4 key4) {
            final Map<?, ?> submap = (Map<?, ?>) super.handleGet(key4);
            return submap == null ? null
                : new M4<K3, K2, K1, V>(submap, super.mapConstructors, super.indexStart + 2);
        }

        @SuppressWarnings("unchecked")
        public V put(K4 key4, K3 key3, K2 key2, K1 key1, V value) {
            return (V) super.handlePut(value, key4, key3, key2, key1);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Iterator<Entry<K4, Map<K3, Map<K2, Map<K1, V>>>>> iterator() {
            return (Iterator<Entry<K4, Map<K3, Map<K2, Map<K1, V>>>>>) super.iterator();
        }

        @SuppressWarnings("unchecked")
        public Iterable<Row.R5<K4, K3, K2, K1, V>> rows() {
            List<R5<K4, K3, K2, K1, V>> result = new ArrayList<R5<K4, K3, K2, K1, V>>();
            for (Entry<Object, Object> entry0 : super.mapBase.entrySet()) {
                for (Entry<Object, Object> entry1 : ((Map<Object, Object>) entry0.getValue()).entrySet()) {
                    for (Entry<Object, Object> entry2 : ((Map<Object, Object>) entry1.getValue()).entrySet()) {
                        for (Entry<Object, Object> entry3 : ((Map<Object, Object>) entry2.getValue()).entrySet()) {
                            R5<K4, K3, K2, K1, V> item = (R5<K4, K3, K2, K1, V>) Row.of(
                                entry0.getKey(), entry1.getKey(), entry2.getKey(), entry3.getKey(), entry3.getValue());
                            result.add(item);
                        }
                    }
                }
            }
            return result;
        }

        @SuppressWarnings("unchecked")
        public Set<K4> keySet() {
            return (Set<K4>) super.mapBase.keySet();
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

    @SuppressWarnings("unchecked")
    private static Constructor<Map<Object, Object>>[] constructorList(Map<? extends Object, ? extends Object>... maps) {
        Constructor<Map<Object, Object>>[] tempMapConstructors = new Constructor[maps.length - 1];
        items: for (int i = 0; i < maps.length - 1; ++i) {
            for (Constructor<?> constructor : maps[i + 1].getClass().getConstructors()) {
                if (constructor.getParameterTypes().length == 0) {
                    tempMapConstructors[i] = (Constructor<Map<Object, Object>>) constructor;
                    continue items;
                }
            }
            throw new IllegalArgumentException("Couldn't create empty constructor for " + maps[i]);
        }
        return tempMapConstructors;
    }

    public static <T> Constructor<T> getEmptyConstructor(Class<T> c) {
        for (Constructor<?> constructor : c.getConstructors()) {
            if (constructor.getParameterTypes().length == 0) {
                return (Constructor<T>) constructor;
            }
        }
        return null;
    }

    public static <K2, K1, V> M3<K2, K1, V> of(Map<K2, Object> map2, Map<K1, Object> map1, Class<V> valueClass) {
        return new M3<K2, K1, V>(map2, map1, valueClass);
    }

    public static <K3, K2, K1, V> M4<K3, K2, K1, V> of(Map<K3, Object> map3, Map<K2, Object> map2, Map<K1, Object> map1, Class<V> valueClass) {
        return new M4<K3, K2, K1, V>(map3, map2, map1, valueClass);
    }

    public static <K4, K3, K2, K1, V> M5<K4, K3, K2, K1, V> of(
        Map<K4, Object> map4, Map<K3, Object> map3, Map<K2, Object> map2, Map<K1, Object> map1, Class<V> valueClass) {
        return new M5<K4, K3, K2, K1, V>(map4, map3, map2, map1, valueClass);
    }

    private Object iterator() {
        return mapBase.entrySet().iterator();
    }

    public void clear() {
        mapBase.clear();
    }

    @SuppressWarnings("unchecked")
    private Object handleGet(Object... keys) {
        Map<Object, Object> map = mapBase;
        int last = keys.length - 1;
        for (int i = 0; i < last; ++i) {
            Object key = keys[i];
            map = (Map<Object, Object>) map.get(key);
            if (map == null) {
                return null;
            }
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
        return value == null ? map.remove(keys[last]) : map.put(keys[last], value);
    }

    @Override
    public String toString() {
        return mapBase.toString();
    }

    public static void main(String[] args) {
        M5<Boolean, Byte, String, Integer, Double> foo = ChainedMap.of(
            new TreeMap<Boolean, Object>(),
            new HashMap<Byte, Object>(),
            new TreeMap<String, Object>(),
            new TreeMap<Integer, Object>(),
            Double.class);

        System.out.println(foo.put(true, (byte) 0, "abc", 3, 1.5));
        System.out.println(foo.get(true, (byte) 0, "abc", 3));

        System.out.println(foo.put(false, (byte) 0, "Def", 3, 1.5));
        System.out.println(foo.put(true, (byte) -1, "ghi", 3, 2.0));
        System.out.println(foo.put(true, (byte) 0, "ghi", 3, 3.0));
        System.out.println(foo.put(true, (byte) 0, "abc", 4, 1.5));

        for (Entry<Boolean, Map<Byte, Map<String, Map<Integer, Double>>>> entry : foo) {
            System.out.println("entries: " + entry);
        }
        for (R5<Boolean, Byte, String, Integer, Double> entry : foo.rows()) {
            System.out.println("rows: " + entry);
        }
    }
}

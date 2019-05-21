package org.unicode.cldr.util;

/**
 *******************************************************************************
 * Copyright (C) 1996-2001, Google, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Revision$
 *
 *******************************************************************************
 */

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public final class Counter2<T> implements Iterable<T>, Comparable<Counter2<T>> {
    Map<T, Double> map;
    Comparator<T> comparator;

    public Counter2() {
        this(null);
    }

    public Counter2(boolean naturalOrdering) {
        this(naturalOrdering ? new CldrUtility.ComparableComparator() : null);
    }

    public Counter2(Comparator<T> comparator) {
        if (comparator != null) {
            this.comparator = comparator;
            map = new TreeMap<T, Double>(comparator);
        } else {
            map = new LinkedHashMap<T, Double>();
        }
    }

    public Counter2<T> add(T obj, Double one2) {
        Double count = map.get(obj);
        if (count == null) {
            map.put(obj, one2);
        } else {
            map.put(obj, addN(count, one2));
        }
        return this;
    }

    public Counter2<T> put(T obj, Double one2) {
        map.put(obj, one2);
        return this;
    }

    public static Double ZERO = new Double(0);
    public static Double ONE = new Double(1);

    private Double addN(Double count, Double countValue) {
        // TODO Auto-generated method stub
        return count + countValue;
    }

    private int compare(Double a, Double b) {
        return a < b ? -1 : a > b ? 1 : 0;
    }

    public Double getCount(T obj) {
        Double count = map.get(obj);
        return count == null ? ZERO : count;
    }

    public Counter2<T> clear() {
        map.clear();
        return this;
    }

    public Number getTotal() {
        Double count = ZERO;
        for (T item : map.keySet()) {
            count = addN(count, map.get(item));
        }
        return count;
    }

    public int getItemCount() {
        return map.size();
    }

    private static class Entry<T> {
        double count;
        T value;
        int uniqueness;

        public Entry(Double count, T value, int uniqueness) {
            this.count = count.doubleValue();
            this.value = value;
            this.uniqueness = uniqueness;
        }
    }

    private static class EntryComparator<T> implements Comparator<Entry<T>> {
        int countOrdering;
        Comparator<T> byValue;

        public EntryComparator(boolean ascending, Comparator<T> byValue) {
            countOrdering = ascending ? 1 : -1;
            this.byValue = byValue;
        }

        public int compare(Entry<T> o1, Entry<T> o2) {
            if (o1.count != o2.count) {
                if (o1.count < o2.count) return -countOrdering;
                return countOrdering;
            }
            if (byValue != null) {
                return byValue.compare(o1.value, o2.value);
            }
            return o1.uniqueness - o2.uniqueness;
        }
    }

    public Set<T> getKeysetSortedByCount(boolean ascending) {
        return getKeysetSortedByCount(ascending, null);
    }

    public Set<T> getKeysetSortedByCount(boolean ascending, Comparator<T> byValue) {
        Set<Entry<T>> count_key = new TreeSet<Entry<T>>(new EntryComparator<T>(ascending, byValue));
        int counter = 0;
        for (T key : map.keySet()) {
            count_key.add(new Entry<T>(map.get(key), key, counter++));
        }
        Set<T> result = new LinkedHashSet<T>();
        for (Entry<T> entry : count_key) {
            result.add(entry.value);
        }
        return result;
    }

    public Set<T> getKeysetSortedByKey() {
        Set<T> s = new TreeSet<T>(comparator);
        s.addAll(map.keySet());
        return s;
    }

    // public Map<T,RWInteger> getKeyToKey() {
    // Map<T,RWInteger> result = new HashMap<T,RWInteger>();
    // Iterator<T> it = map.keySet().iterator();
    // while (it.hasNext()) {
    // Object key = it.next();
    // result.put(key, key);
    // }
    // return result;
    // }

    public Set<T> keySet() {
        return map.keySet();
    }

    public Iterator<T> iterator() {
        return map.keySet().iterator();
    }

    public Map<T, Double> getMap() {
        return map; // older code was protecting map, but not the integer values.
    }

    public int size() {
        return map.size();
    }

    public String toString() {
        return map.toString();
    }

    public Counter2<T> addAll(Collection<T> keys, Double delta) {
        for (T key : keys) {
            add(key, delta);
        }
        return this;
    }

    public Counter2<T> addAll(Counter2<T> keys) {
        for (T key : keys) {
            add(key, keys.getCount(key));
        }
        return this;
    }

    public int compareTo(Counter2<T> o) {
        Iterator<T> i = map.keySet().iterator();
        Iterator<T> j = o.map.keySet().iterator();
        while (true) {
            boolean goti = i.hasNext();
            boolean gotj = j.hasNext();
            if (!goti || !gotj) {
                return goti ? 1 : gotj ? -1 : 0;
            }
            T ii = i.next();
            T jj = i.next();
            int result = ((Comparable) ii).compareTo(jj);
            if (result != 0) {
                return result;
            }
            final Double iv = map.get(ii);
            final Double jv = o.map.get(jj);
            int comp = compare(iv, jv);
            if (comp != 0) return comp;
        }
    }

    public Counter2<T> increment(T key) {
        return add(key, ONE);
    }
}

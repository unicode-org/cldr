/**
 *******************************************************************************
 * Copyright (C) 1996-2001, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Date$
 * $Revision$
 *
 *******************************************************************************
 */

package org.unicode.cldr.util;


import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class Counter<T> implements Iterable<T> {
  Map<T,RWLong> map = new LinkedHashMap<T,RWLong>();

  static public final class RWLong implements Comparable<RWLong> {
    // the uniqueCount ensures that two different RWIntegers will always be different
    static int uniqueCount;
    public long value;
    private final int forceUnique;
    {
      synchronized (RWLong.class) { // make thread-safe
        forceUnique = uniqueCount++;
      }
    }

    public int compareTo(RWLong that) {
      if (that.value < value) return -1;
      if (that.value > value) return 1;
      if (this == that) return 0;
      synchronized (this) { // make thread-safe
        if (that.forceUnique < forceUnique) return -1;
      }
      return 1; // the forceUnique values must be different, so this is the only remaining case
    }
    public String toString() {
      return String.valueOf(value);
    }
  }

  public void add(T obj, long countValue) {
    RWLong count = map.get(obj);
    if (count == null) map.put(obj, count = new RWLong());
    count.value += countValue;
  }

  public long getCount(T obj) {
    RWLong count = map.get(obj);
    return count == null ? 0 : count.value;
  }

  public void clear() {
    map.clear();
  }

  public long getTotal() {
    long count = 0;
    for (T item : map.keySet()) {
      count += map.get(item).value;
    }
    return count;
  }

  public int getItemCount() {
    return map.size();
  }
  
  private static class Entry<T> {
    RWLong count;
    T value;
    int uniqueness;
    public Entry(RWLong count, T value, int uniqueness) {
      this.count = count;
      this.value = value;
      this.uniqueness = uniqueness;
    }
  }
  
  private static class EntryComparator<T> implements Comparator<Entry<T>>{
    int ordering;
    Comparator<T> byValue;
    
    public EntryComparator(boolean ascending, Comparator<T> byValue) {
      ordering = ascending ? 1 : -1;
      this.byValue = byValue;
    }
    public int compare(Entry<T> o1, Entry<T> o2) {
      if (o1.count.value < o2.count.value) return -ordering;
      if (o1.count.value > o2.count.value) return ordering;
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
    return new TreeSet<T>(map.keySet());
  }

  public Set<T> getKeysetSortedByKey(Comparator<T> comparator) {
    Set<T> s = new TreeSet<T>(comparator);
    s.addAll(map.keySet());
    return s;
  }


//public Map<T,RWInteger> getKeyToKey() {
//Map<T,RWInteger> result = new HashMap<T,RWInteger>();
//Iterator<T> it = map.keySet().iterator();
//while (it.hasNext()) {
//Object key = it.next();
//result.put(key, key);
//}
//return result;
//}

  public Set<T> keySet() {
    return map.keySet();
  }

  public Iterator<T> iterator() {
    return map.keySet().iterator();
  }

  public Map<T, RWLong> getMap() {
    return map; // older code was protecting map, but not the integer values.
  }

  public int size() {
    return map.size();
  }

  public String toString() {
    return map.toString();
  }
}
package org.unicode.cldr.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

public class Relation<K, V> implements Map<K, V> {
  private Map<K, Set<V>> data;
  
  Constructor<Set<V>> setCreator;
  Object[] setComparatorParam;
  
  public Relation(Map<K, Set<V>> map, Class<Set<V>> setCreator, Comparator setComparator) {
    try {
      setComparatorParam = setComparator == null ? null : new Object[]{setComparator};
      this.setCreator = setCreator.getConstructor();
      this.setCreator.newInstance(setComparatorParam); // check to make sure compiles
      data = map == null ? new HashMap() : map;     
    } catch (Exception e) {
      throw (RuntimeException) new IllegalArgumentException("Can't create new set").initCause(e);
    }
    
  }
  
  public void clear() {
    data.clear();
  }
  
  public boolean containsKey(Object key) {
    return data.containsKey(key);
  }
  
  public boolean containsValue(Object value) {
    for (Set<V> values : data.values()) {
      if (values.contains(value))
        return true;
    }
    return false;
  }
  
  public Set<Entry<K, V>> entrySet() {
    Set<Entry<K, V>> result = new LinkedHashSet();
    for (K key : data.keySet()) {
      for (V value : data.get(key)) {
        result.add(new SimpleEntry(key, value));
      }
    }
    return result;
  }
  
  public boolean equals(Object o) {
    if (o == null)
      return false;
    if (o.getClass() != this.getClass())
      return false;
    return data.equals(((Relation) o).data);
  }
  
  public V get(Object key) {
    Set<V> set = data.get(key);
    if (set == null || set.size() == 0)
      return null;
    return set.iterator().next();
  }
  
  public Set<V> getAll(Object key) {
    return data.get(key);
  }
  
  public int hashCode() {
    return data.hashCode();
  }
  
  public boolean isEmpty() {
    return data.isEmpty();
  }
  
  public Set<K> keySet() {
    return data.keySet();
  }
  
  public V put(K key, V value) {
    Set<V> set = data.get(key);
    if (set == null) {
      data.put(key, set = newSet());
    }
    set.add(value);
    return null;
  }
  
  private Set<V> newSet() {
    try {
      return (Set<V>) setCreator.newInstance(setComparatorParam);
    } catch (Exception e) {
      throw (RuntimeException) new IllegalArgumentException("Can't create new set").initCause(e);
    }
  }
  
  public void putAll(Map<? extends K, ? extends V> t) {
    for (K key : t.keySet()) {
      put(key, t.get(key));
    }
  }
  
  public void putAll(Relation<? extends K, ? extends V> t) {
    for (K key : t.keySet()) {
      for (V value : t.getAll(key)) {
        put(key, value);
      }
    }
  }
  
  public V remove(Object key) {
    data.remove(key);
    return null;
  }
  
  public int size() {
    return data.size();
  }
  
  public Collection<V> values() {
    Set<V> result = newSet();
    for (K key : data.keySet()) {
      result.addAll(data.get(key));
    }
    return result;
  }
  
  public String toString() {
    return data.toString();
  }
  
  static class SimpleEntry<K, V> implements Entry<K, V> {
    K key;
    
    V value;
    
    public SimpleEntry(K key, V value) {
      this.key = key;
      this.value = value;
    }
    
    public SimpleEntry(Entry<K, V> e) {
      this.key = e.getKey();
      this.value = e.getValue();
    }
    
    public K getKey() {
      return key;
    }
    
    public V getValue() {
      return value;
    }
    
    public V setValue(V value) {
      V oldValue = this.value;
      this.value = value;
      return oldValue;
    }
  }
}
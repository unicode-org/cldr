package org.unicode.cldr.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public final class DumbTreeMap<K extends Comparable<K>,V> implements Map<K,V> {
  K[] keys;
  V[] values;
  int len;
  Comparator<K> comparator;
  Set<K> keySet;
  Collection<V> valueCollection;
  
  @SuppressWarnings("unchecked")
  public DumbTreeMap(int initialSize, Comparator<K> comparator) {
    keys = (K[]) new String[initialSize];
    values = (V[]) new String[initialSize];
    this.comparator = comparator;
  }
  
  public DumbTreeMap(int initialSize) {
    this(initialSize,null);
  }
  
  public DumbTreeMap(Comparator<K> comparator) {
    this(10,comparator);
  }
  
  public DumbTreeMap() {
    this(10,null);
  }
  
  public void clear() {
    clear(0);
  }

  public boolean isEmpty() {
    return len == 0;
  }

  public int size() {
    return len;
  }

  @SuppressWarnings("unchecked")
  public boolean containsKey(Object keyIn) {
    // inlined for speed
    try {
      K key = (K) keyIn;
      for (int i = 0; i < len; ++i) {
        int comparison = comparator == null 
        ? key.compareTo(keys[i]) 
                : comparator.compare(key,keys[i]);
        if (comparison > 0) {
          continue;
        }
        return comparison == 0;
      }
      return false;
    } catch (ClassCastException e) {
      return false;
    }
  }

  public boolean containsValue(Object value) {
    for (int i = 0; i < len; ++i) {
      if (value.equals(values[i])) {
        return true;
      }
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  public V get(Object keyIn) {
    // inlined for speed
    try {
      K key = (K) keyIn;
      for (int i = 0; i < len; ++i) {
        int comparison = comparator == null 
        ? key.compareTo(keys[i]) 
                : comparator.compare(key, keys[i]);
        if (comparison > 0) {
          continue;
        }
        if (comparison == 0) {
          return values[i];
        }
        return null;
      }
      return null;
    } catch (ClassCastException e) {
      return null;
    }
  }

  public V put(K key, V value) {
    // inlined for speed
    for (int i = 0; i < len; ++i) {
      int comparison = comparator == null 
        ? key.compareTo(keys[i]) 
        : comparator.compare(key, keys[i]);
      if (comparison > 0) {
        continue;
      }
      valueCollection = null;
      if (comparison == 0) {
        V result = values[i];
        values[i] = value;
        return result;
      }
      keySet = null;
      // push items up
      // add one to end, and return
      if (len == keys.length) {
        keys = grow(keys, len+2+1);
        values = grow(values, len+2+1);
      }
      for (int j = len; j > i; --j) {
        keys[j] = keys[j - 1];
        values[j] = values[j - 1];
      }
      ++len;
      keys[i] = key;
      values[i] = value;
      return null;
    }
    // add one to end, and return
    if (len == keys.length) {
      keys = grow(keys, len+2+1);
      values = grow(values, len+2+1);
    }
    keys[len] = key;
    values[len++] = value;
    valueCollection = null;
    keySet = null;
    return null;
  }

  public void putAll(Map<? extends K, ? extends V> t) {
    // TODO optimize
    for (K key : t.keySet()) {
      put(key,t.get(key));
    }
  }

  @SuppressWarnings("unchecked")
  public V remove(Object keyIn) {
    try {
      K key = (K) keyIn;
    // inlined for speed
    for (int i = 0; i < len; ++i) {
      int comparison = comparator == null 
        ? key.compareTo(keys[i]) 
        : comparator.compare(key, keys[i]);
      if (comparison > 0) {
        continue;
      }
      if (comparison < 0) {
        return null;
      }
      // found match, remove
      V result = values[i];
      int last = i;
      for (int j = i+1; j < len; ++j) {
        keys[last] = keys[j];
        values[last] = values[j];
        last = j;
      }
      --len;
      valueCollection = null;
      keySet = null;
      return result;
    }
    return null;
    } catch (ClassCastException e) {
      return null;
    }
  }

  public Set<K> keySet() {
    // TODO: optimize
    if (keySet == null) {
      keySet = new LinkedHashSet<K>(len);
      for (int i = 0; i < len; ++i) {
        keySet.add(keys[i]);
      }
    }
    return keySet;
  }

  public Collection<V> values() {
    if (valueCollection == null) {
      valueCollection = new ArrayList(len);
      for (int i = 0; i < len; ++i) {
        valueCollection.add(values[i]);
      }
    }
    return valueCollection;
  }
  
  public Set<Entry<K, V>> entrySet() {
    throw new UnsupportedOperationException();
  }
  
   public String toString() {
    StringBuilder result = new StringBuilder("{");
    for (int i = 0; i < len; ++i) {
      if (i != 0) {
        result.append(", ");
      }
      result.append(keys[i]).append('=').append(values[i]);
    }
    return result.append('}').toString();
  }
  
  public boolean equals(Object other) {
    try {
      Map<K,V> otherMap = (Map<K,V>) other;
      if (otherMap.size() != len) {
        return false;
      }
      for (int i = 0; i < len; ++i) {
        V otherValue = otherMap.get(keys[i]);
        if (!values[i].equals(otherValue)) {
          return false;
        }
      }
      return true;
    } catch (ClassCastException e) {
      return false;
    }
  }

  public static <T> T[] newArray(T[] a, int size) {
    return (T[]) new Object[size];
    // return (T[])java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
  }
 
  public static <T> T[] newArray(Class<?> aClass, int size) {
    return (T[]) Array.newInstance(aClass, size);
  }
 
  public static <T> T[] grow(T[] array, int newLen) {
    T[] temp = (T[]) new String[newLen];
    System.arraycopy(array, 0, temp, 0, array.length);
    return temp;
  }
  
  // ===========================================
  
  private void clear(int newLength) {
    for (int i = newLength; i < len; ++i) {
      keys[i] = null;
      values[i] = null;
    }
    len = newLength;
    keySet = null;
    valueCollection = null;
  }

}

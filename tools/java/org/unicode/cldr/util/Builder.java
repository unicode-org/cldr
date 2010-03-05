package org.unicode.cldr.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import com.ibm.icu.text.Transform;

/**
 * Convenience class for building collections and maps. Allows them to be built by chaining, making it simpler to
 * set as parameters and fields. Also supplies some operations that are missing on the JDK maps and collections,
 * and provides finer control for what happens with equal elements.
 * <p>You start with Builder.with(...) and end with either .get() or .freeze(). With .freeze, the result is unmodifiable (but its objects may be). Examples:
 * <ul>
 * <li>Set<String> sorted = Builder.with(new TreeSet<String>()).addAll(anIterator).addAll(aCollection).addAll(item1, item2, item3).get();</li>
 * <li>Map<String, Integer> map = Builder.with(new TreeMap<String, Integer>()).put("one",2).putAll(otherMap).freeze();
 * </ul>
 *
 * <p>The builder allows some options that the normal collections don't have, with the EqualAction. If none it specified, then it behaves like Java collections.
 * <pre>
 * Operations: A is current contents, B is new collection, x indicates the results
   * A-B   A&B    B-A   Name
   *                    clear()
   * x                  removeAll(B)
   *        x           retainAll(B) -- option 1: keep A, option 2: substitute B
   *               x    keepNew(B)
   * x      x           <no operation>
   *        x      x    clear().addAll(B)
   * x             x    xor(B)
   * x      x      x    addAll(B)          
 * </pre>
 * @author markdavis
 */
public final class Builder {
  enum EqualAction {
    /**
     * If you try to add an item that is already there, or change the mapping, do whatever the source collation or map does.
     */
    NATIVE, 
    /**
     * If you try to add an item that is already there, or change the mapping, take the new item.
     */
    REPLACE, 
    /**
     * If you try to add an item that is already there, or change the mapping, retain the old one.
     */
    RETAIN, 
    /**
     * If you try to add an item that is already there, or change the mapping, throw an exception.
     */
    THROW
    }

  public static <E, C extends Collection<E>> CBuilder<E,C> with(C collection, EqualAction ea) {
    return new CBuilder<E,C>(collection, ea);
  }

  public static <E, C extends Collection<E>> CBuilder<E,C> with(C collection) {
    return new CBuilder<E,C>(collection, EqualAction.NATIVE);
  }

  public static <K, V, M extends Map<K,V>> MBuilder<K,V,M> with(M map, EqualAction ea) {
    return new MBuilder<K,V,M>(map, ea);
  }

  public static <K, V, M extends Map<K,V>> MBuilder<K,V,M> with(M map) {
    return new MBuilder<K,V,M>(map, EqualAction.NATIVE);
  }

  // ===== Collections ======

  public static final class CBuilder<E, U extends Collection<E>> {
    public EqualAction getEqualAction() {
      return equalAction;
    }
    public CBuilder<E, U> setEqualAction(EqualAction equalAction) {
      this.equalAction = equalAction;
      return this;
    }
    
    public CBuilder<E, U> clear() {
      collection.clear();
      return this;
    }

    public CBuilder<E,U> add(E e) {
      switch (equalAction) {
      case NATIVE: 
        break;
      case REPLACE: 
        collection.remove(e);
        break;
      case RETAIN: 
        if (collection.contains(e)) {
          return this;
        }
        break;
      case THROW: 
        if (collection.contains(e)) {
          throw new IllegalArgumentException("Map already contains " + e);
        }
      }
      collection.add(e);
      return this;
    }
    
    public CBuilder<E,U> addAll(Iterable<? extends E> c) {
      if (equalAction == EqualAction.REPLACE && c instanceof Collection) {
        collection.addAll((Collection<E>)c);
      } else {
        for (E item : c) {
          add(item);
        }
      }
      return this;
    }
    
    public CBuilder<E, U> addAll(E... items) {
      for (E item : items) {
        collection.add(item);
      }
      return this;
    }
    
    public CBuilder<E, U> addAll(Iterator<E> items) {
      while (items.hasNext()) {
        collection.add(items.next());
      }
      return this;
    }
    
    public CBuilder<E, U> remove(E o) {
      collection.remove(o);
      return this;
    }
    
    public CBuilder<E,U> removeAll(Collection<? extends E> c) {
      collection.removeAll(c);
      return this;
    }
    
    public CBuilder<E,U> removeAll(E... items) {
      for (E item : items) {
        collection.remove(item);
      }
      return this;
    }
    
    public CBuilder<E,U> removeAll(Iterator<E> items) {
      while (items.hasNext()) {
        collection.remove(items.next());
      }
      return this;
    }
    
    public CBuilder<E,U> retainAll(Collection<? extends E> c) {
      collection.retainAll(c);
      return this;
    }
    
    public CBuilder<E,U> retainAll(E... items) {
      collection.retainAll(Arrays.asList(items));
      return this;
    }
    
    public CBuilder<E,U> retainAll(Iterator<E> items) {
      HashSet<E> temp = Builder.with(new HashSet<E>()).addAll(items).get();
      collection.retainAll(temp);
      return this;
    }
    
    public CBuilder<E,U> xor(Collection<? extends E> c) {
      for (E item : c) {
        boolean changed = collection.remove(item);
        if (!changed) {
          collection.add(item);
        }
      }
      return this;
    }
    
    public CBuilder<E,U> xor(E... items) {
      return xor(Arrays.asList(items));
    }
    
    public CBuilder<E,U> xor(Iterator<E> items) {
      HashSet<E> temp = Builder.with(new HashSet<E>()).addAll(items).get();
      return xor(temp);
    }
    
    public CBuilder<E,U> keepNew(Collection<? extends E> c) {
      HashSet<E> extras = new HashSet<E>(c);
      extras.removeAll(collection);
      collection.clear();
      collection.addAll(extras);
      return this;
    }
    
    public CBuilder<E,U> keepNew(E... items) {
      return keepNew(Arrays.asList(items));
    }
    
    public CBuilder<E,U> keepNew(Iterator<E> items) {
      HashSet<E> temp = Builder.with(new HashSet<E>()).addAll(items).get();
      return keepNew(temp);
    }
    
    public CBuilder<E,U> filter(Transform<E,Boolean> filter) {
      HashSet<E> temp = new HashSet<E>();
      for (E item : collection) {
        if (filter.transform(item) == Boolean.FALSE) {
          temp.add(item);
        }
      }
      collection.removeAll(temp);
      return this;
    }
    
    public U get() {
      U temp = collection;
      collection = null;
      return temp;
    }

    @SuppressWarnings("unchecked")
    public U freeze() {
      U temp;
      if (collection instanceof SortedSet) {
        temp = (U)Collections.unmodifiableSortedSet((SortedSet<E>) collection);
      } else if (collection instanceof Set) {
        temp = (U)Collections.unmodifiableSet((Set<E>) collection);
      } else if (collection instanceof List) {
        temp = (U)Collections.unmodifiableList((List<E>) collection);
      } else {
        temp = (U)Collections.unmodifiableCollection(collection);
      }
      collection = null;
      return temp;
    }
    
    public String toString() {
      return collection.toString();
    }

    // ===== PRIVATES ======

    private CBuilder(U set2, EqualAction ea) {
      this.collection = set2;
      equalAction = ea;
    }
    private U collection;
    private EqualAction equalAction;
  }
  
  // ===== Maps ======

  public static final class MBuilder<K, V, M extends Map<K,V>> {

    public EqualAction getEqualAction() {
      return equalAction;
    }
    public MBuilder<K, V, M> setEqualAction(EqualAction equalAction) {
      this.equalAction = equalAction;
      return this;
    }
    
    public MBuilder<K, V, M> clear() {
      map.clear();
      return this;
    }
    public MBuilder<K, V, M> put(K key, V value) {
      switch (equalAction) {
      case NATIVE: 
        break;
      case REPLACE: 
        map.remove(key);
        break;
      case RETAIN: 
        if (map.containsKey(key)) {
          return this;
        }
        break;
      case THROW: 
        if (map.containsKey(key)) {
          throw new IllegalArgumentException("Map already contains " + key);
        }
      }
      map.put(key, value);
      return this;
    }
    
    public MBuilder<K, V, M> on(K... keys) {
      this.keys = Arrays.asList(keys);
      return this;
    }
    
    public MBuilder<K, V, M> on(Collection<? extends K> keys) {
      this.keys = keys;
      return this;
    }
    
    public MBuilder<K, V, M> put(V value) {
      for (K key : keys) {
        put(key, value);
      }
      keys = null;
      return this;
    }
    
    public MBuilder<K, V, M> put(V... values) {
      int v = 0;
      for (K key : keys) {
        put(key, values[v++]);
        if (v >= values.length) {
          v = 0;
        }
      }
      keys = null;
      return this;
    }
    
    public MBuilder<K, V, M> put(Collection<? extends V> values) {
      Iterator<? extends V> vi = null;
      for (K key : keys) {
        if (vi == null || !vi.hasNext()) {
          vi = values.iterator();
        }
        put(key, vi.next());
      }
      return this;
    }
    
    public MBuilder<K, V, M> putAll(Map<? extends K, ? extends V> m) {
      if (equalAction == EqualAction.NATIVE) {
        map.putAll(m);
      } else {
        for (K key : m.keySet()) {
          put(key, m.get(key));
        }
      }
      keys = null;
      return this;
    }
    
    public MBuilder<K, V, M> remove(K key) {
      map.remove(key);
      return this;
    }
    
    public MBuilder<K, V, M> removeAll(Collection<? extends K> keys) {
      map.keySet().removeAll(keys);
      return this;
    }
    public MBuilder<K, V, M> removeAll(K... keys) {
      return removeAll(Arrays.asList(keys));
    }
    
    public MBuilder<K, V, M> retainAll(Collection<? extends K> keys) {
      map.keySet().retainAll(keys);
      return this;
    }
    public MBuilder<K, V, M> retainAll(K... keys) {
      return retainAll(Arrays.asList(keys));
    }

    public <N extends Map<K,V>> MBuilder<K, V, M> xor(N c) {
      for (K item : c.keySet()) {
        if (map.containsKey(item)) {
          map.remove(item);
        } else {
          put(item, c.get(item));
        }
      }
      return this;
    }
    
    public <N extends Map<K,V>> MBuilder<K, V, M> keepNew(N c) {
      HashSet<K> extras = new HashSet<K>(c.keySet());
      extras.removeAll(map.keySet());
      map.clear();
      for (K key : extras) {
        map.put(key, c.get(key));
      }
      return this;
    }
        
    public M get() {
      M temp = map;
      map = null;
      return temp;
    }
    
    @SuppressWarnings("unchecked")
    public M freeze() {
      M temp;
      if (map instanceof SortedMap<?,?>) {
        temp = (M)Collections.unmodifiableSortedMap((SortedMap<K,V>) map);
      } else {
        temp = (M)Collections.unmodifiableMap((Map<K,V>) map);
      }
      map = null;
      return temp;
    }
    
    public String toString() {
      return map.toString();
    }
    
    // ===== PRIVATES ======
    
    private Collection<? extends K> keys;
    private M map;
    private EqualAction equalAction;

    private MBuilder(M map, EqualAction ea) {
      this.map = map;
      equalAction = ea;
    }
  }
}

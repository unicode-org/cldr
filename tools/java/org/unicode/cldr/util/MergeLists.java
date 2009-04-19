package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MergeLists<T> {
  
  Collection<Collection<T>> source = new ArrayList<Collection<T>>();
  Set<T> orderedWorkingSet;
  
  public MergeLists() {
    this(new LinkedHashSet<T>());
  }
  
  public MergeLists(Set<T> orderedWorkingSet) {
    this.orderedWorkingSet = orderedWorkingSet;
  }
  
  public MergeLists<T> add(Collection<T> orderedItems) {
    if (orderedItems.size() == 0) { // skip empties
      return this;
    }
    final LinkedHashSet<T> linkedHashSet = new LinkedHashSet<T>(orderedItems);
    if (linkedHashSet.size() != orderedItems.size()) {
      throw new IllegalArgumentException("Multiple items in ordering!");
    }
    source.add(linkedHashSet);
    return this;
  }
  
  public MergeLists<T> add(T...stuff) {
    return add(Arrays.asList(stuff));
  }
  
  public MergeLists<T> addAll(Collection<Collection<T>> collectionsOfOrderedItems) {
    for (Collection<T> orderedItems : collectionsOfOrderedItems) {
      add(orderedItems);
    }
    return this;
  }
  
  public List<T> merge() {
    List<T> result = new ArrayList<T>();
    
    for (Collection<T> sublist : source) {
      orderedWorkingSet.addAll(sublist);
    }
    
    // now that we have things ordered, we take the first one that is only at the front of a list
    // this is slower, but puts things into as much of the order specified as possible
    // could be optimized further, but we don't care that much
    
    Set<T> first = new HashSet<T>();
    while (orderedWorkingSet.size() != 0) {
      getFirsts(first);
      // now get first item that is in first
      T best = extractFirstOk(orderedWorkingSet, first); // removes from working set
      // remaining items now contains no non-first items
      removeFromSource(best);
      result.add(best);
    }
    return result;
  }
  
  public static <T> boolean hasConsistentOrder(Collection<T> a, Collection<T> b) {
    LinkedHashSet<T> remainder = new LinkedHashSet<T>(a);
    remainder.retainAll(b);
    if (remainder.size() == 0) {
      return true;
    }
    // remainder is now in a's order, and contains only the items that are in both
    Iterator<T> bi = b.iterator();
    T current = bi.next();
    for (T item : remainder) {
      if (item.equals(current)) {
        if (!bi.hasNext()) {
          return true;
        }
        current = bi.next();
      }
    }
    return !bi.hasNext(); // if we have any left over, we failed
  }
  
  public static <T> Collection<T> hasConsistentOrderWithEachOf(Collection<T> a, Collection<Collection<T>> bs) {
    for (Collection<T> b : bs) {
      if (!hasConsistentOrder(a, b)) {
        return b;
      }
    }
    return null;
  }

  
  // could be optimized since we know the item will only occur at the head of a list
  private void removeFromSource(T item) {
    for (Iterator<Collection<T>> iterator = source.iterator(); iterator.hasNext();) {
      Collection<T> sublist = iterator.next();
      sublist.remove(item);
      if (sublist.size() == 0) {
        iterator.remove();
      }
    }
  }

  /**
   * Get the first item that is also in the ok set.
   */
  private T extractFirstOk(Collection<T> remainingItems, Set<T> ok) {
    for (Iterator<T> it = remainingItems.iterator(); it.hasNext();) {
      T item = it.next();
      if (ok.contains(item)) {
        it.remove();
        return item;
      }
    }
    throw new IllegalArgumentException("Internal Error");
  }

  /**
   * Get first of each sets. Guaranteed non-empty
   */
  public void getFirsts(Set<T> result) {
    result.clear();
    result.addAll(orderedWorkingSet);
    for (Collection<T> sublist : source) {
      // get all the first items
        final Iterator<T> iterator = sublist.iterator();
        iterator.next(); // skip first
        while (iterator.hasNext()) {
          result.remove(iterator.next());
      }
    }
  }
}

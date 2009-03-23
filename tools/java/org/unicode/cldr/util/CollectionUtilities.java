package org.unicode.cldr.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

public class CollectionUtilities {

  public static class ComparableCollection<T extends Comparable> implements Comparable<ComparableCollection<T>> {
    private Iterable<T> contents;
    
    public ComparableCollection(Iterable<T> source){
      contents = source;
    }
    
    public ComparableCollection(T... source){
      contents = Arrays.asList(source);
    }
    
    public int compareTo(ComparableCollection<T> o) {
      Iterator<T> i = contents.iterator();
      Iterator<T> j = o.contents.iterator();
      while (true) {
        boolean goti = i.hasNext();
        boolean gotj = j.hasNext();
        if (!goti || !gotj) {
          return goti ? 1 : gotj ? -1 : 0;
        }
        T ii = i.next();
        T jj = i.next();
        int result = ii.compareTo(jj);
        if (result != 0) {
          return result;
        }
      }
    }

    public Iterable<T> getContents() {
      return contents;
    }

    public ComparableCollection<T> setContents(Iterable<T> contents) {
      this.contents = contents;
      return this;
    }
  }
  
  public static class ComparableMap<T extends Comparable, U extends Comparable> implements Comparable<ComparableMap<T,U>> {
    private Map<T,U> contents;
    
    public ComparableMap(Map<T,U> source){
      contents = source;
    }
    
    public int compareTo(ComparableMap<T,U> o) {
      Iterator<T> i = contents.keySet().iterator();
      Iterator<T> j = o.contents.keySet().iterator();
      while (true) {
        boolean goti = i.hasNext();
        boolean gotj = j.hasNext();
        if (!goti || !gotj) {
          return goti ? 1 : gotj ? -1 : 0;
        }
        T ii = i.next();
        T jj = i.next();
        int result = ii.compareTo(jj);
        if (result != 0) {
          return result;
        }
        final U iv = contents.get(ii);
        final U jv = o.contents.get(jj);
        result = iv.compareTo(jv);
        if (result != 0) {
          return result;
        }
      }
    }

    public Map<T,U> getContents() {
      return contents;
    }

    public ComparableMap<T,U> setContents(Map<T,U> contents) {
      this.contents = contents;
      return this;
    }
  }

  


  /**
   * Utility for interworking with Collections
   */
  public static boolean containsAll(UnicodeSet set, Collection other) {
      Iterator it = other.iterator();
      while (it.hasNext()) {
          if (!set.contains(it.next().toString())) {
              return false;
          }
      }
      return true;
  }
  
  /**
   * Utility for interworking with Collections
   */
  public static boolean containsNone(UnicodeSet set, Collection other) {
      Iterator it = other.iterator();
      while (it.hasNext()) {
          if (set.contains(it.next().toString())) {
              return false;
          }
      }
      return true;
  }

  /**
   * Utility for interworking with Collections
   */
  public static boolean containsSome(UnicodeSet set, Collection other) {
      return !containsNone(set,other);
  }
  
  /**
   * Utility for interworking with Collections
   */
  public static Set<String> toSet(UnicodeSet set) {
      return (Set<String>) addAllTo(set, new TreeSet());
  }

  public static Set<String> addAllTo(UnicodeSet set, Set<String> target) {
    boolean changed = false;
    for (UnicodeSetIterator it = new UnicodeSetIterator(set); it.next();) {
      final String string = it.getString();
      changed |= target.add(string);
    }
    return target;
  }

  /**
   * Utility for interworking with Collections
   */
  public static UnicodeSet removeAll(UnicodeSet set, Collection arg0) {
      for (Iterator it = arg0.iterator(); it.hasNext();) {
          set.remove(it.next().toString());
      }
      return set;
  }

  /**
   * Utility for interworking with Collections
   */
  public static UnicodeSet retainAll(UnicodeSet set, Collection arg0) {
      UnicodeSet toRemove = new UnicodeSet();
      for (UnicodeSetIterator it = new UnicodeSetIterator(set); it.next();) {
          final String string = it.getString();
          if (arg0.contains(string)) {
              toRemove.add(string);
          }
      }
      set.removeAll(toRemove);
      return set;
  }

  /**
   * Utility for interworking with Collections
   */
  public static String[] toArray(UnicodeSet set) {
      return toArray(set, new String[set.size()]);
  }

  /**
   * Utility for interworking with Collections
   */
  public static String[] toArray(UnicodeSet set, String[] arg0) {
      int i = 0;
      for (UnicodeSetIterator it = new UnicodeSetIterator(set); it.next();) {
          arg0[i++] = it.getString();
      }
      return arg0;
  }

  /**
   * Utility for interworking with Collections
   */
  public static Iterator<String> iterator(UnicodeSet set) {
      // TODO Auto-generated method stub
      return new UnicodeSetIterator2(set);
  }
  
  private static class UnicodeSetIterator2 implements Iterator {
      UnicodeSetIterator wrapped;
      String nextString;
      
      UnicodeSetIterator2(UnicodeSet source) {
          wrapped = new UnicodeSetIterator(source);
          next();
      }

      /* (non-Javadoc)
       * @see java.util.Iterator#hasNext()
       */
      public boolean hasNext() {
          return nextString != null;
      }

      /* (non-Javadoc)
       * @see java.util.Iterator#next()
       */
      public Object next() {
          String result = nextString;
          nextString = wrapped.next() ? wrapped.getString() : null;
          return result;
      }

      /* (non-Javadoc)
       * @see java.util.Iterator#remove()
       */
      public void remove() {
          throw new UnsupportedOperationException();
      }  
  }
}

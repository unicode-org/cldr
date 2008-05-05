package org.unicode.cldr.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

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

}

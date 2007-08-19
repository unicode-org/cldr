/*
 **********************************************************************
 * Copyright (c) 2006-2007, Google and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Provide way to associate each of a set of T objects with an integer, where
 * the integer can be used to get the object. All objects are immutable, and
 * created with a corresponding factory object. The integers have no relation to
 * the order in the original set. The integers may not be compact (eg 0..n).
 * 
 * @param <T>
 */
public abstract class IntMap<T> {

  public interface IntMapFactory<T> {
    public IntMap<T> make(Collection<T> values);
  }

  public abstract T get(int index);
  public abstract Map<T, Integer> getValueMap(Map<T, Integer> output);
  
  public  Map<T, Integer> getValueMap() {
    return getValueMap(new HashMap<T, Integer>());
  }
  
  public abstract int approximateStorage();
 
  public String toString() {
    return getValueMap().toString();
  }

  // Concrete classes, embedded for simplicity
  
  public static class BasicIntMap<T> extends IntMap<T> {
    private final T[] intToValue;
    private BasicIntMap(T[] intToValue) {
      this.intToValue = intToValue;
    }

    public T get(int index) {
      return intToValue[index];
    }

    public Map<T,Integer> getValueMap(Map<T,Integer> output) {
      for (int i = 0; i < intToValue.length; ++i) {
        output.put(intToValue[i], i);
      }
      return output;
    }
    
    public int approximateStorage() {
      int size = OBJECT_OVERHEAD;
      for (T item : intToValue) {
        size += 4; // size of int
        size += item.toString().length() * 2 + STRING_OVERHEAD ;
      }
      return size;
    }
  }
  
  public static class BasicIntMapFactory<T> implements IntMapFactory<T> {
    public BasicIntMap<T> make(Collection<T> values) {
      return new BasicIntMap<T>((T[])new ArrayList<T>(new HashSet<T>(values)).toArray());
    }
  }

  /**
   * Stores short strings (255 or less) in compacted fashion. The number of
   * strings is also limited: in the worst case to 2^24 / total number of UTF-8
   * bytes
   * 
   * @author markdavis
   */
  public static class CompactStringIntMap extends IntMap<String> {
    private final String data;
    private final int[] intToValue;
    private CompactStringIntMap(String data, int[] intToValue) {
      this.data = data;
      this.intToValue = intToValue;
    }

    public String get(int index) {
      //    the packedIndex stores the string as an index in the top 24 bits, and length in the bottom 8.
      int packedIndex = intToValue[index];
      int len = packedIndex & 0xFF;
      int dataIndex = packedIndex >>> 8;
      return data.substring(dataIndex, dataIndex + len);
    }

    public Map<String, Integer> getValueMap(Map<String, Integer> output) {
        for (int i = 0; i < intToValue.length; ++i) {
          output.put(get(i), i);
        }
        return output;
    }
    
    public int approximateStorage() {
      int size = OBJECT_OVERHEAD + POINTER_OVERHEAD * 2;
      size += data.length() * 2 + STRING_OVERHEAD ;
      size += 4 * intToValue.length;
      return size;
    }
  }
  
  public static final Comparator<String> LONGEST_FIRST_COMPARATOR =  new Comparator<String>() {
    public int compare(String a, String b) {
      return  a.length() > b.length() ? -1
          : a.length() < b.length() ? 1
              : a.compareTo(b);
    }
  };
  
  public static class CompactStringIntMapFactory implements IntMapFactory<String> {
    public CompactStringIntMap make(Collection<String> values) {
      // first sort, longest first
      Set<String> sorted = new TreeSet(LONGEST_FIRST_COMPARATOR);
      sorted.addAll(values);
      
      StringBuilder data = new StringBuilder();
      int[] intToValue = new int[sorted.size()];
      int count = 0;
      
      // compact the values
      //    the packedIndex stores the string as an index in the top 24 bits, and length in the bottom 8.
      for (String string : sorted) {
        if (string.length() > 255) {
          throw new IllegalArgumentException("String too large: CompactStringIntMapFactory only handles strings up to 255 in length");
        }
        int position = data.indexOf(string);
        if (position < 0) { // add if not there
          position = data.length();
          data.append(string);
        }
        intToValue[count++] = (position<<8) | string.length();
      }
      return new CompactStringIntMap(data.toString(), intToValue);
    }
  }
  
  // for approximateSize
  private static final int OBJECT_OVERHEAD = 16;
  private static final int POINTER_OVERHEAD = 16;
  private static final int STRING_OVERHEAD = 16 + OBJECT_OVERHEAD;
  
}
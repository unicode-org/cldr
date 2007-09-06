/*
 **********************************************************************
 * Copyright (c) 2006-2007, Google and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.util;

import org.unicode.cldr.util.Dictionary.DictionaryBuilder;
import org.unicode.cldr.util.Dictionary.Matcher.Status;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

/**
 * This is a simple dictionary class used for testing. Should be in the package usertest, but it's a pain to rename files in CVS.
 * @author markdavis
 */
public class SimpleDictionary<T> extends Dictionary<T> {
  private TreeMap<CharSequence, T> data = new TreeMap<CharSequence, T>();
  private Set<CharSequence> possibleMatchesBefore;
  private Set<CharSequence> possibleMatchesAfter;
  private Status finalStatus;
  boolean done;
  private int matchCount;
  private CharSequence lastEntry = "";
  
  public static class SimpleDictionaryBuilder<T> implements DictionaryBuilder<T>{

    public SimpleDictionary<T> make(Map<CharSequence, T> source) {
      return new SimpleDictionary(source);
    }
    
  }
 
  private SimpleDictionary(Map<CharSequence,T> source) {
    for (CharSequence text : source.keySet()) {
     addMapping(text, source.get(text));
    }
  }
  
//  private List<T> results;
//  
//  public static <U> Dictionary<U> getInstance(Map<CharSequence, U> source) {
//    SimpleDictionary<U> result = new SimpleDictionary<U>();
//    // if T is not an integer, then get the results, and assign each a number
//    Map<U,Integer> valueToInt = new HashMap<U,Integer>();
//    result.results = new ArrayList<U>();
//    int count = 0;
//    for (U value : source.values()) {
//      Integer oldValue = valueToInt.get(value);
//      if (oldValue == null) {
//        result.results.add(value);
//        valueToInt.put(value, count++);
//      }
//    }
//    
//    
//    for (CharSequence text : source.keySet()) {
//      result.addMapping(text, valueToInt.get(source.get(text)));
//    }
//    return result;
//  }

  
  private void addMapping(CharSequence text, T result) {
    if (CharUtilities.compare(text,lastEntry) <= 0) {
      throw new IllegalArgumentException("Each string must be greater than the previous one.");
    }
    lastEntry = text;
    data.put(text, result);
  }

  public Iterator<Entry<CharSequence, T>> getMapping() {
    return Collections.unmodifiableMap(data).entrySet().iterator();
  }


  @Override
  public Matcher<T> getMatcher() {
    return new SimpleMatcher();
  }
  
  private class SimpleMatcher extends Matcher<T> {

  @Override
  public Matcher<T> setOffset(int offset) {
    possibleMatchesBefore = data.keySet();
    done = false;
    matchValue = null;
    return super.setOffset(offset);
  }

  /**
   * Dumb implementation.
   * 
   */
  @Override
  public Status next() {
    // There are two degenerate cases: our dictionary is empty, or we are called on an empty string.

    // As long as we get matches, we return them.
    // When we fail, we return one of two statuses
    // DONE if there were no more matches in the dictionary past the last match
    // SINGLE if there was a longer match, plus the longest offset that we successfully got to.
    
    // if we have already narrowed down to the end, just return the status
    // everything should already be set to make this work.
    if (done) {
      if (finalStatus == Status.NONE) {
        matchValue = null;
      }
      return finalStatus;
    }

    CharSequence firstMatch = null;
    
    while (text.hasCharAt(matchEnd)) {
      // get the next probe value
      ++matchEnd; 
      CharSequence probe = text.subSequence(offset, matchEnd);
      
      // narrow to the items that start with the probe
      // this filters Before into After
      
      firstMatch = filterToStartsWith(probe);
      
      // if we have a full match, return it
      
      if (firstMatch != null && firstMatch.length() == probe.length()) {
        possibleMatchesAfter.remove(firstMatch);
        possibleMatchesBefore = possibleMatchesAfter;
        matchValue = data.get(firstMatch);
        finalStatus = Status.NONE;
        return Status.MATCH;
      }

      // See if we've run out
      // example: probe = "man"
      // three cases, based on what was in the set
      // {man}: return DONE (we did a match before)
      // {man, many}: return SINGLE
      // {man, many, manner}: return PLURAL
      // {many}: return SINGLE
      if (possibleMatchesAfter.size() == 0) {
        --matchEnd; // backup
        break;
      }
      possibleMatchesBefore = possibleMatchesAfter;
    }
    // no more work to be done.
    done = true;
    
    if (matchEnd == offset || possibleMatchesBefore.size() == 0) {
      matchValue = null;
      return finalStatus = Status.NONE;
    }
    if (firstMatch == null) { // just in case we skipped the above loop
      firstMatch = possibleMatchesBefore.iterator().next();
    }
    matchValue = data.get(firstMatch);
    matchCount = possibleMatchesBefore.size();
    return finalStatus = Status.PARTIAL;
  }
  
  public boolean nextUniquePartial() {
    // we have already set the matchValue, so we don't need to reset here.
    return matchCount == 1;
  } 
  
  /**
   * Returns the first matching item, if there is one, and
   * filters the rest of the list to those that match the probe.
   * @param probe
   * @return
   */
  private CharSequence filterToStartsWith(CharSequence probe) {
    CharSequence result = null;
    possibleMatchesAfter = new TreeSet<CharSequence>();
    for (CharSequence item : possibleMatchesBefore) {
      if (startsWith(item, probe)) {
        if (result == null) {
          result = item;
        }
        possibleMatchesAfter.add(item);
      }
    }
    return result;
  }

  public boolean contains(CharSequence text) {
    return data.containsKey(text);
  }
  
  public T get(CharSequence text) {
    return data.get(text);
  }
//  public static class GeqGetter<K> {
//    private Set<K> set;
//    private Iterator<K> iterator;
//    private Comparator<? super K> comparator;
//    boolean done;
//    K lastItem = null;
//    private int count;
//    
//    public GeqGetter(Set<K> set, Comparator<K> comparator) {
//      this.comparator = comparator;
//      this.set = set;
//      reset();
//    }
//
//    public GeqGetter reset() {
//      iterator = set.iterator();
//      done = false;
//      return this;
//    }
//
//    /**
//     * Returns least element greater than or equal to probe.
//     * @param probe
//     * @return
//     */
//    public K getGeq(K probe) {
//      if (lastItem != null && comparator.compare(lastItem, probe) >= 0) {
//        return lastItem;
//      }
//      count = 0;
//      while (iterator.hasNext()) {
//        lastItem = iterator.next();
//        ++count;
//        if (comparator.compare(lastItem, probe) >= 0) {
//          return lastItem;
//        }
//      }
//      lastItem = null;
//      return lastItem;
//    }
//  }

  @Override
  public Dictionary<T> getDictionary() {
    // TODO Auto-generated method stub
    return SimpleDictionary.this;
  }

//  public static class CharSequenceComparator implements Comparator<CharSequence> {
//    
//    public int compare(CharSequence first, CharSequence second) {
//      int minLen = first.length();
//      if (minLen > second.length())
//        minLen = second.length();
//      int result;
//      for (int i = 0; i < minLen; ++i) {
//        if (0 != (result = first.charAt(i) - second.charAt(i)))
//          return result;
//      }
//      return first.length() - second.length();
//    }
//    
//  }
  }
  
  public static boolean startsWith(CharSequence first, CharSequence possiblePrefix) {
    if (first.length() < possiblePrefix.length()) {
      return false;
    }
    for (int i = 0; i < possiblePrefix.length(); ++i) {
      if (first.charAt(i) != possiblePrefix.charAt(i)) {
        return false;
      }
    }
    return true;
  }
  
}
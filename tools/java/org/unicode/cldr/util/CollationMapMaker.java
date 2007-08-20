/*
 **********************************************************************
 * Copyright (c) 2006-2007, Google and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.util;


import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.CollationElementIterator;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.RawCollationKey;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.Transliterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.ULocale;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

public class CollationMapMaker {
  
  public static final boolean SHOW_DEBUG = false;

  /**
   * Used to pick the "best" sample from a set for using as the canonical form.
   * @author markdavis
   *
   */
  static class ExemplarComparator implements java.util.Comparator {
    Comparator comp;
    
    public int compare(Object o1, Object o2) {
      String s1 = (String) o1;
      String s2 = (String) o2;
      // choose the greater in length
      int result = UTF16.countCodePoint(s2) - UTF16.countCodePoint(s1);
      if (result != 0) { // special fix to make zero be first
        if (s1.length() == 0)
          return -1;
        if (s2.length() == 0)
          return 1;
        return result;
      }
      result = comp.compare(s1, s2);
      if (result != 0)
        return result;
      return s1.compareTo(s2);
    }
    
    public ExemplarComparator(Comparator comp) {
      this.comp = comp;
    }
    
  }



  public static class Folder implements Cloneable {
    private UnicodeMap m = new UnicodeMap();
    
    public Object clone() {
      try {
        Folder result = (Folder) super.clone();
        result.m = (UnicodeMap)m.cloneAsThawed();
        return result;
      } catch (CloneNotSupportedException e) {
        throw new InternalError("Clone problem");
      }
    }
    
    public Object getValue(int i) {
      return m.getValue(i);
    }
    
    public UnicodeSet keySet() {
      return m.keySet();
    }
    
    public Folder put(int cp, String result) {
      m.put(cp, result);
      return this;
    }
    
    public Folder putAll(UnicodeSet set, String result) {
      m.putAll(set, result);
      return this;
    }
    
    public UnicodeSet getCharactersMapped() {
      return m.keySet();
    }
    
    public void minimalize() {
      UnicodeMap newMap = (UnicodeMap)(m.cloneAsThawed());
      
      for (UnicodeSetIterator i = new UnicodeSetIterator(m.keySet()); i.next();) {
        String s = (String)m.getValue(i.codepoint);
        newMap.put(i.codepoint, null);
        String t = newMap.fold(newMap.fold(i.getString()));
        if (!t.equals(s)) { // restore
          newMap.put(i.codepoint, s);
        }
      }
    }
    
    public void complete() {
      while (fixNeeded());
    }
    
    public boolean fixNeeded() {
      UnicodeMap newMap = new UnicodeMap();
      UnicodeSet values = m.keySet();
      boolean changed = false;
      for (UnicodeSetIterator i = new UnicodeSetIterator(values); i.next();) {
        String result = (String) m.getValue(i.codepoint);
        String newResult = fold(result);
        if (!newResult.equals(result)) {
          changed = true;
          if (SHOW_DEBUG) {
            System.out.println(i.getString() );
            System.out.println("->\t"+ result);
            System.out.println("=>\t" + newResult);
          }
        }
        newMap.put(i.codepoint, newResult);
      }
      m = newMap;
      return changed;
    }
    
    public String fold(String s) {
      return m.fold(s);
    }
    
    /**
     * Re
     * @param toRemove
     * @param addIdentity TODO
     * @param source
     * @return
     */
    Folder removeEquals(Folder toRemove, boolean addIdentity) {
      UnicodeMap result = new UnicodeMap();
      for (int i = 0; i <= 0x10FFFF; ++i) {
        Object x = m.getValue(i);
        Object y = toRemove.m.getValue(i);
        if (!UnicodeMap.areEqual(x, y)) {
          if (x != null) {
            result.put(i, x);
          } else if (addIdentity) {
            result.put(i, UTF16.valueOf(i)); // have to add mapping
          }
        }
      }
      m = result;
      return this;
    }
    
    static Transliterator FullWidthKana = Transliterator.getInstance("fullwidth-halfwidth; [[:script=katakana:][:script=hangul:]] halfwidth-fullwidth; katakana-hiragana");
    
    static String getSpecialFolded(String a) {
      String result = a;
      result = Normalizer.normalize(result, Normalizer.NFKC);
      result = FullWidthKana.transliterate(result);
      result = UCharacter.foldCase(result, true);
      result = Normalizer.normalize(result, Normalizer.NFKC);
      return result;
    }

    public UnicodeMap getUnicodeMap() {
      return (UnicodeMap) m.cloneAsThawed();
      
    }
    
    
    
  }
  
  
  
  static final boolean showDetails = false;
  static final RuleBasedCollator uca = (RuleBasedCollator) Collator.getInstance(ULocale.ROOT);
  static final UnicodeSet filteredChars = (UnicodeSet) new UnicodeSet(
  "[{ss}[^[:Co:][:Cf:][:Cc:][:Cn:][:Cs:][:script=Han:][:script=Hangul:]-[:nfkcquickcheck=no:]]]").freeze(); // skip a bunch of stuff, but include the items that are not nfkc
  
  RuleBasedCollator equivalenceClassCollator;
  ExemplarComparator exemplarComparator;
  Map reasonMap = new TreeMap();
  Map equivMap = new TreeMap();
  
  public Map<CharSequence, String> generateCollatorFolding(RuleBasedCollator exemplarCollator, int strength, boolean setExpansions, boolean ignorePunctuation, Map<CharSequence, String> mapping) {
    exemplarCollator.setStrength(exemplarCollator.IDENTICAL);
    exemplarCollator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
    exemplarCollator.setUpperCaseFirst(false);
    if (ignorePunctuation) {
      exemplarCollator.setAlternateHandlingShifted(true);
    }
    exemplarComparator = new ExemplarComparator(exemplarCollator);
    
    try {
      equivalenceClassCollator = (RuleBasedCollator) exemplarCollator.clone();
    } catch (CloneNotSupportedException e) {}
    equivalenceClassCollator.setStrength(strength);
    
    UnicodeSet expansions = new UnicodeSet();
    UnicodeSet contractions = new UnicodeSet();
    equivalenceClassCollator.getContractionsAndExpansions(contractions, expansions, true);
    UnicodeSet trialCharacters = new UnicodeSet(filteredChars).addAll(equivalenceClassCollator.getTailoredSet()).addAll(contractions).addAll(expansions);
    
    for (UnicodeSetIterator i = new UnicodeSetIterator(trialCharacters); i.next();) {
      String item = i.getString();
      addItems(item);
    }
    
    for (UnicodeSetIterator i = new UnicodeSetIterator(getFullExpansions()); i.next();) {
      String item = i.getString();
      addItems(item);
    }
    
    if (showDetails) Log.getLog().println("Printing Values: " + equivMap.size());
    int count = 0;
    int countMapped = 0;
    
    // Now process the results to figure out what we need to keep
    
    for (Iterator it = equivMap.keySet().iterator(); it.hasNext();) {
      Set values = (Set) equivMap.get(it.next());
      // if there is only one result, drop it
      if (values.size() == 1) {
        if (SHOW_DEBUG) {
          String item = (String) values.iterator().next();
          System.out.println("Skipping: " + item + "\t" + equivalenceClassCollator.getRawCollationKey(item, null));
        }
        continue;
      }
      // if (showDetails) Log.getLog().println(bf.showSetNames(values));
      Iterator chars = values.iterator();
      // the lowest value is the exemplar value, so use it as the base
      String target = (String) chars.next();
      if (SHOW_DEBUG) {
        System.out.println("Target: " + target + "\t " + Utility.hex(target) + "\t" + equivalenceClassCollator.getRawCollationKey(target, null));
      }
      while (chars.hasNext()) {
        String source = (String) chars.next();
        mapping.put(source, target);
        if (SHOW_DEBUG) {
          System.out.println("\tSource: " + source + "\t " + Utility.hex(source) + "\t" + equivalenceClassCollator.getRawCollationKey(source, null));
        }
      }
      // for (Iterator it2 = values.iterator(); it.hasNext();) {
      // bf.
      // }
      count++;
      countMapped += values.size() - 1;
    }
    return mapping;
//    // convert mapping to UnicodeMap
//    Set problems = new TreeSet();
//    Folder folder = new Folder();
//    //folder.put('\u2215', "/");
//    
//    for (Iterator it = mapping.keySet().iterator(); it.hasNext();) {
//      String source = (String) it.next();
//      folder.put(UTF16.charAt(source, 0), (String) mapping.get(source));
//    }
//    // redo folder until it stabilizes
//    // folder.complete();
//    
//    for (Iterator it = problems.iterator(); it.hasNext();) {
//      String source = (String) it.next();
//      String target = folder.fold((String) mapping.get(source));
//      String other = folder.fold(source);
//      if (target.equals(other)) {
//        it.remove();
//      } else {
//        if (showDetails) Log.getLog().println("Couldn't map source " + Utility.escape(source)
//            + "\t" + Utility.escape(target) + "\t" + Utility.escape(other));
//      }
//    }
//    if (showDetails && problems.size() != 0) {
//      Log.getLog().println("Problems");
//      for (Iterator it = problems.iterator(); it.hasNext();) {
//        String item = (String) it.next();
//        //Log.getLog().println(bf.showSetNames(item));
//        //Log.getLog().println("\t-" + bf.showSetNames(reasonMap.get(item)));
//      }
//    }
//    
//    if (showDetails) Log.getLog().println( "\tEquivalence Classes:\t" + count + "\tMapped characters:\t" + countMapped + "\tProblems:\t" + problems.size());
//    return folder;
  }
  
  private static UnicodeSet fullExpansions = null;
  
  static UnicodeSet getFullExpansions() {
    if (fullExpansions == null) addExpansionResults(fullExpansions = new UnicodeSet());
    return fullExpansions;
  }
  
  private static UnicodeSet addExpansionResults(UnicodeSet fullExpansions) {
    StringBuffer trialString = new StringBuffer();
    Map stringToKey = new TreeMap();
    Map keyToString = new TreeMap();
    Set nfkc = new HashSet();
    for (int i = 0; i < 0x10FFFF; ++i) {
      int cat = UCharacter.getType(i);
      if (cat == UCharacter.UNASSIGNED || cat == UCharacter.PRIVATE_USE || cat == UCharacter.SURROGATE) continue;
      String source = UTF16.valueOf(i);
      nfkc.add(Normalizer.compose(source, true));
      
      CollationElementIterator x = uca.getCollationElementIterator(source);
      trialString.setLength(0);
      while (true) {
        int element  = x.next();
        if (element == CollationElementIterator.NULLORDER) break;
        char primaryOrder =(char)CollationElementIterator.primaryOrder(element);
        if (primaryOrder == 0) continue;
        trialString.append(primaryOrder);
      }
      if (trialString.length() == 0) continue;
      String key = trialString.toString();
      stringToKey.put(source, key);
      String newSource = (String) keyToString.get(key);
      if (newSource == null) {
        keyToString.put(key, source);
      }
    }
    UnicodeSet expansions = new UnicodeSet();
    UnicodeSet contractions = new UnicodeSet();
    uca.getContractionsAndExpansions(contractions, expansions, true);
    
    fullExpansions = new UnicodeSet();
    global:
      for (UnicodeSetIterator usi = new UnicodeSetIterator(expansions); usi.next();) {
        trialString.setLength(0);
        String source = usi.getString();
        String key = (String) stringToKey.get(source);
        if (key == null || key.length() == 1) continue;
        main:
          while (key.length() > 0) {
            for (Iterator it = keyToString.entrySet().iterator(); it.hasNext();) {
              Entry e = (Entry) it.next();
              String otherKey = (String) e.getKey();
              if (key.startsWith(otherKey)) {
                trialString.append((String)e.getValue());
                key = key.substring(otherKey.length());
                continue main;
              }
            }
            System.out.println("Failed with: " + source);
            continue global;
          }
        String result = trialString.toString();
        if (contractions.contains(result) || nfkc.contains(result)) {
          continue global;
        }
        if (SHOW_DEBUG & false) {
          System.out.println("Adding: " + usi.getString() + "\t=>\t" + trialString);
        }
        fullExpansions.add(result);
      }
    fullExpansions.freeze();
    return fullExpansions;
  }
  
  private void addItems(String item) {
    addItems2(item, item);
    String minNFKD = getMinimalNKFD(item, equivalenceClassCollator);
    if (!minNFKD.equals(item)) {
      addItems2(minNFKD, item);
    }
    
  }
  
  private void addItems2(String item, String original) {
    addToEquiv( item, original);
    String folded = UCharacter.foldCase(item, UCharacter.FOLD_CASE_EXCLUDE_SPECIAL_I);
    if (!folded.equals(item)) {
      addToEquiv( folded, original);
    }
  }
  
  private void addToEquiv(String item, String original) {
//    String norm = Normalizer.compose(item, true);
//    if (UTF16.countCodePoint(norm) < UTF16.countCodePoint(item)) {
//      item = norm;
//    }
    RawCollationKey k = equivalenceClassCollator.getRawCollationKey(item, null);
    Set results = (Set) equivMap.get(k);
    if (results == null) {
      equivMap.put(k, results = new TreeSet(exemplarComparator));
    }
    reasonMap.put(item, original);
    results.add(item);
  }
  
  
  static UnicodeSet spaceTatweelAndNSM = new UnicodeSet("[\\u0020\\u0640[:Mn:][:Me:]]");
  static UnicodeSet NSM = new UnicodeSet("[[:Mn:][:Me:]]");
  /**
   * Return the minimal NFKD string that has the same uca key
   * 
   * @param item
   * @param k
   * @param ucaWeak
   * @return
   */
  private String getMinimalNKFD(String item, Collator ucaWeak) {
    String nfkd = com.ibm.icu.text.Normalizer.decompose(item, true);
    if (nfkd.startsWith(" ")) {
      if (spaceTatweelAndNSM.containsAll(nfkd)) {
        return item; // fails
      }
    }
    String start = "";
    String end = nfkd;
    while (end.length() != 0) {
      int cp = UTF16.charAt(end, 0);
      String tryEnd = end.substring(UTF16.getCharCount(cp));
      String trial = start + tryEnd;
      if (!ucaWeak.equals(trial, item)) { // retain character
        start += UTF16.valueOf(cp);
      }
      end = tryEnd;
    }
    return start;
  }
}

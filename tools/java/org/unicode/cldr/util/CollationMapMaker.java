/*
 **********************************************************************
 * Copyright (c) 2006-2007, Google and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.util;


import org.unicode.cldr.unittest.TestVariantFolder.CaseVariantFolder;

import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.dev.test.util.XEquivalenceMap;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.CanonicalIterator;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
      int result;
      
      // sort normalized first
      int n1 = Normalizer.isNormalized(s1,Normalizer.DECOMP_COMPAT, 0) ? 0 : 1;
      int n2 = Normalizer.isNormalized(s2,Normalizer.DECOMP_COMPAT, 0) ? 0 : 1;
      if ((result = n1 - n2) != 0) {
        return result;
      }
      
      // choose the shortest
      result = UTF16.countCodePoint(s2) - UTF16.countCodePoint(s1);
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
        String t = newMap.transform(newMap.transform(i.getString()));
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
      return m.transform(s);
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
  Comparator exemplarComparator;
  Map<String,String> reasonMap = new TreeMap();
  XEquivalenceMap equivMap = new XEquivalenceMap();
  
  public Map<CharSequence, String> generateCollatorFolding(RuleBasedCollator equivalenceClassCollator, Map<CharSequence, String> mapping) {
    this.equivalenceClassCollator = equivalenceClassCollator;
    try {
      RuleBasedCollator exemplarCollator = (RuleBasedCollator) equivalenceClassCollator.clone();
      exemplarCollator.setStrength(exemplarCollator.IDENTICAL);
      exemplarCollator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
      exemplarCollator.setUpperCaseFirst(false);
      exemplarCollator.setAlternateHandlingShifted(false);
      exemplarCollator.setNumericCollation(false);
      exemplarCollator.setCaseLevel(false);
      exemplarCollator.setFrenchCollation(false);
      exemplarCollator.setHiraganaQuaternary(false);
      exemplarComparator = new ExemplarComparator(exemplarCollator);
    } catch (CloneNotSupportedException e) {} // will never happen
    
    UnicodeSet expansions = new UnicodeSet();
    UnicodeSet contractions = new UnicodeSet();
    try {
      equivalenceClassCollator.getContractionsAndExpansions(contractions, expansions, true);
    } catch (Exception e) {}
    
    UnicodeSet trialCharacters = new UnicodeSet(filteredChars).addAll(equivalenceClassCollator.getTailoredSet()).addAll(contractions).addAll(expansions);
    
    for (UnicodeSetIterator i = new UnicodeSetIterator(trialCharacters); i.next();) {
      String item = i.getString();
      addItems(item);
    }
    
    for (UnicodeSetIterator i = new UnicodeSetIterator(getFullExpansions()); i.next();) {
      String item = i.getString();
      addItems(item);
    }
    
    closeUnderFolding();
    
    if (showDetails) Log.getLog().println("Printing Values: " + equivMap.size());
    int count = 0;
    int countMapped = 0;
    
    // Now process the results to figure out what we need to keep
    Set<String> values = new TreeSet(exemplarComparator);
    
    for (Iterator it = equivMap.iterator(); it.hasNext();) {
      Set<String> values1 = (Set) it.next();
      // if there is only one result, drop it
      if (values1.size() == 1) {
        if (false && SHOW_DEBUG) {
          String item = (String) values1.iterator().next();
          System.out.println("Skipping: " + item + "\t" + equivalenceClassCollator.getRawCollationKey(item, null));
        }
        continue;
      }
      values.clear();
      values.addAll(values1);
      // if (showDetails) Log.getLog().println(bf.showSetNames(values));
      Iterator chars = values.iterator();
      // the lowest value is the exemplar value, so use it as the base
      String target = (String) chars.next();
      if (SHOW_DEBUG) {
        System.out.println("Target: <" + target + ">\t " + Utility.hex(target) + "\t" + equivalenceClassCollator.getRawCollationKey(target, null));
      }
      while (chars.hasNext()) {
        String source = (String) chars.next();
        mapping.put(source, target);
        if (SHOW_DEBUG) {
          System.out.println("\tSource: <" + source + ">\t " + Utility.hex(source) + "\t" + equivalenceClassCollator.getRawCollationKey(source, null));
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
  
  VariantFolder caseFolder = new VariantFolder(new CaseVariantFolder());
  
  VariantFolder.AlternateFetcher COLLATION_FETCHER = new VariantFolder.AlternateFetcher() {
    public Set<String> getAlternates(String item, Set<String> output) {
      output.add(item);
      Set set = equivMap.getEquivalences(item);
      if (set != null) {
        output.addAll(set);
      }
      return output;
    }
   };
  
  private void closeUnderFolding() {
    if (false) return;
    // TODO Generalize
    Set<String> others = new HashSet<String>();
    List<Collection<String>> input = new ArrayList<Collection<String>>();
    VariantFolder recursiveFolder = new VariantFolder(COLLATION_FETCHER);
    TreeSet<CharSequence> hack = new TreeSet();
    hack.add("aa");
    
    while (true) {
      others.clear();
      for (CharSequence item : hack) { // seenSoFar
        if (item.length() == 1) {
          continue;
        }
        String str = item.toString();
        if (UTF16.countCodePoint(str) <= 1) {
          continue;
        }
        Set<String> results = recursiveFolder.getClosure(item.toString());
        results.removeAll(seenSoFar);
        Log.logln(item + "\t" + results);
        others.addAll(results);
      }
      if (others.size() == 0) {
        break;
      }
      for (String item : others) {
        addToEquiv(item, item);
      }
    }
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
    try {
      uca.getContractionsAndExpansions(contractions, expansions, true);
    } catch (Exception e1) {
      throw new IllegalArgumentException(e1);
    }
    
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
  
  CanonicalIterator canonicalIterator = new CanonicalIterator("");

  /**
   * Adds items, looking for all canonically equivalent strings as well.
   * @param item
   */
  private void addItems(String item) {
    addItems2(item, item);
    String minNFKD = getMinimalNKFD(item, equivalenceClassCollator);
    if (!minNFKD.equals(item)) {
      addItems2(minNFKD, item);
    }
    canonicalIterator.setSource(item);
    for (String nextItem = canonicalIterator.next(); nextItem != null; nextItem = canonicalIterator.next()) {
      addItems2(nextItem, item);
    }
  }
  
  /**
   * Adds items, looking for all case-equivalent strings as well.
   * @param item
   * @param original
   */
  private void addItems2(String item, String original) {
    addItems3( item, original);
    for (String nextItem :  caseFolder.getClosure(item)) {
      addItems3(nextItem, original);
    }
  }
  
  private void addItems3(String item, String original) {
    addToEquiv( item, original);
    canonicalIterator.setSource(item);
    for (String newItem = canonicalIterator.next(); newItem != null; newItem = canonicalIterator.next()) {
      addToEquiv( newItem, original);
    }
  }
  
  Set<CharSequence> seenSoFar = new TreeSet<CharSequence>();
  
  private void addToEquiv(String item, String original) {
    if (item.equals("aA")) {
      System.out.println("ouch");
    }
    if (seenSoFar.contains(item)) {
      return;
    }
    seenSoFar.add(item);
//    String norm = Normalizer.compose(item, true);
//    if (UTF16.countCodePoint(norm) < UTF16.countCodePoint(item)) {
//      item = norm;
//    }
    RawCollationKey k = equivalenceClassCollator.getRawCollationKey(item, null);
    equivMap.add(item, k);
    reasonMap.put(item, original);
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

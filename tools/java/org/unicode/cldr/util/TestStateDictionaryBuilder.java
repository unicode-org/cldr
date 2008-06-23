/*
 **********************************************************************
 * Copyright (c) 2006-2007, Google and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.util;

import org.unicode.cldr.util.CharSource;
import org.unicode.cldr.util.CharUtilities.CharSourceWrapper;
import org.unicode.cldr.util.Dictionary.Matcher;
import org.unicode.cldr.util.Dictionary.Matcher.Filter;
import org.unicode.cldr.util.Dictionary.Matcher.Status;
import org.unicode.cldr.util.SimpleDictionary.SimpleDictionaryBuilder;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

/**
 *  Should be in the package usertest, but it's a pain to rename files in CVS.
 * @author markdavis
 *
 * @param <T>
 */
public class TestStateDictionaryBuilder<T> {
  private static final boolean SHORT_TEST = true;
  
  private static final boolean SHOW_CONTENTS = true;
  
  private static final boolean CHECK_BOOLEAN = false;
  
  private final boolean SHOW_STATES = true;
  
  boolean SIMPLE_ONLY = false;
  
  boolean TEST_AGAINST_SIMPLE = true;
  
  Dictionary<T> stateDictionary;
  Dictionary.Matcher<T> stateMatcher;
  
  Dictionary<T> simpleDictionary;
  Dictionary.Matcher<T> simpleMatcher;
  
  Map<CharSequence,T> baseMapping = new TreeMap<CharSequence,T>();
  
  final StateDictionaryBuilder<T> stateDictionaryBuilder = new StateDictionaryBuilder<T>();
  final SimpleDictionaryBuilder<T> simpleDictionaryBuilder = new SimpleDictionaryBuilder<T>();
  
  // TODO: convert to TestFramework
  public static void main(String[] args) {
    
    try {
      new TestStateDictionaryBuilder<String>().test(args);
    } finally {
      System.out.println("DONE");
    }
  }
  
  @SuppressWarnings({"unchecked"})
  public void test(String[] args) {
    
    for (String arg : args) {
      if (arg.equalsIgnoreCase("utf8")) {
        stateDictionaryBuilder.setByteConverter(new Utf8StringByteConverter());
      } else if (arg.equalsIgnoreCase("normal")) {
        stateDictionaryBuilder.setByteConverter(new CompactStringByteConverter(false));
      } else if (arg.equalsIgnoreCase("compact")) {
        stateDictionaryBuilder.setByteConverter(new CompactStringByteConverter(true));
      }
    }
    baseMapping.put("GMT+0000", (T)("t"));   
    baseMapping.put("GMT+0100", (T)("t"));
    baseMapping.put("GMT+0200", (T)("t"));
    baseMapping.put("GMT+0300", (T)("t"));
    baseMapping.put("GMT+0307", (T)("t"));
    showDictionaryContents();
    
    addToBoth("man", 1);
    addToBoth("manner", 100);
    addToBoth("many", 10);
    addToBoth("any", 83);
    showDictionaryContents();
    
    baseMapping.put("man", (T)"Woman");   
    baseMapping.put("many",(T)"Few");
    baseMapping.put("any", (T)"All");
    showDictionaryContents();
    
    for (Filter filter : Filter.values()) {
      final String string = "many manners ma";
      tryFind(string, new CharSourceWrapper(string), stateDictionary, filter);
    }
    
    
    showWords("ma");
    showWords("ma!");
    showWords("!ma");
    showWords("man");
    showWords("man!");
    showWords("mann");
    showWords("mann!");
    showWords("many");
    showWords("many!");
    compare();
    
    addToBoth("m\u03B1nner", 1000);
    showDictionaryContents();
    showWords("m\u03B1");
    compare();
    
    //if (true) return;
    // clear out
    
    addToBoth("fish", 10);
    showDictionaryContents();
    showWords("a fisherman");
    compare();
    
    addToBoth("fisher", 13);
    showDictionaryContents();
    showWords("a fisherman");
    compare();
    
    addToBoth("her", 55);
    showDictionaryContents();
    showWords("a fisherman");
    compare();
    
    // clear out
    
    // check some non-latin
    String[] zoneIDs = TimeZone.getAvailableIDs();
    SimpleDateFormat dt = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.LONG, new ULocale("hi"));
    dt.applyPattern("vvvv");
    for (String zoneID : zoneIDs) {
      TimeZone zone = TimeZone.getTimeZone(zoneID);
      dt.setTimeZone(zone);
      String zoneName = dt.format(0);
      addToBoth(zoneName, (T)(CHECK_BOOLEAN ? "t" : zoneID));
    }
    compare();
    showDictionaryContents();
    ((StateDictionary<T>) stateDictionary).flatten();
    
    if (false) {
      testWithUnicodeNames();
      
      ((StateDictionary<T>) stateDictionary).flatten();
      compare();
      System.out.println();
      showDictionaryContents();
    }
    
  }

  static public <U> void tryFind(CharSequence originalText, CharSource charListText, Dictionary<U> dictionary, Filter filter) {
    System.out.println("Using dictionary: " + Dictionary.load(dictionary.getMapping(), new TreeMap()));
    System.out.println("Searching in: {" + originalText + "} with filter=" + filter);
    // Dictionaries are immutable, so we create a Matcher to search/test text.
    Matcher matcher = dictionary.getMatcher();
    matcher.setText(charListText);
    while (true) {
      Status status = matcher.find(filter);
      String unique = ""; // only set if needed
      if (status == Status.NONE) {
        break;
      } else if (status == Status.PARTIAL) {
        // sets the match value to the "first" partial match
        if (matcher.nextUniquePartial()) {
          unique = "\tUnique";
        } else {
          unique = "\tNot Unique";
        }
      }
      // Show results
      System.out.println("{"
          + showBoth(charListText, 0, matcher.getOffset()) + "[["
          + showBoth(charListText, matcher.getOffset(), matcher.getMatchEnd())
          + "]]" + showBoth(charListText, matcher.getMatchEnd(), charListText.getKnownLength())
          + "}\t" + status + "  \t{" + matcher.getMatchValue() + "}\t" + unique);
    }
    System.out.println();
  }
  
  static public CharSequence showBoth(CharSource source, int start, int end) {
    if (source instanceof CharSourceWrapper) {
      CharSourceWrapper new_name = (CharSourceWrapper) source;
      return new_name.sourceSubSequence(start, end);
    }
    return source.subSequence(start, end);
  }
  
  private void showDictionaryContents() {
    // build stuff to use from now on
    simpleDictionary = simpleDictionaryBuilder.make(baseMapping); 
    simpleMatcher = simpleDictionary.getMatcher();
    stateDictionary = stateDictionaryBuilder.make(baseMapping);
    stateMatcher = stateDictionary.getMatcher();
    baseMapping.clear();
    
//  ((Dictionary.Builder) simpleDictionary).addMapping(string, i);
//  ((Dictionary.Builder) stateDictionary).addMapping(string, i);
    
    System.out.println("Dictionary: " + Dictionary.load(stateDictionary.getMapping(), new TreeMap()));
    System.out.println();
    if (SHOW_STATES) {
      System.out.println("States:" + Utility.LINE_SEPARATOR + stateDictionary);
      System.out.println();
    }
    if (SHOW_CONTENTS) {
      System.out.println("Structure:" + Utility.LINE_SEPARATOR + stateDictionary.debugShow());
      System.out.println();
    }
  }
  
  private void testWithUnicodeNames() {
    UnicodeSet testSet = new UnicodeSet(
    "[[:assigned:] - [:ideographic:] - [:Co:] - [:Cs:]]"); // &
    // [\\u0000-\\u0FFF]
    int count = 0;
    Map<String,T> data = new TreeMap<String,T>();
    for (UnicodeSetIterator it = new UnicodeSetIterator(testSet); it.next();) {
      String name = UCharacter.getExtendedName(it.codepoint);
      if (name == null) {
        continue;
      }
      if ((++count & 0xFF) == 0) {
        System.out.println(count + ":\t"
            + com.ibm.icu.impl.Utility.hex(it.codepoint) + "\t" + name);
      }
      data.put(name, (T)com.ibm.icu.impl.Utility.hex(it.codepoint,4));
      if (false) for (String item : name.split("\\s+")) {
        data.put(item, (T)com.ibm.icu.impl.Utility.hex(it.codepoint,4));
      }
    }
    count = 0;
    for (String item : data.keySet()) {
      if (SHORT_TEST && count++ > 500) continue; // 
      addToBoth(item, data.get(item));
    }
    simpleDictionary = simpleDictionaryBuilder.make(baseMapping);
    stateDictionary = stateDictionaryBuilder.make(baseMapping);
    baseMapping.clear();
    compare();
  }
  
  private void compare() {
    System.out.println("Comparing results: ");
    
    Map<CharSequence, T> dictionaryData = Dictionary.load(stateDictionary.getMapping(), new HashMap());
    Map<CharSequence, T> simpleDictionaryData = Dictionary.load(simpleDictionary.getMapping(), new HashMap());
    
    assert dictionaryData.equals(simpleDictionaryData) : showDifference(dictionaryData, simpleDictionaryData);
    if (SHOW_STATES) {
      System.out.println("Size: " + dictionaryData.size());
      System.out.println("Rows: "
          + ((StateDictionary<T>) stateDictionary).getRowCount());
    }
    
    System.out.println("Checking values: state dictionary");
    checkSimpleMatches(stateMatcher, dictionaryData);
    System.out.println("Checking values: simple dictionary");
    checkSimpleMatches(simpleMatcher, simpleDictionaryData);
    int count = 0;
    System.out.println("Cross-checking all values");
    for (CharSequence myText : simpleDictionaryData.keySet()) {
      if ((++count & 0xFF) == 0xFF) {
        System.out.println(count + ":\t" + myText);
      }
      crossCheck(new CharSourceWrapper(myText));
      crossCheck("!" + myText);
      crossCheck(myText + "!");
    }
  }

  private String showDifference(Map<CharSequence, T> dictionaryData, Map<CharSequence, T> simpleDictionaryData) {
    System.out.println(dictionaryData.size() + ", " + simpleDictionaryData.size());
    Iterator<Entry<CharSequence, T>> it1 = dictionaryData.entrySet().iterator();
    Iterator<Entry<CharSequence, T>> it2 = simpleDictionaryData.entrySet().iterator();
    while (it1.hasNext() || it2.hasNext()) {
      Entry<CharSequence, T> item1 = it1.hasNext() ? it1.next() : null;
      Entry<CharSequence, T> item2 = it2.hasNext() ? it2.next() : null;
      System.out.println(item1 + ", " + item2);
      if (item1 == null || item2 == null || !item1.equals(item2)) {
        return item1 + "!=" + item2;
      }
    }
    return "no difference";
  }
  
  private void crossCheck(CharSequence myText) {
    crossCheck(new CharSourceWrapper(myText));
  }
  private void crossCheck(CharSource myText) {
    stateMatcher.setText(myText); // set the text to operate on
    simpleMatcher.setText(myText); // set the text to operate on
    for (int i = 0; stateMatcher.getText().hasCharAt(i); ++i) {
      stateMatcher.setOffset(i);
      simpleMatcher.setOffset(i);
      while (true) {
        Status stateStatus = stateMatcher.next();
        Status simpleStatus = simpleMatcher.next();
        assert stateStatus == simpleStatus : showValues(stateStatus, simpleStatus);
        final int stateEnd = stateMatcher.getMatchEnd();
        final int simpleEnd = simpleMatcher.getMatchEnd();
        assert stateEnd == simpleEnd 
        : showValues(stateStatus, simpleStatus);
        if (stateStatus == Status.PARTIAL) {
          boolean stateUnique = stateMatcher.nextUniquePartial();
          boolean simpleUnique = simpleMatcher.nextUniquePartial();
          assert stateUnique == simpleUnique 
          : showValues(stateStatus, simpleStatus);
        }
        // test this after checking PARTIAL
        assert stateMatcher.getMatchValue() == simpleMatcher.getMatchValue() 
        : showValues(stateStatus, simpleStatus);
        if (stateStatus != Status.MATCH) {
          break;
        }
      }
    }
  }
  
  private String showValues(Status stateStatus, Status simpleStatus) {
    return Utility.LINE_SEPARATOR + "TEXT:\t" + stateMatcher.text + Utility.LINE_SEPARATOR + "STATE:\t" + showValues(stateStatus, stateMatcher) + Utility.LINE_SEPARATOR + "SIMPLE:\t" + showValues(simpleStatus, simpleMatcher);
  }
  
  private String showValues(Status status, Matcher<T> matcher) {
    boolean uniquePartial = status == Status.PARTIAL && matcher.nextUniquePartial(); // sets matchValue for PARTIAL
    return String.format("\tOffsets: %s,%s\tStatus: %s\tString: \"%s\"\tValue: %s %s",
        matcher.getOffset(),
        matcher.getMatchEnd(),
        status,
        matcher.getMatchText(),
        matcher.getMatchValue(),
        status == Status.PARTIAL && uniquePartial ? "\tUNIQUE" : "");
  }
  
  /**
   * Check that the words all match against themselves.
   * 
   * @param matcher
   * @param data
   */
  private void checkSimpleMatches(Matcher<T> matcher, Map<CharSequence, T> data) {
    int count = 0;
    for (CharSequence myText : data.keySet()) {
      if ((count++ & 0xFF) == 0xFF) {
        System.out.println(count + ":\t" + myText);
      }
      matcher.setText(myText); // set the text to operate on
      
      matcher.setOffset(0);
      int matchEnd = -1;
      T matchValue = null;
      // find the longest match
      while (true) {
        Dictionary.Matcher.Status next1 = matcher.next();
        if (next1 == Dictionary.Matcher.Status.MATCH) {
          matchEnd = matcher.getMatchEnd();
          matchValue = matcher.getMatchValue();
        } else {
          break;
        }
      }
      assert matchEnd == myText.length() : "failed to find end of <" + myText + "> got instead " + matchEnd;
      assert matchValue == data.get(myText);
    }
  }
  
  private void addToBoth(CharSequence string, int i) {
    baseMapping.put(string, (T)(i+"/"+string));
  }
  
  private void addToBoth(CharSequence string, T i) {
    baseMapping.put(string, i);
//  if (simpleDictionary.contains(string)) return;
//  if (!stateDictionary.contains(string)) {
//  stateDictionary.contains(string);
//  }
//  assert stateDictionary.contains(string);
  }
  
  private void showWords(String myText) {
    System.out.format("Finding words in: \"%s\""+Utility.LINE_SEPARATOR, myText);
    if (SIMPLE_ONLY) {
      showWords("", simpleMatcher, myText);
    } else {
      Set<String> simpleResult = showWords("Simple", simpleMatcher, myText);
      Set<String> stateResult = showWords("STATE", stateMatcher, myText);
      if (!simpleResult.equals(stateResult)) {
        // repeat, for debugging
        System.out.println("  DIFFERENCE");
        showWords("Simple", simpleMatcher, myText);
        showWords("STATE", stateMatcher, myText);
        Set<String> simpleMinusState = new LinkedHashSet<String>(simpleResult);
        simpleMinusState.removeAll(stateResult);
        System.out.println("Simple-State" + simpleMinusState);
        Set<String> stateMinusSimple = new LinkedHashSet<String>(stateResult);
        stateMinusSimple.removeAll(simpleResult);
        System.out.println("State-Simple" + stateMinusSimple);
      }
    }
  }
  
  private Set<String> showWords(String title, Matcher<T> matcher, String myText) {
    title = title.equals("") ? "" : "\tType: " + title;
    // Walk through a strings and gather information about what we find
    // according to the matcher
    Set<String> result = new LinkedHashSet<String>();
    // Set the text to operate on
    matcher.setText(myText);
    boolean uniquePartial = false;
    for (int i = 0; matcher.hasCharAt(i); ++i) {
      matcher.setOffset(i);
      Status status;
      // We might get multiple matches at each point, so walk through all of
      // them. The last one might be a partial, so collect some extra
      // information in that case.
      do {
        // Sets matchValue if there is a MATCH
        status = matcher.next();
        if (status == Status.PARTIAL) {
          // Sets matchValue if the next() status was PARTIAL
          uniquePartial = matcher.nextUniquePartial();
        }
        // Format all of the information
        String info = String.format(
            "\tOffsets: %s,%s\tStatus: %s\tString: \"%s\"\tValue: %s%s", //
            matcher.getOffset(), matcher.getMatchEnd(), status, //
            matcher.getMatchText(), matcher.getMatchValue(), //
            status == Status.PARTIAL && uniquePartial ? "\tUNIQUE" : "");
        result.add(info);
        if (status != Status.NONE) {
          // If there was a match or partial match, show what we got
          System.out.println(title + info);
        }
      } while (status == Status.MATCH);
    }
    return result;
  }
}
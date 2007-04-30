package org.unicode.cldr.util;

import org.unicode.cldr.util.Dictionary.Status;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

public class TestStateDictionaryBuilder {
  private static final boolean SHOW_STATES = false;

  static boolean SIMPLE_ONLY = false;

  static boolean TEST_AGAINST_SIMPLE = true;

  static Dictionary stateDictionary = new StateDictionaryBuilder();

  static Dictionary simpleDictionary = new SimpleDictionary();

  public static void main(String[] args) {
    
    try {
      addToBoth("man", 1);
      addToBoth("manner", 100);
      addToBoth("many", 10);
      showDictionaryContents();
      
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
      stateDictionary = new StateDictionaryBuilder();
      simpleDictionary = new SimpleDictionary();

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
      stateDictionary = new StateDictionaryBuilder();
      simpleDictionary = new SimpleDictionary();

      testWithUnicodeNames();
      compare();
      
      if (false) {
        ((StateDictionaryBuilder) stateDictionary).flatten();
        compare();
        System.out.println();
        showDictionaryContents();
      }
    } finally {
      System.out.println("DONE");
    }
  }

  private static void showDictionaryContents() {
    if (SHOW_STATES) {
    System.out.println("States:\r\n" + stateDictionary);
    }
    System.out.println("Dictionary: " + stateDictionary.getMapping());
    System.out.println();
  }

  private static void testWithUnicodeNames() {
    stateDictionary = new StateDictionaryBuilder();
    simpleDictionary = new SimpleDictionary();
    UnicodeSet testSet = new UnicodeSet(
        "[[:assigned:] - [:ideographic:] - [:Co:] - [:Cs:]]"); // &
                                                                // [\\u0000-\\u0FFF]
    int count = 0;
    Map<String,Integer> data = new TreeMap();
    for (UnicodeSetIterator it = new UnicodeSetIterator(testSet); it.next();) {
      String name = UCharacter.getExtendedName(it.codepoint);
      if (name == null) {
        continue;
      }
      if ((++count & 0xFF) == 0) {
        System.out.println(count + ":\t"
            + com.ibm.icu.impl.Utility.hex(it.codepoint) + "\t" + name);
      }
      data.put(name, it.codepoint);
      if (false) for (String item : name.split("\\s+")) {
        data.put(item, it.codepoint);
      }
    }
    for (String item : data.keySet()) {
      addToBoth(item, data.get(item));
    }
  }

  private static void compare() {
    System.out.println("Comparing results: " + simpleDictionary.getMapping().size());
    Map<CharSequence, Integer> dictionaryData = stateDictionary.getMapping();
    Map<CharSequence, Integer> simpleDictionaryData = simpleDictionary.getMapping();
    assert dictionaryData.equals(simpleDictionaryData) : showDifference(dictionaryData, simpleDictionaryData);
    if (SHOW_STATES) {
      System.out.println("Size: " + dictionaryData.size());
      System.out.println("Rows: "
          + ((StateDictionaryBuilder) stateDictionary).getRowCount());
    }
    System.out.println("Checking values: state dictionary");
    checkSimpleMatches(stateDictionary, dictionaryData);
    System.out.println("Checking values: simple dictionary");
    checkSimpleMatches(simpleDictionary, simpleDictionaryData);
    int count = 0;
    System.out.println("Cross-checking all values");
    for (CharSequence myText : simpleDictionaryData.keySet()) {
      if ((++count & 0xFF) == 0xFF) {
        System.out.println(count + ":\t" + myText);
      }
      crossCheck(myText);
      crossCheck("!" + myText);
      crossCheck(myText + "!");
    }
  }

  private static String showDifference(Map<CharSequence, Integer> dictionaryData, Map<CharSequence, Integer> simpleDictionaryData) {
    System.out.println(dictionaryData.size() + ", " + simpleDictionaryData.size());
    Iterator<Entry<CharSequence, Integer>> it1 = dictionaryData.entrySet().iterator();
    Iterator<Entry<CharSequence, Integer>> it2 = simpleDictionaryData.entrySet().iterator();
    while (it1.hasNext() || it2.hasNext()) {
      Entry<CharSequence, Integer> item1 = it1.hasNext() ? it1.next() : null;
      Entry<CharSequence, Integer> item2 = it2.hasNext() ? it2.next() : null;
      System.out.println(item1 + ", " + item2);
      if (item1 == null || item2 == null || !item1.equals(item2)) {
        return item1 + "!=" + item2;
      }
    }
    return "no difference";
  }

  private static void crossCheck(CharSequence myText) {
    stateDictionary.setText(myText); // set the text to operate on
    simpleDictionary.setText(myText); // set the text to operate on
    for (int i = 0; i < stateDictionary.getText().length(); ++i) {
      stateDictionary.setOffset(i);
      simpleDictionary.setOffset(i);
      while (true) {
        Dictionary.Status stateStatus = stateDictionary.next();
        Dictionary.Status simpleStatus = simpleDictionary.next();
        assert stateStatus == simpleStatus : showValues(stateStatus, simpleStatus);
        assert stateDictionary.getMatchEnd() == simpleDictionary.getMatchEnd() 
          : showValues(stateStatus, simpleStatus);
        if (stateStatus == Status.PARTIAL) {
          boolean stateUnique = stateDictionary.nextUniquePartial();
          boolean simpleUnique = simpleDictionary.nextUniquePartial();
          assert stateUnique == simpleUnique 
            : showValues(stateStatus, simpleStatus);
        }
        // test this after checking PARTIAL
        assert stateDictionary.getMatchValue() == simpleDictionary.getMatchValue() 
          : showValues(stateStatus, simpleStatus);
        if (stateStatus != Status.MATCH) {
          break;
        }
      }
    }
  }

  private static String showValues(Status stateStatus, Status simpleStatus) {
    return "\r\nSTATE:\t" + showValues(stateStatus, stateDictionary) + "\r\nSIMPLE:\t" + showValues(simpleStatus, simpleDictionary);
  }

  private static String showValues(Status status, Dictionary dictionary) {
    boolean uniquePartial = status == Status.PARTIAL && dictionary.nextUniquePartial(); // sets matchValue for PARTIAL
    return String.format("\tOffsets: %s,%s\tStatus: %s\tString: \"%s\"\tValue: %d%s",
        dictionary.getOffset(),
        dictionary.getMatchEnd(),
        status,
        dictionary.getMatchText(),
        dictionary.getMatchValue(),
        status == Status.PARTIAL && uniquePartial ? "\tUNIQUE" : "");
  }

  /**
   * Check that the words all match against themselves.
   * 
   * @param dictionary
   * @param data
   */
  private static void checkSimpleMatches(Dictionary dictionary, Map<CharSequence, Integer> data) {
    int count = 0;
    for (CharSequence myText : data.keySet()) {
      if ((count++ & 0xFF) == 0xFF) {
        System.out.println(count + ":\t" + myText);
      }
      dictionary.setText(myText); // set the text to operate on

      dictionary.setOffset(0);
      int matchEnd = -1;
      int matchValue = -1;
      while (true) {
        Dictionary.Status next1 = dictionary.next();
        if (next1 == Dictionary.Status.MATCH) {
          matchEnd = dictionary.getMatchEnd();
          matchValue = dictionary.getMatchValue();
        } else {
          break;
        }
      }
      assert matchEnd == myText.length();
      assert matchValue == data.get(myText);
    }
  }

  private static void addToBoth(String string, int i) {
    if (simpleDictionary.contains(string)) return;
    ((Dictionary.Builder) simpleDictionary).addMapping(string, i);
    ((Dictionary.Builder) stateDictionary).addMapping(string, i);
    if (!stateDictionary.contains(string)) {
      stateDictionary.contains(string);
    }
    assert stateDictionary.contains(string);
  }

  private static void showWords(String myText) {
    System.out.format("Finding words in: \"%s\"\r\n", myText);
    if (SIMPLE_ONLY) {
      showWords("", simpleDictionary, myText);
    } else {
      Set<String> simpleResult = showWords("Simple", simpleDictionary, myText);
      Set<String> stateResult = showWords("STATE", stateDictionary, myText);
      if (!simpleResult.equals(stateResult)) {
        // repeat, for debugging
        System.out.println("  DIFFERENCE");
        showWords("Simple", simpleDictionary, myText);
        showWords("STATE", stateDictionary, myText);
        Set simpleMinusState = new LinkedHashSet(simpleResult);
        simpleMinusState.removeAll(stateResult);
        System.out.println("Simple-State" + simpleMinusState);
        Set stateMinusSimple = new LinkedHashSet(stateResult);
        stateMinusSimple.removeAll(simpleResult);
        System.out.println("State-Simple" + stateMinusSimple);
      }
    }
  }
  
  private static Set<String> showWords(String title, Dictionary dictionary, String myText) {
    Set<String> result = new LinkedHashSet();
    dictionary.setText(myText); // set the text to operate on
    title = title.equals("") ? "" : "\tType: " + title;
    boolean uniquePartial = false;
    for (int i = 0; i < dictionary.length(); ++i) {
      dictionary.setOffset(i);
      while (true) {
        Status status = dictionary.next(); // sets matchValue for MATCH
        if (status == Status.PARTIAL) {
          uniquePartial = dictionary.nextUniquePartial(); // sets matchValue for PARTIAL
        }
        String info = String.format("\tOffsets: %s,%s\tStatus: %s\tString: \"%s\"\tValue: %d%s",
            dictionary.getOffset(),
            dictionary.getMatchEnd(),
            status,
            dictionary.getMatchText(),
            dictionary.getMatchValue(),
            status == Status.PARTIAL && uniquePartial ? "\tUNIQUE" : "");
        result.add(info);  // remove
        info = title + info; // remove
        if (status != Status.NONE) {
          System.out.println(info);
        }
        if (status != Status.MATCH) {
          break;
        }
      }
    }
    return result;
  }
}
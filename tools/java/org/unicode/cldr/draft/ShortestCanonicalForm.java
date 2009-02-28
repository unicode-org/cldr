package org.unicode.cldr.draft;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.unicode.cldr.util.Utility;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.CanonicalIterator;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.text.UTF16.StringComparator;

public class ShortestCanonicalForm {
  
  static final UnicodeSet trailing = new UnicodeSet();
  static final UnicodeSet leading = new UnicodeSet();
  static final UnicodeSet leading3 = new UnicodeSet();
  static StringComparator cpCompare = new UTF16.StringComparator(true, false, 0);
  static final Map<Integer,Set<String>> leadToTrail = new TreeMap();
  static UnicodeSet skip = new UnicodeSet("[[:hangulsyllabletype=l:][:hangulsyllabletype=v:][:hangulsyllabletype=t:]]");
  static Map<String,String> restoreExclusions = new TreeMap();
  static UnicodeSet breakingTrail = new UnicodeSet();
  
  static {
    for (int i = 0; i <= 0x10FFFF; ++i) {
      final String nfd = Normalizer.normalize(i, Normalizer.NFD);
      if (skip.containsAll(nfd)) {
        continue;
      }
      
      final String nfc = Normalizer.normalize(i, Normalizer.NFC);
      if (nfc.codePointCount(0, nfc.length()) > 1) {
        restoreExclusions.put(nfc, UTF16.valueOf(i));
        int first = nfc.codePointAt(0);
        final String trailingString = nfc.substring(first > 0xFFFF ? 2 : 1);
        breakingTrail.addAll(trailingString);
      }
      int nfdLen = nfd.codePointCount(0, nfd.length());
      if (nfdLen > 1) {
        int first = nfd.codePointAt(0);
        leading.add(first);
        final String trailingString = nfd.substring(first > 0xFFFF ? 2 : 1);
        trailing.addAll(trailingString);
        Set<String> trails = leadToTrail.get(first);
        if (trails == null) {
          leadToTrail.put(first, trails = new TreeSet(cpCompare));
        }
        trails.add(trailingString);
        if (UScript.getScript(i) != UScript.HANGUL && trailingString.codePointCount(0, trailingString.length()) > 1) {
          leading3.add(first);
        }
      }
    }
    leading.freeze();
    trailing.freeze();
    for (int first : leadToTrail.keySet()) {
      if (leading3.contains(first)) {
        for (String trail : leadToTrail.get(first)) {
          System.out.println(Utility.hex(first) + "," + Utility.hex(trail, ",") + "\t" + UTF16.valueOf(first) + trail);
        }
      }
    }
    System.out.println("Breaking Trail:\t" + breakingTrail);
    System.out.println("Lead-only: " + new UnicodeSet(leading).removeAll(trailing));
    System.out.println("Trail-only: " + new UnicodeSet(trailing).removeAll(leading));
    System.out.println("Lead and trail: " + new UnicodeSet(leading).retainAll(trailing));
    
    UnicodeSet blockers = new UnicodeSet();
    
    for (int lead : leadToTrail.keySet()) {
      String leadStr = UTF16.valueOf(lead);
      System.out.println("Testing: " + leadStr);
      Set<String> trails = leadToTrail.get(lead);
      for (String trail1 : trails) {
        for (String trail2 : trails) {
          if (trail1.length() >= trail2.length()) {
            continue;
          }
          for (Iterator<String> it = new SubstringIterator(trail1); it.hasNext();) {
            String sub = it.next();
            String trial = leadStr + sub + trail2;
            String nfc = shortNFC(trial);
            String shortest = getShortest(trial, nfc);
            if (!shortest.equals(nfc)) {
              final int blocker = nfc.codePointAt(0);
              if (!blockers.contains(blocker)) {
                blockers.add(blocker);
                System.out.println("Adding blocker: " + Utility.hex(blocker) + "\t" + UTF16.valueOf(blocker));
                System.out.println("\tNFC: " + Utility.hex(nfc) + "\t" + nfc);
                System.out.println("\tShort: " + Utility.hex(shortest) + "\t" + shortest);
              }
            }
          }
        }
      }
    }
    System.out.println("Blockers: " + blockers);
  }

  private static String shortNFC(String trial) {
    String result = Normalizer.normalize(trial, Normalizer.NFC);
    result = result.replace("\u0308\u0301", "\u0344");
    for (String exclusion : restoreExclusions.keySet()) {
      // just simple case
      result = result.replace(exclusion, restoreExclusions.get(exclusion));
    }
    return result;
  }
  
  static class SubstringIterator implements Iterator<String> {
    String string;
    int start;
    int end;
    
    public SubstringIterator(String s) {
      string = s;
      start = 0;
      end = UCharacter.charCount(string.codePointAt(start));
    }
    
    public boolean hasNext() {
      return start < string.length();
    }

    public String next() {
      String result = string.substring(start, end);
      if (end < string.length()) {
        end += UCharacter.charCount(string.codePointAt(end));
      } else {
        start += UCharacter.charCount(string.codePointAt(start));
        if (start < string.length()) {
          end = start + UCharacter.charCount(string.codePointAt(start));
        }
      }
      return result;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
    
//    boolean insideCodePoint(String s, int offset) {
//      if (offset == 0 || offset == s.length()) {
//        return false;
//      }
//      char before = s.charAt(offset - 1);
//      char after = s.charAt(offset);
//      return 0xD800 <= before && before <= 0xDBFF && 0xDC00 <= after && after <= 0xDFFF;
//    }
  }
  
  private static String getShortest(String trial, String nfc) {
    String best = nfc;
    int bestLength = nfc.codePointCount(0, nfc.length());

    CanonicalIterator ci = new CanonicalIterator(trial);
    for (String s = ci.next(); s != null; s = ci.next()) {
      final int currentLength = s.codePointCount(0, s.length());
      if (currentLength < bestLength) {
        bestLength = currentLength;
        best = s;
      }
    }
    return best;
  }

  
  
  CanonicalIterator ci = new CanonicalIterator("");

  public String normalize(String input) {
    // optimize later
    StringBuffer b = new StringBuffer();
    
    boolean inProblem = false;
    int start = 0;
    final int len = input.length();
    // find chunks of NFD !NFD !NFD... and process them
    int cp;
    for (int i = 0; i < len; i += Character.charCount(cp)) {
      cp = input.codePointAt(i);
      boolean isNFD = trailing.contains(cp);
      if (isNFD) {
        if (inProblem) {
          // continue;
        } else {// reached end of problem
          b.append(process(input, start, i));
          start = i;
        }
      } else { // not NFD
        inProblem = true;
      }
    }
    if (start < len) {
      b.append(process(input, start, len));
    }
    return b.toString();
  }

  // of the form NFD? !NFD !NFD... 
  private String process(String input, int start, int end) {
    final String piece = input.substring(start, end);
    String nfc = shortNFC(piece);
    final int nfcLength = nfc.codePointCount(0, nfc.length());
    if (nfcLength == 1) {
      return piece;
    }
    
    ci.setSource(piece);
    String best = null;
    int bestLength = Integer.MAX_VALUE;
    for (String trial = ci.next(); trial != null; trial = ci.next()) {
      int len = trial.codePointCount(0, trial.length());
      if (len < bestLength) {
        best = trial;
        bestLength = len;
      }
    }
    if (nfcLength <= bestLength) {
      return nfc;
    }
    return best;
  }
  
  public static void main(String[] args) {
    ShortestCanonicalForm scf = new ShortestCanonicalForm();
    for (int i = 0; i <= 0x10FFFF; ++i) {
      String s = UTF16.valueOf(i);
      final String nfc = shortNFC(s);
      String shortest = scf.normalize(nfc);
      if (!shortest.equals(nfc)) {
        System.out.println("NFC not shortest: " + shortest + ", " + nfc);
      }
    }
  }
}

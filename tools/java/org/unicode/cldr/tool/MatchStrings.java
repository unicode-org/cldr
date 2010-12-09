package org.unicode.cldr.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import com.ibm.icu.dev.test.util.BagFormatter;
import com.ibm.icu.dev.test.util.Relation;

/**
 * Take mappings to IPA and interleave them.
 */
public class MatchStrings {

  static String cldrDataDir = "C:\\cvsdata\\unicode\\cldr\\tools\\java\\org\\unicode\\cldr\\util\\data\\transforms\\";

  static class Info {
    String english;

    String ipa;

    String fixedIpa;

    public Info(String english, String ipa, String fixedIpa) {
      this.english = english;
      this.ipa = ipa;
      this.fixedIpa = fixedIpa.equals(ipa) ? ipa : fixedIpa; // make ==
    }
    public String toString() {
      return "{" + english + "/" + ipa + (fixedIpa == ipa ? "" : "/" + fixedIpa) + "}";
    }
  }

  Relation<String, Info> letter_correspondances = new Relation(new TreeMap(),
      LinkedHashSet.class);

  MatchStrings() throws IOException {
    BufferedReader in = BagFormatter.openUTF8Reader(cldrDataDir,
        "internal_matchIpaRules.txt");
    while (true) {
      String line = in.readLine();
      if (line == null)
        break;
      if (line.length() == 0)
        continue;
      String[] parts = line.split("\\s+");
      String ipa = parts.length > 1 ? parts[1] : "";
      add(parts[0], ipa, parts.length > 2 ? parts[2] : ipa);
    }
    in.close();
  }

  void add(String english, String ipa, String fixedIpa) {
    String key = english.length() == 0 ? "" : english.substring(0, 1);
    letter_correspondances.put(key, new Info(english, ipa,
        fixedIpa));
  }

  /**
   * Insert the IPA in after the string, such as baitt + /bet/ => b{b}ai{e}t{t}
   * 
   * @param english
   * @param ipa
   * @return
   */
  int interleaveIPA(String english, String ipa, List<Info> output) {
    highWater = 0;
    longestEnglish = 0;
    longestIpa = 0;
    highWaterList.clear();
    this.english = english;
    this.ipa = ipa;
    this.output = output;
    output.clear();
    return interleave2(0, 0);
  }
  
  String english;
  String ipa;
  List<Info> output;
  int highWater = 0;
  List<Info> highWaterList = new ArrayList<Info>();
  private int longestEnglish;
  private int longestIpa;

  /**
   * Recursively match the string. Right now, we just take the matches in order;
   * later we could try a weighted fit
   * 
   * @param english
   * @param englishPosition
   * @param ipa
   * @param ipaPosition
   * @param output
   * @return
   */
  private int interleave2(int englishPosition, int ipaPosition) {

    if (highWater < ipaPosition) {
      highWaterList.clear();
      highWaterList.addAll(output);
      highWater = output.size();
      longestEnglish = englishPosition;
      longestIpa = ipaPosition;
    }
    if (englishPosition == english.length()) {
      if (ipaPosition == ipa.length()) {
        return 1;
      }
      return 0;
    }
    String firstLetter = english.substring( englishPosition, englishPosition + 1);
    Set<Info> possibilities = letter_correspondances.getAll(firstLetter);
    if (possibilities != null) {
      int result = checkPossibilities(possibilities, englishPosition,ipaPosition);
      if (result != 0) {
        return result;
      }
    }
    
    // we failed, try the empty string
    possibilities = letter_correspondances.getAll("");
    if (possibilities != null) {
      int result = checkPossibilities(possibilities, englishPosition, ipaPosition);
      if (result != 0) {
        return result;
      }
    }

    // failed, 

    // we failed to find a pair. Make last check to see if we just 
    // delete one English letter
    Info last = output.size() == 0 ? null : output.get(output.size()-1);
    if (last == null || last.ipa.length() != 0) {
      output.add(new Info(firstLetter,"",""));
      int result = interleave2(englishPosition + 1, ipaPosition);
      if (result == 1) {
        return 1;
      }
      // if we fail, then remove the pair, and continue
      output.remove(output.size() - 1);
    }
    
    // if we get this far, we've exhausted the possibilities, so fail
    return 0;
  }
  
  int checkPossibilities(Collection<Info> possibilities, int englishPosition, int ipaPosition) {
    for (Info englishIpa : possibilities) {
      // skip if we don't match
      String englishPart = englishIpa.english;
      String ipaPart = englishIpa.ipa;
      if (!english.regionMatches(englishPosition, englishPart, 0, englishPart.length())) {
        continue;
      }
//      boolean ipaMatches = ipa.regionMatches(ipaPosition, ipaPart, 0, ipaPart.length());
//      boolean ipa2Matches = matchAtIgnoring(ipaPosition, ipaPart);
//      if (ipaMatches != ipa2Matches) {
//        System.out.println("Fails " + ipa.substring(ipaPosition) + ", " + ipaPart);
//      }
      int matchesUpTo = matchAtIgnoring(ipaPosition, ipaPart);
      if (matchesUpTo < 0) {
        continue;
      }
      // we match, so recurse
      output.add(englishIpa);
      int result = interleave2(englishPosition + englishPart.length(), matchesUpTo);
      if (result == 1) {
        return 1;
      }
      // if we fail, then remove the pair, and continue
      output.remove(output.size() - 1);
    }
    return 0;
  }

  /**
   * Does ipaPart match ipa at the position, ignoring stress marks in ipa?
   * Returns how far it got.
   * @param ipaPosition
   * @param ipaPart
   * @return
   */
  private int matchAtIgnoring(int ipaPosition, String ipaPart) {
    if (ipaPart.length() == 0) return ipaPosition;
    int j = 0;
    for (int i = ipaPosition; i < ipa.length(); ++i) {
      char ch = ipa.charAt(i);
      if (ch == 'ˈ' || ch == 'ˌ') continue;
      char ch2 = ipaPart.charAt(j++);
      if (ch != ch2) return -1;
      if (j >= ipaPart.length()) return i+1;
    }
    return -1;
  }

  List<Info> current = new ArrayList();
  /**
   * Fix the IPA in a string
   * 
   * @param english
   * @param ipa
   * @return
   */
  String fixIPA(String english, String ipa) {
    int result = interleaveIPA(english, ipa, current);
    if (result == 0)
      return null;
    StringBuilder buffer = new StringBuilder();
    for (Info englishIpa : current) {
      buffer.append(englishIpa.fixedIpa);
    }
    return buffer.toString();
  }
  
  String getTrace() {
    return highWaterList.toString() + "\t\t" + english.substring(longestEnglish) + "\t≠\t" + ipa.substring(longestIpa);
  }
}

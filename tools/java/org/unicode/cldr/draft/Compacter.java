/**
 * Shim for doing compaction of strings
 */
package org.unicode.cldr.draft;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.ibm.icu.text.UTF16;

class Compacter {

  static boolean useCibus = false;

  
  /**
   * @param result output buffer for compacted form
   * @param set2 input is collection of strings, where each string is exactly one codepoint. The collection is read in Iterator order.
   */
  static String appendCompacted(Collection<String> set2) {
    if (Compacter.useCibus) {
      StringBuilder temp = new StringBuilder();
      for (String item : set2) {
        temp.appendCodePoint(item.codePointAt(0));
      }
      String compressed = CharacterListCompressor.base88Encode(temp.toString());
      return compressed;
    }
    StringBuilder result = new StringBuilder();
    int first = -1;
    int last = -1;
    for (String item : set2) {
      int cp = UTF16.charAt(item, 0);
      if (first == -1) {
        first = last = cp;
      } else if (cp == last + 1) {
        last = cp;
      } else {
        appendRange(result, first, last);
        first = last = cp;
      }
    }
    if (first != -1) {
      appendRange(result, first, last);
    }
    return result.toString();
  }

  private static void appendRange(StringBuilder result, int first, int last) {
    result.append(UTF16.valueOf(first));
    if (first != last) {
      int delta = 0xE000 + last - first;
      if (delta >= 0xF800) {
        throw new IllegalArgumentException("Range too large: " + GeneratePickerData.toHex(first, true) + "-" + GeneratePickerData.toHex(last, true));
      }
      result.appendCodePoint(delta);  
    }
  }

  /**
   * @param in compacted form
   * @return result list of strings, where each string is exactly one codepoint. The order is exactly the same as the input to compaction,
   * although the collection may be different.
   */
  static List<String> getFromCompacted(String in) {
    if (Compacter.useCibus) {
      String resultStr = CharacterListCompressor.base88Decode(in);
      List<String> result = new ArrayList<String>();
      int cp = 0;
      for (int i = 0; i < resultStr.length(); i += UTF16.getCharCount(cp)) {
        result.add(UTF16.valueOf(cp = resultStr.codePointAt(i)));
      }
      return result;
    }
    List<String> result = new ArrayList<String>();
    int cp;
    int first = 0;
    for (int i = 0; i < in.length(); i += Character.charCount(cp)) {
      cp = in.codePointAt(i);
      if (0xE000 <= cp && cp < 0xF800) {
        for (int j = first+1; j <= first + cp - 0xE000; ++j) {
          result.add(UTF16.valueOf(j));
        }
      } else {
        result.add(UTF16.valueOf(cp));
      }
      first = cp;
    }
    return result;
  }
}
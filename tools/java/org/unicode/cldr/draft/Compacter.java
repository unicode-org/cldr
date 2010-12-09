/**
 * Shim for doing compaction of strings
 */
package org.unicode.cldr.draft;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.unicode.cldr.draft.CharacterListCompressor.Interval;

import com.ibm.icu.text.UTF16;

class Compacter {

  static boolean useCibus = true;
  
  /**
   * @param result output buffer for compacted form
   * @param set2 input is collection of strings, where each string is exactly one codepoint. The collection is read in Iterator order.
   */
  public static String encodeString(Collection<String> set2) {
    List<Interval> intermediate = codePointsToIntervals(set2);
    return CharacterListCompressor.base88EncodeList(intermediate);
  }
  
  /**
   * @param result output buffer for compacted form
   * @param set2 input is collection of strings, where each string is exactly one codepoint. The collection is read in Iterator order.
   */
  public static List<String> decodeString(String encodedString) {
    List<Interval> intermediate = CharacterListCompressor.base88DecodeList(encodedString);
    List<String> result = new ArrayList<String>();
    for (Interval interval : intermediate) {
      for (int i = interval.first; i <= interval.last; ++i) {
        result.add(UTF16.valueOf(i));
      }
    }
    return result;
  }

  private static List<Interval> codePointsToIntervals(Collection<String> set2) {
    List<Interval> result = new ArrayList<Interval>();
    int first = -1;
    int last = -1;
    for (String item : set2) {
      int cp = UTF16.charAt(item, 0);
      if (first == -1) {
        first = last = cp;
      } else if (cp == last + 1) {
        last = cp;
      } else {
        result.add(new Interval(first,last));
        first = last = cp;
      }
    }
    if (first != -1) {
      result.add(new Interval(first,last));
    }
    return result;
  }
  
  /**
   * @param result output buffer for compacted form
   * @param set2 input is collection of strings, where each string is exactly one codepoint. The collection is read in Iterator order.
   */
  static String appendCompacted(Collection<String> set2) {
    if (Compacter.useCibus) {
      return encodeString(set2);
    }
    String resultStr = getInternalRangeString(set2);
    return resultStr;
  }

  public static String getInternalRangeString(Collection<String> set2) {
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
    
    String resultStr = result.toString();
    return resultStr;
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
      return decodeString(in);
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
package org.unicode.cldr.draft;

import com.ibm.icu.text.StringTransform;
import com.ibm.icu.text.UnicodeSet;

/**
 * Immutable StringTransform using a UnicodeSet.
 * @author markdavis
 */
public class UnicodeSetFilteredTransform extends FilteredTransform {
  private final UnicodeSet unicodeSet;
  
  public UnicodeSetFilteredTransform(UnicodeSet filter, StringTransform result) {
    super(result);
    unicodeSet = (UnicodeSet) filter.freeze();
  }
  
  // TODO optimize scanning, add strings
  @Override
  protected boolean getNextRegion(String text, int[] startEnd) {
    int i = startEnd[1];
    final int length = text.length();
    if (length <= i) {
      return false; // done
    }
    int cp;
    findOut:
    {
      // scan for items that are not in set
      for (; i < length; i += Character.charCount(cp)) {
        cp = text.codePointAt(i);
        if (unicodeSet.contains(cp)) {
          startEnd[0] = i;
          break findOut;
        }
      }
      startEnd[0] = startEnd[1] = i;
      return true;
    }
    findIn:
    {
      // now for items that are
      for (; i < length; i += Character.charCount(cp)) {
        cp = text.codePointAt(i);
        if (!unicodeSet.contains(cp)) {
          startEnd[1] = i;
          break findIn;
        }
      }
      startEnd[1] = i;
    }
    return true;
  }
  
  public String toString() {
    return ":: " + unicodeSet.toPattern(false) + ";\r\n" + super.toString();
  }
}

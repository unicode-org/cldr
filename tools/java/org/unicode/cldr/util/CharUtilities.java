package org.unicode.cldr.util;

public class CharUtilities {
  

  /**
   * Simple wrapper for CharSequence
   * @author markdavis
   *
   */
  public static class CharListWrapper <T extends CharSequence> implements CharList {
    protected T source;
    
    public CharListWrapper(T source) {
      this.source = source;
    }
    public boolean hasCharAt(int index) {
      return index < source.length();
    }
    public char charAt(int index) {
      return source.charAt(index);
    }
    public int sourceOffset(int index) {
      return index;
    }
    public CharList sublist(int start, int end) {
      return new CharListWrapper(source.subSequence(start, end));
    }
    public CharList sublist(int start) {
      return new CharListWrapper(source.subSequence(start, source.length()));
    }
    public int getKnownLength() {
      return source.length();
    }
    public CharSequence subSequence(int start, int end) {
      return source.subSequence(start, end);
    }
    @Override
    public String toString() {
      return source.toString();
    }
    public CharSequence sourceSubSequence(int start, int end) {
      return source.subSequence(sourceOffset(start), sourceOffset(end));
    }
  }
  
  /**
   * Return the code point order of two CharSequences.
   * If the text has isolated surrogates, they will not sort correctly.
   * @param text1
   * @param text2
   * @return
   */
  public static int compare(CharList text1, CharList text2) {
    int i1 = 0;
    int i2 = 0;

    while (true) {
      // handle running out of room
      if (!text1.hasCharAt(i1)) {
        if (text2.hasCharAt(i2)) {
          return 0;
        }
        return -1;
      } else if (text2.hasCharAt(i2)) {
        return 1;
      }
      int cp1 = text1.charAt(i1++);
      int cp2 = text2.charAt(i2++);
      // if they are different, do a fixup
      
      if (cp1 != cp2) {
        return (cp1 + utf16Fixup[cp1>>11]) -
        (cp2 + utf16Fixup[cp2>>11]);
      }
    }
  }
  private static final char utf16Fixup[]= {
    0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0x2000, 0xf800, 0xf800, 0xf800, 0xf800
  };
  
  /**
   * Return the code point order of two CharSequences.
   * If the text has isolated surrogates, they will not sort correctly.
   * @param text1
   * @param text2
   * @return
   */
  public static int compare(CharSequence text1, CharSequence text2) {
    int i1 = 0;
    int i2 = 0;

    while (true) {
      // handle running out of room
      if (i1 >= text1.length()) {
        if (i2 >= text2.length()) {
          return 0;
        }
        return -1;
      } else if (i2 >= text2.length()) {
        return 1;
      }
      int cp1 = text1.charAt(i1++);
      int cp2 = text2.charAt(i2++);
      // if they are different, do a fixup
      
      if (cp1 != cp2) {
        return (cp1 + utf16Fixup[cp1>>11]) -
        (cp2 + utf16Fixup[cp2>>11]);
      }
    }
  }

}
package org.unicode.cldr.util;

import java.util.Map;

/**
 * Implements a search for words starting at a given offset. Logically, it is
 * backed by a Map&lt;String,int&gt; (int's restricted to non-negative values).
 * You set the offset you are concerned about, then call next() until it doesn't
 * return MATCH. Along the way, you will get results. For example, here is some
 * sample code and results.
 * 
 * <pre>
 * System.out.println(&quot;Dictionary: &quot; + stateDictionary.getMapping());
 * for (int i = 0; i &lt; dictionary.length(); ++i) {
 *   dictionary.setOffset(i);
 *   while (true) {
 *     Status status = dictionary.next();
 *     if (status != Status.NONE) {
 *       String info = String.format(
 *           &quot;\tOffsets: %s,%s\tStatus: %s\tString: \&quot;%s\&quot;\tValue: %d&quot;, dictionary
 *               .getOffset(), dictionary.getMatchEnd(), status, dictionary
 *               .getMatchText(), dictionary.getMatchValue());
 *       result.add(info); // remove
 *       info = title + info; // remove
 *       System.out.println(info);
 *     }
 *     if (status == Status.MATCH) {
 *       break;
 *     }
 *   }
 * }
 * </pre>
 * 
 * Output:
 * 
 * <pre>
Finding words in: "ma"
*  Offsets: 0,2  Status: PLURAL  String: "ma"  Value: 5
Finding words in: "man"
*  Offsets: 0,3  Status: MATCH String: "man" Value: 5
*  Offsets: 0,3  Status: PLURAL  String: "man" Value: 15
Finding words in: "manx"
*  Offsets: 0,3  Status: MATCH String: "man" Value: 5
*  Offsets: 0,3  Status: PLURAL  String: "man" Value: 15
Finding words in: "mann"
*  Offsets: 0,3  Status: MATCH String: "man" Value: 5
*  Offsets: 0,4  Status: SINGLE  String: "mann"  Value: 15
Finding words in: "many"
*  Offsets: 0,3  Status: MATCH String: "man" Value: 5
*  Offsets: 0,4  Status: MATCH String: "many"  Value: 10
Finding words in: "many!"
*  Offsets: 0,3  Status: MATCH String: "man" Value: 5
*  Offsets: 0,4  Status: MATCH String: "many"  Value: 10
* </pre>
 * 
 * The difference between NONE, SINGLE, and PLURAL are according to whether
 * there would be any longer possible matches at the point where there are no
 * more matches. For example, suppose that the dictionary contains a mapping
 * from English month names to their values.
 * <ol>
 * <li>When we are parsing "Jul 1 1990", we will find a partial match at "Jul",
 * with the value 7, for July.</li>
 * <li>When we are parsing "Ju 1 1990", on the other hand, we will find a
 * multiple match at "Ju". While a value is returned, it is only for one of the
 * possible words ("June" and "July") so we can decide that the parse fails
 * since the month isn't sufficiently distinguished.</li>
 * </ol>
 * 
 * @author markdavis
 * 
 */
public abstract class Dictionary<T> {
  protected CharSequence text;

  protected int offset;

  protected int matchEnd;

//  /*
//   * A Dictionary may also have a builder, that allows the dictionary to be
//   * built at runtime.
//   */
//  public interface Builder<T> {
//    /**
//     * Add strings to the dictionary. It is an error to add the null string, or
//     * to add a string that is less than a previously added strings. That is,
//     * the strings must be added in ascending codepoint order.
//     * 
//     * @param text
//     * @param result
//     * @return
//     */
//    public Dictionary<T> getInstance(Map<CharSequence,T> source);
//  }

  /**
   * Set the target text to match within; also resets the offset to zero.
   * 
   * @param text
   * @return
   */
  public Dictionary setText(CharSequence text) {
    this.text = text;
    return setOffset(0);
  }

  /**
   * Retrieve the target text to match within.
   * 
   * @return
   */
  public CharSequence getText() {
    return text;
  }

  /**
   * Set the position in the target text to match from. Matches only go forwards
   * in the string.
   * 
   * @param offset
   * @return
   */
  public Dictionary setOffset(int offset) {
    this.offset = offset;
    matchEnd = offset;
    return this;
  }

  /**
   * Get the offset from which we are matching.
   * 
   * @return
   */
  public int getOffset() {
    return offset;
  }

  /**
   * Get the length of the target text.
   * 
   * @return
   */
  public int length() {
    return text.length();
  }

  /**
   * Get the latest match value after calling next(); see next() for more information.
   * 
   * @return
   */
  public abstract T getMatchValue();

  /**
   * Get the latest match value after calling next(); see next() for more information. 
   * 
   * @return
   */
  public int getIntMatchValue() {
    try {
      return (Integer)getMatchValue();
    } catch (Exception e) {
      return Integer.MIN_VALUE;
    }
  }

  /**
   * Get the latest match end (that is, how far we matched); see next() for more information.
   * 
   * @return
   */
  public int getMatchEnd() {
    return matchEnd;
  }

  /**
   * Get the text that we matched.
   * 
   * @return
   */
  public CharSequence getMatchText() {
    return text.subSequence(offset, matchEnd);
  }

  /**
   * The status of a match; see next() for more information.
   */
  public enum Status {
    /**
     * There are no further matches at all.
     */
    NONE,
    /**
     * There is a partial match for a single item. Example: dictionary contains
     * "man", text has "max". There will be a partial match after "ma".
     */
    PARTIAL,
    /**
     * There is a full match
     */
    MATCH,
  }

  /**
   * Finds the next match, and sets the matchValue and matchEnd. Normally you
   * call in a loop until you get a value that is not MATCH.<br>
   * <b>Warning: the results of calling next() after getting non-MATCH value are
   * undefined!</b><br>
   * Here is what happens with the different return values:
   * <ul>
   * <li>MATCH: there was an exact match. Its matchValue and matchEnd are set.</li>
   * <li>PARTIAL: there was a partial match. The matchEnd is set to the
   * furthest point that could be reached successfully. To get the matchValue,
   * and whether or not there were multiple partial matches, call nextPartial().</li>
   * <li>NONE: the matchValue and matchEnd are undefined.</li>
   * </ul>
   * 
   * @return MATCH if there is a match.
   */
  public abstract Status next();
  
  /**
   * Determine whether a partial match is singular (there is only one possible
   * continuation) or multiple (there are different continuations). Sets the
   * value of matchValue to that of the string that could have been returned if
   * appropriate additional characters were inserted at matchEnd. If there are
   * multiple possible strings, the matchValue is the one for the lowest (in
   * code point order) string.
   * <p>
   * This must only be called if there is a PARTIAL result from next()
   * 
   * @return true if the partial match is singular, false if it is plural.
   */
  public abstract boolean nextUniquePartial();

  /**
   * Get the strings from the dictionary. The Map is either read-only or a copy;
   * that is, modifications do not affect the builder.
   * 
   * @return
   */
  public abstract Map<CharSequence, T> getMapping();
  
  /**
   * Return the value for a given piece of text, or Integer.MIN_VALUE if there is none. May be overridden for efficiency.
   */
  public T get(CharSequence text) {
    setText(text); // set the text to operate on
    while (true) {
      Status next1 = next();
      if (next1 != Status.MATCH) {
        return null;
      } else if (getMatchEnd() == text.length()) {
        return getMatchValue();
      }
    }
  }
  
  /**
   * Return the value for a given piece of text, or Integer.MIN_VALUE if there is none. May be overridden for efficiency.
   */
  public boolean contains(CharSequence text) {
    setText(text); // set the text to operate on
    while (true) {
      Status next1 = next();
      if (next1 != Status.MATCH) {
        return false;
      } else if (getMatchEnd() == text.length()) {
        return true;
      }
    }
  }
  
  /**
   * Return more comprehensive debug info if available.
   * @return
   */
  public String debugShow() {
    return toString();
  }
  
  /**
   * Return the code point order of two CharSequences. Really ought to be a method on CharSequence.
   * If the text has isolated surrogates, they will not sort correctly.
   * @param text1
   * @param text2
   * @return
   */
  public static int compare(CharSequence text1, CharSequence text2) {
    int i1 = 0;
    int i2 = 0;
    int len1 = text1.length();
    int len2 = text2.length();
    while (true) {
      // handle running out of room
      if (i1 >= len1) {
        if (i2 >= len2) {
          return 0;
        }
        return -1;
      } else if (i2 >= len2) {
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
}
/*
 **********************************************************************
 * Copyright (c) 2006-2007, Google and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.util;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.unicode.cldr.util.CharUtilities.CharSourceWrapper;
import org.unicode.cldr.util.Dictionary.Matcher.Filter;
import org.unicode.cldr.util.Dictionary.Matcher.Status;

/**
 * Provides for detecting all words starting at a given offset, and returning a
 * value associated with each of the words. Logically, it is backed by a
 * Map&lt;String,Object&gt; You set the offset you are concerned about, then
 * call next() until it doesn't return MATCH. Along the way, you will get
 * results. For example, here is some sample code and results.
 * 
 * <pre>
 * System.out.println(&quot;Using dictionary: &quot; + dictionary.getMapping());
 * System.out.println(&quot;Searching in: {&quot; + sampleText + &quot;}&quot;);
 * // Dictionaries are immutable, so we create a Matcher to search/test text.
 * Matcher matcher = dictionary.getMatcher();
 * matcher.setText(sampleText);
 * while (true) {
 *   Status status = matcher.find();
 *   String unique = &quot;&quot;; // only set if needed
 *   if (status == Status.NONE) {
 *     break;
 *   } else if (status == Status.PARTIAL) {
 *     // sets the match value to the &quot;first&quot; partial match
 *     if (matcher.nextUniquePartial()) {
 *       unique = &quot;\tUnique&quot;;
 *     } else {
 *       unique = &quot;\tNot Unique&quot;;
 *     }
 *   }
 *   // Show results
 *   System.out.println(&quot;{&quot; + sampleText.substring(0, matcher.getOffset()) + &quot;[[[&quot;
 *       + sampleText.substring(matcher.getOffset(), matcher.getMatchEnd())
 *       + &quot;]]]&quot; + sampleText.substring(matcher.getMatchEnd()) + &quot;}\t&quot; + status
 *       + &quot;  \t{&quot; + matcher.getMatchValue() + &quot;}\t&quot; + unique);
 * }
 * </pre>
 * 
 * Output:
 * 
 * <pre>
 *   Using dictionary: {any=All, man=Woman, many=Few}
 *   Searching in: {many manners ma}
 *   {[[[man]]]y manners ma} MATCH   {Woman} 
 *   {[[[many]]] manners ma} MATCH   {Few} 
 *   {m[[[any]]] manners ma} MATCH   {All} 
 *   {many [[[man]]]ners ma} MATCH   {Woman} 
 *   {many [[[man]]]ners ma} PARTIAL   {Few}   Unique
 *   {many m[[[an]]]ners ma} PARTIAL   {All}   Unique
 *   {many manners [[[ma]]]} PARTIAL   {Woman}   Not Unique
 * </pre>
 * 
 * When you get a PARTIAL status, the match value is undefined. Often people
 * will just treat PARTIAL matches as if they were NONE. However, sometimes
 * people may be interested in finding out whether the text in question is the
 * truncation or abbreviation of a possible table value. In that case, you can
 * test further at that point, as is done above. For example, suppose that the
 * dictionary contains a mapping from English month names to their numeric
 * values.
 * <ol>
 * <li>When we are parsing "Jul 1 1990", we will find a unique partial match at
 * "Jul", with the value 7, for July, and use it.</li>
 * <li>When we are parsing "Ju 1 1990", on the other hand, we will find a
 * non-unique partial match at "Ju". While a value is returned, it is only for
 * one of the possible words ("June" and "July") so (for this application) we
 * can decide that the parse fails since the month isn't sufficiently
 * distinguished.</li>
 * </ol>
 * 
 * @author markdavis
 * 
 */
public abstract class Dictionary<T> {
  
  /**
   * Get the strings from the dictionary. The Map is either read-only or a copy;
   * that is, modifications do not affect the builder.
   * 
   * @return
   */
  public abstract Iterator<Entry<CharSequence, T>> getMapping();
  
  /**
   * Interface for building a new simple StateDictionary. The Map must be sorted
   * according to Dictionary.CHAR_SEQUENCE_COMPARATOR. It must not contain the key "".
   * 
   * @param source
   * @return
   */
  public interface DictionaryBuilder<T> {
    public Dictionary<T> make(Map<CharSequence, T> source);
  }
  
  /**
   * Return more comprehensive debug info if available.
   * @return
   */
  public String debugShow() {
    return toString();
  }
  
  public abstract Matcher<T> getMatcher();
  
  public abstract static class Matcher<T> {
    protected CharSource text;
    
    protected int offset;
    
    protected int matchEnd;
    
    protected T matchValue;
    
//  /*
//  * A Dictionary may also have a builder, that allows the dictionary to be
//  * built at runtime.
//  */
//  public interface Builder<T> {
//  /**
//  * Add strings to the dictionary. It is an error to add the null string, or
//  * to add a string that is less than a previously added strings. That is,
//  * the strings must be added in ascending codepoint order.
//  * 
//  * @param text
//  * @param result
//  * @return
//  */
//  public Dictionary<T> getInstance(Map<CharSequence,T> source);
//  }
    
    public boolean hasCharAt(int index) {
      return text.hasCharAt(index);
    }

    /**
     * Set the target text to match within; also resets the offset to zero, calling setOffset().
     * 
     * @param text
     * @return
     */
    public Matcher<T> setText(CharSource text) {
      this.text = text;
      return setOffset(0);
    }
    
    /**
     * Set the target text to match within; also resets the offset to zero, calling setOffset().
     * 
     * @param text
     * @return
     */
    public Matcher<T> setText(CharSequence text) {
      this.text = new CharUtilities.CharSourceWrapper(text);
      return setOffset(0);
    }
    
    /**
     * Retrieve the target text to match within.
     * 
     * @return
     */
    public CharSource getText() {
      return text;
    }
    
    /**
     * Set the position in the target text to match from. Matches only go forwards
     * in the string.
     * 
     * @param offset
     * @return
     */
    public Matcher<T> setOffset(int offset) {
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
     * Get the latest match value after calling next(); see next() for more information.
     * 
     * @return
     */
    public T getMatchValue() {
      return matchValue;
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
     * Get the text that we are matching in..
     * 
     * @return
     */
    public CharSource getMatchText() {
      return text.sublist(offset, matchEnd);
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
     * call in a loop until you get NONE.<br>
     * <b>Warning: the results of calling next() after getting NONE are
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
    
    public enum Filter {
      /**
       * Returns all values:  0 or more MATCH or PARTIAL values (with NONE at end)
       */
      ALL, 
      /**
       * Only 0 or more MATCH values (with NONE at end)
       */
      MATCHES, 
      /**
       * Only one value: the longest MATCH value
       */
      LONGEST_MATCH,
      /**
       * Only one value: the longest MATCH or PARTIAL
       */
      LONGEST, 
      /**
       * Only one value: the longest MATCH or unique PARTIAL
       */
      LONGEST_UNIQUE, 
      /**
       * Only one value: the longest MATCH or PARTIAL (but only the Partial if it is at the end).
       */
      LONGEST_WITH_FINAL_PARTIAL};
    
    /**
     * Finds the next match, and sets the matchValue and matchEnd. Normally you
     * call in a loop until you get NONE.<br>
     * <b>Warning: the results of calling next() after getting NONE are
     * undefined!</b><br>
     * Here is what happens with the different return values:
     * <ul>
     * <li>MATCH: there was an exact match. Its matchValue and matchEnd are
     * set.</li>
     * <li>PARTIAL: there was a partial match. The matchEnd is set to the
     * furthest point that could be reached successfully. To get the matchValue,
     * and whether or not there were multiple partial matches, call
     * nextPartial().</li>
     * <li>NONE: the matchValue and matchEnd are undefined.</li>
     * </ul>
     * Question: should we make the name more explicit, like nextAtOffset? Because
     * this iterates through the matches <i>at</i> an offset, not through offsets.
     * 
     * @return MATCH if there is a match.
     */
    public Status next(Filter filter) {
      if (filter == Filter.ALL) {
        return next();
      }
      Status lastStatus = Status.NONE;
      T lastValue = null;
      int lastEnd = -1;
      while (true) {
        Status status = next();
        if (status == Status.NONE) {
          if (lastValue == null) {
            return status;
          }
          matchEnd = lastEnd;
          matchValue = lastValue;
          return lastStatus;
        } else if (status == Status.MATCH
            || (status == Status.PARTIAL 
                && (filter == Filter.LONGEST
                    || filter == Filter.LONGEST_UNIQUE && nextUniquePartial()
                    || filter == Filter.LONGEST_WITH_FINAL_PARTIAL && !text.hasCharAt(matchEnd)))) {
          if (filter == Filter.MATCHES) {
            return status;
          }
          lastStatus = status;
          lastValue = getMatchValue();
          lastEnd = matchEnd;
        }
      }
    }

    /**
     * Determine whether a partial match is singular (there is only one possible
     * continuation) or multiple (there are different continuations).
     * <ul>
     * <li>If so, sets the value of matchValue to that of the string that could
     * have been returned if appropriate additional characters were inserted at
     * matchEnd. </li>
     * <li>If not, then the matchValue is indeterminate. </li>
     * </ul>
     * <p>
     * This must only be called immediatedly following a PARTIAL result from
     * next().
     * <p>
     * QUESTION: would it be useful to be able to get all the partial matches??
     * 
     * @return true if the partial match is singular, false if it is plural.
     */
    public abstract boolean nextUniquePartial();
    
    /**
     * Return the value for a given piece of text, or Integer.MIN_VALUE if there is none. May be overridden for efficiency.
     */
    public T get(CharSource text) {
      setText(text); // set the text to operate on
      while (true) {
        Status next1 = next();
        if (next1 != Status.MATCH) {
          return null;
        } else if (!text.hasCharAt(getMatchEnd())) {
          return getMatchValue();
        }
      }
    }
    
    /**
     * Advances the offset until next() doesn't return NONE. 
     */
    public Status find(Filter filter) {
      while (true) {
        Status status = next(filter);
        if (status != Status.NONE) {
          return status;
        } else if (!text.hasCharAt(getMatchEnd())) {
          return status;
        } else {
          nextOffset();
        }
      }
    }

    /**
     * Increment the offset in the text.
     */
    public Matcher nextOffset() {
      return setOffset(++offset);
    }
    
    /**
     * Convert the remaining text (after offset) to the target. Any substring with a MATCH is replaced by the value.toString(); other characters are copied. Converts to first (shortest) match..
     * TODO add parameter to pick between shortest and longest.
     * @param target
     */
    public Appendable convert(Appendable target) {
      try {
        while (text.hasCharAt(offset)) {
            Status status = next();
            if (status != Status.MATCH) {
              target.append(text.charAt(getOffset()));
              nextOffset();
            } else {
              target.append(getMatchValue().toString());
              setOffset(getMatchEnd());
          }
        }
        return target;
      } catch (IOException e) {
        throw (IllegalArgumentException) new IllegalArgumentException("Internal error").initCause(e);
      }
    }
    
    /**
     * For debugging
     */
    public String toString() {
      return "{offset: " + offset + ", end: " + matchEnd + ", value: " + matchValue + ", text: \"" + text.subSequence(0,text.getKnownLength()) 
      + (text.hasCharAt(text.getKnownLength()) ? "..." : "") + "\"}";
    }

    /**
     * Get the dictionary associated with this matcher.
     */
    public abstract Dictionary<T> getDictionary();

    /**
     * The offset is not yet at the end.
     * @return
     */
    public boolean hasMore() {
      return text.hasCharAt(offset);
    }
  }
  
  /**
   * Return the code point order of two CharSequences. Really ought to be a method on CharSequence.
   * If the text has isolated surrogates, they will not sort correctly.
   */
  public static final Comparator<CharSequence> CHAR_SEQUENCE_COMPARATOR = new Comparator<CharSequence>() {
    public int compare(CharSequence o1, CharSequence o2) {
      return CharUtilities.compare(o1, o2);
    }
  };
  
  
  public static class DictionaryCharList<T extends CharSequence> extends CharSourceWrapper<T> {
    protected boolean failOnLength = false;
    protected StringBuilder buffer = new StringBuilder();
    protected int[] sourceOffsets;
    protected Matcher<T> matcher;
    protected boolean atEnd;
    
    public DictionaryCharList(Dictionary<T> dictionary, T source) {
      super(source);
      matcher = dictionary.getMatcher().setText(source);
      atEnd = source.length() == 0;
      sourceOffsets = new int[source.length()];
    }
    
    public boolean hasCharAt(int index) {
      if (index >= buffer.length()) {
        if (atEnd) {
          return false;
        }
        growToOffset(index + 1);
        return index < buffer.length();
      }
      return true;
    }
    
    public char charAt(int index) {
      if (!atEnd && index >= buffer.length()) {
          growToOffset(index + 1);
      }
      return buffer.charAt(index);
    }
    
    // sourceOffsets contains valid entries up to buffer.length() + 1.
    private void growToOffset(int offset) {
      int length = buffer.length();
      while (length < offset && !atEnd) {
        Status status = matcher.next(Filter.LONGEST_MATCH);
        int currentOffset = matcher.getOffset();
        final int matchEnd = matcher.getMatchEnd();
        if (status == Status.MATCH) {
          final T replacement = matcher.getMatchValue();
          setOffsets(length + 1, replacement.length(), matchEnd);
          buffer.append(replacement);
          length = buffer.length();
          matcher.setOffset(matchEnd);
        } else {
          setOffsets(length + 1, 1, currentOffset + 1);
          buffer.append(source.charAt(currentOffset));
          length = buffer.length();
          matcher.nextOffset();
        }
        atEnd = matcher.getOffset() >= source.length();
      }
    }

    private void setOffsets(final int start, final int count, final int value) {
      final int length = start + count;
      if (sourceOffsets.length < length) {
        int newCapacity = sourceOffsets.length * 2 + 1;
        if (newCapacity < length + 50) {
          newCapacity = length + 50;
        }
        int[] temp = new int[newCapacity];
        System.arraycopy(sourceOffsets, 0, temp, 0, sourceOffsets.length);
        sourceOffsets = temp;
      }
      for (int i = start; i < length; ++i) {
        sourceOffsets[i] = value;
      }
    }
    
    public int fromSourceOffset(int offset) {
      // TODO fix to do real binary search; returning a determinate value.
      return Arrays.binarySearch(sourceOffsets, offset);
    }
    
    public int toSourceOffset(int offset) {
      if (offset > buffer.length()) {
        growToOffset(offset);
        if (offset > buffer.length()) {
          throw new ArrayIndexOutOfBoundsException(offset);
        }
      }
      return sourceOffsets[offset];
    }
    
    public CharSequence subSequence(int start, int end) {
      if (!atEnd && end > buffer.length()) {
        growToOffset(end);
      }
      return buffer.subSequence(start, end);
    }

    public CharSequence sourceSubSequence(int start, int end) {
      // TODO Auto-generated method stub
      return source.subSequence(toSourceOffset(start), toSourceOffset(end));
    }
    
    @Override
    public int getKnownLength() {
      return buffer.length();
    }
  }
 
  public static <A,B> Map<A,B> load(Iterator<Entry<A, B>> input, Map<A, B> output) {
    while (input.hasNext()) {
      Entry<A, B> entry = input.next();
      output.put(entry.getKey(), entry.getValue());
    }
    return output;
  }
}
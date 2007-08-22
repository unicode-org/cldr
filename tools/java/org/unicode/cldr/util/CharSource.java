/*
 **********************************************************************
 * Copyright (c) 2006-2007, Google and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.util;

  /**
   * Implements a sequence of chars that is logically based on another source, which
   * may require incremental processing. For example, where a 10M document is being searched,
   * one may not want to convert the whole document if an item might be found in the first few lines.
   * <p> To force the source to grow to the end, use hasCharAt(Integer.MAX_VALUE).
   * @author markdavis
   * 
   */
  public interface CharList {
    /**
     * Determine whether there is a char at the position. When converting from code that uses String or other CharSequence:
     * <ul><li>replace index < x.length() with x.hasCharAt(index)
     * <li>replace index >= x.length() with !x.hasCharAt(index)
     * <li>replace index == x.length() also with !x.hasCharAt(index) <b> [whenever we are iterating index upwards]</b>
     * </ul>
     * Throws an exception if index < 0.
     * @param index
     * @return
     */
    public boolean hasCharAt(int index);

    /**
     * Get the character at the index. Will grow the charlist if the source can supply the character. Will throw an exception if the index is less than zero or not less than the length. 
     */
    public char charAt(int index);

    /**
     * Get the source offset for the index. For example, where the source is UTF-8, this will differ significantly from the UTF-16 index used in CharSequence or String Java.
     * @param index
     * @return
     */
    public int sourceOffset(int index);

    /**
     * Corresponds to subSequence or subString. May grow the charlist if needed to get the char before end.
     * @param start
     * @param end
     * @return
     */
    public CharList sublist(int start, int end);
    
    /**
     * Corresponds to subSequence or subString. May grow the charlist if needed to get the char before end.
     * @param start
     * @param end
     * @return
     */
    public CharSequence subSequence(int start, int end);
    
    /**
     * Returns a CharList that starts at the given point, and extends further. Operations on either one may affect the other. 
     * @param start
     * @param end
     * @return
     */
    public CharList sublist(int start);
    
    /**
     * Return the length known so far. If hasCharAt(getKnownLength()) == false, then it is the real length.
     * @return
     */
    public int getKnownLength();
    
  }
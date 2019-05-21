/*
 **********************************************************************
 * Copyright (c) 2006-2007, Google and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.util;

/**
 * Implements a sequence of chars that is logically based on another source,
 * which may require incremental processing. For example, where a 10M document
 * is being searched, one may not want to convert the whole document if an item
 * might be found in the first few lines. Thus there is no length() method; use
 * hasCharAt instead.
 * <ul>
 * <li>To force the source to grow to the end, use hasCharAt(Integer.MAX_VALUE).
 * <li>The method discardBefore can be called to release the source before a given offset. Once it is called, then any
 * attempt to access data or offsets before that point will cause an exception.
 * <li>To see
 * </ul>
 *
 * @author markdavis
 *
 */
public interface CharSource {
    /**
     * Determine whether there is a char at the position. When converting from
     * code that uses String or other CharSequence:
     * <ul>
     * <li>replace index < x.length() with x.hasCharAt(index)
     * <li>replace index >= x.length() with !x.hasCharAt(index)
     * <li>replace index == x.length() also with !x.hasCharAt(index) <b> [whenever we are iterating index upwards]</b>
     * </ul>
     * Throws an exception if index < 0.
     *
     * @param index
     * @return
     */
    public boolean hasCharAt(int index);

    /**
     * Get the character at the index. Will grow the charlist if the source can
     * supply the character. Will throw an exception if the index is less than
     * zero or not less than the length.
     */
    public char charAt(int index);

    /**
     * Get the source offset for the index. For example, where the source is
     * UTF-8, this will differ significantly from the UTF-16 index used in
     * CharSequence or String Java. It will grow the internal source if possible to get to the index.
     * This will throw an exception if the index is
     * greater than or equal to the length, or if it is less than the last.
     *
     * @param index
     * @return
     */
    public int toSourceOffset(int index);

    /**
     * Get the index from the source offset; the reverse of the toSourceOffset method. It will not grow the source,
     * however.
     *
     * @param index
     * @return
     */
    public int fromSourceOffset(int index);

    /**
     * Corresponds to subSequence or subString. May grow the charlist if needed to
     * get the char before end.
     *
     * @param start
     * @param end
     * @return
     */
    public CharSource sublist(int start, int end);

    /**
     * Corresponds to subSequence or subString. May grow the charlist if needed to
     * get the char before end.
     *
     * @param start
     * @param end
     * @return
     */
    public CharSequence subSequence(int start, int end);

    /**
     * Returns a CharList that starts at the given point, and extends further.
     * Operations on either one may affect the other.
     *
     * @param start
     * @param end
     * @return
     */
    public CharSource sublist(int start);

    /**
     * Return the length known so far. If hasCharAt(getKnownLength()) == false,
     * then it is the real length.
     *
     * @return
     */
    public int getKnownLength();

    /**
     * Allow the implementation to discard source before the given index. After
     * this is called, all attempted access to data or offsets before the given
     * offset may throw an exception. This is a request; the implementation may
     * ignore it, and just leave the start at 0.
     */
    public CharSource setStart(int index);

    /**
     * Get the start index.
     */
    public int getStart();
}
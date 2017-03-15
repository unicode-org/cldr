/*
 **********************************************************************
 * Copyright (c) 2006-2007, Google and others.  All Rights Reserved.
 **********************************************************************
 * Author: Mark Davis
 **********************************************************************
 */
package org.unicode.cldr.util;

import java.util.ArrayList;

import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.CollationElementIterator;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.util.ULocale;

/**
 * This is intended to be a reference implementation for StringSearch. The
 * implementation is thus not high performance; it just transforms both the key
 * (what is searched for) and the target into collationElement space, and
 * searches there. It is however, architecturally cleaner in that it first has a
 * search that finds and extended range, then picks a boundary within that.
 * <p>
 * The key is that there is a match for a key string in a target text at positions <a,b> iff collator.compare(key,
 * target.substring(a,b)) == 0.
 *
 * @author markdavis
 */
public class ReferenceStringSearch {
    private static final int PADDING = 3;

    private RuleBasedCollator collator = (RuleBasedCollator) Collator.getInstance(ULocale.ROOT);

    private BreakIterator breaker;

    private String key;

    private String target;

    // all of the following are in processed CollationElementSpace

    private int[] keyBuffer;

    private CollationElementIterator2 targetIterator;

    private int[] targetBuffer;

    private int targetBufferStart;
    private int targetBufferLength;

    // Map from target items back to native indices
    private int[] targetBackMapBefore;

    private int[] targetBackMapAfter;
    private int lastAfter;

    // input flags to indicate which way we want to go within the range of
    // possible boundaries
    boolean widestStart, widestLimit;

    /**
     * A struct for showing match information with the range of possible
     * boundaries.
     */
    public static class ExtendedRange {
        int minStart, maxStart, minLimit, maxLimit;

        public String toString() {
            return minStart + ", " + maxStart + ", " + minLimit + ", " + maxLimit;
        }

        public String toString(String key, String target) {
            return "'" + target.substring(0, minStart) + "["
                + target.substring(minStart, maxStart) + "{"
                + target.substring(maxStart, minLimit) + "}"
                + target.substring(minLimit, maxLimit) + "]"
                + target.substring(maxLimit) + "'";
        }
    }

    /**
     * A struct that just shows the start/end, based on the settings given.
     */
    public static class Range {
        public int start;
        public int limit;

        public String toString() {
            return start + ", " + limit;
        }

        public String toString(String key, String target) {
            return "'" + target.substring(0, start) + "["
                + target.substring(start, limit) + "]"
                + target.substring(limit) + "'";
        }
    }

    public RuleBasedCollator getCollator() {
        return collator;
    }

    /**
     * If the collator settings are changed externally, be sure to call
     * setCollator();
     */
    public ReferenceStringSearch setCollator(RuleBasedCollator collator) {
        this.collator = collator;
        targetIterator = new CollationElementIterator2(collator);
        // reset the key and target, since their collationElements may change
        if (key != null) {
            setKey(key);
        }
        if (target != null) {
            setTarget(target);
        }
        return this;
    }

    public BreakIterator getBreaker() {
        return breaker;
    }

    public ReferenceStringSearch setBreaker(BreakIterator breaker) {
        this.breaker = breaker;
        if (target != null) {
            breaker.setText(target);
        }
        return this;
    }

    /**
     * If true, will pick the largest possible limit for a match.
     */
    public boolean isWidestLimit() {
        return widestLimit;
    }

    /**
     * If true, will pick the largest possible limit for a match.
     */
    public void setWidestLimit(boolean widestLimit) {
        this.widestLimit = widestLimit;
    }

    /**
     * If true, will pick the least possible start for a match.
     */
    public boolean isWidestStart() {
        return widestStart;
    }

    /**
     * If true, will pick the least possible start for a match.
     */
    public void setWidestStart(boolean widestStart) {
        this.widestStart = widestStart;
    }

    public String getKey() {
        return key;
    }

    public ReferenceStringSearch setKey(String key) {
        this.key = key;
        ArrayList<Integer> keyBufferList = new ArrayList<Integer>();
        CollationElementIterator2 keyIterator = new CollationElementIterator2(collator).setText(key);
        while (true) {
            int collationElement = keyIterator.nextProcessed();
            if (collationElement == CollationElementIterator.NULLORDER) {
                break;
            }
            // store the primary, plus the index before and after.
            keyBufferList.add(collationElement);
        }
        keyBuffer = getIntBuffer(keyBufferList);
        return this;
    }

    public int getNativeOffset() {
        return targetBackMapBefore[targetBufferStart];
    }

    public ReferenceStringSearch setNativeOffset(int nativeOffset) {
        if (nativeOffset < 0 || nativeOffset > target.length()) {
            throw new ArrayIndexOutOfBoundsException();
        }
        // use dumb implementation. Better implementation would leverage
        // contents of current buffer
        targetIterator.setOffset(nativeOffset);
        targetBufferStart = 0;
        targetBufferLength = 0;
        lastAfter = 0;
        if (nativeOffset != 0) {
            // we have to reset lastAfter!
            targetIterator.previousProcessed();
            lastAfter = targetIterator.offsetAfter;
        }
        fillBuffer();
        return this;
    }

    public String getTarget() {
        return target;
    }

    /**
     * Set the text to search within.
     */
    public ReferenceStringSearch setTarget(String target) {
        this.target = target;
        if (breaker != null) {
            breaker.setText(target);
        }
        targetIterator.setText(target); // TODO optimize creation
        // make the buffer just a bit bigger than we need
        // note: we could optimize to reuse buffers, but probably not worth it
        targetBuffer = new int[keyBuffer.length + PADDING];
        targetBackMapBefore = new int[keyBuffer.length + PADDING];
        targetBackMapAfter = new int[keyBuffer.length + PADDING];
        targetBufferStart = 0;
        lastAfter = 0;
        targetBufferLength = 0;
        fillBuffer();
        return this;
    }

    /**
     * We keep the circular buffer as start + length instead of start + limit so
     * we can distinguish the 0 case.
     *
     * @return
     */
    private boolean shiftBuffer() {
        lastAfter = targetBackMapAfter[targetBufferStart]; // remember last after
        ++targetBufferStart;
        if (targetBufferStart >= targetBuffer.length) {
            targetBufferStart = 0;
        }
        --targetBufferLength;
        return fillBuffer();
    }

    private boolean fillBuffer() {
        // TODO mark as done so we don't call extra at the end
        while (targetBufferLength < keyBuffer.length + 1) {
            int ce = targetIterator.nextProcessed();
            if (ce == CollationElementIterator.NULLORDER) {
                return false;
            }
            int targetBufferLimit = targetBufferStart + targetBufferLength;
            if (targetBufferLimit >= targetBuffer.length) {
                targetBufferLimit -= targetBuffer.length;
            }
            targetBuffer[targetBufferLimit] = ce;
            targetBackMapBefore[targetBufferLimit] = targetIterator.offsetBefore;
            targetBackMapAfter[targetBufferLimit] = targetIterator.offsetAfter;
            ++targetBufferLength;
        }
        return true;
    }

    /**
     * Find the next match, and set the min/maxStart/Limit. Return false if not
     * found. Here is an example, where [...] is the maximal match, and {..} the
     * minimal.
     *
     * <pre>
     *  Raw Positions of 'eß' in ' ess eß ESS̀ '
     *    '[ {ess} ]eß ESS̀ '
     *    ' ess[ {eß} ]ESS̀ '
     *    ' ess eß[ {ESS}̀ ]'
     * </pre>
     *
     * Matches may overlap. Thus
     *
     * <pre>
     *  Raw Positions of 'a!a' in 'b.a.a.a.b'
     *    'b[.{a.a}.]a.b'
     *    'b.a[.{a.a}.]b'
     * </pre>
     */
    public boolean searchForwards(ExtendedRange position) {
        while (targetBufferLength >= keyBuffer.length) {
            if (matchesAt()) {
                // minStart is from the After of previous item.
                position.minStart = lastAfter;

                position.maxStart = targetBackMapBefore[targetBufferStart];
                int last = targetBufferStart + keyBuffer.length - 1;
                if (last >= targetBuffer.length) {
                    last -= targetBuffer.length;
                }
                position.minLimit = targetBackMapAfter[last];

                // maxLimit is from the Before of the next item
                // or, if we are at the very end of the text, the text length.
                if (targetBufferLength == keyBuffer.length) {
                    position.maxLimit = target.length();
                } else {
                    ++last; // we reuse the position since we are already there.
                    if (last >= targetBuffer.length) {
                        last -= targetBuffer.length;
                    }
                    position.maxLimit = targetBackMapBefore[last];
                }
                shiftBuffer(); // move to next offset
                return true;
            }
            shiftBuffer(); // move to next offset
        }
        return false;
    }

    /**
     * Simple match at position.
     */
    private boolean matchesAt() {
        int j = targetBufferStart;
        for (int i = 0; i < keyBuffer.length; ++i) {
            if (keyBuffer[i] != targetBuffer[j]) {
                return false;
            }
            ++j;
            if (j >= targetBuffer.length) {
                j = 0;
            }
        }
        return true;
    }

    private ExtendedRange internalPosition = new ExtendedRange();

    /**
     * This is the main public interface for searching. It filters out anything
     * that doesn't match the breaker, and adjusts the boundaries to the max/min
     * permitted. Examples:
     *
     * <pre>
     *  Positions of 'eß' in ' ess eß ESS̀ '
     *    ' [ess] eß ESS̀ '
     *    ' ess [eß] ESS̀ '
     *    ' ess eß [ESS̀] '
     *    Count: 3
     *
     * Positions of 'a!a' in 'b.a.a.a.b'
     *    'b.[a.a].a.b'
     *    'b.a.[a.a].b'
     *    Count: 2
     * </pre>
     *
     * @param position
     * @return
     */
    public boolean searchForwards(Range position) {
        while (true) {
            boolean succeeds = searchForwards(internalPosition);
            if (!succeeds) return false;
            position.start = getBoundary(breaker, internalPosition.minStart, internalPosition.maxStart, !widestStart);
            if (position.start == -1) continue; // failed to find the right boundary
            position.limit = getBoundary(breaker, internalPosition.minLimit, internalPosition.maxLimit, widestLimit);
            if (position.limit == -1) continue; // failed to find the right boundary
            return true;
        }
    }

    /****************** PRIVATES ***************/

    /**
     * This really ought to be just methods on CollationElementIterator.
     */
    public static class CollationElementIterator2 {
        private CollationElementIterator keyIterator;
        private int strengthMask;
        private int variableTop;
        private int offsetBefore;
        private int offsetAfter;

        public int getOffsetBefore() {
            return offsetBefore;
        }

        public int getOffsetAfter() {
            return offsetAfter;
        }

        public CollationElementIterator2 reset() {
            keyIterator.reset();
            return this;
        }

        public CollationElementIterator2 setOffset(int offset) {
            keyIterator.setOffset(offset);
            return this;
        }

        public CollationElementIterator2 setText(String source) {
            keyIterator.setText(source);
            return this;
        }

        public CollationElementIterator2(RuleBasedCollator collator) {
            // gather some information that we will need later
            strengthMask = 0xFFFF0000;
            variableTop = !collator.isAlternateHandlingShifted() ? -1 : collator.getVariableTop() | 0xFFFF;
            // this needs to be fixed a bit for case-level, etc.
            switch (collator.getStrength()) {
            case Collator.PRIMARY:
                strengthMask = 0xFFFF0000;
                break;
            case Collator.SECONDARY:
                strengthMask = 0xFFFFFF00;
                break;
            default:
                strengthMask = 0xFFFFFFFF;
                break;
            }
            keyIterator = collator.getCollationElementIterator("");
        }

        /**
         * This should be a method on CollationElementIterator. Returns next
         * non-zero collation element, setting indexBefore, indexAfter. Should also
         * process shifted and strength, masking as needed. If a collation element
         * has a continuation, then the indexAfter = indexBefore, for example, if
         * [CE1,CE2] form a single collation element for the characters between
         * native indexes 5 and 8, (C2 being a continuation, then the result of two
         * calls to nextProcessed would be [CE1, 5, 5] then [CE1, 5,8].
         * <p>
         * previousProcessed would do similar things, backwards.
         *
         */
        int nextProcessed() {
            while (true) {
                offsetBefore = keyIterator.getOffset();
                int collationElement = keyIterator.next();
                if (collationElement != CollationElementIterator.NULLORDER) {

                    // note: the collation element iterator ought to give us processed values, but it doesn't
                    // so we have to simulate that.
                    collationElement &= strengthMask; // mask to only the strengths we have
                    // check for shifted.
                    // TODO This is not exactly right, and we also need to eject any following combining marks,
                    // so fix later.
                    if (collationElement < variableTop && collationElement > 0xFFFF) {
                        continue;
                    }
                    if (collationElement == 0) {
                        continue;
                    }

                }
                offsetAfter = keyIterator.getOffset();
                return collationElement;
            }
        }

        int previousProcessed() {
            while (true) {
                offsetAfter = keyIterator.getOffset();
                int collationElement = keyIterator.previous();
                if (collationElement != CollationElementIterator.NULLORDER) {

                    // note: the collation element iterator ought to give us processed values, but it doesn't
                    // so we have to simulate that.
                    collationElement &= strengthMask; // mask to only the strengths we have
                    // check for shifted.
                    // TODO This is not exactly right, and we also need to eject any following combining marks,
                    // so fix later.
                    if (collationElement < variableTop && collationElement > 0xFFFF) {
                        continue;
                    }
                    if (collationElement == 0) {
                        continue;
                    }

                }
                offsetBefore = keyIterator.getOffset();
                return collationElement;
            }
        }
    }

    /**
     * Utility
     *
     * @param keyBufferList
     * @return
     */
    private int[] getIntBuffer(ArrayList<Integer> keyBufferList) {
        int[] buffer = new int[keyBufferList.size()];
        for (int i = 0; i < keyBufferList.size(); ++i) {
            buffer[i] = keyBufferList.get(i).intValue();
        }
        return buffer;
    }

    /**
     * This really should be on breakIterator, so it can be done more efficiently.
     * <p>
     * Get the item that is on a boundary according to the breaker, and is between the input boundaries. The boolean
     * greatest controls whether we pick the least or the greatest possible offset that works according to the breaker.
     * <p>
     * Returns -1 if there is no valid offset according to the breaker.
     *
     * @param minBoundary
     * @param maxBoundary
     * @param greatest
     * @return
     */
    public static int getBoundary(BreakIterator breaker, int minBoundary, int maxBoundary, boolean greatest) {
        if (breaker == null) return greatest ? maxBoundary : minBoundary;
        int result;
        // this may or may not be the most efficient way to test; ask Andy
        if (greatest) {
            result = breaker.preceding(maxBoundary + 1);
            if (result < minBoundary) {
                result = -1;
            }
        } else {
            result = breaker.following(minBoundary - 1);
            if (result < minBoundary) {
                result = -1;
            }
        }
        return result;
    }

}
/**
*******************************************************************************
* Copyright (C) 1996-2009, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

package org.unicode.cldr.util;

/**
 * VERY Basic Diff program. Compares two sequences of objects fed into it, and
 * lets you know where they are different.
 * @author Mark Davis
 * @version 1.0
 */
final public class Differ<T> {
    /**
     * @param stackSize The size of the largest difference you expect.
     * @param matchCount The number of items that have to be the same to count as a match
     */
    @SuppressWarnings("unchecked")
    public Differ(int stackSize, int matchCount) {
        this.STACKSIZE = stackSize;
        this.EQUALSIZE = matchCount;
        a = (T[]) new Object[stackSize + matchCount];
        b = (T[]) new Object[stackSize + matchCount];
    }

    public void add(T aStr, T bStr) {
        addA(aStr);
        addB(bStr);
    }

    public void addA(T aStr) {
        flush();
        a[aCount++] = aStr;
    }

    public void addB(T bStr) {
        flush();
        b[bCount++] = bStr;
    }

    public int getALine(int offset) {
        return aLine + maxSame + offset;
    }

    public T getA(int offset) {
        if (offset < 0) return last;
        if (offset > aTop - maxSame) return next;
        return a[maxSame + offset];
    }

    public int getACount() {
        return aTop - maxSame;
    }

    public int getBCount() {
        return bTop - maxSame;
    }

    public int getBLine(int offset) {
        return bLine + maxSame + offset;
    }

    public T getB(int offset) {
        if (offset < 0) return last;
        if (offset > bTop - maxSame) return next;
        return b[maxSame + offset];
    }

    /**
     * Checks for initial & final match.
     * To be called after addA() and addB().
     * Middle segments that are different are returned via get*Count() and get*().
     *
     * @param finalPass true if no more input
     */
    public void checkMatch(boolean finalPass) {
        // find the initial strings that are the same
        int max = aCount;
        if (max > bCount) max = bCount;
        int i;
        for (i = 0; i < max; ++i) {
            if (!a[i].equals(b[i])) break;
        }
        // at this point, all items up to i are equal
        maxSame = i;
        aTop = bTop = maxSame;
        if (maxSame > 0) last = a[maxSame - 1];
        next = null;

        if (finalPass) {
            aTop = aCount;
            bTop = bCount;
            next = null;
            return;
        }

        if (aCount - maxSame < EQUALSIZE || bCount - maxSame < EQUALSIZE) return;

        // now see if the last few a's occur anywhere in the b's, or vice versa
        int match = find(a, aCount - EQUALSIZE, aCount, b, maxSame, bCount);
        if (match != -1) {
            aTop = aCount - EQUALSIZE;
            bTop = match;
            next = a[aTop];
            return;
        }
        match = find(b, bCount - EQUALSIZE, bCount, a, maxSame, aCount);
        if (match != -1) {
            bTop = bCount - EQUALSIZE;
            aTop = match;
            next = b[bTop];
            return;
        }
        if (aCount >= STACKSIZE || bCount >= STACKSIZE) {
            // flush some of them
            aCount = (aCount + maxSame) / 2;
            bCount = (bCount + maxSame) / 2;
            next = null;
        }
    }

    /**
     * Finds a segment of the first array in the second array.
     * @return -1 if not found, otherwise start position in bArr
     */
    private int find(T[] aArr, int aStart, int aEnd, T[] bArr, int bStart, int bEnd) {
        int len = aEnd - aStart;
        int bEndMinus = bEnd - len;
        tryA: for (int i = bStart; i <= bEndMinus; ++i) {
            for (int j = 0; j < len; ++j) {
                if (!bArr[i + j].equals(aArr[aStart + j])) continue tryA;
            }
            return i; // we have a match!
        }
        return -1;
    }

    // ====================== PRIVATES ======================

    /** Removes equal prefixes of both arrays. */
    private void flush() {
        if (aTop != 0) {
            int newCount = aCount - aTop;
            System.arraycopy(a, aTop, a, 0, newCount);
            aCount = newCount;
            aLine += aTop;
            aTop = 0;
        }

        if (bTop != 0) {
            int newCount = bCount - bTop;
            System.arraycopy(b, bTop, b, 0, newCount);
            bCount = newCount;
            bLine += bTop;
            bTop = 0;
        }
    }

    private int STACKSIZE;
    private int EQUALSIZE;

    // a[] and b[] are equal at 0 to before maxSame.
    // maxSame to before *Top are different.
    // *Top to *Count are equal again.
    private T[] a;
    private T[] b;
    private T last = null;
    private T next = null;
    private int aCount = 0;
    private int bCount = 0;
    private int aLine = 1;
    private int bLine = 1;
    private int maxSame = 0, aTop = 0, bTop = 0;
}

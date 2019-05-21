package org.unicode.cldr.util;

import java.util.EnumSet;

import org.unicode.cldr.util.SupplementalDataInfo.PluralInfo.Count;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.util.Freezable;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;

/**
 * Utility class for returning the plural category for a range of numbers, such as 1–5, so that appropriate messages can be chosen.
 * The rules for determining this value vary widely across locales.
 * @author markdavis
 */
public final class PluralRanges implements Comparable<PluralRanges>, Freezable<PluralRanges> {

    /**
     * Internal class for mapping from two Count values to another.
     * @internal
     * @deprecated
     */
    public static final class Matrix implements Comparable<Matrix>, Cloneable {
        private byte[] data = new byte[Count.LENGTH * Count.LENGTH];
        {
            for (int i = 0; i < data.length; ++i) {
                data[i] = -1;
            }
        }

        /**
         * Internal method for setting.
         * @internal
         * @deprecated
         */
        public void set(Count start, Count end, Count result) {
            data[start.ordinal() * Count.LENGTH + end.ordinal()] = result == null ? (byte) -1 : (byte) result.ordinal();
        }

        /**
         * Internal method for setting; throws exception if already set.
         * @internal
         * @deprecated
         */
        public void setIfNew(Count start, Count end, Count result) {
            byte old = data[start.ordinal() * Count.LENGTH + end.ordinal()];
            if (old >= 0) {
                throw new IllegalArgumentException("Previously set value for <" +
                    start + ", " + end + ", " + Count.VALUES.get(old) + ">");
            }
            data[start.ordinal() * Count.LENGTH + end.ordinal()] = result == null ? (byte) -1 : (byte) result.ordinal();
        }

        /**
         * Internal method for getting.
         * @internal
         * @deprecated
         */
        public Count get(Count start, Count end) {
            byte result = data[start.ordinal() * Count.LENGTH + end.ordinal()];
            return result < 0 ? null : Count.VALUES.get(result);
        }

        /**
         * Internal method to see if <*,end> values are all the same.
         * @internal
         * @deprecated
         */
        public Count endSame(Count end) {
            Count first = null;
            for (Count start : Count.VALUES) {
                Count item = get(start, end);
                if (item == null) {
                    continue;
                }
                if (first == null) {
                    first = item;
                    continue;
                }
                if (first != item) {
                    return null;
                }
            }
            return first;
        }

        /**
         * Internal method to see if <start,*> values are all the same.
         * @internal
         * @deprecated
         */
        public Count startSame(Count start, EnumSet<Count> endDone, Output<Boolean> emit) {
            emit.value = false;
            Count first = null;
            for (Count end : Count.VALUES) {
                Count item = get(start, end);
                if (item == null) {
                    continue;
                }
                if (first == null) {
                    first = item;
                    continue;
                }
                if (first != item) {
                    return null;
                }
                // only emit if we didn't cover with the 'end' values
                if (!endDone.contains(end)) {
                    emit.value = true;
                }
            }
            return first;
        }

        @Override
        public int hashCode() {
            int result = 0;
            for (int i = 0; i < data.length; ++i) {
                result = result * 37 + data[i];
            }
            return result;
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Matrix)) {
                return false;
            }
            return 0 == compareTo((Matrix) other);
        }

        @Override
        public int compareTo(Matrix o) {
            for (int i = 0; i < data.length; ++i) {
                int diff = data[i] - o.data[i];
                if (diff != 0) {
                    return diff;
                }
            }
            return 0;
        }

        @Override
        public Matrix clone() {
            Matrix result = new Matrix();
            result.data = data.clone();
            return result;
        }
    }

    private Matrix matrix = new Matrix();
    private boolean[] explicit = new boolean[Count.LENGTH];

    /**
     * Returns a
     * @return
     */
    public static PluralRanges getInstance(ULocale locale) {
        return null;
    }

    /**
     * Internal method for building. If the start or end are null, it means everything of that type.
     * @param rangeStart
     * @param rangeEnd
     * @param result
     * @internal
     * @deprecated
     */
    public void add(Count rangeStart, Count rangeEnd, Count result) {
        explicit[result.ordinal()] = true;
        if (rangeStart == null) {
            for (Count rs : Count.values()) {
                if (rangeEnd == null) {
                    for (Count re : Count.values()) {
                        matrix.setIfNew(rs, re, result);
                    }
                } else {
                    explicit[rangeEnd.ordinal()] = true;
                    matrix.setIfNew(rs, rangeEnd, result);
                }
            }
        } else if (rangeEnd == null) {
            explicit[rangeStart.ordinal()] = true;
            for (Count re : Count.values()) {
                matrix.setIfNew(rangeStart, re, result);
            }
        } else {
            explicit[rangeStart.ordinal()] = true;
            explicit[rangeEnd.ordinal()] = true;
            matrix.setIfNew(rangeStart, rangeEnd, result);
        }
    }

    /**
     * Internal method to show a range in XML format.
     * @param start
     * @param end
     * @param result
     * @return
     * @internal
     * @deprecated
     */
    public static String showRange(Count start, Count end, Count result) {
        String startEnd = "start=\"" + start + "\"" + Utility.repeat(" ", 5 - start.toString().length())
            + " end=\"" + end + "\"" + Utility.repeat(" ", 5 - end.toString().length());
        return result == null
            ? "<!--         " + startEnd + " result=? -->"
            : "<pluralRange " + startEnd + " result=\"" + result + "\"/>";
    }

    /**
     * Returns the appropriate plural category for a range from start to end.
     * If there is no available data, then 'other' is returned.
     * @param start
     * @param end
     * @return
     */
    public Count get(Count start, Count end) {
        Count result = matrix.get(start, end);
        return result == null ? Count.other : result;
    }

    /**
     * Returns the appropriate plural category for a range from start to end. If the combination does not
     * explicitly occur in the data, returns null.
     * @param start
     * @param end
     * @return
     */
    public Count getExplicit(Count start, Count end) {
        return matrix.get(start, end);
    }

    /**
     * Internal method to determines whether the Count was explicitly used in any add statement.
     * @param count
     * @return
     * @internal
     * @deprecated
     */
    public boolean isExplicitlySet(Count count) {
        return explicit[count.ordinal()];
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PluralRanges ? matrix.equals((PluralRanges) other) : false;
    }

    @Override
    public int hashCode() {
        return matrix.hashCode();
    }

    @Override
    public int compareTo(PluralRanges that) {
        return matrix.compareTo(that.matrix);
    }

    volatile boolean isFrozen;

    @Override
    public boolean isFrozen() {
        return isFrozen;
    }

    @Override
    public PluralRanges freeze() {
        isFrozen = true;
        return this;
    }

    @Override
    public PluralRanges cloneAsThawed() {
        PluralRanges result = new PluralRanges();
        result.explicit = explicit.clone();
        result.matrix = matrix.clone();
        return result;
    }
}
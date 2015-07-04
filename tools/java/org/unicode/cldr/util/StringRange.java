package org.unicode.cldr.util;

import java.util.Collection;
import java.util.Set;

import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.util.ICUException;

@SuppressWarnings("deprecation")
public class StringRange {

    public interface Adder {
        /**
         * @param start
         * @param end   may be null, for adding single string
         */
        void add(String start, String end);
    }

    /**  
     * @param source must be sorted in code point order!
     * @param differentLengths TODO
     * @param output
     * @return
     */
    public static void compact(Set<String> source, Adder adder, boolean mostCompact) {
        String start = null;
        String end = null;
        int lastCp = 0;
        int prefixLen = 0;
        for (String s : source) {
            if (start != null) { // We have something queued up
                if (s.regionMatches(0, start, 0, prefixLen)) {
                    int currentCp = s.codePointAt(prefixLen);
                    if (currentCp == 1+lastCp && s.length() == prefixLen + Character.charCount(currentCp)) {
                        end = s;
                        lastCp = currentCp;
                        continue;
                    }
                }
                // We failed to find continuation. Add what we have and restart
                adder.add(start, end == null ? null 
                    : !mostCompact ? end 
                        : end.substring(prefixLen, end.length()));
            }
            // new possible range
            start = s;
            end = null;
            lastCp = s.codePointBefore(s.length());
            prefixLen = s.length() - Character.charCount(lastCp);
        }
        adder.add(start, end == null ? null 
            : !mostCompact ? end 
                : end.substring(prefixLen, end.length()));
    }
    static Collection<String> expand(String start, String end, Collection<String> output) {
        int[] startCps = CharSequences.codePoints(start);
        int[] endCps = CharSequences.codePoints(end);
        int startOffset = startCps.length - endCps.length;

        if (startOffset < 0) {
            throw new ICUException("Must have start-length ≥ end-length");
        } else if (endCps.length == 0) {
            throw new ICUException("Must have end-length > 0");
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < startOffset; ++i) {
            builder.appendCodePoint(startCps[i]);
        }
        add(0, startOffset, startCps, endCps, builder, output);
        return output;
    }
    private static void add(int endIndex, int startOffset, int[] starts, int[] ends, StringBuilder builder, Collection<String> output) {
        int start = starts[endIndex+startOffset];
        int end = ends[endIndex];
        if (start > end) {
            throw new ICUException("Each two corresponding characters ...x... and ...y... must have x ≤ y.");
        }
        boolean last = endIndex == ends.length - 1;
        int startLen = builder.length();
        for (int i = start; i <= end; ++i) {
            builder.appendCodePoint(i);
            if (last) {
                output.add(builder.toString());
            } else {
                add(endIndex+1, startOffset, starts, ends, builder, output);
            }
            builder.setLength(startLen);
        }
    }
}

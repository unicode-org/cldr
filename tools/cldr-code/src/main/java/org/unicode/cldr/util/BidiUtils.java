package org.unicode.cldr.util;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.ibm.icu.text.Bidi;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSet.SpanCondition;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * A set of utilities for handling BIDI, especially in charts and examples but not restricted to
 * that.
 */
public class BidiUtils {
    public static final String ALERT = "⚠️";
    static final String LRM = CodePointEscaper.LRM.getString();

    // These are intended to be classes of characters that "stick together in order"
    // The initial focus is dates, so this will probably need to be expanded for numbers; might need
    // more syntax

    private enum SpanClass {
        NUMBERS("\\p{N}"),
        LETTERS_MARKS("[\\p{L}\\p{M}]"),
        DATE_PUNCT("[+]"),
        SPACES("\\p{Z}"),
        OTHERS("\\p{any}") // must be last, to pick up remainder.
    ;
        final UnicodeSet uset;

        private SpanClass(String unicodeSetSource) {
            uset = new UnicodeSet(unicodeSetSource);
        }

        static {
            // clean up by removing previous values
            UnicodeSet soFar = new UnicodeSet();
            for (SpanClass sc : SpanClass.values()) {
                sc.uset.removeAll(soFar).freeze();
                soFar.addAll(sc.uset);
            }
        }
    }
    /**
     * Checks the ordering of the example, under the specified bidiDirectionOptions;
     *
     * @param example Source text, not HTMLified
     * @param outputReorderedResults One string for each specified bidiDirectionOption
     * @param bidiDirectionOptions an array of BIDI directions from com.ibm.icu.text.Bidi. if there
     *     are no items, the default is DIRECTION_DEFAULT_LEFT_TO_RIGHT (dir="auto"),
     *     DIRECTION_RIGHT_TO_LEFT (dir="rtl").
     * @return true unless two or more of the resulting strings are different.
     */
    public static boolean isOrderingUnchanged(
            String example, List<String> outputReorderedResults, int... bidiDirectionOptions) {
        boolean hasList = outputReorderedResults != null;
        if (!hasList) {
            outputReorderedResults = new ArrayList<>();
        } else {
            outputReorderedResults.clear();
        }
        boolean result = true;
        for (int count = 0; count < bidiDirectionOptions.length; ++count) {
            String reordered = new Bidi(example, bidiDirectionOptions[count]).writeReordered(0);
            outputReorderedResults.add(reordered);
            if (result && count != 0 && !reordered.equals(outputReorderedResults.get(0))) {
                result = false;
                if (!hasList) {
                    break; // if the output results are not needed, then stop.
                }
            }
        }
        return result;
    }

    /**
     * Return a list of the , where each span is a sequence of:
     *
     * @param orderedLTR
     * @return
     */
    /**
     * Gets the 'fields' in a formatted string, used to test whether bidi reordering causes the
     * original fields to merge when reordered. Each field is the longest contiguous span of
     * characters with the same properties: *
     *
     * <ul>
     *   <li>numbers (\p{N})
     *   <li>letters & marks ([\p{L}\p{M}
     *   <li>Other
     * </ul>
     *
     * @param ordered
     * @return a set of fields, in the same order as found in the text but duplicates removed (ike
     *     LinkedHashSeet).
     */
    public static Set<String> getFields(String reordred, Set<String> result) {
        int start = 0;
        while (start < reordred.length()) {
            for (SpanClass sc : SpanClass.values()) {
                int end = sc.uset.span(reordred, start, SpanCondition.CONTAINED);
                if (end != start) {
                    result.add(reordred.substring(start, end));
                    start = end;
                    break;
                }
            }
        }
        return ImmutableSet.copyOf(result);
    }

    /**
     * Show when the fields in strings are different
     *
     * @param bidiReordereds
     * @return
     */
    public static String getAlert(List<String> bidiReordereds) {
        Set<Set<String>> results = new LinkedHashSet<>();
        for (String bidiReordered : bidiReordereds) {
            Set<String> fieldsLTR = BidiUtils.getFields(bidiReordered, new TreeSet<>());
            results.add(fieldsLTR);
        }
        if (results.size() < 2) {
            return "";
        }
        // there can still be differences within a field of OTHERS, that we  ignore.
        // EG ⚠️ 20,28,2B; 2B,28,20 " (+" vs " (+"

        // show just the difference in the first 2, for now.
        Iterator<Set<String>> it = results.iterator();
        Set<String> first = it.next();
        Set<String> second = it.next();
        SetView<String> uniqueFirst = Sets.difference(first, second);
        SetView<String> uniqueSecond = Sets.difference(second, first);
        return ALERT + " " + escape(uniqueFirst) + "; " + escape(uniqueSecond);
    }

    public static String escape(Set<String> uniqueFirst) {
        return uniqueFirst.stream()
                .map(x -> CodePointEscaper.toEscaped(x))
                .collect(Collectors.joining(LRM + ", " + LRM, LRM, LRM));
    }

    public static String alphagram(String string) {
        return string.codePoints()
                .sorted()
                .collect(
                        StringBuilder::new, // Supplier<R> supplier
                        StringBuilder::appendCodePoint, // ObjIntConsumer<R> accumulator
                        StringBuilder::append // BiConsumer<R,​R> combiner
                        )
                .toString();
    }
}

package org.unicode.cldr.unittest;

import java.text.CharacterIterator;
import java.util.Map;
import java.util.TreeMap;

import org.unicode.cldr.util.CollationMapMaker;
import org.unicode.cldr.util.Dictionary;
import org.unicode.cldr.util.Dictionary.DictionaryCharList;
import org.unicode.cldr.util.ReferenceStringSearch;
import org.unicode.cldr.util.ReferenceStringSearch.ExtendedRange;
import org.unicode.cldr.util.ReferenceStringSearch.Range;
import org.unicode.cldr.util.StateDictionary;
import org.unicode.cldr.util.StateDictionaryBuilder;
import org.unicode.cldr.util.Timer;
import org.unicode.cldr.util.Utf8StringByteConverter;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.SearchIterator;
import com.ibm.icu.text.StringCharacterIterator;
import com.ibm.icu.text.StringSearch;
import com.ibm.icu.util.ULocale;

public class TestReferenceStringSearch {
    /****************** SIMPLE TESTING ***************/

    /**
     * for simple testing of this class.
     *
     * @param args
     */
    public static final void main(String[] args) {
        checkTestCases();
        checkAgainstStringSearch();
    }

    static final RuleBasedCollator TEST_COLLATOR = (RuleBasedCollator) Collator
        .getInstance(ULocale.ENGLISH);
    static {
        TEST_COLLATOR.setStrength(Collator.PRIMARY);
        TEST_COLLATOR.setAlternateHandlingShifted(true); // ignore puncuation
    }

    static final BreakIterator TEST_BREAKER = BreakIterator
        .getCharacterInstance();

    private static void checkTestCases() {
        String[][] testCases = { { "abc", "ABCABC" }, { "abc", "abc" },
            { "e\u00DF", " ess e\u00DF ESS\u0300 " },
            { "a!a", "b.a.a.a.b" },
            { "\u03BA\u03B1\u03B9", "\u03BA\u03B1\u1FBE" }, };

        ReferenceStringSearch refSearch = new ReferenceStringSearch()
            .setCollator(TEST_COLLATOR).setBreaker(TEST_BREAKER);
        ExtendedRange extendedRange = new ExtendedRange();
        Range range = new Range();

        for (int i = 0; i < testCases.length; ++i) {
            String key = testCases[i][0];
            String target = testCases[i][1];
            refSearch.setKey(key).setTarget(target);

            System.out.println("Raw Positions of '" + key + "' in '" + target
                + "'");
            int count = 0;
            while (refSearch.searchForwards(extendedRange)) {
                // System.out.println(extendedRange); // for the numeric offsets
                System.out.println("\t" + extendedRange.toString(key, target));
                ++count;
            }
            System.out.println("  Count: " + count);

            refSearch.setNativeOffset(0);
            System.out
                .println("Positions of '" + key + "' in '" + target + "'");
            count = 0;
            while (refSearch.searchForwards(range)) {
                // System.out.println(range); // for the numeric offsets
                System.out.println("\t" + range.toString(key, target));
                ++count;
            }
            System.out.println("  Count: " + count);
            System.out.println();
        }
    }

    private static void checkAgainstStringSearch() {
        int maxCount = (int) Math.pow(2, 12);
        Timer icuTimer = new Timer();
        Timer newTimer = new Timer();
        Timer directTimer = new Timer();

        for (int count = 1; count <= maxCount; count *= 2) {
            String bigText = Utility.repeat(
                "The quick brown fox jumped over the L\u00E3zy dog. ",
                count);
            int[] icuPos = new int[count * 2];
            int[] newPos = new int[count * 2];
            int[] directPos = new int[count * 2];

            int oldCount = checkOld("lazy", bigText, icuPos, icuTimer);
            int newCount = checkNew("lazy", bigText, newPos, newTimer, icuTimer);
            int directCount = checkNew("lazy", bigText, directPos, directTimer,
                icuTimer);

            int diff = findDifference(icuPos, newPos, oldCount, newCount);
            if (diff >= 0) {
                System.out.println("\tDifference at " + diff + ", "
                    + icuPos[diff] + ", " + newPos[diff]);
            } else {
                System.out.println("\tNo Difference in results: icu vs new");
            }
            diff = findDifference(icuPos, directPos, oldCount, directCount);
            if (diff >= 0) {
                System.out.println("\tDifference at " + diff + ", "
                    + icuPos[diff] + ", " + directPos[diff]);
            } else {
                System.out.println("\tNo Difference in results: icu vs direct");
            }

            System.out.println();
            System.out.flush();
        }
    }

    private static int checkNew(String key, String bigText, int[] newPos,
        Timer newTimer, Timer icuTimer) {
        int count;

        count = 0;
        Range range = new Range();
        ReferenceStringSearch rss = new ReferenceStringSearch()
            .setCollator(TEST_COLLATOR).setBreaker(TEST_BREAKER)
            .setKey(key).setTarget(bigText);

        rss.searchForwards(range);
        newPos[count++] = range.start;
        newPos[count++] = range.limit;

        newTimer.start();

        while (rss.searchForwards(range)) {
            newPos[count++] = range.start;
            newPos[count++] = range.limit;
        }

        newTimer.stop();
        System.out.println("New: " + nf.format(count) + ", Time: "
            + newTimer.toString(icuTimer));

        return count;
    }

    static DirectStringSearch rss = new DirectStringSearch().setCollator(
        TEST_COLLATOR).setBreaker(TEST_BREAKER);

    private static int checkDirect(String key, String bigText, int[] newPos,
        Timer newTimer, Timer icuTimer) {
        int count;

        count = 0;
        Range range = new Range();
        rss.setKey(key).setTarget(bigText);

        rss.searchForwards(range);
        newPos[count++] = range.start;
        newPos[count++] = range.limit;

        newTimer.start();

        while (rss.searchForwards(range)) {
            newPos[count++] = range.start;
            newPos[count++] = range.limit;
        }

        newTimer.stop();
        System.out.println("New: " + nf.format(count) + ", Time: "
            + newTimer.toString(icuTimer));

        return count;
    }

    static class DirectStringSearch {

        private DictionaryCharList<String> textToSearchIn;
        private DictionaryCharList<String> key;

        private BreakIterator breaker;
        int textPosition = 0;

        private StateDictionary<String> dictionary;
        private int keyLength;

        public DirectStringSearch setCollator(RuleBasedCollator collation) {
            Map<CharSequence, String> map = new TreeMap<CharSequence, String>(
                Dictionary.CHAR_SEQUENCE_COMPARATOR);
            new CollationMapMaker().generateCollatorFolding(collation, map);
            // for compactness, we'd use .setIntMapFactory(new
            // IntMap.CompactStringIntMapFactory())
            dictionary = new StateDictionaryBuilder<String>().setByteConverter(
                new Utf8StringByteConverter()).make(map);
            return this;
        }

        public boolean searchForwards(Range range) {
            main: for (; textToSearchIn.hasCharAt(textPosition + keyLength - 1); ++textPosition) {
                // see if we match at position
                for (int i = 0; i < keyLength; ++i) {
                    if (key.charAt(i) != textToSearchIn
                        .charAt(textPosition + i)) {
                        continue main;
                    }
                }
                // we have a match, so return the values
                final int start = textToSearchIn.toSourceOffset(textPosition);
                // TODO get boundaries
                // position.start = getBoundary(breaker,
                // internalPosition.minStart, internalPosition.maxStart,
                // !widestStart);
                // if (position.start == -1) continue; // failed to find the
                // right boundary
                final int limit = textToSearchIn.toSourceOffset(textPosition
                    + keyLength);
                range.start = start;
                range.limit = limit;
                return true;
            }
            return true;
        }

        public DirectStringSearch setTarget(String textToSearchIn) {
            this.textToSearchIn = new DictionaryCharList<String>(dictionary,
                textToSearchIn);
            if (breaker != null) {
                breaker.setText(textToSearchIn);
            }
            return this;
        }

        public DirectStringSearch setKey(String key) {
            this.key = new DictionaryCharList<String>(dictionary, key);
            keyLength = key.length();
            return this;
        }

        public DirectStringSearch setBreaker(BreakIterator breaker) {
            this.breaker = breaker;
            return this;
        }

        // char[] convert(String text) {
        // StringBuilder result = new StringBuilder();
        // keyMatcher.setText(text); // reset the matcher to the start
        // while (true) {
        // CharSequence value = next(keyMatcher);
        // if (value == null) {
        // break;
        // }
        // result.append(value);
        // }
        // // convert to char array
        // char[] tempResult = new char[result.length()];
        // result.getChars(0, tempResult.length, tempResult, 0);
        // return tempResult;
        // }

        // /**
        // * Get the next sequence of chars from the matcher, and move the
        // cursor past it.
        // * @param matcher
        // * @param result
        // * @param resultOffset
        // * @return
        // */
        // static <T extends CharSequence> CharSequence next(Matcher<T> matcher)
        // {
        // if (!matcher.hasMore()) {
        // return null;
        // }
        // Status status = matcher.next(Matcher.Filter.LONGEST_MATCH);
        // if (status == Status.MATCH) {
        // matcher.setOffset(matcher.getMatchEnd());
        // return matcher.getMatchValue();
        // } else {
        // // needs optimization
        // final int offset = matcher.getOffset();
        // matcher.setOffset(offset + 1);
        // return matcher.getText().subSequence(offset, offset+1);
        // }
        // }
    }

    //
    // static class CircularBuffer<T> {
    // private T[] buffer;
    // private int start = 0;
    // private int limit = 0;
    // // invariants:
    // // both < buffer.length;
    // // limit == start means it is empty. So any non-empty buffer has
    // different values
    //
    // public CircularBuffer(T[] buffer) {
    // this.buffer = buffer;
    // }
    //
    // /**
    // * Get the total capacity of the buffer.
    // */
    // public int capacity() {
    // return buffer.length - 1;
    // }
    //
    // /**
    // * Remove from the front of the queue
    // * @return
    // */
    // public T remove() {
    // if (start == limit) {
    // throw new ArrayIndexOutOfBoundsException();
    // }
    // T value = buffer[start];
    // ++start;
    // if (start == buffer.length) {
    // start = 0;
    // }
    // return value;
    // }
    //
    // /**
    // * Add to the end of the queue
    // * @param item
    // */
    // public void add(T item) {
    // int oldLimit = limit;
    // if (limit + 1 == start || start == 0 && limit + 1 == buffer.length) {
    // throw new ArrayIndexOutOfBoundsException();
    // }
    // ++ limit;
    // if (limit == buffer.length) {
    // limit = 0;
    // }
    // if (limit == start) {
    // limit = oldLimit;
    // throw new ArrayIndexOutOfBoundsException();
    // }
    // buffer[oldLimit] = item;
    // }
    //
    // /**
    // * Get the number of items in the queue
    // * @return
    // */
    // int length() {
    // int len = limit - start;
    // return len >= 0 ? len : buffer.length - len;
    // // buffer.length-start + limit-0;
    // }
    //
    // /**
    // * Compare the contents to some other list of values
    // * @param key
    // * @return
    // */
    // boolean matchesAt(T[] key) {
    // int j = start;
    // int i = 0;
    // while (true) {
    // // fail if no match
    // if (key[i] != buffer[j]) {
    // return false;
    // }
    // // succeed if at end of key
    // ++i;
    // if (i == key.length) {
    // return true;
    // }
    // // fail if end of buffer
    // ++j;
    // if (j == limit) {
    // return false;
    // }
    // // wrap around if needed
    // if (j == buffer.length) {
    // j = 0;
    // }
    // }
    // }
    // }
    //
    //
    // static class CharCircularBuffer {
    // private char[] buffer;
    // private int start = 0;
    // private int limit = 0;
    // // invariants:
    // // both < buffer.length;
    // // limit == start means it is empty. So any non-empty buffer has
    // different values
    //
    // public void set(char[] buffer) {
    // this.buffer = buffer;
    // start = limit = 0;
    // }
    //
    // /**
    // * Get the total capacity of the buffer.
    // */
    // public int capacity() {
    // return buffer.length - 1;
    // }
    //
    // /**
    // * Remove from the front of the queue
    // * @return
    // */
    // public char remove() {
    // if (start == limit) {
    // throw new ArrayIndexOutOfBoundsException();
    // }
    // char value = buffer[start];
    // ++start;
    // if (start == buffer.length) {
    // start = 0;
    // }
    // return value;
    // }
    //
    // /**
    // * Add to the end of the queue
    // * @param item
    // */
    // public void add(char item) {
    // int oldLimit = limit;
    // if (limit + 1 == start || start == 0 && limit + 1 == buffer.length) {
    // throw new ArrayIndexOutOfBoundsException();
    // }
    // ++ limit;
    // if (limit == buffer.length) {
    // limit = 0;
    // }
    // if (limit == start) {
    // limit = oldLimit;
    // throw new ArrayIndexOutOfBoundsException();
    // }
    // buffer[oldLimit] = item;
    // }
    //
    // /**
    // * Get the number of items in the queue
    // * @return
    // */
    // int length() {
    // int len = limit - start;
    // return len >= 0 ? len : buffer.length - len;
    // // buffer.length-start + limit-0;
    // }
    //
    // /**
    // * Compare the contents to some other list of values
    // * @param key
    // * @return
    // */
    // boolean matchesAt(char[] key) {
    // int j = start;
    // int i = 0;
    // while (true) {
    // // fail if no match
    // if (key[i] != buffer[j]) {
    // return false;
    // }
    // // succeed if at end of key
    // ++i;
    // if (i == key.length) {
    // return true;
    // }
    // // fail if end of buffer
    // ++j;
    // if (j == limit) {
    // return false;
    // }
    // // wrap around if needed
    // if (j == buffer.length) {
    // j = 0;
    // }
    // }
    // }
    // }
    //
    private static int checkOld(String key, String bigText, int[] icuPos,
        Timer icuTimer) {
        int count = 0;
        long time;
        CharacterIterator ci = new StringCharacterIterator(bigText);
        StringSearch foo = new StringSearch("lazy", ci, TEST_COLLATOR,
            TEST_BREAKER);

        foo.next();
        icuPos[count++] = foo.getMatchStart();
        icuPos[count++] = foo.getMatchStart() + foo.getMatchLength();

        icuTimer.start();

        while (foo.next() != SearchIterator.DONE) {
            icuPos[count++] = foo.getMatchStart();
            icuPos[count++] = foo.getMatchStart() + foo.getMatchLength();
        }

        icuTimer.stop();
        System.out.println("ICU: " + nf.format(count) + ", Time: "
            + icuTimer.toString());
        return count;
    }

    static NumberFormat nf = NumberFormat.getNumberInstance();

    private static int findDifference(int[] icuPos, int[] newPos, int oldCount,
        int newCount) {
        int count = Math.min(oldCount, newCount);
        for (int i = 0; i < count; ++i) {
            if (icuPos[i] != newPos[i]) {
                return i;
            }
        }
        if (oldCount != newCount) {
            return count;
        }
        return -1;
    }

}
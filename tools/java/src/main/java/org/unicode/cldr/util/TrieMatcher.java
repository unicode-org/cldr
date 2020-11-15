package org.unicode.cldr.util;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.function.Function;

import com.ibm.icu.util.BytesTrie.Result;
import com.ibm.icu.util.CharsTrie;
import com.ibm.icu.util.CharsTrieBuilder;
import com.ibm.icu.util.StringTrieBuilder.Option;

/**
 * Provides longest matches within strings using CharsTrie. Like CharsTrie, this is NOT thread-safe. However, clones are cheap.
 * @author markdavis
 *
 * @param <T>
 */
public final class TrieMatcher<T> implements Cloneable {

    private final CharsTrie charsTrie;
    private final T[] values;
    private int valueIndex;
    private int start;
    private int end;

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return new TrieMatcher<>(charsTrie.reset(), values);
    }

    @Override
    public String toString() {
        return start + ".." + end + ":" + valueIndex;
    }

    private TrieMatcher(CharsTrie charsTrie, T[] values) {
        this.charsTrie = charsTrie;
        this.values = values;
    }

    /**
     * Get the value from the last longestMatch. Returns null if there was not a match.
     * @return
     */
    public final T getValue() {
        return valueIndex < 0 ? null : values[valueIndex];
    }

    public final boolean hasValue() {
        return valueIndex >= 0;
    }

    public final int getStart() {
        return start;
    }

    public final int getEnd() {
        return end;
    }

    /**
     * Checks the string substring starting at start position for a match. If there is one, returns the value;
     * A subsequent getValue will also get the value.
     */
    public T matchEnd(CharSequence sequence, int position) {
        start = position;
        end = start;
        valueIndex = -1;
        final int length = sequence.length();
        if (position < length) {
            Result result = charsTrie.first(sequence.charAt(position++));
            main:
                while (true) {
                    switch(result) {
                    case NO_VALUE:
                        break;
                    case NO_MATCH:
                        break main;
                    case INTERMEDIATE_VALUE:
                        valueIndex = charsTrie.getValue();
                        end = position;
                        break;
                    case FINAL_VALUE:
                        valueIndex = charsTrie.getValue();
                        end = position;
                        return getValue();
                    }
                    if (position >= length) {
                        break main;
                    }
                    result = charsTrie.next(sequence.charAt(position++));
                }
        }
        return getValue();
    }

    /**
     * Returns the next match at or after setment.end. At the start, set segment.end to 0. When there are no more matches, returns null.
     */
    public T nextMatch(CharSequence sequence, int position) {
        valueIndex = -1;
        final int length = sequence.length();
        while (position < length) {
            matchEnd(sequence, position);
            if (valueIndex >= 0) {
                break;
            }
            position++;
        }
        return getValue();
    }

    /**
     * Convenience utility
     * @param <T>
     * @param source
     * @return
     */
    public CharSequence subsequence(CharSequence source) {
        return source.subSequence(start, end);
    }

    public CharSequence replaceAll(CharSequence source) {
        return replaceAll(source, Object::toString);
    }
    /**
     * Convenience utility
     * @param <T>
     * @param source
     * @param valueToCharSequence
     * @return
     */
    public CharSequence replaceAll(CharSequence source, Function<T, CharSequence> valueToCharSequence) {
        StringBuilder buffer = null;
        int lastEnd = 0;
        for (int i = 0; ; i = hasValue() ? end : i + 1) {
            T value = nextMatch(source, i);
            if (value == null) {
                break;
            }
            if (buffer == null) {
                buffer = new StringBuilder();
            }
            if (lastEnd < start) {
                buffer.append(source, lastEnd, start);
            }
            buffer.append(valueToCharSequence.apply(value));
            lastEnd = getEnd();
        }
        if (buffer != null && lastEnd < source.length()) {
            buffer.append(source, lastEnd, source.length());
        }
        return buffer == null ? source : buffer.toString();
    }

    /**
     * Create a builder for a TrieMatcher
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Create a builder and add the first key-value pair for matching
     */
    public static <T> Builder<T> put(CharSequence string, T value) {
        return new Builder<T>().put(string, value);
    }

    /**
     * Builder for TrieMatcher
     */
    public static class Builder<T> {
        private final CharsTrieBuilder ctBuilder = new CharsTrieBuilder();
        private final HashMap<T, Integer> values = new HashMap<>();

        private Builder() {}

        /**
         * Add a key-value pair for matching. Allows chaining.
         */
        public Builder<T> put(CharSequence string, T value) {
            if (value == null || string.length() == 0) {
                throw new IllegalArgumentException();
            }
            Integer index = values.get(value);
            if (index == null) {
                values.put(value, index = values.size());
            }
            ctBuilder.add(string, index);
            return this;
        }

        /**
         * Add multiple key-value pair for matching. Allows chaining.
         */
        public Builder<T> putAll(Iterable<CharSequence> strings, T value) {
            if (value == null) {
                throw new IllegalArgumentException();
            }
            Integer index = values.get(value);
            if (index == null) {
                values.put(value, index = values.size());
            }
            for (CharSequence string : strings) {
                ctBuilder.add(string, index);
            }
            return this;
        }

        /**
         * Create a TrieMatcher with the selected buildOption (FAST or SMALL)
         */
        public TrieMatcher<T> build(Option buildOption) {
            @SuppressWarnings("unchecked")
            T[] finalValues = (T[]) new Object[values.size()];
            for (Entry<T, Integer> entry : values.entrySet()) {
                finalValues[entry.getValue()] = entry.getKey();
            }
            return new TrieMatcher<>(ctBuilder.build(buildOption), finalValues);
        }
    }
}

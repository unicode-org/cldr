package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import sun.text.normalizer.UTF16;

import com.ibm.icu.lang.CharSequences;

/**
 * Simple cover class for converting iterators, lists of items, arrays, and
 * CharSequences into forms usable with for loops. Example:
 * 
 * <pre>
 * for (String s : With.in(someIterator)) {
 *     doSomethingWith(s);
 * }
 * 
 * for (int codePoint : With.in(&quot;abc\uD800\uDC00&quot;)) {
 *     doSomethingWith(codePoint);
 * }
 * 
 * for (int integer : With.in(1, 99, 3, 42)) {
 *     doSomethingWith(integer);
 * }
 * </pre>
 * 
 * @author markdavis
 * 
 * @param <V>
 */
public final class With<V> implements Iterable<V> {
    private Iterator<V> iterator;

    /**
     * Create a simple object for use in for loops. Example:
     * 
     * <pre>
     * for (String s : With.in(someIterator)) {
     *     doSomethingWith(s);
     * }
     * </pre>
     * 
     * @param <V>
     * @param iterator
     * @return Iterable, for use in for loops, etc.
     */
    public static <V> With<V> in(Iterator<V> iterator) {
        return new With<V>(iterator);
    }

    /**
     * Create a simple object for use in for loops. Example:
     * 
     * <pre>
     * for (int integer : With.in(1, 99, 3, 42)) {
     *     doSomethingWith(integer);
     * }
     * </pre>
     * 
     * @param <V>
     * @param iterator
     * @return Iterable, for use in for loops, etc.
     */
    public static <V> Collection<V> in(V... values) {
        // TODO: optimize this. No need to create Arrays object.
        return Arrays.asList(values);
    }

    /**
     * Create a simple object for use in for loops, handling code points
     * properly. Example:
     * 
     * <pre>
     * for (int codePoint : With.in(&quot;abc\uD800\uDC00&quot;)) {
     *     doSomethingWith(codePoint);
     * }
     * </pre>
     * 
     * Actually returns an array, which avoids boxing/unboxing costs.
     * 
     * @param <V>
     * @param iterator
     * @return Iterable, for use in for loops, etc.
     */
    public static int[] in(CharSequence source) {
        return CharSequences.codePoints(source);
    }

    public static With<CharSequence> inCP(CharSequence source) {
        return new With(new CharSequenceSimpleIterator(source));
    }

    private With(Iterator<V> iterator) {
        this.iterator = iterator;
    }

    private With(SimpleIterator<V> iterator) {
        this.iterator = new IteratorWrapper<V>(iterator);
    }

    @Override
    public Iterator<V> iterator() {
        return iterator;
    }

    public Collection<V> toCollection() {
        Collection<V> result = new ArrayList<V>();
        while (iterator.hasNext()) {
            result.add(iterator.next());
        }
        return result;
    }
    
    public interface SimpleIterator<T> {
        /**
         * Returns null when done
         * @return object, or null when done.
         */
        public T next();
    }
    
    public static class CharSequenceSimpleIterator implements SimpleIterator<CharSequence> {
        private int position;
        private CharSequence source;
        public CharSequenceSimpleIterator(CharSequence source) {
            this.source = source;
        }
        @Override
        public CharSequence next() {
            // TODO optimize
            if (position >= source.length()) {
                return null;
            }
            int codePoint = Character.codePointAt(source, position);
            position += Character.charCount(codePoint);
            return UTF16.valueOf(codePoint);
        }
    }
    
    public static class IteratorWrapper<T> implements Iterator<T> {
        private final SimpleIterator<T> simpleIterator;
        private T current;
        /**
         * @param simpleIterator
         */
        public IteratorWrapper(SimpleIterator<T> simpleIterator) {
            this.simpleIterator = simpleIterator;
            current = simpleIterator.next();
        }

        @Override
        public boolean hasNext() {
            return current != null;
        }

        @Override
        public T next() {
            T result = current;
            current = simpleIterator.next();
            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
package org.unicode.cldr.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.Transform;
import com.ibm.icu.text.UTF16;

/**
 * Simple cover class for converting iterators, lists of items, arrays, and
 * CharSequences into forms usable with for loops. Example:
 *
 * <pre>
 * for (String s : With.in(someIterator)) {
 *     doSomethingWith(s);
 * }
 *
 * for (int codePoint : With.codePointArray(&quot;abc\uD800\uDC00&quot;)) {
 *     doSomethingWith(codePoint);
 * }
 *
 * for (int integer : With.array(1, 99, 3, 42)) {
 *     doSomethingWith(integer);
 * }
 * </pre>
 *
 * @author markdavis
 *
 * @param <V>
 */
public final class With<V> implements Iterable<V>, Iterator<V> {
    List<Iterator<V>> iterators = new ArrayList<Iterator<V>>();
    int current;

    /**
     * Interface for an iterator that is simpler to implement, without 'look-ahead'.
     * Using With.in(), this can be transformed into a regular Java iterator.
     * The one restriction is that elements cannot be null, since that signals end of the sequence.
     *
     * @author markdavis
     *
     * @param <T>
     */
    public interface SimpleIterator<T> {
        /**
         * Returns null when done
         *
         * @return object, or null when done.
         */
        public T next();
    }

    @Override
    public Iterator<V> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        while (current < iterators.size()) {
            if (iterators.get(current).hasNext()) {
                return true;
            }
            current++;
        }
        return false;
    }

    @Override
    public V next() {
        return iterators.get(current).next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * Create a collection from whatever is left in the iterator. For example, myCollection =
     * With.in(anIterator).toList();
     *
     * @return
     */
    public List<V> toList() {
        return toCollection(new ArrayList<V>());
    }

    /**
     * Create a collection from whatever is left in the iterator. For example, myCollection =
     * With.in(anIterator).toList();
     *
     * @return
     */
    public <C extends Collection<V>> C toCollection(C output) {
        while (hasNext()) {
            output.add(next());
        }
        return output;
    }

    /**
     * Create a collection from whatever is left in the iterator. For example, myCollection =
     * With.in(anIterator).toList();
     *
     * @return
     */
    public <C extends Collection<V>> C toUnmodifiableCollection(C output) {
        while (hasNext()) {
            output.add(next());
        }
        return CldrUtility.protectCollection(output);
    }

    /**
     * Create a collection from whatever is left in the iterator. For example, myCollection =
     * With.in(anIterator).toList();
     *
     * @return
     */
    public <W, C extends Collection<W>> C toCollection(Transform<V, W> filter, C output) {
        while (hasNext()) {
            W transformedItem = filter.transform(next());
            if (transformedItem != null) {
                output.add(transformedItem);
            }
        }
        return output;
    }

    /**
     * Create an immutable collection from whatever is left in the iterator. For example, myCollection =
     * With.in(anIterator).toList();
     *
     * @return
     */
    public <W, C extends Collection<W>> C toUnmodifiableCollection(Transform<V, W> filter, C output) {
        return CldrUtility.protectCollection(toCollection(filter, output));
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
    @SuppressWarnings("unchecked")
    public static <V> V[] array(V... values) {
        return values;
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
     * @param iterator
     * @return Iterable, for use in for loops, etc.
     */
    public static int[] codePointArray(CharSequence source) {
        return CharSequences.codePoints(source);
    }

    /**
     * An alterative to With.in(CharSequence) that is better when it is likely that only a portion of the text will be
     * looked at,
     * such as when an iterator over codepoints is aborted partway.
     *
     * @param old
     * @return
     */
    public static With<CharSequence> codePoints(CharSequence... charSequences) {
        return new With<CharSequence>().andCodePoints(charSequences);
    }

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
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <V> With<V> in(Iterator<V>... iterators) {
        return new With<V>().and(iterators);
    }

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
    @SuppressWarnings("unchecked")
    public static <V> With<V> in(Iterable<V>... iterables) {
        return new With<V>().and(iterables);
    }

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
    @SuppressWarnings("unchecked")
    public static <V> With<V> in(V... items) {
        return new With<V>().and(items);
    }

    /**
     * Creates an iterable from a simple iterator.
     *
     * @param <T>
     * @param old
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> Iterable<T> in(SimpleIterator<T>... sources) {
        return new With<T>().and(sources);
    }

    private With() {
    }

    @SuppressWarnings("unchecked")
    public With<V> and(Iterator<V>... iterators) {
        for (Iterator<V> iterator : iterators) {
            this.iterators.add(iterator);
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public With<V> and(V... items) {
        return and(Arrays.asList(items));
    }

    @SuppressWarnings("unchecked")
    public With<V> and(Iterable<V>... iterables) {
        for (Iterable<V> iterable : iterables) {
            this.iterators.add(iterable.iterator());
        }
        return this;
    }

    @SuppressWarnings("unchecked")
    public With<V> and(SimpleIterator<V>... iterators) {
        for (SimpleIterator<V> iterator : iterators) {
            this.iterators.add(new ToIterator<V>(iterator));
        }
        return this;
    }

    /**
     * Will fail if V is not a CharSequence.
     *
     * @param sources
     * @return
     */
    public With<V> andCodePoints(CharSequence... sources) {
        for (CharSequence charSequence : sources) {
            this.iterators
                .add((Iterator<V>) new ToIterator<CharSequence>(new CharSequenceSimpleIterator(charSequence)));
        }
        return this;
    }

    // new CharSequenceSimpleIterator(source)

    private static class CharSequenceSimpleIterator implements SimpleIterator<CharSequence> {
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

    public static <T> Iterator<T> toIterator(SimpleIterator<T> simple) {
        return new ToIterator<T>(simple);
    }

    public static <T> Iterable<T> toIterable(SimpleIterator<T> simple) {
        return new ToIterator<T>(simple);
    }

    public static <T> ToSimpleIterator<T> toSimpleIterator(Iterator<T> iterator) {
        return new ToSimpleIterator<T>(iterator);
    }

    private static class ToIterator<T> implements Iterator<T>, Iterable<T> {
        private final SimpleIterator<T> simpleIterator;
        private T current;

        /**
         * @param simpleIterator
         */
        public ToIterator(SimpleIterator<T> simpleIterator) {
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

        @Override
        public Iterator<T> iterator() {
            return this;
        }
    }

    private static class ToSimpleIterator<T> implements SimpleIterator<T> {
        private final Iterator<T> iterator;

        public ToSimpleIterator(Iterator<T> iterator) {
            this.iterator = iterator;
        }

        @Override
        public T next() {
            return iterator.hasNext() ? iterator.next() : null;
        }
    }

}

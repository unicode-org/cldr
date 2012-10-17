package org.unicode.cldr.draft;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.ibm.icu.text.Transform;

public final class Iterables<T> implements Iterable<T>, Iterator<T> {

    private List<Iterator<T>> iterators = new ArrayList<Iterator<T>>();
    private Iterator<T> current = null;
    private int position = 0;

    public Iterables<T> and(Iterator<T> iteratorsIn) {
        if (current == null) {
            current = iteratorsIn;
        }
        iterators.add(iteratorsIn);
        return this;
    }

    public Iterables<T> and(Iterable<T> iterable) {
        return and(iterable.iterator());
    }

    public Iterables<T> and(T... iteratorsIn) {
        return and(Arrays.asList(iteratorsIn));
    }

    public <S> Iterables<T> and(Transform<S, T> transform, Iterator<S> iteratorsIn) {
        return and(new TransformIterator<S, T>(transform, iteratorsIn));
    }

    public <S> Iterables<T> and(Transform<S, T> transform, Iterable<S> iteratorsIn) {
        return and(new TransformIterator<S, T>(transform, iteratorsIn.iterator()));
    }

    public <S> Iterables<T> and(Transform<S, T> transform, S... iteratorsIn) {
        return and(transform, Arrays.asList(iteratorsIn).iterator());
    }

    // Convenience methods

    // static <T> Iterable<T> from(Iterator<T> iteratorsIn) {
    // return new Iterables<T>().and(iteratorsIn);
    // }
    //
    // static <T> Iterable<T> from(Iterable<T> iteratorsIn) {
    // return new Iterables<T>().and(iteratorsIn);
    // }
    //
    // public static <T> Iterable<T> from(T iteratorsIn) {
    // return new Iterables<T>().and(iteratorsIn);
    // }
    //
    // static <S,T> Iterable<T> from(Transform<S,T> transform, Iterator<T> iteratorsIn) {
    // return new Iterables<T>().and(iteratorsIn);
    // }
    //
    // static <S,T> Iterable<T> from(Transform<S,T> transform, Iterable<T> iteratorsIn) {
    // return new Iterables<T>().and(iteratorsIn);
    // }
    //
    // public static <S,T> Iterable<T> from(Transform<S,T> transform, T... iteratorsIn) {
    // return new Iterables<T>().and(iteratorsIn);
    // }

    public Iterator<T> iterator() {
        return this;
    }

    public boolean hasNext() {
        while (current != null) {
            if (current.hasNext()) {
                return true;
            }
            ++position;
            if (position >= iterators.size()) {
                current = null;
                return false;
            }
            current = iterators.get(position);
        }
        return false;
    }

    public T next() {
        return current.next();
    }

    public void remove() {
        current.remove();
    }

    static final class TransformIterator<S, T> implements Iterator<T> {

        private Transform<S, T> transform;
        private Iterator<S> iterator;

        public TransformIterator(Transform<S, T> transform, Iterator<S> iterator) {
            this.iterator = iterator;
            this.transform = transform;
        }

        public boolean hasNext() {
            return iterator.hasNext();
        }

        public T next() {
            return transform.transform(iterator.next());
        }

        public void remove() {
            iterator.remove();
        }
    }
}
